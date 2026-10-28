package com.ariai.app.data.repository

import com.ariai.app.data.models.WorkspaceFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceToolsTest {
    private val html = WorkspaceFile(
        name = "index.html",
        content = """
            <!doctype html>
            <html><head><link rel="stylesheet" href="styles.css"><script>sendSecret()</script></head>
            <body><button onclick="sendSecret()">Hello</button><img src="https://example.test/image.png">
            <iframe src="https://example.test"></iframe></body></html>
        """.trimIndent(),
        updatedAt = 1L
    )
    private val css = WorkspaceFile("styles.css", "body { color: red; }", 1L)

    @Test
    fun htmlPreviewInlinesLocalStylesAndRemovesActiveOrRemoteContent() {
        val result = renderStaticHtmlPreview(html, listOf(html, css))

        assertTrue(result.contains("body { color: red; }"))
        assertTrue(result.contains("default-src 'none'"))
        assertFalse(result.contains("<script"))
        assertFalse(result.contains("onclick"))
        assertFalse(result.contains("https://example.test"))
        assertFalse(result.contains("<iframe"))
    }

    @Test
    fun consoleOnlyInspectsTheProvidedWorkspaceFiles() {
        val files = listOf(html.copy(content = "alpha\nbeta\nAlpha"))

        assertTrue(executeWorkspaceConsole("ls", files).contains("index.html"))
        assertTrue(executeWorkspaceConsole("cat index.html", files).contains("beta"))
        assertTrue(executeWorkspaceConsole("grep alpha index.html", files).contains("3: Alpha"))
        assertTrue(executeWorkspaceConsole("help", files).contains("No shell or code execution"))
        assertTrue(executeWorkspaceConsole("sh -c 'echo unsafe'", files).contains("does not run shell commands"))
    }
}
