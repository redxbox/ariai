package com.ariai.app.ui.screens

import com.ariai.app.data.models.AIModel
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeSearchModelsTest {
    @Test
    fun optionsContainOnlyGeminiAndAnthropicAndRequireTheirOwnUsableProviderKeys() {
        val gemini = provider(
            id = "gemini",
            type = ProviderType.GEMINI,
            apiKey = "gemini-key",
            models = listOf(
                model("gemini-text", "Gemini Text", "gemini"),
                model("imagen", "Imagen", "gemini", outputsText = false)
            )
        )
        val anthropicWithoutKey = provider("anthropic-no-key", ProviderType.ANTHROPIC, apiKey = "")
        val openRouter = provider(
            id = "openrouter",
            type = ProviderType.OPENAI_COMPATIBLE,
            apiKey = "openrouter-key",
            models = listOf(model("anthropic/claude", "Claude via OpenRouter", "openrouter")),
            baseUrl = "https://openrouter.ai/api/v1"
        )
        val openAI = provider("openai", ProviderType.OPENAI, apiKey = "openai-key")

        val options = nativeSearchProviderOptions(
            listOf(anthropicWithoutKey, openAI, openRouter, gemini),
            selectedProviderId = null,
            selectedModelId = null
        )

        assertEquals(listOf(ProviderType.GEMINI, ProviderType.ANTHROPIC), options.map { it.type })
        assertEquals("gemini", options[0].provider?.id)
        assertEquals("gemini-text", options[0].model?.id)
        assertTrue(options[0].isReady)
        assertEquals("anthropic-no-key", options[1].provider?.id)
        assertFalse(options[1].hasUsableKey)
        assertNull(options[1].model)
        assertFalse(options[1].isReady)
    }

    @Test
    fun currentProviderAndTextModelArePreferredForEachNativeProvider() {
        val firstGemini = provider(
            id = "gemini-first",
            type = ProviderType.GEMINI,
            apiKey = "first-key",
            models = listOf(model("gemini-first-model", "Gemini First", "gemini-first"))
        )
        val selectedGemini = provider(
            id = "gemini-selected",
            type = ProviderType.GEMINI,
            apiKey = "selected-key",
            models = listOf(
                model("image-only", "Imagen", "gemini-selected", outputsText = false),
                model("gemini-selected-model", "Gemini Selected", "gemini-selected")
            )
        )
        val anthropic = provider(
            id = "anthropic",
            type = ProviderType.ANTHROPIC,
            apiKey = "anthropic-key",
            models = listOf(model("claude", "Claude", "anthropic"))
        )

        val options = nativeSearchProviderOptions(
            listOf(firstGemini, anthropic, selectedGemini),
            selectedProviderId = "gemini-selected",
            selectedModelId = "gemini-selected-model"
        )

        assertEquals("gemini-selected", options[0].provider?.id)
        assertEquals("gemini-selected-model", options[0].model?.id)
        assertEquals("anthropic", options[1].provider?.id)
        assertEquals("claude", options[1].model?.id)
        assertTrue(options.all { it.hasUsableKey })
    }

    @Test
    fun disabledOrLockedProviderKeysCannotEnableNativeSearch() {
        val disabled = provider("gemini-disabled", ProviderType.GEMINI, enabled = false)
        val locked = provider("anthropic-locked", ProviderType.ANTHROPIC, apiKeyNeedsReentry = true)

        val options = nativeSearchProviderOptions(listOf(disabled, locked), null, null)

        assertEquals(2, options.size)
        assertFalse(options[0].hasUsableKey)
        assertFalse(options[1].hasUsableKey)
        assertNull(options[0].model)
        assertNull(options[1].model)
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
