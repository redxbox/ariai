package com.ariai.app.data.remote

import com.ariai.app.data.models.ProviderType
import com.ariai.app.data.models.TokenUsage
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentStepStreamAccumulatorTest {
    @Test
    fun `accumulates OpenAI content tool fragments and usage`() {
        val accumulator = AgentStepStreamAccumulator(ProviderType.OPENAI_COMPATIBLE)
        accumulator.consume(JSONObject("""{"choices":[{"delta":{"content":"Checking "}}]}"""))
        accumulator.consume(openAiToolChunk("call_", "search_", "{\"query\":"))
        accumulator.consume(openAiToolChunk("1", "web", "\"weather\"}"))
        accumulator.consume(JSONObject("""{"choices":[],"usage":{"prompt_tokens":12,"completion_tokens":4,"total_tokens":16}}"""))

        val step = accumulator.finish()
        assertEquals("Checking ", step.text)
        assertEquals("call_1", step.toolCalls.single().id)
        assertEquals("search_web", step.toolCalls.single().name)
        assertEquals("{\"query\":\"weather\"}", step.toolCalls.single().arguments)
        assertEquals(TokenUsage(12, 4, 16), step.usage)
    }

    @Test
    fun `accumulates Anthropic text tool JSON fragments and split usage`() {
        val accumulator = AgentStepStreamAccumulator(ProviderType.ANTHROPIC)
        accumulator.consume(JSONObject("""{"type":"message_start","message":{"usage":{"input_tokens":8,"output_tokens":0}}}"""), "message_start")
        accumulator.consume(JSONObject("""{"type":"content_block_start","index":0,"content_block":{"type":"tool_use","id":"toolu_1","name":"search_web","input":{}}}"""), "content_block_start")
        accumulator.consume(JSONObject()
            .put("type", "content_block_delta")
            .put("index", 0)
            .put("delta", JSONObject().put("type", "input_json_delta").put("partial_json", "{\"query\":\"news\"}")),
            "content_block_delta")
        accumulator.consume(JSONObject("""{"type":"content_block_delta","index":1,"delta":{"type":"text_delta","text":"Found it."}}"""), "content_block_delta")
        accumulator.consume(JSONObject("""{"type":"message_delta","usage":{"output_tokens":5}}"""), "message_delta")

        val step = accumulator.finish()
        assertEquals("Found it.", step.text)
        assertEquals("toolu_1", step.toolCalls.single().id)
        assertEquals("search_web", step.toolCalls.single().name)
        assertEquals("{\"query\":\"news\"}", step.toolCalls.single().arguments)
        assertEquals(TokenUsage(8, 5, 13), step.usage)
    }

    @Test
    fun `accumulates Gemini text function calls and usage`() {
        val accumulator = AgentStepStreamAccumulator(ProviderType.GEMINI)
        accumulator.consume(JSONObject("""{"candidates":[{"content":{"parts":[{"text":"Searching"},{"functionCall":{"name":"search_web","args":{"query":"weather"}}}]}}]}"""))
        accumulator.consume(JSONObject("""{"usageMetadata":{"promptTokenCount":17,"candidatesTokenCount":6,"totalTokenCount":23}}"""))

        val step = accumulator.finish()
        assertEquals("Searching", step.text)
        assertEquals("search_web", step.toolCalls.single().name)
        assertEquals("{\"query\":\"weather\"}", step.toolCalls.single().arguments)
        assertEquals(TokenUsage(17, 6, 23), step.usage)
    }

    @Test
    fun `does not invent a tool call when a streamed response contains only text`() {
        val accumulator = AgentStepStreamAccumulator(ProviderType.OPENAI)
        assertEquals("Hello", accumulator.consume(JSONObject("""{"choices":[{"delta":{"content":"Hello"}}]}""")))
        val step = accumulator.finish()
        assertEquals("Hello", step.text)
        assertTrue(step.toolCalls.isEmpty())
    }

    private fun openAiToolChunk(id: String, name: String, arguments: String): JSONObject =
        JSONObject().put("choices", JSONArray().put(
            JSONObject().put("delta", JSONObject().put("tool_calls", JSONArray().put(
                JSONObject()
                    .put("index", 0)
                    .put("id", id)
                    .put("function", JSONObject().put("name", name).put("arguments", arguments))
            )))
        ))
}
