package com.ariai.app.ui.screens

import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderSelectionTest {
    private val active = Provider("active", "Active", ProviderType.OPENAI_COMPATIBLE, "https://active.example/v1", "key")
    private val disabled = Provider("disabled", "Disabled", ProviderType.OPENAI_COMPATIBLE, "https://disabled.example/v1", "key", enabled = false)

    @Test
    fun explicitDisabledSelectionIsRejectedInsteadOfFallingBack() {
        assertNull(resolveEnabledProvider(listOf(disabled, active), "disabled"))
    }

    @Test
    fun missingSelectionFallsBackOnlyToAnEnabledProvider() {
        assertEquals(active, resolveEnabledProvider(listOf(disabled, active), "deleted-provider"))
    }

    @Test
    fun noSelectionNeverChoosesADisabledProvider() {
        assertEquals(active, resolveEnabledProvider(listOf(disabled, active), null))
        assertNull(resolveEnabledProvider(listOf(disabled), null))
    }
}
