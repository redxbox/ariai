package com.ariai.app.data.repository

import com.ariai.app.data.local.*
import com.ariai.app.data.models.*
import com.ariai.app.data.remote.AIClient
import com.ariai.app.data.remote.SearchClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class ChatRepository(
    private val db: AppDatabase,
    private val aiClient: AIClient,
    private val searchClient: SearchClient
) {
    // Providers - crash-proof
    fun getProviders(): Flow<List<Provider>> = db.providerDao().getAllProviders()
        .map { list ->
            try {
                list.map { it.toModel() }
            } catch (e: Exception) {
                emptyList()
            }
        }
        .catch { emit(emptyList()) }

    suspend fun getProviderById(id: String): Provider? = try {
        db.providerDao().getProviderById(id)?.toModel()
    } catch (e: Exception) {
        null
    }

    suspend fun saveProvider(provider: Provider) {
        try {
            db.providerDao().insertProvider(provider.toEntity())
        } catch (e: Exception) {
            // Log but don't crash
        }
    }

    suspend fun deleteProvider(id: String) {
        try {
            db.providerDao().deleteById(id)
        } catch (e: Exception) {}
    }

    // Chats
    fun getChats(): Flow<List<Chat>> = db.chatDao().getAllChats()
        .map { list ->
            try {
                list.map { it.toModel() }.sortedByDescending { it.updatedAt }
            } catch (e: Exception) {
                emptyList()
            }
        }
        .catch { emit(emptyList()) }

    suspend fun getChatById(id: String): Chat? = try {
        db.chatDao().getChatById(id)?.toModel()
    } catch (e: Exception) {
        null
    }

    suspend fun saveChat(chat: Chat) {
        try {
            db.chatDao().insertChat(chat.toEntity())
        } catch (e: Exception) {}
    }

    suspend fun deleteChat(id: String) {
        try {
            db.messageDao().deleteMessagesForChat(id)
            db.chatDao().deleteById(id)
        } catch (e: Exception) {}
    }

    // Messages
    fun getMessages(chatId: String): Flow<List<ChatMessage>> = 
        db.messageDao().getMessagesForChat(chatId)
            .map { list ->
                try {
                    list.map { it.toModel() }.sortedBy { it.timestamp }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            .catch { emit(emptyList()) }

    suspend fun getMessagesSync(chatId: String): List<ChatMessage> = try {
        db.messageDao().getMessagesForChatSync(chatId).map { it.toModel() }.sortedBy { it.timestamp }
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun saveMessage(message: ChatMessage) {
        try {
            db.messageDao().insertMessage(message.toEntity())
            // Update chat timestamp
            try {
                db.chatDao().getChatById(message.chatId)?.let { chat ->
                    db.chatDao().insertChat(chat.copy(updatedAt = System.currentTimeMillis()))
                }
            } catch (e: Exception) {}
        } catch (e: Exception) {}
    }

    suspend fun deleteMessage(id: String) {
        try {
            db.messageDao().deleteById(id)
        } catch (e: Exception) {}
    }

    // Agents
    fun getAgents(): Flow<List<Agent>> = db.agentDao().getAllAgents()
        .map { list ->
            try {
                if (list.isEmpty()) {
                    // Insert built-in agents on first run
                    try {
                        getBuiltInAgents().forEach { agent ->
                            db.agentDao().insertAgent(agent.toEntity())
                        }
                    } catch (e: Exception) {}
                    getBuiltInAgents()
                } else {
                    list.map { it.toModel() }
                }
            } catch (e: Exception) {
                getBuiltInAgents()
            }
        }
        .catch { emit(getBuiltInAgents()) }

    suspend fun saveAgent(agent: Agent) {
        try {
            db.agentDao().insertAgent(agent.toEntity())
        } catch (e: Exception) {}
    }

    suspend fun deleteAgent(id: String) {
        try {
            db.agentDao().deleteById(id)
        } catch (e: Exception) {}
    }

    // AI Operations - with error handling
    fun streamChat(
        provider: Provider,
        messages: List<ChatMessage>,
        modelId: String,
        systemPrompt: String?,
        temperature: Float = 0.7f,
        searchContext: String? = null
    ) = aiClient.chatCompletionStream(provider, messages, modelId, systemPrompt, temperature, searchContext != null, searchContext)

    suspend fun generateImage(provider: Provider, request: ImageGenRequest) = try {
        aiClient.generateImage(provider, request)
    } catch (e: Exception) {
        // Fallback to Pollinations
        val encodedPrompt = java.net.URLEncoder.encode(request.prompt, "UTF-8")
        val imageUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=1024&height=1024&model=flux&nologo=true"
        ImageGenResponse(
            images = listOf(GeneratedImage(url = imageUrl, revisedPrompt = request.prompt)),
            model = "pollinations-fallback"
        )
    }

    suspend fun searchWeb(query: String, searchProvider: SearchProvider): SearchResponse = try {
        searchClient.search(query, searchProvider)
    } catch (e: Exception) {
        SearchResponse(query, emptyList(), searchProvider.type)
    }

    fun formatSearchForLLM(response: SearchResponse) = try {
        searchClient.formatResultsForLLM(response)
    } catch (e: Exception) {
        "Search failed: ${e.message}"
    }

    // Mappers with safety
    private fun ProviderEntity.toModel() = try {
        Provider(
            id = id,
            name = name,
            type = try { ProviderType.valueOf(type) } catch (e: Exception) { ProviderType.OPENAI_COMPATIBLE },
            baseUrl = baseUrl,
            apiKey = apiKey,
            enabled = enabled,
            customHeaders = try { Converters().toStringMap(customHeaders) } catch (e: Exception) { emptyMap() },
            customBody = customBody,
            createdAt = createdAt
        )
    } catch (e: Exception) {
        Provider(id = id, name = name.ifBlank { "Unknown" }, type = ProviderType.OPENAI_COMPATIBLE, baseUrl = baseUrl.ifBlank { "https://text.pollinations.ai/openai" }, apiKey = apiKey)
    }

    private fun Provider.toEntity() = ProviderEntity(
        id = id,
        name = name,
        type = type.name,
        baseUrl = baseUrl,
        apiKey = apiKey,
        enabled = enabled,
        customHeaders = try { Converters().fromStringMap(customHeaders) } catch (e: Exception) { "" },
        customBody = customBody,
        createdAt = createdAt
    )

    private fun ChatEntity.toModel() = Chat(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        providerId = providerId,
        modelId = modelId,
        agentId = agentId,
        systemPrompt = systemPrompt,
        isPinned = isPinned,
        isArchived = isArchived,
        memoryEnabled = memoryEnabled
    )

    private fun Chat.toEntity() = ChatEntity(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        providerId = providerId,
        modelId = modelId,
        agentId = agentId,
        systemPrompt = systemPrompt,
        isPinned = isPinned,
        isArchived = isArchived,
        memoryEnabled = memoryEnabled
    )

    private fun MessageEntity.toModel() = try {
        ChatMessage(
            id = id,
            chatId = chatId,
            role = try { MessageRole.valueOf(role) } catch (e: Exception) { MessageRole.USER },
            content = content,
            timestamp = timestamp,
            status = try { MessageStatus.valueOf(status) } catch (e: Exception) { MessageStatus.SENT },
            modelId = modelId,
            providerId = providerId,
            parentId = parentId,
            branchChildren = if (branchChildren.isEmpty()) emptyList() else branchChildren.split("|||"),
            reasoning = reasoning
        )
    } catch (e: Exception) {
        ChatMessage(id = id, chatId = chatId, role = MessageRole.USER, content = content, timestamp = timestamp)
    }

    private fun ChatMessage.toEntity() = MessageEntity(
        id = id,
        chatId = chatId,
        role = role.name,
        content = content,
        timestamp = timestamp,
        status = status.name,
        modelId = modelId,
        providerId = providerId,
        parentId = parentId,
        branchChildren = branchChildren.joinToString("|||"),
        reasoning = reasoning
    )

    private fun AgentEntity.toModel() = Agent(
        id = id,
        name = name,
        description = description,
        systemPrompt = systemPrompt,
        avatar = avatar,
        modelId = modelId,
        providerId = providerId,
        temperature = temperature,
        topP = topP,
        isBuiltIn = isBuiltIn,
        createdAt = createdAt
    )

    private fun Agent.toEntity() = AgentEntity(
        id = id,
        name = name,
        description = description,
        systemPrompt = systemPrompt,
        avatar = avatar,
        modelId = modelId,
        providerId = providerId,
        temperature = temperature,
        topP = topP,
        isBuiltIn = isBuiltIn,
        createdAt = createdAt
    )
}
