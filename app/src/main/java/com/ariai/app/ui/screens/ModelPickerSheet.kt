package com.ariai.app.ui.screens

import com.ariai.app.util.tx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.ariai.app.ui.theme.*

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.ariai.app.data.models.AIModel
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.favoriteKey
import com.ariai.app.data.remote.ConnectionResult
import com.ariai.app.ui.components.ProviderBrandMark
import com.ariai.app.ui.components.providerBrandColor
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

private val Accent: Color @Composable @ReadOnlyComposable get() = AriAccent
private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk
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

@Composable
private fun ProviderSectionHeader(provider: Provider, count: Int, selected: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProviderBrandMark(provider = provider, size = 28.dp)
        Spacer(Modifier.width(9.dp))
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(provider.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text("$count ${tx("models")}", fontSize = 10.sp, color = Ink.copy(alpha = 0.48f))
        }
        Spacer(Modifier.weight(1f))
        if (selected) {
            Text(tx("Selected"), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = AriAccent)
        }
    }
}

@Composable
private fun ExpandModelsButton(
    expanded: Boolean,
    count: Int,
    testTag: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(testTag),
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = AriAccent)
    ) {
        Icon(
            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(if (expanded) tx("Show less") else "${tx("Show all")} $count", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
private fun ProviderDock(
    providers: List<Provider>,
    selectedProviderId: String?,
    onSelect: (String?) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = AriCard.copy(alpha = 0.88f),
        shadowElevation = 5.dp,
        border = BorderStroke(0.8.dp, AriLine.copy(alpha = 0.85f)),
        modifier = Modifier.fillMaxWidth().testTag("model-picker-provider-dock")
    ) {
        Column(modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tx("Providers"), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = AriMuted)
                Spacer(Modifier.weight(1f))
                Text(
                    providers.firstOrNull { it.id == selectedProviderId }?.name ?: tx("All"),
                    fontSize = 10.sp,
                    color = AriAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                item(key = "provider-filter:all") {
                    ProviderFilterButton(
                        provider = null,
                        label = tx("All"),
                        selected = selectedProviderId == null,
                        tag = "provider-filter-all",
                        onClick = { onSelect(null) }
                    )
                }
                items(providers, key = { provider -> "provider-filter:${provider.id}" }) { provider ->
                    ProviderFilterButton(
                        provider = provider,
                        label = provider.name,
                        selected = selectedProviderId == provider.id,
                        tag = "provider-filter-${provider.id}",
                        onClick = { onSelect(provider.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderFilterButton(
    provider: Provider?,
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(15.dp)
    val providerColor = providerBrandColor(provider)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) providerColor.copy(alpha = 0.13f) else AriCard.copy(alpha = 0.84f),
        border = BorderStroke(1.dp, if (selected) providerColor.copy(alpha = 0.32f) else AriLine.copy(alpha = 0.8f)),
        modifier = Modifier.heightIn(min = 48.dp).testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            ProviderBrandMark(provider = provider, size = 23.dp)
            Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) providerColor else Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Ink.copy(alpha = 0.5f),
        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
    )
}

@Composable
private fun ModelRow(
    provider: Provider,
    model: AIModel,
    selected: Boolean,
    favorite: Boolean,
    usageCount: Int,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val rowColor by animateColorAsState(
        targetValue = if (selected) AriAccentSoft.copy(alpha = 0.82f) else providerBrandColor(provider).copy(alpha = 0.075f),
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "model-row-selection"
    )
    Surface(
        shape = RoundedCornerShape(19.dp),
        color = rowColor,
        border = BorderStroke(
            0.8.dp,
            if (selected) Accent.copy(alpha = 0.25f) else AriLine.copy(alpha = 0.72f)
        ),
        modifier = Modifier.fillMaxWidth()
            .animateContentSize(tween(200, easing = FastOutSlowInEasing))
            .clickable(onClick = onSelect)
            .testTag("model-row-${provider.id}-${model.id}")
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProviderBrandMark(provider = provider, size = 40.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    model.displayName,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) Accent else Ink,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    model.id,
                    fontSize = 10.sp,
                    color = Ink.copy(alpha = 0.45f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (model.supportsVision) CapabilityTag(tx("Vision"), AriAccentSoft, AriAccent)
                    if (model.supportsFunctionCalling) CapabilityTag(tx("Tools"), AriAccentSoft, AriAccent)
                    if (model.supportsImageGen) CapabilityTag(tx("Image"), AriTagAmberBg, AriTagAmberFg)
                    if (usageCount > 0) CapabilityTag("${usageCount}×", AriAccentSoft, AriAccentDark)
                }
            }
            if (selected) {
                Icon(Icons.Default.Star, contentDescription = tx("Selected"), tint = Accent, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (favorite) tx("Remove from favorites") else tx("Add to favorites"),
                    tint = if (favorite) AriAccent else Ink.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CapabilityTag(label: String, background: Color, foreground: Color) {
    Surface(shape = RoundedCornerShape(50), color = background) {
        Text(
            label,
            color = foreground,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ProviderCheckRow(provider: Provider, check: suspend (Provider) -> ConnectionResult) {
    var checking by remember(provider.id) { mutableStateOf(false) }
    var result by remember(provider.id) { mutableStateOf<ConnectionResult?>(null) }
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
            onClick = {
                checking = true
                result = null
                scope.launch {
                    try {
                        result = check(provider)
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        result = ConnectionResult.Failed(tx("Could not connect"), tx("Check your connection, or the Base URL."))
                    } finally {
                        checking = false
                    }
                }
            },
            enabled = !checking,
            shape = RoundedCornerShape(50),
            border = androidx.compose.foundation.BorderStroke(1.dp, AriLine)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = AriInk, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (checking) tx("Checking…") else tx("Check connection"), color = AriInk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        when (val r = result) {
            is ConnectionResult.Ok -> Text(
                "${tx("Connected")} · ${r.durationMs} ms · ${r.models} ${tx("models")}",
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AriAccentGreen
            )
            is ConnectionResult.Failed -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    r.problem + (r.code?.let { " (HTTP $it)" } ?: ""),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AriError
                )
                Text(r.hint, fontSize = 12.sp, color = AriMuted)
            }
            null -> Unit
        }
    }
}
