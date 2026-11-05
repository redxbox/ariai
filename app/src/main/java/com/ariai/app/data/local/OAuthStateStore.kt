package com.ariai.app.data.local

import com.ariai.app.data.models.McpAuthType
import com.ariai.app.data.models.OAuth2Config
import com.ariai.app.data.models.ProviderAuthType
import com.ariai.app.data.models.sameSettingsAs
import com.ariai.app.util.KeyVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Encrypted token-state storage shared by provider and MCP OAuth clients. */
class OAuthStateStore(
    private val database: AppDatabase,
    private val preferences: PreferencesManager
) {
    suspend fun providerState(providerId: String, config: OAuth2Config, resourceUri: String): String? = withContext(Dispatchers.IO) {
        val entity = database.providerDao().getProviderById(providerId) ?: return@withContext null
        if (entity.authType != ProviderAuthType.OAUTH2.name || !entity.matchesOAuthConfig(config) ||
            entity.baseUrl != resourceUri.trim()
        ) {
            return@withContext null
        }
        entity.oauthState?.let(KeyVault::decryptOrNull)?.takeIf(String::isNotBlank)
    }

    suspend fun updateProviderStateIfUnchanged(
        providerId: String,
        config: OAuth2Config,
        resourceUri: String,
        expectedState: String,
        replacementState: String
    ): Boolean = withContext(Dispatchers.IO) {
        val entity = database.providerDao().getProviderById(providerId) ?: return@withContext false
        if (entity.authType != ProviderAuthType.OAUTH2.name || !entity.matchesOAuthConfig(config) ||
            entity.baseUrl != resourceUri.trim()
        ) {
            return@withContext false
        }
        val current = entity.oauthState?.let(KeyVault::decryptOrNull) ?: return@withContext false
        if (current != expectedState) return@withContext false
        val normalized = config.requireValid()
        database.providerDao().updateOAuthStateIfUnchanged(
            id = providerId,
            resourceUri = resourceUri.trim(),
            authorizationEndpoint = normalized.authorizationEndpoint,
            tokenEndpoint = normalized.tokenEndpoint,
            clientId = normalized.clientId,
            scopes = normalized.scopes,
            redirectUri = normalized.redirectUri,
            oldCiphertext = entity.oauthState,
            newCiphertext = KeyVault.encrypt(replacementState)
        ) == 1
    }

    suspend fun completeProviderAuthorization(
        providerId: String,
        config: OAuth2Config,
        resourceUri: String,
        state: String
    ): Boolean = withContext(Dispatchers.IO) {
        val normalized = config.requireValid()
        database.providerDao().replaceOAuthStateForAuthorization(
            id = providerId,
            resourceUri = resourceUri.trim(),
            authorizationEndpoint = normalized.authorizationEndpoint,
            tokenEndpoint = normalized.tokenEndpoint,
            clientId = normalized.clientId,
            scopes = normalized.scopes,
            redirectUri = normalized.redirectUri,
            ciphertext = KeyVault.encrypt(state)
        ) == 1
    }

    suspend fun mcpState(serverId: String, config: OAuth2Config, resourceUri: String): String? =
        preferences.mcpServersFlow.first().firstOrNull {
            it.id == serverId && it.authType == McpAuthType.OAUTH2 &&
                it.oauthConfig.sameSettingsAs(config) && it.url == resourceUri.trim()
        }
            ?.authSecret
            ?.takeIf(String::isNotBlank)

    suspend fun updateMcpStateIfUnchanged(
        serverId: String,
        config: OAuth2Config,
        resourceUri: String,
        expectedState: String,
        replacementState: String
    ): Boolean = preferences.updateMcpOAuthStateIfUnchanged(
        serverId, config, resourceUri.trim(), expectedState, replacementState
    )

    suspend fun completeMcpAuthorization(
        serverId: String,
        config: OAuth2Config,
        resourceUri: String,
        state: String
    ): Boolean = preferences.replaceMcpOAuthStateForAuthorization(serverId, config, resourceUri.trim(), state)
}

private fun ProviderEntity.matchesOAuthConfig(config: OAuth2Config): Boolean {
    val normalized = config.requireValid()
    return oauthAuthorizationEndpoint == normalized.authorizationEndpoint &&
        oauthTokenEndpoint == normalized.tokenEndpoint &&
        oauthClientId == normalized.clientId &&
        oauthScopes == normalized.scopes &&
        oauthRedirectUri == normalized.redirectUri
}
