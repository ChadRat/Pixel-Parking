package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Pixel Precision Waypoint Radar Visual
 * Recreates the Google Find My Device scalloped flower proximity badge.
 * - Outer boundary: 12-lobed scalloped stroke contour
 * - Inner shape: 12-lobed scalloped fill that expands as the user approaches the vehicle
 * - Directional arrow: Fixed-size rounded navigation arrow centered inside, pointing towards the target
 */
@Composable
fun CompassRadarNeedle(
    relativeArrowAngle: Float,
    compassAzimuth: Float,
    distanceMeters: Float,
    isAlignedWithTarget: Boolean,
    hasActiveTarget: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Prevent the 360-degree backflip when crossing the 0-degree boundary
    var continuousAngle by remember { mutableStateOf(relativeArrowAngle) }
    
    LaunchedEffect(relativeArrowAngle) {
        val diff = (relativeArrowAngle - continuousAngle) % 360f
        val shortestDiff = (diff + 540f) % 360f - 180f
        continuousAngle += shortestDiff
    }

    // Smooth rotation animation for the directional arrow
    val animatedAngle by animateFloatAsState(
        targetValue = continuousAngle,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "radarArrowAngle"
    )

    // Calculate proximity scale: fills outer boundary as user gets closer
    // Far (> 45m): ~0.28f, Close (10m): ~0.72f, Arrived (<= 2.5m): 1.0f (completely fills outer boundary)
    val targetScale = if (!hasActiveTarget) {
        0.28f
    } else {
        val clamped = distanceMeters.coerceIn(2.5f, 45f)
        val progress = ((45f - clamped) / (45f - 2.5f)).coerceIn(0f, 1f)
        0.28f + 0.72f * progress
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scallopFillScale"
    )

    val outerStrokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val innerFillColor = MaterialTheme.colorScheme.primary
    val arrowColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .testTag("compass_radar_needle"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxRadius = size.minDimension / 2f * 0.88f
            val strokeWidthPx = 3.5.dp.toPx()

            // 1. Draw outer 12-lobed scalloped boundary stroke
            val outerPath = buildScallopPath(cx, cy, maxRadius)
            drawPath(
                path = outerPath,
                color = outerStrokeColor,
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 2. Draw inner expanding 12-lobed scalloped filled shape
            // When animatedScale reaches 1.0f (arrived), innerRadius expands to completely fill the outer boundary
            val fillRadius = (maxRadius * animatedScale + (strokeWidthPx / 2f) * animatedScale).coerceAtLeast(maxRadius * 0.25f)
            val innerPath = buildScallopPath(cx, cy, fillRadius)
            drawPath(
                path = innerPath,
                color = innerFillColor,
                style = Fill
            )

            // 3. Draw fixed-size small rounded navigation arrow centered inside
            // Stays constant small size while the surrounding scalloped shape expands
            val arrowWidth = 32.dp.toPx()
            val arrowHeight = 44.dp.toPx()
            val arrowPath = buildDirectionalArrowPath(cx, cy, arrowWidth, arrowHeight)

            rotate(degrees = animatedAngle, pivot = Offset(cx, cy)) {
                drawPath(
                    path = arrowPath,
                    color = arrowColor,
                    style = Fill
                )
            }
        }
    }
}

/**
 * Constructs a 12-lobed smoothed scalloped flower path matching Material 3 Find My Device badge.
 * High-density angular sampling (720 steps) ensures buttery smooth curves without modifying the shape.
 */
private fun buildScallopPath(cx: Float, cy: Float, radius: Float): Path {
    val path = Path()
    val lobeCount = 12
    val steps = 720
    val twoPi = (2.0 * Math.PI).toFloat()

    for (i in 0 until steps) {
        val theta = i * (twoPi / steps)
        // Fourier modulation for organic rounded petals
        val modulation = 1f + 0.115f * kotlin.math.cos(lobeCount * theta) - 0.018f * kotlin.math.cos(2 * lobeCount * theta)
        val r = radius * modulation
        val x = cx + r * kotlin.math.cos(theta)
        val y = cy + r * kotlin.math.sin(theta)
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
 * Constructs the solid filled directional navigation arrow with rounded tips and stem.
 * Faithful reproduction of the reference arrow geometry.
 */
private fun buildDirectionalArrowPath(
    cx: Float,
    cy: Float,
    width: Float,
    height: Float
): Path {
    val path = Path()
    val halfW = width / 2f
    val halfH = height / 2f

    val topY = cy - halfH
    val bottomY = cy + halfH
    val stemHalfW = width * 0.16f
    val wingDropY = topY + height * 0.44f
    val barbInY = topY + height * 0.35f

    val apexRadius = width * 0.10f
    val wingRadius = width * 0.11f
    val stemRadius = stemHalfW

    // Start at right side of stem above bottom cap
    path.moveTo(cx + stemHalfW, bottomY - stemRadius)

    // Semicircular bottom rounded cap
    path.arcTo(
        rect = Rect(
            left = cx - stemHalfW,
            top = bottomY - 2 * stemRadius,
            right = cx + stemHalfW,
            bottom = bottomY
        ),
        startAngleDegrees = 0f,
        sweepAngleDegrees = 180f,
        forceMoveTo = false
    )

    // Up left side of stem to underbarb
    path.lineTo(cx - stemHalfW, barbInY)

    // Outward along underside to left wing corner
    path.lineTo(cx - halfW + wingRadius * 0.5f, wingDropY)

    // Left wing rounded tip
    path.quadraticTo(
        cx - halfW, wingDropY - wingRadius * 0.5f,
        cx - halfW + wingRadius * 0.4f, wingDropY - wingRadius
    )

    // Up left slope towards apex
    val apexDx = apexRadius * 0.7f
    val apexDy = apexRadius * 0.9f
    path.lineTo(cx - apexDx, topY + apexDy)

    // Apex rounded cap
    path.quadraticTo(
        cx, topY,
        cx + apexDx, topY + apexDy
    )

    // Down right slope to right wing tip
    path.lineTo(cx + halfW - wingRadius * 0.4f, wingDropY - wingRadius)

    // Right wing rounded tip
    path.quadraticTo(
        cx + halfW, wingDropY - wingRadius * 0.5f,
        cx + halfW - wingRadius * 0.5f, wingDropY
    )

    // Inward along underside of right barb to stem
    path.lineTo(cx + stemHalfW, barbInY)

    // Down right side of stem to bottom
    path.lineTo(cx + stemHalfW, bottomY - stemRadius)

    path.close()
    return path
}

