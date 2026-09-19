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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewToolsScreen(
    onBack: () -> Unit = {},
    onToolClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().background(Color(0xFFF5F7FF))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Ari", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
                                Text("AI", color = Color(0xFF6C4DFF), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
                            }
                            Text("Smarter Tools. Real Results.", color = Color.Black.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    actions = {
                        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Tools", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("Turn your ideas into reality.", style = MaterialTheme.typography.bodySmall, color = Color.Black.copy(alpha = 0.6f))
                        }
                        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 1.dp) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("All Tools", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Grid of tools
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolCardLight(title = "Summarize", desc = "Turn long content\ninto clear summaries.", icon = "📄", bg = Color(0xFFE8E0FF), modifier = Modifier.weight(1f), onClick = { onToolClick("Summarize") })
                            ToolCardLight(title = "Write", desc = "Create articles, emails,\nideas and more.", icon = "✏️", bg = Color(0xFFD6E4FF), modifier = Modifier.weight(1f), onClick = { onToolClick("Write") })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolCardLight(title = "Code", desc = "Build, debug,\nexplain code.", icon = "</>", bg = Color(0xFFD1F5E0), modifier = Modifier.weight(1f), onClick = { onToolClick("Code") })
                            ToolCardLight(title = "Generate Image", desc = "Create images from\ntext descriptions.", icon = "🖼️", bg = Color(0xFFFFD6E0), modifier = Modifier.weight(1f), onClick = { onToolClick("Generate Image") })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolCardLight(title = "Analyze File", desc = "Understand PDFs,\nDocs, spreadsheets.", icon = "📁", bg = Color(0xFFFFE8B5), modifier = Modifier.weight(1f), onClick = { onToolClick("Analyze File") })
                            ToolCardLight(title = "Web Search", desc = "Get real-time\ninformation from the web.", icon = "🌐", bg = Color(0xFFD6D6FF), modifier = Modifier.weight(1f), onClick = { onToolClick("Web Search") })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolCardLight(title = "Voice", desc = "Speak, transcribe,\nand get answers.", icon = "🎤", bg = Color(0xFFC5F0E0), modifier = Modifier.weight(1f), onClick = { onToolClick("Voice") })
                            ToolCardLight(title = "Analyze Video", desc = "Understand and\nsummarize videos.", icon = "🎬", bg = Color(0xFFFFD6F0), modifier = Modifier.weight(1f), onClick = { onToolClick("Analyze Video") })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolCardLight(title = "Translate", desc = "Translate text\nbetween languages.", icon = "A文", bg = Color(0xFFD6E8FF), modifier = Modifier.weight(1f), onClick = { onToolClick("Translate") })
                            ToolCardLight(title = "Analyze Data", desc = "Find insights in\nyour data.", icon = "📊", bg = Color(0xFFFFD6C0), modifier = Modifier.weight(1f), onClick = { onToolClick("Analyze Data") })
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8E0FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier.size(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✨", style = MaterialTheme.typography.titleLarge)
                                }
                                Column {
                                    Text("More tools coming soon", fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text("We're constantly adding new capabilities.", style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(alpha = 0.6f))
                                }
                            }
                            Button(
                                onClick = {},
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4DFF))
                            ) {
                                Text("Suggest a tool", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun ToolCardLight(title: String, desc: String, icon: String, bg: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                Surface(shape = RoundedCornerShape(12.dp), color = bg, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(icon, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, color = Color.Black, style = MaterialTheme.typography.titleSmall)
                    Text(desc, style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(alpha = 0.6f))
                }
            }
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFF0F0FF), modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Black.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
