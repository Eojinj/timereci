package com.timereci.focus.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import com.timereci.focus.R

/**
 * A tiled film-grain brush from res/drawable/noise.png, cached per composition. Drawn with
 * [androidx.compose.ui.graphics.BlendMode.Overlay] so mid-grey noise leaves the base alone
 * while lighter/darker specks add texture — this is what stops the flat vector gradients
 * from reading as plastic.
 */
@Composable
fun rememberGrainBrush(): ShaderBrush {
    val noise = androidx.compose.ui.graphics.ImageBitmap.imageResource(R.drawable.noise)
    return remember(noise) { ShaderBrush(ImageShader(noise, TileMode.Repeated, TileMode.Repeated)) }
}

/**
 * A value-anchored background: a near-neutral [base] with a single soft colour [blob]
 * radial placed off-centre, finished with film grain. Replaces the flat pastel fills.
 *
 * @param blobCenter fractional position of the blob (0..1 of width/height).
 */
@Composable
fun Modifier.grainyBackground(
    base: Color,
    blob: Color,
    blobCenter: Offset = Offset(0.85f, 0.1f),
    blobRadiusScale: Float = 0.85f,
    grainAlpha: Float = 0.10f,
): Modifier {
    val grain = rememberGrainBrush()
    return this.drawWithCache {
        val blobBrush = Brush.radialGradient(
            colors = listOf(blob, Color.Transparent),
            center = Offset(size.width * blobCenter.x, size.height * blobCenter.y),
            radius = size.maxDimension * blobRadiusScale,
        )
        onDrawBehind {
            drawRect(base)
            drawRect(blobBrush)
            drawRect(
                brush = grain,
                alpha = grainAlpha,
                blendMode = androidx.compose.ui.graphics.BlendMode.Overlay,
            )
        }
    }
}

/** Just the grain overlay, for surfaces that already have their own base colour. */
@Composable
fun Modifier.grain(alpha: Float = 0.08f): Modifier {
    val grain = rememberGrainBrush()
    return this.drawWithCache {
        onDrawWithContent {
            drawContent()
            drawRect(
                brush = grain,
                alpha = alpha,
                blendMode = androidx.compose.ui.graphics.BlendMode.Overlay,
            )
        }
    }
}
