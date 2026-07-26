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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val density = LocalDensity.current
    val actionWidth = 76.dp
    val totalWidthDp = if (onEdit != null) actionWidth * 2 else actionWidth
    val totalWidthPx = with(density) { totalWidthDp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun close() = scope.launch { offsetX.animateTo(0f, tween(200)) }

    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius)),
    ) {
        // matchParentSize (not fillMaxHeight, which no-ops to wrap-content under the
        // unbounded height a scrolling Column gives here) makes this exactly the row's own
        // height — taller and it peeks out below the content on top, which is what was
        // bleeding "Edit"/"Delete" into view even at rest.
        Row(Modifier.matchParentSize(), horizontalArrangement = Arrangement.End) {
            if (onEdit != null) {
                SwipeAction(
                    label = "Edit",
                    icon = Icons.Outlined.Edit,
                    background = FocusColors.Muted2,
                    width = actionWidth,
                    onClick = { onEdit(); close() },
                )
            }
            SwipeAction(
                label = "Delete",
                icon = Icons.Outlined.DeleteOutline,
                background = FocusColors.Danger,
                width = actionWidth,
                onClick = onDelete,
            )
        }
        Box(
            Modifier
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
    Column(
        Modifier
            .width(width)
            .fillMaxHeight()
            .background(background)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier)
        Text(label, color = Color.White, fontSize = 11.5.sp)
    }
}
