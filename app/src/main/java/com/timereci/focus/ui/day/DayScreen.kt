package com.timereci.focus.ui.day

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.timereci.focus.data.PhotoAspect
import com.timereci.focus.data.PhotoRef
import com.timereci.focus.data.PhotoStorage
import com.timereci.focus.ui.model.SessionCard
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.MonoFamily
import com.timereci.focus.ui.theme.PhotoTones

/**
 * A day's sessions as a swipeable carousel (Instagram post style): swipe horizontally
 * between sessions. Each page shows the photo cropped to the chosen aspect ratio with the
 * task + comment rendered large below it, like a caption.
 */
@Composable
fun DayScreen(
    onBack: () -> Unit,
    onOpenReceipt: (Long) -> Unit,
    viewModel: DayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val aspect by viewModel.photoAspect.collectAsStateWithLifecycle()
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
                    aspect = aspect,
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
                    .clip(CircleShape)
                    .background(FocusColors.Glass)
                    .border(1.dp, FocusColors.GlassBorder, CircleShape)
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
                        .clip(CircleShape)
                        .background(FocusColors.Glass)
                        .border(1.dp, FocusColors.GlassBorder, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
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
    }
}

@Composable
private fun DaySessionPage(session: SessionCard, aspect: PhotoAspect, onClick: () -> Unit) {
    var photoIndex by rememberSaveable(session.id) { mutableIntStateOf(0) }
    val photos = session.photos.ifEmpty { listOf(PhotoRef()) }
    val current = photos[photoIndex.coerceIn(0, photos.lastIndex)]
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .background(FocusColors.Night),
    ) {
        // ── Photo, cropped to the chosen aspect ratio, edge to edge ──
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspect.ratio)
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

            // Top scrim so the screen header stays legible over the photo.
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.28f)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color(0x8018262F), Color(0x0018262F)))),
            )
            // Bottom scrim + stamp/focus badge.
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.32f)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color(0x0018262F), Color(0x9918262F)))),
            )
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(session.stamp, color = Color.White.copy(alpha = 0.92f), fontFamily = MonoFamily, fontSize = 12.5.sp)
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(horizontal = 11.dp, vertical = 5.dp),
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

            // Multi-photo: dots (top) + left/right tap zones (kept narrow so the pager keeps the swipe).
            if (photos.size > 1) {
                Row(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 70.dp),
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
                        .fillMaxHeight()
                        .width(44.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { photoIndex = (photoIndex - 1 + photos.size) % photos.size },
                )
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(44.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { photoIndex = (photoIndex + 1) % photos.size },
                )
            }
        }

        // ── Caption: task + big comment, like an Instagram post caption ──
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 22.dp),
        ) {
            if (session.task.isNotBlank()) {
                Text(
                    session.task,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 31.sp,
                )
                Spacer(Modifier.height(12.dp))
            }
            session.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                Text(
                    comment,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 22.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 33.sp,
                )
            }
        }
    }
}
