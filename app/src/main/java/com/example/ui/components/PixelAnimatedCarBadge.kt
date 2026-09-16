package com.example.ui.components

import android.graphics.Matrix
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Custom Material 3 Expressive Scalloped Animated Badge matching Google Pixel styling.
 *
 * - Main Shape: 10-lobed smooth scalloped flower badge rotating very slowly 1 turn clockwise, then smoothly reversing 1 turn counter-clockwise.
 * - Center Icon: Car / Location vehicle icon cleanly centered.
 * - Top-Left: Small stationary circular satellite dot.
 * - Bottom-Right: 4-lobed squircle flower rotating in the opposite direction at 1.5x speed.
 * - 6 Companion Elements with matching structural shapes:
 *   1. Top-Left (Squircle): RED Mitsubishi Lancer Evolution VIII (Evo 8) - front bumper focused
 *   2. Mid-Left: 8-Point Star
 *   3. Bottom-Left (8-Lobed Scallop): Lamborghini supercar
 *   4. Top-Right (Organic Pebble): Black Range Rover luxury SUV
 *   5. Right: Circular Satellite Dot
 *   6. Bottom-Right (4-Petal Clover): BLUE Mitsubishi Lancer Evolution X (Evo X) - closed hood & front grille focused
 *
 * Supports two rendering styles using the exact same iconic shapes:
 * - MATERIAL: Clean, flat illustrated vector line-art matching Material 3 expressive language.
 * - CINEMATIC: Authentic, clean, high-resolution photographic car cards showing the full front bumper and body clearly inside the respective badge shapes.
 */
@Composable
fun PixelAnimatedCarBadge(
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
    style: CarBadgeStyle = CarBadgeStyle.CINEMATIC,
    badgeColor: Color = Color(0xFFB8D19F),
    satelliteColor: Color = Color(0xFFB4C8A2),
    dotColor: Color = Color(0xFFFAF2DC),
    iconColor: Color = Color(0xFF283B1D)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scallopBadgeAnimation")

    // Realistic photographic image assets for Cinematic mode
    val evo8Bitmap = if (style == CarBadgeStyle.CINEMATIC) ImageBitmap.imageResource(id = R.drawable.img_car_evo8) else null
    val rangeRoverBitmap = if (style == CarBadgeStyle.CINEMATIC) ImageBitmap.imageResource(id = R.drawable.img_car_rangerover) else null
    val lamboBitmap = if (style == CarBadgeStyle.CINEMATIC) ImageBitmap.imageResource(id = R.drawable.img_car_lamborghini) else null
    val evoXBitmap = if (style == CarBadgeStyle.CINEMATIC) ImageBitmap.imageResource(id = R.drawable.img_car_evox) else null

    // Ultra smooth 1-turn (0 -> 360 deg) clockwise, then 1-turn (360 -> 0 deg) counter-clockwise
    val mainRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 14000,
                easing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mainScallopRotation"
    )

    val satelliteRotation = -mainRotation * 1.5f

    // Very very very slow rotation for car shapes (30 seconds per half cycle)
    // Top-Right & Bottom-Left: 1 turn clockwise, then 1 turn counter-clockwise
    val trBlShapeRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 30000,
                easing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trBlShapeRotation"
    )

    // Bottom-Right & Top-Left: 1 turn counter-clockwise, then 1 turn clockwise
    val brTlShapeRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 30000,
                easing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brTlShapeRotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val h = this.size.height
            val w = this.size.width

            val mainRadiusMax = h * 0.31f
            val mainRadiusMin = h * 0.24f

            // =========================================================================
            // 6 COMPANION ICONS WITH IDENTICAL SHAPES ACROSS MATERIAL & CINEMATIC MODES
            // =========================================================================

            // 1. Top-Left Squircle Badge: RED Mitsubishi Lancer Evolution VIII (Evo 8)
            // Rotates 1 turn counter-clockwise, then 1 turn clockwise (car inside stays still)
            val tlCenter = Offset(center.x - w * 0.325f, center.y - h * 0.31f)
            drawEvo8Badge(
                center = tlCenter,
                width = h * 0.35f,
                height = h * 0.35f,
                style = style,
                photoBitmap = evo8Bitmap,
                rotation = brTlShapeRotation
            )

            // 2. Bottom-Left 8-Lobed Wavy Scallop Badge: Lamborghini (Low Wedge Supercar)
            // Rotates 1 turn clockwise, then 1 turn counter-clockwise (car inside stays still)
            val blCenter = Offset(center.x - w * 0.27f, center.y + h * 0.32f)
            drawLamborghiniBadge(
                center = blCenter,
                radius = h * 0.185f,
                style = style,
                photoBitmap = lamboBitmap,
                rotation = trBlShapeRotation
            )

            // 3. Far-Left Rounded Star Polygon (15.dp corner radius on each point)
            // Rotates in opposite direction as bottom-left car shape at 1.3x speed (0.3x faster)
            val starCenter = Offset(center.x - w * 0.465f, center.y + h * 0.02f)
            drawFarLeftRoundedStar(
                center = starCenter,
                outerRadius = h * 0.088f,
                innerRadius = h * 0.050f,
                cornerRadiusPx = 15.dp.toPx(),
                numPoints = 8,
                rotation = -trBlShapeRotation * 1.3f
            )

            // 4. Top-Right Organic Pebble Badge: Black Range Rover (Upright Luxury SUV)
            // Rotates 1 turn clockwise, then 1 turn counter-clockwise (car inside stays still)
            val trCenter = Offset(center.x + w * 0.325f, center.y - h * 0.31f)
            drawRangeRoverBadge(
                center = trCenter,
                width = h * 0.35f,
                height = h * 0.35f,
                style = style,
                photoBitmap = rangeRoverBitmap,
                rotation = trBlShapeRotation
            )

            // 5. Bottom-Right 4-Petal Clover Badge: BLUE Mitsubishi Lancer Evolution X (Evo X)
            // Rotates 1 turn counter-clockwise, then 1 turn clockwise (car inside stays still)
            val brCenter = Offset(center.x + w * 0.325f, center.y + h * 0.32f)
            drawEvoXBadge(
                center = brCenter,
                radius = h * 0.185f,
                style = style,
                photoBitmap = evoXBitmap,
                rotation = brTlShapeRotation
            )

            // 6. Far-Right Satellite Dot
            val rightDotCenter = Offset(center.x + w * 0.465f, center.y - h * 0.02f)
            drawRightDot(
                center = rightDotCenter,
                radius = h * 0.042f
            )

            // =========================================================================
            // CENTRAL 3 ELEMENTS & ANIMATIONS
            // =========================================================================

            // 1. Top-Left Stationary Small Dot
            val dotCenter = Offset(center.x - mainRadiusMax * 0.90f, center.y - mainRadiusMax * 0.85f)
            val dotRadius = h * 0.038f
            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = dotCenter
            )

            // 2. Bottom-Right 4-Lobed Flower (rotates in opposite direction at 1.5x speed)
            // Positioned so the outer lobes are about to touch the main scallop at peak rotation but never collide
            val satCenter = Offset(center.x + mainRadiusMax * 0.96f, center.y + mainRadiusMax * 0.92f)
            val satRadiusMax = h * 0.090f
            val satRadiusMin = h * 0.065f
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
            modifier = Modifier.size(size * 0.28f)
        )
    }
}

// =============================================================================
// 1. TOP-LEFT: SQUIRCLE SHAPE - EVO 8 (MATERIAL) & PORSCHE 911 (CINEMATIC)
// =============================================================================
private fun DrawScope.drawEvo8Badge(
    center: Offset,
    width: Float,
    height: Float,
    style: CarBadgeStyle,
    photoBitmap: ImageBitmap?,
    rotation: Float = 0f
) {
    val left = center.x - width / 2f
    val top = center.y - height / 2f
    val cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())

    val baseSquircle = Path().apply {
        addRoundRect(
            RoundRect(
                left = left,
                top = top,
                right = left + width,
                bottom = top + height,
                cornerRadius = cornerRadius
            )
        )
    }
    val squirclePath = baseSquircle.rotated(rotation, center)

    if (style == CarBadgeStyle.CINEMATIC) {
        clipPath(squirclePath) {
            drawPorsche911Cinematic(center, width, height, left, top, squirclePath)
        }
    } else {
        clipPath(squirclePath) {
            drawEvo8Material(center, width, height, left, top, squirclePath)
        }
    }
}

/**
 * Cinematic Illustrated Porsche 911 in dynamic 3/4 action angle with speed lines.
 */
private fun DrawScope.drawPorsche911Cinematic(
    center: Offset,
    width: Float,
    height: Float,
    left: Float,
    top: Float,
    squirclePath: Path
) {
    // Container background: soft sage green (rotates with shape)
    drawPath(path = squirclePath, color = Color(0xFFB4C8A2))

    val cx = center.x
    val cy = center.y + height * 0.04f
    val w = width * 0.86f
    val h = height * 0.42f

    val wheelY = cy + h * 0.28f
    val frontWheelX = cx - w * 0.26f
    val rearWheelX = cx + w * 0.24f
    val wheelRadius = h * 0.22f

    // Road firmly touching bottom of tires
    val roadY = wheelY + wheelRadius
    drawRoundRect(
        color = Color(0xFFA2B88F),
        topLeft = Offset(left + width * 0.06f, roadY),
        size = Size(width * 0.88f, height * 0.08f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Dynamic Speed Lines / Motion Streaks cutting through
    drawLine(
        color = Color(0xFFFAF4EB).copy(alpha = 0.85f),
        start = Offset(cx - w * 0.52f, cy - h * 0.58f),
        end = Offset(cx + w * 0.48f, cy - h * 0.58f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFFAF4EB).copy(alpha = 0.50f),
        start = Offset(cx - w * 0.48f, cy - h * 0.35f),
        end = Offset(cx - w * 0.18f, cy - h * 0.35f),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF283B1D).copy(alpha = 0.40f),
        start = Offset(cx + w * 0.15f, cy + h * 0.42f),
        end = Offset(cx + w * 0.48f, cy + h * 0.42f),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round
    )

    // Porsche 911 Sleek Low Rear Spoiler in 3/4 angle
    val wingPath = Path().apply {
        moveTo(cx + w * 0.28f, cy - h * 0.16f)
        lineTo(cx + w * 0.32f, cy - h * 0.34f)
        cubicTo(cx + w * 0.36f, cy - h * 0.38f, cx + w * 0.46f, cy - h * 0.36f, cx + w * 0.48f, cy - h * 0.26f)
        lineTo(cx + w * 0.44f, cy - h * 0.14f)
        close()
    }
    drawPath(wingPath, color = Color(0xFF283B1D))

    // Porsche 911 Aerodynamic Curved Body in 3/4 Angle
    val porscheBody = Path().apply {
        moveTo(cx - w * 0.52f, cy + h * 0.28f)
        lineTo(cx - w * 0.52f, cy + h * 0.10f)
        cubicTo(cx - w * 0.48f, cy + h * 0.02f, cx - w * 0.38f, cy - h * 0.14f, cx - w * 0.20f, cy - h * 0.22f)
        cubicTo(cx - w * 0.10f, cy - h * 0.48f, cx + w * 0.06f, cy - h * 0.56f, cx + w * 0.22f, cy - h * 0.42f)
        cubicTo(cx + w * 0.34f, cy - h * 0.30f, cx + w * 0.44f, cy - h * 0.12f, cx + w * 0.48f, cy + h * 0.08f)
        lineTo(cx + w * 0.48f, cy + h * 0.28f)
        lineTo(cx + w * 0.35f, cy + h * 0.28f)
        cubicTo(cx + w * 0.33f, cy + h * 0.06f, cx + w * 0.15f, cy + h * 0.06f, cx + w * 0.13f, cy + h * 0.28f)
        lineTo(cx - w * 0.15f, cy + h * 0.28f)
        cubicTo(cx - w * 0.17f, cy + h * 0.06f, cx - w * 0.35f, cy + h * 0.06f, cx - w * 0.37f, cy + h * 0.28f)
        close()
    }
    drawPath(porscheBody, color = Color(0xFF4C5E35))

    // Teardrop Porsche Windshield & Side Glass in warm ivory
    val glass = Path().apply {
        moveTo(cx - w * 0.16f, cy - h * 0.18f)
        cubicTo(cx - w * 0.08f, cy - h * 0.44f, cx + w * 0.04f, cy - h * 0.48f, cx + w * 0.18f, cy - h * 0.36f)
        cubicTo(cx + w * 0.24f, cy - h * 0.28f, cx + w * 0.28f, cy - h * 0.14f, cx + w * 0.30f, cy - h * 0.08f)
        lineTo(cx - w * 0.16f, cy - h * 0.08f)
        close()
    }
    drawPath(glass, color = Color(0xFFF7F4EB))
    drawLine(color = Color(0xFF4C5E35), start = Offset(cx + w * 0.06f, cy - h * 0.46f), end = Offset(cx + w * 0.06f, cy - h * 0.08f), strokeWidth = 2.5f)

    // Oval Porsche Headlight in 3/4 perspective
    val headlight = Path().apply {
        moveTo(cx - w * 0.46f, cy - h * 0.02f)
        cubicTo(cx - w * 0.44f, cy - h * 0.12f, cx - w * 0.36f, cy - h * 0.16f, cx - w * 0.32f, cy - h * 0.08f)
        cubicTo(cx - w * 0.30f, cy - h * 0.02f, cx - w * 0.36f, cy + h * 0.04f, cx - w * 0.44f, cy + h * 0.04f)
        close()
    }
    drawPath(headlight, color = Color(0xFFF7F4EB))

    // Front Bumper Intake Dam
    drawRoundRect(
        color = Color(0xFF283B1D),
        topLeft = Offset(cx - w * 0.50f, cy + h * 0.12f),
        size = Size(w * 0.14f, h * 0.14f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Wheels in 3/4 perspective with tire touching road
    drawCircle(color = Color(0xFF283B1D), radius = wheelRadius, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFF283B1D), radius = wheelRadius * 0.92f, center = Offset(rearWheelX, wheelY))
    drawCircle(color = Color(0xFFD4E3C7), radius = wheelRadius * 0.54f, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFFD4E3C7), radius = wheelRadius * 0.50f, center = Offset(rearWheelX, wheelY))
    drawCircle(color = Color(0xFF283B1D), radius = wheelRadius * 0.20f, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFF283B1D), radius = wheelRadius * 0.18f, center = Offset(rearWheelX, wheelY))
}

/**
 * Material Illustrated Evo 8 (Muted, minimalist flat vector art matching Google Pixel face style).
 */
private fun DrawScope.drawEvo8Material(
    center: Offset,
    width: Float,
    height: Float,
    left: Float,
    top: Float,
    squirclePath: Path
) {
    // Container background: soft sage green matching top-left squircle in reference image (rotates with shape)
    drawPath(path = squirclePath, color = Color(0xFFB4C8A2))

    val cx = center.x
    val cy = center.y + height * 0.04f
    val w = width * 0.86f
    val h = height * 0.42f

    val wheelY = cy + h * 0.28f
    val frontWheelX = cx - w * 0.23f
    val rearWheelX = cx + w * 0.23f
    val wheelRadius = h * 0.23f

    // Road firmly touching bottom of tires
    val roadY = wheelY + wheelRadius
    drawRoundRect(
        color = Color(0xFFA2B88F),
        topLeft = Offset(left + width * 0.06f, roadY),
        size = Size(width * 0.88f, height * 0.08f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Sleek sport rear spoiler in dark forest olive
    val wingPath = Path().apply {
        moveTo(cx + w * 0.32f, cy - h * 0.10f)
        lineTo(cx + w * 0.35f, cy - h * 0.32f)
        cubicTo(cx + w * 0.38f, cy - h * 0.36f, cx + w * 0.46f, cy - h * 0.36f, cx + w * 0.48f, cy - h * 0.28f)
        lineTo(cx + w * 0.45f, cy - h * 0.08f)
        close()
    }
    drawPath(wingPath, color = Color(0xFF283B1D))

    // Car Body in medium olive moss (matching shirt/shoulders in reference)
    val carBody = Path().apply {
        moveTo(cx - w * 0.50f, cy + h * 0.28f)
        lineTo(cx - w * 0.50f, cy + h * 0.12f)
        lineTo(cx - w * 0.48f, cy - h * 0.02f)
        cubicTo(cx - w * 0.42f, cy - h * 0.08f, cx - w * 0.30f, cy - h * 0.18f, cx - w * 0.18f, cy - h * 0.24f)
        lineTo(cx - w * 0.06f, cy - h * 0.54f)
        cubicTo(cx + w * 0.04f, cy - h * 0.56f, cx + w * 0.16f, cy - h * 0.54f, cx + w * 0.24f, cy - h * 0.40f)
        lineTo(cx + w * 0.46f, cy - h * 0.06f)
        lineTo(cx + w * 0.46f, cy + h * 0.28f)
        lineTo(cx + w * 0.34f, cy + h * 0.28f)
        cubicTo(cx + w * 0.32f, cy + h * 0.06f, cx + w * 0.14f, cy + h * 0.06f, cx + w * 0.12f, cy + h * 0.28f)
        lineTo(cx - w * 0.12f, cy + h * 0.28f)
        cubicTo(cx - w * 0.14f, cy + h * 0.06f, cx - w * 0.32f, cy + h * 0.06f, cx - w * 0.34f, cy + h * 0.28f)
        close()
    }
    drawPath(carBody, color = Color(0xFF4C5E35))

    // Windows/Greenhouse in warm ivory (matching hair tone in reference)
    val windows = Path().apply {
        moveTo(cx - w * 0.14f, cy - h * 0.20f)
        lineTo(cx - w * 0.04f, cy - h * 0.46f)
        cubicTo(cx + w * 0.08f, cy - h * 0.48f, cx + w * 0.15f, cy - h * 0.46f, cx + w * 0.22f, cy - h * 0.34f)
        lineTo(cx + w * 0.26f, cy - h * 0.10f)
        lineTo(cx - w * 0.14f, cy - h * 0.10f)
        close()
    }
    drawPath(windows, color = Color(0xFFF7F4EB))
    drawLine(color = Color(0xFF4C5E35), start = Offset(cx + w * 0.07f, cy - h * 0.47f), end = Offset(cx + w * 0.07f, cy - h * 0.10f), strokeWidth = 2.5f)

    // Bumper intake in dark forest olive
    val intercoolerMouth = Path().apply {
        moveTo(cx - w * 0.49f, cy + h * 0.10f)
        lineTo(cx - w * 0.37f, cy + h * 0.10f)
        lineTo(cx - w * 0.37f, cy + h * 0.24f)
        lineTo(cx - w * 0.49f, cy + h * 0.24f)
        close()
    }
    drawPath(intercoolerMouth, color = Color(0xFF283B1D))
    drawRoundRect(color = Color(0xFFD4E3C7), topLeft = Offset(cx - w * 0.46f, cy + h * 0.13f), size = Size(w * 0.07f, h * 0.08f), cornerRadius = CornerRadius(1.5f, 1.5f))

    // Minimal headlight in warm ivory
    val headlightPath = Path().apply {
        moveTo(cx - w * 0.46f, cy - h * 0.05f)
        lineTo(cx - w * 0.36f, cy - h * 0.14f)
        lineTo(cx - w * 0.32f, cy - h * 0.06f)
        lineTo(cx - w * 0.43f, cy + h * 0.02f)
        close()
    }
    drawPath(headlightPath, color = Color(0xFFF7F4EB))

    // Wheels in dark olive & pale sage hub with tires touching the road
    drawCircle(color = Color(0xFF283B1D), radius = wheelRadius, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFF283B1D), radius = wheelRadius, center = Offset(rearWheelX, wheelY))
    drawCircle(color = Color(0xFFD4E3C7), radius = wheelRadius * 0.54f, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFFD4E3C7), radius = wheelRadius * 0.54f, center = Offset(rearWheelX, wheelY))
    drawCircle(color = Color(0xFF283B1D), radius = wheelRadius * 0.20f, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFF283B1D), radius = wheelRadius * 0.20f, center = Offset(rearWheelX, wheelY))
}

// =============================================================================
// 2. FAR-LEFT ROUNDED STAR POLYGON (15.dp Corner Radius on each point)
// =============================================================================
private fun DrawScope.drawFarLeftRoundedStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    cornerRadiusPx: Float = 15.dp.toPx(),
    numPoints: Int = 8,
    color: Color = Color(0xFFB4C8A2),
    rotation: Float = 0f
) {
    val path = Path()
    val totalVertices = numPoints * 2
    val step = (2f * PI.toFloat()) / totalVertices
    val startAngle = -PI.toFloat() / 2f

    // Calculate all polygon vertices (outer tips and inner valleys)
    val vertices = (0 until totalVertices).map { idx ->
        val angle = startAngle + idx * step
        val r = if (idx % 2 == 0) outerRadius else innerRadius
        Offset(center.x + r * cos(angle), center.y + r * sin(angle))
    }

    // Connect with exact corner radius on each outer point tip
    for (i in 0 until totalVertices) {
        val curr = vertices[i]
        val prev = vertices[(i - 1 + totalVertices) % totalVertices]
        val next = vertices[(i + 1) % totalVertices]

        if (i % 2 == 0) {
            // Outer star point tip: compute tangent points using 15.dp corner radius
            val v1 = prev - curr
            val v2 = next - curr
            val len1 = sqrt(v1.x * v1.x + v1.y * v1.y)
            val len2 = sqrt(v2.x * v2.x + v2.y * v2.y)
            val u1 = Offset(v1.x / len1, v1.y / len1)
            val u2 = Offset(v2.x / len2, v2.y / len2)

            val dot = (u1.x * u2.x + u1.y * u2.y).coerceIn(-1f, 1f)
            val halfAngle = acos(dot) / 2f
            val maxT = min(len1, len2) * 0.48f
            val t = if (sin(halfAngle) > 0.001f) {
                min(cornerRadiusPx / tan(halfAngle), maxT)
            } else {
                maxT
            }

            val p1 = curr + u1 * t
            val p2 = curr + u2 * t

            if (i == 0) {
                path.moveTo(p1.x, p1.y)
            } else {
                path.lineTo(p1.x, p1.y)
            }
            // Quadratic Bézier curve through apex point curr
            path.quadraticTo(curr.x, curr.y, p2.x, p2.y)
        } else {
            // Inner valley
            path.lineTo(curr.x, curr.y)
        }
    }
    path.close()
    rotate(degrees = rotation, pivot = center) {
        drawPath(path, color = color)
    }
}

// =============================================================================
// 3. BOTTOM-LEFT: 8-LOBED SCALLOP - LAMBORGHINI (MATERIAL) & FERRARI F40 (CINEMATIC)
// =============================================================================
private fun DrawScope.drawLamborghiniBadge(
    center: Offset,
    radius: Float,
    style: CarBadgeStyle,
    photoBitmap: ImageBitmap?,
    rotation: Float = 0f
) {
    val baseContainerPath = createScallopPath(
        center = center,
        numLobes = 8,
        radiusMax = radius,
        radiusMin = radius * 0.82f
    )
    val containerPath = baseContainerPath.rotated(rotation, center)

    if (style == CarBadgeStyle.CINEMATIC) {
        clipPath(containerPath) {
            drawFerrariF40Cinematic(center, radius, containerPath)
        }
    } else {
        clipPath(containerPath) {
            drawLamborghiniMaterial(center, radius, containerPath)
        }
    }
}

/**
 * Cinematic Illustrated Ferrari F40 Supercar in dynamic low-slung 3/4 action view with speed lines.
 */
private fun DrawScope.drawFerrariF40Cinematic(
    center: Offset,
    radius: Float,
    containerPath: Path
) {
    // Container background: pale sage green/grey matching bottom-left scallop
    drawPath(containerPath, color = Color(0xFFBAC5B0))

    val cx = center.x
    val cy = center.y + radius * 0.08f
    val w = radius * 1.48f
    val h = radius * 0.61f

    val wheelY = cy + h * 0.26f
    val frontWheelX = cx - w * 0.25f
    val rearWheelX = cx + w * 0.25f
    val frontWheelR = h * 0.21f
    val rearWheelR = h * 0.24f

    // Road firmly touching bottom of tires
    val roadY = wheelY + rearWheelR
    drawRoundRect(
        color = Color(0xFFA9B59F),
        topLeft = Offset(center.x - radius * 0.80f, roadY),
        size = Size(radius * 1.60f, radius * 0.15f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Dynamic Rushing Speed Lines
    drawLine(
        color = Color(0xFFFAF3DE).copy(alpha = 0.90f),
        start = Offset(cx - w * 0.52f, cy - h * 0.60f),
        end = Offset(cx + w * 0.48f, cy - h * 0.60f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFFAF3DE).copy(alpha = 0.60f),
        start = Offset(cx - w * 0.48f, cy - h * 0.38f),
        end = Offset(cx - w * 0.12f, cy - h * 0.38f),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF222F17).copy(alpha = 0.40f),
        start = Offset(cx + w * 0.10f, cy + h * 0.44f),
        end = Offset(cx + w * 0.50f, cy + h * 0.44f),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round
    )

    // Iconic F40 Box Rear Wing in 3/4 perspective
    val wingPath = Path().apply {
        moveTo(cx + w * 0.24f, cy - h * 0.10f)
        lineTo(cx + w * 0.26f, cy - h * 0.72f)
        lineTo(cx + w * 0.48f, cy - h * 0.72f)
        lineTo(cx + w * 0.46f, cy - h * 0.08f)
        close()
    }
    drawPath(wingPath, color = Color(0xFF222F17))

    // F40 Low Wedge Body in medium forest olive
    val f40Body = Path().apply {
        moveTo(cx - w * 0.54f, cy + h * 0.26f)
        lineTo(cx - w * 0.54f, cy + h * 0.14f)
        lineTo(cx - w * 0.50f, cy + h * 0.04f)
        cubicTo(cx - w * 0.42f, cy - h * 0.02f, cx - w * 0.28f, cy - h * 0.20f, cx - w * 0.14f, cy - h * 0.46f)
        lineTo(cx + w * 0.10f, cy - h * 0.46f)
        cubicTo(cx + w * 0.24f, cy - h * 0.30f, cx + w * 0.40f, cy - h * 0.10f, cx + w * 0.52f, cy + h * 0.02f)
        lineTo(cx + w * 0.53f, cy + h * 0.26f)
        lineTo(cx + w * 0.36f, cy + h * 0.26f)
        cubicTo(cx + w * 0.34f, cy + h * 0.04f, cx + w * 0.16f, cy + h * 0.04f, cx + w * 0.14f, cy + h * 0.26f)
        lineTo(cx - w * 0.14f, cy + h * 0.26f)
        cubicTo(cx - w * 0.16f, cy + h * 0.04f, cx - w * 0.34f, cy + h * 0.04f, cx - w * 0.36f, cy + h * 0.26f)
        close()
    }
    drawPath(f40Body, color = Color(0xFF3B4F27))

    // Low Greenhouse & Windshield in warm ivory
    val glass = Path().apply {
        moveTo(cx - w * 0.20f, cy - h * 0.10f)
        lineTo(cx - w * 0.10f, cy - h * 0.38f)
        lineTo(cx + w * 0.06f, cy - h * 0.38f)
        lineTo(cx + w * 0.24f, cy - h * 0.10f)
        close()
    }
    drawPath(glass, color = Color(0xFFFAF3DE))

    // Twin NACA Ducts on Hood & Flank in dark forest olive
    val nacaHood = Path().apply {
        moveTo(cx - w * 0.38f, cy - h * 0.04f)
        lineTo(cx - w * 0.30f, cy - h * 0.12f)
        lineTo(cx - w * 0.26f, cy - h * 0.08f)
        close()
    }
    drawPath(nacaHood, color = Color(0xFF222F17))

    val nacaFlank = Path().apply {
        moveTo(cx + w * 0.08f, cy - h * 0.02f)
        lineTo(cx + w * 0.18f, cy - h * 0.02f)
        lineTo(cx + w * 0.14f, cy + h * 0.16f)
        close()
    }
    drawPath(nacaFlank, color = Color(0xFF222F17))

    // Front Low Splitter
    drawRoundRect(
        color = Color(0xFF222F17),
        topLeft = Offset(cx - w * 0.54f, cy + h * 0.18f),
        size = Size(w * 0.16f, h * 0.08f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Wide 3/4 Perspective Wheels with tires firmly on road
    drawCircle(color = Color(0xFF222F17), radius = frontWheelR, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFF222F17), radius = rearWheelR, center = Offset(rearWheelX, wheelY))
    drawCircle(color = Color(0xFFD5DEC8), radius = frontWheelR * 0.54f, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFFD5DEC8), radius = rearWheelR * 0.54f, center = Offset(rearWheelX, wheelY))
    drawCircle(color = Color(0xFF222F17), radius = frontWheelR * 0.20f, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFF222F17), radius = rearWheelR * 0.20f, center = Offset(rearWheelX, wheelY))
}

/**
 * Material Illustrated Lamborghini (Muted, minimalist flat vector art matching Google Pixel face style).
 */
private fun DrawScope.drawLamborghiniMaterial(
    center: Offset,
    radius: Float,
    containerPath: Path
) {
    // Container background: pale sage green/grey matching bottom-left scallop in reference image
    drawPath(containerPath, color = Color(0xFFBAC5B0))

    val cx = center.x
    val cy = center.y + radius * 0.08f
    val w = radius * 1.48f
    val h = radius * 0.61f

    val wheelY = cy + h * 0.26f
    val frontWheelX = cx - w * 0.23f
    val rearWheelX = cx + w * 0.25f
    val frontWheelR = h * 0.20f
    val rearWheelR = h * 0.24f

    // Road firmly touching bottom of tires
    val roadY = wheelY + rearWheelR
    drawRoundRect(
        color = Color(0xFFA9B59F),
        topLeft = Offset(center.x - radius * 0.80f, roadY),
        size = Size(radius * 1.60f, radius * 0.15f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Car Body in medium forest olive (matching face tone in reference)
    val lamboBody = Path().apply {
        moveTo(cx - w * 0.52f, cy + h * 0.28f)
        lineTo(cx - w * 0.53f, cy + h * 0.16f)
        lineTo(cx - w * 0.49f, cy + h * 0.06f)
        cubicTo(cx - w * 0.40f, cy - h * 0.04f, cx - w * 0.26f, cy - h * 0.22f, cx - w * 0.12f, cy - h * 0.48f)
        lineTo(cx + w * 0.08f, cy - h * 0.48f)
        cubicTo(cx + w * 0.24f, cy - h * 0.32f, cx + w * 0.40f, cy - h * 0.10f, cx + w * 0.52f, cy + h * 0.02f)
        lineTo(cx + w * 0.53f, cy + h * 0.28f)
        lineTo(cx + w * 0.36f, cy + h * 0.28f)
        cubicTo(cx + w * 0.34f, cy + h * 0.06f, cx + w * 0.16f, cy + h * 0.06f, cx + w * 0.14f, cy + h * 0.28f)
        lineTo(cx - w * 0.12f, cy + h * 0.28f)
        cubicTo(cx - w * 0.14f, cy + h * 0.06f, cx - w * 0.32f, cy + h * 0.06f, cx - w * 0.34f, cy + h * 0.28f)
        close()
    }
    drawPath(lamboBody, color = Color(0xFF3B4F27))

    // Cockpit / roof in warm ivory (matching shirt in reference)
    val cockpit = Path().apply {
        moveTo(cx - w * 0.20f, cy - h * 0.12f)
        lineTo(cx - w * 0.10f, cy - h * 0.40f)
        lineTo(cx + w * 0.06f, cy - h * 0.40f)
        lineTo(cx + w * 0.24f, cy - h * 0.12f)
        close()
    }
    drawPath(cockpit, color = Color(0xFFFAF3DE))

    // Front splitter / intake in dark forest olive (matching hair in reference)
    val frontAirDam = Path().apply {
        moveTo(cx - w * 0.51f, cy + h * 0.14f)
        lineTo(cx - w * 0.42f, cy + h * 0.12f)
        lineTo(cx - w * 0.40f, cy + h * 0.26f)
        lineTo(cx - w * 0.50f, cy + h * 0.26f)
        close()
    }
    drawPath(frontAirDam, color = Color(0xFF222F17))

    // Subtle signature headlight in warm ivory
    val yHeadlight = Path().apply {
        moveTo(cx - w * 0.32f, cy - h * 0.04f)
        lineTo(cx - w * 0.42f, cy + h * 0.02f)
        lineTo(cx - w * 0.48f, cy - h * 0.02f)
        moveTo(cx - w * 0.42f, cy + h * 0.02f)
        lineTo(cx - w * 0.47f, cy + h * 0.06f)
    }
    drawPath(yHeadlight, color = Color(0xFFFAF3DE), style = Stroke(width = 2.4f))

    // Side air intake scoop in dark forest olive
    val flankScoop = Path().apply {
        moveTo(cx + w * 0.08f, cy - h * 0.04f)
        lineTo(cx + w * 0.22f, cy - h * 0.04f)
        lineTo(cx + w * 0.18f, cy + h * 0.18f)
        lineTo(cx + w * 0.06f, cy + h * 0.18f)
        close()
    }
    drawPath(flankScoop, color = Color(0xFF222F17))

    // Muted 2-tone wheels with tires firmly touching road
    drawCircle(color = Color(0xFF222F17), radius = frontWheelR, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFF222F17), radius = rearWheelR, center = Offset(rearWheelX, wheelY))
    drawCircle(color = Color(0xFFD5DEC8), radius = frontWheelR * 0.52f, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFFD5DEC8), radius = rearWheelR * 0.52f, center = Offset(rearWheelX, wheelY))
    drawCircle(color = Color(0xFF222F17), radius = frontWheelR * 0.20f, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFF222F17), radius = rearWheelR * 0.20f, center = Offset(rearWheelX, wheelY))
}

// =============================================================================
// 4. TOP-RIGHT: ORGANIC PEBBLE SHAPE - RANGE ROVER (MATERIAL) & TOYOTA SUPRA MK4 (CINEMATIC)
// =============================================================================
private fun DrawScope.drawRangeRoverBadge(
    center: Offset,
    width: Float,
    height: Float,
    style: CarBadgeStyle,
    photoBitmap: ImageBitmap?,
    rotation: Float = 0f
) {
    val basePebblePath = Path().apply {
        val rx = width / 2f
        val ry = height / 2f
        moveTo(center.x, center.y - ry)
        cubicTo(center.x + rx * 1.05f, center.y - ry * 0.9f, center.x + rx * 1.1f, center.y + ry * 0.4f, center.x + rx * 0.6f, center.y + ry * 0.95f)
        cubicTo(center.x + rx * 0.1f, center.y + ry * 1.1f, center.x - rx * 0.8f, center.y + ry * 0.9f, center.x - rx * 0.95f, center.y + ry * 0.3f)
        cubicTo(center.x - rx * 1.1f, center.y - ry * 0.5f, center.x - rx * 0.5f, center.y - ry * 1.05f, center.x, center.y - ry)
        close()
    }
    val pebblePath = basePebblePath.rotated(rotation, center)

    if (style == CarBadgeStyle.CINEMATIC) {
        clipPath(pebblePath) {
            drawSupraMK4Cinematic(center, width, height, pebblePath)
        }
    } else {
        clipPath(pebblePath) {
            drawRangeRoverMaterial(center, width, height, pebblePath)
        }
    }
}

/**
 * Cinematic Illustrated Toyota Supra MK4 Turbo in dynamic speeding fastback 3/4 action view with speed lines.
 */
private fun DrawScope.drawSupraMK4Cinematic(
    center: Offset,
    width: Float,
    height: Float,
    pebblePath: Path
) {
    // Container background: deep forest olive matching top-right pebble
    drawPath(pebblePath, color = Color(0xFF3F542A))

    val cx = center.x
    val cy = center.y + height * 0.04f
    val w = width * 0.86f
    val h = height * 0.44f

    val wheelY = cy + h * 0.28f
    val frontWheelX = cx - w * 0.25f
    val rearWheelX = cx + w * 0.24f
    val wheelRadius = h * 0.22f

    // Road firmly touching bottom of tires
    val roadY = wheelY + wheelRadius
    drawRoundRect(
        color = Color(0xFF30411F),
        topLeft = Offset(center.x - width * 0.44f, roadY),
        size = Size(width * 0.88f, height * 0.08f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Dynamic Speed Lines / Streaks cutting across
    drawLine(
        color = Color(0xFFD3E6C1).copy(alpha = 0.85f),
        start = Offset(cx - w * 0.50f, cy - h * 0.62f),
        end = Offset(cx + w * 0.46f, cy - h * 0.62f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFD3E6C1).copy(alpha = 0.55f),
        start = Offset(cx - w * 0.46f, cy - h * 0.38f),
        end = Offset(cx - w * 0.14f, cy - h * 0.38f),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF192310).copy(alpha = 0.45f),
        start = Offset(cx + w * 0.12f, cy + h * 0.42f),
        end = Offset(cx + w * 0.48f, cy + h * 0.42f),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round
    )

    // Supra MK4 Refined Lower Hoop Rear Wing
    val supraWing = Path().apply {
        moveTo(cx + w * 0.30f, cy - h * 0.10f)
        lineTo(cx + w * 0.33f, cy - h * 0.36f)
        cubicTo(cx + w * 0.36f, cy - h * 0.40f, cx + w * 0.46f, cy - h * 0.38f, cx + w * 0.48f, cy - h * 0.28f)
        lineTo(cx + w * 0.45f, cy - h * 0.10f)
        close()
    }
    drawPath(supraWing, color = Color(0xFF202C14))

    // Supra MK4 Aerodynamic Curved Body in 3/4 Action Angle
    val supraBody = Path().apply {
        moveTo(cx - w * 0.52f, cy + h * 0.28f)
        lineTo(cx - w * 0.52f, cy + h * 0.08f)
        cubicTo(cx - w * 0.48f, cy - h * 0.02f, cx - w * 0.36f, cy - h * 0.14f, cx - w * 0.18f, cy - h * 0.20f)
        cubicTo(cx - w * 0.08f, cy - h * 0.48f, cx + w * 0.08f, cy - h * 0.54f, cx + w * 0.24f, cy - h * 0.38f)
        cubicTo(cx + w * 0.36f, cy - h * 0.24f, cx + w * 0.46f, cy - h * 0.08f, cx + w * 0.48f, cy + h * 0.12f)
        lineTo(cx + w * 0.48f, cy + h * 0.28f)
        lineTo(cx + w * 0.35f, cy + h * 0.28f)
        cubicTo(cx + w * 0.33f, cy + h * 0.06f, cx + w * 0.15f, cy + h * 0.06f, cx + w * 0.13f, cy + h * 0.28f)
        lineTo(cx - w * 0.14f, cy + h * 0.28f)
        cubicTo(cx - w * 0.16f, cy + h * 0.06f, cx - w * 0.34f, cy + h * 0.06f, cx - w * 0.36f, cy + h * 0.28f)
        close()
    }
    drawPath(supraBody, color = Color(0xFF202C14))

    // Supra Fastback Glass & Windshield in pale sage grey
    val glass = Path().apply {
        moveTo(cx - w * 0.14f, cy - h * 0.18f)
        cubicTo(cx - w * 0.06f, cy - h * 0.44f, cx + w * 0.06f, cy - h * 0.46f, cx + w * 0.20f, cy - h * 0.32f)
        cubicTo(cx + w * 0.26f, cy - h * 0.24f, cx + w * 0.30f, cy - h * 0.12f, cx + w * 0.32f, cy - h * 0.06f)
        lineTo(cx - w * 0.14f, cy - h * 0.06f)
        close()
    }
    drawPath(glass, color = Color(0xFFCBD8BF))
    drawLine(color = Color(0xFF202C14), start = Offset(cx + w * 0.08f, cy - h * 0.44f), end = Offset(cx + w * 0.08f, cy - h * 0.06f), strokeWidth = 2.5f)

    // Supra Triple Jewel Headlight in light pistachio
    drawCircle(color = Color(0xFFD3E6C1), radius = h * 0.05f, center = Offset(cx - w * 0.42f, cy - h * 0.04f))
    drawCircle(color = Color(0xFFD3E6C1), radius = h * 0.045f, center = Offset(cx - w * 0.36f, cy - h * 0.07f))
    drawCircle(color = Color(0xFFD3E6C1), radius = h * 0.04f, center = Offset(cx - w * 0.30f, cy - h * 0.09f))

    // Front Bumper Air Dam & Intercooler
    drawRoundRect(
        color = Color(0xFF192310),
        topLeft = Offset(cx - w * 0.50f, cy + h * 0.10f),
        size = Size(w * 0.16f, h * 0.14f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Wheels with tires firmly on road
    drawCircle(color = Color(0xFF192310), radius = wheelRadius, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFF192310), radius = wheelRadius * 0.94f, center = Offset(rearWheelX, wheelY))
    drawCircle(color = Color(0xFFD3E6C1), radius = wheelRadius * 0.56f, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFFD3E6C1), radius = wheelRadius * 0.52f, center = Offset(rearWheelX, wheelY))
    drawCircle(color = Color(0xFF192310), radius = wheelRadius * 0.20f, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFF192310), radius = wheelRadius * 0.18f, center = Offset(rearWheelX, wheelY))
}

/**
 * Material Illustrated Range Rover (Muted, minimalist flat vector art matching Google Pixel face style).
 */
private fun DrawScope.drawRangeRoverMaterial(
    center: Offset,
    width: Float,
    height: Float,
    pebblePath: Path
) {
    // Container background: deep forest olive matching top-right pebble in reference image
    drawPath(pebblePath, color = Color(0xFF3F542A))

    val cx = center.x
    val cy = center.y + height * 0.04f
    val w = width * 0.86f
    val h = height * 0.44f

    val wheelY = cy + h * 0.28f
    val frontWheelX = cx - w * 0.23f
    val rearWheelX = cx + w * 0.23f
    val wheelRadius = h * 0.23f

    // Road firmly touching bottom of tires
    val roadY = wheelY + wheelRadius
    drawRoundRect(
        color = Color(0xFF30411F),
        topLeft = Offset(center.x - width * 0.44f, roadY),
        size = Size(width * 0.88f, height * 0.08f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Car body in deep dark olive (matching hair in top-right reference)
    val carBody = Path().apply {
        moveTo(cx - w * 0.50f, cy + h * 0.28f)
        lineTo(cx - w * 0.50f, cy - h * 0.08f)
        cubicTo(cx - w * 0.48f, cy - h * 0.18f, cx - w * 0.36f, cy - h * 0.22f, cx - w * 0.18f, cy - h * 0.24f)
        lineTo(cx - w * 0.08f, cy - h * 0.56f)
        lineTo(cx + w * 0.36f, cy - h * 0.52f)
        lineTo(cx + w * 0.46f, cy - h * 0.16f)
        lineTo(cx + w * 0.46f, cy + h * 0.28f)
        lineTo(cx + w * 0.34f, cy + h * 0.28f)
        cubicTo(cx + w * 0.32f, cy + h * 0.06f, cx + w * 0.14f, cy + h * 0.06f, cx + w * 0.12f, cy + h * 0.28f)
        lineTo(cx - w * 0.12f, cy + h * 0.28f)
        cubicTo(cx - w * 0.14f, cy + h * 0.06f, cx - w * 0.32f, cy + h * 0.06f, cx - w * 0.34f, cy + h * 0.28f)
        close()
    }
    drawPath(carBody, color = Color(0xFF202C14))

    // Windows in pale sage grey (matching shirt in top-right reference)
    val windows = Path().apply {
        moveTo(cx - w * 0.06f, cy - h * 0.50f)
        lineTo(cx + w * 0.34f, cy - h * 0.46f)
        lineTo(cx + w * 0.38f, cy - h * 0.20f)
        lineTo(cx - w * 0.14f, cy - h * 0.20f)
        close()
    }
    drawPath(windows, color = Color(0xFFCBD8BF))

    val p1 = cx + w * 0.04f
    val p2 = cx + w * 0.18f
    drawLine(color = Color(0xFF202C14), start = Offset(p1, cy - h * 0.48f), end = Offset(p1, cy - h * 0.20f), strokeWidth = 2.5f)
    drawLine(color = Color(0xFF202C14), start = Offset(p2, cy - h * 0.47f), end = Offset(p2, cy - h * 0.20f), strokeWidth = 2.5f)

    // Grille in deep dark olive & light pistachio border
    val grilleRect = Path().apply {
        moveTo(cx - w * 0.49f, cy - h * 0.04f)
        lineTo(cx - w * 0.38f, cy - h * 0.04f)
        lineTo(cx - w * 0.38f, cy + h * 0.08f)
        lineTo(cx - w * 0.49f, cy + h * 0.08f)
        close()
    }
    drawPath(grilleRect, color = Color(0xFF202C14))
    drawPath(grilleRect, color = Color(0xFFD3E6C1), style = Stroke(width = 1.5f))

    // Headlight & lower bar in light pistachio (matching face in top-right reference)
    drawRoundRect(color = Color(0xFFD3E6C1), topLeft = Offset(cx - w * 0.49f, cy - h * 0.12f), size = Size(w * 0.14f, h * 0.065f), cornerRadius = CornerRadius(2f, 2f))
    drawRoundRect(color = Color(0xFFCBD8BF), topLeft = Offset(cx - w * 0.49f, cy + h * 0.14f), size = Size(w * 0.14f, h * 0.04f), cornerRadius = CornerRadius(1.5f, 1.5f))

    // Muted 2-tone wheels with tires firmly touching road
    drawCircle(color = Color(0xFF192310), radius = wheelRadius, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFF192310), radius = wheelRadius, center = Offset(rearWheelX, wheelY))
    drawCircle(color = Color(0xFFD3E6C1), radius = wheelRadius * 0.56f, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFFD3E6C1), radius = wheelRadius * 0.56f, center = Offset(rearWheelX, wheelY))
    drawCircle(color = Color(0xFF192310), radius = wheelRadius * 0.20f, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFF192310), radius = wheelRadius * 0.20f, center = Offset(rearWheelX, wheelY))
}

// =============================================================================
// 5. FAR-RIGHT SATELLITE DOT
// =============================================================================
private fun DrawScope.drawRightDot(
    center: Offset,
    radius: Float,
    color: Color = Color(0xFFF5EEDA)
) {
    drawCircle(
        color = color,
        radius = radius,
        center = center
    )
}

// =============================================================================
// 6. BOTTOM-RIGHT: 4-PETAL CLOVER - EVO X (MATERIAL) & NISSAN SKYLINE R34 (CINEMATIC)
// =============================================================================
private fun DrawScope.drawEvoXBadge(
    center: Offset,
    radius: Float,
    style: CarBadgeStyle,
    photoBitmap: ImageBitmap?,
    rotation: Float = 0f
) {
    val baseCloverPath = createScallopPath(
        center = center,
        numLobes = 4,
        radiusMax = radius,
        radiusMin = radius * 0.72f
    )
    val cloverPath = baseCloverPath.rotated(rotation, center)

    if (style == CarBadgeStyle.CINEMATIC) {
        clipPath(cloverPath) {
            drawSkylineR34Cinematic(center, radius, cloverPath)
        }
    } else {
        clipPath(cloverPath) {
            drawEvoXMaterial(center, radius, cloverPath)
        }
    }
}

/**
 * Cinematic Illustrated Nissan Skyline GT-R R34 in aggressive speeding 3/4 action view with speed lines.
 * Scaled a tiny bit smaller inside its clover shape.
 */
private fun DrawScope.drawSkylineR34Cinematic(
    center: Offset,
    radius: Float,
    cloverPath: Path
) {
    // Container background: warm ivory
    drawPath(cloverPath, color = Color(0xFFFAF2DC))

    // Car sized a bit smaller inside the shape
    val cx = center.x
    val cy = center.y + radius * 0.08f
    val w = radius * 1.30f
    val h = radius * 0.55f

    val wheelY = cy + h * 0.28f
    val frontWheelX = cx - w * 0.25f
    val rearWheelX = cx + w * 0.24f
    val wheelRadius = h * 0.22f

    // Road firmly touching bottom of tires
    val roadY = wheelY + wheelRadius
    drawRoundRect(
        color = Color(0xFFE8DFC8),
        topLeft = Offset(center.x - radius * 0.75f, roadY),
        size = Size(radius * 1.50f, radius * 0.14f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Dynamic Rushing Speed Lines
    drawLine(
        color = Color(0xFFFAF2DC).copy(alpha = 0.90f),
        start = Offset(cx - w * 0.52f, cy - h * 0.62f),
        end = Offset(cx + w * 0.48f, cy - h * 0.62f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFFAF2DC).copy(alpha = 0.60f),
        start = Offset(cx - w * 0.48f, cy - h * 0.36f),
        end = Offset(cx - w * 0.16f, cy - h * 0.36f),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF242C1E).copy(alpha = 0.40f),
        start = Offset(cx + w * 0.12f, cy + h * 0.42f),
        end = Offset(cx + w * 0.50f, cy + h * 0.42f),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round
    )

    // Skyline GT-R R34 Sleek Lower Rear Wing in 3/4 angle
    val r34Wing = Path().apply {
        moveTo(cx + w * 0.30f, cy - h * 0.08f)
        lineTo(cx + w * 0.33f, cy - h * 0.34f)
        lineTo(cx + w * 0.46f, cy - h * 0.34f)
        lineTo(cx + w * 0.45f, cy - h * 0.08f)
        close()
    }
    drawPath(r34Wing, color = Color(0xFF242C1E))

    // Skyline GT-R R34 Muscular Body in warm ochre / olive brown
    val r34Body = Path().apply {
        moveTo(cx - w * 0.54f, cy + h * 0.28f)
        lineTo(cx - w * 0.54f, cy + h * 0.08f)
        cubicTo(cx - w * 0.48f, cy - h * 0.04f, cx - w * 0.34f, cy - h * 0.16f, cx - w * 0.18f, cy - h * 0.22f)
        lineTo(cx - w * 0.08f, cy - h * 0.52f)
        lineTo(cx + w * 0.22f, cy - h * 0.50f)
        lineTo(cx + w * 0.46f, cy - h * 0.06f)
        lineTo(cx + w * 0.46f, cy + h * 0.28f)
        lineTo(cx + w * 0.34f, cy + h * 0.28f)
        cubicTo(cx + w * 0.32f, cy + h * 0.06f, cx + w * 0.14f, cy + h * 0.06f, cx + w * 0.12f, cy + h * 0.28f)
        lineTo(cx - w * 0.12f, cy + h * 0.28f)
        cubicTo(cx - w * 0.14f, cy + h * 0.06f, cx - w * 0.32f, cy + h * 0.06f, cx - w * 0.34f, cy + h * 0.28f)
        close()
    }
    drawPath(r34Body, color = Color(0xFF7A5C1E))

    // Windows in medium olive grey
    val windows = Path().apply {
        moveTo(cx - w * 0.14f, cy - h * 0.18f)
        lineTo(cx - w * 0.06f, cy - h * 0.44f)
        lineTo(cx + w * 0.20f, cy - h * 0.42f)
        lineTo(cx + w * 0.28f, cy - h * 0.08f)
        lineTo(cx - w * 0.14f, cy - h * 0.08f)
        close()
    }
    drawPath(windows, color = Color(0xFF76846B))
    drawLine(color = Color(0xFF242C1E), start = Offset(cx + w * 0.06f, cy - h * 0.43f), end = Offset(cx + w * 0.06f, cy - h * 0.08f), strokeWidth = 2f)

    // Aggressive Front Grille & Intercooler in dark charcoal olive
    val frontGrille = Path().apply {
        moveTo(cx - w * 0.53f, cy - h * 0.02f)
        lineTo(cx - w * 0.38f, cy - h * 0.02f)
        lineTo(cx - w * 0.40f, cy + h * 0.24f)
        lineTo(cx - w * 0.53f, cy + h * 0.24f)
        close()
    }
    drawPath(frontGrille, color = Color(0xFF242C1E))

    // Sharp Headlight in warm cream
    val headlight = Path().apply {
        moveTo(cx - w * 0.50f, cy - h * 0.12f)
        lineTo(cx - w * 0.38f, cy - h * 0.06f)
        lineTo(cx - w * 0.36f, cy - h * 0.02f)
        lineTo(cx - w * 0.49f, cy - h * 0.02f)
        close()
    }
    drawPath(headlight, color = Color(0xFFFAF2DC))

    // Wheels firmly touching road
    drawCircle(color = Color(0xFF242C1E), radius = wheelRadius, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFF242C1E), radius = wheelRadius * 0.94f, center = Offset(rearWheelX, wheelY))
    drawCircle(color = Color(0xFFE8DFC8), radius = wheelRadius * 0.54f, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFFE8DFC8), radius = wheelRadius * 0.50f, center = Offset(rearWheelX, wheelY))
    drawCircle(color = Color(0xFF242C1E), radius = wheelRadius * 0.20f, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFF242C1E), radius = wheelRadius * 0.18f, center = Offset(rearWheelX, wheelY))
}

/**
 * Material Illustrated Evo X (Muted, minimalist flat vector art matching Google Pixel face style).
 * Sized a tiny bit smaller inside its shape.
 */
private fun DrawScope.drawEvoXMaterial(
    center: Offset,
    radius: Float,
    cloverPath: Path
) {
    // Container background: warm ivory / soft light cream matching bottom-right clover in reference image
    drawPath(cloverPath, color = Color(0xFFFAF2DC))

    // Sized a bit smaller inside its shape
    val cx = center.x
    val cy = center.y + radius * 0.08f
    val w = radius * 1.30f
    val h = radius * 0.55f

    val wheelY = cy + h * 0.28f
    val frontWheelX = cx - w * 0.23f
    val rearWheelX = cx + w * 0.23f
    val wheelRadius = h * 0.23f

    // Road firmly touching bottom of tires
    val roadY = wheelY + wheelRadius
    drawRoundRect(
        color = Color(0xFFE8DFC8),
        topLeft = Offset(center.x - radius * 0.75f, roadY),
        size = Size(radius * 1.50f, radius * 0.14f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Evo X Sleek Low Rear Wing in dark charcoal olive
    val evoXWing = Path().apply {
        moveTo(cx + w * 0.34f, cy - h * 0.08f)
        lineTo(cx + w * 0.38f, cy - h * 0.28f)
        lineTo(cx + w * 0.48f, cy - h * 0.28f)
        lineTo(cx + w * 0.46f, cy - h * 0.06f)
        close()
    }
    drawPath(evoXWing, color = Color(0xFF242C1E))

    // Evo X Body in warm ochre / olive-brown (matching shirt/shoulders in bottom-right reference)
    val carBody = Path().apply {
        moveTo(cx - w * 0.52f, cy + h * 0.28f)
        lineTo(cx - w * 0.52f, cy + h * 0.08f)
        cubicTo(cx - w * 0.48f, cy - h * 0.04f, cx - w * 0.34f, cy - h * 0.16f, cx - w * 0.18f, cy - h * 0.24f)
        lineTo(cx - w * 0.08f, cy - h * 0.54f)
        lineTo(cx + w * 0.20f, cy - h * 0.52f)
        lineTo(cx + w * 0.46f, cy - h * 0.06f)
        lineTo(cx + w * 0.46f, cy + h * 0.28f)
        lineTo(cx + w * 0.34f, cy + h * 0.28f)
        cubicTo(cx + w * 0.32f, cy + h * 0.06f, cx + w * 0.14f, cy + h * 0.06f, cx + w * 0.12f, cy + h * 0.28f)
        lineTo(cx - w * 0.12f, cy + h * 0.28f)
        cubicTo(cx - w * 0.14f, cy + h * 0.06f, cx - w * 0.32f, cy + h * 0.06f, cx - w * 0.34f, cy + h * 0.28f)
        close()
    }
    drawPath(carBody, color = Color(0xFF7A5C1E))

    // Windows in medium olive grey (matching face/neck in bottom-right reference)
    val windows = Path().apply {
        moveTo(cx - w * 0.14f, cy - h * 0.20f)
        lineTo(cx - w * 0.06f, cy - h * 0.46f)
        lineTo(cx + w * 0.18f, cy - h * 0.44f)
        lineTo(cx + w * 0.26f, cy - h * 0.08f)
        lineTo(cx - w * 0.14f, cy - h * 0.08f)
        close()
    }
    drawPath(windows, color = Color(0xFF76846B))
    drawLine(color = Color(0xFF242C1E), start = Offset(cx - w * 0.04f, cy - h * 0.44f), end = Offset(cx + w * 0.10f, cy - h * 0.42f), strokeWidth = 2f)

    // Jet fighter grille in dark charcoal olive
    val jetFighterGrille = Path().apply {
        moveTo(cx - w * 0.51f, cy - h * 0.04f)
        lineTo(cx - w * 0.36f, cy - h * 0.04f)
        lineTo(cx - w * 0.38f, cy + h * 0.24f)
        lineTo(cx - w * 0.51f, cy + h * 0.24f)
        close()
    }
    drawPath(jetFighterGrille, color = Color(0xFF242C1E))

    // Headlight in warm cream/ivory
    val headlightPath = Path().apply {
        moveTo(cx - w * 0.50f, cy - h * 0.14f)
        lineTo(cx - w * 0.38f, cy - h * 0.08f)
        lineTo(cx - w * 0.36f, cy - h * 0.04f)
        lineTo(cx - w * 0.49f, cy - h * 0.04f)
        close()
    }
    drawPath(headlightPath, color = Color(0xFFFAF2DC))

    // Muted 2-tone wheels with tires firmly touching road
    drawCircle(color = Color(0xFF242C1E), radius = wheelRadius, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFF242C1E), radius = wheelRadius, center = Offset(rearWheelX, wheelY))
    drawCircle(color = Color(0xFFE8DFC8), radius = wheelRadius * 0.54f, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFFE8DFC8), radius = wheelRadius * 0.54f, center = Offset(rearWheelX, wheelY))
    drawCircle(color = Color(0xFF242C1E), radius = wheelRadius * 0.20f, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFF242C1E), radius = wheelRadius * 0.20f, center = Offset(rearWheelX, wheelY))
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

/**
 * Rotates a Path around a pivot point using Android Matrix transformation.
 */
private fun Path.rotated(degrees: Float, pivot: Offset): Path {
    if (degrees == 0f) return this
    val matrix = Matrix().apply {
        postRotate(degrees, pivot.x, pivot.y)
    }
    return Path().apply {
        addPath(this@rotated)
        asAndroidPath().transform(matrix)
    }
}
