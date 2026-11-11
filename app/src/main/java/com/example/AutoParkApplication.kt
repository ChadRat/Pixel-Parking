package com.example

import android.app.Application
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.database.AppDatabase
import com.example.data.repository.ParkingRepository
import com.example.notification.ParkingNotificationHelper
import com.example.receiver.BluetoothDisconnectReceiver
import com.example.util.BluetoothDeviceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoParkApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: ParkingRepository
        private set

    private val bluetoothReceiver = BluetoothDisconnectReceiver()

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getDatabase(this)
        repository = ParkingRepository(
            database.parkingSpotDao(),
            database.bluetoothCarDeviceDao()
        )

        // Initialize Notification Channels
        ParkingNotificationHelper.createNotificationChannels(this)

        // Initialize persistent background monitoring service if enabled
        if (com.example.service.AutoParkLocationService.isMonitoringEnabled(this)) {
            com.example.service.AutoParkLocationService.startMonitoring(this)
        }

        // Dynamically register BluetoothDisconnectReceiver for real-time background delivery
        registerDynamicBluetoothReceiver()

        // Restore Activity Recognition, step sensors, and armed perimeter geofences on startup
        com.example.geofence.GeofenceManager.restoreOnStartup(this)

        // Seed initial preview data, warm location cache, and sync paired devices
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.seedInitialDataIfEmpty()
                syncPairedVehiclesOnStartup()
                try {
                    com.example.sensor.LocationHelper.getCurrentLocation(this@AutoParkApplication)
                } catch (_: Throwable) {}
            } catch (e: Throwable) {
                Log.e("AutoParkApp", "Startup initialization error", e)
            }
        }
    }

    private fun registerDynamicBluetoothReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
                addAction("com.example.autopark.ACTION_SIMULATE_DISCONNECT")
                addAction("com.example.autopark.ACTION_NAVIGATE_CAR")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.registerReceiver(
                    this,
                    bluetoothReceiver,
                    filter,
                    ContextCompat.RECEIVER_EXPORTED
                )
            } else {
                registerReceiver(bluetoothReceiver, filter)
            }
            Log.d("AutoParkApp", "BluetoothDisconnectReceiver dynamically registered successfully")
        } catch (e: Throwable) {
            Log.e("AutoParkApp", "Failed to register Bluetooth receiver dynamically", e)
        }
    }

    private suspend fun syncPairedVehiclesOnStartup() {
        try {
            val systemBonded = BluetoothDeviceHelper.getSystemBondedDevices(this)
            if (!systemBonded.isNullOrEmpty()) {
                val existing = repository.getAllDevicesDirect()
                val existingMap = existing.associateBy { it.address.uppercase() }
                systemBonded.forEach { dev ->
                    val found = existingMap[dev.address.uppercase()]
                    val isCar = BluetoothDeviceHelper.isCarDevice(dev.name, dev.deviceType) ||
                            (dev.originalName.isNotBlank() && BluetoothDeviceHelper.isCarDevice(dev.originalName, dev.deviceType))
                    val isNonCar = BluetoothDeviceHelper.isNonCarDevice(dev.name) ||
                            (dev.originalName.isNotBlank() && BluetoothDeviceHelper.isNonCarDevice(dev.originalName))

                    if (found == null) {
                        repository.registerBluetoothDevice(
                            dev.copy(
                                originalName = dev.name,
                                isMonitoredCar = isCar
                            ),
                            setAsPrimary = isCar
                        )
                    } else if (isCar && !found.isMonitoredCar) {
                        repository.updateBluetoothDevice(found.copy(isMonitoredCar = true, deviceType = "Car Audio / Infotainment"))
                    }
                }
            }

            // If phone is already connected to car Bluetooth on launch, start silent background watcher immediately
            val monitored = repository.getMonitoredDevicesDirect()
            val connectedCar = monitored.firstOrNull { dev ->
                BluetoothDeviceHelper.isDeviceConnected(this@AutoParkApplication, dev.address)
            } ?: if (monitored.isEmpty()) {
                repository.getAllDevicesDirect().firstOrNull { dev ->
                    (BluetoothDeviceHelper.isCarDevice(dev.name, dev.deviceType) ||
                     (dev.originalName.isNotBlank() && BluetoothDeviceHelper.isCarDevice(dev.originalName, dev.deviceType))) &&
                    BluetoothDeviceHelper.isDeviceConnected(this@AutoParkApplication, dev.address)
                }
            } else null

            if (connectedCar != null && !com.example.service.AutoParkLocationService.isWatching) {
                Log.d("AutoParkApp", "Vehicle '${connectedCar.name}' already connected on startup; starting watcher service.")
                com.example.service.AutoParkLocationService.startWatching(
                    this@AutoParkApplication,
                    connectedCar.name,
                    connectedCar.address
                )
            }
        } catch (e: Throwable) {
            Log.e("AutoParkApp", "Error syncing paired vehicles on startup", e)
        }
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN || level in TRIM_MEMORY_RUNNING_MODERATE..TRIM_MEMORY_RUNNING_CRITICAL) {
            com.example.ui.screens.MapTileEngine.clearMemoryCache()
            com.example.ui.screens.HighZoomVectorEngine.trimMemory(keepAll = false)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        com.example.ui.screens.MapTileEngine.clearMemoryCache()
        com.example.ui.screens.HighZoomVectorEngine.trimMemory(keepAll = false)
    }
}
