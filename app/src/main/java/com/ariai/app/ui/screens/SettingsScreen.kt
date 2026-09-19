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
import com.ariai.app.util.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentLanguage: String,
    currentTheme: String,
    dynamicColor: Boolean,
    searchKeys: Map<String, String>,
    onLanguageChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onSearchKeyChange: (String, String) -> Unit,
    onProvidersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Personal Workspace header - glass
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth().clickable { }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF6C4DFF), Color(0xFF00D4AA)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("A", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AriAI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Personal Workspace", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }

            // Appearance section - like image
            item {
                Text("Appearance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AppearanceOption(
                                title = "Light",
                                icon = Icons.Default.LightMode,
                                selected = currentTheme == "light",
                                onClick = { onThemeChange("light") },
                                modifier = Modifier.weight(1f)
                            )
                            AppearanceOption(
                                title = "Dark",
                                icon = Icons.Default.DarkMode,
                                selected = currentTheme == "dark",
                                onClick = { onThemeChange("dark") },
                                modifier = Modifier.weight(1f)
                            )
                            AppearanceOption(
                                title = "System",
                                icon = Icons.Default.SettingsBrightness,
                                selected = currentTheme == "system",
                                onClick = { onThemeChange("system") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Divider()
                        Text("Accent Color", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                Color(0xFF6C4DFF),
                                Color(0xFF2196F3),
                                Color(0xFFE91E63),
                                Color(0xFFFF9800),
                                Color(0xFF4CAF50)
                            ).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (color == Color(0xFF6C4DFF)) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Language, Chat, Privacy etc - glass list
            item {
                GlassSettingsGroup {
                    GlassSettingsItem(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = "English",
                        onClick = {}
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    GlassSettingsItem(
                        icon = Icons.Default.Chat,
                        title = "Chat",
                        onClick = {}
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    GlassSettingsItem(
                        icon = Icons.Default.Security,
                        title = "Privacy",
                        onClick = {}
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    GlassSettingsItem(
                        icon = Icons.Default.CloudSync,
                        title = "Data & Sync",
                        onClick = {}
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    GlassSettingsItem(
                        icon = Icons.Default.Settings,
                        title = "Advanced",
                        onClick = {}
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    GlassSettingsItem(
                        icon = Icons.Default.Info,
                        title = "About",
                        onClick = {}
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Small settings.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("A more personal AI.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Text("Premium", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                                Text("Glass Design", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun AppearanceOption(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GlassSettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
fun GlassSettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
