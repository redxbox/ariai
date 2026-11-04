package com.ariai.app.ui.screens

/** Shared, stable navigation order for linked message versions in separate chat branches. */
internal fun branchNavigationIds(messageId: String, siblingIds: List<String>): List<String> =
    (listOf(messageId) + siblingIds).distinct().sorted()

/** Returns all linked siblings except the message represented by [currentMessageId]. */
internal fun linkedBranchSiblingIds(
    currentMessageId: String,
    existingSiblingIds: List<String>,
    newBranchMessageId: String
): List<String> =
    (existingSiblingIds + newBranchMessageId).distinct().filterNot { it == currentMessageId }
