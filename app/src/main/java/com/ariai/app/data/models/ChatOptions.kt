package com.ariai.app.data.models

/** How hard the model should think. Each provider maps this to its own parameter. */
enum class ReasoningLevel(
    val label: String,
    val hint: String,
    /** OpenAI-compatible `reasoning_effort`, null = not sent. */
    val effort: String?,
    /** Anthropic `thinking.budget_tokens`, null = not sent. */
    val budgetTokens: Int?,
    /** Gemini `thinkingBudget`, null = not sent. */
    val geminiBudget: Int?
) {
    OFF("Off", "Skip extended thinking. Fastest, least depth.", null, null, 0),
    AUTO("Auto", "Let the provider decide how much to think.", null, null, null),
    LOW("Low", "A small thinking budget for quick reasoning.", "low", 1024, 1024),
    MEDIUM("Medium", "A balance between speed and depth.", "medium", 4096, 8192),
    HIGH("High", "Deepest reasoning. Slower and uses more tokens.", "high", 16000, 24576);

    companion object {
        fun fromName(name: String?): ReasoningLevel = values().firstOrNull { it.name == name } ?: AUTO
    }
}

/** Where web search results come from for the next message. */
enum class SearchMode {
    OFF,
    /** Built into the provider (Gemini Google Search grounding). */
    MODEL,
    /** Fetched by the app from a configured search service. */
    LOCAL;

    companion object {
        fun fromName(name: String?): SearchMode = values().firstOrNull { it.name == name } ?: OFF
    }
}

/** Stable key for a model inside favorites storage. */
fun favoriteKey(providerId: String, modelId: String) = "$providerId|$modelId"
