package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.CategoryItem
import com.example.model.MediaItem
import com.example.model.PrimaryTab
import com.example.ui.icons.JoyTubeIconType
import com.example.ui.icons.JoyTubeSvgIcon
import com.example.ui.theme.JoyTubeColors
import com.example.ui.theme.JoyTubeRadius
import com.example.ui.theme.JoyTubeTypeScale
import com.example.ui.theme.joyTubeOutlinedSurface
import kotlin.math.roundToInt

import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.LocalGlassDesignEnabled
import kotlin.math.abs

private val CompactThumbShape = RoundedCornerShape(10.dp)
private val SkeletonBarShape = RoundedCornerShape(4.dp)

/**
 * Reusable zero-recomposition tactile spring scale modifier for glass controls & icons.
 */
@Composable
private fun Modifier.joyTubeTouchBounce(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.92f,
    selectedScale: Float = 1.0f
): Modifier {
    val isGlass = LocalGlassDesignEnabled.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val targetScale = when {
        isPressed -> if (isGlass) pressedScale else 0.97f
        else -> if (isGlass) selectedScale else 1.0f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = if (isGlass) Spring.DampingRatioMediumBouncy else Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "GlassTouchScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Stylized Dark Monogram Account Avatar with Chalk/Metallic Ring & Outlining (Matches Image 1 & 2)
 */
@Composable
fun JoyTubeAccountAvatarBadge(
    initial: String = "D",
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .drawWithCache {
                val r = this.size.width * 0.5f
                val bgBrush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF23252C),
                        Color(0xFF0E0F13),
                        Color(0xFF050507)
                    ),
                    center = Offset(r * 0.85f, r * 0.85f),
                    radius = r * 1.15f
                )
                val ringBrush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0x66FFFFFF),
                        Color(0x18FFFFFF),
                        Color(0x52FFFFFF),
                        Color(0x14FFFFFF),
                        Color(0x66FFFFFF)
                    )
                )
                val innerStroke = 1.6.dp.toPx()
                onDrawWithContent {
                    drawCircle(brush = bgBrush)
                    drawCircle(
                        brush = ringBrush,
                        radius = r - innerStroke * 1.4f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = innerStroke)
                    )
                    drawContent()
                }
            }
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x90FFFFFF),
                        Color(0x30FFFFFF),
                        Color(0x66FFFFFF)
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.take(1).uppercase(),
            style = JoyTubeTypeScale.SectionHeading.copy(
                color = Color(0xFFEAECEF)
            )
        )
    }
}

/**
 * 1. ESTABLISHED JOYTUBE BRAND HEADER (PART I — SECTION 03 + TOP-RIGHT ACCOUNT TRIGGER)
 */
@Composable
fun JoyTubeBrandHeader(
    onSearchClick: () -> Unit,
    onAccountClick: (() -> Unit)? = null,
    accountInitial: String = "D",
    modifier: Modifier = Modifier
) {
    val searchInteraction = remember { MutableInteractionSource() }
    val accountInteraction = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.testTag("brand_logo_header")
        ) {
            JoyTubeSvgIcon(
                type = JoyTubeIconType.BrandLogoLeftTriangle,
                size = 18.dp,
                tint = JoyTubeColors.TextPrimary
            )
            Text(
                text = "JoyTube",
                style = JoyTubeTypeScale.BrandWordmark
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Outlined Top-Right Action Capsule (Search + Circular Account Avatar strictly matching Image 1 & 2)
            Row(
                modifier = Modifier
                    .height(46.dp)
                    .joyTubeOutlinedSurface(
                        shape = JoyTubeRadius.Capsule,
                        backgroundColor = Color(0xD90A0B0E),
                        borderColor = JoyTubeColors.BorderGlass,
                        borderWidth = 1.dp,
                        topHighlightAlpha = 0.22f
                    )
                    .padding(start = 6.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .joyTubeTouchBounce(searchInteraction, pressedScale = 0.86f)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = searchInteraction,
                            indication = null
                        ) { onSearchClick() }
                        .testTag("header_search_button"),
                    contentAlignment = Alignment.Center
                ) {
                    JoyTubeSvgIcon(
                        type = JoyTubeIconType.NavSearch,
                        size = 19.dp,
                        tint = JoyTubeColors.TextPrimary
                    )
                }

                if (onAccountClick != null) {
                    Box(
                        modifier = Modifier
                            .joyTubeTouchBounce(accountInteraction, pressedScale = 0.88f)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = accountInteraction,
                                indication = null
                            ) { onAccountClick() }
                            .testTag("header_account_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        JoyTubeAccountAvatarBadge(
                            initial = accountInitial,
                            size = 36.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2. SECONDARY SCREEN TOP BAR
 */
@Composable
fun JoyTubeSecondaryTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    centeredTitle: Boolean = false,
    rightActionIcon1: JoyTubeIconType? = null,
    onRightAction1Click: (() -> Unit)? = null,
    rightActionIcon2: JoyTubeIconType? = null,
    onRightAction2Click: (() -> Unit)? = null
) {
    val isGlass = LocalGlassDesignEnabled.current
    val backInteraction = remember { MutableInteractionSource() }
    val action1Interaction = remember { MutableInteractionSource() }
    val action2Interaction = remember { MutableInteractionSource() }

    val glassCircleModifier = if (isGlass) {
        Modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0x28FFFFFF), Color(0x12FFFFFF))
                ),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        JoyTubeColors.LiquidGlassBorderBright,
                        JoyTubeColors.LiquidGlassBorderSubtle
                    )
                ),
                shape = CircleShape
            )
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .joyTubeTouchBounce(backInteraction, pressedScale = 0.88f)
                    .then(glassCircleModifier)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = backInteraction,
                        indication = null
                    ) { onBackClick() }
                    .testTag("back_button"),
                contentAlignment = Alignment.Center
            ) {
                JoyTubeSvgIcon(
                    type = JoyTubeIconType.ArrowBack,
                    size = 20.dp,
                    tint = JoyTubeColors.TextPrimary
                )
            }
            if (!centeredTitle && title.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = JoyTubeTypeScale.PageTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (centeredTitle && title.isNotEmpty()) {
            Text(
                text = title,
                style = JoyTubeTypeScale.SectionHeading,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            if (rightActionIcon1 != null && onRightAction1Click != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .joyTubeTouchBounce(action1Interaction, pressedScale = 0.88f)
                        .then(glassCircleModifier)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = action1Interaction,
                            indication = null
                        ) { onRightAction1Click() }
                        .testTag("top_bar_action_1"),
                    contentAlignment = Alignment.Center
                ) {
                    JoyTubeSvgIcon(type = rightActionIcon1, size = 19.dp)
                }
            }
            if (rightActionIcon2 != null && onRightAction2Click != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .joyTubeTouchBounce(action2Interaction, pressedScale = 0.88f)
                        .then(glassCircleModifier)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = action2Interaction,
                            indication = null
                        ) { onRightAction2Click() }
                        .testTag("top_bar_action_2"),
                    contentAlignment = Alignment.Center
                ) {
                    JoyTubeSvgIcon(type = rightActionIcon2, size = 19.dp)
                }
            }
        }
    }
}

/**
 * Reusable Smoked Radial Glass Shell Modifier (matches Image 1 & Image 2 bottom nav glass physics)
 */
private fun Modifier.joyTubeSmokedRadialGlassShell(
    shape: androidx.compose.ui.graphics.Shape,
    hasCyanEdgeRefraction: Boolean = false
): Modifier = this
    .background(Color(0xDE08090C), shape)
    .drawWithCache {
        val topRimGlow = Brush.verticalGradient(
            colors = listOf(
                Color(0x45FFFFFF),
                Color(0x14FFFFFF),
                Color.Transparent
            ),
            startY = 0f,
            endY = size.height * 0.46f
        )
        val bottomRimGlow = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                if (hasCyanEdgeRefraction) Color(0x1A68C8FF) else Color(0x10FFFFFF),
                if (hasCyanEdgeRefraction) Color(0x4080D4FF) else Color(0x2EFFFFFF)
            ),
            startY = size.height * 0.56f,
            endY = size.height
        )
        val topHighlightLine = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x66FFFFFF),
                Color.Transparent
            )
        )
        val p1 = Offset(size.width * 0.14f, 1.dp.toPx())
        val p2 = Offset(size.width * 0.86f, 1.dp.toPx())
        val sw = 1.2.dp.toPx()
        onDrawWithContent {
            drawRect(brush = topRimGlow)
            drawRect(brush = bottomRimGlow)
            drawContent()
            drawLine(brush = topHighlightLine, start = p1, end = p2, strokeWidth = sw)
        }
    }
    .border(
        width = 1.15.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0x75FFFFFF),
                Color(0x28FFFFFF),
                if (hasCyanEdgeRefraction) Color(0x6678CCFF) else Color(0x4CFFFFFF)
            )
        ),
        shape = shape
    )

/**
 * 3. PREMIUM FLOATING NAVIGATION DOCK (IMAGE 1 & IMAGE 2 GLASS UI WITH JOYTUBE SVG ICONS)
 *
 * When Glass Design is enabled:
 * - Expanded State (Image 1):
 *   - Left Main Smoked-Glass Capsule containing JoyTube's `Home`, `Shorts`, and `Account` tabs
 *     (using our own JoyTube SVG icons + labels) with a sliding curved glass pillow + glowing
 *     top white specular bar (`━━`) on the selected tab.
 *   - Right Detached Circular Smoked-Glass `Search` Button (`64.dp`) with our own `NavSearch` SVG icon
 *     and crisp outlining.
 * - Scrolled-Down Compacted State (Image 2):
 *   - Smoothly morphs the left capsule into a `64.dp` Circular Glass Sphere on the bottom-left
 *     displaying the active JoyTube SVG icon while the `64.dp` Circular Glass Search Sphere floats
 *     on the bottom-right. Tapping or scrolling up smoothly expands the full capsule back out.
 */
@Composable
fun JoyTubeFloatingNavigationDock(
    selectedTab: PrimaryTab,
    onSelectTab: (PrimaryTab) -> Unit,
    isScrollCompacted: Boolean = false,
    playbackSnapshot: com.example.backend.playback.GlobalPlaybackSnapshot? = null,
    onMiniPlayerClick: (() -> Unit)? = null,
    onMiniPlayerPlayPause: (() -> Unit)? = null,
    onMiniPlayerNext: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isGlass = LocalGlassDesignEnabled.current
    val showMiniPlayer = playbackSnapshot != null && playbackSnapshot.isMiniBarVisible

    if (!isGlass) {
        // Classic Matte Outlined JoyTube Dock (when Glass Design is disabled in Settings)
        val tabs = PrimaryTab.entries
        val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0).toFloat()
        var innerWidthPx by remember { mutableIntStateOf(0) }
        val circleSize = 42.dp
        val circleSizePx = with(LocalDensity.current) { circleSize.toPx() }
        val animatedIndex by animateFloatAsState(
            targetValue = selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "MatteDockSlide"
        )

        Column(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showMiniPlayer && playbackSnapshot != null) {
                JoyTubeGlassMiniPlayerPill(
                    snapshot = playbackSnapshot,
                    isCompactBetweenOrbs = false,
                    onClick = { onMiniPlayerClick?.invoke() },
                    onPlayPause = { onMiniPlayerPlayPause?.invoke() },
                    onNext = { onMiniPlayerNext?.invoke() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .joyTubeOutlinedSurface(
                        shape = JoyTubeRadius.Capsule,
                        backgroundColor = JoyTubeColors.SurfaceFloatingDock,
                        borderColor = JoyTubeColors.BorderGlass,
                        borderWidth = 1.dp,
                        topHighlightAlpha = 0.16f
                    )
                    .padding(horizontal = 12.dp)
                    .onSizeChanged { innerWidthPx = it.width }
                    .testTag("floating_navigation_dock")
            ) {
                Box(
                    modifier = Modifier
                        .offset {
                            val slotW = innerWidthPx / tabs.size.toFloat()
                            val x = (slotW * animatedIndex + (slotW - circleSizePx) * 0.5f).roundToInt()
                            IntOffset(x, 0)
                        }
                        .align(Alignment.CenterStart)
                        .size(circleSize)
                        .background(JoyTubeColors.Selected, CircleShape)
                        .border(1.dp, Color(0x45FFFFFF), CircleShape)
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val iconType = when (tab) {
                            PrimaryTab.Home -> JoyTubeIconType.NavHome
                            PrimaryTab.Shorts -> JoyTubeIconType.NavShorts
                            PrimaryTab.Search -> JoyTubeIconType.NavSearch
                            PrimaryTab.Account -> JoyTubeIconType.NavAccount
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onSelectTab(tab) }
                                .testTag("nav_tab_${tab.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            JoyTubeSvgIcon(
                                type = iconType,
                                size = 20.dp,
                                tint = JoyTubeColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }
        return
    }

    // GLASS UI NAVIGATION (Continuous Sliding & Morphing between Image 1 Expanded Capsule and Image 2 Compacted Dual Glass Spheres + Mini-Player)
    val mainCapsuleTabs = remember { listOf(PrimaryTab.Home, PrimaryTab.Shorts, PrimaryTab.Account) }
    val activeCapsuleIndex = when (selectedTab) {
        PrimaryTab.Home -> 0f
        PrimaryTab.Shorts -> 1f
        PrimaryTab.Account -> 2f
        PrimaryTab.Search -> -1f
    }
    var lastNonSearchIndex by remember { mutableFloatStateOf(0f) }
    if (activeCapsuleIndex >= 0f) {
        lastNonSearchIndex = activeCapsuleIndex
    }

    // Continuous 0f (Expanded Capsule) <-> 1f (Compacted Left Sphere + Center Mini-Player) sliding progress
    val slideCompactProgress by animateFloatAsState(
        targetValue = if (isScrollCompacted) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.80f,
            stiffness = 230f
        ),
        label = "NavCapsuleSlideMorph"
    )

    val animatedPillIndex by animateFloatAsState(
        targetValue = if (activeCapsuleIndex >= 0f) activeCapsuleIndex else lastNonSearchIndex,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "GlassPillSlide"
    )
    val pillAlpha by animateFloatAsState(
        targetValue = if (activeCapsuleIndex >= 0f) 1f else 0f,
        animationSpec = tween(180),
        label = "GlassPillAlpha"
    )

    val density = LocalDensity.current
    var totalDockWidthPx by remember { mutableIntStateOf(0) }
    val orbSizeDp = 66.dp
    val orbSizePx = with(density) { orbSizeDp.toPx() }
    val gapPx = with(density) { 10.dp.toPx() }
    val padPx = with(density) { 10.dp.toPx() } // 5.dp horizontal padding * 2

    val expandedCapsuleWidthPx = if (totalDockWidthPx > 0) {
        (totalDockWidthPx - orbSizePx - gapPx).coerceAtLeast(orbSizePx)
    } else 0f

    val currentCapsuleWidthPx = if (expandedCapsuleWidthPx > 0f) {
        expandedCapsuleWidthPx + (orbSizePx - expandedCapsuleWidthPx) * slideCompactProgress
    } else 0f

    val currentCapsuleWidthDp = with(density) { currentCapsuleWidthPx.toDp() }
    val expandedInnerWidthPx = (expandedCapsuleWidthPx - padPx).coerceAtLeast(1f)
    val expandedInnerWidthDp = with(density) { expandedInnerWidthPx.toDp() }

    val searchInteraction = remember { MutableInteractionSource() }
    val compactOrbInteraction = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // EXPANDED POSITION: Mini-player sits directly above the full navigation bar (Section 17 & Image 1)
        if (showMiniPlayer && playbackSnapshot != null && slideCompactProgress < 0.85f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val openVis = (1f - slideCompactProgress * 1.3f).coerceIn(0f, 1f)
                        alpha = openVis
                        translationY = (1f - openVis) * 18.dp.toPx()
                        val sc = 0.94f + 0.06f * openVis
                        scaleX = sc
                        scaleY = sc
                    }
            ) {
                JoyTubeGlassMiniPlayerPill(
                    snapshot = playbackSnapshot,
                    isCompactBetweenOrbs = false,
                    onClick = { onMiniPlayerClick?.invoke() },
                    onPlayPause = { onMiniPlayerPlayPause?.invoke() },
                    onNext = { onMiniPlayerNext?.invoke() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mini_player_expanded_above_dock")
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(orbSizeDp)
                .onSizeChanged { totalDockWidthPx = it.width }
                .testTag("floating_navigation_dock"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // CONTINUOUSLY SLIDING & MORPHING LEFT GLASS CAPSULE <-> HOME SPHERE
            Box(
                modifier = Modifier
                    .then(
                        if (totalDockWidthPx > 0) {
                            Modifier.width(currentCapsuleWidthDp)
                        } else {
                            Modifier.weight(1f)
                        }
                    )
                    .fillMaxHeight()
                    .joyTubeTouchBounce(
                        interactionSource = compactOrbInteraction,
                        pressedScale = if (slideCompactProgress > 0.5f) 0.88f else 0.98f
                    )
                    .clip(JoyTubeRadius.Capsule)
                    .joyTubeSmokedRadialGlassShell(shape = JoyTubeRadius.Capsule, hasCyanEdgeRefraction = false)
                    .then(
                        if (slideCompactProgress > 0.6f) {
                            Modifier.clickable(
                                interactionSource = compactOrbInteraction,
                                indication = null
                            ) {
                                onSelectTab(PrimaryTab.Home)
                            }
                        } else Modifier
                    )
                    .padding(horizontal = 5.dp, vertical = 5.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // LAYER 1: Expanded 3-Tab Content (Slides & Fades Smoothly as Capsule Slides Open/Shut)
                if (slideCompactProgress < 0.99f && expandedCapsuleWidthPx > 0f) {
                    val slotWidthPx = expandedInnerWidthPx / mainCapsuleTabs.size.toFloat()
                    val slotWidthDp = with(density) { slotWidthPx.toDp() }

                    Box(
                        modifier = Modifier
                            .requiredWidth(expandedInnerWidthDp)
                            .fillMaxHeight()
                            .graphicsLayer {
                                alpha = (1f - slideCompactProgress * 1.35f).coerceIn(0f, 1f)
                                translationX = -slideCompactProgress * (expandedCapsuleWidthPx - orbSizePx) * 0.35f
                                val sc = 1f - slideCompactProgress * 0.10f
                                scaleX = sc
                                scaleY = sc
                            }
                    ) {
                        // Sliding Curved Inner Glass Pill with Top Glowing White Specular Bar (Strictly matching Image 1)
                        Box(
                            modifier = Modifier
                                .offset {
                                    val x = (slotWidthPx * animatedPillIndex).roundToInt()
                                    IntOffset(x, 0)
                                }
                                .width(slotWidthDp)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    alpha = pillAlpha * (1f - slideCompactProgress * 1.4f).coerceIn(0f, 1f)
                                    val delta = abs((if (activeCapsuleIndex >= 0f) activeCapsuleIndex else lastNonSearchIndex) - animatedPillIndex)
                                    scaleX = 1f + (delta * 0.14f).coerceAtMost(0.18f)
                                    scaleY = 1f - (delta * 0.06f).coerceAtMost(0.08f)
                                }
                                .clip(JoyTubeRadius.Capsule)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x54FFFFFF),
                                            Color(0x1AFFFFFF),
                                            Color(0x30FFFFFF)
                                        )
                                    ),
                                    shape = JoyTubeRadius.Capsule
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x88FFFFFF),
                                            Color(0x24FFFFFF),
                                            Color(0x48FFFFFF)
                                        )
                                    ),
                                    shape = JoyTubeRadius.Capsule
                                )
                        ) {
                            // Signature Top Center Specular White Light Bar (`━━`) from Image 1
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 1.5.dp)
                                    .width(24.dp)
                                    .height(2.4.dp)
                                    .background(Color(0xF2FFFFFF), JoyTubeRadius.Capsule)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            mainCapsuleTabs.forEach { tab ->
                                val isSelected = tab == selectedTab
                                val tabInteraction = remember { MutableInteractionSource() }
                                val iconType = when (tab) {
                                    PrimaryTab.Home -> JoyTubeIconType.NavHome
                                    PrimaryTab.Shorts -> JoyTubeIconType.NavShorts
                                    PrimaryTab.Account -> JoyTubeIconType.NavAccount
                                    PrimaryTab.Search -> JoyTubeIconType.NavSearch
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .joyTubeTouchBounce(
                                            interactionSource = tabInteraction,
                                            pressedScale = 0.86f,
                                            selectedScale = if (isSelected) 1.04f else 1.0f
                                        )
                                        .clickable(
                                            interactionSource = tabInteraction,
                                            indication = null,
                                            enabled = slideCompactProgress < 0.6f
                                        ) { onSelectTab(tab) }
                                        .testTag("nav_tab_${tab.name.lowercase()}"),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    JoyTubeSvgIcon(
                                        type = iconType,
                                        size = 20.dp,
                                        tint = if (isSelected) Color.White else Color(0xFFA2A4AE)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = tab.label,
                                        style = JoyTubeTypeScale.Microcopy.copy(
                                            color = if (isSelected) Color.White else Color(0xFF9496A0)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // LAYER 2: Compacted Home Sphere Icon (Section 17 & Image 2: When scrolled down, Home and Search remain visible with Mini-Player between them)
                if (slideCompactProgress > 0.05f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val progress = ((slideCompactProgress - 0.25f) / 0.75f).coerceIn(0f, 1f)
                                alpha = progress
                                val sc = 0.68f + 0.32f * progress
                                scaleX = sc
                                scaleY = sc
                                translationX = (1f - progress) * 18.dp.toPx()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        JoyTubeSvgIcon(
                            type = JoyTubeIconType.NavHome,
                            size = 24.dp,
                            tint = Color.White
                        )
                    }
                }
            }

            // COMPACTED CENTER POSITION: Mini-player appears between Home sphere and Search sphere when scrolled down (Section 17 & Image 2)
            if (showMiniPlayer && playbackSnapshot != null && slideCompactProgress > 0.30f) {
                val centerProgress = ((slideCompactProgress - 0.30f) / 0.70f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .graphicsLayer {
                            alpha = centerProgress
                            val sc = 0.85f + 0.15f * centerProgress
                            scaleX = sc
                            scaleY = sc
                        }
                ) {
                    JoyTubeGlassMiniPlayerPill(
                        snapshot = playbackSnapshot,
                        isCompactBetweenOrbs = true,
                        onClick = { onMiniPlayerClick?.invoke() },
                        onPlayPause = { onMiniPlayerPlayPause?.invoke() },
                        onNext = { onMiniPlayerNext?.invoke() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mini_player_compacted_between_orbs")
                    )
                }
            }

            // RIGHT DETACHED CIRCULAR GLASS SEARCH SPHERE (Matches Image 1 & Image 2 with our NavSearch SVG icon)
            val isSearchSelected = selectedTab == PrimaryTab.Search
            Box(
                modifier = Modifier
                    .size(orbSizeDp)
                    .joyTubeTouchBounce(
                        interactionSource = searchInteraction,
                        pressedScale = 0.85f,
                        selectedScale = if (isSearchSelected) 1.05f else 1.0f
                    )
                    .clip(CircleShape)
                    .joyTubeSmokedRadialGlassShell(
                        shape = CircleShape,
                        hasCyanEdgeRefraction = true
                    )
                    .then(
                        if (isSearchSelected) {
                            Modifier.background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0x45FFFFFF), Color(0x14FFFFFF), Color(0x28FFFFFF))
                                ),
                                shape = CircleShape
                            )
                        } else Modifier
                    )
                    .clickable(
                        interactionSource = searchInteraction,
                        indication = null
                    ) { onSelectTab(PrimaryTab.Search) }
                    .testTag("nav_tab_search"),
                contentAlignment = Alignment.Center
            ) {
                if (isSearchSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 3.dp)
                            .width(18.dp)
                            .height(2.2.dp)
                            .background(Color(0xF2FFFFFF), JoyTubeRadius.Capsule)
                    )
                }
                JoyTubeSvgIcon(
                    type = JoyTubeIconType.NavSearch,
                    size = 22.dp,
                    tint = if (isSearchSelected) Color.White else Color(0xFFD8DADF)
                )
            }
        }
    }
}

/**
 * Synchronized Smoked-Glass Mini-Player Bar (Section 17 & BitChord Screenshots 1 & 2)
 * - In Expanded mode (`isCompactBetweenOrbs == false`): sits directly above the full navigation dock
 *   displaying `[Artwork] [Title / Artist] [Play/Pause] [Skip Next]`.
 * - In Compacted mode (`isCompactBetweenOrbs == true`): sits between the Home sphere and Search sphere
 *   displaying `[Artwork] [Title / Artist] [Play/Pause]` with cyan-white glass edge refraction.
 */
@Composable
fun JoyTubeGlassMiniPlayerPill(
    snapshot: com.example.backend.playback.GlobalPlaybackSnapshot,
    isCompactBetweenOrbs: Boolean,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying = snapshot.lifecycleState == com.example.backend.playback.GlobalPlaybackLifecycleState.PLAYING
    Row(
        modifier = modifier
            .height(if (isCompactBetweenOrbs) 60.dp else 58.dp)
            .clip(JoyTubeRadius.Capsule)
            .joyTubeSmokedRadialGlassShell(
                shape = JoyTubeRadius.Capsule,
                hasCyanEdgeRefraction = isCompactBetweenOrbs
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        JoyTubeMediaThumbnail(
            preset = snapshot.artworkPreset,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = snapshot.activeTitle,
                style = JoyTubeTypeScale.CardTitle.copy(color = Color.White),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = snapshot.activeCreatorOrSource,
                style = JoyTubeTypeScale.Microcopy.copy(color = Color(0xFFA6A8B2)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { onPlayPause() }
                .testTag("mini_player_play_pause"),
            contentAlignment = Alignment.Center
        ) {
            JoyTubeSvgIcon(
                type = if (isPlaying) JoyTubeIconType.Pause else JoyTubeIconType.Play,
                size = 18.dp,
                tint = Color.White
            )
        }

        if (!isCompactBetweenOrbs) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onNext() }
                    .testTag("mini_player_skip_next"),
                contentAlignment = Alignment.Center
            ) {
                JoyTubeSvgIcon(
                    type = JoyTubeIconType.ArrowForward,
                    size = 18.dp,
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * 4. BUTTON SYSTEM (PART XI — SECTION 37)
 */
@Composable
fun JoyTubePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: JoyTubeIconType? = null
) {
    val isGlass = LocalGlassDesignEnabled.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor = when {
        !enabled -> Color(0xFF141416)
        isPressed -> JoyTubeColors.InteractivePressed
        else -> JoyTubeColors.Selected
    }
    val borderColor = if (enabled) Color(0x3DFFFFFF) else JoyTubeColors.BorderSubtle
    val textColor = if (enabled) JoyTubeColors.TextPrimary else JoyTubeColors.TextDisabled

    val buttonSurfaceModifier = if (isGlass && enabled) {
        Modifier
            .joyTubeTouchBounce(interactionSource, pressedScale = 0.95f)
            .background(
                brush = Brush.linearGradient(
                    colors = if (isPressed) {
                        listOf(Color(0xEB464854), Color(0xD62A2C34))
                    } else {
                        listOf(
                            JoyTubeColors.LiquidGlassPillActiveTop,
                            JoyTubeColors.LiquidGlassPillActiveBottom
                        )
                    }
                ),
                shape = JoyTubeRadius.InputField
            )
            .drawWithCache {
                val topSheen = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.36f), Color.Transparent)
                )
                val p1 = Offset(size.width * 0.1f, 1.dp.toPx())
                val p2 = Offset(size.width * 0.9f, 1.dp.toPx())
                val sw = 1.15.dp.toPx()
                onDrawWithContent {
                    drawContent()
                    drawLine(brush = topSheen, start = p1, end = p2, strokeWidth = sw)
                }
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        JoyTubeColors.LiquidGlassBorderBright,
                        JoyTubeColors.LiquidGlassBorderSubtle
                    )
                ),
                shape = JoyTubeRadius.InputField
            )
    } else {
        Modifier
            .background(bgColor, JoyTubeRadius.InputField)
            .border(1.dp, borderColor, JoyTubeRadius.InputField)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(buttonSurfaceModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading
            ) { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = textColor
                )
            } else if (leadingIcon != null) {
                JoyTubeSvgIcon(type = leadingIcon, size = 17.dp, tint = textColor)
            }
            Text(
                text = text,
                style = JoyTubeTypeScale.ButtonText.copy(color = textColor)
            )
        }
    }
}

@Composable
fun JoyTubeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
    leadingIcon: JoyTubeIconType? = null
) {
    val isGlass = LocalGlassDesignEnabled.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor = if (isPressed) JoyTubeColors.InteractiveHover else JoyTubeColors.Background
    val borderColor = when {
        !enabled -> JoyTubeColors.BorderSubtle
        isDestructive -> Color(0x4DFFFFFF)
        else -> JoyTubeColors.BorderStandard
    }
    val textColor = if (enabled) JoyTubeColors.TextPrimary else JoyTubeColors.TextDisabled

    val secondarySurfaceModifier = if (isGlass && enabled) {
        Modifier
            .joyTubeTouchBounce(interactionSource, pressedScale = 0.95f)
            .joyTubeOutlinedSurface(
                shape = JoyTubeRadius.InputField,
                backgroundColor = if (isPressed) JoyTubeColors.InteractiveHover else JoyTubeColors.Surface,
                borderColor = borderColor,
                topHighlightAlpha = 0.22f
            )
    } else {
        Modifier
            .background(bgColor, JoyTubeRadius.InputField)
            .border(1.dp, borderColor, JoyTubeRadius.InputField)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(secondarySurfaceModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingIcon != null) {
                JoyTubeSvgIcon(type = leadingIcon, size = 17.dp, tint = textColor)
            }
            Text(
                text = text,
                style = JoyTubeTypeScale.ButtonText.copy(color = textColor)
            )
        }
    }
}

@Composable
fun JoyTubeIconButton(
    icon: JoyTubeIconType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 18.dp,
    isSelected: Boolean = false
) {
    val isGlass = LocalGlassDesignEnabled.current
    val interactionSource = remember { MutableInteractionSource() }
    val bgColor = if (isSelected) JoyTubeColors.Selected else JoyTubeColors.Surface
    val borderColor = if (isSelected) JoyTubeColors.BorderFocused else JoyTubeColors.BorderStandard
    Box(
        modifier = modifier
            .size(size)
            .joyTubeTouchBounce(interactionSource, pressedScale = 0.88f)
            .joyTubeOutlinedSurface(
                shape = JoyTubeRadius.SmallControl,
                backgroundColor = bgColor,
                borderColor = borderColor,
                topHighlightAlpha = if (isGlass) 0.26f else 0.08f
            )
            .clip(JoyTubeRadius.SmallControl)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        JoyTubeSvgIcon(type = icon, size = iconSize, tint = JoyTubeColors.TextPrimary)
    }
}

/**
 * 5. FILTER PILLS & SEGMENTED CONTROLS
 */
@Composable
fun JoyTubeFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isGlass = LocalGlassDesignEnabled.current
    val interactionSource = remember { MutableInteractionSource() }
    val bg = if (isSelected) JoyTubeColors.Selected else JoyTubeColors.Background
    val border = if (isSelected) Color(0x4DFFFFFF) else JoyTubeColors.BorderStandard
    val textColor = if (isSelected) JoyTubeColors.TextPrimary else JoyTubeColors.TextSecondary

    val pillModifier = if (isGlass) {
        val pillBrush = if (isSelected) {
            Brush.linearGradient(
                colors = listOf(
                    JoyTubeColors.LiquidGlassPillActiveTop,
                    JoyTubeColors.LiquidGlassPillActiveBottom
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    JoyTubeColors.LiquidGlassSurfaceTop,
                    JoyTubeColors.LiquidGlassSurfaceBottom
                )
            )
        }
        val pillBorderBrush = Brush.linearGradient(
            colors = if (isSelected) {
                listOf(JoyTubeColors.LiquidGlassBorderBright, Color(0x45FFFFFF))
            } else {
                listOf(Color(0x38FFFFFF), Color(0x16FFFFFF))
            }
        )
        Modifier
            .joyTubeTouchBounce(
                interactionSource = interactionSource,
                pressedScale = 0.91f,
                selectedScale = if (isSelected) 1.03f else 1.0f
            )
            .background(brush = pillBrush, shape = JoyTubeRadius.Capsule)
            .border(width = 1.dp, brush = pillBorderBrush, shape = JoyTubeRadius.Capsule)
    } else {
        Modifier
            .background(bg, JoyTubeRadius.Capsule)
            .border(1.dp, border, JoyTubeRadius.Capsule)
    }

    Box(
        modifier = modifier
            .height(34.dp)
            .then(pillModifier)
            .clip(JoyTubeRadius.Capsule)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = JoyTubeTypeScale.Label.copy(color = textColor)
        )
    }
}

@Composable
fun JoyTubeSegmentedRow(
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isGlass = LocalGlassDesignEnabled.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            val interactionSource = remember { MutableInteractionSource() }
            val bg = if (isSelected) JoyTubeColors.SurfaceElevated else JoyTubeColors.Background
            val border = if (isSelected) Color(0x4DFFFFFF) else JoyTubeColors.BorderStandard
            val textCol = if (isSelected) JoyTubeColors.TextPrimary else JoyTubeColors.TextSecondary

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .joyTubeTouchBounce(interactionSource, pressedScale = 0.94f)
                    .joyTubeOutlinedSurface(
                        shape = JoyTubeRadius.InputField,
                        backgroundColor = if (isSelected && isGlass) JoyTubeColors.Selected else bg,
                        borderColor = border,
                        topHighlightAlpha = if (isSelected) 0.24f else 0.08f
                    )
                    .clip(JoyTubeRadius.InputField)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onSelect(option) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = JoyTubeTypeScale.ButtonText.copy(color = textCol)
                )
            }
        }
    }
}

/**
 * 6. INPUT FIELDS (Search Bar, Browser Address Bar & Form Inputs)
 */
@Composable
fun JoyTubeTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: JoyTubeIconType? = null,
    trailingIcon: JoyTubeIconType? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    isCapsule: Boolean = false,
    errorMessage: String? = null,
    onSubmit: (() -> Unit)? = null
) {
    val isGlass = LocalGlassDesignEnabled.current
    var isFocused by remember { mutableStateOf(false) }
    val trailingInteraction = remember { MutableInteractionSource() }
    val clearInteraction = remember { MutableInteractionSource() }
    val shape = if (isCapsule) JoyTubeRadius.Capsule else JoyTubeRadius.InputField
    val borderColor = when {
        errorMessage != null -> JoyTubeColors.StatusErrorBorder
        isFocused -> if (isGlass) JoyTubeColors.BorderActiveWhite else JoyTubeColors.BorderFocused
        else -> JoyTubeColors.BorderStandard
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .joyTubeOutlinedSurface(
                    shape = shape,
                    backgroundColor = if (isFocused && isGlass) JoyTubeColors.SurfaceElevated else JoyTubeColors.Surface,
                    borderColor = borderColor,
                    borderWidth = if (isFocused && isGlass) 1.25.dp else 1.dp,
                    topHighlightAlpha = if (isFocused) 0.32f else 0.16f
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .then(
                            if (isGlass) {
                                Modifier
                                    .background(Color(0x1FFFFFFF), CircleShape)
                                    .border(0.8.dp, Color(0x33FFFFFF), CircleShape)
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    JoyTubeSvgIcon(
                        type = leadingIcon,
                        size = 16.dp,
                        tint = if (isFocused || isGlass) JoyTubeColors.TextPrimary else JoyTubeColors.TextSecondary
                    )
                }
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = JoyTubeTypeScale.InputText.copy(color = JoyTubeColors.TextSubdued),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = JoyTubeTypeScale.InputText,
                    cursorBrush = SolidColor(JoyTubeColors.TextPrimary),
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (onSubmit != null) ImeAction.Search else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { onSubmit?.invoke() },
                        onDone = { onSubmit?.invoke() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                )
            }
            if (value.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .joyTubeTouchBounce(clearInteraction, pressedScale = 0.85f)
                        .then(
                            if (isGlass) {
                                Modifier
                                    .background(Color(0x24FFFFFF), CircleShape)
                                    .border(0.8.dp, Color(0x38FFFFFF), CircleShape)
                            } else Modifier
                        )
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = clearInteraction,
                            indication = null
                        ) { onValueChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    JoyTubeSvgIcon(
                        type = JoyTubeIconType.Close,
                        size = 14.dp,
                        tint = JoyTubeColors.TextPrimary
                    )
                }
            } else if (trailingIcon != null) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .joyTubeTouchBounce(trailingInteraction, pressedScale = 0.85f)
                        .then(
                            if (isGlass) {
                                Modifier
                                    .background(Color(0x22FFFFFF), CircleShape)
                                    .border(0.8.dp, Color(0x36FFFFFF), CircleShape)
                            } else Modifier
                        )
                        .clip(CircleShape)
                        .then(
                            if (onTrailingIconClick != null) {
                                Modifier.clickable(
                                    interactionSource = trailingInteraction,
                                    indication = null
                                ) { onTrailingIconClick() }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    JoyTubeSvgIcon(
                        type = trailingIcon,
                        size = 16.dp,
                        tint = if (isGlass) JoyTubeColors.TextPrimary else JoyTubeColors.TextSecondary
                    )
                }
            }
        }
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage,
                style = JoyTubeTypeScale.Metadata.copy(color = JoyTubeColors.StatusError),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/**
 * 7. OUTLINED CONTENT CARDS (PART XI — SECTION 39)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OutlinedVideoCard(
    media: MediaItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) JoyTubeColors.BorderActiveWhite else JoyTubeColors.BorderStandard
    Column(
        modifier = modifier
            .fillMaxWidth()
            .joyTubeOutlinedSurface(
                shape = JoyTubeRadius.ContentCard,
                backgroundColor = JoyTubeColors.Surface,
                borderColor = borderColor
            )
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onLongClick() else onClick()
                },
                onLongClick = onLongClick
            )
            .padding(12.dp)
            .testTag("video_card_${media.id}")
    ) {
        JoyTubeMediaThumbnail(
            preset = media.artwork,
            customUri = media.customThumbnailUri,
            sourceBadge = media.sourceDomain,
            durationBadge = media.durationLabel,
            showBookmark = true,
            isBookmarked = media.isBookmarked,
            onBookmarkClick = onBookmarkClick,
            progressFraction = media.watchProgress,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9.5f)
                .clip(JoyTubeRadius.ThumbnailInner)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.title,
                    style = JoyTubeTypeScale.CardTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Imported by @${media.importerUsername}",
                    style = JoyTubeTypeScale.Metadata
                )
            }

            if (isSelectionMode) {
                SelectionCheckIndicator(isSelected = isSelected)
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onMoreClick() }
                        .testTag("video_more_${media.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    JoyTubeSvgIcon(
                        type = JoyTubeIconType.MoreHorizontal,
                        size = 18.dp,
                        tint = JoyTubeColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun CompactContinueWatchingCard(
    media: MediaItem,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
    ) {
        JoyTubeMediaThumbnail(
            preset = media.artwork,
            customUri = media.customThumbnailUri,
            showBookmark = true,
            isBookmarked = media.isBookmarked,
            onBookmarkClick = onBookmarkClick,
            progressFraction = media.watchProgress,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.25f)
                .clip(JoyTubeRadius.ThumbnailInner)
                .border(1.dp, JoyTubeColors.BorderStandard, JoyTubeRadius.ThumbnailInner)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = media.title,
            style = JoyTubeTypeScale.CardTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactMediaRowItem(
    media: MediaItem,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    trailingIcon: JoyTubeIconType = JoyTubeIconType.MoreHorizontal,
    onTrailingClick: (() -> Unit)? = null,
    subtitleOverride: String? = null,
    secondaryMeta: String? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode && onLongClick != null) onLongClick()
                    else onClick()
                },
                onLongClick = { onLongClick?.invoke() }
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        JoyTubeMediaThumbnail(
            preset = media.artwork,
            customUri = media.customThumbnailUri,
            modifier = Modifier
                .width(106.dp)
                .height(62.dp)
                .clip(CompactThumbShape)
                .border(1.dp, JoyTubeColors.BorderStandard, CompactThumbShape)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = media.title,
                style = JoyTubeTypeScale.CardTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitleOverride ?: "Imported by @${media.importerUsername}",
                style = JoyTubeTypeScale.Metadata
            )
            if (secondaryMeta != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = secondaryMeta,
                    style = JoyTubeTypeScale.Microcopy
                )
            }
        }

        if (isSelectionMode) {
            SelectionCheckIndicator(isSelected = isSelected)
        } else if (onTrailingClick != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onTrailingClick() },
                contentAlignment = Alignment.Center
            ) {
                JoyTubeSvgIcon(
                    type = trailingIcon,
                    size = 18.dp,
                    tint = if (trailingIcon == JoyTubeIconType.HeartFilled) JoyTubeColors.TextPrimary else JoyTubeColors.TextSecondary
                )
            }
        }
    }
}

/**
 * 8. OUTLINED CATEGORY CARD
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OutlinedCategoryCard(
    category: CategoryItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) JoyTubeColors.BorderActiveWhite else JoyTubeColors.BorderStandard
    Column(
        modifier = modifier
            .fillMaxWidth()
            .joyTubeOutlinedSurface(
                shape = JoyTubeRadius.ContentCard,
                backgroundColor = JoyTubeColors.Surface,
                borderColor = borderColor
            )
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onLongClick() else onClick()
                },
                onLongClick = onLongClick
            )
            .padding(12.dp)
            .testTag("category_card_${category.id}")
    ) {
        JoyTubeMediaThumbnail(
            preset = category.artwork,
            sourceBadge = category.sourceDomain,
            showBookmark = true,
            isBookmarked = category.isBookmarked,
            onBookmarkClick = onBookmarkClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(JoyTubeRadius.ThumbnailInner)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = category.name,
                style = JoyTubeTypeScale.CardTitle,
                modifier = Modifier.weight(1f)
            )
            if (isSelectionMode) {
                SelectionCheckIndicator(isSelected = isSelected)
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onMoreClick() },
                    contentAlignment = Alignment.Center
                ) {
                    JoyTubeSvgIcon(
                        type = JoyTubeIconType.MoreHorizontal,
                        size = 18.dp,
                        tint = JoyTubeColors.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Created by @${category.creatorUsername}",
                style = JoyTubeTypeScale.Metadata
            )
            Text(
                text = category.visibility.label,
                style = JoyTubeTypeScale.Label.copy(color = JoyTubeColors.TextPrimary)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${category.mediaIds.size} items",
            style = JoyTubeTypeScale.Metadata
        )
    }
}

@Composable
fun SelectionCheckIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .background(
                if (isSelected) JoyTubeColors.Selected else Color.Transparent,
                CircleShape
            )
            .border(
                width = 1.4.dp,
                color = if (isSelected) JoyTubeColors.TextPrimary else JoyTubeColors.TextSecondary,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            JoyTubeSvgIcon(
                type = JoyTubeIconType.Check,
                size = 12.dp,
                tint = JoyTubeColors.TextPrimary
            )
        }
    }
}

/**
 * 9. DESIGNED EMPTY STATE CARD
 */
@Composable
fun JoyTubeEmptyStateCard(
    icon: JoyTubeIconType,
    title: String,
    subtitle: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .joyTubeOutlinedSurface(
                shape = JoyTubeRadius.ContentCard,
                backgroundColor = JoyTubeColors.Surface,
                borderColor = JoyTubeColors.BorderStandard
            )
            .padding(vertical = 28.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        JoyTubeSvgIcon(
            type = icon,
            size = 24.dp,
            tint = JoyTubeColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = JoyTubeTypeScale.CardTitle
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = JoyTubeTypeScale.SupportingText
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = actionLabel,
            style = JoyTubeTypeScale.Label.copy(color = JoyTubeColors.TextPrimary),
            modifier = Modifier
                .clip(JoyTubeRadius.SmallControl)
                .clickable { onActionClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * 10. STRUCTURAL SKELETON PLACEHOLDER (GPU-accelerated via graphicsLayer)
 */
@Composable
fun JoyTubeSkeletonVideoCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "SkeletonPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SkeletonAlpha"
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .joyTubeOutlinedSurface(
                shape = JoyTubeRadius.ContentCard,
                backgroundColor = JoyTubeColors.Surface,
                borderColor = JoyTubeColors.BorderStandard
            )
            .padding(12.dp)
            .graphicsLayer { this.alpha = alpha }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9.5f)
                .background(JoyTubeColors.SurfaceElevated, JoyTubeRadius.ThumbnailInner)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .height(16.dp)
                .background(JoyTubeColors.SurfaceElevated, SkeletonBarShape)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.38f)
                .height(12.dp)
                .background(JoyTubeColors.SurfaceElevated, SkeletonBarShape)
        )
    }
}

/**
 * 11. BULK SELECTION BOTTOM TOOLBAR
 */
@Composable
fun BulkMediaSelectionToolbar(
    onMoveClick: () -> Unit,
    onFavouriteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .joyTubeOutlinedSurface(
                shape = JoyTubeRadius.ContentCard,
                backgroundColor = JoyTubeColors.SurfaceModal,
                borderColor = JoyTubeColors.BorderStandard
            )
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BulkToolbarAction(icon = JoyTubeIconType.FolderPlus, label = "Move", onClick = onMoveClick)
        BulkToolbarAction(icon = JoyTubeIconType.HeartOutline, label = "Favourite", onClick = onFavouriteClick)
        BulkToolbarAction(icon = JoyTubeIconType.Download, label = "Download", onClick = onDownloadClick)
        BulkToolbarAction(icon = JoyTubeIconType.Trash, label = "Delete", onClick = onDeleteClick)
    }
}

@Composable
private fun BulkToolbarAction(
    icon: JoyTubeIconType,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(JoyTubeRadius.SmallControl)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        JoyTubeSvgIcon(type = icon, size = 18.dp, tint = JoyTubeColors.TextPrimary)
        Text(text = label, style = JoyTubeTypeScale.Microcopy.copy(color = JoyTubeColors.TextPrimary))
    }
}

/**
 * 12. FLOATING CONTEXTUAL TEXT / FEEDBACK BANNER
 */
@Composable
fun FloatingFeedbackToast(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(tween(150)) + slideInVertically(tween(180)) { -it / 2 },
        exit = fadeOut(tween(150)) + slideOutVertically(tween(160)) { -it / 2 },
        modifier = modifier
    ) {
        if (message != null) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .joyTubeOutlinedSurface(
                        shape = JoyTubeRadius.Capsule,
                        backgroundColor = JoyTubeColors.SurfaceElevated,
                        borderColor = JoyTubeColors.BorderFocused,
                        topHighlightAlpha = 0.15f
                    )
                    .clickable { onDismiss() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                JoyTubeSvgIcon(
                    type = JoyTubeIconType.CheckCircle,
                    size = 16.dp,
                    tint = JoyTubeColors.TextPrimary
                )
                Text(
                    text = message,
                    style = JoyTubeTypeScale.Label,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
