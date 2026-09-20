package com.PixelParking.wear.datalayer

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearDataLayerListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "WearDataLayerService"
        const val PATH_PARKING_SPOT = "/parking_spot"
        const val PATH_TELEMETRY = "/parking_telemetry"
        const val PATH_CONFIG = "/parking_config"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val stateHolder = WearParkingStateHolder.getInstance(applicationContext)

        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val uri = event.dataItem.uri
                if (uri.path == PATH_PARKING_SPOT) {
                    try {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val hasSpot = dataMap.getBoolean("has_active_spot", false)
                        val lat = dataMap.getDouble("latitude", 0.0)
                        val lng = dataMap.getDouble("longitude", 0.0)
                        val alt = dataMap.getDouble("altitude", 0.0)
                        val name = dataMap.getString("spot_name", "Car") ?: "Car"
                        val address = dataMap.getString("address", "") ?: ""
                        val timestamp = dataMap.getLong("timestamp", 0L)

                        Log.d(TAG, "Received active parking spot from DataLayer: hasSpot=$hasSpot, lat=$lat, lng=$lng")
                        stateHolder.updateSpot(hasSpot, lat, lng, alt, name, address, timestamp)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing parking spot DataMap", e)
                    }
                } else if (uri.path == PATH_CONFIG) {
                    try {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val crownOnRight = dataMap.getBoolean("crown_on_right", true)
                        Log.d(TAG, "Received crown config from DataLayer: crownOnRight=$crownOnRight")
                        stateHolder.updateCrownPosition(crownOnRight)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing config DataMap", e)
                    }
                }
            } else if (event.type == DataEvent.TYPE_DELETED) {
                if (event.dataItem.uri.path == PATH_PARKING_SPOT) {
                    stateHolder.updateSpot(false, 0.0, 0.0, 0.0, "Car", "", 0L)
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == PATH_TELEMETRY) {
            try {
                val payload = String(messageEvent.data, Charsets.UTF_8)
                val parts = payload.split(",")
                if (parts.size >= 3) {
                    val dist = parts[0].toFloatOrNull() ?: return
                    val relativeAngle = parts[2].toFloatOrNull() ?: return
                    WearParkingStateHolder.getInstance(applicationContext)
                        .updatePhoneTelemetry(dist, relativeAngle)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error parsing telemetry: ${e.message}")
            }
        } else if (messageEvent.path == PATH_CONFIG) {
            try {
                val payload = String(messageEvent.data, Charsets.UTF_8)
                val crownOnRight = payload == "1" || payload.equals("true", ignoreCase = true)
                Log.d(TAG, "Instant crown config message received: crownOnRight=$crownOnRight")
                WearParkingStateHolder.getInstance(applicationContext)
                    .updateCrownPosition(crownOnRight)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing crown config message", e)
            }
        }
    }
}
