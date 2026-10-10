package com.ariai.app.data.local

import java.io.File
import java.util.UUID

/** Keeps generated images as files in app storage; messages keep only the path. */
object ImageStore {
    private var dir: File? = null

    fun init(filesDir: File) {
        dir = File(filesDir, "generated").apply { mkdirs() }
    }

    fun saveVideo(bytes: ByteArray): File {
        val target = File(requireNotNull(dir) { "ImageStore not initialised" }, "${UUID.randomUUID()}.mp4")
        target.writeBytes(bytes)
        return target
    }

    /** Total size of generated images and videos on this device. */
    fun generatedBytes(): Long = dir?.listFiles()?.sumOf { it.length() } ?: 0L

    /** Deletes every generated file. Returns how many were removed. */
    fun clearGenerated(): Int {
        val files = dir?.listFiles().orEmpty()
        files.forEach { it.delete() }
        return files.size
    }

    fun save(bytes: ByteArray): File {
        val target = File(requireNotNull(dir) { "ImageStore not initialised" }, "${UUID.randomUUID()}.png")
        target.writeBytes(bytes)
        return target
    }
}
