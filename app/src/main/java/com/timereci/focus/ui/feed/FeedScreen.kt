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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.timereci.focus.ui.components.PrimaryButton
import com.timereci.focus.ui.model.FeedDay
import com.timereci.focus.ui.model.FeedTile
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.MonoFamily
import com.timereci.focus.ui.theme.PhotoTones
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars

@Composable
fun FeedScreen(
    onStartFocus: () -> Unit,
    onOpenDay: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        Modifier
            .fillMaxSize()
            .background(FocusColors.Paper),
    ) {
        Column(Modifier.fillMaxSize()) {
            FeedHeader(
                subtitle = (state as? FeedUiState.Content)?.subtitle ?: "0장",
                onOpenSettings = onOpenSettings,
            )

            when (val s = state) {
                FeedUiState.Loading -> Spacer(Modifier.fillMaxSize())
                FeedUiState.Empty -> EmptyFeed()
                is FeedUiState.Content -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                ) {
                    items(s.days, key = { it.epochDay }) { day ->
                        DayBlock(day = day, onClick = { onOpenDay(day.epochDay) })
                    }
                }
            }
        }

        // Sticky "집중 시작" action with a fade scrim.
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
}

@Composable
private fun FeedHeader(subtitle: String, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("집중", color = FocusColors.Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                color = FocusColors.Muted,
                fontFamily = MonoFamily,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = "설정", tint = FocusColors.Muted)
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

@Composable
private fun DayBlock(day: FeedDay, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
            .padding(bottom = 26.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                day.dayLabel,
                color = FocusColors.Ink,
                fontFamily = MonoFamily,
                fontSize = 21.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(10.dp))
            Text(day.weekday, color = FocusColors.InkSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                    .height(1.dp)
                    .background(FocusColors.Line),
            )
            Text(day.summary, color = FocusColors.Muted2, fontFamily = MonoFamily, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        PhotoGrid(day.tiles)
    }
}

/** 3-column grid clipped to a single rounded panel, matching the prototype. */
@Composable
private fun PhotoGrid(tiles: List<FeedTile>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        tiles.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                row.forEach { tile ->
                    PhotoTile(tile, Modifier.weight(1f))
                }
                // Pad short rows so the last row's tiles keep 1/3 width.
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PhotoTile(tile: FeedTile, modifier: Modifier) {
    val context = LocalContext.current
    Box(
        modifier
            .aspectRatio(1f)
            .background(PhotoTones.brush(tile.photo.toneIndex)),
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
