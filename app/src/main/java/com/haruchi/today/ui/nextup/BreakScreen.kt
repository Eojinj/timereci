package com.haruchi.today.ui.nextup

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.haruchi.today.ui.i18n.LocalStrings
import com.haruchi.today.ui.theme.FocusColors
import com.haruchi.today.ui.util.Formatters
import kotlinx.coroutines.delay

/**
 * Break — same numerals and progress line as Focus, in green, on the same
 * light ground (no dark "night" screens left in the app). "Add 5 Minutes" extends the
 * countdown in place; "Skip Break" ends it immediately.
 */
@Composable
fun BreakScreen(
    onDone: () -> Unit,
    viewModel: BreakViewModel = hiltViewModel(),
) {
    var totalMs by remember { mutableLongStateOf(viewModel.breakMinutes * 60_000L) }
    var remainingMs by remember { mutableLongStateOf(totalMs) }
    var targetElapsed by remember { mutableLongStateOf(SystemClock.elapsedRealtime() + totalMs) }
    var finished by remember { mutableStateOf(false) }
    val strings = LocalStrings.current

    fun finish() {
        if (finished) return
        finished = true
        onDone()
    }

    LaunchedEffect(targetElapsed) {
        while (true) {
            val left = targetElapsed - SystemClock.elapsedRealtime()
            remainingMs = left.coerceAtLeast(0)
            if (left <= 0L) { finish(); break }
            delay(200)
        }
    }

    val progress = if (totalMs <= 0) 0f else (1f - remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)

    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val brush = Brush.radialGradient(
                    0f to Color(0xFF8FDCC2),
                    0.5f to Color(0xFFC7ECDD),
                    1f to Color(0xFFF7F9FB),
                    center = Offset(size.width / 2f, size.height * 0.35f),
                    radius = size.minDimension * 0.8f,
                )
                onDrawBehind { drawRect(brush) }
            }
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                strings.breakTitle,
                color = FocusColors.Muted,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 20.dp),
            )
            Text(
                Formatters.clock(remainingMs),
                color = FocusColors.Ink,
                fontSize = 62.sp,
                fontWeight = FontWeight.SemiBold,
                style = androidx.compose.ui.text.TextStyle(letterSpacing = (-1).sp, fontFeatureSettings = "tnum"),
            )
            Box(
                Modifier
                    .fillMaxWidth(0.85f)
                    .padding(vertical = 16.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x2E2E4257)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(FocusColors.Success),
                )
            }
            Text(strings.thenNextFocus, color = FocusColors.Muted, fontSize = 14.sp)

            Column(Modifier.fillMaxWidth().padding(top = 40.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xEBFFFFFF))
                        .clickable {
                            totalMs += 5 * 60_000L
                            targetElapsed += 5 * 60_000L
                        },
                    contentAlignment = Alignment.Center,
                ) { Text(strings.addFiveMinutes, color = FocusColors.AccentBlue, fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold) }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(FocusColors.AccentBlue)
                        .clickable { finish() },
                    contentAlignment = Alignment.Center,
                ) { Text(strings.skipBreak, color = Color.White, fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
