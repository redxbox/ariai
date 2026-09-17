package com.ariai.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ariai.app.data.models.Chat
import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import com.ariai.app.data.models.Provider
import com.ariai.app.ui.components.MessageBubble
import com.ariai.app.util.LocalStrings
import com.ariai.app.util.VoiceManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ChatScreen(
    chat: Chat?,
    messages: List<ChatMessage>,
    providers: List<Provider> = emptyList(),
    isStreaming: Boolean,
    currentStreamingContent: String,
    selectedModel: String?,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit,
    onBranchMessage: (ChatMessage) -> Unit,
    onRegenerate: (ChatMessage) -> Unit,
    onCopyMessage: (String) -> Unit,
    onProviderChange: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Voice manager
    val voiceManager = remember { VoiceManager(context) }
    val isListening by voiceManager.isListening.collectAsState()
    val recognizedText by voiceManager.recognizedText.collectAsState()
    val isSpeaking by voiceManager.isSpeaking.collectAsState()
    
    var showProviderSheet by remember { mutableStateOf(false) }
    var isVoiceMode by remember { mutableStateOf(false) }

    // Handle recognized text
    LaunchedEffect(recognizedText) {
        if (recognizedText.isNotBlank()) {
            inputText = recognizedText
            voiceManager.clearRecognizedText()
        }
    }

    LaunchedEffect(messages.size, currentStreamingContent) {
        if (messages.isNotEmpty() || currentStreamingContent.isNotEmpty()) {
            scope.launch {
                try {
                    listState.animateScrollToItem(maxOf(0, messages.size))
                } catch (e: Exception) {}
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.destroy()
        }
    }

    Scaffold(
        topBar = {
            // Glassmorphism Top Bar
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            )
                        )
                    )
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = chat?.title ?: "New Chat",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                if (isStreaming) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00C853))
                                    )
                                }
                            }
                            if (selectedModel != null) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = selectedModel.take(25),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (chat?.providerId != null) {
                                        val provider = providers.find { it.id == chat.providerId }
                                        if (provider != null) {
                                            Text(
                                                "• ${provider.name}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Provider selector button - prominent
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                                            )
                                        )
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    "Provider",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        IconButton(onClick = { showProviderSheet = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Provider Settings")
                        }
                        IconButton(onClick = { 
                            if (isSpeaking) voiceManager.stopSpeaking()
                            else if (messages.lastOrNull()?.role == MessageRole.ASSISTANT) {
                                voiceManager.speak(messages.last().content)
                            }
                        }) {
                            Icon(
                                if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Speak",
                                tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        bottomBar = {
            // Glassmorphism Bottom Bar with blur effect
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                )
                            )
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Voice mode indicator
                        AnimatedVisibility(
                            visible = isListening,
                            enter = slideInVertically() + fadeIn(),
                            exit = slideOutVertically() + fadeOut()
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFF3D57).copy(alpha = 0.1f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF3D57)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text("Listening...", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFF3D57))
                                        Text("Speak now, I'm listening", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    // Pulsing animation
                                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                    val scale by infiniteTransition.animateFloat(
                                        initialValue = 1f,
                                        targetValue = 1.3f,
                                        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                                        label = "scale"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size((12 * scale).dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF3D57).copy(alpha = 0.5f))
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Voice button - glass style
                            Surface(
                                shape = CircleShape,
                                color = if (isListening) Color(0xFFFF3D57) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isListening) {
                                            voiceManager.stopListening()
                                        } else {
                                            voiceManager.startListening(
                                                when (strings.appName) {
                                                    "آریاای" -> "fa-IR"
                                                    "آريا آي" -> "ar-SA"
                                                    else -> "en-US"
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = "Voice",
                                        tint = if (isListening) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { 
                                    Text(
                                        if (isListening) "Listening..." else strings.typeMessage,
                                        color = if (isListening) Color(0xFFFF3D57) else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) 
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 5,
                                enabled = !isStreaming,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            )

                            // Send button - gradient and glass
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (inputText.isNotBlank() && !isStreaming) {
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFF6C4DFF), Color(0xFF00D4AA))
                                            )
                                        } else {
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                )
                                            )
                                        }
                                    )
                            ) {
                                IconButton(
                                    onClick = {
                                        if (inputText.isNotBlank()) {
                                            try {
                                                onSendMessage(inputText)
                                                inputText = ""
                                            } catch (e: Exception) {
                                                // Prevent crash
                                            }
                                        }
                                    },
                                    enabled = inputText.isNotBlank() && !isStreaming,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        if (isStreaming) Icons.Default.Stop else Icons.Default.Send,
                                        contentDescription = strings.send,
                                        tint = if (inputText.isNotBlank() && !isStreaming) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Quick actions - glass chips
                        AnimatedVisibility(visible = inputText.isEmpty() && messages.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("🌐 Search", "🎨 Image", "💻 Code", "✍️ Write").forEach { action ->
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable {
                                                inputText = when {
                                                    action.contains("Search") -> "Search latest AI news and summarize"
                                                    action.contains("Image") -> "Generate image of "
                                                    action.contains("Code") -> "Write a function to "
                                                    else -> "Write a story about "
                                                }
                                            }
                                    ) {
                                        Text(
                                            action,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (messages.isEmpty() && currentStreamingContent.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // Animated logo
                                val infiniteTransition = rememberInfiniteTransition(label = "logo")
                                val rotation by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
                                    label = "rotation"
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFF6C4DFF), Color(0xFF00D4AA))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "A",
                                        style = MaterialTheme.typography.displayMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Text(
                                    text = strings.welcomeTitle,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "✨ ${selectedModel ?: "AI"} ready",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Ask anything, search web, generate images, or use voice",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                SuggestionChips(
                                    onSuggestionClick = { suggestion ->
                                        inputText = suggestion
                                    }
                                )
                            }
                        }
                    }
                }

                items(messages, key = { it.id }) { message ->
                    // Add animation for message appearance
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                    ) {
                        MessageItem(
                            message = message,
                            onBranch = { 
                                try { onBranchMessage(message) } catch (e: Exception) {}
                            },
                            onRegenerate = { 
                                try { onRegenerate(message) } catch (e: Exception) {}
                            },
                            onCopy = { 
                                try { onCopyMessage(message.content) } catch (e: Exception) {}
                            },
                            onSpeak = { voiceManager.speak(message.content) }
                        )
                    }
                }

                if (currentStreamingContent.isNotEmpty()) {
                    item {
                        MessageBubble(
                            content = currentStreamingContent,
                            isUser = false,
                            modelName = selectedModel
                        )
                        if (isStreaming) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text(
                                    "Generating...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                // Typing dots animation
                                val infiniteTransition = rememberInfiniteTransition(label = "dots")
                                val dots by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 3f,
                                    animationSpec = infiniteRepeatable(tween(900)),
                                    label = "dots"
                                )
                                Text(
                                    ".".repeat(dots.toInt() + 1),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Glassmorphism provider selector sheet
            if (showProviderSheet) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { showProviderSheet = false },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.7f)
                            .clickable(enabled = false) {},
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Handle
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                    .align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Select Provider & Model", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { showProviderSheet = false }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(providers) { provider ->
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (provider.id == chat?.providerId) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val model = provider.models.firstOrNull()?.id ?: ""
                                                onProviderChange(provider.id, model)
                                                showProviderSheet = false
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        Brush.linearGradient(
                                                            colors = listOf(
                                                                MaterialTheme.colorScheme.primary,
                                                                MaterialTheme.colorScheme.tertiary
                                                            )
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(provider.name.first().toString(), color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(provider.name, fontWeight = FontWeight.Bold)
                                                Text("${provider.models.size} models", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            if (provider.id == chat?.providerId) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                                // Model selector for current provider
                                if (chat?.providerId != null) {
                                    val currentProvider = providers.find { it.id == chat.providerId }
                                    if (currentProvider != null && currentProvider.models.isNotEmpty()) {
                                        item {
                                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                                            Text("Models for ${currentProvider.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        }
                                        items(currentProvider.models) { model ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (model.id == chat.modelId) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        onProviderChange(currentProvider.id, model.id)
                                                        showProviderSheet = false
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column {
                                                        Text(model.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                                        Text(model.id, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    if (model.id == chat.modelId) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
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
    onSpeak: () -> Unit = {},
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
                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onCopy)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = strings.copy, modifier = Modifier.size(14.dp))
                        Text(strings.copy, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onBranch)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Share, contentDescription = strings.branch, modifier = Modifier.size(14.dp))
                        Text(strings.branch, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onRegenerate)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = strings.regenerate, modifier = Modifier.size(14.dp))
                        Text(strings.regenerate, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onSpeak)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Speak", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Speak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val suggestions = listOf(
            "Explain quantum computing simply",
            "Write a Python function to sort list",
            "Search latest AI news",
            "Generate image of futuristic city"
        )
        suggestions.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { suggestion ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onSuggestionClick(suggestion) }
                    ) {
                        Text(
                            suggestion,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
