package com.timereci.focus.ui.publish

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.timereci.focus.data.PlannedFocusEntity
import com.timereci.focus.ui.components.ChoosePhotoSheet
import com.timereci.focus.ui.theme.FocusColors

/**
 * Session Complete (Merci v5 screen 5). No swipe card — Discard/Save are labeled nav actions,
 * and the photo row opens the shared Choose Photo sheet.
 */
@Composable
fun PublishScreen(
    onFinished: (PlannedFocusEntity?) -> Unit,
    viewModel: PublishViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val recentPhotos by viewModel.recentPhotos.collectAsStateWithLifecycle()
    val alarmActive by viewModel.alarmActive.collectAsStateWithLifecycle()
    val askForPhoto by viewModel.askForPhotoAfterSession.collectAsStateWithLifecycle()
    var showPhotoSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::setPhoto) }
    fun launchSystemPicker() {
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    LaunchedEffect(askForPhoto) {
        if (askForPhoto && ui.photo == null) launchSystemPicker()
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val brush = Brush.radialGradient(
                        0f to Color(0xFF6FA8DE),
                        0.5f to Color(0xFFBEE0F5),
                        1f to Color(0xFFFFFFFF),
                        center = Offset(size.width / 2f, size.height * 0.15f),
                        radius = size.minDimension * 0.8f,
                    )
                    onDrawBehind { drawRect(brush) }
                },
        )

        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(46.dp)) {
                Text(
                    "Discard",
                    color = FocusColors.Danger,
                    fontSize = 16.5.sp,
                    modifier = Modifier.align(Alignment.CenterStart).clickable { viewModel.discard(onFinished) },
                )
                Text("Session Complete", color = FocusColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center))
                Text(
                    "Save",
                    color = FocusColors.AccentBlue,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterEnd).clickable(enabled = !ui.saving) { viewModel.store(onFinished) },
                )
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        ui.focus,
                        color = FocusColors.Ink,
                        fontSize = 50.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                    )
                    Text(
                        "${ui.task.ifBlank { "Focus" }} · ${ui.stamp}",
                        color = FocusColors.Muted,
                        fontSize = 14.5.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                SectionLabel("PHOTO")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xEBFFFFFF))
                        .clickable { showPhotoSheet = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val photoFile = ui.photo?.fileName
                    if (photoFile != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(PhotoStorage.fileIn(context, photoFile)).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
                        )
                    } else {
                        Icon(Icons.Outlined.Image, contentDescription = null, tint = FocusColors.AccentBlue, modifier = Modifier.size(28.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(if (photoFile != null) "Change Photo" else "Add Photo", color = FocusColors.Ink, fontSize = 16.sp)
                        if (photoFile == null) {
                            Text("Optional", color = FocusColors.Muted, fontSize = 12.5.sp)
                        }
                    }
                }

                SectionLabel("NOTE")
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xEBFFFFFF))
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                ) {
                    BasicTextField(
                        value = ui.comment,
                        onValueChange = viewModel::setComment,
                        textStyle = TextStyle(color = FocusColors.Ink, fontSize = 16.sp, lineHeight = 22.sp),
                        cursorBrush = SolidColor(FocusColors.AccentBlue),
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        decorationBox = { inner ->
                            if (ui.comment.isEmpty()) {
                                Text("Add a note about this session…", color = FocusColors.Muted2, fontSize = 16.sp)
                            }
                            inner()
                        },
                    )
                }

                Text(
                    "Saved sessions appear in History. Discarded sessions are never recorded.",
                    color = FocusColors.Muted,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp),
                )

                if (alarmActive) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xCCFFFFFF))
                            .border(1.dp, FocusColors.Line, RoundedCornerShape(20.dp))
                            .clickable(onClick = viewModel::stopAlarm)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Outlined.NotificationsOff, contentDescription = null, tint = FocusColors.Ink2, modifier = Modifier)
                        Text("Stop Alarm", color = FocusColors.Ink2, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showPhotoSheet) {
        ChoosePhotoSheet(
            recent = recentPhotos,
            selectedFileName = ui.photo?.fileName,
            onPickRecent = { ref -> viewModel.setExistingPhoto(ref); showPhotoSheet = false },
            onPickNew = { showPhotoSheet = false; launchSystemPicker() },
            onRemove = if (ui.photo != null) ({ viewModel.removePhoto(); showPhotoSheet = false }) else null,
            onDismiss = { showPhotoSheet = false },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = FocusColors.Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp, start = 4.dp),
    )
}
