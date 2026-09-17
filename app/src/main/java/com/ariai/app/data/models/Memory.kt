package com.ariai.app.data.models

/**
 * Memory system like ChatGPT
 * Stores user preferences, facts, and conversation summaries
 */
data class Memory(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val type: MemoryType = MemoryType.FACT,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
    val importance: Int = 5, // 1-10
    val tags: List<String> = emptyList()
)

enum class MemoryType {
    FACT,        // User fact: "User likes coffee"
    PREFERENCE,  // Preference: "User prefers concise answers"
    CONTEXT,     // Context: "User is working on Android app"
    SUMMARY      // Conversation summary
}

class MemoryManager {
    private val memories = mutableListOf<Memory>()

    fun addMemory(content: String, type: MemoryType = MemoryType.FACT, tags: List<String> = emptyList()) {
        memories.add(Memory(content = content, type = type, tags = tags))
    }

    fun getRelevantMemories(query: String, limit: Int = 5): List<Memory> {
        // Simple relevance - in real implementation would use embeddings
        return memories
            .sortedByDescending { it.importance }
            .take(limit)
    }

    fun formatMemoriesForPrompt(query: String): String {
        val relevant = getRelevantMemories(query)
        if (relevant.isEmpty()) return ""
        return buildString {
            appendLine("Relevant memories about user:")
            relevant.forEach { mem ->
                appendLine("- ${mem.content} [${mem.type.name}]")
            }
        }
    }

    fun getAllMemories(): List<Memory> = memories.toList()

    fun deleteMemory(id: String) {
        memories.removeIf { it.id == id }
    }

    fun clearAll() {
        memories.clear()
    }
}
