package com.example.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * Maps Icon with a rounded square badge background matching the timer / park here button design.
 *
 * The rounded square slowly rotates one fourth of a full rotation (90°) clockwise, and then counter-clockwise
 * with a very smooth transition animation, while the map icon inside remains completely fixed / still.
 */
@Composable
fun SpinningSquareMapIcon(
    containerColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier.size(34.dp),
    animateSpin: Boolean = true
) {
    val spinTransition = rememberInfiniteTransition(label = "spinning_square_map_transition")
    val rotationDeg by if (animateSpin) {
        spinTransition.animateFloat(
            initialValue = 0f,
            targetValue = 90f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 7500,
                    easing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "spinning_square_map_rotation"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = minOf(size.width, size.height)
            val center = Offset(size.width / 2f, size.height / 2f)

            rotate(degrees = rotationDeg, pivot = center) {
                val squareSize = minDim * 0.72f
                val cornerRadius = minDim * 0.24f
                drawRoundRect(
                    color = containerColor,
                    topLeft = Offset(center.x - squareSize / 2f, center.y - squareSize / 2f),
                    size = Size(squareSize, squareSize),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            }
        }

        Icon(
            imageVector = Icons.Default.Map,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(17.dp)
        )
    }
}
