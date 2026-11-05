package com.ariai.app.data.remote

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.nio.charset.StandardCharsets

class OAuthOkHttpHttpURLConnectionTest {
    @Test
    fun postsFormBodyThroughOkHttpAndReadsTokenResponse() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("{\"access_token\":\"test-token\"}"))
        server.start()

        try {
            val requestBody = "grant_type=authorization_code&code=sample-code"
            val connection = OAuthOkHttpHttpURLConnection(
                server.url("/token").url,
                OkHttpClient.Builder().build()
            )
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setDoOutput(true)
            connection.outputStream.use {
                it.write(requestBody.toByteArray(StandardCharsets.UTF_8))
            }

            assertEquals(200, connection.responseCode)
            assertEquals("{\"access_token\":\"test-token\"}", connection.inputStream.bufferedReader().use { it.readText() })

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("application/x-www-form-urlencoded", request.getHeader("Content-Type"))
            assertEquals(requestBody, request.body.readUtf8())
            assertEquals(requestBody.length.toString(), request.getHeader("Content-Length"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun doesNotFollowTokenEndpointRedirectsAndExposesErrorBody() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(307)
                .setHeader("Location", "/redirected-token-endpoint")
                .setBody("{\"error\":\"invalid_grant\"}")
        )
        server.enqueue(MockResponse().setBody("should-not-be-requested"))
        server.start()

        try {
            val connection = OAuthOkHttpHttpURLConnection(
                server.url("/token").url,
                OkHttpClient.Builder().build()
            )
            connection.requestMethod = "POST"
            connection.setDoOutput(true)
            connection.outputStream.write("grant_type=refresh_token".toByteArray(StandardCharsets.UTF_8))

            assertEquals(307, connection.responseCode)
            val errorStream = connection.errorStream
            assertNotNull(errorStream)
            assertEquals(
                "{\"error\":\"invalid_grant\"}",
                errorStream!!.bufferedReader().use { it.readText() }
            )
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }
}
