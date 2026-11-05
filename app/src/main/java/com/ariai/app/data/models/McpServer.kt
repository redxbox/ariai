package com.ariai.app.data.models

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

/** MCP transports supported by AriAI's remote-client implementation. */
enum class McpTransport(val label: String) {
    SSE("SSE (legacy)"),
    STREAMABLE_HTTP("Streamable HTTP")
}

enum class McpAuthType {
    NONE,
    BEARER,
    CUSTOM_HEADER,
    OAUTH2
}

/**
 * A remote MCP server configuration. [authSecret] exists only in memory; PreferencesManager
 * stores it separately after Android-Keystore encryption and McpServerCodec never serializes it.
 */
data class McpServerItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val transport: McpTransport,
    val enabled: Boolean = true,
    val authType: McpAuthType = McpAuthType.NONE,
    val authHeaderName: String = "X-API-Key",
    /** Static credentials or encrypted OAuth AuthState JSON, hydrated only in memory. */
    val authSecret: String? = null,
    val allowedToolNames: Set<String> = emptySet(),
    val oauthConfig: OAuth2Config? = null,
    /** In-memory command used to clear an otherwise preserved encrypted secret during edits. */
    val clearAuthSecret: Boolean = false
)

/** Small, version-tolerant codec for non-secret MCP server metadata. */
object McpServerCodec {
    private const val MAX_SERVERS = 50
    private const val MAX_NAME_LENGTH = 64
    private const val MAX_URL_LENGTH = 2_048
    private const val MAX_ID_LENGTH = 64
    private const val MAX_HEADER_LENGTH = 128
    private const val MAX_TOOL_NAME_LENGTH = 128
    private const val MAX_TOOLS_PER_SERVER = 16

    fun encode(servers: List<McpServerItem>): String = JSONArray().apply {
        servers.take(MAX_SERVERS).forEach { server ->
            put(
                JSONObject()
                    .put("id", server.id.take(MAX_ID_LENGTH))
                    .put("name", server.name.trim().take(MAX_NAME_LENGTH))
                    .put("url", server.url.trim().take(MAX_URL_LENGTH))
                    .put("transport", server.transport.name)
                    .put("enabled", server.enabled)
                    .put("authType", server.authType.name)
                    .put("authHeaderName", server.authHeaderName.trim().take(MAX_HEADER_LENGTH))
                    .put("allowedTools", JSONArray(server.allowedToolNames.take(MAX_TOOLS_PER_SERVER)
                        .map { it.take(MAX_TOOL_NAME_LENGTH) }.filter(String::isNotBlank)))
                    .apply { server.oauthConfig?.let { put("oauthConfig", it.toJson()) } }
            )
        }
    }.toString()

    fun decode(value: String?): List<McpServerItem> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(value)
            (0 until minOf(array.length(), MAX_SERVERS)).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val id = item.optString("id").take(MAX_ID_LENGTH).takeIf(String::isNotBlank)
                    ?: return@mapNotNull null
                val name = item.optString("name").trim().take(MAX_NAME_LENGTH)
                    .takeIf(String::isNotBlank) ?: return@mapNotNull null
                val url = item.optString("url").trim().take(MAX_URL_LENGTH)
                    .takeIf(::isValidStoredMcpServerUrl) ?: return@mapNotNull null
                val transport = runCatching { McpTransport.valueOf(item.optString("transport")) }
                    .getOrNull() ?: return@mapNotNull null
                val authType = runCatching { McpAuthType.valueOf(item.optString("authType", "NONE")) }
                    .getOrDefault(McpAuthType.NONE)
                val headerName = item.optString("authHeaderName", "X-API-Key")
                    .trim().take(MAX_HEADER_LENGTH).ifBlank { "X-API-Key" }
                if (authType == McpAuthType.CUSTOM_HEADER && !isValidMcpHeaderName(headerName)) {
                    return@mapNotNull null
                }
                val oauthConfig = if (authType == McpAuthType.OAUTH2) {
                    OAuth2Config.fromJson(item.optJSONObject("oauthConfig") ?: return@mapNotNull null)
                        ?: return@mapNotNull null
                } else null
                val allowedTools = item.optJSONArray("allowedTools")?.let { names ->
                    buildSet {
                        for (toolIndex in 0 until minOf(names.length(), MAX_TOOLS_PER_SERVER)) {
                            val toolName = names.optString(toolIndex).trim().take(MAX_TOOL_NAME_LENGTH)
                            if (toolName.isNotBlank() && toolName.none { it.code < 0x20 }) add(toolName)
                        }
                    }
                }.orEmpty()
                McpServerItem(
                    id = id,
                    name = name,
                    url = url,
                    transport = transport,
                    enabled = item.optBoolean("enabled", true),
                    authType = authType,
                    authHeaderName = headerName,
                    authSecret = null,
                    allowedToolNames = allowedTools,
                    oauthConfig = oauthConfig
                )
            }
        }.getOrDefault(emptyList())
    }
}

/** Stores only per-server ciphertext in DataStore, so one lost credential does not block edits. */
object McpServerAuthCipherCodec {
    private const val MAX_SERVERS = 50
    private const val MAX_CIPHERTEXT_LENGTH = 64_000

    fun encode(ciphertexts: Map<String, String>): String = JSONArray().apply {
        ciphertexts.entries.take(MAX_SERVERS).forEach { (id, value) ->
            if (id.isNotBlank() && value.startsWith("enc1:")) {
                put(JSONObject().put("id", id.take(64)).put("ciphertext", value.take(MAX_CIPHERTEXT_LENGTH)))
            }
        }
    }.toString()

    fun decode(value: String?): Map<String, String> {
        if (value.isNullOrBlank()) return emptyMap()
        return runCatching {
            val array = JSONArray(value)
            buildMap {
                for (index in 0 until minOf(array.length(), MAX_SERVERS)) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").take(64).takeIf(String::isNotBlank) ?: continue
                    val ciphertext = item.optString("ciphertext").take(MAX_CIPHERTEXT_LENGTH)
                        .takeIf { it.startsWith("enc1:") } ?: continue
                    put(id, ciphertext)
                }
            }
        }.getOrDefault(emptyMap())
    }
}

/** Preserve older saved endpoint values while still requiring a well-formed credential-free URL. */
private fun isValidStoredMcpServerUrl(value: String): Boolean {
    val uri = runCatching { URI(value.trim()) }.getOrNull() ?: return false
    return uri.scheme?.lowercase() in setOf("http", "https") &&
        !uri.host.isNullOrBlank() && uri.userInfo == null && uri.rawQuery == null && uri.rawFragment == null
}

/** Accept HTTPS endpoints, and cleartext only for loopback development endpoints. */
fun isValidMcpServerUrl(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isBlank() || trimmed.length > 2_048) return false
    val uri = runCatching { URI(trimmed) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase()
    val host = uri.host?.lowercase()?.removeSurrounding("[", "]")
    val loopback = host in setOf("localhost", "127.0.0.1", "::1", "10.0.2.2")
    return (scheme == "https" || (scheme == "http" && loopback)) &&
        !host.isNullOrBlank() &&
        uri.userInfo == null &&
        uri.rawQuery == null &&
        uri.rawFragment == null
}

/** Prevents control-character injection and invalid custom HTTP-header names. */
fun isValidMcpHeaderName(value: String): Boolean =
    value.length in 1..128 && value.matches(Regex("^[!#$%&'*+.^_`|~0-9A-Za-z-]+$"))
