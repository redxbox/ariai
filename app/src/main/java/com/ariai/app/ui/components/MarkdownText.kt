package com.ariai.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Simple markdown parsing - for full RikkaHub experience we would use multiplatform-markdown-renderer
        // This is a lightweight implementation handling code blocks, bold, etc.
        val lines = text.split("\n")
        var inCodeBlock = false
        var codeBlockContent = StringBuilder()
        var codeBlockLang = ""

        for (line in lines) {
            when {
                line.trim().startsWith("```") -> {
                    if (inCodeBlock) {
                        // End code block
                        CodeBlock(
                            code = codeBlockContent.toString(),
                            language = codeBlockLang
                        )
                        codeBlockContent = StringBuilder()
                        inCodeBlock = false
                    } else {
                        inCodeBlock = true
                        codeBlockLang = line.trim().removePrefix("```").trim()
                    }
                }
                inCodeBlock -> {
                    codeBlockContent.appendLine(line)
                }
                line.trim().startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# ").trim(),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                line.trim().startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## ").trim(),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                line.trim().startsWith("- ") || line.trim().startsWith("* ") -> {
                    Text(
                        text = "• ${line.trim().removePrefix("- ").removePrefix("* ").trim()}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 2.dp)
                    )
                }
                line.trim().startsWith("> ") -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = line.removePrefix("> ").trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> {
                    // Handle inline formatting roughly
                    var processed = line
                    // Bold **text**
                    processed = processed.replace(Regex("\\*\\*(.*?)\\*\\*")) { it.groupValues[1] }
                    
                    Text(
                        text = processed,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
        if (inCodeBlock && codeBlockContent.isNotEmpty()) {
            CodeBlock(code = codeBlockContent.toString(), language = codeBlockLang)
        }
    }
}

@Composable
fun CodeBlock(
    code: String,
    language: String = "",
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column {
            if (language.isNotBlank()) {
                Text(
                    text = language,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            Box(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = code.trim(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    content: String,
    isUser: Boolean,
    modifier: Modifier = Modifier,
    modelName: String? = null,
    timestamp: Long? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (modelName != null && !isUser) {
            Text(
                text = modelName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                if (isUser) {
                    Text(
                        text = content,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    MarkdownText(text = content)
                }
            }
        }
    }
}
