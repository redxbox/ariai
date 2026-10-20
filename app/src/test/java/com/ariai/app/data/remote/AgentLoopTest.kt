package com.ariai.app.data.remote

import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import com.ariai.app.data.models.ToolCall
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentLoopTest {
    @Test
    fun `executes tool then returns only final model answer`() = runBlocking {
        var requestCount = 0
        var calculatorInput = ""
        val tool = ChatFunctionTool(
            name = "calculate",
            description = "Calculate arithmetic",
            parameters = JSONObject().put("type", "object")
        ) { args ->
            calculatorInput = args.getString("expression")
            "4"
        }

        val answer = AgentLoop().run(
            initialMessages = listOf(ChatMessage(chatId = "chat", role = MessageRole.USER, content = "What is 2+2?")),
            tools = listOf(tool)
        ) { history, _, _ ->
            requestCount++
            if (requestCount == 1) {
                AgentStep("", listOf(ToolCall("call-1", "calculate", "{\"expression\":\"2+2\"}")))
            } else {
                assertTrue(history.any { it.role == MessageRole.TOOL && it.content == "4" })
                AgentStep("The result is 4.", emptyList())
            }
        }

        assertEquals("2+2", calculatorInput)
        assertEquals(2, requestCount)
        assertEquals("The result is 4.", answer)
    }

    @Test
    fun `synthesizes a final response after the tool step limit without more tools`() = runBlocking {
        var requestCount = 0
        val tool = ChatFunctionTool("sample", "test", JSONObject()) { "result" }
        val answer = AgentLoop(maxSteps = 1).run(
            initialMessages = listOf(ChatMessage(chatId = "chat", role = MessageRole.USER, content = "Use the tool")),
            tools = listOf(tool)
        ) { history, availableTools, recovery ->
            requestCount++
            if (requestCount == 1) {
                AgentStep("", listOf(ToolCall("call-1", "sample", "{}")))
            } else {
                assertTrue(availableTools.isEmpty())
                assertTrue(recovery)
                assertTrue(history.any { it.role == MessageRole.TOOL && it.content == "result" })
                AgentStep("The tool returned result.", emptyList())
            }
        }

        assertEquals(2, requestCount)
        assertEquals("The tool returned result.", answer)
    }

    @Test
    fun `retries an acknowledgement in the same turn`() = runBlocking {
        var recoveryWasRequested = false
        var requestCount = 0
        val answer = AgentLoop().run(
            initialMessages = listOf(ChatMessage(chatId = "chat", role = MessageRole.USER, content = "Review this")),
            tools = emptyList()
        ) { _, _, recovery ->
            requestCount++
            if (recovery) recoveryWasRequested = true
            if (requestCount == 1) AgentStep("باشه", emptyList())
            else AgentStep("برای بررسی، لطفاً فایل را بفرست.", emptyList())
        }

        assertTrue(recoveryWasRequested)
        assertEquals(2, requestCount)
        assertEquals("برای بررسی، لطفاً فایل را بفرست.", answer)
    }

    @Test
    fun `retries a promise to inspect instead of treating it as an answer`() = runBlocking {
        var requestCount = 0
        val answer = AgentLoop().run(
            initialMessages = listOf(ChatMessage(chatId = "chat", role = MessageRole.USER, content = "Review this repository")),
            tools = emptyList()
        ) { _, _, _ ->
            requestCount++
            if (requestCount == 1) AgentStep("I'll clone the repository and inspect the files.", emptyList())
            else AgentStep("Please provide a repository URL or attach the source files.", emptyList())
        }

        assertEquals(2, requestCount)
        assertEquals("Please provide a repository URL or attach the source files.", answer)
    }

    @Test
    fun `returns explicit fallback instead of repeated acknowledgement`() = runBlocking {
        val answer = AgentLoop().run(
            initialMessages = listOf(ChatMessage(chatId = "chat", role = MessageRole.USER, content = "بررسی کن")),
            tools = emptyList()
        ) { _, _, _ -> AgentStep("حتماً", emptyList()) }

        assertTrue(answer.contains("بررسی یا پاسخ نهایی تولید نشد"))
    }
}
