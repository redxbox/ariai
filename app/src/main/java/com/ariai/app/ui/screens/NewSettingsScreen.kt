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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSettingsScreen(
    onBack: () -> Unit,
    onProvidersClick: () -> Unit,
    currentTheme: String = "system",
    currentLanguage: String = "en",
    dynamicColor: Boolean = true,
    onThemeChange: (String) -> Unit = {},
    onLanguageChange: (String) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF5F7FF))) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Black)
                        }
                    },
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Settings", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text("Customize your AriAI experience.", color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    actions = { Spacer(modifier = Modifier.width(48.dp)) }
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
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().clickable { }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(0xFF6C4DFF), Color(0xFF00D4AA)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AriAI", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("A smarter you, every day.", color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                            }
                            Text("v1.0.0", color = Color.Black.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall)
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Black.copy(alpha = 0.3f), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }

                item { Text("Account", color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp)) }
                item {
                    SettingsGroupLight {
                        SettingsRowLight(icon = Icons.Default.Person, title = "Account", subtitle = "Manage your profile and subscription", onClick = {})
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRowLight(icon = Icons.Default.Star, title = "Subscription", subtitle = "Free Plan", onClick = {})
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRowWithSwitchLight(icon = Icons.Default.Sync, title = "Sync", subtitle = "Backup and sync your chats", checked = true, onCheckedChange = {}, onClick = {})
                    }
                }

                item { Text("AI & Models", color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp)) }
                item {
                    SettingsGroupLight {
                        SettingsRowLight(icon = Icons.Default.Storage, title = "Providers", subtitle = "Manage AI providers and API keys", onClick = onProvidersClick)
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRowLight(icon = Icons.Default.Tune, title = "Default Model", subtitle = "GPT-4o", onClick = {})
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRowLight(icon = Icons.Default.Settings, title = "Model Parameters", subtitle = "Temperature, max tokens, etc.", onClick = {})
                    }
                }

                item { Text("Chat", color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp)) }
                item {
                    SettingsGroupLight {
                        SettingsRowLight(icon = Icons.Default.Chat, title = "Chat Settings", subtitle = "Behavior, style, and response format", onClick = {})
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRowLight(icon = Icons.Default.Description, title = "Data & Storage", subtitle = "Manage your chat history", onClick = {})
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRowLight(icon = Icons.Default.Image, title = "Media Settings", subtitle = "Images, videos, and file handling", onClick = {})
                    }
                }

                item { Text("Appearance", color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp)) }
                item {
                    SettingsGroupLight {
                        SettingsRowLight(icon = Icons.Default.Palette, title = "Theme", subtitle = "System (Auto)", onClick = { onThemeChange(if (currentTheme == "light") "dark" else "light") })
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRowLight(icon = Icons.Default.Language, title = "Language", subtitle = "English", onClick = {})
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRowWithSwitchLight(icon = Icons.Default.ColorLens, title = "Use Dynamic Color", subtitle = "Match your device theme", checked = dynamicColor, onCheckedChange = onDynamicColorChange, onClick = {})
                    }
                }

                item { Text("Advanced", color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp)) }
                item {
                    SettingsGroupLight {
                        SettingsRowLight(icon = Icons.Default.Code, title = "Advanced Settings", subtitle = "Developer options", onClick = {})
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRowLight(icon = Icons.Default.Security, title = "Privacy & Security", subtitle = "Your data, your control", onClick = {})
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRowLight(icon = Icons.Default.Info, title = "About", subtitle = "Version, licenses, acknowledgments", onClick = {})
                    }
                }

                item {
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFFF6B6B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Out", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun SettingsGroupLight(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsRowLight(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF0F0FF)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color(0xFF6C4DFF), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.Black, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Black.copy(alpha = 0.2f), modifier = Modifier.size(20.dp))
    }
}

@Composable
fun SettingsRowWithSwitchLight(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var isChecked by remember { mutableStateOf(checked) }
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF0F0FF)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color(0xFF6C4DFF), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.Black, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = {
                isChecked = it
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF6C4DFF), uncheckedThumbColor = Color.White, uncheckedTrackColor = Color.Black.copy(alpha = 0.1f))
        )
    }
}
