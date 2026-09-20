package com.example.data.repository

import android.content.Context
import com.example.data.dao.BluetoothCarDeviceDao
import com.example.data.dao.ParkingSpotDao
import com.example.data.entity.BluetoothCarDevice
import com.example.data.entity.ParkingSpot
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

    suspend fun saveNewParkingSpot(spot: ParkingSpot): Long {
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
        if (!cleanAddress.isNullOrBlank()) {
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

            // 4. Fuzzy containment match on name or originalName among monitored devices
            val matchByFuzzy = monitored.firstOrNull { dev ->
                (dev.name.isNotBlank() && (dev.name.contains(cleanName, ignoreCase = true) || cleanName.contains(dev.name, ignoreCase = true))) ||
                        (dev.originalName.isNotBlank() && (dev.originalName.contains(cleanName, ignoreCase = true) || cleanName.contains(dev.originalName, ignoreCase = true)))
            }
            if (matchByFuzzy != null) return matchByFuzzy
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
