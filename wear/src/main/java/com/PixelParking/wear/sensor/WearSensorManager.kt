package com.PixelParking.wear.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WearTelemetry(
    val distanceMeters: Float = 0f,
    val targetBearing: Float = 0f,
    val relativeArrowAngle: Float = 0f,
    val hasTarget: Boolean = false,
    val isAligned: Boolean = false
)

class WearSensorManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _azimuthDegrees = MutableStateFlow(0f)
    val azimuthDegrees: StateFlow<Float> = _azimuthDegrees.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private var targetLat: Double = 0.0
    private var targetLng: Double = 0.0
    private var hasTarget: Boolean = false
    private var crownOnRight: Boolean = true

    private val _telemetry = MutableStateFlow(WearTelemetry())
    val telemetry: StateFlow<WearTelemetry> = _telemetry.asStateFlow()

    private var locationCallback: LocationCallback? = null

    private var lastRawPhoneDist: Float? = null
    private var lastRawPhoneAngle: Float? = null

    companion object {
        private const val TAG = "WearSensorManager"
    }

    fun setCrownOrientation(isRight: Boolean) {
        if (crownOnRight != isRight) {
            crownOnRight = isRight
            if (_currentLocation.value == null && lastRawPhoneDist != null && lastRawPhoneAngle != null) {
                applyPhoneTelemetry(lastRawPhoneDist!!, lastRawPhoneAngle!!)
            } else {
                recalculateTelemetry()
            }
        }
    }

    fun setTarget(lat: Double, lng: Double, active: Boolean) {
        targetLat = lat
        targetLng = lng
        hasTarget = active
        recalculateTelemetry()
    }

    fun updateFromPhoneTelemetry(phoneDist: Float, phoneAngle: Float) {
        lastRawPhoneDist = phoneDist
        lastRawPhoneAngle = phoneAngle
        // If watch location fix is still pending, use phone telemetry
        if (_currentLocation.value == null && hasTarget) {
            applyPhoneTelemetry(phoneDist, phoneAngle)
        }
    }

    private fun applyPhoneTelemetry(phoneDist: Float, phoneAngle: Float) {
        // Apply 180° inversion offset if watch is worn with crown on the left
        val adjustedPhoneAngle = if (!crownOnRight) {
            (phoneAngle + 180f) % 360f
        } else {
            phoneAngle
        }
        val isAligned = kotlin.math.abs(adjustedPhoneAngle) < 15f || adjustedPhoneAngle > 345f
        _telemetry.value = WearTelemetry(
            distanceMeters = phoneDist,
            targetBearing = 0f,
            relativeArrowAngle = adjustedPhoneAngle,
            hasTarget = true,
            isAligned = isAligned
        )
    }

    @SuppressLint("MissingPermission")
    fun start() {
        // Compass rotation sensor
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        rotationSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        // Watch GPS
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    _currentLocation.value = loc
                    recalculateTelemetry()
                }
            }

            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { loc ->
                        _currentLocation.value = loc
                        recalculateTelemetry()
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.d(TAG, "Location permission not yet granted on watch: ${e.message}")
        }
    }

    fun stop() {
        try {
            sensorManager?.unregisterListener(this)
            locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
            locationCallback = null
        } catch (_: Exception) {}
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientation)
            var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
            if (azimuth < 0) azimuth += 360f
            _azimuthDegrees.value = azimuth
            recalculateTelemetry()
        } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
            _azimuthDegrees.value = (event.values[0] + 360f) % 360f
            recalculateTelemetry()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun recalculateTelemetry() {
        if (!hasTarget) {
            _telemetry.value = WearTelemetry(hasTarget = false)
            return
        }

        val loc = _currentLocation.value
        if (loc != null) {
            val results = FloatArray(2)
            Location.distanceBetween(
                loc.latitude, loc.longitude,
                targetLat, targetLng,
                results
            )
            val distance = results[0]
            var bearing = results[1]
            if (bearing < 0) bearing += 360f

            val azimuth = _azimuthDegrees.value
            val baseAngle = (bearing - azimuth + 360f) % 360f
            // If crown is worn on the left side (watch rotated 180°), offset sensor azimuth by 180°
            val relativeAngle = if (!crownOnRight) {
                (baseAngle + 180f) % 360f
            } else {
                baseAngle
            }
            val isAligned = kotlin.math.abs(relativeAngle) < 15f || relativeAngle > 345f

            _telemetry.value = WearTelemetry(
                distanceMeters = distance,
                targetBearing = bearing,
                relativeArrowAngle = relativeAngle,
                hasTarget = true,
                isAligned = isAligned
            )
        }
    }
}
