package com.ariai.app.ui.screens

import kotlinx.coroutines.CompletableDeferred

/** Orders a first send behind the asynchronous insert of the new chat row. */
internal class ChatPersistenceGate {
    private val pending = mutableMapOf<String, CompletableDeferred<Boolean>>()

    fun begin(chatId: String): CompletableDeferred<Boolean> =
        CompletableDeferred<Boolean>().also { pending[chatId] = it }

    fun complete(chatId: String, ticket: CompletableDeferred<Boolean>, saved: Boolean) {
        ticket.complete(saved)
        // Once persisted, later sends no longer need to wait. Keep failures blocked until the
        // chat is discarded so a message can never race into a chat row that was not created.
        if (saved && pending[chatId] === ticket) pending.remove(chatId)
    }

    suspend fun await(chatId: String): Boolean = pending[chatId]?.await() ?: true

    fun forget(chatId: String) {
        pending.remove(chatId)
    }
}
