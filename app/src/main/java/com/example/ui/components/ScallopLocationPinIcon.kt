package com.example.ui.components

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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * Park Here Pin/Location Icon with an 8-lobed cookie badge shape background
 * matching the folder badge from the utility drawer furniture icon.
 *
 * The background shape slowly rotates clockwise, while the map pin icon inside
 * remains completely fixed / still.
 */
@Composable
fun ScallopLocationPinIcon(
    containerColor: Color,
    pinColor: Color,
    modifier: Modifier = Modifier.size(34.dp),
    animateSpin: Boolean = true
) {
    val spinTransition = rememberInfiniteTransition(label = "scallop_pin_spin_transition")
    val rotationDeg by if (animateSpin) {
        spinTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 40000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "scallop_pin_rotation"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Canvas(modifier = modifier) {
        val minDim = minOf(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)

        // Rotate ONLY the 8-lobed scalloped cookie background badge shape
        rotate(degrees = rotationDeg, pivot = center) {
            val cookiePath = createScallopCookiePath(
                center = center,
                numLobes = 8,
                radiusMax = minDim * 0.48f,
                radiusMin = minDim * 0.38f,
                samples = 96
            )
            drawPath(path = cookiePath, color = containerColor)
        }

        // Draw crisp Location Pin STILL / FIXED inside
        // Pin head center shifted slightly up to account for the bottom tip
        val pinHeadCenter = Offset(center.x, center.y - minDim * 0.05f)
        val pinHeadRadius = minDim * 0.17f

        val pinPath = Path().apply {
            // Start from left side of head
            moveTo(pinHeadCenter.x - pinHeadRadius, pinHeadCenter.y)
            // Arc over the top to right side
            cubicTo(
                pinHeadCenter.x - pinHeadRadius, pinHeadCenter.y - pinHeadRadius * 1.35f,
                pinHeadCenter.x + pinHeadRadius, pinHeadCenter.y - pinHeadRadius * 1.35f,
                pinHeadCenter.x + pinHeadRadius, pinHeadCenter.y
            )
            // Curve down to bottom needle point
            cubicTo(
                pinHeadCenter.x + pinHeadRadius * 0.95f, pinHeadCenter.y + pinHeadRadius * 0.9f,
                pinHeadCenter.x + pinHeadRadius * 0.4f, center.y + minDim * 0.22f,
                center.x, center.y + minDim * 0.26f
            )
            // Curve back up to left side
            cubicTo(
                pinHeadCenter.x - pinHeadRadius * 0.4f, center.y + minDim * 0.22f,
                pinHeadCenter.x - pinHeadRadius * 0.95f, pinHeadCenter.y + pinHeadRadius * 0.9f,
                pinHeadCenter.x - pinHeadRadius, pinHeadCenter.y
            )
            close()
        }

        // Fill pin body
        drawPath(
            path = pinPath,
            color = pinColor,
            style = Fill
        )

        // Cutout small circle inside pin head
        drawCircle(
            color = containerColor,
            radius = pinHeadRadius * 0.45f,
            center = pinHeadCenter
        )
    }
}
