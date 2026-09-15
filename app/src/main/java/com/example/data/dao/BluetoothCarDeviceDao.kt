package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.BluetoothCarDevice
import kotlinx.coroutines.flow.Flow

@Dao
interface BluetoothCarDeviceDao {
    @Query("SELECT * FROM bluetooth_devices ORDER BY lastConnectedTimestamp DESC")
    fun getAllDevices(): Flow<List<BluetoothCarDevice>>

    @Query("SELECT * FROM bluetooth_devices WHERE isMonitoredCar = 1")
    fun getMonitoredDevices(): Flow<List<BluetoothCarDevice>>

    @Query("SELECT * FROM bluetooth_devices WHERE isMonitoredCar = 1 LIMIT 1")
    suspend fun getSelectedMonitoredDevice(): BluetoothCarDevice?

    @Query("SELECT * FROM bluetooth_devices WHERE isMonitoredCar = 1 LIMIT 1")
    fun getSelectedMonitoredDeviceFlow(): Flow<BluetoothCarDevice?>

    @Query("UPDATE bluetooth_devices SET isMonitoredCar = 0")
    suspend fun clearAllMonitoredStatus()

    @Query("UPDATE bluetooth_devices SET isMonitoredCar = 1 WHERE address = :address")
    suspend fun setSoleMonitoredDevice(address: String)

    @Query("SELECT * FROM bluetooth_devices WHERE address = :address")
    suspend fun getDeviceByAddress(address: String): BluetoothCarDevice?

    @Query("UPDATE bluetooth_devices SET name = :name, isCustomRenamed = 1 WHERE address = :address")
    suspend fun updateDeviceName(address: String, name: String)

    @Query("UPDATE bluetooth_devices SET isMonitoredCar = :isMonitored WHERE address = :address")
    suspend fun setDeviceMonitoredState(address: String, isMonitored: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(device: BluetoothCarDevice)

    @Update
    suspend fun update(device: BluetoothCarDevice)

    @Query("DELETE FROM bluetooth_devices WHERE address = :address")
    suspend fun deleteDevice(address: String)
}
