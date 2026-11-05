package com.ariai.app.data.remote

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class AIClientGeminiResponseTest {
    @Test
    fun `normal response omits Gemini thought parts`() {
        val response = JSONObject(
            """
            {
              "candidates": [{
                "content": {"parts": [
                  {"text": "internal reasoning", "thought": true},
                  {"text": "User-facing answer."},
                  {"text": "another thought", "thought": true}
                ]}
              }]
            }
            """.trimIndent()
        )

        assertEquals("User-facing answer.", AIClient().parseGeminiResponse(response))
    }

    @Test
    fun `normal response tolerates missing thought marker`() {
        val response = JSONObject(
            """
            {"candidates":[{"content":{"parts":[{"text":"Visible"}]}}]}
            """.trimIndent()
        )

        assertEquals("Visible", AIClient().parseGeminiResponse(response))
    }
}
