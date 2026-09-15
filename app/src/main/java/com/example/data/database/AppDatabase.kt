package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.BluetoothCarDeviceDao
import com.example.data.dao.ParkingSpotDao
import com.example.data.entity.BluetoothCarDevice
import com.example.data.entity.ParkingSpot

@Database(
    entities = [
        ParkingSpot::class,
        BluetoothCarDevice::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun parkingSpotDao(): ParkingSpotDao
    abstract fun bluetoothCarDeviceDao(): BluetoothCarDeviceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autopark_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
