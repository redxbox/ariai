package com.ariai.app.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class ProviderType {
    OPENAI,
    GEMINI,
    ANTHROPIC,
    OPENAI_COMPATIBLE,
    OLLAMA,
    CUSTOM
}

@Parcelize
data class Provider(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val type: ProviderType = ProviderType.OPENAI_COMPATIBLE,
    val baseUrl: String,
    val apiKey: String,
    val models: List<AIModel> = emptyList(),
    val enabled: Boolean = true,
    val customHeaders: Map<String, String> = emptyMap(),
    val customBody: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class AIModel(
    val id: String,
    val displayName: String,
    val providerId: String,
    val supportsVision: Boolean = false,
    val supportsFunctionCalling: Boolean = true,
    val supportsImageGen: Boolean = false,
    val contextWindow: Int = 8192,
    val isCustom: Boolean = false
) : Parcelable

fun getDefaultModelsForType(type: ProviderType, providerId: String): List<AIModel> {
    return when (type) {
        ProviderType.OPENAI, ProviderType.OPENAI_COMPATIBLE -> listOf(
            AIModel("gpt-4o", "GPT-4o", providerId, true, true, false, 128000),
            AIModel("gpt-4o-mini", "GPT-4o Mini", providerId, true, true, false, 128000),
            AIModel("gpt-4-turbo", "GPT-4 Turbo", providerId, true, true, false, 128000),
            AIModel("o1-preview", "o1 Preview", providerId, false, false, false, 128000),
            AIModel("o1-mini", "o1 Mini", providerId, false, false, false, 128000),
            AIModel("dall-e-3", "DALL·E 3", providerId, false, false, true, 4000),
            AIModel("gpt-image-1", "GPT Image 1", providerId, false, false, true, 4000)
        )
        ProviderType.GEMINI -> listOf(
            AIModel("gemini-2.0-flash-exp", "Gemini 2.0 Flash", providerId, true, true, false, 1000000),
            AIModel("gemini-1.5-pro", "Gemini 1.5 Pro", providerId, true, true, false, 2000000),
            AIModel("gemini-1.5-flash", "Gemini 1.5 Flash", providerId, true, true, false, 1000000),
            AIModel("gemini-1.5-flash-8b", "Gemini 1.5 Flash 8B", providerId, true, true, false, 1000000),
            AIModel("imagen-3.0-generate-001", "Imagen 3", providerId, false, false, true, 8192)
        )
        ProviderType.ANTHROPIC -> listOf(
            AIModel("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", providerId, true, true, false, 200000),
            AIModel("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", providerId, true, true, false, 200000),
            AIModel("claude-3-opus-20240229", "Claude 3 Opus", providerId, true, true, false, 200000)
        )
        ProviderType.OLLAMA -> listOf(
            AIModel("llama3.2", "Llama 3.2", providerId, false, true, false, 128000),
            AIModel("qwen2.5", "Qwen 2.5", providerId, true, true, false, 128000),
            AIModel("mistral", "Mistral", providerId, false, true, false, 32000),
            AIModel("llava", "LLaVA Vision", providerId, true, false, false, 32000)
        )
        else -> emptyList()
    }
}
