package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ui.i18n.getSavedAppLanguage
import com.example.util.UpdateManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.ACTION_DAILY_UPDATE_CHECK") {
            UpdateManager.checkForUpdates(context, isAutomatic = true, language = context.getSavedAppLanguage())
        }
    }
}
