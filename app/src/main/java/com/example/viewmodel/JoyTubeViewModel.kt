package com.example.viewmodel

import android.app.Activity
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import android.provider.OpenableColumns
import android.media.MediaMetadataRetriever
import com.example.backend.auth.SupabaseAuthSession
import com.example.backend.auth.SupabaseAuthRuntime
import com.example.backend.contracts.*
import com.example.backend.data.LocalDeviceDownloadCoordinator
import com.example.backend.data.MediaFileSignatureValidator
import com.example.backend.data.LocalMediaFileOperations
import com.example.backend.data.SupabaseJoyTubeRepository
import com.example.backend.playback.GlobalPlaybackLifecycleState
import com.example.backend.playback.GlobalPlaybackSnapshot
import com.example.backend.playback.PlaybackCoordinator
import com.example.backend.providers.*
import com.example.backend.resilience.LatencyPercentileSummary
import com.example.backend.resilience.ObservabilityEngine
import com.example.backend.resilience.QuotaBudgetManager
import com.example.backend.resilience.QuotaBudgetSnapshot
import com.example.backend.search.QueryNormalizationPipeline
import com.example.backend.search.UnifiedSearchCoordinator
import com.example.backend.update.GitHubReleaseUpdateChecker
import com.example.backend.update.JoyTubeReleaseUpdate
import com.example.model.*
import com.example.ui.components.ArtworkPreset
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class JoyTubeUiState(
    val currentRoute: JoyTubeRoute = JoyTubeRoute.Main(PrimaryTab.Home),
    val backStack: List<JoyTubeRoute> = emptyList(),
    val simulationMode: UiSimulationMode = UiSimulationMode.Populated,
    val selectedHomeFilter: String = "All",
    val username: String = "",
    val displayName: String = "Your Account",
    val email: String = "",
    val isSignedIn: Boolean = false,

    // Media & Categories (Strictly user-imported or user-created — no fake templates)
    val mediaList: List<MediaItem> = emptyList(),
    val categories: List<CategoryItem> = emptyList(),
    val importedItemsCount: Int = 0,

    // Search & Unified Federated Backend State
    val searchQuery: String = "",
    val recentSearches: List<String> = emptyList(),
    val searchFilter: String = "All", // "All", "Shorts", "Videos", "Live", "Music", "Categories", "Web"
    val isUnifiedSearchLoading: Boolean = false,
    val unifiedSearchResponse: UnifiedSearchResponse? = null,
    val quotaSnapshot: QuotaBudgetSnapshot = QuotaBudgetManager().snapshot(),
    val latencySummary: LatencyPercentileSummary = LatencyPercentileSummary(0, 0L, 0L, 0L),
    val authSession: SupabaseAuthSession? = null,
    val globalPlayback: GlobalPlaybackSnapshot = GlobalPlaybackSnapshot(),
    val updateAvailable: JoyTubeReleaseUpdate? = null,

    // Player state
    val isPlaying: Boolean = false,
    val currentPositionSec: Int = 0,
    val isMuted: Boolean = false,
    val playbackSpeed: String = "1.0×",
    val videoQuality: String = "Auto",
    val screenOrientation: String = "Auto",
    val subtitlesSetting: String = "Off",

    // Favourites & Downloads & History
    val favouritesTab: String = "Videos", // "Videos", "Shorts", "Categories"
    val downloadsFilter: String = "All",  // "All", "Downloading", "Completed"
    val downloads: List<DownloadEntry> = emptyList(),
    val historyTab: String = "Watch history", // "Watch history", "Browser history"
    val watchHistory: List<WatchHistoryEntry> = emptyList(),
    val browserHistory: List<BrowserHistoryEntry> = emptyList(),

    // Import workflow
    val importUrlInput: String = "",
    val importUrlError: String? = null,
    val pendingImport: PendingImportDraft = PendingImportDraft(),

    // Bulk Selection Mode (Screenshot 18)
    val isMediaSelectionMode: Boolean = false,
    val selectedMediaIds: Set<String> = emptySet(),
    val isCategorySelectionMode: Boolean = false,
    val selectedCategoryIds: Set<String> = emptySet(),

    // Built-in Browser & Ad-Blocker Settings (Screenshots 1, 3, 18)
    val browserAddressInput: String = "",
    val activeBrowserUrl: String? = null,
    val browserTabs: List<BrowserTabItem> = defaultBrowserTabs(),
    val activeBrowserTabId: String = "tab_1",
    val adBlockEnabled: Boolean = true,
    val blockAdRequests: Boolean = true,
    val blockPopups: Boolean = true,
    val hideCookieBanners: Boolean = true,

    // Active Pop-up / Bottom Sheet / Dialog Overlay
    val activeOverlay: ActiveOverlay? = null,

    // Translucent Liquid Glass Design Mode (User-toggleable in Settings anytime)
    val glassDesignEnabled: Boolean = true,

    // Floating Contextual Feedback Banner (Screenshot 17)
    val feedbackMessage: String? = null
)

class JoyTubeViewModel : ViewModel() {
    val quotaManager = QuotaBudgetManager(dailyBudgetUnits = 10_000)
    val observability = ObservabilityEngine()
    val authManager = SupabaseAuthRuntime.manager
    private val cloudRepository = SupabaseJoyTubeRepository()
    val localDownloadCoordinator = LocalDeviceDownloadCoordinator()
    val playbackCoordinator = PlaybackCoordinator()
    private val releaseUpdateChecker = GitHubReleaseUpdateChecker()
    private var updateCheckInProgress = false

    val videoProvider = VideoProvider(quotaManager)
    val shortsProvider = ShortsProvider(quotaManager) { _uiState.value.mediaList }
    val liveProvider = LiveProvider(quotaManager)
    val musicProvider = MusicProvider(quotaManager)

    val unifiedSearchCoordinator = UnifiedSearchCoordinator(
        videoAdapter = videoProvider,
        shortsAdapter = shortsProvider,
        liveAdapter = liveProvider,
        musicAdapter = musicProvider,
        observability = observability
    )

    private var searchDebounceJob: Job? = null

    private val restoredAuthSession = authManager.currentSession()
    private val _uiState = MutableStateFlow(
        JoyTubeUiState(
            username = restoredAuthSession?.username.orEmpty(),
            displayName = restoredAuthSession?.displayName ?: "Your Account",
            email = restoredAuthSession?.email.orEmpty(),
            isSignedIn = restoredAuthSession != null,
            authSession = restoredAuthSession,
            quotaSnapshot = quotaManager.snapshot(),
            globalPlayback = playbackCoordinator.snapshot()
        )
    )
    val uiState: StateFlow<JoyTubeUiState> = _uiState.asStateFlow()

    init {
        if (restoredAuthSession != null) {
            viewModelScope.launch {
                val refreshed = authManager.refreshIfNeeded()
                if (refreshed == null) {
                    _uiState.update {
                        it.copy(
                            isSignedIn = false,
                            authSession = null,
                            username = "",
                            displayName = "Your Account",
                            email = "",
                            currentRoute = JoyTubeRoute.AuthFlow(AuthStep.SignIn),
                            feedbackMessage = "Your JoyTube session expired. Sign in again."
                        )
                    }
                } else {
                    val cloudLibrary = runCatching { cloudRepository.loadLibrary(refreshed) }
                    _uiState.update {
                        it.copy(
                            isSignedIn = true,
                            authSession = refreshed,
                            username = refreshed.username,
                            displayName = refreshed.displayName,
                            email = refreshed.email,
                            mediaList = cloudLibrary.getOrNull()?.let { library ->
                                authManager.applicationContextOrNull()?.let { context -> cloudRepository.restoreLocalMediaPointers(context, refreshed, library.media) } ?: library.media
                            } ?: it.mediaList,
                            categories = cloudLibrary.getOrNull()?.collections ?: it.categories,
                            importedItemsCount = cloudLibrary.getOrNull()?.media?.size ?: it.importedItemsCount,
                            glassDesignEnabled = cloudLibrary.getOrNull()?.theme != "normal",
                            blockAdRequests = cloudLibrary.getOrNull()?.settings?.optBoolean("blockAdRequests", it.blockAdRequests) ?: it.blockAdRequests,
                            blockPopups = cloudLibrary.getOrNull()?.settings?.optBoolean("blockPopups", it.blockPopups) ?: it.blockPopups,
                            hideCookieBanners = cloudLibrary.getOrNull()?.settings?.optBoolean("hideCookieBanners", it.hideCookieBanners) ?: it.hideCookieBanners,
                            feedbackMessage = cloudLibrary.exceptionOrNull()?.let { error -> "Signed in, but cloud library failed to load: ${error.message}" }
                        )
                    }
                }
            }
        }
    }

    fun selectPrimaryTab(tab: PrimaryTab) {
        _uiState.update { state ->
            state.copy(
                currentRoute = JoyTubeRoute.Main(tab),
                isMediaSelectionMode = false,
                selectedMediaIds = emptySet(),
                isCategorySelectionMode = false,
                selectedCategoryIds = emptySet()
            )
        }
    }

    fun navigateTo(route: JoyTubeRoute) {
        if (route is JoyTubeRoute.VideoPlayer) {
            val item = _uiState.value.mediaList.firstOrNull { it.id == route.mediaId }
            val session = _uiState.value.authSession
            if (item?.isOwnedByCurrentUser == true && session != null) viewModelScope.launch {
                runCatching { cloudRepository.recordView(session, item.id) }
                    .onFailure { error -> _uiState.update { it.copy(feedbackMessage = "View count sync failed: ${error.message}") } }
            }
        }
        _uiState.update { state ->
            val updatedPlayback = if (route is JoyTubeRoute.VideoPlayer) {
                val media = state.mediaList.find { it.id == route.mediaId }
                val searchMatch = state.unifiedSearchResponse?.results?.find { it.id == route.mediaId }
                when {
                    media != null -> {
                        val snap = playbackCoordinator.selectAndPlay(
                            PlaybackItem(
                                id = media.id,
                                providerId = media.id,
                                providerKind = ProviderKind.JOYTUBE_OWNED,
                                contentMode = if (media.type == MediaType.Short) UnifiedSearchMode.Shorts else UnifiedSearchMode.Videos,
                                title = media.title,
                                subtitleOrArtist = "@${media.importerUsername}",
                                artworkPreset = media.artwork,
                                durationSeconds = media.durationSeconds
                            )
                        )
                        snap
                    }
                    searchMatch != null -> playbackCoordinator.selectAndPlay(searchMatch.toPlaybackItem())
                    else -> state.globalPlayback
                }
            } else state.globalPlayback

            if (state.currentRoute == route) state.copy(globalPlayback = updatedPlayback, isPlaying = updatedPlayback.lifecycleState == GlobalPlaybackLifecycleState.PLAYING)
            else state.copy(
                backStack = state.backStack + state.currentRoute,
                currentRoute = route,
                activeOverlay = null,
                globalPlayback = updatedPlayback,
                isPlaying = updatedPlayback.lifecycleState == GlobalPlaybackLifecycleState.PLAYING
            )
        }
    }

    fun navigateBack(): Boolean {
        val current = _uiState.value
        if (current.activeOverlay != null) {
            _uiState.update { it.copy(activeOverlay = null) }
            return true
        }
        // When signed out, keep the user on the JoyTube sign-in screen
        if (!current.isSignedIn) {
            return false
        }
        if (current.isMediaSelectionMode || current.isCategorySelectionMode) {
            clearSelectionModes()
            return true
        }
        if (current.currentRoute is JoyTubeRoute.BrowserHome && current.activeBrowserUrl != null) {
            _uiState.update { it.copy(activeBrowserUrl = null, browserAddressInput = "") }
            return true
        }
        if (current.backStack.isNotEmpty()) {
            val previous = current.backStack.last()
            _uiState.update {
                it.copy(
                    currentRoute = previous,
                    backStack = it.backStack.dropLast(1)
                )
            }
            return true
        }
        if (current.currentRoute !is JoyTubeRoute.Main || current.currentRoute.tab != PrimaryTab.Home) {
            _uiState.update { it.copy(currentRoute = JoyTubeRoute.Main(PrimaryTab.Home)) }
            return true
        }
        return false
    }

    fun setSimulationMode(mode: UiSimulationMode) {
        val msg = when (mode) {
            UiSimulationMode.Populated -> null
            UiSimulationMode.Loading -> "Showing skeleton loading layout"
            UiSimulationMode.Empty -> "Showing empty collection states"
            UiSimulationMode.Offline -> "You're offline. Check your connection."
            UiSimulationMode.Error -> "We couldn't load this section. Try again."
        }
        _uiState.update { it.copy(simulationMode = mode, feedbackMessage = msg) }
    }

    fun setHomeFilter(filter: String) {
        _uiState.update { it.copy(selectedHomeFilter = filter) }
    }

    fun showOverlay(overlay: ActiveOverlay?) {
        _uiState.update { it.copy(activeOverlay = overlay) }
    }

    fun showFeedback(message: String) {
        _uiState.update { it.copy(feedbackMessage = message) }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun checkForAppUpdate() {
        if (!_uiState.value.isSignedIn || updateCheckInProgress) return
        updateCheckInProgress = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { releaseUpdateChecker.findNewerApk() }.getOrNull()
            updateCheckInProgress = false
            if (_uiState.value.isSignedIn) _uiState.update { it.copy(updateAvailable = result) }
        }
    }

    fun clearAvailableAppUpdate() {
        updateCheckInProgress = false
        _uiState.update { it.copy(updateAvailable = null) }
    }

    fun toggleGlassDesign() {
        _uiState.update { state ->
            val next = !state.glassDesignEnabled
            state.copy(
                glassDesignEnabled = next,
                feedbackMessage = if (next) {
                    "Glass Design enabled — Translucent surfaces & scroll motion active"
                } else {
                    "Glass Design disabled — Classic matte outlined surfaces active"
                }
            )
        }
        _uiState.value.authSession?.let { session ->
            viewModelScope.launch {
                runCatching { cloudRepository.saveTheme(session, if (_uiState.value.glassDesignEnabled) "glass" else "normal") }
                    .onFailure { error -> _uiState.update { it.copy(feedbackMessage = "Theme sync failed: ${error.message}") } }
            }
        }
    }

    // Favourites & Bookmarks
    fun toggleMediaFavourite(mediaId: String) {
        _uiState.update { state ->
            var nowFav = false
            val updated = state.mediaList.map { item ->
                if (item.id == mediaId) {
                    nowFav = !item.isFavourite
                    item.copy(isFavourite = nowFav)
                } else item
            }
            state.copy(
                mediaList = updated,
                feedbackMessage = if (nowFav) "Added to favourites" else "Removed from favourites"
            )
        }
        val session = _uiState.value.authSession
        val media = _uiState.value.mediaList.firstOrNull { it.id == mediaId }
        if (session != null && media?.isOwnedByCurrentUser == true) viewModelScope.launch {
            runCatching { cloudRepository.setFavourite(session, mediaId, media.isFavourite) }
                .onFailure { error -> _uiState.update { it.copy(feedbackMessage = "Favourite sync failed: ${error.message}") } }
        }
    }

    fun toggleMediaBookmark(mediaId: String) {
        _uiState.update { state ->
            var nowBookmarked = false
            val updated = state.mediaList.map { item ->
                if (item.id == mediaId) {
                    nowBookmarked = !item.isBookmarked
                    item.copy(isBookmarked = nowBookmarked, isFavourite = nowBookmarked)
                } else item
            }
            state.copy(
                mediaList = updated,
                feedbackMessage = if (nowBookmarked) "Added to favourites" else "Removed from favourites"
            )
        }
    }

    fun toggleCategoryFavourite(categoryId: String) {
        _uiState.update { state ->
            var nowFav = false
            val updated = state.categories.map { cat ->
                if (cat.id == categoryId) {
                    nowFav = !cat.isFavourite
                    cat.copy(isFavourite = nowFav, isBookmarked = nowFav)
                } else cat
            }
            state.copy(
                categories = updated,
                feedbackMessage = if (nowFav) "Added to favourites" else "Removed from favourites"
            )
        }
        val session = _uiState.value.authSession
        val category = _uiState.value.categories.firstOrNull { it.id == categoryId }
        if (session != null && category != null) viewModelScope.launch {
            runCatching { cloudRepository.setCollectionFavourite(session, categoryId, category.isFavourite) }
                .onFailure { error -> _uiState.update { it.copy(feedbackMessage = "Collection favourite sync failed: ${error.message}") } }
        }
    }

    fun setFavouritesTab(tab: String) {
        _uiState.update { it.copy(favouritesTab = tab) }
    }

    // Bulk Selection Architecture (Part VII — Section 27 & Screenshot 18)
    fun enterMediaSelectionMode(initialMediaId: String) {
        _uiState.update {
            it.copy(
                isMediaSelectionMode = true,
                selectedMediaIds = setOf(initialMediaId)
            )
        }
    }

    fun toggleMediaSelection(mediaId: String) {
        _uiState.update { state ->
            val next = if (mediaId in state.selectedMediaIds) {
                state.selectedMediaIds - mediaId
            } else {
                state.selectedMediaIds + mediaId
            }
            state.copy(
                selectedMediaIds = next,
                isMediaSelectionMode = next.isNotEmpty()
            )
        }
    }

    fun selectAllMedia(ids: List<String>) {
        _uiState.update {
            it.copy(
                isMediaSelectionMode = ids.isNotEmpty(),
                selectedMediaIds = ids.toSet()
            )
        }
    }

    fun enterCategorySelectionMode(initialCategoryId: String) {
        _uiState.update {
            it.copy(
                isCategorySelectionMode = true,
                selectedCategoryIds = setOf(initialCategoryId)
            )
        }
    }

    fun toggleCategorySelection(categoryId: String) {
        _uiState.update { state ->
            val next = if (categoryId in state.selectedCategoryIds) {
                state.selectedCategoryIds - categoryId
            } else {
                state.selectedCategoryIds + categoryId
            }
            state.copy(
                selectedCategoryIds = next,
                isCategorySelectionMode = next.isNotEmpty()
            )
        }
    }

    fun clearSelectionModes() {
        _uiState.update {
            it.copy(
                isMediaSelectionMode = false,
                selectedMediaIds = emptySet(),
                isCategorySelectionMode = false,
                selectedCategoryIds = emptySet()
            )
        }
    }

    fun bulkFavouriteSelectedMedia() {
        _uiState.update { state ->
            val updated = state.mediaList.map { item ->
                if (item.id in state.selectedMediaIds) item.copy(isFavourite = true, isBookmarked = true)
                else item
            }
            state.copy(
                mediaList = updated,
                isMediaSelectionMode = false,
                selectedMediaIds = emptySet(),
                feedbackMessage = "Added to favourites"
            )
        }
    }

    fun bulkDeleteOwnedMedia(mediaIds: Set<String>) {
        _uiState.update { state ->
            val ownedIds = state.mediaList
                .filter { it.id in mediaIds && it.isOwnedByCurrentUser }
                .map { it.id }
                .toSet()
            val updatedMedia = state.mediaList.filterNot { it.id in ownedIds }
            val updatedCategories = state.categories.map { cat ->
                cat.copy(mediaIds = cat.mediaIds.filterNot { it in ownedIds })
            }
            state.copy(
                mediaList = updatedMedia,
                categories = updatedCategories,
                importedItemsCount = updatedMedia.count { it.isOwnedByCurrentUser },
                isMediaSelectionMode = false,
                selectedMediaIds = emptySet(),
                activeOverlay = null,
                feedbackMessage = if (ownedIds.isNotEmpty()) {
                    "Deleted ${ownedIds.size} imported item(s) from this session"
                } else {
                    "Only content you imported can be deleted"
                }
            )
        }
    }

    // Categories Management (Part VI — Sections 19-23)
    fun createCategory(name: String, visibility: ContentVisibility, description: String = "") {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val newCat = CategoryItem(
            id = "cat_${System.currentTimeMillis()}",
            name = trimmed,
            creatorUsername = _uiState.value.username,
            isOwnedByCurrentUser = true,
            visibility = visibility,
            artwork = ArtworkPreset.SynthwaveStudio,
            sourceDomain = "joytube.app",
            mediaIds = emptyList(),
            description = description.trim().take(500)
        )
        val session = _uiState.value.authSession
        if (session == null) {
            _uiState.update { it.copy(feedbackMessage = "Sign in to save collections to JoyTube cloud.") }
            return
        }
        viewModelScope.launch {
            val savedCategory = runCatching { cloudRepository.createCollection(session, newCat) }
            if (savedCategory.isFailure) {
                _uiState.update { it.copy(activeOverlay = null, feedbackMessage = "Collection save failed: ${savedCategory.exceptionOrNull()?.message}") }
                return@launch
            }
            _uiState.update { state ->
            state.copy(
                categories = listOf(savedCategory.getOrThrow()) + state.categories,
                activeOverlay = null,
                feedbackMessage = "Created category \"$trimmed\" (${visibility.label})"
            )
            }
        }
    }

    fun renameCategory(categoryId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                categories = state.categories.map {
                    if (it.id == categoryId) it.copy(name = trimmed) else it
                },
                activeOverlay = null,
                feedbackMessage = "Category renamed to \"$trimmed\""
            )
        }
        val session = _uiState.value.authSession
        val category = _uiState.value.categories.firstOrNull { it.id == categoryId }
        if (session != null && category != null) viewModelScope.launch {
            runCatching { cloudRepository.updateCollection(session, category) }
                .onFailure { error -> _uiState.update { it.copy(feedbackMessage = "Collection sync failed: ${error.message}") } }
        }
    }

    fun toggleCategoryVisibility(categoryId: String) {
        _uiState.update { state ->
            var newVis = ContentVisibility.Public
            val updated = state.categories.map {
                if (it.id == categoryId) {
                    newVis = if (it.visibility == ContentVisibility.Public) ContentVisibility.Private else ContentVisibility.Public
                    it.copy(visibility = newVis)
                } else it
            }
            state.copy(
                categories = updated,
                activeOverlay = null,
                feedbackMessage = "Category visibility changed to ${newVis.label}"
            )
        }
        val session = _uiState.value.authSession
        val category = _uiState.value.categories.firstOrNull { it.id == categoryId }
        if (session != null && category != null) viewModelScope.launch {
            runCatching { cloudRepository.updateCollection(session, category) }
                .onFailure { error -> _uiState.update { it.copy(feedbackMessage = "Collection sync failed: ${error.message}") } }
        }
    }

    fun deleteCategoryAndContents(categoryId: String) {
        _uiState.update { state ->
            val target = state.categories.find { it.id == categoryId }
            val containedOwnedIds = target?.mediaIds?.toSet() ?: emptySet()
            val nextCategories = state.categories.filterNot { it.id == categoryId }
            val nextMedia = state.mediaList.filterNot { it.id in containedOwnedIds && it.isOwnedByCurrentUser }
            val nextRoute = if (state.currentRoute is JoyTubeRoute.CategoryDetail && state.currentRoute.categoryId == categoryId) {
                JoyTubeRoute.MyCategories
            } else state.currentRoute

            state.copy(
                categories = nextCategories,
                mediaList = nextMedia,
                importedItemsCount = nextMedia.count { it.isOwnedByCurrentUser },
                currentRoute = nextRoute,
                activeOverlay = null,
                feedbackMessage = "Deleted category and its contained media"
            )
        }
        _uiState.value.authSession?.let { session -> viewModelScope.launch {
            runCatching { cloudRepository.deleteCollection(session, categoryId) }
                .onFailure { error -> _uiState.update { it.copy(feedbackMessage = "Collection delete sync failed: ${error.message}") } }
        } }
    }

    fun deleteSelectedCategories(categoryIds: Set<String>) {
        val session = _uiState.value.authSession
        if (session != null) viewModelScope.launch {
            categoryIds.forEach { id -> runCatching { cloudRepository.deleteCollection(session, id) } }
        }
        _uiState.update { state ->
            val ownedCatIds = state.categories
                .filter { it.id in categoryIds && it.isOwnedByCurrentUser }
                .map { it.id }
                .toSet()
            state.copy(
                categories = state.categories.filterNot { it.id in ownedCatIds },
                isCategorySelectionMode = false,
                selectedCategoryIds = emptySet(),
                activeOverlay = null,
                feedbackMessage = "Deleted ${ownedCatIds.size} selected categories"
            )
        }
    }

    fun addMediaToCategories(mediaIds: List<String>, targetCategoryIds: Set<String>) {
        val session = _uiState.value.authSession
        if (session != null) viewModelScope.launch {
            targetCategoryIds.forEach { categoryId -> mediaIds.forEachIndexed { index, mediaId ->
                runCatching { cloudRepository.attachAsset(session, categoryId, mediaId, index) }
                    .onFailure { error -> _uiState.update { it.copy(feedbackMessage = "Collection item sync failed: ${error.message}") } }
            } }
        }
        _uiState.update { state ->
            val updated = state.categories.map { cat ->
                if (cat.id in targetCategoryIds) {
                    cat.copy(mediaIds = (cat.mediaIds + mediaIds).distinct())
                } else cat
            }
            state.copy(
                categories = updated,
                isMediaSelectionMode = false,
                selectedMediaIds = emptySet(),
                activeOverlay = null,
                feedbackMessage = "Added to ${targetCategoryIds.size} category(s)"
            )
        }
    }

    fun removeMediaFromCategory(categoryId: String, mediaId: String) {
        // Removing from a category does NOT delete the original imported media (Section 21)
        _uiState.update { state ->
            val updated = state.categories.map { cat ->
                if (cat.id == categoryId) cat.copy(mediaIds = cat.mediaIds - mediaId)
                else cat
            }
            state.copy(
                categories = updated,
                feedbackMessage = "Removed from category (original media preserved)"
            )
        }
        _uiState.value.authSession?.let { session -> viewModelScope.launch {
            runCatching { cloudRepository.removeAssetFromCollection(session, categoryId, mediaId) }
                .onFailure { error -> _uiState.update { it.copy(feedbackMessage = "Collection item sync failed: ${error.message}") } }
        } }
    }

    // Search Architecture (Sections 16–20: 300ms Debounce + Cancellation + Federated Providers)
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchDebounceJob?.cancel()
        val normalized = QueryNormalizationPipeline.normalize(query)
        if (normalized.isEmpty()) {
            _uiState.update {
                it.copy(
                    isUnifiedSearchLoading = false,
                    unifiedSearchResponse = null
                )
            }
            return
        }
        searchDebounceJob = viewModelScope.launch {
            delay(300L) // 300ms debounce before invoking federated backend providers
            executeFederatedSearchInternal(normalized, _uiState.value.searchFilter)
        }
    }

    fun submitSearch(query: String) {
        val normalized = QueryNormalizationPipeline.normalize(query)
        if (normalized.isEmpty()) return
        searchDebounceJob?.cancel()
        _uiState.update { state ->
            val nextHistory = listOf(normalized) + state.recentSearches.filterNot { it.equals(normalized, ignoreCase = true) }
            state.copy(
                searchQuery = normalized,
                recentSearches = nextHistory.take(8)
            )
        }
        viewModelScope.launch {
            executeFederatedSearchInternal(normalized, _uiState.value.searchFilter)
        }
    }

    fun removeRecentSearch(item: String) {
        _uiState.update { state ->
            state.copy(recentSearches = state.recentSearches - item)
        }
    }

    fun setSearchFilter(filter: String) {
        _uiState.update { it.copy(searchFilter = filter) }
        val normalized = QueryNormalizationPipeline.normalize(_uiState.value.searchQuery)
        if (normalized.isNotEmpty() && filter != "Categories" && filter != "Web") {
            searchDebounceJob?.cancel()
            viewModelScope.launch {
                executeFederatedSearchInternal(normalized, filter)
            }
        }
    }

    private suspend fun executeFederatedSearchInternal(normalizedQuery: String, filterLabel: String) {
        val genId = unifiedSearchCoordinator.nextGenerationId()
        _uiState.update { it.copy(isUnifiedSearchLoading = true) }
        val mode = UnifiedSearchMode.fromLabel(filterLabel)
        val response = unifiedSearchCoordinator.executeUnifiedSearch(
            rawQuery = normalizedQuery,
            mode = mode,
            callerGenerationId = genId
        )
        // Section 12: Discard stale search responses if a newer query generation has started
        if (!unifiedSearchCoordinator.isGenerationCurrent(genId)) return

        _uiState.update { state ->
            state.copy(
                isUnifiedSearchLoading = false,
                unifiedSearchResponse = response,
                quotaSnapshot = quotaManager.snapshot(),
                latencySummary = observability.computeLatencyPercentiles()
            )
        }
    }

    // Player Controls (Section 16 & 17 Global Playback Coordination)
    fun togglePlayPause() {
        val snap = playbackCoordinator.togglePlayPause()
        _uiState.update { state ->
            state.copy(
                isPlaying = snap.lifecycleState == GlobalPlaybackLifecycleState.PLAYING,
                globalPlayback = snap
            )
        }
    }

    fun skipToNextPlaybackItem() {
        val snap = playbackCoordinator.skipToNext()
        _uiState.update { state ->
            state.copy(
                isPlaying = snap.lifecycleState == GlobalPlaybackLifecycleState.PLAYING,
                currentPositionSec = snap.currentPositionSec,
                globalPlayback = snap,
                feedbackMessage = "Playing \"${snap.activeTitle}\""
            )
        }
    }

    fun openActiveMiniPlayer() {
        val activeId = _uiState.value.globalPlayback.activeMediaId
        navigateTo(JoyTubeRoute.VideoPlayer(activeId))
    }

    fun seekTo(seconds: Int) {
        val snap = playbackCoordinator.seekTo(seconds)
        _uiState.update { state ->
            state.copy(
                currentPositionSec = snap.currentPositionSec,
                globalPlayback = snap
            )
        }
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun updatePlaybackSpeed(speed: String) {
        _uiState.update { it.copy(playbackSpeed = speed, feedbackMessage = "Playback speed: $speed") }
    }

    fun updateVideoQuality(quality: String) {
        _uiState.update { it.copy(videoQuality = quality, feedbackMessage = "Quality set to $quality") }
    }

    fun updateScreenOrientation(orientation: String) {
        _uiState.update { it.copy(screenOrientation = orientation) }
    }

    fun updateSubtitles(subtitles: String) {
        _uiState.update { it.copy(subtitlesSetting = subtitles) }
    }

    // Import Workflow (Part VII — Sections 24-26 & Section 11 Magic-Byte + YouTube Stream Guard + Supabase Persistence)
    fun updateImportUrl(url: String) {
        _uiState.update { it.copy(importUrlInput = url, importUrlError = null) }
    }

    fun startImportFromUrl() {
        val raw = _uiState.value.importUrlInput.trim()
        if (raw.isEmpty() || (!raw.startsWith("http://") && !raw.startsWith("https://"))) {
            _uiState.update {
                it.copy(
                    importUrlError = "Enter a valid https:// media link to import.",
                    feedbackMessage = "Enter a valid https:// media link."
                )
            }
            return
        }
        // Section 10 & 11 Compliance Guard: Never import or download YouTube audiovisual streams
        if (MediaFileSignatureValidator.isProhibitedYouTubeSource(raw)) {
            _uiState.update {
                it.copy(
                    importUrlError = "Policy restriction: YouTube streams cannot be imported or stored in JoyTube.",
                    feedbackMessage = "YouTube streams are playback-only via the official player."
                )
            }
            return
        }
        if (raw.endsWith(".exe", ignoreCase = true) || raw.endsWith(".zip", ignoreCase = true)) {
            _uiState.update {
                it.copy(
                    importUrlError = "This video format isn't supported (MIME & magic-byte check failed).",
                    feedbackMessage = "This video format isn't supported."
                )
            }
            return
        }
        val host = raw.removePrefix("https://").removePrefix("http://").substringBefore("/").ifEmpty { "joytube.app" }
        val fileSegment = raw.substringBefore("?").substringAfterLast("/").substringBeforeLast(".")
            .replace(Regex("[-_]+"), " ")
            .trim()
        val inferredTitle = if (fileSegment.isNotBlank() && !fileSegment.equals(host, ignoreCase = true)) {
            fileSegment.replaceFirstChar { it.uppercase() }
        } else {
            "Imported Media (${host})"
        }
        _uiState.update { state ->
            state.copy(
                pendingImport = PendingImportDraft(
                    sourceUrlOrFile = raw,
                    title = inferredTitle,
                    sourceDomain = host,
                    artwork = ArtworkPreset.CyberpunkCityscape,
                    customUri = null,
                    type = MediaType.Video,
                    durationSeconds = 180,
                    visibility = ContentVisibility.Public,
                    targetCategoryId = state.categories.firstOrNull()?.id ?: ""
                )
            )
        }
        navigateTo(JoyTubeRoute.ImportPreview)
    }

    fun startImportFromDeviceUri(uriString: String?, labelName: String = "") {
        if (uriString.isNullOrBlank()) {
            _uiState.update { it.copy(feedbackMessage = "Choose a media file from your device first.") }
            return
        }
        val uri = Uri.parse(uriString)
        val appContext = authManager.applicationContextOrNull()
        val metadata = appContext?.contentResolver?.let { resolver ->
            var displayName = labelName
            var size: Long? = null
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) displayName = cursor.getString(nameIndex).orEmpty().ifBlank { displayName }
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }
            Triple(displayName, size, resolver.getType(uri))
        }
        val durableName = metadata?.first?.takeIf { it.isNotBlank() } ?: labelName
        val resolvedUri = uriString
        val inferredTitle = durableName.trim().ifEmpty {
            durableName.substringBeforeLast(".")
                .replace(Regex("[-_]+"), " ")
                .trim()
                .replaceFirstChar { it.uppercase() }
                .ifEmpty { "Device Media" }
        }
        val durationSeconds = appContext?.let { context ->
            runCatching {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(context, uri)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.div(1000L)?.toInt()
                }
            }.getOrNull()
        } ?: 0
        _uiState.update { state ->
            state.copy(
                pendingImport = PendingImportDraft(
                    sourceUrlOrFile = resolvedUri,
                    title = inferredTitle,
                    sourceDomain = "local.device",
                    artwork = ArtworkPreset.CyberpunkCityscape,
                    customUri = uriString,
                    type = MediaType.Video,
                    durationSeconds = durationSeconds,
                    visibility = ContentVisibility.Public,
                    targetCategoryId = state.categories.firstOrNull()?.id ?: ""
                )
            )
        }
        navigateTo(JoyTubeRoute.ImportPreview)
    }

    fun updatePendingImportTitle(title: String) {
        _uiState.update { state ->
            state.copy(pendingImport = state.pendingImport.copy(title = title))
        }
    }

    fun updatePendingImportType(type: MediaType) {
        _uiState.update { state ->
            state.copy(pendingImport = state.pendingImport.copy(type = type))
        }
    }

    fun updatePendingImportDuration(durationSec: Int) {
        _uiState.update { state ->
            state.copy(pendingImport = state.pendingImport.copy(durationSeconds = durationSec))
        }
    }

    fun updatePendingImportVisibility(visibility: ContentVisibility) {
        _uiState.update { state ->
            state.copy(pendingImport = state.pendingImport.copy(visibility = visibility))
        }
    }

    fun updatePendingImportCategory(categoryId: String) {
        _uiState.update { state ->
            state.copy(pendingImport = state.pendingImport.copy(targetCategoryId = categoryId))
        }
    }

    fun togglePendingThumbnailFailure() {
        _uiState.update { state ->
            val nextFail = !state.pendingImport.thumbnailFailed
            state.copy(
                pendingImport = state.pendingImport.copy(thumbnailFailed = nextFail),
                feedbackMessage = if (nextFail) "Thumbnail unavailable with retry option" else "Thumbnail automatically generated from the media"
            )
        }
    }

    fun confirmImportNow() {
        val draft = _uiState.value.pendingImport
        // Enforce 10-minute maximum duration for Shorts (Section 15 & 24)
        if (draft.type == MediaType.Short && draft.durationSeconds > 600) {
            _uiState.update {
                it.copy(
                    feedbackMessage = "Shorts cannot exceed 10 minutes (current: ${draft.durationSeconds / 60}m). Switch to Video or trim."
                )
            }
            return
        }
        val cleanTitle = draft.title.trim().ifEmpty { "Imported ${draft.type.label}" }
        val newId = "vid_import_${System.currentTimeMillis()}"
        val session = _uiState.value.authSession
        if (session == null) {
            _uiState.update { it.copy(feedbackMessage = "Sign in to save imported media metadata to JoyTube cloud.") }
            return
        }

        val mins = draft.durationSeconds / 60
        val secs = draft.durationSeconds % 60
        val durStr = "%02d:%02d".format(mins, secs)

        if (draft.customUri.isNullOrBlank()) {
            _uiState.update { it.copy(feedbackMessage = "URL metadata import is not connected to a playable local file yet. Choose a device file to add playable media.") }
            return
        }

        val localUri = draft.customUri
        val localMetadata = localUri?.let { rawUri ->
            val context = authManager.applicationContextOrNull()
            val uri = Uri.parse(rawUri)
            val resolver = context?.contentResolver
            if (resolver == null) null else {
                var size: Long? = null
                var fileName = ""
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex >= 0) fileName = cursor.getString(nameIndex).orEmpty()
                        if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                    }
                }
                Triple(fileName, size, resolver.getType(uri))
            }
        }
        val newItem = MediaItem(
            id = newId,
            title = cleanTitle,
            importerUsername = _uiState.value.username,
            isOwnedByCurrentUser = true,
            type = draft.type,
            visibility = draft.visibility,
            durationLabel = durStr,
            durationSeconds = draft.durationSeconds,
            categoryTag = "All",
            artwork = draft.artwork,
            sourceDomain = draft.sourceDomain.ifEmpty { "joytube.app" },
            customThumbnailUri = null,
            localContentUri = localUri,
            localMimeType = localMetadata?.third,
            localSizeBytes = localMetadata?.second
        )

        viewModelScope.launch {
        val savedId = runCatching { cloudRepository.createImportedAsset(session, newItem) }
        if (savedId.isFailure) {
            _uiState.update { it.copy(feedbackMessage = "Import metadata save failed: ${savedId.exceptionOrNull()?.message}") }
            return@launch
        }
        val persistedItem = newItem.copy(id = savedId.getOrThrow())
        localUri?.let { uri -> authManager.applicationContextOrNull()?.let { context ->
            cloudRepository.saveLocalMediaPointer(context, session.userIdUuid, persistedItem.id, uri, persistedItem.localMimeType, persistedItem.localSizeBytes)
        } }
        runCatching {
            cloudRepository.recordRecentImport(session, persistedItem.id)
            _uiState.value.categories.firstOrNull { it.id == draft.targetCategoryId }?.let { cat ->
                cloudRepository.attachAsset(session, cat.id, persistedItem.id, cat.mediaIds.size)
            }
        }.onFailure { error -> _uiState.update { it.copy(feedbackMessage = "Imported, but collection/recent sync failed: ${error.message}") } }

        _uiState.update { state ->
            val updatedCats = state.categories.map { cat ->
                if (cat.id == draft.targetCategoryId) {
                    cat.copy(mediaIds = listOf(persistedItem.id) + cat.mediaIds)
                } else cat
            }
            state.copy(
                mediaList = listOf(persistedItem) + state.mediaList,
                categories = updatedCats,
                importedItemsCount = state.importedItemsCount + 1,
                importUrlInput = "",
                importUrlError = null,
                feedbackMessage = "Added \"$cleanTitle\" to your JoyTube library. Media stays on this device."
            )
        }
        navigateTo(JoyTubeRoute.ImportCompletion)
        }
    }

    // Downloads Management (Section 29 & Screenshot 5)
    fun setDownloadsFilter(filter: String) {
        _uiState.update { it.copy(downloadsFilter = filter) }
    }

    fun startDownloadWithQuality(media: MediaItem, quality: String) {
        val uriString = media.localContentUri
        val parsed = uriString?.let(Uri::parse)
        if (parsed == null || parsed.scheme != "content") {
            _uiState.update { it.copy(activeOverlay = null, feedbackMessage = "Only media files selected from your device can be downloaded here. YouTube downloads are not supported.") }
            return
        }
        val appContext = authManager.applicationContextOrNull()
        if (appContext == null) {
            _uiState.update { it.copy(activeOverlay = null, feedbackMessage = "Device media access is unavailable. Reopen JoyTube and retry.") }
            return
        }
        viewModelScope.launch {
            val result = runCatching {
                val destination = appContext.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES) ?: appContext.filesDir
                LocalMediaFileOperations.copySelectedMedia(appContext, parsed, destination, media.title).file
            }
            val file = result.getOrNull()
            val entry = DownloadEntry(
                id = "dl_${System.currentTimeMillis()}", mediaId = media.id, title = media.title,
                quality = "Original", sizeLabel = file?.let { android.text.format.Formatter.formatShortFileSize(appContext, it.length()) } ?: "",
                status = if (file != null) DownloadStatus.Completed else DownloadStatus.Failed,
                progressPercent = if (file != null) 100 else 0, artwork = media.artwork,
                localFilePath = file?.absolutePath, sizeBytes = file?.length() ?: 0L
            )
            _uiState.update { state -> state.copy(
                downloads = listOf(entry) + state.downloads,
                activeOverlay = null,
                feedbackMessage = file?.let { "Saved local copy (${entry.sizeLabel})" } ?: "Download failed: ${result.exceptionOrNull()?.message}"
            ) }
        }
    }

    fun togglePauseResumeDownload(downloadId: String) {
        _uiState.update { it.copy(feedbackMessage = "Downloads are copied in one operation; pause and resume are not available for local file copies.") }
    }

    fun completeDownloadNow(downloadId: String) = Unit

    fun removeDownloadEntry(downloadId: String, isCancel: Boolean) {
        // Differentiates deleting a local downloaded file from deleting original imported media (Section 29)
        val existing = _uiState.value.downloads.firstOrNull { it.id == downloadId }
        existing?.localFilePath?.let { path -> runCatching { java.io.File(path).delete() } }
        _uiState.update { state ->
            state.copy(
                downloads = state.downloads.filterNot { it.id == downloadId },
                feedbackMessage = if (isCancel) "Download cancelled" else "Deleted local downloaded file (original media kept)"
            )
        }
    }

    fun playDownloadedFile(downloadId: String) {
        val entry = _uiState.value.downloads.firstOrNull { it.id == downloadId }
        val path = entry?.localFilePath?.let { java.io.File(it) }
        if (entry == null || path == null || !path.isFile || path.length() == 0L) {
            _uiState.update { it.copy(feedbackMessage = "The downloaded file is unavailable on this device.") }
            return
        }
        val original = _uiState.value.mediaList.firstOrNull { it.id == entry.mediaId }
        if (original == null) {
            _uiState.update { it.copy(feedbackMessage = "The JoyTube library item for this local file is unavailable.") }
            return
        }
        val localCopy = original.copy(localContentUri = Uri.fromFile(path).toString())
        _uiState.update { state -> state.copy(mediaList = state.mediaList.map { if (it.id == original.id) localCopy else it }) }
        navigateTo(JoyTubeRoute.VideoPlayer(entry.mediaId))
    }

    // Watch & Browser History (Section 30 & Screenshot 3)
    fun setHistoryTab(tab: String) {
        _uiState.update { it.copy(historyTab = tab) }
    }

    fun removeWatchHistoryItem(id: String) {
        _uiState.update { state ->
            state.copy(
                watchHistory = state.watchHistory.filterNot { it.id == id },
                feedbackMessage = "Removed from watch history"
            )
        }
    }

    fun removeBrowserHistoryItem(id: String) {
        _uiState.update { state ->
            state.copy(
                browserHistory = state.browserHistory.filterNot { it.id == id },
                feedbackMessage = "Removed from browser history"
            )
        }
    }

    fun executeConfirmAction(action: ConfirmActionType) {
        _uiState.update { state ->
            when (action) {
                ConfirmActionType.ClearWatchHistory -> state.copy(
                    watchHistory = emptyList(),
                    activeOverlay = null,
                    feedbackMessage = "Watch history cleared"
                )
                ConfirmActionType.ClearBrowserHistory, ConfirmActionType.ClearBrowsingData -> state.copy(
                    browserHistory = emptyList(),
                    activeOverlay = null,
                    feedbackMessage = "Browser history cleared"
                )
                ConfirmActionType.SignOut -> {
                    viewModelScope.launch { authManager.signOut() }
                    val clearedPlayback = playbackCoordinator.clearOnSignOut()
                    state.copy(
                        isSignedIn = false,
                        authSession = null,
                        globalPlayback = clearedPlayback,
                        isPlaying = false,
                        backStack = emptyList(),
                        activeOverlay = null,
                        currentRoute = JoyTubeRoute.AuthFlow(AuthStep.SignIn),
                        feedbackMessage = "Signed out of JoyTube"
                    )
                }
                ConfirmActionType.DeleteAccount -> {
                    viewModelScope.launch { authManager.signOut() }
                    val clearedPlayback = playbackCoordinator.clearOnSignOut()
                    state.copy(
                        isSignedIn = false,
                        authSession = null,
                        globalPlayback = clearedPlayback,
                        isPlaying = false,
                        mediaList = emptyList(),
                        categories = emptyList(),
                        importedItemsCount = 0,
                        backStack = emptyList(),
                        activeOverlay = null,
                        currentRoute = JoyTubeRoute.AuthFlow(AuthStep.SignIn),
                        feedbackMessage = "Signed out. Account deletion is not available yet."
                    )
                }
            }
        }
    }

    // Built-in Browser & Ad-Blocker (Part IX — Sections 31-33 & Screenshots 1, 3, 18)
    fun updateBrowserAddressInput(input: String) {
        _uiState.update { it.copy(browserAddressInput = input) }
    }

    fun openBrowserUrlOrSearch(queryOrUrl: String) {
        val trimmed = queryOrUrl.trim()
        if (trimmed.isEmpty()) return
        val normalizedUrl = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> "https://duckduckgo.com/?q=${trimmed.replace(" ", "+")}"
        }
        val domain = normalizedUrl.removePrefix("https://").removePrefix("http://").substringBefore("/")
        val newHistoryEntry = BrowserHistoryEntry(
            id = "bh_${System.currentTimeMillis()}",
            pageTitle = trimmed,
            domain = domain,
            url = normalizedUrl,
            visitTimeLabel = "Visited just now",
            bucket = HistoryBucket.Today
        )
        _uiState.update { state ->
            val updatedTabs = state.browserTabs.map { tab ->
                if (tab.id == state.activeBrowserTabId) {
                    tab.copy(title = trimmed, url = normalizedUrl, domain = domain)
                } else tab
            }
            state.copy(
                activeBrowserUrl = normalizedUrl,
                browserAddressInput = normalizedUrl,
                browserTabs = updatedTabs,
                browserHistory = listOf(newHistoryEntry) + state.browserHistory,
                currentRoute = JoyTubeRoute.BrowserHome
            )
        }
    }

    fun createNewBrowserTab(isVideoTab: Boolean = false) {
        val newId = "tab_${System.currentTimeMillis()}"
        val newTab = BrowserTabItem(
            id = newId,
            title = if (isVideoTab) "Video page" else "Example webpage",
            url = if (isVideoTab) "https://joytube.app/watch/city" else "https://example.com",
            domain = if (isVideoTab) "joytube.app" else "example.com",
            isVideoPage = isVideoTab,
            artwork = if (isVideoTab) ArtworkPreset.CyberpunkCityscape else ArtworkPreset.NightMountainLake
        )
        _uiState.update { state ->
            state.copy(
                browserTabs = state.browserTabs + newTab,
                activeBrowserTabId = newId,
                activeBrowserUrl = null,
                browserAddressInput = "",
                activeOverlay = null
            )
        }
    }

    fun selectBrowserTab(tabId: String) {
        _uiState.update { state ->
            val tab = state.browserTabs.find { it.id == tabId }
            state.copy(
                activeBrowserTabId = tabId,
                browserAddressInput = tab?.url ?: "",
                currentRoute = JoyTubeRoute.BrowserHome
            )
        }
    }

    fun closeBrowserTab(tabId: String) {
        _uiState.update { state ->
            val remaining = state.browserTabs.filterNot { it.id == tabId }
            val fallback = remaining.ifEmpty {
                listOf(
                    BrowserTabItem(
                        id = "tab_default",
                        title = "Example webpage",
                        url = "https://example.com",
                        domain = "example.com",
                        isVideoPage = false
                    )
                )
            }
            state.copy(
                browserTabs = fallback,
                activeBrowserTabId = fallback.first().id
            )
        }
    }

    fun toggleAdBlockSetting(settingKey: String) {
        _uiState.update { state ->
            val next = when (settingKey) {
                "ads" -> state.copy(blockAdRequests = !state.blockAdRequests)
                "popups" -> state.copy(blockPopups = !state.blockPopups)
                "cookies" -> state.copy(hideCookieBanners = !state.hideCookieBanners)
                else -> state
            }
            val anyActive = next.blockAdRequests || next.blockPopups || next.hideCookieBanners
            next.copy(adBlockEnabled = anyActive)
        }
        saveCloudSettings()
    }

    // JoyTube profile controls
    fun updateProfile(newUsername: String, newDisplayName: String) {
        val cleanUser = newUsername.trim().removePrefix("@").ifEmpty { "username" }
        val cleanName = newDisplayName.trim().ifEmpty { "Your Account" }
        _uiState.update {
            it.copy(
                username = cleanUser,
                displayName = cleanName,
                feedbackMessage = "Profile updated (@$cleanUser)"
            )
        }
        val session = _uiState.value.authSession
        if (session != null) viewModelScope.launch {
            runCatching { authManager.updateProfile(session, cleanUser, cleanName) }
                .onSuccess { cloudRepository.saveProfile(session, cleanName) }
                .onFailure { error -> _uiState.update { it.copy(feedbackMessage = "Profile sync failed: ${error.message}") } }
        }
    }

    private fun saveCloudSettings() {
        val session = _uiState.value.authSession ?: return
        val state = _uiState.value
        val settings = org.json.JSONObject()
            .put("blockAdRequests", state.blockAdRequests)
            .put("blockPopups", state.blockPopups)
            .put("hideCookieBanners", state.hideCookieBanners)
        viewModelScope.launch {
            runCatching { cloudRepository.saveSettings(session, settings) }
                .onFailure { error -> _uiState.update { it.copy(feedbackMessage = "Settings sync failed: ${error.message}") } }
        }
    }

    fun toggleYouTubeReadonlyScopeConnection() {
        _uiState.update { it.copy(feedbackMessage = "JoyTube does not connect to YouTube accounts") }
    }

    fun continueWithGoogleSignIn(activity: Activity) {
        _uiState.update { it.copy(feedbackMessage = "Opening Google sign-in…") }
        viewModelScope.launch {
            runCatching { authManager.signInWithGoogle(activity) }
                .onSuccess { session ->
                    val cloudLibrary = runCatching { cloudRepository.loadLibrary(session) }
                    _uiState.update {
                        it.copy(
                            isSignedIn = true,
                            authSession = session,
                            username = session.username,
                            displayName = session.displayName,
                            email = session.email,
                            mediaList = cloudLibrary.getOrNull()?.let { library ->
                                authManager.applicationContextOrNull()?.let { context -> cloudRepository.restoreLocalMediaPointers(context, session, library.media) } ?: library.media
                            } ?: it.mediaList,
                            categories = cloudLibrary.getOrNull()?.collections ?: it.categories,
                            importedItemsCount = cloudLibrary.getOrNull()?.media?.size ?: 0,
                            glassDesignEnabled = cloudLibrary.getOrNull()?.theme != "normal",
                            blockAdRequests = cloudLibrary.getOrNull()?.settings?.optBoolean("blockAdRequests", it.blockAdRequests) ?: it.blockAdRequests,
                            blockPopups = cloudLibrary.getOrNull()?.settings?.optBoolean("blockPopups", it.blockPopups) ?: it.blockPopups,
                            hideCookieBanners = cloudLibrary.getOrNull()?.settings?.optBoolean("hideCookieBanners", it.hideCookieBanners) ?: it.hideCookieBanners,
                            currentRoute = JoyTubeRoute.Main(PrimaryTab.Home),
                            backStack = emptyList(),
                            activeOverlay = null,
                            feedbackMessage = cloudLibrary.exceptionOrNull()?.let { error -> "Signed in, but cloud library failed to load: ${error.message}" } ?: "Signed in as ${session.email}"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(feedbackMessage = error.message ?: "Google sign-in failed. Please try again.")
                    }
                }
        }
    }
}

private fun defaultBrowserTabs(): List<BrowserTabItem> = listOf(
    BrowserTabItem(
        id = "tab_1",
        title = "New Tab",
        url = "https://duckduckgo.com",
        domain = "duckduckgo.com",
        isVideoPage = false,
        artwork = ArtworkPreset.NightMountainLake
    )
)
