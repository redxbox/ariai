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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ariai.app.data.models.Chat
import com.ariai.app.util.LocalStrings
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    chats: List<Chat>,
    onChatClick: (Chat) -> Unit,
    onNewChat: () -> Unit,
    onDeleteChat: (Chat) -> Unit,
    onPinChat: (Chat) -> Unit,
    onVoiceClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Chats", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = onVoiceClick) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewChat,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(strings.newChat, fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        if (chats.isEmpty()) {
            // Glass home empty - Good morning design
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Good morning",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "What shall we explore today?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    // Glass ask anything input
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Ask anything...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF6C4DFF),
                                    modifier = Modifier.size(40.dp).clickable { onNewChat() }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // Glass triangles Explain / Write / Code
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassActionCard(
                            title = "Explain",
                            icon = Icons.Default.Lightbulb,
                            colors = listOf(Color(0xFFE0E0E0), Color(0xFFF5F5F5)),
                            modifier = Modifier.weight(1f),
                            onClick = onNewChat
                        )
                        GlassActionCard(
                            title = "Write",
                            icon = Icons.Default.Edit,
                            colors = listOf(Color(0xFF6C4DFF), Color(0xFF8B5CF6)),
                            isPrimary = true,
                            modifier = Modifier.weight(1f),
                            onClick = onNewChat
                        )
                        GlassActionCard(
                            title = "Code",
                            icon = Icons.Default.Code,
                            colors = listOf(Color(0xFFE0E0E0), Color(0xFFF5F5F5)),
                            modifier = Modifier.weight(1f),
                            onClick = onNewChat
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassSmallCard(title = "Quick Chat", subtitle = "Start a new conversation", icon = Icons.Default.Chat, modifier = Modifier.weight(1f), onClick = onNewChat)
                        GlassSmallCard(title = "Choose a Model", subtitle = "Find the best AI for your task", icon = Icons.Default.Category, modifier = Modifier.weight(1f), onClick = onNewChat)
                        GlassSmallCard(title = "Explore Tools", subtitle = "Unlock more possibilities", icon = Icons.Default.Apps, modifier = Modifier.weight(1f), onClick = onNewChat)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val pinned = chats.filter { it.isPinned }
                val unpinned = chats.filter { !it.isPinned }
                
                if (pinned.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFFD700))
                            Text("Pinned", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(
                        count = pinned.size,
                        key = { index -> "${pinned[index].id}_${pinned[index].updatedAt}_pinned_$index" }
                    ) { index ->
                        val chat = pinned[index]
                        GlassChatItem(
                            chat = chat,
                            onClick = { onChatClick(chat) },
                            onDelete = { onDeleteChat(chat) },
                            onPin = { onPinChat(chat) }
                        )
                    }
                    item {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Today", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    item {
                        Text("Today", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                items(
                    count = unpinned.size,
                    key = { index -> "${unpinned[index].id}_${unpinned[index].updatedAt}_$index" }
                ) { index ->
                    val chat = unpinned[index]
                    GlassChatItem(
                        chat = chat,
                        onClick = { onChatClick(chat) },
                        onDelete = { onDeleteChat(chat) },
                        onPin = { onPinChat(chat) }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun GlassActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colors: List<Color>,
    isPrimary: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) Color(0xFF6C4DFF) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = if (isPrimary) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, contentDescription = null, tint = if (isPrimary) Color.White else MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (isPrimary) Color.White else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun GlassSmallCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassChatItem(
    chat: Chat,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF6C4DFF).copy(alpha = 0.8f), Color(0xFF00D4AA).copy(alpha = 0.8f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    chat.title.firstOrNull()?.uppercase() ?: "C",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chat.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${chat.modelId?.take(20) ?: \"Chat\"} • ${formatTime(chat.updatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                formatTime(chat.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (chat.isPinned) "Unpin" else "Pin") },
                        onClick = { showMenu = false; onPin() },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }
        }
    }
}

fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}
