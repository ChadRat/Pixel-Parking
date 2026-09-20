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
import com.example.sensor.BluetoothProximityManager
import com.example.sensor.BtProximityState
import com.example.sensor.BtSignalTier
import com.example.sensor.CompassSensorManager
import com.example.sensor.CompassState
import com.example.sensor.DeviceHardwareProfile
import com.example.sensor.HapticPatternEvent
import com.example.sensor.HardwareSensorProfiler
import com.example.sensor.LocationHelper
import com.example.sensor.SunCalculator
import com.example.sensor.WaypointHapticMode
import com.example.service.ParkingRadarService
import com.example.service.WaypointHapticService
import com.example.ui.components.CarBadgeStyle
import com.example.ui.components.CapyVariant
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.getAppStrings
import com.example.ui.i18n.localizeFloor
import com.example.ui.i18n.localizeSpotName
import com.example.ui.theme.AppThemeMode
import com.example.util.BluetoothDeviceHelper
import com.example.util.HapticHelper
import com.example.util.HapticProfile
import com.example.wear.WearDataLayerBridge
import com.google.android.gms.location.LocationCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    private val bluetoothProximityManager = BluetoothProximityManager(application, compassSensorManager)

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

    private val _autoSunTheme = MutableStateFlow(prefs.getBoolean("auto_sun_theme", false))
    val autoSunTheme: StateFlow<Boolean> = _autoSunTheme.asStateFlow()

    private val _isDaytime = MutableStateFlow(true)
    val isDaytime: StateFlow<Boolean> = _isDaytime.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean("dynamic_color", true))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()


    private val _carBadgeStyle = MutableStateFlow(
        try {
            CarBadgeStyle.valueOf(prefs.getString("car_badge_style", CarBadgeStyle.CINEMATIC.name) ?: CarBadgeStyle.CINEMATIC.name)
        } catch (_: Exception) {
            CarBadgeStyle.CINEMATIC
        }
    )
    val carBadgeStyle: StateFlow<CarBadgeStyle> = _carBadgeStyle.asStateFlow()

    private val _capyVariant = MutableStateFlow(
        try {
            CapyVariant.valueOf(prefs.getString("capy_variant", CapyVariant.BABY.name) ?: CapyVariant.BABY.name)
        } catch (_: Exception) {
            CapyVariant.BABY
        }
    )
    val capyVariant: StateFlow<CapyVariant> = _capyVariant.asStateFlow()

    private val _appLanguage = MutableStateFlow(
        try {
            val systemDefaultLang = if (java.util.Locale.getDefault().language.equals("el", ignoreCase = true)) {
                AppLanguage.GREEK.name
            } else {
                AppLanguage.ENGLISH.name
            }
            AppLanguage.valueOf(prefs.getString("app_language", systemDefaultLang) ?: systemDefaultLang)
        } catch (_: Exception) {
            AppLanguage.ENGLISH
        }
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    val currentStrings: AppStrings
        get() = getAppStrings(_appLanguage.value)

    private val _wearCrownOnRight = MutableStateFlow(prefs.getBoolean("wear_crown_on_right", true))
    val wearCrownOnRight: StateFlow<Boolean> = _wearCrownOnRight.asStateFlow()

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

    private val _isParkingTimerFeatureEnabled = MutableStateFlow(prefs.getBoolean("parking_timer_feature_enabled", false))
    val isParkingTimerFeatureEnabled: StateFlow<Boolean> = _isParkingTimerFeatureEnabled.asStateFlow()

    // Geofencing Auto-Park Feature (Default disabled, toggleable in Dev Settings)
    private val _isGeofenceAutoParkEnabled = MutableStateFlow(prefs.getBoolean("geofence_auto_park_enabled", false))
    val isGeofenceAutoParkEnabled: StateFlow<Boolean> = _isGeofenceAutoParkEnabled.asStateFlow()

    // Capy Cars Option in Developer Options (Default disabled)
    private val _isDevCapyCarsEnabled = MutableStateFlow(prefs.getBoolean("dev_capy_cars_enabled", false))
    val isDevCapyCarsEnabled: StateFlow<Boolean> = _isDevCapyCarsEnabled.asStateFlow()

    // Active Bluetooth Proximity Tracking
    val btProximityState: StateFlow<BtProximityState> = bluetoothProximityManager.proximityState

    val activeBtProximityDevice: StateFlow<BluetoothCarDevice?> = bluetoothProximityManager.proximityState
        .map { it.targetDevice }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val btProximityRssi: StateFlow<Int> = bluetoothProximityManager.proximityState
        .map { it.smoothedRssi }
        .stateIn(viewModelScope, SharingStarted.Eagerly, -100)

    val btProximityDistanceMeters: StateFlow<Float> = bluetoothProximityManager.proximityState
        .map { it.distanceMeters }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 15.0f)

    // --- Parking & Charging Timer Feature ---
    private val _timerTotalSeconds = MutableStateFlow(
        prefs.getLong("parking_timer_total_seconds", 30 * 60L).coerceIn(60L, 24 * 3600L)
    )
    val timerTotalSeconds: StateFlow<Long> = _timerTotalSeconds.asStateFlow()

    private val _timerRemainingSeconds = MutableStateFlow(
        prefs.getLong("parking_timer_total_seconds", 30 * 60L).coerceIn(60L, 24 * 3600L)
    )
    val timerRemainingSeconds: StateFlow<Long> = _timerRemainingSeconds.asStateFlow()

    private val _timerIsRunning = MutableStateFlow(false)
    val timerIsRunning: StateFlow<Boolean> = _timerIsRunning.asStateFlow()

    private val _timerIsPaused = MutableStateFlow(false)
    val timerIsPaused: StateFlow<Boolean> = _timerIsPaused.asStateFlow()

    // Up to 3 reminder alerts before end of timer (e.g. 15, 10, 5 mins)
    private val _timerRemindersMinutes = MutableStateFlow(
        prefs.getString("parking_timer_reminders", "15,10,5")
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.filter { it > 0 }
            ?.take(3)
            ?.ifEmpty { listOf(15, 10, 5) }
            ?: listOf(15, 10, 5)
    )
    val timerRemindersMinutes: StateFlow<List<Int>> = _timerRemindersMinutes.asStateFlow()

    private val _timerAlertsTriggered = MutableStateFlow<Set<Int>>(emptySet())
    val timerAlertsTriggered: StateFlow<Set<Int>> = _timerAlertsTriggered.asStateFlow()

    private val _isAlarmActive = MutableStateFlow(false)
    val isAlarmActive: StateFlow<Boolean> = _isAlarmActive.asStateFlow()

    private val _showTimerDialog = MutableStateFlow(false)
    val showTimerDialog: StateFlow<Boolean> = _showTimerDialog.asStateFlow()

    private val _alarmSoundTitle = MutableStateFlow(com.example.util.AlarmSoundHelper.getAlarmTitle(application))
    val alarmSoundTitle: StateFlow<String> = _alarmSoundTitle.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null

    val btProximityRelativeAngle: StateFlow<Float> = bluetoothProximityManager.proximityState
        .map { it.relativeAngle }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0f)

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

            val isAligned = kotlin.math.abs(relativeAngle) < 15f || relativeAngle > 345f
            if (spot != null && spot.isActive) {
                wearDataLayerBridge.syncLiveTelemetry(
                    distanceMeters = dist,
                    targetBearing = bearing,
                    relativeArrowAngle = relativeAngle,
                    isAligned = isAligned
                )
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

    private val wearDataLayerBridge = WearDataLayerBridge.getInstance(application)

    init {
        compassSensorManager.hapticProfile = _hapticProfile.value
        startLocationTracking()
        refreshCurrentLocation()
        syncSystemPairedDevices()
        updateDaytimeState()

        viewModelScope.launch {
            activeSpot.collect { spot ->
                wearDataLayerBridge.syncParkingSpot(spot)
            }
        }

        viewModelScope.launch {
            wearCrownOnRight.collect { onRight ->
                wearDataLayerBridge.syncCrownPosition(onRight)
            }
        }

        viewModelScope.launch {
            while (isActive) {
                updateDaytimeState()
                kotlinx.coroutines.delay(60_000L)
            }
        }
    }

    fun triggerWearSync() {
        wearDataLayerBridge.syncParkingSpot(activeSpot.value)
    }

    fun startLocationTracking() {
        if (locationCallback != null) return
        locationCallback = LocationHelper.startContinuousLocationUpdates(getApplication(), 4000L) { loc ->
            _currentLocation.value = loc
            updateDaytimeState()
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

    fun setAutoSunTheme(enabled: Boolean) {
        _autoSunTheme.value = enabled
        prefs.edit().putBoolean("auto_sun_theme", enabled).apply()
        updateDaytimeState()
    }

    fun updateDaytimeState() {
        val loc = _currentLocation.value
        val sunTimes = SunCalculator.getSunTimes(loc?.latitude, loc?.longitude)
        _isDaytime.value = sunTimes.isDaytime
    }

    fun setOledMode(enabled: Boolean) {
        _oledMode.value = enabled
        prefs.edit().putBoolean("oled_mode", enabled).apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        prefs.edit().putBoolean("dynamic_color", enabled).apply()
    }

    fun setWearCrownOnRight(onRight: Boolean) {
        _wearCrownOnRight.value = onRight
        prefs.edit().putBoolean("wear_crown_on_right", onRight).apply()
        wearDataLayerBridge.syncCrownPosition(onRight)
    }


    fun setCarBadgeStyle(style: CarBadgeStyle) {
        _carBadgeStyle.value = style
        prefs.edit().putString("car_badge_style", style.name).apply()
    }

    fun setCapyVariant(variant: CapyVariant) {
        _capyVariant.value = variant
        prefs.edit().putString("capy_variant", variant.name).apply()
    }

    fun setAppLanguage(language: AppLanguage) {
        _appLanguage.value = language
        prefs.edit().putString("app_language", language.name).apply()
        ParkingNotificationHelper.createNotificationChannels(getApplication())
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

    fun handleQsTileLongPress() {
        viewModelScope.launch {
            val active = repository.getActiveSpotDirect()
            if (active != null && active.isActive) {
                selectTab(AppTab.COMPASS_RADAR)
            } else {
                selectTab(AppTab.HISTORY)
            }
        }
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
                updateDaytimeState()
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
                    com.example.notification.ParkingNotificationHelper.showSystemNotification(
                        getApplication(),
                        currentStrings.appName,
                        currentStrings.notificationGpsAcquiring
                    )
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
                com.example.notification.ParkingNotificationHelper.showSystemNotification(
                    getApplication(),
                    currentStrings.appName,
                    String.format(currentStrings.notificationLocationSaved, address)
                )
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
                val targetName = spot.spotName.ifBlank { currentStrings.parkedVehicle }
                com.example.notification.ParkingNotificationHelper.showSystemNotification(
                    getApplication(),
                    currentStrings.appName,
                    String.format(currentStrings.notificationNavigatingTo, targetName)
                )
            }
        }
    }

    fun setParkingMeter(minutes: Int) {
        viewModelScope.launch {
            val current = activeSpot.value ?: return@launch
            val expiry = System.currentTimeMillis() + (minutes * 60 * 1000L)
            repository.updateParkingSpot(current.copy(meterExpiryTimestamp = expiry))
            withContext(Dispatchers.Main) {
                com.example.notification.ParkingNotificationHelper.showSystemNotification(
                    getApplication(),
                    currentStrings.appName,
                    String.format(currentStrings.notificationMeterAlarmSet, minutes)
                )
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
                val existing = repository.getAllDevicesDirect()
                val existingMap = existing.associateBy { it.address.uppercase() }
                val hasAnyMonitored = existing.any { it.isMonitoredCar }

                systemBonded.forEach { dev ->
                    val found = existingMap[dev.address.uppercase()]
                    if (found == null) {
                        repository.registerBluetoothDevice(
                            dev.copy(
                                originalName = dev.name,
                                isMonitoredCar = false
                            ),
                            setAsPrimary = false
                        )
                    } else {
                        // Preserve user custom renaming and record original hardware name
                        val origName = if (found.originalName.isNotBlank()) found.originalName else dev.name
                        val effectiveName = if (found.isCustomRenamed) found.name else dev.name
                        if (found.name != effectiveName || found.originalName != origName) {
                            repository.updateBluetoothDevice(
                                found.copy(
                                    name = effectiveName,
                                    originalName = origName
                                )
                            )
                        }
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
                val deviceName = selected?.name ?: "Device"
                com.example.notification.ParkingNotificationHelper.showSystemNotification(
                    getApplication(),
                    currentStrings.appName,
                    String.format(currentStrings.notificationDeviceEnabledAutoPark, deviceName)
                )
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
                com.example.notification.ParkingNotificationHelper.showSystemNotification(
                    getApplication(),
                    currentStrings.appName,
                    String.format(currentStrings.notificationRenamedVehicle, newName.trim())
                )
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
                com.example.notification.ParkingNotificationHelper.showSystemNotification(
                    getApplication(),
                    currentStrings.appName,
                    String.format(currentStrings.notificationDeviceAddedAutoPark, name)
                )
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
            com.example.notification.ParkingNotificationHelper.showSystemNotification(
                getApplication(),
                currentStrings.appName,
                currentStrings.notificationOpenBluetoothSettings
            )
        }
    }

    fun simulateBluetoothDisconnect(deviceName: String, deviceAddress: String) {
        val intent = Intent("com.example.autopark.ACTION_SIMULATE_DISCONNECT").apply {
            setPackage(getApplication<Application>().packageName)
            putExtra("simulated_name", deviceName)
            putExtra("simulated_address", deviceAddress)
            _currentLocation.value?.let { loc ->
                if (loc.latitude != 0.0 || loc.longitude != 0.0) {
                    putExtra("simulated_lat", loc.latitude)
                    putExtra("simulated_lng", loc.longitude)
                    putExtra("simulated_alt", loc.altitude)
                    putExtra("simulated_accuracy", loc.accuracy)
                }
            }
        }
        getApplication<Application>().sendBroadcast(intent)
    }

    fun openGoogleMapsNavigation(context: Context, spot: ParkingSpot? = activeSpot.value) {
        if (spot == null) {
            com.example.notification.ParkingNotificationHelper.showSystemNotification(
                getApplication(),
                currentStrings.appName,
                currentStrings.notificationNoActiveSpotToNavigate
            )
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
                    com.example.notification.ParkingNotificationHelper.showSystemNotification(
                        getApplication(),
                        currentStrings.appName,
                        currentStrings.notificationCouldNotOpenMaps
                    )
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
            val displayFloor = localizeFloor(current.floorLevel, currentStrings)
            val displaySpot = localizeSpotName(current.spotName, currentStrings)
            ParkingRadarService.start(getApplication(), displaySpot, "$dist • ${currentStrings.notificationFloorLabel} $displayFloor")
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

    fun setParkingTimerFeatureEnabled(enabled: Boolean) {
        _isParkingTimerFeatureEnabled.value = enabled
        prefs.edit().putBoolean("parking_timer_feature_enabled", enabled).apply()
        if (!enabled) {
            // Optional: reset or stop timer if disabled while running
            resetTimerToZero()
        }
    }

    fun setDevCapyCarsEnabled(enabled: Boolean) {
        _isDevCapyCarsEnabled.value = enabled
        prefs.edit().putBoolean("dev_capy_cars_enabled", enabled).apply()
        if (!enabled && _carBadgeStyle.value == CarBadgeStyle.CAPY) {
            setCarBadgeStyle(CarBadgeStyle.CINEMATIC)
        }
    }

    fun setGeofenceAutoParkEnabled(enabled: Boolean) {
        _isGeofenceAutoParkEnabled.value = enabled
        prefs.edit().putBoolean("geofence_auto_park_enabled", enabled).apply()
        if (enabled) {
            val loc = _currentLocation.value ?: activeSpot.value?.let {
                Location("car_spot").apply {
                    latitude = it.latitude
                    longitude = it.longitude
                }
            }
            if (loc != null && (loc.latitude != 0.0 || loc.longitude != 0.0)) {
                com.example.geofence.GeofenceManager.registerCarPerimeterGeofence(
                    getApplication(),
                    loc.latitude,
                    loc.longitude
                )
            }
        } else {
            com.example.geofence.GeofenceManager.removeGeofences(getApplication())
        }
    }

    fun simulateGeofenceExit() {
        val loc = _currentLocation.value ?: activeSpot.value?.let {
            Location("car_spot").apply {
                latitude = it.latitude
                longitude = it.longitude
            }
        }
        val lat = loc?.latitude ?: 0.0
        val lng = loc?.longitude ?: 0.0
        if (lat != 0.0 && lng != 0.0) {
            com.example.geofence.GeofenceManager.simulateGeofenceExit(getApplication(), lat, lng)
        }
    }

    fun startBtProximityFinder(device: BluetoothCarDevice): Boolean {
        val curLoc = _currentLocation.value
        val spot = activeSpot.value
        val started = bluetoothProximityManager.startProximityFinder(
            device = device,
            currentPhoneLat = curLoc?.latitude,
            currentPhoneLng = curLoc?.longitude,
            spotLat = spot?.latitude,
            spotLng = spot?.longitude
        )
        if (started) {
            startCompass()
        }
        return started
    }

    fun stopBtProximityFinder() {
        bluetoothProximityManager.stopProximityFinder()
    }

    // --- Parking & Charging Timer Methods ---

    fun openParkingTimer() {
        _showTimerDialog.value = true
    }

    fun closeParkingTimer() {
        _showTimerDialog.value = false
    }

    fun startParkingTimer() {
        if (_timerRemainingSeconds.value <= 0) {
            _timerRemainingSeconds.value = _timerTotalSeconds.value
        }
        _timerIsRunning.value = true
        _timerIsPaused.value = false
        _isAlarmActive.value = false
        com.example.util.AlarmSoundHelper.stopAlarm(getApplication())

        // Sync with active spot if present
        activeSpot.value?.let { spot ->
            val expiryMs = System.currentTimeMillis() + (_timerRemainingSeconds.value * 1000L)
            viewModelScope.launch {
                repository.updateParkingSpot(spot.copy(meterExpiryTimestamp = expiryMs))
            }
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerIsRunning.value && _timerRemainingSeconds.value > 0) {
                kotlinx.coroutines.delay(1000L)
                val remaining = _timerRemainingSeconds.value - 1
                _timerRemainingSeconds.value = remaining

                // Check early notification reminders (up to 3 times before end)
                val remainingMinutes = (remaining / 60).toInt()
                val triggered = _timerAlertsTriggered.value
                val reminders = _timerRemindersMinutes.value

                for (alertMin in reminders) {
                    if (remainingMinutes <= alertMin && remainingMinutes > 0 && !triggered.contains(alertMin)) {
                        _timerAlertsTriggered.value = triggered + alertMin
                        com.example.notification.ParkingNotificationHelper.showSystemNotification(
                            getApplication(),
                            currentStrings.notificationTimerAlertTitle,
                            String.format(currentStrings.notificationTimerAlertBody, alertMin)
                        )
                        HapticHelper.getVibrator(getApplication())?.let { v ->
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                v.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1))
                            } else {
                                @Suppress("DEPRECATION")
                                v.vibrate(longArrayOf(0, 300, 200, 300), -1)
                            }
                        }
                    }
                }

                if (remaining <= 0) {
                    _timerIsRunning.value = false
                    _timerIsPaused.value = false
                    _isAlarmActive.value = true
                    _timerRemainingSeconds.value = 0

                    com.example.util.AlarmSoundHelper.playAlarm(getApplication())
                    com.example.notification.ParkingNotificationHelper.showSystemNotification(
                        getApplication(),
                        currentStrings.notificationTimerExpiredTitle,
                        currentStrings.notificationTimerExpiredBody
                    )
                    break
                }
            }
        }
    }

    fun pauseParkingTimer() {
        _timerIsRunning.value = false
        _timerIsPaused.value = true
        timerJob?.cancel()
    }

    fun resetParkingTimer() {
        _timerIsRunning.value = false
        _timerIsPaused.value = false
        timerJob?.cancel()
        _timerRemainingSeconds.value = _timerTotalSeconds.value
        _timerAlertsTriggered.value = emptySet()
        _isAlarmActive.value = false
        com.example.util.AlarmSoundHelper.stopAlarm(getApplication())
    }

    fun resetTimerToZero() {
        _timerIsRunning.value = false
        _timerIsPaused.value = false
        timerJob?.cancel()
        _timerTotalSeconds.value = 0L
        _timerRemainingSeconds.value = 0L
        prefs.edit().putLong("parking_timer_total_seconds", 0L).apply()
        _timerAlertsTriggered.value = emptySet()
        _isAlarmActive.value = false
        com.example.util.AlarmSoundHelper.stopAlarm(getApplication())
    }

    fun toggleTimerPlayPause() {
        if (_timerIsRunning.value) {
            pauseParkingTimer()
        } else {
            if (_timerRemainingSeconds.value > 0L) {
                startParkingTimer()
            }
        }
    }

    fun addTimerMinutes(minutes: Int) {
        val newTotal = (_timerTotalSeconds.value + (minutes * 60L)).coerceIn(60L, 24 * 3600L)
        _timerTotalSeconds.value = newTotal
        prefs.edit().putLong("parking_timer_total_seconds", newTotal).apply()
        val newRemaining = (_timerRemainingSeconds.value + (minutes * 60L)).coerceIn(0L, 24 * 3600L)
        _timerRemainingSeconds.value = newRemaining
    }

    fun setTimerDuration(seconds: Long) {
        val clamped = seconds.coerceIn(0L, 24 * 3600L)
        _timerTotalSeconds.value = clamped
        prefs.edit().putLong("parking_timer_total_seconds", clamped).apply()
        if (!_timerIsRunning.value) {
            _timerRemainingSeconds.value = clamped
            _timerAlertsTriggered.value = emptySet()
        }
    }

    fun adjustTimerByDrag(isClockwise: Boolean) {
        if (isClockwise) {
            // Clockwise: add minutes (+1 min)
            val current = _timerTotalSeconds.value
            val newTotal = (current + 60L).coerceIn(60L, 24 * 3600L)
            _timerTotalSeconds.value = newTotal
            prefs.edit().putLong("parking_timer_total_seconds", newTotal).apply()
            if (!_timerIsRunning.value) {
                _timerRemainingSeconds.value = newTotal
            } else {
                _timerRemainingSeconds.value = (_timerRemainingSeconds.value + 60L).coerceIn(1L, 24 * 3600L)
            }
        } else {
            // Counter-clockwise: subtract minutes (-1 min) down to 0
            val current = _timerTotalSeconds.value
            val newTotal = (current - 60L).coerceAtLeast(0L)
            _timerTotalSeconds.value = newTotal
            prefs.edit().putLong("parking_timer_total_seconds", newTotal).apply()
            if (!_timerIsRunning.value) {
                _timerRemainingSeconds.value = newTotal
            } else {
                _timerRemainingSeconds.value = (_timerRemainingSeconds.value - 60L).coerceAtLeast(0L)
            }
        }
    }

    fun adjustReminderMinutes(index: Int, isAdd: Boolean) {
        val currentList = _timerRemindersMinutes.value.toMutableList()
        while (currentList.size < 3) {
            currentList.add(when (currentList.size) { 0 -> 15; 1 -> 10; else -> 5 })
        }
        if (index in 0 until currentList.size) {
            val currentVal = currentList[index]
            val newVal = if (isAdd) {
                (currentVal + 1).coerceIn(1, 180)
            } else {
                (currentVal - 1).coerceAtLeast(0)
            }
            currentList[index] = newVal
            _timerRemindersMinutes.value = currentList
            prefs.edit().putString("parking_timer_reminders", currentList.joinToString(",")).apply()
        }
    }

    fun setTimerReminders(reminders: List<Int>) {
        val valid = reminders.filter { it > 0 }.take(3).distinct().sortedDescending()
        _timerRemindersMinutes.value = valid
        prefs.edit().putString("parking_timer_reminders", valid.joinToString(",")).apply()
    }

    fun dismissTimerAlarm() {
        _isAlarmActive.value = false
        com.example.util.AlarmSoundHelper.stopAlarm(getApplication())
    }

    fun snoozeTimerAlarm(minutes: Int = 5) {
        dismissTimerAlarm()
        setTimerDuration(minutes * 60L)
        startParkingTimer()
    }

    fun updateAlarmSoundUri(uri: Uri) {
        com.example.util.AlarmSoundHelper.saveAlarmUri(getApplication(), uri)
        _alarmSoundTitle.value = com.example.util.AlarmSoundHelper.getAlarmTitle(getApplication(), uri)
    }

    fun refreshAlarmTitle() {
        _alarmSoundTitle.value = com.example.util.AlarmSoundHelper.getAlarmTitle(getApplication())
    }

    private val _updateCheckState = kotlinx.coroutines.flow.MutableStateFlow<com.example.util.UpdateCheckResult?>(null)
    val updateCheckState: kotlinx.coroutines.flow.StateFlow<com.example.util.UpdateCheckResult?> = _updateCheckState.asStateFlow()

    private val _isCheckingForUpdates = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isCheckingForUpdates: kotlinx.coroutines.flow.StateFlow<Boolean> = _isCheckingForUpdates.asStateFlow()

    fun checkForUpdates(context: Context) {
        _isCheckingForUpdates.value = true
        com.example.util.UpdateManager.checkForUpdates(
            context = context,
            isAutomatic = false,
            language = _appLanguage.value
        ) { result ->
            _isCheckingForUpdates.value = false
            _updateCheckState.value = result
        }
    }

    fun clearUpdateCheckState() {
        _updateCheckState.value = null
    }

    fun downloadAndInstallUpdate(context: Context, downloadUrl: String) {
        com.example.util.UpdateManager.startDownload(context, downloadUrl, _appLanguage.value)
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothProximityManager.stopProximityFinder()
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
