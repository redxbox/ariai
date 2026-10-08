package com.ariai.app.data.remote

import com.ariai.app.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
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
        when (provider.type) {
            SearchProviderType.TAVILY -> searchTavily(query, provider.apiKey)
            SearchProviderType.BRAVE -> searchBrave(query, provider.apiKey)
            SearchProviderType.EXA -> searchExa(query, provider.apiKey)
            SearchProviderType.SERPER -> searchSerper(query, provider.apiKey)
            else -> searchDuckDuckGo(query)
        }
    }

    private fun searchTavily(query: String, apiKey: String): SearchResponse {
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
        val response = client.newCall(request).execute()
        val body = JSONObject(response.body!!.string())
        val results = mutableListOf<SearchResult>()
        val arr = body.optJSONArray("results") ?: return SearchResponse(query, emptyList(), SearchProviderType.TAVILY)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            results.add(SearchResult(
                title = obj.optString("title"),
                url = obj.optString("url"),
                content = obj.optString("content"),
                score = obj.optDouble("score", 0.0)
            ))
        }
        return SearchResponse(query, results, SearchProviderType.TAVILY)
    }

    private fun searchBrave(query: String, apiKey: String): SearchResponse {
        val request = Request.Builder()
            .url("https://api.search.brave.com/res/v1/web/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&count=5")
            .addHeader("X-Subscription-Token", apiKey)
            .addHeader("Accept", "application/json")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val body = JSONObject(response.body!!.string())
        val results = mutableListOf<SearchResult>()
        val web = body.optJSONObject("web")?.optJSONArray("results") ?: return SearchResponse(query, emptyList(), SearchProviderType.BRAVE)
        for (i in 0 until web.length()) {
            val obj = web.getJSONObject(i)
            results.add(SearchResult(
                title = obj.optString("title"),
                url = obj.optString("url"),
                content = obj.optString("description"),
                score = 0.0
            ))
        }
        return SearchResponse(query, results, SearchProviderType.BRAVE)
    }

    private fun searchExa(query: String, apiKey: String): SearchResponse {
        val json = JSONObject().apply {
            put("query", query)
            put("numResults", 5)
            put("type", "auto")
        }
        val request = Request.Builder()
            .url("https://api.exa.ai/search")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("x-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .build()
        val response = client.newCall(request).execute()
        val body = JSONObject(response.body!!.string())
        val results = mutableListOf<SearchResult>()
        val arr = body.optJSONArray("results") ?: return SearchResponse(query, emptyList(), SearchProviderType.EXA)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            results.add(SearchResult(
                title = obj.optString("title"),
                url = obj.optString("url"),
                content = obj.optString("text", obj.optString("snippet", "")),
                score = obj.optDouble("score", 0.0)
            ))
        }
        return SearchResponse(query, results, SearchProviderType.EXA)
    }

    private fun searchSerper(query: String, apiKey: String): SearchResponse {
        val json = JSONObject().apply {
            put("q", query)
            put("num", 5)
        }
        val request = Request.Builder()
            .url("https://google.serper.dev/search")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("X-API-KEY", apiKey)
            .build()
        val response = client.newCall(request).execute()
        val body = JSONObject(response.body!!.string())
        val results = mutableListOf<SearchResult>()
        val arr = body.optJSONArray("organic") ?: return SearchResponse(query, emptyList(), SearchProviderType.SERPER)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            results.add(SearchResult(
                title = obj.optString("title"),
                url = obj.optString("link"),
                content = obj.optString("snippet"),
                score = 0.0
            ))
        }
        return SearchResponse(query, results, SearchProviderType.SERPER)
    }

    private fun searchDuckDuckGo(query: String): SearchResponse {
        // Fallback: use DuckDuckGo instant answer (limited but free)
        return SearchResponse(query, listOf(
            SearchResult(
                title = "Web search disabled - add API key in settings",
                url = "",
                content = "Please configure Tavily, Brave, Exa or Serper API key in Settings > Search to enable real web search. Query was: $query",
                score = 0.0
            )
        ), SearchProviderType.CUSTOM)
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
