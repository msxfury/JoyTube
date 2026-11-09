package com.example.ui.icons

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JoyTubeColors

/**
 * JOYTUBE UNIFIED SVG ICON ENGINEERING SYSTEM (PART III — SECTION 10 & PART XII — SECTION 44)
 *
 * Uses `drawWithCache` so all vector Paths and Strokes are allocated once per size
 * and cached in hardware display lists (zero allocations during scrolling).
 */
enum class JoyTubeIconType {
    BrandLogoLeftTriangle,
    NavHome,
    NavShorts,
    NavSearch,
    NavAccount,
    ArrowBack,
    ArrowForward,
    ArrowUpLeft,
    ChevronRight,
    ChevronDown,
    Close,
    MoreHorizontal,
    Bookmark,
    BookmarkFilled,
    HeartOutline,
    HeartFilled,
    Download,
    Folder,
    FolderPlus,
    PlusSquare,
    Plus,
    VideoCamera,
    History,
    Globe,
    Settings,
    Sliders,
    Play,
    Pause,
    SkipPrevious,
    SkipNext,
    VolumeUp,
    VolumeOff,
    Fullscreen,
    Refresh,
    EditPen,
    Trash,
    Lock,
    Check,
    CheckCircle,
    ShieldCheck,
    Link,
    Microphone,
    TabsStack,
    SortArrows,
    Info,
    Bell,
    Document,
    SignOut,
    Database,
    RotateScreen,
    Subtitles,
    Sparkles
}

@Composable
fun JoyTubeSvgIcon(
    type: JoyTubeIconType,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = JoyTubeColors.TextPrimary,
    strokeWidthDp: Dp = 1.75.dp
) {
    Spacer(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val w = this.size.width
                val s = w / 24f
                val stroke = Stroke(
                    width = strokeWidthDp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )

                // Pre-build Paths once per icon instance in drawWithCache
                var cachedPath1: Path? = null
                var cachedPath2: Path? = null

                when (type) {
                    JoyTubeIconType.BrandLogoLeftTriangle -> {
                        cachedPath1 = Path().apply {
                            moveTo(4.5f * s, 12f * s)
                            lineTo(18.5f * s, 3.8f * s)
                            lineTo(18.5f * s, 20.2f * s)
                            close()
                        }
                    }

                    JoyTubeIconType.NavHome -> {
                        cachedPath1 = Path().apply {
                            moveTo(4f * s, 10.5f * s)
                            lineTo(12f * s, 3.8f * s)
                            lineTo(20f * s, 10.5f * s)
                            lineTo(20f * s, 19.5f * s)
                            quadraticTo(20f * s, 20.5f * s, 19f * s, 20.5f * s)
                            lineTo(5f * s, 20.5f * s)
                            quadraticTo(4f * s, 20.5f * s, 4f * s, 19.5f * s)
                            close()
                            moveTo(9.5f * s, 20.5f * s)
                            lineTo(9.5f * s, 14f * s)
                            lineTo(14.5f * s, 14f * s)
                            lineTo(14.5f * s, 20.5f * s)
                        }
                    }

                    JoyTubeIconType.NavShorts -> {
                        cachedPath1 = Path().apply {
                            moveTo(10.5f * s, 12.2f * s)
                            lineTo(14.5f * s, 14.5f * s)
                            lineTo(10.5f * s, 16.8f * s)
                            close()
                        }
                    }

                    JoyTubeIconType.NavAccount -> {
                        cachedPath1 = Path().apply {
                            moveTo(5.2f * s, 19.8f * s)
                            cubicTo(5.8f * s, 15.2f * s, 18.2f * s, 15.2f * s, 18.8f * s, 19.8f * s)
                        }
                    }

                    JoyTubeIconType.ArrowBack -> {
                        cachedPath1 = Path().apply {
                            moveTo(11f * s, 6f * s)
                            lineTo(5f * s, 12f * s)
                            lineTo(11f * s, 18f * s)
                        }
                    }

                    JoyTubeIconType.ArrowForward -> {
                        cachedPath1 = Path().apply {
                            moveTo(13f * s, 6f * s)
                            lineTo(19f * s, 12f * s)
                            lineTo(13f * s, 18f * s)
                        }
                    }

                    JoyTubeIconType.ArrowUpLeft -> {
                        cachedPath1 = Path().apply {
                            moveTo(15f * s, 7f * s)
                            lineTo(7f * s, 7f * s)
                            lineTo(7f * s, 15f * s)
                        }
                    }

                    JoyTubeIconType.ChevronRight -> {
                        cachedPath1 = Path().apply {
                            moveTo(9.5f * s, 6.5f * s)
                            lineTo(15f * s, 12f * s)
                            lineTo(9.5f * s, 17.5f * s)
                        }
                    }

                    JoyTubeIconType.ChevronDown -> {
                        cachedPath1 = Path().apply {
                            moveTo(6.5f * s, 9.5f * s)
                            lineTo(12f * s, 15f * s)
                            lineTo(17.5f * s, 9.5f * s)
                        }
                    }

                    JoyTubeIconType.Bookmark, JoyTubeIconType.BookmarkFilled -> {
                        cachedPath1 = Path().apply {
                            moveTo(6.5f * s, 4.5f * s)
                            lineTo(17.5f * s, 4.5f * s)
                            lineTo(17.5f * s, 19.5f * s)
                            lineTo(12f * s, 15.5f * s)
                            lineTo(6.5f * s, 19.5f * s)
                            close()
                        }
                    }

                    JoyTubeIconType.HeartOutline, JoyTubeIconType.HeartFilled -> {
                        cachedPath1 = Path().apply {
                            moveTo(12f * s, 19.5f * s)
                            cubicTo(5f * s, 14.8f * s, 3.5f * s, 10.5f * s, 3.5f * s, 8.2f * s)
                            cubicTo(3.5f * s, 5.5f * s, 5.6f * s, 4f * s, 8f * s, 4f * s)
                            cubicTo(9.7f * s, 4f * s, 11.1f * s, 4.9f * s, 12f * s, 6.2f * s)
                            cubicTo(12.9f * s, 4.9f * s, 14.3f * s, 4f * s, 16f * s, 4f * s)
                            cubicTo(18.4f * s, 4f * s, 20.5f * s, 5.5f * s, 20.5f * s, 8.2f * s)
                            cubicTo(20.5f * s, 10.5f * s, 19f * s, 14.8f * s, 12f * s, 19.5f * s)
                            close()
                        }
                    }

                    JoyTubeIconType.Download -> {
                        cachedPath1 = Path().apply {
                            moveTo(8f * s, 11.5f * s)
                            lineTo(12f * s, 15.5f * s)
                            lineTo(16f * s, 11.5f * s)
                        }
                        cachedPath2 = Path().apply {
                            moveTo(5f * s, 17f * s)
                            lineTo(5f * s, 19.5f * s)
                            lineTo(19f * s, 19.5f * s)
                            lineTo(19f * s, 17f * s)
                        }
                    }

                    JoyTubeIconType.Folder, JoyTubeIconType.FolderPlus -> {
                        cachedPath1 = Path().apply {
                            moveTo(3.5f * s, 6.5f * s)
                            lineTo(9.5f * s, 6.5f * s)
                            lineTo(11.5f * s, 8.8f * s)
                            lineTo(20.5f * s, 8.8f * s)
                            lineTo(20.5f * s, 18.5f * s)
                            lineTo(3.5f * s, 18.5f * s)
                            close()
                        }
                    }

                    JoyTubeIconType.VideoCamera -> {
                        cachedPath1 = Path().apply {
                            moveTo(15f * s, 10.2f * s)
                            lineTo(20.2f * s, 7.5f * s)
                            lineTo(20.2f * s, 16.5f * s)
                            lineTo(15f * s, 13.8f * s)
                            close()
                        }
                    }

                    JoyTubeIconType.History -> {
                        cachedPath1 = Path().apply {
                            moveTo(4.5f * s, 5.5f * s)
                            lineTo(4.5f * s, 9.5f * s)
                            lineTo(8.5f * s, 9.5f * s)
                        }
                        cachedPath2 = Path().apply {
                            moveTo(12f * s, 8.2f * s)
                            lineTo(12f * s, 12.2f * s)
                            lineTo(15f * s, 14f * s)
                        }
                    }

                    JoyTubeIconType.Settings -> {
                        cachedPath1 = Path().apply {
                            for (angle in 0 until 360 step 45) {
                                val rad = Math.toRadians(angle.toDouble())
                                val cosA = kotlin.math.cos(rad).toFloat()
                                val sinA = kotlin.math.sin(rad).toFloat()
                                moveTo(12f * s + 7.4f * s * cosA, 12f * s + 7.4f * s * sinA)
                                lineTo(12f * s + 9.2f * s * cosA, 12f * s + 9.2f * s * sinA)
                            }
                        }
                    }

                    JoyTubeIconType.Play -> {
                        cachedPath1 = Path().apply {
                            moveTo(8f * s, 5.5f * s)
                            lineTo(18f * s, 12f * s)
                            lineTo(8f * s, 18.5f * s)
                            close()
                        }
                    }

                    JoyTubeIconType.SkipPrevious -> {
                        cachedPath1 = Path().apply {
                            moveTo(17.5f * s, 6.5f * s)
                            lineTo(9.5f * s, 12f * s)
                            lineTo(17.5f * s, 17.5f * s)
                            close()
                        }
                    }

                    JoyTubeIconType.SkipNext -> {
                        cachedPath1 = Path().apply {
                            moveTo(6.5f * s, 6.5f * s)
                            lineTo(14.5f * s, 12f * s)
                            lineTo(6.5f * s, 17.5f * s)
                            close()
                        }
                    }

                    JoyTubeIconType.VolumeUp, JoyTubeIconType.VolumeOff -> {
                        cachedPath1 = Path().apply {
                            moveTo(5f * s, 9.5f * s)
                            lineTo(8.5f * s, 9.5f * s)
                            lineTo(13f * s, 5.5f * s)
                            lineTo(13f * s, 18.5f * s)
                            lineTo(8.5f * s, 14.5f * s)
                            lineTo(5f * s, 14.5f * s)
                            close()
                        }
                    }

                    JoyTubeIconType.Fullscreen -> {
                        cachedPath1 = Path().apply {
                            moveTo(4.5f * s, 9f * s); lineTo(4.5f * s, 4.5f * s); lineTo(9f * s, 4.5f * s)
                            moveTo(15f * s, 4.5f * s); lineTo(19.5f * s, 4.5f * s); lineTo(19.5f * s, 9f * s)
                            moveTo(19.5f * s, 15f * s); lineTo(19.5f * s, 19.5f * s); lineTo(15f * s, 19.5f * s)
                            moveTo(9f * s, 19.5f * s); lineTo(4.5f * s, 19.5f * s); lineTo(4.5f * s, 15f * s)
                        }
                    }

                    JoyTubeIconType.Refresh, JoyTubeIconType.RotateScreen -> {
                        cachedPath1 = Path().apply {
                            moveTo(15.5f * s, 4.5f * s)
                            lineTo(19f * s, 8f * s)
                            lineTo(15f * s, 9.5f * s)
                        }
                    }

                    JoyTubeIconType.EditPen -> {
                        cachedPath1 = Path().apply {
                            moveTo(4.5f * s, 19.5f * s)
                            lineTo(8.5f * s, 18.5f * s)
                            lineTo(18.5f * s, 8.5f * s)
                            lineTo(15.5f * s, 5.5f * s)
                            lineTo(5.5f * s, 15.5f * s)
                            close()
                        }
                    }

                    JoyTubeIconType.Check -> {
                        cachedPath1 = Path().apply {
                            moveTo(5.5f * s, 12.5f * s)
                            lineTo(10f * s, 17f * s)
                            lineTo(18.5f * s, 7.5f * s)
                        }
                    }

                    JoyTubeIconType.CheckCircle, JoyTubeIconType.ShieldCheck -> {
                        cachedPath1 = Path().apply {
                            moveTo(8.5f * s, 12.2f * s)
                            lineTo(11f * s, 14.8f * s)
                            lineTo(15.8f * s, 9.5f * s)
                        }
                    }

                    JoyTubeIconType.TabsStack -> {
                        cachedPath1 = Path().apply {
                            moveTo(7.5f * s, 4.5f * s)
                            lineTo(19.5f * s, 4.5f * s)
                            lineTo(19.5f * s, 15.5f * s)
                        }
                    }

                    JoyTubeIconType.SortArrows -> {
                        cachedPath1 = Path().apply {
                            moveTo(5.5f * s, 8f * s); lineTo(8.5f * s, 5f * s); lineTo(11.5f * s, 8f * s)
                            moveTo(12.5f * s, 16f * s); lineTo(15.5f * s, 19f * s); lineTo(18.5f * s, 16f * s)
                        }
                    }

                    JoyTubeIconType.Bell -> {
                        cachedPath1 = Path().apply {
                            moveTo(6.5f * s, 16f * s)
                            lineTo(17.5f * s, 16f * s)
                            lineTo(16f * s, 10f * s)
                            cubicTo(16f * s, 7f * s, 14.2f * s, 5f * s, 12f * s, 5f * s)
                            cubicTo(9.8f * s, 5f * s, 8f * s, 7f * s, 8f * s, 10f * s)
                            close()
                        }
                    }

                    JoyTubeIconType.SignOut -> {
                        cachedPath1 = Path().apply {
                            moveTo(13f * s, 4.5f * s)
                            lineTo(5.5f * s, 4.5f * s)
                            lineTo(5.5f * s, 19.5f * s)
                            lineTo(13f * s, 19.5f * s)
                        }
                        cachedPath2 = Path().apply {
                            moveTo(16f * s, 8.5f * s); lineTo(19.5f * s, 12f * s); lineTo(16f * s, 15.5f * s)
                        }
                    }

                    JoyTubeIconType.Sparkles -> {
                        cachedPath1 = Path().apply {
                            moveTo(12f * s, 4f * s)
                            quadraticTo(12f * s, 12f * s, 20f * s, 12f * s)
                            quadraticTo(12f * s, 12f * s, 12f * s, 20f * s)
                            quadraticTo(12f * s, 12f * s, 4f * s, 12f * s)
                            quadraticTo(12f * s, 12f * s, 12f * s, 4f * s)
                            close()
                        }
                    }

                    else -> {}
                }

                onDrawBehind {
                    when (type) {
                        JoyTubeIconType.BrandLogoLeftTriangle,
                        JoyTubeIconType.BookmarkFilled,
                        JoyTubeIconType.HeartFilled -> {
                            cachedPath1?.let { drawPath(it, color = tint) }
                        }

                        JoyTubeIconType.NavHome,
                        JoyTubeIconType.ChevronRight,
                        JoyTubeIconType.ChevronDown,
                        JoyTubeIconType.Bookmark,
                        JoyTubeIconType.HeartOutline,
                        JoyTubeIconType.Folder,
                        JoyTubeIconType.Play,
                        JoyTubeIconType.Fullscreen,
                        JoyTubeIconType.EditPen,
                        JoyTubeIconType.Check,
                        JoyTubeIconType.Sparkles -> {
                            cachedPath1?.let { drawPath(it, color = tint, style = stroke) }
                        }

                        JoyTubeIconType.NavShorts -> {
                            drawRoundRect(
                                color = tint,
                                topLeft = Offset(4f * s, 4.5f * s),
                                size = Size(16f * s, 15f * s),
                                cornerRadius = CornerRadius(3f * s, 3f * s),
                                style = stroke
                            )
                            drawLine(tint, Offset(4f * s, 9.5f * s), Offset(20f * s, 9.5f * s), stroke.width)
                            drawLine(tint, Offset(8.5f * s, 4.5f * s), Offset(10.5f * s, 9.5f * s), stroke.width)
                            drawLine(tint, Offset(14f * s, 4.5f * s), Offset(16f * s, 9.5f * s), stroke.width)
                            cachedPath1?.let { drawPath(it, color = tint) }
                        }

                        JoyTubeIconType.NavSearch -> {
                            drawCircle(tint, 6.5f * s, Offset(11f * s, 11f * s), style = stroke)
                            drawLine(tint, Offset(15.8f * s, 15.8f * s), Offset(20f * s, 20f * s), stroke.width, StrokeCap.Round)
                        }

                        JoyTubeIconType.NavAccount -> {
                            drawCircle(tint, 3.6f * s, Offset(12f * s, 8.2f * s), style = stroke)
                            cachedPath1?.let { drawPath(it, color = tint, style = stroke) }
                        }

                        JoyTubeIconType.ArrowBack -> {
                            drawLine(tint, Offset(19f * s, 12f * s), Offset(5f * s, 12f * s), stroke.width, StrokeCap.Round)
                            cachedPath1?.let { drawPath(it, tint, style = stroke) }
                        }

                        JoyTubeIconType.ArrowForward -> {
                            drawLine(tint, Offset(5f * s, 12f * s), Offset(19f * s, 12f * s), stroke.width, StrokeCap.Round)
                            cachedPath1?.let { drawPath(it, tint, style = stroke) }
                        }

                        JoyTubeIconType.ArrowUpLeft -> {
                            drawLine(tint, Offset(17f * s, 17f * s), Offset(7f * s, 7f * s), stroke.width, StrokeCap.Round)
                            cachedPath1?.let { drawPath(it, tint, style = stroke) }
                        }

                        JoyTubeIconType.Close -> {
                            drawLine(tint, Offset(6.5f * s, 6.5f * s), Offset(17.5f * s, 17.5f * s), stroke.width, StrokeCap.Round)
                            drawLine(tint, Offset(17.5f * s, 6.5f * s), Offset(6.5f * s, 17.5f * s), stroke.width, StrokeCap.Round)
                        }

                        JoyTubeIconType.MoreHorizontal -> {
                            val r = 1.4f * s
                            drawCircle(tint, r, Offset(6f * s, 12f * s))
                            drawCircle(tint, r, Offset(12f * s, 12f * s))
                            drawCircle(tint, r, Offset(18f * s, 12f * s))
                        }

                        JoyTubeIconType.Download -> {
                            drawLine(tint, Offset(12f * s, 4.5f * s), Offset(12f * s, 15.5f * s), stroke.width, StrokeCap.Round)
                            cachedPath1?.let { drawPath(it, tint, style = stroke) }
                            cachedPath2?.let { drawPath(it, tint, style = stroke) }
                        }

                        JoyTubeIconType.FolderPlus -> {
                            cachedPath1?.let { drawPath(it, tint, style = stroke) }
                            drawLine(tint, Offset(12f * s, 11.5f * s), Offset(12f * s, 15.8f * s), stroke.width, StrokeCap.Round)
                            drawLine(tint, Offset(9.85f * s, 13.65f * s), Offset(14.15f * s, 13.65f * s), stroke.width, StrokeCap.Round)
                        }

                        JoyTubeIconType.PlusSquare -> {
                            drawRoundRect(tint, Offset(4f * s, 4f * s), Size(16f * s, 16f * s), CornerRadius(3.5f * s), style = stroke)
                            drawLine(tint, Offset(12f * s, 8.5f * s), Offset(12f * s, 15.5f * s), stroke.width, StrokeCap.Round)
                            drawLine(tint, Offset(8.5f * s, 12f * s), Offset(15.5f * s, 12f * s), stroke.width, StrokeCap.Round)
                        }

                        JoyTubeIconType.Plus -> {
                            drawLine(tint, Offset(12f * s, 5.5f * s), Offset(12f * s, 18.5f * s), stroke.width, StrokeCap.Round)
                            drawLine(tint, Offset(5.5f * s, 12f * s), Offset(18.5f * s, 12f * s), stroke.width, StrokeCap.Round)
                        }

                        JoyTubeIconType.VideoCamera -> {
                            drawRoundRect(tint, Offset(3.5f * s, 6.5f * s), Size(11.5f * s, 11f * s), CornerRadius(2.5f * s), style = stroke)
                            cachedPath1?.let { drawPath(it, tint, style = stroke) }
                        }

                        JoyTubeIconType.History -> {
                            drawArc(tint, -60f, 300f, false, Offset(4.5f * s, 4.5f * s), Size(15f * s, 15f * s), style = stroke)
                            cachedPath1?.let { drawPath(it, tint, style = stroke) }
                            cachedPath2?.let { drawPath(it, tint, style = stroke) }
                        }

                        JoyTubeIconType.Globe -> {
                            drawCircle(tint, 8f * s, Offset(12f * s, 12f * s), style = stroke)
                            drawLine(tint, Offset(4f * s, 12f * s), Offset(20f * s, 12f * s), stroke.width)
                            drawOval(tint, Offset(8.2f * s, 4f * s), Size(7.6f * s, 16f * s), style = stroke)
                        }

                        JoyTubeIconType.Settings -> {
                            drawCircle(tint, 3.2f * s, Offset(12f * s, 12f * s), style = stroke)
                            drawCircle(tint, 7.6f * s, Offset(12f * s, 12f * s), style = stroke)
                            cachedPath1?.let { drawPath(it, tint, style = stroke) }
                        }

                        JoyTubeIconType.Sliders -> {
                            drawLine(tint, Offset(4f * s, 8f * s), Offset(20f * s, 8f * s), stroke.width, StrokeCap.Round)
                            drawCircle(JoyTubeColors.Background, 2.3f * s, Offset(9f * s, 8f * s))
                            drawCircle(tint, 2.3f * s, Offset(9f * s, 8f * s), style = stroke)
                            drawLine(tint, Offset(4f * s, 16f * s), Offset(20f * s, 16f * s), stroke.width, StrokeCap.Round)
                            drawCircle(JoyTubeColors.Background, 2.3f * s, Offset(15f * s, 16f * s))
                            drawCircle(tint, 2.3f * s, Offset(15f * s, 16f * s), style = stroke)
                        }

                        JoyTubeIconType.Pause -> {
                            drawLine(tint, Offset(9f * s, 6.5f * s), Offset(9f * s, 17.5f * s), stroke.width * 1.2f, StrokeCap.Round)
                            drawLine(tint, Offset(15f * s, 6.5f * s), Offset(15f * s, 17.5f * s), stroke.width * 1.2f, StrokeCap.Round)
                        }

                        JoyTubeIconType.SkipPrevious -> {
                            drawLine(tint, Offset(6.5f * s, 6.5f * s), Offset(6.5f * s, 17.5f * s), stroke.width, StrokeCap.Round)
                            cachedPath1?.let { drawPath(it, tint, style = stroke) }
                        }

                        JoyTubeIconType.SkipNext -> {
                            drawLine(tint, Offset(17.5f * s, 6.5f * s), Offset(17.5f * s, 17.5f * s), stroke.width, StrokeCap.Round)
                            cachedPath1?.let { drawPath(it, tint, style = stroke) }
                        }

                        JoyTubeIconType.VolumeUp, JoyTubeIconType.VolumeOff -> {
                            cachedPath1?.let { drawPath(it, tint, style = stroke) }
                            if (type == JoyTubeIconType.VolumeUp) {
                                drawArc(tint, -45f, 90f, false, Offset(12f * s, 8f * s), Size(6f * s, 8f * s), style = stroke)
                            } else {
                                drawLine(tint, Offset(16f * s, 9.5f * s), Offset(20f * s, 14.5f * s), stroke.width, StrokeCap.Round)
                                drawLine(tint, Offset(20f * s, 9.5f * s), Offset(16f * s, 14.5f * s), stroke.width, StrokeCap.Round)
                            }
                        }

                        JoyTubeIconType.Refresh, JoyTubeIconType.RotateScreen -> {
                            drawArc(tint, -30f, 280f, false, Offset(5f * s, 5f * s), Size(14f * s, 14f * s), style = stroke)
                            cachedPath1?.let { drawPath(it, tint, style = stroke) }
                        }

                        JoyTubeIconType.Trash -> {
                            drawLine(tint, Offset(4.5f * s, 7f * s), Offset(19.5f * s, 7f * s), stroke.width, StrokeCap.Round)
                            drawLine(tint, Offset(9.5f * s, 4.5f * s), Offset(14.5f * s, 4.5f * s), stroke.width, StrokeCap.Round)
                            drawRoundRect(tint, Offset(6.5f * s, 7f * s), Size(11f * s, 12.5f * s), CornerRadius(2f * s), style = stroke)
                            drawLine(tint, Offset(10f * s, 10.5f * s), Offset(10f * s, 16f * s), stroke.width, StrokeCap.Round)
                            drawLine(tint, Offset(14f * s, 10.5f * s), Offset(14f * s, 16f * s), stroke.width, StrokeCap.Round)
                        }

                        JoyTubeIconType.Lock -> {
                            drawRoundRect(tint, Offset(5.5f * s, 11f * s), Size(13f * s, 9f * s), CornerRadius(2.5f * s), style = stroke)
                            drawArc(tint, 180f, 180f, false, Offset(8f * s, 5f * s), Size(8f * s, 10f * s), style = stroke)
                        }

                        JoyTubeIconType.CheckCircle, JoyTubeIconType.ShieldCheck -> {
                            drawCircle(tint, 8f * s, Offset(12f * s, 12f * s), style = stroke)
                            cachedPath1?.let { drawPath(it, tint, style = stroke) }
                        }

                        JoyTubeIconType.Link -> {
                            drawArc(tint, 135f, 180f, false, Offset(4.5f * s, 7.5f * s), Size(9f * s, 9f * s), style = stroke)
                            drawArc(tint, -45f, 180f, false, Offset(10.5f * s, 7.5f * s), Size(9f * s, 9f * s), style = stroke)
                            drawLine(tint, Offset(9f * s, 15f * s), Offset(15f * s, 9f * s), stroke.width, StrokeCap.Round)
                        }

                        JoyTubeIconType.Microphone -> {
                            drawRoundRect(tint, Offset(9f * s, 4f * s), Size(6f * s, 10f * s), CornerRadius(3f * s), style = stroke)
                            drawArc(tint, 0f, 180f, false, Offset(6.5f * s, 8f * s), Size(11f * s, 9f * s), style = stroke)
                            drawLine(tint, Offset(12f * s, 17f * s), Offset(12f * s, 20f * s), stroke.width, StrokeCap.Round)
                        }

                        JoyTubeIconType.TabsStack -> {
                            drawRoundRect(tint, Offset(4.5f * s, 7f * s), Size(13f * s, 12f * s), CornerRadius(2.5f * s), style = stroke)
                            cachedPath1?.let { drawPath(it, tint, style = stroke) }
                        }

                        JoyTubeIconType.SortArrows -> {
                            drawLine(tint, Offset(8.5f * s, 5f * s), Offset(8.5f * s, 19f * s), stroke.width, StrokeCap.Round)
                            drawLine(tint, Offset(15.5f * s, 5f * s), Offset(15.5f * s, 19f * s), stroke.width, StrokeCap.Round)
                            cachedPath1?.let { drawPath(it, tint, style = stroke) }
                        }

                        JoyTubeIconType.Info -> {
                            drawCircle(tint, 8f * s, Offset(12f * s, 12f * s), style = stroke)
                            drawLine(tint, Offset(12f * s, 11f * s), Offset(12f * s, 16f * s), stroke.width, StrokeCap.Round)
                            drawCircle(tint, 1f * s, Offset(12f * s, 8f * s))
                        }

                        JoyTubeIconType.Bell -> {
                            cachedPath1?.let { drawPath(it, tint, style = stroke) }
                            drawArc(tint, 0f, 180f, false, Offset(10f * s, 16f * s), Size(4f * s, 3.5f * s), style = stroke)
                        }

                        JoyTubeIconType.Document -> {
                            drawRoundRect(tint, Offset(5.5f * s, 4f * s), Size(13f * s, 16f * s), CornerRadius(2f * s), style = stroke)
                            drawLine(tint, Offset(8.5f * s, 9f * s), Offset(15.5f * s, 9f * s), stroke.width, StrokeCap.Round)
                            drawLine(tint, Offset(8.5f * s, 13f * s), Offset(15.5f * s, 13f * s), stroke.width, StrokeCap.Round)
                        }

                        JoyTubeIconType.SignOut -> {
                            cachedPath1?.let { drawPath(it, tint, style = stroke) }
                            drawLine(tint, Offset(10f * s, 12f * s), Offset(19.5f * s, 12f * s), stroke.width, StrokeCap.Round)
                            cachedPath2?.let { drawPath(it, tint, style = stroke) }
                        }

                        JoyTubeIconType.Database -> {
                            drawOval(tint, Offset(5f * s, 4.5f * s), Size(14f * s, 4.5f * s), style = stroke)
                            drawLine(tint, Offset(5f * s, 6.7f * s), Offset(5f * s, 17.2f * s), stroke.width)
                            drawLine(tint, Offset(19f * s, 6.7f * s), Offset(19f * s, 17.2f * s), stroke.width)
                            drawArc(tint, 0f, 180f, false, Offset(5f * s, 10f * s), Size(14f * s, 4.5f * s), style = stroke)
                            drawArc(tint, 0f, 180f, false, Offset(5f * s, 15f * s), Size(14f * s, 4.5f * s), style = stroke)
                        }

                        JoyTubeIconType.Subtitles -> {
                            drawRoundRect(tint, Offset(3.5f * s, 5.5f * s), Size(17f * s, 13f * s), CornerRadius(2.5f * s), style = stroke)
                            drawLine(tint, Offset(7f * s, 11.5f * s), Offset(11.5f * s, 11.5f * s), stroke.width, StrokeCap.Round)
                            drawLine(tint, Offset(14f * s, 11.5f * s), Offset(17f * s, 11.5f * s), stroke.width, StrokeCap.Round)
                            drawLine(tint, Offset(7f * s, 15f * s), Offset(15.5f * s, 15f * s), stroke.width, StrokeCap.Round)
                        }
                    }
                }
            }
    )
}
