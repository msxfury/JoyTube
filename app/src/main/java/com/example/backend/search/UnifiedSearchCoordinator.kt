package com.example.backend.search

import com.example.backend.contracts.*
import com.example.backend.providers.DiscoveryProviderAdapter
import com.example.backend.providers.ProviderSearchInput
import com.example.backend.resilience.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.pow

/**
 * ============================================================================
 * UNIFIED SEARCH COORDINATOR, QUERY NORMALIZER, RANKING & PAGINATION ENGINE
 * (SECTIONS 11, 12, 13, 24, 25)
 *
 * Implements:
 * 1. Unicode-safe Query Normalization Pipeline (NFC normalization, whitespace
 *    collapsing, control character stripping, preserving quotes/hyphens/accents).
 * 2. Generation-Numbered Request Cancellation (`requestGenerationId`) so stale
 *    responses from an older query can never overwrite a newer query.
 * 3. Federated Concurrent Execution across `MusicProvider`, `VideoProvider`,
 *    `ShortsProvider`, and `LiveProvider` with partial-failure tolerance and
 *    single-adapter isolation when an individual tab is selected.
 * 4. Mathematical Ranking Model (Section 12):
 *      Score = w1 * R + w2 * C + w3 * F + w4 * P
 *    with strict convexity constraint:
 *      w1 + w2 + w3 + w4 = 1.0
 *    Distinguishes missing popularity (`verifiedPopularityCount == null`) from
 *    explicit zero popularity (`0L`), and applies source-appropriate freshness.
 * 5. Independent Per-Provider Pagination (`ProviderPaginationState`) with
 *    duplicate continuation token loop detection and composite-key TTL caching.
 * ============================================================================
 */

object QueryNormalizationPipeline {
    const val MAX_QUERY_LENGTH = 160

    fun normalize(rawQuery: String): String {
        if (rawQuery.isBlank()) return ""
        val nfc = Normalizer.normalize(rawQuery, Normalizer.Form.NFC)
        val withoutControl = buildString(nfc.length) {
            for (ch in nfc) {
                if (!Character.isISOControl(ch)) {
                    append(ch)
                } else if (ch == '\t' || ch == '\n' || ch == '\r') {
                    append(' ')
                }
            }
        }
        return withoutControl
            .trim()
            .replace(Regex("\\s+"), " ")
            .take(MAX_QUERY_LENGTH)
    }
}

/**
 * Section 12: Cross-Provider Mathematical Ranking Weights
 *   Score = w1*R + w2*C + w3*F + w4*P
 *   subject to: w1 + w2 + w3 + w4 = 1.0
 */
data class RankingWeights(
    val wQueryRelevance: Double = 0.45,        // w1 (R: Textual relevance)
    val wFreshness: Double = 0.20,             // w3 (F: Source-appropriate freshness)
    val wModeIntent: Double = 0.20,            // w2 (C: Category relevance)
    val wMetadataCompleteness: Double = 0.15   // w4 (P / Completeness & Popularity signal)
) {
    init {
        val sum = wQueryRelevance + wFreshness + wModeIntent + wMetadataCompleteness
        require(abs(sum - 1.0) < 1e-6) {
            "Ranking weights must sum to 1.0 (w1 + w2 + w3 + w4 = 1), got $sum"
        }
        require(wQueryRelevance >= 0.0 && wFreshness >= 0.0 && wModeIntent >= 0.0 && wMetadataCompleteness >= 0.0) {
            "Ranking weights must be non-negative"
        }
    }
}

object CrossProviderRankingEngine {
    fun exponentialFreshnessDecay(ageDays: Double, lambda: Double = 0.08): Double {
        require(lambda > 0.0) { "Decay rate lambda must be > 0" }
        val safeAge = ageDays.coerceAtLeast(0.0)
        return exp(-lambda * safeAge).coerceIn(0.0, 1.0)
    }

    fun halfLifeFreshnessDecay(ageDays: Double, halfLifeDays: Double = 14.0): Double {
        require(halfLifeDays > 0.0) { "Half-life H must be > 0" }
        val safeAge = ageDays.coerceAtLeast(0.0)
        return 2.0.pow(-safeAge / halfLifeDays).coerceIn(0.0, 1.0)
    }

    fun computeQueryRelevanceR(normalizedQuery: String, item: NormalizedSearchResult): Double {
        if (normalizedQuery.isBlank()) return 0.75
        val qLower = normalizedQuery.lowercase()
        val titleLower = item.title.lowercase()
        val creatorLower = item.creatorOrChannelTitle.lowercase()

        if (titleLower == qLower) return 1.0
        var score = 0.0
        if (titleLower.startsWith(qLower)) score += 0.55
        else if (titleLower.contains(qLower)) score += 0.45

        if (creatorLower.contains(qLower)) score += 0.20

        val queryTokens = qLower.split(" ").filter { it.isNotBlank() }
        if (queryTokens.isNotEmpty()) {
            val matchedTokens = queryTokens.count { token ->
                titleLower.contains(token) || creatorLower.contains(token)
            }
            score += 0.35 * (matchedTokens.toDouble() / queryTokens.size.toDouble())
        }
        return score.coerceIn(0.0, 1.0)
    }

    fun computeCategoryRelevanceC(selectedMode: UnifiedSearchMode, item: NormalizedSearchResult): Double {
        return when (selectedMode) {
            UnifiedSearchMode.All -> {
                when (item.resultMode) {
                    UnifiedSearchMode.Music -> 0.90
                    UnifiedSearchMode.Videos -> 0.90
                    UnifiedSearchMode.Live -> if (item.liveMetadata?.broadcastState == LiveBroadcastState.ACTIVE_LIVE) 0.96 else 0.82
                    UnifiedSearchMode.Shorts -> if (item.shortsVerification == ShortsVerificationState.VERIFIED_REEL_SURFACE || item.shortsVerification == ShortsVerificationState.USER_OWNED_SHORT) 0.90 else 0.72
                    UnifiedSearchMode.All -> 0.85
                }
            }
            item.resultMode -> 1.0
            else -> 0.20
        }
    }

    /**
     * Source-appropriate freshness F in [0, 1] (Section 12):
     * - An active livestream (`ACTIVE_LIVE`) is immediately time-critical (`1.0`).
     * - Music albums/tracks use a longer half-life (`H = 60 days`) so older music
     *   catalogues are not penalized against recent uploads.
     * - Standard videos and Shorts use `H = 14 days`.
     */
    fun computeSourceAppropriateFreshnessF(item: NormalizedSearchResult, nowEpochMs: Long): Double {
        if (item.resultMode == UnifiedSearchMode.Live) {
            return when (item.liveMetadata?.broadcastState) {
                LiveBroadcastState.ACTIVE_LIVE -> 1.0
                LiveBroadcastState.UPCOMING -> 0.92
                LiveBroadcastState.COMPLETED -> 0.55
                else -> 0.50
            }
        }
        val ageDays = ((nowEpochMs - item.publishedAtEpochMs).coerceAtLeast(0L)).toDouble() / 86_400_000.0
        val halfLife = if (item.resultMode == UnifiedSearchMode.Music) 60.0 else 14.0
        return halfLifeFreshnessDecay(ageDays = ageDays, halfLifeDays = halfLife)
    }

    /**
     * Verified Popularity Normalization P in [0, 1] (Section 12):
     * - Distinguishes missing popularity (`verifiedPopularityCount == null`) from an
     *   explicit zero (`verifiedPopularityCount == 0L`).
     * - Missing popularity receives a neutral baseline (`0.55`) so providers that do
     *   not return view counts on tracks/albums are not treated as zero-popularity items.
     * - Explicit `0L` receives `0.0`.
     * - Positive counts are log10-scaled into `[0.05, 1.0]`.
     */
    fun computeNormalizedPopularityP(popularityCount: Long?): Double {
        if (popularityCount == null) return 0.55 // Missing != 0
        if (popularityCount <= 0L) return 0.0    // Explicit zero
        val logScaled = log10(1.0 + popularityCount.toDouble()) / 7.5 // ~31M views -> 1.0
        return logScaled.coerceIn(0.05, 1.0)
    }

    fun scoreAndRank(
        items: List<NormalizedSearchResult>,
        normalizedQuery: String,
        selectedMode: UnifiedSearchMode,
        nowEpochMs: Long = System.currentTimeMillis(),
        weights: RankingWeights = RankingWeights()
    ): List<NormalizedSearchResult> {
        return items.map { item ->
            val rScore = computeQueryRelevanceR(normalizedQuery, item)
            val cScore = computeCategoryRelevanceC(selectedMode, item)
            val fScore = computeSourceAppropriateFreshnessF(item, nowEpochMs)
            val pScore = computeNormalizedPopularityP(item.verifiedPopularityCount)

            val composite = (weights.wQueryRelevance * rScore) +
                (weights.wModeIntent * cScore) +
                (weights.wFreshness * fScore) +
                (weights.wMetadataCompleteness * pScore)

            item.copy(
                textRelevanceScore = rScore,
                modeIntentScore = cScore,
                freshnessScore = fScore,
                popularityScore = pScore,
                metadataCompletenessScore = pScore,
                compositeScore = composite.coerceIn(0.0, 1.0)
            )
        }.sortedByDescending { it.compositeScore }
    }
}

/**
 * Section 24: In-Memory TTL Cache Entry with Composite Key
 * Never caches credentials or permanent provider catalogues.
 */
data class SearchCacheKey(
    val normalizedQuery: String,
    val mode: UnifiedSearchMode,
    val continuationToken: String?
)

private data class CachedSearchEntry(
    val response: UnifiedSearchResponse,
    val expiresAtEpochMs: Long
)

class UnifiedSearchCoordinator(
    private val videoAdapter: DiscoveryProviderAdapter,
    private val shortsAdapter: DiscoveryProviderAdapter,
    private val liveAdapter: DiscoveryProviderAdapter,
    private val musicAdapter: DiscoveryProviderAdapter,
    private val rateLimiter: TokenBucketRateLimiter = TokenBucketRateLimiter(capacityB = 20.0, refillRatePerSecondR = 4.0),
    private val observability: ObservabilityEngine = ObservabilityEngine(),
    private val perProviderTimeoutMs: Long = 2_500L
) {
    private val generationCounter = AtomicLong(0L)
    private val paginationStates = ConcurrentHashMap<ProviderKind, ProviderPaginationState>()
    private val responseCache = ConcurrentHashMap<SearchCacheKey, CachedSearchEntry>()

    private val circuitBreakers = mapOf(
        ProviderKind.YOUTUBE_VIDEO to ProviderCircuitBreaker(),
        ProviderKind.YOUTUBE_SHORTS_HEURISTIC to ProviderCircuitBreaker(),
        ProviderKind.YOUTUBE_LIVE to ProviderCircuitBreaker(),
        ProviderKind.YOUTUBE_MUSIC_ADAPTER to ProviderCircuitBreaker()
    )

    fun currentGenerationId(): Long = generationCounter.get()

    fun nextGenerationId(): Long = generationCounter.incrementAndGet()

    fun isGenerationCurrent(generationId: Long): Boolean = generationCounter.get() == generationId

    fun circuitBreakerState(kind: ProviderKind): CircuitBreakerState =
        circuitBreakers[kind]?.currentState() ?: CircuitBreakerState.CLOSED

    fun getProviderPaginationState(kind: ProviderKind): ProviderPaginationState? =
        paginationStates[kind]

    fun resetCircuitBreakers() {
        circuitBreakers.values.forEach { it.reset() }
        paginationStates.clear()
        responseCache.clear()
    }

    suspend fun executeUnifiedSearch(
        rawQuery: String,
        mode: UnifiedSearchMode,
        continuationCursor: String? = null,
        maxResultsPerProvider: Int = 6,
        simulatedFailingProviders: Set<ProviderKind> = emptySet(),
        callerGenerationId: Long? = null
    ): UnifiedSearchResponse = coroutineScope {
        val generationId = callerGenerationId ?: nextGenerationId()
        val requestId = observability.newRequestId()
        val startedAtMs = System.currentTimeMillis()
        val normalizedQuery = QueryNormalizationPipeline.normalize(rawQuery)

        // Validate continuation cursor if supplied
        val decodedCursor: OpaqueContinuationCursor? = if (continuationCursor != null) {
            val verified = OpaqueContinuationCursor.decodeAndVerify(continuationCursor)
            if (verified == null) {
                return@coroutineScope UnifiedSearchResponse(
                    requestId = requestId,
                    requestGenerationId = generationId,
                    rawQuery = rawQuery,
                    normalizedQuery = normalizedQuery,
                    mode = mode,
                    results = emptyList(),
                    providerReports = emptyList(),
                    totalLatencyMs = System.currentTimeMillis() - startedAtMs,
                    totalQuotaUnitsConsumed = 0,
                    error = BackendErrorEnvelope(
                        requestId = requestId,
                        code = "INVALID_CONTINUATION_CURSOR",
                        category = ErrorCategory.INVALID_PARAMETERS,
                        message = "Continuation cursor signature verification failed.",
                        isRetryable = false
                    )
                )
            }
            verified
        } else null

        if (!rateLimiter.tryAcquire(1.0)) {
            return@coroutineScope UnifiedSearchResponse(
                requestId = requestId,
                requestGenerationId = generationId,
                rawQuery = rawQuery,
                normalizedQuery = normalizedQuery,
                mode = mode,
                results = emptyList(),
                providerReports = emptyList(),
                totalLatencyMs = System.currentTimeMillis() - startedAtMs,
                totalQuotaUnitsConsumed = 0,
                error = BackendErrorEnvelope(
                    requestId = requestId,
                    code = "RATE_LIMITED",
                    category = ErrorCategory.RATE_LIMITED,
                    message = "Search request rate limit reached. Please wait a moment.",
                    isRetryable = true
                )
            )
        }

        // Section 11: Individual tabs invoke ONLY their own provider adapter; All invokes all 4
        val activeAdapters: List<DiscoveryProviderAdapter> = when (mode) {
            UnifiedSearchMode.All -> listOf(musicAdapter, videoAdapter, shortsAdapter, liveAdapter)
            UnifiedSearchMode.Music -> listOf(musicAdapter)
            UnifiedSearchMode.Videos -> listOf(videoAdapter)
            UnifiedSearchMode.Shorts -> listOf(shortsAdapter)
            UnifiedSearchMode.Live -> listOf(liveAdapter)
        }

        val deferredOutputs = activeAdapters.map { adapter ->
            async {
                val pStart = System.currentTimeMillis()
                val breaker = circuitBreakers[adapter.providerKind]
                if (breaker != null && !breaker.allowRequest(pStart)) {
                    return@async Pair(
                        emptyList<NormalizedSearchResult>(),
                        ProviderStatusReport(
                            providerKind = adapter.providerKind,
                            status = ProviderExecutionStatus.CIRCUIT_OPEN,
                            supportStatus = adapter.capabilitySupportStatus,
                            itemCount = 0,
                            latencyMs = 0L,
                            quotaUnitsUsed = 0,
                            diagnosticMessage = "Circuit breaker OPEN for ${adapter.providerKind.displayLabel}"
                        ) to null
                    )
                }

                val incomingToken = decodedCursor?.providerTokens?.get(adapter.providerKind)
                val existingPag = paginationStates[adapter.providerKind]

                // Section 13: Prevent infinite pagination loops when a continuation token repeats
                if (incomingToken != null && existingPag != null && incomingToken in existingPag.visitedTokens) {
                    val haltedState = existingPag.copy(
                        isLoadingNextPage = false,
                        haltedDueToDuplicateToken = true,
                        nextContinuationToken = null
                    )
                    paginationStates[adapter.providerKind] = haltedState
                    return@async Pair(
                        emptyList<NormalizedSearchResult>(),
                        ProviderStatusReport(
                            providerKind = adapter.providerKind,
                            status = ProviderExecutionStatus.SUCCESS,
                            supportStatus = adapter.capabilitySupportStatus,
                            itemCount = 0,
                            latencyMs = 1L,
                            quotaUnitsUsed = 0,
                            diagnosticMessage = "Halted pagination: duplicate continuation token detected"
                        ) to null
                    )
                }

                try {
                    val output = withTimeout(perProviderTimeoutMs) {
                        adapter.search(
                            ProviderSearchInput(
                                normalizedQuery = normalizedQuery,
                                maxResults = maxResultsPerProvider,
                                pageToken = incomingToken,
                                simulateFailure = adapter.providerKind in simulatedFailingProviders
                            )
                        )
                    }
                    breaker?.recordSuccess()

                    val visited = (existingPag?.visitedTokens.orEmpty()) + listOfNotNull(incomingToken)
                    val isRepeatedNext = output.nextPageToken != null && output.nextPageToken in visited
                    val nextVisited = if (output.nextPageToken != null && !isRepeatedNext) visited else visited
                    paginationStates[adapter.providerKind] = ProviderPaginationState(
                        providerKind = adapter.providerKind,
                        normalizedQuery = normalizedQuery,
                        categoryMode = mode,
                        nextContinuationToken = if (isRepeatedNext || output.items.isEmpty()) null else output.nextPageToken,
                        visitedTokens = nextVisited,
                        isLoadingNextPage = false,
                        reachedEnd = output.nextPageToken == null || output.items.isEmpty(),
                        haltedDueToDuplicateToken = isRepeatedNext
                    )

                    val latency = (System.currentTimeMillis() - pStart).coerceAtLeast(1L)
                    observability.recordEvent(
                        requestId = requestId,
                        subsystem = "ProviderAdapter",
                        operation = "search_${adapter.providerKind.wireName}",
                        durationMs = latency,
                        providerKind = adapter.providerKind,
                        quotaUnits = output.quotaUnitsConsumed,
                        note = output.diagnosticMessage
                    )
                    Pair(
                        output.items,
                        ProviderStatusReport(
                            providerKind = adapter.providerKind,
                            status = ProviderExecutionStatus.SUCCESS,
                            supportStatus = output.supportStatus,
                            itemCount = output.items.size,
                            latencyMs = latency,
                            quotaUnitsUsed = output.quotaUnitsConsumed,
                            diagnosticMessage = output.diagnosticMessage
                        ) to output.nextPageToken
                    )
                } catch (e: Exception) {
                    breaker?.recordFailure()
                    val latency = (System.currentTimeMillis() - pStart).coerceAtLeast(1L)
                    observability.recordEvent(
                        requestId = requestId,
                        subsystem = "ProviderAdapter",
                        operation = "search_${adapter.providerKind.wireName}_failed",
                        durationMs = latency,
                        providerKind = adapter.providerKind,
                        errorCategory = ErrorCategory.TRANSIENT_UPSTREAM_FAILURE,
                        note = e.message ?: "Provider execution error"
                    )
                    Pair(
                        emptyList<NormalizedSearchResult>(),
                        ProviderStatusReport(
                            providerKind = adapter.providerKind,
                            status = ProviderExecutionStatus.FAILED,
                            supportStatus = adapter.capabilitySupportStatus,
                            itemCount = 0,
                            latencyMs = latency,
                            quotaUnitsUsed = 0,
                            diagnosticMessage = "Provider isolated failure: ${e.message ?: "Upstream error"}"
                        ) to null
                    )
                }
            }
        }

        val collected = deferredOutputs.map { it.await() }
        val rawItems = collected.flatMap { it.first }
        val reports = collected.map { it.second.first }
        val nextTokens = collected.mapNotNull { (_, pair) ->
            val report = pair.first
            val token = pair.second
            if (token != null) report.providerKind to token else null
        }.toMap()

        // Section 11: Deduplicate by verified (providerKind, providerId, resultMode) — NEVER by title alone!
        val deduplicated = rawItems
            .distinctBy { Triple(it.providerKind, it.providerId, it.resultMode) }

        val rankedResults = CrossProviderRankingEngine.scoreAndRank(
            items = deduplicated,
            normalizedQuery = normalizedQuery,
            selectedMode = mode
        )

        val nextCursor = if (nextTokens.isNotEmpty()) {
            OpaqueContinuationCursor(
                normalizedQuery = normalizedQuery,
                mode = mode,
                pageNumber = (decodedCursor?.pageNumber ?: 1) + 1,
                providerTokens = nextTokens,
                issuedAtEpochMs = System.currentTimeMillis()
            ).encodeSigned()
        } else null

        val capabilityNotice = when (mode) {
            UnifiedSearchMode.Music ->
                "MusicProvider (Keyless WEB_REMIX): Parses Tracks, Albums (MPREb_), Artists (UC), Playlists (VL), and Music Videos from structured renderer metadata."
            UnifiedSearchMode.Shorts ->
                "ShortsProvider: Distinguishes verified Reel surface items (shortsLockupViewModel / reelWatchEndpoint) and JoyTube Shorts from uncertain candidates."
            UnifiedSearchMode.Live ->
                "LiveProvider: Verifies ACTIVE_LIVE, UPCOMING (local timezone converted), and COMPLETED broadcasts from badge and schedule fields."
            UnifiedSearchMode.Videos ->
                "VideoProvider (Keyless WEB): Independent videoRenderer discovery preserving nullable optional metadata."
            UnifiedSearchMode.All ->
                "Federated Keyless Search coordinating MusicProvider, VideoProvider, ShortsProvider, and LiveProvider with partial-failure tolerance."
        }

        val totalQuota = reports.sumOf { it.quotaUnitsUsed }
        val totalLatency = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(1L)

        UnifiedSearchResponse(
            requestId = requestId,
            requestGenerationId = generationId,
            rawQuery = rawQuery,
            normalizedQuery = normalizedQuery,
            mode = mode,
            results = rankedResults,
            providerReports = reports,
            providerPaginationStates = paginationStates.toMap(),
            nextContinuationCursor = nextCursor,
            totalLatencyMs = totalLatency,
            totalQuotaUnitsConsumed = totalQuota,
            capabilityBannerMessage = capabilityNotice
        )
    }
}
