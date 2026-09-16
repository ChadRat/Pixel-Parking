package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AlarmSoundHelper {

    private const val TAG = "AlarmSoundHelper"
    private const val PREFS_NAME = "pixel_parking_prefs"
    private const val KEY_ALARM_URI = "timer_alarm_sound_uri"

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var previewJob: Job? = null

    fun getAlarmUri(context: Context): Uri {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUriString = prefs.getString(KEY_ALARM_URI, null)
        if (!savedUriString.isNullOrBlank()) {
            try {
                return Uri.parse(savedUriString)
            } catch (e: Exception) {
                Log.e(TAG, "Failed parsing saved alarm URI", e)
            }
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }

    fun saveAlarmUri(context: Context, uri: Uri) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ALARM_URI, uri.toString()).apply()
    }

    fun getAlarmTitle(context: Context, uri: Uri = getAlarmUri(context)): String {
        return try {
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.getTitle(context) ?: "Device Alarm"
        } catch (e: Exception) {
            "Default Device Alarm"
        }
    }

    @Synchronized
    fun playAlarm(context: Context) {
        stopAlarm(context)
        try {
            val alarmUri = getAlarmUri(context)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            isPlaying = true

            // Trigger distinct alarm vibration pattern
            val vibrator = HapticHelper.getVibrator(context)
            if (vibrator != null && vibrator.hasVibrator()) {
                val timings = longArrayOf(0, 700, 300, 700, 300, 1000)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(timings, 1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed playing alarm with MediaPlayer, trying Ringtone fallback", e)
            try {
                val ringtone = RingtoneManager.getRingtone(context, getAlarmUri(context))
                ringtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                ringtone?.play()
                isPlaying = true
            } catch (ex: Exception) {
                Log.e(TAG, "Ringtone fallback failed", ex)
            }
        }
    }

    @Synchronized
    fun stopAlarm(context: Context? = null) {
        previewJob?.cancel()
        previewJob = null

        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping mediaPlayer", e)
        } finally {
            mediaPlayer = null
            isPlaying = false
        }

        context?.let { ctx ->
            val vibrator = HapticHelper.getVibrator(ctx)
            vibrator?.cancel()
        }
    }

    fun isAlarmActive(): Boolean = isPlaying

    fun previewAlarm(context: Context, onFinish: () -> Unit = {}) {
        playAlarm(context)
        previewJob = CoroutineScope(Dispatchers.Main).launch {
            delay(3500)
            stopAlarm(context)
            onFinish()
        }
    }
}
