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
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Provider

// RikkaHub inspired providers - clean minimal not crowded
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
    val tabs = listOf("All", "Cloud", "Local", "Custom")

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFFEFBFF))) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                        }
                    },
                    title = { Text("Providers", fontWeight = FontWeight.SemiBold, color = Color.Black) },
                    actions = {
                        IconButton(onClick = onAddProvider) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black)
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onAddProvider,
                    containerColor = Color(0xFF6C4DFF),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Provider")
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Tabs - clean pill style like RikkaHub
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        tabs.forEach { tab ->
                            val selected = selectedTab == tab
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (selected) Color(0xFF6C4DFF) else Color.White,
                                border = if (!selected) androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f)) else null,
                                modifier = Modifier.clickable { selectedTab = tab }
                            ) {
                                Text(tab, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = if (selected) Color.White else Color.Black.copy(alpha = 0.7f), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 13.sp)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Built-in providers - clean list
                item {
                    Text("Built-in", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Black.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 4.dp))
                }

                val builtIn = listOf(
                    Triple("OpenAI", "GPT-4o, 4o-mini, o3", Color(0xFF10A37F)),
                    Triple("Anthropic", "Claude 3.5 Sonnet", Color(0xFFD4A574)),
                    Triple("Google", "Gemini 2.0 Flash", Color(0xFF4285F4)),
                    Triple("Meta", "Llama 3.3", Color(0xFF0668E1)),
                    Triple("DeepSeek", "R1, V3", Color(0xFF4D6BFE)),
                )

                items(count = builtIn.size) { i ->
                    val (name, models, color) = builtIn[i]
                    var enabled by remember { mutableStateOf(i % 2 == 0) }
                    CleanProviderRow(name = name, models = models, color = color, enabled = enabled, onToggle = { enabled = it }, onClick = {})
                }

                if (providers.isNotEmpty()) {
                    item {
                        Text("Custom", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Black.copy(alpha = 0.6f), modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                    }
                    items(count = providers.size, key = { idx -> providers[idx].id }) { idx ->
                        val p = providers[idx]
                        CleanProviderRow(name = p.name, models = p.models.joinToString(", ").take(30), color = Color(0xFF6C4DFF), enabled = p.enabled, onToggle = {}, onClick = { onEditProvider(p) })
                    }
                }
            }
        }
    }
}

@Composable
fun CleanProviderRow(name: String, models: String, color: Color, enabled: Boolean, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text(name.first().toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.Black)
                Text(models, fontSize = 12.sp, color = Color.Black.copy(alpha = 0.5f), maxLines = 1)
            }
            Switch(checked = enabled, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF6C4DFF), uncheckedTrackColor = Color(0xFFE5E5EA)))
        }
    }
}
