package com.ariai.app.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ariai.app.data.models.Agent
import com.ariai.app.data.models.McpServerItem
import com.ariai.app.data.models.WorkspaceFile
import com.ariai.app.ui.screens.AgentStudioScreen
import com.ariai.app.ui.screens.QuickParametersSheet
import com.ariai.app.ui.screens.SearchServiceScreen
import com.ariai.app.ui.screens.SimpleMcpScreen
import com.ariai.app.ui.screens.WorkspaceScreen
import com.ariai.app.ui.theme.AriAiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeatureScreensUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun workspaceConsoleTabIsCompactAndCommandsCannotRunShellCode() {
        val files = mutableStateListOf(WorkspaceFile("index.html", "<h1>Local page</h1>", 1L))
        compose.setContent {
            AriAiTheme(darkTheme = false, dynamicColor = false) {
                WorkspaceScreen(
                    files = files,
                    onBack = {},
                    onCreateFile = { name, content, onResult ->
                        files.add(WorkspaceFile(name, content, 2L))
                        onResult(null)
                    },
                    onSaveFile = { name, content, onResult ->
                        val index = files.indexOfFirst { it.name == name }
                        if (index >= 0) files[index] = files[index].copy(content = content, updatedAt = 3L)
                        onResult(null)
                    },
                    onDeleteFile = { name, onResult ->
                        files.removeAll { it.name == name }
                        onResult(null)
                    },
                    onGenerateFile = { _, _, _, onResult -> onResult(null, "No provider configured") }
                )
            }
        }

        compose.onNodeWithTag("workspace-tab-console").assertIsDisplayed().performClick()
        compose.onNodeWithText("Console").assertIsDisplayed()
        compose.onNodeWithText("This console can list/read files in this workspace only. It does not run shell commands or code.")
            .assertExists()
        compose.onNodeWithText("Command").performTextInput("help")
        compose.onNodeWithText("Run").performClick()
        compose.onNodeWithText("Read-only workspace commands:", substring = true).assertExists()

        compose.onNodeWithText("Command").performTextInput("rm -rf /")
        compose.onNodeWithText("Run").performClick()
        compose.onNodeWithText("Command not available.", substring = true).assertExists()
    }

    @Test
    fun workspaceFileCanBeCreatedEditedSavedAndPreviewed() {
        val files = mutableStateListOf<WorkspaceFile>()
        compose.setContent {
            AriAiTheme(darkTheme = false, dynamicColor = false) {
                WorkspaceScreen(
                    files = files,
                    onBack = {},
                    onCreateFile = { name, content, onResult ->
                        files.add(WorkspaceFile(name, content, 1L))
                        onResult(null)
                    },
                    onSaveFile = { name, content, onResult ->
                        val index = files.indexOfFirst { it.name == name }
                        if (index >= 0) files[index] = files[index].copy(content = content, updatedAt = 2L)
                        onResult(null)
                    },
                    onDeleteFile = { name, onResult ->
                        files.removeAll { it.name == name }
                        onResult(null)
                    },
                    onGenerateFile = { _, _, _, onResult -> onResult(null, "No provider configured") }
                )
            }
        }

        compose.onNodeWithContentDescription("New file").performClick()
        compose.onNodeWithTag("workspace-create-name").performTextInput("index.html")
        compose.onNodeWithTag("workspace-create-submit").performClick()
        compose.onNodeWithTag("workspace-file-content").performTextInput("\n<p>Saved from the editor.</p>")
        compose.onNodeWithText("Save file").performClick()

        assertEquals(1, files.size)
        assertTrue(files.single().content.contains("Saved from the editor."))
        compose.onNodeWithTag("workspace-tab-preview").performClick()
        compose.onNodeWithText("HTML preview is static. JavaScript and network requests are blocked.").assertExists()
    }

    @Test
    fun quickParametersSystemPromptCanBeSavedAndBottomActionRemainsReachable() {
        var savedPrompt: String? = null
        var savedTemperature = -1f
        var savedTopP = -1f

        compose.setContent {
            AriAiTheme(darkTheme = false, dynamicColor = false) {
                QuickParametersSheet(
                    temperature = 0.7f,
                    topP = 1f,
                    systemPrompt = null,
                    onSave = { temperature, topP, prompt ->
                        savedTemperature = temperature
                        savedTopP = topP
                        savedPrompt = prompt
                    },
                    onDismiss = {}
                )
            }
        }

        compose.onNodeWithText("Optional instructions for this conversation").performTextInput("Be concise and cite assumptions.")
        compose.onNodeWithTag("quick-parameters-save").performScrollTo().assertIsDisplayed().performClick()

        assertEquals("Be concise and cite assumptions.", savedPrompt)
        assertEquals(0.7f, savedTemperature, 0.001f)
        assertEquals(1f, savedTopP, 0.001f)
    }

    @Test
    fun agentPromptCanBeCreatedSavedAndStartedWithBottomActionVisible() {
        val agents = mutableStateListOf<Agent>()
        var savedAgent: Agent? = null
        var startedAgent: Agent? = null

        compose.setContent {
            AriAiTheme(darkTheme = false, dynamicColor = false) {
                AgentStudioScreen(
                    agents = agents,
                    providers = emptyList(),
                    onBack = {},
                    onSaveAgent = { saved ->
                        savedAgent = saved
                        agents.add(saved)
                    },
                    onDeleteAgent = { id -> agents.removeAll { it.id == id } },
                    onStartAgent = { startedAgent = it }
                )
            }
        }

        compose.onNodeWithContentDescription("Create agent").performClick()
        compose.onNodeWithTag("agent-name").performTextInput("Prompt QA")
        compose.onNodeWithTag("agent-system-prompt").performTextInput("Check facts, then answer clearly.")
        compose.onNodeWithTag("agent-save").performScrollTo().assertIsDisplayed().performClick()

        assertNotNull(savedAgent)
        assertEquals("Prompt QA", savedAgent?.name)
        assertEquals("Check facts, then answer clearly.", savedAgent?.systemPrompt)
        compose.onNodeWithText("Start chat").assertIsDisplayed().performClick()
        assertEquals(savedAgent?.id, startedAgent?.id)
        assertEquals(savedAgent?.systemPrompt, startedAgent?.systemPrompt)
    }

    @Test
    fun searchKeyCanBeTestedWithoutSavingAndThenStoredByTheSettingsCallback() {
        val searchKeys = mutableStateOf(emptyMap<String, String>())
        var testedProvider: String? = null
        var testedKey: String? = null

        compose.setContent {
            AriAiTheme(darkTheme = false, dynamicColor = false) {
                SearchServiceScreen(
                    searchKeys = searchKeys.value,
                    onSaveKey = { name, key ->
                        searchKeys.value = if (key.isBlank()) searchKeys.value - name else searchKeys.value + (name to key)
                    },
                    onTestKey = { name, key, callback ->
                        testedProvider = name
                        testedKey = key
                        callback(null)
                    },
                    onBack = {}
                )
            }
        }

        compose.onAllNodesWithText("No key").assertCountEquals(4)
        compose.onNodeWithText("Serper").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Tavily").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("search-key-tavily").performTextInput("test-key-from-ui")
        compose.onNodeWithTag("search-test-tavily").performClick()
        compose.onNodeWithText("Connection successful.").assertIsDisplayed()
        assertEquals("tavily", testedProvider)
        assertEquals("test-key-from-ui", testedKey)

        compose.onNodeWithText("Save").performClick()
        assertEquals("test-key-from-ui", searchKeys.value["tavily"])
        compose.onNodeWithText("Active").assertExists()
    }

    @Test
    fun mcpServerEntryValidatesUrlCanBeSavedToggledAndRemovedLocally() {
        val servers = mutableStateListOf<McpServerItem>()

        compose.setContent {
            AriAiTheme(darkTheme = false, dynamicColor = false) {
                SimpleMcpScreen(
                    servers = servers,
                    onToggle = { id ->
                        val index = servers.indexOfFirst { it.id == id }
                        if (index >= 0) servers[index] = servers[index].copy(enabled = !servers[index].enabled)
                    },
                    onAdd = { servers.add(it) },
                    onRemove = { id -> servers.removeAll { it.id == id } },
                    onBack = {}
                )
            }
        }

        compose.onNodeWithText("MCP support is a preview: server URLs are not contacted and tools are not yet used in chats.")
            .assertExists()
        compose.onNodeWithContentDescription("Add server").performClick()
        compose.onNodeWithTag("mcp-server-name").performTextInput("Local MCP")
        compose.onNodeWithTag("mcp-server-url").performTextInput("https://")
        compose.onNodeWithTag("mcp-server-save").assertIsNotEnabled()
        compose.onNodeWithTag("mcp-server-url").performTextClearance()
        compose.onNodeWithTag("mcp-server-url").performTextInput("https://mcp.example.test/mcp")
        compose.onNodeWithText("SSE").performClick()
        compose.onNodeWithTag("mcp-server-save").performScrollTo().assertIsDisplayed().performClick()

        assertEquals(1, servers.size)
        assertEquals("Local MCP", servers.single().name)
        assertTrue(servers.single().enabled)
        compose.onNodeWithText("Enabled locally · not connected").assertExists()
        compose.onNodeWithTag("mcp-toggle-${servers.single().id}").performClick()
        compose.onNodeWithText("Disabled").assertExists()
        compose.onNodeWithContentDescription("Remove server").performClick()
        compose.onNodeWithText("No MCP servers yet").assertExists()
        assertNull(servers.firstOrNull())
    }
}
