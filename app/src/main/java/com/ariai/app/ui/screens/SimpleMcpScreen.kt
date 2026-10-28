package com.ariai.app.ui.screens

import com.ariai.app.util.tx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.ariai.app.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.McpServerItem
import com.ariai.app.data.models.McpTransport
import com.ariai.app.data.models.isValidMcpServerUrl

private val Accent: Color @Composable @ReadOnlyComposable get() = AriAccentGreen
private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleMcpScreen(
    servers: List<McpServerItem>,
    onToggle: (String) -> Unit,
    onAdd: (McpServerItem) -> Unit,
    onRemove: (String) -> Unit,
    onBack: () -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AriPaper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AriPaper),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tx("Back"), tint = Ink) } },
                title = { Text(tx("MCP servers"), fontWeight = FontWeight.SemiBold, color = Ink) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                containerColor = Accent,
                contentColor = AriCard,
                shape = RoundedCornerShape(18.dp),
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
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(tx("Model Context Protocol"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink)
                        Text(tx("MCP support is a preview: server URLs are not contacted and tools are not yet used in chats."), fontSize = 12.sp, color = Ink.copy(alpha = 0.6f))
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

            itemsIndexed(servers, key = { _, server -> "server:${server.id}" }) { _, server ->
                Surface(shape = RoundedCornerShape(18.dp), color = AriCard, shadowElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(server.name, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(server.transport.label, fontSize = 11.sp, color = Ink.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(server.url, fontSize = 11.sp, color = Ink.copy(alpha = 0.4f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (server.enabled) tx("Enabled locally · not connected") else tx("Disabled"), fontSize = 11.sp, color = if (server.enabled) AriInk else Ink.copy(alpha = 0.45f))
                        }
                        IconButton(onClick = { onRemove(server.id) }) { Icon(Icons.Default.Delete, contentDescription = tx("Remove server"), tint = Ink.copy(alpha = 0.45f)) }
                        Switch(
                            checked = server.enabled,
                            onCheckedChange = { onToggle(server.id) },
                            colors = SwitchDefaults.colors(checkedTrackColor = Accent),
                            modifier = Modifier.testTag("mcp-toggle-${server.id}")
                                .semantics { contentDescription = "${server.name} ${tx("Enable")}" }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showAdd) {
        AddMcpServerSheet(
            onSave = { onAdd(it); showAdd = false },
            onDismiss = { showAdd = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMcpServerSheet(onSave: (McpServerItem) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var transport by remember { mutableStateOf(McpTransport.STREAMABLE_HTTP) }
    var enabled by remember { mutableStateOf(true) }
    val urlValid = isValidMcpServerUrl(url)
    val valid = name.isNotBlank() && urlValid

    val sheetShape = paperSheetShape(28.dp)
    PaperBottomSheet(
        onDismissRequest = onDismiss,
        shape = sheetShape,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(tx("Add MCP server"), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Ink)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(tx("Enable"), fontWeight = FontWeight.Medium, color = Ink)
                    Text(tx("Preview only: connection and authentication are not active."), fontSize = 12.sp, color = Ink.copy(alpha = 0.5f))
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Accent),
                    modifier = Modifier.semantics { contentDescription = tx("Enable") }
                )
            }

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
                McpTransport.values().forEach { t ->
                    FilterChip(selected = transport == t, onClick = { transport = t }, label = { Text(t.label) })
                }
            }

            OutlinedTextField(
                value = url,
                onValueChange = { url = it.take(2048) },
                label = { Text(tx("Server URL")) },
                placeholder = { Text("https://") },
                singleLine = true,
                isError = url.isNotBlank() && !urlValid,
                supportingText = if (url.isNotBlank() && !urlValid) {
                    { Text(tx("Enter an HTTP(S) URL without embedded credentials or query parameters.")) }
                } else null,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("mcp-server-url")
            )

            Button(
                onClick = { onSave(McpServerItem(name = name.trim(), url = url.trim(), transport = transport, enabled = enabled)) },
                enabled = valid,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("mcp-server-save")
            ) { Text(tx("Save"), fontWeight = FontWeight.SemiBold) }
        }
    }
}
