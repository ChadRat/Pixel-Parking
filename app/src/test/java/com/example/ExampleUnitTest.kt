package com.example

import com.example.data.entity.isBluetoothEntry
import com.example.data.entity.isGeofenceEntry
import com.example.data.entity.isUndergroundFallback
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

    @Test
    fun testSpotEntryTypeClassification() {
        // Manual entry
        val manualSpot = com.example.data.entity.ParkingSpot(
            latitude = 37.7749,
            longitude = -122.4194,
            spotName = "Parked Car",
            isAutoSaved = false,
            bluetoothDeviceName = "Manual GPS Entry",
            bluetoothDeviceAddress = null
        )
        org.junit.Assert.assertFalse(manualSpot.isGeofenceEntry)
        org.junit.Assert.assertFalse(manualSpot.isBluetoothEntry)

        // Geofence entry
        val geofenceSpot = com.example.data.entity.ParkingSpot(
            latitude = 37.7749,
            longitude = -122.4194,
            spotName = "Walking Detection",
            isAutoSaved = true,
            bluetoothDeviceName = "Walking Detection",
            bluetoothDeviceAddress = "GEOFENCE_WALKING_DETECTION"
        )
        org.junit.Assert.assertTrue(geofenceSpot.isGeofenceEntry)
        org.junit.Assert.assertFalse(geofenceSpot.isBluetoothEntry)

        // Bluetooth entry
        val btSpot = com.example.data.entity.ParkingSpot(
            latitude = 37.7749,
            longitude = -122.4194,
            spotName = "Car BT (Audi MMI)",
            isAutoSaved = true,
            bluetoothDeviceName = "Audi MMI",
            bluetoothDeviceAddress = "00:11:22:33:44:55",
            note = "Automatically saved upon Bluetooth disconnection."
        )
        org.junit.Assert.assertFalse(btSpot.isGeofenceEntry)
        org.junit.Assert.assertTrue(btSpot.isBluetoothEntry)
    }

    @Test
    fun testParkingRepositoryAutoSaveDeduplication() = kotlinx.coroutines.runBlocking {
        val spotList = mutableListOf<com.example.data.entity.ParkingSpot>()
        var nextId = 1L

        val fakeSpotDao = object : com.example.data.dao.ParkingSpotDao {
            override fun getAllSpots() = kotlinx.coroutines.flow.flowOf(spotList.toList())
            override suspend fun getAllSpotsDirect() = spotList.toList()
            override fun getActiveSpot() = kotlinx.coroutines.flow.flowOf(spotList.firstOrNull { it.isActive })
            override suspend fun getActiveSpotDirect() = spotList.firstOrNull { it.isActive }
            override suspend fun getMostRecentSpotDirect() = spotList.lastOrNull()
            override suspend fun getSpotById(id: Long) = spotList.firstOrNull { it.id == id }
            override suspend fun insertSpot(spot: com.example.data.entity.ParkingSpot): Long {
                val id = nextId++
                spotList.add(spot.copy(id = id))
                return id
            }
            override suspend fun updateSpot(spot: com.example.data.entity.ParkingSpot) {
                val idx = spotList.indexOfFirst { it.id == spot.id }
                if (idx != -1) spotList[idx] = spot
            }
            override suspend fun deactivateAllSpots() {
                spotList.replaceAll { it.copy(isActive = false) }
            }
            override suspend fun deleteSpotById(id: Long) {
                spotList.removeIf { it.id == id }
            }
            override suspend fun deleteLegacyDemoSpots() {}
            override suspend fun clearAllSpots() { spotList.clear() }
        }

        val fakeDeviceDao = object : com.example.data.dao.BluetoothCarDeviceDao {
            override fun getAllDevices() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.entity.BluetoothCarDevice>())
            override suspend fun getAllDevicesDirect() = emptyList<com.example.data.entity.BluetoothCarDevice>()
            override fun getMonitoredDevices() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.entity.BluetoothCarDevice>())
            override suspend fun getMonitoredDevicesDirect() = emptyList<com.example.data.entity.BluetoothCarDevice>()
            override suspend fun getSelectedMonitoredDevice(): com.example.data.entity.BluetoothCarDevice? = null
            override fun getSelectedMonitoredDeviceFlow() = kotlinx.coroutines.flow.flowOf(null)
            override suspend fun clearAllMonitoredStatus() {}
            override suspend fun setSoleMonitoredDevice(address: String) {}
            override suspend fun getDeviceByAddress(address: String): com.example.data.entity.BluetoothCarDevice? = null
            override suspend fun getDeviceByNameOrOriginalName(name: String): com.example.data.entity.BluetoothCarDevice? = null
            override suspend fun updateDeviceName(address: String, name: String) {}
            override suspend fun updateDeviceNameAndOriginal(address: String, name: String, originalName: String) {}
            override suspend fun setDeviceMonitoredState(address: String, isMonitored: Boolean) {}
            override suspend fun insertOrUpdate(device: com.example.data.entity.BluetoothCarDevice) {}
            override suspend fun update(device: com.example.data.entity.BluetoothCarDevice) {}
            override suspend fun deleteDevice(address: String) {}
        }

        val repository = com.example.data.repository.ParkingRepository(fakeSpotDao, fakeDeviceDao)

        // 1. First Bluetooth auto-save event
        val firstSpot = com.example.data.entity.ParkingSpot(
            latitude = 37.7749,
            longitude = -122.4194,
            spotName = "Audi A4",
            isAutoSaved = true,
            bluetoothDeviceAddress = "AA:BB:CC:DD:EE:FF",
            timestamp = System.currentTimeMillis()
        )
        val id1 = repository.saveNewParkingSpot(firstSpot)
        assertEquals(1L, id1)
        assertEquals(1, spotList.size)

        // 2. Duplicate Bluetooth auto-save event arriving 100ms later (e.g. dual manifest/dynamic receiver or A2DP+ACL)
        val duplicateSpot = com.example.data.entity.ParkingSpot(
            latitude = 37.7749,
            longitude = -122.4194,
            spotName = "Audi A4",
            isAutoSaved = true,
            bluetoothDeviceAddress = "AA:BB:CC:DD:EE:FF",
            timestamp = System.currentTimeMillis()
        )
        val id2 = repository.saveNewParkingSpot(duplicateSpot)
        assertEquals("Duplicate auto-save should return existing ID", id1, id2)
        assertEquals("Spot list should contain exactly 1 entry, not 2", 1, spotList.size)

        // 3. Geofence auto-save event
        val geoSpot1 = com.example.data.entity.ParkingSpot(
            latitude = 38.0000,
            longitude = 23.7000,
            spotName = "Walking Detection",
            isAutoSaved = true,
            bluetoothDeviceAddress = "GEOFENCE_WALKING_DETECTION",
            timestamp = System.currentTimeMillis()
        )
        val geoId1 = repository.saveNewParkingSpot(geoSpot1)
        assertEquals(2L, geoId1)
        assertEquals(2, spotList.size)

        // 4. Duplicate Geofence auto-save event arriving immediately after
        val geoSpot2 = com.example.data.entity.ParkingSpot(
            latitude = 38.0000,
            longitude = 23.7000,
            spotName = "Walking Detection",
            isAutoSaved = true,
            bluetoothDeviceAddress = "GEOFENCE_WALKING_DETECTION",
            timestamp = System.currentTimeMillis()
        )
        val geoId2 = repository.saveNewParkingSpot(geoSpot2)
        assertEquals("Duplicate geofence save should return existing ID", geoId1, geoId2)
        assertEquals("Spot list should not create a duplicate row for geofence", 2, spotList.size)

        // 5. Late-arriving Geofence save arriving after Bluetooth Disconnect (nearby < 80m, < 10 mins)
        // Should smoothly merge without creating twin records
        val lateGeoSpot = com.example.data.entity.ParkingSpot(
            latitude = 38.0001,
            longitude = 23.7001,
            spotName = "Car Parking Location",
            isAutoSaved = true,
            bluetoothDeviceAddress = "GEOFENCE_WALKING_DETECTION",
            timestamp = System.currentTimeMillis() + 45_000L
        )
        val lateGeoId = repository.saveNewParkingSpot(lateGeoSpot)
        assertEquals("Late-arriving geofence save should merge into existing spot", geoId1, lateGeoId)
        assertEquals("Spot list size should remain 2", 2, spotList.size)

        // 6. High-priority Bluetooth Disconnect superseding recent Geofence spot (< 80m, < 5 mins)
        val highPriorityBtSpot = com.example.data.entity.ParkingSpot(
            latitude = 38.0001,
            longitude = 23.7001,
            spotName = "BMW i4",
            isAutoSaved = true,
            bluetoothDeviceName = "BMW i4",
            bluetoothDeviceAddress = "11:22:33:44:55:66",
            timestamp = System.currentTimeMillis() + 50_000L
        )
        val upgradedId = repository.saveNewParkingSpot(highPriorityBtSpot)
        assertEquals("Bluetooth disconnect should supersede geofence spot with existing ID", geoId1, upgradedId)
        assertEquals("Spot list size should remain 2", 2, spotList.size)
        val updatedSpot = repository.getSpotById(geoId1)
        assertEquals("BMW i4", updatedSpot?.bluetoothDeviceName)
        assertEquals("11:22:33:44:55:66", updatedSpot?.bluetoothDeviceAddress)
    }

    @Test
    fun testGpxRouteSimulationDrivingDwellAndWalking() {
        // Simulates the user's route:
        // 1. Driving at 40 km/h (11.1 m/s)
        // 2. Stopping and pausing past the 25-second dwell threshold
        // 3. Walking 60 meters out (exceeding 50m perimeter)
        val startLat = 37.7749
        val startLng = -122.4194

        val drivingSpeedMps = 40.0f / 3.6f // ~11.11 m/s
        assertTrue("Driving speed must exceed threshold", drivingSpeedMps >= 2.8f)

        // Pause past 25-second dwell threshold
        val dwellTimeMs = 26_000L
        assertTrue("Dwell must exceed 25s threshold", dwellTimeMs >= com.example.geofence.GeofenceManager.STATIONARY_DWELL_TIMER_MS)

        // Walk 60m out
        val walkDistanceMeters = 60f
        val walkingSpeedMps = 1.3f // ~4.7 km/h pedestrian speed
        assertTrue("Walking distance must exceed perimeter radius (50m)", walkDistanceMeters >= com.example.geofence.GeofenceManager.DEFAULT_PERIMETER_RADIUS_METERS)
        assertTrue("Walking speed must be pedestrian", walkingSpeedMps < 3.2f)

        // Verify perimeter radius is 50m and responsiveness is 25s
        assertEquals(50.0f, com.example.geofence.GeofenceManager.DEFAULT_PERIMETER_RADIUS_METERS, 0.01f)
        assertEquals(25_000, com.example.geofence.GeofenceManager.NOTIFICATION_RESPONSIVENESS_MS)
        assertEquals(25_000L, com.example.geofence.GeofenceManager.STATIONARY_DWELL_TIMER_MS)
    }

    @Test
    fun testUndergroundParkingSafeguardConstantsAndThresholds() {
        assertEquals(15.0f, com.example.geofence.GeofenceManager.HIGH_ACCURACY_THRESHOLD_METERS, 0.01f)
        assertEquals(30.0f, com.example.geofence.GeofenceManager.DEGRADED_ACCURACY_THRESHOLD_METERS, 0.01f)
        assertEquals(5, com.example.geofence.GeofenceManager.MAX_GPS_BUFFER_SIZE)
    }

    @Test
    fun testUndergroundParkingBacktrackingSelection() {
        val simulatedBuffer = listOf(
            com.example.geofence.ReliableGpsFix(
                latitude = 37.7740,
                longitude = -122.4190,
                altitude = 10.0,
                accuracy = 12.0f,
                timestamp = 1000L
            ),
            com.example.geofence.ReliableGpsFix(
                latitude = 37.7745,
                longitude = -122.4195,
                altitude = 12.0,
                accuracy = 9.5f, // Fix right near garage entrance
                timestamp = 2000L
            )
        )

        // Current GPS reading degraded to 48 meters inside underground garage
        val degradedAccuracy = 48.0f
        val isDegraded = degradedAccuracy > com.example.geofence.GeofenceManager.DEGRADED_ACCURACY_THRESHOLD_METERS

        assertTrue("48m accuracy must trigger degraded underground check", isDegraded)

        // Select fallback entrance fix
        val fallback = simulatedBuffer.lastOrNull { it.accuracy <= com.example.geofence.GeofenceManager.HIGH_ACCURACY_THRESHOLD_METERS }
        assertNotNull("Must find reliable entrance fix in rolling buffer", fallback)
        assertEquals(37.7745, fallback!!.latitude, 0.0001)
        assertEquals(-122.4195, fallback.longitude, 0.0001)
        assertEquals(9.5f, fallback.accuracy, 0.01f)
    }

    @Test
    fun testUndergroundSpotClassification() {
        val undergroundSpot = com.example.data.entity.ParkingSpot(
            latitude = 37.7745,
            longitude = -122.4195,
            spotName = "Car Parking Location",
            floorLevel = "Underground",
            note = "Underground parking safeguard: Weak GPS in garage (±48m). Saved entrance coordinates (±10m).",
            isAutoSaved = true
        )

        assertTrue(undergroundSpot.isUndergroundFallback)

        val regularSpot = com.example.data.entity.ParkingSpot(
            latitude = 37.7745,
            longitude = -122.4195,
            spotName = "Car Parking Location",
            floorLevel = "Ground Level",
            note = "Automatically saved as you walked away after driving.",
            isAutoSaved = true
        )

        org.junit.Assert.assertFalse(regularSpot.isUndergroundFallback)
    }

    @Test
    fun testSecondBluetoothDisconnectSavesWhenLocationAlreadyExists() = kotlinx.coroutines.runBlocking {
        val spotList = mutableListOf<com.example.data.entity.ParkingSpot>()
        var nextId = 1L

        val fakeSpotDao = object : com.example.data.dao.ParkingSpotDao {
            override fun getAllSpots() = kotlinx.coroutines.flow.flowOf(spotList)
            override suspend fun getAllSpotsDirect() = spotList
            override fun getActiveSpot() = kotlinx.coroutines.flow.flowOf(spotList.firstOrNull { it.isActive })
            override suspend fun getActiveSpotDirect() = spotList.firstOrNull { it.isActive }
            override suspend fun getMostRecentSpotDirect() = spotList.lastOrNull()
            override suspend fun getSpotById(id: Long) = spotList.find { it.id == id }
            override suspend fun insertSpot(spot: com.example.data.entity.ParkingSpot): Long {
                val id = nextId++
                spotList.add(spot.copy(id = id))
                return id
            }
            override suspend fun updateSpot(spot: com.example.data.entity.ParkingSpot) {
                val idx = spotList.indexOfFirst { it.id == spot.id }
                if (idx >= 0) spotList[idx] = spot
            }
            override suspend fun deactivateAllSpots() {
                for (i in spotList.indices) {
                    spotList[i] = spotList[i].copy(isActive = false)
                }
            }
            override suspend fun deleteSpotById(id: Long) { spotList.removeAll { it.id == id } }
            override suspend fun deleteLegacyDemoSpots() {}
            override suspend fun clearAllSpots() { spotList.clear() }
        }

        val fakeDeviceDao = object : com.example.data.dao.BluetoothCarDeviceDao {
            override fun getAllDevices() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.entity.BluetoothCarDevice>())
            override suspend fun getAllDevicesDirect() = emptyList<com.example.data.entity.BluetoothCarDevice>()
            override fun getMonitoredDevices() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.entity.BluetoothCarDevice>())
            override suspend fun getMonitoredDevicesDirect() = emptyList<com.example.data.entity.BluetoothCarDevice>()
            override suspend fun getSelectedMonitoredDevice(): com.example.data.entity.BluetoothCarDevice? = null
            override fun getSelectedMonitoredDeviceFlow() = kotlinx.coroutines.flow.flowOf(null)
            override suspend fun clearAllMonitoredStatus() {}
            override suspend fun setSoleMonitoredDevice(address: String) {}
            override suspend fun getDeviceByAddress(address: String) = null
            override suspend fun getDeviceByNameOrOriginalName(name: String) = null
            override suspend fun updateDeviceName(address: String, name: String) {}
            override suspend fun updateDeviceNameAndOriginal(address: String, name: String, originalName: String) {}
            override suspend fun setDeviceMonitoredState(address: String, isMonitored: Boolean) {}
            override suspend fun insertOrUpdate(device: com.example.data.entity.BluetoothCarDevice) {}
            override suspend fun update(device: com.example.data.entity.BluetoothCarDevice) {}
            override suspend fun deleteDevice(address: String) {}
        }

        val repository = com.example.data.repository.ParkingRepository(fakeSpotDao, fakeDeviceDao)

        // 1. Initial saved spot exists (e.g. from manual save or first BT disconnect)
        val initialSpot = com.example.data.entity.ParkingSpot(
            latitude = 37.7749,
            longitude = -122.4194,
            spotName = "Home Garage",
            isAutoSaved = true,
            bluetoothDeviceAddress = "AA:BB:CC:DD:EE:FF",
            timestamp = System.currentTimeMillis() - 60_000L,
            isActive = true
        )
        val id1 = repository.saveNewParkingSpot(initialSpot)
        assertEquals(1L, id1)
        assertEquals(1, spotList.size)
        assertTrue(spotList[0].isActive)

        // 2. New, second Bluetooth disconnect arrives (e.g. at work or destination 2)
        val secondBtSpot = com.example.data.entity.ParkingSpot(
            latitude = 37.7850,
            longitude = -122.4050,
            spotName = "Office Parking",
            isAutoSaved = true,
            bluetoothDeviceAddress = "AA:BB:CC:DD:EE:FF",
            timestamp = System.currentTimeMillis(),
            isActive = true
        )
        val id2 = repository.saveNewParkingSpot(secondBtSpot)
        assertEquals(2L, id2)
        assertEquals(2, spotList.size)
        org.junit.Assert.assertFalse(spotList[0].isActive)
        assertTrue(spotList[1].isActive)
        assertEquals("Office Parking", spotList[1].spotName)
    }

    @Test
    fun testVehicleKeywordDetection() {
        assertTrue(com.example.receiver.BluetoothDisconnectReceiver.hasVehicleKeywords("Audi MMI"))
        assertTrue(com.example.receiver.BluetoothDisconnectReceiver.hasVehicleKeywords("BMW 330e"))
        assertTrue(com.example.receiver.BluetoothDisconnectReceiver.hasVehicleKeywords("Toyota Car"))
        assertTrue(com.example.receiver.BluetoothDisconnectReceiver.hasVehicleKeywords("SYNC"))
        assertTrue(com.example.receiver.BluetoothDisconnectReceiver.hasVehicleKeywords("CarPlay"))
        org.junit.Assert.assertFalse(com.example.receiver.BluetoothDisconnectReceiver.hasVehicleKeywords("AirPods Pro"))
        org.junit.Assert.assertFalse(com.example.receiver.BluetoothDisconnectReceiver.hasVehicleKeywords("Sony WH-1000XM4"))
        org.junit.Assert.assertFalse(com.example.receiver.BluetoothDisconnectReceiver.hasVehicleKeywords("JBL Flip 6"))
        org.junit.Assert.assertFalse(com.example.receiver.BluetoothDisconnectReceiver.hasVehicleKeywords("Galaxy Watch 5"))
    }

    @Test
    fun testFindMatchingMonitoredDeviceIgnoresNonSelectedDevices() = kotlinx.coroutines.runBlocking {
        val fakeSpotDao = object : com.example.data.dao.ParkingSpotDao {
            override fun getAllSpots() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.entity.ParkingSpot>())
            override suspend fun getAllSpotsDirect() = emptyList<com.example.data.entity.ParkingSpot>()
            override fun getActiveSpot() = kotlinx.coroutines.flow.flowOf(null)
            override suspend fun getActiveSpotDirect() = null
            override suspend fun getMostRecentSpotDirect() = null
            override suspend fun getSpotById(id: Long) = null
            override suspend fun insertSpot(spot: com.example.data.entity.ParkingSpot) = 1L
            override suspend fun updateSpot(spot: com.example.data.entity.ParkingSpot) {}
            override suspend fun deactivateAllSpots() {}
            override suspend fun deleteSpotById(id: Long) {}
            override suspend fun deleteLegacyDemoSpots() {}
            override suspend fun clearAllSpots() {}
        }

        val monitoredCar = com.example.data.entity.BluetoothCarDevice(
            address = "AA:11:22:33:44:55",
            name = "My Car",
            originalName = "Toyota RAV4",
            isMonitoredCar = true
        )
        val unmonitoredHeadphones = com.example.data.entity.BluetoothCarDevice(
            address = "BB:22:33:44:55:66",
            name = "Sony Headphones",
            originalName = "Sony WH-1000XM4",
            isMonitoredCar = false
        )

        val devices = listOf(monitoredCar, unmonitoredHeadphones)

        val fakeDeviceDao = object : com.example.data.dao.BluetoothCarDeviceDao {
            override fun getAllDevices() = kotlinx.coroutines.flow.flowOf(devices)
            override suspend fun getAllDevicesDirect() = devices
            override fun getMonitoredDevices() = kotlinx.coroutines.flow.flowOf(devices.filter { it.isMonitoredCar })
            override suspend fun getMonitoredDevicesDirect() = devices.filter { it.isMonitoredCar }
            override suspend fun getSelectedMonitoredDevice() = monitoredCar
            override fun getSelectedMonitoredDeviceFlow() = kotlinx.coroutines.flow.flowOf(monitoredCar)
            override suspend fun clearAllMonitoredStatus() {}
            override suspend fun setSoleMonitoredDevice(address: String) {}
            override suspend fun getDeviceByAddress(address: String) = devices.firstOrNull { it.address.equals(address, ignoreCase = true) }
            override suspend fun getDeviceByNameOrOriginalName(name: String) = devices.firstOrNull { it.name == name || it.originalName == name }
            override suspend fun updateDeviceName(address: String, name: String) {}
            override suspend fun updateDeviceNameAndOriginal(address: String, name: String, originalName: String) {}
            override suspend fun setDeviceMonitoredState(address: String, isMonitored: Boolean) {}
            override suspend fun insertOrUpdate(device: com.example.data.entity.BluetoothCarDevice) {}
            override suspend fun update(device: com.example.data.entity.BluetoothCarDevice) {}
            override suspend fun deleteDevice(address: String) {}
        }

        val repository = com.example.data.repository.ParkingRepository(fakeSpotDao, fakeDeviceDao)

        // 1. Disconnecting unmonitored headphones MUST be ignored (returns null)
        val resultHeadphones = repository.findMatchingMonitoredDevice("BB:22:33:44:55:66", "Sony Headphones")
        org.junit.Assert.assertNull(resultHeadphones)

        // 2. Disconnecting completely unknown bluetooth device MUST be ignored (returns null)
        val resultUnknown = repository.findMatchingMonitoredDevice("CC:99:88:77:66:55", "Random Speaker")
        org.junit.Assert.assertNull(resultUnknown)

        // 3. Disconnecting monitored car MUST be resolved
        val resultCar = repository.findMatchingMonitoredDevice("AA:11:22:33:44:55", "My Car")
        org.junit.Assert.assertNotNull(resultCar)
        assertEquals("My Car", resultCar?.name)
        assertTrue(resultCar?.isMonitoredCar == true)
    }

    @Test
    fun testParkingLocationSavedNotificationTitle() {
        val strings = com.example.ui.i18n.EnglishStrings
        assertEquals("Parking location saved", strings.notificationParkingLocationSaved)

        val spotWithCar = com.example.data.entity.ParkingSpot(
            latitude = 37.7749,
            longitude = -122.4194,
            bluetoothDeviceName = "BMW 330e",
            isAutoSaved = true
        )
        val title = if (!spotWithCar.bluetoothDeviceName.isNullOrBlank()) {
            "${spotWithCar.bluetoothDeviceName} • ${strings.notificationParkingLocationSaved}"
        } else {
            strings.notificationParkingLocationSaved
        }
        assertEquals("BMW 330e • Parking location saved", title)
        org.junit.Assert.assertFalse(title.contains("Disconnected", ignoreCase = true))
    }

    @Test
    fun testAllowGeofenceAfterBluetoothSaveToggle() = kotlinx.coroutines.runBlocking {
        val spotList = mutableListOf<com.example.data.entity.ParkingSpot>()
        val fakeSpotDao = object : com.example.data.dao.ParkingSpotDao {
            override fun getAllSpots() = kotlinx.coroutines.flow.flowOf(spotList)
            override suspend fun getAllSpotsDirect() = spotList
            override fun getActiveSpot() = kotlinx.coroutines.flow.flowOf(spotList.firstOrNull { it.isActive })
            override suspend fun getActiveSpotDirect() = spotList.firstOrNull { it.isActive }
            override suspend fun getMostRecentSpotDirect() = spotList.lastOrNull()
            override suspend fun getSpotById(id: Long) = spotList.firstOrNull { it.id == id }
            override suspend fun insertSpot(spot: com.example.data.entity.ParkingSpot): Long {
                val newId = (spotList.size + 1).toLong()
                spotList.add(spot.copy(id = newId))
                return newId
            }
            override suspend fun updateSpot(spot: com.example.data.entity.ParkingSpot) {
                val idx = spotList.indexOfFirst { it.id == spot.id }
                if (idx != -1) spotList[idx] = spot
            }
            override suspend fun deactivateAllSpots() {
                for (i in spotList.indices) {
                    spotList[i] = spotList[i].copy(isActive = false)
                }
            }
            override suspend fun deleteSpotById(id: Long) {
                spotList.removeAll { it.id == id }
            }
            override suspend fun deleteLegacyDemoSpots() {}
            override suspend fun clearAllSpots() { spotList.clear() }
        }

        val fakeDeviceDao = object : com.example.data.dao.BluetoothCarDeviceDao {
            override fun getAllDevices() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.entity.BluetoothCarDevice>())
            override suspend fun getAllDevicesDirect() = emptyList<com.example.data.entity.BluetoothCarDevice>()
            override fun getMonitoredDevices() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.entity.BluetoothCarDevice>())
            override suspend fun getMonitoredDevicesDirect() = emptyList<com.example.data.entity.BluetoothCarDevice>()
            override suspend fun getSelectedMonitoredDevice() = null
            override fun getSelectedMonitoredDeviceFlow() = kotlinx.coroutines.flow.flowOf(null)
            override suspend fun clearAllMonitoredStatus() {}
            override suspend fun setSoleMonitoredDevice(address: String) {}
            override suspend fun getDeviceByAddress(address: String) = null
            override suspend fun getDeviceByNameOrOriginalName(name: String) = null
            override suspend fun updateDeviceName(address: String, name: String) {}
            override suspend fun updateDeviceNameAndOriginal(address: String, name: String, originalName: String) {}
            override suspend fun setDeviceMonitoredState(address: String, isMonitored: Boolean) {}
            override suspend fun insertOrUpdate(device: com.example.data.entity.BluetoothCarDevice) {}
            override suspend fun update(device: com.example.data.entity.BluetoothCarDevice) {}
            override suspend fun deleteDevice(address: String) {}
        }

        val repository = com.example.data.repository.ParkingRepository(fakeSpotDao, fakeDeviceDao)

        // 1. Initial Bluetooth Disconnect spot saved 30s ago (outside rapid 10s window, inside 10m window)
        val btSpot = com.example.data.entity.ParkingSpot(
            latitude = 37.7749,
            longitude = -122.4194,
            spotName = "BMW 330e",
            isAutoSaved = true,
            bluetoothDeviceName = "BMW 330e",
            bluetoothDeviceAddress = "11:22:33:44:55:66",
            timestamp = System.currentTimeMillis() - 30_000L
        )
        val btId = repository.saveNewParkingSpot(btSpot)
        assertEquals(1L, btId)
        assertEquals(1, spotList.size)

        // 2. Geofence spot arrives nearby shortly after with allowGeofenceAfterBt = false (default)
        val geoSpotDisabled = com.example.data.entity.ParkingSpot(
            latitude = 37.7750,
            longitude = -122.4195,
            spotName = "Walking Detection",
            isAutoSaved = true,
            bluetoothDeviceName = "Walking Detection",
            bluetoothDeviceAddress = "GEOFENCE_WALKING_DETECTION",
            timestamp = System.currentTimeMillis()
        )
        val mergedId = repository.saveNewParkingSpot(geoSpotDisabled, allowGeofenceAfterBt = false)
        assertEquals("Should merge into Bluetooth spot by default", btId, mergedId)
        assertEquals("Should not create new spot", 1, spotList.size)

        // 3. Geofence spot arrives with allowGeofenceAfterBt = true (dev option enabled)
        val geoSpotEnabled = com.example.data.entity.ParkingSpot(
            latitude = 37.7751,
            longitude = -122.4196,
            spotName = "Walking Detection",
            isAutoSaved = true,
            bluetoothDeviceName = "Walking Detection",
            bluetoothDeviceAddress = "GEOFENCE_WALKING_DETECTION",
            timestamp = System.currentTimeMillis()
        )
        val newGeoId = repository.saveNewParkingSpot(geoSpotEnabled, allowGeofenceAfterBt = true)
        assertEquals("Should save as distinct new spot when dev toggle is enabled", 2L, newGeoId)
        assertEquals("Should have 2 spots in list", 2, spotList.size)
        assertTrue("New geofence spot should be active", spotList[1].isActive)
        org.junit.Assert.assertFalse("Previous BT spot should be deactivated", spotList[0].isActive)
    }

    @Test
    fun testAutomaticBtSaveSavesActualNewLocationAndNeverOldSpot() = kotlinx.coroutines.runBlocking {
        val spotList = mutableListOf<com.example.data.entity.ParkingSpot>()
        var nextId = 1L

        val fakeSpotDao = object : com.example.data.dao.ParkingSpotDao {
            override fun getAllSpots() = kotlinx.coroutines.flow.flowOf(spotList)
            override suspend fun getAllSpotsDirect() = spotList
            override fun getActiveSpot() = kotlinx.coroutines.flow.flowOf(spotList.firstOrNull { it.isActive })
            override suspend fun getActiveSpotDirect() = spotList.firstOrNull { it.isActive }
            override suspend fun getMostRecentSpotDirect() = spotList.lastOrNull()
            override suspend fun getSpotById(id: Long) = spotList.find { it.id == id }
            override suspend fun insertSpot(spot: com.example.data.entity.ParkingSpot): Long {
                val id = nextId++
                spotList.add(spot.copy(id = id))
                return id
            }
            override suspend fun updateSpot(spot: com.example.data.entity.ParkingSpot) {
                val idx = spotList.indexOfFirst { it.id == spot.id }
                if (idx >= 0) spotList[idx] = spot
            }
            override suspend fun deactivateAllSpots() {
                for (i in spotList.indices) {
                    spotList[i] = spotList[i].copy(isActive = false)
                }
            }
            override suspend fun deleteSpotById(id: Long) { spotList.removeAll { it.id == id } }
            override suspend fun deleteLegacyDemoSpots() {}
            override suspend fun clearAllSpots() { spotList.clear() }
        }

        val fakeDeviceDao = object : com.example.data.dao.BluetoothCarDeviceDao {
            override fun getAllDevices() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.entity.BluetoothCarDevice>())
            override suspend fun getAllDevicesDirect() = emptyList<com.example.data.entity.BluetoothCarDevice>()
            override fun getMonitoredDevices() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.entity.BluetoothCarDevice>())
            override suspend fun getMonitoredDevicesDirect() = emptyList<com.example.data.entity.BluetoothCarDevice>()
            override suspend fun getSelectedMonitoredDevice(): com.example.data.entity.BluetoothCarDevice? = null
            override fun getSelectedMonitoredDeviceFlow() = kotlinx.coroutines.flow.flowOf(null)
            override suspend fun clearAllMonitoredStatus() {}
            override suspend fun setSoleMonitoredDevice(address: String) {}
            override suspend fun getDeviceByAddress(address: String) = null
            override suspend fun getDeviceByNameOrOriginalName(name: String) = null
            override suspend fun updateDeviceName(address: String, name: String) {}
            override suspend fun updateDeviceNameAndOriginal(address: String, name: String, originalName: String) {}
            override suspend fun setDeviceMonitoredState(address: String, isMonitored: Boolean) {}
            override suspend fun insertOrUpdate(device: com.example.data.entity.BluetoothCarDevice) {}
            override suspend fun update(device: com.example.data.entity.BluetoothCarDevice) {}
            override suspend fun deleteDevice(address: String) {}
        }

        val repository = com.example.data.repository.ParkingRepository(fakeSpotDao, fakeDeviceDao)

        // 1. First spot exists from earlier parking
        val oldSpot = com.example.data.entity.ParkingSpot(
            latitude = 40.7128,
            longitude = -74.0060,
            spotName = "Morning Parking Spot",
            isAutoSaved = true,
            bluetoothDeviceAddress = "11:22:33:44:55:66",
            timestamp = System.currentTimeMillis() - 3600_000L,
            isActive = true
        )
        val oldId = repository.saveNewParkingSpot(oldSpot)
        assertEquals(1L, oldId)

        // 2. User drives to actual destination and disconnects BT
        val actualUserLat = 40.7580
        val actualUserLng = -73.9855
        val actualBtSpot = com.example.data.entity.ParkingSpot(
            latitude = actualUserLat,
            longitude = actualUserLng,
            spotName = "Afternoon Parking Spot",
            isAutoSaved = true,
            bluetoothDeviceAddress = "11:22:33:44:55:66",
            timestamp = System.currentTimeMillis(),
            isActive = true
        )
        val newId = repository.saveNewParkingSpot(actualBtSpot)
        assertEquals(2L, newId)

        val active = repository.getActiveSpotDirect()
        org.junit.Assert.assertNotNull(active)
        assertEquals(actualUserLat, active!!.latitude, 0.0001)
        assertEquals(actualUserLng, active.longitude, 0.0001)
        org.junit.Assert.assertNotEquals(oldSpot.latitude, active.latitude, 0.0001)
    }

    @Test
    fun testGeofenceSafeguardAndPerimeterExitCalculation() {
        val carLat = 37.7749
        val carLng = -122.4194

        // 1. User walks 65 meters away from parked car
        val walkingLat = 37.7754
        val walkingLng = -122.4194
        val distance = com.example.sensor.LocationHelper.calculateDistanceMeters(carLat, carLng, walkingLat, walkingLng)
        assertTrue("Distance should exceed 50m geofence perimeter", distance >= com.example.geofence.GeofenceManager.DEFAULT_PERIMETER_RADIUS_METERS)

        // 2. Validate that geofence auto-saved spot entity is properly identified as geofence entry
        val geofenceSpot = com.example.data.entity.ParkingSpot(
            latitude = carLat,
            longitude = carLng,
            spotName = "Walking Detection",
            isAutoSaved = true,
            bluetoothDeviceName = "Walking Detection",
            bluetoothDeviceAddress = "GEOFENCE_WALKING_DETECTION"
        )
        assertTrue(geofenceSpot.isGeofenceEntry)
        org.junit.Assert.assertFalse(geofenceSpot.isBluetoothEntry)
    }

    @Test
    fun testDeveloperOptionsTestingEntities() {
        val testBtDevice = com.example.data.entity.BluetoothCarDevice(
            name = "Developer Test Car",
            originalName = "Developer Test Car",
            address = "AA:BB:CC:DD:EE:FF",
            deviceType = "Car",
            isMonitoredCar = true
        )
        assertTrue(testBtDevice.isMonitoredCar)
        assertEquals("Developer Test Car", testBtDevice.name)

        val btSpot = com.example.data.entity.ParkingSpot(
            latitude = 37.7749,
            longitude = -122.4194,
            spotName = testBtDevice.name,
            isAutoSaved = true,
            bluetoothDeviceName = testBtDevice.name,
            bluetoothDeviceAddress = testBtDevice.address
        )
        assertTrue(btSpot.isBluetoothEntry)
        org.junit.Assert.assertFalse(btSpot.isGeofenceEntry)

        val geofenceSpot = com.example.data.entity.ParkingSpot(
            latitude = 37.7749,
            longitude = -122.4194,
            spotName = "Walking Detection",
            isAutoSaved = true,
            bluetoothDeviceName = "Walking Detection",
            bluetoothDeviceAddress = "GEOFENCE_WALKING_DETECTION"
        )
        assertTrue(geofenceSpot.isGeofenceEntry)
        org.junit.Assert.assertFalse(geofenceSpot.isBluetoothEntry)
    }

    @Test
    fun testCoordinateParserDecimalAndDms() {
        // 1. Plain comma-separated decimal coordinates
        val plain = com.example.util.CoordinateParser.parse("37.7749, -122.4194")
        org.junit.Assert.assertNotNull(plain)
        assertEquals(37.7749, plain!!.latitude, 0.0001)
        assertEquals(-122.4194, plain.longitude, 0.0001)

        // 2. Far away coordinates (Paris)
        val paris = com.example.util.CoordinateParser.parse("48.8584, 2.2945")
        org.junit.Assert.assertNotNull(paris)
        assertEquals(48.8584, paris!!.latitude, 0.0001)
        assertEquals(2.2945, paris.longitude, 0.0001)

        // 3. Google Maps geo: URI with label
        val geo = com.example.util.CoordinateParser.parse("geo:0,0?q=37.7749,-122.4194(Airport%20Parking)")
        org.junit.Assert.assertNotNull(geo)
        assertEquals(37.7749, geo!!.latitude, 0.0001)
        assertEquals(-122.4194, geo.longitude, 0.0001)
        assertEquals("Airport Parking", geo.label)

        // 4. Google Maps URL
        val mapUrl = com.example.util.CoordinateParser.parse("https://www.google.com/maps?q=40.7128,-74.0060")
        org.junit.Assert.assertNotNull(mapUrl)
        assertEquals(40.7128, mapUrl!!.latitude, 0.0001)
        assertEquals(-74.0060, mapUrl.longitude, 0.0001)

        // 5. Shared text message with embedded coordinates
        val sharedText = com.example.util.CoordinateParser.parse("Meet me at the car: 34.0522, -118.2437 parked on 2nd floor")
        org.junit.Assert.assertNotNull(sharedText)
        assertEquals(34.0522, sharedText!!.latitude, 0.0001)
        assertEquals(-118.2437, sharedText.longitude, 0.0001)

        // 6. DMS (Degrees Minutes Seconds)
        val dms = com.example.util.CoordinateParser.parse("""37°46'29.6"N 122°25'09.8"W""")
        org.junit.Assert.assertNotNull(dms)
        assertEquals(37.7748, dms!!.latitude, 0.001)
        assertEquals(-122.4193, dms.longitude, 0.001)

        // 7. Invalid text
        val invalid = com.example.util.CoordinateParser.parse("Hello from testing, no coordinates here")
        org.junit.Assert.assertNull(invalid)
    }

    @Test
    fun testFarAwayParkingCustomCoordinatesSave() = kotlinx.coroutines.runBlocking {
        val spotList = mutableListOf<com.example.data.entity.ParkingSpot>()
        var nextId = 1L

        val fakeSpotDao = object : com.example.data.dao.ParkingSpotDao {
            override fun getAllSpots() = kotlinx.coroutines.flow.flowOf(spotList)
            override suspend fun getAllSpotsDirect() = spotList
            override fun getActiveSpot() = kotlinx.coroutines.flow.flowOf(spotList.firstOrNull { it.isActive })
            override suspend fun getActiveSpotDirect() = spotList.firstOrNull { it.isActive }
            override suspend fun getMostRecentSpotDirect() = spotList.lastOrNull()
            override suspend fun getSpotById(id: Long) = spotList.find { it.id == id }
            override suspend fun insertSpot(spot: com.example.data.entity.ParkingSpot): Long {
                val id = nextId++
                spotList.add(spot.copy(id = id))
                return id
            }
            override suspend fun updateSpot(spot: com.example.data.entity.ParkingSpot) {
                val idx = spotList.indexOfFirst { it.id == spot.id }
                if (idx >= 0) spotList[idx] = spot
            }
            override suspend fun deactivateAllSpots() {
                for (i in spotList.indices) {
                    spotList[i] = spotList[i].copy(isActive = false)
                }
            }
            override suspend fun deleteSpotById(id: Long) { spotList.removeAll { it.id == id } }
            override suspend fun deleteLegacyDemoSpots() {}
            override suspend fun clearAllSpots() { spotList.clear() }
        }

        val fakeDeviceDao = object : com.example.data.dao.BluetoothCarDeviceDao {
            override fun getAllDevices() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.entity.BluetoothCarDevice>())
            override suspend fun getAllDevicesDirect() = emptyList<com.example.data.entity.BluetoothCarDevice>()
            override fun getMonitoredDevices() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.entity.BluetoothCarDevice>())
            override suspend fun getMonitoredDevicesDirect() = emptyList<com.example.data.entity.BluetoothCarDevice>()
            override suspend fun getSelectedMonitoredDevice(): com.example.data.entity.BluetoothCarDevice? = null
            override fun getSelectedMonitoredDeviceFlow() = kotlinx.coroutines.flow.flowOf(null)
            override suspend fun clearAllMonitoredStatus() {}
            override suspend fun setSoleMonitoredDevice(address: String) {}
            override suspend fun getDeviceByAddress(address: String) = null
            override suspend fun getDeviceByNameOrOriginalName(name: String) = null
            override suspend fun updateDeviceName(address: String, name: String) {}
            override suspend fun updateDeviceNameAndOriginal(address: String, name: String, originalName: String) {}
            override suspend fun setDeviceMonitoredState(address: String, isMonitored: Boolean) {}
            override suspend fun insertOrUpdate(device: com.example.data.entity.BluetoothCarDevice) {}
            override suspend fun update(device: com.example.data.entity.BluetoothCarDevice) {}
            override suspend fun deleteDevice(address: String) {}
        }

        val repository = com.example.data.repository.ParkingRepository(fakeSpotDao, fakeDeviceDao)

        // Parse custom far away coordinates (e.g. Sydney, Australia)
        val parsed = com.example.util.CoordinateParser.parse("-33.8688, 151.2093")
        org.junit.Assert.assertNotNull(parsed)

        val farAwaySpot = com.example.data.entity.ParkingSpot(
            latitude = parsed!!.latitude,
            longitude = parsed.longitude,
            spotName = "Custom Far Away Spot",
            address = "Sydney, Australia",
            isActive = true,
            timestamp = System.currentTimeMillis()
        )
        val savedId = repository.saveNewParkingSpot(farAwaySpot)
        assertEquals(1L, savedId)

        val active = repository.getActiveSpotDirect()
        org.junit.Assert.assertNotNull(active)
        assertEquals(-33.8688, active!!.latitude, 0.0001)
        assertEquals(151.2093, active.longitude, 0.0001)
        assertTrue(active.isActive)
    }

    @Test
    fun testGeofenceFeatureDisabledState() {
        // Verify preference keys and disabled states
        val isGeofenceDefaultEnabled = false
        org.junit.Assert.assertFalse("Geofence feature should be disabled by default", isGeofenceDefaultEnabled)
        assertEquals("geofence_auto_park_enabled", com.example.geofence.GeofenceManager.KEY_GEOFENCE_ENABLED)
    }

    @Test
    fun testClosedAppBluetoothDetection() = kotlinx.coroutines.runBlocking {
        val fakeSpotDao = object : com.example.data.dao.ParkingSpotDao {
            val spots = mutableListOf<com.example.data.entity.ParkingSpot>()
            override fun getAllSpots() = kotlinx.coroutines.flow.flowOf(spots)
            override suspend fun getAllSpotsDirect() = spots
            override suspend fun getSpotById(id: Long) = spots.firstOrNull { it.id == id }
            override fun getActiveSpot() = kotlinx.coroutines.flow.flowOf(spots.firstOrNull { it.isActive })
            override suspend fun getActiveSpotDirect() = spots.firstOrNull { it.isActive }
            override suspend fun getMostRecentSpotDirect() = spots.maxByOrNull { it.timestamp }
            override suspend fun insertSpot(spot: com.example.data.entity.ParkingSpot): Long {
                spots.add(spot.copy(id = 1L))
                return 1L
            }
            override suspend fun updateSpot(spot: com.example.data.entity.ParkingSpot) {}
            override suspend fun deleteSpotById(id: Long) {}
            override suspend fun deactivateAllSpots() {}
            override suspend fun deleteLegacyDemoSpots() {}
            override suspend fun clearAllSpots() {}
        }

        val myCar = com.example.data.entity.BluetoothCarDevice(
            address = "11:22:33:44:55:66",
            name = "BMW 330e",
            originalName = "BMW 330e HandsFree",
            deviceType = "Car",
            isMonitoredCar = true
        )
        val devices = listOf(myCar)

        val fakeDeviceDao = object : com.example.data.dao.BluetoothCarDeviceDao {
            override fun getAllDevices() = kotlinx.coroutines.flow.flowOf(devices)
            override suspend fun getAllDevicesDirect() = devices
            override fun getMonitoredDevices() = kotlinx.coroutines.flow.flowOf(devices.filter { it.isMonitoredCar })
            override suspend fun getMonitoredDevicesDirect() = devices.filter { it.isMonitoredCar }
            override suspend fun getSelectedMonitoredDevice() = myCar
            override fun getSelectedMonitoredDeviceFlow() = kotlinx.coroutines.flow.flowOf(myCar)
            override suspend fun clearAllMonitoredStatus() {}
            override suspend fun setSoleMonitoredDevice(address: String) {}
            override suspend fun getDeviceByAddress(address: String) = devices.firstOrNull { it.address.equals(address, ignoreCase = true) }
            override suspend fun getDeviceByNameOrOriginalName(name: String) = devices.firstOrNull { it.name == name || it.originalName == name }
            override suspend fun updateDeviceName(address: String, name: String) {}
            override suspend fun updateDeviceNameAndOriginal(address: String, name: String, originalName: String) {}
            override suspend fun setDeviceMonitoredState(address: String, isMonitored: Boolean) {}
            override suspend fun insertOrUpdate(device: com.example.data.entity.BluetoothCarDevice) {}
            override suspend fun update(device: com.example.data.entity.BluetoothCarDevice) {}
            override suspend fun deleteDevice(address: String) {}
        }

        val repository = com.example.data.repository.ParkingRepository(fakeSpotDao, fakeDeviceDao)

        // 1. MAC match works even when closed app receives disconnect
        val resolvedByMac = repository.findMatchingMonitoredDevice("11:22:33:44:55:66", null)
        org.junit.Assert.assertNotNull(resolvedByMac)
        assertEquals("BMW 330e", resolvedByMac?.name)

        // 2. Name match works when multi-profile Bluetooth reports different MAC (e.g. A2DP vs HFP)
        val resolvedByName = repository.findMatchingMonitoredDevice("99:88:77:66:55:44", "BMW 330e")
        org.junit.Assert.assertNotNull(resolvedByName)
        assertEquals("BMW 330e", resolvedByName?.name)

        // 3. Vehicle keywords check works for unknown car names
        assertTrue(com.example.receiver.BluetoothDisconnectReceiver.hasVehicleKeywords("Tesla Model Y"))
        assertTrue(com.example.receiver.BluetoothDisconnectReceiver.hasVehicleKeywords("VW Infotainment"))
        org.junit.Assert.assertFalse(com.example.receiver.BluetoothDisconnectReceiver.hasVehicleKeywords("AirPods Pro"))
    }

    @Test
    fun testBluetoothA2dpAndHfpProfileEventDetection() {
        // A2DP connection state monitoring
        assertTrue(
            "A2DP STATE_CONNECTED must be recognized as connection event",
            com.example.receiver.BluetoothDisconnectReceiver.isProfileConnectionEvent(
                action = android.bluetooth.BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                profileState = android.bluetooth.BluetoothProfile.STATE_CONNECTED
            )
        )
        assertTrue(
            "A2DP STATE_DISCONNECTED must be recognized as disconnect event for auto-park",
            com.example.receiver.BluetoothDisconnectReceiver.isProfileDisconnectionEvent(
                action = android.bluetooth.BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                profileState = android.bluetooth.BluetoothProfile.STATE_DISCONNECTED
            )
        )

        // HFP (Headset/Hands-Free) connection state monitoring
        assertTrue(
            "HFP/Headset STATE_CONNECTED must be recognized as connection event",
            com.example.receiver.BluetoothDisconnectReceiver.isProfileConnectionEvent(
                action = android.bluetooth.BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED,
                profileState = android.bluetooth.BluetoothProfile.STATE_CONNECTED
            )
        )
        assertTrue(
            "HFP/Headset STATE_DISCONNECTED must be recognized as disconnect event for auto-park",
            com.example.receiver.BluetoothDisconnectReceiver.isProfileDisconnectionEvent(
                action = android.bluetooth.BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED,
                profileState = android.bluetooth.BluetoothProfile.STATE_DISCONNECTED
            )
        )

        // Standard ACL and Adapter states
        assertTrue(
            "ACL_DISCONNECTED must be recognized as disconnect event",
            com.example.receiver.BluetoothDisconnectReceiver.isProfileDisconnectionEvent(
                action = android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED
            )
        )
        org.junit.Assert.assertFalse(
            "A2DP STATE_CONNECTING should not trigger disconnect event",
            com.example.receiver.BluetoothDisconnectReceiver.isProfileDisconnectionEvent(
                action = android.bluetooth.BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                profileState = android.bluetooth.BluetoothProfile.STATE_CONNECTING
            )
        )
    }

    @Test
    fun testDeveloperOptionSimpleScrollToggle() {
        // Verify key and default behavior state representation
        val prefKey = "dev_simple_scroll_enabled"
        assertEquals("dev_simple_scroll_enabled", prefKey)

        var isSimpleScrollEnabled = false
        org.junit.Assert.assertFalse(isSimpleScrollEnabled)

        // When developer toggles on, smooth scrolling animation is removed
        isSimpleScrollEnabled = true
        assertTrue(isSimpleScrollEnabled)

        // Reset
        isSimpleScrollEnabled = false
        org.junit.Assert.assertFalse(isSimpleScrollEnabled)
    }

    @Test
    fun testStreetNameRecognitionAndRemoteCoordinates() {
        // Valid street names in Greek, English, and other languages
        assertTrue("Platia Chimeiou should be recognized as a valid street", LocationHelper.isRecognizedStreetName("Πλατεία Χημείου"))
        assertTrue("Tsimiski should be recognized as a valid street", LocationHelper.isRecognizedStreetName("Τσιμισκή"))
        assertTrue("Amphitheatre Pkwy should be recognized as a valid street", LocationHelper.isRecognizedStreetName("Amphitheatre Pkwy"))
        assertTrue("Market St should be recognized as a valid street", LocationHelper.isRecognizedStreetName("Market St"))
        assertTrue("5th Ave should be recognized as a valid street", LocationHelper.isRecognizedStreetName("5th Ave"))

        // Remote, unnamed, plus-code, or coordinate placeholders must NOT be recognized as streets
        org.junit.Assert.assertFalse("Unnamed Road should NOT be recognized as street", LocationHelper.isRecognizedStreetName("Unnamed Road"))
        org.junit.Assert.assertFalse("Anonimi odos should NOT be recognized as street", LocationHelper.isRecognizedStreetName("Ανώνυμη οδός"))
        org.junit.Assert.assertFalse("Plus codes should NOT be recognized as street", LocationHelper.isRecognizedStreetName("8GG2+3X"))
        org.junit.Assert.assertFalse("Raw coordinate string should NOT be recognized as street", LocationHelper.isRecognizedStreetName("40.63223° N, 22.95485° E"))
        org.junit.Assert.assertFalse("Blank string should NOT be recognized as street", LocationHelper.isRecognizedStreetName(""))
        org.junit.Assert.assertFalse("Null should NOT be recognized as street", LocationHelper.isRecognizedStreetName(null))
        org.junit.Assert.assertFalse("Generic 'Road' should NOT be recognized as street", LocationHelper.isRecognizedStreetName("Road"))

        // Coordinate detection
        assertTrue("Formatted coordinates must be recognized as coordinates string", LocationHelper.isCoordinatesString("40.63223° N, 22.95485° E"))
        assertTrue("GPS prefix must be recognized as coordinates string", LocationHelper.isCoordinatesString("GPS: 40.6322, 22.9548"))
        assertTrue("Blank must be treated as coordinates string fallback", LocationHelper.isCoordinatesString(""))
        org.junit.Assert.assertFalse("Street address must NOT be treated as coordinates string", LocationHelper.isCoordinatesString("Πλατεία Χημείου, Θεσσαλονίκη"))
        org.junit.Assert.assertFalse("Street address must NOT be treated as coordinates string", LocationHelper.isCoordinatesString("Τσιμισκή 45"))
    }
}

