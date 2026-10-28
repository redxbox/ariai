package com.ariai.app.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchProviderSelectionTest {
    @Test
    fun braveIsTheDefaultAndBingRemainsAnExplicitKeylessOption() {
        assertEquals("brave", resolveSearchProviderKey(emptyMap(), null))
        assertEquals(SearchProviderType.BING, searchProviderTypeForKey("bing"))
        assertEquals("Bing", searchProviderDisplayName("bing"))

        val keys = mapOf("brave" to "brave-key")
        assertEquals("brave", resolveSearchProviderKey(keys, null))
        assertEquals("bing", resolveSearchProviderKey(keys, "bing"))
        assertEquals("brave", resolveSearchProviderKey(emptyMap(), "brave"))
        assertEquals(SearchProviderType.BRAVE, searchProviderTypeForKey("brave"))
    }

    @Test
    fun legacySearchKeysDoNotOverrideTheNewBingAndBraveChoices() {
        val keys = mapOf("serper" to "google-key", "tavily" to "tavily-key")

        assertEquals("brave", resolveSearchProviderKey(keys, null))
        assertEquals("brave", resolveSearchProviderKey(keys, "serper"))
        assertEquals("Google Search", searchProviderDisplayName("serper"))
        assertEquals(SearchProviderType.SERPER, searchProviderTypeForKey("serper"))
    }

    @Test
    fun unsupportedProvidersStillHaveNoSearchImplementation() {
        assertEquals("brave", resolveSearchProviderKey(mapOf("duckduckgo" to "unused-key"), "duckduckgo"))
        assertNull(searchProviderTypeForKey("duckduckgo"))
        assertNull(searchProviderDisplayName("duckduckgo"))
    }
}
