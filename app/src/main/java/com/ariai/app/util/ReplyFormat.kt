package com.ariai.app.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/** A reply is a list of normal text paragraphs and code blocks. */
sealed class ReplyPart {
    data class Text(val raw: String) : ReplyPart()
    data class Code(val language: String, val code: String, val complete: Boolean = true) : ReplyPart()
}

private data class ReplyLine(
    val text: String,
    val headingLevel: Int = 0,
    val listIndent: Int? = null,
    val isBlank: Boolean = false
)

private val boldRegex = Regex("""\*\*(.+?)\*\*|__(.+?)__""")
private val italicRegex = Regex("""(?<![\p{L}\p{N}])\*(?=\S)(.+?)(?<=\S)\*(?![\p{L}\p{N}*])|(?<![\p{L}\p{N}_])_(?=\S)(.+?)(?<=\S)_(?![\p{L}\p{N}_])""")
private val codeRegex = Regex("""`([^`]+)`""")
private val codePlaceholder = Regex("\\uE000(\\d+)\\uE001")
private val linkOrUrl = Regex("""\[([^\]]+)\]\((https?://[^)\s]+)\)|(https?://[^\s)\]]+)""")
private val tableSeparator = Regex("""^\|?[\s:|-]+\|?$""")
private val ruleLine = Regex("""^(-{3,}|\*{3,}|_{3,})$""")
private val bulletPrefix = Regex("""^[-*+]\s+""")
private val numberedPrefix = Regex("""^([\p{Nd}]+)[.)]\s+""")
private val headingPrefix = Regex("""^(#{1,6})\s*(.*)$""")
private val linkColor = Color(0xFF2563EB)
private const val URL_TRAILING_PUNCTUATION = ".,;:!?،؛؟…"

private val localMathDelimiter = Regex(
    """\$\$[\s\S]+?\$\$|(?<!\\)\$(?!\s)[^$\n]+?(?<!\\)\$(?!\$)|\\\[[\s\S]+?\\\]|\\\([\s\S]+?\\\)"""
)

/** True when a completed reply needs the bundled offline KaTeX/Mermaid renderer. */
internal fun hasLocalRichMarkdown(raw: String): Boolean {
    val parts = splitReply(raw)
    if (parts.filterIsInstance<ReplyPart.Code>().any { part ->
        part.language.trim().lowercase() in setOf("mermaid", "math", "latex", "tex")
    }) return true
    return parts.filterIsInstance<ReplyPart.Text>().any { localMathDelimiter.containsMatchIn(it.raw) }
}

private fun isListItemLine(line: String): Boolean {
    val trimmed = line.trimStart()
    return bulletPrefix.containsMatchIn(trimmed) || numberedPrefix.containsMatchIn(trimmed)
}

/** Splits a reply into text paragraphs and fenced code blocks. */
fun splitReply(raw: String): List<ReplyPart> {
    val parts = mutableListOf<ReplyPart>()
    val text = StringBuilder()
    val code = StringBuilder()
    val sourceLines = raw.lines()
    val nextNonBlankLine = IntArray(sourceLines.size) { -1 }
    var nextIndex = -1
    for (index in sourceLines.indices.reversed()) {
        if (sourceLines[index].isNotBlank()) nextIndex = index
        nextNonBlankLine[index] = nextIndex
    }
    var lastTextLine = ""
    var inCode = false
    var language = ""
    fun flushText() {
        if (text.isNotBlank()) parts.add(ReplyPart.Text(text.toString()))
        text.clear()
        lastTextLine = ""
    }
    for ((index, line) in sourceLines.withIndex()) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            if (inCode) {
                parts.add(ReplyPart.Code(language, code.toString().trimEnd('\n'), complete = true))
                code.clear()
                inCode = false
            } else {
                flushText()
                inCode = true
                language = trimmed.removePrefix("```").trim()
            }
        } else if (inCode) {
            code.append(line).append('\n')
        } else if (trimmed.isEmpty()) {
            val nextLine = nextNonBlankLine[index].takeIf { it > index }?.let(sourceLines::get).orEmpty()
            if (!(isListItemLine(lastTextLine) && isListItemLine(nextLine))) {
                // Keep separate list items in one tight block, but split real paragraphs into
                // compact blocks so a Markdown blank line never becomes a blank text line.
                flushText()
            }
        } else {
            text.append(line).append('\n')
            lastTextLine = line
        }
    }
    if (inCode) parts.add(ReplyPart.Code(language, code.toString().trimEnd('\n'), complete = false)) else flushText()
    return parts
}

/** Caches completed Markdown blocks while reparsing only the trailing streaming block. */
class ReplyBlockCache {
    private var committedRaw = ""
    private var committedParts = emptyList<ReplyPart>()

    fun parse(raw: String, streaming: Boolean): List<ReplyPart> {
        if (!streaming) {
            reset()
            return splitReply(raw)
        }
        if (!raw.startsWith(committedRaw)) reset()

        val stableBoundary = stableReplyBoundary(raw, committedRaw.length)
        if (stableBoundary > committedRaw.length) {
            val addition = raw.substring(committedRaw.length, stableBoundary)
            committedParts = committedParts + splitReply(addition)
            committedRaw = raw.substring(0, stableBoundary)
        }
        return committedParts + splitReply(raw.substring(committedRaw.length))
    }

    private fun reset() {
        committedRaw = ""
        committedParts = emptyList()
    }

    private fun stableReplyBoundary(raw: String, startOffset: Int): Int {
        data class Line(val start: Int, val end: Int, val text: String, val terminated: Boolean)
        val lines = mutableListOf<Line>()
        var cursor = startOffset
        while (cursor < raw.length) {
            val newline = raw.indexOf('\n', cursor)
            val end = if (newline >= 0) newline + 1 else raw.length
            val contentEnd = if (newline >= 0 && newline > cursor && raw[newline - 1] == '\r') newline - 1
                else if (newline >= 0) newline else raw.length
            lines += Line(cursor, end, raw.substring(cursor, contentEnd), newline >= 0)
            cursor = end
        }

        val nextNonBlankLines = arrayOfNulls<Line>(lines.size)
        var nextNonBlank: Line? = null
        for (index in lines.indices.reversed()) {
            nextNonBlankLines[index] = nextNonBlank
            if (lines[index].text.isNotBlank()) nextNonBlank = lines[index]
        }

        var inCode = false
        var previousNonBlank: String? = null
        var stableBoundary = startOffset
        for (index in lines.indices) {
            val line = lines[index]
            // The final unterminated line may still grow into a fence or list marker.
            if (!line.terminated) break
            val trimmed = line.text.trim()
            if (trimmed.startsWith("```")) {
                if (inCode) {
                    inCode = false
                    stableBoundary = line.end
                } else {
                    // Everything before a complete opening-fence line is now immutable.
                    stableBoundary = maxOf(stableBoundary, line.start)
                    inCode = true
                }
                previousNonBlank = null
                continue
            }
            if (inCode) continue
            if (trimmed.isBlank()) {
                val nextLine = nextNonBlankLines[index]
                val previous = previousNonBlank
                if (previous != null && nextLine != null && !(isListItemLine(previous) && isListItemLine(nextLine.text))) {
                    stableBoundary = maxOf(stableBoundary, nextLine.start)
                }
            } else {
                previousNonBlank = line.text
            }
        }
        return stableBoundary.coerceAtMost(raw.length)
    }
}

/**
 * Turns Markdown into mobile-friendly rich text: section headings gain hierarchy, lists wrap with
 * a hanging indent, tables become compact rows, and links remain tappable.
 */
fun formatReply(raw: String, baseFontSizeSp: Float = 15f): AnnotatedString {
    val lines = mutableListOf<ReplyLine>()
    var pendingBlank = false
    for (original in raw.lines()) {
        var line = original.trim()
        if (line.isEmpty()) {
            pendingBlank = lines.isNotEmpty()
            continue
        }
        if (ruleLine.matches(line) || (tableSeparator.matches(line) && line.contains('-'))) continue

        var headingLevel = 0
        headingPrefix.find(line)?.let { heading ->
            headingLevel = heading.groupValues[1].length
            line = heading.groupValues[2].trim()
        }
        if (line.startsWith(">")) line = line.removePrefix(">").trim()

        var listIndent: Int? = null
        val sourceIndent = original.takeWhile { it == ' ' || it == '\t' }.length / 2
        val numbered = numberedPrefix.find(line)
        val bullet = bulletPrefix.find(line)
        when {
            numbered != null -> {
                line = "${numbered.groupValues[1]}.  " + line.removeRange(numbered.range)
                listIndent = sourceIndent
            }
            bullet != null -> {
                line = "•  " + line.removeRange(bullet.range)
                listIndent = sourceIndent
            }
        }
        if (line.startsWith("|") && line.endsWith("|")) {
            line = line.trim('|').split("|").map { it.trim() }.filter { it.isNotEmpty() }.joinToString("  ·  ")
        }
        if (line.isEmpty()) continue
        if (pendingBlank) {
            val previousContentLine = lines.lastOrNull { !it.isBlank }
            val separatesListItems = previousContentLine?.listIndent != null && listIndent != null
            if (!separatesListItems) lines.add(ReplyLine(text = "", isBlank = true))
            pendingBlank = false
        }
        lines.add(ReplyLine(line, headingLevel, listIndent))
    }

    return buildAnnotatedString {
        lines.forEachIndexed { index, line ->
            if (index > 0) append("\n")
            if (line.isBlank) return@forEachIndexed

            val paragraphStyle = line.listIndent?.let { indent ->
                val firstLine = (baseFontSizeSp * 1.15f * indent).sp
                val continuation = (baseFontSizeSp * 1.35f * (indent + 1)).sp
                ParagraphStyle(textIndent = TextIndent(firstLine = firstLine, restLine = continuation))
            } ?: ParagraphStyle()
            withStyle(paragraphStyle) {
                if (line.headingLevel > 0) {
                    val scale = when (line.headingLevel) {
                        1 -> 1.20f
                        2 -> 1.13f
                        else -> 1.06f
                    }
                    withStyle(SpanStyle(fontSize = (baseFontSizeSp * scale).sp, fontWeight = FontWeight.Bold)) {
                        appendInline(line.text)
                    }
                } else {
                    appendInline(line.text)
                }
            }
        }
    }
}

private fun AnnotatedString.Builder.appendInline(line: String) {
    val inlineCode = mutableListOf<String>()
    val cleaned = codeRegex.replace(line) { match ->
        val index = inlineCode.size
        inlineCode += match.groupValues[1]
        "\uE000${index}\uE001"
    }
    var last = 0
    for (match in linkOrUrl.findAll(cleaned)) {
        appendBold(cleaned.substring(last, match.range.first), inlineCode)
        val isMarkdownLink = match.groupValues[2].isNotEmpty()
        val rawUrl = match.groupValues[2].ifEmpty { match.groupValues[3] }
        // Punctuation inside Markdown parentheses is part of its explicit target; only detach
        // sentence punctuation that follows a bare URL.
        val (url, punctuation) = if (isMarkdownLink) rawUrl to "" else trimUrlPunctuation(rawUrl)
        val domain = domainLabel(url)
        // Markdown labels and bare URLs share the same compact domain-only presentation.
        // Keep the complete destination tappable while hiding its path and query.
        val displayLabel = domain
        pushStringAnnotation(tag = "URL", annotation = url)
        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
            appendBold(displayLabel, inlineCode)
        }
        pop()
        append(punctuation)
        last = match.range.last + 1
    }
    appendBold(cleaned.substring(last), inlineCode)
}

private fun trimUrlPunctuation(value: String): Pair<String, String> {
    var end = value.length
    while (end > 0 && URL_TRAILING_PUNCTUATION.contains(value[end - 1])) end--
    return value.substring(0, end) to value.substring(end)
}

private fun domainLabel(url: String): String {
    val uri = runCatching { java.net.URI(url) }.getOrNull()
    if (uri != null) {
        val host = uri.host?.lowercase(java.util.Locale.ROOT)?.removePrefix("www.")
        if (!host.isNullOrBlank()) {
            val defaultPort = (uri.scheme.equals("https", ignoreCase = true) && uri.port == 443) ||
                (uri.scheme.equals("http", ignoreCase = true) && uri.port == 80)
            return host + if (uri.port >= 0 && !defaultPort) ":${uri.port}" else ""
        }
    }
    return url.substringAfter("://", url)
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .removePrefix("www.")
}

private fun AnnotatedString.Builder.appendBold(segment: String, inlineCode: List<String>) {
    var last = 0
    for (m in boldRegex.findAll(segment)) {
        appendItalic(segment.substring(last, m.range.first), inlineCode)
        val inner = m.groupValues[1].ifEmpty { m.groupValues[2] }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendItalic(inner, inlineCode) }
        last = m.range.last + 1
    }
    appendItalic(segment.substring(last), inlineCode)
}

private fun AnnotatedString.Builder.appendItalic(segment: String, inlineCode: List<String>) {
    var last = 0
    for (code in codePlaceholder.findAll(segment)) {
        appendItalicText(segment.substring(last, code.range.first))
        val index = code.groupValues[1].toInt()
        append(inlineCode[index])
        last = code.range.last + 1
    }
    appendItalicText(segment.substring(last))
}

private fun AnnotatedString.Builder.appendItalicText(segment: String) {
    var last = 0
    for (m in italicRegex.findAll(segment)) {
        append(segment.substring(last, m.range.first))
        val inner = m.groupValues[1].ifEmpty { m.groupValues[2] }
        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(inner) }
        last = m.range.last + 1
    }
    append(segment.substring(last))
}
