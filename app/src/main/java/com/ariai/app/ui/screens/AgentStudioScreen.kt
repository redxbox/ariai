package com.ariai.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Agent
import com.ariai.app.data.models.AgentTool
import com.ariai.app.data.models.Provider
import com.ariai.app.ui.theme.AriAccent
import com.ariai.app.ui.theme.AriCard
import com.ariai.app.ui.theme.AriInk
import com.ariai.app.ui.theme.AriMuted
import com.ariai.app.ui.theme.AriPaper
import com.ariai.app.ui.theme.AriTint
import com.ariai.app.ui.theme.paperPopupModifier
import com.ariai.app.ui.theme.paperPopupShape
import com.ariai.app.util.tx
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentStudioScreen(
    agents: List<Agent>,
    providers: List<Provider>,
    onBack: () -> Unit,
    onSaveAgent: (Agent) -> Unit,
    onDeleteAgent: (String) -> Unit,
    onStartAgent: (Agent) -> Unit
) {
    var editingAgent by remember { mutableStateOf<Agent?>(null) }
    fun newAgentDraft(): Agent = Agent(
        id = UUID.randomUUID().toString(),
        name = "",
        description = "",
        systemPrompt = "",
        avatar = "✦",
        isBuiltIn = false
    )
    val builtInAgents = agents.filter { it.isBuiltIn }
    val customAgents = agents.filterNot { it.isBuiltIn }

    Scaffold(
        containerColor = AriPaper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AriPaper),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = tx("Back"), tint = AriInk)
                    }
                },
                title = { Text(tx("Agents & prompts"), color = AriInk, fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { editingAgent = newAgentDraft() }) {
                        Icon(Icons.Default.Add, contentDescription = tx("Create agent"), tint = AriInk)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    tx("Shape a specialist's instructions, model, and available tools. Agent settings stay on this device."),
                    color = AriMuted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            if (builtInAgents.isNotEmpty()) {
                item { SectionLabel(tx("Built-in agents")) }
                items(builtInAgents, key = { "builtin:${it.id}" }) { agent ->
                    AgentCard(agent, onStart = { onStartAgent(agent) }, onEdit = { editingAgent = agent })
                }
            }
            if (customAgents.isNotEmpty()) {
                item { SectionLabel(tx("Your agents")) }
                items(customAgents, key = { "custom:${it.id}" }) { agent ->
                    AgentCard(agent, onStart = { onStartAgent(agent) }, onEdit = { editingAgent = agent })
                }
            }
            if (agents.isEmpty()) {
                item {
                    Text(tx("Your agents are loading…"), color = AriMuted, modifier = Modifier.padding(12.dp))
                }
            }
            item {
                Button(
                    onClick = { editingAgent = newAgentDraft() },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AriInk, contentColor = AriPaper)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tx("Create agent"), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    editingAgent?.let { agent ->
        AgentEditorSheet(
            agent = agent,
            providers = providers,
            onDismiss = { editingAgent = null },
            onSave = { saved ->
                onSaveAgent(saved)
                editingAgent = null
            },
            onDelete = { id ->
                onDeleteAgent(id)
                editingAgent = null
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = AriMuted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.padding(start = 2.dp, top = 8.dp))
}

@Composable
private fun AgentCard(agent: Agent, onStart: () -> Unit, onEdit: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AriCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(AriTint), contentAlignment = Alignment.Center) {
                    Text(agent.avatar.ifBlank { "✦" }.take(2), fontSize = 21.sp, color = AriInk)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(agent.name, color = AriInk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (agent.isBuiltIn) AssistChip(onClick = {}, label = { Text(tx("Built-in"), fontSize = 10.sp) })
                    }
                    Text(agent.description.ifBlank { tx("Custom instructions") }, color = AriMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = tx("Edit agent"), tint = AriMuted)
                }
            }
            val toolSummary = agent.tools.mapNotNull { tool ->
                when (tool) {
                    AgentTool.CALCULATOR -> tx("Calculator")
                    AgentTool.WEB_SEARCH -> tx("Web search")
                    AgentTool.IMAGE_GENERATION -> tx("Image mode")
                    AgentTool.MCP -> null
                    AgentTool.CODE_EXECUTION -> null
                    AgentTool.MEMORY -> null
                }
            }
            if (toolSummary.isNotEmpty()) {
                Text(toolSummary.joinToString(" · "), color = AriMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AriInk, contentColor = AriPaper)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(tx("Start chat"))
                }
                OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(13.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(tx("Edit"))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentEditorSheet(
    agent: Agent,
    providers: List<Provider>,
    onDismiss: () -> Unit,
    onSave: (Agent) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember(agent.id) { mutableStateOf(agent.name) }
    var description by remember(agent.id) { mutableStateOf(agent.description) }
    var prompt by remember(agent.id) { mutableStateOf(agent.systemPrompt) }
    var avatar by remember(agent.id) { mutableStateOf(agent.avatar) }
    var providerId by remember(agent.id) { mutableStateOf(agent.providerId) }
    var modelId by remember(agent.id) { mutableStateOf(agent.modelId) }
    var temperature by remember(agent.id) { mutableFloatStateOf(agent.temperature.coerceIn(0f, 2f)) }
    var topP by remember(agent.id) { mutableFloatStateOf(agent.topP.coerceIn(0f, 1f)) }
    var variables by remember(agent.id) { mutableStateOf(agent.promptVariables.entries.joinToString("\n") { "${it.key}=${it.value}" }) }
    var examples by remember(agent.id) { mutableStateOf(agent.examples.joinToString("\n")) }
    var selectedTools by remember(agent.id) { mutableStateOf(agent.tools.toSet()) }
    var providerMenu by remember { mutableStateOf(false) }
    var modelMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val enabledProviders = providers.filter { it.enabled }
    val selectedProvider = enabledProviders.firstOrNull { it.id == providerId }
    val modelChoices = if (selectedProvider == null) enabledProviders.flatMap { provider -> provider.models.map { model -> provider to model } }
        else selectedProvider.models.map { selectedProvider to it }
    val selectedModel = modelChoices.firstOrNull { it.first.id == providerId && it.second.id == modelId }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = AriCard, contentColor = AriInk) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (agent.isBuiltIn) tx("Customize agent") else tx("Agent configuration"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(tx("System instructions"), fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(48) },
                label = { Text(tx("Agent name")) },
                leadingIcon = { Text(avatar.ifBlank { "✦" }, modifier = Modifier.clickable { avatar = if (avatar == "✦") "✧" else "✦" }) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(140) },
                label = { Text(tx("Description")) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 3
            )
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it.take(12_000) },
                label = { Text(tx("System prompt")) },
                placeholder = { Text(tx("Describe the role, style, and boundaries for this agent.")) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                maxLines = 10
            )
            Text(tx("Variables: {{time}}, {{date}}, {{model_name}} and your custom key=value entries."), color = AriMuted, fontSize = 12.sp)
            OutlinedTextField(
                value = variables,
                onValueChange = { variables = it.take(4_000) },
                label = { Text(tx("Prompt variables (one key=value per line)")) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5
            )
            OutlinedTextField(
                value = examples,
                onValueChange = { examples = it.take(4_000) },
                label = { Text(tx("Example prompts (one per line)")) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Text(tx("Model binding"), fontWeight = FontWeight.Medium)
            Box {
                OutlinedButton(onClick = { providerMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedProvider?.name ?: tx("Use default provider"), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = providerMenu, onDismissRequest = { providerMenu = false }) {
                    DropdownMenuItem(text = { Text(tx("Use default provider")) }, onClick = { providerId = null; modelId = null; providerMenu = false })
                    enabledProviders.forEach { provider ->
                        DropdownMenuItem(text = { Text(provider.name) }, onClick = { providerId = provider.id; modelId = null; providerMenu = false })
                    }
                }
            }
            Box {
                OutlinedButton(onClick = { modelMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedModel?.second?.displayName ?: tx("Use default model"), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                    DropdownMenuItem(text = { Text(tx("Use default model")) }, onClick = { modelId = null; modelMenu = false })
                    modelChoices.forEach { (provider, model) ->
                        DropdownMenuItem(
                            text = { Text("${model.displayName} · ${provider.name}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            onClick = { providerId = provider.id; modelId = model.id; modelMenu = false }
                        )
                    }
                }
            }

            Text(tx("Generation"), fontWeight = FontWeight.Medium)
            AgentSlider(tx("Temperature"), temperature, 0f..2f, 19) { temperature = it }
            AgentSlider(tx("Top-P"), topP, 0f..1f, 19) { topP = it }

            Text(tx("Available tools"), fontWeight = FontWeight.Medium)
            AgentToolSwitch(tx("Calculator"), tx("Safe arithmetic only; no arbitrary code is executed."), AgentTool.CALCULATOR in selectedTools, true) {
                selectedTools = selectedTools.toggle(AgentTool.CALCULATOR)
            }
            AgentToolSwitch(tx("Web search"), tx("Uses the search service configured in Settings."), AgentTool.WEB_SEARCH in selectedTools, true) {
                selectedTools = selectedTools.toggle(AgentTool.WEB_SEARCH)
            }
            HorizontalDivider(color = AriInk.copy(alpha = 0.08f))
            Text(tx("Not available yet"), color = AriMuted, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            AgentToolSwitch(tx("MCP tools"), tx("MCP server connections are still a preview."), AgentTool.MCP in selectedTools, false) { }
            AgentToolSwitch(tx("Code execution"), tx("Arbitrary terminal execution is not enabled in this app."), AgentTool.CODE_EXECUTION in selectedTools, false) { }
            AgentToolSwitch(tx("Persistent memory"), tx("Memory controls are not implemented yet."), AgentTool.MEMORY in selectedTools, false) { }
            AgentToolSwitch(tx("Image generation"), tx("Use Image mode from the chat composer; it is not a callable agent tool."), AgentTool.IMAGE_GENERATION in selectedTools, false) { }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!agent.isBuiltIn) {
                    TextButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(tx("Delete"))
                    }
                }
                Button(
                    onClick = {
                        val vars = variables.lineSequence().mapNotNull { line ->
                            val split = line.indexOf('=')
                            if (split <= 0) null else line.substring(0, split).trim().takeIf { it.isNotBlank() }?.let { key ->
                                key to line.substring(split + 1).trim()
                            }
                        }.toMap()
                        onSave(
                            agent.copy(
                                name = name.trim(),
                                description = description.trim(),
                                systemPrompt = prompt.trim(),
                                avatar = avatar.ifBlank { "✦" }.take(4),
                                providerId = providerId,
                                modelId = modelId,
                                temperature = temperature.coerceIn(0f, 2f),
                                topP = topP.coerceIn(0f, 1f),
                                tools = selectedTools.toList(),
                                promptVariables = vars,
                                examples = examples.lines().map(String::trim).filter(String::isNotBlank).take(20)
                            )
                        )
                    },
                    enabled = name.isNotBlank() && prompt.isNotBlank(),
                    modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(containerColor = AriInk, contentColor = AriPaper)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(tx("Save agent"))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    if (confirmDelete) {
        val dialogShape = paperPopupShape(24.dp)
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            modifier = paperPopupModifier(dialogShape),
            shape = dialogShape,
            containerColor = AriCard,
            title = { Text(tx("Delete agent?")) },
            text = { Text(tx("This removes the agent from this device. Existing chats are kept.")) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(agent.id) }) { Text(tx("Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(tx("Cancel")) }
            }
        )
    }
}

@Composable
private fun AgentSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = AriInk, fontWeight = FontWeight.Medium)
            Text(String.format(java.util.Locale.ROOT, "%.2f", value), color = AriMuted, fontSize = 12.sp)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@Composable
private fun AgentToolSwitch(label: String, description: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = if (enabled) AriInk else AriMuted, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(description, color = AriMuted, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = AriAccent)
        )
    }
}

private fun Set<AgentTool>.toggle(tool: AgentTool): Set<AgentTool> =
    if (tool in this) this - tool else this + tool
