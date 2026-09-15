package com.example.receiver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.entity.ParkingSpot
import com.example.notification.ParkingNotificationHelper
import com.example.sensor.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BluetoothDisconnectReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("BluetoothReceiver", "Received action: $action")

        val isBluetoothDisconnect = action == BluetoothDevice.ACTION_ACL_DISCONNECTED
        val isSimulatedDisconnect = action == "com.example.autopark.ACTION_SIMULATE_DISCONNECT"

        if (isBluetoothDisconnect || isSimulatedDisconnect) {
            val pendingResult = goAsync()

            val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            val deviceName = intent.getStringExtra("simulated_name") 
                ?: try { device?.name } catch (e: SecurityException) { null } 
                ?: "Car Audio System"
            val deviceAddress = intent.getStringExtra("simulated_address") 
                ?: device?.address 
                ?: "00:11:22:33:44:55"

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val repository = com.example.data.repository.ParkingRepository(
                        db.parkingSpotDao(),
                        db.bluetoothCarDeviceDao()
                    )

                    // Strictly check if this device is the one specific selected car device
                    val isMonitored = repository.isDeviceMonitored(deviceAddress)
                    if (!isMonitored) {
                        Log.d("BluetoothReceiver", "Ignored disconnect from unselected device: $deviceAddress ($deviceName)")
                        return@launch
                    }

                    // Grab exact current GPS location
                    val location = LocationHelper.getCurrentLocation(context)
                    if (location == null) {
                        Log.w("BluetoothReceiver", "Cannot determine actual GPS location on disconnect; aborting auto-save to prevent fake coordinates")
                        return@launch
                    }

                    val lat = location.latitude
                    val lng = location.longitude
                    val alt = location.altitude
                    val accuracy = location.accuracy

                    val address = LocationHelper.getAddressFromCoordinates(context, lat, lng)

                    val spot = ParkingSpot(
                        latitude = lat,
                        longitude = lng,
                        altitude = alt,
                        accuracyMeters = accuracy,
                        address = address,
                        spotName = "$deviceName Parking Spot",
                        floorLevel = "Ground Level",
                        note = "Automatically saved upon Bluetooth disconnection.",
                        timestamp = System.currentTimeMillis(),
                        isAutoSaved = true,
                        bluetoothDeviceName = deviceName,
                        bluetoothDeviceAddress = deviceAddress,
                        meterExpiryTimestamp = null,
                        isActive = true
                    )

                    val newId = repository.saveNewParkingSpot(spot)
                    val savedSpot = spot.copy(id = newId)

                    // Show rich notification with instant navigation actions
                    ParkingNotificationHelper.showCarParkedNotification(context, savedSpot)
                    Log.d("BluetoothReceiver", "Saved auto-park spot at $lat, $lng with id: $newId")
                } catch (e: Exception) {
                    Log.e("BluetoothReceiver", "Error saving auto-park spot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
