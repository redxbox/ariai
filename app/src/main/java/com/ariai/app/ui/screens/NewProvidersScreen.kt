package com.ariai.app.ui.screens

import com.ariai.app.util.tx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.ariai.app.ui.theme.*

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import com.ariai.app.data.models.providerFromJson
import com.ariai.app.ui.components.ProviderBrandMark

private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk
private val DisabledTint: Color @Composable @ReadOnlyComposable get() = AriTint

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
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tx("Back"), tint = Ink) } },
                title = {},
                actions = {
                    IconButton(onClick = onAddProvider) { Icon(Icons.Default.Add, contentDescription = tx("Add provider"), tint = Ink) }
                    IconButton(onClick = { showImport = true }) { Icon(Icons.Default.FileOpen, contentDescription = tx("Import provider"), tint = Ink) }
                    IconButton(onClick = { showRecommended = true }) { Icon(Icons.Default.AutoAwesome, contentDescription = tx("Recommended providers"), tint = Ink) }
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
                    tx("Providers"),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Normal,
                    color = Ink,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                )
            }

            item {
                val active = providers.count { it.enabled }
                val models = providers.sumOf { it.models.size }
                Text(
                    if (providers.isEmpty()) tx("Connect a provider to start")
                    else "$active ${tx("active")}  ·  $models ${tx("models")}",
                    fontSize = 13.sp,
                    color = AriMuted,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(tx("Search providers"), color = Ink.copy(alpha = 0.45f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AriAccent) },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AriAccent.copy(alpha = 0.45f),
                        unfocusedBorderColor = AriLine.copy(alpha = 0.7f),
                        focusedContainerColor = AriCard,
                        unfocusedContainerColor = AriCard
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (visible.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(AriTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AriInk, modifier = Modifier.size(28.dp))
                        }
                        Text(if (providers.isEmpty()) tx("No providers yet") else tx("No match"), fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 16.sp)
                        Text(
                            if (providers.isEmpty()) tx("Tap the star icon for recommended providers, or + to add one.") else tx("Try another name."),
                            color = AriMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }

            val onList = visible.filter { it.enabled }
            val offList = visible.filter { !it.enabled }
            if (onList.isNotEmpty()) {
                item(key = "providers:active") { SectionTitle(tx("Active")) }
                itemsIndexed(onList, key = { _, p -> "provider:${p.id}" }) { _, provider ->
                    ProviderCard(provider = provider, onClick = { onEditProvider(provider) }, onToggle = { onToggleProvider(provider) })
                }
            }
            if (offList.isNotEmpty()) {
                item(key = "providers:turned-off") { SectionTitle(tx("Turned off")) }
                itemsIndexed(offList, key = { _, p -> "provider:${p.id}" }) { _, provider ->
                    ProviderCard(provider = provider, onClick = { onEditProvider(provider) }, onToggle = { onToggleProvider(provider) })
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = AriMuted,
        modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 2.dp)
    )
}

/**
 * Rounded provider card with a distinct brand mark, compact metadata chips, and a stable enable switch.
 */
@Composable
private fun ProviderCard(provider: Provider, onClick: () -> Unit, onToggle: () -> Unit) {
    val host = remember(provider.baseUrl) {
        provider.baseUrl.trim().removePrefix("https://").removePrefix("http://").substringBefore('/')
    }
    val statusBackground = if (provider.enabled) AriAccentSoft else AriTint
    val statusForeground = if (provider.enabled) AriAccent else AriMuted
    val shape = RoundedCornerShape(24.dp)
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProviderBrandMark(provider = provider, size = 48.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    provider.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MiniChip(
                        text = if (provider.models.isEmpty()) tx("No models") else "${provider.models.size} ${tx("models")}",
                        bg = AriTagBlueBg,
                        fg = AriTagBlueFg
                    )
                    MiniChip(
                        text = if (provider.enabled) tx("Active") else tx("Turned off"),
                        bg = statusBackground,
                        fg = statusForeground
                    )
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecommendedProvidersSheet(onPick: (ProviderType) -> Unit, onDismiss: () -> Unit) {
    val descriptions = mapOf(
        ProviderType.OPENAI to "GPT-4o and o-series models",
        ProviderType.GEMINI to tx("Gemini models, with Search and URL tools"),
        ProviderType.ANTHROPIC to tx("Claude models"),
        ProviderType.OLLAMA to tx("Models that run on your own computer"),
        ProviderType.OPENAI_COMPATIBLE to tx("Any OpenAI-compatible API")
    )
    val sheetShape = paperSheetShape(28.dp)
    PaperBottomSheet(
        onDismissRequest = onDismiss,
        shape = sheetShape,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(tx("Recommended providers"), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            providerPresets.forEach { preset ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AriPaper,
                    tonalElevation = 0.dp,
                    shadowElevation = 1.dp,
                    border = BorderStroke(0.5.dp, AriLine),
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
