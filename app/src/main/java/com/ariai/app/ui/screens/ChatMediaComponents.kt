package com.ariai.app.ui.screens

import android.graphics.Bitmap
import com.ariai.app.util.decodeSampledBitmapFromBytes
import com.ariai.app.util.decodeSampledBitmapFromPath
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.remote.PublicUrlFetcher
import com.ariai.app.ui.theme.*
import com.ariai.app.util.tx
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val remoteImageFetcher by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { PublicUrlFetcher() }

/** Shows a generated image. Decoding runs off the main thread and is downsampled to save memory. */
@Composable
internal fun GeneratedImageView(path: String) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = decodeSampledBitmapFromPath(path, maxDimension = 1024)?.asImageBitmap()
    }
    var full by remember { mutableStateOf(false) }
    bitmap?.let { bmp ->
        Image(
            bitmap = bmp,
            contentDescription = tx("Generated image"),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable { full = true }
        )
    }
    if (full) bitmap?.let { FullImageDialog(it) { full = false } }
}

/** Full-screen view of an image; tap anywhere to close. */
@Composable
private fun FullImageDialog(bitmap: ImageBitmap, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black).clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
internal fun EditImageDialog(
    source: ChatMessage,
    onDismiss: () -> Unit,
    onConfirm: (String, File?) -> Unit
) {
    var text by remember(source.id) { mutableStateOf("") }
    var strokes by remember(source.id) { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var activeStroke by remember(source.id) { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember(source.id) { mutableStateOf(IntSize.Zero) }
    var brushWidthDp by remember(source.id) { mutableStateOf(24f) }
    var preparingMask by remember(source.id) { mutableStateOf(false) }
    var maskError by remember(source.id) { mutableStateOf<String?>(null) }
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val sourceBitmap by produceState<Bitmap?>(initialValue = null, key1 = source.imagePath) {
        value = source.imagePath?.let { decodeSampledBitmapFromPath(it, maxDimension = 1024) }
    }
    val preview = sourceBitmap?.asImageBitmap()
    DisposableEffect(sourceBitmap) {
        onDispose { sourceBitmap?.recycle() }
    }
    val dialogShape = paperPopupShape(26.dp)
    AlertDialog(
        onDismissRequest = { if (!preparingMask) onDismiss() },
        modifier = paperPopupModifier(dialogShape),
        shape = dialogShape,
        containerColor = AriCard,
        title = { Text(tx("Edit image")) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                if (preview != null) {
                    val aspectRatio = preview.width.toFloat() / preview.height.toFloat()
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val maxCanvasHeight = 250.dp
                        val displayWidth = minOf(maxWidth, maxCanvasHeight * aspectRatio)
                        val displayHeight = displayWidth / aspectRatio
                        Canvas(
                            modifier = Modifier.width(displayWidth).height(displayHeight)
                                .clip(RoundedCornerShape(16.dp))
                                .onSizeChanged { canvasSize = it }
                                .pointerInput(preview) {
                                    detectDragGestures(
                                        onDragStart = { point -> activeStroke = listOf(point) },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            activeStroke = activeStroke + change.position
                                        },
                                        onDragEnd = {
                                            if (activeStroke.isNotEmpty()) strokes = strokes + listOf(activeStroke)
                                            activeStroke = emptyList()
                                        },
                                        onDragCancel = {
                                            if (activeStroke.isNotEmpty()) strokes = strokes + listOf(activeStroke)
                                            activeStroke = emptyList()
                                        }
                                    )
                                }
                        ) {
                            drawImage(
                                preview,
                                dstOffset = IntOffset.Zero,
                                dstSize = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))
                            )
                            val overlayColor = Color(0xAA3977D5)
                            val widthPx = with(density) { brushWidthDp.dp.toPx() }
                            (strokes + listOf(activeStroke)).filter { it.isNotEmpty() }.forEach { points ->
                                if (points.size == 1) {
                                    drawCircle(overlayColor, radius = widthPx / 2f, center = points.first())
                                } else {
                                    val path = Path().apply {
                                        moveTo(points.first().x, points.first().y)
                                        points.drop(1).forEach { lineTo(it.x, it.y) }
                                    }
                                    drawPath(
                                        path,
                                        color = overlayColor,
                                        style = Stroke(width = widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        tx("Paint the area you want to replace. Leave the canvas blank to edit the full image."),
                        color = AriMuted,
                        fontSize = 12.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(tx("Brush size"), color = AriInk, fontSize = 12.sp)
                        TextButton(onClick = { strokes = emptyList(); activeStroke = emptyList(); maskError = null }) {
                            Text(tx("Clear mask"), color = AriAccent)
                        }
                    }
                    Slider(
                        value = brushWidthDp,
                        onValueChange = { brushWidthDp = it },
                        valueRange = 8f..48f,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(tx("Image preview unavailable. You can still edit it with a prompt."), color = AriMuted, fontSize = 12.sp)
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(2_000) },
                    placeholder = { Text(tx("What should change?")) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                maskError?.let { Text(it, color = Color(0xFFB3261E), fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank() && !preparingMask,
                onClick = {
                    if (text.isBlank() || preparingMask) return@TextButton
                    preparingMask = true
                    maskError = null
                    val submittedText = text.trim()
                    val submittedStrokes = strokes + listOf(activeStroke).filter { it.isNotEmpty() }
                    val submittedBitmap = sourceBitmap
                    val submittedCanvasSize = canvasSize
                    val submittedBrushWidth = brushWidthDp
                    val submittedDensity = density
                    scope.launch {
                        var generatedMask: File? = null
                        try {
                            val mask = withContext(Dispatchers.IO) {
                                if (submittedBitmap != null && submittedStrokes.isNotEmpty() && submittedCanvasSize.width > 0 && submittedCanvasSize.height > 0) {
                                    createImageEditMaskFile(
                                        bitmap = submittedBitmap,
                                        strokes = submittedStrokes,
                                        canvasWidth = submittedCanvasSize.width,
                                        canvasHeight = submittedCanvasSize.height,
                                        brushWidthDp = submittedBrushWidth,
                                        density = submittedDensity
                                    ).also { generatedMask = it }
                                } else null
                            }
                            preparingMask = false
                            onConfirm(submittedText, mask)
                            generatedMask = null // Ownership has moved to the view model.
                        } catch (cancelled: CancellationException) {
                            generatedMask?.let { com.ariai.app.data.local.ImageStore.deleteTemp(it) }
                            preparingMask = false
                            throw cancelled
                        } catch (e: Exception) {
                            com.ariai.app.util.AppLog.warn(e, "ChatMediaComponents.kt")
                            generatedMask?.let { com.ariai.app.data.local.ImageStore.deleteTemp(it) }
                            preparingMask = false
                            maskError = tx("Could not prepare the image mask.")
                        }
                    }
                }
            ) { Text(if (preparingMask) tx("Preparing…") else tx("Send")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !preparingMask) { Text(tx("Cancel")) }
        }
    )
}

private fun createImageEditMaskFile(
    bitmap: Bitmap,
    strokes: List<List<Offset>>,
    canvasWidth: Int,
    canvasHeight: Int,
    brushWidthDp: Float,
    density: Float
): File {
    require(canvasWidth > 0 && canvasHeight > 0)
    val mask = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    try {
        val canvas = android.graphics.Canvas(mask)
        canvas.drawColor(AndroidColor.WHITE)
        val scaleX = bitmap.width.toFloat() / canvasWidth
        val scaleY = bitmap.height.toFloat() / canvasHeight
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.TRANSPARENT
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = brushWidthDp * density * minOf(scaleX, scaleY)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        strokes.filter { it.isNotEmpty() }.forEach { points ->
            if (points.size == 1) {
                paint.style = Paint.Style.FILL
                canvas.drawCircle(points.first().x * scaleX, points.first().y * scaleY, paint.strokeWidth / 2f, paint)
                paint.style = Paint.Style.STROKE
            } else {
                val path = android.graphics.Path().apply {
                    moveTo(points.first().x * scaleX, points.first().y * scaleY)
                    points.drop(1).forEach { lineTo(it.x * scaleX, it.y * scaleY) }
                }
                canvas.drawPath(path, paint)
            }
        }
        val png = java.io.ByteArrayOutputStream().use { output ->
            check(mask.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode the image mask." }
            output.toByteArray()
        }
        return com.ariai.app.data.local.ImageStore.createTempImage(png, "image/png")
    } finally {
        mask.recycle()
    }
}

internal fun isVideoPath(path: String) = path.endsWith(".mp4", ignoreCase = true)
internal fun isAudioPath(path: String) = path.endsWith(".mp3", ignoreCase = true)

/** Small thin label in the chat's top-left corner: shows the active mode, closes back to text. */
@Composable
internal fun ModeChip(mode: ChatMode, onClose: () -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(AriCard)
            .border(0.5.dp, AriLine, RoundedCornerShape(50))
            .padding(start = 10.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            when (mode) {
                ChatMode.VIDEO -> tx("Video")
                ChatMode.AUDIO -> tx("Audio")
                else -> tx("Image")
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            color = AriInk
        )
        IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = tx("Close"), tint = AriMuted, modifier = Modifier.size(12.dp))
        }
    }
}

@android.annotation.SuppressLint("UnsafeOptInUsageError")
@Composable
internal fun VideoCard(path: String) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(22.dp), color = AriCard, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(AriTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AriInk)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(tx("Video"), fontWeight = FontWeight.SemiBold, color = AriInk)
                Text(tx("Generated video"), fontSize = 12.sp, color = AriMuted)
            }
            TextButton(onClick = { playing = true }) { Text(tx("Open"), color = AriInk, fontWeight = FontWeight.SemiBold) }
        }
    }
    if (playing) VideoPlayerDialog(path) { playing = false }
}

/** Generated audio: play and pause in place. */
@Composable
internal fun AudioCard(path: String) {
    val context = LocalContext.current
    val player = remember(path) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(path))))
            prepare()
        }
    }
    var playing by remember(path) { mutableStateOf(false) }
    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    Surface(shape = RoundedCornerShape(22.dp), color = AriCard, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = { if (player.isPlaying) player.pause() else player.play() },
                modifier = Modifier.size(44.dp).clip(CircleShape).background(AriTint)
            ) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) tx("Pause") else tx("Play"),
                    tint = AriInk
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(tx("Audio"), fontWeight = FontWeight.SemiBold, color = AriInk)
                Text(tx("Generated audio"), fontSize = 12.sp, color = AriMuted)
            }
        }
    }
}

/** Plays a generated video inside the app with the Media3 player and its standard controls. */
@androidx.media3.common.util.UnstableApi
@Composable
private fun VideoPlayerDialog(path: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val player = remember(path) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(path))))
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    androidx.media3.ui.PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Default.Close, contentDescription = tx("Close"), tint = Color.White)
            }
        }
    }
}

private val ImageLinkRegex = Regex(
    """!\[[^\]]*\]\((https?://[^\s)]+)\)|(https?://media\.pollinations\.ai/[^\s)]+|https?://[^\s)]+\.(?:png|jpe?g|webp|gif)(?:\?[^\s)]*)?)""",
    RegexOption.IGNORE_CASE
)

internal fun extractImageUrls(text: String): List<String> =
    ImageLinkRegex.findAll(text).map { m -> m.groupValues[1].ifEmpty { m.groupValues[2] } }.distinct().toList()

internal fun stripImageUrls(text: String): String = ImageLinkRegex.replace(text, "").trim()

private suspend fun decodeRemoteImage(bytes: ByteArray): ImageBitmap? =
    decodeSampledBitmapFromBytes(bytes, maxDimension = 1024)?.asImageBitmap()

/** Safely downloads a bounded public image off the main thread and shows a downsampled preview. */
@Composable
internal fun RemoteImage(url: String) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = url) {
        value = withContext(Dispatchers.IO) {
            try {
                decodeRemoteImage(remoteImageFetcher.fetchImageBytes(url))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                com.ariai.app.util.AppLog.warn(e, "ChatMediaComponents.kt")
                null
            }
        }
    }
    val shape = RoundedCornerShape(20.dp)
    var full by remember { mutableStateOf(false) }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().clip(shape).clickable { full = true }
        )
    } ?: Box(
        modifier = Modifier.fillMaxWidth().height(180.dp).clip(shape).background(AriTint),
        contentAlignment = Alignment.Center
    ) {
        Text(tx("Loading image…"), fontSize = 12.sp, color = AriMuted)
    }
    if (full) bitmap?.let { FullImageDialog(it) { full = false } }
}
