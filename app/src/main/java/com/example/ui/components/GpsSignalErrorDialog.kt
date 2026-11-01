package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.GpsSignalErrorInfo
import com.example.ui.viewmodel.GpsSignalErrorType

/**
 * Material 3 AlertDialog informing the user when a location save fails
 * due to no GPS satellite signal or weak GPS accuracy.
 */
@Composable
fun GpsSignalErrorDialog(
    errorInfo: GpsSignalErrorInfo,
    onDismiss: () -> Unit,
    onTryAgain: () -> Unit,
    onSaveAnyway: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isNoSignal = errorInfo.type == GpsSignalErrorType.NO_SIGNAL

    val title = if (isNoSignal) strings.gpsNoSignalTitle else strings.gpsWeakSignalTitle
    val message = if (isNoSignal) {
        strings.gpsNoSignalMessage
    } else {
        String.format(strings.gpsWeakSignalMessage, errorInfo.accuracyMeters.toInt())
    }

    val iconVector = if (isNoSignal) Icons.Default.LocationOff else Icons.Default.GpsNotFixed
    val iconContainerColor = if (isNoSignal) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val iconContentColor = if (isNoSignal) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.testTag("gps_signal_error_dialog"),
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconContentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // If weak signal, offer the user the option to save anyway
                if (!isNoSignal) {
                    OutlinedButton(
                        onClick = onSaveAnyway,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .testTag("gps_error_save_anyway_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.gpsSignalSaveAnyway,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("gps_error_dismiss_btn")
                    ) {
                        Text(
                            text = strings.cancel,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onTryAgain,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 44.dp)
                            .testTag("gps_error_try_again_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.gpsSignalTryAgain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        dismissButton = null
    )
}
