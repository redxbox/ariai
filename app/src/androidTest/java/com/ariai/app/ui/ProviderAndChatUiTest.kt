package com.ariai.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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

    private inline fun assertDisplayed(description: String, assertion: () -> Unit) {
        try {
            assertion()
        } catch (error: AssertionError) {
            throw AssertionError("$description should be displayed: ${error.message}", error)
        }
    }

    private inline fun clickWithLabel(description: String, click: () -> Unit) {
        try {
            click()
        } catch (error: Throwable) {
            throw AssertionError("$description click failed: ${error.message}", error)
        }
    }

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
            Triple(Color(0xFFF7F6FC), Color(0xFFFCFBFF), Color(0xFF324472)),
            colors.value
        )
    }

    @Test
    fun emptyWelcomeDoesNotDuplicateTheTopModelSelector() {
        val provider = Provider(
            id = "provider-empty-model-selector-test",
            name = "Model selector test provider",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://api.example.test/v1",
            apiKey = "test-key",
            models = listOf(AIModel("selector-model", "Selector model", "provider-empty-model-selector-test"))
        )
        val chat = Chat(
            id = "chat-empty-model-selector-test",
            title = "New chat",
            providerId = provider.id,
            modelId = ""
        )

        compose.setContent {
            AriAiTheme(darkTheme = false) {
                NewChatScreen(
                    chat = chat,
                    messages = emptyList(),
                    isStreaming = false,
                    currentStreamingContent = "",
                    selectedModel = null,
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
        compose.waitForIdle()

        assertDisplayed("top model selector remains available") {
            compose.onNodeWithText("Choose model").assertIsDisplayed()
        }
        assertEquals(
            "The welcome screen should not repeat the top Choose model action",
            1,
            compose.onAllNodesWithText("Choose model").fetchSemanticsNodes().size
        )
    }

    @Test
    fun sendIsDisabledUntilAProviderAndModelAreReady() {
        val chat = Chat(id = "providerless-send-test", title = "New chat")
        var sendCalls = 0

        compose.setContent {
            AriAiTheme(darkTheme = false) {
                NewChatScreen(
                    chat = chat,
                    messages = emptyList(),
                    isStreaming = false,
                    currentStreamingContent = "",
                    selectedModel = null,
                    selectedProviderId = null,
                    providers = emptyList(),
                    onSendMessage = { _, _, _, _, _ -> sendCalls++; true },
                    onBack = {},
                    onBranchMessage = {},
                    onRegenerate = {},
                    onCopyMessage = {},
                    onCheckProvider = { ConnectionResult.Ok(0) }
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText("Add your first AI provider to start chatting").assertIsDisplayed()
        compose.onNodeWithTag("chat-composer").performTextInput("Draft waits for provider")
        compose.onNodeWithTag("chat-send-button").assertIsNotEnabled()
        compose.onNodeWithTag("chat-composer").assertTextEquals("Draft waits for provider")
        assertEquals("a missing provider must not start a chat request", 0, sendCalls)
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
        compose.waitForIdle()
        compose.onNodeWithTag("provider-toggle-${provider.value.id}").assertIsOff()
        assertTrue("provider toggle callback should persist its disabled state", !provider.value.enabled)

        screen.value = "chat"
        compose.waitForIdle()
        compose.onNodeWithText(chat.title).assertExists().performClick()
        compose.waitForIdle()
        assertEquals(chat.id, openedChatId)
    }

    @Test
    fun providerEditorKeepsFloatingActionsReachableAndShowsConnectionDialogResult() {
        var savedProvider: Provider? = null
        var connectionRequests = 0

        compose.setContent {
            AriAiTheme(darkTheme = false) {
                AddProviderScreen(
                    presetType = ProviderType.OPENAI,
                    fetchModels = { listOf(AIModel("mock-model", "Mock model", "provider-editor-test")) },
                    onSave = { savedProvider = it },
                    onBack = {},
                    onCheckProvider = {
                        connectionRequests++
                        ConnectionResult.Ok(models = 1, durationMs = 12)
                    }
                )
            }
        }
        compose.waitForIdle()

        val actionBar = compose.onNodeWithTag("provider-save-action-bar")
            .assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val saveAction = compose.onNodeWithTag("provider-save")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val testAction = compose.onNodeWithTag("provider-test-connection")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val rootBottom = compose.onRoot().fetchSemanticsNode().boundsInRoot.bottom
        assertTrue("provider actions should stay clear of the screen edge", actionBar.bottom < rootBottom)
        assertTrue("Save and Test should be full-size, balanced actions", saveAction.width > actionBar.width * 0.35f && testAction.width > actionBar.width * 0.35f)
        val presetTags = listOf("openai", "gemini", "anthropic", "ollama", "deepseek", "openrouter", "openai_compatible")
        assertTrue(
            "The provider chooser should stay on Quick connect instead of repeating in the form",
            presetTags.all { providerTag ->
                compose.onAllNodesWithTag("provider-preset-$providerTag").fetchSemanticsNodes().isEmpty()
            }
        )
        compose.onNodeWithText("https://api.openai.com/v1").assertIsDisplayed()

        compose.onNodeWithTag("provider-test-connection").assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Connected", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("provider-connection-dialog").assertIsDisplayed()
        val resultCard = compose.onNodeWithTag("provider-connection-result")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("the complete connection result should remain visible", resultCard.height >= 60f)
        compose.onNodeWithText("Connected", substring = true).assertIsDisplayed()
        compose.onNodeWithText("12 ms").assertIsDisplayed()
        assertEquals(1, connectionRequests)
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

        compose.onNodeWithTag("chat-thinking-chip", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Thinking depth").assertIsDisplayed()
        compose.onNodeWithText("Not every model supports this", substring = true).assertIsDisplayed()
    }

    @Test
    fun welcomeSuggestionsStayAboveTheComposerSendButton() {
        val provider = Provider(
            id = "provider-welcome-layout-test",
            name = "Welcome layout provider",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://api.example.test/v1",
            apiKey = "test-key",
            models = listOf(AIModel("mock-model", "Mock model", "provider-welcome-layout-test"))
        )
        val chat = Chat(
            id = "chat-welcome-layout-test",
            title = "New chat",
            providerId = provider.id,
            modelId = "mock-model"
        )

        compose.setContent {
            AriAiTheme(darkTheme = false) {
                NewChatScreen(
                    chat = chat,
                    messages = emptyList(),
                    isStreaming = false,
                    currentStreamingContent = "",
                    selectedModel = "mock-model",
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
            models = listOf(
                AIModel("popular-three", "Most used three", "provider-model-picker-test"),
                AIModel("popular-one", "Most used one", "provider-model-picker-test"),
                AIModel("popular-two", "Most used two", "provider-model-picker-test"),
                AIModel("long-tail", "Long tail model", "provider-model-picker-test"),
                AIModel("gpt-5.5-pro", "GPT-5.5 Pro", "provider-model-picker-test")
            )
        )
        val alternateProvider = Provider(
            id = "provider-model-picker-alternate",
            name = "Alternate provider",
            type = ProviderType.GEMINI,
            baseUrl = "https://api.example.test/v1",
            apiKey = "test-key",
            models = listOf(AIModel("alternate-model", "Alternative model", "provider-model-picker-alternate"))
        )
        var addCalls = 0
        var manageCalls = 0

        val modelUsageCounts = mapOf(
            (provider.id to "popular-one") to 12,
            (provider.id to "popular-two") to 8,
            (provider.id to "popular-three") to 4,
            (provider.id to "gpt-5.5-pro") to 15
        )
        val chat = Chat(
            id = "chat-model-picker-test",
            title = "Model picker test",
            providerId = provider.id,
            modelId = "popular-one"
        )

        compose.setContent {
            AriAiTheme(darkTheme = false) {
                NewChatScreen(
                    chat = chat,
                    messages = emptyList(),
                    isStreaming = false,
                    currentStreamingContent = "",
                    selectedModel = "popular-one",
                    selectedProviderId = provider.id,
                    providers = listOf(provider, alternateProvider),
                    modelUsageCounts = modelUsageCounts,
                    onSendMessage = { _, _, _, _, _ -> false },
                    onBack = {},
                    onBranchMessage = {},
                    onRegenerate = {},
                    onCopyMessage = {},
                    onAddProvider = { addCalls++ },
                    onManageProviders = { manageCalls++ },
                    onCheckProvider = { ConnectionResult.Ok(0) }
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("chat-model-floater").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("model-picker-overflow").assertExists()

        compose.onNodeWithTag("model-picker-provider-dock", useUnmergedTree = true).assertExists()
        assertDisplayed("all-models provider filter") {
            compose.onNodeWithTag("provider-filter-all").assertIsDisplayed()
        }
        assertDisplayed("saved-provider filter") {
            compose.onNodeWithTag("provider-filter-${provider.id}").assertIsDisplayed()
        }
        compose.onNodeWithTag("provider-model-count-all", useUnmergedTree = true).assertTextContains("6")
        compose.onNodeWithTag("model-picker-selected-count", useUnmergedTree = true).assertTextContains("5")
        if (compose.onAllNodesWithTag("model-picker-provider-rail").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("model-picker-provider-rail")
                .performScrollToNode(hasTestTag("provider-filter-${alternateProvider.id}"))
        }
        assertDisplayed("alternate provider chip") {
            compose.onNodeWithTag("provider-filter-${alternateProvider.id}").assertIsDisplayed()
        }
        val chatGptGroup = compose.onNodeWithTag("model-picker-group-chatgpt")
        assertDisplayed("ChatGPT group starts collapsed") { chatGptGroup.assertIsDisplayed() }
        val chatGptHeaderBounds = chatGptGroup.fetchSemanticsNode().boundsInRoot
        val chatGptTitleBounds = compose.onNodeWithTag(
            "model-picker-group-chatgpt-title",
            useUnmergedTree = true
        ).fetchSemanticsNode().boundsInRoot
        val chatGptArrowBounds = compose.onNodeWithTag(
            "model-picker-group-chatgpt-expand-arrow",
            useUnmergedTree = true
        ).fetchSemanticsNode().boundsInRoot
        val otherGroupBounds = compose.onNodeWithTag("model-picker-group-other-models")
            .fetchSemanticsNode().boundsInRoot
        val detailsBounds = compose.onNodeWithTag("model-picker-details-pane", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val arrowGap = chatGptArrowBounds.left - chatGptTitleBounds.right
        assertTrue(
            "Expand arrow should sit beside the group label rather than at the far edge",
            arrowGap in 0f..(chatGptHeaderBounds.width * 0.18f)
        )
        assertTrue(
            "Compact model groups should use both columns instead of leaving a blank half-row",
            chatGptHeaderBounds.width < detailsBounds.width * 0.65f &&
                kotlin.math.abs(chatGptHeaderBounds.top - otherGroupBounds.top) < chatGptHeaderBounds.height * 0.15f
        )
        assertTrue(compose.onAllNodesWithText("GPT-5.5 Pro").fetchSemanticsNodes().isEmpty())
        clickWithLabel("open ChatGPT group") { chatGptGroup.performClick() }
        val chatGptPopup = compose.onNodeWithTag("model-picker-group-popup-chatgpt")
        assertDisplayed("ChatGPT models open in a floating popup") { chatGptPopup.assertIsDisplayed() }
        assertDisplayed("ChatGPT model inside the floating popup") {
            compose.onNodeWithText("GPT-5.5 Pro").assertIsDisplayed()
        }
        clickWithLabel("collapse ChatGPT group") { chatGptGroup.performClick() }

        val modelGroup = compose.onNodeWithTag("model-picker-group-other-models")
        assertDisplayed("model group is initially collapsed") { modelGroup.assertIsDisplayed() }
        assertTrue(compose.onAllNodesWithText("Most used one").fetchSemanticsNodes().isEmpty())
        clickWithLabel("open model group") { modelGroup.performClick() }
        val modelPopup = compose.onNodeWithTag("model-picker-group-popup-other-models")
        assertDisplayed("models open in a floating popup") { modelPopup.assertIsDisplayed() }
        val firstPopular = compose.onNodeWithText("Most used one")
        val secondPopular = compose.onNodeWithText("Most used two")
        val thirdPopular = compose.onNodeWithText("Most used three")
        assertDisplayed("most-used model one") { firstPopular.assertIsDisplayed() }
        assertDisplayed("most-used model two") { secondPopular.assertIsDisplayed() }
        assertDisplayed("most-used model three") { thirdPopular.assertIsDisplayed() }
        val popupBounds = modelPopup.fetchSemanticsNode().boundsInRoot
        val firstPopularBounds = firstPopular.fetchSemanticsNode().boundsInRoot
        val secondPopularBounds = secondPopular.fetchSemanticsNode().boundsInRoot
        val thirdPopularBounds = thirdPopular.fetchSemanticsNode().boundsInRoot
        assertTrue(firstPopularBounds.top < secondPopularBounds.top && secondPopularBounds.top < thirdPopularBounds.top)
        assertTrue(firstPopularBounds.top >= popupBounds.top && thirdPopularBounds.bottom <= popupBounds.bottom)
        assertTrue(compose.onAllNodesWithText("Long tail model").fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodesWithText("Alternative model").fetchSemanticsNodes().isEmpty())
        clickWithLabel("show-all models button") {
            compose.onNodeWithTag("model-picker-expand-${provider.id}").performClick()
        }
        assertDisplayed("expanded long-tail model") { compose.onNodeWithText("Long tail model").assertIsDisplayed() }
        clickWithLabel("alternate provider filter") {
            compose.onNodeWithTag("provider-filter-${alternateProvider.id}").performClick()
        }
        compose.waitForIdle()
        val alternateModelGroup = compose.onNodeWithTag("model-picker-group-other-models")
        assertDisplayed("alternate provider group") { alternateModelGroup.assertIsDisplayed() }
        clickWithLabel("open alternate provider group") { alternateModelGroup.performClick() }
        assertDisplayed("alternate provider group opens a floating popup") {
            compose.onNodeWithTag("model-picker-group-popup-other-models").assertIsDisplayed()
        }
        assertDisplayed("alternate provider model") { compose.onNodeWithText("Alternative model").assertIsDisplayed() }
        assertTrue(compose.onAllNodesWithText("Long tail model").fetchSemanticsNodes().isEmpty())
        clickWithLabel("close alternate provider model popup") { alternateModelGroup.performClick() }

        clickWithLabel("model picker overflow menu") {
            compose.onNodeWithTag("model-picker-overflow").performClick()
        }
        val addProviderButton = compose.onNodeWithTag("model-picker-add-menu")
        assertDisplayed("add provider menu action") { addProviderButton.assertIsDisplayed() }
        clickWithLabel("add provider menu action") { addProviderButton.performClick() }
        compose.waitForIdle()
        compose.onNodeWithTag("chat-model-floater").performClick()
        compose.waitForIdle()

        clickWithLabel("model picker overflow menu after add") {
            compose.onNodeWithTag("model-picker-overflow").performClick()
        }
        val manageProvidersButton = compose.onNodeWithTag("model-picker-manage-menu")
        assertDisplayed("manage providers menu action") { manageProvidersButton.assertIsDisplayed() }
        clickWithLabel("manage providers menu action") { manageProvidersButton.performClick() }
        assertEquals(1, addCalls)
        assertEquals(1, manageCalls)
    }

    @Test
    fun compactTopModelSelectorStaysOutOfChatAndOpensProviderDock() {
        val responseStreaming = mutableStateOf(false)
        val provider = Provider(
            id = "provider-floater-test",
            name = "Pebble provider",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://api.example.test/v1",
            apiKey = "test-key",
            models = listOf(
                AIModel("model-one", "Model one", "provider-floater-test"),
                AIModel("model-two", "Model two", "provider-floater-test"),
                AIModel("model-three", "Model three", "provider-floater-test"),
                AIModel("model-hidden", "Hidden model", "provider-floater-test")
            )
        )
        val chat = Chat(
            id = "chat-floater-test",
            title = "Floating selector",
            providerId = provider.id,
            modelId = "model-one"
        )

        compose.setContent {
            AriAiTheme(darkTheme = false) {
                NewChatScreen(
                    chat = chat,
                    messages = emptyList(),
                    isStreaming = responseStreaming.value,
                    currentStreamingContent = if (responseStreaming.value) "Streaming response preview" else "",
                    selectedModel = "model-one",
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
        compose.waitForIdle()

        assertDisplayed("chat title stays in the top bar") {
            compose.onNodeWithTag("chat-title-header").assertIsDisplayed()
        }
        assertDisplayed("selected model name in the compact dropdown") {
            compose.onNodeWithText("AriAI (Model one)", useUnmergedTree = true).assertIsDisplayed()
        }
        val modelFloater = compose.onNodeWithTag("chat-model-floater")
        val selectorRowBounds = compose.onNodeWithTag("chat-model-selector-container")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val modelBounds = modelFloater.assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val screenBounds = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val composerBounds = compose.onNodeWithTag("chat-composer-container")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val sendBounds = compose.onNodeWithTag("chat-send-button")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val rootCenterX = (screenBounds.left + screenBounds.right) / 2f
        val selectorCenterX = (modelBounds.left + modelBounds.right) / 2f
        val selectorCenterY = (modelBounds.top + modelBounds.bottom) / 2f
        val selectorRowCenterY = (selectorRowBounds.top + selectorRowBounds.bottom) / 2f
        assertTrue(
            "Selector should remain a small centered pill below the top bar: selector=$modelBounds row=$selectorRowBounds root=$screenBounds",
            modelBounds.width < screenBounds.width * 0.72f &&
                modelBounds.height < screenBounds.height * 0.08f &&
                kotlin.math.abs(selectorCenterX - rootCenterX) < screenBounds.width * 0.03f &&
                kotlin.math.abs(selectorCenterY - selectorRowCenterY) < screenBounds.height * 0.015f
        )
        assertTrue(
            "Top selector must not cover the composer or Send button: selector=$modelBounds composer=$composerBounds send=$sendBounds",
            modelBounds.bottom < composerBounds.top &&
                sendBounds.left >= composerBounds.left && sendBounds.right <= composerBounds.right &&
                sendBounds.top >= composerBounds.top && sendBounds.bottom <= composerBounds.bottom
        )

        responseStreaming.value = true
        compose.waitForIdle()
        val streamingBounds = modelFloater.fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Selector should stay pinned while the assistant is responding: before=$modelBounds after=$streamingBounds",
            kotlin.math.abs(streamingBounds.left - modelBounds.left) < 1f &&
                kotlin.math.abs(streamingBounds.top - modelBounds.top) < 1f
        )
        responseStreaming.value = false
        compose.waitForIdle()
        clickWithLabel("chat model floater") { modelFloater.performClick() }
        compose.waitForIdle()
        compose.onNodeWithTag("model-picker-overflow").assertExists()
        compose.onNodeWithTag("model-picker-provider-dock", useUnmergedTree = true).assertExists()
        assertDisplayed("all-models provider filter opened from chat") {
            compose.onNodeWithTag("provider-filter-all").assertIsDisplayed()
        }
        assertDisplayed("configured provider filter opened from chat") {
            compose.onNodeWithTag("provider-filter-${provider.id}").assertIsDisplayed()
        }
        val selectedModelGroup = compose.onNodeWithTag("model-picker-group-other-models")
        assertDisplayed("selected model family") { selectedModelGroup.assertIsDisplayed() }
        clickWithLabel("open selected model family") { selectedModelGroup.performClick() }
        compose.waitForIdle()
        assertDisplayed("selected model family popup") {
            compose.onNodeWithTag("model-picker-group-popup-other-models").assertIsDisplayed()
        }
        val rowTag = "model-row-${provider.id}-model-one"
        val selectedModelRow = compose.onNodeWithTag(rowTag, useUnmergedTree = true)
        val rowBounds = runCatching { selectedModelRow.fetchSemanticsNode().boundsInRoot }.getOrNull()
        val modelListBounds = runCatching {
            compose.onNodeWithTag("model-picker-model-list", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        }.getOrNull()
        val rootBounds = runCatching { compose.onRoot().fetchSemanticsNode().boundsInRoot }.getOrNull()
        val dockBounds = runCatching {
            compose.onNodeWithTag("model-picker-provider-dock", useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot
        }.getOrNull()
        val rowCount = compose.onAllNodesWithTag(rowTag, useUnmergedTree = true).fetchSemanticsNodes().size
        val modelTextCount = compose.onAllNodesWithText("Model one", useUnmergedTree = true).fetchSemanticsNodes().size
        val expandButtonCount = compose.onAllNodesWithTag(
            "model-picker-expand-${provider.id}",
            useUnmergedTree = true
        ).fetchSemanticsNodes().size
        val pickerDiagnostics =
            "selected model picker row=$rowBounds list=$modelListBounds dock=$dockBounds root=$rootBounds " +
                "rowCount=$rowCount modelTextCount=$modelTextCount expandButtonCount=$expandButtonCount"
        assertTrue("Picker should compose its selected model row: $pickerDiagnostics", rowCount > 0)
        assertDisplayed(pickerDiagnostics) {
            selectedModelRow.assertIsDisplayed()
        }
        assertTrue(compose.onAllNodesWithText("Hidden model").fetchSemanticsNodes().isEmpty())
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
