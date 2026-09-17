package com.example.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.example.AutoParkApplication
import com.example.data.entity.ParkingSpot
import com.example.sensor.LocationHelper
import kotlinx.coroutines.*

class QuickSaveTileService : TileService() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onClick() {
        val application = applicationContext as AutoParkApplication
        val repository = application.repository

        // Instant visual feedback
        Toast.makeText(applicationContext, "Parking location saved!", Toast.LENGTH_SHORT).show()
        val tile = qsTile
        if (tile != null) {
            tile.state = Tile.STATE_INACTIVE
            tile.updateTile()
        }

        scope.launch {
            val location = LocationHelper.getCurrentLocation(applicationContext)
            if (location != null) {
                val address = LocationHelper.getAddressFromCoordinates(applicationContext, location.latitude, location.longitude)
                
                val spot = ParkingSpot(
                    spotName = "Quick Saved Spot",
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = address ?: "Unknown Address",
                    timestamp = System.currentTimeMillis(),
                    isActive = true,
                    floorLevel = "Ground Level",
                    note = ""
                )
                
                repository.saveNewParkingSpot(spot)
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Failed to get location. Ensure GPS is enabled.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile
        tile?.state = Tile.STATE_INACTIVE
        tile?.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
