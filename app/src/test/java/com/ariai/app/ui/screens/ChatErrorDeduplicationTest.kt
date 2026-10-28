package com.ariai.app.ui.screens

import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import com.ariai.app.data.models.MessageStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatErrorDeduplicationTest {
    @Test
    fun repeatedLatestStorageErrorIsNotAppendedAgain() {
        val error = ChatMessage(
            chatId = "chat",
            role = MessageRole.ASSISTANT,
            content = "Could not save your message. Please try again.",
            status = MessageStatus.ERROR
        )

        assertTrue(repeatsLatestAssistantError(listOf(error), error.content, error.parentId))
    }

    @Test
    fun aDifferentLatestTurnDoesNotSuppressTheError() {
        val userTurn = ChatMessage(chatId = "chat", role = MessageRole.USER, content = "Try again")

        assertFalse(repeatsLatestAssistantError(listOf(userTurn), "Could not save your message. Please try again.", null))
    }
}
