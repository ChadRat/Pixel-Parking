package com.PixelParking.wear.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Creates an 8-lobed scalloped cookie/star badge shape path centered at [center].
 * Exact same mathematical function as in UtilityDrawerHeroBadge.
 */
fun createWearScallopCookiePath(
    center: Offset,
    numLobes: Int = 8,
    radiusMax: Float = 22.5f,
    radiusMin: Float = 18f,
    samples: Int = 120
): Path {
    val path = Path()
    val rAvg = (radiusMax + radiusMin) / 2f
    val rAmp = (radiusMax - radiusMin) / 2f

    for (i in 0..samples) {
        val angle = (i.toFloat() / samples) * 2f * PI.toFloat()
        val r = rAvg + rAmp * cos(numLobes * angle)
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    return path
}

/**
 * Found Car Checkmark Icon with an 8-lobed cookie badge shape background
 * matching the folder badge from the utility drawer furniture icon.
 * Spins very very slowly clockwise.
 */
@Composable
fun WearScallopCheckMarkIcon(
    containerColor: Color,
    checkColor: Color,
    modifier: Modifier = Modifier.size(18.dp),
    animateSpin: Boolean = true
) {
    val spinTransition = rememberInfiniteTransition(label = "wear_scallop_spin_transition")
    val rotationDeg by if (animateSpin) {
        spinTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 40000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wear_scallop_rotation"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Canvas(modifier = modifier) {
        val minDim = minOf(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)

        // Rotate ONLY the 8-lobed scalloped cookie background badge shape
        rotate(degrees = rotationDeg, pivot = center) {
            val cookiePath = createWearScallopCookiePath(
                center = center,
                numLobes = 8,
                radiusMax = minDim * 0.48f,
                radiusMin = minDim * 0.38f,
                samples = 96
            )
            drawPath(path = cookiePath, color = containerColor)
        }

        // Draw crisp checkmark FIXED / STILL (not rotated)
        val checkPath = Path().apply {
            moveTo(center.x - minDim * 0.20f, center.y + minDim * 0.02f)
            lineTo(center.x - minDim * 0.05f, center.y + minDim * 0.17f)
            lineTo(center.x + minDim * 0.22f, center.y - minDim * 0.15f)
        }

        drawPath(
            path = checkPath,
            color = checkColor,
            style = Stroke(
                width = minDim * 0.12f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
