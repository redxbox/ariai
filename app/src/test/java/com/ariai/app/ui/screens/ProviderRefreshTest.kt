package com.ariai.app.ui.screens

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ProviderRefreshTest {
    @Test
    fun `save callback runs after a successful fetch for the current source`() = runBlocking {
        var saved: List<String>? = null

        val result = refreshProviderModels(
            fetch = { listOf("model-a") },
            sourceIsCurrent = { true },
            onSuccess = { saved = it }
        )

        assertEquals(ProviderRefreshOutcome.Updated, result)
        assertEquals(listOf("model-a"), saved)
    }

    @Test
    fun `failed fetch does not invoke save callback`() = runBlocking {
        var saveCalled = false
        val failure = IOException("offline")

        val result = refreshProviderModels(
            fetch = { throw failure },
            sourceIsCurrent = { true },
            onSuccess = { saveCalled = true }
        )

        assertTrue(result is ProviderRefreshOutcome.Failed)
        assertSame(failure, (result as ProviderRefreshOutcome.Failed).error)
        assertFalse(saveCalled)
    }

    @Test
    fun `source change during fetch does not invoke save callback`() = runBlocking {
        var saveCalled = false

        val result = refreshProviderModels(
            fetch = { listOf("stale-model") },
            sourceIsCurrent = { false },
            onSuccess = { saveCalled = true }
        )

        assertEquals(ProviderRefreshOutcome.SourceChanged, result)
        assertFalse(saveCalled)
    }

    @Test
    fun `cancellation is rethrown and does not invoke save callback`() {
        var saveCalled = false
        val cancellation = CancellationException("screen disposed")

        val thrown = org.junit.Assert.assertThrows(CancellationException::class.java) {
            runBlocking {
                refreshProviderModels(
                    fetch = { throw cancellation },
                    sourceIsCurrent = { true },
                    onSuccess = { saveCalled = true }
                )
            }
        }

        assertSame(cancellation, thrown)
        assertFalse(saveCalled)
    }
}
