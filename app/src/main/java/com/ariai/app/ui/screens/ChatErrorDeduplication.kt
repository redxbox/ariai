package com.ariai.app.ui.screens

import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import com.ariai.app.data.models.MessageStatus

/** True when appending this would repeat the same assistant error at the end of the chat. */
internal fun repeatsLatestAssistantError(
    messages: List<ChatMessage>,
    content: String,
    parentId: String?
): Boolean {
    val latest = messages.lastOrNull() ?: return false
    return latest.role == MessageRole.ASSISTANT &&
        latest.status == MessageStatus.ERROR &&
        latest.content == content &&
        latest.parentId == parentId
}
