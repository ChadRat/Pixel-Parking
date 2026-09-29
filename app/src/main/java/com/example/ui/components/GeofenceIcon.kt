package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * A clean, modern vector-drawn icon resembling a Geofence:
 * - A circular perimeter boundary with dashes representing the virtual perimeter (fence).
 * - 4 subtle radial fence tick-marks at 12, 3, 6, 9 o'clock.
 * - Center location target dot / pinpoint representing the car inside the perimeter.
 */
@Composable
fun GeofenceIcon(
    modifier: Modifier = Modifier.size(16.dp),
    tint: Color = MaterialTheme.colorScheme.primary,
    fillAlpha: Float = 0.14f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = minOf(w, h) * 0.42f
        val strokeWidth = (minOf(w, h) * 0.08f).coerceAtLeast(1.2f.dp.toPx())

        // 1. Subtle translucent interior fill of the geofenced area
        if (fillAlpha > 0f) {
            drawCircle(
                color = tint.copy(alpha = fillAlpha),
                radius = radius,
                center = Offset(cx, cy)
            )
        }

        // 2. Circular boundary with dashes (representing the virtual perimeter / geofence)
        val dashLength = (radius * 0.45f).coerceAtLeast(3.dp.toPx())
        val dashGap = (radius * 0.32f).coerceAtLeast(2.dp.toPx())
        drawCircle(
            color = tint,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, dashGap), 0f)
            )
        )

        // 3. Small perimeter tick-marks / boundary posts at top, right, bottom, left
        val tickLength = radius * 0.22f
        val tickOffsetStart = radius - tickLength * 0.5f
        val tickOffsetEnd = radius + tickLength * 0.5f
        val tickStroke = strokeWidth * 0.9f

        // Top tick
        drawLine(
            color = tint,
            start = Offset(cx, cy - tickOffsetStart),
            end = Offset(cx, cy - tickOffsetEnd),
            strokeWidth = tickStroke,
            cap = StrokeCap.Round
        )
        // Bottom tick
        drawLine(
            color = tint,
            start = Offset(cx, cy + tickOffsetStart),
            end = Offset(cx, cy + tickOffsetEnd),
            strokeWidth = tickStroke,
            cap = StrokeCap.Round
        )
        // Left tick
        drawLine(
            color = tint,
            start = Offset(cx - tickOffsetStart, cy),
            end = Offset(cx - tickOffsetEnd, cy),
            strokeWidth = tickStroke,
            cap = StrokeCap.Round
        )
        // Right tick
        drawLine(
            color = tint,
            start = Offset(cx + tickOffsetStart, cy),
            end = Offset(cx + tickOffsetEnd, cy),
            strokeWidth = tickStroke,
            cap = StrokeCap.Round
        )

        // 4. Center pinpoint / target dot representing the vehicle inside the geofence
        val centerDotRadius = radius * 0.32f
        drawCircle(
            color = tint,
            radius = centerDotRadius,
            center = Offset(cx, cy)
        )
        // Inner bright accent dot
        drawCircle(
            color = Color.White,
            radius = centerDotRadius * 0.42f,
            center = Offset(cx, cy)
        )
    }
}

/**
 * Compact pill tag displaying the Geofence icon with an optional text label (e.g. "25m" or "Geofence").
 */
@Composable
fun GeofenceTagBadge(
    modifier: Modifier = Modifier,
    label: String = "25m",
    iconSize: Dp = 12.dp
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            GeofenceIcon(
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.primary,
                fillAlpha = 0.2f
            )
            if (label.isNotBlank()) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    lineHeight = 10.sp
                )
            }
        }
    }
}
