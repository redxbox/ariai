package com.ariai.app.data.models

enum class SearchProviderType {
    TAVILY,
    BRAVE,
    EXA,
    SERPER,
    BOCHA,
    CUSTOM
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
