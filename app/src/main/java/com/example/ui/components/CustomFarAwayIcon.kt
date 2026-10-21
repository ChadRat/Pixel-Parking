package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Unique icon for custom far away parking spot entries.
 * Features an illustrated earth globe with latitude/longitude meridians matching the app's art style.
 */
@Composable
fun CustomFarAwayIcon(
    modifier: Modifier = Modifier.size(20.dp),
    tint: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = "Custom Far Away Spot"
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val cx = w / 2f
        val cy = h / 2f
        val radius = minDim * 0.42f
        val strokeWidth = (minDim * 0.08f).coerceIn(1.5f, 3.5f)

        // Outer globe circle perimeter
        drawCircle(
            color = tint,
            radius = radius,
            center = androidx.compose.ui.geometry.Offset(cx, cy),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Equator line
        val equatorPath = Path().apply {
            moveTo(cx - radius * 0.95f, cy)
            quadraticTo(cx, cy + radius * 0.25f, cx + radius * 0.95f, cy)
        }
        drawPath(
            path = equatorPath,
            color = tint,
            style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round)
        )

        // Prime meridian arc
        val meridianPath = Path().apply {
            moveTo(cx, cy - radius * 0.95f)
            quadraticTo(cx + radius * 0.35f, cy, cx, cy + radius * 0.95f)
        }
        drawPath(
            path = meridianPath,
            color = tint,
            style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round)
        )
    }
}
