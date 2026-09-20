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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Chat
import java.text.SimpleDateFormat
import java.util.*

// RikkaHub inspired chat list - clean minimal not crowded
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    chats: List<Chat>,
    onChatClick: (Chat) -> Unit,
    onNewChat: () -> Unit,
    onDeleteChat: (Chat) -> Unit,
    onPinChat: (Chat) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFFEFBFF))) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNewChat,
                    containerColor = Color(0xFF6C4DFF),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "New Chat", tint = Color.White)
                }
            }
        ) { padding ->
            if (chats.isEmpty()) {
                // Empty state - clean centered like RikkaHub
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
                        Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFF6C4DFF).copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Color(0xFF6C4DFF), modifier = Modifier.size(36.dp))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("No conversations yet", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Color.Black)
                            Text("Start a new chat to begin", color = Color.Black.copy(alpha = 0.5f), fontSize = 14.sp)
                        }
                        Button(onClick = onNewChat, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4DFF)), modifier = Modifier.padding(top = 8.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("New Chat")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search chats", color = Color.Black.copy(alpha = 0.4f)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black.copy(alpha = 0.4f), modifier = Modifier.size(20.dp)) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF6C4DFF).copy(alpha = 0.3f),
                                unfocusedBorderColor = Color.Black.copy(alpha = 0.08f)
                            ),
                            singleLine = true
                        )
                    }

                    val filtered = if (searchQuery.isBlank()) chats else chats.filter { it.title.contains(searchQuery, ignoreCase = true) }

                    if (filtered.isNotEmpty()) {
                        item {
                            Text("Today", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Black.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                        }
                        items(count = filtered.size, key = { i -> filtered[i].id }) { i ->
                            val chat = filtered[i]
                            CleanChatItem(chat = chat, onClick = { onChatClick(chat) }, onDelete = { onDeleteChat(chat) }, onPin = { onPinChat(chat) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CleanChatItem(chat: Chat, onClick: () -> Unit, onDelete: () -> Unit, onPin: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF3F0FF)), contentAlignment = Alignment.Center) {
                Text(chat.title.firstOrNull()?.uppercase() ?: "C", color = Color(0xFF6C4DFF), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(chat.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${chat.modelId} • ${formatTimeClean(chat.updatedAt)}", fontSize = 12.sp, color = Color.Black.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Black.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Pin") }, onClick = { showMenu = false; onPin() }, leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) })
                }
            }
        }
    }
}

fun formatTimeClean(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "now"
        diff < 3600_000 -> "${diff / 60000}m"
        diff < 86400_000 -> "${diff / 3600000}h"
        diff < 604800_000 -> "${diff / 86400000}d"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}
