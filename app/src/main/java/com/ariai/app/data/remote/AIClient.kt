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
            val isStream = shouldUseStreaming(provider)
            val body = buildRequestBody(provider, messages, modelId, systemPrompt, temperature, searchContext, isStream)

            val requestBuilder = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream, application/json")

            // Auth headers
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
                ProviderType.GEMINI -> {
                    // key in URL
                }
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
                // Try to parse error JSON for better message
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
                // Gemini - handle both streaming and non-streaming
                val raw = responseBody.string()
                try {
                    // Try streaming format first (multiple JSON objects)
                    if (raw.contains("\"candidates\"")) {
                        // Could be single JSON or multiple
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
                            // Try single JSON
                            val json = JSONObject(raw)
                            val text = parseGeminiResponse(json)
                            if (text.isNotEmpty()) emit(text)
                        }
                    } else {
                        emit(raw)
                    }
                } catch (e: Exception) {
                    // Fallback: emit raw
                    if (raw.isNotBlank()) emit(raw) else throw e
                }
            } else {
                // OpenAI-compatible
                val contentType = response.header("Content-Type") ?: ""
                if (contentType.contains("text/event-stream") || isStream) {
                    // Streaming
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
                                // Check for finish reason
                                val choices = json.optJSONArray("choices")
                                if (choices != null && choices.length() > 0) {
                                    val first = choices.getJSONObject(0)
                                    val finish = first.optString("finish_reason")
                                    if (finish.isNotBlank() && finish != "null") {
                                        // Stream finished
                                    }
                                }
                            } catch (e: Exception) {
                                continue
                            }
                        } else if (line.trim().startsWith("{") && line.contains("\"choices\"")) {
                            // Some providers send JSON directly without data: prefix
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
                        // No streaming data, try non-streaming parse
                        throw Exception("No streaming data received, trying fallback")
                    }
                } else {
                    // Non-streaming JSON
                    val raw = responseBody.string()
                    try {
                        val json = JSONObject(raw)
                        val text = parseOpenAINonStreaming(json)
                        if (text.isNotBlank()) emit(text) else emit(raw.take(2000))
                    } catch (e: Exception) {
                        // If not JSON, emit raw
                        if (raw.isNotBlank()) emit(raw.take(4000))
                        else throw Exception("Failed to parse response: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            // If streaming fails, try non-streaming fallback for free providers
            if (isFreeProvider(provider) && e.message?.contains("No streaming data") == true) {
                try {
                    val fallbackText = chatCompletionNonStream(provider, messages, modelId, systemPrompt, temperature, searchContext)
                    if (fallbackText.isNotBlank()) {
                        emit(fallbackText)
                        return@flow
                    }
                } catch (e2: Exception) {
                    throw Exception("Stream failed and fallback also failed: ${e2.message}. Original: ${e.message}")
                }
            }
            throw e
        }
    }

    private suspend fun chatCompletionNonStream(
        provider: Provider,
        messages: List<ChatMessage>,
        modelId: String,
        systemPrompt: String?,
        temperature: Float,
        searchContext: String?
    ): String {
        val url = buildChatUrl(provider, modelId, forceNonStream = true)
        val body = buildRequestBody(provider, messages, modelId, systemPrompt, temperature, searchContext, false)

        val builder = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")

        if (provider.apiKey.isNotBlank()) {
            builder.addHeader("Authorization", "Bearer ${provider.apiKey}")
        }

        val response = client.newCall(builder.build()).execute()
        if (!response.isSuccessful) {
            throw Exception("Fallback failed: ${response.body?.string()?.take(300)}")
        }
        val raw = response.body?.string() ?: ""
        return try {
            val json = JSONObject(raw)
            parseOpenAINonStreaming(json).ifBlank { raw.take(4000) }
        } catch (e: Exception) {
            raw.take(4000)
        }
    }

    suspend fun generateImage(
        provider: Provider,
        request: ImageGenRequest
    ): ImageGenResponse {
        return try {
            // Pollinations free image gen
            if (provider.baseUrl.contains("pollinations")) {
                // Pollinations image: https://image.pollinations.ai/prompt/{prompt}
                val encodedPrompt = java.net.URLEncoder.encode(request.prompt, "UTF-8")
                val imageUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=${request.size.split("x").firstOrNull() ?: "1024"}&height=${request.size.split("x").lastOrNull() ?: "1024"}&model=flux&nologo=true"
                return ImageGenResponse(
                    images = listOf(GeneratedImage(url = imageUrl, revisedPrompt = request.prompt)),
                    model = "pollinations-flux"
                )
            }

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
                // Fallback to Pollinations
                val encodedPrompt = java.net.URLEncoder.encode(request.prompt, "UTF-8")
                val imageUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=1024&height=1024&model=flux&nologo=true"
                return ImageGenResponse(
                    images = listOf(GeneratedImage(url = imageUrl, revisedPrompt = request.prompt)),
                    model = "pollinations-fallback"
                )
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
            if (images.isEmpty()) {
                // Fallback
                val encodedPrompt = java.net.URLEncoder.encode(request.prompt, "UTF-8")
                val imageUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=1024&height=1024&model=flux&nologo=true"
                images.add(GeneratedImage(url = imageUrl, revisedPrompt = request.prompt))
            }
            ImageGenResponse(images, request.model)
        } catch (e: Exception) {
            // Ultimate fallback - Pollinations
            val encodedPrompt = java.net.URLEncoder.encode(request.prompt, "UTF-8")
            val imageUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=1024&height=1024&model=flux&nologo=true"
            ImageGenResponse(
                images = listOf(GeneratedImage(url = imageUrl, revisedPrompt = request.prompt)),
                model = "pollinations-emergency"
            )
        }
    }

    suspend fun listModels(provider: Provider): List<AIModel> {
        return try {
            if (isFreeProvider(provider)) {
                return getDefaultModelsForType(provider.type, provider.id)
            }
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
        
        // Free providers special handling
        if (base.contains("pollinations.ai")) {
            // Pollinations OpenAI compatible: https://text.pollinations.ai/openai
            // It already is the full endpoint, don't append /chat/completions
            return if (base.endsWith("/openai")) base else "$base/openai"
        }
        
        if (base.contains("llm7.io")) {
            return "$base/chat/completions"
        }

        return when (provider.type) {
            ProviderType.GEMINI -> {
                // Use provided model or default
                val model = if (modelId.isNotBlank() && !modelId.contains("/")) modelId else "gemini-1.5-flash"
                if (forceNonStream) {
                    "$base/models/$model:generateContent?key=${provider.apiKey}"
                } else {
                    "$base/models/$model:streamGenerateContent?alt=sse&key=${provider.apiKey}"
                }
            }
            ProviderType.ANTHROPIC -> "$base/v1/messages"
            else -> {
                // Ensure /v1 is present for OpenAI compatible
                if (base.contains("/v1")) {
                    "$base/chat/completions"
                } else if (base.contains("groq.com") || base.contains("cerebras") || base.contains("openrouter")) {
                    "$base/chat/completions"
                } else {
                    "$base/chat/completions"
                }
            }
        }
    }

    private fun shouldUseStreaming(provider: Provider): Boolean {
        // Some free providers don't support streaming well
        if (provider.baseUrl.contains("pollinations")) return false
        return true
    }

    private fun isFreeProvider(provider: Provider): Boolean {
        return provider.baseUrl.contains("pollinations") || 
               provider.baseUrl.contains("llm7") ||
               provider.id.contains("demo") ||
               provider.apiKey.isBlank()
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
                    put("temperature", temperature)
                    put("maxOutputTokens", 8192)
                })
            }
            ProviderType.ANTHROPIC -> {
                json.put("model", if (modelId.isNotBlank()) modelId else "claude-3-5-sonnet-20241022")
                json.put("max_tokens", 4096)
                json.put("temperature", temperature)
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
                // OpenAI compatible - handle free providers
                val actualModel = when {
                    provider.baseUrl.contains("pollinations") -> if (modelId.isBlank()) "openai" else modelId
                    provider.baseUrl.contains("groq") -> if (modelId.isBlank()) "llama-3.3-70b-versatile" else modelId
                    provider.baseUrl.contains("openrouter") -> if (modelId.isBlank()) "meta-llama/llama-3.2-3b-instruct:free" else modelId
                    else -> if (modelId.isBlank()) "gpt-4o-mini" else modelId
                }
                
                json.put("model", actualModel)
                json.put("temperature", temperature.coerceIn(0f, 2f))
                json.put("stream", isStream)
                if (!provider.baseUrl.contains("pollinations")) {
                    json.put("max_tokens", 2048)
                }
                
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
                    // Skip empty messages
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
