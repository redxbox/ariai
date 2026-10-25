package com.ariai.app.ui.drawer

import com.ariai.app.util.UiLang
import com.ariai.app.util.tx

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
import com.ariai.app.data.models.Provider
import java.util.Calendar


private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk
private enum class DayBucket(val labelKey: String) {
    PINNED("Pinned"), TODAY("Today"), YESTERDAY("Yesterday"), PREVIOUS_7_DAYS("Previous 7 days"), EARLIER("Earlier");

    val label: String get() = tx(labelKey)
}

private fun bucketOf(timestamp: Long, now: Long = System.currentTimeMillis()): DayBucket {
    val todayStart = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val yesterdayStart = (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    val previousWeekStart = (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }
    return when {
        timestamp >= todayStart.timeInMillis -> DayBucket.TODAY
        timestamp >= yesterdayStart.timeInMillis -> DayBucket.YESTERDAY
        timestamp >= previousWeekStart.timeInMillis -> DayBucket.PREVIOUS_7_DAYS
        else -> DayBucket.EARLIER
    }
}

private sealed interface DrawerRow {
    data class Header(val id: String, val label: String) : DrawerRow
    data class ChatRow(val chat: Chat) : DrawerRow
}

/**
 * Side drawer with searchable, date- and folder-grouped conversations, a local profile/provider card,
 * and compact navigation to the app's main areas.
 */
@Composable
fun ColumnScope.AppDrawerContent(
    chats: List<Chat>,
    currentChatId: String?,
    currentRoute: String,
    activeProvider: Provider?,
    activeModelName: String?,
    onNewChat: () -> Unit,
    onChatClick: (Chat) -> Unit,
    onPinChat: (Chat) -> Unit,
    onDeleteChat: (Chat) -> Unit,
    onSetChatFolder: (Chat, String?) -> Unit,
    onNavigate: (String) -> Unit,
    onAbout: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val rows: List<DrawerRow> = remember(chats, query, UiLang.code) {
        // Older databases may contain malformed chat rows. They must never produce a `chat/` route.
        val validChats = chats.filter { it.id.isNotBlank() }
        val filtered = if (query.isBlank()) validChats else validChats.filter { it.title.contains(query, ignoreCase = true) }
        val out = mutableListOf<DrawerRow>()
        val pinned = filtered.filter { it.isPinned }
        if (pinned.isNotEmpty()) {
            out += DrawerRow.Header("header:pinned", DayBucket.PINNED.label)
            pinned.forEach { out += DrawerRow.ChatRow(it) }
        }
        val rest = filtered.filterNot { it.isPinned }.sortedByDescending { it.updatedAt }
        val folders = rest.mapNotNull { it.folder?.trim()?.takeIf(String::isNotEmpty) }
            .distinctBy { it.lowercase() }
            .sortedBy(String::lowercase)
        folders.forEach { folder ->
            val group = rest.filter { it.folder?.trim().equals(folder, ignoreCase = true) }
            if (group.isNotEmpty()) {
                out += DrawerRow.Header("header:folder:$folder", "▸ $folder")
                group.forEach { out += DrawerRow.ChatRow(it) }
            }
        }
        val unfiled = rest.filter { it.folder.isNullOrBlank() }
        DayBucket.values().filter { it != DayBucket.PINNED }.forEach { bucket ->
            val group = unfiled.filter { bucketOf(it.updatedAt) == bucket }
            if (group.isNotEmpty()) {
                out += DrawerRow.Header("header:${bucket.name.lowercase()}", bucket.label)
                group.forEach { out += DrawerRow.ChatRow(it) }
            }
        }
        out
    }

    Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(15.dp))
                .background(Brush.linearGradient(listOf(AriAccentDark, AriAccent))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("AriAI", color = Ink, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Text(tx("Your AI, your way"), color = AriMuted, fontSize = 11.sp)
        }
        Box(Modifier.size(8.dp).clip(CircleShape).background(AriAccent))
    }

    Button(
        onClick = onNewChat,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp),
        shape = RoundedCornerShape(17.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AriAccent, contentColor = Color.White)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(tx("New chat"), fontWeight = FontWeight.SemiBold)
    }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        singleLine = true,
        placeholder = { Text(tx("Search conversations"), color = AriMuted, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AriAccent, modifier = Modifier.size(18.dp)) },
        shape = RoundedCornerShape(17.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AriTint,
            unfocusedContainerColor = AriTint,
            focusedBorderColor = AriAccent.copy(alpha = 0.6f),
            unfocusedBorderColor = AriLine
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    )

    // Conversation list
    LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (rows.isEmpty()) {
            item(key = "drawer:empty") {
                Text(
                    if (query.isBlank()) tx("No conversations yet") else tx("No matches"),
                    color = Ink.copy(alpha = 0.45f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        itemsIndexed(rows, key = { _, row ->
            when (row) {
                is DrawerRow.Header -> row.id
                is DrawerRow.ChatRow -> "chat:${row.chat.id}"
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
                    onDelete = { onDeleteChat(row.chat) },
                    folderNames = chats.mapNotNull { it.folder?.trim()?.takeIf(String::isNotEmpty) }
                        .distinctBy { it.lowercase() }
                        .sortedBy(String::lowercase),
                    onSetFolder = { folder -> onSetChatFolder(row.chat, folder) }
                )
            }
        }
    }

    HorizontalDivider(color = AriInk.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))

    // Local profile and active provider: no cloud account is implied.
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AriTint,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onNavigate("workspace") }
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(AriAccentDark, AriAccent))),
                contentAlignment = Alignment.Center
            ) {
                Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(tx("Personal workspace"), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text(
                    activeProvider?.let { provider -> listOfNotNull(provider.name, activeModelName).joinToString(" · ") }
                        ?: tx("No provider selected"),
                    color = Ink.copy(alpha = 0.5f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (activeProvider?.enabled == true) Color(0xFF37865B) else Ink.copy(alpha = 0.2f)))
        }
    }

    // Footer: quick access to the main areas. Each button opens a real screen.
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        FooterAction(Icons.Default.FolderOpen, tx("Workspace"), currentRoute == "workspace") { onNavigate("workspace") }
        FooterAction(Icons.Default.Person, tx("Agents & prompts"), currentRoute == "agents") { onNavigate("agents") }
        FooterAction(Icons.Default.Storage, tx("Providers"), currentRoute == "providers") { onNavigate("providers") }
        FooterAction(Icons.Default.Extension, "MCP", currentRoute == "mcp") { onNavigate("mcp") }
        FooterAction(Icons.Default.Public, tx("Search"), currentRoute == "search_service") { onNavigate("search_service") }
        FooterAction(Icons.Default.Settings, tx("Settings"), currentRoute == "settings") { onNavigate("settings") }
        FooterAction(Icons.Default.Info, tx("About"), false, onAbout)
    }
    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
}

@Composable
private fun ChatRowItem(
    modifier: Modifier = Modifier,
    chat: Chat,
    selected: Boolean,
    folderNames: List<String>,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onSetFolder: (String?) -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var folderName by remember(chat.id, chat.folder) { mutableStateOf(chat.folder.orEmpty()) }
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
            chat.title.ifBlank { tx("New chat") },
            color = Ink,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = tx("Chat options"), tint = Ink.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
            }
            val menuShape = paperPopupShape(16.dp, 12.dp)
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                modifier = Modifier.background(AriCard, menuShape).then(paperPopupModifier(menuShape, 12.dp))
            ) {
                DropdownMenuItem(
                    text = { Text(if (chat.isPinned) tx("Unpin") else tx("Pin")) },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                    onClick = { menu = false; onPin() }
                )
                DropdownMenuItem(
                    text = { Text(tx("Move to folder")) },
                    leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                    onClick = { menu = false; folderName = chat.folder.orEmpty(); showFolderDialog = true }
                )
                DropdownMenuItem(
                    text = { Text(tx("Delete")) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    onClick = { menu = false; onDelete() }
                )
            }
        }
    }
    if (showFolderDialog) {
        val dialogShape = paperPopupShape(24.dp)
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            modifier = paperPopupModifier(dialogShape),
            shape = dialogShape,
            containerColor = AriCard,
            title = { Text(tx("Move to folder")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it.take(40) },
                        label = { Text(tx("Folder name")) },
                        placeholder = { Text(tx("e.g. Projects")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val suggestions = folderNames.filterNot { it.equals(chat.folder, ignoreCase = true) }.take(4)
                    if (suggestions.isNotEmpty()) {
                        Text(tx("Existing folders"), color = Ink.copy(alpha = 0.5f), fontSize = 12.sp)
                        suggestions.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { suggestion ->
                                    AssistChip(onClick = { folderName = suggestion }, label = { Text(suggestion, maxLines = 1) })
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onSetFolder(folderName.trim().takeIf { it.isNotEmpty() }); showFolderDialog = false }) {
                    Text(tx("Save"))
                }
            },
            dismissButton = {
                Row {
                    if (!chat.folder.isNullOrBlank()) {
                        TextButton(onClick = { onSetFolder(null); showFolderDialog = false }) { Text(tx("Remove folder")) }
                    }
                    TextButton(onClick = { showFolderDialog = false }) { Text(tx("Cancel")) }
                }
            }
        )
    }
}

@Composable
private fun FooterAction(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (selected) AriTint else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) AriAccent else AriMuted,
            modifier = Modifier.size(21.dp)
        )
        Text(
            label,
            fontSize = 14.sp,
            color = if (selected) AriAccent else Ink,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
