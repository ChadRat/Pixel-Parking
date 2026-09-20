package com.example

import com.example.sensor.LocationHelper
import com.example.sensor.SunCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testDistanceCalculation() {
        val distance = LocationHelper.calculateDistanceMeters(
            lat1 = 37.7749,
            lng1 = -122.4194,
            lat2 = 37.7749,
            lng2 = -122.4194
        )
        assertEquals(0f, distance, 0.1f)
    }

    @Test
    fun testBearingCalculation() {
        val bearing = LocationHelper.calculateBearingDegrees(
            lat1 = 37.7749,
            lng1 = -122.4194,
            lat2 = 37.7849,
            lng2 = -122.4194
        )
        // Heading North is approx 0 degrees
        assertEquals(0f, bearing, 1.0f)
    }

    @Test
    fun testDistanceFormatting() {
        val distShort = LocationHelper.formatDistance(25f)
        assertEquals("25 m", distShort)

        val distLong = LocationHelper.formatDistance(1500f)
        assertEquals("1.5 km", distLong)
    }

    @Test
    fun testSunCalculator() {
        val times = SunCalculator.getSunTimes(37.9838, 23.7275) // Athens, Greece
        assertTrue(times.sunriseTimestampMillis > 0)
        assertTrue(times.sunsetTimestampMillis > 0)
        assertTrue(times.sunsetTimestampMillis > times.sunriseTimestampMillis)
    }

    @Test
    fun testGeofenceConstants() {
        assertEquals("CAR_PERIMETER_GEOFENCE", com.example.geofence.GeofenceManager.GEOFENCE_REQUEST_ID)
        assertTrue(com.example.geofence.GeofenceManager.DEFAULT_PERIMETER_RADIUS_METERS > 0f)
    }

    @Test
    fun testAppThemeModes() {
        val modes = com.example.ui.theme.AppThemeMode.values()
        assertEquals(3, modes.size)
        assertTrue(modes.contains(com.example.ui.theme.AppThemeMode.SYSTEM))
        assertTrue(modes.contains(com.example.ui.theme.AppThemeMode.LIGHT))
        assertTrue(modes.contains(com.example.ui.theme.AppThemeMode.DARK))
    }
}
