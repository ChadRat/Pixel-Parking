package com.example

import com.example.sensor.LocationHelper
import com.example.sensor.SunCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
}
