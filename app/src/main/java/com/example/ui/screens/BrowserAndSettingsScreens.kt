package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
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
 * 1. BUILT-IN BROWSER HOME & ACTIVE PAGE (PART IX — SECTION 31 & SCREENSHOTS 1, 3)
 */
@Composable
fun BrowserHomeScreen(
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    val browserController = remember { GeckoBrowserController() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
    ) {
        // Browser Top Header ("Browser" + "+" + Tabs Icon)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.navigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    JoyTubeSvgIcon(type = JoyTubeIconType.ArrowBack, size = 19.dp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    JoyTubeSvgIcon(type = JoyTubeIconType.BrandLogoLeftTriangle, size = 18.dp)
                    Text(text = "JoyTube", style = JoyTubeTypeScale.PageTitle)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.createNewBrowserTab(false) },
                    contentAlignment = Alignment.Center
                ) {
                    JoyTubeSvgIcon(type = JoyTubeIconType.Plus, size = 18.dp)
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.navigateTo(JoyTubeRoute.BrowserTabs) }
                        .testTag("browser_tabs_button"),
                    contentAlignment = Alignment.Center
                ) {
                    JoyTubeSvgIcon(type = JoyTubeIconType.TabsStack, size = 18.dp)
                }
            }
        }

        // Capsule Address / Search Bar (Screenshot 1)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            JoyTubeTextInput(
                value = uiState.browserAddressInput,
                onValueChange = { viewModel.updateBrowserAddressInput(it) },
                placeholder = "Search or enter address",
                leadingIcon = JoyTubeIconType.ShieldCheck,
                trailingIcon = JoyTubeIconType.NavSearch,
                onTrailingIconClick = { viewModel.openBrowserUrlOrSearch(uiState.browserAddressInput) },
                isCapsule = true,
                onSubmit = { viewModel.openBrowserUrlOrSearch(uiState.browserAddressInput) },
                modifier = Modifier.testTag("browser_address_input")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.activeBrowserUrl != null) {
            GeckoBrowserView(
                url = uiState.activeBrowserUrl,
                controller = browserController,
                onLocationChanged = viewModel::updateBrowserLocation,
                privacyFilteringEnabled = uiState.adBlockEnabled,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .clip(JoyTubeRadius.ThumbnailInner)
            )
        } else LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
                // Quick Access 3-Column Outlined Cards: YouTube | History | Downloads
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Quick access",
                            style = JoyTubeTypeScale.SectionHeading
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BrowserQuickAccessCard(
                                icon = JoyTubeIconType.Globe,
                                label = "YouTube",
                                onClick = { viewModel.openBrowserUrlOrSearch("https://m.youtube.com") },
                                modifier = Modifier.weight(1f)
                            )
                            BrowserQuickAccessCard(
                                icon = JoyTubeIconType.History,
                                label = "History",
                                onClick = {
                                    viewModel.setHistoryTab("Browser history")
                                    viewModel.navigateTo(JoyTubeRoute.History)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            BrowserQuickAccessCard(
                                icon = JoyTubeIconType.Download,
                                label = "Downloads",
                                onClick = { viewModel.navigateTo(JoyTubeRoute.Downloads) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

        }

        // Bottom Outlined Browser Toolbar: ← | → | ↻ | ⌂ | ... (Screenshot 1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(48.dp)
                .joyTubeOutlinedSurface(
                    shape = JoyTubeRadius.InputField,
                    backgroundColor = JoyTubeColors.Surface,
                    borderColor = JoyTubeColors.BorderStandard
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrowserToolbarIcon(icon = JoyTubeIconType.ArrowBack) {
                if (uiState.activeBrowserUrl != null && browserController.canGoBack) {
                    browserController.goBack()
                } else if (uiState.activeBrowserUrl != null) {
                    viewModel.updateBrowserAddressInput("")
                    viewModel.navigateBack()
                } else {
                    viewModel.navigateBack()
                }
            }
            BrowserToolbarIcon(icon = JoyTubeIconType.ArrowForward) {
                browserController.goForward()
            }
            BrowserToolbarIcon(icon = JoyTubeIconType.Refresh) {
                browserController.reload()
            }
            BrowserToolbarIcon(icon = JoyTubeIconType.NavHome) {
                viewModel.updateBrowserAddressInput("")
                if (uiState.activeBrowserUrl != null) viewModel.navigateBack()
            }
            BrowserToolbarIcon(icon = JoyTubeIconType.MoreHorizontal) {
                viewModel.showOverlay(ActiveOverlay.BrowserMenu)
            }
        }
    }
}

@Composable
private fun BrowserQuickAccessCard(
    icon: JoyTubeIconType,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(78.dp)
            .joyTubeOutlinedSurface(
                shape = JoyTubeRadius.ContentCard,
                backgroundColor = JoyTubeColors.Surface,
                borderColor = JoyTubeColors.BorderStandard
            )
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        JoyTubeSvgIcon(type = icon, size = 19.dp)
        Spacer(modifier = Modifier.height(7.dp))
        Text(text = label, style = JoyTubeTypeScale.Label)
    }
}

@Composable
private fun BrowserToolbarIcon(
    icon: JoyTubeIconType,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        JoyTubeSvgIcon(type = icon, size = 17.dp, tint = JoyTubeColors.TextPrimary)
    }
}

/**
 * 2. BROWSER TABS PAGE (PART IX — SECTION 32 & SCREENSHOT 1)
 */
@Composable
fun BrowserTabsScreen(
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tabs",
                style = JoyTubeTypeScale.PageTitle
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.createNewBrowserTab(false) },
                    contentAlignment = Alignment.Center
                ) {
                    JoyTubeSvgIcon(type = JoyTubeIconType.Plus, size = 18.dp)
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.navigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    JoyTubeSvgIcon(type = JoyTubeIconType.Close, size = 18.dp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("browser_tabs_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(uiState.browserTabs, key = { it.id }) { tab ->
                val isSelected = tab.id == uiState.activeBrowserTabId
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .joyTubeOutlinedSurface(
                            shape = JoyTubeRadius.ContentCard,
                            backgroundColor = JoyTubeColors.Surface,
                            borderColor = if (isSelected) JoyTubeColors.BorderFocused else JoyTubeColors.BorderStandard
                        )
                        .clickable { viewModel.selectBrowserTab(tab.id) }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            JoyTubeSvgIcon(
                                type = if (tab.isVideoPage) JoyTubeIconType.Play else JoyTubeIconType.Globe,
                                size = 16.dp
                            )
                            Text(
                                text = tab.title,
                                style = JoyTubeTypeScale.CardTitle
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.closeBrowserTab(tab.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            JoyTubeSvgIcon(
                                type = JoyTubeIconType.Close,
                                size = 14.dp,
                                tint = JoyTubeColors.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(106.dp)
                            .clip(JoyTubeRadius.ThumbnailInner)
                            .background(JoyTubeColors.Background, JoyTubeRadius.ThumbnailInner)
                            .border(1.dp, JoyTubeColors.BorderSubtle, JoyTubeRadius.ThumbnailInner),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            JoyTubeSvgIcon(
                                type = if (tab.isVideoPage) JoyTubeIconType.VideoCamera else JoyTubeIconType.Globe,
                                size = 20.dp,
                                tint = JoyTubeColors.TextSecondary
                            )
                            Text(
                                text = if (tab.isVideoPage) "Media preview" else "Website preview",
                                style = JoyTubeTypeScale.Label
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. AD-BLOCKER SETTINGS PAGE (PART IX — SECTION 33 & SCREENSHOT 18)
 *
 * Strictly limited to three functions:
 * 1. Block advertising requests
 * 2. Block pop-ups
 * 3. Hide cookie banners
 * + Media compatibility ("Protected")
 */
@Composable
fun AdBlockerSettingsScreen(
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
    ) {
        JoyTubeSecondaryTopBar(
            title = "Privacy filtering",
            onBackClick = { viewModel.navigateBack() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("ad_blocker_settings_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .joyTubeOutlinedSurface(
                            shape = JoyTubeRadius.ContentCard,
                            backgroundColor = JoyTubeColors.Surface,
                            borderColor = JoyTubeColors.BorderStandard
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Privacy filtering",
                            style = JoyTubeTypeScale.SectionHeading
                        )
                        Text(
                            text = if (uiState.adBlockEnabled) "On" else "Off",
                            style = JoyTubeTypeScale.Label.copy(
                                color = if (uiState.adBlockEnabled) JoyTubeColors.StatusSuccess else JoyTubeColors.TextSecondary
                            )
                        )
                    }

                    AdBlockToggleRow(
                        title = "Filter trackers in GeckoView",
                        subtitle = "Use Brave's Rust filter engine with the EasyPrivacy list.",
                        checked = uiState.adBlockEnabled,
                        onToggle = { viewModel.toggleAdBlockSetting("master") },
                        modifier = Modifier.testTag("privacy_filter_master_switch")
                    )

                    // Media compatibility row ("Protected")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                text = "YouTube compatibility",
                                    style = JoyTubeTypeScale.CardTitle
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                text = "YouTube domains and requests initiated by YouTube pages bypass filtering.",
                                    style = JoyTubeTypeScale.Metadata
                                )
                            }
                            Text(
                                text = "Protected",
                                style = JoyTubeTypeScale.Label.copy(color = JoyTubeColors.StatusSuccess)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This filters matching network requests in GeckoView only. It does not modify YouTube playback or YouTube advertising.",
                            style = JoyTubeTypeScale.Microcopy
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdBlockToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = JoyTubeTypeScale.CardTitle)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = subtitle, style = JoyTubeTypeScale.Metadata)
        }

        // Custom Outlined Capsule Switch matching Screenshot 18
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(22.dp)
                .clip(JoyTubeRadius.Capsule)
                .background(
                    if (checked) JoyTubeColors.Selected else JoyTubeColors.Background,
                    JoyTubeRadius.Capsule
                )
                .border(1.dp, JoyTubeColors.BorderFocused, JoyTubeRadius.Capsule)
                .padding(horizontal = 3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        if (checked) JoyTubeColors.TextPrimary else JoyTubeColors.TextSecondary,
                        CircleShape
                    )
            )
        }
    }
}

/**
 * 4. SETTINGS PAGE (PART X — SECTION 35 & SCREENSHOT 2)
 */
@Composable
fun SettingsScreen(
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
    ) {
        JoyTubeSecondaryTopBar(
            title = "Settings",
            onBackClick = { viewModel.navigateBack() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Group 0: Appearance — Glass Design Toggle (Enable & Disable Anytime)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Appearance",
                        style = JoyTubeTypeScale.CardTitle,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .joyTubeOutlinedSurface(
                                shape = JoyTubeRadius.ContentCard,
                                backgroundColor = JoyTubeColors.Surface,
                                borderColor = if (uiState.glassDesignEnabled) JoyTubeColors.BorderActiveWhite else JoyTubeColors.BorderStandard,
                                topHighlightAlpha = if (uiState.glassDesignEnabled) 0.28f else 0.08f
                            )
                            .clickable { viewModel.toggleGlassDesign() }
                            .padding(16.dp)
                            .testTag("settings_glass_design_toggle"),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            if (uiState.glassDesignEnabled) Color(0x28FFFFFF) else JoyTubeColors.SurfaceElevated,
                                            CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            if (uiState.glassDesignEnabled) Color(0x55FFFFFF) else JoyTubeColors.BorderStandard,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    JoyTubeSvgIcon(
                                        type = JoyTubeIconType.Sparkles,
                                        size = 17.dp,
                                        tint = JoyTubeColors.TextPrimary
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Glass Design",
                                        style = JoyTubeTypeScale.SectionHeading
                                    )
                                    Text(
                                        text = if (uiState.glassDesignEnabled) "Active · Translucent liquid glass" else "Disabled · Classic matte outline",
                                        style = JoyTubeTypeScale.Microcopy.copy(
                                            color = if (uiState.glassDesignEnabled) JoyTubeColors.StatusSuccess else JoyTubeColors.TextSecondary
                                        )
                                    )
                                }
                            }

                            // Outlined Capsule Switch
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(24.dp)
                                    .clip(JoyTubeRadius.Capsule)
                                    .background(
                                        if (uiState.glassDesignEnabled) JoyTubeColors.Selected else JoyTubeColors.Background,
                                        JoyTubeRadius.Capsule
                                    )
                                    .border(
                                        1.dp,
                                        if (uiState.glassDesignEnabled) JoyTubeColors.BorderActiveWhite else JoyTubeColors.BorderFocused,
                                        JoyTubeRadius.Capsule
                                    )
                                    .padding(horizontal = 3.dp),
                                contentAlignment = if (uiState.glassDesignEnabled) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (uiState.glassDesignEnabled) JoyTubeColors.TextPrimary else JoyTubeColors.TextSecondary,
                                            CircleShape
                                        )
                                )
                            }
                        }

                        Text(
                            text = "Enables translucent frosted glass surfaces, scroll-down/up floating navigation dock animation, liquid droplet tab transitions, and tactile spring physics on search, icons, and buttons.",
                            style = JoyTubeTypeScale.Metadata
                        )
                    }
                }
            }

            // Group 1: Playback
            item {
                SettingsGroupSection(
                    groupTitle = "Playback",
                    items = listOf(
                        Triple(JoyTubeIconType.Play, "Video playback") {
                            viewModel.showOverlay(ActiveOverlay.PlaybackSettings)
                        },
                        Triple(JoyTubeIconType.VideoCamera, "Video quality preferences") {
                            viewModel.showOverlay(ActiveOverlay.PlaybackSettings)
                        },
                        Triple(JoyTubeIconType.RotateScreen, "Screen rotation") {
                            viewModel.showOverlay(ActiveOverlay.PlaybackSettings)
                        },
                        Triple(JoyTubeIconType.Subtitles, "Subtitles") {
                            viewModel.showOverlay(ActiveOverlay.PlaybackSettings)
                        }
                    )
                )
            }

            // Group 2: Browser
            item {
                SettingsGroupSection(
                    groupTitle = "Browser",
                    items = listOf(
                        Triple(JoyTubeIconType.ShieldCheck, "Ad blocking") {
                            viewModel.navigateTo(JoyTubeRoute.AdBlockerSettings)
                        },
                        Triple(JoyTubeIconType.History, "Clear browsing data") {
                            viewModel.showOverlay(
                                ActiveOverlay.GenericConfirm(
                                    title = "Clear all browser history?",
                                    message = "This removes recorded website visits while preserving essential site login cookies.",
                                    confirmLabel = "Clear data",
                                    onConfirmAction = ConfirmActionType.ClearBrowsingData
                                )
                            )
                        },
                        Triple(JoyTubeIconType.TabsStack, "Tab settings") {
                            viewModel.navigateTo(JoyTubeRoute.BrowserTabs)
                        }
                    )
                )
            }

            // Group 3: Storage and privacy
            item {
                SettingsGroupSection(
                    groupTitle = "Storage and privacy",
                    items = listOf(
                        Triple(JoyTubeIconType.Database, "Storage management") {
                            viewModel.navigateTo(JoyTubeRoute.Downloads)
                        },
                        Triple(JoyTubeIconType.Lock, "Privacy and permissions") {
                            viewModel.showFeedback("Using zero-permission Android Photo Picker")
                        },
                        Triple(JoyTubeIconType.Folder, "Imported media management") {
                            viewModel.navigateTo(JoyTubeRoute.ImportedVideos)
                        }
                    )
                )
            }

            // Group 4: Application
            item {
                SettingsGroupSection(
                    groupTitle = "Application",
                    items = listOf(
                        Triple(
                            JoyTubeIconType.NavHome,
                            "Hide JoyTube's Nav · ${if (uiState.hideJoyTubeNav) "On" else "Off"}"
                        ) {
                            viewModel.toggleHideJoyTubeNav()
                        },
                        Triple(
                            JoyTubeIconType.Sparkles,
                            if (uiState.glassDesignEnabled) "Glass Design (Enabled)" else "Glass Design (Disabled)"
                        ) {
                            viewModel.toggleGlassDesign()
                        },
                        Triple(JoyTubeIconType.Bell, "Notifications") {
                            viewModel.showFeedback("Download & import completion alerts enabled")
                        },
                        Triple(JoyTubeIconType.Info, "About JoyTube") {
                            viewModel.navigateTo(JoyTubeRoute.AboutJoyTube)
                        },
                        Triple(JoyTubeIconType.Document, "Terms and privacy policy") {
                            viewModel.navigateTo(JoyTubeRoute.TermsAndPrivacy)
                        },
                        Triple(JoyTubeIconType.SignOut, "Sign out") {
                            viewModel.showOverlay(
                                ActiveOverlay.GenericConfirm(
                                    title = "Sign out of JoyTube?",
                                    message = "You can sign back in to @${uiState.username} at any time.",
                                    confirmLabel = "Sign out",
                                    onConfirmAction = ConfirmActionType.SignOut
                                )
                            )
                        }
                    )
                )
            }

        }
    }
}

@Composable
private fun SettingsGroupSection(
    groupTitle: String,
    items: List<Triple<JoyTubeIconType, String, () -> Unit>>
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = groupTitle,
            style = JoyTubeTypeScale.CardTitle,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        items.forEach { (icon, label, action) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(JoyTubeRadius.SmallControl)
                    .clickable { action() }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                JoyTubeSvgIcon(
                    type = icon,
                    size = 18.dp,
                    tint = JoyTubeColors.TextSecondary
                )
                Text(
                    text = label,
                    style = JoyTubeTypeScale.BodyText,
                    modifier = Modifier.weight(1f)
                )
                JoyTubeSvgIcon(
                    type = JoyTubeIconType.ChevronRight,
                    size = 16.dp,
                    tint = JoyTubeColors.TextSecondary
                )
            }
        }
    }
}

/**
 * 5. PROFILE PAGE (PART X — SECTION 34 & SCREENSHOTS 2, 14)
 */
@Composable
fun ProfileScreen(
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var draftName by remember(uiState.displayName) { mutableStateOf(uiState.displayName) }
    var draftUsername by remember(uiState.username) { mutableStateOf(uiState.username) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
    ) {
        JoyTubeSecondaryTopBar(
            title = "",
            onBackClick = { viewModel.navigateBack() },
            rightActionIcon1 = JoyTubeIconType.Settings,
            onRightAction1Click = { viewModel.navigateTo(JoyTubeRoute.Settings) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("profile_screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Centered Avatar + "Your Account" + "@username" + "Edit profile" pill (Screenshot 2)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(JoyTubeColors.Surface, CircleShape)
                            .border(1.dp, JoyTubeColors.BorderStandard, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        JoyTubeAccountAvatarBadge(
                            initial = uiState.authSession?.displayName?.take(1)
                                ?: uiState.displayName.take(1).ifEmpty { "D" },
                            size = 76.dp,
                            imageUrl = uiState.authSession?.avatarUrl
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.authSession?.displayName ?: uiState.displayName,
                        style = JoyTubeTypeScale.PageTitle
                    )
                    val accountEmail = uiState.authSession?.email ?: uiState.email
                    if (accountEmail.isNotBlank()) {
                        Text(
                            text = accountEmail,
                            style = JoyTubeTypeScale.Metadata
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    JoyTubeFilterPill(
                        label = if (isEditing) "Cancel edit" else "Edit profile",
                        isSelected = isEditing,
                        onClick = { isEditing = !isEditing }
                    )
                }
            }

            if (isEditing) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .joyTubeOutlinedSurface(
                                shape = JoyTubeRadius.ContentCard,
                                backgroundColor = JoyTubeColors.Surface,
                                borderColor = JoyTubeColors.BorderStandard
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "Edit profile", style = JoyTubeTypeScale.CardTitle)
                        JoyTubeTextInput(
                            value = draftName,
                            onValueChange = { draftName = it },
                            placeholder = "Display name"
                        )
                        JoyTubeTextInput(
                            value = draftUsername,
                            onValueChange = { draftUsername = it },
                            placeholder = "Username"
                        )
                        JoyTubePrimaryButton(
                            text = "Save changes",
                            onClick = {
                                viewModel.updateProfile(draftUsername, draftName)
                                isEditing = false
                            }
                        )
                    }
                }
            }

            // 2 Side-by-Side Stat Cards (Live Categories & Imported Items Count)
            item {
                val ownedCategoriesCount = uiState.categories.count { it.isOwnedByCurrentUser }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileStatCard(
                        number = ownedCategoriesCount.toString(),
                        label = "Categories",
                        onClick = { viewModel.navigateTo(JoyTubeRoute.MyCategories) },
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatCard(
                        number = uiState.importedItemsCount.toString(),
                        label = "Imported items",
                        onClick = { viewModel.navigateTo(JoyTubeRoute.ImportedVideos) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Profile Action Rows
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SettingsGroupSection(
                        groupTitle = "Account management",
                        items = listOf(
                            Triple(JoyTubeIconType.EditPen, "Edit username and profile") {
                                isEditing = true
                            },
                            Triple(JoyTubeIconType.ShieldCheck, "JoyTube account session") {
                                viewModel.showFeedback("Signed in with Google through Supabase Auth")
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStatCard(
    number: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .joyTubeOutlinedSurface(
                shape = JoyTubeRadius.ContentCard,
                backgroundColor = JoyTubeColors.Surface,
                borderColor = JoyTubeColors.BorderStandard
            )
            .clickable { onClick() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = number, style = JoyTubeTypeScale.PageTitle)
        Text(text = label, style = JoyTubeTypeScale.Metadata)
    }
}

/** Google ID-token sign-in creates a JoyTube Auth session in Supabase. */
@Composable
fun AuthFlowScreen(
    initialStep: AuthStep,
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    val activity = LocalContext.current as? Activity

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top bar: ONLY show a close button if the user is ALREADY signed in (e.g., clicked "Add account").
        // When signed out (!uiState.isSignedIn), NO back arrow is rendered so the user cannot go back without signing in!
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.isSignedIn) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.navigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    JoyTubeSvgIcon(type = JoyTubeIconType.ArrowBack, size = 18.dp)
                }
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }

            Box(
                modifier = Modifier
                    .joyTubeOutlinedSurface(
                        shape = JoyTubeRadius.Capsule,
                        backgroundColor = JoyTubeColors.SurfaceElevated,
                        borderColor = JoyTubeColors.BorderStandard
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (uiState.isSignedIn) "JoyTube profile" else "Sign in required",
                    style = JoyTubeTypeScale.Microcopy.copy(color = JoyTubeColors.TextSecondary)
                )
            }
        }

        // Google and JoyTube profile sign-in card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .joyTubeOutlinedSurface(
                        shape = JoyTubeRadius.ModalDialog,
                        backgroundColor = JoyTubeColors.Surface,
                        borderColor = JoyTubeColors.BorderActiveWhite,
                        topHighlightAlpha = 0.24f
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                JoyTubeSvgIcon(
                    type = JoyTubeIconType.BrandLogoLeftTriangle,
                    size = 44.dp,
                    tint = JoyTubeColors.TextPrimary
                )
                Text(
                    text = "JoyTube",
                    style = JoyTubeTypeScale.Display
                )
                Text(
                    text = "Sign in to JoyTube with your Google account.",
                    style = JoyTubeTypeScale.Metadata,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .joyTubeOutlinedSurface(
                            shape = JoyTubeRadius.ContentCard,
                            backgroundColor = JoyTubeColors.SurfaceElevated,
                            borderColor = JoyTubeColors.BorderStandard
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    JoyTubeAccountAvatarBadge(
                        initial = "G",
                        size = 42.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Google account",
                            style = JoyTubeTypeScale.CardTitle
                        )
                        Text(
                            text = "Email and basic profile",
                            style = JoyTubeTypeScale.Metadata
                        )
                    }
                    Text(
                        text = "Secure",
                        style = JoyTubeTypeScale.Label.copy(color = JoyTubeColors.TextPrimary)
                    )
                }

                JoyTubePrimaryButton(
                    text = "Continue with Google",
                    leadingIcon = JoyTubeIconType.Globe,
                    onClick = {
                        if (activity != null) viewModel.continueWithGoogleSignIn(activity)
                        else viewModel.showFeedback("Google sign-in needs an Android Activity. Please reopen JoyTube.")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("continue_with_google_button")
                )

                Text(
                    text = "JoyTube stores your account profile and settings. It does not store your Google password or YouTube activity.",
                    style = JoyTubeTypeScale.Microcopy
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun AboutJoyTubeScreen(viewModel: JoyTubeViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(JoyTubeColors.Background)
    ) {
        JoyTubeSecondaryTopBar(title = "About JoyTube", onBackClick = { viewModel.navigateBack() })
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DocumentCard(
                    title = "JoyTube for Android",
                    body = "Version ${com.example.BuildConfig.VERSION_NAME}. JoyTube helps you discover public YouTube and YouTube Music content and organize media you own or have permission to use."
                )
            }
            item {
                DocumentCard(
                    title = "Discovery and playback",
                    body = "Search results are requested from YouTube and YouTube Music using their web client interfaces. These are not the official YouTube Data API and can change without notice. YouTube content is played through YouTube's embedded player. Playback availability, ads, quality, captions, and live-stream support are controlled by YouTube and the content owner; JoyTube cannot guarantee every item will play."
                )
            }
            item {
                DocumentCard(
                    title = "Account and sync",
                    body = "Google sign-in is used to authenticate your JoyTube account through Supabase. JoyTube does not request your Google password or YouTube account access. Account profile details, preferences, collections, and imported-item metadata are synced through Supabase when the service is configured and reachable."
                )
            }
            item {
                DocumentCard(
                    title = "Files and downloads",
                    body = "Imported media files remain on your device; cloud records hold metadata and a local file reference may be kept on-device. JoyTube does not upload downloaded media files to Supabase. JoyTube does not provide a YouTube stream downloader."
                )
            }
            item {
                DocumentCard(
                    title = "Independence",
                    body = "JoyTube is an independent project and is not affiliated with, endorsed by, or sponsored by Google or YouTube."
                )
            }
        }
    }
}

@Composable
fun TermsAndPrivacyScreen(viewModel: JoyTubeViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(JoyTubeColors.Background)
    ) {
        JoyTubeSecondaryTopBar(title = "Terms and privacy", onBackClick = { viewModel.navigateBack() })
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DocumentCard(
                    title = "Using JoyTube",
                    body = "Use JoyTube lawfully and respect the rights of creators and service providers. Do not use it to copy, redistribute, or download content unless you have the rights or the service explicitly permits it. YouTube playback remains subject to YouTube's terms and the content owner's settings."
                )
            }
            item {
                DocumentCard(
                    title = "Information JoyTube handles",
                    body = "When you sign in, Supabase Auth receives your authentication request and JoyTube uses your account ID, email, display name, username, and available profile image. JoyTube may sync your app theme and settings, collection names and descriptions, imported-item metadata, favourites, recent imports, and related counts. Cloud operations require a connection and are subject to the project's Supabase configuration and access policies."
                )
            }
            item {
                DocumentCard(
                    title = "Information kept on your device",
                    body = "Selected local media references, app state, and files you import or download are kept on this device. Media files are not uploaded to Supabase by JoyTube. Removing the app or clearing its data may remove app-private files and locally stored references."
                )
            }
            item {
                DocumentCard(
                    title = "YouTube and Google services",
                    body = "Search requests go to YouTube or YouTube Music, and playback loads YouTube's embedded player. Those services receive network and request information under their own policies. JoyTube does not access or store your YouTube subscriptions, likes, dislikes, Google password, or YouTube account credentials, and it does not write your viewing activity to YouTube watch history through an account API."
                )
            }
            item {
                DocumentCard(
                    title = "Security and availability",
                    body = "JoyTube uses Supabase authentication and user-scoped database access for cloud records. Keep your device secure and sign out on shared devices. Network services, search interfaces, and playback are provided by third parties and may be unavailable or change. JoyTube is provided without a guarantee of uninterrupted service or playback."
                )
            }
            item {
                DocumentCard(
                    title = "Account deletion",
                    body = "Permanent account deletion is not currently available inside JoyTube. Signing out only ends the session and does not delete cloud profile or library records. Do not treat sign-out as an account deletion request."
                )
            }
            item {
                val uriHandler = LocalUriHandler.current
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .joyTubeOutlinedSurface(
                            shape = JoyTubeRadius.ContentCard,
                            backgroundColor = JoyTubeColors.Surface,
                            borderColor = JoyTubeColors.BorderStandard
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Third-party policies", style = JoyTubeTypeScale.CardTitle)
                    listOf(
                        "YouTube Terms of Service" to "https://www.youtube.com/t/terms",
                        "Google Privacy Policy" to "https://policies.google.com/privacy",
                        "Supabase Privacy Policy" to "https://supabase.com/privacy"
                    ).forEach { (label, url) ->
                        Text(
                            text = label,
                            style = JoyTubeTypeScale.Metadata.copy(color = JoyTubeColors.TextPrimary),
                            modifier = Modifier.clickable { runCatching { uriHandler.openUri(url) } }
                        )
                    }
                }
            }
            item {
                DocumentCard(
                    title = "Contact and changes",
                    body = "These in-app terms and privacy notes describe the current app behavior; they are not a promise that every planned feature exists. The project owner may update the app and this text as behavior changes. For questions or corrections, use the JoyTube project's issue tracker on GitHub."
                )
            }
        }
    }
}

@Composable
private fun DocumentCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .joyTubeOutlinedSurface(
                shape = JoyTubeRadius.ContentCard,
                backgroundColor = JoyTubeColors.Surface,
                borderColor = JoyTubeColors.BorderStandard
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = title, style = JoyTubeTypeScale.CardTitle)
        Text(text = body, style = JoyTubeTypeScale.Metadata)
    }
}

/**
 * 7. DESIGN SYSTEM & VISUAL ENGINEERING SHOWCASE (PART XIV — SECTIONS 48-50 & SCREENSHOTS 10, 14, 16)
 *
 * Interactive specification screen displaying the exact tokens, floating dock states,
 * button & input states, empty states, and navigation rules from the reference sheets.
 */
@Composable
fun DesignSystemScreen(
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    var previewDockTab by remember { mutableStateOf(PrimaryTab.Home) }
    var demoInputText by remember { mutableStateOf("") }
    var selectedSegment by remember { mutableStateOf("Selected") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
    ) {
        JoyTubeSecondaryTopBar(
            title = "Visual Design System",
            onBackClick = { viewModel.navigateBack() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("design_system_screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Pure black. Pure focus. + Colour Tokens (Screenshot 10)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .joyTubeOutlinedSurface(
                            shape = JoyTubeRadius.ContentCard,
                            backgroundColor = JoyTubeColors.Background,
                            borderColor = JoyTubeColors.BorderStandard
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Pure black. Pure focus.", style = JoyTubeTypeScale.Display)
                    Text(
                        text = "A minimal video experience with clean surfaces and carefully spaced controls.",
                        style = JoyTubeTypeScale.SupportingText
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ColorTokenSwatchCard(
                            label = "Background",
                            hex = "#050505",
                            color = JoyTubeColors.Background,
                            modifier = Modifier.weight(1f)
                        )
                        ColorTokenSwatchCard(
                            label = "Surface",
                            hex = "#101010",
                            color = JoyTubeColors.Surface,
                            modifier = Modifier.weight(1f)
                        )
                        ColorTokenSwatchCard(
                            label = "Selected",
                            hex = "#242424",
                            color = JoyTubeColors.Selected,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section 2: Premium Floating Navigation Dock Specification (Screenshot 10)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .joyTubeOutlinedSurface(
                            shape = JoyTubeRadius.ContentCard,
                            backgroundColor = JoyTubeColors.Surface,
                            borderColor = JoyTubeColors.BorderStandard
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${previewDockTab.name.uppercase()} SELECTED",
                        style = JoyTubeTypeScale.Microcopy
                    )
                    JoyTubeFloatingNavigationDock(
                        selectedTab = previewDockTab,
                        onSelectTab = { previewDockTab = it }
                    )
                    Text(
                        text = "The selected circle slides smoothly between the four unchanged icons.",
                        style = JoyTubeTypeScale.Metadata,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Section 3: Button and Input Design (Screenshot 14 & 16)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .joyTubeOutlinedSurface(
                            shape = JoyTubeRadius.ContentCard,
                            backgroundColor = JoyTubeColors.Surface,
                            borderColor = JoyTubeColors.BorderStandard
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Primary button", style = JoyTubeTypeScale.CardTitle)
                    JoyTubePrimaryButton(
                        text = "Import now",
                        onClick = { viewModel.navigateTo(JoyTubeRoute.ImportMedia) }
                    )

                    Text(text = "Secondary button", style = JoyTubeTypeScale.CardTitle)
                    JoyTubeSecondaryButton(
                        text = "Cancel",
                        onClick = { viewModel.showFeedback("Secondary button tapped") }
                    )

                    Text(text = "Icon button", style = JoyTubeTypeScale.CardTitle)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        JoyTubeIconButton(
                            icon = JoyTubeIconType.Plus,
                            onClick = { viewModel.showOverlay(ActiveOverlay.CreateCategoryModal) }
                        )
                        JoyTubeIconButton(
                            icon = JoyTubeIconType.HeartOutline,
                            onClick = { viewModel.showFeedback("Added to favourites") }
                        )
                        JoyTubeIconButton(
                            icon = JoyTubeIconType.MoreHorizontal,
                            onClick = {
                                val firstMedia = uiState.mediaList.firstOrNull()
                                if (firstMedia != null) {
                                    viewModel.showOverlay(ActiveOverlay.VideoOptions(firstMedia))
                                } else {
                                    viewModel.showFeedback("Import a video first to open video options")
                                }
                            }
                        )
                    }

                    Text(text = "Text input", style = JoyTubeTypeScale.CardTitle)
                    JoyTubeTextInput(
                        value = demoInputText,
                        onValueChange = { demoInputText = it },
                        placeholder = "Enter category name"
                    )

                    Text(text = "Selected control", style = JoyTubeTypeScale.CardTitle)
                    JoyTubeSegmentedRow(
                        options = listOf("Selected", "Unselected"),
                        selectedOption = selectedSegment,
                        onSelect = { selectedSegment = it }
                    )
                }
            }

            // Section 4: Designed Empty States Grid (Screenshot 16)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Empty states", style = JoyTubeTypeScale.SectionHeading)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        JoyTubeEmptyStateCard(
                            icon = JoyTubeIconType.Folder,
                            title = "No categories yet",
                            subtitle = "Create your first category.",
                            actionLabel = "Create category",
                            onActionClick = { viewModel.showOverlay(ActiveOverlay.CreateCategoryModal) },
                            modifier = Modifier.weight(1f)
                        )
                        JoyTubeEmptyStateCard(
                            icon = JoyTubeIconType.HeartOutline,
                            title = "No favourites yet",
                            subtitle = "Your saved items appear here.",
                            actionLabel = "Explore videos",
                            onActionClick = { viewModel.selectPrimaryTab(PrimaryTab.Home) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        JoyTubeEmptyStateCard(
                            icon = JoyTubeIconType.Download,
                            title = "No downloads",
                            subtitle = "Your downloaded media appears here.",
                            actionLabel = "Browse videos",
                            onActionClick = { viewModel.selectPrimaryTab(PrimaryTab.Home) },
                            modifier = Modifier.weight(1f)
                        )
                        JoyTubeEmptyStateCard(
                            icon = JoyTubeIconType.NavSearch,
                            title = "No results found",
                            subtitle = "Try another search.",
                            actionLabel = "Clear search",
                            onActionClick = { viewModel.selectPrimaryTab(PrimaryTab.Search) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorTokenSwatchCard(
    label: String,
    hex: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .joyTubeOutlinedSurface(
                shape = JoyTubeRadius.InputField,
                backgroundColor = color,
                borderColor = JoyTubeColors.BorderStandard
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, style = JoyTubeTypeScale.Microcopy)
        Text(text = hex, style = JoyTubeTypeScale.MonoToken)
    }
}
