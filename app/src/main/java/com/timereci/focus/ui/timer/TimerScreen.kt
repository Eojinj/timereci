package com.timereci.focus.ui.timer

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.ScreenRotation
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.timereci.focus.data.PhotoStorage
import com.timereci.focus.ui.components.IconActionButton
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.GothicFamily
import com.timereci.focus.ui.util.Formatters
import com.timereci.focus.ui.util.findActivity

/**
 * Focus — the running countdown (Merci v5 screen 4). No ring: plain numerals over a thin
 * progress line. Controls live in a bottom dock bar (Note / Pause / Finish, then Cancel
 * Session below) — nothing floats in the middle of the screen.
 */
@Composable
fun TimerScreen(
    onCompleted: () -> Unit,
    onAbandon: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel(),
) {
    val state by viewModel.timerState.collectAsStateWithLifecycle()
    val keepRunning by viewModel.keepRunningWhileCommenting.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val commentFocusRequester = remember { FocusRequester() }
    val sensorLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var forcedLandscape by rememberSaveable { mutableStateOf(false) }
    val isLandscape = sensorLandscape || forcedLandscape
    // Tracks whether focusing the note field is what paused the session, so unfocusing it
    // only resumes that same auto-pause — not a pause the user set deliberately with Pause.
    var autoPausedByNote by rememberSaveable { mutableStateOf(false) }

    androidx.compose.runtime.DisposableEffect(Unit) {
        val activity = context.findActivity()
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        onDispose {
            activity?.requestedOrientation = previous ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
    LaunchedEffect(forcedLandscape) {
        context.findActivity()?.requestedOrientation =
            if (forcedLandscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_USER
    }

    LaunchedEffect(state.phase) {
        if (state.phase == com.timereci.focus.timer.TimerPhase.COMPLETED) onCompleted()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(FocusColors.BaseLight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { focusManager.clearFocus() },
            ),
    ) {
        val backdropName = state.backdropFileName
        if (backdropName != null) {
            val backdropRequest = remember(backdropName) {
                ImageRequest.Builder(context)
                    .data(PhotoStorage.fileIn(context, backdropName)).crossfade(true).build()
            }
            AsyncImage(
                model = backdropRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xE6F6F7F9), Color(0xB8E6E9ED), Color(0x8FD4D8DE)),
                        ),
                    ),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val brush = Brush.radialGradient(
                            0f to Color(0xFF6FA8DE),
                            0.5f to Color(0xFFBEE0F5),
                            1f to Color(0xFFFFFFFF),
                            center = Offset(size.width / 2f, size.height * 0.4f),
                            radius = size.minDimension * 0.7f,
                        )
                        onDrawBehind { drawRect(brush) }
                    },
            )
        }

        IconActionButton(
            icon = Icons.Outlined.ScreenRotation,
            contentDescription = "Toggle landscape",
            onClick = { forcedLandscape = !forcedLandscape },
            size = 42.dp,
            accent = forcedLandscape,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
        )

        RunningContent(
            remainingMs = state.remainingMs,
            progress = state.progress,
            isLandscape = isLandscape,
            task = state.taskLabel,
            comment = state.draftComment,
            onCommentChange = viewModel::updateComment,
            commentFocusRequester = commentFocusRequester,
            onCommentFocusChanged = { focused ->
                if (focused) {
                    if (!keepRunning && state.phase != com.timereci.focus.timer.TimerPhase.PAUSED) {
                        viewModel.pause()
                        autoPausedByNote = true
                    }
                } else if (autoPausedByNote) {
                    viewModel.resume()
                    autoPausedByNote = false
                }
            },
            isPaused = state.phase == com.timereci.focus.timer.TimerPhase.PAUSED,
            onNote = { commentFocusRequester.requestFocus() },
            onTogglePause = { if (state.phase == com.timereci.focus.timer.TimerPhase.PAUSED) viewModel.resume() else viewModel.pause() },
            onCancel = { viewModel.abandon(); onAbandon() },
            onFinish = viewModel::completeNow,
        )
    }
}

@Composable
private fun RunningContent(
    remainingMs: Long,
    progress: Float,
    isLandscape: Boolean,
    task: String,
    comment: String,
    onCommentChange: (String) -> Unit,
    commentFocusRequester: FocusRequester,
    onCommentFocusChanged: (Boolean) -> Unit,
    isPaused: Boolean,
    onNote: () -> Unit,
    onTogglePause: () -> Unit,
    onCancel: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (task.isNotBlank()) {
                Text(
                    task,
                    color = FocusColors.Ink2,
                    fontFamily = GothicFamily,
                    fontSize = if (isLandscape) 22.sp else 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp),
                )
            }
            Text(
                Formatters.clock(remainingMs),
                color = FocusColors.Ink,
                style = TextStyle(
                    fontFamily = GothicFamily,
                    fontSize = if (isLandscape) 132.sp else 94.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-2).sp,
                    fontFeatureSettings = "tnum",
                ),
            )
            ProgressLine(progress = progress, isLandscape = isLandscape)
            Text(
                "Ends at ${Formatters.timeOfDay(System.currentTimeMillis() + remainingMs)}",
                color = FocusColors.Muted,
                fontSize = 14.sp,
            )
            NoteField(
                comment = comment,
                onCommentChange = onCommentChange,
                focusRequester = commentFocusRequester,
                onFocusChanged = onCommentFocusChanged,
            )
        }

        // No opaque bar here — the individual pill buttons below already read fine directly
        // over the gradient/backdrop, so the dock stays transparent instead of a solid strip.
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DockButton(icon = Icons.Outlined.EditNote, label = "Note", onClick = onNote, modifier = Modifier.weight(1f))
                DockButton(
                    icon = if (isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                    label = if (isPaused) "Resume" else "Pause",
                    onClick = onTogglePause,
                    modifier = Modifier.weight(1f),
                )
                DockButton(icon = Icons.Outlined.Check, label = "Finish", onClick = onFinish, filled = true, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Cancel Session",
                color = FocusColors.Danger,
                fontSize = 15.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCancel,
                    )
                    .padding(vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun DockButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = false) {
    // No pill background — just the icon and label sitting directly on the backdrop, like
    // the pre-start star used to. Finish still reads as the primary action via color alone.
    val tint = if (filled) FocusColors.AccentBlue else FocusColors.Ink2
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = tint, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * The note, editable right where it's shown — no separate popup. It's the same text whether
 * you're looking at it or typing it, so there's only one place it ever lives.
 */
@Composable
private fun NoteField(
    comment: String,
    onCommentChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
) {
    val textStyle = TextStyle(
        color = FocusColors.InkSoft,
        fontFamily = GothicFamily,
        fontSize = 15.5.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 22.sp,
        textAlign = TextAlign.Center,
    )
    BasicTextField(
        value = comment,
        onValueChange = onCommentChange,
        textStyle = textStyle,
        cursorBrush = SolidColor(FocusColors.AccentBlue),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusChanged(it.isFocused) },
        decorationBox = { inner ->
            if (comment.isEmpty()) {
                Text("Add a note about right now…", style = textStyle.copy(color = FocusColors.Muted2), modifier = Modifier.fillMaxWidth())
            }
            inner()
        },
    )
}

@Composable
private fun ProgressLine(progress: Float, isLandscape: Boolean) {
    Box(
        Modifier
            .then(if (isLandscape) Modifier.width(400.dp) else Modifier.fillMaxWidth(0.85f))
            .padding(vertical = 16.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0x2E2E4257)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(FocusColors.AccentBlue),
        )
    }
}

