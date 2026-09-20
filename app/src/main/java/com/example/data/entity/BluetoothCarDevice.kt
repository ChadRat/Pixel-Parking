package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bluetooth_devices")
data class BluetoothCarDevice(
    @PrimaryKey
    val address: String,
    val name: String,
    val isMonitoredCar: Boolean = true,
    val lastConnectedTimestamp: Long = System.currentTimeMillis(),
    val deviceType: String = "Car Audio / Infotainment", // "Car", "Headset", "OBD2", "Other"
    val isCustomRenamed: Boolean = false,
    val originalName: String = ""
)
