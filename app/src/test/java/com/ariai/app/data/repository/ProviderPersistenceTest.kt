package com.ariai.app.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ProviderPersistenceTest {
    @Test
    fun `successful persistence reports success`() = runBlocking {
        var persisted: String? = null

        val result = persistWithResult(
            prepare = { "provider-entity" },
            persist = { persisted = it },
            onFailure = { throw AssertionError("unexpected failure", it) }
        )

        assertTrue(result)
        org.junit.Assert.assertEquals("provider-entity", persisted)
    }

    @Test
    fun `persistence failure is returned to caller`() = runBlocking {
        var reported: Exception? = null
        val failure = IOException("database unavailable")

        val result = persistWithResult(
            prepare = { "provider-entity" },
            persist = { throw failure },
            onFailure = { reported = it }
        )

        assertFalse(result)
        assertSame(failure, reported)
    }

    @Test
    fun `cancellation is not converted into a save failure`() {
        val cancellation = CancellationException("save cancelled")
        val thrown = org.junit.Assert.assertThrows(CancellationException::class.java) {
            runBlocking {
                persistWithResult(
                    prepare = { throw cancellation },
                    persist = {},
                    onFailure = { throw AssertionError("cancellation must not be reported as an error", it) }
                )
            }
        }

        assertSame(cancellation, thrown)
    }
}
