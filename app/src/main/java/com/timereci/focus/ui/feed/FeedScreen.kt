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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.timereci.focus.ui.model.FeedDay
import com.timereci.focus.ui.model.SessionCard
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.PhotoTones
import com.timereci.focus.ui.theme.patternPlaceholder
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/**
 * History (Merci v5 screen 8): each day is a block of that day's session tiles — a busy day
 * reads as a full grid, a quiet day as one tile. Tapping a tile opens Session Detail directly
 * (there's no separate "Day" screen anymore).
 */
@Composable
fun FeedScreen(
    onOpenReceipt: (Long) -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                item { Header(content.subtitle) }
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("This week", content.thisWeekText, Modifier.weight(1f))
                        StatCard("Day streak", "${content.dayStreak}", Modifier.weight(1f))
                    }
                }
                items(content.days, key = { it.epochDay }) { day ->
                    DayBlock(day = day, onOpenReceipt = onOpenReceipt)
                }
            }
            FeedUiState.Empty -> Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) { Header(""); EmptyFeed() }
            FeedUiState.Loading -> Box(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun Header(subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("History", color = FocusColors.Ink, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.03).sp)
        if (subtitle.isNotEmpty()) {
            Text(subtitle, color = FocusColors.Muted, fontSize = 14.5.sp, modifier = Modifier.padding(top = 2.dp))
        }
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

private fun relativeDayLabel(date: LocalDate): String = when (date) {
    LocalDate.now() -> "Today"
    LocalDate.now().minusDays(1) -> "Yesterday"
    else -> "${date.month.getDisplayName(JavaTextStyle.FULL, Locale.ENGLISH)} ${date.dayOfMonth}"
}

@Composable
private fun DayBlock(day: FeedDay, onOpenReceipt: (Long) -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(relativeDayLabel(day.date), color = FocusColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${day.focusText} · ${day.sessions.size} session${if (day.sessions.size == 1) "" else "s"}",
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

@Composable
private fun PhotoTile(session: SessionCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val photo = session.photos.first()
    val minutes = (session.focusMs / 60_000L).toInt().coerceAtLeast(1)
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (photo.fileName == null) Modifier.patternPlaceholder(photo.toneIndex)
                else Modifier.background(PhotoTones.brush(photo.toneIndex)),
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
            Text("${minutes}m", color = FocusColors.Ink, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyFeed() {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "No sessions yet.\nFinish your first focus and it'll show up here.",
            color = FocusColors.Ink2,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center,
        )
    }
}
