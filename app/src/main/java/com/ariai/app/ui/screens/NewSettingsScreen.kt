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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSettingsScreen(
    onBack: () -> Unit,
    onProvidersClick: () -> Unit,
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

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFFEFBFF))) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                        }
                    },
                    title = { Text("Settings", fontWeight = FontWeight.SemiBold, color = Color.Black) },
                    actions = {
                        IconButton(onClick = { scope.launch { snackbarHostState.showSnackbar("Search settings") } }) {
                            Icon(Icons.Default.Search, contentDescription = "Search settings", tint = Color.Black)
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth().clickable { scope.launch { snackbarHostState.showSnackbar("Personal Workspace: AriAI") } }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF6C4DFF)), contentAlignment = Alignment.Center) {
                                Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AriAI", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.Black)
                                Text("Personal Workspace", color = Color.Black.copy(alpha = 0.5f), fontSize = 12.sp)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Black.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                item {
                    SettingsGroup(title = "Appearance") {
                        Text("Theme", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Black, modifier = Modifier.padding(bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ThemeChip(value = "light", label = "Light", icon = Icons.Default.WbSunny, selected = currentTheme == "light", onClick = { onThemeChange("light") }, modifier = Modifier.weight(1f))
                            ThemeChip(value = "dark", label = "Dark", icon = Icons.Default.NightsStay, selected = currentTheme == "dark", onClick = { onThemeChange("dark") }, modifier = Modifier.weight(1f))
                            ThemeChip(value = "system", label = "System", icon = Icons.Default.Settings, selected = currentTheme == "system", onClick = { onThemeChange("system") }, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Dynamic colors", fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                                Text("Use system colors", fontSize = 11.sp, color = Color.Black.copy(alpha = 0.5f))
                            }
                            Switch(checked = dynamicColor, onCheckedChange = onDynamicColorChange, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF6C4DFF)))
                        }
                    }
                }

                item {
                    SettingsGroup(title = "General") {
                        SettingsItem(icon = Icons.Default.Language, title = "Language", value = if (currentLanguage == "en") "English" else currentLanguage, onClick = {
                            val newLang = if (currentLanguage == "en") "fa" else "en"
                            onLanguageChange(newLang)
                            scope.launch { snackbarHostState.showSnackbar("Language: $newLang") }
                        })
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                        SettingsItem(icon = Icons.Default.Storage, title = "Providers", value = "Manage", onClick = onProvidersClick)
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                        SettingsItem(icon = Icons.Default.Chat, title = "Chat Settings", value = "", onClick = { scope.launch { snackbarHostState.showSnackbar("Chat: font size, bubbles") } })
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                        SettingsItem(icon = Icons.Default.Notifications, title = "Notifications", value = "", onClick = { scope.launch { snackbarHostState.showSnackbar("Notifications settings") } })
                    }
                }

                item {
                    SettingsGroup(title = "Privacy & Data") {
                        SettingsItem(icon = Icons.Default.Lock, title = "Privacy", value = "", onClick = { scope.launch { snackbarHostState.showSnackbar("Privacy: local storage only") } })
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                        SettingsItem(icon = Icons.Default.Cloud, title = "Data & Sync", value = "", onClick = { scope.launch { snackbarHostState.showSnackbar("Data: backup & sync") } })
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                        SettingsItem(icon = Icons.Default.Delete, title = "Clear Data", value = "", onClick = { scope.launch { snackbarHostState.showSnackbar("Clear data: chats, cache") } })
                    }
                }

                item {
                    SettingsGroup(title = "Advanced") {
                        SettingsItem(icon = Icons.Default.Code, title = "Developer", value = "", onClick = { scope.launch { snackbarHostState.showSnackbar("Developer options") } })
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                        SettingsItem(icon = Icons.Default.BugReport, title = "Debug Logs", value = "", onClick = { scope.launch { snackbarHostState.showSnackbar("Logs: export debug logs") } })
                    }
                }

                item {
                    SettingsGroup(title = "About") {
                        SettingsItem(icon = Icons.Default.Info, title = "About AriAI", value = "v1.0", onClick = { scope.launch { snackbarHostState.showSnackbar("AriAI v1.0 - Personal AI Assistant") } })
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                        SettingsItem(icon = Icons.Default.Star, title = "Rate App", value = "", onClick = { scope.launch { snackbarHostState.showSnackbar("Rate on Play Store") } })
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                        SettingsItem(icon = Icons.Default.Share, title = "Share App", value = "", onClick = { scope.launch { snackbarHostState.showSnackbar("Share AriAI") } })
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
fun ThemeChip(value: String, label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFF6C4DFF) else Color(0xFFF2F2F7),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = label, tint = if (selected) Color.White else Color.Black.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            Text(label, color = if (selected) Color.White else Color.Black, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black.copy(alpha = 0.5f), modifier = Modifier.padding(start = 4.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF2F2F7)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.Black.copy(alpha = 0.65f), modifier = Modifier.size(18.dp))
        }
        Text(title, modifier = Modifier.weight(1f), fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Medium)
        if (value.isNotEmpty()) Text(value, fontSize = 12.sp, color = Color.Black.copy(alpha = 0.5f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Black.copy(alpha = 0.25f), modifier = Modifier.size(18.dp))
    }
}
