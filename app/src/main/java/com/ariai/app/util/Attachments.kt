package com.ariai.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.ariai.app.data.models.Attachment
import com.ariai.app.data.models.AttachmentType
import com.ariai.app.util.tx
import java.io.ByteArrayOutputStream

private const val MAX_IMAGE_EDGE = 1280
private const val MAX_TEXT_CHARS = 60_000
private const val MAX_VIDEO_BYTES = 12L * 1024 * 1024
private val TEXT_EXTENSIONS = setOf(
    "txt", "md", "json", "csv", "xml", "html", "kt", "java", "py", "js", "ts", "yaml", "yml", "log", "sql", "toml"
)

sealed interface ReadResult {
    data class Ok(val attachment: Attachment) : ReadResult
    data class Error(val message: String) : ReadResult
}

/**
 * Reads a file picked by the user. Images are scaled down and JPEG-encoded to keep
 * requests small; text-like files are returned as text to inline into the prompt.
 */
fun readAttachment(context: Context, uri: Uri): ReadResult {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri) ?: "application/octet-stream"
    val name = displayName(context, uri) ?: "file"
    return try {
        when {
            mime.startsWith("image/") -> {
                val bitmap = decodeScaled(context, uri) ?: return ReadResult.Error("Could not read $name")
                ReadResult.Ok(imageAttachment(bitmap, name))
            }
            mime.startsWith("text/") || mime == "application/json" ||
                name.substringAfterLast('.', "").lowercase() in TEXT_EXTENSIONS -> {
                val text = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: return ReadResult.Error("Could not open $name")
                ReadResult.Ok(
                    Attachment(
                        type = AttachmentType.TEXT,
                        name = name,
                        mimeType = mime,
                        textContent = text.take(MAX_TEXT_CHARS)
                    )
                )
            }
            mime.startsWith("video/") -> {
                val size = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                if (size > MAX_VIDEO_BYTES) return ReadResult.Error(tx("Video is larger than 12 MB"))
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return ReadResult.Error("Could not open $name")
                if (bytes.size > MAX_VIDEO_BYTES) return ReadResult.Error(tx("Video is larger than 12 MB"))
                ReadResult.Ok(
                    Attachment(
                        type = AttachmentType.VIDEO,
                        name = name,
                        mimeType = mime,
                        base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    )
                )
            }
            else -> ReadResult.Error("$name is not supported yet. Pick an image, a video or a text file.")
        }
    } catch (e: Exception) {
        ReadResult.Error("Could not read $name")
    }
}

/** Wraps a bitmap from the camera preview as an image attachment. */
fun bitmapAttachment(bitmap: Bitmap, name: String = "camera.jpg"): Attachment =
    imageAttachment(scaleDown(bitmap), name)

private fun imageAttachment(bitmap: Bitmap, name: String): Attachment {
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
    return Attachment(
        type = AttachmentType.IMAGE,
        name = name,
        mimeType = "image/jpeg",
        base64Data = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    )
}

private fun decodeScaled(context: Context, uri: Uri): Bitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > MAX_IMAGE_EDGE * 2 || bounds.outHeight / sample > MAX_IMAGE_EDGE * 2) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
    return scaleDown(decoded)
}

private fun scaleDown(bitmap: Bitmap): Bitmap {
    val longest = maxOf(bitmap.width, bitmap.height)
    if (longest <= MAX_IMAGE_EDGE) return bitmap
    val ratio = MAX_IMAGE_EDGE.toFloat() / longest
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * ratio).toInt().coerceAtLeast(1),
        (bitmap.height * ratio).toInt().coerceAtLeast(1),
        true
    )
}

private fun displayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
