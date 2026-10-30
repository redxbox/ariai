package com.ariai.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderNamingTest {
    @Test
    fun firstCustomProviderGetsNumberedDefaultName() {
        assertEquals("Custom 1", nextUniqueProviderName("Custom", emptyList()))
    }

    @Test
    fun skipsExistingCustomNamesIgnoringCaseAndWhitespace() {
        assertEquals(
            "Custom 3",
            nextUniqueProviderName("Custom", listOf("Custom 1", " custom 2 ", "OpenAI"))
        )
    }

    @Test
    fun blankBaseNameUsesProviderFallback() {
        assertEquals("Provider 1", nextUniqueProviderName("  ", emptyList()))
    }
}
