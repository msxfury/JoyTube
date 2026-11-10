package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class GeckoView145BlockingProbeTest {
    @Test
    fun webRequestBlockingCancelsARequestInPinnedGeckoView() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var runtime: GeckoRuntime
        instrumentation.runOnMainSync {
            runtime = GeckoRuntime.create(context, GeckoRuntimeSettings.Builder().build())
        }
        lateinit var session: GeckoSession
        instrumentation.runOnMainSync { session = GeckoSession() }
        val server = ServerSocket(0).apply { soTimeout = 500 }
        val blockedResourceRequests = AtomicInteger()
        val pageLoaded = CountDownLatch(1)
        val finishServer = AtomicInteger(0)

        val serverThread = Thread {
            try {
                while (finishServer.get() == 0) {
                    val socket = try {
                        server.accept()
                    } catch (_: SocketTimeoutException) {
                        continue
                    }
                    socket.use { client ->
                        val reader = client.getInputStream().bufferedReader()
                        val requestLine = reader.readLine().orEmpty()
                        val path = requestLine.split(' ').getOrNull(1).orEmpty()
                        while (reader.readLine()?.isNotEmpty() == true) { }
                        if (path == "/blocked-resource") blockedResourceRequests.incrementAndGet()
                        val body = if (path == "/page") {
                            "<html><body>ready<script>fetch('/blocked-resource').catch(()=>{});</script></body></html>"
                        } else "resource"
                        val bytes = body.toByteArray()
                        val output = client.getOutputStream()
                        output.write("HTTP/1.1 200 OK\r\nContent-Type: ${if (path == "/page") "text/html" else "text/plain"}\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        output.write(bytes)
                        output.flush()
                        if (path == "/page") pageLoaded.countDown()
                    }
                }
            } catch (_: Exception) {
                // The instrumentation assertion below reports whether the request escaped filtering.
            }
        }.apply { start() }

        try {
            val extensionInstalled = CountDownLatch(1)
            var extensionInstallError: Throwable? = null
            instrumentation.runOnMainSync {
                runtime.webExtensionController.ensureBuiltIn(
                    "resource://android/assets/joytube-privacy-probe/",
                    "joytube-geckoview-probe@joytube.local"
                ).accept(
                    { extensionInstalled.countDown() },
                    { error -> extensionInstallError = error; extensionInstalled.countDown() }
                )
            }
            check(extensionInstalled.await(30, TimeUnit.SECONDS)) { "Built-in extension installation timed out" }
            check(extensionInstallError == null) { "GeckoView 145 rejected the blocking extension: $extensionInstallError" }

            instrumentation.runOnMainSync {
                session.open(runtime)
                session.loadUri("http://127.0.0.1:${server.localPort}/page")
            }
            check(pageLoaded.await(20, TimeUnit.SECONDS)) { "GeckoView did not load the local probe page" }
            Thread.sleep(2_000)
            assertEquals("webRequestBlocking should cancel the matching subresource", 0, blockedResourceRequests.get())
        } finally {
            finishServer.set(1)
            runCatching { server.close() }
            instrumentation.runOnMainSync {
                session.close()
                runtime.shutdown()
            }
            serverThread.join(2_000)
        }
    }
}
