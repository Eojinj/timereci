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
import androidx.compose.foundation.shape.CircleShape
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

    // Full-screen dark page; the photo card (cropped to the chosen ratio) is centered and
    // ALL text is layered inside it, over the image itself — swipe left/right = carousel.
    Box(
        Modifier
            .fillMaxSize()
            .background(FocusColors.Night),
        contentAlignment = Alignment.Center,
    ) {
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

            // Scrims on the image so the overlaid text stays legible.
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.24f)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color(0x66182630), Color(0x00182630)))),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.62f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color(0x00182630), Color(0x59182630), Color(0xE6111C26))),
                    ),
            )

            // Stamp + focus badge — top of the photo.
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(session.stamp, color = Color.White.copy(alpha = 0.9f), fontFamily = MonoFamily, fontSize = 12.5.sp)
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

            // Task + big comment — layered over the bottom of the photo.
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 22.dp),
            ) {
                if (session.task.isNotBlank()) {
                    Text(
                        session.task,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 33.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                session.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                    Text(
                        comment,
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 22.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 32.sp,
                    )
                }
            }

            // Multi-photo within one session: dot indicator + narrow edge tap zones.
            if (photos.size > 1) {
                Row(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 46.dp),
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
    }
}
