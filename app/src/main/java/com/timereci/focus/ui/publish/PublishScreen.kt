package com.timereci.focus.ui.publish

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timereci.focus.ui.components.PhotoCard
import com.timereci.focus.ui.components.PrimaryButton
import com.timereci.focus.ui.components.SecondaryButton
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.MonoFamily

/** The publish moment: the just-earned card, with room to add a photo or a closing note. */
@Composable
fun PublishScreen(
    onStored: () -> Unit,
    viewModel: PublishViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::addPhoto) }

    Column(
        Modifier
            .fillMaxSize()
            .background(FocusColors.Night)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "방금 담김",
            color = FocusColors.NightMuted,
            fontFamily = MonoFamily,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
        )

        Box(
            Modifier
                .weight(1f)
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            PhotoCard(
                photos = ui.photos,
                stamp = ui.stamp,
                task = ui.task,
                focus = ui.focus,
                cornerRadius = 20.dp,
                modifier = Modifier
                    .width(320.dp)
                    .height(420.dp),
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CommentField(value = ui.comment, onChange = viewModel::setComment)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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

            PrimaryButton(
                text = "피드에 담기",
                onClick = { viewModel.store(onStored) },
                container = FocusColors.AccentSky,
                content = FocusColors.Night2,
                enabled = !ui.saving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CommentField(value: String, onChange: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FocusColors.Glass)
            .border(1.dp, FocusColors.GlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(color = FocusColors.NightInk, fontSize = 15.sp),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text("코멘트를 남겨보세요 (선택)", color = FocusColors.NightMuted, fontSize = 15.sp)
                }
                inner()
            },
        )
    }
}
