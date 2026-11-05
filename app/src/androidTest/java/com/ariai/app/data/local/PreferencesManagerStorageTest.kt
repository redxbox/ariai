package com.ariai.app.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ariai.app.data.models.McpAuthType
import com.ariai.app.data.models.McpServerItem
import com.ariai.app.data.models.McpTransport
import com.ariai.app.data.models.OAuth2Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PreferencesManagerStorageTest {
    @Test
    fun implicitSearchDefaultMigratesToBraveOnlyOnceAndPreservesLaterChoice() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "prefs-search-default-${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        val preferences = PreferencesManager(context, store)

        try {
            preferences.migrateLocalSearchDefaultToBrave()
            assertEquals("brave", preferences.searchProviderFlow.first())

            preferences.setSearchProvider("bing")
            preferences.migrateLocalSearchDefaultToBrave()
            assertEquals("bing", preferences.searchProviderFlow.first())
        } finally {
            scope.cancel()
            file.delete()
        }
    }

    @Test
    fun explicitLegacyBingChoiceIsPreservedDuringDefaultMigration() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "prefs-search-explicit-${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        val preferences = PreferencesManager(context, store)

        try {
            preferences.setSearchProvider("bing")
            preferences.migrateLocalSearchDefaultToBrave()
            assertEquals("bing", preferences.searchProviderFlow.first())
        } finally {
            scope.cancel()
            file.delete()
        }
    }

    @Test
    fun searchKeysAreEncryptedAndMcpServerSettingsPersist() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "prefs-test-${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        val preferences = PreferencesManager(context, store)

        try {
            val searchKey = "test-search-secret-not-a-live-credential"
            preferences.setSearchApiKey("tavily", searchKey)
            val encrypted = store.data.first()[PreferencesManager.TAVILY_API_KEY].orEmpty()
            assertTrue(encrypted.startsWith("enc1:"))
            assertFalse(encrypted.contains(searchKey))
            assertEquals(searchKey, preferences.searchKeysFlow.first()["tavily"])
            assertEquals("tavily", preferences.searchProviderFlow.first())
            preferences.setSearchProvider("brave")
            assertEquals("brave", preferences.searchProviderFlow.first())

            val mcpSecret = "mcp-test-secret-never-used-live"
            val server = McpServerItem(
                id = "persisted-mcp-server",
                name = "Local test server",
                url = "http://localhost:8080/mcp",
                transport = McpTransport.STREAMABLE_HTTP,
                authType = McpAuthType.BEARER,
                authSecret = mcpSecret
            )
            preferences.updateMcpServers { it + server }
            assertEquals(listOf(server), preferences.mcpServersFlow.first())
            val encryptedMcpAuth = store.data.first()[PreferencesManager.MCP_SERVER_AUTH].orEmpty()
            assertTrue(encryptedMcpAuth.contains("\"ciphertext\":\"enc1:"))
            assertFalse(encryptedMcpAuth.contains(mcpSecret))

            preferences.updateMcpServers { current ->
                current.map { it.copy(enabled = false) }
            }
            assertFalse(preferences.mcpServersFlow.first().single().enabled)
            assertEquals(mcpSecret, preferences.mcpServersFlow.first().single().authSecret)

            preferences.updateMcpServers { current -> current.filterNot { it.id == server.id } }
            assertTrue(preferences.mcpServersFlow.first().isEmpty())
            assertTrue(store.data.first()[PreferencesManager.MCP_SERVER_AUTH].isNullOrBlank())
        } finally {
            scope.cancel()
            file.delete()
        }
    }

    @Test
    fun oauthMcpTokenStateIsEncryptedAndRefreshUsesCompareAndSet() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "prefs-mcp-oauth-${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        val preferences = PreferencesManager(context, store)

        try {
            val config = OAuth2Config(
                authorizationEndpoint = "https://identity.example.test/authorize",
                tokenEndpoint = "https://identity.example.test/token",
                clientId = "mcp-public-client",
                scopes = "tools.read"
            )
            val initialState = "opaque-old-oauth-state"
            val server = McpServerItem(
                id = "oauth-mcp-server",
                name = "OAuth MCP",
                url = "https://mcp.example.test/mcp",
                transport = McpTransport.STREAMABLE_HTTP,
                authType = McpAuthType.OAUTH2,
                authSecret = initialState,
                oauthConfig = config
            )
            preferences.updateMcpServers { it + server }
            val ciphertext = store.data.first()[PreferencesManager.MCP_SERVER_AUTH].orEmpty()
            assertTrue(ciphertext.contains("enc1:"))
            assertFalse(ciphertext.contains(initialState))
            assertEquals(initialState, preferences.getMcpOAuthState(server.id))

            assertTrue(preferences.updateMcpOAuthStateIfUnchanged(
                server.id, config, server.url, initialState, "opaque-refreshed-oauth-state"
            ))
            assertFalse(preferences.updateMcpOAuthStateIfUnchanged(
                server.id, config, server.url, initialState, "stale-refresh-must-not-overwrite"
            ))
            assertEquals("opaque-refreshed-oauth-state", preferences.getMcpOAuthState(server.id))
            assertFalse(preferences.replaceMcpOAuthStateForAuthorization(
                server.id,
                config.copy(clientId = "different-client"),
                server.url,
                "wrong-config-state"
            ))
            assertFalse(preferences.replaceMcpOAuthStateForAuthorization(
                server.id,
                config,
                "https://other.example.test/mcp",
                "wrong-resource-state"
            ))
        } finally {
            scope.cancel()
            file.delete()
        }
    }
}
