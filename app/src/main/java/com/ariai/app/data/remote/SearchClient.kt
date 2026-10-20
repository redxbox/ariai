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
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class SearchClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun search(
        query: String,
        provider: SearchProvider
    ): SearchResponse = withContext(Dispatchers.IO) {
        if (query.isBlank()) throw IllegalArgumentException("Search query is empty")
        if (!provider.enabled) throw IllegalArgumentException("Search service is disabled")
        if (provider.type != SearchProviderType.CUSTOM && provider.apiKey.isBlank()) {
            throw IllegalArgumentException("Search API key is missing")
        }
        when (provider.type) {
            SearchProviderType.TAVILY -> searchTavily(query, provider.apiKey)
            SearchProviderType.BRAVE -> searchBrave(query, provider.apiKey)
            SearchProviderType.EXA -> searchExa(query, provider.apiKey)
            SearchProviderType.SERPER -> searchSerper(query, provider.apiKey)
            SearchProviderType.BOCHA -> throw IOException("Bocha search is not supported yet")
            SearchProviderType.CUSTOM -> throw IOException("Custom search endpoints are not supported yet")
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
            .url("https://api.tavily.com/search")
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
            .url("https://api.search.brave.com/res/v1/web/search?q=$encodedQuery&count=5")
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
            .url("https://api.exa.ai/search")
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
            .url("https://google.serper.dev/search")
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

    private suspend fun executeJson(request: Request, service: String): JSONObject = suspendCancellableCoroutine { continuation ->
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
                        val body = received.body?.string().orEmpty()
                        if (!received.isSuccessful) {
                            throw IOException("$service search failed: HTTP ${received.code}: ${body.take(300)}")
                        }
                        if (body.isBlank()) throw IOException("$service search returned an empty response")
                        JSONObject(body)
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
