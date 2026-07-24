package com.timereci.focus.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import com.timereci.focus.data.PhotoStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Cheap accent-color extraction from a stored photo, so UI elements near it can pick up its
 * mood instead of always using the same fixed brand color. No palette library needed: downsample
 * hard, then favor pixels that are both saturated and mid-lightness (skips near-black/near-white
 * areas that would otherwise dominate a naive average).
 */
object PhotoPalette {
    suspend fun accentColor(context: Context, fileName: String): Color? = withContext(Dispatchers.IO) {
        runCatching {
            val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
            val decoded = BitmapFactory.decodeFile(PhotoStorage.fileIn(context, fileName).path, opts)
                ?: return@runCatching null
            val small = Bitmap.createScaledBitmap(decoded, 16, 16, true)

            var bestR = 0
            var bestG = 0
            var bestB = 0
            var bestScore = -1f
            for (y in 0 until small.height) {
                for (x in 0 until small.width) {
                    val px = small.getPixel(x, y)
                    val r = (px shr 16) and 0xFF
                    val g = (px shr 8) and 0xFF
                    val b = px and 0xFF
                    val maxC = max(r, max(g, b))
                    val minC = min(r, min(g, b))
                    val saturation = if (maxC == 0) 0f else (maxC - minC).toFloat() / maxC
                    val lightness = maxC / 255f
                    val score = saturation * (1f - abs(lightness - 0.55f))
                    if (score > bestScore) {
                        bestScore = score
                        bestR = r
                        bestG = g
                        bestB = b
                    }
                }
            }
            if (bestScore <= 0f) null else Color(bestR, bestG, bestB)
        }.getOrNull()
    }
}
