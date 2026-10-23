package com.ariai.app.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRoutingTest {
    @Test
    fun `calculator is exposed for explicit or recognizable arithmetic requests`() {
        assertTrue(shouldOfferCalculatorTool("What is 2 * 3?"))
        assertTrue(shouldOfferCalculatorTool("Please calculate the square root of 81"))
        assertTrue(shouldOfferCalculatorTool("لطفاً این را محاسبه کن"))
        assertFalse(shouldOfferCalculatorTool("Explain why leaves change color."))
    }

    @Test
    fun `stream preference applies to ordinary tool-free replies only`() {
        assertTrue(shouldStreamToolFreeChat(streamEnabled = true, hasTools = false))
        assertFalse(shouldStreamToolFreeChat(streamEnabled = false, hasTools = false))
        assertFalse(shouldStreamToolFreeChat(streamEnabled = true, hasTools = true))
    }
}
