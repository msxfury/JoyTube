package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.LocalGlassDesignEnabled
import com.example.ui.theme.joyTubeOutlinedSurface
import com.example.viewmodel.JoyTubeUiState
import com.example.viewmodel.JoyTubeViewModel

/**
 * 1. HOME SCREEN (PART V — SECTION 14 & SCREENSHOT 12)
 */
@Composable
fun HomeScreen(
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    val categories = listOf("All", "Music", "Technology", "Nature")
    val publicVideos = remember(uiState.mediaList, uiState.selectedHomeFilter) {
        uiState.mediaList.filter { item ->
            item.type == MediaType.Video &&
                (uiState.selectedHomeFilter == "All" || item.categoryTag.equals(uiState.selectedHomeFilter, ignoreCase = true))
        }
    }
    val heroVideo = publicVideos.firstOrNull()
    val continueWatching = remember(uiState.mediaList) {
        uiState.mediaList.filter { it.watchProgress > 0f }
    }
    val feedVideos = remember(publicVideos, heroVideo) {
        if (heroVideo == null) emptyList()
        else publicVideos.filterNot { it.id == heroVideo.id }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
    ) {
        if (uiState.isMediaSelectionMode) {
            BulkMediaSelectionHeader(
                selectedCount = uiState.selectedMediaIds.size,
                onClear = { viewModel.clearSelectionModes() },
                onSelectAll = { viewModel.selectAllMedia(publicVideos.map { it.id }) }
            )
        } else {
            JoyTubeBrandHeader(
                onSearchClick = { viewModel.selectPrimaryTab(PrimaryTab.Search) },
                onAccountClick = { viewModel.showOverlay(ActiveOverlay.SwitchAccountModal) },
                accountInitial = uiState.displayName.take(1).ifEmpty { "D" }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_feed_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "For you",
                            style = JoyTubeTypeScale.PageTitle
                        )
                        // Interactive state switcher pill so user can inspect Loading/Empty/Offline states
                        StateSimulationSelectorPill(
                            currentMode = uiState.simulationMode,
                            onModeChange = { viewModel.setSimulationMode(it) }
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { filter ->
                            JoyTubeFilterPill(
                                label = filter,
                                isSelected = uiState.selectedHomeFilter == filter,
                                onClick = { viewModel.setHomeFilter(filter) }
                            )
                        }
                    }
                }
            }

            when (uiState.simulationMode) {
                UiSimulationMode.Loading -> {
                    items(3) {
                        JoyTubeSkeletonVideoCard()
                    }
                }

                UiSimulationMode.Empty -> {
                    item {
                        JoyTubeEmptyStateCard(
                            icon = JoyTubeIconType.VideoCamera,
                            title = "No videos in this view",
                            subtitle = "Import supported media or switch category filters.",
                            actionLabel = "Import media",
                            onActionClick = {
                                viewModel.setSimulationMode(UiSimulationMode.Populated)
                                viewModel.navigateTo(JoyTubeRoute.ImportMedia)
                            }
                        )
                    }
                }

                UiSimulationMode.Offline -> {
                    item {
                        JoyTubeEmptyStateCard(
                            icon = JoyTubeIconType.Globe,
                            title = "You're offline. Check your connection.",
                            subtitle = "Your downloaded videos remain available for offline playback.",
                            actionLabel = "Go to downloads",
                            onActionClick = {
                                viewModel.setSimulationMode(UiSimulationMode.Populated)
                                viewModel.navigateTo(JoyTubeRoute.Downloads)
                            }
                        )
                    }
                }

                UiSimulationMode.Error -> {
                    item {
                        JoyTubeEmptyStateCard(
                            icon = JoyTubeIconType.Info,
                            title = "Could not load video discovery",
                            subtitle = "A temporary rendering error occurred while refreshing the feed.",
                            actionLabel = "Retry now",
                            onActionClick = { viewModel.setSimulationMode(UiSimulationMode.Populated) }
                        )
                    }
                }

                UiSimulationMode.Populated -> {
                    if (heroVideo == null) {
                        item(key = "home_empty_no_templates") {
                            JoyTubeEmptyStateCard(
                                icon = JoyTubeIconType.VideoCamera,
                                title = "No videos yet",
                                subtitle = "Import your own media to this device or search Music, Videos, Shorts & Live.",
                                actionLabel = "Import media",
                                onActionClick = { viewModel.navigateTo(JoyTubeRoute.ImportMedia) }
                            )
                        }
                    } else {
                        item(key = "hero_${heroVideo.id}") {
                            OutlinedVideoCard(
                                media = heroVideo,
                                onClick = { viewModel.navigateTo(JoyTubeRoute.VideoPlayer(heroVideo.id)) },
                                onLongClick = {
                                    if (uiState.isMediaSelectionMode) viewModel.toggleMediaSelection(heroVideo.id)
                                    else viewModel.enterMediaSelectionMode(heroVideo.id)
                                },
                                onMoreClick = { viewModel.showOverlay(ActiveOverlay.VideoOptions(heroVideo)) },
                                onBookmarkClick = { viewModel.toggleMediaBookmark(heroVideo.id) },
                                isSelectionMode = uiState.isMediaSelectionMode,
                                isSelected = heroVideo.id in uiState.selectedMediaIds
                            )
                        }

                        if (continueWatching.isNotEmpty()) {
                            item(key = "continue_watching_section") {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Continue watching",
                                        style = JoyTubeTypeScale.SectionHeading
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        continueWatching.forEach { item ->
                                            CompactContinueWatchingCard(
                                                media = item,
                                                onClick = { viewModel.navigateTo(JoyTubeRoute.VideoPlayer(item.id)) },
                                                onBookmarkClick = { viewModel.toggleMediaBookmark(item.id) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        items(feedVideos, key = { it.id }) { video ->
                            OutlinedVideoCard(
                                media = video,
                                onClick = { viewModel.navigateTo(JoyTubeRoute.VideoPlayer(video.id)) },
                                onLongClick = {
                                    if (uiState.isMediaSelectionMode) viewModel.toggleMediaSelection(video.id)
                                    else viewModel.enterMediaSelectionMode(video.id)
                                },
                                onMoreClick = { viewModel.showOverlay(ActiveOverlay.VideoOptions(video)) },
                                onBookmarkClick = { viewModel.toggleMediaBookmark(video.id) },
                                isSelectionMode = uiState.isMediaSelectionMode,
                                isSelected = video.id in uiState.selectedMediaIds
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BulkMediaSelectionHeader(
    selectedCount: Int,
    onClear: () -> Unit,
    onSelectAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onClear() },
                contentAlignment = Alignment.Center
            ) {
                JoyTubeSvgIcon(type = JoyTubeIconType.Close, size = 18.dp)
            }
            Text(
                text = "$selectedCount selected",
                style = JoyTubeTypeScale.SectionHeading
            )
        }
        Text(
            text = "Select all",
            style = JoyTubeTypeScale.Label.copy(color = JoyTubeColors.TextSecondary),
            modifier = Modifier
                .clip(JoyTubeRadius.SmallControl)
                .clickable { onSelectAll() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun StateSimulationSelectorPill(
    currentMode: UiSimulationMode,
    onModeChange: (UiSimulationMode) -> Unit
) {
    val modes = UiSimulationMode.entries
    val nextMode = modes[(modes.indexOf(currentMode) + 1) % modes.size]
    Box(
        modifier = Modifier
            .clip(JoyTubeRadius.Capsule)
            .background(JoyTubeColors.Surface, JoyTubeRadius.Capsule)
            .border(1.dp, JoyTubeColors.BorderStandard, JoyTubeRadius.Capsule)
            .clickable { onModeChange(nextMode) }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = "State: ${currentMode.label}",
            style = JoyTubeTypeScale.Microcopy.copy(color = JoyTubeColors.TextSecondary)
        )
    }
}

/**
 * 2. SHORTS SCREEN (PART V — SECTION 15 & SCREENSHOT 6)
 *
 * Uses a vertical swipe feed. Controls contain ONLY JoyTube actions:
 * favourite, download, add to category, and more (...).
 * No social likes, dislikes, comments, or creator-follow controls.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortsScreen(
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    isFullscreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shortsList = remember(uiState.mediaList) {
        uiState.mediaList.filter { it.type == MediaType.Short }
    }
    val pagerState = rememberPagerState(pageCount = { shortsList.size })
    var isShortPlaying by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
    ) {
        if (isFullscreen) {
            JoyTubeSecondaryTopBar(
                title = "Shorts",
                onBackClick = { viewModel.navigateBack() },
                centeredTitle = true
            )
        } else {
            JoyTubeBrandHeader(
                onSearchClick = { viewModel.selectPrimaryTab(PrimaryTab.Search) },
                onAccountClick = { viewModel.showOverlay(ActiveOverlay.SwitchAccountModal) },
                accountInitial = uiState.displayName.take(1).ifEmpty { "D" }
            )
        }

        if (shortsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                JoyTubeEmptyStateCard(
                    icon = JoyTubeIconType.NavShorts,
                    title = "No Shorts yet",
                    subtitle = "Import a Short (up to 10m) or search Shorts in the Search tab.",
                    actionLabel = "Import Short",
                    onActionClick = { viewModel.navigateTo(JoyTubeRoute.ImportMedia) }
                )
            }
            return@Column
        }

        VerticalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = if (isFullscreen) 16.dp else 94.dp)
                .testTag("shorts_vertical_pager")
        ) { pageIndex ->
            val shortItem = shortsList[pageIndex]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isFullscreen) 12.dp else 44.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outlined Vertical 9:16 Short Card matching Screenshot 6
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                        .joyTubeOutlinedSurface(
                            shape = JoyTubeRadius.ModalDialog,
                            backgroundColor = JoyTubeColors.Surface,
                            borderColor = JoyTubeColors.BorderStandard
                        )
                        .clickable { isShortPlaying = !isShortPlaying }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        JoyTubeMediaThumbnail(
                            preset = shortItem.artwork,
                            customUri = shortItem.customThumbnailUri,
                            durationBadge = shortItem.durationLabel,
                            showBookmark = true,
                            isBookmarked = shortItem.isBookmarked,
                            onBookmarkClick = { viewModel.toggleMediaBookmark(shortItem.id) },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Fullscreen button at top-left inside Short
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(JoyTubeColors.GlassBadgeTint, CircleShape)
                                .border(1.dp, JoyTubeColors.BorderGlass, CircleShape)
                                .clickable {
                                    if (isFullscreen) viewModel.navigateBack()
                                    else viewModel.navigateTo(JoyTubeRoute.FullscreenShorts(shortItem.id))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            JoyTubeSvgIcon(
                                type = JoyTubeIconType.Fullscreen,
                                size = 14.dp,
                                tint = JoyTubeColors.TextPrimary
                            )
                        }

                        if (!isShortPlaying) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(JoyTubeColors.GlassBadgeTint, CircleShape)
                                    .border(1.dp, JoyTubeColors.BorderGlass, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                JoyTubeSvgIcon(
                                    type = JoyTubeIconType.Play,
                                    size = 22.dp,
                                    tint = JoyTubeColors.TextPrimary
                                )
                            }
                        }
                    }

                    // Bottom Metadata & JoyTube Action Bar (Screenshot 6)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(JoyTubeColors.Background)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = shortItem.title,
                                style = JoyTubeTypeScale.CardTitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable { viewModel.showOverlay(ActiveOverlay.VideoOptions(shortItem)) },
                                contentAlignment = Alignment.Center
                            ) {
                                JoyTubeSvgIcon(
                                    type = JoyTubeIconType.MoreHorizontal,
                                    size = 16.dp,
                                    tint = JoyTubeColors.TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Imported by @${shortItem.importerUsername}",
                            style = JoyTubeTypeScale.Metadata
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Strictly JoyTube actions: Favourite, Download, Add to Category
                        val isGlass = LocalGlassDesignEnabled.current
                        val shortActionGlassMod = if (isGlass) {
                            Modifier
                                .background(Color(0x24FFFFFF), CircleShape)
                                .border(1.dp, Color(0x40FFFFFF), CircleShape)
                        } else Modifier

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .then(shortActionGlassMod)
                                    .clip(CircleShape)
                                    .clickable { viewModel.toggleMediaFavourite(shortItem.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                JoyTubeSvgIcon(
                                    type = if (shortItem.isFavourite) JoyTubeIconType.HeartFilled else JoyTubeIconType.HeartOutline,
                                    size = 18.dp,
                                    tint = JoyTubeColors.TextPrimary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .then(shortActionGlassMod)
                                    .clip(CircleShape)
                                    .clickable { viewModel.showOverlay(ActiveOverlay.DownloadQuality(shortItem)) },
                                contentAlignment = Alignment.Center
                            ) {
                                JoyTubeSvgIcon(
                                    type = JoyTubeIconType.Download,
                                    size = 18.dp,
                                    tint = JoyTubeColors.TextPrimary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .then(shortActionGlassMod)
                                    .clip(CircleShape)
                                    .clickable { viewModel.showOverlay(ActiveOverlay.AddToCategory(listOf(shortItem.id))) },
                                contentAlignment = Alignment.Center
                            ) {
                                JoyTubeSvgIcon(
                                    type = JoyTubeIconType.FolderPlus,
                                    size = 18.dp,
                                    tint = JoyTubeColors.TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. SEARCH SCREEN (PART V — SECTION 16 & SCREENSHOT 6)
 *
 * - Capsule search input: "Search videos, Shorts, categories or web"
 * - Recent searches with clock icon and arrow/remove action
 * - Popular results & filtered public results (videos, Shorts, public categories, web)
 * - Strictly excludes Private categories and Private imported content from public discovery!
 */
@Composable
fun SearchScreen(
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    // Search belongs to YouTube here: use its own mobile search UI inside GeckoView.
    // JoyTube imported media and collections remain available from their own screens.
    val browserController = remember { GeckoBrowserController() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
    ) {
        JoyTubeBrandHeader(
            onSearchClick = { browserController.reload() },
            onAccountClick = { viewModel.showOverlay(ActiveOverlay.SwitchAccountModal) },
            accountInitial = uiState.displayName.take(1).ifEmpty { "D" }
        )
        GeckoBrowserView(
            url = "https://m.youtube.com",
            controller = browserController,
            onLocationChanged = viewModel::updateBrowserLocation,
            privacyFilteringEnabled = uiState.adBlockEnabled,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .clip(JoyTubeRadius.ThumbnailInner)
        )
    }
}

@Composable
fun PublicCategorySearchRow(
    category: CategoryItem,
    onClick: () -> Unit,
    onFavouriteToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .joyTubeOutlinedSurface(
                shape = JoyTubeRadius.ContentCard,
                backgroundColor = JoyTubeColors.Surface,
                borderColor = JoyTubeColors.BorderStandard
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        JoyTubeMediaThumbnail(
            preset = category.artwork,
            modifier = Modifier
                .width(88.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = category.name, style = JoyTubeTypeScale.CardTitle)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "Created by @${category.creatorUsername}", style = JoyTubeTypeScale.Metadata)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "Public category · ${category.mediaIds.size} items", style = JoyTubeTypeScale.Microcopy)
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { onFavouriteToggle() },
            contentAlignment = Alignment.Center
        ) {
            JoyTubeSvgIcon(
                type = if (category.isFavourite) JoyTubeIconType.HeartFilled else JoyTubeIconType.HeartOutline,
                size = 18.dp,
                tint = JoyTubeColors.TextPrimary
            )
        }
    }
}

/**
 * 4. ACCOUNT SCREEN (PART V — SECTION 18 & SCREENSHOT 7)
 *
 * Organised personal workspace containing:
 * - Profile row ("Your Account", "@username")
 * - My Categories
 * - Create Category
 * - Imported Videos
 * - Favourites
 * - Downloads
 * - History
 * - Browser
 * - Settings
 */
@Composable
fun AccountScreen(
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
    ) {
        JoyTubeBrandHeader(
            onSearchClick = { viewModel.selectPrimaryTab(PrimaryTab.Search) },
            onAccountClick = { viewModel.showOverlay(ActiveOverlay.SwitchAccountModal) },
            accountInitial = uiState.displayName.take(1).ifEmpty { "D" }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("account_destinations_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Profile Identity Header Row (Screenshot 7)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(JoyTubeRadius.ContentCard)
                        .clickable { viewModel.navigateTo(JoyTubeRoute.Profile) }
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                        .testTag("account_profile_entry"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(JoyTubeColors.Surface, CircleShape)
                            .border(1.dp, JoyTubeColors.BorderStandard, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        JoyTubeSvgIcon(
                            type = JoyTubeIconType.NavAccount,
                            size = 24.dp,
                            tint = JoyTubeColors.TextPrimary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.displayName,
                            style = JoyTubeTypeScale.SectionHeading
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "@${uiState.username}",
                            style = JoyTubeTypeScale.Metadata
                        )
                    }

                    JoyTubeSvgIcon(
                        type = JoyTubeIconType.ChevronRight,
                        size = 18.dp,
                        tint = JoyTubeColors.TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Primary Account Destinations matching Screenshot 7
            item {
                AccountDestinationRow(
                    icon = JoyTubeIconType.Folder,
                    title = "My Categories",
                    tag = "dest_my_categories",
                    onClick = { viewModel.navigateTo(JoyTubeRoute.MyCategories) }
                )
            }
            item {
                AccountDestinationRow(
                    icon = JoyTubeIconType.PlusSquare,
                    title = "Create Category",
                    tag = "dest_create_category",
                    onClick = { viewModel.showOverlay(ActiveOverlay.CreateCategoryModal) }
                )
            }
            item {
                AccountDestinationRow(
                    icon = JoyTubeIconType.VideoCamera,
                    title = "Imported Videos",
                    tag = "dest_imported_videos",
                    onClick = { viewModel.navigateTo(JoyTubeRoute.ImportedVideos) }
                )
            }
            item {
                AccountDestinationRow(
                    icon = JoyTubeIconType.HeartOutline,
                    title = "Favourites",
                    tag = "dest_favourites",
                    onClick = { viewModel.navigateTo(JoyTubeRoute.Favourites) }
                )
            }
            item {
                AccountDestinationRow(
                    icon = JoyTubeIconType.Download,
                    title = "Downloads",
                    tag = "dest_downloads",
                    onClick = { viewModel.navigateTo(JoyTubeRoute.Downloads) }
                )
            }
            item {
                AccountDestinationRow(
                    icon = JoyTubeIconType.History,
                    title = "History",
                    tag = "dest_history",
                    onClick = { viewModel.navigateTo(JoyTubeRoute.History) }
                )
            }
            item {
                AccountDestinationRow(
                    icon = JoyTubeIconType.Globe,
                    title = "Browser",
                    tag = "dest_browser",
                    onClick = { viewModel.navigateTo(JoyTubeRoute.BrowserHome) }
                )
            }
            item {
                AccountDestinationRow(
                    icon = JoyTubeIconType.Settings,
                    title = "Settings",
                    tag = "dest_settings",
                    onClick = { viewModel.navigateTo(JoyTubeRoute.Settings) }
                )
            }

            // Direct shortcuts to Import Media, Auth Screens & Design System Specs
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    JoyTubeSecondaryButton(
                        text = "Import Media",
                        leadingIcon = JoyTubeIconType.Link,
                        onClick = { viewModel.navigateTo(JoyTubeRoute.ImportMedia) },
                        modifier = Modifier.weight(1f)
                    )
                    JoyTubeSecondaryButton(
                        text = "Auth & Welcome",
                        leadingIcon = JoyTubeIconType.NavAccount,
                        onClick = { viewModel.navigateTo(JoyTubeRoute.AuthFlow(AuthStep.Splash)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountDestinationRow(
    icon: JoyTubeIconType,
    title: String,
    tag: String,
    onClick: () -> Unit
) {
    val isGlass = LocalGlassDesignEnabled.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .then(
                if (isGlass) {
                    Modifier
                        .joyTubeOutlinedSurface(
                            shape = JoyTubeRadius.InputField,
                            backgroundColor = JoyTubeColors.Surface,
                            borderColor = JoyTubeColors.BorderStandard,
                            topHighlightAlpha = 0.16f
                        )
                        .padding(horizontal = 12.dp)
                } else {
                    Modifier.padding(horizontal = 6.dp)
                }
            )
            .clip(JoyTubeRadius.InputField)
            .clickable { onClick() }
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .then(
                    if (isGlass) {
                        Modifier
                            .background(Color(0x1FFFFFFF), CircleShape)
                            .border(0.8.dp, Color(0x36FFFFFF), CircleShape)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            JoyTubeSvgIcon(
                type = icon,
                size = 18.dp,
                tint = JoyTubeColors.TextPrimary
            )
        }
        Text(
            text = title,
            style = JoyTubeTypeScale.BodyText.copy(fontSize = androidx.compose.ui.unit.TextUnit(15f, androidx.compose.ui.unit.TextUnitType.Sp)),
            modifier = Modifier.weight(1f)
        )
        JoyTubeSvgIcon(
            type = JoyTubeIconType.ChevronRight,
            size = 17.dp,
            tint = JoyTubeColors.TextSecondary
        )
    }
}
