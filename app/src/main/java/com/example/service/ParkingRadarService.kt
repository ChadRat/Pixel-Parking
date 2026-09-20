package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.notification.ParkingNotificationHelper
import com.example.ui.i18n.getAppStrings
import com.example.ui.i18n.localizeSpotName

class ParkingRadarService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_RADAR) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val strings = getAppStrings()
        val distanceText = intent?.getStringExtra("distance_text") ?: strings.notificationTrackingDistance
        val spotName = intent?.getStringExtra("spot_name") ?: strings.parkedVehicle

        val notification = buildForegroundNotification(spotName, distanceText)
        startForeground(ParkingNotificationHelper.NOTIFICATION_ID_SERVICE, notification)

        return START_STICKY
    }

    private fun buildForegroundNotification(spotName: String, distanceText: String): Notification {
        val strings = getAppStrings()
        val displaySpotName = localizeSpotName(spotName, strings)
        val title = String.format(strings.notificationActiveCompass, displaySpotName)

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

        val stopIntent = Intent(this, ParkingRadarService::class.java).apply {
            action = ACTION_STOP_RADAR
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
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, strings.notificationStopGuidance, pendingStopIntent)
            .build()
    }

    companion object {
        const val ACTION_START_RADAR = "com.example.autopark.START_RADAR"
        const val ACTION_STOP_RADAR = "com.example.autopark.STOP_RADAR"

        fun start(context: Context, spotName: String, distanceText: String) {
            val intent = Intent(context, ParkingRadarService::class.java).apply {
                action = ACTION_START_RADAR
                putExtra("spot_name", spotName)
                putExtra("distance_text", distanceText)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ParkingRadarService::class.java).apply {
                action = ACTION_STOP_RADAR
            }
            context.startService(intent)
        }
    }
}
