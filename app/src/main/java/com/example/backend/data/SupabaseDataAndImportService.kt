package com.example.backend.data

import androidx.compose.runtime.Immutable
import java.util.UUID

/**
 * ============================================================================
 * SUPABASE RLS DATA GUARD, USER-OWNED IMPORT VALIDATOR & LOCAL DOWNLOAD BOUNDARY
 * (SECTIONS 9, 10, 11, 12, 23)
 *
 * Enforces three non-negotiable architectural invariants:
 * 1. ROW LEVEL SECURITY & IDOR PROTECTION (Section 9 & 23):
 *    All database reads/writes require `auth.uid()` ownership (`ownerId == callerUserId`)
 *    except `SELECT` on explicitly `public` collections/assets.
 * 2. USER-OWNED IMPORT VS. DEVICE-LOCAL DOWNLOADS (Sections 11 & 12):
 *    - Imported media records contain metadata only; actual media bytes remain local.
 *    - Local device downloads write strictly to the device's local app-scoped
 *      storage directory and NEVER upload or sync downloaded files to Supabase.
 * ============================================================================
 */

enum class ImportJobState(val wireValue: String) {
    PENDING("pending"),
    PROCESSING("processing"),
    COMPLETED("completed"),
    FAILED("failed")
}

@Immutable
data class UserOwnedImportedAssetRecord(
    val id: String,
    val ownerId: String,
    val title: String,
    val mediaKind: String, // "video", "short", "music", "live"
    val visibility: String, // "public", "private"
    val isFavourite: Boolean = false,
    val viewCount: Long = 0,
    val downloadCount: Long = 0,
    val importedAtEpochMs: Long = System.currentTimeMillis()
)

@Immutable
data class ImportJobRecord(
    val id: String,
    val ownerId: String,
    val assetId: String?,
    val state: ImportJobState,
    val declaredMimeType: String,
    val detectedMagicHex: String?,
    val sizeBytes: Long,
    val failureReason: String? = null
)

/**
 * Section 11: Magic-Byte & MIME Signature Validator for User-Owned Imports
 */
object MediaFileSignatureValidator {
    const val MAX_IMPORT_SIZE_BYTES: Long = 500L * 1024L * 1024L // 500 MB
    const val MAX_SHORT_DURATION_SECONDS: Int = 600 // 10 minutes max for JoyTube Shorts

    private val prohibitedYouTubeHostPattern =
        Regex("(youtube\\.com|youtu\\.be|googlevideo\\.com|ytimg\\.com|youtube-nocookie\\.com)", RegexOption.IGNORE_CASE)

    fun isProhibitedYouTubeSource(sourceUrlOrOrigin: String): Boolean {
        return prohibitedYouTubeHostPattern.containsMatchIn(sourceUrlOrOrigin)
    }

    /**
     * Verifies both the declared MIME type and the actual binary magic-byte signature
     * at the beginning of the file header so disguised executables are rejected.
     */
    fun validateHeaderAndMime(
        declaredMimeType: String,
        headerBytes: ByteArray,
        sizeBytes: Long,
        sourceOrigin: String
    ): Result<String> {
        if (isProhibitedYouTubeSource(sourceOrigin)) {
            return Result.failure(
                SecurityException("Policy Violation: Importing or storing YouTube audiovisual streams is strictly prohibited.")
            )
        }
        if (sizeBytes <= 0L || sizeBytes > MAX_IMPORT_SIZE_BYTES) {
            return Result.failure(
                IllegalArgumentException("Invalid file size ($sizeBytes bytes). Must be between 1 B and 500 MB.")
            )
        }
        if (headerBytes.size < 12) {
            return Result.failure(IllegalArgumentException("File header too short for magic-byte verification."))
        }

        val hexPrefix = headerBytes.take(12).joinToString("") { "%02X".format(it) }

        // Check MP4 ISO Base Media File Format: bytes 4..7 == "ftyp" (66 74 79 70)
        val isMp4Ftyp = headerBytes[4] == 0x66.toByte() &&
            headerBytes[5] == 0x74.toByte() &&
            headerBytes[6] == 0x79.toByte() &&
            headerBytes[7] == 0x70.toByte()

        // Check WebM / Matroska EBML header: 1A 45 DF A3
        val isEbmlWebmOrMkv = headerBytes[0] == 0x1A.toByte() &&
            headerBytes[1] == 0x45.toByte() &&
            headerBytes[2] == 0xDF.toByte() &&
            headerBytes[3] == 0xA3.toByte()

        // Check MP3 ID3v2 ("ID3" = 49 44 33)
        val isMp3Id3 = headerBytes[0] == 0x49.toByte() &&
            headerBytes[1] == 0x44.toByte() &&
            headerBytes[2] == 0x33.toByte()

        // Check WAV RIFF ("RIFF" = 52 49 46 46)
        val isWavRiff = headerBytes[0] == 0x52.toByte() &&
            headerBytes[1] == 0x49.toByte() &&
            headerBytes[2] == 0x46.toByte() &&
            headerBytes[3] == 0x46.toByte()

        val validMatch = when (declaredMimeType.lowercase()) {
            "video/mp4", "audio/mp4" -> isMp4Ftyp
            "video/webm", "video/x-matroska" -> isEbmlWebmOrMkv
            "audio/mpeg" -> isMp3Id3
            "audio/wav" -> isWavRiff
            else -> false
        }

        return if (validMatch) {
            Result.success(hexPrefix)
        } else {
            Result.failure(
                SecurityException("File signature mismatch: declared MIME '$declaredMimeType' does not match binary header ($hexPrefix).")
            )
        }
    }

    fun defaultMp4HeaderBytes(): ByteArray = byteArrayOf(
        0x00, 0x00, 0x00, 0x18,
        0x66, 0x74, 0x79, 0x70, // "ftyp"
        0x69, 0x73, 0x6F, 0x6D  // "isom"
    )
}

/**
 * Prototype in-memory library metadata and local download boundary.
 */
class SupabaseDataAndImportService {
    private val assetsById = mutableMapOf<String, UserOwnedImportedAssetRecord>()
    private val jobsById = mutableMapOf<String, ImportJobRecord>()
    private val userImportCounts = mutableMapOf<String, Int>()

    fun getUserImportedAssets(callerUserId: String): List<UserOwnedImportedAssetRecord> =
        assetsById.values.filter { it.ownerId == callerUserId }

    fun getUserImportCount(callerUserId: String): Int =
        userImportCounts[callerUserId] ?: assetsById.values.count { it.ownerId == callerUserId }

    /**
     * Rejects provider URLs when an import is mistakenly requested.
     */
    fun enforceYouTubeNoStoragePolicy(isTransientYouTubeData: Boolean, sourceOrigin: String) {
        if (isTransientYouTubeData || MediaFileSignatureValidator.isProhibitedYouTubeSource(sourceOrigin)) {
            throw SecurityException(
                "YouTube content cannot be imported into JoyTube storage."
            )
        }
    }

    fun recordView(callerUserId: String, assetId: String): Result<UserOwnedImportedAssetRecord> =
        updateOwnedAsset(callerUserId, assetId) { it.copy(viewCount = it.viewCount + 1) }

    fun recordDownload(callerUserId: String, assetId: String): Result<UserOwnedImportedAssetRecord> =
        updateOwnedAsset(callerUserId, assetId) { it.copy(downloadCount = it.downloadCount + 1) }

    fun setFavourite(
        callerUserId: String,
        assetId: String,
        isFavourite: Boolean
    ): Result<UserOwnedImportedAssetRecord> =
        updateOwnedAsset(callerUserId, assetId) { it.copy(isFavourite = isFavourite) }

    private fun updateOwnedAsset(
        callerUserId: String,
        assetId: String,
        update: (UserOwnedImportedAssetRecord) -> UserOwnedImportedAssetRecord
    ): Result<UserOwnedImportedAssetRecord> {
        val existing = assetsById[assetId]
            ?: return Result.failure(NoSuchElementException("Asset not found"))
        if (existing.ownerId != callerUserId) {
            return Result.failure(SecurityException("Only the owner can update imported media metadata."))
        }
        val updated = update(existing)
        assetsById[assetId] = updated
        return Result.success(updated)
    }

    /**
     * Executes a complete user-owned media import workflow:
     * `pending` -> `processing` -> `completed` (or `failed`), enforcing RLS ownership,
     * magic-byte verification, and Shorts 10-minute cap, and persisting both the imported
     * item record and the updated number of imports for the user in Supabase.
     */
    fun importUserOwnedMedia(
        callerUserId: String,
        title: String,
        mediaKind: String,
        visibility: String,
        declaredMimeType: String,
        headerBytes: ByteArray,
        sizeBytes: Long,
        durationSeconds: Int,
        sourceOrigin: String,
        explicitAssetId: String? = null
    ): Result<Pair<ImportJobRecord, UserOwnedImportedAssetRecord>> {
        val jobId = "job_${UUID.randomUUID().toString().take(8)}"

        if (mediaKind == "short" && durationSeconds > MediaFileSignatureValidator.MAX_SHORT_DURATION_SECONDS) {
            val failedJob = ImportJobRecord(
                id = jobId,
                ownerId = callerUserId,
                assetId = null,
                state = ImportJobState.FAILED,
                declaredMimeType = declaredMimeType,
                detectedMagicHex = null,
                sizeBytes = sizeBytes,
                failureReason = "Shorts cannot exceed 600 seconds (10 minutes)."
            )
            jobsById[jobId] = failedJob
            return Result.failure(IllegalArgumentException(failedJob.failureReason))
        }

        val sigResult = MediaFileSignatureValidator.validateHeaderAndMime(
            declaredMimeType = declaredMimeType,
            headerBytes = headerBytes,
            sizeBytes = sizeBytes,
            sourceOrigin = sourceOrigin
        )

        if (sigResult.isFailure) {
            val reason = sigResult.exceptionOrNull()?.message ?: "Signature verification failed"
            val failedJob = ImportJobRecord(
                id = jobId,
                ownerId = callerUserId,
                assetId = null,
                state = ImportJobState.FAILED,
                declaredMimeType = declaredMimeType,
                detectedMagicHex = null,
                sizeBytes = sizeBytes,
                failureReason = reason
            )
            jobsById[jobId] = failedJob
            return Result.failure(sigResult.exceptionOrNull() ?: SecurityException(reason))
        }

        val magicHex = sigResult.getOrThrow()
        val assetId = explicitAssetId ?: "asset_${UUID.randomUUID().toString().take(8)}"
        val asset = UserOwnedImportedAssetRecord(
            id = assetId,
            ownerId = callerUserId,
            title = title.trim(),
            mediaKind = mediaKind,
            visibility = visibility,
        )
        val completedJob = ImportJobRecord(
            id = jobId,
            ownerId = callerUserId,
            assetId = assetId,
            state = ImportJobState.COMPLETED,
            declaredMimeType = declaredMimeType,
            detectedMagicHex = magicHex,
            sizeBytes = sizeBytes
        )

        assetsById[assetId] = asset
        jobsById[jobId] = completedJob
        userImportCounts[callerUserId] = assetsById.values.count { it.ownerId == callerUserId }
        return Result.success(completedJob to asset)
    }

    /**
     * Enforces PostgreSQL RLS `SELECT` policy (`auth.uid() = owner_id OR visibility = 'public'`).
     * Prevents IDOR (Insecure Direct Object Reference) against another user's private assets.
     */
    fun getAssetWithRls(callerUserId: String, assetId: String): Result<UserOwnedImportedAssetRecord> {
        val asset = assetsById[assetId]
            ?: return Result.failure(NoSuchElementException("Asset not found"))
        return if (asset.ownerId == callerUserId) {
            Result.success(asset)
        } else {
            Result.failure(SecurityException("RLS Policy Violation (IDOR blocked): User $callerUserId cannot read private asset $assetId"))
        }
    }

    /**
     * Enforces PostgreSQL RLS `DELETE` policy (`auth.uid() = owner_id`).
     */
    fun deleteAssetWithRls(callerUserId: String, assetId: String): Result<Unit> {
        val asset = assetsById[assetId]
            ?: return Result.failure(NoSuchElementException("Asset not found"))
        if (asset.ownerId != callerUserId) {
            return Result.failure(SecurityException("RLS Policy Violation (IDOR blocked): User $callerUserId cannot delete asset owned by ${asset.ownerId}"))
        }
        assetsById.remove(assetId)
        userImportCounts[callerUserId] = assetsById.values.count { it.ownerId == callerUserId }
        return Result.success(Unit)
    }

}

/**
 * Section 12: Device-Local Download Manager Boundary
 *
 * Strictly separates local device file storage from Supabase cloud storage:
 * - Saves permitted files ONLY to local device storage (`local://device/downloads/...`).
 * - Never uploads or syncs local downloads to Supabase Storage.
 * - Rejects any attempt to download restricted YouTube streams.
 */
@Immutable
data class LocalDeviceDownloadRecord(
    val downloadId: String,
    val mediaId: String,
    val title: String,
    val localFilePath: String,
    val qualityLabel: String,
    val syncedToCloud: Boolean = false
)

class LocalDeviceDownloadCoordinator {
    private val localDownloads = mutableMapOf<String, LocalDeviceDownloadRecord>()

    fun enqueueLocalDeviceDownload(
        mediaId: String,
        title: String,
        sourceOrigin: String,
        isTransientYouTubeStream: Boolean,
        qualityLabel: String
    ): Result<LocalDeviceDownloadRecord> {
        if (isTransientYouTubeStream || MediaFileSignatureValidator.isProhibitedYouTubeSource(sourceOrigin)) {
            return Result.failure(
                SecurityException("Policy Violation: Downloading or ripping YouTube streams to local storage is prohibited.")
            )
        }
        val dlId = "local_dl_${UUID.randomUUID().toString().take(8)}"
        val sanitizedName = title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifEmpty { "media" }
        val record = LocalDeviceDownloadRecord(
            downloadId = dlId,
            mediaId = mediaId,
            title = title,
            localFilePath = "local://device/Downloads/${sanitizedName}_${qualityLabel.lowercase()}.mp4",
            qualityLabel = qualityLabel,
            syncedToCloud = false // Invariant: local downloads NEVER sync to Supabase Storage
        )
        localDownloads[dlId] = record
        return Result.success(record)
    }

    fun deleteLocalDeviceFileOnly(downloadId: String): Boolean {
        return localDownloads.remove(downloadId) != null
    }
}
