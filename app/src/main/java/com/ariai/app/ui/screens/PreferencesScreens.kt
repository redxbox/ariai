package com.ariai.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Provider

private val Accent = Color(0xFF6C4DFF)
private val Ink = Color(0xFF1C1B1F)
private val Surface0 = Color(0xFFF7F6FB)

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink.copy(alpha = 0.5f), modifier = Modifier.padding(start = 6.dp, top = 8.dp))
}

@Composable
private fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, color = Ink, fontSize = 15.sp)
            Text(subtitle, fontSize = 12.sp, color = Ink.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = Accent))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    theme: String,
    onThemeChange: (String) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    showReasoning: Boolean,
    onReasoningChange: (Boolean) -> Unit,
    fontSize: Int,
    onFontSizeChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = Surface0,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface0),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Ink) } },
                title = { Text("Preferences", fontWeight = FontWeight.SemiBold, color = Ink) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionTitle("Appearance") }
            item {
                SettingCard {
                    Text("Theme", fontWeight = FontWeight.Medium, color = Ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
                            FilterChip(selected = theme == key, onClick = { onThemeChange(key) }, label = { Text(label) }, modifier = Modifier.weight(1f))
                        }
                    }
                    SwitchRow("Dynamic colors", "Use wallpaper-based palette", dynamicColor, onDynamicColorChange)
                }
            }

            item { SectionTitle("Chat") }
            item {
                SettingCard {
                    SwitchRow("Show reasoning", "Display the model's reasoning steps", showReasoning, onReasoningChange)
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))
                    Text("Message text size", fontWeight = FontWeight.Medium, color = Ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(13 to "Small", 15 to "Medium", 17 to "Large").forEach { (size, label) ->
                            FilterChip(selected = fontSize == size, onClick = { onFontSizeChange(size) }, label = { Text(label) }, modifier = Modifier.weight(1f))
                        }
                    }
                    Text("Preview: the quick brown fox jumps over the lazy dog.", fontSize = fontSize.sp, color = Ink.copy(alpha = 0.7f))
                }
            }

            item { SectionTitle("Language") }
            item {
                SettingCard {
                    Text("English", fontWeight = FontWeight.Medium, color = Ink)
                    Text("AriAI currently ships in English only.", fontSize = 12.sp, color = Ink.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultModelScreen(
    providers: List<Provider>,
    defaultModelId: String?,
    onSelect: (providerId: String, modelId: String) -> Unit,
    onAddProvider: () -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val groups = providers
        .filter { it.enabled }
        .map { p -> p to p.models.filter { query.isBlank() || it.displayName.contains(query, true) || it.id.contains(query, true) } }
        .filter { it.second.isNotEmpty() }

    Scaffold(
        containerColor = Surface0,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface0),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Ink) } },
                title = { Text("Default model", fontWeight = FontWeight.SemiBold, color = Ink) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("New chats start with this model.", fontSize = 13.sp, color = Ink.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 4.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search models") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (groups.isEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("No models yet", fontWeight = FontWeight.SemiBold, color = Ink)
                        Button(onClick = onAddProvider, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Add provider") }
                    }
                }
            }
            groups.forEach { (provider, models) ->
                item(key = "h_${provider.id}") {
                    Text(provider.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink.copy(alpha = 0.5f), modifier = Modifier.padding(top = 10.dp, start = 6.dp))
                }
                itemsIndexed(models, key = { i, m -> "${provider.id}_${m.id}_$i" }) { _, model ->
                    val selected = model.id == defaultModelId
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) Accent.copy(alpha = 0.10f) else Color.White,
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(provider.id, model.id) }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(model.displayName, fontWeight = FontWeight.Medium, color = if (selected) Accent else Ink)
                                Text(model.id, fontSize = 11.sp, color = Ink.copy(alpha = 0.45f))
                            }
                            RadioButton(selected = selected, onClick = { onSelect(provider.id, model.id) }, colors = RadioButtonDefaults.colors(selectedColor = Accent))
                        }
                    }
                }
            }
        }
    }
}
