package com.timereci.focus.ui.nextup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.North
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timereci.focus.data.PlannedFocusEntity
import com.timereci.focus.ui.components.IconActionButton
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.GothicFamily
import com.timereci.focus.ui.theme.MonoFamily

private val BREAK_PRESETS = listOf(5, 10)

/**
 * Shown after a session ends when the todo queue still has something in it. The head of the
 * queue is the "hero" — shown big, front and center, with skip / start / rest controls under
 * it — and everything else waits in a horizontal, snap-scrolling row below so the whole queue
 * stays browsable at a glance instead of hiding behind a single "next" pick.
 */
@Composable
fun NextUpScreen(
    onContinueNow: (task: String, minutes: Int) -> Unit,
    onBreak: (breakMinutes: Int, task: String, minutes: Int) -> Unit,
    onSkip: () -> Unit,
    onAddMore: () -> Unit,
    viewModel: NextUpViewModel = hiltViewModel(),
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()

    // Rotates which item leads the queue without touching the database — "건너뛰기" and
    // tapping a waiting card both just move the pointer, nothing is lost or reordered for real
    // until something actually starts.
    var passIndex by remember { mutableStateOf(0) }
    val ordered = remember(queue, passIndex) {
        if (queue.isEmpty()) emptyList() else List(queue.size) { i -> queue[(passIndex + i) % queue.size] }
    }
    val current = ordered.firstOrNull()
    val waiting = if (ordered.size > 1) ordered.subList(1, ordered.size) else emptyList()

    var showBreakOptions by remember(current?.id) { mutableStateOf(false) }

    fun start(item: PlannedFocusEntity) {
        val minutes = (item.plannedMs / 60_000L).toInt().coerceAtLeast(1)
        viewModel.consume(item.id)
        passIndex = 0
        onContinueNow(item.label, minutes)
    }

    fun promote(item: PlannedFocusEntity) {
        val k = ordered.indexOf(item)
        if (k > 0) passIndex += k
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    0f to Color(0xFF6FA8DE),
                    0.5f to Color(0xFFBEE0F5),
                    1f to Color(0xFFF7F9FB),
                ),
            ),
    ) {
        DecorativeStars()

        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconActionButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = "나중에",
                    onClick = onSkip,
                    size = 42.dp,
                )
                Text(
                    "다음 집중",
                    color = FocusColors.Ink,
                    fontFamily = GothicFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(42.dp))
            }

            // Hero: the head of the queue, front and center, with its controls.
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (current == null) {
                    Text(
                        "오늘 할 일을 다 살펴봤어요",
                        color = FocusColors.Muted,
                        fontFamily = MonoFamily,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        "지금 시작할 일",
                        color = FocusColors.AccentBlue,
                        fontFamily = MonoFamily,
                        fontSize = 12.sp,
                        letterSpacing = 3.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        current.label.ifBlank { "집중" },
                        color = FocusColors.Ink,
                        fontFamily = GothicFamily,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 40.sp,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(32.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(26.dp), verticalAlignment = Alignment.CenterVertically) {
                        GlassRoundButton(
                            icon = Icons.Outlined.SkipNext,
                            label = "건너뛰기",
                            onClick = { if (ordered.size > 1) passIndex++ },
                        )
                        IconActionButton(
                            icon = Icons.Filled.PlayArrow,
                            contentDescription = "바로 시작",
                            onClick = { start(current) },
                            accent = true,
                            size = 84.dp,
                        )
                        GlassRoundButton(
                            icon = Icons.Outlined.LocalCafe,
                            label = "휴식",
                            onClick = { showBreakOptions = !showBreakOptions },
                        )
                    }

                    AnimatedVisibility(visible = showBreakOptions) {
                        Column {
                            Spacer(Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                BREAK_PRESETS.forEach { m ->
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color.White)
                                            .border(1.dp, FocusColors.Line, RoundedCornerShape(20.dp))
                                            .clickable {
                                                val minutes = (current.plannedMs / 60_000L).toInt().coerceAtLeast(1)
                                                viewModel.consume(current.id)
                                                passIndex = 0
                                                onBreak(m, current.label, minutes)
                                            }
                                            .padding(horizontal = 16.dp, vertical = 9.dp),
                                    ) {
                                        Text("${m}분 휴식", color = FocusColors.Ink2, fontFamily = MonoFamily, fontSize = 12.5.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Waiting: everything else in the queue, browsable in a horizontal snap-scroll row.
            Column(Modifier.padding(bottom = 20.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "대기 중인 작업",
                        color = FocusColors.Ink,
                        fontFamily = GothicFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xB3FFFFFF))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text("${waiting.size}개", color = FocusColors.AccentBlue, fontFamily = MonoFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(4.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(waiting, key = { it.id }) { item ->
                        WaitingCard(item = item, onClick = { promote(item) })
                    }
                    item {
                        AddMoreCard(onClick = onAddMore)
                    }
                }
            }
        }
    }
}

/** A translucent circular icon button with a caption underneath — matches the design's glass look. */
@Composable
private fun GlassRoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0x66FFFFFF))
                .border(1.dp, Color(0x80FFFFFF), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = FocusColors.AccentDeep, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = FocusColors.Ink2, fontFamily = MonoFamily, fontSize = 10.sp)
    }
}

/** One waiting-queue card. Tapping it brings that task to the front as the new hero. */
@Composable
private fun WaitingCard(item: PlannedFocusEntity, onClick: () -> Unit) {
    val minutes = (item.plannedMs / 60_000L).toInt().coerceAtLeast(1)
    Column(
        Modifier
            .width(190.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xE6FFFFFF))
            .border(1.dp, FocusColors.Line, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Text(
                item.label.ifBlank { "집중" },
                color = FocusColors.Ink,
                fontFamily = GothicFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(FocusColors.Mist),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.North, contentDescription = "다음으로 올리기", tint = FocusColors.AccentBlue, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = FocusColors.Muted, modifier = Modifier.size(14.dp))
            Text("${minutes}분", color = FocusColors.Muted, fontFamily = MonoFamily, fontSize = 12.sp)
        }
    }
}

/** The trailing card in the waiting row — jumps out to the todo screen to add more. */
@Composable
private fun AddMoreCard(onClick: () -> Unit) {
    Column(
        Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x40FFFFFF))
            .border(1.dp, Color(0x80FFFFFF), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
            .height(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.Add, contentDescription = "새로운 작업 추가", tint = FocusColors.Ink2, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text("새로운 작업", color = FocusColors.Ink2, fontFamily = MonoFamily, fontSize = 11.sp)
    }
}

/** A few soft, blurred stars scattered behind everything — decorative only. */
@Composable
private fun DecorativeStars() {
    val tint = FocusColors.AccentBlue.copy(alpha = 0.35f)
    Box(Modifier.fillMaxSize()) {
        Icon(
            Icons.Filled.Star, contentDescription = null, tint = tint,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 36.dp, top = 90.dp).size(30.dp)
                .blur(6.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
        )
        Icon(
            Icons.Filled.Star, contentDescription = null, tint = FocusColors.AccentSky.copy(alpha = 0.4f),
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 48.dp, top = 160.dp).size(46.dp)
                .blur(8.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
        )
        Icon(
            Icons.Filled.Star, contentDescription = null, tint = tint,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 260.dp).size(22.dp)
                .blur(5.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
        )
        Icon(
            Icons.Filled.Star, contentDescription = null, tint = FocusColors.AccentSky.copy(alpha = 0.35f),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 60.dp, bottom = 320.dp).size(34.dp)
                .blur(7.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
        )
    }
}
