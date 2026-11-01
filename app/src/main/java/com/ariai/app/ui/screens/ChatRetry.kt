package com.ariai.app.ui.screens

import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import com.ariai.app.data.models.MessageStatus

/** Return the original conversation prefix through the exact user request being retried. */
internal fun messagesThroughUserTurn(history: List<ChatMessage>, userMessageId: String): List<ChatMessage> {
    val index = history.indexOfFirst { it.id == userMessageId && it.role == MessageRole.USER }
    return if (index < 0) emptyList() else history.take(index + 1)
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
