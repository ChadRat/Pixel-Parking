package com.example.sensor

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Astronomical Solar Position Calculator for calculating precise Sunrise and Sunset
 * based on geographic coordinates (Latitude, Longitude) and local timestamp using NOAA algorithms.
 */
object SunCalculator {

    data class SunTimes(
        val sunriseTimestampMillis: Long,
        val sunsetTimestampMillis: Long,
        val isDaytime: Boolean
    )

    /**
     * Determines whether the current timestamp is daytime (between sunrise and sunset)
     * for the given latitude and longitude.
     * If coordinates are null or zero, it falls back to a standard local day schedule (06:30 - 19:30).
     */
    fun getSunTimes(
        latitude: Double?,
        longitude: Double?,
        timestampMillis: Long = System.currentTimeMillis()
    ): SunTimes {
        if (latitude == null || longitude == null || (latitude == 0.0 && longitude == 0.0)) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestampMillis
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            val currentMinuteOfDay = hour * 60 + minute

            val sunriseMinute = 6 * 60 + 30 // 06:30
            val sunsetMinute = 19 * 60 + 30 // 19:30

            val isDay = currentMinuteOfDay in sunriseMinute until sunsetMinute
            return SunTimes(
                sunriseTimestampMillis = timestampMillis,
                sunsetTimestampMillis = timestampMillis,
                isDaytime = isDay
            )
        }

        val calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = timestampMillis
        }
        val dayOfYear = calUtc.get(Calendar.DAY_OF_YEAR)

        val lngHour = longitude / 15.0

        // Sunrise UTC hour
        val tRise = dayOfYear + ((6.0 - lngHour) / 24.0)
        val sunriseUtcHours = calculateSunHour(tRise, latitude, longitude, isSunrise = true)

        // Sunset UTC hour
        val tSet = dayOfYear + ((18.0 - lngHour) / 24.0)
        val sunsetUtcHours = calculateSunHour(tSet, latitude, longitude, isSunrise = false)

        val calStartOfDayUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = timestampMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDayUtcMillis = calStartOfDayUtc.timeInMillis

        val sunriseMillis = if (sunriseUtcHours != null) {
            startOfDayUtcMillis + (sunriseUtcHours * 3600 * 1000).toLong()
        } else {
            startOfDayUtcMillis + (6.5 * 3600 * 1000).toLong()
        }

        val sunsetMillis = if (sunsetUtcHours != null) {
            startOfDayUtcMillis + (sunsetUtcHours * 3600 * 1000).toLong()
        } else {
            startOfDayUtcMillis + (19.5 * 3600 * 1000).toLong()
        }

        val isDay = if (sunriseUtcHours == null || sunsetUtcHours == null) {
            // Polar day / polar night handling
            if (latitude > 0 && dayOfYear in 80..264) true
            else if (latitude < 0 && (dayOfYear < 80 || dayOfYear > 264)) true
            else false
        } else {
            timestampMillis in sunriseMillis until sunsetMillis
        }

        return SunTimes(
            sunriseTimestampMillis = sunriseMillis,
            sunsetTimestampMillis = sunsetMillis,
            isDaytime = isDay
        )
    }

    private fun calculateSunHour(
        t: Double,
        latitude: Double,
        longitude: Double,
        isSunrise: Boolean
    ): Double? {
        // Sun's mean anomaly
        val M = (0.9856 * t) - 3.289
        val mRad = Math.toRadians(M)

        // Sun's true longitude
        var L = M + (1.916 * sin(mRad)) + (0.020 * sin(2 * mRad)) + 282.634
        L = normalizeDegrees(L)
        val lRad = Math.toRadians(L)

        // Sun's right ascension
        var RA = Math.toDegrees(atan(0.91764 * tan(lRad)))
        RA = normalizeDegrees(RA)

        // RA value needs to be in the same quadrant as L
        val lQuadrant = floor(L / 90.0) * 90.0
        val raQuadrant = floor(RA / 90.0) * 90.0
        RA += (lQuadrant - raQuadrant)
        RA /= 15.0 // Convert to hours

        // Sun's declination
        val sinDec = 0.39782 * sin(lRad)
        val cosDec = cos(asin(sinDec))

        // Sun's local hour angle
        // Zenith for official sunrise/sunset is 90° 50' = 90.8333°
        val zenith = 90.8333
        val zenithRad = Math.toRadians(zenith)
        val latRad = Math.toRadians(latitude)

        val cosH = (cos(zenithRad) - (sinDec * sin(latRad))) / (cosDec * cos(latRad))

        if (cosH > 1.0) {
            // Polar night: Sun never rises
            return null
        }
        if (cosH < -1.0) {
            // Polar day: Sun never sets
            return null
        }

        val H = if (isSunrise) {
            360.0 - Math.toDegrees(acos(cosH))
        } else {
            Math.toDegrees(acos(cosH))
        }
        val HHours = H / 15.0

        // Local mean time of rising/setting
        val T = HHours + RA - (0.06571 * t) - 6.622

        // Universal Coordinated Time (UTC)
        return normalizeHours(T - (longitude / 15.0))
    }

    private fun normalizeDegrees(value: Double): Double {
        var v = value % 360.0
        if (v < 0) v += 360.0
        return v
    }

    private fun normalizeHours(value: Double): Double {
        var v = value % 24.0
        if (v < 0) v += 24.0
        return v
    }
}
