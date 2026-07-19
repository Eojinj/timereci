package com.timereci.focus.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The prototype pairs Pretendard (Korean body) with IBM Plex Mono (numbers, stamps).
 * To stay asset-free and buildable out of the box we approximate: system default for
 * body text, [FontFamily.Monospace] for the "mono" tabular numerals. Dropping the real
 * font files into res/font and pointing [MonoFamily]/[Typography] at them is a drop-in
 * upgrade later.
 */
val MonoFamily: FontFamily = FontFamily.Monospace

val AppTypography = Typography()

/** Extra text styles used across screens for the monospaced "receipt" feel. */
object FocusText {
    val Stamp = TextStyle(
        fontFamily = MonoFamily,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Medium,
    )
    val Label = TextStyle(
        fontFamily = MonoFamily,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
    )
    val TimerDigits = TextStyle(
        fontFamily = MonoFamily,
        fontSize = 96.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 4.sp,
    )
}
