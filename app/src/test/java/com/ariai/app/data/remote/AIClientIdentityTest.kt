package com.ariai.app.data.remote

import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AIClientIdentityTest {
    @Test
    fun `MIA Router catalog request uses the identity accepted by direct model discovery`() {
        val request = Request.Builder()
            .url("https://miarouter.online/v1/models")
            .get()
            .build()

        val identified = applyProviderClientIdentity(request)

        assertEquals("AriAI-Android", identified.header("User-Agent"))
        assertEquals("application/json", identified.header("Accept"))
    }

    @Test
    fun `MIA Router chat request preserves its streaming Accept header`() {
        val request = Request.Builder()
            .url("https://miarouter.online/v1/chat/completions")
            .post("{}".toRequestBody())
            .header("Accept", "text/event-stream, application/json")
            .build()

        val identified = applyProviderClientIdentity(request)

        assertEquals("AriAI-Android", identified.header("User-Agent"))
        assertEquals("text/event-stream, application/json", identified.header("Accept"))
    }

    @Test
    fun `unlisted providers keep the default OkHttp identity`() {
        val request = Request.Builder()
            .url("https://api.example.com/v1/models")
            .get()
            .build()

        val identified = applyProviderClientIdentity(request)

        assertNull(identified.header("User-Agent"))
        assertNull(identified.header("Accept"))
    }
}
