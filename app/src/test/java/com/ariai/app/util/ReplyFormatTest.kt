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
    fun literalAsterisksArePreservedOutsideMarkdownEmphasis() {
        assertEquals("Multiply 2 * 3 = 6; use * as a wildcard.", formatReply("Multiply 2 * 3 = 6; use * as a wildcard.").text)
        assertEquals("2*3*4", formatReply("2*3*4").text)
        assertEquals("foo_bar_baz", formatReply("foo_bar_baz").text)
        assertEquals("The symbol is *.", formatReply("The symbol is `*`.").text)
        assertEquals("Keep ** as code.", formatReply("Keep `**` as code.").text)
    }

    @Test
    fun singleAsteriskEmphasisRemainsStyledWithoutLeakingMarkers() {
        assertEquals("italic", formatReply("*italic*").text)
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
