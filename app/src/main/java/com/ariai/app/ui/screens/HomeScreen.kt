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
    // Dark background like screenshot - deep navy with purple waves
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A1E),
                        Color(0xFF121230),
                        Color(0xFF0A0A1E)
                    )
                )
            )
    ) {
        // Purple wave decorations
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF6C4DFF).copy(alpha = 0.15f),
                            Color(0xFF00D4FF).copy(alpha = 0.1f),
                            Color(0xFF6C4DFF).copy(alpha = 0.15f)
                        )
                    )
                )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // Left sidebar - dark glass
            Card(
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F2A).copy(alpha = 0.9f)),
                modifier = Modifier.width(200.dp).fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        // Logo
                        Column {
                            Text(
                                "AriAI",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "Think freely.\nGo beyond.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f),
                                lineHeight = 14.sp
                            )
                        }

                        // Navigation
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SidebarItem(icon = Icons.Default.Home, label = "Home", selected = true, onClick = {})
                            SidebarItem(icon = Icons.Default.Chat, label = "Chat", onClick = onNavigateToChat)
                            SidebarItem(icon = Icons.Default.ViewInAr, label = "Providers", onClick = onNavigateToProviders)
                            Spacer(modifier = Modifier.height(16.dp))
                            SidebarItem(icon = Icons.Default.Apps, label = "Tools", onClick = onNavigateToTools)
                            SidebarItem(icon = Icons.Default.Folder, label = "Library", onClick = {})
                            SidebarItem(icon = Icons.Default.Settings, label = "Settings", onClick = onNavigateToSettings)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Crystal pyramid decoration
                        Box(
                            modifier = Modifier.size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔷", style = MaterialTheme.typography.displayMedium)
                        }
                        Text(
                            "A Smarter\nYou\nEvery Day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f),
                            lineHeight = 16.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { }.padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("My Account", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Main content
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    // Top bar with search and more
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }

                item {
                    // Orb with A logo
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Glow effect
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF6C4DFF).copy(alpha = 0.5f),
                                            Color(0xFF00D4FF).copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        // Orb
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF1A1A4A),
                                            Color(0xFF2A2A6A)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "A",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                            )
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Good morning",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text("☀️", style = MaterialTheme.typography.titleLarge)
                        }
                        Text(
                            "What would you like to do today?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                item {
                    // 3x2 grid of glass cards
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            HomeGlassCard(
                                title = "Explain",
                                subtitle = "Make complex\nthings simple",
                                icon = "💡",
                                iconBg = Color(0xFF6C4DFF),
                                modifier = Modifier.weight(1f),
                                onClick = onNewChat
                            )
                            HomeGlassCard(
                                title = "Write",
                                subtitle = "Turn ideas into\ncontent",
                                icon = "✏️",
                                iconBg = Color(0xFF4A90E2),
                                modifier = Modifier.weight(1f),
                                onClick = onNewChat
                            )
                            HomeGlassCard(
                                title = "Code",
                                subtitle = "Build, debug,\ncreate",
                                icon = "</>",
                                iconBg = Color(0xFF00E676),
                                modifier = Modifier.weight(1f),
                                onClick = onNewChat
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            HomeGlassCard(
                                title = "Create",
                                subtitle = "Generate images\nand visuals",
                                icon = "🖼️",
                                iconBg = Color(0xFFE91E63),
                                modifier = Modifier.weight(1f),
                                onClick = onNewChat
                            )
                            HomeGlassCard(
                                title = "Search",
                                subtitle = "Get real-time\ninformation",
                                icon = "🌐",
                                iconBg = Color(0xFFFF9800),
                                modifier = Modifier.weight(1f),
                                onClick = onNewChat
                            )
                            HomeGlassCard(
                                title = "More",
                                subtitle = "Discover all tools",
                                icon = "•••",
                                iconBg = Color(0xFF9C27B0),
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToTools
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SidebarItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean = false, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFF6C4DFF).copy(alpha = 0.3f) else Color.Transparent,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6C4DFF).copy(alpha = 0.5f)) else null,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) Color(0xFF9C7CFF) else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = if (selected) Color.White else Color.White.copy(alpha = 0.6f), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
fun HomeGlassCard(title: String, subtitle: String, icon: String, iconBg: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Surface(shape = RoundedCornerShape(12.dp), color = iconBg.copy(alpha = 0.2f), modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(icon, style = MaterialTheme.typography.titleMedium, color = iconBg)
                    }
                }
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), lineHeight = 14.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }
        }
    }
}
