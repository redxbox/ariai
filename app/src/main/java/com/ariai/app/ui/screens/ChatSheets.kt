package com.ariai.app.ui.screens

import com.ariai.app.util.tx

import androidx.compose.runtime.ReadOnlyComposable

import com.ariai.app.ui.theme.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.ReasoningLevel
import com.ariai.app.data.models.SearchMode
import kotlin.math.roundToInt

private val Accent: Color @Composable @ReadOnlyComposable get() = AriInk
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

/** Chooses where web results come from for the next message. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSheet(
    mode: SearchMode,
    modelSearchAvailable: Boolean,
    localSearchConfigured: Boolean,
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(tx("Web search"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchCard(
                    icon = Icons.Default.AutoAwesome,
                    title = tx("Model search"),
                    subtitle = when {
                        modelSearchAvailable -> tx("Built into the provider")
                        localSearchConfigured -> tx("Uses your search service")
                        else -> tx("No key yet. Tap to set up")
                    },
                    selected = mode == SearchMode.MODEL,
                    enabled = true,
                    modifier = Modifier.weight(1f)
                ) {
                    if (modelSearchAvailable || localSearchConfigured) onModeChange(SearchMode.MODEL) else onConfigureLocal()
                    onDismiss()
                }
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    color = AriInk.copy(alpha = 0.14f)
                )
                SearchCard(
                    icon = Icons.Default.Public,
                    title = tx("Local search"),
                    subtitle = if (localSearchConfigured) tx("Uses your search service") else tx("No key yet. Tap to set up"),
                    selected = mode == SearchMode.LOCAL,
                    enabled = true,
                    modifier = Modifier.weight(1f)
                ) {
                    if (localSearchConfigured) onModeChange(SearchMode.LOCAL) else onConfigureLocal()
                    onDismiss()
                }
            }
            TextButton(
                onClick = { onModeChange(SearchMode.OFF); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
                enabled = mode != SearchMode.OFF
            ) {
                Text(tx("Turn off search"), color = if (mode != SearchMode.OFF) Accent else Ink.copy(alpha = 0.35f))
            }
        }
    }
}

@Composable
private fun SearchCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(18.dp)
    val cardColor = if (selected) Accent.copy(alpha = 0.10f) else AriTint
    val borderColor = if (selected) Accent.copy(alpha = 0.32f) else AriInk.copy(alpha = 0.12f)
    Column(
        modifier = modifier
            .clip(cardShape)
            .background(cardColor, cardShape)
            .border(BorderStroke(1.dp, borderColor), cardShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp))
            if (selected) Icon(Icons.Default.CheckCircle, contentDescription = tx("Selected"), tint = Accent, modifier = Modifier.size(18.dp))
        }
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Accent else Ink)
        Text(subtitle, fontSize = 12.sp, color = Ink.copy(alpha = 0.55f))
    }
}
