package com.ariai.app.util

import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyFormatTest {

    @Test
    fun localRendererDetectionFindsMathAndMermaidButIgnoresCodeSamples() {
        assertTrue(hasLocalRichMarkdown("Inline \$x^2\$ and display \$\$y=mx+b\$\$"))
        assertTrue(hasLocalRichMarkdown("""\(a+b\)"""))
        assertTrue(hasLocalRichMarkdown("```mermaid\ngraph TD; A-->B\n```"))
        assertTrue(!hasLocalRichMarkdown("```text\n\$x\$\n```"))
        assertTrue(!hasLocalRichMarkdown("The price is \$5 today."))
    }
    @Test
    fun codeFenceBecomesSeparatePart() {
        val parts = splitReply("Intro\n```kotlin\nval x = 1\n```\nOutro")
        assertEquals(3, parts.size)
        val code = parts[1] as ReplyPart.Code
        assertEquals("kotlin", code.language)
        assertEquals("val x = 1", code.code)
        assertTrue(code.complete)
    }

    @Test
    fun blankLinesSplitParagraphBlocksButKeepListItemsTight() {
        val parts = splitReply("First paragraph.\ncontinued.\n\n- one\n\n- two\n\nFinal paragraph.")
        assertEquals(3, parts.size)
        assertEquals("First paragraph.\ncontinued.\n", (parts[0] as ReplyPart.Text).raw)
        assertEquals("- one\n- two\n", (parts[1] as ReplyPart.Text).raw)
        assertEquals("Final paragraph.\n", (parts[2] as ReplyPart.Text).raw)

        val numbered = splitReply("1. first\n\n2. second")
        assertEquals(1, numbered.size)
        assertEquals("1. first\n2. second\n", (numbered.single() as ReplyPart.Text).raw)

        val persianNumbered = splitReply("۱. مورد اول\n\n۲. مورد دوم")
        assertEquals(1, persianNumbered.size)
        assertEquals("۱. مورد اول\n۲. مورد دوم\n", (persianNumbered.single() as ReplyPart.Text).raw)
    }

    @Test
    fun streamingReplyCacheKeepsCompletedParagraphsAndFencedBlocksStable() {
        val cache = ReplyBlockCache()
        val first = cache.parse("Intro paragraph.\n\nSecond", streaming = true)
        val second = cache.parse("Intro paragraph.\n\nSecond paragraph.", streaming = true)

        assertEquals("Intro paragraph.\n", (second.first() as ReplyPart.Text).raw)
        assertEquals(first.first(), second.first())
        assertEquals("Second paragraph.\n", (second.last() as ReplyPart.Text).raw)

        val withOpenCode = cache.parse("Intro paragraph.\n\nSecond paragraph.\n```kotlin\nval x =", streaming = true)
        assertTrue(!(withOpenCode.last() as ReplyPart.Code).complete)
        val withClosedCode = cache.parse("Intro paragraph.\n\nSecond paragraph.\n```kotlin\nval x = 1\n```\n", streaming = true)
        assertTrue((withClosedCode.last() as ReplyPart.Code).complete)

        val finished = cache.parse("Intro paragraph.\n\nSecond paragraph.\n```kotlin\nval x = 1\n```\n", streaming = false)
        assertEquals(withClosedCode, finished)
    }

    @Test
    fun streamingReplyCacheKeepsConsecutiveListItemsInOneBlock() {
        val cache = ReplyBlockCache()
        cache.parse("- one\n\n- two", streaming = true)
        val parts = cache.parse("- one\n\n- two\n\nParagraph", streaming = true)

        assertEquals(2, parts.size)
        assertEquals("- one\n- two\n", (parts.first() as ReplyPart.Text).raw)
        assertEquals("Paragraph\n", (parts.last() as ReplyPart.Text).raw)
    }

    @Test
    fun blankLinesBetweenListItemsDoNotCreateOversizedVerticalGaps() {
        val formatted = formatReply("1. first\n\n2. second\n\n- third")
        assertEquals("1.  first\n2.  second\n•  third", formatted.text)

        val persianNumbered = formatReply("۱. مورد اول\n\n۲. مورد دوم")
        assertEquals("۱.  مورد اول\n۲.  مورد دوم", persianNumbered.text)
    }

    @Test
    fun unclosedFenceKeepsRestAsCode() {
        val parts = splitReply("```python\nprint(1)\nprint(2)")
        assertEquals(1, parts.size)
        assertEquals("print(1)\nprint(2)", (parts[0] as ReplyPart.Code).code)
        assertTrue(!(parts[0] as ReplyPart.Code).complete)
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
    fun markdownLinkShowsDomainOnlyAndKeepsFullClickableDestination() {
        val formatted = formatReply("[Docs](https://a.dev/guide/start?source=app)")
        assertEquals("a.dev", formatted.text)
        assertEquals("https://a.dev/guide/start?source=app", formatted.getStringAnnotations("URL", 0, formatted.length).single().item)
    }

    @Test
    fun bareLinkShowsDomainAndKeepsFullClickableDestination() {
        val formatted = formatReply("Visit https://www.github.com/redxbox/ariai. now")
        assertEquals("Visit github.com. now", formatted.text)
        assertEquals("https://www.github.com/redxbox/ariai", formatted.getStringAnnotations("URL", 0, formatted.length).single().item)
    }

    @Test
    fun markdownTargetIsPreservedExactlyWhileShowingOnlyTheDomain() {
        val formatted = formatReply("[Docs](https://a.dev/guide.)")
        assertEquals("a.dev", formatted.text)
        assertEquals("https://a.dev/guide.", formatted.getStringAnnotations("URL", 0, formatted.length).single().item)
    }

    @Test
    fun explicitDomainLabelIsNotRepeated() {
        assertEquals("github.com", formatReply("[github.com](https://github.com/redxbox/ariai)").text)
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
