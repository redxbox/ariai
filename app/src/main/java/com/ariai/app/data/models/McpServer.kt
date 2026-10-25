package com.ariai.app.data.models

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

/** Transports surfaced by the current MCP configuration preview. */
enum class McpTransport(val label: String) {
    SSE("SSE"),
    STREAMABLE_HTTP("Streamable HTTP")
}

/** Local MCP server metadata. Connections/tools are not active in this preview. */
data class McpServerItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val transport: McpTransport,
    val enabled: Boolean = true
)

/** Small, version-tolerant codec for the private DataStore preference. */
object McpServerCodec {
    private const val MAX_SERVERS = 50
    private const val MAX_NAME_LENGTH = 64
    private const val MAX_URL_LENGTH = 2_048

    fun encode(servers: List<McpServerItem>): String = JSONArray().apply {
        servers.take(MAX_SERVERS).forEach { server ->
            put(
                JSONObject()
                    .put("id", server.id.take(64))
                    .put("name", server.name.trim().take(MAX_NAME_LENGTH))
                    .put("url", server.url.trim().take(MAX_URL_LENGTH))
                    .put("transport", server.transport.name)
                    .put("enabled", server.enabled)
            )
        }
    }.toString()

    fun decode(value: String?): List<McpServerItem> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(value)
            (0 until minOf(array.length(), MAX_SERVERS)).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val id = item.optString("id").take(64).takeIf(String::isNotBlank) ?: return@mapNotNull null
                val name = item.optString("name").trim().take(MAX_NAME_LENGTH)
                    .takeIf(String::isNotBlank) ?: return@mapNotNull null
                val url = item.optString("url").trim().take(MAX_URL_LENGTH)
                    .takeIf(::isValidMcpServerUrl) ?: return@mapNotNull null
                val transport = runCatching { McpTransport.valueOf(item.optString("transport")) }
                    .getOrNull() ?: return@mapNotNull null
                McpServerItem(id, name, url, transport, item.optBoolean("enabled", true))
            }
        }.getOrDefault(emptyList())
    }
}

/** Accept only HTTP(S) endpoints with a host; credentials must not be hidden in the URL. */
fun isValidMcpServerUrl(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isBlank() || trimmed.length > 2_048) return false
    val uri = runCatching { URI(trimmed) }.getOrNull() ?: return false
    return uri.scheme?.lowercase() in setOf("http", "https") &&
        !uri.host.isNullOrBlank() &&
        uri.userInfo == null &&
        uri.rawQuery == null
}
