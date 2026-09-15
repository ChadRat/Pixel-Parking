package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PixelOrange
import com.example.ui.theme.PixelRed
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun ParkingMeterWidget(
    meterExpiryTimestamp: Long?,
    parkedTimestamp: Long,
    onSetMeter: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val elapsedMinutes = ((currentTime - parkedTimestamp) / 60000L).coerceAtLeast(0)
    val elapsedHours = elapsedMinutes / 60
    val elapsedRemMins = elapsedMinutes % 60
    val elapsedText = if (elapsedHours > 0) "${elapsedHours}h ${elapsedRemMins}m" else "${elapsedRemMins}m"

    val remainingMs = (meterExpiryTimestamp ?: 0L) - currentTime
    val hasMeter = meterExpiryTimestamp != null && meterExpiryTimestamp > 0
    val isExpired = hasMeter && remainingMs <= 0

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .testTag("parking_meter_widget")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Timer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Column {
                        Text(
                            text = "PARKED DURATION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Parked for $elapsedText",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (hasMeter) {
                    val remSeconds = (remainingMs / 1000).coerceAtLeast(0)
                    val remMins = remSeconds / 60
                    val remSecs = remSeconds % 60
                    val timerString = String.format(Locale.US, "%02d:%02d", remMins, remSecs)

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isExpired) "METER EXPIRED" else "METER REMAINING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpired) PixelRed else PixelOrange
                        )
                        Text(
                            text = if (isExpired) "EXPIRED" else timerString,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpired) PixelRed else PixelOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Meter Presets:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(30 to "30m", 60 to "1h", 120 to "2h", 240 to "4h").forEach { (mins, label) ->
                    AssistChip(
                        onClick = { onSetMeter(mins) },
                        label = { Text(label, fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.testTag("meter_chip_$label")
                    )
                }
            }
        }
    }
}
