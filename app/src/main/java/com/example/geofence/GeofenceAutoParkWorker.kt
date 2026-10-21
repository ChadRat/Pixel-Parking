package com.example.geofence

import android.app.Notification
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.data.database.AppDatabase
import com.example.data.entity.ParkingSpot
import com.example.data.entity.isBluetoothEntry
import com.example.data.repository.ParkingRepository
import com.example.notification.ParkingNotificationHelper
import com.example.sensor.LocationHelper
import com.example.ui.i18n.getAppStrings
import com.example.util.HapticHelper
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Expedited WorkManager task that offloads database persistence, Wear OS companion sync,
 * and network-dependent Geocoder address resolution when parking is detected upon walking departure.
 *
 * CRITICAL RELIABILITY RULES:
 * 1. Saves raw coordinates immediately to Room DB so underground / offline parking events are never lost.
 * 2. Attempts reverse geocoding with a strict timeout so hanging network calls cannot freeze execution.
 * 3. Complies with modern Android background restrictions without holding manual WakeLocks in BroadcastReceivers.
 */
class GeofenceAutoParkWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "GeofenceAutoParkWorker"
        const val KEY_CAR_LAT = "car_lat"
        const val KEY_CAR_LNG = "car_lng"
        const val KEY_CAR_ALT = "car_alt"
        const val KEY_CAR_ACC = "car_acc"
        const val KEY_IS_SIMULATED = "is_simulated"
        const val KEY_UNDERGROUND_FALLBACK_ACTIVE = "underground_fallback_active"
        const val KEY_UNDERGROUND_ORIGINAL_ACC = "underground_original_acc"
        const val KEY_UNDERGROUND_FALLBACK_ACC = "underground_fallback_acc"
        private const val EXPEDITED_NOTIFICATION_ID = 2001
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        ParkingNotificationHelper.createNotificationChannels(context)
        val notification: Notification = NotificationCompat.Builder(context, ParkingNotificationHelper.CHANNEL_PARKING_ALERTS)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Saving parking location")
            .setContentText("Saving your parked car coordinates...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        return ForegroundInfo(EXPEDITED_NOTIFICATION_ID, notification)
    }

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences(GeofenceManager.PREFS_NAME, Context.MODE_PRIVATE)
        val isGeofenceEnabled = prefs.getBoolean(GeofenceManager.KEY_GEOFENCE_ENABLED, false)
        val isSimulated = inputData.getBoolean(KEY_IS_SIMULATED, false)
        if (!isGeofenceEnabled && !isSimulated) {
            Log.d(TAG, "Geofence auto-park is disabled in settings. Skipping auto-save.")
            return Result.success()
        }

        // 1. Resolve candidate car coordinates (persisted in SharedPreferences)
        var carLat = inputData.getDouble(KEY_CAR_LAT, 0.0)
        var carLng = inputData.getDouble(KEY_CAR_LNG, 0.0)
        var carAlt = inputData.getDouble(KEY_CAR_ALT, 0.0)
        var carAcc = inputData.getFloat(KEY_CAR_ACC, 10f)

        if (carLat == 0.0 || carLng == 0.0) {
            val candidateLat = prefs.getFloat(GeofenceManager.KEY_CAR_STOP_LAT, 0f).toDouble()
            val candidateLng = prefs.getFloat(GeofenceManager.KEY_CAR_STOP_LNG, 0f).toDouble()
            if (candidateLat != 0.0 && candidateLng != 0.0) {
                carLat = candidateLat
                carLng = candidateLng
                carAlt = prefs.getFloat(GeofenceManager.KEY_CAR_STOP_ALT, 0f).toDouble()
                carAcc = prefs.getFloat(GeofenceManager.KEY_CAR_STOP_ACC, 10f)
            } else {
                val centerLat = prefs.getFloat(GeofenceManager.KEY_GEOFENCE_CENTER_LAT, 0f).toDouble()
                val centerLng = prefs.getFloat(GeofenceManager.KEY_GEOFENCE_CENTER_LNG, 0f).toDouble()
                if (centerLat != 0.0 && centerLng != 0.0) {
                    carLat = centerLat
                    carLng = centerLng
                    carAlt = 0.0
                    carAcc = 15f
                } else {
                    val loc = LocationHelper.getLocationForAutoPark(context)
                        ?: LocationHelper.getCurrentLocation(context)
                    if (loc != null && (loc.latitude != 0.0 || loc.longitude != 0.0)) {
                        carLat = loc.latitude
                        carLng = loc.longitude
                        carAlt = loc.altitude
                        carAcc = loc.accuracy
                    } else {
                        Log.w(TAG, "No valid car location fix available for auto-park.")
                        return Result.failure()
                    }
                }
            }
        }

        // Passenger Drop-Off Safeguard & Coordinate Staleness Check
        if (!isSimulated) {
            val safeguardPassed = GeofenceManager.verifyPassengerDropOffSafeguard(context, carLat, carLng)
            if (!safeguardPassed) {
                Log.w(TAG, "Passenger drop-off safeguard: Rejected false parking trigger (moving ride or stale coordinates).")
                return Result.failure()
            }
        }

        val db = AppDatabase.getDatabase(context)
        val repository = ParkingRepository(
            db.parkingSpotDao(),
            db.bluetoothCarDeviceDao()
        )
        val strings = context.getAppStrings()

        val isUndergroundFallback = inputData.getBoolean(KEY_UNDERGROUND_FALLBACK_ACTIVE, false) ||
            prefs.getBoolean(GeofenceManager.KEY_UNDERGROUND_FALLBACK_ACTIVE, false)
        val undergroundOrigAcc = inputData.getFloat(
            KEY_UNDERGROUND_ORIGINAL_ACC,
            prefs.getFloat(GeofenceManager.KEY_UNDERGROUND_ORIGINAL_ACC, 45f)
        )

        val spotNote = if (isUndergroundFallback) {
            String.format(
                strings.undergroundFallbackNote,
                undergroundOrigAcc.toInt(),
                carAcc.toInt()
            )
        } else {
            strings.geofenceExitNote
        }

        val spotFloor = if (isUndergroundFallback) {
            strings.undergroundFloorLevel
        } else {
            strings.floorGroundLevel
        }

        // 2. CRITICAL: Save RAW COORDINATES immediately to Room Database!
        // This ensures the app saves coordinates right away even if network/underground geocoding hangs.
        val spot = ParkingSpot(
            latitude = carLat,
            longitude = carLng,
            altitude = carAlt,
            accuracyMeters = carAcc,
            address = "", // Raw coordinates saved immediately
            spotName = strings.geofenceSavedSpotDefaultName,
            floorLevel = spotFloor,
            note = spotNote,
            timestamp = System.currentTimeMillis(),
            isAutoSaved = true,
            bluetoothDeviceName = "Walk Away Detection",
            bluetoothDeviceAddress = "GEOFENCE_WALKING_DETECTION",
            meterExpiryTimestamp = null,
            photoUri = null,
            isActive = true
        )

        val allowGeofenceAfterBt = prefs.getBoolean(GeofenceManager.KEY_DEV_ALLOW_GEOFENCE_AFTER_BT, false)
        val priorSpot = repository.getMostRecentSpotDirect()
        val newId = repository.saveNewParkingSpot(spot, allowGeofenceAfterBt)
        val isMergedIntoBtSpot = !allowGeofenceAfterBt && (priorSpot != null && priorSpot.id == newId && priorSpot.isBluetoothEntry)
        var savedSpot = spot.copy(id = newId)
        GeofenceManager.clearGpsBuffer(context)
        Log.d(TAG, "Successfully auto-saved RAW coordinates to Room DB with ID: $newId at ($carLat, $carLng)")

        // If underground safeguard was triggered, inform the user with a distinct notification/alert
        if (isUndergroundFallback && !isMergedIntoBtSpot) {
            try {
                val notifTitle = strings.undergroundSafeguardNotificationTitle
                val notifMsg = String.format(strings.undergroundSafeguardNotificationText, carAcc.toInt())
                ParkingNotificationHelper.showSystemNotification(context, notifTitle, notifMsg)
                Log.i(TAG, "Underground parking safeguard: Notified user of entrance coordinate fallback.")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to display underground safeguard alert notification: ${e.message}")
            }
        }

        // 3. Network-dependent Geocoder lookup with safety timeout (so underground / offline calls never hang the worker)
        try {
            val resolvedAddress = withTimeoutOrNull(3_500L) {
                try {
                    LocationHelper.getAddressFromCoordinates(context, carLat, carLng)
                } catch (e: Exception) {
                    Log.w(TAG, "Geocoder lookup failed or offline: ${e.message}")
                    null
                }
            }
            if (!resolvedAddress.isNullOrBlank()) {
                savedSpot = savedSpot.copy(address = resolvedAddress)
                repository.updateParkingSpot(savedSpot)
                Log.d(TAG, "Updated spot $newId with resolved address: $resolvedAddress")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error in geocoder background lookup: ${e.message}")
        }

        // 4. Mark trip complete, cleanly wipe transient candidate parking state so future trips start pristine
        GeofenceManager.clearCandidateParkingState(context)

        // 5. Remove or disarm active geofences
        GeofenceManager.removeGeofences(context)

        // 6. Sync active spot to Wear OS companion app
        try {
            com.example.wear.WearDataLayerBridge.getInstance(context).syncParkingSpot(savedSpot)
        } catch (_: Throwable) {}

        // 7. Show rich system notification with Instant Navigate action (unless smoothly merged into existing BT spot)
        if (!isMergedIntoBtSpot) {
            ParkingNotificationHelper.showCarParkedNotification(context, savedSpot)

            // 8. Haptic vibration confirmation
            HapticHelper.getVibrator(context)?.let { vibrator ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 150, 100, 200),
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 150, 100, 200), -1)
                }
            }
        } else {
            Log.d(TAG, "Geofence event merged smoothly into existing Bluetooth parking spot without re-notifying.")
        }

        return Result.success()
    }
}
