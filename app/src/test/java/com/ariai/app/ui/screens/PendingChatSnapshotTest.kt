package com.ariai.app.ui.screens

import com.ariai.app.data.models.Chat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingChatSnapshotTest {
    @Test
    fun selectedAgentChatIsAvailableBeforeTheDatabaseFlowEmitsIt() {
        val agentChat = Chat(
            id = "pending-agent-chat",
            title = "Research Assistant",
            providerId = "agent-provider",
            modelId = "agent-model",
            agentId = "researcher",
            temperature = 0.25f,
            topP = 0.8f
        )

        assertEquals(
            "the first send and chat screen must keep the selected agent's model and settings",
            agentChat,
            chatSnapshotForId(agentChat.id, persistedChats = emptyList(), pendingChats = mapOf(agentChat.id to agentChat))
        )
    }

    @Test
    fun persistedChatTakesPrecedenceOverThePendingSnapshot() {
        val pending = Chat(id = "persisted-chat", title = "Old title", agentId = "writer")
        val persisted = pending.copy(title = "Renamed chat")

        assertEquals(
            persisted,
            chatSnapshotForId(persisted.id, persistedChats = listOf(persisted), pendingChats = mapOf(persisted.id to pending))
        )
    }

    @Test
    fun unknownOrMissingChatIdReturnsNull() {
        assertNull(chatSnapshotForId(null, persistedChats = emptyList(), pendingChats = emptyMap()))
        assertNull(chatSnapshotForId("missing", persistedChats = emptyList(), pendingChats = emptyMap()))
    }
}
