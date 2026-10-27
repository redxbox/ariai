package com.ariai.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.AIModel
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import com.ariai.app.data.models.SearchMode
import com.ariai.app.data.remote.supportsNativeSearch
import com.ariai.app.ui.components.ProviderBrandMark
import com.ariai.app.ui.theme.AriAnthropic
import com.ariai.app.ui.theme.AriCard
import com.ariai.app.ui.theme.AriGemini
import com.ariai.app.ui.theme.AriInk
import com.ariai.app.ui.theme.AriMuted
import com.ariai.app.ui.theme.AriTint
import com.ariai.app.util.tx
import java.util.UUID

internal data class NativeSearchProviderOption(
    val type: ProviderType,
    val provider: Provider?,
    val model: AIModel?
) {
    val title: String
        get() = when (type) {
            ProviderType.GEMINI -> "Google Gemini"
            ProviderType.ANTHROPIC -> "Anthropic Claude"
            else -> "Anthropic Claude"
        }

    val hasUsableKey: Boolean
        get() = provider?.let {
            it.enabled && it.apiKey.isNotBlank() && !it.apiKeyNeedsReentry && supportsNativeSearch(it)
        } == true

    val isReady: Boolean
        get() = hasUsableKey && model != null
}

/** Exactly two built-in search choices; provider keys and selected models are kept in sync. */
internal fun nativeSearchProviderOptions(
    providers: List<Provider>,
    selectedProviderId: String?,
    selectedModelId: String?
): List<NativeSearchProviderOption> = listOf(ProviderType.GEMINI, ProviderType.ANTHROPIC).map { type ->
    val typedProviders = providers.filter { it.type == type }
    val selectedProvider = typedProviders.firstOrNull { it.id == selectedProviderId }
    val keyedProvider = typedProviders.firstOrNull {
        it.enabled && it.apiKey.isNotBlank() && !it.apiKeyNeedsReentry && supportsNativeSearch(it)
    }
    val selectedUsableProvider = selectedProvider?.takeIf {
        it.enabled && it.apiKey.isNotBlank() && !it.apiKeyNeedsReentry && supportsNativeSearch(it)
    }
    val provider = selectedUsableProvider ?: keyedProvider ?: selectedProvider ?: typedProviders.firstOrNull { it.enabled } ?: typedProviders.firstOrNull()
    val usable = provider?.takeIf {
        it.enabled && it.apiKey.isNotBlank() && !it.apiKeyNeedsReentry && supportsNativeSearch(it)
    }
    val textModels = usable?.models.orEmpty().filter { it.outputsText && it.id.isNotBlank() }
    val model = if (provider?.id == selectedProviderId) {
        textModels.firstOrNull { it.id == selectedModelId } ?: textModels.firstOrNull()
    } else {
        textModels.firstOrNull()
    }
    NativeSearchProviderOption(type = type, provider = provider, model = model)
}

/** A built-in-search choice with an inline API-key editor; saving updates the AI provider itself. */
@Composable
internal fun NativeSearchProviderCard(
    option: NativeSearchProviderOption,
    mode: SearchMode,
    selectedProviderId: String?,
    selectedModelId: String?,
    onModeChange: (SearchMode) -> Unit,
    onSelectModel: (String, String) -> Unit,
    onSaveProvider: (Provider, (Provider?) -> Unit) -> Unit,
    onAfterSelection: () -> Unit = {}
) {
    val isSelected = mode == SearchMode.MODEL && option.model != null &&
        option.provider?.id == selectedProviderId && option.model.id == selectedModelId
    val brandColor = if (option.type == ProviderType.GEMINI) AriGemini else AriAnthropic
    var editing by remember(option.type, option.provider?.id) { mutableStateOf(false) }
    var apiKey by remember(option.type, option.provider?.id, option.provider?.apiKey) {
        mutableStateOf(option.provider?.apiKey.orEmpty())
    }
    var revealKey by remember(option.type, option.provider?.id) { mutableStateOf(false) }
    var saving by remember(option.type, option.provider?.id) { mutableStateOf(false) }
    var saveError by remember(option.type, option.provider?.id) { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            onClick = {
                if (option.isReady && option.provider != null && option.model != null) {
                    if (isSelected) {
                        onModeChange(SearchMode.OFF)
                    } else {
                        onSelectModel(option.provider.id, option.model.id)
                        onModeChange(SearchMode.MODEL)
                    }
                    onAfterSelection()
                } else {
                    editing = true
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("native-search-provider-${option.type.name.lowercase()}"),
            shape = RoundedCornerShape(16.dp),
            color = if (isSelected) brandColor.copy(alpha = 0.12f) else AriTint,
            border = BorderStroke(
                1.dp,
                if (isSelected) brandColor.copy(alpha = 0.48f) else AriInk.copy(alpha = 0.09f)
            ),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                if (option.provider != null) {
                    ProviderBrandMark(provider = option.provider, size = 38.dp)
                } else {
                    ProviderBrandMark(type = option.type, name = option.title, size = 38.dp)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(tx(option.title), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AriInk, maxLines = 1)
                    Text(
                        when {
                            option.isReady -> "${option.model?.displayName.orEmpty()} · ${tx("Uses this AI provider API key")}"
                            option.hasUsableKey -> tx("Add a text model to enable web search")
                            option.provider != null && !option.provider.enabled -> tx("Provider is turned off; configure it here")
                            else -> tx("Add the provider API key to enable native search")
                        },
                        fontSize = 11.sp,
                        color = AriMuted,
                        maxLines = 2
                    )
                }
                if (isSelected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = tx("Selected"), tint = brandColor, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { editing = !editing; saveError = null },
                    modifier = Modifier.size(40.dp).testTag("native-search-settings-${option.type.name.lowercase()}")
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = tx(if (option.type == ProviderType.GEMINI) "Configure Gemini web search" else "Configure Anthropic web search"),
                        tint = if (editing) brandColor else AriMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (editing) {
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("native-search-editor-${option.type.name.lowercase()}"),
                shape = RoundedCornerShape(16.dp),
                color = AriCard,
                border = BorderStroke(1.dp, brandColor.copy(alpha = 0.20f)),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(tx("Provider API key"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AriInk)
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it; saveError = null },
                        label = { Text(tx("API key")) },
                        placeholder = { Text(tx("Paste the provider API key")) },
                        singleLine = true,
                        visualTransformation = if (revealKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { revealKey = !revealKey }) {
                                Icon(
                                    if (revealKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = tx(if (revealKey) "Hide key" else "Show key"),
                                    tint = AriMuted
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("native-search-key-${option.type.name.lowercase()}")
                    )
                    Text(
                        tx("Saved to the AI provider and reused for web search. No separate search key is needed."),
                        fontSize = 11.sp,
                        color = AriMuted
                    )
                    saveError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { editing = false; saveError = null },
                            enabled = !saving,
                            modifier = Modifier.weight(1f)
                        ) { Text(tx("Cancel"), maxLines = 1) }
                        Button(
                            onClick = {
                                val trimmedKey = apiKey.trim()
                                if (trimmedKey.isNotBlank()) {
                                    val base = option.provider ?: Provider(
                                        id = UUID.randomUUID().toString(),
                                        name = if (option.type == ProviderType.GEMINI) "Gemini" else "Anthropic",
                                        type = option.type,
                                        baseUrl = if (option.type == ProviderType.GEMINI) {
                                            "https://generativelanguage.googleapis.com/v1beta"
                                        } else {
                                            "https://api.anthropic.com/v1"
                                        },
                                        apiKey = trimmedKey,
                                        models = emptyList()
                                    )
                                    saving = true
                                    saveError = null
                                    onSaveProvider(
                                        base.copy(
                                            apiKey = trimmedKey,
                                            models = emptyList(),
                                            enabled = true,
                                            apiKeyNeedsReentry = false
                                        )
                                    ) { savedProvider ->
                                        saving = false
                                        val searchModel = savedProvider?.models?.firstOrNull { it.outputsText && it.id.isNotBlank() }
                                        if (savedProvider != null && searchModel != null) {
                                            apiKey = savedProvider.apiKey
                                            onSelectModel(savedProvider.id, searchModel.id)
                                            onModeChange(SearchMode.MODEL)
                                            editing = false
                                        } else {
                                            saveError = tx("Could not connect or load models. Check the API key and try again.")
                                        }
                                    }
                                }
                            },
                            enabled = apiKey.isNotBlank() && !saving,
                            modifier = Modifier.weight(1f).testTag("native-search-save-${option.type.name.lowercase()}")
                        ) {
                            if (saving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text(tx("Save provider key"), maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
