package com.ariai.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var showLangMenu by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var selectedSearchProvider by remember { mutableStateOf("tavily") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(strings.settings) })
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(title = strings.general)
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = strings.settingsLanguage,
                    subtitle = when (currentLanguage) {
                        "fa" -> strings.persian
                        "ar" -> strings.arabic
                        "tr" -> strings.turkish
                        "de" -> strings.german
                        "fr" -> strings.french
                        else -> strings.english
                    },
                    onClick = { showLangMenu = true }
                )
                DropdownMenu(
                    expanded = showLangMenu,
                    onDismissRequest = { showLangMenu = false }
                ) {
                    DropdownMenuItem(text = { Text("English 🇺🇸") }, onClick = { onLanguageChange("en"); showLangMenu = false })
                    DropdownMenuItem(text = { Text("فارسی 🇮🇷") }, onClick = { onLanguageChange("fa"); showLangMenu = false })
                    DropdownMenuItem(text = { Text("العربية 🇸🇦") }, onClick = { onLanguageChange("ar"); showLangMenu = false })
                    DropdownMenuItem(text = { Text("Türkçe 🇹🇷") }, onClick = { onLanguageChange("tr"); showLangMenu = false })
                    DropdownMenuItem(text = { Text("Deutsch 🇩🇪") }, onClick = { onLanguageChange("de"); showLangMenu = false })
                    DropdownMenuItem(text = { Text("Français 🇫🇷") }, onClick = { onLanguageChange("fr"); showLangMenu = false })
                }
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = strings.settingsTheme,
                    subtitle = when (currentTheme) {
                        "light" -> strings.light
                        "dark" -> strings.dark
                        else -> strings.system
                    },
                    onClick = { showThemeMenu = true }
                )
                DropdownMenu(
                    expanded = showThemeMenu,
                    onDismissRequest = { showThemeMenu = false }
                ) {
                    DropdownMenuItem(text = { Text(strings.light) }, onClick = { onThemeChange("light"); showThemeMenu = false })
                    DropdownMenuItem(text = { Text(strings.dark) }, onClick = { onThemeChange("dark"); showThemeMenu = false })
                    DropdownMenuItem(text = { Text(strings.system) }, onClick = { onThemeChange("system"); showThemeMenu = false })
                }
            }
            item {
                SettingsItemWithSwitch(
                    icon = Icons.Default.ColorLens,
                    title = "Dynamic Color (Material You)",
                    subtitle = "Use wallpaper colors",
                    checked = dynamicColor,
                    onCheckedChange = onDynamicColorChange
                )
            }
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSection(title = "AI Providers")
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Cloud,
                    title = strings.providers,
                    subtitle = "Manage OpenAI, Gemini, Claude, etc.",
                    onClick = onProvidersClick
                )
            }
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSection(title = strings.webSearch)
            }
            item {
                SearchProviderItem(
                    name = "Tavily",
                    key = searchKeys["tavily"] ?: "",
                    enabled = (searchKeys["tavily"] ?: "").isNotBlank(),
                    onClick = { selectedSearchProvider = "tavily"; showSearchDialog = true }
                )
            }
            item {
                SearchProviderItem(
                    name = "Brave Search",
                    key = searchKeys["brave"] ?: "",
                    enabled = (searchKeys["brave"] ?: "").isNotBlank(),
                    onClick = { selectedSearchProvider = "brave"; showSearchDialog = true }
                )
            }
            item {
                SearchProviderItem(
                    name = "Exa",
                    key = searchKeys["exa"] ?: "",
                    enabled = (searchKeys["exa"] ?: "").isNotBlank(),
                    onClick = { selectedSearchProvider = "exa"; showSearchDialog = true }
                )
            }
            item {
                SearchProviderItem(
                    name = "Serper (Google)",
                    key = searchKeys["serper"] ?: "",
                    enabled = (searchKeys["serper"] ?: "").isNotBlank(),
                    onClick = { selectedSearchProvider = "serper"; showSearchDialog = true }
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🔍 Real Web Search", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Add API keys to enable real-time web search like RikkaHub. Get free keys from:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• tavily.com (1000 free searches)", style = MaterialTheme.typography.labelSmall)
                        Text("• brave.com/search/api (2000 free)", style = MaterialTheme.typography.labelSmall)
                        Text("• exa.ai (1000 free)", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSection(title = strings.about)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🤖", style = MaterialTheme.typography.displayMedium)
                        Text("AriAi", style = MaterialTheme.typography.titleLarge)
                        Text("v2.0.0 - RikkaHub Edition", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            strings.rikkaHubInspired,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Features: Multi-provider, Web Search, Image Gen, Agents, Memory, Branching, Markdown, 6 Languages, Material You",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }

    if (showSearchDialog) {
        var tempKey by remember { mutableStateOf(searchKeys[selectedSearchProvider] ?: "") }
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("Set ${selectedSearchProvider.uppercase()} API Key") },
            text = {
                OutlinedTextField(
                    value = tempKey,
                    onValueChange = { tempKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSearchKeyChange(selectedSearchProvider, tempKey)
                    showSearchDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun SettingsItemWithSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchProviderItem(
    name: String,
    key: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (enabled) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (enabled) "Configured • ${key.take(8)}..." else "Not configured - tap to add",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
