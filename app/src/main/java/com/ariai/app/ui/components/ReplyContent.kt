package com.ariai.app.ui.components

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.ui.theme.AriTint
import com.ariai.app.util.ReplyPart
import com.ariai.app.util.formatReply
import com.ariai.app.util.splitReply
import com.ariai.app.util.tx

/** Shows a model reply: text paragraphs (with tappable links) and separate code blocks with a copy button. */
@Composable
fun ReplyContent(text: String, fontSizeSp: Float, color: Color) {
    val parts = remember(text) { splitReply(text) }
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEach { part ->
            when (part) {
                is ReplyPart.Text -> {
                    val annotated = remember(part.raw) { formatReply(part.raw) }
                    if (annotated.text.isNotBlank()) {
                        ClickableText(
                            text = annotated,
                            style = TextStyle(
                                color = color,
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp + 8f).sp,
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
    Surface(shape = RoundedCornerShape(12.dp), color = AriTint, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    language.ifBlank { tx("Code") },
                    fontSize = 12.sp,
                    color = color.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = tx("Copy code"), tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                code,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = color,
                style = TextStyle(textDirection = TextDirection.Ltr),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            )
        }
    }
}
