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
        val isActivityUpdate = action == GeofenceManager.ACTION_ACTIVITY_UPDATE
        val isLocationUpdate = action == GeofenceManager.ACTION_LOCATION_UPDATE
        val isForcedTrigger = intent.getBooleanExtra("force_trigger", false)

        if (!isSimulated && !isGeofenceTransition && !isActivityTransition && !isActivityUpdate && !isLocationUpdate) {
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

        // Car Bluetooth Connection Safeguard:
        // If phone is currently connected to car Bluetooth, user is inside the vehicle!
        if (!isSimulated && com.example.util.BluetoothDeviceHelper.isAnyCarBluetoothConnected(context)) {
            Log.d(TAG, "Geofence event suppressed: phone is connected to car Bluetooth (user is in vehicle).")
            GeofenceManager.removeGeofences(context)
            return
        }

        // --- Handle Android Activity Recognition Updates ---
        if ((isActivityUpdate || isActivityTransition) && com.google.android.gms.location.ActivityRecognitionResult.hasResult(intent)) {
            val result = com.google.android.gms.location.ActivityRecognitionResult.extractResult(intent)
            if (result != null) {
                GeofenceManager.processActivityRecognitionResult(context, result)
            }
        }

        // --- Handle Android Activity Transitions ---
        if (isActivityTransition && ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            if (result != null) {
                GeofenceManager.processActivityTransitionResult(context, result)
            }
        }

        // --- Handle Geofence Transitions ---
        var isExitTransition = isSimulated || isForcedTrigger
        if (isGeofenceTransition && !isForcedTrigger && !isSimulated) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            if (geofencingEvent != null) {
                if (geofencingEvent.hasError()) {
                    val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
                    Log.e(TAG, "Geofencing event error: $errorMessage (code ${geofencingEvent.errorCode})")
                    return
                } else {
                    val transitionType = geofencingEvent.geofenceTransition
                    if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT || transitionType == Geofence.GEOFENCE_TRANSITION_DWELL) {
                        Log.d(TAG, "Geofence EXIT/DWELL transition detected (user moved out of car perimeter).")
                        // CRITICAL: Verify with Android Activity API so that driving away does not misfire!
                        val isWalking = GeofenceManager.isWalkingAccordingToActivityApi(context)
                        if (!isWalking) {
                            val curSteps = prefs.getInt(GeofenceManager.KEY_CURRENT_STEP_COUNT, -1)
                            val stepsAtStop = prefs.getInt(GeofenceManager.KEY_STEPS_AT_STOP, -1)
                            val stepsTaken = if (stepsAtStop >= 0 && curSteps >= stepsAtStop) (curSteps - stepsAtStop) else 0
                            if (stepsTaken >= 8) {
                                prefs.edit()
                                    .putBoolean(GeofenceManager.KEY_IS_CURRENTLY_WALKING, true)
                                    .putLong(GeofenceManager.KEY_LAST_WALKING_ACTIVITY_TIME, System.currentTimeMillis())
                                    .putInt(GeofenceManager.KEY_LAST_DETECTED_ACTIVITY_TYPE, DetectedActivity.WALKING)
                                    .apply()
                                isExitTransition = true
                            } else {
                                Log.w(TAG, "Geofence perimeter exit detected, but Android Activity API does NOT say the user is walking (likely in vehicle/driving away). Suppressing misfire.")
                                prefs.edit().putBoolean(GeofenceManager.KEY_IS_MOVING_IN_CAR, true).apply()
                                GeofenceManager.removeGeofences(context)
                                return
                            }
                        } else {
                            isExitTransition = true
                        }
                    } else {
                        Log.d(TAG, "Ignored non-exit geofence transition type: $transitionType")
                    }
                }
            } else {
                // If geofencingEvent is null but ACTION_GEOFENCE_TRANSITION was received, verify with Activity API
                if (GeofenceManager.isWalkingAccordingToActivityApi(context)) {
                    isExitTransition = true
                }
            }
        }

        if (!isExitTransition) return

        // CRITICAL REQUIREMENT: Save the location ONLY when the Android Activity API says that the user is walking
        if (!isSimulated && !GeofenceManager.isWalkingAccordingToActivityApi(context)) {
            Log.w(TAG, "Auto-save blocked: Android Activity API does NOT say that the user is walking.")
            return
        }

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
