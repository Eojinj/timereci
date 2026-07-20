package com.timereci.focus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.timereci.focus.data.PhotoRef
import com.timereci.focus.data.PhotoStorage
import com.timereci.focus.ui.theme.MonoFamily
import com.timereci.focus.ui.theme.PhotoTones

/**
 * The signature card: a full-bleed photo (or calm gradient placeholder) stamped with time,
 * focused duration and the task label — a faithful port of PhotoCard.dc.html. Swipes
 * through multiple photos via the side tap zones.
 */
@Composable
fun PhotoCard(
    photos: List<PhotoRef>,
    stamp: String,
    task: String,
    focus: String,
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 0.dp,
    onClick: (() -> Unit)? = null,
) {
    val safePhotos = if (photos.isEmpty()) listOf(PhotoRef()) else photos
    var index by rememberSaveable(safePhotos.size) { mutableIntStateOf(0) }
    val current = safePhotos[index.coerceIn(0, safePhotos.lastIndex)]
    val context = LocalContext.current

    val shape = RoundedCornerShape(cornerRadius)
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )
    } else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .background(PhotoTones.brush(current.toneIndex))
            .then(clickModifier),
    ) {
        // Real photo, drawn over the gradient when present.
        current.fileName?.let { name ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(PhotoStorage.fileIn(context, name))
                    .crossfade(true)
                    .build(),
                contentDescription = task.ifBlank { "집중 사진" },
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Soft highlight + top/bottom scrims for text legibility.
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.22f)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color(0x6B18262F), Color(0x0018262F)))),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.34f)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color(0x0018262F), Color(0x8C18262F)))),
        )

        // Placeholder label when there is no real photo.
        if (current.fileName == null) {
            Text(
                text = "◦ ${placeholderLabel(current.toneIndex)}",
                color = Color.White.copy(alpha = 0.62f),
                fontFamily = MonoFamily,
                fontSize = 10.5.sp,
                letterSpacing = 1.5.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Top row: stamp + focus badge.
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 17.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = stamp,
                color = Color.White.copy(alpha = 0.94f),
                fontFamily = MonoFamily,
                fontSize = 11.5.sp,
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.86f))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Text(
                    text = focus,
                    color = Color(0xFF1F3247),
                    fontFamily = MonoFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // Task label, bottom-left.
        if (task.isNotBlank()) {
            Text(
                text = task,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 23.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, bottom = 17.dp),
            )
        }

        // Multi-photo: dots + tap zones.
        if (safePhotos.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                safePhotos.indices.forEach { i ->
                    val active = i == index
                    Box(
                        Modifier
                            .height(6.dp)
                            .width(if (active) 14.dp else 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = if (active) 0.95f else 0.5f)),
                    )
                }
            }
            // Left / right tap zones.
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { index = (index - 1 + safePhotos.size) % safePhotos.size },
                contentAlignment = Alignment.Center,
            ) {
                Text("‹", color = Color.White.copy(alpha = 0.9f), fontSize = 24.sp)
            }
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { index = (index + 1) % safePhotos.size },
                contentAlignment = Alignment.Center,
            ) {
                Text("›", color = Color.White.copy(alpha = 0.9f), fontSize = 24.sp)
            }
        }
    }
}

private fun placeholderLabel(tone: Int): String = when (tone % 3) {
    0 -> "사진 없음"
    1 -> "기록"
    else -> "메모"
}
