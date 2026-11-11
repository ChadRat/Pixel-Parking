package com.example.wear

import android.content.Context
import android.util.Log
import com.example.data.entity.ParkingSpot
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WearDataLayerBridge private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataClient by lazy { Wearable.getDataClient(context) }
    private val messageClient by lazy { Wearable.getMessageClient(context) }
    private val nodeClient by lazy { Wearable.getNodeClient(context) }

    companion object {
        private const val TAG = "WearDataLayerBridge"
        const val PATH_PARKING_SPOT = "/parking_spot"
        const val PATH_TELEMETRY = "/parking_telemetry"
        const val PATH_CONFIG = "/parking_config"
        const val PATH_REQUEST_SYNC = "/request_sync"
        const val PATH_RESTORE_LATEST_SPOT = "/restore_latest_spot"

        @Volatile
        private var INSTANCE: WearDataLayerBridge? = null

        fun getInstance(context: Context): WearDataLayerBridge {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WearDataLayerBridge(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Syncs the current active parking spot coordinates and metadata to Wear OS via DataClient.
     */
    fun syncParkingSpot(spot: ParkingSpot?) {
        scope.launch {
            try {
                val putDataMapReq = PutDataMapRequest.create(PATH_PARKING_SPOT).apply {
                    val isActive = spot != null && spot.isActive
                    dataMap.putBoolean("has_active_spot", isActive)
                    dataMap.putDouble("latitude", spot?.latitude ?: 0.0)
                    dataMap.putDouble("longitude", spot?.longitude ?: 0.0)
                    dataMap.putDouble("altitude", spot?.altitude ?: 0.0)
                    dataMap.putString("spot_name", spot?.note?.ifBlank { null } ?: spot?.spotName ?: spot?.bluetoothDeviceName ?: "Car")
                    dataMap.putString("address", spot?.address ?: "")
                    dataMap.putString("vehicle_name", spot?.bluetoothDeviceName ?: spot?.spotName ?: "")
                    dataMap.putLong("timestamp", spot?.timestamp ?: 0L)
                    dataMap.putLong("sync_timestamp", System.currentTimeMillis())

                    val prefs = context.getSharedPreferences("pixel_parking_prefs", Context.MODE_PRIVATE)
                    val crownOnRight = prefs.getBoolean("wear_crown_on_right", true)
                    val aodBatteryMode = prefs.getString("aod_battery_mode", "PERCENTAGE") ?: "PERCENTAGE"
                    dataMap.putBoolean("crown_on_right", crownOnRight)
                    dataMap.putString("aod_battery_mode", aodBatteryMode)
                }

                val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
                dataClient.putDataItem(putDataReq).await()
                Log.d(TAG, "Successfully synced parking spot to Wear DataLayer: active=${spot?.isActive}, lat=${spot?.latitude}, lng=${spot?.longitude}")
            } catch (e: Exception) {
                Log.w(TAG, "Could not sync parking spot to Wear OS DataLayer: ${e.message}")
            }
        }
    }

    /**
     * Broadcasts live navigation telemetry to connected Wear OS devices for real-time glanceable updates.
     */
    fun syncLiveTelemetry(
        distanceMeters: Float,
        targetBearing: Float,
        relativeArrowAngle: Float,
        isAligned: Boolean,
        distanceCoveredProgress: Float = 0f
    ) {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) return@launch

                val payload = "$distanceMeters,$targetBearing,$relativeArrowAngle,${if (isAligned) 1 else 0},$distanceCoveredProgress".toByteArray(Charsets.UTF_8)
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, PATH_TELEMETRY, payload)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Telemetry sync skipped: ${e.message}")
            }
        }
    }

    /**
     * Syncs watch crown position configuration (left vs right) to connected Wear OS devices instantly.
     */
    fun syncCrownPosition(crownOnRight: Boolean) {
        scope.launch {
            val prefs = context.getSharedPreferences("pixel_parking_prefs", Context.MODE_PRIVATE)
            val aodBatteryMode = prefs.getString("aod_battery_mode", "PERCENTAGE") ?: "PERCENTAGE"
            // 1. Send instant direct message to all active nodes for real-time immediate update
            try {
                val nodes = nodeClient.connectedNodes.await()
                val payload = "${if (crownOnRight) "1" else "0"},$aodBatteryMode".toByteArray(Charsets.UTF_8)
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, PATH_CONFIG, payload)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Crown direct message notification skipped: ${e.message}")
            }

            // 2. Persist in DataItem so it is saved and delivered even if watch connects later
            try {
                val putDataMapReq = PutDataMapRequest.create(PATH_CONFIG).apply {
                    dataMap.putBoolean("crown_on_right", crownOnRight)
                    dataMap.putString("aod_battery_mode", aodBatteryMode)
                    dataMap.putLong("sync_timestamp", System.currentTimeMillis())
                }
                val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
                dataClient.putDataItem(putDataReq).await()
                Log.d(TAG, "Synced crown position: crownOnRight=$crownOnRight, aodBatteryMode=$aodBatteryMode")
            } catch (e: Exception) {
                Log.w(TAG, "Could not sync crown position: ${e.message}")
            }
        }
    }
}
