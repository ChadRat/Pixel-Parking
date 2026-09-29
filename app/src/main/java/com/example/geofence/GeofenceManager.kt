package com.example.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.receiver.GeofenceTransitionsReceiver
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object GeofenceManager {

    private const val TAG = "GeofenceManager"
    const val GEOFENCE_REQUEST_ID = "CAR_PERIMETER_GEOFENCE"
    const val DEFAULT_PERIMETER_RADIUS_METERS = 25.0f // 25 meter perimeter around parked car

    const val ACTION_GEOFENCE_TRANSITION = "com.example.autopark.ACTION_GEOFENCE_TRANSITION"
    const val ACTION_SIMULATE_GEOFENCE_EXIT = "com.example.autopark.ACTION_SIMULATE_GEOFENCE_EXIT"
    const val ACTION_ACTIVITY_TRANSITION = "com.example.autopark.ACTION_ACTIVITY_TRANSITION"

    // SharedPreferences keys for car tracking
    const val PREFS_NAME = "auto_park_prefs"
    const val KEY_GEOFENCE_ENABLED = "geofence_auto_park_enabled"
    const val KEY_IS_MOVING_IN_CAR = "is_moving_in_car"
    const val KEY_CAR_STOP_LAT = "candidate_car_park_lat"
    const val KEY_CAR_STOP_LNG = "candidate_car_park_lng"
    const val KEY_CAR_STOP_ALT = "candidate_car_park_alt"
    const val KEY_CAR_STOP_ACC = "candidate_car_park_acc"
    const val KEY_CAR_STOP_TIME = "candidate_car_park_time"
    const val KEY_GEOFENCE_CENTER_LAT = "geofence_center_lat"
    const val KEY_GEOFENCE_CENTER_LNG = "geofence_center_lng"
    const val KEY_GEOFENCE_ARMED_TIME = "geofence_armed_time"

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

    /**
     * Enables or updates the geofence & walking auto-park feature.
     */
    fun startGeofenceWalkingAutoPark(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_GEOFENCE_ENABLED, true).apply()
        registerActivityTransitions(context)
    }

    /**
     * Disables geofence & walking auto-park.
     */
    fun stopGeofenceWalkingAutoPark(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_GEOFENCE_ENABLED, false).apply()
        removeGeofences(context)
        removeActivityTransitions(context)
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
     * Records car location during movement and stops.
     * When vehicular speed is observed, marks user as moving in car.
     * When speed drops to 0 or car stops, arms perimeter geofence around car stop position.
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
        if (!prefs.getBoolean(KEY_GEOFENCE_ENABLED, true)) return

        val now = System.currentTimeMillis()
        val wasMovingInCar = prefs.getBoolean(KEY_IS_MOVING_IN_CAR, false)
        val candidateLat = prefs.getFloat(KEY_CAR_STOP_LAT, 0f).toDouble()
        val candidateLng = prefs.getFloat(KEY_CAR_STOP_LNG, 0f).toDouble()
        val lastStopTime = prefs.getLong(KEY_CAR_STOP_TIME, 0L)

        // 1. If moving at driving speed (>= 2.8 m/s ~ 10 km/h)
        if (speedMps >= 2.8f) {
            prefs.edit()
                .putBoolean(KEY_IS_MOVING_IN_CAR, true)
                .putFloat(KEY_CAR_STOP_LAT, latitude.toFloat())
                .putFloat(KEY_CAR_STOP_LNG, longitude.toFloat())
                .putFloat(KEY_CAR_STOP_ALT, altitude.toFloat())
                .putFloat(KEY_CAR_STOP_ACC, accuracy)
                .putLong(KEY_CAR_STOP_TIME, now)
                .apply()
        } else if (speedMps < 1.2f) {
            // 2. Stopped or parked
            if (wasMovingInCar || (now - lastStopTime < 15 * 60 * 1000L)) {
                // Update car candidate location if stationary
                prefs.edit()
                    .putFloat(KEY_CAR_STOP_LAT, latitude.toFloat())
                    .putFloat(KEY_CAR_STOP_LNG, longitude.toFloat())
                    .putFloat(KEY_CAR_STOP_ALT, altitude.toFloat())
                    .putFloat(KEY_CAR_STOP_ACC, accuracy)
                    .putLong(KEY_CAR_STOP_TIME, now)
                    .apply()

                // Register perimeter geofence around the car's stopped position (25m radius)
                registerCarPerimeterGeofence(context, latitude, longitude, DEFAULT_PERIMETER_RADIUS_METERS)
            }
        }

        // 3. Flawless walking departure check:
        // If user was moving in car and has now walked 25 meters or more away from the candidate car location on foot
        if ((wasMovingInCar || (now - lastStopTime < 30 * 60 * 1000L)) && candidateLat != 0.0 && candidateLng != 0.0) {
            val dist = FloatArray(1)
            android.location.Location.distanceBetween(candidateLat, candidateLng, latitude, longitude, dist)
            if (dist[0] >= DEFAULT_PERIMETER_RADIUS_METERS && speedMps < 3.2f) {
                Log.d(TAG, "Walking detected: User moved ${dist[0]}m (>= 25m) away from parked car. Auto-saving car location!")
                triggerWalkingAutoSave(context, candidateLat, candidateLng)
            }
        }
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
        val isEnabled = prefs.getBoolean(KEY_GEOFENCE_ENABLED, true)
        if (!isEnabled) {
            Log.d(TAG, "Geofencing auto-park is disabled in settings. Skipping geofence registration.")
            return
        }

        try {
            val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

            val geofence = Geofence.Builder()
                .setRequestId(GEOFENCE_REQUEST_ID)
                .setCircularRegion(latitude, longitude, radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                .setNotificationResponsiveness(3000) // 3 second responsiveness
                .build()

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            val pendingIntent = getGeofencePendingIntent(context)

            // Remove any old geofence before registering new perimeter
            geofencingClient.removeGeofences(pendingIntent).addOnCompleteListener {
                try {
                    geofencingClient.addGeofences(request, pendingIntent)
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully registered car perimeter geofence at ($latitude, $longitude) with radius $radiusMeters m")
                            prefs.edit()
                                .putFloat(KEY_GEOFENCE_CENTER_LAT, latitude.toFloat())
                                .putFloat(KEY_GEOFENCE_CENTER_LNG, longitude.toFloat())
                                .putLong(KEY_GEOFENCE_ARMED_TIME, System.currentTimeMillis())
                                .apply()
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to register geofence: ${e.message}")
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
     * Removes all active car perimeter geofences.
     */
    fun removeGeofences(context: Context) {
        try {
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
        val carLat = latitude ?: prefs.getFloat(KEY_CAR_STOP_LAT, 37.4220f).toDouble()
        val carLng = longitude ?: prefs.getFloat(KEY_CAR_STOP_LNG, -122.0841f).toDouble()

        // Mark that user was moving in car and stopped at this spot
        prefs.edit()
            .putBoolean(KEY_IS_MOVING_IN_CAR, true)
            .putFloat(KEY_CAR_STOP_LAT, carLat.toFloat())
            .putFloat(KEY_CAR_STOP_LNG, carLng.toFloat())
            .putFloat(KEY_GEOFENCE_CENTER_LAT, carLat.toFloat())
            .putFloat(KEY_GEOFENCE_CENTER_LNG, carLng.toFloat())
            .putLong(KEY_CAR_STOP_TIME, System.currentTimeMillis())
            .apply()

        // Fire simulation intent to GeofenceTransitionsReceiver
        val intent = Intent(context, GeofenceTransitionsReceiver::class.java).apply {
            action = ACTION_SIMULATE_GEOFENCE_EXIT
            putExtra("car_lat", carLat)
            putExtra("car_lng", carLng)
            // Simulated user walking location (e.g. 50 meters away)
            putExtra("simulated_lat", carLat + 0.0004)
            putExtra("simulated_lng", carLng + 0.0004)
        }
        context.sendBroadcast(intent)
    }
}
