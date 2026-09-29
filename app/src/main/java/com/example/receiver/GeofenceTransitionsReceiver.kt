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
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Intelligent Receiver for Auto-Parking when the user starts walking after moving in a car.
 * Handles:
 * 1. Geofence perimeter departures (user walks away from parked car perimeter).
 * 2. Activity Recognition transitions (user begins WALKING after IN_VEHICLE).
 * 3. Instant developer simulations for testing.
 *
 * CRITICAL RULE: Always saves the CAR'S location (where vehicle was parked), NOT where the user
 * is currently standing after walking away!
 */
class GeofenceTransitionsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
        private var lastGeofenceAutoSaveTimestamp = 0L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val isSimulated = action == GeofenceManager.ACTION_SIMULATE_GEOFENCE_EXIT
        val isGeofenceTransition = action == GeofenceManager.ACTION_GEOFENCE_TRANSITION
        val isActivityTransition = action == GeofenceManager.ACTION_ACTIVITY_TRANSITION

        if (!isSimulated && !isGeofenceTransition && !isActivityTransition) {
            return
        }

        val prefs = context.getSharedPreferences(GeofenceManager.PREFS_NAME, Context.MODE_PRIVATE)
        val isGeofenceAutoParkEnabled = prefs.getBoolean(GeofenceManager.KEY_GEOFENCE_ENABLED, true)

        if (!isGeofenceAutoParkEnabled && !isSimulated) {
            Log.d(TAG, "Geofence auto-park is disabled in settings. Ignoring event.")
            return
        }

        // --- Handle Activity Recognition Transitions ---
        if (isActivityTransition && ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            if (result != null) {
                var shouldTriggerWalkAfterCar = false
                val now = System.currentTimeMillis()

                for (event in result.transitionEvents) {
                    when (event.activityType) {
                        DetectedActivity.IN_VEHICLE -> {
                            if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                                Log.d(TAG, "ActivityRecognition: Entered vehicle. Marking moving in car.")
                                prefs.edit().putBoolean(GeofenceManager.KEY_IS_MOVING_IN_CAR, true).apply()
                            } else if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                                Log.d(TAG, "ActivityRecognition: Exited vehicle. Recording car candidate stop.")
                                prefs.edit()
                                    .putLong(GeofenceManager.KEY_CAR_STOP_TIME, now)
                                    .apply()
                            }
                        }
                        DetectedActivity.WALKING, DetectedActivity.ON_FOOT -> {
                            if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                                val wasMovingInCar = prefs.getBoolean(GeofenceManager.KEY_IS_MOVING_IN_CAR, false)
                                val stopTime = prefs.getLong(GeofenceManager.KEY_CAR_STOP_TIME, 0L)
                                val recentStop = (now - stopTime) < 15 * 60 * 1000L // within last 15 mins

                                Log.d(TAG, "ActivityRecognition: Started walking. wasMovingInCar=$wasMovingInCar, recentStop=$recentStop")
                                if (wasMovingInCar || recentStop) {
                                    shouldTriggerWalkAfterCar = true
                                }
                            }
                        }
                    }
                }

                if (!shouldTriggerWalkAfterCar) {
                    return
                }
            }
        }

        // --- Handle Geofence Transitions ---
        val isForcedTrigger = intent.getBooleanExtra("force_trigger", false)
        var isExitTransition = isSimulated || isForcedTrigger
        if (isGeofenceTransition && !isForcedTrigger) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            if (geofencingEvent != null) {
                if (geofencingEvent.hasError()) {
                    val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
                    Log.e(TAG, "Geofencing event error: $errorMessage (code ${geofencingEvent.errorCode})")
                    return
                }

                val transitionType = geofencingEvent.geofenceTransition
                if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    Log.d(TAG, "Geofence EXIT transition detected (user walked out of car perimeter).")
                    isExitTransition = true
                } else {
                    Log.d(TAG, "Ignored non-exit geofence transition type: $transitionType")
                    return
                }
            }
        }

        if (!isExitTransition && !isActivityTransition) return

        // Debounce: prevent duplicate saves within 45 seconds
        val now = System.currentTimeMillis()
        if (now - lastGeofenceAutoSaveTimestamp < 45_000L && !isSimulated) {
            Log.d(TAG, "Debounced duplicate walking/geofence auto-save event.")
            return
        }
        lastGeofenceAutoSaveTimestamp = now

        val pendingResult = goAsync()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PixelParking:GeofenceWalkingWakeLock"
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

                    // 1. Resolve THE CAR'S LOCATION (where the vehicle stopped, NOT where the user is walking!)
                    val simCarLat = intent.getDoubleExtra("car_lat", 0.0)
                    val simCarLng = intent.getDoubleExtra("car_lng", 0.0)

                    val candidateLat = prefs.getFloat(GeofenceManager.KEY_CAR_STOP_LAT, 0f).toDouble()
                    val candidateLng = prefs.getFloat(GeofenceManager.KEY_CAR_STOP_LNG, 0f).toDouble()
                    val candidateAlt = prefs.getFloat(GeofenceManager.KEY_CAR_STOP_ALT, 0f).toDouble()
                    val candidateAcc = prefs.getFloat(GeofenceManager.KEY_CAR_STOP_ACC, 10f)

                    val centerLat = prefs.getFloat(GeofenceManager.KEY_GEOFENCE_CENTER_LAT, 0f).toDouble()
                    val centerLng = prefs.getFloat(GeofenceManager.KEY_GEOFENCE_CENTER_LNG, 0f).toDouble()

                    val carLat: Double
                    val carLng: Double
                    val carAlt: Double
                    val carAcc: Float

                    if ((isSimulated || isForcedTrigger) && simCarLat != 0.0 && simCarLng != 0.0) {
                        carLat = simCarLat
                        carLng = simCarLng
                        carAlt = 0.0
                        carAcc = 10f
                    } else if (candidateLat != 0.0 && candidateLng != 0.0) {
                        carLat = candidateLat
                        carLng = candidateLng
                        carAlt = candidateAlt
                        carAcc = candidateAcc
                    } else if (centerLat != 0.0 && centerLng != 0.0) {
                        carLat = centerLat
                        carLng = centerLng
                        carAlt = 0.0
                        carAcc = 15f
                    } else {
                        // Fallback to location fix if no prior car stop recorded
                        val loc = LocationHelper.getLocationForAutoPark(context)
                            ?: LocationHelper.getCurrentLocation(context)
                        if (loc != null && (loc.latitude != 0.0 || loc.longitude != 0.0)) {
                            carLat = loc.latitude
                            carLng = loc.longitude
                            carAlt = loc.altitude
                            carAcc = loc.accuracy
                        } else {
                            Log.w(TAG, "No valid car location fix available for auto-park.")
                            return@withTimeoutOrNull
                        }
                    }

                    // 2. Resolve address of the car's parked location
                    val address = LocationHelper.getAddressFromCoordinates(context, carLat, carLng)
                    val strings = context.getAppStrings()

                    // 3. Create & Save Parking Spot
                    val spot = ParkingSpot(
                        latitude = carLat,
                        longitude = carLng,
                        altitude = carAlt,
                        accuracyMeters = carAcc,
                        address = address,
                        spotName = strings.geofenceSavedSpotDefaultName,
                        floorLevel = strings.floorGroundLevel,
                        note = strings.geofenceExitNote,
                        timestamp = System.currentTimeMillis(),
                        isAutoSaved = true,
                        bluetoothDeviceName = "Walking Detection",
                        bluetoothDeviceAddress = "GEOFENCE_WALKING_DETECTION",
                        meterExpiryTimestamp = null,
                        photoUri = null,
                        isActive = true
                    )

                    val newId = repository.saveNewParkingSpot(spot)
                    val savedSpot = spot.copy(id = newId)
                    Log.d(TAG, "Successfully auto-saved CAR LOCATION upon walking detection at ($carLat, $carLng) with ID: $newId")

                    // 4. Mark that trip is complete, reset moving in car flag
                    prefs.edit()
                        .putBoolean(GeofenceManager.KEY_IS_MOVING_IN_CAR, false)
                        .apply()

                    // 5. Remove or rearm perimeter geofence
                    GeofenceManager.removeGeofences(context)

                    // 6. Sync active spot to Wear OS companion app
                    try {
                        com.example.wear.WearDataLayerBridge.getInstance(context).syncParkingSpot(savedSpot)
                    } catch (_: Throwable) {}

                    // 7. Show rich system notification with Instant Navigate action
                    ParkingNotificationHelper.showCarParkedNotification(context, savedSpot)

                    // 8. Haptic vibration confirmation
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
                Log.e(TAG, "Error handling walking auto-save: ${e.message}", e)
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
