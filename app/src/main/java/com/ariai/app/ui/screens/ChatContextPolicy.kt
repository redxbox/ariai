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
