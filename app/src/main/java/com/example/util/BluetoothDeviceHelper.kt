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
                val devName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try { device.alias?.ifBlank { null } } catch (_: Throwable) { null } ?: device.name ?: "Unknown Device (${device.address.takeLast(5)})"
                } else {
                    device.name ?: "Unknown Device (${device.address.takeLast(5)})"
                }
                val isCar = isCarDevice(name = devName, deviceType = type, bluetoothDevice = device)
                BluetoothCarDevice(
                    address = device.address,
                    name = devName,
                    originalName = devName,
                    isMonitoredCar = isCar,
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
        // Custom created virtual paired devices or blank address should not be considered connected hardware
        if (address.isBlank() || address.startsWith("AA:BB:CC")) return false

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

    /**
     * Checks if any monitored car Bluetooth device is currently connected to the phone.
     * Prevents false triggers while stopped at red lights or idling in traffic.
     */
    fun isAnyCarBluetoothConnected(context: Context): Boolean {
        return try {
            if (com.example.service.AutoParkLocationService.isWatching) {
                val watchedAddr = com.example.service.AutoParkLocationService.watchedCarAddress
                if (!watchedAddr.isNullOrBlank() && !watchedAddr.startsWith("AA:BB:CC") && isDeviceConnected(context, watchedAddr)) {
                    return true
                }
            }
            val db = com.example.data.database.AppDatabase.getDatabase(context)
            val monitored = db.bluetoothCarDeviceDao().getMonitoredDevicesBlocking()
            monitored.any { !it.address.startsWith("AA:BB:CC") && isDeviceConnected(context, it.address) }
        } catch (_: Throwable) {
            false
        }
    }

    private val NON_CAR_REGEX_PATTERNS = listOf(
        // TVs and Displays
        Regex("""\b(tv|tvs|television|televisions|smart-?tv|apple-?tv|fire-?tv|fire-?stick|android-?tv|google-?tv|roku|bravia|chromecast|soundbar|webos|tizen|oled|qled|projector|monitor)\b""", RegexOption.IGNORE_CASE),
        // Headphones, Headsets, Earbuds, Earpieces
        Regex("""\b(headphone|headphones|headset|headsets|earphone|earphones|earbud|earbuds|earpiece|earpieces|in-?ear|over-?ear|on-?ear)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(airpod|airpods|earpod|earpods|pixel\s*buds|galaxy\s*buds|freebuds|redmi\s*buds|linkbuds)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(quietcomfort|soundlink|soundtrue|soundsport|qc35|qc45|qc\s*ultra|wh-?1000|wf-?1000|wi-?1000|mdr-|ch720|ch520)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(powerbeats|beats\s*studio|beats\s*fit|beats\s*solo|beats\s*flex|beats\s*pill|studio\s*buds)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(elite\s*(3|4|7|8|10|65|75|85)|evolve2?|shokz|aftershokz|openrun|openswim|soundcore|liberty)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(crusher|hesh|indy|dime|riff|jib)\b""", RegexOption.IGNORE_CASE),
        // Speakers and Smart Home
        Regex("""\b(speaker|speakers|loudspeaker|subwoofer|boombox|jbl|partybox|flip\s*[3-7]|charge\s*[3-6]|clip\s*[3-5]|xtreme\s*[2-4]|pulse\s*[3-5])\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(echo\s*dot|echo\s*show|echo\s*pop|alexa|homepod|google\s*home|nest\s*mini|nest\s*hub|nest\s*audio)\b""", RegexOption.IGNORE_CASE),
        // Wearables, Smartwatches & Trackers
        Regex("""\b(watch|smartwatch|smart-?watch|fitbit|garmin|forerunner|fenix|venu|whoop|oura|smart-?band|mi\s*band)\b""", RegexOption.IGNORE_CASE),
        // Computers, Tablets, Peripherals & Gaming
        Regex("""\b(ipad|tablet|galaxy\s*tab|kindle|surface|macbook|imac|laptop|desktop|thinkpad|chromebook)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(keyboard|mouse|trackpad|stylus|dualsense|dualshock|gamepad|controller|xbox|playstation|joy-?con|nintendo)\b""", RegexOption.IGNORE_CASE),
        // General mobile phones (when paired to each other)
        Regex("""\b(iphone|galaxy\s*s\d+|pixel\s*\d+[a-z]?\b)""", RegexOption.IGNORE_CASE)
    )

    fun hasNonCarKeywords(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val trimmed = name.trim()
        return NON_CAR_REGEX_PATTERNS.any { it.containsMatchIn(trimmed) }
    }

    @SuppressLint("MissingPermission")
    fun isNonCarDevice(
        name: String?,
        bluetoothDevice: BluetoothDevice? = null,
        deviceClass: Int = 0
    ): Boolean {
        val cls = if (deviceClass != 0) deviceClass else try {
            bluetoothDevice?.bluetoothClass?.deviceClass ?: 0
        } catch (_: Throwable) {
            0
        }
        val majorCls = try {
            bluetoothDevice?.bluetoothClass?.majorDeviceClass ?: 0
        } catch (_: Throwable) {
            0
        }

        // 1. Non-car Bluetooth Major Classes:
        // 1792 = WEARABLE (smartwatches, trackers)
        // 256  = COMPUTER (laptops, PCs, desktops)
        // 512  = PHONE (smartphones, cellular)
        // 1280 = PERIPHERAL (keyboards, mice, gamepads)
        // 1536 = IMAGING (printers, cameras, displays)
        if (majorCls == 1792 || majorCls == 256 || majorCls == 512 || majorCls == 1280 || majorCls == 1536) {
            return true
        }

        // 2. Non-car Audio/Video Device Classes:
        // 1048 = AUDIO_VIDEO_HEADPHONES
        // 1028 = AUDIO_VIDEO_WEARABLE_HEADSET
        // 1044 = AUDIO_VIDEO_LOUDSPEAKER
        // 1060 = AUDIO_VIDEO_SET_TOP_BOX
        // 1080 = AUDIO_VIDEO_VIDEO_MONITOR
        // 1084 = AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER (TVs)
        // 1068 = AUDIO_VIDEO_VCR
        // 1072 = AUDIO_VIDEO_VIDEO_CAMERA
        // 1076 = AUDIO_VIDEO_CAMCORDER
        // 1088 = AUDIO_VIDEO_VIDEO_CONFERENCING
        if (cls == 1048 || cls == 1028 || cls == 1044 || cls == 1060 || cls == 1080 ||
            cls == 1084 || cls == 1068 || cls == 1072 || cls == 1076 || cls == 1088
        ) {
            return true
        }

        // 3. Name-based non-car detection (Headphones, Earbuds, TVs, Speakers, Watches, PCs, etc.)
        if (hasNonCarKeywords(name)) {
            return true
        }

        return false
    }

    @SuppressLint("MissingPermission")
    fun inferDeviceType(device: BluetoothDevice): String {
        return try {
            val deviceClass = device.bluetoothClass?.deviceClass ?: 0
            val majorClass = device.bluetoothClass?.majorDeviceClass ?: 0
            val name = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try { device.alias?.ifBlank { null } } catch (_: Throwable) { null } ?: device.name ?: ""
            } else {
                device.name ?: ""
            })

            when {
                isNonCarDevice(name = name, bluetoothDevice = device, deviceClass = deviceClass) -> {
                    val lower = name.lowercase()
                    when {
                        deviceClass == 1084 || deviceClass == 1080 || deviceClass == 1060 ||
                            Regex("""\b(tv|tvs|television|smart-?tv|apple-?tv|fire-?tv|roku|bravia|oled|qled|monitor|projector)\b""").containsMatchIn(lower) -> "TV / Display"
                        deviceClass == 1048 || deviceClass == 1028 || deviceClass == 1044 || majorClass == 1024 ||
                            Regex("""\b(headphone|headphones|headset|earphone|earphones|earbud|earbuds|airpod|airpods|buds|speaker|speakers|soundbar|quietcomfort|jbl|boombox|wh-?1000|wf-?1000)\b""").containsMatchIn(lower) -> "Audio / Headphones"
                        majorClass == 1792 || Regex("""\b(watch|smartwatch|fitbit|garmin|band|forerunner|fenix|whoop)\b""").containsMatchIn(lower) -> "Wearable / Watch"
                        majorClass == 512 -> "Phone"
                        majorClass == 256 -> "Computer"
                        majorClass == 1280 -> "Peripheral / Controller"
                        else -> "Bluetooth Device"
                    }
                }
                isCarDevice(name = name, bluetoothDevice = device, deviceClass = deviceClass) -> "Car Audio / Infotainment"
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

    @SuppressLint("MissingPermission")
    fun isCarDevice(
        name: String?,
        deviceType: String? = null,
        bluetoothDevice: BluetoothDevice? = null,
        deviceClass: Int = 0
    ): Boolean {
        // 1. Explicit exclusion: never classify headphones, TVs, smartwatches, speakers, PCs, etc. as cars
        if (isNonCarDevice(name = name, bluetoothDevice = bluetoothDevice, deviceClass = deviceClass)) {
            return false
        }

        // 2. Hardware device class check: 1056 = BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO
        val cls = if (deviceClass != 0) deviceClass else try {
            bluetoothDevice?.bluetoothClass?.deviceClass ?: 0
        } catch (_: Throwable) {
            0
        }
        if (cls == 1056) {
            return true
        }

        // 3. Positive vehicle keyword detection in name
        if (!name.isNullOrBlank() && com.example.receiver.BluetoothDisconnectReceiver.hasVehicleKeywords(name)) {
            return true
        }

        // 4. Positive classification from previously inferred or user-defined car type
        if (!deviceType.isNullOrBlank() &&
            (deviceType.equals("Car", ignoreCase = true) ||
             deviceType.contains("Car Audio", ignoreCase = true) ||
             deviceType.contains("Infotainment", ignoreCase = true))
        ) {
            return true
        }

        return false
    }
}

