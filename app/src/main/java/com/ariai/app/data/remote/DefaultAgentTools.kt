package com.ariai.app.data.remote

import com.ariai.app.util.SafeCalculator
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

object DefaultAgentTools {
    fun calculator(): ChatFunctionTool = ChatFunctionTool(
        name = "calculate",
        description = "Evaluate a basic arithmetic expression using numbers, parentheses, +, -, *, /, %, and ^. Never use this tool for arbitrary code.",
        parameters = objectSchema(
            properties = JSONObject().put("expression", stringProperty("Arithmetic expression to evaluate")),
            required = listOf("expression")
        )
    ) { args ->
        SafeCalculator.evaluate(args.optString("expression"))
    }

    fun webSearch(execute: suspend (String) -> String): ChatFunctionTool = ChatFunctionTool(
        name = "search_web",
        description = "Search the web for current information. Use focused query terms, then base claims on returned results and cite their URLs.",
        parameters = objectSchema(
            properties = JSONObject().put("query", stringProperty("Focused web search query")),
            required = listOf("query")
        )
    ) { args ->
        val query = args.optString("query").trim()
        require(query.isNotBlank()) { "Search query is required" }
        require(query.length <= 500) { "Search query is too long" }
        execute(query)
    }

    fun openUserUrl(urls: List<String>, fetcher: PublicUrlFetcher): ChatFunctionTool? {
        val allowed = urls.mapNotNull { raw ->
            runCatching {
                val uri = URI(raw)
                val scheme = uri.scheme?.lowercase()
                require(raw.length <= 2_048 && uri.normalize() == uri) { "Invalid URL path" }
                require((scheme == "http" || scheme == "https") && uri.host != null && uri.userInfo == null) {
                    "Only public HTTP(S) URLs without credentials are accepted"
                }
                uri.toASCIIString()
            }.getOrNull()
        }.toSet()
        if (allowed.isEmpty()) return null
        val hosts = allowed.mapNotNull { runCatching { URI(it).host?.lowercase() }.getOrNull() }.toSet()
        return ChatFunctionTool(
            name = "open_user_url",
            description = "Fetch and read a public web page explicitly linked by the user in this request. Use only the user's supplied URL(s); do not claim to have read a page until this tool returns its content.",
            parameters = objectSchema(
                properties = JSONObject().put("url", stringProperty("One of the exact URLs supplied by the user")),
                required = listOf("url")
            )
        ) { args ->
            val requested = args.optString("url").trim().trimEnd('.', ',', ';', ')', ']', '،', '؛')
            val uri = runCatching { URI(requested) }.getOrElse { throw IllegalArgumentException("Invalid URL") }
            val host = uri.host?.lowercase()
            val scheme = uri.scheme?.lowercase()
            require(uri.normalize() == uri && host != null && host in hosts &&
                (scheme == "http" || scheme == "https") && uri.toASCIIString() in allowed
            ) {
                "Only exact URLs supplied by the user in this turn may be opened."
            }
            fetcher.fetch(uri.toASCIIString())
        }
    }

    fun githubRepository(repository: GitHubRepositoryClient.Repository, client: GitHubRepositoryClient): List<ChatFunctionTool> = listOf(
        ChatFunctionTool(
            name = "github_list_files",
            description = "List the files in the public GitHub repository URL supplied by the user. Use this before choosing source files to read. Pass path_prefix to focus on a folder when the tree is large.",
            parameters = objectSchema(
                properties = JSONObject().put("path_prefix", stringProperty("Optional repository folder prefix, such as app/src/main")),
                required = emptyList()
            )
        ) { args -> client.listFiles(repository, args.optString("path_prefix").takeIf { it.isNotBlank() }) },
        ChatFunctionTool(
            name = "github_read_file",
            description = "Read one UTF-8 text file from the exact public GitHub repository supplied by the user. Use paths returned by github_list_files; sensitive key files and binary files are blocked.",
            parameters = objectSchema(
                properties = JSONObject().put("path", stringProperty("Repository-relative path returned by github_list_files")),
                required = listOf("path")
            )
        ) { args ->
            val path = args.optString("path").trim()
            require(path.isNotBlank()) { "Repository file path is required" }
            client.readFile(repository, path)
        }
    )

    private fun stringProperty(description: String) = JSONObject()
        .put("type", "string")
        .put("description", description)

    private fun objectSchema(properties: JSONObject, required: List<String>) = JSONObject()
        .put("type", "object")
        .put("properties", properties)
        .put("required", JSONArray(required))
}
