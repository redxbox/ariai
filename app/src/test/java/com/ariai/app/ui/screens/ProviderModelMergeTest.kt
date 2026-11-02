package com.ariai.app.ui.screens

import com.ariai.app.data.models.AIModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderModelMergeTest {
    @Test
    fun manualCapabilitiesSurviveRefreshWhenApiOmitsThem() {
        val manual = AIModel(
            id = "image-model",
            displayName = "My image model",
            providerId = "provider",
            supportsVision = false,
            supportsImageGen = true,
            isCustom = true,
            outputsText = false
        )
        val apiModel = AIModel(
            id = "image-model",
            displayName = "image-model",
            providerId = "provider",
            supportsVision = false,
            supportsImageGen = false,
            outputsText = true
        )

        val merged = mergeFetched(listOf(manual), listOf(apiModel))

        assertEquals(1, merged.size)
        assertEquals("My image model", merged.single().displayName)
        assertTrue(merged.single().supportsImageGen)
        assertFalse(merged.single().supportsVision)
        assertFalse(merged.single().outputsText)
        assertTrue(merged.single().isCustom)
    }

    @Test
    fun freshApiCapabilitiesUpdateDiscoveredModels() {
        val existing = AIModel(
            id = "provider-model",
            displayName = "Old name",
            providerId = "provider",
            supportsImageGen = false,
            headers = mapOf("x-model" to "custom")
        )
        val apiModel = AIModel(
            id = "provider-model",
            displayName = "Provider model",
            providerId = "provider",
            supportsImageGen = true
        )

        val merged = mergeFetched(listOf(existing), listOf(apiModel)).single()

        assertEquals("Provider model", merged.displayName)
        assertTrue(merged.supportsImageGen)
        assertEquals("custom", merged.headers["x-model"])
    }
}
