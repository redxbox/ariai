package com.ariai.app.data.remote

import com.ariai.app.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
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
        searchContext: String? = null,
        reasoning: ReasoningLevel = ReasoningLevel.AUTO,
        nativeSearch: Boolean = false
    ): Flow<String> = flow {
        try {
            val url = buildChatUrl(provider, modelId)
            val isStream = true
            val body = buildRequestBody(provider, messages, modelId, systemPrompt, temperature, searchContext, isStream, reasoning, nativeSearch)

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

            val modelHeaders = provider.models.find { it.id == modelId }?.headers.orEmpty()
            (provider.customHeaders + modelHeaders).forEach { (k, v) ->
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
                val code = response.code
                response.close()
                // Some gateways (e.g. OpenRouter) make images through chat completions instead.
                if (code in listOf(400, 404, 405, 422)) return generateImageViaChat(provider, request)
                throw Exception("Image gen failed: $code")
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

    /** Edits an image with an instruction (OpenAI-compatible /images/edits). */
    suspend fun editImage(
        provider: Provider,
        model: String,
        image: java.io.File,
        prompt: String
    ): ImageGenResponse = withContext(Dispatchers.IO) {
        val url = "${provider.baseUrl.trimEnd('/')}/images/edits"
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", model)
            .addFormDataPart("prompt", prompt)
            .addFormDataPart("n", "1")
            .addFormDataPart("image", image.name, image.asRequestBody("image/png".toMediaType()))
            .build()
        val request = Request.Builder()
            .url(url)
            .post(body)
            .apply {
                if (provider.apiKey.isNotBlank()) addHeader("Authorization", "Bearer ${provider.apiKey}")
            }
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw Exception("Image edit failed: HTTP ${response.code}")
            val data = JSONObject(text).optJSONArray("data") ?: JSONArray()
            val images = (0 until data.length()).map { i ->
                val obj = data.getJSONObject(i)
                GeneratedImage(
                    url = obj.optString("url", null),
                    base64 = obj.optString("b64_json", null),
                    revisedPrompt = obj.optString("revised_prompt", null)
                )
            }
            ImageGenResponse(images, model)
        }
    }

    private fun generateImageViaChat(provider: Provider, request: ImageGenRequest): ImageGenResponse {
        val url = "${provider.baseUrl.trimEnd('/')}/chat/completions"
        val body = JSONObject().apply {
            put("model", request.model)
            put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", request.prompt)))
            put("modalities", JSONArray().put("image").put("text"))
        }
        val httpRequest = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .apply { if (provider.apiKey.isNotBlank()) addHeader("Authorization", "Bearer ${provider.apiKey}") }
            .build()
        return client.newCall(httpRequest).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw Exception("Image gen failed: ${response.code}")
            val message = JSONObject(text).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
            val images = message?.optJSONArray("images") ?: JSONArray()
            val out = (0 until images.length()).mapNotNull { i ->
                val u = images.optJSONObject(i)?.optJSONObject("image_url")?.optString("url", "").orEmpty()
                when {
                    u.isBlank() -> null
                    u.startsWith("data:") -> GeneratedImage(base64 = u.substringAfter("base64,"))
                    else -> GeneratedImage(url = u)
                }
            }
            if (out.isEmpty()) throw Exception("The model returned no image")
            ImageGenResponse(out, request.model)
        }
    }

    /** Returns the bytes of a generated image, from its base64 payload or its URL. */
    suspend fun imageBytes(image: GeneratedImage): ByteArray = withContext(Dispatchers.IO) {
        val b64 = image.base64
        if (!b64.isNullOrBlank()) {
            return@withContext android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
        }
        val url = image.url ?: throw Exception("No image data returned")
        client.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) throw Exception("Image download failed: HTTP ${r.code}")
            r.body?.bytes() ?: throw Exception("Empty image response")
        }
    }

    /**
     * Lists the models the provider's API supports. Runs on the IO dispatcher, because
     * network calls on the main thread throw. Failures carry a readable message.
     */
    suspend fun listModels(provider: Provider): List<AIModel> = withContext(Dispatchers.IO) {
        val base = provider.baseUrl.trim().trimEnd('/')
        val url = when (provider.type) {
            ProviderType.GEMINI -> "$base/models?key=${provider.apiKey.trim()}"
            // Anthropic's models endpoint is /v1/models. The base URL may or may not already end in /v1.
            ProviderType.ANTHROPIC -> "${base.removeSuffix("/v1")}/v1/models"
            // OpenRouter lists only text models by default; ask for every output type.
            else -> if (base.contains("openrouter.ai")) "$base/models?output_modalities=all" else "$base/models"
        }

        val builder = Request.Builder().url(url).get()
        val key = provider.apiKey.trim()
        if (key.isNotBlank()) {
            when (provider.type) {
                ProviderType.GEMINI -> {}
                ProviderType.ANTHROPIC -> {
                    builder.addHeader("x-api-key", key)
                    builder.addHeader("anthropic-version", "2023-06-01")
                }
                else -> builder.addHeader("Authorization", "Bearer $key")
            }
        }
        provider.customHeaders.forEach { (k, v) ->
            if (k.isNotBlank() && v.isNotBlank()) builder.addHeader(k, v)
        }

        val response = client.newCall(builder.build()).execute()
        response.use { r ->
            if (!r.isSuccessful) throw IllegalStateException("HTTP ${r.code} from ${url.substringBefore('?')}")
            val json = JSONObject(r.body?.string() ?: throw IllegalStateException("Empty response"))
            val models = mutableListOf<AIModel>()

            if (provider.type == ProviderType.GEMINI) {
                val arr = json.optJSONArray("models") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val m = arr.getJSONObject(i)
                    val name = m.getString("name").removePrefix("models/")
                    // Image models are the ones that offer the "predict" method (Imagen), not by name.
                    val methods = m.optJSONArray("supportedGenerationMethods")
                    val imageGen = (0 until (methods?.length() ?: 0)).any { methods?.optString(it) == "predict" }
                    if (name.contains("gemini") || imageGen) {
                        models.add(AIModel(name, name, provider.id, true, true, imageGen, 1000000))
                    }
                }
            } else {
                val arr = json.optJSONArray("data") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val m = arr.getJSONObject(i)
                    val id = m.getString("id")
                    val arch = m.optJSONObject("architecture")
                    val inputs = modalityList(arch?.optJSONArray("input_modalities"))
                    val outputs = modalityList(arch?.optJSONArray("output_modalities"))
                    models.add(
                        AIModel(
                            id = id,
                            displayName = id,
                            providerId = provider.id,
                            supportsVision = if (inputs.isEmpty()) id.contains("vision") || id.contains("4o") else "image" in inputs,
                            supportsFunctionCalling = true,
                            supportsImageGen = "image" in outputs,
                            contextWindow = 128000,
                            supportsVideoGen = "video" in outputs,
                            outputsText = outputs.isEmpty() || "text" in outputs
                        )
                    )
                }
            }
            // OpenRouter lists video models on their own endpoint, not in /models.
            if (base.contains("openrouter.ai")) models.addAll(openRouterVideoModels(provider, base))
            if (models.isEmpty()) throw IllegalStateException("No models returned")
            models
        }
    }

    /** Video models from OpenRouter's /videos/models endpoint. Returns empty on any failure. */
    private fun openRouterVideoModels(provider: Provider, base: String): List<AIModel> {
        return try {
            val builder = Request.Builder().url("$base/videos/models").get()
            val key = provider.apiKey.trim()
            if (key.isNotBlank()) builder.addHeader("Authorization", "Bearer $key")
            client.newCall(builder.build()).execute().use { r ->
                if (!r.isSuccessful) return emptyList()
                val arr = JSONObject(r.body?.string() ?: return emptyList()).optJSONArray("data") ?: return emptyList()
                (0 until arr.length()).mapNotNull { i ->
                    val m = arr.optJSONObject(i) ?: return@mapNotNull null
                    val id = m.optString("id").ifBlank { return@mapNotNull null }
                    AIModel(
                        id = id,
                        displayName = m.optString("name").ifBlank { id },
                        providerId = provider.id,
                        supportsVision = false,
                        supportsFunctionCalling = false,
                        supportsImageGen = false,
                        contextWindow = 0,
                        supportsVideoGen = true,
                        outputsText = false
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Built-in tool names enabled for this model. Only Gemini supports them. */
    private fun modelTools(provider: Provider, modelId: String): MutableList<String> {
        if (provider.type != ProviderType.GEMINI) return mutableListOf()
        val enabled = provider.models.find { it.id == modelId }?.builtInTools.orEmpty()
        return enabled.filter { it == "google_search" || it == "url_context" }.toMutableList()
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

    /** Message text with extracted text-file contents appended. */
    private fun ChatMessage.promptText(): String {
        val files = attachments.mapNotNull { a -> a.textContent?.let { "\n\n[File: ${a.name}]\n$it" } }
        return content + files.joinToString("")
    }

    /** Images that carry base64 data and can be sent to vision models. */
    private fun ChatMessage.imageParts(): List<Attachment> =
        attachments.filter { it.type == AttachmentType.IMAGE && !it.base64Data.isNullOrBlank() }

    private fun buildRequestBody(
        provider: Provider,
        messages: List<ChatMessage>,
        modelId: String,
        systemPrompt: String?,
        temperature: Float,
        searchContext: String?,
        isStream: Boolean = true,
        reasoning: ReasoningLevel = ReasoningLevel.AUTO,
        nativeSearch: Boolean = false
    ): RequestBody {
        val json = JSONObject()
        val lastMessage = messages.lastOrNull()

        when (provider.type) {
            ProviderType.GEMINI -> {
                val contents = JSONArray()
                messages.forEach { msg ->
                    if (msg.role == MessageRole.SYSTEM) return@forEach
                    val parts = JSONArray()
                    var text = msg.promptText()
                    if (searchContext != null && msg == lastMessage) {
                        text = "Web search results:\n$searchContext\n\nUser question: $text\n\nAnswer using the search results when relevant."
                    }
                    if (text.isNotBlank()) parts.put(JSONObject().put("text", text))
                    msg.imageParts().forEach { img ->
                        parts.put(JSONObject().put("inline_data", JSONObject().put("mime_type", img.mimeType).put("data", img.base64Data)))
                    }
                    if (parts.length() == 0) return@forEach
                    contents.put(JSONObject().apply {
                        put("role", if (msg.role == MessageRole.USER) "user" else "model")
                        put("parts", parts)
                    })
                }
                json.put("contents", contents)
                if (systemPrompt != null) {
                    json.put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
                }
                json.put("generationConfig", JSONObject().apply {
                    put("temperature", temperature.toDouble())
                    reasoning.geminiBudget?.let { put("thinkingConfig", JSONObject().put("thinkingBudget", it)) }
                })
                val tools = modelTools(provider, modelId)
                if (nativeSearch && !tools.contains("google_search")) tools.add("google_search")
                if (tools.isNotEmpty()) {
                    json.put("tools", JSONArray().apply { tools.forEach { put(JSONObject().put(it, JSONObject())) } })
                }
            }
            ProviderType.ANTHROPIC -> {
                val budget = reasoning.budgetTokens
                json.put("model", if (modelId.isNotBlank()) modelId else "claude-3-5-sonnet-20241022")
                json.put("max_tokens", if (budget != null) budget + 4096 else 4096)
                json.put("stream", isStream)
                if (nativeSearch) {
                    json.put("tools", JSONArray().put(JSONObject().put("type", "web_search_20250305").put("name", "web_search")))
                }
                if (budget != null) {
                    // Extended thinking requires temperature 1 (the API default), so it is omitted here.
                    json.put("thinking", JSONObject().put("type", "enabled").put("budget_tokens", budget))
                } else {
                    json.put("temperature", temperature.toDouble())
                }
                val systemText = listOfNotNull(
                    systemPrompt,
                    searchContext?.let { "You have access to web search results:\n$it\nUse them to answer accurately with citations." }
                ).joinToString("\n\n")
                if (systemText.isNotBlank()) json.put("system", systemText)
                val msgs = JSONArray()
                messages.filter { it.role != MessageRole.SYSTEM }.forEach { m ->
                    val content = JSONArray()
                    m.imageParts().forEach { img ->
                        content.put(JSONObject().put("type", "image").put("source", JSONObject()
                            .put("type", "base64").put("media_type", img.mimeType).put("data", img.base64Data)))
                    }
                    val text = m.promptText()
                    if (text.isNotBlank()) content.put(JSONObject().put("type", "text").put("text", text))
                    if (content.length() == 0) return@forEach
                    msgs.put(JSONObject().apply {
                        put("role", if (m.role == MessageRole.USER) "user" else "assistant")
                        put("content", content)
                    })
                }
                json.put("messages", msgs)
            }
            else -> {
                val actualModel = if (modelId.isBlank()) "gpt-4o-mini" else modelId

                json.put("model", actualModel)
                json.put("temperature", temperature.toDouble().coerceIn(0.0, 2.0))
                json.put("stream", isStream)
                reasoning.effort?.let { json.put("reasoning_effort", it) }
                if (nativeSearch && provider.baseUrl.contains("openrouter.ai")) {
                    json.put("plugins", JSONArray().put(JSONObject().put("id", "web")))
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
                    val text = m.promptText()
                    val images = m.imageParts()
                    if (text.isBlank() && images.isEmpty()) return@forEach
                    val content: Any = if (m.role == MessageRole.USER && images.isNotEmpty()) {
                        JSONArray().apply {
                            put(JSONObject().put("type", "text").put("text", text))
                            images.forEach { img ->
                                put(JSONObject().put("type", "image_url").put("image_url",
                                    JSONObject().put("url", "data:${img.mimeType};base64,${img.base64Data}")))
                            }
                        }
                    } else {
                        text
                    }
                    msgs.put(JSONObject().apply {
                        put("role", when (m.role) {
                            MessageRole.USER -> "user"
                            MessageRole.ASSISTANT -> "assistant"
                            MessageRole.SYSTEM -> "system"
                            MessageRole.TOOL -> "tool"
                        })
                        put("content", content)
                    })
                }
                json.put("messages", msgs)
            }
        }

        val modelBody = provider.models.find { it.id == modelId }?.customBody
        listOfNotNull(provider.customBody, modelBody).forEach { custom ->
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
            // Anthropic streams text as content_block_delta events.
            if (json.optString("type") == "content_block_delta") {
                return json.optJSONObject("delta")?.optString("text", "") ?: ""
            }
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

    private fun modalityList(arr: JSONArray?): List<String> =
        arr?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()

    /**
     * Starts an async video job, then waits until it finishes and returns the MP4 bytes.
     * [onProgress] gets a short status line with the elapsed time, updated every second.
     */
    suspend fun generateVideo(
        provider: Provider,
        modelId: String,
        prompt: String,
        onProgress: (String) -> Unit = {}
    ): ByteArray = withContext(Dispatchers.IO) {
        val base = provider.baseUrl.trim().trimEnd('/')
        fun auth(b: Request.Builder): Request.Builder = b.apply {
            if (provider.apiKey.isNotBlank()) addHeader("Authorization", "Bearer ${provider.apiKey.trim()}")
        }
        onProgress("Sending request")
        val submitBody = JSONObject().put("model", modelId).put("prompt", prompt)
            .toString().toRequestBody("application/json".toMediaType())
        val jobId = client.newCall(auth(Request.Builder().url("$base/videos").post(submitBody)).build()).execute().use { r ->
            val text = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw IllegalStateException("Video submit failed: ${r.code} ${text.take(160)}")
            JSONObject(text).optString("id").ifBlank { throw IllegalStateException("No job id returned") }
        }

        val started = System.currentTimeMillis()
        val timeoutMs = 15 * 60_000L
        var state = "queued"
        var lastPoll = 0L
        while (true) {
            Thread.sleep(1_000)
            val now = System.currentTimeMillis()
            if (now - started > timeoutMs) throw IllegalStateException("Video timed out")
            // Poll the job every 5 seconds; the elapsed timer still ticks every second.
            if (now - lastPoll >= 5_000) {
                lastPoll = now
                val status = client.newCall(auth(Request.Builder().url("$base/videos/$jobId").get()).build()).execute().use { r ->
                    if (!r.isSuccessful) throw IllegalStateException("Video status failed: ${r.code}")
                    JSONObject(r.body?.string().orEmpty())
                }
                state = status.optString("status", state)
                if (state == "completed") break
                if (state == "failed" || state == "cancelled" || state == "expired") {
                    throw IllegalStateException(status.optString("error").ifBlank { "Video generation $state" })
                }
            }
            val label = when (state) {
                "queued", "pending" -> "In queue"
                "in_progress", "running", "processing" -> "Rendering"
                else -> state.replaceFirstChar { it.uppercaseChar() }
            }
            val secs = ((now - started) / 1000).toInt()
            onProgress("Generating video · $label · %02d:%02d".format(secs / 60, secs % 60))
        }
        client.newCall(auth(Request.Builder().url("$base/videos/$jobId/content?index=0").get()).build()).execute().use { r ->
            if (!r.isSuccessful) throw IllegalStateException("Video download failed: ${r.code}")
            r.body?.bytes() ?: throw IllegalStateException("Empty video")
        }
    }
}

/** True when the provider can search the web itself (Gemini, Anthropic, OpenRouter). */
fun supportsNativeSearch(provider: Provider): Boolean =
    provider.type == ProviderType.GEMINI || provider.type == ProviderType.ANTHROPIC || provider.baseUrl.contains("openrouter.ai")
