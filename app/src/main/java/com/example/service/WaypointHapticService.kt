package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.notification.ParkingNotificationHelper
import com.example.ui.i18n.getAppStrings
import com.example.ui.i18n.localizeSpotName
import com.example.sensor.AdaptiveWaypointHapticScheduler
import com.example.sensor.DeviceHardwareProfile
import com.example.sensor.HapticPatternEvent
import com.example.sensor.HardwareSensorProfiler
import com.example.sensor.WaypointHapticMode
import com.example.ui.viewmodel.NavigationTelemetry

class WaypointHapticService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): WaypointHapticService = this@WaypointHapticService
    }

    private val binder = LocalBinder()
    private lateinit var hapticScheduler: AdaptiveWaypointHapticScheduler

    val hardwareProfile: DeviceHardwareProfile by lazy {
        HardwareSensorProfiler.profileDevice(this)
    }

    var currentHapticMode: WaypointHapticMode = WaypointHapticMode.AUTO_ADAPTIVE

    override fun onCreate() {
        super.onCreate()
        hapticScheduler = AdaptiveWaypointHapticScheduler(this)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START_HAPTIC_SERVICE -> {
                val spotName = intent.getStringExtra(EXTRA_SPOT_NAME) ?: "Parked Car"
                val modeName = intent.getStringExtra(EXTRA_HAPTIC_MODE) ?: WaypointHapticMode.AUTO_ADAPTIVE.name
                currentHapticMode = try {
                    WaypointHapticMode.valueOf(modeName)
                } catch (_: Exception) {
                    WaypointHapticMode.AUTO_ADAPTIVE
                }

                val notification = buildHapticServiceNotification(
                    spotName = spotName,
                    distanceText = "Adaptive haptic navigation active (${hardwareProfile.actuatorType.name})"
                )
                startForeground(NOTIFICATION_ID_HAPTIC_SERVICE, notification)
            }

            ACTION_UPDATE_TELEMETRY -> {
                val distance = intent.getFloatExtra(EXTRA_DISTANCE_METERS, 0f)
                val relativeAngle = intent.getFloatExtra(EXTRA_RELATIVE_ANGLE, 0f)
                val hasTarget = intent.getBooleanExtra(EXTRA_HAS_TARGET, false)

                val telemetry = NavigationTelemetry(
                    distanceMeters = distance,
                    formattedDistance = String.format("%.1fm", distance),
                    targetBearing = 0f,
                    relativeArrowAngle = relativeAngle,
                    altitudeDifference = 0.0,
                    hasActiveTarget = hasTarget
                )

                hapticScheduler.processNavigationTelemetry(telemetry, currentHapticMode)
            }

            ACTION_TEST_PATTERN -> {
                val eventName = intent.getStringExtra(EXTRA_EVENT_TYPE) ?: HapticPatternEvent.BEARING_LOCK.name
                val event = try {
                    HapticPatternEvent.valueOf(eventName)
                } catch (_: Exception) {
                    HapticPatternEvent.BEARING_LOCK
                }
                hapticScheduler.playPattern(event, currentHapticMode)
            }

            ACTION_STOP_HAPTIC_SERVICE -> {
                hapticScheduler.stopHaptics()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    fun processTelemetryDirectly(telemetry: NavigationTelemetry) {
        hapticScheduler.processNavigationTelemetry(telemetry, currentHapticMode)
    }

    fun triggerTestPattern(event: HapticPatternEvent) {
        hapticScheduler.playPattern(event, currentHapticMode)
    }

    fun triggerCardinalSweep(azimuthDegrees: Float) {
        hapticScheduler.checkCardinalSweep(azimuthDegrees, currentHapticMode)
    }

    private fun buildHapticServiceNotification(spotName: String, distanceText: String): Notification {
        val strings = getAppStrings()
        val displaySpotName = localizeSpotName(spotName, strings)
        val title = String.format(strings.notificationWaypointHaptics, displaySpotName)

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("action", "open_radar")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, WaypointHapticService::class.java).apply {
            action = ACTION_STOP_HAPTIC_SERVICE
        }
        val pendingStopIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ParkingNotificationHelper.CHANNEL_RADAR_SERVICE)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title)
            .setContentText(distanceText)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, strings.notificationStopHaptics, pendingStopIntent)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID_HAPTIC_SERVICE = 9002

        const val ACTION_START_HAPTIC_SERVICE = "com.example.autopark.START_HAPTIC_SERVICE"
        const val ACTION_UPDATE_TELEMETRY = "com.example.autopark.UPDATE_HAPTIC_TELEMETRY"
        const val ACTION_TEST_PATTERN = "com.example.autopark.TEST_HAPTIC_PATTERN"
        const val ACTION_STOP_HAPTIC_SERVICE = "com.example.autopark.STOP_HAPTIC_SERVICE"

        const val EXTRA_SPOT_NAME = "extra_spot_name"
        const val EXTRA_HAPTIC_MODE = "extra_haptic_mode"
        const val EXTRA_DISTANCE_METERS = "extra_distance_meters"
        const val EXTRA_RELATIVE_ANGLE = "extra_relative_angle"
        const val EXTRA_HAS_TARGET = "extra_has_target"
        const val EXTRA_EVENT_TYPE = "extra_event_type"

        fun startHapticService(context: Context, spotName: String, mode: WaypointHapticMode) {
            val intent = Intent(context, WaypointHapticService::class.java).apply {
                action = ACTION_START_HAPTIC_SERVICE
                putExtra(EXTRA_SPOT_NAME, spotName)
                putExtra(EXTRA_HAPTIC_MODE, mode.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateTelemetry(context: Context, distance: Float, relativeAngle: Float, hasTarget: Boolean) {
            val intent = Intent(context, WaypointHapticService::class.java).apply {
                action = ACTION_UPDATE_TELEMETRY
                putExtra(EXTRA_DISTANCE_METERS, distance)
                putExtra(EXTRA_RELATIVE_ANGLE, relativeAngle)
                putExtra(EXTRA_HAS_TARGET, hasTarget)
            }
            context.startService(intent)
        }

        fun triggerTestPattern(context: Context, event: HapticPatternEvent) {
            val intent = Intent(context, WaypointHapticService::class.java).apply {
                action = ACTION_TEST_PATTERN
                putExtra(EXTRA_EVENT_TYPE, event.name)
            }
            context.startService(intent)
        }

        fun stopHapticService(context: Context) {
            val intent = Intent(context, WaypointHapticService::class.java).apply {
                action = ACTION_STOP_HAPTIC_SERVICE
            }
            context.startService(intent)
        }
    }
}
