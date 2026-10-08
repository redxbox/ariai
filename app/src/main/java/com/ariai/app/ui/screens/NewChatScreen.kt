package com.ariai.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Chat
import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import com.ariai.app.data.models.Provider
import kotlinx.coroutines.launch

private val Accent = Color(0xFF6C4DFF)
private val Ink = Color(0xFF1C1B1F)
private val Muted = Color(0xFF1C1B1F).copy(alpha = 0.55f)

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
    onSelectModel: (String, String) -> Unit = { _, _ -> },
    onAddProvider: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showModelSheet by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var useSearch by remember { mutableStateOf(false) }
    var useReasoning by remember { mutableStateOf(false) }
    var showAttachments by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(messages.size, currentStreamingContent.length) {
        val last = messages.size + if (currentStreamingContent.isNotEmpty()) 1 else 0
        if (last > 0) listState.animateScrollToItem(last - 1)
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Message", text))
        scope.launch { snackbarHostState.showSnackbar("Copied") }
        onCopyMessage(text)
    }

    fun send() {
        val text = inputText.trim()
        if (text.isBlank() || isStreaming) return
        val prefix = buildString {
            if (useSearch) append("[WebSearch] ")
            if (useReasoning) append("[Reasoning] ")
        }
        onSendMessage(prefix + text)
        inputText = ""
        showAttachments = false
    }

    if (showModelSheet) {
        ModelPickerSheet(
            providers = providers,
            selectedModelId = selectedModel,
            onSelect = { pid, mid -> onSelectModel(pid, mid) },
            onAddProvider = { showModelSheet = false; onAddProvider() },
            onDismiss = { showModelSheet = false }
        )
    }

    Scaffold(
        containerColor = Color(0xFFFEFBFF),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFEFBFF)),
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu", tint = Ink)
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().clickable { showModelSheet = true }) {
                        Text(chat?.title?.takeIf { it.isNotBlank() } ?: "New chat", color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, fontSize = 16.sp)
                        Text(selectedModel ?: "Tap to choose a model", color = Muted, fontSize = 11.sp, maxLines = 1)
                    }
                },
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Edit, contentDescription = "New chat", tint = Ink)
                    }
                    Box {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Ink)
                        }
                        DropdownMenu(expanded = showTopMenu, onDismissRequest = { showTopMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Copy conversation") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    copyToClipboard(messages.joinToString("\n\n") { it.content })
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Model") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    scope.launch { snackbarHostState.showSnackbar("Model: ${selectedModel ?: "none"}") }
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFFEFBFF)).navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(visible = showAttachments) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AttachChip(icon = Icons.Default.Image, label = "Image") {
                            scope.launch { snackbarHostState.showSnackbar("Image attachments are coming soon") }
                        }
                        AttachChip(icon = Icons.Default.AttachFile, label = "File") {
                            scope.launch { snackbarHostState.showSnackbar("File attachments are coming soon") }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)) {
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Message AriAI", color = Muted) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 5,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Accent,
                                focusedTextColor = Ink,
                                unfocusedTextColor = Ink
                            )
                        )
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconCircle(icon = Icons.Default.Add, description = "Attachments", active = showAttachments) {
                                showAttachments = !showAttachments
                            }
                            Spacer(Modifier.width(6.dp))
                            ToggleChip(icon = Icons.Default.Search, label = "Search", active = useSearch) { useSearch = !useSearch }
                            Spacer(Modifier.width(6.dp))
                            ToggleChip(icon = Icons.Default.Star, label = "Reasoning", active = useReasoning) { useReasoning = !useReasoning }
                            Spacer(Modifier.weight(1f))
                            val canSend = inputText.isNotBlank() && !isStreaming
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape)
                                    .background(if (canSend) Accent else Color(0xFFE6E0F5))
                                    .clickable(enabled = canSend) { send() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Send", tint = if (canSend) Color.White else Ink.copy(alpha = 0.35f), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (messages.isEmpty() && currentStreamingContent.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Accent, modifier = Modifier.size(30.dp))
                    }
                    Text("How can I help you?", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    Text("Type a message below to start.", color = Muted, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(count = messages.size, key = { i -> "${messages[i].id}_$i" }) { i ->
                    val m = messages[i]
                    if (m.role == MessageRole.USER) {
                        UserBubble(m.content)
                    } else {
                        AssistantBlock(
                            text = m.content,
                            onCopy = { copyToClipboard(m.content) },
                            onBranch = { onBranchMessage(m) },
                            onRetry = { onRegenerate(m) }
                        )
                    }
                }
                if (currentStreamingContent.isNotEmpty()) {
                    item(key = "streaming") {
                        AssistantBlock(text = currentStreamingContent, streaming = isStreaming)
                    }
                }
            }
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp),
            color = Accent.copy(alpha = 0.11f),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(text, color = Ink, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        }
    }
}

@Composable
private fun AssistantBlock(
    text: String,
    streaming: Boolean = false,
    onCopy: (() -> Unit)? = null,
    onBranch: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text, color = Ink, fontSize = 15.sp, lineHeight = 23.sp)
        if (streaming) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = Accent, trackColor = Accent.copy(alpha = 0.12f))
        } else if (onCopy != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ActionIcon(Icons.Default.ContentCopy, "Copy", onCopy)
                ActionIcon(Icons.Default.Share, "Branch", onBranch ?: {})
                ActionIcon(Icons.Default.Refresh, "Retry", onRetry ?: {})
            }
        }
    }
}

@Composable
private fun ActionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(icon, contentDescription = description, tint = Muted, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun IconCircle(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape).background(if (active) Accent.copy(alpha = 0.14f) else Color(0xFFF2F2F7)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = if (active) Accent else Ink.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ToggleChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (active) Accent.copy(alpha = 0.14f) else Color(0xFFF2F2F7),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = label, tint = if (active) Accent else Ink.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            Text(label, color = if (active) Accent else Ink.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

@Composable
private fun AttachChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp, modifier = Modifier.clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = label, tint = Accent, modifier = Modifier.size(18.dp))
            Text(label, color = Ink, fontSize = 13.sp)
        }
    }
}
