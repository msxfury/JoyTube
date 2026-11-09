package com.example.backend.resilience

import androidx.compose.runtime.Immutable
import com.example.backend.contracts.ErrorCategory
import com.example.backend.contracts.ProviderKind
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * ============================================================================
 * QUOTA BUDGETING, TOKEN-BUCKET RATE LIMITING, CIRCUIT BREAKER & OBSERVABILITY
 * (SECTIONS 19, 20 & 24)
 * ============================================================================
 */

enum class YouTubeEndpointOperation(val apiName: String, val unitCost: Int) {
    SEARCH_LIST("search.list", 100),
    VIDEOS_LIST("videos.list", 1),
    CHANNELS_LIST("channels.list", 1),
    PLAYLISTS_LIST("playlists.list", 1),
    PLAYLIST_ITEMS_LIST("playlistItems.list", 1)
}

@Immutable
data class QuotaBudgetSnapshot(
    val dailyBudgetUnits: Int,
    val consumedUnits: Int,
    val remainingUnits: Int,
    val operationCounts: Map<YouTubeEndpointOperation, Int>,
    val isWarningThresholdReached: Boolean,
    val isExhausted: Boolean
)

/**
 * Section 19: Mathematical Quota Budget Manager
 *
 * Implements:
 *   Q_total = Σ (N_i * C_i)
 * where N_i is the number of calls to operation i and C_i is its unit cost.
 */
class QuotaBudgetManager(
    private val dailyBudgetUnits: Int = 10_000,
    private val warningThresholdRatio: Double = 0.80
) {
    private val counts = ConcurrentHashMap<YouTubeEndpointOperation, Int>()

    @Synchronized
    fun canAfford(operations: List<YouTubeEndpointOperation>): Boolean {
        val incrementalCost = operations.sumOf { it.unitCost }
        return calculateTotalConsumedUnits() + incrementalCost <= dailyBudgetUnits
    }

    @Synchronized
    fun recordOperations(vararg operations: YouTubeEndpointOperation): Int {
        var addedCost = 0
        for (op in operations) {
            counts[op] = (counts[op] ?: 0) + 1
            addedCost += op.unitCost
        }
        return addedCost
    }

    @Synchronized
    fun calculateTotalConsumedUnits(): Int {
        // Q_total = Σ (N_i * C_i)
        return YouTubeEndpointOperation.entries.sumOf { op ->
            val count = counts[op] ?: 0
            count * op.unitCost
        }
    }

    @Synchronized
    fun snapshot(): QuotaBudgetSnapshot {
        val consumed = calculateTotalConsumedUnits()
        val remaining = (dailyBudgetUnits - consumed).coerceAtLeast(0)
        return QuotaBudgetSnapshot(
            dailyBudgetUnits = dailyBudgetUnits,
            consumedUnits = consumed,
            remainingUnits = remaining,
            operationCounts = YouTubeEndpointOperation.entries.associateWith { counts[it] ?: 0 },
            isWarningThresholdReached = consumed >= (dailyBudgetUnits * warningThresholdRatio).toInt(),
            isExhausted = consumed >= dailyBudgetUnits
        )
    }

    @Synchronized
    fun reset() {
        counts.clear()
    }
}

/**
 * Section 19: Mathematical Token-Bucket Rate Limiter
 *
 * Implements:
 *   Tokens(t) = min(B, Tokens(t_prev) + r * Δt)
 * where:
 *   B  = bucket capacity
 *   r  = refill rate in tokens per second
 *   Δt = elapsed time in seconds
 */
class TokenBucketRateLimiter(
    val capacityB: Double,
    val refillRatePerSecondR: Double,
    initialTimestampMs: Long = System.currentTimeMillis()
) {
    private var availableTokens: Double = capacityB
    private var lastRefillTimestampMs: Long = initialTimestampMs

    @Synchronized
    fun currentTokens(nowMs: Long = System.currentTimeMillis()): Double {
        refill(nowMs)
        return availableTokens
    }

    @Synchronized
    fun tryAcquire(tokensRequired: Double = 1.0, nowMs: Long = System.currentTimeMillis()): Boolean {
        refill(nowMs)
        return if (availableTokens >= tokensRequired) {
            availableTokens -= tokensRequired
            true
        } else {
            false
        }
    }

    private fun refill(nowMs: Long) {
        val deltaSeconds = ((nowMs - lastRefillTimestampMs).coerceAtLeast(0L)).toDouble() / 1000.0
        availableTokens = min(capacityB, availableTokens + refillRatePerSecondR * deltaSeconds)
        lastRefillTimestampMs = nowMs
    }
}

/**
 * Section 17 & 20: Exponential Backoff with Jitter
 *
 * Implements:
 *   Delay(n) = min(MaxDelay, BaseDelay * 2^n) + Jitter
 */
object ExponentialBackoffPolicy {
    fun computeDelayMs(
        attemptIndex: Int,
        baseDelayMs: Long = 250L,
        maxDelayMs: Long = 4000L,
        maxJitterMs: Long = 120L,
        deterministicJitterMs: Long? = null
    ): Long {
        val safeAttempt = attemptIndex.coerceIn(0, 10)
        val exponential = (baseDelayMs.toDouble() * 2.0.pow(safeAttempt.toDouble())).toLong()
        val capped = min(maxDelayMs, exponential)
        val jitter = deterministicJitterMs ?: if (maxJitterMs > 0) Random.nextLong(0, maxJitterMs + 1) else 0L
        return capped + jitter
    }
}

/**
 * Section 17 & 20: Per-Provider Circuit Breaker
 * States: CLOSED -> OPEN -> HALF_OPEN
 */
enum class CircuitBreakerState {
    CLOSED,
    OPEN,
    HALF_OPEN
}

class ProviderCircuitBreaker(
    private val failureThreshold: Int = 3,
    private val openCooldownMs: Long = 15_000L
) {
    private var state: CircuitBreakerState = CircuitBreakerState.CLOSED
    private var consecutiveFailures: Int = 0
    private var openedAtEpochMs: Long = 0L

    @Synchronized
    fun currentState(nowMs: Long = System.currentTimeMillis()): CircuitBreakerState {
        if (state == CircuitBreakerState.OPEN && (nowMs - openedAtEpochMs) >= openCooldownMs) {
            state = CircuitBreakerState.HALF_OPEN
        }
        return state
    }

    @Synchronized
    fun allowRequest(nowMs: Long = System.currentTimeMillis()): Boolean {
        return when (currentState(nowMs)) {
            CircuitBreakerState.CLOSED, CircuitBreakerState.HALF_OPEN -> true
            CircuitBreakerState.OPEN -> false
        }
    }

    @Synchronized
    fun recordSuccess() {
        consecutiveFailures = 0
        state = CircuitBreakerState.CLOSED
    }

    @Synchronized
    fun recordFailure(nowMs: Long = System.currentTimeMillis()) {
        consecutiveFailures += 1
        if (consecutiveFailures >= failureThreshold || state == CircuitBreakerState.HALF_OPEN) {
            state = CircuitBreakerState.OPEN
            openedAtEpochMs = nowMs
        }
    }

    @Synchronized
    fun reset() {
        consecutiveFailures = 0
        state = CircuitBreakerState.CLOSED
        openedAtEpochMs = 0L
    }
}

/**
 * Section 24: Structured Operational Observability Engine
 * Tracks request correlation IDs, latency percentiles (p50, p95, p99), quota usage,
 * and error categories without logging PII, tokens, or restricted YouTube metadata.
 */
@Immutable
data class StructuredOperationalLog(
    val requestId: String,
    val timestampEpochMs: Long,
    val subsystem: String,
    val operation: String,
    val providerKind: ProviderKind? = null,
    val durationMs: Long,
    val quotaUnits: Int = 0,
    val cacheHit: Boolean = false,
    val errorCategory: ErrorCategory? = null,
    val sanitizedNote: String
)

@Immutable
data class LatencyPercentileSummary(
    val sampleCount: Int,
    val p50Ms: Long,
    val p95Ms: Long,
    val p99Ms: Long
)

class ObservabilityEngine(private val maxRetainedLogs: Int = 100) {
    private val logs = ArrayDeque<StructuredOperationalLog>()
    private val latenciesMs = mutableListOf<Long>()

    fun newRequestId(): String = "req_${UUID.randomUUID().toString().take(8)}"

    @Synchronized
    fun recordEvent(
        requestId: String,
        subsystem: String,
        operation: String,
        durationMs: Long,
        providerKind: ProviderKind? = null,
        quotaUnits: Int = 0,
        cacheHit: Boolean = false,
        errorCategory: ErrorCategory? = null,
        note: String = ""
    ) {
        // Strictly redact any accidental bearer tokens or secrets
        val redactedNote = note
            .replace(Regex("Bearer\\s+[A-Za-z0-9._\\-]+", RegexOption.IGNORE_CASE), "Bearer [REDACTED]")
            .replace(Regex("AIza[0-9A-Za-z\\-_]{20,}"), "[REDACTED_API_KEY]")

        val entry = StructuredOperationalLog(
            requestId = requestId,
            timestampEpochMs = System.currentTimeMillis(),
            subsystem = subsystem,
            operation = operation,
            providerKind = providerKind,
            durationMs = durationMs.coerceAtLeast(0L),
            quotaUnits = quotaUnits,
            cacheHit = cacheHit,
            errorCategory = errorCategory,
            sanitizedNote = redactedNote
        )
        if (logs.size >= maxRetainedLogs) {
            logs.removeFirst()
        }
        logs.addLast(entry)
        latenciesMs.add(durationMs.coerceAtLeast(0L))
    }

    @Synchronized
    fun computeLatencyPercentiles(): LatencyPercentileSummary {
        if (latenciesMs.isEmpty()) {
            return LatencyPercentileSummary(sampleCount = 0, p50Ms = 0L, p95Ms = 0L, p99Ms = 0L)
        }
        val sorted = latenciesMs.sorted()
        fun percentile(p: Double): Long {
            val idx = ((sorted.size - 1) * p).toInt().coerceIn(0, sorted.lastIndex)
            return sorted[idx]
        }
        return LatencyPercentileSummary(
            sampleCount = sorted.size,
            p50Ms = percentile(0.50),
            p95Ms = percentile(0.95),
            p99Ms = percentile(0.99)
        )
    }

    @Synchronized
    fun recentLogs(limit: Int = 20): List<StructuredOperationalLog> =
        logs.takeLast(limit).reversed()
}
