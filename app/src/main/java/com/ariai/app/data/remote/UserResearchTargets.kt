package com.ariai.app.data.remote

import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole

internal data class UserResearchTargets(
    val publicUrls: List<String>,
    val githubRepository: GitHubRepositoryClient.Repository?
)

private val USER_URL_REGEX = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)

/**
 * Carry explicit links forward when the user follows up about them, but only from recent user
 * messages that are also part of the model's current conversation context. Assistant-invented URLs
 * are never treated as permission to browse.
 */
internal suspend fun userResearchTargets(
    conversationContext: List<ChatMessage>,
    isSafePublicUrl: suspend (String) -> Boolean
): UserResearchTargets {
    val recentUserMessages = conversationContext
        .asSequence()
        .filter { it.role == MessageRole.USER }
        .toList()
        .takeLast(MAX_USER_MESSAGES_WITH_LINKS)

    val candidateUrls = recentUserMessages.asSequence()
        .flatMap { message -> USER_URL_REGEX.findAll(message.content).map { it.value } }
        .map { it.trimEnd('.', ',', ';', ')', ']', '،', '؛') }
        .distinct()
        .toList()
    val urls = mutableListOf<String>()
    for (url in candidateUrls) {
        val isSafe = try {
            isSafePublicUrl(url)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
        if (isSafe) urls += url
        if (urls.size == MAX_PUBLIC_URLS_PER_TURN) break
    }

    val repository = recentUserMessages.asReversed()
        .firstNotNullOfOrNull { GitHubRepositoryClient.Repository.fromUserText(it.content) }

    return UserResearchTargets(publicUrls = urls, githubRepository = repository)
}

private const val MAX_USER_MESSAGES_WITH_LINKS = 10
private const val MAX_PUBLIC_URLS_PER_TURN = 10
