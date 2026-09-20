package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.entity.ParkingSpot
import com.example.data.repository.ParkingRepository
import com.example.geofence.GeofenceManager
import com.example.notification.ParkingNotificationHelper
import com.example.sensor.LocationHelper
import com.example.ui.i18n.getAppStrings
import com.example.util.HapticHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class GeofenceTransitionsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
        private var lastGeofenceAutoSaveTimestamp = 0L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val isSimulated = action == GeofenceManager.ACTION_SIMULATE_GEOFENCE_EXIT
        val isGeofenceTransition = action == GeofenceManager.ACTION_GEOFENCE_TRANSITION

        if (!isSimulated && !isGeofenceTransition) {
            return
        }

        val prefs = context.getSharedPreferences("auto_park_prefs", Context.MODE_PRIVATE)
        val isGeofenceAutoParkEnabled = prefs.getBoolean("geofence_auto_park_enabled", false)

        if (!isGeofenceAutoParkEnabled && !isSimulated) {
            Log.d(TAG, "Geofence auto-park is disabled in Developer settings. Ignoring event.")
            return
        }

        var isExitTransition = isSimulated
        var triggeringLat: Double? = null
        var triggeringLng: Double? = null

        if (isGeofenceTransition) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            if (geofencingEvent != null) {
                if (geofencingEvent.hasError()) {
                    val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
                    Log.e(TAG, "Geofencing event error: $errorMessage (code ${geofencingEvent.errorCode})")
                    return
                }

                val transitionType = geofencingEvent.geofenceTransition
                if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    isExitTransition = true
                    val trigLoc = geofencingEvent.triggeringLocation
                    if (trigLoc != null && (trigLoc.latitude != 0.0 || trigLoc.longitude != 0.0)) {
                        triggeringLat = trigLoc.latitude
                        triggeringLng = trigLoc.longitude
                    }
                } else {
                    Log.d(TAG, "Ignored non-exit geofence transition type: $transitionType")
                    return
                }
            }
        }

        if (!isExitTransition) return

        // Debounce: prevent saving multiple times within 45 seconds
        val now = System.currentTimeMillis()
        if (now - lastGeofenceAutoSaveTimestamp < 45_000L && !isSimulated) {
            Log.d(TAG, "Debounced duplicate geofence exit event.")
            return
        }
        lastGeofenceAutoSaveTimestamp = now

        val pendingResult = goAsync()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PixelParking:GeofenceExitWakeLock"
        )
        wakeLock?.acquire(15_000L)

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                withTimeoutOrNull(12_000L) {
                    val db = AppDatabase.getDatabase(context)
                    val repository = ParkingRepository(
                        db.parkingSpotDao(),
                        db.bluetoothCarDeviceDao()
                    )

                    // 1. Resolve coordinates
                    val simLat = intent.getDoubleExtra("simulated_lat", 0.0)
                    val simLng = intent.getDoubleExtra("simulated_lng", 0.0)

                    val lat: Double
                    val lng: Double
                    val alt: Double
                    val accuracy: Float

                    if (isSimulated && simLat != 0.0 && simLng != 0.0) {
                        lat = simLat
                        lng = simLng
                        alt = 0.0
                        accuracy = 10f
                    } else if (triggeringLat != null && triggeringLng != null) {
                        lat = triggeringLat
                        lng = triggeringLng
                        alt = 0.0
                        accuracy = 15f
                    } else {
                        // Obtain best current location from LocationHelper
                        val location = LocationHelper.getLocationForAutoPark(context)
                            ?: LocationHelper.getCurrentLocation(context)

                        if (location != null && (location.latitude != 0.0 || location.longitude != 0.0)) {
                            lat = location.latitude
                            lng = location.longitude
                            alt = location.altitude
                            accuracy = location.accuracy
                        } else {
                            // Fallback to center of armed geofence if recorded
                            val centerLat = prefs.getFloat("geofence_center_lat", 0f).toDouble()
                            val centerLng = prefs.getFloat("geofence_center_lng", 0f).toDouble()
                            if (centerLat != 0.0 && centerLng != 0.0) {
                                lat = centerLat
                                lng = centerLng
                                alt = 0.0
                                accuracy = 25f
                            } else {
                                val previousSpot = repository.getActiveSpotDirect()
                                    ?: repository.getMostRecentSpotDirect()
                                if (previousSpot != null && (previousSpot.latitude != 0.0 || previousSpot.longitude != 0.0)) {
                                    lat = previousSpot.latitude
                                    lng = previousSpot.longitude
                                    alt = previousSpot.altitude
                                    accuracy = 25f
                                } else {
                                    Log.w(TAG, "No valid location fix available for geofence exit auto-park.")
                                    return@withTimeoutOrNull
                                }
                            }
                        }
                    }

                    // 2. Resolve address
                    val address = LocationHelper.getAddressFromCoordinates(context, lat, lng)
                    val strings = context.getAppStrings()

                    // 3. Create Parking Spot
                    val spot = ParkingSpot(
                        latitude = lat,
                        longitude = lng,
                        altitude = alt,
                        accuracyMeters = accuracy,
                        address = address,
                        spotName = strings.geofenceSavedSpotDefaultName,
                        floorLevel = strings.floorGroundLevel,
                        note = strings.geofenceExitNote,
                        timestamp = System.currentTimeMillis(),
                        isAutoSaved = true,
                        bluetoothDeviceName = "Geofence Perimeter",
                        bluetoothDeviceAddress = "GEOFENCE_AUTO_DETECTION",
                        meterExpiryTimestamp = null,
                        photoUri = null,
                        isActive = true
                    )

                    val newId = repository.saveNewParkingSpot(spot)
                    val savedSpot = spot.copy(id = newId)
                    Log.d(TAG, "Successfully auto-saved parking location via Geofence exit at ($lat, $lng) with ID: $newId")

                    // 4. Show rich notification with instant navigation actions
                    ParkingNotificationHelper.showCarParkedNotification(context, savedSpot)

                    // 5. Trigger haptic pulse
                    HapticHelper.getVibrator(context)?.let { vibrator ->
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(
                                android.os.VibrationEffect.createWaveform(
                                    longArrayOf(0, 150, 100, 200),
                                    -1
                                )
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(longArrayOf(0, 150, 100, 200), -1)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling geofence auto-save: ${e.message}", e)
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (_: Exception) {}
                pendingResult.finish()
            }
        }
    }
}
