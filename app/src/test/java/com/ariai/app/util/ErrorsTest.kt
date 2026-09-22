package com.ariai.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorsTest {
    @Test fun keyRejected() {
        assertTrue(Errors.friendly(IllegalStateException("HTTP 401")).contains("API key"))
    }

    @Test fun rateLimit() {
        assertTrue(Errors.friendly(RuntimeException("429 too many")).contains("Too many"))
    }

    @Test fun offline() {
        assertTrue(Errors.friendly(java.net.UnknownHostException("api.openai.com")).contains("internet"))
    }

    @Test fun redactBearer() {
        val out = Errors.redact("Authorization: Bearer sk-abcdefghijklmnop")
        assertFalse(out.contains("sk-abcdefgh"))
        assertTrue(out.contains("***"))
    }

    @Test fun redactsQueryKey() {
        val out = Errors.redact("https://example.com/v1?key=secret-value&x=1")
        assertFalse(out.contains("secret-value"))
        assertTrue(out.contains("key=***"))
    }

    @Test fun emptyMessage() {
        val s = Errors.friendly(Exception(""))
        assertTrue(s.contains("Exception") || s.contains("failed"))
    }
    @Test fun redactJsonCredential() {
        val out = Errors.redact("""{"apiKey":"super-secret","authorization":"Bearer hidden-token","x-goog-api-key":"gemini-secret"}""")
        assertFalse(out.contains("super-secret"))
        assertFalse(out.contains("hidden-token"))
        assertFalse(out.contains("gemini-secret"))
        assertTrue(out.contains("***"))
    }

    @Test fun redactHeaderCredential() {
        val out = Errors.redact("x-api-key: secret-header")
        assertFalse(out.contains("secret-header"))
        assertTrue(out.contains("***"))
    }

}
