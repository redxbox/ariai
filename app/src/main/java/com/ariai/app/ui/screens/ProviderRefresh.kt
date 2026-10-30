package com.ariai.app.ui.screens

import kotlinx.coroutines.CancellationException

internal sealed interface ProviderRefreshOutcome {
    data object Updated : ProviderRefreshOutcome
    data object SourceChanged : ProviderRefreshOutcome
    data class Failed(val error: Exception) : ProviderRefreshOutcome
}

/** Calls [onSuccess] only after a successful fetch for the still-current provider source. */
internal suspend fun <T> refreshProviderModels(
    fetch: suspend () -> List<T>,
    sourceIsCurrent: () -> Boolean,
    onSuccess: (List<T>) -> Unit
): ProviderRefreshOutcome {
    val models = try {
        fetch()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        return ProviderRefreshOutcome.Failed(error)
    }
    if (!sourceIsCurrent()) return ProviderRefreshOutcome.SourceChanged
    onSuccess(models)
    return ProviderRefreshOutcome.Updated
}
