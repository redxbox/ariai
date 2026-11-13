package com.ariai.app.data.remote

import com.ariai.app.data.models.TokenUsage
import com.ariai.app.data.models.ToolCall
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.yield
import org.json.JSONObject

/** A model-visible function and the local action that actually performs it. */
data class ChatFunctionTool(
    val name: String,
    val description: String,
    val parameters: JSONObject,
    val progressLabel: String = name,
    /** Overrides the agent loop's default timeout for tools with different latency profiles. */
    val timeoutMs: Long? = null,
    val execute: suspend (JSONObject) -> String
)

sealed interface ChatGenerationState {
    data object Idle : ChatGenerationState
    data object Streaming : ChatGenerationState
    data class ToolCalling(val toolName: String) : ChatGenerationState
    data class ToolExecuting(val toolName: String) : ChatGenerationState
    data object ToolCompleted : ChatGenerationState
}

enum class ToolExecutionPhase {
    CALLING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED;

    val isActive: Boolean get() = this == CALLING || this == RUNNING
}

data class ToolExecutionUpdate(
    val callId: String,
    val label: String,
    val phase: ToolExecutionPhase,
    val elapsedMs: Long = 0L,
    val toolName: String = label
)

/** One provider response step. Tool calls are executed by [AgentLoop], never by the model itself. */
data class AgentStep(
    val text: String,
    val toolCalls: List<ToolCall>,
    val usage: TokenUsage? = null
)

/** A bounded, sequential tool loop: model -> tools -> model, returning only the final text. */
class AgentLoop(
    private val maxSteps: Int = 6,
    private val maxCallsPerStep: Int = 16,
    private val maxTotalToolCalls: Int = 24,
    private val maxCharsPerToolResult: Int = 8_000,
    private val maxTotalToolChars: Int = 20_000,
    private val toolTimeoutMs: Long = DEFAULT_TOOL_TIMEOUT_MS
) {
    init {
        require(maxSteps > 0)
        require(maxCallsPerStep > 0)
        require(maxTotalToolCalls > 0)
        require(maxCharsPerToolResult > 0)
        require(maxTotalToolChars >= 0)
        require(toolTimeoutMs > 0)
    }

    suspend fun run(
        initialMessages: List<com.ariai.app.data.models.ChatMessage>,
        tools: List<ChatFunctionTool>,
        onToolProgress: suspend (ToolExecutionUpdate) -> Unit = {},
        requestStep: suspend (List<com.ariai.app.data.models.ChatMessage>, List<ChatFunctionTool>, Boolean) -> AgentStep
    ): String = runInternal(
        initialMessages = initialMessages,
        tools = tools,
        onToolProgress = onToolProgress,
        onTextDelta = {},
        onDiscardText = {},
        onCommitText = {},
        requestStep = { messages, availableTools, recovery, _ ->
            requestStep(messages, availableTools, recovery)
        }
    )

    /** Runs the same bounded loop while exposing provisional model text as it arrives. */
    suspend fun runStreaming(
        initialMessages: List<com.ariai.app.data.models.ChatMessage>,
        tools: List<ChatFunctionTool>,
        onToolProgress: suspend (ToolExecutionUpdate) -> Unit = {},
        onTextDelta: suspend (String) -> Unit = {},
        onDiscardText: suspend () -> Unit = {},
        onCommitText: suspend () -> Unit = {},
        requestStep: suspend (
            List<com.ariai.app.data.models.ChatMessage>,
            List<ChatFunctionTool>,
            Boolean,
            suspend (String) -> Unit
        ) -> AgentStep
    ): String = runInternal(
        initialMessages = initialMessages,
        tools = tools,
        onToolProgress = onToolProgress,
        onTextDelta = onTextDelta,
        onDiscardText = onDiscardText,
        onCommitText = onCommitText,
        requestStep = requestStep
    )

    private suspend fun runInternal(
        initialMessages: List<com.ariai.app.data.models.ChatMessage>,
        tools: List<ChatFunctionTool>,
        onToolProgress: suspend (ToolExecutionUpdate) -> Unit,
        onTextDelta: suspend (String) -> Unit,
        onDiscardText: suspend () -> Unit,
        onCommitText: suspend () -> Unit,
        requestStep: suspend (
            List<com.ariai.app.data.models.ChatMessage>,
            List<ChatFunctionTool>,
            Boolean,
            suspend (String) -> Unit
        ) -> AgentStep
    ): String {
        require(tools.map { it.name }.distinct().size == tools.size) { "Tool names must be unique" }
        require(tools.all { it.name.matches(Regex("^[A-Za-z0-9_-]{1,64}$")) }) { "Tool names must be valid identifiers" }
        require(tools.all { it.timeoutMs == null || it.timeoutMs > 0L }) { "Tool timeouts must be positive" }
        val history = initialMessages.toMutableList()
        val originalUserText = history.lastOrNull { it.role == com.ariai.app.data.models.MessageRole.USER }?.content.orEmpty()
        val acknowledgementWasRequested = isAcknowledgementRequest(originalUserText)
        var acknowledgementRecoveryUsed = false
        var totalToolChars = 0
        var totalToolCalls = 0

        repeat(maxSteps) {
            val step = requestStep(history, tools, acknowledgementRecoveryUsed) { delta ->
                if (delta.isNotEmpty()) onTextDelta(delta)
            }
            if (step.toolCalls.isEmpty()) {
                if (acknowledgementWasRequested || !isAcknowledgementOnly(step.text)) {
                    onCommitText()
                    return step.text
                }
                onDiscardText()
                if (acknowledgementRecoveryUsed) return acknowledgementFallback(history.lastOrNull()?.content.orEmpty())
                acknowledgementRecoveryUsed = true
                // A short acknowledgement is not completion. The next request carries a
                // stronger system instruction while preserving the original conversation.
                return@repeat
            }

            // A model may stream a short preamble before announcing a tool call. Keep it
            // provisional and remove it from the reply bubble once the structured call arrives.
            onDiscardText()
            val lastUser = history.lastOrNull { it.role == com.ariai.app.data.models.MessageRole.USER }
                ?: error("Cannot execute tools without a user message")
            history += com.ariai.app.data.models.ChatMessage(
                chatId = lastUser.chatId,
                role = com.ariai.app.data.models.MessageRole.ASSISTANT,
                content = step.text,
                toolCalls = step.toolCalls
            )
            var callsThisStep = 0
            for (call in step.toolCalls) {
                val remainingChars = (maxTotalToolChars - totalToolChars).coerceAtLeast(0)
                val allowedChars = minOf(maxCharsPerToolResult, remainingChars)
                val tool = tools.firstOrNull { it.name == call.name }
                val withinStepBudget = callsThisStep++ < maxCallsPerStep
                val withinTurnBudget = totalToolCalls < maxTotalToolCalls
                val output = when {
                    !withinStepBudget -> "Tool '${call.name}' was not run because the per-step tool execution limit was reached."
                    !withinTurnBudget -> "Tool '${call.name}' was not run because the total tool execution limit was reached."
                    allowedChars == 0 -> "Tool '${call.name}' was not run because the response context budget was reached."
                    tool == null -> "Tool '${call.name}' is not available in this request."
                    else -> {
                        totalToolCalls++
                        val progressId = "${call.id.ifBlank { "tool" }}:$totalToolCalls"
                        val startNanos = System.nanoTime()
                        reportToolProgress(
                            onToolProgress,
                            ToolExecutionUpdate(
                                progressId,
                                tool.progressLabel,
                                ToolExecutionPhase.CALLING,
                                toolName = tool.name
                            )
                        )
                        yield()
                        reportToolProgress(
                            onToolProgress,
                            ToolExecutionUpdate(
                                progressId,
                                tool.progressLabel,
                                ToolExecutionPhase.RUNNING,
                                toolName = tool.name
                            )
                        )
                        val effectiveTimeoutMs = tool.timeoutMs ?: toolTimeoutMs
                        var phase = ToolExecutionPhase.COMPLETED
                        val result = try {
                            require(call.arguments.length <= MAX_TOOL_ARGUMENT_CHARS) { "Tool arguments are too large" }
                            val args = call.arguments.takeIf { it.isNotBlank() }?.let { JSONObject(it) } ?: JSONObject()
                            val timedResult = kotlinx.coroutines.withTimeoutOrNull(effectiveTimeoutMs) {
                                tool.execute(args)
                            }
                            if (timedResult == null) {
                                phase = ToolExecutionPhase.FAILED
                                val timeout = if (effectiveTimeoutMs % 1_000L == 0L) {
                                    "${effectiveTimeoutMs / 1_000L} seconds"
                                } else {
                                    "$effectiveTimeoutMs milliseconds"
                                }
                                "Tool '${call.name}' timed out after $timeout."
                            } else {
                                timedResult
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            phase = ToolExecutionPhase.FAILED
                            "Tool '${call.name}' failed: ${error.message?.take(500) ?: error.javaClass.simpleName}"
                        }
                        val elapsedMs = ((System.nanoTime() - startNanos) / 1_000_000L).coerceAtLeast(0L)
                        reportToolProgress(
                            onToolProgress,
                            ToolExecutionUpdate(
                                progressId,
                                tool.progressLabel,
                                phase,
                                elapsedMs,
                                toolName = tool.name
                            )
                        )
                        result
                    }
                }
                val truncationNotice = "\n[Tool output truncated to fit the response context budget.]"
                val boundedOutput = when {
                    allowedChars == 0 -> output
                    output.length <= allowedChars -> output
                    else -> output.take((allowedChars - truncationNotice.length).coerceAtLeast(0)) + truncationNotice.take(allowedChars)
                }
                if (allowedChars > 0) totalToolChars += boundedOutput.length
                history += com.ariai.app.data.models.ChatMessage(
                    chatId = lastUser.chatId,
                    role = com.ariai.app.data.models.MessageRole.TOOL,
                    content = boundedOutput,
                    toolCallId = call.id,
                    toolName = call.name,
                    parentId = lastUser.id
                )
            }
        }

        // A final synthesis step is still allowed after the tool-step limit, but no more tools
        // are exposed, preventing runaway tool loops.
        val final = requestStep(history, emptyList(), true) { delta ->
            if (delta.isNotEmpty()) onTextDelta(delta)
        }
        if (final.text.isNotBlank() && !isAcknowledgementOnly(final.text)) {
            onCommitText()
            return final.text
        }
        onDiscardText()
        return acknowledgementFallback(history.lastOrNull { it.role == com.ariai.app.data.models.MessageRole.USER }?.content.orEmpty())
    }

    private suspend fun reportToolProgress(
        callback: suspend (ToolExecutionUpdate) -> Unit,
        update: ToolExecutionUpdate
    ) {
        try {
            callback(update)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            com.ariai.app.util.AppLog.warn(e, "AgentTool.kt")
            // Observability must never break an otherwise valid tool call.
        }
    }

    private companion object {
        const val MAX_TOOL_ARGUMENT_CHARS = 4_000
        const val DEFAULT_TOOL_TIMEOUT_MS = 60_000L
    }

    private fun isAcknowledgementOnly(text: String): Boolean {
        val normalized = text.trim().lowercase()
            .replace('’', '\'')
            .replace(Regex("[.!?؟،,؛:…]+$"), "")
            .replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) return true
        val acknowledgements = setOf(
            "ok", "okay", "sure", "alright", "all right", "got it", "understood", "certainly", "of course", "will do", "i am on it", "on it",
            "باشه", "حتما", "حتماً", "چشم", "اوکی", "متوجه شدم"
        )
        if (normalized in acknowledgements) return true
        val englishProgress = Regex(
            "^(?:(?:ok|okay|sure|got it)[, ]+)?(?:i(?:'m| am) (?:currently )?(?:checking|reviewing|inspecting|examining|looking|searching|analyzing|working|downloading|fetching|cloning|reading|opening)|i(?:'ll| will) (?:check|review|inspect|examine|look|search|analyze|download|fetch|clone|read|open|take a look)|let me (?:check|review|inspect|examine|look|search|download|fetch|clone|read|open))(?:\\s+.+)?$"
        )
        val persianProgress = Regex(
            "^(?:(?:باشه|حتما|حتماً|چشم)[،, ]*)?(?:(?:الان|در حال حاضر|در حال)\\s*)?(?:دارم\\s*)?(?:بررسی|بازبینی|چک|نگاه|جستجو|تحلیل|واکاوی|خواندن|بازخوانی)(?:\\s+.+)?(?:\\s*(?:می کنم|میکنم|می‌کنم|کنم))?$"
        )
        return englishProgress.matches(normalized) || persianProgress.matches(normalized)
    }

    private fun isAcknowledgementRequest(text: String): Boolean {
        val normalized = text.trim().lowercase().replace('’', '\'').replace(Regex("\\s+"), " ")
        return normalized.matches(Regex("^(?:just say|only say|reply only with|respond only with) (?:ok|okay|sure)[.! ]*$")) ||
            normalized.matches(Regex("^(?:فقط بگو|فقط جواب بده) (?:باشه|حتما|حتماً|اوکی)[.! ]*$"))
    }

    private fun acknowledgementFallback(userText: String): String =
        if (userText.any { it in '\u0600'..'\u06FF' }) {
            "پاسخ مدل فقط تأیید بود و بررسی یا پاسخ نهایی تولید نشد. لطفاً درخواست را دقیق‌تر کنید یا فایل، نشانی، یا اطلاعات لازم را بفرستید."
        } else {
            "The model only acknowledged the request and did not provide a result. Please clarify the task or provide the file, URL, or information needed."
        }
}
