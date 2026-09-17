package com.ariai.app.data.repository

import com.ariai.app.data.local.*
import com.ariai.app.data.models.*
import com.ariai.app.data.remote.AIClient
import com.ariai.app.data.remote.SearchClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository(
    private val db: AppDatabase,
    private val aiClient: AIClient,
    private val searchClient: SearchClient
) {
    // Providers
    fun getProviders(): Flow<List<Provider>> = db.providerDao().getAllProviders().map { list ->
        list.map { it.toModel() }
    }

    suspend fun getProviderById(id: String): Provider? = db.providerDao().getProviderById(id)?.toModel()

    suspend fun saveProvider(provider: Provider) {
        db.providerDao().insertProvider(provider.toEntity())
    }

    suspend fun deleteProvider(id: String) {
        db.providerDao().deleteById(id)
    }

    // Chats
    fun getChats(): Flow<List<Chat>> = db.chatDao().getAllChats().map { list ->
        list.map { it.toModel() }
    }

    suspend fun getChatById(id: String): Chat? = db.chatDao().getChatById(id)?.toModel()

    suspend fun saveChat(chat: Chat) {
        db.chatDao().insertChat(chat.toEntity())
    }

    suspend fun deleteChat(id: String) {
        db.messageDao().deleteMessagesForChat(id)
        db.chatDao().deleteById(id)
    }

    // Messages
    fun getMessages(chatId: String): Flow<List<ChatMessage>> = 
        db.messageDao().getMessagesForChat(chatId).map { list ->
            list.map { it.toModel() }
        }

    suspend fun getMessagesSync(chatId: String): List<ChatMessage> =
        db.messageDao().getMessagesForChatSync(chatId).map { it.toModel() }

    suspend fun saveMessage(message: ChatMessage) {
        db.messageDao().insertMessage(message.toEntity())
        // Update chat timestamp
        db.chatDao().getChatById(message.chatId)?.let { chat ->
            db.chatDao().insertChat(chat.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteMessage(id: String) {
        db.messageDao().deleteById(id)
    }

    // Agents
    fun getAgents(): Flow<List<Agent>> = db.agentDao().getAllAgents().map { list ->
        if (list.isEmpty()) {
            // Insert built-in agents on first run
            getBuiltInAgents().forEach { agent ->
                db.agentDao().insertAgent(agent.toEntity())
            }
            getBuiltInAgents()
        } else {
            list.map { it.toModel() }
        }
    }

    suspend fun saveAgent(agent: Agent) {
        db.agentDao().insertAgent(agent.toEntity())
    }

    suspend fun deleteAgent(id: String) {
        db.agentDao().deleteById(id)
    }

    // AI Operations
    fun streamChat(
        provider: Provider,
        messages: List<ChatMessage>,
        modelId: String,
        systemPrompt: String?,
        temperature: Float = 0.7f,
        searchContext: String? = null
    ) = aiClient.chatCompletionStream(provider, messages, modelId, systemPrompt, temperature, searchContext != null, searchContext)

    suspend fun generateImage(provider: Provider, request: ImageGenRequest) =
        aiClient.generateImage(provider, request)

    suspend fun searchWeb(query: String, searchProvider: SearchProvider): SearchResponse =
        searchClient.search(query, searchProvider)

    fun formatSearchForLLM(response: SearchResponse) = searchClient.formatResultsForLLM(response)

    // Mappers
    private fun ProviderEntity.toModel() = Provider(
        id = id,
        name = name,
        type = ProviderType.valueOf(type),
        baseUrl = baseUrl,
        apiKey = apiKey,
        enabled = enabled,
        customHeaders = Converters().toStringMap(customHeaders),
        customBody = customBody,
        createdAt = createdAt
    )

    private fun Provider.toEntity() = ProviderEntity(
        id = id,
        name = name,
        type = type.name,
        baseUrl = baseUrl,
        apiKey = apiKey,
        enabled = enabled,
        customHeaders = Converters().fromStringMap(customHeaders),
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

    private fun MessageEntity.toModel() = ChatMessage(
        id = id,
        chatId = chatId,
        role = MessageRole.valueOf(role),
        content = content,
        timestamp = timestamp,
        status = MessageStatus.valueOf(status),
        modelId = modelId,
        providerId = providerId,
        parentId = parentId,
        branchChildren = if (branchChildren.isEmpty()) emptyList() else branchChildren.split("|||"),
        reasoning = reasoning
    )

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
