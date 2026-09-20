package com.PixelParking.wear.datalayer

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.android.gms.tasks.Tasks

data class WearParkingState(
    val hasActiveSpot: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val spotName: String = "Car",
    val address: String = "",
    val timestamp: Long = 0L,
    val phoneDistanceMeters: Float? = null,
    val phoneRelativeAngle: Float? = null,
    val crownOnRight: Boolean = true,
    val lastSyncTime: Long = 0L
)

class WearParkingStateHolder private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wear_parking_prefs", Context.MODE_PRIVATE)

    private val _parkingState = MutableStateFlow(loadFromPrefs())
    val parkingState: StateFlow<WearParkingState> = _parkingState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "WearParkingState"

        @Volatile
        private var INSTANCE: WearParkingStateHolder? = null

        fun getInstance(context: Context): WearParkingStateHolder {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WearParkingStateHolder(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun loadFromPrefs(): WearParkingState {
        val hasSpot = prefs.getBoolean("has_active_spot", false)
        val lat = prefs.getFloat("latitude", 0f).toDouble()
        val lng = prefs.getFloat("longitude", 0f).toDouble()
        val name = prefs.getString("spot_name", "Car") ?: "Car"
        val address = prefs.getString("address", "") ?: ""
        val timestamp = prefs.getLong("timestamp", 0L)
        val crownRight = prefs.getBoolean("crown_on_right", true)
        return WearParkingState(
            hasActiveSpot = hasSpot,
            latitude = lat,
            longitude = lng,
            spotName = name,
            address = address,
            timestamp = timestamp,
            crownOnRight = crownRight
        )
    }

    fun updateCrownPosition(crownOnRight: Boolean) {
        _parkingState.value = _parkingState.value.copy(crownOnRight = crownOnRight)
        prefs.edit().putBoolean("crown_on_right", crownOnRight).apply()
        Log.d(TAG, "Updated crown position: crownOnRight=$crownOnRight")
    }

    fun updateSpot(
        hasActive: Boolean,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        spotName: String,
        address: String,
        timestamp: Long
    ) {
        val currentCrown = _parkingState.value.crownOnRight
        val state = WearParkingState(
            hasActiveSpot = hasActive,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            spotName = spotName,
            address = address,
            timestamp = timestamp,
            crownOnRight = currentCrown,
            lastSyncTime = System.currentTimeMillis()
        )
        _parkingState.value = state

        prefs.edit()
            .putBoolean("has_active_spot", hasActive)
            .putFloat("latitude", latitude.toFloat())
            .putFloat("longitude", longitude.toFloat())
            .putString("spot_name", spotName)
            .putString("address", address)
            .putLong("timestamp", timestamp)
            .apply()

        Log.d(TAG, "Updated Wear parking state: hasSpot=$hasActive, lat=$latitude, lng=$longitude")
    }

    fun updatePhoneTelemetry(distanceMeters: Float, relativeAngle: Float) {
        _parkingState.value = _parkingState.value.copy(
            phoneDistanceMeters = distanceMeters,
            phoneRelativeAngle = relativeAngle
        )
    }

    /**
     * Marks the active parking spot as found and notifies connected companion nodes.
     */
    fun markCarFound() {
        updateSpot(
            hasActive = false,
            latitude = 0.0,
            longitude = 0.0,
            altitude = 0.0,
            spotName = "Car",
            address = "",
            timestamp = 0L
        )

        scope.launch {
            try {
                val nodeClient = Wearable.getNodeClient(context)
                val messageClient = Wearable.getMessageClient(context)
                val nodes = Tasks.await(nodeClient.connectedNodes)
                Log.d(TAG, "Sending car_found to connected nodes: ${nodes.size}")
                for (node in nodes) {
                    Tasks.await(messageClient.sendMessage(node.id, "/car_found", ByteArray(0)))
                    Log.d(TAG, "Sent /car_found to node ${node.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending car found message: ${e.message}", e)
            }
        }
    }

    /**
     * Manually saves current location on the watch and notifies connected companion nodes to start navigation.
     */
    fun saveCurrentSpot(
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0,
        spotName: String = "Car"
    ) {
        val timestamp = System.currentTimeMillis()
        updateSpot(
            hasActive = true,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            spotName = spotName,
            address = "Saved from Watch",
            timestamp = timestamp
        )

        scope.launch {
            try {
                val nodeClient = Wearable.getNodeClient(context)
                val messageClient = Wearable.getMessageClient(context)
                val payload = "$latitude,$longitude,$altitude,$spotName".toByteArray(Charsets.UTF_8)
                val nodes = Tasks.await(nodeClient.connectedNodes)
                Log.d(TAG, "Connected nodes: ${nodes.size}")
                for (node in nodes) {
                    Tasks.await(messageClient.sendMessage(node.id, "/save_spot", payload))
                    Log.d(TAG, "Sent /save_spot to node ${node.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending save spot message: ${e.message}", e)
            }
        }
    }

    /**
     * Reads cached DataItems from local Play Services and asks connected phone nodes for fresh sync.
     */
    fun refreshFromDataLayer() {
        scope.launch {
            try {
                val dataClient = Wearable.getDataClient(context)
                dataClient.dataItems.addOnSuccessListener { buffer ->
                    try {
                        for (i in 0 until buffer.count) {
                            val item = buffer.get(i)
                            if (item.uri.path == "/parking_spot") {
                                val dataMap = DataMapItem.fromDataItem(item).dataMap
                                val hasSpot = dataMap.getBoolean("has_active_spot", false)
                                val lat = dataMap.getDouble("latitude", 0.0)
                                val lng = dataMap.getDouble("longitude", 0.0)
                                val alt = dataMap.getDouble("altitude", 0.0)
                                val name = dataMap.getString("spot_name", "Car") ?: "Car"
                                val address = dataMap.getString("address", "") ?: ""
                                val timestamp = dataMap.getLong("timestamp", 0L)
                                updateSpot(hasSpot, lat, lng, alt, name, address, timestamp)
                            } else if (item.uri.path == "/parking_config") {
                                val dataMap = DataMapItem.fromDataItem(item).dataMap
                                val crownOnRight = dataMap.getBoolean("crown_on_right", true)
                                updateCrownPosition(crownOnRight)
                            }
                        }
                    } finally {
                        buffer.release()
                    }
                }

                val nodeClient = Wearable.getNodeClient(context)
                nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                    val messageClient = Wearable.getMessageClient(context)
                    for (node in nodes) {
                        messageClient.sendMessage(node.id, "/request_sync", ByteArray(0))
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Sync initialization note: ${e.message}")
            }
        }
    }
}
