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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    private const val PREFS_LOCATION = "location_helper_prefs"
    private const val KEY_PERSISTED_LAT = "persisted_lat"
    private const val KEY_PERSISTED_LNG = "persisted_lng"
    private const val KEY_PERSISTED_ALT = "persisted_alt"
    private const val KEY_PERSISTED_ACC = "persisted_acc"
    private const val KEY_PERSISTED_TIME = "persisted_time"

    @Volatile
    var lastKnownLocation: Location? = null

    fun warmUpLocation(context: Context) {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) persistLocation(context, loc)
                }
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) persistLocation(context, loc)
            }
        } catch (_: Throwable) {}
    }

    fun persistLocation(context: Context, loc: Location) {
        if (loc.latitude == 0.0 && loc.longitude == 0.0) return
        lastKnownLocation = loc
        try {
            context.getSharedPreferences(PREFS_LOCATION, Context.MODE_PRIVATE)
                .edit()
                .putFloat(KEY_PERSISTED_LAT, loc.latitude.toFloat())
                .putFloat(KEY_PERSISTED_LNG, loc.longitude.toFloat())
                .putFloat(KEY_PERSISTED_ALT, loc.altitude.toFloat())
                .putFloat(KEY_PERSISTED_ACC, loc.accuracy)
                .putLong(KEY_PERSISTED_TIME, if (loc.time > 0) loc.time else System.currentTimeMillis())
                .apply()
        } catch (_: Throwable) {}
    }

    fun getPersistedLocation(context: Context): Location? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_LOCATION, Context.MODE_PRIVATE)
            val lat = prefs.getFloat(KEY_PERSISTED_LAT, 0f).toDouble()
            val lng = prefs.getFloat(KEY_PERSISTED_LNG, 0f).toDouble()
            if (lat != 0.0 || lng != 0.0) {
                Location("persisted_cache").apply {
                    latitude = lat
                    longitude = lng
                    altitude = prefs.getFloat(KEY_PERSISTED_ALT, 0f).toDouble()
                    accuracy = prefs.getFloat(KEY_PERSISTED_ACC, 15f)
                    time = prefs.getLong(KEY_PERSISTED_TIME, System.currentTimeMillis())
                }
            } else null
        } catch (_: Throwable) {
            null
        }
    }

    fun formatCoordinates(lat: Double, lng: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lngDir = if (lng >= 0) "E" else "W"
        return String.format(Locale.US, "%.5f° %s, %.5f° %s", kotlin.math.abs(lat), latDir, kotlin.math.abs(lng), lngDir)
    }

    @SuppressLint("MissingPermission")
    suspend fun getLocationForAutoPark(
        context: Context,
        oldSpotLat: Double? = null,
        oldSpotLng: Double? = null,
        oldSpotTimestamp: Long = 0L,
        triggerTime: Long = System.currentTimeMillis()
    ): Location? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 1. Fast Check: If Fused Location Provider has an ultra-recent, accurate fix from driving (<= 25s, <= 15m), return immediately
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val fusedLast = withTimeoutOrNull(400L) {
                suspendCancellableCoroutine<Location?> { cont ->
                    fusedClient.lastLocation
                        .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                        .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                }
            }
            if (fusedLast != null && (fusedLast.latitude != 0.0 || fusedLast.longitude != 0.0)) {
                val age = now - fusedLast.time
                val isRecent = age in 0..25_000L
                val isStationary = !fusedLast.hasSpeed() || fusedLast.speed <= 2.2f
                val isNewerThanOldSpot = oldSpotTimestamp == 0L || fusedLast.time > oldSpotTimestamp
                if (isRecent && isStationary && isNewerThanOldSpot && fusedLast.accuracy <= 15f) {
                    val isOldDuplicate = if (oldSpotLat != null && oldSpotLng != null && (oldSpotLat != 0.0 || oldSpotLng != 0.0)) {
                        val results = FloatArray(1)
                        Location.distanceBetween(oldSpotLat, oldSpotLng, fusedLast.latitude, fusedLast.longitude, results)
                        results[0] < 5f && (fusedLast.time in 1..oldSpotTimestamp)
                    } else false

                    if (!isOldDuplicate) {
                        persistLocation(context, fusedLast)
                        return@withContext fusedLast
                    }
                }
            }
        } catch (_: Throwable) {}

        // 2. Actively stream live high-precision GPS fixes with rapid convergence (capped at 2.4s)
        val liveFix = fetchLiveLocationFix(context, triggerTime = triggerTime, maxWaitMs = 2_400L)
        if (liveFix != null && (liveFix.latitude != 0.0 || liveFix.longitude != 0.0)) {
            val isOldSpotDuplicate = if (oldSpotLat != null && oldSpotLng != null && (oldSpotLat != 0.0 || oldSpotLng != 0.0)) {
                val results = FloatArray(1)
                Location.distanceBetween(oldSpotLat, oldSpotLng, liveFix.latitude, liveFix.longitude, results)
                results[0] < 5f && (liveFix.time in 1..oldSpotTimestamp)
            } else false

            if (!isOldSpotDuplicate) {
                persistLocation(context, liveFix)
                return@withContext liveFix
            }
        }

        // 3. Fallback: Return best available instant fix (Fused, System, Cache) with zero delay
        val instantFallback = getBestAvailableInstantFix(context)
        if (instantFallback != null) {
            persistLocation(context, instantFallback)
            return@withContext instantFallback
        }

        null
    }

    @SuppressLint("MissingPermission")
    suspend fun getLocationForAutoPark(context: Context): Location? {
        return getLocationForAutoPark(context, null, null, 0L, System.currentTimeMillis())
    }

    /**
     * Instantaneous 0ms fix retrieval from FusedLocation, LocationManager, and persisted cache.
     * Guaranteed never to block or time out. Used for instant Stage 1 auto-saving.
     */
    @SuppressLint("MissingPermission")
    fun getBestAvailableInstantFix(context: Context): Location? {
        val now = System.currentTimeMillis()
        val candidates = mutableListOf<Location>()

        // 1. Try system LocationManager providers (GPS, Network, Passive, Fused)
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (lm != null) {
                val providers = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER
                )
                for (p in providers) {
                    try {
                        if (lm.isProviderEnabled(p)) {
                            val loc = lm.getLastKnownLocation(p)
                            if (loc != null && (loc.latitude != 0.0 || loc.longitude != 0.0)) {
                                candidates.add(loc)
                            }
                        }
                    } catch (_: Throwable) {}
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        if (lm.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
                            val loc = lm.getLastKnownLocation(LocationManager.FUSED_PROVIDER)
                            if (loc != null && (loc.latitude != 0.0 || loc.longitude != 0.0)) {
                                candidates.add(loc)
                            }
                        }
                    } catch (_: Throwable) {}
                }
            }
        } catch (_: Throwable) {}

        // 2. Persisted cache from SharedPreferences
        try {
            val cached = getPersistedLocation(context)
            if (cached != null && (cached.latitude != 0.0 || cached.longitude != 0.0)) {
                candidates.add(cached)
            }
        } catch (_: Throwable) {}

        if (candidates.isEmpty()) return null

        // Score candidates: prioritize freshness (< 15 mins) and accuracy
        return candidates.minByOrNull { loc ->
            val ageMinutes = ((now - loc.time).coerceAtLeast(0L)) / (60 * 1000f)
            val baseAcc = if (loc.accuracy > 0f) loc.accuracy else 50f
            // Age penalty: add 3 meters penalty per elapsed minute
            baseAcc + (ageMinutes * 3f)
        }
    }

    /**
     * Background refinement pass: streams live high-precision GPS fixes for up to maxWaitMs.
     * Triggers onBetterFix whenever an improved satellite fix is received.
     */
    @SuppressLint("MissingPermission")
    suspend fun refineLocationFix(
        context: Context,
        currentAccuracy: Float,
        maxWaitMs: Long = 4_000L,
        onBetterFix: suspend (Location) -> Unit
    ) = withContext(Dispatchers.IO) {
        var bestAcc = if (currentAccuracy > 0f) currentAccuracy else 100f
        val shouldStop = java.util.concurrent.atomic.AtomicBoolean(false)
        try {
            val fusedClient = try { LocationServices.getFusedLocationProviderClient(context) } catch (_: Throwable) { null }
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

            val fusedCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    for (loc in result.locations) {
                        if ((loc.latitude != 0.0 || loc.longitude != 0.0) && loc.accuracy > 0f && loc.accuracy < (bestAcc - 1.5f)) {
                            bestAcc = loc.accuracy
                            persistLocation(context, loc)
                            CoroutineScope(Dispatchers.IO).launch {
                                onBetterFix(loc)
                            }
                            if (bestAcc <= 8f) {
                                shouldStop.set(true)
                            }
                        }
                    }
                }
            }

            val sysListener = object : android.location.LocationListener {
                override fun onLocationChanged(loc: Location) {
                    if ((loc.latitude != 0.0 || loc.longitude != 0.0) && loc.accuracy > 0f && loc.accuracy < (bestAcc - 1.5f)) {
                        bestAcc = loc.accuracy
                        persistLocation(context, loc)
                        CoroutineScope(Dispatchers.IO).launch {
                            onBetterFix(loc)
                        }
                        if (bestAcc <= 8f) {
                            shouldStop.set(true)
                        }
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            if (fusedClient != null) {
                try {
                    val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 300L)
                        .setMinUpdateIntervalMillis(150L)
                        .setMaxUpdates(15)
                        .setDurationMillis(maxWaitMs)
                        .build()
                    fusedClient.requestLocationUpdates(req, fusedCallback, Looper.getMainLooper())
                } catch (_: Throwable) {}
            }

            if (lm != null) {
                try {
                    if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 300L, 0f, sysListener, Looper.getMainLooper())
                    }
                } catch (_: Throwable) {}
            }

            val startTime = System.currentTimeMillis()
            while (!shouldStop.get() && (System.currentTimeMillis() - startTime) < maxWaitMs) {
                delay(200L)
            }

            try {
                if (fusedClient != null) fusedClient.removeLocationUpdates(fusedCallback)
                lm?.removeUpdates(sysListener)
            } catch (_: Throwable) {}
        } catch (_: Throwable) {}
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchLiveLocationFix(
        context: Context,
        triggerTime: Long = System.currentTimeMillis(),
        maxWaitMs: Long = 2_800L
    ): Location? = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<Location?>()
        val candidates = java.util.Collections.synchronizedList(mutableListOf<Location>())
        val hasCompleted = java.util.concurrent.atomic.AtomicBoolean(false)

        val fusedClient = try {
            LocationServices.getFusedLocationProviderClient(context)
        } catch (_: Throwable) { null }

        val locationManager = try {
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        } catch (_: Throwable) { null }

        val cts = CancellationTokenSource()

        fun completeWith(loc: Location?) {
            if (hasCompleted.compareAndSet(false, true)) {
                try { cts.cancel() } catch (_: Throwable) {}
                deferred.complete(loc)
            }
        }

        fun onNewFix(fix: Location?) {
            if (fix == null || (fix.latitude == 0.0 && fix.longitude == 0.0)) return
            if (fix.accuracy <= 0f) return

            // Moving fix filter: allow up to 4.5 m/s (~16 km/h) for parking lot maneuver
            if (fix.hasSpeed() && fix.speed > 4.5f) {
                return
            }

            // Freshness validation: accept fixes within 120s of disconnect trigger
            val ageBeforeTrigger = triggerTime - fix.time
            if (ageBeforeTrigger > 120_000L) {
                return
            }

            candidates.add(fix)

            // Pinpoint accuracy (<= 12m): Excellent fix at the exact spot. Complete immediately.
            if (fix.accuracy <= 12f) {
                completeWith(fix)
                return
            }
        }

        // Fast check on fusedClient's latest cached location
        try {
            fusedClient?.lastLocation?.addOnSuccessListener { loc ->
                onNewFix(loc)
            }
        } catch (_: Throwable) {}

        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (loc in result.locations) {
                    onNewFix(loc)
                }
            }
        }

        val sysListener = object : android.location.LocationListener {
            override fun onLocationChanged(loc: Location) {
                onNewFix(loc)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        // 1. Request live streaming from Fused Location Provider Client
        try {
            if (fusedClient != null) {
                val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 250L)
                    .setMinUpdateIntervalMillis(100L)
                    .setMinUpdateDistanceMeters(0f)
                    .setMaxUpdates(25)
                    .setDurationMillis(maxWaitMs)
                    .build()
                fusedClient.requestLocationUpdates(req, fusedCallback, Looper.getMainLooper())
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc -> onNewFix(loc) }
            }
        } catch (_: Throwable) {}

        // 2. Request live streaming from Android LocationManager (GPS, Network, Passive, Fused)
        try {
            if (locationManager != null) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        300L,
                        0f,
                        sysListener,
                        Looper.getMainLooper()
                    )
                }
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        300L,
                        0f,
                        sysListener,
                        Looper.getMainLooper()
                    )
                }
                if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.PASSIVE_PROVIDER,
                        300L,
                        0f,
                        sysListener,
                        Looper.getMainLooper()
                    )
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        if (locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
                            locationManager.requestLocationUpdates(
                                LocationManager.FUSED_PROVIDER,
                                300L,
                                0f,
                                sysListener,
                                Looper.getMainLooper()
                            )
                        }
                    } catch (_: Throwable) {}
                }
            }
        } catch (_: Throwable) {}

        // Supervisor coroutine with rapid convergence window:
        // At 800ms: If pinpoint/high accuracy fix (<= 16m) is present, finish immediately.
        // At 1.6s: If good fix (<= 24m) is present, finish immediately.
        // At maxWaitMs (2.4s total): Finish with the best candidate among all received.
        val supervisorJob = CoroutineScope(Dispatchers.Main).launch {
            delay(800L)
            if (!hasCompleted.get()) {
                val highAcc = synchronized(candidates) {
                    candidates.filter { it.accuracy <= 16f }.minByOrNull { it.accuracy }
                }
                if (highAcc != null) {
                    completeWith(highAcc)
                    return@launch
                }
            }

            delay(800L) // 1.6s total
            if (!hasCompleted.get()) {
                val goodAcc = synchronized(candidates) {
                    candidates.filter { it.accuracy <= 24f }.minByOrNull { it.accuracy }
                }
                if (goodAcc != null) {
                    completeWith(goodAcc)
                    return@launch
                }
            }

            val remainingWait = (maxWaitMs - 1_600L).coerceAtLeast(0L)
            if (remainingWait > 0) {
                delay(remainingWait)
            }
            if (!hasCompleted.get()) {
                val best = synchronized(candidates) {
                    candidates.minByOrNull { it.accuracy }
                }
                completeWith(best)
            }
        }

        try {
            deferred.await()
        } finally {
            supervisorJob.cancel()
            try {
                fusedClient?.removeLocationUpdates(fusedCallback)
                locationManager?.removeUpdates(sysListener)
            } catch (_: Throwable) {}
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? = withContext(Dispatchers.IO) {
        // 1. Try Google Play Services FusedLocationProviderClient
        try {
            val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
            
            // Check fresh cached location first
            val lastLoc = withTimeoutOrNull(1500L) {
                suspendCancellableCoroutine<Location?> { cont ->
                    fusedClient.lastLocation
                        .addOnSuccessListener { loc ->
                            if (cont.isActive) cont.resume(loc)
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                }
            }

            // If last location is very recent (< 2 minutes old), return it immediately
            if (lastLoc != null && (System.currentTimeMillis() - lastLoc.time) < 120_000L) {
                persistLocation(context, lastLoc)
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
                persistLocation(context, freshLoc)
                return@withContext freshLoc
            }

            if (lastLoc != null) {
                persistLocation(context, lastLoc)
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
                persistLocation(context, callbackLoc)
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
                    val bestLoc = validLocs.minByOrNull { it.accuracy }
                    if (bestLoc != null) {
                        persistLocation(context, bestLoc)
                        return@withContext bestLoc
                    }
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
                    persistLocation(context, activeSysLoc)
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
                    result.lastLocation?.let {
                        lastKnownLocation = it
                        persistLocation(context, it)
                        onLocationChanged(it)
                    }
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

    fun isRecognizedStreetName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val trimmed = name.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        val unnamedKeywords = listOf(
            "unnamed", "unknown", "sin nombre", "sans nom", "ohne namen",
            "ανώνυμ", "ανωνυμ", "χωρίς όνομα", "χωρις ονομα", "unnamed road",
            "unknown road", "road", "street", "οδός", "οδος", "δρόμος", "δρομος"
        )
        if (unnamedKeywords.any { lower == it || lower.startsWith("unnamed") || lower.startsWith("ανώνυμ") || lower.startsWith("ανωνυμ") }) {
            return false
        }

        // Plus Codes (e.g. "8GG2+3X", "8GG2+3X Thessaloniki")
        if (trimmed.contains("+") && trimmed.matches(Regex("""^[A-Z0-9]{4,8}\+[A-Z0-9]{2,5}.*"""))) {
            return false
        }

        // Coordinate strings or pure numbers/symbols
        if (trimmed.matches(Regex("""^[\d\s.,°NSEWnsew\-+/]+$"""))) {
            return false
        }

        // Must contain at least 2 letters
        val letterCount = trimmed.count { it.isLetter() }
        if (letterCount < 2) {
            return false
        }

        return true
    }

    fun isCoordinatesString(str: String?): Boolean {
        if (str.isNullOrBlank()) return true
        val trimmed = str.trim()
        if (trimmed.contains("°") || trimmed.contains("° N") || trimmed.contains("° S")) return true
        if (trimmed.startsWith("GPS:") || trimmed.startsWith("Lat:") || trimmed.startsWith("Location:")) return true
        if (trimmed.matches(Regex("""^[\d\s.,°NSEWnsew\-+/]+$"""))) return true
        if (trimmed == "Fetching address..." || trimmed == "Address not found") return true
        return false
    }

    private fun extractStreetFromAddress(addr: Address): String? {
        val thoroughfare = addr.thoroughfare?.trim()?.takeIf { it.isNotBlank() }
        val streetCandidate = when {
            isRecognizedStreetName(thoroughfare) -> thoroughfare
            else -> {
                val firstPart = addr.getAddressLine(0)?.split(",")?.firstOrNull()?.trim()
                if (isRecognizedStreetName(firstPart)) firstPart else null
            }
        } ?: return null

        val houseNumber = addr.subThoroughfare?.trim()?.takeIf { it.isNotBlank() }
        val locality = addr.locality?.trim()?.takeIf { it.isNotBlank() }
            ?: addr.subLocality?.trim()?.takeIf { it.isNotBlank() }

        val streetWithNumber = if (houseNumber != null && !streetCandidate.contains(houseNumber)) {
            val line = addr.getAddressLine(0)
            if (line != null && line.startsWith(houseNumber)) {
                "$houseNumber $streetCandidate"
            } else {
                "$streetCandidate $houseNumber"
            }
        } else {
            streetCandidate
        }

        return if (locality != null && !streetWithNumber.contains(locality)) {
            "$streetWithNumber, $locality"
        } else {
            streetWithNumber
        }
    }

    /**
     * Resolves the street name for a given location.
     * When the location is on a street, returns the street name (with house number and locality if available).
     * When the location is in a remote area without street names (mountains, wilderness, beach, unnamed paths),
     * returns clean coordinates format (e.g. "40.63223° N, 22.95485° E").
     */
    suspend fun getStreetNameOrCoordinates(
        context: Context,
        lat: Double,
        lng: Double
    ): String = withContext(Dispatchers.IO) {
        if (lat == 0.0 && lng == 0.0) return@withContext formatCoordinates(lat, lng)

        // 1. Try Android System Geocoder
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val geocoderResult = withTimeoutOrNull(2500L) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine<String?> { cont ->
                            geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) {
                                    if (addresses.isNotEmpty()) {
                                        cont.resume(extractStreetFromAddress(addresses[0]))
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
                            extractStreetFromAddress(addresses[0])
                        } else null
                    }
                }
                if (!geocoderResult.isNullOrBlank()) {
                    return@withContext geocoderResult
                }
            }
        } catch (_: Throwable) {
            // Geocoder service error or timeout, proceed to Nominatim fallback
        }

        // 2. HTTP Reverse Geocode Fallback (OpenStreetMap Nominatim)
        try {
            val urlStr = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lng&zoom=18&addressdetails=1"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 2500
                readTimeout = 2500
                setRequestProperty("User-Agent", "PixelParkingApp/1.0 (Android AutoPark)")
            }
            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(json)
                val addressObj = obj.optJSONObject("address")
                val road = addressObj?.optString("road")?.takeIf { it.isNotBlank() }
                    ?: addressObj?.optString("pedestrian")?.takeIf { it.isNotBlank() }
                    ?: addressObj?.optString("residential")?.takeIf { it.isNotBlank() }
                    ?: addressObj?.optString("street")?.takeIf { it.isNotBlank() }
                    ?: addressObj?.optString("footway")?.takeIf { it.isNotBlank() }

                if (isRecognizedStreetName(road)) {
                    val houseNumber = addressObj?.optString("house_number")?.trim()?.takeIf { it.isNotBlank() }
                    val city = addressObj?.optString("city")
                        ?: addressObj?.optString("town")
                        ?: addressObj?.optString("suburb")
                        ?: addressObj?.optString("village")

                    val streetWithNumber = if (houseNumber != null && !road!!.contains(houseNumber)) {
                        "$road $houseNumber"
                    } else {
                        road!!
                    }
                    val formatted = if (!city.isNullOrBlank() && !streetWithNumber.contains(city)) {
                        "$streetWithNumber, $city"
                    } else {
                        streetWithNumber
                    }
                    return@withContext formatted
                }
            }
        } catch (_: Throwable) {
            // Fallback to coordinates
        }

        // 3. Fallback: remote location without street names -> coordinates
        formatCoordinates(lat, lng)
    }

    suspend fun getAddressFromCoordinates(
        context: Context,
        lat: Double,
        lng: Double
    ): String = getStreetNameOrCoordinates(context, lat, lng)

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
        try {
            val results = FloatArray(1)
            Location.distanceBetween(lat1, lng1, lat2, lng2, results)
            return results[0]
        } catch (_: Throwable) {
            // Pure Math Haversine fallback for unit testing environments
            val r = 6371000.0 // Earth radius in meters
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLng / 2) * sin(dLng / 2)
            val c = 2 * atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
            return (r * c).toFloat()
        }
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

    fun formatDistanceWithUnit(meters: Float, unit: DistanceUnit): String {
        return if (unit == DistanceUnit.IMPERIAL) {
            formatDistanceFeet(meters)
        } else {
            formatDistance(meters)
        }
    }
}
