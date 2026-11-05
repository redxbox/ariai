package com.ariai.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class OAuthFailureMessagesTest {
    @Test
    fun mapsAuthorizationDenialAndExpiredAuthorizationCodeToHelpfulMessages() {
        assertEquals(
            "OAuth access was denied. You can try signing in again.",
            oauthFailureMessageKey("access_denied", OAuthFailureStage.AUTHORIZATION)
        )
        assertEquals(
            "The OAuth authorization code expired or was already used. Start sign-in again.",
            oauthFailureMessageKey("invalid_grant", OAuthFailureStage.TOKEN_EXCHANGE)
        )
    }

    @Test
    fun mapsRevokedRefreshTokenAndConfigurationErrorsWithoutShowingServerText() {
        assertEquals(
            "The OAuth refresh token expired or was revoked. Reconnect to continue.",
            oauthFailureMessageKey("INVALID_GRANT", OAuthFailureStage.TOKEN_REFRESH)
        )
        assertEquals(
            "The OAuth server rejected the requested scopes. Check the configured scopes.",
            oauthFailureMessageKey("invalid_scope", OAuthFailureStage.TOKEN_EXCHANGE)
        )
        assertEquals(
            "OAuth token refresh failed. Check your connection and try again.",
            oauthFailureMessageKey("unrecognized_error", OAuthFailureStage.TOKEN_REFRESH)
        )
    }
}
