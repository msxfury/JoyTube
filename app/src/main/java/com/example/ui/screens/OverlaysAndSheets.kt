package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * GLOBAL OVERLAY, POP-UP, DIALOG & BOTTOM SHEET HOST (PART XI — SECTION 40 & SCREENSHOTS 1, 4, 8, 9, 11, 17)
 */
@Composable
fun JoyTubeOverlayHost(
    uiState: JoyTubeUiState,
    viewModel: JoyTubeViewModel
) {
    val overlay = uiState.activeOverlay ?: return
    val isSwitchAccount = overlay is ActiveOverlay.SwitchAccountModal

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isSwitchAccount) Color(0x99000000) else JoyTubeColors.ScrimOverlay)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { viewModel.showOverlay(null) }
            .padding(
                horizontal = 18.dp,
                vertical = if (isSwitchAccount) 44.dp else 24.dp
            )
            .navigationBarsPadding()
            .statusBarsPadding(),
        contentAlignment = if (isSwitchAccount) Alignment.TopCenter else Alignment.BottomCenter
    ) {
        val modalShape = if (isSwitchAccount) RoundedCornerShape(28.dp) else JoyTubeRadius.ModalDialog
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .then(
                    if (isSwitchAccount) {
                        Modifier
                            .background(Color(0xF00B0C0F), modalShape)
                            .drawWithCache {
                                val topGlow = Brush.verticalGradient(
                                    colors = listOf(Color(0x24FFFFFF), Color.Transparent),
                                    startY = 0f,
                                    endY = size.height * 0.32f
                                )
                                val botGlow = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0x18FFFFFF)),
                                    startY = size.height * 0.72f,
                                    endY = size.height
                                )
                                onDrawWithContent {
                                    drawRect(brush = topGlow)
                                    drawRect(brush = botGlow)
                                    drawContent()
                                }
                            }
                            .border(
                                width = 1.15.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0x58FFFFFF),
                                        Color(0x20FFFFFF),
                                        Color(0x3EFFFFFF)
                                    )
                                ),
                                shape = modalShape
                            )
                    } else {
                        Modifier.joyTubeOutlinedSurface(
                            shape = modalShape,
                            backgroundColor = JoyTubeColors.SurfaceModal,
                            borderColor = JoyTubeColors.BorderGlass,
                            borderWidth = 1.dp,
                            topHighlightAlpha = 0.14f
                        )
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume clicks inside modal */ }
                .padding(horizontal = if (isSwitchAccount) 24.dp else 20.dp, vertical = if (isSwitchAccount) 26.dp else 20.dp)
                .testTag("active_modal_overlay"),
            verticalArrangement = Arrangement.spacedBy(if (isSwitchAccount) 16.dp else 14.dp)
        ) {
            when (overlay) {
                is ActiveOverlay.VideoOptions -> {
                    val media = overlay.media
                    ModalTitleRow(
                        title = "Video options",
                        onClose = { viewModel.showOverlay(null) }
                    )
                    ModalOptionRow(
                        icon = if (media.isFavourite) JoyTubeIconType.HeartFilled else JoyTubeIconType.HeartOutline,
                        label = if (media.isFavourite) "Remove from favourites" else "Add to favourites"
                    ) {
                        viewModel.toggleMediaFavourite(media.id)
                        viewModel.showOverlay(null)
                    }
                    ModalOptionRow(
                        icon = JoyTubeIconType.FolderPlus,
                        label = "Add to category"
                    ) {
                        viewModel.showOverlay(ActiveOverlay.AddToCategory(listOf(media.id)))
                    }
                    ModalOptionRow(
                        icon = JoyTubeIconType.Download,
                        label = "Download"
                    ) {
                        viewModel.showOverlay(ActiveOverlay.DownloadQuality(media))
                    }
                    ModalOptionRow(
                        icon = JoyTubeIconType.Play,
                        label = "Play next"
                    ) {
                        viewModel.showOverlay(null)
                        viewModel.showFeedback("Playing \"${media.title}\" next")
                    }
                    ModalOptionRow(
                        icon = JoyTubeIconType.PlusSquare,
                        label = "Add to queue"
                    ) {
                        viewModel.showOverlay(null)
                        viewModel.showFeedback("Added \"${media.title}\" to queue")
                    }
                    if (media.isOwnedByCurrentUser) {
                        ModalOptionRow(
                            icon = JoyTubeIconType.Trash,
                            label = "Delete imported media"
                        ) {
                            viewModel.showOverlay(ActiveOverlay.DeleteSelectedMediaConfirm(setOf(media.id)))
                        }
                    }
                    Text(
                        text = "Delete appears only for content the user imported.",
                        style = JoyTubeTypeScale.Microcopy
                    )
                }

                is ActiveOverlay.CategoryOptions -> {
                    val cat = overlay.category
                    ModalTitleRow(
                        title = "Category options",
                        onClose = { viewModel.showOverlay(null) }
                    )
                    if (cat.isOwnedByCurrentUser) {
                        ModalOptionRow(
                            icon = JoyTubeIconType.EditPen,
                            label = "Rename category"
                        ) {
                            viewModel.showOverlay(ActiveOverlay.RenameCategoryModal(cat))
                        }
                        ModalOptionRow(
                            icon = if (cat.visibility == ContentVisibility.Public) JoyTubeIconType.Globe else JoyTubeIconType.Lock,
                            label = "Change visibility (${cat.visibility.label})"
                        ) {
                            viewModel.showOverlay(ActiveOverlay.ChangeVisibilityModal(cat))
                        }
                        ModalOptionRow(
                            icon = JoyTubeIconType.FolderPlus,
                            label = "Add media"
                        ) {
                            viewModel.showOverlay(null)
                            viewModel.navigateTo(JoyTubeRoute.ImportMedia)
                        }
                    }
                    ModalOptionRow(
                        icon = if (cat.isFavourite) JoyTubeIconType.HeartFilled else JoyTubeIconType.HeartOutline,
                        label = if (cat.isFavourite) "Remove from favourites" else "Add to favourites"
                    ) {
                        viewModel.toggleCategoryFavourite(cat.id)
                        viewModel.showOverlay(null)
                    }
                    if (cat.isOwnedByCurrentUser) {
                        ModalOptionRow(
                            icon = JoyTubeIconType.Trash,
                            label = "Delete category"
                        ) {
                            viewModel.showOverlay(ActiveOverlay.DeleteCategoryConfirm(cat))
                        }
                    }
                }

                is ActiveOverlay.AddToCategory -> {
                    var filterText by remember { mutableStateOf("") }
                    var chosenCatIds by remember {
                        mutableStateOf(setOf(uiState.categories.firstOrNull()?.id ?: ""))
                    }
                    val matchingCats = uiState.categories.filter {
                        filterText.isBlank() || it.name.contains(filterText, ignoreCase = true)
                    }

                    ModalTitleRow(
                        title = "Add to category",
                        onClose = { viewModel.showOverlay(null) }
                    )
                    JoyTubeTextInput(
                        value = filterText,
                        onValueChange = { filterText = it },
                        placeholder = "Find a category",
                        leadingIcon = JoyTubeIconType.NavSearch
                    )
                    matchingCats.forEach { cat ->
                        val isChosen = cat.id in chosenCatIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(JoyTubeRadius.SmallControl)
                                .clickable {
                                    chosenCatIds = if (isChosen) chosenCatIds - cat.id else chosenCatIds + cat.id
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
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
                            RadioCircleDot(selected = isChosen)
                        }
                    }
                    JoyTubePrimaryButton(
                        text = "Add selected",
                        enabled = chosenCatIds.isNotEmpty(),
                        onClick = { viewModel.addMediaToCategories(overlay.mediaIds, chosenCatIds) }
                    )
                }

                is ActiveOverlay.DownloadQuality -> {
                    val qualities = listOf(
                        "Original quality" to "Best available",
                        "1080p" to "1920 × 1080",
                        "720p" to "1280 × 720",
                        "480p" to "854 × 480"
                    )
                    var selectedQuality by remember { mutableStateOf("1080p") }

                    ModalTitleRow(
                        title = "Choose download quality",
                        onClose = { viewModel.showOverlay(null) }
                    )
                    qualities.forEach { (qTitle, qSub) ->
                        val isSel = selectedQuality == qTitle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(JoyTubeRadius.SmallControl)
                                .clickable { selectedQuality = qTitle }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = qTitle, style = JoyTubeTypeScale.CardTitle)
                                Text(text = qSub, style = JoyTubeTypeScale.Metadata)
                            }
                            RadioCircleDot(selected = isSel)
                        }
                    }
                    JoyTubePrimaryButton(
                        text = "Start download",
                        onClick = { viewModel.startDownloadWithQuality(overlay.media, selectedQuality) }
                    )
                    Text(
                        text = "Only qualities offered by the source are shown.",
                        style = JoyTubeTypeScale.Microcopy
                    )
                }

                is ActiveOverlay.PlaybackSettings -> {
                    val speeds = listOf("0.25×", "0.5×", "0.75×", "1.0×", "1.25×", "1.5×", "1.75×", "2.0×")
                    val qualities = listOf("Auto", "Original", "1080p", "720p", "480p")
                    val orientations = listOf("Auto", "Portrait", "Landscape")
                    val subs = listOf("Off", "English")

                    ModalTitleRow(
                        title = "Playback settings",
                        onClose = { viewModel.showOverlay(null) }
                    )

                    PlaybackSettingCycleRow(
                        label = "Playback speed",
                        value = uiState.playbackSpeed,
                        onClick = {
                            val next = speeds[(speeds.indexOf(uiState.playbackSpeed) + 1) % speeds.size]
                            viewModel.updatePlaybackSpeed(next)
                        }
                    )
                    PlaybackSettingCycleRow(
                        label = "Video quality",
                        value = uiState.videoQuality,
                        onClick = {
                            val next = qualities[(qualities.indexOf(uiState.videoQuality) + 1) % qualities.size]
                            viewModel.updateVideoQuality(next)
                        }
                    )
                    PlaybackSettingCycleRow(
                        label = "Screen orientation",
                        value = uiState.screenOrientation,
                        onClick = {
                            val next = orientations[(orientations.indexOf(uiState.screenOrientation) + 1) % orientations.size]
                            viewModel.updateScreenOrientation(next)
                        }
                    )
                    PlaybackSettingCycleRow(
                        label = "Subtitles",
                        value = uiState.subtitlesSetting,
                        onClick = {
                            val next = subs[(subs.indexOf(uiState.subtitlesSetting) + 1) % subs.size]
                            viewModel.updateSubtitles(next)
                        }
                    )
                    Text(
                        text = "Speed options: 0.25×, 0.5×, 0.75×, 1×, 1.25×, 1.5×, 1.75× and 2×. Quality options appear only when supported by the source.",
                        style = JoyTubeTypeScale.Microcopy
                    )
                }

                is ActiveOverlay.CreateCategoryModal -> {
                    var categoryName by remember { mutableStateOf("") }
                    var visibility by remember { mutableStateOf(ContentVisibility.Public) }
                    var nameError by remember { mutableStateOf<String?>(null) }

                    ModalTitleRow(
                        title = "Create category",
                        onClose = { viewModel.showOverlay(null) }
                    )
                    Text(
                        text = "CATEGORY NAME",
                        style = JoyTubeTypeScale.Microcopy
                    )
                    JoyTubeTextInput(
                        value = categoryName,
                        onValueChange = { categoryName = it; nameError = null },
                        placeholder = "Enter category name",
                        errorMessage = nameError,
                        modifier = Modifier.testTag("create_category_name_input")
                    )

                    Text(
                        text = "Visibility",
                        style = JoyTubeTypeScale.CardTitle
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VisibilityChoiceCard(
                            icon = JoyTubeIconType.Globe,
                            title = "Public",
                            subtitle = "Discoverable by others",
                            isSelected = visibility == ContentVisibility.Public,
                            onClick = { visibility = ContentVisibility.Public },
                            modifier = Modifier.weight(1f)
                        )
                        VisibilityChoiceCard(
                            icon = JoyTubeIconType.Lock,
                            title = "Private",
                            subtitle = "Only you can see it",
                            isSelected = visibility == ContentVisibility.Private,
                            onClick = { visibility = ContentVisibility.Private },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    JoyTubePrimaryButton(
                        text = "Create category",
                        onClick = {
                            if (categoryName.isBlank()) {
                                nameError = "Please enter a category name."
                            } else {
                                viewModel.createCategory(categoryName, visibility)
                            }
                        }
                    )
                    JoyTubeSecondaryButton(
                        text = "Cancel",
                        onClick = { viewModel.showOverlay(null) }
                    )
                }

                is ActiveOverlay.EditCategoryModal -> {
                    val cat = overlay.category
                    ModalTitleRow(
                        title = "Edit category",
                        onClose = { viewModel.showOverlay(null) }
                    )
                    ModalOptionRow(icon = JoyTubeIconType.EditPen, label = "Rename") {
                        viewModel.showOverlay(ActiveOverlay.RenameCategoryModal(cat))
                    }
                    ModalOptionRow(icon = JoyTubeIconType.Globe, label = "Change visibility") {
                        viewModel.showOverlay(ActiveOverlay.ChangeVisibilityModal(cat))
                    }
                    ModalOptionRow(icon = JoyTubeIconType.Trash, label = "Delete category") {
                        viewModel.showOverlay(ActiveOverlay.DeleteCategoryConfirm(cat))
                    }
                    Text(
                        text = "Cancel",
                        style = JoyTubeTypeScale.Metadata,
                        modifier = Modifier
                            .clickable { viewModel.showOverlay(null) }
                            .padding(vertical = 4.dp)
                    )
                }

                is ActiveOverlay.RenameCategoryModal -> {
                    val cat = overlay.category
                    var updatedName by remember(cat.name) { mutableStateOf(cat.name) }
                    ModalTitleRow(
                        title = "Rename category",
                        onClose = { viewModel.showOverlay(null) }
                    )
                    JoyTubeTextInput(
                        value = updatedName,
                        onValueChange = { updatedName = it },
                        placeholder = "Enter category name"
                    )
                    JoyTubePrimaryButton(
                        text = "Save name",
                        onClick = { viewModel.renameCategory(cat.id, updatedName) }
                    )
                    JoyTubeSecondaryButton(
                        text = "Cancel",
                        onClick = { viewModel.showOverlay(null) }
                    )
                }

                is ActiveOverlay.ChangeVisibilityModal -> {
                    val cat = overlay.category
                    val targetVis = if (cat.visibility == ContentVisibility.Public) "Private" else "Public"
                    ModalTitleRow(
                        title = "Make this category ${targetVis.lowercase()}?",
                        onClose = { viewModel.showOverlay(null) }
                    )
                    Text(
                        text = if (targetVis == "Public") {
                            "Public categories can be discovered in Search and favourited by other JoyTube users."
                        } else {
                            "Private categories are visible only to you and never appear in public search."
                        },
                        style = JoyTubeTypeScale.SupportingText
                    )
                    JoyTubePrimaryButton(
                        text = "Change to $targetVis",
                        onClick = { viewModel.toggleCategoryVisibility(cat.id) }
                    )
                    JoyTubeSecondaryButton(
                        text = "Cancel",
                        onClick = { viewModel.showOverlay(null) }
                    )
                }

                is ActiveOverlay.DeleteCategoryConfirm -> {
                    val cat = overlay.category
                    JoyTubeSvgIcon(type = JoyTubeIconType.Trash, size = 20.dp)
                    Text(
                        text = "Delete category?",
                        style = JoyTubeTypeScale.SectionHeading
                    )
                    Text(
                        text = "This also deletes every video and Short inside this category.",
                        style = JoyTubeTypeScale.SupportingText
                    )
                    JoyTubePrimaryButton(
                        text = "Delete everything",
                        onClick = { viewModel.deleteCategoryAndContents(cat.id) }
                    )
                    JoyTubeSecondaryButton(
                        text = "Cancel",
                        onClick = { viewModel.showOverlay(null) }
                    )
                }

                is ActiveOverlay.DeleteSelectedCategoriesConfirm -> {
                    val count = overlay.categoryIds.size
                    JoyTubeSvgIcon(type = JoyTubeIconType.Trash, size = 20.dp)
                    Text(
                        text = "Delete $count selected categories?",
                        style = JoyTubeTypeScale.SectionHeading
                    )
                    Text(
                        text = "This also deletes every video and Short inside the selected categories.",
                        style = JoyTubeTypeScale.SupportingText
                    )
                    JoyTubePrimaryButton(
                        text = "Delete selected categories",
                        onClick = { viewModel.deleteSelectedCategories(overlay.categoryIds) }
                    )
                    JoyTubeSecondaryButton(
                        text = "Cancel",
                        onClick = { viewModel.showOverlay(null) }
                    )
                }

                is ActiveOverlay.DeleteSelectedMediaConfirm -> {
                    val count = overlay.mediaIds.size
                    JoyTubeSvgIcon(type = JoyTubeIconType.Trash, size = 20.dp)
                    Text(
                        text = "Delete $count selected ${if (count == 1) "item" else "items"}?",
                        style = JoyTubeTypeScale.SectionHeading
                    )
                    Text(
                        text = "These imported videos or Shorts will be removed from JoyTube. This cannot be undone.",
                        style = JoyTubeTypeScale.SupportingText
                    )
                    JoyTubePrimaryButton(
                        text = "Delete selected",
                        onClick = { viewModel.bulkDeleteOwnedMedia(overlay.mediaIds) }
                    )
                    JoyTubeSecondaryButton(
                        text = "Cancel",
                        onClick = { viewModel.showOverlay(null) }
                    )
                }

                is ActiveOverlay.BrowserMenu -> {
                    ModalTitleRow(
                        title = "Browser menu",
                        onClose = { viewModel.showOverlay(null) }
                    )
                    ModalOptionRow(icon = JoyTubeIconType.Plus, label = "New tab") {
                        viewModel.createNewBrowserTab(false)
                    }
                    ModalOptionRow(icon = JoyTubeIconType.TabsStack, label = "All tabs") {
                        viewModel.showOverlay(null)
                        viewModel.navigateTo(JoyTubeRoute.BrowserTabs)
                    }
                    ModalOptionRow(icon = JoyTubeIconType.Download, label = "Downloads") {
                        viewModel.showOverlay(null)
                        viewModel.navigateTo(JoyTubeRoute.Downloads)
                    }
                    ModalOptionRow(icon = JoyTubeIconType.History, label = "History") {
                        viewModel.showOverlay(null)
                        viewModel.setHistoryTab("Browser history")
                        viewModel.navigateTo(JoyTubeRoute.History)
                    }
                    ModalOptionRow(icon = JoyTubeIconType.ShieldCheck, label = "Ad blocking") {
                        viewModel.showOverlay(null)
                        viewModel.navigateTo(JoyTubeRoute.AdBlockerSettings)
                    }
                    ModalOptionRow(icon = JoyTubeIconType.Download, label = "Import media") {
                        viewModel.showOverlay(null)
                        viewModel.navigateTo(JoyTubeRoute.ImportMedia)
                    }
                    ModalOptionRow(icon = JoyTubeIconType.Settings, label = "Browser settings") {
                        viewModel.showOverlay(null)
                        viewModel.navigateTo(JoyTubeRoute.Settings)
                    }
                }

                is ActiveOverlay.SwitchAccountModal -> {
                    // Strictly matches Image 1 ("Switch account" popup when top-right account is clicked)
                    Text(
                        text = "Switch account",
                        style = JoyTubeTypeScale.PageTitle.copy(
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )

                    Text(
                        text = "@${uiState.username}",
                        style = JoyTubeTypeScale.BodyText.copy(
                            fontSize = 15.sp,
                            color = Color(0xFF8C8E98)
                        )
                    )

                    // Active Account Row (Avatar + Density / @density_goat + Checkmark)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(JoyTubeRadius.InputField)
                            .clickable {
                                viewModel.showOverlay(null)
                                viewModel.navigateTo(JoyTubeRoute.Profile)
                            }
                            .padding(vertical = 6.dp)
                            .testTag("switch_account_active_row"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        JoyTubeAccountAvatarBadge(
                            initial = uiState.displayName.take(1).ifEmpty { "D" },
                            size = 48.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.displayName,
                                style = JoyTubeTypeScale.CardTitle.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "@${uiState.username}",
                                style = JoyTubeTypeScale.Metadata.copy(
                                    fontSize = 14.sp,
                                    color = Color(0xFF8C8E98)
                                )
                            )
                        }
                        JoyTubeSvgIcon(
                            type = JoyTubeIconType.Check,
                            size = 20.dp,
                            tint = Color.White
                        )
                    }

                    // Action 1: + Add account
                    SwitchAccountActionRow(
                        icon = JoyTubeIconType.Plus,
                        iconSize = 22.dp,
                        label = "Add account",
                        tag = "switch_account_add"
                    ) {
                        viewModel.showOverlay(null)
                        viewModel.navigateTo(JoyTubeRoute.AuthFlow(AuthStep.SignIn))
                    }

                    // Action 2: ✕ Manage accounts
                    SwitchAccountActionRow(
                        icon = JoyTubeIconType.Close,
                        iconSize = 20.dp,
                        label = "Manage accounts",
                        tag = "switch_account_manage"
                    ) {
                        viewModel.showOverlay(null)
                        viewModel.navigateTo(JoyTubeRoute.Profile)
                    }

                    // Action 3: ⚙ Settings
                    SwitchAccountActionRow(
                        icon = JoyTubeIconType.Settings,
                        iconSize = 22.dp,
                        label = "Settings",
                        tag = "switch_account_settings"
                    ) {
                        viewModel.showOverlay(null)
                        viewModel.navigateTo(JoyTubeRoute.Settings)
                    }
                }

                is ActiveOverlay.GenericConfirm -> {
                    JoyTubeSvgIcon(type = JoyTubeIconType.Trash, size = 20.dp)
                    Text(
                        text = overlay.title,
                        style = JoyTubeTypeScale.SectionHeading
                    )
                    Text(
                        text = overlay.message,
                        style = JoyTubeTypeScale.SupportingText
                    )
                    JoyTubePrimaryButton(
                        text = overlay.confirmLabel,
                        onClick = { viewModel.executeConfirmAction(overlay.onConfirmAction) }
                    )
                    JoyTubeSecondaryButton(
                        text = "Cancel",
                        onClick = { viewModel.showOverlay(null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModalTitleRow(
    title: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = JoyTubeTypeScale.SectionHeading)
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            JoyTubeSvgIcon(type = JoyTubeIconType.Close, size = 16.dp)
        }
    }
}

@Composable
private fun ModalOptionRow(
    icon: JoyTubeIconType,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(JoyTubeRadius.SmallControl)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        JoyTubeSvgIcon(type = icon, size = 18.dp, tint = JoyTubeColors.TextSecondary)
        Text(text = label, style = JoyTubeTypeScale.BodyText)
    }
}

@Composable
private fun RadioCircleDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(
                if (selected) JoyTubeColors.TextPrimary else Color(0xFF686870),
                CircleShape
            )
            .border(
                width = 1.dp,
                color = if (selected) JoyTubeColors.TextPrimary else JoyTubeColors.BorderFocused,
                shape = CircleShape
            )
    )
}

@Composable
private fun PlaybackSettingCycleRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(JoyTubeRadius.SmallControl)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = JoyTubeTypeScale.BodyText)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = value, style = JoyTubeTypeScale.Metadata)
            JoyTubeSvgIcon(
                type = JoyTubeIconType.ChevronRight,
                size = 15.dp,
                tint = JoyTubeColors.TextSecondary
            )
        }
    }
}

@Composable
private fun VisibilityChoiceCard(
    icon: JoyTubeIconType,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) JoyTubeColors.SurfaceElevated else JoyTubeColors.Background
    val border = if (isSelected) Color(0x55FFFFFF) else JoyTubeColors.BorderStandard
    Column(
        modifier = modifier
            .clip(JoyTubeRadius.InputField)
            .background(bg, JoyTubeRadius.InputField)
            .border(1.dp, border, JoyTubeRadius.InputField)
            .clickable { onClick() }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        JoyTubeSvgIcon(type = icon, size = 17.dp, tint = JoyTubeColors.TextSecondary)
        Text(text = title, style = JoyTubeTypeScale.CardTitle)
        Text(text = subtitle, style = JoyTubeTypeScale.Microcopy)
    }
}

@Composable
private fun SwitchAccountActionRow(
    icon: JoyTubeIconType,
    iconSize: androidx.compose.ui.unit.Dp,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(JoyTubeRadius.InputField)
            .clickable { onClick() }
            .padding(vertical = 8.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.Center
        ) {
            JoyTubeSvgIcon(
                type = icon,
                size = iconSize,
                tint = Color.White,
                strokeWidthDp = 2.dp
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = JoyTubeTypeScale.BodyText.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White
            )
        )
    }
}

