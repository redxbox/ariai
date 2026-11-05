package com.ariai.app.data.remote

import com.ariai.app.data.models.OAuth2Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OAuthPkceTest {
    @Test
    fun generatesUrlSafeVerifierWithRfc7636LengthAndEntropyEncoding() {
        val verifier = OAuthPkce.generateCodeVerifier()

        assertEquals(43, verifier.length)
        assertTrue(OAuthPkce.isValidCodeVerifier(verifier))
        assertEquals(43, OAuthPkce.codeChallenge(verifier).length)
    }

    @Test
    fun pendingAuthorizationUsesSecureStateAndVerifierValues() {
        val config = OAuth2Config(
            authorizationEndpoint = "https://identity.example.test/authorize",
            tokenEndpoint = "https://identity.example.test/token",
            clientId = "mcp-public-client",
            scopes = "tools.read"
        )
        val pending = PendingOAuthAuthorization.create(
            OAuthOwnerType.MCP_SERVER,
            "mcp-server-1",
            "https://mcp.example.test/mcp",
            config
        )

        assertTrue(OAuthPkce.isValidState(pending.state))
        assertTrue(OAuthPkce.isValidCodeVerifier(pending.codeVerifier))
        assertEquals(43, OAuthPkce.codeChallenge(pending.codeVerifier).length)
    }

    @Test
    fun derivesS256ChallengeUsingRfc7636KnownVector() {
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"

        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            OAuthPkce.codeChallenge(verifier)
        )
        assertEquals("S256", OAuthPkce.CODE_CHALLENGE_METHOD_S256)
    }

    @Test
    fun rejectsInvalidVerifierAndMissingOrMismatchedCallbackState() {
        assertFalse(OAuthPkce.isValidCodeVerifier("short"))
        assertFalse(OAuthPkce.isValidCodeVerifier("x".repeat(43) + "+"))
        assertThrows(IllegalArgumentException::class.java) { OAuthPkce.codeChallenge("short") }

        val state = "s".repeat(43)
        assertTrue(OAuthPkce.matchesState(state, state))
        assertFalse(OAuthPkce.matchesState(state, null))
        assertFalse(OAuthPkce.matchesState(state, "t".repeat(43)))
        assertFalse(OAuthPkce.matchesState("short", "short"))
    }
}
