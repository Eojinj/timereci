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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
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
import com.timereci.focus.ui.theme.PhotoTones
import com.timereci.focus.ui.theme.patternPlaceholder
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
    val durationPresets by viewModel.durationPresets.collectAsStateWithLifecycle()
    val recentPhotos by viewModel.recentPhotos.collectAsStateWithLifecycle()
    var editingPresetIndex by remember { mutableStateOf<Int?>(null) }
    var showBackdropPicker by remember { mutableStateOf(false) }
    val emptyBackdropTone = remember { PhotoTones.indexFor(java.time.LocalDate.now().toEpochDay()) }

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
    val displayMs = if (isPreStart) duration else state.remainingMs

    Box(
        Modifier
            .fillMaxSize()
            .background(FocusColors.BaseLight)
            .grain(0.07f),
    ) {
        // Backdrop: chosen photo, else a neutral ground (no plastic blue).
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
                .patternPlaceholder(emptyBackdropTone),
        )
        // Wash so the digits stay readable over any photo (neutral, not blue).
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xE6F6F7F9), Color(0xB8E6E9ED), Color(0x8FD4D8DE)),
                    ),
                ),
        )

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

        if (isPreStart) {
            PreStartContent(
                task = task,
                onTaskChange = viewModel::setTask,
                durationMs = duration,
                onDurationChange = viewModel::setDuration,
                isLandscape = isLandscape,
                hasBackdrop = backdrop != null,
                onPickBackdrop = {
                    if (recentPhotos.isEmpty()) launchSystemBackdropPicker() else showBackdropPicker = true
                },
                onStart = viewModel::start,
                presets = durationPresets,
                onLongPressPreset = { index -> editingPresetIndex = index },
                cameFromQueue = viewModel.cameFromQueue,
            )
        } else {
            RunningContent(
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
                IconActionButton(
                    icon = Icons.Outlined.EditNote,
                    contentDescription = "코멘트",
                    onClick = {
                        if (!keepRunning) viewModel.pause()
                        viewModel.openComment()
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconActionButton(
                        icon = Icons.Outlined.Close,
                        contentDescription = "정지",
                        onClick = {
                            viewModel.abandon()
                            onAbandon()
                        },
                    )
                    IconActionButton(
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

    editingPresetIndex?.let { index ->
        EditPresetDialog(
            initialMinutes = durationPresets.getOrElse(index) { 25 },
            onDismiss = { editingPresetIndex = null },
            onSave = { minutes ->
                viewModel.updatePreset(index, minutes)
                editingPresetIndex = null
            },
        )
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
}

/**
 * The pre-start setup, presented as a single card floating over the backdrop: task + big
 * minutes field, presets, and a full-width start button anchored to the bottom of the card.
 * Typing "할 일 20" into the task field and dismissing the keyboard splits it into the task
 * label and the minutes field in one move — the same shorthand as the todo quick-add.
 */
@Composable
private fun PreStartContent(
    task: String,
    onTaskChange: (String) -> Unit,
    durationMs: Long,
    onDurationChange: (Long) -> Unit,
    isLandscape: Boolean,
    hasBackdrop: Boolean,
    onPickBackdrop: () -> Unit,
    onStart: () -> Unit,
    presets: List<Int>,
    onLongPressPreset: (index: Int) -> Unit,
    cameFromQueue: Boolean,
) {
    var minutesText by rememberSaveable { mutableStateOf((durationMs / 60_000L).coerceAtLeast(1).toString()) }

    fun commit(newText: String) {
        minutesText = newText
        newText.toIntOrNull()?.let { m -> if (m in 1..MAX_MINUTES) onDurationChange(m * 60_000L) }
    }

    val validMinutes = minutesText.toIntOrNull()?.let { it in 1..MAX_MINUTES } == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 22.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .then(if (isLandscape) Modifier.widthIn(max = 420.dp) else Modifier.fillMaxWidth())
                .clip(RoundedCornerShape(28.dp))
                .background(FocusColors.Paper)
                .border(1.dp, FocusColors.LineSoft, RoundedCornerShape(28.dp))
                .padding(horizontal = 22.dp, vertical = 22.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (cameFromQueue) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(FocusColors.Mist)
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

            Spacer(Modifier.height(if (isLandscape) 10.dp else 18.dp))

            TaskField(value = task, onValueChange = onTaskChange, big = !isLandscape)

            Spacer(Modifier.height(if (isLandscape) 10.dp else 18.dp))

            MinutesField(
                text = minutesText,
                onTextChange = ::commit,
                numberSize = if (isLandscape) 76.sp else 60.sp,
                suffixSize = if (isLandscape) 26.sp else 20.sp,
            )

            Spacer(Modifier.height(if (isLandscape) 14.dp else 20.dp))

            PresetRow(
                presets = presets,
                selectedMinutes = minutesText.toIntOrNull(),
                onSelect = { m -> commit(m.toString()) },
                onLongPress = onLongPressPreset,
            )

            Spacer(Modifier.height(if (isLandscape) 18.dp else 24.dp))

            StartButton(onClick = onStart, enabled = validMinutes)
        }
    }
}

@Composable
private fun MinutesField(
    text: String,
    onTextChange: (String) -> Unit,
    numberSize: TextUnit,
    suffixSize: TextUnit,
) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicTextField(
            value = text,
            onValueChange = { raw -> onTextChange(raw.filter(Char::isDigit).take(3)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            textStyle = TextStyle(
                fontFamily = GothicFamily,
                fontSize = numberSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-2).sp,
                fontFeatureSettings = "tnum",
                color = FocusColors.Ink,
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(FocusColors.AccentBlue),
            modifier = Modifier.widthIn(min = 64.dp),
        )
        Text(
            "분",
            color = FocusColors.Muted,
            fontFamily = GothicFamily,
            fontSize = suffixSize,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}

/** Preset chips — tap to pick, long-press to rename ("이미 있는 버튼 꾹 눌러서 수정"). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetRow(presets: List<Int>, selectedMinutes: Int?, onSelect: (Int) -> Unit, onLongPress: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEachIndexed { index, min ->
            val active = min == selectedMinutes
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (active) FocusColors.AccentDeep else Color(0xB3FFFFFF))
                    .combinedClickable(
                        onClick = { onSelect(min) },
                        onLongClick = { onLongPress(index) },
                    )
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
private fun EditPresetDialog(initialMinutes: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by rememberSaveable(initialMinutes) { mutableStateOf(initialMinutes.toString()) }
    val valid = text.toIntOrNull()?.let { it in 1..MAX_MINUTES } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("프리셋 시간 수정", fontWeight = FontWeight.Bold) },
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
                        modifier = Modifier.widthIn(min = 40.dp),
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

/** Full-width primary action, anchored to the bottom of the pre-start card. */
@Composable
private fun StartButton(onClick: () -> Unit, enabled: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) FocusColors.AccentDeep else FocusColors.AccentDeep.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = FocusColors.Paper,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text("시작", color = FocusColors.Paper, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/** Countdown while a session is running: big digits, breathing progress line. */
@Composable
private fun RunningContent(displayMs: Long, progress: Float, isLandscape: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
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
    }
}

@Composable
private fun TaskField(value: String, onValueChange: (String) -> Unit, big: Boolean) {
    val fontSize = if (big) 26.sp else 19.sp
    Box(contentAlignment = Alignment.Center) {
        if (value.isBlank()) {
            Text(
                "할 일 한 줄 (선택)",
                color = FocusColors.Muted2,
                fontFamily = GothicFamily,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            cursorBrush = SolidColor(FocusColors.AccentBlue),
            textStyle = TextStyle(
                color = FocusColors.Ink,
                fontFamily = GothicFamily,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
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
