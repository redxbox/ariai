package com.ariai.app.data.local

import java.io.File
import java.util.UUID

/** Keeps generated images as files in app storage; messages keep only the path. */
object ImageStore {
    private var dir: File? = null

    fun init(filesDir: File) {
        dir = File(filesDir, "generated").apply { mkdirs() }
    }

    fun save(bytes: ByteArray): File {
        val target = File(requireNotNull(dir) { "ImageStore not initialised" }, "${UUID.randomUUID()}.png")
        target.writeBytes(bytes)
        return target
    }
}
