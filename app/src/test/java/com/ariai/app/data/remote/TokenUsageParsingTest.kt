package com.ariai.app.data.remote

import com.ariai.app.data.models.ProviderType
import com.ariai.app.data.models.TokenUsage
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TokenUsageParsingTest {
    @Test
    fun readsOpenAiCompatibleUsage() {
        val usage = parseTokenUsage(
            JSONObject("""{"usage":{"prompt_tokens":120,"completion_tokens":35,"total_tokens":155}}"""),
            ProviderType.OPENAI_COMPATIBLE
        )

        assertEquals(TokenUsage(promptTokens = 120, completionTokens = 35, totalTokens = 155), usage)
    }

    @Test
    fun mergesAnthropicStreamingInputAndOutputUsage() {
        val input = parseTokenUsage(
            JSONObject("""{"type":"message_start","message":{"usage":{"input_tokens":84,"output_tokens":0}}}"""),
            ProviderType.ANTHROPIC
        )
        val output = parseTokenUsage(
            JSONObject("""{"type":"message_delta","usage":{"output_tokens":27}}"""),
            ProviderType.ANTHROPIC
        )

        assertEquals(TokenUsage(promptTokens = 84, completionTokens = 27, totalTokens = 111), mergeTokenUsage(input, output!!))
    }

    @Test
    fun readsGeminiUsageMetadata() {
        val usage = parseTokenUsage(
            JSONObject("""{"usageMetadata":{"promptTokenCount":20,"candidatesTokenCount":8,"totalTokenCount":28}}"""),
            ProviderType.GEMINI
        )

        assertEquals(TokenUsage(promptTokens = 20, completionTokens = 8, totalTokens = 28), usage)
    }

    @Test
    fun ignoresAbsentOrEmptyUsage() {
        assertNull(parseTokenUsage(JSONObject("{}"), ProviderType.OPENAI))
        assertNull(parseTokenUsage(JSONObject("""{"usage":{"prompt_tokens":0,"completion_tokens":0}}"""), ProviderType.OPENAI))
    }
}
