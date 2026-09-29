package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Custom vector icon representing a baby feeding bottle (not an emoji).
 * Designed according to Material 3 icon aesthetics:
 * - Rounded silicone dome teat at top
 * - Screw collar / cap ring
 * - Contoured bottle body with volumetric measurement tick marks and milk level
 */
@Composable
fun BabyBottleIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = LocalContentColor.current
) {
    Canvas(modifier = modifier) {
        drawBabyBottle(tint = tint)
    }
}

fun DrawScope.drawBabyBottle(tint: Color) {
    val w = size.width
    val h = size.height
    val scale = minOf(w, h) / 24f
    val strokeWidth = (1.8f * scale).coerceAtLeast(1.2f)
    val cx = w / 2f

    // 1. Teat / Nipple (Soft rounded dome at top)
    val teatPath = Path().apply {
        moveTo(cx - 2.2f * scale, 5.0f * scale)
        cubicTo(
            cx - 2.2f * scale, 2.7f * scale,
            cx - 1.2f * scale, 1.6f * scale,
            cx, 1.6f * scale
        )
        cubicTo(
            cx + 1.2f * scale, 1.6f * scale,
            cx + 2.2f * scale, 2.7f * scale,
            cx + 2.2f * scale, 5.0f * scale
        )
        close()
    }
    drawPath(teatPath, color = tint.copy(alpha = 0.28f), style = Fill)
    drawPath(
        teatPath,
        color = tint,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // 2. Collar / Screw Cap (Horizontal ring)
    val collarLeft = cx - 4.2f * scale
    val collarTop = 5.0f * scale
    val collarWidth = 8.4f * scale
    val collarHeight = 2.7f * scale
    val collarRadius = 1.0f * scale

    val collarPath = Path().apply {
        addRoundRect(
            RoundRect(
                left = collarLeft,
                top = collarTop,
                right = collarLeft + collarWidth,
                bottom = collarTop + collarHeight,
                radiusX = collarRadius,
                radiusY = collarRadius
            )
        )
    }
    drawPath(collarPath, color = tint.copy(alpha = 0.40f), style = Fill)
    drawPath(
        collarPath,
        color = tint,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // 3. Bottle Body (Contoured with rounded shoulder and base)
    val neckY = collarTop + collarHeight
    val bodyLeft = cx - 4.4f * scale
    val bodyRight = cx + 4.4f * scale
    val bodyBottom = 22.0f * scale
    val shoulderY = 10.0f * scale
    val baseRadius = 2.2f * scale

    val bodyPath = Path().apply {
        moveTo(cx - 3.4f * scale, neckY)
        // Left shoulder curve
        cubicTo(
            cx - 3.8f * scale, neckY + 1.2f * scale,
            bodyLeft, shoulderY - 0.5f * scale,
            bodyLeft, shoulderY + 1.0f * scale
        )
        // Left side down to base
        lineTo(bodyLeft, bodyBottom - baseRadius)
        // Bottom left corner
        cubicTo(
            bodyLeft, bodyBottom,
            bodyLeft + baseRadius, bodyBottom,
            bodyLeft + baseRadius, bodyBottom
        )
        // Bottom edge
        lineTo(bodyRight - baseRadius, bodyBottom)
        // Bottom right corner
        cubicTo(
            bodyRight, bodyBottom,
            bodyRight, bodyBottom - baseRadius,
            bodyRight, bodyBottom - baseRadius
        )
        // Right side up to shoulder
        lineTo(bodyRight, shoulderY + 1.0f * scale)
        // Right shoulder curve
        cubicTo(
            bodyRight, shoulderY - 0.5f * scale,
            cx + 3.8f * scale, neckY + 1.2f * scale,
            cx + 3.4f * scale, neckY
        )
        close()
    }

    drawPath(bodyPath, color = tint.copy(alpha = 0.15f), style = Fill)
    drawPath(
        bodyPath,
        color = tint,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // 4. Liquid level line (horizontal meniscus)
    val liquidY = 11.2f * scale
    drawLine(
        color = tint.copy(alpha = 0.50f),
        start = Offset(bodyLeft + 1.2f * scale, liquidY),
        end = Offset(bodyRight - 1.2f * scale, liquidY),
        strokeWidth = strokeWidth * 0.8f,
        cap = StrokeCap.Round
    )

    // 5. Volume measurement graduation ticks (on the left side)
    val tickXStart = bodyLeft + 1.4f * scale
    // Tick 1 (top)
    drawLine(
        color = tint,
        start = Offset(tickXStart, 13.2f * scale),
        end = Offset(tickXStart + 2.4f * scale, 13.2f * scale),
        strokeWidth = strokeWidth * 0.9f,
        cap = StrokeCap.Round
    )
    // Tick 2 (middle - longer)
    drawLine(
        color = tint,
        start = Offset(tickXStart, 15.8f * scale),
        end = Offset(tickXStart + 3.6f * scale, 15.8f * scale),
        strokeWidth = strokeWidth * 0.9f,
        cap = StrokeCap.Round
    )
    // Tick 3 (bottom)
    drawLine(
        color = tint,
        start = Offset(tickXStart, 18.4f * scale),
        end = Offset(tickXStart + 2.4f * scale, 18.4f * scale),
        strokeWidth = strokeWidth * 0.9f,
        cap = StrokeCap.Round
    )
}
