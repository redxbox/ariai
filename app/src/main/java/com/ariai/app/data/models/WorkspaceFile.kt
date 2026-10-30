package com.ariai.app.data.models

/** A UTF-8 text file stored in AriAI's private, on-device workspace. */
data class WorkspaceFile(
    val name: String,
    val content: String,
    val updatedAt: Long
)
