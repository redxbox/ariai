package com.ariai.app.ui.screens

import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import com.ariai.app.data.models.MessageStatus
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
    fun `failed user turns are omitted from the next model request`() {
        val history = listOf(
            ChatMessage(id = "u1", chatId = "c", role = MessageRole.USER, content = "successful question"),
            ChatMessage(id = "a1", chatId = "c", role = MessageRole.ASSISTANT, content = "successful answer"),
            ChatMessage(id = "u2", chatId = "c", role = MessageRole.USER, content = "failed question"),
            ChatMessage(
                id = "a2",
                chatId = "c",
                role = MessageRole.ASSISTANT,
                content = "API Error 402",
                status = MessageStatus.ERROR,
                parentId = "u2"
            ),
            ChatMessage(id = "u3", chatId = "c", role = MessageRole.USER, content = "new question")
        )

        assertEquals(listOf("u1", "a1", "u3"), messagesForModelContext(history).map { it.id })
    }

    @Test
    fun `failed prompt remains available when an explicit retry removes its error`() {
        val failedPrompt = ChatMessage(id = "u1", chatId = "c", role = MessageRole.USER, content = "retry me")

        assertEquals(listOf(failedPrompt), messagesForModelContext(listOf(failedPrompt)))
    }

    @Test
    fun `unknown retry request returns no context`() {
        assertTrue(messagesThroughUserTurn(emptyList(), "missing").isEmpty())
    }
}
