package com.ariai.app.ui.screens

import androidx.compose.foundation.background
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
    var showLangMenu by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var selectedSearchProvider by remember { mutableStateOf("tavily") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(strings.settings, fontWeight = FontWeight.Bold)
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    title = "Dynamic Color",
                    subtitle = "Use wallpaper colors (Android 12+)",
                    checked = dynamicColor,
                    onCheckedChange = onDynamicColorChange
                )
            }
            item {
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                SettingsSection(title = "AI Providers")
            }
            item {
                Card(
                    onClick = onProvidersClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF6C4DFF), Color(0xFF00D4AA))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.providers, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Manage OpenAI, Gemini, Groq, Free demos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
            item {
                Divider(modifier = Modifier.padding(vertical = 4.dp))
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
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Real Web Search", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Add API keys to enable real-time web search. AI will use live results to answer.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• tavily.com (1000 free)", style = MaterialTheme.typography.labelSmall)
                        Text("• brave.com/search/api (2000 free)", style = MaterialTheme.typography.labelSmall)
                        Text("• exa.ai (1000 free)", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            item {
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                SettingsSection(title = strings.about)
            }
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF6C4DFF).copy(alpha = 0.1f),
                                        Color(0xFF00D4AA).copy(alpha = 0.1f)
                                    )
                                )
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF6C4DFF), Color(0xFF00D4AA))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("AriAi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("v2.1.0 • Premium Edition", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                strings.tagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text("Multi-Provider", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text("Web Search", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                                    Text("6 Languages", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Free demo: Pollinations, LLM7, Groq, Gemini, OpenRouter, Cerebras",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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
        fontWeight = FontWeight.Bold,
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (enabled) Color(0xFF00C853).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        if (enabled) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (enabled) Color(0xFF00C853) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    text = if (enabled) "Configured • ${key.take(8)}..." else "Not configured - tap to add",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
