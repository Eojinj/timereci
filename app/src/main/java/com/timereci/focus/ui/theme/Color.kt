package com.timereci.focus.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens lifted directly from the handoff prototype (집중영수증.dc.html).
 * A calm blue-grey paper palette for light surfaces and a deep navy for the "night"
 * surfaces (day view, publish) where photos take over.
 */
object FocusColors {
    // Light / paper
    val Paper = Color(0xFFF7FBFE)
    val PageTop = Color(0xFFF2F8FD)
    val PageMid = Color(0xFFE4F0FA)
    val PageBottom = Color(0xFFDBEAF6)
    val Mist = Color(0xFFE9F3FB)
    val ExportBg = Color(0xFFEEF6FC)

    // Value-anchored base: near-neutral off-white ground for a monochrome canvas.
    val BaseLight = Color(0xFFF5F6F8)

    // Ink
    val Ink = Color(0xFF2E4257)
    val Ink2 = Color(0xFF3A5068)
    val InkSoft = Color(0xFF54697F)
    val Muted = Color(0xFF6E8196)
    val Muted2 = Color(0xFF8CA0B4)

    // Lines
    val Line = Color(0xFFDCEAF4)
    val LineSoft = Color(0xFFD9E7F2)
    val LineStrong = Color(0xFFC7DDEE)

    // Accents
    val AccentDeep = Color(0xFF22364A)
    val AccentInk = Color(0xFF2E4257)
    val AccentBlue = Color(0xFF3E6A94)
    val AccentSky = Color(0xFF99C3E4)

    // Night surfaces
    val Night = Color(0xFF0F1B26)
    val Night2 = Color(0xFF12222F)
    val NightInk = Color(0xFFEAF2F8)
    val NightMuted = Color(0xFF9DB6CC)
    val Glass = Color(0x14FFFFFF)        // rgba(255,255,255,0.08)
    val GlassBorder = Color(0x2EFFFFFF)  // rgba(255,255,255,0.18)
}
