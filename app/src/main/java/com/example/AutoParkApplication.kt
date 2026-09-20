package com.example

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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

        // Dynamically register BluetoothDisconnectReceiver for real-time background delivery
        registerDynamicBluetoothReceiver()

        // Seed initial preview data and sync paired devices
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.seedInitialDataIfEmpty()
                syncPairedVehiclesOnStartup()
            } catch (e: Throwable) {
                Log.e("AutoParkApp", "Startup initialization error", e)
            }
        }
    }

    private fun registerDynamicBluetoothReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
                addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")
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
                val hasAnyMonitored = existing.any { it.isMonitoredCar }

                systemBonded.forEach { dev ->
                    val found = existingMap[dev.address.uppercase()]
                    if (found == null) {
                        repository.registerBluetoothDevice(
                            dev.copy(
                                originalName = dev.name,
                                isMonitoredCar = false
                            ),
                            setAsPrimary = false
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("AutoParkApp", "Error syncing paired vehicles on startup", e)
        }
    }
}
