package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import java.io.File
import java.io.FileOutputStream

object WearApkManager {

    private const val ASSET_FILE_NAME = "wear-debug.apk"
    const val DEFAULT_WEAR_APP_VERSION = "0.1"

    fun getWearAppVersion(context: Context): String {
        return try {
            val wearCacheDir = File(context.cacheDir, "wear_temp")
            wearCacheDir.mkdirs()
            val tempFile = File(wearCacheDir, "temp_version_check.apk")
            context.assets.open(ASSET_FILE_NAME).use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            }
            val packageInfo = context.packageManager.getPackageArchiveInfo(tempFile.absolutePath, 0)
            tempFile.delete()
            packageInfo?.versionName ?: DEFAULT_WEAR_APP_VERSION
        } catch (_: Exception) {
            DEFAULT_WEAR_APP_VERSION
        }
    }

    fun getExportFileName(context: Context): String {
        val version = getWearAppVersion(context)
        return "PixelParking-Wear-v$version.apk"
    }

    /**
     * Checks if the wear-debug.apk asset is bundled in the app.
     */
    fun isAssetAvailable(context: Context): Boolean {
        return try {
            context.assets.open(ASSET_FILE_NAME).use { true }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Gets formatted size string of the wear APK.
     */
    fun getApkSizeString(context: Context): String {
        return try {
            val fd = context.assets.openFd(ASSET_FILE_NAME)
            val bytes = fd.length
            fd.close()
            String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
        } catch (_: Exception) {
            try {
                context.assets.open(ASSET_FILE_NAME).use { stream ->
                    val bytes = stream.available()
                    String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
                }
            } catch (_: Exception) {
                "~16 MB"
            }
        }
    }

    /**
     * Exports the Wear OS debug APK directly into the user's public Downloads directory.
     */
    fun exportApkToDownloads(context: Context): Pair<Boolean, String> {
        return try {
            val fileName = getExportFileName(context)
            val inputStream = context.assets.open(ASSET_FILE_NAME)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return Pair(false, "Could not create file in Downloads")

                resolver.openOutputStream(uri)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                } ?: return Pair(false, "Could not write to Downloads")

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                Pair(true, "Saved to Downloads: $fileName")
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, fileName)

                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                Pair(true, "Saved to: ${targetFile.absolutePath}")
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Failed to export APK")
        }
    }

    /**
     * Prepares the Wear OS APK in the app's cache and opens the Android Share sheet
     * so it can be sent directly to Easy Fire Tools, Bugjaeger, GeminiMan, Drive, etc.
     */
    fun shareApkForSideload(context: Context) {
        try {
            val fileName = getExportFileName(context)
            val version = getWearAppVersion(context)
            val wearCacheDir = File(context.cacheDir, "wear")
            wearCacheDir.mkdirs()
            val cacheFile = File(wearCacheDir, fileName)

            context.assets.open(ASSET_FILE_NAME).use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Pixel Parking Wear OS APK (v$version)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Sideload Wear OS APK via...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing APK: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
