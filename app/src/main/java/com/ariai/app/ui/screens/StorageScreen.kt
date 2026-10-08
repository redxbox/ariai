package com.ariai.app.ui.screens

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Chat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Accent = Color(0xFF6C4DFF)
private val Ink = Color(0xFF1C1B1F)

/** Local data overview with a chat list export and a guarded "clear all". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    chats: List<Chat>,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Delete all chats?") },
            text = { Text("This removes every conversation on this device. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClearAll() }) { Text("Delete all", color = Color(0xFFBA1A1A)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        containerColor = Color(0xFFF7F6FB),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF7F6FB)),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Ink) } },
                title = { Text("Data & storage", fontWeight = FontWeight.SemiBold, color = Ink) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = Accent)
                        }
                        Column {
                            Text("${chats.size} conversations", fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 16.sp)
                            Text("Stored locally on this device", fontSize = 12.sp, color = Ink.copy(alpha = 0.5f))
                        }
                    }
                }
            }
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth().clickable {
                        val text = buildString {
                            appendLine("AriAI chat list")
                            appendLine("Exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())}")
                            appendLine()
                            chats.sortedByDescending { it.updatedAt }.forEach {
                                appendLine("- ${it.title.ifBlank { "New chat" }} (${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it.updatedAt))})")
                            }
                        }
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(send, "Export chat list"))
                    }
                ) {
                    Row(modifier = Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Accent)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Export chat list", fontWeight = FontWeight.Medium, color = Ink)
                            Text("Share a text list of your conversations", fontSize = 12.sp, color = Ink.copy(alpha = 0.5f))
                        }
                    }
                }
            }
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth().clickable(enabled = chats.isNotEmpty()) { confirmClear = true }
                ) {
                    Row(modifier = Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFBA1A1A))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Delete all chats", fontWeight = FontWeight.Medium, color = Color(0xFFBA1A1A))
                            Text("Removes every conversation on this device", fontSize = 12.sp, color = Ink.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}
