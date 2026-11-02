package com.ariai.app.ui.screens

internal fun isSelectedModel(
    providerId: String,
    modelId: String,
    selectedProviderId: String?,
    selectedModelId: String?
): Boolean = selectedProviderId == providerId && selectedModelId == modelId
