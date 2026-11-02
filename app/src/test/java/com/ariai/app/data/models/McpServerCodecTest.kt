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
    fun preservesLegacyRemoteHttpConfigurationWithoutAllowingNewConnections() {
        val legacy = McpServerItem(
            id = "legacy-http",
            name = "Old server",
            url = "http://mcp.example.test/sse",
            transport = McpTransport.SSE
        )
        assertFalse(isValidMcpServerUrl(legacy.url))
        assertEquals(legacy, McpServerCodec.decode(McpServerCodec.encode(listOf(legacy))).single())
    }

    @Test
    fun acceptsOnlyHttpsRemoteEndpointsAndLoopbackHttpDevelopmentEndpoints() {
        assertTrue(isValidMcpServerUrl("https://mcp.example.test/v1/mcp"))
        assertTrue(isValidMcpServerUrl("http://127.0.0.1:8080/sse"))
        assertTrue(isValidMcpServerUrl("http://10.0.2.2:8080/sse"))
        assertFalse(isValidMcpServerUrl("http://mcp.example.test/mcp"))
        assertFalse(isValidMcpServerUrl("https://"))
        assertFalse(isValidMcpServerUrl("ftp://mcp.example.test"))
        assertFalse(isValidMcpServerUrl("https://user:secret@mcp.example.test/mcp"))
        assertFalse(isValidMcpServerUrl("https://mcp.example.test/mcp?token=secret"))
        assertFalse(isValidMcpServerUrl("https://mcp.example.test/mcp#token=secret"))
        assertFalse(isValidMcpServerUrl("not a URL"))
    }

    @Test
    fun authenticationSecretIsExcludedFromServerMetadata() {
        val server = McpServerItem(
            id = "server-auth",
            name = "Authenticated server",
            url = "https://mcp.example.test/mcp",
            transport = McpTransport.STREAMABLE_HTTP,
            authType = McpAuthType.BEARER,
            authSecret = "never-store-this-in-cleartext",
            allowedToolNames = setOf("read_file", "search")
        )
        val metadata = McpServerCodec.encode(listOf(server))
        assertFalse(metadata.contains(server.authSecret!!))
        assertEquals(server.copy(authSecret = null), McpServerCodec.decode(metadata).single())

        val ciphertext = "enc1:opaque-base64-ciphertext"
        val encryptedJson = McpServerAuthCipherCodec.encode(mapOf(server.id to ciphertext))
        assertFalse(encryptedJson.contains(server.authSecret!!))
        assertEquals(mapOf(server.id to ciphertext), McpServerAuthCipherCodec.decode(encryptedJson))
    }

    @Test
    fun malformedStoredDataFailsClosed() {
        assertTrue(McpServerCodec.decode("not-json").isEmpty())
        assertTrue(McpServerCodec.decode(null).isEmpty())
    }
}
