package com.ariai.app

import com.ariai.app.ui.theme.*

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
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
import com.ariai.app.data.models.McpServerItem
import com.ariai.app.data.models.OAuth2Config
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import com.ariai.app.data.remote.OAuthAuthorizationLauncher
import com.ariai.app.data.models.SearchMode
import com.ariai.app.util.UiLang
import com.ariai.app.util.tx
import kotlinx.coroutines.launch

private const val PRIVACY_POLICY_URL = "https://github.com/redxbox/ariai/blob/main/PRIVACY_POLICY.md"

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = application as AriAiApp
            val viewModelFactory = AppViewModelFactory(
                app.repository,
                app.preferencesManager,
                app.workspaceRepository,
                app.oauthSessionManager
            )
            val viewModel: AppViewModel = viewModel(factory = viewModelFactory)

            val language by viewModel.language.collectAsState()
            val theme by viewModel.theme.collectAsState()
            val dynamicColor by viewModel.dynamicColor.collectAsState()
            val isDarkTheme = when (theme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            SideEffect { UiLang.code = language }
            val direction = if (language == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
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
    DisposableEffect(viewModel) {
        val processLifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.onAppBackgrounded()
        }
        processLifecycle.addObserver(observer)
        onDispose { processLifecycle.removeObserver(observer) }
    }
    val navController = rememberNavController()
    // Opening the app lands directly on a chat, not on a list.
    val startRoute = remember { "chat/${viewModel.createNewChat()}" }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: startRoute
    val isProtectedSettingsRoute = isSensitiveSettingsRoute(currentRoute)
    val snackbarHostState = remember { SnackbarHostState() }
    var showAbout by remember { mutableStateOf(false) }

    fun beginOAuth(ownerType: String, ownerId: String, resourceUri: String, config: OAuth2Config) {
        val activity = context.findActivity()
        if (activity == null) {
            scope.launch { snackbarHostState.showSnackbar(tx("OAuth sign-in is unavailable here.")) }
            return
        }
        val app = activity.application as AriAiApp
        scope.launch {
            try {
                OAuthAuthorizationLauncher.begin(activity, app.preferencesManager, ownerType, ownerId, resourceUri, config)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                snackbarHostState.showSnackbar(failure.message ?: tx("Could not open the OAuth sign-in page."))
            }
        }
    }

    fun authorizeProvider(provider: Provider) {
        val config = provider.oauthConfig
        if (config == null) {
            scope.launch { snackbarHostState.showSnackbar(tx("Enter valid OAuth settings and client ID.")) }
            return
        }
        viewModel.persistProvider(provider) { saved ->
            if (saved) beginOAuth("PROVIDER", provider.id, provider.baseUrl, config)
            else scope.launch { snackbarHostState.showSnackbar(tx("Could not save provider. Please try again.")) }
        }
    }

    fun authorizeMcpServer(server: McpServerItem) {
        val config = server.oauthConfig
        if (config == null) {
            scope.launch { snackbarHostState.showSnackbar(tx("Enter valid OAuth settings and client ID.")) }
            return
        }
        viewModel.addMcpServer(server) { saved ->
            if (saved) beginOAuth("MCP_SERVER", server.id, server.url, config)
            else scope.launch { snackbarHostState.showSnackbar(tx("Could not save MCP server. Please try again.")) }
        }
    }

    val providers by viewModel.providers.collectAsState()
    val chats by viewModel.chats.collectAsState()
    val modelOverrides by viewModel.modelOverrides.collectAsState()
    val chatModes by viewModel.chatModes.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    val toolExecutionUpdates by viewModel.toolExecutionUpdates.collectAsState()
    val generatedMediaBytes by viewModel.generatedMediaBytes.collectAsState()
    val language by viewModel.language.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val agents by viewModel.agents.collectAsState()
    val workspaceFiles by viewModel.workspaceFiles.collectAsState()
    val mcpServers by viewModel.mcpServers.collectAsState()
    val mcpServerRuntime by viewModel.mcpServerRuntime.collectAsState()
    val currentChatId by viewModel.currentChatId.collectAsState()
    val defaultProviderId by viewModel.defaultProviderId.collectAsState()
    val defaultModelId by viewModel.defaultModelId.collectAsState()
    val reasoningLevel by viewModel.reasoningLevel.collectAsState()
    val streamResponse by viewModel.streamResponse.collectAsState()
    val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsState()
    var protectedSettingsAuthenticated by remember { mutableStateOf(false) }
    var biometricAuthAttempt by remember { mutableIntStateOf(0) }
    var biometricAuthError by remember { mutableStateOf<String?>(null) }
    val searchModeState by viewModel.searchMode.collectAsState()
    val searchProviderState by viewModel.searchProvider.collectAsState()
    val searchKeys by viewModel.searchKeys.collectAsState()
    val favoriteModels by viewModel.favoriteModels.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()

    LaunchedEffect(biometricLockEnabled, isProtectedSettingsRoute, biometricAuthAttempt) {
        when {
            !biometricLockEnabled -> {
                protectedSettingsAuthenticated = false
                biometricAuthError = null
            }
            !isProtectedSettingsRoute -> {
                protectedSettingsAuthenticated = false
                biometricAuthError = null
            }
            !protectedSettingsAuthenticated -> {
                val activity = context as? FragmentActivity
                if (activity == null) {
                    biometricAuthError = tx("Biometric authentication is unavailable.")
                } else if (androidx.biometric.BiometricManager.from(activity)
                        .canAuthenticate(biometricAuthenticatorTypes()) != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
                    biometricAuthError = tx("Biometric authentication is unavailable.")
                } else {
                    biometricAuthError = null
                    val authenticated = authenticateSensitiveSettings(activity)
                    if (authenticated) {
                        protectedSettingsAuthenticated = true
                        biometricAuthError = null
                    } else {
                        biometricAuthError = tx("Authentication was cancelled or failed.")
                    }
                }
            }
        }
    }

    DisposableEffect(biometricLockEnabled, isProtectedSettingsRoute) {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        var backgrounded = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                backgrounded = true
                if (biometricLockEnabled && isProtectedSettingsRoute) protectedSettingsAuthenticated = false
            } else if (event == Lifecycle.Event.ON_START && backgrounded) {
                backgrounded = false
                if (biometricLockEnabled && isProtectedSettingsRoute) biometricAuthAttempt++
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val needsBiometricGate = biometricLockEnabled && isProtectedSettingsRoute && !protectedSettingsAuthenticated
    val modelUsageCounts = remember(chats, modelOverrides) {
        chats.mapNotNull { savedChat ->
            val override = modelOverrides[savedChat.id]
            val providerId = override?.first ?: savedChat.providerId
            val modelId = override?.second ?: savedChat.modelId
            if (providerId != null && modelId != null) providerId to modelId else null
        }.groupingBy { it }.eachCount()
    }
    val defaultModelName = providers.firstOrNull { it.id == defaultProviderId }?.let { p ->
        p.models.firstOrNull { it.id == defaultModelId }?.let { "${it.displayName} · ${p.name}" }
    } ?: tx("Not chosen")
    val activeChatForDrawer = chats.firstOrNull { it.id == currentChatId }
    val drawerProviderId = activeChatForDrawer?.let { modelOverrides[it.id]?.first ?: it.providerId } ?: defaultProviderId
    val drawerModelId = activeChatForDrawer?.let { modelOverrides[it.id]?.second ?: it.modelId } ?: defaultModelId
    val drawerProvider = providers.firstOrNull { it.id == drawerProviderId && it.enabled }
    val drawerModelName = drawerProvider?.models?.firstOrNull { it.id == drawerModelId }?.displayName
    val searchModelReady = defaultProviderId != null && defaultModelId != null && providers.any { provider ->
        provider.id == defaultProviderId && provider.enabled && provider.models.any { it.id == defaultModelId }
    }

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

    fun activateNativeSearch(provider: Provider) {
        if (provider.type != ProviderType.GEMINI && provider.type != ProviderType.ANTHROPIC) return
        if (!provider.enabled || provider.apiKey.isBlank() || provider.apiKeyNeedsReentry || provider.providerConfigNeedsReentry) return
        val textModel = provider.models.firstOrNull { it.outputsText && it.id.isNotBlank() } ?: return
        currentChatId?.let { viewModel.updateChatProvider(it, provider.id, textModel.id) }
            ?: viewModel.setDefaultSelection(provider.id, textModel.id)
        viewModel.setSearchMode(SearchMode.MODEL)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        scrimColor = Color.Black.copy(alpha = 0.34f),
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 30.dp, bottomEnd = 30.dp),
                drawerContainerColor = AriCard,
                drawerContentColor = AriInk,
                drawerTonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth(0.88f).widthIn(max = 360.dp)
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
                            showAbout = true
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
                        onDeleteFile = { name, onResult -> viewModel.deleteWorkspaceFile(name, onResult) },
                        onGenerateFile = { name, prompt, existingContent, onResult ->
                            viewModel.generateWorkspaceFile(name, prompt, existingContent, onResult)
                        }
                    )
                }

                composable(
                    "chat/{chatId}",
                    arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                ) { entry ->
                    val chatId = entry.arguments?.getString("chatId") ?: ""
                    val currentChat = chats.firstOrNull { it.id == chatId } ?: viewModel.getChatSnapshot(chatId)
                    val activeAgent = currentChat?.agentId?.let { id -> agents.firstOrNull { it.id == id } }
                    LaunchedEffect(chatId) { viewModel.loadChat(chatId) }
                    NewChatScreen(
                        chat = currentChat,
                        activeAgent = activeAgent,
                        screenChatId = chatId,
                        messages = messages,
                        isStreaming = isStreaming,
                        isSending = isSending,
                        currentStreamingContent = streamingContent,
                        toolExecutionUpdates = toolExecutionUpdates,
                        selectedModel = modelOverrides[chatId]?.second ?: currentChat?.modelId,
                        selectedProviderId = modelOverrides[chatId]?.first ?: currentChat?.providerId,
                        providers = providers,
                        reasoning = reasoningLevel,
                        searchMode = searchModeState,
                        searchProvider = searchProviderState,
                        searchKeys = searchKeys,
                        biometricLockEnabled = biometricLockEnabled,
                        favorites = favoriteModels,
                        modelUsageCounts = modelUsageCounts,
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
                        onNavigateToBranch = { messageId ->
                            viewModel.resolveBranchChat(messageId) { branchChatId ->
                                if (branchChatId != null) openChat(branchChatId)
                                else scope.launch { snackbarHostState.showSnackbar(tx("This conversation branch is no longer available.")) }
                            }
                        },
                        onRegenerate = { message ->
                            val retryChatId = viewModel.regenerateMessage(message, reasoningLevel, searchModeState)
                            if (retryChatId != null && retryChatId != chatId) {
                                navController.navigate("chat/$retryChatId") { launchSingleTop = true }
                            }
                        },
                        onCopyMessage = { },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenAgents = { navController.navigate("agents") { launchSingleTop = true } },
                        onSelectModel = { pid, mid -> viewModel.updateChatProvider(chatId, pid, mid) },
                        onEditImage = { message, instruction, mask -> viewModel.editGeneratedImage(message, instruction, mask) },
                        onToggleFavorite = { pid, mid -> viewModel.toggleFavoriteModel(pid, mid) },
                        onReasoningChange = { viewModel.setReasoningLevel(it) },
                        onSearchModeChange = { viewModel.setSearchMode(it) },
                        onSearchProviderChange = { viewModel.setSearchProvider(it) },
                        onSaveSearchKey = { provider, key -> viewModel.setSearchKey(provider, key) },
                        onCompressHistory = { done -> viewModel.compressHistory(done) },
                        onNewImageChat = { newImageChat() },
                        chatMode = chatModes[chatId] ?: ChatMode.TEXT,
                        onChatModeChange = { viewModel.setChatMode(chatId, it) },
                        onOpenExtensions = { navController.navigate("mcp") },
                        fontSize = fontSize,
                        onAddProvider = { navController.navigate("add_provider") },
                        onManageProviders = { navController.navigate("providers") },
                        onSaveNativeSearchProvider = { provider, onSaved ->
                            viewModel.saveProviderAndReturn(provider, onSaved)
                        },
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
                        onAddPreset = { navController.navigate("add_provider?preset=${it.name}&presetLabelKey=") },
                        onAddProviderPreset = { type, labelKey ->
                            navController.navigate("add_provider?preset=${type.name}&presetLabelKey=$labelKey")
                        },
                        fetchModels = { viewModel.fetchModels(it) },
                        onSaveProvider = { provider, onSaved ->
                            viewModel.saveProviderAndReturn(provider) { savedProvider ->
                                if (savedProvider != null) activateNativeSearch(savedProvider)
                                else scope.launch { snackbarHostState.showSnackbar(tx("Could not save provider. Please try again.")) }
                                onSaved(savedProvider)
                            }
                        },
                        onAuthorizeOAuth = { provider -> authorizeProvider(provider) },
                        onImportProvider = { provider ->
                            viewModel.saveProviderAndReturn(provider) { savedProvider ->
                                if (savedProvider == null) {
                                    scope.launch { snackbarHostState.showSnackbar(tx("Could not save provider. Please try again.")) }
                                } else {
                                    activateNativeSearch(savedProvider)
                                }
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
                    "add_provider?preset={preset}&presetLabelKey={presetLabelKey}",
                    arguments = listOf(
                        navArgument("preset") { type = NavType.StringType; defaultValue = "" },
                        navArgument("presetLabelKey") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { entry ->
                    AddProviderScreen(
                        presetType = entry.arguments?.getString("preset")
                            ?.let { runCatching { com.ariai.app.data.models.ProviderType.valueOf(it) }.getOrNull() },
                        presetLabelKey = entry.arguments?.getString("presetLabelKey")?.ifBlank { null },
                        existingProviderNames = providers.map { it.name }.toSet(),
                        fetchModels = { viewModel.fetchModels(it) },
                        onSave = { provider ->
                            viewModel.saveProviderAndReturn(provider) { savedProvider ->
                                if (savedProvider != null) {
                                    activateNativeSearch(savedProvider)
                                    navController.popBackStack()
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar(tx("Could not save provider. Please try again.")) }
                                }
                            }
                        },
                        onAuthorizeOAuth = { provider -> authorizeProvider(provider) },
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
                            viewModel.saveProviderAndReturn(provider) { savedProvider ->
                                if (savedProvider != null) {
                                    activateNativeSearch(savedProvider)
                                    navController.popBackStack()
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar(tx("Could not save provider. Please try again.")) }
                                }
                            }
                        },
                        onAuthorizeOAuth = { provider -> authorizeProvider(provider) },
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
                        onDefaultModelClick = { navController.navigate("default_model") },
                        onPreferencesClick = { navController.navigate("preferences") },
                        defaultModelName = defaultModelName,
                        onAboutClick = { showAbout = true },
                        onPrivacyPolicyClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
                            }.onFailure {
                                scope.launch { snackbarHostState.showSnackbar(tx("Could not open the privacy policy.")) }
                            }
                        }
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
                        biometricLockEnabled = biometricLockEnabled,
                        onBiometricLockChange = { viewModel.setBiometricLockEnabled(it) },
                        currentLanguage = language,
                        onLanguageChange = { viewModel.setLanguage(it) },
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
                        providers = providers,
                        searchMode = searchModeState,
                        selectedProviderId = drawerProviderId,
                        selectedModelId = drawerModelId,
                        onModeChange = { viewModel.setSearchMode(it) },
                        onSelectModel = { providerId, modelId ->
                            currentChatId?.let { viewModel.updateChatProvider(it, providerId, modelId) }
                                ?: viewModel.setDefaultSelection(providerId, modelId)
                        },
                        onSaveProvider = { provider, onSaved ->
                            viewModel.saveProviderAndReturn(provider, onSaved)
                        },
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
                        onToggle = viewModel::toggleMcpServer,
                        onAdd = { viewModel.addMcpServer(it) },
                        onAuthorizeOAuth = { server -> authorizeMcpServer(server) },
                        onRemove = viewModel::removeMcpServer,
                        onBack = { navController.popBackStack() },
                        runtimeStates = mcpServerRuntime,
                        onTestConnection = viewModel::testMcpServer,
                        onToggleTool = viewModel::toggleMcpTool,
                        onListResources = viewModel::listMcpResources,
                        onListPrompts = viewModel::listMcpPrompts
                    )
                }

                composable("search") {
                    SimpleSearchScreen(
                        onBack = { navController.popBackStack() },
                        canSearch = searchModelReady,
                        onConfigureModel = { navController.navigate("default_model") },
                        onConfigureSearch = { navController.navigate("search_service") },
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

    if (showAbout) {
        val aboutShape = paperPopupShape(26.dp)
        AlertDialog(
            onDismissRequest = { showAbout = false },
            modifier = paperPopupModifier(aboutShape),
            shape = aboutShape,
            containerColor = AriCard,
            title = { Text(tx("About AriAI"), color = AriInk, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) },
            text = { Text("${tx("Version")} ${BuildConfig.VERSION_NAME}", color = AriMuted) },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text(tx("Close")) }
            }
        )
    }
    if (needsBiometricGate) {
        BiometricLockOverlay(
            error = biometricAuthError,
            onRetry = {
                biometricAuthError = null
                biometricAuthAttempt++
            },
            onCancel = {
                navController.popBackStack()
                if (isProtectedSettingsDestination(navController.currentBackStackEntry?.destination?.route)) {
                    navController.popBackStack()
                }
            }
        )
    }
}

private fun isProtectedSettingsDestination(route: String?): Boolean =
    route?.let(::isSensitiveSettingsRoute) == true

class AppViewModelFactory(
    private val repository: com.ariai.app.data.repository.ChatRepository,
    private val prefs: com.ariai.app.data.local.PreferencesManager,
    private val workspaceRepository: com.ariai.app.data.repository.WorkspaceRepository,
    private val oauthSessionManager: com.ariai.app.data.remote.OAuthSessionManager
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(repository, prefs, workspaceRepository, oauthSessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
