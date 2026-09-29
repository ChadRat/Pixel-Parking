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
import com.example.receiver.ParkingAlarmDismissReceiver
import com.example.receiver.ParkingTimerActionReceiver
import com.example.ui.i18n.getAppStrings
import java.util.Locale

object ParkingNotificationHelper {

    const val CHANNEL_PARKING_ALERTS = "autopark_channel_alerts"
    const val CHANNEL_RADAR_SERVICE = "autopark_channel_radar"
    const val CHANNEL_TIMER_ALARM = "autopark_channel_timer_alarm"
    const val CHANNEL_TIMER_RUNNING = "autopark_channel_timer_running"

    const val NOTIFICATION_ID_PARKED = 1001
    const val NOTIFICATION_ID_SERVICE = 1002
    const val NOTIFICATION_ID_GENERAL = 1003
    const val NOTIFICATION_ID_TIMER_ALARM = 1004
    const val NOTIFICATION_ID_TIMER_RUNNING = 1005
    const val NOTIFICATION_ID_TIMER_STOPPED = 1006

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

            val alarmChannel = NotificationChannel(
                CHANNEL_TIMER_ALARM,
                "Parking Timer Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts and sound when parking meter timer expires"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setBypassDnd(true)
            }

            val timerRunningChannel = NotificationChannel(
                CHANNEL_TIMER_RUNNING,
                "Ongoing Parking Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous status of active parking timer"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(alertsChannel)
            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(timerRunningChannel)
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
            String.format(Locale.US, "%.5f, %.5f", spot.latitude, spot.longitude)
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
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * Continuous, non-dismissible system notification for ongoing timer with Open App action button.
     */
    fun showOngoingTimerNotification(context: Context, remainingSeconds: Long) {
        createNotificationChannels(context)
        val strings = context.getAppStrings()

        val hours = (remainingSeconds / 3600).toInt()
        val minutes = ((remainingSeconds % 3600) / 60).toInt()
        val secs = (remainingSeconds % 60).toInt()
        val formattedTime = if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, secs)
        }

        // Tap content -> bring app to Home Tab
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "open_home")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            201,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppBtnText = if (strings.languageSection == "Γλώσσα") "Άνοιγμα Εφαρμογής" else "Open App"
        val title = if (strings.languageSection == "Γλώσσα") "Χρονόμετρο Στάθμευσης" else "Parking Timer"
        val text = if (strings.languageSection == "Γλώσσα") "Υπολειπόμενος χρόνος: $formattedTime" else "Time remaining: $formattedTime"

        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER_RUNNING)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_directions,
                openAppBtnText,
                contentPendingIntent
            )

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TIMER_RUNNING, builder.build())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun dismissOngoingTimerNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID_TIMER_RUNNING)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * System notification informing user that timer was stopped, with option to Continue Timer.
     */
    fun showTimerStoppedNotification(context: Context) {
        createNotificationChannels(context)
        val strings = context.getAppStrings()

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "open_home")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            301,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val continueIntent = Intent(context, ParkingTimerActionReceiver::class.java).apply {
            action = ParkingTimerActionReceiver.ACTION_RESUME_TIMER
        }
        val continuePendingIntent = PendingIntent.getBroadcast(
            context,
            302,
            continueIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (strings.languageSection == "Γλώσσα") "Το χρονόμετρο διακόπηκε" else "Parking timer stopped"
        val message = if (strings.languageSection == "Γλώσσα") "Το χρονόμετρο στάθμευσης έχει τεθεί σε παύση." else "The parking timer has been stopped."
        val continueLabel = if (strings.languageSection == "Γλώσσα") "Συνέχιση Χρονομέτρου" else "Continue Timer"

        val builder = NotificationCompat.Builder(context, CHANNEL_PARKING_ALERTS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_media_play,
                continueLabel,
                continuePendingIntent
            )

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TIMER_STOPPED, builder.build())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun dismissTimerStoppedNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID_TIMER_STOPPED)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun showTimerExpiredNotification(context: Context, title: String?, message: String) {
        createNotificationChannels(context)
        val strings = context.getAppStrings()

        val displayTitle = when {
            title.isNullOrBlank() || title == "Pixel Parking" -> strings.alarmExpiredTitle
            else -> title
        }

        // Tap content -> bring app to front
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "timer_expired")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            101,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action -> triggers broadcast receiver to silence alarm and dismiss notification
        val dismissIntent = Intent(context, ParkingAlarmDismissReceiver::class.java).apply {
            action = ParkingAlarmDismissReceiver.ACTION_DISMISS_ALARM
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            102,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(displayTitle)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Shows on lockscreen
            .setOngoing(true) // Does NOT autohide while ringtone is ringing
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .setFullScreenIntent(contentPendingIntent, true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                strings.dismissAlarm,
                dismissPendingIntent
            )

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TIMER_ALARM, builder.build())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun dismissTimerExpiredNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID_TIMER_ALARM)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
