package com.ariai.app.ui.screens

import com.ariai.app.util.tx

import androidx.compose.runtime.ReadOnlyComposable

import com.ariai.app.ui.theme.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.ReasoningLevel
import com.ariai.app.data.models.SearchMode
import kotlin.math.roundToInt

private val Accent: Color @Composable @ReadOnlyComposable get() = AriAccent
private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk
private val Tile: Color @Composable @ReadOnlyComposable get() = AriTint
/** Actions offered by the "+" button above the chat input. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentSheet(
    onPhoto: () -> Unit,
    onCamera: () -> Unit,
    onFile: () -> Unit,
    onCompress: () -> Unit,
    onExtensions: () -> Unit,
    onImage: () -> Unit,
    onVideo: () -> Unit,
    onAudio: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetShape = paperSheetShape(28.dp)
    PaperBottomSheet(
        onDismissRequest = onDismiss,
        shape = sheetShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(tx("Add to message"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SheetTile(Icons.Default.Image, tx("Photo"), Modifier.weight(1f), com.ariai.app.ui.theme.AriGemini) { onDismiss(); onPhoto() }
                SheetTile(Icons.Default.CameraAlt, tx("Camera"), Modifier.weight(1f), com.ariai.app.ui.theme.AriOpenAI) { onDismiss(); onCamera() }
                SheetTile(Icons.Default.Description, tx("Text file"), Modifier.weight(1f), com.ariai.app.ui.theme.AriAnthropic) { onDismiss(); onFile() }
            }
            HorizontalDivider(color = AriInk.copy(alpha = 0.06f))
            Text(tx("Generate"), fontSize = 12.sp, color = Ink.copy(alpha = 0.5f))
            SheetRow(Icons.Default.Image, tx("Image"), tx("Next message makes an image")) { onDismiss(); onImage() }
            SheetRow(Icons.Default.Movie, tx("Video"), tx("Next message makes a video")) { onDismiss(); onVideo() }
            SheetRow(Icons.Default.MusicNote, tx("Audio"), tx("Next message makes audio")) { onDismiss(); onAudio() }
            HorizontalDivider(color = AriInk.copy(alpha = 0.06f))
            Text(tx("Conversation"), fontSize = 12.sp, color = Ink.copy(alpha = 0.5f))
            SheetRow(Icons.Default.Archive, tx("Compress history"), tx("Summarize older messages to save context")) { onDismiss(); onCompress() }
            SheetRow(Icons.Default.Extension, tx("Extensions"), tx("MCP preview; tool calls are not active yet")) { onDismiss(); onExtensions() }
        }
    }
}

@Composable
private fun SheetTile(icon: ImageVector, label: String, modifier: Modifier, tint: Color? = null, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Tile)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint ?: Accent, modifier = Modifier.size(24.dp))
        Text(label, fontSize = 13.sp, color = Ink)
    }
}

@Composable
private fun SheetRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Tile)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink)
            Text(subtitle, fontSize = 12.sp, color = Ink.copy(alpha = 0.55f))
        }
    }
}

/** Slider for how much the model should think before answering. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThinkingDepthSheet(
    level: ReasoningLevel,
    onLevelChange: (ReasoningLevel) -> Unit,
    onDismiss: () -> Unit
) {
    val levels = ReasoningLevel.values()
    val sheetShape = paperSheetShape(28.dp)
    PaperBottomSheet(
        onDismissRequest = onDismiss,
        shape = sheetShape,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(tx("Thinking depth"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(
                "Not every model supports this. If nothing changes, check your provider's documentation.",
                fontSize = 13.sp,
                color = Ink.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Accent, modifier = Modifier.size(28.dp).padding(top = 6.dp))
            Text(level.label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Accent)
            Text(level.hint, fontSize = 13.sp, color = Ink.copy(alpha = 0.6f), textAlign = TextAlign.Center)

            Slider(
                value = levels.indexOf(level).toFloat(),
                onValueChange = { value ->
                    val picked = levels[value.roundToInt().coerceIn(0, levels.lastIndex)]
                    if (picked != level) onLevelChange(picked)
                },
                valueRange = 0f..levels.lastIndex.toFloat(),
                steps = levels.size - 2,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                levels.forEach { l ->
                    Text(
                        l.label,
                        fontSize = 11.sp,
                        color = if (l == level) Accent else Ink.copy(alpha = 0.45f),
                        fontWeight = if (l == level) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/** Compact search controls opened from the composer; API keys remain in Search service settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSheet(
    mode: SearchMode,
    modelSearchAvailable: Boolean,
    localSearchConfigured: Boolean,
    activeSearchProviderKey: String? = null,
    activeSearchProviderName: String? = null,
    onModeChange: (SearchMode) -> Unit,
    onConfigureLocal: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetShape = paperSheetShape(28.dp)
    PaperBottomSheet(
        onDismissRequest = onDismiss,
        shape = sheetShape,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(tx("Web search"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(
                    tx("Choose how AriAI searches for your next answer."),
                    fontSize = 12.sp,
                    color = Ink.copy(alpha = 0.55f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchModeChoice(
                    label = tx("Local Search"),
                    selected = mode == SearchMode.LOCAL,
                    modifier = Modifier.weight(1.15f).testTag("search-mode-local")
                ) {
                    when {
                        mode == SearchMode.LOCAL -> {
                            onModeChange(SearchMode.OFF)
                            onDismiss()
                        }
                        localSearchConfigured -> {
                            onModeChange(SearchMode.LOCAL)
                            onDismiss()
                        }
                        else -> {
                            onDismiss()
                            onConfigureLocal()
                        }
                    }
                }
                SearchModeChoice(
                    label = tx("Model search"),
                    selected = mode == SearchMode.MODEL,
                    modifier = Modifier.weight(1.25f).testTag("search-mode-model")
                ) {
                    when {
                        mode == SearchMode.MODEL -> {
                            onModeChange(SearchMode.OFF)
                            onDismiss()
                        }
                        modelSearchAvailable || localSearchConfigured -> {
                            onModeChange(SearchMode.MODEL)
                            onDismiss()
                        }
                        else -> {
                            onDismiss()
                            onConfigureLocal()
                        }
                    }
                }
                SearchModeChoice(
                    label = tx("Off"),
                    selected = mode == SearchMode.OFF,
                    modifier = Modifier.weight(0.72f).testTag("search-mode-off")
                ) {
                    if (mode != SearchMode.OFF) onModeChange(SearchMode.OFF)
                    onDismiss()
                }
            }
            HorizontalDivider(color = Ink.copy(alpha = 0.08f))
            SearchProviderSettingsRow(
                providerKey = activeSearchProviderKey,
                providerName = activeSearchProviderName,
                localSearchConfigured = localSearchConfigured,
                mode = mode,
                modelSearchAvailable = modelSearchAvailable,
                onClick = {
                    onDismiss()
                    onConfigureLocal()
                }
            )
        }
    }
}

@Composable
private fun SearchModeChoice(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val borderColor = if (selected) Accent.copy(alpha = 0.48f) else Ink.copy(alpha = 0.10f)
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 42.dp),
        shape = shape,
        color = if (selected) Accent.copy(alpha = 0.12f) else AriTint,
        contentColor = if (selected) Accent else Ink.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SearchProviderSettingsRow(
    providerKey: String?,
    providerName: String?,
    localSearchConfigured: Boolean,
    mode: SearchMode,
    modelSearchAvailable: Boolean,
    onClick: () -> Unit
) {
    val (mark, brand) = when (providerKey?.lowercase()) {
        "serper" -> "G" to Color(0xFF4285F4)
        "brave" -> "B" to Color(0xFFFB542B)
        "tavily" -> "T" to Color(0xFF14A89A)
        "exa" -> "E" to Color(0xFF6658D3)
        else -> "⌕" to Accent
    }
    val detail = when {
        mode == SearchMode.MODEL && modelSearchAvailable && !localSearchConfigured ->
            tx("Built-in search is active · add a key for Local Search")
        !localSearchConfigured -> tx("Add an API key to enable local search")
        mode == SearchMode.LOCAL -> tx("Selected for Local Search")
        mode == SearchMode.MODEL && modelSearchAvailable -> tx("Fallback provider · tap to manage")
        mode == SearchMode.MODEL -> tx("Used by Model search · tap to manage")
        else -> tx("Tap to manage this provider")
    }
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("search-provider-settings"),
        shape = RoundedCornerShape(18.dp),
        color = AriTint,
        border = BorderStroke(1.dp, Ink.copy(alpha = 0.09f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(brand.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                if (providerKey == null) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = brand, modifier = Modifier.size(20.dp))
                } else {
                    Text(mark, color = brand, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    providerName?.let(::tx) ?: tx("No provider configured"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                )
                Text(detail, fontSize = 11.sp, color = Ink.copy(alpha = 0.55f), maxLines = 1)
            }
            Icon(
                Icons.Default.Settings,
                contentDescription = tx("Search service settings"),
                tint = Ink.copy(alpha = 0.58f),
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
