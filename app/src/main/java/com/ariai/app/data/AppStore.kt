package com.ariai.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AppStore(context: Context) {
    private val p = context.getSharedPreferences("ariai", Context.MODE_PRIVATE)
    private val secrets = SecretStore(context)

    fun bool(k: String, d: Boolean = false) = p.getBoolean(k, d)
    fun setBool(k: String, v: Boolean) = p.edit().putBoolean(k, v).apply()
    fun str(k: String, d: String = "") = p.getString(k, d) ?: d
    fun setStr(k: String, v: String) = p.edit().putString(k, v).apply()
    fun int(k: String, d: Int = 0) = p.getInt(k, d)
    fun setInt(k: String, v: Int) = p.edit().putInt(k, v).apply()
    fun inc(k: String) = setInt(k, int(k) + 1)

    /**
     * Export configuration without exporting provider credentials.
     * Secrets live in Android Keystore-backed storage and are intentionally
     * never included in portable backups.
     */
    fun exportJson(): String {
        val o = JSONObject()
        p.all.forEach { (k, v) ->
            if (k == "providers" || k == "mcp") {
                val arr = JSONArray(v.toString())
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    if (k == "providers") item.put("apiKey", "")
                    item.put("headers", "")
                }
                o.put(k, arr)
            } else {
                o.put(k, v)
            }
        }
        return o.toString(2)
    }

    fun importJson(raw: String) {
        val o = JSONObject(raw)
        val e = p.edit()
        o.keys().forEach { k ->
            // Portable backups intentionally never restore credential fields.
            if (k == "providers" || k == "mcp") {
                val arr = o.optJSONArray(k) ?: return@forEach
                val sanitized = JSONArray()
                for (i in 0 until arr.length()) {
                    val item = JSONObject(arr.getJSONObject(i).toString())
                    if (k == "providers") item.put("apiKey", "")
                    item.put("headers", "")
                    sanitized.put(item)
                }
                e.putString(k, sanitized.toString())
                return@forEach
            }
            when (val v = o.get(k)) {
                is Boolean -> e.putBoolean(k, v)
                is Int -> e.putInt(k, v)
                is Long -> e.putLong(k, v)
                else -> e.putString(k, v.toString())
            }
        }
        e.apply()
    }

    fun providers(): List<Provider> {
        val list = parseArr("providers") { o ->
            val id = o.getString("id")
            val legacyKey = o.optString("apiKey")
            val legacyHeaders = o.optString("headers")
            val apiKey = secrets.get(apiKeySecret(id)).ifBlank { legacyKey }
            val headers = secrets.get(headersSecret(id)).ifBlank { legacyHeaders }
            Provider(
                id, o.getString("name"), o.optString("baseUrl"),
                o.optString("model"), apiKey, mutableListOf<String>().also { models ->
                    val m = o.optJSONArray("models")
                    if (m != null) for (i in 0 until m.length()) models += m.getString(i)
                },
                headers, o.optString("kind", "openai"),
                o.optDouble("temperature", 0.7).toFloat(), o.optInt("maxTokens", 4096),
                o.optString("lastOk"), o.optBoolean("enabled", apiKey.isNotBlank())
            )
        }

        val extra = catalog().filter { c -> list.none { it.id == c.id } }
        val merged = when {
            list.isEmpty() -> catalog()
            extra.isNotEmpty() -> extra + list
            else -> list
        }.map { provider ->
            // Gemini 2.0 Flash was shut down by Google on June 1, 2026.
            // Migrate the bundled default and any previously saved Gemini
            // selection so Test/Chat do not target a retired model.
            if (provider.id == "gemini" &&
                (provider.model == "gemini-2.0-flash" || provider.model == "gemini-1.5-pro" || provider.model == "gemini-1.5-flash")
            ) {
                provider.copy(
                    model = "gemini-3.5-flash",
                    models = listOf("gemini-3.5-flash", "gemini-3.1-flash-lite")
                )
            } else provider
        }

        // One-time migration: old versions stored credentials directly in JSON.
        // saveProviders moves them to the Keystore-backed vault.
        if (merged.any { it.apiKey.isNotBlank() || it.headers.isNotBlank() } ||
            merged.size != list.size
        ) {
            saveProviders(merged)
        }
        return merged
    }

    companion object {
        fun id() = UUID.randomUUID().toString()

        fun catalog() = listOf(
            Provider(
                "openai", "OpenAI", "https://api.openai.com/v1", "gpt-4o-mini", "",
                listOf("gpt-4o", "gpt-4o-mini", "gpt-4.1", "o4-mini", "o3-mini"), kind = "openai"
            ),
            Provider(
                "anthropic", "Anthropic", "https://api.anthropic.com", "claude-3-5-sonnet-latest", "",
                listOf("claude-3-5-sonnet-latest", "claude-3-5-haiku-latest", "claude-3-opus-latest"), kind = "anthropic"
            ),
            Provider(
                "gemini", "Google Gemini", "https://generativelanguage.googleapis.com/v1beta", "gemini-3.5-flash", "",
                listOf("gemini-3.5-flash", "gemini-3.1-flash-lite"), kind = "gemini"
            ),
            Provider(
                "openrouter", "OpenRouter", "https://openrouter.ai/api/v1", "openai/gpt-4o-mini", "",
                listOf("openai/gpt-4o-mini", "anthropic/claude-3.5-sonnet", "google/gemini-flash-1.5", "meta-llama/llama-3.1-70b-instruct")
            ),
            Provider(
                "deepseek", "DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat", "",
                listOf("deepseek-chat", "deepseek-reasoner")
            ),
            Provider(
                "groq", "Groq", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", "",
                listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768")
            ),
            Provider(
                "qwen", "Qwen", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus", "",
                listOf("qwen-plus", "qwen-turbo", "qwen-max")
            ),
            Provider(
                "mistral", "Mistral", "https://api.mistral.ai/v1", "mistral-large-latest", "",
                listOf("mistral-large-latest", "mistral-medium-latest", "mistral-small-latest")
            )
        )
    }

    fun saveProviders(list: List<Provider>) {
        val activeIds = list.map { it.id }.toSet()
        list.forEach { x ->
            secrets.put(apiKeySecret(x.id), x.apiKey)
            secrets.put(headersSecret(x.id), x.headers)
        }

        // Remove credentials belonging to providers that no longer exist.
        p.getString("providers", "[]")?.let { raw ->
            try {
                val old = JSONArray(raw)
                for (i in 0 until old.length()) {
                    val id = old.getJSONObject(i).optString("id")
                    if (id.isNotBlank() && id !in activeIds) {
                        secrets.remove(apiKeySecret(id))
                        secrets.remove(headersSecret(id))
                    }
                }
            } catch (_: Exception) { }
        }

        saveArr("providers", list) { x ->
            JSONObject().put("id", x.id).put("name", x.name).put("baseUrl", x.baseUrl)
                .put("model", x.model).put("apiKey", "")
                .put("models", JSONArray(x.models)).put("headers", "")
                .put("kind", x.kind).put("temperature", x.temperature.toDouble()).put("maxTokens", x.maxTokens)
                .put("lastOk", x.lastOk).put("enabled", x.enabled)
        }
    }

    fun modelProfiles(): List<ModelProfile> = parseArr("model_profiles") { o ->
        ModelProfile(
            id = o.getString("id"),
            providerId = o.optString("providerId"),
            modelId = o.optString("modelId"),
            displayName = o.optString("displayName"),
            modelType = o.optString("modelType", "Chat"),
            inputModalities = o.optJSONArray("inputModalities")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: listOf("Text"),
            outputModalities = o.optJSONArray("outputModalities")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: listOf("Text"),
            abilities = o.optJSONArray("abilities")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
            providerOverride = o.optString("providerOverride"),
            headers = o.optString("headers"),
            body = o.optString("body"),
            builtInTools = o.optJSONArray("builtInTools")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
        )
    }

    fun saveModelProfiles(list: List<ModelProfile>) = saveArr("model_profiles", list) { x ->
        JSONObject()
            .put("id", x.id)
            .put("providerId", x.providerId)
            .put("modelId", x.modelId)
            .put("displayName", x.displayName)
            .put("modelType", x.modelType)
            .put("inputModalities", JSONArray(x.inputModalities))
            .put("outputModalities", JSONArray(x.outputModalities))
            .put("abilities", JSONArray(x.abilities))
            .put("providerOverride", x.providerOverride)
            .put("headers", x.headers)
            .put("body", x.body)
            .put("builtInTools", JSONArray(x.builtInTools))
    }

    fun assistants(): List<Assistant> {
        val list = parseArr("assistants") { o ->
            Assistant(o.getString("id"), o.getString("name"), o.optString("prompt", "You are a helpful assistant."))
        }
        return if (list.isEmpty()) {
            val d = listOf(Assistant("a1", "Default Assistant", "You are a helpful assistant."))
            saveAssistants(d); d
        } else list
    }

    fun saveAssistants(list: List<Assistant>) = saveArr("assistants", list) { x ->
        JSONObject().put("id", x.id).put("name", x.name).put("prompt", x.prompt)
    }

    fun mcp(): List<McpServer> {
        val list = parseArr("mcp") { o ->
            val id = o.getString("id")
            val legacyHeaders = o.optString("headers")
            val headers = secrets.get(mcpHeadersSecret(id)).ifBlank { legacyHeaders }
            McpServer(id, o.getString("name"), o.optString("url"), o.optBoolean("enabled", true), o.optString("transport", "http"), headers)
        }
        // One-time migration for legacy MCP auth headers.
        if (list.any { it.headers.isNotBlank() }) saveMcp(list)
        return list
    }

    fun saveMcp(list: List<McpServer>) {
        val activeIds = list.map { it.id }.toSet()
        list.forEach { x -> secrets.put(mcpHeadersSecret(x.id), x.headers) }

        p.getString("mcp", "[]")?.let { raw ->
            try {
                val old = JSONArray(raw)
                for (i in 0 until old.length()) {
                    val id = old.getJSONObject(i).optString("id")
                    if (id.isNotBlank() && id !in activeIds) secrets.remove(mcpHeadersSecret(id))
                }
            } catch (_: Exception) { }
        }

        saveArr("mcp", list) { x ->
            JSONObject().put("id", x.id).put("name", x.name).put("url", x.url)
                .put("enabled", x.enabled).put("transport", x.transport).put("headers", "")
        }
    }

    fun prompts(): List<PromptItem> = parseArr("prompts") { o ->
        PromptItem(o.getString("id"), o.getString("title"), o.optString("body"))
    }

    fun savePrompts(list: List<PromptItem>) = saveArr("prompts", list) { x ->
        JSONObject().put("id", x.id).put("title", x.title).put("body", x.body)
    }

    fun quick(): List<QuickMsg> = parseArr("quick") { o ->
        QuickMsg(o.getString("id"), o.getString("text"))
    }

    fun saveQuick(list: List<QuickMsg>) = saveArr("quick", list) { x ->
        JSONObject().put("id", x.id).put("text", x.text)
    }

    fun conversations(): List<Conversation> {
        val list = parseArr("conversations") { o ->
            val msgs = mutableListOf<ChatMessage>()
            val mArr = o.optJSONArray("messages") ?: JSONArray()
            for (j in 0 until mArr.length()) {
                val m = mArr.getJSONObject(j)
                msgs += ChatMessage(
                    m.getString("id"),
                    ChatMessage.Role.valueOf(m.getString("role")),
                    m.getString("text"),
                    m.optString("attachment").ifBlank { null },
                    m.optString("image").ifBlank { null }
                )
            }
            Conversation(o.getString("id"), o.getString("title"), o.optString("preview"), o.optLong("updatedAt"), msgs, o.optString("providerId").ifBlank { null }, o.optBoolean("pinned"))
        }
        return list.sortedWith(compareByDescending<Conversation> { it.pinned }.thenByDescending { it.updatedAt })
    }

    fun saveConversation(c: Conversation) {
        persist(conversations().filter { it.id != c.id } + c)
    }

    fun deleteConversation(id: String) = persist(conversations().filter { it.id != id })

    private fun persist(list: List<Conversation>) = saveArr("conversations", list) { c ->
        val msgs = JSONArray()
        c.messages.forEach { m ->
            msgs.put(
                JSONObject().put("id", m.id).put("role", m.role.name).put("text", m.text)
                    .put("attachment", m.attachmentName ?: "").put("image", m.imageBase64 ?: "")
            )
        }
        JSONObject().put("id", c.id).put("title", c.title).put("preview", c.preview)
            .put("updatedAt", c.updatedAt).put("messages", msgs).put("providerId", c.providerId ?: "").put("pinned", c.pinned)
    }

    private fun <T> parseArr(key: String, map: (JSONObject) -> T): List<T> {
        val arr = JSONArray(p.getString(key, "[]"))
        return (0 until arr.length()).map { map(arr.getJSONObject(it)) }
    }

    private fun <T> saveArr(key: String, list: List<T>, map: (T) -> JSONObject) {
        val arr = JSONArray()
        list.forEach { arr.put(map(it)) }
        p.edit().putString(key, arr.toString()).apply()
    }

    private fun apiKeySecret(id: String) = "provider_api_key_$id"
    private fun headersSecret(id: String) = "provider_headers_$id"
    private fun mcpHeadersSecret(id: String) = "mcp_headers_$id"
}
