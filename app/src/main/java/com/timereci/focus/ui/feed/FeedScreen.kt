package com.timereci.focus.ui.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.timereci.focus.data.PhotoAspect
import com.timereci.focus.data.PhotoStorage
import com.timereci.focus.data.PlannedFocusEntity
import com.timereci.focus.ui.components.PrimaryButton
import com.timereci.focus.ui.components.SessionOverlayCard
import com.timereci.focus.ui.components.grainyBackground
import com.timereci.focus.ui.model.FeedDay
import com.timereci.focus.ui.model.FeedTile
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.GothicFamily
import com.timereci.focus.ui.theme.MonoFamily
import com.timereci.focus.ui.theme.PhotoTones
import com.timereci.focus.ui.timer.DURATION_PRESETS

private data class PendingDelete(val id: Long, val label: String)

@Composable
fun FeedScreen(
    onStartFocus: () -> Unit,
    onStartPlanned: (String, Int) -> Unit,
    onOpenDay: (Long) -> Unit,
    onOpenReceipt: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val aspect by viewModel.photoAspect.collectAsStateWithLifecycle()
    val planned by viewModel.planned.collectAsStateWithLifecycle()
    val rollSessions by viewModel.rollSessions.collectAsStateWithLifecycle()

    var mode by remember { mutableStateOf(FeedViewMode.GRID) }
    var pendingDelete by remember { mutableStateOf<PendingDelete?>(null) }
    var showAddPlanned by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .grainyBackground(
                base = FocusColors.BaseLight,
                blob = FocusColors.AccentDeep.copy(alpha = 0.14f),
            ),
    ) {
        Column(Modifier.fillMaxSize()) {
            FeedHeader(
                subtitle = (state as? FeedUiState.Content)?.subtitle ?: "0장",
                mode = mode,
                onToggleMode = { mode = if (mode == FeedViewMode.GRID) FeedViewMode.ROLL else FeedViewMode.GRID },
                onOpenSettings = onOpenSettings,
            )

            PlannedStrip(
                planned = planned,
                onStart = { p -> onStartPlanned(p.label, (p.plannedMs / 60_000L).toInt().coerceAtLeast(1)) },
                onDelete = { viewModel.deletePlanned(it) },
                onAdd = { showAddPlanned = true },
            )

            when {
                state is FeedUiState.Empty -> EmptyFeed()
                mode == FeedViewMode.ROLL -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                ) {
                    items(rollSessions, key = { it.id }) { session ->
                        SessionOverlayCard(
                            session = session,
                            aspect = aspect,
                            modifier = Modifier.fillMaxWidth(),
                            taskSize = 30.sp,
                            commentSize = 20.sp,
                            onClick = { onOpenReceipt(session.id) },
                            onLongClick = { pendingDelete = PendingDelete(session.id, session.task) },
                        )
                    }
                }
                state is FeedUiState.Content -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                ) {
                    items((state as FeedUiState.Content).days, key = { it.epochDay }) { day ->
                        DayBlock(
                            day = day,
                            aspect = aspect,
                            onOpenDay = { onOpenDay(day.epochDay) },
                            onLongPressTile = { tile -> pendingDelete = PendingDelete(tile.sessionId, tile.task) },
                        )
                    }
                }
                else -> Spacer(Modifier.fillMaxSize())
            }
        }

        // Sticky "집중 시작" action.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(bottom = 16.dp),
        ) {
            PrimaryButton(text = "집중 시작", onClick = onStartFocus, modifier = Modifier.fillMaxWidth())
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("이 집중을 삭제할까요?", fontWeight = FontWeight.Bold) },
            text = { Text(target.label.ifBlank { "제목 없는 집중" }) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target.id)
                    pendingDelete = null
                }) { Text("삭제", color = FocusColors.AccentBlue, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("취소", color = FocusColors.Muted) }
            },
        )
    }

    if (showAddPlanned) {
        AddPlannedDialog(
            onDismiss = { showAddPlanned = false },
            onConfirm = { label, minutes ->
                viewModel.addPlanned(label, minutes)
                showAddPlanned = false
            },
        )
    }
}

@Composable
private fun FeedHeader(
    subtitle: String,
    mode: FeedViewMode,
    onToggleMode: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(start = 20.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("집중", color = FocusColors.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
        Spacer(Modifier.width(10.dp))
        Text(subtitle, color = FocusColors.Muted, fontFamily = MonoFamily, fontSize = 11.5.sp)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggleMode) {
            Icon(
                if (mode == FeedViewMode.GRID) Icons.Outlined.ViewDay else Icons.Outlined.GridView,
                contentDescription = "보기 전환",
                tint = FocusColors.Ink2,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = "설정", tint = FocusColors.Muted)
        }
    }
}

/** Slim horizontal strip of today's planned focus intentions. */
@Composable
private fun PlannedStrip(
    planned: List<PlannedFocusEntity>,
    onStart: (PlannedFocusEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        planned.forEach { p ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, FocusColors.LineStrong, RoundedCornerShape(20.dp))
                    .clickable { onStart(p) }
                    .padding(start = 13.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(p.label.ifBlank { "집중" }, color = FocusColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("${(p.plannedMs / 60_000L).toInt()}분", color = FocusColors.Muted, fontFamily = MonoFamily, fontSize = 11.sp)
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .clickable { onDelete(p.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "삭제", tint = FocusColors.Muted2, modifier = Modifier.size(13.dp))
                }
            }
        }
        // Add chip.
        Row(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(FocusColors.Mist)
                .clickable(onClick = onAdd)
                .padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = FocusColors.AccentBlue, modifier = Modifier.size(15.dp))
            Text("오늘 할 집중", color = FocusColors.AccentBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AddPlannedDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var label by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf(25) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("오늘 할 집중", fontWeight = FontWeight.Bold) },
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
                        textStyle = TextStyle(color = FocusColors.Ink, fontSize = 15.sp),
                        decorationBox = { inner ->
                            if (label.isEmpty()) Text("무엇에 집중할까요?", color = FocusColors.Muted2, fontSize = 15.sp)
                            inner()
                        },
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DURATION_PRESETS.forEach { m ->
                        val active = m == minutes
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) FocusColors.AccentDeep else FocusColors.Mist)
                                .clickable { minutes = m }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            Text(
                                "${m}분",
                                color = if (active) FocusColors.Paper else FocusColors.Ink2,
                                fontFamily = MonoFamily,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(label.ifBlank { "집중" }, minutes) },
            ) { Text("추가", color = FocusColors.AccentBlue, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = FocusColors.Muted) }
        },
    )
}

@Composable
private fun EmptyFeed() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, FocusColors.LineStrong, RoundedCornerShape(16.dp)),
        )
        Spacer(Modifier.height(22.dp))
        Text(
            "아직 담긴 사진이 없어요.\n첫 집중을 마치면 여기 첫 장이 담깁니다.",
            color = FocusColors.Ink2,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DayBlock(
    day: FeedDay,
    aspect: PhotoAspect,
    onOpenDay: () -> Unit,
    onLongPressTile: (FeedTile) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 22.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenDay)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(day.dayLabel, color = FocusColors.Ink, fontFamily = MonoFamily, fontSize = 22.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(9.dp))
            Text(day.weekday, color = FocusColors.InkSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = GothicFamily)
            Spacer(Modifier.width(9.dp))
            Text(day.focusText, color = FocusColors.AccentBlue, fontFamily = MonoFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                    .height(1.dp)
                    .background(FocusColors.Line),
            )
            Text(day.sessionText, color = FocusColors.Muted2, fontFamily = MonoFamily, fontSize = 11.sp)
        }
        Spacer(Modifier.height(6.dp))
        PhotoGrid(day.tiles, aspect, onOpenDay, onLongPressTile)
    }
}

@Composable
private fun PhotoGrid(
    tiles: List<FeedTile>,
    aspect: PhotoAspect,
    onOpenDay: () -> Unit,
    onLongPressTile: (FeedTile) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        tiles.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                row.forEach { tile ->
                    PhotoTile(tile, aspect, Modifier.weight(1f), onOpenDay, { onLongPressTile(tile) })
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoTile(
    tile: FeedTile,
    aspect: PhotoAspect,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    Box(
        modifier
            .aspectRatio(aspect.ratio)
            // Empty slots are neutral grey so only real photos carry colour.
            .background(if (tile.photo.fileName == null) PhotoTones.EmptyLight else PhotoTones.brush(tile.photo.toneIndex))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        tile.photo.fileName?.let { name ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(PhotoStorage.fileIn(context, name)).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (tile.task.isNotBlank()) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0x00182630), Color(0xB3182630))))
                    .padding(horizontal = 7.dp, vertical = 6.dp),
            ) {
                Text(
                    tile.task,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        tile.moreLabel?.let { label ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x801F3145)),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = Color.White, fontFamily = MonoFamily, fontSize = 15.sp)
            }
        }
    }
}
