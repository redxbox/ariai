package com.ariai.app.ui.screens

import android.graphics.Color as AndroidColor
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ariai.app.data.models.WorkspaceFile
import com.ariai.app.data.repository.executeWorkspaceConsole
import com.ariai.app.data.repository.renderStaticHtmlPreview
import com.ariai.app.ui.theme.AriCard
import com.ariai.app.ui.theme.AriInk
import com.ariai.app.ui.theme.AriMuted
import com.ariai.app.ui.theme.AriPaper
import com.ariai.app.ui.theme.AriTint
import com.ariai.app.ui.theme.paperPopupModifier
import com.ariai.app.ui.theme.paperPopupShape
import com.ariai.app.util.tx
import java.io.ByteArrayInputStream

private enum class WorkspaceTab { EDITOR, PREVIEW, CONSOLE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    files: List<WorkspaceFile>,
    onBack: () -> Unit,
    onCreateFile: (String, String, (String?) -> Unit) -> Unit,
    onSaveFile: (String, String, (String?) -> Unit) -> Unit,
    onDeleteFile: (String, (String?) -> Unit) -> Unit,
    onGenerateFile: (String, String, String?, (WorkspaceFile?, String?) -> Unit) -> Unit
) {
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(files) {
        if (files.none { it.name == selectedFileName }) selectedFileName = files.firstOrNull()?.name
    }
    val selectedFile = files.firstOrNull { it.name == selectedFileName }
    var draft by remember(selectedFile?.name, selectedFile?.content) { mutableStateOf(selectedFile?.content.orEmpty()) }
    var savedDraft by remember(selectedFile?.name, selectedFile?.content) { mutableStateOf(selectedFile?.content.orEmpty()) }
    var selectedTab by remember { mutableStateOf(WorkspaceTab.EDITOR) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showGenerateDialog by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var generationFileName by remember { mutableStateOf("index.html") }
    var generationPrompt by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var consoleCommand by remember { mutableStateOf("") }
    var consoleOutput by remember { mutableStateOf(tx("Type help for available safe commands.")) }
    val isHtmlFile = selectedFile?.let { it.name.substringAfterLast('.', "").lowercase() in setOf("html", "htm") } == true

    Scaffold(
        containerColor = AriPaper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AriPaper),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = tx("Back"), tint = AriInk)
                    }
                },
                title = { Text(tx("Workspace"), color = AriInk, fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = {
                        generationFileName = selectedFile?.name ?: "index.html"
                        generationPrompt = ""
                        notice = ""
                        showGenerateDialog = true
                    }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = tx("Generate with AriAI"), tint = AriInk)
                    }
                    IconButton(onClick = { newFileName = ""; notice = ""; showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = tx("New file"), tint = AriInk)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                tx("Files stay in AriAI's private on-device workspace."),
                color = AriMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            if (files.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    files.forEach { file ->
                        FilterChip(
                            selected = file.name == selectedFileName,
                            onClick = {
                                selectedFileName = file.name
                                notice = ""
                                if (file.name.substringAfterLast('.', "").lowercase() !in setOf("html", "htm") && selectedTab == WorkspaceTab.PREVIEW) {
                                    selectedTab = WorkspaceTab.EDITOR
                                }
                            },
                            label = { Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = if (file.name == selectedFileName) ({ Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) }) else null
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                WorkspaceTab.values().forEach { tab ->
                    val enabled = tab != WorkspaceTab.PREVIEW || isHtmlFile == true
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { if (enabled) selectedTab = tab },
                        enabled = enabled,
                        label = { Text(tab.label()) },
                        leadingIcon = when (tab) {
                            WorkspaceTab.EDITOR -> ({ Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) })
                            WorkspaceTab.PREVIEW -> ({ Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp)) })
                            WorkspaceTab.CONSOLE -> null
                        }
                    )
                }
            }
            HorizontalDivider(color = AriInk.copy(alpha = 0.08f))
            when (selectedTab) {
                WorkspaceTab.EDITOR -> {
                    if (selectedFile == null) {
                        EmptyWorkspace(onCreate = { newFileName = "index.html"; notice = ""; showCreateDialog = true })
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                selectedFile.name,
                                color = AriInk,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = tx("Delete file"), tint = AriMuted)
                            }
                        }
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it; notice = "" },
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            placeholder = { Text(tx("File contents")) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, color = AriInk),
                            minLines = 12,
                            maxLines = Int.MAX_VALUE
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    onSaveFile(selectedFile.name, draft) { error ->
                                        if (error == null) {
                                            savedDraft = draft
                                            notice = tx("File saved")
                                        } else notice = error
                                    }
                                },
                                enabled = draft != savedDraft,
                                colors = ButtonDefaults.buttonColors(containerColor = AriInk, contentColor = AriPaper)
                            ) { Text(tx("Save file")) }
                            if (notice.isNotBlank()) Text(notice, color = AriMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            else if (draft == savedDraft) Text(tx("Saved"), color = AriMuted, fontSize = 12.sp)
                        }
                    }
                }
                WorkspaceTab.PREVIEW -> {
                    Text(
                        tx("HTML preview is static. JavaScript and network requests are blocked."),
                        color = AriMuted,
                        fontSize = 12.sp
                    )
                    if (selectedFile == null) {
                        Text(tx("No HTML file selected."), color = AriMuted, modifier = Modifier.padding(12.dp))
                    } else {
                        val previewFiles = remember(files, selectedFile.name, draft) {
                            files.map { if (it.name == selectedFile.name) it.copy(content = draft) else it }
                        }
                        val previewHtml = remember(previewFiles, selectedFile.name) {
                            renderStaticHtmlPreview(previewFiles.firstOrNull { it.name == selectedFile.name }, previewFiles)
                        }
                        StaticPreviewWebView(previewHtml, Modifier.fillMaxWidth().weight(1f))
                    }
                }
                WorkspaceTab.CONSOLE -> {
                    Text(
                        tx("This console can list/read files in this workspace only. It does not run shell commands or code."),
                        color = AriMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        colors = CardDefaults.cardColors(containerColor = AriCard),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            consoleOutput,
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                            color = AriInk,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = consoleCommand,
                            onValueChange = { consoleCommand = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text(tx("Command")) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                consoleOutput = runWorkspaceCommand(consoleCommand, files, consoleOutput)
                                consoleCommand = ""
                            })
                        )
                        Button(
                            onClick = {
                                consoleOutput = runWorkspaceCommand(consoleCommand, files, consoleOutput)
                                consoleCommand = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AriInk, contentColor = AriPaper)
                        ) { Text(tx("Run")) }
                    }
                }
            }
        }
    }

    if (showGenerateDialog) {
        val dialogShape = paperPopupShape(24.dp)
        AlertDialog(
            onDismissRequest = { if (!isGenerating) showGenerateDialog = false },
            modifier = paperPopupModifier(dialogShape),
            shape = dialogShape,
            containerColor = AriCard,
            title = { Text(tx("Generate code")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(tx("Uses the selected default AI provider and model."), color = AriMuted, fontSize = 12.sp)
                    OutlinedTextField(
                        value = generationFileName,
                        onValueChange = { generationFileName = it.take(64); notice = "" },
                        label = { Text(tx("File name")) },
                        singleLine = true,
                        enabled = !isGenerating
                    )
                    OutlinedTextField(
                        value = generationPrompt,
                        onValueChange = { generationPrompt = it.take(4_000); notice = "" },
                        label = { Text(tx("Describe what to build or change")) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        enabled = !isGenerating
                    )
                    if (notice.isNotBlank()) Text(notice, color = AriMuted, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isGenerating && generationPrompt.isNotBlank(),
                    onClick = {
                        val name = generationFileName.trim()
                        val existing = files.firstOrNull { it.name.equals(name, ignoreCase = true) }
                        if (existing != null && !existing.name.equals(selectedFile?.name, ignoreCase = true)) {
                            notice = tx("Select the existing file you want to replace before generating.")
                            return@TextButton
                        }
                        isGenerating = true
                        val currentContent = existing?.let { if (it.name.equals(selectedFile?.name, ignoreCase = true)) draft else it.content }
                        onGenerateFile(name, generationPrompt, currentContent) { generated, error ->
                            isGenerating = false
                            if (generated != null) {
                                selectedFileName = generated.name
                                selectedTab = WorkspaceTab.EDITOR
                                draft = generated.content
                                savedDraft = generated.content
                                notice = tx("Generated file saved.")
                                showGenerateDialog = false
                            } else {
                                notice = error ?: tx("Could not generate the file.")
                            }
                        }
                    }
                ) { Text(if (isGenerating) tx("Generating…") else tx("Generate")) }
            },
            dismissButton = {
                TextButton(enabled = !isGenerating, onClick = { showGenerateDialog = false }) { Text(tx("Cancel")) }
            }
        )
    }

    if (showCreateDialog) {
        val dialogShape = paperPopupShape(24.dp)
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            modifier = paperPopupModifier(dialogShape),
            shape = dialogShape,
            containerColor = AriCard,
            title = { Text(tx("Create file")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it.take(64) },
                        label = { Text(tx("File name")) },
                        placeholder = { Text(tx("e.g. index.html")) },
                        singleLine = true
                    )
                    if (notice.isNotBlank()) Text(notice, color = AriMuted, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newFileName.trim()
                    if (name.isBlank()) {
                        notice = tx("File name is required.")
                        return@TextButton
                    }
                    if (files.any { it.name.equals(name, ignoreCase = true) }) {
                        notice = tx("A file with this name already exists.")
                        return@TextButton
                    }
                    val initialContent = starterContent(name)
                    onCreateFile(name, initialContent) { error ->
                        if (error == null) {
                            selectedFileName = name
                            selectedTab = WorkspaceTab.EDITOR
                            draft = initialContent
                            savedDraft = initialContent
                            notice = tx("File saved")
                            showCreateDialog = false
                        } else notice = error
                    }
                }) { Text(tx("Create file")) }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text(tx("Cancel")) } }
        )
    }

    if (showDeleteDialog && selectedFile != null) {
        val dialogShape = paperPopupShape(24.dp)
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            modifier = paperPopupModifier(dialogShape),
            shape = dialogShape,
            containerColor = AriCard,
            title = { Text(tx("Delete file?")) },
            text = { Text(tx("This deletes the local file. It cannot be undone.")) },
            confirmButton = {
                TextButton(onClick = {
                    val deleting = selectedFile.name
                    onDeleteFile(deleting) { error ->
                        if (error == null) {
                            notice = tx("File deleted")
                            selectedFileName = null
                            draft = ""
                            savedDraft = ""
                        } else notice = error
                        showDeleteDialog = false
                    }
                }) { Text(tx("Delete file")) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(tx("Cancel")) } }
        )
    }
}

@Composable
private fun EmptyWorkspace(onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(tx("No project files yet."), color = AriInk, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(tx("Create an HTML, CSS, or text file to begin."), color = AriMuted, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        Button(onClick = onCreate, colors = ButtonDefaults.buttonColors(containerColor = AriInk, contentColor = AriPaper)) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(tx("New file"))
        }
    }
}

@Composable
private fun StaticPreviewWebView(html: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(AriCard),
        factory = {
            WebView(context).apply {
                setBackgroundColor(AndroidColor.WHITE)
                settings.apply {
                    javaScriptEnabled = false
                    javaScriptCanOpenWindowsAutomatically = false
                    allowFileAccess = false
                    allowContentAccess = false
                    allowFileAccessFromFileURLs = false
                    allowUniversalAccessFromFileURLs = false
                    domStorageEnabled = false
                    databaseEnabled = false
                    blockNetworkLoads = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) safeBrowsingEnabled = true
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = true
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        val scheme = request?.url?.scheme?.lowercase()
                        return if (scheme == "about" || scheme == "data") null
                        else WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                    }
                }
                loadDataWithBaseURL("about:blank", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            if (webView.tag != html) {
                webView.tag = html
                webView.loadDataWithBaseURL("about:blank", html, "text/html", "UTF-8", null)
            }
        }
    )
}

private fun WorkspaceTab.label(): String = when (this) {
    WorkspaceTab.EDITOR -> tx("Editor")
    WorkspaceTab.PREVIEW -> tx("Preview")
    WorkspaceTab.CONSOLE -> tx("Read-only console")
}

private fun starterContent(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "html", "htm" -> """<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>AriAI workspace</title>
  <style>body { font: 16px sans-serif; padding: 2rem; }</style>
</head>
<body>
  <h1>Hello, workspace</h1>
  <p>Edit this local HTML file to begin.</p>
</body>
</html>
"""
    "css" -> "body {\n  font-family: sans-serif;\n}\n"
    else -> ""
}

private fun runWorkspaceCommand(command: String, files: List<WorkspaceFile>, previousOutput: String): String {
    val trimmed = command.trim()
    if (trimmed.equals("clear", ignoreCase = true)) return ""
    if (trimmed.isBlank()) return previousOutput
    val output = executeWorkspaceConsole(trimmed, files)
    val entry = "\$ /workspace \$ $trimmed\n$output"
    return (entry + if (previousOutput.isBlank()) "" else "\n\n$previousOutput").take(24_000)
}
