package com.ariai.app.net

import org.json.JSONObject

/** Pure JSON parsing helpers shared by the network client and unit tests. */
internal object LlmJson {
    fun parseModelIds(body: String): List<String> {
        if (body.isBlank()) return emptyList()

        return runCatching {
            val json = JSONObject(body)
            val data = json.optJSONArray("data") ?: json.optJSONArray("models") ?: return@runCatching emptyList()
            (0 until data.length()).mapNotNull { i ->
                val item = data.opt(i)
                val model = item as? JSONObject ?: return@mapNotNull null
                model.optString("id")
                    .ifBlank { model.optString("name") }
                    .removePrefix("models/")
                    .takeIf { it.isNotBlank() }
            }.distinct().sorted()
        }.getOrDefault(emptyList())
    }
}