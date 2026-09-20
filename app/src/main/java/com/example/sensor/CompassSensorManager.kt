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
    val needsCalibration: Boolean = false,
    val hasMagneticInterference: Boolean = false
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
    
    private var hasRotationVector = false
    private var currentMagInterference = false

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
        val magSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        hasRotationVector = rotationVectorSensor != null

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            // Fallback to Accelerometer + Magnetic field sensors
            val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accelSensor != null) sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI)
        }

        // Always register magnetic field sensor to detect interference
        if (magSensor != null) {
            sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_UI)
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
            if (!hasRotationVector && geomagneticValues != null) {
                if (SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, geomagneticValues)) {
                    processRotationMatrix(event.accuracy)
                }
            }
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagneticValues = event.values.clone()

            // Detect magnetic interference (Earth's magnetic field is ~25 to 65 microteslas)
            val magnitude = kotlin.math.sqrt((event.values[0] * event.values[0] +
                                              event.values[1] * event.values[1] +
                                              event.values[2] * event.values[2]).toDouble())
            currentMagInterference = magnitude > 150.0 || magnitude < 5.0

            if (!hasRotationVector && gravityValues != null) {
                if (SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, geomagneticValues)) {
                    processRotationMatrix(event.accuracy)
                }
            }
        }
    }

    private fun processRotationMatrix(accuracy: Int) {
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        val originalPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val originalRoll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

        // Dynamically select the most horizontal axis to avoid gimbal lock and backwards readings
        // rotationMatrix[7] is the Up component of the Device Y axis (Top)
        // rotationMatrix[8] is the Up component of the Device Z axis (Screen). So -rotationMatrix[8] is Camera.
        val yUp = kotlin.math.abs(rotationMatrix[7])
        val cameraUp = kotlin.math.abs(rotationMatrix[8])

        var rawAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

        if (cameraUp < yUp) {
            // Camera is more horizontal than Top. Remap to track Camera (-Z) forward.
            // We manually remap instead of using SensorManager.remapCoordinateSystem because
            // the Android API has a bug where it produces a left-handed coordinate system
            // for this specific axis mapping, causing the azimuth to be exactly 180 degrees off.
            // Mapping: New X = Original X, New Y = -Original Z, New Z = Original Y
            val remappedMatrix = FloatArray(9)
            remappedMatrix[0] = rotationMatrix[0]
            remappedMatrix[3] = rotationMatrix[3]
            remappedMatrix[6] = rotationMatrix[6]
            
            remappedMatrix[1] = -rotationMatrix[2]
            remappedMatrix[4] = -rotationMatrix[5]
            remappedMatrix[7] = -rotationMatrix[8]
            
            remappedMatrix[2] = rotationMatrix[1]
            remappedMatrix[5] = rotationMatrix[4]
            remappedMatrix[8] = rotationMatrix[7]
            
            val remappedAngles = FloatArray(3)
            SensorManager.getOrientation(remappedMatrix, remappedAngles)
            rawAzimuth = Math.toDegrees(remappedAngles[0].toDouble()).toFloat()
        }

        val normalizedAzimuth = (rawAzimuth + 360f) % 360f

        // Always use original physical pitch and roll for UI consistency
        val pitch = originalPitch
        val roll = originalRoll

        val now = System.currentTimeMillis()
        if (lastSampleTimestamp > 0L) {
            val dt = now - lastSampleTimestamp
            val deltaAngle = kotlin.math.abs(normalizeAngleDifference(normalizedAzimuth - lastRawAzimuth))
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

        val isAccuracyLow = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
        val isJittery = instabilitySpikeCount >= 3

        if (isManualCalibrationActive && !isAccuracyLow && !isJittery && !currentMagInterference) {
            isManualCalibrationActive = false
            isUserDismissedCalibration = true
        }

        val shouldCalibrate = isManualCalibrationActive ||
                (isAccuracyLow && targetBearing != null && !isUserDismissedCalibration)

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
            needsCalibration = shouldCalibrate,
            hasMagneticInterference = currentMagInterference
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        val lowAcc = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
        
        if (isManualCalibrationActive && !lowAcc && !currentMagInterference) {
            isManualCalibrationActive = false
            isUserDismissedCalibration = true
        }
        
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
