package com.ariai.app.ui.screens

import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import com.ariai.app.data.models.MessageStatus

/** Return the original conversation prefix through the exact user request being retried. */
internal fun messagesThroughUserTurn(history: List<ChatMessage>, userMessageId: String): List<ChatMessage> {
    val index = history.indexOfFirst { it.id == userMessageId && it.role == MessageRole.USER }
    return if (index < 0) emptyList() else history.take(index + 1)
}

/** Precompute retry availability in one pass instead of scanning the entire chat per row. */
internal fun retryableAssistantMessageFlags(messages: List<ChatMessage>): List<Boolean> {
    val userMessageIds = messages.asSequence()
        .filter { it.role == MessageRole.USER }
        .map { it.id }
        .toHashSet()
    var lastUserId: String? = null
    return messages.map { message ->
        val associatedUserId = (if (message.status == MessageStatus.ERROR) message.parentId else null) ?: lastUserId
        val canRetry = message.role == MessageRole.ASSISTANT && associatedUserId != null && associatedUserId in userMessageIds
        if (message.role == MessageRole.USER) lastUserId = message.id
        canRetry
    }
}

/** Exclude failed user turns from later requests while retaining them in the visible chat. */
internal fun messagesForModelContext(messages: List<ChatMessage>): List<ChatMessage> {
    val failedUserTurnIds = messages.asSequence()
        .filter { it.role == MessageRole.ASSISTANT && it.status == MessageStatus.ERROR }
        .mapNotNull { it.parentId }
        .toSet()

    return messages.filter { message ->
        message.status != MessageStatus.ERROR &&
            !(message.role == MessageRole.USER && message.id in failedUserTurnIds) &&
            (message.content.isNotBlank() || message.attachments.isNotEmpty())
    }
}
