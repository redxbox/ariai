package com.ariai.app.data.repository

import com.ariai.app.data.models.WorkspaceFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class WorkspaceArchiveTest {
    @Test
    fun `exports each workspace file as a flat utf8 zip entry`() {
        val output = ByteArrayOutputStream()

        WorkspaceArchive.writeZip(
            listOf(
                WorkspaceFile(name = "index.html", content = "<h1>Hello</h1>", updatedAt = 0L),
                WorkspaceFile(name = "notes.txt", content = "سلام AriAI", updatedAt = 0L)
            ),
            output
        )

        val entries = mutableMapOf<String, String>()
        ZipInputStream(output.toByteArray().inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                assertTrue(!entry.name.startsWith("/"))
                assertTrue(!entry.name.contains(".."))
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
            }
        }

        assertEquals(mapOf("index.html" to "<h1>Hello</h1>", "notes.txt" to "سلام AriAI"), entries)
    }

    @Test
    fun `rejects path traversal names before writing an archive`() {
        val output = ByteArrayOutputStream()

        val failure = runCatching {
            WorkspaceArchive.writeZip(listOf(WorkspaceFile(name = "../outside.txt", content = "no", updatedAt = 0L)), output)
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals(0, output.size())
    }
}
