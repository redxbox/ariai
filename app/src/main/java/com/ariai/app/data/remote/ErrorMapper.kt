package com.ariai.app.data.remote

import com.ariai.app.util.tx
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** Localized, non-sensitive explanation for an API/provider failure. */
data class MappedProviderError(
    val messageKey: String,
    val hintKey: String,
    val httpCode: String? = null
)

/** Converts transport/provider exceptions into stable user-facing messages. */
object ErrorMapper {
    private val httpCodePattern = Regex(
        "(?:HTTP\\s+|API Error\\s+|(?:submit|request|status|download|gen|edit) failed:\\s*)(\\d{3})",
        RegexOption.IGNORE_CASE
    )

    fun map(error: Throwable): MappedProviderError {
        val chain = generateSequence(error) { it.cause }.take(8).toList()
        val rawMessage = chain.mapNotNull { it.message }.joinToString(" ")
        val code = chain.asSequence()
            .mapNotNull { it.message?.let(httpCodePattern::find)?.groupValues?.getOrNull(1) }
            .firstOrNull()
        val status = code?.toIntOrNull()

        return when {
            status == 400 -> MappedProviderError(
                "The provider rejected the request",
                "Check that the selected model supports this request and that its settings are valid.",
                code
            )
            status == 401 -> MappedProviderError(
                "The API key was rejected",
                "Check the key. It must belong to this provider and the Base URL must match it.",
                code
            )
            status == 403 -> MappedProviderError(
                "The provider denied access",
                "Check that this account and API key are allowed to use the selected model.",
                code
            )
            status == 402 -> MappedProviderError(
                "The account has no credit",
                "Add credit on the provider's billing page.",
                code
            )
            status == 404 -> MappedProviderError(
                "The API endpoint or model was not found",
                "Check the Base URL and make sure the selected model is available to this provider.",
                code
            )
            status == 408 -> MappedProviderError(
                "The provider did not respond in time",
                "Try again, or check whether the provider is available.",
                code
            )
            status == 413 -> MappedProviderError(
                "The request is too large",
                "Shorten the message or reduce the size of its attachments.",
                code
            )
            status == 415 -> MappedProviderError(
                "The provider does not support this file format",
                "Try a supported image format or remove the attachment.",
                code
            )
            status == 422 -> MappedProviderError(
                "The provider could not process this request",
                "Check the selected model and the input format.",
                code
            )
            status == 429 -> MappedProviderError(
                "Too many requests right now",
                "Wait a minute and try again.",
                code
            )
            status != null && status >= 500 -> MappedProviderError(
                "The provider's server had an error",
                "This is on the provider's side. Try again later.",
                code
            )
            chain.any { it is UnknownHostException } -> MappedProviderError(
                "The server name was not found",
                "Check the spelling of the Base URL and your internet connection."
            )
            chain.any { it is SocketTimeoutException } -> MappedProviderError(
                "The server did not answer in time",
                "Check your connection, or the Base URL."
            )
            chain.any { it is SSLException } -> MappedProviderError(
                "The secure (https) connection failed",
                "Check that the Base URL uses https and that the server certificate is valid."
            )
            rawMessage.contains("CLEARTEXT", ignoreCase = true) -> MappedProviderError(
                "Plain http is blocked on this phone",
                "Use https. Plain http is allowed only for localhost."
            )
            rawMessage.contains("This provider has no built-in web search", ignoreCase = true) -> MappedProviderError(
                "This provider has no built-in search, and the selected local search service needs an API key.",
                "Add a key in Web Search settings or choose a provider with built-in search."
            )
            rawMessage.contains("This model is marked as not supporting tool calling", ignoreCase = true) -> MappedProviderError(
                "This model does not support the selected search tools.",
                "Choose a model that supports tool calling, or turn off the selected search mode."
            )
            rawMessage.contains("Web search is not enabled for this agent", ignoreCase = true) -> MappedProviderError(
                "Web search is not enabled for this agent.",
                "Enable Web Search in Agents & prompts or turn search off."
            )
            else -> MappedProviderError(
                "The provider request could not be completed.",
                "Check your internet connection and provider settings, then try again.",
                code
            )
        }
    }

    /** A chat error contains no raw server body, URL, key, or exception detail. */
    fun chatMessage(error: Throwable, providerName: String? = null): String {
        val original = error.message?.trim().orEmpty()
        val safeLocalKey = SAFE_LOCAL_MESSAGES.firstOrNull { key -> original == key || original == tx(key) }
        if (safeLocalKey != null) return tx(safeLocalKey)
        val safeComposite = SAFE_LOCAL_COMPOSITE_MESSAGES.firstOrNull { keys ->
            original == keys.joinToString(" ") { tx(it) }
        }
        if (safeComposite != null) return safeComposite.joinToString(" ") { tx(it) }

        val mapped = map(error)
        val providerSuffix = providerName?.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
        return "${tx(mapped.messageKey)}$providerSuffix"
    }

    private val SAFE_LOCAL_COMPOSITE_MESSAGES = listOf(
        listOf("The saved API key could not be unlocked.", "Re-enter it in AI Providers and save.")
    )

    private val SAFE_LOCAL_MESSAGES = listOf(
        "Empty response from API",
        "The provider returned no text response",
        "The provider returned neither a reply nor a tool call",
        "The provider requested a tool call that AriAI cannot execute yet. Disable custom tools or use built-in search.",
        "No image returned",
        "The model returned no image",
        "No image data returned",
        "Empty image response",
        "No audio returned",
        "Could not save the generated image",
        "Could not save the generated audio",
        "Could not save the generated video",
        "Image editing is not supported for Gemini providers yet.",
        "This provider has no built-in search, and the selected local service needs an API key.",
        "This model is marked as not supporting tool calling, which is required for the selected search mode.",
        "Web search is not enabled for this agent. Enable it in Agents & prompts or turn search off.",
        "Could not save the reply. Please try again.",
        "Could not save your message. Please try again."
    )
}
