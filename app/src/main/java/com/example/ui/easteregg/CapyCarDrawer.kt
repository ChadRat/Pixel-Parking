package com.example.ui.easteregg

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom 2D side-scrolling profile renderer for the Capybara Cars in the universe game.
 * Renders the car chassis, animated spinning wheels, suspension physics, and the capybara driver on board.
 */
object CapyCarDrawer {

    fun drawCar(
        drawScope: DrawScope,
        carType: CapyCarType,
        carX: Float,
        carY: Float,
        carWidth: Float,
        carHeight: Float,
        wheelAngle: Float,
        suspensionBounce: Float,
        speedKmh: Float,
        isTurbo: Boolean
    ) {
        with(drawScope) {
            val cy = carY + suspensionBounce
            val cx = carX
            val w = carWidth
            val h = carHeight

            // Wheel geometry
            val wheelRadius = h * 0.23f
            val frontWheelX = cx + w * 0.28f
            val rearWheelX = cx - w * 0.28f
            val wheelY = cy + h * 0.32f

            // Shadow under car on road
            drawOval(
                color = Color.Black.copy(alpha = 0.28f),
                topLeft = Offset(cx - w * 0.44f, wheelY + wheelRadius * 0.72f),
                size = Size(w * 0.88f, wheelRadius * 0.48f)
            )

            // Exhaust effect behind rear of car
            drawExhaustEffect(
                rearWheelX = rearWheelX,
                rearY = cy + h * 0.24f,
                carWidth = w,
                speedKmh = speedKmh,
                isTurbo = isTurbo,
                carType = carType
            )

            // Draw specific car chassis & capybara driver
            when (carType) {
                CapyCarType.HAPPY_CABRIO -> drawHappyCabrio(cx, cy, w, h)
                CapyCarType.COOL_SUPERCAR -> drawCoolSupercar(cx, cy, w, h)
                CapyCarType.LUXURY_SUV -> drawLuxurySUV(cx, cy, w, h)
                CapyCarType.DRIFT_TUNER -> drawDriftTuner(cx, cy, w, h, speedKmh)
            }

            // Draw front and rear rotating wheels (wheels spin!)
            drawSpinningWheel(frontWheelX, wheelY, wheelRadius, wheelAngle, carType)
            drawSpinningWheel(rearWheelX, wheelY, wheelRadius, wheelAngle, carType)
        }
    }

    private fun DrawScope.drawHappyCabrio(cx: Float, cy: Float, w: Float, h: Float) {
        // Red Cabriolet / Convertible body
        val bodyColor = Color(0xFFE53935)
        val bodyDark = Color(0xFFC62828)
        val bodyLight = Color(0xFFEF5350)

        // Lower body
        val bodyPath = Path().apply {
            moveTo(cx - w * 0.44f, cy + h * 0.26f)
            lineTo(cx - w * 0.44f, cy + h * 0.04f)
            cubicTo(cx - w * 0.40f, cy - h * 0.08f, cx - w * 0.25f, cy - h * 0.10f, cx - w * 0.12f, cy - h * 0.10f)
            lineTo(cx + w * 0.20f, cy - h * 0.10f)
            cubicTo(cx + w * 0.32f, cy - h * 0.08f, cx + w * 0.42f, cy + h * 0.02f, cx + w * 0.45f, cy + h * 0.18f)
            lineTo(cx + w * 0.44f, cy + h * 0.26f)
            close()
        }
        drawPath(bodyPath, bodyColor)

        // Lower skirt / shadow line
        drawRoundRect(
            color = bodyDark,
            topLeft = Offset(cx - w * 0.42f, cy + h * 0.20f),
            size = Size(w * 0.84f, h * 0.08f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // Top body highlight
        drawRoundRect(
            color = bodyLight,
            topLeft = Offset(cx - w * 0.32f, cy - h * 0.08f),
            size = Size(w * 0.60f, h * 0.04f),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // Open Cabrio Cockpit Interior (Cream / Tan)
        drawRoundRect(
            color = Color(0xFFD7CCC8),
            topLeft = Offset(cx - w * 0.18f, cy - h * 0.14f),
            size = Size(w * 0.36f, h * 0.22f),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // CAPYBARA DRIVER: Happy & Smiling (Baby / Adult Capy with cheerful closed eyes)
        drawCapybaraDriver(
            headCenter = Offset(cx + w * 0.02f, cy - h * 0.18f),
            scale = h * 0.55f,
            hasSunglasses = false,
            hasYuzu = false,
            hasHeadband = false,
            isSmiling = true
        )

        // Front Windshield (Angled glass)
        val windshieldPath = Path().apply {
            moveTo(cx + w * 0.12f, cy - h * 0.10f)
            lineTo(cx + w * 0.18f, cy - h * 0.34f)
            lineTo(cx + w * 0.22f, cy - h * 0.34f)
            lineTo(cx + w * 0.20f, cy - h * 0.10f)
            close()
        }
        drawPath(windshieldPath, Color(0x90B3E5FC))
        drawPath(windshieldPath, Color(0xFFE0E0E0), style = Stroke(width = 2.5f))

        // Steering wheel
        drawLine(
            color = Color(0xFF3E2723),
            start = Offset(cx + w * 0.10f, cy - h * 0.06f),
            end = Offset(cx + w * 0.14f, cy - h * 0.20f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )

        // Headlight (front) & Taillight (rear)
        drawCircle(Color(0xFFFFF9C4), radius = h * 0.06f, center = Offset(cx + w * 0.42f, cy + h * 0.08f))
        drawCircle(Color.White, radius = h * 0.035f, center = Offset(cx + w * 0.42f, cy + h * 0.08f))
        drawRoundRect(
            color = Color(0xFFFF1744),
            topLeft = Offset(cx - w * 0.44f, cy + h * 0.04f),
            size = Size(w * 0.04f, h * 0.08f),
            cornerRadius = CornerRadius(2f, 2f)
        )
    }

    private fun DrawScope.drawCoolSupercar(cx: Float, cy: Float, w: Float, h: Float) {
        // Yellow exotic supercar wedge body
        val bodyColor = Color(0xFFFDD835)
        val bodyDark = Color(0xFFF9A825)
        val bodyLight = Color(0xFFFFF176)

        // Low aerodynamic wedge
        val bodyPath = Path().apply {
            moveTo(cx - w * 0.46f, cy + h * 0.25f)
            lineTo(cx - w * 0.45f, cy + h * 0.02f)
            lineTo(cx - w * 0.22f, cy - h * 0.06f)
            lineTo(cx - w * 0.08f, cy - h * 0.36f)
            cubicTo(cx + w * 0.04f, cy - h * 0.38f, cx + w * 0.16f, cy - h * 0.36f, cx + w * 0.22f, cy - h * 0.22f)
            lineTo(cx + w * 0.46f, cy + h * 0.15f)
            lineTo(cx + w * 0.46f, cy + h * 0.25f)
            close()
        }
        drawPath(bodyPath, bodyColor)

        // Side air intake scoop
        val scoopPath = Path().apply {
            moveTo(cx - w * 0.12f, cy + h * 0.05f)
            lineTo(cx - w * 0.04f, cy + h * 0.05f)
            lineTo(cx - w * 0.08f, cy + h * 0.18f)
            lineTo(cx - w * 0.18f, cy + h * 0.18f)
            close()
        }
        drawPath(scoopPath, Color(0xFF212121))

        // Dark cockpit canopy
        val glassPath = Path().apply {
            moveTo(cx - w * 0.06f, cy - h * 0.32f)
            lineTo(cx + w * 0.14f, cy - h * 0.32f)
            lineTo(cx + w * 0.28f, cy - h * 0.04f)
            lineTo(cx - w * 0.14f, cy - h * 0.04f)
            close()
        }
        drawPath(glassPath, Color(0xCC1A1A1A))
        drawPath(glassPath, bodyLight, style = Stroke(width = 2f))

        // CAPYBARA DRIVER: Cool Capy with Sunglasses
        drawCapybaraDriver(
            headCenter = Offset(cx + w * 0.04f, cy - h * 0.16f),
            scale = h * 0.50f,
            hasSunglasses = true,
            hasYuzu = false,
            hasHeadband = false,
            isSmiling = false
        )

        // Aggressive aerodynamic front splitter & rear diffuser
        drawRoundRect(
            color = Color(0xFF1E1E1E),
            topLeft = Offset(cx + w * 0.38f, cy + h * 0.22f),
            size = Size(w * 0.10f, h * 0.05f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawRoundRect(
            color = Color(0xFF1E1E1E),
            topLeft = Offset(cx - w * 0.47f, cy + h * 0.22f),
            size = Size(w * 0.10f, h * 0.05f),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // Sleek sharp LED headlights
        drawLine(
            color = Color(0xFFE0F7FA),
            start = Offset(cx + w * 0.38f, cy + h * 0.06f),
            end = Offset(cx + w * 0.44f, cy + h * 0.12f),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
    }

    private fun DrawScope.drawLuxurySUV(cx: Float, cy: Float, w: Float, h: Float) {
        // Deep Green Lifted Luxury SUV
        val bodyColor = Color(0xFF2E7D32)
        val bodyDark = Color(0xFF1B5E20)
        val bodyLight = Color(0xFF4CAF50)

        // Upright SUV Boxy Profile
        val bodyPath = Path().apply {
            moveTo(cx - w * 0.44f, cy + h * 0.26f)
            lineTo(cx - w * 0.44f, cy - h * 0.04f)
            lineTo(cx - w * 0.40f, cy - h * 0.38f)
            lineTo(cx + w * 0.14f, cy - h * 0.38f)
            lineTo(cx + w * 0.26f, cy - h * 0.08f)
            lineTo(cx + w * 0.44f, cy - h * 0.02f)
            lineTo(cx + w * 0.44f, cy + h * 0.26f)
            close()
        }
        drawPath(bodyPath, bodyColor)

        // Heavy-duty plastic wheel cladding & side guard
        drawRoundRect(
            color = Color(0xFF263238),
            topLeft = Offset(cx - w * 0.42f, cy + h * 0.18f),
            size = Size(w * 0.84f, h * 0.08f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // Silver Roof Rail
        drawLine(
            color = Color(0xFFB0BEC5),
            start = Offset(cx - w * 0.36f, cy - h * 0.42f),
            end = Offset(cx + w * 0.12f, cy - h * 0.42f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )

        // Windows (Front & Rear side windows)
        val rearWindow = Path().apply {
            moveTo(cx - w * 0.36f, cy - h * 0.08f)
            lineTo(cx - w * 0.36f, cy - h * 0.34f)
            lineTo(cx - w * 0.12f, cy - h * 0.34f)
            lineTo(cx - w * 0.12f, cy - h * 0.08f)
            close()
        }
        drawPath(rearWindow, Color(0xAA1C313A))

        val frontWindow = Path().apply {
            moveTo(cx - w * 0.08f, cy - h * 0.08f)
            lineTo(cx - w * 0.08f, cy - h * 0.34f)
            lineTo(cx + w * 0.12f, cy - h * 0.34f)
            lineTo(cx + w * 0.22f, cy - h * 0.08f)
            close()
        }
        drawPath(frontWindow, Color(0x77B0BEC5))

        // CAPYBARA DRIVER: Calm Capybara with Yuzu fruit on its head
        drawCapybaraDriver(
            headCenter = Offset(cx + w * 0.06f, cy - h * 0.16f),
            scale = h * 0.52f,
            hasSunglasses = false,
            hasYuzu = true,
            hasHeadband = false,
            isSmiling = false
        )

        // Big round off-road headlights
        drawCircle(Color(0xFFFFF9C4), radius = h * 0.065f, center = Offset(cx + w * 0.42f, cy + h * 0.04f))
        drawCircle(Color(0xFFECEFF1), radius = h * 0.040f, center = Offset(cx + w * 0.42f, cy + h * 0.04f))
    }

    private fun DrawScope.drawDriftTuner(cx: Float, cy: Float, w: Float, h: Float, speedKmh: Float) {
        // Electric Blue Tuner Drift Car with GT Spoiler & Aero Skirts
        val bodyColor = Color(0xFF1E88E5)
        val bodyDark = Color(0xFF0D47A1)
        val bodyLight = Color(0xFF42A5F5)

        // Rear Drift GT Wing / Spoiler
        val spoilerPath = Path().apply {
            moveTo(cx - w * 0.44f, cy - h * 0.12f)
            lineTo(cx - w * 0.40f, cy - h * 0.42f)
            lineTo(cx - w * 0.28f, cy - h * 0.42f)
            lineTo(cx - w * 0.32f, cy - h * 0.12f)
            close()
        }
        drawPath(spoilerPath, bodyDark)
        drawRoundRect(
            color = Color(0xFF212121),
            topLeft = Offset(cx - w * 0.46f, cy - h * 0.46f),
            size = Size(w * 0.20f, h * 0.06f),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // Sleek Coupe Drift Body
        val bodyPath = Path().apply {
            moveTo(cx - w * 0.45f, cy + h * 0.25f)
            lineTo(cx - w * 0.43f, cy + h * 0.02f)
            lineTo(cx - w * 0.18f, cy - h * 0.08f)
            lineTo(cx - w * 0.04f, cy - h * 0.36f)
            cubicTo(cx + w * 0.08f, cy - h * 0.38f, cx + w * 0.18f, cy - h * 0.36f, cx + w * 0.22f, cy - h * 0.22f)
            lineTo(cx + w * 0.44f, cy + h * 0.10f)
            lineTo(cx + w * 0.45f, cy + h * 0.25f)
            close()
        }
        drawPath(bodyPath, bodyColor)

        // Racing Stripe (White & Yellow)
        drawLine(
            color = Color.White,
            start = Offset(cx - w * 0.42f, cy + h * 0.05f),
            end = Offset(cx + w * 0.40f, cy + h * 0.05f),
            strokeWidth = 3f
        )

        // Cockpit window
        val glassPath = Path().apply {
            moveTo(cx - w * 0.02f, cy - h * 0.32f)
            lineTo(cx + w * 0.16f, cy - h * 0.32f)
            lineTo(cx + w * 0.24f, cy - h * 0.06f)
            lineTo(cx - w * 0.10f, cy - h * 0.06f)
            close()
        }
        drawPath(glassPath, Color(0xCC0D47A1))

        // CAPYBARA DRIVER: Intense racer capybara with red racing headband
        drawCapybaraDriver(
            headCenter = Offset(cx + w * 0.05f, cy - h * 0.16f),
            scale = h * 0.50f,
            hasSunglasses = false,
            hasYuzu = false,
            hasHeadband = true,
            isSmiling = false
        )

        // Lower drift aero skirt
        drawRoundRect(
            color = bodyDark,
            topLeft = Offset(cx - w * 0.44f, cy + h * 0.20f),
            size = Size(w * 0.88f, h * 0.06f),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // Twin JDM Projector Headlights
        drawCircle(Color(0xFFE0F7FA), radius = h * 0.045f, center = Offset(cx + w * 0.42f, cy + h * 0.08f))
        drawCircle(Color(0xFF80DEEA), radius = h * 0.035f, center = Offset(cx + w * 0.38f, cy + h * 0.06f))
    }

    /**
     * Draws the Capybara driver sitting inside the car.
     */
    private fun DrawScope.drawCapybaraDriver(
        headCenter: Offset,
        scale: Float,
        hasSunglasses: Boolean,
        hasYuzu: Boolean,
        hasHeadband: Boolean,
        isSmiling: Boolean
    ) {
        val hx = headCenter.x
        val hy = headCenter.y

        // Capybara Body / Shoulders (Warm brown fur)
        val furColor = Color(0xFFA06535)
        val furDark = Color(0xFF7A4A28)
        val furSnout = Color(0xFF5D361B)

        drawRoundRect(
            color = furColor,
            topLeft = Offset(hx - scale * 0.36f, hy + scale * 0.08f),
            size = Size(scale * 0.72f, scale * 0.42f),
            cornerRadius = CornerRadius(scale * 0.18f, scale * 0.18f)
        )

        // Cute Little Capybara Ear on rear
        val earPath = Path().apply {
            moveTo(hx - scale * 0.26f, hy - scale * 0.22f)
            cubicTo(
                hx - scale * 0.38f, hy - scale * 0.34f,
                hx - scale * 0.22f, hy - scale * 0.40f,
                hx - scale * 0.16f, hy - scale * 0.26f
            )
            close()
        }
        drawPath(earPath, furDark)
        drawCircle(Color(0xFFF48FB1), radius = scale * 0.05f, center = Offset(hx - scale * 0.24f, hy - scale * 0.28f))

        // Blocky Capybara Head (Classic wide snout profile)
        val headPath = Path().apply {
            moveTo(hx - scale * 0.24f, hy - scale * 0.24f)
            lineTo(hx + scale * 0.14f, hy - scale * 0.24f)
            cubicTo(
                hx + scale * 0.34f, hy - scale * 0.18f,
                hx + scale * 0.36f, hy + scale * 0.10f,
                hx + scale * 0.24f, hy + scale * 0.18f
            )
            lineTo(hx - scale * 0.22f, hy + scale * 0.18f)
            close()
        }
        drawPath(headPath, furColor)

        // Snout & Nose
        drawRoundRect(
            color = furSnout,
            topLeft = Offset(hx + scale * 0.20f, hy - scale * 0.04f),
            size = Size(scale * 0.16f, scale * 0.18f),
            cornerRadius = CornerRadius(scale * 0.06f, scale * 0.06f)
        )
        // Nostril dot
        drawCircle(Color.Black, radius = scale * 0.024f, center = Offset(hx + scale * 0.30f, hy + scale * 0.02f))

        // Eyes: Sunglasses VS Happy Smiling (^ _ ^) VS Calm Sleepy
        if (hasSunglasses) {
            // Cool Black Sunglasses with white reflection slit
            drawRoundRect(
                color = Color(0xFF1A1A1A),
                topLeft = Offset(hx - scale * 0.06f, hy - scale * 0.14f),
                size = Size(scale * 0.28f, scale * 0.13f),
                cornerRadius = CornerRadius(scale * 0.03f, scale * 0.03f)
            )
            // Lens white shine glint
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(hx + scale * 0.02f, hy - scale * 0.12f),
                end = Offset(hx + scale * 0.14f, hy - scale * 0.04f),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
            // Sunglasses bridge & arm
            drawLine(
                color = Color.Black,
                start = Offset(hx - scale * 0.06f, hy - scale * 0.08f),
                end = Offset(hx - scale * 0.18f, hy - scale * 0.12f),
                strokeWidth = 2f
            )
        } else if (isSmiling) {
            // Sweet happy closed curved eye arc (^_^)
            val smileEye = Path().apply {
                moveTo(hx + scale * 0.04f, hy - scale * 0.04f)
                cubicTo(
                    hx + scale * 0.08f, hy - scale * 0.14f,
                    hx + scale * 0.16f, hy - scale * 0.14f,
                    hx + scale * 0.20f, hy - scale * 0.04f
                )
            }
            drawPath(smileEye, Color(0xFF1B1B1B), style = Stroke(width = 3.5f, cap = StrokeCap.Round))
            // Sweet smile line at mouth
            val mouth = Path().apply {
                moveTo(hx + scale * 0.20f, hy + scale * 0.12f)
                cubicTo(
                    hx + scale * 0.24f, hy + scale * 0.16f,
                    hx + scale * 0.28f, hy + scale * 0.16f,
                    hx + scale * 0.30f, hy + scale * 0.11f
                )
            }
            drawPath(mouth, Color(0xFF261C14), style = Stroke(width = 2.5f, cap = StrokeCap.Round))
        } else {
            // Calm, sleepy horizontal capybara eye
            drawLine(
                color = Color(0xFF1B1B1B),
                start = Offset(hx + scale * 0.04f, hy - scale * 0.06f),
                end = Offset(hx + scale * 0.18f, hy - scale * 0.06f),
                strokeWidth = 3.2f,
                cap = StrokeCap.Round
            )
        }

        // Japanese Onsen Yuzu fruit on head (for Luxury SUV)
        if (hasYuzu) {
            val yx = hx - scale * 0.04f
            val yy = hy - scale * 0.32f
            drawCircle(Color(0xFFFFB300), radius = scale * 0.11f, center = Offset(yx, yy))
            drawCircle(Color(0xFFFFF176), radius = scale * 0.06f, center = Offset(yx - scale * 0.02f, yy - scale * 0.03f))
            // Green leaf on yuzu
            val leafPath = Path().apply {
                moveTo(yx, yy - scale * 0.10f)
                cubicTo(
                    yx + scale * 0.06f, yy - scale * 0.18f,
                    yx + scale * 0.12f, yy - scale * 0.14f,
                    yx + scale * 0.04f, yy - scale * 0.08f
                )
                close()
            }
            drawPath(leafPath, Color(0xFF4CAF50))
        }

        // Red Racing Headband with fluttering ribbon tails (for Drift Tuner)
        if (hasHeadband) {
            // Headband strap around brow
            drawLine(
                color = Color(0xFFE53935),
                start = Offset(hx - scale * 0.24f, hy - scale * 0.12f),
                end = Offset(hx + scale * 0.22f, hy - scale * 0.12f),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
            // Knot & fluttering tails blowing in wind behind capybara
            val ribbonTail = Path().apply {
                moveTo(hx - scale * 0.24f, hy - scale * 0.12f)
                cubicTo(
                    hx - scale * 0.40f, hy - scale * 0.18f,
                    hx - scale * 0.44f, hy - scale * 0.06f,
                    hx - scale * 0.52f, hy - scale * 0.14f
                )
            }
            drawPath(ribbonTail, Color(0xFFE53935), style = Stroke(width = 4f, cap = StrokeCap.Round))
        }

        // Capybara Paws holding the steering wheel
        drawCircle(furDark, radius = scale * 0.06f, center = Offset(hx + scale * 0.26f, hy + scale * 0.14f))
    }

    /**
     * Draws the spinning wheel with rotating spokes, rim, bolts, and tire treads.
     */
    private fun DrawScope.drawSpinningWheel(
        wx: Float,
        wy: Float,
        radius: Float,
        wheelAngle: Float,
        carType: CapyCarType
    ) {
        val center = Offset(wx, wy)

        // Outer black rubber tire
        drawCircle(color = Color(0xFF1E1E1E), radius = radius, center = center)

        // Rotating tire tread notches on outer perimeter
        rotate(degrees = wheelAngle, pivot = center) {
            val numTreads = 10
            for (i in 0 until numTreads) {
                val rad = Math.toRadians((i * (360f / numTreads)).toDouble())
                val tx = wx + cos(rad).toFloat() * (radius * 0.88f)
                val ty = wy + sin(rad).toFloat() * (radius * 0.88f)
                drawCircle(color = Color(0xFF37474F), radius = radius * 0.08f, center = Offset(tx, ty))
            }
        }

        // Inner rim lip
        val rimColor = carType.wheelColor
        drawCircle(color = rimColor, radius = radius * 0.68f, center = center)

        // Brake disc behind spokes
        drawCircle(color = Color(0xFF546E7A), radius = radius * 0.52f, center = center)

        // Rotating rim spokes
        rotate(degrees = wheelAngle, pivot = center) {
            val numSpokes = if (carType == CapyCarType.LUXURY_SUV) 5 else 6
            for (i in 0 until numSpokes) {
                val rad = Math.toRadians((i * (360f / numSpokes)).toDouble())
                val endX = wx + cos(rad).toFloat() * (radius * 0.62f)
                val endY = wy + sin(rad).toFloat() * (radius * 0.62f)
                drawLine(
                    color = Color.White.copy(alpha = 0.92f),
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = radius * 0.16f,
                    cap = StrokeCap.Round
                )
            }
            // Center hub nut
            drawCircle(color = Color(0xFF212121), radius = radius * 0.22f, center = center)
            drawCircle(color = Color(0xFFCFD8DC), radius = radius * 0.10f, center = center)
        }
    }

    /**
     * Dynamic exhaust trails: subtle smoke at cruise, billowing smoke for drift, turbo flames at edge speed!
     */
    private fun DrawScope.drawExhaustEffect(
        rearWheelX: Float,
        rearY: Float,
        carWidth: Float,
        speedKmh: Float,
        isTurbo: Boolean,
        carType: CapyCarType
    ) {
        val tailX = rearWheelX - carWidth * 0.14f

        if (isTurbo || speedKmh > 140f) {
            // Turbo Flame Burst (Blue core, orange/yellow tip)
            val flameLength = (speedKmh / 220f) * carWidth * 0.35f
            val flamePath = Path().apply {
                moveTo(tailX, rearY - 4f)
                lineTo(tailX - flameLength * 0.5f, rearY - 8f)
                lineTo(tailX - flameLength, rearY)
                lineTo(tailX - flameLength * 0.5f, rearY + 8f)
                lineTo(tailX, rearY + 4f)
                close()
            }
            drawPath(
                flamePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF00E5FF), Color(0xFFFF9100), Color(0xFFFF1744)),
                    startX = tailX,
                    endX = tailX - flameLength
                )
            )
        } else if (carType == CapyCarType.DRIFT_TUNER || speedKmh > 70f) {
            // Gentle white/grey tire smoke puff
            drawCircle(
                color = Color.White.copy(alpha = 0.40f),
                radius = 12f,
                center = Offset(tailX - 10f, rearY)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = 18f,
                center = Offset(tailX - 26f, rearY - 4f)
            )
        }
    }
}
