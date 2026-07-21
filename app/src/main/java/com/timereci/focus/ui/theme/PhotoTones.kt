package com.timereci.focus.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The six calm gradients from the prototype, used as placeholders when a card has no photo
 * (and as the timer backdrop before a photo is picked). Ported from the `tones` array in
 * PhotoCard.dc.html.
 */
object PhotoTones {
    private val stops: List<List<Pair<Float, Color>>> = listOf(
        listOf(0f to Color(0xFF9CC0E0), 0.55f to Color(0xFFC6DEEF), 1f to Color(0xFFE6F2FB)),
        listOf(0f to Color(0xFF6E93B8), 1f to Color(0xFF9EBFDB)),
        listOf(0f to Color(0xFF41607F), 1f to Color(0xFF7C9EBE)),
        listOf(0f to Color(0xFFBBD3E7), 1f to Color(0xFFE9F4FC)),
        listOf(0f to Color(0xFF2E4257), 0.92f to Color(0xFF5C7E9E)),
        listOf(0f to Color(0xFF84A8C9), 1f to Color(0xFFB6D2E8)),
    )

    val count: Int get() = stops.size

    /** A diagonal (~150°) gradient brush matching the prototype's `linear-gradient(150deg …)`. */
    fun brush(index: Int): Brush {
        val s = stops[((index % count) + count) % count]
        return Brush.linearGradient(
            colorStops = s.toTypedArray(),
            start = Offset(0f, 0f),
            end = Offset(0f, Float.POSITIVE_INFINITY),
        )
    }
}
