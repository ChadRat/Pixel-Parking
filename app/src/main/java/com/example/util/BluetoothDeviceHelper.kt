package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.entity.BluetoothCarDevice

object BluetoothDeviceHelper {

    /**
     * Safely queries the system's paired (bonded) Bluetooth devices.
     * If permission is missing or on emulator without paired devices, returns null.
     */
    @SuppressLint("MissingPermission")
    fun getSystemBondedDevices(context: Context): List<BluetoothCarDevice>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return null
            }
        }

        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            val bonded = adapter?.bondedDevices

            bonded?.map { device ->
                val type = inferDeviceType(device)
                val devName = device.name ?: "Unknown Device (${device.address.takeLast(5)})"
                BluetoothCarDevice(
                    address = device.address,
                    name = devName,
                    originalName = devName,
                    isMonitoredCar = false,
                    deviceType = type
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if a specific Bluetooth device is currently connected to the phone.
     */
    @SuppressLint("MissingPermission")
    fun isDeviceConnected(context: Context, address: String): Boolean {
        // Custom created virtual paired devices or testing fallback
        if (address.startsWith("AA:BB:CC")) return true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }

        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter() ?: return false

            if (!adapter.isEnabled) return false

            val profiles = intArrayOf(
                BluetoothProfile.A2DP,
                BluetoothProfile.HEADSET,
                BluetoothProfile.GATT,
                BluetoothProfile.GATT_SERVER
            )

            for (profile in profiles) {
                try {
                    val connected = bluetoothManager?.getConnectedDevices(profile) ?: emptyList()
                    if (connected.any { it.address.equals(address, ignoreCase = true) }) {
                        return true
                    }
                } catch (_: Exception) {}
            }

            // Check via reflection for device.isConnected() method
            try {
                val device = adapter.getRemoteDevice(address)
                val isConnectedMethod = device.javaClass.getMethod("isConnected")
                val isConnected = isConnectedMethod.invoke(device) as? Boolean
                if (isConnected == true) return true
            } catch (_: Exception) {}

            false
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun inferDeviceType(device: BluetoothDevice): String {
        return try {
            val deviceClass = device.bluetoothClass?.deviceClass ?: 0
            val majorClass = device.bluetoothClass?.majorDeviceClass ?: 0
            val name = (device.name ?: "").lowercase()

            when {
                name.contains("car") || name.contains("auto") || name.contains("handsfree") ||
                        name.contains("hands-free") || name.contains("infotainment") || name.contains("vehicle") ||
                        deviceClass == 1032 || deviceClass == 1056 -> "Car Audio / Infotainment"
                majorClass == 1024 -> "Audio / Headphones"
                majorClass == 512 -> "Phone"
                majorClass == 256 -> "Computer"
                majorClass == 1792 -> "Wearable / Watch"
                else -> "Bluetooth Device"
            }
        } catch (e: Exception) {
            "Bluetooth Device"
        }
    }
}

