package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.icons.JoyTubeIconType
import com.example.ui.icons.JoyTubeSvgIcon
import com.example.ui.theme.JoyTubeColors
import com.example.ui.theme.JoyTubeTypeScale

enum class ArtworkPreset {
    NightMountainLake,     // "A Journey Beyond"
    CyberpunkCityscape,    // "The City That Never Sleeps"
    WorkspaceDesk,         // "My New Workspace" / "A Complete Desk Setup"
    GamingRigSetup,        // "Gaming Collection"
    EmeraldWaterfall,      // "Into the Wild" / "Hidden Waterfalls"
    NorthernLights,        // "The Northern Lights" (Short)
    WinterCabin,           // "Winter Cabin"
    MistyAlpineLake,       // "Nature Collection" / "A Quiet Place"
    OceanReef,             // "Ocean from Above" / "Coastal Escape"
    ModernArchitecture,    // "Modern Architecture" / "Modern House"
    VoxelLandscape,        // "Building a New World" / "Mountain Village"
    RedstoneCircuit,       // "Redstone Project"
    SynthwaveStudio        // "Music Collection"
}

private val DomainPillShape = RoundedCornerShape(50)
private val DurationPillShape = RoundedCornerShape(6.dp)
private val StarCoordinates = floatArrayOf(
    0.12f, 0.14f, 0.24f, 0.08f, 0.38f, 0.19f,
    0.49f, 0.11f, 0.55f, 0.25f, 0.68f, 0.15f,
    0.81f, 0.10f, 0.89f, 0.24f, 0.18f, 0.31f,
    0.53f, 0.06f, 0.62f, 0.29f, 0.75f, 0.21f
)
private val CityTowerSpecs = arrayOf(
    floatArrayOf(0.06f, 0.38f, 0.08f),
    floatArrayOf(0.16f, 0.28f, 0.07f),
    floatArrayOf(0.26f, 0.42f, 0.09f),
    floatArrayOf(0.36f, 0.14f, 0.06f),
    floatArrayOf(0.45f, 0.34f, 0.10f),
    floatArrayOf(0.58f, 0.20f, 0.07f),
    floatArrayOf(0.68f, 0.36f, 0.09f),
    floatArrayOf(0.80f, 0.26f, 0.11f)
)

/**
 * Performance-Engineered Media Thumbnail (Part XII — Section 44)
 * Combines scene rendering, vignette, and watch progress bar into a single
 * cached DrawScope pass (`drawWithCache`) with zero per-frame allocations.
 */
@Composable
fun JoyTubeMediaThumbnail(
    preset: ArtworkPreset,
    modifier: Modifier = Modifier,
    customUri: String? = null,
    sourceBadge: String? = null,
    durationBadge: String? = null,
    showBookmark: Boolean = false,
    isBookmarked: Boolean = false,
    onBookmarkClick: (() -> Unit)? = null,
    progressFraction: Float = 0f
) {
    Box(modifier = modifier) {
        if (!customUri.isNullOrBlank()) {
            AsyncImage(
                model = customUri,
                contentDescription = "Imported media thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AtmosphericSceneCanvas(
                preset = preset,
                progressFraction = progressFraction,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top-right Bookmark Circle Badge
        if (showBookmark) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(32.dp)
                    .background(JoyTubeColors.GlassBadgeTint, CircleShape)
                    .border(1.dp, JoyTubeColors.BorderGlass, CircleShape)
                    .clip(CircleShape)
                    .then(
                        if (onBookmarkClick != null) {
                            Modifier.clickable { onBookmarkClick() }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                JoyTubeSvgIcon(
                    type = if (isBookmarked) JoyTubeIconType.BookmarkFilled else JoyTubeIconType.Bookmark,
                    size = 15.dp,
                    tint = JoyTubeColors.TextPrimary
                )
            }
        }

        // Bottom-left Domain Source Pill
        if (!sourceBadge.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .background(Color(0xCC08080A), DomainPillShape)
                    .border(0.8.dp, Color(0x2EFFFFFF), DomainPillShape)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = sourceBadge,
                    style = JoyTubeTypeScale.Microcopy.copy(color = JoyTubeColors.TextPrimary)
                )
            }
        }

        // Optional bottom-right duration pill
        if (!durationBadge.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .background(Color(0xCC08080A), DurationPillShape)
                    .border(0.8.dp, Color(0x26FFFFFF), DurationPillShape)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = durationBadge,
                    style = JoyTubeTypeScale.Microcopy.copy(color = JoyTubeColors.TextPrimary)
                )
            }
        }
    }
}

/**
 * Hardware-Cached Scene Renderer (`drawWithCache`)
 * Pre-computes all Brushes, Paths, and Vignettes only once when layout size changes.
 * Scrolling in LazyColumn executes zero object allocations per frame.
 */
@Composable
fun AtmosphericSceneCanvas(
    preset: ArtworkPreset,
    progressFraction: Float = 0f,
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val clampedProgress = progressFraction.coerceIn(0f, 1f)
            val barH = 3.dp.toPx()

            val vignetteBrush = Brush.verticalGradient(
                0.0f to Color(0x2E000000),
                0.55f to Color.Transparent,
                1.0f to Color(0x88000000)
            )

            // Pre-build Paths & Brushes once per size
            val skyBrush: Brush
            var secondaryBrush: Brush? = null
            var lakeBrush: Brush? = null
            var primaryPath: Path? = null
            var secondaryPath: Path? = null
            var strokePath1: Path? = null
            var strokePath2: Path? = null

            when (preset) {
                ArtworkPreset.NightMountainLake -> {
                    skyBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF050A14),
                            Color(0xFF0D1E3A),
                            Color(0xFF142C4E),
                            Color(0xFF0A192F),
                            Color(0xFF040911)
                        )
                    )
                    secondaryBrush = Brush.radialGradient(
                        colors = listOf(Color(0x448AB4F8), Color(0x185E81AC), Color.Transparent),
                        center = Offset(w * 0.52f, h * 0.22f),
                        radius = (w * 0.35f).coerceAtLeast(1f)
                    )
                    primaryPath = Path().apply {
                        moveTo(0f, h * 0.56f)
                        lineTo(w * 0.16f, h * 0.36f)
                        lineTo(w * 0.31f, h * 0.46f)
                        lineTo(w * 0.48f, h * 0.29f)
                        lineTo(w * 0.65f, h * 0.44f)
                        lineTo(w * 0.82f, h * 0.32f)
                        lineTo(w, h * 0.50f)
                        lineTo(w, h * 0.58f)
                        lineTo(0f, h * 0.58f)
                        close()
                    }
                    lakeBrush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F2544), Color(0xFF173761), Color(0xFF060E1A)),
                        startY = h * 0.58f,
                        endY = h
                    )
                }

                ArtworkPreset.CyberpunkCityscape -> {
                    skyBrush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF060814), Color(0xFF0E1B38), Color(0xFF1A103C), Color(0xFF080B16))
                    )
                    secondaryBrush = Brush.radialGradient(
                        colors = listOf(Color(0x5500E5FF), Color(0x229D4EDD), Color.Transparent),
                        center = Offset(w * 0.45f, h * 0.52f),
                        radius = (w * 0.55f).coerceAtLeast(1f)
                    )
                    strokePath1 = Path().apply {
                        moveTo(w * 0.15f, h)
                        quadraticTo(w * 0.48f, h * 0.78f, w * 0.62f, h * 0.72f)
                    }
                    strokePath2 = Path().apply {
                        moveTo(w * 0.28f, h)
                        quadraticTo(w * 0.52f, h * 0.80f, w * 0.64f, h * 0.72f)
                    }
                }

                ArtworkPreset.WorkspaceDesk, ArtworkPreset.GamingRigSetup -> {
                    val isGaming = preset == ArtworkPreset.GamingRigSetup
                    skyBrush = Brush.linearGradient(
                        colors = if (isGaming) {
                            listOf(Color(0xFF140D2B), Color(0xFF0B132B), Color(0xFF07070C))
                        } else {
                            listOf(Color(0xFF1A0E1C), Color(0xFF0C1624), Color(0xFF060609))
                        }
                    )
                    secondaryBrush = Brush.radialGradient(
                        colors = listOf(
                            if (isGaming) Color(0x557B2CBF) else Color(0x44E63946),
                            Color(0x2200B4D8),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.45f, h * 0.42f),
                        radius = (w * 0.5f).coerceAtLeast(1f)
                    )
                    lakeBrush = Brush.linearGradient(
                        colors = listOf(Color(0xFF1D3557), Color(0xFFE76F51), Color(0xFF0F172A))
                    )
                }

                ArtworkPreset.EmeraldWaterfall -> {
                    skyBrush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1B2E1E), Color(0xFF0F2419), Color(0xFF081715), Color(0xFF051014))
                    )
                    secondaryBrush = Brush.radialGradient(
                        colors = listOf(Color(0x55D9ED92), Color(0x2252B788), Color.Transparent),
                        center = Offset(w * 0.46f, h * 0.12f),
                        radius = (w * 0.5f).coerceAtLeast(1f)
                    )
                    lakeBrush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF164A41), Color(0xFF0B2B26)),
                        startY = h * 0.72f,
                        endY = h
                    )
                }

                ArtworkPreset.NorthernLights -> {
                    skyBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF090D1C),
                            Color(0xFF281845),
                            Color(0xFF164E63),
                            Color(0xFF0F172A),
                            Color(0xFF080D1A)
                        )
                    )
                    secondaryBrush = Brush.radialGradient(
                        colors = listOf(Color(0x77EC4899), Color(0x33A855F7), Color.Transparent),
                        center = Offset(w * 0.35f, h * 0.34f),
                        radius = (w * 0.55f).coerceAtLeast(1f)
                    )
                    lakeBrush = Brush.radialGradient(
                        colors = listOf(Color(0x7710B981), Color(0x4406B6D4), Color.Transparent),
                        center = Offset(w * 0.72f, h * 0.40f),
                        radius = (w * 0.55f).coerceAtLeast(1f)
                    )
                    primaryPath = Path().apply {
                        moveTo(0f, h * 0.76f)
                        lineTo(w * 0.25f, h * 0.58f)
                        lineTo(w * 0.52f, h * 0.68f)
                        lineTo(w * 0.80f, h * 0.60f)
                        lineTo(w, h * 0.70f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                }

                ArtworkPreset.WinterCabin -> {
                    skyBrush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F1D2E), Color(0xFF192B3E), Color(0xFF0E1B2A), Color(0xFF071019))
                    )
                    primaryPath = Path().apply {
                        moveTo(0f, h * 0.55f)
                        lineTo(w * 0.35f, h * 0.22f)
                        lineTo(w * 0.68f, h * 0.16f)
                        lineTo(w, h * 0.42f)
                        lineTo(w, h * 0.58f)
                        lineTo(0f, h * 0.58f)
                        close()
                    }
                    val cx = w * 0.44f
                    val cy = h * 0.45f
                    secondaryPath = Path().apply {
                        moveTo(cx - w * 0.08f, cy + h * 0.10f)
                        lineTo(cx - w * 0.08f, cy)
                        lineTo(cx, cy - h * 0.09f)
                        lineTo(cx + w * 0.08f, cy)
                        lineTo(cx + w * 0.08f, cy + h * 0.10f)
                        close()
                    }
                    secondaryBrush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF9F1C), Color(0x55F77F00), Color.Transparent),
                        center = Offset(cx, cy + h * 0.03f),
                        radius = (w * 0.12f).coerceAtLeast(1f)
                    )
                }

                ArtworkPreset.MistyAlpineLake -> {
                    skyBrush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF283848), Color(0xFF475B6D), Color(0xFF1D4E5F), Color(0xFF0D2C3A))
                    )
                    primaryPath = Path().apply {
                        moveTo(0f, h * 0.52f)
                        lineTo(w * 0.22f, h * 0.22f)
                        lineTo(w * 0.48f, h * 0.42f)
                        lineTo(w * 0.78f, h * 0.20f)
                        lineTo(w, h * 0.46f)
                        lineTo(w, h * 0.54f)
                        lineTo(0f, h * 0.54f)
                        close()
                    }
                    secondaryBrush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x66CBD5E1), Color.Transparent),
                        startY = h * 0.36f,
                        endY = h * 0.56f
                    )
                }

                ArtworkPreset.OceanReef -> {
                    skyBrush = Brush.linearGradient(
                        colors = listOf(Color(0xFF042F40), Color(0xFF006D77), Color(0xFF14B8A6), Color(0xFF0F172A))
                    )
                    secondaryBrush = Brush.radialGradient(
                        colors = listOf(Color(0x665EEAD4), Color.Transparent),
                        center = Offset(w * 0.35f, h * 0.48f),
                        radius = (w * 0.4f).coerceAtLeast(1f)
                    )
                }

                ArtworkPreset.ModernArchitecture -> {
                    skyBrush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF090D16))
                    )
                }

                ArtworkPreset.VoxelLandscape -> {
                    skyBrush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF2B192E), Color(0xFF4A2545), Color(0xFF1B3B2B), Color(0xFF0D1F18))
                    )
                    secondaryBrush = Brush.radialGradient(
                        colors = listOf(Color(0x88F59E0B), Color.Transparent),
                        center = Offset(w * 0.65f, h * 0.32f),
                        radius = (w * 0.35f).coerceAtLeast(1f)
                    )
                }

                ArtworkPreset.RedstoneCircuit, ArtworkPreset.SynthwaveStudio -> {
                    skyBrush = Brush.linearGradient(
                        colors = listOf(Color(0xFF1F0933), Color(0xFF3B0764), Color(0xFF0F172A))
                    )
                    secondaryBrush = Brush.radialGradient(
                        colors = listOf(Color(0x77F43F5E), Color(0x338B5CF6), Color.Transparent),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = (w * 0.45f).coerceAtLeast(1f)
                    )
                }
            }

            onDrawBehind {
                drawRect(brush = skyBrush)

                when (preset) {
                    ArtworkPreset.NightMountainLake -> {
                        secondaryBrush?.let { drawOval(brush = it) }
                        var i = 0
                        while (i < StarCoordinates.size) {
                            val sx = w * StarCoordinates[i]
                            val sy = h * StarCoordinates[i + 1]
                            val idx = i / 2
                            drawCircle(
                                color = if (idx % 2 == 0) Color(0xD9FFFFFF) else Color(0x73FFFFFF),
                                radius = if (idx % 3 == 0) 2.2f else 1.4f,
                                center = Offset(sx, sy)
                            )
                            i += 2
                        }
                        primaryPath?.let { drawPath(it, Color(0xFF0A1424)) }
                        lakeBrush?.let {
                            drawRect(
                                brush = it,
                                topLeft = Offset(0f, h * 0.58f),
                                size = Size(w, h * 0.42f)
                            )
                        }
                    }

                    ArtworkPreset.CyberpunkCityscape -> {
                        secondaryBrush?.let {
                            drawCircle(brush = it, center = Offset(w * 0.45f, h * 0.52f), radius = w * 0.55f)
                        }
                        for (idx in CityTowerSpecs.indices) {
                            val spec = CityTowerSpecs[idx]
                            val tx = w * spec[0]
                            val ty = h * spec[1]
                            val tw = w * spec[2]
                            val th = h * (0.76f - spec[1])
                            drawRect(
                                color = Color(0xFF0B1325),
                                topLeft = Offset(tx, ty),
                                size = Size(tw, th)
                            )
                            drawLine(
                                color = if (idx % 2 == 0) Color(0x9900E5FF) else Color(0x99B5179E),
                                start = Offset(tx + tw * 0.5f, ty - h * 0.05f),
                                end = Offset(tx + tw * 0.5f, ty + th),
                                strokeWidth = 2f
                            )
                        }
                        strokePath1?.let { drawPath(it, Color(0xCCFF9E00), style = Stroke(width = 4f)) }
                        strokePath2?.let { drawPath(it, Color(0xCC00F5D4), style = Stroke(width = 3f)) }
                    }

                    ArtworkPreset.WorkspaceDesk, ArtworkPreset.GamingRigSetup -> {
                        secondaryBrush?.let {
                            drawCircle(brush = it, center = Offset(w * 0.45f, h * 0.42f), radius = w * 0.5f)
                        }
                        val monLeft = w * 0.18f
                        val monTop = h * 0.18f
                        val monW = w * 0.46f
                        val monH = h * 0.40f
                        lakeBrush?.let {
                            drawRoundRect(
                                brush = it,
                                topLeft = Offset(monLeft, monTop),
                                size = Size(monW, monH),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                        drawRoundRect(
                            color = Color(0xFF334155),
                            topLeft = Offset(monLeft, monTop),
                            size = Size(monW, monH),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = Stroke(width = 3f)
                        )
                        val pcLeft = w * 0.68f
                        val pcTop = h * 0.20f
                        val pcW = w * 0.20f
                        val pcH = h * 0.46f
                        drawRoundRect(
                            color = Color(0xFF0B0E17),
                            topLeft = Offset(pcLeft, pcTop),
                            size = Size(pcW, pcH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        for (fanIdx in 0..2) {
                            val fy = pcTop + pcH * (0.22f + fanIdx * 0.28f)
                            drawCircle(
                                color = Color(0xFF00F5D4),
                                radius = pcW * 0.22f,
                                center = Offset(pcLeft + pcW * 0.55f, fy),
                                style = Stroke(width = 3f)
                            )
                        }
                        drawRect(
                            color = Color(0xFF090A10),
                            topLeft = Offset(0f, h * 0.68f),
                            size = Size(w, h * 0.32f)
                        )
                    }

                    ArtworkPreset.EmeraldWaterfall -> {
                        secondaryBrush?.let {
                            drawCircle(brush = it, center = Offset(w * 0.46f, h * 0.12f), radius = w * 0.5f)
                        }
                        drawRect(
                            color = Color(0x66E8F8F5),
                            topLeft = Offset(w * 0.44f, h * 0.18f),
                            size = Size(w * 0.08f, h * 0.56f)
                        )
                        lakeBrush?.let {
                            drawRect(
                                brush = it,
                                topLeft = Offset(0f, h * 0.72f),
                                size = Size(w, h * 0.28f)
                            )
                        }
                    }

                    ArtworkPreset.NorthernLights -> {
                        secondaryBrush?.let { drawOval(brush = it) }
                        lakeBrush?.let { drawOval(brush = it) }
                        primaryPath?.let { drawPath(it, Color(0xFF111C2D)) }
                    }

                    ArtworkPreset.WinterCabin -> {
                        primaryPath?.let { drawPath(it, Color(0xFF1F3247)) }
                        secondaryPath?.let { drawPath(it, Color(0xFF2B1D14)) }
                        secondaryBrush?.let {
                            drawCircle(
                                brush = it,
                                center = Offset(w * 0.44f, h * 0.48f),
                                radius = w * 0.12f
                            )
                        }
                    }

                    ArtworkPreset.MistyAlpineLake -> {
                        primaryPath?.let { drawPath(it, Color(0xFF1E2D3D)) }
                        secondaryBrush?.let {
                            drawRect(
                                brush = it,
                                topLeft = Offset(0f, h * 0.36f),
                                size = Size(w, h * 0.20f)
                            )
                        }
                    }

                    ArtworkPreset.OceanReef, ArtworkPreset.VoxelLandscape,
                    ArtworkPreset.RedstoneCircuit, ArtworkPreset.SynthwaveStudio -> {
                        secondaryBrush?.let { drawOval(brush = it) }
                    }

                    ArtworkPreset.ModernArchitecture -> {
                        drawRect(
                            color = Color(0xFF334155),
                            topLeft = Offset(w * 0.18f, h * 0.30f),
                            size = Size(w * 0.64f, h * 0.10f)
                        )
                        drawRect(
                            color = Color(0xCCFBBF24),
                            topLeft = Offset(w * 0.24f, h * 0.40f),
                            size = Size(w * 0.52f, h * 0.24f)
                        )
                        drawRect(
                            color = Color(0xFF1E293B),
                            topLeft = Offset(w * 0.14f, h * 0.64f),
                            size = Size(w * 0.72f, h * 0.08f)
                        )
                    }
                }

                // Bottom vignette in same draw pass
                drawRect(brush = vignetteBrush)

                // Optional watch progress bar in same draw pass
                if (clampedProgress > 0f) {
                    drawRect(
                        color = Color(0x4DFFFFFF),
                        topLeft = Offset(0f, h - barH),
                        size = Size(w, barH)
                    )
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(0f, h - barH),
                        size = Size(w * clampedProgress, barH)
                    )
                }
            }
        }
    )
}
