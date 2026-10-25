package com.ariai.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.ui.theme.AriAccent
import com.ariai.app.ui.theme.AriAccentGreen
import com.ariai.app.ui.theme.AriCard
import com.ariai.app.ui.theme.AriInk
import com.ariai.app.ui.theme.AriMuted
import com.ariai.app.ui.theme.AriPaper
import com.ariai.app.util.tx

private val Accent: Color @Composable @ReadOnlyComposable get() = AriAccent
private data class SearchService(val key: String, val name: String, val descriptionKey: String) {
    val description: String get() = tx(descriptionKey)
}

private val searchServices = listOf(
    SearchService("tavily", "Tavily", "Research-focused search API"),
    SearchService("brave", "Brave Search", "Independent web index"),
    SearchService("exa", "Exa", "Semantic neural search"),
    SearchService("serper", "Serper", "Google results via API")
)

/** Each key is encrypted in private DataStore; an explicit test makes one real search request. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SearchServiceScreen(
    searchKeys: Map<String, String>,
    onSaveKey: (String, String) -> Unit,
    onTestKey: (String, String, (String?) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    var expanded by remember { mutableStateOf<String?>(null) }
    val activeKey = searchServices.firstOrNull { !searchKeys[it.key].isNullOrBlank() }?.key

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
                title = { Text(tx("Search service"), fontWeight = FontWeight.SemiBold, color = AriInk) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    tx("Add an API key to let AriAI search the web before answering. Keys stay on this device."),
                    color = AriInk.copy(alpha = 0.62f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            itemsIndexed(searchServices, key = { _, service -> "search-service:${service.key}" }) { index, service ->
                val brand = if (index % 2 == 0) AriAccent else AriAccentGreen
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
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(brand.copy(alpha = 0.13f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = brand, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(service.name, fontWeight = FontWeight.SemiBold, color = AriInk)
                                Text(service.description, fontSize = 12.sp, color = AriMuted)
                            }
                            Text(
                                when {
                                    isActive -> tx("Active")
                                    key.isNotBlank() -> tx("Saved")
                                    else -> tx("No key")
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isActive) AriAccentGreen else AriMuted
                            )
                        }
                        if (isOpen) {
                            var draft by remember(service.key, isOpen) { mutableStateOf(key) }
                            var reveal by remember(service.key, isOpen) { mutableStateOf(false) }
                            var testing by remember(service.key, isOpen) { mutableStateOf(false) }
                            var testMessage by remember(service.key, isOpen) { mutableStateOf("") }
                            var testSucceeded by remember(service.key, isOpen) { mutableStateOf<Boolean?>(null) }

                            OutlinedTextField(
                                value = draft,
                                onValueChange = { draft = it; testMessage = ""; testSucceeded = null },
                                label = { Text(tx("API key")) },
                                singleLine = true,
                                visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { reveal = !reveal }) {
                                        Icon(
                                            if (reveal) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = tx(if (reveal) "Hide key" else "Show key"),
                                            tint = AriMuted
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("search-key-${service.key}")
                            )
                            Text(
                                tx("Testing sends one search request and may use your provider quota."),
                                fontSize = 11.sp,
                                color = AriMuted
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { onSaveKey(service.key, ""); draft = ""; testMessage = "" },
                                    enabled = key.isNotBlank() && !testing,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) { Text(tx("Remove"), maxLines = 1) }
                                OutlinedButton(
                                    onClick = {
                                        testing = true
                                        testSucceeded = null
                                        testMessage = tx("Testing connection…")
                                        onTestKey(service.key, draft.trim()) { error ->
                                            testing = false
                                            testSucceeded = error == null
                                            testMessage = error ?: tx("Connection successful.")
                                        }
                                    },
                                    enabled = draft.isNotBlank() && !testing,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f).testTag("search-test-${service.key}")
                                ) { Text(if (testing) tx("Testing…") else tx("Test"), maxLines = 1) }
                                Button(
                                    onClick = { onSaveKey(service.key, draft.trim()); expanded = null },
                                    enabled = draft.isNotBlank() && draft.trim() != key && !testing,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                    modifier = Modifier.weight(1f)
                                ) { Text(tx("Save"), maxLines = 1) }
                            }
                            if (testMessage.isNotBlank()) {
                                Text(
                                    testMessage,
                                    modifier = Modifier.testTag("search-test-result-${service.key}"),
                                    color = when (testSucceeded) {
                                        true -> AriAccentGreen
                                        false -> MaterialTheme.colorScheme.error
                                        null -> AriMuted
                                    },
                                    fontSize = 12.sp,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
