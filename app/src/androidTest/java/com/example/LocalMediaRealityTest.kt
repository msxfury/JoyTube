package com.example

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.backend.data.LocalMediaFileOperations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** On-device checks against ContentResolver, real file IO, and Media3's decoder pipeline. */
@RunWith(AndroidJUnit4::class)
class LocalMediaRealityTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val targetContext get() = instrumentation.targetContext
    private val testContext get() = instrumentation.context
    private val providerBase = Uri.parse("content://com.aistudio.joytube.xkvpzr.testmedia")

    private fun providerUri(name: String) = providerBase.buildUpon().appendPath(name).build()

    private fun createFixture(name: String, kind: String): Uri {
        val args = Bundle().apply { putString("name", name); putString("kind", kind) }
        val created = requireNotNull(testContext.contentResolver.call(providerBase, "create_fixture", null, args))
        assertTrue("Test fixture was not created", created.getLong("size") >= 0L)
        return providerUri(name)
    }

    @Test
    fun copies64MiBContentUriByteForByteAndReportsRealSize() {
        val source = createFixture("large_64m.mp4", "large_mp4")
        val digest = MessageDigest.getInstance("SHA-256")
        val block = ByteArray(128 * 1024) { index -> ((index * 31 + 17) and 0xff).toByte() }
        byteArrayOf(0, 0, 0, 24, 0x66, 0x74, 0x79, 0x70, 0x69, 0x73, 0x6f, 0x6d).copyInto(block)
        repeat(512) { digest.update(block) }

        val copied = LocalMediaFileOperations.copySelectedMedia(
            context = targetContext,
            source = source,
            outputDirectory = File(targetContext.cacheDir, "joytube_copies"),
            title = "large fixture"
        )
        assertEquals("video/mp4", copied.mimeType)
        assertEquals(64L * 1024L * 1024L, copied.sizeBytes)
        assertEquals(copied.sizeBytes, copied.file.length())
        assertEquals(digest.digest().toList(), sha256(copied.file).toList())
    }

    @Test
    fun realMp4ContentUriCopiesAndMedia3ReachesReady() {
        val source = createFixture("joytube_fixture_2s.mp4", "mp4")
        val result = LocalMediaFileOperations.copySelectedMedia(
            context = targetContext,
            source = source,
            outputDirectory = File(targetContext.cacheDir, "joytube_copies"),
            title = "playback fixture"
        )
        assertTrue(result.file.length() > 1_000)

        val terminal = CountDownLatch(1)
        val error = AtomicReference<PlaybackException?>()
        val playerRef = AtomicReference<ExoPlayer>()
        val duration = AtomicReference(0L)
        instrumentation.runOnMainSync {
            val player = ExoPlayer.Builder(targetContext).build()
            playerRef.set(player)
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                        duration.set(player.duration)
                        terminal.countDown()
                    }
                }
                override fun onPlayerError(playbackException: PlaybackException) {
                    error.set(playbackException)
                    terminal.countDown()
                }
            })
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(result.file)))
            player.prepare()
        }
        try {
            assertTrue("Media3 did not prepare the local MP4 within 15 seconds", terminal.await(15, TimeUnit.SECONDS))
            assertEquals(null, error.get())
        } finally {
            instrumentation.runOnMainSync { playerRef.get().release() }
        }
        assertTrue("Expected Media3 to expose a positive duration", duration.get() > 0L)
    }

    @Test
    fun rejectsLargeNonMediaAndEmptyMediaInsteadOfMarkingSuccess() {
        val unsupported = createFixture("large_64m.bin", "unsupported")
        val unsupportedResult = runCatching {
            LocalMediaFileOperations.copySelectedMedia(targetContext, unsupported, File(targetContext.cacheDir, "joytube_copies"), "bad")
        }
        assertTrue("application/octet-stream must be rejected", unsupportedResult.isFailure)

        val empty = createFixture("empty.mp4", "empty_mp4")
        val emptyResult = runCatching {
            LocalMediaFileOperations.copySelectedMedia(targetContext, empty, File(targetContext.cacheDir, "joytube_copies"), "empty")
        }
        assertTrue("A zero-byte MP4 must not complete", emptyResult.isFailure)
        assertFalse(emptyResult.getOrNull()?.file?.exists() == true)

        val forged = createFixture("forged.mp4", "forged_mp4")
        val forgedResult = runCatching {
            LocalMediaFileOperations.copySelectedMedia(targetContext, forged, File(targetContext.cacheDir, "joytube_copies"), "forged")
        }
        assertTrue("A large file with an MP4 extension but no ftyp signature must fail", forgedResult.isFailure)
    }

    private fun sha256(file: File): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(128 * 1024).use { input ->
            val bytes = ByteArray(128 * 1024)
            while (true) {
                val read = input.read(bytes)
                if (read < 0) break
                digest.update(bytes, 0, read)
            }
        }
        return digest.digest()
    }
}
