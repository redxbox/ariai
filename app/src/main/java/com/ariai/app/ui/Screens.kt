package com.ariai.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import android.Manifest
import android.graphics.Bitmap
import android.util.Base64
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.ChatMessage
import com.ariai.app.data.McpServer
import com.ariai.app.data.Screen
import com.ariai.app.data.AppStore

@Composable
fun AriAiApp(vm: AriAiViewModel) {
    val snack = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            ctx.contentResolver.openInputStream(uri)?.use { ins ->
                val bytes = ins.readBytes()
                val b64 = Base64.encodeToString(if (bytes.size > 350_000) bytes.copyOf(350_000) else bytes, Base64.NO_WRAP)
                vm.attachImage(b64, "photo")
            }
        }
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment ?: "file"
            val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()?.take(8000)
            vm.input = (vm.input + "\n" + (text ?: "[file $name]")).trim()
            vm.attach(name)
        }
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) {
            val os = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 82, os)
            val b64 = Base64.encodeToString(os.toByteArray(), Base64.NO_WRAP)
            vm.attachImage(b64, "camera")
        }
    }
    val camPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) takePhoto.launch(null) else vm.snack = "Camera permission denied"
    }
    LaunchedEffect(vm.snack) {
        vm.snack?.let { snack.showSnackbar(it); vm.snack = null }
    }
    val dir = if (vm.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides dir) {
        Box(Modifier.fillMaxSize()) {
            AuroraBackdrop()
            Box(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
            when (vm.screen) {
                Screen.Onboarding -> OnboardPage(vm)
                Screen.Home -> HomePage(vm)
                Screen.Tools -> ToolsPage(vm)
                Screen.Profile -> ProfilePage(vm)
                Screen.Privacy -> SimplePage(vm, "Privacy", "API keys and chats stay on this device. Network calls go only to the base URL you set. AriAi has no account server.")
                Screen.Chat -> ChatPage(vm)
                Screen.Research -> ResearchPage(vm)
                Screen.Agents -> AgentsPage(vm)
                Screen.Projects -> ProjectsPage(vm)
                Screen.Models -> ModelsHubPage(vm)
                Screen.ModelCatalog -> ModelCatalogPage(vm)
                Screen.Settings -> SettingsPage(vm)
                Screen.Preferences -> PrefsPage(vm)
                Screen.General -> GeneralPage(vm)
                Screen.Notifications -> NotifPage(vm)
                Screen.Theme -> ThemePage(vm)
                Screen.Assistant -> AssistantPage(vm)
                Screen.Extensions -> ExtPage(vm)
                Screen.ModelSettings -> ModelPage(vm)
                Screen.Providers -> ProvidersPage(vm)
                Screen.Speech -> SpeechPage(vm)
                Screen.Mcp -> McpPage(vm)
                Screen.Statistics -> StatsPage(vm)
                Screen.SearchService -> SearchPage(vm)
                Screen.WebServer -> WebPage(vm)
                Screen.Backup -> BackupPage(vm)
                Screen.About -> SimplePage(vm, "About", "AriAi 1.2 — a bring-your-own-key client. Keys never leave this phone except to the URL you set. Source: github.com/arashiaz/ariai")
                Screen.Docs -> SimplePage(vm, "Documentation", "Settings → Providers → Name, Base URL (…/v1), API key, Fetch models. Then chat.")
                Screen.Logs -> LogsPage(vm)
                Screen.ChatHistory -> HistoryPage(vm)
                Screen.SearchChats -> SearchChatsPage(vm)
                Screen.QuickMessages -> QuickPage(vm)
                Screen.Prompts -> PromptsPage(vm)
                Screen.Skills -> SimplePage(vm, "Agent Skills", "Skill packages are injected as extra system instructions via Prompts.")
                Screen.Workspace -> SimplePage(vm, "Workspace", "Local files can be attached with + → Upload File.")
            }
            if (vm.drawerOpen) Drawer(vm)
            if (vm.plusOpen) PlusSheet(
                vm,
                onPhoto = { pickImage.launch("image/*") },
                onCamera = {
                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                        takePhoto.launch(null)
                    else camPerm.launch(Manifest.permission.CAMERA)
                },
                onFile = { pickFile.launch("*/*") }
            )
            if (vm.providerSheet) ProviderSheet(vm)
            if (vm.mcpImport) ImportMcp(vm)
            vm.mcpDraft?.let { McpEditor(vm, it) }
            SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).padding(16.dp))
            }
        }
    }
}

@Composable
private fun HomePage(vm: AriAiViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("AriAi", color = Ink, fontWeight = FontWeight.Bold, fontSize = 23.sp)
                Text("Your personal AI workspace", color = Mute, fontSize = 13.sp)
            }
            IconBtn(Icons.Filled.Settings) { vm.go(Screen.Settings) }
        }

        Spacer(Modifier.height(26.dp))

        GlassSurface(
            Modifier.fillMaxWidth(),
            radius = 28,
            emphasized = true
        ) {
            Text("What are you working on?", color = Mute, fontSize = 13.sp)
            Spacer(Modifier.height(5.dp))
            Text("Ask, research, create, or build.", color = Ink, fontWeight = FontWeight.Bold, fontSize = 25.sp)
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(AriGlass.AccentSoft)
                    .border(1.dp, AriGlass.StrokeDark, RoundedCornerShape(17.dp))
                    .clickable { vm.openNewChat() }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, null, tint = Accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Start a conversation", color = Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text("→", color = Accent, fontSize = 20.sp)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Workspace", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(10.dp))
        GlassSurface(Modifier.fillMaxWidth(), 22, emphasized = true) {
            Text("Choose how AriAi should help.", color = Mute, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkspaceCard("Research", "Sources", Icons.Filled.Search, Modifier.weight(1f)) { vm.go(Screen.Research) }
                WorkspaceCard("Create", "Files", Icons.Filled.Edit, Modifier.weight(1f)) { vm.go(Screen.Projects) }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkspaceCard("Agents", "Tools", Icons.Filled.Build, Modifier.weight(1f)) { vm.go(Screen.Agents) }
                WorkspaceCard("Models", "Providers", Icons.Filled.Star, Modifier.weight(1f)) { vm.go(Screen.Models) }
            }
        }

        Spacer(Modifier.height(22.dp))
        Text("Quick start", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile("Explain", "Make ideas simple", Icons.Filled.Info, Modifier.weight(1f), 0) {
                vm.startPrompt("Explain this in simple terms:\n")
            }
            ActionTile("Write", "Draft content", Icons.Filled.Edit, Modifier.weight(1f), 1) {
                vm.startPrompt("Write a clear draft about:\n")
            }
            ActionTile("Code", "Build and debug", Icons.Filled.Build, Modifier.weight(1f), 2) {
                vm.startPrompt("Help me write and debug this code:\n")
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Recent chats", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        if (vm.conversations.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBg)
                    .border(1.dp, Chip, RoundedCornerShape(18.dp)).padding(20.dp)
            ) {
                Text("Your workspace is ready", color = Ink, fontWeight = FontWeight.Medium)
                Text("Start a conversation and it will appear here.", color = Mute, fontSize = 13.sp)
            }
        } else {
            vm.conversations.take(6).forEach { c ->
                HomeRow(c.title, c.preview.take(72), Icons.Filled.Email) { vm.openConv(c.id) }
            }
            Text("See all conversations", color = Link, modifier = Modifier.padding(8.dp).clickable { vm.go(Screen.ChatHistory) })
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun WorkspaceCard(title: String, subtitle: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    GlassSurface(
        modifier,
        radius = 20,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(AccentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = Mute, fontSize = 11.sp)
        }
            Text("›", color = Mute, fontSize = 18.sp)
        }
    }
}

@Composable
private fun ResearchPage(vm: AriAiViewModel) {
    PageScaffold("Research", onBack = { vm.go(Screen.Home) }) {
        Text("Turn a question into a sourced conversation.", color = Mute, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        HubCard("Web research", "Configure live search", Icons.Filled.Search, 0, Modifier.fillMaxWidth()) {
            vm.go(Screen.SearchService)
        }
        Spacer(Modifier.height(10.dp))
        HubCard("Research chat", "Ask with web context enabled", Icons.Filled.Info, 1, Modifier.fillMaxWidth()) {
            vm.startPrompt("Research this topic using current web information. Separate verified facts from uncertainty and include source titles:\n")
        }
        Spacer(Modifier.height(18.dp))
        GlassSurface(Modifier.fillMaxWidth(), 20, emphasized = true) {
            Text("Research workflow", color = Ink, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Question → search → source context → answer", color = Mute, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text("AriAi keeps the research entry point separate from ordinary chat.", color = Mute, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AgentsPage(vm: AriAiViewModel) {
    PageScaffold("Agents", onBack = { vm.go(Screen.Home) }) {
        Text("Configure assistants around instructions, models and tools.", color = Mute, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        HubCard("Assistants", "System instructions and personas", Icons.Filled.Person, 0, Modifier.fillMaxWidth()) {
            vm.go(Screen.Assistant)
        }
        Spacer(Modifier.height(10.dp))
        HubCard("MCP tools", "Connect external tool servers", Icons.Filled.Settings, 1, Modifier.fillMaxWidth()) {
            vm.go(Screen.Mcp)
        }
        Spacer(Modifier.height(10.dp))
        HubCard("Agent skills", "Reusable prompt workflows", Icons.Filled.Build, 2, Modifier.fillMaxWidth()) {
            vm.go(Screen.Skills)
        }
    }
}

@Composable
private fun ProjectsPage(vm: AriAiViewModel) {
    PageScaffold("Projects", onBack = { vm.go(Screen.Home) }) {
        Text("Keep files and work together instead of scattering them across chats.", color = Mute, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        HubCard("Workspace", "Local files and attachments", Icons.Filled.List, 0, Modifier.fillMaxWidth()) {
            vm.go(Screen.Workspace)
        }
        Spacer(Modifier.height(10.dp))
        HubCard("Chat history", "Continue previous work", Icons.Filled.Email, 1, Modifier.fillMaxWidth()) {
            vm.go(Screen.ChatHistory)
        }
        Spacer(Modifier.height(10.dp))
        HubCard("Prompts", "Reusable project instructions", Icons.Filled.Edit, 2, Modifier.fillMaxWidth()) {
            vm.go(Screen.Prompts)
        }
    }
}

@Composable
private fun ModelsHubPage(vm: AriAiViewModel) {
    PageScaffold("Models", onBack = { vm.go(Screen.Home) }) {
        Text("Choose the provider and model that power your workspace.", color = Mute, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        HubCard("Models", "Model profiles, capabilities and tools", Icons.Filled.Star, 0, Modifier.fillMaxWidth()) {
            vm.go(Screen.ModelCatalog)
        }
        Spacer(Modifier.height(10.dp))
        HubCard("Providers", "API keys, endpoints and model discovery", Icons.Filled.Person, 1, Modifier.fillMaxWidth()) {
            vm.go(Screen.Providers)
        }
        Spacer(Modifier.height(10.dp))
        HubCard("Model settings", "Chat, fast, translation and compression", Icons.Filled.Star, 1, Modifier.fillMaxWidth()) {
            vm.go(Screen.ModelSettings)
        }
        Spacer(Modifier.height(10.dp))
        HubCard("Connection logs", "Inspect recent network requests", Icons.Filled.List, 2, Modifier.fillMaxWidth()) {
            vm.go(Screen.Logs)
        }
    }
}

@Composable
private fun ActionTile(title: String, hint: String, icon: ImageVector, modifier: Modifier, kind: Int, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(22.dp)).background(AriGlass.Surface)
            .border(1.dp, AriGlass.StrokeDark, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Glyph(icon, kind, Modifier.size(48.dp), onClick)
        Spacer(Modifier.height(8.dp))
        Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(hint, color = Mute, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HomeRow(title: String, sub: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(18.dp))
            .background(AriGlass.Surface).border(1.dp, AriGlass.StrokeDark, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(AccentSoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontWeight = FontWeight.SemiBold)
            Text(sub, color = Mute, fontSize = 13.sp)
        }
        Text("›", color = Mute, fontSize = 20.sp)
    }
}

@Composable
private fun ToolsPage(vm: AriAiViewModel) {
    PageScaffold("Tools", onBack = { vm.go(Screen.Home) }) {
        Text("Only tools that actually run.", color = Mute, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubCard("Image", "Create from text", Icons.Filled.Star, 0, Modifier.weight(1f)) { vm.startPrompt("Describe an image concept I can refine:\n") }
            HubCard("Vision", "Analyze images", Icons.Filled.Search, 1, Modifier.weight(1f)) { vm.plusOpen = true; vm.go(Screen.Chat) }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubCard("Documents", "Chat with files", Icons.Filled.List, 2, Modifier.weight(1f)) { vm.plusOpen = true; vm.go(Screen.Chat) }
            HubCard("Web Search", "Live information", Icons.Filled.Home, 3, Modifier.weight(1f)) { vm.go(Screen.SearchService) }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubCard("Speech", "Voice in and out", Icons.Filled.Notifications, 0, Modifier.weight(1f)) { vm.go(Screen.Speech) }
            HubCard("Prompts", "Ready templates", Icons.Filled.Edit, 1, Modifier.weight(1f)) { vm.go(Screen.Prompts) }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubCard("Playground", "Test models", Icons.Filled.Build, 2, Modifier.weight(1f)) { vm.go(Screen.ModelSettings) }
            HubCard("MCP", "Tool servers", Icons.Filled.Settings, 3, Modifier.weight(1f)) { vm.go(Screen.Mcp) }
        }
    }
}

@Composable
private fun ProfilePage(vm: AriAiViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(CardBg).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Accent, Color(0xFFB4A8FF)))))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(vm.userName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink, modifier = Modifier.clickable {
                    vm.setUser(if (vm.userName == "User") "Ari" else "User")
                })
                Text("Personal workspace", color = Mute, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        HomeRow("Appearance", vm.colorMode, Icons.Filled.Star) { vm.go(Screen.Theme) }
        HomeRow("Providers", "Keys and models", Icons.Filled.Person) { vm.go(Screen.Providers) }
        HomeRow("Chat settings", "Stream, haptics", Icons.Filled.Settings) { vm.go(Screen.Preferences) }
        HomeRow("Privacy", "Keys stay on device", Icons.Filled.Info) { vm.go(Screen.Privacy) }
        HomeRow("About", "AriAi 1.1", Icons.Filled.Info) { vm.go(Screen.About) }
    }
}

@Composable
private fun OnboardPage(vm: AriAiViewModel) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("AriAi", color = Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Your AI, your keys.", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 28.sp)
        Spacer(Modifier.height(18.dp))
        Glass(Modifier.fillMaxWidth().clickable { vm.go(Screen.Providers) }, 22) {
            Text("1  Key", fontWeight = FontWeight.Bold, color = Accent)
            Text("Paste your API key on a provider card.", color = Mute)
        }
        Spacer(Modifier.height(10.dp))
        Glass(Modifier.fillMaxWidth().clickable { vm.providerSheet = true; vm.go(Screen.Chat) }, 22) {
            Text("2  Model", fontWeight = FontWeight.Bold, color = Accent)
            Text("Pick gpt, Claude, or Gemini.", color = Mute)
        }
        Spacer(Modifier.height(10.dp))
        Glass(Modifier.fillMaxWidth(), 22) {
            Text("3  Hello", fontWeight = FontWeight.Bold, color = Accent)
            Text("Send one sentence. Tokens stream in.", color = Mute)
        }
        Spacer(Modifier.height(20.dp))
        PrimaryBtn(if (vm.configured) "Continue" else "Add a provider") { vm.finishOnboard() }
        Text("Privacy", color = Link, modifier = Modifier.padding(top = 12.dp).clickable { vm.go(Screen.Privacy) })
    }
}

@Composable
private fun ChatPage(vm: AriAiViewModel) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Icons.Filled.ArrowBack) { vm.go(Screen.Home) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Chat", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text(vm.selectedProvider?.model?.ifBlank { vm.chatModel } ?: "Pick a model", color = Accent, fontSize = 12.sp, modifier = Modifier.clickable { vm.providerSheet = true })
            }
            IconBtn(Icons.Filled.Add) { vm.openNewChat() }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val msgs = vm.current?.messages.orEmpty()
            if (msgs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GlassSurface(Modifier.padding(28.dp), 28, emphasized = true) {
                        Text("A quiet glass room", color = Accent, fontWeight = FontWeight.SemiBold)
                        Text("Write below. Replies stream in softly.", color = Mute, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(msgs, key = { it.id }) { Bubble(it) }
                    if (vm.sending) item {
                        Text("Thinking… tap to stop", color = Mute, fontSize = 13.sp, modifier = Modifier.padding(8.dp).clickable { vm.stop() })
                    }
                    if (!vm.sending && msgs.any { it.role == ChatMessage.Role.User }) {
                        item {
                            Row {
                                Text("Edit last", color = Link, modifier = Modifier.clickable { vm.editLastUser() }.padding(8.dp))
                                if (msgs.any { it.role == ChatMessage.Role.Assistant }) {
                                    Text("Regenerate", color = Link, modifier = Modifier.clickable { vm.regenerate() }.padding(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        if (vm.pendingAttach != null) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(vm.pendingAttach!!, color = Accent, fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.Close, null, Modifier.size(16.dp).clickable { vm.pendingAttach = null }, tint = Mute)
            }
        }
        Composer(vm)
    }
}

@Composable
private fun Composer(vm: AriAiViewModel) {
    GlassSurface(Modifier.padding(12.dp), 28, emphasized = true) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Chat", "Research", "Create", "Code", "Analyze", "Agent").forEach { mode ->
                Pill(mode, vm.composerMode == mode) { vm.composerMode = mode }
            }
        }
        BasicTextField(
            value = vm.input,
            onValueChange = { v ->
                if (vm.flags["send_enter"] == true && v.endsWith("\n")) {
                    vm.input = v.trimEnd()
                    vm.send()
                } else vm.input = v
            },
            textStyle = TextStyle(color = Ink, fontSize = 16.sp, textAlign = TextAlign.Start),
            cursorBrush = SolidColor(Accent),
            keyboardOptions = KeyboardOptions(imeAction = if (vm.flags["send_enter"] == true) ImeAction.Send else ImeAction.Default),
            keyboardActions = KeyboardActions(onSend = { vm.send() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            decorationBox = { inner ->
                if (vm.input.isEmpty()) Text("Ask anything...", color = Mute, fontSize = 16.sp)
                inner()
            }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            val can = vm.input.isNotBlank() || vm.pendingAttach != null
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(if (vm.sending) DangerInk else if (can) Accent else Color(0xFFD8D9DE))
                    .clickable {
                        if (vm.sending) vm.stop() else if (can) vm.send()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(if (vm.sending) "■" else "↑", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            IconBtn(Icons.Filled.Add) { vm.plusOpen = true }
            Spacer(Modifier.weight(1f))
            IconBtn(Icons.Filled.Person) { vm.providerSheet = true }
        }
    }
}

@Composable
private fun Bubble(m: ChatMessage) {
    val mine = m.role == ChatMessage.Role.User
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(20.dp))
                .background(if (mine) Accent.copy(alpha = 0.18f) else CardBg.copy(alpha = 0.8f))
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .padding(12.dp)
        ) {
            if (m.attachmentName != null) Text(m.attachmentName, color = Accent, fontSize = 12.sp)
            m.imageBase64?.let { b64 ->
                val bmp = remember(b64) {
                    try {
                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (_: Exception) { null }
                }
                bmp?.let {
                    androidx.compose.foundation.Image(it.asImageBitmap(), null, Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).height(160.dp))
                }
            }
            MarkdownText(m.text, light = true)
            if (!mine && m.text.isNotBlank()) {
                val ctx = LocalContext.current
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Copy", color = Link, fontSize = 12.sp, modifier = Modifier.clickable {
                        val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("ariai", m.text))
                    })
                    Text("Share", color = Link, fontSize = 12.sp, modifier = Modifier.clickable {
                        val i = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, m.text)
                        ctx.startActivity(android.content.Intent.createChooser(i, "Share"))
                    })
                }
            }
        }
    }
}

@Composable
private fun PlusSheet(vm: AriAiViewModel, onPhoto: () -> Unit, onCamera: () -> Unit, onFile: () -> Unit) {
    Overlay({ vm.plusOpen = false }) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Page).padding(20.dp)
        ) {
            Handle()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Glyph(Icons.Filled.Email, 1, Modifier.size(64.dp), onFile)
                    Text("File", color = Ink, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Glyph(Icons.Filled.Star, 0, Modifier.size(64.dp), onPhoto)
                    Text("Gallery", color = Ink, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Glyph(Icons.Filled.Add, 2, Modifier.size(64.dp), onCamera)
                    Text("Lens", color = Ink, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            SheetRow("Extensions", Icons.Filled.Build) { vm.go(Screen.Extensions) }
            Spacer(Modifier.height(8.dp))
            SheetRow("Compress History", Icons.Filled.List) { vm.compressHistory() }
        }
    }
}

@Composable
private fun PlusTile(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(Chip), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Ink, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Ink, fontSize = 13.sp)
    }
}

@Composable
private fun ProviderSheet(vm: AriAiViewModel) {
    Overlay({ vm.providerSheet = false }) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.72f).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Page).padding(20.dp)
        ) {
            Handle()
            SearchBar("Enter model name to search", vm.modelQuery) { vm.modelQuery = it }
            Spacer(Modifier.height(16.dp))
            val models = vm.providers.flatMap { p ->
                val ids = (if (p.models.isEmpty()) listOf(p.model).filter { it.isNotBlank() } else p.models)
                ids.map { m -> p to m }
            }.filter { (p, m) ->
                val q = vm.modelQuery
                q.isBlank() || p.name.contains(q, true) || m.contains(q, true)
            }
            if (vm.providers.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                    Text("No available AI providers, please add in settings", color = Mute, fontSize = 15.sp, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(models, key = { it.first.id + it.second }) { (p, m) ->
                        CardRow(m, p.name, Icons.Filled.Person, selected = p.id == vm.selectedProviderId && p.model == m) {
                            vm.selectProvider(p.id)
                            vm.selectModel(m)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Drawer(vm: AriAiViewModel) {
    Overlay({ vm.drawerOpen = false }) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(CardBg)
                .padding(18.dp)
        ) {
            Handle()
            Text(vm.userName, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
            Text(vm.greeting, color = Mute, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            HomeRow("Search chats", "Find a thread", Icons.Filled.Search) { vm.go(Screen.SearchChats) }
            HomeRow("History", "All conversations", Icons.Filled.List) { vm.go(Screen.ChatHistory) }
            HomeRow("New chat", "Blank thread", Icons.Filled.Add) { vm.openNewChat() }
            Spacer(Modifier.height(8.dp))
            val filtered = vm.conversations
            if (filtered.isEmpty()) {
                Text("No chats yet", color = Mute, modifier = Modifier.padding(12.dp))
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(filtered, key = { it.id }) { c ->
                        HomeRow(c.title, c.preview.take(60), Icons.Filled.Email) { vm.openConv(c.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(vm: AriAiViewModel) {
    PageScaffold("Settings", onBack = { vm.go(Screen.Home) }) {
        if (!vm.configured) {
            Glass(Modifier.fillMaxWidth(), 20) {
                Text("Keys still sleeping", fontWeight = FontWeight.SemiBold, color = DangerInk)
                Text("Drop an API key on a provider card to wake the models.", color = DangerInk, fontSize = 13.sp)
                Text("Open keys", color = Link, modifier = Modifier.clickable { vm.go(Screen.Providers) }.padding(top = 8.dp))
            }
            Spacer(Modifier.height(10.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubCard("Keys", "Providers", Icons.Filled.Person, 0, Modifier.weight(1f)) { vm.go(Screen.Providers) }
            HubCard("Look", vm.colorMode, Icons.Filled.Star, 1, Modifier.weight(1f)) { vm.go(Screen.Theme) }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubCard("Voice", "Speech", Icons.Filled.Notifications, 2, Modifier.weight(1f)) { vm.go(Screen.Speech) }
            HubCard("Pulse", "Stats", Icons.Filled.List, 3, Modifier.weight(1f)) { vm.go(Screen.Statistics) }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubCard("Agents", "Personas", Icons.Filled.Person, 3, Modifier.weight(1f)) { vm.go(Screen.Assistant) }
            HubCard("Vault", "Backup", Icons.Filled.Send, 0, Modifier.weight(1f)) { vm.go(Screen.Backup) }
        }
        Shutter("More rooms", "Toggles, logs, extras", Icons.Filled.Build, false) {
            SettingRow("Preferences", "Input and haptics", Icons.Filled.Settings) { vm.go(Screen.Preferences) }
            SettingRow("Default models", "Chat slots", Icons.Filled.Star) { vm.go(Screen.ModelSettings) }
            SettingRow("Search", "DuckDuckGo", Icons.Filled.Home) { vm.go(Screen.SearchService) }
            SettingRow("MCP", "Tools", Icons.Filled.Build) { vm.go(Screen.Mcp) }
            SettingRow("Web flag", "", Icons.Filled.Settings) { vm.go(Screen.WebServer) }
            SettingRow("Logs", "", Icons.Filled.List) { vm.go(Screen.Logs) }
            SettingRow("About", "v1.1", Icons.Filled.Info) { vm.go(Screen.About) }
            SettingRow("Privacy", "Keys stay on device", Icons.Filled.Info) { vm.go(Screen.Privacy) }
        }
    }
}

@Composable
private fun PrefsPage(vm: AriAiViewModel) {
    PageScaffold("Preferences", onBack = { vm.go(Screen.Settings) }) {
        Group {
            SettingRow("Theme", "Dynamic color, theme, AMOLED dark mode", Icons.Filled.Star) { vm.go(Screen.Theme) }
            SettingRow("Notifications", "Update alerts, message generation notifications", Icons.Filled.Notifications) { vm.go(Screen.Notifications) }
            SettingRow("General", "Interaction behavior, scrolling, input settings", Icons.Filled.Settings) { vm.go(Screen.General) }
            SettingRow("UI Preferences", "Message display, fonts, code blocks", Icons.Filled.Settings) { vm.snack = "UI preferences saved" }
            SettingRow("Network", "User-Agent and network request settings", Icons.Filled.Home) { vm.snack = "Network defaults" }
        }
    }
}

@Composable
private fun GeneralPage(vm: AriAiViewModel) {
    PageScaffold("General", onBack = { vm.go(Screen.Preferences) }) {
        Group {
            ToggleRow("New chat on launch", "Create a new conversation when the app starts", "new_chat_launch", vm)
            ToggleRow("Send on Enter", "Press Enter to send message instead of adding a new line", "send_enter", vm)
            ToggleRow("Show Message Jumper", "Display quick jump buttons on the right when scrolling", "jumper", vm)
            ToggleRow("Message Jumper Position", "Move Message Jumper to left side", "jumper_left", vm)
            ToggleRow("Auto Scroll", "Automatically scroll to the bottom when AI is generating", "autoscroll", vm)
            ToggleRow("Use App Icon-Style Loading Indicator", "Use the animated app icon instead of circular loading", "icon_loading", vm)
            ToggleRow("Enable Blur Effect", "Enable blur effect on chat input bar", "blur", vm)
            ToggleRow("Message Generation Haptic Effect", "Enable haptic feedback when messages are generated", "haptic", vm)
            ToggleRow("Skip Image Editing", "Skip crop interface after importing images", "skip_crop", vm)
            ToggleRow("Paste Long Text as File", "When pasting text exceeds the threshold, save as file", "paste_file", vm)
            ToggleRow("Volume Key Page Scroll", "Scroll pages with volume keys", "volume_scroll", vm)
        }
        SectionLabel("TTS Settings")
        Group {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.End) {
                Text("Default playback speed", fontWeight = FontWeight.Medium, color = Ink)
                Text("Applied locally to every TTS provider", color = Mute, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("x${"%.1f".format(0.5 + vm.ttsSpeed * 0.25)}", color = Mute, fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    Row {
                        (0..10).forEach { i ->
                            Box(
                                Modifier.padding(2.dp).size(10.dp).clip(CircleShape)
                                    .background(if (i <= vm.ttsSpeed) AccentSoft else Chip)
                                    .clickable { vm.ttsSpeed = i; vm.store.setInt("tts_speed", i) }
                            )
                        }
                    }
                }
            }
            ToggleRow("TTS Read Quoted Content Only", "TTS will only read content within quotation marks", "tts_quotes", vm)
            ToggleRow("TTS Skip Bracketed Content", "TTS will not read content inside brackets", "tts_brackets", vm)
        }
    }
}

@Composable
private fun NotifPage(vm: AriAiViewModel) {
    PageScaffold("Notifications", onBack = { vm.go(Screen.Preferences) }) {
        Group {
            SettingRow("Show Updates", "Update reminders are enabled", Icons.Filled.Notifications) { vm.snack = "Updates on" }
            ToggleRow("Enable notification after message generated", "Show a notification when a message is generated if the app is not in the foreground", "notif_gen", vm)
        }
    }
}

@Composable
private fun ThemePage(vm: AriAiViewModel) {
    PageScaffold("Color Mode", onBack = { vm.go(Screen.Settings) }) {
        Group {
            listOf("System", "Light", "Dark").forEach { mode ->
                SettingRow(mode, if (vm.colorMode == mode) "Selected" else "", Icons.Filled.Star) {
                    vm.colorMode = mode
                    vm.store.setStr("color_mode", mode)
                }
            }
        }
    }
}

@Composable
private fun AssistantPage(vm: AriAiViewModel) {
    var q by remember { mutableStateOf("") }
    PageScaffold("Assistant Settings", onBack = { vm.go(Screen.Settings) }, extra = {
        IconBtn(Icons.Filled.Add) { vm.addAssistant("Default Assistant") }
    }) {
        SearchBar("Search assistants", q) { q = it }
        vm.assistants.filter { it.name.contains(q, true) }.forEach { a ->
            var prompt by remember(a.id) { mutableStateOf(a.prompt) }
            CardRow(a.name, a.prompt.take(40), Icons.Filled.MoreVert, leading = {
                Box(Modifier.size(36.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Accent, Color(0xFFC45C26)))))
            }) {
                vm.selectedAssistantId = a.id
                vm.store.setStr("sel_assistant", a.id)
                vm.go(Screen.Chat)
            }
            Field("System prompt", prompt) { prompt = it }
            Text("Save prompt", color = Link, modifier = Modifier.clickable { vm.updateAssistant(a.copy(prompt = prompt)) }.padding(8.dp))
        }
    }
}

@Composable
private fun ExtPage(vm: AriAiViewModel) {
    PageScaffold("Extensions", onBack = { vm.go(Screen.Settings) }) {
        SectionLabel("Extensions")
        Group {
            SettingRow("Quick Messages", "Manage shared quick message templates", Icons.Filled.Star) { vm.go(Screen.QuickMessages) }
            SettingRow("Prompts", "Manage and use custom prompts", Icons.Filled.Info) { vm.go(Screen.Prompts) }
            SettingRow("Agent Skills", "Manage skill packages for AI to load on demand", Icons.Filled.Build) { vm.go(Screen.Skills) }
            SettingRow("Workspace", "Manage local working directories accessible by Agent", Icons.Filled.Home) { vm.go(Screen.Workspace) }
        }
    }
}

@Composable
private fun ModelCatalogPage(vm: AriAiViewModel) {
    var addOpen by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf("Basic Settings") }
    var providerId by remember { mutableStateOf(vm.selectedProviderId) }
    var modelId by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var modelType by remember { mutableStateOf("Chat") }
    var inputImage by remember { mutableStateOf(false) }
    var outputImage by remember { mutableStateOf(false) }
    var abilities by remember { mutableStateOf("") }
    var providerOverride by remember { mutableStateOf("") }
    var headers by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var tools by remember { mutableStateOf(setOf<String>()) }

    PageScaffold("Models", onBack = { vm.go(Screen.Models) }, extra = {
        IconBtn(Icons.Filled.Add) { addOpen = true; tab = "Basic Settings" }
    }) {
        Text("Model profiles", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text("Define capabilities and request settings independently from providers.", color = Mute, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))

        if (vm.modelProfiles.isEmpty()) {
            GlassSurface(Modifier.fillMaxWidth(), 20, emphasized = true) {
                Text("No custom model profiles yet.", color = Ink, fontWeight = FontWeight.Medium)
                Text("Use + to create a model with its own capabilities and tools.", color = Mute, fontSize = 13.sp)
            }
        } else {
            vm.modelProfiles.forEach { m ->
                val provider = vm.providers.find { it.id == m.providerId }
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(CardBg).border(1.dp, Chip, RoundedCornerShape(20.dp))
                        .padding(15.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(AccentSoft), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Star, null, tint = Accent, modifier = Modifier.size(21.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.displayName, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(m.modelId, color = Mute, fontSize = 12.sp)
                            Text(provider?.name ?: "Custom provider", color = Link, fontSize = 12.sp)
                        }
                        Text(m.modelType, color = Accent, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        m.inputModalities.forEach { Text("in:$it", color = Ink, fontSize = 11.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Chip).padding(horizontal = 8.dp, vertical = 5.dp)) }
                        m.outputModalities.forEach { Text("out:$it", color = Ink, fontSize = 11.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Chip).padding(horizontal = 8.dp, vertical = 5.dp)) }
                        m.abilities.forEach { Text(it, color = Link, fontSize = 11.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(AccentSoft).padding(horizontal = 8.dp, vertical = 5.dp)) }
                    }
                    if (m.builtInTools.isNotEmpty()) {
                        Text("Tools: " + m.builtInTools.joinToString(", "), color = Mute, fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp))
                    }
                    Text("Remove", color = DangerInk, modifier = Modifier.align(Alignment.End).clickable { vm.deleteModelProfile(m.id) }.padding(top = 7.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (addOpen) {
        Overlay({ addOpen = false }) {
            Column(
                Modifier.fillMaxWidth().fillMaxHeight(0.88f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Page).padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Handle()
                Text("Add Model", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth()) {
                    Tab("Built-in Tools", tab == "Built-in Tools") { tab = "Built-in Tools" }
                    Tab("Advanced Settings", tab == "Advanced Settings") { tab = "Advanced Settings" }
                    Tab("Basic Settings", tab == "Basic Settings") { tab = "Basic Settings" }
                }
                Spacer(Modifier.height(14.dp))

                when (tab) {
                    "Basic Settings" -> {
                        Field("Model ID", modelId) { modelId = it }
                        Field("Model Display Name", displayName) { displayName = it }
                        Label("Provider")
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            vm.providers.forEach { p ->
                                Pill(p.name, providerId == p.id) { providerId = p.id }
                            }
                        }
                        Label("Model Type")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Embedding", "Image", "Chat").forEach { t -> Pill(t, modelType == t) { modelType = t } }
                        }
                        Label("Input Modality")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill("Image", inputImage) { inputImage = !inputImage }
                            Pill("Text", !inputImage) { inputImage = false }
                        }
                        Label("Output Modality")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill("Image", outputImage) { outputImage = !outputImage }
                            Pill("Text", !outputImage) { outputImage = false }
                        }
                        Label("Abilities")
                        Field("Vision, Reasoning, Function Calling...", abilities) { abilities = it }
                    }
                    "Advanced Settings" -> {
                        Label("Provider Override")
                        Text("Optional endpoint/provider settings for this specific model.", color = Mute, fontSize = 13.sp)
                        Field("Base URL override", providerOverride) { providerOverride = it }
                        Label("Custom Headers")
                        Field("JSON headers", headers) { headers = it }
                        Label("Custom Body")
                        Field("JSON body", body) { body = it }
                    }
                    "Built-in Tools" -> {
                        Label("Built-in Tools")
                        Text("Tools are capabilities supplied by the model API. They can be enabled per model profile.", color = Mute, fontSize = 13.sp)
                        listOf(
                            "Google Search",
                            "URL Context",
                            "Code Execution",
                            "Google Maps",
                            "File Search"
                        ).forEach { tool ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBg)
                                    .clickable { tools = if (tool in tools) tools - tool else tools + tool }
                                    .padding(15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(tool, color = Ink, fontWeight = FontWeight.Medium)
                                    Text("Enable when supported by the selected provider/model.", color = Mute, fontSize = 12.sp)
                                }
                                if (tool in tools) Icon(Icons.Filled.Check, null, tint = Accent)
                            }
                            Spacer(Modifier.height(7.dp))
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text("Cancel", color = Link, modifier = Modifier.clickable { addOpen = false }.padding(12.dp))
                    Text(
                        "Add",
                        color = if (modelId.isBlank() || providerId.isBlank()) Mute else Accent,
                        modifier = Modifier.clickable(enabled = modelId.isNotBlank() && providerId.isNotBlank()) {
                            vm.addModelProfile(
                                providerId,
                                modelId,
                                displayName,
                                modelType,
                                if (inputImage) listOf("Image", "Text") else listOf("Text"),
                                if (outputImage) listOf("Image", "Text") else listOf("Text"),
                                abilities.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                providerOverride,
                                headers,
                                body,
                                tools.toList()
                            )
                            addOpen = false
                            modelId = ""; displayName = ""; abilities = ""; providerOverride = ""; headers = ""; body = ""; tools = emptySet()
                        }.padding(12.dp)
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun ModelPage(vm: AriAiViewModel) {
    PageScaffold("Model Settings", onBack = { vm.go(Screen.Settings) }) {
        SectionLabel("Chat Model")
        ModelPick("Chat Model", "Global default chat model", vm.chatModel, vm) { vm.pickSlot("chat", it) }
        SectionLabel("Fast Model")
        ModelPick("Fast Model", "Model used for titles, chat suggestions, and other fast tasks", vm.fastModel, vm) { vm.pickSlot("fast", it) }
        CardRow("Thinking Budget", "Reasoning", Icons.Filled.Star) { vm.snack = "Thinking budget" }
        ToggleRow("Enable Chat Suggestions", "", "suggestions", vm)
        SectionLabel("Translation Model")
        ModelPick("Translation Model", "The model used for translation features", vm.translateModel, vm) { vm.pickSlot("tr", it) }
        SectionLabel("OCR Model")
        ModelPick("OCR Model", "Model used for optical character recognition on images", vm.ocrModel, vm) { vm.pickSlot("ocr", it) }
        SectionLabel("Compress Model")
        ModelPick("Compress Model", "Model for compressing conversation history", vm.compressModel, vm) { vm.pickSlot("cmp", it) }
    }
}

@Composable
private fun ModelPick(title: String, hint: String, id: String, vm: AriAiViewModel, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    CardRow(title, if (id.isBlank()) "Select Model" else vm.modelName(id), Icons.Filled.Person) { open = !open }
    Text(hint, color = Mute, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp), textAlign = TextAlign.End)
    if (open) {
        val models = vm.allModels()
        if (models.isEmpty()) Text("Add a provider and fetch models first", color = Mute, modifier = Modifier.padding(8.dp))
        models.forEach { m ->
            CardRow(m, "", Icons.Filled.Check) { onPick(m); open = false }
        }
    }
}

@Composable
private fun ProvidersPage(vm: AriAiViewModel) {
    var expandedProvider by remember { mutableStateOf<String?>(null) }
    var providerQuery by remember { mutableStateOf("") }
    var addOpen by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://api.openai.com/v1") }
    var model by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("openai") }

    PageScaffold("Providers", onBack = { vm.go(Screen.Home) }, extra = {
        IconBtn(Icons.Filled.Add) { addOpen = true }
    }) {
        SearchBar("Search providers", providerQuery) { providerQuery = it }

        Spacer(Modifier.height(12.dp))

        vm.providers
            .filter { it.name.contains(providerQuery, ignoreCase = true) }
            .forEach { p ->
                val expanded = expandedProvider == p.id
                var providerKey by remember(p.id) { mutableStateOf(p.apiKey) }

                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBg)
                        .border(1.dp, if (expanded) Accent else Chip, RoundedCornerShape(20.dp))
                ) {
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { expandedProvider = if (expanded) null else p.id }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(46.dp).clip(CircleShape).background(
                                when (p.kind) {
                                    "anthropic" -> Color(0xFFD4A574)
                                    "gemini" -> Color(0xFF4285F4)
                                    "deepseek" -> Color(0xFF4C6FFF)
                                    "groq" -> Color(0xFFF55036)
                                    else -> AccentSoft
                                }
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                p.name.take(1).uppercase(),
                                color = if (p.kind == "openai" || p.kind == "anthropic" || p.kind == "gemini" || p.kind == "deepseek" || p.kind == "groq") Color.White else Ink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 17.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (p.models.isNotEmpty()) "models" else "models not loaded",
                                    color = Link,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                        .background(AccentSoft)
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    if (p.enabled && p.apiKey.isNotBlank()) "Enabled" else "Disabled",
                                    color = if (p.enabled && p.apiKey.isNotBlank()) Color(0xFF2E7D32) else Color(0xFF9A5A00),
                                    fontSize = 12.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                        .background(if (p.enabled && p.apiKey.isNotBlank()) Color(0xFFC8F3CF) else Color(0xFFFFE2B8))
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(if (expanded) "⌃" else "›", color = Mute, fontSize = 22.sp)
                    }

                    if (expanded) {
                        Column(
                            Modifier.fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        ) {
                            Text("Models", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(Modifier.height(6.dp))

                            if (p.models.isEmpty()) {
                                Text("No models loaded yet.", color = Mute, fontSize = 12.sp)
                            } else {
                                p.models.forEach { m ->
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (m == p.model) AccentSoft else Color.Transparent)
                                            .clickable { vm.selectProvider(p.id); vm.selectModel(m) }
                                            .padding(horizontal = 10.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (m == p.model) {
                                            Icon(Icons.Filled.Check, null, tint = Accent, modifier = Modifier.size(17.dp))
                                            Spacer(Modifier.width(7.dp))
                                        }
                                        Text(m, color = if (m == p.model) Accent else Ink, fontSize = 13.sp)
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text("Use", color = Link, modifier = Modifier.clickable { vm.selectProvider(p.id) }.padding(8.dp))
                                Text("Fetch", color = Link, modifier = Modifier.clickable { vm.fetchModels(p.id) }.padding(8.dp))
                                Text("Test", color = Link, modifier = Modifier.clickable { vm.testProvider(p.id) }.padding(8.dp))
                            }

                            Text("API key", color = Mute, fontSize = 12.sp)
                            Field("API key", providerKey) { providerKey = it }
                            if (providerKey != p.apiKey) {
                                Text(
                                    "Save key",
                                    color = Link,
                                    modifier = Modifier.clickable {
                                        vm.updateProvider(p.copy(apiKey = providerKey, enabled = providerKey.isNotBlank()))
                                        vm.snack = "Key saved"
                                    }.padding(8.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

        if (vm.providers.none { it.name.contains(providerQuery, ignoreCase = true) }) {
            Text("No providers found.", color = Mute, modifier = Modifier.padding(16.dp))
        }

        if (addOpen) {
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBg).padding(14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Add provider", color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("Close", color = Link, modifier = Modifier.clickable { addOpen = false }.padding(8.dp))
                }
                Field("Name", name) { name = it }
                Field("Base URL", url) { url = it }
                Field("Model id (optional)", model) { model = it }
                Field("API key", key) { key = it }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Pill("OpenAI", kind == "openai") { kind = "openai" }
                    Spacer(Modifier.width(6.dp))
                    Pill("Anthropic", kind == "anthropic") { kind = "anthropic" }
                    Spacer(Modifier.width(6.dp))
                    Pill("Gemini", kind == "gemini") { kind = "gemini" }
                }
                PrimaryBtn(if (vm.fetching) "Working…" else "Save") {
                    if (name.isNotBlank() && url.isNotBlank() && key.isNotBlank()) {
                        vm.addProvider(name, url, model, key, "", kind)
                        name = ""; model = ""; key = ""; addOpen = false
                    } else vm.snack = "Name, URL and API key are required"
                }
            }
        }
    }
}

@Composable
private fun SpeechPage(vm: AriAiViewModel) {
    PageScaffold("Speech", onBack = { vm.go(Screen.Settings) }, extra = {
        IconBtn(Icons.Filled.Add) { vm.snack = "Add speech provider" }
    }) {
        SpeechCard("System TTS", "Device TextToSpeech", "S", vm.speechId == "sys") { vm.pickSpeech("sys") }
        PrimaryBtn("Speak last reply") { vm.speakLast() }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Phone, null, tint = Mute)
                Text("Speech Recognition", color = Mute, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.clip(RoundedCornerShape(16.dp)).background(AccentSoft).padding(8.dp)) {
                    Icon(Icons.Filled.Notifications, null, tint = Accent)
                }
                Text("Text to Speech", color = Ink, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SpeechCard(title: String, sub: String, badge: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(18.dp))
            .background(if (selected) AccentSoft else CardBg).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(sub, color = Mute, fontSize = 13.sp)
            if (selected) {
                Box(Modifier.padding(top = 6.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFC8E6C9)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("Selected", color = Color(0xFF2E7D32), fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(36.dp).clip(CircleShape).background(if (badge == "S") Color(0xFF90CAF9) else Color(0xFF5C6BC0)), contentAlignment = Alignment.Center) {
            Text(badge, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun McpPage(vm: AriAiViewModel) {
    PageScaffold("MCP", onBack = { vm.go(Screen.Settings) }, extra = {
        Row {
            IconBtn(Icons.Filled.Add) {
                vm.mcpDraft = McpServer(AppStore.id(), "", "", true, "http", "")
            }
            IconBtn(Icons.Filled.Send) { vm.mcpImport = true }
        }
    }) {
        Text("Names are saved and injected into the system prompt. This build does not speak the MCP wire protocol yet.", color = Mute, fontSize = 13.sp)
        if (vm.mcp.isEmpty()) Text("No MCP servers. Tap + to add.", color = Mute)
        vm.mcp.forEach { s ->
            CardRow(s.name.ifBlank { "Unnamed" }, s.url, Icons.Filled.Build) { vm.mcpDraft = s }
        }
    }
}

@Composable
private fun McpEditor(vm: AriAiViewModel, s: McpServer) {
    var name by remember { mutableStateOf(s.name) }
    var url by remember { mutableStateOf(s.url) }
    var enabled by remember { mutableStateOf(s.enabled) }
    var transport by remember { mutableStateOf(s.transport) }
    var headers by remember { mutableStateOf(s.headers) }
    Overlay({ vm.mcpDraft = null }) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Page).padding(20.dp).verticalScroll(rememberScrollState())) {
            Handle()
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f)) { Tab("Tools", false) {} }
                Box(Modifier.weight(1f)) { Tab("Basic Settings", true) {} }
            }
            Spacer(Modifier.height(12.dp))
            Label("Enable")
            Text("Whether to enable this MCP server", color = Mute, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            Switch(enabled, { enabled = it }, colors = SwitchDefaults.colors(checkedTrackColor = Accent))
            Label("Name")
            Text("Display name for the MCP server", color = Mute, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            Field("Name", name) { name = it }
            Label("Transport Type")
            Text("Select the transport protocol type for the MCP server", color = Mute, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                Pill("SSE", transport == "sse") { transport = "sse" }
                Spacer(Modifier.width(8.dp))
                Pill("Streamable HTTP", transport != "sse") { transport = "http" }
            }
            Label("Server URL")
            Text("URL address for Streamable HTTP server", color = Mute, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            Field("URL", url) { url = it }
            Label("Custom Headers")
            Field("Headers", headers) { headers = it }
            Text("Save", color = Link, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable {
                vm.saveMcp(s.copy(name = name, url = url, enabled = enabled, transport = transport, headers = headers))
            }.padding(8.dp))
        }
    }
}

@Composable
private fun ImportMcp(vm: AriAiViewModel) {
    Overlay({ vm.mcpImport = false }) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.75f).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Page).padding(20.dp)) {
            Handle()
            Text("Import MCP Server", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            Text("Paste MCP Server JSON config, supports standard mcpServers format", color = Mute, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f).fillMaxWidth().border(1.dp, Chip, RoundedCornerShape(12.dp)).padding(12.dp)) {
                BasicTextField(
                    value = vm.importJson,
                    onValueChange = { vm.importJson = it },
                    textStyle = TextStyle(color = Ink, fontSize = 14.sp),
                    modifier = Modifier.fillMaxSize(),
                    decorationBox = { inner ->
                        if (vm.importJson.isEmpty()) Text("{ { ... } :\"mcpServers\" }", color = Mute)
                        inner()
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Accent).clickable { vm.importMcp(vm.importJson) }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Text("Import", color = Color.White, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(16.dp))
                Text("Cancel", color = Ink, modifier = Modifier.clickable { vm.mcpImport = false })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsPage(vm: AriAiViewModel) {
    PageScaffold("Statistics", onBack = { vm.go(Screen.Chat) }) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBg).padding(16.dp)) {
            Text("Chat Heatmap", fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(7 * 18) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(Chip))
                }
            }
            Text("More  ● ● ○   Less", color = Mute, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Total Messages", vm.store.int("msg_count").toString(), Icons.Filled.Email, Modifier.weight(1f))
            StatCard("Total Conversations", vm.conversations.size.toString(), Icons.Filled.List, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Output Tokens", vm.store.int("out_tokens").toString(), Icons.Filled.Star, Modifier.weight(1f))
            StatCard("Input Tokens", vm.store.int("in_tokens").toString(), Icons.Filled.Star, Modifier.weight(1f))
        }
        StatCard("App Launch Count", vm.store.int("launches").toString(), Icons.Filled.Star, Modifier.fillMaxWidth())
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(CardBg).padding(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        Icon(icon, null, tint = Accent, modifier = Modifier.size(20.dp))
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(title, color = Mute, fontSize = 13.sp)
    }
}

@Composable
private fun HistoryPage(vm: AriAiViewModel) {
    PageScaffold("Chat History", onBack = { vm.go(Screen.Chat) }) {
        if (vm.lastDeleted != null) Text("Undo delete", color = Link, modifier = Modifier.clickable { vm.undoDelete() }.padding(8.dp))
        if (vm.conversations.isEmpty()) Text("No conversations", color = Mute)
        vm.conversations.forEach { c ->
            CardRow((if (c.pinned) "★ " else "") + c.title, c.preview, Icons.Filled.Email) { vm.openConv(c.id) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(if (c.pinned) "Unpin" else "Pin", color = Link, modifier = Modifier.clickable { vm.pinConv(c.id) }.padding(8.dp))
                Text("Delete", color = DangerInk, modifier = Modifier.clickable { vm.deleteConv(c.id) }.padding(8.dp))
            }
        }
    }
}

@Composable
private fun SearchChatsPage(vm: AriAiViewModel) {
    PageScaffold("Search Chats", onBack = { vm.go(Screen.Chat) }) {
        SearchBar("Search chats", vm.chatQuery) { vm.chatQuery = it }
        vm.conversations.filter { it.title.contains(vm.chatQuery, true) || it.preview.contains(vm.chatQuery, true) }.forEach { c ->
            CardRow(c.title, c.preview, Icons.Filled.Search) { vm.openConv(c.id) }
        }
    }
}


@Composable
private fun SearchPage(vm: AriAiViewModel) {
    PageScaffold("Search Service", onBack = { vm.go(Screen.Settings) }) {
        Text("When enabled, DuckDuckGo results are added to the user message before calling the model.", color = Mute, fontSize = 13.sp)
        Field("Optional API key (unused for DuckDuckGo)", vm.searchKey) { vm.searchKey = it }
        PrimaryBtn(if (vm.searchOn) "Search is ON — tap to save" else "Enable search") {
            vm.saveSearch(!vm.searchOn, vm.searchKey)
        }
    }
}

@Composable
private fun BackupPage(vm: AriAiViewModel) {
    PageScaffold("Data Backup", onBack = { vm.go(Screen.Settings) }) {
        Text("Copy JSON to another device, or paste to restore.", color = Mute, fontSize = 13.sp)
        Field("Backup JSON", vm.backupText) { vm.backupText = it }
        PrimaryBtn("Refresh export") { vm.backupText = vm.store.exportJson() }
        PrimaryBtn("Restore from JSON") { vm.doBackupImport(vm.backupText) }
    }
}

@Composable
private fun LogsPage(vm: AriAiViewModel) {
    PageScaffold("Request Logs", onBack = { vm.go(Screen.Settings) }) {
        if (vm.logs.isEmpty()) Text("No requests yet. Send a chat or fetch models.", color = Mute)
        vm.logs.forEach { l ->
            val ok = l.status in 200..299
            GlassSurface(Modifier.fillMaxWidth().padding(vertical = 4.dp), 16, emphasized = true) {
                Text("${l.method}  ${if (ok) "OK" else "Fail"}  ${l.status}", color = if (ok) Accent else DangerInk, fontWeight = FontWeight.SemiBold)
                Text(l.url.take(80), color = Mute, fontSize = 12.sp)
                Text(l.body.take(160), color = Ink, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun QuickPage(vm: AriAiViewModel) {
    var txt by remember { mutableStateOf("") }
    PageScaffold("Quick Messages", onBack = { vm.go(Screen.Extensions) }) {
        vm.quick.forEach { q ->
            CardRow(q.text.take(40), "", Icons.Filled.Star) { vm.useQuick(q.text) }
        }
        Field("New template", txt) { txt = it }
        PrimaryBtn("Add") { if (txt.isNotBlank()) { vm.addQuick(txt); txt = "" } }
    }
}

@Composable
private fun PromptsPage(vm: AriAiViewModel) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    PageScaffold("Prompts", onBack = { vm.go(Screen.Extensions) }) {
        vm.prompts.forEach { pr ->
            CardRow(pr.title, pr.body.take(50), Icons.Filled.Info) { vm.usePrompt(pr.body) }
        }
        Field("Title", title) { title = it }
        Field("Prompt body", body) { body = it }
        PrimaryBtn("Add prompt") {
            if (title.isNotBlank()) { vm.addPrompt(title, body); title = ""; body = "" }
        }
    }
}

@Composable
private fun WebPage(vm: AriAiViewModel) {
    PageScaffold("Web Server", onBack = { vm.go(Screen.Settings) }) {
        Text("Honest note: this is only a local preference flag. There is no HTTP server in this build. Chats stay on-device.", color = Mute, fontSize = 13.sp)
        PrimaryBtn(if (vm.webOn) "Enabled" else "Enable flag") {
            vm.webOn = !vm.webOn
            vm.store.setBool("web_on", vm.webOn)
            vm.snack = if (vm.webOn) "Flag on" else "Flag off"
        }
    }
}

@Composable
private fun SimplePage(vm: AriAiViewModel, title: String, body: String) {
    PageScaffold(title, onBack = { vm.go(Screen.Settings) }) {
        Group { Text(body, color = Mute, modifier = Modifier.padding(16.dp).fillMaxWidth()) }
        PrimaryBtn("OK") { vm.go(Screen.Settings) }
    }
}

/* ——— chrome ——— */

@Composable
private fun PageScaffold(title: String, onBack: () -> Unit, extra: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(CardBg).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ArrowBack, null, tint = Ink)
            }
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f))
            extra()
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            content()
            Spacer(Modifier.height(56.dp))
        }
    }
}

@Composable
private fun Overlay(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0x66000000)).clickable(onClick = onDismiss), contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.clickable(enabled = false) {}) { content() }
    }
}

@Composable private fun Handle() {
    Box(Modifier.fillMaxWidth().padding(bottom = 12.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFC5C6CC)))
    }
}

@Composable
private fun IconBtn(icon: ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Ink, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).background(AccentSoft).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Ink, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun MenuLine(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(title, color = Ink, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Icon(icon, null, tint = Ink, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SectionLabel(t: String) {
    Text(t, color = Section, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp, end = 4.dp), textAlign = TextAlign.End)
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBg)) { content() }
}

@Composable
private fun SettingRow(title: String, sub: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            if (sub.isNotBlank()) Text(sub, color = Mute, fontSize = 13.sp)
        }
        Text("›", color = Mute, fontSize = 18.sp)
    }
}

@Composable
private fun ToggleRow(title: String, sub: String, key: String, vm: AriAiViewModel) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontWeight = FontWeight.Medium)
            if (sub.isNotBlank()) Text(sub, color = Mute, fontSize = 13.sp)
        }
        Switch(vm.flags[key] == true, { vm.toggle(key) }, colors = SwitchDefaults.colors(checkedTrackColor = Accent))
    }
}

@Composable
private fun CardRow(title: String, sub: String, icon: ImageVector, selected: Boolean = false, leading: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp))
            .background(if (selected) AccentSoft else CardBg).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Mute, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(title, color = Ink, fontWeight = FontWeight.Medium)
            if (sub.isNotBlank()) Text(sub, color = Mute, fontSize = 12.sp)
        }
        if (leading != null) {
            Spacer(Modifier.width(10.dp))
            leading()
        }
    }
}

@Composable
private fun SheetRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Chip).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(title, color = Ink, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Icon(icon, null, tint = Ink)
    }
}

@Composable
private fun SearchBar(hint: String, value: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Chip).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, null, tint = Mute)
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(color = Ink, fontSize = 15.sp, textAlign = TextAlign.End),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) Text(hint, color = Mute, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    inner()
                }
            }
        )
    }
}

@Composable
private fun Field(hint: String, value: String, onChange: (String) -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, Chip, RoundedCornerShape(14.dp)).background(CardBg).padding(14.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(color = Ink, fontSize = 15.sp, textAlign = TextAlign.Start),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(hint, color = Mute, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                inner()
            }
        )
    }
}

@Composable
private fun PrimaryBtn(t: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp).clip(RoundedCornerShape(14.dp)).background(Accent).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(t, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Label(t: String) {
    Text(t, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), textAlign = TextAlign.End)
}

@Composable
private fun Tab(t: String, on: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(t, color = if (on) Ink else Mute, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.height(2.dp).fillMaxWidth(0.4f).background(if (on) Accent else Color.Transparent))
    }
}

@Composable
private fun Pill(t: String, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(20.dp)).background(if (on) AccentSoft else Chip).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (on) Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = Accent)
        Spacer(Modifier.width(4.dp))
        Text(t, color = Ink, fontSize = 13.sp)
    }
}
