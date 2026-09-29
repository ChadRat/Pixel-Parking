package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Manual Parking Save button ("Park Here") with the exact same dimensions and styling
 * as the Found Car and Parking Timer buttons:
 * - Surface container (RoundedCornerShape(24.dp), height 48.dp)
 * - 8-lobed scalloped cookie shape slowly spinning clockwise, with map pin fixed/still inside
 * - Beside it a pill with the "Park Here" text styled identically to the timer/found car pill
 */
@Composable
fun ParkHereTimerStyleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFullWidth: Boolean = false,
    testTag: String = "manual_park_now_btn"
) {
    val finalModifier = if (isFullWidth) {
        modifier
            .fillMaxWidth()
            .testTag(testTag)
    } else {
        modifier
            .testTag(testTag)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
        shadowElevation = 2.dp,
        modifier = finalModifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 8-lobed scalloped cookie pin badge slowly spinning clockwise with fixed pin inside
            ScallopLocationPinIcon(
                containerColor = MaterialTheme.colorScheme.primary,
                pinColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(34.dp),
                animateSpin = true
            )

            // Pill with "Park Here" text matching timer / found car pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    fontFamily = HeavyRoundFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
