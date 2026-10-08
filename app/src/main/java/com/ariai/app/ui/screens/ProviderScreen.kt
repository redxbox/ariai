package com.ariai.app.ui.screens

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
                                    Brush.linearGradient(colors = listOf(Color(0xFF6C4DFF).copy(alpha = 0.3f), Color(0xFF00D4AA).copy(alpha = 0.3f)))
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
                    PopularProviderRow(name = "OpenAI", subtitle = "GPT-4o, 4o-mini, o3", letter = "O", colors = listOf(Color(0xFF74AA9C), Color(0xFF10A37F)), onClick = onAddProvider)
                    PopularProviderRow(name = "Anthropic", subtitle = "Claude 3.7, 3.5", letter = "C", colors = listOf(Color(0xFFD4A574), Color(0xFFCC785C)), onClick = onAddProvider)
                    PopularProviderRow(name = "Google", subtitle = "Gemini 2.0, 1.5", letter = "G", colors = listOf(Color(0xFF4285F4), Color(0xFF34A853)), onClick = onAddProvider)
                    PopularProviderRow(name = "Meta", subtitle = "Llama 3.3, 3.1", letter = "M", colors = listOf(Color(0xFF0668E1), Color(0xFF00C7FB)), onClick = onAddProvider)
                    PopularProviderRow(name = "DeepSeek", subtitle = "R1, V3", letter = "D", colors = listOf(Color(0xFF4D6BFE), Color(0xFF6C4DFF)), onClick = onAddProvider)
                    PopularProviderRow(name = "Qwen", subtitle = "Qwen3, Qwen2.5", letter = "Q", colors = listOf(Color(0xFF7C4DFF), Color(0xFF536DFE)), onClick = onAddProvider)
                    PopularProviderRow(name = "Mistral", subtitle = "Large, Medium, Small", letter = "Mi", colors = listOf(Color(0xFFFF6B35), Color(0xFFF7931E)), onClick = onAddProvider)
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
        ProviderType.OPENAI -> listOf(Color(0xFF74AA9C), Color(0xFF10A37F))
        ProviderType.GEMINI -> listOf(Color(0xFF4285F4), Color(0xFF34A853))
        ProviderType.ANTHROPIC -> listOf(Color(0xFFD4A574), Color(0xFFCC785C))
        ProviderType.OLLAMA -> listOf(Color(0xFF000000), Color(0xFF434343))
        else -> listOf(Color(0xFF6C4DFF), Color(0xFF00D4AA))
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
                    color = Color.White,
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
                Text(letter, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
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
private data class ProviderPreset(val label: String, val type: ProviderType, val baseUrl: String, val color: Color)

private val providerPresets = listOf(
    ProviderPreset("OpenAI", ProviderType.OPENAI, "https://api.openai.com/v1", Color(0xFF10A37F)),
    ProviderPreset("Gemini", ProviderType.GEMINI, "https://generativelanguage.googleapis.com/v1beta", Color(0xFF4285F4)),
    ProviderPreset("Anthropic", ProviderType.ANTHROPIC, "https://api.anthropic.com/v1", Color(0xFFC96442)),
    ProviderPreset("Ollama", ProviderType.OLLAMA, "http://localhost:11434/v1", Color(0xFF3A3A3C)),
    ProviderPreset("Custom", ProviderType.OPENAI_COMPATIBLE, "", Color(0xFF6C4DFF))
)

/**
 * Add or edit an AI provider. Brand presets fill in the endpoint and type,
 * models are created from the provider defaults when the provider is first saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProviderScreen(
    initialProvider: Provider? = null,
    onSave: (Provider) -> Unit,
    onBack: () -> Unit,
    onDelete: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isEdit = initialProvider != null
    val initialPreset = providerPresets.firstOrNull { it.type == initialProvider?.type } ?: providerPresets.last()
    var name by remember { mutableStateOf(initialProvider?.name ?: "") }
    var baseUrl by remember { mutableStateOf(initialProvider?.baseUrl ?: "https://api.openai.com/v1") }
    var apiKey by remember { mutableStateOf(initialProvider?.apiKey ?: "") }
    var selectedType by remember { mutableStateOf(initialProvider?.type ?: ProviderType.OPENAI) }
    var reveal by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val accent = providerPresets.firstOrNull { it.type == selectedType }?.color ?: initialPreset.color
    val canSave = name.isNotBlank() && baseUrl.isNotBlank()

    if (confirmDelete && initialProvider != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete provider?") },
            text = { Text("${initialProvider.name} and its models will be removed.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete(initialProvider.id) }) { Text("Delete", color = Color(0xFFBA1A1A)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        containerColor = Color(0xFFF7F6FB),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF7F6FB)),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF1C1B1F)) } },
                title = { Text(if (isEdit) "Edit provider" else "Add provider", fontWeight = FontWeight.SemiBold, color = Color(0xFF1C1B1F)) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero preview
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.55f))))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        }
                        Column {
                            Text(name.ifBlank { "Provider name" }, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(providerTypeLabel(selectedType), color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        }
                    }
                }
            }

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
                                .background(if (selected) preset.color.copy(alpha = 0.12f) else Color.White)
                                .clickable {
                                    selectedType = preset.type
                                    baseUrl = preset.baseUrl
                                    if (name.isBlank() && preset.type != ProviderType.OPENAI_COMPATIBLE) name = preset.label
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(preset.color), contentAlignment = Alignment.Center) {
                                Text(preset.label.first().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text(preset.label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) preset.color else Color(0xFF1C1B1F).copy(alpha = 0.7f), maxLines = 1)
                        }
                    }
                }
            }

            // Connection
            item { SectionLabel("Connection") }
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
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
                                    Icon(Icons.Default.Visibility, contentDescription = if (reveal) "Hide key" else "Show key", tint = Color(0xFF1C1B1F).copy(alpha = 0.5f))
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Stored only on this device.", fontSize = 12.sp, color = Color(0xFF1C1B1F).copy(alpha = 0.5f))
                    }
                }
            }

            // Models
            item { SectionLabel("Models") }
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (initialProvider?.models?.isNotEmpty() == true) "${initialProvider.models.size} models" else "Default models for this provider",
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1C1B1F)
                            )
                            Text("Added automatically when you save.", fontSize = 12.sp, color = Color(0xFF1C1B1F).copy(alpha = 0.5f))
                        }
                        Icon(Icons.Default.Star, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                    }
                }
            }

            item { Spacer(Modifier.height(4.dp)) }

            // Actions
            item {
                Button(
                    onClick = {
                        val id = initialProvider?.id ?: java.util.UUID.randomUUID().toString()
                        val models = if (initialProvider?.models?.isNotEmpty() == true) initialProvider.models
                        else getDefaultModelsForType(selectedType, id)
                        onSave(
                            Provider(
                                id = id,
                                name = name.trim(),
                                type = selectedType,
                                baseUrl = baseUrl.trim(),
                                apiKey = apiKey.trim(),
                                models = models,
                                enabled = initialProvider?.enabled ?: true,
                                customHeaders = initialProvider?.customHeaders ?: emptyMap(),
                                customBody = initialProvider?.customBody,
                                createdAt = initialProvider?.createdAt ?: System.currentTimeMillis()
                            )
                        )
                    },
                    enabled = canSave,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4DFF)),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Save", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
            }

            if (isEdit) {
                item {
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text("Delete provider", color = Color(0xFFBA1A1A), fontWeight = FontWeight.SemiBold) }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1C1B1F).copy(alpha = 0.5f), modifier = Modifier.padding(start = 6.dp))
}
