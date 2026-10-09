package com.ariai.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.ariai.app.ui.theme.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.ariai.app.data.models.AIModel
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.favoriteKey

private val Accent: Color @Composable @ReadOnlyComposable get() = AriInk
private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk
/**
 * Bottom sheet to pick the model for the current chat. Shows favorites first, then
 * models grouped by provider, with capability tags and a provider filter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerSheet(
    providers: List<Provider>,
    selectedModelId: String?,
    favorites: Set<String>,
    onToggleFavorite: (providerId: String, modelId: String) -> Unit,
    onSelect: (providerId: String, modelId: String) -> Unit,
    onAddProvider: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var providerFilter by remember { mutableStateOf<String?>(null) }
    // Provider ids whose full model list is open. Closed sections show only their top model.
    var expandedProviders by remember { mutableStateOf(setOf<String>()) }

    val enabled = providers.filter { it.enabled && it.models.isNotEmpty() }

    fun matches(provider: Provider, model: AIModel): Boolean =
        query.isBlank() ||
            model.displayName.contains(query, true) ||
            model.id.contains(query, true) ||
            provider.name.contains(query, true)

    val favoriteEntries = enabled.flatMap { p ->
        p.models.filter { favorites.contains(favoriteKey(p.id, it.id)) && matches(p, it) }.map { p to it }
    }
    val sections = enabled
        .filter { providerFilter == null || it.id == providerFilter }
        .map { p -> p to p.models.filter { matches(p, it) } }
        .filter { it.second.isNotEmpty() }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AriPaper) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search models", color = Ink.copy(alpha = 0.4f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Ink.copy(alpha = 0.4f)) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AriCard,
                    unfocusedContainerColor = AriCard,
                    focusedBorderColor = Accent.copy(alpha = 0.35f),
                    unfocusedBorderColor = AriInk.copy(alpha = 0.06f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (enabled.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("No models available", fontWeight = FontWeight.SemiBold, color = Ink)
                    Text("Add a provider with at least one model.", color = Ink.copy(alpha = 0.5f), fontSize = 13.sp)
                    Button(onClick = onAddProvider, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                        Text("Add provider")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (providerFilter == null && favoriteEntries.isNotEmpty()) {
                        item(key = "h_favorites") { SectionTitle("Favorites") }
                        items(favoriteEntries, key = { (p, m) -> "fav_${p.id}_${m.id}" }) { (p, m) ->
                            ModelRow(
                                model = m,
                                selected = m.id == selectedModelId,
                                favorite = true,
                                onSelect = { onSelect(p.id, m.id); onDismiss() },
                                onToggleFavorite = { onToggleFavorite(p.id, m.id) }
                            )
                        }
                    }
                    sections.forEach { (provider, allModels) ->
                        // While searching, show every match so nothing is hidden.
                        val isOpen = query.isNotBlank() || expandedProviders.contains(provider.id)
                        val shown = if (isOpen) allModels else allModels.take(1)
                        val canToggle = query.isBlank() && allModels.size > 1
                        val onToggle: () -> Unit = {
                            expandedProviders = if (isOpen) expandedProviders - provider.id else expandedProviders + provider.id
                        }
                        val first = shown.firstOrNull()
                        // The provider name, count and arrow sit inside the card of the first model.
                        item(key = "h_${provider.id}") {
                            ProviderCard(
                                name = provider.name,
                                count = allModels.size,
                                open = isOpen,
                                canToggle = canToggle,
                                onToggle = onToggle
                            ) {
                                if (first != null) {
                                    ModelRow(
                                        model = first,
                                        selected = first.id == selectedModelId,
                                        favorite = favorites.contains(favoriteKey(provider.id, first.id)),
                                        onSelect = { onSelect(provider.id, first.id); onDismiss() },
                                        onToggleFavorite = { onToggleFavorite(provider.id, first.id) }
                                    )
                                }
                            }
                        }
                        items(shown.drop(1), key = { m -> "${provider.id}_${m.id}" }) { model ->
                            ModelRow(
                                model = model,
                                selected = model.id == selectedModelId,
                                favorite = favorites.contains(favoriteKey(provider.id, model.id)),
                                onSelect = { onSelect(provider.id, model.id); onDismiss() },
                                onToggleFavorite = { onToggleFavorite(provider.id, model.id) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(selected = providerFilter == null, onClick = { providerFilter = null }, label = { Text("All") })
                    enabled.forEach { p ->
                        FilterChip(
                            selected = providerFilter == p.id,
                            onClick = { providerFilter = p.id },
                            label = { Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    name: String,
    count: Int,
    open: Boolean,
    canToggle: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(shape = RoundedCornerShape(16.dp), color = AriCard, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (canToggle) Modifier.clickable(onClick = onToggle) else Modifier)
                    .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                )
                if (canToggle) {
                    Text("$count", fontSize = 12.sp, color = Ink.copy(alpha = 0.4f))
                    Icon(
                        if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (open) "Collapse" else "Expand",
                        tint = Ink.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun ProviderHeader(
    name: String,
    count: Int,
    open: Boolean,
    canToggle: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canToggle, onClick = onToggle)
            .padding(top = 8.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )
        if (canToggle) {
            Text("$count", fontSize = 12.sp, color = Ink.copy(alpha = 0.4f))
            Icon(
                if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (open) "Collapse" else "Expand",
                tint = Ink.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
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
    model: AIModel,
    selected: Boolean,
    favorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Accent.copy(alpha = 0.10f) else AriCard,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    model.displayName,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) Accent else Ink,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    model.id,
                    fontSize = 11.sp,
                    color = Ink.copy(alpha = 0.45f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (model.supportsVision) CapabilityTag("Vision")
                    if (model.supportsFunctionCalling) CapabilityTag("Tools")
                    if (model.supportsImageGen) CapabilityTag("Image")
                }
            }
            if (selected) {
                Icon(Icons.Default.Star, contentDescription = "Selected", tint = Accent, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (favorite) "Remove from favorites" else "Add to favorites",
                    tint = if (favorite) AriAccent else Ink.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CapabilityTag(label: String) {
    Surface(shape = RoundedCornerShape(6.dp), color = Accent.copy(alpha = 0.08f)) {
        Text(
            label,
            color = Accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
