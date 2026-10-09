package com.ariai.app.ui.screens

import com.ariai.app.ui.theme.*

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Attachment
import com.ariai.app.data.models.AttachmentType
import com.ariai.app.data.models.Chat
import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import com.ariai.app.data.models.ReasoningLevel
import com.ariai.app.data.models.SearchMode
import com.ariai.app.util.ReadResult
import com.ariai.app.util.bitmapAttachment
import com.ariai.app.util.readAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Accent = AriInk
private val Ink = Color(0xFF1C1B1F)
private val Muted = Color(0xFF1C1B1F).copy(alpha = 0.55f)
private val PageBg = AriPaper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    chat: Chat?,
    messages: List<ChatMessage>,
    isStreaming: Boolean,
    currentStreamingContent: String,
    selectedModel: String?,
    providers: List<Provider> = emptyList(),
    reasoning: ReasoningLevel = ReasoningLevel.AUTO,
    searchMode: SearchMode = SearchMode.OFF,
    favorites: Set<String> = emptySet(),
    localSearchConfigured: Boolean = false,
    onSendMessage: (String, List<Attachment>, ReasoningLevel, SearchMode) -> Unit,
    onBack: () -> Unit,
    onBranchMessage: (ChatMessage) -> Unit,
    onRegenerate: (ChatMessage) -> Unit,
    onCopyMessage: (String) -> Unit,
    onOpenDrawer: () -> Unit = {},
    onSelectModel: (String, String) -> Unit = { _, _ -> },
    onToggleFavorite: (String, String) -> Unit = { _, _ -> },
    onReasoningChange: (ReasoningLevel) -> Unit = {},
    onSearchModeChange: (SearchMode) -> Unit = {},
    onCompressHistory: ((String) -> Unit) -> Unit = {},
    onOpenExtensions: () -> Unit = {},
    onOpenSearchSettings: () -> Unit = {},
    onAddProvider: () -> Unit = {},
    fontSize: Int = 15,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var showModelSheet by remember { mutableStateOf(false) }
    var showFullEditor by remember { mutableStateOf(false) }
    if (showFullEditor) {
        FullScreenMessageEditor(
            text = inputText,
            onTextChange = { inputText = it },
            onCollapse = { showFullEditor = false }
        )
    }
    var showAttachments by remember { mutableStateOf(false) }
    var showThinking by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val currentProvider = providers.find { it.id == chat?.providerId } ?: providers.firstOrNull()
    val modelSearchAvailable = currentProvider?.type == ProviderType.GEMINI

    LaunchedEffect(messages.size, currentStreamingContent.length) {
        val last = messages.size + if (currentStreamingContent.isNotEmpty()) 1 else 0
        if (last > 0) listState.animateScrollToItem(last - 1)
    }

    fun showNote(text: String) {
        scope.launch { snackbarHostState.showSnackbar(text) }
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Message", text))
        showNote("Copied")
        onCopyMessage(text)
    }

    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    fun addFile(uri: Uri) {
        scope.launch {
            when (val result = withContext(Dispatchers.IO) { readAttachment(context, uri) }) {
                is ReadResult.Ok -> pending = pending + result.attachment
                is ReadResult.Error -> showNote(result.message)
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) addFile(uri)
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) addFile(uri)
    }
    val cameraPicker = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            scope.launch {
                pending = pending + withContext(Dispatchers.Default) { bitmapAttachment(bitmap) }
            }
        }
    }

    fun send() {
        val text = inputText.trim()
        if ((text.isBlank() && pending.isEmpty()) || isStreaming) return
        onSendMessage(text, pending, reasoning, searchMode)
        inputText = ""
        pending = emptyList()
    }

    if (showModelSheet) {
        ModelPickerSheet(
            providers = providers,
            selectedModelId = selectedModel,
            favorites = favorites,
            onToggleFavorite = onToggleFavorite,
            onSelect = onSelectModel,
            onAddProvider = { showModelSheet = false; onAddProvider() },
            onDismiss = { showModelSheet = false }
        )
    }
    if (showAttachments) {
        AttachmentSheet(
            onPhoto = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onCamera = { cameraPicker.launch(null) },
            onFile = { filePicker.launch(arrayOf("text/*", "application/json", "image/*")) },
            onCompress = { onCompressHistory { showNote(it) } },
            onExtensions = onOpenExtensions,
            onDismiss = { showAttachments = false }
        )
    }
    if (showThinking) {
        ThinkingDepthSheet(level = reasoning, onLevelChange = onReasoningChange, onDismiss = { showThinking = false })
    }
    if (showSearch) {
        SearchSheet(
            mode = searchMode,
            modelSearchAvailable = modelSearchAvailable,
            localSearchConfigured = localSearchConfigured,
            onModeChange = onSearchModeChange,
            onConfigureLocal = onOpenSearchSettings,
            onDismiss = { showSearch = false }
        )
    }

    Scaffold(
        containerColor = PageBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PageBg),
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu", tint = Ink)
                    }
                },
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().clickable { showModelSheet = true }
                    ) {
                        Text(
                            chat?.title?.takeIf { it.isNotBlank() } ?: "New chat",
                            color = Ink,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 16.sp
                        )
                        Text(
                            selectedModel ?: "Tap to choose a model",
                            color = Muted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                                    copyToClipboard(messages.filter { it.role != MessageRole.SYSTEM }.joinToString("\n\n") { it.content })
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Choose model") },
                                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                                onClick = { showTopMenu = false; showModelSheet = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Compress history") },
                                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    onCompressHistory { showNote(it) }
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth().background(PageBg).navigationBarsPadding().imePadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (pending.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        items(pending, key = { it.id }) { attachment ->
                            PendingChip(attachment) { pending = pending.filterNot { it.id == attachment.id } }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                        // Long messages get an expand button that opens a full-screen editor.
                        if (inputText.length > 120 || inputText.contains('\n')) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Icon(
                                    Icons.Default.OpenInFull,
                                    contentDescription = "Expand message",
                                    tint = Ink.copy(alpha = 0.55f),
                                    modifier = Modifier.size(20.dp).clickable { showFullEditor = true }
                                )
                            }
                        }
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
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconCircle(Icons.Default.Add, "Add to message", active = false) { showAttachments = true }
                            Spacer(Modifier.width(6.dp))
                            IconCircle(Icons.Default.Lightbulb, "Thinking depth", active = reasoning != ReasoningLevel.AUTO) {
                                showThinking = true
                            }
                            Spacer(Modifier.width(6.dp))
                            IconCircle(Icons.Default.Public, "Web search", active = searchMode != SearchMode.OFF) {
                                showSearch = true
                            }
                            Spacer(Modifier.weight(1f))
                            ModelChip(text = selectedModel?.substringAfterLast('/') ?: "Choose model") {
                                showModelSheet = true
                            }
                            Spacer(Modifier.width(6.dp))
                            val canSend = (inputText.isNotBlank() || pending.isNotEmpty()) && !isStreaming
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape)
                                    .background(animateColorAsState(if (canSend) AriAccent else AriTint, tween(200), label = "send").value)
                                    .clickable(enabled = canSend) { send() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = "Send",
                                    tint = if (canSend) Color.White else Ink.copy(alpha = 0.35f),
                                    modifier = Modifier.size(20.dp)
                                )
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
                    when (m.role) {
                        MessageRole.SYSTEM -> AppearIn(m.id) { CompressedNotice() }
                        MessageRole.USER -> AppearIn(m.id) { UserBubble(m.content, m.attachments, fontSize) }
                        else -> AppearIn(m.id) {
                            AssistantBlock(
                                text = m.content,
                                fontSize = fontSize,
                                onCopy = { copyToClipboard(m.content) },
                                onShare = { shareText(m.content) },
                                onBranch = { onBranchMessage(m) },
                                onRetry = { onRegenerate(m) }
                            )
                        }
                    }
                }
                if (currentStreamingContent.isNotEmpty()) {
                    item(key = "streaming") {
                        AssistantBlock(text = currentStreamingContent, streaming = isStreaming, fontSize = fontSize)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = AriTint
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text,
                color = Ink.copy(alpha = 0.75f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp)
            )
            Spacer(Modifier.width(4.dp))
            // Small arrow: opens the list with all models of the provider.
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Open models",
                tint = Ink.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun PendingChip(attachment: Attachment, onRemove: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = if (attachment.type == AttachmentType.IMAGE) Icons.Default.Image else Icons.Default.Description
            Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                attachment.name,
                fontSize = 12.sp,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove attachment", tint = Ink.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun CompressedNotice() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Black.copy(alpha = 0.08f))
        Text("Earlier messages compressed", fontSize = 11.sp, color = Muted)
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Black.copy(alpha = 0.08f))
    }
}

@Composable
private fun UserBubble(text: String, attachments: List<Attachment>, fontSize: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp),
            color = Accent.copy(alpha = 0.11f),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (text.isNotBlank()) {
                    SelectionContainer {
                        Text(text, color = Ink, fontSize = fontSize.sp, lineHeight = (fontSize + 7).sp)
                    }
                }
                if (attachments.isNotEmpty()) {
                    Text(
                        attachments.joinToString(" · ") { it.name },
                        color = Muted,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantBlock(
    text: String,
    fontSize: Int,
    streaming: Boolean = false,
    onCopy: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onBranch: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // SelectionContainer lets the user select and copy part of the reply.
        SelectionContainer {
            Text(text, color = Ink, fontSize = fontSize.sp, lineHeight = (fontSize + 8).sp)
        }
        if (streaming) {
            TypingDots()
        } else if (onCopy != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ActionIcon(Icons.Default.ContentCopy, "Copy", onCopy)
                ActionIcon(Icons.Default.Share, "Share", onShare ?: {})
                ActionIcon(Icons.Default.CallSplit, "Branch", onBranch ?: {})
                ActionIcon(Icons.Default.Refresh, "Retry", onRetry ?: {})
            }
        }
    }
}

@Composable
private fun ActionIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(icon, contentDescription = description, tint = Muted, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun IconCircle(icon: ImageVector, description: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape)
            .background(if (active) Accent.copy(alpha = 0.14f) else AriTint)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = if (active) Accent else Ink.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
    }
}

private val appearedKeys = HashSet<String>()

/** Fades and slides each message in once when it first appears. */
@Composable
private fun AppearIn(key: String, content: @Composable () -> Unit) {
    // Items already shown once skip the entrance animation (LazyColumn recreates items on scroll).
    var visible by remember(key) { mutableStateOf(appearedKeys.contains(key)) }
    LaunchedEffect(key) {
        appearedKeys.add(key)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it / 6 }
    ) {
        content()
    }
}

/** Three pulsing dots shown while the model is generating. */
@Composable
private fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 4.dp)) {
        repeat(3) { i ->
            val alpha by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(600, delayMillis = i * 150), RepeatMode.Reverse),
                label = "dot$i"
            )
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Accent.copy(alpha = alpha)))
        }
    }
}

@Composable
private fun FullScreenMessageEditor(
    text: String,
    onTextChange: (String) -> Unit,
    onCollapse: () -> Unit
) {
    Dialog(
        onDismissRequest = onCollapse,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Icon(
                    Icons.Default.CloseFullscreen,
                    contentDescription = "Collapse",
                    tint = Ink,
                    modifier = Modifier.size(24.dp).clickable { onCollapse() }
                )
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text("Message AriAI", color = Muted) },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = AriAccent,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink
                    )
                )
            }
        }
    }
}
