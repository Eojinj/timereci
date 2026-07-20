package com.timereci.focus.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.timereci.focus.data.PhotoAspect
import com.timereci.focus.data.PhotoRef
import com.timereci.focus.data.PhotoStorage
import com.timereci.focus.ui.model.SessionCard
import com.timereci.focus.ui.theme.MonoFamily
import com.timereci.focus.ui.theme.PhotoTones

/**
 * The full-width session card used by both the day carousel and the vertical roll: a photo
 * cropped to [aspect] with the stamp, minutes badge, task and comment laid *over* the image.
 * Multiple photos in one session are swiped via the narrow left/right tap zones.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionOverlayCard(
    session: SessionCard,
    aspect: PhotoAspect,
    modifier: Modifier = Modifier,
    taskSize: TextUnit = 44.sp,
    commentSize: TextUnit = 30.sp,
    cornerRadius: Dp = 0.dp,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    var photoIndex by rememberSaveable(session.id) { mutableIntStateOf(0) }
    val photos = session.photos.ifEmpty { listOf(PhotoRef()) }
    val current = photos[photoIndex.coerceIn(0, photos.lastIndex)]
    val context = LocalContext.current

    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(aspect.ratio)
            .clip(RoundedCornerShape(cornerRadius))
            .background(PhotoTones.brush(current.toneIndex))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
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
                .background(Brush.verticalGradient(listOf(Color(0x00182630), Color(0x59182630), Color(0xE6111C26)))),
        )

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
                Text(session.focus, color = Color(0xFF1F3247), fontFamily = MonoFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, bottom = 26.dp),
        ) {
            if (session.task.isNotBlank()) {
                Text(
                    session.task,
                    color = Color.White,
                    fontSize = taskSize,
                    fontWeight = FontWeight.Bold,
                    lineHeight = taskSize * 1.15f,
                    letterSpacing = (-0.5).sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
            }
            session.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                Text(
                    comment,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = commentSize,
                    fontWeight = FontWeight.Medium,
                    lineHeight = commentSize * 1.33f,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

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
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { photoIndex = (photoIndex - 1 + photos.size) % photos.size },
                    ),
            )
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(44.dp)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { photoIndex = (photoIndex + 1) % photos.size },
                    ),
            )
        }
    }
}
