package com.example.receiver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.entity.BluetoothCarDevice
import com.example.data.entity.ParkingSpot
import com.example.notification.ParkingNotificationHelper
import com.example.sensor.LocationHelper
import com.example.ui.i18n.getAppStrings
import com.example.util.BluetoothDeviceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class BluetoothDisconnectReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BluetoothReceiver"
        private const val DEBOUNCE_WINDOW_MS = 4_000L // Fast 4 seconds window for hardware disconnects

        private val processLock = Any()

        @Volatile
        private var isDisconnectProcessing: Boolean = false

        @Volatile
        private var disconnectProcessingStartTime: Long = 0L

        @Volatile
        private var lastDisconnectHandledTime: Long = 0L

        @Volatile
        private var lastSavedAddress: String? = null

        fun resetProcessingState() {
            synchronized(processLock) {
                isDisconnectProcessing = false
            }
        }

        private val VEHICLE_KEYWORDS = listOf(
            "car", "auto", "vehicle", "infotainment", "handsfree", "hands-free",
            "head unit", "sync", "uconnect", "carplay",
            "android auto", "toyota", "vw", "volkswagen", "bmw", "audi", "mercedes",
            "ford", "hyundai", "kia", "nissan", "honda", "tesla", "chevy", "chevrolet",
            "mazda", "subaru", "jeep", "dodge", "ram", "lexus", "volvo", "renault",
            "peugeot", "fiat", "skoda", "seat", "citroen", "opel", "mitsubishi",
            "land rover", "porsche", "alfa", "suzuki", "genesis", "multimedia", "mmi", "idrive"
        )

        fun hasVehicleKeywords(name: String?): Boolean {
            if (name.isNullOrBlank()) return false
            val lower = name.lowercase()
            return VEHICLE_KEYWORDS.any { lower.contains(it) }
        }

        fun isProfileConnectionEvent(action: String?, profileState: Int = -1, adapterState: Int = -1): Boolean {
            val isAclConnected = action == BluetoothDevice.ACTION_ACL_CONNECTED
            val isAdapterConnConnected = action == BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED &&
                    adapterState == BluetoothAdapter.STATE_CONNECTED
            val isA2dpConnected = (action == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED ||
                    action == "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED") &&
                    profileState == BluetoothProfile.STATE_CONNECTED
            val isHeadsetConnected = (action == BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED ||
                    action == "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED") &&
                    profileState == BluetoothProfile.STATE_CONNECTED
            return isAclConnected || isAdapterConnConnected || isA2dpConnected || isHeadsetConnected
        }

        fun isProfileDisconnectionEvent(action: String?, profileState: Int = -1, adapterState: Int = -1): Boolean {
            val isAclDisconnect = action == BluetoothDevice.ACTION_ACL_DISCONNECTED
            val isAdapterConnDisconnect = action == BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED &&
                    adapterState == BluetoothAdapter.STATE_DISCONNECTED
            val isA2dpDisconnect = (action == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED ||
                    action == "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED") &&
                    profileState == BluetoothProfile.STATE_DISCONNECTED
            val isHeadsetDisconnect = (action == BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED ||
                    action == "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED") &&
                    profileState == BluetoothProfile.STATE_DISCONNECTED
            val isBtTurningOff = action == BluetoothAdapter.ACTION_STATE_CHANGED &&
                    (adapterState == BluetoothAdapter.STATE_TURNING_OFF || adapterState == BluetoothAdapter.STATE_OFF)
            val isSimulatedDisconnect = action == "com.example.autopark.ACTION_SIMULATE_DISCONNECT"
            return isAclDisconnect || isAdapterConnDisconnect || isA2dpDisconnect ||
                    isHeadsetDisconnect || isBtTurningOff || isSimulatedDisconnect
        }
    }

    @SuppressLint("MissingPermission", "WakelockTimeout")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive action: $action")

        // 1. Handle system boot / update actions to warm up Application & services
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d(TAG, "Device booted / package replaced; app initialized.")
            com.example.geofence.GeofenceManager.restoreOnStartup(context)
            return
        }

        // 2. Safe device extraction compatible with Android 13+ (Tiramisu) and legacy versions
        val device: BluetoothDevice? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    ?: @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }
        } catch (_: Throwable) {
            null
        }

        val rawAddress: String? = intent.getStringExtra("simulated_address")
            ?: try { device?.address } catch (_: Throwable) { null }

        val rawName: String? = intent.getStringExtra("simulated_name")
            ?: intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
            ?: try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    device?.alias ?: device?.name
                } else {
                    device?.name
                }
            } catch (_: Throwable) {
                null
            }

        // Acquire a temporary partial WakeLock so Android Doze doesn't suspend the CPU
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = try {
            powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "PixelParking:BluetoothDisconnectWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(25_000L) // Safe cap: release after 25 seconds max
            }
        } catch (_: Throwable) {
            null
        }

        // 3. Classify event type
        val profileState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
        val adapterConnState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1)
        val adapterState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)

        val isConnectedEvent = isProfileConnectionEvent(action, profileState, adapterConnState)

        // 4. Handle connection events: refresh last-connected timestamp and register vehicle if new
        if (isConnectedEvent) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val repository = com.example.data.repository.ParkingRepository(
                        db.parkingSpotDao(),
                        db.bluetoothCarDeviceDao()
                    )

                    val connAddress = rawAddress
                    val connName = rawName

                    val matched = repository.findMatchingDevice(connAddress, connName)
                        ?: if (!connAddress.isNullOrBlank()) repository.getDeviceByAddress(connAddress) else null

                    if (matched != null) {
                        repository.updateBluetoothDevice(matched.copy(lastConnectedTimestamp = System.currentTimeMillis()))
                    } else if (!connAddress.isNullOrBlank()) {
                        val isBonded = try {
                            device?.bondState == BluetoothDevice.BOND_BONDED
                        } catch (_: Throwable) {
                            false
                        }
                        if (isBonded) {
                            val effectiveName = connName ?: "Bluetooth Device"
                            val isCar = hasVehicleKeywords(effectiveName) || repository.getMonitoredDevicesDirect().isEmpty()
                            repository.registerBluetoothDevice(
                                BluetoothCarDevice(
                                    address = connAddress,
                                    name = effectiveName,
                                    originalName = effectiveName,
                                    isMonitoredCar = isCar,
                                    lastConnectedTimestamp = System.currentTimeMillis()
                                ),
                                setAsPrimary = isCar
                            )
                        }
                    }

                    // Warm up GPS & fused location providers while driving so location is primed when parking
                    try {
                        LocationHelper.warmUpLocation(context)
                    } catch (_: Throwable) {}

                    // If a navigation was running or an active spot exists for this car, mark it as found automatically upon connecting
                    val activeSpot = repository.getActiveSpotDirect()
                    if (activeSpot != null && activeSpot.isActive) {
                        val isCarMatch = if (!connAddress.isNullOrBlank() && !activeSpot.bluetoothDeviceAddress.isNullOrBlank()) {
                            activeSpot.bluetoothDeviceAddress.equals(connAddress, ignoreCase = true)
                        } else if (!connName.isNullOrBlank() && !activeSpot.bluetoothDeviceName.isNullOrBlank()) {
                            activeSpot.bluetoothDeviceName.equals(connName, ignoreCase = true)
                        } else {
                            matched?.isMonitoredCar == true || repository.getMonitoredDevicesDirect().any { it.address.equals(connAddress, ignoreCase = true) }
                        }
                        if (isCarMatch) {
                            Log.d(TAG, "Car connected while active parking/navigation was present. Marking car as found.")
                            repository.deactivateActiveSpot()
                            try {
                                com.example.service.ParkingRadarService.stop(context)
                            } catch (_: Throwable) {}
                            try {
                                com.example.wear.WearDataLayerBridge.getInstance(context).syncParkingSpot(null)
                            } catch (_: Throwable) {}
                        }
                    }

                    val isCarOrMonitored = matched?.isMonitoredCar == true ||
                            (connName != null && hasVehicleKeywords(connName)) ||
                            repository.getMonitoredDevicesDirect().any { it.address.equals(connAddress, ignoreCase = true) } ||
                            repository.getMonitoredDevicesDirect().isEmpty()

                    if (isCarOrMonitored) {
                        val activeCarName = matched?.name ?: connName ?: "Car Bluetooth"
                        com.example.service.AutoParkLocationService.startWatching(context, activeCarName, connAddress)
                    }

                    if (matched?.isMonitoredCar == true) {
                        com.example.geofence.GeofenceManager.clearGpsBuffer(context)
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Error updating connected device", e)
                } finally {
                    try {
                        if (wakeLock?.isHeld == true) wakeLock.release()
                    } catch (_: Throwable) {}
                    pendingResult.finish()
                }
            }
            return
        }

        // 5. Classify disconnect triggers
        val isSimulatedDisconnect = action == "com.example.autopark.ACTION_SIMULATE_DISCONNECT"
        val isBtTurningOff = action == BluetoothAdapter.ACTION_STATE_CHANGED &&
                (adapterState == BluetoothAdapter.STATE_TURNING_OFF || adapterState == BluetoothAdapter.STATE_OFF)

        val isDisconnectEvent = isProfileDisconnectionEvent(
            action = action,
            profileState = profileState,
            adapterState = if (action == BluetoothAdapter.ACTION_STATE_CHANGED) adapterState else adapterConnState
        )

        if (!isDisconnectEvent) {
            safeReleaseWakeLock(wakeLock)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            var acquiredProcessingLock = false
            try {
                val db = AppDatabase.getDatabase(context)
                val repository = com.example.data.repository.ParkingRepository(
                    db.parkingSpotDao(),
                    db.bluetoothCarDeviceDao()
                )

                // 6. Check explicitly selected (monitored) car devices from the BT car menu
                var allMonitored = repository.getMonitoredDevicesDirect()
                if (allMonitored.isEmpty() && !isSimulatedDisconnect) {
                    val allDevices = repository.getAllDevicesDirect()
                    val candidateCar = allDevices.firstOrNull { dev ->
                        dev.deviceType == "Car" || hasVehicleKeywords(dev.name) || hasVehicleKeywords(dev.originalName)
                    } ?: if (!rawAddress.isNullOrBlank() || !rawName.isNullOrBlank()) {
                        repository.findMatchingDevice(rawAddress, rawName)?.takeIf {
                            it.deviceType == "Car" || hasVehicleKeywords(it.name) || hasVehicleKeywords(it.originalName)
                        }
                    } else null
                    ?: if (allDevices.size == 1) allDevices.first() else null

                    if (candidateCar != null) {
                        Log.d(TAG, "No vehicle was explicitly toggled, auto-monitoring '${candidateCar.name}' for disconnect auto-park.")
                        repository.setDeviceMonitored(candidateCar.address, true)
                        allMonitored = listOf(candidateCar.copy(isMonitoredCar = true))
                    } else if (hasVehicleKeywords(rawName)) {
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
                    } else {
                        val fallbackName = rawName ?: "Car Bluetooth"
                        val fallbackAddress = rawAddress ?: "00:00:00:00:00:00"
                        Log.d(TAG, "No car device explicitly selected, auto-registering '$fallbackName' ($fallbackAddress) for auto-park.")
                        val newCar = BluetoothCarDevice(
                            address = fallbackAddress,
                            name = fallbackName,
                            originalName = fallbackName,
                            deviceType = "Car",
                            isMonitoredCar = true,
                            lastConnectedTimestamp = System.currentTimeMillis()
                        )
                        repository.registerBluetoothDevice(newCar, setAsPrimary = true)
                        allMonitored = listOf(newCar)
                    }
                }

                val targetAddress: String? = rawAddress
                val targetName: String? = rawName

                // Resolve matching vehicle against monitored vehicles
                val resolvedVehicle: BluetoothCarDevice? = if (isSimulatedDisconnect) {
                    repository.findMatchingMonitoredDevice(targetAddress, targetName)
                        ?: if (!targetAddress.isNullOrBlank()) {
                            repository.getDeviceByAddress(targetAddress)?.takeIf { it.isMonitoredCar }
                        } else null
                        ?: allMonitored.firstOrNull()
                        ?: BluetoothCarDevice(
                            name = targetName ?: "Developer Test Car",
                            originalName = targetName ?: "Developer Test Car",
                            address = targetAddress ?: "AA:BB:CC:DD:EE:FF",
                            deviceType = "Car",
                            isMonitoredCar = true
                        )
                } else if (!targetAddress.isNullOrBlank() || !targetName.isNullOrBlank()) {
                    // Physical device disconnect: match monitored device, or fallback to watched car / sole monitored car
                    repository.findMatchingMonitoredDevice(targetAddress, targetName)
                        ?: if (com.example.service.AutoParkLocationService.isWatching &&
                            !com.example.service.AutoParkLocationService.watchedCarAddress.isNullOrBlank()
                        ) {
                            repository.getDeviceByAddress(com.example.service.AutoParkLocationService.watchedCarAddress!!)
                        } else null
                        ?: if (allMonitored.size == 1) {
                            allMonitored.first()
                        } else null
                } else if (allMonitored.size == 1) {
                    allMonitored.first()
                } else {
                    // Physical disconnect with null/empty extras (common on Android 12+ ACL broadcasts)
                    if (com.example.service.AutoParkLocationService.isWatching &&
                        !com.example.service.AutoParkLocationService.watchedCarAddress.isNullOrBlank()
                    ) {
                        repository.getDeviceByAddress(com.example.service.AutoParkLocationService.watchedCarAddress!!)
                    } else {
                        allMonitored.maxByOrNull { it.lastConnectedTimestamp }
                            ?: allMonitored.firstOrNull()
                    }
                }

                // If not resolved, fallback to watched car, sole monitored car, or most recently connected monitored car
                val effectiveVehicle = resolvedVehicle
                    ?: if (com.example.service.AutoParkLocationService.isWatching &&
                        !com.example.service.AutoParkLocationService.watchedCarAddress.isNullOrBlank()
                    ) {
                        repository.getDeviceByAddress(com.example.service.AutoParkLocationService.watchedCarAddress!!)
                    } else null
                    ?: if (allMonitored.size == 1) allMonitored.first() else null
                    ?: allMonitored.maxByOrNull { it.lastConnectedTimestamp }
                    ?: allMonitored.firstOrNull()

                if (!isSimulatedDisconnect && effectiveVehicle == null) {
                    Log.d(
                        TAG,
                        "Ignored disconnect: device ($targetAddress, $targetName) is not a selected car in the BT car menu."
                    )
                    return@launch
                }

                val targetVehicle = effectiveVehicle ?: BluetoothCarDevice(
                    name = targetName ?: "Developer Test Car",
                    originalName = targetName ?: "Developer Test Car",
                    address = targetAddress ?: "AA:BB:CC:DD:EE:FF",
                    deviceType = "Car",
                    isMonitoredCar = true
                )

                // Strict process synchronization: drops concurrent or rapid duplicate broadcasts for this car
                val entryTime = System.currentTimeMillis()
                val shouldProceed = synchronized(processLock) {
                    val now = System.currentTimeMillis()
                    if (isDisconnectProcessing && (now - disconnectProcessingStartTime) > 30_000L) {
                        isDisconnectProcessing = false
                    }
                    val debounce = if (isSimulatedDisconnect) 3_000L else DEBOUNCE_WINDOW_MS
                    if (isDisconnectProcessing) {
                        Log.d(TAG, "Dropping disconnect broadcast: another disconnect event is currently being processed")
                        false
                    } else if ((entryTime - lastDisconnectHandledTime) < debounce) {
                        Log.d(TAG, "Dropping duplicate disconnect broadcast received within debounce window ($debounce ms)")
                        false
                    } else {
                        isDisconnectProcessing = true
                        disconnectProcessingStartTime = entryTime
                        lastDisconnectHandledTime = entryTime
                        true
                    }
                }

                if (!shouldProceed) {
                    return@launch
                }
                acquiredProcessingLock = true

                // 7. Handle Disconnection Event directly inside goAsync() with WakeLock:
                // Reliable execution that works even when the app is completely closed or killed.
                withTimeoutOrNull(25_000L) {
                    val activeSpot = repository.getActiveSpotDirect()
                    val mostRecentSpot = repository.getMostRecentSpotDirect()
                    val referenceSpot = activeSpot ?: mostRecentSpot

                    // Prioritize user's custom renamed vehicle name
                    val effectiveDeviceName = targetVehicle.name.ifBlank { null }
                        ?: targetVehicle.originalName.ifBlank { null }
                        ?: targetName?.ifBlank { null }
                        ?: "Car Bluetooth"

                    val effectiveDeviceAddress = targetVehicle.address.ifBlank { null }
                        ?: targetAddress?.ifBlank { null }
                        ?: "00:00:00:00:00:00"

                    // Support simulated coordinates from debug test actions if provided
                    val simLat = intent.getDoubleExtra("simulated_lat", 0.0)
                    val simLng = intent.getDoubleExtra("simulated_lng", 0.0)
                    val simAlt = intent.getDoubleExtra("simulated_alt", 0.0)
                    val simAcc = intent.getFloatExtra("simulated_accuracy", 10f)

                    val location: android.location.Location? = if (simLat != 0.0 || simLng != 0.0) {
                        android.location.Location("simulation").apply {
                            latitude = simLat
                            longitude = simLng
                            altitude = simAlt
                            accuracy = simAcc
                        }
                    } else {
                        // Rapid pinpoint live fix acquisition (1.0-2.4s) prioritizing fresh satellite/fused coordinates
                        LocationHelper.getLocationForAutoPark(
                            context,
                            oldSpotLat = referenceSpot?.latitude,
                            oldSpotLng = referenceSpot?.longitude,
                            oldSpotTimestamp = referenceSpot?.timestamp ?: 0L,
                            triggerTime = entryTime
                        ) ?: LocationHelper.getBestAvailableInstantFix(context)
                    }

                    var lat = 0.0
                    var lng = 0.0
                    var alt = 0.0
                    var accuracy = 0f

                    if (location != null && (location.latitude != 0.0 || location.longitude != 0.0)) {
                        lat = location.latitude
                        lng = location.longitude
                        alt = location.altitude
                        accuracy = location.accuracy
                    } else {
                        Log.w(TAG, "No valid fresh location fix available for auto-park. Aborting auto-save to avoid saving previous/incorrect location.")
                        return@withTimeoutOrNull
                    }

                    // Underground Parking Safeguard Check:
                    // Only backtrack to entrance fix if:
                    // 1) GPS accuracy is genuinely degraded (> 80m or satellite lost inside concrete garage)
                    // 2) There is a reliable entrance fix from the CURRENT drive
                    // 3) The entrance fix is physically near the current location (< 350m)!
                    val isDegraded = accuracy > 80.0f
                    val reliableEntranceFix = if (isDegraded) {
                        val fix = com.example.geofence.GeofenceManager.getLastReliableFix(context)
                        if (fix != null && (referenceSpot == null || fix.timestamp > referenceSpot.timestamp)) {
                            val dist = FloatArray(1)
                            Location.distanceBetween(lat, lng, fix.latitude, fix.longitude, dist)
                            if (dist[0] <= 350f) fix else null
                        } else null
                    } else null
                    val isUndergroundFallback = isDegraded && reliableEntranceFix != null
                    val origAcc = accuracy

                    if (isUndergroundFallback && reliableEntranceFix != null) {
                        lat = reliableEntranceFix.latitude
                        lng = reliableEntranceFix.longitude
                        alt = reliableEntranceFix.altitude
                        accuracy = reliableEntranceFix.accuracy
                        Log.w(TAG, "Bluetooth Disconnect: Underground safeguard active. Degraded accuracy (${origAcc}m). Falling back to entrance fix: ($lat, $lng, ±${accuracy}m)")
                    }

                    val strings = context.getAppStrings()

                    // Resolve street name when on a street, or clean coordinates in remote locations
                    val resolvedAddress = withTimeoutOrNull(2200L) {
                        LocationHelper.getStreetNameOrCoordinates(context, lat, lng)
                    } ?: LocationHelper.formatCoordinates(lat, lng)

                    val spotNote = if (isUndergroundFallback) {
                        String.format(strings.undergroundFallbackNote, origAcc.toInt(), accuracy.toInt())
                    } else {
                        strings.bluetoothDisconnectNote
                    }
                    val spotFloor = if (isUndergroundFallback) strings.undergroundFloorLevel else strings.floorGroundLevel

                    val spot = ParkingSpot(
                        latitude = lat,
                        longitude = lng,
                        altitude = alt,
                        accuracyMeters = accuracy,
                        address = resolvedAddress,
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

                    // Stop any ongoing radar service or previous navigation
                    try {
                        com.example.service.ParkingRadarService.stop(context)
                    } catch (_: Throwable) {}

                    // Commit to Room database immediately so spot is 100% secured in just a few seconds
                    val newId = repository.saveNewParkingSpot(spot)
                    val savedSpot = spot.copy(id = newId)

                    // Clear the rolling GPS buffer now that car is parked so old entrance fixes do not pollute next trip
                    com.example.geofence.GeofenceManager.clearGpsBuffer(context)

                    // Arm geofence tracking around the Bluetooth-saved parking location
                    com.example.geofence.GeofenceManager.armGeofenceAfterBluetoothSave(context, lat, lng)

                    // If underground safeguard was triggered, inform the user
                    if (isUndergroundFallback) {
                        try {
                            val notifTitle = strings.undergroundSafeguardNotificationTitle
                            val notifMsg = String.format(strings.undergroundSafeguardNotificationText, accuracy.toInt())
                            ParkingNotificationHelper.showSystemNotification(context, notifTitle, notifMsg)
                        } catch (_: Throwable) {}
                    }

                    // Immediately sync active parking spot to Wear OS companion
                    try {
                        com.example.wear.WearDataLayerBridge.getInstance(context).syncParkingSpot(savedSpot)
                    } catch (_: Throwable) {}

                    // Emit exactly ONE clean notification immediately showing street name (or coordinates if remote)
                    ParkingNotificationHelper.showCarParkedNotification(context, savedSpot)
                    Log.d(TAG, "Successfully auto-parked vehicle '$effectiveDeviceName' ($effectiveDeviceAddress) with ID: $newId at $resolvedAddress (±${accuracy}m)")

                    // If initial save fell back to coordinates due to network delay, resolve in background and update notification
                    if (LocationHelper.isCoordinatesString(resolvedAddress)) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val asyncAddress = withTimeoutOrNull(6000L) {
                                    LocationHelper.getStreetNameOrCoordinates(context, lat, lng)
                                }
                                if (!asyncAddress.isNullOrBlank() && !LocationHelper.isCoordinatesString(asyncAddress) && asyncAddress != savedSpot.address) {
                                    val updatedSpot = savedSpot.copy(address = asyncAddress)
                                    repository.updateParkingSpot(updatedSpot)
                                    com.example.wear.WearDataLayerBridge.getInstance(context).syncParkingSpot(updatedSpot)
                                    ParkingNotificationHelper.showCarParkedNotification(context, updatedSpot)
                                    Log.d(TAG, "Updated database spot and notification with resolved street address: $asyncAddress")
                                }
                            } catch (e: Throwable) {
                                Log.d(TAG, "Background geocoding completed: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error during auto-park bluetooth disconnect handling", e)
            } finally {
                try {
                    com.example.service.AutoParkLocationService.stopWatching(context)
                } catch (_: Throwable) {}
                if (acquiredProcessingLock) {
                    resetProcessingState()
                }
                safeReleaseWakeLock(wakeLock)
                pendingResult.finish()
            }
        }
    }

    private fun safeReleaseWakeLock(wakeLock: PowerManager.WakeLock?) {
        try {
            if (wakeLock?.isHeld == true) wakeLock.release()
        } catch (_: Throwable) {}
    }
}
