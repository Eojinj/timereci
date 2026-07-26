package com.timereci.focus.ui.publish

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.timereci.focus.data.PhotoRef
import com.timereci.focus.data.PhotoStorage
import com.timereci.focus.data.PlannedFocusEntity
import com.timereci.focus.ui.components.IconActionButton
import com.timereci.focus.ui.components.RecentPhotoPickerDialog
import com.timereci.focus.ui.components.grain
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.MonoFamily
import com.timereci.focus.ui.theme.patternPlaceholder
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * The publish moment: the just-earned card with the comment typed directly onto the photo
 * (task + comment overlaid), plus "add photo" / "discard" and the commit button. The whole
 * column scrolls and honors the keyboard inset so typing doesn't jump the layout around.
 */
@Composable
fun PublishScreen(
    onFinished: (PlannedFocusEntity?) -> Unit,
    viewModel: PublishViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val aspect by viewModel.photoAspect.collectAsStateWithLifecycle()
    val recentPhotos by viewModel.recentPhotos.collectAsStateWithLifecycle()
    val alarmActive by viewModel.alarmActive.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPhotoPicker by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::addPhoto) }

    fun launchSystemPicker() {
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

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

        // The card: photo (or gradient), with the comment editable right on top of it. Also a
        // Tinder-style swipeable card — drag right to commit it to the feed, left to discard —
        // matching the same swipe motif used to browse the next-up queue.
        val photos = ui.photos.ifEmpty { listOf(PhotoRef(toneIndex = ui.placeholderTone)) }
        val current = photos.first()
        val scope = rememberCoroutineScope()
        val cardOffsetX = remember { Animatable(0f) }
        val density = LocalDensity.current
        val flyDistancePx = with(density) { 600.dp.toPx() }
        val swipeThresholdPx = with(density) { 120.dp.toPx() }
        val swipeProgress = (cardOffsetX.value / swipeThresholdPx).coerceIn(-1f, 1f)
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspect.ratio)
                .offset { IntOffset(cardOffsetX.value.roundToInt(), 0) }
                .rotate((cardOffsetX.value / 42f).coerceIn(-10f, 10f))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                when {
                                    ui.saving -> Unit
                                    cardOffsetX.value > swipeThresholdPx -> {
                                        cardOffsetX.animateTo(flyDistancePx, tween(220))
                                        viewModel.store(onFinished)
                                    }
                                    cardOffsetX.value < -swipeThresholdPx -> {
                                        cardOffsetX.animateTo(-flyDistancePx, tween(220))
                                        viewModel.discard(onFinished)
                                    }
                                    else -> cardOffsetX.animateTo(0f, tween(220))
                                }
                            }
                        },
                        onDragCancel = { scope.launch { cardOffsetX.animateTo(0f, tween(220)) } },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch { cardOffsetX.snapTo(cardOffsetX.value + dragAmount) }
                        },
                    )
                }
                .clip(RoundedCornerShape(20.dp))
                .then(
                    if (current.fileName == null) {
                        Modifier.patternPlaceholder(current.toneIndex)
                    } else {
                        Modifier.background(com.timereci.focus.ui.theme.PhotoTones.brush(current.toneIndex))
                    },
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

            // Swipe feedback — a color wash + stamp that fades in with drag distance, telling
            // you which way you're about to commit before you let go.
            if (swipeProgress > 0.08f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(FocusColors.AccentSky.copy(alpha = swipeProgress * 0.28f)),
                )
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = swipeProgress))
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    Text("피드에 담기", color = Color(0xFF1F3247), fontFamily = MonoFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            } else if (swipeProgress < -0.08f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFF111C26).copy(alpha = -swipeProgress * 0.45f)),
                )
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(14.dp))
                        .background(FocusColors.NightInk.copy(alpha = -swipeProgress))
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    Text("버리기", color = FocusColors.Night, fontFamily = MonoFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            IconActionButton(
                icon = Icons.Outlined.AddPhotoAlternate,
                contentDescription = "사진 추가",
                onClick = {
                    if (recentPhotos.isEmpty()) launchSystemPicker() else showPhotoPicker = true
                },
                onDark = true,
                size = 50.dp,
            )
            IconActionButton(
                icon = Icons.Outlined.DeleteOutline,
                contentDescription = "버리기",
                onClick = { viewModel.discard(onFinished) },
                onDark = true,
                size = 50.dp,
            )
            IconActionButton(
                icon = Icons.Outlined.Check,
                contentDescription = "피드에 담기",
                onClick = { viewModel.store(onFinished) },
                accent = true,
                enabled = !ui.saving,
                size = 62.dp,
            )
        }

        // Completion alarm (sound + vibration) rings until dismissed — no auto-timeout — so
        // this stays visible the whole time it's active. Lives below the main actions rather
        // than up top, so it doesn't compete with the card for attention.
        if (alarmActive) {
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(FocusColors.Glass)
                    .clickable(onClick = viewModel::stopAlarm)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Outlined.NotificationsOff, contentDescription = null, tint = FocusColors.NightInk, modifier = Modifier.size(15.dp))
                Text("알람 끄기", color = FocusColors.NightInk, fontFamily = MonoFamily, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(20.dp))
    }

    if (showPhotoPicker) {
        RecentPhotoPickerDialog(
            recent = recentPhotos,
            onPickRecent = { ref ->
                viewModel.addExistingPhoto(ref)
                showPhotoPicker = false
            },
            onPickNew = {
                showPhotoPicker = false
                launchSystemPicker()
            },
            onDismiss = { showPhotoPicker = false },
        )
    }
}
