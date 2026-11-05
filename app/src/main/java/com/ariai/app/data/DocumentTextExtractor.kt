package com.ariai.app.data

import android.content.Context
import android.net.Uri
import com.ariai.app.util.attachmentDisplayName
import com.ariai.app.util.extractDocxText
import com.ariai.app.util.extractPdfText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

/** Bounded, on-device text extraction for PDF and DOCX documents. */
class DocumentTextExtractor(context: Context) {
    private val appContext = context.applicationContext

    /** True for documents recognized by their MIME type or filename extension. */
    suspend fun supports(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val mimeType = appContext.contentResolver.getType(uri)
        val name = attachmentDisplayName(appContext, uri).orEmpty()
        formatFor(mimeType, name) != DocumentFormat.UNSUPPORTED
    }

    /** Extracts document text off the main thread, returning parser and I/O failures as [Result]. */
    suspend fun extractText(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val mimeType = appContext.contentResolver.getType(uri)
            val name = attachmentDisplayName(appContext, uri).orEmpty()
            val result = when (formatFor(mimeType, name)) {
                DocumentFormat.PDF -> extractPdfText(appContext, uri)
                DocumentFormat.DOCX -> {
                    val input = appContext.contentResolver.openInputStream(uri)
                        ?: throw IOException("Could not open this document.")
                    extractDocxText(input)
                }
                DocumentFormat.UNSUPPORTED -> throw IOException("Choose a PDF or DOCX document.")
            }
            Result.success(result)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Result.failure(failure)
        }
    }

    companion object {
        const val PDF_MIME_TYPE = "application/pdf"
        const val DOCX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

        internal fun isSupportedDocument(mimeType: String?, displayName: String?): Boolean =
            formatFor(mimeType, displayName) != DocumentFormat.UNSUPPORTED

        internal enum class DocumentFormat { PDF, DOCX, UNSUPPORTED }

        internal fun formatFor(mimeType: String?, displayName: String?): DocumentFormat {
            val normalizedMime = mimeType.orEmpty().substringBefore(';').trim().lowercase(Locale.ROOT)
            val extension = displayName.orEmpty().substringAfterLast('.', "").lowercase(Locale.ROOT)
            return when {
                normalizedMime == PDF_MIME_TYPE || extension == "pdf" -> DocumentFormat.PDF
                normalizedMime == DOCX_MIME_TYPE || extension == "docx" -> DocumentFormat.DOCX
                else -> DocumentFormat.UNSUPPORTED
            }
        }
    }
}
