package com.example.ui.components

import android.location.Location
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ParkingSpot
import com.example.ui.i18n.LocalAppStrings
import java.util.Locale

private data class FloorOption(val id: String, val label: String, val aliases: List<String>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSpotDialog(
    initialSpot: ParkingSpot?,
    currentLocation: Location? = null,
    currentAddress: String = "",
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

    var spotName by remember { mutableStateOf(initialName) }
    var selectedFloor by remember { mutableStateOf(initialFloor) }
    var note by remember { mutableStateOf(initialSpot?.note ?: "") }

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("edit_spot_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row (Matches layout with square rounded icon and title/subtitle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AddLocationAlt,
                            contentDescription = strings.saveParkingSpotTitle,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (initialSpot != null) strings.editSpotDetailsTitle else strings.saveParkingSpotTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (initialSpot != null) strings.editSpotDetailsSubtitle else strings.recordVehiclePositionSubtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // LOCATION COORDINATES CARD (Clean, minimal, accurate)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = if (initialSpot != null) strings.savedParkingCoordinatesHeader else strings.parkingCoordinatesHeader,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val displayAddress = when {
                        initialSpot != null -> initialSpot.address.ifBlank { strings.recordedParkingSpot }
                        currentAddress.isNotBlank() -> currentAddress
                        currentLocation != null -> String.format(Locale.US, "Lat: %.5f, Lng: %.5f", currentLocation.latitude, currentLocation.longitude)
                        else -> strings.currentVehicleLocation
                    }

                    Text(
                        text = displayAddress,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
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
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Spot Name Field
            OutlinedTextField(
                value = spotName,
                onValueChange = { spotName = it },
                label = { Text(strings.spotNameOrVehicleLabel) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("spot_name_input")
            )

            // Floor / Underground Level Selector
            Text(
                text = strings.floorOrParkingLevelLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                        label = { Text(option.label, fontSize = 12.sp) },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.testTag("floor_chip_${option.id}")
                    )
                }
            }

            // Notes Field
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(strings.notesOrPillarLabel) },
                placeholder = { Text(strings.notesOrPillarPlaceholder) },
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("spot_note_input")
            )

            // Bottom Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("cancel_spot_btn")
                ) {
                    Text(strings.cancel)
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
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("save_spot_confirm_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text(if (initialSpot != null) strings.updateDetailsButton else strings.saveSpotButton, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
