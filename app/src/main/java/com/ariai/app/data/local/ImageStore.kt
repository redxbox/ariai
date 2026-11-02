package com.ariai.app.data.local

import com.ariai.app.data.models.AttachmentType
import java.io.File
import java.util.UUID

/** Stores generated media, user attachments, and short-lived edit inputs separately. */
object ImageStore {
    private var generatedDir: File? = null
    private var attachmentDir: File? = null
    private var tempDir: File? = null

    fun init(filesDir: File) {
        generatedDir = File(filesDir, "generated").apply { mkdirs() }
        attachmentDir = File(filesDir, "attachments").apply { mkdirs() }
        tempDir = File(filesDir, "media-temp").apply { mkdirs() }
    }

    fun saveAudio(bytes: ByteArray): File = write(generatedDirectory(), "${UUID.randomUUID()}.mp3", bytes)

    fun saveVideo(bytes: ByteArray): File = write(generatedDirectory(), "${UUID.randomUUID()}.mp4", bytes)

    /** Saves a model-generated image, preserving the common formats returned by providers. */
    fun save(bytes: ByteArray): File = saveGeneratedImage(bytes, detectImageMimeType(bytes))

    fun saveGeneratedImage(bytes: ByteArray, mimeType: String): File {
        val ext = extensionFor(mimeType)
        return write(generatedDirectory(), "${UUID.randomUUID()}.$ext", bytes)
    }

    /** Persists user-supplied image/video bytes outside the generated-media directory. */
    fun saveAttachment(bytes: ByteArray, mimeType: String, type: AttachmentType): File {
        val ext = when (type) {
            AttachmentType.VIDEO -> "mp4"
            AttachmentType.PDF -> "pdf"
            AttachmentType.DOCX -> "docx"
            AttachmentType.TEXT -> "txt"
            AttachmentType.IMAGE, AttachmentType.AUDIO -> extensionFor(mimeType)
        }
        return write(attachmentDirectory(), "${UUID.randomUUID()}.$ext", bytes)
    }

    /** Creates a temporary input file for an image-edit request; callers delete it afterward. */
    fun createTempImage(bytes: ByteArray, mimeType: String): File =
        write(tempDirectory(), "${UUID.randomUUID()}.${extensionFor(mimeType)}", bytes)

    fun deleteTemp(file: File) {
        if (isInside(tempDirectory(), file)) file.delete()
    }

    /** Removes a path only when it belongs to AriAi's private media directories. */
    fun deleteOwnedFile(path: String) {
        val file = runCatching { File(path).canonicalFile }.getOrNull() ?: return
        if (isInside(generatedDir, file) || isInside(attachmentDir, file) || isInside(tempDir, file)) {
            file.delete()
        }
    }

    /** Total generated-media size, excluding legacy attachment files still in this directory. */
    fun generatedBytes(excludedPaths: Set<String> = emptySet()): Long {
        val excluded = canonicalPaths(excludedPaths)
        return generatedDir?.listFiles().orEmpty()
            .filterNot { file -> runCatching { file.canonicalPath }.getOrNull()?.let(excluded::contains) == true }
            .sumOf { it.length() }
    }

    /** Deletes generated media only; paths still used as attachments are preserved. */
    fun clearGenerated(excludedPaths: Set<String> = emptySet()): Int {
        val excluded = canonicalPaths(excludedPaths)
        val files = generatedDir?.listFiles().orEmpty()
            .filterNot { file -> runCatching { file.canonicalPath }.getOrNull()?.let(excluded::contains) == true }
        return files.count { it.delete() }
    }

    private fun write(directory: File, name: String, bytes: ByteArray): File {
        val target = File(directory, name)
        try {
            target.outputStream().buffered().use { it.write(bytes) }
            return target
        } catch (e: Exception) {
            target.delete()
            throw e
        }
    }

    private fun generatedDirectory(): File = requireNotNull(generatedDir) { "ImageStore not initialised" }
    private fun attachmentDirectory(): File = requireNotNull(attachmentDir) { "ImageStore not initialised" }
    private fun tempDirectory(): File = requireNotNull(tempDir) { "ImageStore not initialised" }

    private fun detectImageMimeType(bytes: ByteArray): String = when {
        bytes.size >= 8 && bytes.sliceArray(0..7).contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)) -> "image/png"
        bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() && bytes[2] == 0xff.toByte() -> "image/jpeg"
        bytes.size >= 12 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" && String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"
        bytes.size >= 6 && (String(bytes, 0, 6, Charsets.US_ASCII) == "GIF87a" || String(bytes, 0, 6, Charsets.US_ASCII) == "GIF89a") -> "image/gif"
        else -> "image/png"
    }

    private fun extensionFor(mimeType: String): String = when (mimeType.lowercase().substringBefore(';')) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "image/png" -> "png"
        "audio/mpeg", "audio/mp3" -> "mp3"
        "audio/wav", "audio/x-wav" -> "wav"
        "audio/ogg" -> "ogg"
        "audio/aac" -> "aac"
        "audio/mp4", "audio/x-m4a" -> "m4a"
        "video/mp4" -> "mp4"
        "application/pdf" -> "pdf"
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
        "text/plain" -> "txt"
        else -> "bin"
    }

    private fun canonicalPaths(paths: Set<String>): Set<String> = paths.mapNotNullTo(mutableSetOf()) {
        runCatching { File(it).canonicalPath }.getOrNull()
    }

    private fun isInside(directory: File?, file: File): Boolean {
        val base = directory?.let { runCatching { it.canonicalFile.path }.getOrNull() } ?: return false
        val candidate = runCatching { file.canonicalFile.path }.getOrNull() ?: return false
        return candidate.startsWith(base + File.separator)
    }
}
