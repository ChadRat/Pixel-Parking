package com.example.ui.i18n

import android.content.Context

enum class AppLanguage(val code: String, val title: String, val nativeTitle: String) {
    ENGLISH("en", "English", "English"),
    GREEK("el", "Greek", "Ελληνικά")
}

fun Context.getSavedAppLanguage(): AppLanguage {
    val prefs = getSharedPreferences("pixel_parking_prefs", Context.MODE_PRIVATE)
    return try {
        AppLanguage.valueOf(prefs.getString("app_language", AppLanguage.ENGLISH.name) ?: AppLanguage.ENGLISH.name)
    } catch (_: Exception) {
        AppLanguage.ENGLISH
    }
}
