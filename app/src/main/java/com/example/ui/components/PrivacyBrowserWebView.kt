package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

/** A basic in-app browser with best-effort ad host filtering; YouTube traffic is always allowed. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PrivacyBrowserWebView(
    url: String,
    blockAds: Boolean,
    blockPopups: Boolean,
    modifier: Modifier = Modifier
) {
    AndroidView(
            modifier = modifier,
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(Color.WHITE)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.javaScriptCanOpenWindowsAutomatically = !blockPopups
                    settings.setSupportMultipleWindows(!blockPopups)
                    webChromeClient = WebChromeClient()
                    webViewClient = filteringClient(blockAds, blockPopups)
                    loadUrl(url)
                }
            },
            update = { webView ->
                webView.webViewClient = filteringClient(blockAds, blockPopups)
                webView.settings.javaScriptCanOpenWindowsAutomatically = !blockPopups
                webView.settings.setSupportMultipleWindows(!blockPopups)
                if (webView.url != url) webView.loadUrl(url)
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.webChromeClient = null
                webView.webViewClient = WebViewClient()
                webView.destroy()
            }
    )
}

private val adHosts = setOf(
    "doubleclick.net",
    "googlesyndication.com",
    "googleadservices.com",
    "adnxs.com",
    "taboola.com",
    "outbrain.com"
)

private val youtubeHosts = setOf(
    "youtube.com",
    "youtube-nocookie.com",
    "youtu.be",
    "googlevideo.com",
    "ytimg.com"
)

private fun filteringClient(blockAds: Boolean, blockPopups: Boolean) = object : WebViewClient() {
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val host = request?.url?.host?.lowercase() ?: return null
        if (blockAds && isBlockedHost(host)) {
            return WebResourceResponse(
                "text/plain",
                "utf-8",
                ByteArrayInputStream(ByteArray(0))
            )
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        if (blockPopups && request?.hasGesture() == false && request.isForMainFrame) return true
        return false
    }
}

private fun isBlockedHost(host: String): Boolean {
    if (youtubeHosts.any { host == it || host.endsWith(".$it") }) return false
    return adHosts.any { host == it || host.endsWith(".$it") }
}
