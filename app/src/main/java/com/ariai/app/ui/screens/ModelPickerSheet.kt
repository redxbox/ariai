package com.ariai.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Provider

private val Accent = Color(0xFF6C4DFF)
private val Ink = Color(0xFF1C1B1F)

/** Bottom sheet to pick a provider model for the current chat, with search and provider filter. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerSheet(
    providers: List<Provider>,
    selectedModelId: String?,
    onSelect: (providerId: String, modelId: String) -> Unit,
    onAddProvider: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var providerFilter by remember { mutableStateOf<String?>(null) }

    val enabled = providers.filter { it.enabled && it.models.isNotEmpty() }
    val shown = enabled
        .filter { providerFilter == null || it.id == providerFilter }
        .map { p ->
            p to p.models.filter {
                query.isBlank() || it.displayName.contains(query, true) || it.id.contains(query, true) || p.name.contains(query, true)
            }
        }
        .filter { it.second.isNotEmpty() }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFFF7F6FB)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("No models available", fontWeight = FontWeight.SemiBold, color = Ink)
                    Text("Add a provider with at least one model.", color = Ink.copy(alpha = 0.5f), fontSize = 13.sp)
                    Button(onClick = onAddProvider, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                        Text("Add provider")
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    shown.forEach { (provider, models) ->
                        item(key = "h_${provider.id}") {
                            Text(provider.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink.copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp, start = 4.dp))
                        }
                        itemsIndexed(models, key = { i, m -> "${provider.id}_${m.id}_$i" }) { _, model ->
                            val selected = model.id == selectedModelId
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) Accent.copy(alpha = 0.10f) else Color.White,
                                modifier = Modifier.fillMaxWidth().clickable { onSelect(provider.id, model.id); onDismiss() }
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(model.displayName, fontWeight = FontWeight.Medium, color = if (selected) Accent else Ink, fontSize = 14.sp)
                                        Text(model.id, fontSize = 11.sp, color = Ink.copy(alpha = 0.45f))
                                    }
                                    if (selected) Icon(Icons.Default.Star, contentDescription = "Selected", tint = Accent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(selected = providerFilter == null, onClick = { providerFilter = null }, label = { Text("All") })
                    enabled.forEach { p ->
                        FilterChip(selected = providerFilter == p.id, onClick = { providerFilter = p.id }, label = { Text(p.name, maxLines = 1) })
                    }
                }
            }
        }
    }
}
