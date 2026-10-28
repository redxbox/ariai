package com.ariai.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.SearchMode
import com.ariai.app.ui.theme.AriCard
import com.ariai.app.ui.theme.AriInk
import com.ariai.app.ui.theme.AriMuted
import com.ariai.app.ui.theme.AriPaper
import com.ariai.app.util.tx

/** Manages Gemini and Anthropic provider keys, which are also used by built-in web search. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SearchServiceScreen(
    providers: List<Provider>,
    searchMode: SearchMode,
    selectedProviderId: String?,
    selectedModelId: String?,
    onModeChange: (SearchMode) -> Unit,
    onSelectModel: (String, String) -> Unit,
    onSaveProvider: (Provider, (Provider?) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val options = remember(providers, selectedProviderId, selectedModelId) {
        nativeSearchProviderOptions(providers, selectedProviderId, selectedModelId)
    }

    Scaffold(
        containerColor = AriPaper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AriPaper),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tx("Back"), tint = AriInk)
                    }
                },
                title = { Text(tx("Search service"), fontWeight = FontWeight.SemiBold, color = AriInk) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = AriCard,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(tx("Built-in web search"), fontWeight = FontWeight.SemiBold, color = AriInk)
                        Text(
                            tx("Manage the same Gemini or Anthropic API key used by your AI provider. Saving a key here selects that provider and turns on web search; no separate search key is stored."),
                            fontSize = 13.sp,
                            color = AriMuted
                        )
                    }
                }
            }
            items(options, key = { it.type.name }) { option ->
                NativeSearchProviderCard(
                    option = option,
                    mode = searchMode,
                    selectedProviderId = selectedProviderId,
                    selectedModelId = selectedModelId,
                    onModeChange = onModeChange,
                    onSelectModel = onSelectModel,
                    onSaveProvider = onSaveProvider
                )
            }
        }
    }
}
