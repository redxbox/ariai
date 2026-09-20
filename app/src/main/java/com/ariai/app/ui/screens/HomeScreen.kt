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
import androidx.compose.ui.unit.sp

// Ultra clean light glass premium - exact like image 1
@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToProviders: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNewChat: (String) -> Unit, // Now takes prompt for unique actions
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFF0F4FF), Color(0xFFF8FAFF), Color.White))
        )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row {
                            Text("Ari", color = Color(0xFF1A1A2E), fontWeight = FontWeight.Bold, fontSize = 28.sp)
                            Text("AI", color = Color(0xFF6C4DFF), fontWeight = FontWeight.Bold, fontSize = 28.sp)
                        }
                        Text("Ideas flow differently here.", color = Color(0xFF1A1A2E).copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
                    }
                    Surface(shape = CircleShape, color = Color.White, shadowElevation = 3.dp, modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("A", color = Color(0xFF6C4DFF), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Good morning", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color(0xFF1A1A2E))
                    Text("What shall we explore today?", color = Color(0xFF1A1A2E).copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                }
            }

            item {
                // Ask anything - functional: opens chat with empty
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onNewChat("") }
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text("Ask anything...", color = Color.Black.copy(alpha = 0.35f), fontSize = 16.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = Color.Black.copy(alpha = 0.4f), modifier = Modifier.size(20.dp).clickable { onNewChat("Attach file") })
                                Icon(Icons.Default.Language, contentDescription = "Web", tint = Color.Black.copy(alpha = 0.4f), modifier = Modifier.size(20.dp).clickable { onNewChat("Search web") })
                                Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color.Black.copy(alpha = 0.4f), modifier = Modifier.size(20.dp).clickable { onNewChat("Voice input") })
                            }
                            Surface(shape = CircleShape, color = Color(0xFF6C4DFF), modifier = Modifier.size(40.dp).clickable { onNewChat("") }) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                // 3 cards with UNIQUE actions - not same
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    HomeActionCard(title = "Explain", desc = "Make complex\nthings simple", icon = "💡", bg = Color(0xFFF3F0FF), modifier = Modifier.weight(1f), onClick = { onNewChat("Explain quantum computing simply") })
                    HomeActionCardPrimary(modifier = Modifier.weight(1f), onClick = { onNewChat("Write a blog post about AI") })
                    HomeActionCard(title = "Code", desc = "Build, debug,\ncreate", icon = "💻", bg = Color(0xFFE8F5E9), modifier = Modifier.weight(1f), onClick = { onNewChat("Write a Python function to sort list") })
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    HomeSmallCard(title = "Quick Chat", subtitle = "Start new chat", icon = "⚡", onClick = { onNewChat("") }, modifier = Modifier.weight(1f))
                    HomeSmallCard(title = "Choose Model", subtitle = "GPT-4o, Claude, Gemini", icon = "🤖", onClick = onNavigateToProviders, modifier = Modifier.weight(1f))
                    HomeSmallCard(title = "Explore Tools", subtitle = "Image, Code, Search", icon = "🛠️", onClick = onNavigateToTools, modifier = Modifier.weight(1f))
                }
            }

            item {
                // Functional shortcuts - each unique
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Shortcuts", fontWeight = FontWeight.Bold, color = Color.Black, style = MaterialTheme.typography.titleSmall)
                        ShortcutRow(icon = Icons.Default.Chat, title = "Chat History", subtitle = "${0} conversations", onClick = onNavigateToChat)
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                        ShortcutRow(icon = Icons.Default.Image, title = "Create Images", subtitle = "DALL·E 3, Imagen", onClick = { onNavigateToTools() })
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                        ShortcutRow(icon = Icons.Default.Search, title = "Web Search", subtitle = "Real-time info", onClick = { onNewChat("Search latest AI news") })
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                        ShortcutRow(icon = Icons.Default.Settings, title = "Customize", subtitle = "Theme, Language", onClick = onNavigateToSettings)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun HomeActionCard(title: String, desc: String, icon: String, bg: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.height(120.dp).clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(12.dp), color = bg, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Text(icon, fontSize = 22.sp) }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 13.sp, maxLines = 1)
                Text(desc, color = Color.Black.copy(alpha = 0.5f), fontSize = 10.sp, lineHeight = 11.sp, maxLines = 2)
            }
        }
    }
}

@Composable
fun HomeActionCardPrimary(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF6C4DFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.height(120.dp).clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Write", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Text("Turn ideas\ninto content", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, lineHeight = 11.sp, maxLines = 2)
            }
        }
    }
}

@Composable
fun HomeSmallCard(title: String, subtitle: String, icon: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(icon, fontSize = 18.sp)
            Text(title, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp, maxLines = 1)
            Text(subtitle, color = Color.Black.copy(alpha = 0.5f), fontSize = 10.sp, lineHeight = 11.sp, maxLines = 2)
        }
    }
}

@Composable
fun ShortcutRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF0F0FF), modifier = Modifier.size(36.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, contentDescription = null, tint = Color(0xFF6C4DFF), modifier = Modifier.size(18.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.Black, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text(subtitle, color = Color.Black.copy(alpha = 0.5f), fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Black.copy(alpha = 0.2f), modifier = Modifier.size(18.dp))
    }
}
