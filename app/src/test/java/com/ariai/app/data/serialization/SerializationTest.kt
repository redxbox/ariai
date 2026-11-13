package com.ariai.app.data.serialization

import com.ariai.app.data.models.AIModel
import com.ariai.app.data.models.McpAuthType
import com.ariai.app.data.models.McpServerItem
import com.ariai.app.data.models.McpTransport
import com.ariai.app.data.models.OAuth2Config
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import com.ariai.app.data.models.modelsFromJson
import com.ariai.app.data.models.modelsToJson
import com.ariai.app.data.models.providerFromJson
import com.ariai.app.data.models.providerToJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@Serializable
private data class ProviderTypeEnvelope(val type: ProviderType = ProviderType.OPENAI_COMPATIBLE)

class SerializationTest {
    @Test
    fun sharedJsonParserUsesTheConfiguredCompatibilityOptions() {
        assertTrue(appJson.configuration.ignoreUnknownKeys)
        assertTrue(appJson.configuration.coerceInputValues)
        assertTrue(appJson.configuration.encodeDefaults)
        assertTrue(appJson.configuration.isLenient)
    }

    @Test
    fun sharedJsonParserAcceptsUnknownFieldsAndCoercesInvalidEnumValues() {
        val models = modelsFromJson(
            """[{"id":"gpt-safe","displayName":"Safe","providerId":"p1","futureField":true}]"""
        )
        val envelope = appJson.decodeFromString<ProviderTypeEnvelope>("""{"type":"FUTURE_PROVIDER"}""")

        assertEquals(1, models.size)
        assertEquals("gpt-safe", models.single().id)
        assertEquals(ProviderType.OPENAI_COMPATIBLE, envelope.type)
    }

    @Test
    fun providerSerializationDoesNotEmitCredentialsOrEncryptedSettings() {
        val provider = Provider(
            id = "provider-1",
            name = "Private provider",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://example.test/v1",
            apiKey = "secret-api-key",
            models = listOf(
                AIModel(
                    id = "private-model",
                    displayName = "Private model",
                    providerId = "provider-1",
                    headers = mapOf("X-Model-Key" to "model-secret"),
                    customBody = "{\"token\":\"body-secret\"}"
                )
            ),
            customHeaders = mapOf("X-Provider-Key" to "header-secret"),
            customBody = "{\"token\":\"provider-body-secret\"}",
            oauthStateJson = "oauth-refresh-secret",
            oauthConfig = OAuth2Config(
                authorizationEndpoint = "https://auth.example.test/authorize",
                tokenEndpoint = "https://auth.example.test/token",
                clientId = "public-client"
            )
        )

        val encoded = appJson.encodeToString(Provider.serializer(), provider)

        listOf(
            "secret-api-key",
            "model-secret",
            "header-secret",
            "body-secret",
            "oauth-refresh-secret"
        ).forEach { assertFalse("Unexpected secret in $encoded", encoded.contains(it)) }
        val decoded = appJson.decodeFromString<Provider>(encoded)
        assertEquals("provider-1", decoded.id)
        assertEquals("", decoded.apiKey)
        assertTrue(decoded.models.isEmpty())
        assertTrue(decoded.customHeaders.isEmpty())
        assertNull(decoded.customBody)
        assertNull(decoded.oauthStateJson)
        assertNotNull(decoded.oauthConfig)
    }

    @Test
    fun mcpServerSerializationOmitsInMemoryCredentialsAndKeepsStableMetadataNames() {
        val server = McpServerItem(
            id = "mcp-server",
            name = "Remote MCP",
            url = "https://mcp.example.test/",
            transport = McpTransport.STREAMABLE_HTTP,
            authType = McpAuthType.BEARER,
            authSecret = "mcp-access-token",
            allowedToolNames = setOf("read_file"),
            clearAuthSecret = true
        )

        val encoded = appJson.encodeToString(McpServerItem.serializer(), server)
        val decoded = appJson.decodeFromString<McpServerItem>(encoded)

        assertFalse(encoded.contains("mcp-access-token"))
        assertTrue(encoded.contains("allowedTools"))
        assertEquals(setOf("read_file"), decoded.allowedToolNames)
        assertNull(decoded.authSecret)
        assertFalse(decoded.clearAuthSecret)
    }

    @Test
    fun providerTransferJsonRemainsExplicitAndRoundTripsQrFields() {
        val provider = Provider(
            id = "provider-qr",
            name = "QR provider",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://example.test/v1",
            apiKey = "qr-api-key",
            customHeaders = mapOf("X-Custom" to "header-value")
        )

        val transferJson = providerToJson(provider)
        val restored = providerFromJson(transferJson)

        assertTrue(transferJson.contains("qr-api-key"))
        assertNotNull(restored)
        assertEquals(provider.name, restored?.name)
        assertEquals(provider.type, restored?.type)
        assertEquals(provider.baseUrl, restored?.baseUrl)
        assertEquals(provider.apiKey, restored?.apiKey)
        assertEquals(provider.customHeaders, restored?.customHeaders)
    }

    @Test
    fun encryptedModelCodecPreservesSettingsAndToleratesFutureFields() {
        val model = AIModel(
            id = "vision-model",
            displayName = "Vision Model",
            providerId = "provider-1",
            supportsVision = true,
            headers = mapOf("X-Model-Key" to "encrypted-before-storage"),
            customBody = "{\"temperature\":0.4}"
        )

        val encoded = modelsToJson(listOf(model))
        val restored = modelsFromJson(encoded + " ")

        assertEquals(model.id, restored.single().id)
        assertEquals(model.headers, restored.single().headers)
        assertEquals(model.customBody, restored.single().customBody)
        val futureRecord = encoded.dropLast(2) + ",\"futureField\":\"ignored\"}]"
        assertEquals(model.id, modelsFromJson(futureRecord).single().id)
    }

    @Test
    fun jsonRpcDtoEscapesPayloadValuesAndIgnoresUnknownResponseFields() {
        val request = JsonRpcRequest(
            id = 7,
            method = "tools/call",
            params = buildJsonObject { put("text", "quote: \" slash: \\ newline:\n") }
        )
        val encoded = appJson.encodeToString(JsonRpcRequest.serializer(), request)
        val decoded = appJson.decodeFromString<JsonRpcRequest>(encoded)
        assertEquals(request, decoded)

        val response = appJson.decodeFromString<JsonRpcResponse>(
            """{"jsonrpc":"2.0","id":7,"result":{"tools":[]},"serverExtension":{"v":1}}"""
        )
        assertEquals("2.0", response.jsonrpc)
        assertEquals("7", (response.id as kotlinx.serialization.json.JsonPrimitive).content)
        assertNull(response.error)
    }
}
