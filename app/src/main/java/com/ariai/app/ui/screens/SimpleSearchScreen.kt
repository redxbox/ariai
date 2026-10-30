package com.ariai.app.ui.screens

import com.ariai.app.util.tx

import com.ariai.app.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class RecentSearch(val id: String, val text: String, val isLocalizationKey: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSearchScreen(
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    canSearch: Boolean,
    onConfigureModel: () -> Unit,
    onConfigureSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val suggestedSearches = remember {
        listOf(
            RecentSearch("sample:ai-news", "AI news today", isLocalizationKey = true),
            RecentSearch("sample:python", "Python best practices", isLocalizationKey = true),
            RecentSearch("sample:quantum", "Quantum computing", isLocalizationKey = true)
        )
    }
    val recentSearches = remember { mutableStateListOf<RecentSearch>() }
    val submitSearch: (String) -> Unit = { rawQuery ->
        val text = rawQuery.trim()
        if (canSearch && text.isNotBlank()) {
            recentSearches.removeAll { !it.isLocalizationKey && it.text.equals(text, ignoreCase = true) }
            recentSearches.add(0, RecentSearch(java.util.UUID.randomUUID().toString(), text))
            while (recentSearches.size > 8) recentSearches.removeAt(recentSearches.lastIndex)
            query = ""
            onSearch(text)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(AriPaper)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AriCard),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tx("Back"), tint = AriInk)
                        }
                    },
                    title = { Text(tx("Search"), fontWeight = FontWeight.SemiBold, color = AriInk) }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        enabled = canSearch,
                        placeholder = { Text(tx("Ask AriAI with web search"), color = AriInk.copy(alpha = 0.4f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AriInk.copy(alpha = 0.4f)) },
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start, textDirection = TextDirection.ContentOrLtr),
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = tx("Clear search"), tint = AriInk)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AriCard,
                            unfocusedContainerColor = AriCard,
                            focusedBorderColor = AriInk.copy(alpha = 0.3f),
                            unfocusedBorderColor = AriInk.copy(alpha = 0.08f)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { submitSearch(query) })
                    )
                }

                if (!canSearch) {
                    item {
                        Surface(shape = RoundedCornerShape(16.dp), color = AriCard, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(tx("Choose a chat model before searching."), color = AriInk, fontWeight = FontWeight.Medium)
                                Button(onClick = onConfigureModel, colors = ButtonDefaults.buttonColors(containerColor = AriAccent)) {
                                    Text(tx("Choose model"))
                                }
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            tx("Questions open in a new chat with web search."),
                            fontSize = 12.sp,
                            color = AriInk.copy(alpha = 0.6f)
                        )
                        TextButton(onClick = onConfigureSearch) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(tx("Configure search"))
                        }
                    }
                }

                if (query.isBlank()) {
                    if (recentSearches.isNotEmpty()) {
                        item {
                            Text(tx("Recent searches"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AriInk.copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    items(recentSearches, key = { it.id }) { item ->
                        val label = if (item.isLocalizationKey) tx(item.text) else item.text
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = AriCard),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, enabled = canSearch, onClick = { submitSearch(label) })
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = AriInk.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                                    Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp, color = AriInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = AriInk.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    item {
                        Text(tx("Suggested searches"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AriInk.copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp))
                    }
                    items(suggestedSearches, key = { it.id }) { suggestion ->
                        val label = if (suggestion.isLocalizationKey) tx(suggestion.text) else suggestion.text
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = AriCard),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, enabled = canSearch, onClick = { submitSearch(label) })
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = AriInk.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                                    Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp, color = AriInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = AriInk.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    item {
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AriAccent.copy(alpha = 0.1f)), modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, enabled = canSearch, onClick = { submitSearch(query) })) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = AriAccent)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        tx("Search for") + " \"$query\"",
                                        fontWeight = FontWeight.Medium,
                                        color = AriInk,
                                        fontSize = 14.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(tx("Press to search with AI"), color = AriInk.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = AriAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
