package com.PixelParking.wear.presentation.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive

/**
 * Mini Waypoint Shapes for Wear OS:
 * Exactly mirrors the companion app's 12-lobed scalloped radar geometry, dual-rotation physics,
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
    arrowColor: Color = Color(0xFF00391F),
    distanceCoveredProgress: Float? = null
) {
    val targetArrowAngle = if (hasActiveTarget) relativeArrowAngle else 0f
    // Prevent the 360-degree backflip when crossing the 0-degree boundary
    var continuousAngle by remember { mutableStateOf(targetArrowAngle) }

    LaunchedEffect(targetArrowAngle) {
        val diff = (targetArrowAngle - continuousAngle) % 360f
        val shortestDiff = (diff + 540f) % 360f - 180f
        continuousAngle += shortestDiff
    }

    // Smooth rotation animation for the directional arrow
    val animatedAngle by animateFloatAsState(
        targetValue = continuousAngle,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "wearRadarArrowAngle"
    )

    // Calculate proximity scale: fills outer boundary as user gets closer
    // according to the percentage of distance that the user has completed.
    // 0% covered: 0.28f (base inner size) -> 100% covered: 1.0f (completely fills outer boundary)
    var maxObservedDistance by remember(hasActiveTarget) { mutableFloatStateOf(distanceMeters) }
    if (distanceMeters > maxObservedDistance) {
        maxObservedDistance = distanceMeters
    }
    val fallbackProgress = if (maxObservedDistance <= 2.5f || distanceMeters <= 2.5f) {
        1.0f
    } else {
        val effectiveMax = maxOf(maxObservedDistance, 15f)
        ((effectiveMax - distanceMeters.coerceAtLeast(2.5f)) / (effectiveMax - 2.5f)).coerceIn(0f, 1f)
    }
    val progress = distanceCoveredProgress?.coerceIn(0f, 1f) ?: fallbackProgress

    val targetScale = if (!hasActiveTarget) {
        0.28f
    } else {
        0.28f + 0.72f * progress
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "wearScallopFillScale"
    )

    // Proximity fill ratio: 0f (far) to 1f (completely filled)
    val fillRatio = ((animatedScale - 0.28f) / (1.0f - 0.28f)).coerceIn(0f, 1f)

    // Slower, ultra-gentle rotation speeds (degrees per second):
    // Standard slow spin: 2.0 deg/sec (~180 seconds per revolution)
    // Even slower spin when filled up: 0.8 deg/sec (~450 seconds per revolution)
    val baseSpeed = 2.0f
    val filledSpeed = 0.8f

    // Outer target speed: spins clockwise (+), slows down even more when filled up
    val outerTargetSpeed = if (fillRatio >= 0.85f) {
        val slowDown = ((fillRatio - 0.85f) / 0.15f).coerceIn(0f, 1f)
        val smoothSlowDown = slowDown * slowDown * (3f - 2f * slowDown)
        baseSpeed - (baseSpeed - filledSpeed) * smoothSlowDown
    } else {
        baseSpeed
    }

    // Inner target speed:
    // - Counter-clockwise (-baseSpeed) when separated
    // - As shapes get closer and start to fill up (fillRatio: 0.35f -> 0.75f), smoothly transitions
    //   so that it rotates the same way and speed as the outer shape (+baseSpeed)
    // - Once filled up (fillRatio >= 0.85f), slows down rotation even more to filledSpeed (+0.8f)
    val innerTargetSpeed = when {
        fillRatio < 0.35f -> -baseSpeed
        fillRatio < 0.75f -> {
            val t = ((fillRatio - 0.35f) / 0.40f).coerceIn(0f, 1f)
            val smoothT = t * t * (3f - 2f * t)
            -baseSpeed + (2f * baseSpeed) * smoothT
        }
        fillRatio < 0.85f -> baseSpeed
        else -> {
            val slowDown = ((fillRatio - 0.85f) / 0.15f).coerceIn(0f, 1f)
            val smoothSlowDown = slowDown * slowDown * (3f - 2f * slowDown)
            baseSpeed - (baseSpeed - filledSpeed) * smoothSlowDown
        }
    }

    val smoothEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    val animatedOuterSpeed by animateFloatAsState(
        targetValue = outerTargetSpeed,
        animationSpec = if (fillRatio >= 0.75f) tween(durationMillis = 600, easing = smoothEasing) else tween(durationMillis = 2800, easing = smoothEasing),
        label = "wearOuterBadgeRotationSpeed"
    )

    val animatedInnerSpeed by animateFloatAsState(
        targetValue = innerTargetSpeed,
        animationSpec = if (fillRatio >= 0.75f) tween(durationMillis = 600, easing = smoothEasing) else tween(durationMillis = 3200, easing = smoothEasing),
        label = "wearInnerBadgeRotationSpeed"
    )

    // Continuous smooth angular phase integration
    var outerRotationAngle by remember { mutableStateOf(0f) }
    var innerRotationAngle by remember { mutableStateOf(0f) }

    val currentOuterSpeed by rememberUpdatedState(animatedOuterSpeed)
    val currentInnerSpeed by rememberUpdatedState(animatedInnerSpeed)
    val currentFillRatio by rememberUpdatedState(fillRatio)

    LaunchedEffect(Unit) {
        var lastTimeNanos = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { nowNanos ->
                val dt = (nowNanos - lastTimeNanos) / 1_000_000_000f
                lastTimeNanos = nowNanos
                val safeDt = dt.coerceIn(0f, 0.1f)

                val newOuter = (outerRotationAngle + currentOuterSpeed * safeDt) % 360f

                // Lobe alignment synchronization (12 lobes = 30° period)
                // When starting to fill up (fillRatio >= 0.40f), gently guide inner angle into matching lobe phase
                val syncFactor = ((currentFillRatio - 0.40f) / 0.40f).coerceIn(0f, 1f)
                val smoothSync = syncFactor * syncFactor * (3f - 2f * syncFactor)
                val phaseError = if (smoothSync > 0f) {
                    val rawDiff = (newOuter - innerRotationAngle) % 30f
                    (rawDiff + 45f) % 30f - 15f
                } else {
                    0f
                }

                // Dynamic, responsive phase correction:
                // Gentle pull during early expansion, accelerating to immediate lock-in when close/arrived
                val phaseRate = if (currentFillRatio >= 0.75f) {
                    8.0f + 16.0f * ((currentFillRatio - 0.75f) / 0.25f).coerceIn(0f, 1f)
                } else {
                    1.5f + 4.5f * smoothSync
                }
                val phaseNudge = phaseError * phaseRate * smoothSync

                val newInner = (innerRotationAngle + (currentInnerSpeed + phaseNudge) * safeDt) % 360f

                outerRotationAngle = if (newOuter < 0f) newOuter + 360f else newOuter
                innerRotationAngle = if (newInner < 0f) newInner + 360f else newInner
            }
        }
    }

    val strokeWidth = 3.5.dp

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val strokeWidthPx = strokeWidth.toPx()

            // Outer scalloped boundary fits neatly inside canvas with safe padding to avoid clipping on round bezels
            val maxRadius = (this.size.width / 2f) - strokeWidthPx - 2.dp.toPx()

            // 1. Draw outer 12-lobed scalloped boundary stroke (spins clockwise very slowly)
            rotate(degrees = outerRotationAngle, pivot = Offset(cx, cy)) {
                val outerPath = buildScallopPath(cx, cy, maxRadius)

                // Lobe phase alignment (each lobe is 30 degrees = 360 / 12)
                val rawDiff = (innerRotationAngle - outerRotationAngle) % 30f
                val lobePhaseOffset = (rawDiff + 45f) % 30f - 15f

                // As inner shape fills outer badge (fillRatio: 0.70 -> 0.95),
                // smoothly eliminate any remaining lobe phase discrepancy so petals perfectly align
                val alignProgress = ((fillRatio - 0.70f) / 0.25f).coerceIn(0f, 1f)
                val smoothAlign = alignProgress * alignProgress * (3f - 2f * alignProgress)
                val effectiveInnerOffset = lobePhaseOffset * (1f - smoothAlign)

                // 2. Draw inner expanding 12-lobed scalloped filled shape:
                // Strictly clipped to outerPath so the inner filler NEVER overlaps or exceeds the outer boundary
                clipPath(path = outerPath) {
                    rotate(degrees = effectiveInnerOffset, pivot = Offset(cx, cy)) {
                        val fillRadius = (maxRadius * animatedScale + (strokeWidthPx / 2f) * animatedScale)
                            .coerceAtLeast(maxRadius * 0.25f)
                        val innerPath = buildScallopPath(cx, cy, fillRadius)
                        drawPath(
                            path = innerPath,
                            color = scallopColor,
                            style = Fill
                        )
                    }
                }

                // Draw outer boundary stroke on top of the fill
                drawPath(
                    path = outerPath,
                    color = scallopColor.copy(alpha = 0.35f),
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 3. Draw centered navigation arrow pointing towards target car
            val arrowWidth = (size * 0.17f).toPx()
            val arrowHeight = (size * 0.23f).toPx()
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
