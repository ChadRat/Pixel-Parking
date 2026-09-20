package com.example.wear

import android.location.Location
import android.util.Log
import com.example.AutoParkApplication
import com.example.data.database.AppDatabase
import com.example.data.entity.ParkingSpot
import com.example.data.repository.ParkingRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PhoneWearableListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == WearDataLayerBridge.PATH_REQUEST_SYNC) {
            Log.d("PhoneWearableService", "Received sync request from Wear OS node: ${messageEvent.sourceNodeId}")
            val db = AppDatabase.getDatabase(applicationContext)
            val repository = (applicationContext as? AutoParkApplication)?.repository
                ?: ParkingRepository(db.parkingSpotDao(), db.bluetoothCarDeviceDao())

            scope.launch {
                val activeSpot = repository.getActiveSpotDirect()
                val bridge = WearDataLayerBridge.getInstance(applicationContext)
                bridge.syncParkingSpot(activeSpot)
                val prefs = applicationContext.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                val crownOnRight = prefs.getBoolean("wear_crown_on_right", true)
                bridge.syncCrownPosition(crownOnRight)
            }
        } else if (messageEvent.path == "/car_found" || messageEvent.path == "/mark_found") {
            Log.d("PhoneWearableService", "Received car found message from Wear OS node: ${messageEvent.sourceNodeId}")
            val db = AppDatabase.getDatabase(applicationContext)
            val repository = (applicationContext as? AutoParkApplication)?.repository
                ?: ParkingRepository(db.parkingSpotDao(), db.bluetoothCarDeviceDao())

            scope.launch {
                repository.deactivateActiveSpot()
                val bridge = WearDataLayerBridge.getInstance(applicationContext)
                bridge.syncParkingSpot(null)
            }
        } else if (messageEvent.path == "/save_spot") {
            Log.d("PhoneWearableService", "Received save spot message from Wear OS node: ${messageEvent.sourceNodeId}")
            val db = AppDatabase.getDatabase(applicationContext)
            val repository = (applicationContext as? AutoParkApplication)?.repository
                ?: ParkingRepository(db.parkingSpotDao(), db.bluetoothCarDeviceDao())

            scope.launch {
                var lat = 0.0
                var lng = 0.0
                var alt = 0.0
                var spotName = "Car"
                try {
                    val payload = String(messageEvent.data, Charsets.UTF_8)
                    if (payload.isNotBlank()) {
                        val parts = payload.split(",")
                        if (parts.size >= 2) {
                            lat = parts[0].toDoubleOrNull() ?: 0.0
                            lng = parts[1].toDoubleOrNull() ?: 0.0
                        }
                        if (parts.size >= 3) {
                            alt = parts[2].toDoubleOrNull() ?: 0.0
                        }
                        if (parts.size >= 4 && parts[3].isNotBlank()) {
                            spotName = parts[3]
                        }
                    }
                } catch (_: Exception) {}

                // Fallback to phone's location if watch coordinates were not available
                if (lat == 0.0 && lng == 0.0) {
                    try {
                        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
                        val loc = fusedLocationClient.lastLocation.await()
                        if (loc != null) {
                            lat = loc.latitude
                            lng = loc.longitude
                            alt = loc.altitude
                        }
                    } catch (_: Exception) {}
                }

                if (lat != 0.0 || lng != 0.0) {
                    val newSpot = ParkingSpot(
                        latitude = lat,
                        longitude = lng,
                        altitude = alt,
                        spotName = spotName,
                        address = "Saved from Watch",
                        timestamp = System.currentTimeMillis(),
                        isActive = true
                    )
                    repository.saveNewParkingSpot(newSpot)
                    val bridge = WearDataLayerBridge.getInstance(applicationContext)
                    bridge.syncParkingSpot(newSpot)
                }
            }
        }
    }
}
