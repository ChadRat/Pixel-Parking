package com.PixelParking.wear.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Mini Waypoint Shapes for Wear OS:
 * Faithfully mirrors the phone app's 12-lobed scalloped radar geometry,
 * proximity-based dynamic fill, and continuous directional arrow rotation.
 * STRICTLY shapes only - NO text, perfectly glanceable on watch display.
 */
@Composable
fun WearWaypointShapes(
    distanceMeters: Float,
    relativeArrowAngle: Float,
    hasActiveTarget: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp,
    scallopColor: Color = Color(0xFF00E676),
    arrowColor: Color = Color(0xFF00391F)
) {
    // Proximity scaling matching phone CompassRadarNeedle logic:
    // <= 2.5m: 1.0f (fully filled)
    // >= 45m: 0.28f
    val targetScale = remember(distanceMeters, hasActiveTarget) {
        if (!hasActiveTarget) {
            0.28f
        } else {
            val clampedDist = distanceMeters.coerceIn(2.5f, 45f)
            val progress = (45f - clampedDist) / (45f - 2.5f)
            0.28f + (0.72f * progress)
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "wear_scallop_scale"
    )

    // Continuous shortest-distance rotation animation (prevents 360 flip)
    var previousRawAngle by remember { mutableFloatStateOf(relativeArrowAngle) }
    var continuousAngle by remember { mutableFloatStateOf(relativeArrowAngle) }

    val currentRaw = if (hasActiveTarget) relativeArrowAngle else 0f
    val delta = (currentRaw - previousRawAngle + 540f) % 360f - 180f
    continuousAngle += delta
    previousRawAngle = currentRaw

    val animatedAngle by animateFloatAsState(
        targetValue = continuousAngle,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "wear_arrow_rotation"
    )

    val strokeWidth = 3.5.dp

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val strokeWidthPx = strokeWidth.toPx()

            // Outer scalloped boundary fits neatly inside canvas with safe padding to avoid clipping on round bezel bezels
            val maxRadius = (this.size.width / 2f) - strokeWidthPx - 2.dp.toPx()
            val outerScallopPath = buildScallopPath(cx, cy, maxRadius)

            // 1. Draw outer 12-lobed scalloped contour boundary (35% alpha)
            drawPath(
                path = outerScallopPath,
                color = scallopColor.copy(alpha = 0.35f),
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 2. Draw inner expanding 12-lobed scalloped fill
            val fillRadius = (maxRadius * animatedScale + (strokeWidthPx / 2f) * animatedScale)
                .coerceAtLeast(maxRadius * 0.25f)
            val innerFillPath = buildScallopPath(cx, cy, fillRadius)

            drawPath(
                path = innerFillPath,
                color = scallopColor,
                style = Fill
            )

            // 3. Draw centered navigation arrow pointing towards target car (proportionally sized to match larger scallop)
            val arrowWidth = (size * 0.17f).toPx()
            val arrowHeight = (size * 0.23f).toPx()
            val arrowPath = buildDirectionalArrowPath(cx, cy, arrowWidth, arrowHeight)

            rotate(degrees = animatedAngle, pivot = androidx.compose.ui.geometry.Offset(cx, cy)) {
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
 * 12-lobed modulated circular path formula identical to phone app's buildScallopPath.
 */
private fun buildScallopPath(cx: Float, cy: Float, radius: Float): Path {
    val path = Path()
    val lobeCount = 12
    val steps = 720
    val twoPi = (2.0 * Math.PI).toFloat()

    for (i in 0 until steps) {
        val theta = i * (twoPi / steps)
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
 * Directional navigation arrow geometry identical to phone app's buildDirectionalArrowPath.
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

    path.moveTo(cx + stemHalfW, bottomY - stemRadius)

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

    path.lineTo(cx - stemHalfW, barbInY)
    path.lineTo(cx - halfW + wingRadius * 0.5f, wingDropY)

    path.quadraticTo(
        cx - halfW, wingDropY - wingRadius * 0.5f,
        cx - halfW + wingRadius * 0.4f, wingDropY - wingRadius
    )

    val apexDx = apexRadius * 0.7f
    val apexDy = apexRadius * 0.9f
    path.lineTo(cx - apexDx, topY + apexDy)

    path.quadraticTo(
        cx, topY,
        cx + apexDx, topY + apexDy
    )

    path.lineTo(cx + halfW - wingRadius * 0.4f, wingDropY - wingRadius)

    path.quadraticTo(
        cx + halfW, wingDropY - wingRadius * 0.5f,
        cx + halfW - wingRadius * 0.5f, wingDropY
    )

    path.lineTo(cx + stemHalfW, barbInY)
    path.lineTo(cx + stemHalfW, bottomY - stemRadius)

    path.close()
    return path
}
