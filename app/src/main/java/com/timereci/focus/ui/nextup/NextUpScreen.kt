package com.timereci.focus.ui.nextup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
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
import com.timereci.focus.data.PlannedFocusEntity
import com.timereci.focus.ui.i18n.LocalStrings
import com.timereci.focus.ui.theme.FocusColors

/**
 * Up Next (Merci v5 screen 6): confirmation, plus one decision with three plainly worded
 * choices. No queue browsing — just whatever is next.
 */
@Composable
fun NextUpScreen(
    onContinueNow: () -> Unit,
    onBreak: (breakMinutes: Int, task: String, minutes: Int) -> Unit,
    onSkip: () -> Unit,
    viewModel: NextUpViewModel = hiltViewModel(),
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val justSaved by viewModel.justSaved.collectAsStateWithLifecycle()
    val current = queue.firstOrNull()
    val waitingCount = (queue.size - 1).coerceAtLeast(0)
    var showBreakInput by remember(current?.id) { mutableStateOf(false) }
    var breakInput by remember(current?.id) { mutableStateOf("") }
    val strings = LocalStrings.current

    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val brush = Brush.radialGradient(
                    0f to Color(0xFFA0C8FF),
                    0.5f to Color(0xFFBEE0F5),
                    1f to Color(0xFFF7F9FB),
                    center = Offset(size.width / 2f, size.height * 0.15f),
                    radius = size.minDimension * 0.9f,
                )
                onDrawBehind { drawRect(brush) }
            },
    ) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column(
                Modifier.fillMaxWidth().padding(top = 44.dp, start = 32.dp, end = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = FocusColors.Success, modifier = Modifier.size(58.dp))
                Text(
                    strings.sessionSaved,
                    color = FocusColors.Ink,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp),
                )
                justSaved?.let { session ->
                    Text(
                        "${session.focus} · ${session.task.ifBlank { strings.focusFallback }}",
                        color = FocusColors.Muted,
                        fontSize = 14.5.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            if (current != null) {
                Text(
                    strings.upNext,
                    color = FocusColors.Muted,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 32.dp, bottom = 8.dp),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xEBFFFFFF))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = FocusColors.AccentBlue, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                        Text(current.label.ifBlank { strings.focusFallback }, color = FocusColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        if (waitingCount > 0) {
                            Text(strings.moreWaiting(waitingCount), color = FocusColors.Muted, fontSize = 12.5.sp)
                        }
                    }
                    val minutes = (current.plannedMs / 60_000L).toInt().coerceAtLeast(1)
                    Text(strings.minutes(minutes), color = FocusColors.Muted, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.weight(1f))

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (current != null) {
                    DockPrimary(strings.startNextSession) { viewModel.startNow(current); onContinueNow() }
                    Spacer(Modifier.height(9.dp))
                    if (showBreakInput) {
                        BreakInputRow(
                            value = breakInput,
                            onValueChange = { breakInput = it.filter(Char::isDigit).take(3) },
                            onConfirm = {
                                val breakMinutes = breakInput.toIntOrNull()?.coerceIn(1, 180) ?: 5
                                val minutes = (current.plannedMs / 60_000L).toInt().coerceAtLeast(1)
                                viewModel.consumeForBreak(current)
                                onBreak(breakMinutes, current.label, minutes)
                            },
                        )
                    } else {
                        // Tapping this doesn't start a fixed break — it just opens the
                        // "how many minutes" field below, so the length is always chosen.
                        DockTint(strings.takeABreak) { showBreakInput = true; breakInput = "" }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    strings.doneForNow,
                    color = FocusColors.AccentBlue,
                    fontSize = 15.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onSkip).padding(vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun DockPrimary(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FocusColors.AccentBlue)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Revealed when "Take a Break" is tapped — lets the user pick how many minutes, instead of
 * committing to a fixed length right away. */
@Composable
private fun BreakInputRow(value: String, onValueChange: (String) -> Unit, onConfirm: () -> Unit) {
    val strings = LocalStrings.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xEBFFFFFF))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(strings.restFor, color = FocusColors.Ink, fontSize = 16.sp, modifier = Modifier.weight(1f))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            cursorBrush = SolidColor(FocusColors.AccentBlue),
            textStyle = TextStyle(color = FocusColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onConfirm() }),
            modifier = Modifier.width(40.dp).focusRequester(focusRequester),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text("5", color = FocusColors.Muted2, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                }
                inner()
            },
        )
        Text(strings.minLabel, color = FocusColors.Muted, fontSize = 15.sp, modifier = Modifier.padding(end = 10.dp))
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(FocusColors.AccentBlue)
                .clickable(onClick = onConfirm),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Check, contentDescription = strings.startBreak, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun DockTint(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xEBFFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = FocusColors.AccentBlue, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}
