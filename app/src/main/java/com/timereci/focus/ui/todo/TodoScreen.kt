package com.timereci.focus.ui.todo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.timereci.focus.data.PhotoStorage
import com.timereci.focus.data.PlannedFocusEntity
import com.timereci.focus.ui.components.IconActionButton
import com.timereci.focus.ui.components.grainyBackground
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.MonoFamily
import com.timereci.focus.ui.util.Formatters
import com.timereci.focus.ui.util.PhotoPalette
import com.timereci.focus.ui.util.QuickEntry
import java.time.LocalDate

/** How much the rounded list sheet overlaps the header photo below it. */
private val SHEET_OVERLAP = 24.dp

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

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(Modifier.fillMaxSize()) {
        // Full-bleed background behind the whole screen — the custom photo when set, not just
        // the header strip — or the app's usual gradient.
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
                    .grainyBackground(
                        base = FocusColors.BaseLight,
                        blob = FocusColors.AccentDeep.copy(alpha = 0.12f),
                    ),
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                // Only sides here — the header handles the top inset, the add bar the bottom.
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            TodoHeader(
                hasBackground = background != null,
                accentColor = accentColor,
                onPickBackground = {
                    backgroundPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onClearBackground = viewModel::clearBackground,
            )

            // The list + add bar live in a rounded "sheet" that overlaps the header photo by a
            // little, so the seam reads as a deliberate layer instead of a flat photo-to-grey
            // cut. Translucent (not solid) when there's a custom photo, so it keeps showing
            // through behind the list too instead of only in the header strip.
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .offset(y = (-SHEET_OVERLAP))
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(if (background != null) Color(0xE6FFFFFF) else FocusColors.Paper),
            ) {
                if (planned.isEmpty()) {
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = SHEET_OVERLAP + 20.dp),
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
                        contentPadding = PaddingValues(start = 20.dp, top = SHEET_OVERLAP + 4.dp, end = 20.dp, bottom = 4.dp),
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

                // Add bar, pinned to the bottom. It sits just above the keyboard while typing,
                // and above the floating tab bar when the keyboard is closed.
                val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                Column(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime).only(WindowInsetsSides.Bottom))
                        .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = if (keyboardOpen) 12.dp else 92.dp),
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
                            onAdd = { task -> viewModel.add(task.label, task.minutes) },
                            onDismiss = { task -> viewModel.dismissRecent(task.label) },
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
        }
    }
}

/**
 * Big title + today's date over a full-bleed header photo — the user's own picked photo when
 * set, otherwise the app's usual soft gradient. The small button lets them set/change/clear it.
 */
@Composable
private fun TodoHeader(
    hasBackground: Boolean,
    accentColor: Color?,
    onPickBackground: () -> Unit,
    onClearBackground: () -> Unit,
) {
    var showBackgroundMenu by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }

    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp),
    ) {
        if (hasBackground) {
            // Scrim at the top (where the title sits), tinted from the photo's own accent
            // color when we have one so it reads as part of the photo, not a generic overlay.
            // The photo itself is drawn once behind the whole screen, not here.
            val scrimTop = accentColor?.let { lerp(it, Color.Black, 0.55f) } ?: Color(0xFF101820)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(scrimTop.copy(alpha = 0.8f), scrimTop.copy(alpha = 0.05f), Color.Transparent),
                        ),
                    ),
            )
        }

        Row(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(start = 20.dp, end = 12.dp, top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "오늘 할 일",
                    color = if (hasBackground) Color.White else FocusColors.Ink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    Formatters.monthDayWeekday(today),
                    color = if (hasBackground) Color(0xE6FFFFFF) else FocusColors.Muted,
                    fontSize = 12.5.sp,
                )
            }
            IconActionButton(
                icon = Icons.Outlined.AddPhotoAlternate,
                contentDescription = "배경 사진",
                onClick = { showBackgroundMenu = true },
                onDark = hasBackground,
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
                        onPickBackground()
                    },
                    accent = true,
                    size = 40.dp,
                )
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasBackground) {
                        IconActionButton(
                            icon = Icons.Outlined.Close,
                            contentDescription = "기본으로",
                            onClick = {
                                showBackgroundMenu = false
                                onClearBackground()
                            },
                            size = 40.dp,
                        )
                    }
                }
            },
        )
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
    val chipInk = accentColor?.let { lerp(it, FocusColors.Ink, 0.4f) } ?: FocusColors.Ink2

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

@Composable
private fun TodoRow(item: PlannedFocusEntity, onStart: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FocusColors.Mist)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.label.ifBlank { "집중" },
                color = FocusColors.Ink,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                "${(item.plannedMs / 60_000L).toInt()}분",
                color = FocusColors.Muted,
                fontFamily = MonoFamily,
                fontSize = 11.5.sp,
            )
        }
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "삭제", tint = FocusColors.Muted2, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(8.dp))
        IconActionButton(
            icon = Icons.Filled.PlayArrow,
            contentDescription = "시작",
            onClick = onStart,
            accent = true,
            size = 34.dp,
        )
    }
}
