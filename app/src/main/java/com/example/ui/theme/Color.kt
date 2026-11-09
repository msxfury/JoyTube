package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * JOYTUBE DARK COLOUR ARCHITECTURE (PART II — SECTION 04 & SCREENSHOT 10)
 *
 * Pure black. Pure focus.
 * Every surface level has a deliberate relationship with neighboring layers
 * using subtle graphite, charcoal, near-black, and neutral grey steps combined
 * with crisp 1.dp hairlines rather than heavy shadows or saturated accents.
 */
object JoyTubeColors {
    // 1. Core Surface Hierarchy (Explicitly specified in Screenshot 10)
    val Background = Color(0xFF050505)          // Deepest canvas background (#050505)
    val Surface = Color(0xFF101010)             // Primary outlined card & input surface (#101010)
    val SurfaceElevated = Color(0xFF151517)     // Elevated cards, dialogs & bottom sheets
    val SurfaceModal = Color(0xFF0C0C0E)        // Modal & pop-up container surface
    val SurfaceFloatingDock = Color(0xE80E0E10) // Matte-black floating glass dock (91% opacity)
    val Selected = Color(0xFF242424)            // Active dock circle, primary button & selected pill (#242424)
    val InteractivePressed = Color(0xFF2E2E32)  // Pressed state surface
    val InteractiveHover = Color(0xFF1C1C1F)    // Hover / subtle highlight surface

    // 2. Glassmorphism & Specular Layering Tokens (Part II — Section 05)
    val GlassCardTint = Color(0xCC101012)       // 80% neutral graphite tint
    val GlassSheetTint = Color(0xF20D0D0F)      // 95% high-readability modal glass
    val GlassBadgeTint = Color(0xB808080A)      // 72% floating thumbnail pill & bookmark badge
    val InnerHighlightTop = Color(0x2EFFFFFF)   // 18% white top specular rim highlight
    val ScrimOverlay = Color(0xB3000000)        // 70% backdrop dimming for sheets & dialogs

    // 2b. Dynamic Liquid Glass Mode Tokens (Translucent Glass Design setting)
    val LiquidGlassSurfaceTop = Color(0xCC18191E)     // Translucent top-left smoked glass catch-light
    val LiquidGlassSurfaceBottom = Color(0xB3090A0D)  // Translucent bottom-right deep crystal tint
    val LiquidGlassDockTop = Color(0xE014151A)        // Floating dock translucent obsidian top
    val LiquidGlassDockBottom = Color(0xCC07080A)     // Floating dock translucent obsidian base
    val LiquidGlassPillActiveTop = Color(0xEB3A3C44)  // Active pill/button luminous glass top
    val LiquidGlassPillActiveBottom = Color(0xCC1C1D22) // Active pill/button luminous glass bottom
    val LiquidGlassBorderBright = Color(0x80FFFFFF)   // Top-left specular glass rim (50% white)
    val LiquidGlassBorderMid = Color(0x2EFFFFFF)      // Mid glass rim (18% white)
    val LiquidGlassBorderSubtle = Color(0x52FFFFFF)   // Bottom-right glass refraction rim (32% white)

    // 3. Borders & Dividers (Crisp Outlining System)
    val BorderStandard = Color(0xFF34343C)      // Crisp 1.dp white/grey card & control outline
    val BorderGlass = Color(0x42FFFFFF)         // 26% white crisp outer edge for floating dock & modals
    val BorderSubtle = Color(0xFF222228)        // Hairline row divider
    val BorderFocused = Color(0xFFA0A0AC)       // Active input focus & selected card outline
    val BorderActiveWhite = Color(0xCCFFFFFF)   // High-emphasis selected control border

    // 4. Typography & Iconography Hierarchy
    val TextPrimary = Color(0xFFFFFFFF)         // Pure white for primary titles, active icons & emphasis
    val TextSecondary = Color(0xFF9A9AA2)       // Muted neutral grey for supporting text & attribution
    val TextSubdued = Color(0xFF686870)         // Metadata, placeholders, timestamps & helper copy
    val TextDisabled = Color(0xFF45454B)        // Disabled labels & unavailable actions

    // 5. Semantic & Status Tokens (Ad-blocking, validation, downloads)
    val StatusSuccess = Color(0xFF4ADE80)       // "On", "Enabled", "Protected" in Ad-blocking
    val StatusSuccessBg = Color(0x1F4ADE80)     // Subtle status pill backing
    val StatusWarning = Color(0xFFFBBF24)       // Duration warning / offline banner
    val StatusError = Color(0xFFF87171)         // Validation failure / destructive alert
    val StatusErrorBorder = Color(0x66F87171)   // Destructive / invalid field outline
}
