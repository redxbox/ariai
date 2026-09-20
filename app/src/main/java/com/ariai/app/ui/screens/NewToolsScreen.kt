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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// RikkaHub inspired tools - clean minimal 2 columns not crowded
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewToolsScreen(
    onToolClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tools = listOf(
        ToolData("Image Generation", "Create images", Icons.Default.Image, Color(0xFF6750A4)),
        ToolData("Vision", "Analyze images", Icons.Default.Visibility, Color(0xFF006A60)),
        ToolData("Document Chat", "Chat with docs", Icons.Default.Description, Color(0xFF904D00)),
        ToolData("Web Search", "Search web", Icons.Default.Search, Color(0xFF00639B)),
        ToolData("Code Assistant", "Write code", Icons.Default.Code, Color(0xFF6C4DFF)),
        ToolData("Speech", "Voice chat", Icons.Default.RecordVoiceOver, Color(0xFFBA1A1A)),
        ToolData("Playground", "Test models", Icons.Default.Science, Color(0xFF4F6600)),
        ToolData("Templates", "Quick prompts", Icons.Default.Dashboard, Color(0xFF7D5260)),
    )

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFFEFBFF))) {
        Scaffold(containerColor = Color.Transparent) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tools", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.Black)
                        Text("Enhance your workflow", color = Color.Black.copy(alpha = 0.6f), fontSize = 14.sp)
                    }
                }
                items(tools.size) { i ->
                    val tool = tools[i]
                    CleanToolCard(tool = tool, onClick = { onToolClick(tool.name) })
                }
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

data class ToolData(val name: String, val desc: String, val icon: ImageVector, val color: Color)

@Composable
fun CleanToolCard(tool: ToolData, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().height(110.dp).clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tool.color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(tool.icon, contentDescription = null, tint = tool.color, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(tool.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Black, maxLines = 1)
                Text(tool.desc, fontSize = 11.sp, color = Color.Black.copy(alpha = 0.5f), maxLines = 1)
            }
        }
    }
}
