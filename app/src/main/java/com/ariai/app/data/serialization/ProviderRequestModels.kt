package com.ariai.app.data.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Typed request envelopes for the provider wire formats used by AIClient. */
@Serializable
internal data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiChatMessage>,
    val temperature: Double? = null,
    @SerialName("top_p") val topP: Double? = null,
    val stream: Boolean? = null,
    @SerialName("stream_options") val streamOptions: OpenAiStreamOptions? = null,
    @SerialName("reasoning_effort") val reasoningEffort: String? = null,
    val plugins: List<OpenAiPlugin>? = null,
    val tools: List<OpenAiTool>? = null,
    @SerialName("tool_choice") val toolChoice: String? = null,
    val modalities: List<String>? = null
)

@Serializable
internal data class OpenAiChatMessage(
    val role: String,
    val content: JsonElement,
    val name: String? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    @SerialName("tool_calls") val toolCalls: List<OpenAiToolCall>? = null
)

@Serializable
internal data class OpenAiTextContentPart(
    val type: String = "text",
    val text: String
)

@Serializable
internal data class OpenAiImageContentPart(
    val type: String = "image_url",
    @SerialName("image_url") val imageUrl: OpenAiImageUrl
)

@Serializable
internal data class OpenAiImageUrl(val url: String)

@Serializable
internal data class OpenAiToolCall(
    val id: String,
    val type: String = "function",
    val function: OpenAiToolCallFunction
)

@Serializable
internal data class OpenAiToolCallFunction(
    val name: String,
    val arguments: String
)

@Serializable
internal data class OpenAiTool(
    val type: String = "function",
    val function: OpenAiFunctionDefinition
)

@Serializable
internal data class OpenAiFunctionDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject
)

@Serializable
internal data class OpenAiStreamOptions(
    @SerialName("include_usage") val includeUsage: Boolean
)

@Serializable
internal data class OpenAiPlugin(val id: String)

@Serializable
internal data class GeminiGenerateContentRequest(
    val contents: List<GeminiContent>,
    @SerialName("systemInstruction") val systemInstruction: GeminiSystemInstruction? = null,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig,
    val tools: List<GeminiTool>? = null
)

@Serializable
internal data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>
)

@Serializable
internal data class GeminiPart(
    val text: String? = null,
    @SerialName("inline_data") val inlineData: GeminiInlineData? = null,
    @SerialName("functionResponse") val functionResponse: GeminiFunctionResponse? = null,
    @SerialName("functionCall") val functionCall: GeminiFunctionCall? = null
)

@Serializable
internal data class GeminiInlineData(
    @SerialName("mime_type") val mimeType: String,
    val data: String
)

@Serializable
internal data class GeminiFunctionResponse(
    val name: String,
    val response: JsonObject,
    val id: String? = null
)

@Serializable
internal data class GeminiFunctionCall(
    val name: String,
    val args: JsonObject,
    val id: String? = null
)

@Serializable
internal data class GeminiSystemInstruction(val parts: List<GeminiPart>)

@Serializable
internal data class GeminiGenerationConfig(
    val temperature: Double,
    val topP: Double,
    @SerialName("thinkingConfig") val thinkingConfig: GeminiThinkingConfig? = null
)

@Serializable
internal data class GeminiThinkingConfig(val thinkingBudget: Int)

@Serializable
internal data class GeminiTool(
    @SerialName("google_search") val googleSearch: JsonObject? = null,
    @SerialName("url_context") val urlContext: JsonObject? = null,
    @SerialName("functionDeclarations") val functionDeclarations: List<GeminiFunctionDeclaration>? = null
)

@Serializable
internal data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: JsonObject
)

@Serializable
internal data class AnthropicMessagesRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val stream: Boolean,
    val tools: List<AnthropicTool>? = null,
    val thinking: AnthropicThinking? = null,
    val temperature: Double? = null,
    @SerialName("top_p") val topP: Double? = null,
    val system: String? = null,
    val messages: List<AnthropicMessage>
)

@Serializable
internal data class AnthropicTool(
    val name: String,
    val type: String? = null,
    val description: String? = null,
    @SerialName("input_schema") val inputSchema: JsonObject? = null
)

@Serializable
internal data class AnthropicThinking(
    val type: String,
    @SerialName("budget_tokens") val budgetTokens: Int
)

@Serializable
internal data class AnthropicMessage(
    val role: String,
    val content: List<AnthropicContentBlock>
)

/** Optional protocol members are absent on wire unless used by the selected block type. */
@Serializable
internal data class AnthropicContentBlock(
    val type: String,
    val text: String? = null,
    val source: AnthropicImageSource? = null,
    val id: String? = null,
    val name: String? = null,
    val input: JsonObject? = null,
    @SerialName("tool_use_id") val toolUseId: String? = null,
    val content: String? = null
)

@Serializable
internal data class AnthropicImageSource(
    val type: String,
    @SerialName("media_type") val mediaType: String,
    val data: String
)

@Serializable
internal data class OpenAiImageGenerationRequest(
    val model: String,
    val prompt: String,
    val n: Int,
    val size: String,
    val quality: String? = null,
    val style: String? = null
)

@Serializable
internal data class ImagenPredictRequest(
    val instances: List<ImagenInstance>,
    val parameters: ImagenParameters
)

@Serializable
internal data class ImagenInstance(val prompt: String)

@Serializable
internal data class ImagenParameters(
    val sampleCount: Int,
    val aspectRatio: String
)

@Serializable
internal data class OpenAiAudioSpeechRequest(
    val model: String,
    val input: String,
    val voice: String
)

@Serializable
internal data class VeoPredictRequest(val instances: List<VeoInstance>)

@Serializable
internal data class VeoInstance(val prompt: String)

@Serializable
internal data class VideoGenerationRequest(
    val model: String,
    val prompt: String
)
