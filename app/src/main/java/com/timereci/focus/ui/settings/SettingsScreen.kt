package com.timereci.focus.ui.settings

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timereci.focus.data.PhotoAspect
import com.timereci.focus.ui.i18n.AppLanguage
import com.timereci.focus.ui.i18n.LocalStrings
import com.timereci.focus.ui.i18n.Strings
import com.timereci.focus.ui.theme.FocusColors

/** Settings (Merci v5 screen 10): a plain grouped list, current value on the right. */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val context = LocalContext.current
    var showDurationPicker by remember { mutableStateOf(false) }
    var showShapePicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val brush = Brush.radialGradient(
                    0f to Color(0xFF6FA8DE),
                    0.5f to Color(0xFFBEE0F5),
                    1f to Color(0xFFF7F9FB),
                    center = Offset(size.width / 2f, size.height * 0.05f),
                    radius = size.minDimension * 0.9f,
                )
                onDrawBehind { drawRect(brush) }
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                strings.settingsTitle,
                color = FocusColors.Ink,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.03).sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )

            SectionLabel(strings.languageSection)
            Group {
                ValueRow(strings.language, languageLabel(settings.language, strings), onClick = { showLanguagePicker = true })
            }

            SectionLabel(strings.timerSection)
            Group {
                ValueRow(strings.defaultDuration, strings.minutes((settings.defaultDurationMs / 60_000L).toInt()), onClick = { showDurationPicker = true })
                Divider()
                ToggleRow(
                    strings.keepRunningWhileNote,
                    checked = settings.keepRunningWhileCommenting,
                    onCheckedChange = viewModel::setKeepRunning,
                )
            }

            SectionLabel(strings.photosSection)
            Group {
                val shapeLabel = if (settings.photoAspect == PhotoAspect.SQUARE) strings.shapeSquare else strings.shapePortrait
                ValueRow(strings.photoShape, shapeLabel, onClick = { showShapePicker = true })
            }

            SectionLabel(strings.notificationsSection)
            Group {
                ToggleRow(
                    strings.playSoundOnEnd,
                    checked = settings.alertWhenSessionEnds,
                    onCheckedChange = viewModel::setAlertWhenSessionEnds,
                )
                Divider()
                ToggleRow(
                    strings.vibrateOnEnd,
                    checked = settings.vibrateWhenSessionEnds,
                    onCheckedChange = viewModel::setVibrateWhenSessionEnds,
                )
                Divider()
                ValueRow(strings.sound, strings.systemDefault, onClick = {
                    context.startActivity(
                        Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                })
            }

            Text(
                strings.settingsFooter,
                color = FocusColors.Muted,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 4.dp).padding(top = 16.dp, bottom = 120.dp),
            )
        }
    }

    if (showDurationPicker) {
        DurationPickerDialog(
            current = settings.defaultDurationMs,
            presets = settings.durationPresets,
            onPick = { viewModel.setDefaultDuration(it); showDurationPicker = false },
            onDismiss = { showDurationPicker = false },
        )
    }
    if (showShapePicker) {
        ShapePickerDialog(
            current = settings.photoAspect,
            onPick = { viewModel.setPhotoAspect(it); showShapePicker = false },
            onDismiss = { showShapePicker = false },
        )
    }
    if (showLanguagePicker) {
        LanguagePickerDialog(
            current = settings.language,
            onPick = { viewModel.setLanguage(it); showLanguagePicker = false },
            onDismiss = { showLanguagePicker = false },
        )
    }
}

private fun languageLabel(language: AppLanguage, strings: Strings): String = when (language) {
    AppLanguage.SYSTEM -> strings.languageSystem
    AppLanguage.ENGLISH -> strings.languageEnglish
    AppLanguage.KOREAN -> strings.languageKorean
}

@Composable
private fun LanguagePickerDialog(current: AppLanguage, onPick: (AppLanguage) -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.language, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                AppLanguage.entries.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(option) }.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(languageLabel(option, strings), color = FocusColors.Ink, fontSize = 16.sp)
                        if (option == current) Text("✓", color = FocusColors.AccentBlue, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Text(strings.cancel, color = FocusColors.AccentBlue, fontSize = 15.sp, modifier = Modifier.clickable(onClick = onDismiss))
        },
    )
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

@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xEBFFFFFF)),
    ) { content() }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(FocusColors.LineSoft))
}

@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = FocusColors.Ink, fontSize = 16.sp)
        Text(value, color = FocusColors.Muted, fontSize = 15.sp)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = FocusColors.Ink, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = FocusColors.AccentBlue,
                uncheckedTrackColor = FocusColors.Line,
                uncheckedBorderColor = FocusColors.LineStrong,
            ),
        )
    }
}

@Composable
private fun DurationPickerDialog(current: Long, presets: List<Int>, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.defaultDuration, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                presets.forEach { minutes ->
                    val ms = minutes * 60_000L
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(ms) }.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(strings.minutes(minutes), color = FocusColors.Ink, fontSize = 16.sp)
                        if (ms == current) Text("✓", color = FocusColors.AccentBlue, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Text(strings.cancel, color = FocusColors.AccentBlue, fontSize = 15.sp, modifier = Modifier.clickable(onClick = onDismiss))
        },
    )
}

@Composable
private fun ShapePickerDialog(current: PhotoAspect, onPick: (PhotoAspect) -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.photoShape, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                PhotoAspect.entries.forEach { aspect ->
                    val label = if (aspect == PhotoAspect.SQUARE) strings.shapeSquare else strings.shapePortrait
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(aspect) }.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(label, color = FocusColors.Ink, fontSize = 16.sp)
                        if (aspect == current) Text("✓", color = FocusColors.AccentBlue, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Text(strings.cancel, color = FocusColors.AccentBlue, fontSize = 15.sp, modifier = Modifier.clickable(onClick = onDismiss))
        },
    )
}
