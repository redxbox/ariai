package com.ariai.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.ui.theme.AriTint
import com.ariai.app.util.ReplyPart
import com.ariai.app.util.formatReply
import com.ariai.app.util.splitReply
import com.ariai.app.util.tx

/** Shows model replies with tappable links and copyable, line-numbered code blocks. */
@Composable
fun ReplyContent(text: String, fontSizeSp: Float, color: Color) {
    val parts = remember(text) { splitReply(text) }
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        parts.forEach { part ->
            when (part) {
                is ReplyPart.Text -> {
                    val annotated = remember(part.raw, fontSizeSp) { formatReply(part.raw, fontSizeSp) }
                    if (annotated.text.isNotBlank()) {
                        ClickableText(
                            text = annotated,
                            style = TextStyle(
                                color = color,
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp * 1.55f).sp,
                                textDirection = TextDirection.Content
                            ),
                            onClick = { offset ->
                                annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                                    runCatching { uriHandler.openUri(it.item) }
                                }
                            }
                        )
                    }
                }
                is ReplyPart.Code -> CodeBlock(part.language, part.code, color)
            }
        }
    }
}

@Composable
private fun CodeBlock(language: String, code: String, color: Color) {
    val clipboard = LocalClipboardManager.current
    // A plain Box, not Surface: Surface adds a tonal (beige) overlay in Material 3.
    Box(modifier = Modifier.fillMaxWidth().background(AriTint, RoundedCornerShape(12.dp))) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = tx("Copy code"), tint = color, modifier = Modifier.size(18.dp))
                }
                if (language.isNotBlank()) {
                    Text(language, fontSize = 12.sp, color = color.copy(alpha = 0.6f))
                }
            }
            val displayCode = code.trimEnd('\n', '\r')
            val lineCount = displayCode.count { it == '\n' } + 1
            val highlightedCode = remember(displayCode, language, color) { highlightCode(displayCode, language, color) }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    (1..lineCount).joinToString("\n"),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = color.copy(alpha = 0.42f),
                    style = TextStyle(textDirection = TextDirection.Ltr),
                    modifier = Modifier.padding(start = 12.dp, end = 10.dp, bottom = 12.dp)
                )
                Text(
                    highlightedCode,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = color,
                    style = TextStyle(textDirection = TextDirection.Ltr),
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                        .padding(end = 12.dp, bottom = 12.dp)
                )
            }
        }
    }
}

/** Lightweight, dependency-free token coloring for common programming languages. */
private fun highlightCode(code: String, language: String, baseColor: Color): AnnotatedString {
    val commonKeywords = setOf(
        "if", "else", "when", "for", "while", "return", "break", "continue", "true", "false", "null",
        "class", "interface", "object", "fun", "val", "var", "const", "let", "function", "def", "import",
        "from", "as", "in", "is", "not", "and", "or", "try", "catch", "finally", "throw", "new", "this",
        "public", "private", "protected", "static", "async", "await", "yield", "package", "extends", "implements"
    )
    val languageKeywords = when (language.lowercase().substringAfterLast('.')) {
        "py", "python" -> commonKeywords + setOf("elif", "lambda", "pass", "raise", "with", "self", "None", "True", "False")
        "js", "jsx", "ts", "tsx", "javascript", "typescript" -> commonKeywords + setOf("export", "default", "typeof", "instanceof", "undefined", "void", "switch", "case")
        "json" -> setOf("true", "false", "null")
        "kt", "kts", "kotlin", "java" -> commonKeywords + setOf("suspend", "override", "companion", "data", "enum", "sealed", "val", "var")
        "swift" -> commonKeywords + setOf("guard", "defer", "struct", "enum", "extension", "protocol", "self", "Self")
        else -> commonKeywords
    }
    val keywordPattern = languageKeywords.sortedByDescending(String::length).joinToString("|") { Regex.escape(it) }
    val tokenPattern = Regex(
        """(?m)//[^\n]*|/\*[\s\S]*?\*/|#[^\n]*|"{3}[\s\S]*?"{3}|'''[\s\S]*?'''|`(?:\\.|[^`\\])*`|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\b\d+(?:\.\d+)?\b|\b(?:$keywordPattern)\b"""
    )
    val darkText = baseColor.luminance() > 0.5f
    val keywordColor = if (darkText) Color(0xFFD8B4FE) else Color(0xFF7137A8)
    val stringColor = if (darkText) Color(0xFF9FE0A8) else Color(0xFF236B38)
    val commentColor = if (darkText) Color(0xFFA2A7B0) else Color(0xFF747A85)
    val numberColor = if (darkText) Color(0xFFFFC078) else Color(0xFFB85618)

    return buildAnnotatedString {
        var cursor = 0
        tokenPattern.findAll(code).forEach { match ->
            append(code.substring(cursor, match.range.first))
            val token = match.value
            val tokenColor = when {
                token.startsWith("//") || token.startsWith("/*") || token.startsWith("#") -> commentColor
                token.startsWith('"') || token.startsWith('\'') || token.startsWith('`') -> stringColor
                token.firstOrNull()?.isDigit() == true -> numberColor
                token in languageKeywords -> keywordColor
                else -> baseColor
            }
            withStyle(SpanStyle(color = tokenColor)) { append(token) }
            cursor = match.range.last + 1
        }
        append(code.substring(cursor))
    }
}
