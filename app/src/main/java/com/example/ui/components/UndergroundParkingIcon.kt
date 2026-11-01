package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Unique icon for underground learning detection & underground safeguard entries.
 * Features a subterranean garage representation:
 * - Upper street/ground level with diagonal ceiling hatchings
 * - Downward subterranean entrance ramp
 * - Parked car silhouette stationed securely below the ground layer
 */
@Composable
fun UndergroundParkingIcon(
    modifier: Modifier = Modifier.size(20.dp),
    tint: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = "Underground Detection Entry"
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val strokeThick = (minDim * 0.085f).coerceIn(1.8f, 3.8f)
        val strokeMed = (minDim * 0.065f).coerceIn(1.4f, 2.8f)

        // 1. Street level horizontal ground line
        val groundY = h * 0.30f
        drawLine(
            color = tint,
            start = Offset(w * 0.08f, groundY),
            end = Offset(w * 0.92f, groundY),
            strokeWidth = strokeThick,
            cap = StrokeCap.Round
        )

        // 2. Subterranean earth hatch marks above ground level
        val hatchOffsets = listOf(0.18f, 0.36f, 0.54f, 0.72f, 0.88f)
        for (hx in hatchOffsets) {
            drawLine(
                color = tint.copy(alpha = 0.75f),
                start = Offset(w * (hx - 0.07f), groundY - h * 0.16f),
                end = Offset(w * hx, groundY - h * 0.02f),
                strokeWidth = strokeMed,
                cap = StrokeCap.Round
            )
        }

        // 3. Underground ramp descending into subterranean floor
        val rampPath = Path().apply {
            moveTo(w * 0.15f, groundY + h * 0.02f)
            lineTo(w * 0.35f, h * 0.65f)
            lineTo(w * 0.44f, h * 0.65f)
        }
        drawPath(
            path = rampPath,
            color = tint.copy(alpha = 0.60f),
            style = Stroke(width = strokeMed, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 4. Downward entrance direction arrow
        val arrowPath = Path().apply {
            moveTo(w * 0.22f, groundY + h * 0.16f)
            lineTo(w * 0.28f, groundY + h * 0.32f)
            lineTo(w * 0.34f, groundY + h * 0.24f)
        }
        drawPath(
            path = arrowPath,
            color = tint,
            style = Stroke(width = strokeMed, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 5. Stylized subterranean parked car silhouette
        val carBody = Path().apply {
            // Front bumper
            moveTo(w * 0.46f, h * 0.82f)
            // Hood
            lineTo(w * 0.55f, h * 0.82f)
            // Windshield
            lineTo(w * 0.62f, h * 0.64f)
            // Roof
            lineTo(w * 0.77f, h * 0.64f)
            // Rear glass
            lineTo(w * 0.84f, h * 0.76f)
            // Trunk / rear
            lineTo(w * 0.90f, h * 0.76f)
            lineTo(w * 0.90f, h * 0.84f)
            lineTo(w * 0.46f, h * 0.84f)
            close()
        }
        drawPath(
            path = carBody,
            color = tint,
            style = Stroke(width = strokeMed, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Car wheels
        val wheelRadius = minDim * 0.065f
        drawCircle(
            color = tint,
            radius = wheelRadius,
            center = Offset(w * 0.56f, h * 0.85f)
        )
        drawCircle(
            color = tint,
            radius = wheelRadius,
            center = Offset(w * 0.80f, h * 0.85f)
        )
    }
}
