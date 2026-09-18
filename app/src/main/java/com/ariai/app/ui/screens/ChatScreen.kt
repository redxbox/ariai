package com.ariai.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ariai.app.data.models.Chat
import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import com.ariai.app.data.models.Provider
import com.ariai.app.ui.components.MessageBubble
import com.ariai.app.util.LocalStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chat: Chat?,
    messages: List<ChatMessage>,
    isStreaming: Boolean,
    currentStreamingContent: String,
    selectedModel: String?,
    providers: List<Provider> = emptyList(),
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit,
    onBranchMessage: (ChatMessage) -> Unit,
    onRegenerate: (ChatMessage) -> Unit,
    onCopyMessage: (String) -> Unit,
    onProviderChange: (String, String) -> Unit = { _, _ -> },
    onVoiceClick: () -> Unit = {},
    voiceManager: com.ariai.app.util.VoiceManager? = null,
    hasAudioPermission: Boolean = false,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showProviders by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size, currentStreamingContent) {
        if (messages.isNotEmpty() || currentStreamingContent.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(maxOf(0, messages.size - 1))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = chat?.title ?: "New Chat",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        if (selectedModel != null) {
                            Text(
                                text = selectedModel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showProviders = !showProviders }) {
                        Icon(Icons.Default.Settings, contentDescription = "Provider")
                    }
                    IconButton(onClick = onVoiceClick) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = androidx.compose.ui.Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text(strings.typeMessage) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 5,
                        enabled = !isStreaming
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && !isStreaming
                    ) {
                        Icon(
                            if (isStreaming) Icons.Default.Stop else Icons.Default.Send,
                            contentDescription = strings.send
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showProviders) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Providers:", style = MaterialTheme.typography.titleSmall)
                    providers.forEach { provider ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val model = provider.models.firstOrNull()?.id ?: provider.model
                                    onProviderChange(provider.id, model)
                                    showProviders = false
                                },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(provider.name, style = MaterialTheme.typography.titleMedium)
                                Text(provider.baseUrl.take(40), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (messages.isEmpty() && currentStreamingContent.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 100.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "🤖",
                                    style = MaterialTheme.typography.displayLarge
                                )
                                Text(
                                    text = strings.welcomeTitle,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
                }

                items(messages, key = { it.id }) { message ->
                    MessageItem(
                        message = message,
                        onBranch = { onBranchMessage(message) },
                        onRegenerate = { onRegenerate(message) },
                        onCopy = { onCopyMessage(message.content) }
                    )
                }

                if (currentStreamingContent.isNotEmpty()) {
                    item {
                        MessageBubble(
                            content = currentStreamingContent,
                            isUser = false,
                            modelName = selectedModel
                        )
                        if (isStreaming) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageItem(
    message: ChatMessage,
    onBranch: () -> Unit,
    onRegenerate: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Column(modifier = modifier.fillMaxWidth()) {
        MessageBubble(
            content = message.content,
            isUser = message.role == MessageRole.USER,
            modelName = message.modelId,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (message.role == MessageRole.ASSISTANT) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = strings.copy,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onBranch,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = strings.branch,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onRegenerate,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = strings.regenerate,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestionChips(
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        val suggestions = listOf(
            "Explain quantum computing simply",
            "Write a Python function"
        )
        suggestions.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { onSuggestionClick(suggestion) },
                        label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    }
}
