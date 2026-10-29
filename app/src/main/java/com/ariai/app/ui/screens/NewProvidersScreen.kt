package com.ariai.app.ui.screens

import com.ariai.app.util.tx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.ariai.app.ui.theme.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.AIModel
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import com.ariai.app.data.models.providerFromJson
import com.ariai.app.ui.components.ProviderBrandMark

private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk
fun providerTypeLabel(type: ProviderType): String = when (type) {
    ProviderType.OPENAI -> "OpenAI"
    ProviderType.GEMINI -> "Google Gemini"
    ProviderType.ANTHROPIC -> "Anthropic"
    ProviderType.OPENAI_COMPATIBLE -> tx("OpenAI compatible")
    ProviderType.OLLAMA -> tx("Ollama (local)")
    ProviderType.CUSTOM -> tx("Custom")
}

/** Each provider type has its own brand colour. */
fun providerBrandColor(type: ProviderType): Color = when (type) {
    ProviderType.OPENAI -> AriOpenAI
    ProviderType.GEMINI -> AriGemini
    ProviderType.ANTHROPIC -> AriAnthropic
    ProviderType.OLLAMA -> AriOllama
    ProviderType.OPENAI_COMPATIBLE, ProviderType.CUSTOM -> AriCustom
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
    onToggleProvider: (Provider) -> Unit = {},
    fetchModels: (suspend (Provider) -> List<AIModel>)? = null,
    onSaveProvider: ((Provider, (Provider?) -> Unit) -> Unit)? = null
) {
    var query by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var showAddProvider by remember { mutableStateOf(false) }
    var addProviderPreset by remember { mutableStateOf<ProviderType?>(null) }
    val fetchModelsForAdd = fetchModels
    val saveProviderForAdd = onSaveProvider
    fun openAddProvider(preset: ProviderType? = null) {
        if (fetchModelsForAdd != null && saveProviderForAdd != null) {
            addProviderPreset = preset
            showAddProvider = true
        } else if (preset != null) {
            onAddPreset(preset)
        } else {
            onAddProvider()
        }
    }
    val visible = providers.filter {
        query.isBlank() ||
            it.name.contains(query, ignoreCase = true) ||
            providerTypeLabel(it.type).contains(query, ignoreCase = true) ||
            it.baseUrl.contains(query, ignoreCase = true)
    }
    val visiblePresets = providerPresets.filter { preset ->
        query.isBlank() ||
            preset.label.contains(query, ignoreCase = true) ||
            providerTypeLabel(preset.type).contains(query, ignoreCase = true) ||
            providerPresetDescription(preset.type).contains(query, ignoreCase = true)
    }
    val activeProviders = visible.filter { it.enabled }
    val disabledProviders = visible.filter { !it.enabled }

    if (showImport) {
        ImportProviderDialog(
            onImport = { onImportProvider(it); showImport = false },
            onDismiss = { showImport = false }
        )
    }
    Scaffold(
        containerColor = AriPaper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AriPaper),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tx("Back"), tint = Ink) } },
                title = {},
                actions = {
                    TextButton(onClick = { openAddProvider() }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = AriAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(tx("Add provider"), color = AriAccent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                    IconButton(onClick = { showImport = true }) {
                        Icon(Icons.Default.FileOpen, contentDescription = tx("Import provider"), tint = Ink)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)) {
                    Text(
                        tx("Providers"),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink
                    )
                }
            }

            item { ProviderNetworkHero(providers) }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(tx("Search providers"), color = Ink.copy(alpha = 0.45f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AriAccent) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AriAccent.copy(alpha = 0.45f),
                        unfocusedBorderColor = AriLine.copy(alpha = 0.7f),
                        focusedContainerColor = AriCard,
                        unfocusedContainerColor = AriCard
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("provider-search")
                )
            }

            if (visiblePresets.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx("Quick connect"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            Text(tx("OpenAI, Gemini, Claude and more"), fontSize = 12.sp, color = AriMuted)
                        }
                        Text(
                            visiblePresets.size.toString().padStart(2, '0'),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AriAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(AriAccentSoft)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                item {
                    ProviderCatalogGrid(
                        presets = visiblePresets,
                        providers = providers,
                        onSelect = { openAddProvider(it) }
                    )
                }
            }

            if (query.isNotBlank() && visible.isEmpty() && visiblePresets.isEmpty()) {
                item { EmptyProviderSearchState() }
            }

            if (visible.isNotEmpty()) {
                item { SectionTitle(tx("Your connections")) }
                if (activeProviders.isNotEmpty()) {
                    item { SectionTitle(tx("Active")) }
                    itemsIndexed(activeProviders, key = { _, provider -> "provider:${provider.id}" }) { _, provider ->
                        ProviderCard(
                            provider = provider,
                            onClick = { onEditProvider(provider) },
                            onToggle = { onToggleProvider(provider) }
                        )
                    }
                }
                if (disabledProviders.isNotEmpty()) {
                    item { SectionTitle(tx("Turned off")) }
                    itemsIndexed(disabledProviders, key = { _, provider -> "provider:${provider.id}" }) { _, provider ->
                        ProviderCard(
                            provider = provider,
                            onClick = { onEditProvider(provider) },
                            onToggle = { onToggleProvider(provider) }
                        )
                    }
                }
            } else if (providers.isEmpty() && query.isBlank()) {
                item { EmptyConnectionsHint() }
            }
        }
    }

    if (showAddProvider && fetchModelsForAdd != null && saveProviderForAdd != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showAddProvider = false
                addProviderPreset = null
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AriPaper,
            contentColor = AriInk
        ) {
            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.90f)) {
                AddProviderScreen(
                    presetType = addProviderPreset,
                    fetchModels = fetchModelsForAdd,
                    onSave = { provider ->
                        saveProviderForAdd(provider) { savedProvider ->
                            if (savedProvider != null) {
                                showAddProvider = false
                                addProviderPreset = null
                            }
                        }
                    },
                    onBack = {
                        showAddProvider = false
                        addProviderPreset = null
                    },
                    existingProviderNames = providers.map { it.name }.toSet(),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private fun providerPresetDescription(type: ProviderType): String = when (type) {
    ProviderType.OPENAI -> tx("GPT-4o and o-series models")
    ProviderType.GEMINI -> tx("Gemini models, with Search and URL tools")
    ProviderType.ANTHROPIC -> tx("Claude models")
    ProviderType.OLLAMA -> tx("Models that run on your own computer")
    ProviderType.OPENAI_COMPATIBLE, ProviderType.CUSTOM -> tx("Any OpenAI-compatible API")
}

@Composable
private fun ProviderNetworkHero(providers: List<Provider>) {
    val activeCount = providers.count { it.enabled }
    val modelCount = providers.sumOf { it.models.size }
    val shape = RoundedCornerShape(25.dp)
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(AriInk, AriAccent, AriAccent.copy(alpha = 0.92f))))
            .padding(horizontal = 18.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(tx("Your AI network"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(
                if (providers.isEmpty()) tx("Connect a provider to start")
                else "${providers.size} ${tx("connections")}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ProviderNetworkMetric(Icons.Default.Storage, "$modelCount ${tx("models")}")
                ProviderNetworkMetric(Icons.Default.CheckCircle, "$activeCount ${tx("active")}")
            }
        }
        ProviderNetworkGlyph()
    }
}

@Composable
private fun ProviderNetworkMetric(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, contentDescription = null, tint = AriAccentLight, modifier = Modifier.size(14.dp))
        Text(value, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1)
    }
}

@Composable
private fun ProviderNetworkGlyph() {
    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(78.dp).border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)) {}
        Box(Modifier.size(56.dp).border(1.dp, Color.White.copy(alpha = 0.21f), CircleShape)) {}
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Storage, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
        }
        Box(
            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-7).dp, y = 7.dp)
                .size(9.dp).clip(CircleShape).background(AriAccentLight)
        )
        Box(
            modifier = Modifier.align(Alignment.BottomStart).offset(x = 7.dp, y = (-7).dp)
                .size(8.dp).clip(CircleShape).background(AriAccentGreenSoft)
        )
    }
}

@Composable
private fun ProviderCatalogGrid(
    presets: List<ProviderPreset>,
    providers: List<Provider>,
    onSelect: (ProviderType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        presets.chunked(2).forEach { rowPresets ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                rowPresets.forEach { preset ->
                    ProviderCatalogCard(
                        preset = preset,
                        configuredCount = providers.count { it.type == preset.type },
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(preset.type) }
                    )
                }
                if (rowPresets.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProviderCatalogCard(
    preset: ProviderPreset,
    configuredCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        onClick = onClick,
        modifier = modifier.height(128.dp).testTag("provider-catalog-${preset.type.name.lowercase()}"),
        shape = shape,
        color = AriCard,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(0.75.dp, AriLine.copy(alpha = 0.78f))
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(3.dp).background(preset.color))
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 11.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProviderBrandMark(type = preset.type, name = preset.label, size = 32.dp)
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier.size(25.dp).clip(CircleShape).background(AriAccentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = AriAccent, modifier = Modifier.size(16.dp))
                    }
                }
                Text(preset.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1)
                Text(
                    providerPresetDescription(preset.type),
                    fontSize = 10.sp,
                    color = AriMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (configuredCount > 0) {
                    Text("$configuredCount ${tx("configured")}", fontSize = 10.sp, color = AriAccent, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun EmptyConnectionsHint() {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("provider-empty-connections"),
        shape = RoundedCornerShape(19.dp),
        color = AriAccentSoft.copy(alpha = 0.42f),
        border = BorderStroke(0.75.dp, AriAccent.copy(alpha = 0.12f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.76f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = AriAccent, modifier = Modifier.size(19.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tx("No providers yet"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(tx("Your first connection will appear here"), fontSize = 11.sp, color = AriMuted)
            }
        }
    }
}

@Composable
private fun EmptyProviderSearchState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(tx("No match"), fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 15.sp)
        Text(tx("Try another name."), color = AriMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        color = Ink,
        modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 1.dp)
    )
}

/** A connection card with a distinct identity area and a clear status/model footer. */
@Composable
private fun ProviderCard(provider: Provider, onClick: () -> Unit, onToggle: () -> Unit) {
    val host = remember(provider.baseUrl) {
        provider.baseUrl.trim().removePrefix("https://").removePrefix("http://").substringBefore('/')
    }
    val statusBackground = if (provider.enabled) AriAccentGreenSoft else AriTint
    val statusForeground = if (provider.enabled) AriAccentGreen else AriMuted
    val shape = RoundedCornerShape(22.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(tween(220, easing = FastOutSlowInEasing))
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag("provider-card-${provider.id}"),
        shape = shape,
        color = AriCard,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(0.75.dp, AriLine.copy(alpha = 0.8f))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Box(
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(15.dp))
                        .background(providerBrandColor(provider.type).copy(alpha = 0.11f)),
                    contentAlignment = Alignment.Center
                ) {
                    ProviderBrandMark(provider = provider, size = 34.dp)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        provider.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = AriInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (host.isBlank()) providerTypeLabel(provider.type) else host,
                        fontSize = 12.sp,
                        color = AriMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Switch(
                    modifier = Modifier.testTag("provider-toggle-${provider.id}"),
                    checked = provider.enabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = AriAccent.copy(alpha = 0.42f),
                        checkedThumbColor = AriAccent,
                        checkedBorderColor = AriAccent,
                        uncheckedTrackColor = AriTint,
                        uncheckedThumbColor = AriMuted,
                        uncheckedBorderColor = AriLine
                    )
                )
            }
            HorizontalDivider(color = AriLine.copy(alpha = 0.62f), modifier = Modifier.padding(horizontal = 14.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniChip(
                    text = if (provider.enabled) tx("Active") else tx("Turned off"),
                    bg = statusBackground,
                    fg = statusForeground
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Storage, contentDescription = null, tint = AriMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    if (provider.models.isEmpty()) tx("No models") else "${provider.models.size} ${tx("models")}",
                    fontSize = 11.sp,
                    color = AriMuted,
                    maxLines = 1
                )
                Spacer(Modifier.weight(1f))
                Text(tx("Manage"), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AriAccent)
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = AriAccent, modifier = Modifier.size(18.dp))
            }
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
    val dialogShape = paperPopupShape(28.dp)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = paperPopupModifier(dialogShape),
        shape = dialogShape,
        containerColor = AriCard,
        tonalElevation = 8.dp,
        title = { Text(tx("Import provider"), fontWeight = FontWeight.SemiBold, color = Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    tx("Paste a provider as JSON with name, type, baseUrl and apiKey."),
                    fontSize = 13.sp,
                    color = Ink.copy(alpha = 0.6f)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(tx("Provider JSON")) },
                    placeholder = {
                        Text(
                            "{\n  \"name\": \"My provider\",\n  \"type\": \"OPENAI_COMPATIBLE\",\n  \"baseUrl\": \"https://api.example.com/v1\",\n  \"apiKey\": \"YOUR_API_KEY\"\n}",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = AriMuted.copy(alpha = 0.78f)
                        )
                    },
                    minLines = 5,
                    maxLines = 8,
                    isError = text.isNotBlank() && parsed == null,
                    supportingText = { if (text.isNotBlank() && parsed == null) Text(tx("Not a valid provider JSON")) },
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start, textDirection = TextDirection.Ltr),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink,
                        cursorColor = AriAccent,
                        focusedBorderColor = AriAccent,
                        unfocusedBorderColor = AriAccent.copy(alpha = 0.48f),
                        errorBorderColor = AriError,
                        focusedContainerColor = AriPaper,
                        unfocusedContainerColor = AriPaper,
                        errorContainerColor = AriPaper
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("provider-import-json")
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onImport) }, enabled = parsed != null) {
                Text(tx("Import"), color = Ink, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tx("Cancel"), color = Ink) } }
    )
}
