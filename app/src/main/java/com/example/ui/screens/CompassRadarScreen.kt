package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import com.example.sensor.CompassState
import com.example.ui.components.CompassRadarNeedle
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.PixelGreen
import com.example.ui.viewmodel.NavigationTelemetry
import com.example.ui.viewmodel.ParkingViewModel
import kotlin.math.abs

@Composable
fun CompassRadarScreen(
    viewModel: ParkingViewModel,
    activeSpot: ParkingSpot?,
    telemetry: NavigationTelemetry,
    compassState: CompassState,
    isNavBarVisible: Boolean = true,
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

    DisposableEffect(Unit) {
        viewModel.startCompass()
        onDispose {
            viewModel.stopCompass()
        }
    }

    val activeBtDevice by viewModel.activeBtProximityDevice.collectAsStateWithLifecycle()
    val btRssi by viewModel.btProximityRssi.collectAsStateWithLifecycle()
    val btDistanceMeters by viewModel.btProximityDistanceMeters.collectAsStateWithLifecycle()
    val btRelativeAngle by viewModel.btProximityRelativeAngle.collectAsStateWithLifecycle()

    val isBtMode = activeBtDevice != null

    val effectiveRelativeArrowAngle = if (isBtMode) btRelativeAngle else telemetry.relativeArrowAngle
    val effectiveDistanceMeters = if (isBtMode) btDistanceMeters else telemetry.distanceMeters
    val effectiveHasTarget = if (isBtMode) true else telemetry.hasActiveTarget
    val effectiveIsAligned = if (isBtMode) {
        abs(btRelativeAngle) < 20f || btRelativeAngle > 340f
    } else {
        telemetry.hasActiveTarget && (abs(telemetry.relativeArrowAngle) < 15f || telemetry.relativeArrowAngle > 345f)
    }

    val isAligned = effectiveIsAligned
    val localeSep = java.text.DecimalFormatSymbols.getInstance().decimalSeparator
    val distanceDisplay = if (isBtMode) {
        "${btDistanceMeters.toInt()} ${strings.meters}"
    } else when {
        activeSpot == null -> "--"
        !telemetry.hasActiveTarget -> {
            if (telemetry.distanceMeters > 0f) "${telemetry.distanceMeters.toInt()} ${strings.meters}" else strings.locating
        }
        telemetry.formattedDistance.isBlank() || telemetry.formattedDistance == "No GPS Fix" -> {
            if (telemetry.distanceMeters > 0f) "${telemetry.distanceMeters.toInt()} ${strings.meters}" else strings.locating
        }
        else -> {
            val formatted = telemetry.formattedDistance.replace('.', localeSep)
            if (strings.meters != "m" && formatted.endsWith("m")) {
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
    val proximityStatus = if (isBtMode) {
        when {
            btRssi >= -45 -> strings.carIsHere
            btRssi >= -60 -> strings.carVeryClose
            btRssi >= -75 -> strings.walkInThisDirection
            else -> strings.waypointRadarSubtitle
        }
    } else when {
        activeSpot == null -> strings.noSpotsFound
        !telemetry.hasActiveTarget -> strings.locating
        telemetry.distanceMeters <= 3.0f -> strings.carIsHere
        telemetry.distanceMeters <= 8.0f -> strings.carVeryClose
        telemetry.distanceMeters <= 20.0f -> strings.walkInThisDirection
        telemetry.distanceMeters <= 45.0f -> strings.alignPhone
        else -> strings.waypointRadarSubtitle
    }

    val isCarHereOrVeryClose = if (isBtMode) {
        btRssi >= -60
    } else {
        telemetry.hasActiveTarget && (
            telemetry.distanceMeters <= 8.0f ||
            proximityStatus == strings.carIsHere ||
            proximityStatus == strings.carVeryClose
        )
    }

    val spotDisplayName = if (isBtMode) {
        activeBtDevice?.name ?: "Bluetooth Device"
    } else when {
        activeSpot == null -> strings.myParkedCar
        isDeviceRenamed -> associatedDevice.name
        else -> strings.myParkedCar
    }

    Box(
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

                Text(
                    text = spotDisplayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

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
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.calibrateCompass, color = MaterialTheme.colorScheme.onSurface) },
                            leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                showMenu = false
                                viewModel.recalibrateCompass()
                                showCalibrationDialog = true
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(strings.shareSpot, color = MaterialTheme.colorScheme.onSurface) },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                showMenu = false
                                viewModel.shareParkingLocation(context, activeSpot)
                            }
                        )

                        if (activeSpot != null) {
                            DropdownMenuItem(
                                text = { Text(strings.editDetails, color = MaterialTheme.colorScheme.onSurface) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    showMenu = false
                                    currentNoteText = activeSpot.note
                                    showEditNoteDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(strings.foundCar, color = MaterialTheme.colorScheme.onSurface) },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    showMenu = false
                                    viewModel.markCarFound()
                                    onBack()
                                }
                            )
                        }
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
                                    text = "Finding Bluetooth Device",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "RSSI Signal Finder Active • Tap to exit",
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
                visible = compassState.needsCalibration,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
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
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.compassCalibrationNeeded,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.compassCalibrationInstruction,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f),
                                lineHeight = 16.sp
                            )
                        }
                        IconButton(
                            onClick = { viewModel.dismissCompassCalibration() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.7f))

            // CENTER FIND MY DEVICE SCALLOPED PROXIMITY BADGE WITH FIXED DIRECTIONAL ARROW
            CompassRadarNeedle(
                relativeArrowAngle = effectiveRelativeArrowAngle,
                compassAzimuth = compassState.azimuthDegrees,
                distanceMeters = effectiveDistanceMeters,
                isAlignedWithTarget = isAligned,
                hasActiveTarget = effectiveHasTarget,
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .aspectRatio(1f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // METERS LEFT (BIG DISPLAY REPLACING PREVIOUS POSITION, NEVER GOES AWAY)
            Text(
                text = distanceDisplay,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("waypoint_meters_text")
            )

            Spacer(modifier = Modifier.height(4.dp))

            // PROXIMITY STATUS (Sits below the meters left number, e.g. "Car is right here")
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

            Spacer(modifier = Modifier.height(6.dp))

            // HELPER EXPLANATORY SUBTITLE (Disappears when arrived/car is right here)
            AnimatedVisibility(
                visible = !isCarHereOrVeryClose,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = if (activeSpot != null) strings.radarMoveInstruction else strings.radarMoveInstructionIdle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 32.dp)
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
                // Dynamically shows when user is 50m or less from parked spot
                AnimatedVisibility(
                    visible = activeSpot != null && telemetry.hasActiveTarget && telemetry.distanceMeters <= 50.0f,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Button(
                        onClick = {
                            viewModel.markCarFound()
                            onBack()
                        },
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("waypoint_found_car_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.foundCar,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Button(
                    onClick = {
                        if (activeSpot != null) {
                            viewModel.openGoogleMapsNavigation(context, activeSpot)
                        } else {
                            showNoSpotAlert = true
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("waypoint_maps_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.googleMaps,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(
                modifier = Modifier
                    .height(animatedBottomSpacerHeight)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            )
        }
    }

    if (showEditNoteDialog && activeSpot != null) {
        AlertDialog(
            onDismissRequest = { showEditNoteDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    strings.editDetails,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                OutlinedTextField(
                    value = currentNoteText,
                    onValueChange = { currentNoteText = it },
                    placeholder = { Text(strings.enterNotes) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSpotNote(activeSpot.id, currentNoteText)
                        showEditNoteDialog = false
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditNoteDialog = false },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(strings.cancel)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
