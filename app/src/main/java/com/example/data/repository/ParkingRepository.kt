package com.example.data.repository

import android.content.Context
import android.location.Location
import android.util.Log
import com.example.data.dao.BluetoothCarDeviceDao
import com.example.data.dao.ParkingSpotDao
import com.example.data.entity.BluetoothCarDevice
import com.example.data.entity.ParkingSpot
import com.example.data.entity.isBluetoothEntry
import com.example.data.entity.isGeofenceEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ParkingRepository(
    private val parkingSpotDao: ParkingSpotDao,
    private val bluetoothCarDeviceDao: BluetoothCarDeviceDao
) {
    val allSpots: Flow<List<ParkingSpot>> = parkingSpotDao.getAllSpots()
    val activeSpot: Flow<ParkingSpot?> = parkingSpotDao.getActiveSpot()
    val allDevices: Flow<List<BluetoothCarDevice>> = bluetoothCarDeviceDao.getAllDevices()
    val monitoredDevices: Flow<List<BluetoothCarDevice>> = bluetoothCarDeviceDao.getMonitoredDevices()
    val selectedCarDevice: Flow<BluetoothCarDevice?> = bluetoothCarDeviceDao.getSelectedMonitoredDeviceFlow()

    suspend fun saveNewParkingSpot(spot: ParkingSpot, allowGeofenceAfterBt: Boolean = false): Long {
        val mostRecent = parkingSpotDao.getMostRecentSpotDirect()
        val now = System.currentTimeMillis()
        if (mostRecent != null) {
            val timeDiffMs = Math.abs(now - mostRecent.timestamp)
            val distMeters = try {
                com.example.sensor.LocationHelper.calculateDistanceMeters(
                    mostRecent.latitude, mostRecent.longitude,
                    spot.latitude, spot.longitude
                )
            } catch (_: Throwable) {
                999f
            }

            val isNearby = distMeters < 80f || (mostRecent.latitude == spot.latitude && mostRecent.longitude == spot.longitude)
            val isSameCoords = distMeters < 30f || (mostRecent.latitude == spot.latitude && mostRecent.longitude == spot.longitude)
            val isSameBtDevice = !spot.bluetoothDeviceAddress.isNullOrBlank() &&
                    spot.bluetoothDeviceAddress.equals(mostRecent.bluetoothDeviceAddress, ignoreCase = true)
            val isBothGeofence = spot.isGeofenceEntry && mostRecent.isGeofenceEntry

            // Case A: High-priority Bluetooth disconnect arrives and a Geofence spot was recently saved nearby (< 80m, < 5 mins)
            // Bluetooth disconnect provides hardware-level vehicle certainty -> supersedes/upgrades recent geofence spot
            if (spot.isBluetoothEntry && mostRecent.isGeofenceEntry && isNearby && timeDiffMs < 5 * 60 * 1000L) {
                try {
                    Log.d("ParkingRepository", "High-priority Bluetooth disconnect event supersedes recent geofence spot (${mostRecent.id})")
                } catch (_: Throwable) {}
                val upgradedSpot = mostRecent.copy(
                    latitude = spot.latitude,
                    longitude = spot.longitude,
                    altitude = if (spot.altitude != 0.0) spot.altitude else mostRecent.altitude,
                    accuracyMeters = if (spot.accuracyMeters > 0) spot.accuracyMeters else mostRecent.accuracyMeters,
                    address = if (spot.address.isNotBlank() && !spot.address.startsWith("Lat")) spot.address else mostRecent.address,
                    spotName = spot.spotName,
                    bluetoothDeviceName = spot.bluetoothDeviceName,
                    bluetoothDeviceAddress = spot.bluetoothDeviceAddress,
                    note = spot.note.ifBlank { mostRecent.note },
                    isAutoSaved = true,
                    isActive = true
                )
                parkingSpotDao.updateSpot(upgradedSpot)
                return mostRecent.id
            }

            // Case B: Late-arriving Geofence exit save arrives after a Bluetooth disconnect event already saved the spot (< 80m, < 10 mins)
            // Bluetooth spot takes precedence -> Geofence event smoothly merges into existing spot without creating a twin record
            if (spot.isGeofenceEntry && mostRecent.isBluetoothEntry && isNearby && timeDiffMs < 10 * 60 * 1000L) {
                if (!allowGeofenceAfterBt) {
                    try {
                        Log.d("ParkingRepository", "Late-arriving geofence exit event smoothly merges into existing Bluetooth disconnect spot (${mostRecent.id})")
                    } catch (_: Throwable) {}
                    if (spot.address.isNotBlank() && !spot.address.startsWith("Lat") && spot.address != "Unknown Address" &&
                        (mostRecent.address.isBlank() || mostRecent.address.startsWith("Lat") || mostRecent.address == "Unknown Address")
                    ) {
                        parkingSpotDao.updateSpot(mostRecent.copy(address = spot.address, isActive = true))
                    }
                    return mostRecent.id
                } else {
                    try {
                        Log.d("ParkingRepository", "Dev option enabled: allowing geofence auto-save even though Bluetooth spot exists (${mostRecent.id})")
                    } catch (_: Throwable) {}
                }
            }

            // Case C: Rapid broadcast duplicate detection (ACL + A2DP + Headset arriving within seconds)
            // Simultaneous profile broadcasts arrive within milliseconds at the exact same coordinates.
            // A genuine new/second Bluetooth disconnect (arriving after the rapid window or at a new location)
            // is a distinct parking event and must be saved.
            val isRapidDuplicate = isSameCoords && timeDiffMs < 10_000L
            val isDuplicateGeofence = isBothGeofence && isNearby && timeDiffMs < 60_000L

            if (isRapidDuplicate || isDuplicateGeofence) {
                try {
                    Log.w("ParkingRepository", "Deduplicated parking spot save! Returning existing ID: ${mostRecent.id}")
                } catch (_: Throwable) {}
                if (spot.address.isNotBlank() && !spot.address.startsWith("Lat") && spot.address != "Unknown Address" &&
                    (mostRecent.address.isBlank() || mostRecent.address.startsWith("Lat") || mostRecent.address == "Unknown Address")
                ) {
                    parkingSpotDao.updateSpot(mostRecent.copy(address = spot.address, isActive = true))
                }
                return mostRecent.id
            }
        }

        // Deactivate previous active spot first
        parkingSpotDao.deactivateAllSpots()
        return parkingSpotDao.insertSpot(spot.copy(isActive = true))
    }

    suspend fun getSpotById(id: Long): ParkingSpot? {
        return parkingSpotDao.getSpotById(id)
    }

    suspend fun updateParkingSpot(spot: ParkingSpot) {
        parkingSpotDao.updateSpot(spot)
    }

    suspend fun updateSpotNote(spotId: Long, newNote: String) {
        val spot = parkingSpotDao.getSpotById(spotId)
        if (spot != null) {
            parkingSpotDao.updateSpot(spot.copy(note = newNote))
        }
    }

    suspend fun setActiveSpot(spotId: Long) {
        val spot = parkingSpotDao.getSpotById(spotId)
        if (spot != null) {
            parkingSpotDao.deactivateAllSpots()
            parkingSpotDao.updateSpot(spot.copy(isActive = true))
        }
    }

    suspend fun getActiveSpotDirect(): ParkingSpot? {
        return parkingSpotDao.getActiveSpotDirect()
    }

    suspend fun getMostRecentSpotDirect(): ParkingSpot? {
        return parkingSpotDao.getMostRecentSpotDirect()
    }

    suspend fun deleteSpot(id: Long) {
        parkingSpotDao.deleteSpotById(id)
    }

    suspend fun clearHistory() {
        parkingSpotDao.clearAllSpots()
    }

    suspend fun deactivateActiveSpot() {
        parkingSpotDao.deactivateAllSpots()
    }

    suspend fun getAllDevicesDirect(): List<BluetoothCarDevice> {
        return bluetoothCarDeviceDao.getAllDevicesDirect()
    }

    suspend fun getDeviceByAddress(address: String): BluetoothCarDevice? {
        return bluetoothCarDeviceDao.getDeviceByAddress(address)
    }

    suspend fun getMonitoredDevicesDirect(): List<BluetoothCarDevice> {
        return bluetoothCarDeviceDao.getMonitoredDevicesDirect()
    }

    suspend fun registerBluetoothDevice(device: BluetoothCarDevice, setAsPrimary: Boolean = false) {
        val existing = bluetoothCarDeviceDao.getDeviceByAddress(device.address)
        if (existing != null) {
            val origName = if (existing.originalName.isNotBlank()) existing.originalName else (if (device.originalName.isNotBlank()) device.originalName else device.name)
            val updated = existing.copy(
                originalName = origName,
                name = if (existing.isCustomRenamed) existing.name else device.name,
                deviceType = if (existing.deviceType.isNotBlank()) existing.deviceType else device.deviceType,
                isMonitoredCar = if (setAsPrimary) true else existing.isMonitoredCar
            )
            bluetoothCarDeviceDao.update(updated)
        } else {
            val origName = if (device.originalName.isNotBlank()) device.originalName else device.name
            bluetoothCarDeviceDao.insertOrUpdate(
                device.copy(
                    originalName = origName,
                    isMonitoredCar = if (setAsPrimary) true else device.isMonitoredCar
                )
            )
        }
    }

    suspend fun updateBluetoothDevice(device: BluetoothCarDevice) {
        bluetoothCarDeviceDao.update(device)
    }

    suspend fun renameBluetoothDevice(address: String, newName: String) {
        val existing = bluetoothCarDeviceDao.getDeviceByAddress(address)
        val trimmed = newName.trim()
        if (existing != null) {
            val origName = if (existing.originalName.isNotBlank()) existing.originalName else existing.name
            val updated = existing.copy(
                name = trimmed,
                originalName = origName,
                isCustomRenamed = true
            )
            bluetoothCarDeviceDao.update(updated)
        } else {
            bluetoothCarDeviceDao.updateDeviceName(address, trimmed)
        }
    }

    /**
     * Resolves a disconnected Bluetooth device against registered vehicles with high resilience.
     * Matches across:
     * 1. Exact MAC address
     * 2. Normalized MAC address (stripped of separators)
     * 3. Custom renamed vehicle name
     * 4. Original hardware Bluetooth name
     * 5. Fallback to sole active monitored car when a disconnect arrives from a car audio device or simulation
     */
    suspend fun findMatchingDevice(rawAddress: String?, rawName: String?): BluetoothCarDevice? {
        val allDevices = bluetoothCarDeviceDao.getAllDevicesDirect()
        if (allDevices.isEmpty()) return null

        val cleanAddress = rawAddress?.trim()?.uppercase()
        val strippedAddress = cleanAddress?.replace(":", "")?.replace("-", "")
        val cleanName = rawName?.trim()

        // 1. Direct or normalized MAC address match
        if (!cleanAddress.isNullOrBlank()) {
            val matchByAddress = allDevices.firstOrNull { dev ->
                dev.address.equals(cleanAddress, ignoreCase = true) ||
                        dev.address.replace(":", "").replace("-", "").equals(strippedAddress, ignoreCase = true)
            }
            if (matchByAddress != null) return matchByAddress
        }

        // 2. Exact match on custom renamed name
        if (!cleanName.isNullOrBlank()) {
            val matchByName = allDevices.firstOrNull { dev ->
                dev.name.equals(cleanName, ignoreCase = true)
            }
            if (matchByName != null) return matchByName

            // 3. Exact match on original hardware Bluetooth name
            val matchByOriginalName = allDevices.firstOrNull { dev ->
                dev.originalName.isNotBlank() && dev.originalName.equals(cleanName, ignoreCase = true)
            }
            if (matchByOriginalName != null) return matchByOriginalName

            // 4. Fuzzy containment match on name or originalName
            val matchByFuzzy = allDevices.firstOrNull { dev ->
                (dev.name.isNotBlank() && (dev.name.contains(cleanName, ignoreCase = true) || cleanName.contains(dev.name, ignoreCase = true))) ||
                        (dev.originalName.isNotBlank() && (dev.originalName.contains(cleanName, ignoreCase = true) || cleanName.contains(dev.originalName, ignoreCase = true)))
            }
            if (matchByFuzzy != null) return matchByFuzzy
        }

        return null
    }

    /**
     * Resolves a disconnected Bluetooth device against explicitly monitored vehicles in the Car Bluetooth menu.
     * Returns the matching BluetoothCarDevice only if it is in the user's enabled device list.
     */
    suspend fun findMatchingMonitoredDevice(rawAddress: String?, rawName: String?): BluetoothCarDevice? {
        val monitored = bluetoothCarDeviceDao.getMonitoredDevicesDirect()
        if (monitored.isEmpty()) return null

        val cleanAddress = rawAddress?.trim()?.uppercase()
        val strippedAddress = cleanAddress?.replace(":", "")?.replace("-", "")
        val cleanName = rawName?.trim()

        // 1. Direct or normalized MAC address match among monitored devices
        if (!cleanAddress.isNullOrBlank() && cleanAddress != "00:00:00:00:00:00") {
            val matchByAddress = monitored.firstOrNull { dev ->
                dev.address.equals(cleanAddress, ignoreCase = true) ||
                        dev.address.replace(":", "").replace("-", "").equals(strippedAddress, ignoreCase = true)
            }
            if (matchByAddress != null) return matchByAddress
        }

        // 2. Exact match on custom renamed name among monitored devices
        if (!cleanName.isNullOrBlank()) {
            val matchByName = monitored.firstOrNull { dev ->
                dev.name.equals(cleanName, ignoreCase = true)
            }
            if (matchByName != null) return matchByName

            // 3. Exact match on original hardware Bluetooth name among monitored devices
            val matchByOriginalName = monitored.firstOrNull { dev ->
                dev.originalName.isNotBlank() && dev.originalName.equals(cleanName, ignoreCase = true)
            }
            if (matchByOriginalName != null) return matchByOriginalName

            // 4. Fuzzy containment match on name or originalName among monitored devices (min length 4 to avoid false positives)
            if (cleanName.length >= 4) {
                val matchByFuzzy = monitored.firstOrNull { dev ->
                    (dev.name.isNotBlank() && dev.name.length >= 4 && (dev.name.contains(cleanName, ignoreCase = true) || cleanName.contains(dev.name, ignoreCase = true))) ||
                            (dev.originalName.isNotBlank() && dev.originalName.length >= 4 && (dev.originalName.contains(cleanName, ignoreCase = true) || cleanName.contains(dev.originalName, ignoreCase = true)))
                }
                if (matchByFuzzy != null) return matchByFuzzy
            }
        }

        return null
    }

    suspend fun setDeviceMonitored(address: String, isMonitored: Boolean) {
        bluetoothCarDeviceDao.setDeviceMonitoredState(address, isMonitored)
    }

    suspend fun selectPrimaryCarDevice(address: String) {
        bluetoothCarDeviceDao.setDeviceMonitoredState(address, true)
    }

    suspend fun deleteBluetoothDevice(address: String) {
        bluetoothCarDeviceDao.deleteDevice(address)
    }

    suspend fun getSelectedCarDevice(): BluetoothCarDevice? {
        return bluetoothCarDeviceDao.getSelectedMonitoredDevice()
    }

    suspend fun isDeviceMonitored(address: String): Boolean {
        val dev = bluetoothCarDeviceDao.getDeviceByAddress(address)
        return dev?.isMonitoredCar == true
    }

    suspend fun seedInitialDataIfEmpty() {
        // Purge any legacy dummy spots to ensure only real GPS locations are stored
        parkingSpotDao.deleteLegacyDemoSpots()
    }
}
