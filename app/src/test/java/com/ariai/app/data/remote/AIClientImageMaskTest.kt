package com.ariai.app.data.remote

import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Base64

class AIClientImageMaskTest {
    @Test
    fun `attaches a png mask to image edit multipart request`() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"data\":[{\"b64_json\":\"${Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3))}\"}]}")
        )
        server.start()
        val source = File.createTempFile("ariai-edit-source", ".png").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val mask = File.createTempFile("ariai-edit-mask", ".png").apply { writeBytes(byteArrayOf(5, 6, 7, 8)) }
        val provider = Provider(
            name = "Test",
            type = ProviderType.OPENAI,
            baseUrl = server.url("/v1").toString().trimEnd('/'),
            apiKey = "test-key"
        )

        try {
            AIClient().editImage(provider, "dall-e-2", source, "replace the marked area", mask)
            val request = server.takeRequest()
            val body = request.body.readUtf8()

            assertTrue(request.path.orEmpty().endsWith("/v1/images/edits"))
            assertTrue(request.getHeader("Authorization").orEmpty().contains("Bearer test-key"))
            assertTrue(body.contains("name=\"image\""))
            assertTrue(body.contains("name=\"mask\""))
            assertTrue(body.contains("filename=\"${mask.name}\""))
            assertTrue(body.contains("replace the marked area"))
            assertTrue(body.contains("image/png"))
        } finally {
            source.delete()
            mask.delete()
            server.shutdown()
        }
    }
}
