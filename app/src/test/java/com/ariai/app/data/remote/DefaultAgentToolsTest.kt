package com.ariai.app.data.remote

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class DefaultAgentToolsTest {
    @Test
    fun `URL tool accepts only the exact URL from the current user message`() {
        val tool = DefaultAgentTools.openUserUrl(listOf("https://example.com/docs"), PublicUrlFetcher())
        assertNotNull(tool)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { tool!!.execute(JSONObject().put("url", "https://example.com/private")) }
        }
    }

    @Test
    fun `URL tool is not offered for credentials or dot segment paths`() {
        assertNull(DefaultAgentTools.openUserUrl(listOf("https://user:secret@example.com/docs"), PublicUrlFetcher()))
        assertNull(DefaultAgentTools.openUserUrl(listOf("https://example.com/a/../b"), PublicUrlFetcher()))
    }
}
