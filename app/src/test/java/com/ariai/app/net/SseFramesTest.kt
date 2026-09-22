package com.ariai.app.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SseFramesTest {
    @Test fun emitsDataFramesUntilDone() {
        val out = mutableListOf<String>()
        val done = SseFrames.consume(
            sequenceOf("data: one", "", "data: two", "data: [DONE]", "data: ignored"),
            { it },
            out::add
        )
        assertTrue(done)
        assertEquals(listOf("one", "two"), out)
    }

    @Test fun skipsMalformedFramesAndKeepsValidOnes() {
        val out = mutableListOf<String>()
        SseFrames.consume(sequenceOf("data: ok", "data: {bad", "data: good"), { value ->
            if (value == "{bad") error("bad frame")
            value
        }, out::add)
        assertEquals(listOf("ok", "good"), out)
    }

    @Test fun fallsBackToBufferedJsonWhenNoDataFrames() {
        val out = mutableListOf<String>()
        val done = SseFrames.consume(sequenceOf("{first}", "{second}"), { it }, out::add)
        assertFalse(done)
        assertEquals(listOf("{first}{second}"), out)
    }

    @Test fun doesNotEmitFallbackAfterDataFrames() {
        val out = mutableListOf<String>()
        SseFrames.consume(sequenceOf("data: streamed", "{fallback}"), { it }, out::add)
        assertEquals(listOf("streamed"), out)
    }

    @Test fun ignoresEmptyParsedPieces() {
        val out = mutableListOf<String>()
        SseFrames.consume(sequenceOf("data: empty", "data: [DONE]"), { "" }, out::add)
        assertEquals(emptyList<String>(), out)
    }
}