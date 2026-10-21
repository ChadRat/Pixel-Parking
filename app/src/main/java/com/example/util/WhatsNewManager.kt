package com.example.util

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Manages the What's New window triggers and developer release notes.
 *
 * Characteristics:
 * - The window only shows up dynamically when an update is being downloaded
 *   and as soon as the developer changes its message.
 * - Current default message: "hello world ( ꈍᴗꈍ) "
 * - Covers dynamically the screen with as much space needed according to how much text it has.
 * - Similar style to the BT car window without any X or back buttons and blurring the rest of the screen.
 */
object WhatsNewManager {

    private const val PREFS_NAME = "pixel_parking_whats_new"
    private const val KEY_LAST_SEEN_MESSAGE = "last_seen_message"
    private const val KEY_CUSTOM_DEV_MESSAGE = "custom_dev_message"
    private const val KEY_PENDING_SHOW = "pending_show"

    /**
     * The developer-controlled What's New message.
     * Right now, the default message is: "hello world ( ꈍᴗꈍ) "
     */
    const val CURRENT_MESSAGE: String = "hello world ( ꈍᴗꈍ) "

    private val _showWhatsNewEvent = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val showWhatsNewEvent = _showWhatsNewEvent.asSharedFlow()

    /**
     * Retrieves the active message (either custom set in dev options or default CURRENT_MESSAGE).
     */
    fun getActiveMessage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_DEV_MESSAGE, CURRENT_MESSAGE) ?: CURRENT_MESSAGE
    }

    /**
     * Allows developer to change the message.
     * As soon as the developer changes the message, if a download is active,
     * the window immediately appears.
     */
    fun setDeveloperMessage(context: Context, newMessage: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_DEV_MESSAGE, newMessage).apply()

        // "as soon as the developer changes its message"
        if (hasMessageChanged(context)) {
            _showWhatsNewEvent.tryEmit(newMessage)
        }
    }

    /**
     * Checks if the developer has updated the What's New message since the user last saw it.
     */
    fun hasMessageChanged(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val activeMsg = getActiveMessage(context)
        val lastSeen = prefs.getString(KEY_LAST_SEEN_MESSAGE, null)
        return lastSeen != activeMsg
    }

    /**
     * Called whenever an update download begins.
     * If the developer message changed, triggers the window and flags it as pending.
     */
    fun onUpdateDownloadStarted(context: Context) {
        val activeMsg = getActiveMessage(context)
        if (hasMessageChanged(context)) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_PENDING_SHOW, true).apply()
            _showWhatsNewEvent.tryEmit(activeMsg)
        }
    }

    /**
     * Checks and consumes any pending show flag (e.g. if download started in background).
     */
    fun checkAndConsumePendingShow(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pending = prefs.getBoolean(KEY_PENDING_SHOW, false)
        if (pending && hasMessageChanged(context)) {
            prefs.edit().putBoolean(KEY_PENDING_SHOW, false).apply()
            return true
        }
        return false
    }

    /**
     * Marks the active message as seen so it will not show again until changed by developer.
     */
    fun markMessageSeen(context: Context, message: String = getActiveMessage(context)) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LAST_SEEN_MESSAGE, message)
            .putBoolean(KEY_PENDING_SHOW, false)
            .apply()
    }

    /**
     * Resets seen state (useful for developer testing).
     */
    fun resetSeenState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_LAST_SEEN_MESSAGE).apply()
    }

    /**
     * Resets to default CURRENT_MESSAGE ("hello world ( ꈍᴗꈍ) ").
     */
    fun resetToDefaultMessage(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CUSTOM_DEV_MESSAGE).apply()
    }
}
