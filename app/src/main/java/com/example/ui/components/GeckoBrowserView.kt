package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension

/** Reuses the Gecko runtime and owns the optional built-in privacy filter extension. */
private object JoyTubeGeckoRuntime {
    private const val EXTENSION_ID = "joytube-privacy-filter@joytube.local"
    private const val EXTENSION_ASSET = "resource://android/assets/joytube-privacy-filter/"
    private const val NATIVE_APP = "com.example.joytube.privacy"
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var instance: GeckoRuntime? = null
    private var extension: WebExtension? = null
    private var nativePort: WebExtension.Port? = null
    private var desiredEnabled = false
    private var nextRequestId = 0
    private var extensionSetupInProgress = false
    private val waitingCallbacks = mutableListOf<(Boolean) -> Unit>()
    private val pendingReady = mutableMapOf<Int, List<(Boolean) -> Unit>>()

    fun get(context: Context): GeckoRuntime = instance ?: synchronized(this) {
        instance ?: GeckoRuntime.create(
            context.applicationContext,
            GeckoRuntimeSettings.Builder()
                .preferredColorScheme(GeckoRuntimeSettings.COLOR_SCHEME_DARK)
                .build()
        ).also { instance = it }
    }

    fun preparePrivacyFilter(context: Context, enabled: Boolean, onReady: (Boolean) -> Unit) {
        val runtime = get(context)
        val shouldInstall: Boolean
        synchronized(this) {
            desiredEnabled = enabled
            waitingCallbacks += onReady
            if (extension != null && nativePort != null) {
                sendConfigurationLocked()
                return
            }
            shouldInstall = !extensionSetupInProgress
            if (shouldInstall) extensionSetupInProgress = true
        }
        if (!shouldInstall) return
        runtime.webExtensionController.ensureBuiltIn(EXTENSION_ASSET, EXTENSION_ID).accept(
            { installed ->
                mainHandler.post {
                    if (installed == null) {
                        synchronized(this) { extensionSetupInProgress = false }
                        finishAllReady(false)
                        return@post
                    }
                    synchronized(this) {
                        extensionSetupInProgress = false
                        extension = installed
                        installed.setMessageDelegate(NativeMessageDelegate, NATIVE_APP)
                        if (nativePort != null) sendConfigurationLocked()
                    }
                }
            },
            { error ->
                Log.e("JoyTubePrivacy", "Could not install GeckoView privacy extension", error)
                mainHandler.post {
                    synchronized(this) { extensionSetupInProgress = false }
                    finishAllReady(false)
                }
            }
        )
    }

    private fun sendConfigurationLocked() {
        val port = nativePort ?: return
        val requestId = nextRequestId++
        pendingReady[requestId] = waitingCallbacks.toList()
        waitingCallbacks.clear()
        runCatching {
            port.postMessage(JSONObject().put("type", "configure").put("enabled", desiredEnabled).put("requestId", requestId))
        }.onFailure { error ->
            Log.e("JoyTubePrivacy", "Could not send privacy settings to GeckoView", error)
            val callbacks = pendingReady.remove(requestId).orEmpty()
            mainHandler.post { callbacks.forEach { it(false) } }
        }
    }

    private fun finishAllReady(success: Boolean) {
        val callbacks = synchronized(this) {
            (waitingCallbacks + pendingReady.values.flatten()).also {
                waitingCallbacks.clear()
                pendingReady.clear()
            }
        }
        callbacks.forEach { callback -> callback(success) }
    }

    private object NativeMessageDelegate : WebExtension.MessageDelegate {
        override fun onConnect(port: WebExtension.Port) {
            port.setDelegate(object : WebExtension.PortDelegate {
                override fun onPortMessage(message: Any, port: WebExtension.Port) {
                    val response = message as? JSONObject ?: return
                    if (response.optString("type") != "ready") return
                    val requestId = response.optInt("requestId", -1)
                    val callbacks = synchronized(this@JoyTubeGeckoRuntime) {
                        pendingReady.remove(requestId).orEmpty()
                    }
                    callbacks.forEach { ready -> mainHandler.post { ready(response.optBoolean("ok")) } }
                }

                override fun onDisconnect(port: WebExtension.Port) {
                    synchronized(this@JoyTubeGeckoRuntime) {
                        if (nativePort === port) nativePort = null
                    }
                }
            })
            synchronized(this@JoyTubeGeckoRuntime) {
                nativePort = port
                if (extension != null) sendConfigurationLocked()
            }
        }
    }
}

private data class BrowserBinding(val session: GeckoSession, var requestedUrl: String, var filteringEnabled: Boolean, var opened: Boolean = false, var released: Boolean = false)

@Stable
class GeckoBrowserController {
    var canGoBack by mutableStateOf(false)
        private set
    var canGoForward by mutableStateOf(false)
        private set
    var privacyFilterError by mutableStateOf<String?>(null)
        private set

    private var session: GeckoSession? = null

    internal fun attach(session: GeckoSession) {
        this.session = session
        canGoBack = false
        canGoForward = false
    }

    internal fun detach(session: GeckoSession) {
        if (this.session === session) {
            this.session = null
            canGoBack = false
            canGoForward = false
        }
    }

    internal fun updateHistory(back: Boolean, forward: Boolean) {
        canGoBack = back
        canGoForward = forward
    }

    internal fun updatePrivacyFilterError(message: String?) {
        privacyFilterError = message
    }

    fun goBack() {
        if (canGoBack) session?.goBack()
    }

    fun goForward() {
        if (canGoForward) session?.goForward()
    }

    fun reload() {
        session?.reload()
    }
}

@Composable
fun GeckoBrowserView(
    url: String,
    controller: GeckoBrowserController,
    onLocationChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    privacyFilteringEnabled: Boolean = false
) {
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { context ->
            val session = GeckoSession()
            val binding = BrowserBinding(session, url, privacyFilteringEnabled)
            session.setNavigationDelegate(object : GeckoSession.NavigationDelegate {
                override fun onLoadRequest(
                    session: GeckoSession,
                    request: GeckoSession.NavigationDelegate.LoadRequest
                ): GeckoResult<AllowOrDeny>? {
                    val scheme = Uri.parse(request.uri).scheme?.lowercase()
                    if (request.target == GeckoSession.NavigationDelegate.TARGET_WINDOW_NEW &&
                        scheme in setOf("http", "https")
                    ) {
                        // Keep links that ask for a new browser window inside this JoyTube page.
                        session.loadUri(request.uri)
                        return GeckoResult.deny()
                    }
                    return null
                }

                override fun onLocationChange(
                    session: GeckoSession,
                    url: String?,
                    perms: List<GeckoSession.PermissionDelegate.ContentPermission>,
                    hasUserGesture: Boolean
                ) {
                    if (session !== binding.session || url.isNullOrBlank()) return
                    if (url.startsWith("https://") || url.startsWith("http://")) {
                        onLocationChanged(url)
                    }
                }

                override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                    if (session === binding.session) {
                        controller.updateHistory(canGoBack, controller.canGoForward)
                    }
                }

                override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                    if (session === binding.session) {
                        controller.updateHistory(controller.canGoBack, canGoForward)
                    }
                }
            })
            controller.attach(session)
            GeckoView(context).apply {
                tag = binding
                JoyTubeGeckoRuntime.preparePrivacyFilter(context, privacyFilteringEnabled) { ready ->
                    if (binding.released) return@preparePrivacyFilter
                    if (!ready && binding.filteringEnabled) {
                        Log.e("JoyTubePrivacy", "Privacy filtering was enabled but its engine did not start")
                        controller.updatePrivacyFilterError("Privacy filtering could not start. Open Privacy filtering in Settings and turn it off to continue browsing.")
                        return@preparePrivacyFilter
                    }
                    controller.updatePrivacyFilterError(null)
                    session.open(JoyTubeGeckoRuntime.get(context))
                    setSession(session)
                    binding.opened = true
                    session.loadUri(binding.requestedUrl)
                }
            }
        },
        update = { view ->
            val binding = view.tag as? BrowserBinding ?: return@AndroidView
            if (binding.filteringEnabled != privacyFilteringEnabled) {
                binding.filteringEnabled = privacyFilteringEnabled
                JoyTubeGeckoRuntime.preparePrivacyFilter(view.context, privacyFilteringEnabled) { ready ->
                    if (!ready && privacyFilteringEnabled) {
                        Log.e("JoyTubePrivacy", "Privacy filtering could not be enabled for this GeckoView session")
                        controller.updatePrivacyFilterError("Privacy filtering could not start. Open Privacy filtering in Settings and turn it off to continue browsing.")
                    } else {
                        controller.updatePrivacyFilterError(null)
                    }
                }
            }
            if (binding.requestedUrl != url) {
                binding.requestedUrl = url
                if (binding.opened) binding.session.loadUri(url)
            }
        },
        onRelease = { view ->
            val binding = view.tag as? BrowserBinding
            if (binding != null) binding.released = true
            view.releaseSession()
            binding?.session?.let { session ->
                controller.detach(session)
                session.close()
            }
            view.tag = null
        }
        )
        controller.privacyFilterError?.let { message ->
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xFF101114)),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(text = message, color = Color.White)
            }
        }
    }
}
