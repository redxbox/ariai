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

internal fun hasUnexecutedToolCalls(json: JSONObject, providerType: ProviderType): Boolean = when (providerType) {
    ProviderType.GEMINI -> {
        val candidates = json.optJSONArray("candidates")
        (0 until (candidates?.length() ?: 0)).any { candidateIndex ->
            val parts = candidates?.optJSONObject(candidateIndex)?.optJSONObject("content")?.optJSONArray("parts")
            (0 until (parts?.length() ?: 0)).any { partIndex ->
                val part = parts?.optJSONObject(partIndex)
                part?.has("functionCall") == true || part?.has("function_call") == true
            }
        }
    }
    ProviderType.ANTHROPIC -> {
        val block = json.optJSONObject("content_block")
        val blocks = json.optJSONArray("content")
        json.optString("type") == "content_block_start" && block?.optString("type") == "tool_use" ||
            (0 until (blocks?.length() ?: 0)).any { index -> blocks?.optJSONObject(index)?.optString("type") == "tool_use" }
    }
    else -> {
        val choices = json.optJSONArray("choices")
        (0 until (choices?.length() ?: 0)).any { index ->
            val choice = choices?.optJSONObject(index) ?: return@any false
            val delta = choice.optJSONObject("delta")
            val message = choice.optJSONObject("message")
            val tools = delta?.optJSONArray("tool_calls") ?: message?.optJSONArray("tool_calls")
            (tools?.length() ?: 0) > 0 || delta?.has("function_call") == true || message?.has("function_call") == true
        }
    }
}

internal fun parseOpenAIStreamChunk(json: JSONObject): String {
    return try {
        // Anthropic streams text as content_block_delta events.
        if (json.optString("type") == "content_block_delta") {
            val delta = json.optJSONObject("delta") ?: return ""
            return if (delta.optString("type") == "text_delta") delta.optString("text", "") else ""
        }
        val choices = json.optJSONArray("choices") ?: return ""
        if (choices.length() == 0) return ""
        val first = choices.optJSONObject(0) ?: return ""
        val delta = first.optJSONObject("delta")
        if (delta != null) responseText(delta.opt("content"))
        else responseText(first.optJSONObject("message")?.opt("content"))
    } catch (e: Exception) {
        ""
    }
}

internal fun parseOpenAINonStreaming(json: JSONObject): String {
    return try {
        val choices = json.optJSONArray("choices") ?: return ""
        if (choices.length() == 0) return ""
        val first = choices.optJSONObject(0) ?: return ""
        val message = first.optJSONObject("message")
        responseText(message?.opt("content")).ifBlank {
            first.optString("text", json.optString("content", ""))
        }
    } catch (e: Exception) {
        ""
    }
}

internal fun parseAnthropicResponse(json: JSONObject): String {
    val content = json.optJSONArray("content") ?: return ""
    return buildString {
        for (index in 0 until content.length()) {
            val block = content.optJSONObject(index) ?: continue
            if (block.optString("type") == "text") append(block.optString("text", ""))
        }
    }
}

internal fun parseAgentStep(json: JSONObject, providerType: ProviderType): AgentStep {
    val usage = parseTokenUsage(json, providerType)
    return when (providerType) {
    ProviderType.GEMINI -> {
        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
        val parts = candidate?.optJSONObject("content")?.optJSONArray("parts") ?: JSONArray()
        val text = StringBuilder()
        val calls = mutableListOf<ToolCall>()
        for (index in 0 until parts.length()) {
            val part = parts.optJSONObject(index) ?: continue
            if (!part.optBoolean("thought", false)) text.append(part.optString("text", ""))
            val function = part.optJSONObject("functionCall") ?: part.optJSONObject("function_call") ?: continue
            val name = function.optString("name").takeIf { it.isNotBlank() } ?: continue
            calls += ToolCall(
                id = function.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                name = name,
                arguments = function.optJSONObject("args")?.toString()
                    ?: function.optJSONObject("arguments")?.toString()
                    ?: "{}"
            )
        }
        AgentStep(text.toString(), calls, usage)
    }
    ProviderType.ANTHROPIC -> {
        val blocks = json.optJSONArray("content") ?: JSONArray()
        val text = StringBuilder()
        val calls = mutableListOf<ToolCall>()
        for (index in 0 until blocks.length()) {
            val block = blocks.optJSONObject(index) ?: continue
            when (block.optString("type")) {
                "text" -> text.append(block.optString("text", ""))
                "tool_use" -> {
                    val name = block.optString("name").takeIf { it.isNotBlank() } ?: continue
                    calls += ToolCall(
                        id = block.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                        name = name,
                        arguments = block.optJSONObject("input")?.toString() ?: "{}"
                    )
                }
            }
        }
        AgentStep(text.toString(), calls, usage)
    }
    else -> {
        val message = json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
            ?: return AgentStep("", emptyList(), usage)
        val text = responseText(message.opt("content"))
        val calls = mutableListOf<ToolCall>()
        val toolCalls = message.optJSONArray("tool_calls") ?: JSONArray()
        for (index in 0 until toolCalls.length()) {
            val item = toolCalls.optJSONObject(index) ?: continue
            val function = item.optJSONObject("function") ?: continue
            val name = function.optString("name").takeIf { it.isNotBlank() } ?: continue
            calls += ToolCall(
                id = item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                name = name,
                arguments = function.optString("arguments", "{}").ifBlank { "{}" }
            )
        }
        val legacyCall = message.optJSONObject("function_call")
        if (calls.isEmpty() && legacyCall != null) {
            val name = legacyCall.optString("name")
            if (name.isNotBlank()) calls += ToolCall(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                arguments = legacyCall.optString("arguments", "{}").ifBlank { "{}" }
            )
        }
        AgentStep(text, calls, usage)
    }
    }
}

internal fun responseText(value: Any?): String = when (value) {
    is String -> value
    is JSONArray -> buildString {
        for (index in 0 until value.length()) {
            val part = value.optJSONObject(index) ?: continue
            append(part.optString("text", ""))
        }
    }
    is JSONObject -> value.optString("text", "")
    else -> ""
}

internal fun modalityList(arr: JSONArray?): List<String> =
    arr?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
