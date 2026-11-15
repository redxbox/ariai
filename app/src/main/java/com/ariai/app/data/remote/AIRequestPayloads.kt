package com.ariai.app.data.remote

import com.ariai.app.data.models.*
import com.ariai.app.data.serialization.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.*
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import com.ariai.app.util.tx
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * What the selected model accepts, read from the model itself (never from the provider name).
 * Video falls back to the Gemini default only for rows saved before modality metadata existed.
 */
internal data class MediaInputSupport(val video: Boolean, val audio: Boolean)

internal fun mediaInputFor(provider: Provider, modelId: String): MediaInputSupport {
    val model = provider.models.firstOrNull { it.id == modelId }
    return MediaInputSupport(
        video = model?.supportsVideoInput == true || (model == null && provider.type == ProviderType.GEMINI),
        audio = model?.supportsAudioInput == true
    )
}

/** OpenAI-style input_audio accepts only wav and mp3; other formats are sent as a note. */
internal fun openAiAudioFormat(mimeType: String): String? = when (mimeType.lowercase()) {
    "audio/wav", "audio/x-wav", "audio/wave" -> "wav"
    "audio/mpeg", "audio/mp3" -> "mp3"
    else -> null
}

/** Message text with extracted text-file contents appended. Media that is not sent gets a note. */
internal fun ChatMessage.promptText(sendVideo: Boolean = false, sendAudio: (Attachment) -> Boolean = { false }): String {
    val files = attachments.mapNotNull { a -> a.textContent?.let { "\n\n[File: ${a.name}]\n$it" } }
    val imageNotes = attachments
        .filter { it.type == AttachmentType.IMAGE && it.base64Data.isNullOrBlank() }
        .map { "\n\n[Image attached: ${it.name}, but its local file data is unavailable.]" }
    val videoNotes = attachments
        .filter { it.type == AttachmentType.VIDEO && (!sendVideo || it.base64Data.isNullOrBlank()) }
        .map { "\n\n[Video attached: ${it.name}. This model cannot watch videos.]" }
    val audioNotes = attachments
        .filter { it.type == AttachmentType.AUDIO && (it.base64Data.isNullOrBlank() || !sendAudio(it)) }
        .map { "\n\n[Audio attached: ${it.name}. This model cannot listen to audio.]" }
    return content + files.joinToString("") + imageNotes.joinToString("") + videoNotes.joinToString("") + audioNotes.joinToString("")
}

internal fun ChatMessage.videoParts(): List<Attachment> =
    attachments.filter { it.type == AttachmentType.VIDEO && !it.base64Data.isNullOrBlank() }

internal fun ChatMessage.audioParts(): List<Attachment> =
    attachments.filter { it.type == AttachmentType.AUDIO && !it.base64Data.isNullOrBlank() }

/** Images that carry base64 data and can be sent to vision models. */
internal fun ChatMessage.imageParts(): List<Attachment> =
    attachments.filter { it.type == AttachmentType.IMAGE && !it.base64Data.isNullOrBlank() }

internal fun <T> T.toProviderJsonObject(serializer: KSerializer<T>): JsonObject =
    appJson.encodeToJsonElement(serializer, this).jsonObject

internal fun <T> T.toProviderJsonElement(serializer: KSerializer<T>): JsonElement =
    appJson.encodeToJsonElement(serializer, this)

internal fun <T> providerRequestBody(value: T, serializer: KSerializer<T>): RequestBody =
    appJson.encodeToString(serializer, value).toRequestBody("application/json".toMediaType())

internal fun JsonObject.toGeminiSchema(): JsonObject = buildJsonObject {
    this@toGeminiSchema.forEach { (key, value) ->
        if (key != "additionalProperties") put(key, value.toGeminiSchemaValue(key))
    }
}

internal fun JsonElement.toGeminiSchemaValue(propertyName: String? = null): JsonElement = when (this) {
    is JsonObject -> toGeminiSchema()
    is JsonArray -> JsonArray(map { it.toGeminiSchemaValue() })
    is JsonPrimitive -> if (propertyName == "type" && isString) JsonPrimitive(content.uppercase()) else this
}

internal fun String.asJsonObject(): JsonObject = runCatching {
    appJson.parseToJsonElement(this).jsonObject
}.getOrElse {
    buildJsonObject {}
}
