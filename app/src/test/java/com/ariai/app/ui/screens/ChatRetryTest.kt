package com.ariai.app.ui.screens

import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRetryTest {
    @Test
    fun `retry branch includes only the original context through the requested user turn`() {
        val history = listOf(
            ChatMessage(id = "u1", chatId = "c", role = MessageRole.USER, content = "first"),
            ChatMessage(id = "a1", chatId = "c", role = MessageRole.ASSISTANT, content = "answer one"),
            ChatMessage(id = "u2", chatId = "c", role = MessageRole.USER, content = "retry this"),
            ChatMessage(id = "a2", chatId = "c", role = MessageRole.ASSISTANT, content = "old answer"),
            ChatMessage(id = "u3", chatId = "c", role = MessageRole.USER, content = "later turn")
        )

        val branch = messagesThroughUserTurn(history, "u2")

        assertEquals(listOf("u1", "a1", "u2"), branch.map { it.id })
        assertTrue(branch.none { it.id == "a2" || it.id == "u3" })
    }

    @Test
    fun `unknown retry request returns no context`() {
        assertTrue(messagesThroughUserTurn(emptyList(), "missing").isEmpty())
    }
}
