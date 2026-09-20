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

    Box(modifier = modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFE8F0FF), Color(0xFFF0F4FF), Color(0xFFF8FAFF))))) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.7f)),
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Black)
                            }
                        },
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("AI Providers", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                Text("Connect. Mix. Create.", color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        actions = {
                            IconButton(onClick = onAddProvider) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    )
                    // Tabs like screenshot - All Cloud Local Custom
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Cloud", "Local", "Custom").forEach { tab ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (selectedTab == tab) Color(0xFFE8E0FF) else Color.White.copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedTab == tab) Color(0xFF6C4DFF).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f).clickable { selectedTab = tab }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                                    Text(tab, color = if (selectedTab == tab) Color(0xFF6C4DFF) else Color.Black.copy(alpha = 0.6f), fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onAddProvider() }
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF6C4DFF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Custom Provider", color = Color(0xFF6C4DFF), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Providers like screenshot - with icons and toggles
                item { ProviderGlassRow(name = "OpenAI", models = "GPT-4o, 4o-mini, o3", iconBg = Color(0xFF10A37F), letter = "O", enabled = true) }
                item { ProviderGlassRow(name = "Anthropic", models = "Claude 3.7, 3.5", iconBg = Color(0xFFD4A574), letter = "A", enabled = false) }
                item { ProviderGlassRow(name = "Google", models = "Gemini 2.0, 1.5", iconBg = Color(0xFF4285F4), letter = "G", enabled = true) }
                item { ProviderGlassRow(name = "Meta", models = "Llama 3.3, 3.1", iconBg = Color(0xFF0668E1), letter = "∞", enabled = false) }
                item { ProviderGlassRow(name = "DeepSeek", models = "R1, V3", iconBg = Color(0xFF4D6BFE), letter = "D", enabled = true) }
                item { ProviderGlassRow(name = "Qwen", models = "Qwen3, Qwen2.5", iconBg = Color(0xFF7C4DFF), letter = "Q", enabled = false) }
                item { ProviderGlassRow(name = "Mistral", models = "Large, Medium, Small", iconBg = Color(0xFFFF6B35), letter = "M", enabled = false) }

                // User providers
                items(providers.size) { index ->
                    val p = providers[index]
                    ProviderGlassRow(
                        name = p.name,
                        models = p.models.take(2).joinToString(", ") { it.displayName }.ifEmpty { p.baseUrl.take(20) },
                        iconBg = Color(0xFF6C4DFF),
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
fun ProviderGlassRow(name: String, models: String, iconBg: Color, letter: String, enabled: Boolean, onClick: () -> Unit = {}) {
    var isEnabled by remember { mutableStateOf(enabled) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                Text(letter.take(2), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(models, color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = { isEnabled = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF6C4DFF), uncheckedThumbColor = Color.White, uncheckedTrackColor = Color.Black.copy(alpha = 0.1f))
            )
        }
    }
}
