package com.ariai.app.data.remote

import com.ariai.app.data.models.SearchProvider
import com.ariai.app.data.models.SearchProviderType
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchClientTest {
    @Test
    fun tavilySendsKeyInJsonAndParsesResults() = withServer { server ->
        server.enqueue(
            MockResponse().setBody(
                """{"results":[{"title":"A result","url":"https://example.test/a","content":"Summary","score":0.91}]}"""
            )
        )
        val client = clientFor(server, SearchProviderType.TAVILY, "/tavily")

        val response = runBlocking {
            client.search("AriAI search", SearchProvider(name = "Tavily", type = SearchProviderType.TAVILY, apiKey = "tavily-secret"))
        }
        val request = server.takeRequest()
        val body = JSONObject(request.body.readUtf8())

        assertEquals("POST", request.method)
        assertEquals("/tavily", request.requestUrl?.encodedPath)
        assertEquals("AriAI search", body.getString("query"))
        assertEquals("tavily-secret", body.getString("api_key"))
        assertEquals("A result", response.results.single().title)
        assertEquals("https://example.test/a", response.results.single().url)
    }

    @Test
    fun braveSendsSubscriptionHeaderAndEncodesQuery() = withServer { server ->
        server.enqueue(
            MockResponse().setBody(
                """{"web":{"results":[{"title":"Brave result","url":"https://example.test/b","description":"Brave snippet"}]}}"""
            )
        )
        val client = clientFor(server, SearchProviderType.BRAVE, "/brave")

        val response = runBlocking {
            client.search("blue green app", SearchProvider(name = "Brave", type = SearchProviderType.BRAVE, apiKey = "brave-secret"))
        }
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/brave", request.requestUrl?.encodedPath)
        assertEquals("blue green app", request.requestUrl?.queryParameter("q"))
        assertEquals("5", request.requestUrl?.queryParameter("count"))
        assertEquals("brave-secret", request.getHeader("X-Subscription-Token"))
        assertEquals("Brave snippet", response.results.single().content)
    }

    @Test
    fun bingUsesKeylessRssAndParsesSafeResultFields() = withServer { server ->
        server.enqueue(
            MockResponse().setBody(
                """<?xml version="1.0"?><rss version="2.0"><channel><item><title>Example &amp; Guide</title><link>https://example.test/bing</link><description><![CDATA[Useful <b>summary</b> & details]]></description><pubDate>Tue, 27 Oct 2026 12:00:00 GMT</pubDate></item></channel></rss>"""
            )
        )
        val client = clientFor(server, SearchProviderType.BING, "/bing")

        val response = runBlocking {
            client.search("Bing blue sky", SearchProvider(name = "Bing", type = SearchProviderType.BING, apiKey = ""))
        }
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/bing", request.requestUrl?.encodedPath)
        assertEquals("rss", request.requestUrl?.queryParameter("format"))
        assertEquals("Bing blue sky", request.requestUrl?.queryParameter("q"))
        assertTrue(request.getHeader("Accept").orEmpty().contains("application/rss+xml"))
        assertEquals(SearchProviderType.BING, response.provider)
        assertEquals("Example & Guide", response.results.single().title)
        assertEquals("Useful summary & details", response.results.single().content)
        assertEquals("https://example.test/bing", response.results.single().url)
    }

    @Test
    fun exaSendsApiKeyHeaderAndParsesSemanticResults() = withServer { server ->
        server.enqueue(
            MockResponse().setBody(
                """{"results":[{"title":"Exa result","url":"https://example.test/c","text":"Neural snippet","score":0.8}]}"""
            )
        )
        val client = clientFor(server, SearchProviderType.EXA, "/exa")

        val response = runBlocking {
            client.search("semantic search", SearchProvider(name = "Exa", type = SearchProviderType.EXA, apiKey = "exa-secret"))
        }
        val request = server.takeRequest()
        val body = JSONObject(request.body.readUtf8())

        assertEquals("POST", request.method)
        assertEquals("/exa", request.requestUrl?.encodedPath)
        assertEquals("exa-secret", request.getHeader("x-api-key"))
        assertEquals("semantic search", body.getString("query"))
        assertEquals("Neural snippet", response.results.single().content)
    }

    @Test
    fun serperSendsApiKeyHeaderAndMapsGoogleOrganicResults() = withServer { server ->
        server.enqueue(
            MockResponse().setBody(
                """{"organic":[{"title":"Serper result","link":"https://example.test/d","snippet":"Google snippet"}]}"""
            )
        )
        val client = clientFor(server, SearchProviderType.SERPER, "/serper")

        val response = runBlocking {
            client.search("provider test", SearchProvider(name = "Serper", type = SearchProviderType.SERPER, apiKey = "serper-secret"))
        }
        val request = server.takeRequest()
        val body = JSONObject(request.body.readUtf8())

        assertEquals("POST", request.method)
        assertEquals("/serper", request.requestUrl?.encodedPath)
        assertEquals("serper-secret", request.getHeader("X-API-KEY"))
        assertEquals("provider test", body.getString("q"))
        assertEquals("https://example.test/d", response.results.single().url)
        assertEquals("Google snippet", response.results.single().content)
    }

    @Test
    fun missingKeyFailsBeforeMakingARequest() = withServer { server ->
        val client = clientFor(server, SearchProviderType.TAVILY, "/tavily")
        val error = runCatching {
            runBlocking {
                client.search("query", SearchProvider(name = "Tavily", type = SearchProviderType.TAVILY, apiKey = ""))
            }
        }.exceptionOrNull()

        assertEquals("Search API key is missing", error?.message)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun providerErrorBodyIsNotExposedWithTheCredential() = withServer { server ->
        server.enqueue(MockResponse().setResponseCode(401).setBody("invalid secret: tavily-secret"))
        val client = clientFor(server, SearchProviderType.TAVILY, "/tavily")
        val error = runCatching {
            runBlocking {
                client.search("query", SearchProvider(name = "Tavily", type = SearchProviderType.TAVILY, apiKey = "tavily-secret"))
            }
        }.exceptionOrNull()

        assertNotNull(error)
        assertTrue(error?.message.orEmpty().contains("HTTP 401"))
        assertFalse(error?.message.orEmpty().contains("tavily-secret"))
    }

    private fun clientFor(server: MockWebServer, type: SearchProviderType, path: String): SearchClient =
        SearchClient(endpointOverrides = mapOf(type to server.url(path).toString()))

    private fun withServer(block: (MockWebServer) -> Unit) {
        val server = MockWebServer()
        server.start()
        try {
            block(server)
        } finally {
            server.shutdown()
        }
    }
}
