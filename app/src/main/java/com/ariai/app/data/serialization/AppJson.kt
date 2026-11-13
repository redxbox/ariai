package com.ariai.app.data.serialization

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject

/** Shared tolerant parser for provider and MCP payloads that may gain fields over time. */
val appJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
    isLenient = true
    explicitNulls = false
}

/** Bridge only at legacy dynamic-schema boundaries; protocol envelopes use serializable DTOs. */
internal fun JSONObject.toKotlinJsonObject(): JsonObject = appJson.parseToJsonElement(toString()).jsonObject

internal fun JsonObject.toAndroidJsonObject(): JSONObject =
    JSONObject(appJson.encodeToString(JsonElement.serializer(), this))
