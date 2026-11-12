package com.ariai.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Xml
import com.googlecode.tesseract.android.TessBaseAPI
import com.ariai.app.data.models.Attachment
import com.ariai.app.data.models.AttachmentType
import com.ariai.app.util.tx
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.Writer
import java.util.zip.ZipInputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

private const val MAX_IMAGE_EDGE = 1024
private const val MAX_TEXT_CHARS = 60_000
private const val MAX_VIDEO_BYTES = 12L * 1024 * 1024
private const val MAX_PDF_BYTES = 20L * 1024 * 1024
private const val MAX_DOCX_XML_BYTES = 5 * 1024 * 1024
private const val MAX_DOCX_EXPANDED_BYTES = 40L * 1024 * 1024
internal const val MAX_PDF_EXTRACT_PAGES = 20
internal fun pdfPageCountToExtract(pageCount: Int, maximumPages: Int = MAX_PDF_EXTRACT_PAGES): Int =
    pageCount.coerceAtLeast(0).coerceAtMost(maximumPages.coerceAtLeast(0))
private const val MAX_OCR_PAGES = 20
private const val MAX_OCR_PAGE_EDGE = 1800
private const val TRUNCATION_NOTICE = "\n[Extracted text truncated to fit the response context.]"
private const val OCR_UNAVAILABLE_NOTICE = "\n[Offline OCR could not process some scanned pages.]"
private val OCR_MODEL_SIZES = mapOf("eng.traineddata" to 4_113_088L, "fas.traineddata" to 431_500L)
private val OCR_DATA_LOCK = Any()
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
suspend fun readAttachment(context: Context, uri: Uri, extractedDocumentText: String? = null): ReadResult =
    withContext(Dispatchers.IO) { readAttachmentOnIo(context, uri, extractedDocumentText) }

private fun readAttachmentOnIo(context: Context, uri: Uri, extractedDocumentText: String?): ReadResult {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri) ?: "application/octet-stream"
    val name = attachmentDisplayName(context, uri) ?: "file"
    return try {
        when {
            mime.startsWith("image/") -> {
                val bitmap = decodeScaled(context, uri) ?: return ReadResult.Error(tx("Could not read this image."))
                val attachment = try {
                    imageAttachment(bitmap, name)
                } finally {
                    bitmap.recycle()
                }
                ReadResult.Ok(attachment)
            }
            mime == "application/pdf" || name.substringAfterLast('.', "").equals("pdf", ignoreCase = true) -> {
                val text = extractedDocumentText ?: extractPdfText(context, uri)
                ReadResult.Ok(Attachment(
                    type = AttachmentType.PDF,
                    name = name,
                    mimeType = "application/pdf",
                    textContent = text
                ))
            }
            mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
                name.substringAfterLast('.', "").equals("docx", ignoreCase = true) -> {
                val text = extractedDocumentText ?: extractDocxText(
                    resolver.openInputStream(uri) ?: return ReadResult.Error(tx("Could not open this file."))
                )
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
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (e: Exception) {
        ReadResult.Error(tx("Could not read this file."))
    }
}

private class AttachmentReadException(message: String, cause: Throwable? = null) : Exception(message, cause)

internal fun extractPdfText(context: Context, uri: Uri): String {
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
        val contentLimit = MAX_TEXT_CHARS - TRUNCATION_NOTICE.length - OCR_UNAVAILABLE_NOTICE.length
        val pageTexts = mutableListOf<String>()
        val scannedPageIndexes = mutableListOf<Int>()
        var usedCharacters = 0
        val pageCount = pdfPageCountToExtract(pdf.numberOfPages)
        var truncated = pdf.numberOfPages > pageCount

        for (pageNumber in 1..pageCount) {
            val separatorLength = if (usedCharacters > 0) 2 else 0
            val remaining = (contentLimit - usedCharacters - separatorLength).coerceAtLeast(0)
            if (remaining == 0) {
                truncated = true
                break
            }
            val writer = LimitedTextWriter(remaining)
            val stripper = com.tom_roush.pdfbox.text.PDFTextStripper().apply {
                startPage = pageNumber
                endPage = pageNumber
            }
            stripper.writeText(pdf, writer)
            val pageText = writer.value().trim()
            pageTexts += pageText
            if (pageText.isBlank()) {
                scannedPageIndexes += pageNumber - 1
            } else {
                usedCharacters += pageText.length + separatorLength
            }
            if (writer.wasTruncated) {
                truncated = true
                break
            }
        }
        if (pageTexts.size < pageCount) truncated = true

        var ocrUnavailable = false
        if (scannedPageIndexes.isNotEmpty()) {
            if (scannedPageIndexes.size > MAX_OCR_PAGES) truncated = true
            val pagesToRecognize = scannedPageIndexes.take(MAX_OCR_PAGES)
            try {
                PdfOcrSession.open(context, pdfFile).use { ocr ->
                    for (pageIndex in pagesToRecognize) {
                        val recognized = ocr.recognizePage(pageIndex).trim()
                        if (recognized.isBlank()) continue
                        val separatorLength = if (usedCharacters > 0) 2 else 0
                        val remaining = (contentLimit - usedCharacters - separatorLength).coerceAtLeast(0)
                        if (remaining == 0) {
                            truncated = true
                            break
                        }
                        val acceptedText = recognized.take(remaining)
                        pageTexts[pageIndex] = acceptedText
                        usedCharacters += acceptedText.length + separatorLength
                        if (acceptedText.length < recognized.length) {
                            truncated = true
                            break
                        }
                    }
                }
            } catch (error: AttachmentReadException) {
                if (pageTexts.none { it.isNotBlank() }) throw error
                ocrUnavailable = true
            } catch (error: Exception) {
                if (pageTexts.none { it.isNotBlank() }) {
                    throw AttachmentReadException("Offline OCR could not be started for this PDF.", error)
                }
                ocrUnavailable = true
            }
        }

        val extracted = pageTexts.filter { it.isNotBlank() }.joinToString("\n\n").trim()
        if (extracted.isBlank()) {
            throw AttachmentReadException("No readable text was found in this PDF.")
        }
        val notices = buildString {
            if (ocrUnavailable) append(OCR_UNAVAILABLE_NOTICE)
            if (truncated) append(TRUNCATION_NOTICE)
        }
        return extracted.take((MAX_TEXT_CHARS - notices.length).coerceAtLeast(0)) + notices
    }
}

private class PdfOcrSession private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    private val tess: TessBaseAPI
) : java.io.Closeable {
    fun recognizePage(pageIndex: Int): String {
        if (pageIndex !in 0 until renderer.pageCount) return ""
        renderer.openPage(pageIndex).use { page ->
            val scale = minOf(2f, MAX_OCR_PAGE_EDGE.toFloat() / max(page.width, page.height).coerceAtLeast(1))
            val bitmapWidth = (page.width * scale).roundToInt().coerceAtLeast(1)
            val bitmapHeight = (page.height * scale).roundToInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            try {
                Canvas(bitmap).drawColor(Color.WHITE)
                val transform = Matrix().apply { setScale(scale, scale) }
                page.render(bitmap, null, transform, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                tess.setImage(bitmap)
                return tess.getUTF8Text().orEmpty()
            } finally {
                tess.clear()
                bitmap.recycle()
            }
        }
    }

    override fun close() {
        try {
            tess.recycle()
        } finally {
            try {
                renderer.close()
            } finally {
                descriptor.close()
            }
        }
    }

    companion object {
        fun open(context: Context, pdfFile: File): PdfOcrSession {
            val descriptor = try {
                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            } catch (error: Exception) {
                throw AttachmentReadException("Could not open scanned PDF pages for OCR.", error)
            }
            var renderer: PdfRenderer? = null
            var tess: TessBaseAPI? = null
            try {
                val dataPath = installOcrModels(context)
                val openedRenderer = PdfRenderer(descriptor)
                renderer = openedRenderer
                val openedTess = TessBaseAPI()
                tess = openedTess
                if (!openedTess.init(dataPath.absolutePath, "fas+eng", TessBaseAPI.OEM_LSTM_ONLY)) {
                    throw AttachmentReadException("Offline OCR could not be initialized.")
                }
                return PdfOcrSession(descriptor, openedRenderer, openedTess)
            } catch (error: Exception) {
                runCatching { tess?.recycle() }
                runCatching { renderer?.close() }
                runCatching { descriptor.close() }
                if (error is AttachmentReadException) throw error
                throw AttachmentReadException("Offline OCR could not be initialized.", error)
            }
        }
    }
}

private fun installOcrModels(context: Context): File = synchronized(OCR_DATA_LOCK) {
    val root = File(context.filesDir, "ariai-ocr")
    val tessData = File(root, "tessdata")
    if (!tessData.isDirectory && !tessData.mkdirs()) {
        throw AttachmentReadException("Offline OCR data could not be prepared.")
    }
    OCR_MODEL_SIZES.forEach { (fileName, expectedSize) ->
        val target = File(tessData, fileName)
        if (target.isFile && target.length() == expectedSize) return@forEach
        val temporary = File(tessData, "$fileName.tmp")
        try {
            context.assets.open("tessdata/$fileName").use { input ->
                temporary.outputStream().buffered().use { output -> input.copyTo(output) }
            }
            if (temporary.length() != expectedSize) {
                temporary.delete()
                throw AttachmentReadException("Offline OCR data could not be installed.")
            }
            if (target.exists() && !target.delete()) {
                temporary.delete()
                throw AttachmentReadException("Offline OCR data could not be installed.")
            }
            if (!temporary.renameTo(target)) {
                temporary.delete()
                throw AttachmentReadException("Offline OCR data could not be installed.")
            }
        } catch (error: Exception) {
            temporary.delete()
            if (error is AttachmentReadException) throw error
            throw AttachmentReadException("Offline OCR data could not be installed.", error)
        }
    }
    root
}

internal fun extractDocxText(
    input: InputStream,
    parserFactory: () -> XmlPullParser = { Xml.newPullParser() }
): String {
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

    val parser = parserFactory()
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
    val rowHasCells = mutableListOf<Boolean>()
    var tableDepth = 0
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT && text.length < maxChars) {
        val tag = if (event == XmlPullParser.START_TAG || event == XmlPullParser.END_TAG) {
            parser.name.substringAfter(':')
        } else {
            ""
        }
        if (event == XmlPullParser.START_TAG) {
            when (tag) {
                "tbl" -> {
                    if (tableDepth == 0 && text.isNotEmpty() && text.last() != '\n') appendLimited("\n")
                    tableDepth++
                }
                "tr" -> if (tableDepth > 0) {
                    if (text.isNotEmpty() && text.last() != '\n') appendLimited("\n")
                    rowHasCells += false
                }
                "tc" -> if (tableDepth > 0 && rowHasCells.isNotEmpty()) {
                    val rowIndex = rowHasCells.lastIndex
                    if (rowHasCells[rowIndex]) appendLimited("\t")
                    rowHasCells[rowIndex] = true
                }
                "p" -> if (text.isNotEmpty() && text.last() != '\n' && text.last() != '\t') appendLimited("\n")
                "t" -> appendLimited(parser.nextText())
                "tab" -> appendLimited("\t")
                "br", "cr" -> appendLimited("\n")
            }
        } else if (event == XmlPullParser.END_TAG) {
            when (tag) {
                "tr" -> if (tableDepth > 0) {
                    if (rowHasCells.isNotEmpty()) rowHasCells.removeAt(rowHasCells.lastIndex)
                    if (text.isNotEmpty() && text.last() != '\n') appendLimited("\n")
                }
                "tbl" -> {
                    tableDepth = (tableDepth - 1).coerceAtLeast(0)
                    if (tableDepth == 0 && text.isNotEmpty() && text.last() != '\n') appendLimited("\n")
                }
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
fun bitmapAttachment(bitmap: Bitmap, name: String = "camera.webp"): Attachment {
    val scaled = scaleDown(bitmap)
    return try {
        imageAttachment(scaled, name)
    } finally {
        if (scaled !== bitmap) scaled.recycle()
    }
}

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

private fun decodeScaled(context: Context, uri: Uri): Bitmap? =
    decodeSampledBitmapFromUriBlocking(context, uri, maxDimension = MAX_IMAGE_EDGE)

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

internal fun attachmentDisplayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
