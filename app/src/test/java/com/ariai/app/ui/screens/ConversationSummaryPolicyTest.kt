package com.ariai.app.ui.screens

import com.ariai.app.data.models.Attachment
import com.ariai.app.data.models.AttachmentType
import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationSummaryPolicyTest {
    @Test
    fun `split preserves a complete latest user turn`() {
        val history = listOf(
            message(MessageRole.USER, 1),
            message(MessageRole.ASSISTANT, 2),
            message(MessageRole.USER, 3),
            message(MessageRole.ASSISTANT, 4),
            message(MessageRole.USER, 5),
            message(MessageRole.ASSISTANT, 6),
            message(MessageRole.USER, 7),
            message(MessageRole.ASSISTANT, 8),
            message(MessageRole.USER, 9),
            message(MessageRole.ASSISTANT, 10)
        )

        val (older, retained) = splitHistoryForSummary(history, keepAtLeast = 3)

        assertEquals(listOf(1, 2, 3, 4, 5, 6), older.map { it.timestamp.toInt() })
        assertEquals(listOf(7, 8, 9, 10), retained.map { it.timestamp.toInt() })
        assertEquals(MessageRole.USER, retained.first().role)
    }

    @Test
    fun `summary marker is positioned between old and retained turns`() {
        val older = message(MessageRole.ASSISTANT, 100)
        val retained = message(MessageRole.USER, 110)
        val all = listOf(older, retained)

        val markerTimestamp = summaryMarkerTimestamp(all, retained)

        assertEquals(105L, markerTimestamp)
    }

    @Test
    fun `summary marker is refused when timestamps leave no ordering gap`() {
        val prior = message(MessageRole.ASSISTANT, 10)
        val retained = message(MessageRole.USER, 11)

        assertNull(summaryMarkerTimestamp(listOf(prior, retained), retained))
    }

    @Test
    fun `token estimate accounts for extracted text and media`() {
        val message = ChatMessage(
            chatId = "chat",
            role = MessageRole.USER,
            content = "hello",
            attachments = listOf(
                Attachment(
                    type = AttachmentType.PDF,
                    name = "notes.pdf",
                    mimeType = "application/pdf",
                    textContent = "abcd"
                ),
                Attachment(
                    type = AttachmentType.IMAGE,
                    name = "photo.webp",
                    mimeType = "image/webp"
                )
            )
        )

        assertEquals(771L, estimateMessageTokens(message))
    }

    @Test
    fun `history too short to split is retained whole`() {
        val history = listOf(message(MessageRole.USER, 1), message(MessageRole.ASSISTANT, 2))

        val (older, retained) = splitHistoryForSummary(history)

        assertEquals(history, older)
        assertTrue(retained.isEmpty())
    }

    private fun message(role: MessageRole, timestamp: Long) = ChatMessage(
        chatId = "chat",
        role = role,
        content = "message-$timestamp",
        timestamp = timestamp
    )
}
