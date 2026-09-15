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

    suspend fun deleteSpot(id: Long) {
        parkingSpotDao.deleteSpotById(id)
    }

    suspend fun clearHistory() {
        parkingSpotDao.clearAllSpots()
    }

    suspend fun registerBluetoothDevice(device: BluetoothCarDevice, setAsPrimary: Boolean = false) {
        bluetoothCarDeviceDao.insertOrUpdate(device)
    }

    suspend fun updateBluetoothDevice(device: BluetoothCarDevice) {
        bluetoothCarDeviceDao.update(device)
    }

    suspend fun renameBluetoothDevice(address: String, newName: String) {
        bluetoothCarDeviceDao.updateDeviceName(address, newName)
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
