package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Signature expressive Squiggly Line Wavy Divider.
 */
@Composable
fun WavyDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    waveLength: Float = 36f,
    amplitude: Float = 8f,
    thickness: Float = 4.5f
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
    ) {
        val width = size.width
        val midY = size.height / 2f
        val path = Path().apply {
            moveTo(0f, midY)
            var x = 0f
            while (x < width) {
                val nextX = x + waveLength / 2f
                val ctrlX = x + waveLength / 4f
                val targetY = if ((x / (waveLength / 2f)).toInt() % 2 == 0) midY + amplitude else midY - amplitude
                quadraticTo(ctrlX, targetY, nextX, midY)
                x = nextX
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = thickness, cap = StrokeCap.Round)
        )
    }
}
