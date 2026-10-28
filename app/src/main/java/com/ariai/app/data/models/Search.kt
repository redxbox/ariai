package com.ariai.app.data.models

enum class SearchProviderType {
    BING,
    TAVILY,
    BRAVE,
    EXA,
    SERPER,
    BOCHA,
    CUSTOM
}

/** Local providers exposed in chat; Brave is the default, while Bing remains a keyless option. */
val LOCAL_SEARCH_PROVIDER_PRIORITY = listOf("brave", "bing")

/** Preserves a valid explicit choice; a fresh install defaults to Brave even before its key is added. */
fun resolveSearchProviderKey(searchKeys: Map<String, String>, preferredProvider: String?): String? {
    val preferred = preferredProvider?.trim()?.lowercase()
        ?.takeIf { it in LOCAL_SEARCH_PROVIDER_PRIORITY }
    return preferred ?: LOCAL_SEARCH_PROVIDER_PRIORITY.firstOrNull()
}

fun searchProviderTypeForKey(provider: String): SearchProviderType? = when (provider.trim().lowercase()) {
    "bing" -> SearchProviderType.BING
    "tavily" -> SearchProviderType.TAVILY
    "brave" -> SearchProviderType.BRAVE
    "exa" -> SearchProviderType.EXA
    "serper" -> SearchProviderType.SERPER
    else -> null
}

fun searchProviderDisplayName(provider: String?): String? = when (provider?.trim()?.lowercase()) {
    "bing" -> "Bing"
    "tavily" -> "Tavily"
    "brave" -> "Brave Search"
    "exa" -> "Exa"
    "serper" -> "Google Search"
    else -> null
}

data class SearchProvider(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val type: SearchProviderType,
    val apiKey: String,
    val baseUrl: String? = null,
    val enabled: Boolean = true
)

data class SearchResult(
    val title: String,
    val url: String,
    val content: String,
    val score: Double = 0.0,
    val publishedDate: String? = null
)

data class SearchResponse(
    val query: String,
    val results: List<SearchResult>,
    val provider: SearchProviderType
)

data class ImageGenRequest(
    val prompt: String,
    val model: String = "dall-e-3",
    val size: String = "1024x1024",
    val quality: String = "standard",
    val n: Int = 1,
    val style: String? = null
)

data class ImageGenResponse(
    val images: List<GeneratedImage>,
    val model: String
)

data class GeneratedImage(
    val url: String? = null,
    val base64: String? = null,
    val revisedPrompt: String? = null
)
