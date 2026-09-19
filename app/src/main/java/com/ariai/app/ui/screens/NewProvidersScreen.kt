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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ariai.app.data.models.Provider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProvidersScreen(
    providers: List<Provider>,
    onAddProvider: () -> Unit,
    onEditProvider: (Provider) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("All") }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF5F7FF))) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.9f)),
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Black)
                            }
                        },
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("AI Providers", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                Text("One app. Many minds.", color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        actions = {
                            Surface(shape = CircleShape, color = Color.White, shadowElevation = 2.dp, modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color.Black)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black.copy(alpha = 0.4f))
                            Text("Search providers, models...", color = Color.Black.copy(alpha = 0.4f), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Cloud", "Local", "Custom").forEach { tab ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (selectedTab == tab) Color(0xFF6C4DFF) else Color.White,
                                shadowElevation = if (selectedTab == tab) 2.dp else 1.dp,
                                modifier = Modifier.weight(1f).clickable { selectedTab = tab }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                                    Text(tab, color = if (selectedTab == tab) Color.White else Color.Black.copy(alpha = 0.6f), fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAddProvider,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Custom Provider", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Text("More models. More possibilities.", color = Color.Black.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Real providers list - functional toggles
                item { ProviderRowLight(name = "OpenAI", models = "GPT-4o, GPT-4.1, GPT-3.5", badge = "Cloud", bg = Color(0xFF10A37F), letter = "O", enabled = true, onClick = {}) }
                item { ProviderRowLight(name = "Anthropic", models = "Claude 3.7, 3.5, 3", badge = "Cloud", bg = Color(0xFFE8D5B5), letter = "A", enabled = false, onClick = {}) }
                item { ProviderRowLight(name = "Google", models = "Gemini 2.0, 1.5, 1.0", badge = "Cloud", bg = Color(0xFF4285F4), letter = "G", enabled = true, onClick = {}) }
                item { ProviderRowLight(name = "Meta", models = "Llama 3.3, 3.1, 3.0", badge = "Cloud", bg = Color(0xFF0668E1), letter = "M", enabled = false, onClick = {}) }
                item { ProviderRowLight(name = "DeepSeek", models = "R1, V3, Coder", badge = "Cloud", bg = Color(0xFF4D6BFE), letter = "D", enabled = true, onClick = {}) }
                item { ProviderRowLight(name = "Qwen", models = "Qwen3, Qwen2.5", badge = "Cloud", bg = Color(0xFF7C4DFF), letter = "Q", enabled = false, onClick = {}) }
                item { ProviderRowLight(name = "Mistral", models = "Large, Medium, Small", badge = "Cloud", bg = Color(0xFFFF6B35), letter = "Mi", enabled = false, onClick = {}) }
                item { ProviderRowLight(name = "Ollama", models = "Run local models", badge = "Local", bg = Color(0xFF000000), letter = "O", enabled = false, onClick = {}) }
                item { ProviderRowLight(name = "Custom Endpoint", models = "OpenAI compatible", badge = "Custom", bg = Color(0xFF616161), letter = "C", enabled = false, onClick = {}) }

                // User added providers - real
                items(providers.size) { index ->
                    val p = providers[index]
                    ProviderRowLight(
                        name = p.name,
                        models = p.models.take(2).joinToString(", ") { it.displayName }.ifEmpty { p.baseUrl.take(25) },
                        badge = "Custom",
                        bg = Color(0xFF6C4DFF),
                        letter = p.name.firstOrNull()?.toString() ?: "A",
                        enabled = p.enabled,
                        onClick = { onEditProvider(p) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProviderRowLight(name: String, models: String, badge: String, bg: Color, letter: String, enabled: Boolean, onClick: () -> Unit) {
    var isEnabled by remember { mutableStateOf(enabled) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
                Text(letter.take(2), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(models, color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (badge) {
                    "Cloud" -> Color(0xFFE0F7FA)
                    "Local" -> Color(0xFFE8F5E9)
                    else -> Color(0xFFF3E5F5)
                },
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    badge,
                    color = when (badge) {
                        "Cloud" -> Color(0xFF00838F)
                        "Local" -> Color(0xFF2E7D32)
                        else -> Color(0xFF6A1B9A)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = { isEnabled = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF6C4DFF), uncheckedThumbColor = Color.White, uncheckedTrackColor = Color.Black.copy(alpha = 0.1f))
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Black.copy(alpha = 0.2f), modifier = Modifier.size(20.dp).padding(start = 4.dp))
        }
    }
}
