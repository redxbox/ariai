package com.ariai.app.ui.screens

import com.ariai.app.util.tx
import com.ariai.app.util.QRCodeUtil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.ariai.app.ui.theme.*
import com.ariai.app.ui.components.ProviderBrandMark

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ariai.app.data.models.AIModel
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import com.ariai.app.data.models.providerFromJson

private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk
fun providerTypeLabel(type: ProviderType): String = when (type) {
    ProviderType.OPENAI -> "OpenAI"
    ProviderType.GEMINI -> "Google Gemini"
    ProviderType.ANTHROPIC -> "Anthropic"
    ProviderType.OPENAI_COMPATIBLE -> tx("OpenAI compatible")
    ProviderType.OLLAMA -> tx("Ollama (local)")
    ProviderType.CUSTOM -> tx("Custom")
}

/** Each provider has a matching brand tint where a known brand can be identified. */
fun providerBrandColor(type: ProviderType, name: String? = null): Color = when {
    name?.contains("deepseek", ignoreCase = true) == true -> Color(0xFF5786FE)
    name?.contains("openrouter", ignoreCase = true) == true -> Color(0xFF94A3B8)
    else -> when (type) {
        ProviderType.OPENAI -> AriOpenAI
        ProviderType.GEMINI -> AriGemini
        ProviderType.ANTHROPIC -> AriAnthropic
        ProviderType.OLLAMA -> AriOllama
        ProviderType.OPENAI_COMPATIBLE, ProviderType.CUSTOM -> AriCustom
    }
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
    onSaveProvider: ((Provider, (Provider?) -> Unit) -> Unit)? = null,
    onAddProviderPreset: ((ProviderType, String) -> Unit)? = null
) {
    var query by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var showAddProvider by remember { mutableStateOf(false) }
    var addProviderPreset by remember { mutableStateOf<ProviderPreset?>(null) }
    val fetchModelsForAdd = fetchModels
    val saveProviderForAdd = onSaveProvider
    fun openAddProvider(preset: ProviderPreset? = null) {
        if (fetchModelsForAdd != null && saveProviderForAdd != null) {
            addProviderPreset = preset
            showAddProvider = true
        } else if (preset != null) {
            onAddProviderPreset?.invoke(preset.type, preset.labelKey) ?: onAddPreset(preset.type)
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
            providerPresetDescription(preset).contains(query, ignoreCase = true) ||
            providerPresetSearchTerms(preset).contains(query, ignoreCase = true)
    }
    var displayedPresets by remember { mutableStateOf(visiblePresets) }
    LaunchedEffect(visiblePresets) {
        if (visiblePresets.isNotEmpty()) displayedPresets = visiblePresets
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

            item { ProviderSummaryStrip(providers) }

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

            item(key = "provider-quick-connect") {
                AnimatedVisibility(
                    visible = visiblePresets.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                        expandVertically(tween(240, easing = FastOutSlowInEasing), expandFrom = Alignment.Top),
                    exit = fadeOut(tween(120)) +
                        shrinkVertically(tween(180, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().animateContentSize(tween(220, easing = FastOutSlowInEasing)),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(tx("Quick connect"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            Text(tx("Connect to popular AI providers in seconds"), fontSize = 12.sp, color = AriMuted)
                        }
                        ProviderCatalogGrid(
                            presets = displayedPresets,
                            onSelect = { openAddProvider(it) }
                        )
                    }
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
                    presetType = addProviderPreset?.type,
                    presetLabelKey = addProviderPreset?.labelKey,
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

private fun providerPresetDescription(preset: ProviderPreset): String = when (preset.labelKey) {
    "OpenAI" -> tx("GPT models")
    "Gemini" -> tx("Google models")
    "Anthropic" -> tx("Claude models")
    "Ollama" -> tx("Local models")
    "DeepSeek" -> tx("DeepSeek models")
    "OpenRouter" -> tx("Multiple providers")
    else -> tx("Connect to any OpenAI-compatible API")
}

private fun providerPresetSearchTerms(preset: ProviderPreset): String = when (preset.labelKey) {
    "OpenAI" -> tx("GPT-4o and o-series models")
    "Gemini" -> tx("Gemini models, with Search and URL tools")
    "Anthropic" -> tx("Claude models")
    "Ollama" -> tx("Models that run on your own computer")
    "DeepSeek" -> tx("DeepSeek chat and reasoning models")
    "OpenRouter" -> tx("One API for models from many providers")
    else -> tx("Any OpenAI-compatible API")
}

@Composable
private fun ProviderSummaryStrip(providers: List<Provider>) {
    val activeCount = providers.count { it.enabled }
    val modelCount = providers.sumOf { it.models.size }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("${providers.size} ${tx("connections")}", fontSize = 12.sp, color = AriMuted)
        Text("·", fontSize = 13.sp, color = AriLine)
        Text("$activeCount ${tx("active")}", fontSize = 12.sp, color = AriMuted)
        Text("·", fontSize = 13.sp, color = AriLine)
        Text("$modelCount ${tx("models")}", fontSize = 12.sp, color = AriMuted)
    }
}

@Composable
private fun ProviderCatalogGrid(
    presets: List<ProviderPreset>,
    onSelect: (ProviderPreset) -> Unit
) {
    val popularPresets = presets.filterNot { it.labelKey == "Custom" }
    val customPreset = presets.firstOrNull { it.labelKey == "Custom" }
    Column(
        modifier = Modifier.fillMaxWidth().animateContentSize(tween(220, easing = FastOutSlowInEasing)),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        popularPresets.chunked(2).forEachIndexed { rowIndex, rowPresets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowPresets.forEachIndexed { columnIndex, preset ->
                    AnimatedVisibility(
                        visible = true,
                        modifier = Modifier.weight(1f),
                        enter = fadeIn(tween(180, delayMillis = rowIndex * 45 + columnIndex * 28)) +
                            scaleIn(
                                initialScale = 0.96f,
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            ),
                        exit = fadeOut(tween(120))
                    ) {
                        ProviderCatalogTile(preset = preset, onClick = { onSelect(preset) })
                    }
                }
                if (rowPresets.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        customPreset?.let { preset ->
            AnimatedVisibility(
                visible = true,
                modifier = Modifier.fillMaxWidth(),
                enter = fadeIn(tween(180, delayMillis = popularPresets.chunked(2).size * 45 + 60)) +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(220, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(120))
            ) {
                ProviderCatalogTile(preset = preset, onClick = { onSelect(preset) })
            }
        }
    }
}

@Composable
private fun ProviderCatalogTile(
    preset: ProviderPreset,
    onClick: () -> Unit
) {
    val isCustom = preset.labelKey == "Custom"
    val customBorderColor = AriLine.copy(alpha = 0.9f)
    val tileModifier = Modifier.fillMaxWidth()
        .height(if (isCustom) 66.dp else 64.dp)
        .animateContentSize(tween(180, easing = FastOutSlowInEasing))
        .then(
            if (isCustom) Modifier.drawWithContent {
                drawContent()
                drawRoundRect(
                    color = customBorderColor,
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                    )
                )
            } else Modifier
        )
        .testTag("provider-catalog-${preset.labelKey.lowercase()}")
    Surface(
        onClick = onClick,
        modifier = tileModifier,
        shape = RoundedCornerShape(18.dp),
        color = AriCard,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = if (isCustom) null else BorderStroke(0.8.dp, AriLine.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = if (isCustom) 13.dp else 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape)
                    .background(if (isCustom) AriAccent.copy(alpha = 0.10f) else preset.color.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                if (isCustom) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = AriAccent, modifier = Modifier.size(25.dp))
                } else {
                    ProviderBrandMark(type = preset.type, name = preset.label, size = 34.dp)
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    preset.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    providerPresetDescription(preset),
                    fontSize = 10.sp,
                    color = AriMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AriMuted,
                modifier = Modifier.size(19.dp)
            )
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

/** A compact connection row with status, model count, and an inline enable switch. */
@Composable
private fun ProviderCard(provider: Provider, onClick: () -> Unit, onToggle: () -> Unit) {
    val host = remember(provider.baseUrl) {
        provider.baseUrl.trim().removePrefix("https://").removePrefix("http://").substringBefore('/')
    }
    val statusBackground = if (provider.enabled) AriAccent.copy(alpha = 0.12f) else AriTint
    val statusForeground = if (provider.enabled) AriAccent else AriMuted
    val shape = RoundedCornerShape(17.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(tween(180, easing = FastOutSlowInEasing))
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag("provider-card-${provider.id}"),
        shape = shape,
        color = AriCard,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(0.75.dp, AriLine.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(providerBrandColor(provider.type, provider.name).copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                ProviderBrandMark(provider = provider, size = 30.dp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        provider.name,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    MiniChip(
                        text = if (provider.enabled) tx("Active") else tx("Turned off"),
                        bg = statusBackground,
                        fg = statusForeground
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (host.isBlank()) providerTypeLabel(provider.type) else host,
                        modifier = Modifier.weight(1f),
                        fontSize = 11.sp,
                        color = AriMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (provider.models.isEmpty()) tx("No models") else "${provider.models.size} ${tx("models")}",
                        fontSize = 10.sp,
                        color = AriMuted,
                        maxLines = 1
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
    var scannedProvider by remember { mutableStateOf<Provider?>(null) }
    var isReadingQr by remember { mutableStateOf(false) }
    var qrMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pastedProvider = remember(text) { if (text.isBlank()) null else providerFromJson(text) }
    val parsed = scannedProvider ?: pastedProvider
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            isReadingQr = true
            qrMessage = null
            scope.launch {
                val qrText = withContext(Dispatchers.IO) {
                    runCatching { decodeQrFromImage(context, uri) }.getOrNull()
                }
                val provider = qrText?.let(::providerFromJson)
                isReadingQr = false
                if (provider != null) {
                    scannedProvider = provider
                    text = ""
                    qrMessage = tx("Provider QR ready to import")
                } else {
                    qrMessage = if (qrText == null) {
                        tx("No QR code found in this image.")
                    } else {
                        tx("This QR code does not contain valid provider data.")
                    }
                }
            }
        }
    }
    val dialogShape = RoundedCornerShape(28.dp)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = dialogShape,
        containerColor = AriCard,
        tonalElevation = 8.dp,
        title = { Text(tx("Import provider"), fontWeight = FontWeight.SemiBold, color = Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    tx("Paste a provider as JSON with name, type, baseUrl and apiKey."),
                    fontSize = 13.sp,
                    color = Ink.copy(alpha = 0.6f)
                )
                OutlinedButton(
                    onClick = {
                        scannedProvider = null
                        qrMessage = null
                        galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth().testTag("provider-import-gallery"),
                    enabled = !isReadingQr,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AriAccent),
                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 9.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(21.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(tx("Scan provider QR from gallery"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(tx("Choose an image with a provider QR code."), fontSize = 10.sp, color = AriMuted, maxLines = 1)
                        }
                        if (isReadingQr) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AriAccent)
                        } else {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(19.dp))
                        }
                    }
                }
                if (qrMessage != null) {
                    Text(
                        qrMessage.orEmpty(),
                        fontSize = 11.sp,
                        color = if (scannedProvider != null) AriAccent else AriError
                    )
                }
                if (scannedProvider != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(providerBrandColor(scannedProvider!!.type, scannedProvider!!.name).copy(alpha = 0.10f))
                            .padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        ProviderBrandMark(provider = scannedProvider!!, size = 29.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(scannedProvider!!.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1)
                            Text(providerTypeLabel(scannedProvider!!.type), fontSize = 10.sp, color = AriMuted, maxLines = 1)
                        }
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AriAccent, modifier = Modifier.size(18.dp))
                    }
                }
                Text(tx("Or paste provider JSON"), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AriMuted)
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        scannedProvider = null
                        qrMessage = null
                    },
                    label = { Text(tx("Provider JSON")) },
                    placeholder = {
                        Text(
                            "{\n  \"name\": \"My provider\",\n  \"type\": \"OPENAI_COMPATIBLE\",\n  \"baseUrl\": \"https://api.example.com/v1\",\n  \"apiKey\": \"YOUR_API_KEY\"\n}",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = AriMuted.copy(alpha = 0.78f)
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    isError = text.isNotBlank() && pastedProvider == null,
                    supportingText = { if (text.isNotBlank() && pastedProvider == null) Text(tx("Not a valid provider JSON")) },
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

private fun decodeQrFromImage(context: Context, uri: Uri): String? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsStream = resolver.openInputStream(uri) ?: return null
    boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
    val maxDimension = maxOf(bounds.outWidth, bounds.outHeight)
    if (maxDimension <= 0) return null

    var sampleSize = 1
    while (maxDimension / sampleSize > 2048) sampleSize *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.RGB_565
    }
    val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
    return try {
        QRCodeUtil.decodeQRCode(bitmap)
    } finally {
        bitmap.recycle()
    }
}
