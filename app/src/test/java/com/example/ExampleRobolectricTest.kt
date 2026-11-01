package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Pixel Parking", appName)
  }

  @Test
  fun `test walking detection tied to android activity api`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences(com.example.geofence.GeofenceManager.PREFS_NAME, Context.MODE_PRIVATE)

    // 1. Initial / idle state: user is NOT walking
    prefs.edit()
      .putBoolean(com.example.geofence.GeofenceManager.KEY_IS_CURRENTLY_WALKING, false)
      .remove(com.example.geofence.GeofenceManager.KEY_LAST_WALKING_ACTIVITY_TIME)
      .putInt(com.example.geofence.GeofenceManager.KEY_LAST_DETECTED_ACTIVITY_TYPE, com.google.android.gms.location.DetectedActivity.STILL)
      .commit()
    org.junit.Assert.assertFalse(
      "Should not be walking when Activity API has not detected walking",
      com.example.geofence.GeofenceManager.isWalkingAccordingToActivityApi(context)
    )

    // 2. User is driving in car (IN_VEHICLE): must NOT be considered walking
    prefs.edit()
      .putBoolean(com.example.geofence.GeofenceManager.KEY_IS_CURRENTLY_WALKING, false)
      .putInt(com.example.geofence.GeofenceManager.KEY_LAST_DETECTED_ACTIVITY_TYPE, com.google.android.gms.location.DetectedActivity.IN_VEHICLE)
      .commit()
    org.junit.Assert.assertFalse(
      "Should not be walking when Activity API detects IN_VEHICLE",
      com.example.geofence.GeofenceManager.isWalkingAccordingToActivityApi(context)
    )

    // 3. Android Activity API confirms WALKING
    prefs.edit()
      .putBoolean(com.example.geofence.GeofenceManager.KEY_IS_CURRENTLY_WALKING, true)
      .putLong(com.example.geofence.GeofenceManager.KEY_LAST_WALKING_ACTIVITY_TIME, System.currentTimeMillis())
      .putInt(com.example.geofence.GeofenceManager.KEY_LAST_DETECTED_ACTIVITY_TYPE, com.google.android.gms.location.DetectedActivity.WALKING)
      .commit()
    org.junit.Assert.assertTrue(
      "Must be walking when Android Activity API confirms WALKING",
      com.example.geofence.GeofenceManager.isWalkingAccordingToActivityApi(context)
    )

    // 4. Android Activity API confirms ON_FOOT
    prefs.edit()
      .putInt(com.example.geofence.GeofenceManager.KEY_LAST_DETECTED_ACTIVITY_TYPE, com.google.android.gms.location.DetectedActivity.ON_FOOT)
      .commit()
    org.junit.Assert.assertTrue(
      "Must be walking when Android Activity API confirms ON_FOOT",
      com.example.geofence.GeofenceManager.isWalkingAccordingToActivityApi(context)
    )

    // 5. Activity API walking detection staleness (> 90s)
    prefs.edit()
      .putBoolean(com.example.geofence.GeofenceManager.KEY_IS_CURRENTLY_WALKING, false)
      .putLong(com.example.geofence.GeofenceManager.KEY_LAST_WALKING_ACTIVITY_TIME, System.currentTimeMillis() - 120_000L)
      .commit()
    org.junit.Assert.assertFalse(
      "Stale walking detection (> 90s) should not count as active walking",
      com.example.geofence.GeofenceManager.isWalkingAccordingToActivityApi(context)
    )
  }

  @Test
  fun `test safeguard requires android activity api walking`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences(com.example.geofence.GeofenceManager.PREFS_NAME, Context.MODE_PRIVATE)

    val carLat = 37.7749
    val carLng = -122.4194

    prefs.edit()
      .putFloat(com.example.geofence.GeofenceManager.KEY_CAR_STOP_LAT, carLat.toFloat())
      .putFloat(com.example.geofence.GeofenceManager.KEY_CAR_STOP_LNG, carLng.toFloat())
      .putLong(com.example.geofence.GeofenceManager.KEY_CAR_STOP_TIME, System.currentTimeMillis())
      .commit()

    // 1. Without Activity API confirming walking, safeguard MUST reject to prevent misfire
    prefs.edit()
      .putBoolean(com.example.geofence.GeofenceManager.KEY_IS_CURRENTLY_WALKING, false)
      .remove(com.example.geofence.GeofenceManager.KEY_LAST_WALKING_ACTIVITY_TIME)
      .putInt(com.example.geofence.GeofenceManager.KEY_LAST_DETECTED_ACTIVITY_TYPE, com.google.android.gms.location.DetectedActivity.IN_VEHICLE)
      .commit()

    val rejected = com.example.geofence.GeofenceManager.verifyPassengerDropOffSafeguard(context, carLat, carLng)
    org.junit.Assert.assertFalse("Safeguard MUST reject when Activity API does NOT report walking", rejected)

    // 2. With Android Activity API confirming walking, safeguard MUST approve
    prefs.edit()
      .putBoolean(com.example.geofence.GeofenceManager.KEY_IS_CURRENTLY_WALKING, true)
      .putLong(com.example.geofence.GeofenceManager.KEY_LAST_WALKING_ACTIVITY_TIME, System.currentTimeMillis())
      .putInt(com.example.geofence.GeofenceManager.KEY_LAST_DETECTED_ACTIVITY_TYPE, com.google.android.gms.location.DetectedActivity.WALKING)
      .commit()

    val approved = com.example.geofence.GeofenceManager.verifyPassengerDropOffSafeguard(context, carLat, carLng)
    org.junit.Assert.assertTrue("Safeguard MUST approve when Android Activity API confirms user is walking", approved)
  }
}
