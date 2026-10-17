package com.ariai.app.data.remote

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Legacy MCP façade. Server discovery and tool execution are not implemented yet;
 * this class must not pretend that remote calls have succeeded.
 */
data class MCPServer(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    val tools: List<MCPTool> = emptyList()
)

data class MCPTool(
    val name: String,
    val description: String,
    val inputSchema: String // JSON schema
)

class MCPClient {
    private val servers = mutableListOf<MCPServer>()

    fun addServer(server: MCPServer) {
        servers.add(server)
    }

    fun removeServer(id: String) {
        servers.removeIf { it.id == id }
    }

    fun getServers(): List<MCPServer> = servers.toList()

    fun getAvailableTools(): List<MCPTool> {
        return servers.filter { it.enabled }.flatMap { it.tools }
    }

    fun callTool(toolName: String, arguments: String): Flow<String> = flow {
        throw UnsupportedOperationException("MCP tool invocation is not implemented")
    }

    /** No tools are advertised to models until server discovery and invocation are wired up. */
    fun formatToolsForPrompt(): String = ""
}
