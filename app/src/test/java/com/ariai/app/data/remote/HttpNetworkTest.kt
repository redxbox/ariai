package com.ariai.app.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException

class HttpNetworkTest {
    @Test
    fun `provider credentials are only allowed on the exact HTTPS origin`() {
        val base = "https://generativelanguage.googleapis.com/v1beta"

        assertTrue(shouldSendVeoCredentials(base, "https://generativelanguage.googleapis.com/v1beta/files/1".toHttpUrl()))
        assertFalse(shouldSendVeoCredentials(base, "https://storage.googleapis.com/video.mp4".toHttpUrl()))
        assertFalse(shouldSendVeoCredentials(base, "https://generativelanguage.googleapis.com:444/video".toHttpUrl()))
        assertFalse(shouldSendVeoCredentials("http://generativelanguage.googleapis.com/v1beta", "https://generativelanguage.googleapis.com/video".toHttpUrl()))
    }

    @Test
    fun `download URLs must be HTTPS and contain no embedded credentials`() {
        assertThrows(IOException::class.java) { parseSecureHttpsUrl("http://example.com/video.mp4") }
        assertThrows(IOException::class.java) { parseSecureHttpsUrl("https://user:secret@example.com/video.mp4") }
        assertThrows(IOException::class.java) { parseSecureHttpsUrl("not a URL") }
        assertTrue(parseSecureHttpsUrl("https://storage.googleapis.com/video.mp4").isHttps)
    }

    @Test
    fun `bounded response reader accepts exact limit`() {
        assertArrayEquals(byteArrayOf(1, 2, 3), readLimited(ByteArrayInputStream(byteArrayOf(1, 2, 3)), 3))
    }

    @Test
    fun `bounded response reader rejects overflow even without a length header`() {
        assertThrows(IOException::class.java) {
            readLimited(ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)), 3)
        }
    }
}
