package com.timereci.focus.ui.components

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timereci.focus.ui.i18n.LocalStrings
import com.timereci.focus.ui.theme.FocusColors
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * The one swipe gesture in the app: drag [content] left to reveal labeled Edit/Delete actions
 * underneath, matching the standard iOS row-swipe (Merci v5 screen 11) — no destructive
 * Tinder-style card swipe anywhere else.
 */
@Composable
fun SwipeableRow(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    onEdit: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val strings = LocalStrings.current
    val density = LocalDensity.current
    val actionWidth = 76.dp
    val totalWidthDp = if (onEdit != null) actionWidth * 2 else actionWidth
    val totalWidthPx = with(density) { totalWidthDp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Measured directly from the content below, in pixels, so the actions are pinned to
    // exactly that height — no reliance on fillMaxHeight()/matchParentSize() correctly
    // reading a bounded constraint that may not exist inside a scrolling column.
    var contentHeightPx by remember { mutableIntStateOf(0) }

    // The row backgrounds in this app are translucent white over a gradient, so actions parked
    // behind a closed row are legible *through* it. Rather than fight that with opacity, the
    // actions simply aren't composed until the row is actually dragged open. derivedStateOf
    // keeps this to one recomposition per open/close instead of one per animation frame.
    val revealed by remember { derivedStateOf { offsetX.value != 0f } }

    fun close() = scope.launch { offsetX.animateTo(0f, tween(200)) }

    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius)),
    ) {
        if (revealed) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(with(density) { contentHeightPx.toDp() }),
                horizontalArrangement = Arrangement.End,
            ) {
                if (onEdit != null) {
                    SwipeAction(
                        label = strings.edit,
                        icon = Icons.Outlined.Edit,
                        background = FocusColors.Muted2,
                        width = actionWidth,
                        onClick = { onEdit(); close() },
                    )
                }
                SwipeAction(
                    label = strings.delete,
                    icon = Icons.Outlined.DeleteOutline,
                    background = FocusColors.Danger,
                    width = actionWidth,
                    onClick = onDelete,
                )
            }
        }
        Box(
            Modifier
                .onSizeChanged { contentHeightPx = it.height }
                .offset { androidx.compose.ui.unit.IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(totalWidthPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val target = if (offsetX.value < -totalWidthPx / 2) -totalWidthPx else 0f
                            scope.launch { offsetX.animateTo(target, tween(200)) }
                        },
                        onDragCancel = { close() },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-totalWidthPx, 0f))
                            }
                        },
                    )
                },
        ) {
            content()
        }
    }
}

@Composable
private fun SwipeAction(
    label: String,
    icon: ImageVector,
    background: Color,
    width: Dp,
    onClick: () -> Unit,
) {
    // Icon and label side by side (one line) rather than stacked — a single line is always
    // shorter than any real row, so there's a wide safety margin against ever overflowing it.
    Row(
        Modifier
            .width(width)
            .fillMaxHeight()
            .background(background)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color.White, fontSize = 11.5.sp)
    }
}
