package com.ariai.app.data.remote

import com.ariai.app.data.models.AIModel
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderAuthType
import com.ariai.app.data.models.ProviderType
import com.ariai.app.util.tx
import kotlinx.coroutines.CancellationException

/** Result of checking a provider's address and key. */
sealed class ConnectionResult {
    data class Ok(val models: Int, val durationMs: Long = 0) : ConnectionResult()
    data class Failed(val problem: String, val hint: String, val code: String? = null) : ConnectionResult()
}

/**
 * Checks whether the Base URL and API key work by asking the provider for its model list.
 * Each failure is turned into a plain explanation of what to fix.
 */
suspend fun checkConnection(provider: Provider, fetch: suspend (Provider) -> List<AIModel>): ConnectionResult {
    val base = provider.baseUrl.trim()
    if (!base.startsWith("https://") && !base.startsWith("http://")) {
        return ConnectionResult.Failed(
            tx("The Base URL is missing or incomplete"),
            tx("It must start with https:// and point to the API root, for example https://api.openai.com/v1")
        )
    }
    if (provider.providerConfigNeedsReentry) {
        return ConnectionResult.Failed(
            tx("Saved provider settings could not be unlocked."),
            tx("Re-enter the provider configuration in AI Providers and save.")
        )
    }
    if (provider.authType == ProviderAuthType.API_KEY && provider.apiKeyNeedsReentry &&
        provider.apiKey.isBlank() && provider.type != ProviderType.OLLAMA
    ) {
        return ConnectionResult.Failed(
            tx("The saved API key could not be unlocked."),
            tx("Re-enter it in AI Providers and save.")
        )
    }
    if (provider.authType == ProviderAuthType.API_KEY && provider.type != ProviderType.OLLAMA && provider.apiKey.isBlank()) {
        return ConnectionResult.Failed(tx("No API key entered"), tx("Paste the key from the provider's dashboard."))
    }
    if (provider.authType == ProviderAuthType.OAUTH2 && provider.oauthConfig?.validationError() != null) {
        return ConnectionResult.Failed(tx("OAuth settings are incomplete."), tx("Enter valid OAuth endpoints and client ID."))
    }
    if (provider.authType == ProviderAuthType.OAUTH2 && provider.oauthStateJson.isNullOrBlank()) {
        return ConnectionResult.Failed(tx("Connect this provider with OAuth before testing."), tx("Use the Connect with OAuth button in provider settings."))
    }
    return try {
        val startedAt = System.nanoTime()
        val models = fetch(provider)
        val durationMs = ((System.nanoTime() - startedAt) / 1_000_000L).coerceAtLeast(0L)
        if (models.isEmpty()) {
            ConnectionResult.Failed(
                tx("Connected, but the provider returned no models"),
                tx("Check that the Base URL is the API root, such as https://api.openai.com/v1")
            )
        } else {
            ConnectionResult.Ok(models.size, durationMs)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (e: Exception) {
        classify(e)
    }
}

private fun classify(e: Exception): ConnectionResult {
    val mapped = ErrorMapper.map(e)
    return ConnectionResult.Failed(
        problem = tx(mapped.messageKey),
        hint = tx(mapped.hintKey),
        code = mapped.httpCode
    )
}
