package com.haruchi.today.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import com.haruchi.today.ui.i18n.LocalStrings
import com.haruchi.today.ui.i18n.durationOf
import com.haruchi.today.ui.i18n.minutesOf
import com.haruchi.today.ui.model.TaskStat
import com.haruchi.today.ui.theme.FocusColors

/**
 * Stats — what you actually spend your focus on, rolled up per task. Tasks are matched by
 * name, so anything you run more than once (a repeating task especially) accumulates here.
 * Tapping a task opens its own trend.
 */
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    onOpenTask: (String) -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

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
                        contentDescription = strings.back,
                        tint = FocusColors.AccentBlue,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onBack)
                            .padding(6.dp)
                            .size(22.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(strings.statsTitle, color = FocusColors.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.03).sp)
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SummaryCard(strings.allTime, strings.durationOf(state.totalFocusMs), Modifier.weight(1f))
                    SummaryCard(strings.thisWeek, strings.durationOf(state.thisWeekMs), Modifier.weight(1f))
                    SummaryCard(strings.sessionsLabel, "${state.totalSessions}", Modifier.weight(1f))
                }
            }

            if (state.tasks.isEmpty()) {
                item { EmptyStats() }
            } else {
                item {
                    Text(
                        strings.byTask,
                        color = FocusColors.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 24.dp, top = 22.dp, bottom = 8.dp),
                    )
                }
                items(state.tasks, key = { it.key }) { task ->
                    TaskStatRow(
                        task = task,
                        topTaskMs = state.topTaskMs,
                        onClick = { onOpenTask(task.key) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xEBFFFFFF))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(label, color = FocusColors.Muted, fontSize = 11.5.sp)
        Text(value, color = FocusColors.Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun TaskStatRow(task: TaskStat, topTaskMs: Long, onClick: () -> Unit) {
    val strings = LocalStrings.current
    val share = if (topTaskMs > 0) (task.totalFocusMs.toFloat() / topTaskMs).coerceIn(0f, 1f) else 0f
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xEBFFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                task.label,
                color = FocusColors.Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(end = 10.dp),
            )
            Text(
                strings.durationOf(task.totalFocusMs),
                color = FocusColors.AccentDeep,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        // Bar length is relative to the heaviest task, so the list reads as a ranking at a glance.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(FocusColors.Mist),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(share)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(FocusColors.AccentDeep),
            )
        }
        Text(
            strings.taskRowSummary(
                strings.sessionsCount(task.sessions),
                strings.daysCount(task.daysActive),
                strings.minutesOf(task.averageFocusMs),
            ),
            color = FocusColors.Muted,
            fontSize = 12.5.sp,
            modifier = Modifier.padding(top = 7.dp),
        )
    }
}

@Composable
private fun EmptyStats() {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            strings.emptyStats,
            color = FocusColors.Ink2,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
        )
    }
}
