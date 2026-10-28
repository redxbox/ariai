package com.ariai.app.data.remote

import com.ariai.app.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.IOException
import java.io.StringReader
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

class SearchClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    /** Injectable only to keep provider protocol tests local; production uses the official endpoints. */
    private val endpointOverrides: Map<SearchProviderType, String> = emptyMap()
) {
    private fun endpoint(type: SearchProviderType, officialUrl: String): String =
        endpointOverrides[type] ?: officialUrl

    suspend fun search(
        query: String,
        provider: SearchProvider
    ): SearchResponse = withContext(Dispatchers.IO) {
        if (query.isBlank()) throw IllegalArgumentException("Search query is empty")
        if (!provider.enabled) throw IllegalArgumentException("Search service is disabled")
        if (provider.type !in setOf(SearchProviderType.BING, SearchProviderType.CUSTOM) && provider.apiKey.isBlank()) {
            throw IllegalArgumentException("Search API key is missing")
        }
        when (provider.type) {
            SearchProviderType.BING -> searchBing(query)
            SearchProviderType.TAVILY -> searchTavily(query, provider.apiKey)
            SearchProviderType.BRAVE -> searchBrave(query, provider.apiKey)
            SearchProviderType.EXA -> searchExa(query, provider.apiKey)
            SearchProviderType.SERPER -> searchSerper(query, provider.apiKey)
            SearchProviderType.BOCHA -> throw IOException("Bocha search is not supported yet")
            SearchProviderType.CUSTOM -> throw IOException("Custom search endpoints are not supported yet")
        }
    }

    /** Keyless Bing RSS search; only public result titles, links and snippets are retained. */
    private suspend fun searchBing(query: String): SearchResponse {
        val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8.name())
        val request = Request.Builder()
            .url("${endpoint(SearchProviderType.BING, "https://www.bing.com/search")}?format=rss&q=$encodedQuery&count=5")
            .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0")
            .header("Accept", "application/rss+xml, application/xml;q=0.9, */*;q=0.8")
            .get()
            .build()
        val body = executeBody(request, "Bing Search")
        return try {
            parseBingRss(query, body)
        } catch (failure: IOException) {
            throw failure
        } catch (_: Exception) {
            throw IOException("Bing Search returned an invalid RSS response")
        }
    }

    private suspend fun searchTavily(query: String, apiKey: String): SearchResponse {
        val json = JSONObject().apply {
            put("api_key", apiKey)
            put("query", query)
            put("search_depth", "advanced")
            put("include_answer", true)
            put("max_results", 5)
        }
        val request = Request.Builder()
            .url(endpoint(SearchProviderType.TAVILY, "https://api.tavily.com/search"))
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val body = executeJson(request, "Tavily")
        val arr = body.optJSONArray("results") ?: return SearchResponse(query, emptyList(), SearchProviderType.TAVILY)
        val results = (0 until arr.length()).mapNotNull { index ->
            val obj = arr.optJSONObject(index) ?: return@mapNotNull null
            SearchResult(
                title = obj.optString("title"),
                url = obj.optString("url"),
                content = obj.optString("content"),
                score = obj.optDouble("score", 0.0)
            )
        }
        return SearchResponse(query, results, SearchProviderType.TAVILY)
    }

    private suspend fun searchBrave(query: String, apiKey: String): SearchResponse {
        val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8.name())
        val request = Request.Builder()
            .url("${endpoint(SearchProviderType.BRAVE, "https://api.search.brave.com/res/v1/web/search")}?q=$encodedQuery&count=5")
            .header("X-Subscription-Token", apiKey)
            .header("Accept", "application/json")
            .get()
            .build()
        val body = executeJson(request, "Brave Search")
        val web = body.optJSONObject("web")?.optJSONArray("results")
            ?: return SearchResponse(query, emptyList(), SearchProviderType.BRAVE)
        val results = (0 until web.length()).mapNotNull { index ->
            val obj = web.optJSONObject(index) ?: return@mapNotNull null
            SearchResult(
                title = obj.optString("title"),
                url = obj.optString("url"),
                content = obj.optString("description"),
                score = 0.0
            )
        }
        return SearchResponse(query, results, SearchProviderType.BRAVE)
    }

    private suspend fun searchExa(query: String, apiKey: String): SearchResponse {
        val json = JSONObject().apply {
            put("query", query)
            put("numResults", 5)
            put("type", "auto")
        }
        val request = Request.Builder()
            .url(endpoint(SearchProviderType.EXA, "https://api.exa.ai/search"))
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .header("x-api-key", apiKey)
            .header("Content-Type", "application/json")
            .build()
        val body = executeJson(request, "Exa")
        val arr = body.optJSONArray("results") ?: return SearchResponse(query, emptyList(), SearchProviderType.EXA)
        val results = (0 until arr.length()).mapNotNull { index ->
            val obj = arr.optJSONObject(index) ?: return@mapNotNull null
            SearchResult(
                title = obj.optString("title"),
                url = obj.optString("url"),
                content = obj.optString("text", obj.optString("snippet", "")),
                score = obj.optDouble("score", 0.0)
            )
        }
        return SearchResponse(query, results, SearchProviderType.EXA)
    }

    private suspend fun searchSerper(query: String, apiKey: String): SearchResponse {
        val json = JSONObject().apply {
            put("q", query)
            put("num", 5)
        }
        val request = Request.Builder()
            .url(endpoint(SearchProviderType.SERPER, "https://google.serper.dev/search"))
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .header("X-API-KEY", apiKey)
            .build()
        val body = executeJson(request, "Serper")
        val arr = body.optJSONArray("organic") ?: return SearchResponse(query, emptyList(), SearchProviderType.SERPER)
        val results = (0 until arr.length()).mapNotNull { index ->
            val obj = arr.optJSONObject(index) ?: return@mapNotNull null
            SearchResult(
                title = obj.optString("title"),
                url = obj.optString("link"),
                content = obj.optString("snippet"),
                score = 0.0
            )
        }
        return SearchResponse(query, results, SearchProviderType.SERPER)
    }

    private suspend fun executeJson(request: Request, service: String): JSONObject =
        JSONObject(executeBody(request, service))

    private suspend fun executeBody(request: Request, service: String): String = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWith(Result.failure(e))
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (!continuation.isActive) {
                    response.close()
                    return
                }
                val result = runCatching {
                    response.use { received ->
                        val body = received.body?.readUtf8AtMost(HttpBodyLimits.SEARCH_BYTES).orEmpty()
                        if (!received.isSuccessful) {
                            // Do not surface or log provider error bodies; some vendors echo credential details.
                            throw IOException("$service search failed (HTTP ${received.code})")
                        }
                        if (body.isBlank()) throw IOException("$service search returned an empty response")
                        body
                    }
                }
                if (continuation.isActive) continuation.resumeWith(result)
            }
        })
    }

    fun formatResultsForLLM(response: SearchResponse): String {
        if (response.results.isEmpty()) return "No search results found for '${response.query}'"
        return buildString {
            appendLine("Search results for '${response.query}':")
            response.results.forEachIndexed { idx, result ->
                appendLine("${idx + 1}. ${result.title}")
                appendLine("   URL: ${result.url}")
                appendLine("   Content: ${result.content.take(500)}")
                appendLine()
            }
        }
    }
}

/** Parses Bing's public RSS response without permitting DTDs or external XML entities. */
internal fun parseBingRss(query: String, xml: String): SearchResponse {
    if (xml.contains("<!DOCTYPE", ignoreCase = true)) {
        throw IOException("Bing Search responses cannot contain a document type")
    }
    val factory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = false
        isExpandEntityReferences = false
        // These features vary slightly across Android/JAXP parsers; the precheck and resolver below
        // still block DTD/entity access if a platform parser does not recognize one of them.
        runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
        runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
    }
    val builder = factory.newDocumentBuilder().apply {
        setEntityResolver { _, _ -> InputSource(StringReader("")) }
    }
    val document = builder.parse(InputSource(StringReader(xml)))
    if (document.documentElement?.tagName !in setOf("rss", "feed")) {
        throw IOException("Bing Search returned an unexpected response")
    }
    val items = document.getElementsByTagName("item")
    val results = (0 until minOf(items.length, 5)).mapNotNull { index ->
        val item = items.item(index) as? Element ?: return@mapNotNull null
        fun value(tag: String): String = item.getElementsByTagName(tag).item(0)?.textContent.orEmpty().trim()
        val title = stripSearchMarkup(value("title")).take(300)
        val url = value("link").take(2_000)
        if (title.isBlank() || !(url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true))) {
            return@mapNotNull null
        }
        SearchResult(
            title = title,
            url = url,
            content = stripSearchMarkup(value("description")).take(1_500),
            score = 0.0,
            publishedDate = value("pubDate").takeIf { it.isNotBlank() }?.take(100)
        )
    }
    return SearchResponse(query, results, SearchProviderType.BING)
}

private fun stripSearchMarkup(value: String): String =
    value.replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
