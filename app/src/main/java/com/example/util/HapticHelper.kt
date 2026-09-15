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
}
