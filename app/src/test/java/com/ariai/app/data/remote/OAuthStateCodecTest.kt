package com.ariai.app.data.remote

import com.ariai.app.data.models.OAuth2Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OAuthStateCodecTest {
    private val binding = oauthStateBinding(OAuthOwnerType.PROVIDER, "provider-1", "https://api.example.test/v1")
    private val config = OAuth2Config(
        authorizationEndpoint = "https://id.example.test/authorize",
        tokenEndpoint = "https://id.example.test/token",
        clientId = "native-client",
        scopes = "read write"
    )

    @Test
    fun wrapsAndUnwrapsTokenStateForMatchingOAuthSettings() {
        val authState = "{\"accessToken\":\"test-access-token\",\"refreshToken\":\"test-refresh-token\"}"
        val encoded = OAuthStateCodec.encode(config, binding, authState)
        assertTrue(encoded.contains("bindingFingerprint"))
        assertTrue(encoded.contains("test-access-token"))
        assertEquals(authState, OAuthStateCodec.decode(encoded, config, binding))
        assertNull(OAuthStateCodec.decode(encoded, config, binding + "changed-resource"))
    }

    @Test
    fun pendingAuthorizationPersistsResourceBindingAndRejectsWrongTransactionId() {
        val pending = PendingOAuthAuthorization(
            transactionId = "transaction-1",
            ownerType = OAuthOwnerType.PROVIDER,
            ownerId = "provider-1",
            resourceUri = "https://api.example.test/v1",
            config = config,
            state = "s".repeat(43),
            codeVerifier = "v".repeat(43),
            createdAtMillis = System.currentTimeMillis()
        )
        assertEquals(pending, PendingOAuthAuthorization.fromJson(pending.toJson(), pending.transactionId))
        assertNull(PendingOAuthAuthorization.fromJson(pending.toJson(), "another-transaction"))
    }

    @Test
    fun refusesStateWithDifferentSettingsOrMalformedPayload() {
        val encoded = OAuthStateCodec.encode(config, binding, "{\"accessToken\":\"secret\"}")
        assertNull(OAuthStateCodec.decode(encoded, config.copy(clientId = "other-client"), binding))
        assertNull(OAuthStateCodec.decode("not-json", config, binding))
    }
}
