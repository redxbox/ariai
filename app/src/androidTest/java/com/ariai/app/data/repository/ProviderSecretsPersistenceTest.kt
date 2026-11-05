package com.ariai.app.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ariai.app.data.local.AppDatabase
import com.ariai.app.data.local.Converters
import com.ariai.app.data.local.OAuthStateStore
import com.ariai.app.data.local.PreferencesManager
import com.ariai.app.data.local.ProviderEntity
import com.ariai.app.data.models.AIModel
import com.ariai.app.data.models.OAuth2Config
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderAuthType
import com.ariai.app.data.models.ProviderType
import com.ariai.app.data.remote.AIClient
import com.ariai.app.data.remote.SearchClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProviderSecretsPersistenceTest {
    @Test
    fun providerCredentialsAndCustomRequestConfigurationAreEncryptedAtRest() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repository = ChatRepository(database, AIClient(), SearchClient())
            val providerId = "encrypted-provider"
            val provider = Provider(
                id = providerId,
                name = "Encrypted test provider",
                type = ProviderType.OPENAI_COMPATIBLE,
                baseUrl = "https://example.com/v1",
                apiKey = "provider-api-secret",
                customHeaders = mapOf("Authorization" to "Bearer custom-header-secret"),
                customBody = """{"metadata_key":"custom-body-secret"}""",
                models = listOf(
                    AIModel(
                        id = "model-1",
                        displayName = "Model 1",
                        providerId = providerId,
                        headers = mapOf("X-Model-Key" to "model-header-secret")
                    )
                )
            )

            assertTrue(repository.saveProvider(provider))
            val raw = database.providerDao().getProviderById(providerId)!!
            assertTrue(raw.apiKey.startsWith("enc1:"))
            assertTrue(raw.customHeaders.startsWith("enc1:"))
            assertTrue(raw.customBody.orEmpty().startsWith("enc1:"))
            assertTrue(raw.modelsJson.startsWith("enc1:"))
            assertFalse(raw.apiKey.contains("provider-api-secret"))
            assertFalse(raw.customHeaders.contains("custom-header-secret"))
            assertFalse(raw.customBody.orEmpty().contains("custom-body-secret"))
            assertFalse(raw.modelsJson.contains("model-header-secret"))

            val restored = repository.getProviderById(providerId)!!
            assertEquals(provider.apiKey, restored.apiKey)
            assertEquals(provider.customHeaders, restored.customHeaders)
            assertEquals(provider.customBody, restored.customBody)
            assertEquals(provider.models.single().headers, restored.models.single().headers)
        } finally {
            database.close()
        }
    }

    @Test
    fun oauthTokenStateIsEncryptedAndBoundToPersistedProviderSettings() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repository = ChatRepository(database, AIClient(), SearchClient())
            val config = OAuth2Config(
                authorizationEndpoint = "https://identity.example.test/authorize",
                tokenEndpoint = "https://identity.example.test/token",
                clientId = "public-client",
                scopes = "models.read"
            )
            val state = "oauth-refresh-token-never-plaintext"
            val provider = Provider(
                id = "oauth-provider",
                name = "OAuth provider",
                type = ProviderType.OPENAI_COMPATIBLE,
                baseUrl = "https://api.example.test/v1",
                apiKey = "unused-static-key",
                authType = ProviderAuthType.OAUTH2,
                oauthConfig = config,
                oauthStateJson = state
            )

            assertTrue(repository.saveProvider(provider))
            val raw = database.providerDao().getProviderById(provider.id)!!
            assertEquals(ProviderAuthType.OAUTH2.name, raw.authType)
            assertTrue(raw.oauthState.orEmpty().startsWith("enc1:"))
            assertFalse(raw.oauthState.orEmpty().contains(state))
            assertEquals(config.authorizationEndpoint, raw.oauthAuthorizationEndpoint)
            assertEquals(config.clientId, raw.oauthClientId)

            val restored = repository.getProviderById(provider.id)!!
            assertEquals(state, restored.oauthStateJson)
            assertEquals(config, restored.oauthConfig)
            assertEquals(provider.apiKey, restored.apiKey)

            val stateStore = OAuthStateStore(database, PreferencesManager(context))
            assertEquals(state, stateStore.providerState(provider.id, config, provider.baseUrl))
            assertNull(stateStore.providerState(provider.id, config, "https://different.example.test/v1"))
            assertFalse(stateStore.completeProviderAuthorization(
                provider.id, config, "https://different.example.test/v1", "wrong-resource-state"
            ))
            assertEquals(state, repository.getProviderById(provider.id)?.oauthStateJson)
        } finally {
            database.close()
        }
    }

    @Test
    fun lockedCiphertextIsPreservedWhenSavingUnrelatedProviderSettings() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repository = ChatRepository(database, AIClient(), SearchClient())
            val providerId = "locked-provider"
            val lockedHeaders = "enc1:invalid-headers-ciphertext"
            val lockedBody = "enc1:invalid-body-ciphertext"
            val lockedModels = "enc1:invalid-models-ciphertext"
            database.providerDao().insertProvider(
                ProviderEntity(
                    id = providerId,
                    name = "Locked provider",
                    type = ProviderType.OPENAI_COMPATIBLE.name,
                    baseUrl = "https://example.com/v1",
                    apiKey = "",
                    enabled = true,
                    customHeaders = lockedHeaders,
                    customBody = lockedBody,
                    createdAt = 1L,
                    modelsJson = lockedModels
                )
            )

            val lockedProvider = repository.getProviderById(providerId)!!
            assertTrue(lockedProvider.providerConfigNeedsReentry)
            assertTrue(repository.saveProvider(lockedProvider.copy(enabled = false)))

            val afterSave = database.providerDao().getProviderById(providerId)!!
            assertEquals(lockedHeaders, afterSave.customHeaders)
            assertEquals(lockedBody, afterSave.customBody)
            assertEquals(lockedModels, afterSave.modelsJson)
            assertFalse(afterSave.enabled)
        } finally {
            database.close()
        }
    }

    @Test
    fun legacyPlaintextProviderConfigurationIsMigratedWithoutDataLoss() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repository = ChatRepository(database, AIClient(), SearchClient())
            val providerId = "legacy-provider"
            val model = AIModel(
                id = "legacy-model",
                displayName = "Legacy model",
                providerId = providerId,
                headers = mapOf("X-Legacy-Key" to "legacy-model-secret")
            )
            database.providerDao().insertProvider(
                ProviderEntity(
                    id = providerId,
                    name = "Legacy provider",
                    type = ProviderType.OPENAI_COMPATIBLE.name,
                    baseUrl = "https://example.com/v1",
                    apiKey = "legacy-api-secret",
                    enabled = true,
                    customHeaders = Converters().fromStringMap(mapOf("Authorization" to "Bearer legacy-header-secret")),
                    customBody = """{"legacy_body_key":"legacy-body-secret"}""",
                    createdAt = 1L,
                    modelsJson = com.ariai.app.data.models.modelsToJson(listOf(model))
                )
            )

            repository.migrateProviderKeysToEncrypted()
            val raw = database.providerDao().getProviderById(providerId)!!
            assertTrue(raw.apiKey.startsWith("enc1:"))
            assertTrue(raw.customHeaders.startsWith("enc1:"))
            assertTrue(raw.customBody.orEmpty().startsWith("enc1:"))
            assertTrue(raw.modelsJson.startsWith("enc1:"))
            assertFalse(raw.apiKey.contains("legacy-api-secret"))
            assertFalse(raw.customHeaders.contains("legacy-header-secret"))
            assertFalse(raw.customBody.orEmpty().contains("legacy-body-secret"))
            assertFalse(raw.modelsJson.contains("legacy-model-secret"))

            val restored = repository.getProviderById(providerId)!!
            assertEquals("legacy-api-secret", restored.apiKey)
            assertEquals(mapOf("Authorization" to "Bearer legacy-header-secret"), restored.customHeaders)
            assertEquals("""{"legacy_body_key":"legacy-body-secret"}""", restored.customBody)
            assertEquals("legacy-model-secret", restored.models.single().headers["X-Legacy-Key"])
        } finally {
            database.close()
        }
    }

    @Test
    fun staleSecretMigrationDoesNotOverwriteAConcurrentProviderUpdate() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val providerId = "migration-race-provider"
            val stale = ProviderEntity(
                id = providerId,
                name = "Provider",
                type = ProviderType.OPENAI_COMPATIBLE.name,
                baseUrl = "https://example.com/v1",
                apiKey = "old-api-key",
                enabled = true,
                customHeaders = "{}",
                customBody = null,
                createdAt = 1L,
                modelsJson = "[]"
            )
            database.providerDao().insertProvider(stale)
            database.providerDao().insertProvider(stale.copy(apiKey = "new-api-key"))

            val migratedRows = database.providerDao().migrateProviderSecretsIfUnchanged(
                id = stale.id,
                oldApiKey = stale.apiKey,
                oldCustomHeaders = stale.customHeaders,
                oldCustomBody = stale.customBody,
                oldModelsJson = stale.modelsJson,
                oldOauthState = stale.oauthState,
                apiKey = "enc1:old",
                customHeaders = "enc1:headers",
                customBody = null,
                modelsJson = "enc1:models",
                oauthState = null
            )

            assertEquals(0, migratedRows)
            assertEquals("new-api-key", database.providerDao().getProviderById(providerId)?.apiKey)
        } finally {
            database.close()
        }
    }
}
