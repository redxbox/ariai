package com.ariai.app.ui.screens

import com.ariai.app.util.tx

import com.ariai.app.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Chat
import com.ariai.app.ui.components.ConfirmDeleteChatDialog
import com.ariai.app.ui.components.MenuIconBadge
import com.ariai.app.ui.components.MenuIconTone
import java.text.SimpleDateFormat
import java.util.*
import com.ariai.app.util.ChatDateBucket
import com.ariai.app.util.chatDateBucket
import com.ariai.app.util.UiLang

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

    Box(modifier = modifier.fillMaxSize().background(AriPaper)) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                if (chats.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = onNewChat,
                        containerColor = AriAccent,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = tx("New chat"), tint = Color.White)
                    }
                }
            }
        ) { padding ->
            if (chats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
                        Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(AriAccent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = AriAccent, modifier = Modifier.size(36.dp))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(tx("No conversations yet"), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = AriInk)
                            Text(tx("Start a new chat to begin"), color = AriInk.copy(alpha = 0.5f), fontSize = 14.sp)
                        }
                        Button(onClick = onNewChat, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = AriAccent, contentColor = Color.White), modifier = Modifier.padding(top = 8.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tx("New Chat"))
                        }
                    }
                }
            } else {
                val listState = rememberLazyListState()
                ClearFocusOnScroll(listState)
                val filtered = remember(chats, searchQuery) {
                    if (searchQuery.isBlank()) chats else chats.filter {
                        it.title.contains(searchQuery, ignoreCase = true)
                    }
                }
                val groups = remember(filtered) {
                    filtered
                        .sortedWith(compareByDescending<Chat> { it.isPinned }.thenByDescending { it.updatedAt })
                        .groupBy { chat -> if (chat.isPinned) ChatDateBucket.PINNED else chatDateBucket(chat.updatedAt) }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(tx("Search chats"), color = AriInk.copy(alpha = 0.4f)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AriInk.copy(alpha = 0.4f), modifier = Modifier.size(20.dp)) },
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start, textDirection = TextDirection.ContentOrLtr),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AriCard,
                                unfocusedContainerColor = AriCard,
                                focusedBorderColor = AriInk.copy(alpha = 0.3f),
                                unfocusedBorderColor = AriInk.copy(alpha = 0.08f)
                            ),
                            singleLine = true
                        )
                    }

                    groups.forEach { (bucket, bucketChats) ->
                        item(key = "history:${bucket.name}") {
                            Text(
                                tx(bucket.labelKey),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = AriInk.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(bucketChats, key = { it.id }) { chat ->
                            CleanChatItem(chat = chat, onClick = { onChatClick(chat) }, onDelete = { onDeleteChat(chat) }, onPin = { onPinChat(chat) })
                        }
                    }
                    if (filtered.isEmpty() && searchQuery.isNotBlank()) {
                        item { Text(tx("No matches"), color = AriMuted, modifier = Modifier.padding(vertical = 20.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun CleanChatItem(chat: Chat, onClick: () -> Unit, onDelete: () -> Unit, onPin: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AriCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(vertical = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(AriTint), contentAlignment = Alignment.Center) {
                Text(chat.title.firstOrNull()?.uppercase() ?: "C", color = AriInk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(chat.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = AriInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${chat.modelId} • ${formatTimeClean(chat.updatedAt)}", fontSize = 12.sp, color = AriInk.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = tx("Chat options"), tint = AriInk.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                }
                val menuShape = paperPopupShape(16.dp)
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(AriCard, menuShape).then(paperPopupModifier(menuShape))
                ) {
                    DropdownMenuItem(text = { Text(tx("Pin")) }, onClick = { showMenu = false; onPin() }, leadingIcon = { MenuIconBadge(Icons.Default.Star, MenuIconTone.Mint, badgeSize = 30.dp, iconSize = 16.dp) })
                    DropdownMenuItem(text = { Text(tx("Delete")) }, onClick = { showMenu = false; confirmDelete = true }, leadingIcon = { MenuIconBadge(Icons.Default.Delete, MenuIconTone.Danger, badgeSize = 30.dp, iconSize = 16.dp) })
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDeleteChatDialog(
            onDismiss = { confirmDelete = false },
            onConfirm = { confirmDelete = false; onDelete() }
        )
    }
}

fun formatTimeClean(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val locale = when (UiLang.code) {
        "fa" -> Locale.forLanguageTag("fa-IR")
        else -> Locale.US
    }
    return when {
        diff < 60_000 -> tx("Just now")
        diff < 3600_000 -> "${diff / 60_000} ${tx("min ago")}"
        diff < 86400_000 -> "${diff / 3600_000} ${tx("hr ago")}"
        diff < 604800_000 -> "${diff / 86400_000} ${tx("d ago")}"
        else -> SimpleDateFormat("MMM d", locale).format(Date(timestamp))
    }
}
