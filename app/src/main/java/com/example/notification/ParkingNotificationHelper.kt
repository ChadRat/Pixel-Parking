package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.entity.ParkingSpot
import com.example.ui.i18n.getAppStrings

object ParkingNotificationHelper {

    const val CHANNEL_PARKING_ALERTS = "autopark_channel_alerts"
    const val CHANNEL_RADAR_SERVICE = "autopark_channel_radar"
    const val NOTIFICATION_ID_PARKED = 1001
    const val NOTIFICATION_ID_SERVICE = 1002
    const val NOTIFICATION_ID_GENERAL = 1003

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val strings = context.getAppStrings()

            val alertsChannel = NotificationChannel(
                CHANNEL_PARKING_ALERTS,
                strings.notificationChannelAlertsName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = strings.notificationChannelAlertsDesc
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_RADAR_SERVICE,
                strings.notificationChannelRadarName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = strings.notificationChannelRadarDesc
            }

            notificationManager.createNotificationChannel(alertsChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    fun showCarParkedNotification(context: Context, spot: ParkingSpot) {
        createNotificationChannels(context)
        val strings = context.getAppStrings()

        // Main Tap Intent -> Open App
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("spot_id", spot.id)
            putExtra("action", "view_spot")
        }
        val pendingContentIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Navigate with Google Maps
        val mapUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${spot.latitude},${spot.longitude}&travelmode=walking")
        val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingMapIntent = PendingIntent.getActivity(
            context,
            1,
            mapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Open Waypoint Direction in App
        val compassIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "open_radar")
            putExtra("spot_id", spot.id)
        }
        val pendingCompassIntent = PendingIntent.getActivity(
            context,
            2,
            compassIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val deviceLabel = spot.bluetoothDeviceName?.takeIf { it.isNotBlank() } ?: strings.notificationCarBluetoothDefault
        val title = String.format(strings.notificationDeviceDisconnected, deviceLabel)
        val addressText = spot.address.ifBlank {
            String.format(java.util.Locale.US, "%.5f, %.5f", spot.latitude, spot.longitude)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_PARKING_ALERTS)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(addressText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(addressText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingContentIntent)
            .addAction(android.R.drawable.ic_dialog_map, strings.notificationActionGoogleMaps, pendingMapIntent)
            .addAction(android.R.drawable.ic_menu_compass, strings.notificationActionWaypointDirection, pendingCompassIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_PARKED, builder.build())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun showSystemNotification(context: Context, message: String) {
        showSystemNotification(context, null, message)
    }

    fun showSystemNotification(context: Context, title: String?, message: String) {
        createNotificationChannels(context)
        val strings = context.getAppStrings()

        val displayTitle = when {
            title.isNullOrBlank() || title == "Pixel Parking" -> strings.appName
            else -> title
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_PARKING_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(displayTitle)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            // Using current time as ID to allow multiple generic notifications if needed
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
