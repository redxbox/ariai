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
    var searchQuery by remember { mutableStateOf("") }

    Box(
        modifier = modifier.fillMaxSize().background(Color(0xFF0A0A1E))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                            }
                        },
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("AI Providers", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                Text("One app. Many minds.", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        actions = {
                            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    )
                    // Search bar
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                            Text("Search providers, models...", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    // Tabs All Cloud Local Custom
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Cloud", "Local", "Custom").forEach { tab ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (selectedTab == tab) Color(0xFF6C4DFF) else Color.White.copy(alpha = 0.08f),
                                modifier = Modifier.weight(1f).clickable { selectedTab = tab }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                                    Text(tab, color = if (selectedTab == tab) Color.White else Color.White.copy(alpha = 0.6f), fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth().clickable { onAddProvider() }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Custom Provider", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("More models. More possibilities.", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Provider list like screenshot
                item { ProviderRow(name = "OpenAI", models = "GPT-4o, GPT-4.1, GPT-3.5", badge = "Cloud", iconColor = Color(0xFF10A37F), letter = "O", enabled = true, onClick = {}) }
                item { ProviderRow(name = "Anthropic", models = "Claude 3.7, 3.5, 3", badge = "Cloud", iconColor = Color(0xFFE8D5B5), letter = "AI", enabled = false, onClick = {}) }
                item { ProviderRow(name = "Google", models = "Gemini 2.0, 1.5, 1.0", badge = "Cloud", iconColor = Color(0xFF4285F4), letter = "G", enabled = true, onClick = {}) }
                item { ProviderRow(name = "Meta", models = "Llama 3.3, 3.1, 3.0", badge = "Cloud", iconColor = Color(0xFF0668E1), letter = "∞", enabled = false, onClick = {}) }
                item { ProviderRow(name = "DeepSeek", models = "R1, V3, Coder", badge = "Cloud", iconColor = Color(0xFF4D6BFE), letter = "D", enabled = true, onClick = {}) }
                item { ProviderRow(name = "Qwen", models = "Qwen3, Qwen2.5", badge = "Cloud", iconColor = Color(0xFF7C4DFF), letter = "Q", enabled = false, onClick = {}) }
                item { ProviderRow(name = "Mistral", models = "Large, Medium, Small", badge = "Cloud", iconColor = Color(0xFFFF6B35), letter = "M", enabled = false, onClick = {}) }
                item { ProviderRow(name = "Ollama", models = "Run local models", badge = "Local", iconColor = Color(0xFF000000), letter = "🦙", enabled = false, onClick = {}) }
                item { ProviderRow(name = "Custom Endpoint", models = "OpenAI compatible", badge = "Custom", iconColor = Color(0xFF333333), letter = "🔗", enabled = false, onClick = {}) }

                // Also show user added providers
                items(providers.size) { index ->
                    val p = providers[index]
                    ProviderRow(name = p.name, models = p.models.take(2).joinToString(", ") { it.displayName }.ifEmpty { p.baseUrl.take(20) }, badge = "Custom", iconColor = Color(0xFF6C4DFF), letter = p.name.firstOrNull()?.toString() ?: "A", enabled = true, onClick = { onEditProvider(p) })
                }
            }
        }
    }
}

@Composable
fun ProviderRow(name: String, models: String, badge: String, iconColor: Color, letter: String, enabled: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(iconColor),
                contentAlignment = Alignment.Center
            ) {
                Text(letter.take(2), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(models, color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (badge) {
                    "Cloud" -> Color(0xFF00BCD4).copy(alpha = 0.15f)
                    "Local" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                    else -> Color(0xFF9C27B0).copy(alpha = 0.15f)
                },
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    badge,
                    color = when (badge) {
                        "Cloud" -> Color(0xFF00BCD4)
                        "Local" -> Color(0xFF4CAF50)
                        else -> Color(0xFFCE93D8)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF6C4DFF),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(20.dp).padding(start = 8.dp))
        }
    }
}
