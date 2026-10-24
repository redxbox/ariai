package com.ariai.app.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
import com.ariai.app.data.remote.ConnectionResult
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
}
