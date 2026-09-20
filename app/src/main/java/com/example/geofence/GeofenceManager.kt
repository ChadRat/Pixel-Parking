package com.example.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.receiver.GeofenceTransitionsReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object GeofenceManager {

    private const val TAG = "GeofenceManager"
    const val GEOFENCE_REQUEST_ID = "CAR_PERIMETER_GEOFENCE"
    const val DEFAULT_PERIMETER_RADIUS_METERS = 80.0f // 80 meter radius around the car
    const val ACTION_GEOFENCE_TRANSITION = "com.example.autopark.ACTION_GEOFENCE_TRANSITION"
    const val ACTION_SIMULATE_GEOFENCE_EXIT = "com.example.autopark.ACTION_SIMULATE_GEOFENCE_EXIT"

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
        val prefs = context.getSharedPreferences("auto_park_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("geofence_auto_park_enabled", false)
        if (!isEnabled) {
            Log.d(TAG, "Geofencing auto-park is disabled in Developer settings. Skipping geofence registration.")
            return
        }

        try {
            val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

            val geofence = Geofence.Builder()
                .setRequestId(GEOFENCE_REQUEST_ID)
                .setCircularRegion(latitude, longitude, radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                .setNotificationResponsiveness(5000) // 5 second responsiveness
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
                            // Save active geofence center to prefs
                            prefs.edit()
                                .putFloat("geofence_center_lat", latitude.toFloat())
                                .putFloat("geofence_center_lng", longitude.toFloat())
                                .putLong("geofence_armed_time", System.currentTimeMillis())
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
     * Simulates a geofence departure for instant testing in Developer settings.
     */
    fun simulateGeofenceExit(context: Context, latitude: Double? = null, longitude: Double? = null) {
        val intent = Intent(context, GeofenceTransitionsReceiver::class.java).apply {
            action = ACTION_SIMULATE_GEOFENCE_EXIT
            if (latitude != null && longitude != null) {
                putExtra("simulated_lat", latitude)
                putExtra("simulated_lng", longitude)
            }
        }
        context.sendBroadcast(intent)
    }
}
