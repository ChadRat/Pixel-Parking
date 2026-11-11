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
 * Minimalist geometric floor level indicator icons:
 * - "ground": Just a clean horizontal line
 * - "lvl1": Horizontal line with 1 dot above it
 * - "lvl2": Horizontal line with 2 dots above it
 * - "lvl3": Horizontal line with 3 dots above it
 * - "p1": Horizontal line with 1 dot below it
 * - "p2": Horizontal line with 2 dots below it
 * - "p3": Horizontal line with 3 dots below it
 * - "roof": Horizontal line with a peaked rooftop chevron (^) above it
 */
@Composable
fun FloorLevelIcon(
    floorId: String,
    tint: Color = MaterialTheme.colorScheme.onPrimary,
    modifier: Modifier = Modifier.size(24.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val stroke = (minDim * 0.10f).coerceIn(2.2f, 3.2f)
        val dotRadius = minDim * 0.085f

        val lineStartX = w * 0.16f
        val lineEndX = w * 0.84f

        when (floorId) {
            "none" -> {
                // No floor / none: an X icon
                val pad = minDim * 0.28f
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f - pad, h * 0.5f - pad),
                    end = Offset(w * 0.5f + pad, h * 0.5f + pad),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f + pad, h * 0.5f - pad),
                    end = Offset(w * 0.5f - pad, h * 0.5f + pad),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
            "ground" -> {
                // Ground level: just a line
                val lineY = h * 0.50f
                drawLine(
                    color = tint,
                    start = Offset(lineStartX, lineY),
                    end = Offset(lineEndX, lineY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
            "lvl1" -> {
                // First floor: a line and one dot above it
                val lineY = h * 0.60f
                val dotY = h * 0.30f
                drawLine(
                    color = tint,
                    start = Offset(lineStartX, lineY),
                    end = Offset(lineEndX, lineY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = tint,
                    radius = dotRadius,
                    center = Offset(w * 0.50f, dotY)
                )
            }
            "lvl2" -> {
                // Second floor: a line and two dots above it
                val lineY = h * 0.60f
                val dotY = h * 0.30f
                drawLine(
                    color = tint,
                    start = Offset(lineStartX, lineY),
                    end = Offset(lineEndX, lineY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = tint,
                    radius = dotRadius,
                    center = Offset(w * 0.38f, dotY)
                )
                drawCircle(
                    color = tint,
                    radius = dotRadius,
                    center = Offset(w * 0.62f, dotY)
                )
            }
            "lvl3" -> {
                // Third floor: a line and three dots above it
                val lineY = h * 0.60f
                val dotY = h * 0.30f
                drawLine(
                    color = tint,
                    start = Offset(lineStartX, lineY),
                    end = Offset(lineEndX, lineY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = tint,
                    radius = dotRadius,
                    center = Offset(w * 0.28f, dotY)
                )
                drawCircle(
                    color = tint,
                    radius = dotRadius,
                    center = Offset(w * 0.50f, dotY)
                )
                drawCircle(
                    color = tint,
                    radius = dotRadius,
                    center = Offset(w * 0.72f, dotY)
                )
            }
            "p1" -> {
                // Underground P1: a line and one dot below it
                val lineY = h * 0.40f
                val dotY = h * 0.70f
                drawLine(
                    color = tint,
                    start = Offset(lineStartX, lineY),
                    end = Offset(lineEndX, lineY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = tint,
                    radius = dotRadius,
                    center = Offset(w * 0.50f, dotY)
                )
            }
            "p2" -> {
                // Underground P2: a line and two dots below it
                val lineY = h * 0.40f
                val dotY = h * 0.70f
                drawLine(
                    color = tint,
                    start = Offset(lineStartX, lineY),
                    end = Offset(lineEndX, lineY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = tint,
                    radius = dotRadius,
                    center = Offset(w * 0.38f, dotY)
                )
                drawCircle(
                    color = tint,
                    radius = dotRadius,
                    center = Offset(w * 0.62f, dotY)
                )
            }
            "p3" -> {
                // Underground P3: a line and three dots below it
                val lineY = h * 0.40f
                val dotY = h * 0.70f
                drawLine(
                    color = tint,
                    start = Offset(lineStartX, lineY),
                    end = Offset(lineEndX, lineY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = tint,
                    radius = dotRadius,
                    center = Offset(w * 0.28f, dotY)
                )
                drawCircle(
                    color = tint,
                    radius = dotRadius,
                    center = Offset(w * 0.50f, dotY)
                )
                drawCircle(
                    color = tint,
                    radius = dotRadius,
                    center = Offset(w * 0.72f, dotY)
                )
            }
            "roof" -> {
                // Rooftop: horizontal baseline with a peaked rooftop chevron (^) above it
                val lineY = h * 0.62f
                drawLine(
                    color = tint,
                    start = Offset(lineStartX, lineY),
                    end = Offset(lineEndX, lineY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                val roofPath = Path().apply {
                    moveTo(w * 0.26f, lineY - h * 0.10f)
                    lineTo(w * 0.50f, h * 0.22f)
                    lineTo(w * 0.74f, lineY - h * 0.10f)
                }
                drawPath(
                    path = roofPath,
                    color = tint,
                    style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            else -> {
                // Fallback: horizontal line
                val lineY = h * 0.50f
                drawLine(
                    color = tint,
                    start = Offset(lineStartX, lineY),
                    end = Offset(lineEndX, lineY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
