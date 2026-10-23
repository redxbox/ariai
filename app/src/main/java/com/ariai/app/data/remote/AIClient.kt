package com.ariai.app.data.remote

import com.ariai.app.data.models.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
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

/**
 * Name-based video detection. Used together with API metadata, because many providers
 * (OpenAI, Gemini, relays) do not say which modalities a model produces.
 */
private val videoModelName = Regex(
    "sora|veo|video|\\bwan[-.]?\\d|kling|seedance|hailuo|runway|luma|ray-?2|pika|\\bltx|cogvideo|hunyuan.?video|mochi|vidu|i2v|t2v",
    RegexOption.IGNORE_CASE
)

fun looksLikeVideoModel(id: String): Boolean = videoModelName.containsMatchIn(id)

private val audioModelName = Regex("tts|speech|music|lyria|stable-audio|sound-effect|text-to-sound|elevenlabs", RegexOption.IGNORE_CASE)

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

private val clientIdentityInterceptor = Interceptor { chain ->
    chain.proceed(applyProviderClientIdentity(chain.request()))
}

class AIClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addNetworkInterceptor(clientIdentityInterceptor)
        .build()

    /** Applies provider auth, then custom headers so explicitly configured headers can override defaults. */
    private fun Request.Builder.addProviderHeaders(provider: Provider, modelId: String? = null): Request.Builder {
        val key = provider.apiKey.trim()
        when (provider.type) {
            ProviderType.GEMINI -> if (key.isNotBlank()) header("x-goog-api-key", key)
            ProviderType.ANTHROPIC -> {
                if (key.isNotBlank()) header("x-api-key", key)
                header("anthropic-version", "2023-06-01")
            }
            else -> if (key.isNotBlank()) header("Authorization", "Bearer $key")
        }
        val modelHeaders = provider.models.find { it.id == modelId }?.headers.orEmpty()
        (provider.customHeaders + modelHeaders).forEach { (name, value) ->
            if (name.isNotBlank() && value.isNotBlank()) header(name, value)
        }
        return this
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun chatCompletionStream(
        provider: Provider,
        messages: List<ChatMessage>,
        modelId: String,
        systemPrompt: String? = null,
        temperature: Float = 0.7f,
        searchContext: String? = null,
        reasoning: ReasoningLevel = ReasoningLevel.AUTO,
        nativeSearch: Boolean = false,
        stream: Boolean = true,
        allowUrlContext: Boolean = true
    ): Flow<String> = if (stream) callbackFlow {
        val producer = this
        val url = buildChatUrl(provider, modelId)
        val body = buildRequestBody(
            provider = provider,
            messages = messages,
            modelId = modelId,
            systemPrompt = systemPrompt,
            temperature = temperature,
            searchContext = searchContext,
            isStream = true,
            reasoning = reasoning,
            nativeSearch = nativeSearch,
            allowUrlContext = allowUrlContext
        )
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream, application/json")
            .addProviderHeaders(provider, modelId)
            .build()

        val eventSource = EventSources.createFactory(client).newEventSource(
            request,
            object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: Response) {
                    if (!response.isSuccessful) {
                        producer.close(IOException("API Error ${response.code}: ${response.message}"))
                        eventSource.cancel()
                    }
                }

                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    if (data.trim() == "[DONE]") {
                        producer.close()
                        eventSource.cancel()
                        return
                    }
                    try {
                        val json = JSONObject(data)
                        if (json.optString("type") == "error" || json.has("error")) {
                            val error = json.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                                ?: json.optString("error", "Provider returned an error")
                            producer.close(IOException(error))
                            eventSource.cancel()
                            return
                        }
                        if (hasUnexecutedToolCalls(json, provider.type)) {
                            producer.close(IOException("The provider requested a tool call that AriAI cannot execute yet. Disable custom tools or use built-in search."))
                            eventSource.cancel()
                            return
                        }
                        val text = if (provider.type == ProviderType.GEMINI) {
                            parseGeminiResponse(json)
                        } else {
                            parseOpenAIStreamChunk(json)
                        }
                        if (text.isNotEmpty()) {
                            val result = producer.trySend(text)
                            if (result.isFailure) eventSource.cancel()
                        }
                    } catch (e: Exception) {
                        // Ignore non-JSON keep-alive events; real provider errors are handled above.
                        com.ariai.app.util.AppLog.warn(e, "SSE event parse")
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    producer.close()
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                    val responseError = runCatching {
                        response?.body?.readUtf8AtMost(HttpBodyLimits.ERROR_BYTES)?.take(500).orEmpty()
                    }.getOrDefault("")
                    val message = when {
                        response != null && !response.isSuccessful -> "API Error ${response.code}: ${responseError.ifBlank { response.message }}"
                        !responseError.isBlank() -> responseError
                        else -> t?.message ?: "Streaming request failed"
                    }
                    response?.close()
                    eventSource.cancel()
                    producer.close(IOException(message, t))
                }
            }
        )

        awaitClose { eventSource.cancel() }
    } else flow {
        emit(requestNonStreaming(provider, messages, modelId, systemPrompt, temperature, searchContext, reasoning, nativeSearch, allowUrlContext))
    }

    private suspend fun requestNonStreaming(
        provider: Provider,
        messages: List<ChatMessage>,
        modelId: String,
        systemPrompt: String?,
        temperature: Float,
        searchContext: String?,
        reasoning: ReasoningLevel,
        nativeSearch: Boolean,
        allowUrlContext: Boolean
    ): String {
        val request = Request.Builder()
            .url(buildChatUrl(provider, modelId, forceNonStream = true))
            .post(buildRequestBody(
                provider = provider,
                messages = messages,
                modelId = modelId,
                systemPrompt = systemPrompt,
                temperature = temperature,
                searchContext = searchContext,
                isStream = false,
                reasoning = reasoning,
                nativeSearch = nativeSearch,
                allowUrlContext = allowUrlContext
            ))
            .header("Content-Type", "application/json")
            .addProviderHeaders(provider, modelId)
            .build()

        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching {
                        response.use { received ->
                            val raw = received.body?.readUtf8AtMost(HttpBodyLimits.JSON_BYTES).orEmpty()
                            if (!received.isSuccessful) {
                                throw IOException("API Error ${received.code}: ${raw.ifBlank { received.message }.take(500)}")
                            }
                            if (raw.isBlank()) throw IOException("Empty response from API")
                            val json = JSONObject(raw)
                            if (json.has("error")) {
                                val error = json.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                                    ?: json.optString("error", "Provider returned an error")
                                throw IOException(error)
                            }
                            if (hasUnexecutedToolCalls(json, provider.type)) {
                                throw IOException("The provider requested a tool call that AriAI cannot execute yet. Disable custom tools or use built-in search.")
                            }
                            val text = when (provider.type) {
                                ProviderType.GEMINI -> parseGeminiResponse(json)
                                ProviderType.ANTHROPIC -> parseAnthropicResponse(json)
                                else -> parseOpenAINonStreaming(json)
                            }
                            if (text.isBlank()) throw IOException("The provider returned no text response")
                            text
                        }
                    }
                    if (continuation.isActive) continuation.resumeWith(result)
                }
            })
        }
    }

    /** Requests one non-streaming model step with executable function tools attached. */
    suspend fun chatCompletionStep(
        provider: Provider,
        messages: List<ChatMessage>,
        modelId: String,
        systemPrompt: String?,
        tools: List<ChatFunctionTool>,
        temperature: Float = 0.7f,
        searchContext: String? = null,
        reasoning: ReasoningLevel = ReasoningLevel.AUTO,
        nativeSearch: Boolean = false,
        allowUrlContext: Boolean = true
    ): AgentStep {
        val request = Request.Builder()
            .url(buildChatUrl(provider, modelId, forceNonStream = true))
            .post(buildRequestBody(
                provider = provider,
                messages = messages,
                modelId = modelId,
                systemPrompt = systemPrompt,
                temperature = temperature,
                searchContext = searchContext,
                isStream = false,
                reasoning = reasoning,
                nativeSearch = nativeSearch,
                functionTools = tools,
                allowUrlContext = allowUrlContext
            ))
            .header("Content-Type", "application/json")
            .addProviderHeaders(provider, modelId)
            .build()

        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching {
                        response.use { received ->
                            val raw = received.body?.readUtf8AtMost(HttpBodyLimits.JSON_BYTES).orEmpty()
                            if (!received.isSuccessful) {
                                throw IOException("API Error ${received.code}: ${raw.ifBlank { received.message }.take(500)}")
                            }
                            if (raw.isBlank()) throw IOException("Empty response from API")
                            val json = JSONObject(raw)
                            if (json.has("error")) {
                                val error = json.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                                    ?: json.optString("error", "Provider returned an error")
                                throw IOException(error)
                            }
                            val step = parseAgentStep(json, provider.type)
                            if (step.text.isBlank() && step.toolCalls.isEmpty()) {
                                throw IOException("The provider returned neither a reply nor a tool call")
                            }
                            step
                        }
                    }
                    if (continuation.isActive) continuation.resumeWith(result)
                }
            })
        }
    }

    suspend fun generateImage(
        provider: Provider,
        request: ImageGenRequest
    ): ImageGenResponse = withContext(Dispatchers.IO) {
        if (provider.type == ProviderType.GEMINI) {
            return@withContext generateGeminiImage(provider, request)
        }

        val url = "${provider.baseUrl.trimEnd('/')}/images/generations"
        val jsonBody = JSONObject().apply {
            put("model", request.model)
            put("prompt", request.prompt)
            put("n", request.n.coerceIn(1, 4))
            put("size", request.size)
            if (request.quality.isNotBlank()) put("quality", request.quality)
            if (request.style != null) put("style", request.style)
        }
        val httpRequest = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .addProviderHeaders(provider, request.model)
            .build()

        client.executeCancellable(httpRequest).use { response ->
            val raw = response.body?.readUtf8AtMost(HttpBodyLimits.IMAGE_JSON_BYTES).orEmpty()
            if (!response.isSuccessful) {
                // Some OpenAI-compatible gateways return image output through chat completions.
                if (response.code in listOf(400, 404, 405, 422) && provider.type != ProviderType.ANTHROPIC) {
                    return@withContext generateImageViaChat(provider, request)
                }
                throw IOException("Image generation failed: HTTP ${response.code}: ${raw.take(500)}")
            }
            val dataArray = JSONObject(raw).optJSONArray("data") ?: JSONArray()
            val images = (0 until dataArray.length()).map { i ->
                val obj = dataArray.getJSONObject(i)
                GeneratedImage(
                    url = obj.optString("url").takeIf { it.isNotBlank() },
                    base64 = obj.optString("b64_json").takeIf { it.isNotBlank() },
                    revisedPrompt = obj.optString("revised_prompt").takeIf { it.isNotBlank() }
                )
            }
            ImageGenResponse(images, request.model)
        }
    }

    /** Imagen uses Google's predict endpoint, not the OpenAI /images/generations protocol. */
    private suspend fun generateGeminiImage(provider: Provider, request: ImageGenRequest): ImageGenResponse {
        val widthHeight = request.size.lowercase().split('x').mapNotNull { it.toIntOrNull() }
        val ratio = if (widthHeight.size == 2 && widthHeight[1] != 0) {
            val value = widthHeight[0].toDouble() / widthHeight[1]
            when {
                value > 1.2 -> "16:9"
                value < 0.83 -> "9:16"
                else -> "1:1"
            }
        } else "1:1"
        val body = JSONObject()
            .put("instances", JSONArray().put(JSONObject().put("prompt", request.prompt)))
            .put("parameters", JSONObject()
                .put("sampleCount", request.n.coerceIn(1, 4))
                .put("aspectRatio", ratio))
        val url = "${provider.baseUrl.trimEnd('/')}/models/${request.model}:predict"
        val httpRequest = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .addProviderHeaders(provider, request.model)
            .build()
        return client.executeCancellable(httpRequest).use { response ->
            val raw = response.body?.readUtf8AtMost(HttpBodyLimits.IMAGE_JSON_BYTES).orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Imagen request failed: HTTP ${response.code}: ${raw.take(500)}")
            }
            val predictions = JSONObject(raw).optJSONArray("predictions") ?: JSONArray()
            val images = (0 until predictions.length()).mapNotNull { i ->
                val prediction = predictions.optJSONObject(i) ?: return@mapNotNull null
                val encoded = prediction.optString("bytesBase64Encoded").takeIf { it.isNotBlank() }
                val imageUrl = prediction.optString("url").takeIf { it.isNotBlank() }
                if (encoded == null && imageUrl == null) return@mapNotNull null
                GeneratedImage(
                    url = imageUrl,
                    base64 = encoded,
                    revisedPrompt = prediction.optString("prompt").takeIf { it.isNotBlank() }
                )
            }
            ImageGenResponse(images, request.model)
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
        val imageType = when (image.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            else -> "image/png"
        }
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", model)
            .addFormDataPart("prompt", prompt)
            .addFormDataPart("n", "1")
            .addFormDataPart("image", image.name, image.asRequestBody(imageType.toMediaType()))
            .build()
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addProviderHeaders(provider, model)
            .build()
        client.executeCancellable(request).use { response ->
            val text = response.body?.readUtf8AtMost(HttpBodyLimits.IMAGE_JSON_BYTES).orEmpty()
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

    private suspend fun generateImageViaChat(provider: Provider, request: ImageGenRequest): ImageGenResponse {
        val url = "${provider.baseUrl.trimEnd('/')}/chat/completions"
        val body = JSONObject().apply {
            put("model", request.model)
            put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", request.prompt)))
            put("modalities", JSONArray().put("image").put("text"))
        }
        val httpRequest = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .addProviderHeaders(provider, request.model)
            .build()
        return client.executeCancellable(httpRequest).use { response ->
            val text = response.body?.readUtf8AtMost(HttpBodyLimits.IMAGE_JSON_BYTES).orEmpty()
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
        client.executeCancellable(Request.Builder().url(url).build()).use { r ->
            if (!r.isSuccessful) throw Exception("Image download failed: HTTP ${r.code}")
            r.body?.readBytesAtMost(HttpBodyLimits.IMAGE_BYTES) ?: throw Exception("Empty image response")
        }
    }

    /**
     * Lists the models the provider's API supports. Runs on the IO dispatcher, because
     * network calls on the main thread throw. Failures carry a readable message.
     */
    suspend fun listModels(provider: Provider): List<AIModel> = withContext(Dispatchers.IO) {
        val base = provider.baseUrl.trim().trimEnd('/')
        val url = when (provider.type) {
            ProviderType.GEMINI -> "$base/models"
            // Anthropic's models endpoint is /v1/models. The base URL may or may not already end in /v1.
            ProviderType.ANTHROPIC -> "${base.removeSuffix("/v1")}/v1/models"
            // OpenRouter lists only text models by default; ask for every output type.
            else -> if (base.contains("openrouter.ai")) "$base/models?output_modalities=all" else "$base/models"
        }

        val request = Request.Builder()
            .url(url)
            .get()
            .addProviderHeaders(provider)
            .build()

        val response = client.executeCancellable(request)
        response.use { r ->
            if (!r.isSuccessful) throw IllegalStateException("HTTP ${r.code} from ${url.substringBefore('?')}")
            val json = JSONObject(r.body?.readUtf8AtMost(HttpBodyLimits.JSON_BYTES) ?: throw IllegalStateException("Empty response"))
            val models = mutableListOf<AIModel>()

            if (provider.type == ProviderType.GEMINI) {
                val arr = json.optJSONArray("models") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val m = arr.getJSONObject(i)
                    val name = m.getString("name").removePrefix("models/")
                    // Gemini TTS uses a different API from chat; it is not offered as a chat model.
                    if (looksLikeAudioModel(name)) continue
                    // Image models are the ones that offer the "predict" method (Imagen), not by name.
                    val methods = m.optJSONArray("supportedGenerationMethods")
                    val imageGen = (0 until (methods?.length() ?: 0)).any { methods?.optString(it) == "predict" }
                    if (looksLikeVideoModel(name)) {
                        models.add(AIModel(name, name, provider.id, false, false, false, 0, supportsVideoGen = true, outputsText = false))
                    } else if (name.contains("gemini") || imageGen) {
                        val chatModel = name.contains("gemini")
                        models.add(
                            AIModel(
                                id = name,
                                displayName = name,
                                providerId = provider.id,
                                supportsVision = chatModel,
                                supportsFunctionCalling = chatModel,
                                supportsImageGen = imageGen,
                                contextWindow = if (chatModel) 1000000 else 8192,
                                outputsText = chatModel
                            )
                        )
                    }
                }
            } else {
                val arr = json.optJSONArray("data") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val m = arr.getJSONObject(i)
                    val id = m.getString("id")
                    val arch = m.optJSONObject("architecture")
                    val inputs = modalityList(arch?.optJSONArray("input_modalities"))
                    val outputs = modalityList(arch?.optJSONArray("output_modalities") ?: m.optJSONArray("output_modalities"))
                    val videoOut = "video" in outputs || looksLikeVideoModel(id)
                    val audioOut = ("audio" in outputs || looksLikeAudioModel(id)) && "text" !in outputs
                    models.add(
                        AIModel(
                            id = id,
                            displayName = id,
                            providerId = provider.id,
                            supportsVision = if (inputs.isEmpty()) id.contains("vision") || id.contains("4o") else "image" in inputs,
                            supportsFunctionCalling = true,
                            supportsImageGen = "image" in outputs,
                            contextWindow = 128000,
                            supportsVideoGen = videoOut,
                            supportsAudioGen = audioOut,
                            outputsText = (outputs.isEmpty() || "text" in outputs) && !videoOut && !audioOut
                        )
                    )
                }
            }
            // OpenRouter lists video models on their own endpoint, not in /models.
            if (base.contains("openrouter.ai")) {
                models.addCatalog(catalogModels(provider, "$base/videos/models", listOf("video")))
                models.addCatalog(catalogModels(provider, "$base/models?output_modalities=speech", listOf("audio")))
            }
            if (base.contains("pollinations.ai")) {
                val origin = base.removeSuffix("/v1")
                models.addCatalog(catalogModels(provider, "$origin/video/models"))
                models.addCatalog(catalogModels(provider, "$origin/image/models"))
                models.addCatalog(catalogModels(provider, "$origin/audio/models", listOf("audio")))
            }
            if (models.isEmpty()) throw IllegalStateException("No models returned")
            models
        }
    }

    /**
     * Models from an extra catalog endpoint (OpenRouter /videos/models, Pollinations /video/models).
     * Type comes from output_modalities; [fixedOutputs] is used when the endpoint only lists one type.
     */
    private suspend fun catalogModels(provider: Provider, url: String, fixedOutputs: List<String>? = null): List<AIModel> {
        return try {
            val request = Request.Builder()
                .url(url)
                .get()
                .addProviderHeaders(provider)
                .build()
            client.executeCancellable(request).use { r ->
                if (!r.isSuccessful) return emptyList()
                val text = r.body?.readUtf8AtMost(HttpBodyLimits.JSON_BYTES) ?: return emptyList()
                val arr = if (text.trimStart().startsWith("[")) JSONArray(text)
                    else JSONObject(text).optJSONArray("data") ?: return emptyList()
                (0 until arr.length()).mapNotNull { i ->
                    val m = arr.optJSONObject(i) ?: return@mapNotNull null
                    val id = m.optString("id").ifBlank { m.optString("name") }.ifBlank { return@mapNotNull null }
                    val outputs = fixedOutputs ?: modalityList(m.optJSONArray("output_modalities"))
                    if (outputs.isEmpty()) return@mapNotNull null
                    AIModel(
                        id = id,
                        displayName = m.optString("title").ifBlank { m.optString("name").ifBlank { id } },
                        providerId = provider.id,
                        supportsVision = "image" in modalityList(m.optJSONArray("input_modalities")),
                        supportsFunctionCalling = false,
                        supportsImageGen = "image" in outputs,
                        contextWindow = 0,
                        supportsVideoGen = "video" in outputs,
                        supportsAudioGen = "audio" in outputs,
                        outputsText = "text" in outputs && "video" !in outputs && "audio" !in outputs
                    )
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Adds catalog models, replacing any entry with the same id. */
    private fun MutableList<AIModel>.addCatalog(extra: List<AIModel>) {
        val ids = extra.map { it.id }.toSet()
        removeAll { it.id in ids }
        addAll(extra)
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
                    "$base/models/$model:generateContent"
                } else {
                    "$base/models/$model:streamGenerateContent?alt=sse"
                }
            }
            ProviderType.ANTHROPIC -> "${base.removeSuffix("/v1")}/v1/messages"
            else -> {
                "$base/chat/completions"
            }
        }
    }

    /** Message text with extracted text-file contents appended. */
    private fun ChatMessage.promptText(gemini: Boolean = false): String {
        val files = attachments.mapNotNull { a -> a.textContent?.let { "\n\n[File: ${a.name}]\n$it" } }
        // Only Gemini receives video bytes here; other models get a note so they do not guess.
        val imageNotes = attachments
            .filter { it.type == AttachmentType.IMAGE && it.base64Data.isNullOrBlank() }
            .map { "\n\n[Image attached: ${it.name}, but its local file data is unavailable.]" }
        val videoNotes = attachments
            .filter { it.type == AttachmentType.VIDEO && (!gemini || it.base64Data.isNullOrBlank()) }
            .map { "\n\n[Video attached: ${it.name}. This model cannot watch videos.]" }
        return content + files.joinToString("") + imageNotes.joinToString("") + videoNotes.joinToString("")
    }

    private fun ChatMessage.videoParts(): List<Attachment> =
        attachments.filter { it.type == AttachmentType.VIDEO && !it.base64Data.isNullOrBlank() }

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
        nativeSearch: Boolean = false,
        functionTools: List<ChatFunctionTool> = emptyList(),
        allowUrlContext: Boolean = true
    ): RequestBody {
        val json = JSONObject()
        val lastMessage = messages.lastOrNull()

        when (provider.type) {
            ProviderType.GEMINI -> {
                val contents = JSONArray()
                messages.forEach { msg ->
                    if (msg.role == MessageRole.SYSTEM) return@forEach
                    val parts = JSONArray()
                    if (msg.role == MessageRole.TOOL) {
                        val response = JSONObject().put("result", msg.content)
                        val functionResponse = JSONObject()
                            .put("name", msg.toolName.orEmpty())
                            .put("response", response)
                        msg.toolCallId?.takeIf { it.isNotBlank() }?.let { functionResponse.put("id", it) }
                        parts.put(JSONObject().put("functionResponse", functionResponse))
                    } else {
                        var text = msg.promptText(gemini = true)
                        if (searchContext != null && msg == lastMessage && msg.role == MessageRole.USER) {
                            text = "Web search results:\n$searchContext\n\nUser question: $text\n\nAnswer using the search results when relevant."
                        }
                        if (text.isNotBlank()) parts.put(JSONObject().put("text", text))
                        msg.imageParts().forEach { img ->
                            parts.put(JSONObject().put("inline_data", JSONObject().put("mime_type", img.mimeType).put("data", img.base64Data)))
                        }
                        msg.videoParts().forEach { vid ->
                            parts.put(JSONObject().put("inline_data", JSONObject().put("mime_type", vid.mimeType).put("data", vid.base64Data)))
                        }
                        msg.toolCalls.forEach { call ->
                            val functionCall = JSONObject()
                                .put("name", call.name)
                                .put("args", call.arguments.asJsonObject())
                            if (call.id.isNotBlank()) functionCall.put("id", call.id)
                            parts.put(JSONObject().put("functionCall", functionCall))
                        }
                    }
                    if (parts.length() == 0) return@forEach
                    contents.put(JSONObject().apply {
                        put("role", if (msg.role == MessageRole.USER || msg.role == MessageRole.TOOL) "user" else "model")
                        put("parts", parts)
                    })
                }
                json.put("contents", contents)
                if (systemPrompt != null) {
                    json.put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
                }
                json.put("generationConfig", JSONObject().apply {
                    put("temperature", temperature.toDouble())
                    reasoning.geminiBudget?.takeIf { it > 0 }?.let { put("thinkingConfig", JSONObject().put("thinkingBudget", it)) }
                })
                val nativeTools = modelTools(provider, modelId)
                    .filterNot { it == "url_context" && !allowUrlContext }
                    .toMutableList()
                if (nativeSearch && !nativeTools.contains("google_search")) nativeTools.add("google_search")
                val toolsJson = JSONArray().apply {
                    nativeTools.forEach { put(JSONObject().put(it, JSONObject())) }
                    if (functionTools.isNotEmpty()) {
                        put(JSONObject().put("functionDeclarations", JSONArray().apply {
                            functionTools.forEach { tool ->
                                put(JSONObject()
                                    .put("name", tool.name)
                                    .put("description", tool.description)
                                    .put("parameters", tool.parameters.toGeminiSchema()))
                            }
                        }))
                    }
                }
                if (toolsJson.length() > 0) json.put("tools", toolsJson)
            }
            ProviderType.ANTHROPIC -> {
                val budget = reasoning.budgetTokens
                json.put("model", if (modelId.isNotBlank()) modelId else "claude-3-5-sonnet-20241022")
                val thinking = budget != null && budget > 0
                json.put("max_tokens", if (thinking) budget!! + 4096 else 4096)
                json.put("stream", isStream)
                val anthropicTools = JSONArray()
                if (nativeSearch) {
                    anthropicTools.put(JSONObject().put("type", "web_search_20250305").put("name", "web_search"))
                }
                functionTools.forEach { tool ->
                    anthropicTools.put(JSONObject()
                        .put("name", tool.name)
                        .put("description", tool.description)
                        .put("input_schema", tool.parameters))
                }
                if (anthropicTools.length() > 0) json.put("tools", anthropicTools)
                if (thinking) {
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
                var pendingToolResults = JSONArray()
                fun flushToolResults() {
                    if (pendingToolResults.length() == 0) return
                    msgs.put(JSONObject().put("role", "user").put("content", pendingToolResults))
                    pendingToolResults = JSONArray()
                }
                messages.filter { it.role != MessageRole.SYSTEM }.forEach { m ->
                    if (m.role == MessageRole.TOOL) {
                        pendingToolResults.put(JSONObject()
                            .put("type", "tool_result")
                            .put("tool_use_id", m.toolCallId.orEmpty())
                            .put("content", m.content))
                        return@forEach
                    }
                    flushToolResults()
                    val content = JSONArray()
                    m.imageParts().forEach { img ->
                        content.put(JSONObject().put("type", "image").put("source", JSONObject()
                            .put("type", "base64").put("media_type", img.mimeType).put("data", img.base64Data)))
                    }
                    val text = m.promptText()
                    if (text.isNotBlank()) content.put(JSONObject().put("type", "text").put("text", text))
                    m.toolCalls.forEach { call ->
                        content.put(JSONObject()
                            .put("type", "tool_use")
                            .put("id", call.id)
                            .put("name", call.name)
                            .put("input", call.arguments.asJsonObject()))
                    }
                    if (content.length() == 0) return@forEach
                    msgs.put(JSONObject().apply {
                        put("role", if (m.role == MessageRole.USER) "user" else "assistant")
                        put("content", content)
                    })
                }
                flushToolResults()
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
                if (functionTools.isNotEmpty()) {
                    json.put("tools", JSONArray().apply {
                        functionTools.forEach { tool ->
                            put(JSONObject()
                                .put("type", "function")
                                .put("function", JSONObject()
                                    .put("name", tool.name)
                                    .put("description", tool.description)
                                    .put("parameters", tool.parameters)))
                        }
                    })
                    json.put("tool_choice", "auto")
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
                    if (m.role == MessageRole.TOOL) {
                        msgs.put(JSONObject().apply {
                            put("role", "tool")
                            put("tool_call_id", m.toolCallId.orEmpty())
                            m.toolName?.takeIf { it.isNotBlank() }?.let { put("name", it) }
                            put("content", m.content)
                        })
                        return@forEach
                    }
                    val text = m.promptText()
                    val images = m.imageParts()
                    if (text.isBlank() && images.isEmpty() && m.toolCalls.isEmpty()) return@forEach
                    val content: Any = if (m.role == MessageRole.USER && images.isNotEmpty()) {
                        JSONArray().apply {
                            put(JSONObject().put("type", "text").put("text", text))
                            images.forEach { img ->
                                put(JSONObject().put("type", "image_url").put("image_url",
                                    JSONObject().put("url", "data:${img.mimeType};base64,${img.base64Data}")))
                            }
                        }
                    } else if (m.role == MessageRole.ASSISTANT && text.isBlank() && m.toolCalls.isNotEmpty()) {
                        JSONObject.NULL
                    } else {
                        text
                    }
                    val role = when (m.role) {
                        MessageRole.USER -> "user"
                        MessageRole.ASSISTANT -> "assistant"
                        MessageRole.SYSTEM -> "system"
                        MessageRole.TOOL -> "tool"
                    }
                    val message = JSONObject().put("role", role).put("content", content)
                    if (m.toolCalls.isNotEmpty()) {
                        message.put("tool_calls", JSONArray().apply {
                            m.toolCalls.forEach { call ->
                                put(JSONObject()
                                    .put("id", call.id)
                                    .put("type", "function")
                                    .put("function", JSONObject()
                                        .put("name", call.name)
                                        .put("arguments", call.arguments)))
                            }
                        })
                    }
                    msgs.put(message)
                }
                json.put("messages", msgs)
            }
        }

        val modelBody = provider.models.find { it.id == modelId }?.customBody
        listOfNotNull(provider.customBody, modelBody).forEach { custom ->
            try {
                val customJson = JSONObject(custom)
                customJson.keys().forEach { key ->
                    if (key != "model" && key != "messages" && key != "stream" && !(key == "tools" && functionTools.isNotEmpty())) {
                        json.put(key, customJson.get(key))
                    }
                }
            } catch (e: Exception) { com.ariai.app.util.AppLog.warn(e) }
        }

        return json.toString().toRequestBody("application/json".toMediaType())
    }

    private fun hasUnexecutedToolCalls(json: JSONObject, providerType: ProviderType): Boolean = when (providerType) {
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

    private fun parseOpenAIStreamChunk(json: JSONObject): String {
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

    private fun parseOpenAINonStreaming(json: JSONObject): String {
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

    private fun parseAnthropicResponse(json: JSONObject): String {
        val content = json.optJSONArray("content") ?: return ""
        return buildString {
            for (index in 0 until content.length()) {
                val block = content.optJSONObject(index) ?: continue
                if (block.optString("type") == "text") append(block.optString("text", ""))
            }
        }
    }

    private fun parseAgentStep(json: JSONObject, providerType: ProviderType): AgentStep {
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
            AgentStep(text.toString(), calls)
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
            AgentStep(text.toString(), calls)
        }
        else -> {
            val message = json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                ?: return AgentStep("", emptyList())
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
            AgentStep(text, calls)
        }
        }
    }

    private fun JSONObject.toGeminiSchema(): JSONObject {
        val output = JSONObject()
        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key == "additionalProperties") continue
            val value = get(key)
            val converted = when {
                key == "type" && value is String -> value.uppercase()
                value is JSONObject -> value.toGeminiSchema()
                value is JSONArray -> JSONArray().apply {
                    for (index in 0 until value.length()) {
                        val item = value.get(index)
                        put(if (item is JSONObject) item.toGeminiSchema() else item)
                    }
                }
                else -> value
            }
            output.put(key, converted)
        }
        return output
    }

    private fun String.asJsonObject(): JSONObject = runCatching { JSONObject(this) }.getOrElse { JSONObject() }

    private fun responseText(value: Any?): String = when (value) {
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

    private fun parseGeminiResponse(json: JSONObject): String {
        return try {
            val candidates = json.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val first = candidates.getJSONObject(0)
            val content = first.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            if (parts.length() == 0) return ""
            buildString {
                for (i in 0 until parts.length()) {
                    append(parts.optJSONObject(i)?.optString("text", "").orEmpty())
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun modalityList(arr: JSONArray?): List<String> =
        arr?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()

    /** Speech or music from text. Pollinations uses GET /audio/{text}; OpenAI-style APIs use POST /audio/speech. */
    suspend fun generateAudio(
        provider: Provider,
        modelId: String,
        prompt: String,
        onProgress: (String) -> Unit = {}
    ): ByteArray = withContext(Dispatchers.IO) {
        val base = provider.baseUrl.trim().trimEnd('/')
        val isMusic = Regex("music|lyria|stable-audio|sound", RegexOption.IGNORE_CASE).containsMatchIn(modelId)
        val longClient = client.newBuilder()
            .readTimeout(5, TimeUnit.MINUTES)
            .callTimeout(10, TimeUnit.MINUTES)
            .build()
        onProgress(tx("Generating audio"))
        val request = if (base.contains("pollinations.ai")) {
            val origin = base.removeSuffix("/v1")
            val encodedPrompt = java.net.URLEncoder.encode(prompt, "UTF-8").replace("+", "%20")
            val encodedModel = java.net.URLEncoder.encode(modelId, "UTF-8")
            val voice = if (isMusic) "" else "&voice=alloy"
            Request.Builder().url("$origin/audio/$encodedPrompt?model=$encodedModel$voice").get()
        } else {
            val body = JSONObject().put("model", modelId).put("input", prompt).put("voice", "alloy")
                .toString().toRequestBody("application/json".toMediaType())
            Request.Builder().url("$base/audio/speech").post(body)
        }
        request.addProviderHeaders(provider, modelId)
        longClient.executeCancellable(request.build()).use { r ->
            if (!r.isSuccessful) throw IllegalStateException("API Error ${r.code}: ${r.body?.readUtf8AtMost(HttpBodyLimits.ERROR_BYTES)?.take(200)}")
            r.body?.readBytesAtMost(HttpBodyLimits.AUDIO_BYTES) ?: throw IllegalStateException("Empty audio response")
        }
    }

    /** Veo on the Gemini API: start a long-running operation, poll it, then download the MP4. */
    private suspend fun generateVeo(provider: Provider, modelId: String, prompt: String, onProgress: (String) -> Unit): ByteArray {
        val base = provider.baseUrl.trim().trimEnd('/')
        fun keyed(b: Request.Builder): Request.Builder = b.addProviderHeaders(provider, modelId)
        onProgress("Sending request")
        val body = JSONObject()
            .put("instances", JSONArray().put(JSONObject().put("prompt", prompt)))
            .toString().toRequestBody("application/json".toMediaType())
        val opName = client.executeCancellable(keyed(Request.Builder().url("$base/models/$modelId:predictLongRunning").post(body)).build()).use { r ->
            val text = r.body?.readUtf8AtMost(HttpBodyLimits.JSON_BYTES).orEmpty()
            if (!r.isSuccessful) throw IllegalStateException("Video submit failed: ${r.code} ${text.take(160)}")
            JSONObject(text).optString("name").ifBlank { throw IllegalStateException("No operation returned") }
        }

        val started = System.currentTimeMillis()
        val timeoutMs = 15 * 60_000L
        var lastPoll = 0L
        var bytes: ByteArray? = null
        while (bytes == null) {
            kotlinx.coroutines.delay(1_000)
            val now = System.currentTimeMillis()
            if (now - started > timeoutMs) throw IllegalStateException("Video timed out")
            if (now - lastPoll >= 5_000) {
                lastPoll = now
                val op = client.executeCancellable(keyed(Request.Builder().url("$base/$opName").get()).build()).use { r ->
                    if (!r.isSuccessful) throw IllegalStateException("Video status failed: ${r.code}")
                    JSONObject(r.body?.readUtf8AtMost(HttpBodyLimits.JSON_BYTES).orEmpty())
                }
                op.optJSONObject("error")?.let { err ->
                    throw IllegalStateException(err.optString("message").ifBlank { "Video generation failed" })
                }
                if (op.optBoolean("done")) {
                    val response = op.optJSONObject("response")
                    val first = (response?.optJSONObject("generateVideoResponse")?.optJSONArray("generatedSamples")
                        ?: response?.optJSONArray("generatedVideos"))?.optJSONObject(0)
                    val uri = first?.optJSONObject("video")?.optString("uri").orEmpty()
                    if (uri.isBlank()) throw IllegalStateException("No video in the response. It may have been blocked by the safety filter.")
                    val downloadUrl = parseSecureHttpsUrl(uri)
                    val downloadRequest = Request.Builder().url(downloadUrl).get().apply {
                        // Veo may return a signed URL on a separate storage host. Never forward provider
                        // API keys or custom headers across origins.
                        if (shouldSendVeoCredentials(base, downloadUrl)) addProviderHeaders(provider, modelId)
                    }.build()
                    bytes = client.executeCancellable(downloadRequest).use { r ->
                        if (!r.isSuccessful) throw IllegalStateException("Video download failed: ${r.code}")
                        r.body?.readBytesAtMost(HttpBodyLimits.VIDEO_BYTES) ?: throw IllegalStateException("Empty video")
                    }
                }
            }
            if (bytes == null) {
                val secs = ((now - started) / 1000).toInt()
                onProgress("Generating video · Rendering · %02d:%02d".format(secs / 60, secs % 60))
            }
        }
        return bytes ?: throw IllegalStateException("Video timed out")
    }

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
        fun auth(b: Request.Builder): Request.Builder = b.addProviderHeaders(provider, modelId)
        if (base.contains("pollinations.ai")) {
            onProgress("Rendering")
            val origin = base.removeSuffix("/v1")
            val encodedPrompt = java.net.URLEncoder.encode(prompt, "UTF-8").replace("+", "%20")
            val encodedModel = java.net.URLEncoder.encode(modelId, "UTF-8")
            val longClient = client.newBuilder()
                .readTimeout(10, java.util.concurrent.TimeUnit.MINUTES)
                .callTimeout(15, java.util.concurrent.TimeUnit.MINUTES)
                .build()
            val request = auth(Request.Builder().url("$origin/video/$encodedPrompt?model=$encodedModel").get()).build()
            longClient.executeCancellable(request).use { r ->
                if (!r.isSuccessful) throw IllegalStateException("API Error ${r.code}: ${r.body?.readUtf8AtMost(HttpBodyLimits.ERROR_BYTES)?.take(200)}")
                return@withContext r.body?.readBytesAtMost(HttpBodyLimits.VIDEO_BYTES) ?: throw IllegalStateException("Empty video response")
            }
        }
        if (provider.type == ProviderType.GEMINI) {
            return@withContext generateVeo(provider, modelId, prompt, onProgress)
        }
        onProgress("Sending request")
        val submitBody = JSONObject().put("model", modelId).put("prompt", prompt)
            .toString().toRequestBody("application/json".toMediaType())
        val jobId = client.executeCancellable(auth(Request.Builder().url("$base/videos").post(submitBody)).build()).use { r ->
            val text = r.body?.readUtf8AtMost(HttpBodyLimits.JSON_BYTES).orEmpty()
            if (!r.isSuccessful) throw IllegalStateException("Video submit failed: ${r.code} ${text.take(160)}")
            JSONObject(text).optString("id").ifBlank { throw IllegalStateException("No job id returned") }
        }

        val started = System.currentTimeMillis()
        val timeoutMs = 15 * 60_000L
        var state = "queued"
        var lastPoll = 0L
        while (true) {
            kotlinx.coroutines.delay(1_000)
            val now = System.currentTimeMillis()
            if (now - started > timeoutMs) throw IllegalStateException("Video timed out")
            // Poll the job every 5 seconds; the elapsed timer still ticks every second.
            if (now - lastPoll >= 5_000) {
                lastPoll = now
                val status = client.executeCancellable(auth(Request.Builder().url("$base/videos/$jobId").get()).build()).use { r ->
                    if (!r.isSuccessful) throw IllegalStateException("Video status failed: ${r.code}")
                    JSONObject(r.body?.readUtf8AtMost(HttpBodyLimits.JSON_BYTES).orEmpty())
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
        client.executeCancellable(auth(Request.Builder().url("$base/videos/$jobId/content?index=0").get()).build()).use { r ->
            if (!r.isSuccessful) throw IllegalStateException("Video download failed: ${r.code}")
            r.body?.readBytesAtMost(HttpBodyLimits.VIDEO_BYTES) ?: throw IllegalStateException("Empty video")
        }
    }
}

/** True when the provider can search the web itself (Gemini, Anthropic, OpenRouter). */
fun supportsNativeSearch(provider: Provider): Boolean =
    provider.type == ProviderType.GEMINI || provider.type == ProviderType.ANTHROPIC || provider.baseUrl.contains("openrouter.ai")
