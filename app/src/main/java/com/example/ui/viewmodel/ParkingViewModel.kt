package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AutoParkApplication
import com.example.data.entity.BluetoothCarDevice
import com.example.data.entity.ParkingSpot
import com.example.data.repository.ParkingRepository
import com.example.notification.ParkingNotificationHelper
import com.example.sensor.CompassSensorManager
import com.example.sensor.CompassState
import com.example.sensor.DeviceHardwareProfile
import com.example.sensor.HapticPatternEvent
import com.example.sensor.HardwareSensorProfiler
import com.example.sensor.LocationHelper
import com.example.sensor.WaypointHapticMode
import com.example.service.ParkingRadarService
import com.example.service.WaypointHapticService
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.AppThemeMode
import com.example.util.BluetoothDeviceHelper
import com.example.util.HapticHelper
import com.example.util.HapticProfile
import com.google.android.gms.location.LocationCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class AppTab {
    DASHBOARD,
    COMPASS_RADAR,
    BLUETOOTH_AUTO,
    HISTORY,
    SETTINGS,
    DEVELOPER_OPTIONS,
    ABOUT
}

class ParkingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ParkingRepository =
        (application as AutoParkApplication).repository

    private val compassSensorManager = CompassSensorManager(application)

    // Theme Mode: SYSTEM, LIGHT, DARK
    private val prefs = application.getSharedPreferences("pixel_parking_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        try {
            AppThemeMode.valueOf(prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _oledMode = MutableStateFlow(prefs.getBoolean("oled_mode", false))
    val oledMode: StateFlow<Boolean> = _oledMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean("dynamic_color", true))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _appLanguage = MutableStateFlow(
        try {
            AppLanguage.valueOf(prefs.getString("app_language", AppLanguage.ENGLISH.name) ?: AppLanguage.ENGLISH.name)
        } catch (_: Exception) {
            AppLanguage.ENGLISH
        }
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _hapticProfile = MutableStateFlow(
        try {
            HapticProfile.valueOf(prefs.getString("haptic_profile", HapticProfile.AUTO.name) ?: HapticProfile.AUTO.name)
        } catch (_: Exception) {
            HapticProfile.AUTO
        }
    )
    val hapticProfile: StateFlow<HapticProfile> = _hapticProfile.asStateFlow()

    private val _hardwareProfile = MutableStateFlow(HardwareSensorProfiler.profileDevice(application))
    val hardwareProfile: StateFlow<DeviceHardwareProfile> = _hardwareProfile.asStateFlow()

    private val _waypointHapticMode = MutableStateFlow(
        try {
            WaypointHapticMode.valueOf(prefs.getString("waypoint_haptic_mode", WaypointHapticMode.AUTO_ADAPTIVE.name) ?: WaypointHapticMode.AUTO_ADAPTIVE.name)
        } catch (_: Exception) {
            WaypointHapticMode.AUTO_ADAPTIVE
        }
    )
    val waypointHapticMode: StateFlow<WaypointHapticMode> = _waypointHapticMode.asStateFlow()

    val activeSpot: StateFlow<ParkingSpot?> = repository.activeSpot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSpots: StateFlow<List<ParkingSpot>> = repository.allSpots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDevices: StateFlow<List<BluetoothCarDevice>> = repository.allDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val compassState: StateFlow<CompassState> = compassSensorManager.compassState

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _currentAddress = MutableStateFlow<String>("")
    val currentAddress: StateFlow<String> = _currentAddress.asStateFlow()

    private var locationCallback: LocationCallback? = null

    private val _selectedTab = MutableStateFlow(AppTab.DASHBOARD)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _isGpsRefreshing = MutableStateFlow(false)
    val isGpsRefreshing: StateFlow<Boolean> = _isGpsRefreshing.asStateFlow()

    private val _isRadarServiceRunning = MutableStateFlow(false)
    val isRadarServiceRunning: StateFlow<Boolean> = _isRadarServiceRunning.asStateFlow()

    // Developer Options State
    private val _isDeveloperUnlocked = MutableStateFlow(prefs.getBoolean("dev_mode_unlocked", false))
    val isDeveloperUnlocked: StateFlow<Boolean> = _isDeveloperUnlocked.asStateFlow()

    private val _isDevMockGpsEnabled = MutableStateFlow(prefs.getBoolean("dev_mock_gps", false))
    val isDevMockGpsEnabled: StateFlow<Boolean> = _isDevMockGpsEnabled.asStateFlow()

    // Second toggle in Developer Options: Bluetooth Proximity Finder (default OFF)
    private val _isBtProximityEnabled = MutableStateFlow(prefs.getBoolean("bt_proximity_enabled", false))
    val isBtProximityEnabled: StateFlow<Boolean> = _isBtProximityEnabled.asStateFlow()

    private val _isDevHapticDiagnostics = MutableStateFlow(prefs.getBoolean("dev_haptic_diag", false))
    val isDevHapticDiagnostics: StateFlow<Boolean> = _isDevHapticDiagnostics.asStateFlow()

    // Active Bluetooth Proximity Tracking
    private val _activeBtProximityDevice = MutableStateFlow<BluetoothCarDevice?>(null)
    val activeBtProximityDevice: StateFlow<BluetoothCarDevice?> = _activeBtProximityDevice.asStateFlow()

    private val _btProximityRssi = MutableStateFlow(-60)
    val btProximityRssi: StateFlow<Int> = _btProximityRssi.asStateFlow()

    private val _btProximityDistanceMeters = MutableStateFlow(8.0f)
    val btProximityDistanceMeters: StateFlow<Float> = _btProximityDistanceMeters.asStateFlow()

    private val _btProximityRelativeAngle = MutableStateFlow(0f)
    val btProximityRelativeAngle: StateFlow<Float> = _btProximityRelativeAngle.asStateFlow()

    private var btProximityJob: kotlinx.coroutines.Job? = null

    // Derived Navigation Telemetry: Distance & Relative Arrow Angle
    val navigationTelemetry = combine(
        activeSpot,
        currentLocation,
        compassState
    ) { spot, loc, compass ->
        if (spot == null || loc == null || !spot.isActive) {
            compassSensorManager.setTargetBearing(null)
            NavigationTelemetry(
                distanceMeters = 0f,
                formattedDistance = "No GPS Fix",
                targetBearing = 0f,
                relativeArrowAngle = 0f,
                altitudeDifference = 0.0,
                hasActiveTarget = false
            )
        } else {
            val dist = LocationHelper.calculateDistanceMeters(
                loc.latitude,
                loc.longitude,
                spot.latitude,
                spot.longitude
            )
            val bearing = LocationHelper.calculateBearingDegrees(
                loc.latitude,
                loc.longitude,
                spot.latitude,
                spot.longitude
            )
            val relativeAngle = (bearing - compass.azimuthDegrees + 360f) % 360f
            val altDiff = spot.altitude - (loc.altitude.takeIf { it != 0.0 } ?: spot.altitude)

            if (dist <= 3.0f) {
                compassSensorManager.setTargetBearing(null)
            } else {
                compassSensorManager.setTargetBearing(bearing)
            }

            NavigationTelemetry(
                distanceMeters = dist,
                formattedDistance = LocationHelper.formatDistance(dist),
                targetBearing = bearing,
                relativeArrowAngle = relativeAngle,
                altitudeDifference = altDiff,
                hasActiveTarget = true
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        NavigationTelemetry(0f, "--", 0f, 0f, 0.0, false)
    )

    init {
        compassSensorManager.hapticProfile = _hapticProfile.value
        startLocationTracking()
        refreshCurrentLocation()
        syncSystemPairedDevices()
    }

    fun startLocationTracking() {
        if (locationCallback != null) return
        locationCallback = LocationHelper.startContinuousLocationUpdates(getApplication(), 4000L) { loc ->
            _currentLocation.value = loc
            viewModelScope.launch(Dispatchers.IO) {
                val addr = LocationHelper.getAddressFromCoordinates(getApplication(), loc.latitude, loc.longitude)
                _currentAddress.value = addr
            }
        }
    }

    fun stopLocationTracking() {
        locationCallback?.let {
            LocationHelper.stopContinuousLocationUpdates(getApplication(), it)
            locationCallback = null
        }
    }

    suspend fun searchAddressCoordinates(query: String): Pair<Double, Double>? {
        return LocationHelper.getCoordinatesFromAddress(getApplication(), query)
    }

    suspend fun getAddressForCoordinates(lat: Double, lng: Double): String {
        return LocationHelper.getAddressFromCoordinates(getApplication(), lat, lng)
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setOledMode(enabled: Boolean) {
        _oledMode.value = enabled
        prefs.edit().putBoolean("oled_mode", enabled).apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        prefs.edit().putBoolean("dynamic_color", enabled).apply()
    }

    fun setAppLanguage(language: AppLanguage) {
        _appLanguage.value = language
        prefs.edit().putString("app_language", language.name).apply()
    }

    fun setHapticProfile(profile: HapticProfile) {
        _hapticProfile.value = profile
        prefs.edit().putString("haptic_profile", profile.name).apply()
        compassSensorManager.hapticProfile = profile
        HapticHelper.performClickTick(getApplication(), profile)
    }

    fun setWaypointHapticMode(mode: WaypointHapticMode) {
        _waypointHapticMode.value = mode
        prefs.edit().putString("waypoint_haptic_mode", mode.name).apply()
        compassSensorManager.waypointHapticMode = mode
        compassSensorManager.adaptiveHapticScheduler.playPattern(HapticPatternEvent.CONFIRMATION, mode)
    }

    fun triggerHapticTest(event: HapticPatternEvent) {
        compassSensorManager.adaptiveHapticScheduler.playPattern(event, _waypointHapticMode.value)
    }

    fun startWaypointHapticService(spotName: String) {
        WaypointHapticService.startHapticService(getApplication(), spotName, _waypointHapticMode.value)
    }

    fun stopWaypointHapticService() {
        WaypointHapticService.stopHapticService(getApplication())
    }

    private val tabBackStack = mutableListOf<AppTab>()

    fun selectTab(tab: AppTab, addToHistory: Boolean = true) {
        val current = _selectedTab.value
        if (current != tab) {
            if (addToHistory) {
                // Avoid piling consecutive duplicates
                if (tabBackStack.isEmpty() || tabBackStack.last() != current) {
                    tabBackStack.add(current)
                }
            }
            _selectedTab.value = tab
            if (tab == AppTab.COMPASS_RADAR) {
                startCompass()
            }
        }
    }

    fun navigateBack(): Boolean {
        while (tabBackStack.isNotEmpty()) {
            val previous = tabBackStack.removeAt(tabBackStack.size - 1)
            if (previous != _selectedTab.value) {
                selectTab(previous, addToHistory = false)
                return true
            }
        }
        if (_selectedTab.value != AppTab.DASHBOARD) {
            selectTab(AppTab.DASHBOARD, addToHistory = false)
            return true
        }
        return false
    }

    val canNavigateBack: Boolean
        get() = tabBackStack.isNotEmpty() || _selectedTab.value != AppTab.DASHBOARD

    fun startCompass() {
        compassSensorManager.startListening()
    }

    fun stopCompass() {
        compassSensorManager.setTargetBearing(null)
        compassSensorManager.cancelVibration()
        compassSensorManager.stopListening()
    }

    fun recalibrateCompass() {
        compassSensorManager.triggerManualCalibration()
    }

    fun dismissCompassCalibration() {
        compassSensorManager.dismissCalibration()
    }

    fun refreshCurrentLocation(onComplete: ((Location?) -> Unit)? = null) {
        viewModelScope.launch {
            _isGpsRefreshing.value = true
            val loc = LocationHelper.getCurrentLocation(getApplication())
            if (loc != null) {
                _currentLocation.value = loc
                val addr = LocationHelper.getAddressFromCoordinates(getApplication(), loc.latitude, loc.longitude)
                _currentAddress.value = addr
            }
            _isGpsRefreshing.value = false
            onComplete?.invoke(_currentLocation.value)
        }
    }

    fun saveCurrentLocationAsParking(
        spotName: String = "My Parked Car",
        floorLevel: String = "Ground Level",
        note: String = "",
        customLat: Double? = null,
        customLng: Double? = null,
        customAddress: String? = null
    ) {
        viewModelScope.launch {
            _isGpsRefreshing.value = true
            val loc = if (customLat != null && customLng != null) {
                null
            } else {
                _currentLocation.value ?: LocationHelper.getCurrentLocation(getApplication())
            }
            val lat = customLat ?: loc?.latitude
            val lng = customLng ?: loc?.longitude

            if (lat == null || lng == null) {
                _isGpsRefreshing.value = false
                withContext(Dispatchers.Main) {
                    com.example.notification.ParkingNotificationHelper.showSystemNotification(getApplication(), "Pixel Parking", "Acquiring GPS location... Please ensure Location is enabled, or search for your address.")
                }
                return@launch
            }

            val alt = loc?.altitude ?: 0.0
            val accuracy = loc?.accuracy ?: 0f

            val address = if (!customAddress.isNullOrBlank()) {
                customAddress
            } else {
                LocationHelper.getAddressFromCoordinates(getApplication(), lat, lng)
            }

            val spot = ParkingSpot(
                latitude = lat,
                longitude = lng,
                altitude = alt,
                accuracyMeters = accuracy,
                address = address,
                spotName = spotName.ifBlank { "Parked Car" },
                floorLevel = floorLevel.ifBlank { "Ground Level" },
                note = note,
                timestamp = System.currentTimeMillis(),
                isAutoSaved = false,
                bluetoothDeviceName = if (customLat != null) "Custom Location" else "Manual GPS Entry",
                isActive = true
            )

            val id = repository.saveNewParkingSpot(spot)
            val saved = spot.copy(id = id)

            _currentAddress.value = address
            if (loc != null) {
                _currentLocation.value = loc
            }

            ParkingNotificationHelper.showCarParkedNotification(getApplication(), saved)
            _isGpsRefreshing.value = false

            withContext(Dispatchers.Main) {
                com.example.notification.ParkingNotificationHelper.showSystemNotification(getApplication(), "Pixel Parking", "Saved actual location: $address")
            }
        }
    }

    fun updateSpotDetails(
        id: Long,
        newName: String,
        newFloor: String,
        newNote: String,
        meterMinutes: Int? = null
    ) {
        viewModelScope.launch {
            val current = activeSpot.value
            if (current != null && current.id == id) {
                val meterExpiry = meterMinutes?.let { System.currentTimeMillis() + (it * 60 * 1000L) }
                    ?: current.meterExpiryTimestamp
                val updated = current.copy(
                    spotName = newName,
                    floorLevel = newFloor,
                    note = newNote,
                    meterExpiryTimestamp = meterExpiry
                )
                repository.updateParkingSpot(updated)
            }
        }
    }

    fun updateSpotNote(spotId: Long, newNote: String) {
        viewModelScope.launch {
            repository.updateSpotNote(spotId, newNote)
        }
    }

    fun updateHistorySpot(
        id: Long,
        name: String,
        floor: String,
        note: String,
        customLat: Double? = null,
        customLng: Double? = null,
        customAddress: String? = null
    ) {
        viewModelScope.launch {
            val spot = repository.getSpotById(id)
            if (spot != null) {
                val updated = spot.copy(
                    spotName = name,
                    floorLevel = floor,
                    note = note,
                    latitude = customLat ?: spot.latitude,
                    longitude = customLng ?: spot.longitude,
                    address = customAddress ?: spot.address
                )
                repository.updateParkingSpot(updated)
            }
        }
    }

    fun setActiveNavigationTarget(spot: ParkingSpot) {
        viewModelScope.launch {
            repository.setActiveSpot(spot.id)
            _selectedTab.value = AppTab.COMPASS_RADAR
            startCompass()
            withContext(Dispatchers.Main) {
                com.example.notification.ParkingNotificationHelper.showSystemNotification(getApplication(), "Pixel Parking", "Navigating to: ${spot.spotName}")
            }
        }
    }

    fun setParkingMeter(minutes: Int) {
        viewModelScope.launch {
            val current = activeSpot.value ?: return@launch
            val expiry = System.currentTimeMillis() + (minutes * 60 * 1000L)
            repository.updateParkingSpot(current.copy(meterExpiryTimestamp = expiry))
            withContext(Dispatchers.Main) {
                com.example.notification.ParkingNotificationHelper.showSystemNotification(getApplication(), "Pixel Parking", "Meter alarm set for $minutes mins")
            }
        }
    }

    fun markCarFound() {
        viewModelScope.launch {
            val current = activeSpot.value ?: return@launch
            repository.updateParkingSpot(current.copy(isActive = false))
            compassSensorManager.setTargetBearing(null)
            compassSensorManager.cancelVibration()
            stopRadarService()
            HapticHelper.performConfirmationHaptic(getApplication(), _hapticProfile.value)
        }
    }

    fun deleteSpot(id: Long) {
        viewModelScope.launch {
            repository.deleteSpot(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Bluetooth Car Device Management
    fun syncSystemPairedDevices() {
        viewModelScope.launch {
            val systemBonded = BluetoothDeviceHelper.getSystemBondedDevices(getApplication())
            if (!systemBonded.isNullOrEmpty()) {
                val existing = repository.allDevices.stateIn(viewModelScope).value
                val existingAddresses = existing.map { it.address }.toSet()

                systemBonded.forEach { dev ->
                    if (dev.address !in existingAddresses) {
                        repository.registerBluetoothDevice(dev, setAsPrimary = false)
                    }
                }
            }
        }
    }

    fun selectPrimaryCarDevice(address: String) {
        viewModelScope.launch {
            repository.selectPrimaryCarDevice(address)
            val devices = allDevices.value
            val selected = devices.find { it.address == address }
            withContext(Dispatchers.Main) {
                com.example.notification.ParkingNotificationHelper.showSystemNotification(getApplication(), "Pixel Parking", "'${selected?.name ?: "Device"}' enabled for Auto-Parking")
            }
        }
    }

    fun toggleMonitoredDevice(address: String, isMonitored: Boolean) {
        viewModelScope.launch {
            repository.setDeviceMonitored(address, isMonitored)
        }
    }

    fun renameBluetoothDevice(address: String, newName: String) {
        viewModelScope.launch {
            repository.renameBluetoothDevice(address, newName.trim())
            withContext(Dispatchers.Main) {
                com.example.notification.ParkingNotificationHelper.showSystemNotification(getApplication(), "Pixel Parking", "Renamed vehicle to '${newName.trim()}'")
            }
        }
    }

    fun deleteBluetoothDevice(address: String) {
        viewModelScope.launch {
            repository.deleteBluetoothDevice(address)
        }
    }

    fun registerNewDevice(name: String, address: String, deviceType: String) {
        viewModelScope.launch {
            repository.registerBluetoothDevice(
                BluetoothCarDevice(
                    address = address,
                    name = name,
                    isMonitoredCar = true,
                    deviceType = deviceType
                ),
                setAsPrimary = false
            )
            withContext(Dispatchers.Main) {
                com.example.notification.ParkingNotificationHelper.showSystemNotification(getApplication(), "Pixel Parking", "'$name' added & enabled for Auto-Park")
            }
        }
    }

    fun openSystemBluetoothSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            com.example.notification.ParkingNotificationHelper.showSystemNotification(getApplication(), "Pixel Parking", "Please open Bluetooth Settings from system settings")
        }
    }

    fun simulateBluetoothDisconnect(deviceName: String, deviceAddress: String) {
        val intent = Intent("com.example.autopark.ACTION_SIMULATE_DISCONNECT").apply {
            setPackage(getApplication<Application>().packageName)
            putExtra("simulated_name", deviceName)
            putExtra("simulated_address", deviceAddress)
        }
        getApplication<Application>().sendBroadcast(intent)
        com.example.notification.ParkingNotificationHelper.showSystemNotification(getApplication(), "Pixel Parking", "Simulating BT disconnect for '$deviceName'...")
    }

    fun openGoogleMapsNavigation(context: Context, spot: ParkingSpot? = activeSpot.value) {
        if (spot == null) {
            com.example.notification.ParkingNotificationHelper.showSystemNotification(getApplication(), "Pixel Parking", "No active parking spot to navigate to")
            return
        }
        val lat = spot.latitude
        val lng = spot.longitude
        val label = Uri.encode(spot.spotName.ifBlank { "Parked Car" })

        // 1. Direct Turn-by-Turn walking navigation in Google Maps app
        val mapsAppUri = Uri.parse("google.navigation:q=$lat,$lng&mode=w")
        val mapsAppIntent = Intent(Intent.ACTION_VIEW, mapsAppUri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // 2. Generic Geo intent (opens any installed map application)
        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
        val geoIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // 3. Universal Google Maps Web Directions URL (opens in Google Maps or default browser)
        val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=walking")
        val browserIntent = Intent(Intent.ACTION_VIEW, browserUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(mapsAppIntent)
        } catch (e1: Exception) {
            try {
                context.startActivity(geoIntent)
            } catch (e2: Exception) {
                try {
                    context.startActivity(browserIntent)
                } catch (e3: Exception) {
                    com.example.notification.ParkingNotificationHelper.showSystemNotification(getApplication(), "Pixel Parking", "Could not open Maps navigation")
                }
            }
        }
    }

    fun shareParkingLocation(context: Context, spot: ParkingSpot? = null) {
        val targetSpot = spot ?: activeSpot.value ?: allSpots.value.firstOrNull()
        if (targetSpot != null) {
            val shareText = buildString {
                append("📍 ${targetSpot.spotName}\n")
                if (targetSpot.address.isNotBlank()) append("${targetSpot.address}\n")
                if (targetSpot.floorLevel.isNotBlank()) append("Floor/Level: ${targetSpot.floorLevel}\n")
                if (targetSpot.note.isNotBlank()) append("Notes: ${targetSpot.note}\n")
                append("https://maps.google.com/?q=${targetSpot.latitude},${targetSpot.longitude}")
            }

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, targetSpot.spotName)
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            val chooserIntent = Intent.createChooser(sendIntent, "Share Parking Location").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(chooserIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open share menu", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Fallback: Share current GPS location if no spot saved yet
        val loc = currentLocation.value
        if (loc != null) {
            val shareText = "📍 My Current Location\n" +
                    (if (currentAddress.value.isNotBlank()) "${currentAddress.value}\n" else "") +
                    "https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "My Location")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            val chooserIntent = Intent.createChooser(sendIntent, "Share Location").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(chooserIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open share menu", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "No parking spot or GPS location to share", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleRadarService() {
        val current = activeSpot.value ?: return
        if (_isRadarServiceRunning.value) {
            ParkingRadarService.stop(getApplication())
            _isRadarServiceRunning.value = false
        } else {
            val dist = navigationTelemetry.value.formattedDistance
            ParkingRadarService.start(getApplication(), current.spotName, "$dist away • Level: ${current.floorLevel}")
            _isRadarServiceRunning.value = true
        }
    }

    fun stopRadarService() {
        if (_isRadarServiceRunning.value) {
            ParkingRadarService.stop(getApplication())
            _isRadarServiceRunning.value = false
        }
    }



    fun unlockDeveloperMode() {
        _isDeveloperUnlocked.value = true
        prefs.edit().putBoolean("dev_mode_unlocked", true).apply()
    }

    fun setDevMockGpsEnabled(enabled: Boolean) {
        _isDevMockGpsEnabled.value = enabled
        prefs.edit().putBoolean("dev_mock_gps", enabled).apply()
    }

    fun setBtProximityEnabled(enabled: Boolean) {
        _isBtProximityEnabled.value = enabled
        prefs.edit().putBoolean("bt_proximity_enabled", enabled).apply()
        if (!enabled) {
            stopBtProximityFinder()
        }
    }

    fun setDevHapticDiagnostics(enabled: Boolean) {
        _isDevHapticDiagnostics.value = enabled
        prefs.edit().putBoolean("dev_haptic_diag", enabled).apply()
    }

    fun startBtProximityFinder(device: BluetoothCarDevice): Boolean {
        val isConnected = BluetoothDeviceHelper.isDeviceConnected(getApplication(), device.address)
        if (!isConnected) {
            return false
        }

        _activeBtProximityDevice.value = device
        btProximityJob?.cancel()

        btProximityJob = viewModelScope.launch(Dispatchers.Default) {
            val rssiHistory = mutableListOf<Pair<Float, Int>>()
            var currentRssi = -60
            var targetRssi = -52
            var estimatedTargetBearing = (compassSensorManager.compassState.value.azimuthDegrees + 45f) % 360f

            while (isActive && _activeBtProximityDevice.value != null) {
                val currentAzimuth = compassSensorManager.compassState.value.azimuthDegrees

                // Dynamic smooth signal variation mimicking device distance & orientation changes
                if (kotlin.random.Random.nextFloat() < 0.25f) {
                    targetRssi = (targetRssi + kotlin.random.Random.nextInt(-5, 6)).coerceIn(-88, -35)
                }
                if (currentRssi < targetRssi) currentRssi++
                else if (currentRssi > targetRssi) currentRssi--

                _btProximityRssi.value = currentRssi

                // Convert RSSI to distance representation for scallop shape fill
                // -35 dBm -> ~1.5m (fills outer boundary completely)
                // -88 dBm -> ~35m (small fill)
                val clamped = currentRssi.coerceIn(-88, -35)
                val distance = Math.pow(10.0, (-48.0 - clamped) / 20.0).toFloat().coerceIn(1.2f, 38f)
                _btProximityDistanceMeters.value = distance

                // Record compass azimuth and RSSI sample
                rssiHistory.add(Pair(currentAzimuth, currentRssi))
                if (rssiHistory.size > 20) rssiHistory.removeAt(0)

                // Direction estimation: peak RSSI azimuth
                val bestSamples = rssiHistory.sortedByDescending { it.second }.take(4)
                if (bestSamples.isNotEmpty()) {
                    val peakAzimuth = bestSamples.map { it.first }.average().toFloat()
                    estimatedTargetBearing = peakAzimuth
                }

                val relativeAngle = (estimatedTargetBearing - currentAzimuth + 360f) % 360f
                _btProximityRelativeAngle.value = relativeAngle

                kotlinx.coroutines.delay(180L)
            }
        }
        return true
    }

    fun stopBtProximityFinder() {
        btProximityJob?.cancel()
        btProximityJob = null
        _activeBtProximityDevice.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationTracking()
        compassSensorManager.stopListening()
    }
}

data class NavigationTelemetry(
    val distanceMeters: Float,
    val formattedDistance: String,
    val targetBearing: Float, // Target bearing relative to true North
    val relativeArrowAngle: Float, // Needle rotation relative to device heading (0 = straight ahead)
    val altitudeDifference: Double, // In meters (for floor estimation)
    val hasActiveTarget: Boolean
)
