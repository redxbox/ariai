package com.ariai.app.data.remote

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Read-only access to public repositories, bound to a GitHub URL explicitly supplied by the user. */
class GitHubRepositoryClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()
    private val branchCache = ConcurrentHashMap<String, String>()

    data class Repository(val owner: String, val name: String) {
        init {
            require(isRepositoryComponent(owner) && isRepositoryComponent(name)) { "Invalid GitHub repository identity" }
        }

        val key: String get() = "$owner/$name"

        companion object {
            fun fromUserText(text: String): Repository? {
                val raw = Regex("https?://(?:www\\.)?github\\.com/[^\\s<>\\\"]+", RegexOption.IGNORE_CASE)
                    .find(text)?.value?.trimEnd('.', ',', ';', ')', ']', '،', '؛')?.takeIf { it.length <= 2_048 } ?: return null
                val uri = runCatching { URI(raw) }.getOrNull() ?: return null
                val host = uri.host?.lowercase()
                if (uri.userInfo != null || (host != "github.com" && host != "www.github.com")) return null
                val parts = uri.path.trim('/').split('/').filter { it.isNotBlank() }
                if (parts.size < 2) return null
                val owner = parts[0]
                val name = parts[1].removeSuffix(".git")
                if (!isRepositoryComponent(owner) || !isRepositoryComponent(name)) return null
                return Repository(owner, name)
            }

            private val OWNER_OR_REPO = Regex("^[A-Za-z0-9_.-]{1,100}$")

            private fun isRepositoryComponent(value: String): Boolean =
                OWNER_OR_REPO.matches(value) && value != "." && value != ".."
        }
    }

    suspend fun listFiles(repository: Repository, rawPathPrefix: String? = null): String {
        val repoJson = getJson("https://api.github.com/repos/${repository.owner}/${repository.name}")
        val branch = repoJson.optString("default_branch").takeIf { it.isNotBlank() }
            ?: throw IOException("GitHub did not report a default branch")
        branchCache[repository.key] = branch
        val encodedBranch = encodePathSegment(branch)
        val treeJson = getJson("https://api.github.com/repos/${repository.owner}/${repository.name}/git/trees/$encodedBranch?recursive=1")
        val tree = treeJson.optJSONArray("tree") ?: JSONArray()
        val pathPrefix = rawPathPrefix?.trim()?.trim('/')?.takeIf { it.isNotBlank() }
        if (pathPrefix != null && (pathPrefix.length > 500 || pathPrefix.split('/').any { it == ".." || it.isBlank() })) {
            throw IOException("Invalid repository folder path")
        }
        val paths = (0 until tree.length()).mapNotNull { index ->
            val item = tree.optJSONObject(index) ?: return@mapNotNull null
            if (item.optString("type") != "blob") return@mapNotNull null
            val path = item.optString("path").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            if (pathPrefix != null && path != pathPrefix && !path.startsWith("$pathPrefix/")) return@mapNotNull null
            val size = item.optLong("size", -1L)
            "$path${if (size >= 0) "  (${size} bytes)" else ""}"
        }
        val cap = 200
        return buildString {
            appendLine("Public repository: ${repository.key}")
            appendLine("Default branch: $branch")
            appendLine("Files in tree: ${paths.size}${if (treeJson.optBoolean("truncated")) " (GitHub truncated the tree)" else ""}")
            var displayed = 0
            for (path in paths.take(cap)) {
                if (length + path.length + 1 > MAX_TREE_CHARS) break
                appendLine(path)
                displayed++
            }
            if (paths.size > displayed) appendLine("[Showing $displayed paths only; call github_list_files with path_prefix for a narrower folder if needed.]")
        }
    }

    suspend fun readFile(repository: Repository, rawPath: String): String {
        val path = rawPath.trim().removePrefix("/")
        if (path.isBlank() || path.length > 500 || path.split('/').any { it == ".." || it.isBlank() }) {
            throw IOException("Invalid repository path")
        }
        val sensitiveNames = setOf(
            ".git-credentials", ".netrc", ".npmrc", ".pypirc", ".ssh", "id_rsa", "id_ed25519", "id_ecdsa",
            "authorized_keys", "credentials.json", "secrets.json"
        )
        val containsSensitivePath = path.split('/').any { segment ->
            val name = segment.lowercase()
            name.startsWith(".env") || name.contains("secret") || name.contains("credential") || name in sensitiveNames ||
                name.endsWith(".pem") || name.endsWith(".key") || name.endsWith(".p12") || name.endsWith(".pfx") ||
                name.endsWith(".jks") || name.endsWith(".keystore")
        }
        if (containsSensitivePath) throw IOException("Sensitive files are not available through the repository reader")

        val branch = branchCache[repository.key] ?: getJson("https://api.github.com/repos/${repository.owner}/${repository.name}")
            .optString("default_branch").also { if (it.isNotBlank()) branchCache[repository.key] = it }
        if (branch.isBlank()) throw IOException("GitHub did not report a default branch")
        val encodedPath = path.split('/').joinToString("/") { encodePathSegment(it) }
        val encodedRef = URLEncoder.encode(branch, Charsets.UTF_8.name()).replace("+", "%20")
        val content = getJson("https://api.github.com/repos/${repository.owner}/${repository.name}/contents/$encodedPath?ref=$encodedRef")
        val size = content.optLong("size", -1L)
        if (size > MAX_FILE_BYTES) throw IOException("File is larger than ${MAX_FILE_BYTES / 1024} KB")
        if (content.optString("type") != "file") throw IOException("The requested path is not a file")
        if (content.optString("encoding") != "base64") throw IOException("GitHub did not return text content for this file")
        val encoded = content.optString("content").replace("\n", "").replace("\r", "")
        val bytes = try { Base64.getDecoder().decode(encoded) } catch (e: Exception) { throw IOException("Could not decode file contents", e) }
        if (bytes.size.toLong() > MAX_FILE_BYTES) throw IOException("File is larger than ${MAX_FILE_BYTES / 1024} KB")
        val text = bytes.toString(Charsets.UTF_8)
        if (text.any { it.code < 0x20 && it != '\n' && it != '\r' && it != '\t' }) {
            throw IOException("Binary files cannot be read as source text")
        }
        return buildString {
            appendLine("Repository: ${repository.key} (branch $branch)")
            appendLine("File: $path")
            appendLine("---")
            append(text.take(MAX_FILE_CHARS))
            if (text.length > MAX_FILE_CHARS) append("\n[File truncated at $MAX_FILE_CHARS characters]")
        }
    }

    private suspend fun getJson(url: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "AriAI-Android")
            .get()
            .build()
        return executeCancellable(request).use { response ->
            val body = readLimitedBody(response)
            if (!response.isSuccessful) {
                val detail = runCatching { JSONObject(body).optString("message") }.getOrDefault("")
                val status = when (response.code) {
                    403 -> "GitHub API rate limit reached; try again later."
                    404 -> "Repository or file not found, or it is private. Only public repositories are supported."
                    else -> "GitHub API request failed (HTTP ${response.code})${if (detail.isNotBlank()) ": $detail" else ""}"
                }
                throw IOException(status)
            }
            if (body.isBlank()) throw IOException("GitHub returned an empty response")
            JSONObject(body)
        }
    }

    private suspend fun executeCancellable(request: Request): Response = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWith(Result.failure(e))
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (continuation.isActive) continuation.resumeWith(Result.success(response)) else response.close()
            }
        })
    }

    private fun readLimitedBody(response: Response): String {
        val body = response.body ?: return ""
        if (body.contentLength() > MAX_RESPONSE_BYTES.toLong()) throw IOException("GitHub response exceeds the size limit")
        val output = ByteArrayOutputStream()
        body.byteStream().use { input ->
            val buffer = ByteArray(8_192)
            while (output.size() <= MAX_RESPONSE_BYTES) {
                val count = input.read(buffer, 0, minOf(buffer.size, MAX_RESPONSE_BYTES + 1 - output.size()))
                if (count < 0) break
                output.write(buffer, 0, count)
            }
        }
        if (output.size() > MAX_RESPONSE_BYTES) throw IOException("GitHub response exceeds the size limit")
        return output.toByteArray().toString(Charsets.UTF_8)
    }

    private fun encodePathSegment(segment: String): String =
        URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")

    private companion object {
        const val MAX_FILE_BYTES = 48 * 1024L
        const val MAX_FILE_CHARS = 8_000
        const val MAX_TREE_CHARS = 4_000
        const val MAX_RESPONSE_BYTES = 4 * 1024 * 1024
    }
}
