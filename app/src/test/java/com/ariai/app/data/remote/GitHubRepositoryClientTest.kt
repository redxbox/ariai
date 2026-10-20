package com.ariai.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GitHubRepositoryClientTest {
    @Test
    fun `extracts only a real github repository URL`() {
        val repo = GitHubRepositoryClient.Repository.fromUserText("Review https://github.com/redxbox/ariai/tree/main/app")
        assertEquals("redxbox/ariai", repo?.key)
    }

    @Test
    fun `does not accept non github or incomplete links`() {
        assertNull(GitHubRepositoryClient.Repository.fromUserText("https://example.com/owner/repo"))
        assertNull(GitHubRepositoryClient.Repository.fromUserText("https://github.com/owner"))
        assertNull(GitHubRepositoryClient.Repository.fromUserText("https://github.com.evil.example/owner/repo"))
        assertNull(GitHubRepositoryClient.Repository.fromUserText("https://github.com/../.."))
    }
}
