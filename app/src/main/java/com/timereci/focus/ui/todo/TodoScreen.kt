package com.timereci.focus.ui.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timereci.focus.data.PlannedFocusEntity
import com.timereci.focus.ui.components.IconActionButton
import com.timereci.focus.ui.components.grainyBackground
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.MonoFamily
import com.timereci.focus.ui.util.QuickEntry

/**
 * A dedicated page for the todo queue ("오늘 할 집중"): a scrollable list up top, and an
 * add bar pinned to the bottom (like a chat compose bar) so it's always in thumb reach. Typing
 * "빨래 20" and hitting Enter adds "빨래" at 20 minutes directly — no need to tap a preset —
 * and the field stays focused so several items can be queued back-to-back.
 */
@Composable
fun TodoScreen(
    onStartPlanned: (String, Int) -> Unit,
    viewModel: TodoViewModel = hiltViewModel(),
) {
    val planned by viewModel.planned.collectAsStateWithLifecycle()
    val presets by viewModel.durationPresets.collectAsStateWithLifecycle()

    var label by remember { mutableStateOf("") }
    var minutes by remember(presets) { mutableStateOf(presets.getOrElse(1) { 25 }) }
    var showCustomMinutes by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val (previewLabel, previewMinutes) = remember(label) { QuickEntry.parse(label) }

    fun submit() {
        val finalLabel = previewLabel.ifBlank { label.trim() }
        if (finalLabel.isNotEmpty()) {
            viewModel.add(finalLabel, previewMinutes ?: minutes)
            label = ""
        }
        keyboard?.show()
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .grainyBackground(
                base = FocusColors.BaseLight,
                blob = FocusColors.AccentDeep.copy(alpha = 0.12f),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("할 일", color = FocusColors.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("${planned.size}개", color = FocusColors.Muted, fontFamily = MonoFamily, fontSize = 12.sp)
        }

        if (planned.isEmpty()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "아직 할 일이 없어요.\n아래에 적고 엔터를 누르면 바로 쌓여요.\n\"빨래 20\"처럼 뒤에 숫자를 붙이면 분까지 한 번에 설정돼요.",
                    color = FocusColors.Muted,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            ) {
                items(planned, key = { it.id }) { item ->
                    TodoRow(
                        item = item,
                        onStart = {
                            viewModel.remove(item.id)
                            onStartPlanned(item.label, (item.plannedMs / 60_000L).toInt().coerceAtLeast(1))
                        },
                        onDelete = { viewModel.remove(item.id) },
                    )
                }
            }
        }

        // Add bar, pinned to the bottom and lifted above the keyboard while typing.
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 100.dp),
        ) {
            if (previewMinutes != null) {
                Text(
                    "→ ${previewLabel.ifBlank { "집중" }} · ${previewMinutes}분",
                    color = FocusColors.AccentBlue,
                    fontFamily = MonoFamily,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { m ->
                    val active = m == minutes && previewMinutes == null
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (active) FocusColors.AccentDeep else FocusColors.Mist)
                            .clickable { minutes = m }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            "${m}분",
                            color = if (active) FocusColors.Paper else FocusColors.Ink2,
                            fontFamily = MonoFamily,
                            fontSize = 12.5.sp,
                        )
                    }
                }
                val customActive = previewMinutes == null && minutes !in presets
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (customActive) FocusColors.AccentDeep else FocusColors.Mist)
                        .clickable { showCustomMinutes = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        if (customActive) "${minutes}분" else "직접",
                        color = if (customActive) FocusColors.Paper else FocusColors.Ink2,
                        fontFamily = MonoFamily,
                        fontSize = 12.5.sp,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(FocusColors.Paper)
                        .border(1.dp, FocusColors.LineStrong, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    BasicTextField(
                        value = label,
                        onValueChange = { label = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        cursorBrush = SolidColor(FocusColors.AccentBlue),
                        textStyle = TextStyle(color = FocusColors.Ink, fontSize = 15.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        decorationBox = { inner ->
                            if (label.isEmpty()) Text("무엇에 집중할까요? (예: 빨래 20)", color = FocusColors.Muted2, fontSize = 15.sp)
                            inner()
                        },
                    )
                }
                IconActionButton(
                    icon = Icons.Outlined.Add,
                    contentDescription = "추가",
                    onClick = ::submit,
                    accent = true,
                    size = 48.dp,
                )
            }
        }
    }

    if (showCustomMinutes) {
        CustomMinutesDialog(
            initialMinutes = minutes,
            onDismiss = { showCustomMinutes = false },
            onSave = { m ->
                minutes = m
                showCustomMinutes = false
            },
        )
    }
}

@Composable
private fun CustomMinutesDialog(initialMinutes: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by rememberSaveable(initialMinutes) { mutableStateOf(initialMinutes.toString()) }
    val valid = text.toIntOrNull()?.let { it in 1..300 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("시간 직접 입력", fontWeight = FontWeight.Bold) },
        text = {
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(FocusColors.Mist)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it.filter(Char::isDigit).take(3) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        textStyle = TextStyle(color = FocusColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(FocusColors.AccentBlue),
                        modifier = Modifier.width(50.dp),
                    )
                    Text("분", color = FocusColors.Muted, fontSize = 15.sp)
                }
            }
        },
        confirmButton = {
            IconActionButton(
                icon = Icons.Outlined.Check,
                contentDescription = "저장",
                onClick = { text.toIntOrNull()?.let(onSave) },
                accent = true,
                enabled = valid,
                size = 40.dp,
            )
        },
        dismissButton = {
            IconActionButton(icon = Icons.Outlined.Close, contentDescription = "취소", onClick = onDismiss, size = 40.dp)
        },
    )
}

@Composable
private fun TodoRow(item: PlannedFocusEntity, onStart: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FocusColors.Paper)
            .border(1.dp, FocusColors.LineSoft, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.label.ifBlank { "집중" },
                color = FocusColors.Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${(item.plannedMs / 60_000L).toInt()}분",
                color = FocusColors.Muted,
                fontFamily = MonoFamily,
                fontSize = 12.sp,
            )
        }
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "삭제", tint = FocusColors.Muted2, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(10.dp))
        IconActionButton(
            icon = Icons.Filled.PlayArrow,
            contentDescription = "시작",
            onClick = onStart,
            accent = true,
            size = 38.dp,
        )
    }
}
