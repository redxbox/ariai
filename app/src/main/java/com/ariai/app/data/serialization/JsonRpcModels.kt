package com.ariai.app.data.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Long,
    val method: String,
    val params: JsonObject
)

@Serializable
data class JsonRpcNotification(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: JsonObject
)

@Serializable
data class JsonRpcError(
    val code: Int? = null,
    val message: String? = null
)

/** Unknown response fields are ignored by [appJson], preserving forward compatibility. */
@Serializable
data class JsonRpcResponse(
    val jsonrpc: String? = null,
    val id: JsonElement? = null,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null
)

@Serializable
data class McpToolCallParams(
    val name: String,
    val arguments: JsonObject
)
