package com.ariai.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ariai.app.data.models.Agent
import com.ariai.app.util.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentListScreen(
    agents: List<Agent>,
    onAgentClick: (Agent) -> Unit,
    onAddAgent: () -> Unit,
    onDeleteAgent: (Agent) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(strings.agents) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAgent) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                count = agents.size,
                key = { index -> "${agents[index].id}_${agents[index].hashCode()}_$index" }
            ) { index ->
                val agent = agents[index]
                AgentCard(
                    agent = agent,
                    onClick = { onAgentClick(agent) },
                    onDelete = { onDeleteAgent(agent) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentCard(
    agent: Agent,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = agent.avatar,
                    style = MaterialTheme.typography.headlineMedium
                )
                if (!agent.isBuiltIn) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
            Text(
                text = agent.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = agent.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (agent.tools.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    agent.tools.take(3).forEach { tool ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = tool.name.take(4),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAgentScreen(
    onSave: (Agent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var systemPrompt by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf("🤖") }
    var temperature by remember { mutableStateOf(0.7f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Agent") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (name.isNotBlank() && systemPrompt.isNotBlank()) {
                                onSave(
                                    Agent(
                                        name = name,
                                        description = description,
                                        systemPrompt = systemPrompt,
                                        avatar = avatar,
                                        temperature = temperature
                                    )
                                )
                            }
                        },
                        enabled = name.isNotBlank() && systemPrompt.isNotBlank()
                    ) {
                        Text(strings.save)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("🤖", "💻", "✍️", "🌐", "🔍", "🎨", "📚", "🧠").forEach { emoji ->
                        FilterChip(
                            selected = avatar == emoji,
                            onClick = { avatar = emoji },
                            label = { Text(emoji) }
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Agent Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text(strings.systemPrompt) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    placeholder = { Text("You are a helpful assistant... Use {time} {model} variables") }
                )
            }
            item {
                Column {
                    Text("Temperature: ${"%.1f".format(temperature)}")
                    Slider(
                        value = temperature,
                        onValueChange = { temperature = it },
                        valueRange = 0f..2f,
                        steps = 19
                    )
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Prompt Variables", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• {time} - Current time", style = MaterialTheme.typography.bodySmall)
                        Text("• {model} - Current model name", style = MaterialTheme.typography.bodySmall)
                        Text("• {date} - Current date", style = MaterialTheme.typography.bodySmall)
                        Text("• {user} - User name", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
