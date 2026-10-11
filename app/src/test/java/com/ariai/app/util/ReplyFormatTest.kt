package com.ariai.app.util

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
    fun bulletsBecomeDots() {
        assertEquals("•  one\n•  two", formatReply("- one\n- two").text)
    }
}
