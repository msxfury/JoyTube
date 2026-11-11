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
    val controller = remember { GeckoBrowserController() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
    ) {
        JoyTubeBrandHeader(
            onSearchClick = { viewModel.selectPrimaryTab(PrimaryTab.Search) },
            onLogoClick = if (uiState.hideJoyTubeNav) ({ viewModel.selectPrimaryTab(PrimaryTab.Account) }) else null
        )
        GeckoBrowserView(
            url = "https://m.youtube.com",
            controller = controller,
            onLocationChanged = viewModel::updateBrowserLocation,
            privacyFilteringEnabled = uiState.adBlockEnabled,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (uiState.hideJoyTubeNav) 0.dp else 72.dp)
                .testTag("youtube_home_geckoview")
        )
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
    val controller = remember { GeckoBrowserController() }
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
                onLogoClick = if (uiState.hideJoyTubeNav) ({ viewModel.selectPrimaryTab(PrimaryTab.Account) }) else null
            )
        }
        GeckoBrowserView(
            url = "https://m.youtube.com/shorts",
            controller = controller,
            onLocationChanged = viewModel::updateBrowserLocation,
            privacyFilteringEnabled = uiState.adBlockEnabled,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (uiState.hideJoyTubeNav || isFullscreen) 0.dp else 72.dp)
                .testTag("youtube_shorts_geckoview")
        )
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
            onSearchClick = { browserController.loadUrl("https://m.youtube.com/results?search_query=") },
            onLogoClick = if (uiState.hideJoyTubeNav) ({ viewModel.selectPrimaryTab(PrimaryTab.Account) }) else null
        )
        GeckoBrowserView(
            url = "https://m.youtube.com/results?search_query=",
            controller = browserController,
            onLocationChanged = viewModel::updateBrowserLocation,
            privacyFilteringEnabled = uiState.adBlockEnabled,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = if (uiState.hideJoyTubeNav) 0.dp else 72.dp)
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
            onLogoClick = if (uiState.hideJoyTubeNav) ({ viewModel.selectPrimaryTab(PrimaryTab.Account) }) else null
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
                    icon = JoyTubeIconType.Globe,
                    title = "New Imported Joy's",
                    tag = "dest_new_imported_joys",
                    onClick = { viewModel.navigateTo(JoyTubeRoute.NewImportedJoys) }
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
                    icon = JoyTubeIconType.Settings,
                    title = "Settings",
                    tag = "dest_settings",
                    onClick = { viewModel.navigateTo(JoyTubeRoute.Settings) }
                )
            }

            // Keep the media import shortcut; authentication is handled by the account session flow.
            item {
                Spacer(modifier = Modifier.height(12.dp))
                JoyTubeSecondaryButton(
                    text = "Import Media",
                    leadingIcon = JoyTubeIconType.Link,
                    onClick = { viewModel.navigateTo(JoyTubeRoute.ImportMedia) },
                    modifier = Modifier.fillMaxWidth()
                )
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
