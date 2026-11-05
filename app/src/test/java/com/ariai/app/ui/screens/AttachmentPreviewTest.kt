package com.ariai.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class AttachmentPreviewTest {
    @Test
    fun collapsesWhitespaceAndKeepsPreviewShort() {
        assertEquals("First paragraph Second line", attachmentTextPreview(" First  paragraph\n\tSecond line "))
        assertEquals("abcde…", attachmentTextPreview("abcdefghijk", maxChars = 6))
    }

    @Test
    fun estimatesTokensFromTheFullTextRatherThanThePreview() {
        assertEquals(0, estimateAttachmentTextTokens(""))
        assertEquals(1, estimateAttachmentTextTokens("text"))
        assertEquals(25, estimateAttachmentTextTokens("x".repeat(100)))
    }
}
