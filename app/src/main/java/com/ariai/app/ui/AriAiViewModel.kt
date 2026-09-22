package com.ariai.app.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ariai.app.data.AppStore
import com.ariai.app.data.Assistant
import com.ariai.app.data.ChatMessage
import com.ariai.app.data.Conversation
import com.ariai.app.data.McpServer
import com.ariai.app.data.PromptItem
import com.ariai.app.data.Provider
import com.ariai.app.data.QuickMsg
import com.ariai.app.data.RequestLog
import com.ariai.app.data.Screen
import com.ariai.app.net.LlmClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class AriAiViewModel(app: Application) : AndroidViewModel(app) {
    val store = AppStore(app)
    private val llm = LlmClient()
    private var tts: TextToSpeech? = null

    var screen by mutableStateOf(Screen.Chat)
    var drawerOpen by mutableStateOf(false)
    var plusOpen by mutableStateOf(false)
    var providerSheet by mutableStateOf(false)
    var newMenu by mutableStateOf(false)
    var mcpImport by mutableStateOf(false)
    var mcpDraft by mutableStateOf<McpServer?>(null)

    var providers by mutableStateOf(store.providers())
    var modelProfiles by mutableStateOf(store.modelProfiles())
    var assistants by mutableStateOf(store.assistants())
    var mcp by mutableStateOf(store.mcp())
    var conversations by mutableStateOf(store.conversations())
    var prompts by mutableStateOf(store.prompts())
    var quick by mutableStateOf(store.quick())
    var current by mutableStateOf<Conversation?>(null)
    var input by mutableStateOf("")
    var sending by mutableStateOf(false)
    var pendingAttach by mutableStateOf<String?>(null)
    var pendingImage by mutableStateOf<String?>(null)
    var snack by mutableStateOf<String?>(null)
    var selectedProviderId by mutableStateOf(store.str("sel_provider"))
    var selectedAssistantId by mutableStateOf(store.str("sel_assistant", assistants.firstOrNull()?.id ?: ""))
    var modelQuery by mutableStateOf("")
    var chatQuery by mutableStateOf("")
    var importJson by mutableStateOf("")
    var userName by mutableStateOf(store.str("user_name", "User"))
    var logs by mutableStateOf<List<RequestLog>>(emptyList())
    var backupText by mutableStateOf("")
    var fetching by mutableStateOf(false)
    var searchKey by mutableStateOf(store.str("search_key"))
    var searchOn by mutableStateOf(store.bool("search_on"))
    var webOn by mutableStateOf(store.bool("web_on"))
    var lastDeleted by mutableStateOf<Conversation?>(null)
    var rtl by mutableStateOf(store.bool("rtl", false))
    var composerMode by mutableStateOf("Chat")

    val flags = mutableStateMapOf<String, Boolean>().apply {
        listOf(
            "new_chat_launch" to true,
            "send_enter" to false,
            "jumper" to true,
            "jumper_left" to false,
            "autoscroll" to true,
            "icon_loading" to true,
            "blur" to false,
            "haptic" to true,
            "skip_crop" to true,
            "paste_file" to false,
            "volume_scroll" to false,
            "tts_quotes" to false,
            "tts_brackets" to false,
            "notif_gen" to false,
            "suggestions" to true,
            "rtl" to false
        ).forEach { (k, d) -> put(k, store.bool(k, d)) }
    }

    var ttsSpeed by mutableStateOf(store.int("tts_speed", 6))
    var colorMode by mutableStateOf(store.str("color_mode", "System"))
    var speechId by mutableStateOf(store.str("speech", "sys"))
    var chatModel by mutableStateOf(store.str("chat_model"))
    var fastModel by mutableStateOf(store.str("fast_model"))
    var translateModel by mutableStateOf(store.str("tr_model"))
    var ocrModel by mutableStateOf(store.str("ocr_model"))
    var compressModel by mutableStateOf(store.str("cmp_model"))

    val selectedProvider get() = providers.find { it.id == selectedProviderId }
    val selectedAssistant get() = assistants.find { it.id == selectedAssistantId } ?: assistants.firstOrNull()
    val configured get() = providers.any { it.apiKey.isNotBlank() && it.baseUrl.isNotBlank() }

    val greeting: String
        get() {
            val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when {
                h < 12 -> "Good morning"
                h < 18 -> "Good afternoon"
                else -> "Good evening"
            }
        }

    init {
        store.inc("launches")
        tts = TextToSpeech(app) { st ->
            if (st == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
        }
        if (!store.bool("onboard_done") && !configured) screen = Screen.Onboarding
        else screen = Screen.Home
    }

    fun finishOnboard() {
        store.setBool("onboard_done", true)
        go(Screen.Home)
    }

    fun startPrompt(text: String) {
        openNewChat()
        input = text
        if (configured) send() else go(Screen.Providers)
    }

    fun tick() {
        if (flags["haptic"] != true) return
        try {
            val v = getApplication<Application>().getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
            @Suppress("DEPRECATION")
            v.vibrate(18)
        } catch (_: Exception) { }
    }

    override fun onCleared() {
        tts?.shutdown()
        super.onCleared()
    }

    fun toggle(k: String) {
        val v = !(flags[k] ?: false)
        flags[k] = v
        store.setBool(k, v)
        if (k == "rtl") rtl = v
    }

    fun go(s: Screen) {
        screen = s
        drawerOpen = false
        plusOpen = false
        providerSheet = false
        newMenu = false
        if (s == Screen.Logs) logs = llm.lastLogs
        if (s == Screen.Backup) backupText = store.exportJson()
    }

    fun openNewChat() {
        current = null
        input = ""
        pendingAttach = null
        pendingImage = null
        go(Screen.Chat)
    }

    fun openConv(id: String) {
        current = conversations.find { it.id == id }
        go(Screen.Chat)
    }

    fun attach(kind: String) {
        pendingAttach = kind
        plusOpen = false
        snack = "$kind attached — send with your message"
    }

    fun attachImage(b64: String, label: String) {
        pendingImage = b64
        pendingAttach = label
        plusOpen = false
        snack = "Image attached"
    }

    fun send() {
        val text = input.trim()
        if (text.isBlank() && pendingAttach == null && pendingImage == null) return
        val p = selectedProvider
        if (p == null || p.apiKey.isBlank() || p.baseUrl.isBlank()) {
            snack = "Add an API key first — open Keys."
            go(Screen.Providers)
            return
        }
        var conv = current
        if (conv == null) {
            conv = Conversation(AppStore.id(), text.take(32).ifBlank { "New Chat" }, text, System.currentTimeMillis(), emptyList(), p.id)
        }
        val body = buildString {
            pendingAttach?.let { if (pendingImage == null) append("[$it] ") }
            append(if (text.isBlank()) "Please look at this." else text)
        }
        val user = ChatMessage(AppStore.id(), ChatMessage.Role.User, body, pendingAttach, pendingImage)
        conv = conv.copy(
            messages = conv.messages + user,
            preview = body.take(80),
            updatedAt = System.currentTimeMillis(),
            title = if (conv.messages.isEmpty()) body.take(32) else conv.title,
            providerId = p.id
        )
        current = conv
        store.saveConversation(conv)
        refresh()
        input = ""
        pendingAttach = null
        pendingImage = null
        reply(conv)
    }

    private fun reply(conv: Conversation) {
        // Keep a conversation pinned to the provider it was created with.
        // Changing the global provider while a chat is open must not silently
        // route the existing conversation through a different backend.
        val p = providers.find { it.id == conv.providerId } ?: selectedProvider ?: return
        sending = true
        viewModelScope.launch {
            try {
                val sys = buildSystem()
                var msgs = conv.messages
                val researchMode = composerMode == "Research"
                if ((searchOn || researchMode) && msgs.lastOrNull()?.role == ChatMessage.Role.User) {
                    val q = msgs.last().text
                    val results = withContext(Dispatchers.IO) {
                        llm.searchDuckResults(q, limit = if (researchMode) 5 else 3)
                    }
                    logs = llm.lastLogs
                    if (results.isNotEmpty()) {
                        val label = if (researchMode) "Research sources" else "Web search"
                        val web = results.mapIndexed { index, result ->
                            val snippet = result.snippet?.takeIf { it.isNotBlank() }?.let { " — $it" }.orEmpty()
                            val page = if (researchMode) {
                                withContext(Dispatchers.IO) { llm.fetchWebPage(result.url, maxChars = 10000) }
                            } else ""
                            val content = page.takeIf { it.isNotBlank() }?.let { "\nSource content:\n$it" }.orEmpty()
                            "[${index + 1}] ${result.title} — ${result.url}$snippet$content"
                        }.joinToString("\n\n")
                        msgs = msgs.dropLast(1) + msgs.last().copy(text = "${msgs.last().text}\n\n$label:\n$web")
                                        } else if (researchMode) {
                        msgs = msgs.dropLast(1) + msgs.last().copy(
                            text = "${msgs.last().text}\n\nResearch note: live web search returned no usable results. Do not invent sources."
                        )
                    }
                }
                val model = p.model.ifBlank { chatModel.ifBlank { p.models.firstOrNull().orEmpty() } }
                val botId = AppStore.id()
                val latest0 = current?.takeIf { it.id == conv.id } ?: conv
                current = latest0.copy(messages = latest0.messages + ChatMessage(botId, ChatMessage.Role.Assistant, ""))
                val acc = StringBuilder()
                val mainH = android.os.Handler(android.os.Looper.getMainLooper())
                val answer = withContext(Dispatchers.IO) {
                    llm.chatStream(p, model, msgs, sys) { delta ->
                        val first = acc.isEmpty()
                        acc.append(delta)
                        val snap = acc.toString()
                        mainH.post {
                            if (first) tick()
                            val cur = current?.takeIf { it.id == conv.id } ?: return@post
                            current = cur.copy(messages = cur.messages.map { if (it.id == botId) it.copy(text = snap) else it })
                        }
                    }
                }
                logs = llm.lastLogs
                val latest = current?.takeIf { it.id == conv.id } ?: conv
                val done = latest.copy(
                    messages = latest.messages.map { if (it.id == botId) it.copy(text = answer.ifBlank { acc.toString() }) else it },
                    updatedAt = System.currentTimeMillis()
                )
                current = done
                store.saveConversation(done)
                store.setInt("msg_count", store.int("msg_count") + 2)
                store.setInt("out_tokens", store.int("out_tokens") + answer.split(" ").size)
                store.setInt("in_tokens", store.int("in_tokens") + msgs.sumOf { it.text.split(" ").size })
                if (done.messages.count { it.role == ChatMessage.Role.User } == 1 && fastModel.isNotBlank()) {
                    titleChat(done, p)
                }
                if (flags["notif_gen"] == true) snack = "Reply ready"
                refresh()
            } catch (e: Exception) {
                snack = llm.friendly(e)
                logs = llm.lastLogs
            } finally {
                sending = false
            }
        }
    }

    private fun titleChat(conv: Conversation, p: Provider) {
        viewModelScope.launch {
            try {
                val t = withContext(Dispatchers.IO) {
                    llm.chat(
                        p,
                        fastModel.ifBlank { p.model },
                        listOf(ChatMessage(AppStore.id(), ChatMessage.Role.User, "Title this chat in 5 words: ${conv.preview}")),
                        "Reply with only a short title."
                    )
                }
                val latest = current?.takeIf { it.id == conv.id } ?: return@launch
                val done = latest.copy(title = t.lineSequence().first().take(40))
                current = done
                store.saveConversation(done)
                refresh()
            } catch (_: Exception) { }
        }
    }

    private fun buildSystem(): String = buildString {
        when (composerMode) {
            "Research" -> append("You are in Research mode. Use the supplied live web sources as evidence. Cite factual claims with [n] matching the supplied source numbers, never invent or renumber sources, and do not cite anything that is not in the supplied source list. Separate verified facts from uncertainty. End with a concise Sources section listing the cited [n] sources as Markdown links using their supplied URLs.\\n")
            "Create" -> append("You are in Create mode. Produce polished, usable output and keep the response focused on the requested deliverable.\\n")
            "Code" -> append("You are in Code mode. Prefer correct, maintainable code, explain important implementation choices briefly, and consider edge cases.\\n")
            "Analyze" -> append("You are in Analyze mode. Break the problem into evidence, assumptions, and conclusions without overstating certainty.\\n")
            "Agent" -> append("You are in Agent mode. Work through the task step by step, using configured tools or MCP context when appropriate.\\n")
        }
        append(selectedAssistant?.prompt ?: "You are a helpful assistant.")
        val tools = mcp.filter { it.enabled }
        if (tools.isNotEmpty()) {
            append("\nMCP servers the user configured:\n")
            tools.forEach { append("- ${it.name} (${it.transport}) ${it.url}\n") }
        }
    }

    fun stop() {
        llm.cancel()
        sending = false
        snack = "Stopped"
    }

    fun regenerate() {
        val conv = current ?: return
        val lastA = conv.messages.indexOfLast { it.role == ChatMessage.Role.Assistant }
        if (lastA >= 0) {
            val trimmed = conv.copy(messages = conv.messages.take(lastA))
            current = trimmed
            store.saveConversation(trimmed)
            reply(trimmed)
        }
    }

    fun addModelProfile(
        providerId: String,
        modelId: String,
        displayName: String,
        modelType: String,
        inputModalities: List<String>,
        outputModalities: List<String>,
        abilities: List<String>,
        providerOverride: String,
        headers: String,
        body: String,
        builtInTools: List<String>
    ) {
        val profile = com.ariai.app.data.ModelProfile(
            id = AppStore.id(),
            providerId = providerId,
            modelId = modelId.trim(),
            displayName = displayName.ifBlank { modelId.trim() },
            modelType = modelType,
            inputModalities = inputModalities,
            outputModalities = outputModalities,
            abilities = abilities,
            providerOverride = providerOverride,
            headers = headers,
            body = body,
            builtInTools = builtInTools
        )
        val list = modelProfiles.filterNot { it.id == profile.id } + profile
        store.saveModelProfiles(list)
        modelProfiles = list
        providers.find { it.id == providerId }?.let { p ->
            if (profile.modelId.isNotBlank() && profile.modelId !in p.models) {
                updateProvider(p.copy(models = p.models + profile.modelId, model = p.model.ifBlank { profile.modelId }))
            }
        }
        snack = "Model added"
    }

    fun deleteModelProfile(id: String) {
        val list = modelProfiles.filterNot { it.id == id }
        store.saveModelProfiles(list)
        modelProfiles = list
        snack = "Model removed"
    }

    fun addProvider(name: String, url: String, model: String, key: String, headers: String = "", kind: String = "openai") {
        val p = Provider(AppStore.id(), name.ifBlank { "Provider" }, url.trimEnd('/'), model, key, emptyList(), headers, kind, enabled = true)
        val list = providers + p
        store.saveProviders(list)
        providers = list
        selectedProviderId = p.id
        store.setStr("sel_provider", p.id)
        if (chatModel.isBlank() && model.isNotBlank()) {
            chatModel = model
            store.setStr("chat_model", model)
        }
        snack = "Provider saved"
    }

    fun updateProvider(p: Provider) {
        val list = providers.map { if (it.id == p.id) p else it }
        store.saveProviders(list)
        providers = list
    }

    fun removeProvider(id: String) {
        val list = providers.filter { it.id != id }
        store.saveProviders(list)
        providers = list
        if (selectedProviderId == id) {
            selectedProviderId = list.firstOrNull()?.id ?: ""
            store.setStr("sel_provider", selectedProviderId)
        }
    }

    fun setProviderOn(id: String, on: Boolean) {
        val p = providers.find { it.id == id } ?: return
        updateProvider(p.copy(enabled = on))
        if (on) selectProvider(id)
    }

    fun selectProvider(id: String) {
        selectedProviderId = id
        store.setStr("sel_provider", id)
        providers.find { it.id == id }?.model?.takeIf { it.isNotBlank() }?.let {
            chatModel = it
            store.setStr("chat_model", it)
        }
        providerSheet = false
    }

    fun selectModel(model: String) {
        val p = selectedProvider ?: return
        updateProvider(p.copy(model = model))
        chatModel = model
        store.setStr("chat_model", model)
        providerSheet = false
        snack = "Using $model"
    }

    fun fetchModels(id: String) {
        val p = providers.find { it.id == id } ?: return
        fetching = true
        viewModelScope.launch {
            try {
                val models = withContext(Dispatchers.IO) { llm.listModels(p) }
                logs = llm.lastLogs
                updateProvider(p.copy(models = models, model = p.model.ifBlank { models.firstOrNull().orEmpty() }))
                snack = if (models.isEmpty()) "No models returned" else "${models.size} models"
            } catch (e: Exception) {
                snack = llm.friendly(e)
                logs = llm.lastLogs
            } finally {
                fetching = false
            }
        }
    }

    fun testProvider(id: String) {
        val p = providers.find { it.id == id } ?: return
        fetching = true
        viewModelScope.launch {
            try {
                val a = withContext(Dispatchers.IO) {
                    llm.chat(p, p.model.ifBlank { p.models.firstOrNull().orEmpty() }, listOf(ChatMessage(AppStore.id(), ChatMessage.Role.User, "Say hi in 5 words.")), null)
                }
                logs = llm.lastLogs
                updateProvider(p.copy(lastOk = a.take(80)))
                snack = "OK: $a"
            } catch (e: Exception) {
                snack = llm.friendly(e)
                logs = llm.lastLogs
            } finally {
                fetching = false
            }
        }
    }

    fun addAssistant(name: String, prompt: String = "You are a helpful assistant.") {
        val a = Assistant(AppStore.id(), name.ifBlank { "Assistant" }, prompt)
        val list = assistants + a
        store.saveAssistants(list)
        assistants = list
    }

    fun updateAssistant(a: Assistant) {
        val list = assistants.map { if (it.id == a.id) a else it }
        store.saveAssistants(list)
        assistants = list
    }

    fun saveMcp(s: McpServer) {
        val list = mcp.filter { it.id != s.id } + s
        store.saveMcp(list)
        mcp = list
        mcpDraft = null
        snack = "MCP saved"
    }

    fun importMcp(raw: String) {
        if (raw.isBlank()) {
            snack = "Paste JSON first"
            return
        }
        try {
            val o = org.json.JSONObject(raw)
            val servers = o.optJSONObject("mcpServers") ?: o
            val keys = servers.keys()
            var n = 0
            while (keys.hasNext()) {
                val name = keys.next()
                val s = servers.getJSONObject(name)
                val url = s.optString("url").ifBlank { s.optString("server") }
                saveMcp(McpServer(AppStore.id(), name, url, true, s.optString("type", "http"), s.optJSONObject("headers")?.toString() ?: ""))
                n++
            }
            snack = "Imported $n server(s)"
        } catch (_: Exception) {
            saveMcp(McpServer(AppStore.id(), "Imported MCP", "", true, "http", raw.take(400)))
        }
        importJson = ""
        mcpImport = false
    }

    fun deleteConv(id: String) {
        lastDeleted = conversations.find { it.id == id }
        store.deleteConversation(id)
        if (current?.id == id) current = null
        refresh()
    }

    fun undoDelete() {
        val c = lastDeleted ?: return
        store.saveConversation(c)
        lastDeleted = null
        refresh()
        snack = "Restored"
    }

    fun pinConv(id: String) {
        val c = conversations.find { it.id == id } ?: return
        store.saveConversation(c.copy(pinned = !c.pinned))
        refresh()
    }

    fun editLastUser() {
        val conv = current ?: return
        val i = conv.messages.indexOfLast { it.role == ChatMessage.Role.User }
        if (i < 0) return
        val msg = conv.messages[i]
        input = msg.text
        val trimmed = conv.copy(messages = conv.messages.take(i))
        current = trimmed
        store.saveConversation(trimmed)
        refresh()
    }

    fun setUser(n: String) {
        userName = n
        store.setStr("user_name", n)
    }

    fun pickSpeech(id: String) {
        speechId = id
        store.setStr("speech", id)
    }

    fun speakLast() {
        val text = current?.messages?.lastOrNull { it.role == ChatMessage.Role.Assistant }?.text
        if (text.isNullOrBlank()) {
            snack = "No reply to speak"
            return
        }
        var t = text
        if (flags["tts_quotes"] == true) {
            t = Regex("[\"“](.*?)[\"”]").findAll(text).joinToString(" ") { it.groupValues[1] }.ifBlank { text }
        }
        if (flags["tts_brackets"] == true) t = t.replace(Regex("[\\[\\(].*?[\\]\\)]"), "")
        val rate = 0.5f + ttsSpeed * 0.15f
        tts?.setSpeechRate(rate)
        tts?.speak(t, TextToSpeech.QUEUE_FLUSH, null, "ariai")
        snack = "Speaking"
    }

    fun pickSlot(slot: String, id: String) {
        when (slot) {
            "chat" -> { chatModel = id; store.setStr("chat_model", id); selectedProvider?.let { updateProvider(it.copy(model = id)) } }
            "fast" -> { fastModel = id; store.setStr("fast_model", id) }
            "tr" -> { translateModel = id; store.setStr("tr_model", id) }
            "ocr" -> { ocrModel = id; store.setStr("ocr_model", id) }
            "cmp" -> { compressModel = id; store.setStr("compress_model", id); store.setStr("cmp_model", id) }
        }
        snack = "Model selected"
    }

    fun modelName(id: String): String {
        if (id.isBlank()) return "Select Model"
        providers.forEach { p ->
            if (p.model == id || p.models.contains(id)) return id
        }
        return providers.find { it.id == id }?.let { pr -> pr.model.ifBlank { pr.name } } ?: id
    }

    fun allModels(): List<String> = providers.flatMap { if (it.models.isEmpty()) listOfNotNull(it.model.takeIf { m -> m.isNotBlank() }) else it.models }.distinct()

    fun compressHistory() {
        val conv = current ?: run { snack = "No chat"; return }
        val p = selectedProvider ?: run { snack = "No provider"; return }
        plusOpen = false
        sending = true
        viewModelScope.launch {
            try {
                val model = compressModel.ifBlank { p.model }
                val summary = withContext(Dispatchers.IO) {
                    llm.chat(p, model, conv.messages.takeLast(20), "Summarize this conversation briefly for context.")
                }
                val sys = ChatMessage(AppStore.id(), ChatMessage.Role.System, "Summary: $summary")
                val keep = conv.messages.takeLast(2)
                val done = conv.copy(messages = listOf(sys) + keep, updatedAt = System.currentTimeMillis())
                current = done
                store.saveConversation(done)
                refresh()
                snack = "History compressed"
            } catch (e: Exception) {
                snack = e.message
            } finally {
                sending = false
            }
        }
    }

    fun translateLast() {
        val last = current?.messages?.lastOrNull() ?: return
        val p = selectedProvider ?: return
        input = "Translate to English:\n${last.text}"
        send()
        p.model
    }

    fun addPrompt(title: String, body: String) {
        val list = prompts + PromptItem(AppStore.id(), title, body)
        store.savePrompts(list)
        prompts = list
    }

    fun usePrompt(body: String) {
        input = body
        go(Screen.Chat)
    }

    fun addQuick(text: String) {
        val list = quick + QuickMsg(AppStore.id(), text)
        store.saveQuick(list)
        quick = list
    }

    fun useQuick(text: String) {
        input = text
        go(Screen.Chat)
    }

    fun saveSearch(on: Boolean, key: String) {
        searchOn = on
        searchKey = key
        store.setBool("search_on", on)
        store.setStr("search_key", key)
        snack = if (on) "Search enabled (DuckDuckGo)" else "Search off"
    }

    fun doBackupImport(raw: String) {
        try {
            store.importJson(raw)
            providers = store.providers()
            assistants = store.assistants()
            conversations = store.conversations()
            mcp = store.mcp()
            prompts = store.prompts()
            quick = store.quick()
            snack = "Backup restored"
            go(Screen.Chat)
        } catch (e: Exception) {
            snack = "Invalid backup: ${e.message}"
        }
    }

    fun clearStorage() {
        conversations.forEach { store.deleteConversation(it.id) }
        current = null
        refresh()
        snack = "Chats cleared"
    }

    private fun refresh() {
        conversations = store.conversations()
        providers = store.providers()
        modelProfiles = store.modelProfiles()
        assistants = store.assistants()
    }
}
