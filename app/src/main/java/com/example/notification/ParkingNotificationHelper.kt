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

object ParkingNotificationHelper {

    const val CHANNEL_PARKING_ALERTS = "autopark_channel_alerts"
    const val CHANNEL_RADAR_SERVICE = "autopark_channel_radar"
    const val NOTIFICATION_ID_PARKED = 1001
    const val NOTIFICATION_ID_SERVICE = 1002
    const val NOTIFICATION_ID_GENERAL = 1003

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alertsChannel = NotificationChannel(
                CHANNEL_PARKING_ALERTS,
                "Car Parking Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when your car's Bluetooth disconnects and automatically saves parking location"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_RADAR_SERVICE,
                "Active Waypoint Guidance",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground waypoint tracking to navigate back to your parked car"
            }

            notificationManager.createNotificationChannel(alertsChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    fun showCarParkedNotification(context: Context, spot: ParkingSpot) {
        createNotificationChannels(context)

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

        val deviceLabel = spot.bluetoothDeviceName ?: "Car Bluetooth"
        val subtitle = "${spot.address}\nFloor: ${spot.floorLevel}"

        val builder = NotificationCompat.Builder(context, CHANNEL_PARKING_ALERTS)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("$deviceLabel Disconnected")
            .setContentText("Location automatically saved at ${spot.address}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${spot.spotName}\n${spot.address}\nFloor: ${spot.floorLevel}\nSaved automatically via Bluetooth disconnect.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingContentIntent)
            .addAction(android.R.drawable.ic_dialog_map, "Google Maps", pendingMapIntent)
            .addAction(android.R.drawable.ic_menu_compass, "Waypoint Direction", pendingCompassIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_PARKED, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showSystemNotification(context: Context, message: String) {
        showSystemNotification(context, null, message)
    }

    fun showSystemNotification(context: Context, title: String?, message: String) {
        createNotificationChannels(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_PARKING_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .apply {
                if (!title.isNullOrBlank() && title != "Pixel Parking") {
                    setContentTitle(title)
                }
            }
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            // Using current time as ID to allow multiple generic notifications if needed
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
