package com.example.ui.components

import android.location.Location
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ParkingSpot
import com.example.data.entity.isGeofenceEntry
import com.example.ui.i18n.LocalAppStrings
import java.util.Locale

private data class FloorOption(val id: String, val label: String, val aliases: List<String>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSpotDialog(
    initialSpot: ParkingSpot?,
    currentLocation: Location? = null,
    currentAddress: String = "",
    carBadgeStyle: CarBadgeStyle = CarBadgeStyle.CINEMATIC,
    capyVariant: CapyVariant = CapyVariant.BABY,
    isTimerRunning: Boolean = false,
    timerRemainingSeconds: Long = 0L,
    isTimerOpen: Boolean = false,
    viewModel: com.example.ui.viewmodel.ParkingViewModel? = null,
    onOpenTimer: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, floor: String, note: String, customLat: Double?, customLng: Double?, customAddress: String?) -> Unit,
    onRefreshGps: () -> Unit = {},
    isRefreshingGps: Boolean = false,
    onSearchAddress: (suspend (String) -> Pair<Double, Double>?)? = null
) {
    val strings = LocalAppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialName = remember(initialSpot?.spotName, strings) {
        val raw = initialSpot?.spotName
        if (raw.isNullOrBlank() || raw == "My Parked Car" || raw == "Το Αυτοκίνητό μου") {
            strings.myParkedCar
        } else {
            raw
        }
    }

    val initialFloor = remember(initialSpot?.floorLevel, strings) {
        when (val raw = initialSpot?.floorLevel) {
            "Ground Level", "Ισόγειο", null, "" -> strings.floorGroundLevel
            "Level 1", "1ος Όροφος", "Επίπεδο 1" -> strings.floorLevel1
            "Level 2", "2ος Όροφος", "Επίπεδο 2" -> strings.floorLevel2
            "Level 3", "3ος Όροφος", "Επίπεδο 3" -> strings.floorLevel3
            "Underground P1", "Υπόγειο -1", "Υπόγειο 1" -> strings.floorUndergroundP1
            "Underground P2", "Υπόγειο -2", "Υπόγειο 2" -> strings.floorUndergroundP2
            "Underground P3", "Υπόγειο -3", "Υπόγειο 3" -> strings.floorUndergroundP3
            "Roof Deck", "Ταράτσα" -> strings.floorRoofDeck
            else -> raw
        }
    }

    var spotName by rememberSaveable { mutableStateOf(initialName) }
    var selectedFloor by rememberSaveable { mutableStateOf(initialFloor) }
    var note by rememberSaveable { mutableStateOf(initialSpot?.note ?: "") }
    var showTimerInstructions by rememberSaveable { mutableStateOf(false) }
    val timerArrowRotation by animateFloatAsState(
        targetValue = if (showTimerInstructions) 180f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "timer_instructions_arrow"
    )

    val commonFloors = remember(strings) {
        listOf(
            FloorOption("ground", strings.floorGroundLevel, listOf("Ground Level", "Ισόγειο")),
            FloorOption("lvl1", strings.floorLevel1, listOf("Level 1", "1ος Όροφος", "Επίπεδο 1")),
            FloorOption("lvl2", strings.floorLevel2, listOf("Level 2", "2ος Όροφος", "Επίπεδο 2")),
            FloorOption("lvl3", strings.floorLevel3, listOf("Level 3", "3ος Όροφος", "Επίπεδο 3")),
            FloorOption("p1", strings.floorUndergroundP1, listOf("Underground P1", "Υπόγειο -1", "Υπόγειο 1")),
            FloorOption("p2", strings.floorUndergroundP2, listOf("Underground P2", "Υπόγειο -2", "Υπόγειο 2")),
            FloorOption("p3", strings.floorUndergroundP3, listOf("Underground P3", "Υπόγειο -3", "Υπόγειο 3")),
            FloorOption("roof", strings.floorRoofDeck, listOf("Roof Deck", "Ταράτσα"))
        )
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val maxSheetHeight = (screenHeight - 110.dp).coerceAtMost(screenHeight * 0.82f)

    if (!isTimerOpen) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            scrimColor = Color.Black.copy(alpha = 0.65f),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 28.dp)
                .testTag("edit_spot_bottom_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            // Hero Section: Utility Drawer Icon with surrounding shapes
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp, bottom = 0.dp),
                contentAlignment = Alignment.Center
            ) {
                UtilityDrawerHeroBadge(
                    modifier = Modifier.size(width = 250.dp, height = 180.dp)
                )
            }

            // Header Title (Clean single title with geofence badge if applicable)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (initialSpot != null) strings.editSpotDetailsTitle else strings.saveParkingSpotTitle,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            // Dedicated Parking Timer Option with interactive Pill on the right side & clear instructions
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isTimerRunning) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_spot_timer_btn")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showTimerInstructions = !showTimerInstructions }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            val isTimerStopped = viewModel?.timerIsStopped?.collectAsStateWithLifecycle()?.value ?: false
                            val iconBgColor = when {
                                isTimerStopped -> Color(0xFFD93025)
                                isTimerRunning -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            }
                            val iconTint = when {
                                isTimerStopped || isTimerRunning -> Color.White
                                else -> MaterialTheme.colorScheme.primary
                            }
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        color = iconBgColor,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = strings.parkingTimerTitle,
                                    tint = iconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.parkingTimerTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (showTimerInstructions) "Hide instructions" else "Show instructions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer { rotationZ = timerArrowRotation }
                            )
                        }

                        // Interactive Timer Pill on the right side
                        ParkingTimerButton(
                            isRunning = isTimerRunning,
                            remainingSeconds = timerRemainingSeconds,
                            viewModel = viewModel,
                            isFullWidth = false
                        )
                    }

                    // Instructions of how to interact with the pill and set the timer (dynamically and smoothly revealed under arrow)
                    AnimatedVisibility(
                        visible = showTimerInstructions,
                        enter = expandVertically(animationSpec = tween(durationMillis = 280)) + fadeIn(animationSpec = tween(durationMillis = 240)),
                        exit = shrinkVertically(animationSpec = tween(durationMillis = 280)) + fadeOut(animationSpec = tween(durationMillis = 200))
                    ) {
                        val instructions = if (strings.languageSection == "Γλώσσα") {
                            "• 1ο πάτημα: Ρύθμιση ωρών (σύρετε πάνω/κάτω)\n• 2ο πάτημα: Ρύθμιση λεπτών (σύρετε πάνω/κάτω)\n• 3ο πάτημα: Έναρξη χρονομέτρου\n• Παρατεταμένο πάτημα (500ms): Διακοπή χρονομέτρου\n• Παρατεταμένο πάτημα (2s): Μηδενισμός (00:00)"
                        } else {
                            "• Tap 1: Set hours (swipe up/down)\n• Tap 2: Set minutes (swipe up/down)\n• Tap 3: Start timer\n• Press & hold (500ms): Stop timer\n• Press & hold (2s): Reset (00:00)"
                        }
                        Text(
                            text = instructions,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                    }
                }
            }

            // Location Coordinates Card (Home Tab Live Strip Aesthetic)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (initialSpot != null) strings.savedParkingCoordinatesHeader else strings.parkingCoordinatesHeader,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val displayAddress = when {
                            initialSpot != null -> initialSpot.address.ifBlank { strings.recordedParkingSpot }
                            currentAddress.isNotBlank() -> currentAddress
                            currentLocation != null -> String.format(Locale.US, "Lat: %.5f, Lng: %.5f", currentLocation.latitude, currentLocation.longitude)
                            else -> strings.currentVehicleLocation
                        }
                        Text(
                            text = displayAddress,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val coordsText = when {
                            initialSpot != null -> String.format(Locale.US, "GPS: %.5f, %.5f", initialSpot.latitude, initialSpot.longitude)
                            currentLocation != null -> String.format(
                                Locale.US,
                                "GPS: %.5f, %.5f (±%dm)",
                                currentLocation.latitude,
                                currentLocation.longitude,
                                currentLocation.accuracy.toInt()
                            )
                            else -> null
                        }
                        if (coordsText != null) {
                            Text(
                                text = coordsText,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (initialSpot == null) {
                        IconButton(
                            onClick = onRefreshGps,
                            modifier = Modifier.size(34.dp)
                        ) {
                            if (isRefreshingGps) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh GPS",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Spot Name Input Field
            OutlinedTextField(
                value = spotName,
                onValueChange = { spotName = it },
                label = { Text(strings.spotNameOrVehicleLabel) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("spot_name_input")
            )

            // Floor / Level Selector
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.floorOrParkingLevelLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    commonFloors.forEach { option ->
                        val isSelected = selectedFloor == option.label || option.aliases.any { it.equals(selectedFloor, ignoreCase = true) }
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFloor = option.label },
                            label = { Text(option.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold) },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.testTag("floor_chip_${option.id}")
                        )
                    }
                }
            }

            // Notes / Pillar Field
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(strings.notesOrPillarLabel) },
                placeholder = { Text(strings.notesOrPillarPlaceholder) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                maxLines = 3,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("spot_note_input")
            )

            // Bottom Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("cancel_spot_btn")
                ) {
                    Text(strings.cancel, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        if (initialSpot != null) {
                            onSave(spotName, selectedFloor, note, null, null, null)
                        } else if (currentLocation != null) {
                            onSave(spotName, selectedFloor, note, currentLocation.latitude, currentLocation.longitude, currentAddress)
                        } else {
                            onSave(spotName, selectedFloor, note, null, null, null)
                        }
                        onDismiss()
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .heightIn(min = 48.dp)
                        .testTag("save_spot_confirm_btn")
                ) {
                    Icon(
                        imageVector = if (initialSpot != null) Icons.Default.CheckCircle else Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (initialSpot != null) strings.updateDetailsButton else strings.saveSpotButton,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
}
