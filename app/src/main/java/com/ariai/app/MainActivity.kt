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
import com.ariai.app.util.getStringsForLanguage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = application as AriAiApp
            val viewModelFactory = AppViewModelFactory(app.repository, app.preferencesManager)
            val viewModel: AppViewModel = viewModel(factory = viewModelFactory)

            val language by viewModel.language.collectAsState()
            val theme by viewModel.theme.collectAsState()
            val strings = getStringsForLanguage(language)

            val isDarkTheme = when (theme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            // The app is English-only: force LTR so menus and text never mirror on RTL devices.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                CompositionLocalProvider(LocalStrings provides strings) {
                    AriAiTheme(darkTheme = isDarkTheme) {
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
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    val language by viewModel.language.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val searchKeys by viewModel.searchKeys.collectAsState()
    val currentChatId by viewModel.currentChatId.collectAsState()
    val mcpServers = remember { mutableStateListOf<McpServerItem>() }
    val defaultProviderId by viewModel.defaultProviderId.collectAsState()
    val defaultModelId by viewModel.defaultModelId.collectAsState()
    val reasoningLevel by viewModel.reasoningLevel.collectAsState()
    val searchModeState by viewModel.searchMode.collectAsState()
    val favoriteModels by viewModel.favoriteModels.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val defaultModelName = providers.firstOrNull { it.id == defaultProviderId }?.let { p ->
        p.models.firstOrNull { it.id == defaultModelId }?.let { "${it.displayName} · ${p.name}" }
    } ?: "Not chosen"

    fun go(route: String) {
        scope.launch { drawerState.close() }
        navController.navigate(route) { launchSingleTop = true }
    }

    fun openChat(chatId: String) {
        scope.launch { drawerState.close() }
        viewModel.loadChat(chatId)
        navController.navigate("chat/$chatId") { launchSingleTop = true }
    }

    fun newChat() {
        scope.launch { drawerState.close() }
        val id = viewModel.createNewChat()
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
                    onNewChat = { newChat() },
                    onChatClick = { openChat(it.id) },
                    onPinChat = { viewModel.pinChat(it) },
                    onDeleteChat = { viewModel.deleteChat(it.id) },
                    onNavigate = { go(it) },
                    onAbout = {
                        scope.launch {
                            drawerState.close()
                            snackbarHostState.showSnackbar("AriAI v1.0 - Personal AI")
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
                        onDeleteChat = { viewModel.deleteChat(it.id) },
                        onPinChat = { viewModel.pinChat(it) }
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
                        messages = messages,
                        isStreaming = isStreaming,
                        currentStreamingContent = streamingContent,
                        selectedModel = currentChat?.modelId,
                        providers = providers,
                        reasoning = reasoningLevel,
                        searchMode = searchModeState,
                        favorites = favoriteModels,
                        localSearchConfigured = searchKeys.values.any { it.isNotBlank() },
                        onSendMessage = { text, files, level, mode -> viewModel.sendMessage(text, files, level, mode) },
                        onBack = { newChat() },
                        onBranchMessage = { viewModel.branchMessage(it) },
                        onRegenerate = { viewModel.regenerateMessage(it) },
                        onCopyMessage = { },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onSelectModel = { pid, mid -> viewModel.updateChatProvider(chatId, pid, mid) },
                        onToggleFavorite = { pid, mid -> viewModel.toggleFavoriteModel(pid, mid) },
                        onReasoningChange = { viewModel.setReasoningLevel(it) },
                        onSearchModeChange = { viewModel.setSearchMode(it) },
                        onCompressHistory = { done -> viewModel.compressHistory(done) },
                        onOpenExtensions = { navController.navigate("mcp") },
                        onOpenSearchSettings = { navController.navigate("search_service") },
                        fontSize = fontSize,
                        onAddProvider = { navController.navigate("add_provider") }
                    )
                }

                composable("providers") {
                    NewProvidersScreen(
                        providers = providers,
                        onAddProvider = { navController.navigate("add_provider") },
                        onEditProvider = { navController.navigate("edit_provider/${it.id}") },
                        onBack = { navController.popBackStack() },
                        onToggleProvider = { viewModel.saveProvider(it.copy(enabled = !it.enabled)) }
                    )
                }

                composable("add_provider") {
                    AddProviderScreen(
                        onSave = { viewModel.saveProvider(it); navController.popBackStack() },
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
                        onSave = { viewModel.saveProvider(it); navController.popBackStack() },
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
                        dynamicColor = false,
                        onDynamicColorChange = {},
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
                        dynamicColor = false,
                        onDynamicColorChange = {},
                        reasoning = reasoningLevel,
                        onReasoningChange = { viewModel.setReasoningLevel(it) },
                        fontSize = fontSize,
                        onFontSizeChange = { viewModel.setFontSize(it) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("default_model") {
                    DefaultModelScreen(
                        providers = providers,
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
                            chats.forEach { viewModel.deleteChat(it.id) }
                            scope.launch { snackbarHostState.showSnackbar("All chats deleted") }
                        },
                        onBack = { navController.popBackStack() }
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
    private val prefs: com.ariai.app.data.local.PreferencesManager
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(repository, prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
