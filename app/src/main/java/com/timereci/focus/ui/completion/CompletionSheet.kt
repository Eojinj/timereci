package com.timereci.focus.ui.completion

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.timereci.focus.data.PhotoStorage
import com.timereci.focus.ui.components.ChoosePhotoSheet
import com.timereci.focus.ui.i18n.LocalStrings
import com.timereci.focus.ui.theme.FocusColors

/**
 * Shown over whatever screen is current once a session completes. The session is already
 * saved by the time this appears, so there is no Save and no Discard — a note, a photo, a
 * break and the next task are all optional, and swiping the sheet away is the normal exit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletionSheet(
    onStartNext: () -> Unit,
    onTakeBreak: (minutes: Int) -> Unit,
    viewModel: CompletionViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val alarmActive by viewModel.alarmActive.collectAsStateWithLifecycle()
    val recentPhotos by viewModel.recentPhotos.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val context = LocalContext.current
    var showPhotoSheet by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::setPhoto) }

    if (!ui.visible) return

    ModalBottomSheet(
        onDismissRequest = viewModel::dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FocusColors.Paper,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                ui.focus,
                color = FocusColors.Ink,
                fontSize = 40.sp,
                fontWeight = FontWeight.SemiBold,
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
            Text(
                "${ui.task.ifBlank { strings.focusFallback }} · ${strings.sessionComplete}",
                color = FocusColors.Muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 18.dp),
            )

            if (alarmActive) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(FocusColors.AccentDeep)
                        .clickable(onClick = viewModel::stopAlarm),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.NotificationsOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(8.dp)
                    Text(
                        strings.stopAlarm,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(16.dp, vertical = true)
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FocusColors.Mist)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                BasicTextField(
                    value = ui.comment,
                    onValueChange = viewModel::setComment,
                    textStyle = TextStyle(
                        color = FocusColors.Ink,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                    ),
                    cursorBrush = SolidColor(FocusColors.AccentBlue),
                    modifier = Modifier.fillMaxWidth().height(62.dp),
                    decorationBox = { inner ->
                        if (ui.comment.isEmpty()) {
                            Text(
                                strings.noteSessionPlaceholder,
                                color = FocusColors.Muted2,
                                fontSize = 16.sp,
                            )
                        }
                        inner()
                    },
                )
            }

            Spacer(10.dp, vertical = true)

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FocusColors.Mist)
                    .clickable { showPhotoSheet = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val photoFile = ui.photo?.fileName
                if (photoFile != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(PhotoStorage.fileIn(context, photoFile))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
                    )
                } else {
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = null,
                        tint = FocusColors.AccentBlue,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        if (photoFile != null) strings.changePhoto else strings.addPhoto,
                        color = FocusColors.Ink,
                        fontSize = 16.sp,
                    )
                    if (photoFile == null) {
                        Text(strings.optional, color = FocusColors.Muted, fontSize = 12.5.sp)
                    }
                }
            }

            Spacer(18.dp, vertical = true)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(FocusColors.Mist)
                        .clickable { onTakeBreak(BREAK_MINUTES) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        strings.takeABreak,
                        color = FocusColors.AccentBlue,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                val next = ui.next
                if (next != null) {
                    Row(
                        Modifier
                            .weight(1.4f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(FocusColors.AccentDeep)
                            .clickable { viewModel.startNext(next); onStartNext() }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(6.dp)
                        Text(
                            next.label,
                            color = Color.White,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    if (showPhotoSheet) {
        ChoosePhotoSheet(
            recent = recentPhotos,
            selectedFileName = ui.photo?.fileName,
            onPickRecent = { ref -> viewModel.setExistingPhoto(ref); showPhotoSheet = false },
            onPickNew = {
                showPhotoSheet = false
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onRemove = if (ui.photo != null) ({ viewModel.removePhoto(); showPhotoSheet = false }) else null,
            onDismiss = { showPhotoSheet = false },
        )
    }
}

/** The one break length the sheet offers. Anything finer belongs in the break screen itself. */
private const val BREAK_MINUTES = 5

@Composable
private fun Spacer(size: androidx.compose.ui.unit.Dp, vertical: Boolean = false) {
    androidx.compose.foundation.layout.Spacer(
        if (vertical) Modifier.height(size) else Modifier.width(size),
    )
}
