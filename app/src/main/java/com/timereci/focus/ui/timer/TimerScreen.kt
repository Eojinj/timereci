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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.MonoFamily
import com.timereci.focus.ui.theme.PhotoTones
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

    val context = LocalContext.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // The rest of the app is portrait-locked (see AndroidManifest); unlock rotation only
    // while this screen is shown, so landscape kicks in when the user actually turns the
    // phone rather than being forced on entry. Restored to the app default on exit.
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        onDispose {
            activity?.requestedOrientation = previous ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
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

        // Doubles as the "pick a backdrop photo" button while setting up.
        Text(
            "배경 · 시작 전 지정 사진",
            color = FocusColors.InkSoft,
            fontFamily = MonoFamily,
            fontSize = 9.5.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x80FFFFFF))
                .then(
                    if (isPreStart) {
                        Modifier.clickable {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        }
                    } else Modifier,
                )
                .padding(horizontal = 9.dp, vertical = 3.dp),
        )

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
                GlassButton(text = "코멘트", onClick = {
                    if (!keepRunning) viewModel.pause()
                    viewModel.openComment()
                })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton(text = "정지", onClick = {
                        viewModel.abandon()
                        onAbandon()
                    })
                    DeepButton(text = "완주 →", onClick = viewModel::completeNow, wide = false)
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

/** Task label + big digits + a big circular start button + a numeric keypad to set the duration. */
@Composable
private fun PreStartContent(
    task: String,
    onTaskChange: (String) -> Unit,
    durationMs: Long,
    onDurationChange: (Long) -> Unit,
    isLandscape: Boolean,
    onStart: () -> Unit,
) {
    // The buffer starts pre-filled with the current default so the display isn't blank, but
    // the first keystroke wipes it and starts a fresh entry — typing "2" right after opening
    // shouldn't append onto the old default and produce a huge number.
    var digits by rememberSaveable { mutableStateOf(msToDigits(durationMs)) }
    var edited by rememberSaveable { mutableStateOf(false) }

    fun apply(newDigits: String) {
        val ms = digitsToMs(newDigits).coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
        digits = msToDigits(ms)
        onDurationChange(ms)
    }

    fun typeDigit(d: Int) {
        val base = if (edited) digits else "00000"
        edited = true
        apply((base + d).takeLast(5))
    }

    fun backspace() {
        edited = true
        apply(("0" + digits).dropLast(1))
    }

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

        DigitDisplay(digits = digits, fontSize = if (isLandscape) 84.sp else 58.sp)

        Spacer(Modifier.height(if (isLandscape) 18.dp else 26.dp))

        StartCircle(onClick = onStart, size = if (isLandscape) 84.dp else 96.dp)

        Spacer(Modifier.height(if (isLandscape) 16.dp else 24.dp))

        Keypad(
            modifier = Modifier.widthIn(max = 300.dp),
            onDigit = ::typeDigit,
            onBackspace = ::backspace,
        )
    }
}

@Composable
private fun DigitDisplay(digits: String, fontSize: androidx.compose.ui.unit.TextUnit) {
    val h = digits.substring(0, 1)
    val m = digits.substring(1, 3)
    val s = digits.substring(3, 5)
    Text(
        "$h : $m : $s",
        color = Color(0xFF22364A),
        style = TextStyle(
            fontFamily = MonoFamily,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
        ),
    )
}

@Composable
private fun StartCircle(onClick: () -> Unit, size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(FocusColors.AccentDeep)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("시작", color = FocusColors.Paper, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Keypad(modifier: Modifier = Modifier, onDigit: (Int) -> Unit, onBackspace: () -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { n ->
                    KeypadKey(label = "$n", modifier = Modifier.weight(1f), onClick = { onDigit(n) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeypadKey(
                label = "⌫",
                modifier = Modifier.weight(1f),
                accent = true,
                onClick = onBackspace,
            )
            KeypadKey(label = "0", modifier = Modifier.weight(1f), onClick = { onDigit(0) })
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun KeypadKey(label: String, modifier: Modifier = Modifier, accent: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (accent) Color(0x992E4257) else Color(0xB3FFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (accent) FocusColors.Paper else FocusColors.Ink2,
            fontFamily = MonoFamily,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
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
private fun GlassButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xA8FFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = FocusColors.Ink2, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DeepButton(text: String, onClick: () -> Unit, wide: Boolean) {
    Box(
        Modifier
            .height(46.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(FocusColors.AccentDeep)
            .clickable(onClick = onClick)
            .padding(horizontal = if (wide) 42.dp else 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = FocusColors.Paper, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Color.White)
                .clickable(enabled = false) {}
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
                    .clip(RoundedCornerShape(10.dp))
                    .background(FocusColors.AccentDeep)
                    .clickable(onClick = onClose)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text("완료", color = FocusColors.Paper, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private const val MIN_DURATION_MS = 60_000L        // 1분
private const val MAX_DURATION_MS = 180 * 60_000L  // 3시간 (단일 시 자리에 들어가는 상한)

/** ms -> 5-digit "HMMSS" buffer, e.g. 25분 -> "02500" -> displayed "0 : 25 : 00". */
private fun msToDigits(ms: Long): String {
    val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
    val h = (totalSec / 3600).coerceIn(0, 9)
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return "%01d%02d%02d".format(h, m, s)
}

private fun digitsToMs(digits: String): Long {
    val safe = digits.padStart(5, '0').takeLast(5)
    val h = safe.substring(0, 1).toInt()
    val m = safe.substring(1, 3).toInt()
    val s = safe.substring(3, 5).toInt()
    return (h * 3_600_000L) + (m * 60_000L) + (s * 1_000L)
}
