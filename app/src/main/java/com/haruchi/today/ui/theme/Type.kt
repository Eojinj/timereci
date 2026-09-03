package com.haruchi.today.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.haruchi.today.R

/**
 * Pretendard — the gothic (sans-serif) family the design was drawn in, bundled as OTF in
 * res/font so it renders identically on every device (no Play Services / network needed).
 * This is the app's default text family; [MonoFamily] is used only for the "receipt" style
 * tabular numerals (timer digits, stamps).
 */
val GothicFamily: FontFamily = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

val MonoFamily: FontFamily = FontFamily.Monospace

private val base = Typography()

/** Material typography with every style locked to the gothic family. */
val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = GothicFamily),
    displayMedium = base.displayMedium.copy(fontFamily = GothicFamily),
    displaySmall = base.displaySmall.copy(fontFamily = GothicFamily),
    headlineLarge = base.headlineLarge.copy(fontFamily = GothicFamily),
    headlineMedium = base.headlineMedium.copy(fontFamily = GothicFamily),
    headlineSmall = base.headlineSmall.copy(fontFamily = GothicFamily),
    titleLarge = base.titleLarge.copy(fontFamily = GothicFamily),
    titleMedium = base.titleMedium.copy(fontFamily = GothicFamily),
    titleSmall = base.titleSmall.copy(fontFamily = GothicFamily),
    bodyLarge = base.bodyLarge.copy(fontFamily = GothicFamily),
    bodyMedium = base.bodyMedium.copy(fontFamily = GothicFamily),
    bodySmall = base.bodySmall.copy(fontFamily = GothicFamily),
    labelLarge = base.labelLarge.copy(fontFamily = GothicFamily),
    labelMedium = base.labelMedium.copy(fontFamily = GothicFamily),
    labelSmall = base.labelSmall.copy(fontFamily = GothicFamily),
)

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
