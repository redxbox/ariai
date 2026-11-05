package com.ariai.app.data.remote

import com.ariai.app.util.tx
import java.io.IOException
import java.util.Locale

internal enum class OAuthFailureStage {
    AUTHORIZATION,
    TOKEN_EXCHANGE,
    TOKEN_REFRESH
}

/** Maps untrusted OAuth protocol codes to fixed, localized messages; provider descriptions are not shown. */
internal fun oauthFailureMessageKey(errorCode: String?, stage: OAuthFailureStage): String =
    when (errorCode?.trim()?.lowercase(Locale.ROOT)) {
        "access_denied" -> "OAuth access was denied. You can try signing in again."
        "invalid_grant" -> when (stage) {
            OAuthFailureStage.TOKEN_EXCHANGE -> "The OAuth authorization code expired or was already used. Start sign-in again."
            OAuthFailureStage.AUTHORIZATION -> "OAuth authorization expired or was rejected. Start sign-in again."
            OAuthFailureStage.TOKEN_REFRESH -> "The OAuth refresh token expired or was revoked. Reconnect to continue."
        }
        "invalid_client", "unauthorized_client" ->
            "The OAuth client was rejected. Check the client ID and OAuth server configuration."
        "invalid_scope" -> "The OAuth server rejected the requested scopes. Check the configured scopes."
        "invalid_request", "unsupported_response_type", "unsupported_grant_type" ->
            "The OAuth server rejected the request. Check the OAuth configuration."
        "server_error", "temporarily_unavailable" ->
            "The OAuth server is temporarily unavailable. Try again later."
        else -> when (stage) {
            OAuthFailureStage.AUTHORIZATION -> "OAuth authorization failed. Please try again."
            OAuthFailureStage.TOKEN_EXCHANGE -> "OAuth token exchange failed. Check your connection and try again."
            OAuthFailureStage.TOKEN_REFRESH -> "OAuth token refresh failed. Check your connection and try again."
        }
    }

/** A safe, stable error key for surfacing token-refresh failures in MCP connection UI. */
internal class OAuthSessionException(
    val messageKey: String,
    cause: Throwable? = null
) : IOException(tx(messageKey), cause)
