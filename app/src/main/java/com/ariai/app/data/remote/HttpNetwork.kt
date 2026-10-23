package com.ariai.app.data.remote

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.resume

/** Execute an OkHttp request without blocking a coroutine thread; cancellation cancels the call. */
internal suspend fun OkHttpClient.executeCancellable(request: Request): Response =
    suspendCancellableCoroutine { continuation ->
        val call = newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { _, undeliveredResponse, _ -> undeliveredResponse.close() }
            }
        })
    }

/** Read at most [maxBytes], detecting overflow even when Content-Length is absent or incorrect. */
internal fun readLimited(input: InputStream, maxBytes: Int): ByteArray {
    require(maxBytes > 0) { "Maximum response size must be positive" }
    input.use { stream ->
        val out = ByteArrayOutputStream(minOf(maxBytes, 16 * 1024))
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val count = stream.read(buffer, 0, minOf(buffer.size, maxBytes - total + 1))
            if (count < 0) break
            total += count
            if (total > maxBytes) throw IOException("Response exceeds the ${maxBytes / 1024} KB limit")
            out.write(buffer, 0, count)
        }
        return out.toByteArray()
    }
}

internal fun ResponseBody.readBytesAtMost(maxBytes: Int): ByteArray {
    if (contentLength() > maxBytes) throw IOException("Response exceeds the ${maxBytes / 1024} KB limit")
    return readLimited(byteStream(), maxBytes)
}

internal fun ResponseBody.readUtf8AtMost(maxBytes: Int): String =
    String(readBytesAtMost(maxBytes), Charsets.UTF_8)

internal fun parseSecureHttpsUrl(rawUrl: String): HttpUrl {
    val url = rawUrl.toHttpUrlOrNull() ?: throw IOException("Invalid download URL")
    if (!url.isHttps || url.username.isNotEmpty() || url.password.isNotEmpty()) {
        throw IOException("Only credential-free HTTPS download URLs are allowed")
    }
    return url
}

/** Provider secrets may only be attached to the same HTTPS origin as the API endpoint. */
internal fun shouldSendVeoCredentials(apiBaseUrl: String, downloadUrl: HttpUrl): Boolean {
    val apiOrigin = apiBaseUrl.toHttpUrlOrNull() ?: return false
    return apiOrigin.isHttps && downloadUrl.isHttps &&
        apiOrigin.host == downloadUrl.host && apiOrigin.port == downloadUrl.port
}

internal object HttpBodyLimits {
    const val ERROR_BYTES = 16 * 1024
    const val JSON_BYTES = 4 * 1024 * 1024
    const val IMAGE_JSON_BYTES = 32 * 1024 * 1024
    const val IMAGE_BYTES = 20 * 1024 * 1024
    const val AUDIO_BYTES = 32 * 1024 * 1024
    const val VIDEO_BYTES = 32 * 1024 * 1024
    const val SEARCH_BYTES = 2 * 1024 * 1024
}
