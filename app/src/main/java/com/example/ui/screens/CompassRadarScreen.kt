package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import com.example.ui.i18n.isFloorConfigured
import com.example.ui.i18n.localizeFloor
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EditSpotDialog
import com.example.ui.components.ParkingTimerButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ParkingSpot
import com.example.data.entity.isBluetoothEntry
import com.example.data.entity.isGeofenceEntry
import com.example.sensor.CompassState
import com.example.sensor.LocationHelper
import com.example.ui.components.CompassRadarNeedle
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.PixelGreen
import com.example.ui.viewmodel.NavigationTelemetry
import com.example.ui.viewmodel.ParkingViewModel
import kotlin.math.abs
import kotlin.math.sqrt

@Composable
fun CompassRadarScreen(
    viewModel: ParkingViewModel,
    activeSpot: ParkingSpot?,
    telemetry: NavigationTelemetry,
    compassState: CompassState,
    isNavBarVisible: Boolean = true,
    isTabSelected: Boolean = true,
    onBack: () -> Unit = {},
    onUserInteraction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var showMenu by remember { mutableStateOf(false) }
    var showEditNoteDialog by remember { mutableStateOf(false) }
    var showNoSpotAlert by remember { mutableStateOf(false) }
    var showCalibrationDialog by remember { mutableStateOf(false) }
    var currentNoteText by remember { mutableStateOf(activeSpot?.note ?: "") }
    var isInterferenceDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(compassState.hasMagneticInterference, compassState.needsCalibration) {
        if (!compassState.hasMagneticInterference && !compassState.needsCalibration) {
            isInterferenceDismissed = false
        }
    }

    val isTimerRunning by viewModel.timerIsRunning.collectAsStateWithLifecycle()
    val timerRemainingSeconds by viewModel.timerRemainingSeconds.collectAsStateWithLifecycle()
    val isParkingTimerEnabled by viewModel.isParkingTimerFeatureEnabled.collectAsStateWithLifecycle()
    val showTimerDialog by viewModel.showTimerDialog.collectAsStateWithLifecycle()
    val carBadgeStyle by viewModel.carBadgeStyle.collectAsStateWithLifecycle()
    val capyVariant by viewModel.capyVariant.collectAsStateWithLifecycle()

    DisposableEffect(isTabSelected) {
        if (isTabSelected) {
            viewModel.startCompass()
        }
        onDispose {
            viewModel.stopCompass()
        }
    }

    val btProximityState by viewModel.btProximityState.collectAsStateWithLifecycle()
    val isBtMode = btProximityState.isSearching && btProximityState.targetDevice != null

    val effectiveRelativeArrowAngle = if (isBtMode) btProximityState.relativeAngle else telemetry.relativeArrowAngle
    val effectiveDistanceMeters = if (isBtMode) btProximityState.distanceMeters else telemetry.distanceMeters
    val effectiveHasTarget = if (isBtMode) true else telemetry.hasActiveTarget
    val effectiveIsAligned = if (isBtMode) {
        abs(btProximityState.relativeAngle) < 20f || btProximityState.relativeAngle > 340f
    } else {
        telemetry.hasActiveTarget && (abs(telemetry.relativeArrowAngle) < 15f || telemetry.relativeArrowAngle > 345f)
    }

    val isAligned = effectiveIsAligned
    val localeSep = java.text.DecimalFormatSymbols.getInstance().decimalSeparator
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val distanceUnit by viewModel.distanceUnit.collectAsStateWithLifecycle()
    val isImperial = appLanguage == com.example.ui.i18n.AppLanguage.ENGLISH && distanceUnit == com.example.sensor.DistanceUnit.IMPERIAL

    val distanceDisplay = if (isBtMode) {
        if (isImperial) {
            val feet = btProximityState.distanceMeters * 3.28084f
            if (feet < 6f) "< 6 ft" else "${feet.toInt()} ft"
        } else {
            if (btProximityState.distanceMeters < 2f) {
                "< 2 ${strings.meters}"
            } else {
                "${btProximityState.distanceMeters.toInt()} ${strings.meters}"
            }
        }
    } else when {
        activeSpot == null -> "--"
        !telemetry.hasActiveTarget -> {
            if (telemetry.distanceMeters > 0f) {
                if (isImperial) {
                    val feet = telemetry.distanceMeters * 3.28084f
                    if (feet < 5280) "${feet.toInt()} ft" else String.format(java.util.Locale.US, "%.1f mi", feet / 5280f)
                } else {
                    "${telemetry.distanceMeters.toInt()} ${strings.meters}"
                }
            } else strings.locating
        }
        telemetry.formattedDistance.isBlank() || telemetry.formattedDistance == "No GPS Fix" -> {
            if (telemetry.distanceMeters > 0f) {
                if (isImperial) {
                    val feet = telemetry.distanceMeters * 3.28084f
                    if (feet < 5280) "${feet.toInt()} ft" else String.format(java.util.Locale.US, "%.1f mi", feet / 5280f)
                } else {
                    "${telemetry.distanceMeters.toInt()} ${strings.meters}"
                }
            } else strings.locating
        }
        else -> {
            val formatted = telemetry.formattedDistance.replace('.', localeSep)
            if (strings.meters != "m" && formatted.endsWith("m") && !formatted.endsWith("km")) {
                formatted.replace("m", strings.meters)
            } else {
                formatted
            }
        }
    }

    val allDevices by viewModel.allDevices.collectAsStateWithLifecycle()
    val associatedDevice = remember(activeSpot, allDevices) {
        val addr = activeSpot?.bluetoothDeviceAddress
        if (addr != null) {
            allDevices.firstOrNull { it.address.equals(addr, ignoreCase = true) }
        } else {
            val devName = activeSpot?.bluetoothDeviceName
            if (devName != null) {
                allDevices.firstOrNull { it.name.equals(devName, ignoreCase = true) }
            } else null
        }
    }

    val isDeviceRenamed = associatedDevice?.isCustomRenamed == true && associatedDevice.name.isNotBlank()

    // Dynamic proximity status title matching Google Find My Device UI
    val proximityStatus: String? = if (isBtMode) {
        when (btProximityState.signalTier) {
            com.example.sensor.BtSignalTier.IMMEDIATE -> strings.carIsHere
            com.example.sensor.BtSignalTier.VERY_CLOSE -> strings.carVeryClose
            com.example.sensor.BtSignalTier.CLOSE -> strings.carVeryClose
            com.example.sensor.BtSignalTier.MODERATE -> strings.walkInThisDirection
            com.example.sensor.BtSignalTier.WEAK -> strings.alignPhone
            com.example.sensor.BtSignalTier.SEARCHING -> "Searching Bluetooth signal..."
        }
    } else when {
        activeSpot == null -> strings.noSpotsFound
        !telemetry.hasActiveTarget -> strings.locating
        telemetry.distanceMeters <= 3.0f -> strings.carIsHere
        telemetry.distanceMeters <= 8.0f -> strings.carVeryClose
        telemetry.distanceMeters <= 20.0f -> strings.walkInThisDirection
        telemetry.distanceMeters <= 45.0f -> strings.alignPhone
        else -> null
    }

    val isCarHereOrVeryClose = if (isBtMode) {
        btProximityState.signalTier == com.example.sensor.BtSignalTier.IMMEDIATE ||
        btProximityState.signalTier == com.example.sensor.BtSignalTier.VERY_CLOSE ||
        btProximityState.signalTier == com.example.sensor.BtSignalTier.CLOSE
    } else {
        telemetry.hasActiveTarget && (
            telemetry.distanceMeters <= 8.0f ||
            proximityStatus == strings.carIsHere ||
            proximityStatus == strings.carVeryClose
        )
    }

    // 5-second initial guarantee timer upon opening app or starting navigation
    var isInitial5Seconds by remember { mutableStateOf(true) }
    LaunchedEffect(isTabSelected, activeSpot?.id) {
        if (isTabSelected) {
            isInitial5Seconds = true
            kotlinx.coroutines.delay(5000L)
            isInitial5Seconds = false
        }
    }

    // Dynamic movement detection: track user motion via accelerometer and GPS
    var lastMotionTimestamp by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var isUserMoving by remember { mutableStateOf(false) }

    // Accelerometer motion listener for responsive step and walking detection
    DisposableEffect(isTabSelected) {
        if (!isTabSelected) {
            onDispose { }
        } else {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val listener = object : SensorEventListener {
                private var lastSampleTime = 0L

                override fun onSensorChanged(event: SensorEvent?) {
                    if (event == null || event.values.size < 3) return
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastSampleTime < 100L) return
                    lastSampleTime = now

                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val magnitude = sqrt(x * x + y * y + z * z)
                    val delta = abs(magnitude - SensorManager.GRAVITY_EARTH)

                    // Walking or moving around causes acceleration variation > 0.45 m/s²
                    if (delta > 0.45f) {
                        lastMotionTimestamp = now
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            if (accelSensor != null) {
                sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            }

            onDispose {
                sensorManager?.unregisterListener(listener)
            }
        }
    }

    // Track GPS location changes and speed
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    var previousLocation by remember { mutableStateOf<android.location.Location?>(null) }
    LaunchedEffect(currentLocation) {
        val loc = currentLocation
        if (loc != null) {
            val now = SystemClock.elapsedRealtime()
            if (loc.hasSpeed() && loc.speed > 0.35f) {
                lastMotionTimestamp = now
            } else {
                val prev = previousLocation
                if (prev != null) {
                    val distMoved = LocationHelper.calculateDistanceMeters(
                        prev.latitude, prev.longitude,
                        loc.latitude, loc.longitude
                    )
                    if (distMoved >= 1.2f) {
                        lastMotionTimestamp = now
                    }
                }
            }
            previousLocation = loc
        }
    }

    // Track navigation distance updates
    var previousDistance by remember { mutableFloatStateOf(effectiveDistanceMeters) }
    LaunchedEffect(effectiveDistanceMeters) {
        if (abs(effectiveDistanceMeters - previousDistance) >= 0.6f) {
            lastMotionTimestamp = SystemClock.elapsedRealtime()
        }
        previousDistance = effectiveDistanceMeters
    }

    // Periodically evaluate if user seems to not move (stationary for >= 1800ms)
    LaunchedEffect(isTabSelected) {
        if (isTabSelected) {
            while (true) {
                val now = SystemClock.elapsedRealtime()
                isUserMoving = (now - lastMotionTimestamp) < 1800L
                kotlinx.coroutines.delay(250L)
            }
        }
    }

    // Instruction ("Once navigation starts move around. The shape fills as you get closer to the vehicle.")
    // shows initially and hides after the initial display window (or 2s after nav bar retracts),
    // and latches dismissed so it NEVER reappears when the user starts moving again.
    val isNavigationActive = activeSpot != null || isBtMode
    var hasInstructionDismissed by remember(activeSpot?.id, isBtMode) { mutableStateOf(false) }
    var isIdleInstructionVisible by remember(activeSpot?.id, isBtMode) { mutableStateOf(true) }

    LaunchedEffect(isNavBarVisible, isNavigationActive) {
        if (!isNavBarVisible) {
            kotlinx.coroutines.delay(2000L)
            isIdleInstructionVisible = false
            hasInstructionDismissed = true
        }
    }

    LaunchedEffect(isInitial5Seconds, isUserMoving) {
        if (!isInitial5Seconds) {
            hasInstructionDismissed = true
            isIdleInstructionVisible = false
        }
    }

    val shouldShowInstruction = if (hasInstructionDismissed) {
        false
    } else if (!isNavigationActive) {
        isIdleInstructionVisible && isInitial5Seconds
    } else {
        !isCarHereOrVeryClose && isInitial5Seconds
    }

    val spotDisplayName = if (isBtMode) {
        btProximityState.targetDevice?.name ?: "Bluetooth Device"
    } else when {
        activeSpot == null -> strings.myParkedCar
        isDeviceRenamed -> associatedDevice.name
        else -> strings.myParkedCar
    }

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            onUserInteraction()
                        }
                    }
                }
            }
            .testTag("waypoint_screen")
    ) {
        val availWidth = maxWidth
        val availHeight = maxHeight

        val hasCalibrationBanner = (compassState.needsCalibration || compassState.hasMagneticInterference) && !isInterferenceDismissed
        val hasTimerBtn = (activeSpot != null || telemetry.hasActiveTarget) && isParkingTimerEnabled
        val hasFoundCarBtn = activeSpot != null && telemetry.hasActiveTarget && telemetry.distanceMeters <= 50.0f
        val actionButtonCount = 1 + (if (hasTimerBtn) 1 else 0) + (if (hasFoundCarBtn) 1 else 0)

        // Calculate reserved vertical height for header, active banners, distance readout, instructions, and bottom controls
        val reservedVerticalSpace = 60.dp +
            (if (isBtMode) 68.dp else 0.dp) +
            (if (hasCalibrationBanner) 78.dp else 0.dp) +
            (if (shouldShowInstruction) 136.dp else 92.dp) +
            (actionButtonCount * 52).dp +
            (if (isNavBarVisible) 106.dp else 28.dp)

        val maxRadarByWidth = availWidth * 0.78f
        val maxRadarByHeight = (availHeight - reservedVerticalSpace).coerceAtLeast(110.dp)
        val adaptiveRadarSize = minOf(maxRadarByWidth, maxRadarByHeight, 340.dp).coerceAtLeast(110.dp)
        val isCompactHeight = availHeight < 680.dp || adaptiveRadarSize < 210.dp
        val radarToTextSpacing = if (isCompactHeight) 14.dp else 28.dp
        val distanceFontSize = if (isCompactHeight) 34.sp else 42.sp

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP MINIMAL HEADER BAR (Centered spot name, More "⋮" on right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spacer matches the 48dp options button to keep spotDisplayName centered
                Spacer(modifier = Modifier.size(48.dp))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = spotDisplayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("waypoint_menu_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .widthIn(min = 210.dp)
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        // 1. Share Spot
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = strings.shareSpot,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            leadingIcon = {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                showMenu = false
                                viewModel.shareParkingLocation(context, activeSpot)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .testTag("menu_share_spot")
                        )

                        // 2. Edit Details (when active spot exists)
                        if (activeSpot != null) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = strings.editDetails,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    showEditNoteDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .testTag("menu_edit_details")
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )

                        // 3. Mark Car Found
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = strings.foundCar,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            leadingIcon = {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                showMenu = false
                                viewModel.markCarFound()
                                onBack()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .testTag("menu_found_car")
                        )
                    }
                }
            }

            // BLUETOOTH PROXIMITY FINDER ACTIVE BANNER
            AnimatedVisibility(
                visible = isBtMode,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                Surface(
                    onClick = { viewModel.stopBtProximityFinder() },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("bt_proximity_active_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.BluetoothSearching,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = btProximityState.targetDevice?.name ?: "Bluetooth Proximity Finder",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = btProximityState.statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.stopBtProximityFinder() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Bluetooth Search",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // AUTOMATIC COMPASS CALIBRATION BANNER
            AnimatedVisibility(
                visible = (compassState.needsCalibration || compassState.hasMagneticInterference) && !isInterferenceDismissed,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (compassState.hasMagneticInterference) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = if (compassState.hasMagneticInterference) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("compass_calibration_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (compassState.hasMagneticInterference) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    tint = if (compassState.hasMagneticInterference) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (compassState.hasMagneticInterference) strings.compassMagneticInterference else strings.compassCalibrationNeeded,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (compassState.hasMagneticInterference) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (compassState.hasMagneticInterference) strings.compassMagneticInterferenceInstruction else strings.compassCalibrationInstruction,
                                style = MaterialTheme.typography.bodySmall,
                                color = (if (compassState.hasMagneticInterference) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer).copy(alpha = 0.9f),
                                lineHeight = 16.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                isInterferenceDismissed = true
                                viewModel.dismissCompassCalibration()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = if (compassState.hasMagneticInterference) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.7f))

            val distanceCoveredProgress = if (isBtMode) {
                viewModel.getDistanceCoveredProgress(null, btProximityState.distanceMeters, isBtMode = true)
            } else {
                telemetry.distanceCoveredProgress
            }

            // CENTER FIND MY DEVICE SCALLOPED PROXIMITY BADGE WITH FIXED DIRECTIONAL ARROW
            CompassRadarNeedle(
                relativeArrowAngle = effectiveRelativeArrowAngle,
                compassAzimuth = compassState.azimuthDegrees,
                distanceMeters = effectiveDistanceMeters,
                isAlignedWithTarget = isAligned,
                hasActiveTarget = effectiveHasTarget,
                distanceCoveredProgress = distanceCoveredProgress,
                modifier = Modifier.size(adaptiveRadarSize)
            )

            Spacer(modifier = Modifier.height(radarToTextSpacing))

            // METERS LEFT (BIG DISPLAY REPLACING PREVIOUS POSITION, NEVER GOES AWAY)
            Text(
                text = distanceDisplay,
                fontSize = distanceFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("waypoint_meters_text")
            )

            // CONFIGURED FLOOR (Displayed below the meters in active navigation)
            val configuredFloor = activeSpot?.floorLevel?.trim().orEmpty()
            val isAutoBtOrWalkAway = activeSpot != null && (activeSpot.isBluetoothEntry || activeSpot.isGeofenceEntry)
            if (activeSpot != null && !isAutoBtOrWalkAway && isFloorConfigured(configuredFloor)) {
                val displayFloor = localizeFloor(configuredFloor, strings)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    onClick = { showEditNoteDialog = true },
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("waypoint_configured_floor_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "${strings.floor}: $displayFloor",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // PROXIMITY STATUS (Sits below the meters left number, e.g. "Car is right here")
            if (!proximityStatus.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = proximityStatus,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (telemetry.distanceMeters <= 5.0f && telemetry.hasActiveTarget) {
                        PixelGreen
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // HELPER EXPLANATORY SUBTITLE
            // - Idle ("Once navigation starts move around..."): disappears when and only when the navigation bar retracts, and reappears when the navigation bar reappears.
            // - Active ("Move around..."): dynamically shows/hides with user motion (always showing for first 5s).
            AnimatedVisibility(
                visible = shouldShowInstruction,
                enter = fadeIn(animationSpec = tween(durationMillis = 400)) + expandVertically(animationSpec = tween(durationMillis = 400)),
                exit = fadeOut(animationSpec = tween(durationMillis = 400)) + shrinkVertically(animationSpec = tween(durationMillis = 400))
            ) {
                Text(
                    text = if (isNavigationActive) strings.radarMoveInstruction else strings.radarMoveInstructionIdle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .testTag("waypoint_move_instruction_text")
                )
            }

            if (activeSpot != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = activeSpot.address.ifBlank { activeSpot.spotName },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // BOTTOM-RIGHT ACTION BUTTONS (Found Car [<= 50m] above Google Maps)
            val animatedBottomSpacerHeight by animateDpAsState(
                targetValue = if (isNavBarVisible) 102.dp else 24.dp,
                animationSpec = tween(
                    durationMillis = 650,
                    easing = FastOutSlowInEasing
                ),
                label = "waypoint_bottom_spacer_height"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Blue Parking Timer button: same size as Found Car button, sits just above it
                if ((activeSpot != null || telemetry.hasActiveTarget) && isParkingTimerEnabled) {
                    ParkingTimerButton(
                        isRunning = isTimerRunning,
                        remainingSeconds = timerRemainingSeconds,
                        viewModel = viewModel,
                        isFullWidth = false
                    )
                }

                // Dynamically shows when user is 50m or less from parked spot
                AnimatedVisibility(
                    visible = activeSpot != null && telemetry.hasActiveTarget && telemetry.distanceMeters <= 50.0f,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    com.example.ui.components.FoundCarTimerStyleButton(
                        text = strings.foundCar,
                        onClick = {
                            viewModel.markCarFound()
                            onBack()
                        },
                        isFullWidth = false,
                        testTag = "waypoint_found_car_button"
                    )
                }

                com.example.ui.components.WaypointMapsTimerStyleButton(
                    text = strings.googleMaps,
                    onClick = {
                        if (activeSpot != null) {
                            viewModel.openGoogleMapsNavigation(context, activeSpot)
                        } else {
                            showNoSpotAlert = true
                        }
                    },
                    isFullWidth = false,
                    badgeShape = com.example.ui.components.MapBadgeShape.ROUNDED_SQUARE,
                    testTag = "waypoint_maps_button"
                )
            }

            Spacer(
                modifier = Modifier
                    .height(animatedBottomSpacerHeight)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            )
        }
    }

    if (showEditNoteDialog && activeSpot != null) {
        EditSpotDialog(
            initialSpot = activeSpot,
            carBadgeStyle = carBadgeStyle,
            capyVariant = capyVariant,
            isTimerRunning = isTimerRunning,
            timerRemainingSeconds = timerRemainingSeconds,
            isTimerOpen = showTimerDialog,
            viewModel = viewModel,
            onOpenTimer = {
                viewModel.openParkingTimer()
            },
            onDismiss = { showEditNoteDialog = false },
            onSave = { name, floor, note, _, _, _ ->
                viewModel.updateSpotDetails(activeSpot.id, name, floor, note)
                showEditNoteDialog = false
            }
        )
    }

    if (showNoSpotAlert) {
        AlertDialog(
            onDismissRequest = { showNoSpotAlert = false },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = strings.selectParkingSpotFirst,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { showNoSpotAlert = false },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("dismiss_no_spot_alert_btn")
                ) {
                    Text(strings.confirm)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    if (showCalibrationDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissCompassCalibration()
                showCalibrationDialog = false
                isInterferenceDismissed = true
            },
            shape = RoundedCornerShape(28.dp),
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = strings.calibrateCompass,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.compassCalibrationInstruction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Heading",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${compassState.azimuthDegrees.toInt()}°  ${compassState.cardinalDirection}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissCompassCalibration()
                        showCalibrationDialog = false
                        isInterferenceDismissed = true
                        Toast.makeText(context, strings.compassRecalibrated, Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_compass_calibration_btn")
                ) {
                    Text(strings.confirm)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}
