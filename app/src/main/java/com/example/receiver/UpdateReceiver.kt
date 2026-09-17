package com.example.receiver

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast

class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        if (action == "com.example.ACTION_DOWNLOAD_UPDATE") {
            val downloadUrl = intent.getStringExtra("EXTRA_DOWNLOAD_URL")
            if (downloadUrl != null) {
                Toast.makeText(context, "Starting download...", Toast.LENGTH_SHORT).show()
                startDownload(context, downloadUrl)
            }
        } else if (action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId != -1L) {
                installUpdate(context, downloadId)
            }
        }
    }

    private fun startDownload(context: Context, url: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Pixel Parking Update")
            .setDescription("Downloading latest version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "pixel_parking_update.apk")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }

    private fun installUpdate(context: Context, downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = downloadManager.getUriForDownloadedFile(downloadId)
        if (uri != null) {
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            try {
                context.startActivity(installIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
