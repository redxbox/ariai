package com.ariai.app.ui.screens

internal const val ATTACHMENT_PREVIEW_MAX_CHARS = 160

/** Compact, whitespace-normalized preview for locally extracted document text. */
internal fun attachmentTextPreview(text: String, maxChars: Int = ATTACHMENT_PREVIEW_MAX_CHARS): String {
    val limit = maxChars.coerceAtLeast(1)
    val flattened = text.replace(Regex("\\s+"), " ").trim()
    if (flattened.length <= limit) return flattened
    return flattened.take((limit - 1).coerceAtLeast(0)).trimEnd() + "…"
}

/** Conservative token estimate used only for the attachment preview, not provider billing. */
internal fun estimateAttachmentTextTokens(text: String): Int =
    ((text.length.toLong() + 3L) / 4L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
