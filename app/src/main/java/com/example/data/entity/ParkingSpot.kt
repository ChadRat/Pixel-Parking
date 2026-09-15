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
