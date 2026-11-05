package com.ariai.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates which reply inputs are routed to the offline KaTeX/Mermaid renderer.
 * Math delimiters and Mermaid fences must be detected while plain prose, currency,
 * and math-looking samples inside normal code fences stay on the native renderer.
 */
class LocalRichMarkdownValidationTest {

    @Test
    fun detectsInlineAndDisplayMathDelimiters() {
        assertTrue(hasLocalRichMarkdown("Euler: \$e^{i\\pi}+1=0\$"))
        assertTrue(hasLocalRichMarkdown("Energy \$E=mc^2\$ is famous."))
        assertTrue(
            hasLocalRichMarkdown(
                "\$\$\n\\int_0^1 x^2\\,dx = \\frac{1}{3}\n\$\$"
            )
        )
        assertTrue(hasLocalRichMarkdown("Bracket forms: \\[a+b\\] and \\(c+d\\)"))
    }

    @Test
    fun rejectsPlainCurrencyLoneDollarsAndUnpairedMarkers() {
        assertFalse(hasLocalRichMarkdown("The price is \$5 today."))
        assertFalse(hasLocalRichMarkdown("Cost: \$ per hour, nothing else."))
        assertFalse(hasLocalRichMarkdown("Lone \$ sign in a sentence."))
        assertFalse(hasLocalRichMarkdown("Use \$\$ for double-dollar fences."))
        assertFalse(hasLocalRichMarkdown("No math delimiters in this reply at all."))
    }

    @Test
    fun escapedDollarSignsAreNotTreatedAsMath() {
        assertFalse(hasLocalRichMarkdown("Escaped \$5 and \$6 stay plain text."))
        assertFalse(hasLocalRichMarkdown("Path C:\\temp\\\$file is not math."))
    }

    @Test
    fun mermaidAndLatexFencesRouteToRendererButOtherCodeDoesNot() {
        assertTrue(
            hasLocalRichMarkdown(
                "```mermaid\nsequenceDiagram\nAlice->>Bob: hello\n```"
            )
        )
        assertTrue(hasLocalRichMarkdown("```latex\nE = mc^2\n```"))
        assertTrue(hasLocalRichMarkdown("```math\na^2 + b^2 = c^2\n```"))
        assertFalse(
            hasLocalRichMarkdown(
                "```kotlin\nval x = \"\$x\$\"\n```"
            )
        )
        assertFalse(
            hasLocalRichMarkdown(
                "Intro text only.\n```python\nprint('\$x')\n```"
            )
        )
        assertFalse(hasLocalRichMarkdown("```text\n\\frac{1}{2}\n```"))
    }

    @Test
    fun mermaidFenceIsParsedAsACodePartWithItsLanguage() {
        val source = "Flow:\n```mermaid\ngraph TD; A-->B\n```"
        val parts = splitReply(source)
        assertEquals(2, parts.size)

        val code = parts[1] as ReplyPart.Code
        assertEquals("mermaid", code.language)
        assertEquals("graph TD; A-->B", code.code.trim())
        assertTrue(code.complete)
        assertTrue(hasLocalRichMarkdown(source))
    }

    @Test
    fun streamingCacheKeepsMermaidLanguageForCompletedFences() {
        val cache = ReplyBlockCache()
        val open = cache.parse("```mermaid\ngraph TD; A", streaming = true)
        assertFalse((open.last() as ReplyPart.Code).complete)

        val done = cache.parse("```mermaid\ngraph TD; A-->B\n```", streaming = true)
        val code = done.last() as ReplyPart.Code
        assertEquals("mermaid", code.language)
        assertTrue(code.complete)
    }
}
