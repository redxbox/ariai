package com.ariai.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.FileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DEFAULT_BITMAP_EDGE = 1_024
private const val MAX_BITMAP_EDGE = 1_024
private const val MAX_DECODE_ATTEMPTS = 3

/**
 * Decodes a bounded image preview on Dispatchers.IO. JPEGs are opaque, so RGB_565 halves their
 * decoded pixel memory; formats that may contain alpha keep ARGB_8888 to preserve transparency.
 */
suspend fun decodeSampledBitmapFromUri(
    context: Context,
    uri: Uri,
    maxDimension: Int = DEFAULT_BITMAP_EDGE
): Bitmap? = withContext(Dispatchers.IO) {
    decodeSampledBitmapFromUriBlocking(context, uri, maxDimension)
}

/** Blocking counterpart for code that is already running inside Dispatchers.IO. */
internal fun decodeSampledBitmapFromUriBlocking(
    context: Context,
    uri: Uri,
    maxDimension: Int = DEFAULT_BITMAP_EDGE
): Bitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val mimeType = runCatching { resolver.getType(uri) }.getOrNull()
    val boundsRead = runCatching {
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    }.isSuccess
    if (!boundsRead || !bounds.hasValidDimensions()) return null

    return decodeWithFallback(bounds.outWidth, bounds.outHeight, maxDimension) { sample ->
        runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions(sample, mimeType ?: bounds.outMimeType))
            }
        }.getOrNull()
    }
}

/** Bounded file decode for thumbnails and generated images. */
suspend fun decodeSampledBitmapFromPath(
    path: String,
    maxDimension: Int = DEFAULT_BITMAP_EDGE
): Bitmap? = withContext(Dispatchers.IO) {
    decodeSampledBitmapFromPathBlocking(path, maxDimension)
}

internal fun decodeSampledBitmapFromPathBlocking(
    path: String,
    maxDimension: Int = DEFAULT_BITMAP_EDGE
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsRead = runCatching {
        FileInputStream(path).use { BitmapFactory.decodeStream(it, null, bounds) }
    }.isSuccess
    if (!boundsRead || !bounds.hasValidDimensions()) return null

    return decodeWithFallback(bounds.outWidth, bounds.outHeight, maxDimension) { sample ->
        runCatching {
            FileInputStream(path).use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions(sample, bounds.outMimeType))
            }
        }.getOrNull()
    }
}

/** Bounded decode for in-memory image attachments and downloaded image bytes. */
suspend fun decodeSampledBitmapFromBytes(
    bytes: ByteArray,
    maxDimension: Int = DEFAULT_BITMAP_EDGE
): Bitmap? = withContext(Dispatchers.IO) {
    decodeSampledBitmapFromBytesBlocking(bytes, maxDimension)
}

internal fun decodeSampledBitmapFromBytesBlocking(
    bytes: ByteArray,
    maxDimension: Int = DEFAULT_BITMAP_EDGE
): Bitmap? {
    if (bytes.isEmpty()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsRead = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds) }.isSuccess
    if (!boundsRead || !bounds.hasValidDimensions()) return null

    return decodeWithFallback(bounds.outWidth, bounds.outHeight, maxDimension) { sample ->
        runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions(sample, bounds.outMimeType))
        }.getOrNull()
    }
}

private inline fun decodeWithFallback(
    width: Int,
    height: Int,
    maxDimension: Int,
    decode: (sampleSize: Int) -> Bitmap?
): Bitmap? {
    val safeEdge = maxDimension.coerceIn(1, MAX_BITMAP_EDGE)
    var sampleSize = calculateSampleSize(width, height, safeEdge)
    repeat(MAX_DECODE_ATTEMPTS) {
        try {
            decode(sampleSize)?.let { return it }
        } catch (_: OutOfMemoryError) {
            // Retry at a smaller scale if this device has less free memory than expected.
        }
        if (sampleSize >= (1 shl 29)) return null
        sampleSize *= 2
    }
    return null
}

private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    var sampleSize = 1
    while (width / sampleSize > maxDimension || height / sampleSize > maxDimension) {
        if (sampleSize >= (1 shl 29)) return sampleSize
        sampleSize *= 2
    }
    return sampleSize
}

private fun decodeOptions(sampleSize: Int, mimeType: String?): BitmapFactory.Options {
    val isOpaqueJpeg = mimeType.equals("image/jpeg", ignoreCase = true) ||
        mimeType.equals("image/jpg", ignoreCase = true)
    return BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = if (isOpaqueJpeg) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
        inDither = isOpaqueJpeg
    }
}

private fun BitmapFactory.Options.hasValidDimensions(): Boolean = outWidth > 0 && outHeight > 0
