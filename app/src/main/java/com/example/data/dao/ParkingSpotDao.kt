package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.ParkingSpot
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingSpotDao {
    @Query("SELECT * FROM parking_spots ORDER BY timestamp DESC")
    fun getAllSpots(): Flow<List<ParkingSpot>>

    @Query("SELECT * FROM parking_spots WHERE isActive = 1 ORDER BY timestamp DESC LIMIT 1")
    fun getActiveSpot(): Flow<ParkingSpot?>

    @Query("SELECT * FROM parking_spots WHERE isActive = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getActiveSpotDirect(): ParkingSpot?

    @Query("SELECT * FROM parking_spots ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentSpotDirect(): ParkingSpot?

    @Query("SELECT * FROM parking_spots WHERE id = :id")
    suspend fun getSpotById(id: Long): ParkingSpot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpot(spot: ParkingSpot): Long

    @Update
    suspend fun updateSpot(spot: ParkingSpot)

    @Query("UPDATE parking_spots SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAllSpots()

    @Query("DELETE FROM parking_spots WHERE id = :id")
    suspend fun deleteSpotById(id: Long)

    @Query("DELETE FROM parking_spots WHERE spotName LIKE '%SF Center%' OR spotName LIKE '%Tesla%' OR spotName LIKE '%BMW%' OR spotName LIKE '%Demo%' OR spotName LIKE '%Dummy%' OR spotName LIKE '%Fake%' OR address LIKE '%Market St%' OR address LIKE '%San Francisco%'")
    suspend fun deleteLegacyDemoSpots()

    @Query("DELETE FROM parking_spots")
    suspend fun clearAllSpots()
}
