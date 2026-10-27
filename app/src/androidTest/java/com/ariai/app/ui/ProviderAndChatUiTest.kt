package com.ariai.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
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

    private inline fun assertDisplayed(description: String, assertion: () -> Unit) {
        try {
            assertion()
        } catch (error: AssertionError) {
            throw AssertionError("$description should be displayed", error)
        }
    }

    private inline fun clickWithLabel(description: String, click: () -> Unit) {
        try {
            click()
        } catch (error: Throwable) {
            throw AssertionError("$description click failed", error)
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
            Triple(Color(0xFFF7F6FC), Color(0xFFFCFBFF), Color(0xFF4D6090)),
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
        val presetTags = listOf("openai", "gemini", "anthropic", "ollama", "openai_compatible")
        val presetHeights = presetTags.map { providerTag ->
            val preset = compose.onNodeWithTag("provider-preset-$providerTag")
            assertDisplayed("$providerTag branded provider preset") { preset.assertIsDisplayed() }
            preset.fetchSemanticsNode().boundsInRoot.height
        }
        val presetHeight = presetHeights.first()
        assertTrue(
            "Provider tiles should keep a crisp, fixed height instead of resizing with selection: $presetHeights",
            presetHeights.all { kotlin.math.abs(it - presetHeight) < 1f }
        )
        compose.onNodeWithTag("provider-preset-openai").assertIsSelected()
        clickWithLabel("Gemini provider preset") {
            compose.onNodeWithTag("provider-preset-gemini").performClick()
        }
        compose.onNodeWithTag("provider-preset-gemini").assertIsSelected()
        assertDisplayed("Gemini base URL after preset selection") {
            compose.onNodeWithText("https://generativelanguage.googleapis.com/v1beta").assertIsDisplayed()
        }
        clickWithLabel("OpenAI provider preset") {
            compose.onNodeWithTag("provider-preset-openai").performClick()
        }

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
            models = listOf(
                AIModel("popular-three", "Most used three", "provider-model-picker-test"),
                AIModel("popular-one", "Most used one", "provider-model-picker-test"),
                AIModel("popular-two", "Most used two", "provider-model-picker-test"),
                AIModel("long-tail", "Long tail model", "provider-model-picker-test")
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

        compose.setContent {
            AriAiTheme(darkTheme = false) {
                ModelPickerSheet(
                    providers = listOf(provider, alternateProvider),
                    selectedProviderId = provider.id,
                    selectedModelId = "popular-one",
                    favorites = emptySet(),
                    modelUsageCounts = mapOf(
                        (provider.id to "popular-one") to 12,
                        (provider.id to "popular-two") to 8,
                        (provider.id to "popular-three") to 4
                    ),
                    onToggleFavorite = { _, _ -> },
                    onSelect = { _, _ -> },
                    onAddProvider = { addCalls++ },
                    onManageProviders = { manageCalls++ },
                    onCheckProvider = { ConnectionResult.Ok(0) },
                    onDismiss = {}
                )
            }
        }
        compose.waitForIdle()

        assertDisplayed("provider dock") {
            compose.onNodeWithTag("model-picker-provider-dock").assertIsDisplayed()
        }
        assertDisplayed("alternate provider chip") {
            compose.onNodeWithTag("provider-filter-${alternateProvider.id}").assertIsDisplayed()
        }
        val firstPopular = compose.onNodeWithText("Most used one")
        val secondPopular = compose.onNodeWithText("Most used two")
        val thirdPopular = compose.onNodeWithText("Most used three")
        assertDisplayed("most-used model one") { firstPopular.assertIsDisplayed() }
        assertDisplayed("most-used model two") { secondPopular.assertIsDisplayed() }
        assertDisplayed("most-used model three") { thirdPopular.assertIsDisplayed() }
        val firstPopularTop = firstPopular.fetchSemanticsNode().boundsInRoot.top
        val secondPopularTop = secondPopular.fetchSemanticsNode().boundsInRoot.top
        val thirdPopularTop = thirdPopular.fetchSemanticsNode().boundsInRoot.top
        assertTrue(firstPopularTop < secondPopularTop && secondPopularTop < thirdPopularTop)
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
        assertDisplayed("alternate provider model") { compose.onNodeWithText("Alternative model").assertIsDisplayed() }
        assertTrue(compose.onAllNodesWithText("Long tail model").fetchSemanticsNodes().isEmpty())

        val addProviderButton = compose.onNodeWithText("Add provider")
        assertDisplayed("add provider button") { addProviderButton.assertIsDisplayed() }
        clickWithLabel("add provider button") { addProviderButton.performClick() }
        val manageProvidersButton = compose.onNodeWithText("Manage providers")
        assertDisplayed("manage providers button") { manageProvidersButton.assertIsDisplayed() }
        clickWithLabel("manage providers button") { manageProvidersButton.performClick() }
        assertEquals(1, addCalls)
        assertEquals(1, manageCalls)
    }

    @Test
    fun neuralGlassSelectorOpensProviderDockNearTheComposer() {
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
                    isStreaming = false,
                    currentStreamingContent = "",
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
        val modelFloater = compose.onNodeWithTag("chat-model-floater")
        val sendButton = compose.onNodeWithTag("chat-send-button")
        val composerBounds = compose.onNodeWithTag("chat-composer-container")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val modelBounds = modelFloater.assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val sendBounds = sendButton.assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Provider/model selector should be compact within the composer: selector=$modelBounds composer=$composerBounds",
            modelBounds.left >= composerBounds.left && modelBounds.right <= composerBounds.right &&
                modelBounds.width < composerBounds.width * 0.72f
        )
        assertTrue(
            "Send should stay above the model selector inside the composer: send=$sendBounds selector=$modelBounds",
            sendBounds.bottom <= modelBounds.top && sendBounds.left >= composerBounds.left &&
                sendBounds.right <= composerBounds.right
        )
        clickWithLabel("chat model floater") { modelFloater.performClick() }
        compose.waitForIdle()
        assertDisplayed("provider dock opened from chat") {
            compose.onNodeWithTag("model-picker-provider-dock").assertIsDisplayed()
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
