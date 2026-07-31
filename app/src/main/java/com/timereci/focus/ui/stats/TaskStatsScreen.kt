package com.timereci.focus.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timereci.focus.ui.model.TrendBar
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.util.Formatters

/**
 * One task's history: how much of it you've done, how it's trended over the last two weeks,
 * and a one-tap way to run it again — reusing the task is what keeps these numbers moving.
 */
@Composable
fun TaskStatsScreen(
    onBack: () -> Unit,
    onStarted: () -> Unit,
    viewModel: TaskStatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val stat = state.stat

    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val brush = Brush.radialGradient(
                    0f to Color(0xFF6FA8DE),
                    0.5f to Color(0xFFBEE0F5),
                    1f to Color(0xFFF7F9FB),
                    center = Offset(size.width / 2f, size.height * 0.1f),
                    radius = size.minDimension * 0.9f,
                )
                onDrawBehind { drawRect(brush) }
            },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = FocusColors.AccentBlue,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onBack)
                            .padding(6.dp)
                            .size(22.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stat?.label ?: "Task",
                        color = FocusColors.Ink,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.03).sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (stat == null) {
                item {
                    Text(
                        "No sessions for this task.",
                        color = FocusColors.Muted,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                    )
                }
                return@LazyColumn
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Metric("Total", Formatters.focusDuration(stat.totalFocusMs), Modifier.weight(1f))
                    Metric("Sessions", "${stat.sessions}", Modifier.weight(1f))
                    Metric("Average", Formatters.focus(stat.averageFocusMs), Modifier.weight(1f))
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Metric("Days done", "${stat.daysActive}", Modifier.weight(1f))
                    Metric("Last time", Formatters.stamp(stat.lastDoneEpoch), Modifier.weight(2f))
                }
            }

            item { SectionLabel("LAST 14 DAYS") }
            item { TrendChart(state.trend) }

            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 18.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(FocusColors.AccentDeep)
                            .clickable { viewModel.startAgain(); onStarted() },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Start again · ${stat.typicalMinutes} min",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.addToToday() }
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Repeat, contentDescription = null, tint = FocusColors.AccentBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Add to Today as repeating", color = FocusColors.AccentBlue, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            item { SectionLabel("SESSIONS") }
            items(state.sessions, key = { it.id }) { session ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xEBFFFFFF))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(session.stamp, color = FocusColors.Ink, fontSize = 14.5.sp)
                        val comment = session.comment
                        if (!comment.isNullOrBlank()) {
                            Text(comment, color = FocusColors.Muted, fontSize = 12.5.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    Text(session.focus, color = FocusColors.AccentDeep, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = FocusColors.Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 24.dp, top = 22.dp, bottom = 8.dp),
    )
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xEBFFFFFF))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(label, color = FocusColors.Muted, fontSize = 11.5.sp)
        Text(value, color = FocusColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
    }
}

/** A plain column chart — each bar is one day, scaled against the busiest day in the window. */
@Composable
private fun TrendChart(trend: List<TrendBar>) {
    val peak = trend.maxOfOrNull { it.focusMs } ?: 0L
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xEBFFFFFF))
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .height(104.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        trend.forEach { bar ->
            // Days with nothing on them still get a visible sliver, so the gap reads as a gap
            // rather than as a missing bar.
            val fraction = if (peak > 0) (bar.focusMs.toFloat() / peak).coerceIn(0f, 1f) else 0f
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(if (bar.focusMs > 0) FocusColors.AccentDeep else FocusColors.Line),
                    )
                }
                Text(
                    "${bar.date.dayOfMonth}",
                    color = FocusColors.Muted2,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
