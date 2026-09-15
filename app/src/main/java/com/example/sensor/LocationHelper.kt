package com.example.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? = withContext(Dispatchers.IO) {
        // 1. Try Google Play Services FusedLocationProviderClient
        try {
            val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
            
            // Check fresh cached location first
            val lastLoc = suspendCancellableCoroutine<Location?> { cont ->
                fusedClient.lastLocation
                    .addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }

            // If last location is very recent (< 2 minutes old), return it immediately
            if (lastLoc != null && (System.currentTimeMillis() - lastLoc.time) < 120_000L) {
                return@withContext lastLoc
            }

            // Request fresh high-accuracy location with a safe timeout
            val freshLoc = withTimeoutOrNull(4000L) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val cts = CancellationTokenSource()
                    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { loc ->
                            if (cont.isActive) cont.resume(loc)
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                    cont.invokeOnCancellation { cts.cancel() }
                }
            }

            if (freshLoc != null) {
                return@withContext freshLoc
            }

            if (lastLoc != null) {
                return@withContext lastLoc
            }

            // Try a fast single update via LocationCallback with timeout
            val callbackLoc = withTimeoutOrNull(3500L) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                        .setMaxUpdates(1)
                        .setDurationMillis(3000L)
                        .build()
                    val cb = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            fusedClient.removeLocationUpdates(this)
                            if (cont.isActive) cont.resume(result.lastLocation)
                        }
                    }
                    fusedClient.requestLocationUpdates(req, cb, Looper.getMainLooper())
                    cont.invokeOnCancellation { fusedClient.removeLocationUpdates(cb) }
                }
            }

            if (callbackLoc != null) {
                return@withContext callbackLoc
            }
        } catch (e: Exception) {
            // Location permission not granted or Google Play Services error
        }

        // 2. Fallback to System LocationManager (GPS, Network, Passive)
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null) {
                val providers = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER
                )
                val validLocs = providers.mapNotNull { provider ->
                    try {
                        if (locationManager.isProviderEnabled(provider)) {
                            locationManager.getLastKnownLocation(provider)
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
                if (validLocs.isNotEmpty()) {
                    return@withContext validLocs.minByOrNull { it.accuracy }
                }

                // 3. Active single listener request if cached is not ready
                val activeSysLoc = withTimeoutOrNull(3000L) {
                    suspendCancellableCoroutine<Location?> { cont ->
                        val listener = object : android.location.LocationListener {
                            override fun onLocationChanged(loc: Location) {
                                locationManager.removeUpdates(this)
                                if (cont.isActive) cont.resume(loc)
                            }
                            @Deprecated("Deprecated in Java")
                            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                            override fun onProviderEnabled(provider: String) {}
                            override fun onProviderDisabled(provider: String) {}
                        }
                        try {
                            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener, Looper.getMainLooper())
                            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener, Looper.getMainLooper())
                            } else {
                                if (cont.isActive) cont.resume(null)
                            }
                            cont.invokeOnCancellation { locationManager.removeUpdates(listener) }
                        } catch (e: Exception) {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                }
                if (activeSysLoc != null) {
                    return@withContext activeSysLoc
                }
            }
        } catch (e: Exception) {
            // Fallback failed
        }

        null
    }

    @SuppressLint("MissingPermission")
    fun startContinuousLocationUpdates(
        context: Context,
        intervalMs: Long = 3000L,
        onLocationChanged: (Location) -> Unit
    ): LocationCallback? {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
                .setMinUpdateIntervalMillis(intervalMs / 2)
                .setMinUpdateDistanceMeters(2f)
                .build()
            val cb = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { onLocationChanged(it) }
                }
            }
            fusedClient.requestLocationUpdates(req, cb, Looper.getMainLooper())
            return cb
        } catch (e: Exception) {
            return null
        }
    }

    fun stopContinuousLocationUpdates(context: Context, callback: LocationCallback?) {
        if (callback == null) return
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.removeLocationUpdates(callback)
        } catch (e: Exception) {
            // Ignored
        }
    }

    suspend fun getAddressFromCoordinates(
        context: Context,
        lat: Double,
        lng: Double
    ): String = withContext(Dispatchers.IO) {
        // 1. Try Android System Geocoder with timeout
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val sysAddress = withTimeoutOrNull(3000L) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine<String?> { cont ->
                            geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) {
                                    if (addresses.isNotEmpty()) {
                                        val addr = addresses[0]
                                        val line = addr.getAddressLine(0)
                                            ?: "${addr.thoroughfare ?: ""} ${addr.subThoroughfare ?: ""}, ${addr.locality ?: ""}".trim()
                                        cont.resume(line.ifBlank { null })
                                    } else {
                                        cont.resume(null)
                                    }
                                }
                                override fun onError(errorMessage: String?) {
                                    cont.resume(null)
                                }
                            })
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            val line = addr.getAddressLine(0)
                                ?: "${addr.thoroughfare ?: ""} ${addr.subThoroughfare ?: ""}, ${addr.locality ?: ""}".trim()
                            if (line.isNotBlank()) line else null
                        } else null
                    }
                }
                if (!sysAddress.isNullOrBlank()) {
                    return@withContext sysAddress
                }
            }
        } catch (e: Exception) {
            // Geocoder service error or timeout
        }

        // 2. HTTP Reverse Geocode Fallback (OpenStreetMap Nominatim)
        try {
            val urlStr = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lng&zoom=18&addressdetails=1"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("User-Agent", "PixelParkingApp/1.0 (Android AutoPark)")
            }
            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(json)
                val displayName = obj.optString("display_name")
                val addressObj = obj.optJSONObject("address")
                val road = addressObj?.optString("road")
                val houseNumber = addressObj?.optString("house_number")
                val city = addressObj?.optString("city")
                    ?: addressObj?.optString("town")
                    ?: addressObj?.optString("suburb")
                    ?: addressObj?.optString("village")

                val clean = when {
                    !road.isNullOrBlank() && !city.isNullOrBlank() -> {
                        if (!houseNumber.isNullOrBlank()) "$road $houseNumber, $city" else "$road, $city"
                    }
                    !displayName.isNullOrBlank() -> displayName.split(",").take(3).joinToString(",").trim()
                    else -> null
                }
                if (!clean.isNullOrBlank()) {
                    return@withContext clean
                }
            }
        } catch (e: Exception) {
            // Fallback
        }

        // 3. Fallback clean coordinate representation
        val latDir = if (lat >= 0) "N" else "S"
        val lngDir = if (lng >= 0) "E" else "W"
        String.format(Locale.US, "%.5f° %s, %.5f° %s", kotlin.math.abs(lat), latDir, kotlin.math.abs(lng), lngDir)
    }

    suspend fun getCoordinatesFromAddress(
        context: Context,
        query: String
    ): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext null

        // 1. Try Geocoder
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val result = withTimeoutOrNull(3000L) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine<Pair<Double, Double>?> { cont ->
                            geocoder.getFromLocationName(query, 1, object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) {
                                    if (addresses.isNotEmpty()) {
                                        cont.resume(Pair(addresses[0].latitude, addresses[0].longitude))
                                    } else {
                                        cont.resume(null)
                                    }
                                }
                                override fun onError(errorMessage: String?) {
                                    cont.resume(null)
                                }
                            })
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val list = geocoder.getFromLocationName(query, 1)
                        if (!list.isNullOrEmpty()) {
                            Pair(list[0].latitude, list[0].longitude)
                        } else null
                    }
                }
                if (result != null) return@withContext result
            }
        } catch (e: Exception) {
            // Geocoder failed
        }

        // 2. HTTP Geocode Search Fallback (OpenStreetMap Nominatim)
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val urlStr = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=1"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("User-Agent", "PixelParkingApp/1.0 (Android AutoPark)")
            }
            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(json)
                if (arr.length() > 0) {
                    val first = arr.getJSONObject(0)
                    val lat = first.getDouble("lat")
                    val lon = first.getDouble("lon")
                    return@withContext Pair(lat, lon)
                }
            }
        } catch (e: Exception) {
            // Failed
        }

        null
    }

    /**
     * Calculates distance between two coordinates in meters (Haversine formula)
     */
    fun calculateDistanceMeters(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }

    /**
     * Calculates initial bearing (0..360 degrees) from source to target
     */
    fun calculateBearingDegrees(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Float {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLngRad = Math.toRadians(lng2 - lng1)

        val y = sin(deltaLngRad) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLngRad)
        val bearingRad = atan2(y, x)
        val bearingDeg = Math.toDegrees(bearingRad)
        return ((bearingDeg + 360) % 360).toFloat()
    }

    fun formatDistance(meters: Float): String {
        return if (meters < 1000) {
            "${meters.toInt()} m"
        } else if (meters < 10000 && (meters % 100 == 0f || (meters / 100f).toInt() % 10 == 0)) {
            String.format(Locale.US, "%.1f km", meters / 1000f)
        } else if (meters < 10000) {
            String.format(Locale.US, "%.1f km", meters / 1000f)
        } else {
            String.format(Locale.US, "%.2f km", meters / 1000f)
        }
    }

    fun formatDistanceFeet(meters: Float): String {
        val feet = meters * 3.28084f
        return if (feet < 5280) {
            "${feet.toInt()} ft"
        } else {
            String.format(Locale.US, "%.2f mi", feet / 5280f)
        }
    }
}
