package com.ariai.app.data.repository

import com.ariai.app.data.models.WorkspaceFile

/** Builds a static-only preview document. It never enables scripts or loads remote resources. */
fun renderStaticHtmlPreview(selectedFile: WorkspaceFile?, files: List<WorkspaceFile>): String {
    val file = selectedFile ?: return previewMessage("Select an HTML file to preview.")
    val extension = file.name.substringAfterLast('.', "").lowercase()
    if (extension != "html" && extension != "htm") return previewMessage("Only HTML files can be previewed.")

    val stylesheetLink = Regex("""(?is)<link\b[^>]*>""")
    val withLocalStyles = stylesheetLink.replace(file.content) { match ->
        val tag = match.value
        val relation = attribute(tag, "rel").orEmpty().lowercase().split(Regex("\\s+"))
        val href = attribute(tag, "href")
        val safeName = href
            ?.takeIf { it.isNotBlank() && !it.contains(":") && !it.startsWith("/") && ".." !in it }
            ?.substringBefore('#')
            ?.substringBefore('?')
            ?.substringAfterLast('/')
        val css = if ("stylesheet" in relation && safeName != null) {
            files.firstOrNull { it.name.equals(safeName, ignoreCase = true) }
        } else null
        if (css == null) "" else {
            val safeCss = Regex("(?i)</style").replace(css.content) { "<\\/style" }
            "<style>\n$safeCss\n</style>"
        }
    }

    var safe = withLocalStyles
    safe = Regex("""(?is)<(script|iframe|frame|object|embed|form|portal)\b[^>]*>.*?</\1\s*>""").replace(safe, "")
    safe = Regex("""(?is)<(script|iframe|frame|object|embed|form|portal)\b[^>]*?/?>""").replace(safe, "")
    safe = Regex("""(?is)<meta\b[^>]*http-equiv[^>]*>""").replace(safe, "")
    safe = Regex("""(?is)<base\b[^>]*>""").replace(safe, "")
    safe = Regex("""(?i)\s+on[a-z0-9_-]+\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>]+)""").replace(safe, "")
    safe = Regex("""(?i)\s+srcset\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>]+)""").replace(safe, "")
    val remoteUrlAttribute = Regex(
        """(?i)\b(href|src|action|formaction)\s*=\s*(?:(['"])\s*(?:javascript:|https?:|file:|blob:|intent:)[^'"]*\2|(?:javascript:|https?:|file:|blob:|intent:)[^\s>]+)"""
    )
    safe = remoteUrlAttribute.replace(safe) { match -> "${match.groupValues[1]}=\"#\"" }

    val headMatch = Regex("""(?is)<head\b[^>]*>(.*?)</head\s*>""").find(safe)
    val bodyMatch = Regex("""(?is)<body\b[^>]*>(.*?)</body\s*>""").find(safe)
    val cleanHead = headMatch?.groupValues?.getOrNull(1).orEmpty()
    val cleanBody = bodyMatch?.groupValues?.getOrNull(1) ?: safe
        .replace(Regex("""(?is)<head\b[^>]*>.*?</head\s*>"""), "")
        .replace(Regex("""(?is)<!doctype[^>]*>"""), "")
        .replace(Regex("""(?is)</?(?:html|body)\b[^>]*>"""), "")
    val csp = "default-src 'none'; img-src data:; style-src 'unsafe-inline'; font-src data:; media-src 'none'; script-src 'none'; connect-src 'none'; frame-src 'none'; object-src 'none'; form-action 'none'; base-uri 'none'"
    return "<!doctype html><html><head><meta charset=\"utf-8\"><meta http-equiv=\"Content-Security-Policy\" content=\"$csp\">$cleanHead</head><body>$cleanBody</body></html>"
}

private fun attribute(tag: String, name: String): String? {
    val pattern = Regex("""(?is)\b${Regex.escape(name)}\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))""")
    val match = pattern.find(tag) ?: return null
    return match.groupValues.drop(1).firstOrNull { it.isNotEmpty() }
}

private fun previewMessage(message: String): String =
    "<!doctype html><html><head><meta charset=\"utf-8\"></head><body><main>$message</main></body></html>"

/** A tiny read-only command parser. Commands inspect the supplied list only; no process is started. */
fun executeWorkspaceConsole(command: String, files: List<WorkspaceFile>): String {
    val tokens = tokenizeWorkspaceCommand(command)
    if (tokens.isEmpty()) return ""
    return when (tokens.first().lowercase()) {
        "help" -> "Read-only workspace commands:\nhelp · pwd · ls · cat <file> · wc <file> · grep <text> <file> · clear\nNo shell or code execution is available."
        "pwd" -> "/workspace (private app storage)"
        "ls" -> files.sortedBy { it.name.lowercase() }.joinToString("\n") { it.name }
            .ifBlank { "Workspace is empty." }
        "cat" -> {
            val name = tokens.getOrNull(1) ?: return "Usage: cat <file>"
            val file = files.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: return "File not found: $name"
            if (file.content.length <= MAX_CONSOLE_OUTPUT_CHARS) file.content
            else file.content.take(MAX_CONSOLE_OUTPUT_CHARS) + "\n… output truncated …"
        }
        "wc" -> {
            val name = tokens.getOrNull(1) ?: return "Usage: wc <file>"
            val file = files.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: return "File not found: $name"
            val lines = if (file.content.isEmpty()) 0 else file.content.count { it == '\n' } + 1
            val words = file.content.trim().split(Regex("\\s+")).count { it.isNotBlank() }
            "$lines lines · $words words · ${file.content.length} characters · ${file.name}"
        }
        "grep" -> {
            if (tokens.size < 3) return "Usage: grep <text> <file>"
            val name = tokens.last()
            val needle = tokens.subList(1, tokens.lastIndex).joinToString(" ")
            val file = files.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: return "File not found: $name"
            file.content.lineSequence().mapIndexedNotNull { index, line ->
                if (line.contains(needle, ignoreCase = true)) "${index + 1}: $line" else null
            }.take(100).joinToString("\n").ifBlank { "No matches." }
        }
        "clear" -> ""
        else -> "Command not available. This read-only console only inspects workspace files; it does not run shell commands or code."
    }
}

private fun tokenizeWorkspaceCommand(command: String): List<String> {
    val tokens = mutableListOf<String>()
    val current = StringBuilder()
    var quote: Char? = null
    var index = 0
    while (index < command.length) {
        val char = command[index]
        when {
            quote != null && char == quote -> quote = null
            quote != null && char == '\\' && index + 1 < command.length && command[index + 1] == quote -> {
                current.append(quote)
                index++
            }
            quote != null -> current.append(char)
            char == '\'' || char == '"' -> quote = char
            char.isWhitespace() -> if (current.isNotEmpty()) {
                tokens += current.toString()
                current.setLength(0)
            }
            else -> current.append(char)
        }
        index++
    }
    if (current.isNotEmpty() || quote != null) tokens += current.toString()
    return tokens
}

private const val MAX_CONSOLE_OUTPUT_CHARS = 20_000
