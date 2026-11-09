package com.example.backend.contracts

import androidx.compose.runtime.Immutable
import com.example.ui.components.ArtworkPreset
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * ============================================================================
 * JOYTUBE KEYLESS MULTIMEDIA CONTRACTS, DEDICATED ENTITY MODELS & PAGINATION
 * (VERSION v1 — SECTIONS 4, 5, 7, 8, 9, 10, 13, 16, 25, 29)
 * ============================================================================
 */
const val CONTRACT_VERSION_V1 = "v1"

/**
 * Capability Classification Matrix Statuses
 */
enum class CapabilitySupportStatus(val label: String, val badgeText: String) {
    OFFICIALLY_SUPPORTED("Supported Keyless Client Interface", "Verified Keyless"),
    TECHNICALLY_POSSIBLE_UNOFFICIAL("Keyless Client Interface (Surface / Signal Verified)", "Verified Client Surface"),
    UNSUPPORTED("Unsupported / Unverified Operation", "Unsupported")
}

@Immutable
data class CapabilityMatrixEntry(
    val featureDomain: String,
    val clientIdentifier: String,
    val status: CapabilitySupportStatus,
    val verifiedInterfaceReference: String,
    val architecturalBoundaryNote: String
)

object JoyTubeCapabilityMatrix {
    val entries: List<CapabilityMatrixEntry> = listOf(
        CapabilityMatrixEntry(
            featureDomain = "MusicProvider (Tracks, Albums, Artists, Playlists, Music Videos)",
            clientIdentifier = "WEB_REMIX (1.20241106.01.00)",
            status = CapabilitySupportStatus.TECHNICALLY_POSSIBLE_UNOFFICIAL,
            verifiedInterfaceReference = "BitChord Innertube.kt — POST /youtubei/v1/search & /youtubei/v1/browse (musicShelfRenderer -> musicResponsiveListItemRenderer)",
            architecturalBoundaryNote = "Keyless client-side integration. Distinguishes MusicTrack, MusicAlbum (MPREb_), MusicArtist (UC), MusicPlaylist (VL/PL), and MusicVideo using structured renderer metadata and browseId prefixes rather than title guessing."
        ),
        CapabilityMatrixEntry(
            featureDomain = "VideoProvider (Standard Long-Form Videos)",
            clientIdentifier = "WEB (2.20241106.01.00)",
            status = CapabilitySupportStatus.TECHNICALLY_POSSIBLE_UNOFFICIAL,
            verifiedInterfaceReference = "POST /youtubei/v1/search (params=EgIQAQ==, sectionListRenderer -> itemSectionRenderer -> videoRenderer)",
            architecturalBoundaryNote = "Independent from MusicProvider. Preserves nullable optional fields (distinguishing absent view count/duration from zero) and maintains isolated continuation tokens."
        ),
        CapabilityMatrixEntry(
            featureDomain = "ShortsProvider (Dedicated Shorts Surface & Signal Classifier)",
            clientIdentifier = "WEB (2.20241106.01.00) + JoyTube Owned Shorts",
            status = CapabilitySupportStatus.TECHNICALLY_POSSIBLE_UNOFFICIAL,
            verifiedInterfaceReference = "reelShelfRenderer -> shortsLockupViewModel (reelWatchEndpoint)",
            architecturalBoundaryNote = "Never classifies ordinary videos as Shorts based on duration alone. Records explicit classification source and confidence (VERIFIED_REEL_SURFACE, USER_OWNED_SHORT, or INFERRED_CANDIDATE)."
        ),
        CapabilityMatrixEntry(
            featureDomain = "LiveProvider (Active, Upcoming & Completed Broadcasts)",
            clientIdentifier = "WEB (2.20241106.01.00)",
            status = CapabilitySupportStatus.TECHNICALLY_POSSIBLE_UNOFFICIAL,
            verifiedInterfaceReference = "POST /youtubei/v1/search (params=EgJAAQ==, BADGE_STYLE_TYPE_LIVE_NOW, upcomingEventData.startTime)",
            architecturalBoundaryNote = "Derives ACTIVE_LIVE, UPCOMING, and COMPLETED states from verified badge/schedule fields (never from 'live' in title), records observation timestamp, and converts scheduled times to local timezone."
        ),
        CapabilityMatrixEntry(
            featureDomain = "JoyTube Accounts, Preferences & Permitted User Imports",
            clientIdentifier = "Supabase Auth (OIDC/PKCE) + PostgreSQL (RLS)",
            status = CapabilitySupportStatus.OFFICIALLY_SUPPORTED,
            verifiedInterfaceReference = "Supabase Auth + PostgreSQL Row-Level Security + Private Storage Bucket",
            architecturalBoundaryNote = "Public discovery never routes through Supabase as a mandatory proxy. Supabase stores only JoyTube-owned user records."
        )
    )
}

/**
 * Unified Search Filter Modes (Section 5 & 11)
 */
enum class UnifiedSearchMode(val label: String, val wireKey: String) {
    All("All", "all"),
    Music("Music", "music"),
    Videos("Videos", "videos"),
    Shorts("Shorts", "shorts"),
    Live("Live", "live");

    companion object {
        fun fromLabel(label: String): UnifiedSearchMode =
            entries.find { it.label.equals(label, ignoreCase = true) || it.wireKey.equals(label, ignoreCase = true) }
                ?: All
    }
}

/**
 * Independent Provider Identifiers (Section 5)
 */
enum class ProviderKind(val wireName: String, val displayLabel: String) {
    YOUTUBE_MUSIC_ADAPTER("music_provider", "MusicProvider (WEB_REMIX)"),
    YOUTUBE_VIDEO("video_provider", "VideoProvider (WEB)"),
    YOUTUBE_SHORTS_HEURISTIC("shorts_provider", "ShortsProvider (Reel Surface)"),
    YOUTUBE_LIVE("live_provider", "LiveProvider (Broadcasts)"),
    JOYTUBE_OWNED("joytube_owned", "JoyTube Library")
}

// ============================================================================
// SECTION 7: DEDICATED MUSIC ENTITY MODELS (BitChord WEB_REMIX Architecture)
// ============================================================================

enum class MusicEntityKind(val label: String) {
    TRACK("Song"),
    ALBUM("Album"),
    ARTIST("Artist"),
    PLAYLIST("Playlist"),
    MUSIC_VIDEO("Music Video")
}

@Immutable
data class MusicTrack(
    val providerId: String,
    val title: String,
    val artistNames: List<String>,
    val albumTitle: String? = null,
    val albumBrowseId: String? = null,
    val durationSeconds: Int? = null,
    val durationLabel: String? = null,
    val isExplicit: Boolean = false,
    val artworkPreset: ArtworkPreset = ArtworkPreset.SynthwaveStudio,
    val isPlayable: Boolean = true
)

@Immutable
data class MusicArtist(
    val browseId: String,
    val name: String,
    val subscriberCountText: String? = null,
    val artworkPreset: ArtworkPreset = ArtworkPreset.SynthwaveStudio
)

@Immutable
data class MusicAlbum(
    val browseId: String,
    val title: String,
    val artistNames: List<String>,
    val releaseYear: Int? = null,
    val albumTypeLabel: String = "Album", // "Album", "EP", "Single"
    val artworkPreset: ArtworkPreset = ArtworkPreset.SynthwaveStudio
)

@Immutable
data class MusicPlaylist(
    val browseId: String,
    val title: String,
    val ownerOrCuratorName: String? = null,
    val trackCountText: String? = null,
    val artworkPreset: ArtworkPreset = ArtworkPreset.SynthwaveStudio
)

@Immutable
data class MusicVideo(
    val providerId: String,
    val title: String,
    val artistOrChannelName: String,
    val durationSeconds: Int? = null,
    val durationLabel: String? = null,
    val viewCountText: String? = null,
    val popularityCount: Long? = null,
    val artworkPreset: ArtworkPreset = ArtworkPreset.SynthwaveStudio
)

// ============================================================================
// SECTION 8: DEDICATED VIDEO MODEL (`VideoResult`)
// Distinguishes absent/null metadata from zero or empty string.
// ============================================================================

@Immutable
data class VideoResult(
    val videoId: String,
    val title: String,
    val channelName: String,
    val channelId: String? = null,
    val durationSeconds: Int? = null, // null = not supplied by provider (never invented)
    val durationLabel: String? = null,
    val publishedTimeText: String? = null,
    val publishedAtEpochMs: Long? = null,
    val viewCount: Long? = null, // null = absent; 0L = explicitly zero views
    val viewCountText: String? = null,
    val isPlayable: Boolean = true,
    val artworkPreset: ArtworkPreset = ArtworkPreset.WorkspaceDesk
)

// ============================================================================
// SECTION 9: DEDICATED SHORTS MODEL (`ShortResult`) & CLASSIFICATION EVIDENCE
// ============================================================================

enum class ShortsVerificationState(val badgeLabel: String, val confidenceScore: Double) {
    NOT_APPLICABLE("", 0.0),
    VERIFIED_REEL_SURFACE("Verified Short (Reel Surface)", 1.0),
    USER_OWNED_SHORT("JoyTube Short", 1.0),
    HEURISTIC_CANDIDATE("Short Candidate (Uncertain Signal)", 0.55)
}

enum class ShortsClassificationSignal(val description: String) {
    EXPLICIT_REEL_WATCH_ENDPOINT("Explicit reelWatchEndpoint in shortsLockupViewModel"),
    JOYTUBE_OWNED_SHORT_DECLARATION("User-authored JoyTube Short (<= 10m)"),
    INFERRED_VERTICAL_AND_TAG("Indirect metadata signal (never treated as provider-confirmed Short)")
}

@Immutable
data class ShortResult(
    val shortId: String,
    val title: String,
    val creatorHandle: String,
    val durationSeconds: Int? = null,
    val durationLabel: String? = null,
    val viewsText: String? = null,
    val popularityCount: Long? = null,
    val verificationState: ShortsVerificationState,
    val classificationSignal: ShortsClassificationSignal,
    val artworkPreset: ArtworkPreset = ArtworkPreset.NorthernLights,
    val isPlayable: Boolean = true
)

// ============================================================================
// SECTION 10: DEDICATED LIVE MODEL (`LiveResult`) & TIMEZONE NORMALIZATION
// ============================================================================

enum class LiveBroadcastState(val badgeLabel: String) {
    NONE(""),
    ACTIVE_LIVE("LIVE NOW"),
    UPCOMING("UPCOMING"),
    COMPLETED("COMPLETED STREAM"),
    UNVERIFIED("LIVE UNVERIFIED")
}

@Immutable
data class LiveStreamMetadata(
    val broadcastState: LiveBroadcastState,
    val actualStartTimeIso: String? = null,
    val scheduledStartTimeIso: String? = null,
    val scheduledStartEpochMs: Long? = null,
    val actualEndTimeIso: String? = null,
    val concurrentViewers: Long? = null,
    val observedAtEpochMs: Long = System.currentTimeMillis(),
    val statusExpiresAtEpochMs: Long = System.currentTimeMillis() + 30_000L
) {
    /**
     * Converts the UTC scheduled start time into the user's local timezone for display.
     */
    fun formatScheduledLocalTime(zoneId: ZoneId = ZoneId.systemDefault()): String? {
        val epochMs = scheduledStartEpochMs ?: return scheduledStartTimeIso
        return try {
            val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm z").withZone(zoneId)
            formatter.format(Instant.ofEpochMilli(epochMs))
        } catch (_: Exception) {
            scheduledStartTimeIso
        }
    }
}

@Immutable
data class LiveResult(
    val videoId: String,
    val title: String,
    val channelName: String,
    val broadcastState: LiveBroadcastState,
    val concurrentViewers: Long? = null, // null when upcoming/completed or not supplied
    val scheduledStartEpochMs: Long? = null,
    val publishedOrStreamedText: String? = null,
    val observedAtEpochMs: Long,
    val statusExpiresAtEpochMs: Long,
    val artworkPreset: ArtworkPreset = ArtworkPreset.CyberpunkCityscape,
    val isPlayable: Boolean = true
)

// ============================================================================
// SECTION 16: COMMON PLAYBACK ITEM ABSTRACTION (`PlaybackItem`)
// Separates content discovery from playback coordination.
// ============================================================================

@Immutable
data class PlaybackItem(
    val id: String,
    val providerId: String,
    val providerKind: ProviderKind,
    val contentMode: UnifiedSearchMode,
    val title: String,
    val subtitleOrArtist: String,
    val artworkPreset: ArtworkPreset,
    val durationSeconds: Int? = null,
    val isLiveBroadcast: Boolean = false,
    val liveBroadcastState: LiveBroadcastState = LiveBroadcastState.NONE,
    val isPlayable: Boolean = true
)

// ============================================================================
// SECTION 11, 12 & 29: NORMALIZED SEARCH RESULT CONTRACT (`v1`)
// Retains source identity and category-specific entity payloads while providing
// a unified display & ranking representation.
// ============================================================================

@Immutable
data class NormalizedSearchResult(
    val contractVersion: String = CONTRACT_VERSION_V1,
    val id: String,
    val providerId: String,
    val providerKind: ProviderKind,
    val resultMode: UnifiedSearchMode,
    val title: String,
    val creatorOrChannelTitle: String,
    val channelId: String? = null,
    val publishedAtEpochMs: Long,
    val durationSeconds: Int,
    val durationLabel: String,
    val artworkPreset: ArtworkPreset,
    val thumbnailUrl: String? = null,
    val isEmbeddable: Boolean = true,
    val isTransientYouTubeData: Boolean = true,
    val shortsVerification: ShortsVerificationState = ShortsVerificationState.NOT_APPLICABLE,
    val liveMetadata: LiveStreamMetadata? = null,
    val musicCapabilityNote: String? = null,
    // Category-specific domain models preserved (Section 29)
    val musicEntityKind: MusicEntityKind? = null,
    val musicTrack: MusicTrack? = null,
    val musicAlbum: MusicAlbum? = null,
    val musicArtist: MusicArtist? = null,
    val musicPlaylist: MusicPlaylist? = null,
    val musicVideo: MusicVideo? = null,
    val videoResult: VideoResult? = null,
    val shortResult: ShortResult? = null,
    val liveResult: LiveResult? = null,
    // Verified popularity signal (null = not supplied by provider; never confused with 0)
    val verifiedPopularityCount: Long? = null,
    // Ranking breakdown components (Section 12: Score = w1*R + w2*C + w3*F + w4*P)
    val textRelevanceScore: Double = 0.0,
    val freshnessScore: Double = 0.0,
    val modeIntentScore: Double = 0.0,
    val metadataCompletenessScore: Double = 0.0,
    val popularityScore: Double = 0.0,
    val compositeScore: Double = 0.0
) {
    fun toPlaybackItem(): PlaybackItem = PlaybackItem(
        id = id,
        providerId = providerId,
        providerKind = providerKind,
        contentMode = resultMode,
        title = title,
        subtitleOrArtist = creatorOrChannelTitle,
        artworkPreset = artworkPreset,
        durationSeconds = durationSeconds.takeIf { it > 0 },
        isLiveBroadcast = resultMode == UnifiedSearchMode.Live,
        liveBroadcastState = liveMetadata?.broadcastState ?: LiveBroadcastState.NONE,
        isPlayable = isEmbeddable && (musicEntityKind == null || musicEntityKind == MusicEntityKind.TRACK || musicEntityKind == MusicEntityKind.MUSIC_VIDEO)
    )
}

// ============================================================================
// SECTION 13: INDEPENDENT PER-PROVIDER PAGINATION STATE & LOOP DETECTION
// ============================================================================

@Immutable
data class ProviderPaginationState(
    val providerKind: ProviderKind,
    val normalizedQuery: String,
    val categoryMode: UnifiedSearchMode,
    val nextContinuationToken: String? = null,
    val visitedTokens: Set<String> = emptySet(),
    val isLoadingNextPage: Boolean = false,
    val reachedEnd: Boolean = false,
    val haltedDueToDuplicateToken: Boolean = false
) {
    fun canLoadMore(): Boolean =
        !isLoadingNextPage && !reachedEnd && !haltedDueToDuplicateToken && !nextContinuationToken.isNullOrBlank()
}

/**
 * Section 25: Session Request Planning Mathematics
 *   TotalRequests = S * A * P
 */
object SessionRequestPlanner {
    fun estimateTotalRequests(
        searchesCountS: Int,
        activeAdaptersPerSearchA: Int,
        averagePagesPerAdapterP: Double
    ): Double {
        require(searchesCountS >= 0 && activeAdaptersPerSearchA >= 0 && averagePagesPerAdapterP >= 0.0) {
            "Request planning inputs must be non-negative"
        }
        return searchesCountS.toDouble() * activeAdaptersPerSearchA.toDouble() * averagePagesPerAdapterP
    }
}

enum class ProviderExecutionStatus {
    SUCCESS,
    PARTIAL_OR_UNSUPPORTED_CAPABILITY,
    SKIPPED_BY_FILTER,
    RATE_LIMITED,
    CIRCUIT_OPEN,
    TIMED_OUT,
    FAILED
}

@Immutable
data class ProviderStatusReport(
    val providerKind: ProviderKind,
    val status: ProviderExecutionStatus,
    val supportStatus: CapabilitySupportStatus,
    val itemCount: Int,
    val latencyMs: Long,
    val quotaUnitsUsed: Int,
    val diagnosticMessage: String
)

@Immutable
data class BackendErrorEnvelope(
    val version: String = CONTRACT_VERSION_V1,
    val requestId: String,
    val code: String,
    val category: ErrorCategory,
    val message: String,
    val isRetryable: Boolean
)

enum class ErrorCategory {
    NETWORK,
    TIMEOUT,
    SERVER,
    AUTHORIZATION,
    UNAVAILABLE_CONTENT,
    MALFORMED_RESPONSE,
    UNSUPPORTED_OPERATION,
    RATE_LIMITED,
    INVALID_PARAMETERS,
    UNAUTHORIZED,
    FORBIDDEN_OR_QUOTA_EXCEEDED,
    NOT_FOUND,
    TRANSIENT_UPSTREAM_FAILURE,
    CIRCUIT_BREAKER_OPEN,
    POLICY_VIOLATION_BLOCKED,
    CAPABILITY_UNSUPPORTED
}

/**
 * Opaque HMAC-Signed Continuation Cursor encapsulating independent per-provider tokens.
 */
@Immutable
data class OpaqueContinuationCursor(
    val normalizedQuery: String,
    val mode: UnifiedSearchMode,
    val pageNumber: Int,
    val providerTokens: Map<ProviderKind, String>,
    val issuedAtEpochMs: Long
) {
    fun encodeSigned(secret: String = "joytube-cursor-hmac-v1"): String {
        val tokensPart = providerTokens.entries
            .sortedBy { it.key.wireName }
            .joinToString(",") { "${it.key.wireName}=${it.value}" }
        val payload = "$CONTRACT_VERSION_V1|$normalizedQuery|${mode.wireKey}|$pageNumber|$issuedAtEpochMs|$tokensPart"
        val payloadB64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.toByteArray(StandardCharsets.UTF_8))
        val signature = hmacSha256UrlSafe(payloadB64, secret)
        return "$payloadB64.$signature"
    }

    companion object {
        fun decodeAndVerify(encodedCursor: String, secret: String = "joytube-cursor-hmac-v1"): OpaqueContinuationCursor? {
            val parts = encodedCursor.split(".")
            if (parts.size != 2) return null
            val payloadB64 = parts[0]
            val providedSig = parts[1]
            val expectedSig = hmacSha256UrlSafe(payloadB64, secret)
            if (providedSig != expectedSig) return null

            return try {
                val decoded = String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8)
                val fields = decoded.split("|")
                if (fields.size < 6 || fields[0] != CONTRACT_VERSION_V1) return null
                val query = fields[1]
                val mode = UnifiedSearchMode.fromLabel(fields[2])
                val page = fields[3].toIntOrNull() ?: return null
                val issuedAt = fields[4].toLongOrNull() ?: return null
                val tokenMap = mutableMapOf<ProviderKind, String>()
                if (fields[5].isNotBlank()) {
                    fields[5].split(",").forEach { kv ->
                        val pair = kv.split("=", limit = 2)
                        if (pair.size == 2) {
                            val pk = ProviderKind.entries.find { it.wireName == pair[0] }
                            if (pk != null) tokenMap[pk] = pair[1]
                        }
                    }
                }
                OpaqueContinuationCursor(
                    normalizedQuery = query,
                    mode = mode,
                    pageNumber = page,
                    providerTokens = tokenMap,
                    issuedAtEpochMs = issuedAt
                )
            } catch (_: Exception) {
                null
            }
        }

        private fun hmacSha256UrlSafe(data: String, secret: String): String {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
            val rawHmac = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac)
        }
    }
}

@Immutable
data class UnifiedSearchResponse(
    val version: String = CONTRACT_VERSION_V1,
    val requestId: String,
    val requestGenerationId: Long = 1L,
    val rawQuery: String,
    val normalizedQuery: String,
    val mode: UnifiedSearchMode,
    val results: List<NormalizedSearchResult>,
    val providerReports: List<ProviderStatusReport>,
    val providerPaginationStates: Map<ProviderKind, ProviderPaginationState> = emptyMap(),
    val nextContinuationCursor: String? = null,
    val totalLatencyMs: Long,
    val totalQuotaUnitsConsumed: Int = 0,
    val capabilityBannerMessage: String? = null,
    val error: BackendErrorEnvelope? = null
)
