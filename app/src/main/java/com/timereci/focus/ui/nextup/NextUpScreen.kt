package com.timereci.focus.ui.nextup

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timereci.focus.data.PlannedFocusEntity
import com.timereci.focus.ui.components.IconActionButton
import com.timereci.focus.ui.theme.FocusColors
import com.timereci.focus.ui.theme.GothicFamily
import com.timereci.focus.ui.theme.MonoFamily
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val BREAK_PRESETS = listOf(5, 10)
private val CARD_SIZE_W = 230.dp
private val CARD_SIZE_H = 280.dp

/**
 * Shown after a session ends when the todo queue still has something in it. The whole queue
 * is browsable as a Tinder-style card stack — drag (or tap the buttons below) right to start
 * a task right now, left to set it aside and see the next one. Same soft blue-to-white
 * gradient as the timer screen; "나중에" (leave the whole queue for later) lives as a close
 * button in the corner.
 */
@Composable
fun NextUpScreen(
    onContinueNow: (task: String, minutes: Int) -> Unit,
    onBreak: (breakMinutes: Int, task: String, minutes: Int) -> Unit,
    onSkip: () -> Unit,
    viewModel: NextUpViewModel = hiltViewModel(),
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()

    // How many cards have been passed on so far — rotates the stack rather than deleting
    // anything, so swiping left just brings the next one up and the passed one comes back
    // around later.
    var passIndex by remember { mutableStateOf(0) }
    val visible = remember(queue, passIndex) {
        if (queue.isEmpty()) emptyList() else List(queue.size) { i -> queue[(passIndex + i) % queue.size] }
    }

    fun start(item: PlannedFocusEntity) {
        val minutes = (item.plannedMs / 60_000L).toInt().coerceAtLeast(1)
        viewModel.consume(item.id)
        passIndex = 0
        onContinueNow(item.label, minutes)
    }

    fun pass() {
        if (queue.isNotEmpty()) passIndex++
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    0f to Color(0xFF6FA8DE),
                    0.5f to Color(0xFFBEE0F5),
                    1f to Color(0xFFFFFFFF),
                ),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        IconActionButton(
            icon = Icons.Outlined.Close,
            contentDescription = "나중에",
            onClick = onSkip,
            size = 42.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "다음 집중",
                color = FocusColors.Muted,
                fontFamily = MonoFamily,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(18.dp))

            Box(
                Modifier.size(width = CARD_SIZE_W + 24.dp, height = CARD_SIZE_H + 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (visible.isEmpty()) {
                    Text("오늘 할 일을 다 살펴봤어요", color = FocusColors.Muted, fontSize = 14.sp)
                } else {
                    val shown = visible.take(3)
                    shown.withIndex().toList().asReversed().forEach { (depth, item) ->
                        if (depth == 0) {
                            key(item.id) {
                                TopCard(
                                    item = item,
                                    onSwipedRight = { start(item) },
                                    onSwipedLeft = { pass() },
                                )
                            }
                        } else {
                            StackedCard(item = item, depth = depth)
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
                IconActionButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = "패스",
                    onClick = { pass() },
                    enabled = visible.isNotEmpty(),
                    size = 52.dp,
                )
                IconActionButton(
                    icon = Icons.Filled.PlayArrow,
                    contentDescription = "바로 시작",
                    onClick = { visible.firstOrNull()?.let(::start) },
                    accent = true,
                    enabled = visible.isNotEmpty(),
                    size = 84.dp,
                )
            }

            if (visible.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                val top = visible.first()
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BREAK_PRESETS.forEach { m ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xCCFFFFFF))
                                .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(20.dp))
                                .clickable {
                                    val minutes = (top.plannedMs / 60_000L).toInt().coerceAtLeast(1)
                                    viewModel.consume(top.id)
                                    passIndex = 0
                                    onBreak(m, top.label, minutes)
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

/** A background card peeking out from behind the top of the stack — static, not interactive. */
@Composable
private fun StackedCard(item: PlannedFocusEntity, depth: Int) {
    val scale = 1f - depth * 0.06f
    val bg = if (depth == 1) Color(0x99FFFFFF) else Color(0x66FFFFFF)
    Box(
        Modifier
            .size(CARD_SIZE_W, CARD_SIZE_H)
            .scale(scale)
            .offset(y = depth * 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(bg)
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            item.label.ifBlank { "집중" },
            color = FocusColors.Ink.copy(alpha = 0.55f),
            fontFamily = GothicFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

/** The interactive top card — drag right to start it, left to pass and see the next one. */
@Composable
private fun TopCard(item: PlannedFocusEntity, onSwipedRight: () -> Unit, onSwipedLeft: () -> Unit) {
    val scope = rememberCoroutineScope()
    val offsetX = remember(item.id) { Animatable(0f) }
    val density = LocalDensity.current
    val flyDistancePx = with(density) { 480.dp.toPx() }
    val thresholdPx = with(density) { 100.dp.toPx() }
    val minutes = (item.plannedMs / 60_000L).toInt().coerceAtLeast(1)
    val progress = (offsetX.value / thresholdPx).coerceIn(-1f, 1f)

    Box(
        Modifier
            .size(CARD_SIZE_W, CARD_SIZE_H)
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .rotate((offsetX.value / 28f).coerceIn(-14f, 14f))
            .pointerInput(item.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            when {
                                offsetX.value > thresholdPx -> {
                                    offsetX.animateTo(flyDistancePx, tween(220))
                                    onSwipedRight()
                                }
                                offsetX.value < -thresholdPx -> {
                                    offsetX.animateTo(-flyDistancePx, tween(220))
                                    onSwipedLeft()
                                }
                                else -> offsetX.animateTo(0f, tween(220))
                            }
                        }
                    },
                    onDragCancel = { scope.launch { offsetX.animateTo(0f, tween(220)) } },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                    },
                )
            }
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xE6FFFFFF))
            .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (progress > 0.15f) {
                Text(
                    "시작",
                    color = FocusColors.AccentBlue.copy(alpha = progress),
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            } else if (progress < -0.15f) {
                Text(
                    "나중에",
                    color = FocusColors.Muted.copy(alpha = -progress),
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            Text(
                item.label.ifBlank { "집중" },
                color = FocusColors.Ink,
                fontFamily = GothicFamily,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 18.dp),
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(FocusColors.Mist)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text("${minutes}분", color = FocusColors.Ink2, fontFamily = MonoFamily, fontSize = 13.sp)
            }
        }
    }
}
