package com.ariai.app.ui.screens

import com.ariai.app.util.tx

import androidx.compose.runtime.ReadOnlyComposable

import com.ariai.app.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ReasoningLevel
import com.ariai.app.data.models.SearchMode
import com.ariai.app.data.models.resolveSearchProviderKey
import kotlin.math.roundToInt

private val Accent: Color @Composable @ReadOnlyComposable get() = AriAccent
private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk

/** Source shortcuts and generation actions shown by the chat composer’s plus button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentSheet(
    onPhoto: () -> Unit,
    onVideoFile: () -> Unit,
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
        shape = paperSheetShape(40.dp),
        showOutline = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 520.dp, max = 700.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AttachmentSourceTile(
                    icon = Icons.Outlined.Image,
                    label = tx("Photos"),
                    tag = "attachment-source-photos"
                ) { onDismiss(); onPhoto() }
                AttachmentSourceTile(
                    icon = Icons.Outlined.Movie,
                    label = tx("Videos"),
                    tag = "attachment-source-videos"
                ) { onDismiss(); onVideoFile() }
                AttachmentSourceTile(
                    icon = Icons.Outlined.Description,
                    label = tx("Files"),
                    tag = "attachment-source-files"
                ) { onDismiss(); onFile() }
                AttachmentSourceTile(
                    icon = Icons.Outlined.CameraAlt,
                    label = tx("Camera"),
                    tag = "attachment-source-camera"
                ) { onDismiss(); onCamera() }
            }

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                AttachmentActionRow(
                    icon = Icons.Outlined.Image,
                    title = tx("Create images"),
                    subtitle = tx("Visualise and edit"),
                    tag = "attachment-action-create-images"
                ) { onDismiss(); onImage() }
                AttachmentActionRow(
                    icon = Icons.Outlined.Movie,
                    title = tx("Create videos"),
                    subtitle = tx("Bring ideas to life"),
                    tag = "attachment-action-create-videos"
                ) { onDismiss(); onVideo() }
                AttachmentActionRow(
                    icon = Icons.Outlined.MusicNote,
                    title = tx("Create music"),
                    subtitle = tx("Make audio tracks"),
                    tag = "attachment-action-create-music"
                ) { onDismiss(); onAudio() }
                AttachmentActionRow(
                    icon = Icons.Outlined.Archive,
                    title = tx("Compress history"),
                    subtitle = tx("Summarize older messages to save context"),
                    tag = "attachment-action-compress-history"
                ) { onDismiss(); onCompress() }
                AttachmentActionRow(
                    icon = Icons.Outlined.Extension,
                    title = tx("Extensions"),
                    subtitle = tx("MCP preview; tool calls are not active yet"),
                    tag = "attachment-action-extensions"
                ) { onDismiss(); onExtensions() }
            }
        }
    }
}

@Composable
private fun AttachmentSourceTile(
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(100.dp).height(112.dp).testTag(tag),
        shape = RoundedCornerShape(40.dp),
        color = Ink.copy(alpha = 0.055f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
        ) {
            Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(27.dp))
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Ink, maxLines = 1)
        }
    }
}

@Composable
private fun AttachmentActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(27.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Ink, maxLines = 1)
            Text(subtitle, fontSize = 14.sp, color = AriMuted, maxLines = 1)
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

/** Compact local-service picker plus the existing Gemini/Anthropic native-search path. */
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
    onDismiss: () -> Unit,
    searchProvider: String? = null,
    searchKeys: Map<String, String> = emptyMap(),
    onSearchProviderChange: (String) -> Unit = {},
    onSaveSearchKey: (String, String) -> Unit = { _, _ -> }
) {
    val options = remember(providers, selectedProviderId, selectedModelId) {
        nativeSearchProviderOptions(providers, selectedProviderId, selectedModelId)
    }
    var localProvider by remember(searchProvider, searchKeys) {
        mutableStateOf(resolveSearchProviderKey(searchKeys, searchProvider) ?: "bing")
    }
    PaperBottomSheet(
        onDismissRequest = onDismiss,
        shape = paperSheetShape(30.dp),
        showOutline = false
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .animateContentSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(16.dp)).background(AriAccentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = Accent, modifier = Modifier.size(23.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(tx("Web search"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text(
                        tx("Choose how AriAI searches for your next answer."),
                        fontSize = 12.sp,
                        color = Ink.copy(alpha = 0.58f),
                        lineHeight = 16.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(AriTint).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SearchModeTab(
                    label = "Local Search",
                    icon = Icons.Default.Public,
                    selected = mode == SearchMode.LOCAL,
                    tag = "search-mode-local",
                    modifier = Modifier.weight(1f)
                ) {
                    onSearchProviderChange(localProvider)
                    onModeChange(SearchMode.LOCAL)
                }
                SearchModeTab(
                    label = "Built-in",
                    icon = Icons.Default.AutoAwesome,
                    selected = mode == SearchMode.MODEL,
                    tag = "search-mode-model",
                    modifier = Modifier.weight(1f)
                ) { onModeChange(SearchMode.MODEL) }
                SearchModeTab(
                    label = "Off",
                    icon = Icons.Default.Close,
                    selected = mode == SearchMode.OFF,
                    tag = "search-mode-off",
                    modifier = Modifier.weight(1f)
                ) { onModeChange(SearchMode.OFF) }
            }

            AnimatedVisibility(visible = mode == SearchMode.LOCAL) {
                LocalSearchProviderPicker(
                    selectedProvider = localProvider,
                    searchKeys = searchKeys,
                    onProviderChange = { provider ->
                        localProvider = provider
                        onSearchProviderChange(provider)
                    },
                    onSaveKey = onSaveSearchKey
                )
            }

            AnimatedVisibility(visible = mode == SearchMode.MODEL) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(
                        tx("Use Gemini or Anthropic's built-in search with the selected AI provider."),
                        fontSize = 12.sp,
                        color = Ink.copy(alpha = 0.58f),
                        lineHeight = 16.sp
                    )
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
                }
            }
        }
    }
}

@Composable
private fun SearchModeTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    tag: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) Accent else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 190),
        label = "search-mode-background"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else Ink.copy(alpha = 0.68f),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 190),
        label = "search-mode-content"
    )
    Surface(
        onClick = onClick,
        modifier = modifier.height(42.dp).testTag(tag),
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        contentColor = contentColor,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
            Text(text = tx(label), fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun LocalSearchProviderPicker(
    selectedProvider: String,
    searchKeys: Map<String, String>,
    onProviderChange: (String) -> Unit,
    onSaveKey: (String, String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var settingsExpanded by remember { mutableStateOf(false) }
    val braveKey = searchKeys["brave"].orEmpty()
    var braveKeyDraft by remember(braveKey) { mutableStateOf(braveKey) }
    var showKey by remember { mutableStateOf(false) }
    val isBrave = selectedProvider == "brave"
    val providerLabel = if (isBrave) "Brave" else "Bing"
    val providerSubtitle = when {
        !isBrave -> tx("No API key needed")
        braveKey.isNotBlank() -> tx("API key saved on this device")
        else -> tx("API key required")
    }
    val menuShape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("search-provider-row"),
            shape = RoundedCornerShape(22.dp),
            color = AriPaper,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(17.dp))
                        .clickable { menuExpanded = true }
                        .padding(horizontal = 4.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SearchServiceMark(selectedProvider, 38.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(tx(providerLabel), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text(providerSubtitle, fontSize = 11.sp, color = Ink.copy(alpha = 0.58f), maxLines = 1)
                    }
                }
                IconButton(
                    onClick = { settingsExpanded = !settingsExpanded },
                    modifier = Modifier.size(40.dp).testTag("search-provider-settings")
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = tx("Search service settings"),
                        tint = if (settingsExpanded) Accent else Ink.copy(alpha = 0.60f),
                        modifier = Modifier.size(19.dp)
                    )
                }
                Box {
                    IconButton(
                        onClick = { menuExpanded = !menuExpanded },
                        modifier = Modifier.size(40.dp).testTag("search-provider-dropdown")
                    ) {
                        Icon(
                            if (menuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = tx("Choose search provider"),
                            tint = Ink.copy(alpha = 0.65f),
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(AriCard, menuShape)
                            .then(paperPopupModifier(menuShape, 8.dp))
                            .testTag("search-provider-menu")
                    ) {
                        listOf("bing", "brave").forEach { provider ->
                            val isSelected = provider == selectedProvider
                            DropdownMenuItem(
                                text = { Text(tx(if (provider == "bing") "Bing" else "Brave"), fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                                leadingIcon = { SearchServiceMark(provider, 28.dp) },
                                trailingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = tx("Selected"), tint = Accent, modifier = Modifier.size(18.dp)) }
                                } else null,
                                modifier = Modifier.testTag("search-provider-option-$provider"),
                                onClick = {
                                    onProviderChange(provider)
                                    menuExpanded = false
                                    if (provider == "brave" && braveKey.isBlank()) settingsExpanded = true
                                }
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = settingsExpanded) {
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("search-provider-editor"),
                shape = RoundedCornerShape(19.dp),
                color = AriCard,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    if (isBrave) {
                        Text(tx("Brave API key"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        OutlinedTextField(
                            value = braveKeyDraft,
                            onValueChange = { braveKeyDraft = it },
                            label = { Text(tx("API key")) },
                            placeholder = { Text(tx("Enter a Brave Search API key.")) },
                            singleLine = true,
                            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showKey = !showKey }) {
                                    Icon(
                                        if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = tx(if (showKey) "Hide key" else "Show key"),
                                        tint = Ink.copy(alpha = 0.58f)
                                    )
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Accent,
                                unfocusedBorderColor = Ink.copy(alpha = 0.16f)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("search-provider-key")
                        )
                        Text(
                            tx("Brave Search uses your API key. It stays on this device."),
                            fontSize = 11.sp,
                            color = Ink.copy(alpha = 0.58f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (braveKey.isNotBlank()) {
                                TextButton(onClick = {
                                    onSaveKey("brave", "")
                                    braveKeyDraft = ""
                                }) { Text(tx("Remove"), maxLines = 1) }
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { settingsExpanded = false }) { Text(tx("Cancel"), maxLines = 1) }
                            Button(
                                onClick = {
                                    val key = braveKeyDraft.trim()
                                    onSaveKey("brave", key)
                                    onProviderChange("brave")
                                    settingsExpanded = false
                                },
                                enabled = braveKeyDraft.isNotBlank(),
                                modifier = Modifier.testTag("search-provider-save")
                            ) { Text(tx("Save key"), maxLines = 1) }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SearchServiceMark("bing", 34.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(tx("Bing"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                                Text(tx("No API key needed"), fontSize = 11.sp, color = Ink.copy(alpha = 0.58f))
                            }
                        }
                        Text(
                            tx("Bing uses public search results and does not require an API key."),
                            fontSize = 12.sp,
                            color = Ink.copy(alpha = 0.62f),
                            lineHeight = 17.sp
                        )
                        Text(
                            tx("Public Bing search may be rate limited."),
                            fontSize = 11.sp,
                            color = Ink.copy(alpha = 0.50f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchServiceMark(provider: String, size: androidx.compose.ui.unit.Dp) {
    val isBing = provider == "bing"
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = if (isBing) Color(0xFF1473E6) else Color(0xFFFB542B),
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                if (isBing) "b" else "br",
                color = Color.White,
                fontSize = if (isBing) 20.sp else 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
