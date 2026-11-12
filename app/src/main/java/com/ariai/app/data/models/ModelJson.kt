package com.ariai.app.data.models

import com.ariai.app.data.serialization.appJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/** Version-tolerant on-disk model record. Defaults keep old and partially upgraded rows readable. */
@Serializable
private data class StoredModel(
    val id: String = "",
    val displayName: String = "",
    val providerId: String = "",
    val supportsVision: Boolean = false,
    val supportsFunctionCalling: Boolean = true,
    val supportsImageGen: Boolean = false,
    val contextWindow: Int = 8192,
    val isCustom: Boolean = false,
    val builtInTools: List<String> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
    val customBody: String? = null,
    val supportsVideoGen: Boolean = false,
    val outputsText: Boolean = true,
    val supportsAudioGen: Boolean = false
) {
    fun toModel(): AIModel? {
        val modelId = id.trim().takeIf(String::isNotBlank) ?: return null
        return AIModel(
            id = modelId,
            displayName = displayName.ifBlank { modelId },
            providerId = providerId,
            supportsVision = supportsVision,
            supportsFunctionCalling = supportsFunctionCalling,
            supportsImageGen = supportsImageGen,
            contextWindow = contextWindow.coerceIn(1, 10_000_000),
            isCustom = isCustom,
            builtInTools = builtInTools.take(MAX_MODEL_TOOLS).map { it.take(MAX_MODEL_TOOL_LENGTH) },
            headers = headers.entries.take(MAX_MODEL_HEADERS)
                .associate { (key, value) -> key.take(MAX_MODEL_HEADER_NAME_LENGTH) to value.take(MAX_MODEL_HEADER_VALUE_LENGTH) },
            customBody = customBody?.take(MAX_MODEL_CUSTOM_BODY_LENGTH),
            supportsVideoGen = supportsVideoGen,
            outputsText = outputsText,
            supportsAudioGen = supportsAudioGen
        )
    }

    companion object {
        fun fromModel(model: AIModel) = StoredModel(
            id = model.id.take(MAX_MODEL_ID_LENGTH),
            displayName = model.displayName.take(MAX_MODEL_DISPLAY_NAME_LENGTH),
            providerId = model.providerId.take(MAX_MODEL_ID_LENGTH),
            supportsVision = model.supportsVision,
            supportsFunctionCalling = model.supportsFunctionCalling,
            supportsImageGen = model.supportsImageGen,
            contextWindow = model.contextWindow.coerceIn(1, 10_000_000),
            isCustom = model.isCustom,
            builtInTools = model.builtInTools.take(MAX_MODEL_TOOLS).map { it.take(MAX_MODEL_TOOL_LENGTH) },
            headers = model.headers.entries.take(MAX_MODEL_HEADERS)
                .associate { (key, value) -> key.take(MAX_MODEL_HEADER_NAME_LENGTH) to value.take(MAX_MODEL_HEADER_VALUE_LENGTH) },
            customBody = model.customBody?.take(MAX_MODEL_CUSTOM_BODY_LENGTH),
            supportsVideoGen = model.supportsVideoGen,
            outputsText = model.outputsText,
            supportsAudioGen = model.supportsAudioGen
        )
    }
}

/** Explicit transfer DTO. Credentials are included only for user-initiated export/QR transfer. */
@Serializable
private data class ProviderImportPayload(
    val name: String = "",
    val type: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
    val customHeaders: Map<String, String> = emptyMap(),
    val customBody: String? = null
)

/** Stores a provider's models in the database as a forward-compatible JSON column. */
fun modelsToJson(models: List<AIModel>): String {
    val bounded = models.take(MAX_STORED_MODELS).map(StoredModel::fromModel)
    val full = appJson.encodeToString(bounded)
    if (full.length <= MAX_MODELS_JSON_CHARS) return full

    // Find the largest complete prefix that fits the read-side limit without repeatedly
    // encoding successively smaller lists (model catalogs can contain hundreds of entries).
    var low = 0
    var high = bounded.size
    var best = "[]"
    while (low <= high) {
        val middle = (low + high) ushr 1
        val candidate = appJson.encodeToString(bounded.take(middle))
        if (candidate.length <= MAX_MODELS_JSON_CHARS) {
            best = candidate
            low = middle + 1
        } else {
            high = middle - 1
        }
    }
    return best
}

fun modelsFromJson(raw: String): List<AIModel> {
    if (raw.length > MAX_MODELS_JSON_CHARS) return emptyList()
    return try {
        appJson.decodeFromString<List<StoredModel>>(raw)
            .take(MAX_STORED_MODELS)
            .mapNotNull(StoredModel::toModel)
    } catch (e: Exception) {
        com.ariai.app.util.AppLog.warn(e, "models fromJson")
        emptyList()
    }
}

/** Serializes a provider for explicit user-initiated transfer (such as a QR code). */
fun providerToJson(provider: Provider): String = appJson.encodeToString(
    ProviderImportPayload(
        name = provider.name.take(MAX_PROVIDER_NAME_LENGTH),
        type = provider.type.name,
        baseUrl = provider.baseUrl.take(MAX_PROVIDER_URL_LENGTH),
        apiKey = provider.apiKey.take(MAX_PROVIDER_SECRET_LENGTH),
        customHeaders = provider.customHeaders.entries.take(MAX_PROVIDER_HEADERS)
            .associate { (key, value) -> key.take(MAX_PROVIDER_HEADER_NAME_LENGTH) to value.take(MAX_PROVIDER_HEADER_VALUE_LENGTH) }
    )
)

/** Reads a user-imported provider with resilient, typed parsing and bounded values. */
fun providerFromJson(text: String): Provider? {
    if (text.length > MAX_PROVIDER_IMPORT_JSON_CHARS) return null
    return try {
        val payload = appJson.decodeFromString<ProviderImportPayload>(text.trim())
        val name = payload.name.trim().take(MAX_PROVIDER_NAME_LENGTH)
        val baseUrl = payload.baseUrl.trim().take(MAX_PROVIDER_URL_LENGTH)
        if (name.isBlank() || baseUrl.isBlank()) return null
        Provider(
            name = name,
            type = runCatching { ProviderType.valueOf(payload.type.trim().uppercase()) }
                .getOrDefault(ProviderType.OPENAI_COMPATIBLE),
            baseUrl = baseUrl,
            apiKey = payload.apiKey.trim().take(MAX_PROVIDER_SECRET_LENGTH),
            customHeaders = payload.customHeaders.entries.take(MAX_PROVIDER_HEADERS)
                .associate { (key, value) -> key.take(MAX_PROVIDER_HEADER_NAME_LENGTH) to value.take(MAX_PROVIDER_HEADER_VALUE_LENGTH) },
            customBody = payload.customBody?.take(MAX_PROVIDER_CUSTOM_BODY_LENGTH)
        )
    } catch (_: Exception) {
        null
    }
}

private const val MAX_MODELS_JSON_CHARS = 1_000_000
private const val MAX_STORED_MODELS = 512
private const val MAX_MODEL_ID_LENGTH = 256
private const val MAX_MODEL_DISPLAY_NAME_LENGTH = 512
private const val MAX_MODEL_TOOLS = 64
private const val MAX_MODEL_TOOL_LENGTH = 128
private const val MAX_MODEL_HEADERS = 32
private const val MAX_MODEL_HEADER_NAME_LENGTH = 128
private const val MAX_MODEL_HEADER_VALUE_LENGTH = 2_048
private const val MAX_MODEL_CUSTOM_BODY_LENGTH = 16_000
private const val MAX_PROVIDER_IMPORT_JSON_CHARS = 256_000
private const val MAX_PROVIDER_NAME_LENGTH = 128
private const val MAX_PROVIDER_URL_LENGTH = 2_048
private const val MAX_PROVIDER_SECRET_LENGTH = 8_192
private const val MAX_PROVIDER_HEADERS = 64
private const val MAX_PROVIDER_HEADER_NAME_LENGTH = 128
private const val MAX_PROVIDER_HEADER_VALUE_LENGTH = 4_096
private const val MAX_PROVIDER_CUSTOM_BODY_LENGTH = 64_000
