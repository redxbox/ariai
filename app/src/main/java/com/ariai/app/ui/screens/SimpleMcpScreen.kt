package com.ariai.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleMcpScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var mcpServers by remember { mutableStateOf(listOf("Local Tools", "File System", "Web Search")) }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFFEFBFF))) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                        }
                    },
                    title = { Text("MCP Servers", fontWeight = FontWeight.SemiBold, color = Color.Black) },
                    actions = {
                        IconButton(onClick = { scope.launch { snackbarHostState.showSnackbar("Add MCP server") } }) {
                            Icon(Icons.Default.Add, contentDescription = "Add MCP", tint = Color.Black)
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { scope.launch { snackbarHostState.showSnackbar("Add MCP server coming soon") } },
                    containerColor = Color(0xFF6C4DFF),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Server")
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Model Context Protocol", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
                            Text("MCP lets AI access local tools, files, and services securely.", fontSize = 12.sp, color = Color.Black.copy(alpha = 0.6f))
                        }
                    }
                }

                if (mcpServers.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Black.copy(alpha = 0.3f), modifier = Modifier.size(32.dp))
                                Text("No MCP servers", color = Color.Black.copy(alpha = 0.5f), fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    items(count = mcpServers.size, key = { i -> mcpServers[i] }) { i ->
                        val server = mcpServers[i]
                        var enabled by remember { mutableStateOf(i == 0) }
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth().clickable { scope.launch { snackbarHostState.showSnackbar("$server details") } }
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(36.dp).background(Color(0xFFF2F2F7), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xFF6C4DFF), modifier = Modifier.size(18.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(server, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.Black)
                                    Text(if (enabled) "Connected" else "Disabled", fontSize = 11.sp, color = if (enabled) Color(0xFF4CAF50) else Color.Black.copy(alpha = 0.5f))
                                }
                                Switch(checked = enabled, onCheckedChange = { enabled = it }, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF6C4DFF)))
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
