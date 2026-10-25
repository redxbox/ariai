package com.ariai.app

import com.ariai.app.ui.theme.*

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ariai.app.ui.drawer.AppDrawerContent
import com.ariai.app.ui.screens.*
import com.ariai.app.ui.theme.AriAiTheme
import com.ariai.app.util.LocalStrings
import com.ariai.app.data.models.SearchMode
import com.ariai.app.util.UiLang
import com.ariai.app.util.getStringsForLanguage
import com.ariai.app.util.tx
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = application as AriAiApp
            val viewModelFactory = AppViewModelFactory(app.repository, app.preferencesManager, app.workspaceRepository)
            val viewModel: AppViewModel = viewModel(factory = viewModelFactory)

            val language by viewModel.language.collectAsState()
            val theme by viewModel.theme.collectAsState()
            val dynamicColor by viewModel.dynamicColor.collectAsState()
            val strings = getStringsForLanguage(language)

            val isDarkTheme = when (theme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            SideEffect { UiLang.code = language }
            val direction = if (language == "fa" || language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                CompositionLocalProvider(LocalStrings provides strings) {
                    AriAiTheme(darkTheme = isDarkTheme, dynamicColor = dynamicColor) {
                        AppRoot(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(viewModel: AppViewModel) {
    val navController = rememberNavController()
    // Opening the app lands directly on a chat, not on a list.
    val startRoute = remember { "chat/${viewModel.createNewChat()}" }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: startRoute
    val snackbarHostState = remember { SnackbarHostState() }

    val providers by viewModel.providers.collectAsState()
    val chats by viewModel.chats.collectAsState()
    val modelOverrides by viewModel.modelOverrides.collectAsState()
    val chatModes by viewModel.chatModes.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    val generatedMediaBytes by viewModel.generatedMediaBytes.collectAsState()
    val language by viewModel.language.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val agents by viewModel.agents.collectAsState()
    val workspaceFiles by viewModel.workspaceFiles.collectAsState()
    val searchKeys by viewModel.searchKeys.collectAsState()
    val currentChatId by viewModel.currentChatId.collectAsState()
    val mcpServers = remember { mutableStateListOf<McpServerItem>() }
    val defaultProviderId by viewModel.defaultProviderId.collectAsState()
    val defaultModelId by viewModel.defaultModelId.collectAsState()
    val reasoningLevel by viewModel.reasoningLevel.collectAsState()
    val streamResponse by viewModel.streamResponse.collectAsState()
    val searchModeState by viewModel.searchMode.collectAsState()
    val favoriteModels by viewModel.favoriteModels.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val defaultModelName = providers.firstOrNull { it.id == defaultProviderId }?.let { p ->
        p.models.firstOrNull { it.id == defaultModelId }?.let { "${it.displayName} · ${p.name}" }
    } ?: tx("Not chosen")
    val activeChatForDrawer = chats.firstOrNull { it.id == currentChatId }
    val drawerProviderId = activeChatForDrawer?.let { modelOverrides[it.id]?.first ?: it.providerId } ?: defaultProviderId
    val drawerModelId = activeChatForDrawer?.let { modelOverrides[it.id]?.second ?: it.modelId } ?: defaultModelId
    val drawerProvider = providers.firstOrNull { it.id == drawerProviderId && it.enabled }
    val drawerModelName = drawerProvider?.models?.firstOrNull { it.id == drawerModelId }?.displayName

    fun go(route: String) {
        scope.launch { drawerState.close() }
        navController.navigate(route) { launchSingleTop = true }
    }

    fun openChat(chatId: String) {
        if (chatId.isBlank()) {
            com.ariai.app.util.AppLog.warn(
                IllegalArgumentException("Refusing to navigate to a chat with a blank ID"),
                "open chat"
            )
            scope.launch {
                drawerState.close()
                snackbarHostState.showSnackbar(tx("This saved chat cannot be opened because its ID is missing."))
            }
            return
        }
        scope.launch { drawerState.close() }
        viewModel.loadChat(chatId)
        navController.navigate("chat/$chatId") { launchSingleTop = true }
    }

    fun newChat() {
        scope.launch { drawerState.close() }
        val id = viewModel.createNewChat()
        navController.navigate("chat/$id") { launchSingleTop = true }
    }

    fun deleteChatFromUi(chatId: String) {
        val deletingActiveChat = currentChatId == chatId
        viewModel.deleteChat(chatId)
        if (deletingActiveChat) {
            scope.launch { drawerState.close() }
            navController.navigate("chats") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    fun newImageChat() {
        scope.launch { drawerState.close() }
        val (providerId, modelId) = viewModel.firstImageModel() ?: (null to null)
        val id = viewModel.createNewChat(providerId, modelId)
        viewModel.setChatMode(id, ChatMode.IMAGE)
        navController.navigate("chat/$id") { launchSingleTop = true }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = AriPaper,
                drawerContentColor = AriInk,
                modifier = Modifier.width(312.dp)
            ) {
                AppDrawerContent(
                    chats = chats,
                    currentChatId = currentChatId,
                    currentRoute = currentRoute,
                    activeProvider = drawerProvider,
                    activeModelName = drawerModelName,
                    onNewChat = { newChat() },
                    onChatClick = { openChat(it.id) },
                    onPinChat = { viewModel.pinChat(it) },
                    onDeleteChat = { deleteChatFromUi(it.id) },
                    onSetChatFolder = { chat, folder -> viewModel.setChatFolder(chat.id, folder) },
                    onNavigate = { go(it) },
                    onAbout = {
                        scope.launch {
                            drawerState.close()
                            snackbarHostState.showSnackbar(tx("AriAI v1.0 - Personal AI"))
                        }
                    }
                )
            }
        }
    ) {
        // Screens handle their own status/navigation-bar insets, so the outer
        // Scaffold adds none. This removes the double top and bottom margin.
        Scaffold(
            containerColor = AriPaper,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = startRoute,
                modifier = Modifier.padding(padding),
                enterTransition = {
                    fadeIn(tween(220)) + slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { it / 6 }
                },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = {
                    fadeOut(tween(160)) + slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { it / 6 }
                }
            ) {
                composable("chats") {
                    ChatListScreen(
                        chats = chats,
                        onChatClick = { openChat(it.id) },
                        onNewChat = { newChat() },
                        onDeleteChat = { deleteChatFromUi(it.id) },
                        onPinChat = { viewModel.pinChat(it) }
                    )
                }

                composable("agents") {
                    AgentStudioScreen(
                        agents = agents,
                        providers = providers,
                        onBack = { navController.popBackStack() },
                        onSaveAgent = { viewModel.saveAgent(it) },
                        onDeleteAgent = { viewModel.deleteAgent(it) },
                        onStartAgent = { agent ->
                            scope.launch { drawerState.close() }
                            val id = viewModel.createNewChat(agentId = agent.id)
                            navController.navigate("chat/$id") { launchSingleTop = true }
                        }
                    )
                }

                composable("workspace") {
                    WorkspaceScreen(
                        files = workspaceFiles,
                        onBack = { navController.popBackStack() },
                        onCreateFile = { name, content, onResult -> viewModel.createWorkspaceFile(name, content, onResult) },
                        onSaveFile = { name, content, onResult -> viewModel.saveWorkspaceFile(name, content, onResult) },
                        onDeleteFile = { name, onResult -> viewModel.deleteWorkspaceFile(name, onResult) }
                    )
                }

                composable(
                    "chat/{chatId}",
                    arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                ) { entry ->
                    val chatId = entry.arguments?.getString("chatId") ?: ""
                    val currentChat = chats.find { it.id == chatId }
                    LaunchedEffect(chatId) { viewModel.loadChat(chatId) }
                    NewChatScreen(
                        chat = currentChat,
                        screenChatId = chatId,
                        messages = messages,
                        isStreaming = isStreaming,
                        isSending = isSending,
                        currentStreamingContent = streamingContent,
                        selectedModel = modelOverrides[chatId]?.second ?: currentChat?.modelId,
                        selectedProviderId = modelOverrides[chatId]?.first ?: currentChat?.providerId,
                        providers = providers,
                        reasoning = reasoningLevel,
                        searchMode = searchModeState,
                        favorites = favoriteModels,
                        localSearchConfigured = searchKeys.values.any { it.isNotBlank() },
                        onSendMessage = { text, files, level, mode, onSaved ->
                            viewModel.sendMessage(text, files, level, mode, onSaved)
                        },
                        onStopGeneration = { viewModel.stopGeneration() },
                        onChatScreenDisposed = { viewModel.onChatScreenDisposed(it) },
                        onBack = { newChat() },
                        onRenameChat = { viewModel.renameChat(chatId, it) },
                        onBranchMessage = {
                            val branchId = viewModel.branchMessage(it)
                            navController.navigate("chat/$branchId") { launchSingleTop = true }
                        },
                        onRegenerate = { message ->
                            val retryChatId = viewModel.regenerateMessage(message, reasoningLevel, searchModeState)
                            if (retryChatId != null && retryChatId != chatId) {
                                navController.navigate("chat/$retryChatId") { launchSingleTop = true }
                            }
                        },
                        onCopyMessage = { },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onSelectModel = { pid, mid -> viewModel.updateChatProvider(chatId, pid, mid) },
                        onEditImage = { message, instruction -> viewModel.editGeneratedImage(message, instruction) },
                        onToggleFavorite = { pid, mid -> viewModel.toggleFavoriteModel(pid, mid) },
                        onReasoningChange = { viewModel.setReasoningLevel(it) },
                        onSearchModeChange = { viewModel.setSearchMode(it) },
                        onCompressHistory = { done -> viewModel.compressHistory(done) },
                        onNewImageChat = { newImageChat() },
                        chatMode = chatModes[chatId] ?: ChatMode.TEXT,
                        onChatModeChange = { viewModel.setChatMode(chatId, it) },
                        onOpenExtensions = { navController.navigate("mcp") },
                        onOpenSearchSettings = { navController.navigate("search_service") },
                        fontSize = fontSize,
                        onAddProvider = { navController.navigate("add_provider") },
                        onManageProviders = { navController.navigate("providers") },
                        onCheckProvider = { provider -> com.ariai.app.data.remote.checkConnection(provider) { viewModel.fetchModels(it) } },
                        onUpdateChatOptions = { temperature, topP, systemPrompt ->
                            viewModel.updateChatOptions(chatId, temperature, topP, systemPrompt)
                        }
                    )
                }

                composable("providers") {
                    NewProvidersScreen(
                        providers = providers,
                        onAddProvider = { navController.navigate("add_provider") },
                        onAddPreset = { navController.navigate("add_provider?preset=${it.name}") },
                        onImportProvider = { provider ->
                            viewModel.saveProvider(provider) { saved ->
                                if (!saved) scope.launch { snackbarHostState.showSnackbar(tx("Could not save provider. Please try again.")) }
                            }
                        },
                        onEditProvider = { navController.navigate("edit_provider/${it.id}") },
                        onBack = { navController.popBackStack() },
                        onToggleProvider = { provider ->
                            viewModel.saveProvider(provider.copy(enabled = !provider.enabled)) { saved ->
                                if (!saved) scope.launch { snackbarHostState.showSnackbar(tx("Could not save provider. Please try again.")) }
                            }
                        }
                    )
                }

                composable(
                    "add_provider?preset={preset}",
                    arguments = listOf(navArgument("preset") { type = NavType.StringType; defaultValue = "" })
                ) { entry ->
                    AddProviderScreen(
                        presetType = entry.arguments?.getString("preset")
                            ?.let { runCatching { com.ariai.app.data.models.ProviderType.valueOf(it) }.getOrNull() },
                        existingProviderNames = providers.map { it.name }.toSet(),
                        fetchModels = { viewModel.fetchModels(it) },
                        onSave = { provider ->
                            viewModel.saveProvider(provider) { saved ->
                                if (saved) navController.popBackStack()
                                else scope.launch { snackbarHostState.showSnackbar(tx("Could not save provider. Please try again.")) }
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    "edit_provider/{providerId}",
                    arguments = listOf(navArgument("providerId") { type = NavType.StringType })
                ) { entry ->
                    val providerId = entry.arguments?.getString("providerId") ?: ""
                    AddProviderScreen(
                        initialProvider = providers.find { it.id == providerId },
                        existingProviderNames = providers.map { it.name }.toSet(),
                        fetchModels = { viewModel.fetchModels(it) },
                        onSave = { provider ->
                            viewModel.saveProvider(provider) { saved ->
                                if (saved) navController.popBackStack()
                                else scope.launch { snackbarHostState.showSnackbar(tx("Could not save provider. Please try again.")) }
                            }
                        },
                        onBack = { navController.popBackStack() },
                        onDelete = { id -> viewModel.deleteProvider(id); navController.popBackStack() }
                    )
                }

                composable("settings") {
                    SimpleSettingsScreen(
                        onBack = { navController.popBackStack() },
                        onProvidersClick = { navController.navigate("providers") },
                        onSearchServiceClick = { navController.navigate("search_service") },
                        onStorageClick = { navController.navigate("storage") },
                        onMcpClick = { navController.navigate("mcp") },
                        onClearAll = { navController.navigate("storage") },
                        currentTheme = theme,
                        currentLanguage = language,
                        dynamicColor = dynamicColor,
                        onDynamicColorChange = { viewModel.setDynamicColor(it) },
                        onThemeChange = { viewModel.setTheme(it) },
                        onLanguageChange = { viewModel.setLanguage(it) },
                        onDefaultModelClick = { navController.navigate("default_model") },
                        onPreferencesClick = { navController.navigate("preferences") },
                        defaultModelName = defaultModelName
                    )
                }

                composable("preferences") {
                    PreferencesScreen(
                        theme = theme,
                        onThemeChange = { viewModel.setTheme(it) },
                        dynamicColor = dynamicColor,
                        onDynamicColorChange = { viewModel.setDynamicColor(it) },
                        reasoning = reasoningLevel,
                        onReasoningChange = { viewModel.setReasoningLevel(it) },
                        fontSize = fontSize,
                        onFontSizeChange = { viewModel.setFontSize(it) },
                        streamResponse = streamResponse,
                        onStreamResponseChange = { viewModel.setStreamResponse(it) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("default_model") {
                    DefaultModelScreen(
                        providers = providers,
                        defaultProviderId = defaultProviderId,
                        defaultModelId = defaultModelId,
                        onSelect = { pid, mid -> viewModel.setDefaultSelection(pid, mid) },
                        onAddProvider = { navController.navigate("add_provider") },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("search_service") {
                    SearchServiceScreen(
                        searchKeys = searchKeys,
                        onSaveKey = { name, key -> viewModel.setSearchKey(name, key) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("storage") {
                    StorageScreen(
                        chats = chats,
                        onClearAll = {
                            val ids = chats.map { it.id }
                            val deletingActiveChat = currentChatId != null && currentChatId in ids
                            viewModel.deleteChats(ids) {
                                if (deletingActiveChat) {
                                    navController.navigate("chats") {
                                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                                scope.launch { snackbarHostState.showSnackbar(tx("All chats deleted")) }
                            }
                        },
                        onBack = { navController.popBackStack() },
                        generatedBytes = generatedMediaBytes,
                        onClearGenerated = {
                            viewModel.clearGeneratedMedia {
                                scope.launch { snackbarHostState.showSnackbar(tx("Generated files deleted")) }
                            }
                        }
                    )
                }

                composable("mcp") {
                    SimpleMcpScreen(
                        servers = mcpServers,
                        onToggle = { id ->
                            val i = mcpServers.indexOfFirst { it.id == id }
                            if (i >= 0) mcpServers[i] = mcpServers[i].copy(enabled = !mcpServers[i].enabled)
                        },
                        onAdd = { mcpServers.add(it) },
                        onRemove = { id -> mcpServers.removeAll { it.id == id } },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("search") {
                    SimpleSearchScreen(
                        onBack = { navController.popBackStack() },
                        onSearch = { query ->
                            val id = viewModel.createNewChat()
                            navController.navigate("chat/$id")
                            viewModel.sendMessage(query, searchMode = SearchMode.LOCAL)
                        }
                    )
                }
            }
        }
    }
}

class AppViewModelFactory(
    private val repository: com.ariai.app.data.repository.ChatRepository,
    private val prefs: com.ariai.app.data.local.PreferencesManager,
    private val workspaceRepository: com.ariai.app.data.repository.WorkspaceRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(repository, prefs, workspaceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
