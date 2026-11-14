package com.ariai.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.ariai.app.data.models.AIModel
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import com.ariai.app.data.models.favoriteKey
import com.ariai.app.data.remote.ConnectionResult
import com.ariai.app.ui.components.ProviderBrandMark
import com.ariai.app.ui.theme.*
import com.ariai.app.util.tx
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val OTHER_MODELS_KEY = "other-models"

private data class PickerModelEntry(
    val provider: Provider,
    val model: AIModel
)

private data class PickerModelGroup(
    val key: String,
    val title: String,
    val entries: List<PickerModelEntry>
)

/**
 * Responsive model chooser. Wide layouts use the two-panel provider/model view; narrow phones use
 * a compact provider rail and a two-column grid of floating model-family menus.
 */
@Composable
internal fun ModelPickerWorkspace(
    providers: List<Provider>,
    selectedProviderId: String?,
    selectedModelId: String?,
    favorites: Set<String>,
    modelUsageCounts: Map<Pair<String, String>, Int>,
    onToggleFavorite: (providerId: String, modelId: String) -> Unit,
    onSelect: (providerId: String, modelId: String) -> Unit,
    onAddProvider: () -> Unit,
    onManageProviders: () -> Unit,
    onCheckProvider: suspend (Provider) -> ConnectionResult,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var requestedProviderFilter by remember(selectedProviderId, providers) {
        mutableStateOf(selectedProviderId?.takeIf { id -> providers.any { it.id == id && it.enabled } })
    }
    var favoritesExpanded by remember { mutableStateOf(false) }
    var openGroupPopupKey by remember(requestedProviderFilter) { mutableStateOf<String?>(null) }
    var expandedGroups by remember(requestedProviderFilter) { mutableStateOf(emptySet<String>()) }

    val enabledProviders = providers.filter { it.enabled }
    val providerFilter = requestedProviderFilter?.takeIf { id -> enabledProviders.any { it.id == id } }
    val selectedProvider = enabledProviders.firstOrNull { it.id == providerFilter }
    val totalModelCount = enabledProviders.sumOf { it.models.size }

    fun rankedEntries(sourceProviders: List<Provider>): List<PickerModelEntry> = sourceProviders
        .flatMap { provider -> provider.models.map { PickerModelEntry(provider, it) } }
        .sortedWith(
            compareByDescending<PickerModelEntry> { modelUsageCounts[it.provider.id to it.model.id] ?: 0 }
                .thenByDescending { favorites.contains(favoriteKey(it.provider.id, it.model.id)) }
                .thenByDescending { isSelectedModel(it.provider.id, it.model.id, selectedProviderId, selectedModelId) }
        )

    val providersForModels = if (providerFilter == null) enabledProviders else enabledProviders.filter { it.id == providerFilter }
    val matchingEntries = rankedEntries(providersForModels).filter { entry ->
        query.isBlank() ||
            entry.model.displayName.contains(query, ignoreCase = true) ||
            entry.model.id.contains(query, ignoreCase = true) ||
            entry.provider.name.contains(query, ignoreCase = true) ||
            modelFamilyTitle(entry.model).contains(query, ignoreCase = true)
    }
    val groups = matchingEntries
        .groupBy { modelFamilyKey(it.model) }
        .map { (key, entries) -> PickerModelGroup(key, modelFamilyTitle(entries.first().model), entries) }
        .sortedWith(
            compareByDescending<PickerModelGroup> { group ->
                group.entries.maxOfOrNull { modelUsageCounts[it.provider.id to it.model.id] ?: 0 } ?: 0
            }
                .thenBy { modelFamilyOrder(it.key) }
                .thenBy { it.title }
        )
    val allFavoriteEntries = if (providerFilter == null) {
        rankedEntries(enabledProviders).filter {
            favorites.contains(favoriteKey(it.provider.id, it.model.id)) &&
                (query.isBlank() ||
                    it.model.displayName.contains(query, ignoreCase = true) ||
                    it.model.id.contains(query, ignoreCase = true) ||
                    it.provider.name.contains(query, ignoreCase = true) ||
                    modelFamilyTitle(it.model).contains(query, ignoreCase = true))
        }
    } else {
        emptyList()
    }

    // Keep the dialog aligned with the opener: Android dialog windows can briefly inherit the
    // device direction before Compose applies the app direction, causing a visible RTL/LTR flip.
    val currentLayoutDirection = LocalLayoutDirection.current
    val pickerLayoutDirection = remember { currentLayoutDirection }
    CompositionLocalProvider(LocalLayoutDirection provides pickerLayoutDirection) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
            SideEffect { dialogWindow?.setDimAmount(0.08f) }
            Box(
                modifier = Modifier.fillMaxSize().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
            ) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val isWide = maxWidth >= 720.dp
                    val panelTop = (maxHeight * 0.16f).coerceIn(48.dp, 228.dp)
                    val providerPanelOffset = (maxHeight * 0.04f).coerceIn(16.dp, 32.dp)
                    val compactPanelHeight = minOf(
                        maxHeight * 0.60f,
                        900.dp,
                        (maxHeight - panelTop - 64.dp).coerceAtLeast(180.dp)
                    )
                    val wideProviderHeight = minOf(
                        maxHeight * 0.45f,
                        900.dp,
                        (maxHeight - panelTop - 64.dp).coerceAtLeast(180.dp)
                    )
                    val wideModelHeight = minOf(
                        maxHeight * 0.52f,
                        900.dp,
                        (maxHeight - panelTop - providerPanelOffset - 64.dp).coerceAtLeast(180.dp)
                    )
                    val compactPanelModifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = panelTop)
                        .fillMaxWidth(0.88f)
                        .height(compactPanelHeight)
                    val wideRowModifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = panelTop + providerPanelOffset)
                        .fillMaxWidth(0.92f)
                        .height(wideModelHeight)

                    if (isWide) {
                        Row(
                            modifier = wideRowModifier,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProviderPickerPane(
                                providers = enabledProviders,
                                selectedProviderId = providerFilter,
                                totalModelCount = totalModelCount,
                                onSelectProvider = { providerId ->
                                    requestedProviderFilter = providerId
                                    query = ""
                                    openGroupPopupKey = null
                                },
                                onManageProviders = { onDismiss(); onManageProviders() },
                                modifier = Modifier.weight(0.51f).height(wideProviderHeight)
                                    .offset(y = -providerPanelOffset)
                            )
                            ModelDetailsPane(
                                modifier = Modifier.weight(0.49f).fillMaxHeight(),
                                compact = false,
                                provider = selectedProvider,
                                isAllModels = providerFilter == null,
                                totalModelCount = totalModelCount,
                                query = query,
                                onQueryChange = { query = it; openGroupPopupKey = null },
                                groups = groups,
                                favorites = favorites,
                                favoriteEntries = allFavoriteEntries,
                                favoritesExpanded = favoritesExpanded,
                                onFavoritesExpandedChange = { favoritesExpanded = it },
                                selectedProviderId = selectedProviderId,
                                selectedModelId = selectedModelId,
                                openGroupPopupKey = openGroupPopupKey,
                                onOpenGroupPopupChange = { openGroupPopupKey = it },
                                expandedGroups = expandedGroups,
                                onExpandedGroupsChange = { expandedGroups = it },
                                onSelectProvider = { providerId ->
                                    requestedProviderFilter = providerId
                                    query = ""
                                    openGroupPopupKey = null
                                },
                                onToggleFavorite = onToggleFavorite,
                                onSelectModel = { providerId, modelId -> onSelect(providerId, modelId); onDismiss() },
                                onAddProvider = { onDismiss(); onAddProvider() },
                                onManageProviders = { onDismiss(); onManageProviders() },
                                onCheckProvider = onCheckProvider,
                                onDismiss = onDismiss
                            )
                        }
                    } else {
                        Column(
                            modifier = compactPanelModifier,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CompactProviderRail(
                                providers = enabledProviders,
                                selectedProviderId = providerFilter,
                                totalModelCount = totalModelCount,
                                onSelectProvider = { providerId ->
                                    requestedProviderFilter = providerId
                                    query = ""
                                    openGroupPopupKey = null
                                },
                                onManageProviders = { onDismiss(); onManageProviders() },
                                modifier = Modifier.fillMaxWidth()
                            )
                            ModelDetailsPane(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                compact = true,
                                provider = selectedProvider,
                                isAllModels = providerFilter == null,
                                totalModelCount = totalModelCount,
                                query = query,
                                onQueryChange = { query = it; openGroupPopupKey = null },
                                groups = groups,
                                favorites = favorites,
                                favoriteEntries = allFavoriteEntries,
                                favoritesExpanded = favoritesExpanded,
                                onFavoritesExpandedChange = { favoritesExpanded = it },
                                selectedProviderId = selectedProviderId,
                                selectedModelId = selectedModelId,
                                openGroupPopupKey = openGroupPopupKey,
                                onOpenGroupPopupChange = { openGroupPopupKey = it },
                                expandedGroups = expandedGroups,
                                onExpandedGroupsChange = { expandedGroups = it },
                                onSelectProvider = { providerId ->
                                    requestedProviderFilter = providerId
                                    query = ""
                                    openGroupPopupKey = null
                                },
                                onToggleFavorite = onToggleFavorite,
                                onSelectModel = { providerId, modelId -> onSelect(providerId, modelId); onDismiss() },
                                onAddProvider = { onDismiss(); onAddProvider() },
                                onManageProviders = { onDismiss(); onManageProviders() },
                                onCheckProvider = onCheckProvider,
                                onDismiss = onDismiss
                            )
                        }
                    }
                }
            }
        }
    }
}
