package com.timereci.focus.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Decorative fill for a card that has no photo, used instead of a flat neutral grey: one of
 * the existing gradient tones with a light pattern (dots / stars) scattered over it.
 * Both the colour and the pattern kind are derived from [toneIndex], so the same session
 * always renders the same way but different sessions read as varied, not blank.
 */
fun Modifier.patternPlaceholder(toneIndex: Int): Modifier = this.drawWithCache {
    val brush = PhotoTones.brush(toneIndex)
    val ink = Color.White.copy(alpha = 0.4f)
    onDrawBehind {
        drawRect(brush)
        when (((toneIndex % 2) + 2) % 2) {
            0 -> drawDots(ink)
            else -> drawStars(ink)
        }
    }
}

private fun DrawScope.drawDots(color: Color) {
    val spacing = 24.dp.toPx()
    val radius = 3.dp.toPx()
    var row = 0
    var y = spacing / 2
    while (y < size.height + spacing) {
        val xOffset = if (row % 2 == 0) 0f else spacing / 2
        var x = spacing / 2 + xOffset
        while (x < size.width + spacing) {
            drawCircle(color = color, radius = radius, center = Offset(x, y))
            x += spacing
        }
        y += spacing * 0.87f
        row++
    }
}

/** Normalized (0..1) scatter positions for the star pattern. */
private val SCATTER = listOf(
    0.14f to 0.18f, 0.5f to 0.1f, 0.84f to 0.2f,
    0.26f to 0.46f, 0.7f to 0.44f, 0.1f to 0.72f,
    0.56f to 0.72f, 0.88f to 0.66f, 0.38f to 0.9f,
)

private fun DrawScope.drawStars(color: Color) {
    val base = size.minDimension * 0.05f
    SCATTER.forEachIndexed { i, (fx, fy) ->
        val scale = if (i % 3 == 0) 1.3f else 0.85f
        drawPath(
            starPath(size.width * fx, size.height * fy, base * scale, base * scale * 0.42f),
            color = color,
        )
    }
}

private fun starPath(cx: Float, cy: Float, outerR: Float, innerR: Float): Path {
    val path = Path()
    val points = 5
    val step = Math.PI / points
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outerR else innerR
        val angle = -Math.PI / 2 + i * step
        val x = cx + (r * cos(angle)).toFloat()
        val y = cy + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
