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

/** Message text with extracted text-file contents appended. */
internal fun ChatMessage.promptText(gemini: Boolean = false): String {
    val files = attachments.mapNotNull { a -> a.textContent?.let { "\n\n[File: ${a.name}]\n$it" } }
    // Only Gemini receives video bytes here; other models get a note so they do not guess.
    val imageNotes = attachments
        .filter { it.type == AttachmentType.IMAGE && it.base64Data.isNullOrBlank() }
        .map { "\n\n[Image attached: ${it.name}, but its local file data is unavailable.]" }
    val videoNotes = attachments
        .filter { it.type == AttachmentType.VIDEO && (!gemini || it.base64Data.isNullOrBlank()) }
        .map { "\n\n[Video attached: ${it.name}. This model cannot watch videos.]" }
    return content + files.joinToString("") + imageNotes.joinToString("") + videoNotes.joinToString("")
}

internal fun ChatMessage.videoParts(): List<Attachment> =
    attachments.filter { it.type == AttachmentType.VIDEO && !it.base64Data.isNullOrBlank() }

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
