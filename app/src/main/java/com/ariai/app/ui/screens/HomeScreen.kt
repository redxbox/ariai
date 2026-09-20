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

// RikkaHub inspired - clean minimal not crowded
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                // Greeting - clean minimal
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Text("Good morning", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black, lineHeight = 32.sp)
                    Text("How can I help you today?", fontSize = 16.sp, color = Color.Black.copy(alpha = 0.6f))
                }
            }

            item {
                // Main input - RikkaHub style clean
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onNewChat("") }
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Ask anything...", color = Color.Black.copy(alpha = 0.35f), fontSize = 16.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color.Black.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                                Icon(Icons.Default.Language, contentDescription = null, tint = Color.Black.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                            }
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF6C4DFF)).clickable { onNewChat("") }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            item {
                // Quick actions - 2 per row, not crowded, minimal
                Text("Quick actions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black.copy(alpha = 0.8f))
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    CleanActionCard(title = "Explain", subtitle = "Simplify complex topics", icon = Icons.Default.Lightbulb, color = Color(0xFF6750A4), modifier = Modifier.weight(1f), onClick = { onNewChat("Explain quantum computing in simple terms") })
                    CleanActionCard(title = "Write", subtitle = "Draft content", icon = Icons.Default.Edit, color = Color(0xFF6C4DFF), isPrimary = true, modifier = Modifier.weight(1f), onClick = { onNewChat("Help me write a professional email") })
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    CleanActionCard(title = "Code", subtitle = "Build and debug", icon = Icons.Default.Code, color = Color(0xFF006A60), modifier = Modifier.weight(1f), onClick = { onNewChat("Write a Python function to sort a list") })
                    CleanActionCard(title = "Analyze", subtitle = "Review data", icon = Icons.Default.Analytics, color = Color(0xFF904D00), modifier = Modifier.weight(1f), onClick = { onNewChat("Analyze this data and give insights") })
                }
            }

            item {
                // Tools row - minimal 3
                Text("Tools", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black.copy(alpha = 0.8f), modifier = Modifier.padding(top = 8.dp))
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    MiniToolCard(icon = Icons.Default.Chat, title = "Chat", onClick = { onNewChat("") }, modifier = Modifier.weight(1f))
                    MiniToolCard(icon = Icons.Default.Image, title = "Images", onClick = onNavigateToTools, modifier = Modifier.weight(1f))
                    MiniToolCard(icon = Icons.Default.Storage, title = "Models", onClick = onNavigateToProviders, modifier = Modifier.weight(1f))
                    MiniToolCard(icon = Icons.Default.Settings, title = "Settings", onClick = onNavigateToSettings, modifier = Modifier.weight(1f))
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun CleanActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier, isPrimary: Boolean = false, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isPrimary) Color(0xFF6C4DFF) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPrimary) 0.dp else 1.dp),
        modifier = modifier.height(110.dp).clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(if (isPrimary) Color.White.copy(alpha = 0.2f) else color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = if (isPrimary) Color.White else color, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = if (isPrimary) Color.White else Color.Black)
                Text(subtitle, fontSize = 11.sp, color = if (isPrimary) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.5f), maxLines = 1)
            }
        }
    }
}

@Composable
fun MiniToolCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFF3F0FF)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color(0xFF6C4DFF), modifier = Modifier.size(18.dp))
            }
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Black)
        }
    }
}
