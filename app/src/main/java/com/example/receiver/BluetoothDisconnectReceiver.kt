package com.example.receiver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
        private const val DEBOUNCE_WINDOW_MS = 25_000L // 25 seconds window for hardware disconnects

        private val processLock = Any()

        @Volatile
        private var isDisconnectProcessing: Boolean = false

        @Volatile
        private var lastDisconnectHandledTime: Long = 0L

        @Volatile
        private var lastSavedAddress: String? = null

        private val VEHICLE_KEYWORDS = listOf(
            "car", "auto", "vehicle", "infotainment", "handsfree", "hands-free",
            "head unit", "stereo", "audio", "bt", "sync", "uconnect", "carplay",
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
    }

    @SuppressLint("MissingPermission", "WakelockTimeout")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive action: $action")

        // 1. Handle system boot / update actions to warm up Application & services
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d(TAG, "Device booted / package replaced; app initialized.")
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
        val isAclConnected = action == BluetoothDevice.ACTION_ACL_CONNECTED
        val isAdapterConnConnected = action == BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED &&
                intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1) == BluetoothAdapter.STATE_CONNECTED
        val isA2dpConnected = action == "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED" &&
                intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1) == BluetoothProfile.STATE_CONNECTED
        val isHeadsetConnected = action == "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED" &&
                intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1) == BluetoothProfile.STATE_CONNECTED

        val isConnectedEvent = isAclConnected || isAdapterConnConnected || isA2dpConnected || isHeadsetConnected

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
                        val effectiveName = connName ?: "Bluetooth Device"
                        repository.registerBluetoothDevice(
                            BluetoothCarDevice(
                                address = connAddress,
                                name = effectiveName,
                                originalName = effectiveName,
                                isMonitoredCar = false,
                                lastConnectedTimestamp = System.currentTimeMillis()
                            ),
                            setAsPrimary = false
                        )
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
        val isAclDisconnect = action == BluetoothDevice.ACTION_ACL_DISCONNECTED ||
                action == BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED
        val isAdapterConnDisconnect = action == BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED &&
                intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1) == BluetoothAdapter.STATE_DISCONNECTED
        val isA2dpDisconnect = action == "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED" &&
                intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1) == BluetoothProfile.STATE_DISCONNECTED
        val isHeadsetDisconnect = action == "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED" &&
                intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1) == BluetoothProfile.STATE_DISCONNECTED
        val isBtTurningOff = action == BluetoothAdapter.ACTION_STATE_CHANGED &&
                (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_TURNING_OFF ||
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF)
        val isSimulatedDisconnect = action == "com.example.autopark.ACTION_SIMULATE_DISCONNECT"

        val isDisconnectEvent = isAclDisconnect || isAdapterConnDisconnect || isA2dpDisconnect ||
                isHeadsetDisconnect || isBtTurningOff || isSimulatedDisconnect

        if (!isDisconnectEvent) {
            safeReleaseWakeLock(wakeLock)
            return
        }

        // Strict process synchronization: drops all concurrent or rapid duplicate broadcasts
        // (e.g. simultaneous ACL + A2DP profiles, dual manifest/dynamic receiver registrations).
        val entryTime = System.currentTimeMillis()
        synchronized(processLock) {
            if (!isSimulatedDisconnect) {
                if (isDisconnectProcessing) {
                    Log.d(TAG, "Dropping disconnect broadcast: another disconnect event is currently being processed")
                    safeReleaseWakeLock(wakeLock)
                    return
                }
                if ((entryTime - lastDisconnectHandledTime) < DEBOUNCE_WINDOW_MS) {
                    Log.d(TAG, "Dropping duplicate disconnect broadcast received within debounce window ($DEBOUNCE_WINDOW_MS ms)")
                    safeReleaseWakeLock(wakeLock)
                    return
                }
                isDisconnectProcessing = true
                lastDisconnectHandledTime = entryTime
            } else {
                lastDisconnectHandledTime = entryTime
            }
        }

        // 6. Handle Disconnection Event
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(25_000L) {
                    val db = AppDatabase.getDatabase(context)
                    val repository = com.example.data.repository.ParkingRepository(
                        db.parkingSpotDao(),
                        db.bluetoothCarDeviceDao()
                    )

                    // Auto-sync bonded devices from system if DB is empty or missing this address
                    val allDevicesDirect = repository.getAllDevicesDirect()
                    if (allDevicesDirect.isEmpty() || (!rawAddress.isNullOrBlank() && allDevicesDirect.none { it.address.equals(rawAddress, ignoreCase = true) })) {
                        try {
                            val systemBonded = BluetoothDeviceHelper.getSystemBondedDevices(context)
                            if (!systemBonded.isNullOrEmpty()) {
                                systemBonded.forEach { sysDev ->
                                    if (allDevicesDirect.none { it.address.equals(sysDev.address, ignoreCase = true) }) {
                                        repository.registerBluetoothDevice(
                                            sysDev.copy(isMonitoredCar = false),
                                            setAsPrimary = false
                                        )
                                    }
                                }
                            }
                        } catch (_: Throwable) {}
                    }

                    val allMonitored = repository.getMonitoredDevicesDirect()

                    // If no vehicles are enabled in the Car Bluetooth menu, never save location
                    if (!isSimulatedDisconnect && allMonitored.isEmpty()) {
                        Log.d(TAG, "No vehicles are enabled in the Car Bluetooth menu. Ignoring disconnect: address=$rawAddress, name=$rawName")
                        synchronized(processLock) {
                            lastDisconnectHandledTime = 0L
                        }
                        return@withTimeoutOrNull
                    }

                    var targetAddress: String? = rawAddress
                    var targetName: String? = rawName

                    val resolvedVehicle: BluetoothCarDevice? = if (isSimulatedDisconnect) {
                        repository.findMatchingMonitoredDevice(targetAddress, targetName)
                            ?: repository.findMatchingDevice(targetAddress, targetName)
                            ?: allMonitored.firstOrNull()
                    } else if (targetAddress.isNullOrBlank() && isBtTurningOff) {
                        // Bluetooth was turned off globally without a specific device extra.
                        // Only trigger if an enabled car was connected within the last 15 seconds.
                        val now = System.currentTimeMillis()
                        val recentlyConnected = allMonitored.filter { (now - it.lastConnectedTimestamp) in 0..15_000L }
                        if (recentlyConnected.isNotEmpty()) {
                            recentlyConnected.maxByOrNull { it.lastConnectedTimestamp }
                        } else {
                            Log.d(TAG, "Bluetooth turned off, but no enabled car was recently connected. Ignoring.")
                            synchronized(processLock) {
                                lastDisconnectHandledTime = 0L
                            }
                            return@withTimeoutOrNull
                        }
                    } else {
                        // Strict filter: match ONLY against enabled devices from the Car Bluetooth menu
                        repository.findMatchingMonitoredDevice(targetAddress, targetName)
                    }

                    // STRICT FILTER: If the disconnecting device is NOT on the enabled device list of the Car Bluetooth menu, IGNORE IT and DO NOT SAVE LOCATION
                    if (resolvedVehicle == null) {
                        Log.d(
                            TAG,
                            "Ignored disconnect from unmonitored Bluetooth device: address=$rawAddress, name=$rawName. Not on the enabled device list of the Car Bluetooth menu."
                        )
                        synchronized(processLock) {
                            lastDisconnectHandledTime = 0L
                        }
                        return@withTimeoutOrNull
                    }

                    // Prioritize user's custom renamed vehicle name
                    val effectiveDeviceName = resolvedVehicle.name.ifBlank { null }
                        ?: resolvedVehicle.originalName.ifBlank { null }
                        ?: targetName?.ifBlank { null }
                        ?: "Car Bluetooth"

                    val effectiveDeviceAddress = resolvedVehicle.address.ifBlank { null }
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
                        LocationHelper.getLocationForAutoPark(context)
                    }

                    val lat: Double
                    val lng: Double
                    val alt: Double
                    val accuracy: Float

                    if (location != null && (location.latitude != 0.0 || location.longitude != 0.0)) {
                        lat = location.latitude
                        lng = location.longitude
                        alt = location.altitude
                        accuracy = location.accuracy
                    } else {
                        // Fall back to active spot or most recent spot
                        val previousSpot = repository.getActiveSpotDirect()
                            ?: repository.getMostRecentSpotDirect()
                        if (previousSpot != null && (previousSpot.latitude != 0.0 || previousSpot.longitude != 0.0)) {
                            lat = previousSpot.latitude
                            lng = previousSpot.longitude
                            alt = previousSpot.altitude
                            accuracy = 25f
                        } else {
                            // Try system location manager one last time
                            val sysLoc = LocationHelper.getCurrentLocation(context)
                            if (sysLoc != null && (sysLoc.latitude != 0.0 || sysLoc.longitude != 0.0)) {
                                lat = sysLoc.latitude
                                lng = sysLoc.longitude
                                alt = sysLoc.altitude
                                accuracy = sysLoc.accuracy
                            } else {
                                Log.w(TAG, "No valid location fix available for auto-park. Aborting auto-save to avoid incorrect location.")
                                return@withTimeoutOrNull
                            }
                        }
                    }

                    val strings = context.getAppStrings()

                    // Resolve address up front (up to 3.5s) so the single notification displays the exact street address immediately
                    var streetAddress: String? = null
                    try {
                        streetAddress = withTimeoutOrNull(3500L) {
                            LocationHelper.getAddressFromCoordinates(context, lat, lng)
                        }
                    } catch (_: Throwable) {}

                    val displayAddress = if (!streetAddress.isNullOrBlank()) {
                        streetAddress
                    } else {
                        LocationHelper.formatCoordinates(lat, lng)
                    }

                    val spot = ParkingSpot(
                        latitude = lat,
                        longitude = lng,
                        altitude = alt,
                        accuracyMeters = accuracy,
                        address = displayAddress,
                        spotName = String.format(strings.autoSavedSpotDefaultName, effectiveDeviceName),
                        floorLevel = strings.floorGroundLevel,
                        note = strings.bluetoothDisconnectNote,
                        timestamp = System.currentTimeMillis(),
                        isAutoSaved = true,
                        bluetoothDeviceName = effectiveDeviceName,
                        bluetoothDeviceAddress = effectiveDeviceAddress,
                        meterExpiryTimestamp = null,
                        isActive = true
                    )

                    // Commit to Room database so spot is 100% secured
                    val newId = repository.saveNewParkingSpot(spot)
                    val savedSpot = spot.copy(id = newId)

                    // Emit exactly ONE clean notification
                    ParkingNotificationHelper.showCarParkedNotification(context, savedSpot)
                    Log.d(TAG, "Successfully auto-parked vehicle '$effectiveDeviceName' ($effectiveDeviceAddress) with ID: $newId at $displayAddress")

                    // If address was formatted coordinates because network geocoding was slow, asynchronously resolve in background and update the database ONLY (do NOT send another notification!)
                    if (streetAddress.isNullOrBlank()) {
                        try {
                            val asyncAddress = withTimeoutOrNull(4000L) {
                                LocationHelper.getAddressFromCoordinates(context, lat, lng)
                            }
                            if (!asyncAddress.isNullOrBlank()) {
                                val updatedSpot = savedSpot.copy(address = asyncAddress)
                                repository.updateParkingSpot(updatedSpot)
                                Log.d(TAG, "Updated database spot with reverse-geocoded address: $asyncAddress")
                            }
                        } catch (e: Throwable) {
                            Log.d(TAG, "Background geocoding completed: ${e.message}")
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error during auto-park bluetooth disconnect handling", e)
            } finally {
                synchronized(processLock) {
                    isDisconnectProcessing = false
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
