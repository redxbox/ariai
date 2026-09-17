package com.ariai.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    val freeProviders = remember { getFreeProviders() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(strings.providers, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${providers.size} configured • ${freeProviders.size} free available", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProvider,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(strings.addProvider) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Free Demo Section - Graphical
            item {
                FreeDemoHeader()
            }

            item {
                Text(
                    "✨ Free Demo Providers - No Credit Card",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "Start instantly without API keys. Perfect for trying AriAi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(freeProviders.filter { it.isRecommended }) { freeProvider ->
                FreeProviderCard(
                    freeProvider = freeProvider,
                    isConfigured = providers.any { it.baseUrl.contains(freeProvider.baseUrl.take(20)) || it.name == freeProvider.name },
                    onAdd = {
                        // Auto-add with empty key if no key required, otherwise prompt
                        if (!freeProvider.requiresKey) {
                            onAddProvider() // Will handle in parent, but for demo we add directly
                        } else {
                            onAddProvider()
                        }
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🌐 More Free Options",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${freeProviders.filter { !it.isRecommended }.size} providers",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(freeProviders.filter { !it.isRecommended }) { freeProvider ->
                CompactFreeProviderCard(
                    freeProvider = freeProvider,
                    isConfigured = providers.any { it.name == freeProvider.name },
                    onAdd = { onAddProvider() }
                )
            }

            // Configured Providers
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Your Providers (${providers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (providers.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🤖", style = MaterialTheme.typography.displaySmall)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(strings.noProviders, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(strings.addFirstProvider, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onAddProvider,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.addProvider)
                            }
                        }
                    }
                }
            }

            items(providers, key = { it.id }) { provider ->
                PremiumProviderCard(
                    provider = provider,
                    onEdit = { onEditProvider(provider) },
                    onDelete = { onDeleteProvider(provider) },
                    onTest = { onTestProvider(provider) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun FreeDemoHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6C4DFF),
                            Color(0xFF00D4AA),
                            Color(0xFFFF6B6B)
                        ),
                        start = androidx.compose.ui.geometry.Offset(offset * 100, 0f),
                        end = androidx.compose.ui.geometry.Offset(1000f - offset * 100, 1000f)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("🚀", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Try AriAi Free!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "No API key needed for demo",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Pollinations, LLM7 and other free providers work instantly. For best experience, add Groq or Gemini free keys (30 sec setup).",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeProviderCard(
    freeProvider: FreeProviderInfo,
    isConfigured: Boolean,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientColors = freeProvider.gradient.map { hex ->
        try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            Color(0xFF6C4DFF)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = gradientColors + gradientColors.first()
                    )
                )
                .padding(1.dp)
        ) {
            Card(
                shape = RoundedCornerShape(19.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(freeProvider.icon, style = MaterialTheme.typography.titleLarge)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        freeProvider.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (freeProvider.isRecommended) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFFD700).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                "★ Recommended",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFB8860B),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Text(
                                    freeProvider.freeLimit,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (freeProvider.requiresKey) MaterialTheme.colorScheme.tertiary else Color(0xFF00A86B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isConfigured) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF00C853).copy(alpha = 0.1f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(18.dp))
                                }
                            }
                        } else {
                            FilledTonalButton(
                                onClick = onAdd,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(if (freeProvider.requiresKey) "Add" else "Use Free", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        freeProvider.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        freeProvider.models.take(3).forEach { model ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    model.take(20),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (freeProvider.models.size > 3) {
                            Text(
                                "+${freeProvider.models.size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (freeProvider.requiresKey) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Get key: ${freeProvider.keyUrl}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactFreeProviderCard(
    freeProvider: FreeProviderInfo,
    isConfigured: Boolean,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(freeProvider.icon, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(freeProvider.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(freeProvider.freeLimit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isConfigured) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(20.dp))
            } else {
                IconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumProviderCard(
    provider: Provider,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
    val providerColor = when (provider.type) {
        ProviderType.OPENAI -> listOf(Color(0xFF74AA9C), Color(0xFF10A37F))
        ProviderType.GEMINI -> listOf(Color(0xFF4285F4), Color(0xFF34A853))
        ProviderType.ANTHROPIC -> listOf(Color(0xFFD4A574), Color(0xFFCC785C))
        ProviderType.OLLAMA -> listOf(Color(0xFF000000), Color(0xFF434343))
        else -> listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(colors = providerColor)
                        ),
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
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (provider.type) {
                                ProviderType.OPENAI -> Color(0xFF10A37F).copy(alpha = 0.1f)
                                ProviderType.GEMINI -> Color(0xFF4285F4).copy(alpha = 0.1f)
                                ProviderType.ANTHROPIC -> Color(0xFFCC785C).copy(alpha = 0.1f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            }
                        ) {
                            Text(
                                provider.type.name,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = when (provider.type) {
                                    ProviderType.OPENAI -> Color(0xFF10A37F)
                                    ProviderType.GEMINI -> Color(0xFF4285F4)
                                    ProviderType.ANTHROPIC -> Color(0xFFCC785C)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            provider.baseUrl.take(25) + if (provider.baseUrl.length > 25) "..." else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (provider.models.isNotEmpty()) {
                        Text(
                            "${provider.models.size} models • ${if (provider.apiKey.isNotBlank()) "Key set" else "No key"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Test Connection") },
                            onClick = { showMenu = false; onTest() },
                            leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { showMenu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                }
            }
            
            if (provider.models.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    provider.models.take(3).forEach { model ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                text = model.displayName.take(18),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                maxLines = 1
                            )
                        }
                    }
                    if (provider.models.size > 3) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                "+${provider.models.size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
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
    var showFreeProviders by remember { mutableStateOf(initialProvider == null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialProvider == null) strings.addProvider else strings.edit, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (name.isNotBlank() && baseUrl.isNotBlank()) {
                                onSave(
                                    Provider(
                                        id = initialProvider?.id ?: java.util.UUID.randomUUID().toString(),
                                        name = name,
                                        type = selectedType,
                                        baseUrl = baseUrl,
                                        apiKey = apiKey,
                                        models = initialProvider?.models ?: emptyList()
                                    )
                                )
                            }
                        },
                        enabled = name.isNotBlank() && baseUrl.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.save)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showFreeProviders && initialProvider == null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🚀", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Quick Start - Free Providers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            getFreeProviders().take(3).forEach { free ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            name = free.name
                                            baseUrl = free.baseUrl
                                            selectedType = free.type
                                            apiKey = ""
                                            showFreeProviders = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(free.icon, style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(free.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(free.freeLimit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            TextButton(onClick = { showFreeProviders = false }) {
                                Text("Custom setup instead")
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.providerName) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("OpenAI, Gemini, Groq...") },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = showTypeMenu,
                    onExpandedChange = { showTypeMenu = !showTypeMenu }
                ) {
                    OutlinedTextField(
                        value = selectedType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.providerType) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeMenu) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false }
                    ) {
                        ProviderType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedType = type
                                    showTypeMenu = false
                                    baseUrl = when (type) {
                                        ProviderType.OPENAI -> "https://api.openai.com/v1"
                                        ProviderType.GEMINI -> "https://generativelanguage.googleapis.com/v1beta"
                                        ProviderType.ANTHROPIC -> "https://api.anthropic.com"
                                        ProviderType.OLLAMA -> "http://localhost:11434/v1"
                                        else -> baseUrl
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text(strings.baseUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://api.openai.com/v1") },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(strings.apiKey) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(if (baseUrl.contains("pollinations") || baseUrl.contains("llm7")) "No key needed - leave empty" else "sk-...") },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("How to get API key?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (selectedType) {
                                ProviderType.OPENAI -> "OpenAI: platform.openai.com > API keys > Create new key (paid)"
                                ProviderType.GEMINI -> "Gemini: aistudio.google.com > Get API key (FREE 1500/day)"
                                ProviderType.ANTHROPIC -> "Claude: console.anthropic.com > API keys"
                                ProviderType.OLLAMA -> "Ollama: No API key needed for local"
                                else -> if (baseUrl.contains("groq")) "Groq: console.groq.com/keys (FREE 14K/day - Recommended!)"
                                else if (baseUrl.contains("pollinations") || baseUrl.contains("llm7")) "Free! No API key required - just save and use"
                                else "Check provider docs for API key"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
