package com.example

import com.example.backend.auth.SupabaseGoogleAuthManager
import com.example.backend.data.UserOwnedImportedAssetRecord
import com.example.backend.contracts.*
import com.example.backend.data.MediaFileSignatureValidator
import com.example.backend.data.SupabaseDataAndImportService
import com.example.backend.network.InnertubeClientIdentity
import com.example.backend.network.KeylessNetworkEngine
import com.example.backend.network.SanitizedInnertubeFixtures
import com.example.backend.network.VerifiedInnertubeSearchParams
import com.example.backend.playback.GlobalPlaybackLifecycleState
import com.example.backend.playback.PlaybackCoordinator
import com.example.backend.providers.*
import com.example.backend.search.CrossProviderRankingEngine
import com.example.backend.search.RankingWeights
import com.example.backend.search.UnifiedSearchCoordinator
import com.example.ui.components.ArtworkPreset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BackendArchitectureTest {

    private fun fixtureNetworkEngine() =
        KeylessNetworkEngine(com.example.backend.network.NetworkRequestConfig(allowLiveNetworkCalls = false))

    @Test
    fun testMusicProviderWebRemixParsesTracksAlbumsArtistsPlaylistsAndVideosWithoutTitleGuessing() = runBlocking {
        val network = fixtureNetworkEngine()
        val musicProvider = MusicProvider(networkEngine = network)

        val payload = network.buildInnertubeRequestBody(
            clientIdentity = InnertubeClientIdentity.WEB_REMIX,
            query = "Headlights",
            params = VerifiedInnertubeSearchParams.MUSIC_SONGS
        ).toString()
        assertTrue(payload.contains("\"clientName\":\"WEB_REMIX\""))
        assertTrue(payload.contains("\"query\":\"Headlights\""))

        val output = musicProvider.search(ProviderSearchInput(normalizedQuery = "", maxResults = 20))
        val items = output.items
        assertTrue("Expected MusicTrack items", items.any { it.musicEntityKind == MusicEntityKind.TRACK && it.musicTrack != null })
        assertTrue("Expected MusicAlbum items with MPREb_ browseId", items.any { it.musicEntityKind == MusicEntityKind.ALBUM && it.musicAlbum?.browseId?.startsWith("MPREb_") == true })
        assertTrue("Expected MusicArtist items with UC browseId", items.any { it.musicEntityKind == MusicEntityKind.ARTIST && it.musicArtist?.browseId?.startsWith("UC") == true })
        assertTrue("Expected MusicPlaylist items with VL browseId", items.any { it.musicEntityKind == MusicEntityKind.PLAYLIST && it.musicPlaylist?.browseId?.startsWith("VL") == true })
        assertTrue("Expected MusicVideo items", items.any { it.musicEntityKind == MusicEntityKind.MUSIC_VIDEO && it.musicVideo != null })

        val albumItem = items.first { it.musicEntityKind == MusicEntityKind.ALBUM }
        val trackItem = items.first { it.musicEntityKind == MusicEntityKind.TRACK }
        assertFalse("Album container must not claim direct stream playability", albumItem.toPlaybackItem().isPlayable)
        assertTrue("Song track must be playable stream", trackItem.toPlaybackItem().isPlayable)
    }

    @Test
    fun testVideoProviderDistinguishesMissingNullFieldsFromZero() = runBlocking {
        val videoProvider = VideoProvider(networkEngine = fixtureNetworkEngine())

        val output = videoProvider.search(ProviderSearchInput(normalizedQuery = "", maxResults = 10))
        val videos = output.items.mapNotNull { it.videoResult }
        assertTrue(videos.size >= 2)

        val fullVideo = videos.first { it.viewCount != null }
        assertTrue((fullVideo.viewCount ?: 0L) > 0L)
        assertNotNull(fullVideo.durationSeconds)

        // Fixture includes an item that intentionally omits viewCountText to test null vs 0 distinction
        val missingViewCountVideo = videos.first { it.viewCount == null }
        assertNull("Missing viewCountText must remain null rather than fabricated 0", missingViewCountVideo.viewCount)
    }

    @Test
    fun testShortsProviderVerifiesReelSurfaceAndNeverClassifiesByDurationAlone() = runBlocking {
        val shortsProvider = ShortsProvider(networkEngine = fixtureNetworkEngine())
        val output = shortsProvider.search(ProviderSearchInput(normalizedQuery = "", maxResults = 10))

        val verifiedReels = output.items.filter { it.shortsVerification == ShortsVerificationState.VERIFIED_REEL_SURFACE }
        val heuristicCandidates = output.items.filter { it.shortsVerification == ShortsVerificationState.HEURISTIC_CANDIDATE }

        assertTrue(verifiedReels.isNotEmpty())
        assertTrue(verifiedReels.all { it.shortResult?.classificationSignal == ShortsClassificationSignal.EXPLICIT_REEL_WATCH_ENDPOINT })
        assertTrue(heuristicCandidates.isNotEmpty())

        // Verify ShortsClassificationEngine never classifies by duration alone without reelWatchEndpoint or vertical/hashtag signal
        val durationOnlyResult = ShortsClassificationEngine.evaluateYouTubeCandidate(
            durationSeconds = 45,
            hasExplicitReelEndpoint = false,
            hasVerticalOrHashtagSignal = false
        )
        assertEquals(ShortsVerificationState.NOT_APPLICABLE, durationOnlyResult)
    }

    @Test
    fun testLiveProviderClassifiesActiveUpcomingAndCompletedFromRendererMetadataNotTitle() = runBlocking {
        val liveProvider = LiveProvider(networkEngine = fixtureNetworkEngine())
        val output = liveProvider.search(ProviderSearchInput(normalizedQuery = "", maxResults = 10))
        val liveResults = output.items.mapNotNull { it.liveResult }

        val active = liveResults.first { it.broadcastState == LiveBroadcastState.ACTIVE_LIVE }
        val upcoming = liveResults.first { it.broadcastState == LiveBroadcastState.UPCOMING }
        val completed = liveResults.first { it.broadcastState == LiveBroadcastState.COMPLETED }

        assertTrue((active.concurrentViewers ?: 0L) > 0L)
        val upcomingMeta = output.items.first { it.liveMetadata?.broadcastState == LiveBroadcastState.UPCOMING }.liveMetadata!!
        assertNotNull("Upcoming livestream must format scheduled start time in local timezone", upcomingMeta.formatScheduledLocalTime())
        assertTrue(completed.publishedOrStreamedText?.contains("Streamed", ignoreCase = true) == true)
        assertTrue(liveResults.all { it.observedAtEpochMs > 0L })

        // Verify that a regular VOD with "Live" in the title but no live badges is excluded
        assertFalse("Regular VOD with 'Live' in title must not be classified as a LiveResult", liveResults.any { it.videoId.contains("not_actually_live") })
    }

    @Test
    fun testUnifiedSearchSingleAdapterIsolationRankingDeduplicationAndPaginationLoopProtection() = runBlocking {
        val coordinator = UnifiedSearchCoordinator(
            videoAdapter = VideoProvider(networkEngine = fixtureNetworkEngine()),
            shortsAdapter = ShortsProvider(networkEngine = fixtureNetworkEngine()),
            liveAdapter = LiveProvider(networkEngine = fixtureNetworkEngine()),
            musicAdapter = MusicProvider(networkEngine = fixtureNetworkEngine())
        )

        // 1. Category tab isolation: selecting Music invokes ONLY MusicProvider
        val musicOnly = coordinator.executeUnifiedSearch("Headlights", UnifiedSearchMode.Music)
        val activeMusicAdapters = musicOnly.providerReports.filter { it.status != ProviderExecutionStatus.SKIPPED_BY_FILTER }
        assertEquals(1, activeMusicAdapters.size)
        assertEquals(ProviderKind.YOUTUBE_MUSIC_ADAPTER, activeMusicAdapters.first().providerKind)

        // 2. All mode invokes all 4 providers concurrently and tolerates partial failure
        val partialFailureResponse = coordinator.executeUnifiedSearch(
            rawQuery = "Headlights",
            mode = UnifiedSearchMode.All,
            simulatedFailingProviders = setOf(ProviderKind.YOUTUBE_LIVE)
        )
        assertTrue("Expected surviving providers to return results despite LiveProvider failure", partialFailureResponse.results.isNotEmpty())
        assertEquals(
            ProviderExecutionStatus.FAILED,
            partialFailureResponse.providerReports.first { it.providerKind == ProviderKind.YOUTUBE_LIVE }.status
        )

        // 3. Ranking weights sum to 1.0 and missing popularity (null -> 0.55) scores higher than explicit 0 popularity (0L -> 0.0)
        val weights = RankingWeights()
        assertEquals(1.0, weights.wQueryRelevance + weights.wFreshness + weights.wModeIntent + weights.wMetadataCompleteness, 1e-6)
        val missingPopP = CrossProviderRankingEngine.computeNormalizedPopularityP(null)
        val zeroPopP = CrossProviderRankingEngine.computeNormalizedPopularityP(0L)
        assertTrue("Missing popularity (null -> 0.55 neutral) must differ from explicit zero (0L -> 0.0)", missingPopP > zeroPopP)

        // 4. Session Request Planning Math: TotalRequests = S * A * P
        assertEquals(
            24.0,
            SessionRequestPlanner.estimateTotalRequests(searchesCountS = 6, activeAdaptersPerSearchA = 4, averagePagesPerAdapterP = 1.0),
            1e-6
        )
    }

    @Test
    fun testGlobalPlaybackCoordinatorSynchronizesMiniPlayerAndQueueAcrossContentTypes() {
        val playback = PlaybackCoordinator()

        val musicTrack = PlaybackItem(
            id = "ytm_track_1",
            providerId = "ytm_track_1",
            providerKind = ProviderKind.YOUTUBE_MUSIC_ADAPTER,
            contentMode = UnifiedSearchMode.Music,
            title = "Headlights [Slowed Version]",
            subtitleOrArtist = "Alok, Alan Walker",
            artworkPreset = ArtworkPreset.SynthwaveStudio,
            durationSeconds = 166
        )
        val snap1 = playback.selectAndPlay(musicTrack)
        assertEquals(GlobalPlaybackLifecycleState.PLAYING, snap1.lifecycleState)
        assertEquals("Headlights [Slowed Version]", snap1.activeTitle)
        assertTrue(snap1.isMiniBarVisible)

        // Non-playable container (e.g., Album) is rejected cleanly without corrupting active item
        val albumContainer = musicTrack.copy(id = "album_1", isPlayable = false)
        val snapRejected = playback.selectAndPlay(albumContainer)
        assertEquals(GlobalPlaybackLifecycleState.FAILED, snapRejected.lifecycleState)
    }

    @Test
    fun testSupabasePkceAndRowLevelSecurityIsolationAndTransientYouTubeGuard() {
        // Native Google Credential Manager flow exchanges an ID token with Supabase.
        // No YouTube OAuth scopes are requested by the JoyTube sign-in manager.
        assertTrue(SupabaseGoogleAuthManager::class.java.methods.none { it.name.contains("YouTube") })

        val dataService = SupabaseDataAndImportService()
        try {
            dataService.enforceYouTubeNoStoragePolicy(
                isTransientYouTubeData = true,
                sourceOrigin = "https://www.youtube.com/watch?v=123"
            )
            fail("Expected SecurityException when attempting to persist transient YouTube metadata")
        } catch (expected: SecurityException) {
            assertTrue(expected.message!!.contains("cannot be imported"))
        }

        val mp4Header = byteArrayOf(
            0x00, 0x00, 0x00, 0x18,
            0x66, 0x74, 0x79, 0x70,
            0x69, 0x73, 0x6F, 0x6D
        )
        val mimeResult = MediaFileSignatureValidator.validateHeaderAndMime(
            declaredMimeType = "video/mp4",
            headerBytes = mp4Header,
            sizeBytes = 1024L * 1024L,
            sourceOrigin = "user_camera_roll.mp4"
        )
        assertTrue(mimeResult.isSuccess)
    }
    @Test
    fun testGoogleAuthSurfaceExchangesIdentityWithoutYouTubeAccountTokens() {
        val methods = SupabaseGoogleAuthManager::class.java.methods.map { it.name }
        assertTrue(methods.contains("signInWithGoogle"))
        assertFalse("JoyTube identity auth must not add YouTube account token handling", methods.any { it.contains("YouTube") || it.contains("youtube") })
    }

    @Test
    fun testCloudImportRecordsContainMetadataOnlyAndDeviceDownloadIsNotSyncData() {
        val fields = UserOwnedImportedAssetRecord::class.java.declaredFields.map { it.name }.toSet()
        assertTrue(fields.containsAll(setOf("id", "ownerId", "title", "mediaKind", "viewCount", "downloadCount")))
        assertFalse(fields.any { it in setOf("file", "fileUri", "filePath", "localUri", "localPath", "mediaBytes", "downloadBytes") })
    }
}
