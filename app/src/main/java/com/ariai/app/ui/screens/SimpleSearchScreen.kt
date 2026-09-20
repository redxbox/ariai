package com.ariai.app.ui.screens

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
    val recentSearches = remember { mutableStateListOf("AI news today", "Python best practices", "Quantum computing") }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFFEFBFF))) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                        }
                    },
                    title = { Text("Search", fontWeight = FontWeight.SemiBold, color = Color.Black) }
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
                        placeholder = { Text("Search web or chat history", color = Color.Black.copy(alpha = 0.4f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black.copy(alpha = 0.4f)) },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = {
                                    recentSearches.add(0, query)
                                    onSearch(query)
                                }) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Search", tint = Color(0xFF6C4DFF))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF6C4DFF).copy(alpha = 0.3f),
                            unfocusedBorderColor = Color.Black.copy(alpha = 0.08f)
                        ),
                        singleLine = true
                    )
                }

                if (query.isBlank()) {
                    item {
                        Text("Recent searches", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black.copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp))
                    }
                    items(count = recentSearches.size, key = { i -> recentSearches[i] }) { i ->
                        val item = recentSearches[i]
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth().clickable { onSearch(item) }
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                                    Text(item, fontSize = 14.sp, color = Color.Black)
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Black.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    item {
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Search providers", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.Black)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Web", "Chats", "Docs").forEach { type ->
                                        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFF2F2F7), modifier = Modifier.clickable { scope.launch { snackbarHostState.showSnackbar("$type search") } }) {
                                            Text(type, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, color = Color.Black.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF6C4DFF).copy(alpha = 0.1f)), modifier = Modifier.fillMaxWidth().clickable { onSearch(query) }) {
                            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF6C4DFF))
                                Column {
                                    Text("Search for \"$query\"", fontWeight = FontWeight.Medium, color = Color(0xFF6C4DFF), fontSize = 14.sp)
                                    Text("Press to search with AI", color = Color.Black.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
