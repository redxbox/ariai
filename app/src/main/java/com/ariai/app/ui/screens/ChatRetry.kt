package com.ariai.app.ui.screens

import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole

/** Return the original conversation prefix through the exact user request being retried. */
internal fun messagesThroughUserTurn(history: List<ChatMessage>, userMessageId: String): List<ChatMessage> {
    val index = history.indexOfFirst { it.id == userMessageId && it.role == MessageRole.USER }
    return if (index < 0) emptyList() else history.take(index + 1)
}
