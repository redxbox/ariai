package com.ariai.app.ui.screens

import com.ariai.app.util.tx
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.McpAuthType
import com.ariai.app.data.models.McpServerItem
import com.ariai.app.data.models.McpTransport
import com.ariai.app.data.models.OAuth2Config
import com.ariai.app.data.models.isValidOAuth2Endpoint
import com.ariai.app.data.models.sameSettingsAs
import com.ariai.app.data.models.isValidMcpHeaderName
import com.ariai.app.data.models.isValidMcpServerUrl
import com.ariai.app.data.remote.MCPPrompt
import com.ariai.app.data.remote.MCPResource
import com.ariai.app.ui.theme.*

private enum class McpCatalogKind { RESOURCES, PROMPTS }

private data class McpCatalogDialogState(
    val serverId: String,
    val serverName: String,
    val kind: McpCatalogKind,
    val loading: Boolean = true,
    val resources: List<MCPResource> = emptyList(),
    val prompts: List<MCPPrompt> = emptyList(),
    val error: String? = null
)

private val Accent: Color @Composable @ReadOnlyComposable get() = AriAccent
private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleMcpScreen(
    servers: List<McpServerItem>,
    onToggle: (String) -> Unit,
    onAdd: (McpServerItem) -> Unit,
    onAuthorizeOAuth: (McpServerItem) -> Unit = {},
    onRemove: (String) -> Unit,
    onBack: () -> Unit,
    runtimeStates: Map<String, McpServerRuntimeState> = emptyMap(),
    onTestConnection: (String) -> Unit = {},
    onToggleTool: (String, String) -> Unit = { _, _ -> },
    onListResources: (String, (List<MCPResource>, String?) -> Unit) -> Unit = { _, done -> done(emptyList(), null) },
    onListPrompts: (String, (List<MCPPrompt>, String?) -> Unit) -> Unit = { _, done -> done(emptyList(), null) }
) {
    var showAdd by remember { mutableStateOf(false) }
    var editingServer by remember { mutableStateOf<McpServerItem?>(null) }
    var serverToEnable by remember { mutableStateOf<McpServerItem?>(null) }
    var catalogDialog by remember { mutableStateOf<McpCatalogDialogState?>(null) }

    Scaffold(
        containerColor = AriPaper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AriPaper),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tx("Back"), tint = Ink)
                    }
                },
                title = { Text(tx("MCP servers"), fontWeight = FontWeight.SemiBold, color = Ink) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingServer = null; showAdd = true },
                containerColor = Accent,
                contentColor = AriCard,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("mcp-add-server").semantics {
                    contentDescription = tx("Add server")
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(tx("Add server"), fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = AriCard, shadowElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Extension, contentDescription = null, tint = Accent)
                            Text(tx("Model Context Protocol"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink)
                        }
                        Text(
                            tx("Connect remote MCP servers. Choose specific tools before they can be used in chats."),
                            fontSize = 12.sp,
                            color = Ink.copy(alpha = 0.68f)
                        )
                        Text(
                            tx("MCP tools can read or change data on a server. Enable only servers you trust; results from servers are untrusted."),
                            fontSize = 12.sp,
                            color = Ink.copy(alpha = 0.68f)
                        )
                    }
                }
            }

            if (servers.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Accent)
                        }
                        Text(tx("No MCP servers yet"), fontWeight = FontWeight.SemiBold, color = Ink)
                    }
                }
            }

            items(servers, key = { "server:${it.id}" }) { server ->
                val status = runtimeStates[server.id] ?: McpServerRuntimeState()
                val credentialMissing = server.authType != McpAuthType.NONE && server.authSecret.isNullOrBlank()
                Surface(shape = RoundedCornerShape(18.dp), color = AriCard, shadowElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(server.name, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(server.transport.label, fontSize = 11.sp, color = Ink.copy(alpha = 0.55f))
                                Text(server.url, fontSize = 11.sp, color = Ink.copy(alpha = 0.45f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(
                                onClick = { editingServer = server; showAdd = true },
                                modifier = Modifier.semantics { contentDescription = tx("Edit MCP server") }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Ink.copy(alpha = 0.55f))
                            }
                            IconButton(
                                onClick = { onTestConnection(server.id) },
                                enabled = !status.isConnecting,
                                modifier = Modifier.semantics { contentDescription = tx("Test connection") }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = if (!status.isConnecting) Accent else Ink.copy(alpha = 0.35f))
                            }
                            IconButton(onClick = { onRemove(server.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = tx("Remove server"), tint = Ink.copy(alpha = 0.45f))
                            }
                            Switch(
                                checked = server.enabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) serverToEnable = server else onToggle(server.id)
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = Accent),
                                modifier = Modifier.testTag("mcp-toggle-${server.id}")
                                    .semantics { contentDescription = "${server.name} ${tx("Enable")}" }
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val statusColor = when {
                                status.error != null -> Color(0xFFB3261E)
                                status.isConnecting -> Accent
                                status.checkedAtMillis != null -> Color(0xFF2E7D5B)
                                else -> Ink.copy(alpha = 0.55f)
                            }
                            when {
                                status.error != null -> Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                                status.checkedAtMillis != null && status.error == null -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                            }
                            val statusText = when {
                                status.isConnecting -> tx("Connecting…")
                                status.error != null -> "${tx("Connection failed")}: ${status.error}"
                                credentialMissing -> tx("Credential needs to be entered again.")
                                status.checkedAtMillis != null -> "${status.toolNames.size} ${tx("tools discovered")}"
                                server.enabled -> tx("Enabled · not tested")
                                else -> tx("Disabled")
                            }
                            Text(statusText, fontSize = 11.sp, color = statusColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }

                        if (server.authType != McpAuthType.NONE) {
                            Text(
                                if (credentialMissing) tx("Credential needs to be entered again.") else tx("Credential stored securely"),
                                fontSize = 11.sp,
                                color = Ink.copy(alpha = 0.55f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                enabled = server.enabled && !credentialMissing && !status.isConnecting,
                                onClick = {
                                    catalogDialog = McpCatalogDialogState(server.id, server.name, McpCatalogKind.RESOURCES)
                                    onListResources(server.id) { resources, error ->
                                        catalogDialog = catalogDialog?.takeIf {
                                            it.serverId == server.id && it.kind == McpCatalogKind.RESOURCES
                                        }?.copy(loading = false, resources = resources, error = error)
                                    }
                                }
                            ) { Text(tx("Resources")) }
                            TextButton(
                                enabled = server.enabled && !credentialMissing && !status.isConnecting,
                                onClick = {
                                    catalogDialog = McpCatalogDialogState(server.id, server.name, McpCatalogKind.PROMPTS)
                                    onListPrompts(server.id) { prompts, error ->
                                        catalogDialog = catalogDialog?.takeIf {
                                            it.serverId == server.id && it.kind == McpCatalogKind.PROMPTS
                                        }?.copy(loading = false, prompts = prompts, error = error)
                                    }
                                }
                            ) { Text(tx("Prompts")) }
                        }

                        if (status.toolNames.isNotEmpty()) {
                            Text(tx("Discovered tools · select which may be used in chats."), fontSize = 11.sp, color = Ink.copy(alpha = 0.65f))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                status.toolNames.forEach { toolName ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Checkbox(
                                            checked = toolName in server.allowedToolNames,
                                            onCheckedChange = { onToggleTool(server.id, toolName) },
                                            colors = CheckboxDefaults.colors(checkedColor = Accent),
                                            modifier = Modifier.testTag("mcp-tool-${server.id}-$toolName")
                                        )
                                        Text(toolName, fontSize = 12.sp, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    catalogDialog?.let { catalog ->
        val title = tx(if (catalog.kind == McpCatalogKind.RESOURCES) "MCP resources" else "MCP prompts")
        AlertDialog(
            onDismissRequest = { catalogDialog = null },
            title = { Text("$title · ${catalog.serverName}", color = Ink, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when {
                        catalog.loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Accent, strokeWidth = 2.dp)
                            Text(tx("Loading MCP catalog…"), color = Ink)
                        }
                        catalog.error != null -> Text(catalog.error, color = Color(0xFFB3261E))
                        catalog.kind == McpCatalogKind.RESOURCES && catalog.resources.isEmpty() ->
                            Text(tx("This server did not advertise any resources."), color = Ink.copy(alpha = 0.65f))
                        catalog.kind == McpCatalogKind.PROMPTS && catalog.prompts.isEmpty() ->
                            Text(tx("This server did not advertise any prompts."), color = Ink.copy(alpha = 0.65f))
                        catalog.kind == McpCatalogKind.RESOURCES -> catalog.resources.forEach { resource ->
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(resource.name, color = Ink, fontWeight = FontWeight.Medium)
                                Text(resource.uri, color = Ink.copy(alpha = 0.65f), fontSize = 11.sp)
                                if (resource.mimeType.isNotBlank()) Text(resource.mimeType, color = Ink.copy(alpha = 0.55f), fontSize = 11.sp)
                                if (resource.description.isNotBlank()) Text(resource.description, color = Ink.copy(alpha = 0.72f), fontSize = 12.sp)
                            }
                        }
                        else -> catalog.prompts.forEach { prompt ->
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(prompt.name, color = Ink, fontWeight = FontWeight.Medium)
                                if (prompt.description.isNotBlank()) Text(prompt.description, color = Ink.copy(alpha = 0.72f), fontSize = 12.sp)
                                if (prompt.arguments.isNotEmpty()) {
                                    val argumentNames = prompt.arguments.joinToString(", ") { argument ->
                                        argument.name + if (argument.required) " *" else ""
                                    }
                                    Text("${tx("Arguments")}: $argumentNames", color = Ink.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { catalogDialog = null }) { Text(tx("Close")) }
            },
            containerColor = AriCard
        )
    }

    if (showAdd) {
        AddMcpServerSheet(
            initial = editingServer,
            onSave = { onAdd(it); showAdd = false; editingServer = null },
            onAuthorizeOAuth = { server -> onAuthorizeOAuth(server); showAdd = false; editingServer = null },
            onDismiss = { showAdd = false; editingServer = null }
        )
    }

    serverToEnable?.let { server ->
        AlertDialog(
            onDismissRequest = { serverToEnable = null },
            title = { Text(tx("Enable MCP server?"), color = Ink, fontWeight = FontWeight.SemiBold) },
            text = { Text(tx("MCP tools can read or change data on a server. Enable only servers you trust; results from servers are untrusted."), color = Ink) },
            confirmButton = {
                TextButton(onClick = {
                    onToggle(server.id)
                    serverToEnable = null
                }) { Text(tx("Enable server")) }
            },
            dismissButton = {
                TextButton(onClick = { serverToEnable = null }) { Text(tx("Cancel")) }
            },
            containerColor = AriCard
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMcpServerSheet(
    initial: McpServerItem?,
    onSave: (McpServerItem) -> Unit,
    onAuthorizeOAuth: (McpServerItem) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var url by remember(initial?.id) { mutableStateOf(initial?.url.orEmpty()) }
    var transport by remember(initial?.id) { mutableStateOf(initial?.transport ?: McpTransport.STREAMABLE_HTTP) }
    var authType by remember(initial?.id) { mutableStateOf(initial?.authType ?: McpAuthType.NONE) }
    var authHeaderName by remember(initial?.id) { mutableStateOf(initial?.authHeaderName ?: "X-API-Key") }
    var authSecret by remember(initial?.id) { mutableStateOf("") }
    var oauthAuthorizationEndpoint by remember(initial?.id) { mutableStateOf(initial?.oauthConfig?.authorizationEndpoint.orEmpty()) }
    var oauthTokenEndpoint by remember(initial?.id) { mutableStateOf(initial?.oauthConfig?.tokenEndpoint.orEmpty()) }
    var oauthClientId by remember(initial?.id) { mutableStateOf(initial?.oauthConfig?.clientId.orEmpty()) }
    var oauthScopes by remember(initial?.id) { mutableStateOf(initial?.oauthConfig?.scopes.orEmpty()) }
    val oauthConfig = OAuth2Config(
        authorizationEndpoint = oauthAuthorizationEndpoint,
        tokenEndpoint = oauthTokenEndpoint,
        clientId = oauthClientId,
        scopes = oauthScopes
    ).normalized()
    val urlValid = isValidMcpServerUrl(url)
    val canKeepSavedCredential = initial != null && initial.authType == authType && !initial.authSecret.isNullOrBlank()
    val suppliedCredentialIsValid = authSecret.isNotBlank() && authSecret.length <= 4_096 && authSecret.none { it == '\r' || it == '\n' }
    val authValid = when (authType) {
        McpAuthType.NONE -> true
        McpAuthType.OAUTH2 -> oauthConfig.validationError() == null
        McpAuthType.BEARER, McpAuthType.CUSTOM_HEADER ->
            (suppliedCredentialIsValid || (authSecret.isBlank() && canKeepSavedCredential)) &&
                (authType != McpAuthType.CUSTOM_HEADER || isValidMcpHeaderName(authHeaderName))
    }
    val valid = name.isNotBlank() && urlValid && authValid

    fun draftServer(): McpServerItem = McpServerItem(
        id = initial?.id ?: java.util.UUID.randomUUID().toString(),
        name = name.trim(),
        url = url.trim(),
        transport = transport,
        enabled = initial?.enabled ?: false,
        authType = authType,
        authHeaderName = authHeaderName.trim().ifBlank { "X-API-Key" },
        authSecret = when (authType) {
            McpAuthType.NONE -> null
            McpAuthType.OAUTH2 -> initial?.takeIf {
                it.oauthConfig.sameSettingsAs(oauthConfig) && it.url.trim() == url.trim()
            }?.authSecret
            else -> when {
                authSecret.isNotBlank() -> authSecret.trim()
                canKeepSavedCredential -> initial?.authSecret
                else -> null
            }
        },
        allowedToolNames = initial?.allowedToolNames.orEmpty(),
        oauthConfig = oauthConfig.takeIf { authType == McpAuthType.OAUTH2 },
        clearAuthSecret = initial != null && authType != McpAuthType.NONE && authSecret.isBlank() &&
            (authType != initial.authType ||
                (authType == McpAuthType.OAUTH2 &&
                    (!initial.oauthConfig.sameSettingsAs(oauthConfig) || initial.url.trim() != url.trim())))
    )

    PaperBottomSheet(
        onDismissRequest = onDismiss,
        shape = paperSheetShape(28.dp),
        skipPartiallyExpanded = true
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 760.dp).imePadding()
                .padding(horizontal = 20.dp).padding(bottom = 12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
            Text(tx(if (initial == null) "Add MCP server" else "Edit MCP server"), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Ink)
            Text(
                tx(if (initial == null) {
                    "New servers start disabled. Enable only servers you trust; a server may read or change data when its tools are called."
                } else {
                    "Leave the credential blank to keep the saved value."
                }),
                fontSize = 12.sp,
                color = Ink.copy(alpha = 0.62f)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(64) },
                label = { Text(tx("Name")) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("mcp-server-name")
            )

            Text(tx("Transport"), fontWeight = FontWeight.Medium, color = Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                McpTransport.values().forEach { item ->
                    FilterChip(selected = transport == item, onClick = { transport = item }, label = { Text(item.label) })
                }
            }

            OutlinedTextField(
                value = url,
                onValueChange = { url = it.take(2_048) },
                label = { Text(tx("Server URL")) },
                placeholder = { Text("https://") },
                singleLine = true,
                isError = url.isNotBlank() && !urlValid,
                supportingText = if (url.isNotBlank() && !urlValid) {
                    { Text(tx("Use HTTPS for remote servers. HTTP is allowed only for localhost testing; do not put credentials in the URL.")) }
                } else null,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("mcp-server-url")
            )

            Text(tx("Authentication"), fontWeight = FontWeight.Medium, color = Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = authType == McpAuthType.NONE,
                    onClick = { authType = McpAuthType.NONE },
                    label = { Text(tx("None")) }
                )
                FilterChip(
                    selected = authType == McpAuthType.BEARER,
                    onClick = { authType = McpAuthType.BEARER },
                    label = { Text(tx("Bearer token")) }
                )
                FilterChip(
                    selected = authType == McpAuthType.CUSTOM_HEADER,
                    onClick = { authType = McpAuthType.CUSTOM_HEADER },
                    label = { Text(tx("Custom header")) }
                )
                FilterChip(
                    selected = authType == McpAuthType.OAUTH2,
                    onClick = { authType = McpAuthType.OAUTH2 },
                    label = { Text(tx("OAuth 2.0")) }
                )
            }
            if (authType == McpAuthType.OAUTH2) {
                Text(
                    tx("Uses OAuth 2.0 Authorization Code with PKCE. No client secret is stored in the app."),
                    fontSize = 12.sp,
                    color = Ink.copy(alpha = 0.62f)
                )
                OutlinedTextField(
                    value = oauthAuthorizationEndpoint,
                    onValueChange = { oauthAuthorizationEndpoint = it.take(OAuth2Config.MAX_ENDPOINT_LENGTH) },
                    label = { Text(tx("Authorization endpoint (HTTPS)")) },
                    singleLine = true,
                    isError = oauthAuthorizationEndpoint.isNotBlank() && !isValidOAuth2Endpoint(oauthAuthorizationEndpoint),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("mcp-oauth-authorization-endpoint")
                )
                OutlinedTextField(
                    value = oauthTokenEndpoint,
                    onValueChange = { oauthTokenEndpoint = it.take(OAuth2Config.MAX_ENDPOINT_LENGTH) },
                    label = { Text(tx("Token endpoint (HTTPS)")) },
                    singleLine = true,
                    isError = oauthTokenEndpoint.isNotBlank() && !isValidOAuth2Endpoint(oauthTokenEndpoint),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("mcp-oauth-token-endpoint")
                )
                OutlinedTextField(
                    value = oauthClientId,
                    onValueChange = { oauthClientId = it.take(OAuth2Config.MAX_CLIENT_ID_LENGTH) },
                    label = { Text(tx("OAuth client ID")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("mcp-oauth-client-id")
                )
                OutlinedTextField(
                    value = oauthScopes,
                    onValueChange = { oauthScopes = it.take(OAuth2Config.MAX_SCOPES_LENGTH) },
                    label = { Text(tx("Scopes (space-separated)")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("mcp-oauth-scopes")
                )
                Text(tx("Redirect URI"), fontSize = 12.sp, color = Ink.copy(alpha = 0.62f))
                SelectionContainer {
                    Text(OAuth2Config.DEFAULT_REDIRECT_URI, fontSize = 13.sp, color = Ink)
                }
                Text(
                    if (initial?.authType == McpAuthType.OAUTH2 &&
                        initial.oauthConfig.sameSettingsAs(oauthConfig) && initial.url.trim() == url.trim() &&
                        !initial.authSecret.isNullOrBlank()
                    ) tx("OAuth connected; tokens are stored encrypted on this device.")
                    else tx("Save and connect to authorize this MCP server."),
                    fontSize = 12.sp,
                    color = Ink.copy(alpha = 0.62f)
                )
            } else if (authType != McpAuthType.NONE) {
                if (authType == McpAuthType.CUSTOM_HEADER) {
                    OutlinedTextField(
                        value = authHeaderName,
                        onValueChange = { authHeaderName = it.take(128) },
                        label = { Text(tx("Header name")) },
                        singleLine = true,
                        isError = authHeaderName.isNotBlank() && !isValidMcpHeaderName(authHeaderName),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("mcp-auth-header")
                    )
                }
                OutlinedTextField(
                    value = authSecret,
                    onValueChange = { authSecret = it.take(4_096) },
                    label = { Text(if (authType == McpAuthType.BEARER) tx("Bearer token") else tx("API key or header value")) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = authSecret.isNotBlank() && (authSecret.any { it == '\r' || it == '\n' } || authSecret.length > 4_096),
                    supportingText = { Text(tx("Stored encrypted on this device. Never include the token in the server URL.")) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("mcp-auth-secret")
                )
            }

            if (authType == McpAuthType.OAUTH2) {
                OutlinedButton(
                    onClick = { onAuthorizeOAuth(draftServer()) },
                    enabled = valid,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("mcp-oauth-connect")
                ) { Text(tx("Connect with OAuth"), fontWeight = FontWeight.SemiBold) }
            }
            }

            // Pinned below the scrollable form so Save stays reachable on short screens
            // and with the keyboard open.
            Button(
                onClick = { onSave(draftServer()) },
                enabled = valid,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 4.dp).testTag("mcp-server-save")
            ) { Text(tx("Save"), fontWeight = FontWeight.SemiBold) }
        }
    }
}
