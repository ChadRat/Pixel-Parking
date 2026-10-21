package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.geofence.GeofenceAutoParkWorker
import com.example.geofence.GeofenceManager
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Intelligent Receiver for Auto-Parking when the user starts walking after moving in a car.
 * Handles:
 * 1. Geofence perimeter departures (user walks away from parked car perimeter).
 * 2. Activity Recognition transitions (user begins WALKING after IN_VEHICLE or explicit IN_VEHICLE EXIT).
 * 3. Instant developer simulations for testing.
 *
 * CRITICAL RELIABILITY & ARCHITECTURAL RULES:
 * - Always saves the CAR'S location (where vehicle was parked), NOT where the user is walking!
 * - Never holds a manual WakeLock: uses goAsync() solely to capture the event and offloads the
 *   database write, Wear OS sync, and network-dependent Geocoder lookup to an Expedited WorkManager task.
 */
class GeofenceTransitionsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
        private val processLock = Any()
        @Volatile
        private var isProcessing = false
        @Volatile
        private var lastGeofenceAutoSaveTimestamp = 0L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val isSimulated = action == GeofenceManager.ACTION_SIMULATE_GEOFENCE_EXIT
        val isGeofenceTransition = action == GeofenceManager.ACTION_GEOFENCE_TRANSITION
        val isActivityTransition = action == GeofenceManager.ACTION_ACTIVITY_TRANSITION
        val isLocationUpdate = action == GeofenceManager.ACTION_LOCATION_UPDATE

        if (!isSimulated && !isGeofenceTransition && !isActivityTransition && !isLocationUpdate) {
            return
        }

        // --- Handle Background Location Updates from PendingIntent ---
        if (isLocationUpdate && com.google.android.gms.location.LocationResult.hasResult(intent)) {
            val result = com.google.android.gms.location.LocationResult.extractResult(intent)
            val loc = result?.lastLocation
            if (loc != null) {
                GeofenceManager.recordCarLocation(
                    context = context,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    altitude = loc.altitude,
                    accuracy = loc.accuracy,
                    speedMps = if (loc.hasSpeed()) loc.speed else 0f
                )
            }
            return
        }

        val prefs = context.getSharedPreferences(GeofenceManager.PREFS_NAME, Context.MODE_PRIVATE)
        val isGeofenceAutoParkEnabled = prefs.getBoolean(GeofenceManager.KEY_GEOFENCE_ENABLED, false)

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
                                prefs.edit()
                                    .putBoolean(GeofenceManager.KEY_IS_MOVING_IN_CAR, true)
                                    .putBoolean(GeofenceManager.KEY_EXPLICIT_VEHICLE_EXIT, false)
                                    .putLong(GeofenceManager.KEY_STATIONARY_START_TIME, 0L)
                                    .apply()
                                GeofenceManager.removeGeofences(context)
                            } else if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                                Log.d(TAG, "ActivityRecognition: Explicit IN_VEHICLE (EXIT) detected with high confidence (>= 70%). Recording candidate stop.")
                                prefs.edit()
                                    .putBoolean(GeofenceManager.KEY_EXPLICIT_VEHICLE_EXIT, true)
                                    .putLong(GeofenceManager.KEY_CAR_STOP_TIME, now)
                                    .apply()

                                // Capture step counter snapshot at vehicle exit for pedestrian departure telemetry
                                val curSteps = prefs.getInt(GeofenceManager.KEY_CURRENT_STEP_COUNT, -1)
                                if (curSteps >= 0) {
                                    prefs.edit().putInt(GeofenceManager.KEY_STEPS_AT_STOP, curSteps).apply()
                                }

                                // Arm geofence immediately around candidate car stop location
                                val candidateLat = prefs.getFloat(GeofenceManager.KEY_CAR_STOP_LAT, 0f).toDouble()
                                val candidateLng = prefs.getFloat(GeofenceManager.KEY_CAR_STOP_LNG, 0f).toDouble()
                                val lastStopTime = prefs.getLong(GeofenceManager.KEY_CAR_STOP_TIME, 0L)
                                val isCandidateFresh = candidateLat != 0.0 && candidateLng != 0.0 && (now - lastStopTime < 60_000L)

                                if (!isCandidateFresh) {
                                    val asyncPending = goAsync()
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                        try {
                                            val loc = kotlinx.coroutines.withTimeoutOrNull(4000L) {
                                                com.example.sensor.LocationHelper.getLocationForAutoPark(context)
                                                    ?: com.example.sensor.LocationHelper.getCurrentLocation(context)
                                            } ?: com.example.sensor.LocationHelper.lastKnownLocation
                                              ?: com.example.sensor.LocationHelper.getPersistedLocation(context)

                                            if (loc != null && (loc.latitude != 0.0 || loc.longitude != 0.0)) {
                                                val resolvedLat = loc.latitude
                                                val resolvedLng = loc.longitude
                                                prefs.edit()
                                                    .putFloat(GeofenceManager.KEY_CAR_STOP_LAT, resolvedLat.toFloat())
                                                    .putFloat(GeofenceManager.KEY_CAR_STOP_LNG, resolvedLng.toFloat())
                                                    .putFloat(GeofenceManager.KEY_CAR_STOP_ALT, loc.altitude.toFloat())
                                                    .putFloat(GeofenceManager.KEY_CAR_STOP_ACC, loc.accuracy)
                                                    .putLong(GeofenceManager.KEY_CAR_STOP_TIME, System.currentTimeMillis())
                                                    .apply()
                                                Log.d(TAG, "Explicit vehicle exit: Armed geofence around live acquired fix ($resolvedLat, $resolvedLng, ±${loc.accuracy}m).")
                                                GeofenceManager.registerCarPerimeterGeofence(context, resolvedLat, resolvedLng)
                                            }
                                        } finally {
                                            asyncPending.finish()
                                        }
                                    }
                                } else {
                                    Log.d(TAG, "Explicit vehicle exit: Arming geofence perimeter around candidate ($candidateLat, $candidateLng).")
                                    GeofenceManager.registerCarPerimeterGeofence(context, candidateLat, candidateLng)
                                }
                            }
                        }
                        DetectedActivity.WALKING, DetectedActivity.ON_FOOT, DetectedActivity.RUNNING -> {
                            if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                                val wasMovingInCar = prefs.getBoolean(GeofenceManager.KEY_IS_MOVING_IN_CAR, false)
                                val isArmed = prefs.getBoolean(GeofenceManager.KEY_IS_GEOFENCE_ARMED, false)
                                val stopTime = prefs.getLong(GeofenceManager.KEY_CAR_STOP_TIME, 0L)
                                val recentStop = (now - stopTime) < 30 * 60 * 1000L // within last 30 mins

                                Log.d(TAG, "ActivityRecognition: Started walking/running. wasMovingInCar=$wasMovingInCar, isArmed=$isArmed, recentStop=$recentStop")
                                if (wasMovingInCar || isArmed || recentStop) {
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
                } else {
                    val transitionType = geofencingEvent.geofenceTransition
                    if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT || transitionType == Geofence.GEOFENCE_TRANSITION_DWELL) {
                        Log.d(TAG, "Geofence EXIT/DWELL transition detected (user walked out of car perimeter).")
                        isExitTransition = true
                    } else {
                        Log.d(TAG, "Ignored non-exit geofence transition type: $transitionType")
                    }
                }
            } else {
                // If geofencingEvent is null but ACTION_GEOFENCE_TRANSITION was received, treat as valid exit
                isExitTransition = true
            }
        }

        if (!isExitTransition && !isActivityTransition) return

        // Debounce: prevent duplicate saves (45s for live walking/geofence, 3s for simulation / forced trigger)
        val now = System.currentTimeMillis()
        val debounceWindow = if (isSimulated || isForcedTrigger) 3_000L else 45_000L
        synchronized(processLock) {
            if (isProcessing) {
                Log.d(TAG, "Dropping geofence/walking event: another event is currently being processed.")
                return
            }
            if (now - lastGeofenceAutoSaveTimestamp < debounceWindow) {
                Log.d(TAG, "Debounced duplicate walking/geofence auto-save event ($debounceWindow ms).")
                return
            }
            isProcessing = true
            lastGeofenceAutoSaveTimestamp = now
        }

        // Use goAsync() solely to capture the event and offload the database write, Wear OS sync,
        // and network-dependent Geocoder lookup to an Expedited WorkManager task.
        val pendingResult = goAsync()
        try {
            // Extract coordinates to pass to worker
            val simCarLat = intent.getDoubleExtra("car_lat", 0.0)
            val simCarLng = intent.getDoubleExtra("car_lng", 0.0)

            val candidateLat = prefs.getFloat(GeofenceManager.KEY_CAR_STOP_LAT, 0f).toDouble()
            val candidateLng = prefs.getFloat(GeofenceManager.KEY_CAR_STOP_LNG, 0f).toDouble()
            val candidateAlt = prefs.getFloat(GeofenceManager.KEY_CAR_STOP_ALT, 0f).toDouble()
            val candidateAcc = prefs.getFloat(GeofenceManager.KEY_CAR_STOP_ACC, 10f)

            val centerLat = prefs.getFloat(GeofenceManager.KEY_GEOFENCE_CENTER_LAT, 0f).toDouble()
            val centerLng = prefs.getFloat(GeofenceManager.KEY_GEOFENCE_CENTER_LNG, 0f).toDouble()

            var carLat: Double = 0.0
            var carLng: Double = 0.0
            var carAlt: Double = 0.0
            var carAcc: Float = 10f

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
                val loc = com.example.sensor.LocationHelper.lastKnownLocation
                    ?: com.example.sensor.LocationHelper.getPersistedLocation(context)
                if (loc != null && (loc.latitude != 0.0 || loc.longitude != 0.0)) {
                    carLat = loc.latitude
                    carLng = loc.longitude
                    carAlt = loc.altitude
                    carAcc = loc.accuracy
                }
            }

            val isUnderground = prefs.getBoolean(GeofenceManager.KEY_UNDERGROUND_FALLBACK_ACTIVE, false)
            val undergroundOrigAcc = prefs.getFloat(GeofenceManager.KEY_UNDERGROUND_ORIGINAL_ACC, 0f)
            val undergroundFallbackAcc = prefs.getFloat(GeofenceManager.KEY_UNDERGROUND_FALLBACK_ACC, 0f)

            val workData = workDataOf(
                GeofenceAutoParkWorker.KEY_CAR_LAT to carLat,
                GeofenceAutoParkWorker.KEY_CAR_LNG to carLng,
                GeofenceAutoParkWorker.KEY_CAR_ALT to carAlt,
                GeofenceAutoParkWorker.KEY_CAR_ACC to carAcc,
                GeofenceAutoParkWorker.KEY_IS_SIMULATED to isSimulated,
                GeofenceAutoParkWorker.KEY_UNDERGROUND_FALLBACK_ACTIVE to isUnderground,
                GeofenceAutoParkWorker.KEY_UNDERGROUND_ORIGINAL_ACC to undergroundOrigAcc,
                GeofenceAutoParkWorker.KEY_UNDERGROUND_FALLBACK_ACC to undergroundFallbackAcc
            )

            val workRequest = OneTimeWorkRequestBuilder<GeofenceAutoParkWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(workData)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d(TAG, "Enqueued expedited GeofenceAutoParkWorker task for coordinates ($carLat, $carLng)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue GeofenceAutoParkWorker: ${e.message}", e)
        } finally {
            synchronized(processLock) {
                isProcessing = false
            }
            pendingResult.finish()
        }
    }
}
