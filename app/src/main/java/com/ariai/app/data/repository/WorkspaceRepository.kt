package com.ariai.app.data.repository

import android.util.AtomicFile
import com.ariai.app.data.models.WorkspaceFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Stores small text files in app-private storage. The repository deliberately has no shell,
 * process execution, public-storage access, or file-path API.
 */
class WorkspaceRepository(filesDir: File) {
    private val root = File(filesDir, "ariai_workspace").canonicalFile
    private val mutex = Mutex()
    private val _files = MutableStateFlow<List<WorkspaceFile>>(emptyList())
    private var loaded = false

    val files = _files.asStateFlow()

    suspend fun load(): List<WorkspaceFile> = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureLoadedLocked()
            _files.value
        }
    }

    suspend fun saveFile(rawName: String, content: String): WorkspaceFile = writeFile(rawName, content, createOnly = false)

    suspend fun createFile(rawName: String, content: String): WorkspaceFile = writeFile(rawName, content, createOnly = true)

    private suspend fun writeFile(rawName: String, content: String, createOnly: Boolean): WorkspaceFile = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureLoadedLocked()
            val requestedName = sanitizeFileName(rawName) ?: throw IllegalArgumentException(
                "Use a file name with letters or numbers, spaces, dots, underscores, or hyphens."
            )
            val matchingExisting = _files.value.firstOrNull { it.name.equals(requestedName, ignoreCase = true) }
            if (createOnly && matchingExisting != null) throw IllegalArgumentException("A file with this name already exists.")
            val name = matchingExisting?.name ?: requestedName
            if (content.length > MAX_FILE_BYTES) throw IllegalArgumentException("Files must be 256 KB or smaller.")
            val bytes = content.toByteArray(StandardCharsets.UTF_8)
            if (bytes.size > MAX_FILE_BYTES) throw IllegalArgumentException("Files must be 256 KB or smaller.")
            if (matchingExisting == null && _files.value.size >= MAX_FILES) {
                throw IllegalStateException("The workspace can contain at most $MAX_FILES files.")
            }
            val totalAfterSave = _files.value.sumOf { file ->
                if (file.name == name) bytes.size.toLong() else file.content.toByteArray(StandardCharsets.UTF_8).size.toLong()
            } + if (matchingExisting == null) bytes.size.toLong() else 0L
            if (totalAfterSave > MAX_TOTAL_BYTES) throw IllegalStateException("The workspace storage limit is 2 MB.")

            val target = File(root, name).canonicalFile
            require(target.parentFile == root) { "The file name is outside the workspace." }
            val atomic = AtomicFile(target)
            val output = atomic.startWrite()
            try {
                output.write(bytes)
                atomic.finishWrite(output)
            } catch (error: Exception) {
                atomic.failWrite(output)
                throw error
            }
            val file = WorkspaceFile(name = name, content = content, updatedAt = target.lastModified())
            refreshFilesLocked()
            _files.value.firstOrNull { it.name == name } ?: file
        }
    }

    suspend fun deleteFile(rawName: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureLoadedLocked()
            val name = sanitizeFileName(rawName) ?: return@withLock false
            val target = File(root, name).canonicalFile
            if (target.parentFile != root || !target.isFile) return@withLock false
            AtomicFile(target).delete()
            refreshFilesLocked()
            true
        }
    }

    private fun ensureLoadedLocked() {
        if (loaded) return
        if (!root.exists() && !root.mkdirs()) throw IOException("Could not create the local workspace.")
        refreshFilesLocked()
        loaded = true
    }

    private fun refreshFilesLocked() {
        if (!root.exists() && !root.mkdirs()) throw IOException("Could not open the local workspace.")
        _files.value = root.listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.isFile && !it.isHidden && runCatching { it.canonicalFile.parentFile == root }.getOrDefault(false) }
            .mapNotNull { file ->
                val name = sanitizeFileName(file.name) ?: return@mapNotNull null
                runCatching {
                    val bytes = file.readBytes()
                    if (bytes.size > MAX_FILE_BYTES) null
                    else WorkspaceFile(name, String(bytes, StandardCharsets.UTF_8), file.lastModified())
                }.getOrNull()
            }
            .sortedBy { it.name.lowercase() }
            .toList()
    }

    companion object {
        const val MAX_FILE_BYTES = 256 * 1024
        const val MAX_TOTAL_BYTES = 2 * 1024 * 1024
        const val MAX_FILES = 80
        private val FILE_NAME_PATTERN = Regex("^[\\p{L}\\p{N}][\\p{L}\\p{N}._ -]{0,63}$")

        /** Allows only a flat, human-readable file name; path segments and hidden files are rejected. */
        fun sanitizeFileName(rawName: String): String? {
            val name = rawName.trim()
            if (name == "." || name == ".." || !FILE_NAME_PATTERN.matches(name)) return null
            return name
        }
    }
}
