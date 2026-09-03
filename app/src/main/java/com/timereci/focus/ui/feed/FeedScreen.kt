package com.timereci.focus.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.timereci.focus.data.PhotoStorage
import com.timereci.focus.ui.i18n.LocalStrings
import com.timereci.focus.ui.i18n.Strings
import com.timereci.focus.ui.i18n.durationOf
import com.timereci.focus.ui.model.FeedDay
import com.timereci.focus.ui.model.SessionCard
import com.timereci.focus.ui.theme.FocusColors
import java.time.LocalDate

private enum class HistoryViewMode { GALLERY, COMMENTS }

/**
 * History (Merci v5 screen 8): each day is a block of that day's session tiles — a busy day
 * reads as a full grid, a quiet day as one tile. Tapping a tile opens Session Detail directly
 * (there's no separate "Day" screen anymore). A toggle switches to a text-only list of just
 * the comments, for reading back over what you wrote without the photos.
 */
@Composable
fun FeedScreen(
    onOpenReceipt: (Long) -> Unit,
    onOpenStats: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf(HistoryViewMode.GALLERY) }
    val strings = LocalStrings.current

    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val brush = Brush.radialGradient(
                    0f to Color(0xFF6FA8DE),
                    0.5f to Color(0xFFBEE0F5),
                    1f to Color(0xFFF7F9FB),
                    center = Offset(size.width / 2f, size.height * 0.1f),
                    radius = size.minDimension * 0.9f,
                )
                onDrawBehind { drawRect(brush) }
            },
    ) {
        when (val content = state) {
            is FeedUiState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars),
                contentPadding = PaddingValues(bottom = 110.dp),
            ) {
                item {
                    Header(
                        subtitle = strings.historySubtitle(
                            strings.monthName(content.month),
                            strings.sessionsCount(content.sessionCount),
                        ),
                        mode = mode,
                        onToggleMode = { mode = if (mode == HistoryViewMode.GALLERY) HistoryViewMode.COMMENTS else HistoryViewMode.GALLERY },
                        onOpenStats = onOpenStats,
                    )
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(strings.thisWeek, strings.durationOf(content.thisWeekMs), Modifier.weight(1f))
                        StatCard(strings.dayStreak, strings.daysCount(content.dayStreak), Modifier.weight(1f))
                    }
                }
                items(content.days, key = { it.epochDay }) { day ->
                    if (mode == HistoryViewMode.GALLERY) {
                        DayBlock(day = day, onOpenReceipt = onOpenReceipt)
                    } else {
                        DayCommentsBlock(day = day, onOpenReceipt = onOpenReceipt)
                    }
                }
            }
            FeedUiState.Empty -> Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                Header(subtitle = "", mode = mode, onToggleMode = {}, onOpenStats = onOpenStats)
                EmptyFeed()
            }
            FeedUiState.Loading -> Box(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun Header(subtitle: String, mode: HistoryViewMode, onToggleMode: () -> Unit, onOpenStats: () -> Unit) {
    val strings = LocalStrings.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(strings.historyTitle, color = FocusColors.Ink, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.03).sp)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = FocusColors.Muted, fontSize = 14.5.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Icon(
            if (mode == HistoryViewMode.GALLERY) Icons.Outlined.Notes else Icons.Outlined.GridView,
            contentDescription = if (mode == HistoryViewMode.GALLERY) strings.showCommentsOnly else strings.showGallery,
            tint = FocusColors.AccentBlue,
            modifier = Modifier
                .padding(bottom = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onToggleMode)
                .padding(6.dp),
        )
        Icon(
            Icons.Outlined.BarChart,
            contentDescription = strings.statsAction,
            tint = FocusColors.AccentBlue,
            modifier = Modifier
                .padding(bottom = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenStats)
                .padding(6.dp),
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xEBFFFFFF))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(label, color = FocusColors.Muted, fontSize = 12.5.sp)
        Text(value, color = FocusColors.Ink, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 1.dp))
    }
}

private fun relativeDayLabel(date: LocalDate, strings: Strings): String = when (date) {
    LocalDate.now() -> strings.relativeToday
    LocalDate.now().minusDays(1) -> strings.relativeYesterday
    else -> strings.monthDay(date)
}

@Composable
private fun DayBlock(day: FeedDay, onOpenReceipt: (Long) -> Unit) {
    val strings = LocalStrings.current
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(relativeDayLabel(day.date, strings), color = FocusColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                strings.daySummary(strings.durationOf(day.focusMs), strings.sessionsCount(day.sessions.size)),
                color = FocusColors.Muted,
                fontSize = 12.5.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            day.sessions.chunked(3).forEach { rowSessions ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowSessions.forEach { session ->
                        PhotoTile(session = session, onClick = { onOpenReceipt(session.id) }, modifier = Modifier.weight(1f))
                    }
                    repeat(3 - rowSessions.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** The same day, as a text-only list of what was written — no photos, just the comments. */
@Composable
private fun DayCommentsBlock(day: FeedDay, onOpenReceipt: (Long) -> Unit) {
    val strings = LocalStrings.current
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(relativeDayLabel(day.date, strings), color = FocusColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                strings.daySummary(strings.durationOf(day.focusMs), strings.sessionsCount(day.sessions.size)),
                color = FocusColors.Muted,
                fontSize = 12.5.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xEBFFFFFF)),
        ) {
            day.sessions.forEachIndexed { index, session ->
                if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(FocusColors.LineSoft))
                CommentRow(session = session, onClick = { onOpenReceipt(session.id) })
            }
        }
    }
}

@Composable
private fun CommentRow(session: SessionCard, onClick: () -> Unit) {
    val strings = LocalStrings.current
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(session.task.ifBlank { strings.focusFallback }, color = FocusColors.Ink, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
            Text(session.stamp, color = FocusColors.Muted, fontSize = 12.sp)
        }
        val comment = session.comment
        Text(
            if (comment.isNullOrBlank()) strings.noNote else comment,
            color = if (comment.isNullOrBlank()) FocusColors.Muted2 else FocusColors.Ink2,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun PhotoTile(session: SessionCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val photo = session.photos.first()
    val minutes = (session.focusMs / 60_000L).toInt().coerceAtLeast(1)
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .then(
                Modifier.background(FocusColors.Mist),
            )
            .clickable(onClick = onClick),
    ) {
        photo.fileName?.let { name ->
            AsyncImage(
                model = ImageRequest.Builder(context).data(PhotoStorage.fileIn(context, name)).crossfade(true).build(),
                contentDescription = session.task,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xE6FFFFFF))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(strings.minutesCompact(minutes), color = FocusColors.Ink, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyFeed() {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            strings.emptyFeed,
            color = FocusColors.Ink2,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center,
        )
    }
}
