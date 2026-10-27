package com.ariai.app.ui.screens

import com.ariai.app.util.tx
import com.ariai.app.ui.components.ReplyContent
import com.ariai.app.ui.components.AriBinaryBrainMark
import com.ariai.app.ui.components.ProviderBrandMark
import com.ariai.app.ui.components.providerBrandColor
import com.ariai.app.util.UiLang
import androidx.compose.ui.viewinterop.AndroidView
import com.ariai.app.data.remote.supportsNativeSearch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.ariai.app.ui.theme.*

import android.content.ClipData
import android.content.ContentValues
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Attachment
import com.ariai.app.data.models.AttachmentType
import com.ariai.app.data.models.Chat
import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import com.ariai.app.data.models.MessageStatus
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import com.ariai.app.data.models.ReasoningLevel
import com.ariai.app.data.models.SearchMode
import com.ariai.app.data.remote.PublicUrlFetcher
import com.ariai.app.data.remote.ConnectionResult
import com.ariai.app.util.ReadResult
import com.ariai.app.util.bitmapAttachment
import com.ariai.app.util.readAttachment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Accent: Color @Composable @ReadOnlyComposable get() = AriAccent
private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk
private val Muted: Color @Composable @ReadOnlyComposable get() = AriMuted
private val PageBg: Color @Composable @ReadOnlyComposable get() = AriPaper
private val remoteImageFetcher by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { PublicUrlFetcher() }
private const val MAX_REMOTE_IMAGE_SIDE = 2_048
private const val MAX_REMOTE_IMAGE_PIXELS = 100_000_000L
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    chat: Chat?,
    messages: List<ChatMessage>,
    isStreaming: Boolean,
    isSending: Boolean = false,
    currentStreamingContent: String,
    selectedModel: String?,
    selectedProviderId: String? = chat?.providerId,
    providers: List<Provider> = emptyList(),
    reasoning: ReasoningLevel = ReasoningLevel.AUTO,
    searchMode: SearchMode = SearchMode.OFF,
    favorites: Set<String> = emptySet(),
    modelUsageCounts: Map<Pair<String, String>, Int> = emptyMap(),
    localSearchConfigured: Boolean = false,
    onSendMessage: (String, List<Attachment>, ReasoningLevel, SearchMode, (Boolean) -> Unit) -> Boolean,
    onStopGeneration: () -> Unit = {},
    onBack: () -> Unit,
    onRenameChat: (String) -> Unit = {},
    onBranchMessage: (ChatMessage) -> Unit,
    onRegenerate: (ChatMessage) -> Unit,
    onCopyMessage: (String) -> Unit,
    onOpenDrawer: () -> Unit = {},
    onSelectModel: (String, String) -> Unit = { _, _ -> },
    onEditImage: (ChatMessage, String) -> Unit = { _, _ -> },
    onToggleFavorite: (String, String) -> Unit = { _, _ -> },
    onReasoningChange: (ReasoningLevel) -> Unit = {},
    onSearchModeChange: (SearchMode) -> Unit = {},
    onCompressHistory: ((String) -> Unit) -> Unit = {},
    onNewImageChat: () -> Unit = {},
    chatMode: ChatMode = ChatMode.TEXT,
    onChatModeChange: (ChatMode) -> Unit = {},
    onOpenExtensions: () -> Unit = {},
    onOpenSearchSettings: () -> Unit = {},
    onAddProvider: () -> Unit = {},
    onManageProviders: () -> Unit = {},
    onCheckProvider: suspend (Provider) -> com.ariai.app.data.remote.ConnectionResult,
    onUpdateChatOptions: (Float, Float, String?) -> Unit = { _, _, _ -> },
    fontSize: Int = 15,
    modifier: Modifier = Modifier,
    screenChatId: String? = chat?.id,
    onChatScreenDisposed: (String) -> Unit = {}
) {
    val onChatDisposedState = rememberUpdatedState(onChatScreenDisposed)
    DisposableEffect(screenChatId) {
        val disposedChatId = screenChatId
        onDispose { disposedChatId?.let(onChatDisposedState.value) }
    }

    var inputText by remember { mutableStateOf("") }
    // Voice input with the device's built-in speech recognizer. Spoken text is added to the input box.
    var listening by remember { mutableStateOf(false) }
    val voiceContext = LocalContext.current
    val recognizer = remember {
        if (android.speech.SpeechRecognizer.isRecognitionAvailable(voiceContext)) {
            android.speech.SpeechRecognizer.createSpeechRecognizer(voiceContext)
        } else {
            null
        }
    }
    DisposableEffect(recognizer) { onDispose { recognizer?.destroy() } }
    fun startListening() {
        val engine = recognizer ?: return
        engine.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onResults(results: android.os.Bundle?) {
                val spoken = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (spoken.isNotBlank()) {
                    inputText = listOf(inputText.trim(), spoken.trim()).filter { it.isNotEmpty() }.joinToString(" ")
                }
                listening = false
            }
            override fun onError(error: Int) { listening = false }
            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })
        engine.startListening(
            android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, if (UiLang.code == "fa") "fa-IR" else "en-US")
        )
        listening = true
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening()
    }
    var pending by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var showModelSheet by remember { mutableStateOf(false) }
    var showQuickParameters by remember { mutableStateOf(false) }
    var showFullEditor by remember { mutableStateOf(false) }
    var editingImage by remember { mutableStateOf<ChatMessage?>(null) }
    editingImage?.let { source ->
        EditImageDialog(
            onDismiss = { editingImage = null },
            onConfirm = { instruction ->
                onEditImage(source, instruction)
                editingImage = null
            }
        )
    }
    var showRename by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    if (showRename) {
        val dialogShape = paperPopupShape(26.dp)
        AlertDialog(
            onDismissRequest = { showRename = false },
            modifier = paperPopupModifier(dialogShape),
            shape = dialogShape,
            containerColor = AriCard,
            title = { Text(tx("Rename chat")) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    placeholder = { Text(tx("Chat title")) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRenameChat(renameText)
                    showRename = false
                }) { Text(tx("Save")) }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text(tx("Cancel")) }
            }
        )
    }

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

    val currentProvider = providers.firstOrNull { it.id == selectedProviderId && it.enabled }
        ?: providers.firstOrNull { it.enabled }
    var connectionResult by remember(currentProvider?.id) { mutableStateOf<ConnectionResult?>(null) }
    var checkingConnection by remember(currentProvider?.id) { mutableStateOf(false) }
    val modelSearchAvailable = currentProvider?.let { supportsNativeSearch(it) } == true
    val selectedModelCapabilities = currentProvider?.models?.firstOrNull { it.id == selectedModel }
    val selectedModelLabel = selectedModelCapabilities?.displayName
        ?: selectedModel?.substringAfterLast('/')
        ?: tx("Choose model")
    val selectedModelIsImageOnly = selectedModelCapabilities?.let { it.supportsImageGen && !it.outputsText } == true
    val selectedModelIsVideoOnly = selectedModelCapabilities?.let { it.supportsVideoGen && !it.outputsText } == true
    val selectedModelIsAudioOnly = selectedModelCapabilities?.let { it.supportsAudioGen && !it.outputsText } == true
    val hasStreamingText = currentStreamingContent.isNotBlank()
    val showWaitingStatus = isSending && !hasStreamingText
    val waitingStatus = when {
        chatMode == ChatMode.IMAGE || selectedModelIsImageOnly ->
            tx("Preparing image generation…")
        chatMode == ChatMode.VIDEO || selectedModelIsVideoOnly ->
            tx("Preparing video generation…")
        chatMode == ChatMode.AUDIO || selectedModelIsAudioOnly ->
            tx("Preparing audio generation…")
        searchMode != SearchMode.OFF -> tx("Searching the web…")
        else -> tx("Thinking…")
    }
    val imageGenerationAvailable = providers.any { provider ->
        provider.enabled && provider.models.any { it.supportsImageGen }
    }

    // Follow the newest text only while the user is at the bottom. Once they scroll up, stay where they are.
    var followStream by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) {
                followStream = !listState.canScrollForward
            }
        }
    }
    // Wait for the streamed text to be measured, then keep the bottom of its row inside the viewport.
    // Streaming rows do not animate their size: an animated height would make the scroll chase stale layout.
    suspend fun followToEnd() {
        val itemCount = messages.size + if (hasStreamingText || showWaitingStatus) 1 else 0
        val target = itemCount - 1
        if (target < 0) return

        repeat(4) {
            withFrameNanos { }
            val info = listState.layoutInfo
            if (target >= info.totalItemsCount) return@repeat
            val lastItem = info.visibleItemsInfo.firstOrNull { it.index == target }
            if (lastItem == null) {
                listState.scrollToItem(target)
                return@repeat
            }
            if (lastItem.size <= 0) return@repeat

            val overflow = lastItem.offset + lastItem.size - info.viewportEndOffset
            if (overflow <= 0) return
            // scrollToItem's offset is relative to the first visible item, so add only the hidden tail.
            listState.scrollToItem(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset + overflow
            )
        }
    }

    LaunchedEffect(messages.size, currentStreamingContent.length, showWaitingStatus) {
        if (!hasStreamingText && messages.lastOrNull()?.role == MessageRole.USER) followStream = true
        if (followStream) followToEnd()
    }
    // The keyboard shrinks the list. If the user was at the bottom, keep the newest text visible.
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeBottom) {
        if (followStream) followToEnd()
    }

    fun showNote(text: String) {
        scope.launch { snackbarHostState.showSnackbar(text) }
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(tx("Message"), text))
        showNote(tx("Copied"))
        onCopyMessage(text)
    }

    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    fun saveImage(path: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            showNote(tx("Saving needs Android 10 or newer"))
            return
        }
        scope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    val file = File(path)
                    val extension = file.extension.lowercase().takeIf { it in setOf("png", "jpg", "jpeg", "webp", "gif") } ?: "png"
                    val mimeType = when (extension) {
                        "jpg", "jpeg" -> "image/jpeg"
                        "webp" -> "image/webp"
                        "gif" -> "image/gif"
                        else -> "image/png"
                    }
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "ariai_${System.currentTimeMillis()}.$extension")
                        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AriAI")
                    }
                    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: throw java.io.IOException("Could not create image in MediaStore")
                    try {
                        val output = context.contentResolver.openOutputStream(uri)
                            ?: throw java.io.IOException("Could not open image output stream")
                        output.use { out -> file.inputStream().use { it.copyTo(out) } }
                    } catch (e: Exception) {
                        context.contentResolver.delete(uri, null, null)
                        throw e
                    }
                }
                Result.success(Unit)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            result.onSuccess { showNote(tx("Saved to Pictures/AriAI")) }
                .onFailure { showNote(tx("Save failed")) }
        }
    }

    fun addFiles(uris: List<Uri>) {
        scope.launch {
            for (uri in uris) {
                when (val result = withContext(Dispatchers.IO) { readAttachment(context, uri) }) {
                    is ReadResult.Ok -> pending = pending + result.attachment
                    is ReadResult.Error -> showNote(result.message)
                }
            }
        }
    }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        if (uris.isNotEmpty()) addFiles(uris)
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) addFiles(uris)
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
        val submittedAttachments = pending
        if ((text.isBlank() && submittedAttachments.isEmpty()) || isStreaming || isSending) return
        onSendMessage(text, submittedAttachments, reasoning, searchMode) { saved ->
            if (saved && inputText.trim() == text && pending == submittedAttachments) {
                inputText = ""
                pending = emptyList()
            }
        }
    }

    fun launchPickerAfterSheetCloses(launch: () -> Unit) {
        // Let the modal finish dismissing before handing control to an external Android activity.
        scope.launch {
            delay(350)
            launch()
        }
    }

    if (showModelSheet) {
        ModelPickerSheet(
            providers = providers,
            selectedProviderId = selectedProviderId,
            selectedModelId = selectedModel,
            favorites = favorites,
            modelUsageCounts = modelUsageCounts,
            onToggleFavorite = onToggleFavorite,
            onSelect = onSelectModel,
            onAddProvider = { showModelSheet = false; onAddProvider() },
            onManageProviders = { showModelSheet = false; onManageProviders() },
            onCheckProvider = onCheckProvider,
            onDismiss = { showModelSheet = false }
        )
    }
    if (showQuickParameters) {
        QuickParametersSheet(
            temperature = chat?.temperature ?: 0.7f,
            topP = chat?.topP ?: 1.0f,
            systemPrompt = chat?.systemPrompt,
            onSave = { temperature, topP, systemPrompt ->
                onUpdateChatOptions(temperature, topP, systemPrompt)
                showQuickParameters = false
            },
            onDismiss = { showQuickParameters = false }
        )
    }
    if (showAttachments) {
        AttachmentSheet(
            onPhoto = {
                launchPickerAfterSheetCloses {
                    mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }
            },
            onCamera = { launchPickerAfterSheetCloses { cameraPicker.launch(null) } },
            onFile = {
                launchPickerAfterSheetCloses {
                    filePicker.launch(arrayOf("text/*", "application/json", "image/*"))
                }
            },
            onCompress = { onCompressHistory { showNote(it) } },
            onExtensions = onOpenExtensions,
            onImage = {
                if (imageGenerationAvailable) onChatModeChange(ChatMode.IMAGE)
                else showNote(tx("No image model found. Add an image model, or fetch the models in provider settings."))
            },
            onVideo = {
                if (providers.any { p -> p.enabled && p.models.any { it.supportsVideoGen } }) onChatModeChange(ChatMode.VIDEO)
                else showNote(tx("No video model. Fetch models in provider settings."))
            },
            onAudio = {
                if (providers.any { p -> p.enabled && p.models.any { it.supportsAudioGen } }) onChatModeChange(ChatMode.AUDIO)
                else showNote(tx("No audio model. Fetch models in provider settings."))
            },
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
            Column(modifier = Modifier.fillMaxWidth().background(PageBg)) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PageBg),
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = tx("Open menu"), tint = Ink)
                        }
                    },
                    title = {
                        ChatHeaderTitle(
                            chatTitle = chat?.title?.takeIf { it.isNotBlank() } ?: tx("New chat")
                        )
                    },
                    actions = {
                        val contextLimit = selectedModelCapabilities?.contextWindow?.takeIf { it > 0 } ?: 0
                        if (contextLimit > 0) {
                            val latestUserIndex = messages.indexOfLast { it.role == MessageRole.USER }
                            val latestAssistantIndex = messages.indexOfLast { it.role == MessageRole.ASSISTANT }
                            val actualPromptTokens = messages.getOrNull(latestAssistantIndex)
                                ?.takeIf { latestAssistantIndex > latestUserIndex && it.status != MessageStatus.ERROR }
                                ?.usage?.promptTokens?.takeIf { it > 0 }
                            val visibleTokens = messages.filter { it.role != MessageRole.SYSTEM }.sumOf { message ->
                                ((message.content.length + 3).toLong() / 4L) + message.attachments.sumOf { attachment ->
                                    ((attachment.textContent?.length ?: 0).toLong() + 3L) / 4L + when (attachment.type) {
                                        AttachmentType.IMAGE -> 768L
                                        AttachmentType.VIDEO -> 2048L
                                        else -> 0L
                                    }
                                }
                            } + ((chat?.systemPrompt?.length ?: 0).toLong() + 3L) / 4L
                            val tokenCount = actualPromptTokens ?: visibleTokens.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                            ContextUsageIndicator(
                                tokens = tokenCount,
                                contextWindow = contextLimit,
                                approximate = actualPromptTokens == null,
                                onClick = {
                                    val prefix = if (actualPromptTokens == null) tx("Approx.") + " " else ""
                                    showNote("$prefix$tokenCount / $contextLimit ${tx("tokens in context")}")
                                }
                            )
                        }
                        IconButton(onClick = { showQuickParameters = true }) {
                            Icon(Icons.Default.Tune, contentDescription = tx("Quick parameters"), tint = Ink)
                        }
                        Box {
                            IconButton(onClick = { showTopMenu = true }) {
                                Icon(Icons.Default.MoreHoriz, contentDescription = tx("More"), tint = Ink)
                            }
                            val menuShape = paperPopupShape(18.dp, 12.dp)
                            DropdownMenu(
                                expanded = showTopMenu,
                                onDismissRequest = { showTopMenu = false },
                                modifier = Modifier.background(AriCard, menuShape).then(paperPopupModifier(menuShape, 12.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (checkingConnection) tx("Checking…") else tx("Check connection")) },
                                    leadingIcon = { Icon(Icons.Default.NetworkCheck, contentDescription = null) },
                                    enabled = currentProvider != null && !checkingConnection,
                                    onClick = {
                                        showTopMenu = false
                                        val provider = currentProvider
                                        if (provider != null) scope.launch {
                                            checkingConnection = true
                                            try {
                                                connectionResult = onCheckProvider(provider)
                                                when (val result = connectionResult) {
                                                    is ConnectionResult.Ok -> showNote("${tx("Connected")} · ${result.durationMs} ms · ${result.models} ${tx("models")}")
                                                    is ConnectionResult.Failed -> showNote(result.problem)
                                                    null -> Unit
                                                }
                                            } catch (cancelled: CancellationException) {
                                                throw cancelled
                                            } catch (error: Exception) {
                                                connectionResult = ConnectionResult.Failed(tx("Could not connect"), error.message.orEmpty())
                                                showNote(tx("Could not connect"))
                                            } finally {
                                                checkingConnection = false
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(tx("Rename chat")) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showTopMenu = false
                                        renameText = chat?.title ?: ""
                                        showRename = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(tx("Copy conversation")) },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                    onClick = {
                                        showTopMenu = false
                                        copyToClipboard(messages.filter { it.role != MessageRole.SYSTEM }.joinToString("\n\n") { it.content })
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(tx("Image chat")) },
                                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = AriAccent) },
                                    onClick = {
                                        showTopMenu = false
                                        if (imageGenerationAvailable) onNewImageChat()
                                        else showNote(tx("No image model found. Add an image model, or fetch the models in provider settings."))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(tx("Choose model")) },
                                    leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = AriAccent) },
                                    onClick = { showTopMenu = false; showModelSheet = true }
                                )
                                DropdownMenuItem(
                                    text = { Text(tx("Compress history")) },
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
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .testTag("chat-model-selector-container")
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ChatModelSelector(
                        provider = currentProvider,
                        modelName = selectedModelLabel,
                        onClick = { showModelSheet = true },
                        modifier = Modifier.testTag("chat-model-floater")
                    )
                }
            }
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

                // Keep model selection above the conversation so it never crowds message entry.
                val composerShape = RoundedCornerShape(26.dp)
                Surface(
                    shape = composerShape,
                    color = AriCard,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(tween(220, easing = FastOutSlowInEasing))
                        .border(1.dp, AriLine.copy(alpha = 0.9f), composerShape)
                        .testTag("chat-composer-container")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            .animateContentSize(tween(220, easing = FastOutSlowInEasing))
                    ) {
                        // Long messages get an expand button that opens a full-screen editor.
                        if (inputText.length > 120 || inputText.contains('\n')) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Icon(
                                    Icons.Default.OpenInFull,
                                    contentDescription = tx("Expand message"),
                                    tint = Ink.copy(alpha = 0.55f),
                                    modifier = Modifier.size(20.dp).clickable { showFullEditor = true }
                                )
                            }
                        }
                        if (chatMode != ChatMode.TEXT) {
                            Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp)) {
                                ModeChip(mode = chatMode, onClose = { onChatModeChange(ChatMode.TEXT) }, modifier = Modifier)
                            }
                        }
                        val canSend = (inputText.isNotBlank() || pending.isNotEmpty()) && !isStreaming && !isSending
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Resolve paragraph direction from the input itself so Persian remains RTL
                            // even when the app's interface language is English.
                            TextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text(tx("Message AriAI"), color = Muted) },
                                textStyle = LocalTextStyle.current.copy(
                                    textAlign = TextAlign.Start,
                                    textDirection = TextDirection.ContentOrLtr
                                ),
                                modifier = Modifier.weight(1f).testTag("chat-composer"),
                                maxLines = 12,
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
                            if (isStreaming || isSending) {
                                // While a request is pending or a reply is coming in, allow cancellation.
                                Surface(
                                    onClick = { onStopGeneration() },
                                    modifier = Modifier.size(42.dp).testTag("chat-send-button"),
                                    shape = CircleShape,
                                    color = AriAccentDark,
                                    contentColor = Color.White,
                                    shadowElevation = 3.dp,
                                    tonalElevation = 0.dp
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Stop, contentDescription = tx("Stop"), modifier = Modifier.size(18.dp))
                                    }
                                }
                            } else {
                                Surface(
                                    onClick = { send() },
                                    enabled = canSend,
                                    modifier = Modifier.size(42.dp).testTag("chat-send-button"),
                                    shape = CircleShape,
                                    color = if (canSend) AriAccent else AriAccent.copy(alpha = 0.56f),
                                    contentColor = Color.White,
                                    shadowElevation = if (canSend) 4.dp else 1.dp,
                                    tonalElevation = 0.dp
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = tx("Send"),
                                            tint = Color.White.copy(alpha = 0.94f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconCircle(Icons.Default.Add, tx("Add to message"), active = false) { showAttachments = true }
                            Spacer(Modifier.width(6.dp))
                            IconCircle(Icons.Default.Public, tx("Web search"), active = searchMode != SearchMode.OFF) {
                                showSearch = true
                            }
                            Spacer(Modifier.width(6.dp))
                            IconCircle(Icons.Default.Mic, tx("Voice input"), active = listening) {
                                when {
                                    listening -> recognizer?.stopListening()
                                    androidx.core.content.ContextCompat.checkSelfPermission(
                                        context, android.Manifest.permission.RECORD_AUDIO
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> startListening()
                                    else -> micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            }
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (messages.isEmpty() && !hasStreamingText && !isSending) {
            val compactWelcome = imeBottom > 0
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (compactWelcome) 6.dp else 10.dp)
                ) {
                    Box(
                        modifier = Modifier.size(if (compactWelcome) 38.dp else 48.dp)
                            .clip(CircleShape)
                            .background(AriAccentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AriAccentDark,
                            modifier = Modifier.size(if (compactWelcome) 18.dp else 22.dp)
                        )
                    }
                    Text(
                        "AriAI",
                        color = Muted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        tx("How can I help you?"),
                        color = Ink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (compactWelcome) 17.sp else 19.sp,
                        textAlign = TextAlign.Center
                    )
                    if (!compactWelcome) {
                        Text(
                            tx("Type a message below to start."),
                            color = Muted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.testTag("welcome-suggestions"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = { inputText = tx("Help me brainstorm a few ideas.") },
                                label = { Text(tx("Brainstorm")) },
                                leadingIcon = { Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = AriCard,
                                    labelColor = Ink,
                                    leadingIconContentColor = AriAccent
                                )
                            )
                            AssistChip(
                                onClick = { inputText = tx("Summarize this for me.") },
                                label = { Text(tx("Summarize")) },
                                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = AriCard,
                                    labelColor = Ink,
                                    leadingIconContentColor = AriAccent
                                )
                            )
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Keep this control in normal layout flow so it reserves room above the first
                // message instead of obscuring the message text beneath it.
                ThinkingChip(
                    level = reasoning,
                    onClick = { showThinking = true },
                    modifier = Modifier
                        .padding(start = 12.dp, top = 6.dp, bottom = 4.dp)
                        .testTag("chat-thinking-chip")
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(count = messages.size, key = { i -> messages[i].id }) { i ->
                        val m = messages[i]
                        val isError = m.status == MessageStatus.ERROR
                        val retryUserId = if (isError) m.parentId else null
                        val associatedUserId = retryUserId ?: messages.take(i)
                            .lastOrNull { it.role == MessageRole.USER }
                            ?.id
                        val canRetry = m.role == MessageRole.ASSISTANT && associatedUserId?.let { userId ->
                            messages.any { it.id == userId && it.role == MessageRole.USER }
                        } == true
                        when (m.role) {
                            MessageRole.SYSTEM -> AppearIn(m.id) { CompressedNotice() }
                            MessageRole.USER -> AppearIn(m.id) { UserBubble(m.content, m.attachments, fontSize) }
                            else -> AppearIn(m.id) {
                                AssistantBlock(
                                    text = m.content,
                                    fontSize = fontSize,
                                    imagePath = m.imagePath,
                                    isError = isError,
                                    retryable = canRetry && !isStreaming && !isSending,
                                    onCopy = { copyToClipboard(m.content) },
                                    onShare = { shareText(m.content) },
                                    onEdit = { editingImage = m },
                                    onSave = { m.imagePath?.let { saveImage(it) } },
                                    onBranch = { onBranchMessage(m) },
                                    onRetry = { onRegenerate(m) }
                                )
                            }
                        }
                    }
                    if (hasStreamingText) {
                        item(key = "streaming") {
                            AssistantBlock(text = currentStreamingContent, streaming = isStreaming, fontSize = fontSize)
                        }
                    } else if (showWaitingStatus) {
                        item(key = "waiting-status") { WaitingStatus(waitingStatus) }
                    }
                }
            }

        }
    }
}

@Composable
private fun ChatHeaderTitle(chatTitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("chat-title-header").padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProviderBrandMark(provider = null, size = 30.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            chatTitle,
            modifier = Modifier.weight(1f),
            color = Ink,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun ChatModelSelector(
    provider: Provider?,
    modelName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val providerAccent = providerBrandColor(provider)
    val label = when {
        provider == null -> tx("Choose model")
        modelName.isBlank() || modelName == tx("Choose model") -> "AriAI (${provider.name})"
        else -> "AriAI ($modelName)"
    }
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        modifier = modifier.widthIn(min = 144.dp, max = 272.dp).height(40.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = tx("Choose model")
                stateDescription = "${provider?.name ?: tx("No provider")} · $modelName"
            },
        shape = shape,
        color = Color.White.copy(alpha = 0.97f),
        contentColor = Ink,
        shadowElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, providerAccent.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AriBinaryBrainMark(size = 22.dp, tint = AriAccentDark)
            Text(
                text = label,
                modifier = Modifier.widthIn(max = 196.dp),
                color = AriAccentDark,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = AriAccentDark,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ContextUsageIndicator(
    tokens: Int,
    contextWindow: Int,
    approximate: Boolean,
    onClick: () -> Unit
) {
    val fraction = if (contextWindow > 0) (tokens.toFloat() / contextWindow).coerceIn(0f, 1f) else 0f
    Box(
        modifier = Modifier.size(42.dp).clip(CircleShape).clickable(onClick = onClick).testTag("chat-context-usage"),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = fraction,
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.5.dp,
            color = if (fraction >= 0.9f) AriError else AriAccent,
            trackColor = AriTint
        )
        Text(
            if (approximate) "~${(fraction * 100).toInt()}%" else "${(fraction * 100).toInt()}%",
            color = Ink,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ModelChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = AriAccentSoft
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
                contentDescription = tx("Open models"),
                tint = Ink.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun PendingChip(attachment: Attachment, onRemove: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = AriCard, shadowElevation = 0.dp) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (attachment.type) {
                AttachmentType.IMAGE -> Icons.Default.Image
                AttachmentType.VIDEO -> Icons.Default.PlayArrow
                else -> Icons.Default.Description
            }
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
                Icon(Icons.Default.Close, contentDescription = tx("Remove attachment"), tint = Ink.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
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
        HorizontalDivider(modifier = Modifier.weight(1f), color = AriInk.copy(alpha = 0.08f))
        Text(tx("Earlier messages compressed"), fontSize = 11.sp, color = Muted)
        HorizontalDivider(modifier = Modifier.weight(1f), color = AriInk.copy(alpha = 0.08f))
    }
}

/** Shows a sent photo inside the bubble, read from its stored file or from the in-memory bytes. */
@Composable
private fun AttachmentThumb(attachment: Attachment) {
    val bitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = attachment.uri,
        key2 = attachment.base64Data
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }
                val uri = attachment.uri
                if (uri != null && java.io.File(uri).exists()) {
                    android.graphics.BitmapFactory.decodeFile(uri, options)
                } else {
                    attachment.base64Data?.let {
                        val bytes = android.util.Base64.decode(it, android.util.Base64.DEFAULT)
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    }
                }
            }.getOrNull()?.asImageBitmap()
        }
    }
    val currentBitmap = bitmap
    if (currentBitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = currentBitmap,
            contentDescription = attachment.name,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.size(180.dp).clip(RoundedCornerShape(14.dp))
        )
    } else {
        Text(attachment.name, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                attachments.filter { it.type == AttachmentType.IMAGE }.forEach { AttachmentThumb(it) }
                val others = attachments.filter { it.type != AttachmentType.IMAGE }
                if (others.isNotEmpty()) {
                    Text(
                        others.joinToString(" · ") { it.name },
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
private fun WaitingStatus(label: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp)) {
        Surface(shape = RoundedCornerShape(18.dp), color = AriTint) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Accent)
                Spacer(Modifier.width(9.dp))
                Text(label, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun AssistantBlock(
    text: String,
    fontSize: Int,
    streaming: Boolean = false,
    imagePath: String? = null,
    isError: Boolean = false,
    retryable: Boolean = false,
    onCopy: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onBranch: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth().then(if (streaming) Modifier else Modifier.animateContentSize()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (imagePath != null) {
            when {
                isVideoPath(imagePath) -> VideoCard(imagePath)
                isAudioPath(imagePath) -> AudioCard(imagePath)
                else -> GeneratedImageView(imagePath)
            }
        }
        // Models often answer with image links; show those as pictures instead of raw URLs.
        val imageUrls = remember(text) { extractImageUrls(text) }
        val visibleText = remember(text) { if (imageUrls.isEmpty()) text else stripImageUrls(text) }
        if (visibleText.isNotBlank()) {
            // SelectionContainer lets the user select and copy part of the reply.
            SelectionContainer {
                ReplyContent(visibleText, fontSize.toFloat(), Ink)
            }
        }
        imageUrls.forEach { RemoteImage(it) }
        if (imagePath != null && !isVideoPath(imagePath) && !isAudioPath(imagePath)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onEdit?.invoke() }) {
                    Text(tx("Edit"), color = Ink.copy(alpha = 0.7f), fontSize = 13.sp)
                }
                TextButton(onClick = { onSave?.invoke() }) {
                    Text(tx("Save"), color = Ink.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
        } else if (onCopy != null && isError) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionIcon(Icons.Default.ContentCopy, tx("Copy"), onCopy)
                if (retryable && onRetry != null) {
                    TextButton(
                        onClick = onRetry,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(tx("Retry"), fontSize = 13.sp)
                    }
                }
            }
        } else if (onCopy != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ActionIcon(Icons.Default.ContentCopy, tx("Copy"), onCopy)
                ActionIcon(Icons.Default.Share, tx("Share"), onShare ?: {})
                ActionIcon(Icons.Default.CallSplit, tx("Branch"), onBranch ?: {})
                if (retryable && onRetry != null) ActionIcon(Icons.Default.Refresh, tx("Retry"), onRetry)
            }
        }
    }
}

/** Shows a generated image. Decoding runs off the main thread and is downsampled to save memory. */
@Composable
private fun GeneratedImageView(path: String) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) {
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeFile(path, options)?.asImageBitmap()
        }
    }
    var full by remember { mutableStateOf(false) }
    bitmap?.let { bmp ->
        Image(
            bitmap = bmp,
            contentDescription = tx("Generated image"),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { full = true }
        )
    }
    if (full) bitmap?.let { FullImageDialog(it) { full = false } }
}

/** Full-screen view of an image; tap anywhere to close. */
@Composable
private fun FullImageDialog(bitmap: ImageBitmap, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black).clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun EditImageDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val dialogShape = paperPopupShape(26.dp)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = paperPopupModifier(dialogShape),
        shape = dialogShape,
        containerColor = AriCard,
        title = { Text(tx("Edit image")) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(tx("What should change?")) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) { Text(tx("Send")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tx("Cancel")) }
        }
    )
}

@Composable
private fun ActionIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(icon, contentDescription = description, tint = Muted, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun IconCircle(icon: ImageVector, description: String, active: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = if (active) AriAccentSoft else AriTint,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "composer-action-background"
    )
    val iconColor by animateColorAsState(
        targetValue = if (active) Accent else Ink.copy(alpha = 0.7f),
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "composer-action-foreground"
    )
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape)
            .background(backgroundColor)
            .animateContentSize(tween(180, easing = FastOutSlowInEasing))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = iconColor, modifier = Modifier.size(20.dp))
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
        val pageShape = paperPopupShape(28.dp)
        Surface(
            modifier = Modifier.fillMaxSize().padding(8.dp).then(paperPopupModifier(pageShape)),
            shape = pageShape,
            color = AriCard
        ) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Icon(
                    Icons.Default.CloseFullscreen,
                    contentDescription = tx("Collapse"),
                    tint = Ink,
                    modifier = Modifier.size(24.dp).clickable { onCollapse() }
                )
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text(tx("Message AriAI"), color = Muted) },
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

private fun isVideoPath(path: String) = path.endsWith(".mp4", ignoreCase = true)
private fun isAudioPath(path: String) = path.endsWith(".mp3", ignoreCase = true)

/** Small thin label in the chat's top-left corner: shows the active mode, closes back to text. */
@Composable
private fun ModeChip(mode: ChatMode, onClose: () -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(AriCard)
            .border(0.5.dp, AriLine, RoundedCornerShape(50))
            .padding(start = 10.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            when (mode) {
                ChatMode.VIDEO -> tx("Video")
                ChatMode.AUDIO -> tx("Audio")
                else -> tx("Image")
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            color = AriInk
        )
        IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = tx("Close"), tint = AriMuted, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun VideoCard(path: String) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(16.dp), color = AriCard, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(AriTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AriInk)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(tx("Video"), fontWeight = FontWeight.SemiBold, color = AriInk)
                Text(tx("Generated video"), fontSize = 12.sp, color = AriMuted)
            }
            TextButton(onClick = { playing = true }) { Text(tx("Open"), color = AriInk, fontWeight = FontWeight.SemiBold) }
        }
    }
    if (playing) VideoPlayerDialog(path) { playing = false }
}

/** Generated audio: play and pause in place. */
@Composable
private fun AudioCard(path: String) {
    val context = LocalContext.current
    val player = remember(path) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(path))))
            prepare()
        }
    }
    var playing by remember(path) { mutableStateOf(false) }
    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    Surface(shape = RoundedCornerShape(16.dp), color = AriCard, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = { if (player.isPlaying) player.pause() else player.play() },
                modifier = Modifier.size(44.dp).clip(CircleShape).background(AriTint)
            ) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) tx("Pause") else tx("Play"),
                    tint = AriInk
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(tx("Audio"), fontWeight = FontWeight.SemiBold, color = AriInk)
                Text(tx("Generated audio"), fontSize = 12.sp, color = AriMuted)
            }
        }
    }
}

/** Plays a generated video inside the app with the Media3 player and its standard controls. */
@Composable
private fun VideoPlayerDialog(path: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val player = remember(path) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(path))))
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    androidx.media3.ui.PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Default.Close, contentDescription = tx("Close"), tint = Color.White)
            }
        }
    }
}

private val ImageLinkRegex = Regex(
    """!\[[^\]]*\]\((https?://[^\s)]+)\)|(https?://media\.pollinations\.ai/[^\s)]+|https?://[^\s)]+\.(?:png|jpe?g|webp|gif)(?:\?[^\s)]*)?)""",
    RegexOption.IGNORE_CASE
)

private fun extractImageUrls(text: String): List<String> =
    ImageLinkRegex.findAll(text).map { m -> m.groupValues[1].ifEmpty { m.groupValues[2] } }.distinct().toList()

private fun stripImageUrls(text: String): String = ImageLinkRegex.replace(text, "").trim()

private fun decodeRemoteImage(bytes: ByteArray): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val width = bounds.outWidth
    val height = bounds.outHeight
    if (width <= 0 || height <= 0 || width.toLong() * height.toLong() > MAX_REMOTE_IMAGE_PIXELS) return null

    var sampleSize = 1
    while (width / sampleSize > MAX_REMOTE_IMAGE_SIDE || height / sampleSize > MAX_REMOTE_IMAGE_SIDE) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
}

/** Safely downloads a bounded public image off the main thread and shows a downsampled preview. */
@Composable
private fun RemoteImage(url: String) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = url) {
        value = withContext(Dispatchers.IO) {
            try {
                decodeRemoteImage(remoteImageFetcher.fetchImageBytes(url))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
    }
    val shape = RoundedCornerShape(16.dp)
    var full by remember { mutableStateOf(false) }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().clip(shape).clickable { full = true }
        )
    } ?: Box(
        modifier = Modifier.fillMaxWidth().height(180.dp).clip(shape).background(AriTint),
        contentAlignment = Alignment.Center
    ) {
        Text(tx("Loading image…"), fontSize = 12.sp, color = AriMuted)
    }
    if (full) bitmap?.let { FullImageDialog(it) { full = false } }
}

/** Thin label in the chat's top-left corner: current thinking depth, opens the depth sheet. */
@Composable
private fun ThinkingChip(level: ReasoningLevel, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(AriCard)
            .border(0.5.dp, AriLine, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AriMuted, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            level.name.lowercase().replaceFirstChar { it.uppercaseChar() },
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            color = AriInk
        )
    }
}
