package com.example.util

import android.content.Context
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.getAppStrings
import com.example.ui.i18n.getSavedAppLanguage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Manages the What's New window triggers and release notes.
 *
 * Characteristics:
 * - Decoupled from GitHub releases.
 * - Displays dynamically translated notes according to the active language (English / Greek).
 * - Allows developer override in developer options.
 */
object WhatsNewManager {

    private const val PREFS_NAME = "pixel_parking_whats_new"
    private const val KEY_LAST_SEEN_MESSAGE = "last_seen_message"
    private const val KEY_LAST_SEEN_VERSION_CODE = "last_seen_version_code"
    private const val KEY_CUSTOM_DEV_MESSAGE = "custom_dev_message"
    private const val KEY_PENDING_SHOW = "pending_show"

    /**
     * Default What's New message for the latest version.
     */
    const val CURRENT_MESSAGE: String =
        "• Added a map in the history tab to display all the locations in a fun and functional way\n" +
        "• Improved the functionality of the walk away feature.\n" +
        "• Improved haptics and animations\n" +
        "• Various Bug fixes"

    private val _showWhatsNewEvent = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val showWhatsNewEvent = _showWhatsNewEvent.asSharedFlow()

    /**
     * Retrieves the active message. If a developer custom message is configured, it is returned;
     * otherwise the localized whatsNewMessage from strings (or current saved language) is used.
     */
    fun getActiveMessage(context: Context, strings: AppStrings? = null): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customDev = prefs.getString(KEY_CUSTOM_DEV_MESSAGE, null)
        if (!customDev.isNullOrBlank()) return customDev

        return strings?.whatsNewMessage
            ?: getAppStrings(context.getSavedAppLanguage()).whatsNewMessage
    }

    /**
     * Allows developer to change the message.
     * When changed, emits the event immediately.
     */
    fun setDeveloperMessage(context: Context, newMessage: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_DEV_MESSAGE, newMessage).apply()

        if (hasMessageChanged(context)) {
            _showWhatsNewEvent.tryEmit(newMessage)
        }
    }

    /**
     * Checks if the What's New message differs from what was last marked seen.
     */
    fun hasMessageChanged(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val activeMsg = getActiveMessage(context)
        val lastSeen = prefs.getString(KEY_LAST_SEEN_MESSAGE, null)
        return lastSeen != activeMsg
    }

    /**
     * Called when ACTION_MY_PACKAGE_REPLACED broadcast is received upon update installation.
     */
    fun onPackageReplaced(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PENDING_SHOW, true).apply()
    }

    /**
     * Checks if What's New should show as soon as the user opens the app after updating.
     */
    fun checkShouldShowAfterUpdate(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val activeMsg = getActiveMessage(context)
        val lastSeenMsg = prefs.getString(KEY_LAST_SEEN_MESSAGE, null)
        val lastSeenVersionCode = prefs.getInt(KEY_LAST_SEEN_VERSION_CODE, -1)
        val currentVersionCode = com.example.BuildConfig.VERSION_CODE
        val pending = prefs.getBoolean(KEY_PENDING_SHOW, false)

        if (pending && lastSeenMsg != activeMsg) {
            prefs.edit().putBoolean(KEY_PENDING_SHOW, false).apply()
            return true
        }

        // Check if this run is an update
        if (lastSeenVersionCode != currentVersionCode && lastSeenMsg != activeMsg) {
            prefs.edit().putBoolean(KEY_PENDING_SHOW, false).apply()
            return true
        }

        if (pending) {
            prefs.edit().putBoolean(KEY_PENDING_SHOW, false).apply()
            return true
        }

        return false
    }

    fun checkAndConsumePendingShow(context: Context): Boolean {
        return checkShouldShowAfterUpdate(context)
    }

    fun markMessageSeen(context: Context, message: String = getActiveMessage(context)) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LAST_SEEN_MESSAGE, message)
            .putInt(KEY_LAST_SEEN_VERSION_CODE, com.example.BuildConfig.VERSION_CODE)
            .putBoolean(KEY_PENDING_SHOW, false)
            .apply()
    }

    fun resetSeenState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_LAST_SEEN_MESSAGE)
            .remove(KEY_LAST_SEEN_VERSION_CODE)
            .remove(KEY_PENDING_SHOW)
            .apply()
    }

    fun resetToDefaultMessage(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_CUSTOM_DEV_MESSAGE)
            .apply()
    }
}
