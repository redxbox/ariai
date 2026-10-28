package com.ariai.app.ui.screens

import com.ariai.app.data.models.Provider

/** Resolve a persisted selection without ever falling back from an explicitly disabled provider. */
internal fun resolveEnabledProvider(providers: List<Provider>, selectedProviderId: String?): Provider? {
    val selected = selectedProviderId?.let { id -> providers.firstOrNull { it.id == id } }
    if (selected != null) return selected.takeIf { it.enabled }
    if (selectedProviderId != null && providers.any { it.id == selectedProviderId }) return null
    return providers.firstOrNull { it.enabled }
}
