package com.ariai.app.ui.screens

/** Returns the first unused numbered name for another provider of the same preset. */
internal fun nextUniqueProviderName(baseName: String, existingNames: Iterable<String>): String {
    val base = baseName.trim().ifBlank { "Provider" }
    val existing = existingNames.asSequence().map { it.trim().lowercase() }.toSet()
    var suffix = 1
    while ("$base $suffix".lowercase() in existing) suffix++
    return "$base $suffix"
}
