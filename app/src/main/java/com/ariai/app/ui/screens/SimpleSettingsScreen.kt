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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// Simple clean settings - only essential options
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSettingsScreen(
    onBack: () -> Unit,
    onProvidersClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onSearchServiceClick: () -> Unit = {},
    onStorageClick: () -> Unit = {},
    onDefaultModelClick: () -> Unit = {},
    onPreferencesClick: () -> Unit = {},
    defaultModelName: String = "Not chosen",
    onClearAll: () -> Unit = {},
    onMcpClick: () -> Unit = {},
    currentTheme: String,
    currentLanguage: String,
    dynamicColor: Boolean,
    onThemeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize().background(AriPaper)) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AriCard),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                        }
                    },
                    title = { Text("Settings", fontWeight = FontWeight.SemiBold, color = Color.Black) }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SimpleGroup(title = "General") {
                        Text("Theme", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Black, modifier = Modifier.padding(bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SimpleChip(label = "Light", selected = currentTheme == "light", onClick = { onThemeChange("light") }, modifier = Modifier.weight(1f))
                            SimpleChip(label = "Dark", selected = currentTheme == "dark", onClick = { onThemeChange("dark") }, modifier = Modifier.weight(1f))
                            SimpleChip(label = "System", selected = currentTheme == "system", onClick = { onThemeChange("system") }, modifier = Modifier.weight(1f))
                        }
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                        SimpleRow(icon = Icons.Default.Settings, title = "Preferences", subtitle = "Chat options, text size, theme", onClick = onPreferencesClick)
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                        SimpleRow(icon = Icons.Default.Language, title = "Language", subtitle = if (currentLanguage == "en") "English" else currentLanguage, onClick = {
                            val newLang = if (currentLanguage == "en") "fa" else "en"
                            onLanguageChange(newLang)
                        })
                    }
                }

                item {
                    SimpleGroup(title = "Models & services") {
                        SimpleRow(icon = Icons.Default.Star, title = "Default model", subtitle = defaultModelName, onClick = onDefaultModelClick)
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                        SimpleRow(icon = Icons.Default.Storage, title = "Providers", subtitle = "OpenAI, Gemini, Claude and more", onClick = onProvidersClick)
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                        SimpleRow(icon = Icons.Default.Search, title = "Search service", subtitle = "Web search for answers", onClick = onSearchServiceClick)
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                        SimpleRow(icon = Icons.Default.Settings, title = "MCP servers", subtitle = "Connect local tools", onClick = onMcpClick)
                    }
                }

                item {
                    SimpleGroup(title = "Data") {
                        SimpleRow(icon = Icons.Default.Storage, title = "Data & storage", subtitle = "Export list, storage overview", onClick = onStorageClick)
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                        SimpleRow(icon = Icons.Default.Delete, title = "Delete all chats", subtitle = "Remove every conversation", onClick = onClearAll)
                    }
                }

                item {
                    SimpleGroup(title = "About") {
                        SimpleRow(icon = Icons.Default.Info, title = "About AriAI", subtitle = "Version 1.0", onClick = {
                            scope.launch { snackbarHostState.showSnackbar("AriAI v1.0 - Personal AI") }
                        })
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun SimpleGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Black.copy(alpha = 0.5f), modifier = Modifier.padding(start = 4.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AriCard), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SimpleChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) AriInk else AriTint,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) AriCard else Color.Black, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun SimpleRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(AriTint), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.Black.copy(alpha = 0.65f), modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 11.sp, color = Color.Black.copy(alpha = 0.5f))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Black.copy(alpha = 0.2f), modifier = Modifier.size(18.dp))
    }
}
