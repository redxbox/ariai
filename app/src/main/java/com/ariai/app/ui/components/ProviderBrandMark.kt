package com.ariai.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ariai.app.R
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import com.ariai.app.ui.theme.AriAccent
import com.ariai.app.ui.theme.AriAnthropic
import com.ariai.app.ui.theme.AriCustom
import com.ariai.app.ui.theme.AriGemini
import com.ariai.app.ui.theme.AriOllama
import com.ariai.app.ui.theme.AriOpenAI

/** A compact, consistent provider badge used across chat, settings, and model selection. */
@Composable
fun ProviderBrandMark(
    provider: Provider?,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    ProviderBrandMarkContent(
        type = provider?.type,
        name = provider?.name,
        modifier = modifier,
        size = size
    )
}

/** The same brand marks are used for provider presets before a Provider model exists. */
@Composable
fun ProviderBrandMark(
    type: ProviderType,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    ProviderBrandMarkContent(type = type, name = name, modifier = modifier, size = size)
}

@Composable
private fun ProviderBrandMarkContent(
    type: ProviderType?,
    name: String?,
    modifier: Modifier,
    size: Dp
) {
    val brandColor = providerBrandColor(type, name)
    val mark = providerMarkKind(type, name)
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.96f),
        border = BorderStroke(0.8.dp, brandColor.copy(alpha = 0.22f)),
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (mark) {
                ProviderMarkKind.OPENAI -> BrandVectorMark(R.drawable.brand_openai, size * 0.68f)
                ProviderMarkKind.GOOGLE -> BrandVectorMark(R.drawable.brand_google_g, size * 0.70f)
                ProviderMarkKind.ANTHROPIC -> BrandVectorMark(R.drawable.brand_anthropic, size * 0.62f)
                ProviderMarkKind.OLLAMA -> BrandVectorMark(R.drawable.brand_ollama, size * 0.68f)
                ProviderMarkKind.CUSTOM -> SpectrumSparkle(size * 0.64f)
                ProviderMarkKind.INITIAL -> Text(
                    name?.trim()?.firstOrNull()?.uppercase() ?: "A",
                    color = brandColor,
                    fontSize = (size.value * 0.43f).sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                ProviderMarkKind.NEURAL -> AriBinaryBrainMark(size = size * 0.68f, tint = brandColor)
            }
        }
    }
}

@Composable
private fun BrandVectorMark(resource: Int, iconSize: Dp) {
    Image(
        painter = painterResource(resource),
        contentDescription = null,
        modifier = Modifier.size(iconSize)
    )
}

private enum class ProviderMarkKind {
    OPENAI,
    GOOGLE,
    ANTHROPIC,
    OLLAMA,
    CUSTOM,
    INITIAL,
    NEURAL
}

private fun providerMarkKind(type: ProviderType?, name: String?): ProviderMarkKind {
    if (type == null) return ProviderMarkKind.NEURAL
    return when (type) {
        ProviderType.OPENAI -> ProviderMarkKind.OPENAI
        ProviderType.GEMINI -> ProviderMarkKind.GOOGLE
        ProviderType.ANTHROPIC -> ProviderMarkKind.ANTHROPIC
        ProviderType.OLLAMA -> ProviderMarkKind.OLLAMA
        ProviderType.OPENAI_COMPATIBLE, ProviderType.CUSTOM -> {
            val normalizedName = name.orEmpty().trim().lowercase()
            when {
                "openai" in normalizedName -> ProviderMarkKind.OPENAI
                "gemini" in normalizedName || "google" in normalizedName -> ProviderMarkKind.GOOGLE
                "anthropic" in normalizedName || "claude" in normalizedName -> ProviderMarkKind.ANTHROPIC
                "ollama" in normalizedName -> ProviderMarkKind.OLLAMA
                normalizedName.isBlank() || normalizedName == "custom" -> ProviderMarkKind.CUSTOM
                else -> ProviderMarkKind.INITIAL
            }
        }
    }
}

/** A small, original neural-brain mark with connected binary-style nodes for AriAI. */
@Composable
fun AriBinaryBrainMark(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    tint: Color = AriAccent
) {
    Canvas(modifier = modifier.size(size)) {
        val side = this.size.minDimension
        val unit = side / 24f
        val originX = (this.size.width - side) / 2f
        val originY = (this.size.height - side) / 2f
        withTransform({
            translate(left = originX, top = originY)
            scale(scaleX = unit, scaleY = unit, pivot = androidx.compose.ui.geometry.Offset.Zero)
        }) {
            val brain = Path().apply {
                moveTo(12f, 20f)
                cubicTo(10.8f, 21.1f, 8f, 20.5f, 7.9f, 17.9f)
                cubicTo(5.1f, 18.3f, 3.9f, 16f, 5.2f, 13.9f)
                cubicTo(3.2f, 12.4f, 3.7f, 9.2f, 6.2f, 8.5f)
                cubicTo(5.5f, 5.8f, 8.2f, 3.9f, 10.8f, 5.4f)
                cubicTo(10.5f, 2.8f, 13.5f, 2.8f, 13.2f, 5.4f)
                cubicTo(15.8f, 3.9f, 18.5f, 5.8f, 17.8f, 8.5f)
                cubicTo(20.3f, 9.2f, 20.8f, 12.4f, 18.8f, 13.9f)
                cubicTo(20.1f, 16f, 18.9f, 18.3f, 16.1f, 17.9f)
                cubicTo(16f, 20.5f, 13.2f, 21.1f, 12f, 20f)
                close()
            }
            drawPath(brain, tint.copy(alpha = 0.09f))
            drawPath(
                brain,
                tint,
                style = Stroke(width = 1.35f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawLine(tint.copy(alpha = 0.56f), androidx.compose.ui.geometry.Offset(12f, 5.6f), androidx.compose.ui.geometry.Offset(12f, 18.4f), 1f, cap = StrokeCap.Round)

            val connections = listOf(
                androidx.compose.ui.geometry.Offset(7.2f, 8.9f) to androidx.compose.ui.geometry.Offset(10.8f, 10.1f),
                androidx.compose.ui.geometry.Offset(6.6f, 12f) to androidx.compose.ui.geometry.Offset(10.7f, 12f),
                androidx.compose.ui.geometry.Offset(7.8f, 15.4f) to androidx.compose.ui.geometry.Offset(10.8f, 13.9f),
                androidx.compose.ui.geometry.Offset(16.8f, 8.9f) to androidx.compose.ui.geometry.Offset(13.2f, 10.1f),
                androidx.compose.ui.geometry.Offset(17.4f, 12f) to androidx.compose.ui.geometry.Offset(13.3f, 12f),
                androidx.compose.ui.geometry.Offset(16.2f, 15.4f) to androidx.compose.ui.geometry.Offset(13.2f, 13.9f)
            )
            connections.forEach { (start, end) ->
                drawLine(tint.copy(alpha = 0.68f), start, end, 0.9f, cap = StrokeCap.Round)
            }
            val nodes = listOf(
                androidx.compose.ui.geometry.Offset(7.2f, 8.9f),
                androidx.compose.ui.geometry.Offset(6.6f, 12f),
                androidx.compose.ui.geometry.Offset(7.8f, 15.4f),
                androidx.compose.ui.geometry.Offset(10.8f, 10.1f),
                androidx.compose.ui.geometry.Offset(10.7f, 12f),
                androidx.compose.ui.geometry.Offset(10.8f, 13.9f),
                androidx.compose.ui.geometry.Offset(16.8f, 8.9f),
                androidx.compose.ui.geometry.Offset(17.4f, 12f),
                androidx.compose.ui.geometry.Offset(16.2f, 15.4f),
                androidx.compose.ui.geometry.Offset(13.2f, 10.1f),
                androidx.compose.ui.geometry.Offset(13.3f, 12f),
                androidx.compose.ui.geometry.Offset(13.2f, 13.9f)
            )
            nodes.forEachIndexed { index, center ->
                val nodeColor = if (index % 3 == 1) AriGemini else tint
                drawCircle(Color.White, radius = 1.3f, center = center)
                drawCircle(nodeColor, radius = 0.83f, center = center)
            }
        }
    }
}

@Composable
private fun SpectrumSparkle(iconSize: Dp) {
    Canvas(modifier = Modifier.size(iconSize)) {
        val canvasWidth = this.size.width
        val canvasHeight = this.size.height
        val cx = canvasWidth / 2f
        val cy = canvasHeight / 2f
        val sparkle = Path().apply {
            moveTo(cx, 0f)
            cubicTo(cx + canvasWidth * 0.09f, cy - canvasHeight * 0.25f, cx + canvasWidth * 0.25f, cy - canvasHeight * 0.09f, canvasWidth, cy)
            cubicTo(cx + canvasWidth * 0.25f, cy + canvasHeight * 0.09f, cx + canvasWidth * 0.09f, cy + canvasHeight * 0.25f, cx, canvasHeight)
            cubicTo(cx - canvasWidth * 0.09f, cy + canvasHeight * 0.25f, cx - canvasWidth * 0.25f, cy + canvasHeight * 0.09f, 0f, cy)
            cubicTo(cx - canvasWidth * 0.25f, cy - canvasHeight * 0.09f, cx - canvasWidth * 0.09f, cy - canvasHeight * 0.25f, cx, 0f)
            close()
        }
        drawPath(
            sparkle,
            Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF4285F4),
                    Color(0xFF8A59D1),
                    Color(0xFFE74A7B),
                    Color(0xFFFFA53B),
                    Color(0xFF34A853),
                    Color(0xFF4285F4)
                ),
                center = androidx.compose.ui.geometry.Offset(cx, cy)
            )
        )
    }
}

fun providerBrandColor(provider: Provider?): Color = providerBrandColor(provider?.type, provider?.name)

private fun providerBrandColor(type: ProviderType?, name: String?): Color = when {
    type == null -> AriAccent
    type == ProviderType.OPENAI || name?.contains("openai", ignoreCase = true) == true -> AriOpenAI
    type == ProviderType.GEMINI || name?.contains("gemini", ignoreCase = true) == true -> AriGemini
    type == ProviderType.ANTHROPIC || name?.contains("anthropic", ignoreCase = true) == true -> AriAnthropic
    type == ProviderType.OLLAMA || name?.contains("ollama", ignoreCase = true) == true -> AriOllama
    else -> AriCustom
}
