package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws the stopwatch / timer icon with custom tint color options (blue or red for stopped state):
 * - Top pusher: deep horizontal capsule/pill with neck centered at 12 o'clock
 * - Side pusher: rounded tab angled outward at ~60° (2 o'clock position)
 * - Outer casing: solid circular shell
 * - Inner dial face: inset circle with gradient
 * - Clock hands: Starting at 10:10 (Hour hand at 10 o'clock, Minute hand at 10 minutes / 2 o'clock)
 *   with smooth slow rotation for both hands.
 */
fun DrawScope.drawExactStopwatchClock(
    center: Offset,
    radius: Float = 19f,
    needleRotationDegrees: Float = 0f,
    minuteHandRotationDegrees: Float = needleRotationDegrees * 12f,
    isRedStopped: Boolean = false
) {
    val cx = center.x
    val cy = center.y

    // Color palettes matching reference screenshot
    val sidePusherGradient = if (isRedStopped) {
        listOf(Color(0xFFFF8A80), Color(0xFFEA4335), Color(0xFFD93025))
    } else {
        listOf(Color(0xFF5CA4FC), Color(0xFF388DF8), Color(0xFF2676E8))
    }

    val neckColor = if (isRedStopped) Color(0xFFB71C1C) else Color(0xFF274DE0)

    val topBtnGradient = if (isRedStopped) {
        listOf(Color(0xFFEF5350), Color(0xFFD32F2F), Color(0xFFB71C1C))
    } else {
        listOf(Color(0xFF3B67F6), Color(0xFF2850E2), Color(0xFF1E3FD0))
    }

    val casingGradient = if (isRedStopped) {
        listOf(Color(0xFFEF5350), Color(0xFFE53935), Color(0xFFC62828))
    } else {
        listOf(Color(0xFF3E70F8), Color(0xFF3262F0), Color(0xFF254FE0))
    }

    val dialGradient = if (isRedStopped) {
        listOf(
            Color(0xFFFF8A80),
            Color(0xFFE53935),
            Color(0xFFB71C1C)
        )
    } else {
        listOf(
            Color(0xFF7B8CF8),
            Color(0xFF5572F6),
            Color(0xFF2E57EE)
        )
    }

    val handColor = if (isRedStopped) Color(0xFF4A0007) else Color(0xFF142054)

    // 1. Minutes Tab / Pusher (positioned at the 10th minute: 60° from 12 o'clock)
    val minutesTabAngle = 60f
    val minutesTabWidth = radius * 0.38f
    val minutesTabLength = radius * 0.34f
    withTransform({
        rotate(minutesTabAngle, center)
    }) {
        val btnLeft = cx - minutesTabWidth / 2f
        val btnTop = cy - radius - minutesTabLength + 1.2f
        val btnRight = cx + minutesTabWidth / 2f
        val btnBottom = cy - radius + 1.8f
        val minutesTabPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = btnLeft,
                    top = btnTop,
                    right = btnRight,
                    bottom = btnBottom,
                    cornerRadius = CornerRadius(radius * 0.12f, radius * 0.12f)
                )
            )
        }
        drawPath(
            path = minutesTabPath,
            brush = Brush.verticalGradient(
                colors = sidePusherGradient,
                startY = btnTop,
                endY = btnBottom
            )
        )
    }

    // 2. Top Pusher / Crown (12 o'clock)
    // Connecting neck
    val neckW = radius * 0.36f
    val neckH = radius * 0.16f
    drawRoundRect(
        color = neckColor,
        topLeft = Offset(cx - neckW / 2f, cy - radius - neckH * 0.6f),
        size = Size(neckW, neckH),
        cornerRadius = CornerRadius(neckW * 0.2f, neckW * 0.2f)
    )

    // Main top button pill / capsule
    val topBtnW = radius * 0.82f
    val topBtnH = radius * 0.34f
    val topBtnTop = cy - radius - topBtnH * 0.95f
    val topBtnPath = Path().apply {
        addRoundRect(
            RoundRect(
                left = cx - topBtnW / 2f,
                top = topBtnTop,
                right = cx + topBtnW / 2f,
                bottom = topBtnTop + topBtnH,
                cornerRadius = CornerRadius(topBtnH / 2f, topBtnH / 2f)
            )
        )
    }
    drawPath(
        path = topBtnPath,
        brush = Brush.verticalGradient(
            colors = topBtnGradient,
            startY = topBtnTop,
            endY = topBtnTop + topBtnH
        )
    )

    // 3. Outer Circular Body / Casing
    drawCircle(
        brush = Brush.radialGradient(
            colors = casingGradient,
            center = Offset(cx - radius * 0.15f, cy - radius * 0.15f),
            radius = radius * 1.25f
        ),
        radius = radius,
        center = center
    )

    // 4. Dial Face (Inset circular region)
    val rDial = radius * 0.84f
    val dialPath = Path().apply {
        addOval(Rect(cx - rDial, cy - rDial, cx + rDial, cy + rDial))
    }

    clipPath(dialPath) {
        drawCircle(
            brush = Brush.linearGradient(
                colors = dialGradient,
                start = Offset(cx - rDial * 0.4f, cy - rDial * 0.9f),
                end = Offset(cx + rDial * 0.6f, cy + rDial * 0.9f)
            ),
            radius = rDial,
            center = center
        )

        // Starting point: 10:10 on the clock
        // Hour hand pointing at 10 o'clock (215° in Cartesian, top-left direction)
        val hourBaseAngleDeg = 215.0 + needleRotationDegrees
        val hourAngleRad = Math.toRadians(hourBaseAngleDeg).toFloat()
        val hdx = cos(hourAngleRad)
        val hdy = sin(hourAngleRad)

        val hourLen = rDial * 0.52f
        val hourStroke = rDial * 0.20f

        val hourStartOffset = Offset(cx - hdx * 1.5f, cy - hdy * 1.5f)
        val hourEndOffset = Offset(cx + hdx * hourLen, cy + hdy * hourLen)

        drawLine(
            color = handColor,
            start = hourStartOffset,
            end = hourEndOffset,
            strokeWidth = hourStroke,
            cap = StrokeCap.Round
        )

        // Minute hand pointing at 10-minute position (2 o'clock mark, 330° in Cartesian, top-right direction)
        val minuteBaseAngleDeg = 330.0 + minuteHandRotationDegrees
        val minuteAngleRad = Math.toRadians(minuteBaseAngleDeg).toFloat()
        val mdx = cos(minuteAngleRad)
        val mdy = sin(minuteAngleRad)

        val minuteLen = rDial * 0.72f
        val minuteStroke = rDial * 0.15f

        val minuteStartOffset = Offset(cx - mdx * 1.5f, cy - mdy * 1.5f)
        val minuteEndOffset = Offset(cx + mdx * minuteLen, cy + mdy * minuteLen)

        drawLine(
            color = handColor,
            start = minuteStartOffset,
            end = minuteEndOffset,
            strokeWidth = minuteStroke,
            cap = StrokeCap.Round
        )

        // Rounded Center Pivot Cap
        drawCircle(
            color = handColor,
            radius = hourStroke * 0.70f,
            center = center
        )
    }
}

/**
 * Creates the organic 4-lobed / rounded puffy squircle badge matching the top-left key badge in the screenshot.
 */
private fun createKeyBadgePath(radius: Float): Path {
    val path = Path()
    val numPoints = 80
    for (i in 0..numPoints) {
        val angle = (i.toFloat() / numPoints) * 2f * PI.toFloat()
        val rad = radius * (0.84f + 0.16f * cos(4f * angle - (PI / 4f).toFloat()))
        val x = rad * cos(angle)
        val y = rad * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/**
 * Creates an 8-lobed organic scalloped cookie/cloud shape path centered at (0,0).
 */
private fun createScallopPath(
    radiusMax: Float,
    radiusMin: Float,
    numLobes: Int = 8,
    phase: Float = 0f,
    samples: Int = 120
): Path {
    val path = Path()
    val rAvg = (radiusMax + radiusMin) / 2f
    val rAmp = (radiusMax - radiusMin) / 2f

    for (i in 0..samples) {
        val angle = (i.toFloat() / samples) * 2f * PI.toFloat()
        val r = rAvg + rAmp * cos(numLobes * angle + phase)
        val x = r * cos(angle)
        val y = r * sin(angle)
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
 * Stateful independent badge spinner:
 * - Completely independent speed, direction, and timing for each drawer icon shape.
 * - Smooth velocity transition across direction reversals (using C1 smoothstep over ~0.65-0.90s)
 *   so the turnaround is fluid and natural without coming to an extended stop or lingering.
 */
private class IndependentBadgeSpinner(
    initialDirection: Float, // +1f for CW, -1f for CCW
    private val minSpeed: Float,
    private val maxSpeed: Float,
    seed: Long,
    initialPhaseOffsetSec: Float = 0f
) {
    private val random = kotlin.random.Random(seed)
    var currentAngle: Float = random.nextFloat() * 360f
        private set

    private var currentDirection: Float = initialDirection
    private var currentSpeed: Float = minSpeed + random.nextFloat() * (maxSpeed - minSpeed)

    private var isReversing: Boolean = false
    private var stateElapsedSec: Float = initialPhaseOffsetSec
    // Independent cruise duration between 3.0s and 7.0s
    private var cruiseDurationSec: Float = 3.0f + random.nextFloat() * 4.0f

    // Turnaround duration: concise ~0.65s to 0.88s so it never stays stopped for long
    private var reverseDurationSec: Float = 0.72f + random.nextFloat() * 0.16f
    private var fromVelocity: Float = 0f
    private var toVelocity: Float = 0f

    fun update(deltaSec: Float) {
        stateElapsedSec += deltaSec

        if (!isReversing) {
            val velocity = currentDirection * currentSpeed
            val newAngle = (currentAngle + velocity * deltaSec) % 360f
            currentAngle = if (newAngle < 0f) newAngle + 360f else newAngle

            if (stateElapsedSec >= cruiseDurationSec) {
                isReversing = true
                stateElapsedSec = 0f
                fromVelocity = currentDirection * currentSpeed

                // Independently reverse direction and pick a new speed within its unique envelope
                currentDirection = -currentDirection
                currentSpeed = minSpeed + random.nextFloat() * (maxSpeed - minSpeed)
                toVelocity = currentDirection * currentSpeed

                // Concise smooth turnaround window
                reverseDurationSec = 0.68f + random.nextFloat() * 0.20f
                // Next independent cruise duration (1 to 2.5 complete turns worth of time)
                val nextRotations = 1.0f + random.nextFloat() * 1.5f
                cruiseDurationSec = (nextRotations * 360f) / currentSpeed
            }
        } else {
            // Smooth velocity transition: 3t^2 - 2t^3 Hermite smoothstep
            // Guarantees continuous acceleration with zero lingering at the stop
            val t = (stateElapsedSec / reverseDurationSec).coerceIn(0f, 1f)
            val smoothT = t * t * (3f - 2f * t)
            val instantaneousVelocity = fromVelocity + (toVelocity - fromVelocity) * smoothT
            val newAngle = (currentAngle + instantaneousVelocity * deltaSec) % 360f
            currentAngle = if (newAngle < 0f) newAngle + 360f else newAngle

            if (stateElapsedSec >= reverseDurationSec) {
                isReversing = false
                stateElapsedSec = 0f
            }
        }
    }
}

/**
 * Utility Drawer Hero Graphic
 * Exact 1:1 match to reference screenshot:
 * - Cabinet: 3-drawer dark slate dresser (#363B4E) with 2 dividers (#7B8198), 3 circular knobs (#CDD1F8), and arched legs
 * - 4 icons in total:
 *    1. Stopwatch / Clock Icon: positioned on top of the cabinet, touching top edge
 *    2. Key Icon inside top-left 4-corner puffy cloud shape
 *    3. Folder Icon inside bottom-left 8-lobed scalloped cloud shape
 *    4. Crossed Tools Icon (Wrench & Hammer) inside middle-right 8-lobed scalloped cloud shape
 * - Exact static positioning matching the reference image.
 */
@Composable
fun UtilityDrawerHeroBadge(
    modifier: Modifier = Modifier.size(width = 250.dp, height = 180.dp)
) {
    val colorDresser = Color(0xFF363B4E)
    val colorDivider = Color(0xFF7B8198)
    val colorKnob = Color(0xFFCDD1F8)
    val colorBadgeCloud = Color(0xFFCDD1F8)
    val colorBadgeIcon = Color(0xFF30354E)

    // Animated smooth rotation values:
    // Surrounding shape badges rotate a few turns in one direction, then smoothly reverse and rotate a few turns in the other direction.
    // - Turn counts are randomly chosen and strictly never exceed 3 rotations in the same direction.
    // - Transition between direction changes uses Ken Perlin's quintic smootherstep (zero velocity & zero acceleration at reversal points).
    // - Spinning animation speed is noticeably slower (~36-40 deg/sec).
    var badge1Angle by remember { mutableFloatStateOf(0f) }
    var badge2Angle by remember { mutableFloatStateOf(0f) }
    var badge3Angle by remember { mutableFloatStateOf(0f) }
    var clockMinuteSpin by remember { mutableFloatStateOf(0f) }
    var clockHourSpin by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val spinner1 = IndependentBadgeSpinner(
            initialDirection = 1f,
            minSpeed = 16f,
            maxSpeed = 22f,
            seed = 101L,
            initialPhaseOffsetSec = 0.8f
        )
        val spinner2 = IndependentBadgeSpinner(
            initialDirection = -1f,
            minSpeed = 10f,
            maxSpeed = 15f,
            seed = 202L,
            initialPhaseOffsetSec = 3.2f
        )
        val spinner3 = IndependentBadgeSpinner(
            initialDirection = 1f,
            minSpeed = 20f,
            maxSpeed = 28f,
            seed = 303L,
            initialPhaseOffsetSec = 1.9f
        )

        val startTime = withFrameNanos { it }
        var lastFrameNanos = startTime
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                val deltaSec = ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
                lastFrameNanos = frameTimeNanos
                val elapsedSec = (frameTimeNanos - startTime) / 1_000_000_000f

                spinner1.update(deltaSec)
                spinner2.update(deltaSec)
                spinner3.update(deltaSec)

                badge1Angle = spinner1.currentAngle
                badge2Angle = spinner2.currentAngle
                badge3Angle = spinner3.currentAngle

                // Smooth clock hands rotation (tuned slightly slower to match the relaxed tempo)
                fun computeClockRotation(targetSpeedDegPerSec: Float, rampUpSec: Float = 0.85f): Float {
                    return if (elapsedSec < rampUpSec) {
                        0.5f * (targetSpeedDegPerSec / rampUpSec) * elapsedSec * elapsedSec
                    } else {
                        val rampDistance = 0.5f * targetSpeedDegPerSec * rampUpSec
                        rampDistance + targetSpeedDegPerSec * (elapsedSec - rampUpSec)
                    }
                }
                clockMinuteSpin = computeClockRotation(targetSpeedDegPerSec = 26f, rampUpSec = 1.0f)
                clockHourSpin = computeClockRotation(targetSpeedDegPerSec = 4.5f, rampUpSec = 1.2f)
            }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Dimensions of Dresser Cabinet (centered horizontally, sturdy and nicely proportioned chest of drawers)
        val dresserW = w * 0.46f
        val dresserH = h * 0.62f
        val dresserLeft = (w - dresserW) / 2f
        val dresserTop = h * 0.23f
        val dresserRight = dresserLeft + dresserW
        val dresserBottom = dresserTop + dresserH

        // 1. Dresser Body with bottom cutout arch (legs)
        val cornerRadius = 14.dp.toPx()
        val archCutoutW = dresserW * 0.52f
        val archCutoutH = dresserH * 0.13f
        val archLeft = dresserLeft + (dresserW - archCutoutW) / 2f
        val archRight = archLeft + archCutoutW

        val dresserPath = Path().apply {
            moveTo(dresserLeft + cornerRadius, dresserTop)
            lineTo(dresserRight - cornerRadius, dresserTop)
            quadraticTo(dresserRight, dresserTop, dresserRight, dresserTop + cornerRadius)
            lineTo(dresserRight, dresserBottom - 4.dp.toPx())
            quadraticTo(dresserRight, dresserBottom, dresserRight - 4.dp.toPx(), dresserBottom)
            lineTo(archRight, dresserBottom)
            quadraticTo(
                archLeft + archCutoutW / 2f, dresserBottom - archCutoutH * 1.5f,
                archLeft, dresserBottom
            )
            lineTo(dresserLeft + 4.dp.toPx(), dresserBottom)
            quadraticTo(dresserLeft, dresserBottom, dresserLeft, dresserBottom - 4.dp.toPx())
            lineTo(dresserLeft, dresserTop + cornerRadius)
            quadraticTo(dresserLeft, dresserTop, dresserLeft + cornerRadius, dresserTop)
            close()
        }

        drawPath(
            path = dresserPath,
            color = colorDresser
        )

        // 2. Horizontal Drawer Dividers (2 dividers creating 3 drawer segments)
        val usableDresserH = dresserH - archCutoutH
        val drawerHeight = usableDresserH / 3f
        val dividerStroke = 2.6.dp.toPx()

        for (i in 1..2) {
            val dividerY = dresserTop + drawerHeight * i
            drawLine(
                color = colorDivider,
                start = Offset(dresserLeft + 4.dp.toPx(), dividerY),
                end = Offset(dresserRight - 4.dp.toPx(), dividerY),
                strokeWidth = dividerStroke,
                cap = StrokeCap.Round
            )
        }

        // 3. Centered Drawer Knobs (exact circular dot knobs matching reference)
        val knobRadius = 4.2.dp.toPx()
        val knobCenterX = dresserLeft + dresserW / 2f

        for (i in 0..2) {
            val knobCenterY = dresserTop + drawerHeight * i + drawerHeight / 2f
            drawCircle(
                color = colorKnob,
                radius = knobRadius,
                center = Offset(knobCenterX, knobCenterY)
            )
        }

        // 4. Icon 1: Compact Blue Stopwatch Clock Sitting Directly On Top of Cabinet (Starting at 10:10, slowly spinning)
        val clockRadius = dresserW * 0.130f
        val clockCenter = Offset(
            x = dresserLeft + dresserW * 0.50f,
            y = dresserTop - clockRadius // Exactly sits on top of the cabinet surface without overlapping or gaps
        )
        drawExactStopwatchClock(
            center = clockCenter,
            radius = clockRadius,
            needleRotationDegrees = clockHourSpin,
            minuteHandRotationDegrees = clockMinuteSpin,
            isRedStopped = false
        )

        // 5. Surrounding Shape Icons: Outer cloud shapes spin slowly while inner icons remain stationary & fixed

        // Icon 2 / Shape 1: Top-Left Puffy Cloud with Key (Overlapping Drawer 1)
        val badge1Radius = w * 0.096f
        val badge1Center = Offset(
            x = dresserLeft - badge1Radius * 0.28f,
            y = dresserTop + drawerHeight * 0.50f
        )
        // Outer 4-corner puffy cloud spins smoothly
        withTransform({
            translate(badge1Center.x, badge1Center.y)
            rotate(badge1Angle, pivot = Offset.Zero)
        }) {
            val path1 = createKeyBadgePath(badge1Radius)
            drawPath(path1, color = colorBadgeCloud)
        }
        // Inner Key icon remains stationary and fixed in normal horizontal upright position
        withTransform({
            translate(badge1Center.x, badge1Center.y)
            val iconScale = badge1Radius * 0.052f
            scale(iconScale, iconScale, Offset.Zero)
        }) {
            // Key bow (circular ring)
            drawCircle(
                color = colorBadgeIcon,
                radius = 4.8f,
                center = Offset(-5.2f, 0f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.4f)
            )
            // Key shaft
            drawLine(
                color = colorBadgeIcon,
                start = Offset(-0.4f, 0f),
                end = Offset(8.8f, 0f),
                strokeWidth = 2.4f,
                cap = StrokeCap.Round
            )
            // Teeth
            drawLine(
                color = colorBadgeIcon,
                start = Offset(4.8f, 0f),
                end = Offset(4.8f, 4.0f),
                strokeWidth = 2.2f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = colorBadgeIcon,
                start = Offset(7.8f, 0f),
                end = Offset(7.8f, 3.4f),
                strokeWidth = 2.2f,
                cap = StrokeCap.Round
            )
        }

        // Icon 3 / Shape 2: Bottom-Left Scalloped Cloud with Folder (Overlapping Drawer 3)
        val badge2RadiusMax = w * 0.095f
        val badge2RadiusMin = w * 0.078f
        val badge2Center = Offset(
            x = dresserLeft - badge2RadiusMax * 0.28f,
            y = dresserTop + drawerHeight * 2.45f
        )
        // Outer 8-lobed scalloped cloud spins smoothly
        withTransform({
            translate(badge2Center.x, badge2Center.y)
            rotate(badge2Angle, pivot = Offset.Zero)
        }) {
            val path2 = createScallopPath(badge2RadiusMax, badge2RadiusMin, 8, phase = 0f)
            drawPath(path2, color = colorBadgeCloud)
        }
        // Inner Folder icon remains stationary and fixed in normal horizontal upright position
        withTransform({
            translate(badge2Center.x, badge2Center.y)
            val iconScale = badge2RadiusMin * 0.072f
            scale(iconScale, iconScale, Offset.Zero)
        }) {
            val folderPath = Path().apply {
                moveTo(-7f, -4.5f)
                lineTo(-3f, -4.5f)
                lineTo(-1.5f, -2.5f)
                lineTo(7f, -2.5f)
                quadraticTo(8f, -2.5f, 8f, -1.5f)
                lineTo(8f, 4.8f)
                quadraticTo(8f, 5.8f, 7f, 5.8f)
                lineTo(-7f, 5.8f)
                quadraticTo(-8f, 5.8f, -8f, 4.8f)
                lineTo(-8f, -3.5f)
                quadraticTo(-8f, -4.5f, -7f, -4.5f)
                close()
            }
            drawPath(folderPath, color = colorBadgeIcon)
        }

        // Icon 4 / Shape 3: Middle-Right Scalloped Cloud with Crossed Tools (Overlapping Drawer 2)
        val badge3RadiusMax = w * 0.095f
        val badge3RadiusMin = w * 0.078f
        val badge3Center = Offset(
            x = dresserRight + badge3RadiusMax * 0.28f,
            y = dresserTop + drawerHeight * 1.50f
        )
        // Outer 8-lobed scalloped cloud spins smoothly
        withTransform({
            translate(badge3Center.x, badge3Center.y)
            rotate(badge3Angle, pivot = Offset.Zero)
        }) {
            val path3 = createScallopPath(badge3RadiusMax, badge3RadiusMin, 8, phase = (PI / 8f).toFloat())
            drawPath(path3, color = colorBadgeCloud)
        }
        // Inner Crossed Tools icon remains stationary and fixed in its normal position
        withTransform({
            translate(badge3Center.x, badge3Center.y)
            val iconScale = badge3RadiusMin * 0.070f
            scale(iconScale, iconScale, Offset.Zero)
        }) {
            // 1. Wrench (angled at 45 degrees, from bottom-left to top-right)
            withTransform({
                rotate(45f, pivot = Offset.Zero)
            }) {
                // Wrench shaft
                drawLine(
                    color = colorBadgeIcon,
                    start = Offset(0f, -2.2f),
                    end = Offset(0f, 8.5f),
                    strokeWidth = 2.4f,
                    cap = StrokeCap.Round
                )
                // Open-ended Wrench Head at top
                val wrenchHeadPath = Path().apply {
                    moveTo(-3.2f, -3.8f)
                    lineTo(-3.2f, -7.0f)
                    quadraticTo(-3.2f, -8.2f, -2.0f, -8.2f)
                    lineTo(-1.3f, -8.2f)
                    lineTo(-1.3f, -5.6f)
                    quadraticTo(0f, -4.8f, 1.3f, -5.6f)
                    lineTo(1.3f, -8.2f)
                    lineTo(2.0f, -8.2f)
                    quadraticTo(3.2f, -8.2f, 3.2f, -7.0f)
                    lineTo(3.2f, -3.8f)
                    close()
                }
                drawPath(wrenchHeadPath, color = colorBadgeIcon)
            }

            // 2. Hammer (angled at -45 degrees, from bottom-right to top-left)
            withTransform({
                rotate(-45f, pivot = Offset.Zero)
            }) {
                // Hammer handle
                drawLine(
                    color = colorBadgeIcon,
                    start = Offset(0f, -2.2f),
                    end = Offset(0f, 8.5f),
                    strokeWidth = 2.4f,
                    cap = StrokeCap.Round
                )
                // Hammer Head (Flat striking face on right, curved claw on left)
                val hammerHeadPath = Path().apply {
                    moveTo(4.6f, -4.5f)
                    lineTo(4.6f, -7.5f)
                    lineTo(1.2f, -7.5f)
                    quadraticTo(-2.5f, -8.0f, -5.2f, -5.0f)
                    quadraticTo(-2.8f, -5.8f, -1.2f, -4.5f)
                    lineTo(4.6f, -4.5f)
                    close()
                }
                drawPath(hammerHeadPath, color = colorBadgeIcon)
            }
        }
    }
}

/**
 * Standalone Composable for the exact Stopwatch / Timer Icon matching the reference image.
 */
@Composable
fun ExactStopwatchClockIcon(
    modifier: Modifier = Modifier.size(36.dp),
    animateHands: Boolean = false,
    isRedStopped: Boolean = false
) {
    val clockTransition = rememberInfiniteTransition(label = "exact_clock_needle_spin")
    val hourRotDeg by clockTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "standalone_hourRotDeg"
    )

    val minuteRotDeg by clockTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "standalone_minuteRotDeg"
    )

    Canvas(modifier = modifier) {
        val minDim = minOf(size.width, size.height)
        val radius = minDim * 0.38f
        val center = Offset(size.width / 2f, size.height / 2f + minDim * 0.04f)
        drawExactStopwatchClock(
            center = center,
            radius = radius,
            needleRotationDegrees = if (animateHands && !isRedStopped) hourRotDeg else 0f,
            minuteHandRotationDegrees = if (animateHands && !isRedStopped) minuteRotDeg else 0f,
            isRedStopped = isRedStopped
        )
    }
}
