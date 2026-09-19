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
@Composable
fun AddProviderScreen(
    initialProvider: Provider? = null,
    onSave: (Provider) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var name by remember { mutableStateOf(initialProvider?.name ?: "") }
    var baseUrl by remember { mutableStateOf(initialProvider?.baseUrl ?: "https://api.openai.com/v1") }
    var apiKey by remember { mutableStateOf(initialProvider?.apiKey ?: "") }
    var selectedType by remember { mutableStateOf(initialProvider?.type ?: ProviderType.OPENAI_COMPATIBLE) }
    var showTypeMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialProvider == null) strings.addProvider else strings.edit, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } },
                actions = {
                    Button(
                        onClick = {
                            if (name.isNotBlank() && baseUrl.isNotBlank()) {
                                onSave(Provider(id = initialProvider?.id ?: java.util.UUID.randomUUID().toString(), name = name, type = selectedType, baseUrl = baseUrl, apiKey = apiKey, models = initialProvider?.models ?: emptyList()))
                            }
                        },
                        enabled = name.isNotBlank() && baseUrl.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(strings.save) }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Quick Setup", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("OpenAI" to "https://api.openai.com/v1", "Gemini" to "https://generativelanguage.googleapis.com/v1beta", "Groq" to "https://api.groq.com/openai/v1").forEach { (label, url) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (baseUrl == url) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.clickable {
                                        baseUrl = url
                                        name = label
                                        selectedType = if (label == "Gemini") ProviderType.GEMINI else ProviderType.OPENAI_COMPATIBLE
                                    }
                                ) {
                                    Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.providerName) }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("OpenAI, Gemini, Groq...") }, shape = RoundedCornerShape(12.dp))
            }
            item {
                ExposedDropdownMenuBox(expanded = showTypeMenu, onExpandedChange = { showTypeMenu = !showTypeMenu }) {
                    OutlinedTextField(value = selectedType.name, onValueChange = {}, readOnly = true, label = { Text(strings.providerType) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeMenu) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                    ExposedDropdownMenu(expanded = showTypeMenu, onDismissRequest = { showTypeMenu = false }) {
                        ProviderType.values().forEach { type ->
                            DropdownMenuItem(text = { Text(type.name) }, onClick = {
                                selectedType = type
                                showTypeMenu = false
                                baseUrl = when (type) {
                                    ProviderType.OPENAI -> "https://api.openai.com/v1"
                                    ProviderType.GEMINI -> "https://generativelanguage.googleapis.com/v1beta"
                                    ProviderType.ANTHROPIC -> "https://api.anthropic.com"
                                    ProviderType.OLLAMA -> "http://localhost:11434/v1"
                                    else -> baseUrl
                                }
                            })
                        }
                    }
                }
            }
            item {
                OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text(strings.baseUrl) }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("https://api.openai.com/v1") }, shape = RoundedCornerShape(12.dp))
            }
            item {
                OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text(strings.apiKey) }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("sk-...") }, shape = RoundedCornerShape(12.dp))
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("How to get API key?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (selectedType) {
                                ProviderType.OPENAI -> "OpenAI: platform.openai.com > API keys"
                                ProviderType.GEMINI -> "Gemini: aistudio.google.com > Get API key"
                                ProviderType.ANTHROPIC -> "Claude: console.anthropic.com > API keys"
                                ProviderType.OLLAMA -> "Ollama: No API key needed for local"
                                else -> "Check provider docs for API key"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
