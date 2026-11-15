package com.ariai.app.ui.screens

import com.ariai.app.data.models.AIModel

/**
 * Picks the model to use right after the first API key is added. Uses only the model's own
 * capability metadata (never its name): text output is required, then tool support, then vision.
 * Ties keep the provider's order.
 */
internal fun pickStartingModel(models: List<AIModel>): AIModel? =
    models
        .filter { it.outputsText && it.id.isNotBlank() }
        .maxByOrNull { score(it) } // maxByOrNull keeps the first of equal scores

private fun score(model: AIModel): Int =
    (if (model.supportsFunctionCalling) 2 else 0) + (if (model.supportsVision) 1 else 0)
