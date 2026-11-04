package com.ariai.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Xml
import com.ariai.app.data.models.Attachment
import com.ariai.app.data.models.AttachmentType
import com.ariai.app.util.tx
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.Writer
import java.util.zip.ZipInputStream
import org.xmlpull.v1.XmlPullParser

private const val MAX_IMAGE_EDGE = 1280
private const val MAX_TEXT_CHARS = 60_000
private const val MAX_VIDEO_BYTES = 12L * 1024 * 1024
private const val MAX_PDF_BYTES = 20L * 1024 * 1024
private const val MAX_DOCX_XML_BYTES = 5 * 1024 * 1024
private const val MAX_DOCX_EXPANDED_BYTES = 40L * 1024 * 1024
private const val MAX_PDF_PAGES = 200
private const val TRUNCATION_NOTICE = "\n[Extracted text truncated to fit the response context.]"
private val TEXT_EXTENSIONS = setOf(
    "txt", "md", "json", "csv", "xml", "html", "kt", "java", "py", "js", "ts", "yaml", "yml", "log", "sql", "toml"
)

sealed interface ReadResult {
    data class Ok(val attachment: Attachment) : ReadResult
    data class Error(val message: String) : ReadResult
}

/**
 * Reads a file picked by the user. Images are scaled down and WebP-encoded to keep
 * requests small; text-like files are returned as text to inline into the prompt.
 */
fun readAttachment(context: Context, uri: Uri): ReadResult {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri) ?: "application/octet-stream"
    val name = displayName(context, uri) ?: "file"
    return try {
        when {
            mime.startsWith("image/") -> {
                val bitmap = decodeScaled(context, uri) ?: return ReadResult.Error(tx("Could not read this image."))
                ReadResult.Ok(imageAttachment(bitmap, name))
            }
            mime == "application/pdf" || name.substringAfterLast('.', "").equals("pdf", ignoreCase = true) -> {
                val text = extractPdfText(context, uri)
                ReadResult.Ok(Attachment(
                    type = AttachmentType.PDF,
                    name = name,
                    mimeType = "application/pdf",
                    textContent = text
                ))
            }
            mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
                name.substringAfterLast('.', "").equals("docx", ignoreCase = true) -> {
                val text = extractDocxText(resolver.openInputStream(uri) ?: return ReadResult.Error(tx("Could not open this file.")))
                ReadResult.Ok(Attachment(
                    type = AttachmentType.DOCX,
                    name = name,
                    mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    textContent = text
                ))
            }
            mime.startsWith("text/") || mime == "application/json" ||
                name.substringAfterLast('.', "").lowercase() in TEXT_EXTENSIONS -> {
                val text = resolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    val result = StringBuilder()
                    val buffer = CharArray(4096)
                    while (result.length < MAX_TEXT_CHARS) {
                        val count = reader.read(buffer, 0, minOf(buffer.size, MAX_TEXT_CHARS - result.length))
                        if (count < 0) break
                        result.append(buffer, 0, count)
                    }
                    result.toString()
                } ?: return ReadResult.Error(tx("Could not open this file."))
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
                val bytes = resolver.openInputStream(uri)?.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_VIDEO_BYTES) return ReadResult.Error(tx("Video is larger than 12 MB"))
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                } ?: return ReadResult.Error(tx("Could not open this file."))
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
            else -> ReadResult.Error(tx("This file type is not supported yet. Choose an image, video, PDF, DOCX, or text file."))
        }
    } catch (e: AttachmentReadException) {
        ReadResult.Error(tx(e.message ?: "Could not read this document."))
    } catch (e: Exception) {
        ReadResult.Error(tx("Could not read this file."))
    }
}

private class AttachmentReadException(message: String) : Exception(message)

private fun extractPdfText(context: Context, uri: Uri): String {
    val temp = File.createTempFile("ariai-pdf-", ".pdf", context.cacheDir)
    try {
        val input = context.contentResolver.openInputStream(uri) ?: throw AttachmentReadException("Could not open PDF.")
        input.use { source ->
            temp.outputStream().buffered().use { target ->
                copyBounded(source, target, MAX_PDF_BYTES, "PDF is larger than 20 MB.")
            }
        }
        return extractPdfTextFromFile(context, temp)
    } finally {
        temp.delete()
    }
}

internal fun extractPdfTextFromFile(context: Context, pdfFile: File): String {
    com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context.applicationContext)
    val document = try {
        com.tom_roush.pdfbox.pdmodel.PDDocument.load(pdfFile)
    } catch (password: com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException) {
        throw AttachmentReadException("This PDF is password-protected.")
    }
    document.use { pdf ->
        if (pdf.numberOfPages <= 0) throw AttachmentReadException("This PDF has no pages.")
        val writer = LimitedTextWriter(MAX_TEXT_CHARS - TRUNCATION_NOTICE.length)
        val stripper = com.tom_roush.pdfbox.text.PDFTextStripper().apply {
            startPage = 1
            endPage = pdf.numberOfPages.coerceAtMost(MAX_PDF_PAGES)
        }
        stripper.writeText(pdf, writer)
        val extracted = writer.value().trim()
        if (extracted.isBlank()) {
            throw AttachmentReadException("No selectable text was found in this PDF. Scanned pages may need OCR.")
        }
        val wasTruncated = writer.wasTruncated || pdf.numberOfPages > MAX_PDF_PAGES
        return if (wasTruncated) extracted + TRUNCATION_NOTICE else extracted
    }
}

internal fun extractDocxText(input: InputStream): String {
    val xml = input.use { source ->
        ZipInputStream(source.buffered()).use { zip ->
            var expandedBytes = 0L
            var documentXml: ByteArray? = null
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                if (entry.name == "word/document.xml") {
                    documentXml = readZipEntryBounded(zip, MAX_DOCX_XML_BYTES, "The DOCX document XML is too large.")
                    break
                }
                val buffer = ByteArray(8192)
                while (true) {
                    val count = zip.read(buffer)
                    if (count < 0) break
                    expandedBytes += count
                    if (expandedBytes > MAX_DOCX_EXPANDED_BYTES) {
                        throw AttachmentReadException("The DOCX document expands beyond the supported size.")
                    }
                }
                zip.closeEntry()
            }
            documentXml ?: throw AttachmentReadException("This DOCX file has no document.xml content.")
        }
    }

    val parser = Xml.newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
    runCatching { parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false) }
    parser.setInput(xml.inputStream(), "UTF-8")
    val text = StringBuilder()
    val maxChars = MAX_TEXT_CHARS - TRUNCATION_NOTICE.length
    var truncated = false
    fun appendLimited(value: String) {
        val remaining = (maxChars - text.length).coerceAtLeast(0)
        val accepted = minOf(value.length, remaining)
        if (accepted > 0) text.append(value, 0, accepted)
        if (accepted < value.length) truncated = true
    }
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT && text.length < maxChars) {
        if (event == XmlPullParser.START_TAG) {
            when (parser.name.substringAfter(':')) {
                "p" -> if (text.isNotEmpty() && text.last() != '\n') appendLimited("\n")
                "t" -> appendLimited(parser.nextText())
                "tab" -> appendLimited("\t")
                "br", "cr" -> appendLimited("\n")
            }
        }
        event = parser.next()
    }
    truncated = truncated || event != XmlPullParser.END_DOCUMENT
    val extracted = text.toString().trim()
    if (extracted.isBlank()) throw AttachmentReadException("No text was found in this DOCX document.")
    return if (truncated) extracted + TRUNCATION_NOTICE else extracted
}

private fun readZipEntryBounded(input: InputStream, maxBytes: Int, message: String): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var total = 0
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        total += count
        if (total > maxBytes) throw AttachmentReadException(message)
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

private fun copyBounded(input: InputStream, output: java.io.OutputStream, maxBytes: Long, message: String) {
    val buffer = ByteArray(8192)
    var total = 0L
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        total += count
        if (total > maxBytes) throw AttachmentReadException(message)
        output.write(buffer, 0, count)
    }
}

private class LimitedTextWriter(private val maxChars: Int) : Writer() {
    private val result = StringBuilder()
    var wasTruncated: Boolean = false
        private set

    override fun write(chars: CharArray, offset: Int, length: Int) {
        val remaining = (maxChars - result.length).coerceAtLeast(0)
        val accepted = minOf(length, remaining)
        if (accepted > 0) result.append(chars, offset, accepted)
        if (accepted < length) wasTruncated = true
    }

    override fun flush() = Unit
    override fun close() = Unit
    fun value(): String = result.toString()
}

/** Wraps a bitmap from the camera preview as an image attachment. */
fun bitmapAttachment(bitmap: Bitmap, name: String = "camera.webp"): Attachment =
    imageAttachment(scaleDown(bitmap), name)

private fun imageAttachment(bitmap: Bitmap, name: String): Attachment {
    val out = ByteArrayOutputStream()
    @Suppress("DEPRECATION")
    bitmap.compress(Bitmap.CompressFormat.WEBP, 82, out)
    val webpName = name.substringBeforeLast('.', name).ifBlank { "image" } + ".webp"
    return Attachment(
        type = AttachmentType.IMAGE,
        name = webpName,
        mimeType = "image/webp",
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
