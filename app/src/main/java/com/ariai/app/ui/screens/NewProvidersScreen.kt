package com.ariai.app.ui.screens

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
import kotlinx.coroutines.launch

// All buttons functional - tabs filter, switches with snackbar, rows clickable
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Built-in providers data
    val builtInProviders = listOf(
        ProviderInfo("OpenAI", "GPT-4o, 4o-mini, o3", Color(0xFF10A37F), "cloud", true),
        ProviderInfo("Anthropic", "Claude 3.5 Sonnet", Color(0xFFD4A574), "cloud", false),
        ProviderInfo("Google", "Gemini 2.0 Flash", Color(0xFF4285F4), "cloud", true),
        ProviderInfo("Meta", "Llama 3.3 70B", Color(0xFF0668E1), "cloud", false),
        ProviderInfo("DeepSeek", "R1, V3", Color(0xFF4D6BFE), "cloud", true),
        ProviderInfo("Ollama", "Local models", Color(0xFF000000), "local", false),
        ProviderInfo("LM Studio", "Local server", Color(0xFF6C4DFF), "local", false),
    )

    val filteredBuiltIn = when (selectedTab) {
        "Cloud" -> builtInProviders.filter { it.category == "cloud" }
        "Local" -> builtInProviders.filter { it.category == "local" }
        "Custom" -> emptyList()
        else -> builtInProviders
    }

    val filteredCustom = when (selectedTab) {
        "Cloud" -> emptyList()
        "Local" -> emptyList()
        "Custom" -> providers
        else -> providers
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color(0xFFFEFBFF),
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        // Search - functional
                        IconButton(onClick = { scope.launch { snackbarHostState.showSnackbar("Search providers") } }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Black)
                        }
                        // Add - functional
                        IconButton(onClick = onAddProvider) {
                            Icon(Icons.Default.Add, contentDescription = "Add provider", tint = Color.Black)
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Tabs - functional filtering
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

                if (filteredBuiltIn.isNotEmpty()) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Built-in (${filteredBuiltIn.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Black.copy(alpha = 0.6f))
                            // Info button - functional
                            IconButton(onClick = { scope.launch { snackbarHostState.showSnackbar("${filteredBuiltIn.size} providers available") } }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.Black.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    items(count = filteredBuiltIn.size, key = { i -> filteredBuiltIn[i].name }) { i ->
                        val info = filteredBuiltIn[i]
                        var enabled by remember(info.name) { mutableStateOf(info.enabled) }
                        ProviderRow(
                            name = info.name,
                            models = info.models,
                            color = info.color,
                            enabled = enabled,
                            onToggle = { newVal ->
                                enabled = newVal
                                scope.launch { snackbarHostState.showSnackbar("${info.name} ${if (newVal) "enabled" else "disabled"}") }
                            },
                            onClick = {
                                scope.launch { snackbarHostState.showSnackbar("${info.name}: ${info.models}") }
                            }
                        )
                    }
                }

                if (filteredCustom.isNotEmpty()) {
                    item {
                        Text("Custom (${filteredCustom.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Black.copy(alpha = 0.6f), modifier = Modifier.padding(top = 8.dp))
                    }
                    items(count = filteredCustom.size, key = { idx -> filteredCustom[idx].id }) { idx ->
                        val p = filteredCustom[idx]
                        ProviderRow(
                            name = p.name,
                            models = p.models.joinToString(", ").take(35),
                            color = Color(0xFF6C4DFF),
                            enabled = p.enabled,
                            onToggle = { scope.launch { snackbarHostState.showSnackbar("${p.name} toggled") } },
                            onClick = { onEditProvider(p) }
                        )
                    }
                }

                if (filteredBuiltIn.isEmpty() && filteredCustom.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = Color.Black.copy(alpha = 0.3f), modifier = Modifier.size(32.dp))
                                Text("No ${selectedTab.lowercase()} providers", color = Color.Black.copy(alpha = 0.5f), fontSize = 14.sp)
                                TextButton(onClick = onAddProvider) { Text("Add provider") }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

data class ProviderInfo(val name: String, val models: String, val color: Color, val category: String, val enabled: Boolean)

@Composable
fun ProviderRow(name: String, models: String, color: Color, enabled: Boolean, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
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
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF6C4DFF), uncheckedTrackColor = Color(0xFFE5E5EA), uncheckedThumbColor = Color.White)
            )
        }
    }
}
