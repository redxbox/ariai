package com.ariai.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun roundTripsHeadersThatContainFormerDelimiters() {
        val headers = mapOf(
            "X-Pipe" to "abc|||def",
            "X-Colons" to "a:::b",
            "X-Empty" to "",
            "کلید" to "مقدار با \"نقل قول\" و یونیکد ✅"
        )
        val stored = converters.fromStringMap(headers)
        assertEquals(headers, converters.toStringMap(stored))
    }

    @Test
    fun readsLegacyDelimiterFormat() {
        assertEquals(
            mapOf("a" to "1", "b" to "2:::3"),
            converters.toStringMap("a:::1|||b:::2:::3")
        )
        assertEquals(emptyMap<String, String>(), converters.toStringMap(""))
    }

    @Test
    fun fallsBackToEmptyMapForGarbageInsteadOfCrashing() {
        assertEquals(emptyMap<String, String>(), converters.toStringMap("{not json at all"))
        assertEquals(emptyMap<String, String>(), converters.toStringMap(":::"))
    }
}
