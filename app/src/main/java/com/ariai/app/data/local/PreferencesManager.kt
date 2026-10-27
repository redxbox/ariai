package com.ariai.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.ariai.app.data.models.McpServerCodec
import com.ariai.app.data.models.McpServerItem
import com.ariai.app.util.KeyVault

val Context.dataStore by preferencesDataStore(name = "ariai_prefs")

class PreferencesManager(
    context: Context,
    private val dataStore: DataStore<Preferences> = context.dataStore
) {
    
    companion object {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme") // light, dark, system
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_PROVIDER_ID = stringPreferencesKey("default_provider_id")
        val DEFAULT_MODEL_ID = stringPreferencesKey("default_model_id")
        val SEARCH_PROVIDER = stringPreferencesKey("search_provider")
        val SEARCH_API_KEY = stringPreferencesKey("search_api_key")
        val TAVILY_API_KEY = stringPreferencesKey("tavily_api_key")
        val BRAVE_API_KEY = stringPreferencesKey("brave_api_key")
        val EXA_API_KEY = stringPreferencesKey("exa_api_key")
        val SERPER_API_KEY = stringPreferencesKey("serper_api_key")
        val MCP_SERVERS = stringPreferencesKey("mcp_servers")
        val STREAM_RESPONSE = booleanPreferencesKey("stream_response")
        val SHOW_REASONING = booleanPreferencesKey("show_reasoning")
        val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        val AUTO_TITLE = booleanPreferencesKey("auto_title")
        val FONT_SIZE = intPreferencesKey("font_size")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val REASONING_LEVEL = stringPreferencesKey("reasoning_level")
        val SEARCH_MODE = stringPreferencesKey("search_mode")
        val FAVORITE_MODELS = stringSetPreferencesKey("favorite_models")
    }

    val languageFlow: Flow<String> = dataStore.data.map { it[LANGUAGE] ?: "en" }
    val themeFlow: Flow<String> = dataStore.data.map { it[THEME] ?: "system" }
    val dynamicColorFlow: Flow<Boolean> = dataStore.data.map { it[DYNAMIC_COLOR] ?: false }
    val defaultProviderFlow: Flow<String?> = dataStore.data.map { it[DEFAULT_PROVIDER_ID] }
    val defaultModelFlow: Flow<String?> = dataStore.data.map { it[DEFAULT_MODEL_ID] }
    val streamResponseFlow: Flow<Boolean> = dataStore.data.map { it[STREAM_RESPONSE] ?: true }
    val memoryEnabledFlow: Flow<Boolean> = dataStore.data.map { it[MEMORY_ENABLED] ?: true }
    val fontSizeFlow: Flow<Int> = dataStore.data.map { it[FONT_SIZE] ?: 15 }
    val firstLaunchFlow: Flow<Boolean> = dataStore.data.map { it[FIRST_LAUNCH] ?: true }
    val reasoningLevelFlow: Flow<String?> = dataStore.data.map { it[REASONING_LEVEL] }
    val searchModeFlow: Flow<String?> = dataStore.data.map { it[SEARCH_MODE] }
    val searchProviderFlow: Flow<String?> = dataStore.data.map { it[SEARCH_PROVIDER] }
    val favoriteModelsFlow: Flow<Set<String>> = dataStore.data.map { it[FAVORITE_MODELS] ?: emptySet() }

    val searchKeysFlow: Flow<Map<String, String>> = dataStore.data.map { prefs ->
        mapOf(
            "tavily" to KeyVault.decrypt(prefs[TAVILY_API_KEY] ?: ""),
            "brave" to KeyVault.decrypt(prefs[BRAVE_API_KEY] ?: ""),
            "exa" to KeyVault.decrypt(prefs[EXA_API_KEY] ?: ""),
            "serper" to KeyVault.decrypt(prefs[SERPER_API_KEY] ?: "")
        )
    }
    val mcpServersFlow: Flow<List<McpServerItem>> = dataStore.data.map { prefs ->
        McpServerCodec.decode(prefs[MCP_SERVERS])
    }

    /** Atomically persists MCP server metadata for the local configuration preview. */
    suspend fun updateMcpServers(transform: (List<McpServerItem>) -> List<McpServerItem>) {
        dataStore.edit { prefs ->
            val current = McpServerCodec.decode(prefs[MCP_SERVERS])
            val updated = runCatching { transform(current) }.getOrDefault(current)
            prefs[MCP_SERVERS] = McpServerCodec.encode(updated)
        }
    }

    suspend fun setLanguage(lang: String) {
        dataStore.edit { it[LANGUAGE] = lang }
    }

    suspend fun setTheme(theme: String) {
        dataStore.edit { it[THEME] = theme }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun setDefaultProvider(id: String) {
        dataStore.edit { it[DEFAULT_PROVIDER_ID] = id }
    }

    suspend fun setDefaultModel(id: String) {
        dataStore.edit { it[DEFAULT_MODEL_ID] = id }
    }

    suspend fun setSearchApiKey(provider: String, key: String) {
        val normalizedProvider = provider.trim().lowercase()
        val prefKey = when (normalizedProvider) {
            "tavily" -> TAVILY_API_KEY
            "brave" -> BRAVE_API_KEY
            "exa" -> EXA_API_KEY
            "serper" -> SERPER_API_KEY
            else -> SEARCH_API_KEY
        }
        val trimmed = key.trim()
        dataStore.edit { prefs ->
            prefs[prefKey] = KeyVault.encrypt(trimmed)
            if (trimmed.isNotBlank() && normalizedProvider in setOf("tavily", "brave", "exa", "serper") &&
                prefs[SEARCH_PROVIDER].isNullOrBlank()
            ) {
                prefs[SEARCH_PROVIDER] = normalizedProvider
            }
        }
    }

    suspend fun setSearchProvider(provider: String) {
        val normalized = provider.trim().lowercase()
        if (normalized in setOf("tavily", "brave", "exa", "serper")) {
            dataStore.edit { it[SEARCH_PROVIDER] = normalized }
        }
    }

    /** Encrypts legacy plain-text keys in place; already-encrypted values are left untouched. */
    suspend fun migrateSearchKeysToEncrypted() {
        dataStore.edit { prefs ->
            listOf(TAVILY_API_KEY, BRAVE_API_KEY, EXA_API_KEY, SERPER_API_KEY, SEARCH_API_KEY).forEach { key ->
                val raw = prefs[key] ?: return@forEach
                if (raw.isNotBlank() && !raw.startsWith("enc1:")) {
                    runCatching { KeyVault.encrypt(raw) }
                        .onSuccess { prefs[key] = it }
                        .onFailure { com.ariai.app.util.AppLog.warn(it, "search key migration") }
                }
            }
        }
    }

    suspend fun setFirstLaunchDone() {
        dataStore.edit { it[FIRST_LAUNCH] = false }
    }

    suspend fun setDefaultSelection(providerId: String, modelId: String) {
        dataStore.edit {
            it[DEFAULT_PROVIDER_ID] = providerId
            it[DEFAULT_MODEL_ID] = modelId
        }
    }

    suspend fun setStreamResponse(enabled: Boolean) {
        dataStore.edit { it[STREAM_RESPONSE] = enabled }
    }

    suspend fun setFontSize(size: Int) {
        dataStore.edit { it[FONT_SIZE] = size }
    }

    suspend fun setReasoningLevel(name: String) {
        dataStore.edit { it[REASONING_LEVEL] = name }
    }

    suspend fun setSearchMode(name: String) {
        dataStore.edit { it[SEARCH_MODE] = name }
    }

    suspend fun toggleFavoriteModel(key: String) {
        dataStore.edit { prefs ->
            val current = prefs[FAVORITE_MODELS] ?: emptySet()
            prefs[FAVORITE_MODELS] = if (key in current) current - key else current + key
        }
    }
}
