package com.ariai.app.data.remote

import com.ariai.app.data.models.GeneratedImage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class AIClientGeneratedImageSecurityTest {
    @Test
    fun acceptsWithinLimitAndRejectsOverLimitBase64Images() {
        assertArrayEquals(
            byteArrayOf(1, 2, 3),
            decodeGeneratedImageBase64("AQID", maxBytes = 3)
        )
        assertThrows(IOException::class.java) {
            decodeGeneratedImageBase64("AQIDBA==", maxBytes = 3)
        }
    }

    @Test
    fun generatedImageUrlsRejectCleartextAndPrivateHosts() {
        val client = AIClient()
        listOf(
            "http://example.com/image.png",
            "https://127.0.0.1/image.png",
            "https://[::1]/image.png"
        ).forEach { url ->
            assertThrows(IOException::class.java) {
                runBlocking { client.imageBytes(GeneratedImage(url = url)) }
            }
        }
    }
}
