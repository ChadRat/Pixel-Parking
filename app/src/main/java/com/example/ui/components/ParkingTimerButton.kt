package com.example.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import java.util.Locale

// Heavy round font from Google Fonts (Fredoka)
val HeavyRoundFont = FontFamily(Font(R.font.fredoka, FontWeight.Bold))

/**
 * Blue button for Parking Timer:
 * - On Waypoint tab: Sits right above "Found Car" button with exact same height (48.dp) and shape (18.dp).
 * - On Home tab: Positioned above manual parking save button.
 * - Displays remaining time in heavy round font when set.
 */
@Composable
fun ParkingTimerButton(
    isRunning: Boolean,
    remainingSeconds: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFullWidth: Boolean = false
) {
    val hasTimerSet = isRunning || remainingSeconds > 0
    val formattedTime = if (hasTimerSet) {
        val totalMinutes = (remainingSeconds / 60).toInt()
        val secs = (remainingSeconds % 60).toInt()
        String.format(Locale.US, "%02d:%02d", totalMinutes, secs)
    } else {
        "Parking Timer"
    }

    val finalModifier = if (isFullWidth) {
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("parking_timer_blue_btn")
    } else {
        modifier
            .height(48.dp)
            .testTag("parking_timer_blue_btn")
    }

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1A73E8), // Vibrant blue
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = finalModifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Parking Timer",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            if (hasTimerSet) {
                Text(
                    text = formattedTime,
                    fontFamily = HeavyRoundFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "Parking Timer",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
