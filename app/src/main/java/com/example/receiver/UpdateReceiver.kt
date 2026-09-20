package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.util.UpdateManager
import java.io.File

class UpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i("UpdateReceiver", "Received broadcast action: $action")

        when (action) {
            "com.example.ACTION_DOWNLOAD_UPDATE" -> {
                val downloadUrl = intent.getStringExtra("EXTRA_DOWNLOAD_URL")
                if (!downloadUrl.isNullOrBlank()) {
                    UpdateManager.startDownload(context, downloadUrl)
                } else {
                    Log.w("UpdateReceiver", "EXTRA_DOWNLOAD_URL was null or blank")
                }
            }
            "com.example.ACTION_INSTALL_UPDATE" -> {
                val apkPath = intent.getStringExtra("EXTRA_APK_PATH")
                if (!apkPath.isNullOrBlank()) {
                    val apkFile = File(apkPath)
                    UpdateManager.installApk(context, apkFile)
                }
            }
        }
    }
}
