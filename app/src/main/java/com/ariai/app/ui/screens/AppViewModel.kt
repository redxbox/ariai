package com.ariai.app.ui.screens

import com.ariai.app.util.tx

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ariai.app.data.local.PreferencesManager
import com.ariai.app.data.models.*
import com.ariai.app.data.remote.supportsNativeSearch
import com.ariai.app.data.remote.looksLikeVideoModel
import com.ariai.app.data.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** What the next message in a chat produces. */
enum class ChatMode { TEXT, IMAGE, VIDEO, AUDIO }

private const val MAX_MEDIA_BYTES_PER_REQUEST = 16L * 1024L * 1024L

class AppViewModel(
    private val repository: ChatRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    // Preferences
    val language = prefs.languageFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "en")
    val theme = prefs.themeFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "system")
    val dynamicColor = prefs.dynamicColorFlow.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val searchKeys = prefs.searchKeysFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    val defaultProviderId = prefs.defaultProviderFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val defaultModelId = prefs.defaultModelFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val streamResponse = prefs.streamResponseFlow.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val fontSize = prefs.fontSizeFlow.stateIn(viewModelScope, SharingStarted.Eagerly, 15)
    val reasoningLevel = prefs.reasoningLevelFlow.map { ReasoningLevel.fromName(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReasoningLevel.OFF)
    val searchMode = prefs.searchModeFlow.map { SearchMode.fromName(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SearchMode.OFF)
    val favoriteModels = prefs.favoriteModelsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun setDefaultSelection(providerId: String, modelId: String) {
        viewModelScope.launch { prefs.setDefaultSelection(providerId, modelId) }
    }

    fun setStreamResponse(enabled: Boolean) {
        viewModelScope.launch { prefs.setStreamResponse(enabled) }
    }

    fun setFontSize(size: Int) {
        viewModelScope.launch { prefs.setFontSize(size) }
    }

    // Data
    val providers = repository.getProviders().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val chats = repository.getChats().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val agents = repository.getAgents().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Chat state
    private val _currentChatId = MutableStateFlow<String?>(null)
    // chatId -> (providerId, modelId) chosen in the chat screen this session.
    private val _modelOverrides = MutableStateFlow<Map<String, Pair<String, String>>>(emptyMap())
    val modelOverrides = _modelOverrides.asStateFlow()
    private val _chatModes = MutableStateFlow<Map<String, ChatMode>>(emptyMap())
    val chatModes = _chatModes.asStateFlow()

    fun setChatMode(chatId: String, mode: ChatMode) {
        _chatModes.value = _chatModes.value + (chatId to mode)
        // Picking Image or Video switches the chat to a matching model automatically.
        if (mode != ChatMode.TEXT) {
            val preferred = resolveProvider(chatId)
            val found = when (mode) {
                ChatMode.IMAGE -> findImageModel(preferred)
                ChatMode.AUDIO -> findModel(preferred) { it.supportsAudioGen }
                else -> findModel(preferred) { it.supportsVideoGen }
            }
            found?.let { (p, m) -> updateChatProvider(chatId, p.id, m.id) }
        }
    }

    /** Only models whose output modalities include image (never decided by name). */
    private fun findImageModel(preferred: Provider?): Pair<Provider, com.ariai.app.data.models.AIModel>? =
        findModel(preferred) { it.supportsImageGen }

    /** Adds a message unless it is already in the list (the DB flow can deliver it first). */
    private fun appendMessage(message: ChatMessage) {
        if (_currentChatId.value != message.chatId) return
        if (_messages.value.none { it.id == message.id }) _messages.value = _messages.value + message
    }

    /** Finds a model matching [pred]; the chat's own provider is preferred. */
    private fun findModel(preferred: Provider?, pred: (com.ariai.app.data.models.AIModel) -> Boolean): Pair<Provider, com.ariai.app.data.models.AIModel>? {
        preferred?.models?.firstOrNull(pred)?.let { return preferred to it }
        return providers.value.asSequence()
            .filter { it.enabled }
            .mapNotNull { p -> p.models.firstOrNull(pred)?.let { p to it } }
            .firstOrNull()
    }
    val currentChatId = _currentChatId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    private val _isSending = MutableStateFlow(false)
    val isSending = _isSending.asStateFlow()
    private var sendPending = false
    private val RECENT_MESSAGES_FOR_MODEL = 20
    private var sendJob: kotlinx.coroutines.Job? = null
    private var messagesCollectionJob: kotlinx.coroutines.Job? = null

    /** Stops the reply being generated. The text received so far is kept as the reply. */
    fun stopGeneration() {
        sendJob?.cancel()
    }
    val isStreaming = _isStreaming.asStateFlow()

    private val _streamingContent = MutableStateFlow("")
    val streamingContent = _streamingContent.asStateFlow()

    private val _generatedImages = MutableStateFlow<List<GeneratedImage>>(emptyList())
    val generatedImages = _generatedImages.asStateFlow()
    private val _generatedMediaBytes = MutableStateFlow(0L)
    val generatedMediaBytes = _generatedMediaBytes.asStateFlow()

    private suspend fun refreshGeneratedMediaBytes() {
        runCatching { repository.generatedMediaBytes() }
            .onSuccess { _generatedMediaBytes.value = it }
            .onFailure { com.ariai.app.util.AppLog.warn(it, "generated media size") }
    }

    init {
        viewModelScope.launch { refreshGeneratedMediaBytes() }
    }

    private val _isGeneratingImage = MutableStateFlow(false)
    val isGeneratingImage = _isGeneratingImage.asStateFlow()

    fun setLanguage(lang: String) {
        viewModelScope.launch { prefs.setLanguage(lang) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { prefs.setTheme(theme) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { prefs.setDynamicColor(enabled) }
    }

    fun setSearchKey(provider: String, key: String) {
        viewModelScope.launch { prefs.setSearchApiKey(provider, key) }
    }

    suspend fun fetchModels(provider: Provider): List<AIModel> = repository.fetchModels(provider)

    fun saveProvider(provider: Provider) {
        viewModelScope.launch {
            val models = if (provider.models.isEmpty()) {
                runCatching { repository.fetchModels(provider) }.getOrNull()?.takeIf { it.isNotEmpty() }
                    ?: getDefaultModelsForType(provider.type, provider.id)
            } else provider.models
            repository.saveProvider(provider.copy(models = models))
        }
    }

    fun deleteProvider(id: String) {
        viewModelScope.launch { repository.deleteProvider(id) }
    }

    fun saveAgent(agent: Agent) {
        viewModelScope.launch { repository.saveAgent(agent) }
    }

    fun deleteAgent(id: String) {
        viewModelScope.launch { repository.deleteAgent(id) }
    }

    /** First enabled model that generates images, as (providerId, modelId). */
    fun firstImageModel(): Pair<String, String>? =
        providers.value.asSequence()
            .filter { it.enabled }
            .flatMap { p -> p.models.asSequence().filter { isImageModel(it) }.map { p.id to it.id } }
            .firstOrNull()

    fun createNewChat(providerId: String? = null, modelId: String? = null, agentId: String? = null): String {
        val id = java.util.UUID.randomUUID().toString()
        val chat = Chat(
            id = id,
            title = "New Chat",
            providerId = providerId ?: defaultProviderId.value ?: providers.value.firstOrNull()?.id,
            modelId = modelId ?: defaultModelId.value ?: providers.value.firstOrNull()?.models?.firstOrNull()?.id,
            agentId = agentId
        )
        // Set active state synchronously: callers navigate to this id immediately after return.
        sendJob?.cancel()
        messagesCollectionJob?.cancel()
        _currentChatId.value = id
        _messages.value = emptyList()
        viewModelScope.launch { repository.saveChat(chat) }
        return id
    }

    fun loadChat(chatId: String) {
        if (_currentChatId.value != chatId) {
            sendJob?.cancel()
            _currentChatId.value = chatId
            _messages.value = emptyList()
        }
        messagesCollectionJob?.cancel()
        messagesCollectionJob = viewModelScope.launch {
            _messages.value = repository.getMessagesSync(chatId)
            repository.getMessages(chatId).collect { msgs ->
                if (_currentChatId.value == chatId) _messages.value = msgs
            }
        }
    }

    fun deleteChat(id: String) {
        viewModelScope.launch {
            repository.deleteChat(id)
            runCatching { _generatedMediaBytes.value = repository.generatedMediaBytes() }
        }
    }

    fun clearGeneratedMedia(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                repository.clearGeneratedMedia()
                _generatedMediaBytes.value = repository.generatedMediaBytes()
            }.onSuccess { onDone() }
                .onFailure { com.ariai.app.util.AppLog.warn(it, "clear generated media") }
        }
    }

    fun pinChat(chat: Chat) {
        viewModelScope.launch {
            repository.saveChat(chat.copy(isPinned = !chat.isPinned))
        }
    }

    /** Summary of older turns (if any) and the messages that still go to the model. */
    /** Keeps the newest turns within a conservative estimate of the selected model's context. */
    private fun recentForModel(history: List<ChatMessage>, contextWindow: Int): List<ChatMessage> {
        val tokenBudget = ((contextWindow.takeIf { it > 0 } ?: 8192) * 3L / 4L).coerceAtLeast(1024L)
        val mediaByteBudget = MAX_MEDIA_BYTES_PER_REQUEST
        var used = 0L
        var usedMediaBytes = 0L
        val newestFirst = mutableListOf<ChatMessage>()
        for (message in history.takeLast(RECENT_MESSAGES_FOR_MODEL).asReversed()) {
            val characters = message.content.length.toLong() + message.attachments.sumOf { it.textContent?.length?.toLong() ?: 0L }
            val mediaTokens = message.attachments.sumOf { attachment ->
                when (attachment.type) {
                    AttachmentType.IMAGE -> 768L
                    AttachmentType.VIDEO -> 2048L
                    else -> 0L
                }
            }
            val estimate = maxOf(1L, (characters + 3L) / 4L) + mediaTokens
            val messageMediaBytes = message.attachments.sumOf { it.estimatedBytes() }
            if (newestFirst.isNotEmpty() &&
                (used + estimate > tokenBudget || usedMediaBytes + messageMediaBytes > mediaByteBudget)
            ) break
            newestFirst += message
            used += estimate
            usedMediaBytes += messageMediaBytes
        }
        // Most providers expect the conversation to start with a user message.
        return newestFirst.asReversed().dropWhile { it.role != MessageRole.USER }
    }

    private fun Attachment.estimatedBytes(): Long =
        base64Data?.length?.toLong()?.times(3L)?.div(4L)
            ?: uri?.let { runCatching { java.io.File(it).length() }.getOrDefault(0L) }
            ?: 0L

    private fun activeContext(all: List<ChatMessage>): Pair<String?, List<ChatMessage>> {
        val idx = all.indexOfLast { it.role == MessageRole.SYSTEM }
        val summary = if (idx >= 0) all[idx].content else null
        val after = all.drop(idx + 1).filter {
            it.status != MessageStatus.ERROR && (it.content.isNotBlank() || it.attachments.isNotEmpty())
        }
        return summary to after
    }

    private fun resolveProvider(chatId: String? = null): Provider? = try {
        val providerId = (chatId?.let { _modelOverrides.value[it]?.first }) ?: getCurrentChat()?.providerId
        providers.value.find { it.id == providerId } ?: providers.value.firstOrNull()
    } catch (e: Exception) {
        null
    }

    /** The first attached photo, saved as a file so image models can edit it. */
    private suspend fun List<Attachment>.firstImageFile(): java.io.File? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val image = firstOrNull { it.type == AttachmentType.IMAGE } ?: return@withContext null
            val bytes = runCatching {
                image.base64Data?.takeIf { it.isNotBlank() }?.let {
                    android.util.Base64.decode(it, android.util.Base64.DEFAULT)
                } ?: image.uri?.let { path -> java.io.File(path).takeIf { it.isFile }?.readBytes() }
            }.getOrNull() ?: return@withContext null
            com.ariai.app.data.local.ImageStore.createTempImage(bytes, image.mimeType)
        }

    /** Creates an image (or edits [source]) and stores it as its own assistant message. */
    private suspend fun generateImageReply(
        chatId: String,
        provider: Provider,
        modelId: String,
        prompt: String,
        source: java.io.File?
    ) {
        if (source != null && provider.type == ProviderType.GEMINI) {
            com.ariai.app.data.local.ImageStore.deleteTemp(source)
            postAssistantError(chatId, tx("Image editing is not supported for Gemini providers yet."))
            return
        }
        _isStreaming.value = true
        try {
            val response = if (source == null) {
                repository.generateImage(provider, ImageGenRequest(prompt = prompt, model = modelId))
            } else {
                repository.editImage(provider, modelId, source, prompt)
            }
            val first = response.images.firstOrNull() ?: throw Exception(tx("No image returned"))
            val imageBytes = repository.imageBytes(first)
            val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.ariai.app.data.local.ImageStore.save(imageBytes)
            }
            val reply = ChatMessage(
                chatId = chatId,
                role = MessageRole.ASSISTANT,
                content = first.revisedPrompt.orEmpty(),
                modelId = modelId,
                providerId = provider.id,
                imagePath = file.absolutePath
            )
            if (!repository.saveMessage(reply)) {
                com.ariai.app.data.local.ImageStore.deleteOwnedFile(file.absolutePath)
                throw Exception(tx("Could not save the generated image"))
            }
            appendMessage(reply)
            refreshGeneratedMediaBytes()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            postAssistantError(chatId, "❌ Image error: ${e.message ?: e::class.java.simpleName}")
        } finally {
            source?.let { com.ariai.app.data.local.ImageStore.deleteTemp(it) }
            _isStreaming.value = false
        }
    }

    private suspend fun generateAudioReply(chatId: String, provider: Provider, modelId: String, prompt: String) {
        _isStreaming.value = true
        _streamingContent.value = tx("Generating audio…")
        try {
            val bytes = repository.generateAudio(provider, modelId, prompt) { _streamingContent.value = it }
            val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.ariai.app.data.local.ImageStore.saveAudio(bytes)
            }
            val reply = ChatMessage(
                chatId = chatId,
                role = MessageRole.ASSISTANT,
                content = "",
                modelId = modelId,
                providerId = provider.id,
                imagePath = file.absolutePath
            )
            if (!repository.saveMessage(reply)) {
                com.ariai.app.data.local.ImageStore.deleteOwnedFile(file.absolutePath)
                throw Exception(tx("Could not save the generated audio"))
            }
            appendMessage(reply)
            refreshGeneratedMediaBytes()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            postAssistantError(chatId, "❌ Audio error: ${e.message ?: e::class.java.simpleName}")
        } finally {
            _streamingContent.value = ""
            _isStreaming.value = false
        }
    }

    private suspend fun generateVideoReply(chatId: String, provider: Provider, modelId: String, prompt: String) {
        _isStreaming.value = true
        _streamingContent.value = tx("Generating video…")
        try {
            // The progress line shows in the reply bubble until the video is ready.
            val bytes = repository.generateVideo(provider, modelId, prompt) { _streamingContent.value = it }
            val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.ariai.app.data.local.ImageStore.saveVideo(bytes)
            }
            val reply = ChatMessage(
                chatId = chatId,
                role = MessageRole.ASSISTANT,
                content = "",
                modelId = modelId,
                providerId = provider.id,
                imagePath = file.absolutePath
            )
            if (!repository.saveMessage(reply)) {
                com.ariai.app.data.local.ImageStore.deleteOwnedFile(file.absolutePath)
                throw Exception(tx("Could not save the generated video"))
            }
            appendMessage(reply)
            refreshGeneratedMediaBytes()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            postAssistantError(chatId, "❌ Video error: ${e.message ?: e::class.java.simpleName}")
        } finally {
            _streamingContent.value = ""
            _isStreaming.value = false
        }
    }

    /** Sends an edit request for a generated image; the result is a separate image message. */
    fun editGeneratedImage(source: ChatMessage, instruction: String) {
        val path = source.imagePath ?: return
        val modelId = source.modelId ?: return
        val chatId = source.chatId
        if (instruction.isBlank() || _isStreaming.value || sendPending) return
        val provider = providers.value.firstOrNull { it.id == source.providerId } ?: resolveProvider(chatId) ?: return
        sendPending = true
        _isSending.value = true
        sendJob = viewModelScope.launch {
            try {
                val userMessage = ChatMessage(chatId = chatId, role = MessageRole.USER, content = "✏️ $instruction")
                if (!repository.saveMessage(userMessage)) {
                    postAssistantError(chatId, tx("Could not save your message. Check free storage and try again."))
                    return@launch
                }
                appendMessage(userMessage)
                generateImageReply(chatId, provider, modelId, instruction, java.io.File(path))
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                com.ariai.app.util.AppLog.warn(e, "edit image")
                postAssistantError(chatId, "❌ Image error: ${e.message ?: e::class.java.simpleName}")
            }
        }.also { job ->
            job.invokeOnCompletion {
                sendPending = false
                _isSending.value = false
            }
        }
    }

    private fun postAssistantError(chatId: String, text: String) {
        viewModelScope.launch {
            val message = ChatMessage(chatId = chatId, role = MessageRole.ASSISTANT, content = text, status = MessageStatus.ERROR)
            try {
                repository.saveMessage(message)
                appendMessage(message)
            } catch (e: Exception) { com.ariai.app.util.AppLog.warn(e) }
        }
    }

    fun sendMessage(
        content: String,
        attachments: List<Attachment> = emptyList(),
        reasoning: ReasoningLevel = ReasoningLevel.AUTO,
        searchMode: SearchMode = SearchMode.OFF,
        onUserMessageSaved: (Boolean) -> Unit = {}
    ): Boolean {
        val chatId = _currentChatId.value ?: return false
        if (content.isBlank() && attachments.isEmpty()) return false
        if (_isStreaming.value || sendPending) return false
        if (attachments.sumOf { it.estimatedBytes() } > MAX_MEDIA_BYTES_PER_REQUEST) {
            postAssistantError(chatId, tx("Attachments exceed the 16 MB per-request limit. Remove some files and try again."))
            return false
        }

        val provider = resolveProvider(chatId)
        if (provider == null) {
            postAssistantError(chatId, "⚠️ No provider configured. Add a provider in Settings > AI Providers.")
            return false
        }

        val localSearchKey = if (searchMode == SearchMode.LOCAL || (searchMode == SearchMode.MODEL && !supportsNativeSearch(provider))) {
            listOf("tavily", "brave", "exa", "serper").firstNotNullOfOrNull { name ->
                searchKeys.value[name]?.takeIf { it.isNotBlank() }?.let { name to it }
            }
        } else {
            null
        }
        if (searchMode == SearchMode.LOCAL && content.isNotBlank() && localSearchKey == null) {
            postAssistantError(chatId, "🔎 Local search needs an API key. Add one in Settings > Search service.")
            return false
        }

        // Set before launching so a fast second tap cannot start a second send.
        sendPending = true
        _isSending.value = true
        sendJob = viewModelScope.launch {
            try {
                val userMessage = ChatMessage(
                    chatId = chatId,
                    role = MessageRole.USER,
                    content = content,
                    attachments = attachments
                )
                if (!repository.saveMessage(userMessage)) {
                    onUserMessageSaved(false)
                    postAssistantError(chatId, tx("Could not save your message. Check free storage and try again."))
                    return@launch
                }
                onUserMessageSaved(true)
                appendMessage(repository.getMessageById(userMessage.id) ?: userMessage)

                val targetModelId = _modelOverrides.value[chatId]?.second
                    ?: getCurrentChat()?.modelId
                    ?: provider.models.firstOrNull()?.id
                val current = provider.models.firstOrNull { it.id == targetModelId }
                if ((_chatModes.value[chatId] ?: ChatMode.TEXT) == ChatMode.TEXT &&
                    attachments.any { it.type == AttachmentType.IMAGE } && current != null && !current.supportsVision
                ) {
                    postAssistantError(chatId, tx("This model is not marked as accepting images. Choose a vision model or enable Vision in its model settings."))
                    return@launch
                }
                when (_chatModes.value[chatId] ?: ChatMode.TEXT) {
                    ChatMode.IMAGE -> {
                        if (content.isBlank()) {
                            postAssistantError(chatId, tx("Describe the image you want, or explain how the attached image should change."))
                            return@launch
                        }
                        // Only a model flagged for image output generates; nothing else is guessed.
                        val found = findImageModel(provider)
                        if (found == null) {
                            postAssistantError(chatId, tx("No image model found. Add an image model, or fetch the models in provider settings."))
                            return@launch
                        }
                        val source = attachments.firstImageFile()
                        if (attachments.any { it.type == AttachmentType.IMAGE } && source == null) {
                            postAssistantError(chatId, tx("The attached image is no longer available. Please attach it again."))
                            return@launch
                        }
                        generateImageReply(chatId, found.first, found.second.id, content, source)
                        return@launch
                    }
                    ChatMode.AUDIO -> {
                        if (attachments.isNotEmpty()) {
                            postAssistantError(chatId, tx("Photos and videos are not used in audio mode. Switch to text mode to send them."))
                            return@launch
                        }
                        val found = findModel(provider) { it.supportsAudioGen }
                        if (found == null) {
                            postAssistantError(chatId, tx("No audio model found. Fetch the models of a provider that has one."))
                            return@launch
                        }
                        generateAudioReply(chatId, found.first, found.second.id, content)
                        return@launch
                    }
                    ChatMode.VIDEO -> {
                        if (attachments.isNotEmpty()) {
                            postAssistantError(chatId, tx("Photos and videos are not used in video mode yet. Switch to text mode to send them."))
                            return@launch
                        }
                        val found = findModel(provider) { it.supportsVideoGen }
                        if (found == null) {
                            postAssistantError(chatId, "⚠️ No video model found. Fetch the models of a provider that has one.")
                            return@launch
                        }
                        generateVideoReply(chatId, found.first, found.second.id, content)
                        return@launch
                    }
                    ChatMode.TEXT -> if (current != null && current.supportsAudioGen && !current.outputsText) {
                        // Audio-only model selected in text mode: generate audio instead of calling chat.
                        generateAudioReply(chatId, provider, current.id, content)
                        return@launch
                    } else if (current != null && ((current.supportsVideoGen && !current.outputsText) || looksLikeVideoModel(current.id))) {
                        // Video-only model selected in text mode: generate a video instead of calling chat.
                        generateVideoReply(chatId, provider, current.id, content)
                        return@launch
                    } else if (current != null && current.supportsImageGen && !current.outputsText) {
                        // Image-only model: text cannot work, so it generates an image.
                        if (content.isBlank()) {
                            postAssistantError(chatId, tx("Describe the image you want, or explain how the attached image should change."))
                            return@launch
                        }
                        val source = attachments.firstImageFile()
                        if (attachments.any { it.type == AttachmentType.IMAGE } && source == null) {
                            postAssistantError(chatId, tx("The attached image is no longer available. Please attach it again."))
                            return@launch
                        }
                        generateImageReply(chatId, provider, current.id, content, source)
                        return@launch
                    }
                }

                var searchContext: String? = null
                if (localSearchKey != null && content.isNotBlank()) {
                    try {
                        val sp = SearchProvider(
                            name = localSearchKey.first,
                            type = when (localSearchKey.first) {
                                "tavily" -> SearchProviderType.TAVILY
                                "brave" -> SearchProviderType.BRAVE
                                "exa" -> SearchProviderType.EXA
                                "serper" -> SearchProviderType.SERPER
                                else -> SearchProviderType.CUSTOM
                            },
                            apiKey = localSearchKey.second
                        )
                        val result = repository.searchWeb(content, sp)
                        searchContext = repository.formatSearchForLLM(result)
                        if (result.results.isEmpty()) {
                            postAssistantError(chatId, tx("Web search returned no results. Check the search service key and try again."))
                            return@launch
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        com.ariai.app.util.AppLog.warn(e, "web search")
                        postAssistantError(chatId, tx("Web search failed: ${e.message?.take(180) ?: "check the search service settings"}"))
                        return@launch
                    }
                }

                _isStreaming.value = true
                _streamingContent.value = ""

                var fullResponse = ""
                val requestModelId = targetModelId ?: "openai"
                try {
                    val (summary, allHistory) = activeContext(_messages.value)
                    val history = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        recentForModel(allHistory, current?.contextWindow ?: 8192)
                    }
                    val systemPrompt = listOfNotNull(
                        getCurrentChat()?.systemPrompt ?: getAgentSystemPrompt(),
                        summary?.let { "Summary of the earlier conversation:\n$it" }
                    ).joinToString("\n\n").ifBlank { null }

                    var lastPublishMs = 0L
                    repository.streamChat(
                        provider = provider,
                        messages = history,
                        modelId = requestModelId,
                        systemPrompt = systemPrompt,
                        searchContext = searchContext,
                        reasoning = reasoning,
                        nativeSearch = searchMode == SearchMode.MODEL && supportsNativeSearch(provider),
                        stream = streamResponse.value
                    ).collect { chunk ->
                        fullResponse += chunk
                        // Publish at most ~16 times per second; each publish recomposes and re-renders markdown.
                        val now = System.currentTimeMillis()
                        if (now - lastPublishMs >= 60) {
                            lastPublishMs = now
                            _streamingContent.value = fullResponse
                        }
                    }
                    _streamingContent.value = fullResponse

                    if (fullResponse.isBlank()) {
                        throw Exception(tx("Empty response from API"))
                    }

                    val assistantMessage = ChatMessage(
                        chatId = chatId,
                        role = MessageRole.ASSISTANT,
                        content = fullResponse,
                        modelId = requestModelId,
                        providerId = provider.id
                    )
                    if (!repository.saveMessage(assistantMessage)) {
                        postAssistantError(chatId, tx("Could not save the reply. Check free storage."))
                    }
                    appendMessage(assistantMessage)

                    if (_messages.value.size <= 3) {
                        updateChatTitle(chatId, content.ifBlank { attachments.first().name }.take(30))
                    }

                } catch (e: kotlinx.coroutines.CancellationException) {
                    // Stopped by the user: keep the text that has arrived so far as the reply.
                    if (fullResponse.isNotBlank()) {
                        val partial = ChatMessage(
                            chatId = chatId,
                            role = MessageRole.ASSISTANT,
                            content = fullResponse,
                            modelId = requestModelId,
                            providerId = provider.id
                        )
                        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                            repository.saveMessage(partial)
                            appendMessage(partial)
                        }
                    }
                    throw e
                } catch (e: Exception) {
                    val errorMsg = when {
                        e.message?.contains("API Error 401") == true -> tx("🔑 Invalid API key. Please check your provider settings in AI Providers.")
                        e.message?.contains("API Error 429") == true -> tx("⏳ Rate limit exceeded. Please wait a moment or try another provider.")
                        e.message?.contains("Unable to resolve host") == true -> tx("🌐 No internet connection. Please check your network.")
                        else -> "❌ Error: ${e.message?.take(300)}\n\nPlease check your API key and provider settings."
                    }

                    val errorMessage = ChatMessage(
                        chatId = chatId,
                        role = MessageRole.ASSISTANT,
                        content = errorMsg,
                        status = MessageStatus.ERROR
                    )
                    try {
                        repository.saveMessage(errorMessage)
                        appendMessage(errorMessage)
                    } catch (e2: Exception) {}
                } finally {
                    _isStreaming.value = false
                    _streamingContent.value = ""
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                _isStreaming.value = false
                _streamingContent.value = ""
                throw e
            } catch (e: Exception) {
                com.ariai.app.util.AppLog.warn(e, "sendMessage")
                _isStreaming.value = false
                _streamingContent.value = ""
            }
        }.also { job ->
            job.invokeOnCompletion {
                sendPending = false
                _isSending.value = false
            }
        }
        return true
    }

    /**
     * Replaces the older part of the conversation with a model-written summary, so long
     * chats stay within context. [onDone] receives a short message for the user.
     */
    fun compressHistory(onDone: (String) -> Unit) {
        val chatId = _currentChatId.value ?: return
        if (_isStreaming.value) {
            onDone(tx("Wait for the reply to finish first"))
            return
        }
        val provider = resolveProvider()
        if (provider == null) {
            onDone(tx("Add a provider first"))
            return
        }
        val (previousSummary, history) = activeContext(_messages.value)
        if (history.size < 2) {
            onDone(tx("Not enough messages to compress yet"))
            return
        }
        viewModelScope.launch {
            _isStreaming.value = true
            try {
                val instruction = ChatMessage(
                    chatId = chatId,
                    role = MessageRole.USER,
                    content = "Summarize the conversation so far for your own later reference. Keep facts about the user, decisions, preferences and open tasks. Reply in the language the user used. Be concise."
                )
                val modelId = getCurrentChat()?.modelId ?: provider.models.firstOrNull()?.id ?: "openai"
                val systemPrompt = previousSummary?.let { "Summary of the earlier conversation:\n$it" }
                val summary = StringBuilder()
                repository.streamChat(
                    provider = provider,
                    messages = history + instruction,
                    modelId = modelId,
                    systemPrompt = systemPrompt,
                    stream = streamResponse.value
                ).collect { summary.append(it) }

                val text = summary.toString().trim()
                if (text.isBlank()) throw Exception("empty summary")
                val marker = ChatMessage(chatId = chatId, role = MessageRole.SYSTEM, content = text)
                repository.saveMessage(marker)
                appendMessage(marker)
                onDone(tx("Older messages compressed"))
            } catch (e: Exception) {
                onDone("Compression failed: ${e.message?.take(120)}")
            } finally {
                _isStreaming.value = false
            }
        }
    }

    fun setReasoningLevel(level: ReasoningLevel) {
        viewModelScope.launch { prefs.setReasoningLevel(level.name) }
    }

    fun setSearchMode(mode: SearchMode) {
        viewModelScope.launch { prefs.setSearchMode(mode.name) }
    }

    fun toggleFavoriteModel(providerId: String, modelId: String) {
        viewModelScope.launch { prefs.toggleFavoriteModel(favoriteKey(providerId, modelId)) }
    }

    fun updateChatProvider(chatId: String, providerId: String, modelId: String) {
        // Keep the choice in memory first so the chip and sending use it immediately,
        // then persist it to the chat row.
        _modelOverrides.value = _modelOverrides.value + (chatId to (providerId to modelId))
        viewModelScope.launch {
            try {
                val chat = chats.value.find { it.id == chatId } ?: repository.getChatById(chatId)
                if (chat != null) repository.saveChat(chat.copy(providerId = providerId, modelId = modelId))
            } catch (e: Exception) { com.ariai.app.util.AppLog.warn(e) }
        }
    }

    fun branchMessage(message: ChatMessage): String {
        val newChatId = java.util.UUID.randomUUID().toString()
        val currentMessages = _messages.value
        val branchIndex = currentMessages.indexOfFirst { it.id == message.id }
        val sourceMessages = if (branchIndex >= 0) currentMessages.take(branchIndex + 1) else emptyList()
        val newIds = sourceMessages.associate { it.id to java.util.UUID.randomUUID().toString() }
        val branchedMessages = sourceMessages.map { source ->
            source.copy(
                id = newIds.getValue(source.id),
                chatId = newChatId,
                parentId = source.parentId?.let(newIds::get),
                branchChildren = source.branchChildren.mapNotNull(newIds::get)
            )
        }

        sendJob?.cancel()
        messagesCollectionJob?.cancel()
        _currentChatId.value = newChatId
        _messages.value = emptyList()
        viewModelScope.launch {
            val originalChat = repository.getChatById(message.chatId) ?: return@launch
            val now = System.currentTimeMillis()
            val newChat = originalChat.copy(
                id = newChatId,
                title = "${originalChat.title} (Branch)",
                createdAt = now,
                updatedAt = now
            )
            repository.saveChat(newChat)
            branchedMessages.forEach { repository.saveMessage(it) }
            if (_currentChatId.value == newChatId) {
                _messages.value = repository.getMessagesSync(newChatId)
            }
        }
        return newChatId
    }

    fun regenerateMessage(message: ChatMessage) {
        // Delete this message and resend previous user message
        viewModelScope.launch {
            val idx = _messages.value.indexOf(message)
            if (idx > 0) {
                val prevUserMessage = _messages.value[idx - 1]
                if (prevUserMessage.role == MessageRole.USER) {
                    repository.deleteMessage(message.id)
                    _messages.value = _messages.value.filter { it.id != message.id }
                    sendMessage(prevUserMessage.content, prevUserMessage.attachments)
                }
            }
        }
    }

    fun generateImage(prompt: String, model: String, size: String) {
        val provider = providers.value.firstOrNull { it.models.any { m -> m.supportsImageGen } }
            ?: providers.value.firstOrNull() ?: return

        viewModelScope.launch {
            _isGeneratingImage.value = true
            try {
                val request = ImageGenRequest(
                    prompt = prompt,
                    model = model,
                    size = size
                )
                val response = repository.generateImage(provider, request)
                _generatedImages.value = response.images + _generatedImages.value
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isGeneratingImage.value = false
            }
        }
    }

    private fun getCurrentChat(): Chat? {
        val id = _currentChatId.value ?: return null
        return chats.value.find { it.id == id }
    }

    private fun getAgentSystemPrompt(): String? {
        val chat = getCurrentChat() ?: return null
        val agentId = chat.agentId ?: return null
        val agent = agents.value.find { it.id == agentId } ?: return null
        return agent.systemPrompt
            .replace("{time}", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
            .replace("{model}", chat.modelId ?: tx("AI"))
            .replace("{date}", java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()))
    }

    /** Renames a chat; blank titles are ignored. */
    fun renameChat(chatId: String, newTitle: String) {
        val title = newTitle.trim()
        if (title.isBlank()) return
        viewModelScope.launch {
            val chat = repository.getChatById(chatId) ?: return@launch
            repository.saveChat(chat.copy(title = title))
        }
    }

    private fun updateChatTitle(chatId: String, newTitle: String) {
        viewModelScope.launch {
            val chat = repository.getChatById(chatId) ?: return@launch
            if (chat.title == "New Chat") {
                repository.saveChat(chat.copy(title = newTitle))
            }
        }
    }
}

/** A model makes images if the provider flagged it or its name says so. */
fun isImageModel(model: com.ariai.app.data.models.AIModel): Boolean = model.supportsImageGen
