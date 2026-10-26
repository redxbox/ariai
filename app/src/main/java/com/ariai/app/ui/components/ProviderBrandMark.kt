package com.ariai.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import com.ariai.app.ui.theme.AriAccent
import com.ariai.app.ui.theme.AriAnthropic
import com.ariai.app.ui.theme.AriCustom
import com.ariai.app.ui.theme.AriGemini
import com.ariai.app.ui.theme.AriOllama
import com.ariai.app.ui.theme.AriOpenAI

/** A small, original provider mark used consistently in the chat and model picker. */
@Composable
fun ProviderBrandMark(
    provider: Provider?,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val color = providerBrandColor(provider)
    val glyph = if (provider == null) {
        "✦"
    } else {
        when (provider.type) {
            ProviderType.OPENAI -> "✳"
            ProviderType.GEMINI -> "✦"
            ProviderType.ANTHROPIC -> "A"
            ProviderType.OLLAMA -> "O"
            ProviderType.OPENAI_COMPATIBLE, ProviderType.CUSTOM -> provider.name.trim().firstOrNull()?.uppercase() ?: "A"
        }
    }
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(size * 0.32f),
        color = color.copy(alpha = 0.13f),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.24f)),
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                glyph,
                color = color,
                fontSize = (size.value * 0.42f).sp,
                lineHeight = (size.value * 0.48f).sp
            )
        }
    }
}

fun providerBrandColor(provider: Provider?): Color = when {
    provider == null -> AriAccent
    provider.type == ProviderType.OPENAI || provider.name.contains("openai", ignoreCase = true) -> AriOpenAI
    provider.type == ProviderType.GEMINI || provider.name.contains("gemini", ignoreCase = true) -> AriGemini
    provider.type == ProviderType.ANTHROPIC || provider.name.contains("anthropic", ignoreCase = true) -> AriAnthropic
    provider.type == ProviderType.OLLAMA || provider.name.contains("ollama", ignoreCase = true) -> AriOllama
    else -> AriCustom
}
