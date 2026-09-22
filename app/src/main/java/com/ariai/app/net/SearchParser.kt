package com.ariai.app.net

/** Pure DuckDuckGo HTML result extraction, kept separate from networking for testability. */
internal object SearchParser {
    data class SearchResult(val title: String, val url: String, val snippet: String?)

    private val resultPattern = Regex(
        """class=["'][^"']*result__a[^"']*["'][^>]*href=["']([^"']+)["'][^>]*>(.*?)</a>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val snippetPattern = Regex(
        """class=["'][^"']*result__snippet[^"']*["'][^>]*>(.*?)</(?:td|div|span)>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val tagPattern = Regex("<[^>]+>")
    private val whitespacePattern = Regex("\\s+")

    fun parseResults(body: String, limit: Int = 5): List<SearchResult> {
        if (body.isBlank() || limit <= 0) return emptyList()

        val matches = resultPattern.findAll(body).take(limit).toList()

        return matches.mapIndexed { index, match ->
            val nextStart = matches.getOrNull(index + 1)?.range?.first ?: body.length
            val block = body.substring(match.range.first, nextStart)
            val snippet = snippetPattern.find(block)?.groupValues?.getOrNull(1)?.let(::clean)

            SearchResult(
                title = clean(match.groupValues[2]),
                url = decodeUrl(match.groupValues[1]),
                snippet = snippet?.takeIf { it.isNotBlank() }
            )
        }
            .filter { it.title.isNotBlank() && it.url.isNotBlank() }
            .distinctBy { it.url }
            .toList()
    }

    fun parse(body: String, limit: Int = 5): String =
        parseResults(body, limit).joinToString("\n") { format(it.title, it.snippet) }


    /** Extracts a bounded plain-text view of a fetched HTML page for Research context. */
    fun extractText(body: String, maxChars: Int = 12000): String {
        if (body.isBlank() || maxChars <= 0) return ""
        val withoutNoise = body
            .replace(Regex("(?is)<(?:script|style|noscript|svg|nav|footer|header)[^>]*>.*?</(?:script|style|noscript|svg|nav|footer|header)>"), " ")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\\n")
            .replace(Regex("</(p|div|li|h[1-6]|article|section)>", RegexOption.IGNORE_CASE), "\\n")
        return clean(withoutNoise)
            .replace(Regex("\\n\\s*\\n+"), "\\n")
            .trim()
            .take(maxChars)
    }

    private fun format(title: String, snippet: String?): String =
        if (snippet.isNullOrBlank()) "- $title" else "- $title: $snippet"

    private fun decodeUrl(value: String): String {
        val cleaned = clean(value)
        val uddg = Regex("""[?&]uddg=([^&]+)""").find(cleaned)?.groupValues?.get(1)
        return runCatching {
            java.net.URLDecoder.decode(uddg ?: cleaned, "UTF-8")
        }.getOrDefault(cleaned)
    }

    private fun clean(value: String): String =
        value.replace(tagPattern, "")
            .replace("&amp;", "&", ignoreCase = true)
            .replace("&quot;", "\"", ignoreCase = true)
            .replace("&#39;", "'", ignoreCase = true)
            .replace("&lt;", "<", ignoreCase = true)
            .replace("&gt;", ">", ignoreCase = true)
            .replace(whitespacePattern, " ")
            .trim()
}
