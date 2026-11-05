package com.ariai.app.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.ariai.app.data.models.Provider
import com.ariai.app.data.remote.ConnectionResult

/** Opens the responsive provider-and-model picker shown from the chat model control. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerSheet(
    providers: List<Provider>,
    selectedProviderId: String?,
    selectedModelId: String?,
    favorites: Set<String>,
    modelUsageCounts: Map<Pair<String, String>, Int> = emptyMap(),
    onToggleFavorite: (providerId: String, modelId: String) -> Unit,
    onSelect: (providerId: String, modelId: String) -> Unit,
    onAddProvider: () -> Unit,
    onManageProviders: () -> Unit,
    onCheckProvider: suspend (Provider) -> ConnectionResult,
    onDismiss: () -> Unit
) {
    ModelPickerWorkspace(
        providers = providers,
        selectedProviderId = selectedProviderId,
        selectedModelId = selectedModelId,
        favorites = favorites,
        modelUsageCounts = modelUsageCounts,
        onToggleFavorite = onToggleFavorite,
        onSelect = onSelect,
        onAddProvider = onAddProvider,
        onManageProviders = onManageProviders,
        onCheckProvider = onCheckProvider,
        onDismiss = onDismiss
    )
}
