package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.AppTab
import com.example.util.HapticHelper
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom High-Craft Navigation Bar Icon Animations (2.0s duration):
 *
 * 1. Car Icon (Dashboard): Stays completely fixed in place. Headlights do not cast large beams;
 *    instead, the two headlight circles flash white twice like a driver flashing their high beams,
 *    returning to resting color. Haptics authentically recreate a ferocious Hellcat V8 supercharged
 *    engine startup roar and heavy idle rumble.
 *
 * 2. Waypoint Arrow (Compass/Radar): Remains fixed in its central axis. Points far left (-50°),
 *    then far right (+50°), then center (0°), and smoothly settles back to its resting heading.
 *
 * 3. History Icon (Clock & Arrow): The clock hands remain centered while spinning rapidly through a full
 *    24-hour cycle. Surrounding the clock hands, the circular arrow rotates anticlockwise with a clear
 *    anticlockwise pointing arrow tip.
 *
 * 4. Gear Icon (Settings): Industrial mechanical spur gear with 6 square-profile teeth and clean center hub,
 *    eliminating the ship-wheel spoke aesthetic. Rotates smoothly slowing down, with ultra strong clicky
 *    haptics matching each tooth striking as it rotates.
 */
@Composable
fun AnimatedNavIcon(
    tab: AppTab,
    icon: ImageVector,
    contentDescription: String?,
    tint: Color,
    triggerCount: Int,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    val context = LocalContext.current
    val animMillis = remember { Animatable(0f) }

    // Track total accumulated half-turns for history icon so it stays at its final rotation after each animation
    val historyRotationAnim = remember { Animatable(0f) }

    LaunchedEffect(triggerCount) {
        if (triggerCount > 0) {
            // Trigger dedicated hardware LRA haptic pattern
            when (tab) {
                AppTab.DASHBOARD -> HapticHelper.performCarEngineStartHaptic(context)
                AppTab.COMPASS_RADAR -> HapticHelper.performWaypointPathHaptic(context)
                AppTab.HISTORY -> HapticHelper.performHistorySpinHaptic(context)
                AppTab.SETTINGS -> HapticHelper.performGearMechanicalClickHaptic(context)
                else -> {}
            }

            if (tab == AppTab.HISTORY) {
                // Animate squiggly arrow by an additional +180° (half turn) with smooth start & finish, staying at the target
                val targetRot = historyRotationAnim.value + 180f
                launch {
                    historyRotationAnim.animateTo(
                        targetValue = targetRot,
                        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
                    )
                }
            }

            // Run organic animation (2000ms for all tabs including slow mechanical spin for Settings gear)
            val duration = 2000
            animMillis.snapTo(0f)
            animMillis.animateTo(
                targetValue = duration.toFloat(),
                animationSpec = tween(durationMillis = duration, easing = LinearEasing)
            )
        }
    }

    val currentMs = animMillis.value

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        when (tab) {
            AppTab.DASHBOARD -> {
                CarDashboardNavIcon(
                    icon = icon,
                    contentDescription = contentDescription,
                    tint = tint,
                    ms = currentMs,
                    size = size
                )
            }
            AppTab.COMPASS_RADAR -> {
                WaypointNavIcon(
                    icon = icon,
                    contentDescription = contentDescription,
                    tint = tint,
                    ms = currentMs,
                    size = size
                )
            }
            AppTab.HISTORY -> {
                HistoryClockNavIcon(
                    contentDescription = contentDescription,
                    tint = tint,
                    ms = currentMs,
                    bodyRotation = historyRotationAnim.value,
                    size = size
                )
            }
            AppTab.SETTINGS -> {
                FullGearNavIcon(
                    icon = icon,
                    contentDescription = contentDescription,
                    tint = tint,
                    ms = currentMs,
                    size = size
                )
            }
            else -> {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(size)
                )
            }
        }
    }
}

/**
 * Car Icon (Fixed position, headlight circles flash white twice like high-beam flashing).
 */
@Composable
private fun CarDashboardNavIcon(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color,
    ms: Float,
    size: Dp
) {
    // Two distinct high-beam flashes:
    // Flash 1: 150ms -> 450ms (peak around 300ms)
    // Dark gap: 450ms -> 600ms
    // Flash 2: 600ms -> 900ms (peak around 750ms)
    // Off: 900ms -> 2000ms
    val flashIntensity = when {
        ms in 150f..450f -> {
            val p = (ms - 150f) / 300f
            sin(p * PI.toFloat())
        }
        ms in 600f..900f -> {
            val p = (ms - 600f) / 300f
            sin(p * PI.toFloat())
        }
        else -> 0f
    }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Fixed Car Icon - never moves
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size)
        )

        // Headlight circles flash white twice like high-beam flashing - NO beam cone
        if (flashIntensity > 0f) {
            val baseColor = Color.White
            val auraColor = Color(0xFFE0F7FA)
            
            Canvas(modifier = Modifier.size(size)) {
                val w = this.size.width
                val h = this.size.height

                // DirectionsCar headlight circle positions on 24x24 grid: (6.5, 14.5) and (17.5, 14.5)
                val leftX = w * (6.5f / 24f)
                val rightX = w * (17.5f / 24f)
                val bulbY = h * (14.5f / 24f)
                val bulbRadius = w * (1.75f / 24f)

                val flashColor = baseColor.copy(alpha = flashIntensity.coerceIn(0f, 1f))
                val outerAura = auraColor.copy(alpha = (flashIntensity * 0.5f).coerceIn(0f, 1f))

                // Outer crisp aura
                drawCircle(color = outerAura, radius = bulbRadius * 1.5f, center = Offset(leftX, bulbY))
                drawCircle(color = outerAura, radius = bulbRadius * 1.5f, center = Offset(rightX, bulbY))

                // High-intensity flashing headlight bulbs
                drawCircle(color = flashColor, radius = bulbRadius, center = Offset(leftX, bulbY))
                drawCircle(color = flashColor, radius = bulbRadius, center = Offset(rightX, bulbY))
            }
        }
    }
}

/**
 * Waypoint Arrow Nav Icon:
 * Custom drawn crisp directional arrow with exactly 2dp rounded corners on all edges/corners,
 * pointing far left (-50°), then far right (+50°), then center (0°), settling into resting heading.
 */
@Composable
private fun WaypointNavIcon(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color,
    ms: Float,
    size: Dp
) {
    val restingAngle = 0f

    // 0 - 500ms: Point to Far Left (-50°)
    // 500 - 1100ms: Point to Far Right (+50°)
    // 1100 - 1600ms: Point to Center (0°)
    // 1600 - 2000ms: Smoothly settle to resting position
    val currentAngle = when {
        ms <= 0f || ms >= 2000f -> restingAngle
        ms < 500f -> {
            val p = ms / 500f
            val smooth = (1f - cos(p * PI.toFloat())) * 0.5f
            restingAngle + (-50f * smooth)
        }
        ms < 1100f -> {
            val p = (ms - 500f) / 600f
            val smooth = (1f - cos(p * PI.toFloat())) * 0.5f
            -50f + (100f * smooth) // from -50° to +50°
        }
        ms < 1600f -> {
            val p = (ms - 1100f) / 500f
            val smooth = (1f - cos(p * PI.toFloat())) * 0.5f
            50f - (50f * smooth) // from +50° to 0° (Center)
        }
        else -> {
            val p = (ms - 1600f) / 400f
            val damp = (1f - p).coerceIn(0f, 1f)
            val bounce = sin(p * PI.toFloat() * 2f) * damp * 8f
            restingAngle + bounce
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                // Fixed in its central axis (no translations)
                rotationZ = currentAngle
            },
        contentAlignment = Alignment.Center
    ) {
        // Draw custom waypoint arrow with rounder edges on its 3 outer points and inner notch
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f

            // Waypoint arrow vertices centered on canvas:
            // Tip points up: (cx, cy - h * 0.38f)
            // Bottom-right wing: (cx + w * 0.32f, cy + h * 0.36f)
            // Inner notch: (cx, cy + h * 0.16f)
            // Bottom-left wing: (cx - w * 0.32f, cy + h * 0.36f)
            val tip = Offset(cx, cy - h * 0.38f)
            val rightWing = Offset(cx + w * 0.32f, cy + h * 0.36f)
            val notch = Offset(cx, cy + h * 0.16f)
            val leftWing = Offset(cx - w * 0.32f, cy + h * 0.36f)

            // Prominently round corner radius for the 3 outer points (tip, right wing, left wing)
            val outerCornerRadiusPx = 4.5.dp.toPx()
            val notchCornerRadiusPx = 2.5.dp.toPx()

            // Function to generate path with rounded corners by cutting back along edges and inserting curves
            val points = listOf(tip, rightWing, notch, leftWing)
            val n = points.size
            val path = Path()

            for (i in 0 until n) {
                val prev = points[(i - 1 + n) % n]
                val curr = points[i]
                val next = points[(i + 1) % n]

                val vPrev = prev - curr
                val vNext = next - curr
                val lenPrev = kotlin.math.hypot(vPrev.x, vPrev.y)
                val lenNext = kotlin.math.hypot(vNext.x, vNext.y)

                // Apply larger radius to the 3 outer points (indices 0, 1, 3)
                val targetRadius = if (i == 2) notchCornerRadiusPx else outerCornerRadiusPx
                val r = targetRadius.coerceAtMost((lenPrev * 0.42f).coerceAtMost(lenNext * 0.42f))

                val startPt = curr + Offset(vPrev.x / lenPrev * r, vPrev.y / lenPrev * r)
                val endPt = curr + Offset(vNext.x / lenNext * r, vNext.y / lenNext * r)

                if (i == 0) {
                    path.moveTo(startPt.x, startPt.y)
                } else {
                    path.lineTo(startPt.x, startPt.y)
                }
                path.quadraticTo(curr.x, curr.y, endPt.x, endPt.y)
            }
            path.close()

            drawPath(path = path, color = tint, style = Fill)
        }
    }
}

/**
 * History Clock Nav Icon:
 * The clock hands remain fixed at the center while spinning quickly to complete a full 24 hours
 * (hour hand completes 2 full rotations = 720°, minute hand spins 24 rotations = 8640°),
 * while the outer circular arrow counter-rotates / rotates around them.
 */
/**
 * History Clock Nav Icon:
 * - Outer squiggly circle has a smooth rounded triangular arrow tip showing its clockwise rotation direction.
 * - Rotates at half speed and half distance (180° clockwise) with a smooth ease-in-out start and finish.
 * - Maintains its organic traveling ripple snake animation.
 * - Clock hands rest at 10:10 and spin counter-clockwise for 12 hours.
 */
@Composable
private fun HistoryClockNavIcon(
    contentDescription: String?,
    tint: Color,
    ms: Float,
    bodyRotation: Float,
    size: Dp
) {
    // 10:10 Watch Hands baseline angles (measured clockwise from 12 o'clock / -Y):
    // 10 hours and 10 minutes: 10 * 30° + (10/60) * 30° = 300° + 5° = 305°
    val baseHourAngle = 305f
    // 10 minutes: 10 * 6° = 60°
    val baseMinuteAngle = 60f

    // 12-hour counter-clockwise progression:
    // 0ms to 1700ms: Hour hand spins counter-clockwise by -360° (1 full turn), minute hand spins counter-clockwise by -(12 * 360°)
    // 1700ms to 2000ms: Softly settles back to resting 10:10 position
    val hourHandAngle = when {
        ms <= 0f || ms >= 2000f -> baseHourAngle
        ms < 1700f -> {
            val p = ms / 1700f
            val smooth = (1f - cos(p * PI.toFloat())) * 0.5f
            baseHourAngle - (360f * smooth) // Counter-clockwise 1 full turn (12 hours)
        }
        else -> baseHourAngle
    }

    val minuteHandAngle = when {
        ms <= 0f || ms >= 2000f -> baseMinuteAngle
        ms < 1700f -> {
            val p = ms / 1700f
            val smooth = (1f - cos(p * PI.toFloat())) * 0.5f
            baseMinuteAngle - ((12f * 360f) * smooth) // Counter-clockwise 12 full turns (12 hours)
        }
        else -> baseMinuteAngle
    }

    // Snake-like wave phase shift traveling along the squiggly body (gentle 1 cycle)
    val wavePhase = if (ms in 1f..1999f) {
        val p = (ms / 2000f).coerceIn(0f, 1f)
        val smooth = (1f - cos(p * PI.toFloat())) * 0.5f
        smooth * 2f * PI.toFloat()
    } else {
        0f
    }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer squiggly ring with attached smooth triangular arrow tip, rotating clockwise
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer { rotationZ = bodyRotation },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val w = this.size.width
                val center = Offset(w / 2f, w / 2f)
                val baseRadius = w * 0.38f
                val strokeWidth = w * 0.082f
                val waveAmplitude = w * 0.042f

                // Arc runs clockwise from 180° to 125° (sweep of 305°)
                val startDeg = 180f
                val totalSweepDeg = 305f
                val numWaves = 6.5f
                val steps = 120
                val path = Path()

                var lastX = 0f
                var lastY = 0f
                var secondLastX = 0f
                var secondLastY = 0f

                for (i in 0..steps) {
                    val frac = i.toFloat() / steps
                    val angleDeg = startDeg + (frac * totalSweepDeg)
                    val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()

                    // Sine wave displacement perpendicular to the circle radius
                    val ripple = sin(frac * numWaves * 2f * PI.toFloat() - wavePhase) * waveAmplitude
                    val r = baseRadius + ripple

                    val x = center.x + r * cos(angleRad)
                    val y = center.y + r * sin(angleRad)

                    if (i == steps - 1) {
                        secondLastX = x
                        secondLastY = y
                    }
                    if (i == steps) {
                        lastX = x
                        lastY = y
                    }

                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                // Draw squiggly track
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Add simple triangular arrow tip with no rough edges at the end of the arc
                // Tangent vector of the arc at its end pointing in the clockwise direction
                val dx = lastX - secondLastX
                val dy = lastY - secondLastY
                val tangentAngle = atan2(dy, dx)

                // Arrowhead dimensions with smooth rounded corners (no rough edges)
                val arrowHeadLen = w * 0.17f
                val arrowHeadWidth = w * 0.17f
                val tipCornerRadius = 1.8.dp.toPx()

                // Triangle tip point extends along tangent
                val tip = Offset(
                    lastX + arrowHeadLen * cos(tangentAngle),
                    lastY + arrowHeadLen * sin(tangentAngle)
                )

                // Normal vector perpendicular to tangent
                val normalAngle = tangentAngle + (PI.toFloat() / 2f)
                val baseLeft = Offset(
                    lastX + (arrowHeadWidth * 0.5f) * cos(normalAngle),
                    lastY + (arrowHeadWidth * 0.5f) * sin(normalAngle)
                )
                val baseRight = Offset(
                    lastX - (arrowHeadWidth * 0.5f) * cos(normalAngle),
                    lastY - (arrowHeadWidth * 0.5f) * sin(normalAngle)
                )

                // Draw simple triangular arrow tip with rounded corners
                val arrowPath = Path()
                val trianglePoints = listOf(tip, baseLeft, baseRight)
                val n = trianglePoints.size
                for (j in 0 until n) {
                    val prev = trianglePoints[(j - 1 + n) % n]
                    val curr = trianglePoints[j]
                    val next = trianglePoints[(j + 1) % n]

                    val vPrev = prev - curr
                    val vNext = next - curr
                    val lenPrev = kotlin.math.hypot(vPrev.x, vPrev.y)
                    val lenNext = kotlin.math.hypot(vNext.x, vNext.y)

                    val r = tipCornerRadius.coerceAtMost((lenPrev * 0.35f).coerceAtMost(lenNext * 0.35f))

                    val startPt = curr + Offset(vPrev.x / lenPrev * r, vPrev.y / lenPrev * r)
                    val endPt = curr + Offset(vNext.x / lenNext * r, vNext.y / lenNext * r)

                    if (j == 0) {
                        arrowPath.moveTo(startPt.x, startPt.y)
                    } else {
                        arrowPath.lineTo(startPt.x, startPt.y)
                    }
                    arrowPath.quadraticTo(curr.x, curr.y, endPt.x, endPt.y)
                }
                arrowPath.close()

                drawPath(path = arrowPath, color = tint, style = Fill)
            }
        }

        // Inner Clock Hands showing 10:10, spinning counter-clockwise for 12 hours
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2f, h / 2f)

            // Center pivot pin
            drawCircle(color = tint, radius = w * 0.08f, center = center)

            // Hour Hand (shorter, thicker)
            rotate(degrees = hourHandAngle, pivot = center) {
                drawLine(
                    color = tint,
                    start = center,
                    end = Offset(center.x, center.y - h * 0.22f),
                    strokeWidth = w * 0.10f,
                    cap = StrokeCap.Round
                )
            }

            // Minute Hand (longer, slightly leaner)
            rotate(degrees = minuteHandAngle, pivot = center) {
                drawLine(
                    color = tint,
                    start = center,
                    end = Offset(center.x, center.y - h * 0.32f),
                    strokeWidth = w * 0.075f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Gear Nav Icon (Settings):
 * Mechanical spur gear with a wider main hub body and prominently rounded tooth corners (2.4 dp corner radius).
 * Starting and ending position has one of its gears looking directly upwards (12 o'clock).
 * Rotates smoothly with organic deceleration over 2 seconds, synchronized with tactile tooth clicks.
 */
@Composable
fun FullGearNavIcon(
    icon: ImageVector = Icons.Default.Settings,
    contentDescription: String? = null,
    tint: Color,
    ms: Float = 0f,
    size: Dp = 24.dp
) {
    // Rotation curve: Starts acceleration slowly with mechanical inertia, progressively ramps up
    // to peak rotational speed (around ~560ms), smoothly decelerates, and as it reaches its starting point (360°),
    // performs a tiny mechanical bounce (gentle overshoot and springy recoil) before settling into rest.
    val gearDuration = 2000f
    val rotation = when {
        ms <= 0f || ms >= gearDuration -> 0f
        else -> {
            val t = (ms / gearDuration).coerceIn(0f, 1f)
            val t0 = 0.28f
            val k = 2.4f
            val totalRotation = 360f
            val areaFactor = (t0 / 2f) + ((1f - t0) / (k + 1f))
            val vPeak = totalRotation / areaFactor
            val baseRotation = if (t <= t0) {
                // Acceleration phase: starts slowly with gentle mechanical inertia (quintic smooth onset),
                // building up to peak rotational speed at t0
                val p = t / t0
                // Integral of smootherstep 6*p^5 - 15*p^4 + 10*p^3:
                // integral = p^4 * (p^2 - 3p + 2.5), which reaches 0.5 at p = 1.0
                val accelProgress = p * p * p * p * (p * p - 3f * p + 2.5f)
                vPeak * t0 * accelProgress
            } else {
                // Deceleration phase: smoothly decelerates from vPeak to 0 velocity, stopping gently at 360°
                val a1 = vPeak * (t0 / 2f)
                val remainingRatio = (1f - t) / (1f - t0)
                val decelIntegral = ((1f - t0) / (k + 1f)) * (1f - Math.pow(remainingRatio.toDouble(), (k + 1.0)).toFloat())
                a1 + vPeak * decelIntegral
            }

            // Tiny mechanical rotational bounce as the gear returns to starting position (1650ms - 2000ms):
            // Softly overshoots past 360° by ~3.7°, then recoils back to settle cleanly at 360° (0°) resting position.
            val bounce = if (ms >= 1650f) {
                val tb = (ms - 1650f) / 350f
                (14f * kotlin.math.sin(tb * 1.7f * Math.PI.toFloat()) * (1f - tb) * Math.pow(tb.toDouble(), 0.7).toFloat())
            } else {
                0f
            }
            baseRotation + bounce
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                rotationZ = rotation
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f

            // Gear geometry: 6 prominent spur teeth with wider main hub body & extra rounded corners
            val numTeeth = 6
            val innerRadius = w * 0.355f  // Wider main body
            val outerRadius = w * 0.465f  // Outer tooth reach
            // Extra rounded tooth corners: 2.4 dp corner radius at 24 dp size (scaled proportionally with size)
            val cornerRadiusDp = (size.value * 0.10f).dp // Exactly 2.4 dp at 24 dp size
            val toothCornerRadius = cornerRadiusDp.toPx()

            val path = Path()

            // Starting angle offset (-PI / 2): centers tooth 0 pointing directly UPWARDS at 12 o'clock
            val startAngle = -PI.toFloat() / 2f
            val angleStep = (2f * PI / numTeeth).toFloat()
            val halfToothAngle = angleStep * 0.3f
            val halfRootAngle = angleStep * 0.34f

            for (i in 0 until numTeeth) {
                // Tooth 0 centered at -PI/2 (directly upwards at 12 o'clock)
                val toothCenterAngle = startAngle + (i * angleStep)

                val rootLeft = toothCenterAngle - halfRootAngle
                val tipLeft = toothCenterAngle - halfToothAngle
                val tipRight = toothCenterAngle + halfToothAngle
                val rootRight = toothCenterAngle + halfRootAngle

                val tipLeftPt = Offset(cx + outerRadius * cos(tipLeft), cy + outerRadius * sin(tipLeft))
                val tipRightPt = Offset(cx + outerRadius * cos(tipRight), cy + outerRadius * sin(tipRight))

                val tipLeftBefore = Offset(
                    cx + (outerRadius - toothCornerRadius) * cos(tipLeft),
                    cy + (outerRadius - toothCornerRadius) * sin(tipLeft)
                )
                val tipLeftAfter = Offset(
                    cx + outerRadius * cos(tipLeft + toothCornerRadius / outerRadius),
                    cy + outerRadius * sin(tipLeft + toothCornerRadius / outerRadius)
                )

                val tipRightBefore = Offset(
                    cx + outerRadius * cos(tipRight - toothCornerRadius / outerRadius),
                    cy + outerRadius * sin(tipRight - toothCornerRadius / outerRadius)
                )
                val tipRightAfter = Offset(
                    cx + (outerRadius - toothCornerRadius) * cos(tipRight),
                    cy + (outerRadius - toothCornerRadius) * sin(tipRight)
                )

                val rootLeftPt = Offset(cx + innerRadius * cos(rootLeft), cy + innerRadius * sin(rootLeft))
                val rootRightPt = Offset(cx + innerRadius * cos(rootRight), cy + innerRadius * sin(rootRight))

                if (i == 0) {
                    path.moveTo(rootLeftPt.x, rootLeftPt.y)
                } else {
                    path.lineTo(rootLeftPt.x, rootLeftPt.y)
                }

                // Rise to tooth tip
                path.lineTo(tipLeftBefore.x, tipLeftBefore.y)
                // Generously round tooth top-left corner (2.4 dp radius)
                path.quadraticTo(tipLeftPt.x, tipLeftPt.y, tipLeftAfter.x, tipLeftAfter.y)
                // Across tooth crest
                path.lineTo(tipRightBefore.x, tipRightBefore.y)
                // Generously round tooth top-right corner (2.4 dp radius)
                path.quadraticTo(tipRightPt.x, tipRightPt.y, tipRightAfter.x, tipRightAfter.y)
                // Descent to root
                path.lineTo(rootRightPt.x, rootRightPt.y)
            }
            path.close()

            // Punch out true center bore using EvenOdd fill type
            val boreRadius = w * 0.15f
            path.addOval(
                Rect(
                    left = cx - boreRadius,
                    top = cy - boreRadius,
                    right = cx + boreRadius,
                    bottom = cy + boreRadius
                )
            )
            path.fillType = PathFillType.EvenOdd

            // Draw gear body with punched-out center bore
            drawPath(path = path, color = tint, style = Fill)
        }
    }
}
