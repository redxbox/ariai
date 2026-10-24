package com.ariai.app.data.repository

import com.ariai.app.data.local.*
import com.ariai.app.data.models.*
import com.ariai.app.data.remote.AIClient
import com.ariai.app.data.remote.SearchClient
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.emitAll
import com.ariai.app.data.models.AttachmentType
import kotlinx.coroutines.withContext
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

    /** Asks the provider's API which models it supports. Throws on failure. */
    suspend fun fetchModels(provider: Provider): List<AIModel> = aiClient.listModels(provider)

    suspend fun getProviderById(id: String): Provider? = try {
        db.providerDao().getProviderById(id)?.toModel()
    } catch (e: Exception) {
        null
    }

    suspend fun saveProvider(provider: Provider): Boolean = persistWithResult(
        prepare = { withContext(Dispatchers.IO) { provider.toEntity() } },
        persist = { entity -> db.providerDao().insertProvider(entity) },
        onFailure = { com.ariai.app.util.AppLog.warn(it, "saveProvider") }
    )

    /** Safely upgrades legacy plain-text provider keys without decrypting or replacing ciphertext. */
    suspend fun migrateProviderKeysToEncrypted() {
        val providers = try {
            db.providerDao().getAllProvidersSync()
        } catch (e: Exception) {
            com.ariai.app.util.AppLog.warn(e, "provider key migration")
            return
        }
        providers.forEach { entity ->
            if (entity.apiKey.isNotBlank() && !entity.apiKey.startsWith("enc1:")) {
                runCatching {
                    db.providerDao().insertProvider(entity.copy(apiKey = com.ariai.app.util.KeyVault.encrypt(entity.apiKey)))
                }.onFailure { com.ariai.app.util.AppLog.warn(it, "provider key migration") }
            }
        }
    }

    suspend fun deleteProvider(id: String) {
        try {
            db.providerDao().deleteById(id)
        } catch (e: Exception) { com.ariai.app.util.AppLog.warn(e) }
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

    /** Returns whether the chat row was persisted; failures are logged for device diagnostics. */
    suspend fun saveChat(chat: Chat): Boolean = persistWithResult(
        prepare = { chat.toEntity() },
        persist = { entity -> db.chatDao().insertChat(entity) },
        onFailure = { com.ariai.app.util.AppLog.warn(it, "saveChat") }
    )

    suspend fun deleteChat(id: String) {
        try {
            val removedPaths = db.withTransaction {
                val removedMessages = db.messageDao().getMessagesForChatSync(id)
                db.messageDao().deleteMessagesForChat(id)
                db.chatDao().deleteById(id)
                removedMessages.flatMap { it.storagePaths() }.toSet()
            }
            deleteUnreferencedMedia(removedPaths)
        } catch (e: Exception) {
            com.ariai.app.util.AppLog.warn(e, "deleteChat")
        }
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

    suspend fun getMessageById(id: String): ChatMessage? = try {
        db.messageDao().getMessageById(id)?.toModel()
    } catch (e: Exception) {
        null
    }

    /** Returns false when the message could not be stored, so the caller can tell the user. */
    suspend fun saveMessage(message: ChatMessage): Boolean {
        val createdFiles = mutableListOf<java.io.File>()
        return try {
            val stored = withContext(Dispatchers.IO) {
                message.copy(attachments = message.attachments.map { attachment ->
                    attachment.persisted { createdFiles += it }
                })
            }
            db.withTransaction {
                val chat = db.chatDao().getChatById(message.chatId) ?: run {
                    if (message.role != MessageRole.USER) {
                        throw IllegalStateException(
                            "Cannot save ${message.role} message because chat row ${message.chatId} does not exist"
                        )
                    }
                    // A first user send must not be lost just because the screen's chat row was
                    // not present when this transaction began. Recreate only for user messages,
                    // so a late assistant error cannot resurrect a chat that was intentionally deleted.
                    // IGNORE avoids overwriting a row inserted concurrently by createNewChat.
                    db.chatDao().insertChatIfAbsent(
                        ChatEntity(
                            id = message.chatId,
                            title = "New Chat",
                            createdAt = message.timestamp,
                            updatedAt = message.timestamp,
                            providerId = message.providerId,
                            modelId = message.modelId,
                            agentId = null,
                            systemPrompt = null,
                            isPinned = false,
                            isArchived = false,
                            memoryEnabled = false
                        )
                    )
                    com.ariai.app.util.AppLog.warn(
                        IllegalStateException("Recovered missing chat row for chatId=${message.chatId}"),
                        "saveMessage"
                    )
                    db.chatDao().getChatById(message.chatId)
                        ?: throw IllegalStateException("Could not recreate chat row for chatId=${message.chatId}")
                }
                db.messageDao().insertMessage(stored.toEntity())
                db.chatDao().insertChat(chat.copy(updatedAt = System.currentTimeMillis()))
            }
            true
        } catch (e: Exception) {
            withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
                createdFiles.forEach { it.delete() }
            }
            if (e is kotlinx.coroutines.CancellationException) throw e
            com.ariai.app.util.AppLog.warn(e, "saveMessage")
            false
        }
    }

    suspend fun deleteMessage(id: String) {
        try {
            val removedPaths = db.withTransaction {
                val removed = db.messageDao().getMessageById(id)
                db.messageDao().deleteById(id)
                removed?.storagePaths().orEmpty()
            }
            deleteUnreferencedMedia(removedPaths)
        } catch (e: Exception) {
            com.ariai.app.util.AppLog.warn(e, "deleteMessage")
        }
    }

    private suspend fun deleteUnreferencedMedia(paths: Set<String>) {
        if (paths.isEmpty()) return
        val remainingReferences = db.messageDao().getAllMessagesSync()
            .flatMapTo(mutableSetOf()) { it.storagePaths() }
        withContext(Dispatchers.IO) {
            (paths - remainingReferences).forEach { ImageStore.deleteOwnedFile(it) }
        }
    }

    suspend fun generatedMediaBytes(): Long = withContext(Dispatchers.IO) {
        val legacyAttachments = db.messageDao().getAllMessagesSync()
            .flatMapTo(mutableSetOf()) { parseAttachments(it.attachmentsJson).mapNotNull { a -> a.uri } }
        ImageStore.generatedBytes(legacyAttachments)
    }

    suspend fun clearGeneratedMedia(): Int = withContext(Dispatchers.IO) {
        val legacyAttachments = db.messageDao().getAllMessagesSync()
            .flatMapTo(mutableSetOf()) { parseAttachments(it.attachmentsJson).mapNotNull { a -> a.uri } }
        ImageStore.clearGenerated(legacyAttachments)
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
                    } catch (e: Exception) { com.ariai.app.util.AppLog.warn(e) }
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
        } catch (e: Exception) { com.ariai.app.util.AppLog.warn(e) }
    }

    suspend fun deleteAgent(id: String) {
        try {
            db.agentDao().deleteById(id)
        } catch (e: Exception) { com.ariai.app.util.AppLog.warn(e) }
    }

    // AI Operations - with error handling
    fun agentChat(
        provider: Provider,
        messages: List<ChatMessage>,
        modelId: String,
        systemPrompt: String?,
        tools: List<com.ariai.app.data.remote.ChatFunctionTool>,
        temperature: Float = 0.7f,
        searchContext: String? = null,
        reasoning: ReasoningLevel = ReasoningLevel.AUTO,
        nativeSearch: Boolean = false,
        maxSteps: Int = 6,
        contextWindow: Int = 8192,
        allowUrlContext: Boolean = true
    ): Flow<String> = kotlinx.coroutines.flow.flow {
        val readyMessages = messages.map { m -> m.copy(attachments = m.attachments.map { it.hydrated() }) }
        val toolBudget = ((contextWindow.coerceAtLeast(4096).toLong() * 3L) / 2L).coerceAtMost(20_000L).toInt()
        val loop = com.ariai.app.data.remote.AgentLoop(maxSteps = maxSteps, maxTotalToolChars = toolBudget)
        val answer = loop.run(readyMessages, tools) { stepMessages, stepTools, recovery ->
            val stepPrompt = if (recovery) {
                listOfNotNull(
                    systemPrompt,
                    "Do not answer with an acknowledgement or progress-only message. Complete the request now using the available tool results, or clearly ask for the exact missing input. If a tool step limit was reached, summarize only what was actually retrieved."
                ).joinToString("\n\n")
            } else systemPrompt
            aiClient.chatCompletionStep(
                provider = provider,
                messages = stepMessages,
                modelId = modelId,
                systemPrompt = stepPrompt,
                tools = stepTools,
                temperature = temperature,
                searchContext = searchContext,
                reasoning = reasoning,
                nativeSearch = nativeSearch,
                allowUrlContext = allowUrlContext
            )
        }
        emit(answer)
    }.flowOn(Dispatchers.IO)

    fun streamChat(
        provider: Provider,
        messages: List<ChatMessage>,
        modelId: String,
        systemPrompt: String?,
        temperature: Float = 0.7f,
        searchContext: String? = null,
        reasoning: ReasoningLevel = ReasoningLevel.AUTO,
        nativeSearch: Boolean = false,
        stream: Boolean = true,
        allowUrlContext: Boolean = true
    ): Flow<String> = kotlinx.coroutines.flow.flow {
        // Load stored attachment bytes here, off the main thread, right before the request.
        val ready = messages.map { m -> m.copy(attachments = m.attachments.map { it.hydrated() }) }
        emitAll(aiClient.chatCompletionStream(
            provider = provider,
            messages = ready,
            modelId = modelId,
            systemPrompt = systemPrompt,
            temperature = temperature,
            searchContext = searchContext,
            reasoning = reasoning,
            nativeSearch = nativeSearch,
            stream = stream,
            allowUrlContext = allowUrlContext
        ))
    }
        // Network work must not run on the main thread.
        .flowOn(Dispatchers.IO)

    suspend fun editImage(provider: Provider, model: String, image: java.io.File, prompt: String) =
        withContext(Dispatchers.IO) { aiClient.editImage(provider, model, image, prompt) }

    suspend fun imageBytes(image: com.ariai.app.data.models.GeneratedImage): ByteArray =
        aiClient.imageBytes(image)

    suspend fun generateAudio(
        provider: Provider,
        modelId: String,
        prompt: String,
        onProgress: (String) -> Unit = {}
    ): ByteArray = withContext(Dispatchers.IO) { aiClient.generateAudio(provider, modelId, prompt, onProgress) }

    suspend fun generateVideo(
        provider: Provider,
        modelId: String,
        prompt: String,
        onProgress: (String) -> Unit = {}
    ): ByteArray = withContext(Dispatchers.IO) { aiClient.generateVideo(provider, modelId, prompt, onProgress) }

    suspend fun generateImage(provider: Provider, request: ImageGenRequest) = 
        withContext(Dispatchers.IO) { aiClient.generateImage(provider, request) }

    suspend fun searchWeb(query: String, searchProvider: SearchProvider): SearchResponse =
        searchClient.search(query, searchProvider)

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
            apiKey = com.ariai.app.util.KeyVault.decrypt(apiKey),
            enabled = enabled,
            customHeaders = try { Converters().toStringMap(customHeaders) } catch (e: Exception) { emptyMap() },
            customBody = customBody,
            createdAt = createdAt,
            models = com.ariai.app.data.models.modelsFromJson(modelsJson).ifEmpty {
                // Providers saved before model storage existed start with the defaults.
                if (modelsJson == "[]") com.ariai.app.data.models.getDefaultModelsForType(runCatching { ProviderType.valueOf(type) }.getOrDefault(ProviderType.OPENAI_COMPATIBLE), id) else emptyList()
            }
        )
    } catch (e: Exception) {
        Provider(id = id, name = name.ifBlank { "Unknown" }, type = ProviderType.OPENAI_COMPATIBLE, baseUrl = baseUrl.ifBlank { "https://api.openai.com/v1" }, apiKey = com.ariai.app.util.KeyVault.decrypt(apiKey))
    }

    private fun Provider.toEntity() = ProviderEntity(
        id = id,
        name = name,
        type = type.name,
        baseUrl = baseUrl,
        // Encrypt at rest; KeyVault.decrypt() still reads legacy plain-text database rows.
        apiKey = com.ariai.app.util.KeyVault.encrypt(apiKey),
        enabled = enabled,
        customHeaders = try { Converters().fromStringMap(customHeaders) } catch (e: Exception) { "" },
        customBody = customBody,
        createdAt = createdAt,
        modelsJson = com.ariai.app.data.models.modelsToJson(models)
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

    /** Writes attachment data to its own storage area; failures must not silently drop the bytes. */
    private fun Attachment.persisted(onFileSaved: (java.io.File) -> Unit): Attachment {
        val data = base64Data ?: return this
        val bytes = android.util.Base64.decode(data, android.util.Base64.DEFAULT)
        val file = ImageStore.saveAttachment(bytes, mimeType, type)
        onFileSaved(file)
        return copy(uri = file.absolutePath, base64Data = null)
    }

    private fun MessageEntity.storagePaths(): Set<String> = buildSet {
        imagePath?.let { add(it) }
        parseAttachments(attachmentsJson).forEach { attachment ->
            attachment.uri?.let { add(it) }
        }
    }

    /** Loads the bytes of a stored attachment so a model can receive it. */
    private fun Attachment.hydrated(): Attachment {
        if (base64Data != null || uri == null) return this
        return try {
            val bytes = java.io.File(uri).readBytes()
            copy(base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
        } catch (e: Exception) {
            this
        }
    }

    private fun parseAttachments(json: String): List<Attachment> = try {
        val array = org.json.JSONArray(json)
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Attachment(
                id = o.getString("id"),
                type = AttachmentType.valueOf(o.getString("type")),
                name = o.getString("name"),
                uri = o.optString("uri").ifBlank { null },
                mimeType = o.optString("mimeType"),
                textContent = o.optString("textContent").ifBlank { null }
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun List<Attachment>.toJson(): String {
        val array = org.json.JSONArray()
        forEach { a ->
            array.put(org.json.JSONObject().apply {
                put("id", a.id)
                put("type", a.type.name)
                put("name", a.name)
                put("uri", a.uri ?: "")
                put("mimeType", a.mimeType)
                put("textContent", a.textContent ?: "")
            })
        }
        return array.toString()
    }

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
            reasoning = reasoning,
            imagePath = imagePath,
            attachments = parseAttachments(attachmentsJson)
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
        reasoning = reasoning,
        imagePath = imagePath,
        attachmentsJson = attachments.toJson()
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
