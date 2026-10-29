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

@Composable
internal fun ModelPickerGridBadge(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(size * 0.30f),
        color = AriAccentSoft,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(size * 0.23f),
            verticalArrangement = Arrangement.spacedBy(size * 0.09f)
        ) {
            repeat(2) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(size * 0.09f)
                ) {
                    repeat(2) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .clip(RoundedCornerShape(size * 0.08f))
                                .background(AriAccent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderPickerPane(
    providers: List<Provider>,
    selectedProviderId: String?,
    totalModelCount: Int,
    onSelectProvider: (String?) -> Unit,
    onManageProviders: () -> Unit,
    modifier: Modifier = Modifier
) {
    PickerPanelSurface(
        modifier = modifier.testTag("model-picker-provider-dock")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item(key = "provider-filter:all") {
                    ProviderListItem(
                        provider = null,
                        label = tx("All models"),
                        modelCount = totalModelCount,
                        selected = selectedProviderId == null,
                        tag = "provider-filter-all",
                        onClick = { onSelectProvider(null) }
                    )
                }
                items(providers, key = { "provider:${it.id}" }) { provider ->
                    ProviderListItem(
                        provider = provider,
                        label = provider.name,
                        modelCount = provider.models.size,
                        selected = selectedProviderId == provider.id,
                        tag = "provider-filter-${provider.id}",
                        onClick = { onSelectProvider(provider.id) }
                    )
                }
            }
            HorizontalDivider(color = AriLine.copy(alpha = 0.72f), modifier = Modifier.padding(horizontal = 12.dp))
            ManageProvidersItem(onClick = onManageProviders, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
private fun CompactProviderRail(
    providers: List<Provider>,
    selectedProviderId: String?,
    totalModelCount: Int,
    onSelectProvider: (String?) -> Unit,
    onManageProviders: () -> Unit,
    modifier: Modifier = Modifier
) {
    PickerPanelSurface(
        modifier = modifier.heightIn(min = 52.dp).testTag("model-picker-provider-dock"),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f).testTag("model-picker-provider-rail"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                item(key = "provider-filter:all") {
                    ProviderCompactItem(
                        provider = null,
                        label = tx("All models"),
                        modelCount = totalModelCount,
                        selected = selectedProviderId == null,
                        tag = "provider-filter-all",
                        onClick = { onSelectProvider(null) }
                    )
                }
                items(providers, key = { "provider:${it.id}" }) { provider ->
                    ProviderCompactItem(
                        provider = provider,
                        label = provider.name,
                        modelCount = provider.models.size,
                        selected = selectedProviderId == provider.id,
                        tag = "provider-filter-${provider.id}",
                        onClick = { onSelectProvider(provider.id) }
                    )
                }
            }
            IconButton(
                onClick = onManageProviders,
                modifier = Modifier.size(44.dp).testTag("model-picker-manage-providers")
            ) {
                Icon(Icons.Default.Settings, contentDescription = tx("Manage providers & models"), tint = AriAccent)
            }
        }
    }
}

@Composable
private fun ModelDetailsPane(
    modifier: Modifier = Modifier,
    compact: Boolean,
    provider: Provider?,
    isAllModels: Boolean,
    totalModelCount: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    groups: List<PickerModelGroup>,
    favorites: Set<String>,
    favoriteEntries: List<PickerModelEntry>,
    favoritesExpanded: Boolean,
    onFavoritesExpandedChange: (Boolean) -> Unit,
    selectedProviderId: String?,
    selectedModelId: String?,
    openGroupPopupKey: String?,
    onOpenGroupPopupChange: (String?) -> Unit,
    expandedGroups: Set<String>,
    onExpandedGroupsChange: (Set<String>) -> Unit,
    onSelectProvider: (String?) -> Unit,
    onToggleFavorite: (providerId: String, modelId: String) -> Unit,
    onSelectModel: (providerId: String, modelId: String) -> Unit,
    onAddProvider: () -> Unit,
    onManageProviders: () -> Unit,
    onCheckProvider: suspend (Provider) -> ConnectionResult,
    onDismiss: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var checkingConnection by remember(provider?.id) { mutableStateOf(false) }
    var connectionResult by remember(provider?.id) { mutableStateOf<ConnectionResult?>(null) }
    val scope = rememberCoroutineScope()
    val displayedModelCount = if (isAllModels) totalModelCount else provider?.models?.size ?: 0

    PickerPanelSurface(modifier = modifier.testTag("model-picker-details-pane")) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { if (isAllModels) onDismiss() else onSelectProvider(null) },
                    modifier = Modifier.size(44.dp).testTag("model-picker-back")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tx("Back"), tint = AriInk)
                }
                ProviderBadge(provider = provider, isAllModels = isAllModels, selected = true, size = 36.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (isAllModels) tx("All models") else provider?.name.orEmpty(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AriInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        modelCountLabel(displayedModelCount),
                        fontSize = 10.sp,
                        color = AriMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("model-picker-selected-count")
                    )
                }
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(44.dp).testTag("model-picker-overflow")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = tx("More"), tint = AriInk)
                    }
                    val menuShape = RoundedCornerShape(18.dp)
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(AriCard, menuShape).then(paperPopupModifier(menuShape))
                    ) {
                        if (provider != null) {
                            DropdownMenuItem(
                                text = { Text(if (checkingConnection) tx("Checking…") else tx("Check connection")) },
                                leadingIcon = {
                                    if (checkingConnection) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AriAccent)
                                    } else {
                                        Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = AriAccent)
                                    }
                                },
                                enabled = !checkingConnection,
                                onClick = {
                                    menuExpanded = false
                                    connectionResult = null
                                    checkingConnection = true
                                    scope.launch {
                                        try {
                                            connectionResult = onCheckProvider(provider)
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (error: Exception) {
                                            connectionResult = ConnectionResult.Failed(
                                                tx("Could not connect"),
                                                error.message?.take(180) ?: tx("Check your connection, or the Base URL.")
                                            )
                                        } finally {
                                            checkingConnection = false
                                        }
                                    }
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(tx("Add provider")) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = AriAccent) },
                            modifier = Modifier.testTag("model-picker-add-menu"),
                            onClick = { menuExpanded = false; onAddProvider() }
                        )
                        DropdownMenuItem(
                            text = { Text(tx("Manage providers & models")) },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = AriAccent) },
                            modifier = Modifier.testTag("model-picker-manage-menu"),
                            onClick = { menuExpanded = false; onManageProviders() }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = { Text(tx("Search models") + "…", fontSize = 14.sp, color = AriMuted.copy(alpha = 0.75f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AriInk, modifier = Modifier.size(20.dp)) },
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AriInk,
                    unfocusedTextColor = AriInk,
                    cursorColor = AriAccent,
                    focusedBorderColor = AriLine,
                    unfocusedBorderColor = AriLine,
                    focusedContainerColor = AriTint.copy(alpha = 0.62f),
                    unfocusedContainerColor = AriTint.copy(alpha = 0.62f)
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).heightIn(min = 48.dp)
                    .testTag("model-picker-search")
            )

            connectionResult?.let { result ->
                ConnectionStatus(result, modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("model-picker-model-list"),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (isAllModels && favoriteEntries.isNotEmpty()) {
                    item(key = "group:favorites") {
                        PickerGroupHeader(
                            title = tx("Favorites"),
                            count = favoriteEntries.size,
                            mark = PickerGroupMark.FAVORITES,
                            expanded = true,
                            onClick = {}
                        )
                    }
                    val visibleFavorites = if (favoritesExpanded || query.isNotBlank()) favoriteEntries else favoriteEntries.take(3)
                    items(visibleFavorites, key = { "favorite:${it.provider.id}:${it.model.id}" }) { entry ->
                        PickerModelRow(
                            entry = entry,
                            selected = isSelectedModel(entry.provider.id, entry.model.id, selectedProviderId, selectedModelId),
                            favorite = true,
                            onSelect = { onSelectModel(entry.provider.id, entry.model.id) },
                            onToggleFavorite = { onToggleFavorite(entry.provider.id, entry.model.id) }
                        )
                    }
                    if (query.isBlank() && favoriteEntries.size > 3) {
                        item(key = "expand:favorites") {
                            ExpandPickerModelsButton(
                                expanded = favoritesExpanded,
                                count = favoriteEntries.size,
                                tag = "model-picker-expand-favorites",
                                onClick = { onFavoritesExpandedChange(!favoritesExpanded) }
                            )
                        }
                    }
                }

                if (groups.isEmpty()) {
                    item(key = "empty-model-list") {
                        EmptyModelResults(query = query, hasProviders = totalModelCount > 0)
                    }
                }

                val searchActive = query.isNotBlank()
                if (compact && !searchActive) {
                    groups.chunked(2).forEachIndexed { rowIndex, rowGroups ->
                        item(key = "group-grid-row:$rowIndex") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                rowGroups.forEach { group ->
                                    PickerModelGroupCard(
                                        modifier = Modifier.weight(1f),
                                        group = group,
                                        provider = provider,
                                        isAllModels = isAllModels,
                                        isSearching = false,
                                        favorites = favorites,
                                        selectedProviderId = selectedProviderId,
                                        selectedModelId = selectedModelId,
                                        openGroupPopupKey = openGroupPopupKey,
                                        expandedGroups = expandedGroups,
                                        onOpenGroupPopupChange = onOpenGroupPopupChange,
                                        onExpandedGroupsChange = onExpandedGroupsChange,
                                        onToggleFavorite = onToggleFavorite,
                                        onSelectModel = onSelectModel
                                    )
                                }
                                if (rowGroups.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    groups.forEach { group ->
                        item(key = "group:${group.key}") {
                            PickerModelGroupCard(
                                modifier = Modifier.fillMaxWidth(),
                                group = group,
                                provider = provider,
                                isAllModels = isAllModels,
                                isSearching = searchActive,
                                favorites = favorites,
                                selectedProviderId = selectedProviderId,
                                selectedModelId = selectedModelId,
                                openGroupPopupKey = openGroupPopupKey,
                                expandedGroups = expandedGroups,
                                onOpenGroupPopupChange = onOpenGroupPopupChange,
                                onExpandedGroupsChange = onExpandedGroupsChange,
                                onToggleFavorite = onToggleFavorite,
                                onSelectModel = onSelectModel
                            )
                        }
                        if (searchActive) {
                            items(group.entries, key = { "model:${it.provider.id}:${it.model.id}" }) { entry ->
                                PickerModelRow(
                                    entry = entry,
                                    selected = isSelectedModel(entry.provider.id, entry.model.id, selectedProviderId, selectedModelId),
                                    favorite = favorites.contains(favoriteKey(entry.provider.id, entry.model.id)),
                                    onSelect = { onSelectModel(entry.provider.id, entry.model.id) },
                                    onToggleFavorite = { onToggleFavorite(entry.provider.id, entry.model.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun PickerModelGroupCard(
    modifier: Modifier = Modifier,
    group: PickerModelGroup,
    provider: Provider?,
    isAllModels: Boolean,
    isSearching: Boolean,
    favorites: Set<String>,
    selectedProviderId: String?,
    selectedModelId: String?,
    openGroupPopupKey: String?,
    expandedGroups: Set<String>,
    onOpenGroupPopupChange: (String?) -> Unit,
    onExpandedGroupsChange: (Set<String>) -> Unit,
    onToggleFavorite: (providerId: String, modelId: String) -> Unit,
    onSelectModel: (providerId: String, modelId: String) -> Unit
) {
    val isPopupOpen = !isSearching && openGroupPopupKey == group.key
    val previewLimit = if (group.key == OTHER_MODELS_KEY) 3 else 5
    val showAll = isSearching || expandedGroups.contains(group.key)
    val visibleEntries = if (showAll) group.entries else group.entries.take(previewLimit)
    val expandTag = if (group.key == OTHER_MODELS_KEY && !isAllModels && provider != null) {
        "model-picker-expand-${provider.id}"
    } else {
        "model-picker-expand-${group.key}"
    }

    Box(modifier = modifier) {
        PickerGroupHeader(
            title = group.title,
            count = group.entries.size,
            mark = PickerGroupMark.BRAND(group.title),
            expanded = isSearching || isPopupOpen,
            onClick = {
                if (!isSearching) onOpenGroupPopupChange(if (isPopupOpen) null else group.key)
            },
            testTag = "model-picker-group-${group.key}"
        )
        val popupShape = RoundedCornerShape(16.dp)
        DropdownMenu(
            expanded = isPopupOpen,
            onDismissRequest = {
                if (openGroupPopupKey == group.key) onOpenGroupPopupChange(null)
            },
            offset = androidx.compose.ui.unit.DpOffset(0.dp, 7.dp),
            modifier = Modifier.widthIn(min = 220.dp, max = 340.dp)
                .background(AriCard, popupShape)
                .then(paperPopupModifier(popupShape))
                .testTag("model-picker-group-popup-${group.key}")
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp)) {
                visibleEntries.forEachIndexed { index, entry ->
                    PickerModelRow(
                        entry = entry,
                        selected = isSelectedModel(entry.provider.id, entry.model.id, selectedProviderId, selectedModelId),
                        favorite = favorites.contains(favoriteKey(entry.provider.id, entry.model.id)),
                        onSelect = { onSelectModel(entry.provider.id, entry.model.id) },
                        onToggleFavorite = { onToggleFavorite(entry.provider.id, entry.model.id) }
                    )
                    if (index < visibleEntries.lastIndex) Spacer(Modifier.height(4.dp))
                }
                if (!isSearching && group.entries.size > previewLimit) {
                    Spacer(Modifier.height(4.dp))
                    ExpandPickerModelsButton(
                        expanded = showAll,
                        count = group.entries.size,
                        tag = expandTag,
                        onClick = {
                            val nextExpandedGroups = if (showAll) expandedGroups - group.key
                            else expandedGroups + group.key
                            onExpandedGroupsChange(nextExpandedGroups)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerPanelSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = AriCard.copy(alpha = 0.98f),
        contentColor = AriInk,
        shadowElevation = 4.dp,
        border = BorderStroke(0.75.dp, AriLine.copy(alpha = 0.82f)),
        tonalElevation = 0.dp
    ) {
        content()
    }
}

@Composable
private fun ProviderListItem(
    provider: Provider?,
    label: String,
    modelCount: Int,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) AriAccentSoft.copy(alpha = 0.76f) else Color.Transparent,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            ProviderBadge(provider = provider, isAllModels = provider == null, selected = selected, size = 40.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) AriAccent else AriInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    modelCountLabel(modelCount),
                    fontSize = 12.sp,
                    color = AriMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(if (provider == null) "provider-model-count-all" else "provider-model-count-${provider.id}")
                )
            }
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = if (selected) AriAccent else AriInk.copy(alpha = 0.82f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ProviderCompactItem(
    provider: Provider?,
    label: String,
    modelCount: Int,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val chipColor by animateColorAsState(
        targetValue = if (selected) AriAccentSoft.copy(alpha = 0.72f) else AriTint.copy(alpha = 0.38f),
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "provider-filter-surface"
    )
    val chipBorder by animateColorAsState(
        targetValue = if (selected) AriAccent.copy(alpha = 0.2f) else AriLine.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "provider-filter-border"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = chipColor,
        border = BorderStroke(0.75.dp, chipBorder),
        modifier = Modifier.heightIn(min = 48.dp).testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ProviderBadge(provider = provider, isAllModels = provider == null, selected = selected, size = 26.dp)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) AriAccent else AriInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    modelCountLabel(modelCount),
                    fontSize = 9.sp,
                    color = AriMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(if (provider == null) "provider-model-count-all" else "provider-model-count-${provider.id}")
                )
            }
        }
    }
}

@Composable
private fun ManageProvidersItem(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("model-picker-manage-providers")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = AriInk, modifier = Modifier.size(26.dp))
            Text(
                tx("Manage providers & models"),
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AriInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = AriAccent, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun ProviderBadge(provider: Provider?, isAllModels: Boolean, selected: Boolean, size: Dp) {
    val shape = RoundedCornerShape(size * 0.32f)
    Surface(
        modifier = Modifier.size(size),
        shape = shape,
        color = if (selected) AriCard.copy(alpha = 0.8f) else AriTint.copy(alpha = 0.75f),
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                isAllModels -> AllModelsGlyph(size = size * 0.54f)
                provider != null && provider.type in setOf(ProviderType.OPENAI_COMPATIBLE, ProviderType.CUSTOM) ->
                    ServerStackGlyph(modifier = Modifier.size(size * 0.62f), tint = AriInk)
                provider != null -> ProviderBrandMark(provider = provider, size = size * 0.80f)
            }
        }
    }
}

@Composable
private fun AllModelsGlyph(size: Dp) {
    Column(
        modifier = Modifier.size(size),
        verticalArrangement = Arrangement.spacedBy(size * 0.14f)
    ) {
        repeat(2) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(size * 0.14f)) {
                repeat(2) {
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(size * 0.18f)).background(AriAccent)
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerStackGlyph(modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val left = size.width * 0.10f
        val right = size.width * 0.90f
        val boxHeight = size.height * 0.32f
        val firstTop = size.height * 0.12f
        val secondTop = size.height * 0.56f
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(left, firstTop),
            size = androidx.compose.ui.geometry.Size(right - left, boxHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.12f),
            style = stroke
        )
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(left, secondTop),
            size = androidx.compose.ui.geometry.Size(right - left, boxHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.12f),
            style = stroke
        )
        listOf(firstTop + boxHeight * 0.5f, secondTop + boxHeight * 0.5f).forEach { y ->
            drawCircle(tint, radius = size.minDimension * 0.045f, center = androidx.compose.ui.geometry.Offset(size.width * 0.28f, y))
        }
    }
}

@Composable
private fun PickerGroupHeader(
    title: String,
    count: Int,
    mark: PickerGroupMark,
    expanded: Boolean,
    onClick: () -> Unit,
    testTag: String = ""
) {
    val headerTag = testTag.ifBlank { "model-group-${title.lowercase().replace(' ', '-')}" }
    val headerColor by animateColorAsState(
        targetValue = if (expanded) AriAccentSoft.copy(alpha = 0.72f) else AriTint.copy(alpha = 0.34f),
        animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
        label = "model-group-surface"
    )
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "model-group-chevron"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = headerColor,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(headerTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (mark) {
                PickerGroupMark.FAVORITES -> Surface(
                    modifier = Modifier.size(26.dp),
                    shape = CircleShape,
                    color = AriAccentSoft
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = AriAccent, modifier = Modifier.size(14.dp))
                    }
                }
                is PickerGroupMark.BRAND -> ProviderBrandMark(
                    type = providerTypeForFamily(mark.name),
                    name = mark.name,
                    size = 26.dp
                )
            }
            Column(modifier = Modifier.widthIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    title,
                    modifier = Modifier.testTag("$headerTag-title"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AriInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(modelCountLabel(count), fontSize = 9.sp, color = AriMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = if (expanded) tx("Show less") else tx("Show all models"),
                tint = AriInk,
                modifier = Modifier.size(18.dp).rotate(arrowRotation).testTag("$headerTag-expand-arrow")
            )
        }
    }
}

private sealed interface PickerGroupMark {
    data object FAVORITES : PickerGroupMark
    data class BRAND(val name: String) : PickerGroupMark
}

@Composable
private fun PickerModelRow(
    entry: PickerModelEntry,
    selected: Boolean,
    favorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val selectedColor by animateColorAsState(
        targetValue = if (selected) AriAccentSoft.copy(alpha = 0.76f) else Color.Transparent,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "picker-model-selection"
    )
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = selectedColor,
        border = BorderStroke(0.75.dp, if (selected) AriAccent.copy(alpha = 0.20f) else AriLine.copy(alpha = 0.5f)),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
            .animateContentSize(tween(durationMillis = 160, easing = FastOutSlowInEasing))
            .clickable(onClick = onSelect)
            .testTag("model-row-${entry.provider.id}-${entry.model.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(start = 9.dp, end = 3.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ModelCubeGlyph(modifier = Modifier.size(22.dp), tint = AriInk)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        entry.model.displayName.ifBlank { entry.model.id },
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) AriAccent else AriInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selected) {
                        Surface(shape = RoundedCornerShape(50), color = AriAccentSoft) {
                            Text(tx("Selected"), fontSize = 9.sp, color = AriAccent, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Text(
                    modelSummary(entry),
                    fontSize = 10.sp,
                    color = AriMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(38.dp)) {
                Icon(
                    if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (favorite) tx("Remove from favorites") else tx("Add to favorites"),
                    tint = if (favorite) AriAccent else AriMuted.copy(alpha = 0.72f),
                    modifier = Modifier.size(17.dp)
                )
            }
            RadioButton(
                selected = selected,
                onClick = onSelect,
                modifier = Modifier.size(40.dp),
                colors = RadioButtonDefaults.colors(selectedColor = AriAccent, unselectedColor = AriMuted.copy(alpha = 0.48f))
            )
        }
    }
}

@Composable
private fun ModelCubeGlyph(modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerX = w * 0.5f
        val centerY = h * 0.5f
        val strokeWidth = size.minDimension * 0.075f
        val outline = Path().apply {
            moveTo(centerX, h * 0.08f)
            lineTo(w * 0.91f, h * 0.29f)
            lineTo(w * 0.91f, h * 0.71f)
            lineTo(centerX, h * 0.93f)
            lineTo(w * 0.09f, h * 0.71f)
            lineTo(w * 0.09f, h * 0.29f)
            close()
        }
        drawPath(outline, tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(tint, androidx.compose.ui.geometry.Offset(centerX, h * 0.08f), androidx.compose.ui.geometry.Offset(centerX, centerY), strokeWidth, cap = StrokeCap.Round)
        drawLine(tint, androidx.compose.ui.geometry.Offset(w * 0.09f, h * 0.29f), androidx.compose.ui.geometry.Offset(centerX, centerY), strokeWidth, cap = StrokeCap.Round)
        drawLine(tint, androidx.compose.ui.geometry.Offset(w * 0.91f, h * 0.29f), androidx.compose.ui.geometry.Offset(centerX, centerY), strokeWidth, cap = StrokeCap.Round)
        drawLine(tint, androidx.compose.ui.geometry.Offset(centerX, centerY), androidx.compose.ui.geometry.Offset(centerX, h * 0.93f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
private fun ExpandPickerModelsButton(
    expanded: Boolean,
    count: Int,
    tag: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).testTag(tag),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = AriAccent)
    ) {
        Icon(
            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            if (expanded) tx("Show fewer models") else "${tx("Show all models")} ($count)",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyModelResults(query: String, hasProviders: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            if (query.isBlank()) tx("No models available") else tx("No matching models"),
            fontWeight = FontWeight.SemiBold,
            color = AriInk
        )
        if (query.isBlank() && !hasProviders) {
            Text(tx("Add a provider with at least one model."), color = AriMuted, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun ConnectionStatus(result: ConnectionResult, modifier: Modifier = Modifier) {
    when (result) {
        is ConnectionResult.Ok -> Text(
            "${tx("Connected")} · ${result.durationMs} ms · ${result.models} ${tx("models")}",
            modifier = modifier,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = AriAccentGreen,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        is ConnectionResult.Failed -> Text(
            result.problem + (result.code?.let { " (HTTP $it)" } ?: ""),
            modifier = modifier,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = AriError,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun modelFamilyKey(model: AIModel): String {
    val idAndName = "${model.id} ${model.displayName}".lowercase()
    return when {
        "gpt" in idAndName || "openai" in idAndName -> "chatgpt"
        "gemini" in idAndName -> "gemini"
        "claude" in idAndName || "anthropic" in idAndName -> "claude"
        "deepseek" in idAndName -> "deepseek"
        "qwen" in idAndName -> "qwen"
        "llama" in idAndName || "meta-llama" in idAndName -> "meta"
        "mistral" in idAndName || "mixtral" in idAndName -> "mistral"
        "cohere" in idAndName || "command-r" in idAndName -> "cohere"
        "phi-" in idAndName || "phi " in idAndName -> "microsoft"
        else -> OTHER_MODELS_KEY
    }
}

private fun modelFamilyTitle(model: AIModel): String = when (modelFamilyKey(model)) {
    "chatgpt" -> "ChatGPT"
    "gemini" -> "Gemini"
    "claude" -> "Claude"
    "deepseek" -> "DeepSeek"
    "qwen" -> "Qwen"
    "meta" -> "Meta"
    "mistral" -> "Mistral"
    "cohere" -> "Cohere"
    "microsoft" -> "Microsoft"
    else -> tx("Other models")
}

private fun modelFamilyOrder(key: String): Int = when (key) {
    "chatgpt" -> 0
    "gemini" -> 1
    "claude" -> 2
    "deepseek" -> 3
    "qwen" -> 4
    "meta" -> 5
    "mistral" -> 6
    "cohere" -> 7
    "microsoft" -> 8
    else -> 9
}

private fun providerTypeForFamily(name: String): ProviderType = when (name) {
    "ChatGPT" -> ProviderType.OPENAI
    "Gemini" -> ProviderType.GEMINI
    "Claude" -> ProviderType.ANTHROPIC
    else -> ProviderType.CUSTOM
}

private fun modelSummary(entry: PickerModelEntry): String {
    val model = entry.model
    val distinctId = model.id.takeIf { it.isNotBlank() && !it.equals(model.displayName, ignoreCase = true) }
    val details = buildList {
        if (distinctId != null) add(distinctId)
        if (model.supportsVision) add(tx("Vision"))
        if (model.supportsFunctionCalling) add(tx("Tools"))
        if (model.supportsImageGen) add(tx("Image"))
        if (model.supportsAudioGen) add(tx("Audio"))
        if (model.supportsVideoGen) add(tx("Video"))
    }
    return details.joinToString(" · ").ifBlank { entry.provider.name }
}

private fun modelCountLabel(count: Int): String = "$count ${tx("models")}"
