package com.ariai.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ariai.app.data.models.AIModel
import com.ariai.app.data.models.Chat
import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import com.ariai.app.data.models.MessageStatus
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import com.ariai.app.data.models.TokenUsage
import com.ariai.app.data.remote.ConnectionResult
import com.ariai.app.ui.screens.AddProviderScreen
import com.ariai.app.ui.screens.ChatListScreen
import com.ariai.app.ui.screens.NewChatScreen
import com.ariai.app.ui.screens.ModelPickerSheet
import com.ariai.app.ui.screens.NewProvidersScreen
import com.ariai.app.ui.theme.AriAiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProviderAndChatUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lightThemeUsesLavenderPaperWhiteCardsAndSlateBlueAccent() {
        val colors = mutableStateOf<Triple<Color, Color, Color>?>(null)
        compose.setContent {
            AriAiTheme(darkTheme = false) {
                val colorScheme = MaterialTheme.colorScheme
                SideEffect {
                    colors.value = Triple(
                        colorScheme.background,
                        colorScheme.surface,
                        colorScheme.primary
                    )
                }
            }
        }
        compose.waitForIdle()

        assertEquals(
            Triple(Color(0xFFF3F2F8), Color(0xFFF8F7FD), Color(0xFF4D6090)),
            colors.value
        )
    }

    @Test
    fun providerCanBeDisabledAndChatCanBeOpenedWithoutNetwork() {
        val provider = mutableStateOf(
            Provider(
                id = "provider-test-1",
                name = "Mock provider",
                type = ProviderType.OPENAI_COMPATIBLE,
                baseUrl = "https://api.example.test/v1",
                apiKey = "test-key",
                models = listOf(AIModel("mock-model", "Mock model", "provider-test-1"))
            )
        )
        val screen = mutableStateOf("providers")
        val chat = Chat(
            id = "chat-test-1",
            title = "Deterministic chat",
            providerId = provider.value.id,
            modelId = "mock-model"
        )
        var openedChatId: String? = null

        compose.setContent {
            AriAiTheme(darkTheme = false) {
                if (screen.value == "providers") {
                    NewProvidersScreen(
                        providers = listOf(provider.value),
                        onAddProvider = {},
                        onAddPreset = {},
                        onEditProvider = {},
                        onImportProvider = {},
                        onBack = {},
                        onToggleProvider = { current -> provider.value = current.copy(enabled = !current.enabled) }
                    )
                } else {
                    ChatListScreen(
                        chats = listOf(chat),
                        onChatClick = { openedChatId = it.id },
                        onNewChat = {},
                        onDeleteChat = {},
                        onPinChat = {}
                    )
                }
            }
        }

        compose.onNodeWithText("Mock provider").assertExists()
        compose.onNodeWithTag("provider-toggle-${provider.value.id}").assertIsOn().performClick()
        compose.onNodeWithText("Turned off").assertExists()

        screen.value = "chat"
        compose.waitForIdle()
        compose.onNodeWithText(chat.title).assertExists().performClick()
        compose.waitForIdle()
        assertEquals(chat.id, openedChatId)
    }

    @Test
    fun providerEditorKeepsFloatingActionsReachableAndShowsConnectionDialogResult() {
        var savedProvider: Provider? = null

        compose.setContent {
            AriAiTheme(darkTheme = false) {
                AddProviderScreen(
                    presetType = ProviderType.OPENAI,
                    fetchModels = { listOf(AIModel("mock-model", "Mock model", "provider-editor-test")) },
                    onSave = { savedProvider = it },
                    onBack = {},
                    onCheckProvider = { ConnectionResult.Ok(models = 1, durationMs = 12) }
                )
            }
        }
        compose.waitForIdle()

        val actionBar = compose.onNodeWithTag("provider-save-action-bar")
            .assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val rootBottom = compose.onRoot().fetchSemanticsNode().boundsInRoot.bottom
        assertTrue("provider actions should stay clear of the screen edge", actionBar.bottom < rootBottom)

        compose.onNodeWithTag("provider-test-connection").assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Connected", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("provider-connection-dialog").assertIsDisplayed()
        compose.onNodeWithText("Connected", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        assertTrue(
            compose.onAllNodesWithTag("provider-connection-dialog").fetchSemanticsNodes().isEmpty()
        )

        compose.onNodeWithTag("provider-save").assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 5_000) { savedProvider != null }
        assertEquals("OpenAI", savedProvider?.name)
    }

    @Test
    fun thinkingChipAndFirstMessageAreVisibleAndPersianComposerTextRemainsVisible() {
        val chat = Chat(id = "chat-layout-test", title = "New chat", modelId = "mock-model")
        val errorText = "Could not save your message. Please try again."
        val errorMessage = ChatMessage(
            chatId = chat.id,
            role = MessageRole.ASSISTANT,
            content = errorText,
            status = MessageStatus.ERROR
        )

        compose.setContent {
            AriAiTheme(darkTheme = false) {
                NewChatScreen(
                    chat = chat,
                    messages = listOf(errorMessage),
                    isStreaming = false,
                    currentStreamingContent = "",
                    selectedModel = "mock-model",
                    onSendMessage = { _, _, _, _, _ -> false },
                    onBack = {},
                    onBranchMessage = {},
                    onRegenerate = {},
                    onCopyMessage = {},
                    onCheckProvider = { ConnectionResult.Ok(0) }
                )
            }
        }
        compose.waitForIdle()

        val chip = compose.onNodeWithTag("chat-thinking-chip", useUnmergedTree = true)
            .assertIsDisplayed().fetchSemanticsNode()
        val firstMessage = compose.onNodeWithText(errorText, useUnmergedTree = true)
            .assertIsDisplayed().fetchSemanticsNode()
        assertTrue("reasoning chip must leave space before the first message", chip.boundsInRoot.bottom < firstMessage.boundsInRoot.top)

        compose.onNodeWithTag("chat-composer").performClick().performTextInput("سلام")
        compose.onNodeWithTag("chat-composer").assertTextEquals("سلام")
    }

    @Test
    fun welcomeSuggestionsStayAboveTheComposerSendButton() {
        val chat = Chat(id = "chat-welcome-layout-test", title = "New chat", modelId = "mock-model")

        compose.setContent {
            AriAiTheme(darkTheme = false) {
                NewChatScreen(
                    chat = chat,
                    messages = emptyList(),
                    isStreaming = false,
                    currentStreamingContent = "",
                    selectedModel = "mock-model",
                    onSendMessage = { _, _, _, _, _ -> false },
                    onBack = {},
                    onBranchMessage = {},
                    onRegenerate = {},
                    onCopyMessage = {},
                    onCheckProvider = { ConnectionResult.Ok(0) }
                )
            }
        }
        compose.waitForIdle()

        val suggestions = compose.onNodeWithTag("welcome-suggestions")
            .assertIsDisplayed().fetchSemanticsNode()
        val sendButton = compose.onNodeWithTag("chat-send-button")
            .assertIsDisplayed().fetchSemanticsNode()
        assertTrue(
            "welcome suggestions must stay clear of the bottom send button",
            suggestions.boundsInRoot.bottom < sendButton.boundsInRoot.top
        )
    }

    @Test
    fun modelPickerKeepsAddAndManageProviderActionsAfterAProviderExists() {
        val provider = Provider(
            id = "provider-model-picker-test",
            name = "Saved provider",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://api.example.test/v1",
            apiKey = "test-key",
            models = listOf(AIModel("saved-model", "Saved model", "provider-model-picker-test"))
        )
        var addCalls = 0
        var manageCalls = 0

        compose.setContent {
            AriAiTheme(darkTheme = false) {
                ModelPickerSheet(
                    providers = listOf(provider),
                    selectedProviderId = provider.id,
                    selectedModelId = "saved-model",
                    favorites = emptySet(),
                    onToggleFavorite = { _, _ -> },
                    onSelect = { _, _ -> },
                    onAddProvider = { addCalls++ },
                    onManageProviders = { manageCalls++ },
                    onCheckProvider = { ConnectionResult.Ok(0) },
                    onDismiss = {}
                )
            }
        }

        compose.onNodeWithText("Add provider").assertIsDisplayed().performClick()
        compose.onNodeWithText("Manage providers").assertIsDisplayed().performClick()
        assertEquals(1, addCalls)
        assertEquals(1, manageCalls)
    }

    @Test
    fun contextGaugeDistinguishesProviderReportedTokensFromAnEstimate() {
        val provider = Provider(
            id = "provider-token-gauge",
            name = "Token provider",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://api.example.test/v1",
            apiKey = "test-key",
            models = listOf(AIModel("token-model", "Token model", "provider-token-gauge", contextWindow = 4096))
        )
        val chat = Chat(
            id = "chat-token-gauge",
            title = "Token gauge",
            providerId = provider.id,
            modelId = "token-model"
        )
        val messages = mutableStateOf(
            listOf(
                ChatMessage(id = "token-user", chatId = chat.id, role = MessageRole.USER, content = "question"),
                ChatMessage(
                    id = "token-assistant",
                    chatId = chat.id,
                    role = MessageRole.ASSISTANT,
                    content = "answer",
                    usage = TokenUsage(promptTokens = 123, completionTokens = 45, totalTokens = 168)
                )
            )
        )

        compose.setContent {
            AriAiTheme(darkTheme = false, dynamicColor = false) {
                NewChatScreen(
                    chat = chat,
                    messages = messages.value,
                    isStreaming = false,
                    currentStreamingContent = "",
                    selectedModel = "token-model",
                    selectedProviderId = provider.id,
                    providers = listOf(provider),
                    onSendMessage = { _, _, _, _, _ -> false },
                    onBack = {},
                    onBranchMessage = {},
                    onRegenerate = {},
                    onCopyMessage = {},
                    onCheckProvider = { ConnectionResult.Ok(0) }
                )
            }
        }

        compose.onNodeWithTag("chat-context-usage").assertIsDisplayed()
        compose.onNodeWithText("3%", useUnmergedTree = true).assertExists()

        messages.value = messages.value.map { message ->
            if (message.role == MessageRole.ASSISTANT) message.copy(usage = null) else message
        }
        compose.waitForIdle()
        compose.onNodeWithText("~0%", useUnmergedTree = true).assertExists()
    }
}
