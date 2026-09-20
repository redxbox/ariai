package com.ariai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    val snackbarHostState = remember { SnackbarHostState() }

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                drawerContentColor = Color.Black,
                modifier = Modifier.width(300.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF6C4DFF)), contentAlignment = Alignment.Center) {
                            Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        }
                        Column {
                            Text("AriAI", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
                            Text("Personal AI Assistant", color = Color.Black.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = {
                            scope.launch { drawerState.close() }
                            val newId = viewModel.createNewChat()
                            navController.navigate("chat/$newId")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4DFF))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("New Chat", fontWeight = FontWeight.Medium)
                    }
                }
                HorizontalDivider(color = Color.Black.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))
                DrawerItem(icon = Icons.Default.ChatBubbleOutline, label = "Chats", selected = currentRoute.startsWith("chat") || currentRoute == "chats" || currentRoute == "home", onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("chats") { launchSingleTop = true }
                })
                DrawerItem(icon = Icons.Default.AutoAwesome, label = "Tools", selected = currentRoute == "tools", onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("tools") { launchSingleTop = true }
                })
                DrawerItem(icon = Icons.Default.Storage, label = "Providers", selected = currentRoute == "providers", onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("providers") { launchSingleTop = true }
                })
                DrawerItem(icon = Icons.Default.Image, label = "Image Gen", selected = currentRoute == "imagegen", onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("imagegen") { launchSingleTop = true }
                })
                DrawerItem(icon = Icons.Default.Settings, label = "Settings", selected = currentRoute == "settings", onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("settings") { launchSingleTop = true }
                })
                Spacer(Modifier.weight(1f))
                HorizontalDivider(color = Color.Black.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
                DrawerItem(icon = Icons.Default.Info, label = "About AriAI v1.0", selected = false, onClick = {
                    scope.launch {
                        drawerState.close()
                        snackbarHostState.showSnackbar("AriAI v1.0 - Personal AI Assistant")
                    }
                })
                DrawerItem(icon = Icons.Default.Share, label = "Share App", selected = false, onClick = {
                    scope.launch {
                        drawerState.close()
                        snackbarHostState.showSnackbar("Share AriAI with friends")
                    }
                })
                Spacer(Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            containerColor = Color(0xFFFEFBFF),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (!currentRoute.startsWith("chat/")) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.95f)),
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Open menu", tint = Color.Black)
                            }
                        },
                        title = {
                            Text(
                                when {
                                    currentRoute == "home" || currentRoute == "chats" -> "AriAI"
                                    currentRoute == "tools" -> "Tools"
                                    currentRoute == "providers" -> "Providers"
                                    currentRoute == "settings" -> "Settings"
                                    currentRoute == "imagegen" -> "Image Generation"
                                    else -> "AriAI"
                                },
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                        },
                        actions = {
                            if (currentRoute == "home" || currentRoute == "chats") {
                                IconButton(onClick = {
                                    val newId = viewModel.createNewChat()
                                    navController.navigate("chat/$newId")
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "New chat", tint = Color.Black)
                                }
                            }
                            if (currentRoute == "tools" || currentRoute == "providers") {
                                IconButton(onClick = { scope.launch { snackbarHostState.showSnackbar("Search") } }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Black)
                                }
                            }
                        }
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(padding)
            ) {
                composable("home") {
                    if (chats.isEmpty()) {
                        HomeScreen(
                            onNavigateToChat = { navController.navigate("chats") },
                            onNavigateToProviders = { navController.navigate("providers") },
                            onNavigateToTools = { navController.navigate("tools") },
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNewChat = { prompt ->
                                val newId = viewModel.createNewChat()
                                navController.navigate("chat/$newId")
                                if (prompt.isNotBlank()) {
                                    viewModel.sendMessage(prompt)
                                }
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
                        onCopyMessage = { },
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }

                composable("tools") {
                    NewToolsScreen(
                        onToolClick = { prompt ->
                            if (prompt == "Image Generation") {
                                navController.navigate("imagegen")
                            } else {
                                val newId = viewModel.createNewChat()
                                navController.navigate("chat/$newId")
                                viewModel.sendMessage(prompt)
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

                composable("welcome") {
                    WelcomeScreen(
                        onContinue = { navController.navigate("home") { popUpTo("welcome") { inclusive = true } } },
                        onAddProvider = { navController.navigate("add_provider") }
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = label, tint = if (selected) Color(0xFF6C4DFF) else Color.Black.copy(alpha = 0.6f)) },
        label = { Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) Color(0xFF6C4DFF) else Color.Black) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        shape = RoundedCornerShape(16.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Color(0xFF6C4DFF).copy(alpha = 0.12f),
            unselectedContainerColor = Color.Transparent
        )
    )
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
