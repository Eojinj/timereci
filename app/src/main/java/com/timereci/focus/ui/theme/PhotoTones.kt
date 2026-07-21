package com.timereci.focus.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Calm gradients used as placeholders when a card has no photo (and as the timer backdrop
 * before a photo is picked). Started as the six blues from the prototype's `tones` array in
 * PhotoCard.dc.html; widened with a few more hues (sage, clay, lavender, sand, rose, teal) so
 * a run of photo-less sessions doesn't read as "everything is blue".
 */
object PhotoTones {
    private val stops: List<List<Pair<Float, Color>>> = listOf(
        listOf(0f to Color(0xFF9CC0E0), 0.55f to Color(0xFFC6DEEF), 1f to Color(0xFFE6F2FB)), // blue
        listOf(0f to Color(0xFFA8C3A0), 1f to Color(0xFFE3EEDF)), // sage
        listOf(0f to Color(0xFFD9A483), 1f to Color(0xFFF3E1D3)), // clay
        listOf(0f to Color(0xFF41607F), 1f to Color(0xFF7C9EBE)), // deep blue
        listOf(0f to Color(0xFFB8AEDB), 1f to Color(0xFFEDE9F7)), // lavender
        listOf(0f to Color(0xFFD8C8A8), 1f to Color(0xFFF5EFE0)), // sand
        listOf(0f to Color(0xFF2E4257), 0.92f to Color(0xFF5C7E9E)), // navy
        listOf(0f to Color(0xFFD79FA0), 1f to Color(0xFFF6E4E5)), // dusty rose
        listOf(0f to Color(0xFF5C8A8A), 1f to Color(0xFFC3DEDE)), // slate teal
    )

    val count: Int get() = stops.size

    /**
     * Deterministic tone index for a session that has no photo, derived from a stable seed
     * (e.g. the receipt id) so different sessions land on different tones/patterns instead of
     * all defaulting to index 0.
     */
    fun indexFor(seed: Long): Int = (((seed % count) + count) % count).toInt()

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
