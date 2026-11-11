package com.example.ui.i18n

import android.content.Context

enum class AppLanguage(val code: String, val title: String, val nativeTitle: String) {
    ENGLISH("en", "English", "English"),
    GREEK("el", "Greek", "Ελληνικά")
}

fun Context.getSavedAppLanguage(): AppLanguage {
    val prefs = getSharedPreferences("pixel_parking_prefs", Context.MODE_PRIVATE)
    val saved = try { prefs.getString("app_language", null) } catch (_: Exception) { null }
    if (saved != null) {
        return try { AppLanguage.valueOf(saved) } catch (_: Exception) { AppLanguage.ENGLISH }
    }
    val sysLang = java.util.Locale.getDefault().language.lowercase()
    return if (sysLang == "el" || sysLang == "gr") AppLanguage.GREEK else AppLanguage.ENGLISH
}
