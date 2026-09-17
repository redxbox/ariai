package com.ariai.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ariai.app.util.LocalStrings
import com.ariai.app.util.VoiceManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceChatScreen(
    onBack: () -> Unit,
    onSendVoiceMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val voiceManager = remember { VoiceManager(context) }
    
    val isListening by voiceManager.isListening.collectAsState()
    val recognizedText by voiceManager.recognizedText.collectAsState()
    val isSpeaking by voiceManager.isSpeaking.collectAsState()
    
    var lastResponse by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(recognizedText) {
        if (recognizedText.isNotBlank() && !isListening) {
            isProcessing = true
            onSendVoiceMessage(recognizedText)
            // Simulate response delay
            kotlinx.coroutines.delay(1000)
            isProcessing = false
        }
    }

    DisposableEffect(Unit) {
        onDispose { voiceManager.destroy() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Conversation", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { if (isSpeaking) voiceManager.stopSpeaking() }) {
                        Icon(
                            if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                // Animated voice orb
                Box(contentAlignment = Alignment.Center) {
                    // Outer pulse
                    if (isListening || isSpeaking) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.5f,
                            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                            label = "scale"
                        )
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                            label = "alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size((160 * scale).dp)
                                .clip(CircleShape)
                                .background(
                                    if (isListening) Color(0xFFFF3D57).copy(alpha = alpha)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                                )
                        )
                    }
                    
                    // Main orb - glassmorphism
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = when {
                                        isListening -> listOf(Color(0xFFFF3D57), Color(0xFFFF8A80))
                                        isSpeaking -> listOf(Color(0xFF6C4DFF), Color(0xFF00D4AA))
                                        isProcessing -> listOf(Color(0xFFFFD600), Color(0xFFFFAB00))
                                        else -> listOf(Color(0xFF6C4DFF), Color(0xFF00D4AA))
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when {
                                isListening -> Icons.Default.Mic
                                isSpeaking -> Icons.Default.VolumeUp
                                isProcessing -> Icons.Default.HourglassEmpty
                                else -> Icons.Default.MicOff
                            },
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Color.White
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = when {
                            isListening -> "Listening..."
                            isSpeaking -> "Speaking..."
                            isProcessing -> "Thinking..."
                            else -> "Tap to speak"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            isListening -> "Speak now, I'm listening to you"
                            isSpeaking -> "Playing response"
                            isProcessing -> "Processing your voice"
                            else -> "Have a natural conversation with AI"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Recognized text card - glass
                if (recognizedText.isNotBlank() || lastResponse.isNotBlank()) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (recognizedText.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(shape = CircleShape, color = Color(0xFFFF3D57).copy(alpha = 0.15f), modifier = Modifier.size(32.dp)) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFFF3D57))
                                        }
                                    }
                                    Text("You", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(recognizedText, style = MaterialTheme.typography.bodyMedium)
                                if (lastResponse.isNotBlank()) Divider(modifier = Modifier.padding(vertical = 12.dp))
                            }
                            if (lastResponse.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), modifier = Modifier.size(32.dp)) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Text("A", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Text("AriAi", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(lastResponse, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // Controls - glass buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stop button
                    if (isListening || isSpeaking) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(56.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (isListening) voiceManager.stopListening()
                                    if (isSpeaking) voiceManager.stopSpeaking()
                                },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    // Main mic button - premium glass
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                if (isListening) Brush.linearGradient(listOf(Color(0xFFFF3D57), Color(0xFFFF6B6B)))
                                else Brush.linearGradient(listOf(Color(0xFF6C4DFF), Color(0xFF00D4AA)))
                            )
                    ) {
                        IconButton(
                            onClick = {
                                if (isListening) {
                                    voiceManager.stopListening()
                                } else {
                                    voiceManager.startListening("en-US")
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Mic",
                                modifier = Modifier.size(32.dp),
                                tint = Color.White
                            )
                        }
                    }

                    // Clear button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(56.dp)
                    ) {
                        IconButton(
                            onClick = {
                                voiceManager.clearRecognizedText()
                                lastResponse = ""
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(24.dp))
                        }
                    }
                }

                // Tips - glass
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("💡 Voice Tips", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• Tap mic and speak naturally", style = MaterialTheme.typography.bodySmall)
                        Text("• Works offline for speech recognition", style = MaterialTheme.typography.bodySmall)
                        Text("• AI will speak back automatically", style = MaterialTheme.typography.bodySmall)
                        Text("• Try: 'Search latest news' or 'Generate image'", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
