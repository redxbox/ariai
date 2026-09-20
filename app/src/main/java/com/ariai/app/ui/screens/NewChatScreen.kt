package com.ariai.app.ui.screens

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
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size, currentStreamingContent) {
        if (messages.isNotEmpty() || currentStreamingContent.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(maxOf(0, messages.size - 1)) }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFFEFBFF))) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // RikkaHub top bar - clean, no elevation, edge-to-edge
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    navigationIcon = {
                        Row {
                            IconButton(onClick = onOpenDrawer) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.Black)
                            }
                            IconButton(onClick = { /* add */ }) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black)
                            }
                        }
                    },
                    title = {
                        Column {
                            Text(chat?.title ?: "New Chat", color = Color.Black, fontWeight = FontWeight.SemiBold, maxLines = 1, style = MaterialTheme.typography.titleMedium)
                            if (selectedModel != null) {
                                Text(selectedModel, color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.Black)
                        }
                    }
                )
            },
            bottomBar = {
                // RikkaHub bottom input - exact like screenshot 2 - clean professional
                Surface(color = Color.White, shadowElevation = 0.dp, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Input card - #F2F2F7 rounded 24
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFFF2F2F7),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Crop icon like screenshot
                                Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CropFree, contentDescription = null, tint = Color.Black.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
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
                        // Bottom actions - functional unique
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Send
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(if (inputText.isBlank()) Color(0xFFE5E5EA) else Color(0xFF6C4DFF)).clickable {
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
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Send", tint = if (inputText.isBlank()) Color.Black.copy(alpha = 0.3f) else Color.White, modifier = Modifier.size(18.dp))
                                }
                                // Attach
                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFF2F2F7)).clickable { onSendMessage("Attach file") }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, contentDescription = "Attach", tint = Color.Black.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Search toggle - functional unique
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (useSearch) Color(0xFF6C4DFF).copy(alpha = 0.12f) else Color(0xFFF2F2F7),
                                    modifier = Modifier.clickable { useSearch = !useSearch }
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = if (useSearch) Color(0xFF6C4DFF) else Color.Black.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                        if (useSearch) Text("Search", color = Color(0xFF6C4DFF), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                                // Reasoning toggle - functional unique
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (useReasoning) Color(0xFF6C4DFF).copy(alpha = 0.12f) else Color(0xFFF2F2F7),
                                    modifier = Modifier.clickable { useReasoning = !useReasoning }
                                ) {
                                    Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Psychology, contentDescription = null, tint = if (useReasoning) Color(0xFF6C4DFF) else Color.Black.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (messages.isEmpty() && currentStreamingContent.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF6C4DFF)), contentAlignment = Alignment.Center) {
                                    Text("A", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                                }
                                Text("How can I help you today?", color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyMedium)
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
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF6C4DFF)), contentAlignment = Alignment.Center) {
                                Text("A", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(m.content, color = Color.Black, style = MaterialTheme.typography.bodyMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Black.copy(alpha = 0.35f), modifier = Modifier.size(16.dp).clickable { onCopyMessage(m.content) })
                                        Icon(Icons.Default.Share, contentDescription = "Branch", tint = Color.Black.copy(alpha = 0.35f), modifier = Modifier.size(16.dp).clickable { onBranchMessage(m) })
                                        Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.Black.copy(alpha = 0.35f), modifier = Modifier.size(16.dp).clickable { onRegenerate(m) })
                                    }
                                }
                            }
                        }
                    }
                }

                if (currentStreamingContent.isNotEmpty()) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
