package com.timereci.focus.ui.publish

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.timereci.focus.data.PhotoRef
import com.timereci.focus.data.PhotoStorage
import com.timereci.focus.ui.components.PrimaryButton
import com.timereci.focus.ui.components.SecondaryButton
import com.timereci.focus.ui.components.grain
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.MonoFamily

/**
 * The publish moment: the just-earned card with the comment typed directly onto the photo
 * (task + comment overlaid), plus "add photo" / "discard" and the commit button. The whole
 * column scrolls and honors the keyboard inset so typing doesn't jump the layout around.
 */
@Composable
fun PublishScreen(
    onStored: () -> Unit,
    viewModel: PublishViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val aspect by viewModel.photoAspect.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::addPhoto) }

    Column(
        Modifier
            .fillMaxSize()
            .background(FocusColors.Night)
            .grain(0.05f)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "방금 담김",
            color = FocusColors.NightMuted,
            fontFamily = MonoFamily,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 14.dp, bottom = 14.dp),
        )

        // The card: photo (or gradient), with the comment editable right on top of it.
        val photos = ui.photos.ifEmpty { listOf(PhotoRef()) }
        val current = photos.first()
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspect.ratio)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (current.fileName == null) com.timereci.focus.ui.theme.PhotoTones.EmptyDark
                    else com.timereci.focus.ui.theme.PhotoTones.brush(current.toneIndex),
                ),
        ) {
            current.fileName?.let { name ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(PhotoStorage.fileIn(context, name)).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Scrims for legibility.
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.2f)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color(0x66182630), Color(0x00182630)))),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color(0x00182630), Color(0xE6111C26)))),
            )

            // Stamp + minutes badge.
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(ui.stamp, color = Color.White.copy(alpha = 0.9f), fontFamily = MonoFamily, fontSize = 11.5.sp)
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(ui.focus, color = Color(0xFF1F3247), fontFamily = MonoFamily, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Task + editable comment, layered onto the bottom of the photo.
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 22.dp),
            ) {
                if (ui.task.isNotBlank()) {
                    Text(
                        ui.task,
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 37.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                BasicTextField(
                    value = ui.comment,
                    onValueChange = viewModel::setComment,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 30.sp,
                    ),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (ui.comment.isEmpty()) {
                            Text(
                                "여기에 코멘트를 남겨보세요",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        inner()
                    },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(
                text = "사진 추가",
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onDark = true,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = "버리기",
                onClick = { viewModel.discard(onStored) },
                onDark = true,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))

        PrimaryButton(
            text = "피드에 담기",
            onClick = { viewModel.store(onStored) },
            container = FocusColors.AccentSky,
            content = FocusColors.Night2,
            enabled = !ui.saving,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
    }
}
