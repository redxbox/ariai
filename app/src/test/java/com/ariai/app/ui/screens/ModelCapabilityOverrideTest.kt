package com.ariai.app.ui.screens

import com.ariai.app.data.models.AIModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCapabilityOverrideTest {
    @Test
    fun manualCapabilitiesSurviveAnApiRefresh() {
        val current = listOf(AIModel("m", "m", "p", supportsAudioInput = true, capabilityOverridden = true))
        val fetched = listOf(AIModel("m", "m", "p", supportsAudioInput = false))
        val merged = mergeFetched(current, fetched).single()
        assertTrue(merged.supportsAudioInput)
        assertTrue(merged.capabilityOverridden)
    }

    @Test
    fun untouchedModelsFollowTheFreshFetch() {
        val current = listOf(AIModel("m", "m", "p", supportsAudioInput = true))
        val fetched = listOf(AIModel("m", "m", "p", supportsAudioInput = false))
        assertFalse(mergeFetched(current, fetched).single().supportsAudioInput)
    }
}
