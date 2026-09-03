package com.haruchi.today.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.haruchi.today.ui.theme.FocusColors

/**
 * The app's standard circular icon button — used everywhere a text-label button used to be
 * (add, save, discard, confirm, cancel, delete…). `accent` gives the solid deep-navy CTA
 * treatment; otherwise it's a translucent glass chip that adapts to light/dark surfaces via
 * `onDark`.
 */
@Composable
fun IconActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    accent: Boolean = false,
    onDark: Boolean = false,
    enabled: Boolean = true,
) {
    val bg = when {
        accent -> FocusColors.AccentDeep
        onDark -> FocusColors.Glass
        else -> Color(0xB3FFFFFF)
    }
    val fg = when {
        accent -> FocusColors.Paper
        onDark -> FocusColors.NightInk
        else -> FocusColors.Ink2
    }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (enabled) bg else bg.copy(alpha = 0.4f))
            .then(if (onDark && !accent) Modifier.border(1.dp, FocusColors.GlassBorder, CircleShape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = fg, modifier = Modifier.size(size * 0.44f))
    }
}
