package com.ariai.app.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

private val PUBLIC_ADDRESS_DNS = object : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = InetAddress.getAllByName(hostname).toList()
        if (addresses.isEmpty() || addresses.any { !isPublicInternetAddress(it) }) {
            throw UnknownHostException("URL host resolved to a non-public IP address")
        }
        return addresses
    }
}

private fun isPublicInternetAddress(address: InetAddress): Boolean {
    if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
        address.isSiteLocalAddress || address.isMulticastAddress
    ) return false
    val bytes = address.address.map { it.toInt() and 0xff }
    return when (address) {
        is Inet4Address -> {
            val a = bytes[0]
            val b = bytes[1]
            val c = bytes[2]
            !(a == 0 || a == 10 || a == 127 || a >= 224 ||
                a == 100 && b in 64..127 ||
                a == 169 && b == 254 ||
                a == 172 && b in 16..31 ||
                a == 192 && (b == 0 || b == 168) ||
                a == 192 && b == 0 && c == 2 ||
                a == 192 && b == 88 && c == 99 ||
                a == 198 && (b == 18 || b == 19) ||
                a == 198 && b == 51 && c == 100 ||
                a == 203 && b == 0 && c == 113)
        }
        is Inet6Address -> {
            val first = bytes[0]
            val second = bytes[1]
            val globalUnicast = (first and 0xe0) == 0x20 // 2000::/3
            val documentation = first == 0x20 && second == 0x01 && bytes[2] == 0x0d && bytes[3] == 0xb8
            val transitionRange = first == 0x20 && second == 0x01 && bytes[2] == 0x00 && bytes[3] == 0x00 ||
                first == 0x20 && second == 0x02
            globalUnicast && !documentation && !transitionRange
        }
        else -> false
    }
}

/** Read-only fetcher for user-supplied public HTTP(S) URLs. */
class PublicUrlFetcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .dns(PUBLIC_ADDRESS_DNS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    /** DNS-only preflight used before enabling a provider-managed URL context tool. */
    suspend fun isSafePublicUrl(rawUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            parseAndValidate(rawUrl)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchImageBytes(rawUrl: String, maxBytes: Int = MAX_IMAGE_BYTES): ByteArray {
        require(maxBytes in 1..MAX_IMAGE_BYTES) { "Requested image limit is out of range" }
        var current = parseAndValidate(rawUrl)
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            val request = Request.Builder()
                .url(current.toASCIIString())
                .header("User-Agent", "AriAI/1.0 (image preview)")
                .header("Accept", "image/png,image/jpeg,image/webp,image/gif")
                .get()
                .build()
            var redirectedTo: URI? = null
            val bytes = client.executeCancellable(request).use { response ->
                if (response.code in REDIRECT_CODES) {
                    val location = response.header("Location") ?: throw IOException("Redirect did not include a Location header")
                    redirectedTo = current.resolve(location)
                    return@use null
                }
                if (!response.isSuccessful) throw IOException("Image fetch failed: HTTP ${response.code}")
                val body = response.body ?: throw IOException("Image URL returned an empty body")
                val mediaType = body.contentType()?.let { "${it.type}/${it.subtype}" }?.lowercase()
                    ?: throw IOException("Image URL did not declare a content type")
                if (mediaType !in IMAGE_TYPES) throw IOException("Unsupported image content type: $mediaType")
                if (body.contentLength() > maxBytes) throw IOException("Image exceeds the ${maxBytes / 1024} KB limit")
                readLimited(body.byteStream(), maxBytes)
            }
            if (redirectedTo != null) {
                if (redirectCount == MAX_REDIRECTS) throw IOException("Too many redirects")
                current = parseAndValidate(redirectedTo.toString())
            } else {
                return bytes ?: throw IOException("Image URL returned no image data")
            }
        }
        throw IOException("Too many redirects")
    }

    suspend fun fetch(rawUrl: String, maxChars: Int = 12_000): String {
        require(maxChars in 1..50_000) { "Requested text limit is out of range" }
        var current = parseAndValidate(rawUrl)
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            val request = Request.Builder()
                .url(current.toASCIIString())
                .header("User-Agent", "AriAI/1.0 (public URL reader)")
                .header("Accept", "text/html,text/plain,application/json,application/xml,text/markdown;q=0.9,*/*;q=0.2")
                .get()
                .build()
            var redirectedTo: URI? = null
            val text = client.executeCancellable(request).use { response ->
                if (response.code in REDIRECT_CODES) {
                    val location = response.header("Location") ?: throw IOException("Redirect did not include a Location header")
                    redirectedTo = current.resolve(location)
                    return@use null
                }
                if (!response.isSuccessful) throw IOException("URL fetch failed: HTTP ${response.code}")
                val body = response.body ?: throw IOException("URL returned an empty body")
                val mediaType = body.contentType()?.let { "${it.type}/${it.subtype}" }.orEmpty().lowercase()
                if (mediaType.isNotBlank() && mediaType !in TEXT_TYPES && !mediaType.endsWith("+json") && !mediaType.endsWith("+xml")) {
                    throw IOException("Unsupported URL content type: $mediaType")
                }
                val bytes = readLimited(body.byteStream(), MAX_BYTES)
                val charset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
                val raw = bytes.toString(charset)
                if (raw.indexOf('\u0000') >= 0) throw IOException("Binary URL responses cannot be read as text")
                val title = if (mediaType == "text/html" || raw.contains("<html", ignoreCase = true)) {
                    TITLE_REGEX.find(raw)?.groupValues?.getOrNull(1)?.let(::decodeEntities).orEmpty()
                } else ""
                val plain = if (mediaType == "text/html" || raw.contains("<html", ignoreCase = true)) htmlToText(raw) else raw
                buildString {
                    appendLine("Fetched URL: ${current}")
                    if (title.isNotBlank()) appendLine("Title: ${title.take(200)}")
                    append(plain.take(maxChars).trim())
                    if (plain.length > maxChars) append("\n[Content truncated at $maxChars characters]")
                }
            }
            if (redirectedTo != null) {
                if (redirectCount == MAX_REDIRECTS) throw IOException("Too many redirects")
                current = parseAndValidate(redirectedTo.toString())
            } else {
                return text ?: throw IOException("URL returned no readable text")
            }
        }
        throw IOException("Too many redirects")
    }

    private fun parseAndValidate(rawUrl: String): URI {
        if (rawUrl.length > 2_048) throw IOException("URL is too long")
        val uri = try { URI(rawUrl.trim()) } catch (e: Exception) { throw IOException("Invalid URL", e) }
        if (uri.normalize() != uri) throw IOException("URL path contains dot segments")
        val scheme = uri.scheme?.lowercase()
        if (scheme != "https" && scheme != "http") throw IOException("Only HTTP and HTTPS URLs can be opened")
        if (uri.userInfo != null) throw IOException("URLs containing credentials are not allowed")
        val host = uri.host?.lowercase()?.trimEnd('.') ?: throw IOException("URL has no valid host")
        if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local") || host.endsWith(".internal")) {
            throw IOException("Local network URLs are not allowed")
        }
        val lookupHost = host.removePrefix("[").removeSuffix("]")
        val addresses = runCatching { InetAddress.getAllByName(lookupHost) }
            .getOrElse { throw IOException("URL host could not be resolved", it) }
        if (addresses.isEmpty() || addresses.any { !isPublicInternetAddress(it) }) {
            throw IOException("Private and local IP addresses are not allowed")
        }
        return uri
    }

    private fun htmlToText(html: String): String = html
        .replace(SCRIPT_REGEX, " ")
        .replace(STYLE_REGEX, " ")
        .replace(TAG_REGEX, " ")
        .let(::decodeEntities)
        .replace(Regex("[\\t\\x0B\\f\\r ]+"), " ")
        .replace(Regex("\\n\\s*\\n+"), "\n")
        .trim()

    private fun decodeEntities(text: String): String = text
        .replace("&nbsp;", " ", ignoreCase = true)
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)

    private companion object {
        const val MAX_BYTES = 512 * 1024
        const val MAX_IMAGE_BYTES = 20 * 1024 * 1024
        const val MAX_REDIRECTS = 4
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
        val TEXT_TYPES = setOf("text/html", "text/plain", "application/json", "application/xml", "text/xml", "text/markdown")
        val IMAGE_TYPES = setOf("image/png", "image/jpeg", "image/webp", "image/gif")
        val TITLE_REGEX = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val SCRIPT_REGEX = Regex("<(script|noscript)[^>]*>.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val STYLE_REGEX = Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val TAG_REGEX = Regex("<[^>]+>")
    }
}
