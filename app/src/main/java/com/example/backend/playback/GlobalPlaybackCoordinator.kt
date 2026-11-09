package com.example.backend.playback

import androidx.compose.runtime.Immutable
import com.example.backend.contracts.LiveBroadcastState
import com.example.backend.contracts.PlaybackItem
import com.example.backend.contracts.ProviderKind
import com.example.backend.contracts.UnifiedSearchMode
import com.example.ui.components.ArtworkPreset

/**
 * ============================================================================
 * GLOBAL PLAYBACK COORDINATOR & MINI-PLAYER STATE MACHINE (SECTIONS 16 & 17)
 *
 * Responsibilities:
 * 1. Maintains one authoritative representation of the current `PlaybackItem`,
 *    queue, and playback lifecycle state (`IDLE`, `PREPARING`, `READY`, `PLAYING`,
 *    `PAUSED`, `BUFFERING`, `COMPLETED`, `FAILED`) across the full player and
 *    the floating mini-player.
 * 2. Starts cleanly in `IDLE` with an empty queue (no pre-populated fake songs
 *    or template videos). The Mini-Player becomes visible only when the user
 *    actually plays an imported or discovered item.
 * 3. Preserves active playback across tab switches, scroll-down/up dock morphs,
 *    and settings/account navigation.
 * ============================================================================
 */

enum class GlobalPlaybackLifecycleState(val label: String) {
    IDLE("Idle"),
    PREPARING("Preparing"),
    LOADING("Loading"),
    READY("Ready"),
    PLAYING("Playing"),
    PAUSED("Paused"),
    BUFFERING("Buffering"),
    COMPLETED("Completed"),
    ENDED("Ended"),
    FAILED("Failed")
}

@Immutable
data class GlobalPlaybackSnapshot(
    val lifecycleState: GlobalPlaybackLifecycleState = GlobalPlaybackLifecycleState.IDLE,
    val activeMediaId: String = "",
    val activeTitle: String = "",
    val activeCreatorOrSource: String = "",
    val contentMode: UnifiedSearchMode = UnifiedSearchMode.Videos,
    val providerKind: ProviderKind = ProviderKind.JOYTUBE_OWNED,
    val artworkPreset: ArtworkPreset = ArtworkPreset.WorkspaceDesk,
    val currentPositionSec: Int = 0,
    val totalDurationSec: Int = 0,
    val isLiveStream: Boolean = false,
    val liveBroadcastState: LiveBroadcastState = LiveBroadcastState.NONE,
    val isMiniBarVisible: Boolean = false,
    val queue: List<PlaybackItem> = emptyList(),
    val queueIndex: Int = 0,
    val lastErrorReason: String? = null
)

class PlaybackCoordinator {
    private var currentSnapshot: GlobalPlaybackSnapshot = GlobalPlaybackSnapshot()

    @Synchronized
    fun snapshot(): GlobalPlaybackSnapshot = currentSnapshot

    /**
     * Transitions cleanly to a newly selected `PlaybackItem` (Music, Video, Short, or Live),
     * resetting position and replacing all stale metadata atomically.
     */
    @Synchronized
    fun selectAndPlay(item: PlaybackItem): GlobalPlaybackSnapshot {
        if (!item.isPlayable) {
            currentSnapshot = currentSnapshot.copy(
                lifecycleState = GlobalPlaybackLifecycleState.FAILED,
                lastErrorReason = "Selected item is a browse container and is not directly playable."
            )
            return currentSnapshot
        }
        val nextQueue = if (currentSnapshot.queue.any { it.id == item.id }) {
            currentSnapshot.queue
        } else {
            listOf(item) + currentSnapshot.queue
        }
        val nextIndex = nextQueue.indexOfFirst { it.id == item.id }.coerceAtLeast(0)

        currentSnapshot = GlobalPlaybackSnapshot(
            lifecycleState = GlobalPlaybackLifecycleState.PLAYING,
            activeMediaId = item.id,
            activeTitle = item.title,
            activeCreatorOrSource = item.subtitleOrArtist,
            contentMode = item.contentMode,
            providerKind = item.providerKind,
            artworkPreset = item.artworkPreset,
            currentPositionSec = 0,
            totalDurationSec = (item.durationSeconds ?: 180).coerceAtLeast(1),
            isLiveStream = item.isLiveBroadcast,
            liveBroadcastState = item.liveBroadcastState,
            isMiniBarVisible = true,
            queue = nextQueue,
            queueIndex = nextIndex,
            lastErrorReason = null
        )
        return currentSnapshot
    }

    @Synchronized
    fun togglePlayPause(): GlobalPlaybackSnapshot {
        if (currentSnapshot.activeMediaId.isBlank()) return currentSnapshot
        val nextState = when (currentSnapshot.lifecycleState) {
            GlobalPlaybackLifecycleState.PLAYING -> GlobalPlaybackLifecycleState.PAUSED
            else -> GlobalPlaybackLifecycleState.PLAYING
        }
        currentSnapshot = currentSnapshot.copy(lifecycleState = nextState)
        return currentSnapshot
    }

    @Synchronized
    fun skipToNext(): GlobalPlaybackSnapshot {
        val q = currentSnapshot.queue
        if (q.isEmpty()) return currentSnapshot
        val nextIdx = (currentSnapshot.queueIndex + 1) % q.size
        return selectAndPlay(q[nextIdx])
    }

    @Synchronized
    fun seekTo(seconds: Int): GlobalPlaybackSnapshot {
        if (currentSnapshot.activeMediaId.isBlank()) return currentSnapshot
        val clamped = seconds.coerceIn(0, currentSnapshot.totalDurationSec.coerceAtLeast(1))
        val reachedEnd = !currentSnapshot.isLiveStream && clamped >= currentSnapshot.totalDurationSec
        currentSnapshot = currentSnapshot.copy(
            currentPositionSec = clamped,
            lifecycleState = if (reachedEnd) GlobalPlaybackLifecycleState.COMPLETED else currentSnapshot.lifecycleState
        )
        return currentSnapshot
    }

    @Synchronized
    fun clearOnSignOut(): GlobalPlaybackSnapshot {
        currentSnapshot = GlobalPlaybackSnapshot()
        return currentSnapshot
    }
}

object YouTubePlayerViewportValidator {
    const val MIN_YOUTUBE_PLAYER_WIDTH_DP = 200
    const val MIN_YOUTUBE_PLAYER_HEIGHT_DP = 200

    fun isViewportCompliant(widthDp: Int, heightDp: Int): Boolean {
        return widthDp >= MIN_YOUTUBE_PLAYER_WIDTH_DP && heightDp >= MIN_YOUTUBE_PLAYER_HEIGHT_DP
    }
}
