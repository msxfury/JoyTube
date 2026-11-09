package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * JOYTUBE TYPOGRAPHY ARCHITECTURE (PART III — SECTION 08)
 *
 * Uses locally bundled Google Fonts (Inter + JetBrains Mono) with explicit
 * hierarchy across all 13 documented typography roles:
 * Display, PageTitle, SectionHeading, SubsectionHeading, CardTitle, BodyText,
 * SupportingText, Metadata, Labels, NavigationText, ButtonText, InputText, Microcopy.
 */
val InterFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium)
)

object JoyTubeTypeScale {
    // 1. Display Style — Hero brand statements ("Pure black. Pure focus.")
    val Display = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
        color = JoyTubeColors.TextPrimary
    )

    // 2. Page Title — Primary screen titles ("For you", "Favourites", "Downloads", "History")
    val PageTitle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
        color = JoyTubeColors.TextPrimary
    )

    // 3. Brand Wordmark — "JoyTube" in top bar alongside left-facing triangle
    val BrandWordmark = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.3).sp,
        color = JoyTubeColors.TextPrimary
    )

    // 4. Section Heading — Major section dividers ("Continue watching", "Recent searches", "Quick access")
    val SectionHeading = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
        color = JoyTubeColors.TextPrimary
    )

    // 5. Subsection Heading — Settings group headers & modal headers ("Playback", "Browser", "Video options")
    val SubsectionHeading = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp,
        color = JoyTubeColors.TextPrimary
    )

    // 6. Card Title — Media & category titles ("The City That Never Sleeps", "Gaming Collection")
    val CardTitle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp,
        color = JoyTubeColors.TextPrimary
    )

    // 7. Body Text — Standard list items, menu options & descriptions
    val BodyText = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        color = JoyTubeColors.TextPrimary
    )

    // 8. Supporting Text — Secondary explanations & dialog descriptions
    val SupportingText = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
        color = JoyTubeColors.TextSecondary
    )

    // 9. Metadata — Attribution ("Imported by @username", "Created by @username", "1080p · 245 MB")
    val Metadata = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
        color = JoyTubeColors.TextSecondary
    )

    // 10. Labels — Filter pills, visibility badges ("Public", "Private"), form field headers
    val Label = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
        color = JoyTubeColors.TextPrimary
    )

    // 11. Button Text — Primary, secondary & action bar button labels
    val ButtonText = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
        color = JoyTubeColors.TextPrimary
    )

    // 12. Input Text — Search, URL & form inputs
    val InputText = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        color = JoyTubeColors.TextPrimary
    )

    // 13. Microcopy — Timestamps ("02:34", "12:48"), source domain chips ("rawpixel.com"), hex codes
    val Microcopy = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp,
        color = JoyTubeColors.TextSecondary
    )

    val MonoToken = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = JoyTubeColors.TextPrimary
    )
}

val Typography = Typography(
    displayLarge = JoyTubeTypeScale.Display,
    headlineMedium = JoyTubeTypeScale.PageTitle,
    titleLarge = JoyTubeTypeScale.BrandWordmark,
    titleMedium = JoyTubeTypeScale.SectionHeading,
    titleSmall = JoyTubeTypeScale.CardTitle,
    bodyLarge = JoyTubeTypeScale.BodyText,
    bodyMedium = JoyTubeTypeScale.SupportingText,
    bodySmall = JoyTubeTypeScale.Metadata,
    labelLarge = JoyTubeTypeScale.ButtonText,
    labelMedium = JoyTubeTypeScale.Label,
    labelSmall = JoyTubeTypeScale.Microcopy
)
