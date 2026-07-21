package com.timereci.focus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.timereci.focus.data.PhotoRef
import com.timereci.focus.data.PhotoStorage
import com.timereci.focus.ui.theme.FocusColors

/**
 * Lets the user reuse a photo from a past session instead of always going through the system
 * picker — shown when there's at least one recent photo to offer.
 */
@Composable
fun RecentPhotoPickerDialog(
    recent: List<PhotoRef>,
    onPickRecent: (PhotoRef) -> Unit,
    onPickNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("사진 선택", fontWeight = FontWeight.Bold) },
        text = {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(FocusColors.Mist)
                            .clickable(onClick = onPickNew),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = "새 사진", tint = FocusColors.Ink2)
                    }
                }
                items(recent) { ref ->
                    val name = ref.fileName
                    if (name != null) {
                        Box(
                            Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPickRecent(ref) },
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(PhotoStorage.fileIn(context, name))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            IconActionButton(icon = Icons.Outlined.Close, contentDescription = "닫기", onClick = onDismiss, size = 40.dp)
        },
    )
}
