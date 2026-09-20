package com.ariai.app.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class MessageRole {
    USER, ASSISTANT, SYSTEM, TOOL
}

enum class MessageStatus {
    SENDING, SENT, ERROR, STREAMING
}

@Parcelize
data class Attachment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: AttachmentType,
    val name: String,
    val uri: String? = null,
    val base64Data: String? = null,
    val mimeType: String
) : Parcelable

enum class AttachmentType {
    IMAGE, PDF, DOCX, TEXT, AUDIO
}

@Parcelize
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val chatId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val modelId: String? = null,
    val providerId: String? = null,
    val attachments: List<Attachment> = emptyList(),
    val parentId: String? = null, // For branching
    val branchChildren: List<String> = emptyList(),
    val toolCalls: List<ToolCall> = emptyList(),
    val reasoning: String? = null, // For o1/thinking models
    val usage: TokenUsage? = null
) : Parcelable

@Parcelize
data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String,
    val result: String? = null
) : Parcelable

@Parcelize
data class TokenUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0
) : Parcelable

@Parcelize
data class Chat(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val providerId: String? = null,
    val modelId: String? = null,
    val agentId: String? = null,
    val systemPrompt: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val memoryEnabled: Boolean = false
) : Parcelable

@Parcelize
data class Agent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val systemPrompt: String,
    val avatar: String = "🤖",
    val modelId: String? = null,
    val providerId: String? = null,
    val temperature: Float = 0.7f,
    val topP: Float = 1.0f,
    val tools: List<AgentTool> = emptyList(),
    val promptVariables: Map<String, String> = emptyMap(),
    val examples: List<String> = emptyList(),
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

enum class AgentTool {
    WEB_SEARCH,
    IMAGE_GENERATION,
    CODE_EXECUTION,
    MEMORY,
    MCP
}

fun getBuiltInAgents(): List<Agent> = listOf(
    Agent(
        id = "general",
        name = "General Assistant",
        description = "Helpful general purpose assistant",
        systemPrompt = "You are AriAi, a helpful AI assistant. You are friendly, knowledgeable and concise. Current time: {time}, Model: {model}",
        avatar = "🤖",
        isBuiltIn = true
    ),
    Agent(
        id = "coder",
        name = "Code Expert",
        description = "Expert programmer for all languages",
        systemPrompt = "You are an expert programmer. You write clean, efficient, well-documented code. You explain complex concepts clearly. Always provide code examples when relevant. Current time: {time}",
        avatar = "💻",
        tools = listOf(AgentTool.CODE_EXECUTION, AgentTool.WEB_SEARCH),
        isBuiltIn = true
    ),
    Agent(
        id = "writer",
        name = "Creative Writer",
        description = "Creative writing and storytelling",
        systemPrompt = "You are a creative writer with a vivid imagination. You craft engaging stories, poems, and creative content. Your writing is evocative and original.",
        avatar = "✍️",
        isBuiltIn = true
    ),
    Agent(
        id = "translator",
        name = "Translator Pro",
        description = "Professional translator for 50+ languages",
        systemPrompt = "You are a professional translator. You translate accurately while preserving tone, context and cultural nuances. Always show original and translated text.",
        avatar = "🌐",
        tools = listOf(AgentTool.WEB_SEARCH),
        isBuiltIn = true
    ),
    Agent(
        id = "researcher",
        name = "Research Assistant",
        description = "Deep research with web search",
        systemPrompt = "You are a research assistant. You provide thorough, well-sourced information. Use web search when needed. Always cite sources. Be objective and comprehensive. Time: {time}",
        avatar = "🔍",
        tools = listOf(AgentTool.WEB_SEARCH, AgentTool.MEMORY),
        isBuiltIn = true
    ),
    Agent(
        id = "artist",
        name = "Image Creator",
        description = "AI image generation specialist",
        systemPrompt = "You are an image generation expert. You create detailed, creative prompts for image generation. You understand composition, lighting, styles and artistic techniques.",
        avatar = "🎨",
        tools = listOf(AgentTool.IMAGE_GENERATION),
        isBuiltIn = true
    )
)
