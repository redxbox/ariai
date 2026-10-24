package com.ariai.app.data.remote

import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRoutingTest {
    @Test
    fun `calculator is exposed for explicit or recognizable arithmetic requests`() {
        assertTrue(shouldOfferCalculatorTool("What is 2 * 3?"))
        assertTrue(shouldOfferCalculatorTool("Please calculate the square root of 81"))
        assertTrue(shouldOfferCalculatorTool("لطفاً این را محاسبه کن"))
        assertFalse(shouldOfferCalculatorTool("Explain why leaves change color."))
    }

    @Test
    fun `recent user supplied GitHub links stay available for follow-up requests`() {
        val history = listOf(
            ChatMessage(
                chatId = "chat",
                role = MessageRole.USER,
                content = "Please review https://github.com/redxbox/ariai"
            ),
            ChatMessage(
                chatId = "chat",
                role = MessageRole.ASSISTANT,
                content = "Another link is https://assistant.example/private"
            ),
            ChatMessage(chatId = "chat", role = MessageRole.USER, content = "Can you review that repository now?")
        )

        val targets = userResearchTargets(history) { true }

        assertEquals("redxbox", targets.githubRepository?.owner)
        assertEquals("ariai", targets.githubRepository?.name)
        assertEquals(listOf("https://github.com/redxbox/ariai"), targets.publicUrls)
    }

    @Test
    fun `stream preference applies to ordinary tool-free replies only`() {
        assertTrue(shouldStreamToolFreeChat(streamEnabled = true, hasTools = false))
        assertFalse(shouldStreamToolFreeChat(streamEnabled = false, hasTools = false))
        assertFalse(shouldStreamToolFreeChat(streamEnabled = true, hasTools = true))
    }
}
