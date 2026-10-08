package com.ariai.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

private val Accent = Color(0xFF6C4DFF)
private val Ink = Color(0xFF1C1B1F)

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

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFFF7F6FB)) {
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
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Accent.copy(alpha = 0.35f),
                    unfocusedBorderColor = Color.Black.copy(alpha = 0.06f)
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
                    sections.forEach { (provider, models) ->
                        item(key = "h_${provider.id}") { SectionTitle(provider.name) }
                        items(models, key = { m -> "${provider.id}_${m.id}" }) { model ->
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
        color = if (selected) Accent.copy(alpha = 0.10f) else Color.White,
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
                    tint = if (favorite) Accent else Ink.copy(alpha = 0.4f),
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
