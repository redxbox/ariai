package com.ariai.app.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchParserTest {
    @Test fun parsesTitlesAndSnippets() {
        val html = """
            <a class="result__a" href="https://example.com/one">First &amp; Result</a>
            <td class="result__snippet">A useful <b>summary</b>.</td>
            <a class="result__a" href="https://example.com/two">Second</a>
            <div class="result__snippet">Another &quot;summary&quot;.</div>
        """.trimIndent()

        assertEquals(
            """- First & Result: A useful summary.
- Second: Another "summary".""",
            SearchParser.parse(html)
        )
    }

    @Test fun exposesStructuredUrls() {
        val html = """<a class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Fpage%3Fa%3D1">Example</a>"""
        val result = SearchParser.parseResults(html).single()

        assertEquals("Example", result.title)
        assertEquals("https://example.com/page?a=1", result.url)
        assertTrue(result.snippet == null)
    }

    @Test fun acceptsSingleQuotedAttributesAndDeduplicatesUrls() {
        val html = """
            <a class='result__a' href='https://example.com'>One</a>
            <a class='result__a' href='https://example.com'>Duplicate</a>
            <a class='result__a' href='https://example.org'>Two</a>
        """.trimIndent()

        val results = SearchParser.parseResults(html, limit = 5)
        assertEquals(2, results.size)
        assertEquals("One", results.first().title)
    }

    @Test fun keepsSnippetsAttachedToTheirOwnResult() {
        val html = """
            <div class="result"><a class="result__a" href="https://example.com/one">First</a></div>
            <div class="result"><a class="result__a" href="https://example.com/two">Second</a>
            <div class="result__snippet">Second only</div></div>
        """.trimIndent()

        val results = SearchParser.parseResults(html)
        assertEquals(null, results[0].snippet)
        assertEquals("Second only", results[1].snippet)
    }

    @Test fun extractsReadablePageTextAndRemovesNoise() {
        val html = """
            <html>
              <header>Navigation</header>
              <article>
                <h1>Research title</h1>
                <p>First paragraph with <b>useful</b> text.</p>
                <script>ignoreMe()</script>
                <div>Second paragraph.</div>
              </article>
              <footer>Footer links</footer>
            </html>
        """.trimIndent()

        val text = SearchParser.extractText(html)

        assertTrue(text.contains("Research title"))
        assertTrue(text.contains("First paragraph with useful text."))
        assertTrue(text.contains("Second paragraph."))
        assertTrue(!text.contains("ignoreMe"))
        assertTrue(!text.contains("Navigation"))
        assertTrue(!text.contains("Footer links"))
    }

    @Test fun boundsExtractedPageText() {
        val text = SearchParser.extractText("<p>abcdefghij</p>", maxChars = 5)
        assertEquals("abcde", text)
    }

    @Test fun omitsMissingSnippetWithoutDanglingColon() {
        val html = """<a class="result__a" href="https://example.com">Only title</a>"""
        assertEquals("- Only title", SearchParser.parse(html))
    }

    @Test fun respectsLimitAndEmptyInput() {
        val html = """
            <a class="result__a" href="https://example.com/1">One</a>
            <a class="result__a" href="https://example.com/2">Two</a>
            <a class="result__a" href="https://example.com/3">Three</a>
        """.trimIndent()

        assertEquals(
            """- One
- Two""",
            SearchParser.parse(html, limit = 2)
        )
        assertTrue(SearchParser.parseResults("", 5).isEmpty())
        assertEquals("", SearchParser.parse(html, limit = 0))
    }
}
