package com.ariai.app.data.remote

import com.ariai.app.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AIClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun chatCompletionStream(
        provider: Provider,
        messages: List<ChatMessage>,
        modelId: String,
        systemPrompt: String? = null,
        temperature: Float = 0.7f,
        useSearch: Boolean = false,
        searchContext: String? = null
    ): Flow<String> = flow {
        try {
            val url = buildChatUrl(provider, modelId)
            val isStream = true
            val body = buildRequestBody(provider, messages, modelId, systemPrompt, temperature, searchContext, isStream)

            val requestBuilder = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream, application/json")

            when (provider.type) {
                ProviderType.OPENAI, ProviderType.OPENAI_COMPATIBLE, ProviderType.OLLAMA, ProviderType.CUSTOM -> {
                    if (provider.apiKey.isNotBlank()) {
                        requestBuilder.addHeader("Authorization", "Bearer ${provider.apiKey}")
                    }
                }
                ProviderType.ANTHROPIC -> {
                    if (provider.apiKey.isNotBlank()) {
                        requestBuilder.addHeader("x-api-key", provider.apiKey)
                        requestBuilder.addHeader("anthropic-version", "2023-06-01")
                    }
                }
                ProviderType.GEMINI -> {}
            }

            provider.customHeaders.forEach { (k, v) ->
                if (k.isNotBlank() && v.isNotBlank()) {
                    requestBuilder.addHeader(k, v)
                }
            }

            val request = requestBuilder.build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                val errorMsg = try {
                    val json = JSONObject(errorBody)
                    json.optString("error", json.optJSONObject("error")?.optString("message", errorBody) ?: errorBody)
                } catch (e: Exception) {
                    errorBody.take(500)
                }
                throw Exception("API Error ${response.code}: $errorMsg")
            }

            val responseBody = response.body ?: throw Exception("Empty response")

            if (provider.type == ProviderType.GEMINI) {
                val raw = responseBody.string()
                try {
                    if (raw.contains("\"candidates\"")) {
                        val lines = raw.split("\n")
                        var fullText = ""
                        for (line in lines) {
                            val trimmed = line.trim()
                            if (trimmed.isEmpty() || trimmed == "[" || trimmed == "]" || trimmed == ",") continue
                            try {
                                val json = JSONObject(trimmed.removeSuffix(","))
                                val text = parseGeminiResponse(json)
                                if (text.isNotEmpty()) {
                                    fullText += text
                                    emit(text)
                                }
                            } catch (e: Exception) {
                                continue
                            }
                        }
                        if (fullText.isEmpty()) {
                            val json = JSONObject(raw)
                            val text = parseGeminiResponse(json)
                            if (text.isNotEmpty()) emit(text)
                        }
                    } else {
                        emit(raw)
                    }
                } catch (e: Exception) {
                    if (raw.isNotBlank()) emit(raw) else throw e
                }
            } else {
                val contentType = response.header("Content-Type") ?: ""
                if (contentType.contains("text/event-stream") || isStream) {
                    val source = responseBody.source()
                    var hasEmitted = false
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (line.startsWith("data: ")) {
                            val data = line.removePrefix("data: ").trim()
                            if (data == "[DONE]" || data.isEmpty()) {
                                if (data == "[DONE]") break
                                continue
                            }
                            try {
                                val json = JSONObject(data)
                                val delta = parseOpenAIStreamChunk(json)
                                if (delta.isNotEmpty()) {
                                    hasEmitted = true
                                    emit(delta)
                                }
                            } catch (e: Exception) {
                                continue
                            }
                        } else if (line.trim().startsWith("{") && line.contains("\"choices\"")) {
                            try {
                                val json = JSONObject(line.trim())
                                val delta = parseOpenAIStreamChunk(json)
                                if (delta.isNotEmpty()) {
                                    hasEmitted = true
                                    emit(delta)
                                }
                            } catch (e: Exception) {
                                continue
                            }
                        }
                    }
                    if (!hasEmitted) {
                        throw Exception("No streaming data received")
                    }
                } else {
                    val raw = responseBody.string()
                    try {
                        val json = JSONObject(raw)
                        val text = parseOpenAINonStreaming(json)
                        if (text.isNotBlank()) emit(text) else emit(raw.take(2000))
                    } catch (e: Exception) {
                        if (raw.isNotBlank()) emit(raw.take(4000))
                        else throw Exception("Failed to parse response: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun generateImage(
        provider: Provider,
        request: ImageGenRequest
    ): ImageGenResponse {
        return try {
            val url = "${provider.baseUrl.trimEnd('/')}/images/generations"
            val jsonBody = JSONObject().apply {
                put("model", request.model)
                put("prompt", request.prompt)
                put("n", request.n)
                put("size", request.size)
                if (request.quality.isNotBlank()) put("quality", request.quality)
                if (request.style != null) put("style", request.style)
            }

            val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val httpRequest = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .apply {
                    if (provider.apiKey.isNotBlank()) {
                        addHeader("Authorization", "Bearer ${provider.apiKey}")
                    }
                }
                .build()

            val response = client.newCall(httpRequest).execute()
            if (!response.isSuccessful) {
                throw Exception("Image gen failed: ${response.code}")
            }

            val json = JSONObject(response.body!!.string())
            val dataArray = json.optJSONArray("data") ?: JSONArray()
            val images = mutableListOf<GeneratedImage>()
            for (i in 0 until dataArray.length()) {
                val obj = dataArray.getJSONObject(i)
                images.add(
                    GeneratedImage(
                        url = obj.optString("url", null),
                        base64 = obj.optString("b64_json", null),
                        revisedPrompt = obj.optString("revised_prompt", null)
                    )
                )
            }
            ImageGenResponse(images, request.model)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun listModels(provider: Provider): List<AIModel> {
        return try {
            val url = when (provider.type) {
                ProviderType.GEMINI -> "${provider.baseUrl.trimEnd('/')}/models?key=${provider.apiKey}"
                else -> "${provider.baseUrl.trimEnd('/')}/models"
            }

            val builder = Request.Builder().url(url).get()
            if (provider.type != ProviderType.GEMINI && provider.apiKey.isNotBlank()) {
                builder.addHeader("Authorization", "Bearer ${provider.apiKey}")
            }

            val response = client.newCall(builder.build()).execute()
            if (!response.isSuccessful) return getDefaultModelsForType(provider.type, provider.id)

            val json = JSONObject(response.body!!.string())
            val models = mutableListOf<AIModel>()

            if (provider.type == ProviderType.GEMINI) {
                val arr = json.optJSONArray("models") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val m = arr.getJSONObject(i)
                    val name = m.getString("name").removePrefix("models/")
                    if (name.contains("gemini") || name.contains("imagen")) {
                        models.add(AIModel(name, name, provider.id, true, true, name.contains("imagen"), 1000000))
                    }
                }
            } else {
                val arr = json.optJSONArray("data") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val m = arr.getJSONObject(i)
                    val id = m.getString("id")
                    models.add(AIModel(id, id, provider.id, id.contains("vision") || id.contains("4o"), true, id.contains("dall-e") || id.contains("image"), 128000))
                }
            }
            if (models.isEmpty()) getDefaultModelsForType(provider.type, provider.id) else models
        } catch (e: Exception) {
            getDefaultModelsForType(provider.type, provider.id)
        }
    }

    private fun buildChatUrl(provider: Provider, modelId: String = "", forceNonStream: Boolean = false): String {
        val base = provider.baseUrl.trimEnd('/')

        return when (provider.type) {
            ProviderType.GEMINI -> {
                val model = if (modelId.isNotBlank() && !modelId.contains("/")) modelId else "gemini-1.5-flash"
                if (forceNonStream) {
                    "$base/models/$model:generateContent?key=${provider.apiKey}"
                } else {
                    "$base/models/$model:streamGenerateContent?alt=sse&key=${provider.apiKey}"
                }
            }
            ProviderType.ANTHROPIC -> "$base/v1/messages"
            else -> {
                "$base/chat/completions"
            }
        }
    }

    private fun buildRequestBody(
        provider: Provider,
        messages: List<ChatMessage>,
        modelId: String,
        systemPrompt: String?,
        temperature: Float,
        searchContext: String?,
        isStream: Boolean = true
    ): RequestBody {
        val json = JSONObject()

        when (provider.type) {
            ProviderType.GEMINI -> {
                val contents = JSONArray()
                messages.forEach { msg ->
                    if (msg.role == MessageRole.SYSTEM) return@forEach
                    val contentObj = JSONObject().apply {
                        put("role", if (msg.role == MessageRole.USER) "user" else "model")
                        val parts = JSONArray()
                        var text = msg.content
                        if (searchContext != null && msg == messages.lastOrNull()) {
                            text = "Web search results:\n$searchContext\n\nUser question: $text\n\nAnswer using the search results when relevant."
                        }
                        parts.put(JSONObject().put("text", text))
                        put("parts", parts)
                    }
                    contents.put(contentObj)
                }
                json.put("contents", contents)
                if (systemPrompt != null) {
                    json.put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
                }
                json.put("generationConfig", JSONObject().apply {
                    put("temperature", temperature.toDouble())
                    put("maxOutputTokens", 8192)
                })
            }
            ProviderType.ANTHROPIC -> {
                json.put("model", if (modelId.isNotBlank()) modelId else "claude-3-5-sonnet-20241022")
                json.put("max_tokens", 4096)
                json.put("temperature", temperature.toDouble())
                json.put("stream", isStream)
                if (systemPrompt != null) json.put("system", systemPrompt)
                val msgs = JSONArray()
                messages.filter { it.role != MessageRole.SYSTEM }.forEach { m ->
                    msgs.put(JSONObject().apply {
                        put("role", if (m.role == MessageRole.USER) "user" else "assistant")
                        put("content", m.content)
                    })
                }
                json.put("messages", msgs)
            }
            else -> {
                val actualModel = if (modelId.isBlank()) "gpt-4o-mini" else modelId
                
                json.put("model", actualModel)
                json.put("temperature", temperature.toDouble().coerceIn(0.0, 2.0))
                json.put("stream", isStream)
                json.put("max_tokens", 2048)
                
                val msgs = JSONArray()
                if (systemPrompt != null && systemPrompt.isNotBlank()) {
                    msgs.put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                }
                if (searchContext != null) {
                    msgs.put(JSONObject().apply {
                        put("role", "system")
                        put("content", "You have access to web search results:\n$searchContext\nUse them to answer accurately with citations.")
                    })
                }
                messages.forEach { m ->
                    if (m.role == MessageRole.SYSTEM && systemPrompt != null) return@forEach
                    if (m.content.isBlank()) return@forEach
                    msgs.put(JSONObject().apply {
                        put("role", when (m.role) {
                            MessageRole.USER -> "user"
                            MessageRole.ASSISTANT -> "assistant"
                            MessageRole.SYSTEM -> "system"
                            MessageRole.TOOL -> "tool"
                        })
                        put("content", m.content)
                    })
                }
                json.put("messages", msgs)
            }
        }

        provider.customBody?.let { custom ->
            try {
                val customJson = JSONObject(custom)
                customJson.keys().forEach { key ->
                    if (key != "model" && key != "messages" && key != "stream") {
                        json.put(key, customJson.get(key))
                    }
                }
            } catch (_: Exception) {}
        }

        return json.toString().toRequestBody("application/json".toMediaType())
    }

    private fun parseOpenAIStreamChunk(json: JSONObject): String {
        return try {
            val choices = json.optJSONArray("choices") ?: return ""
            if (choices.length() == 0) return ""
            val first = choices.getJSONObject(0)
            val delta = first.optJSONObject("delta")
            if (delta != null) {
                delta.optString("content", "")
            } else {
                first.optJSONObject("message")?.optString("content", "") ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseOpenAINonStreaming(json: JSONObject): String {
        return try {
            val choices = json.optJSONArray("choices") ?: return ""
            if (choices.length() == 0) return ""
            val first = choices.getJSONObject(0)
            first.optJSONObject("message")?.optString("content", "") 
                ?: first.optString("text", "")
                ?: json.optString("content", "")
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseGeminiResponse(json: JSONObject): String {
        return try {
            val candidates = json.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val first = candidates.getJSONObject(0)
            val content = first.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            if (parts.length() == 0) return ""
            parts.getJSONObject(0).optString("text", "")
        } catch (e: Exception) {
            ""
        }
    }
}
