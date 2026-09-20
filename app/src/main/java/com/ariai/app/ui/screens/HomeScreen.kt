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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToProviders: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNewChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(Color(0xFFFEFBFF))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Text("Good morning", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black, lineHeight = 32.sp)
                    Text("How can I help you today?", fontSize = 15.sp, color = Color.Black.copy(alpha = 0.55f))
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().clickable { onNewChat("") }) {
                            Text("Ask anything...", color = Color.Black.copy(alpha = 0.35f), fontSize = 16.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { onNewChat("I want to upload a file") }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.AttachFile, contentDescription = "Attach file", tint = Color.Black.copy(alpha = 0.55f), modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { onNewChat("Search the web for latest AI news") }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Language, contentDescription = "Web search", tint = Color.Black.copy(alpha = 0.55f), modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { onNewChat("Voice input mode") }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color.Black.copy(alpha = 0.55f), modifier = Modifier.size(20.dp))
                                }
                            }
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF6C4DFF)).clickable { onNewChat("") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "New chat", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            item {
                Text("Quick actions", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Black.copy(alpha = 0.6f))
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionCard(title = "Explain", subtitle = "Simplify topics", icon = Icons.Default.Info, color = Color(0xFF6750A4), modifier = Modifier.weight(1f), onClick = { onNewChat("Explain quantum computing in simple terms") })
                    ActionCard(title = "Write", subtitle = "Draft content", icon = Icons.Default.Edit, color = Color(0xFF6C4DFF), isPrimary = true, modifier = Modifier.weight(1f), onClick = { onNewChat("Help me write a professional email about project update") })
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionCard(title = "Code", subtitle = "Build & debug", icon = Icons.Default.Code, color = Color(0xFF006A60), modifier = Modifier.weight(1f), onClick = { onNewChat("Write a Python function to sort a list efficiently") })
                    ActionCard(title = "Analyze", subtitle = "Review data", icon = Icons.Default.Search, color = Color(0xFF904D00), modifier = Modifier.weight(1f), onClick = { onNewChat("Analyze this data and provide key insights") })
                }
            }

            item {
                Text("Explore", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Black.copy(alpha = 0.6f), modifier = Modifier.padding(top = 4.dp))
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MiniCard(icon = Icons.Default.Chat, title = "New Chat", subtitle = "Start conversation", onClick = { onNewChat("") }, modifier = Modifier.weight(1f))
                    MiniCard(icon = Icons.Default.Image, title = "Images", subtitle = "Generate art", onClick = onNavigateToTools, modifier = Modifier.weight(1f))
                    MiniCard(icon = Icons.Default.Storage, title = "Models", subtitle = "GPT-4o, Claude", onClick = onNavigateToProviders, modifier = Modifier.weight(1f))
                    MiniCard(icon = Icons.Default.Settings, title = "Settings", subtitle = "Preferences", onClick = onNavigateToSettings, modifier = Modifier.weight(1f))
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun ActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier, isPrimary: Boolean = false, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isPrimary) Color(0xFF6C4DFF) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPrimary) 0.dp else 1.dp),
        modifier = modifier.height(108.dp).clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(if (isPrimary) Color.White.copy(alpha = 0.2f) else color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = if (isPrimary) Color.White else color, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (isPrimary) Color.White else Color.Black)
                Text(subtitle, fontSize = 11.sp, color = if (isPrimary) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.5f), maxLines = 1)
            }
        }
    }
}

@Composable
fun MiniCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFF3F0FF)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color(0xFF6C4DFF), modifier = Modifier.size(18.dp))
            }
            Column {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black, maxLines = 1)
                Text(subtitle, fontSize = 10.sp, color = Color.Black.copy(alpha = 0.5f), maxLines = 1)
            }
        }
    }
}
