package com.ariai.app.data.remote

import com.ariai.app.data.models.*
import com.ariai.app.data.serialization.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.*
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import com.ariai.app.util.tx
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

// Model-name detection, client identity, token-usage parsing and stream accumulation for AIClient.

/**
 * Name-based video detection. Used together with API metadata, because many providers
 * (OpenAI, Gemini, relays) do not say which modalities a model produces.
 */
internal const val MAX_IMAGE_EDIT_FILE_BYTES = 20L * 1024L * 1024L

internal val videoModelName = Regex(
    "sora|veo|video|\\bwan[-.]?\\d|kling|seedance|hailuo|runway|luma|ray-?2|pika|\\bltx|cogvideo|hunyuan.?video|mochi|vidu|i2v|t2v",
    RegexOption.IGNORE_CASE
)

fun looksLikeVideoModel(id: String): Boolean = videoModelName.containsMatchIn(id)

internal val audioModelName = Regex("tts|speech|music|lyria|stable-audio|sound-effect|text-to-sound|elevenlabs", RegexOption.IGNORE_CASE)

fun looksLikeAudioModel(id: String): Boolean = audioModelName.containsMatchIn(id)

/**
 * Some relays only accept requests from known clients. AgentRouter rejects the default OkHttp
 * User-Agent with 401 even when the key is valid. The MIA Router model-catalog probe succeeds
 * with AriAI's explicit identity, so keep in-app requests consistent with that probe. Other hosts
 * retain OkHttp's default identity.
 */
internal fun applyProviderClientIdentity(request: Request): Request {
    val host = request.url.host
    return when {
        host == "agentrouter.org" || host.endsWith(".agentrouter.org") ->
            request.newBuilder().header("User-Agent", "opencode/1.18.31 (android; okhttp)").build()
        host == "miarouter.online" || host.endsWith(".miarouter.online") -> {
            val builder = request.newBuilder().header("User-Agent", "AriAI-Android")
            if (request.method == "GET" && request.header("Accept") == null) {
                builder.header("Accept", "application/json")
            }
            builder.build()
        }
        else -> request
    }
}

internal val clientIdentityInterceptor = Interceptor { chain ->
    chain.proceed(applyProviderClientIdentity(chain.request()))
}

internal fun parseTokenUsage(json: JSONObject, providerType: ProviderType): TokenUsage? {
    val usage = when (providerType) {
        ProviderType.GEMINI -> json.optJSONObject("usageMetadata")
            ?: json.optJSONObject("usage_metadata")
            ?: json.optJSONObject("usage")
        ProviderType.ANTHROPIC -> json.optJSONObject("usage")
            ?: json.optJSONObject("message")?.optJSONObject("usage")
        else -> json.optJSONObject("usage")
            ?: json.optJSONObject("usage_metadata")
    } ?: return null

    fun read(keys: List<String>): Int = keys.firstNotNullOfOrNull { key ->
        if (usage.has(key)) usage.optInt(key, 0).coerceAtLeast(0) else null
    } ?: 0

    val promptKeys = when (providerType) {
        ProviderType.GEMINI -> listOf("promptTokenCount", "prompt_token_count", "prompt_tokens", "input_tokens")
        ProviderType.ANTHROPIC -> listOf("input_tokens", "prompt_tokens")
        else -> listOf("prompt_tokens", "input_tokens")
    }
    val completionKeys = when (providerType) {
        ProviderType.GEMINI -> listOf("candidatesTokenCount", "candidates_token_count", "completion_tokens", "output_tokens")
        ProviderType.ANTHROPIC -> listOf("output_tokens", "completion_tokens")
        else -> listOf("completion_tokens", "output_tokens")
    }
    val totalKeys = if (providerType == ProviderType.GEMINI) {
        listOf("totalTokenCount", "total_token_count", "total_tokens")
    } else listOf("total_tokens")

    val prompt = read(promptKeys)
    val completion = read(completionKeys)
    val reportedTotal = read(totalKeys)
    if (prompt == 0 && completion == 0 && reportedTotal == 0) return null
    val total = reportedTotal.takeIf { it > 0 } ?: (prompt.toLong() + completion).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    return TokenUsage(prompt, completion, total)
}

internal fun mergeTokenUsage(current: TokenUsage?, update: TokenUsage): TokenUsage {
    val prompt = update.promptTokens.takeIf { it > 0 } ?: current?.promptTokens ?: 0
    val completion = update.completionTokens.takeIf { it > 0 } ?: current?.completionTokens ?: 0
    val sum = (prompt.toLong() + completion).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    val total = when {
        update.totalTokens > 0 && update.promptTokens > 0 && update.completionTokens > 0 -> update.totalTokens
        sum > 0 -> sum
        update.totalTokens > 0 -> update.totalTokens
        else -> current?.totalTokens ?: 0
    }
    return TokenUsage(prompt, completion, total)
}

/** Text and structured tool-call events emitted while a tool-enabled completion is in flight. */
internal sealed interface AgentStepStreamEvent {
    data class TextDelta(val text: String) : AgentStepStreamEvent
    data class Completed(val step: AgentStep) : AgentStepStreamEvent
}

/** Accumulates the three provider streaming dialects used by AriAI. */
internal class AgentStepStreamAccumulator(private val providerType: ProviderType) {
    private data class ToolCallBuilder(
        val id: StringBuilder = StringBuilder(),
        val name: StringBuilder = StringBuilder(),
        val arguments: StringBuilder = StringBuilder(),
        var structuredArguments: JSONObject? = null
    ) {
        fun appendArguments(value: Any?) {
            when (value) {
                is JSONObject -> {
                    if (value.length() > 0) {
                        val merged = structuredArguments ?: JSONObject().also { structuredArguments = it }
                        value.keys().forEach { key -> merged.put(key, value.opt(key)) }
                    }
                }
                is String -> arguments.append(value)
            }
        }

        fun argumentString(): String = when {
            arguments.isNotEmpty() -> arguments.toString()
            structuredArguments != null -> structuredArguments.toString()
            else -> "{}"
        }

        fun toToolCall(): ToolCall? {
            val callName = name.toString().takeIf { it.isNotBlank() } ?: return null
            return ToolCall(
                id = id.toString().ifBlank { java.util.UUID.randomUUID().toString() },
                name = callName,
                arguments = argumentString()
            )
        }
    }

    private val toolCalls = linkedMapOf<String, ToolCallBuilder>()
    private var accumulatedUsage: TokenUsage? = null
    val usage: TokenUsage? get() = accumulatedUsage

    /** Returns only newly received user-visible text (never tool argument fragments). */
    fun consume(json: JSONObject, eventType: String? = null): String {
        parseTokenUsage(json, providerType)?.let { accumulatedUsage = mergeTokenUsage(accumulatedUsage, it) }
        return when (providerType) {
            ProviderType.GEMINI -> consumeGemini(json)
            ProviderType.ANTHROPIC -> consumeAnthropic(json, eventType)
            else -> consumeOpenAi(json)
        }
    }

    fun finish(): AgentStep = AgentStep(
        text = text.toString(),
        toolCalls = toolCalls.values.mapNotNull { it.toToolCall() },
        usage = accumulatedUsage
    )

    private val text = StringBuilder()

    private fun consumeOpenAi(json: JSONObject): String {
        val choices = json.optJSONArray("choices") ?: return ""
        val receivedText = StringBuilder()
        for (choiceIndex in 0 until choices.length()) {
            val choice = choices.optJSONObject(choiceIndex) ?: continue
            val part = choice.optJSONObject("delta") ?: choice.optJSONObject("message") ?: continue
            val chunk = streamText(part.opt("content"))
            if (chunk.isNotEmpty()) {
                text.append(chunk)
                receivedText.append(chunk)
            }
            part.optJSONObject("function_call")?.let { call ->
                val builder = toolCalls.getOrPut("openai:0") { ToolCallBuilder() }
                builder.name.append(call.optString("name"))
                builder.arguments.append(call.optString("arguments"))
            }
            val calls = part.optJSONArray("tool_calls") ?: continue
            for (index in 0 until calls.length()) {
                val call = calls.optJSONObject(index) ?: continue
                val callIndex = call.optInt("index", index)
                val builder = toolCalls.getOrPut("openai:$callIndex") { ToolCallBuilder() }
                builder.id.append(call.optString("id"))
                val function = call.optJSONObject("function") ?: continue
                builder.name.append(function.optString("name"))
                builder.arguments.append(function.optString("arguments"))
            }
        }
        return receivedText.toString()
    }

    private fun consumeAnthropic(json: JSONObject, eventType: String?): String {
        val type = eventType?.takeIf { it.isNotBlank() } ?: json.optString("type")
        val block = json.optJSONObject("content_block")
        val delta = json.optJSONObject("delta")
        when (type) {
            "content_block_start" -> if (block?.optString("type") == "tool_use") {
                val index = json.optInt("index", toolCalls.size)
                val builder = toolCalls.getOrPut("anthropic:$index") { ToolCallBuilder() }
                builder.id.append(block.optString("id"))
                builder.name.append(block.optString("name"))
                builder.appendArguments(block.optJSONObject("input"))
            }
            "content_block_delta" -> when (delta?.optString("type")) {
                "text_delta" -> {
                    val chunk = delta.optString("text")
                    text.append(chunk)
                    return chunk
                }
                "input_json_delta" -> {
                    val index = json.optInt("index", toolCalls.size - 1)
                    val builder = toolCalls.getOrPut("anthropic:$index") { ToolCallBuilder() }
                    builder.arguments.append(delta.optString("partial_json"))
                }
            }
            "message_start", "message_delta" -> {
                val usageJson = json.optJSONObject("message")?.optJSONObject("usage") ?: json.optJSONObject("usage")
                if (usageJson != null) {
                    parseTokenUsage(JSONObject().put("usage", usageJson), providerType)?.let {
                        accumulatedUsage = mergeTokenUsage(accumulatedUsage, it)
                    }
                }
            }
        }
        return ""
    }

    private fun consumeGemini(json: JSONObject): String {
        val candidates = json.optJSONArray("candidates") ?: return ""
        val receivedText = StringBuilder()
        for (candidateIndex in 0 until candidates.length()) {
            val candidate = candidates.optJSONObject(candidateIndex) ?: continue
            val parts = candidate.optJSONObject("content")?.optJSONArray("parts") ?: continue
            for (partIndex in 0 until parts.length()) {
                val part = parts.optJSONObject(partIndex) ?: continue
                val chunk = if (part.optBoolean("thought", false)) "" else part.optString("text")
                if (chunk.isNotEmpty()) {
                    text.append(chunk)
                    receivedText.append(chunk)
                }
                val function = part.optJSONObject("functionCall") ?: part.optJSONObject("function_call") ?: continue
                val name = function.optString("name")
                val id = function.optString("id")
                val key = "gemini:$candidateIndex:$partIndex"
                val builder = toolCalls.getOrPut(key) { ToolCallBuilder() }
                if (name.isNotBlank() && builder.name.isEmpty()) builder.name.append(name)
                if (id.isNotBlank() && builder.id.isEmpty()) builder.id.append(id)
                val arguments = function.opt("args") ?: function.opt("arguments")
                when (arguments) {
                    is JSONObject -> builder.appendArguments(arguments)
                    is String -> builder.arguments.append(arguments)
                }
            }
        }
        return receivedText.toString()
    }

    private fun streamText(value: Any?): String = when (value) {
        is String -> value
        is JSONArray -> buildString {
            for (index in 0 until value.length()) {
                val item = value.optJSONObject(index) ?: continue
                if (item.optString("type") == "text") append(item.optString("text"))
            }
        }
        is JSONObject -> value.optString("text")
        else -> ""
    }
}

internal class StreamingNotSupportedException(message: String, cause: Throwable? = null) : IOException(message, cause)

internal fun decodeGeneratedImageBase64(encoded: String, maxBytes: Int = HttpBodyLimits.IMAGE_BYTES): ByteArray {
    require(maxBytes in 1..HttpBodyLimits.IMAGE_BYTES) { "Requested image limit is out of range" }
    val maxEncodedChars = ((maxBytes.toLong() + 2L) / 3L) * 4L
    if (encoded.length.toLong() > maxEncodedChars) throw IOException("Image exceeds the ${maxBytes / 1024} KB limit")
    val decoded = java.util.Base64.getMimeDecoder().decode(encoded)
    if (decoded.size > maxBytes) throw IOException("Image exceeds the ${maxBytes / 1024} KB limit")
    return decoded
}
