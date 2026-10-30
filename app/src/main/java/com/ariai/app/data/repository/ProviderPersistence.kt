package com.ariai.app.data.repository

import kotlinx.coroutines.CancellationException

/** Run provider conversion and persistence without hiding failures or coroutine cancellation. */
internal suspend fun <T> persistWithResult(
    prepare: suspend () -> T,
    persist: suspend (T) -> Unit,
    onFailure: (Exception) -> Unit
): Boolean = try {
    persist(prepare())
    true
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    onFailure(error)
    false
}
