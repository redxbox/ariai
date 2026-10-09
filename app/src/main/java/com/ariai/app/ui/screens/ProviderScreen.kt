package com.ariai.app.ui.screens

import com.ariai.app.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ariai.app.data.models.*
import com.ariai.app.util.LocalStrings
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
                        Text("AI Providers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Connect. Mix. Create.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onAddProvider) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
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
                    listOf("All", "Cloud", "Local", "Custom").forEachIndexed { index, label ->
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
                            Text("No providers yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Add your first AI provider to start chatting", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = onAddProvider, shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Provider")
                            }
                        }
                    }
                }
            }

            items(count = providers.size, key = { index -> "${providers[index].id}_${providers[index].createdAt}_$index" }) { index ->
                val provider = providers[index]
                GlassProviderCard(
                    provider = provider,
                    onEdit = { onEditProvider(provider) },
                    onDelete = { onDeleteProvider(provider) },
                    onTest = { onTestProvider(provider) }
                )
            }

            item {
                Text("Popular Providers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PopularProviderRow(name = "OpenAI", subtitle = "GPT-4o, 4o-mini, o3", letter = "O", colors = listOf(AriOpenAI, AriOpenAI.copy(alpha = 0.55f)), onClick = onAddProvider)
                    PopularProviderRow(name = "Anthropic", subtitle = "Claude 3.7, 3.5", letter = "C", colors = listOf(AriAnthropic, AriAnthropic.copy(alpha = 0.55f)), onClick = onAddProvider)
                    PopularProviderRow(name = "Google", subtitle = "Gemini 2.0, 1.5", letter = "G", colors = listOf(AriGemini, AriGemini.copy(alpha = 0.55f)), onClick = onAddProvider)
                    PopularProviderRow(name = "Meta", subtitle = "Llama 3.3, 3.1", letter = "M", colors = listOf(AriMuted, AriMuted), onClick = onAddProvider)
                    PopularProviderRow(name = "DeepSeek", subtitle = "R1, V3", letter = "D", colors = listOf(AriMuted, AriMuted), onClick = onAddProvider)
                    PopularProviderRow(name = "Qwen", subtitle = "Qwen3, Qwen2.5", letter = "Q", colors = listOf(AriCustom, AriCustom.copy(alpha = 0.55f)), onClick = onAddProvider)
                    PopularProviderRow(name = "Mistral", subtitle = "Large, Medium, Small", letter = "Mi", colors = listOf(AriAccent, AriAccent.copy(alpha = 0.55f)), onClick = onAddProvider)
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
                        Text("Add Custom Provider", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
            Switch(checked = enabled, onCheckedChange = { enabled = it })
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Test") }, onClick = { showMenu = false; onTest() }, leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) })
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit() }, leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) })
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
internal data class ProviderPreset(val label: String, val type: ProviderType, val baseUrl: String, val color: Color)

internal val providerPresets = listOf(
    ProviderPreset("OpenAI", ProviderType.OPENAI, "https://api.openai.com/v1", AriOpenAI),
    ProviderPreset("Gemini", ProviderType.GEMINI, "https://generativelanguage.googleapis.com/v1beta", AriGemini),
    ProviderPreset("Anthropic", ProviderType.ANTHROPIC, "https://api.anthropic.com/v1", AriAnthropic),
    ProviderPreset("Ollama", ProviderType.OLLAMA, "http://localhost:11434/v1", AriOllama),
    ProviderPreset("Custom", ProviderType.OPENAI_COMPATIBLE, "", AriCustom)
)

/**
 * Add or edit an AI provider. Brand presets fill in the endpoint and type,
 * models are created from the provider defaults when the provider is first saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProviderScreen(
    initialProvider: Provider? = null,
    presetType: ProviderType? = null,
    fetchModels: suspend (Provider) -> List<AIModel>,
    onSave: (Provider) -> Unit,
    onBack: () -> Unit,
    onDelete: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isEdit = initialProvider != null
    val initialPreset = providerPresets.firstOrNull { it.type == initialProvider?.type } ?: providerPresets.last()
    var name by remember { mutableStateOf(initialProvider?.name ?: providerPresets.firstOrNull { it.type == presetType }?.label.orEmpty()) }
    var baseUrl by remember { mutableStateOf(initialProvider?.baseUrl ?: providerPresets.firstOrNull { it.type == presetType }?.baseUrl ?: "https://api.openai.com/v1") }
    var apiKey by remember { mutableStateOf(initialProvider?.apiKey ?: "") }
    var selectedType by remember { mutableStateOf(initialProvider?.type ?: presetType ?: ProviderType.OPENAI) }
    var reveal by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val accent = providerPresets.firstOrNull { it.type == selectedType }?.color ?: initialPreset.color
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
        enabled = initialProvider?.enabled ?: true,
        customHeaders = headers.filter { it.first.isNotBlank() }.toMap(),
        customBody = customBody.trim().ifBlank { null },
        createdAt = initialProvider?.createdAt ?: System.currentTimeMillis()
    )
    fun fetchAndMerge(onDone: (List<AIModel>?) -> Unit) {
        fetching = true
        fetchError = null
        scope.launch {
            val result = runCatching { fetchModels(draftProvider(models)) }
            fetching = false
            result.onSuccess { fetched ->
                val merged = mergeFetched(models, fetched)
                models = merged
                fetchedKey = sourceKey()
                onDone(merged)
            }.onFailure {
                fetchError = "Could not load models: ${it.message ?: it::class.java.simpleName}"
                onDone(null)
            }
        }
    }
    val canSave = name.isNotBlank() && baseUrl.isNotBlank() && isValidJsonObject(customBody)
    if (showAddModel) {
        AddModelSheet(
            providerId = providerId,
            providerType = selectedType,
            onAdd = { model ->
                models = models + model
                modelsEdited = true
                showAddModel = false
            },
            onDismiss = { showAddModel = false }
        )
    }

    if (confirmDelete && initialProvider != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete provider?") },
            text = { Text("${initialProvider.name} and its models will be removed.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete(initialProvider.id) }) { Text("Delete", color = AriAccent) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        containerColor = AriPaper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AriPaper),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AriInk) } },
                title = { Text(if (isEdit) "Edit provider" else "Add provider", fontWeight = FontWeight.SemiBold, color = AriInk) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Brand presets
            item { SectionLabel("Provider") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    providerPresets.forEach { preset ->
                        val selected = selectedType == preset.type
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                .background(if (selected) preset.color.copy(alpha = 0.12f) else AriCard)
                                .clickable {
                                    selectedType = preset.type
                                    baseUrl = preset.baseUrl
                                    if (name.isBlank() && preset.type != ProviderType.OPENAI_COMPATIBLE) name = preset.label
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(preset.color), contentAlignment = Alignment.Center) {
                                Text(preset.label.first().toString(), color = AriCard, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text(preset.label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) preset.color else AriInk.copy(alpha = 0.7f), maxLines = 1)
                        }
                    }
                }
            }

            // Connection
            item { SectionLabel("Connection") }
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = AriCard, shadowElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            label = { Text("Base URL") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API key") },
                            singleLine = true,
                            visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { reveal = !reveal }) {
                                    Icon(Icons.Default.Visibility, contentDescription = if (reveal) "Hide key" else "Show key", tint = AriInk.copy(alpha = 0.5f))
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Stored only on this device.", fontSize = 12.sp, color = AriInk.copy(alpha = 0.5f))
                    }
                }
            }

            // Models: one grouped list. Fetch fills it from the API; rows can be removed.
            item { SectionLabel(if (models.isEmpty()) "Models" else "Models · ${models.size}") }
            item {
                val visibleModels = models.filter {
                    modelFilter.isBlank() || it.id.contains(modelFilter, ignoreCase = true) || it.displayName.contains(modelFilter, ignoreCase = true)
                }
                Surface(shape = RoundedCornerShape(22.dp), color = AriCard, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                fetchError ?: "Loaded from the API on save",
                                fontSize = 12.sp,
                                color = if (fetchError != null) AriAccent else AriMuted,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { fetchAndMerge {} }, enabled = !fetching && baseUrl.isNotBlank()) {
                                Text(if (fetching) "Loading…" else "Fetch", color = AriInk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                        if (models.size > 8) {
                            OutlinedTextField(
                                value = modelFilter,
                                onValueChange = { modelFilter = it },
                                placeholder = { Text("Filter models") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        if (models.isEmpty()) {
                            Text("No models yet", fontSize = 13.sp, color = AriMuted, modifier = Modifier.padding(16.dp))
                        }
                        // Only a few rows are composed by default; the rest load on expand.
                        val collapsible = visibleModels.size > 5 && modelFilter.isBlank()
                        val shownModels = if (collapsible && !modelsExpanded) visibleModels.take(5) else visibleModels
                        shownModels.forEachIndexed { index, model ->
                            if (index > 0) HorizontalDivider(color = AriTint, modifier = Modifier.padding(horizontal = 16.dp))
                            ModelRow(
                                model = model,
                                color = accent,
                                onRemove = { models = models.filterNot { it.id == model.id } }
                            )
                        }
                        if (collapsible) {
                            HorizontalDivider(color = AriTint)
                            TextButton(onClick = { modelsExpanded = !modelsExpanded }, modifier = Modifier.fillMaxWidth()) {
                                Icon(
                                    if (modelsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (modelsExpanded) "Collapse" else "Expand",
                                    tint = AriInk,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (modelsExpanded) "Show less" else "Show all ${visibleModels.size}",
                                    color = AriInk,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        HorizontalDivider(color = AriTint)
                        TextButton(onClick = { showAddModel = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = AriInk, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add model", color = AriInk, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Advanced: headers and body sent to every model of this provider.
            item { SectionLabel("Advanced") }
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = AriCard, shadowElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Custom headers", fontWeight = FontWeight.Medium, color = AriInk)
                        KeyValueEditor(items = headers, onChange = { headers = it })
                        OutlinedTextField(
                            value = customBody,
                            onValueChange = { customBody = it },
                            label = { Text("Custom body (JSON)") },
                            minLines = 2,
                            maxLines = 6,
                            isError = !isValidJsonObject(customBody),
                            supportingText = { if (!isValidJsonObject(customBody)) Text("Must be a JSON object") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(4.dp)) }

            // Actions
            item {
                Button(
                    onClick = {
                        if (sourceKey() != fetchedKey) {
                            // URL, key or type changed: load the models from the API first.
                            fetchAndMerge { list -> onSave(draftProvider(list ?: models)) }
                        } else {
                            onSave(draftProvider(models))
                        }
                    },
                    enabled = canSave && !fetching,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AriInk),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text(if (fetching) "Loading models…" else "Save", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
            }

            if (isEdit) {
                item {
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text("Delete provider", color = AriAccent, fontWeight = FontWeight.SemiBold) }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
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
                if (model.supportsVision) add(Triple("Vision", AriTagBlueBg, AriTagBlueFg))
                if (model.supportsFunctionCalling) add(Triple("Tools", AriTagGreenBg, AriTagGreenFg))
                if (model.supportsImageGen) add(Triple("Image", AriTagAmberBg, AriTagAmberFg))
                if ("google_search" in model.builtInTools) add(Triple("Search", AriTagAmberBg, AriTagAmberFg))
                if ("url_context" in model.builtInTools) add(Triple("URL", AriTagAmberBg, AriTagAmberFg))
            }
            if (tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                    tags.forEach { (label, bg, fg) -> ModelTag(label, bg, fg) }
                }
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Remove model", tint = AriMuted, modifier = Modifier.size(18.dp))
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
                    label = { Text("Header") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = pair.second,
                    onValueChange = { value -> onChange(items.toMutableList().also { it[index] = pair.first to value }) },
                    label = { Text("Value") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onChange(items.toMutableList().also { it.removeAt(index) }) }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove header", tint = AriMuted)
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
            Text("Add header")
        }
    }
}

/** True when the text is empty or a JSON object. */
/** Replaces the list with the API's models, keeping settings already set for the same IDs and custom models. */
private fun mergeFetched(current: List<AIModel>, fetched: List<AIModel>): List<AIModel> {
    val byId = current.associateBy { it.id }
    val fromApi = fetched.map { byId[it.id] ?: it }
    val customOnly = current.filter { it.isCustom && fetched.none { f -> f.id == it.id } }
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
    var tab by remember { mutableStateOf(2) }
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

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AriPaper) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Add model", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = AriInk)
            TabRow(selectedTabIndex = tab, containerColor = AriPaper) {
                listOf("Built-in Tools", "Advanced", "Basic").forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label, fontSize = 12.sp) })
                }
            }

            when (tab) {
                0 -> {
                    Text(
                        if (isGemini) "Tools the API runs for this model." else "Built-in tools need the official Gemini API. Turn on a Gemini provider to use them.",
                        fontSize = 12.sp, color = AriMuted
                    )
                    ToolToggleRow("Search", "Google Search grounding", search, enabled = isGemini) { search = it }
                    ToolToggleRow("URL context", "Read the links in the message", urlContext, enabled = isGemini) { urlContext = it }
                }
                1 -> {
                    Text("Extra headers for this model only.", fontSize = 12.sp, color = AriMuted)
                    KeyValueEditor(items = headers, onChange = { headers = it })
                    OutlinedTextField(
                        value = customBody,
                        onValueChange = { customBody = it },
                        label = { Text("Custom body (JSON)") },
                        placeholder = { Text("{\"temperature\": 0.2}") },
                        minLines = 3,
                        maxLines = 6,
                        isError = !bodyValid,
                        supportingText = { if (!bodyValid) Text("Must be a JSON object") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> {
                    OutlinedTextField(
                        value = modelId, onValueChange = { modelId = it }, label = { Text("Model ID") },
                        singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = displayName, onValueChange = { displayName = it }, label = { Text("Display name (optional)") },
                        singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                    )
                    Text("Type", fontSize = 12.sp, color = AriMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !isImage, onClick = { isImage = false }, label = { Text("Chat") })
                        FilterChip(selected = isImage, onClick = { isImage = true }, label = { Text("Image") })
                    }
                    ToolToggleRow("Accepts images", "Vision input", vision, enabled = true) { vision = it }
                    ToolToggleRow("Tool calling", "Function calling", tools, enabled = true) { tools = it }
                }
            }

            Button(
                onClick = {
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
                            builtInTools = listOfNotNull(
                                "google_search".takeIf { search && isGemini },
                                "url_context".takeIf { urlContext && isGemini }
                            ),
                            headers = headers.filter { it.first.isNotBlank() }.toMap(),
                            customBody = customBody.trim().ifBlank { null }
                        )
                    )
                },
                enabled = modelId.isNotBlank() && bodyValid,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AriInk),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Add", fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AriInk.copy(alpha = 0.5f), modifier = Modifier.padding(start = 6.dp))
}
