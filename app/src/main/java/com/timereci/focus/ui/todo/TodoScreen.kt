package com.timereci.focus.ui.todo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.timereci.focus.data.PhotoStorage
import com.timereci.focus.data.PlannedFocusEntity
import com.timereci.focus.ui.components.IconActionButton
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.GothicFamily
import com.timereci.focus.ui.theme.MonoFamily
import com.timereci.focus.ui.util.Formatters
import com.timereci.focus.ui.util.PhotoPalette
import com.timereci.focus.ui.util.QuickEntry
import java.time.LocalDate

/**
 * A dedicated page for the todo queue ("오늘 할 집중"): the head of the queue leads as a
 * "priority focus" card, the rest wait below as their own cards over the same soft-gradient
 * (or custom-photo) backdrop used everywhere else, and an add bar pinned to the bottom keeps
 * queuing new items a thumb's reach away. Typing "빨래 20" and hitting Enter adds "빨래" at 20
 * minutes directly — no need to tap a preset — and the field stays focused so several items
 * can be queued back-to-back.
 */
@Composable
fun TodoScreen(
    onStartPlanned: (String, Int) -> Unit,
    viewModel: TodoViewModel = hiltViewModel(),
) {
    val planned by viewModel.planned.collectAsStateWithLifecycle()
    val background by viewModel.background.collectAsStateWithLifecycle()
    val recentCompleted by viewModel.recentCompleted.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val backgroundPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::setBackground) }

    // A rough accent color sampled from the custom background photo, so the chips and header
    // wash pick up its mood instead of always using the same fixed brand color.
    var accentColor by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(background) {
        accentColor = background?.let { PhotoPalette.accentColor(context, it) }
    }
    var showBackgroundMenu by remember { mutableStateOf(false) }

    var label by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val (previewLabel, previewMinutes) = remember(label) { QuickEntry.parse(label) }

    fun submit() {
        val finalLabel = previewLabel.ifBlank { label.trim() }
        if (finalLabel.isNotEmpty()) {
            // No number typed ("빨래") just falls back to the app's usual default duration.
            viewModel.add(finalLabel, previewMinutes ?: 25)
            label = ""
        }
        keyboard?.show()
    }

    fun startItem(item: PlannedFocusEntity) {
        viewModel.remove(item.id)
        onStartPlanned(item.label, (item.plannedMs / 60_000L).toInt().coerceAtLeast(1))
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val priority = planned.firstOrNull()
    val pending = if (planned.size > 1) planned.subList(1, planned.size) else emptyList()

    Box(Modifier.fillMaxSize()) {
        // Full-bleed background behind the whole screen — the custom photo when set, or the
        // same soft blue-to-white gradient used across the timer / next-up / publish screens.
        val backgroundFileName = background
        if (backgroundFileName != null) {
            val request = remember(backgroundFileName) {
                ImageRequest.Builder(context)
                    .data(PhotoStorage.fileIn(context, backgroundFileName))
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val brush = Brush.radialGradient(
                            0f to Color(0xFF6FA8DE),
                            0.5f to Color(0xFFBEE0F5),
                            1f to Color(0xFFF7F9FB),
                            center = Offset(size.width / 2f, size.height * 0.32f),
                            radius = size.minDimension * 0.9f,
                        )
                        onDrawBehind { drawRect(brush) }
                    },
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)),
        ) {
            // Cards read fine directly on either backdrop, so everything just scrolls together
            // instead of living inside a separate rounded "sheet".
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                TodoHeader(
                    taskCount = planned.size,
                    onPickBackground = { showBackgroundMenu = true },
                )
                Spacer(Modifier.height(20.dp))

                if (priority != null) {
                    PriorityCard(item = priority, onStart = { startItem(priority) })
                    Spacer(Modifier.height(24.dp))
                }

                Text(
                    if (priority != null) "대기 중" else "오늘 할 일",
                    color = FocusColors.Muted,
                    fontFamily = MonoFamily,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
                )

                if (priority == null) {
                    Text(
                        "아직 할 일이 없어요.\n아래에 적고 엔터를 누르면 바로 쌓여요.\n\"빨래 20\"처럼 뒤에 숫자를 붙이면 분까지 한 번에 설정돼요.",
                        color = FocusColors.Muted,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                } else {
                    pending.forEach { item ->
                        PendingCard(
                            item = item,
                            onStart = { startItem(item) },
                            onDelete = { viewModel.remove(item.id) },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                AddTaskCard(onClick = { focusRequester.requestFocus() })

                // Clears the pinned add bar + floating tab bar below.
                Spacer(Modifier.height(190.dp))
            }
        }

        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            AddBar(
                label = label,
                onLabelChange = { label = it },
                previewLabel = previewLabel,
                previewMinutes = previewMinutes,
                recentCompleted = recentCompleted,
                accentColor = accentColor,
                focusRequester = focusRequester,
                onSubmit = ::submit,
                onAddRecent = { task -> viewModel.add(task.label, task.minutes) },
                onDismissRecent = { task -> viewModel.dismissRecent(task.label) },
            )
        }
    }

    if (showBackgroundMenu) {
        AlertDialog(
            onDismissRequest = { showBackgroundMenu = false },
            title = { Text("배경 사진", fontWeight = FontWeight.Bold) },
            text = { Text("할 일 화면 배경으로 쓸 사진을 골라주세요.", color = FocusColors.Muted, fontSize = 13.5.sp) },
            confirmButton = {
                IconActionButton(
                    icon = Icons.Outlined.AddPhotoAlternate,
                    contentDescription = "사진 선택",
                    onClick = {
                        showBackgroundMenu = false
                        backgroundPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    accent = true,
                    size = 40.dp,
                )
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (background != null) {
                        IconActionButton(
                            icon = Icons.Outlined.Close,
                            contentDescription = "기본으로",
                            onClick = {
                                showBackgroundMenu = false
                                viewModel.clearBackground()
                            },
                            size = 40.dp,
                        )
                    }
                }
            },
        )
    }
}

/** Date eyebrow + big headline + a "N개 남음" pill, with the background-photo button up top. */
@Composable
private fun TodoHeader(taskCount: Int, onPickBackground: () -> Unit) {
    val today = remember { LocalDate.now() }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(
                Formatters.monthDayWeekday(today),
                color = FocusColors.AccentBlue,
                fontFamily = MonoFamily,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "할 일",
                    color = FocusColors.Ink,
                    fontFamily = GothicFamily,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xB3FFFFFF))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("${taskCount}개 남음", color = FocusColors.AccentBlue, fontFamily = MonoFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        IconActionButton(
            icon = Icons.Outlined.AddPhotoAlternate,
            contentDescription = "배경 사진",
            onClick = onPickBackground,
        )
    }
}

/** The head of the queue, led big — task name, start button, and its planned duration. */
@Composable
private fun PriorityCard(item: PlannedFocusEntity, onStart: () -> Unit) {
    val minutes = (item.plannedMs / 60_000L).toInt().coerceAtLeast(1)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xF2FFFFFF))
            .padding(22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(FocusColors.AccentBlue))
            Spacer(Modifier.width(6.dp))
            Text("우선 집중", color = FocusColors.AccentBlue, fontFamily = MonoFamily, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Top) {
            Text(
                item.label.ifBlank { "집중" },
                color = FocusColors.Ink,
                fontFamily = GothicFamily,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 29.sp,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(FocusColors.AccentDeep)
                    .clickable(onClick = onStart),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "시작", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = FocusColors.Muted, modifier = Modifier.size(14.dp))
            Text("${minutes}분", color = FocusColors.Muted, fontFamily = MonoFamily, fontSize = 12.5.sp)
        }
    }
}

/** One waiting item — tap the leading circle (or it) to start, the trailing × to drop it. */
@Composable
private fun PendingCard(item: PlannedFocusEntity, onStart: () -> Unit, onDelete: () -> Unit) {
    val minutes = (item.plannedMs / 60_000L).toInt().coerceAtLeast(1)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xF2FFFFFF))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(FocusColors.Mist)
                .clickable(onClick = onStart),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "시작", tint = FocusColors.AccentBlue, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(item.label.ifBlank { "집중" }, color = FocusColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = FocusColors.Muted, modifier = Modifier.size(12.dp))
                Text("${minutes}분", color = FocusColors.Muted, fontFamily = MonoFamily, fontSize = 11.5.sp)
            }
        }
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "삭제", tint = FocusColors.Muted2, modifier = Modifier.size(14.dp))
        }
    }
}

/** Dashed "new task" affordance — jumps focus down to the pinned add bar. */
@Composable
private fun AddTaskCard(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .dashedBorder(FocusColors.Muted2, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = FocusColors.Muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("새로운 작업", color = FocusColors.Muted, fontFamily = MonoFamily, fontSize = 12.5.sp)
    }
}

private fun Modifier.dashedBorder(color: Color, shape: RoundedCornerShape, width: androidx.compose.ui.unit.Dp = 1.5.dp) = drawBehind {
    val radiusPx = shape.topStart.toPx(size, this)
    drawRoundRect(
        color = color.copy(alpha = 0.45f),
        cornerRadius = CornerRadius(radiusPx, radiusPx),
        style = Stroke(width = width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)),
    )
}

/**
 * The add bar, pinned to the bottom as its own glass card. It sits just above the keyboard
 * while typing, and above the floating tab bar when the keyboard is closed.
 */
@Composable
private fun AddBar(
    label: String,
    onLabelChange: (String) -> Unit,
    previewLabel: String,
    previewMinutes: Int?,
    recentCompleted: List<RecentTask>,
    accentColor: Color?,
    focusRequester: FocusRequester,
    onSubmit: () -> Unit,
    onAddRecent: (RecentTask) -> Unit,
    onDismissRecent: (RecentTask) -> Unit,
) {
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color(0xF7FFFFFF))
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime).only(WindowInsetsSides.Bottom))
            .padding(start = 20.dp, top = 14.dp, end = 20.dp, bottom = if (keyboardOpen) 14.dp else 96.dp),
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

        if (recentCompleted.isNotEmpty()) {
            RecentTaskChips(
                tasks = recentCompleted,
                accentColor = accentColor,
                onAdd = onAddRecent,
                onDismiss = onDismissRecent,
            )
            Spacer(Modifier.height(10.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FocusColors.Mist)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                BasicTextField(
                    value = label,
                    onValueChange = onLabelChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
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
                onClick = onSubmit,
                accent = true,
                size = 48.dp,
            )
        }
    }
}

/**
 * "Add it again" chips for recently completed tasks — tap the label to drop one back into
 * today's queue, tap the small × to hide it from this row. Tinted from [accentColor] (sampled
 * from the header photo) when there is one, so the chips feel tied to the photo above.
 */
@Composable
private fun RecentTaskChips(
    tasks: List<RecentTask>,
    accentColor: Color?,
    onAdd: (RecentTask) -> Unit,
    onDismiss: (RecentTask) -> Unit,
) {
    val chipBg = accentColor?.copy(alpha = 0.18f) ?: FocusColors.Mist
    val chipInk = accentColor?.let { androidx.compose.ui.graphics.lerp(it, FocusColors.Ink, 0.4f) } ?: FocusColors.Ink2

    Column {
        Text(
            "다시 할까요?",
            color = FocusColors.Muted,
            fontFamily = MonoFamily,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tasks, key = { it.label }) { task ->
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(chipBg)
                        .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        Modifier.clickable { onAdd(task) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = chipInk, modifier = Modifier.size(13.dp))
                        Text(task.label, color = chipInk, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                        Text("${task.minutes}분", color = chipInk.copy(alpha = 0.7f), fontFamily = MonoFamily, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .clickable { onDismiss(task) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "숨기기",
                            tint = chipInk.copy(alpha = 0.6f),
                            modifier = Modifier.size(11.dp),
                        )
                    }
                }
            }
        }
    }
}
