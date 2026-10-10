package com.ariai.app.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private val linkRegex = Regex("""\[([^\]]+)\]\((https?://[^)\s]+)\)""")
private val boldRegex = Regex("""\*\*(.+?)\*\*|__(.+?)__""")
private val codeRegex = Regex("""`([^`]+)`""")
private val tableSeparator = Regex("""^\|?[\s:|-]+\|?$""")
private val ruleLine = Regex("""^(-{3,}|\*{3,}|_{3,})$""")
private val bulletPrefix = Regex("""^[-*+]\s+""")

/**
 * Turns a model reply (which often uses Markdown) into clean, readable text:
 * bold is shown in bold, links keep only their label, tables become one line per row,
 * and stray Markdown symbols are removed.
 */
fun formatReply(raw: String): AnnotatedString {
    val lines = mutableListOf<Pair<String, Boolean>>() // text to show, is heading
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

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInline(line: String) {
    val cleaned = line
        .replace(linkRegex) { it.groupValues[1] }
        .replace(codeRegex) { it.groupValues[1] }
    var last = 0
    for (match in boldRegex.findAll(cleaned)) {
        append(cleaned.substring(last, match.range.first).replace("*", ""))
        val inner = match.groupValues[1].ifEmpty { match.groupValues[2] }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(inner.replace("*", "")) }
        last = match.range.last + 1
    }
    append(cleaned.substring(last).replace("*", ""))
}
