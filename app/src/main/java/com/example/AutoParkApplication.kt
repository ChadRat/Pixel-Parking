package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.repository.ParkingRepository
import com.example.notification.ParkingNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoParkApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: ParkingRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getDatabase(this)
        repository = ParkingRepository(
            database.parkingSpotDao(),
            database.bluetoothCarDeviceDao()
        )

        // Initialize Notification Channels
        ParkingNotificationHelper.createNotificationChannels(this)

        // Seed initial preview data
        CoroutineScope(Dispatchers.IO).launch {
            repository.seedInitialDataIfEmpty()
        }
    }
}
