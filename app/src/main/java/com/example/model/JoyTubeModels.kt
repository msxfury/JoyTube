package com.example.model

import androidx.compose.runtime.Immutable
import com.example.ui.components.ArtworkPreset

enum class PrimaryTab(val label: String) {
    Home("Home"),
    Shorts("Shorts"),
    Search("Search"),
    Account("Account")
}

@Immutable
sealed class JoyTubeRoute {
    @Immutable
    data class Main(val tab: PrimaryTab) : JoyTubeRoute()
    @Immutable
    data class VideoPlayer(val mediaId: String, val isFullscreen: Boolean = false) : JoyTubeRoute()
    @Immutable
    data class FullscreenShorts(val startMediaId: String) : JoyTubeRoute()
    @Immutable
    object MyCategories : JoyTubeRoute()
    @Immutable
    data class CategoryDetail(val categoryId: String) : JoyTubeRoute()
    @Immutable
    object CreateCategory : JoyTubeRoute()
    @Immutable
    object ImportMedia : JoyTubeRoute()
    @Immutable
    object ImportPreview : JoyTubeRoute()
    @Immutable
    object ImportCompletion : JoyTubeRoute()
    @Immutable
    object ImportedVideos : JoyTubeRoute()
    @Immutable
    object Favourites : JoyTubeRoute()
    @Immutable
    object Downloads : JoyTubeRoute()
    @Immutable
    object History : JoyTubeRoute()
    @Immutable
    object BrowserHome : JoyTubeRoute()
    @Immutable
    object BrowserTabs : JoyTubeRoute()
    @Immutable
    object AdBlockerSettings : JoyTubeRoute()
    @Immutable
    object Settings : JoyTubeRoute()
    @Immutable
    object Profile : JoyTubeRoute()
    @Immutable
    data class AuthFlow(val step: AuthStep = AuthStep.Splash) : JoyTubeRoute()
    @Immutable
    object DesignSystemSpecs : JoyTubeRoute()
}

enum class AuthStep {
    Splash,
    SignIn,
    CreateAccount,
    VerifyEmail,
    ForgotPassword,
    ResetPassword
}

enum class MediaType(val label: String) {
    Video("Video"),
    Short("Short")
}

enum class ContentVisibility(val label: String, val description: String) {
    Public("Public", "Discoverable by others"),
    Private("Private", "Only you can see it")
}

enum class UiSimulationMode(val label: String) {
    Populated("Populated"),
    Loading("Skeleton"),
    Empty("Empty"),
    Offline("Offline"),
    Error("Error")
}

@Immutable
data class MediaItem(
    val id: String,
    val title: String,
    val importerUsername: String,
    val isOwnedByCurrentUser: Boolean,
    val type: MediaType,
    val visibility: ContentVisibility,
    val durationLabel: String,
    val durationSeconds: Int,
    val categoryTag: String,
    val artwork: ArtworkPreset,
    val sourceDomain: String? = null,
    val customThumbnailUri: String? = null,
    val isFavourite: Boolean = false,
    val isBookmarked: Boolean = false,
    val watchProgress: Float = 0f,
    val availableQualities: List<String> = listOf("Original quality", "1080p", "720p", "480p")
)

@Immutable
data class CategoryItem(
    val id: String,
    val name: String,
    val creatorUsername: String,
    val isOwnedByCurrentUser: Boolean,
    val visibility: ContentVisibility,
    val artwork: ArtworkPreset,
    val sourceDomain: String? = null,
    val mediaIds: List<String>,
    val isFavourite: Boolean = false,
    val isBookmarked: Boolean = false
)

enum class DownloadStatus(val label: String) {
    Downloading("Downloading"),
    Paused("Paused"),
    Completed("Downloaded"),
    Queued("Queued"),
    Failed("Failed")
}

@Immutable
data class DownloadEntry(
    val id: String,
    val mediaId: String,
    val title: String,
    val quality: String,
    val sizeLabel: String,
    val status: DownloadStatus,
    val progressPercent: Int,
    val artwork: ArtworkPreset
)

enum class HistoryBucket(val label: String) {
    Today("Today"),
    Yesterday("Yesterday")
}

@Immutable
data class WatchHistoryEntry(
    val id: String,
    val mediaId: String,
    val title: String,
    val subtitle: String,
    val bucket: HistoryBucket,
    val artwork: ArtworkPreset
)

@Immutable
data class BrowserHistoryEntry(
    val id: String,
    val pageTitle: String,
    val domain: String,
    val url: String,
    val visitTimeLabel: String,
    val bucket: HistoryBucket
)

@Immutable
data class BrowserTabItem(
    val id: String,
    val title: String,
    val url: String,
    val domain: String,
    val isVideoPage: Boolean,
    val artwork: ArtworkPreset? = null
)

@Immutable
data class PendingImportDraft(
    val sourceUrlOrFile: String = "",
    val title: String = "",
    val sourceDomain: String = "",
    val artwork: ArtworkPreset = ArtworkPreset.CyberpunkCityscape,
    val customUri: String? = null,
    val type: MediaType = MediaType.Video,
    val durationSeconds: Int = 180,
    val visibility: ContentVisibility = ContentVisibility.Public,
    val targetCategoryId: String = "",
    val thumbnailFailed: Boolean = false
)

@Immutable
sealed class ActiveOverlay {
    @Immutable
    data class VideoOptions(val media: MediaItem) : ActiveOverlay()
    @Immutable
    data class CategoryOptions(val category: CategoryItem) : ActiveOverlay()
    @Immutable
    data class AddToCategory(val mediaIds: List<String>) : ActiveOverlay()
    @Immutable
    data class DownloadQuality(val media: MediaItem) : ActiveOverlay()
    @Immutable
    object PlaybackSettings : ActiveOverlay()
    @Immutable
    object CreateCategoryModal : ActiveOverlay()
    @Immutable
    data class EditCategoryModal(val category: CategoryItem) : ActiveOverlay()
    @Immutable
    data class RenameCategoryModal(val category: CategoryItem) : ActiveOverlay()
    @Immutable
    data class ChangeVisibilityModal(val category: CategoryItem) : ActiveOverlay()
    @Immutable
    data class DeleteCategoryConfirm(val category: CategoryItem) : ActiveOverlay()
    @Immutable
    data class DeleteSelectedCategoriesConfirm(val categoryIds: Set<String>) : ActiveOverlay()
    @Immutable
    data class DeleteSelectedMediaConfirm(val mediaIds: Set<String>) : ActiveOverlay()
    @Immutable
    object BrowserMenu : ActiveOverlay()
    @Immutable
    object SwitchAccountModal : ActiveOverlay()
    @Immutable
    data class GenericConfirm(
        val title: String,
        val message: String,
        val confirmLabel: String,
        val isDestructive: Boolean = true,
        val onConfirmAction: ConfirmActionType
    ) : ActiveOverlay()
}

enum class ConfirmActionType {
    ClearWatchHistory,
    ClearBrowserHistory,
    SignOut,
    DeleteAccount,
    ClearBrowsingData
}
