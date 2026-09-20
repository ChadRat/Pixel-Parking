package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.ParkingViewModel

@Composable
fun DeveloperOptionsScreen(
    viewModel: ParkingViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isMockGps by viewModel.isDevMockGpsEnabled.collectAsStateWithLifecycle()
    val isBtProximity by viewModel.isBtProximityEnabled.collectAsStateWithLifecycle()
    val isHapticDiag by viewModel.isDevHapticDiagnostics.collectAsStateWithLifecycle()
    val isParkingTimerEnabled by viewModel.isParkingTimerFeatureEnabled.collectAsStateWithLifecycle()
    val isGeofenceAutoPark by viewModel.isGeofenceAutoParkEnabled.collectAsStateWithLifecycle()
    val isCapyCarsEnabled by viewModel.isDevCapyCarsEnabled.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("developer_options_screen")
    ) {
        // Simple, clean header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("dev_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "Developer options",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            thickness = 0.5.dp
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            item {
                DeveloperOptionSimpleRow(
                    title = "Mock location",
                    subtitle = "Simulate GPS sensor location",
                    icon = Icons.Default.GpsFixed,
                    checked = isMockGps,
                    onCheckedChange = { viewModel.setDevMockGpsEnabled(it) },
                    testTag = "toggle_dev_mock_gps"
                )
            }

            item {
                DeveloperOptionSimpleRow(
                    title = strings.devGeofenceAutoParkTitle,
                    subtitle = strings.devGeofenceAutoParkSubtitle,
                    icon = Icons.Default.ShareLocation,
                    checked = isGeofenceAutoPark,
                    onCheckedChange = { viewModel.setGeofenceAutoParkEnabled(it) },
                    testTag = "toggle_dev_geofence_autopark"
                )
            }

            if (isGeofenceAutoPark) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.simulateGeofenceExit() },
                            modifier = Modifier.testTag("btn_simulate_geofence_exit"),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.devSimulateGeofenceExit,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            item {
                DeveloperOptionSimpleRow(
                    title = "Bluetooth proximity",
                    subtitle = "Show signal scanner on paired devices",
                    icon = Icons.Default.BluetoothSearching,
                    checked = isBtProximity,
                    onCheckedChange = { viewModel.setBtProximityEnabled(it) },
                    testTag = "toggle_dev_bt_proximity"
                )
            }

            item {
                DeveloperOptionSimpleRow(
                    title = "Haptic diagnostics",
                    subtitle = "Vibration telemetry and logging",
                    icon = Icons.Default.Vibration,
                    checked = isHapticDiag,
                    onCheckedChange = { viewModel.setDevHapticDiagnostics(it) },
                    testTag = "toggle_dev_haptic_diag"
                )
            }

            item {
                DeveloperOptionSimpleRow(
                    title = "Parking timer",
                    subtitle = "Enable meter countdown tracker",
                    icon = Icons.Default.Timer,
                    checked = isParkingTimerEnabled,
                    onCheckedChange = { viewModel.setParkingTimerFeatureEnabled(it) },
                    testTag = "toggle_dev_parking_timer"
                )
            }

            item {
                DeveloperOptionSimpleRow(
                    title = "Capy cars",
                    subtitle = "Enable capibara Cars as a third car style option",
                    icon = Icons.Default.Pets,
                    checked = isCapyCarsEnabled,
                    onCheckedChange = { viewModel.setDevCapyCarsEnabled(it) },
                    testTag = "toggle_dev_capy_cars"
                )
            }
        }
    }
}

@Composable
fun DeveloperOptionSimpleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun DeveloperOptionToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    DeveloperOptionSimpleRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        checked = checked,
        onCheckedChange = onCheckedChange,
        testTag = testTag
    )
}
