package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * Global CompositionLocal controlling whether the translucent Liquid Glass Design
 * is active across navigation, search, icons, cards, and buttons.
 */
val LocalGlassDesignEnabled = compositionLocalOf { true }

/**
 * JOYTUBE GEOMETRY & HEMISPHERICAL RADIUS SYSTEM (PART II — SECTION 06)
 */
object JoyTubeRadius {
    val SmallControl = RoundedCornerShape(10.dp)    // Icon buttons, small action pills, domain badges
    val InputField = RoundedCornerShape(12.dp)      // Text inputs, segmented controls, buttons
    val ContentCard = RoundedCornerShape(16.dp)     // Outlined video cards, category cards, download cards
    val ThumbnailInner = RoundedCornerShape(12.dp)  // Inner media thumbnail inside outlined containers
    val ModalDialog = RoundedCornerShape(20.dp)     // Pop-up dialogs, bottom sheets, context menus
    val Capsule = RoundedCornerShape(50)            // Hemispherical ends for Floating Dock, Search bar & Filter pills
    val Circle = CircleShape                        // Optically centered active navigation circle & avatars
}

val JoyTubeShapes = Shapes(
    extraSmall = JoyTubeRadius.SmallControl,
    small = JoyTubeRadius.InputField,
    large = JoyTubeRadius.ModalDialog,
    medium = JoyTubeRadius.ContentCard,
    extraLarge = JoyTubeRadius.Capsule
)

private val JoyTubeDarkColorScheme = darkColorScheme(
    primary = JoyTubeColors.TextPrimary,
    onPrimary = JoyTubeColors.Background,
    primaryContainer = JoyTubeColors.Selected,
    onPrimaryContainer = JoyTubeColors.TextPrimary,
    secondary = JoyTubeColors.TextSecondary,
    onSecondary = JoyTubeColors.Background,
    secondaryContainer = JoyTubeColors.SurfaceElevated,
    onSecondaryContainer = JoyTubeColors.TextPrimary,
    background = JoyTubeColors.Background,
    onBackground = JoyTubeColors.TextPrimary,
    surface = JoyTubeColors.Surface,
    onSurface = JoyTubeColors.TextPrimary,
    surfaceVariant = JoyTubeColors.SurfaceElevated,
    onSurfaceVariant = JoyTubeColors.TextSecondary,
    outline = JoyTubeColors.BorderStandard,
    outlineVariant = JoyTubeColors.BorderSubtle,
    error = JoyTubeColors.StatusError
)

/**
 * Performance-optimized Outlined / Liquid-Glass Surface Modifier (Part XII — Section 44)
 * Automatically switches between classic matte outlined surfaces and translucent
 * multi-layered specular glass when [LocalGlassDesignEnabled] is enabled.
 * Uses drawWithCache so all gradients and specular highlights are cached per size change (zero lag).
 */
@Composable
fun Modifier.joyTubeOutlinedSurface(
    shape: Shape = JoyTubeRadius.ContentCard,
    backgroundColor: Color = JoyTubeColors.Surface,
    borderColor: Color = JoyTubeColors.BorderStandard,
    borderWidth: Dp = 1.dp,
    topHighlightAlpha: Float = 0.08f
): Modifier {
    val isGlass = LocalGlassDesignEnabled.current
    return if (isGlass) {
        val glassBrush = Brush.linearGradient(
            colors = listOf(
                JoyTubeColors.LiquidGlassSurfaceTop,
                backgroundColor.copy(alpha = 0.72f),
                JoyTubeColors.LiquidGlassSurfaceBottom
            )
        )
        val glassBorderBrush = Brush.linearGradient(
            colors = listOf(
                JoyTubeColors.LiquidGlassBorderBright,
                borderColor.copy(alpha = 0.55f),
                JoyTubeColors.LiquidGlassBorderSubtle
            )
        )
        val effectiveHighlight = (topHighlightAlpha * 2.4f).coerceIn(0.18f, 0.42f)
        this
            .background(brush = glassBrush, shape = shape)
            .drawWithCache {
                val topSheenBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = effectiveHighlight),
                        Color.Transparent
                    )
                )
                val innerGlowBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.07f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.42f
                )
                val startPt = Offset(size.width * 0.10f, 0.75f)
                val endPt = Offset(size.width * 0.90f, 0.75f)
                val strokePx = 1.15.dp.toPx()
                onDrawWithContent {
                    drawRect(brush = innerGlowBrush)
                    drawContent()
                    drawLine(
                        brush = topSheenBrush,
                        start = startPt,
                        end = endPt,
                        strokeWidth = strokePx
                    )
                }
            }
            .border(width = borderWidth, brush = glassBorderBrush, shape = shape)
    } else {
        this
            .background(backgroundColor, shape)
            .then(
                if (topHighlightAlpha > 0f) {
                    Modifier.drawWithCache {
                        val highlightBrush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = topHighlightAlpha),
                                Color.Transparent
                            )
                        )
                        val startPt = Offset(size.width * 0.12f, 0.5f)
                        val endPt = Offset(size.width * 0.88f, 0.5f)
                        val strokePx = 1.dp.toPx()
                        onDrawWithContent {
                            drawContent()
                            drawLine(
                                brush = highlightBrush,
                                start = startPt,
                                end = endPt,
                                strokeWidth = strokePx
                            )
                        }
                    }
                } else Modifier
            )
            .border(borderWidth, borderColor, shape)
    }
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = JoyTubeDarkColorScheme,
        typography = Typography,
        shapes = JoyTubeShapes,
        content = content
    )
}
