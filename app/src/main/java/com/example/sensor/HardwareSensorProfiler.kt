package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class ActuatorType {
    HIGH_PRECISION_LRA,       // Linear Resonant Actuator with rich primitive composition support
    STANDARD_AMPLITUDE_MOTOR,  // ERM/Vibrator with amplitude control support
    BASIC_VIBRATOR,            // Basic legacy on/off motor
    NONE                       // No vibrator present
}

data class DeviceHardwareProfile(
    val actuatorType: ActuatorType,
    val hasLraCapabilities: Boolean,
    val hasAmplitudeControl: Boolean,
    val supportsTickPrimitive: Boolean,
    val supportsClickPrimitive: Boolean,
    val supportsThudPrimitive: Boolean,
    val supportsSpinPrimitive: Boolean,
    val supportsQuickRisePrimitive: Boolean,
    val supportsSlowRisePrimitive: Boolean,
    val hasMagnetometer: Boolean,
    val hasAccelerometer: Boolean,
    val hasGyroscope: Boolean,
    val apiLevel: Int,
    val deviceManufacturer: String,
    val deviceModel: String
) {
    val profileSummary: String
        get() = when (actuatorType) {
            ActuatorType.HIGH_PRECISION_LRA -> "Linear Resonant Actuator (LRA) • High-Precision Haptics"
            ActuatorType.STANDARD_AMPLITUDE_MOTOR -> "Standard ERM Motor • Variable Amplitude"
            ActuatorType.BASIC_VIBRATOR -> "Basic Vibrator • Standard Pulses"
            ActuatorType.NONE -> "No Haptic Hardware Detected"
        }
}

object HardwareSensorProfiler {

    fun profileDevice(context: Context): DeviceHardwareProfile {
        val vibrator = getVibrator(context)
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

        val hasVibrator = vibrator?.hasVibrator() == true

        var hasAmplitude = false
        var supportsTick = false
        var supportsClick = false
        var supportsThud = false
        var supportsSpin = false
        var supportsQuickRise = false
        var supportsSlowRise = false

        if (hasVibrator && vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                hasAmplitude = vibrator.hasAmplitudeControl()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                supportsTick = vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)
                supportsClick = vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)
                supportsThud = vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_THUD)
                supportsSpin = vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_SPIN)
                supportsQuickRise = vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE)
                supportsSlowRise = vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE)
            }
        }

        val hasLra = supportsTick && supportsClick && hasAmplitude

        val actuatorType = when {
            !hasVibrator -> ActuatorType.NONE
            hasLra -> ActuatorType.HIGH_PRECISION_LRA
            hasAmplitude -> ActuatorType.STANDARD_AMPLITUDE_MOTOR
            else -> ActuatorType.BASIC_VIBRATOR
        }

        val hasMag = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
        val hasAccel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        val hasGyro = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null

        return DeviceHardwareProfile(
            actuatorType = actuatorType,
            hasLraCapabilities = hasLra,
            hasAmplitudeControl = hasAmplitude,
            supportsTickPrimitive = supportsTick,
            supportsClickPrimitive = supportsClick,
            supportsThudPrimitive = supportsThud,
            supportsSpinPrimitive = supportsSpin,
            supportsQuickRisePrimitive = supportsQuickRise,
            supportsSlowRisePrimitive = supportsSlowRise,
            hasMagnetometer = hasMag,
            hasAccelerometer = hasAccel,
            hasGyroscope = hasGyro,
            apiLevel = Build.VERSION.SDK_INT,
            deviceManufacturer = Build.MANUFACTURER,
            deviceModel = Build.MODEL
        )
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
