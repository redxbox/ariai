package com.ariai.app.data.remote

import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ConnectionCheckTest {
    @Test
    fun unreadableStoredKeyRequestsReentryWithoutMakingAProviderCall() = runBlocking {
        var fetched = false
        val provider = Provider(
            name = "Test provider",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://api.example.com/v1",
            apiKey = "",
            apiKeyNeedsReentry = true
        )

        val result = checkConnection(provider) {
            fetched = true
            emptyList()
        }

        assertFalse(fetched)
        assertTrue(result is ConnectionResult.Failed)
        result as ConnectionResult.Failed
        assertEquals("The saved API key could not be unlocked.", result.problem)
        assertEquals("Re-enter it in AI Providers and save.", result.hint)
    }

    @Test
    fun provider401IsReportedAsAnAuthenticationRejection() = runBlocking {
        val provider = Provider(
            name = "Test provider",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://api.example.com/v1",
            apiKey = "test-key"
        )

        val result = checkConnection(provider) {
            throw IOException("API Error 401: Invalid API key")
        }

        assertTrue(result is ConnectionResult.Failed)
        result as ConnectionResult.Failed
        assertEquals("The API key was rejected", result.problem)
        assertEquals("401", result.code)
    }
}
