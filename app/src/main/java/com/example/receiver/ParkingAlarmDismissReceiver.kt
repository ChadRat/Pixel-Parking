package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.notification.ParkingNotificationHelper
import com.example.util.AlarmSoundHelper

class ParkingAlarmDismissReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS_ALARM = "com.example.ACTION_DISMISS_PARKING_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent) {
        AlarmSoundHelper.stopAlarm(context)
        ParkingNotificationHelper.dismissTimerExpiredNotification(context)

        // Notify in-app components to hide the active alarm UI
        val dismissIntent = Intent(ACTION_DISMISS_ALARM).apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(dismissIntent)
    }
}
