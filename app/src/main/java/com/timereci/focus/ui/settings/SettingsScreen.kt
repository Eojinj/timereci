package com.timereci.focus.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timereci.focus.data.PhotoAspect
import com.timereci.focus.ui.components.IconActionButton
import com.timereci.focus.ui.components.grainyBackground
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.MonoFamily

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .grainyBackground(
                base = FocusColors.BaseLight,
                blob = FocusColors.AccentDeep.copy(alpha = 0.10f),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconActionButton(icon = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", onClick = onBack, size = 38.dp)
        }

        Section("타이머 기본값") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                settings.durationPresets.forEach { min ->
                    val ms = min * 60_000L
                    val active = ms == settings.defaultDurationMs
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) FocusColors.AccentDeep else FocusColors.Mist)
                            .clickable { viewModel.setDefaultDuration(ms) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            "${min}m",
                            color = if (active) FocusColors.Paper else FocusColors.Ink2,
                            fontFamily = MonoFamily,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }

        Section("사진 비율") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhotoAspect.entries.forEach { aspect ->
                    val active = aspect == settings.photoAspect
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) FocusColors.AccentDeep else FocusColors.Mist)
                            .clickable { viewModel.setPhotoAspect(aspect) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            Modifier
                                .size(width = 18.dp * aspect.ratio, height = 18.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (active) FocusColors.Paper else FocusColors.LineStrong),
                        )
                        Text(
                            aspect.label,
                            color = if (active) FocusColors.Paper else FocusColors.Ink2,
                            fontFamily = MonoFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        ToggleSection(
            title = "영수증 길이",
            description = if (settings.proportionalLength) {
                "길이 ∝ 집중시간 — 오래 집중할수록 길게 발행 (시그니처)"
            } else {
                "균일 비율 — 모든 영수증이 같은 크기"
            },
            checked = settings.proportionalLength,
            onCheckedChange = viewModel::setProportionalLength,
        )

        ToggleSection(
            title = "코멘트 중 타이머",
            description = if (settings.keepRunningWhileCommenting) {
                "계속 흐름 — 코멘트를 써도 집중시간은 정직하게 흐릅니다"
            } else {
                "일시정지 — 코멘트를 쓰는 동안 타이머가 멈춥니다"
            },
            checked = settings.keepRunningWhileCommenting,
            onCheckedChange = viewModel::setKeepRunning,
        )

        ProCard()

        Text(
            "권한 0 — 시스템 포토 피커만 사용합니다. 저장소 권한이나 추적 요청이 없습니다.",
            color = FocusColors.Muted2,
            fontFamily = MonoFamily,
            fontSize = 10.5.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(title, color = FocusColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun ToggleSection(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = FocusColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(description, color = FocusColors.Muted, fontSize = 12.5.sp, lineHeight = 18.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FocusColors.Paper,
                checkedTrackColor = FocusColors.AccentDeep,
                uncheckedTrackColor = FocusColors.Line,
                uncheckedBorderColor = FocusColors.LineStrong,
            ),
        )
    }
}

@Composable
private fun ProCard() {
    Column(
        Modifier
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FocusColors.AccentInk)
            .padding(20.dp),
    ) {
        Text("집중 영수증 Pro", color = FocusColors.NightInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "테마 팩 · 고해상도 내보내기 · 월간 스프레드 · 홈 위젯.\n타이머와 기본 영수증, 롤 열람은 언제나 무료입니다.",
            color = FocusColors.NightMuted,
            fontSize = 12.5.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(FocusColors.AccentSky),
            contentAlignment = Alignment.Center,
        ) {
            Text("월 ₩2,900 · 7일 무료 체험 (곧 제공)", color = FocusColors.Night2, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}
