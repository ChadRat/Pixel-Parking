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
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
    const val ACTION_ACTIVITY_UPDATE = "com.example.autopark.ACTION_ACTIVITY_UPDATE"
    const val ACTION_LOCATION_UPDATE = "com.example.autopark.ACTION_LOCATION_UPDATE"

    // SharedPreferences keys for persistent car tracking (survives Low Memory Killer)
    const val PREFS_NAME = "auto_park_prefs"
    const val KEY_GEOFENCE_ENABLED = "geofence_auto_park_enabled"
    const val KEY_IS_MOVING_IN_CAR = "is_moving_in_car"
    const val KEY_IS_CURRENTLY_WALKING = "is_currently_walking_activity_api"
    const val KEY_LAST_WALKING_ACTIVITY_TIME = "last_walking_activity_time"
    const val KEY_LAST_DETECTED_ACTIVITY_TYPE = "last_detected_activity_type"
    const val KEY_LAST_DETECTED_ACTIVITY_CONFIDENCE = "last_detected_activity_confidence"
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
    const val KEY_CAR_STOP_LOCKED = "candidate_car_stop_locked"
    const val KEY_LAST_DRIVING_LAT = "last_driving_lat"
    const val KEY_LAST_DRIVING_LNG = "last_driving_lng"
    const val KEY_LAST_DRIVING_ALT = "last_driving_alt"
    const val KEY_LAST_DRIVING_ACC = "last_driving_acc"
    const val KEY_LAST_DRIVING_TIME = "last_driving_time"

    // Underground Parking Safeguard via GPS Accuracy Backtracking
    const val KEY_HIGH_ACCURACY_GPS_BUFFER = "high_accuracy_gps_buffer"
    const val KEY_UNDERGROUND_FALLBACK_ACTIVE = "underground_fallback_active"
    const val KEY_UNDERGROUND_ORIGINAL_ACC = "underground_original_acc"
    const val KEY_UNDERGROUND_FALLBACK_ACC = "underground_fallback_acc"
    const val KEY_UNDERGROUND_FALLBACK_TIME = "underground_fallback_time"

    const val MAX_GPS_BUFFER_SIZE = 5
    const val HIGH_ACCURACY_THRESHOLD_METERS = 15.0f // High-accuracy fixes while driving
    const val DEGRADED_ACCURACY_THRESHOLD_METERS = 30.0f // Degraded accuracy threshold in underground garage

    private const val KEY_POWER_TIER = "walk_away_active_power_tier"
    private const val TIER_OFF = 0
    private const val TIER_IDLE_LOW_POWER = 1
    private const val TIER_ACTIVE_HIGH_PRECISION = 2

    private var stepSensorListener: android.hardware.SensorEventListener? = null
    @Volatile
    private var lastInMemoryStepCount: Int = -1
    @Volatile
    private var lastStepPrefsWriteTimeMs: Long = 0L
    @Volatile
    private var currentRegisteredPowerTier: Int = TIER_OFF
    @Volatile
    private var currentActivityUpdateIntervalMs: Long = -1L
    @Volatile
    private var cachedWalkAwayEnabled: Boolean? = null

    /**
     * Fast in-memory check for whether the Walk Away feature is enabled.
     * Avoids repeated SharedPreferences reads and ensures zero allocations when disabled.
     */
    fun isWalkAwayEnabled(context: Context): Boolean {
        cachedWalkAwayEnabled?.let { return it }
        val enabled = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_GEOFENCE_ENABLED, false)
        cachedWalkAwayEnabled = enabled
        return enabled
    }

    private fun setReceiverComponentEnabled(context: Context, enabled: Boolean) {
        try {
            val componentName = android.content.ComponentName(context, GeofenceTransitionsReceiver::class.java)
            val newState = if (enabled) {
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            val pm = context.packageManager
            if (pm.getComponentEnabledSetting(componentName) != newState) {
                pm.setComponentEnabledSetting(
                    componentName,
                    newState,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
            }
        } catch (_: Throwable) {}
    }

    private fun findExistingPendingIntent(context: Context, requestCode: Int, actionStr: String): PendingIntent? {
        return try {
            val intent = Intent(context, GeofenceTransitionsReceiver::class.java).apply {
                action = actionStr
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_NO_CREATE
            }
            PendingIntent.getBroadcast(context, requestCode, intent, flags)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Stores a 64-bit Double coordinate in SharedPreferences while keeping the legacy Float key
     * in sync so existing callers and unit/Robolectric tests using getFloat/putFloat work seamlessly.
     */
    fun putExactCoordinate(
        editor: android.content.SharedPreferences.Editor,
        key: String,
        value: Double
    ): android.content.SharedPreferences.Editor {
        val asFloat = value.toFloat()
        editor.putFloat(key, asFloat)
        editor.putLong("${key}_bits64", java.lang.Double.doubleToRawLongBits(value))
        editor.putFloat("${key}_fref", asFloat)
        return editor
    }

    /**
     * Reads a 64-bit Double coordinate from SharedPreferences if written via putExactCoordinate,
     * or falls back to the Float key if written directly via putFloat (e.g. in unit tests).
     */
    fun getExactCoordinate(
        prefs: android.content.SharedPreferences,
        key: String,
        defaultValue: Double = 0.0
    ): Double {
        if (!prefs.contains(key)) return defaultValue
        val floatVal = prefs.getFloat(key, defaultValue.toFloat())
        val refFloat = prefs.getFloat("${key}_fref", Float.NaN)
        if (!refFloat.isNaN() && floatVal == refFloat && prefs.contains("${key}_bits64")) {
            return java.lang.Double.longBitsToDouble(
                prefs.getLong("${key}_bits64", java.lang.Double.doubleToRawLongBits(defaultValue))
            )
        }
        return floatVal.toDouble()
    }

    private fun removeExactCoordinate(
        editor: android.content.SharedPreferences.Editor,
        key: String
    ): android.content.SharedPreferences.Editor {
        editor.remove(key)
        editor.remove("${key}_bits64")
        editor.remove("${key}_fref")
        return editor
    }

    /**
     * Determines whether the Walk Away engine should currently be in High-Precision Active Mode
     * (when driving or within 25 minutes of a candidate car stop / armed geofence) or
     * Ultra-Low-Power Idle Mode (when stationary at home/work/indoors).
     */
    private fun shouldUseHighPrecisionTier(prefs: android.content.SharedPreferences, now: Long = System.currentTimeMillis()): Boolean {
        val isMovingInCar = prefs.getBoolean(KEY_IS_MOVING_IN_CAR, false)
        if (isMovingInCar) return true
        val isArmed = prefs.getBoolean(KEY_IS_GEOFENCE_ARMED, false)
        val armedTime = prefs.getLong(KEY_GEOFENCE_ARMED_TIME, 0L)
        if (isArmed && (now - armedTime) < 25 * 60 * 1000L) return true
        val stopTime = prefs.getLong(KEY_CAR_STOP_TIME, 0L)
        val stopLat = getExactCoordinate(prefs, KEY_CAR_STOP_LAT, 0.0)
        if (stopLat != 0.0 && stopTime > 0L && (now - stopTime) < 25 * 60 * 1000L) return true
        return false
    }

    /**
     * Dynamically shifts sensor, activity recognition, and location request parameters between:
     * - TIER_IDLE_LOW_POWER: 60m displacement-gated balanced location + DSP ActivityTransitions + step sensor off/hibernated
     * - TIER_ACTIVE_HIGH_PRECISION: 4s high-accuracy GPS + 5s ActivityRecognition updates + hardware-batched step counter
     */
    fun syncAdaptivePowerTier(context: Context, forceHighPrecision: Boolean = false) {
        if (!isWalkAwayEnabled(context)) {
            currentRegisteredPowerTier = TIER_OFF
            currentActivityUpdateIntervalMs = -1L
            return
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val wantHighPrecision = forceHighPrecision || shouldUseHighPrecisionTier(prefs)
        val targetTier = if (wantHighPrecision) TIER_ACTIVE_HIGH_PRECISION else TIER_IDLE_LOW_POWER

        if (wantHighPrecision) {
            registerStepSensor(context)
        } else {
            // In pure idle state (not driving and no recent candidate car stop), unregister continuous
            // step sensor listener so everyday walking never wakes the CPU or writes to disk.
            unregisterStepSensor(context)
        }

        if (currentRegisteredPowerTier != targetTier) {
            currentRegisteredPowerTier = targetTier
            prefs.edit().putInt(KEY_POWER_TIER, targetTier).apply()
            registerBalancedLocationTracking(context, highPrecision = wantHighPrecision)
            registerActivityTransitions(context, highFrequencyUpdates = wantHighPrecision)
            Log.d(
                TAG,
                "Walk Away adaptive power tier switched to ${if (wantHighPrecision) "HIGH_PRECISION (Driving/Stop Verification)" else "IDLE_LOW_POWER (Zero-Drain Standby)"}"
            )
        }
    }

    /**
     * Registers device step sensor with hardware FIFO batching to provide pedestrian foot-movement
     * telemetry for the passenger drop-off safeguard without waking the CPU on every single step.
     */
    fun registerStepSensor(context: Context) {
        if (!isWalkAwayEnabled(context)) return
        if (stepSensorListener != null) return
        try {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? android.hardware.SensorManager
            val stepCounter = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_STEP_COUNTER)
                ?: sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_STEP_DETECTOR)
            if (stepCounter != null) {
                stepSensorListener = object : android.hardware.SensorEventListener {
                    override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                        val value = event?.values?.getOrNull(0)?.toInt() ?: return
                        val now = System.currentTimeMillis()
                        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

                        val currentStepCount = if (event.sensor.type == android.hardware.Sensor.TYPE_STEP_COUNTER) {
                            lastInMemoryStepCount = value
                            value
                        } else {
                            val base = if (lastInMemoryStepCount >= 0) lastInMemoryStepCount else prefs.getInt(KEY_CURRENT_STEP_COUNT, 0)
                            val cur = base + 1
                            lastInMemoryStepCount = cur
                            cur
                        }

                        val wasMovingInCar = prefs.getBoolean(KEY_IS_MOVING_IN_CAR, false)
                        val stopLat = getExactCoordinate(prefs, KEY_CAR_STOP_LAT, 0.0)
                        val stopLng = getExactCoordinate(prefs, KEY_CAR_STOP_LNG, 0.0)
                        val stopTime = prefs.getLong(KEY_CAR_STOP_TIME, 0L)
                        val isRecentStop = stopTime > 0L && (now - stopTime) < 30 * 60 * 1000L

                        // If neither driving nor in a recent stop window, downshift to idle tier and skip disk I/O
                        if (!wasMovingInCar && !isRecentStop && stopLat == 0.0) {
                            if (now - lastStepPrefsWriteTimeMs > 60_000L) {
                                lastStepPrefsWriteTimeMs = now
                                prefs.edit().putInt(KEY_CURRENT_STEP_COUNT, currentStepCount).apply()
                                syncAdaptivePowerTier(context, forceHighPrecision = false)
                            }
                            return
                        }

                        var stepsAtStop = prefs.getInt(KEY_STEPS_AT_STOP, -1)
                        val lastAutoSaveTime = prefs.getLong(KEY_LAST_AUTO_SAVE_TIME, 0L)
                        val justSaved = (now - lastAutoSaveTime) < 90_000L

                        // Initialize steps snapshot upon car stop if not yet recorded
                        if (stepsAtStop < 0 && (stopLat != 0.0 || wasMovingInCar)) {
                            stepsAtStop = currentStepCount
                            prefs.edit()
                                .putInt(KEY_CURRENT_STEP_COUNT, currentStepCount)
                                .putInt(KEY_STEPS_AT_STOP, stepsAtStop)
                                .apply()
                            lastStepPrefsWriteTimeMs = now
                        } else if (now - lastStepPrefsWriteTimeMs >= 1_500L || (currentStepCount - stepsAtStop) in 4..12) {
                            prefs.edit().putInt(KEY_CURRENT_STEP_COUNT, currentStepCount).apply()
                            lastStepPrefsWriteTimeMs = now
                        }

                        val stepDelta = if (stepsAtStop >= 0 && currentStepCount >= stepsAtStop) (currentStepCount - stepsAtStop) else 0

                        // Concrete physical footstep telemetry: actively confirms walking
                        if (stepDelta >= 4) {
                            prefs.edit()
                                .putInt(KEY_CURRENT_STEP_COUNT, currentStepCount)
                                .putBoolean(KEY_IS_CURRENTLY_WALKING, true)
                                .putLong(KEY_LAST_WALKING_ACTIVITY_TIME, now)
                                .putInt(KEY_LAST_DETECTED_ACTIVITY_TYPE, DetectedActivity.WALKING)
                                .putInt(KEY_LAST_DETECTED_ACTIVITY_CONFIDENCE, 95)
                                .apply()
                        }

                        if ((wasMovingInCar || isRecentStop) && stopLat != 0.0 && stopLng != 0.0 && !justSaved) {
                            if (com.example.util.BluetoothDeviceHelper.isAnyCarBluetoothConnected(context)) {
                                return
                            }
                            if (stepDelta >= 10) {
                                Log.d(TAG, "Walking detected via step counter ($stepDelta footsteps after stop). Auto-saving Walk Away parking location!")
                                prefs.edit()
                                    .putInt(KEY_CURRENT_STEP_COUNT, currentStepCount)
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
                // Use 2.5s hardware FIFO batching (2_500_000 us) so sensor hub batches footsteps in hardware
                sensorManager?.registerListener(
                    stepSensorListener,
                    stepCounter,
                    android.hardware.SensorManager.SENSOR_DELAY_NORMAL,
                    2_500_000
                )
                Log.d(TAG, "Step sensor registered with hardware batching for Walk Away detection.")
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

    private fun getActivityUpdatesPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceTransitionsReceiver::class.java).apply {
            action = ACTION_ACTIVITY_UPDATE
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, 1004, intent, flags)
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
        cachedWalkAwayEnabled = true
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_GEOFENCE_ENABLED, true).apply()
        setReceiverComponentEnabled(context, true)
        currentRegisteredPowerTier = TIER_OFF
        currentActivityUpdateIntervalMs = -1L
        syncAdaptivePowerTier(context)
    }

    /**
     * Disables geofence & walking auto-park and releases 100% of associated memory,
     * PendingIntents, sensors, Play Services listeners, and buffered coordinates.
     */
    fun stopGeofenceWalkingAutoPark(context: Context) {
        cachedWalkAwayEnabled = false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_GEOFENCE_ENABLED, false)
            .putInt(KEY_POWER_TIER, TIER_OFF)
            .apply()
        currentRegisteredPowerTier = TIER_OFF
        currentActivityUpdateIntervalMs = -1L
        lastInMemoryStepCount = -1
        lastStepPrefsWriteTimeMs = 0L
        removeGeofences(context)
        removeActivityTransitions(context)
        unregisterStepSensor(context)
        unregisterBalancedLocationTracking(context)
        clearGpsBuffer(context)
        clearCandidateParkingState(context)
        try {
            androidx.work.WorkManager.getInstance(context).cancelUniqueWork("GeofenceAutoParkUniqueWork")
        } catch (_: Throwable) {}
        setReceiverComponentEnabled(context, false)
    }

    /**
     * Restores Activity Recognition, step sensors, and armed perimeter geofences
     * upon system reboot (BOOT_COMPLETED) or Application process initialization.
     * When disabled, guarantees zero background listeners or PendingIntents remain active
     * without instantiating any Play Services clients unless a stale PendingIntent exists.
     */
    fun restoreOnStartup(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(KEY_GEOFENCE_ENABLED, false)
        cachedWalkAwayEnabled = isEnabled
        if (!isEnabled) {
            currentRegisteredPowerTier = TIER_OFF
            currentActivityUpdateIntervalMs = -1L
            lastInMemoryStepCount = -1
            lastStepPrefsWriteTimeMs = 0L
            unregisterStepSensor(context)
            // Only touch Play Services clients if a stale PendingIntent from a prior session actually exists
            removeGeofences(context)
            removeActivityTransitions(context)
            unregisterBalancedLocationTracking(context)
            setReceiverComponentEnabled(context, false)
            return
        }

        setReceiverComponentEnabled(context, true)
        currentRegisteredPowerTier = TIER_OFF
        currentActivityUpdateIntervalMs = -1L
        syncAdaptivePowerTier(context)

        val isArmed = prefs.getBoolean(KEY_IS_GEOFENCE_ARMED, false)
        val armedTime = prefs.getLong(KEY_GEOFENCE_ARMED_TIME, 0L)
        val centerLat = getExactCoordinate(prefs, KEY_GEOFENCE_CENTER_LAT, 0.0)
        val centerLng = getExactCoordinate(prefs, KEY_GEOFENCE_CENTER_LNG, 0.0)
        val now = System.currentTimeMillis()

        if (isArmed && centerLat != 0.0 && centerLng != 0.0 && (now - armedTime) < 12 * 60 * 60 * 1000L) {
            Log.d(TAG, "Restoring armed perimeter geofence around ($centerLat, $centerLng) after reboot/startup.")
            registerCarPerimeterGeofence(context, centerLat, centerLng, DEFAULT_PERIMETER_RADIUS_METERS)
        }
    }

    @SuppressLint("MissingPermission")
    fun registerBalancedLocationTracking(context: Context, highPrecision: Boolean = false) {
        if (!isWalkAwayEnabled(context)) return
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            // Remove any legacy duplicate foreground callback so we only ever have a single clean subscription
            balancedLocationCallback?.let {
                fusedClient.removeLocationUpdates(it)
                balancedLocationCallback = null
            }

            // Adaptive two-tier LocationRequest:
            // - High Precision (Driving / Candidate Stop Active): 4s high-accuracy GNSS with 4m displacement filter
            //   to capture the exact parking bay as the vehicle decelerates to a halt.
            // - Idle Low Power (Stationary / Standby): Balanced power accuracy with 55m minimum displacement gate
            //   and 45s interval (90s batching) so zero GPS wakes occur while stationary at home, work, or indoors.
            val request = if (highPrecision) {
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4_000L)
                    .setMinUpdateIntervalMillis(2_000L)
                    .setMinUpdateDistanceMeters(4f)
                    .setWaitForAccurateLocation(false)
                    .build()
            } else {
                LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 45_000L)
                    .setMinUpdateIntervalMillis(15_000L)
                    .setMaxUpdateDelayMillis(90_000L)
                    .setMinUpdateDistanceMeters(55f)
                    .setWaitForAccurateLocation(false)
                    .build()
            }

            val pendingIntent = getLocationPendingIntent(context)
            fusedClient.requestLocationUpdates(request, pendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Registered Walk Away location PendingIntent (highPrecision=$highPrecision).")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to register Walk Away location PendingIntent: ${e.message}")
                }
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission not granted for Walk Away background tracking: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Error registering balanced location tracking: ${e.message}")
        }
    }

    fun unregisterBalancedLocationTracking(context: Context) {
        try {
            val existingPi = findExistingPendingIntent(context, 1003, ACTION_LOCATION_UPDATE)
            val cb = balancedLocationCallback
            if (existingPi != null || cb != null) {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                if (existingPi != null) {
                    fusedClient.removeLocationUpdates(existingPi).addOnCompleteListener {
                        try { existingPi.cancel() } catch (_: Throwable) {}
                    }
                }
                if (cb != null) {
                    fusedClient.removeLocationUpdates(cb)
                    balancedLocationCallback = null
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Cleans up transient candidate parking state after a parking session is safely committed to the database,
     * and downshifts Walk Away tracking back to Ultra-Low-Power Idle Standby.
     */
    fun clearCandidateParkingState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putBoolean(KEY_IS_MOVING_IN_CAR, false)
            .remove(KEY_CAR_STOP_ALT)
            .remove(KEY_CAR_STOP_ACC)
            .remove(KEY_CAR_STOP_TIME)
            .remove(KEY_GEOFENCE_ARMED_TIME)
            .putLong(KEY_STATIONARY_START_TIME, 0L)
            .putBoolean(KEY_EXPLICIT_VEHICLE_EXIT, false)
            .putBoolean(KEY_IS_GEOFENCE_ARMED, false)
            .putBoolean(KEY_UNDERGROUND_FALLBACK_ACTIVE, false)
            .remove(KEY_STEPS_AT_STOP)
            .putBoolean(KEY_IS_CURRENTLY_WALKING, false)
            .remove(KEY_LAST_WALKING_ACTIVITY_TIME)
            .remove(KEY_LAST_DETECTED_ACTIVITY_TYPE)
            .remove(KEY_LAST_DETECTED_ACTIVITY_CONFIDENCE)
            .remove(KEY_CAR_STOP_LOCKED)
            .remove(KEY_LAST_DRIVING_ALT)
            .remove(KEY_LAST_DRIVING_ACC)
            .remove(KEY_LAST_DRIVING_TIME)
        removeExactCoordinate(editor, KEY_CAR_STOP_LAT)
        removeExactCoordinate(editor, KEY_CAR_STOP_LNG)
        removeExactCoordinate(editor, KEY_GEOFENCE_CENTER_LAT)
        removeExactCoordinate(editor, KEY_GEOFENCE_CENTER_LNG)
        removeExactCoordinate(editor, KEY_LAST_DRIVING_LAT)
        removeExactCoordinate(editor, KEY_LAST_DRIVING_LNG)
        editor.apply()

        // Downshift back to zero-drain idle mode now that parking session is complete
        if (prefs.getBoolean(KEY_GEOFENCE_ENABLED, false)) {
            syncAdaptivePowerTier(context, forceHighPrecision = false)
        }
    }

    /**
     * Verifies if Android Activity API confirms that the user is actively walking.
     * CRITICAL: To prevent misfires, location saving is tied directly to this verification!
     */
    fun isWalkingAccordingToActivityApi(context: Context): Boolean {
        // If phone is currently connected to car Bluetooth, user is in the car and cannot be walking
        if (com.example.util.BluetoothDeviceHelper.isAnyCarBluetoothConnected(context)) {
            return false
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isCurrentlyWalking = prefs.getBoolean(KEY_IS_CURRENTLY_WALKING, false)
        val lastWalkingTime = prefs.getLong(KEY_LAST_WALKING_ACTIVITY_TIME, 0L)
        val lastActivityType = prefs.getInt(KEY_LAST_DETECTED_ACTIVITY_TYPE, DetectedActivity.UNKNOWN)
        val now = System.currentTimeMillis()

        // If the Android Activity API explicitly detected IN_VEHICLE with no recent walking, user is in vehicle
        if (lastActivityType == DetectedActivity.IN_VEHICLE && (now - lastWalkingTime) > 15_000L) {
            return false
        }

        // Active walking transition is maintained or walking report was received within the last 60 seconds
        val isWalkingFresh = (now - lastWalkingTime) in 0L..60_000L && lastWalkingTime > 0L
        return isCurrentlyWalking || isWalkingFresh
    }

    fun getActivityName(type: Int): String {
        return when (type) {
            DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
            DetectedActivity.WALKING -> "WALKING"
            DetectedActivity.ON_FOOT -> "ON_FOOT"
            DetectedActivity.RUNNING -> "RUNNING"
            DetectedActivity.STILL -> "STILL"
            DetectedActivity.ON_BICYCLE -> "ON_BICYCLE"
            DetectedActivity.TILTING -> "TILTING"
            else -> "UNKNOWN ($type)"
        }
    }

    /**
     * Registers hardware-offloaded Activity Recognition transitions & adaptive periodic updates:
     * - ActivityTransition API runs on the low-power sensor hub DSP at near-zero battery cost.
     * - Periodic ActivityUpdates run at 5s only during active driving / stop verification, and
     *   relax to 60s during idle standby so the CPU is not woken up every 10s all day.
     */
    @SuppressLint("MissingPermission")
    fun registerActivityTransitions(context: Context, highFrequencyUpdates: Boolean = false) {
        if (!isWalkAwayEnabled(context)) return
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
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.ON_FOOT)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.ON_FOOT)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.RUNNING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.RUNNING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )

            val request = ActivityTransitionRequest(transitions)
            val client = ActivityRecognition.getClient(context)

            val transitionPendingIntent = getActivityTransitionPendingIntent(context)
            client.requestActivityTransitionUpdates(request, transitionPendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully registered hardware Activity Transitions for vehicle & walking.")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to register Activity Transitions: ${e.message}")
                }

            val targetIntervalMs = if (highFrequencyUpdates) 5_000L else 60_000L
            if (currentActivityUpdateIntervalMs != targetIntervalMs) {
                currentActivityUpdateIntervalMs = targetIntervalMs
                val updatesPendingIntent = getActivityUpdatesPendingIntent(context)
                client.requestActivityUpdates(targetIntervalMs, updatesPendingIntent)
                    .addOnSuccessListener {
                        Log.d(TAG, "Registered Activity Recognition updates (${targetIntervalMs / 1000}s interval).")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to register Activity Recognition updates: ${e.message}")
                    }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException registering activity transitions: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering activity transitions: ${e.message}", e)
        }
    }

    fun removeActivityTransitions(context: Context) {
        try {
            currentActivityUpdateIntervalMs = -1L
            val transitionPi = findExistingPendingIntent(context, 1002, ACTION_ACTIVITY_TRANSITION)
            val updatesPi = findExistingPendingIntent(context, 1004, ACTION_ACTIVITY_UPDATE)
            if (transitionPi != null || updatesPi != null) {
                val client = ActivityRecognition.getClient(context)
                if (transitionPi != null) {
                    client.removeActivityTransitionUpdates(transitionPi).addOnCompleteListener {
                        try { transitionPi.cancel() } catch (_: Throwable) {}
                    }
                }
                if (updatesPi != null) {
                    client.removeActivityUpdates(updatesPi).addOnCompleteListener {
                        try { updatesPi.cancel() } catch (_: Throwable) {}
                    }
                }
                Log.d(TAG, "Successfully removed activity transitions and periodic updates.")
            }
        } catch (_: Exception) {}
    }

    /**
     * Processes periodic ActivityRecognitionResult from Android Activity API.
     */
    fun processActivityRecognitionResult(context: Context, result: ActivityRecognitionResult) {
        if (!isWalkAwayEnabled(context)) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()

        val activities = result.probableActivities ?: emptyList()
        val walkingCandidate = activities.firstOrNull {
            (it.type == DetectedActivity.WALKING ||
             it.type == DetectedActivity.ON_FOOT ||
             it.type == DetectedActivity.RUNNING) && it.confidence >= 25
        }
        val vehicleCandidate = activities.firstOrNull {
            it.type == DetectedActivity.IN_VEHICLE && it.confidence >= 55
        }

        val mostProbable = result.mostProbableActivity
        val type = mostProbable?.type ?: DetectedActivity.UNKNOWN
        val confidence = mostProbable?.confidence ?: 0

        Log.d(TAG, "Android Activity API update: type=$type (${getActivityName(type)}), confidence=$confidence%, probableCount=${activities.size}")

        if (walkingCandidate != null || type == DetectedActivity.WALKING || type == DetectedActivity.ON_FOOT || type == DetectedActivity.RUNNING) {
            val effConfidence = walkingCandidate?.confidence ?: confidence
            if (effConfidence >= 25) {
                val effType = walkingCandidate?.type ?: type
                prefs.edit()
                    .putBoolean(KEY_IS_CURRENTLY_WALKING, true)
                    .putLong(KEY_LAST_WALKING_ACTIVITY_TIME, now)
                    .putInt(KEY_LAST_DETECTED_ACTIVITY_TYPE, effType)
                    .putInt(KEY_LAST_DETECTED_ACTIVITY_CONFIDENCE, effConfidence)
                    .apply()
                Log.d(TAG, "Android Activity API confirmed user is WALKING (type: ${getActivityName(effType)}, confidence: $effConfidence%).")
                checkWalkingDepartureAutoSave(context)
                return
            }
        }

        if (vehicleCandidate != null && (type == DetectedActivity.IN_VEHICLE || vehicleCandidate.confidence >= 60)) {
            val lastWalking = prefs.getLong(KEY_LAST_WALKING_ACTIVITY_TIME, 0L)
            if (now - lastWalking > 20_000L) {
                val editor = prefs.edit()
                    .putBoolean(KEY_IS_CURRENTLY_WALKING, false)
                    .putBoolean(KEY_IS_MOVING_IN_CAR, true)
                    .putInt(KEY_LAST_DETECTED_ACTIVITY_TYPE, DetectedActivity.IN_VEHICLE)
                    .putInt(KEY_LAST_DETECTED_ACTIVITY_CONFIDENCE, vehicleCandidate.confidence)
                if (lastInMemoryStepCount >= 0) {
                    editor.putInt(KEY_STEPS_AT_STOP, lastInMemoryStepCount)
                }
                editor.apply()
                Log.d(TAG, "Android Activity API confirmed user is IN_VEHICLE (confidence: ${vehicleCandidate.confidence}%). Escalating to high-precision tier.")
                removeGeofences(context)
                syncAdaptivePowerTier(context, forceHighPrecision = true)
            }
            return
        }

        if (type == DetectedActivity.STILL) {
            prefs.edit()
                .putInt(KEY_LAST_DETECTED_ACTIVITY_TYPE, type)
                .putInt(KEY_LAST_DETECTED_ACTIVITY_CONFIDENCE, confidence)
                .apply()
            syncAdaptivePowerTier(context)
        }
    }

    /**
     * Processes ActivityTransitionResult from Android Activity API.
     */
    fun processActivityTransitionResult(context: Context, result: ActivityTransitionResult) {
        if (!isWalkAwayEnabled(context)) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()

        for (event in result.transitionEvents) {
            Log.d(TAG, "Android Activity Transition: activity=${getActivityName(event.activityType)}, transition=${if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) "ENTER" else "EXIT"}")

            when (event.activityType) {
                DetectedActivity.IN_VEHICLE -> {
                    if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                        val editor = prefs.edit()
                            .putBoolean(KEY_IS_MOVING_IN_CAR, true)
                            .putBoolean(KEY_IS_CURRENTLY_WALKING, false)
                            .putBoolean(KEY_EXPLICIT_VEHICLE_EXIT, false)
                            .putInt(KEY_LAST_DETECTED_ACTIVITY_TYPE, DetectedActivity.IN_VEHICLE)
                            .putLong(KEY_STATIONARY_START_TIME, 0L)
                        if (lastInMemoryStepCount >= 0) {
                            editor.putInt(KEY_STEPS_AT_STOP, lastInMemoryStepCount)
                        }
                        editor.apply()
                        removeGeofences(context)
                        syncAdaptivePowerTier(context, forceHighPrecision = true)
                    } else if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                        Log.d(TAG, "Android Activity API: IN_VEHICLE (EXIT). Recording vehicle stop & keeping high-precision verification active.")
                        val editor = prefs.edit()
                            .putBoolean(KEY_EXPLICIT_VEHICLE_EXIT, true)
                            .putLong(KEY_CAR_STOP_TIME, now)
                        if (lastInMemoryStepCount >= 0 && !prefs.contains(KEY_STEPS_AT_STOP)) {
                            editor.putInt(KEY_STEPS_AT_STOP, lastInMemoryStepCount)
                        }
                        editor.apply()
                        syncAdaptivePowerTier(context, forceHighPrecision = true)
                        armCandidateStopFromCurrentLocation(context)
                    }
                }
                DetectedActivity.WALKING, DetectedActivity.ON_FOOT, DetectedActivity.RUNNING -> {
                    if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                        Log.d(TAG, "Android Activity API: User started WALKING.")
                        prefs.edit()
                            .putBoolean(KEY_IS_CURRENTLY_WALKING, true)
                            .putLong(KEY_LAST_WALKING_ACTIVITY_TIME, now)
                            .putInt(KEY_LAST_DETECTED_ACTIVITY_TYPE, event.activityType)
                            .apply()
                        checkWalkingDepartureAutoSave(context)
                    } else if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                        Log.d(TAG, "Android Activity API: User paused WALKING.")
                        prefs.edit()
                            .putBoolean(KEY_IS_CURRENTLY_WALKING, false)
                            .putLong(KEY_LAST_WALKING_ACTIVITY_TIME, now)
                            .apply()
                    }
                }
            }
        }
    }

    /**
     * Checks if walking departure requirements are met when Android Activity API detects walking.
     */
    fun checkWalkingDepartureAutoSave(context: Context) {
        if (!isWalkAwayEnabled(context)) return
        // Suppress if phone is still connected to car Bluetooth
        if (com.example.util.BluetoothDeviceHelper.isAnyCarBluetoothConnected(context)) {
            Log.d(TAG, "checkWalkingDepartureAutoSave: Car Bluetooth is currently connected. Suppressing.")
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val wasMovingInCar = prefs.getBoolean(KEY_IS_MOVING_IN_CAR, false)
        val isArmed = prefs.getBoolean(KEY_IS_GEOFENCE_ARMED, false)
        val stopLat = getExactCoordinate(prefs, KEY_CAR_STOP_LAT, 0.0)
        val stopLng = getExactCoordinate(prefs, KEY_CAR_STOP_LNG, 0.0)
        val stopTime = prefs.getLong(KEY_CAR_STOP_TIME, 0L)
        val centerLat = getExactCoordinate(prefs, KEY_GEOFENCE_CENTER_LAT, 0.0)
        val centerLng = getExactCoordinate(prefs, KEY_GEOFENCE_CENTER_LNG, 0.0)
        val armedTime = prefs.getLong(KEY_GEOFENCE_ARMED_TIME, 0L)
        val now = System.currentTimeMillis()

        val refLat = if (stopLat != 0.0) stopLat else centerLat
        val refLng = if (stopLng != 0.0) stopLng else centerLng
        if (refLat == 0.0 || refLng == 0.0) return

        val refTime = if (stopTime > 0L) stopTime else armedTime
        val isRecentVehicleSession = wasMovingInCar || isArmed || (refTime > 0L && (now - refTime) < 45 * 60 * 1000L)
        if (!isRecentVehicleSession) return

        val lastAutoSave = prefs.getLong(KEY_LAST_AUTO_SAVE_TIME, 0L)
        if (now - lastAutoSave < 90_000L) return

        // Verify with Android Activity API
        if (!isWalkingAccordingToActivityApi(context)) {
            Log.d(TAG, "checkWalkingDepartureAutoSave: Activity API does not confirm walking. Suppressing.")
            return
        }

        val curLoc = com.example.sensor.LocationHelper.lastKnownLocation
            ?: com.example.sensor.LocationHelper.getPersistedLocation(context)

        var distance = 0f
        var curAcc = 10f
        if (curLoc != null && curLoc.latitude != 0.0 && curLoc.longitude != 0.0) {
            val dist = FloatArray(1)
            android.location.Location.distanceBetween(refLat, refLng, curLoc.latitude, curLoc.longitude, dist)
            distance = dist[0]
            if (curLoc.accuracy > 0f) curAcc = curLoc.accuracy
        }

        val stepsAtStop = prefs.getInt(KEY_STEPS_AT_STOP, -1)
        val curSteps = if (lastInMemoryStepCount >= 0) lastInMemoryStepCount else prefs.getInt(KEY_CURRENT_STEP_COUNT, -1)
        val stepsTaken = if (stepsAtStop >= 0 && curSteps >= stepsAtStop) (curSteps - stepsAtStop) else 0

        // Require genuine departure distance beyond GPS accuracy jitter OR concrete footsteps
        val minDepartureDist = kotlin.math.max(15f, curAcc * 0.65f).coerceAtMost(28f)
        val hasWalkedAway = (distance >= minDepartureDist && (curSteps < 0 || stepsTaken >= 6)) || stepsTaken >= 10
        if (hasWalkedAway) {
            Log.d(TAG, "Android Activity API confirms walking departure ($distance m, $stepsTaken steps). Auto-saving car at ($refLat, $refLng)!")
            prefs.edit()
                .putBoolean(KEY_IS_MOVING_IN_CAR, false)
                .putLong(KEY_STATIONARY_START_TIME, 0L)
                .putBoolean(KEY_IS_GEOFENCE_ARMED, false)
                .putLong(KEY_LAST_AUTO_SAVE_TIME, now)
                .apply()
            triggerWalkingAutoSave(context, refLat, refLng)
        }
    }

    /**
     * Arms candidate stop location from current GPS fix when vehicle exit is detected.
     */
    fun armCandidateStopFromCurrentLocation(context: Context) {
        if (!isWalkAwayEnabled(context)) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val candidateLat = getExactCoordinate(prefs, KEY_CAR_STOP_LAT, 0.0)
        val candidateLng = getExactCoordinate(prefs, KEY_CAR_STOP_LNG, 0.0)
        val lastStopTime = prefs.getLong(KEY_CAR_STOP_TIME, 0L)
        val now = System.currentTimeMillis()
        val isCandidateFresh = candidateLat != 0.0 && candidateLng != 0.0 && (now - lastStopTime < 60_000L)

        if (!isCandidateFresh) {
            CoroutineScope(Dispatchers.IO).launch {
                val loc = withTimeoutOrNull(4000L) {
                    com.example.sensor.LocationHelper.getLocationForAutoPark(context)
                        ?: com.example.sensor.LocationHelper.getCurrentLocation(context)
                } ?: com.example.sensor.LocationHelper.lastKnownLocation
                  ?: com.example.sensor.LocationHelper.getPersistedLocation(context)

                if (loc != null && (loc.latitude != 0.0 || loc.longitude != 0.0)) {
                    val resolvedLat = loc.latitude
                    val resolvedLng = loc.longitude
                    val editor = prefs.edit()
                        .putBoolean(KEY_CAR_STOP_LOCKED, true)
                        .putFloat(KEY_CAR_STOP_ALT, loc.altitude.toFloat())
                        .putFloat(KEY_CAR_STOP_ACC, loc.accuracy)
                        .putLong(KEY_CAR_STOP_TIME, System.currentTimeMillis())
                    putExactCoordinate(editor, KEY_CAR_STOP_LAT, resolvedLat)
                    putExactCoordinate(editor, KEY_CAR_STOP_LNG, resolvedLng)
                    editor.apply()
                    Log.d(TAG, "Arming geofence perimeter around exact fix ($resolvedLat, $resolvedLng, ±${loc.accuracy}m).")
                    registerCarPerimeterGeofence(context, resolvedLat, resolvedLng)
                }
            }
        } else {
            prefs.edit().putBoolean(KEY_CAR_STOP_LOCKED, true).apply()
            Log.d(TAG, "Arming geofence perimeter around fresh candidate ($candidateLat, $candidateLng).")
            registerCarPerimeterGeofence(context, candidateLat, candidateLng)
        }
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
        if (!isWalkAwayEnabled(context)) return
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
     * Records car location during movement and stops with:
     * 1. Uncertainty-compensated derived speed (prevents indoor cell/Wi-Fi jumps from falsing as driving).
     * 2. Full 64-bit Double coordinate precision (zero Float quantization error).
     * 3. Inverse-variance (Kalman) stationary stop refinement while sitting in the vehicle before footsteps begin,
     *    followed by strict coordinate freeze once footsteps are detected so walking away never pulls the spot down the street.
     * 4. Automatic 3-tier power scaling between High-Precision Driving/Stop mode and Zero-Drain Idle Standby.
     */
    fun recordCarLocation(
        context: Context,
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0,
        accuracy: Float = 10f,
        speedMps: Float = 0f
    ) {
        if (!isWalkAwayEnabled(context)) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Car Bluetooth Connection Safeguard:
        // If phone is currently connected to car Bluetooth, user is inside the vehicle!
        // Suppress red light stop detection, geofence arming, and walk-away auto-save.
        if (com.example.util.BluetoothDeviceHelper.isAnyCarBluetoothConnected(context)) {
            val wasArmed = prefs.getBoolean(KEY_IS_GEOFENCE_ARMED, false)
            if (wasArmed) {
                removeGeofences(context)
            }
            prefs.edit()
                .putBoolean(KEY_IS_MOVING_IN_CAR, true)
                .putBoolean(KEY_CAR_STOP_LOCKED, false)
                .putLong(KEY_STATIONARY_START_TIME, 0L)
                .remove(KEY_STEPS_AT_STOP)
                .apply()
            return
        }

        val now = System.currentTimeMillis()
        val wasMovingInCar = prefs.getBoolean(KEY_IS_MOVING_IN_CAR, false)
        val isLocked = prefs.getBoolean(KEY_CAR_STOP_LOCKED, false)
        val candidateLat = getExactCoordinate(prefs, KEY_CAR_STOP_LAT, 0.0)
        val candidateLng = getExactCoordinate(prefs, KEY_CAR_STOP_LNG, 0.0)
        val lastStopTime = prefs.getLong(KEY_CAR_STOP_TIME, 0L)
        val isHighAccuracy = accuracy in 0.001f..HIGH_ACCURACY_THRESHOLD_METERS

        // Calculate uncertainty-compensated derived speed if Android Location.hasSpeed() is false or speed <= 0
        val lastTime = prefs.getLong(KEY_LAST_DRIVING_TIME, 0L)
        val lastLat = getExactCoordinate(prefs, KEY_LAST_DRIVING_LAT, 0.0)
        val lastLng = getExactCoordinate(prefs, KEY_LAST_DRIVING_LNG, 0.0)
        val lastAcc = prefs.getFloat(KEY_LAST_DRIVING_ACC, 10f).coerceAtLeast(3f)
        val curAcc = if (accuracy > 0f) accuracy else 15f

        var calculatedSpeed = 0f
        var rawSegmentDist = 0f
        val timeDeltaMs = now - lastTime
        if (lastTime > 0L && timeDeltaMs in 1500L..90_000L && lastLat != 0.0 && lastLng != 0.0 && latitude != 0.0 && longitude != 0.0) {
            val distArray = FloatArray(1)
            android.location.Location.distanceBetween(lastLat, lastLng, latitude, longitude, distArray)
            rawSegmentDist = distArray[0]
            // Subtract accuracy uncertainty floor so indoor cell/Wi-Fi jitter never looks like vehicle movement
            val uncertaintyAllowance = 0.55f * (lastAcc + curAcc)
            val netDisplacement = (rawSegmentDist - uncertaintyAllowance).coerceAtLeast(0f)
            calculatedSpeed = netDisplacement / (timeDeltaMs / 1000f)
        }

        val effectiveSpeed = when {
            speedMps > 0f && curAcc <= 28f -> speedMps
            speedMps > 0f && curAcc > 28f -> kotlin.math.min(speedMps, calculatedSpeed)
            else -> calculatedSpeed
        }

        // Save last reference fix with exact 64-bit precision
        if (latitude != 0.0 && longitude != 0.0 && curAcc <= 65f) {
            val refEd = prefs.edit()
                .putFloat(KEY_LAST_DRIVING_ALT, altitude.toFloat())
                .putFloat(KEY_LAST_DRIVING_ACC, curAcc)
                .putLong(KEY_LAST_DRIVING_TIME, now)
            putExactCoordinate(refEd, KEY_LAST_DRIVING_LAT, latitude)
            putExactCoordinate(refEd, KEY_LAST_DRIVING_LNG, longitude)
            refEd.apply()
        }

        val curSteps = if (lastInMemoryStepCount >= 0) lastInMemoryStepCount else prefs.getInt(KEY_CURRENT_STEP_COUNT, -1)

        // 1. Vehicle driving condition (effectiveSpeed >= 2.2 m/s ~ 8 km/h)
        // Require either explicit Doppler speedMps >= 2.2f, prior driving state, or rawSegmentDist >= 25m
        val isGenuineDrivingSpeed = effectiveSpeed >= 2.2f && (speedMps >= 2.2f || wasMovingInCar || rawSegmentDist >= 25f)
        if (isGenuineDrivingSpeed) {
            if (isHighAccuracy) {
                addHighAccuracyFix(context, latitude, longitude, altitude, curAcc, now)
            }

            val wasArmed = prefs.getBoolean(KEY_IS_GEOFENCE_ARMED, false)
            val editor = prefs.edit()
                .putBoolean(KEY_IS_MOVING_IN_CAR, true)
                .putBoolean(KEY_CAR_STOP_LOCKED, false)
                .putFloat(KEY_CAR_STOP_ALT, altitude.toFloat())
                .putFloat(KEY_CAR_STOP_ACC, curAcc)
                .putLong(KEY_CAR_STOP_TIME, now)
                .putLong(KEY_STATIONARY_START_TIME, 0L)
                .putLong(KEY_LAST_STATIONARY_DWELL_MS, 0L)
                .putBoolean(KEY_EXPLICIT_VEHICLE_EXIT, false)
                .putBoolean(KEY_UNDERGROUND_FALLBACK_ACTIVE, false)
            putExactCoordinate(editor, KEY_CAR_STOP_LAT, latitude)
            putExactCoordinate(editor, KEY_CAR_STOP_LNG, longitude)
            // Keep step baseline synced to current step count while driving so pre-drive steps are zeroed out
            if (curSteps >= 0) {
                editor.putInt(KEY_STEPS_AT_STOP, curSteps)
            } else {
                editor.remove(KEY_STEPS_AT_STOP)
            }
            editor.apply()

            if (wasArmed) {
                Log.d(TAG, "Vehicle driving detected (speed: $effectiveSpeed m/s). Disarming geofence.")
                removeGeofences(context)
            }
            syncAdaptivePowerTier(context, forceHighPrecision = true)
        } else if (effectiveSpeed < 1.5f) {
            // 2. Vehicle stopped condition
            if (wasMovingInCar || (lastStopTime > 0L && (now - lastStopTime < 15 * 60 * 1000L))) {
                val stepsAtStop = prefs.getInt(KEY_STEPS_AT_STOP, -1)
                val stepsTakenSoFar = if (stepsAtStop >= 0 && curSteps >= stepsAtStop) (curSteps - stepsAtStop) else 0
                val isWalkingNow = isWalkingAccordingToActivityApi(context) || stepsTakenSoFar >= 3

                if (!isLocked) {
                    val isDegradedAccuracy = curAcc > DEGRADED_ACCURACY_THRESHOLD_METERS || accuracy <= 0f
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
                        Log.w(TAG, "Underground parking safeguard: Current accuracy degraded (${curAcc}m). Using entrance fix: ($effectiveLat, $effectiveLng).")
                    } else {
                        effectiveLat = latitude
                        effectiveLng = longitude
                        effectiveAlt = altitude
                        effectiveAcc = curAcc
                        usedUndergroundFallback = false
                        if (isHighAccuracy) {
                            addHighAccuracyFix(context, latitude, longitude, altitude, curAcc, now)
                        }
                    }

                    val editor = prefs.edit()
                        .putBoolean(KEY_CAR_STOP_LOCKED, true)
                        .putFloat(KEY_CAR_STOP_ALT, effectiveAlt.toFloat())
                        .putFloat(KEY_CAR_STOP_ACC, effectiveAcc)
                        .putLong(KEY_CAR_STOP_TIME, now)
                        .putBoolean(KEY_UNDERGROUND_FALLBACK_ACTIVE, usedUndergroundFallback)
                    putExactCoordinate(editor, KEY_CAR_STOP_LAT, effectiveLat)
                    putExactCoordinate(editor, KEY_CAR_STOP_LNG, effectiveLng)

                    if (usedUndergroundFallback) {
                        editor.putFloat(KEY_UNDERGROUND_ORIGINAL_ACC, curAcc)
                        editor.putFloat(KEY_UNDERGROUND_FALLBACK_ACC, effectiveAcc)
                        editor.putLong(KEY_UNDERGROUND_FALLBACK_TIME, lastReliable?.timestamp ?: now)
                    }

                    if (curSteps >= 0 && !prefs.contains(KEY_STEPS_AT_STOP)) {
                        editor.putInt(KEY_STEPS_AT_STOP, curSteps)
                    }

                    var stationaryStart = prefs.getLong(KEY_STATIONARY_START_TIME, 0L)
                    if (stationaryStart == 0L) {
                        stationaryStart = now
                        editor.putLong(KEY_STATIONARY_START_TIME, stationaryStart)
                    }
                    editor.apply()
                    Log.d(TAG, "Vehicle stopped! Locked exact candidate parking spot at ($effectiveLat, $effectiveLng, ±${effectiveAcc}m).")
                } else if (!isWalkingNow && effectiveSpeed < 0.8f && curAcc in 0.1f..18f && candidateLat != 0.0 && candidateLng != 0.0) {
                    // Stationary Kalman / Inverse-Variance Refinement BEFORE driver takes footsteps:
                    // While the car is parked and the driver has not yet started walking (stepsTaken < 3),
                    // if a sharper GPS fix arrives within 22m of the initial stop lock, refine the parked car
                    // coordinates toward the higher-accuracy stationary fix!
                    val prevAcc = prefs.getFloat(KEY_CAR_STOP_ACC, 15f).coerceAtLeast(2f)
                    val distToLock = FloatArray(1)
                    android.location.Location.distanceBetween(candidateLat, candidateLng, latitude, longitude, distToLock)
                    if (distToLock[0] <= 22f && curAcc <= prevAcc) {
                        val wNew = (1.0 / (curAcc * curAcc)) / ((1.0 / (prevAcc * prevAcc)) + (1.0 / (curAcc * curAcc)))
                        val refinedLat = candidateLat + (latitude - candidateLat) * wNew
                        val refinedLng = candidateLng + (longitude - candidateLng) * wNew
                        val refinedAcc = kotlin.math.sqrt(1.0 / ((1.0 / (prevAcc * prevAcc)) + (1.0 / (curAcc * curAcc)))).toFloat().coerceAtLeast(2.5f)
                        val editor = prefs.edit()
                            .putFloat(KEY_CAR_STOP_ACC, refinedAcc)
                            .putFloat(KEY_CAR_STOP_ALT, altitude.toFloat())
                        putExactCoordinate(editor, KEY_CAR_STOP_LAT, refinedLat)
                        putExactCoordinate(editor, KEY_CAR_STOP_LNG, refinedLng)
                        editor.apply()
                        Log.d(TAG, "Refined stationary car stop to ($refinedLat, $refinedLng, ±${refinedAcc}m) before footsteps.")
                    }
                }

                val stationaryStart = prefs.getLong(KEY_STATIONARY_START_TIME, now)
                val dwellMs = now - stationaryStart
                val explicitExit = prefs.getBoolean(KEY_EXPLICIT_VEHICLE_EXIT, false)
                val isArmed = prefs.getBoolean(KEY_IS_GEOFENCE_ARMED, false)
                val hasStartedWalking = stepsTakenSoFar >= 8

                if (!isArmed && (dwellMs >= STATIONARY_DWELL_TIMER_MS || explicitExit || hasStartedWalking)) {
                    val lockLat = getExactCoordinate(prefs, KEY_CAR_STOP_LAT, 0.0)
                    val lockLng = getExactCoordinate(prefs, KEY_CAR_STOP_LNG, 0.0)
                    if (lockLat != 0.0 && lockLng != 0.0) {
                        Log.d(TAG, "Arming geofence perimeter around locked stop ($lockLat, $lockLng). Dwell: ${dwellMs}ms.")
                        prefs.edit().putLong(KEY_LAST_STATIONARY_DWELL_MS, dwellMs).apply()
                        registerCarPerimeterGeofence(context, lockLat, lockLng, DEFAULT_PERIMETER_RADIUS_METERS)
                    }
                }
            } else {
                // Not driving and no recent stop -> ensure we are in zero-drain idle tier
                syncAdaptivePowerTier(context, forceHighPrecision = false)
            }
        }

        // 3. Walking departure check
        val latestStopLat = getExactCoordinate(prefs, KEY_CAR_STOP_LAT, 0.0)
        val latestStopLng = getExactCoordinate(prefs, KEY_CAR_STOP_LNG, 0.0)
        val centerLat = getExactCoordinate(prefs, KEY_GEOFENCE_CENTER_LAT, 0.0)
        val centerLng = getExactCoordinate(prefs, KEY_GEOFENCE_CENTER_LNG, 0.0)
        val refLat = if (latestStopLat != 0.0) latestStopLat else centerLat
        val refLng = if (latestStopLng != 0.0) latestStopLng else centerLng
        val isArmed = prefs.getBoolean(KEY_IS_GEOFENCE_ARMED, false)
        val armedTime = prefs.getLong(KEY_GEOFENCE_ARMED_TIME, 0L)

        val hasValidParkPoint = refLat != 0.0 && refLng != 0.0
        val isRecentSession = wasMovingInCar || (lastStopTime > 0L && (now - lastStopTime < 2 * 60 * 60 * 1000L)) || isArmed || (armedTime > 0L && (now - armedTime < 2 * 60 * 60 * 1000L))

        if (hasValidParkPoint && isRecentSession) {
            val dist = FloatArray(1)
            android.location.Location.distanceBetween(refLat, refLng, latitude, longitude, dist)

            val stepsAtStop = prefs.getInt(KEY_STEPS_AT_STOP, -1)
            val stepsTaken = if (stepsAtStop >= 0 && curSteps >= stepsAtStop) (curSteps - stepsAtStop) else 0
            val hasPhysicalSteps = curSteps < 0 || stepsTaken >= 6

            val isPedestrianSpeed = effectiveSpeed in 0.3f..2.2f || effectiveSpeed == 0f
            val isWalkingConfirmed = isWalkingAccordingToActivityApi(context) && hasPhysicalSteps
            val minWalkDist = kotlin.math.max(15f, curAcc * 0.6f).coerceAtMost(26f)
            val hasWalkedAway = (dist[0] >= minWalkDist && isPedestrianSpeed && isWalkingConfirmed) ||
                    (dist[0] >= DEFAULT_PERIMETER_RADIUS_METERS && effectiveSpeed < 2.5f && isWalkingConfirmed) ||
                    (stepsTaken >= 10 && dist[0] >= 12f)

            if (hasWalkedAway) {
                Log.d(TAG, "Walking departure confirmed! Distance: ${dist[0]}m away from locked car position ($refLat, $refLng) ($stepsTaken steps). Auto-saving!")
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
        val stopLat = getExactCoordinate(prefs, KEY_CAR_STOP_LAT, 0.0)
        val stopLng = getExactCoordinate(prefs, KEY_CAR_STOP_LNG, 0.0)
        val centerLat = getExactCoordinate(prefs, KEY_GEOFENCE_CENTER_LAT, 0.0)
        val centerLng = getExactCoordinate(prefs, KEY_GEOFENCE_CENTER_LNG, 0.0)

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

        // 4. CRITICAL: Android Activity API Walking Verification
        // Save the location ONLY when the Android Activity API says that the user is walking!
        if (!isWalkingAccordingToActivityApi(context)) {
            Log.w(TAG, "Safeguard: Android Activity API does NOT say the user is walking. Rejecting false trigger to prevent misfire.")
            return false
        }

        Log.d(TAG, "Safeguard verified: valid parking coordinates and walking departure confirmed by Android Activity API.")
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
        if (!isWalkAwayEnabled(context)) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit().putBoolean(KEY_IS_MOVING_IN_CAR, true)
        if (latitude != null && longitude != null) {
            putExactCoordinate(editor, KEY_CAR_STOP_LAT, latitude)
            putExactCoordinate(editor, KEY_CAR_STOP_LNG, longitude)
            editor.putLong(KEY_CAR_STOP_TIME, System.currentTimeMillis())
        }
        editor.apply()
        syncAdaptivePowerTier(context, forceHighPrecision = true)
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
        if (!isWalkAwayEnabled(context)) {
            Log.d(TAG, "Geofencing auto-park is disabled in settings. Skipping geofence registration.")
            return
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Synchronously record candidate car stop and armed center coordinates with 64-bit precision
        val editor = prefs.edit()
            .putLong(KEY_CAR_STOP_TIME, System.currentTimeMillis())
            .putLong(KEY_GEOFENCE_ARMED_TIME, System.currentTimeMillis())
            .putBoolean(KEY_IS_GEOFENCE_ARMED, true)
        putExactCoordinate(editor, KEY_CAR_STOP_LAT, latitude)
        putExactCoordinate(editor, KEY_CAR_STOP_LNG, longitude)
        putExactCoordinate(editor, KEY_GEOFENCE_CENTER_LAT, latitude)
        putExactCoordinate(editor, KEY_GEOFENCE_CENTER_LNG, longitude)
        editor.apply()

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
        if (!isWalkAwayEnabled(context)) return
        if (latitude == 0.0 && longitude == 0.0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val editor = prefs.edit()
            .putLong(KEY_CAR_STOP_TIME, now)
            .putLong(KEY_LAST_STATIONARY_DWELL_MS, 30_000L)
            .putBoolean(KEY_IS_MOVING_IN_CAR, true)
            .putBoolean(KEY_EXPLICIT_VEHICLE_EXIT, true)
        putExactCoordinate(editor, KEY_CAR_STOP_LAT, latitude)
        putExactCoordinate(editor, KEY_CAR_STOP_LNG, longitude)
        editor.apply()

        registerCarPerimeterGeofence(context, latitude, longitude, DEFAULT_PERIMETER_RADIUS_METERS)
        syncAdaptivePowerTier(context, forceHighPrecision = true)
    }

    /**
     * Removes all active car perimeter geofences without instantiating GeofencingClient
     * or PendingIntent if no geofence PendingIntent currently exists.
     */
    fun removeGeofences(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.contains(KEY_IS_GEOFENCE_ARMED)) {
                prefs.edit().putBoolean(KEY_IS_GEOFENCE_ARMED, false).apply()
            }
            val existingPi = findExistingPendingIntent(context, 1001, ACTION_GEOFENCE_TRANSITION)
            if (existingPi != null) {
                val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
                geofencingClient.removeGeofences(existingPi)
                    .addOnCompleteListener {
                        try { existingPi.cancel() } catch (_: Throwable) {}
                    }
                    .addOnSuccessListener {
                        Log.d(TAG, "Successfully removed active geofences.")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to remove geofences: ${e.message}")
                    }
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
        setReceiverComponentEnabled(context, true)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentLoc = com.example.sensor.LocationHelper.lastKnownLocation
            ?: com.example.sensor.LocationHelper.getPersistedLocation(context)
        val fallbackLat = if (currentLoc != null && currentLoc.latitude != 0.0) currentLoc.latitude else 37.4220
        val fallbackLng = if (currentLoc != null && currentLoc.longitude != 0.0) currentLoc.longitude else -122.0841

        val carLat = latitude ?: run {
            val savedLat = getExactCoordinate(prefs, KEY_CAR_STOP_LAT, 0.0)
            if (savedLat != 0.0) savedLat else fallbackLat
        }
        val carLng = longitude ?: run {
            val savedLng = getExactCoordinate(prefs, KEY_CAR_STOP_LNG, 0.0)
            if (savedLng != 0.0) savedLng else fallbackLng
        }

        // Mark that user was moving in car and stopped at this spot, with confirmed walking in Activity API
        val editor = prefs.edit()
            .putBoolean(KEY_IS_MOVING_IN_CAR, true)
            .putLong(KEY_CAR_STOP_TIME, System.currentTimeMillis())
            .putBoolean(KEY_IS_GEOFENCE_ARMED, true)
            .putBoolean(KEY_IS_CURRENTLY_WALKING, true)
            .putLong(KEY_LAST_WALKING_ACTIVITY_TIME, System.currentTimeMillis())
            .putInt(KEY_LAST_DETECTED_ACTIVITY_TYPE, DetectedActivity.WALKING)
            .putInt(KEY_LAST_DETECTED_ACTIVITY_CONFIDENCE, 100)
        putExactCoordinate(editor, KEY_CAR_STOP_LAT, carLat)
        putExactCoordinate(editor, KEY_CAR_STOP_LNG, carLng)
        putExactCoordinate(editor, KEY_GEOFENCE_CENTER_LAT, carLat)
        putExactCoordinate(editor, KEY_GEOFENCE_CENTER_LNG, carLng)
        editor.apply()

        // Fire simulation intent to GeofenceTransitionsReceiver
        val intent = Intent(context, GeofenceTransitionsReceiver::class.java).apply {
            action = ACTION_SIMULATE_GEOFENCE_EXIT
            putExtra("car_lat", carLat)
            putExtra("car_lng", carLng)
            // Simulated user walking location (e.g. 70 meters away, outside the 50m perimeter)
            putExtra("simulated_lat", carLat + 0.0007)
            putExtra("simulated_lng", carLng + 0.0007)
            putExtra("is_simulated", true)
        }
        context.sendBroadcast(intent)
    }
}
