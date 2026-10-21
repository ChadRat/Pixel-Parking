package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.database.AppDatabase
import com.example.data.entity.BluetoothCarDevice
import com.example.data.entity.ParkingSpot
import com.example.notification.ParkingNotificationHelper
import com.example.receiver.BluetoothDisconnectReceiver
import com.example.sensor.LocationHelper
import com.example.ui.i18n.getAppStrings
import com.example.util.BluetoothDeviceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

class AutoParkLocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("WakelockTimeout")
    override fun onCreate() {
        super.onCreate()
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "PixelParking:AutoParkLocationServiceWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(25_000L)
            }
        } catch (_: Throwable) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasRequiredPermissionsForMonitoring(this)) {
            Log.w(TAG, "AutoParkLocationService started without required location permissions; stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent == null) {
            if (isMonitoringEnabled(this)) {
                // System restarted service after memory pressure; restore persistent monitoring
                isMonitoring = true
                ParkingNotificationHelper.createNotificationChannels(this)
                val monitorNotification = ParkingNotificationHelper.buildPersistentMonitorNotification(this, if (isWatching) watchedCarName else null)
                startForegroundWithTypes(ParkingNotificationHelper.NOTIFICATION_ID_CAR_WATCHER, monitorNotification)
                return START_STICKY
            }
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent.action) {
            ACTION_START_MONITORING -> {
                isMonitoring = true
                ParkingNotificationHelper.createNotificationChannels(this)
                val monitorNotification = ParkingNotificationHelper.buildPersistentMonitorNotification(
                    this,
                    if (isWatching) watchedCarName else null
                )
                startForegroundWithTypes(ParkingNotificationHelper.NOTIFICATION_ID_CAR_WATCHER, monitorNotification)
                return START_STICKY
            }

            ACTION_STOP_MONITORING -> {
                isMonitoring = false
                isWatching = false
                safeCleanupAndStop()
                return START_NOT_STICKY
            }

            ACTION_START_WATCHING -> {
                val carName = intent.getStringExtra("car_name") ?: "Car Bluetooth"
                val carAddress = intent.getStringExtra("car_address")
                isWatching = true
                watchedCarName = carName
                watchedCarAddress = carAddress

                ParkingNotificationHelper.createNotificationChannels(this)
                val watchingNotification = ParkingNotificationHelper.buildCarConnectionNotification(this, carName)
                startForegroundWithTypes(ParkingNotificationHelper.NOTIFICATION_ID_CAR_WATCHER, watchingNotification)

                serviceScope.launch {
                    try {
                        LocationHelper.warmUpLocation(this@AutoParkLocationService)
                    } catch (_: Throwable) {}
                }
                return START_STICKY
            }

            ACTION_STOP_WATCHING -> {
                isWatching = false
                if (isMonitoringEnabled(this)) {
                    val monitorNotification = ParkingNotificationHelper.buildPersistentMonitorNotification(this, null)
                    startForegroundWithTypes(ParkingNotificationHelper.NOTIFICATION_ID_CAR_WATCHER, monitorNotification)
                    return START_STICKY
                } else {
                    safeCleanupAndStop()
                    return START_NOT_STICKY
                }
            }

            ACTION_HANDLE_DISCONNECT -> {
                ParkingNotificationHelper.createNotificationChannels(this)
                val initialNotification = buildInitialSavingNotification()
                startForegroundWithTypes(ParkingNotificationHelper.NOTIFICATION_ID_AUTOPARK_SAVING, initialNotification)

                serviceScope.launch {
                    try {
                        handleDisconnectSave(intent)
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error in AutoParkLocationService execution", e)
                    } finally {
                        isWatching = false
                        if (isMonitoringEnabled(this@AutoParkLocationService)) {
                            val monitorNotification = ParkingNotificationHelper.buildPersistentMonitorNotification(this@AutoParkLocationService, null)
                            startForegroundWithTypes(ParkingNotificationHelper.NOTIFICATION_ID_CAR_WATCHER, monitorNotification)
                        } else {
                            safeCleanupAndStop()
                        }
                    }
                }
                return START_STICKY
            }

            else -> {
                if (isMonitoringEnabled(this)) {
                    isMonitoring = true
                    val monitorNotification = ParkingNotificationHelper.buildPersistentMonitorNotification(this, null)
                    startForegroundWithTypes(ParkingNotificationHelper.NOTIFICATION_ID_CAR_WATCHER, monitorNotification)
                    return START_STICKY
                }
                stopSelf()
                return START_NOT_STICKY
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved: App task swiped from recents; persistent monitor remaining active.")
        if (isMonitoringEnabled(this) && hasRequiredPermissionsForMonitoring(this)) {
            val restartIntent = Intent(applicationContext, AutoParkLocationService::class.java).apply {
                action = ACTION_START_MONITORING
            }
            try {
                val restartPendingIntent = PendingIntent.getService(
                    applicationContext,
                    1,
                    restartIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                alarmManager?.set(
                    android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    android.os.SystemClock.elapsedRealtime() + 1000L,
                    restartPendingIntent
                )
            } catch (_: Throwable) {}
        }
    }

    private fun startForegroundWithTypes(notificationId: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasFine || hasCoarse) {
                try {
                    androidx.core.app.ServiceCompat.startForeground(
                        this,
                        notificationId,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                    return
                } catch (e: SecurityException) {
                    Log.w(TAG, "SecurityException starting foreground with location type", e)
                } catch (e: Throwable) {
                    Log.w(TAG, "Error starting foreground with location type", e)
                }
            } else {
                Log.w(TAG, "Location permissions not granted when starting foreground")
            }
        } else {
            try {
                startForeground(notificationId, notification)
            } catch (e: Throwable) {
                Log.w(TAG, "Cannot start legacy foreground service", e)
            }
        }
    }

    private suspend fun handleDisconnectSave(intent: Intent) {
        val strings = getAppStrings()
        val db = AppDatabase.getDatabase(this)
        val repository = com.example.data.repository.ParkingRepository(
            db.parkingSpotDao(),
            db.bluetoothCarDeviceDao()
        )

        val device: BluetoothDevice? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    ?: @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }
        } catch (_: Throwable) { null }

        val rawAddress = intent.getStringExtra("raw_address")
            ?: try { device?.address } catch (_: Throwable) { null }
        val rawName = intent.getStringExtra("raw_name")
            ?: try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    device?.alias ?: device?.name
                } else {
                    device?.name
                }
            } catch (_: Throwable) { null }

        val isSimulatedDisconnect = intent.getBooleanExtra("is_simulated", false)

        var allMonitored = repository.getMonitoredDevicesDirect()
        if (allMonitored.isEmpty()) {
            val allDevices = repository.getAllDevicesDirect()
            val candidateCar = allDevices.firstOrNull { dev ->
                dev.deviceType == "Car" || BluetoothDisconnectReceiver.hasVehicleKeywords(dev.name) || BluetoothDisconnectReceiver.hasVehicleKeywords(dev.originalName)
            } ?: allDevices.firstOrNull()

            if (candidateCar != null) {
                Log.d(TAG, "AutoParkLocationService: Auto-monitoring '${candidateCar.name}' for disconnect auto-save.")
                repository.setDeviceMonitored(candidateCar.address, true)
                allMonitored = listOf(candidateCar.copy(isMonitoredCar = true))
            } else {
                val newCar = BluetoothCarDevice(
                    address = rawAddress ?: "00:00:00:00:00:00",
                    name = rawName ?: "Car Bluetooth",
                    originalName = rawName ?: "Car Bluetooth",
                    deviceType = "Car",
                    isMonitoredCar = true,
                    lastConnectedTimestamp = System.currentTimeMillis()
                )
                repository.registerBluetoothDevice(newCar, setAsPrimary = true)
                allMonitored = listOf(newCar)
            }
        }

        val resolvedVehicle: BluetoothCarDevice? = if (isSimulatedDisconnect) {
            repository.findMatchingMonitoredDevice(rawAddress, rawName)
                ?: if (!rawAddress.isNullOrBlank()) {
                    repository.getDeviceByAddress(rawAddress)?.takeIf { it.isMonitoredCar }
                } else null
                ?: allMonitored.firstOrNull()
        } else if (!rawAddress.isNullOrBlank() || !rawName.isNullOrBlank()) {
            repository.findMatchingMonitoredDevice(rawAddress, rawName)
                ?: if (allMonitored.size == 1) allMonitored.first() else null
                ?: allMonitored.firstOrNull()
        } else {
            allMonitored.firstOrNull()
        }

        val targetVehicle = resolvedVehicle ?: allMonitored.firstOrNull() ?: BluetoothCarDevice(
            name = rawName ?: "Car Bluetooth",
            originalName = rawName ?: "Car Bluetooth",
            address = rawAddress ?: "00:00:00:00:00:00",
            deviceType = "Car",
            isMonitoredCar = true
        )

        val effectiveDeviceName = targetVehicle.name.ifBlank { null }
            ?: targetVehicle.originalName.ifBlank { null }
            ?: rawName?.ifBlank { null }
            ?: "Car Bluetooth"

        val effectiveDeviceAddress = targetVehicle.address.ifBlank { null }
            ?: rawAddress?.ifBlank { null }
            ?: "00:00:00:00:00:00"

        val simLat = intent.getDoubleExtra("simulated_lat", 0.0)
        val simLng = intent.getDoubleExtra("simulated_lng", 0.0)
        val simAlt = intent.getDoubleExtra("simulated_alt", 0.0)
        val simAcc = intent.getFloatExtra("simulated_accuracy", 10f)

        // Stage 1: Rapid Pinpoint Fix Retrieval (Guaranteed to commit immediately)
        val instantFix: Location? = if (simLat != 0.0 || simLng != 0.0) {
            Location("simulation").apply {
                latitude = simLat
                longitude = simLng
                altitude = simAlt
                accuracy = simAcc
            }
        } else {
            LocationHelper.getLocationForAutoPark(this) ?: LocationHelper.getBestAvailableInstantFix(this)
        }

        var lat = instantFix?.latitude ?: 0.0
        var lng = instantFix?.longitude ?: 0.0
        var alt = instantFix?.altitude ?: 0.0
        var accuracy = instantFix?.accuracy ?: 60f

        val spotFloor = strings.floorGroundLevel
        val spotNote = strings.bluetoothDisconnectNote

        var savedSpotId: Long = 0L
        var savedSpot: ParkingSpot? = null

        // Stop ongoing radar service if any
        try { ParkingRadarService.stop(this) } catch (_: Throwable) {}

        if (lat != 0.0 || lng != 0.0) {
            val initialDisplayAddress = withTimeoutOrNull(2200L) {
                LocationHelper.getStreetNameOrCoordinates(this@AutoParkLocationService, lat, lng)
            } ?: LocationHelper.formatCoordinates(lat, lng)

            val spot = ParkingSpot(
                latitude = lat,
                longitude = lng,
                altitude = alt,
                accuracyMeters = accuracy,
                address = initialDisplayAddress,
                spotName = String.format(strings.autoSavedSpotDefaultName, effectiveDeviceName),
                floorLevel = spotFloor,
                note = spotNote,
                timestamp = System.currentTimeMillis(),
                isAutoSaved = true,
                bluetoothDeviceName = effectiveDeviceName,
                bluetoothDeviceAddress = effectiveDeviceAddress,
                meterExpiryTimestamp = null,
                isActive = true
            )

            savedSpotId = repository.saveNewParkingSpot(spot)
            savedSpot = spot.copy(id = savedSpotId)

            com.example.geofence.GeofenceManager.clearGpsBuffer(this)
            com.example.geofence.GeofenceManager.armGeofenceAfterBluetoothSave(this, lat, lng)

            try {
                com.example.wear.WearDataLayerBridge.getInstance(this).syncParkingSpot(savedSpot)
            } catch (_: Throwable) {}

            ParkingNotificationHelper.showCarParkedNotification(this, savedSpot)
            Log.d(TAG, "Stage 1 instant auto-save completed: Spot ID $savedSpotId saved at $initialDisplayAddress (±${accuracy}m)")
        }

        // Stage 2: Background GPS Refinement (up to 2.5s)
        var bestRefinedLat = lat
        var bestRefinedLng = lng
        var bestRefinedAcc = if (accuracy > 0f) accuracy else 100f
        var bestRefinedAlt = alt

        if (simLat == 0.0 && simLng == 0.0) {
            LocationHelper.refineLocationFix(
                context = this,
                currentAccuracy = bestRefinedAcc,
                maxWaitMs = 2_500L
            ) { refinedFix ->
                if ((refinedFix.latitude != 0.0 || refinedFix.longitude != 0.0) && refinedFix.accuracy < bestRefinedAcc) {
                    bestRefinedLat = refinedFix.latitude
                    bestRefinedLng = refinedFix.longitude
                    bestRefinedAcc = refinedFix.accuracy
                    bestRefinedAlt = refinedFix.altitude
                    Log.d(TAG, "Refinement fix received: ($bestRefinedLat, $bestRefinedLng, ±${bestRefinedAcc}m)")
                }
            }
        }

        // Commit refinement or create spot if instant fix was completely unavailable
        if (savedSpot == null && (bestRefinedLat != 0.0 || bestRefinedLng != 0.0)) {
            val address = LocationHelper.getStreetNameOrCoordinates(this, bestRefinedLat, bestRefinedLng)
            val spot = ParkingSpot(
                latitude = bestRefinedLat,
                longitude = bestRefinedLng,
                altitude = bestRefinedAlt,
                accuracyMeters = bestRefinedAcc,
                address = address,
                spotName = String.format(strings.autoSavedSpotDefaultName, effectiveDeviceName),
                floorLevel = spotFloor,
                note = spotNote,
                timestamp = System.currentTimeMillis(),
                isAutoSaved = true,
                bluetoothDeviceName = effectiveDeviceName,
                bluetoothDeviceAddress = effectiveDeviceAddress,
                meterExpiryTimestamp = null,
                isActive = true
            )
            savedSpotId = repository.saveNewParkingSpot(spot)
            savedSpot = spot.copy(id = savedSpotId)
            ParkingNotificationHelper.showCarParkedNotification(this, savedSpot)
            try { com.example.wear.WearDataLayerBridge.getInstance(this).syncParkingSpot(savedSpot) } catch (_: Throwable) {}
            try { com.example.geofence.GeofenceManager.armGeofenceAfterBluetoothSave(this, bestRefinedLat, bestRefinedLng) } catch (_: Throwable) {}
        } else if (savedSpot != null) {
            val address = LocationHelper.getStreetNameOrCoordinates(this, bestRefinedLat, bestRefinedLng)
            val updatedSpot = savedSpot.copy(
                latitude = bestRefinedLat,
                longitude = bestRefinedLng,
                altitude = bestRefinedAlt,
                accuracyMeters = bestRefinedAcc,
                address = address
            )
            repository.updateParkingSpot(updatedSpot)
            ParkingNotificationHelper.showCarParkedNotification(this, updatedSpot)
            try { com.example.wear.WearDataLayerBridge.getInstance(this).syncParkingSpot(updatedSpot) } catch (_: Throwable) {}
            try { com.example.geofence.GeofenceManager.armGeofenceAfterBluetoothSave(this, bestRefinedLat, bestRefinedLng) } catch (_: Throwable) {}
            Log.d(TAG, "Stage 2 refinement committed for spot $savedSpotId: $address (±${bestRefinedAcc}m)")
        }
    }

    private fun buildInitialSavingNotification(): Notification {
        val strings = getAppStrings()
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ParkingNotificationHelper.CHANNEL_PARKING_ALERTS)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(strings.appName)
            .setContentText(strings.recordingGps)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun safeCleanupAndStop() {
        try {
            BluetoothDisconnectReceiver.resetProcessingState()
        } catch (_: Throwable) {}
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Throwable) {}
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Throwable) {}
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            BluetoothDisconnectReceiver.resetProcessingState()
        } catch (_: Throwable) {}
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Throwable) {}
    }

    companion object {
        private const val TAG = "AutoParkService"
        const val ACTION_START_MONITORING = "com.example.autopark.ACTION_START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.autopark.ACTION_STOP_MONITORING"
        const val ACTION_HANDLE_DISCONNECT = "com.example.autopark.ACTION_HANDLE_DISCONNECT"
        const val ACTION_START_WATCHING = "com.example.autopark.ACTION_START_WATCHING"
        const val ACTION_STOP_WATCHING = "com.example.autopark.ACTION_STOP_WATCHING"

        private const val PREFS_NAME = "autopark_prefs"
        private const val KEY_AUTO_MONITOR_ENABLED = "pref_auto_monitor_enabled"

        @Volatile
        var isMonitoring: Boolean = false
            private set

        @Volatile
        var isWatching: Boolean = false
            private set

        @Volatile
        var watchedCarName: String? = null
            private set

        @Volatile
        var watchedCarAddress: String? = null
            private set

        fun hasRequiredPermissionsForMonitoring(context: Context): Boolean {
            return androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        fun isMonitoringEnabled(context: Context): Boolean {
            return try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.getBoolean(KEY_AUTO_MONITOR_ENABLED, true)
            } catch (_: Throwable) { true }
        }

        fun setMonitoringEnabled(context: Context, enabled: Boolean) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(KEY_AUTO_MONITOR_ENABLED, enabled).apply()
                if (enabled) {
                    startMonitoring(context)
                } else {
                    stopMonitoring(context)
                }
            } catch (_: Throwable) {}
        }

        fun startMonitoring(context: Context) {
            if (!isMonitoringEnabled(context)) return
            if (!hasRequiredPermissionsForMonitoring(context)) {
                Log.d(TAG, "Skipping startMonitoring: required location permissions not yet granted.")
                return
            }
            try {
                val intent = Intent(context, AutoParkLocationService::class.java).apply {
                    action = ACTION_START_MONITORING
                }
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to start AutoParkLocationService monitoring", e)
            }
        }

        fun stopMonitoring(context: Context) {
            try {
                val intent = Intent(context, AutoParkLocationService::class.java).apply {
                    action = ACTION_STOP_MONITORING
                }
                context.startService(intent)
            } catch (_: Throwable) {}
        }

        fun startWatching(context: Context, carName: String, carAddress: String?) {
            if (!hasRequiredPermissionsForMonitoring(context)) {
                Log.d(TAG, "Skipping startWatching: required location permissions not yet granted.")
                return
            }
            try {
                val intent = Intent(context, AutoParkLocationService::class.java).apply {
                    action = ACTION_START_WATCHING
                    putExtra("car_name", carName)
                    putExtra("car_address", carAddress)
                }
                if (isMonitoring) {
                    context.startService(intent)
                } else {
                    androidx.core.content.ContextCompat.startForegroundService(context, intent)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to start watching service in foreground", e)
            }
        }

        fun stopWatching(context: Context) {
            try {
                val intent = Intent(context, AutoParkLocationService::class.java).apply {
                    action = ACTION_STOP_WATCHING
                }
                context.startService(intent)
            } catch (_: Throwable) {}
        }

        fun startDisconnectSave(
            context: Context,
            rawAddress: String?,
            rawName: String?,
            isSimulated: Boolean = false,
            simLat: Double = 0.0,
            simLng: Double = 0.0,
            simAlt: Double = 0.0,
            simAcc: Float = 10f
        ): Boolean {
            return try {
                val intent = Intent(context, AutoParkLocationService::class.java).apply {
                    action = ACTION_HANDLE_DISCONNECT
                    putExtra("raw_address", rawAddress)
                    putExtra("raw_name", rawName)
                    putExtra("is_simulated", isSimulated)
                    putExtra("simulated_lat", simLat)
                    putExtra("simulated_lng", simLng)
                    putExtra("simulated_alt", simAlt)
                    putExtra("simulated_accuracy", simAcc)
                }
                if (isMonitoring || isWatching) {
                    context.startService(intent)
                } else {
                    androidx.core.content.ContextCompat.startForegroundService(context, intent)
                }
                true
            } catch (e: Throwable) {
                Log.w(TAG, "Could not start AutoParkLocationService in foreground: ${e.message}")
                false
            }
        }
    }
}
