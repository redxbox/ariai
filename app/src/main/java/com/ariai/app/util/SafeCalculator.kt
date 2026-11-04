package com.ariai.app.util

import java.math.BigDecimal
import java.math.MathContext

/** Small arithmetic-only parser; it never evaluates Kotlin, JavaScript, or shell code. */
object SafeCalculator {
    fun evaluate(expression: String): String {
        require(expression.length <= 256) { "Expression is too long" }
        val parser = Parser(expression)
        val value = parser.parse()
        require(value.isFinite()) { "Result is not finite" }
        return BigDecimal.valueOf(value).round(MathContext.DECIMAL64).stripTrailingZeros().toPlainString()
    }

    private class Parser(private val input: String) {
        private var index = 0
        private var operations = 0

        fun parse(): Double {
            val value = expression(0)
            whitespace()
            require(index == input.length) { "Unexpected character at position $index" }
            return value
        }

        private fun expression(depth: Int): Double {
            checkDepth(depth)
            var value = term(depth + 1)
            while (true) {
                whitespace()
                when (peek()) {
                    '+' -> { index++; value += term(depth + 1); tick() }
                    '-' -> { index++; value -= term(depth + 1); tick() }
                    else -> return value
                }
            }
        }

        private fun term(depth: Int): Double {
            var value = unary(depth + 1)
            while (true) {
                whitespace()
                when (peek()) {
                    '*' -> { index++; value *= unary(depth + 1); tick() }
                    '/' -> {
                        index++
                        val divisor = unary(depth + 1)
                        require(divisor != 0.0) { "Division by zero" }
                        value /= divisor
                        tick()
                    }
                    '%' -> {
                        index++
                        val divisor = unary(depth + 1)
                        require(divisor != 0.0) { "Remainder by zero" }
                        value %= divisor
                        tick()
                    }
                    else -> return value
                }
            }
        }

        private fun power(depth: Int): Double {
            val base = primary(depth + 1)
            whitespace()
            if (peek() != '^') return base
            index++
            val exponent = unary(depth + 1)
            require(exponent in -12.0..12.0) { "Exponent must be between -12 and 12" }
            tick()
            return Math.pow(base, exponent).also { require(it.isFinite()) { "Result is not finite" } }
        }

        private fun unary(depth: Int): Double {
            checkDepth(depth)
            whitespace()
            return when (peek()) {
                '+' -> { index++; unary(depth + 1) }
                '-' -> { index++; -unary(depth + 1) }
                else -> power(depth + 1)
            }
        }

        private fun primary(depth: Int): Double {
            checkDepth(depth)
            whitespace()
            if (peek() == '(') {
                index++
                val value = expression(depth + 1)
                whitespace()
                require(peek() == ')') { "Missing closing parenthesis" }
                index++
                return value
            }
            val start = index
            var sawDigit = false
            while (peek()?.isDigit() == true) { index++; sawDigit = true }
            if (peek() == '.') {
                index++
                while (peek()?.isDigit() == true) { index++; sawDigit = true }
            }
            require(sawDigit) { "Expected a number at position $index" }
            val value = input.substring(start, index).toDoubleOrNull()
                ?: throw IllegalArgumentException("Invalid number at position $start")
            require(value.isFinite()) { "Number is too large" }
            return value
        }

        private fun peek(): Char? = input.getOrNull(index)
        private fun whitespace() { while (peek()?.isWhitespace() == true) index++ }
        private fun tick() { operations++; require(operations <= 100) { "Expression is too complex" } }
        private fun checkDepth(depth: Int) { require(depth <= 32) { "Expression is too deeply nested" } }
    }
}
