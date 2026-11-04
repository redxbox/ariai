package com.ariai.app.data.repository

import com.ariai.app.data.models.WorkspaceFile
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Writes only validated, flat workspace files into a ZIP archive. */
object WorkspaceArchive {
    fun writeZip(files: List<WorkspaceFile>, output: OutputStream) {
        require(files.size <= WorkspaceRepository.MAX_FILES) { "The workspace contains too many files to export." }
        val names = mutableSetOf<String>()
        val entries = files.map { file ->
            val name = WorkspaceRepository.sanitizeFileName(file.name)
                ?: throw IllegalArgumentException("The workspace contains an invalid file name.")
            require(names.add(name.lowercase())) { "The workspace contains duplicate file names." }
            val bytes = file.content.toByteArray(StandardCharsets.UTF_8)
            require(bytes.size <= WorkspaceRepository.MAX_FILE_BYTES) { "A workspace file exceeds the export size limit." }
            name to bytes
        }
        require(entries.sumOf { it.second.size.toLong() } <= WorkspaceRepository.MAX_TOTAL_BYTES) {
            "The workspace exceeds the export size limit."
        }

        ZipOutputStream(output, StandardCharsets.UTF_8).use { zip ->
            entries.sortedBy { it.first.lowercase() }.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
    }
}
