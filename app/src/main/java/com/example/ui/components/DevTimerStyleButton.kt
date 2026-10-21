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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Developer Options Action Button matching the EXACT styling and dimensions of
 * ParkHereTimerStyleButton, FoundCarTimerStyleButton, and ParkingTimerButton:
 * - Surface container (RoundedCornerShape(24.dp), height 48.dp, elevation 2.dp)
 * - 8-lobed scalloped cookie shape slowly spinning clockwise with vector icon inside
 * - Beside it, a pill with high-contrast text styled with HeavyRoundFont/bold
 */
@Composable
fun DevTimerStyleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.AutoAwesome,
    isFullWidth: Boolean = true,
    pillColor: Color = MaterialTheme.colorScheme.primary,
    pillTextColor: Color = MaterialTheme.colorScheme.onPrimary,
    badgeColor: Color? = null,
    badgeIconColor: Color? = null,
    testTag: String = "dev_timer_style_button"
) {
    val finalModifier = if (isFullWidth) {
        modifier
            .fillMaxWidth()
            .testTag(testTag)
    } else {
        modifier
            .testTag(testTag)
    }

    val actualBadgeColor = badgeColor ?: pillTextColor.copy(alpha = 0.22f)
    val actualIconColor = badgeIconColor ?: pillTextColor

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = pillColor,
        contentColor = pillTextColor,
        shadowElevation = 2.dp,
        modifier = finalModifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Rotating 8-lobed scalloped cookie badge with centered icon
            DevScallopActionBadge(
                icon = icon,
                badgeColor = actualBadgeColor,
                iconColor = actualIconColor,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = text,
                fontFamily = HeavyRoundFont,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = pillTextColor,
                letterSpacing = 0.3.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DevScallopActionBadge(
    icon: ImageVector,
    badgeColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dev_scallop_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Scalloped background spinning
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension / 2f * 0.88f
            val scallopAmplitude = size.minDimension * 0.08f
            val lobes = 8

            val path = Path()
            val totalPoints = 360
            for (i in 0 until totalPoints) {
                val theta = Math.toRadians(i.toDouble())
                val r = baseRadius + scallopAmplitude * cos(lobes * theta).toFloat()
                val px = (center.x + r * cos(theta)).toFloat()
                val py = (center.y + r * sin(theta)).toFloat()
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()

            drawPath(path = path, color = badgeColor)
        }

        // Fixed icon inside
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(17.dp)
        )
    }
}
