package com.ariai.app.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoDetectTest {
    @Test
    fun recognisesVideoModelNames() {
        listOf("sora-2", "veo-3.1-fast-generate-preview", "google/veo-3.1-lite", "wan2.2-t2v", "kling-v2.1", "seedance-1-pro")
            .forEach { assertTrue(it, looksLikeVideoModel(it)) }
    }

    @Test
    fun ignoresChatAndImageNames() {
        listOf("gpt-4o", "gemini-2.5-flash", "claude-sonnet-4-5", "flux-schnell", "gpt-image-1")
            .forEach { assertFalse(it, looksLikeVideoModel(it)) }
    }
}
