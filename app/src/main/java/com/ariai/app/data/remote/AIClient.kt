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
        .addInterceptor { chain ->
            val request = chain.request()
            // Log for debugging
            chain.proceed(request)
        }
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
        val url = buildChatUrl(provider)
        val body = buildRequestBody(provider, messages, modelId, systemPrompt, temperature, searchContext)

        val requestBuilder = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")

        // Add auth headers based on provider type
        when (provider.type) {
            ProviderType.OPENAI, ProviderType.OPENAI_COMPATIBLE, ProviderType.OLLAMA, ProviderType.CUSTOM -> {
                if (provider.apiKey.isNotBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer ${provider.apiKey}")
                }
            }
            ProviderType.ANTHROPIC -> {
                requestBuilder.addHeader("x-api-key", provider.apiKey)
                requestBuilder.addHeader("anthropic-version", "2023-06-01")
            }
            ProviderType.GEMINI -> {
                // Gemini uses query param for API key, handled in URL
            }
        }

        // Custom headers
        provider.customHeaders.forEach { (k, v) ->
            requestBuilder.addHeader(k, v)
        }

        val request = requestBuilder.build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw Exception("API Error ${response.code}: $errorBody")
        }

        val responseBody = response.body ?: throw Exception("Empty response")
        
        if (provider.type == ProviderType.GEMINI) {
            // Gemini non-streaming for simplicity, then emit
            val json = JSONObject(responseBody.string())
            val text = parseGeminiResponse(json)
            emit(text)
        } else {
            // OpenAI-compatible streaming
            val source = responseBody.source()
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val json = JSONObject(data)
                        val delta = parseOpenAIStreamChunk(json)
                        if (delta.isNotEmpty()) {
                            emit(delta)
                        }
                    } catch (e: Exception) {
                        // Skip invalid JSON lines
                        continue
                    }
                }
            }
        }
    }

    suspend fun chatCompletion(
        provider: Provider,
        messages: List<ChatMessage>,
        modelId: String,
        systemPrompt: String? = null,
        temperature: Float = 0.7f,
        searchContext: String? = null
    ): String {
        var fullResponse = ""
        chatCompletionStream(provider, messages, modelId, systemPrompt, temperature, false, searchContext)
            .collect { chunk ->
                fullResponse += chunk
            }
        return fullResponse
    }

    suspend fun generateImage(
        provider: Provider,
        request: ImageGenRequest
    ): ImageGenResponse {
        val url = when {
            provider.baseUrl.contains("openai") || provider.type == ProviderType.OPENAI -> 
                "${provider.baseUrl.trimEnd('/')}/images/generations"
            else -> "${provider.baseUrl.trimEnd('/')}/images/generations"
        }

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
            .addHeader("Authorization", "Bearer ${provider.apiKey}")
            .addHeader("Content-Type", "application/json")
            .build()

        val response = client.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            throw Exception("Image gen failed: ${response.body?.string()}")
        }

        val json = JSONObject(response.body!!.string())
        val dataArray = json.getJSONArray("data")
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
        return ImageGenResponse(images, request.model)
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
                // Gemini models format
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

    private fun buildChatUrl(provider: Provider): String {
        val base = provider.baseUrl.trimEnd('/')
        return when (provider.type) {
            ProviderType.GEMINI -> "$base/models/${"gemini-1.5-flash"}:streamGenerateContent?alt=sse&key=${provider.apiKey}"
            ProviderType.ANTHROPIC -> "$base/v1/messages"
            else -> "$base/chat/completions"
        }
    }

    private fun buildRequestBody(
        provider: Provider,
        messages: List<ChatMessage>,
        modelId: String,
        systemPrompt: String?,
        temperature: Float,
        searchContext: String?
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
                json.put("model", modelId)
                json.put("max_tokens", 8192)
                json.put("temperature", temperature)
                json.put("stream", true)
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
                json.put("model", modelId)
                json.put("temperature", temperature)
                json.put("stream", true)
                json.put("max_tokens", 4096)
                val msgs = JSONArray()
                if (systemPrompt != null) {
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

        // Merge custom body if provided
        provider.customBody?.let { custom ->
            try {
                val customJson = JSONObject(custom)
                customJson.keys().forEach { key ->
                    json.put(key, customJson.get(key))
                }
            } catch (_: Exception) {}
        }

        return json.toString().toRequestBody("application/json".toMediaType())
    }

    private fun parseOpenAIStreamChunk(json: JSONObject): String {
        return try {
            val choices = json.getJSONArray("choices")
            if (choices.length() == 0) return ""
            val first = choices.getJSONObject(0)
            val delta = first.optJSONObject("delta")
            delta?.optString("content", "") ?: first.optJSONObject("message")?.optString("content", "") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseGeminiResponse(json: JSONObject): String {
        return try {
            val candidates = json.getJSONArray("candidates")
            val first = candidates.getJSONObject(0)
            val content = first.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            parts.getJSONObject(0).getString("text")
        } catch (e: Exception) {
            ""
        }
    }
}
