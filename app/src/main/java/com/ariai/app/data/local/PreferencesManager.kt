package com.ariai.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "ariai_prefs")

class PreferencesManager(private val context: Context) {
    
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
        val STREAM_RESPONSE = booleanPreferencesKey("stream_response")
        val SHOW_REASONING = booleanPreferencesKey("show_reasoning")
        val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        val AUTO_TITLE = booleanPreferencesKey("auto_title")
        val FONT_SIZE = intPreferencesKey("font_size")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { it[LANGUAGE] ?: "en" }
    val themeFlow: Flow<String> = context.dataStore.data.map { it[THEME] ?: "system" }
    val dynamicColorFlow: Flow<Boolean> = context.dataStore.data.map { it[DYNAMIC_COLOR] ?: true }
    val defaultProviderFlow: Flow<String?> = context.dataStore.data.map { it[DEFAULT_PROVIDER_ID] }
    val defaultModelFlow: Flow<String?> = context.dataStore.data.map { it[DEFAULT_MODEL_ID] }
    val streamResponseFlow: Flow<Boolean> = context.dataStore.data.map { it[STREAM_RESPONSE] ?: true }
    val memoryEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[MEMORY_ENABLED] ?: true }
    val firstLaunchFlow: Flow<Boolean> = context.dataStore.data.map { it[FIRST_LAUNCH] ?: true }

    val searchKeysFlow: Flow<Map<String, String>> = context.dataStore.data.map { prefs ->
        mapOf(
            "tavily" to (prefs[TAVILY_API_KEY] ?: ""),
            "brave" to (prefs[BRAVE_API_KEY] ?: ""),
            "exa" to (prefs[EXA_API_KEY] ?: ""),
            "serper" to (prefs[SERPER_API_KEY] ?: "")
        )
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[LANGUAGE] = lang }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[THEME] = theme }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun setDefaultProvider(id: String) {
        context.dataStore.edit { it[DEFAULT_PROVIDER_ID] = id }
    }

    suspend fun setDefaultModel(id: String) {
        context.dataStore.edit { it[DEFAULT_MODEL_ID] = id }
    }

    suspend fun setSearchApiKey(provider: String, key: String) {
        val prefKey = when (provider.lowercase()) {
            "tavily" -> TAVILY_API_KEY
            "brave" -> BRAVE_API_KEY
            "exa" -> EXA_API_KEY
            "serper" -> SERPER_API_KEY
            else -> SEARCH_API_KEY
        }
        context.dataStore.edit { it[prefKey] = key }
    }

    suspend fun setFirstLaunchDone() {
        context.dataStore.edit { it[FIRST_LAUNCH] = false }
    }
}
