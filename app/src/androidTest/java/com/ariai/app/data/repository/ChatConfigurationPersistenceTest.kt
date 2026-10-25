package com.ariai.app.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ariai.app.data.local.AppDatabase
import com.ariai.app.data.models.Agent
import com.ariai.app.data.models.AgentTool
import com.ariai.app.data.models.Chat
import com.ariai.app.data.remote.AIClient
import com.ariai.app.data.remote.SearchClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatConfigurationPersistenceTest {
    @Test
    fun savesConversationControlsFoldersAndAgentStudioConfiguration() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repository = ChatRepository(database, AIClient(), SearchClient())
            val chat = Chat(
                id = "configured-chat",
                title = "Compose workspace",
                systemPrompt = "Answer with a short plan.",
                temperature = 1.25f,
                topP = 0.35f,
                folder = "Code Snippets"
            )
            assertTrue(repository.saveChat(chat))
            val restoredChat = repository.getChatById(chat.id)
            assertNotNull(restoredChat)
            assertEquals(chat.systemPrompt, restoredChat?.systemPrompt)
            assertEquals(chat.temperature, restoredChat?.temperature ?: -1f, 0.001f)
            assertEquals(chat.topP, restoredChat?.topP ?: -1f, 0.001f)
            assertEquals(chat.folder, restoredChat?.folder)

            val agent = Agent(
                id = "custom-agent",
                name = "Compose reviewer",
                description = "Reviews mobile UI code",
                systemPrompt = "Review {{model_name}} at {{time}}.",
                providerId = "provider-id",
                modelId = "model-id",
                temperature = 0.4f,
                topP = 0.8f,
                tools = listOf(AgentTool.CALCULATOR, AgentTool.WEB_SEARCH),
                promptVariables = mapOf("project" to "AriAI"),
                examples = listOf("Review this screen", "Suggest accessibility fixes")
            )
            repository.saveAgent(agent)
            val restoredAgent = repository.getAgents().first().single { it.id == agent.id }
            assertEquals(agent.tools, restoredAgent.tools)
            assertEquals(agent.promptVariables, restoredAgent.promptVariables)
            assertEquals(agent.examples, restoredAgent.examples)
            assertEquals(agent.temperature, restoredAgent.temperature, 0.001f)
            assertEquals(agent.topP, restoredAgent.topP, 0.001f)
            assertEquals(agent.modelId, restoredAgent.modelId)
            assertEquals(agent.providerId, restoredAgent.providerId)
        } finally {
            database.close()
        }
    }
}
