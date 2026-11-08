package com.ariai.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Attachment
import com.ariai.app.data.models.AttachmentType
import com.ariai.app.ui.components.ReplyContent
import com.ariai.app.ui.theme.*
import com.ariai.app.util.tx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Accent: Color @Composable @ReadOnlyComposable get() = AriAccent
private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk
private val Muted: Color @Composable @ReadOnlyComposable get() = AriMuted

@Composable
internal fun PendingChip(attachment: Attachment, onRemove: () -> Unit) {
    val extractedText = attachment.textContent?.takeIf(String::isNotBlank)
    val preview = remember(extractedText) { extractedText?.let { attachmentTextPreview(it) } }
    val estimatedTokens = remember(extractedText) { extractedText?.let(::estimateAttachmentTextTokens) }
    Surface(shape = RoundedCornerShape(18.dp), color = AriCard, shadowElevation = 0.dp) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (attachment.type) {
                AttachmentType.IMAGE -> Icons.Default.Image
                AttachmentType.VIDEO -> Icons.Default.PlayArrow
                else -> Icons.Default.Description
            }
            Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Column(
                modifier = Modifier.widthIn(max = 190.dp).padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    attachment.name,
                    fontSize = 12.sp,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!preview.isNullOrBlank()) {
                    Text(
                        preview,
                        color = Muted,
                        fontSize = 10.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("attachment-text-preview")
                    )
                    Text(
                        "${tx("Approx.")} $estimatedTokens ${tx("tokens in context")}",
                        color = Muted,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("attachment-token-estimate")
                    )
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = tx("Remove attachment"), tint = Ink.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
internal fun CompressedNotice() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = AriInk.copy(alpha = 0.08f))
        Text(tx("Earlier messages compressed"), fontSize = 11.sp, color = Muted)
        HorizontalDivider(modifier = Modifier.weight(1f), color = AriInk.copy(alpha = 0.08f))
    }
}

/** Shows a sent photo inside the bubble, read from its stored file or from the in-memory bytes. */
@Composable
private fun AttachmentThumb(attachment: Attachment) {
    val bitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = attachment.uri,
        key2 = attachment.base64Data
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }
                val uri = attachment.uri
                if (uri != null && java.io.File(uri).exists()) {
                    android.graphics.BitmapFactory.decodeFile(uri, options)
                } else {
                    attachment.base64Data?.let {
                        val bytes = android.util.Base64.decode(it, android.util.Base64.DEFAULT)
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    }
                }
            }.getOrNull()?.asImageBitmap()
        }
    }
    val currentBitmap = bitmap
    if (currentBitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = currentBitmap,
            contentDescription = attachment.name,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.size(180.dp).clip(RoundedCornerShape(18.dp))
        )
    } else {
        Text(attachment.name, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun UserBubble(text: String, attachments: List<Attachment>, fontSize: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(28.dp, 28.dp, 9.dp, 28.dp),
            color = Accent.copy(alpha = 0.11f),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (text.isNotBlank()) {
                    SelectionContainer {
                        Text(text, color = Ink, fontSize = fontSize.sp, lineHeight = (fontSize + 7).sp)
                    }
                }
                attachments.filter { it.type == AttachmentType.IMAGE }.forEach { AttachmentThumb(it) }
                val others = attachments.filter { it.type != AttachmentType.IMAGE }
                if (others.isNotEmpty()) {
                    Text(
                        others.joinToString(" · ") { it.name },
                        color = Muted,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun BranchNavigator(
    messageId: String,
    branchMessageIds: List<String>,
    onNavigate: (String) -> Unit
) {
    val index = branchMessageIds.indexOf(messageId)
    if (index < 0 || branchMessageIds.size < 2) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.testTag("branch-navigation-$messageId")
    ) {
        IconButton(
            onClick = { onNavigate(branchMessageIds[index - 1]) },
            enabled = index > 0,
            modifier = Modifier.size(34.dp).semantics { contentDescription = tx("Previous branch") }
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = if (index > 0) Ink else Muted.copy(alpha = 0.35f), modifier = Modifier.size(17.dp))
        }
        Text(
            "${tx("Branch")} ${index + 1} / ${branchMessageIds.size}",
            color = Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        IconButton(
            onClick = { onNavigate(branchMessageIds[index + 1]) },
            enabled = index < branchMessageIds.lastIndex,
            modifier = Modifier.size(34.dp).semantics { contentDescription = tx("Next branch") }
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = if (index < branchMessageIds.lastIndex) Ink else Muted.copy(alpha = 0.35f), modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
internal fun AssistantBlock(
    text: String,
    fontSize: Int,
    streaming: Boolean = false,
    imagePath: String? = null,
    isError: Boolean = false,
    retryable: Boolean = false,
    onCopy: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onBranch: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth().then(if (streaming) Modifier else Modifier.animateContentSize()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (imagePath != null) {
            when {
                isVideoPath(imagePath) -> VideoCard(imagePath)
                isAudioPath(imagePath) -> AudioCard(imagePath)
                else -> GeneratedImageView(imagePath)
            }
        }
        // Models often answer with image links; show those as pictures instead of raw URLs.
        val imageUrls = remember(text) { extractImageUrls(text) }
        val visibleText = remember(text) { if (imageUrls.isEmpty()) text else stripImageUrls(text) }
        if (visibleText.isNotBlank()) {
            // SelectionContainer lets the user select and copy part of the reply.
            SelectionContainer {
                ReplyContent(visibleText, fontSize.toFloat(), Ink, streaming)
            }
        }
        imageUrls.forEach { RemoteImage(it) }
        if (imagePath != null && !isVideoPath(imagePath) && !isAudioPath(imagePath)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onEdit?.invoke() }) {
                    Text(tx("Edit"), color = Ink.copy(alpha = 0.7f), fontSize = 13.sp)
                }
                TextButton(onClick = { onSave?.invoke() }) {
                    Text(tx("Save"), color = Ink.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
        } else if (onCopy != null && isError) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionIcon(Icons.Default.ContentCopy, tx("Copy"), onCopy)
                if (retryable && onRetry != null) {
                    TextButton(
                        onClick = onRetry,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(tx("Retry"), fontSize = 13.sp)
                    }
                }
            }
        } else if (onCopy != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ActionIcon(Icons.Default.ContentCopy, tx("Copy"), onCopy)
                ActionIcon(Icons.Default.Share, tx("Share"), onShare ?: {})
                ActionIcon(Icons.Default.CallSplit, tx("Branch"), onBranch ?: {})
                if (retryable && onRetry != null) ActionIcon(Icons.Default.Refresh, tx("Retry"), onRetry)
            }
        }
    }
}

@Composable
private fun ActionIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(icon, contentDescription = description, tint = Muted, modifier = Modifier.size(16.dp))
    }
}
