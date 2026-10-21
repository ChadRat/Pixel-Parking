package com.example.ui.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.example.sensor.DistanceUnit
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
import com.example.util.CoordinateParser
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
            AppLanguage.valueOf(prefs.getString("app_language", AppLanguage.ENGLISH.name) ?: AppLanguage.ENGLISH.name)
        } catch (_: Exception) {
            AppLanguage.ENGLISH
        }
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    val currentStrings: AppStrings
        get() = getAppStrings(_appLanguage.value)

    private val _distanceUnit = MutableStateFlow(
        try {
            DistanceUnit.valueOf(prefs.getString("distance_unit", DistanceUnit.METRIC.name) ?: DistanceUnit.METRIC.name)
        } catch (_: Exception) {
            DistanceUnit.METRIC
        }
    )
    val distanceUnit: StateFlow<DistanceUnit> = _distanceUnit.asStateFlow()

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

    // Developer toggle to test Bluetooth disconnect on any vehicle in Car BT menu
    private val _isBtDisconnectTestEnabled = MutableStateFlow(prefs.getBoolean("bt_disconnect_test_enabled", false))
    val isBtDisconnectTestEnabled: StateFlow<Boolean> = _isBtDisconnectTestEnabled.asStateFlow()

    private val _isDevHapticDiagnostics = MutableStateFlow(prefs.getBoolean("dev_haptic_diag", false))
    val isDevHapticDiagnostics: StateFlow<Boolean> = _isDevHapticDiagnostics.asStateFlow()

    private val _isParkingTimerFeatureEnabled = MutableStateFlow(prefs.getBoolean("parking_timer_feature_enabled", true))
    val isParkingTimerFeatureEnabled: StateFlow<Boolean> = _isParkingTimerFeatureEnabled.asStateFlow()

    // Geofencing Auto-Park Feature (Default disabled, 50m perimeter)
    private val autoParkPrefs = application.getSharedPreferences(com.example.geofence.GeofenceManager.PREFS_NAME, Context.MODE_PRIVATE)
    private val _isGeofenceAutoParkEnabled = MutableStateFlow(autoParkPrefs.getBoolean(com.example.geofence.GeofenceManager.KEY_GEOFENCE_ENABLED, false))
    val isGeofenceAutoParkEnabled: StateFlow<Boolean> = _isGeofenceAutoParkEnabled.asStateFlow()

    // Capy Cars Option in Developer Options (Default disabled)
    private val _isDevCapyCarsEnabled = MutableStateFlow(prefs.getBoolean("dev_capy_cars_enabled", false))
    val isDevCapyCarsEnabled: StateFlow<Boolean> = _isDevCapyCarsEnabled.asStateFlow()

    // Geofence after Bluetooth Option in Developer Options (Default disabled)
    private val _isDevGeofenceAfterBtEnabled = MutableStateFlow(
        autoParkPrefs.getBoolean(com.example.geofence.GeofenceManager.KEY_DEV_ALLOW_GEOFENCE_AFTER_BT, false)
    )
    val isDevGeofenceAfterBtEnabled: StateFlow<Boolean> = _isDevGeofenceAfterBtEnabled.asStateFlow()

    // Text Weight Option in Developer Options (Default 400 - System Default)
    private val _devTextWeight = MutableStateFlow(prefs.getInt("dev_text_weight", 400))
    val devTextWeight: StateFlow<Int> = _devTextWeight.asStateFlow()

    // Developer option to remove custom smooth scrolling animation, keeping default simple and smooth scrolling
    private val _isDevSimpleScrollEnabled = MutableStateFlow(prefs.getBoolean("dev_simple_scroll_enabled", false))
    val isDevSimpleScrollEnabled: StateFlow<Boolean> = _isDevSimpleScrollEnabled.asStateFlow()

    // Developer Fun Walk Away Icon Option (Default disabled)
    private val _isDevFunWalkAwayIcon = MutableStateFlow(prefs.getBoolean("dev_fun_walk_away_icon", false))
    val isDevFunWalkAwayIcon: StateFlow<Boolean> = _isDevFunWalkAwayIcon.asStateFlow()

    // Developer Custom Far Away Coordinates Options (Default disabled)
    private val _isCustomFarAwayCoordsEnabled = MutableStateFlow(
        prefs.getBoolean("dev_custom_far_away_coords_enabled", false)
    )
    val isCustomFarAwayCoordsEnabled: StateFlow<Boolean> = _isCustomFarAwayCoordsEnabled.asStateFlow()

    private val _customFarAwayCoordsInput = MutableStateFlow(
        prefs.getString("dev_custom_far_away_coords_input", "") ?: ""
    )
    val customFarAwayCoordsInput: StateFlow<String> = _customFarAwayCoordsInput.asStateFlow()

    // Developer Waypoint Proximity Fill Simulator State (null = use real GPS progress)
    private val _devSimulatedProgress = MutableStateFlow<Float?>(null)
    val devSimulatedProgress: StateFlow<Float?> = _devSimulatedProgress.asStateFlow()

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

    // History Tab Pinned Spots
    private val _pinnedSpotIds = MutableStateFlow<Set<Long>>(
        try {
            prefs.getStringSet("pinned_spot_ids", emptySet())
                ?.mapNotNull { it.toLongOrNull() }
                ?.toSet() ?: emptySet()
        } catch (_: Exception) {
            emptySet()
        }
    )
    val pinnedSpotIds: StateFlow<Set<Long>> = _pinnedSpotIds.asStateFlow()

    fun togglePinSpot(spotId: Long) {
        val current = _pinnedSpotIds.value
        val updated = if (current.contains(spotId)) {
            current - spotId
        } else {
            current + spotId
        }
        _pinnedSpotIds.value = updated
        prefs.edit().putStringSet("pinned_spot_ids", updated.map { it.toString() }.toSet()).apply()
    }

    // --- Parking & Charging Timer Feature ---
    private val _timerTotalSeconds = MutableStateFlow(
        prefs.getLong("parking_timer_total_seconds", 0L).coerceIn(0L, 24 * 3600L)
    )
    val timerTotalSeconds: StateFlow<Long> = _timerTotalSeconds.asStateFlow()

    private val _timerRemainingSeconds = MutableStateFlow(
        prefs.getLong("parking_timer_remaining_seconds", 0L).coerceIn(0L, 24 * 3600L)
    )
    val timerRemainingSeconds: StateFlow<Long> = _timerRemainingSeconds.asStateFlow()

    private val _timerEndTimestamp = MutableStateFlow<Long?>(null)
    val timerEndTimestamp: StateFlow<Long?> = _timerEndTimestamp.asStateFlow()

    private val _timerIsRunning = MutableStateFlow(false)
    val timerIsRunning: StateFlow<Boolean> = _timerIsRunning.asStateFlow()

    private val _timerIsPaused = MutableStateFlow(false)
    val timerIsPaused: StateFlow<Boolean> = _timerIsPaused.asStateFlow()

    private val _timerIsStopped = MutableStateFlow(false)
    val timerIsStopped: StateFlow<Boolean> = _timerIsStopped.asStateFlow()

    // Up to 3 reminder alerts before end of timer (e.g. 60, 30, 15 mins)
    private val _timerRemindersMinutes = MutableStateFlow(
        prefs.getString("parking_timer_reminders", "60,30,15")
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.filter { it > 0 }
            ?.take(3)
            ?.ifEmpty { listOf(60, 30, 15) }
            ?: listOf(60, 30, 15)
    )
    val timerRemindersMinutes: StateFlow<List<Int>> = _timerRemindersMinutes.asStateFlow()

    private val _timerAlertsTriggered = MutableStateFlow<Set<Int>>(emptySet())
    val timerAlertsTriggered: StateFlow<Set<Int>> = _timerAlertsTriggered.asStateFlow()

    private val _isAlarmActive = MutableStateFlow(false)
    val isAlarmActive: StateFlow<Boolean> = _isAlarmActive.asStateFlow()

    private val _showTimerDialog = MutableStateFlow(false)
    val showTimerDialog: StateFlow<Boolean> = _showTimerDialog.asStateFlow()

    private val _showWhatsNew = MutableStateFlow(false)
    val showWhatsNew: StateFlow<Boolean> = _showWhatsNew.asStateFlow()

    private val _showBackgroundAutoSaveSetup = MutableStateFlow(false)
    val showBackgroundAutoSaveSetup: StateFlow<Boolean> = _showBackgroundAutoSaveSetup.asStateFlow()

    private val _whatsNewMessage = MutableStateFlow(com.example.util.WhatsNewManager.CURRENT_MESSAGE)
    val whatsNewMessage: StateFlow<String> = _whatsNewMessage.asStateFlow()

    private val _alarmSoundTitle = MutableStateFlow(com.example.util.AlarmSoundHelper.getAlarmTitle(application))
    val alarmSoundTitle: StateFlow<String> = _alarmSoundTitle.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null

    val btProximityRelativeAngle: StateFlow<Float> = bluetoothProximityManager.proximityState
        .map { it.relativeAngle }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0f)

    private val wearDataLayerBridge = WearDataLayerBridge.getInstance(application)

    // Derived Navigation Telemetry: Distance & Relative Arrow Angle
    val navigationTelemetry = combine(
        combine(activeSpot, currentLocation) { spot, loc -> Pair(spot, loc) },
        compassState,
        _distanceUnit,
        _appLanguage,
        _devSimulatedProgress
    ) { (spot, loc), compass, unit, lang, devProgress ->
        if (spot == null || !spot.isActive) {
            compassSensorManager.setTargetBearing(null)
            val effectiveProgress = devProgress ?: 0f
            val hasSim = devProgress != null
            val effectiveDist = if (hasSim) (100f * (1f - effectiveProgress) + 2.5f * effectiveProgress) else 0f
            NavigationTelemetry(
                distanceMeters = effectiveDist,
                formattedDistance = if (hasSim) LocationHelper.formatDistanceWithUnit(effectiveDist, if (lang == AppLanguage.ENGLISH) unit else DistanceUnit.METRIC) else "No GPS Fix",
                targetBearing = 0f,
                relativeArrowAngle = 0f,
                altitudeDifference = 0.0,
                hasActiveTarget = hasSim,
                distanceCoveredProgress = effectiveProgress
            )
        } else {
            val dist = if (loc != null) {
                LocationHelper.calculateDistanceMeters(
                    loc.latitude,
                    loc.longitude,
                    spot.latitude,
                    spot.longitude
                )
            } else {
                100f
            }
            val bearing = if (loc != null) {
                LocationHelper.calculateBearingDegrees(
                    loc.latitude,
                    loc.longitude,
                    spot.latitude,
                    spot.longitude
                )
            } else {
                0f
            }
            val relativeAngle = (bearing - compass.azimuthDegrees + 360f) % 360f
            val altDiff = if (loc != null) {
                spot.altitude - (loc.altitude.takeIf { it != 0.0 } ?: spot.altitude)
            } else {
                0.0
            }

            if (dist <= 3.0f) {
                compassSensorManager.setTargetBearing(null)
            } else {
                compassSensorManager.setTargetBearing(bearing)
            }

            val isAligned = kotlin.math.abs(relativeAngle) < 15f || relativeAngle > 345f
            val realProgress = getDistanceCoveredProgress(spot.id, dist, isBtMode = false)
            val effectiveProgress = devProgress ?: realProgress
            val effectiveDist = if (devProgress != null) {
                val maxRef = maxOf(prefs.getFloat("max_dist_spot_${spot.id}", 100f), 100f)
                maxRef * (1f - effectiveProgress) + 2.5f * effectiveProgress
            } else {
                dist
            }

            if (spot.isActive) {
                wearDataLayerBridge.syncLiveTelemetry(
                    distanceMeters = effectiveDist,
                    targetBearing = bearing,
                    relativeArrowAngle = relativeAngle,
                    isAligned = isAligned,
                    distanceCoveredProgress = effectiveProgress
                )
            }

            val effectiveUnit = if (lang == AppLanguage.ENGLISH) unit else DistanceUnit.METRIC
            NavigationTelemetry(
                distanceMeters = effectiveDist,
                formattedDistance = LocationHelper.formatDistanceWithUnit(effectiveDist, effectiveUnit),
                targetBearing = bearing,
                relativeArrowAngle = relativeAngle,
                altitudeDifference = altDiff,
                hasActiveTarget = true,
                distanceCoveredProgress = effectiveProgress
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        NavigationTelemetry(0f, "--", 0f, 0f, 0.0, false, 0f)
    )

    private val timerActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == com.example.receiver.ParkingTimerActionReceiver.ACTION_RESUME_TIMER) {
                com.example.notification.ParkingNotificationHelper.dismissTimerStoppedNotification(getApplication())
                if (_timerRemainingSeconds.value > 0) {
                    startParkingTimer()
                }
            }
        }
    }

    init {
        compassSensorManager.hapticProfile = _hapticProfile.value
        startLocationTracking()
        refreshCurrentLocation()
        syncSystemPairedDevices()
        updateDaytimeState()

        // Clean initial state: when no timer is active or stopped, ensure strictly 00:00
        if (!_timerIsRunning.value && !_timerIsStopped.value && !_timerIsPaused.value) {
            _timerRemainingSeconds.value = 0L
            _timerTotalSeconds.value = 0L
            prefs.edit().putLong("parking_timer_total_seconds", 0L)
                .putLong("parking_timer_remaining_seconds", 0L).apply()
        }

        val filter = IntentFilter(com.example.receiver.ParkingTimerActionReceiver.ACTION_RESUME_TIMER)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(timerActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            application.registerReceiver(timerActionReceiver, filter)
        }

        var previousActiveSpotId: Long? = null
        var isInitialCollection = true
        viewModelScope.launch {
            activeSpot.collect { spot ->
                wearDataLayerBridge.syncParkingSpot(spot)
                if (!isInitialCollection && spot != null && spot.id != previousActiveSpotId && spot.isAutoSaved) {
                    stopCompass()
                    if (_selectedTab.value == AppTab.COMPASS_RADAR) {
                        _selectedTab.value = AppTab.DASHBOARD
                    }
                }
                isInitialCollection = false
                previousActiveSpotId = spot?.id
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

        viewModelScope.launch {
            com.example.util.WhatsNewManager.showWhatsNewEvent.collect { msg ->
                _whatsNewMessage.value = msg
                _showWhatsNew.value = true
            }
        }

        if (com.example.util.WhatsNewManager.checkAndConsumePendingShow(application)) {
            _whatsNewMessage.value = com.example.util.WhatsNewManager.CURRENT_MESSAGE
            _showWhatsNew.value = true
        }
    }

    fun triggerWearSync() {
        wearDataLayerBridge.syncParkingSpot(activeSpot.value)
        wearDataLayerBridge.syncCrownPosition(_wearCrownOnRight.value)
    }

    fun startLocationTracking() {
        if (locationCallback != null) return
        locationCallback = LocationHelper.startContinuousLocationUpdates(getApplication(), 4000L) { loc ->
            _currentLocation.value = loc
            updateDaytimeState()
            if (_isGeofenceAutoParkEnabled.value) {
                com.example.geofence.GeofenceManager.recordCarLocation(
                    context = getApplication(),
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    altitude = loc.altitude,
                    accuracy = loc.accuracy,
                    speedMps = if (loc.hasSpeed()) loc.speed else 0f
                )
            }
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
        wearDataLayerBridge.syncParkingSpot(activeSpot.value)
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

    fun setDistanceUnit(unit: DistanceUnit) {
        _distanceUnit.value = unit
        prefs.edit().putString("distance_unit", unit.name).apply()
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
                LocationHelper.persistLocation(getApplication(), loc)
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
                    currentStrings.notificationParkingLocationSaved,
                    address
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

    fun getDistanceCoveredProgress(spotId: Long?, currentDistance: Float, isBtMode: Boolean = false): Float {
        if (_devSimulatedProgress.value != null) {
            return _devSimulatedProgress.value!!
        }
        if (currentDistance <= 2.5f) return 1.0f
        val key = if (isBtMode) "max_dist_bt_proximity" else "max_dist_spot_${spotId ?: 0L}"
        val savedMax = prefs.getFloat(key, 0f)
        val maxDist = if (currentDistance > savedMax) {
            prefs.edit().putFloat(key, currentDistance).apply()
            currentDistance
        } else {
            savedMax
        }
        val effectiveMax = maxOf(maxDist, 15f)
        if (effectiveMax <= 2.5f) return 1.0f
        return ((effectiveMax - currentDistance) / (effectiveMax - 2.5f)).coerceIn(0f, 1f)
    }

    fun setDevSimulatedProgress(progress: Float?) {
        _devSimulatedProgress.value = progress?.coerceIn(0f, 1f)
    }

    fun resetDevSimulatedProgress() {
        _devSimulatedProgress.value = null
    }

    fun markCarFound() {
        viewModelScope.launch {
            val current = activeSpot.value ?: return@launch
            repository.updateParkingSpot(current.copy(isActive = false))
            prefs.edit().remove("max_dist_spot_${current.id}").apply()
            com.example.geofence.GeofenceManager.clearCandidateParkingState(getApplication())
            com.example.geofence.GeofenceManager.removeGeofences(getApplication())
            compassSensorManager.setTargetBearing(null)
            compassSensorManager.cancelVibration()
            stopRadarService()
            HapticHelper.performConfirmationHaptic(getApplication(), _hapticProfile.value)
        }
    }

    fun deleteSpot(id: Long) {
        viewModelScope.launch {
            repository.deleteSpot(id)
            if (_pinnedSpotIds.value.contains(id)) {
                val updated = _pinnedSpotIds.value - id
                _pinnedSpotIds.value = updated
                prefs.edit().putStringSet("pinned_spot_ids", updated.map { it.toString() }.toSet()).apply()
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _pinnedSpotIds.value = emptySet()
            prefs.edit().remove("pinned_spot_ids").apply()
        }
    }

    // Bluetooth Car Device Management
    fun syncSystemPairedDevices() {
        viewModelScope.launch {
            val systemBonded = BluetoothDeviceHelper.getSystemBondedDevices(getApplication())
            if (systemBonded != null) {
                val existing = repository.getAllDevicesDirect()
                val existingMap = existing.associateBy { it.address.uppercase() }
                val bondedAddresses = systemBonded.map { it.address.uppercase() }.toSet()

                val hasAnyMonitored = existing.any { it.isMonitoredCar }

                // Register or update currently paired devices from the phone
                systemBonded.forEach { dev ->
                    val found = existingMap[dev.address.uppercase()]
                    if (found == null) {
                        val isCar = !hasAnyMonitored && (dev.deviceType == "Car" || com.example.receiver.BluetoothDisconnectReceiver.hasVehicleKeywords(dev.name) || systemBonded.size == 1)
                        repository.registerBluetoothDevice(
                            dev.copy(
                                originalName = dev.name,
                                isMonitoredCar = isCar
                            ),
                            setAsPrimary = isCar
                        )
                    } else {
                        // Preserve user custom renaming and monitored state, and record original hardware name
                        val origName = if (found.originalName.isNotBlank()) found.originalName else dev.name
                        val effectiveName = if (found.isCustomRenamed) found.name else dev.name
                        if (found.name != effectiveName || found.originalName != origName || found.deviceType != dev.deviceType) {
                            repository.updateBluetoothDevice(
                                found.copy(
                                    name = effectiveName,
                                    originalName = origName,
                                    deviceType = dev.deviceType
                                )
                            )
                        }
                    }
                }

                // Strictly ensure only paired/saved devices on the phone are kept:
                // Remove any devices from the database that are no longer paired with the phone
                existing.forEach { dev ->
                    if (!bondedAddresses.contains(dev.address.uppercase())) {
                        repository.deleteBluetoothDevice(dev.address)
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
            val currentActive = activeSpot.value
            val loc = _currentLocation.value ?: LocationHelper.lastKnownLocation
            if (loc != null && (loc.latitude != 0.0 || loc.longitude != 0.0)) {
                // If an active parking spot is already saved at these coordinates, add a realistic movement offset (~200m)
                // so the new second BT disconnect is correctly recorded as a new parking destination
                val isSameAsActive = currentActive != null &&
                        kotlin.math.abs(currentActive.latitude - loc.latitude) < 0.0001 &&
                        kotlin.math.abs(currentActive.longitude - loc.longitude) < 0.0001
                val lat = if (isSameAsActive) loc.latitude + 0.002 else loc.latitude
                val lng = if (isSameAsActive) loc.longitude + 0.002 else loc.longitude
                putExtra("simulated_lat", lat)
                putExtra("simulated_lng", lng)
                putExtra("simulated_alt", loc.altitude)
                putExtra("simulated_accuracy", loc.accuracy)
            } else if (currentActive != null) {
                putExtra("simulated_lat", currentActive.latitude + 0.002)
                putExtra("simulated_lng", currentActive.longitude + 0.002)
            } else {
                putExtra("simulated_lat", 37.4220)
                putExtra("simulated_lng", -122.0841)
            }
        }
        getApplication<Application>().sendBroadcast(intent)
    }

    fun testBluetoothParkingAutoSave() {
        viewModelScope.launch(Dispatchers.IO) {
            val monitored = repository.getSelectedCarDevice()
                ?: repository.getMonitoredDevicesDirect().firstOrNull()
            val targetName = monitored?.name ?: "Bluetooth Car (Test)"
            val targetAddress = monitored?.address ?: "00:11:22:33:44:55"
            simulateBluetoothDisconnect(targetName, targetAddress)
        }
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

    fun shareParkingLocation(context: Context, spot: ParkingSpot? = null, linkOnly: Boolean = false) {
        val targetSpot = spot ?: activeSpot.value ?: allSpots.value.firstOrNull()
        if (targetSpot != null) {
            val mapsUrl = "https://maps.google.com/?q=${targetSpot.latitude},${targetSpot.longitude}"
            val shareText = if (linkOnly) {
                mapsUrl
            } else {
                buildString {
                    append("📍 ${targetSpot.spotName}\n")
                    if (targetSpot.address.isNotBlank()) append("${targetSpot.address}\n")
                    if (targetSpot.floorLevel.isNotBlank()) append("Floor/Level: ${targetSpot.floorLevel}\n")
                    if (targetSpot.note.isNotBlank()) append("Notes: ${targetSpot.note}\n")
                    append(mapsUrl)
                }
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

    fun setBtDisconnectTestEnabled(enabled: Boolean) {
        _isBtDisconnectTestEnabled.value = enabled
        prefs.edit().putBoolean("bt_disconnect_test_enabled", enabled).apply()
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

    fun setDevGeofenceAfterBtEnabled(enabled: Boolean) {
        _isDevGeofenceAfterBtEnabled.value = enabled
        autoParkPrefs.edit().putBoolean(com.example.geofence.GeofenceManager.KEY_DEV_ALLOW_GEOFENCE_AFTER_BT, enabled).apply()
    }

    fun setDevTextWeight(weight: Int) {
        val clamped = weight.coerceIn(100, 900)
        _devTextWeight.value = clamped
        prefs.edit().putInt("dev_text_weight", clamped).apply()
    }

    fun setDevSimpleScrollEnabled(enabled: Boolean) {
        _isDevSimpleScrollEnabled.value = enabled
        prefs.edit().putBoolean("dev_simple_scroll_enabled", enabled).apply()
    }

    fun setDevFunWalkAwayIcon(enabled: Boolean) {
        _isDevFunWalkAwayIcon.value = enabled
        prefs.edit().putBoolean("dev_fun_walk_away_icon", enabled).apply()
    }

    fun setDevCustomFarAwayCoordsEnabled(enabled: Boolean) {
        _isCustomFarAwayCoordsEnabled.value = enabled
        prefs.edit().putBoolean("dev_custom_far_away_coords_enabled", enabled).apply()
    }

    fun setCustomFarAwayCoordsInput(input: String) {
        _customFarAwayCoordsInput.value = input
        prefs.edit().putString("dev_custom_far_away_coords_input", input).apply()
    }

    fun applyCustomFarAwayCoordinates(
        coordsText: String,
        startNavigation: Boolean = false,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val parsed = CoordinateParser.parseSuspend(coordsText)
            if (parsed == null) {
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false, "Invalid coordinates format. Use 'lat, lng' or Google Maps link.")
                }
                return@launch
            }

            val address = LocationHelper.getAddressFromCoordinates(
                getApplication(),
                parsed.latitude,
                parsed.longitude
            )
            val spotName = when {
                !parsed.label.isNullOrBlank() -> parsed.label.trim()
                !address.isNullOrBlank() && !address.contains("Unknown", ignoreCase = true) && !address.contains("Unable", ignoreCase = true) -> address.trim()
                else -> "Custom Far Away Spot"
            }

            val spot = ParkingSpot(
                latitude = parsed.latitude,
                longitude = parsed.longitude,
                altitude = 0.0,
                accuracyMeters = 5.0f,
                address = address,
                spotName = spotName,
                floorLevel = "Ground Level",
                note = "Dev Custom Coordinates",
                timestamp = System.currentTimeMillis(),
                isAutoSaved = false,
                bluetoothDeviceAddress = "DEV_CUSTOM_FAR_AWAY",
                bluetoothDeviceName = "Custom Coordinates",
                isActive = true
            )

            val id = repository.saveNewParkingSpot(spot)
            val saved = spot.copy(id = id)

            _currentAddress.value = address

            withContext(Dispatchers.Main) {
                if (startNavigation) {
                    selectTab(AppTab.COMPASS_RADAR)
                    startCompass()
                }

                ParkingNotificationHelper.showCarParkedNotification(getApplication(), saved)
                com.example.notification.ParkingNotificationHelper.showSystemNotification(
                    getApplication(),
                    currentStrings.appName,
                    "Saved parking location: $spotName"
                )
                onResult?.invoke(
                    true,
                    "Parking spot saved: $spotName (${String.format(java.util.Locale.US, "%.4f, %.4f", parsed.latitude, parsed.longitude)})"
                )
            }
        }
    }

    fun importSharedCoordinates(
        rawTextOrUri: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val isLikelyUrl = rawTextOrUri.contains("http://", ignoreCase = true) || rawTextOrUri.contains("https://", ignoreCase = true)
            if (isLikelyUrl) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        "Resolving Google Maps location...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            val parsed = CoordinateParser.parseSuspend(rawTextOrUri)
            if (parsed == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        "Could not find valid coordinates in shared content",
                        Toast.LENGTH_SHORT
                    ).show()
                    onComplete?.invoke(false)
                }
                return@launch
            }

            val address = LocationHelper.getAddressFromCoordinates(
                getApplication(),
                parsed.latitude,
                parsed.longitude
            )
            val spotName = when {
                !parsed.label.isNullOrBlank() -> parsed.label.trim()
                !address.isNullOrBlank() && !address.contains("Unknown", ignoreCase = true) && !address.contains("Unable", ignoreCase = true) -> address.trim()
                else -> "Shared Parking Spot"
            }

            val spot = ParkingSpot(
                latitude = parsed.latitude,
                longitude = parsed.longitude,
                altitude = 0.0,
                accuracyMeters = 5.0f,
                address = address,
                spotName = spotName,
                floorLevel = "Ground Level",
                note = "Imported from Google Maps",
                timestamp = System.currentTimeMillis(),
                isAutoSaved = false,
                bluetoothDeviceName = "Shared Location",
                isActive = true
            )

            val id = repository.saveNewParkingSpot(spot)
            val saved = spot.copy(id = id)

            _currentAddress.value = address

            withContext(Dispatchers.Main) {
                // Automatically start navigation and open the Waypoint tab
                selectTab(AppTab.COMPASS_RADAR)
                startCompass()

                ParkingNotificationHelper.showCarParkedNotification(getApplication(), saved)
                com.example.notification.ParkingNotificationHelper.showSystemNotification(
                    getApplication(),
                    currentStrings.appName,
                    "Navigating to shared spot: $spotName"
                )
                Toast.makeText(
                    getApplication(),
                    "Spot saved: $spotName! Navigating via Waypoint radar...",
                    Toast.LENGTH_LONG
                ).show()

                onComplete?.invoke(true)
            }
        }
    }

    fun setGeofenceAutoParkEnabled(enabled: Boolean) {
        _isGeofenceAutoParkEnabled.value = enabled
        prefs.edit().putBoolean("geofence_auto_park_enabled", enabled).apply()
        autoParkPrefs.edit().putBoolean(com.example.geofence.GeofenceManager.KEY_GEOFENCE_ENABLED, enabled).apply()
        if (enabled) {
            com.example.geofence.GeofenceManager.startGeofenceWalkingAutoPark(getApplication())
            viewModelScope.launch(Dispatchers.IO) {
                // If there is already an active parking spot, arm the perimeter around it
                activeSpot.value?.let { spot ->
                    if (spot.latitude != 0.0 || spot.longitude != 0.0) {
                        com.example.geofence.GeofenceManager.registerCarPerimeterGeofence(
                            getApplication(),
                            spot.latitude,
                            spot.longitude,
                            com.example.geofence.GeofenceManager.DEFAULT_PERIMETER_RADIUS_METERS
                        )
                    }
                }
            }
        } else {
            com.example.geofence.GeofenceManager.stopGeofenceWalkingAutoPark(getApplication())
        }
    }

    fun simulateGeofenceExit() {
        viewModelScope.launch(Dispatchers.IO) {
            val loc = _currentLocation.value ?: activeSpot.value?.let {
                Location("car_spot").apply {
                    latitude = it.latitude
                    longitude = it.longitude
                }
            } ?: LocationHelper.getCurrentLocation(getApplication())
            val lat = if (loc != null && loc.latitude != 0.0) loc.latitude else 37.4220
            val lng = if (loc != null && loc.longitude != 0.0) loc.longitude else -122.0841
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
        prefs.edit().remove("max_dist_bt_proximity").apply()
        bluetoothProximityManager.stopProximityFinder()
    }

    // --- Parking & Charging Timer Methods ---

    fun openParkingTimer() {
        _showTimerDialog.value = true
    }

    fun closeParkingTimer() {
        _showTimerDialog.value = false
    }

    fun startParkingTimer(): Boolean {
        if (_timerRemainingSeconds.value <= 0L && _timerTotalSeconds.value <= 0L) {
            resetTimerToZero()
            return false
        }
        if (_timerRemainingSeconds.value <= 0L) {
            _timerRemainingSeconds.value = _timerTotalSeconds.value
        }
        if (_timerRemainingSeconds.value <= 0L) {
            resetTimerToZero()
            return false
        }
        _timerIsRunning.value = true
        _timerIsPaused.value = false
        _timerIsStopped.value = false
        _isAlarmActive.value = false
        _timerEndTimestamp.value = System.currentTimeMillis() + (_timerRemainingSeconds.value * 1000L)
        com.example.util.AlarmSoundHelper.stopAlarm(getApplication())
        com.example.notification.ParkingNotificationHelper.dismissTimerExpiredNotification(getApplication())
        com.example.notification.ParkingNotificationHelper.dismissTimerStoppedNotification(getApplication())
        com.example.notification.ParkingNotificationHelper.showOngoingTimerNotification(getApplication(), _timerRemainingSeconds.value)

        // Sync with active spot if present
        activeSpot.value?.let { spot ->
            val expiryMs = System.currentTimeMillis() + (_timerRemainingSeconds.value * 1000L)
            viewModelScope.launch {
                repository.updateParkingSpot(spot.copy(meterExpiryTimestamp = expiryMs))
            }
        }

        // Initialize already triggered alerts so we never fire reminders for intervals greater than the timer duration
        val initialRemainingMinutes = (_timerRemainingSeconds.value / 60).toInt()
        _timerAlertsTriggered.value = _timerRemindersMinutes.value.filter { it >= initialRemainingMinutes }.toSet()

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerIsRunning.value && _timerRemainingSeconds.value > 0) {
                kotlinx.coroutines.delay(1000L)
                val remaining = _timerRemainingSeconds.value - 1
                _timerRemainingSeconds.value = remaining

                // Update ongoing non-dismissible notification every 5 seconds or when under 60 seconds
                if (remaining % 5 == 0L || remaining <= 60L) {
                    com.example.notification.ParkingNotificationHelper.showOngoingTimerNotification(getApplication(), remaining)
                }

                // Check early notification reminders at exact minute mark
                val remainingMinutes = (remaining / 60).toInt()
                val secondsInMinute = (remaining % 60).toInt()
                val triggered = _timerAlertsTriggered.value
                val reminders = _timerRemindersMinutes.value

                for (alertMin in reminders) {
                    if (alertMin > 0 && remainingMinutes == alertMin && secondsInMinute == 0 && !triggered.contains(alertMin)) {
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
                    _timerIsStopped.value = false
                    _isAlarmActive.value = true
                    _timerRemainingSeconds.value = 0L
                    _timerTotalSeconds.value = 0L
                    _timerEndTimestamp.value = null
                    prefs.edit().putLong("parking_timer_total_seconds", 0L)
                        .putLong("parking_timer_remaining_seconds", 0L).apply()

                    com.example.notification.ParkingNotificationHelper.dismissOngoingTimerNotification(getApplication())
                    com.example.util.AlarmSoundHelper.playAlarm(getApplication())
                    com.example.notification.ParkingNotificationHelper.showTimerExpiredNotification(
                        getApplication(),
                        currentStrings.alarmExpiredTitle,
                        currentStrings.alarmExpiredMessage
                    )
                    break
                }
            }
        }
        return true
    }

    fun pauseParkingTimer() {
        _timerIsRunning.value = false
        _timerIsPaused.value = true
        _timerEndTimestamp.value = null
        timerJob?.cancel()
        com.example.notification.ParkingNotificationHelper.dismissOngoingTimerNotification(getApplication())
    }

    fun stopParkingTimerWithNotification() {
        val hadRunning = _timerIsRunning.value || _timerRemainingSeconds.value > 0
        _timerIsRunning.value = false
        _timerIsPaused.value = false
        _timerIsStopped.value = true
        _timerEndTimestamp.value = null
        _isAlarmActive.value = false
        timerJob?.cancel()
        com.example.util.AlarmSoundHelper.stopAlarm(getApplication())
        com.example.notification.ParkingNotificationHelper.dismissOngoingTimerNotification(getApplication())
        com.example.notification.ParkingNotificationHelper.dismissTimerExpiredNotification(getApplication())
        if (hadRunning) {
            com.example.notification.ParkingNotificationHelper.showTimerStoppedNotification(getApplication())
        }
    }

    fun resetParkingTimer() {
        _timerIsRunning.value = false
        _timerIsPaused.value = false
        _timerIsStopped.value = false
        _timerEndTimestamp.value = null
        timerJob?.cancel()
        _timerRemainingSeconds.value = 0L
        _timerTotalSeconds.value = 0L
        prefs.edit().putLong("parking_timer_total_seconds", 0L)
            .putLong("parking_timer_remaining_seconds", 0L).apply()
        _timerAlertsTriggered.value = emptySet()
        _isAlarmActive.value = false
        com.example.notification.ParkingNotificationHelper.dismissOngoingTimerNotification(getApplication())
        com.example.util.AlarmSoundHelper.stopAlarm(getApplication())
        com.example.notification.ParkingNotificationHelper.dismissTimerExpiredNotification(getApplication())
    }

    fun resetTimerToZero() {
        _timerIsRunning.value = false
        _timerIsPaused.value = false
        _timerIsStopped.value = false
        _timerEndTimestamp.value = null
        timerJob?.cancel()
        _timerTotalSeconds.value = 0L
        _timerRemainingSeconds.value = 0L
        prefs.edit().putLong("parking_timer_total_seconds", 0L).apply()
        _timerAlertsTriggered.value = emptySet()
        _isAlarmActive.value = false
        com.example.notification.ParkingNotificationHelper.dismissOngoingTimerNotification(getApplication())
        com.example.notification.ParkingNotificationHelper.dismissTimerStoppedNotification(getApplication())
        com.example.util.AlarmSoundHelper.stopAlarm(getApplication())
        com.example.notification.ParkingNotificationHelper.dismissTimerExpiredNotification(getApplication())
        activeSpot.value?.let { spot ->
            viewModelScope.launch {
                repository.updateParkingSpot(spot.copy(meterExpiryTimestamp = null))
            }
        }
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

    fun adjustTimerHours(deltaHours: Int) {
        _timerIsStopped.value = false
        val currentSeconds = if (_timerIsRunning.value) _timerRemainingSeconds.value else _timerTotalSeconds.value
        val curHours = (currentSeconds / 3600).toInt()
        val curMins = ((currentSeconds % 3600) / 60).toInt()
        val newHours = (curHours + deltaHours).coerceIn(0, 99)
        val newTotal = (newHours * 3600L) + (curMins * 60L)
        _timerTotalSeconds.value = newTotal
        prefs.edit().putLong("parking_timer_total_seconds", newTotal).apply()
        _timerRemainingSeconds.value = newTotal
        _isAlarmActive.value = false
        com.example.util.AlarmSoundHelper.stopAlarm(getApplication())
        com.example.notification.ParkingNotificationHelper.dismissTimerExpiredNotification(getApplication())

        if (newTotal == 0L) {
            _timerIsRunning.value = false
            timerJob?.cancel()
            _timerEndTimestamp.value = null
            com.example.notification.ParkingNotificationHelper.dismissOngoingTimerNotification(getApplication())
        } else if (_timerIsRunning.value) {
            _timerEndTimestamp.value = System.currentTimeMillis() + (newTotal * 1000L)
            com.example.notification.ParkingNotificationHelper.showOngoingTimerNotification(getApplication(), newTotal)
        }
    }

    fun adjustTimerMinutes(deltaMinutes: Int) {
        _timerIsStopped.value = false
        val currentSeconds = if (_timerIsRunning.value) _timerRemainingSeconds.value else _timerTotalSeconds.value
        val curHours = (currentSeconds / 3600).toInt()
        val curMins = ((currentSeconds % 3600) / 60).toInt()
        val newMins = (curMins + deltaMinutes).coerceIn(0, 59)
        val newTotal = (curHours * 3600L) + (newMins * 60L)
        _timerTotalSeconds.value = newTotal
        prefs.edit().putLong("parking_timer_total_seconds", newTotal).apply()
        _timerRemainingSeconds.value = newTotal
        _isAlarmActive.value = false
        com.example.util.AlarmSoundHelper.stopAlarm(getApplication())
        com.example.notification.ParkingNotificationHelper.dismissTimerExpiredNotification(getApplication())

        if (newTotal == 0L) {
            _timerIsRunning.value = false
            timerJob?.cancel()
            _timerEndTimestamp.value = null
            com.example.notification.ParkingNotificationHelper.dismissOngoingTimerNotification(getApplication())
        } else if (_timerIsRunning.value) {
            _timerEndTimestamp.value = System.currentTimeMillis() + (newTotal * 1000L)
            com.example.notification.ParkingNotificationHelper.showOngoingTimerNotification(getApplication(), newTotal)
        }
    }

    fun addTimerMinutes(minutes: Int) {
        val newTotal = (_timerTotalSeconds.value + (minutes * 60L)).coerceIn(0L, 24 * 3600L)
        _timerTotalSeconds.value = newTotal
        prefs.edit().putLong("parking_timer_total_seconds", newTotal).apply()
        val newRemaining = (_timerRemainingSeconds.value + (minutes * 60L)).coerceIn(0L, 24 * 3600L)
        _timerRemainingSeconds.value = newRemaining
        if (_timerIsRunning.value) {
            _timerEndTimestamp.value = System.currentTimeMillis() + (newRemaining * 1000L)
        }
    }

    fun setTimerDuration(seconds: Long) {
        val clamped = seconds.coerceIn(0L, 24 * 3600L)
        _timerTotalSeconds.value = clamped
        prefs.edit().putLong("parking_timer_total_seconds", clamped).apply()
        if (!_timerIsRunning.value) {
            _timerRemainingSeconds.value = clamped
            _timerAlertsTriggered.value = emptySet()
        } else {
            _timerRemainingSeconds.value = clamped
            _timerEndTimestamp.value = System.currentTimeMillis() + (clamped * 1000L)
        }
    }

    fun adjustTimerByDrag(isClockwise: Boolean) {
        if (isClockwise) {
            // Clockwise: add minutes (+1 min)
            val current = _timerTotalSeconds.value
            val newTotal = (current + 60L).coerceIn(0L, 24 * 3600L)
            _timerTotalSeconds.value = newTotal
            prefs.edit().putLong("parking_timer_total_seconds", newTotal).apply()
            if (!_timerIsRunning.value) {
                _timerRemainingSeconds.value = newTotal
            } else {
                val newRem = (_timerRemainingSeconds.value + 60L).coerceIn(0L, 24 * 3600L)
                _timerRemainingSeconds.value = newRem
                _timerEndTimestamp.value = System.currentTimeMillis() + (newRem * 1000L)
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
                val newRem = (_timerRemainingSeconds.value - 60L).coerceAtLeast(0L)
                _timerRemainingSeconds.value = newRem
                _timerEndTimestamp.value = System.currentTimeMillis() + (newRem * 1000L)
            }
        }
    }

    fun adjustReminderMinutes(index: Int, delta: Int): Boolean {
        val currentList = _timerRemindersMinutes.value.toMutableList()
        while (currentList.size < 3) {
            currentList.add(when (currentList.size) { 0 -> 60; 1 -> 30; else -> 15 })
        }
        if (index in 0 until currentList.size) {
            val currentVal = currentList[index]
            val newVal = (currentVal + delta).coerceIn(0, 180)
            if (newVal != currentVal) {
                currentList[index] = newVal
                _timerRemindersMinutes.value = currentList
                prefs.edit().putString("parking_timer_reminders", currentList.joinToString(",")).apply()
                return true
            }
        }
        return false
    }

    fun adjustReminderMinutes(index: Int, isAdd: Boolean) {
        adjustReminderMinutes(index, if (isAdd) 1 else -1)
    }

    fun setTimerReminders(reminders: List<Int>) {
        val valid = reminders.filter { it > 0 }.take(3).distinct().sortedDescending()
        _timerRemindersMinutes.value = valid
        prefs.edit().putString("parking_timer_reminders", valid.joinToString(",")).apply()
    }

    fun dismissTimerAlarm() {
        _isAlarmActive.value = false
        _timerRemainingSeconds.value = 0L
        _timerTotalSeconds.value = 0L
        prefs.edit().putLong("parking_timer_total_seconds", 0L)
            .putLong("parking_timer_remaining_seconds", 0L).apply()
        com.example.util.AlarmSoundHelper.stopAlarm(getApplication())
        com.example.notification.ParkingNotificationHelper.dismissTimerExpiredNotification(getApplication())
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
        if (com.example.util.WhatsNewManager.hasMessageChanged(context)) {
            _whatsNewMessage.value = com.example.util.WhatsNewManager.getActiveMessage(context)
            _showWhatsNew.value = true
        }
        com.example.util.UpdateManager.startDownload(context, downloadUrl, _appLanguage.value)
    }

    fun showWhatsNewWindow(force: Boolean = false) {
        _whatsNewMessage.value = com.example.util.WhatsNewManager.getActiveMessage(getApplication())
        _showWhatsNew.value = true
    }

    fun dismissWhatsNewWindow() {
        _showWhatsNew.value = false
        com.example.util.WhatsNewManager.markMessageSeen(getApplication(), _whatsNewMessage.value)
    }

    fun showBackgroundAutoSaveSetupSheet() {
        _showBackgroundAutoSaveSetup.value = true
    }

    fun dismissBackgroundAutoSaveSetupSheet() {
        _showBackgroundAutoSaveSetup.value = false
    }

    fun setDeveloperWhatsNewMessage(newMsg: String) {
        _whatsNewMessage.value = newMsg
        com.example.util.WhatsNewManager.setDeveloperMessage(getApplication(), newMsg)
    }

    fun resetDeveloperWhatsNewMessage() {
        com.example.util.WhatsNewManager.resetToDefaultMessage(getApplication())
        _whatsNewMessage.value = com.example.util.WhatsNewManager.CURRENT_MESSAGE
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
    val hasActiveTarget: Boolean,
    val distanceCoveredProgress: Float = 0f
)
