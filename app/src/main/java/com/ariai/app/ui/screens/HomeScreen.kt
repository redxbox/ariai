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

@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToProviders: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Light glass theme - consistent across all pages
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F9FF),
                        Color(0xFFEFF1FF),
                        Color(0xFFF5F7FF)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                // Top bar - AriAI logo + search + more
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Ari", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium, fontSize = 28.sp)
                            Text("AI", color = Color(0xFF6C4DFF), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium, fontSize = 28.sp)
                        }
                        Text("Think freely.\nGo beyond.", color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall, lineHeight = 14.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = CircleShape, color = Color.White, shadowElevation = 2.dp, modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black.copy(alpha = 0.7f))
                            }
                        }
                        Surface(shape = CircleShape, color = Color.White, shadowElevation = 2.dp, modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.Black.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }

            item {
                // Orb with A logo - centered
                Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer glow
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF6C4DFF).copy(alpha = 0.2f),
                                        Color(0xFF00D4FF).copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    // Orb
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF6C4DFF), Color(0xFF8B5CF6), Color(0xFF00D4FF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.displayMedium)
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Good morning", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("☀️", style = MaterialTheme.typography.titleLarge)
                    }
                    Text("What would you like to do today?", style = MaterialTheme.typography.bodyMedium, color = Color.Black.copy(alpha = 0.6f))
                }
            }

            item {
                // Grid 3 columns - fixed height cards, no vertical text bug
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        HomeCard(
                            title = "Explain",
                            subtitle = "Make complex\nthings simple",
                            icon = "💡",
                            bg = Color(0xFFEDE7FF),
                            iconColor = Color(0xFF6C4DFF),
                            modifier = Modifier.weight(1f),
                            onClick = onNewChat
                        )
                        HomeCard(
                            title = "Write",
                            subtitle = "Turn ideas\ninto content",
                            icon = "✏️",
                            bg = Color(0xFFD6E4FF),
                            iconColor = Color(0xFF4A90E2),
                            modifier = Modifier.weight(1f),
                            onClick = onNewChat
                        )
                        HomeCard(
                            title = "Code",
                            subtitle = "Build, debug,\ncreate",
                            icon = "💻",
                            bg = Color(0xFFD1F5E0),
                            iconColor = Color(0xFF00C853),
                            modifier = Modifier.weight(1f),
                            onClick = onNewChat
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        HomeCard(
                            title = "Create",
                            subtitle = "Generate\nimages",
                            icon = "🎨",
                            bg = Color(0xFFFFD6E0),
                            iconColor = Color(0xFFE91E63),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToTools
                        )
                        HomeCard(
                            title = "Search",
                            subtitle = "Get real-time\ninfo",
                            icon = "🌐",
                            bg = Color(0xFFFFE8B5),
                            iconColor = Color(0xFFFF9800),
                            modifier = Modifier.weight(1f),
                            onClick = onNewChat
                        )
                        HomeCard(
                            title = "More",
                            subtitle = "Discover\nall tools",
                            icon = "⋯",
                            bg = Color(0xFFE0E0E0),
                            iconColor = Color(0xFF616161),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToTools
                        )
                    }
                }
            }

            item {
                // Quick actions - functional
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Quick Actions", fontWeight = FontWeight.Bold, color = Color.Black, style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            QuickActionButton(text = "New Chat", icon = Icons.Default.Chat, modifier = Modifier.weight(1f), onClick = onNewChat)
                            QuickActionButton(text = "Providers", icon = Icons.Default.Storage, modifier = Modifier.weight(1f), onClick = onNavigateToProviders)
                            QuickActionButton(text = "Settings", icon = Icons.Default.Settings, modifier = Modifier.weight(1f), onClick = onNavigateToSettings)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun HomeCard(title: String, subtitle: String, icon: String, bg: Color, iconColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = bg, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(icon, style = MaterialTheme.typography.titleMedium)
                }
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = Color.Black, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(alpha = 0.6f), lineHeight = 12.sp, maxLines = 2)
            }
        }
    }
}

@Composable
fun QuickActionButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF0F0FF),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF6C4DFF), modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = Color.Black)
        }
    }
}
