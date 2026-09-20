package com.example.wear

import android.content.Context
import android.util.Log
import com.example.data.entity.ParkingSpot
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicBoolean

class WearDataLayerBridge private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataClient by lazy { Wearable.getDataClient(context) }
    private val messageClient by lazy { Wearable.getMessageClient(context) }
    private val nodeClient by lazy { Wearable.getNodeClient(context) }

    // Cached flag to avoid redundant failed calls on devices without Wear OS / Play Services Wear support
    private val isWearApiAvailable = AtomicBoolean(true)
    private val availabilityChecked = AtomicBoolean(false)

    companion object {
        private const val TAG = "WearDataLayerBridge"
        const val PATH_PARKING_SPOT = "/parking_spot"
        const val PATH_TELEMETRY = "/parking_telemetry"
        const val PATH_CONFIG = "/parking_config"
        const val PATH_REQUEST_SYNC = "/request_sync"

        @Volatile
        private var INSTANCE: WearDataLayerBridge? = null

        fun getInstance(context: Context): WearDataLayerBridge {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WearDataLayerBridge(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Checks if Wearable API is available on this device.
     */
    private suspend fun checkWearableAvailability(): Boolean {
        if (availabilityChecked.get()) {
            return isWearApiAvailable.get()
        }

        return try {
            GoogleApiAvailability.getInstance().checkApiAvailability(dataClient).await()
            isWearApiAvailable.set(true)
            availabilityChecked.set(true)
            true
        } catch (e: Exception) {
            isWearApiAvailable.set(false)
            availabilityChecked.set(true)
            Log.i(TAG, "Wearable.API is not available on this device (no Wear OS companion/service paired). Wear DataLayer sync disabled.")
            false
        }
    }

    /**
     * Syncs the current active parking spot coordinates and metadata to Wear OS via DataClient.
     */
    fun syncParkingSpot(spot: ParkingSpot?) {
        scope.launch {
            if (!checkWearableAvailability()) {
                return@launch
            }

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
                }

                val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
                dataClient.putDataItem(putDataReq).await()
                Log.d(TAG, "Successfully synced parking spot to Wear DataLayer: active=${spot?.isActive}, lat=${spot?.latitude}, lng=${spot?.longitude}")
            } catch (e: ApiException) {
                if (e.statusCode == CommonStatusCodes.API_NOT_CONNECTED || e.statusCode == 17 /* API_UNAVAILABLE */) {
                    isWearApiAvailable.set(false)
                    Log.i(TAG, "Wearable.API unavailable on this device (${e.statusCode}). Suppressing further attempts.")
                } else {
                    Log.w(TAG, "Wear OS sync API exception: ${e.message}")
                }
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
        isAligned: Boolean
    ) {
        if (!isWearApiAvailable.get()) return

        scope.launch {
            if (!checkWearableAvailability()) return@launch

            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) return@launch

                val payload = "$distanceMeters,$targetBearing,$relativeArrowAngle,${if (isAligned) 1 else 0}".toByteArray(Charsets.UTF_8)
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, PATH_TELEMETRY, payload)
                }
            } catch (e: ApiException) {
                if (e.statusCode == CommonStatusCodes.API_NOT_CONNECTED || e.statusCode == 17) {
                    isWearApiAvailable.set(false)
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
            if (!checkWearableAvailability()) return@launch

            // 1. Send instant direct message to all active nodes for real-time immediate update
            try {
                val nodes = nodeClient.connectedNodes.await()
                val payload = (if (crownOnRight) "1" else "0").toByteArray(Charsets.UTF_8)
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
                    dataMap.putLong("sync_timestamp", System.currentTimeMillis())
                }
                val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
                dataClient.putDataItem(putDataReq).await()
                Log.d(TAG, "Synced crown position: crownOnRight=$crownOnRight")
            } catch (e: Exception) {
                Log.w(TAG, "Could not sync crown position: ${e.message}")
            }
        }
    }
}
