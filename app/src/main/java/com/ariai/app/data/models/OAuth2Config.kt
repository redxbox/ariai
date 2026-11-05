package com.ariai.app.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import java.net.URI
import java.security.MessageDigest

/** Configuration for a native OAuth 2.0 public client using Authorization Code + PKCE (S256). */
@Parcelize
data class OAuth2Config(
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val clientId: String,
    val scopes: String = "",
    val redirectUri: String = DEFAULT_REDIRECT_URI
) : Parcelable {
    fun normalized(): OAuth2Config = copy(
        authorizationEndpoint = authorizationEndpoint.trim(),
        tokenEndpoint = tokenEndpoint.trim(),
        clientId = clientId.trim(),
        scopes = scopes.trim().split(Regex("\\s+")).filter(String::isNotBlank).distinct().joinToString(" "),
        redirectUri = redirectUri.trim()
    )

    fun validationError(): String? {
        val value = normalized()
        if (value.clientId.isBlank() || value.clientId.length > MAX_CLIENT_ID_LENGTH) return "Enter a valid OAuth client ID."
        if (!isValidOAuth2Endpoint(value.authorizationEndpoint)) return "The authorization endpoint must be HTTPS and cannot contain credentials, query parameters, or fragments."
        if (!isValidOAuth2Endpoint(value.tokenEndpoint)) return "The token endpoint must be HTTPS and cannot contain credentials, query parameters, or fragments."
        if (value.scopes.length > MAX_SCOPES_LENGTH || value.scopes.any { it.code < 0x20 }) return "OAuth scopes are too long or contain invalid characters."
        if (value.redirectUri != DEFAULT_REDIRECT_URI) return "Use AriAI's registered redirect URI: $DEFAULT_REDIRECT_URI"
        return null
    }

    fun requireValid(): OAuth2Config = normalized().also { normalized ->
        normalized.validationError()?.let { throw IllegalArgumentException(it) }
    }

    /** Stable, one-way binding between a token state and the endpoints/client that created it. */
    fun fingerprint(): String {
        val normalized = normalized()
        val value = listOf(
            normalized.authorizationEndpoint,
            normalized.tokenEndpoint,
            normalized.clientId,
            normalized.scopes,
            normalized.redirectUri
        ).joinToString("\u0000")
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    fun toJson(): JSONObject = JSONObject()
        .put("authorizationEndpoint", authorizationEndpoint.take(MAX_ENDPOINT_LENGTH))
        .put("tokenEndpoint", tokenEndpoint.take(MAX_ENDPOINT_LENGTH))
        .put("clientId", clientId.take(MAX_CLIENT_ID_LENGTH))
        .put("scopes", scopes.take(MAX_SCOPES_LENGTH))
        .put("redirectUri", redirectUri.take(256))

    companion object {
        const val DEFAULT_REDIRECT_URI = "com.ariai.app://oauth2redirect"
        const val MAX_ENDPOINT_LENGTH = 2_048
        const val MAX_CLIENT_ID_LENGTH = 256
        const val MAX_SCOPES_LENGTH = 1_024

        fun fromJson(value: JSONObject): OAuth2Config? = runCatching {
            OAuth2Config(
                authorizationEndpoint = value.optString("authorizationEndpoint").take(MAX_ENDPOINT_LENGTH),
                tokenEndpoint = value.optString("tokenEndpoint").take(MAX_ENDPOINT_LENGTH),
                clientId = value.optString("clientId").take(MAX_CLIENT_ID_LENGTH),
                scopes = value.optString("scopes").take(MAX_SCOPES_LENGTH),
                redirectUri = value.optString("redirectUri", DEFAULT_REDIRECT_URI).take(256)
            ).requireValid()
        }.getOrNull()
    }
}

fun isValidOAuth2Endpoint(value: String): Boolean {
    if (value.isBlank() || value.length > OAuth2Config.MAX_ENDPOINT_LENGTH) return false
    return runCatching {
        val uri = URI(value)
        uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null &&
            uri.rawQuery == null &&
            uri.rawFragment == null &&
            (uri.port == -1 || uri.port in 1..65535) &&
            uri.host.none { it.code < 0x20 }
    }.getOrDefault(false)
}

/** OAuth settings are public metadata; token state is stored separately and encrypted. */
fun OAuth2Config?.sameSettingsAs(other: OAuth2Config?): Boolean =
    this?.normalized() == other?.normalized()
