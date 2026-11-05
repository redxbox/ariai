package com.ariai.app.data.remote

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import com.ariai.app.data.local.OAuthStateStore
import com.ariai.app.data.local.PreferencesManager
import com.ariai.app.data.models.McpAuthType
import com.ariai.app.data.models.McpServerItem
import com.ariai.app.data.models.OAuth2Config
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderAuthType
import com.ariai.app.oauth.OAuthCompletionActivity
import com.ariai.app.oauth.OAuthCanceledActivity
import com.ariai.app.util.tx
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

internal enum class OAuthOwnerType { PROVIDER, MCP_SERVER }

internal fun oauthStateBinding(ownerType: OAuthOwnerType, ownerId: String, resourceUri: String): String =
    listOf(ownerType.name, ownerId, resourceUri.trim()).joinToString("\u0000")

internal data class PendingOAuthAuthorization(
    val transactionId: String,
    val ownerType: OAuthOwnerType,
    val ownerId: String,
    val resourceUri: String,
    val config: OAuth2Config,
    val state: String,
    val codeVerifier: String,
    val createdAtMillis: Long
) {
    fun toJson(): String = JSONObject()
        .put("version", VERSION)
        .put("transactionId", transactionId)
        .put("ownerType", ownerType.name)
        .put("ownerId", ownerId)
        .put("resourceUri", resourceUri)
        .put("config", config.toJson())
        .put("state", state)
        .put("codeVerifier", codeVerifier)
        .put("createdAtMillis", createdAtMillis)
        .toString()

    companion object {
        private const val VERSION = 2
        private const val MAX_AGE_MILLIS = 15 * 60 * 1000L

        fun create(
            ownerType: OAuthOwnerType,
            ownerId: String,
            resourceUri: String,
            config: OAuth2Config
        ): PendingOAuthAuthorization {
            require(ownerId.isNotBlank() && ownerId.length <= 64)
            require(resourceUri.isNotBlank() && resourceUri.length <= OAuth2Config.MAX_ENDPOINT_LENGTH)
            val normalized = config.requireValid()
            return PendingOAuthAuthorization(
                transactionId = UUID.randomUUID().toString(),
                ownerType = ownerType,
                ownerId = ownerId,
                resourceUri = resourceUri.trim(),
                config = normalized,
                state = OAuthPkce.generateState(),
                codeVerifier = OAuthPkce.generateCodeVerifier(),
                createdAtMillis = System.currentTimeMillis()
            )
        }

        fun fromJson(json: String, expectedId: String): PendingOAuthAuthorization? = runCatching {
            val value = JSONObject(json)
            if (value.optInt("version") != VERSION) return null
            val id = value.optString("transactionId")
            if (id != expectedId) return null
            val ownerId = value.optString("ownerId")
            if (ownerId.isBlank() || ownerId.length > 64) return null
            val resourceUri = value.optString("resourceUri").trim()
            if (resourceUri.isBlank() || resourceUri.length > OAuth2Config.MAX_ENDPOINT_LENGTH) return null
            val ownerType = runCatching { OAuthOwnerType.valueOf(value.optString("ownerType")) }.getOrNull()
                ?: return null
            val config = OAuth2Config.fromJson(value.optJSONObject("config") ?: return null) ?: return null
            val state = value.optString("state")
            val verifier = value.optString("codeVerifier")
            val createdAt = value.optLong("createdAtMillis", 0L)
            if (!OAuthPkce.isValidState(state) || !OAuthPkce.isValidCodeVerifier(verifier)) return null
            if (createdAt <= 0L || System.currentTimeMillis() - createdAt !in 0..MAX_AGE_MILLIS) return null
            PendingOAuthAuthorization(id, ownerType, ownerId, resourceUri, config, state, verifier, createdAt)
        }.getOrNull()
    }
}

/** Creates the browser-facing authorization request and encrypts its PKCE verifier before launch. */
object OAuthAuthorizationLauncher {
    suspend fun begin(
        activity: Activity,
        preferences: PreferencesManager,
        ownerType: String,
        ownerId: String,
        resourceUri: String,
        config: OAuth2Config
    ) {
        val type = runCatching { OAuthOwnerType.valueOf(ownerType) }.getOrElse {
            throw IllegalArgumentException("Unsupported OAuth owner type.")
        }
        val pending = PendingOAuthAuthorization.create(type, ownerId, resourceUri, config)
        val service = OAuthAuthorizationServices.create(activity)
        try {
            preferences.addPendingOAuthAuthorization(pending.transactionId, pending.toJson())
            val serviceConfig = AuthorizationServiceConfiguration(
                Uri.parse(pending.config.authorizationEndpoint),
                Uri.parse(pending.config.tokenEndpoint)
            )
            val request = AuthorizationRequest.Builder(
                serviceConfig,
                pending.config.clientId,
                ResponseTypeValues.CODE,
                Uri.parse(pending.config.redirectUri)
            )
                .setScopes(pending.config.scopes.split(Regex("\\s+")).filter(String::isNotBlank))
                .setState(pending.state)
                .setCodeVerifier(
                    pending.codeVerifier,
                    OAuthPkce.codeChallenge(pending.codeVerifier),
                    OAuthPkce.CODE_CHALLENGE_METHOD_S256
                )
                .build()
            val requestCode = SecureRandom().nextInt()
            // AppAuth supplies the redirect result through PendingIntent.send(fillInIntent), so
            // the explicit in-app completion/cancel intents must allow fill-in extras.
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_ONE_SHOT
            val completion = PendingIntent.getActivity(
                activity,
                requestCode,
                Intent(activity, OAuthCompletionActivity::class.java)
                    .putExtra(OAuthCompletionActivity.EXTRA_TRANSACTION_ID, pending.transactionId),
                flags
            )
            val canceled = PendingIntent.getActivity(
                activity,
                requestCode xor 0x5a5a5a5a,
                Intent(activity, OAuthCanceledActivity::class.java)
                    .putExtra(OAuthCanceledActivity.EXTRA_TRANSACTION_ID, pending.transactionId),
                flags
            )
            service.performAuthorizationRequest(request, completion, canceled)
        } catch (cancelled: CancellationException) {
            preferences.removePendingOAuthAuthorization(pending.transactionId)
            throw cancelled
        } catch (failure: Exception) {
            preferences.removePendingOAuthAuthorization(pending.transactionId)
            throw IOException(tx("Could not open the OAuth sign-in page."), failure)
        } finally {
            service.dispose()
        }
    }
}

/** Token refresher shared by AI providers and MCP servers. AppAuth enforces expiry and refresh rules. */
class OAuthSessionManager(
    private val stateStore: OAuthStateStore,
    context: android.content.Context
) {
    private val appContext = context.applicationContext
    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun providerAccessToken(provider: Provider): String {
        if (provider.authType != ProviderAuthType.OAUTH2) {
            throw IOException("Provider is not configured for OAuth 2.0.")
        }
        if (provider.oauthStateNeedsReentry) {
            throw IOException(tx("Reconnect this provider; its saved OAuth state could not be unlocked."))
        }
        val config = provider.oauthConfig?.requireValid()
            ?: throw IOException(tx("Enter valid OAuth settings and connect this provider."))
        val resourceUri = provider.baseUrl.trim()
        val binding = oauthStateBinding(OAuthOwnerType.PROVIDER, provider.id, resourceUri)
        return freshAccessToken("provider:${provider.id}", config, binding,
            load = { stateStore.providerState(provider.id, config, resourceUri) },
            save = { old, new -> stateStore.updateProviderStateIfUnchanged(provider.id, config, resourceUri, old, new) })
    }

    suspend fun mcpAccessToken(server: McpServerItem): String {
        if (server.authType != McpAuthType.OAUTH2) {
            throw OAuthSessionException("Enter valid OAuth settings and connect this MCP server.")
        }
        val config = server.oauthConfig?.let { candidate ->
            runCatching { candidate.requireValid() }.getOrNull()
        } ?: throw OAuthSessionException("Enter valid OAuth settings and connect this MCP server.")
        val resourceUri = server.url.trim()
        val binding = oauthStateBinding(OAuthOwnerType.MCP_SERVER, server.id, resourceUri)
        return freshAccessToken("mcp:${server.id}", config, binding,
            load = { stateStore.mcpState(server.id, config, resourceUri) },
            save = { old, new -> stateStore.updateMcpStateIfUnchanged(server.id, config, resourceUri, old, new) })
    }

    internal suspend fun completeAuthorization(
        pending: PendingOAuthAuthorization,
        response: AuthorizationResponse
    ) {
        val request = response.request
        val challenge = OAuthPkce.codeChallenge(pending.codeVerifier)
        val stateMatches = OAuthPkce.matchesState(pending.state, response.state) &&
            OAuthPkce.matchesState(pending.state, request.state)
        val redirectMatches = request.redirectUri.toString() == pending.config.redirectUri
        val configurationMatches = request.configuration.authorizationEndpoint.toString() == pending.config.authorizationEndpoint &&
            request.configuration.tokenEndpoint.toString() == pending.config.tokenEndpoint
        val pkceMatches = request.codeVerifier == pending.codeVerifier &&
            request.codeVerifierChallenge == challenge &&
            request.codeVerifierChallengeMethod == OAuthPkce.CODE_CHALLENGE_METHOD_S256
        val authorizationCode = response.authorizationCode
        if (!stateMatches || !redirectMatches || !configurationMatches || !pkceMatches ||
            request.clientId != pending.config.clientId || request.responseType != ResponseTypeValues.CODE ||
            authorizationCode.isNullOrBlank() || authorizationCode.length > MAX_AUTHORIZATION_CODE_LENGTH
        ) {
            throw IOException(tx("The OAuth redirect did not match the pending sign-in."))
        }

        val service = OAuthAuthorizationServices.create(appContext)
        try {
            val tokenResult = try {
                withTimeout(TOKEN_EXCHANGE_TIMEOUT_MS) {
                    withContext(Dispatchers.IO) {
                        suspendCancellableCoroutine<Result<TokenResponse>> { continuation ->
                            service.performTokenRequest(response.createTokenExchangeRequest()) { result, exception ->
                                when {
                                    exception != null -> if (continuation.isActive) continuation.resume(Result.failure(exception))
                                    result == null -> if (continuation.isActive) continuation.resume(Result.failure(
                                        IOException(tx("OAuth sign-in did not return an access token."))
                                    ))
                                    else -> if (continuation.isActive) continuation.resume(Result.success(result))
                                }
                            }
                        }
                    }
                }
            } catch (timeout: TimeoutCancellationException) {
                throw IOException(tx("OAuth token exchange timed out. Try again."), timeout)
            }
            val tokenResponse = tokenResult.fold(
                onSuccess = { it },
                onFailure = { failure ->
                    val errorCode = (failure as? AuthorizationException)?.error
                    throw IOException(
                        tx(oauthFailureMessageKey(errorCode, OAuthFailureStage.TOKEN_EXCHANGE)),
                        failure
                    )
                }
            )
            if (tokenResponse.accessToken.isNullOrBlank()) {
                throw IOException(tx("OAuth sign-in did not return an access token."))
            }
            if (!tokenResponse.tokenType.equals("Bearer", ignoreCase = true)) {
                throw IOException(tx("The OAuth server did not return a Bearer access token."))
            }
            val authState = AuthState()
            authState.update(response, null as AuthorizationException?)
            authState.update(tokenResponse, null as AuthorizationException?)
            val binding = oauthStateBinding(pending.ownerType, pending.ownerId, pending.resourceUri)
            val storedState = OAuthStateCodec.encode(pending.config, binding, authState.jsonSerializeString())
            val saved = when (pending.ownerType) {
                OAuthOwnerType.PROVIDER -> stateStore.completeProviderAuthorization(
                    pending.ownerId, pending.config, pending.resourceUri, storedState
                )
                OAuthOwnerType.MCP_SERVER -> stateStore.completeMcpAuthorization(
                    pending.ownerId, pending.config, pending.resourceUri, storedState
                )
            }
            if (!saved) throw IOException(tx("OAuth settings changed before sign-in finished. Save the settings and connect again."))
        } finally {
            service.dispose()
        }
    }

    private suspend fun freshAccessToken(
        key: String,
        config: OAuth2Config,
        binding: String,
        load: suspend () -> String?,
        save: suspend (oldState: String, newState: String) -> Boolean
    ): String = withContext(Dispatchers.IO) {
        locks.computeIfAbsent(key) { Mutex() }.withLock {
            val rawState = load()?.takeIf(String::isNotBlank)
                ?: throw OAuthSessionException("Connect with OAuth before making requests.")
            val serialized = OAuthStateCodec.decode(rawState, config, binding)
                ?: throw OAuthSessionException("OAuth settings changed. Connect again to continue.")
            val authState = runCatching { AuthState.jsonDeserialize(serialized) }
                .getOrElse { throw OAuthSessionException("The saved OAuth state is invalid. Reconnect to continue.", it) }
            val savedConfiguration = authState.getAuthorizationServiceConfiguration()
                ?: throw OAuthSessionException("The saved OAuth state is invalid. Reconnect to continue.")
            if (savedConfiguration.authorizationEndpoint.toString() != config.authorizationEndpoint ||
                savedConfiguration.tokenEndpoint.toString() != config.tokenEndpoint
            ) {
                throw OAuthSessionException("OAuth settings changed. Connect again to continue.")
            }

            val service = OAuthAuthorizationServices.create(appContext)
            try {
                val result = try {
                    withTimeout(TOKEN_REFRESH_TIMEOUT_MS) {
                        suspendCancellableCoroutine<Result<String>> { continuation ->
                            authState.performActionWithFreshTokens(service) { accessToken, _, exception ->
                                when {
                                    exception != null -> if (continuation.isActive) continuation.resume(Result.failure(exception))
                                    accessToken.isNullOrBlank() -> if (continuation.isActive) continuation.resume(Result.failure(
                                        OAuthSessionException("OAuth authorization expired. Reconnect to continue.")
                                    ))
                                    else -> if (continuation.isActive) continuation.resume(Result.success(accessToken))
                                }
                            }
                        }
                    }.fold(
                        onSuccess = { it },
                        onFailure = { failure ->
                            val errorCode = (failure as? AuthorizationException)?.error
                            throw OAuthSessionException(
                                oauthFailureMessageKey(errorCode, OAuthFailureStage.TOKEN_REFRESH),
                                failure
                            )
                        }
                    )
                } catch (timeout: TimeoutCancellationException) {
                    throw OAuthSessionException("OAuth token refresh timed out. Try again.", timeout)
                }

                val updatedState = OAuthStateCodec.encode(config, binding, authState.jsonSerializeString())
                if (updatedState != rawState && !save(rawState, updatedState)) {
                    throw OAuthSessionException("OAuth settings changed. Connect again to continue.")
                }
                result
            } finally {
                service.dispose()
            }
        }
    }

    private companion object {
        const val MAX_AUTHORIZATION_CODE_LENGTH = 8_192
        const val TOKEN_EXCHANGE_TIMEOUT_MS = 45_000L
        const val TOKEN_REFRESH_TIMEOUT_MS = 45_000L
    }
}

/** Keeps AppAuth's bearer/refresh-token state bound to the exact non-secret OAuth configuration. */
internal object OAuthStateCodec {
    private const val VERSION = 2
    private const val MAX_AUTH_STATE_LENGTH = 40_000
    private const val MAX_WRAPPED_STATE_LENGTH = 48_000

    fun encode(config: OAuth2Config, binding: String, authStateJson: String): String {
        require(binding.isNotBlank())
        require(authStateJson.isNotBlank() && authStateJson.length <= MAX_AUTH_STATE_LENGTH)
        return JSONObject()
            .put("version", VERSION)
            .put("bindingFingerprint", bindingFingerprint(config, binding))
            .put("authState", authStateJson)
            .toString()
            .also { require(it.length <= MAX_WRAPPED_STATE_LENGTH) }
    }

    fun decode(value: String, config: OAuth2Config, binding: String): String? = runCatching {
        if (value.length > MAX_WRAPPED_STATE_LENGTH || binding.isBlank()) return null
        val json = JSONObject(value)
        if (json.optInt("version") != VERSION) return null
        if (!constantTimeEquals(bindingFingerprint(config, binding), json.optString("bindingFingerprint"))) return null
        json.optString("authState").takeIf { it.isNotBlank() && it.length <= MAX_AUTH_STATE_LENGTH }
    }.getOrNull()

    private fun bindingFingerprint(config: OAuth2Config, binding: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest((config.fingerprint() + "\u0000" + binding).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

private fun constantTimeEquals(expected: String, actual: String): Boolean =
    MessageDigest.isEqual(expected.toByteArray(Charsets.UTF_8), actual.toByteArray(Charsets.UTF_8))
