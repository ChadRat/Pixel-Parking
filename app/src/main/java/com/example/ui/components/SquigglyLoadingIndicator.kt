package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.PI
import kotlin.math.sin

/**
 * Expressive animated Squiggly Line Progress Indicator.
 * Renders an animated traveling sinusoidal wave with rounded caps and Material 3 coloring.
 */
@Composable
fun SquigglyLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    waveLength: Float = 36f,
    amplitude: Float = 8f,
    strokeWidth: Float = 5f,
    durationMs: Int = 1100
) {
    val infiniteTransition = rememberInfiniteTransition(label = "squiggly_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height((amplitude * 2.5f).dp.coerceAtLeast(18.dp))
    ) {
        val width = size.width
        val midY = size.height / 2f

        val path = Path()
        val step = 4f
        var first = true

        var x = 0f
        while (x <= width) {
            val angle = ((x / waveLength) * 2f * PI.toFloat()) + phase
            val y = midY + amplitude * sin(angle)
            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
            x += step
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

/**
 * Compact inline squiggly line indicator designed for buttons, chips, and headers.
 */
@Composable
fun SquigglyLoadingInline(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    width: Dp = 48.dp,
    height: Dp = 16.dp,
    waveLength: Float = 22f,
    amplitude: Float = 5f,
    strokeWidth: Float = 3.5f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "inline_squiggly")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inline_phase"
    )

    Canvas(
        modifier = modifier
            .size(width = width, height = height)
    ) {
        val path = Path()
        val midY = size.height / 2f
        val step = 3f
        var first = true

        var x = 0f
        while (x <= size.width) {
            val angle = ((x / waveLength) * 2f * PI.toFloat()) + phase
            val y = midY + amplitude * sin(angle)
            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
            x += step
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

/**
 * Polished, high-aesthetic Squiggly Line Loading Screen / Dialog overlay.
 * Features an animated spinning scalloped badge, multiple traveling squiggly lines,
 * and high-contrast typography.
 */
@Composable
fun SquigglyLoadingScreen(
    title: String = "Finding your car...",
    subtitle: String = "Acquiring precision location fix with satellite GPS",
    onDismissRequest: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = { onDismissRequest?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = onDismissRequest != null,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("squiggly_loading_screen_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top row with optional close button
                if (onDismissRequest != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Rotating 8-lobed scalloped badge icon
                ScallopLocationPinIcon(
                    containerColor = MaterialTheme.colorScheme.primary,
                    pinColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(68.dp),
                    animateSpin = true
                )

                // Title & Subtitle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // The signature expressive Squiggly Line
                SquigglyLoadingIndicator(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    waveLength = 32f,
                    amplitude = 9f,
                    strokeWidth = 6f
                )
            }
        }
    }
}
