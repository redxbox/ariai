package com.ariai.app.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchProviderSelectionTest {
    @Test
    fun configuredPreferenceWinsOverRecommendedFallback() {
        val keys = mapOf("serper" to "google-key", "brave" to "brave-key")

        assertEquals("brave", resolveSearchProviderKey(keys, "brave"))
        assertEquals("Google Search", searchProviderDisplayName("serper"))
        assertEquals(SearchProviderType.SERPER, searchProviderTypeForKey("serper"))
    }

    @Test
    fun googleViaSerperIsTheRecommendedFallbackAndBraveIsNext() {
        assertEquals("serper", resolveSearchProviderKey(mapOf("serper" to "google-key", "brave" to "brave-key"), null))
        assertEquals("brave", resolveSearchProviderKey(mapOf("brave" to "brave-key", "exa" to "exa-key"), null))
        assertEquals("tavily", resolveSearchProviderKey(mapOf("tavily" to "tavily-key"), null))
    }

    @Test
    fun missingOrUnsupportedKeysNeverAppearAsActiveProviders() {
        val keys = mapOf("serper" to "", "duckduckgo" to "unimplemented-key")

        assertNull(resolveSearchProviderKey(keys, "duckduckgo"))
        assertNull(searchProviderTypeForKey("duckduckgo"))
        assertNull(searchProviderDisplayName("bing"))
    }
}
