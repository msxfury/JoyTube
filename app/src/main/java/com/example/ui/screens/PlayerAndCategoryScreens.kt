package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.icons.JoyTubeIconType
import com.example.ui.icons.JoyTubeSvgIcon
import com.example.ui.theme.JoyTubeColors
import com.example.ui.theme.JoyTubeRadius
import com.example.ui.theme.JoyTubeTypeScale
import com.example.ui.theme.joyTubeOutlinedSurface
import com.example.viewmodel.JoyTubeUiState
import com.example.viewmodel.JoyTubeViewModel

/**
 * 1. VIDEO PLAYER PAGE (PART V — SECTION 17 & SCREENSHOTS 7, 11)
 *
 * Opens as a dedicated screen. The bottom navigation dock disappears to keep
 * the viewing area clean.
 */
@Composable
fun VideoPlayerScreen(
    mediaId: String,
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    val currentMedia = remember(uiState.mediaList, uiState.unifiedSearchResponse, uiState.globalPlayback, mediaId) {
        uiState.mediaList.find { it.id == mediaId }
            ?: uiState.unifiedSearchResponse?.results?.find { it.id == mediaId }?.let { res ->
                MediaItem(
                    id = res.id,
                    title = res.title,
                    importerUsername = res.creatorOrChannelTitle.removePrefix("@").ifEmpty { uiState.username },
                    isOwnedByCurrentUser = false,
                    type = if (res.resultMode == com.example.backend.contracts.UnifiedSearchMode.Shorts) MediaType.Short else MediaType.Video,
                    visibility = ContentVisibility.Public,
                    durationLabel = res.durationLabel,
                    durationSeconds = res.durationSeconds.coerceAtLeast(60),
                    categoryTag = res.resultMode.label,
                    artwork = res.artworkPreset,
                    sourceDomain = res.providerKind.displayLabel
                )
            }
            ?: uiState.mediaList.firstOrNull()
            ?: MediaItem(
                id = mediaId.ifEmpty { "active_media" },
                title = uiState.globalPlayback.activeTitle.ifEmpty { "No media selected" },
                importerUsername = uiState.globalPlayback.activeCreatorOrSource.removePrefix("@").ifEmpty { uiState.username },
                isOwnedByCurrentUser = false,
                type = MediaType.Video,
                visibility = ContentVisibility.Public,
                durationLabel = "03:00",
                durationSeconds = uiState.globalPlayback.totalDurationSec.coerceAtLeast(180),
                categoryTag = "All",
                artwork = uiState.globalPlayback.artworkPreset,
                sourceDomain = "joytube.app"
            )
    }
    val upNextMedia = remember(uiState.mediaList, currentMedia.id) {
        uiState.mediaList.firstOrNull { it.id != currentMedia.id }
    }
    val selectedYouTubeResult = uiState.unifiedSearchResponse?.results?.firstOrNull { it.id == mediaId }
    val embeddedYouTubeVideoId = selectedYouTubeResult
        ?.takeIf { it.providerKind != com.example.backend.contracts.ProviderKind.JOYTUBE_OWNED }
        ?.providerId
        ?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
    var isFullscreenMode by remember { mutableStateOf(false) }
    val totalSec = currentMedia.durationSeconds.coerceAtLeast(1)
    val elapsedSec = uiState.currentPositionSec.coerceIn(0, totalSec)
    val progress = (elapsedSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)

    if (isFullscreenMode) {
        // Immersive Fullscreen Player State
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(JoyTubeColors.Background)
        ) {
            if (embeddedYouTubeVideoId != null) {
                YouTubeEmbedPlayer(
                    videoId = embeddedYouTubeVideoId,
                    captionsEnabled = uiState.subtitlesSetting != "Off",
                    modifier = Modifier.fillMaxSize()
                )
            } else JoyTubeMediaThumbnail(
                preset = currentMedia.artwork,
                customUri = currentMedia.customThumbnailUri,
                sourceBadge = currentMedia.sourceDomain ?: "joytube.app",
                showBookmark = true,
                isBookmarked = currentMedia.isBookmarked,
                onBookmarkClick = { viewModel.toggleMediaBookmark(currentMedia.id) },
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                JoyTubeSecondaryTopBar(
                    title = currentMedia.title,
                    onBackClick = { isFullscreenMode = false },
                    centeredTitle = true,
                    rightActionIcon1 = JoyTubeIconType.MoreHorizontal,
                    onRightAction1Click = { viewModel.showOverlay(ActiveOverlay.VideoOptions(currentMedia)) }
                )

                if (embeddedYouTubeVideoId == null) PlayerTransportAndTimeline(
                    isPlaying = uiState.isPlaying,
                    isMuted = uiState.isMuted,
                    elapsedSec = elapsedSec,
                    totalSec = totalSec,
                    progress = progress,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onSeekFraction = { frac -> viewModel.seekTo((frac * totalSec).toInt()) },
                    onPrevious = { viewModel.seekTo(0) },
                    onNext = {
                        if (upNextMedia != null) viewModel.navigateTo(JoyTubeRoute.VideoPlayer(upNextMedia.id))
                        else viewModel.skipToNextPlaybackItem()
                    },
                    onToggleMute = { viewModel.toggleMute() },
                    onOpenSettings = { viewModel.showOverlay(ActiveOverlay.PlaybackSettings) },
                    onToggleFullscreen = { isFullscreenMode = false }
                )
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
    ) {
        JoyTubeSecondaryTopBar(
            title = "Now playing",
            onBackClick = { viewModel.navigateBack() },
            centeredTitle = true,
            rightActionIcon1 = JoyTubeIconType.MoreHorizontal,
            onRightAction1Click = { viewModel.showOverlay(ActiveOverlay.VideoOptions(currentMedia)) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("video_player_scroll"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 16:9 Video Display Viewport
            item {
                if (embeddedYouTubeVideoId != null) {
                    YouTubeEmbedPlayer(
                        videoId = embeddedYouTubeVideoId,
                        captionsEnabled = uiState.subtitlesSetting != "Off",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 10f)
                            .clip(JoyTubeRadius.ContentCard)
                            .border(1.dp, JoyTubeColors.BorderStandard, JoyTubeRadius.ContentCard)
                    )
                } else JoyTubeMediaThumbnail(
                    preset = currentMedia.artwork,
                    customUri = currentMedia.customThumbnailUri,
                    sourceBadge = currentMedia.sourceDomain ?: "joytube.app",
                    showBookmark = true,
                    isBookmarked = currentMedia.isBookmarked,
                    onBookmarkClick = { viewModel.toggleMediaBookmark(currentMedia.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .clip(JoyTubeRadius.ContentCard)
                        .border(1.dp, JoyTubeColors.BorderStandard, JoyTubeRadius.ContentCard)
                        .clickable { viewModel.togglePlayPause() }
                )
            }

            // Transport Controls + Timeline + Utility Row (Screenshot 11)
            if (embeddedYouTubeVideoId == null) item {
                PlayerTransportAndTimeline(
                    isPlaying = uiState.isPlaying,
                    isMuted = uiState.isMuted,
                    elapsedSec = elapsedSec,
                    totalSec = totalSec,
                    progress = progress,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onSeekFraction = { frac -> viewModel.seekTo((frac * totalSec).toInt()) },
                    onPrevious = { viewModel.seekTo(0) },
                    onNext = {
                        if (upNextMedia != null) viewModel.navigateTo(JoyTubeRoute.VideoPlayer(upNextMedia.id))
                        else viewModel.skipToNextPlaybackItem()
                    },
                    onToggleMute = { viewModel.toggleMute() },
                    onOpenSettings = { viewModel.showOverlay(ActiveOverlay.PlaybackSettings) },
                    onToggleFullscreen = { isFullscreenMode = true }
                )
            }

            // Video Title & Importer Attribution
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = currentMedia.title,
                        style = JoyTubeTypeScale.SectionHeading
                    )
                    Text(
                        text = "Imported by @${currentMedia.importerUsername}",
                        style = JoyTubeTypeScale.Metadata
                    )
                }
            }

            // 4-Column Outlined Action Buttons (Favourite | Download | Add to | Settings)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlayerActionCard(
                        icon = if (currentMedia.isFavourite) JoyTubeIconType.HeartFilled else JoyTubeIconType.HeartOutline,
                        label = "Favourite",
                        onClick = { viewModel.toggleMediaFavourite(currentMedia.id) },
                        modifier = Modifier.weight(1f)
                    )
                    PlayerActionCard(
                        icon = JoyTubeIconType.Download,
                        label = "Download",
                        onClick = { viewModel.showOverlay(ActiveOverlay.DownloadQuality(currentMedia)) },
                        modifier = Modifier.weight(1f)
                    )
                    PlayerActionCard(
                        icon = JoyTubeIconType.FolderPlus,
                        label = "Add to",
                        onClick = { viewModel.showOverlay(ActiveOverlay.AddToCategory(listOf(currentMedia.id))) },
                        modifier = Modifier.weight(1f)
                    )
                    PlayerActionCard(
                        icon = JoyTubeIconType.Sliders,
                        label = "Settings",
                        onClick = { viewModel.showOverlay(ActiveOverlay.PlaybackSettings) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Up Next Outlined Card (only rendered when another imported video exists)
            if (upNextMedia != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .joyTubeOutlinedSurface(
                                shape = JoyTubeRadius.ContentCard,
                                backgroundColor = JoyTubeColors.Surface,
                                borderColor = JoyTubeColors.BorderStandard
                            )
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Up next",
                            style = JoyTubeTypeScale.CardTitle
                        )
                        CompactMediaRowItem(
                            media = upNextMedia,
                            onClick = { viewModel.navigateTo(JoyTubeRoute.VideoPlayer(upNextMedia.id)) },
                            onTrailingClick = { viewModel.showOverlay(ActiveOverlay.VideoOptions(upNextMedia)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerTransportAndTimeline(
    isPlaying: Boolean,
    isMuted: Boolean,
    elapsedSec: Int,
    totalSec: Int,
    progress: Float,
    onPlayPause: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFullscreen: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Centered SkipPrev / PlayPause / SkipNext row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onPrevious() },
                contentAlignment = Alignment.Center
            ) {
                JoyTubeSvgIcon(type = JoyTubeIconType.SkipPrevious, size = 18.dp)
            }
            Spacer(modifier = Modifier.width(22.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(JoyTubeColors.Selected, CircleShape)
                    .border(1.dp, Color(0x38FFFFFF), CircleShape)
                    .clickable { onPlayPause() }
                    .testTag("player_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                JoyTubeSvgIcon(
                    type = if (isPlaying) JoyTubeIconType.Pause else JoyTubeIconType.Play,
                    size = 20.dp
                )
            }
            Spacer(modifier = Modifier.width(22.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onNext() },
                contentAlignment = Alignment.Center
            ) {
                JoyTubeSvgIcon(type = JoyTubeIconType.SkipNext, size = 18.dp)
            }
        }

        // Elapsed & Total timestamps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatSeconds(elapsedSec), style = JoyTubeTypeScale.Microcopy.copy(color = JoyTubeColors.TextPrimary))
            Text(text = formatSeconds(totalSec), style = JoyTubeTypeScale.Microcopy.copy(color = JoyTubeColors.TextPrimary))
        }

        // Interactive Timeline Scrubber Track
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val frac = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeekFraction(frac)
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeekFraction(frac)
                    }
                }
        ) {
            val cy = size.height / 2f
            val trackH = 3.dp.toPx()
            drawRoundRect(
                color = Color(0xFF242428),
                topLeft = Offset(0f, cy - trackH / 2f),
                size = Size(size.width, trackH)
            )
            val activeW = size.width * progress
            drawRoundRect(
                color = Color(0xFF9A9AA2),
                topLeft = Offset(0f, cy - trackH / 2f),
                size = Size(activeW, trackH)
            )
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = Offset(activeW.coerceIn(5.dp.toPx(), size.width - 5.dp.toPx()), cy)
            )
        }

        // Volume / Playback Sliders / Fullscreen row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable { onToggleMute() },
                contentAlignment = Alignment.Center
            ) {
                JoyTubeSvgIcon(
                    type = if (isMuted) JoyTubeIconType.VolumeOff else JoyTubeIconType.VolumeUp,
                    size = 17.dp,
                    tint = JoyTubeColors.TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center
            ) {
                JoyTubeSvgIcon(
                    type = JoyTubeIconType.Sliders,
                    size = 17.dp,
                    tint = JoyTubeColors.TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable { onToggleFullscreen() },
                contentAlignment = Alignment.Center
            ) {
                JoyTubeSvgIcon(
                    type = JoyTubeIconType.Fullscreen,
                    size = 17.dp,
                    tint = JoyTubeColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun PlayerActionCard(
    icon: JoyTubeIconType,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(60.dp)
            .joyTubeOutlinedSurface(
                shape = JoyTubeRadius.InputField,
                backgroundColor = JoyTubeColors.Surface,
                borderColor = JoyTubeColors.BorderStandard
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        JoyTubeSvgIcon(type = icon, size = 17.dp, tint = JoyTubeColors.TextPrimary)
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label,
            style = JoyTubeTypeScale.Microcopy.copy(color = JoyTubeColors.TextPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatSeconds(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}

/**
 * 2. MY CATEGORIES SCREEN (PART VI — SECTION 20 & SCREENSHOTS 15, 18)
 */
@Composable
fun MyCategoriesScreen(
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
    ) {
        if (uiState.isCategorySelectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${uiState.selectedCategoryIds.size} categories selected",
                    style = JoyTubeTypeScale.SectionHeading
                )
                Text(
                    text = "Cancel",
                    style = JoyTubeTypeScale.Label.copy(color = JoyTubeColors.TextSecondary),
                    modifier = Modifier
                        .clip(JoyTubeRadius.SmallControl)
                        .clickable { viewModel.clearSelectionModes() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        } else {
            JoyTubeSecondaryTopBar(
                title = "My Categories",
                onBackClick = { viewModel.navigateBack() },
                rightActionIcon1 = JoyTubeIconType.Plus,
                onRightAction1Click = { viewModel.showOverlay(ActiveOverlay.CreateCategoryModal) }
            )
        }

        if (uiState.categories.isEmpty() || uiState.simulationMode == UiSimulationMode.Empty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                JoyTubeEmptyStateCard(
                    icon = JoyTubeIconType.Folder,
                    title = "No categories yet",
                    subtitle = "Create your first category.",
                    actionLabel = "Create category",
                    onActionClick = {
                        viewModel.setSimulationMode(UiSimulationMode.Populated)
                        viewModel.showOverlay(ActiveOverlay.CreateCategoryModal)
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("my_categories_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.isCategorySelectionMode) {
                    // Compact multi-category selection rows matching Screenshot 18
                    items(uiState.categories, key = { it.id }) { cat ->
                        val isSel = cat.id in uiState.selectedCategoryIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(JoyTubeRadius.InputField)
                                .clickable { viewModel.toggleCategorySelection(cat.id) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                JoyTubeSvgIcon(type = JoyTubeIconType.Folder, size = 18.dp)
                                Text(text = cat.name, style = JoyTubeTypeScale.BodyText)
                            }
                            SelectionCheckIndicator(isSelected = isSel)
                        }
                    }
                } else {
                    items(uiState.categories, key = { it.id }) { cat ->
                        OutlinedCategoryCard(
                            category = cat,
                            onClick = { viewModel.navigateTo(JoyTubeRoute.CategoryDetail(cat.id)) },
                            onLongClick = { viewModel.enterCategorySelectionMode(cat.id) },
                            onMoreClick = { viewModel.showOverlay(ActiveOverlay.CategoryOptions(cat)) },
                            onBookmarkClick = { viewModel.toggleCategoryFavourite(cat.id) }
                        )
                    }
                }
            }

            if (uiState.isCategorySelectionMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    JoyTubePrimaryButton(
                        text = "Delete selected categories",
                        onClick = {
                            viewModel.showOverlay(
                                ActiveOverlay.DeleteSelectedCategoriesConfirm(uiState.selectedCategoryIds)
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * 3. CATEGORY DETAIL SCREEN (PART VI — SECTION 21 & SCREENSHOT 9)
 */
@Composable
fun CategoryDetailScreen(
    categoryId: String,
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    val category = remember(uiState.categories, categoryId) {
        uiState.categories.find { it.id == categoryId } ?: uiState.categories.firstOrNull()
    }
    if (category == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            JoyTubeEmptyStateCard(
                icon = JoyTubeIconType.Folder,
                title = "Category unavailable",
                subtitle = "This category was removed.",
                actionLabel = "Back to categories",
                onActionClick = { viewModel.navigateBack() },
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    var sortAscending by remember { mutableStateOf(true) }
    val categoryMedia = remember(uiState.mediaList, category.mediaIds, sortAscending) {
        val raw = category.mediaIds.mapNotNull { id -> uiState.mediaList.find { it.id == id } }
        if (sortAscending) raw else raw.reversed()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
    ) {
        JoyTubeSecondaryTopBar(
            title = "",
            onBackClick = { viewModel.navigateBack() },
            rightActionIcon1 = JoyTubeIconType.Plus,
            onRightAction1Click = { viewModel.navigateTo(JoyTubeRoute.ImportMedia) },
            rightActionIcon2 = JoyTubeIconType.MoreHorizontal,
            onRightAction2Click = { viewModel.showOverlay(ActiveOverlay.CategoryOptions(category)) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("category_detail_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Category Banner & Metadata Header (Screenshot 9)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    JoyTubeMediaThumbnail(
                        preset = category.artwork,
                        sourceBadge = category.sourceDomain ?: "digitaliaworks.com",
                        showBookmark = true,
                        isBookmarked = category.isBookmarked,
                        onBookmarkClick = { viewModel.toggleCategoryFavourite(category.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 10f)
                            .clip(JoyTubeRadius.ContentCard)
                            .border(1.dp, JoyTubeColors.BorderStandard, JoyTubeRadius.ContentCard)
                    )

                    Text(
                        text = category.name,
                        style = JoyTubeTypeScale.PageTitle
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Created by @${category.creatorUsername}",
                            style = JoyTubeTypeScale.Metadata
                        )
                        Text(
                            text = category.visibility.label,
                            style = JoyTubeTypeScale.Label
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${categoryMedia.size} videos and Shorts",
                            style = JoyTubeTypeScale.CardTitle
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable { viewModel.toggleCategoryFavourite(category.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                JoyTubeSvgIcon(
                                    type = if (category.isFavourite) JoyTubeIconType.HeartFilled else JoyTubeIconType.HeartOutline,
                                    size = 18.dp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable { sortAscending = !sortAscending },
                                contentAlignment = Alignment.Center
                            ) {
                                JoyTubeSvgIcon(
                                    type = JoyTubeIconType.SortArrows,
                                    size = 18.dp
                                )
                            }
                        }
                    }
                }
            }

            if (categoryMedia.isEmpty()) {
                item {
                    JoyTubeEmptyStateCard(
                        icon = JoyTubeIconType.VideoCamera,
                        title = "No media in this category",
                        subtitle = "Import or add videos and Shorts to this collection.",
                        actionLabel = "Import media",
                        onActionClick = { viewModel.navigateTo(JoyTubeRoute.ImportMedia) }
                    )
                }
            } else {
                items(categoryMedia, key = { it.id }) { item ->
                    val secondaryText = if (item.type == MediaType.Short) "Short" else item.durationLabel
                    CompactMediaRowItem(
                        media = item,
                        onClick = {
                            if (item.type == MediaType.Short) {
                                viewModel.navigateTo(JoyTubeRoute.FullscreenShorts(item.id))
                            } else {
                                viewModel.navigateTo(JoyTubeRoute.VideoPlayer(item.id))
                            }
                        },
                        onLongClick = { viewModel.removeMediaFromCategory(category.id, item.id) },
                        subtitleOverride = "Imported by @${item.importerUsername}",
                        secondaryMeta = secondaryText,
                        onTrailingClick = { viewModel.showOverlay(ActiveOverlay.VideoOptions(item)) }
                    )
                }
            }
        }
    }
}
