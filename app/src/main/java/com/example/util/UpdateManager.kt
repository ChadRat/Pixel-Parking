package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.BuildConfig
import com.example.R
import com.example.receiver.UpdateReceiver
import com.example.ui.i18n.EnglishStrings
import com.example.ui.i18n.GreekStrings
import com.example.ui.i18n.getAppStrings
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

object UpdateManager {

    private const val GITHUB_API_URL = "https://api.github.com/repos/ChadRat/Pixel-Parking/releases/latest"
    private const val NOTIFICATION_CHANNEL_ID = "update_channel"

    fun checkForUpdates(context: Context, isAutomatic: Boolean = false, language: AppLanguage) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val tagName = json.getString("tag_name")
                    val assets = json.getJSONArray("assets")
                    
                    if (assets.length() > 0) {
                        val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
                        
                        // Compare version
                        val latestVersion = tagName.replace("v", "")
                        val currentVersion = BuildConfig.VERSION_NAME.replace("v", "")
                        
                        if (isNewerVersion(currentVersion, latestVersion)) {
                            withContext(Dispatchers.Main) {
                                if (isAutomatic) {
                                    showUpdateNotification(context, downloadUrl, language)
                                } else {
                                    Toast.makeText(context, "Update found! Check notifications.", Toast.LENGTH_SHORT).show()
                                    showUpdateNotification(context, downloadUrl, language)
                                }
                            }
                        } else {
                            if (!isAutomatic) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "You are on the latest version", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                } else if (!isAutomatic) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to check for updates", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (!isAutomatic) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error checking for updates", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        return latest > current // Note: A simple string comparison. Real semver comparison would be better.
    }

    private fun showUpdateNotification(context: Context, downloadUrl: String, language: AppLanguage) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val strings = getAppStrings(language)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val downloadIntent = Intent(context, UpdateReceiver::class.java).apply {
            action = "com.example.ACTION_DOWNLOAD_UPDATE"
            putExtra("EXTRA_DOWNLOAD_URL", downloadUrl)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Pixel Parking")
            .setContentText(strings.updateAvailable)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.stat_sys_download, strings.download, pendingIntent)
            .build()

        notificationManager.notify(1001, notification)
    }

    fun scheduleDailyUpdateCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, com.example.receiver.AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_DAILY_UPDATE_CHECK"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        // If it's already past 8 AM, schedule for next day
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
