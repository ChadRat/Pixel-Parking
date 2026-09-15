package com.example.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Material 3 Expressive Scalloped Animated Badge matching Google Pixel styling.
 *
 * - Main Shape: 10-lobed smooth scalloped flower badge rotating very slowly 1 turn clockwise, then smoothly reversing 1 turn counter-clockwise.
 * - Center Icon: Car / Location vehicle icon cleanly centered.
 * - Top-Left: Small stationary circular satellite dot.
 * - Bottom-Right: 4-lobed squircle flower rotating in the opposite direction at 1.5x speed (0.5x faster pace).
 */
@Composable
fun PixelAnimatedCarBadge(
    modifier: Modifier = Modifier,
    size: Dp = 116.dp,
    badgeColor: Color = MaterialTheme.colorScheme.primaryContainer,
    satelliteColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    dotColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
    iconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scallopBadgeAnimation")

    // Ultra smooth 1-turn (0 -> 360 deg) clockwise, then 1-turn (360 -> 0 deg) counter-clockwise
    val mainRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 14000,
                easing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f) // Symmetrical, buttery smooth ease-in-out
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mainScallopRotation"
    )

    // Satellite rotation: opposite direction, 0.5x faster (1.5x speed)
    val satelliteRotation = -mainRotation * 1.5f

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val mainRadiusMax = size.toPx() * 0.38f
            val mainRadiusMin = size.toPx() * 0.30f

            // 1. Top-Left Stationary Small Dot
            val dotCenter = Offset(size.toPx() * 0.16f, size.toPx() * 0.18f)
            val dotRadius = size.toPx() * 0.045f
            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = dotCenter
            )

            // 2. Bottom-Right (or bottom satellite) 4-Lobed Flower (rotates in opposite direction at 1.5x speed)
            val satCenter = Offset(size.toPx() * 0.86f, size.toPx() * 0.84f)
            val satRadiusMax = size.toPx() * 0.095f
            val satRadiusMin = size.toPx() * 0.068f
            val satPath = createScallopPath(
                center = satCenter,
                numLobes = 4,
                radiusMax = satRadiusMax,
                radiusMin = satRadiusMin
            )

            rotate(degrees = satelliteRotation, pivot = satCenter) {
                drawPath(path = satPath, color = satelliteColor)
            }

            // 3. Main 10-Lobed Scallop Flower
            val mainPath = createScallopPath(
                center = center,
                numLobes = 10,
                radiusMax = mainRadiusMax,
                radiusMin = mainRadiusMin
            )

            rotate(degrees = mainRotation, pivot = center) {
                drawPath(path = mainPath, color = badgeColor)
            }
        }

        // Center Car Icon
        Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = "Car",
            tint = iconColor,
            modifier = Modifier.size(size * 0.32f)
        )
    }
}

/**
 * Creates a mathematically smooth N-lobed scalloped path using polar harmonics.
 */
private fun createScallopPath(
    center: Offset,
    numLobes: Int,
    radiusMax: Float,
    radiusMin: Float,
    samples: Int = 180
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
