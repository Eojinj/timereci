package com.timereci.focus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.timereci.focus.data.PhotoRef
import com.timereci.focus.data.PhotoStorage
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.PhotoTones
import com.timereci.focus.ui.theme.patternPlaceholder

/**
 * The one photo-picking sheet in the app (Merci v5 screen 3): a grid of recently-used photos
 * (tap one to reuse it, matching whichever is already selected), "Choose from Library" for the
 * system picker, and — only when a photo is currently set — "Remove Photo". Shared by Quick
 * Start (background photo) and Session Complete (the receipt's photo).
 */
@Composable
fun ChoosePhotoSheet(
    recent: List<PhotoRef>,
    selectedFileName: String?,
    onPickRecent: (PhotoRef) -> Unit,
    onPickNew: () -> Unit,
    onRemove: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x571E376F))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color(0xFFF4F8FE))
                .clickable(enabled = false) {}
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 18.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .width(36.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(FocusColors.LineStrong),
            )
            Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(48.dp)) {
                Text(
                    "Cancel",
                    color = FocusColors.AccentBlue,
                    fontSize = 16.5.sp,
                    modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onDismiss),
                )
                Text(
                    "Choose Photo",
                    color = FocusColors.Ink,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            if (recent.isNotEmpty()) {
                Text(
                    "RECENTLY USED",
                    color = FocusColors.Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 8.dp),
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth().height(if (recent.size > 3) 220.dp else 108.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(recent, key = { it.fileName ?: it.hashCode() }) { ref ->
                        val name = ref.fileName
                        val selected = name != null && name == selectedFileName
                        Box(
                            Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .then(
                                    if (name == null) Modifier.patternPlaceholder(ref.toneIndex)
                                    else Modifier.background(PhotoTones.brush(ref.toneIndex)),
                                )
                                .clickable { onPickRecent(ref) },
                        ) {
                            name?.let {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(PhotoStorage.fileIn(context, it)).crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            if (selected) {
                                Box(
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                        .size(21.dp)
                                        .clip(CircleShape)
                                        .background(FocusColors.AccentBlue),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xEBFFFFFF)),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onPickNew)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = FocusColors.AccentBlue, modifier = Modifier.size(20.dp))
                    Text("Choose from Library", color = FocusColors.AccentBlue, fontSize = 16.5.sp)
                }
                if (onRemove != null) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(FocusColors.LineSoft))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onRemove)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = FocusColors.Danger, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Remove Photo", color = FocusColors.Danger, fontSize = 16.5.sp)
                    }
                }
            }
            Text(
                "Photos are picked with the system picker — the app never asks for storage access.",
                color = FocusColors.Muted,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp),
            )
        }
    }
}
