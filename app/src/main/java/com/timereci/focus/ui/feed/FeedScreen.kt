package com.timereci.focus.ui.feed

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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.timereci.focus.ui.components.SessionOverlayCard
import com.timereci.focus.ui.components.grainyBackground
import com.timereci.focus.ui.model.FeedDay
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.MonoFamily
import com.timereci.focus.ui.theme.PhotoTones
import com.timereci.focus.ui.theme.patternPlaceholder

private data class PendingEdit(val id: Long, val task: String, val comment: String)

@Composable
fun FeedScreen(
    onOpenDay: (Long) -> Unit,
    onOpenReceipt: (Long) -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val aspect by viewModel.photoAspect.collectAsStateWithLifecycle()
    val rollSessions by viewModel.rollSessions.collectAsStateWithLifecycle()

    var mode by remember { mutableStateOf(FeedViewMode.STRIP) }
    var pendingEdit by remember { mutableStateOf<PendingEdit?>(null) }

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
                onToggleMode = { mode = if (mode == FeedViewMode.STRIP) FeedViewMode.ROLL else FeedViewMode.STRIP },
            )

            when {
                state is FeedUiState.Empty -> EmptyFeed()
                mode == FeedViewMode.ROLL -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                ) {
                    items(rollSessions, key = { it.id }) { session ->
                        SessionOverlayCard(
                            session = session,
                            aspect = aspect,
                            modifier = Modifier.fillMaxWidth(),
                            taskSize = 30.sp,
                            commentSize = 20.sp,
                            onClick = { onOpenReceipt(session.id) },
                            onLongClick = {
                                pendingEdit = PendingEdit(session.id, session.task, session.comment.orEmpty())
                            },
                        )
                    }
                }
                state is FeedUiState.Content -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                ) {
                    items((state as FeedUiState.Content).days, key = { it.epochDay }) { day ->
                        DayStripRow(day = day, onOpenDay = { onOpenDay(day.epochDay) })
                    }
                }
                else -> Spacer(Modifier.fillMaxSize())
            }
        }
    }

    pendingEdit?.let { target ->
        EditSessionDialog(
            target = target,
            onDismiss = { pendingEdit = null },
            onSave = { task, comment ->
                viewModel.updateSession(target.id, task, comment)
                pendingEdit = null
            },
            onDelete = {
                viewModel.delete(target.id)
                pendingEdit = null
            },
        )
    }
}

@Composable
private fun EditSessionDialog(
    target: PendingEdit,
    onDismiss: () -> Unit,
    onSave: (task: String, comment: String) -> Unit,
    onDelete: () -> Unit,
) {
    var task by remember(target.id) { mutableStateOf(target.task) }
    var comment by remember(target.id) { mutableStateOf(target.comment) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("집중 수정", fontWeight = FontWeight.Bold) },
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
                        value = task,
                        onValueChange = { task = it },
                        singleLine = true,
                        cursorBrush = SolidColor(FocusColors.AccentBlue),
                        textStyle = TextStyle(color = FocusColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        decorationBox = { inner ->
                            if (task.isEmpty()) Text("할 일", color = FocusColors.Muted2, fontSize = 15.sp)
                            inner()
                        },
                    )
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(FocusColors.Mist)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    BasicTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        cursorBrush = SolidColor(FocusColors.AccentBlue),
                        textStyle = TextStyle(color = FocusColors.Ink, fontSize = 14.sp, lineHeight = 20.sp),
                        decorationBox = { inner ->
                            if (comment.isEmpty()) Text("코멘트", color = FocusColors.Muted2, fontSize = 14.sp)
                            inner()
                        },
                    )
                }
            }
        },
        confirmButton = {
            IconActionButton(
                icon = Icons.Outlined.Check,
                contentDescription = "저장",
                onClick = { onSave(task, comment) },
                accent = true,
                size = 40.dp,
            )
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconActionButton(
                    icon = Icons.Outlined.DeleteOutline,
                    contentDescription = "삭제",
                    onClick = onDelete,
                    size = 40.dp,
                )
                IconActionButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = "취소",
                    onClick = onDismiss,
                    size = 40.dp,
                )
            }
        },
    )
}

@Composable
private fun FeedHeader(
    subtitle: String,
    mode: FeedViewMode,
    onToggleMode: () -> Unit,
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
                if (mode == FeedViewMode.STRIP) Icons.Outlined.ViewDay else Icons.Outlined.BarChart,
                contentDescription = "보기 전환",
                tint = FocusColors.Ink2,
            )
        }
    }
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

/** 8h of focus fills the bar — a full workday's worth reads as a "complete" strip. */
private const val STRIP_SCALE_MS = 8 * 60 * 60 * 1000L

/**
 * One day as a single proportional band: abbreviated date at the left, a bar whose overall
 * length is the day's focused time against an 8h scale — but instead of one solid fill, it's
 * built from one segment per session (oldest to newest, left to right), each sized by its own
 * share of the day's total, so the bar reads as pieces stitched end to end. Each piece shows
 * its own photo center-cropped, or the same varied placeholder pattern used elsewhere when the
 * session has none. Tapping the row opens that day's feed (photos live there in full).
 */
@Composable
private fun DayStripRow(day: FeedDay, onOpenDay: () -> Unit) {
    val context = LocalContext.current
    val fraction = (day.focusMs.toFloat() / STRIP_SCALE_MS).coerceIn(0.03f, 1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDay)
            .padding(horizontal = 20.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            day.dayLabel,
            color = FocusColors.Ink,
            fontFamily = MonoFamily,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(50.dp),
        )
        Box(
            Modifier
                .weight(1f)
                .height(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(FocusColors.Line),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction),
                horizontalArrangement = Arrangement.spacedBy(1.5.dp),
            ) {
                // Oldest first so the pieces read left-to-right like a timeline.
                day.sessions.asReversed().forEach { session ->
                    val photo = session.photos.first()
                    Box(
                        Modifier
                            .weight(session.focusMs.toFloat().coerceAtLeast(1f))
                            .fillMaxHeight()
                            .then(
                                if (photo.fileName == null) Modifier.patternPlaceholder(photo.toneIndex)
                                else Modifier.background(PhotoTones.brush(photo.toneIndex)),
                            ),
                    ) {
                        photo.fileName?.let { name ->
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(PhotoStorage.fileIn(context, name))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            day.focusText,
            color = FocusColors.Muted,
            fontFamily = MonoFamily,
            fontSize = 11.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(48.dp),
        )
    }
}
