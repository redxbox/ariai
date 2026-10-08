package com.ariai.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType

private val Accent = Color(0xFF6C4DFF)
private val Ink = Color(0xFF1C1B1F)

fun providerTypeLabel(type: ProviderType): String = when (type) {
    ProviderType.OPENAI -> "OpenAI"
    ProviderType.GEMINI -> "Google Gemini"
    ProviderType.ANTHROPIC -> "Anthropic"
    ProviderType.OPENAI_COMPATIBLE -> "OpenAI compatible"
    ProviderType.OLLAMA -> "Ollama (local)"
    ProviderType.CUSTOM -> "Custom"
}

private fun providerColors(type: ProviderType): Pair<Color, Color> = when (type) {
    ProviderType.OPENAI -> Color(0xFF10A37F) to Color(0xFF6BD7B5)
    ProviderType.GEMINI -> Color(0xFF4285F4) to Color(0xFF8AB4F8)
    ProviderType.ANTHROPIC -> Color(0xFFC96442) to Color(0xFFE8A07F)
    ProviderType.OLLAMA -> Color(0xFF3A3A3C) to Color(0xFF8E8E93)
    ProviderType.OPENAI_COMPATIBLE -> Color(0xFF6C4DFF) to Color(0xFFB7A6FF)
    ProviderType.CUSTOM -> Color(0xFF7D5260) to Color(0xFFD7A9BD)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProvidersScreen(
    providers: List<Provider>,
    onAddProvider: () -> Unit,
    onEditProvider: (Provider) -> Unit,
    onBack: () -> Unit,
    onToggleProvider: (Provider) -> Unit = {}
) {
    var filter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Cloud", "Local", "Custom")
    val visible = providers.filter {
        when (filter) {
            "Cloud" -> it.type != ProviderType.OLLAMA && it.type != ProviderType.CUSTOM
            "Local" -> it.type == ProviderType.OLLAMA
            "Custom" -> it.type == ProviderType.CUSTOM || it.type == ProviderType.OPENAI_COMPATIBLE
            else -> true
        }
    }
    val enabledCount = providers.count { it.enabled }
    val modelCount = providers.sumOf { it.models.size }

    Scaffold(
        containerColor = Color(0xFFF7F6FB),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF7F6FB)),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Ink) } },
                title = { Text("Providers", fontWeight = FontWeight.SemiBold, color = Ink) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProvider,
                containerColor = Accent,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add provider", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Hero summary
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(Accent, Color(0xFF9C7CFF), Color(0xFF4FC3F7))))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Connected providers", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            HeroStat(providers.size.toString(), "Total")
                            HeroStat(enabledCount.toString(), "Active")
                            HeroStat(modelCount.toString(), "Models")
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    filters.forEach { f ->
                        val selected = filter == f
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) Accent else Color.White,
                            shadowElevation = if (selected) 0.dp else 1.dp,
                            modifier = Modifier.clickable { filter = f }
                        ) {
                            Text(
                                f,
                                color = if (selected) Color.White else Ink.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            if (visible.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = Accent, modifier = Modifier.size(28.dp))
                        }
                        Text(if (providers.isEmpty()) "No providers yet" else "Nothing in this filter", fontWeight = FontWeight.SemiBold, color = Ink)
                        Text("Add an API endpoint to start chatting.", color = Ink.copy(alpha = 0.5f), fontSize = 13.sp)
                    }
                }
            }

            itemsIndexed(visible, key = { index, p -> "${p.id}_$index" }) { _, provider ->
                ProviderCard(
                    provider = provider,
                    onClick = { onEditProvider(provider) },
                    onToggle = { onToggleProvider(provider) }
                )
            }
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String) {
    Column {
        Text(value, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}

@Composable
private fun ProviderCard(provider: Provider, onClick: () -> Unit, onToggle: () -> Unit) {
    val (c1, c2) = providerColors(provider.type)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(c1, c2))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(provider.name.firstOrNull()?.uppercase() ?: "P", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(provider.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(providerTypeLabel(provider.type), fontSize = 12.sp, color = Ink.copy(alpha = 0.5f), maxLines = 1)
                }
                Switch(
                    checked = provider.enabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(checkedTrackColor = Accent)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                val shown = provider.models.take(3)
                shown.forEach { m ->
                    Surface(shape = RoundedCornerShape(10.dp), color = c1.copy(alpha = 0.10f)) {
                        Text(m.displayName, color = c1, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                val extra = provider.models.size - shown.size
                if (extra > 0) {
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF2F2F7)) {
                        Text("+$extra", color = Ink.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                if (provider.models.isEmpty()) {
                    Text("No models added", color = Ink.copy(alpha = 0.4f), fontSize = 12.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (provider.enabled) Color(0xFF34C759) else Color(0xFFAEAEB2)))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (provider.enabled) "Active" else "Disabled",
                    fontSize = 11.sp,
                    color = Ink.copy(alpha = 0.55f)
                )
                Spacer(Modifier.weight(1f))
                Text(provider.baseUrl.removePrefix("https://").removePrefix("http://").take(28), fontSize = 11.sp, color = Ink.copy(alpha = 0.4f), maxLines = 1)
            }
        }
    }
}
