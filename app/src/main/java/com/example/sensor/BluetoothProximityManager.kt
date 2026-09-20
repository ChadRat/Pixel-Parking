package com.example.sensor

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.entity.BluetoothCarDevice
import com.example.util.HapticHelper
import com.example.util.HapticProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

enum class BtSignalTier {
    SEARCHING,
    WEAK,
    MODERATE,
    CLOSE,
    VERY_CLOSE,
    IMMEDIATE
}

data class BtProximityState(
    val isSearching: Boolean = false,
    val targetDevice: BluetoothCarDevice? = null,
    val rawRssi: Int = -100,
    val smoothedRssi: Int = -100,
    val distanceMeters: Float = 0f,
    val signalStrengthPercent: Int = 0,
    val signalTier: BtSignalTier = BtSignalTier.SEARCHING,
    val relativeAngle: Float = 0f,
    val targetBearing: Float = 0f,
    val isAligned: Boolean = false,
    val lastPacketTimestamp: Long = 0L,
    val packetCount: Int = 0,
    val isConnected: Boolean = false,
    val statusText: String = "Searching for Bluetooth signal..."
)

class BluetoothProximityManager(
    private val context: Context,
    private val compassSensorManager: CompassSensorManager
) {
    companion object {
        private const val TAG = "BtProximityManager"
        private const val TX_POWER_1M = -59.0 // Calibrated 1m transmitter RSSI in dBm
        private const val PATH_LOSS_EXPONENT = 2.2 // Environmental path loss exponent
        private const val RSSI_SMOOTHING_ALPHA = 0.35f
        private const val PACKET_TIMEOUT_MS = 6000L
    }

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _proximityState = MutableStateFlow(BtProximityState())
    val proximityState: StateFlow<BtProximityState> = _proximityState.asStateFlow()

    private var activeJob: Job? = null
    private var gattRssiPollJob: Job? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var isClassicReceiverRegistered = false

    private var targetAddress: String? = null
    private var targetName: String? = null
    private var targetGpsLat: Double? = null
    private var targetGpsLng: Double? = null

    // Signal history for directional estimation
    private val rssiSamples = mutableListOf<Pair<Float, Int>>() // Pair(azimuth, smoothedRssi)
    private var lastSmoothedRssi: Float = -100f
    private var lastHapticAlignmentTime: Long = 0L
    private var previousSignalTier = BtSignalTier.SEARCHING

    // Classic Bluetooth Discovery Receiver
    private val classicDiscoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                    if (device != null && rssi != Short.MIN_VALUE.toInt()) {
                        val addr = device.address
                        val devName = try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) device.alias ?: device.name else device.name
                        } catch (_: Exception) { null }

                        if (matchesTarget(addr, devName)) {
                            onRssiReceived(rssi, isGatt = false)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    // Automatically loop discovery if proximity search is still active
                    if (_proximityState.value.isSearching) {
                        mainHandler.postDelayed({
                            restartClassicDiscoveryIfActive()
                        }, 400L)
                    }
                }
            }
        }
    }

    // BLE Scan Callback
    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val addr = device.address
            val devName = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) device.alias ?: device.name else device.name
            } catch (_: Exception) { null }

            if (matchesTarget(addr, devName)) {
                onRssiReceived(result.rssi, isGatt = false)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            for (result in results) {
                val device = result.device ?: continue
                val addr = device.address
                val devName = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) device.alias ?: device.name else device.name
                } catch (_: Exception) { null }

                if (matchesTarget(addr, devName)) {
                    onRssiReceived(result.rssi, isGatt = false)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "BLE Scan failed with error code: $errorCode")
        }
    }

    // Bluetooth GATT Callback for connected device RSSI polling
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _proximityState.value = _proximityState.value.copy(isConnected = true)
                startGattRssiPolling()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _proximityState.value = _proximityState.value.copy(isConnected = false)
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onRssiReceived(rssi, isGatt = true)
            }
        }
    }

    private fun matchesTarget(address: String?, name: String?): Boolean {
        val targetAddr = targetAddress ?: return false
        if (address != null && address.equals(targetAddr, ignoreCase = true)) {
            return true
        }
        val tName = targetName
        if (tName != null && name != null && name.isNotBlank() && tName.isNotBlank()) {
            if (name.equals(tName, ignoreCase = true) || name.contains(tName, ignoreCase = true) || tName.contains(name, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun onRssiReceived(rawRssi: Int, isGatt: Boolean) {
        if (!_proximityState.value.isSearching) return

        val now = System.currentTimeMillis()
        val prev = lastSmoothedRssi
        val smoothed = if (prev < -98f) {
            rawRssi.toFloat()
        } else {
            RSSI_SMOOTHING_ALPHA * rawRssi + (1f - RSSI_SMOOTHING_ALPHA) * prev
        }
        lastSmoothedRssi = smoothed

        val currentAzimuth = compassSensorManager.compassState.value.azimuthDegrees
        synchronized(rssiSamples) {
            rssiSamples.add(Pair(currentAzimuth, smoothed.toInt()))
            if (rssiSamples.size > 25) {
                rssiSamples.removeAt(0)
            }
        }

        // Calculate distance via log-distance path loss
        val clampedRssi = smoothed.coerceIn(-95f, -40f)
        val distance = 10.0.pow((TX_POWER_1M - clampedRssi) / (10.0 * PATH_LOSS_EXPONENT)).toFloat().coerceIn(0.4f, 35.0f)

        // Calculate signal percentage
        val pct = (((clampedRssi - (-92f)) / (-45f - (-92f))) * 100f).toInt().coerceIn(5, 100)

        val tier = when {
            smoothed >= -50f -> BtSignalTier.IMMEDIATE
            smoothed >= -62f -> BtSignalTier.VERY_CLOSE
            smoothed >= -72f -> BtSignalTier.CLOSE
            smoothed >= -82f -> BtSignalTier.MODERATE
            smoothed >= -92f -> BtSignalTier.WEAK
            else -> BtSignalTier.SEARCHING
        }

        if (tier.ordinal > previousSignalTier.ordinal && tier.ordinal >= BtSignalTier.CLOSE.ordinal) {
            HapticHelper.performClickTick(context, HapticProfile.AUTO)
        }
        previousSignalTier = tier

        val status = when (tier) {
            BtSignalTier.IMMEDIATE -> "Car is right here • Immediate proximity"
            BtSignalTier.VERY_CLOSE -> "Very close to vehicle • ${String.format("%.1f", distance)}m"
            BtSignalTier.CLOSE -> "Approaching vehicle • ${String.format("%.1f", distance)}m"
            BtSignalTier.MODERATE -> "Signal detected • Walk in this direction"
            BtSignalTier.WEAK -> "Weak signal • Keep moving"
            BtSignalTier.SEARCHING -> "Searching for Bluetooth signal..."
        }

        val count = _proximityState.value.packetCount + 1
        _proximityState.value = _proximityState.value.copy(
            rawRssi = rawRssi,
            smoothedRssi = smoothed.toInt(),
            distanceMeters = distance,
            signalStrengthPercent = pct,
            signalTier = tier,
            lastPacketTimestamp = now,
            packetCount = count,
            isConnected = isGatt || _proximityState.value.isConnected,
            statusText = status
        )
    }

    @SuppressLint("MissingPermission")
    fun startProximityFinder(
        device: BluetoothCarDevice,
        currentPhoneLat: Double? = null,
        currentPhoneLng: Double? = null,
        spotLat: Double? = null,
        spotLng: Double? = null
    ): Boolean {
        stopProximityFinder()

        targetAddress = device.address
        targetName = device.name
        targetGpsLat = spotLat
        targetGpsLng = spotLng
        lastSmoothedRssi = -100f
        previousSignalTier = BtSignalTier.SEARCHING
        synchronized(rssiSamples) {
            rssiSamples.clear()
        }

        _proximityState.value = BtProximityState(
            isSearching = true,
            targetDevice = device,
            rawRssi = -100,
            smoothedRssi = -100,
            distanceMeters = 15f,
            signalStrengthPercent = 0,
            signalTier = BtSignalTier.SEARCHING,
            statusText = "Searching for ${device.name}..."
        )

        // Check if device is a virtual test device (AA:BB:CC) or in emulator mode
        val isVirtual = device.address.startsWith("AA:BB:CC", ignoreCase = true)

        if (!isVirtual && hasBluetoothPermissions()) {
            startHardwareScanners(device)
        }

        // Launch tracking and sensor fusion loop
        activeJob = scope.launch {
            while (isActive && _proximityState.value.isSearching) {
                updateDirectionAndFusion(currentPhoneLat, currentPhoneLng)

                // Handle virtual simulation or packet timeout
                val now = System.currentTimeMillis()
                val lastTime = _proximityState.value.lastPacketTimestamp

                if (isVirtual) {
                    simulateRealisticSignal(spotLat, spotLng)
                } else if (lastTime > 0L && now - lastTime > PACKET_TIMEOUT_MS) {
                    // Packet timeout: decay signal to searching
                    _proximityState.value = _proximityState.value.copy(
                        signalTier = BtSignalTier.SEARCHING,
                        statusText = "Searching for Bluetooth signal (${device.name})..."
                    )
                }

                delay(120L)
            }
        }

        return true
    }

    private fun updateDirectionAndFusion(currentPhoneLat: Double?, currentPhoneLng: Double?) {
        val currentAzimuth = compassSensorManager.compassState.value.azimuthDegrees

        // 1. Calculate Peak RSSI Azimuth from directional antenna variations
        var peakBearing = currentAzimuth
        synchronized(rssiSamples) {
            if (rssiSamples.isNotEmpty()) {
                val topSamples = rssiSamples.sortedByDescending { it.second }.take(5)
                if (topSamples.isNotEmpty()) {
                    var sinSum = 0.0
                    var cosSum = 0.0
                    for ((az, _) in topSamples) {
                        val rad = Math.toRadians(az.toDouble())
                        sinSum += kotlin.math.sin(rad)
                        cosSum += kotlin.math.cos(rad)
                    }
                    val avgRad = kotlin.math.atan2(sinSum, cosSum)
                    peakBearing = ((Math.toDegrees(avgRad) + 360) % 360).toFloat()
                }
            }
        }

        // 2. Blend with GPS Bearing if spot coordinates are available
        var fusedTargetBearing = peakBearing
        val sLat = targetGpsLat
        val sLng = targetGpsLng
        if (sLat != null && sLng != null && currentPhoneLat != null && currentPhoneLng != null &&
            sLat != 0.0 && sLng != 0.0 && currentPhoneLat != 0.0 && currentPhoneLng != 0.0
        ) {
            val gpsBearing = LocationHelper.calculateBearingDegrees(currentPhoneLat, currentPhoneLng, sLat, sLng)
            val dist = _proximityState.value.distanceMeters

            // Weighting: at >15m trust GPS, at <6m trust Bluetooth RSSI peak
            val btWeight = when {
                dist <= 5.0f -> 0.85f
                dist <= 12.0f -> 0.60f
                dist <= 20.0f -> 0.35f
                else -> 0.15f
            }

            val radGps = Math.toRadians(gpsBearing.toDouble())
            val radBt = Math.toRadians(peakBearing.toDouble())

            val fusedSin = (1f - btWeight) * kotlin.math.sin(radGps) + btWeight * kotlin.math.sin(radBt)
            val fusedCos = (1f - btWeight) * kotlin.math.cos(radGps) + btWeight * kotlin.math.cos(radBt)
            val fusedRad = kotlin.math.atan2(fusedSin, fusedCos)
            fusedTargetBearing = ((Math.toDegrees(fusedRad) + 360) % 360).toFloat()
        }

        val relativeAngle = (fusedTargetBearing - currentAzimuth + 360f) % 360f
        val isAligned = abs(relativeAngle) <= 18f || relativeAngle >= 342f

        // Provide alignment haptic ticks
        val now = System.currentTimeMillis()
        if (isAligned && _proximityState.value.signalTier.ordinal >= BtSignalTier.CLOSE.ordinal) {
            if (now - lastHapticAlignmentTime >= 1400L) {
                lastHapticAlignmentTime = now
                HapticHelper.performAlignmentTick(context, HapticProfile.AUTO)
            }
        }

        _proximityState.value = _proximityState.value.copy(
            relativeAngle = relativeAngle,
            targetBearing = fusedTargetBearing,
            isAligned = isAligned
        )
    }

    private fun simulateRealisticSignal(spotLat: Double?, spotLng: Double?) {
        val currentAzimuth = compassSensorManager.compassState.value.azimuthDegrees
        val targetBearing = if (spotLat != null && spotLng != null) 45f else 90f
        val angleDiff = abs((targetBearing - currentAzimuth + 360f) % 360f - 180f)
        val alignmentBonus = (180f - angleDiff) / 180f * 14f // Stronger when pointing directly

        val simulatedRaw = (-68 + alignmentBonus.toInt() + kotlin.random.Random.nextInt(-2, 3)).coerceIn(-88, -42)
        onRssiReceived(simulatedRaw, isGatt = false)
    }

    @SuppressLint("MissingPermission")
    private fun startHardwareScanners(device: BluetoothCarDevice) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

        if (adapter == null || !adapter.isEnabled) {
            _proximityState.value = _proximityState.value.copy(
                statusText = "Bluetooth is turned off. Please enable Bluetooth."
            )
            return
        }

        // 1. Start Classic Bluetooth Discovery
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            if (!isClassicReceiverRegistered) {
                context.registerReceiver(classicDiscoveryReceiver, filter)
                isClassicReceiverRegistered = true
            }
            if (!adapter.isDiscovering) {
                adapter.startDiscovery()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Classic BT Discovery", e)
        }

        // 2. Start Low-Latency BLE Scanner
        try {
            val scanner: BluetoothLeScanner? = adapter.bluetoothLeScanner
            if (scanner != null) {
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0)
                    .build()
                scanner.startScan(null, settings, bleScanCallback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BLE Scan", e)
        }

        // 3. Connect GATT client if already connected or connectable
        try {
            val remoteDevice = adapter.getRemoteDevice(device.address)
            if (remoteDevice != null) {
                bluetoothGatt = remoteDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_AUTO)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect GATT for RSSI", e)
        }
    }

    private fun startGattRssiPolling() {
        gattRssiPollJob?.cancel()
        gattRssiPollJob = scope.launch {
            while (isActive && _proximityState.value.isSearching) {
                try {
                    @SuppressLint("MissingPermission")
                    bluetoothGatt?.readRemoteRssi()
                } catch (_: Exception) {}
                delay(450L)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun restartClassicDiscoveryIfActive() {
        if (!_proximityState.value.isSearching) return
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            if (adapter != null && adapter.isEnabled && !adapter.isDiscovering) {
                adapter.startDiscovery()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not restart classic discovery", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopProximityFinder() {
        activeJob?.cancel()
        activeJob = null
        gattRssiPollJob?.cancel()
        gattRssiPollJob = null

        // Stop BLE Scanner
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            adapter?.bluetoothLeScanner?.stopScan(bleScanCallback)
            if (adapter?.isDiscovering == true) {
                adapter.cancelDiscovery()
            }
        } catch (_: Exception) {}

        // Unregister Classic receiver
        if (isClassicReceiverRegistered) {
            try {
                context.unregisterReceiver(classicDiscoveryReceiver)
            } catch (_: Exception) {}
            isClassicReceiverRegistered = false
        }

        // Close GATT
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
        } catch (_: Exception) {}

        targetAddress = null
        targetName = null
        targetGpsLat = null
        targetGpsLng = null
        lastSmoothedRssi = -100f
        synchronized(rssiSamples) {
            rssiSamples.clear()
        }

        _proximityState.value = BtProximityState(isSearching = false)
    }

    private fun hasBluetoothPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scan = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
            val connect = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            return scan == PackageManager.PERMISSION_GRANTED && connect == PackageManager.PERMISSION_GRANTED
        }
        val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        return fineLocation == PackageManager.PERMISSION_GRANTED
    }
}
