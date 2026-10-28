package com.ariai.app.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class PublicUrlFetcherTest {
    @Test
    fun `rejects local and credential-bearing urls before making a request`() {
        val fetcher = PublicUrlFetcher()
        assertThrows(IOException::class.java) { runBlocking { fetcher.fetch("http://127.0.0.1/") } }
        assertThrows(IOException::class.java) { runBlocking { fetcher.fetch("http://127.1/") } }
        assertThrows(IOException::class.java) { runBlocking { fetcher.fetch("http://169.254.169.254/") } }
        assertThrows(IOException::class.java) { runBlocking { fetcher.fetch("http://[::1]/") } }
        assertThrows(IOException::class.java) { runBlocking { fetcher.fetch("http://localhost/") } }
        assertThrows(IOException::class.java) { runBlocking { fetcher.fetch("https://user:password@example.com/") } }
    }
}
