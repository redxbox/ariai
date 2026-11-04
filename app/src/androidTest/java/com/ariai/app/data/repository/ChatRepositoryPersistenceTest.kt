package com.ariai.app.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ariai.app.data.local.AppDatabase
import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import com.ariai.app.data.remote.AIClient
import com.ariai.app.data.remote.SearchClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatRepositoryPersistenceTest {
    @Test
    fun firstMessageRecreatesMissingChatRowAtomically() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repository = ChatRepository(database, AIClient(), SearchClient())
            val message = ChatMessage(
                id = "first-message",
                chatId = "missing-chat",
                role = MessageRole.USER,
                content = "Hello",
                modelId = "test-model",
                providerId = "test-provider"
            )

            assertTrue(repository.saveMessage(message))

            val recoveredChat = repository.getChatById(message.chatId)
            assertNotNull(recoveredChat)
            assertEquals("New Chat", recoveredChat?.title)
            assertEquals(message.providerId, recoveredChat?.providerId)
            assertEquals(message.modelId, recoveredChat?.modelId)
            assertEquals(message.id, repository.getMessagesSync(message.chatId).single().id)
        } finally {
            database.close()
        }
    }
}
