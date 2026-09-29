package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ParkingTimerActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_RESUME_TIMER = "com.example.ACTION_RESUME_PARKING_TIMER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_RESUME_TIMER) {
            val broadcast = Intent(ACTION_RESUME_TIMER).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(broadcast)
        }
    }
}
