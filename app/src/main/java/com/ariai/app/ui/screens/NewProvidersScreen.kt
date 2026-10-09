package com.ariai.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.ariai.app.ui.components.GlassCard
import com.ariai.app.ui.theme.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
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
import com.ariai.app.data.models.providerFromJson

private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk
private val DisabledTint: Color @Composable @ReadOnlyComposable get() = AriTint

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
    onAddPreset: (ProviderType) -> Unit,
    onEditProvider: (Provider) -> Unit,
    onImportProvider: (Provider) -> Unit,
    onBack: () -> Unit,
    onToggleProvider: (Provider) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var showRecommended by remember { mutableStateOf(false) }
    val visible = providers.filter {
        query.isBlank() ||
            it.name.contains(query, ignoreCase = true) ||
            providerTypeLabel(it.type).contains(query, ignoreCase = true)
    }

    if (showImport) {
        ImportProviderDialog(
            onImport = { onImportProvider(it); showImport = false },
            onDismiss = { showImport = false }
        )
    }
    if (showRecommended) {
        RecommendedProvidersSheet(
            onPick = { showRecommended = false; onAddPreset(it) },
            onDismiss = { showRecommended = false }
        )
    }

    Scaffold(
        containerColor = AriPaper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AriPaper),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Ink) } },
                title = {},
                actions = {
                    IconButton(onClick = onAddProvider) { Icon(Icons.Default.Add, contentDescription = "Add provider", tint = Ink) }
                    IconButton(onClick = { showImport = true }) { Icon(Icons.Default.FileOpen, contentDescription = "Import provider", tint = Ink) }
                    IconButton(onClick = { showRecommended = true }) { Icon(Icons.Default.AutoAwesome, contentDescription = "Recommended providers", tint = Ink) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Providers",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Normal,
                    color = Ink,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 12.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search providers", color = Ink.copy(alpha = 0.45f)) },
                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Ink.copy(alpha = 0.5f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Ink.copy(alpha = 0.35f),
                        unfocusedBorderColor = Ink.copy(alpha = 0.2f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
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
                            if (providers.isEmpty()) "Tap the star icon for recommended providers, or + to add one." else "Try another name.",
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
    val brand = if (provider.enabled) providerBrandColor(provider.type) else AriMuted
    val host = remember(provider.baseUrl) {
        provider.baseUrl.trim().removePrefix("https://").removePrefix("http://").substringBefore('/')
    }
    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(providerShape(provider.type)).background(brand.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(provider.name.firstOrNull()?.uppercase() ?: "P", color = brand, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(provider.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AriInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (host.isBlank()) providerTypeLabel(provider.type) else host,
                    fontSize = 12.sp,
                    color = AriMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (provider.models.isEmpty()) "No models" else "${provider.models.size} models",
                    fontSize = 12.sp,
                    color = AriMuted
                )
            }
            Switch(
                checked = provider.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = AriInk,
                    checkedThumbColor = AriPaper,
                    checkedBorderColor = AriInk,
                    uncheckedTrackColor = AriTint,
                    uncheckedThumbColor = AriMuted,
                    uncheckedBorderColor = AriLine
                )
            )
        }
    }
}

@Composable
private fun MiniChip(text: String, bg: Color, fg: Color, onClick: (() -> Unit)? = null) {
    val base = Modifier.clip(RoundedCornerShape(50)).background(bg)
    Box(
        modifier = (if (onClick != null) base.clickable(onClick = onClick) else base)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 11.sp, color = fg, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ImportProviderDialog(onImport: (Provider) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val parsed = remember(text) { if (text.isBlank()) null else providerFromJson(text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = AriCard,
        title = { Text("Import provider", fontWeight = FontWeight.SemiBold, color = Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Paste a provider as JSON with name, type, baseUrl and apiKey.",
                    fontSize = 13.sp,
                    color = Ink.copy(alpha = 0.6f)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    minLines = 4,
                    maxLines = 8,
                    isError = text.isNotBlank() && parsed == null,
                    supportingText = { if (text.isNotBlank() && parsed == null) Text("Not a valid provider JSON") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onImport) }, enabled = parsed != null) {
                Text("Import", color = Ink, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Ink) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecommendedProvidersSheet(onPick: (ProviderType) -> Unit, onDismiss: () -> Unit) {
    val descriptions = mapOf(
        ProviderType.OPENAI to "GPT-4o and o-series models",
        ProviderType.GEMINI to "Gemini models, with Search and URL tools",
        ProviderType.ANTHROPIC to "Claude models",
        ProviderType.OLLAMA to "Models that run on your own computer",
        ProviderType.OPENAI_COMPATIBLE to "Any OpenAI-compatible API"
    )
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AriPaper) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Recommended providers", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            providerPresets.forEach { preset ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AriCard,
                    modifier = Modifier.fillMaxWidth().clickable { onPick(preset.type) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(providerShape(preset.type)).background(preset.color.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(preset.label.first().toString(), color = preset.color, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(preset.label, fontWeight = FontWeight.SemiBold, color = Ink)
                            Text(descriptions[preset.type].orEmpty(), fontSize = 12.sp, color = Ink.copy(alpha = 0.55f))
                        }
                        Icon(Icons.Default.Add, contentDescription = null, tint = Ink)
                    }
                }
            }
        }
    }
}
