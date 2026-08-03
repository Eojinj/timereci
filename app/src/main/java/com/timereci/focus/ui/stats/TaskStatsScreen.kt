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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timereci.focus.ui.i18n.LocalStrings
import com.timereci.focus.ui.i18n.durationOf
import com.timereci.focus.ui.i18n.minutesOf
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
    val strings = LocalStrings.current

    // The length is asked for on the way out, not parked on the screen: Start opens the prompt,
    // prefilled with this task's usual length so it's usually just Start -> Start.
    var askingLength by rememberSaveable { mutableStateOf(false) }

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
                    Text(
                        state.label.ifBlank { strings.focusFallback },
                        color = FocusColors.Ink,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.03).sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // The start controls come first: this screen is the way into a favorite, so
            // scrolling past a wall of numbers to reach them would be backwards.
            item {
                StartBlock(
                    onStart = { askingLength = true },
                    showAddToToday = !state.isFavorite,
                    onAddToToday = viewModel::addToToday,
                )
            }

            if (stat == null) {
                item {
                    Text(
                        strings.noSessionsYet,
                        color = FocusColors.Muted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 32.dp),
                    )
                }
                return@LazyColumn
            }

            item { SectionLabel(strings.totalsSection) }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Metric(strings.total, strings.durationOf(stat.totalFocusMs), Modifier.weight(1f))
                    Metric(strings.sessionsLabel, "${stat.sessions}", Modifier.weight(1f))
                    Metric(strings.average, strings.minutesOf(stat.averageFocusMs), Modifier.weight(1f))
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Metric(strings.daysDone, strings.daysCount(stat.daysActive), Modifier.weight(1f))
                    Metric(strings.lastTime, Formatters.stamp(stat.lastDoneEpoch), Modifier.weight(2f))
                }
            }

            item { SectionLabel(strings.last14Days) }
            item { TrendChart(state.trend) }

            item { SectionLabel(strings.sessionsSection) }
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

    if (askingLength) {
        LengthDialog(
            taskLabel = state.label.ifBlank { strings.focusFallback },
            defaultMinutes = state.defaultMinutes,
            onDismiss = { askingLength = false },
            onStart = { chosen ->
                askingLength = false
                viewModel.startAgain(chosen)
                onStarted()
            },
        )
    }
}

/**
 * Asked for only once Start is pressed. Prefilled with the task's usual length and focused
 * immediately, so the common case is Start, then Start again.
 */
@Composable
private fun LengthDialog(
    taskLabel: String,
    defaultMinutes: Int,
    onDismiss: () -> Unit,
    onStart: (Int) -> Unit,
) {
    val strings = LocalStrings.current
    var minutesText by remember { mutableStateOf(defaultMinutes.toString()) }
    val minutes = minutesText.toIntOrNull()?.takeIf { it in 1..300 }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.howLong, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(taskLabel, color = FocusColors.Muted, fontSize = 14.sp)
                Row(
                    Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(FocusColors.Mist)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        BasicTextField(
                            value = minutesText,
                            onValueChange = { minutesText = it.filter(Char::isDigit).take(3) },
                            singleLine = true,
                            cursorBrush = SolidColor(FocusColors.AccentBlue),
                            textStyle = TextStyle(color = FocusColors.Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { minutes?.let(onStart) }),
                            modifier = Modifier.width(56.dp).focusRequester(focusRequester),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(strings.minLabel, color = FocusColors.Muted, fontSize = 15.sp)
                }
            }
        },
        confirmButton = {
            Text(
                strings.start,
                color = if (minutes != null) FocusColors.AccentDeep else FocusColors.Muted2,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = minutes != null) { minutes?.let(onStart) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        },
        dismissButton = {
            Text(
                strings.cancel,
                color = FocusColors.AccentBlue,
                fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        },
    )
}

/** Just the button — the length is asked for after it's pressed, in [LengthDialog]. */
@Composable
private fun StartBlock(
    onStart: () -> Unit,
    showAddToToday: Boolean,
    onAddToToday: () -> Unit,
) {
    val strings = LocalStrings.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 14.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(FocusColors.AccentDeep)
                .clickable(onClick = onStart),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(strings.start, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }

        if (showAddToToday) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onAddToToday)
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Repeat, contentDescription = null, tint = FocusColors.AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Text(strings.addToFavorites, color = FocusColors.AccentBlue, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
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
