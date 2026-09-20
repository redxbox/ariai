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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ariai.app.util.VoiceManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceChatScreen(
    voiceManager: VoiceManager,
    hasPermission: Boolean,
    onPermissionRequest: () -> Unit,
    onBack: () -> Unit,
    onSendText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening by voiceManager.isListening.collectAsState()
    val recognizedText by voiceManager.recognizedText.collectAsState()
    val isSpeaking by voiceManager.isSpeaking.collectAsState()
    val isProcessing = false

    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening || isSpeaking) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Chat", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { voiceManager.stopSpeaking() }) {
                        Icon(
                            if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Speaker"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (!hasPermission) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.MicOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Text("Microphone permission required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Allow microphone access to use voice chat", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onPermissionRequest, shape = RoundedCornerShape(12.dp)) {
                            Text("Grant Permission")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale),
                contentAlignment = Alignment.Center
            ) {
                if (isListening || isSpeaking) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = when {
                                    isListening -> listOf(Color(0xFFFF3D57), Color(0xFFFF8A65))
                                    isSpeaking -> listOf(Color(0xFF00C853), Color(0xFF69F0AE))
                                    isProcessing -> listOf(Color(0xFFFFD600), Color(0xFFFFAB00))
                                    else -> listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isListening -> Icons.Default.Mic
                            isSpeaking -> Icons.Default.VolumeUp
                            isProcessing -> Icons.Default.HourglassEmpty
                            else -> Icons.Default.MicOff
                        },
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = when {
                        isListening -> "Listening..."
                        isSpeaking -> "Speaking..."
                        isProcessing -> "Processing..."
                        recognizedText.isNotEmpty() -> recognizedText
                        else -> "Tap to speak"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isListening) Color(0xFFFF3D57) else MaterialTheme.colorScheme.onSurface
                )

                if (recognizedText.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFFF3D57))
                            Text(recognizedText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { voiceManager.clearRecognizedText() },
                    modifier = Modifier.size(56.dp),
                    enabled = recognizedText.isNotEmpty()
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(24.dp))
                }

                FilledIconButton(
                    onClick = {
                        if (isListening) {
                            voiceManager.stopListening()
                        } else {
                            voiceManager.startListening()
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isListening) Color(0xFFFF3D57) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isListening) "Stop" else "Mic",
                        modifier = Modifier.size(36.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = {
                        if (recognizedText.isNotEmpty()) {
                            onSendText(recognizedText)
                            voiceManager.clearRecognizedText()
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    enabled = recognizedText.isNotEmpty()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(24.dp))
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Voice Tips", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("• Tap mic to start listening\n• Speak clearly in your language\n• Tap stop when done\n• Send to chat or continue conversation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
