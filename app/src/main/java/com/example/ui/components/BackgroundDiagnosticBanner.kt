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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.i18n.LocalAppStrings

/**
 * Pixel-styled Background Auto-Save Reliability & Setup card.
 *
 * Visually matches the rest of the application:
 * - 26.dp squircles and surfaceContainer layering
 * - Signature rotating scalloped badge with shield/bolt/checkmark
 * - Organic WavyDivider separator
 * - Step-by-step interactive tiles with clear visual feedback and TimerStyle pill action buttons
 * - Live lifecycle-aware diagnostic re-checking on resume from System Settings
 */
@Composable
fun BackgroundDiagnosticBanner(
    modifier: Modifier = Modifier,
    showWhenAllGranted: Boolean = false,
    dismissible: Boolean = true,
    onOpenSetupSheet: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isDismissed by rememberSaveable { mutableStateOf(false) }
    var isExpandedWhenAllGranted by rememberSaveable { mutableStateOf(false) }

    // Diagnostic states
    var isBatteryOptimizationIgnored by remember { mutableStateOf(checkBatteryExemption(context)) }
    var hasBackgroundLocation by remember { mutableStateOf(checkBackgroundLocation(context)) }
    var hasFineLocation by remember { mutableStateOf(checkFineLocation(context)) }
    var hasBluetoothConnect by remember { mutableStateOf(checkBluetoothConnect(context)) }

    // Re-check automatically whenever the user returns to the app from System Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBatteryOptimizationIgnored = checkBatteryExemption(context)
                hasBackgroundLocation = checkBackgroundLocation(context)
                hasFineLocation = checkFineLocation(context)
                hasBluetoothConnect = checkBluetoothConnect(context)
                if (com.example.service.AutoParkLocationService.isMonitoringEnabled(context)) {
                    com.example.service.AutoParkLocationService.startMonitoring(context)
                }
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

    val isBluetoothNeeded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val isBluetoothGranted = !isBluetoothNeeded || hasBluetoothConnect
    val isAllGranted = isBatteryOptimizationIgnored && hasBackgroundLocation && isBluetoothGranted

    val pendingBattery = !isBatteryOptimizationIgnored
    val pendingLocation = !hasBackgroundLocation
    val pendingBluetooth = isBluetoothNeeded && !hasBluetoothConnect

    val pendingCount = (if (pendingBattery) 1 else 0) +
            (if (pendingLocation) 1 else 0) +
            (if (pendingBluetooth) 1 else 0)

    val shouldShow = !isDismissed && (!isAllGranted || showWhenAllGranted)

    AnimatedVisibility(
        visible = shouldShow,
        enter = fadeIn(tween(300)) + expandVertically(tween(350)),
        exit = fadeOut(tween(250)) + shrinkVertically(tween(300))
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(300, easing = FastOutSlowInEasing))
                .testTag("background_diagnostic_banner"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top Header Row matching settings cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isAllGranted) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isAllGranted) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.backgroundDiagnosticTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (isAllGranted) {
                                Spacer(modifier = Modifier.height(2.dp))

                                // Status Pill matching settings badges
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = strings.backgroundSetupComplete,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action at top right: Dismiss on home, or Expand/Collapse toggle in settings
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isAllGranted && showWhenAllGranted) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .clickable { isExpandedWhenAllGranted = !isExpandedWhenAllGranted }
                                    .testTag("toggle_expand_diagnostic_banner")
                            ) {
                                Icon(
                                    imageVector = if (isExpandedWhenAllGranted) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpandedWhenAllGranted) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else if (dismissible) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f))
                                    .clickable { isDismissed = true }
                                    .testTag("dismiss_diagnostic_banner")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = strings.cancel,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }

                // Description
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isAllGranted) strings.backgroundSetupActiveDesc else strings.backgroundDiagnosticDesc,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Divider and Steps List
                val showSteps = !isAllGranted || isExpandedWhenAllGranted

                AnimatedVisibility(
                    visible = showSteps,
                    enter = expandVertically(tween(300)) + fadeIn(tween(250)),
                    exit = shrinkVertically(tween(250)) + fadeOut(tween(200))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // 1. Unrestricted Battery - only show if user hasn't enabled it
                        if (pendingBattery) {
                            SetupStepRow(
                                title = strings.backgroundSetupStepBatteryTitle,
                                description = strings.backgroundSetupStepBatteryDesc,
                                icon = Icons.Default.BatteryChargingFull,
                                isGranted = false,
                                actionLabel = strings.backgroundSetupStatusAction,
                                readyLabel = strings.backgroundSetupStatusReady,
                                onClickAction = {
                                    requestBatteryExemption(context)
                                },
                                onGrantedTap = {},
                                testTag = "setup_step_battery"
                            )
                        }

                        // 2. Background Location - only show if user hasn't enabled it
                        if (pendingLocation) {
                            SetupStepRow(
                                title = strings.backgroundSetupStepLocationTitle,
                                description = strings.backgroundSetupStepLocationDesc,
                                icon = Icons.Default.LocationOn,
                                isGranted = false,
                                actionLabel = strings.backgroundSetupStatusAction,
                                readyLabel = strings.backgroundSetupStatusReady,
                                onClickAction = {
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
                                onGrantedTap = {},
                                testTag = "setup_step_location"
                            )
                        }

                        // 3. Nearby Devices (Android 12+) - only show if user hasn't enabled it
                        if (pendingBluetooth) {
                            SetupStepRow(
                                title = strings.backgroundSetupStepBluetoothTitle,
                                description = strings.backgroundSetupStepBluetoothDesc,
                                icon = Icons.Default.Bluetooth,
                                isGranted = false,
                                actionLabel = strings.backgroundSetupStatusAction,
                                readyLabel = strings.backgroundSetupStatusReady,
                                onClickAction = {
                                    bluetoothConnectLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                },
                                onGrantedTap = {},
                                testTag = "setup_step_bluetooth"
                            )
                        }

                        // Fallback when explicitly viewed with showWhenAllGranted = true
                        if (isAllGranted) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_all_configured")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = strings.backgroundSetupActiveDesc,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupStepRow(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    actionLabel: String,
    readyLabel: String,
    onClickAction: () -> Unit,
    onGrantedTap: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = if (isGranted) onGrantedTap else onClickAction)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Step status icon squircle
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isGranted) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isGranted) Icons.Default.CheckCircle else icon,
                            contentDescription = null,
                            tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action Button or Ready Badge
            if (isGranted) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
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
                            text = readyLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Button(
                    onClick = onClickAction,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("${testTag}_action_btn")
                ) {
                    Text(
                        text = actionLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
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
