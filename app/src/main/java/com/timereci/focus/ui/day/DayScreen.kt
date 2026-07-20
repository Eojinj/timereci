package com.timereci.focus.ui.day

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timereci.focus.data.PhotoAspect
import com.timereci.focus.ui.components.SessionOverlayCard
import com.timereci.focus.ui.model.SessionCard
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.GothicFamily
import com.timereci.focus.ui.theme.MonoFamily

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

    var pendingDelete by remember { mutableStateOf<SessionCard?>(null) }

    // If every session in this day gets deleted, leave the (now empty) day view.
    var hadContent by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.sessions.size) {
        if (state.sessions.isNotEmpty()) hadContent = true
        else if (hadContent) onBack()
    }

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
                    onLongClick = { pendingDelete = state.sessions[page] },
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

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("이 집중을 삭제할까요?", fontFamily = GothicFamily, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    target.task.ifBlank { "제목 없는 집중" } + " · " + target.stamp,
                    fontFamily = GothicFamily,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target.id)
                    pendingDelete = null
                }) { Text("삭제", color = FocusColors.AccentBlue, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("취소", color = FocusColors.Muted)
                }
            },
        )
    }
}

@Composable
private fun DaySessionPage(
    session: SessionCard,
    aspect: PhotoAspect,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    // Full-screen dark page; the aspect-cropped card is centered, text overlaid on the photo.
    Box(
        Modifier
            .fillMaxSize()
            .background(FocusColors.Night),
        contentAlignment = Alignment.Center,
    ) {
        SessionOverlayCard(
            session = session,
            aspect = aspect,
            modifier = Modifier.fillMaxWidth(),
            taskSize = 44.sp,
            commentSize = 30.sp,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}
