package com.ariai.app.ui.screens

import com.ariai.app.data.models.AIModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartingModelPolicyTest {
    @Test
    fun prefersToolSupportThenVisionAndKeepsProviderOrderOnTies() {
        val models = listOf(
            AIModel("plain", "plain", "p", outputsText = true, supportsFunctionCalling = false),
            AIModel("vision", "vision", "p", outputsText = true, supportsVision = true, supportsFunctionCalling = false),
            AIModel("tools", "tools", "p", outputsText = true, supportsFunctionCalling = true),
            AIModel("tools2", "tools2", "p", outputsText = true, supportsFunctionCalling = true),
            AIModel("image", "image", "p", outputsText = false, supportsImageGen = true, supportsFunctionCalling = false)
        )
        assertEquals("tools", pickStartingModel(models)?.id)
    }

    @Test
    fun returnsNullWithoutTextModels() {
        assertNull(pickStartingModel(listOf(AIModel("img", "img", "p", outputsText = false))))
    }
}
