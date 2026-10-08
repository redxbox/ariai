package com.ariai.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

private val Accent = Color(0xFF6C4DFF)
private val Ink = Color(0xFF1C1B1F)
private val SheetBg = Color(0xFFF7F6FB)
private val Tile = Color(0xFFEFEDF6)

/** Actions offered by the "+" button above the chat input. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentSheet(
    onPhoto: () -> Unit,
    onCamera: () -> Unit,
    onFile: () -> Unit,
    onCompress: () -> Unit,
    onExtensions: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SheetBg) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add to message", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SheetTile(Icons.Default.Image, "Photo", Modifier.weight(1f)) { onDismiss(); onPhoto() }
                SheetTile(Icons.Default.CameraAlt, "Camera", Modifier.weight(1f)) { onDismiss(); onCamera() }
                SheetTile(Icons.Default.Description, "Text file", Modifier.weight(1f)) { onDismiss(); onFile() }
            }
            HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
            Text("Conversation", fontSize = 12.sp, color = Ink.copy(alpha = 0.5f))
            SheetRow(Icons.Default.Archive, "Compress history", "Summarize older messages to save context") { onDismiss(); onCompress() }
            SheetRow(Icons.Default.Extension, "Extensions", "Manage MCP tool servers") { onDismiss(); onExtensions() }
        }
    }
}

@Composable
private fun SheetTile(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Tile)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Accent, modifier = Modifier.size(24.dp))
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
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SheetBg) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Thinking depth", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink)
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
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SheetBg) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Web search", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SearchCard(
                    icon = Icons.Default.AutoAwesome,
                    title = "Model search",
                    subtitle = if (modelSearchAvailable) "Built into the provider (Gemini)" else "Only for Gemini providers",
                    selected = mode == SearchMode.MODEL,
                    enabled = modelSearchAvailable,
                    modifier = Modifier.weight(1f)
                ) { onModeChange(SearchMode.MODEL); onDismiss() }
                SearchCard(
                    icon = Icons.Default.Public,
                    title = "Local search",
                    subtitle = if (localSearchConfigured) "Uses your search service" else "No key yet. Tap to set up",
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
                Text("Turn off search", color = if (mode != SearchMode.OFF) Accent else Ink.copy(alpha = 0.35f))
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
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Accent.copy(alpha = 0.10f) else Color.White)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp))
            if (selected) Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = Accent, modifier = Modifier.size(18.dp))
        }
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Accent else Ink)
        Text(subtitle, fontSize = 12.sp, color = Ink.copy(alpha = 0.55f))
    }
}
