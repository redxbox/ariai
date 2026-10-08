package com.ariai.app.data.models

import org.json.JSONArray
import org.json.JSONObject

/** Stores a provider's models in the database as one JSON column. */
fun modelsToJson(models: List<AIModel>): String {
    val arr = JSONArray()
    models.forEach { m ->
        arr.put(
            JSONObject().apply {
                put("id", m.id)
                put("displayName", m.displayName)
                put("providerId", m.providerId)
                put("supportsVision", m.supportsVision)
                put("supportsFunctionCalling", m.supportsFunctionCalling)
                put("supportsImageGen", m.supportsImageGen)
                put("contextWindow", m.contextWindow)
                put("isCustom", m.isCustom)
                put("builtInTools", JSONArray(m.builtInTools))
                put("headers", JSONObject(m.headers))
                put("customBody", m.customBody ?: JSONObject.NULL)
            }
        )
    }
    return arr.toString()
}

fun modelsFromJson(raw: String): List<AIModel> = try {
    val arr = JSONArray(raw)
    (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        val tools = o.optJSONArray("builtInTools")
        val headers = o.optJSONObject("headers")
        AIModel(
            id = o.getString("id"),
            displayName = o.optString("displayName", o.getString("id")),
            providerId = o.optString("providerId"),
            supportsVision = o.optBoolean("supportsVision"),
            supportsFunctionCalling = o.optBoolean("supportsFunctionCalling", true),
            supportsImageGen = o.optBoolean("supportsImageGen"),
            contextWindow = o.optInt("contextWindow", 8192),
            isCustom = o.optBoolean("isCustom"),
            builtInTools = tools?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
            headers = headers?.let { h -> h.keys().asSequence().associateWith { h.getString(it) } } ?: emptyMap(),
            customBody = if (o.isNull("customBody")) null else o.optString("customBody")
        )
    }
} catch (e: Exception) {
    emptyList()
}
