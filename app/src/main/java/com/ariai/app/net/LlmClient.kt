package com.ariai.app.net

import com.ariai.app.data.ChatMessage
import com.ariai.app.data.Provider
import com.ariai.app.data.RequestLog
import com.ariai.app.util.Errors
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class LlmClient {
    @Volatile var lastLogs = listOf<RequestLog>()
        private set

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val active = AtomicReference<Call?>(null)

    fun cancel() {
        active.getAndSet(null)?.cancel()
    }

    fun friendly(e: Throwable): String = Errors.friendly(e)

    fun log(method: String, url: String, status: Int, body: String) {
        lastLogs = (listOf(RequestLog(System.currentTimeMillis(), method, Errors.redact(url), status, Errors.redact(body.take(2500)))) + lastLogs).take(100)
    }

    fun base(p: Provider): String = p.baseUrl.trim().trimEnd('/')

    fun chatStream(
        p: Provider,
        model: String,
        messages: List<ChatMessage>,
        system: String?,
        onDelta: (String) -> Unit
    ): String {
        return when (p.kind) {
            "anthropic" -> anthropic(p, model, messages, system, onDelta)
            "gemini" -> gemini(p, model, messages, system, onDelta)
            else -> openai(p, model, messages, system, onDelta)
        }
    }

    fun chat(p: Provider, model: String, messages: List<ChatMessage>, system: String?): String =
        chatStream(p, model, messages, system) {}

    fun listModels(p: Provider): List<String> {
        val url = when (p.kind) {
            "gemini" -> base(p).trimEnd('/') + "/models"
            "anthropic" -> if (base(p).endsWith("/v1")) base(p) + "/models" else base(p) + "/v1/models"
            else -> base(p) + "/models"
        }
        val b = Request.Builder().url(url).get()
        auth(b, p)
        val req = b.build()
        val call = http.newCall(req)
        active.set(call)
        return try {
            call.execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                log("GET", url, resp.code, body)
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}: ${body.take(400)}")
                return LlmJson.parseModelIds(body)
            }
        } finally {
            active.compareAndSet(call, null)
        }
    }

    internal fun searchDuckResults(q: String, limit: Int = 5): List<SearchParser.SearchResult> {
        val query = q.trim()
        if (query.isBlank() || limit <= 0) return emptyList()
        val url = "https://html.duckduckgo.com/html/?q=" + java.net.URLEncoder.encode(query, "UTF-8")
        val req = Request.Builder().url(url).header("User-Agent", "AriAi/1.0").get().build()
        val call = http.newCall(req)
        active.set(call)
        return try {
            call.execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                log("GET", url, resp.code, body.take(400))
                if (!resp.isSuccessful) return emptyList()
                SearchParser.parseResults(body, limit)
            }
        } finally {
            active.compareAndSet(call, null)
        }
    }

    internal fun fetchWebPage(url: String, maxChars: Int = 12000): String {
        val target = url.trim()
        if (!target.startsWith("https://") && !target.startsWith("http://")) return ""
        val req = Request.Builder().url(target).header("User-Agent", "AriAi/1.0 Research").get().build()
        val call = http.newCall(req)
        active.set(call)
        return try {
            call.execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                log("GET", target, resp.code, body.take(400))
                if (!resp.isSuccessful) return ""
                SearchParser.extractText(body, maxChars)
            }
        } catch (_: Exception) {
            ""
        } finally {
            active.compareAndSet(call, null)
        }
    }

    fun searchDuck(q: String): String =
        searchDuckResults(q).joinToString("\n") {
            if (it.snippet.isNullOrBlank()) "- ${it.title}" else "- ${it.title}: ${it.snippet}"
        }

    private fun openai(p: Provider, model: String, messages: List<ChatMessage>, system: String?, onDelta: (String) -> Unit): String {
        val url = base(p) + "/chat/completions"
        val arr = JSONArray()
        if (!system.isNullOrBlank()) arr.put(JSONObject().put("role", "system").put("content", system))
        messages.forEach { m -> arr.put(openAiMsg(m)) }
        val payload = JSONObject()
            .put("model", model.ifBlank { p.model })
            .put("messages", arr)
            .put("stream", true)
            .put("temperature", p.temperature.toDouble())
            .put("max_tokens", p.maxTokens)
            .toString()
        return sse(url, p, payload, onDelta) { line ->
            val o = JSONObject(line)
            o.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")?.optString("content").orEmpty()
        }
    }

    private fun anthropic(p: Provider, model: String, messages: List<ChatMessage>, system: String?, onDelta: (String) -> Unit): String {
        val url = if (base(p).endsWith("/v1")) base(p) + "/messages" else base(p) + "/v1/messages"
        val arr = JSONArray()
        messages.filter { it.role != ChatMessage.Role.System }.forEach { m ->
            arr.put(JSONObject().put("role", if (m.role == ChatMessage.Role.Assistant) "assistant" else "user").put("content", m.text))
        }
        val payload = JSONObject()
            .put("model", model.ifBlank { p.model })
            .put("max_tokens", p.maxTokens)
            .put("messages", arr)
            .put("stream", true)
        if (!system.isNullOrBlank()) payload.put("system", system)
        return sse(url, p, payload.toString(), onDelta) { line ->
            val o = JSONObject(line)
            if (o.optString("type") == "content_block_delta") o.optJSONObject("delta")?.optString("text").orEmpty()
            else o.optJSONArray("content")?.optJSONObject(0)?.optString("text").orEmpty()
        }
    }

    private fun gemini(p: Provider, model: String, messages: List<ChatMessage>, system: String?, onDelta: (String) -> Unit): String {
        val m = model.ifBlank { p.model }.removePrefix("models/")
        val url = base(p).trimEnd('/') + "/models/${m}:streamGenerateContent?alt=sse"
        val contents = JSONArray()
        if (!system.isNullOrBlank()) {
            contents.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", "System: ${system}"))))
        }
        messages.forEach { msg ->
            val role = if (msg.role == ChatMessage.Role.Assistant) "model" else "user"
            contents.put(JSONObject().put("role", role).put("parts", JSONArray().put(JSONObject().put("text", msg.text))))
        }
        val payload = JSONObject().put("contents", contents)
            .put("generationConfig", JSONObject().put("temperature", p.temperature.toDouble()).put("maxOutputTokens", p.maxTokens))
            .toString()

        return sse(url, p, payload, onDelta) { line ->
            JSONObject(line).optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                .orEmpty()
        }
    }

    private fun sse(url: String, p: Provider, payload: String, onDelta: (String) -> Unit, parse: (String) -> String): String {
        val b = Request.Builder().url(url).post(payload.toRequestBody(JSON)).header("Accept", "text/event-stream")
        auth(b, p)
        val call = http.newCall(b.build())
        active.set(call)
        val out = StringBuilder()
        try {
            call.execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string().orEmpty()
                    log("POST", url, resp.code, err)
                    throw IllegalStateException("HTTP ${resp.code}: ${err.take(500)}")
                }
                val src = resp.body?.source() ?: return ""
                val lines = generateSequence { if (src.exhausted()) null else src.readUtf8Line() }
                SseFrames.consume(lines, parse) { piece ->
                    out.append(piece)
                    onDelta(piece)
                }
                log("POST", url, resp.code, out.take(400).toString())
            }
        } finally {
            active.compareAndSet(call, null)
        }
        return out.toString()
    }

    private fun openAiMsg(m: ChatMessage): JSONObject {
        val role = when (m.role) {
            ChatMessage.Role.User -> "user"
            ChatMessage.Role.Assistant -> "assistant"
            ChatMessage.Role.System -> "system"
        }
        val content: Any = if (!m.imageBase64.isNullOrBlank()) {
            JSONArray()
                .put(JSONObject().put("type", "text").put("text", m.text))
                .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:image/jpeg;base64,${m.imageBase64}")))
        } else m.text
        return JSONObject().put("role", role).put("content", content)
    }

    private fun auth(b: Request.Builder, p: Provider) {
        when (p.kind) {
            "anthropic" -> {
                b.header("x-api-key", p.apiKey)
                b.header("anthropic-version", "2023-06-01")
            }
            "gemini" -> b.header("x-goog-api-key", p.apiKey)
            else -> b.header("Authorization", "Bearer ${p.apiKey}")
        }
        b.header("Content-Type", "application/json")
        p.headers.lines().forEach { line ->
            val i = line.indexOf(':')
            if (i > 0) b.header(line.substring(0, i).trim(), line.substring(i + 1).trim())
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
