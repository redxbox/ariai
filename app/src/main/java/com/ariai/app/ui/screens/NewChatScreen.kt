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
import androidx.compose.ui.graphics.Brush
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
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size, currentStreamingContent) {
        if (messages.isNotEmpty() || currentStreamingContent.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(maxOf(0, messages.size - 1)) }
        }
    }

    // Light glass theme consistent
    Box(
        modifier = modifier.fillMaxSize().background(
            Brush.verticalGradient(colors = listOf(Color(0xFFF8F9FF), Color(0xFFEFF1FF)))
        )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.8f)),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Ari", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("AI", color = Color(0xFF6C4DFF), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }
                                Text("Think freely.", color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Black)
                        }
                    },
                    actions = {
                        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.padding(end = 8.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF10A37F)), contentAlignment = Alignment.Center) {
                                    Text("O", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Text(selectedModel?.take(12) ?: "GPT-4o", color = Color.Black, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }
                        Surface(shape = CircleShape, color = Color(0xFF6C4DFF), modifier = Modifier.size(40.dp).clickable { }) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                )
            },
            bottomBar = {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(shape = CircleShape, color = Color(0xFFF0F0FF), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black.copy(alpha = 0.7f))
                            }
                        }
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Message AriAI...", color = Color.Black.copy(alpha = 0.4f)) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF5F7FF),
                                unfocusedContainerColor = Color(0xFFF5F7FF),
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        IconButton(onClick = { }) { Icon(Icons.Default.Image, contentDescription = null, tint = Color.Black.copy(alpha = 0.6f)) }
                        IconButton(onClick = { }) { Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black.copy(alpha = 0.6f)) }
                        Surface(shape = CircleShape, color = Color(0xFF6C4DFF), modifier = Modifier.size(44.dp).clickable {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        }) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White)
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (messages.isEmpty() && currentStreamingContent.isEmpty()) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(0xFF6C4DFF), Color(0xFF00D4AA)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.widthIn(max = 280.dp)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Hello!", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text("How can I help you today?", color = Color.Black.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Text("9:41", style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(alpha = 0.4f), modifier = Modifier.padding(start = 12.dp, top = 4.dp))
                            }
                        }
                    }
                }

                items(count = messages.size, key = { index -> "${messages[index].id}_${messages[index].timestamp}_$index" }) { index ->
                    val message = messages[index]
                    if (message.role == MessageRole.USER) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF6C4DFF)), modifier = Modifier.widthIn(max = 300.dp)) {
                                Text(message.content, color = Color.White, modifier = Modifier.padding(16.dp))
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(Color(0xFF6C4DFF), Color(0xFF00D4AA)))), contentAlignment = Alignment.Center) {
                                Text("A", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.widthIn(max = 300.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(message.content, color = Color.Black)
                                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(onClick = { onCopyMessage(message.content) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black.copy(alpha = 0.5f))
                                        }
                                        IconButton(onClick = { onBranchMessage(message) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black.copy(alpha = 0.5f))
                                        }
                                        IconButton(onClick = { onRegenerate(message) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (currentStreamingContent.isNotEmpty()) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(Color(0xFF6C4DFF), Color(0xFF00D4AA)))), contentAlignment = Alignment.Center) {
                                Text("A", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(currentStreamingContent, color = Color.Black)
                                    if (isStreaming) {
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), color = Color(0xFF6C4DFF))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
