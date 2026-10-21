package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parking_spots")
data class ParkingSpot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracyMeters: Float = 0f,
    val address: String = "Unknown Address",
    val spotName: String = "Parked Car",
    val floorLevel: String = "Ground Level", // e.g., "Underground P2", "Level 3", "Trailhead Lot"
    val note: String = "",
    val photoUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isAutoSaved: Boolean = false,
    val bluetoothDeviceName: String? = null,
    val bluetoothDeviceAddress: String? = null,
    val meterExpiryTimestamp: Long? = null, // timestamp in ms when parking meter expires
    val isActive: Boolean = true
)

/**
 * Returns true if this parking spot was auto-saved by geofence walking detection.
 */
val ParkingSpot.isGeofenceEntry: Boolean
    get() = bluetoothDeviceAddress == "GEOFENCE_WALKING_DETECTION" ||
            bluetoothDeviceAddress?.startsWith("GEOFENCE") == true ||
            bluetoothDeviceName == "Walking Detection" ||
            bluetoothDeviceName == "Walk Away Detection" ||
            bluetoothDeviceName?.contains("Walk Away", ignoreCase = true) == true ||
            bluetoothDeviceName == "Geofence Auto-Park" ||
            bluetoothDeviceName?.contains("Geofence", ignoreCase = true) == true ||
            spotName.contains("Walk Away", ignoreCase = true) ||
            spotName.contains("Geofence", ignoreCase = true) ||
            note.contains("Walk Away", ignoreCase = true) ||
            note.contains("Geofence", ignoreCase = true) ||
            note.contains("walked away", ignoreCase = true) ||
            note.contains("started walking", ignoreCase = true) ||
            note.contains("απομακρυνθήκατε", ignoreCase = true) ||
            note.contains("ξεκινήσατε να περπατάτε", ignoreCase = true)

val ParkingSpot.isWalkAwayEntry: Boolean
    get() = isGeofenceEntry

/**
 * Returns true if this parking spot was created from custom far away coordinates (developer / testing coordinates).
 */
val ParkingSpot.isCustomFarAwayEntry: Boolean
    get() = bluetoothDeviceAddress == "DEV_CUSTOM_FAR_AWAY" ||
            bluetoothDeviceAddress?.startsWith("DEV_CUSTOM") == true ||
            note.contains("Dev Custom Coordinates", ignoreCase = true) ||
            note.contains("Custom Coordinates", ignoreCase = true) ||
            note.contains("Far Away", ignoreCase = true) ||
            bluetoothDeviceName == "Custom Coordinates" ||
            bluetoothDeviceName?.contains("Custom Coordinates", ignoreCase = true) == true ||
            spotName.contains("Custom Far Away", ignoreCase = true)

/**
 * Returns true if this parking spot was saved via Bluetooth connection/disconnection.
 */
val ParkingSpot.isBluetoothEntry: Boolean
    get() {
        if (isGeofenceEntry || isCustomFarAwayEntry) return false

        val addr = bluetoothDeviceAddress
        if (!addr.isNullOrBlank() && addr != "GEOFENCE_WALKING_DETECTION" && !addr.startsWith("GEOFENCE")) {
            return true
        }

        if (isAutoSaved) {
            return true
        }

        val name = bluetoothDeviceName
        if (!name.isNullOrBlank() &&
            name != "Manual GPS Entry" &&
            name != "Custom Location" &&
            name != "Walking Detection" &&
            name != "Geofence Auto-Park" &&
            !name.contains("Geofence", ignoreCase = true) &&
            !name.contains("Manual", ignoreCase = true)
        ) {
            return true
        }

        if (note.contains("Bluetooth", ignoreCase = true) ||
            note.contains("αποσύνδεσης", ignoreCase = true) ||
            note.contains("disconnect", ignoreCase = true)
        ) {
            return true
        }

        if (spotName.contains("Bluetooth", ignoreCase = true)) {
            return true
        }

        return false
    }

/**
 * Returns true if this parking spot had the underground parking safeguard applied
 * (last reliable entrance GPS fix saved due to degraded underground signal).
 */
val ParkingSpot.isUndergroundFallback: Boolean
    get() = note.contains("Underground", ignoreCase = true) ||
            note.contains("entrance", ignoreCase = true) ||
            note.contains("Υπόγειο", ignoreCase = true) ||
            floorLevel.contains("Underground", ignoreCase = true) ||
            floorLevel.contains("Υπόγειο", ignoreCase = true)


