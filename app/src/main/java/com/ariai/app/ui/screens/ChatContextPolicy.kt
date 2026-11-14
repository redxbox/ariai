package com.ariai.app.ui.screens

import com.ariai.app.util.tx
import com.ariai.app.data.models.*
import com.ariai.app.data.remote.*

/** Output kind selected for a chat composer. */
enum class ChatMode { TEXT, IMAGE, VIDEO, AUDIO }

/** A model makes images if the provider flagged it or its name says so. */
fun isImageModel(model: com.ariai.app.data.models.AIModel): Boolean = model.supportsImageGen

/** Summary of older turns (if any) and the messages that still go to the model. */
internal fun activeContext(all: List<ChatMessage>): Pair<String?, List<ChatMessage>> {
    val idx = all.indexOfLast { it.role == MessageRole.SYSTEM }
    val summary = if (idx >= 0) all[idx].content else null
    val after = messagesForModelContext(all.drop(idx + 1))
    return summary to after
}

internal fun summarySafeHistory(history: List<ChatMessage>): List<ChatMessage> {
    var remainingChars = MAX_SUMMARY_SOURCE_CHARS
    val newestFirst = history.asReversed().mapNotNull { message ->
        if (remainingChars <= 0) return@mapNotNull null
        val content = message.content.take(remainingChars)
        remainingChars -= content.length
        val textAttachments = mutableListOf<Attachment>()
        for (attachment in message.attachments) {
            val attachmentText = attachment.textContent ?: continue
            if (remainingChars <= 0) break
            val accepted = attachmentText.take(remainingChars)
            remainingChars -= accepted.length
            if (accepted.isNotBlank()) {
                textAttachments += attachment.copy(textContent = accepted, base64Data = null, uri = null)
            }
        }
        val omittedMedia = message.attachments.filter {
            it.type == AttachmentType.IMAGE || it.type == AttachmentType.VIDEO || it.type == AttachmentType.AUDIO
        }
        val mediaNote = omittedMedia.joinToString(separator = "\n") { attachment ->
            "[A ${attachment.type.name.lowercase()} attachment named ${attachment.name} was omitted from this summary request.]"
        }.take(remainingChars)
        remainingChars -= mediaNote.length
        val safeContent = listOf(content, mediaNote).filter { it.isNotBlank() }.joinToString("\n")
        if (safeContent.isBlank() && textAttachments.isEmpty()) return@mapNotNull null
        message.copy(content = safeContent, attachments = textAttachments)
    }
    return newestFirst.asReversed()
}

private suspend fun summarizeAndStoreHistory(
    chatId: String,
    provider: Provider,
    modelId: String,
    previousSummary: String?,
    olderMessages: List<ChatMessage>,
    retainedMessages: List<ChatMessage>,
    stream: Boolean,
    shouldContinue: () -> Boolean
): String {
    if (olderMessages.size < 2 || retainedMessages.isEmpty()) throw IllegalStateException(tx("Not enough messages to compress yet"))
    val markerTimestamp = summaryMarkerTimestamp(_messages.value, retainedMessages.first())
        ?: throw IllegalStateException(tx("History cannot be summarized safely right now. Try again later."))
    val instruction = ChatMessage(
        chatId = chatId,
        role = MessageRole.USER,
        content = "Create a concise factual memory of only these earlier conversation messages for future turns. Keep user facts, decisions, preferences, names, and unfinished tasks. Do not answer the conversation or invent details. Write in the user's language."
    )
    val summary = StringBuilder()
    repository.streamChat(
        provider = provider,
        messages = summarySafeHistory(olderMessages) + instruction,
        modelId = modelId,
        systemPrompt = previousSummary?.let { "Summary of the earlier conversation:\n$it" },
        stream = stream
    ).collect { chunk ->
        if (shouldContinue()) summary.append(chunk)
    }
    val text = summary.toString().trim()
    if (!shouldContinue()) return ""
    if (text.isBlank()) throw IllegalStateException(tx("The provider returned no text response"))
    val marker = ChatMessage(
        chatId = chatId,
        role = MessageRole.SYSTEM,
        content = text,
        timestamp = markerTimestamp
    )
    if (!repository.saveMessage(marker)) throw IllegalStateException(tx("Could not save the reply. Please try again."))
    appendMessage(marker)
    return text
}

private suspend fun maybeAutoSummarizeHistoryBeforeSend(
    chatId: String,
    provider: Provider,
    modelId: String,
    contextWindow: Int,
    generation: Long
) {
    if (_currentChatId.value != chatId || historyCompressionJob?.isActive == true) return
    val (previousSummary, history) = activeContext(_messages.value)
    val limit = contextWindow.takeIf { it > 0 } ?: 8192
    val estimatedTokens = history.sumOf(::estimateMessageTokens)
    val conservativeHistoryBudget = ((limit.toLong() * 2L / 3L) * 3L / 4L).coerceAtLeast(1024L)
    val summaryThreshold = conservativeHistoryBudget * 9L / 10L
    // Reserve space for system instructions and tools; summarize just before the
    // request builder would start dropping the oldest conversation turns.
    if (estimatedTokens < summaryThreshold) return
    val (older, retained) = splitHistoryForSummary(history)
    if (older.size < 2 || retained.isEmpty()) return
    if (summaryMarkerTimestamp(_messages.value, retained.first()) == null) return

    setSendStreamingState(generation, streaming = true)
    try {
        summarizeAndStoreHistory(
            chatId = chatId,
            provider = provider,
            modelId = modelId,
            previousSummary = previousSummary,
            olderMessages = older,
            retainedMessages = retained,
            stream = streamResponse.value
        ) {
            sendGeneration == generation && _currentChatId.value == chatId
        }
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        if (sendGeneration == generation && _currentChatId.value == chatId) {
            com.ariai.app.util.AppLog.warn(failure, "automatic history summary")
        }
    } finally {
        setSendStreamingState(generation, streaming = false)
    }
}

internal fun resolvePromptVariables(prompt: String, agent: Agent?, chat: Chat, modelName: String): String {
    val now = java.util.Date()
    val time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(now)
    val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(now)
    val replacements = mapOf(
        "time" to time,
        "date" to date,
        "model" to modelName,
        "model_name" to modelName
    ) + agent?.promptVariables.orEmpty()
    return replacements.entries.sortedByDescending { it.key.length }.fold(prompt) { text, (key, value) ->
        if (key.isBlank()) text else text.replace("{{$key}}", value).replace("{$key}", value)
    }
}
