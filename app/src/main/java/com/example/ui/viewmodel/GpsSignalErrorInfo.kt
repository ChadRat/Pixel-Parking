package com.example.ui.viewmodel

/**
 * Categorizes GPS location failures when attempting to save a parking spot.
 */
enum class GpsSignalErrorType {
    /**
     * No GPS satellite lock or coordinates could be acquired.
     */
    NO_SIGNAL,

    /**
     * GPS signal accuracy is severely degraded (> 65m) and unreliable without user override.
     */
    WEAK_SIGNAL
}

/**
 * Encapsulates error information when the app fails to save a location due to weak or missing GPS signal.
 */
data class GpsSignalErrorInfo(
    val type: GpsSignalErrorType,
    val accuracyMeters: Float = 0f,
    val spotName: String = "My Parked Car",
    val floorLevel: String = "Ground Level",
    val note: String = "",
    val customLat: Double? = null,
    val customLng: Double? = null,
    val customAddress: String? = null
)
