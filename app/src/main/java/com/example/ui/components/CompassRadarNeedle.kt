package com.example.ui.components

import androidx.compose.animation.core.CubicBezierEasing
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
import android.os.SystemClock
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.util.HapticHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

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
    val context = LocalContext.current

    // Navigation tactile haptic feedback:
    // 1. Strong tactile click for every degree that the internal compass meets
    // 2. Bouncy strong click when the arrow points straight ahead, repeating with a bigger gap while staying straight
    val normalizedRelative = ((relativeArrowAngle % 360f + 540f) % 360f) - 180f
    val isStraightAhead = abs(normalizedRelative) <= 3.0f

    // Bouncy strong click loop when staying straight ahead (repeats with a bigger gap)
    LaunchedEffect(hasActiveTarget, isStraightAhead) {
        if (!hasActiveTarget || !isStraightAhead) return@LaunchedEffect
        while (isActive) {
            HapticHelper.performWaypointStraightAheadHaptic(context)
            delay(1350L) // Bigger gap in between when user stays on the straight
        }
    }

    // Strong tactile click for every 2 degrees that the internal compass meets when turning
    var lastHapticAngle by remember { mutableStateOf<Float?>(null) }
    var lastHapticTime by remember { mutableStateOf(0L) }

    LaunchedEffect(hasActiveTarget, compassAzimuth, isStraightAhead) {
        if (!hasActiveTarget || isStraightAhead) {
            lastHapticAngle = null
            return@LaunchedEffect
        }

        val now = SystemClock.uptimeMillis()
        val prevAngle = lastHapticAngle
        if (prevAngle == null) {
            lastHapticAngle = compassAzimuth
        } else {
            val diff = (compassAzimuth - prevAngle) % 360f
            val shortestDiff = (diff + 540f) % 360f - 180f
            if (abs(shortestDiff) >= 2.0f) {
                if (now - lastHapticTime >= 20L) {
                    HapticHelper.performWaypointDegreeClick(context)
                    lastHapticTime = now
                    lastHapticAngle = compassAzimuth
                }
            }
        }
    }

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
        animationSpec = tween(durationMillis = 2800, easing = smoothEasing),
        label = "outerBadgeRotationSpeed"
    )

    val animatedInnerSpeed by animateFloatAsState(
        targetValue = innerTargetSpeed,
        animationSpec = tween(durationMillis = 3200, easing = smoothEasing),
        label = "innerBadgeRotationSpeed"
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
                // Gentle, non-jerky phase pull that seamlessly locks petals together
                val phaseNudge = phaseError * 1.2f * smoothSync

                val newInner = (innerRotationAngle + (currentInnerSpeed + phaseNudge) * safeDt) % 360f

                outerRotationAngle = if (newOuter < 0f) newOuter + 360f else newOuter
                innerRotationAngle = if (newInner < 0f) newInner + 360f else newInner
            }
        }
    }

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

            // 1. Draw outer 12-lobed scalloped boundary stroke (spins clockwise very slowly)
            rotate(degrees = outerRotationAngle, pivot = Offset(cx, cy)) {
                val outerPath = buildScallopPath(cx, cy, maxRadius)

                // 2. Draw inner expanding 12-lobed scalloped filled shape:
                // Strictly clipped to outerPath so the inner filler NEVER overlaps or exceeds the outer boundary
                clipPath(path = outerPath) {
                    rotate(degrees = innerRotationAngle - outerRotationAngle, pivot = Offset(cx, cy)) {
                        val fillRadius = (maxRadius * animatedScale + (strokeWidthPx / 2f) * animatedScale).coerceAtLeast(maxRadius * 0.25f)
                        val innerPath = buildScallopPath(cx, cy, fillRadius)
                        drawPath(
                            path = innerPath,
                            color = innerFillColor,
                            style = Fill
                        )
                    }
                }

                // Draw outer boundary stroke on top of the fill
                drawPath(
                    path = outerPath,
                    color = outerStrokeColor,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

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

