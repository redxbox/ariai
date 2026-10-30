package com.ariai.app.data.remote

/** Only advertise arithmetic when the user message is plausibly a calculation request. */
internal fun shouldOfferCalculatorTool(text: String): Boolean {
    val numericExpression = Regex("""(?<![\p{L}\p{N}])\d+(?:[.,]\d+)?\s*[+*/^%×÷-]\s*\d+(?:[.,]\d+)?""")
    val wordExpression = Regex(
        """(?i)\b\d+(?:[.,]\d+)?\s+(?:plus|minus|times|multiplied by|divided by|modulo)\s+\d+(?:[.,]\d+)?\b"""
    )
    val explicitMath = Regex(
        """(?i)\b(calculate|compute|evaluate|solve|arithmetic|square root|percent of|how many|how much)\b|حساب|محاسبه|جمع|تفریق|ضرب|تقسیم|ریشه"""
    )
    return numericExpression.containsMatchIn(text) || wordExpression.containsMatchIn(text) || explicitMath.containsMatchIn(text)
}

/** The ordinary, tool-free path can use the configured token stream directly. */
internal fun shouldStreamToolFreeChat(streamEnabled: Boolean, hasTools: Boolean): Boolean =
    streamEnabled && !hasTools
