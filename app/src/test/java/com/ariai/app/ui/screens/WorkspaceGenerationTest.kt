package com.ariai.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceGenerationTest {
    @Test
    fun stripsOneSurroundingMarkdownCodeFence() {
        assertEquals("<h1>Ready</h1>", unwrapGeneratedWorkspaceFile("```html\n<h1>Ready</h1>\n```"))
    }

    @Test
    fun leavesPlainTextResponsesUnchangedApartFromOuterWhitespace() {
        assertEquals("body { color: red; }", unwrapGeneratedWorkspaceFile("  body { color: red; }  "))
    }
}
