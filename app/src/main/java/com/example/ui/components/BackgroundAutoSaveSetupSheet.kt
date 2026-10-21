package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.ParkingViewModel

/**
 * Background Auto-Save Setup Pop-up modal sheet.
 *
 * Characteristics:
 * - Appears as a modal pop-up bottom sheet similar to the Bluetooth car window.
 * - Works in combination with MainActivity's background blur to blur the entire rest of the uncovered screen.
 * - Designed with the exact UI aesthetics, rows, squircles, and DevTimerStyleButtons of Developer Options.
 * - Supports live permission updates when returning from system settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundAutoSaveSetupSheet(
    viewModel: ParkingViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Diagnostic states
    var isBatteryOptimizationIgnored by remember { mutableStateOf(checkBatteryExemption(context)) }
    var hasBackgroundLocation by remember { mutableStateOf(checkBackgroundLocation(context)) }
    var hasFineLocation by remember { mutableStateOf(checkFineLocation(context)) }
    var hasBluetoothConnect by remember { mutableStateOf(checkBluetoothConnect(context)) }
    var isPersistentServiceEnabled by remember {
        mutableStateOf(com.example.service.AutoParkLocationService.isMonitoringEnabled(context))
    }

    // Live re-check on app resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBatteryOptimizationIgnored = checkBatteryExemption(context)
                hasBackgroundLocation = checkBackgroundLocation(context)
                hasFineLocation = checkFineLocation(context)
                hasBluetoothConnect = checkBluetoothConnect(context)
                isPersistentServiceEnabled = com.example.service.AutoParkLocationService.isMonitoringEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val fineLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasFineLocation = granted
        hasBackgroundLocation = checkBackgroundLocation(context)
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBackgroundLocation = granted
    }

    val bluetoothConnectLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBluetoothConnect = granted
    }

    val totalSteps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 3 else 2
    val grantedCount = (if (isBatteryOptimizationIgnored) 1 else 0) +
            (if (hasBackgroundLocation) 1 else 0) +
            (if (hasBluetoothConnect || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) 1 else 0)

    val isAllGranted = grantedCount >= totalSteps

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier.testTag("background_auto_save_setup_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
        ) {
            val smoothFling = com.example.util.rememberSmoothFlingBehavior()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                flingBehavior = smoothFling
            ) {
                // Top Hero Card (Styled identically to Developer Options top card)
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_dev_style_auto_save_header")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isAllGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isAllGranted) Icons.Default.CheckCircle else Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.backgroundDiagnosticTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isAllGranted) strings.backgroundSetupActiveDesc else strings.backgroundDiagnosticDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            // Status Pill
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isAllGranted) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isAllGranted) Icons.Default.CheckCircle else Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (isAllGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isAllGranted) strings.backgroundDiagnosticActive else "$grantedCount of $totalSteps permissions configured",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isAllGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Wavy Divider
                item {
                    WavyDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
                        waveLength = 34f,
                        amplitude = 6f,
                        thickness = 4f,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Section Title (Exact Developer Options styling)
                item {
                    Text(
                        text = "System Permissions & Background Optimization",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // 1. Unrestricted Battery Row (Developer Options row style)
                item {
                    DevStylePermissionRow(
                        title = strings.backgroundSetupStepBatteryTitle,
                        subtitle = strings.backgroundSetupStepBatteryDesc,
                        icon = Icons.Default.BatteryChargingFull,
                        isGranted = isBatteryOptimizationIgnored,
                        actionLabel = strings.backgroundSetupStatusAction,
                        onAction = { requestBatteryExemption(context) },
                        testTag = "row_dev_setup_battery"
                    )
                }

                // 2. Background Location Row
                item {
                    DevStylePermissionRow(
                        title = strings.backgroundSetupStepLocationTitle,
                        subtitle = strings.backgroundSetupStepLocationDesc,
                        icon = Icons.Default.LocationOn,
                        isGranted = hasBackgroundLocation,
                        actionLabel = strings.backgroundSetupStatusAction,
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                if (!hasFineLocation) {
                                    fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    try {
                                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                    } catch (_: Throwable) {
                                        openAppSettings(context)
                                    }
                                } else {
                                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                }
                            } else {
                                fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                        testTag = "row_dev_setup_location"
                    )
                }

                // 3. Nearby Devices (Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    item {
                        DevStylePermissionRow(
                            title = strings.backgroundSetupStepBluetoothTitle,
                            subtitle = strings.backgroundSetupStepBluetoothDesc,
                            icon = Icons.Default.Bluetooth,
                            isGranted = hasBluetoothConnect,
                            actionLabel = strings.backgroundSetupStatusAction,
                            onAction = { bluetoothConnectLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) },
                            testTag = "row_dev_setup_bluetooth"
                        )
                    }
                }

                // 4. Persistent Background Service Monitor Toggle (Developer Option Switch row)
                item {
                    DevStyleToggleRow(
                        title = strings.persistentMonitorTitle,
                        subtitle = strings.persistentMonitorDesc,
                        icon = Icons.Default.DirectionsCar,
                        checked = isPersistentServiceEnabled,
                        onCheckedChange = { checked ->
                            isPersistentServiceEnabled = checked
                            com.example.service.AutoParkLocationService.setMonitoringEnabled(context, checked)
                        },
                        testTag = "row_dev_setup_persistent_service"
                    )
                }

                // Wavy Divider
                item {
                    WavyDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
                        waveLength = 34f,
                        amplitude = 6f,
                        thickness = 4f,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Simulation & Actions Section (Styled with Developer Options DevTimerStyleButtons)
                item {
                    Text(
                        text = "Verification & Simulation",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                item {
                    DevTimerStyleButton(
                        text = "Test Car Disconnect Simulation",
                        icon = Icons.Default.DirectionsCar,
                        onClick = {
                            viewModel.testBluetoothParkingAutoSave()
                            Toast.makeText(
                                context,
                                "Simulated car disconnect: parking location automatically recorded!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        pillColor = MaterialTheme.colorScheme.secondaryContainer,
                        pillTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        testTag = "btn_dev_simulate_auto_save_test"
                    )
                }

                item {
                    DevTimerStyleButton(
                        text = "Done & Apply",
                        icon = Icons.Default.CheckCircle,
                        onClick = onDismiss,
                        pillColor = MaterialTheme.colorScheme.primary,
                        pillTextColor = MaterialTheme.colorScheme.onPrimary,
                        testTag = "btn_dev_close_setup_sheet"
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * Developer Options style Permission Row matching DeveloperOptionSimpleRow layout and squircles.
 */
@Composable
private fun DevStylePermissionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    testTag: String
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isGranted) {
                    Toast.makeText(context, "$title is configured and ready", Toast.LENGTH_SHORT).show()
                } else {
                    onAction()
                }
            }
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.CheckCircle else icon,
                        contentDescription = null,
                        tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (isGranted) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.height(34.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Ready",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HeavyRoundFont,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            Surface(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 1.dp,
                modifier = Modifier.height(36.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = actionLabel,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HeavyRoundFont,
                        color = MaterialTheme.colorScheme.onPrimary,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}

/**
 * Developer Options style Toggle Row matching DeveloperOptionSimpleRow.
 */
@Composable
private fun DevStyleToggleRow(
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
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

private fun checkBatteryExemption(context: Context): Boolean {
    return try {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    } catch (_: Throwable) {
        true
    }
}

private fun checkBackgroundLocation(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun checkFineLocation(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun checkBluetoothConnect(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun requestBatteryExemption(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    } catch (_: Throwable) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            openAppSettings(context)
        }
    }
}

private fun openAppSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Throwable) {}
}
