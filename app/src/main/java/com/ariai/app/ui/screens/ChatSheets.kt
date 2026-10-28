package com.ariai.app.ui.screens

import com.ariai.app.util.tx

import androidx.compose.runtime.ReadOnlyComposable

import com.ariai.app.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ReasoningLevel
import com.ariai.app.data.models.SearchMode
import kotlin.math.roundToInt

private val Accent: Color @Composable @ReadOnlyComposable get() = AriAccent
private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk
private val Tile: Color @Composable @ReadOnlyComposable get() = AriAccentSoft

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
    PaperBottomSheet(
        onDismissRequest = onDismiss,
        shape = paperSheetShape(32.dp),
        skipPartiallyExpanded = true,
        showOutline = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(tx("Add to message"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SheetTile(Icons.Outlined.Image, tx("Photo"), Modifier.weight(1f)) { onDismiss(); onPhoto() }
                SheetTile(Icons.Outlined.CameraAlt, tx("Camera"), Modifier.weight(1f)) { onDismiss(); onCamera() }
                SheetTile(Icons.Outlined.Description, tx("Text file"), Modifier.weight(1f)) { onDismiss(); onFile() }
            }
            Text(tx("Generate"), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Ink.copy(alpha = 0.55f))
            SheetRow(Icons.Outlined.Image, tx("Image"), tx("Next message makes an image")) { onDismiss(); onImage() }
            SheetRow(Icons.Outlined.Movie, tx("Video"), tx("Next message makes a video")) { onDismiss(); onVideo() }
            SheetRow(Icons.Outlined.MusicNote, tx("Audio"), tx("Next message makes audio")) { onDismiss(); onAudio() }
            Text(tx("Conversation"), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Ink.copy(alpha = 0.55f))
            SheetRow(Icons.Outlined.Archive, tx("Compress history"), tx("Summarize older messages to save context")) { onDismiss(); onCompress() }
            SheetRow(Icons.Outlined.Extension, tx("Extensions"), tx("MCP preview; tool calls are not active yet")) { onDismiss(); onExtensions() }
        }
    }
}

@Composable
private fun SheetTile(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Tile)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Accent, modifier = Modifier.size(28.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink)
    }
}

@Composable
private fun SheetRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(AriPaper)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(subtitle, fontSize = 12.sp, color = Ink.copy(alpha = 0.55f), lineHeight = 16.sp)
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

/** Compact chooser with exactly two built-in providers and inline key setup. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSheet(
    mode: SearchMode,
    providers: List<Provider>,
    selectedProviderId: String?,
    selectedModelId: String?,
    onModeChange: (SearchMode) -> Unit,
    onSelectSearchModel: (String, String) -> Unit,
    onSaveProvider: (Provider, (Provider?) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val options = remember(providers, selectedProviderId, selectedModelId) {
        nativeSearchProviderOptions(providers, selectedProviderId, selectedModelId)
    }
    PaperBottomSheet(
        onDismissRequest = onDismiss,
        shape = paperSheetShape(32.dp),
        showOutline = false
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(AriAccentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = Accent, modifier = Modifier.size(34.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tx("Web search"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(
                        tx("Choose Gemini or Anthropic. Web search reuses the selected AI provider's API key; no separate search key is needed."),
                        fontSize = 12.sp,
                        color = Ink.copy(alpha = 0.58f),
                        lineHeight = 17.sp
                    )
                }
            }
            options.forEach { option ->
                NativeSearchProviderCard(
                    option = option,
                    mode = mode,
                    selectedProviderId = selectedProviderId,
                    selectedModelId = selectedModelId,
                    onModeChange = onModeChange,
                    onSelectModel = onSelectSearchModel,
                    onSaveProvider = onSaveProvider,
                    onAfterSelection = onDismiss,
                    chatSheetStyle = true
                )
            }
            if (mode == SearchMode.MODEL) {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Accent,
                    tonalElevation = 0.dp
                ) {
                    Text(
                        tx("Web search is on. Tap the selected provider again to turn it off."),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}
