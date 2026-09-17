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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ariai.app.data.models.Provider
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
                AriAiTheme(
                    darkTheme = isDarkTheme,
                    dynamicColor = dynamicColor
                ) {
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
    val agents by viewModel.agents.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    val generatedImages by viewModel.generatedImages.collectAsState()
    val isGeneratingImage by viewModel.isGeneratingImage.collectAsState()
    val language by viewModel.language.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val searchKeys by viewModel.searchKeys.collectAsState()
    val strings = LocalStrings.current

    val bottomNavRoutes = listOf("chats", "agents", "imagegen", "settings")
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                        label = { Text(strings.chats) },
                        selected = currentRoute == "chats",
                        onClick = { navController.navigate("chats") { launchSingleTop = true } }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.SmartToy, contentDescription = null) },
                        label = { Text(strings.agents) },
                        selected = currentRoute == "agents",
                        onClick = { navController.navigate("agents") { launchSingleTop = true } }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Image, contentDescription = null) },
                        label = { Text(strings.imageGen) },
                        selected = currentRoute == "imagegen",
                        onClick = { navController.navigate("imagegen") { launchSingleTop = true } }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(strings.settings) },
                        selected = currentRoute == "settings",
                        onClick = { navController.navigate("settings") { launchSingleTop = true } }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (providers.isEmpty()) "welcome" else "chats",
            modifier = Modifier.padding(padding)
        ) {
            composable("welcome") {
                WelcomeScreen(
                    onContinue = { navController.navigate("chats") { popUpTo("welcome") { inclusive = true } } },
                    onAddProvider = { navController.navigate("add_provider") }
                )
            }
            composable("chats") {
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

            composable(
                "chat/{chatId}",
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                val currentChat = chats.find { it.id == chatId }

                // Load chat when entering
                LaunchedEffect(chatId) {
                    viewModel.loadChat(chatId)
                }

                ChatScreen(
                    chat = currentChat,
                    messages = messages,
                    isStreaming = isStreaming,
                    currentStreamingContent = streamingContent,
                    selectedModel = currentChat?.modelId,
                    onSendMessage = { content ->
                        viewModel.sendMessage(content)
                    },
                    onBack = { navController.popBackStack() },
                    onBranchMessage = { viewModel.branchMessage(it) },
                    onRegenerate = { viewModel.regenerateMessage(it) },
                    onCopyMessage = { /* copy to clipboard */ }
                )
            }

            composable("agents") {
                AgentListScreen(
                    agents = agents,
                    onAgentClick = { agent ->
                        val newId = viewModel.createNewChat(agentId = agent.id)
                        navController.navigate("chat/$newId")
                    },
                    onAddAgent = { navController.navigate("add_agent") },
                    onDeleteAgent = { viewModel.deleteAgent(it.id) }
                )
            }

            composable("add_agent") {
                AddAgentScreen(
                    onSave = { agent ->
                        viewModel.saveAgent(agent)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("imagegen") {
                ImageGenScreen(
                    providers = providers,
                    generatedImages = generatedImages,
                    isGenerating = isGeneratingImage,
                    onGenerate = { prompt, model, size ->
                        viewModel.generateImage(prompt, model, size)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    currentLanguage = language,
                    currentTheme = theme,
                    dynamicColor = dynamicColor,
                    searchKeys = searchKeys,
                    onLanguageChange = { viewModel.setLanguage(it) },
                    onThemeChange = { viewModel.setTheme(it) },
                    onDynamicColorChange = { viewModel.setDynamicColor(it) },
                    onSearchKeyChange = { provider, key -> viewModel.setSearchKey(provider, key) },
                    onProvidersClick = { navController.navigate("providers") }
                )
            }

            composable("providers") {
                ProviderListScreen(
                    providers = providers,
                    onAddProvider = { navController.navigate("add_provider") },
                    onEditProvider = { provider ->
                        navController.navigate("edit_provider/${provider.id}")
                    },
                    onDeleteProvider = { viewModel.deleteProvider(it.id) },
                    onTestProvider = { /* test */ }
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
        }
    }
}

// ViewModel Factory
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
