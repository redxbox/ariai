package com.ariai.app.data.remote

import java.io.IOException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class GitHubRepositoryClientTest {
    @Test
    fun `extracts only a real github repository URL`() {
        val repo = GitHubRepositoryClient.Repository.fromUserText("Review https://github.com/redxbox/ariai/tree/main/app")
        assertEquals("redxbox/ariai", repo?.key)
        assertEquals("main", repo?.ref)
    }

    @Test
    fun `parses an explicitly requested branch ref from the repository URL`() {
        val repo = GitHubRepositoryClient.Repository.fromUserText(
            "Review https://github.com/redxbox/ariai?ref=arena%2F5f5dd5fd-ariai"
        )
        assertEquals("redxbox/ariai", repo?.key)
        assertEquals("arena/5f5dd5fd-ariai", repo?.ref)
    }

    @Test
    fun `parses percent-encoded slash refs from tree and blob URLs without consuming the subpath`() {
        val treeRef = GitHubRepositoryClient.Repository.fromUserText(
            "https://github.com/example/project/tree/feature%2Fapi/app/src"
        )
        assertEquals("feature/api", treeRef?.ref)

        val blobRef = GitHubRepositoryClient.Repository.fromUserText(
            "https://github.com/example/project/blob/release%2F1/README.md"
        )
        assertEquals("release/1", blobRef?.ref)
    }

    @Test
    fun `rejects unsafe or ambiguous branch refs`() {
        assertNull(GitHubRepositoryClient.Repository.fromUserText(
            "https://github.com/redxbox/ariai?ref=arena%2F..%2Fmain"
        ))
        assertNull(GitHubRepositoryClient.Repository.fromUserText(
            "https://github.com/redxbox/ariai?ref=main&ref=other"
        ))
    }

    @Test
    fun `invalid tree response is not reported as an empty repository`() {
        assertThrows(IOException::class.java) {
            requireGitHubTreeArray(JSONObject("{}"))
        }
        assertEquals(0, requireGitHubTreeArray(JSONObject("""{"tree":[]}""")).length())
    }

    @Test
    fun `does not accept non github or incomplete links`() {
        assertNull(GitHubRepositoryClient.Repository.fromUserText("https://example.com/owner/repo"))
        assertNull(GitHubRepositoryClient.Repository.fromUserText("https://github.com/owner"))
        assertNull(GitHubRepositoryClient.Repository.fromUserText("https://github.com.evil.example/owner/repo"))
        assertNull(GitHubRepositoryClient.Repository.fromUserText("https://github.com/../.."))
    }
}
