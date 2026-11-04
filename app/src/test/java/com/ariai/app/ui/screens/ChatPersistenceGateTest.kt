package com.ariai.app.ui.screens

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPersistenceGateTest {
    @Test
    fun firstMessageWaitsUntilNewChatInsertCompletes() = runBlocking {
        val gate = ChatPersistenceGate()
        val ticket = gate.begin("new-chat")
        val firstSend = async { gate.await("new-chat") }

        yield()
        assertFalse("send must wait while the new chat insert is in flight", firstSend.isCompleted)

        gate.complete("new-chat", ticket, saved = true)
        assertTrue(firstSend.await())
        assertTrue("later sends should not need a gate", gate.await("new-chat"))
    }

    @Test
    fun failedChatInsertBlocksMessageSavesUntilChatIsDiscarded() = runBlocking {
        val gate = ChatPersistenceGate()
        val ticket = gate.begin("failed-chat")
        gate.complete("failed-chat", ticket, saved = false)

        assertFalse(gate.await("failed-chat"))
        assertFalse(gate.await("failed-chat"))

        gate.forget("failed-chat")
        assertTrue("a removed chat has no pending persistence barrier", gate.await("failed-chat"))
    }
}
