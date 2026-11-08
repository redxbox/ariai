package com.ariai.app.ui.screens

import com.ariai.app.util.tx
import com.ariai.app.util.UiLang

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.ariai.app.ui.theme.*

import android.content.ClipData
import android.content.ContentValues
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import androidx.compose.foundation.Image
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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.res.painterResource
import com.ariai.app.R
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
import androidx.compose.ui.graphics.Shadow
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
                            Icon(painterResource(R.drawable.ic_menu_staggered), contentDescription = tx("Open menu"), tint = Ink)
                        }
                    },
                    title = {
                        ChatHeaderTitle(
                            chatTitle = chatHeaderTitle,
                            activeAgent = activeAgent,
                            onTitleClick = startRenamingChat,
                            onAgentClick = onOpenAgents
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
                        } else {
                            // Fresh chat or a model without a known context window: keep the
                            // indicator visible; tapping it points the user at the model picker.
                            ContextUsageIndicator(
                                tokens = 0,
                                contextWindow = 0,
                                approximate = true,
                                onClick = { showNote(tx("Choose model")) }
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
