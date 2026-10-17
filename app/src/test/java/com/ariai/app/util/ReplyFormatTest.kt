package com.ariai.app.util

import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyFormatTest {

    @Test
    fun codeFenceBecomesSeparatePart() {
        val parts = splitReply("Intro\n```kotlin\nval x = 1\n```\nOutro")
        assertEquals(3, parts.size)
        val code = parts[1] as ReplyPart.Code
        assertEquals("kotlin", code.language)
        assertEquals("val x = 1", code.code)
    }

    @Test
    fun unclosedFenceKeepsRestAsCode() {
        val parts = splitReply("```python\nprint(1)\nprint(2)")
        assertEquals(1, parts.size)
        assertEquals("print(1)\nprint(2)", (parts[0] as ReplyPart.Code).code)
    }

    @Test
    fun boldMarkersAreRemoved() {
        assertEquals("bold text", formatReply("**bold text**").text)
    }

    @Test
    fun headingsAreStyledAndSeparatedFromParagraphs() {
        val formatted = formatReply("## Summary\nA short answer.\n\n### Details\nMore context.", 15f)
        assertEquals("Summary\nA short answer.\n\nDetails\nMore context.", formatted.text)
        val heading = formatted.spanStyles.first { it.start == 0 && it.end == "Summary".length }
        assertEquals(FontWeight.Bold, heading.item.fontWeight)
        assertEquals(16.95f, heading.item.fontSize.value, 0.01f)
    }

    @Test
    fun headingHashesAreRemoved() {
        assertEquals("Title", formatReply("# Title").text)
    }

    @Test
    fun markdownLinkShowsLabelAndFullAddress() {
        assertEquals("Docs (https://a.dev)", formatReply("[Docs](https://a.dev)").text)
    }

    @Test
    fun bareLinkIsKeptAsIs() {
        assertEquals("Visit https://a.dev now", formatReply("Visit https://a.dev now").text)
    }

    @Test
    fun tableRowBecomesOneLine() {
        val text = formatReply("| Name | Price |\n|---|---|\n| A | 1 |").text
        assertTrue(text, text.contains("Name  ·  Price"))
        assertTrue(text, text.contains("A  ·  1"))
        assertTrue(!text.contains("|"))
    }

    @Test
    fun bulletsAndNumberedListsAreNormalized() {
        assertEquals("•  one\n•  two", formatReply("- one\n* two").text)
        assertEquals("1.  first\n2.  second", formatReply("1) first\n2. second").text)
    }
}
