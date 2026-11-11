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
 * Waypoint / Locations Map navigation button matching the Park Here button styling:
 * - Surface container (RoundedCornerShape(24.dp), height 48.dp)
 * - Rotating badge shape ([MapBadgeShape.ROUNDED_SQUARE] for Google Maps, [MapBadgeShape.ROUNDED_PENTAGON] for Locations Map)
 *   with map icon fixed/still inside
 * - Beside it a pill with the button text styled identically to the Park Here / Found Car / Timer pill
 */
@Composable
fun WaypointMapsTimerStyleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFullWidth: Boolean = false,
    badgeShape: MapBadgeShape = MapBadgeShape.ROUNDED_SQUARE,
    testTag: String = "waypoint_maps_button"
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
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 4.dp,
        modifier = finalModifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpinningSquareMapIcon(
                containerColor = MaterialTheme.colorScheme.primary,
                iconColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(34.dp),
                animateSpin = true,
                badgeShape = badgeShape
            )

            // Pill with text matching timer / found car / park here pill
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
                    fontSize = if (text.length > 14) 12.5.sp else 14.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    letterSpacing = if (text.length > 14) 0.2.sp else 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
