package com.ariai.app.ui.screens

import com.ariai.app.data.models.AIModel
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeSearchModelsTest {
    @Test
    fun availableModelsRequireEnabledProviderAndItsApiKey() {
        val gemini = provider(
            id = "gemini",
            type = ProviderType.GEMINI,
            apiKey = "gemini-key",
            models = listOf(
                model("gemini-text", "Gemini Text", "gemini"),
                model("gemini-text-v2", "Gemini Text v2", "gemini"),
                model("imagen", "Imagen", "gemini", outputsText = false)
            )
        )
        val openRouter = provider(
            id = "openrouter",
            type = ProviderType.OPENAI_COMPATIBLE,
            apiKey = "openrouter-key",
            models = listOf(model("anthropic/claude", "Claude via OpenRouter", "openrouter")),
            baseUrl = "https://openrouter.ai/api/v1"
        )
        val withoutKey = provider("anthropic-no-key", ProviderType.ANTHROPIC, apiKey = "")
        val disabled = provider("gemini-disabled", ProviderType.GEMINI, enabled = false)
        val locked = provider("gemini-locked", ProviderType.GEMINI, apiKeyNeedsReentry = true)
        val incompatible = provider("openai", ProviderType.OPENAI, apiKey = "openai-key")

        val choices = availableNativeSearchModels(
            listOf(withoutKey, incompatible, gemini, openRouter, disabled, locked),
            selectedProviderId = null,
            selectedModelId = null
        )

        assertEquals(listOf("gemini-text", "anthropic/claude", "gemini-text-v2"), choices.map { it.model.id })
        assertEquals(listOf("gemini", "openrouter", "gemini"), choices.map { it.provider.id })
    }

    @Test
    fun currentModelAndAnotherProviderArePrioritizedInTheTwoVisibleChoices() {
        val gemini = provider(
            id = "gemini",
            type = ProviderType.GEMINI,
            apiKey = "gemini-key",
            models = listOf(
                model("gemini-first", "Gemini First", "gemini"),
                model("gemini-selected", "Gemini Selected", "gemini")
            )
        )
        val anthropic = provider(
            id = "anthropic",
            type = ProviderType.ANTHROPIC,
            apiKey = "anthropic-key",
            models = listOf(model("claude", "Claude", "anthropic"))
        )

        val choices = availableNativeSearchModels(
            listOf(gemini, anthropic),
            selectedProviderId = "gemini",
            selectedModelId = "gemini-selected"
        )

        assertEquals(listOf("gemini-selected", "claude", "gemini-first"), choices.map { it.model.id })
        assertTrue(choices.all { it.provider.apiKey.isNotBlank() })
    }

    private fun provider(
        id: String,
        type: ProviderType,
        apiKey: String = "test-key",
        models: List<AIModel> = listOf(model("$id-model", "$id Model", id)),
        enabled: Boolean = true,
        apiKeyNeedsReentry: Boolean = false,
        baseUrl: String = when (type) {
            ProviderType.GEMINI -> "https://generativelanguage.googleapis.com"
            ProviderType.ANTHROPIC -> "https://api.anthropic.com"
            ProviderType.OPENAI -> "https://api.openai.com/v1"
            else -> "https://custom.example/v1"
        }
    ) = Provider(
        id = id,
        name = id,
        type = type,
        baseUrl = baseUrl,
        apiKey = apiKey,
        models = models,
        enabled = enabled,
        apiKeyNeedsReentry = apiKeyNeedsReentry
    )

    private fun model(id: String, name: String, providerId: String, outputsText: Boolean = true) = AIModel(
        id = id,
        displayName = name,
        providerId = providerId,
        outputsText = outputsText
    )
}
