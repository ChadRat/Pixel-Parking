package com.example.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Expressive Material 3 icon container shapes used when the "Fun settings icons"
 * developer option is enabled, as well as in the Background Auto-Save Setup card.
 *
 * Ordered so consecutive entries belong to completely different geometric families
 * (flower, pinched clover, starburst gear, flat-edged pentagon, 8-point star,
 * arch shield, 10-lobe cookie, 4-point diamond quatrefoil).
 */
enum class FunSettingsIconShapeType {
    WAVY_FLOWER_6,
    CLOVER_4,
    STARBURST_12,
    ROUNDED_PENTAGON,
    OCTAGRAM_STAR_8,
    ROUNDED_SHIELD_ARCH,
    WAVY_COOKIE_10,
    DIAMOND_QUATREFOIL
}

/**
 * Returns the expressive [Shape] for a given sequential [index] so that two items
 * one above the other never share the same shape.
 */
fun getSettingsIconContainerShapeForIndex(
    enabled: Boolean,
    index: Int,
    defaultCornerDp: Int = 18
): Shape {
    if (!enabled) {
        return RoundedCornerShape(defaultCornerDp.dp)
    }
    val values = FunSettingsIconShapeType.entries
    val safeIndex = ((index % values.size) + values.size) % values.size
    return FunSettingsGeometricShape(values[safeIndex])
}

/**
 * Returns the expressive [Shape] when [enabled] is true, or the default [RoundedCornerShape]
 * when [enabled] is false.
 */
fun getSettingsIconContainerShape(
    enabled: Boolean,
    shapeType: FunSettingsIconShapeType,
    defaultCornerDp: Int = 18
): Shape {
    if (!enabled) {
        return RoundedCornerShape(defaultCornerDp.dp)
    }
    return FunSettingsGeometricShape(shapeType)
}

private class FunSettingsGeometricShape(
    private val shapeType: FunSettingsIconShapeType
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = buildFunSettingsShapePath(shapeType, size)
        return Outline.Generic(path)
    }
}

private fun buildFunSettingsShapePath(
    shapeType: FunSettingsIconShapeType,
    size: Size
): Path {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val minDim = min(size.width, size.height)
    val twoPi = (2.0 * PI).toFloat()
    val halfPi = (PI / 2.0).toFloat()

    return when (shapeType) {
        FunSettingsIconShapeType.WAVY_FLOWER_6 -> {
            // 6-lobed wavy scalloped flower ("Home and navigation" / "Συνδρομές")
            buildPolarModulatedPath(
                cx = cx,
                cy = cy,
                baseRadius = minDim * 0.45f,
                steps = 360
            ) { theta ->
                1f + 0.095f * cos(6f * (theta + halfPi)) - 0.012f * cos(12f * (theta + halfPi))
            }
        }

        FunSettingsIconShapeType.CLOVER_4 -> {
            // 4-lobed pinched pillow / clover with 4 diagonal corner lobes and concave sides ("Video feed")
            buildPolarModulatedPath(
                cx = cx,
                cy = cy,
                baseRadius = minDim * 0.45f,
                steps = 360
            ) { theta ->
                1f - 0.125f * cos(4f * theta) + 0.018f * cos(8f * theta)
            }
        }

        FunSettingsIconShapeType.STARBURST_12 -> {
            // 12-pointed scalloped seal / gear starburst ("Gestures and feedback")
            buildPolarModulatedPath(
                cx = cx,
                cy = cy,
                baseRadius = minDim * 0.435f,
                steps = 360
            ) { theta ->
                1f + 0.128f * cos(12f * (theta + halfPi)) - 0.022f * cos(24f * (theta + halfPi))
            }
        }

        FunSettingsIconShapeType.ROUNDED_PENTAGON -> {
            // Regular 5-sided pentagon with flat edges and rounded corners ("Display size")
            val r = minDim * 0.515f
            val vertices = List(5) { i ->
                val angle = -halfPi + i * (twoPi / 5f)
                Offset(
                    x = cx + r * cos(angle),
                    y = cy + r * sin(angle)
                )
            }
            buildRoundedPolygonPath(vertices, cornerFraction = 0.25f)
        }

        FunSettingsIconShapeType.OCTAGRAM_STAR_8 -> {
            // 8-pointed soft rounded star badge ("App icon")
            buildPolarModulatedPath(
                cx = cx,
                cy = cy,
                baseRadius = minDim * 0.44f,
                steps = 360
            ) { theta ->
                1f + 0.132f * cos(8f * (theta + halfPi)) - 0.022f * cos(16f * (theta + halfPi))
            }
        }

        FunSettingsIconShapeType.ROUNDED_SHIELD_ARCH -> {
            // 5-vertex arch/shield dome ("Περιεχόμενο και ροές")
            val r = minDim * 0.50f
            val vertices = listOf(
                Offset(cx, cy - r * 0.98f),              // Top peak
                Offset(cx + r * 0.88f, cy - r * 0.32f),  // Upper-right shoulder
                Offset(cx + r * 0.96f, cy + r * 0.68f),  // Lower-right base
                Offset(cx, cy + r * 0.96f),              // Bottom center
                Offset(cx - r * 0.96f, cy + r * 0.68f),  // Lower-left base
                Offset(cx - r * 0.88f, cy - r * 0.32f)   // Upper-left shoulder
            )
            buildRoundedPolygonPath(vertices, cornerFraction = 0.28f)
        }

        FunSettingsIconShapeType.WAVY_COOKIE_10 -> {
            // 10-lobed soft wavy scalloped cookie ("Theme and colors")
            buildPolarModulatedPath(
                cx = cx,
                cy = cy,
                baseRadius = minDim * 0.455f,
                steps = 360
            ) { theta ->
                1f + 0.075f * cos(10f * (theta + halfPi))
            }
        }

        FunSettingsIconShapeType.DIAMOND_QUATREFOIL -> {
            // 4-pointed rounded diamond / quatrefoil oriented vertically & horizontally
            buildPolarModulatedPath(
                cx = cx,
                cy = cy,
                baseRadius = minDim * 0.445f,
                steps = 360
            ) { theta ->
                1f + 0.125f * cos(4f * theta) - 0.018f * cos(8f * theta)
            }
        }
    }
}

private inline fun buildPolarModulatedPath(
    cx: Float,
    cy: Float,
    baseRadius: Float,
    steps: Int,
    modulation: (theta: Float) -> Float
): Path {
    val path = Path()
    val twoPi = (2.0 * PI).toFloat()
    for (i in 0 until steps) {
        val theta = i * (twoPi / steps)
        val r = baseRadius * modulation(theta)
        val x = cx + r * cos(theta)
        val y = cy + r * sin(theta)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun buildRoundedPolygonPath(
    vertices: List<Offset>,
    cornerFraction: Float
): Path {
    val path = Path()
    val n = vertices.size
    if (n < 3) return path

    val frac = cornerFraction.coerceIn(0.05f, 0.45f)
    for (i in 0 until n) {
        val prev = vertices[(i - 1 + n) % n]
        val curr = vertices[i]
        val next = vertices[(i + 1) % n]

        val toPrev = Offset(prev.x - curr.x, prev.y - curr.y)
        val toNext = Offset(next.x - curr.x, next.y - curr.y)
        val lenPrev = sqrt(toPrev.x * toPrev.x + toPrev.y * toPrev.y).coerceAtLeast(1f)
        val lenNext = sqrt(toNext.x * toNext.x + toNext.y * toNext.y).coerceAtLeast(1f)
        val cutDist = min(lenPrev, lenNext) * frac

        val startPt = Offset(
            curr.x + (toPrev.x / lenPrev) * cutDist,
            curr.y + (toPrev.y / lenPrev) * cutDist
        )
        val endPt = Offset(
            curr.x + (toNext.x / lenNext) * cutDist,
            curr.y + (toNext.y / lenNext) * cutDist
        )

        if (i == 0) {
            path.moveTo(startPt.x, startPt.y)
        } else {
            path.lineTo(startPt.x, startPt.y)
        }
        path.quadraticTo(curr.x, curr.y, endPt.x, endPt.y)
    }
    path.close()
    return path
}
