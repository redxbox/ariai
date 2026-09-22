package com.ariai.app.util

import org.json.JSONObject

object Errors {
    fun friendly(e: Throwable): String {
        val raw = e.message.orEmpty()
        val m = (e.javaClass.simpleName + " " + raw)
        val api = parseApiError(raw)
        if (api != null) {
            val (code, status, reason, message) = api
            val label = reason?.takeIf { it.isNotBlank() } ?: status?.takeIf { it.isNotBlank() } ?: "API_ERROR"
            val detail = message?.takeIf { it.isNotBlank() }?.let { ": \${it.take(220)}" }.orEmpty()
            return "Gemini/API error $code $label$detail"
        }
        return when {
            m.contains("401") || m.contains("invalid_api_key", true) || m.contains("authentication", true) ->
                "Authentication failed (401). Check the API key and its project access."
            m.contains("403") ->
                "Permission denied (403). The key/project is authenticated but is not allowed to use this resource."
            m.contains("402") ->
                "Payment required (402). Check billing or prepay credits for this project."
            m.contains("429") ->
                "Too many requests or quota exceeded (429). Wait a moment or check the provider quota."
            m.contains("404") ->
                "Not found (404). Check the base URL and model ID."
            m.contains("400") ->
                "Bad request (400). Check the model, request format, and provider settings."
            m.contains("UnknownHost") || m.contains("Unable to resolve") || m.contains("Failed to connect") ->
                "Network error. Check internet access, DNS, VPN, and the base URL."
            m.contains("timeout", true) || m.contains("timed out", true) ->
                "Timed out. Check the network and try again."
            m.contains("Canceled") || m.contains("cancel", true) -> "Stopped."
            else -> redact(raw).take(240).ifBlank { "Request failed. Check the provider and try again." }
        }
    }

    private fun parseApiError(raw: String): ApiError? {
        val http = Regex("""HTTP\s+(\d{3})""", RegexOption.IGNORE_CASE)
            .find(raw)?.groupValues?.get(1)?.toIntOrNull()
        val jsonStart = raw.indexOf('{')
        if (jsonStart < 0) return null

        return try {
            val root = JSONObject(raw.substring(jsonStart))
            val err = root.optJSONObject("error") ?: return http?.let { ApiError(it, null, null, null) }
            val code = err.optInt("code", http ?: 0)
            val status = err.optString("status").takeIf { it.isNotBlank() }
            val message = err.optString("message").takeIf { it.isNotBlank() }

            var reason: String? = null
            val details = err.optJSONArray("details")
            if (details != null) {
                for (i in 0 until details.length()) {
                    val detail = details.optJSONObject(i) ?: continue
                    reason = detail.optString("reason").takeIf { it.isNotBlank() } ?: reason
                    if (reason != null) break
                }
            }
            ApiError(code, status, reason, message)
        } catch (_: Exception) {
            http?.let { ApiError(it, null, null, null) }
        }
    }

    private data class ApiError(
        val code: Int,
        val status: String?,
        val reason: String?,
        val message: String?
    )

    fun redact(s: String): String =
        s.replace(
            Regex("""(?i)(api[_-]?key|authorization|bearer|x-api-key|x-goog-api-key)\s*[:=]\s*"?[^,;\s}\"']+"""),
            "$1=***"
        )
            .replace(
                Regex("""(?i)("(?:api[_-]?key|authorization|x-api-key|x-goog-api-key)"\s*:\s*")[^"]*(")"""),
                "$1***$2"
            )
            .replace(Regex("""(?i)([?&](?:api[_-]?key|key|token|access_token|authorization)=)[^&\s]+"""), "$1***")
            .replace(Regex("""sk-[A-Za-z0-9]{8,}"""), "sk-***")
            .replace(Regex("""(?i)AQ\.[A-Za-z0-9._~-]{8,}"""), "AQ.***")
}
