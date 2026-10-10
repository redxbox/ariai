package com.ariai.app.ui.screens

import com.ariai.app.util.tx

import com.ariai.app.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSearchScreen(
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val recentSearches = remember { mutableStateListOf(tx("AI news today"), tx("Python best practices"), tx("Quantum computing")) }

    Box(modifier = modifier.fillMaxSize().background(AriPaper)) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AriCard),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = tx("Back"), tint = AriInk)
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
                        placeholder = { Text(tx("Search web or chat history"), color = AriInk.copy(alpha = 0.4f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AriInk.copy(alpha = 0.4f)) },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = {
                                    recentSearches.add(0, query)
                                    onSearch(query)
                                }) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = tx("Search"), tint = AriInk)
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
                        singleLine = true
                    )
                }

                if (query.isBlank()) {
                    item {
                        Text(tx("Recent searches"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AriInk.copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp))
                    }
                    items(count = recentSearches.size, key = { i -> recentSearches[i] }) { i ->
                        val item = recentSearches[i]
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = AriCard),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth().clickable { onSearch(item) }
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = AriInk.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                                    Text(item, fontSize = 14.sp, color = AriInk)
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = AriInk.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    item {
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AriCard), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(tx("Search providers"), fontWeight = FontWeight.Medium, fontSize = 13.sp, color = AriInk)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(tx("Web"), tx("Chats"), tx("Docs")).forEach { type ->
                                        Surface(shape = RoundedCornerShape(20.dp), color = AriTint, modifier = Modifier.clickable { scope.launch { snackbarHostState.showSnackbar("$type search") } }) {
                                            Text(type, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, color = AriInk.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AriInk.copy(alpha = 0.1f)), modifier = Modifier.fillMaxWidth().clickable { onSearch(query) }) {
                            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = AriInk)
                                Column {
                                    Text(tx("Search for") + " \"$query\"", fontWeight = FontWeight.Medium, color = AriInk, fontSize = 14.sp)
                                    Text(tx("Press to search with AI"), color = AriInk.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
