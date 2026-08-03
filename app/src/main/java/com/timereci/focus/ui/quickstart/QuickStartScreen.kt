package com.timereci.focus.ui.quickstart

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timereci.focus.ui.components.ChoosePhotoSheet
import com.timereci.focus.ui.i18n.LocalStrings
import com.timereci.focus.ui.model.RecentTask
import com.timereci.focus.ui.theme.FocusColors

/**
 * "Type Laundry 20 and it becomes Laundry · 20 min" (Merci v5 screen 2). One field, a live
 * preview, Start — no dials, no ring. Starts the session immediately and hands off to the
 * (already-running) Focus screen.
 */
@Composable
fun QuickStartScreen(
    onCancel: () -> Unit,
    onStarted: () -> Unit,
    viewModel: QuickStartViewModel = hiltViewModel(),
) {
    val text by viewModel.text.collectAsStateWithLifecycle()
    val resolved by viewModel.resolved.collectAsStateWithLifecycle()
    val presetMinutes by viewModel.presetMinutes.collectAsStateWithLifecycle()
    val recentCompleted by viewModel.recentCompleted.collectAsStateWithLifecycle()
    val backdrop by viewModel.backdrop.collectAsStateWithLifecycle()
    val repeating by viewModel.repeating.collectAsStateWithLifecycle()
    val recentPhotos by viewModel.recentPhotos.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    var showPhotoSheet by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::importBackdrop) }
    fun launchSystemPicker() {
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
                        center = Offset(size.width / 2f, size.height * 0.3f),
                        radius = size.minDimension * 0.8f,
                    )
                    onDrawBehind { drawRect(brush) }
                },
        )

        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(46.dp)) {
                Text(
                    strings.cancel,
                    color = FocusColors.AccentBlue,
                    fontSize = 16.5.sp,
                    modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onCancel),
                )
                Text(strings.quickStart, color = FocusColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center))
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                SectionLabel(strings.whatFocusingOn, topPadding = 10.dp)

                Row(
                    Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val focusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }
                    BasicTextField(
                        value = text,
                        onValueChange = viewModel::setText,
                        singleLine = true,
                        textStyle = TextStyle(color = FocusColors.Ink, fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
                        cursorBrush = SolidColor(FocusColors.AccentBlue),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            // A bare number ("20") is a valid start too — it just has no
                            // task label — so gate on the raw text, not the parsed label.
                            if (text.isNotBlank()) { viewModel.start(); onStarted() }
                        }),
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        decorationBox = { inner ->
                            if (text.isEmpty()) Text(strings.quickEntryPlaceholder, color = FocusColors.Muted2, fontSize = 24.sp)
                            inner()
                        },
                    )
                }

                if (resolved.label.isNotBlank()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(FocusColors.Mist)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .padding(top = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = FocusColors.AccentBlue, modifier = Modifier.size(20.dp))
                        Row(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(resolved.label, color = FocusColors.Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(strings.minutes(resolved.minutes), color = FocusColors.AccentBlue, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Text(
                    strings.quickEntryHint(presetMinutes),
                    color = FocusColors.Muted,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp),
                )

                if (recentCompleted.isNotEmpty()) {
                    SectionLabel(strings.recentSection)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recentCompleted, key = { it.label }) { task ->
                            RecentChip(
                                task = task,
                                onClick = { viewModel.fillFromRecent(task) },
                                onDismiss = { viewModel.dismissRecent(task.label) },
                            )
                        }
                    }
                }

                SectionLabel(strings.optionsSection)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xEBFFFFFF)),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { showPhotoSheet = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(strings.backgroundPhoto, color = FocusColors.Ink, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text(if (backdrop != null) strings.selected else strings.none, color = FocusColors.Muted, fontSize = 15.sp)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(FocusColors.LineSoft))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setRepeating(!repeating) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(end = 10.dp)) {
                            Text(strings.repeatingTask, color = FocusColors.Ink, fontSize = 16.sp)
                            Text(
                                strings.repeatingTaskHint,
                                color = FocusColors.Muted,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                            )
                        }
                        Switch(
                            checked = repeating,
                            onCheckedChange = viewModel::setRepeating,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = FocusColors.AccentBlue,
                                uncheckedTrackColor = FocusColors.Line,
                                uncheckedBorderColor = FocusColors.LineStrong,
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(120.dp))
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xF7FFFFFF))
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (text.isNotBlank()) FocusColors.AccentDeep else FocusColors.Muted2)
                        .clickable(enabled = text.isNotBlank()) { viewModel.start(); onStarted() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (resolved.label.isNotBlank()) {
                            strings.startWithLabel(resolved.label, strings.minutes(resolved.minutes))
                        } else {
                            strings.startPlain(strings.minutes(resolved.minutes))
                        },
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                // Queue it instead of starting now. Needs a name — an unnamed task on the list
                // would just read as "Focus" and couldn't be told apart from any other.
                if (resolved.label.isNotBlank()) {
                    Text(
                        strings.saveToToday,
                        color = FocusColors.AccentBlue,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.saveToToday(); onCancel() }
                            .padding(vertical = 11.dp),
                    )
                }
            }
        }
    }

    if (showPhotoSheet) {
        ChoosePhotoSheet(
            recent = recentPhotos,
            selectedFileName = backdrop?.fileName,
            onPickRecent = { ref -> viewModel.useRecentBackdrop(ref); showPhotoSheet = false },
            onPickNew = { showPhotoSheet = false; launchSystemPicker() },
            onRemove = if (backdrop != null) ({ viewModel.clearBackdrop(); showPhotoSheet = false }) else null,
            onDismiss = { showPhotoSheet = false },
        )
    }
}

@Composable
private fun SectionLabel(text: String, topPadding: androidx.compose.ui.unit.Dp = 22.dp) {
    Text(
        text,
        color = FocusColors.Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = topPadding, bottom = 8.dp, start = 4.dp),
    )
}

@Composable
private fun RecentChip(task: RecentTask, onClick: () -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xEBFFFFFF))
            .padding(start = 14.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            Modifier.clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(task.label, color = FocusColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("${task.minutes}", color = FocusColors.Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.width(2.dp))
        Box(
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Close, contentDescription = strings.remove, tint = FocusColors.Muted2, modifier = Modifier.size(12.dp))
        }
    }
}
