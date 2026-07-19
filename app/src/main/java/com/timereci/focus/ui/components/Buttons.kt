package com.timereci.focus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timereci.focus.ui.theme.FocusColors

/** Filled dark action button — the primary CTA across screens ("집중 시작", "피드에 담기"). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = FocusColors.AccentInk,
    content: Color = FocusColors.Paper,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (enabled) container else container.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = content, fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
    }
}

/** Outlined / soft secondary button used for "코멘트", "사진 추가" etc. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
) {
    val border = if (onDark) FocusColors.GlassBorder else FocusColors.LineStrong
    val bg = if (onDark) FocusColors.Glass else Color.White
    val fg = if (onDark) FocusColors.NightInk else FocusColors.Ink2
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
    }
}
