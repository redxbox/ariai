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
    fun `handles ten tool calls requested in one step`() = runBlocking {
        var requestCount = 0
        var executed = 0
        val tool = ChatFunctionTool("read_file", "Read a test file", JSONObject()) {
            executed++
            "file contents"
        }
        val calls = (1..10).map { index -> ToolCall("call-$index", "read_file", "{}") }

        val answer = AgentLoop().run(
            initialMessages = listOf(ChatMessage(chatId = "chat", role = MessageRole.USER, content = "Review this repository")),
            tools = listOf(tool)
        ) { history, _, _ ->
            requestCount++
            if (requestCount == 1) AgentStep("", calls)
            else {
                assertEquals(10, history.count { it.role == MessageRole.TOOL })
                AgentStep("I reviewed the ten files.", emptyList())
            }
        }

        assertEquals(10, executed)
        assertEquals(2, requestCount)
        assertEquals("I reviewed the ten files.", answer)
    }

    @Test
    fun `returns tool results for calls skipped by the per-step budget`() = runBlocking {
        var requestCount = 0
        var executed = 0
        val tool = ChatFunctionTool("sample", "test", JSONObject()) {
            executed++
            "result"
        }
        val calls = (1..3).map { index -> ToolCall("call-$index", "sample", "{}") }

        val answer = AgentLoop(maxCallsPerStep = 2).run(
            initialMessages = listOf(ChatMessage(chatId = "chat", role = MessageRole.USER, content = "Use the tools")),
            tools = listOf(tool)
        ) { history, _, _ ->
            requestCount++
            if (requestCount == 1) AgentStep("", calls)
            else {
                val toolResults = history.filter { it.role == MessageRole.TOOL }
                assertEquals(3, toolResults.size)
                assertTrue(toolResults.last().content.contains("per-step tool execution limit"))
                AgentStep("I can answer with the available results.", emptyList())
            }
        }

        assertEquals(2, executed)
        assertEquals("I can answer with the available results.", answer)
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
