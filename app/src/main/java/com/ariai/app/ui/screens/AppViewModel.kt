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
    val dynamicColor = prefs.dynamicColorFlow.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val searchKeys = prefs.searchKeysFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // Data
    val providers = repository.getProviders().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val chats = repository.getChats().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val agents = repository.getAgents().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Auto-add free demo provider on first launch
        viewModelScope.launch {
            // Wait a bit for providers to load
            kotlinx.coroutines.delay(500)
            if (providers.value.isEmpty()) {
                val freeDemo = getFreeProviders().find { it.id == "pollinations" }
                freeDemo?.let {
                    val demoProvider = Provider(
                        id = "demo_pollinations",
                        name = "AriAi Free Demo",
                        type = ProviderType.OPENAI_COMPATIBLE,
                        baseUrl = it.baseUrl,
                        apiKey = "",
                        models = listOf(
                            AIModel("openai", "Pollinations OpenAI", "demo_pollinations", true, true, false, 32000),
                            AIModel("openai-large", "Pollinations Large", "demo_pollinations", true, true, false, 32000),
                            AIModel("mistral", "Mistral Free", "demo_pollinations", true, true, false, 32000)
                        ),
                        enabled = true
                    )
                    repository.saveProvider(demoProvider)
                }
                // Also add Groq as second demo if user wants speed (without key it will fail but we show it)
            }
        }
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

    fun saveProvider(provider: Provider) {
        viewModelScope.launch {
            val models = if (provider.models.isEmpty()) {
                getDefaultModelsForType(provider.type, provider.id)
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
            providerId = providerId ?: providers.value.firstOrNull()?.id,
            modelId = modelId ?: providers.value.firstOrNull()?.models?.firstOrNull()?.id,
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

    fun sendMessage(content: String, useWebSearch: Boolean = false) {
        val chatId = _currentChatId.value ?: return
        if (content.isBlank()) return
        
        val provider = try {
            providers.value.find { it.id == getCurrentChat()?.providerId } ?: providers.value.firstOrNull()
        } catch (e: Exception) {
            null
        }
        
        if (provider == null) {
            viewModelScope.launch {
                val errorMessage = ChatMessage(
                    chatId = chatId,
                    role = MessageRole.ASSISTANT,
                    content = "⚠️ No provider configured. Please add a provider in Settings > Providers. Free demo (Pollinations) should be auto-added. If not, add manually: https://text.pollinations.ai/openai",
                    status = MessageStatus.ERROR
                )
                try {
                    repository.saveMessage(errorMessage)
                    _messages.value = _messages.value + errorMessage
                } catch (e: Exception) {}
            }
            return
        }

        viewModelScope.launch {
            try {
                val userMessage = ChatMessage(
                    chatId = chatId,
                    role = MessageRole.USER,
                    content = content
                )
                repository.saveMessage(userMessage)
                _messages.value = _messages.value + userMessage

                // Check if web search needed
                var searchContext: String? = null
                if (useWebSearch || content.contains(Regex("search|اخبار|جستجو|what's latest|current", RegexOption.IGNORE_CASE))) {
                    val searchProviderKey = searchKeys.value.entries.firstOrNull { it.value.isNotBlank() }
                    if (searchProviderKey != null) {
                        try {
                            val sp = SearchProvider(
                                name = searchProviderKey.key,
                                type = when (searchProviderKey.key) {
                                    "tavily" -> SearchProviderType.TAVILY
                                    "brave" -> SearchProviderType.BRAVE
                                    "exa" -> SearchProviderType.EXA
                                    "serper" -> SearchProviderType.SERPER
                                    else -> SearchProviderType.CUSTOM
                                },
                                apiKey = searchProviderKey.value
                            )
                            val result = repository.searchWeb(content, sp)
                            searchContext = repository.formatSearchForLLM(result)
                        } catch (e: Exception) {
                            // Search failed, continue without
                        }
                    }
                }

                _isStreaming.value = true
                _streamingContent.value = ""

                try {
                    val allMessages = _messages.value.toList()
                    val systemPrompt = getCurrentChat()?.systemPrompt ?: getAgentSystemPrompt()

                    var fullResponse = ""
                    repository.streamChat(
                        provider = provider,
                        messages = allMessages,
                        modelId = getCurrentChat()?.modelId ?: provider.models.firstOrNull()?.id ?: "openai",
                        systemPrompt = systemPrompt,
                        searchContext = searchContext
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

                    // Auto title generation for first message
                    if (_messages.value.size <= 3) {
                        updateChatTitle(chatId, content.take(30))
                    }

                } catch (e: Exception) {
                    val errorMsg = when {
                        e.message?.contains("API Error 401") == true -> "🔑 Invalid API key. Please check your provider settings.\n\nFor free demo, use Pollinations (no key) or get free Groq key at console.groq.com"
                        e.message?.contains("API Error 429") == true -> "⏳ Rate limit exceeded. Please wait a moment or try another provider.\n\nFree providers have limits: Groq 14.4K/day, Gemini 1500/day"
                        e.message?.contains("Unable to resolve host") == true -> "🌐 No internet connection. Please check your network."
                        else -> "❌ Error: ${e.message?.take(300)}\n\nPlease check your API key and provider settings. Try free demo: Pollinations (no key needed)"
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
                // Ultimate crash prevention
                _isStreaming.value = false
                _streamingContent.value = ""
            }
        }
    }

    fun updateChatProvider(chatId: String, providerId: String, modelId: String) {
        viewModelScope.launch {
            try {
                val chat = repository.getChatById(chatId) ?: return@launch
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
