package com.ariai.app.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OAuth2ConfigTest {
    @Test
    fun acceptsHttpsEndpointsAndNormalizesScopes() {
        val config = OAuth2Config(
            authorizationEndpoint = " https://id.example.test/oauth/authorize ",
            tokenEndpoint = "https://id.example.test/oauth/token",
            clientId = " public-client ",
            scopes = "openid   profile\t offline_access "
        )

        assertNull(config.validationError())
        assertEquals("openid profile offline_access", config.normalized().scopes)
        assertEquals(OAuth2Config.DEFAULT_REDIRECT_URI, config.normalized().redirectUri)
    }

    @Test
    fun rejectsInsecureOrCredentialBearingEndpointsAndNonRegisteredRedirects() {
        val good = OAuth2Config(
            authorizationEndpoint = "https://id.example.test/authorize",
            tokenEndpoint = "https://id.example.test/token",
            clientId = "public-client"
        )
        listOf(
            "http://id.example.test/authorize",
            "https://user:password@id.example.test/authorize",
            "https://id.example.test/authorize?access_token=secret",
            "https://id.example.test/authorize#fragment",
            "https://id.example.test:0/authorize"
        ).forEach { endpoint ->
            assertFalse(isValidOAuth2Endpoint(endpoint))
        }
        assertNull(good.validationError())
        assertNotEquals(null, good.copy(redirectUri = "https://attacker.example/callback").validationError())
        assertNotEquals(null, good.copy(clientId = " ").validationError())
    }

    @Test
    fun fingerprintBindsTokensToEveryOAuthSetting() {
        val original = OAuth2Config(
            authorizationEndpoint = "https://id.example.test/authorize",
            tokenEndpoint = "https://id.example.test/token",
            clientId = "public-client",
            scopes = "openid profile"
        )
        assertEquals(original.fingerprint(), original.copy(scopes = "openid  profile").fingerprint())
        assertNotEquals(original.fingerprint(), original.copy(clientId = "another-client").fingerprint())
    }
}
