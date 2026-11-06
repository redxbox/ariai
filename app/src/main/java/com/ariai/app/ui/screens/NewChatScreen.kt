package com.ariai.app.ui.screens

import com.ariai.app.util.tx
import com.ariai.app.ui.components.ReplyContent
import com.ariai.app.ui.components.ProviderBrandMark
import com.ariai.app.util.UiLang
import androidx.compose.ui.viewinterop.AndroidView

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.ariai.app.ui.theme.*

import android.content.ClipData
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.util.Locale
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.DocumentTextExtractor
import com.ariai.app.data.models.Attachment
import com.ariai.app.data.models.AttachmentType
import com.ariai.app.data.models.Agent
import com.ariai.app.data.models.AgentTool
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
import com.ariai.app.data.remote.ToolExecutionPhase
import com.ariai.app.data.remote.ToolExecutionUpdate
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
    toolExecutionUpdates: List<ToolExecutionUpdate> = emptyList(),
    selectedModel: String?,
    selectedProviderId: String? = chat?.providerId,
    providers: List<Provider> = emptyList(),
    reasoning: ReasoningLevel = ReasoningLevel.AUTO,
    searchMode: SearchMode = SearchMode.OFF,
    searchProvider: String? = null,
    searchKeys: Map<String, String> = emptyMap(),
    biometricLockEnabled: Boolean = false,
    favorites: Set<String> = emptySet(),
    modelUsageCounts: Map<Pair<String, String>, Int> = emptyMap(),
    onSendMessage: (String, List<Attachment>, ReasoningLevel, SearchMode, (Boolean) -> Unit) -> Boolean,
    onStopGeneration: () -> Unit = {},
    onBack: () -> Unit,
    onRenameChat: (String) -> Unit = {},
    onBranchMessage: (ChatMessage) -> Unit,
    onNavigateToBranch: (String) -> Unit = {},
    onRegenerate: (ChatMessage) -> Unit,
    onCopyMessage: (String) -> Unit,
    onOpenDrawer: () -> Unit = {},
    onSelectModel: (String, String) -> Unit = { _, _ -> },
    onEditImage: (ChatMessage, String, File?) -> Unit = { _, _, mask -> mask?.delete() },
    onToggleFavorite: (String, String) -> Unit = { _, _ -> },
    onReasoningChange: (ReasoningLevel) -> Unit = {},
    onSearchModeChange: (SearchMode) -> Unit = {},
    onSearchProviderChange: (String) -> Unit = {},
    onSaveSearchKey: (String, String) -> Unit = { _, _ -> },
    onCompressHistory: ((String) -> Unit) -> Unit = {},
    onNewImageChat: () -> Unit = {},
    chatMode: ChatMode = ChatMode.TEXT,
    onChatModeChange: (ChatMode) -> Unit = {},
    onOpenExtensions: () -> Unit = {},
    onAddProvider: () -> Unit = {},
    onManageProviders: () -> Unit = {},
    onSaveNativeSearchProvider: (Provider, (Provider?) -> Unit) -> Unit = { _, onSaved -> onSaved(null) },
    onCheckProvider: suspend (Provider) -> com.ariai.app.data.remote.ConnectionResult,
    onUpdateChatOptions: (Float, Float, String?) -> Unit = { _, _, _ -> },
    fontSize: Int = 15,
    modifier: Modifier = Modifier,
    screenChatId: String? = chat?.id,
    onChatScreenDisposed: (String) -> Unit = {},
    activeAgent: Agent? = null,
    onOpenAgents: () -> Unit = {}
) {
    val onChatDisposedState = rememberUpdatedState(onChatScreenDisposed)
    DisposableEffect(screenChatId) {
        val disposedChatId = screenChatId
        onDispose { disposedChatId?.let(onChatDisposedState.value) }
    }

    var inputText by remember { mutableStateOf("") }
    // Voice input with the device's built-in speech recognizer. Spoken text is added to the input box.
    var listening by remember { mutableStateOf(false) }
    var showVoiceDisclosure by remember { mutableStateOf(false) }
    var voiceDisclosureAccepted by remember { mutableStateOf(false) }
    val voiceContext = LocalContext.current
    val onDeviceSpeechRecognitionAvailable = remember(voiceContext) {
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            android.speech.SpeechRecognizer.isOnDeviceRecognitionAvailable(voiceContext)
    }
    val recognizer = remember(voiceContext, onDeviceSpeechRecognitionAvailable) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && onDeviceSpeechRecognitionAvailable ->
                android.speech.SpeechRecognizer.createOnDeviceSpeechRecognizer(voiceContext)
            android.speech.SpeechRecognizer.isRecognitionAvailable(voiceContext) ->
                android.speech.SpeechRecognizer.createSpeechRecognizer(voiceContext)
            else -> null
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
        val preferredSpeechLanguage = if (
            UiLang.code == "fa" || java.util.Locale.getDefault().language == "fa"
        ) {
            "fa-IR"
        } else {
            "en-US"
        }
        val recognitionIntent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, preferredSpeechLanguage)

            // The UI can be English while the spoken language is Persian. Let Android 14+ switch
            // between both supported languages instead of forcing the recognizer to transcribe English.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val voiceLanguages = arrayListOf("fa-IR", "en-US")
                putExtra(android.speech.RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
                putStringArrayListExtra(
                    android.speech.RecognizerIntent.EXTRA_LANGUAGE_DETECTION_ALLOWED_LANGUAGES,
                    voiceLanguages
                )
                putExtra(
                    android.speech.RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH,
                    android.speech.RecognizerIntent.LANGUAGE_SWITCH_BALANCED
                )
                putStringArrayListExtra(
                    android.speech.RecognizerIntent.EXTRA_LANGUAGE_SWITCH_ALLOWED_LANGUAGES,
                    voiceLanguages
                )
            }
        }
        engine.startListening(recognitionIntent)
        listening = true
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening()
    }
    val requestVoiceInput = {
        if (!voiceDisclosureAccepted) {
            showVoiceDisclosure = true
        } else if (
            androidx.core.content.ContextCompat.checkSelfPermission(
                voiceContext, android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            startListening()
        } else {
            micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }
    if (showVoiceDisclosure) {
        VoiceInputDisclosureDialog(
            onDeviceRecognition = onDeviceSpeechRecognitionAvailable,
            onDismiss = { showVoiceDisclosure = false },
            onContinue = {
                showVoiceDisclosure = false
                voiceDisclosureAccepted = true
                if (
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        voiceContext, android.Manifest.permission.RECORD_AUDIO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) startListening() else micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        )
    }
    var pending by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var showModelSheet by remember { mutableStateOf(false) }
    var showQuickParameters by remember { mutableStateOf(false) }
    var showFullEditor by remember { mutableStateOf(false) }
    var editingImage by remember { mutableStateOf<ChatMessage?>(null) }
    editingImage?.let { source ->
        EditImageDialog(
            source = source,
            onDismiss = { editingImage = null },
            onConfirm = { instruction, mask ->
                onEditImage(source, instruction, mask)
                editingImage = null
            }
        )
    }
    var showRename by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    val startRenamingChat = {
        renameText = chat?.title.orEmpty()
        showRename = true
    }
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
                    modifier = Modifier.testTag("chat-rename-field"),
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
    var showNonGeminiVideoWarning by remember { mutableStateOf(false) }
    var pendingVideoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var mediaPickerVideoConfirmed by remember { mutableStateOf(false) }
    var showThinking by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchSheetAuthenticated by remember(biometricLockEnabled) { mutableStateOf(!biometricLockEnabled) }
    var sensitiveAuthAttempt by remember { mutableIntStateOf(0) }
    val currentShowSearch by rememberUpdatedState(showSearch)
    var showTopMenu by remember { mutableStateOf(false) }
    DisposableEffect(biometricLockEnabled) {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        var backgrounded = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                backgrounded = true
                if (biometricLockEnabled && currentShowSearch) searchSheetAuthenticated = false
            } else if (event == Lifecycle.Event.ON_START && backgrounded) {
                backgrounded = false
                if (biometricLockEnabled && currentShowSearch) sensitiveAuthAttempt++
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val requestedProvider = providers.firstOrNull { it.id == selectedProviderId }
    val currentProvider = requestedProvider?.takeIf { it.enabled } ?: providers.firstOrNull { it.enabled }
    var connectionResult by remember(requestedProvider?.id) { mutableStateOf<ConnectionResult?>(null) }
    var checkingConnection by remember(requestedProvider?.id) { mutableStateOf(false) }
    val selectedModelCapabilities = requestedProvider?.models?.firstOrNull { it.id == selectedModel }
    val selectedModelReady = requestedProvider?.enabled == true && selectedModelCapabilities != null
    val modelSetupMessage = when {
        providers.isEmpty() -> tx("Add your first AI provider to start chatting")
        requestedProvider != null && !requestedProvider.enabled -> tx("Selected provider is disabled. Choose another model.")
        currentProvider == null -> tx("No provider is enabled. Enable one to start chatting.")
        currentProvider?.models?.isEmpty() == true -> tx("No models available. Manage providers to fetch models.")
        else -> tx("Tap to choose a model")
    }
    val selectedModelLabel = selectedModelCapabilities?.displayName
        ?: selectedModel?.substringAfterLast('/')
        ?: tx("Choose model")
    val selectedModelIsImageOnly = selectedModelCapabilities?.let { it.supportsImageGen && !it.outputsText } == true
    val selectedModelIsVideoOnly = selectedModelCapabilities?.let { it.supportsVideoGen && !it.outputsText } == true
    val selectedModelIsAudioOnly = selectedModelCapabilities?.let { it.supportsAudioGen && !it.outputsText } == true
    val hasStreamingText = currentStreamingContent.isNotBlank()
    val generationActive = isSending || isStreaming
    val showWaitingStatus = generationActive && !hasStreamingText
    val activeToolUpdate = toolExecutionUpdates.lastOrNull { it.phase == ToolExecutionPhase.RUNNING }
    val waitingStatus = when {
        activeToolUpdate != null -> tx(activeToolUpdate.label)
        chatMode == ChatMode.IMAGE || selectedModelIsImageOnly -> tx("Preparing image generation")
        chatMode == ChatMode.VIDEO || selectedModelIsVideoOnly -> tx("Preparing video generation")
        chatMode == ChatMode.AUDIO || selectedModelIsAudioOnly -> tx("Preparing audio generation")
        searchMode != SearchMode.OFF -> tx("Searching the web")
        else -> tx("Thinking")
    }
    val completedStatus = when {
        chatMode == ChatMode.IMAGE || selectedModelIsImageOnly -> tx("Image ready")
        chatMode == ChatMode.VIDEO || selectedModelIsVideoOnly -> tx("Video ready")
        chatMode == ChatMode.AUDIO || selectedModelIsAudioOnly -> tx("Audio ready")
        searchMode != SearchMode.OFF -> tx("Search finished")
        else -> tx("Think time")
    }
    var generationStartedAtMs by remember { mutableStateOf<Long?>(null) }
    var elapsedGenerationSeconds by remember { mutableStateOf(0L) }
    var completedGenerationSeconds by remember { mutableStateOf<Long?>(null) }
    var timingChatId by remember { mutableStateOf(screenChatId) }

    LaunchedEffect(generationActive) {
        if (generationActive) {
            if (generationStartedAtMs == null) {
                generationStartedAtMs = SystemClock.elapsedRealtime()
                elapsedGenerationSeconds = 0L
                completedGenerationSeconds = null
            }
        } else {
            generationStartedAtMs?.let { startedAt ->
                val duration = ((SystemClock.elapsedRealtime() - startedAt) / 1_000L).coerceAtLeast(1L)
                elapsedGenerationSeconds = duration
                completedGenerationSeconds = duration
                generationStartedAtMs = null
            }
        }
    }
    LaunchedEffect(generationStartedAtMs) {
        val startedAt = generationStartedAtMs ?: return@LaunchedEffect
        while (true) {
            elapsedGenerationSeconds = ((SystemClock.elapsedRealtime() - startedAt) / 1_000L).coerceAtLeast(0L)
            delay(1_000L)
        }
    }
    LaunchedEffect(screenChatId) {
        if (timingChatId != screenChatId && !generationActive) {
            generationStartedAtMs = null
            elapsedGenerationSeconds = 0L
            completedGenerationSeconds = null
        }
        timingChatId = screenChatId
    }
    val showCompletedTiming = !generationActive && completedGenerationSeconds != null &&
        messages.lastOrNull()?.let { it.role == MessageRole.ASSISTANT && it.status != MessageStatus.ERROR } == true
    val imageGenerationAvailable = providers.any { provider ->
        provider.enabled && provider.models.any { it.supportsImageGen }
    }
    val agentExamples = activeAgent?.examples.orEmpty()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .take(5)
    val agentToolsSummary = activeAgent?.tools.orEmpty().mapNotNull { tool ->
        when (tool) {
            AgentTool.CALCULATOR -> tx("Calculator") +
                if (selectedModelCapabilities?.supportsFunctionCalling == false) " (${tx("Off")})" else ""
            AgentTool.WEB_SEARCH -> tx("Web search") + if (searchMode == SearchMode.OFF) " (${tx("Off")})" else ""
            AgentTool.IMAGE_GENERATION -> tx("Image mode") + if (chatMode != ChatMode.IMAGE) " (${tx("Off")})" else ""
            AgentTool.MCP -> tx("MCP tools")
            AgentTool.CODE_EXECUTION, AgentTool.MEMORY -> null
        }
    }.distinct()
    val storedChatTitle = chat?.title?.takeIf { it.isNotBlank() }
    val isUntitledChat = storedChatTitle == null ||
        storedChatTitle.equals("New Chat", ignoreCase = true) || storedChatTitle == tx("New chat")
    val chatHeaderTitle = if (activeAgent != null && isUntitledChat) {
        activeAgent.name
    } else {
        storedChatTitle ?: tx("New chat")
    }
    val isFreshEmptyChat = activeAgent == null && isUntitledChat && messages.isEmpty()

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

    LaunchedEffect(showSearch, biometricLockEnabled, sensitiveAuthAttempt) {
        if (!showSearch) {
            searchSheetAuthenticated = !biometricLockEnabled
            return@LaunchedEffect
        }
        if (!biometricLockEnabled) {
            searchSheetAuthenticated = true
            return@LaunchedEffect
        }
        searchSheetAuthenticated = false
        val activity = context as? androidx.fragment.app.FragmentActivity
        val authenticated = activity != null && authenticateSensitiveSettings(activity)
        if (authenticated) {
            searchSheetAuthenticated = true
        } else {
            showSearch = false
            showNote(tx("Authentication was cancelled or failed."))
        }
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

    fun readFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        scope.launch {
            val documentExtractor = DocumentTextExtractor(context)
            for (uri in uris) {
                val isDocument = try {
                    documentExtractor.supports(uri)
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    false
                }
                val result = if (isDocument) {
                    val extractedText = documentExtractor.extractText(uri)
                    if (extractedText.isFailure) {
                        showNote(tx("Could not read this document."))
                        continue
                    }
                    withContext(Dispatchers.IO) {
                        readAttachment(context, uri, extractedText.getOrThrow())
                    }
                } else {
                    withContext(Dispatchers.IO) { readAttachment(context, uri) }
                }
                when (result) {
                    is ReadResult.Ok -> pending = pending + result.attachment
                    is ReadResult.Error -> showNote(result.message)
                }
            }
        }
    }

    fun addFiles(uris: List<Uri>, videoAlreadyConfirmed: Boolean = false) {
        val videoUris = uris.filter { uri ->
            runCatching { context.contentResolver.getType(uri)?.startsWith("video/", ignoreCase = true) == true }
                .getOrDefault(false)
        }
        val otherUris = uris.filterNot(videoUris.toSet()::contains)
        readFiles(otherUris)
        if (videoUris.isEmpty() || currentProvider?.type == ProviderType.GEMINI || videoAlreadyConfirmed) {
            readFiles(videoUris)
        } else {
            pendingVideoUris = (pendingVideoUris + videoUris).distinct()
            showNonGeminiVideoWarning = true
        }
    }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        val confirmedVideo = mediaPickerVideoConfirmed
        mediaPickerVideoConfirmed = false
        if (uris.isNotEmpty()) addFiles(uris, videoAlreadyConfirmed = confirmedVideo)
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
        if ((text.isBlank() && submittedAttachments.isEmpty()) || !selectedModelReady || isStreaming || isSending) return
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

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchPickerAfterSheetCloses { cameraPicker.launch(null) }
        } else {
            showNote(tx("Camera permission denied."))
        }
    }

    fun launchCameraCapture() {
        if (
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            launchPickerAfterSheetCloses { cameraPicker.launch(null) }
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
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
                mediaPickerVideoConfirmed = false
                launchPickerAfterSheetCloses {
                    mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            },
            onVideoFile = {
                if (currentProvider?.type == ProviderType.GEMINI) {
                    mediaPickerVideoConfirmed = true
                    launchPickerAfterSheetCloses {
                        mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    }
                } else {
                    pendingVideoUris = emptyList()
                    showNonGeminiVideoWarning = true
                }
            },
            onCamera = { launchCameraCapture() },
            onFile = {
                launchPickerAfterSheetCloses {
                    filePicker.launch(arrayOf(
                        "text/*",
                        "application/json",
                        "application/pdf",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "image/*",
                        "video/*"
                    ))
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
    if (showNonGeminiVideoWarning) {
        AlertDialog(
            onDismissRequest = {
                showNonGeminiVideoWarning = false
                pendingVideoUris = emptyList()
            },
            title = { Text(tx("This provider may not support video")) },
            text = { Text(tx("Video input is supported by Gemini providers. With the selected provider, the video may be ignored or rejected. Continue anyway?")) },
            confirmButton = {
                TextButton(onClick = {
                    showNonGeminiVideoWarning = false
                    val selectedVideos = pendingVideoUris
                    pendingVideoUris = emptyList()
                    if (selectedVideos.isNotEmpty()) {
                        readFiles(selectedVideos)
                    } else {
                        mediaPickerVideoConfirmed = true
                        launchPickerAfterSheetCloses {
                            mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        }
                    }
                }) { Text(tx("Continue")) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNonGeminiVideoWarning = false
                    pendingVideoUris = emptyList()
                }) { Text(tx("Cancel")) }
            }
        )
    }
    if (showThinking) {
        ThinkingDepthSheet(level = reasoning, onLevelChange = onReasoningChange, onDismiss = { showThinking = false })
    }
    if (showSearch && (!biometricLockEnabled || searchSheetAuthenticated)) {
        SearchSheet(
            mode = searchMode,
            providers = providers,
            selectedProviderId = selectedProviderId,
            selectedModelId = selectedModel,
            onModeChange = onSearchModeChange,
            onSelectSearchModel = onSelectModel,
            onSaveProvider = onSaveNativeSearchProvider,
            onDismiss = { showSearch = false },
            searchProvider = searchProvider,
            searchKeys = searchKeys,
            onSearchProviderChange = onSearchProviderChange,
            onSaveSearchKey = onSaveSearchKey
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
                        if (!isFreshEmptyChat) {
                            ChatHeaderTitle(
                                chatTitle = chatHeaderTitle,
                                activeAgent = activeAgent,
                                onTitleClick = startRenamingChat,
                                onAgentClick = onOpenAgents
                            )
                        }
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
                        Box {
                            IconButton(onClick = { showTopMenu = true }) {
                                Icon(Icons.Default.MoreHoriz, contentDescription = tx("More"), tint = Ink)
                            }
                            val menuShape = paperPopupShape(18.dp)
                            DropdownMenu(
                                expanded = showTopMenu,
                                onDismissRequest = { showTopMenu = false },
                                modifier = Modifier.background(AriCard, menuShape).then(paperPopupModifier(menuShape))
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (checkingConnection) tx("Checking…") else tx("Check connection")) },
                                    leadingIcon = { Icon(Icons.Default.NetworkCheck, contentDescription = null) },
                                    enabled = requestedProvider?.enabled == true && !checkingConnection,
                                    onClick = {
                                        showTopMenu = false
                                        val provider = requestedProvider?.takeIf { it.enabled }
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
                                    text = { Text(tx("Quick parameters")) },
                                    leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                                    onClick = { showTopMenu = false; showQuickParameters = true }
                                )
                                DropdownMenuItem(
                                    text = { Text(tx("Rename chat")) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showTopMenu = false
                                        startRenamingChat()
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
                        provider = requestedProvider ?: currentProvider,
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
                val composerShape = RoundedCornerShape(32.dp)
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
                                IconButton(onClick = { showFullEditor = true }) {
                                    Icon(
                                        Icons.Default.OpenInFull,
                                        contentDescription = tx("Expand message"),
                                        tint = Ink.copy(alpha = 0.55f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        if (chatMode != ChatMode.TEXT) {
                            Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp)) {
                                ModeChip(mode = chatMode, onClose = { onChatModeChange(ChatMode.TEXT) }, modifier = Modifier)
                            }
                        }
                        val canSend = (inputText.isNotBlank() || pending.isNotEmpty()) && selectedModelReady && !isStreaming && !isSending
                        val sendButtonColor by animateColorAsState(
                            targetValue = if (canSend) AriAccentSoft else AriAccentSoft.copy(alpha = 0.58f),
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                            label = "chat-send-background"
                        )
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
                                placeholder = {
                                    Text(
                                        activeAgent?.name?.takeIf { it.isNotBlank() }?.let { "${tx("Message")} $it" }
                                            ?: tx("Message AriAI"),
                                        color = Muted
                                    )
                                },
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
                                // Keep a 48 dp tap target while drawing a smaller, rounded stop control.
                                Surface(
                                    onClick = { onStopGeneration() },
                                    modifier = Modifier.size(48.dp).testTag("chat-send-button"),
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    contentColor = AriAccent,
                                    shadowElevation = 0.dp,
                                    tonalElevation = 0.dp
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Surface(
                                            modifier = Modifier.size(40.dp).testTag("chat-send-button-visual"),
                                            shape = RoundedCornerShape(14.dp),
                                            color = AriAccentSoft,
                                            contentColor = AriAccent,
                                            shadowElevation = 1.dp,
                                            tonalElevation = 0.dp
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Stop, contentDescription = tx("Stop"), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    onClick = { send() },
                                    enabled = canSend,
                                    modifier = Modifier.size(48.dp).testTag("chat-send-button"),
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    contentColor = AriAccent,
                                    shadowElevation = 0.dp,
                                    tonalElevation = 0.dp
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Surface(
                                            modifier = Modifier.size(40.dp).testTag("chat-send-button-visual"),
                                            shape = RoundedCornerShape(14.dp),
                                            color = sendButtonColor,
                                            contentColor = AriAccent,
                                            shadowElevation = if (canSend) 2.dp else 1.dp,
                                            tonalElevation = 0.dp
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.ArrowUpward,
                                                    contentDescription = tx("Send"),
                                                    tint = AriAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ComposerActionButton(Icons.Default.Add, tx("Add to message"), active = false) {
                                showAttachments = true
                            }
                            Spacer(Modifier.width(4.dp))
                            ComposerActionButton(
                                Icons.Default.Search,
                                tx("Web search"),
                                active = searchMode != SearchMode.OFF,
                                stateful = true
                            ) {
                                showSearch = true
                            }
                            Spacer(Modifier.width(4.dp))
                            ComposerActionButton(
                                icon = if (listening) Icons.Default.Stop else Icons.Default.Mic,
                                description = if (listening) tx("Stop listening") else tx("Voice input"),
                                active = listening,
                                stateful = true,
                                activeStateDescription = tx("Listening…")
                            ) {
                                when {
                                    listening -> {
                                        recognizer?.stopListening()
                                        listening = false
                                    }
                                    else -> requestVoiceInput()
                                }
                            }
                            if (listening) {
                                Spacer(Modifier.width(6.dp))
                                ListeningIndicator()
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
                    if (activeAgent == null) {
                        Text(
                            text = "AriAI",
                            modifier = Modifier.testTag("empty-chat-watermark"),
                            color = Ink.copy(alpha = 0.16f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 34.sp,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = AriCard.copy(alpha = 0.72f),
                                    offset = Offset(0f, 1f),
                                    blurRadius = 0f
                                )
                            )
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(if (compactWelcome) 38.dp else 48.dp)
                                .clip(CircleShape)
                                .background(AriAccentSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                activeAgent.avatar,
                                fontSize = if (compactWelcome) 18.sp else 22.sp
                            )
                        }
                        Text(
                            activeAgent.name,
                            modifier = Modifier.testTag("welcome-agent-name"),
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
                        activeAgent?.description?.takeIf { it.isNotBlank() }?.let { description ->
                            Text(
                                description,
                                modifier = Modifier.fillMaxWidth().testTag("welcome-agent-description"),
                                color = Muted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (agentToolsSummary.isNotEmpty()) {
                            Text(
                                "${tx("Tools")}: ${agentToolsSummary.joinToString(" · ")}",
                                modifier = Modifier.fillMaxWidth().testTag("welcome-agent-tools"),
                                color = AriAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!selectedModelReady) {
                            Text(
                                modelSetupMessage,
                                color = Muted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            if (!compactWelcome && providers.isNotEmpty() &&
                                (currentProvider == null || currentProvider.models.isEmpty())
                            ) {
                                Button(
                                    onClick = onManageProviders,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AriAccentSoft,
                                        contentColor = AriAccent
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 0.dp)
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text(tx("Manage providers"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        } else if (!compactWelcome) {
                            Text(
                                tx("Type a message below to start."),
                                color = Muted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().testTag("welcome-suggestions"),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                if (agentExamples.isNotEmpty()) {
                                    agentExamples.forEachIndexed { index, example ->
                                        item(key = "agent-example:$index") {
                                            AssistChip(
                                                onClick = { inputText = example },
                                                modifier = Modifier.testTag("agent-example-$index"),
                                                label = {
                                                    Text(example, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                                },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = AriCard,
                                                    labelColor = Ink,
                                                    leadingIconContentColor = AriAccent
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    item(key = "suggestion:brainstorm") {
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
                                    }
                                    item(key = "suggestion:summarize") {
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
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (i == messages.lastIndex && toolExecutionUpdates.isNotEmpty()) {
                                        ToolExecutionTimeline(toolExecutionUpdates)
                                    }
                                    if (showCompletedTiming && i == messages.lastIndex) {
                                        GenerationStatus(
                                            label = completedStatus,
                                            elapsedSeconds = completedGenerationSeconds ?: 0L,
                                            active = false
                                        )
                                    }
                                    val branchIds = branchNavigationIds(m.id, m.branchChildren)
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
                                    if (branchIds.size > 1) {
                                        BranchNavigator(
                                            messageId = m.id,
                                            branchMessageIds = branchIds,
                                            onNavigate = onNavigateToBranch
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (hasStreamingText) {
                        item(key = "streaming") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (generationActive) {
                                    GenerationWorkCard(waitingStatus, elapsedGenerationSeconds, toolExecutionUpdates)
                                } else {
                                    ToolExecutionTimeline(toolExecutionUpdates)
                                }
                                AssistantBlock(text = currentStreamingContent, streaming = isStreaming, fontSize = fontSize)
                            }
                        }
                    } else if (showWaitingStatus) {
                        item(key = "waiting-status") {
                            GenerationWorkCard(waitingStatus, elapsedGenerationSeconds, toolExecutionUpdates)
                        }
                    } else if (toolExecutionUpdates.isNotEmpty() && messages.lastOrNull()?.role != MessageRole.ASSISTANT) {
                        item(key = "tool-timeline") {
                            ToolExecutionTimeline(toolExecutionUpdates)
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun ChatHeaderTitle(
    chatTitle: String,
    activeAgent: Agent?,
    onTitleClick: () -> Unit,
    onAgentClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("chat-title-header").padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onAgentClick,
            modifier = Modifier.size(48.dp)
                .testTag("chat-agent-button")
                .semantics(mergeDescendants = true) {
                    contentDescription = tx("Agents & prompts")
                },
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = Ink,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (activeAgent == null) {
                    ProviderBrandMark(provider = null, size = 30.dp)
                } else {
                    Box(
                        modifier = Modifier.size(30.dp).clip(CircleShape).background(AriAccentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(activeAgent.avatar, fontSize = 17.sp)
                    }
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            chatTitle,
            modifier = Modifier.weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button) { onTitleClick() }
                .semantics { stateDescription = tx("Rename chat") }
                .testTag("chat-title-text")
                .padding(vertical = 5.dp),
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
    val hasSelectedModel = provider != null && modelName.isNotBlank() && modelName != tx("Choose model")
    val label = if (hasSelectedModel) modelName else tx("Choose model")
    Surface(
        onClick = onClick,
        modifier = modifier.widthIn(max = 244.dp).heightIn(min = 40.dp)
            .animateContentSize(tween(190, easing = FastOutSlowInEasing))
            .semantics(mergeDescendants = true) {
                contentDescription = tx("Choose model")
                stateDescription = "${provider?.name ?: tx("No provider")} · $modelName"
            },
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        contentColor = Ink,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.heightIn(min = 40.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier.widthIn(max = 210.dp),
                color = if (hasSelectedModel) Ink else Muted,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(20.dp)
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
    val percentage = (fraction * 100).toInt()
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape)
            .semantics(mergeDescendants = true) {
                contentDescription = tx("Context usage")
                stateDescription = (if (approximate) "${tx("Approx.")} " else "") + "$percentage%"
            }
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("chat-context-usage"),
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
            if (approximate) "~$percentage%" else "$percentage%",
            color = Ink,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PendingChip(attachment: Attachment, onRemove: () -> Unit) {
    val extractedText = attachment.textContent?.takeIf(String::isNotBlank)
    val preview = remember(extractedText) { extractedText?.let { attachmentTextPreview(it) } }
    val estimatedTokens = remember(extractedText) { extractedText?.let(::estimateAttachmentTextTokens) }
    Surface(shape = RoundedCornerShape(18.dp), color = AriCard, shadowElevation = 0.dp) {
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
            Column(
                modifier = Modifier.widthIn(max = 190.dp).padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    attachment.name,
                    fontSize = 12.sp,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!preview.isNullOrBlank()) {
                    Text(
                        preview,
                        color = Muted,
                        fontSize = 10.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("attachment-text-preview")
                    )
                    Text(
                        "${tx("Approx.")} $estimatedTokens ${tx("tokens in context")}",
                        color = Muted,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("attachment-token-estimate")
                    )
                }
            }
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
            modifier = Modifier.size(180.dp).clip(RoundedCornerShape(18.dp))
        )
    } else {
        Text(attachment.name, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun UserBubble(text: String, attachments: List<Attachment>, fontSize: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(28.dp, 28.dp, 9.dp, 28.dp),
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
private fun ToolExecutionTimeline(updates: List<ToolExecutionUpdate>) {
    val finished = updates.filter { it.phase != ToolExecutionPhase.RUNNING }
    if (finished.isEmpty()) return
    val visible = finished.takeLast(3)
    Column(
        modifier = Modifier.padding(start = 8.dp).testTag("chat-tool-timeline"),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (finished.size > visible.size) {
            Text(
                text = "+${finished.size - visible.size}",
                color = Muted.copy(alpha = 0.72f),
                fontSize = 10.sp
            )
        }
        visible.forEach { update ->
            val label = tx(update.label)
            val statusText = when (update.phase) {
                ToolExecutionPhase.COMPLETED -> if (update.elapsedMs >= 1_000L) {
                    "$label · ${formatGenerationDuration(update.elapsedMs / 1_000L)}"
                } else label
                ToolExecutionPhase.FAILED -> "$label · ${tx("Failed")}"
                ToolExecutionPhase.CANCELLED -> "$label · ${tx("Stopped")}"
                ToolExecutionPhase.RUNNING -> label
            }
            val icon = when (update.phase) {
                ToolExecutionPhase.COMPLETED -> Icons.Default.Check
                ToolExecutionPhase.FAILED -> Icons.Default.ErrorOutline
                ToolExecutionPhase.CANCELLED -> Icons.Default.Stop
                ToolExecutionPhase.RUNNING -> Icons.Default.MoreHoriz
            }
            Row(
                modifier = Modifier.testTag("chat-tool-execution-step"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = Muted.copy(alpha = 0.75f), modifier = Modifier.size(13.dp))
                Text(
                    statusText,
                    color = Muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GenerationStatus(label: String, elapsedSeconds: Long, active: Boolean) {
    val duration = formatGenerationDuration(elapsedSeconds)
    val statusText = if (elapsedSeconds > 0L) "$label · $duration" else label
    Surface(
        shape = RoundedCornerShape(50),
        color = AriTint,
        modifier = Modifier.padding(start = 4.dp).widthIn(max = 360.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (active) {
                CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 1.8.dp, color = Accent)
            } else {
                Icon(Icons.Default.Check, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp))
            }
            Text(
                statusText,
                modifier = Modifier.testTag(if (active) "chat-generation-status-active" else "chat-generation-status-complete"),
                color = Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Live status card while a reply is being generated: an animated "thinking /
 * searching" header with the elapsed time, plus every tool step executed so far,
 * all inside one frame.
 */
@Composable
private fun GenerationWorkCard(
    label: String,
    elapsedSeconds: Long,
    updates: List<ToolExecutionUpdate>,
    modifier: Modifier = Modifier
) {
    val steps = updates.takeLast(8)
    val hiddenCount = updates.size - steps.size
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AriCard,
        border = BorderStroke(0.75.dp, AriLine.copy(alpha = 0.85f)),
        modifier = modifier
            .padding(start = 4.dp)
            .widthIn(max = 380.dp)
            .animateContentSize(tween(200, easing = FastOutSlowInEasing))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PulsingWorkDot()
                val statusText = if (elapsedSeconds > 0L) {
                    "$label · ${formatGenerationDuration(elapsedSeconds)}"
                } else {
                    label
                }
                Text(
                    statusText,
                    color = Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat-generation-status-active")
                )
            }
            if (steps.isNotEmpty()) {
                Column(
                    modifier = Modifier.testTag("chat-tool-timeline"),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (hiddenCount > 0) {
                        Text("+$hiddenCount", color = Muted.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                    steps.forEach { update -> GenerationWorkStep(update) }
                }
            }
        }
    }
}

@Composable
private fun GenerationWorkStep(update: ToolExecutionUpdate) {
    val label = tx(update.label)
    val statusText = when (update.phase) {
        ToolExecutionPhase.COMPLETED -> if (update.elapsedMs >= 1_000L) {
            "$label · ${formatGenerationDuration(update.elapsedMs / 1_000L)}"
        } else {
            label
        }
        ToolExecutionPhase.FAILED -> "$label · ${tx("Failed")}"
        ToolExecutionPhase.CANCELLED -> "$label · ${tx("Stopped")}"
        ToolExecutionPhase.RUNNING -> label
    }
    Row(
        modifier = Modifier.testTag("chat-tool-execution-step"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        when (update.phase) {
            ToolExecutionPhase.RUNNING -> CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = Accent
            )
            ToolExecutionPhase.COMPLETED -> Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Accent.copy(alpha = 0.85f),
                modifier = Modifier.size(13.dp)
            )
            ToolExecutionPhase.FAILED -> Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(13.dp)
            )
            ToolExecutionPhase.CANCELLED -> Icon(
                Icons.Default.Stop,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            statusText,
            color = if (update.phase == ToolExecutionPhase.RUNNING) Ink else Muted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Small breathing dot that signals an active generation without a heavy spinner. */
@Composable
private fun PulsingWorkDot() {
    val transition = rememberInfiniteTransition(label = "generation-work-dot")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "generation-work-dot-alpha"
    )
    Box(
        modifier = Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(Accent.copy(alpha = alpha))
    )
}

private fun formatGenerationDuration(totalSeconds: Long): String {
    val numbers = NumberFormat.getIntegerInstance(Locale.forLanguageTag(UiLang.code))
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes == 0L) {
        "${numbers.format(totalSeconds)} ${tx("sec")}"
    } else if (seconds == 0L) {
        "${numbers.format(minutes)} ${tx("min")}"
    } else {
        "${numbers.format(minutes)} ${tx("min")} ${numbers.format(seconds)} ${tx("sec")}"
    }
}

@Composable
private fun BranchNavigator(
    messageId: String,
    branchMessageIds: List<String>,
    onNavigate: (String) -> Unit
) {
    val index = branchMessageIds.indexOf(messageId)
    if (index < 0 || branchMessageIds.size < 2) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.testTag("branch-navigation-$messageId")
    ) {
        IconButton(
            onClick = { onNavigate(branchMessageIds[index - 1]) },
            enabled = index > 0,
            modifier = Modifier.size(34.dp).semantics { contentDescription = tx("Previous branch") }
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = if (index > 0) Ink else Muted.copy(alpha = 0.35f), modifier = Modifier.size(17.dp))
        }
        Text(
            "${tx("Branch")} ${index + 1} / ${branchMessageIds.size}",
            color = Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        IconButton(
            onClick = { onNavigate(branchMessageIds[index + 1]) },
            enabled = index < branchMessageIds.lastIndex,
            modifier = Modifier.size(34.dp).semantics { contentDescription = tx("Next branch") }
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = if (index < branchMessageIds.lastIndex) Ink else Muted.copy(alpha = 0.35f), modifier = Modifier.size(17.dp))
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
                ReplyContent(visibleText, fontSize.toFloat(), Ink, streaming)
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
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable { full = true }
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
private fun EditImageDialog(
    source: ChatMessage,
    onDismiss: () -> Unit,
    onConfirm: (String, File?) -> Unit
) {
    var text by remember(source.id) { mutableStateOf("") }
    var strokes by remember(source.id) { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var activeStroke by remember(source.id) { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember(source.id) { mutableStateOf(IntSize.Zero) }
    var brushWidthDp by remember(source.id) { mutableStateOf(24f) }
    var preparingMask by remember(source.id) { mutableStateOf(false) }
    var maskError by remember(source.id) { mutableStateOf<String?>(null) }
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val sourceBitmap by produceState<Bitmap?>(initialValue = null, key1 = source.imagePath) {
        value = withContext(Dispatchers.IO) { source.imagePath?.let { BitmapFactory.decodeFile(it) } }
    }
    val preview = sourceBitmap?.asImageBitmap()
    DisposableEffect(sourceBitmap) {
        onDispose { sourceBitmap?.recycle() }
    }
    val dialogShape = paperPopupShape(26.dp)
    AlertDialog(
        onDismissRequest = { if (!preparingMask) onDismiss() },
        modifier = paperPopupModifier(dialogShape),
        shape = dialogShape,
        containerColor = AriCard,
        title = { Text(tx("Edit image")) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                if (preview != null) {
                    val aspectRatio = preview.width.toFloat() / preview.height.toFloat()
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val maxCanvasHeight = 250.dp
                        val displayWidth = minOf(maxWidth, maxCanvasHeight * aspectRatio)
                        val displayHeight = displayWidth / aspectRatio
                        Canvas(
                            modifier = Modifier.width(displayWidth).height(displayHeight)
                                .clip(RoundedCornerShape(16.dp))
                                .onSizeChanged { canvasSize = it }
                                .pointerInput(preview) {
                                    detectDragGestures(
                                        onDragStart = { point -> activeStroke = listOf(point) },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            activeStroke = activeStroke + change.position
                                        },
                                        onDragEnd = {
                                            if (activeStroke.isNotEmpty()) strokes = strokes + listOf(activeStroke)
                                            activeStroke = emptyList()
                                        },
                                        onDragCancel = {
                                            if (activeStroke.isNotEmpty()) strokes = strokes + listOf(activeStroke)
                                            activeStroke = emptyList()
                                        }
                                    )
                                }
                        ) {
                            drawImage(
                                preview,
                                dstOffset = IntOffset.Zero,
                                dstSize = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))
                            )
                            val overlayColor = Color(0xAA3977D5)
                            val widthPx = with(density) { brushWidthDp.dp.toPx() }
                            (strokes + listOf(activeStroke)).filter { it.isNotEmpty() }.forEach { points ->
                                if (points.size == 1) {
                                    drawCircle(overlayColor, radius = widthPx / 2f, center = points.first())
                                } else {
                                    val path = Path().apply {
                                        moveTo(points.first().x, points.first().y)
                                        points.drop(1).forEach { lineTo(it.x, it.y) }
                                    }
                                    drawPath(
                                        path,
                                        color = overlayColor,
                                        style = Stroke(width = widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        tx("Paint the area you want to replace. Leave the canvas blank to edit the full image."),
                        color = AriMuted,
                        fontSize = 12.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(tx("Brush size"), color = AriInk, fontSize = 12.sp)
                        TextButton(onClick = { strokes = emptyList(); activeStroke = emptyList(); maskError = null }) {
                            Text(tx("Clear mask"), color = AriAccent)
                        }
                    }
                    Slider(
                        value = brushWidthDp,
                        onValueChange = { brushWidthDp = it },
                        valueRange = 8f..48f,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(tx("Image preview unavailable. You can still edit it with a prompt."), color = AriMuted, fontSize = 12.sp)
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(2_000) },
                    placeholder = { Text(tx("What should change?")) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                maskError?.let { Text(it, color = Color(0xFFB3261E), fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank() && !preparingMask,
                onClick = {
                    if (text.isBlank() || preparingMask) return@TextButton
                    preparingMask = true
                    maskError = null
                    val submittedText = text.trim()
                    val submittedStrokes = strokes + listOf(activeStroke).filter { it.isNotEmpty() }
                    val submittedBitmap = sourceBitmap
                    val submittedCanvasSize = canvasSize
                    val submittedBrushWidth = brushWidthDp
                    val submittedDensity = density
                    scope.launch {
                        var generatedMask: File? = null
                        try {
                            val mask = withContext(Dispatchers.IO) {
                                if (submittedBitmap != null && submittedStrokes.isNotEmpty() && submittedCanvasSize.width > 0 && submittedCanvasSize.height > 0) {
                                    createImageEditMaskFile(
                                        bitmap = submittedBitmap,
                                        strokes = submittedStrokes,
                                        canvasWidth = submittedCanvasSize.width,
                                        canvasHeight = submittedCanvasSize.height,
                                        brushWidthDp = submittedBrushWidth,
                                        density = submittedDensity
                                    ).also { generatedMask = it }
                                } else null
                            }
                            preparingMask = false
                            onConfirm(submittedText, mask)
                            generatedMask = null // Ownership has moved to the view model.
                        } catch (cancelled: CancellationException) {
                            generatedMask?.let { com.ariai.app.data.local.ImageStore.deleteTemp(it) }
                            preparingMask = false
                            throw cancelled
                        } catch (_: Exception) {
                            generatedMask?.let { com.ariai.app.data.local.ImageStore.deleteTemp(it) }
                            preparingMask = false
                            maskError = tx("Could not prepare the image mask.")
                        }
                    }
                }
            ) { Text(if (preparingMask) tx("Preparing…") else tx("Send")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !preparingMask) { Text(tx("Cancel")) }
        }
    )
}

private fun createImageEditMaskFile(
    bitmap: Bitmap,
    strokes: List<List<Offset>>,
    canvasWidth: Int,
    canvasHeight: Int,
    brushWidthDp: Float,
    density: Float
): File {
    require(canvasWidth > 0 && canvasHeight > 0)
    val mask = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    try {
        val canvas = android.graphics.Canvas(mask)
        canvas.drawColor(AndroidColor.WHITE)
        val scaleX = bitmap.width.toFloat() / canvasWidth
        val scaleY = bitmap.height.toFloat() / canvasHeight
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.TRANSPARENT
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = brushWidthDp * density * minOf(scaleX, scaleY)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        strokes.filter { it.isNotEmpty() }.forEach { points ->
            if (points.size == 1) {
                paint.style = Paint.Style.FILL
                canvas.drawCircle(points.first().x * scaleX, points.first().y * scaleY, paint.strokeWidth / 2f, paint)
                paint.style = Paint.Style.STROKE
            } else {
                val path = android.graphics.Path().apply {
                    moveTo(points.first().x * scaleX, points.first().y * scaleY)
                    points.drop(1).forEach { lineTo(it.x * scaleX, it.y * scaleY) }
                }
                canvas.drawPath(path, paint)
            }
        }
        val png = java.io.ByteArrayOutputStream().use { output ->
            check(mask.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode the image mask." }
            output.toByteArray()
        }
        return com.ariai.app.data.local.ImageStore.createTempImage(png, "image/png")
    } finally {
        mask.recycle()
    }
}

@Composable
private fun ActionIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(icon, contentDescription = description, tint = Muted, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ComposerActionButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    stateful: Boolean = false,
    activeStateDescription: String? = null,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (active) AriAccentSoft else Color.Transparent,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "composer-action-background"
    )
    val iconColor by animateColorAsState(
        targetValue = if (active) Accent else Ink.copy(alpha = 0.7f),
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "composer-action-foreground"
    )
    Box(
        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                if (stateful) {
                    stateDescription = if (active) activeStateDescription ?: tx("On") else tx("Off")
                }
            }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ListeningIndicator() {
    val transition = rememberInfiniteTransition(label = "voice-listening-indicator")
    val dotAlpha by transition.animateFloat(
        initialValue = 0.38f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice-listening-pulse"
    )
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier.clip(shape)
            .background(AriAccentSoft.copy(alpha = 0.78f))
            .border(1.dp, AriAccent.copy(alpha = 0.14f), shape)
            .padding(horizontal = 9.dp, vertical = 7.dp)
            .testTag("voice-listening-indicator"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(7.dp).clip(CircleShape)
                .background(AriAccent.copy(alpha = dotAlpha))
        )
        Text(
            text = tx("Listening…"),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = AriAccent,
            maxLines = 1
        )
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

@android.annotation.SuppressLint("UnsafeOptInUsageError")
@Composable
private fun VideoCard(path: String) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(22.dp), color = AriCard, modifier = Modifier.fillMaxWidth()) {
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
    Surface(shape = RoundedCornerShape(22.dp), color = AriCard, modifier = Modifier.fillMaxWidth()) {
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
@androidx.media3.common.util.UnstableApi
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
    val shape = RoundedCornerShape(20.dp)
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
