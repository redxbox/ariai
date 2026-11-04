package com.ariai.app.data.remote

import com.ariai.app.util.UiLang
import com.ariai.app.util.tx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

class ErrorMapperTest {
    @Test
    fun `maps oversized requests without exposing response body`() {
        val mapped = ErrorMapper.map(IOException("HTTP 413: private validation details"))

        assertEquals("The request is too large", mapped.messageKey)
        assertEquals("413", mapped.httpCode)
    }

    @Test
    fun `maps rate limits to a stable actionable message`() {
        val mapped = ErrorMapper.map(IOException("API Error 429: quota details"))

        assertEquals("Too many requests right now", mapped.messageKey)
        assertEquals("Wait a minute and try again.", mapped.hintKey)
        assertEquals("429", mapped.httpCode)
    }

    @Test
    fun `maps video endpoint status text`() {
        val mapped = ErrorMapper.map(IOException("Video submit failed: 429: private quota details"))

        assertEquals("Too many requests right now", mapped.messageKey)
        assertEquals("429", mapped.httpCode)
    }

    @Test
    fun `maps provider authorization failures separately`() {
        val mapped = ErrorMapper.map(IOException("API Error 403: forbidden"))

        assertEquals("The provider denied access", mapped.messageKey)
        assertEquals("403", mapped.httpCode)
    }

    @Test
    fun `does not expose raw provider error bodies in chat messages`() {
        val originalLanguage = UiLang.code
        try {
            UiLang.code = "en"
            val message = ErrorMapper.chatMessage(
                IOException("API Error 401: private response body and secret-value"),
                "Example provider"
            )

            assertTrue(message.contains("The API key was rejected"))
            assertTrue(message.contains("Example provider"))
            assertFalse(message.contains("secret-value"))
            assertFalse(message.contains("private response body"))
        } finally {
            UiLang.code = originalLanguage
        }
    }

    @Test
    fun `preserves localized key recovery guidance`() {
        val originalLanguage = UiLang.code
        try {
            UiLang.code = "fa"
            val source = "${tx("The saved API key could not be unlocked.")} ${tx("Re-enter it in AI Providers and save.")}"
            val message = ErrorMapper.chatMessage(IOException(source))

            assertTrue(message.contains("کلید API ذخیره‌شده"))
            assertFalse(message.contains("The provider request could not be completed"))
        } finally {
            UiLang.code = originalLanguage
        }
    }

    @Test
    fun `maps DNS failures without echoing the host error`() {
        val mapped = ErrorMapper.map(UnknownHostException("private.internal.example"))

        assertEquals("The server name was not found", mapped.messageKey)
        assertFalse(mapped.hintKey.contains("private.internal.example"))
    }
}
