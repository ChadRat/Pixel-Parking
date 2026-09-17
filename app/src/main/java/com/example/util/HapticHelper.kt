package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class HapticProfile {
    AUTO,
    LRA_PRECISION,
    STANDARD_MOTOR
}

object HapticHelper {

    fun isLraSupported(context: Context): Boolean {
        val vibrator = getVibrator(context) ?: return false
        if (!vibrator.hasVibrator()) return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val hasAmplitude = vibrator.hasAmplitudeControl()
            val hasPrimitives = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                vibrator.areAllPrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_TICK,
                    VibrationEffect.Composition.PRIMITIVE_CLICK
                )
            } else {
                vibrator.areAllPrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_TICK,
                    VibrationEffect.Composition.PRIMITIVE_CLICK
                )
            }
            hasAmplitude && hasPrimitives
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.hasAmplitudeControl()
        } else {
            false
        }
    }

    fun getRecommendedProfile(context: Context): HapticProfile {
        return if (isLraSupported(context)) HapticProfile.LRA_PRECISION else HapticProfile.STANDARD_MOTOR
    }

    fun getEffectiveProfile(context: Context, profile: HapticProfile): HapticProfile {
        return if (profile == HapticProfile.AUTO) {
            getRecommendedProfile(context)
        } else {
            profile
        }
    }

    fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun performAlignmentTick(context: Context, profile: HapticProfile) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        val effective = getEffectiveProfile(context, profile)

        try {
            if (effective == HapticProfile.LRA_PRECISION && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                vibrator.vibrate(
                    VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 1.0f, 0)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f, 10)
                        .compose()
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 45, 20, 50)
                val amplitudes = intArrayOf(0, 255, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (_: Exception) {}
    }

    fun performClickTick(context: Context, profile: HapticProfile) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        val effective = getEffectiveProfile(context, profile)

        try {
            if (effective == HapticProfile.LRA_PRECISION && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                vibrator.vibrate(
                    VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 1.0f, 0)
                        .compose()
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(45, 255))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(45)
            }
        } catch (_: Exception) {}
    }

    fun performConfirmationHaptic(context: Context, profile: HapticProfile) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        val effective = getEffectiveProfile(context, profile)

        try {
            if (effective == HapticProfile.LRA_PRECISION && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                vibrator.vibrate(
                    VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 1.0f, 0)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 10)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_SPIN, 1.0f, 20)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f, 30)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 40)
                        .compose()
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 50, 30, 70, 30, 80)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 50, 30, 70, 30, 80), -1)
            }
        } catch (_: Exception) {}
    }

    fun cancelVibration(context: Context) {
        try {
            getVibrator(context)?.cancel()
        } catch (_: Exception) {}
    }

    /**
     * Car Headlight Flash Haptic:
     * Exclusively matches the double flashing of the headlights (Flash 1 at ~300ms, Flash 2 at ~750ms).
     * Two crisp, tactile light-switch clicks synchronized with the visual light bursts.
     */
    fun performCarEngineStartHaptic(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        cancelVibration(context)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vibrator.areAllPrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_CLICK,
                    VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
                )
            ) {
                val comp = VibrationEffect.startComposition()
                // Flash 1 (starts at 150ms, peaks at 300ms)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.70f, 150)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.90f, 0)
                // Flash 2 (starts at 600ms, peaks at 750ms)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.75f, 300)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.95f, 0)
                vibrator.vibrate(comp.compose())
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(
                    150, 32, // Flash 1 (light burst)
                    270, 32  // Flash 2 (light burst)
                )
                val amplitudes = intArrayOf(
                    0,   210,
                    0,   225
                )
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(150, 30, 270, 30), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Enhanced LRA Waypoint Path Haptic (2.0s Organic Sequence):
     * Smooth, delicate gliding feedback matching the waypoint steering left (apex tick),
     * sweeping right (apex tick), moving to center, and a final bouncy tactile tick
     * when it points up and settles with its subtle spring oscillation.
     */
    fun performWaypointPathHaptic(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        cancelVibration(context)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vibrator.areAllPrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_CLICK,
                    VibrationEffect.Composition.PRIMITIVE_TICK
                )
            ) {
                val comp = VibrationEffect.startComposition()
                // 500ms: Left apex glide hit
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.70f, 500)
                // 1100ms: Right apex arch hit
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.75f, 600)
                // 1600ms - 1700ms: Reaches center pointing up and does its spring bounce;
                // tactile bouncy tick matching the upward snap & bounce
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.65f, 550)
                vibrator.vibrate(comp.compose())
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(
                    500, 20,  // ~500ms left apex tick
                    580, 20,  // ~1100ms right apex tick
                    530, 16   // ~1650ms bouncy tactile tick matching upward center bounce
                )
                val amplitudes = intArrayOf(
                    0,   180,
                    0,   190,
                    0,   160
                )
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(500, 20, 580, 20, 530, 16), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Enhanced LRA History Spin & Reversal Haptic (2.0s Organic Sequence):
     * Smooth mechanical ratchet ticks as it spins through 360° + overshoot,
     * and a soft catch at the reversal point.
     * All ending pulses and harsh shocks have been completely removed.
     */
    fun performHistorySpinHaptic(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        cancelVibration(context)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vibrator.areAllPrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_CLICK,
                    VibrationEffect.Composition.PRIMITIVE_TICK
                )
            ) {
                val comp = VibrationEffect.startComposition()
                // 0ms - 1400ms: Rotates one full turn + overshoot (light ratchet spin ticks)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.70f, 0)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.70f, 250)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.75f, 280)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.80f, 310)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.80f, 330)
                // 1400ms: Soft reversal apex catch tick (NO trailing or ending pulse)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.75f, 240)
                vibrator.vibrate(comp.compose())
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(
                    0,   18,
                    230, 18,
                    260, 20,
                    280, 20,
                    300, 20,
                    220, 24  // Apex reversal catch (~1400ms) - NO ending pulse
                )
                val amplitudes = intArrayOf(
                    0,   160,
                    0,   160,
                    0,   170,
                    0,   180,
                    0,   180,
                    0,   190
                )
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 18, 250, 18, 300, 20, 250, 20), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * LRA Mechanical Gear Deceleration Haptic (2.0s Sequence):
     * Exclusively ultra-strong tactile clicks as each tooth hits while rotating and decelerating.
     * With 6 gear teeth rotating through 360°, exactly 6 teeth pass through the contact point,
     * producing 6 distinct ultra-strong tactile clicks with increasing intervals matching the deceleration.
     */
    fun performGearMechanicalClickHaptic(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        cancelVibration(context)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vibrator.areAllPrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_CLICK
                )
            ) {
                val comp = VibrationEffect.startComposition()
                // Exactly 6 mechanical tooth clicks + 1 final settling tick synchronized to the gear rotation:
                // 1. Initial mechanical pickup as inertia turns the gear to ~50°
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.85f, 365)
                // 2. Rapid accelerating tooth engagement leading into peak rotational speed (149ms interval)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.95f, 149)
                // 3. Peak angular velocity tooth click at ~195° (157ms interval)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 157)
                // 4. Deceleration tooth click as visual rotation noticeably slows at ~265° (199ms interval)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.95f, 199)
                // 5. Progressively slowed down tooth engagement at ~325° (288ms interval)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.9f, 288)
                // 6. Soft slowed-down tooth contact as the gear reaches ~358° (480ms interval)
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, 480)
                // 7. Final settling tick as the gear smoothly clicks into place returning to its starting position at 360° / 2000ms
                val finalPrimitive = if (vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)) {
                    VibrationEffect.Composition.PRIMITIVE_TICK
                } else {
                    VibrationEffect.Composition.PRIMITIVE_CLICK
                }
                comp.addPrimitive(finalPrimitive, 0.85f, 330)
                vibrator.vibrate(comp.compose())
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Waveform of mechanical clicks ending with a final tick as the gear settles into start position:
                val timings = longArrayOf(
                    365, 18,
                    131, 18,
                    139, 18,
                    181, 18,
                    270, 18,
                    462, 22,
                    328, 12
                )
                val amplitudes = intArrayOf(
                    0,   220,
                    0,   245,
                    0,   255,
                    0,   245,
                    0,   225,
                    0,   200,
                    0,   180
                )
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(365, 18, 131, 18, 139, 18, 181, 18, 270, 18, 462, 22, 328, 12), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Strong haptic feedback released every 0.5s as timer circle is held down to reset
     */
    fun performHoldStepTick(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)
            ) {
                val comp = VibrationEffect.startComposition()
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
                vibrator.vibrate(comp.compose())
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(45)
            }
        } catch (_: Exception) {}
    }

    /**
     * Strong confirmation haptic burst when 3.0s hold completes and resets timer to 0
     */
    fun performResetConfirmation(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 70, 50, 140), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        } catch (_: Exception) {}
    }
}
