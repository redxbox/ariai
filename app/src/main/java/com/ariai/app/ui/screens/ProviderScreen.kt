package com.ariai.app.ui.screens

import com.ariai.app.util.tx

import com.ariai.app.ui.theme.*

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ariai.app.data.models.*
import com.ariai.app.data.remote.ConnectionResult
import com.ariai.app.data.remote.checkConnection
import com.ariai.app.ui.components.ProviderBrandMark
import com.ariai.app.util.LocalStrings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderListScreen(
    providers: List<Provider>,
    onAddProvider: () -> Unit,
    onEditProvider: (Provider) -> Unit,
    onDeleteProvider: (Provider) -> Unit,
    onTestProvider: (Provider) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(tx("AI Providers"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(tx("Connect. Mix. Create."), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onAddProvider) {
                        Icon(Icons.Default.Add, contentDescription = tx("Add"))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProvider,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(strings.addProvider, fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filter chips simple
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(tx("All"), tx("Cloud"), tx("Local"), tx("Custom")).forEachIndexed { index, label ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (index == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.clickable { }
                        ) {
                            Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (providers.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(80.dp).clip(CircleShape).background(
                                    Brush.linearGradient(colors = listOf(AriInk.copy(alpha = 0.3f), AriInk.copy(alpha = 0.3f)))
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔌", style = MaterialTheme.typography.displaySmall)
                            }
                            Text(tx("No providers yet"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(tx("Add your first AI provider to start chatting"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            items(count = providers.size, key = { index -> "provider:${providers[index].id}" }) { index ->
                val provider = providers[index]
                GlassProviderCard(
                    provider = provider,
                    onEdit = { onEditProvider(provider) },
                    onDelete = { onDeleteProvider(provider) },
                    onTest = { onTestProvider(provider) }
                )
            }

            item {
                Text(tx("Popular Providers"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PopularProviderRow(name = "OpenAI", subtitle = "GPT-4o, 4o-mini, o3", letter = "O", colors = listOf(AriOpenAI, AriOpenAI.copy(alpha = 0.55f)), onClick = onAddProvider)
                    PopularProviderRow(name = "Anthropic", subtitle = "Claude 3.7, 3.5", letter = "C", colors = listOf(AriAnthropic, AriAnthropic.copy(alpha = 0.55f)), onClick = onAddProvider)
                    PopularProviderRow(name = "Google", subtitle = "Gemini 2.0, 1.5", letter = "G", colors = listOf(AriGemini, AriGemini.copy(alpha = 0.55f)), onClick = onAddProvider)
                    PopularProviderRow(name = "Meta", subtitle = "Llama 3.3, 3.1", letter = "M", colors = listOf(AriMuted, AriMuted), onClick = onAddProvider)
                    PopularProviderRow(name = "DeepSeek", subtitle = "R1, V3", letter = "D", colors = listOf(AriMuted, AriMuted), onClick = onAddProvider)
                    PopularProviderRow(name = "Qwen", subtitle = "Qwen3, Qwen2.5", letter = "Q", colors = listOf(AriCustom, AriCustom.copy(alpha = 0.55f)), onClick = onAddProvider)
                    PopularProviderRow(name = "Mistral", subtitle = tx("Large, Medium, Small"), letter = "Mi", colors = listOf(AriAccent, AriAccent.copy(alpha = 0.55f)), onClick = onAddProvider)
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().clickable { onAddProvider() }
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(tx("Add Custom Provider"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun GlassProviderCard(
    provider: Provider,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(provider.enabled) }

    val providerColor = when (provider.type) {
        ProviderType.OPENAI -> listOf(AriOpenAI, AriOpenAI.copy(alpha = 0.55f))
        ProviderType.GEMINI -> listOf(AriGemini, AriGemini.copy(alpha = 0.55f))
        ProviderType.ANTHROPIC -> listOf(AriAnthropic, AriAnthropic.copy(alpha = 0.55f))
        ProviderType.OLLAMA -> listOf(Color(0xFF000000), Color(0xFF434343))
        else -> listOf(AriInk, AriInk)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(colors = providerColor)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (provider.type) {
                        ProviderType.OPENAI -> "O"
                        ProviderType.GEMINI -> "G"
                        ProviderType.ANTHROPIC -> "C"
                        ProviderType.OLLAMA -> "L"
                        else -> provider.name.firstOrNull()?.toString() ?: "A"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = AriCard,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(provider.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    if (provider.models.isNotEmpty()) provider.models.take(2).joinToString(", ") { it.displayName } else provider.baseUrl.take(30),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { enabled = it },
                modifier = Modifier.semantics { contentDescription = "${provider.name} ${tx("Enable")}" }
            )
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = tx("Provider options"), modifier = Modifier.size(18.dp))
                }
                val menuShape = paperPopupShape(16.dp, 12.dp)
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(AriCard, menuShape).then(paperPopupModifier(menuShape, 12.dp))
                ) {
                    DropdownMenuItem(text = { Text(tx("Test")) }, onClick = { showMenu = false; onTest() }, leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) })
                    DropdownMenuItem(text = { Text(tx("Edit")) }, onClick = { showMenu = false; onEdit() }, leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) })
                    DropdownMenuItem(text = { Text(tx("Delete")) }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) })
                }
            }
        }
    }
}

@Composable
fun PopularProviderRow(
    name: String,
    subtitle: String,
    letter: String,
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(colors = colors)),
                contentAlignment = Alignment.Center
            ) {
                Text(letter, color = AriCard, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = false, onCheckedChange = { onClick() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal data class ProviderPreset(val labelKey: String, val type: ProviderType, val baseUrl: String, val color: Color) {
    val label: String get() = tx(labelKey)
}

internal val providerPresets = listOf(
    ProviderPreset("OpenAI", ProviderType.OPENAI, "https://api.openai.com/v1", AriOpenAI),
    ProviderPreset("Gemini", ProviderType.GEMINI, "https://generativelanguage.googleapis.com/v1beta", AriGemini),
    ProviderPreset("Anthropic", ProviderType.ANTHROPIC, "https://api.anthropic.com/v1", AriAnthropic),
    ProviderPreset("Ollama", ProviderType.OLLAMA, "http://localhost:11434/v1", AriOllama),
    ProviderPreset("DeepSeek", ProviderType.OPENAI_COMPATIBLE, "https://api.deepseek.com", Color(0xFF5786FE)),
    ProviderPreset("OpenRouter", ProviderType.OPENAI_COMPATIBLE, "https://openrouter.ai/api/v1", Color(0xFF94A3B8)),
    ProviderPreset("Custom", ProviderType.OPENAI_COMPATIBLE, "", AriCustom)
)

private fun defaultProviderPresetForType(type: ProviderType): ProviderPreset? = when (type) {
    ProviderType.OPENAI_COMPATIBLE, ProviderType.CUSTOM -> providerPresets.firstOrNull { it.labelKey == "Custom" }
    else -> providerPresets.firstOrNull { it.type == type }
}

private fun providerPresetFor(provider: Provider): ProviderPreset? =
    providerPresets.firstOrNull { it.labelKey.equals(provider.name.trim(), ignoreCase = true) }
        ?: defaultProviderPresetForType(provider.type)

/**
 * Add or edit an AI provider. Brand presets fill in the endpoint and type,
 * models are created from the provider defaults when the provider is first saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProviderScreen(
    initialProvider: Provider? = null,
    presetType: ProviderType? = null,
    presetLabelKey: String? = null,
    fetchModels: suspend (Provider) -> List<AIModel>,
    onSave: (Provider) -> Unit,
    onBack: () -> Unit,
    onCheckProvider: suspend (Provider) -> ConnectionResult = { provider -> checkConnection(provider, fetchModels) },
    onDelete: (String) -> Unit = {},
    existingProviderNames: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    val isEdit = initialProvider != null
    val initialPreset = initialProvider?.let(::providerPresetFor)
        ?: presetLabelKey?.let { key -> providerPresets.firstOrNull { it.labelKey == key } }
        ?: presetType?.let(::defaultProviderPresetForType)
    fun defaultNameFor(preset: ProviderPreset?): String {
        if (preset == null) return ""
        return if (preset.labelKey == "Custom") {
            nextUniqueProviderName(preset.label, existingProviderNames)
        } else {
            preset.label
        }
    }
    var name by remember(initialProvider?.id, presetLabelKey, presetType, existingProviderNames) {
        mutableStateOf(initialProvider?.name ?: defaultNameFor(initialPreset))
    }
    var baseUrl by remember(initialProvider?.id, presetLabelKey, presetType) {
        mutableStateOf(initialProvider?.baseUrl ?: initialPreset?.baseUrl ?: "https://api.openai.com/v1")
    }
    var apiKey by remember { mutableStateOf(initialProvider?.apiKey ?: "") }
    var selectedType by remember(initialProvider?.id, presetLabelKey, presetType) {
        mutableStateOf(initialProvider?.type ?: initialPreset?.type ?: presetType ?: ProviderType.OPENAI)
    }
    var reveal by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }
    var advancedOpen by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(initialProvider?.enabled ?: true) }
    var confirmDelete by remember { mutableStateOf(false) }
    val providerId = remember { initialProvider?.id ?: java.util.UUID.randomUUID().toString() }
    var models by remember { mutableStateOf(initialProvider?.models ?: emptyList()) }
    var modelsEdited by remember { mutableStateOf(initialProvider != null) }
    var showAddModel by remember { mutableStateOf(false) }
    var modelFilter by remember { mutableStateOf("") }
    var modelsExpanded by remember { mutableStateOf(false) }
    var headers by remember { mutableStateOf(initialProvider?.customHeaders?.map { it.key to it.value }.orEmpty()) }
    var customBody by remember { mutableStateOf(initialProvider?.customBody.orEmpty()) }
    val scope = rememberCoroutineScope()
    var fetching by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var showConnectionDialog by remember { mutableStateOf(false) }
    var checkingConnection by remember { mutableStateOf(false) }
    var connectionResult by remember { mutableStateOf<ConnectionResult?>(null) }
    // Key of the URL/key/type the models were last fetched for. Save fetches again when it changes.
    fun sourceKey() = "${baseUrl.trim()}|${apiKey.trim()}|$selectedType"
    var fetchedKey by remember {
        mutableStateOf(initialProvider?.let { "${it.baseUrl.trim()}|${it.apiKey.trim()}|${it.type}" })
    }
    fun draftProvider(list: List<AIModel>) = Provider(
        id = providerId,
        name = name.trim(),
        type = selectedType,
        baseUrl = baseUrl.trim(),
        apiKey = apiKey.trim(),
        models = list.map { it.copy(providerId = providerId) },
        enabled = enabled,
        customHeaders = headers.filter { it.first.isNotBlank() }.toMap(),
        customBody = customBody.trim().ifBlank { null },
        createdAt = initialProvider?.createdAt ?: System.currentTimeMillis(),
        apiKeyNeedsReentry = selectedType != ProviderType.OLLAMA && initialProvider?.apiKeyNeedsReentry == true && apiKey.isBlank()
    )
    fun runConnectionCheck() {
        if (checkingConnection || fetching) return
        val requestProvider = draftProvider(models)
        showConnectionDialog = true
        checkingConnection = true
        connectionResult = null
        scope.launch {
            val result = try {
                onCheckProvider(requestProvider)
            } catch (cancelled: CancellationException) {
                checkingConnection = false
                throw cancelled
            } catch (_: Exception) {
                ConnectionResult.Failed(
                    problem = tx("Could not connect"),
                    hint = tx("Check your connection, or the Base URL.")
                )
            }
            connectionResult = result
            checkingConnection = false
        }
    }
    fun fetchAndMerge(
        onDone: (List<AIModel>) -> Unit,
        showModelsWhenFetched: Boolean = false
    ) {
        val requestedSourceKey = sourceKey()
        val requestProvider = draftProvider(models)
        fetching = true
        fetchError = null
        scope.launch {
            val outcome = try {
                refreshProviderModels(
                    fetch = { fetchModels(requestProvider) },
                    sourceIsCurrent = { sourceKey() == requestedSourceKey }
                ) { fetched ->
                    // When the endpoint/key/type changed, only retain explicitly custom models from the old source.
                    val mergeBase = if (requestedSourceKey == fetchedKey) models else models.filter { it.isCustom }
                    val merged = mergeFetched(mergeBase, fetched)
                    models = merged
                    fetchedKey = requestedSourceKey
                    if (showModelsWhenFetched) {
                        modelFilter = ""
                        modelsExpanded = false
                        tab = 1
                    }
                    onDone(merged)
                }
            } catch (cancelled: CancellationException) {
                fetching = false
                throw cancelled
            }
            fetching = false
            when (outcome) {
                ProviderRefreshOutcome.Updated -> Unit
                ProviderRefreshOutcome.SourceChanged -> {
                    fetchError = tx("Connection changed while fetching. Fetch again.")
                }
                is ProviderRefreshOutcome.Failed -> {
                    val error = outcome.error
                    fetchError = "${tx("Could not load models")}: ${error.message ?: error::class.java.simpleName}"
                }
            }
        }
    }
    val keyNeedsReentry = selectedType != ProviderType.OLLAMA && initialProvider?.apiKeyNeedsReentry == true && apiKey.isBlank()
    val canSave = name.isNotBlank() && baseUrl.isNotBlank() && isValidJsonObject(customBody) && !keyNeedsReentry
    val visibleModels = models.filter {
        modelFilter.isBlank() || it.id.contains(modelFilter, ignoreCase = true) || it.displayName.contains(modelFilter, ignoreCase = true)
    }
    fun saveProviderDraft() {
        if (sourceKey() != fetchedKey) {
            // URL, key or type changed: refresh the models before saving.
            fetchAndMerge(onDone = { list -> onSave(draftProvider(list)) })
        } else {
            onSave(draftProvider(models))
        }
    }
    val modelListState = rememberLazyListState()
    LaunchedEffect(tab) { modelListState.scrollToItem(0) }
    if (showConnectionDialog) {
        ProviderConnectionDialog(
            providerName = name.trim().ifBlank { tx("New provider") },
            checking = checkingConnection,
            result = connectionResult,
            onRetry = { runConnectionCheck() },
            onDismiss = { showConnectionDialog = false }
        )
    }
    if (showAddModel) {
        AddModelSheet(
            providerId = providerId,
            providerType = selectedType,
            onAdd = { model ->
                // Re-adding a discovered model lets users correct capability flags when an API omits them.
                models = models.filterNot { it.id == model.id } + model
                modelsEdited = true
                showAddModel = false
            },
            onDismiss = { showAddModel = false }
        )
    }

    if (confirmDelete && initialProvider != null) {
        val dialogShape = paperPopupShape(26.dp)
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            modifier = paperPopupModifier(dialogShape),
            shape = dialogShape,
            containerColor = AriCard,
            title = { Text(tx("Delete provider?")) },
            text = { Text("${initialProvider.name} and its models will be removed.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete(initialProvider.id) }) { Text(tx("Delete"), color = AriAccent) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tx("Cancel")) } }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().background(
            Brush.verticalGradient(
                colors = listOf(AriPaper, AriAccentSoft.copy(alpha = 0.18f), AriPaper)
            )
        ),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tx("Back"), tint = AriInk) } },
                title = { Text(if (isEdit) tx("Edit provider") else tx("Add provider"), fontWeight = FontWeight.SemiBold, color = AriInk) },
                actions = {
                    if (isEdit) {
                        IconButton(onClick = { confirmDelete = true }, modifier = Modifier.testTag("provider-delete")) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = tx("Delete provider"), tint = AriMuted)
                        }
                    }
                }
            )
        },
        bottomBar = {
            ProviderActionBar(
                fetching = fetching,
                checkingConnection = checkingConnection,
                canSave = canSave && !fetching,
                onSave = { saveProviderDraft() },
                onCheckConnection = { if (!checkingConnection && !fetching) runConnectionCheck() }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ProviderEditHeader(
                    name = name,
                    type = selectedType,
                    baseUrl = baseUrl,
                    enabled = enabled,
                    onToggle = { enabled = it }
                )
                SegmentedTabs(
                    labels = listOf(tx("Connection"), tx("Models")),
                    selected = tab,
                    onSelect = { tab = it }
                )
            }
            LazyColumn(
                state = modelListState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (tab == 0) {
            // Connection
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = AriCard,
                    shadowElevation = 0.dp,
                    border = BorderStroke(0.75.dp, AriLine.copy(alpha = 0.8f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(tx("Name")) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = providerFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            label = { Text(tx("Base URL")) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = providerFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text(tx("API key")) },
                            singleLine = true,
                            visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { reveal = !reveal }) {
                                    Icon(Icons.Default.Visibility, contentDescription = if (reveal) tx("Hide key") else tx("Show key"), tint = AriInk.copy(alpha = 0.5f))
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = providerFieldColors(),
                            modifier = Modifier.fillMaxWidth().testTag("provider-api-key")
                        )
                        Text(tx("Stored only on this device."), fontSize = 12.sp, color = AriInk.copy(alpha = 0.5f))
                        if (keyNeedsReentry) {
                            Text(
                                "${tx("The saved API key could not be unlocked.")} ${tx("Re-enter it in AI Providers and save.")}",
                                fontSize = 12.sp,
                                color = AriAccent
                            )
                        }
                        FetchModelsRow(
                            fetching = fetching,
                            canFetch = baseUrl.isNotBlank() && !keyNeedsReentry,
                            status = fetchError ?: if (models.isEmpty()) null else "${models.size} ${tx("models")}",
                            isError = fetchError != null,
                            onFetch = { fetchAndMerge({}, showModelsWhenFetched = true) }
                        )
                    }
                }
            }


            // Advanced: collapsed by default. Headers and body are sent to every model of this provider.
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = AriCard,
                    shadowElevation = 0.dp,
                    border = BorderStroke(0.75.dp, AriLine.copy(alpha = 0.8f)),
                    modifier = Modifier.fillMaxWidth().animateContentSize(tween(220, easing = FastOutSlowInEasing))
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { advancedOpen = !advancedOpen }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tx("Advanced"), fontWeight = FontWeight.SemiBold, color = AriInk, modifier = Modifier.weight(1f))
                            Icon(
                                if (advancedOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = AriInk
                            )
                        }
                        if (advancedOpen) {
                            Column(
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(tx("Custom headers"), fontWeight = FontWeight.Medium, color = AriInk)
                                KeyValueEditor(items = headers, onChange = { headers = it })
                                OutlinedTextField(
                                    value = customBody,
                                    onValueChange = { customBody = it },
                                    label = { Text(tx("Custom body (JSON)")) },
                                    minLines = 2,
                                    maxLines = 6,
                                    isError = !isValidJsonObject(customBody),
                                    supportingText = { if (!isValidJsonObject(customBody)) Text(tx("Must be a JSON object")) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = providerFieldColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            } else {
            // Models: one grouped list. Fetch fills it from the API; rows can be removed.
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = AriCard,
                    shadowElevation = 0.dp,
                    border = BorderStroke(0.75.dp, AriLine.copy(alpha = 0.8f)),
                    modifier = Modifier.fillMaxWidth().animateContentSize(tween(220, easing = FastOutSlowInEasing))
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    if (models.isEmpty()) tx("No models yet") else "${models.size} ${tx("models")}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AriInk
                                )
                                Text(
                                    fetchError ?: when {
                                        models.isEmpty() -> tx("Fetch models to preview available models")
                                        fetchedKey == sourceKey() -> tx("Models are up to date")
                                        else -> tx("Connection changed. Fetch or save to refresh models.")
                                    },
                                    fontSize = 12.sp,
                                    color = if (fetchError != null) AriAccent else AriMuted
                                )
                                Text(
                                    tx("Image mode uses provider output metadata when available. Vision input alone does not mean image generation."),
                                    fontSize = 11.sp,
                                    color = AriMuted
                                )
                            }
                        }
                        if (models.size > 8) {
                            OutlinedTextField(
                                value = modelFilter,
                                onValueChange = { modelFilter = it },
                                placeholder = { Text(tx("Filter models")) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        if (models.isEmpty()) {
                            Text(tx("No models yet"), fontSize = 13.sp, color = AriMuted, modifier = Modifier.padding(16.dp))
                        }
                        // Only a few rows are composed by default; the rest load on expand.
                        val collapsible = visibleModels.size > 5 && modelFilter.isBlank()
                        val shownModels = if (collapsible && !modelsExpanded) visibleModels.take(5) else visibleModels
                        shownModels.forEachIndexed { index, model ->
                            if (index > 0) HorizontalDivider(color = AriTint, modifier = Modifier.padding(horizontal = 16.dp))
                            ModelRow(
                                model = model,
                                color = AriInk,
                                onRemove = { models = models.filterNot { it.id == model.id } }
                            )
                        }
                        if (collapsible) {
                            HorizontalDivider(color = AriTint)
                            if (!modelsExpanded) {
                                Text(
                                    "${shownModels.size} ${tx("shown")} · ${visibleModels.size - shownModels.size} ${tx("more models")}",
                                    fontSize = 12.sp,
                                    color = AriMuted,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            TextButton(onClick = { modelsExpanded = !modelsExpanded }, modifier = Modifier.fillMaxWidth()) {
                                Icon(
                                    if (modelsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (modelsExpanded) tx("Collapse") else tx("Expand"),
                                    tint = AriInk,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (modelsExpanded) tx("Show less") else "${tx("Show all")} ${visibleModels.size}",
                                    color = AriInk,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        HorizontalDivider(color = AriTint)
                        TextButton(onClick = { showAddModel = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = AriInk, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tx("Add model"), color = AriInk, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            }

            }
        }
    }
}

@Composable
private fun ProviderActionBar(
    fetching: Boolean,
    checkingConnection: Boolean,
    canSave: Boolean,
    onSave: () -> Unit,
    onCheckConnection: () -> Unit
) {
    val saveEnabled = canSave && !fetching
    val testEnabled = !checkingConnection && !fetching
    val saveStart by animateColorAsState(
        targetValue = if (saveEnabled) Color(0xFF648BFF) else Color(0xFFB3C0DC),
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "provider-save-start"
    )
    val saveEnd by animateColorAsState(
        targetValue = if (saveEnabled) Color(0xFF324472) else Color(0xFF899BC0),
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "provider-save-end"
    )
    val saveBrush = remember(saveStart, saveEnd) { Brush.linearGradient(listOf(saveStart, saveEnd)) }
    val testStart by animateColorAsState(
        targetValue = if (testEnabled) Color(0xFFFCFDFF) else AriCard,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "provider-test-start"
    )
    val testEnd by animateColorAsState(
        targetValue = if (testEnabled) Color(0xFFDDE7FF) else AriTint,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "provider-test-end"
    )
    val testBrush = remember(testStart, testEnd) { Brush.linearGradient(listOf(testStart, testEnd)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("provider-save-action-bar")
                .animateContentSize(tween(180, easing = FastOutSlowInEasing)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = onSave,
                enabled = saveEnabled,
                modifier = Modifier.weight(1f).height(52.dp).testTag("provider-save"),
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                contentColor = Color.White,
                border = BorderStroke(1.dp, if (saveEnabled) Color.White.copy(alpha = 0.28f) else AriLine.copy(alpha = 0.65f)),
                shadowElevation = if (saveEnabled) 7.dp else 1.dp,
                tonalElevation = 0.dp
            ) {
                Box(Modifier.fillMaxSize().background(saveBrush)) {
                    Spacer(
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color.White.copy(alpha = 0.5f), Color.Transparent)
                                )
                            )
                    )
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(30.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (fetching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(17.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(17.dp))
                            }
                        }
                        if (!fetching) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                tx("Save"),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Surface(
                onClick = onCheckConnection,
                enabled = testEnabled,
                modifier = Modifier.weight(1f).height(52.dp).testTag("provider-test-connection"),
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                contentColor = AriAccent,
                border = BorderStroke(1.dp, AriAccent.copy(alpha = if (testEnabled) 0.22f else 0.12f)),
                shadowElevation = if (testEnabled) 4.dp else 0.dp,
                tonalElevation = 0.dp
            ) {
                Box(Modifier.fillMaxSize().background(testBrush)) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(30.dp).clip(CircleShape)
                                .background(AriAccent.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (checkingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(17.dp),
                                    strokeWidth = 2.dp,
                                    color = AriAccent
                                )
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(17.dp))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (checkingConnection) tx("Checking…") else tx("Test"),
                            color = AriAccent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderConnectionDialog(
    providerName: String,
    checking: Boolean,
    result: ConnectionResult?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val dialogShape = paperPopupShape(29.dp)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 420.dp)
                .then(paperPopupModifier(dialogShape))
                .testTag("provider-connection-dialog"),
            shape = dialogShape,
            color = AriTint,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(AriAccentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = AriAccent, modifier = Modifier.size(23.dp))
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            tx("Check connection"),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = AriInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(providerName, fontSize = 12.sp, color = AriMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                // One fixed-height status slot and a single crossfade prevent the dialog from
                // changing height twice (the old visibility + size animations caused clipping/jitter).
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp, max = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = checking to result,
                        animationSpec = tween(170, easing = FastOutSlowInEasing),
                        label = "provider-connection-status"
                    ) { (isChecking, current) ->
                        when {
                            isChecking -> Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = AriAccentSoft.copy(alpha = 0.58f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                                ) {
                                    Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = AriAccent, modifier = Modifier.size(22.dp))
                                    Text(tx("Checking…"), color = AriMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            current is ConnectionResult.Ok -> Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = AriAccentGreenSoft,
                                modifier = Modifier.fillMaxWidth().testTag("provider-connection-result")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AriAccentGreen, modifier = Modifier.size(23.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            "${tx("Connected")} · ${current.models} ${tx("models")}",
                                            color = AriAccentGreen,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text("${current.durationMs} ms", color = AriMuted, fontSize = 12.sp)
                                    }
                                }
                            }
                            current is ConnectionResult.Failed -> Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = AriError.copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth().testTag("provider-connection-result")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        current.problem + (current.code?.let { " (HTTP $it)" } ?: ""),
                                        color = AriError,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(current.hint, color = AriMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            else -> Spacer(Modifier.height(1.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (result is ConnectionResult.Failed && !checking) {
                        TextButton(onClick = onRetry, modifier = Modifier.testTag("provider-connection-retry")) {
                            Text(tx("Retry"), color = AriAccent)
                        }
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("provider-connection-close")) {
                        Text(tx("Close"), color = AriAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelRow(model: AIModel, color: Color, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(model.displayName.firstOrNull()?.uppercase() ?: "M", color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(model.displayName, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = AriInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(model.id, fontSize = 11.sp, color = AriMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val tags = buildList {
                if (model.supportsVision) add(Triple(tx("Vision"), AriAccentSoft, AriAccent))
                if (model.supportsFunctionCalling) add(Triple(tx("Tools"), AriAccentSoft, AriAccent))
                if (model.supportsImageGen) add(Triple(tx("Image"), AriTagAmberBg, AriTagAmberFg))
                if ("google_search" in model.builtInTools) add(Triple(tx("Search"), AriTagAmberBg, AriTagAmberFg))
                if ("url_context" in model.builtInTools) add(Triple("URL", AriTagAmberBg, AriTagAmberFg))
            }
            if (tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                    tags.forEach { (label, bg, fg) -> ModelTag(label, bg, fg) }
                }
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Close, contentDescription = tx("Remove model"), tint = AriMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ModelTag(label: String, bg: Color, fg: Color) {
    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(label, fontSize = 10.sp, color = fg, fontWeight = FontWeight.Medium)
    }
}

/** Key/value rows for custom headers. */
@Composable
private fun KeyValueEditor(items: List<Pair<String, String>>, onChange: (List<Pair<String, String>>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { index, pair ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pair.first,
                    onValueChange = { key -> onChange(items.toMutableList().also { it[index] = key to pair.second }) },
                    label = { Text(tx("Header")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = pair.second,
                    onValueChange = { value -> onChange(items.toMutableList().also { it[index] = pair.first to value }) },
                    label = { Text(tx("Value")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onChange(items.toMutableList().also { it.removeAt(index) }) }) {
                    Icon(Icons.Default.Close, contentDescription = tx("Remove header"), tint = AriMuted)
                }
            }
        }
        OutlinedButton(
            onClick = { onChange(items + ("" to "")) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(tx("Add header"))
        }
    }
}

/** Replaces fetched models while retaining per-model settings and explicit custom capabilities. */
internal fun mergeFetched(current: List<AIModel>, fetched: List<AIModel>): List<AIModel> {
    val byId = current.associateBy { it.id }
    val fromApi = fetched.map { fresh ->
        val old = byId[fresh.id] ?: return@map fresh
        if (old.isCustom) {
            // A manually added model may intentionally override incomplete API modality metadata.
            fresh.copy(
                displayName = old.displayName,
                supportsVision = old.supportsVision || fresh.supportsVision,
                supportsFunctionCalling = old.supportsFunctionCalling || fresh.supportsFunctionCalling,
                supportsImageGen = old.supportsImageGen || fresh.supportsImageGen,
                isCustom = true,
                builtInTools = old.builtInTools,
                headers = old.headers,
                customBody = old.customBody,
                outputsText = old.outputsText && fresh.outputsText
            )
        } else {
            // Capability flags come from the fresh fetch; only request customization is retained.
            fresh.copy(builtInTools = old.builtInTools, headers = old.headers, customBody = old.customBody)
        }
    }
    val customOnly = current.filter { it.isCustom && fetched.none { fresh -> fresh.id == it.id } }
    return fromApi + customOnly
}

private fun isValidJsonObject(text: String): Boolean =
    text.isBlank() || runCatching { org.json.JSONObject(text) }.isSuccess

@Composable
private fun ToolToggleRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = AriTint, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium, color = if (enabled) AriInk else AriMuted)
                Text(subtitle, fontSize = 12.sp, color = AriMuted)
            }
            Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
        }
    }
}

/** Model form with the reference's three tabs: Built-in Tools, Advanced, Basic. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddModelSheet(
    providerId: String,
    providerType: ProviderType,
    onAdd: (AIModel) -> Unit,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    var modelId by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isImage by remember { mutableStateOf(false) }
    var vision by remember { mutableStateOf(false) }
    var tools by remember { mutableStateOf(true) }
    var search by remember { mutableStateOf(false) }
    var urlContext by remember { mutableStateOf(false) }
    var headers by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var customBody by remember { mutableStateOf("") }
    val bodyValid = isValidJsonObject(customBody)
    val isGemini = providerType == ProviderType.GEMINI

    fun submitModel() {
        val id = modelId.trim()
        onAdd(
            AIModel(
                id = id,
                displayName = displayName.trim().ifBlank { id },
                providerId = providerId,
                supportsVision = vision,
                supportsFunctionCalling = tools,
                supportsImageGen = isImage,
                contextWindow = 8192,
                isCustom = true,
                outputsText = !isImage,
                builtInTools = listOfNotNull(
                    "google_search".takeIf { search && isGemini },
                    "url_context".takeIf { urlContext && isGemini }
                ),
                headers = headers.filter { it.first.isNotBlank() }.toMap(),
                customBody = customBody.trim().ifBlank { null }
            )
        )
    }

    val sheetShape = paperSheetShape(28.dp)
    PaperBottomSheet(
        onDismissRequest = onDismiss,
        shape = sheetShape,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .heightIn(max = 760.dp)
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    tx("Add model"),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = AriInk,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { submitModel() },
                    enabled = modelId.isNotBlank() && bodyValid,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AriAccent, contentColor = Color.White),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(tx("Add"), fontWeight = FontWeight.SemiBold) }
            }
            // Scrollable body: the add action stays reachable in the sheet header.
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SegmentedTabs(
                    labels = listOf(tx("Basic"), tx("Advanced"), tx("Built-in Tools")),
                    selected = tab,
                    onSelect = { tab = it }
                )

                when (tab) {
                    2 -> {
                        Text(
                            if (isGemini) tx("Tools the API runs for this model.") else tx("Built-in tools need the official Gemini API. Turn on a Gemini provider to use them."),
                            fontSize = 12.sp, color = AriMuted
                        )
                        ToolToggleRow(tx("Search"), tx("Google Search grounding"), search, enabled = isGemini) { search = it }
                        ToolToggleRow(tx("URL context"), tx("Read the links in the message"), urlContext, enabled = isGemini) { urlContext = it }
                    }
                    1 -> {
                        Text(tx("Extra headers for this model only."), fontSize = 12.sp, color = AriMuted)
                        KeyValueEditor(items = headers, onChange = { headers = it })
                        OutlinedTextField(
                            value = customBody,
                            onValueChange = { customBody = it },
                            label = { Text(tx("Custom body (JSON)")) },
                            placeholder = { Text("{\"temperature\": 0.2}") },
                            minLines = 3,
                            maxLines = 6,
                            isError = !bodyValid,
                            supportingText = { if (!bodyValid) Text(tx("Must be a JSON object")) },
                            shape = RoundedCornerShape(14.dp),
                            colors = providerFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {
                        OutlinedTextField(
                            value = modelId, onValueChange = { modelId = it }, label = { Text(tx("Model ID")) },
                            singleLine = true, shape = RoundedCornerShape(14.dp), colors = providerFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = displayName, onValueChange = { displayName = it }, label = { Text(tx("Display name (optional)")) },
                            singleLine = true, shape = RoundedCornerShape(14.dp), colors = providerFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(tx("Type"), fontSize = 12.sp, color = AriMuted)
                        SegmentedTabs(
                            labels = listOf(tx("Chat"), tx("Image")),
                            selected = if (isImage) 1 else 0,
                            onSelect = { isImage = it == 1 }
                        )
                        Text(
                            tx("Image generation is separate from Vision input. If API metadata is missing, mark this as Image only when the model can create images."),
                            fontSize = 12.sp,
                            color = AriMuted
                        )
                        ToolToggleRow(tx("Accepts images"), tx("Vision input"), vision, enabled = true) { vision = it }
                        ToolToggleRow(tx("Tool calling"), tx("Function calling"), tools, enabled = true) { tools = it }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AriInk.copy(alpha = 0.5f), modifier = Modifier.padding(start = 6.dp))
}

/** Fetch models from the API, with the status or error underneath. */
@Composable
private fun FetchModelsRow(fetching: Boolean, canFetch: Boolean, status: String?, isError: Boolean, onFetch: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
            onClick = onFetch,
            enabled = !fetching && canFetch,
            shape = RoundedCornerShape(50),
            border = androidx.compose.foundation.BorderStroke(1.dp, AriLine)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = AriInk, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (fetching) tx("Loading…") else tx("Fetch models"), color = AriInk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        if (status != null) {
            Text(status, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (isError) AriAccent else AriInk)
        }
    }
}

@Composable
private fun providerFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AriAccent,
    unfocusedBorderColor = AriLine,
    focusedLabelColor = AriAccent,
    unfocusedLabelColor = AriMuted,
    cursorColor = AriAccent,
    focusedContainerColor = AriCard,
    unfocusedContainerColor = AriCard,
    errorBorderColor = AriError,
    errorLabelColor = AriError,
    errorSupportingTextColor = AriError
)

@Composable
private fun ProviderEditHeader(
    name: String,
    type: ProviderType,
    baseUrl: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val host = remember(baseUrl) {
        runCatching { java.net.URI(baseUrl.trim()).host }.getOrNull()?.removePrefix("www.").orEmpty()
    }
    val displayName = name.trim().ifBlank { tx("New provider") }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = AriCard.copy(alpha = 0.94f),
        shadowElevation = 1.dp,
        border = BorderStroke(0.75.dp, AriLine.copy(alpha = 0.8f)),
        modifier = Modifier.fillMaxWidth().testTag("provider-edit-header")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProviderBrandMark(type = type, name = displayName, size = 50.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = AriInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOf(providerTypeLabel(type), host).filter { it.isNotBlank() }.joinToString(" · "),
                    fontSize = 13.sp,
                    color = AriMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AriAccent,
                    checkedBorderColor = AriAccent,
                    uncheckedThumbColor = AriMuted,
                    uncheckedTrackColor = AriTint,
                    uncheckedBorderColor = AriLine
                )
            )
        }
    }
}

@Composable
private fun SegmentedTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(AriTint)
            .padding(4.dp)
            .animateContentSize(tween(220, easing = FastOutSlowInEasing)),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            val tabColor by animateColorAsState(
                targetValue = if (isSelected) AriAccent else Color.Transparent,
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                label = "provider-tab-background"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else AriInk.copy(alpha = 0.72f),
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                label = "provider-tab-foreground"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(tabColor)
                    .clickable { onSelect(index) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textColor)
            }
        }
    }
}
