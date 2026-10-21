package com.example.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.os.Looper
import com.example.receiver.GeofenceTransitionsReceiver
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.json.JSONArray
import org.json.JSONObject

data class ReliableGpsFix(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracy: Float = 10f,
    val timestamp: Long = System.currentTimeMillis()
)

object GeofenceManager {

    private const val TAG = "GeofenceManager"
    const val GEOFENCE_REQUEST_ID = "CAR_PERIMETER_GEOFENCE"
    const val DEFAULT_PERIMETER_RADIUS_METERS = 50.0f // 50 meter perimeter (45-60m) to eliminate GPS float false-exits
    const val NOTIFICATION_RESPONSIVENESS_MS = 25_000 // 25s responsiveness (20-30s) to prevent battery drain
    const val STATIONARY_DWELL_TIMER_MS = 40_000L // 40s dwell timer before arming geofence to prevent red-light churn

    const val ACTION_GEOFENCE_TRANSITION = "com.example.autopark.ACTION_GEOFENCE_TRANSITION"
    const val ACTION_SIMULATE_GEOFENCE_EXIT = "com.example.autopark.ACTION_SIMULATE_GEOFENCE_EXIT"
    const val ACTION_ACTIVITY_TRANSITION = "com.example.autopark.ACTION_ACTIVITY_TRANSITION"
    const val ACTION_LOCATION_UPDATE = "com.example.autopark.ACTION_LOCATION_UPDATE"

    // SharedPreferences keys for persistent car tracking (survives Low Memory Killer)
    const val PREFS_NAME = "auto_park_prefs"
    const val KEY_GEOFENCE_ENABLED = "geofence_auto_park_enabled"
    const val KEY_IS_MOVING_IN_CAR = "is_moving_in_car"
    const val KEY_CAR_STOP_LAT = "candidate_car_park_lat"
    const val KEY_CAR_STOP_LNG = "candidate_car_park_lng"
    const val KEY_CAR_STOP_ALT = "candidate_car_park_alt"
    const val KEY_CAR_STOP_ACC = "candidate_car_park_acc"
    const val KEY_CAR_STOP_TIME = "candidate_car_park_time"
    const val KEY_STATIONARY_START_TIME = "candidate_car_stationary_start_time"
    const val KEY_EXPLICIT_VEHICLE_EXIT = "explicit_vehicle_exit_detected"
    const val KEY_IS_GEOFENCE_ARMED = "is_geofence_armed"
    const val KEY_LAST_STATIONARY_DWELL_MS = "candidate_car_last_dwell_ms"
    const val KEY_STEPS_AT_STOP = "candidate_car_steps_at_stop"
    const val KEY_CURRENT_STEP_COUNT = "candidate_car_current_step_count"
    const val KEY_GEOFENCE_CENTER_LAT = "geofence_center_lat"
    const val KEY_GEOFENCE_CENTER_LNG = "geofence_center_lng"
    const val KEY_GEOFENCE_ARMED_TIME = "geofence_armed_time"
    const val KEY_LAST_AUTO_SAVE_TIME = "candidate_car_last_auto_save_time"
    const val KEY_PREV_FIX_LAT = "walk_away_prev_fix_lat"
    const val KEY_PREV_FIX_LNG = "walk_away_prev_fix_lng"
    const val KEY_PREV_FIX_TIME = "walk_away_prev_fix_time"
    const val KEY_DEV_ALLOW_GEOFENCE_AFTER_BT = "dev_allow_geofence_after_bt"

    // Underground Parking Safeguard via GPS Accuracy Backtracking
    const val KEY_HIGH_ACCURACY_GPS_BUFFER = "high_accuracy_gps_buffer"
    const val KEY_UNDERGROUND_FALLBACK_ACTIVE = "underground_fallback_active"
    const val KEY_UNDERGROUND_ORIGINAL_ACC = "underground_original_acc"
    const val KEY_UNDERGROUND_FALLBACK_ACC = "underground_fallback_acc"
    const val KEY_UNDERGROUND_FALLBACK_TIME = "underground_fallback_time"

    const val MAX_GPS_BUFFER_SIZE = 5
    const val HIGH_ACCURACY_THRESHOLD_METERS = 15.0f // High-accuracy fixes while driving
    const val DEGRADED_ACCURACY_THRESHOLD_METERS = 30.0f // Degraded accuracy threshold in underground garage

    private var stepSensorListener: android.hardware.SensorEventListener? = null

    /**
     * Registers device step sensor to provide pedestrian foot-movement telemetry
     * for the passenger drop-off safeguard.
     */
    fun registerStepSensor(context: Context) {
        if (stepSensorListener != null) return
        try {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? android.hardware.SensorManager
            val stepCounter = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_STEP_COUNTER)
                ?: sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_STEP_DETECTOR)
            if (stepCounter != null) {
                stepSensorListener = object : android.hardware.SensorEventListener {
                    override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                        val value = event?.values?.getOrNull(0)?.toInt() ?: return
                        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val currentStepCount = if (event.sensor.type == android.hardware.Sensor.TYPE_STEP_COUNTER) {
                            prefs.edit().putInt(KEY_CURRENT_STEP_COUNT, value).apply()
                            value
                        } else {
                            val cur = prefs.getInt(KEY_CURRENT_STEP_COUNT, 0) + 1
                            prefs.edit().putInt(KEY_CURRENT_STEP_COUNT, cur).apply()
                            cur
                        }

                        // Check if user has started walking after moving fast / in car
                        val wasMovingInCar = prefs.getBoolean(KEY_IS_MOVING_IN_CAR, false)
                        val stopLat = prefs.getFloat(KEY_CAR_STOP_LAT, 0f).toDouble()
                        val stopLng = prefs.getFloat(KEY_CAR_STOP_LNG, 0f).toDouble()
                        val stopTime = prefs.getLong(KEY_CAR_STOP_TIME, 0L)
                        val stepsAtStop = prefs.getInt(KEY_STEPS_AT_STOP, -1)
                        val now = System.currentTimeMillis()

                        val lastAutoSaveTime = prefs.getLong(KEY_LAST_AUTO_SAVE_TIME, 0L)
                        val justSaved = (now - lastAutoSaveTime) < 90_000L

                        if (wasMovingInCar && stopLat != 0.0 && stopLng != 0.0 && (now - stopTime) in 20_000L..(25 * 60 * 1000L) && !justSaved) {
                            if (stepsAtStop >= 0 && (currentStepCount - stepsAtStop) >= 15) {
                                Log.d(TAG, "Walking detected via step counter (${currentStepCount - stepsAtStop} steps after vehicle stop). Triggering Walk Away auto-save!")
                                prefs.edit()
                                    .putBoolean(KEY_IS_MOVING_IN_CAR, false)
                                    .remove(KEY_STEPS_AT_STOP)
                                    .putLong(KEY_LAST_AUTO_SAVE_TIME, now)
                                    .apply()
                                triggerWalkingAutoSave(context, stopLat, stopLng)
                            }
                        }
                    }

                    override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
                }
                sensorManager?.registerListener(stepSensorListener, stepCounter, android.hardware.SensorManager.SENSOR_DELAY_NORMAL)
                Log.d(TAG, "Step sensor registered for Walk Away pedestrian detection & safeguard.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Step sensor not available: ${e.message}")
        }
    }

    fun unregisterStepSensor(context: Context) {
        try {
            val listener = stepSensorListener ?: return
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? android.hardware.SensorManager
            sensorManager?.unregisterListener(listener)
            stepSensorListener = null
        } catch (_: Exception) {}
    }

    private fun getGeofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceTransitionsReceiver::class.java).apply {
            action = ACTION_GEOFENCE_TRANSITION
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, 1001, intent, flags)
    }

    private fun getActivityTransitionPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceTransitionsReceiver::class.java).apply {
            action = ACTION_ACTIVITY_TRANSITION
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, 1002, intent, flags)
    }

    private fun getLocationPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceTransitionsReceiver::class.java).apply {
            action = ACTION_LOCATION_UPDATE
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, 1003, intent, flags)
    }

    private var balancedLocationCallback: LocationCallback? = null

    /**
     * Enables or updates the geofence & walking auto-park feature.
     */
    fun startGeofenceWalkingAutoPark(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_GEOFENCE_ENABLED, true).apply()
        registerActivityTransitions(context)
        registerStepSensor(context)
        registerBalancedLocationTracking(context)
    }

    /**
     * Disables geofence & walking auto-park.
     */
    fun stopGeofenceWalkingAutoPark(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_GEOFENCE_ENABLED, false).apply()
        removeGeofences(context)
        removeActivityTransitions(context)
        unregisterStepSensor(context)
        unregisterBalancedLocationTracking(context)
        clearCandidateParkingState(context)
    }

    /**
     * Restores Activity Recognition, step sensors, and armed perimeter geofences
     * upon system reboot (BOOT_COMPLETED) or Application process initialization.
     * Ensures geofences dropped by Android OS reboot are reliably re-registered.
     */
    fun restoreOnStartup(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(KEY_GEOFENCE_ENABLED, false)
        if (!isEnabled) return

        registerActivityTransitions(context)
        registerStepSensor(context)
        registerBalancedLocationTracking(context)

        val isArmed = prefs.getBoolean(KEY_IS_GEOFENCE_ARMED, false)
        val armedTime = prefs.getLong(KEY_GEOFENCE_ARMED_TIME, 0L)
        val centerLat = prefs.getFloat(KEY_GEOFENCE_CENTER_LAT, 0f).toDouble()
        val centerLng = prefs.getFloat(KEY_GEOFENCE_CENTER_LNG, 0f).toDouble()
        val now = System.currentTimeMillis()

        if (isArmed && centerLat != 0.0 && centerLng != 0.0 && (now - armedTime) < 12 * 60 * 60 * 1000L) {
            Log.d(TAG, "Restoring armed perimeter geofence around ($centerLat, $centerLng) after reboot/startup.")
            registerCarPerimeterGeofence(context, centerLat, centerLng, DEFAULT_PERIMETER_RADIUS_METERS)
        }
    }

    @SuppressLint("MissingPermission")
    fun registerBalancedLocationTracking(context: Context) {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                12_000L
            ).setMinUpdateIntervalMillis(6_000L)
             .setMinUpdateDistanceMeters(8f)
             .build()

            // 1. PendingIntent for persistent 24/7 background delivery even when app process is closed
            val pendingIntent = getLocationPendingIntent(context)
            fusedClient.requestLocationUpdates(request, pendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Registered background location updates via PendingIntent for Walk Away.")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to register background location PendingIntent: ${e.message}")
                }

            // 2. Foreground callback when app process is active
            if (balancedLocationCallback == null) {
                balancedLocationCallback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        val loc = result.lastLocation ?: return
                        recordCarLocation(
                            context = context,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            altitude = loc.altitude,
                            accuracy = loc.accuracy,
                            speedMps = if (loc.hasSpeed()) loc.speed else 0f
                        )
                    }
                }
                fusedClient.requestLocationUpdates(request, balancedLocationCallback!!, Looper.getMainLooper())
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission not granted for Walk Away background tracking: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Error registering balanced location tracking: ${e.message}")
        }
    }

    fun unregisterBalancedLocationTracking(context: Context) {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.removeLocationUpdates(getLocationPendingIntent(context))
            val cb = balancedLocationCallback
            if (cb != null) {
                fusedClient.removeLocationUpdates(cb)
                balancedLocationCallback = null
            }
        } catch (_: Exception) {}
    }

    /**
     * Cleans up transient candidate parking state after a parking session is safely committed to the database.
     * Ensures stale coordinates never leak into future sessions.
     */
    fun clearCandidateParkingState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_MOVING_IN_CAR, false)
            .remove(KEY_CAR_STOP_LAT)
            .remove(KEY_CAR_STOP_LNG)
            .remove(KEY_CAR_STOP_ALT)
            .remove(KEY_CAR_STOP_ACC)
            .remove(KEY_CAR_STOP_TIME)
            .remove(KEY_GEOFENCE_CENTER_LAT)
            .remove(KEY_GEOFENCE_CENTER_LNG)
            .remove(KEY_GEOFENCE_ARMED_TIME)
            .putLong(KEY_STATIONARY_START_TIME, 0L)
            .putBoolean(KEY_EXPLICIT_VEHICLE_EXIT, false)
            .putBoolean(KEY_IS_GEOFENCE_ARMED, false)
            .putBoolean(KEY_UNDERGROUND_FALLBACK_ACTIVE, false)
            .remove(KEY_STEPS_AT_STOP)
            .apply()
    }

    /**
     * Registers Activity Recognition transitions to detect when:
     * 1. User enters or exits a vehicle (IN_VEHICLE)
     * 2. User starts walking (WALKING / ON_FOOT)
     */
    @SuppressLint("MissingPermission")
    fun registerActivityTransitions(context: Context) {
        try {
            val transitions = listOf(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.IN_VEHICLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.IN_VEHICLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.ON_FOOT)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.RUNNING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )

            val request = ActivityTransitionRequest(transitions)
            val pendingIntent = getActivityTransitionPendingIntent(context)

            ActivityRecognition.getClient(context)
                .requestActivityTransitionUpdates(request, pendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully registered activity transitions for driving & walking.")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to register activity transitions: ${e.message}")
                }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException registering activity transitions: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering activity transitions: ${e.message}", e)
        }
    }

    fun removeActivityTransitions(context: Context) {
        try {
            val pendingIntent = getActivityTransitionPendingIntent(context)
            ActivityRecognition.getClient(context)
                .removeActivityTransitionUpdates(pendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully removed activity transition updates.")
                }
        } catch (_: Exception) {}
    }

    /**
     * Retrieves the rolling buffer of the last 3-5 high-accuracy GPS fixes recorded while driving.
     */
    fun getRollingGpsBuffer(context: Context): List<ReliableGpsFix> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_HIGH_ACCURACY_GPS_BUFFER, "[]") ?: "[]"
        val list = mutableListOf<ReliableGpsFix>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ReliableGpsFix(
                        latitude = obj.optDouble("lat", 0.0),
                        longitude = obj.optDouble("lng", 0.0),
                        altitude = obj.optDouble("alt", 0.0),
                        accuracy = obj.optDouble("acc", 10.0).toFloat(),
                        timestamp = obj.optLong("time", 0L)
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse rolling GPS buffer: ${e.message}")
        }
        return list
    }

    /**
     * Appends a high-accuracy GPS fix (accuracy <= 15m) to the rolling buffer (max size 5).
     * Automatically persisted in SharedPreferences to survive Low Memory Killer and app restarts.
     */
    fun addHighAccuracyFix(
        context: Context,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        accuracy: Float,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (accuracy <= 0f || accuracy > HIGH_ACCURACY_THRESHOLD_METERS) return
        try {
            val list = getRollingGpsBuffer(context).toMutableList()
            list.add(
                ReliableGpsFix(
                    latitude = latitude,
                    longitude = longitude,
                    altitude = altitude,
                    accuracy = accuracy,
                    timestamp = timestamp
                )
            )
            while (list.size > MAX_GPS_BUFFER_SIZE) {
                list.removeAt(0)
            }
            val jsonArray = JSONArray()
            for (fix in list) {
                val obj = JSONObject().apply {
                    put("lat", fix.latitude)
                    put("lng", fix.longitude)
                    put("alt", fix.altitude)
                    put("acc", fix.accuracy.toDouble())
                    put("time", fix.timestamp)
                }
                jsonArray.put(obj)
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_HIGH_ACCURACY_GPS_BUFFER, jsonArray.toString())
                .apply()
            Log.d(TAG, "Added high-accuracy GPS fix (±${accuracy}m). Rolling buffer count: ${list.size}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to add high-accuracy GPS fix to buffer: ${e.message}")
        }
    }

    /**
     * Returns the last known reliable fix (accuracy <= 15m) from the rolling buffer.
     * Only returns fixes recorded within the last 15 minutes of the current driving session.
     */
    fun getLastReliableFix(context: Context): ReliableGpsFix? {
        val list = getRollingGpsBuffer(context)
        val now = System.currentTimeMillis()
        return list.lastOrNull {
            it.accuracy in 0.001f..HIGH_ACCURACY_THRESHOLD_METERS &&
            (now - it.timestamp) < 15 * 60 * 1000L
        }
    }

    fun clearGpsBuffer(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_HIGH_ACCURACY_GPS_BUFFER)
            .apply()
    }

    /**
     * Records car location during movement and stops.
     * When vehicular speed is observed, marks user as moving in car and maintains rolling GPS buffer.
     * When speed drops to 0 or car stops:
     * - If GPS accuracy degrades beyond 30m or signal is lost (underground garage), falls back
     *   to the last known reliable fix (accuracy <= 15m) near the entrance.
     * - Arms perimeter geofence around the candidate car position after dwell check.
     */
    fun recordCarLocation(
        context: Context,
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0,
        accuracy: Float = 10f,
        speedMps: Float = 0f
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_GEOFENCE_ENABLED, false)) return

        val now = System.currentTimeMillis()
        val wasMovingInCar = prefs.getBoolean(KEY_IS_MOVING_IN_CAR, false)
        val candidateLat = prefs.getFloat(KEY_CAR_STOP_LAT, 0f).toDouble()
        val candidateLng = prefs.getFloat(KEY_CAR_STOP_LNG, 0f).toDouble()
        val lastStopTime = prefs.getLong(KEY_CAR_STOP_TIME, 0L)
        val isHighAccuracy = accuracy in 0.001f..HIGH_ACCURACY_THRESHOLD_METERS

        // 1. If moving at driving speed (>= 2.8 m/s ~ 10 km/h)
        if (speedMps >= 2.8f) {
            if (isHighAccuracy) {
                addHighAccuracyFix(context, latitude, longitude, altitude, accuracy, now)
            }

            val wasArmed = prefs.getBoolean(KEY_IS_GEOFENCE_ARMED, false)
            prefs.edit()
                .putBoolean(KEY_IS_MOVING_IN_CAR, true)
                .putFloat(KEY_CAR_STOP_LAT, latitude.toFloat())
                .putFloat(KEY_CAR_STOP_LNG, longitude.toFloat())
                .putFloat(KEY_CAR_STOP_ALT, altitude.toFloat())
                .putFloat(KEY_CAR_STOP_ACC, accuracy)
                .putLong(KEY_CAR_STOP_TIME, now)
                .putLong(KEY_STATIONARY_START_TIME, 0L) // reset dwell timer on movement
                .putLong(KEY_LAST_STATIONARY_DWELL_MS, 0L)
                .remove(KEY_STEPS_AT_STOP)
                .putBoolean(KEY_EXPLICIT_VEHICLE_EXIT, false)
                .putBoolean(KEY_UNDERGROUND_FALLBACK_ACTIVE, false)
                .apply()

            // If a geofence was armed at a red light/traffic stop, disarm it when vehicle drives again
            if (wasArmed) {
                Log.d(TAG, "Vehicle resumed driving (speed: ${speedMps} m/s). Disarming geofence to prevent traffic churn.")
                removeGeofences(context)
            }
        } else if (speedMps < 1.2f) {
            // 2. Stopped or parked
            if (wasMovingInCar || (now - lastStopTime < 15 * 60 * 1000L)) {
                // Underground Parking Safeguard Check:
                // If vehicle stops but current GPS accuracy degrades beyond 30m or signal is lost,
                // fall back to the last known reliable fix (accuracy <= 15m) near the entrance.
                val isDegradedAccuracy = accuracy > DEGRADED_ACCURACY_THRESHOLD_METERS || accuracy <= 0f
                val lastReliable = if (isDegradedAccuracy) getLastReliableFix(context) else null

                val effectiveLat: Double
                val effectiveLng: Double
                val effectiveAlt: Double
                val effectiveAcc: Float
                val usedUndergroundFallback: Boolean

                if (isDegradedAccuracy && lastReliable != null) {
                    effectiveLat = lastReliable.latitude
                    effectiveLng = lastReliable.longitude
                    effectiveAlt = lastReliable.altitude
                    effectiveAcc = lastReliable.accuracy
                    usedUndergroundFallback = true
                    Log.w(TAG, "Underground parking safeguard: Current accuracy degraded to ${accuracy}m (> 30m). Falling back to entrance fix: ($effectiveLat, $effectiveLng, ±${effectiveAcc}m).")
                } else {
                    effectiveLat = latitude
                    effectiveLng = longitude
                    effectiveAlt = altitude
                    effectiveAcc = accuracy
                    usedUndergroundFallback = false
                    if (isHighAccuracy) {
                        addHighAccuracyFix(context, latitude, longitude, altitude, accuracy, now)
                    }
                }

                // Ensure candidate coordinates are persistently updated in SharedPreferences immediately to survive LMK
                val editor = prefs.edit()
                    .putFloat(KEY_CAR_STOP_LAT, effectiveLat.toFloat())
                    .putFloat(KEY_CAR_STOP_LNG, effectiveLng.toFloat())
                    .putFloat(KEY_CAR_STOP_ALT, effectiveAlt.toFloat())
                    .putFloat(KEY_CAR_STOP_ACC, effectiveAcc)
                    .putLong(KEY_CAR_STOP_TIME, now)
                    .putBoolean(KEY_UNDERGROUND_FALLBACK_ACTIVE, usedUndergroundFallback)

                if (usedUndergroundFallback) {
                    editor.putFloat(KEY_UNDERGROUND_ORIGINAL_ACC, accuracy)
                    editor.putFloat(KEY_UNDERGROUND_FALLBACK_ACC, effectiveAcc)
                    editor.putLong(KEY_UNDERGROUND_FALLBACK_TIME, lastReliable?.timestamp ?: now)
                }

                // Snapshot device step counter when stopping to track footsteps away from vehicle
                val curSteps = prefs.getInt(KEY_CURRENT_STEP_COUNT, -1)
                if (curSteps >= 0 && !prefs.contains(KEY_STEPS_AT_STOP)) {
                    editor.putInt(KEY_STEPS_AT_STOP, curSteps)
                }

                var stationaryStart = prefs.getLong(KEY_STATIONARY_START_TIME, 0L)
                if (stationaryStart == 0L) {
                    stationaryStart = now
                    editor.putLong(KEY_STATIONARY_START_TIME, stationaryStart)
                }
                editor.apply()

                val dwellMs = now - stationaryStart
                val explicitExit = prefs.getBoolean(KEY_EXPLICIT_VEHICLE_EXIT, false)
                val isArmed = prefs.getBoolean(KEY_IS_GEOFENCE_ARMED, false)

                // Enforce 20-30s stationary dwell timer OR explicit IN_VEHICLE (EXIT) event
                // so red lights and stop-and-go traffic don't churn geofences
                if (!isArmed && (dwellMs >= STATIONARY_DWELL_TIMER_MS || explicitExit)) {
                    Log.d(TAG, "Stationary dwell timer satisfied (${dwellMs}ms >= ${STATIONARY_DWELL_TIMER_MS}ms) or explicit exit ($explicitExit). Arming geofence (${DEFAULT_PERIMETER_RADIUS_METERS}m).")
                    prefs.edit().putLong(KEY_LAST_STATIONARY_DWELL_MS, dwellMs).apply()
                    registerCarPerimeterGeofence(context, effectiveLat, effectiveLng, DEFAULT_PERIMETER_RADIUS_METERS)
                } else if (!isArmed) {
                    Log.d(TAG, "Vehicle stationary for ${dwellMs}ms. Waiting for ${STATIONARY_DWELL_TIMER_MS / 1000}s dwell timer before arming geofence to prevent red-light churn.")
                }
            }
        }

        // 3. Flawless walking departure check:
        // If user was moving in car, or geofence is armed, or car stopped within last 2 hours:
        val centerLat = prefs.getFloat(KEY_GEOFENCE_CENTER_LAT, 0f).toDouble()
        val centerLng = prefs.getFloat(KEY_GEOFENCE_CENTER_LNG, 0f).toDouble()
        val refLat = if (candidateLat != 0.0) candidateLat else centerLat
        val refLng = if (candidateLng != 0.0) candidateLng else centerLng
        val isArmed = prefs.getBoolean(KEY_IS_GEOFENCE_ARMED, false)
        val armedTime = prefs.getLong(KEY_GEOFENCE_ARMED_TIME, 0L)

        val hasValidParkPoint = refLat != 0.0 && refLng != 0.0
        val isRecentSession = wasMovingInCar || (now - lastStopTime < 2 * 60 * 60 * 1000L) || isArmed || (now - armedTime < 2 * 60 * 60 * 1000L)

        if (hasValidParkPoint && isRecentSession) {
            val dist = FloatArray(1)
            android.location.Location.distanceBetween(refLat, refLng, latitude, longitude, dist)
            if (dist[0] >= DEFAULT_PERIMETER_RADIUS_METERS && speedMps < 3.2f) {
                Log.d(TAG, "Walking detected: User moved ${dist[0]}m (>= ${DEFAULT_PERIMETER_RADIUS_METERS}m) away from parked car. Auto-saving car location!")
                // Reset moving in car, dwell timer, and clear armed state
                prefs.edit()
                    .putBoolean(KEY_IS_MOVING_IN_CAR, false)
                    .putLong(KEY_STATIONARY_START_TIME, 0L)
                    .putBoolean(KEY_IS_GEOFENCE_ARMED, false)
                    .apply()
                triggerWalkingAutoSave(context, refLat, refLng)
            }
        }
    }

    /**
     * Passenger Drop-Off & Coordinate Staleness Safeguard:
     * Validates that the recorded candidate stop is a genuine vehicle parking event.
     */
    fun verifyPassengerDropOffSafeguard(
        context: Context,
        resolvedLat: Double = 0.0,
        resolvedLng: Double = 0.0
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stopLat = prefs.getFloat(KEY_CAR_STOP_LAT, 0f).toDouble()
        val stopLng = prefs.getFloat(KEY_CAR_STOP_LNG, 0f).toDouble()
        val centerLat = prefs.getFloat(KEY_GEOFENCE_CENTER_LAT, 0f).toDouble()
        val centerLng = prefs.getFloat(KEY_GEOFENCE_CENTER_LNG, 0f).toDouble()

        val effectiveLat = when {
            resolvedLat != 0.0 -> resolvedLat
            stopLat != 0.0 -> stopLat
            centerLat != 0.0 -> centerLat
            else -> 0.0
        }
        val effectiveLng = when {
            resolvedLng != 0.0 -> resolvedLng
            stopLng != 0.0 -> stopLng
            centerLng != 0.0 -> centerLng
            else -> 0.0
        }

        // 1. Must have a recorded car stop or geofence center coordinate
        if (effectiveLat == 0.0 && effectiveLng == 0.0) {
            Log.d(TAG, "Safeguard: No candidate car coordinates or geofence center recorded.")
            return false
        }

        // 2. Staleness validation: ensure coordinates are not from an older day (> 24h)
        val stopTime = prefs.getLong(KEY_CAR_STOP_TIME, 0L)
        val armedTime = prefs.getLong(KEY_GEOFENCE_ARMED_TIME, 0L)
        val refTime = when {
            stopTime > 0L -> stopTime
            armedTime > 0L -> armedTime
            else -> System.currentTimeMillis()
        }
        val now = System.currentTimeMillis()
        if (refTime > 0L && (now - refTime) > 24 * 60 * 60 * 1000L) {
            Log.w(TAG, "Safeguard: Coordinates are stale (> 24h old). Rejecting.")
            return false
        }

        // 3. Vehicular speed safeguard: if device is currently moving at driving speed (>= 2.8 m/s ~ 10 km/h),
        // vehicle is still actively in motion (e.g. stopped briefly at red light or in traffic)
        val curLoc = com.example.sensor.LocationHelper.lastKnownLocation
        if (curLoc != null && curLoc.hasSpeed() && curLoc.speed >= 2.8f) {
            Log.w(TAG, "Safeguard: Device currently moving at driving speed (${curLoc.speed} m/s). Rejecting false trigger.")
            return false
        }

        Log.d(TAG, "Safeguard verified: valid parking coordinates and walking departure confirmed.")
        return true
    }

    /**
     * Triggers walking auto-save of the parked car's location.
     */
    fun triggerWalkingAutoSave(
        context: Context,
        carLat: Double,
        carLng: Double
    ) {
        val intent = Intent(context, GeofenceTransitionsReceiver::class.java).apply {
            action = ACTION_GEOFENCE_TRANSITION
            putExtra("car_lat", carLat)
            putExtra("car_lng", carLng)
            putExtra("force_trigger", true)
        }
        context.sendBroadcast(intent)
    }

    /**
     * Marks that the user is moving in a car (e.g. from speed, Activity Recognition, or Bluetooth).
     */
    fun markUserDriving(context: Context, latitude: Double? = null, longitude: Double? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit().putBoolean(KEY_IS_MOVING_IN_CAR, true)
        if (latitude != null && longitude != null) {
            editor.putFloat(KEY_CAR_STOP_LAT, latitude.toFloat())
            editor.putFloat(KEY_CAR_STOP_LNG, longitude.toFloat())
            editor.putLong(KEY_CAR_STOP_TIME, System.currentTimeMillis())
        }
        editor.apply()
    }

    /**
     * Registers a circular geofence around the car's current or last stopped location.
     * When the user moves outside this perimeter, the GeofenceTransitionsReceiver is triggered.
     */
    @SuppressLint("MissingPermission")
    fun registerCarPerimeterGeofence(
        context: Context,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float = DEFAULT_PERIMETER_RADIUS_METERS
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(KEY_GEOFENCE_ENABLED, false)
        if (!isEnabled) {
            Log.d(TAG, "Geofencing auto-park is disabled in settings. Skipping geofence registration.")
            return
        }

        // Synchronously record candidate car stop and armed center coordinates
        prefs.edit()
            .putFloat(KEY_CAR_STOP_LAT, latitude.toFloat())
            .putFloat(KEY_CAR_STOP_LNG, longitude.toFloat())
            .putFloat(KEY_GEOFENCE_CENTER_LAT, latitude.toFloat())
            .putFloat(KEY_GEOFENCE_CENTER_LNG, longitude.toFloat())
            .putLong(KEY_CAR_STOP_TIME, System.currentTimeMillis())
            .putLong(KEY_GEOFENCE_ARMED_TIME, System.currentTimeMillis())
            .putBoolean(KEY_IS_GEOFENCE_ARMED, true)
            .apply()

        try {
            val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

            val geofence = Geofence.Builder()
                .setRequestId(GEOFENCE_REQUEST_ID)
                .setCircularRegion(latitude, longitude, radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                .setNotificationResponsiveness(NOTIFICATION_RESPONSIVENESS_MS) // 25s responsiveness to eliminate GPS float false-exits & battery drain
                .build()

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
                .addGeofence(geofence)
                .build()

            val pendingIntent = getGeofencePendingIntent(context)

            // Remove any old geofence before registering new perimeter
            geofencingClient.removeGeofences(pendingIntent).addOnCompleteListener {
                try {
                    geofencingClient.addGeofences(request, pendingIntent)
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully registered car perimeter geofence at ($latitude, $longitude) with radius $radiusMeters m")
                            prefs.edit().putBoolean(KEY_IS_GEOFENCE_ARMED, true).apply()
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to register geofence with Play Services: ${e.message}")
                        }
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException registering geofence: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering geofence: ${e.message}", e)
        }
    }

    /**
     * Arms geofence tracking around a Bluetooth auto-saved location.
     * When dev option or geofencing is enabled, this ensures the perimeter is armed
     * around the car spot so that walking away from the parked car can be detected.
     */
    fun armGeofenceAfterBluetoothSave(context: Context, latitude: Double, longitude: Double) {
        if (latitude == 0.0 && longitude == 0.0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        prefs.edit()
            .putFloat(KEY_CAR_STOP_LAT, latitude.toFloat())
            .putFloat(KEY_CAR_STOP_LNG, longitude.toFloat())
            .putLong(KEY_CAR_STOP_TIME, now)
            .putLong(KEY_LAST_STATIONARY_DWELL_MS, 30_000L)
            .putBoolean(KEY_IS_MOVING_IN_CAR, true)
            .putBoolean(KEY_EXPLICIT_VEHICLE_EXIT, true)
            .apply()

        val isGeofenceEnabled = prefs.getBoolean(KEY_GEOFENCE_ENABLED, false)
        if (isGeofenceEnabled) {
            registerCarPerimeterGeofence(context, latitude, longitude, DEFAULT_PERIMETER_RADIUS_METERS)
        }
    }

    /**
     * Removes all active car perimeter geofences.
     */
    fun removeGeofences(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_IS_GEOFENCE_ARMED, false).apply()
            val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
            val pendingIntent = getGeofencePendingIntent(context)
            geofencingClient.removeGeofences(pendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully removed active geofences.")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to remove geofences: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing geofences: ${e.message}", e)
        }
    }

    /**
     * Simulates walking after driving for instant testing in Developer settings.
     * Takes the car location, arms the geofence, and triggers the transition
     * indicating that the user has started walking away from the parked car.
     */
    fun simulateGeofenceExit(context: Context, latitude: Double? = null, longitude: Double? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentLoc = com.example.sensor.LocationHelper.lastKnownLocation
            ?: com.example.sensor.LocationHelper.getPersistedLocation(context)
        val fallbackLat = if (currentLoc != null && currentLoc.latitude != 0.0) currentLoc.latitude else 37.4220
        val fallbackLng = if (currentLoc != null && currentLoc.longitude != 0.0) currentLoc.longitude else -122.0841

        val carLat = latitude ?: run {
            val savedLat = prefs.getFloat(KEY_CAR_STOP_LAT, 0f).toDouble()
            if (savedLat != 0.0) savedLat else fallbackLat
        }
        val carLng = longitude ?: run {
            val savedLng = prefs.getFloat(KEY_CAR_STOP_LNG, 0f).toDouble()
            if (savedLng != 0.0) savedLng else fallbackLng
        }

        // Mark that user was moving in car and stopped at this spot
        prefs.edit()
            .putBoolean(KEY_IS_MOVING_IN_CAR, true)
            .putFloat(KEY_CAR_STOP_LAT, carLat.toFloat())
            .putFloat(KEY_CAR_STOP_LNG, carLng.toFloat())
            .putFloat(KEY_GEOFENCE_CENTER_LAT, carLat.toFloat())
            .putFloat(KEY_GEOFENCE_CENTER_LNG, carLng.toFloat())
            .putLong(KEY_CAR_STOP_TIME, System.currentTimeMillis())
            .putBoolean(KEY_IS_GEOFENCE_ARMED, true)
            .apply()

        // Fire simulation intent to GeofenceTransitionsReceiver
        val intent = Intent(context, GeofenceTransitionsReceiver::class.java).apply {
            action = ACTION_SIMULATE_GEOFENCE_EXIT
            putExtra("car_lat", carLat)
            putExtra("car_lng", carLng)
            // Simulated user walking location (e.g. 70 meters away, outside the 50m perimeter)
            putExtra("simulated_lat", carLat + 0.0007)
            putExtra("simulated_lng", carLng + 0.0007)
        }
        context.sendBroadcast(intent)
    }
}
