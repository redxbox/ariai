package com.ariai.app.data.remote

import com.ariai.app.data.models.AIModel
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import com.ariai.app.util.tx
import kotlinx.coroutines.CancellationException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** Result of checking a provider's address and key. */
sealed class ConnectionResult {
    data class Ok(val models: Int) : ConnectionResult()
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
    if (provider.apiKeyNeedsReentry && provider.apiKey.isBlank() && provider.type != ProviderType.OLLAMA) {
        return ConnectionResult.Failed(
            tx("The saved API key could not be unlocked."),
            tx("Re-enter it in AI Providers and save.")
        )
    }
    if (provider.type != ProviderType.OLLAMA && provider.apiKey.isBlank()) {
        return ConnectionResult.Failed(tx("No API key entered"), tx("Paste the key from the provider's dashboard."))
    }
    return try {
        val models = fetch(provider)
        if (models.isEmpty()) {
            ConnectionResult.Failed(
                tx("Connected, but the provider returned no models"),
                tx("Check that the Base URL is the API root, such as https://api.openai.com/v1")
            )
        } else {
            ConnectionResult.Ok(models.size)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (e: Exception) {
        classify(e)
    }
}

private fun classify(e: Exception): ConnectionResult {
    val msg = e.message.orEmpty()
    val code = Regex("HTTP (\\d{3})").find(msg)?.groupValues?.get(1)
    val status = code?.toIntOrNull()
    return when {
        status == 401 || status == 403 -> ConnectionResult.Failed(
            tx("The API key was rejected"),
            tx("Check the key. It must belong to this provider and the Base URL must match it."),
            code
        )
        status == 402 -> ConnectionResult.Failed(
            tx("The account has no credit"),
            tx("Add credit on the provider's billing page."),
            code
        )
        status == 404 -> ConnectionResult.Failed(
            tx("The address was not found"),
            tx("Check the Base URL. Most providers end it with /v1, and it must not point to a specific endpoint."),
            code
        )
        status == 429 -> ConnectionResult.Failed(
            tx("Too many requests right now"),
            tx("Wait a minute and try again."),
            code
        )
        status != null && status >= 500 -> ConnectionResult.Failed(
            tx("The provider's server had an error"),
            tx("This is on the provider's side. Try again later."),
            code
        )
        e is UnknownHostException -> ConnectionResult.Failed(
            tx("The server name was not found"),
            tx("Check the spelling of the Base URL and your internet connection.")
        )
        e is SocketTimeoutException -> ConnectionResult.Failed(
            tx("The server did not answer in time"),
            tx("Check your connection, or the Base URL.")
        )
        e is SSLException -> ConnectionResult.Failed(
            tx("The secure (https) connection failed"),
            tx("Check that the Base URL uses https and that the server certificate is valid.")
        )
        msg.contains("CLEARTEXT", ignoreCase = true) -> ConnectionResult.Failed(
            tx("Plain http is blocked on this phone"),
            tx("Use https. Plain http is allowed only for localhost.")
        )
        else -> ConnectionResult.Failed(
            tx("Could not connect"),
            msg.take(160).ifBlank { e::class.java.simpleName }
        )
    }
}
