package com.ariai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ariai.app.ui.screens.*
import com.ariai.app.ui.theme.AriAiTheme
import com.ariai.app.util.LocalStrings
import com.ariai.app.util.getStringsForLanguage

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
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            CompositionLocalProvider(LocalStrings provides strings) {
                AriAiTheme(darkTheme = isDarkTheme, dynamicColor = dynamicColor) {
                    AriAiAppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AriAiAppNavigation(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val providers by viewModel.providers.collectAsState()
    val chats by viewModel.chats.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    val generatedImages by viewModel.generatedImages.collectAsState()
    val isGeneratingImage by viewModel.isGeneratingImage.collectAsState()
    val language by viewModel.language.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val searchKeys by viewModel.searchKeys.collectAsState()

    val bottomNavRoutes = listOf("chats", "tools", "providers", "settings")
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color(0xFF0F0F2A),
                    contentColor = Color.White
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                        label = { Text("Chat") },
                        selected = currentRoute == "chats",
                        onClick = { navController.navigate("chats") { launchSingleTop = true } }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                        label = { Text("Tools") },
                        selected = currentRoute == "tools",
                        onClick = { navController.navigate("tools") { launchSingleTop = true } }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Storage, contentDescription = null) },
                        label = { Text("Providers") },
                        selected = currentRoute == "providers",
                        onClick = { navController.navigate("providers") { launchSingleTop = true } }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Settings") },
                        selected = currentRoute == "settings",
                        onClick = { navController.navigate("settings") { launchSingleTop = true } }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    onNavigateToChat = { navController.navigate("chats") },
                    onNavigateToProviders = { navController.navigate("providers") },
                    onNavigateToTools = { navController.navigate("tools") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNewChat = {
                        val newId = viewModel.createNewChat()
                        navController.navigate("chat/$newId")
                    }
                )
            }
            composable("chats") {
                // Show glass home when empty, otherwise chat list
                if (chats.isEmpty()) {
                    HomeScreen(
                        onNavigateToChat = {},
                        onNavigateToProviders = { navController.navigate("providers") },
                        onNavigateToTools = { navController.navigate("tools") },
                        onNavigateToSettings = { navController.navigate("settings") },
                        onNewChat = {
                            val newId = viewModel.createNewChat()
                            navController.navigate("chat/$newId")
                        }
                    )
                } else {
                    ChatListScreen(
                        chats = chats,
                        onChatClick = { chat ->
                            viewModel.loadChat(chat.id)
                            navController.navigate("chat/${chat.id}")
                        },
                        onNewChat = {
                            val newId = viewModel.createNewChat()
                            navController.navigate("chat/$newId")
                        },
                        onDeleteChat = { viewModel.deleteChat(it.id) },
                        onPinChat = { viewModel.pinChat(it) }
                    )
                }
            }

            composable(
                "chat/{chatId}",
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                val currentChat = chats.find { it.id == chatId }

                LaunchedEffect(chatId) { viewModel.loadChat(chatId) }

                NewChatScreen(
                    chat = currentChat,
                    messages = messages,
                    isStreaming = isStreaming,
                    currentStreamingContent = streamingContent,
                    selectedModel = currentChat?.modelId ?: "GPT-4o",
                    providers = providers,
                    onSendMessage = { content -> viewModel.sendMessage(content) },
                    onBack = { navController.popBackStack() },
                    onBranchMessage = { viewModel.branchMessage(it) },
                    onRegenerate = { viewModel.regenerateMessage(it) },
                    onCopyMessage = { }
                )
            }

            composable("tools") {
                NewToolsScreen(
                    onToolClick = { tool ->
                        when (tool) {
                            "Summarize", "Write", "Code" -> {
                                val newId = viewModel.createNewChat()
                                navController.navigate("chat/$newId")
                            }
                            "Generate Image" -> navController.navigate("imagegen")
                            else -> {
                                val newId = viewModel.createNewChat()
                                navController.navigate("chat/$newId")
                            }
                        }
                    }
                )
            }

            composable("imagegen") {
                ImageGenScreen(
                    providers = providers,
                    generatedImages = generatedImages,
                    isGenerating = isGeneratingImage,
                    onGenerate = { prompt, model, size -> viewModel.generateImage(prompt, model, size) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("providers") {
                NewProvidersScreen(
                    providers = providers,
                    onAddProvider = { navController.navigate("add_provider") },
                    onEditProvider = { provider -> navController.navigate("edit_provider/${provider.id}") },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                NewSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onProvidersClick = { navController.navigate("providers") },
                    currentTheme = theme,
                    currentLanguage = language,
                    dynamicColor = dynamicColor,
                    onThemeChange = { viewModel.setTheme(it) },
                    onLanguageChange = { viewModel.setLanguage(it) },
                    onDynamicColorChange = { viewModel.setDynamicColor(it) }
                )
            }

            composable("add_provider") {
                AddProviderScreen(
                    onSave = { provider ->
                        viewModel.saveProvider(provider)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                "edit_provider/{providerId}",
                arguments = listOf(navArgument("providerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
                val provider = providers.find { it.id == providerId }
                AddProviderScreen(
                    initialProvider = provider,
                    onSave = { updated ->
                        viewModel.saveProvider(updated)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Legacy routes
            composable("welcome") {
                WelcomeScreen(
                    onContinue = { navController.navigate("home") { popUpTo("welcome") { inclusive = true } } },
                    onAddProvider = { navController.navigate("add_provider") }
                )
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
