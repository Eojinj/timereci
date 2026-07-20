package com.timereci.focus.ui.timer

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.ScreenRotation
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.timereci.focus.data.PhotoStorage
import com.timereci.focus.timer.TimerPhase
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.MonoFamily
import com.timereci.focus.ui.theme.PhotoTones
import com.timereci.focus.ui.util.Formatters
import com.timereci.focus.ui.util.findActivity

private const val MAX_MINUTES = 180 // 3시간

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

    val isPreStart = state.phase == TimerPhase.IDLE
    val displayMs = if (isPreStart) duration else state.remainingMs

    Box(
        Modifier
            .fillMaxSize()
            .background(FocusColors.Mist),
    ) {
        // Backdrop: chosen photo, else the calm striped placeholder.
        backdrop?.fileName?.let { name ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(PhotoStorage.fileIn(context, name)).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } ?: Box(
            Modifier
                .fillMaxSize()
                .background(PhotoTones.brush(0)),
        )
        // Wash so the digits stay readable over any photo.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xD1F7FBFE), Color(0x9ED6E7F4), Color(0x809EBFDB)),
                    ),
                ),
        )

        // Top corner controls: backdrop photo (pre-start only) + manual landscape toggle (always).
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (isPreStart) {
                IconGlassButton(
                    icon = Icons.Outlined.PhotoCamera,
                    contentDescription = "배경 사진 지정",
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    size = 52.dp,
                )
            } else {
                Spacer(Modifier.size(52.dp))
            }
            IconGlassButton(
                icon = Icons.Outlined.ScreenRotation,
                contentDescription = "가로 모드 전환",
                onClick = { forcedLandscape = !forcedLandscape },
                size = 52.dp,
                accent = forcedLandscape,
            )
        }

        if (isPreStart) {
            PreStartContent(
                task = task,
                onTaskChange = viewModel::setTask,
                durationMs = duration,
                onDurationChange = viewModel::setDuration,
                isLandscape = isLandscape,
                onStart = viewModel::start,
            )
        } else {
            RunningContent(
                task = task,
                displayMs = displayMs,
                progress = state.progress,
                isLandscape = isLandscape,
            )

            // Bottom controls while running.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 30.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconGlassButton(
                    icon = Icons.Outlined.EditNote,
                    contentDescription = "코멘트",
                    onClick = {
                        if (!keepRunning) viewModel.pause()
                        viewModel.openComment()
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconGlassButton(
                        icon = Icons.Outlined.Close,
                        contentDescription = "정지",
                        onClick = {
                            viewModel.abandon()
                            onAbandon()
                        },
                    )
                    IconGlassButton(
                        icon = Icons.Outlined.Check,
                        contentDescription = "완주",
                        onClick = viewModel::completeNow,
                        accent = true,
                    )
                }
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
}

/** Task label + big minutes field (system numeric keyboard) + presets + a circular start button. */
@Composable
private fun PreStartContent(
    task: String,
    onTaskChange: (String) -> Unit,
    durationMs: Long,
    onDurationChange: (Long) -> Unit,
    isLandscape: Boolean,
    onStart: () -> Unit,
) {
    var minutesText by rememberSaveable { mutableStateOf((durationMs / 60_000L).coerceAtLeast(1).toString()) }

    fun commit(newText: String) {
        minutesText = newText
        newText.toIntOrNull()?.let { m -> if (m in 1..MAX_MINUTES) onDurationChange(m * 60_000L) }
    }

    val validMinutes = minutesText.toIntOrNull()?.let { it in 1..MAX_MINUTES } == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TaskField(value = task, onValueChange = onTaskChange)

        Spacer(Modifier.height(if (isLandscape) 14.dp else 22.dp))

        MinutesField(
            text = minutesText,
            onTextChange = ::commit,
            numberSize = if (isLandscape) 88.sp else 64.sp,
            suffixSize = if (isLandscape) 28.sp else 22.sp,
        )

        Spacer(Modifier.height(if (isLandscape) 16.dp else 22.dp))

        PresetRow(selectedMinutes = minutesText.toIntOrNull(), onSelect = { m -> commit(m.toString()) })

        Spacer(Modifier.height(if (isLandscape) 18.dp else 28.dp))

        StartCircle(onClick = onStart, enabled = validMinutes, size = if (isLandscape) 78.dp else 88.dp)
    }
}

@Composable
private fun MinutesField(
    text: String,
    onTextChange: (String) -> Unit,
    numberSize: TextUnit,
    suffixSize: TextUnit,
) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicTextField(
            value = text,
            onValueChange = { raw -> onTextChange(raw.filter(Char::isDigit).take(3)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            textStyle = TextStyle(
                fontFamily = MonoFamily,
                fontSize = numberSize,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF22364A),
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(FocusColors.AccentBlue),
            modifier = Modifier.widthIn(min = 64.dp),
        )
        Text(
            "분",
            color = Color(0xFF22364A),
            fontSize = suffixSize,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 10.dp),
        )
    }
}

@Composable
private fun PresetRow(selectedMinutes: Int?, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DURATION_PRESETS.forEach { min ->
            val active = min == selectedMinutes
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (active) FocusColors.AccentDeep else Color(0xB3FFFFFF))
                    .clickable { onSelect(min) }
                    .padding(horizontal = 15.dp, vertical = 8.dp),
            ) {
                Text(
                    "${min}분",
                    color = if (active) FocusColors.Paper else FocusColors.Ink2,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun StartCircle(onClick: () -> Unit, enabled: Boolean, size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (enabled) FocusColors.AccentDeep else FocusColors.AccentDeep.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = "시작",
            tint = FocusColors.Paper,
            modifier = Modifier.size(size * 0.42f),
        )
    }
}

@Composable
private fun IconGlassButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    accent: Boolean = false,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (accent) FocusColors.AccentDeep else Color(0xB3FFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (accent) FocusColors.Paper else FocusColors.Ink2,
            modifier = Modifier.size(size * 0.44f),
        )
    }
}

/** Countdown while a session is running: current task, big digits, breathing progress line. */
@Composable
private fun RunningContent(task: String, displayMs: Long, progress: Float, isLandscape: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "지금 · ${task.ifBlank { "집중" }}",
            color = FocusColors.InkSoft,
            fontFamily = MonoFamily,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(8.dp))

        Text(
            Formatters.clock(displayMs),
            color = Color(0xFF22364A),
            style = TextStyle(
                fontFamily = MonoFamily,
                fontSize = if (isLandscape) 128.sp else 56.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = if (isLandscape) 5.sp else 2.sp,
            ),
        )

        Spacer(Modifier.height(20.dp))

        ProgressLine(progress = progress, isLandscape = isLandscape)
    }
}

@Composable
private fun TaskField(value: String, onValueChange: (String) -> Unit) {
    Box(contentAlignment = Alignment.Center) {
        if (value.isBlank()) {
            Text("할 일 한 줄 (선택)", color = FocusColors.Muted2, fontSize = 15.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = FocusColors.Ink2,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
        )
    }
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
