package com.ariai.app.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class McpServerCodecTest {
    @Test
    fun roundTripsServerMetadataAndEnabledState() {
        val servers = listOf(
            McpServerItem(
                id = "server-1",
                name = "Local tools",
                url = "http://localhost:8080/mcp",
                transport = McpTransport.STREAMABLE_HTTP,
                enabled = false
            ),
            McpServerItem(
                id = "server-2",
                name = "Remote tools",
                url = "https://mcp.example.test/sse",
                transport = McpTransport.SSE,
                enabled = true
            )
        )

        assertEquals(servers, McpServerCodec.decode(McpServerCodec.encode(servers)))
    }

    @Test
    fun acceptsOnlyHttpEndpointsWithAHostAndNoUrlEmbeddedCredentials() {
        assertTrue(isValidMcpServerUrl("https://mcp.example.test/v1/mcp"))
        assertTrue(isValidMcpServerUrl("http://127.0.0.1:8080/sse"))
        assertFalse(isValidMcpServerUrl("https://"))
        assertFalse(isValidMcpServerUrl("ftp://mcp.example.test"))
        assertFalse(isValidMcpServerUrl("https://user:secret@mcp.example.test/mcp"))
        assertFalse(isValidMcpServerUrl("https://mcp.example.test/mcp?token=secret"))
        assertFalse(isValidMcpServerUrl("not a URL"))
    }

    @Test
    fun malformedStoredDataFailsClosed() {
        assertTrue(McpServerCodec.decode("not-json").isEmpty())
        assertTrue(McpServerCodec.decode(null).isEmpty())
    }
}
