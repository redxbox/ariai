package com.ariai.app.data.remote

import com.ariai.app.data.models.AIModel
import com.ariai.app.data.models.Attachment
import com.ariai.app.data.models.AttachmentType
import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import com.ariai.app.data.models.ReasoningLevel
import com.ariai.app.data.models.ToolCall
import com.ariai.app.data.serialization.ImagenInstance
import com.ariai.app.data.serialization.ImagenParameters
import com.ariai.app.data.serialization.ImagenPredictRequest
import com.ariai.app.data.serialization.OpenAiAudioSpeechRequest
import com.ariai.app.data.serialization.OpenAiImageGenerationRequest
import com.ariai.app.data.serialization.VideoGenerationRequest
import com.ariai.app.data.serialization.VeoInstance
import com.ariai.app.data.serialization.VeoPredictRequest
import com.ariai.app.data.serialization.appJson
import kotlinx.serialization.encodeToString
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AIClientProviderRequestSerializationTest {
    private val client = AIClient()

    @Test
    fun openAiChatRequestRetainsTypedMessagesToolsAndCustomBodyPolicy() {
        val provider = Provider(
            name = "OpenAI",
            type = ProviderType.OPENAI,
            baseUrl = "https://api.openai.com/v1",
            customBody = """{"temperature":0.4,"model":"ignored","messages":[],"stream":false,"tools":[],"extra":{"enabled":true}}""",
            models = listOf(
                AIModel(
                    id = "gpt-test",
                    displayName = "Test",
                    providerId = "provider",
                    customBody = """{"top_p":0.25,"model_only_setting":"kept"}"""
                )
            )
        )
        val functionTools = listOf(
            ChatFunctionTool(
                name = "lookup",
                description = "Look something up",
                parameters = JSONObject()
                    .put("type", "object")
                    .put("properties", JSONObject().put("query", JSONObject().put("type", "string")))
            ) { "found" }
        )
        val messages = listOf(
            ChatMessage(chatId = "chat", role = MessageRole.SYSTEM, content = "Stored system message"),
            ChatMessage(
                chatId = "chat",
                role = MessageRole.USER,
                content = "Inspect this image",
                attachments = listOf(
                    Attachment(
                        type = AttachmentType.IMAGE,
                        name = "photo.png",
                        base64Data = "cGhvdG8=",
                        mimeType = "image/png"
                    )
                )
            ),
            ChatMessage(
                chatId = "chat",
                role = MessageRole.ASSISTANT,
                content = "",
                toolCalls = listOf(ToolCall(id = "call-1", name = "lookup", arguments = "{\"query\":\"weather\"}"))
            ),
            ChatMessage(
                chatId = "chat",
                role = MessageRole.TOOL,
                content = "Sunny",
                toolCallId = "call-1",
                toolName = "lookup"
            )
        )

        val body = client.buildRequestBody(
            provider = provider,
            messages = messages,
            modelId = "gpt-test",
            systemPrompt = "Use tools when needed",
            temperature = 0.7f,
            topP = 0.9f,
            searchContext = "Sunny forecast",
            isStream = true,
            reasoning = ReasoningLevel.HIGH,
            functionTools = functionTools
        ).toJsonObject()

        assertEquals("gpt-test", body.getString("model"))
        assertEquals(0.4, body.getDouble("temperature"), 0.0001)
        assertEquals(0.25, body.getDouble("top_p"), 0.0001)
        assertTrue(body.getBoolean("stream"))
        assertTrue(body.getJSONObject("stream_options").getBoolean("include_usage"))
        assertEquals("high", body.getString("reasoning_effort"))
        assertEquals("auto", body.getString("tool_choice"))
        assertTrue(body.getJSONObject("extra").getBoolean("enabled"))
        assertEquals("kept", body.getString("model_only_setting"))

        val sentMessages = body.getJSONArray("messages")
        assertEquals(5, sentMessages.length())
        assertEquals("system", sentMessages.getJSONObject(0).getString("role"))
        assertEquals("Use tools when needed", sentMessages.getJSONObject(0).getString("content"))
        assertEquals("system", sentMessages.getJSONObject(1).getString("role"))
        assertTrue(sentMessages.getJSONObject(1).getString("content").contains("Sunny forecast"))
        val imageContent = sentMessages.getJSONObject(2).getJSONArray("content")
        assertEquals("text", imageContent.getJSONObject(0).getString("type"))
        assertEquals("image_url", imageContent.getJSONObject(1).getString("type"))
        assertEquals("data:image/png;base64,cGhvdG8=", imageContent.getJSONObject(1)
            .getJSONObject("image_url").getString("url"))
        val assistant = sentMessages.getJSONObject(3)
        assertTrue(assistant.isNull("content"))
        assertEquals("call-1", assistant.getJSONArray("tool_calls").getJSONObject(0).getString("id"))
        assertEquals("{\"query\":\"weather\"}", assistant.getJSONArray("tool_calls").getJSONObject(0)
            .getJSONObject("function").getString("arguments"))
        val toolResult = sentMessages.getJSONObject(4)
        assertEquals("tool", toolResult.getString("role"))
        assertEquals("call-1", toolResult.getString("tool_call_id"))
        assertEquals("lookup", toolResult.getString("name"))
        assertEquals("lookup", body.getJSONArray("tools").getJSONObject(0).getJSONObject("function").getString("name"))
    }

    @Test
    fun geminiChatRequestSerializesContentsNativeToolsAndConvertedFunctionSchemas() {
        val provider = Provider(
            name = "Gemini",
            type = ProviderType.GEMINI,
            baseUrl = "https://generativelanguage.googleapis.com/v1beta",
            models = listOf(
                AIModel(
                    id = "gemini-test",
                    displayName = "Gemini test",
                    providerId = "provider",
                    builtInTools = listOf("google_search", "url_context")
                )
            )
        )
        val functionTools = listOf(
            ChatFunctionTool(
                name = "count_items",
                description = "Count items",
                parameters = JSONObject()
                    .put("type", "object")
                    .put("additionalProperties", false)
                    .put("properties", JSONObject().put("items", JSONObject().put("type", "array")))
            ) { "2" }
        )
        val body = client.buildRequestBody(
            provider = provider,
            messages = listOf(
                ChatMessage(chatId = "chat", role = MessageRole.SYSTEM, content = "ignored"),
                ChatMessage(
                    chatId = "chat",
                    role = MessageRole.USER,
                    content = "Describe the media",
                    attachments = listOf(
                        Attachment(type = AttachmentType.IMAGE, name = "photo.png", base64Data = "aW1hZ2U=", mimeType = "image/png"),
                        Attachment(type = AttachmentType.VIDEO, name = "clip.mp4", base64Data = "dmlkZW8=", mimeType = "video/mp4")
                    )
                ),
                ChatMessage(
                    chatId = "chat",
                    role = MessageRole.ASSISTANT,
                    content = "",
                    toolCalls = listOf(ToolCall("call-2", "count_items", "{\"items\":[1,2]}"))
                ),
                ChatMessage(chatId = "chat", role = MessageRole.TOOL, content = "2", toolCallId = "call-2", toolName = "count_items")
            ),
            modelId = "gemini-test",
            systemPrompt = "Follow the instructions",
            temperature = 0.6f,
            topP = 0.8f,
            searchContext = null,
            nativeSearch = true,
            functionTools = functionTools
        ).toJsonObject()

        assertEquals("Follow the instructions", body.getJSONObject("systemInstruction").getJSONArray("parts")
            .getJSONObject(0).getString("text"))
        val contents = body.getJSONArray("contents")
        assertEquals(3, contents.length())
        assertEquals("user", contents.getJSONObject(0).getString("role"))
        val mediaParts = contents.getJSONObject(0).getJSONArray("parts")
        assertEquals(3, mediaParts.length())
        assertEquals("Describe the media", mediaParts.getJSONObject(0).getString("text"))
        assertEquals("image/png", mediaParts.getJSONObject(1).getJSONObject("inline_data").getString("mime_type"))
        assertEquals("video/mp4", mediaParts.getJSONObject(2).getJSONObject("inline_data").getString("mime_type"))
        val functionCall = contents.getJSONObject(1).getJSONArray("parts").getJSONObject(0).getJSONObject("functionCall")
        assertEquals("count_items", functionCall.getString("name"))
        assertEquals(2, functionCall.getJSONObject("args").getJSONArray("items").length())
        val functionResponse = contents.getJSONObject(2).getJSONArray("parts").getJSONObject(0).getJSONObject("functionResponse")
        assertEquals("call-2", functionResponse.getString("id"))
        assertEquals("2", functionResponse.getJSONObject("response").getString("result"))

        val tools = body.getJSONArray("tools")
        assertTrue(tools.getJSONObject(0).has("google_search"))
        assertTrue(tools.getJSONObject(1).has("url_context"))
        val declaration = tools.getJSONObject(2).getJSONArray("functionDeclarations").getJSONObject(0)
        assertEquals("OBJECT", declaration.getJSONObject("parameters").getString("type"))
        assertFalse(declaration.getJSONObject("parameters").has("additionalProperties"))
        assertEquals("ARRAY", declaration.getJSONObject("parameters").getJSONObject("properties")
            .getJSONObject("items").getString("type"))
        assertEquals(0.8, body.getJSONObject("generationConfig").getDouble("topP"), 0.0001)
    }

    @Test
    fun anthropicChatRequestSerializesThinkingAndToolResultContentBlocks() {
        val tool = ChatFunctionTool(
            name = "lookup",
            description = "Look something up",
            parameters = JSONObject().put("type", "object")
        ) { "found" }
        val body = client.buildRequestBody(
            provider = Provider(name = "Anthropic", type = ProviderType.ANTHROPIC, baseUrl = "https://api.anthropic.com/v1"),
            messages = listOf(
                ChatMessage(
                    chatId = "chat",
                    role = MessageRole.USER,
                    content = "Read this",
                    attachments = listOf(Attachment(type = AttachmentType.IMAGE, name = "photo.png", base64Data = "cGhvdG8=", mimeType = "image/png"))
                ),
                ChatMessage(chatId = "chat", role = MessageRole.ASSISTANT, content = "", toolCalls = listOf(ToolCall("call-3", "lookup", "{}"))),
                ChatMessage(chatId = "chat", role = MessageRole.TOOL, content = "result", toolCallId = "call-3"),
                ChatMessage(chatId = "chat", role = MessageRole.USER, content = "Summarize")
            ),
            modelId = "claude-test",
            systemPrompt = "Be precise",
            temperature = 0.3f,
            topP = 0.7f,
            searchContext = "A cited source",
            isStream = true,
            reasoning = ReasoningLevel.HIGH,
            nativeSearch = true,
            functionTools = listOf(tool)
        ).toJsonObject()

        assertEquals("claude-test", body.getString("model"))
        assertEquals(20_096, body.getInt("max_tokens"))
        assertEquals(16_000, body.getJSONObject("thinking").getInt("budget_tokens"))
        assertFalse(body.has("temperature"))
        assertFalse(body.has("top_p"))
        assertTrue(body.getBoolean("stream"))
        assertTrue(body.getString("system").contains("Be precise"))
        assertTrue(body.getString("system").contains("A cited source"))
        assertEquals("web_search_20250305", body.getJSONArray("tools").getJSONObject(0).getString("type"))
        assertEquals("lookup", body.getJSONArray("tools").getJSONObject(1).getString("name"))

        val messages = body.getJSONArray("messages")
        val userImage = messages.getJSONObject(0).getJSONArray("content")
        assertEquals("image", userImage.getJSONObject(0).getString("type"))
        assertEquals("text", userImage.getJSONObject(1).getString("type"))
        val assistantCall = messages.getJSONObject(1).getJSONArray("content").getJSONObject(0)
        assertEquals("tool_use", assistantCall.getString("type"))
        assertEquals("call-3", assistantCall.getString("id"))
        val toolResultMessage = messages.getJSONObject(2)
        assertEquals("user", toolResultMessage.getString("role"))
        val result = toolResultMessage.getJSONArray("content").getJSONObject(0)
        assertEquals("tool_result", result.getString("type"))
        assertEquals("call-3", result.getString("tool_use_id"))
        assertEquals("result", result.getString("content"))
    }

    @Test
    fun typedMediaRequestModelsUseProviderWireNamesAndOmitOptionalValues() {
        val image = appJson.encodeToString(
            OpenAiImageGenerationRequest.serializer(),
            OpenAiImageGenerationRequest(model = "image-model", prompt = "paint", n = 2, size = "1024x1024")
        )
        val imagen = appJson.encodeToString(
            ImagenPredictRequest.serializer(),
            ImagenPredictRequest(instances = listOf(ImagenInstance("landscape")),
                parameters = ImagenParameters(1, "16:9"))
        )
        val audio = appJson.encodeToString(
            OpenAiAudioSpeechRequest.serializer(), OpenAiAudioSpeechRequest("tts-model", "Hello", "alloy")
        )
        val veo = appJson.encodeToString(
            VeoPredictRequest.serializer(), VeoPredictRequest(listOf(VeoInstance("A film")))
        )
        val video = appJson.encodeToString(
            VideoGenerationRequest.serializer(), VideoGenerationRequest("video-model", "A film")
        )

        assertFalse(JSONObject(image).has("quality"))
        assertFalse(JSONObject(image).has("style"))
        assertEquals("16:9", JSONObject(imagen).getJSONObject("parameters").getString("aspectRatio"))
        assertEquals("Hello", JSONObject(audio).getString("input"))
        assertEquals("A film", JSONObject(veo).getJSONArray("instances").getJSONObject(0).getString("prompt"))
        assertEquals("video-model", JSONObject(video).getString("model"))
    }

    private fun okhttp3.RequestBody.toJsonObject(): JSONObject {
        val buffer = Buffer()
        writeTo(buffer)
        return JSONObject(buffer.readUtf8())
    }
}
