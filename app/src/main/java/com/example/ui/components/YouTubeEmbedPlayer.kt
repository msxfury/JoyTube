package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** Uses YouTube's own embedded player. It does not extract streams or bypass ads. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbedPlayer(
    videoId: String,
    captionsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    require(videoId.matches(Regex("[A-Za-z0-9_-]{11}"))) { "Invalid YouTube video ID" }
    var playerView by remember { mutableStateOf<WebView?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            playerView?.apply {
                stopLoading()
                webChromeClient = null
                webViewClient = WebViewClient()
                destroy()
            }
            playerView = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                playerView = this
                setBackgroundColor(Color.BLACK)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                loadEmbed(videoId, captionsEnabled)
            }
        },
        update = { webView ->
            val expectedUrl = embedUrl(videoId, captionsEnabled)
            if (webView.tag != expectedUrl) {
                webView.loadEmbed(videoId, captionsEnabled)
            }
        }
    )
}

private fun WebView.loadEmbed(videoId: String, captionsEnabled: Boolean) {
    val url = embedUrl(videoId, captionsEnabled)
    tag = url
    loadUrl(url, mapOf("Referer" to "https://www.youtube.com/"))
}

private fun embedUrl(videoId: String, captionsEnabled: Boolean): String {
    val captions = if (captionsEnabled) "&cc_lang_pref=en&cc_load_policy=1" else ""
    return "https://www.youtube.com/embed/$videoId?playsinline=1&controls=1&autoplay=0$captions"
}
