package com.ariai.app.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class WorkspaceRepositoryTest {
    @Test
    fun storesOnlyFlatTextFilesAndRejectsTraversal() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sandbox = File(context.cacheDir, "workspace-test-${UUID.randomUUID()}")
        val filesDir = File(sandbox, "files")

        try {
            assertTrue(WorkspaceRepository.sanitizeFileName("index.html") == "index.html")
            assertNull(WorkspaceRepository.sanitizeFileName("../outside.txt"))
            assertNull(WorkspaceRepository.sanitizeFileName(".hidden"))

            val first = WorkspaceRepository(filesDir)
            assertTrue(first.load().isEmpty())
            val saved = first.createFile("index.html", "<h1>Private workspace</h1>")
            assertEquals("index.html", saved.name)
            assertTrue(File(filesDir, "ariai_workspace/index.html").isFile)
            assertTrue(runCatching { first.createFile("INDEX.HTML", "replace") }.exceptionOrNull() is IllegalArgumentException)

            val restored = WorkspaceRepository(filesDir).load().single()
            assertEquals("<h1>Private workspace</h1>", restored.content)
            first.saveFile("INDEX.HTML", "<h1>Updated</h1>")
            assertEquals("<h1>Updated</h1>", WorkspaceRepository(filesDir).load().single().content)
            assertTrue(first.deleteFile("index.html"))
            assertFalse(File(filesDir, "ariai_workspace/index.html").exists())
        } finally {
            sandbox.deleteRecursively()
        }
    }
}
