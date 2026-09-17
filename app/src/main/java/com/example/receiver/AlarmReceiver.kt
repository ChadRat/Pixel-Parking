package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ui.i18n.AppLanguage
import com.example.util.UpdateManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.ACTION_DAILY_UPDATE_CHECK") {
            // Note: In a real app we might want to fetch language from datastore
            UpdateManager.checkForUpdates(context, isAutomatic = true, language = AppLanguage.ENGLISH)
        }
    }
}
