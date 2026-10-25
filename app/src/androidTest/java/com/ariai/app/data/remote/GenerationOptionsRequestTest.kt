package com.ariai.app.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import okhttp3.RequestBody
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GenerationOptionsRequestTest {
    private val client = AIClient()

    @Test
    fun sendsTopPAsTopPForOpenAiCompatibleProviders() {
        val body = requestBody(ProviderType.OPENAI_COMPATIBLE)
        val json = body.toJsonObject()

        assertEquals(0.35, json.getDouble("top_p"), 0.001)
        assertEquals(0.65, json.getDouble("temperature"), 0.001)
    }

    @Test
    fun sendsTopPInsideGeminiGenerationConfig() {
        val body = requestBody(ProviderType.GEMINI)
        val generationConfig = body.toJsonObject().getJSONObject("generationConfig")

        assertEquals(0.35, generationConfig.getDouble("topP"), 0.001)
        assertEquals(0.65, generationConfig.getDouble("temperature"), 0.001)
    }

    @Test
    fun sendsTopPAsTopPForAnthropicWhenExtendedThinkingIsOff() {
        val json = requestBody(ProviderType.ANTHROPIC).toJsonObject()

        assertEquals(0.35, json.getDouble("top_p"), 0.001)
        assertEquals(0.65, json.getDouble("temperature"), 0.001)
    }

    private fun requestBody(type: ProviderType): RequestBody = client.buildRequestBody(
        provider = Provider(
            name = "Test provider",
            type = type,
            baseUrl = "https://example.invalid/v1",
            apiKey = "test-key"
        ),
        messages = listOf(
            ChatMessage(chatId = "test-chat", role = MessageRole.USER, content = "Hello")
        ),
        modelId = "test-model",
        systemPrompt = null,
        temperature = 0.65f,
        topP = 0.35f,
        searchContext = null,
        isStream = false
    )

    private fun RequestBody.toJsonObject(): JSONObject {
        val buffer = Buffer()
        writeTo(buffer)
        return JSONObject(buffer.readUtf8())
    }
}
