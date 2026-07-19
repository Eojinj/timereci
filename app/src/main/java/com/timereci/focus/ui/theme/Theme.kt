package com.timereci.focus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val FocusColorScheme = lightColorScheme(
    primary = FocusColors.AccentDeep,
    onPrimary = FocusColors.Paper,
    secondary = FocusColors.AccentBlue,
    onSecondary = FocusColors.Paper,
    background = FocusColors.Paper,
    onBackground = FocusColors.Ink,
    surface = FocusColors.Paper,
    onSurface = FocusColors.Ink,
    surfaceVariant = FocusColors.Mist,
    onSurfaceVariant = FocusColors.Muted,
    outline = FocusColors.LineStrong,
    error = FocusColors.AccentBlue,
)

/**
 * App theme. The design is intentionally a single, hand-tuned light palette (with local
 * "night" surfaces drawn explicitly per screen), so we don't follow the system dark theme.
 */
@Composable
fun TimereciTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FocusColorScheme,
        typography = AppTypography,
        content = content,
    )
}
