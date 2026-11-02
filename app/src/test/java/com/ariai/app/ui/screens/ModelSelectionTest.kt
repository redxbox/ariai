package com.ariai.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelSelectionTest {
    @Test
    fun `matching model id from another provider is not selected`() {
        assertTrue(isSelectedModel("provider-a", "gpt-4o", "provider-a", "gpt-4o"))
        assertFalse(isSelectedModel("provider-b", "gpt-4o", "provider-a", "gpt-4o"))
        assertFalse(isSelectedModel("provider-a", "gpt-4o", "provider-a", "gpt-4o-mini"))
        assertFalse(isSelectedModel("provider-a", "gpt-4o", null, "gpt-4o"))
    }
}
