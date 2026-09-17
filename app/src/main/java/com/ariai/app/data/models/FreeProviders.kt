package com.ariai.app.data.models

/**
 * Free Demo Providers - No API key required or free tier
 * These allow users to try AriAi without their own keys
 */

data class FreeProviderInfo(
    val id: String,
    val name: String,
    val description: String,
    val baseUrl: String,
    val type: ProviderType,
    val models: List<String>,
    val requiresKey: Boolean,
    val keyUrl: String?,
    val freeLimit: String,
    val icon: String,
    val gradient: List<String>, // Color hex
    val isRecommended: Boolean = false
)

fun getFreeProviders(): List<FreeProviderInfo> = listOf(
    FreeProviderInfo(
        id = "pollinations",
        name = "Pollinations",
        description = "Completely free, no API key needed. Unlimited text & image generation.",
        baseUrl = "https://text.pollinations.ai/openai",
        type = ProviderType.OPENAI_COMPATIBLE,
        models = listOf("openai", "openai-large", "qwen-coder", "mistral", "claude-hybridspace"),
        requiresKey = false,
        keyUrl = null,
        freeLimit = "Unlimited • No Key",
        icon = "🌸",
        gradient = listOf("#FF6B6B", "#FFE66D"),
        isRecommended = true
    ),
    FreeProviderInfo(
        id = "groq",
        name = "Groq",
        description = "Ultra-fast inference, 14,400 requests/day free. Llama 3.3 70B.",
        baseUrl = "https://api.groq.com/openai/v1",
        type = ProviderType.OPENAI_COMPATIBLE,
        models = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768"),
        requiresKey = true,
        keyUrl = "https://console.groq.com/keys",
        freeLimit = "14.4K req/day • Free",
        icon = "⚡",
        gradient = listOf("#FF8E53", "#FE6B8B"),
        isRecommended = true
    ),
    FreeProviderInfo(
        id = "gemini",
        name = "Google Gemini",
        description = "Best free tier, 1,500 requests/day. Gemini 2.0 Flash & Pro.",
        baseUrl = "https://generativelanguage.googleapis.com/v1beta",
        type = ProviderType.GEMINI,
        models = listOf("gemini-2.0-flash-exp", "gemini-1.5-flash", "gemini-1.5-pro"),
        requiresKey = true,
        keyUrl = "https://aistudio.google.com/app/apikey",
        freeLimit = "1,500 req/day • Free",
        icon = "✨",
        gradient = listOf("#4285F4", "#34A853"),
        isRecommended = true
    ),
    FreeProviderInfo(
        id = "openrouter",
        name = "OpenRouter",
        description = "200+ models, free variants. DeepSeek, Llama, Qwen free.",
        baseUrl = "https://openrouter.ai/api/v1",
        type = ProviderType.OPENAI_COMPATIBLE,
        models = listOf("meta-llama/llama-3.2-3b-instruct:free", "qwen/qwen-2-7b-instruct:free", "google/gemma-2-9b-it:free"),
        requiresKey = true,
        keyUrl = "https://openrouter.ai/keys",
        freeLimit = "50 req/day • Free",
        icon = "🌐",
        gradient = listOf("#6C4DFF", "#00D4AA"),
        isRecommended = false
    ),
    FreeProviderInfo(
        id = "cerebras",
        name = "Cerebras",
        description = "Fastest inference, 1M tokens/day free. Llama 3.3 70B at 2000 tok/s.",
        baseUrl = "https://api.cerebras.ai/v1",
        type = ProviderType.OPENAI_COMPATIBLE,
        models = listOf("llama3.1-8b", "llama3.3-70b"),
        requiresKey = true,
        keyUrl = "https://cloud.cerebras.ai/",
        freeLimit = "1M tokens/day • Free",
        icon = "🧠",
        gradient = listOf("#FF416C", "#FF4B2B"),
        isRecommended = false
    ),
    FreeProviderInfo(
        id = "llm7",
        name = "LLM7",
        description = "19 free models, no key required. OpenAI compatible.",
        baseUrl = "https://api.llm7.io/v1",
        type = ProviderType.OPENAI_COMPATIBLE,
        models = listOf("gpt-4o-mini", "llama-3.1-70b", "mistral-nemo"),
        requiresKey = false,
        keyUrl = null,
        freeLimit = "Free • No Key",
        icon = "🚀",
        gradient = listOf("#00C9FF", "#92FE9D"),
        isRecommended = false
    ),
    FreeProviderInfo(
        id = "github",
        name = "GitHub Models",
        description = "Free for GitHub users, 150 req/day. GPT-4o, Claude, Llama.",
        baseUrl = "https://models.inference.ai.azure.com",
        type = ProviderType.OPENAI_COMPATIBLE,
        models = listOf("gpt-4o-mini", "gpt-4o", "Meta-Llama-3.1-405B-Instruct"),
        requiresKey = true,
        keyUrl = "https://github.com/marketplace/models",
        freeLimit = "150 req/day • GitHub",
        icon = "🐙",
        gradient = listOf("#24292E", "#6E5494"),
        isRecommended = false
    )
)

fun FreeProviderInfo.toProvider(apiKey: String = ""): Provider {
    return Provider(
        name = name,
        type = type,
        baseUrl = baseUrl,
        apiKey = apiKey,
        models = models.map { modelId ->
            AIModel(
                id = modelId,
                displayName = modelId,
                providerId = id,
                supportsVision = modelId.contains("vision") || modelId.contains("4o") || modelId.contains("gemini"),
                supportsImageGen = modelId.contains("dall-e") || modelId.contains("image"),
                contextWindow = 128000
            )
        }
    )
}
