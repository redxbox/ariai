package com.ariai.app.net

/** Pure SSE framing helper. JSON/provider-specific parsing stays outside this class. */
internal object SseFrames {
    fun consume(
        lines: Sequence<String>,
        parse: (String) -> String,
        onPiece: (String) -> Unit
    ): Boolean {
        var sawDone = false
        var emitted = false
        val fallback = StringBuilder()

        for (line in lines) {
            when {
                line.startsWith("data:") -> {
                    val data = line.removePrefix("data:").trim()
                    if (data == "[DONE]") {
                        sawDone = true
                        break
                    }
                    runCatching { parse(data) }
                        .getOrNull()
                        ?.takeIf { it.isNotEmpty() }
                        ?.let {
                            emitted = true
                            onPiece(it)
                        }
                }
                line.isNotBlank() && line.startsWith("{") -> fallback.append(line)
            }
        }

        if (!sawDone && !emitted && fallback.isNotEmpty()) {
            runCatching { parse(fallback.toString()) }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let(onPiece)
        }

        return sawDone
    }
}