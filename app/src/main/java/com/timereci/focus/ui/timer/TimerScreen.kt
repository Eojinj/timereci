package com.timereci.focus.ui.timer

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.timereci.focus.data.PhotoStorage
import com.timereci.focus.timer.TimerPhase
import com.timereci.focus.ui.components.IconActionButton
import com.timereci.focus.ui.components.RecentPhotoPickerDialog
import com.timereci.focus.ui.components.grain
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.GothicFamily
import com.timereci.focus.ui.theme.MonoFamily
import com.timereci.focus.ui.util.Formatters
import com.timereci.focus.ui.util.findActivity

@Composable
fun TimerScreen(
    onCompleted: () -> Unit,
    onAbandon: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel(),
) {
    val state by viewModel.timerState.collectAsStateWithLifecycle()
    val duration by viewModel.durationMs.collectAsStateWithLifecycle()
    val task by viewModel.taskLabel.collectAsStateWithLifecycle()
    val backdrop by viewModel.backdrop.collectAsStateWithLifecycle()
    val keepRunning by viewModel.keepRunningWhileCommenting.collectAsStateWithLifecycle()
    val commentOpen by viewModel.commentSheetOpen.collectAsStateWithLifecycle()
    val recentPhotos by viewModel.recentPhotos.collectAsStateWithLifecycle()
    var showBackdropPicker by remember { mutableStateOf(false) }
    var confirmStop by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sensorLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Sensor rotation is unlocked only on this screen (the rest of the app stays portrait —
    // see AndroidManifest), so landscape kicks in when the user physically turns the phone.
    // A manual toggle button additionally lets them force landscape without rotating at all
    // (e.g. phone propped on a stand).
    var forcedLandscape by rememberSaveable { mutableStateOf(false) }
    val isLandscape = sensorLandscape || forcedLandscape

    DisposableEffect(Unit) {
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

    // Ask for notification permission once (Android 13+) so the foreground timer can post.
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // When the session completes, hand off to the publish screen.
    LaunchedEffect(state.phase) {
        if (state.phase == TimerPhase.COMPLETED) onCompleted()
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::importBackdrop) }

    fun launchSystemBackdropPicker() {
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val isPreStart = state.phase == TimerPhase.IDLE

    // A stopped/finished session shouldn't leave the phone stuck forced into landscape.
    LaunchedEffect(isPreStart) {
        if (isPreStart) forcedLandscape = false
    }
    val displayMs = if (isPreStart) duration else state.remainingMs

    Box(
        Modifier
            .fillMaxSize()
            .background(FocusColors.BaseLight)
            .grain(0.07f),
    ) {
        // Backdrop: chosen photo (with a neutral wash for readability), else a soft blue centre
        // fading out to white at the edges.
        val backdropName = backdrop?.fileName
        if (backdropName != null) {
            // Built once per photo (not on every 250ms tick) so the running screen stays smooth.
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
            // Wash so the digits stay readable over any photo.
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
                    .background(
                        Brush.radialGradient(
                            0f to Color(0xFF6FA8DE),
                            0.5f to Color(0xFFBEE0F5),
                            1f to Color(0xFFFFFFFF),
                        ),
                    ),
            )
        }

        // Manual landscape toggle, top-right (always available).
        IconActionButton(
            icon = Icons.Outlined.ScreenRotation,
            contentDescription = "가로 모드 전환",
            onClick = { forcedLandscape = !forcedLandscape },
            size = 48.dp,
            accent = forcedLandscape,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
        )

        // Crossfade instead of a hard cut, so stopping/starting a session reads as a
        // deliberate transition rather than the screen just snapping to a different state.
        Crossfade(targetState = isPreStart, label = "timer-phase") { preStart ->
            if (preStart) {
                PreStartContent(
                    task = task,
                    onTaskChange = viewModel::setTask,
                    isLandscape = isLandscape,
                    hasBackdrop = backdrop != null,
                    onPickBackdrop = {
                        if (recentPhotos.isEmpty()) launchSystemBackdropPicker() else showBackdropPicker = true
                    },
                    onStart = viewModel::start,
                    cameFromQueue = viewModel.cameFromQueue,
                )
            } else {
                RunningContent(
                    displayMs = displayMs,
                    progress = state.progress,
                    isLandscape = isLandscape,
                    task = task,
                    comment = state.draftComment,
                    onComment = {
                        if (!keepRunning) viewModel.pause()
                        viewModel.openComment()
                    },
                    onStop = { confirmStop = true },
                    onComplete = viewModel::completeNow,
                )
            }
        }

        if (commentOpen) {
            CommentSheet(
                initial = state.draftComment,
                keepRunning = keepRunning,
                onChange = viewModel::updateComment,
                onClose = {
                    if (!keepRunning) viewModel.resume()
                    viewModel.closeComment()
                },
            )
        }
    }

    if (showBackdropPicker) {
        RecentPhotoPickerDialog(
            recent = recentPhotos,
            onPickRecent = { ref ->
                viewModel.useRecentBackdrop(ref)
                showBackdropPicker = false
            },
            onPickNew = {
                showBackdropPicker = false
                launchSystemBackdropPicker()
            },
            onDismiss = { showBackdropPicker = false },
        )
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("집중을 정지할까요?", fontWeight = FontWeight.Bold, color = FocusColors.Ink) },
            text = {
                Text(
                    "정지하면 이번 세션은 기록에 남지 않아요.\n지금까지 집중한 만큼 남기려면 완주(✓)를 눌러 주세요.",
                    color = FocusColors.Muted,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                )
            },
            confirmButton = {
                IconActionButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = "정지",
                    onClick = {
                        confirmStop = false
                        viewModel.abandon()
                        onAbandon()
                    },
                    size = 40.dp,
                )
            },
            dismissButton = {
                IconActionButton(
                    icon = Icons.Filled.PlayArrow,
                    contentDescription = "계속",
                    onClick = { confirmStop = false },
                    accent = true,
                    size = 40.dp,
                )
            },
        )
    }
}

/**
 * The pre-start setup, floating directly over the backdrop (no opaque card, so the gradient
 * stays visible): a soft, glowing star in the middle that you tap to open — the star disappears,
 * the keyboard rises over a bare cursor, and tapping again (once something's typed) starts the
 * session. No card, no button — just the star, then just the text.
 */
@Composable
private fun PreStartContent(
    task: String,
    onTaskChange: (String) -> Unit,
    isLandscape: Boolean,
    hasBackdrop: Boolean,
    onPickBackdrop: () -> Unit,
    onStart: () -> Unit,
    cameFromQueue: Boolean,
) {
    // Already-queued sessions (task prefilled) skip straight to the revealed form.
    var revealed by remember { mutableStateOf(cameFromQueue) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 22.dp, vertical = 16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (cameFromQueue) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xB3FFFFFF))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Outlined.Checklist,
                        contentDescription = null,
                        tint = FocusColors.AccentBlue,
                        modifier = Modifier.size(13.dp),
                    )
                    Text("할 일 큐에서", color = FocusColors.AccentBlue, fontFamily = MonoFamily, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            IconActionButton(
                icon = Icons.Outlined.PhotoCamera,
                contentDescription = "배경 사진 지정",
                onClick = onPickBackdrop,
                size = 42.dp,
                accent = hasBackdrop,
            )
        }

        val diameter = if (isLandscape) 190.dp else 220.dp
        Crossfade(
            targetState = revealed,
            modifier = Modifier.align(Alignment.Center),
            label = "reveal",
        ) { isRevealed ->
            if (isRevealed) {
                BareTaskEntry(
                    diameter = diameter,
                    value = task,
                    onValueChange = onTaskChange,
                    isLandscape = isLandscape,
                    onStart = onStart,
                )
            } else {
                RevealStar(diameter = diameter, onTap = { revealed = true })
            }
        }
    }
}

/** The closed state of the pre-start setup: a single star with a soft blurred glow behind it. */
@Composable
private fun RevealStar(diameter: Dp, onTap: () -> Unit) {
    Box(
        Modifier
            .size(diameter)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(diameter * 0.5f)
                .blur(30.dp)
                .clip(CircleShape)
                .background(FocusColors.AccentBlue.copy(alpha = 0.45f)),
        )
        Icon(
            Icons.Filled.Star,
            contentDescription = "탭해서 시작",
            tint = FocusColors.AccentDeep,
            modifier = Modifier.size(diameter * 0.24f),
        )
    }
}

/**
 * The opened state: nothing but a bare, auto-focused cursor — the keyboard rises the moment
 * the star is tapped. Typing then tapping anywhere around the text (not needed on the text
 * itself) starts the session; pressing the keyboard's done key does the same.
 */
@Composable
private fun BareTaskEntry(
    diameter: Dp,
    value: String,
    onValueChange: (String) -> Unit,
    isLandscape: Boolean,
    onStart: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        Modifier
            .size(diameter)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { if (value.isNotBlank()) onStart() },
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            cursorBrush = SolidColor(FocusColors.AccentBlue),
            textStyle = TextStyle(
                color = FocusColors.Ink,
                fontFamily = GothicFamily,
                fontSize = if (isLandscape) 24.sp else 21.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (value.isNotBlank()) onStart() }),
            modifier = Modifier
                .widthIn(max = 180.dp)
                .focusRequester(focusRequester),
        )
    }
}

/** Countdown while a session is running: the task, big digits, progress line, the live comment
 * surfacing like a lyric, and the comment/stop/complete controls sitting right beneath it. */
@Composable
private fun RunningContent(
    displayMs: Long,
    progress: Float,
    isLandscape: Boolean,
    task: String,
    comment: String,
    onComment: () -> Unit,
    onStop: () -> Unit,
    onComplete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (task.isNotBlank()) {
            Text(
                task,
                color = FocusColors.Ink2,
                fontFamily = GothicFamily,
                fontSize = if (isLandscape) 30.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(if (isLandscape) 20.dp else 14.dp))
        }
        Text(
            Formatters.clock(displayMs),
            color = FocusColors.Ink,
            style = TextStyle(
                fontFamily = GothicFamily,
                fontSize = if (isLandscape) 132.sp else 62.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = if (isLandscape) (-2).sp else (-1).sp,
                fontFeatureSettings = "tnum",
            ),
        )

        Spacer(Modifier.height(20.dp))

        ProgressLine(progress = progress, isLandscape = isLandscape)

        // The comment appears on screen right away, softly, like a line of lyrics —
        // not tucked away in the sheet.
        LyricComment(comment = comment, isLandscape = isLandscape)

        Spacer(Modifier.height(if (isLandscape) 24.dp else 36.dp))

        // Controls, grouped just under the timer instead of pinned to the screen bottom.
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconActionButton(
                icon = Icons.Outlined.EditNote,
                contentDescription = "코멘트",
                onClick = onComment,
            )
            IconActionButton(
                icon = Icons.Outlined.Close,
                contentDescription = "정지",
                onClick = onStop,
            )
            IconActionButton(
                icon = Icons.Outlined.Check,
                contentDescription = "완주",
                onClick = onComplete,
                accent = true,
            )
        }
    }
}

/** The in-session comment, surfaced on screen with a gentle fade-in when it first appears. */
@Composable
private fun LyricComment(comment: String, isLandscape: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (comment.isBlank()) 0f else 1f,
        animationSpec = tween(600),
        label = "lyric",
    )
    if (comment.isBlank()) return
    Spacer(Modifier.height(if (isLandscape) 16.dp else 22.dp))
    Text(
        comment,
        color = FocusColors.InkSoft,
        fontFamily = GothicFamily,
        fontSize = if (isLandscape) 18.sp else 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = if (isLandscape) 27.sp else 24.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
    )
}


@Composable
private fun ProgressLine(progress: Float, isLandscape: Boolean) {
    val transition = rememberInfiniteTransition(label = "breathe")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(
        Modifier
            .then(if (isLandscape) Modifier.width(400.dp) else Modifier.fillMaxWidth(0.85f))
            .height(20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color(0x2E2E4257)),
        )
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(2.dp)
                .background(FocusColors.AccentBlue),
        )
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f)),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(FocusColors.AccentBlue.copy(alpha = pulse)),
            )
        }
    }
}

@Composable
private fun CommentSheet(
    initial: String,
    keepRunning: Boolean,
    onChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x3D0F1B26))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Color.White)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = false,
                ) {}
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 26.dp, vertical = 18.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(FocusColors.LineStrong),
            )
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("코멘트", color = FocusColors.Muted, fontFamily = MonoFamily, fontSize = 11.sp)
                Text(
                    if (keepRunning) "타이머는 계속 흐릅니다" else "타이머 일시정지됨",
                    color = FocusColors.Muted2,
                    fontFamily = MonoFamily,
                    fontSize = 10.5.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            var text by remember { mutableStateOf(initial) }
            BasicTextField(
                value = text,
                onValueChange = { text = it; onChange(it) },
                textStyle = TextStyle(color = FocusColors.Ink2, fontSize = 16.sp),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text("지금의 한 줄을 남겨보세요.", color = FocusColors.Muted2, fontSize = 16.sp)
                    }
                    inner()
                },
            )
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .align(Alignment.End)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(FocusColors.AccentDeep)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Check, contentDescription = "완료", tint = FocusColors.Paper, modifier = Modifier.size(20.dp))
            }
        }
    }
}
