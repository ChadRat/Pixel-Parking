package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BluetoothCarDeviceDao
import com.example.data.dao.ParkingSpotDao
import com.example.data.entity.BluetoothCarDevice
import com.example.data.entity.ParkingSpot

@Database(
    entities = [
        ParkingSpot::class,
        BluetoothCarDevice::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun parkingSpotDao(): ParkingSpotDao
    abstract fun bluetoothCarDeviceDao(): BluetoothCarDeviceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE bluetooth_devices ADD COLUMN originalName TEXT NOT NULL DEFAULT ''")
                    db.execSQL("UPDATE bluetooth_devices SET originalName = name WHERE originalName = ''")
                } catch (_: Exception) {}
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autopark_database"
                )
                    .addMigrations(MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
