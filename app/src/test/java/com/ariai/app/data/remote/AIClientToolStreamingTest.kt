package com.ariai.app.data.remote

import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AIClientToolStreamingTest {
    private val client = AIClient()
    private val messages = listOf(ChatMessage(chatId = "chat", role = MessageRole.USER, content = "Search for the weather"))
    private val tools = listOf(ChatFunctionTool(
        name = "search_web",
        description = "Search the web",
        parameters = JSONObject().put("type", "object").put("properties", JSONObject())
    ) { "sunny" })

    @Test
    fun `uses provider-specific streaming endpoints`() {
        val gemini = provider(ProviderType.GEMINI, "https://generativelanguage.googleapis.com/v1beta")
        val anthropic = provider(ProviderType.ANTHROPIC, "https://api.anthropic.com/v1")
        val openAi = provider(ProviderType.OPENAI_COMPATIBLE, "https://api.example.test/v1")

        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:streamGenerateContent?alt=sse",
            client.buildChatUrl(gemini, "gemini-2.0-flash")
        )
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent",
            client.buildChatUrl(gemini, "gemini-2.0-flash", forceNonStream = true)
        )
        assertEquals("https://api.anthropic.com/v1/messages", client.buildChatUrl(anthropic, "claude"))
        assertEquals("https://api.example.test/v1/chat/completions", client.buildChatUrl(openAi, "gpt"))
    }

    @Test
    fun `includes tool declarations and stream flags in OpenAI and Anthropic requests`() {
        val openAiBody = requestBody(provider(ProviderType.OPENAI_COMPATIBLE, "https://api.example.test/v1"))
        assertTrue(openAiBody.getBoolean("stream"))
        assertEquals("auto", openAiBody.getString("tool_choice"))
        assertEquals("search_web", openAiBody.getJSONArray("tools").getJSONObject(0)
            .getJSONObject("function").getString("name"))

        val anthropicBody = requestBody(provider(ProviderType.ANTHROPIC, "https://api.anthropic.com/v1"))
        assertTrue(anthropicBody.getBoolean("stream"))
        assertEquals("search_web", anthropicBody.getJSONArray("tools").getJSONObject(0).getString("name"))
    }

    @Test
    fun `includes Gemini function declarations and identifies safe stream fallbacks`() {
        val geminiProvider = provider(ProviderType.GEMINI, "https://generativelanguage.googleapis.com/v1beta")
        val geminiBody = requestBody(geminiProvider, modelId = "gemini-2.0-flash")
        val declarations = geminiBody.getJSONArray("tools").getJSONObject(0).getJSONArray("functionDeclarations")
        assertEquals("search_web", declarations.getJSONObject(0).getString("name"))

        assertTrue(client.isUnsupportedStreamingResponse(405, ""))
        assertTrue(client.isUnsupportedStreamingResponse(400, "This endpoint does not support streaming"))
        assertFalse(client.isUnsupportedStreamingResponse(400, "Invalid API key"))
        assertFalse(client.isUnsupportedStreamingResponse(401, "Streaming is disabled"))
    }

    private fun requestBody(provider: Provider, modelId: String = "test-model"): JSONObject {
        val buffer = Buffer()
        client.buildRequestBody(
            provider = provider,
            messages = messages,
            modelId = modelId,
            systemPrompt = null,
            temperature = 0.7f,
            topP = 1.0f,
            searchContext = null,
            isStream = true,
            functionTools = tools
        ).writeTo(buffer)
        return JSONObject(buffer.readUtf8())
    }

    private fun provider(type: ProviderType, baseUrl: String) = Provider(
        name = "Test provider",
        type = type,
        baseUrl = baseUrl,
        apiKey = ""
    )
}
