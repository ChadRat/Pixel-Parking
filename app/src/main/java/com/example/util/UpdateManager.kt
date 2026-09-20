package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.R
import com.example.receiver.AlarmReceiver
import com.example.receiver.UpdateReceiver
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.getAppStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

sealed class UpdateCheckResult {
    data class UpdateAvailable(
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String
    ) : UpdateCheckResult()

    data class UpToDate(
        val currentVersion: String,
        val latestVersion: String,
        val downloadUrl: String?
    ) : UpdateCheckResult()

    data class Error(val message: String) : UpdateCheckResult()
}

object UpdateManager {

    private const val TAG = "UpdateManager"
    private const val GITHUB_API_URL = "https://api.github.com/repos/ChadRat/Pixel-Parking/releases/latest"
    const val NOTIFICATION_CHANNEL_ID = "update_channel"
    const val NOTIFICATION_ID_UPDATE_ALERT = 1001
    const val NOTIFICATION_ID_PROGRESS = 1002
    const val NOTIFICATION_ID_COMPLETED = 1003

    private val updateScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun checkForUpdates(
        context: Context,
        isAutomatic: Boolean = false,
        language: AppLanguage,
        onResult: ((UpdateCheckResult) -> Unit)? = null
    ) {
        updateScope.launch {
            try {
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.setRequestProperty("User-Agent", "PixelParking/${BuildConfig.VERSION_NAME}")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val tagName = json.optString("tag_name", "")
                    val releaseNotes = json.optString("body", "")
                    val assets = json.optJSONArray("assets")

                    var downloadUrl = ""
                    if (assets != null && assets.length() > 0) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true) || downloadUrl.isEmpty()) {
                                downloadUrl = asset.optString("browser_download_url", "")
                            }
                        }
                    }

                    val latestVersion = tagName.trim().removePrefix("v")
                    val currentVersion = BuildConfig.VERSION_NAME.trim().removePrefix("v")

                    if (downloadUrl.isNotEmpty() && isNewerVersion(currentVersion, latestVersion)) {
                        withContext(Dispatchers.Main) {
                            showUpdateNotification(context, downloadUrl, latestVersion, language)
                            if (!isAutomatic) {
                                Toast.makeText(
                                    context,
                                    "Update found (v$latestVersion)! Check notifications or tap download.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            onResult?.invoke(
                                UpdateCheckResult.UpdateAvailable(
                                    latestVersion = latestVersion,
                                    downloadUrl = downloadUrl,
                                    releaseNotes = releaseNotes
                                )
                            )
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            if (!isAutomatic) {
                                Toast.makeText(
                                    context,
                                    "You are on the latest version (v$currentVersion)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            onResult?.invoke(
                                UpdateCheckResult.UpToDate(
                                    currentVersion = currentVersion,
                                    latestVersion = latestVersion.ifEmpty { currentVersion },
                                    downloadUrl = downloadUrl.ifEmpty { null }
                                )
                            )
                        }
                    }
                } else {
                    val errorMsg = "HTTP error ${connection.responseCode}"
                    Log.w(TAG, "Update check failed: $errorMsg")
                    withContext(Dispatchers.Main) {
                        if (!isAutomatic) {
                            Toast.makeText(context, "Failed to check for updates ($errorMsg)", Toast.LENGTH_SHORT).show()
                        }
                        onResult?.invoke(UpdateCheckResult.Error(errorMsg))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception checking for updates", e)
                withContext(Dispatchers.Main) {
                    if (!isAutomatic) {
                        Toast.makeText(context, "Error checking for updates: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                    onResult?.invoke(UpdateCheckResult.Error(e.localizedMessage ?: "Unknown network error"))
                }
            }
        }
    }

    fun isNewerVersion(current: String, latest: String): Boolean {
        return try {
            val currentClean = current.trim().removePrefix("v").substringBefore("-")
            val latestClean = latest.trim().removePrefix("v").substringBefore("-")
            val currentParts = currentClean.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latestClean.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLength = maxOf(currentParts.size, latestParts.size)

            for (i in 0 until maxLength) {
                val curr = currentParts.getOrElse(i) { 0 }
                val lat = latestParts.getOrElse(i) { 0 }
                if (lat > curr) return true
                if (lat < curr) return false
            }
            false
        } catch (e: Exception) {
            latest != current
        }
    }

    fun showUpdateNotification(
        context: Context,
        downloadUrl: String,
        versionName: String = "",
        language: AppLanguage
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val strings = getAppStrings(language)

        ensureNotificationChannel(context)

        // PendingIntent to start in-app download
        val downloadIntent = Intent(context, UpdateReceiver::class.java).apply {
            action = "com.example.ACTION_DOWNLOAD_UPDATE"
            putExtra("EXTRA_DOWNLOAD_URL", downloadUrl)
            putExtra("EXTRA_VERSION", versionName)
            setPackage(context.packageName)
        }
        val downloadPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Secondary action to open download directly in browser
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val browserPendingIntent = PendingIntent.getActivity(
            context,
            1,
            browserIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val versionSuffix = if (versionName.isNotEmpty()) " (v$versionName)" else ""
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(strings.appName)
            .setContentText("${strings.updateAvailable}$versionSuffix")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(downloadPendingIntent)
            .addAction(android.R.drawable.stat_sys_download, strings.download, downloadPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Browser", browserPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_UPDATE_ALERT, notification)
    }

    fun startDownload(context: Context, downloadUrl: String, language: AppLanguage = AppLanguage.ENGLISH) {
        val appContext = context.applicationContext
        val strings = getAppStrings(language)
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureNotificationChannel(appContext)

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(appContext, "Downloading update in background...", Toast.LENGTH_SHORT).show()
        }

        updateScope.launch {
            // Initial progress notification
            val progressBuilder = NotificationCompat.Builder(appContext, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(strings.appName)
                .setContentText("Downloading update...")
                .setProgress(100, 0, true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)

            notificationManager.notify(NOTIFICATION_ID_PROGRESS, progressBuilder.build())

            var connection: HttpURLConnection? = null
            try {
                connection = openConnectionWithRedirects(downloadUrl)
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw IllegalStateException("Server returned HTTP $responseCode")
                }

                val contentLength = connection.contentLength.toLong()
                val downloadDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: appContext.cacheDir
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }

                val apkFile = File(downloadDir, "PixelParking_update.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(apkFile)
                val buffer = ByteArray(16384)
                var bytesRead: Int
                var totalBytesRead = 0L
                var lastProgressUpdate = System.currentTimeMillis()

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastProgressUpdate > 350) {
                        lastProgressUpdate = now
                        if (contentLength > 0) {
                            val progress = (totalBytesRead * 100 / contentLength).toInt().coerceIn(0, 100)
                            val mbDownloaded = totalBytesRead / (1024 * 1024)
                            val mbTotal = contentLength / (1024 * 1024)
                            progressBuilder.setProgress(100, progress, false)
                                .setContentText("Downloading: $progress% ($mbDownloaded MB / $mbTotal MB)")
                        } else {
                            progressBuilder.setProgress(100, 0, true)
                                .setContentText("Downloading: ${totalBytesRead / (1024 * 1024)} MB")
                        }
                        notificationManager.notify(NOTIFICATION_ID_PROGRESS, progressBuilder.build())
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                notificationManager.cancel(NOTIFICATION_ID_PROGRESS)

                if (apkFile.exists() && apkFile.length() > 500_000L) {
                    Log.i(TAG, "Update APK downloaded successfully: ${apkFile.absolutePath} (${apkFile.length()} bytes)")
                    withContext(Dispatchers.Main) {
                        showCompletedNotification(appContext, apkFile)
                        installApk(appContext, apkFile)
                    }
                } else {
                    throw IllegalStateException("Downloaded file is incomplete or corrupt (${apkFile.length()} bytes)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Direct APK download failed, falling back to browser", e)
                notificationManager.cancel(NOTIFICATION_ID_PROGRESS)

                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Download error, opening in browser...", Toast.LENGTH_LONG).show()
                    openInBrowser(appContext, downloadUrl)
                }
            } finally {
                try {
                    connection?.disconnect()
                } catch (ignored: Exception) {}
            }
        }
    }

    private fun openConnectionWithRedirects(initialUrl: String): HttpURLConnection {
        var currentUrl = initialUrl
        var redirectCount = 0

        while (redirectCount < 6) {
            val url = URL(currentUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 20000
            conn.readTimeout = 40000
            conn.setRequestProperty("User-Agent", "PixelParking/${BuildConfig.VERSION_NAME}")
            conn.setRequestProperty("Accept", "application/octet-stream, application/vnd.android.package-archive, */*")

            val status = conn.responseCode
            if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == HttpURLConnection.HTTP_SEE_OTHER ||
                status == 307 || status == 308) {
                val newUrl = conn.getHeaderField("Location")
                conn.disconnect()
                if (newUrl != null) {
                    currentUrl = newUrl
                    redirectCount++
                    continue
                }
            }
            return conn
        }
        throw IllegalStateException("Too many HTTP redirects from server")
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists() || apkFile.length() < 100_000L) {
                Log.e(TAG, "Cannot install: invalid APK file")
                return
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            context,
                            "Please grant permission to install updates when prompted",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch installer", e)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Could not open installer: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCompletedNotification(context: Context, apkFile: File) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureNotificationChannel(context)

        val installBroadcastIntent = Intent(context, UpdateReceiver::class.java).apply {
            action = "com.example.ACTION_INSTALL_UPDATE"
            putExtra("EXTRA_APK_PATH", apkFile.absolutePath)
            setPackage(context.packageName)
        }
        val installPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            installBroadcastIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Pixel Parking Update")
            .setContentText("Download complete! Tap to install.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(installPendingIntent)
            .addAction(android.R.drawable.ic_menu_upload, "Install", installPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_COMPLETED, notification)
    }

    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open browser", e)
        }
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new Pixel Parking updates and download progress"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyUpdateCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
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

