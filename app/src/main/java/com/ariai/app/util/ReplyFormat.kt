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
    data class Code(val language: String, val code: String) : ReplyPart()
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
private val numberedPrefix = Regex("""^(\d+)[.)]\s+""")
private val headingPrefix = Regex("""^(#{1,6})\s*(.*)$""")
private val linkColor = Color(0xFF2563EB)

/** Splits a reply into text paragraphs and fenced code blocks. */
fun splitReply(raw: String): List<ReplyPart> {
    val parts = mutableListOf<ReplyPart>()
    val text = StringBuilder()
    val code = StringBuilder()
    var inCode = false
    var language = ""
    fun flushText() {
        if (text.isNotBlank()) parts.add(ReplyPart.Text(text.toString()))
        text.clear()
    }
    for (line in raw.lines()) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            if (inCode) {
                parts.add(ReplyPart.Code(language, code.toString().trimEnd('\n')))
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
            // Render blank-line-separated paragraphs as separate compact blocks. Keeping blank
            // lines inside one Text adds a full line-height of vertical whitespace on phones.
            flushText()
        } else {
            text.append(line).append('\n')
        }
    }
    if (inCode) parts.add(ReplyPart.Code(language, code.toString().trimEnd('\n'))) else flushText()
    return parts
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
            lines.add(ReplyLine(text = "", isBlank = true))
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
    for (m in linkOrUrl.findAll(cleaned)) {
        appendBold(cleaned.substring(last, m.range.first), inlineCode)
        val url = m.groupValues[2].ifEmpty { m.groupValues[3] }
        val label = m.groupValues[1].ifEmpty { url }
        // The whole "label (address)" is tappable and shows the full address, as requested.
        pushStringAnnotation(tag = "URL", annotation = url)
        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
            appendBold(label, inlineCode)
            if (label != url) append(" ($url)")
        }
        pop()
        last = m.range.last + 1
    }
    appendBold(cleaned.substring(last), inlineCode)
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
