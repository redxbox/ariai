package com.ariai.app.ui.screens

import com.ariai.app.util.tx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.ariai.app.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Accent: Color @Composable @ReadOnlyComposable get() = AriInk
private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk
enum class McpTransport(val label: String) { SSE("SSE"), STREAMABLE_HTTP("Streamable HTTP") }

data class McpServerItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val transport: McpTransport,
    val enabled: Boolean = true
)

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
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = tx("Back"), tint = Ink) } },
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
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = AriCard, shadowElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(tx("Model Context Protocol"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink)
                        Text(tx("Connect MCP servers so the model can call external tools. Only enabled servers are used."), fontSize = 12.sp, color = Ink.copy(alpha = 0.6f))
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

            itemsIndexed(servers, key = { i, s -> "${s.id}_$i" }) { _, server ->
                Surface(shape = RoundedCornerShape(18.dp), color = AriCard, shadowElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(server.name, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Ink)
                            Text(server.transport.label, fontSize = 11.sp, color = Ink.copy(alpha = 0.5f))
                            Text(server.url, fontSize = 11.sp, color = Ink.copy(alpha = 0.4f), maxLines = 1)
                            Text(if (server.enabled) tx("Connected") else tx("Disabled"), fontSize = 11.sp, color = if (server.enabled) AriInk else Ink.copy(alpha = 0.45f))
                        }
                        IconButton(onClick = { onRemove(server.id) }) { Icon(Icons.Default.Delete, contentDescription = tx("Remove server"), tint = Ink.copy(alpha = 0.45f)) }
                        Switch(checked = server.enabled, onCheckedChange = { onToggle(server.id) }, colors = SwitchDefaults.colors(checkedTrackColor = Accent))
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
    val urlValid = url.startsWith("http://") || url.startsWith("https://")
    val valid = name.isNotBlank() && urlValid

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AriPaper) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(tx("Add MCP server"), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Ink)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(tx("Enable"), fontWeight = FontWeight.Medium, color = Ink)
                    Text(tx("Use this server in chats"), fontSize = 12.sp, color = Ink.copy(alpha = 0.5f))
                }
                Switch(checked = enabled, onCheckedChange = { enabled = it }, colors = SwitchDefaults.colors(checkedTrackColor = Accent))
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(tx("Name")) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text(tx("Transport"), fontWeight = FontWeight.Medium, color = Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                McpTransport.values().forEach { t ->
                    FilterChip(selected = transport == t, onClick = { transport = t }, label = { Text(t.label) })
                }
            }

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(tx("Server URL")) },
                placeholder = { Text("https://") },
                singleLine = true,
                isError = url.isNotBlank() && !urlValid,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onSave(McpServerItem(name = name.trim(), url = url.trim(), transport = transport, enabled = enabled)) },
                enabled = valid,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text(tx("Save"), fontWeight = FontWeight.SemiBold) }
        }
    }
}
