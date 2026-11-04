package com.ariai.app.ui.screens

import com.ariai.app.data.models.AttachmentType
import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole

/** Conservative, media-aware token estimate used to decide when history should be summarized. */
internal fun estimateMessageTokens(message: ChatMessage): Long {
    val characters = message.content.length.toLong() +
        message.attachments.sumOf { it.textContent?.length?.toLong() ?: 0L }
    val mediaTokens = message.attachments.sumOf { attachment ->
        when (attachment.type) {
            AttachmentType.IMAGE -> 768L
            AttachmentType.VIDEO -> 2048L
            else -> 0L
        }
    }
    return maxOf(1L, (characters + 3L) / 4L) + mediaTokens
}

/** Splits only at a user turn boundary so the latest request and reply are retained intact. */
internal fun splitHistoryForSummary(
    history: List<ChatMessage>,
    keepAtLeast: Int = 8
): Pair<List<ChatMessage>, List<ChatMessage>> {
    if (history.size <= keepAtLeast) return history to emptyList()
    var start = history.size - keepAtLeast
    while (start > 0 && history[start].role != MessageRole.USER) start--
    if (history[start].role != MessageRole.USER) return history to emptyList()
    return history.take(start) to history.drop(start)
}

/** Finds a timestamp strictly between the prior message and retained history marker. */
internal fun summaryMarkerTimestamp(all: List<ChatMessage>, firstRetained: ChatMessage): Long? {
    val retainedIndex = all.indexOfFirst { it.id == firstRetained.id }
    if (retainedIndex < 0) return null
    val upper = firstRetained.timestamp
    val lower = all.getOrNull(retainedIndex - 1)?.timestamp
    if (lower == null) return upper.takeIf { it > Long.MIN_VALUE }?.minus(1L)
    val gap = upper - lower
    if (gap <= 1L) return null
    return lower + gap / 2L
}
