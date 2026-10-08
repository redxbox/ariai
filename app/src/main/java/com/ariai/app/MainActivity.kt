package com.ariai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            val dynamicColor by viewModel.dynamicColor.collectAsState()
            val strings = getStringsForLanguage(language)

            val isDarkTheme = when (theme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            CompositionLocalProvider(LocalStrings provides strings) {
                AriAiTheme(darkTheme = isDarkTheme, dynamicColor = dynamicColor) {
                    AppRoot(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "chats"
    val snackbarHostState = remember { SnackbarHostState() }

    val providers by viewModel.providers.collectAsState()
    val chats by viewModel.chats.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    val language by viewModel.language.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val searchKeys by viewModel.searchKeys.collectAsState()
    val currentChatId by viewModel.currentChatId.collectAsState()

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
                drawerContainerColor = Color.White,
                drawerContentColor = Color(0xFF1C1B1F),
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
        Scaffold(
            containerColor = Color(0xFFFEFBFF),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "chats",
                modifier = Modifier.padding(padding)
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
                        onSendMessage = { viewModel.sendMessage(it) },
                        onBack = { newChat() },
                        onBranchMessage = { viewModel.branchMessage(it) },
                        onRegenerate = { viewModel.regenerateMessage(it) },
                        onCopyMessage = { },
                        onOpenDrawer = { scope.launch { drawerState.open() } }
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
                        onBack = { navController.popBackStack() }
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
                        onThemeChange = { viewModel.setTheme(it) },
                        onLanguageChange = { viewModel.setLanguage(it) },
                        onDynamicColorChange = { viewModel.setDynamicColor(it) }
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
                    SimpleMcpScreen(onBack = { navController.popBackStack() })
                }

                composable("search") {
                    SimpleSearchScreen(
                        onBack = { navController.popBackStack() },
                        onSearch = { query ->
                            val id = viewModel.createNewChat()
                            navController.navigate("chat/$id")
                            viewModel.sendMessage(query, useWebSearch = true)
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
