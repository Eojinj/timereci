package com.timereci.focus.ui.nextup

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timereci.focus.ui.components.IconActionButton
import com.timereci.focus.ui.components.grain
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.GothicFamily
import com.timereci.focus.ui.theme.MonoFamily
import com.timereci.focus.ui.util.Formatters
import kotlinx.coroutines.delay

/**
 * A short, ephemeral countdown between two queued tasks — deliberately not backed by
 * [com.timereci.focus.timer.FocusTimerController]: breaks aren't focus sessions, don't
 * survive process death, and never produce a receipt.
 */
@Composable
fun BreakScreen(minutes: Int, onDone: () -> Unit) {
    var remainingMs by remember(minutes) { mutableLongStateOf(minutes * 60_000L) }

    LaunchedEffect(minutes) {
        val target = SystemClock.elapsedRealtime() + minutes * 60_000L
        while (true) {
            val left = target - SystemClock.elapsedRealtime()
            remainingMs = left.coerceAtLeast(0)
            if (left <= 0L) break
            delay(200)
        }
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(FocusColors.Night)
            .grain(0.05f)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "휴식",
                color = FocusColors.NightMuted,
                fontFamily = MonoFamily,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                Formatters.clock(remainingMs),
                color = FocusColors.NightInk,
                fontFamily = GothicFamily,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
            )
            Spacer(Modifier.height(36.dp))
            IconActionButton(
                icon = Icons.Outlined.SkipNext,
                contentDescription = "건너뛰기",
                onClick = onDone,
                onDark = true,
                size = 48.dp,
            )
        }
    }
}
