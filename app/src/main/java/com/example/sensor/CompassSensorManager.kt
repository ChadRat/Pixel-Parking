package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.util.HapticHelper
import com.example.util.HapticProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class CompassState(
    val azimuthDegrees: Float = 0f, // 0..360 where 0 is North
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val isAlignedWithTarget: Boolean = false,
    val needsCalibration: Boolean = false
) {
    val cardinalDirection: String
        get() {
            val index = (((azimuthDegrees + 22.5f) % 360f) / 45f).toInt()
            return when (index) {
                0 -> "North"
                1 -> "North-East"
                2 -> "East"
                3 -> "South-East"
                4 -> "South"
                5 -> "South-West"
                6 -> "West"
                7 -> "North-West"
                else -> "North"
            }
        }
}

class CompassSensorManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    var hapticProfile: HapticProfile = HapticProfile.AUTO
    var waypointHapticMode: WaypointHapticMode = WaypointHapticMode.AUTO_ADAPTIVE

    val adaptiveHapticScheduler = AdaptiveWaypointHapticScheduler(context)

    private val _compassState = MutableStateFlow(CompassState())
    val compassState: StateFlow<CompassState> = _compassState.asStateFlow()

    private var targetBearing: Float? = null
    private var lastVibratedTimestamp = 0L

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var smoothedAzimuth = 0f

    private var instabilitySpikeCount = 0
    private var lastRawAzimuth = 0f
    private var lastSampleTimestamp = 0L
    private var isUserDismissedCalibration = false
    private var isManualCalibrationActive = false
    private var gravityValues: FloatArray? = null
    private var geomagneticValues: FloatArray? = null

    fun triggerManualCalibration() {
        isUserDismissedCalibration = false
        isManualCalibrationActive = true
        instabilitySpikeCount = 0
        // Restart sensor listening to reset OS-level magnetic sensor matrix and buffers
        stopListening()
        startListening()
        _compassState.value = _compassState.value.copy(needsCalibration = true)
        HapticHelper.performClickTick(context, hapticProfile)
    }

    fun dismissCalibration() {
        isManualCalibrationActive = false
        isUserDismissedCalibration = true
        instabilitySpikeCount = 0
        _compassState.value = _compassState.value.copy(needsCalibration = false)
        HapticHelper.performConfirmationHaptic(context, hapticProfile)
    }

    fun setTargetBearing(bearing: Float?) {
        if (bearing == null) {
            cancelVibration()
        }
        this.targetBearing = bearing
    }

    fun cancelVibration() {
        HapticHelper.cancelVibration(context)
    }

    fun startListening() {
        val rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            // Fallback to Accelerometer + Magnetic field sensors
            val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val magSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            if (accelSensor != null) sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI)
            if (magSensor != null) sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        cancelVibration()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            processRotationMatrix(event.accuracy)
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            gravityValues = event.values.clone()
            if (geomagneticValues != null) {
                if (SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, geomagneticValues)) {
                    processRotationMatrix(event.accuracy)
                }
            }
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagneticValues = event.values.clone()
            if (gravityValues != null) {
                if (SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, geomagneticValues)) {
                    processRotationMatrix(event.accuracy)
                }
            }
        }
    }

    private fun processRotationMatrix(accuracy: Int) {
        val rawAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        val normalizedAzimuth = (rawAzimuth + 360f) % 360f
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

        val now = System.currentTimeMillis()
        if (lastSampleTimestamp > 0L) {
            val dt = now - lastSampleTimestamp
            val deltaAngle = abs(normalizeAngleDifference(normalizedAzimuth - lastRawAzimuth))
            if (deltaAngle > 35f && dt < 150L) {
                instabilitySpikeCount++
            } else if (dt > 1000L) {
                instabilitySpikeCount = (instabilitySpikeCount - 1).coerceAtLeast(0)
            }
        }
        lastRawAzimuth = normalizedAzimuth
        lastSampleTimestamp = now

        // Fast convergence factor when calibrating
        val smoothFactor = if (isManualCalibrationActive) 0.6f else 0.25f
        smoothedAzimuth = smoothAngle(smoothedAzimuth, normalizedAzimuth, smoothFactor)

        val isAligned = checkAlignment(smoothedAzimuth)
        adaptiveHapticScheduler.checkCardinalSweep(smoothedAzimuth, waypointHapticMode)

        val isAccuracyLow = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
        val isJittery = instabilitySpikeCount >= 3

        val shouldCalibrate = isManualCalibrationActive ||
                ((isAccuracyLow || isJittery) && targetBearing != null && !isUserDismissedCalibration)

        var currentNeedsCalibration = _compassState.value.needsCalibration
        if (shouldCalibrate && !currentNeedsCalibration) {
            currentNeedsCalibration = true
            HapticHelper.performClickTick(context, hapticProfile)
        } else if (!shouldCalibrate && currentNeedsCalibration) {
            currentNeedsCalibration = false
            instabilitySpikeCount = 0
            HapticHelper.performConfirmationHaptic(context, hapticProfile)
        }

        _compassState.value = CompassState(
            azimuthDegrees = smoothedAzimuth,
            pitchDegrees = pitch,
            rollDegrees = roll,
            accuracy = accuracy,
            isAlignedWithTarget = isAligned,
            needsCalibration = shouldCalibrate
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        val lowAcc = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
        val shouldCal = isManualCalibrationActive || (lowAcc && !isUserDismissedCalibration)
        _compassState.value = _compassState.value.copy(
            accuracy = accuracy,
            needsCalibration = shouldCal
        )
    }

    private fun checkAlignment(currentAzimuth: Float): Boolean {
        val target = targetBearing ?: return false
        val diff = abs(normalizeAngleDifference(target - currentAzimuth))
        val aligned = diff <= 8f // Within 8 degrees of target
        if (aligned && System.currentTimeMillis() - lastVibratedTimestamp > 1200L) {
            triggerTargetHaptic()
            lastVibratedTimestamp = System.currentTimeMillis()
        }
        return aligned
    }

    private fun triggerTargetHaptic() {
        adaptiveHapticScheduler.playPattern(HapticPatternEvent.BEARING_LOCK, waypointHapticMode)
    }

    private fun normalizeAngleDifference(angle: Float): Float {
        var diff = angle % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return diff
    }

    private fun smoothAngle(current: Float, target: Float, factor: Float): Float {
        var diff = target - current
        while (diff < -180f) diff += 360f
        while (diff > 180f) diff -= 360f
        return (current + diff * factor + 360f) % 360f
    }
}
