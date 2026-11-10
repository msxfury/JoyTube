package com.example

import android.content.Context
import android.os.Debug
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.WebExtension
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class GeckoPrivacyFilterExtensionTest {
    @Test
    fun enabledExtensionLoadsBraveRustEngineAndEasyPrivacyList() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var runtime: GeckoRuntime
        instrumentation.runOnMainSync {
            runtime = GeckoRuntime.create(context, GeckoRuntimeSettings.Builder().build())
        }
        val disabledReady = CountDownLatch(1)
        val enabledReady = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>()
        val disabledResponse = AtomicReference<JSONObject?>()
        val enabledResponse = AtomicReference<JSONObject?>()
        val nativePort = AtomicReference<WebExtension.Port?>()
        val baselinePssKb = Debug.getPss()

        try {
            instrumentation.runOnMainSync {
                runtime.webExtensionController.ensureBuiltIn(
                    "resource://android/assets/joytube-privacy-filter/",
                    "joytube-privacy-filter@joytube.local"
                ).accept(
                    { extension ->
                        extension?.setMessageDelegate(object : WebExtension.MessageDelegate {
                            override fun onConnect(port: WebExtension.Port) {
                                port.setDelegate(object : WebExtension.PortDelegate {
                                    override fun onPortMessage(message: Any, port: WebExtension.Port) {
                                        val response = message as? JSONObject ?: return
                                        if (response.optString("type") != "ready") return
                                        when (response.optInt("requestId")) {
                                            43 -> { disabledResponse.set(response); disabledReady.countDown() }
                                            44 -> { enabledResponse.set(response); enabledReady.countDown() }
                                        }
                                    }
                                })
                                nativePort.set(port)
                                port.postMessage(JSONObject()
                                    .put("type", "configure")
                                    .put("enabled", false)
                                    .put("requestId", 43))
                            }
                        }, "com.example.joytube.privacy")
                    },
                    { error -> failure.set(error); disabledReady.countDown() }
                )
            }

            assertTrue("Disabled extension did not acknowledge configuration", disabledReady.await(90, TimeUnit.SECONDS))
            failure.get()?.let { throw AssertionError("GeckoView rejected the privacy extension", it) }
            val offResponse = disabledResponse.get()
            assertTrue("Extension did not acknowledge the disabled state", offResponse != null)
            assertTrue("Filtering should remain off before loading Rust: $offResponse", offResponse!!.optBoolean("ok") && !offResponse.optBoolean("enabled"))
            val disabledPssKb = Debug.getPss()

            instrumentation.runOnMainSync {
                nativePort.get()?.postMessage(JSONObject()
                    .put("type", "configure")
                    .put("enabled", true)
                    .put("requestId", 44))
            }
            assertTrue("Brave Rust engine and EasyPrivacy list did not initialize", enabledReady.await(90, TimeUnit.SECONDS))
            val onResponse = enabledResponse.get()
            assertTrue("Extension did not acknowledge the enabled state", onResponse != null)
            assertTrue("Rust engine or bundled EasyPrivacy list failed to initialize: $onResponse", onResponse!!.optBoolean("ok"))
            assertTrue("Filtering was not left enabled", onResponse.optBoolean("enabled"))
            val enabledPssKb = Debug.getPss()
            Log.i("JoyTubePrivacyTest", "PSS before Gecko: ${baselinePssKb}KB; with Gecko and filter off: ${disabledPssKb}KB; filter enabled: ${enabledPssKb}KB; incremental filter PSS: ${enabledPssKb - disabledPssKb}KB")
        } finally {
            instrumentation.runOnMainSync { runtime.shutdown() }
        }
    }
}
