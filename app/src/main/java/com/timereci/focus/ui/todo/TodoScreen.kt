package com.timereci.focus.ui.todo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timereci.focus.data.PlannedFocusEntity
import com.timereci.focus.data.TaskKey
import com.timereci.focus.ui.components.IconActionButton
import com.timereci.focus.ui.components.SwipeableRow
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.util.Formatters

/**
 * "Today" — one list, one line per task (Merci v5 screen 1). Tap a task to start it right away;
 * "+" (top-right, or the trailing row) opens Quick Start. Swipe a row left for Edit/Delete
 * (screen 11) — the only swipe gesture in the app.
 */
@Composable
fun TodoScreen(
    onOpenQuickStart: () -> Unit,
    onStartedSession: () -> Unit,
    onOpenTaskStats: (String) -> Unit,
    viewModel: TodoViewModel = hiltViewModel(),
) {
    val planned by viewModel.planned.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val summary by viewModel.todaySummary.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<PlannedFocusEntity?>(null) }

    val totalPlannedMinutes = planned.sumOf { (it.plannedMs / 60_000L).toInt().coerceAtLeast(1) }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val brush = Brush.radialGradient(
                        0f to Color(0xFF6FA8DE),
                        0.5f to Color(0xFFBEE0F5),
                        1f to Color(0xFFF7F9FB),
                        center = Offset(size.width / 2f, size.height * 0.1f),
                        radius = size.minDimension * 0.95f,
                    )
                    onDrawBehind { drawRect(brush) }
                },
        )

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Today",
                    color = FocusColors.Ink,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.03).sp,
                )
                Text(
                    if (planned.isEmpty()) "No tasks queued" else "${planned.size} task${if (planned.size == 1) "" else "s"} · ${Formatters.focusDuration(totalPlannedMinutes * 60_000L)} planned",
                    color = FocusColors.Muted,
                    fontSize = 14.5.sp,
                    modifier = Modifier.padding(top = 3.dp, bottom = 18.dp),
                )

                Column(
                    Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xEBFFFFFF)),
                ) {
                    planned.forEachIndexed { index, item ->
                        if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(FocusColors.LineSoft).padding(start = 58.dp))
                        SwipeableRow(
                            cornerRadius = 0.dp,
                            onEdit = { editing = item },
                            onDelete = { viewModel.remove(item.id) },
                        ) {
                            TaskRow(item = item, onStart = { viewModel.startItem(item); onStartedSession() })
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(FocusColors.LineSoft).padding(start = 58.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenQuickStart)
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.AddCircleOutline,
                            contentDescription = null,
                            tint = FocusColors.AccentBlue,
                            modifier = Modifier.width(30.dp).size(22.dp),
                        )
                        Text("New Task", color = FocusColors.AccentBlue, fontSize = 16.5.sp)
                    }
                }

                if (favorites.isNotEmpty()) {
                    Text(
                        "FAVORITES",
                        color = FocusColors.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp).padding(top = 22.dp, bottom = 8.dp),
                    )
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xEBFFFFFF)),
                    ) {
                        favorites.forEachIndexed { index, item ->
                            if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(FocusColors.LineSoft).padding(start = 58.dp))
                            SwipeableRow(
                                cornerRadius = 0.dp,
                                onEdit = { editing = item },
                                onDelete = { viewModel.remove(item.id) },
                            ) {
                                FavoriteRow(
                                    item = item,
                                    onOpenStats = { onOpenTaskStats(TaskKey.of(item.label)) },
                                    onEdit = { editing = item },
                                )
                            }
                        }
                    }
                    Text(
                        "Tap for stats and to start it, press and hold to edit.",
                        color = FocusColors.Muted,
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(horizontal = 4.dp).padding(top = 8.dp),
                    )
                }

                Text(
                    "${summary.sessionCount} session${if (summary.sessionCount == 1) "" else "s"} completed today · ${Formatters.focusDuration(summary.focusedMs)} focused.",
                    color = FocusColors.Muted,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 4.dp).padding(top = 10.dp),
                )

                Spacer(Modifier.height(140.dp))
            }
        }

        // The primary "start something new" action — wide, centered and within thumb's
        // reach at the bottom, instead of a small button stuck in the top-right corner.
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp)
                .padding(bottom = 104.dp)
                .fillMaxWidth()
                .height(60.dp)
                .shadow(12.dp, RoundedCornerShape(30.dp))
                .clip(RoundedCornerShape(30.dp))
                .background(FocusColors.AccentDeep)
                .clickable(onClick = onOpenQuickStart),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text("Quick Start", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    editing?.let { target ->
        EditTaskDialog(
            target = target,
            onDismiss = { editing = null },
            onSave = { label, minutes, isRepeating ->
                viewModel.update(target, label, minutes, isRepeating)
                editing = null
            },
        )
    }
}

@Composable
private fun TaskRow(item: PlannedFocusEntity, onStart: () -> Unit) {
    val minutes = (item.plannedMs / 60_000L).toInt().coerceAtLeast(1)
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xEBFFFFFF))
            .clickable(onClick = onStart)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = FocusColors.AccentBlue,
            modifier = Modifier.width(30.dp).size(22.dp),
        )
        Text(
            item.label.ifBlank { "Focus" },
            color = FocusColors.Ink,
            fontSize = 16.5.sp,
            modifier = Modifier.weight(1f).padding(end = 10.dp),
        )
        Text("$minutes min", color = FocusColors.Muted, fontSize = 15.5.sp)
    }
}

/**
 * A repeating task. Tapping opens its stats — that screen is also where you pick a length and
 * start it — and a long press edits it in place, so the row itself stays a single clean line.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteRow(item: PlannedFocusEntity, onOpenStats: () -> Unit, onEdit: () -> Unit) {
    val minutes = (item.plannedMs / 60_000L).toInt().coerceAtLeast(1)
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xEBFFFFFF))
            .combinedClickable(onClick = onOpenStats, onLongClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Repeat,
            contentDescription = null,
            tint = FocusColors.AccentBlue,
            modifier = Modifier.width(30.dp).size(20.dp),
        )
        Text(
            item.label.ifBlank { "Focus" },
            color = FocusColors.Ink,
            fontSize = 16.5.sp,
            modifier = Modifier.weight(1f).padding(end = 10.dp),
        )
        Text("$minutes min", color = FocusColors.Muted, fontSize = 15.5.sp)
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.Outlined.BarChart,
            contentDescription = "Stats",
            tint = FocusColors.Muted2,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun EditTaskDialog(target: PlannedFocusEntity, onDismiss: () -> Unit, onSave: (String, Int, Boolean) -> Unit) {
    var label by remember(target.id) { mutableStateOf(target.label) }
    var minutesText by remember(target.id) {
        mutableStateOf(((target.plannedMs / 60_000L).toInt().coerceAtLeast(1)).toString())
    }
    var isRepeating by remember(target.id) { mutableStateOf(target.isRepeating) }
    fun save() = onSave(label, minutesText.toIntOrNull()?.coerceIn(1, 300) ?: 25, isRepeating)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(FocusColors.Mist)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    BasicTextField(
                        value = label,
                        onValueChange = { label = it },
                        singleLine = true,
                        cursorBrush = SolidColor(FocusColors.AccentBlue),
                        textStyle = TextStyle(color = FocusColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        decorationBox = { inner ->
                            if (label.isEmpty()) Text("Task", color = FocusColors.Muted2, fontSize = 15.sp)
                            inner()
                        },
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = FocusColors.Muted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(FocusColors.Mist)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        BasicTextField(
                            value = minutesText,
                            onValueChange = { minutesText = it.filter(Char::isDigit).take(3) },
                            singleLine = true,
                            cursorBrush = SolidColor(FocusColors.AccentBlue),
                            textStyle = TextStyle(color = FocusColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { save() }),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("min", color = FocusColors.Muted, fontSize = 14.sp)
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { isRepeating = !isRepeating },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Repeat, contentDescription = null, tint = FocusColors.Muted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f).padding(end = 10.dp)) {
                        Text("Repeat", color = FocusColors.Ink, fontSize = 15.sp)
                        Text(
                            "Stays in the list after you start it",
                            color = FocusColors.Muted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                    }
                    Switch(
                        checked = isRepeating,
                        onCheckedChange = { isRepeating = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = FocusColors.AccentBlue,
                            uncheckedTrackColor = FocusColors.Line,
                            uncheckedBorderColor = FocusColors.LineStrong,
                        ),
                    )
                }
            }
        },
        confirmButton = {
            IconActionButton(
                icon = Icons.Outlined.Add,
                contentDescription = "Save",
                onClick = { save() },
                accent = true,
                size = 40.dp,
            )
        },
        dismissButton = {
            IconActionButton(icon = Icons.Outlined.Close, contentDescription = "Cancel", onClick = onDismiss, size = 40.dp)
        },
    )
}
