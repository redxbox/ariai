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

// RikkaHub inspired settings - clean minimal grouped
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
                    title = { Text("Settings", fontWeight = FontWeight.SemiBold, color = Color.Black) }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Profile card - clean
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF6C4DFF)), contentAlignment = Alignment.Center) {
                                Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                            Column {
                                Text("AriAI", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.Black)
                                Text("Personal Workspace", color = Color.Black.copy(alpha = 0.5f), fontSize = 12.sp)
                            }
                        }
                    }
                }

                item {
                    SettingsGroup(title = "Appearance") {
                        // Theme selector - clean chips
                        Text("Theme", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Black, modifier = Modifier.padding(bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("light" to "Light", "dark" to "Dark", "system" to "System").forEach { (value, label) ->
                                val selected = currentTheme == value
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) Color(0xFF6C4DFF) else Color(0xFFF2F2F7),
                                    modifier = Modifier.weight(1f).clickable { onThemeChange(value) }
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            when (value) {
                                                "light" -> Icons.Default.WbSunny
                                                "dark" -> Icons.Default.NightsStay
                                                else -> Icons.Default.SettingsBrightness
                                            },
                                            contentDescription = null,
                                            tint = if (selected) Color.White else Color.Black.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(label, color = if (selected) Color.White else Color.Black, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Dynamic colors", fontSize = 13.sp, color = Color.Black)
                            Switch(checked = dynamicColor, onCheckedChange = onDynamicColorChange, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF6C4DFF)))
                        }
                    }
                }

                item {
                    SettingsGroup(title = "General") {
                        SettingsItem(icon = Icons.Default.Language, title = "Language", value = currentLanguage, onClick = { onLanguageChange(if (currentLanguage == "en") "fa" else "en") })
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                        SettingsItem(icon = Icons.Default.Storage, title = "Providers", value = "Manage", onClick = onProvidersClick)
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                        SettingsItem(icon = Icons.Default.Chat, title = "Chat", value = "", onClick = {})
                    }
                }

                item {
                    SettingsGroup(title = "Privacy & Data") {
                        SettingsItem(icon = Icons.Default.Lock, title = "Privacy", value = "", onClick = {})
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                        SettingsItem(icon = Icons.Default.Cloud, title = "Data & Sync", value = "", onClick = {})
                    }
                }

                item {
                    SettingsGroup(title = "About") {
                        SettingsItem(icon = Icons.Default.Info, title = "About AriAI", value = "v1.0", onClick = {})
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black.copy(alpha = 0.5f), modifier = Modifier.padding(start = 4.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF2F2F7)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.Black.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        }
        Text(title, modifier = Modifier.weight(1f), fontSize = 14.sp, color = Color.Black)
        if (value.isNotEmpty()) Text(value, fontSize = 12.sp, color = Color.Black.copy(alpha = 0.5f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Black.copy(alpha = 0.2f), modifier = Modifier.size(18.dp))
    }
}
