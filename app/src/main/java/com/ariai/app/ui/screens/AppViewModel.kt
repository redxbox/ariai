package com.ariai.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ariai.app.data.local.PreferencesManager
import com.ariai.app.data.models.*
import com.ariai.app.data.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
    val showReasoning = prefs.showReasoningFlow.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val fontSize = prefs.fontSizeFlow.stateIn(viewModelScope, SharingStarted.Eagerly, 15)
    val reasoningLevel = prefs.reasoningLevelFlow.map { ReasoningLevel.fromName(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReasoningLevel.AUTO)
    val searchMode = prefs.searchModeFlow.map { SearchMode.fromName(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SearchMode.OFF)
    val favoriteModels = prefs.favoriteModelsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun setDefaultSelection(providerId: String, modelId: String) {
        viewModelScope.launch { prefs.setDefaultSelection(providerId, modelId) }
    }

    fun setStreamResponse(enabled: Boolean) {
        viewModelScope.launch { prefs.setStreamResponse(enabled) }
    }

    fun setShowReasoning(enabled: Boolean) {
        viewModelScope.launch { prefs.setShowReasoning(enabled) }
    }

    fun setFontSize(size: Int) {
        viewModelScope.launch { prefs.setFontSize(size) }
    }

    // Data
    val providers = repository.getProviders().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val chats = repository.getChats().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val agents = repository.getAgents().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // No auto demo - user must add provider manually to avoid crash
        // Free demo removed per user request to fix crash
    }

    // Chat state
    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId = _currentChatId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming = _isStreaming.asStateFlow()

    private val _streamingContent = MutableStateFlow("")
    val streamingContent = _streamingContent.asStateFlow()

    private val _generatedImages = MutableStateFlow<List<GeneratedImage>>(emptyList())
    val generatedImages = _generatedImages.asStateFlow()

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

    fun createNewChat(providerId: String? = null, modelId: String? = null, agentId: String? = null): String {
        val id = java.util.UUID.randomUUID().toString()
        val chat = Chat(
            id = id,
            title = "New Chat",
            providerId = providerId ?: defaultProviderId.value ?: providers.value.firstOrNull()?.id,
            modelId = modelId ?: defaultModelId.value ?: providers.value.firstOrNull()?.models?.firstOrNull()?.id,
            agentId = agentId
        )
        viewModelScope.launch {
            repository.saveChat(chat)
            _currentChatId.value = id
            _messages.value = emptyList()
        }
        return id
    }

    fun loadChat(chatId: String) {
        _currentChatId.value = chatId
        viewModelScope.launch {
            _messages.value = repository.getMessagesSync(chatId)
            // Also collect flow for future updates if needed
            repository.getMessages(chatId).collect { msgs ->
                if (_currentChatId.value == chatId && !_isStreaming.value) {
                    _messages.value = msgs
                }
            }
        }
    }

    fun deleteChat(id: String) {
        viewModelScope.launch { repository.deleteChat(id) }
    }

    fun pinChat(chat: Chat) {
        viewModelScope.launch {
            repository.saveChat(chat.copy(isPinned = !chat.isPinned))
        }
    }

    /** Summary of older turns (if any) and the messages that still go to the model. */
    private fun activeContext(all: List<ChatMessage>): Pair<String?, List<ChatMessage>> {
        val idx = all.indexOfLast { it.role == MessageRole.SYSTEM }
        val summary = if (idx >= 0) all[idx].content else null
        val after = all.drop(idx + 1).filter {
            it.status != MessageStatus.ERROR && (it.content.isNotBlank() || it.attachments.isNotEmpty())
        }
        return summary to after
    }

    private fun resolveProvider(): Provider? = try {
        providers.value.find { it.id == getCurrentChat()?.providerId } ?: providers.value.firstOrNull()
    } catch (e: Exception) {
        null
    }

    private fun postAssistantError(chatId: String, text: String) {
        viewModelScope.launch {
            val message = ChatMessage(chatId = chatId, role = MessageRole.ASSISTANT, content = text, status = MessageStatus.ERROR)
            try {
                repository.saveMessage(message)
                _messages.value = _messages.value + message
            } catch (_: Exception) {}
        }
    }

    fun sendMessage(
        content: String,
        attachments: List<Attachment> = emptyList(),
        reasoning: ReasoningLevel = ReasoningLevel.AUTO,
        searchMode: SearchMode = SearchMode.OFF
    ) {
        val chatId = _currentChatId.value ?: return
        if (content.isBlank() && attachments.isEmpty()) return
        if (_isStreaming.value) return

        val provider = resolveProvider()
        if (provider == null) {
            postAssistantError(chatId, "⚠️ No provider configured. Add a provider in Settings > AI Providers.")
            return
        }

        val localSearchKey = if (searchMode == SearchMode.LOCAL) {
            searchKeys.value.entries.firstOrNull { it.value.isNotBlank() }
        } else {
            null
        }
        if (searchMode == SearchMode.LOCAL && localSearchKey == null) {
            postAssistantError(chatId, "🔎 Local search needs an API key. Add one in Settings > Search service.")
            return
        }

        viewModelScope.launch {
            try {
                val userMessage = ChatMessage(
                    chatId = chatId,
                    role = MessageRole.USER,
                    content = content,
                    attachments = attachments
                )
                repository.saveMessage(userMessage)
                _messages.value = _messages.value + userMessage

                var searchContext: String? = null
                if (localSearchKey != null) {
                    try {
                        val sp = SearchProvider(
                            name = localSearchKey.key,
                            type = when (localSearchKey.key) {
                                "tavily" -> SearchProviderType.TAVILY
                                "brave" -> SearchProviderType.BRAVE
                                "exa" -> SearchProviderType.EXA
                                "serper" -> SearchProviderType.SERPER
                                else -> SearchProviderType.CUSTOM
                            },
                            apiKey = localSearchKey.value
                        )
                        val result = repository.searchWeb(content, sp)
                        searchContext = repository.formatSearchForLLM(result)
                    } catch (e: Exception) {
                        // Search failed: answer without results instead of blocking the message.
                    }
                }

                _isStreaming.value = true
                _streamingContent.value = ""

                try {
                    val (summary, history) = activeContext(_messages.value)
                    val systemPrompt = listOfNotNull(
                        getCurrentChat()?.systemPrompt ?: getAgentSystemPrompt(),
                        summary?.let { "Summary of the earlier conversation:\n$it" }
                    ).joinToString("\n\n").ifBlank { null }

                    var fullResponse = ""
                    repository.streamChat(
                        provider = provider,
                        messages = history,
                        modelId = getCurrentChat()?.modelId ?: provider.models.firstOrNull()?.id ?: "openai",
                        systemPrompt = systemPrompt,
                        searchContext = searchContext,
                        reasoning = reasoning,
                        nativeSearch = searchMode == SearchMode.MODEL
                    ).collect { chunk ->
                        fullResponse += chunk
                        _streamingContent.value = fullResponse
                    }

                    if (fullResponse.isBlank()) {
                        throw Exception("Empty response from API")
                    }

                    val assistantMessage = ChatMessage(
                        chatId = chatId,
                        role = MessageRole.ASSISTANT,
                        content = fullResponse,
                        modelId = getCurrentChat()?.modelId,
                        providerId = provider.id
                    )
                    repository.saveMessage(assistantMessage)
                    _messages.value = _messages.value + assistantMessage

                    if (_messages.value.size <= 3) {
                        updateChatTitle(chatId, content.ifBlank { attachments.first().name }.take(30))
                    }

                } catch (e: Exception) {
                    val errorMsg = when {
                        e.message?.contains("API Error 401") == true -> "🔑 Invalid API key. Please check your provider settings in AI Providers."
                        e.message?.contains("API Error 429") == true -> "⏳ Rate limit exceeded. Please wait a moment or try another provider."
                        e.message?.contains("Unable to resolve host") == true -> "🌐 No internet connection. Please check your network."
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
                        _messages.value = _messages.value + errorMessage
                    } catch (e2: Exception) {}
                } finally {
                    _isStreaming.value = false
                    _streamingContent.value = ""
                }
            } catch (e: Exception) {
                _isStreaming.value = false
                _streamingContent.value = ""
            }
        }
    }

    /**
     * Replaces the older part of the conversation with a model-written summary, so long
     * chats stay within context. [onDone] receives a short message for the user.
     */
    fun compressHistory(onDone: (String) -> Unit) {
        val chatId = _currentChatId.value ?: return
        if (_isStreaming.value) {
            onDone("Wait for the reply to finish first")
            return
        }
        val provider = resolveProvider()
        if (provider == null) {
            onDone("Add a provider first")
            return
        }
        val (previousSummary, history) = activeContext(_messages.value)
        if (history.size < 2) {
            onDone("Not enough messages to compress yet")
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
                    systemPrompt = systemPrompt
                ).collect { summary.append(it) }

                val text = summary.toString().trim()
                if (text.isBlank()) throw Exception("empty summary")
                val marker = ChatMessage(chatId = chatId, role = MessageRole.SYSTEM, content = text)
                repository.saveMessage(marker)
                _messages.value = _messages.value + marker
                onDone("Older messages compressed")
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
        viewModelScope.launch {
            try {
                val chat = chats.value.find { it.id == chatId } ?: repository.getChatById(chatId) ?: return@launch
                repository.saveChat(chat.copy(providerId = providerId, modelId = modelId))
            } catch (e: Exception) {}
        }
    }

    fun branchMessage(message: ChatMessage) {
        // Create new chat from this message
        viewModelScope.launch {
            val originalChat = repository.getChatById(message.chatId) ?: return@launch
            val newChatId = java.util.UUID.randomUUID().toString()
            val newChat = originalChat.copy(
                id = newChatId,
                title = "${originalChat.title} (Branch)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveChat(newChat)

            // Copy messages up to this message
            val messagesUpToBranch = _messages.value.filter { it.timestamp <= message.timestamp }
            messagesUpToBranch.forEach { msg ->
                val newMsg = msg.copy(id = java.util.UUID.randomUUID().toString(), chatId = newChatId)
                repository.saveMessage(newMsg)
            }

            _currentChatId.value = newChatId
            _messages.value = repository.getMessagesSync(newChatId)
        }
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
                    sendMessage(prevUserMessage.content)
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
            .replace("{model}", chat.modelId ?: "AI")
            .replace("{date}", java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()))
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
