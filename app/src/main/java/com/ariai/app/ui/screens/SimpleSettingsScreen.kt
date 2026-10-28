package com.ariai.app.ui.screens

import com.ariai.app.BuildConfig
import com.ariai.app.util.tx

import com.ariai.app.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Simple clean settings - only essential options
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSettingsScreen(
    onBack: () -> Unit,
    onProvidersClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onSearchServiceClick: () -> Unit = {},
    onStorageClick: () -> Unit = {},
    onDefaultModelClick: () -> Unit = {},
    onPreferencesClick: () -> Unit = {},
    defaultModelName: String = tx("Not chosen"),
    onClearAll: () -> Unit = {},
    onMcpClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                    title = { Text(tx("Settings"), fontWeight = FontWeight.SemiBold, color = AriInk) }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SimpleGroup(title = tx("General")) {
                        SimpleRow(
                            icon = Icons.Default.Settings,
                            tint = AriAccent,
                            title = tx("Preferences"),
                            subtitle = tx("Appearance, language and chat settings"),
                            onClick = onPreferencesClick
                        )
                    }
                }

                item {
                    SimpleGroup(title = tx("Models & services")) {
                        SimpleRow(icon = Icons.Default.Star, tint = com.ariai.app.ui.theme.AriAccent, title = tx("Default model"), subtitle = defaultModelName, onClick = onDefaultModelClick)
                        HorizontalDivider(color = AriInk.copy(alpha = 0.06f))
                        SimpleRow(icon = Icons.Default.Storage, tint = com.ariai.app.ui.theme.AriAccent, title = tx("Providers"), subtitle = tx("OpenAI, Gemini, Claude and more"), onClick = onProvidersClick)
                        HorizontalDivider(color = AriInk.copy(alpha = 0.06f))
                        SimpleRow(icon = Icons.Default.Search, tint = com.ariai.app.ui.theme.AriAccent, title = tx("Search service"), subtitle = tx("Web search for answers"), onClick = onSearchServiceClick)
                        HorizontalDivider(color = AriInk.copy(alpha = 0.06f))
                        SimpleRow(icon = Icons.Default.Settings, tint = com.ariai.app.ui.theme.AriAccent, title = tx("MCP servers"), subtitle = tx("Preview only; tools are not connected yet"), onClick = onMcpClick)
                    }
                }

                item {
                    SimpleGroup(title = tx("Data")) {
                        SimpleRow(icon = Icons.Default.Storage, tint = com.ariai.app.ui.theme.AriAccent, title = tx("Data & storage"), subtitle = tx("Export list, storage overview"), onClick = onStorageClick)
                        HorizontalDivider(color = AriInk.copy(alpha = 0.06f))
                        SimpleRow(icon = Icons.Default.Delete, tint = com.ariai.app.ui.theme.AriAccent, title = tx("Delete all chats"), subtitle = tx("Remove every conversation"), onClick = onClearAll)
                    }
                }

                item {
                    SimpleGroup(title = tx("About")) {
                        SimpleRow(
                            icon = Icons.Default.Info,
                            tint = com.ariai.app.ui.theme.AriAccent,
                            title = tx("About AriAI"),
                            subtitle = "${tx("Version")} ${BuildConfig.VERSION_NAME}",
                            onClick = onAboutClick
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun SimpleGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AriInk.copy(alpha = 0.5f), modifier = Modifier.padding(start = 4.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AriCard), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SimpleRow(icon: ImageVector, title: String, subtitle: String, tint: Color? = null, onClick: () -> Unit) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(AriTint), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint ?: AriInk.copy(alpha = 0.65f), modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = AriInk, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 11.sp, color = AriInk.copy(alpha = 0.5f))
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = AriInk.copy(alpha = 0.2f),
            modifier = Modifier.size(18.dp).scale(scaleX = if (isRtl) -1f else 1f, scaleY = 1f)
        )
    }
}
