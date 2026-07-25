package com.timereci.focus.ui.nextup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.timereci.focus.ui.components.IconActionButton
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.GothicFamily
import com.timereci.focus.ui.theme.MonoFamily

private val BREAK_PRESETS = listOf(5, 10)

/**
 * Shown after a session ends when the todo queue still has something in it: continue straight
 * into the next task, take a short break first, or leave it for later (queue untouched either
 * way unless actually started). Same soft blue-to-white gradient as the timer screen instead of
 * a flat wash, and "나중에" lives as a close button in the corner rather than floating alone at
 * the bottom.
 */
@Composable
fun NextUpScreen(
    plannedId: Long,
    task: String,
    minutes: Int,
    onContinueNow: () -> Unit,
    onBreak: (breakMinutes: Int) -> Unit,
    onSkip: () -> Unit,
    viewModel: NextUpViewModel = hiltViewModel(),
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    0f to Color(0xFF6FA8DE),
                    0.5f to Color(0xFFBEE0F5),
                    1f to Color(0xFFFFFFFF),
                ),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        IconActionButton(
            icon = Icons.Outlined.Close,
            contentDescription = "나중에",
            onClick = onSkip,
            size = 42.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "다음 집중",
                color = FocusColors.Muted,
                fontFamily = MonoFamily,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                task.ifBlank { "집중" },
                color = FocusColors.Ink,
                fontFamily = GothicFamily,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 40.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCCFFFFFF))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text("${minutes}분", color = FocusColors.Ink2, fontFamily = MonoFamily, fontSize = 13.sp)
            }

            Spacer(Modifier.height(40.dp))

            IconActionButton(
                icon = Icons.Filled.PlayArrow,
                contentDescription = "바로 시작",
                onClick = {
                    viewModel.consume(plannedId)
                    onContinueNow()
                },
                accent = true,
                size = 84.dp,
            )

            Spacer(Modifier.height(28.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BREAK_PRESETS.forEach { m ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xCCFFFFFF))
                            .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(20.dp))
                            .clickable {
                                viewModel.consume(plannedId)
                                onBreak(m)
                            }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                    ) {
                        Text("${m}분 휴식", color = FocusColors.Ink2, fontFamily = MonoFamily, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}
