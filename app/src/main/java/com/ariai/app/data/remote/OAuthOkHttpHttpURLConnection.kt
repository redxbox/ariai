package com.ariai.app.data.remote

import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit

/** A small HttpURLConnection bridge for AppAuth backed by OkHttp's public call API. */
internal class OAuthOkHttpHttpURLConnection(
    url: URL,
    private val httpClient: OkHttpClient
) : HttpURLConnection(url) {
    private val requestHeaders = Headers.Builder()
    private val requestBodyBuffer = ByteArrayOutputStream()
    private var outputStreamRequested = false
    private var response: Response? = null
    private var call: okhttp3.Call? = null

    init {
        setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MS)
        setReadTimeout(DEFAULT_READ_TIMEOUT_MS)
        setInstanceFollowRedirects(false)
    }

    @Synchronized
    @Throws(IOException::class)
    override fun connect() {
        if (connected) return

        val method = requestMethod
        val normalizedMethod = method.uppercase(Locale.ROOT)
        val requestHeadersSnapshot = requestHeaders.build()
        val bodyRequiredByMethod = normalizedMethod in METHODS_WITH_REQUIRED_BODY
        if (outputStreamRequested && normalizedMethod in METHODS_WITHOUT_BODY) {
            throw ProtocolException("$method requests cannot include a request body.")
        }
        val body = if (outputStreamRequested || bodyRequiredByMethod) {
            BufferedBytesRequestBody(
                requestBodyBuffer.toByteArray(),
                requestHeadersSnapshot["Content-Type"]?.toMediaTypeOrNull()
            )
        } else {
            null
        }
        val safeHeaders = requestHeadersSnapshot.newBuilder()
            // OkHttp computes this from the buffered body, rather than trusting a caller-supplied value.
            .removeAll("Content-Length")
            .removeAll("Transfer-Encoding")
            .build()
        val request = Request.Builder()
            .url(url)
            .headers(safeHeaders)
            .method(method, body)
            .build()
        val client = httpClient.newBuilder()
            .connectTimeout(connectTimeout.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(readTimeout.toLong(), TimeUnit.MILLISECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .build()
        val activeCall = client.newCall(request)
        call = activeCall
        response = activeCall.execute()
        connected = true
    }

    override fun disconnect() {
        call?.cancel()
        response?.close()
        response = null
        connected = false
    }

    override fun usingProxy(): Boolean = false

    @Throws(IOException::class)
    override fun getOutputStream(): OutputStream {
        if (connected) throw ProtocolException("Cannot write a request body after connecting.")
        if (requestMethod.uppercase(Locale.ROOT) in METHODS_WITHOUT_BODY) {
            throw ProtocolException("$requestMethod requests cannot include a request body.")
        }
        outputStreamRequested = true
        return requestBodyBuffer
    }

    @Throws(IOException::class)
    override fun getInputStream(): InputStream {
        connect()
        val currentResponse = checkNotNull(response)
        if (currentResponse.code >= HttpURLConnection.HTTP_BAD_REQUEST) {
            throw FileNotFoundException(url.toString())
        }
        return currentResponse.body?.byteStream() ?: ByteArrayInputStream(ByteArray(0))
    }

    override fun getErrorStream(): InputStream? {
        val currentResponse = responseOrNull() ?: return null
        if (currentResponse.code in HttpURLConnection.HTTP_OK until HttpURLConnection.HTTP_MULT_CHOICE) return null
        return currentResponse.body?.byteStream() ?: ByteArrayInputStream(ByteArray(0))
    }

    @Throws(IOException::class)
    override fun getResponseCode(): Int {
        connect()
        return checkNotNull(response).code
    }

    @Throws(IOException::class)
    override fun getResponseMessage(): String {
        connect()
        return checkNotNull(response).message
    }

    override fun getHeaderField(name: String?): String? {
        val currentResponse = responseOrNull() ?: return null
        return if (name == null) statusLine(currentResponse) else currentResponse.header(name)
    }

    override fun getHeaderField(n: Int): String? {
        val currentResponse = responseOrNull() ?: return null
        if (n == 0) return statusLine(currentResponse)
        val headerIndex = n - 1
        return if (headerIndex in 0 until currentResponse.headers.size) {
            currentResponse.headers.value(headerIndex)
        } else {
            null
        }
    }

    override fun getHeaderFieldKey(n: Int): String? {
        if (n <= 0) return null
        val currentResponse = responseOrNull() ?: return null
        val headerIndex = n - 1
        return if (headerIndex in 0 until currentResponse.headers.size) {
            currentResponse.headers.name(headerIndex)
        } else {
            null
        }
    }

    override fun getRequestProperty(key: String?): String? =
        key?.let { requestHeaders.build()[it] }

    override fun setRequestProperty(key: String?, value: String?) {
        ensureNotConnected()
        requireNotNull(key) { "Header name must not be null." }
        requireNotNull(value) { "Header value must not be null." }
        requestHeaders.set(key, value)
    }

    override fun addRequestProperty(key: String?, value: String?) {
        ensureNotConnected()
        requireNotNull(key) { "Header name must not be null." }
        requireNotNull(value) { "Header value must not be null." }
        requestHeaders.add(key, value)
    }

    @Synchronized
    private fun responseOrNull(): Response? {
        response?.let { return it }
        return try {
            connect()
            response
        } catch (_: IOException) {
            null
        }
    }

    private fun ensureNotConnected() {
        if (connected) throw IllegalStateException("Already connected.")
    }

    private fun statusLine(response: Response): String =
        "${response.protocol} ${response.code} ${response.message}".trimEnd()

    private class BufferedBytesRequestBody(
        private val bytes: ByteArray,
        private val mediaType: MediaType?
    ) : RequestBody() {
        override fun contentType(): MediaType? = mediaType
        override fun contentLength(): Long = bytes.size.toLong()
        override fun writeTo(sink: BufferedSink) {
            sink.write(bytes)
        }
    }

    private companion object {
        const val DEFAULT_CONNECT_TIMEOUT_MS = 20_000
        const val DEFAULT_READ_TIMEOUT_MS = 30_000
        val METHODS_WITHOUT_BODY = setOf("GET", "HEAD")
        val METHODS_WITH_REQUIRED_BODY = setOf("POST", "PUT", "PATCH", "PROPPATCH", "REPORT")
    }
}
