package com.ariai.app.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ariai.app.data.models.McpServerItem
import com.ariai.app.data.models.McpTransport
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

            val server = McpServerItem(
                id = "persisted-mcp-server",
                name = "Local preview server",
                url = "http://localhost:8080/mcp",
                transport = McpTransport.STREAMABLE_HTTP
            )
            preferences.updateMcpServers { it + server }
            assertEquals(listOf(server), preferences.mcpServersFlow.first())

            preferences.updateMcpServers { current ->
                current.map { it.copy(enabled = false) }
            }
            assertFalse(preferences.mcpServersFlow.first().single().enabled)

            preferences.updateMcpServers { current -> current.filterNot { it.id == server.id } }
            assertTrue(preferences.mcpServersFlow.first().isEmpty())
        } finally {
            scope.cancel()
            file.delete()
        }
    }
}
