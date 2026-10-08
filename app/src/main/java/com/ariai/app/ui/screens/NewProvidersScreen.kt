package com.ariai.app.ui.screens

import com.ariai.app.ui.theme.*

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType

private val Ink = Color(0xFF1C1B1F)

fun providerTypeLabel(type: ProviderType): String = when (type) {
    ProviderType.OPENAI -> "OpenAI"
    ProviderType.GEMINI -> "Google Gemini"
    ProviderType.ANTHROPIC -> "Anthropic"
    ProviderType.OPENAI_COMPATIBLE -> "OpenAI compatible"
    ProviderType.OLLAMA -> "Ollama (local)"
    ProviderType.CUSTOM -> "Custom"
}

/** Each provider type has its own brand colour. */
fun providerBrandColor(type: ProviderType): Color = when (type) {
    ProviderType.OPENAI -> AriOpenAI
    ProviderType.GEMINI -> AriGemini
    ProviderType.ANTHROPIC -> AriAnthropic
    ProviderType.OLLAMA -> AriOllama
    ProviderType.OPENAI_COMPATIBLE, ProviderType.CUSTOM -> AriCustom
}

/** Each provider type has its own avatar shape too. */
private fun providerShape(type: ProviderType): Shape = when (type) {
    ProviderType.OPENAI -> CircleShape
    ProviderType.GEMINI -> RoundedCornerShape(14.dp)
    ProviderType.ANTHROPIC -> RoundedCornerShape(6.dp)
    ProviderType.OLLAMA -> RoundedCornerShape(50)
    ProviderType.OPENAI_COMPATIBLE, ProviderType.CUSTOM -> RoundedCornerShape(10.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProvidersScreen(
    providers: List<Provider>,
    onAddProvider: () -> Unit,
    onEditProvider: (Provider) -> Unit,
    onBack: () -> Unit,
    onToggleProvider: (Provider) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    val visible = providers.filter {
        query.isBlank() ||
            it.name.contains(query, ignoreCase = true) ||
            providerTypeLabel(it.type).contains(query, ignoreCase = true)
    }

    Scaffold(
        containerColor = AriPaper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AriPaper),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Ink) } },
                title = { Text("Providers", fontWeight = FontWeight.SemiBold, color = Ink) },
                actions = {
                    IconButton(onClick = onAddProvider) { Icon(Icons.Default.Add, contentDescription = "Add provider", tint = Ink) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search providers", color = Ink.copy(alpha = 0.45f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Ink.copy(alpha = 0.5f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Ink.copy(alpha = 0.3f),
                        unfocusedBorderColor = Ink.copy(alpha = 0.15f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (visible.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(if (providers.isEmpty()) "No providers yet" else "No match", fontWeight = FontWeight.SemiBold, color = Ink)
                        Text(
                            if (providers.isEmpty()) "Tap + to add an API endpoint." else "Try another name.",
                            color = Ink.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            itemsIndexed(visible, key = { index, p -> "${p.id}_$index" }) { _, provider ->
                ProviderCard(
                    provider = provider,
                    onClick = { onEditProvider(provider) },
                    onToggle = { onToggleProvider(provider) }
                )
            }
        }
    }
}

@Composable
private fun ProviderCard(provider: Provider, onClick: () -> Unit, onToggle: () -> Unit) {
    val brand = providerBrandColor(provider.type)
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(providerShape(provider.type)).background(brand.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    provider.name.firstOrNull()?.uppercase() ?: "P",
                    color = brand,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(provider.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(providerTypeLabel(provider.type), fontSize = 12.sp, color = Ink.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniChip(
                        text = if (provider.models.isEmpty()) "No models" else "${provider.models.size} models",
                        bg = AriTint,
                        fg = Ink.copy(alpha = 0.6f)
                    )
                    MiniChip(
                        text = if (provider.enabled) "On" else "Off",
                        bg = if (provider.enabled) AriTagGreenBg else AriTint,
                        fg = if (provider.enabled) AriTagGreenFg else Ink.copy(alpha = 0.5f)
                    )
                }
            }
            Switch(
                checked = provider.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedTrackColor = AriInk)
            )
        }
    }
}

@Composable
private fun MiniChip(text: String, bg: Color, fg: Color) {
    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(text, fontSize = 11.sp, color = fg, fontWeight = FontWeight.Medium)
    }
}
