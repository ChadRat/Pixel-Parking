package com.example.sensor

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.ui.viewmodel.NavigationTelemetry
import kotlin.math.abs

enum class WaypointHapticMode {
    AUTO_ADAPTIVE,    // Automatically adapt between LRA ticks and ERM waveforms based on hardware profile
    LRA_TICKS_ONLY,   // Force LRA composition primitive ticks
    STANDARD_VIBRATOR,// Force standard waveform vibration
    OFF               // Haptics disabled
}

enum class HapticPatternEvent {
    BEARING_LOCK,     // Pointing directly towards car (aligned)
    PROXIMITY_PULSE,  // Dynamic sonar radar pulse based on distance
    ARRIVAL_GEIGER,   // Very close (<3m) - arrival geiger swarm
    CARDINAL_TICK,    // North/East/South/West orientation sweep tick
    CONFIRMATION      // Waypoint saved / car found confirmation
}

class AdaptiveWaypointHapticScheduler(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    val hardwareProfile: DeviceHardwareProfile by lazy {
        HardwareSensorProfiler.profileDevice(context)
    }

    private var lastHapticTimestamp: Long = 0L
    private var lastBearingLockTimestamp: Long = 0L
    private var lastCardinalTickTimestamp: Long = 0L
    private var previousAzimuthBucket: Int = -1

    /**
     * Intelligently processes navigation telemetry and triggers adaptive haptic feedback.
     */
    fun processNavigationTelemetry(
        telemetry: NavigationTelemetry,
        mode: WaypointHapticMode = WaypointHapticMode.AUTO_ADAPTIVE
    ) {
        if (mode == WaypointHapticMode.OFF) return
        if (vibrator == null || vibrator?.hasVibrator() != true) return
        if (!telemetry.hasActiveTarget) return

        val currentTime = SystemClock.elapsedRealtime()
        val distance = telemetry.distanceMeters
        val relativeAngle = abs(telemetry.relativeArrowAngle)
        val isAligned = relativeAngle < 12f || relativeAngle > 348f

        // 1. ARRIVAL GEIGER SWARM (<3 meters)
        if (distance <= 3.5f) {
            if (currentTime - lastHapticTimestamp >= 300L) {
                lastHapticTimestamp = currentTime
                playPattern(HapticPatternEvent.ARRIVAL_GEIGER, mode)
            }
            return
        }

        // 2. BEARING ALIGNMENT LOCK (Pointing directly at target)
        if (isAligned) {
            if (currentTime - lastBearingLockTimestamp >= 1200L) {
                lastBearingLockTimestamp = currentTime
                lastHapticTimestamp = currentTime
                playPattern(HapticPatternEvent.BEARING_LOCK, mode)
            }
            return
        }

        // 3. PROXIMITY SONAR PULSE (Dynamic interval based on distance)
        val requiredInterval = when {
            distance <= 8.0f -> 450L
            distance <= 18.0f -> 850L
            distance <= 35.0f -> 1400L
            distance <= 60.0f -> 2200L
            else -> 3500L
        }

        if (currentTime - lastHapticTimestamp >= requiredInterval) {
            lastHapticTimestamp = currentTime
            playPattern(HapticPatternEvent.PROXIMITY_PULSE, mode, distance)
        }
    }

    /**
     * Triggers cardinal direction (N, E, S, W) sweep haptics as user rotates phone.
     */
    fun checkCardinalSweep(azimuthDegrees: Float, mode: WaypointHapticMode = WaypointHapticMode.AUTO_ADAPTIVE) {
        if (mode == WaypointHapticMode.OFF) return
        if (vibrator == null || vibrator?.hasVibrator() != true) return

        val currentTime = SystemClock.elapsedRealtime()
        val currentBucket = ((azimuthDegrees + 45f) / 90f).toInt() % 4

        if (previousAzimuthBucket != -1 && previousAzimuthBucket != currentBucket) {
            if (currentTime - lastCardinalTickTimestamp >= 600L) {
                lastCardinalTickTimestamp = currentTime
                playPattern(HapticPatternEvent.CARDINAL_TICK, mode)
            }
        }
        previousAzimuthBucket = currentBucket
    }

    /**
     * Plays a specific haptic pattern dynamically adapting to the device's hardware capability.
     */
    fun playPattern(
        event: HapticPatternEvent,
        mode: WaypointHapticMode = WaypointHapticMode.AUTO_ADAPTIVE,
        distance: Float = 0f
    ) {
        val targetVibrator = vibrator ?: return
        if (!targetVibrator.hasVibrator()) return

        val useLra = when (mode) {
            WaypointHapticMode.LRA_TICKS_ONLY -> true
            WaypointHapticMode.STANDARD_VIBRATOR -> false
            WaypointHapticMode.AUTO_ADAPTIVE -> hardwareProfile.hasLraCapabilities
            WaypointHapticMode.OFF -> return
        }

        try {
            if (useLra && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                playLraCompositionPattern(targetVibrator, event, distance)
            } else {
                playStandardWaveformPattern(targetVibrator, event, distance)
            }
        } catch (_: Exception) {
            // Fallback for missing permissions or device exceptions
        }
    }

    private fun playLraCompositionPattern(
        vib: Vibrator,
        event: HapticPatternEvent,
        distance: Float
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        val composition = VibrationEffect.startComposition()

        when (event) {
            HapticPatternEvent.BEARING_LOCK -> {
                // Maximum strength double pulse burst for target alignment lock
                if (hardwareProfile.supportsQuickRisePrimitive) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 1.0f, 0)
                }
                if (hardwareProfile.supportsSpinPrimitive) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_SPIN, 1.0f, 0)
                } else if (hardwareProfile.supportsClickPrimitive) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0)
                }
                if (hardwareProfile.supportsThudPrimitive) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f, 10)
                } else if (hardwareProfile.supportsTickPrimitive) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f, 10)
                }
            }

            HapticPatternEvent.PROXIMITY_PULSE -> {
                // High-power LRA pulse with intensity between 0.85 and 1.0
                val intensity = (1.0f - (distance / 100f).coerceIn(0f, 0.15f)).coerceIn(0.85f, 1.0f)
                if (hardwareProfile.supportsQuickRisePrimitive) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, intensity, 0)
                }
                if (hardwareProfile.supportsClickPrimitive) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity, 0)
                } else if (hardwareProfile.supportsTickPrimitive) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity, 0)
                }
            }

            HapticPatternEvent.ARRIVAL_GEIGER -> {
                // Maximum power arrival swarm (multi-stage heavy primitives)
                if (hardwareProfile.supportsQuickRisePrimitive) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 1.0f, 0)
                }
                if (hardwareProfile.supportsClickPrimitive) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0)
                }
                if (hardwareProfile.supportsThudPrimitive) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f, 10)
                }
                if (hardwareProfile.supportsClickPrimitive) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 20)
                }
            }

            HapticPatternEvent.CARDINAL_TICK -> {
                if (hardwareProfile.supportsThudPrimitive) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f, 0)
                }
                if (hardwareProfile.supportsClickPrimitive) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0)
                } else if (hardwareProfile.supportsTickPrimitive) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f, 0)
                }
            }

            HapticPatternEvent.CONFIRMATION -> {
                if (hardwareProfile.supportsQuickRisePrimitive) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 1.0f, 0)
                }
                if (hardwareProfile.supportsClickPrimitive) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0)
                }
                if (hardwareProfile.supportsSpinPrimitive) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_SPIN, 1.0f, 15)
                }
                if (hardwareProfile.supportsThudPrimitive) {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f, 30)
                }
            }
        }

        vib.vibrate(composition.compose())
    }

    private fun playStandardWaveformPattern(
        vib: Vibrator,
        event: HapticPatternEvent,
        distance: Float
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hardwareProfile.hasAmplitudeControl) {
            when (event) {
                HapticPatternEvent.BEARING_LOCK -> {
                    val timings = longArrayOf(0, 45, 20, 50)
                    val amplitudes = intArrayOf(0, 255, 0, 255)
                    vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                }
                HapticPatternEvent.PROXIMITY_PULSE -> {
                    val amp = ((1.0f - (distance / 80f).coerceIn(0f, 0.3f)) * 255).toInt().coerceIn(200, 255)
                    vib.vibrate(VibrationEffect.createOneShot(35, amp))
                }
                HapticPatternEvent.ARRIVAL_GEIGER -> {
                    val timings = longArrayOf(0, 30, 20, 40, 20, 50)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                    vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                }
                HapticPatternEvent.CARDINAL_TICK -> {
                    vib.vibrate(VibrationEffect.createOneShot(25, 255))
                }
                HapticPatternEvent.CONFIRMATION -> {
                    val timings = longArrayOf(0, 50, 30, 70, 30, 80)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                    vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(50)
        }
    }

    fun stopHaptics() {
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
    }
}
