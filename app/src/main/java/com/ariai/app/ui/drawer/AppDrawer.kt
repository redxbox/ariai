package com.ariai.app.ui.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.ariai.app.ui.theme.*

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Chat
import java.util.Calendar


private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk
private enum class DayBucket(val label: String) { PINNED("Pinned"), TODAY("Today"), YESTERDAY("Yesterday"), EARLIER("Earlier") }

private fun bucketOf(timestamp: Long, now: Long = System.currentTimeMillis()): DayBucket {
    val nowCal = Calendar.getInstance().apply { timeInMillis = now }
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    fun sameDay(a: Calendar, b: Calendar) = a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    val yesterday = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        sameDay(cal, nowCal) -> DayBucket.TODAY
        sameDay(cal, yesterday) -> DayBucket.YESTERDAY
        else -> DayBucket.EARLIER
    }
}

private sealed interface DrawerRow {
    data class Header(val label: String) : DrawerRow
    data class ChatRow(val chat: Chat) : DrawerRow
}

/**
 * Side drawer in the reference layout: brand header, new chat, searchable
 * conversation list grouped by Pinned / Today / Yesterday / Earlier,
 * and a compact navigation footer.
 */
@Composable
fun ColumnScope.AppDrawerContent(
    chats: List<Chat>,
    currentChatId: String?,
    currentRoute: String,
    onNewChat: () -> Unit,
    onNewImageChat: () -> Unit = onNewChat,
    onChatClick: (Chat) -> Unit,
    onPinChat: (Chat) -> Unit,
    onDeleteChat: (Chat) -> Unit,
    onNavigate: (String) -> Unit,
    onAbout: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val rows: List<DrawerRow> = remember(chats, query) {
        val filtered = if (query.isBlank()) chats else chats.filter { it.title.contains(query, ignoreCase = true) }
        val out = mutableListOf<DrawerRow>()
        val pinned = filtered.filter { it.isPinned }
        if (pinned.isNotEmpty()) {
            out += DrawerRow.Header(DayBucket.PINNED.label)
            pinned.forEach { out += DrawerRow.ChatRow(it) }
        }
        val rest = filtered.filterNot { it.isPinned }.sortedByDescending { it.updatedAt }
        DayBucket.values().filter { it != DayBucket.PINNED }.forEach { bucket ->
            val group = rest.filter { bucketOf(it.updatedAt) == bucket }
            if (group.isNotEmpty()) {
                out += DrawerRow.Header(bucket.label)
                group.forEach { out += DrawerRow.ChatRow(it) }
            }
        }
        out
    }

    // Status-bar inset only: no decorative header or extra top margin.
    Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
    Spacer(Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onNewChat,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AriInk, contentColor = AriPaper)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("New chat", fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(
            onClick = onNewImageChat,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AriInk),
            border = BorderStroke(1.dp, AriLine)
        ) {
            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("عکس", fontWeight = FontWeight.SemiBold)
        }
    }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        singleLine = true,
        placeholder = { Text("Search conversations", color = Ink.copy(alpha = 0.4f), fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Ink.copy(alpha = 0.4f), modifier = Modifier.size(18.dp)) },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AriCard,
            unfocusedContainerColor = AriCard,
            focusedBorderColor = AriInk.copy(alpha = 0.35f),
            unfocusedBorderColor = Color.Black.copy(alpha = 0.06f)
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
    )

    // Conversation list
    LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (rows.isEmpty()) {
            item {
                Text(
                    if (query.isBlank()) "No conversations yet" else "No matches",
                    color = Ink.copy(alpha = 0.45f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        itemsIndexed(rows, key = { index, row ->
            when (row) {
                is DrawerRow.Header -> "h_${row.label}_$index"
                is DrawerRow.ChatRow -> "c_${row.chat.id}_$index"
            }
        }) { _, row ->
            when (row) {
                is DrawerRow.Header -> Text(
                    row.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink.copy(alpha = 0.45f),
                    modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp)
                )
                is DrawerRow.ChatRow -> ChatRowItem(
                    chat = row.chat,
                    selected = row.chat.id == currentChatId,
                    onClick = { onChatClick(row.chat) },
                    onPin = { onPinChat(row.chat) },
                    onDelete = { onDeleteChat(row.chat) }
                )
            }
        }
    }

    HorizontalDivider(color = Color.Black.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))

    // Footer: quick access to the main areas. Each button opens a real screen.
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FooterAction(Icons.Default.Storage, "Providers", currentRoute == "providers") { onNavigate("providers") }
        FooterAction(Icons.Default.Extension, "MCP", currentRoute == "mcp") { onNavigate("mcp") }
        FooterAction(Icons.Default.Public, "Search", currentRoute == "search_service") { onNavigate("search_service") }
        FooterAction(Icons.Default.Settings, "Settings", currentRoute == "settings") { onNavigate("settings") }
        FooterAction(Icons.Default.Info, "About", false, onAbout)
    }
    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
}

@Composable
private fun ChatRowItem(modifier: Modifier = Modifier, chat: Chat, selected: Boolean, onClick: () -> Unit, onPin: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) AriTint else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            // The one coloured element in the drawer: marks the open chat.
            Box(Modifier.size(6.dp).clip(CircleShape).background(AriAccent))
            Spacer(Modifier.width(8.dp))
        }
        if (chat.isPinned) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Ink, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            chat.title.ifBlank { "New chat" },
            color = Ink,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "Chat options", tint = Ink.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(if (chat.isPinned) "Unpin" else "Pin") },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                    onClick = { menu = false; onPin() }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    onClick = { menu = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun FooterAction(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                .background(if (selected) AriTint else AriPaper),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Ink, modifier = Modifier.size(20.dp))
        }
        Text(label, fontSize = 11.sp, color = Ink, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
