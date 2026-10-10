package com.ariai.app.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

/** A reply is a list of normal text paragraphs and code blocks. */
sealed class ReplyPart {
    data class Text(val raw: String) : ReplyPart()
    data class Code(val language: String, val code: String) : ReplyPart()
}

private val boldRegex = Regex("""\*\*(.+?)\*\*|__(.+?)__""")
private val codeRegex = Regex("""`([^`]+)`""")
private val linkOrUrl = Regex("""\[([^\]]+)\]\((https?://[^)\s]+)\)|(https?://[^\s)\]]+)""")
private val tableSeparator = Regex("""^\|?[\s:|-]+\|?$""")
private val ruleLine = Regex("""^(-{3,}|\*{3,}|_{3,})$""")
private val bulletPrefix = Regex("""^[-*+]\s+""")
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
        } else {
            text.append(line).append('\n')
        }
    }
    if (inCode) parts.add(ReplyPart.Code(language, code.toString().trimEnd('\n'))) else flushText()
    return parts
}

/**
 * Turns Markdown text into readable text: bold is bold, headings are bold without '#',
 * tables become one line per row, bullets get a dot, links keep their label and are tagged
 * with their address (annotation tag "URL") so they can be opened on tap.
 */
fun formatReply(raw: String): AnnotatedString {
    val lines = mutableListOf<Pair<String, Boolean>>() // text, is heading
    var pendingBlank = false
    for (original in raw.lines()) {
        var line = original.trim()
        if (line.isEmpty()) {
            pendingBlank = lines.isNotEmpty()
            continue
        }
        if (ruleLine.matches(line) || (tableSeparator.matches(line) && line.contains('-'))) continue
        var heading = false
        if (line.startsWith("#")) {
            heading = true
            line = line.trimStart('#').trim()
        }
        if (line.startsWith(">")) line = line.removePrefix(">").trim()
        if (line.startsWith("|") && line.endsWith("|")) {
            line = line.trim('|').split("|").map { it.trim() }.filter { it.isNotEmpty() }.joinToString("  ·  ")
        } else if (bulletPrefix.containsMatchIn(line)) {
            line = "•  " + line.replaceFirst(bulletPrefix, "")
        }
        if (line.isEmpty()) continue
        if (pendingBlank) {
            lines.add("" to false)
            pendingBlank = false
        }
        lines.add(line to heading)
    }
    return buildAnnotatedString {
        lines.forEachIndexed { index, (line, heading) ->
            if (index > 0) append("\n")
            if (heading) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendInline(line) }
            } else {
                appendInline(line)
            }
        }
    }
}

private fun AnnotatedString.Builder.appendInline(line: String) {
    val cleaned = line.replace(codeRegex) { it.groupValues[1] }
    var last = 0
    for (m in linkOrUrl.findAll(cleaned)) {
        appendBold(cleaned.substring(last, m.range.first))
        val url = m.groupValues[2].ifEmpty { m.groupValues[3] }
        val label = m.groupValues[1].ifEmpty { url }
        pushStringAnnotation(tag = "URL", annotation = url)
        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) { appendBold(label) }
        pop()
        last = m.range.last + 1
    }
    appendBold(cleaned.substring(last))
}

private fun AnnotatedString.Builder.appendBold(segment: String) {
    var last = 0
    for (m in boldRegex.findAll(segment)) {
        append(segment.substring(last, m.range.first).replace("*", ""))
        val inner = m.groupValues[1].ifEmpty { m.groupValues[2] }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(inner.replace("*", "")) }
        last = m.range.last + 1
    }
    append(segment.substring(last).replace("*", ""))
}
