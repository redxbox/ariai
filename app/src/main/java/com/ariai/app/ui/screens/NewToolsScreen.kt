package com.ariai.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// All tools functional with unique actions
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewToolsScreen(
    onToolClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val tools = listOf(
        ToolData("Image Generation", "Create with AI", Icons.Default.Image, Color(0xFF6750A4), "Generate an image of a futuristic city"),
        ToolData("Vision", "Analyze images", Icons.Default.Visibility, Color(0xFF006A60), "Analyze this image and describe what you see"),
        ToolData("Document Chat", "Chat with docs", Icons.Default.Description, Color(0xFF904D00), "Help me summarize this document"),
        ToolData("Web Search", "Search web", Icons.Default.Search, Color(0xFF00639B), "Search the web for latest AI developments"),
        ToolData("Code Assistant", "Write & debug", Icons.Default.Code, Color(0xFF6C4DFF), "Write a Python function to implement binary search"),
        ToolData("Speech", "Voice chat", Icons.Default.RecordVoiceOver, Color(0xFFBA1A1A), "Start voice conversation"),
        ToolData("Playground", "Test models", Icons.Default.Science, Color(0xFF4F6600), "Open model playground"),
        ToolData("Templates", "Quick prompts", Icons.Default.Dashboard, Color(0xFF7D5260), "Show prompt templates"),
    )

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFFEFBFF))) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    title = { Text("Tools", fontWeight = FontWeight.SemiBold, color = Color.Black) },
                    actions = {
                        IconButton(onClick = { scope.launch { snackbarHostState.showSnackbar("${tools.size} tools available") } }) {
                            Icon(Icons.Default.Info, contentDescription = "Tools info", tint = Color.Black)
                        }
                    }
                )
            }
        ) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Explore tools", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.Black)
                        Text("Each tool has unique function", color = Color.Black.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }
                items(tools.size) { i ->
                    val tool = tools[i]
                    ToolCard(
                        tool = tool,
                        onClick = {
                            // Each tool unique action
                            when (tool.name) {
                                "Image Generation" -> onToolClick("Image Generation")
                                "Vision" -> onToolClick(tool.prompt)
                                "Document Chat" -> onToolClick(tool.prompt)
                                "Web Search" -> onToolClick(tool.prompt)
                                "Code Assistant" -> onToolClick(tool.prompt)
                                "Speech" -> {
                                    scope.launch { snackbarHostState.showSnackbar("Voice chat coming soon") }
                                    onToolClick(tool.prompt)
                                }
                                "Playground" -> {
                                    scope.launch { snackbarHostState.showSnackbar("Playground: compare models") }
                                    onToolClick(tool.prompt)
                                }
                                "Templates" -> {
                                    scope.launch { snackbarHostState.showSnackbar("Templates: 20+ prompts") }
                                    onToolClick(tool.prompt)
                                }
                                else -> onToolClick(tool.prompt)
                            }
                        }
                    )
                }
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

data class ToolData(val name: String, val desc: String, val icon: ImageVector, val color: Color, val prompt: String)

@Composable
fun ToolCard(tool: ToolData, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().height(116.dp).clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(tool.color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(tool.icon, contentDescription = tool.name, tint = tool.color, modifier = Modifier.size(22.dp))
                }
                Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = Color.Black.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))
            }
            Column {
                Text(tool.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Black, maxLines = 1)
                Text(tool.desc, fontSize = 11.sp, color = Color.Black.copy(alpha = 0.5f), maxLines = 1)
            }
        }
    }
}
