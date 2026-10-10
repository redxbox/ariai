package com.ariai.app.ui.screens

import com.ariai.app.util.tx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.ariai.app.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Accent: Color @Composable @ReadOnlyComposable get() = AriInk
private val Ink: Color @Composable @ReadOnlyComposable get() = AriInk
private data class SearchService(val key: String, val name: String, val description: String)

private val searchServices = listOf(
    SearchService("tavily", "Tavily", tx("Research-focused search API")),
    SearchService("brave", "Brave Search", tx("Independent web index")),
    SearchService("exa", "Exa", tx("Semantic neural search")),
    SearchService("serper", "Serper", tx("Google results via API"))
)

/**
 * Web search service settings. Each service stores its own API key.
 * The first service with a non-empty key is used for web search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchServiceScreen(
    searchKeys: Map<String, String>,
    onSaveKey: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var expanded by remember { mutableStateOf<String?>(null) }
    val activeKey = searchServices.firstOrNull { !searchKeys[it.key].isNullOrBlank() }?.key

    Scaffold(
        containerColor = AriPaper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AriPaper),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = tx("Back"), tint = Ink) } },
                title = { Text(tx("Search service"), fontWeight = FontWeight.SemiBold, color = Ink) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Add an API key to let AriAI search the web before answering. Keys stay on this device.",
                    color = Ink.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            itemsIndexed(searchServices, key = { i, s -> "${s.key}_$i" }) { _, service ->
                val key = searchKeys[service.key].orEmpty()
                val isOpen = expanded == service.key
                val isActive = activeKey == service.key
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AriCard,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth().clickable { expanded = if (isOpen) null else service.key }
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Accent, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(service.name, fontWeight = FontWeight.SemiBold, color = Ink)
                                Text(service.description, fontSize = 12.sp, color = Ink.copy(alpha = 0.5f))
                            }
                            Text(
                                when {
                                    isActive -> tx("Active")
                                    key.isNotBlank() -> tx("Saved")
                                    else -> tx("No key")
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isActive) AriInk else Ink.copy(alpha = 0.45f)
                            )
                        }
                        if (isOpen) {
                            var draft by remember(service.key, isOpen) { mutableStateOf(key) }
                            var reveal by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = draft,
                                onValueChange = { draft = it },
                                label = { Text(tx("API key")) },
                                singleLine = true,
                                visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { reveal = !reveal }) {
                                        Icon(Icons.Default.Visibility, contentDescription = tx("Show key"), tint = Ink.copy(alpha = 0.5f))
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { onSaveKey(service.key, ""); draft = "" },
                                    enabled = key.isNotBlank(),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) { Text(tx("Remove")) }
                                Button(
                                    onClick = { onSaveKey(service.key, draft.trim()); expanded = null },
                                    enabled = draft.isNotBlank() && draft.trim() != key,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                    modifier = Modifier.weight(1f)
                                ) { Text(tx("Save")) }
                            }
                        }
                    }
                }
            }
        }
    }
}
