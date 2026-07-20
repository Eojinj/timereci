package com.timereci.focus.ui.day

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.timereci.focus.data.PhotoRef
import com.timereci.focus.data.PhotoStorage
import com.timereci.focus.ui.model.SessionCard
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.MonoFamily
import com.timereci.focus.ui.theme.PhotoTones

/**
 * A day's sessions as a swipeable, full-bleed carousel (Instagram post style) instead of a
 * vertical list — swipe horizontally between sessions, tap the photo to open its receipt.
 * Comment and task are overlaid large and bold, like a caption, rather than the small
 * receipt-badge treatment used elsewhere.
 */
@Composable
fun DayScreen(
    onBack: () -> Unit,
    onOpenReceipt: (Long) -> Unit,
    viewModel: DayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { state.sessions.size.coerceAtLeast(1) })

    Box(
        Modifier
            .fillMaxSize()
            .background(FocusColors.Night),
    ) {
        if (state.sessions.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                DaySessionPage(
                    session = state.sessions[page],
                    onClick = { onOpenReceipt(state.sessions[page].id) },
                )
            }
        }

        // Sticky translucent header with a back button and an Instagram-style "1 / N" counter.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(FocusColors.Glass)
                    .border(1.dp, FocusColors.GlassBorder, RoundedCornerShape(11.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text("←", color = FocusColors.NightInk, fontSize = 18.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(state.title, color = FocusColors.NightInk, fontFamily = MonoFamily, fontSize = 14.sp)
                Text(
                    state.summary,
                    color = FocusColors.NightMuted,
                    fontFamily = MonoFamily,
                    fontSize = 10.5.sp,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            if (state.sessions.size > 1) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(FocusColors.Glass)
                        .border(1.dp, FocusColors.GlassBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        "${pagerState.currentPage + 1} / ${state.sessions.size}",
                        color = FocusColors.NightInk,
                        fontFamily = MonoFamily,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        // Dot page indicator, only when there are few enough sessions to read at a glance.
        if (state.sessions.size in 2..8) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(state.sessions.size) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        Modifier
                            .size(if (active) 7.dp else 6.dp)
                            .clip(CircleShape)
                            .background(FocusColors.NightInk.copy(alpha = if (active) 0.95f else 0.4f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySessionPage(session: SessionCard, onClick: () -> Unit) {
    var photoIndex by rememberSaveable(session.id) { mutableIntStateOf(0) }
    val photos = session.photos.ifEmpty { listOf(PhotoRef()) }
    val current = photos[photoIndex.coerceIn(0, photos.lastIndex)]
    val context = LocalContext.current

    Box(
        Modifier
            .fillMaxSize()
            .background(PhotoTones.brush(current.toneIndex))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        current.fileName?.let { name ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(PhotoStorage.fileIn(context, name)).crossfade(true).build(),
                contentDescription = session.task.ifBlank { "집중 사진" },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Scrims so the big caption text stays legible over any photo.
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.3f)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color(0x8018262F), Color(0x0018262F)))),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color(0x0018262F), Color(0xCC18262F)))),
        )

        // Top: stamp + focus badge (kept below the screen header, small and out of the way).
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 60.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(session.stamp, color = Color.White.copy(alpha = 0.9f), fontFamily = MonoFamily, fontSize = 12.5.sp)
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    "집중 ${session.focus}",
                    color = Color(0xFF1F3247),
                    fontFamily = MonoFamily,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // Bottom: big bold task title + big italic comment — an Instagram-caption treatment.
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 36.dp),
        ) {
            if (session.task.isNotBlank()) {
                Text(
                    session.task,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp,
                )
            }
            session.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                Text(
                    comment,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 18.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 25.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        // Multi-photo navigation within one session: narrow edge tap zones + dot row, kept
        // clear of most of the width so the pager's horizontal swipe still owns the page.
        if (photos.size > 1) {
            Row(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                photos.indices.forEach { i ->
                    val active = i == photoIndex
                    Box(
                        Modifier
                            .size(if (active) 7.dp else 6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (active) 0.95f else 0.5f)),
                    )
                }
            }
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight(0.5f)
                    .width(40.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { photoIndex = (photoIndex - 1 + photos.size) % photos.size },
            )
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(0.5f)
                    .width(40.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { photoIndex = (photoIndex + 1) % photos.size },
            )
        }
    }
}
