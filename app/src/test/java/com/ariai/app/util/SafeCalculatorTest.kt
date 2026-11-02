package com.ariai.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SafeCalculatorTest {
    @Test
    fun `evaluates arithmetic with precedence and parentheses`() {
        assertEquals("14", SafeCalculator.evaluate("2 + 3 * 4"))
        assertEquals("20", SafeCalculator.evaluate("(2 + 3) * 4"))
        assertEquals("-4", SafeCalculator.evaluate("-2^2"))
    }

    @Test
    fun `rejects division by zero and executable input`() {
        assertThrows(IllegalArgumentException::class.java) { SafeCalculator.evaluate("1 / 0") }
        assertThrows(IllegalArgumentException::class.java) { SafeCalculator.evaluate("Runtime.getRuntime()") }
    }
}
