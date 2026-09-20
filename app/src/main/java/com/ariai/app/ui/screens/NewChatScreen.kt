package com.ariai.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.ariai.app.data.models.Chat
import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import com.ariai.app.data.models.Provider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    chat: Chat?,
    messages: List<ChatMessage>,
    isStreaming: Boolean,
    currentStreamingContent: String,
    selectedModel: String?,
    providers: List<Provider> = emptyList(),
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit,
    onBranchMessage: (ChatMessage) -> Unit,
    onRegenerate: (ChatMessage) -> Unit,
    onCopyMessage: (String) -> Unit,
    onProviderChange: (String, String) -> Unit = { _, _ -> },
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var useSearch by remember { mutableStateOf(false) }
    var useReasoning by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(messages.size, currentStreamingContent) {
        if (messages.isNotEmpty() || currentStreamingContent.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(maxOf(0, messages.size - 1)) }
        }
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Copied", text))
        scope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
        onCopyMessage(text)
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFFEFBFF))) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    navigationIcon = {
                        Row {
                            IconButton(onClick = onOpenDrawer) {
                                Icon(Icons.Default.Menu, contentDescription = "Open menu", tint = Color.Black)
                            }
                            IconButton(onClick = {
                                scope.launch { snackbarHostState.showSnackbar("Starting new chat") }
                                onBack()
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "New chat", tint = Color.Black)
                            }
                        }
                    },
                    title = {
                        Column {
                            Text(chat?.title ?: "New Chat", color = Color.Black, fontWeight = FontWeight.SemiBold, maxLines = 1, style = MaterialTheme.typography.titleMedium)
                            Text(selectedModel ?: "GPT-4o", color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showTopMenu = true }) {
                                Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Color.Black)
                            }
                            DropdownMenu(expanded = showTopMenu, onDismissRequest = { showTopMenu = false }) {
                                DropdownMenuItem(text = { Text("Clear chat") }, onClick = {
                                    showTopMenu = false
                                    scope.launch { snackbarHostState.showSnackbar("Chat cleared") }
                                }, leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) })
                                DropdownMenuItem(text = { Text("Share chat") }, onClick = {
                                    showTopMenu = false
                                    scope.launch { snackbarHostState.showSnackbar("Share coming soon") }
                                }, leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) })
                                DropdownMenuItem(text = { Text("Model settings") }, onClick = {
                                    showTopMenu = false
                                    scope.launch { snackbarHostState.showSnackbar("Model: $selectedModel") }
                                }, leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) })
                            }
                        }
                    }
                )
            },
            bottomBar = {
                Surface(color = Color.White, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFFF2F2F7), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).clickable {
                                        onSendMessage("I want to attach an image")
                                        scope.launch { snackbarHostState.showSnackbar("Image attachment") }
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = "Attach image", tint = Color.Black.copy(alpha = 0.55f), modifier = Modifier.size(18.dp))
                                }
                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = { Text("Chat with AI", color = Color.Black.copy(alpha = 0.35f)) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        cursorColor = Color(0xFF6C4DFF)
                                    ),
                                    maxLines = 4
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier.size(38.dp).clip(CircleShape).background(if (inputText.isBlank()) Color(0xFFE5E5EA) else Color(0xFF6C4DFF)).clickable {
                                        if (inputText.isNotBlank()) {
                                            var final = inputText
                                            if (useSearch) final = "[WebSearch] $final"
                                            if (useReasoning) final = "[Reasoning] $final"
                                            onSendMessage(final)
                                            inputText = ""
                                        }
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Send message", tint = if (inputText.isBlank()) Color.Black.copy(alpha = 0.3f) else Color.White, modifier = Modifier.size(20.dp))
                                }
                                Box(
                                    modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFF2F2F7)).clickable {
                                        onSendMessage("I want to upload a file")
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Attach file", tint = Color.Black.copy(alpha = 0.65f), modifier = Modifier.size(20.dp))
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (useSearch) Color(0xFF6C4DFF).copy(alpha = 0.12f) else Color(0xFFF2F2F7),
                                    modifier = Modifier.clickable { useSearch = !useSearch }
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Search, contentDescription = "Toggle web search", tint = if (useSearch) Color(0xFF6C4DFF) else Color.Black.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                        if (useSearch) Text("Search", color = Color(0xFF6C4DFF), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (useReasoning) Color(0xFF6C4DFF).copy(alpha = 0.12f) else Color(0xFFF2F2F7),
                                    modifier = Modifier.clickable { useReasoning = !useReasoning }
                                ) {
                                    Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Star, contentDescription = "Toggle reasoning", tint = if (useReasoning) Color(0xFF6C4DFF) else Color.Black.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (messages.isEmpty() && currentStreamingContent.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF6C4DFF)), contentAlignment = Alignment.Center) {
                                    Text("A", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                                }
                                Text("How can I help you today?", color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SuggestionChip(onClick = { onSendMessage("Explain quantum computing") }, label = { Text("Explain") })
                                    SuggestionChip(onClick = { onSendMessage("Write a blog post") }, label = { Text("Write") })
                                    SuggestionChip(onClick = { onSendMessage("Write code for sorting") }, label = { Text("Code") })
                                }
                            }
                        }
                    }
                }

                items(count = messages.size, key = { i -> "${messages[i].id}_${messages[i].timestamp}_$i" }) { i ->
                    val m = messages[i]
                    if (m.role == MessageRole.USER) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF6C4DFF)), modifier = Modifier.widthIn(max = 320.dp)) {
                                Text(m.content, color = Color.White, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF6C4DFF)), contentAlignment = Alignment.Center) {
                                Text("A", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(m.content, color = Color.Black, style = MaterialTheme.typography.bodyMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                        Row(modifier = Modifier.clickable { copyToClipboard(m.content) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy message", tint = Color.Black.copy(alpha = 0.45f), modifier = Modifier.size(16.dp))
                                            Text("Copy", fontSize = MaterialTheme.typography.labelSmall.fontSize, color = Color.Black.copy(alpha = 0.45f))
                                        }
                                        Row(modifier = Modifier.clickable { onBranchMessage(m) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.Share, contentDescription = "Branch chat", tint = Color.Black.copy(alpha = 0.45f), modifier = Modifier.size(16.dp))
                                            Text("Branch", fontSize = MaterialTheme.typography.labelSmall.fontSize, color = Color.Black.copy(alpha = 0.45f))
                                        }
                                        Row(modifier = Modifier.clickable { onRegenerate(m) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = Color.Black.copy(alpha = 0.45f), modifier = Modifier.size(16.dp))
                                            Text("Retry", fontSize = MaterialTheme.typography.labelSmall.fontSize, color = Color.Black.copy(alpha = 0.45f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (currentStreamingContent.isNotEmpty()) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF6C4DFF)), contentAlignment = Alignment.Center) {
                                Text("A", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(currentStreamingContent, color = Color.Black, style = MaterialTheme.typography.bodyMedium)
                                    if (isStreaming) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), color = Color(0xFF6C4DFF))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
