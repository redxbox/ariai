package com.ariai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
    val context = LocalContext.current
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                // Glassmorphism Bottom Navigation
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    tonalElevation = 0.dp,
                    shadowElevation = 12.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 0.dp,
                            modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        ) {
                            NavigationBarItem(
                                icon = { 
                                    BadgedBox(badge = { if (chats.isNotEmpty()) Badge { Text("${chats.size}") } }) {
                                        Icon(Icons.Default.Chat, contentDescription = null) 
                                    }
                                },
                                label = { Text(strings.chats) },
                                selected = currentRoute == "chats",
                                onClick = { 
                                    try {
                                        navController.navigate("chats") { launchSingleTop = true }
                                    } catch (e: Exception) {}
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Face, contentDescription = null) },
                                label = { Text(strings.agents) },
                                selected = currentRoute == "agents",
                                onClick = { 
                                    try {
                                        navController.navigate("agents") { launchSingleTop = true }
                                    } catch (e: Exception) {}
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Image, contentDescription = null) },
                                label = { Text(strings.imageGen) },
                                selected = currentRoute == "imagegen",
                                onClick = { 
                                    try {
                                        navController.navigate("imagegen") { launchSingleTop = true }
                                    } catch (e: Exception) {}
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text(strings.settings) },
                                selected = currentRoute == "settings",
                                onClick = { 
                                    try {
                                        navController.navigate("settings") { launchSingleTop = true }
                                    } catch (e: Exception) {}
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (providers.isEmpty()) "welcome" else "chats",
            modifier = Modifier.padding(padding),
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 3 }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 3 }) + fadeOut() }
        ) {
            composable("welcome") {
                WelcomeScreen(
                    onContinue = { 
                        try {
                            navController.navigate("chats") { popUpTo("welcome") { inclusive = true } }
                        } catch (e: Exception) {}
                    },
                    onAddProvider = { 
                        try {
                            navController.navigate("add_provider")
                        } catch (e: Exception) {}
                    }
                )
            }
            composable("chats") {
                ChatListScreen(
                    chats = chats,
                    onChatClick = { chat ->
                        try {
                            viewModel.loadChat(chat.id)
                            navController.navigate("chat/${chat.id}")
                        } catch (e: Exception) {}
                    },
                    onNewChat = {
                        try {
                            val newId = viewModel.createNewChat()
                            navController.navigate("chat/$newId")
                        } catch (e: Exception) {}
                    },
                    onDeleteChat = { 
                        try { viewModel.deleteChat(it.id) } catch (e: Exception) {}
                    },
                    onPinChat = { 
                        try { viewModel.pinChat(it) } catch (e: Exception) {}
                    },
                    onVoiceClick = {
                        if (!hasAudioPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            try { navController.navigate("voice") } catch (e: Exception) {}
                        }
                    }
                )
            }

            composable("voice") {
                VoiceChatScreen(
                    onBack = { 
                        try { navController.popBackStack() } catch (e: Exception) {}
                    },
                    onSendVoiceMessage = { text ->
                        try {
                            // Create new chat with voice input
                            val newId = viewModel.createNewChat()
                            viewModel.loadChat(newId)
                            viewModel.sendMessage(text)
                            navController.navigate("chat/$newId") {
                                popUpTo("voice") { inclusive = true }
                            }
                        } catch (e: Exception) {}
                    }
                )
            }

            composable(
                "chat/{chatId}",
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                val currentChat = chats.find { it.id == chatId }

                LaunchedEffect(chatId) {
                    try {
                        viewModel.loadChat(chatId)
                    } catch (e: Exception) {}
                }

                ChatScreen(
                    chat = currentChat,
                    messages = messages,
                    providers = providers,
                    isStreaming = isStreaming,
                    currentStreamingContent = streamingContent,
                    selectedModel = currentChat?.modelId,
                    onSendMessage = { content ->
                        try { viewModel.sendMessage(content) } catch (e: Exception) {}
                    },
                    onBack = { 
                        try { navController.popBackStack() } catch (e: Exception) {}
                    },
                    onBranchMessage = { 
                        try { viewModel.branchMessage(it) } catch (e: Exception) {}
                    },
                    onRegenerate = { 
                        try { viewModel.regenerateMessage(it) } catch (e: Exception) {}
                    },
                    onCopyMessage = { },
                    onProviderChange = { providerId, modelId ->
                        try { viewModel.updateChatProvider(chatId, providerId, modelId) } catch (e: Exception) {}
                    }
                )
            }

            composable("agents") {
                AgentListScreen(
                    agents = agents,
                    onAgentClick = { agent ->
                        try {
                            val newId = viewModel.createNewChat(agentId = agent.id)
                            navController.navigate("chat/$newId")
                        } catch (e: Exception) {}
                    },
                    onAddAgent = { 
                        try { navController.navigate("add_agent") } catch (e: Exception) {}
                    },
                    onDeleteAgent = { 
                        try { viewModel.deleteAgent(it.id) } catch (e: Exception) {}
                    }
                )
            }

            composable("add_agent") {
                AddAgentScreen(
                    onSave = { agent ->
                        try {
                            viewModel.saveAgent(agent)
                            navController.popBackStack()
                        } catch (e: Exception) {}
                    },
                    onBack = { 
                        try { navController.popBackStack() } catch (e: Exception) {}
                    }
                )
            }

            composable("imagegen") {
                ImageGenScreen(
                    providers = providers,
                    generatedImages = generatedImages,
                    isGenerating = isGeneratingImage,
                    onGenerate = { prompt, model, size ->
                        try { viewModel.generateImage(prompt, model, size) } catch (e: Exception) {}
                    },
                    onBack = { 
                        try { navController.popBackStack() } catch (e: Exception) {}
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    currentLanguage = language,
                    currentTheme = theme,
                    dynamicColor = dynamicColor,
                    searchKeys = searchKeys,
                    onLanguageChange = { 
                        try { viewModel.setLanguage(it) } catch (e: Exception) {}
                    },
                    onThemeChange = { 
                        try { viewModel.setTheme(it) } catch (e: Exception) {}
                    },
                    onDynamicColorChange = { 
                        try { viewModel.setDynamicColor(it) } catch (e: Exception) {}
                    },
                    onSearchKeyChange = { provider, key -> 
                        try { viewModel.setSearchKey(provider, key) } catch (e: Exception) {}
                    },
                    onProvidersClick = { 
                        try { navController.navigate("providers") } catch (e: Exception) {}
                    }
                )
            }

            composable("providers") {
                ProviderListScreen(
                    providers = providers,
                    onAddProvider = { 
                        try { navController.navigate("add_provider") } catch (e: Exception) {}
                    },
                    onEditProvider = { provider ->
                        try { navController.navigate("edit_provider/${provider.id}") } catch (e: Exception) {}
                    },
                    onDeleteProvider = { 
                        try { viewModel.deleteProvider(it.id) } catch (e: Exception) {}
                    },
                    onTestProvider = { }
                )
            }

            composable("add_provider") {
                AddProviderScreen(
                    onSave = { provider ->
                        try {
                            viewModel.saveProvider(provider)
                            navController.popBackStack()
                        } catch (e: Exception) {}
                    },
                    onBack = { 
                        try { navController.popBackStack() } catch (e: Exception) {}
                    }
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
                        try {
                            viewModel.saveProvider(updated)
                            navController.popBackStack()
                        } catch (e: Exception) {}
                    },
                    onBack = { 
                        try { navController.popBackStack() } catch (e: Exception) {}
                    }
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
