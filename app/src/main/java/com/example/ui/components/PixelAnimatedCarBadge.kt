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
import androidx.compose.material.icons.filled.Pets
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
import androidx.compose.ui.graphics.StrokeJoin
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
    capyVariant: CapyVariant = CapyVariant.BABY,
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

        // Center Car Icon OR Cute Capybara (Baby vs Adult)
        if (style == CarBadgeStyle.CAPY) {
            Canvas(
                modifier = Modifier.size(size * 0.38f)
            ) {
                if (capyVariant == CapyVariant.BABY) {
                    drawBabyCapybaraFace(
                        center = Offset(size.toPx() * 0.19f, size.toPx() * 0.19f),
                        size = size.toPx() * 0.38f,
                        tintColor = iconColor
                    )
                } else {
                    drawAdultCapybaraFace(
                        center = Offset(size.toPx() * 0.19f, size.toPx() * 0.19f),
                        size = size.toPx() * 0.38f,
                        tintColor = iconColor
                    )
                }
            }
        } else {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = "Car",
                tint = iconColor,
                modifier = Modifier.size(size * 0.28f)
            )
        }
    }
}

/**
 * Draws an ultra-cute, chubby baby Capybara with sweet closed smiling eyes:
 * - Round, squishy mochi-like baby head and chubby cheek proportions
 * - Sweet happy closed smiling eye arcs with tiny baby eyelashes: ( ˘ ‿ ˘ )
 * - Chubby glowing rosy pink blushing cheeks with shine highlight dots
 * - Small soft rounded baby snout with cute button nose & cheerful baby split smile
 * - Tiny cupped baby ears with pastel pink inner fluff
 * - Cute tiny front baby paws with little rounded toe pads
 * - Adorable little golden yuzu fruit with emerald leaf balanced on head
 */
private fun DrawScope.drawBabyCapybaraFace(
    center: Offset,
    size: Float,
    tintColor: Color
) {
    val cx = center.x
    val cy = center.y + size * 0.03f
    val w = size
    val h = size

    val outlineColor = Color(0xFF221612)
    val bodyFillColor = Color(0xFFE8BA8E)
    val snoutColor = Color(0xFFA67756)
    val earDarkColor = Color(0xFF8D5B3E)
    val earPinkColor = Color(0xFFFFAEC9)
    val blushColor = Color(0xFFFF7B8B).copy(alpha = 0.65f)
    val mainStrokeWidth = w * 0.054f
    val detailStrokeWidth = w * 0.038f

    // 0. CUTE YUZU ORANGE ON HEAD (World-famous cute baby capybara feature)
    val yuzuRadius = w * 0.085f
    val yuzuCenter = Offset(cx, cy - h * 0.36f)

    // Yuzu Leaf
    val leafPath = Path().apply {
        moveTo(yuzuCenter.x, yuzuCenter.y - yuzuRadius * 0.9f)
        cubicTo(
            yuzuCenter.x + w * 0.06f, yuzuCenter.y - yuzuRadius * 1.8f,
            yuzuCenter.x + w * 0.10f, yuzuCenter.y - yuzuRadius * 1.5f,
            yuzuCenter.x + w * 0.04f, yuzuCenter.y - yuzuRadius * 0.7f
        )
        close()
    }
    drawPath(leafPath, color = Color(0xFF4CAF50))
    drawPath(leafPath, color = outlineColor, style = Stroke(width = detailStrokeWidth * 0.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // Yuzu Fruit body
    drawCircle(
        color = Color(0xFFFFB300),
        radius = yuzuRadius,
        center = yuzuCenter
    )
    drawCircle(
        color = outlineColor,
        radius = yuzuRadius,
        center = yuzuCenter,
        style = Stroke(width = detailStrokeWidth * 1.1f)
    )
    // Yuzu Highlight
    drawCircle(
        color = Color(0xFFFFF9C4),
        radius = yuzuRadius * 0.28f,
        center = Offset(yuzuCenter.x - yuzuRadius * 0.35f, yuzuCenter.y - yuzuRadius * 0.35f)
    )

    // 1. CUTE BABY EARS (Rounded chubby baby ears)
    // Left Ear
    val leftEarPath = Path().apply {
        moveTo(cx - w * 0.21f, cy - h * 0.26f)
        cubicTo(
            cx - w * 0.30f, cy - h * 0.42f,
            cx - w * 0.42f, cy - h * 0.36f,
            cx - w * 0.34f, cy - h * 0.18f
        )
        close()
    }
    drawPath(leftEarPath, color = earDarkColor)
    drawPath(leftEarPath, color = outlineColor, style = Stroke(width = mainStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // Left Inner Ear Pink Pad
    val leftInnerPad = Path().apply {
        moveTo(cx - w * 0.24f, cy - h * 0.25f)
        cubicTo(
            cx - w * 0.29f, cy - h * 0.36f,
            cx - w * 0.36f, cy - h * 0.32f,
            cx - w * 0.32f, cy - h * 0.20f
        )
        close()
    }
    drawPath(leftInnerPad, color = earPinkColor)

    // Right Ear
    val rightEarPath = Path().apply {
        moveTo(cx + w * 0.21f, cy - h * 0.26f)
        cubicTo(
            cx + w * 0.30f, cy - h * 0.42f,
            cx + w * 0.42f, cy - h * 0.36f,
            cx + w * 0.34f, cy - h * 0.18f
        )
        close()
    }
    drawPath(rightEarPath, color = earDarkColor)
    drawPath(rightEarPath, color = outlineColor, style = Stroke(width = mainStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // Right Inner Ear Pink Pad
    val rightInnerPad = Path().apply {
        moveTo(cx + w * 0.24f, cy - h * 0.25f)
        cubicTo(
            cx + w * 0.29f, cy - h * 0.36f,
            cx + w * 0.36f, cy - h * 0.32f,
            cx + w * 0.32f, cy - h * 0.20f
        )
        close()
    }
    drawPath(rightInnerPad, color = earPinkColor)

    // 2. CHUBBY SQUISHY BABY BODY SILHOUETTE
    val babyBodyPath = Path().apply {
        moveTo(cx, cy - h * 0.34f)
        // Top-left skull
        cubicTo(cx - w * 0.18f, cy - h * 0.34f, cx - w * 0.30f, cy - h * 0.28f, cx - w * 0.34f, cy - h * 0.18f)
        // Chubby puffy left cheek
        cubicTo(cx - w * 0.40f, cy - h * 0.08f, cx - w * 0.39f, cy + h * 0.06f, cx - w * 0.34f, cy + h * 0.14f)
        // Chubby lower belly
        cubicTo(cx - w * 0.38f, cy + h * 0.24f, cx - w * 0.38f, cy + h * 0.34f, cx - w * 0.26f, cy + h * 0.38f)
        // Bottom center
        cubicTo(cx - w * 0.14f, cy + h * 0.40f, cx + w * 0.14f, cy + h * 0.40f, cx + w * 0.26f, cy + h * 0.38f)
        // Right chubby belly
        cubicTo(cx + w * 0.38f, cy + h * 0.34f, cx + w * 0.38f, cy + h * 0.24f, cx + w * 0.34f, cy + h * 0.14f)
        // Chubby puffy right cheek
        cubicTo(cx + w * 0.39f, cy + h * 0.06f, cx + w * 0.40f, cy - h * 0.08f, cx + w * 0.34f, cy - h * 0.18f)
        // Top-right skull
        cubicTo(cx + w * 0.30f, cy - h * 0.28f, cx + w * 0.18f, cy - h * 0.34f, cx, cy - h * 0.34f)
        close()
    }
    drawPath(babyBodyPath, color = bodyFillColor)
    drawPath(babyBodyPath, color = outlineColor, style = Stroke(width = mainStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // 3. CUTE TINY SITTING BABY FEET
    // Left Foot
    val leftFoot = Path().apply {
        moveTo(cx - w * 0.22f, cy + h * 0.33f)
        cubicTo(cx - w * 0.35f, cy + h * 0.31f, cx - w * 0.37f, cy + h * 0.40f, cx - w * 0.20f, cy + h * 0.39f)
        close()
    }
    drawPath(leftFoot, color = snoutColor)
    drawPath(leftFoot, color = outlineColor, style = Stroke(width = detailStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // Right Foot
    val rightFoot = Path().apply {
        moveTo(cx + w * 0.22f, cy + h * 0.33f)
        cubicTo(cx + w * 0.35f, cy + h * 0.31f, cx + w * 0.37f, cy + h * 0.40f, cx + w * 0.20f, cy + h * 0.39f)
        close()
    }
    drawPath(rightFoot, color = snoutColor)
    drawPath(rightFoot, color = outlineColor, style = Stroke(width = detailStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // 4. CUTE TINY FRONT BABY PAWS (Tucked in center chest)
    val babyPawTopY = cy + h * 0.22f
    val babyPawBottomY = cy + h * 0.35f

    // Left Front Baby Paw
    val leftBabyPaw = Path().apply {
        moveTo(cx - w * 0.13f, babyPawTopY)
        lineTo(cx - w * 0.13f, babyPawBottomY - h * 0.02f)
        cubicTo(cx - w * 0.13f, babyPawBottomY + h * 0.015f, cx - w * 0.09f, babyPawBottomY + h * 0.015f, cx - w * 0.08f, babyPawBottomY)
        cubicTo(cx - w * 0.07f, babyPawBottomY + h * 0.015f, cx - w * 0.03f, babyPawBottomY + h * 0.015f, cx - w * 0.02f, babyPawBottomY - h * 0.02f)
        lineTo(cx - w * 0.02f, babyPawTopY)
        close()
    }
    drawPath(leftBabyPaw, color = snoutColor)
    drawPath(leftBabyPaw, color = outlineColor, style = Stroke(width = detailStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // Right Front Baby Paw
    val rightBabyPaw = Path().apply {
        moveTo(cx + w * 0.02f, babyPawTopY)
        lineTo(cx + w * 0.02f, babyPawBottomY - h * 0.02f)
        cubicTo(cx + w * 0.03f, babyPawBottomY + h * 0.015f, cx + w * 0.07f, babyPawBottomY + h * 0.015f, cx + w * 0.08f, babyPawBottomY)
        cubicTo(cx + w * 0.09f, babyPawBottomY + h * 0.015f, cx + w * 0.13f, babyPawBottomY + h * 0.015f, cx + w * 0.13f, babyPawBottomY - h * 0.02f)
        lineTo(cx + w * 0.13f, babyPawTopY)
        close()
    }
    drawPath(rightBabyPaw, color = snoutColor)
    drawPath(rightBabyPaw, color = outlineColor, style = Stroke(width = detailStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // 5. ROUND SOFT BABY SNOUT / MUZZLE
    val babySnoutW = w * 0.26f
    val babySnoutH = h * 0.26f
    val babySnoutTop = cy - h * 0.15f
    val babySnoutPath = Path().apply {
        addRoundRect(
            RoundRect(
                left = cx - babySnoutW / 2f,
                top = babySnoutTop,
                right = cx + babySnoutW / 2f,
                bottom = babySnoutTop + babySnoutH,
                cornerRadius = CornerRadius(babySnoutW * 0.48f, babySnoutH * 0.48f)
            )
        )
    }
    drawPath(babySnoutPath, color = snoutColor)
    drawPath(babySnoutPath, color = outlineColor, style = Stroke(width = mainStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // Baby Button Nose & Nostrils
    val babyNoseY = cy - h * 0.085f
    drawCircle(
        color = outlineColor,
        radius = w * 0.024f,
        center = Offset(cx - w * 0.032f, babyNoseY)
    )
    drawCircle(
        color = outlineColor,
        radius = w * 0.024f,
        center = Offset(cx + w * 0.032f, babyNoseY)
    )

    // Sweet Baby Smile (Cheerful curved kitten/baby split mouth)
    val mouthY = cy + h * 0.005f
    drawLine(
        color = outlineColor,
        start = Offset(cx, babyNoseY + h * 0.012f),
        end = Offset(cx, mouthY),
        strokeWidth = detailStrokeWidth * 1.1f,
        cap = StrokeCap.Round
    )
    // Left smile curve
    val leftSmile = Path().apply {
        moveTo(cx, mouthY)
        quadraticTo(cx - w * 0.035f, mouthY + h * 0.025f, cx - w * 0.055f, mouthY + h * 0.005f)
    }
    drawPath(leftSmile, color = outlineColor, style = Stroke(width = detailStrokeWidth * 1.15f, cap = StrokeCap.Round))

    // Right smile curve
    val rightSmile = Path().apply {
        moveTo(cx, mouthY)
        quadraticTo(cx + w * 0.035f, mouthY + h * 0.025f, cx + w * 0.055f, mouthY + h * 0.005f)
    }
    drawPath(rightSmile, color = outlineColor, style = Stroke(width = detailStrokeWidth * 1.15f, cap = StrokeCap.Round))

    // 6. ADORABLE HAPPY CLOSED SMILING EYES ( ^  ‿  ^ )
    val eyeLevelY = cy - h * 0.11f
    // Left Closed Smiling Eye Arc
    val leftClosedEye = Path().apply {
        moveTo(cx - w * 0.26f, eyeLevelY + h * 0.005f)
        cubicTo(
            cx - w * 0.25f, eyeLevelY - h * 0.035f,
            cx - w * 0.17f, eyeLevelY - h * 0.035f,
            cx - w * 0.15f, eyeLevelY + h * 0.005f
        )
    }
    drawPath(leftClosedEye, color = outlineColor, style = Stroke(width = detailStrokeWidth * 1.35f, cap = StrokeCap.Round))
    // Tiny cute left eyelash at outer corner
    drawLine(
        color = outlineColor,
        start = Offset(cx - w * 0.26f, eyeLevelY - h * 0.01f),
        end = Offset(cx - w * 0.29f, eyeLevelY - h * 0.025f),
        strokeWidth = detailStrokeWidth * 0.9f,
        cap = StrokeCap.Round
    )

    // Right Closed Smiling Eye Arc
    val rightClosedEye = Path().apply {
        moveTo(cx + w * 0.15f, eyeLevelY + h * 0.005f)
        cubicTo(
            cx + w * 0.17f, eyeLevelY - h * 0.035f,
            cx + w * 0.25f, eyeLevelY - h * 0.035f,
            cx + w * 0.26f, eyeLevelY + h * 0.005f
        )
    }
    drawPath(rightClosedEye, color = outlineColor, style = Stroke(width = detailStrokeWidth * 1.35f, cap = StrokeCap.Round))
    // Tiny cute right eyelash at outer corner
    drawLine(
        color = outlineColor,
        start = Offset(cx + w * 0.26f, eyeLevelY - h * 0.01f),
        end = Offset(cx + w * 0.29f, eyeLevelY - h * 0.025f),
        strokeWidth = detailStrokeWidth * 0.9f,
        cap = StrokeCap.Round
    )

    // 7. BIG ROSY CHUBBY BLUSHING CHEEKS WITH GLOW
    val babyBlushY = cy - h * 0.035f
    // Left Cheek
    drawOval(
        color = blushColor,
        topLeft = Offset(cx - w * 0.32f, babyBlushY - h * 0.045f),
        size = Size(w * 0.14f, h * 0.09f)
    )

    // Right Cheek
    drawOval(
        color = blushColor,
        topLeft = Offset(cx + w * 0.18f, babyBlushY - h * 0.045f),
        size = Size(w * 0.14f, h * 0.09f)
    )
}

/**
 * Draws an authentic, adorable sitting Capybara matching the reference artwork:
 * - Bell/pear-shaped chubby tan body with bold dark outline
 * - Small dark-brown cupped ears on top corners
 * - Sleepy horizontal slit eyes
 * - Large chocolate-brown rounded snout with teardrop nostrils & inverted-Y split lip
 * - Soft painterly rosy pink blushing cheeks with sketch marks
 * - Two upright 3-toed front paws in center and two rounded sitting back feet on the sides
 */
private fun DrawScope.drawAdultCapybaraFace(
    center: Offset,
    size: Float,
    tintColor: Color
) {
    val cx = center.x
    val cy = center.y + size * 0.02f
    val w = size
    val h = size

    val outlineColor = Color(0xFF1E1715)
    val bodyFillColor = Color(0xFFE2B789)
    val snoutColor = Color(0xFF986B4D)
    val earDarkColor = Color(0xFF754B31)
    val innerEarFold = Color(0xFF4B2B1B)
    val blushColor = Color(0xFFFF8B7D).copy(alpha = 0.65f)
    val mainStrokeWidth = w * 0.052f
    val detailStrokeWidth = w * 0.040f

    // 1. EARS (Drawn behind head contour)
    // Left Ear
    val leftEarPath = Path().apply {
        moveTo(cx - w * 0.22f, cy - h * 0.28f)
        cubicTo(
            cx - w * 0.32f, cy - h * 0.44f,
            cx - w * 0.44f, cy - h * 0.38f,
            cx - w * 0.36f, cy - h * 0.20f
        )
        close()
    }
    drawPath(leftEarPath, color = earDarkColor)
    drawPath(
        path = leftEarPath,
        color = outlineColor,
        style = Stroke(width = mainStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    // Left Inner Ear Fold
    val leftInnerFold = Path().apply {
        moveTo(cx - w * 0.28f, cy - h * 0.28f)
        quadraticTo(cx - w * 0.36f, cy - h * 0.35f, cx - w * 0.34f, cy - h * 0.24f)
    }
    drawPath(leftInnerFold, color = innerEarFold, style = Stroke(width = detailStrokeWidth * 0.9f, cap = StrokeCap.Round))

    // Right Ear
    val rightEarPath = Path().apply {
        moveTo(cx + w * 0.22f, cy - h * 0.28f)
        cubicTo(
            cx + w * 0.32f, cy - h * 0.44f,
            cx + w * 0.44f, cy - h * 0.38f,
            cx + w * 0.36f, cy - h * 0.20f
        )
        close()
    }
    drawPath(rightEarPath, color = earDarkColor)
    drawPath(
        path = rightEarPath,
        color = outlineColor,
        style = Stroke(width = mainStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    // Right Inner Ear Fold
    val rightInnerFold = Path().apply {
        moveTo(cx + w * 0.28f, cy - h * 0.28f)
        quadraticTo(cx + w * 0.36f, cy - h * 0.35f, cx + w * 0.34f, cy - h * 0.24f)
    }
    drawPath(rightInnerFold, color = innerEarFold, style = Stroke(width = detailStrokeWidth * 0.9f, cap = StrokeCap.Round))

    // 2. MAIN BELL/PEAR-SHAPED BODY (Accurate silhouette from reference)
    val bodyPath = Path().apply {
        moveTo(cx, cy - h * 0.35f) // Top-center skull
        // Top-left head curve
        cubicTo(cx - w * 0.16f, cy - h * 0.35f, cx - w * 0.28f, cy - h * 0.30f, cx - w * 0.32f, cy - h * 0.20f)
        // Left cheek swell
        cubicTo(cx - w * 0.36f, cy - h * 0.10f, cx - w * 0.36f, cy - h * 0.02f, cx - w * 0.33f, cy + h * 0.05f)
        // Left waist / torso widening into chubby lower belly & hip
        cubicTo(cx - w * 0.36f, cy + h * 0.12f, cx - w * 0.43f, cy + h * 0.22f, cx - w * 0.42f, cy + h * 0.33f)
        // Bottom left corner
        cubicTo(cx - w * 0.41f, cy + h * 0.39f, cx - w * 0.28f, cy + h * 0.40f, cx, cy + h * 0.40f)
        // Bottom right corner
        cubicTo(cx + w * 0.28f, cy + h * 0.40f, cx + w * 0.41f, cy + h * 0.39f, cx + w * 0.42f, cy + h * 0.33f)
        // Right hip & waist
        cubicTo(cx + w * 0.43f, cy + h * 0.22f, cx + w * 0.36f, cy + h * 0.12f, cx + w * 0.33f, cy + h * 0.05f)
        // Right cheek swell
        cubicTo(cx + w * 0.36f, cy - h * 0.02f, cx + w * 0.36f, cy - h * 0.10f, cx + w * 0.32f, cy - h * 0.20f)
        // Top-right head curve
        cubicTo(cx + w * 0.28f, cy - h * 0.30f, cx + w * 0.16f, cy - h * 0.35f, cx, cy - h * 0.35f)
        close()
    }
    // Fill Body
    drawPath(bodyPath, color = bodyFillColor)
    // Stroke Body Outline
    drawPath(
        path = bodyPath,
        color = outlineColor,
        style = Stroke(width = mainStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // 3. SITTING PAWS & FEET AT BASE (Matching reference artwork)
    val pawTopY = cy + h * 0.26f
    val pawBottomY = cy + h * 0.39f

    // Outer Left Hind Foot (Horizontal rounded pad)
    val leftHindFoot = Path().apply {
        moveTo(cx - w * 0.26f, cy + h * 0.34f)
        cubicTo(
            cx - w * 0.42f, cy + h * 0.32f,
            cx - w * 0.43f, cy + h * 0.42f,
            cx - w * 0.26f, cy + h * 0.41f
        )
        close()
    }
    drawPath(leftHindFoot, color = snoutColor)
    drawPath(leftHindFoot, color = outlineColor, style = Stroke(width = detailStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Hind foot toe notches
    drawLine(outlineColor, Offset(cx - w * 0.38f, cy + h * 0.35f), Offset(cx - w * 0.34f, cy + h * 0.38f), strokeWidth = detailStrokeWidth * 0.8f, cap = StrokeCap.Round)
    drawLine(outlineColor, Offset(cx - w * 0.34f, cy + h * 0.35f), Offset(cx - w * 0.30f, cy + h * 0.38f), strokeWidth = detailStrokeWidth * 0.8f, cap = StrokeCap.Round)

    // Outer Right Hind Foot
    val rightHindFoot = Path().apply {
        moveTo(cx + w * 0.26f, cy + h * 0.34f)
        cubicTo(
            cx + w * 0.42f, cy + h * 0.32f,
            cx + w * 0.43f, cy + h * 0.42f,
            cx + w * 0.26f, cy + h * 0.41f
        )
        close()
    }
    drawPath(rightHindFoot, color = snoutColor)
    drawPath(rightHindFoot, color = outlineColor, style = Stroke(width = detailStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Hind foot toe notches
    drawLine(outlineColor, Offset(cx + w * 0.38f, cy + h * 0.35f), Offset(cx + w * 0.34f, cy + h * 0.38f), strokeWidth = detailStrokeWidth * 0.8f, cap = StrokeCap.Round)
    drawLine(outlineColor, Offset(cx + w * 0.34f, cy + h * 0.35f), Offset(cx + w * 0.30f, cy + h * 0.38f), strokeWidth = detailStrokeWidth * 0.8f, cap = StrokeCap.Round)

    // Left Front Paw (Vertical column with 3 rounded toes at bottom)
    val leftFrontPaw = Path().apply {
        moveTo(cx - w * 0.16f, pawTopY)
        lineTo(cx - w * 0.16f, pawBottomY - h * 0.03f)
        // 3 cute rounded toes
        cubicTo(cx - w * 0.16f, pawBottomY + h * 0.02f, cx - w * 0.12f, pawBottomY + h * 0.02f, cx - w * 0.11f, pawBottomY - h * 0.01f)
        cubicTo(cx - w * 0.10f, pawBottomY + h * 0.02f, cx - w * 0.06f, pawBottomY + h * 0.02f, cx - w * 0.05f, pawBottomY - h * 0.01f)
        cubicTo(cx - w * 0.04f, pawBottomY + h * 0.02f, cx - w * 0.01f, pawBottomY + h * 0.01f, cx - w * 0.02f, pawBottomY - h * 0.03f)
        lineTo(cx - w * 0.02f, pawTopY)
        close()
    }
    drawPath(leftFrontPaw, color = snoutColor)
    drawPath(leftFrontPaw, color = outlineColor, style = Stroke(width = detailStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // Right Front Paw (Vertical column with 3 rounded toes at bottom)
    val rightFrontPaw = Path().apply {
        moveTo(cx + w * 0.02f, pawTopY)
        lineTo(cx + w * 0.02f, pawBottomY - h * 0.03f)
        // 3 cute rounded toes
        cubicTo(cx + w * 0.01f, pawBottomY + h * 0.01f, cx + w * 0.04f, pawBottomY + h * 0.02f, cx + w * 0.05f, pawBottomY - h * 0.01f)
        cubicTo(cx + w * 0.06f, pawBottomY + h * 0.02f, cx + w * 0.10f, pawBottomY + h * 0.02f, cx + w * 0.11f, pawBottomY - h * 0.01f)
        cubicTo(cx + w * 0.12f, pawBottomY + h * 0.02f, cx + w * 0.16f, pawBottomY + h * 0.02f, cx + w * 0.16f, pawBottomY - h * 0.03f)
        lineTo(cx + w * 0.16f, pawTopY)
        close()
    }
    drawPath(rightFrontPaw, color = snoutColor)
    drawPath(rightFrontPaw, color = outlineColor, style = Stroke(width = detailStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // 4. DISTINCT OVAL CHOCOLATE SNOUT / MUZZLE
    val snoutWidth = w * 0.28f
    val snoutHeight = h * 0.32f
    val snoutTop = cy - h * 0.21f
    val snoutPath = Path().apply {
        addRoundRect(
            RoundRect(
                left = cx - snoutWidth / 2f,
                top = snoutTop,
                right = cx + snoutWidth / 2f,
                bottom = snoutTop + snoutHeight,
                cornerRadius = CornerRadius(snoutWidth * 0.44f, snoutHeight * 0.46f)
            )
        )
    }
    drawPath(snoutPath, color = snoutColor)
    drawPath(
        path = snoutPath,
        color = outlineColor,
        style = Stroke(width = mainStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // 5. NOSTRILS & DOWNWARD SPLIT LIP (Inside Snout)
    val nostrilY = cy - h * 0.12f
    // Left nostril teardrop
    val leftNostril = Path().apply {
        moveTo(cx - w * 0.045f, nostrilY - h * 0.025f)
        cubicTo(cx - w * 0.06f, nostrilY, cx - w * 0.045f, nostrilY + h * 0.02f, cx - w * 0.03f, nostrilY)
        close()
    }
    drawPath(leftNostril, color = outlineColor)

    // Right nostril teardrop
    val rightNostril = Path().apply {
        moveTo(cx + w * 0.045f, nostrilY - h * 0.025f)
        cubicTo(cx + w * 0.06f, nostrilY, cx + w * 0.045f, nostrilY + h * 0.02f, cx + w * 0.03f, nostrilY)
        close()
    }
    drawPath(rightNostril, color = outlineColor)

    // Vertical Philtrum line
    val mouthY = cy - h * 0.01f
    drawLine(
        color = outlineColor,
        start = Offset(cx, nostrilY + h * 0.005f),
        end = Offset(cx, mouthY),
        strokeWidth = detailStrokeWidth * 1.1f,
        cap = StrokeCap.Round
    )

    // Inverted-Y Calm / Neutral Split Lip
    val leftLip = Path().apply {
        moveTo(cx, mouthY)
        quadraticTo(cx - w * 0.035f, mouthY + h * 0.025f, cx - w * 0.05f, mouthY + h * 0.018f)
    }
    drawPath(leftLip, color = outlineColor, style = Stroke(width = detailStrokeWidth * 1.1f, cap = StrokeCap.Round))

    val rightLip = Path().apply {
        moveTo(cx, mouthY)
        quadraticTo(cx + w * 0.035f, mouthY + h * 0.025f, cx + w * 0.05f, mouthY + h * 0.018f)
    }
    drawPath(rightLip, color = outlineColor, style = Stroke(width = detailStrokeWidth * 1.1f, cap = StrokeCap.Round))

    // 6. SLEEPY HORIZONTAL DASH EYES (-   -)
    val eyeY = cy - h * 0.145f
    // Left Eye
    val leftEye = Path().apply {
        moveTo(cx - w * 0.28f, eyeY - h * 0.01f)
        quadraticTo(cx - w * 0.22f, eyeY, cx - w * 0.17f, eyeY - h * 0.005f)
    }
    drawPath(leftEye, color = outlineColor, style = Stroke(width = detailStrokeWidth * 1.25f, cap = StrokeCap.Round))

    // Right Eye
    val rightEye = Path().apply {
        moveTo(cx + w * 0.17f, eyeY - h * 0.005f)
        quadraticTo(cx + w * 0.22f, eyeY, cx + w * 0.28f, eyeY - h * 0.01f)
    }
    drawPath(rightEye, color = outlineColor, style = Stroke(width = detailStrokeWidth * 1.25f, cap = StrokeCap.Round))

    // 7. PAINTERLY ROSY BLUSH CHEEKS (Matching Reference)
    val blushY = cy - h * 0.08f
    // Left Cheek Blush Oval
    drawOval(
        color = blushColor,
        topLeft = Offset(cx - w * 0.29f, blushY - h * 0.045f),
        size = Size(w * 0.13f, h * 0.09f)
    )
    // Left Cheek Texture Marks
    drawLine(Color(0xFFE57373).copy(alpha = 0.7f), Offset(cx - w * 0.24f, blushY - h * 0.02f), Offset(cx - w * 0.22f, blushY + h * 0.02f), strokeWidth = w * 0.018f, cap = StrokeCap.Round)
    drawLine(Color(0xFFE57373).copy(alpha = 0.7f), Offset(cx - w * 0.20f, blushY - h * 0.02f), Offset(cx - w * 0.18f, blushY + h * 0.02f), strokeWidth = w * 0.018f, cap = StrokeCap.Round)

    // Right Cheek Blush Oval
    drawOval(
        color = blushColor,
        topLeft = Offset(cx + w * 0.16f, blushY - h * 0.045f),
        size = Size(w * 0.13f, h * 0.09f)
    )
    // Right Cheek Texture Marks
    drawLine(Color(0xFFE57373).copy(alpha = 0.7f), Offset(cx + w * 0.18f, blushY - h * 0.02f), Offset(cx + w * 0.20f, blushY + h * 0.02f), strokeWidth = w * 0.018f, cap = StrokeCap.Round)
    drawLine(Color(0xFFE57373).copy(alpha = 0.7f), Offset(cx + w * 0.22f, blushY - h * 0.02f), Offset(cx + w * 0.24f, blushY + h * 0.02f), strokeWidth = w * 0.018f, cap = StrokeCap.Round)
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

    if (style == CarBadgeStyle.CAPY) {
        clipPath(squirclePath) {
            drawCapyHappyCar(center, width, height, left, top, squirclePath)
        }
    } else if (style == CarBadgeStyle.CINEMATIC) {
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

    if (style == CarBadgeStyle.CAPY) {
        clipPath(containerPath) {
            drawCapyCoolGlassesCar(center, radius, containerPath)
        }
    } else if (style == CarBadgeStyle.CINEMATIC) {
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

    if (style == CarBadgeStyle.CAPY) {
        clipPath(pebblePath) {
            drawCapyTintedWindowSUV(center, width, height, pebblePath)
        }
    } else if (style == CarBadgeStyle.CINEMATIC) {
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

    if (style == CarBadgeStyle.CAPY) {
        clipPath(cloverPath) {
            drawCapyDriftingCar(center, radius, cloverPath)
        }
    } else if (style == CarBadgeStyle.CINEMATIC) {
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

// =============================================================================
// CAPY CARS: CAPYBARAS DRIVING CARS
// =============================================================================

/**
 * 1. Top-Left: Happy, smiling capybara driving a bright red coupe with open window.
 */
private fun DrawScope.drawCapyHappyCar(
    center: Offset,
    width: Float,
    height: Float,
    left: Float,
    top: Float,
    squirclePath: Path
) {
    // Soft sage background
    drawPath(path = squirclePath, color = Color(0xFFB4C8A2))

    val cx = center.x
    val cy = center.y + height * 0.05f
    val w = width * 0.86f
    val h = height * 0.44f

    val wheelY = cy + h * 0.30f
    val frontWheelX = cx - w * 0.28f
    val rearWheelX = cx + w * 0.26f
    val wheelRadius = h * 0.22f

    // Ground line
    val roadY = wheelY + wheelRadius
    drawRoundRect(
        color = Color(0xFFA2B88F),
        topLeft = Offset(left + width * 0.06f, roadY),
        size = Size(width * 0.88f, height * 0.08f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Cheerful breeze / sparkle streaks
    drawLine(
        color = Color(0xFFFAF4EB).copy(alpha = 0.85f),
        start = Offset(cx - w * 0.45f, cy - h * 0.55f),
        end = Offset(cx + w * 0.40f, cy - h * 0.55f),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFFAF4EB).copy(alpha = 0.55f),
        start = Offset(cx - w * 0.35f, cy - h * 0.70f),
        end = Offset(cx - w * 0.10f, cy - h * 0.70f),
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )

    // Red Car Body - Lower Chassis
    val bodyPath = Path().apply {
        moveTo(cx - w * 0.48f, cy + h * 0.22f)
        lineTo(cx - w * 0.46f, cy)
        cubicTo(cx - w * 0.44f, cy - h * 0.15f, cx - w * 0.30f, cy - h * 0.18f, cx - w * 0.18f, cy - h * 0.18f)
        lineTo(cx + w * 0.25f, cy - h * 0.18f)
        cubicTo(cx + w * 0.38f, cy - h * 0.15f, cx + w * 0.46f, cy, cx + w * 0.48f, cy + h * 0.22f)
        lineTo(cx - w * 0.48f, cy + h * 0.22f)
        close()
    }
    drawPath(bodyPath, color = Color(0xFFE53935))

    // Cabrio Front Windshield (A-pillar in front of driver - roof behind removed for open-top cabrio)
    val windshieldPillar = Path().apply {
        moveTo(cx - w * 0.20f, cy - h * 0.18f)
        lineTo(cx - w * 0.06f, cy - h * 0.60f)
        lineTo(cx - w * 0.01f, cy - h * 0.58f)
        lineTo(cx - w * 0.14f, cy - h * 0.18f)
        close()
    }
    drawPath(windshieldPillar, color = Color(0xFFC62828))

    // Cabrio Windshield Glass (Light tint)
    val windshieldGlass = Path().apply {
        moveTo(cx - w * 0.18f, cy - h * 0.18f)
        lineTo(cx - w * 0.06f, cy - h * 0.58f)
        lineTo(cx - w * 0.02f, cy - h * 0.56f)
        lineTo(cx - w * 0.14f, cy - h * 0.18f)
        close()
    }
    drawPath(windshieldGlass, color = Color(0x55FAF4EB))

    // HAPPY & SERENE CAPYBARA DRIVER (Visible in Open-Air Cabrio)
    val capyX = cx + w * 0.04f
    val capyY = cy - h * 0.32f

    // Capybara Body / Shoulders (stout, warm golden brown)
    drawRoundRect(
        color = Color(0xFFA06535),
        topLeft = Offset(capyX - w * 0.10f, capyY + h * 0.08f),
        size = Size(w * 0.22f, h * 0.22f),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // Small cupped rodent ear placed at top-rear corner of skull
    val earPath = Path().apply {
        moveTo(capyX + w * 0.08f, capyY - h * 0.16f)
        cubicTo(capyX + w * 0.13f, capyY - h * 0.26f, capyX + w * 0.18f, capyY - h * 0.20f, capyX + w * 0.12f, capyY - h * 0.10f)
        close()
    }
    drawPath(earPath, color = Color(0xFF7A4A28))
    val earInner = Path().apply {
        moveTo(capyX + w * 0.09f, capyY - h * 0.16f)
        cubicTo(capyX + w * 0.12f, capyY - h * 0.23f, capyX + w * 0.15f, capyY - h * 0.19f, capyX + w * 0.11f, capyY - h * 0.12f)
        close()
    }
    drawPath(earInner, color = Color(0xFFF48FB1))

    // Capybara Head & Snout Profile (Signature flat top, long blocky snout facing forward-left)
    val capyHeadProfile = Path().apply {
        moveTo(capyX + w * 0.10f, capyY - h * 0.18f) // Back of flat head
        lineTo(capyX - w * 0.04f, capyY - h * 0.18f) // Flat top skull
        lineTo(capyX - w * 0.16f, capyY - h * 0.08f) // Long flat snout bridge
        lineTo(capyX - w * 0.17f, capyY + h * 0.06f) // Blunt vertical front nose pad
        cubicTo(capyX - w * 0.16f, capyY + h * 0.12f, capyX - w * 0.08f, capyY + h * 0.16f, capyX, capyY + h * 0.16f) // Deep chubby rodent jowl
        lineTo(capyX + w * 0.08f, capyY + h * 0.12f) // Throat into neck
        close()
    }
    drawPath(capyHeadProfile, color = Color(0xFFB87843))

    // Snout Muzzle Overlay
    val muzzleProfile = Path().apply {
        moveTo(capyX - w * 0.04f, capyY - h * 0.06f)
        lineTo(capyX - w * 0.16f, capyY - h * 0.08f)
        lineTo(capyX - w * 0.17f, capyY + h * 0.06f)
        cubicTo(capyX - w * 0.15f, capyY + h * 0.12f, capyX - w * 0.06f, capyY + h * 0.14f, capyX - w * 0.02f, capyY + h * 0.08f)
        close()
    }
    drawPath(muzzleProfile, color = Color(0xFF754522))

    // Dark Rodent Nose on blunt front tip
    drawRoundRect(
        color = Color(0xFF24140A),
        topLeft = Offset(capyX - w * 0.175f, capyY - h * 0.06f),
        size = Size(w * 0.035f, h * 0.07f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    // Nostril slit
    drawCircle(
        color = Color(0xFF110803),
        radius = h * 0.015f,
        center = Offset(capyX - w * 0.155f, capyY - h * 0.03f)
    )

    // Gentle smiling rodent lip line
    val lipLine = Path().apply {
        moveTo(capyX - w * 0.17f, capyY + h * 0.04f)
        quadraticTo(capyX - w * 0.10f, capyY + h * 0.08f, capyX - w * 0.05f, capyY + h * 0.04f)
    }
    drawPath(lipLine, color = Color(0xFF24140A), style = Stroke(width = 2.2f, cap = StrokeCap.Round))

    // Whisker dots on snout side
    drawCircle(Color(0xFF3E2210), h * 0.012f, Offset(capyX - w * 0.12f, capyY + h * 0.01f))
    drawCircle(Color(0xFF3E2210), h * 0.012f, Offset(capyX - w * 0.09f, capyY + h * 0.03f))
    drawCircle(Color(0xFF3E2210), h * 0.012f, Offset(capyX - w * 0.11f, capyY + h * 0.06f))

    // Serene / sleepy high-set almond eye
    val eyeYPos = capyY - h * 0.10f
    val eyeXPos = capyX + w * 0.01f
    val eyeArc = Path().apply {
        moveTo(eyeXPos - w * 0.035f, eyeYPos)
        quadraticTo(eyeXPos, eyeYPos - h * 0.04f, eyeXPos + w * 0.035f, eyeYPos)
    }
    drawPath(eyeArc, color = Color(0xFF24140A), style = Stroke(width = 2.4f, cap = StrokeCap.Round))
    drawCircle(Color(0xFF24140A), h * 0.016f, Offset(eyeXPos, eyeYPos + h * 0.005f))

    // Rosy blushing cheek
    drawCircle(
        color = Color(0xFFFF8A80).copy(alpha = 0.60f),
        radius = h * 0.045f,
        center = Offset(capyX - w * 0.03f, capyY + h * 0.04f)
    )

    // Cute Yuzu fruit on top of head!
    val carYuzuY = capyY - h * 0.23f
    val carYuzuX = capyX + w * 0.02f
    drawCircle(Color(0xFFFF9800), h * 0.055f, Offset(carYuzuX, carYuzuY))
    drawCircle(Color(0xFFFFB74D), h * 0.025f, Offset(carYuzuX - w * 0.01f, carYuzuY - h * 0.015f))
    val yuzuLeaf = Path().apply {
        moveTo(carYuzuX, carYuzuY - h * 0.05f)
        quadraticTo(carYuzuX + w * 0.035f, carYuzuY - h * 0.08f, carYuzuX + w * 0.045f, carYuzuY - h * 0.06f)
        quadraticTo(carYuzuX + w * 0.025f, carYuzuY - h * 0.04f, carYuzuX, carYuzuY - h * 0.05f)
    }
    drawPath(yuzuLeaf, color = Color(0xFF4CAF50))

    // Cute paws resting on steering wheel
    val steeringY = cy - h * 0.05f
    drawRoundRect(
        color = Color(0xFF212121),
        topLeft = Offset(cx - w * 0.16f, steeringY),
        size = Size(w * 0.10f, h * 0.14f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawCircle(
        color = Color(0xFFA06535),
        radius = h * 0.045f,
        center = Offset(cx - w * 0.12f, steeringY + h * 0.02f)
    )
    drawCircle(
        color = Color(0xFFA06535),
        radius = h * 0.045f,
        center = Offset(cx - w * 0.06f, steeringY + h * 0.03f)
    )

    // Door Sill & Window Sill
    drawRoundRect(
        color = Color(0xFFEF5350),
        topLeft = Offset(cx - w * 0.22f, cy - h * 0.04f),
        size = Size(w * 0.48f, h * 0.08f),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // Headlight & Taillight
    drawRoundRect(
        color = Color(0xFFFFF59D),
        topLeft = Offset(cx - w * 0.46f, cy + h * 0.02f),
        size = Size(w * 0.08f, h * 0.10f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = Color(0xFFB71C1C),
        topLeft = Offset(cx + w * 0.40f, cy + h * 0.02f),
        size = Size(w * 0.06f, h * 0.10f),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // Wheels
    listOf(frontWheelX, rearWheelX).forEach { wx ->
        drawCircle(color = Color(0xFF1E1E1E), radius = wheelRadius, center = Offset(wx, wheelY))
        drawCircle(color = Color(0xFFB0BEC5), radius = wheelRadius * 0.58f, center = Offset(wx, wheelY))
        drawCircle(color = Color(0xFF37474F), radius = wheelRadius * 0.25f, center = Offset(wx, wheelY))
    }
}

/**
 * 2. Bottom-Left: Cool & serious capybara wearing dark sunglasses in an exotic yellow supercar.
 */
private fun DrawScope.drawCapyCoolGlassesCar(
    center: Offset,
    radius: Float,
    containerPath: Path
) {
    // Pale sage background
    drawPath(containerPath, color = Color(0xFFBAC5B0))

    val cx = center.x
    val cy = center.y + radius * 0.06f
    val w = radius * 1.62f
    val h = radius * 0.72f

    val wheelY = cy + h * 0.32f
    val frontWheelX = cx - w * 0.28f
    val rearWheelX = cx + w * 0.28f
    val wheelRadius = h * 0.24f

    // Road strip
    val roadY = wheelY + wheelRadius
    drawRoundRect(
        color = Color(0xFFA5B29B),
        topLeft = Offset(center.x - radius * 0.88f, roadY),
        size = Size(radius * 1.76f, radius * 0.16f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Cool speed accent lines
    drawLine(
        color = Color(0xFFFAF4EB).copy(alpha = 0.8f),
        start = Offset(cx - w * 0.48f, cy - h * 0.50f),
        end = Offset(cx + w * 0.40f, cy - h * 0.50f),
        strokeWidth = 2.4f,
        cap = StrokeCap.Round
    )

    // Sleek Supercar Lower Chassis (Vibrant Yellow)
    val supercarBody = Path().apply {
        moveTo(cx - w * 0.48f, cy + h * 0.22f)
        lineTo(cx - w * 0.44f, cy)
        cubicTo(cx - w * 0.36f, cy - h * 0.12f, cx - w * 0.18f, cy - h * 0.16f, cx - w * 0.08f, cy - h * 0.16f)
        lineTo(cx + w * 0.24f, cy - h * 0.16f)
        cubicTo(cx + w * 0.38f, cy - h * 0.12f, cx + w * 0.46f, cy + h * 0.05f, cx + w * 0.48f, cy + h * 0.22f)
        lineTo(cx - w * 0.48f, cy + h * 0.22f)
        close()
    }
    drawPath(supercarBody, color = Color(0xFFFFD54F))

    // Low aerodynamic roofline
    val roofPath = Path().apply {
        moveTo(cx - w * 0.18f, cy - h * 0.16f)
        lineTo(cx - w * 0.06f, cy - h * 0.58f)
        cubicTo(cx + w * 0.02f, cy - h * 0.62f, cx + w * 0.18f, cy - h * 0.62f, cx + w * 0.22f, cy - h * 0.54f)
        lineTo(cx + w * 0.36f, cy - h * 0.16f)
        lineTo(cx + w * 0.28f, cy - h * 0.16f)
        lineTo(cx + w * 0.18f, cy - h * 0.50f)
        lineTo(cx - w * 0.02f, cy - h * 0.50f)
        lineTo(cx - w * 0.10f, cy - h * 0.16f)
        close()
    }
    drawPath(roofPath, color = Color(0xFFFFA000))

    // Cabin background
    drawRoundRect(
        color = Color(0xFF212121),
        topLeft = Offset(cx - w * 0.10f, cy - h * 0.48f),
        size = Size(w * 0.36f, h * 0.32f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // COOL & SERIOUS CAPYBARA DRIVER WITH SUNGLASSES
    val capyX = cx + w * 0.05f
    val capyY = cy - h * 0.30f

    // Body / Shoulders
    drawRoundRect(
        color = Color(0xFFA06535),
        topLeft = Offset(capyX - w * 0.10f, capyY + h * 0.06f),
        size = Size(w * 0.22f, h * 0.22f),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // Small cupped ear placed at rear top corner
    val coolEar = Path().apply {
        moveTo(capyX + w * 0.08f, capyY - h * 0.16f)
        cubicTo(capyX + w * 0.13f, capyY - h * 0.26f, capyX + w * 0.18f, capyY - h * 0.20f, capyX + w * 0.12f, capyY - h * 0.10f)
        close()
    }
    drawPath(coolEar, color = Color(0xFF7A4A28))
    val coolEarInner = Path().apply {
        moveTo(capyX + w * 0.09f, capyY - h * 0.16f)
        cubicTo(capyX + w * 0.12f, capyY - h * 0.23f, capyX + w * 0.15f, capyY - h * 0.19f, capyX + w * 0.11f, capyY - h * 0.12f)
        close()
    }
    drawPath(coolEarInner, color = Color(0xFFF48FB1))

    // Capybara Head Profile (Flat top, long blocky snout)
    val coolHeadProfile = Path().apply {
        moveTo(capyX + w * 0.10f, capyY - h * 0.18f) // Back flat top
        lineTo(capyX - w * 0.04f, capyY - h * 0.18f) // Flat skull
        lineTo(capyX - w * 0.16f, capyY - h * 0.08f) // Long flat snout bridge
        lineTo(capyX - w * 0.17f, capyY + h * 0.06f) // Blunt vertical nose pad
        cubicTo(capyX - w * 0.16f, capyY + h * 0.12f, capyX - w * 0.08f, capyY + h * 0.16f, capyX, capyY + h * 0.16f) // Deep jowl
        lineTo(capyX + w * 0.08f, capyY + h * 0.12f)
        close()
    }
    drawPath(coolHeadProfile, color = Color(0xFFB87843))

    // Snout Muzzle Overlay
    val coolMuzzle = Path().apply {
        moveTo(capyX - w * 0.04f, capyY - h * 0.06f)
        lineTo(capyX - w * 0.16f, capyY - h * 0.08f)
        lineTo(capyX - w * 0.17f, capyY + h * 0.06f)
        cubicTo(capyX - w * 0.15f, capyY + h * 0.12f, capyX - w * 0.06f, capyY + h * 0.14f, capyX - w * 0.02f, capyY + h * 0.08f)
        close()
    }
    drawPath(coolMuzzle, color = Color(0xFF754522))

    // Dark Rodent Nose on blunt front tip
    drawRoundRect(
        color = Color(0xFF24140A),
        topLeft = Offset(capyX - w * 0.175f, capyY - h * 0.06f),
        size = Size(w * 0.035f, h * 0.07f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawCircle(Color(0xFF110803), h * 0.015f, Offset(capyX - w * 0.155f, capyY - h * 0.03f))

    // Cool straight mouth line & whisker dots
    drawLine(
        color = Color(0xFF24140A),
        start = Offset(capyX - w * 0.16f, capyY + h * 0.05f),
        end = Offset(capyX - w * 0.06f, capyY + h * 0.05f),
        strokeWidth = 2.2f,
        cap = StrokeCap.Round
    )
    drawCircle(Color(0xFF3E2210), h * 0.012f, Offset(capyX - w * 0.12f, capyY + h * 0.01f))
    drawCircle(Color(0xFF3E2210), h * 0.012f, Offset(capyX - w * 0.09f, capyY + h * 0.03f))

    // COOL DARK SUNGLASSES / SHADES (Fitted over high-set capybara eye plane)
    drawRoundRect(
        color = Color(0xFF111111),
        topLeft = Offset(capyX - w * 0.06f, capyY - h * 0.14f),
        size = Size(w * 0.08f, h * 0.09f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawRoundRect(
        color = Color(0xFF111111),
        topLeft = Offset(capyX + w * 0.03f, capyY - h * 0.14f),
        size = Size(w * 0.07f, h * 0.09f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    // Sunglasses Bridge & Temple arm
    drawLine(
        color = Color(0xFF111111),
        start = Offset(capyX + w * 0.02f, capyY - h * 0.11f),
        end = Offset(capyX + w * 0.03f, capyY - h * 0.11f),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF111111),
        start = Offset(capyX + w * 0.09f, capyY - h * 0.12f),
        end = Offset(capyX + w * 0.14f, capyY - h * 0.14f),
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )
    // Glossy white lens glint reflection streaks
    drawLine(
        color = Color(0xFFFFFFFF).copy(alpha = 0.9f),
        start = Offset(capyX - w * 0.045f, capyY - h * 0.13f),
        end = Offset(capyX - w * 0.015f, capyY - h * 0.07f),
        strokeWidth = 1.6f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFFFFFFF).copy(alpha = 0.9f),
        start = Offset(capyX + w * 0.045f, capyY - h * 0.13f),
        end = Offset(capyX + w * 0.075f, capyY - h * 0.07f),
        strokeWidth = 1.6f,
        cap = StrokeCap.Round
    )

    // Cool paw resting nonchalantly on the door sill
    drawRoundRect(
        color = Color(0xFFFBC02D),
        topLeft = Offset(cx - w * 0.18f, cy - h * 0.02f),
        size = Size(w * 0.44f, h * 0.07f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawCircle(
        color = Color(0xFF8D5524),
        radius = h * 0.04f,
        center = Offset(cx - w * 0.10f, cy - h * 0.02f)
    )

    // Supercar Front Headlight (Unlit, off, no beam)
    val supercarHeadlight = Path().apply {
        moveTo(cx - w * 0.46f, cy + h * 0.02f)
        lineTo(cx - w * 0.43f, cy - h * 0.02f)
        lineTo(cx - w * 0.33f, cy - h * 0.10f)
        lineTo(cx - w * 0.35f, cy - h * 0.03f)
        close()
    }
    drawPath(supercarHeadlight, color = Color(0xFFECEFF1))
    drawPath(supercarHeadlight, color = Color(0xFFE65100), style = Stroke(width = 1.4f))
    drawCircle(
        color = Color(0xFF78909C),
        radius = h * 0.026f,
        center = Offset(cx - w * 0.38f, cy - h * 0.04f)
    )
    drawCircle(
        color = Color(0xFF37474F),
        radius = h * 0.014f,
        center = Offset(cx - w * 0.38f, cy - h * 0.04f)
    )

    // Wheels
    listOf(frontWheelX, rearWheelX).forEach { wx ->
        drawCircle(color = Color(0xFF1C1C1C), radius = wheelRadius, center = Offset(wx, wheelY))
        drawCircle(color = Color(0xFFCFD8DC), radius = wheelRadius * 0.58f, center = Offset(wx, wheelY))
        drawCircle(color = Color(0xFF37474F), radius = wheelRadius * 0.24f, center = Offset(wx, wheelY))
    }
}

/**
 * 3. Top-Right: Capybara visible through tinted windows of a luxury green SUV.
 */
private fun DrawScope.drawCapyTintedWindowSUV(
    center: Offset,
    width: Float,
    height: Float,
    pebblePath: Path
) {
    // Soft moss background
    drawPath(pebblePath, color = Color(0xFFC2D4B2))

    val cx = center.x
    val cy = center.y + height * 0.04f
    val w = width * 0.88f
    val h = height * 0.46f

    val wheelY = cy + h * 0.30f
    val frontWheelX = cx - w * 0.28f
    val rearWheelX = cx + w * 0.26f
    val wheelRadius = h * 0.22f

    // Road strip
    val roadY = wheelY + wheelRadius
    drawRoundRect(
        color = Color(0xFFAEBFA0),
        topLeft = Offset(center.x - width * 0.45f, roadY),
        size = Size(width * 0.90f, height * 0.08f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Upright Luxury SUV Body - Metallic Forest Green
    val suvBody = Path().apply {
        moveTo(cx - w * 0.46f, cy + h * 0.24f)
        lineTo(cx - w * 0.44f, cy - h * 0.05f)
        lineTo(cx - w * 0.28f, cy - h * 0.12f)
        lineTo(cx - w * 0.22f, cy - h * 0.58f)
        lineTo(cx + w * 0.36f, cy - h * 0.58f)
        lineTo(cx + w * 0.44f, cy - h * 0.05f)
        lineTo(cx + w * 0.46f, cy + h * 0.24f)
        close()
    }
    drawPath(suvBody, color = Color(0xFF2E4D3B))

    // SUV Front Headlight (Unlit, off, no beam)
    val suvHeadlight = Path().apply {
        moveTo(cx - w * 0.45f, cy + h * 0.02f)
        lineTo(cx - w * 0.43f, cy - h * 0.06f)
        lineTo(cx - w * 0.35f, cy - h * 0.09f)
        lineTo(cx - w * 0.36f, cy + h * 0.02f)
        close()
    }
    drawPath(suvHeadlight, color = Color(0xFFECEFF1))
    drawPath(suvHeadlight, color = Color(0xFF1B2B20), style = Stroke(width = 1.6f))
    drawCircle(
        color = Color(0xFF90A4AE),
        radius = h * 0.028f,
        center = Offset(cx - w * 0.40f, cy - h * 0.03f)
    )
    drawCircle(
        color = Color(0xFF455A64),
        radius = h * 0.015f,
        center = Offset(cx - w * 0.40f, cy - h * 0.03f)
    )

    // Roof rack rails
    drawLine(
        color = Color(0xFFB0BEC5),
        start = Offset(cx - w * 0.18f, cy - h * 0.62f),
        end = Offset(cx + w * 0.32f, cy - h * 0.62f),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round
    )

    // Cabin Interior Background
    val windowArea = Path().apply {
        moveTo(cx - w * 0.20f, cy - h * 0.10f)
        lineTo(cx - w * 0.16f, cy - h * 0.52f)
        lineTo(cx + w * 0.32f, cy - h * 0.52f)
        lineTo(cx + w * 0.36f, cy - h * 0.10f)
        close()
    }
    drawPath(windowArea, color = Color(0xFF1B2B20))

    // CAPYBARA INSIDE (Driving calmly)
    val capyX = cx + w * 0.02f
    val capyY = cy - h * 0.30f

    // Capybara body / shoulders
    drawRoundRect(
        color = Color(0xFFA06535),
        topLeft = Offset(capyX - w * 0.10f, capyY + h * 0.06f),
        size = Size(w * 0.22f, h * 0.22f),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // Small cupped ear placed at rear top corner
    val suvEar = Path().apply {
        moveTo(capyX + w * 0.07f, capyY - h * 0.15f)
        cubicTo(capyX + w * 0.12f, capyY - h * 0.24f, capyX + w * 0.16f, capyY - h * 0.19f, capyX + w * 0.11f, capyY - h * 0.10f)
        close()
    }
    drawPath(suvEar, color = Color(0xFF7A4A28))

    // Capybara Head Profile (Flat top, long blocky snout)
    val suvHeadProfile = Path().apply {
        moveTo(capyX + w * 0.09f, capyY - h * 0.16f) // Back of flat head
        lineTo(capyX - w * 0.03f, capyY - h * 0.16f) // Flat top skull
        lineTo(capyX - w * 0.14f, capyY - h * 0.07f) // Long flat snout bridge
        lineTo(capyX - w * 0.15f, capyY + h * 0.06f) // Blunt vertical nose pad
        cubicTo(capyX - w * 0.14f, capyY + h * 0.12f, capyX - w * 0.07f, capyY + h * 0.15f, capyX, capyY + h * 0.15f) // Deep jowl
        lineTo(capyX + w * 0.07f, capyY + h * 0.11f)
        close()
    }
    drawPath(suvHeadProfile, color = Color(0xFFB87843))

    // Snout Muzzle Overlay
    val suvMuzzle = Path().apply {
        moveTo(capyX - w * 0.03f, capyY - h * 0.05f)
        lineTo(capyX - w * 0.14f, capyY - h * 0.07f)
        lineTo(capyX - w * 0.15f, capyY + h * 0.06f)
        cubicTo(capyX - w * 0.13f, capyY + h * 0.11f, capyX - w * 0.05f, capyY + h * 0.13f, capyX - w * 0.01f, capyY + h * 0.07f)
        close()
    }
    drawPath(suvMuzzle, color = Color(0xFF754522))

    // Dark Rodent Nose & Nostril
    drawRoundRect(
        color = Color(0xFF24140A),
        topLeft = Offset(capyX - w * 0.155f, capyY - h * 0.05f),
        size = Size(w * 0.03f, h * 0.06f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawCircle(Color(0xFF110803), h * 0.012f, Offset(capyX - w * 0.14f, capyY - h * 0.025f))

    // Peaceful sleepy eye (high-set horizontal slit)
    val suvEyeX = capyX
    val suvEyeY = capyY - h * 0.09f
    drawLine(
        color = Color(0xFF24140A),
        start = Offset(suvEyeX - w * 0.025f, suvEyeY),
        end = Offset(suvEyeX + w * 0.025f, suvEyeY),
        strokeWidth = 2.2f,
        cap = StrokeCap.Round
    )

    // Gentle lip line & whisker dots
    drawLine(
        color = Color(0xFF24140A),
        start = Offset(capyX - w * 0.15f, capyY + h * 0.04f),
        end = Offset(capyX - w * 0.05f, capyY + h * 0.04f),
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )
    drawCircle(Color(0xFF3E2210), h * 0.010f, Offset(capyX - w * 0.11f, capyY + h * 0.01f))
    drawCircle(Color(0xFF3E2210), h * 0.010f, Offset(capyX - w * 0.08f, capyY + h * 0.03f))

    // Cute Yuzu on head in SUV!
    val suvYuzuY = capyY - h * 0.21f
    drawCircle(Color(0xFFFF9800), h * 0.045f, Offset(capyX + w * 0.01f, suvYuzuY))
    drawCircle(Color(0xFF4CAF50), h * 0.015f, Offset(capyX + w * 0.025f, suvYuzuY - h * 0.04f))

    // Steering wheel & paws
    drawCircle(color = Color(0xFF1E1E1E), radius = h * 0.07f, center = Offset(cx - w * 0.10f, cy - h * 0.04f))
    drawCircle(color = Color(0xFF9E6438), radius = h * 0.035f, center = Offset(cx - w * 0.10f, cy - h * 0.04f))

    // TINTED WINDOW GLASS OVERLAY (Deep smoked tint with visible silhouette / capybara inside)
    drawPath(windowArea, color = Color(0x99121E16))

    // Window Divider Pillar
    drawLine(
        color = Color(0xFF2E4D3B),
        start = Offset(cx + w * 0.08f, cy - h * 0.52f),
        end = Offset(cx + w * 0.08f, cy - h * 0.10f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )

    // Glossy Tinted Glass Reflection Streaks across the window
    drawLine(
        color = Color(0xFFFFFFFF).copy(alpha = 0.28f),
        start = Offset(cx - w * 0.14f, cy - h * 0.12f),
        end = Offset(cx + w * 0.04f, cy - h * 0.50f),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFFFFFFF).copy(alpha = 0.22f),
        start = Offset(cx + w * 0.12f, cy - h * 0.12f),
        end = Offset(cx + w * 0.28f, cy - h * 0.50f),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round
    )

    // Lower Door Sill & Handle
    drawRoundRect(
        color = Color(0xFF385E49),
        topLeft = Offset(cx - w * 0.26f, cy - h * 0.08f),
        size = Size(w * 0.62f, h * 0.08f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawRoundRect(
        color = Color(0xFFB0BEC5),
        topLeft = Offset(cx - w * 0.04f, cy - h * 0.05f),
        size = Size(w * 0.08f, h * 0.03f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Wheels
    listOf(frontWheelX, rearWheelX).forEach { wx ->
        drawCircle(color = Color(0xFF1E1E1E), radius = wheelRadius, center = Offset(wx, wheelY))
        drawCircle(color = Color(0xFF90A4AE), radius = wheelRadius * 0.58f, center = Offset(wx, wheelY))
        drawCircle(color = Color(0xFF263238), radius = wheelRadius * 0.25f, center = Offset(wx, wheelY))
    }
}

/**
 * 4. Bottom-Right: Capybara drifting a lowered drift car with ACTION LINES, TIRE SMOKE, and a VERY SERIOUS FACE!
 */
private fun DrawScope.drawCapyDriftingCar(
    center: Offset,
    radius: Float,
    cloverPath: Path
) {
    // Sage background
    drawPath(cloverPath, color = Color(0xFFB4C8A2))

    val cx = center.x + radius * 0.02f
    // Lowered stance: lowered body center line
    val cy = center.y + radius * 0.08f
    val w = radius * 1.58f
    val h = radius * 0.72f

    // Lowered suspension stance: wheels tucked into fenders
    val wheelY = cy + h * 0.24f
    val frontWheelX = cx - w * 0.28f
    val rearWheelX = cx + w * 0.26f
    val wheelRadius = h * 0.22f

    // Road with drift marks touching base of tires
    val roadY = wheelY + wheelRadius
    drawRoundRect(
        color = Color(0xFFA2B88F),
        topLeft = Offset(center.x - radius * 0.88f, roadY),
        size = Size(radius * 1.76f, radius * 0.16f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Skid mark on asphalt
    drawLine(
        color = Color(0xFF424242).copy(alpha = 0.55f),
        start = Offset(cx + w * 0.15f, roadY + 2f),
        end = Offset(center.x + radius * 0.85f, roadY + 2f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )

    // ACTION LINES / DRIFT SPEED STREAKS
    drawLine(
        color = Color(0xFFFAF4EB).copy(alpha = 0.90f),
        start = Offset(cx - w * 0.52f, cy - h * 0.65f),
        end = Offset(cx + w * 0.44f, cy - h * 0.65f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFFAF4EB).copy(alpha = 0.65f),
        start = Offset(cx - w * 0.46f, cy - h * 0.42f),
        end = Offset(cx - w * 0.16f, cy - h * 0.42f),
        strokeWidth = 2.4f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF283B1D).copy(alpha = 0.45f),
        start = Offset(cx + w * 0.22f, cy + h * 0.46f),
        end = Offset(cx + w * 0.52f, cy + h * 0.46f),
        strokeWidth = 2.8f,
        cap = StrokeCap.Round
    )

    // DRIFT TIRE SMOKE PUFFS (Erupting around rear wheel and under chassis)
    val smokeX = rearWheelX + w * 0.08f
    val smokeY = wheelY + h * 0.05f
    // Layered soft puffy smoke clouds
    drawCircle(color = Color(0x88E0E0E0), radius = h * 0.32f, center = Offset(smokeX, smokeY - h * 0.10f))
    drawCircle(color = Color(0xCCE8E8E8), radius = h * 0.26f, center = Offset(smokeX - w * 0.04f, smokeY))
    drawCircle(color = Color(0xF0FFFFFF), radius = h * 0.20f, center = Offset(smokeX + w * 0.04f, smokeY + h * 0.04f))
    drawCircle(color = Color(0xD9FFFFFF), radius = h * 0.16f, center = Offset(smokeX - w * 0.10f, smokeY + h * 0.08f))

    // High Downforce Rear Wing / Drift Spoiler
    val spoilerPath = Path().apply {
        moveTo(cx + w * 0.28f, cy - h * 0.18f)
        lineTo(cx + w * 0.32f, cy - h * 0.56f)
        lineTo(cx + w * 0.48f, cy - h * 0.56f)
        lineTo(cx + w * 0.44f, cy - h * 0.18f)
        close()
    }
    drawPath(spoilerPath, color = Color(0xFF0D47A1))

    // Blue Tuner Drift Car Body (Lowered, low ground clearance drift chassis)
    val driftBody = Path().apply {
        moveTo(cx - w * 0.48f, cy + h * 0.20f)
        lineTo(cx - w * 0.44f, cy - h * 0.02f)
        cubicTo(cx - w * 0.40f, cy - h * 0.14f, cx - w * 0.24f, cy - h * 0.16f, cx - w * 0.14f, cy - h * 0.16f)
        lineTo(cx + w * 0.24f, cy - h * 0.16f)
        cubicTo(cx + w * 0.38f, cy - h * 0.10f, cx + w * 0.46f, cy + h * 0.06f, cx + w * 0.48f, cy + h * 0.20f)
        lineTo(cx - w * 0.48f, cy + h * 0.20f)
        close()
    }
    drawPath(driftBody, color = Color(0xFF1976D2))

    // Drift Front Headlight (Unlit, off, no beam)
    val driftHeadlight = Path().apply {
        moveTo(cx - w * 0.46f, cy + h * 0.02f)
        lineTo(cx - w * 0.43f, cy - h * 0.03f)
        lineTo(cx - w * 0.34f, cy - h * 0.11f)
        lineTo(cx - w * 0.36f, cy - h * 0.02f)
        close()
    }
    drawPath(driftHeadlight, color = Color(0xFFECEFF1))
    drawPath(driftHeadlight, color = Color(0xFF0D47A1), style = Stroke(width = 1.6f))
    drawCircle(
        color = Color(0xFF78909C),
        radius = h * 0.026f,
        center = Offset(cx - w * 0.39f, cy - h * 0.04f)
    )
    drawCircle(
        color = Color(0xFF37474F),
        radius = h * 0.014f,
        center = Offset(cx - w * 0.39f, cy - h * 0.04f)
    )

    // Roof & Pillar
    val driftRoof = Path().apply {
        moveTo(cx - w * 0.16f, cy - h * 0.16f)
        lineTo(cx - w * 0.04f, cy - h * 0.60f)
        cubicTo(cx + w * 0.04f, cy - h * 0.64f, cx + w * 0.18f, cy - h * 0.64f, cx + w * 0.22f, cy - h * 0.56f)
        lineTo(cx + w * 0.34f, cy - h * 0.16f)
        lineTo(cx + w * 0.26f, cy - h * 0.16f)
        lineTo(cx + w * 0.18f, cy - h * 0.52f)
        lineTo(cx, cy - h * 0.52f)
        lineTo(cx - w * 0.08f, cy - h * 0.16f)
        close()
    }
    drawPath(driftRoof, color = Color(0xFF0D47A1))

    // Cabin interior
    drawRoundRect(
        color = Color(0xFF1A1A1A),
        topLeft = Offset(cx - w * 0.08f, cy - h * 0.50f),
        size = Size(w * 0.34f, h * 0.34f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // CAPYBARA DRIFT DRIVER WITH VERY SERIOUS / INTENSE FACE
    val capyX = cx + w * 0.06f
    val capyY = cy - h * 0.32f

    // Torso / racing posture leaning into drift
    drawRoundRect(
        color = Color(0xFFA06535),
        topLeft = Offset(capyX - w * 0.10f, capyY + h * 0.06f),
        size = Size(w * 0.22f, h * 0.22f),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // Small cupped ear swept back aerodynamically
    val driftEar = Path().apply {
        moveTo(capyX + w * 0.08f, capyY - h * 0.14f)
        cubicTo(capyX + w * 0.14f, capyY - h * 0.22f, capyX + w * 0.18f, capyY - h * 0.16f, capyX + w * 0.11f, capyY - h * 0.08f)
        close()
    }
    drawPath(driftEar, color = Color(0xFF7A4A28))
    val driftEarInner = Path().apply {
        moveTo(capyX + w * 0.09f, capyY - h * 0.14f)
        cubicTo(capyX + w * 0.13f, capyY - h * 0.20f, capyX + w * 0.16f, capyY - h * 0.16f, capyX + w * 0.11f, capyY - h * 0.10f)
        close()
    }
    drawPath(driftEarInner, color = Color(0xFFF48FB1))

    // Capybara Head Profile (Flat top, long blocky snout)
    val driftHeadProfile = Path().apply {
        moveTo(capyX + w * 0.10f, capyY - h * 0.18f) // Back of skull
        lineTo(capyX - w * 0.04f, capyY - h * 0.18f) // Flat top head
        lineTo(capyX - w * 0.16f, capyY - h * 0.08f) // Long flat snout bridge
        lineTo(capyX - w * 0.17f, capyY + h * 0.06f) // Blunt front nose pad
        cubicTo(capyX - w * 0.16f, capyY + h * 0.12f, capyX - w * 0.08f, capyY + h * 0.16f, capyX, capyY + h * 0.16f) // Deep jowl
        lineTo(capyX + w * 0.08f, capyY + h * 0.12f)
        close()
    }
    drawPath(driftHeadProfile, color = Color(0xFFB87843))

    // Snout Muzzle Overlay
    val driftMuzzle = Path().apply {
        moveTo(capyX - w * 0.04f, capyY - h * 0.06f)
        lineTo(capyX - w * 0.16f, capyY - h * 0.08f)
        lineTo(capyX - w * 0.17f, capyY + h * 0.06f)
        cubicTo(capyX - w * 0.15f, capyY + h * 0.12f, capyX - w * 0.06f, capyY + h * 0.14f, capyX - w * 0.02f, capyY + h * 0.08f)
        close()
    }
    drawPath(driftMuzzle, color = Color(0xFF754522))

    // Dark Rodent Nose on blunt front tip
    drawRoundRect(
        color = Color(0xFF24140A),
        topLeft = Offset(capyX - w * 0.175f, capyY - h * 0.06f),
        size = Size(w * 0.035f, h * 0.07f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawCircle(Color(0xFF110803), h * 0.015f, Offset(capyX - w * 0.155f, capyY - h * 0.03f))

    // VERY SERIOUS DETERMINED MOUTH LINE & whisker dots
    drawLine(
        color = Color(0xFF24140A),
        start = Offset(capyX - w * 0.16f, capyY + h * 0.06f),
        end = Offset(capyX - w * 0.04f, capyY + h * 0.06f),
        strokeWidth = 2.4f,
        cap = StrokeCap.Round
    )
    drawCircle(Color(0xFF3E2210), h * 0.012f, Offset(capyX - w * 0.12f, capyY + h * 0.01f))
    drawCircle(Color(0xFF3E2210), h * 0.012f, Offset(capyX - w * 0.09f, capyY + h * 0.03f))

    // INTENSE, VERY SERIOUS EYES & FURROWED BROW LINES
    // High-set sharp focused eyes
    drawCircle(color = Color(0xFF111111), radius = h * 0.032f, center = Offset(capyX - w * 0.02f, capyY - h * 0.11f))
    drawCircle(color = Color(0xFFFFFFFF), radius = h * 0.012f, center = Offset(capyX - w * 0.025f, capyY - h * 0.12f))
    // Left furrowed angry/intense eyebrow (\)
    drawLine(
        color = Color(0xFF26150A),
        start = Offset(capyX - w * 0.05f, capyY - h * 0.16f),
        end = Offset(capyX, capyY - h * 0.13f),
        strokeWidth = 2.4f,
        cap = StrokeCap.Round
    )

    // Right eye (focused into apex)
    drawCircle(color = Color(0xFF111111), radius = h * 0.030f, center = Offset(capyX + w * 0.05f, capyY - h * 0.11f))
    drawCircle(color = Color(0xFFFFFFFF), radius = h * 0.012f, center = Offset(capyX + w * 0.045f, capyY - h * 0.12f))
    // Right furrowed eyebrow (/)
    drawLine(
        color = Color(0xFF26150A),
        start = Offset(capyX + w * 0.07f, capyY - h * 0.16f),
        end = Offset(capyX + w * 0.03f, capyY - h * 0.13f),
        strokeWidth = 2.4f,
        cap = StrokeCap.Round
    )

    // Steering wheel & paws gripping firmly
    val driftSteerX = cx - w * 0.12f
    val driftSteerY = cy - h * 0.04f
    drawCircle(color = Color(0xFF212121), radius = h * 0.08f, center = Offset(driftSteerX, driftSteerY))
    drawCircle(color = Color(0xFF8D5524), radius = h * 0.045f, center = Offset(driftSteerX - w * 0.02f, driftSteerY - h * 0.02f))
    drawCircle(color = Color(0xFF8D5524), radius = h * 0.045f, center = Offset(driftSteerX + w * 0.03f, driftSteerY + h * 0.01f))

    // Car Door Sill
    drawRoundRect(
        color = Color(0xFF2196F3),
        topLeft = Offset(cx - w * 0.18f, cy - h * 0.02f),
        size = Size(w * 0.44f, h * 0.07f),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // Front Wheel (Perfect circular geometry with matching rim and center dot)
    drawCircle(color = Color(0xFF1E1E1E), radius = wheelRadius, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFF90A4AE), radius = wheelRadius * 0.58f, center = Offset(frontWheelX, wheelY))
    drawCircle(color = Color(0xFF37474F), radius = wheelRadius * 0.25f, center = Offset(frontWheelX, wheelY))

    // Spinning Rear Wheel (with matching rim & hub dot)
    drawCircle(color = Color(0xFF1E1E1E), radius = wheelRadius, center = Offset(rearWheelX, wheelY))
    drawCircle(color = Color(0xFF90A4AE), radius = wheelRadius * 0.58f, center = Offset(rearWheelX, wheelY))
    drawCircle(color = Color(0xFF37474F), radius = wheelRadius * 0.25f, center = Offset(rearWheelX, wheelY))
}

