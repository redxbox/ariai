package com.ariai.app.ui.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.ariai.app.ui.theme.AriAccent
import org.json.JSONObject

private const val LOCAL_MARKDOWN_PAGE = "file:///android_asset/rendering/markdown.html"

/** Offline Markdown renderer for replies containing LaTeX or Mermaid diagrams. */
@Composable
internal fun LocalMarkdownRenderer(
    markdown: String,
    color: Color,
    fontSizeSp: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var contentHeightDp by remember(markdown) { mutableFloatStateOf(48f) }
    val webView = remember(context) {
        LocalMarkdownWebView(context).apply {
            onHeightMeasured = { measured -> contentHeightDp = measured.coerceIn(32f, 3_000f) }
            onExternalUrl = { url -> runCatching { uriHandler.openUri(url) } }
        }
    }
    DisposableEffect(webView) {
        onDispose { webView.disposeRenderer() }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { webView },
        update = { view -> view.render(markdown, color, AriAccent, fontSizeSp) },
        modifier = modifier.fillMaxWidth().height(contentHeightDp.dp)
    )
}

private class LocalMarkdownWebView(context: Context) : WebView(context) {
    private data class RenderRequest(val markdown: String, val color: String, val linkColor: String, val fontSize: Float)

    private var ready = false
    private var latestRequest: RenderRequest? = null
    var onHeightMeasured: ((Float) -> Unit)? = null
    var onExternalUrl: ((String) -> Unit)? = null

    init {
        setBackgroundColor(AndroidColor.TRANSPARENT)
        overScrollMode = View.OVER_SCROLL_NEVER
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
            allowFileAccess = true // The page and all rendering libraries are bundled app assets.
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            blockNetworkLoads = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                ready = true
                latestRequest?.let(::renderNow)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (url.startsWith("file:///android_asset/rendering/")) return false
                if (url.startsWith("https://", ignoreCase = true) ||
                    url.startsWith("http://", ignoreCase = true) ||
                    url.startsWith("mailto:", ignoreCase = true)) {
                    onExternalUrl?.invoke(url)
                }
                return true
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url.startsWith("file:///android_asset/rendering/")) return false
                if (url.startsWith("https://", ignoreCase = true) ||
                    url.startsWith("http://", ignoreCase = true) ||
                    url.startsWith("mailto:", ignoreCase = true)) {
                    onExternalUrl?.invoke(url)
                }
                return true
            }
        }
        loadUrl(LOCAL_MARKDOWN_PAGE)
    }

    fun render(markdown: String, color: Color, linkColor: Color, fontSizeSp: Float) {
        val cssColor = "#%06X".format(color.toArgb() and 0xFFFFFF)
        val cssLinkColor = "#%06X".format(linkColor.toArgb() and 0xFFFFFF)
        val request = RenderRequest(markdown, cssColor, cssLinkColor, fontSizeSp)
        if (request == latestRequest) return
        latestRequest = request
        if (ready) renderNow(request)
    }

    private fun renderNow(request: RenderRequest) {
        val script = "window.renderMarkdown(" +
            "${JSONObject.quote(request.markdown)}," +
            "${JSONObject.quote(request.color)}," +
            "${JSONObject.quote(request.linkColor)}," +
            "${request.fontSize.coerceIn(12f, 28f)});"
        evaluateJavascript(script) {
            postDelayed({
                evaluateJavascript("document.documentElement.scrollHeight.toString()") { height ->
                    val parsed = height?.trim('"')?.toFloatOrNull()
                    if (parsed != null && parsed.isFinite() && parsed > 0f) onHeightMeasured?.invoke(parsed + 8f)
                }
            }, 120L)
        }
    }

    fun disposeRenderer() {
        ready = false
        stopLoading()
        loadUrl("about:blank")
        removeAllViews()
        destroy()
    }
}
