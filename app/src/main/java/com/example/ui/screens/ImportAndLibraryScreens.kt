package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
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
 * 1. IMPORT MEDIA SCREEN (PART VII — SECTION 24 & SCREENSHOT 8)
 */
@Composable
fun ImportMediaScreen(
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.startImportFromDeviceUri(uri.toString())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
    ) {
        JoyTubeSecondaryTopBar(
            title = "Import media",
            onBackClick = { viewModel.navigateBack() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("import_media_screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Paste video link (Screenshot 8)
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
                    JoyTubeSvgIcon(type = JoyTubeIconType.Link, size = 20.dp)
                    Text(
                        text = "Paste video link",
                        style = JoyTubeTypeScale.CardTitle
                    )
                    Text(
                        text = "Enter a supported video or media URL",
                        style = JoyTubeTypeScale.Metadata
                    )
                    JoyTubeTextInput(
                        value = uiState.importUrlInput,
                        onValueChange = { viewModel.updateImportUrl(it) },
                        placeholder = "https://...",
                        errorMessage = uiState.importUrlError,
                        onSubmit = { viewModel.startImportFromUrl() },
                        modifier = Modifier.testTag("import_url_input")
                    )
                    JoyTubePrimaryButton(
                        text = "Find media",
                        onClick = { viewModel.startImportFromUrl() },
                        modifier = Modifier.testTag("find_media_button")
                    )
                }
            }

            // Card 2: Import from device (Screenshot 8)
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
                    JoyTubeSvgIcon(type = JoyTubeIconType.NavShorts, size = 20.dp)
                    Text(
                        text = "Import from device",
                        style = JoyTubeTypeScale.CardTitle
                    )
                    Text(
                        text = "Choose a supported video file from your device.",
                        style = JoyTubeTypeScale.Metadata
                    )
                    JoyTubeSecondaryButton(
                        text = "Choose file",
                        onClick = {
                            try {
                                mediaPickerLauncher.launch(arrayOf("video/*", "audio/*", "image/*"))
                            } catch (_: Exception) {
                                viewModel.showFeedback("Could not open the device file picker. Please try again.")
                            }
                        },
                        modifier = Modifier.testTag("choose_file_button")
                    )
                }
            }
        }
    }
}

/**
 * 2. IMPORT PREVIEW SCREEN ("Review import" — PART VII — SECTION 25 & SCREENSHOTS 8, 13)
 */
@Composable
fun ImportPreviewScreen(
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    val draft = uiState.pendingImport
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    val selectedCategory = uiState.categories.find { it.id == draft.targetCategoryId }
    val isShortOverLimit = draft.type == MediaType.Short && draft.durationSeconds > 600

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
                text = "Review import",
                style = JoyTubeTypeScale.SectionHeading
            )
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("import_preview_screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Auto-generated thumbnail or fallback with retry route (Section 25)
            item {
                if (draft.thumbnailFailed) {
                    JoyTubeEmptyStateCard(
                        icon = JoyTubeIconType.Refresh,
                        title = "Thumbnail unavailable",
                        subtitle = "Automatic thumbnail generation failed for this source.",
                        actionLabel = "Retry thumbnail generation",
                        onActionClick = { viewModel.togglePendingThumbnailFailure() }
                    )
                } else {
                    JoyTubeMediaThumbnail(
                        preset = draft.artwork,
                        customUri = draft.customUri,
                        sourceBadge = draft.sourceDomain,
                        showBookmark = true,
                        isBookmarked = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 10f)
                            .clip(JoyTubeRadius.ContentCard)
                            .border(1.dp, JoyTubeColors.BorderStandard, JoyTubeRadius.ContentCard)
                    )
                }
            }

            // Title + "Thumbnail automatically generated from the media" + Refresh button (Screenshot 13)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = draft.title,
                            style = JoyTubeTypeScale.CardTitle
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (draft.thumbnailFailed) {
                                "Thumbnail unavailable with retry option"
                            } else {
                                "Thumbnail automatically generated from the media"
                            },
                            style = JoyTubeTypeScale.Metadata
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.togglePendingThumbnailFailure() },
                        contentAlignment = Alignment.Center
                    ) {
                        JoyTubeSvgIcon(
                            type = JoyTubeIconType.Refresh,
                            size = 16.dp,
                            tint = JoyTubeColors.TextSecondary
                        )
                    }
                }
            }

            // Content type selector: Video | Short (Screenshot 13)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Content type", style = JoyTubeTypeScale.CardTitle)
                        // Duration toggle to test 10-minute Short limit validation
                        Text(
                            text = if (draft.durationSeconds > 600) "Duration: 12:30 (Test >10m)" else "Duration: 07:45",
                            style = JoyTubeTypeScale.Microcopy.copy(
                                color = if (isShortOverLimit) JoyTubeColors.StatusError else JoyTubeColors.TextSecondary
                            ),
                            modifier = Modifier
                                .clip(JoyTubeRadius.SmallControl)
                                .clickable {
                                    viewModel.updatePendingImportDuration(
                                        if (draft.durationSeconds > 600) 465 else 750
                                    )
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ContentTypeCard(
                            icon = JoyTubeIconType.VideoCamera,
                            label = "Video",
                            isSelected = draft.type == MediaType.Video,
                            onClick = { viewModel.updatePendingImportType(MediaType.Video) },
                            modifier = Modifier.weight(1f)
                        )
                        ContentTypeCard(
                            icon = JoyTubeIconType.NavShorts,
                            label = "Short",
                            isSelected = draft.type == MediaType.Short,
                            onClick = { viewModel.updatePendingImportType(MediaType.Short) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (draft.type == MediaType.Short) {
                        Text(
                            text = if (isShortOverLimit) {
                                "Validation error: Shorts have a 10-minute maximum duration (selected media is 12:30)."
                            } else {
                                "Maximum duration for Shorts is 10 minutes. Regular videos have no duration limit."
                            },
                            style = JoyTubeTypeScale.Metadata.copy(
                                color = if (isShortOverLimit) JoyTubeColors.StatusError else JoyTubeColors.TextSecondary
                            )
                        )
                    }
                }
            }

            // Visibility selector: Public | Private (Screenshot 13)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Visibility", style = JoyTubeTypeScale.CardTitle)
                    JoyTubeSegmentedRow(
                        options = listOf("Public", "Private"),
                        selectedOption = draft.visibility.label,
                        onSelect = { label ->
                            viewModel.updatePendingImportVisibility(
                                if (label == "Public") ContentVisibility.Public else ContentVisibility.Private
                            )
                        }
                    )
                }
            }

            // Add to category selector (Screenshot 13)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Add to category", style = JoyTubeTypeScale.CardTitle)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .joyTubeOutlinedSurface(
                                    shape = JoyTubeRadius.InputField,
                                    backgroundColor = JoyTubeColors.Surface,
                                    borderColor = JoyTubeColors.BorderStandard
                                )
                                .clickable { categoryDropdownExpanded = true }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = selectedCategory?.name ?: "Select a category",
                                style = JoyTubeTypeScale.BodyText
                            )
                            JoyTubeSvgIcon(
                                type = JoyTubeIconType.ChevronDown,
                                size = 16.dp,
                                tint = JoyTubeColors.TextSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false },
                            modifier = Modifier
                                .background(JoyTubeColors.SurfaceElevated)
                                .border(1.dp, JoyTubeColors.BorderStandard, JoyTubeRadius.InputField)
                        ) {
                            uiState.categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${cat.name} (${cat.visibility.label})",
                                            style = JoyTubeTypeScale.BodyText
                                        )
                                    },
                                    onClick = {
                                        viewModel.updatePendingImportCategory(cat.id)
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Import now button
            item {
                Spacer(modifier = Modifier.height(4.dp))
                JoyTubePrimaryButton(
                    text = "Import now",
                    enabled = !isShortOverLimit,
                    onClick = { viewModel.confirmImportNow() },
                    modifier = Modifier.testTag("confirm_import_now_button")
                )
            }
        }
    }
}

@Composable
private fun ContentTypeCard(
    icon: JoyTubeIconType,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) JoyTubeColors.SurfaceElevated else JoyTubeColors.Background
    val border = if (isSelected) Color(0x55FFFFFF) else JoyTubeColors.BorderStandard
    Column(
        modifier = modifier
            .height(68.dp)
            .clip(JoyTubeRadius.InputField)
            .background(bg, JoyTubeRadius.InputField)
            .border(1.dp, border, JoyTubeRadius.InputField)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        JoyTubeSvgIcon(type = icon, size = 18.dp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, style = JoyTubeTypeScale.ButtonText)
    }
}

/**
 * 3. IMPORT COMPLETION SCREEN (SCREENSHOTS 5, 13)
 */
@Composable
fun ImportCompletionScreen(
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    val latestImported = uiState.mediaList.firstOrNull()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .joyTubeOutlinedSurface(
                    shape = JoyTubeRadius.ModalDialog,
                    backgroundColor = JoyTubeColors.Surface,
                    borderColor = JoyTubeColors.BorderStandard
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(JoyTubeColors.Background, CircleShape)
                    .border(1.dp, JoyTubeColors.BorderStandard, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                JoyTubeSvgIcon(type = JoyTubeIconType.Check, size = 20.dp)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Import complete",
                style = JoyTubeTypeScale.SectionHeading
            )
            Text(
                text = "Stored on this device · Total imported items: ${uiState.importedItemsCount}",
                style = JoyTubeTypeScale.SupportingText
            )
            Spacer(modifier = Modifier.height(6.dp))

            JoyTubePrimaryButton(
                text = "Watch now",
                onClick = {
                    if (latestImported != null) {
                        viewModel.navigateTo(JoyTubeRoute.VideoPlayer(latestImported.id))
                    } else {
                        viewModel.selectPrimaryTab(PrimaryTab.Home)
                    }
                }
            )
            JoyTubeSecondaryButton(
                text = "Go to imported videos",
                onClick = { viewModel.navigateTo(JoyTubeRoute.ImportedVideos) }
            )
        }
    }
}

/**
 * 4. IMPORTED VIDEOS LIBRARY SCREEN (PART VII — SECTION 26 & SCREENSHOT 18)
 */
@Composable
fun NewImportedJoysScreen(
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
            title = "New Imported Joy's",
            onBackClick = { viewModel.navigateBack() },
            rightActionIcon1 = JoyTubeIconType.Refresh,
            onRightAction1Click = { viewModel.refreshPublicJoys() }
        )
        when {
            uiState.isLoadingPublicJoys -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading public Joys…", style = JoyTubeTypeScale.Metadata)
            }
            uiState.publicJoysError != null -> Box(
                Modifier.fillMaxSize().padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Public Joys could not load", style = JoyTubeTypeScale.SectionHeading)
                    Text(uiState.publicJoysError, style = JoyTubeTypeScale.Metadata)
                    JoyTubePrimaryButton(text = "Retry", onClick = { viewModel.refreshPublicJoys() })
                }
            }
            uiState.publicJoys.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                JoyTubeEmptyStateCard(
                    icon = JoyTubeIconType.Globe,
                    title = "No public Joys yet",
                    subtitle = "Publicly published videos, Shorts, and music will appear here.",
                    actionLabel = "Refresh",
                    onActionClick = { viewModel.refreshPublicJoys() }
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("new_imported_joys_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.publicJoys, key = { it.id }) { item ->
                    CompactMediaRowItem(
                        media = item,
                        onClick = { item.remoteMediaUrl?.let(viewModel::openBrowserUrlOrSearch) },
                        subtitleOverride = "By @${item.importerUsername}",
                        secondaryMeta = "${item.mediaKindLabel ?: "Video"} · Public",
                        trailingIcon = JoyTubeIconType.Play,
                        onTrailingClick = { item.remoteMediaUrl?.let(viewModel::openBrowserUrlOrSearch) }
                    )
                }
            }
        }
    }
}

/**
 * 5. IMPORTED VIDEOS LIBRARY SCREEN (PART VII — SECTION 26 & SCREENSHOT 18)
 */
@Composable
fun ImportedVideosScreen(
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    val ownedMedia = remember(uiState.mediaList) {
        uiState.mediaList.filter { it.isOwnedByCurrentUser }
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
                onSelectAll = { viewModel.selectAllMedia(ownedMedia.map { it.id }) }
            )
        } else {
            JoyTubeSecondaryTopBar(
                title = "Imported Videos (${uiState.importedItemsCount})",
                onBackClick = { viewModel.navigateBack() },
                rightActionIcon1 = JoyTubeIconType.Plus,
                onRightAction1Click = { viewModel.navigateTo(JoyTubeRoute.ImportMedia) }
            )
        }

        if (ownedMedia.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                JoyTubeEmptyStateCard(
                    icon = JoyTubeIconType.VideoCamera,
                    title = "No imported videos yet",
                    subtitle = "Import supported videos or Shorts from a link or your device.",
                    actionLabel = "Import media",
                    onActionClick = { viewModel.navigateTo(JoyTubeRoute.ImportMedia) }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("imported_videos_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ownedMedia, key = { it.id }) { item ->
                    val isSel = item.id in uiState.selectedMediaIds
                    CompactMediaRowItem(
                        media = item,
                        onClick = {
                            if (item.type == MediaType.Short) {
                                viewModel.navigateTo(JoyTubeRoute.FullscreenShorts(item.id))
                            } else {
                                viewModel.navigateTo(JoyTubeRoute.VideoPlayer(item.id))
                            }
                        },
                        onLongClick = {
                            if (uiState.isMediaSelectionMode) viewModel.toggleMediaSelection(item.id)
                            else viewModel.enterMediaSelectionMode(item.id)
                        },
                        subtitleOverride = "Imported by @${item.importerUsername}",
                        secondaryMeta = "${item.type.label} · ${item.visibility.label} · ${item.durationLabel}",
                        isSelectionMode = uiState.isMediaSelectionMode,
                        isSelected = isSel,
                        onTrailingClick = { viewModel.showOverlay(ActiveOverlay.VideoOptions(item)) }
                    )
                }
            }

            if (uiState.isMediaSelectionMode) {
                BulkMediaSelectionToolbar(
                    onMoveClick = {
                        viewModel.showOverlay(ActiveOverlay.AddToCategory(uiState.selectedMediaIds.toList()))
                    },
                    onFavouriteClick = { viewModel.bulkFavouriteSelectedMedia() },
                    onDownloadClick = {
                        val first = ownedMedia.find { it.id in uiState.selectedMediaIds }
                        if (first != null) viewModel.showOverlay(ActiveOverlay.DownloadQuality(first))
                    },
                    onDeleteClick = {
                        viewModel.showOverlay(ActiveOverlay.DeleteSelectedMediaConfirm(uiState.selectedMediaIds))
                    }
                )
            }
        }
    }
}

/**
 * 5. FAVOURITES PAGE (PART VIII — SECTION 28 & SCREENSHOT 5)
 *
 * Favourites contains saved videos, Shorts and public categories.
 * Uses three small filter tabs (Videos, Shorts, Categories), not a social feed.
 */
@Composable
fun FavouritesScreen(
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    val favVideos = remember(uiState.mediaList) {
        uiState.mediaList.filter { it.isFavourite && it.type == MediaType.Video }
    }
    val favShorts = remember(uiState.mediaList) {
        uiState.mediaList.filter { it.isFavourite && it.type == MediaType.Short }
    }
    val favCategories = remember(uiState.categories) {
        uiState.categories.filter { it.isFavourite && it.visibility == ContentVisibility.Public }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
    ) {
        JoyTubeSecondaryTopBar(
            title = "Favourites",
            onBackClick = { viewModel.navigateBack() },
            rightActionIcon1 = JoyTubeIconType.NavSearch,
            onRightAction1Click = { viewModel.selectPrimaryTab(PrimaryTab.Search) }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Videos", "Shorts", "Categories").forEach { tab ->
                JoyTubeFilterPill(
                    label = tab,
                    isSelected = uiState.favouritesTab == tab,
                    onClick = { viewModel.setFavouritesTab(tab) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("favourites_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (uiState.favouritesTab) {
                "Videos" -> {
                    if (favVideos.isEmpty() || uiState.simulationMode == UiSimulationMode.Empty) {
                        item {
                            JoyTubeEmptyStateCard(
                                icon = JoyTubeIconType.HeartOutline,
                                title = "No favourites yet",
                                subtitle = "Your saved items appear here.",
                                actionLabel = "Explore videos",
                                onActionClick = {
                                    viewModel.setSimulationMode(UiSimulationMode.Populated)
                                    viewModel.selectPrimaryTab(PrimaryTab.Home)
                                }
                            )
                        }
                    } else {
                        items(favVideos, key = { it.id }) { video ->
                            CompactMediaRowItem(
                                media = video,
                                onClick = { viewModel.navigateTo(JoyTubeRoute.VideoPlayer(video.id)) },
                                trailingIcon = JoyTubeIconType.HeartOutline,
                                onTrailingClick = { viewModel.toggleMediaFavourite(video.id) }
                            )
                        }
                    }
                }

                "Shorts" -> {
                    if (favShorts.isEmpty()) {
                        item {
                            JoyTubeEmptyStateCard(
                                icon = JoyTubeIconType.HeartOutline,
                                title = "No favourite Shorts yet",
                                subtitle = "Your saved Shorts appear here.",
                                actionLabel = "Explore Shorts",
                                onActionClick = { viewModel.selectPrimaryTab(PrimaryTab.Shorts) }
                            )
                        }
                    } else {
                        items(favShorts, key = { it.id }) { shortItem ->
                            CompactMediaRowItem(
                                media = shortItem,
                                onClick = { viewModel.navigateTo(JoyTubeRoute.FullscreenShorts(shortItem.id)) },
                                trailingIcon = JoyTubeIconType.HeartOutline,
                                onTrailingClick = { viewModel.toggleMediaFavourite(shortItem.id) }
                            )
                        }
                    }
                }

                "Categories" -> {
                    if (favCategories.isEmpty()) {
                        item {
                            JoyTubeEmptyStateCard(
                                icon = JoyTubeIconType.HeartOutline,
                                title = "No favourite categories yet",
                                subtitle = "Public categories you save appear here.",
                                actionLabel = "Search categories",
                                onActionClick = { viewModel.selectPrimaryTab(PrimaryTab.Search) }
                            )
                        }
                    } else {
                        items(favCategories, key = { it.id }) { cat ->
                            PublicCategorySearchRow(
                                category = cat,
                                onClick = { viewModel.navigateTo(JoyTubeRoute.CategoryDetail(cat.id)) },
                                onFavouriteToggle = { viewModel.toggleCategoryFavourite(cat.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 6. DOWNLOADS PAGE (PART VIII — SECTION 29 & SCREENSHOT 5)
 */
@Composable
fun DownloadsScreen(
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    val filteredDownloads = remember(uiState.downloads, uiState.downloadsFilter) {
        uiState.downloads.filter { dl ->
            when (uiState.downloadsFilter) {
                "Downloading" -> dl.status == DownloadStatus.Downloading || dl.status == DownloadStatus.Paused
                "Completed" -> dl.status == DownloadStatus.Completed
                else -> true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
    ) {
        JoyTubeSecondaryTopBar(
            title = "Downloads",
            onBackClick = { viewModel.navigateBack() },
            rightActionIcon1 = JoyTubeIconType.Sliders,
            onRightAction1Click = { viewModel.navigateTo(JoyTubeRoute.Settings) }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Downloading", "Completed").forEach { tab ->
                JoyTubeFilterPill(
                    label = tab,
                    isSelected = uiState.downloadsFilter == tab,
                    onClick = { viewModel.setDownloadsFilter(tab) }
                )
            }
        }

        if (filteredDownloads.isEmpty() || uiState.simulationMode == UiSimulationMode.Empty) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                JoyTubeEmptyStateCard(
                    icon = JoyTubeIconType.Download,
                    title = "No downloads",
                    subtitle = "Your downloaded media appears here.",
                    actionLabel = "Browse videos",
                    onActionClick = {
                        viewModel.setSimulationMode(UiSimulationMode.Populated)
                        viewModel.selectPrimaryTab(PrimaryTab.Home)
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("downloads_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredDownloads, key = { it.id }) { dl ->
                    DownloadItemCard(
                        entry = dl,
                        onPlayOffline = { viewModel.playDownloadedFile(dl.id) },
                        onTogglePauseResume = { viewModel.togglePauseResumeDownload(dl.id) },
                        onCancelOrDelete = { isCancel -> viewModel.removeDownloadEntry(dl.id, isCancel) }
                    )
                }
            }
        }

        // Bottom Storage Used Indicator
        val downloadedBytes = uiState.downloads
            .filter { it.status == DownloadStatus.Completed }
            .sumOf { it.sizeBytes }
        val usedLabel = android.text.format.Formatter.formatShortFileSize(
            androidx.compose.ui.platform.LocalContext.current,
            downloadedBytes
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Storage used", style = JoyTubeTypeScale.Metadata)
                Text(
                    text = "$usedLabel used on this device",
                    style = JoyTubeTypeScale.Label
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun DownloadItemCard(
    entry: DownloadEntry,
    onPlayOffline: () -> Unit,
    onTogglePauseResume: () -> Unit,
    onCancelOrDelete: (Boolean) -> Unit
) {
    val isCompleted = entry.status == DownloadStatus.Completed
    val isPaused = entry.status == DownloadStatus.Paused
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .joyTubeOutlinedSurface(
                shape = JoyTubeRadius.ContentCard,
                backgroundColor = JoyTubeColors.Surface,
                borderColor = JoyTubeColors.BorderStandard
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            JoyTubeMediaThumbnail(
                preset = entry.artwork,
                modifier = Modifier
                    .width(96.dp)
                    .height(58.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = JoyTubeTypeScale.CardTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${entry.quality} · ${entry.sizeLabel}",
                    style = JoyTubeTypeScale.Metadata
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isCompleted) "Downloaded" else "${entry.status.label} · ${entry.progressPercent}%",
                    style = JoyTubeTypeScale.Microcopy.copy(color = JoyTubeColors.TextPrimary)
                )
            }

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .border(1.dp, JoyTubeColors.BorderStandard, CircleShape)
                    .clickable {
                        if (isCompleted) onPlayOffline() else onTogglePauseResume()
                    },
                contentAlignment = Alignment.Center
            ) {
                JoyTubeSvgIcon(
                    type = when {
                        isCompleted -> JoyTubeIconType.Check
                        isPaused -> JoyTubeIconType.Play
                        else -> JoyTubeIconType.Pause
                    },
                    size = 13.dp
                )
            }
        }

        if (!isCompleted) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(JoyTubeColors.BorderStandard)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((entry.progressPercent / 100f).coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(JoyTubeColors.TextSecondary)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCompleted) {
                Text(
                    text = "Play offline",
                    style = JoyTubeTypeScale.Microcopy.copy(color = JoyTubeColors.TextSecondary),
                    modifier = Modifier
                        .clip(JoyTubeRadius.SmallControl)
                        .clickable { onPlayOffline() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Delete",
                    style = JoyTubeTypeScale.Microcopy.copy(color = JoyTubeColors.TextSecondary),
                    modifier = Modifier
                        .clip(JoyTubeRadius.SmallControl)
                        .clickable { onCancelOrDelete(false) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            } else {
                Text(
                    text = if (isPaused) "Resume" else "Pause",
                    style = JoyTubeTypeScale.Microcopy.copy(color = JoyTubeColors.TextSecondary),
                    modifier = Modifier
                        .clip(JoyTubeRadius.SmallControl)
                        .clickable { onTogglePauseResume() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cancel",
                    style = JoyTubeTypeScale.Microcopy.copy(color = JoyTubeColors.TextSecondary),
                    modifier = Modifier
                        .clip(JoyTubeRadius.SmallControl)
                        .clickable { onCancelOrDelete(true) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * 7. WATCH HISTORY & BROWSER HISTORY PAGE (PART VIII — SECTION 30 & SCREENSHOT 3)
 */
@Composable
fun HistoryScreen(
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel,
    modifier: Modifier = Modifier
) {
    val isWatchTab = uiState.historyTab == "Watch history"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JoyTubeColors.Background)
    ) {
        JoyTubeSecondaryTopBar(
            title = "History",
            onBackClick = { viewModel.navigateBack() },
            rightActionIcon1 = JoyTubeIconType.MoreHorizontal,
            onRightAction1Click = {
                viewModel.showOverlay(
                    ActiveOverlay.GenericConfirm(
                        title = if (isWatchTab) "Clear all watch history?" else "Clear all browser history?",
                        message = "This will permanently remove all recorded activity in this tab.",
                        confirmLabel = "Clear history",
                        onConfirmAction = if (isWatchTab) ConfirmActionType.ClearWatchHistory else ConfirmActionType.ClearBrowserHistory
                    )
                )
            }
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            JoyTubeSegmentedRow(
                options = listOf("Watch history", "Browser history"),
                selectedOption = uiState.historyTab,
                onSelect = { viewModel.setHistoryTab(it) }
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("history_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isWatchTab) {
                if (uiState.watchHistory.isEmpty()) {
                    item {
                        JoyTubeEmptyStateCard(
                            icon = JoyTubeIconType.History,
                            title = "No watch history",
                            subtitle = "Videos and Shorts you watch appear here.",
                            actionLabel = "Explore videos",
                            onActionClick = { viewModel.selectPrimaryTab(PrimaryTab.Home) }
                        )
                    }
                } else {
                    HistoryBucket.entries.forEach { bucket ->
                        val itemsForBucket = uiState.watchHistory.filter { it.bucket == bucket }
                        if (itemsForBucket.isNotEmpty()) {
                            item {
                                Text(
                                    text = bucket.label,
                                    style = JoyTubeTypeScale.CardTitle
                                )
                            }
                            items(itemsForBucket, key = { it.id }) { entry ->
                                val media = uiState.mediaList.find { it.id == entry.mediaId }
                                    ?: MediaItem(
                                        id = entry.mediaId,
                                        title = entry.title,
                                        importerUsername = uiState.username,
                                        isOwnedByCurrentUser = true,
                                        type = MediaType.Video,
                                        visibility = ContentVisibility.Public,
                                        durationLabel = "03:00",
                                        durationSeconds = 180,
                                        categoryTag = "All",
                                        artwork = entry.artwork
                                    )
                                CompactMediaRowItem(
                                    media = media.copy(title = entry.title, artwork = entry.artwork),
                                    onClick = { viewModel.navigateTo(JoyTubeRoute.VideoPlayer(media.id)) },
                                    subtitleOverride = entry.subtitle,
                                    onTrailingClick = { viewModel.removeWatchHistoryItem(entry.id) }
                                )
                            }
                        }
                    }
                }
            } else {
                if (uiState.browserHistory.isEmpty()) {
                    item {
                        JoyTubeEmptyStateCard(
                            icon = JoyTubeIconType.Globe,
                            title = "No browser history",
                            subtitle = "Websites you visit in the built-in browser appear here.",
                            actionLabel = "Open Browser",
                            onActionClick = { viewModel.navigateTo(JoyTubeRoute.BrowserHome) }
                        )
                    }
                } else {
                    HistoryBucket.entries.forEach { bucket ->
                        val itemsForBucket = uiState.browserHistory.filter { it.bucket == bucket }
                        if (itemsForBucket.isNotEmpty()) {
                            item {
                                Text(
                                    text = bucket.label,
                                    style = JoyTubeTypeScale.CardTitle
                                )
                            }
                            items(itemsForBucket, key = { it.id }) { bEntry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .joyTubeOutlinedSurface(
                                            shape = JoyTubeRadius.ContentCard,
                                            backgroundColor = JoyTubeColors.Surface,
                                            borderColor = JoyTubeColors.BorderStandard
                                        )
                                        .clickable { viewModel.openBrowserUrlOrSearch(bEntry.url) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(JoyTubeColors.Background, CircleShape)
                                            .border(1.dp, JoyTubeColors.BorderStandard, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        JoyTubeSvgIcon(type = JoyTubeIconType.Globe, size = 18.dp)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bEntry.pageTitle,
                                            style = JoyTubeTypeScale.CardTitle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${bEntry.domain} · ${bEntry.visitTimeLabel}",
                                            style = JoyTubeTypeScale.Metadata
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable { viewModel.removeBrowserHistoryItem(bEntry.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        JoyTubeSvgIcon(
                                            type = JoyTubeIconType.Close,
                                            size = 15.dp,
                                            tint = JoyTubeColors.TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            JoyTubeSecondaryButton(
                text = if (isWatchTab) "Clear watch history" else "Clear browser history",
                onClick = {
                    viewModel.showOverlay(
                        ActiveOverlay.GenericConfirm(
                            title = if (isWatchTab) "Clear all watch history?" else "Clear all browser history?",
                            message = "This action cannot be undone.",
                            confirmLabel = "Clear all",
                            onConfirmAction = if (isWatchTab) ConfirmActionType.ClearWatchHistory else ConfirmActionType.ClearBrowserHistory
                        )
                    )
                }
            )
        }
    }
}
