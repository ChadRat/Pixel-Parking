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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class MapBadgeShape {
    ROUNDED_SQUARE,
    ROUNDED_PENTAGON
}

/**
 * Maps Icon with a rotating badge background:
 * - [MapBadgeShape.ROUNDED_SQUARE]: Square with rounded corners (used for the Google Maps button).
 * - [MapBadgeShape.ROUNDED_PENTAGON]: Pentagon with rounded edges matching the Check What's New button
 *   on the About menu (used for the Locations Map button).
 *
 * The badge slowly rotates one fourth of a full rotation (90°) clockwise, and then counter-clockwise
 * with a very smooth transition animation, while the map icon inside remains completely fixed / still.
 */
@Composable
fun SpinningSquareMapIcon(
    containerColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier.size(34.dp),
    animateSpin: Boolean = true,
    badgeShape: MapBadgeShape = MapBadgeShape.ROUNDED_SQUARE
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

    val pentagonPath = remember { Path() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = minOf(size.width, size.height)
            val center = Offset(size.width / 2f, size.height / 2f)

            rotate(degrees = rotationDeg, pivot = center) {
                when (badgeShape) {
                    MapBadgeShape.ROUNDED_SQUARE -> {
                        val side = minDim * 0.78f
                        val corner = side * 0.28f
                        drawRoundRect(
                            color = containerColor,
                            topLeft = Offset(center.x - side / 2f, center.y - side / 2f),
                            size = Size(side, side),
                            cornerRadius = CornerRadius(corner, corner),
                            style = Fill
                        )
                    }
                    MapBadgeShape.ROUNDED_PENTAGON -> {
                        val r = minDim * 0.50f
                        val sides = 5
                        val vertices = Array(sides) { i ->
                            val angle = -PI / 2.0 + i * (2.0 * PI / sides)
                            Offset(
                                x = center.x + (r * cos(angle)).toFloat(),
                                y = center.y + (r * sin(angle)).toFloat()
                            )
                        }
                        val cornerFraction = 0.26f
                        pentagonPath.reset()
                        for (i in 0 until sides) {
                            val prev = vertices[(i - 1 + sides) % sides]
                            val curr = vertices[i]
                            val next = vertices[(i + 1) % sides]

                            val startCorner = Offset(
                                x = curr.x + (prev.x - curr.x) * cornerFraction,
                                y = curr.y + (prev.y - curr.y) * cornerFraction
                            )
                            val endCorner = Offset(
                                x = curr.x + (next.x - curr.x) * cornerFraction,
                                y = curr.y + (next.y - curr.y) * cornerFraction
                            )

                            if (i == 0) {
                                pentagonPath.moveTo(startCorner.x, startCorner.y)
                            } else {
                                pentagonPath.lineTo(startCorner.x, startCorner.y)
                            }
                            pentagonPath.quadraticTo(curr.x, curr.y, endCorner.x, endCorner.y)
                        }
                        pentagonPath.close()

                        drawPath(
                            path = pentagonPath,
                            color = containerColor,
                            style = Fill
                        )
                    }
                }
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
