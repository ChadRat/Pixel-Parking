package com.example.ui.i18n

import android.content.Context
import androidx.compose.runtime.compositionLocalOf

data class AppStrings(
    // App & Tabs
    val appName: String,
    val tabHome: String,
    val tabRadar: String,
    val tabHistory: String,
    val tabBluetooth: String,
    val tabSettings: String,

    // Home Screen
    val activeParking: String,
    val readyToPark: String,
    val parkedVehicle: String,
    val myParkedCar: String,
    val parkedAt: String,
    val accuracy: String,
    val parkHereNow: String,
    val recordingGps: String,
    val foundCar: String,
    val waypointRadar: String,
    val googleMaps: String,
    val editDetails: String,
    val shareSpot: String,
    val noActiveSpotSubtitle: String,
    val autoSavedViaBt: String,
    val gpsLocationSaved: String,
    val noActiveParking: String,
    val recentSpots: String,
    val viewAll: String,
    val floor: String,
    val notes: String,
    val addPhoto: String,
    val changePhoto: String,
    val spotName: String,
    val enterSpotName: String,
    val enterFloor: String,
    val enterNotes: String,
    val save: String,
    val cancel: String,
    val confirm: String,
    val carFoundSuccess: String,
    val spotSavedSuccess: String,
    val noRecentSpots: String,
    val tapToNavigateRadar: String,

    // Compass Radar Screen
    val waypointRadarTitle: String,
    val meters: String,
    val locating: String,
    val carIsHere: String,
    val carVeryClose: String,
    val walkInThisDirection: String,
    val alignPhone: String,
    val radarMoveInstruction: String,
    val radarMoveInstructionIdle: String,
    val selectParkingSpotFirst: String,

    // History Screen
    val historyTitle: String,
    val historySubtitle: String,
    val searchSpots: String,
    val noSpotsFound: String,
    val noSpotsSubtitle: String,
    val clearAll: String,
    val clearAllConfirmTitle: String,
    val clearAllConfirmMsg: String,
    val delete: String,
    val share: String,
    val exportGpx: String,
    val navigate: String,

    // Bluetooth Screen
    val bluetoothTitle: String,
    val bluetoothSubtitle: String,
    val vehiclesActive: String,
    val oneVehicleActive: String,
    val noVehiclesActive: String,
    val autoParkActive: String,
    val bluetoothDisconnectDesc: String,
    val readyToAutoSave: String,
    val selectYourVehicles: String,
    val selectVehiclesSubtitle: String,
    val pairedDevices: String,
    val pairNew: String,
    val add: String,
    val noPairedDevices: String,
    val noPairedSubtitle: String,
    val autoParkEnabled: String,
    val tapToMonitor: String,
    val renameVehicle: String,
    val renameSubtitle: String,
    val vehicleName: String,
    val addVehicleTitle: String,
    val addVehicleSubtitle: String,
    val macAddressOptional: String,
    val pairDevice: String,

    // Settings Screen
    val settingsTitle: String,
    val settingsSubtitle: String,
    val languageSection: String,
    val languageTitle: String,
    val languageSubtitle: String,
    val english: String,
    val greek: String,
    val appearanceSection: String,
    val themeMode: String,
    val systemTheme: String,
    val lightTheme: String,
    val darkTheme: String,
    val additionalSettings: String,
    val sunScheduleTitle: String,
    val sunScheduleSubtitle: String,
    val oledMode: String,
    val oledSubtitle: String,
    val smoothThemeTransition: String,
    val smoothThemeTransitionSubtitle: String,
    val dynamicColor: String,
    val dynamicColorSubtitle: String,
    val ignoreBatteryOptimizations: String,
    val ignoreBatteryOptimizationsDesc: String,
    val hapticSection: String,
    val hapticSubtitle: String,
    val hapticAuto: String,
    val hapticLra: String,
    val hapticLraDesc: String,
    val hapticStandard: String,
    val hapticStandardDesc: String,
    val suggestedForYourDevice: String,
    val recommendedTag: String,
    val calibrateCompass: String,
    val compassCalibrationNeeded: String,
    val compassCalibrationInstruction: String,
    val compassMagneticInterference: String,
    val compassMagneticInterferenceInstruction: String,
    val compassRecalibrated: String,
    val mapsGroundingTitle: String,
    val mapsGroundingSubtitle: String,
    val groundingButton: String,
    val findNearbyParking: String,
    val landmarksHeading: String,
    val parkingTipsHeading: String,
    val groundingLoading: String,
    val groundingError: String,
    val askGroundingQuestion: String,
    val autoGroundingTitle: String,
    val dataStorageSection: String,
    val clearAllSpots: String,
    val clearSpotsSubtitle: String,
    val historyClearedToast: String,
    val aboutSection: String,
    val appVersion: String,
    val sourceCodeGitHub: String,
    val sourceCodeSubtitle: String,
    val licenseTitle: String,
    val licenseSubtitle: String,
    val viewLicenseTerms: String,
    val copyButton: String,
    val gnuOrgButton: String,
    val aboutTitle: String,
    val checkForUpdates: String,
    val updateAvailable: String,
    val download: String,
    val aboutSettingsSubtitle: String,
    val reportAnIssueTitle: String,
    val reportAnIssueUrl: String,
    val carBadgeStyleTitle: String,
    val carBadgeStyleSubtitle: String,
    val carBadgeStyleMaterial: String,
    val carBadgeStyleCinematic: String,
    val carBadgeStyleCapy: String = "Capy",
    val capyVariantTitle: String = "Capybara Model",
    val capyVariantSubtitle: String = "Choose the central capybara character appearance",
    val capyVariantBaby: String = "Baby Capy",
    val capyVariantAdult: String = "Adult Capy",
    val devCapyCarsTitle: String = "Capy cars",
    val devCapyCarsSubtitle: String = "Enable capibara Cars as a third car style option",

    // Sub-groups to avoid hitting the 254 parameter limit
    val timerStrings: TimerStrings = TimerStrings(),
    val dialogStrings: DialogStrings = DialogStrings(),
    val floorStrings: FloorStrings = FloorStrings(),
    val notificationStrings: NotificationStrings = NotificationStrings(),
    val devGeofenceStrings: DevGeofenceStrings = DevGeofenceStrings(),
    val exportStrings: ExportStrings = ExportStrings()
) {
    // Delegated properties for backwards compatibility throughout codebase
    val parkingTimerTitle: String get() = timerStrings.parkingTimerTitle
    val setParkingTimer: String get() = timerStrings.setParkingTimer
    val parkingTimerRunning: String get() = timerStrings.parkingTimerRunning
    val earlyReminderAlerts: String get() = timerStrings.earlyReminderAlerts
    val reminderMinutesBefore: String get() = timerStrings.reminderMinutesBefore
    val timerAlarmSound: String get() = timerStrings.timerAlarmSound
    val timerAlarmSoundDesc: String get() = timerStrings.timerAlarmSoundDesc
    val testAlarmSound: String get() = timerStrings.testAlarmSound
    val openDeviceSoundSettings: String get() = timerStrings.openDeviceSoundSettings
    val alarmExpiredTitle: String get() = timerStrings.alarmExpiredTitle
    val alarmExpiredMessage: String get() = timerStrings.alarmExpiredMessage
    val dismissAlarm: String get() = timerStrings.dismissAlarm
    val snooze5m: String get() = timerStrings.snooze5m

    val saveParkingSpotTitle: String get() = dialogStrings.saveParkingSpotTitle
    val editSpotDetailsTitle: String get() = dialogStrings.editSpotDetailsTitle
    val recordVehiclePositionSubtitle: String get() = dialogStrings.recordVehiclePositionSubtitle
    val editSpotDetailsSubtitle: String get() = dialogStrings.editSpotDetailsSubtitle
    val parkingCoordinatesHeader: String get() = dialogStrings.parkingCoordinatesHeader
    val savedParkingCoordinatesHeader: String get() = dialogStrings.savedParkingCoordinatesHeader
    val recordedParkingSpot: String get() = dialogStrings.recordedParkingSpot
    val currentVehicleLocation: String get() = dialogStrings.currentVehicleLocation
    val spotNameOrVehicleLabel: String get() = dialogStrings.spotNameOrVehicleLabel
    val floorOrParkingLevelLabel: String get() = dialogStrings.floorOrParkingLevelLabel
    val notesOrPillarLabel: String get() = dialogStrings.notesOrPillarLabel
    val notesOrPillarPlaceholder: String get() = dialogStrings.notesOrPillarPlaceholder
    val updateDetailsButton: String get() = dialogStrings.updateDetailsButton
    val saveSpotButton: String get() = dialogStrings.saveSpotButton

    val floorGroundLevel: String get() = floorStrings.floorGroundLevel
    val floorLevel1: String get() = floorStrings.floorLevel1
    val floorLevel2: String get() = floorStrings.floorLevel2
    val floorLevel3: String get() = floorStrings.floorLevel3
    val floorUndergroundP1: String get() = floorStrings.floorUndergroundP1
    val floorUndergroundP2: String get() = floorStrings.floorUndergroundP2
    val floorUndergroundP3: String get() = floorStrings.floorUndergroundP3
    val floorRoofDeck: String get() = floorStrings.floorRoofDeck

    val notificationChannelAlertsName: String get() = notificationStrings.notificationChannelAlertsName
    val notificationChannelAlertsDesc: String get() = notificationStrings.notificationChannelAlertsDesc
    val notificationChannelRadarName: String get() = notificationStrings.notificationChannelRadarName
    val notificationChannelRadarDesc: String get() = notificationStrings.notificationChannelRadarDesc
    val notificationChannelUpdatesName: String get() = notificationStrings.notificationChannelUpdatesName
    val notificationCarBluetoothDefault: String get() = notificationStrings.notificationCarBluetoothDefault
    val notificationDeviceDisconnected: String get() = notificationStrings.notificationDeviceDisconnected
    val notificationLocationSavedAt: String get() = notificationStrings.notificationLocationSavedAt
    val notificationFloorLabel: String get() = notificationStrings.notificationFloorLabel
    val notificationSavedViaBtDisconnect: String get() = notificationStrings.notificationSavedViaBtDisconnect
    val notificationActionGoogleMaps: String get() = notificationStrings.notificationActionGoogleMaps
    val notificationActionWaypointDirection: String get() = notificationStrings.notificationActionWaypointDirection
    val notificationActiveCompass: String get() = notificationStrings.notificationActiveCompass
    val notificationStopGuidance: String get() = notificationStrings.notificationStopGuidance
    val notificationTrackingDistance: String get() = notificationStrings.notificationTrackingDistance
    val notificationWaypointHaptics: String get() = notificationStrings.notificationWaypointHaptics
    val notificationStopHaptics: String get() = notificationStrings.notificationStopHaptics
    val notificationTimerAlertTitle: String get() = notificationStrings.notificationTimerAlertTitle
    val notificationTimerAlertBody: String get() = notificationStrings.notificationTimerAlertBody
    val notificationTimerExpiredTitle: String get() = notificationStrings.notificationTimerExpiredTitle
    val notificationTimerExpiredBody: String get() = notificationStrings.notificationTimerExpiredBody
    val notificationGpsAcquiring: String get() = notificationStrings.notificationGpsAcquiring
    val notificationLocationSaved: String get() = notificationStrings.notificationLocationSaved
    val notificationNavigatingTo: String get() = notificationStrings.notificationNavigatingTo
    val notificationMeterAlarmSet: String get() = notificationStrings.notificationMeterAlarmSet
    val notificationDeviceEnabledAutoPark: String get() = notificationStrings.notificationDeviceEnabledAutoPark
    val notificationRenamedVehicle: String get() = notificationStrings.notificationRenamedVehicle
    val notificationDeviceAddedAutoPark: String get() = notificationStrings.notificationDeviceAddedAutoPark
    val notificationOpenBluetoothSettings: String get() = notificationStrings.notificationOpenBluetoothSettings
    val notificationSimulatingBtDisconnect: String get() = notificationStrings.notificationSimulatingBtDisconnect
    val notificationNoActiveSpotToNavigate: String get() = notificationStrings.notificationNoActiveSpotToNavigate
    val notificationCouldNotOpenMaps: String get() = notificationStrings.notificationCouldNotOpenMaps
    val bluetoothDisconnectNote: String get() = notificationStrings.bluetoothDisconnectNote
    val autoSavedSpotDefaultName: String get() = notificationStrings.autoSavedSpotDefaultName

    val devGeofenceAutoParkTitle: String get() = devGeofenceStrings.devGeofenceAutoParkTitle
    val devGeofenceAutoParkSubtitle: String get() = devGeofenceStrings.devGeofenceAutoParkSubtitle
    val devSimulateGeofenceExit: String get() = devGeofenceStrings.devSimulateGeofenceExit
    val geofenceExitNote: String get() = devGeofenceStrings.geofenceExitNote
    val notificationGeofenceSaved: String get() = devGeofenceStrings.notificationGeofenceSaved
    val geofenceSavedSpotDefaultName: String get() = devGeofenceStrings.geofenceSavedSpotDefaultName

    val exportDataSection: String get() = exportStrings.exportDataSection
    val exportDataSubtitle: String get() = exportStrings.exportDataSubtitle
    val exportFormatLabel: String get() = exportStrings.exportFormatLabel
    val exportScopeLabel: String get() = exportStrings.exportScopeLabel
    val exportScopeActive: String get() = exportStrings.exportScopeActive
    val exportScopeAll: String get() = exportStrings.exportScopeAll
    val exportShareButton: String get() = exportStrings.exportShareButton
    val exportSuccessToast: String get() = exportStrings.exportSuccessToast
    val exportNoSpotsToast: String get() = exportStrings.exportNoSpotsToast
    val exportShareSubject: String get() = exportStrings.exportShareSubject
}

data class TimerStrings(
    val parkingTimerTitle: String = "Parking Timer",
    val setParkingTimer: String = "Set Parking Timer",
    val parkingTimerRunning: String = "Parking Timer",
    val earlyReminderAlerts: String = "Early Reminder Alerts",
    val reminderMinutesBefore: String = "Minutes before expiry",
    val timerAlarmSound: String = "Timer Alarm Sound",
    val timerAlarmSoundDesc: String = "Device alarm sound played when timer ends",
    val testAlarmSound: String = "Test Alarm Sound",
    val openDeviceSoundSettings: String = "Device Sound Settings",
    val alarmExpiredTitle: String = "Parking Timer Expired!",
    val alarmExpiredMessage: String = "Your parking timer has ended. Please check your vehicle.",
    val dismissAlarm: String = "Dismiss Alarm",
    val snooze5m: String = "Snooze 5 Min"
)

data class DialogStrings(
    val saveParkingSpotTitle: String = "Save Parking Spot",
    val editSpotDetailsTitle: String = "Edit Spot Details",
    val recordVehiclePositionSubtitle: String = "Record your vehicle position",
    val editSpotDetailsSubtitle: String = "Customized parking notes & floor",
    val parkingCoordinatesHeader: String = "PARKING COORDINATES",
    val savedParkingCoordinatesHeader: String = "SAVED PARKING COORDINATES",
    val recordedParkingSpot: String = "Recorded Parking Spot",
    val currentVehicleLocation: String = "Current vehicle location",
    val spotNameOrVehicleLabel: String = "Spot Name / Vehicle",
    val floorOrParkingLevelLabel: String = "Floor / Parking Level",
    val notesOrPillarLabel: String = "Pillar #, Section, or Notes",
    val notesOrPillarPlaceholder: String = "e.g., Near Pillar 42C, Blue elevator",
    val updateDetailsButton: String = "Update Details",
    val saveSpotButton: String = "Save Spot"
)

data class FloorStrings(
    val floorGroundLevel: String = "Ground Level",
    val floorLevel1: String = "Level 1",
    val floorLevel2: String = "Level 2",
    val floorLevel3: String = "Level 3",
    val floorUndergroundP1: String = "Underground P1",
    val floorUndergroundP2: String = "Underground P2",
    val floorUndergroundP3: String = "Underground P3",
    val floorRoofDeck: String = "Roof Deck"
)

data class NotificationStrings(
    val notificationChannelAlertsName: String = "Car Parking Alerts",
    val notificationChannelAlertsDesc: String = "Notifies when your car's Bluetooth disconnects and automatically saves parking location",
    val notificationChannelRadarName: String = "Active Waypoint Guidance",
    val notificationChannelRadarDesc: String = "Foreground waypoint tracking to navigate back to your parked car",
    val notificationChannelUpdatesName: String = "App Updates",
    val notificationCarBluetoothDefault: String = "Car Bluetooth",
    val notificationDeviceDisconnected: String = "%s Disconnected",
    val notificationLocationSavedAt: String = "Location automatically saved at %s",
    val notificationFloorLabel: String = "Floor:",
    val notificationSavedViaBtDisconnect: String = "Saved automatically via Bluetooth disconnect.",
    val notificationActionGoogleMaps: String = "Google Maps",
    val notificationActionWaypointDirection: String = "Waypoint Direction",
    val notificationActiveCompass: String = "Active Compass Direction: %s",
    val notificationStopGuidance: String = "Stop Guidance",
    val notificationTrackingDistance: String = "Tracking distance...",
    val notificationWaypointHaptics: String = "Waypoint Adaptive Haptics: %s",
    val notificationStopHaptics: String = "Stop Haptics",
    val notificationTimerAlertTitle: String = "Parking Timer",
    val notificationTimerAlertBody: String = "%d minutes remaining before your parking time ends.",
    val notificationTimerExpiredTitle: String = "Parking Timer Expired",
    val notificationTimerExpiredBody: String = "Time is up! Your parking session has ended. Move your car now.",
    val notificationGpsAcquiring: String = "Acquiring GPS location... Please ensure Location is enabled, or search for your address.",
    val notificationLocationSaved: String = "Saved actual location: %s",
    val notificationNavigatingTo: String = "Navigating to: %s",
    val notificationMeterAlarmSet: String = "Meter alarm set for %d mins",
    val notificationDeviceEnabledAutoPark: String = "'%s' enabled for Auto-Parking",
    val notificationRenamedVehicle: String = "Renamed vehicle to '%s'",
    val notificationDeviceAddedAutoPark: String = "'%s' added & enabled for Auto-Park",
    val notificationOpenBluetoothSettings: String = "Please open Bluetooth Settings from system settings",
    val notificationSimulatingBtDisconnect: String = "Simulating BT disconnect for '%s'...",
    val notificationNoActiveSpotToNavigate: String = "No active parking spot to navigate to",
    val notificationCouldNotOpenMaps: String = "Could not open Maps navigation",
    val bluetoothDisconnectNote: String = "Automatically saved upon Bluetooth disconnection.",
    val autoSavedSpotDefaultName: String = "%s Parking Spot"
)

data class DevGeofenceStrings(
    val devGeofenceAutoParkTitle: String = "Geofence auto-park",
    val devGeofenceAutoParkSubtitle: String = "Auto-save location when exiting car perimeter",
    val devSimulateGeofenceExit: String = "Simulate geofence exit",
    val geofenceExitNote: String = "Automatically saved upon exiting car perimeter.",
    val notificationGeofenceSaved: String = "Auto-saved parking location upon departing vehicle area: %s",
    val geofenceSavedSpotDefaultName: String = "Auto-Park (Geofence)"
)

data class ExportStrings(
    val exportDataSection: String = "Export & Share Data",
    val exportDataSubtitle: String = "Export parking locations to GPX or KML format for Google Earth & Maps",
    val exportFormatLabel: String = "File Format",
    val exportScopeLabel: String = "Export Selection",
    val exportScopeActive: String = "Active Spot",
    val exportScopeAll: String = "All History (%d)",
    val exportShareButton: String = "Export & Share",
    val exportSuccessToast: String = "Exported %d spot(s) to %s",
    val exportNoSpotsToast: String = "No parking spots available to export",
    val exportShareSubject: String = "Pixel Parking Location Export"
)

val EnglishStrings = AppStrings(
    appName = "Pixel Parking",
    tabHome = "Home",
    tabRadar = "Waypoint",
    tabHistory = "History",
    tabBluetooth = "Car BT",
    tabSettings = "Settings",

    activeParking = "Active Parking",
    readyToPark = "Ready to Park",
    parkedVehicle = "Parked Vehicle",
    myParkedCar = "My Parked Car",
    parkedAt = "Parked at",
    accuracy = "GPS Accuracy",
    parkHereNow = "Manual Parking Save",
    recordingGps = "Recording GPS...",
    foundCar = "Found Car",
    waypointRadar = "Waypoint Radar",
    googleMaps = "Google Maps",
    editDetails = "Edit Details",
    shareSpot = "Share Spot",
    noActiveSpotSubtitle = "Your parking spot will save automatically when your car's Bluetooth disconnects, or tap below to record your parking spot manually.",
    autoSavedViaBt = "Auto-saved via Bluetooth disconnect",
    gpsLocationSaved = "GPS location saved",
    noActiveParking = "No Active Parking",
    recentSpots = "Recent Spots",
    viewAll = "View All",
    floor = "Floor",
    notes = "Notes",
    addPhoto = "Add Photo",
    changePhoto = "Change Photo",
    spotName = "Spot Name",
    enterSpotName = "e.g. Row 4, Main Entrance",
    enterFloor = "e.g. Level 2, B1, Ground",
    enterNotes = "e.g. Near pillar 23",
    save = "Save",
    cancel = "Cancel",
    confirm = "Confirm",
    carFoundSuccess = "Vehicle marked as found!",
    spotSavedSuccess = "Parking spot recorded successfully",
    noRecentSpots = "No recent parking spots saved yet",
    tapToNavigateRadar = "Tap card to start Waypoint radar navigation",

    waypointRadarTitle = "Waypoint Radar",
    meters = "m",
    locating = "Locating...",
    carIsHere = "Car is right here",
    carVeryClose = "Car is very close",
    walkInThisDirection = "Walk in this direction",
    alignPhone = "Turn to align with target",
    radarMoveInstruction = "Move around. The shape fills as you get closer to the vehicle.",
    radarMoveInstructionIdle = "Once navigation starts move around. The shape fills as you get closer to the vehicle.",
    selectParkingSpotFirst = "Select a parking spot first",

    historyTitle = "Parking History",
    historySubtitle = "All your saved and recorded parking spots",
    searchSpots = "Search parking spots...",
    noSpotsFound = "No Parking Spots Found",
    noSpotsSubtitle = "Saved parking spots will appear here automatically.",
    clearAll = "Clear All",
    clearAllConfirmTitle = "Clear Parking History?",
    clearAllConfirmMsg = "This will permanently remove all saved parking spots from history.",
    delete = "Delete",
    share = "Share",
    exportGpx = "Export",
    navigate = "Navigate",

    bluetoothTitle = "Car Bluetooth",
    bluetoothSubtitle = "Set up your vehicles for automatic parking",
    vehiclesActive = "Vehicles Active",
    oneVehicleActive = "1 Vehicle Active",
    noVehiclesActive = "No Vehicles Active",
    autoParkActive = "Auto-Park Active",
    bluetoothDisconnectDesc = "When any configured vehicle disconnects, Pixel Parking immediately saves your precise GPS location automatically in the background.",
    readyToAutoSave = "Ready to auto-save spot on vehicle disconnect",
    selectYourVehicles = "Select Your Vehicles",
    selectVehiclesSubtitle = "Toggle on any vehicles from the paired list below. Whenever you park and turn off your car, your spot will be recorded instantly.",
    pairedDevices = "Paired Devices",
    pairNew = "Pair New",
    add = "Add",
    noPairedDevices = "No Paired Devices Found",
    noPairedSubtitle = "Pair your phone to your car's Bluetooth in Android Settings, or add a vehicle preset below.",
    autoParkEnabled = "Auto-Park Enabled",
    tapToMonitor = "Tap switch to monitor this vehicle",
    renameVehicle = "Rename Vehicle",
    renameSubtitle = "Set a custom display name for this vehicle Bluetooth device.",
    vehicleName = "Vehicle Name",
    addVehicleTitle = "Add Vehicle Bluetooth",
    addVehicleSubtitle = "Enter your vehicle's Bluetooth name or infotainment identifier to enable auto-parking triggers.",
    macAddressOptional = "MAC Address (Optional)",
    pairDevice = "Add Vehicle",

    settingsTitle = "Settings",
    settingsSubtitle = "Manage app appearance and language",
    languageSection = "Language",
    languageTitle = "Language",
    languageSubtitle = "Choose display language for Pixel Parking",
    english = "English",
    greek = "Ελληνικά (Greek)",
    appearanceSection = "Appearance & Theme",
    themeMode = "Theme Mode",
    systemTheme = "System",
    lightTheme = "Light",
    darkTheme = "Dark",
    additionalSettings = "Additional Settings",
    sunScheduleTitle = "Sunrise & Sunset Schedule",
    sunScheduleSubtitle = "Makes automatic light dark theming independent from the system.",
    oledMode = "Pure Black",
    oledSubtitle = "Deep black backgrounds for AMOLED screens",
    smoothThemeTransition = "Smooth Transition",
    smoothThemeTransitionSubtitle = "Hardware-accelerated cross-fade optimized for 60 to 144 Hz displays",
    dynamicColor = "Dynamic Color (Material You)",
    dynamicColorSubtitle = "Match system wallpaper palette",
    ignoreBatteryOptimizations = "Ignore Battery Optimizations",
    ignoreBatteryOptimizationsDesc = "Prevents background operations from being killed by the operating system",
    hapticSection = "Haptic Feedback & Vibration",
    hapticSubtitle = "Tactile vibration profiles for navigation & actions",
    hapticAuto = "Auto-Detect (Recommended)",
    hapticLra = "Linear Resonant Motor (LRA)",
    hapticLraDesc = "High-precision crisp micro ticks",
    hapticStandard = "Standard Haptic Motor",
    hapticStandardDesc = "Classic strong vibration pulses",
    suggestedForYourDevice = "Suggested for your device",
    recommendedTag = "Recommended",
    calibrateCompass = "Calibrate Compass",
    compassCalibrationNeeded = "Compass Calibration Suggested",
    compassCalibrationInstruction = "Wave phone in a figure-8 motion to restore accuracy",
    compassMagneticInterference = "Magnetic Interference Detected",
    compassMagneticInterferenceInstruction = "Move away from metal objects or magnets",
    compassRecalibrated = "Compass Recalibrated!",
    mapsGroundingTitle = "Google Maps Grounding",
    mapsGroundingSubtitle = "AI place detection & visual landmarks via Gemini 3.5 Flash",
    groundingButton = "Ground Spot with Maps",
    findNearbyParking = "Find Nearby Parking",
    landmarksHeading = "Nearby Visual Landmarks",
    parkingTipsHeading = "Parking Advice & Restrictions",
    groundingLoading = "Querying Google Maps via Gemini 3.5 Flash...",
    groundingError = "Unable to retrieve Maps grounding details.",
    askGroundingQuestion = "Ask Maps AI about this location...",
    autoGroundingTitle = "Auto-Enriched Spot Details",
    dataStorageSection = "Data & Storage",
    clearAllSpots = "Clear All Parking Spots",
    clearSpotsSubtitle = "Remove all recorded parking entries",
    historyClearedToast = "Parking history cleared",
    aboutSection = "About Pixel Parking",
    appVersion = "Version 1.2.0 • Material You",
    sourceCodeGitHub = "Source Code on GitHub",
    sourceCodeSubtitle = "github.com/ChadRat/Pixel-Parking",
    licenseTitle = "GNU General Public License v3.0",
    licenseSubtitle = "GPL-3.0 • Free & Open Source",
    viewLicenseTerms = "View License",
    copyButton = "Copy",
    gnuOrgButton = "GNU.org",
    aboutTitle = "About",
    checkForUpdates = "Check for updates",
    updateAvailable = "A new update is available",
    download = "Download",
    aboutSettingsSubtitle = "Settings",
    reportAnIssueTitle = "Report an Issue",
    reportAnIssueUrl = "issues.new",
    carBadgeStyleTitle = "Car Artwork Style",
    carBadgeStyleSubtitle = "Choose between minimalist flat vectors or dynamic cinematic illustrations",
    carBadgeStyleMaterial = "Material",
    carBadgeStyleCinematic = "Cinematic",
    carBadgeStyleCapy = "Capy",
    capyVariantTitle = "Capybara Model",
    capyVariantSubtitle = "Choose between the cute baby capybara or the adult capybara",
    capyVariantBaby = "Baby Capy",
    capyVariantAdult = "Adult Capy",
    devCapyCarsTitle = "Capy cars",
    devCapyCarsSubtitle = "Enable capibara Cars as a third car style option",

    timerStrings = TimerStrings(),
    dialogStrings = DialogStrings(),
    floorStrings = FloorStrings(),
    notificationStrings = NotificationStrings(),
    devGeofenceStrings = DevGeofenceStrings(),
    exportStrings = ExportStrings()
)

val GreekStrings = AppStrings(
    appName = "Pixel Parking",
    tabHome = "Αρχική",
    tabRadar = "Πλοήγηση",
    tabHistory = "Ιστορικό",
    tabBluetooth = "Bluetooth",
    tabSettings = "Ρυθμίσεις",

    activeParking = "Ενεργή Στάθμευση",
    readyToPark = "Έτοιμο για Στάθμευση",
    parkedVehicle = "Σταθμευμένο Όχημα",
    myParkedCar = "Το Αυτοκίνητό μου",
    parkedAt = "Στάθμευση στις",
    accuracy = "Ακρίβεια GPS",
    parkHereNow = "Χειροκίνητη Αποθήκευση",
    recordingGps = "Καταγραφή GPS...",
    foundCar = "Βρέθηκε το Όχημα",
    waypointRadar = "Ραντάρ Πλοήγησης",
    googleMaps = "Χάρτες Google",
    editDetails = "Επεξεργασία",
    shareSpot = "Κοινοποίηση",
    noActiveSpotSubtitle = "Η τοποθεσία στάθμευσης θα αποθηκευτεί αυτόματα μόλις αποσυνδεθεί το Bluetooth του αυτοκινήτου σας, ή πατήστε παρακάτω για να την καταγράψετε χειροκίνητα.",
    autoSavedViaBt = "Αποθηκεύτηκε αυτόματα μέσω Bluetooth",
    gpsLocationSaved = "Η τοποθεσία GPS αποθηκεύτηκε",
    noActiveParking = "Δεν υπάρχει ενεργή στάθμευση",
    recentSpots = "Πρόσφατες Θέσεις",
    viewAll = "Όλες",
    floor = "Όροφος",
    notes = "Σημειώσεις",
    addPhoto = "Προσθήκη Φωτογραφίας",
    changePhoto = "Αλλαγή Φωτογραφίας",
    spotName = "Όνομα Θέσης",
    enterSpotName = "π.χ. Σειρά 4, Κεντρική Είσοδος",
    enterFloor = "π.χ. Όροφος 2, B1, Ισόγειο",
    enterNotes = "π.χ. Κοντά στην κολώνα 23",
    save = "Αποθήκευση",
    cancel = "Ακύρωση",
    confirm = "Επιβεβαίωση",
    carFoundSuccess = "Το όχημα σημειώθηκε ως ευρεθέν!",
    spotSavedSuccess = "Η θέση στάθμευσης αποθηκεύτηκε επιτυχώς",
    noRecentSpots = "Δεν υπάρχουν ακόμη αποθηκευμένες θέσεις",
    tapToNavigateRadar = "Πατήστε για έναρξη πλοήγησης με Ραντάρ",

    waypointRadarTitle = "Ραντάρ Πλοήγησης",
    meters = "μ.",
    locating = "Εντοπισμός...",
    carIsHere = "Το όχημα είναι ακριβώς εδώ",
    carVeryClose = "Το όχημα είναι πολύ κοντά",
    walkInThisDirection = "Περπατήστε προς αυτή την κατεύθυνση",
    alignPhone = "Στρέψτε το τηλέφωνο προς το στόχο",
    radarMoveInstruction = "Κινηθείτε. Το σχήμα γεμίζει καθώς πλησιάζετε στο όχημα.",
    radarMoveInstructionIdle = "Μόλις ξεκινήσει η πλοήγηση, κινηθείτε. Το σχήμα γεμίζει καθώς πλησιάζετε στο όχημα.",
    selectParkingSpotFirst = "Επιλέξτε πρώτα μια θέση στάθμευσης",

    historyTitle = "Ιστορικό Στάθμευσης",
    historySubtitle = "Όλες οι αποθηκευμένες θέσεις στάθμευσης",
    searchSpots = "Αναζήτηση θέσεων...",
    noSpotsFound = "Δεν βρέθηκαν θέσεις στάθμευσης",
    noSpotsSubtitle = "Οι αποθηκευμένες θέσεις θα εμφανίζονται εδώ αυτόματα.",
    clearAll = "Εκκαθάριση Όλων",
    clearAllConfirmTitle = "Εκκαθάριση Ιστορικού;",
    clearAllConfirmMsg = "Αυτό θα διαγράψει οριστικά όλες τις αποθηκευμένες θέσεις από το ιστορικό.",
    delete = "Διαγραφή",
    share = "Κοινοποίηση",
    exportGpx = "Εξαγωγή",
    navigate = "Πλοήγηση",

    bluetoothTitle = "Bluetooth Αυτοκινήτου",
    bluetoothSubtitle = "Ρυθμίστε τα οχήματά σας για αυτόματη στάθμευση",
    vehiclesActive = "Ενεργά Οχήματα",
    oneVehicleActive = "1 Ενεργό Όχημα",
    noVehiclesActive = "Κανένα Ενεργό Όχημα",
    autoParkActive = "Αυτόματη Στάθμευση Ενεργή",
    bluetoothDisconnectDesc = "Όταν οποιοδήποτε ρυθμισμένο όχημα αποσυνδεθεί, το Pixel Parking αποθηκεύει άμεσα την ακριβή τοποθεσία GPS αυτόματα στο παρασκήνιο.",
    readyToAutoSave = "Έτοιμο για αυτόματη αποθήκευση κατά την αποσύνδεση",
    selectYourVehicles = "Επιλέξτε τα Οχήματά σας",
    selectVehiclesSubtitle = "Ενεργοποιήστε τα οχήματα από τη λίστα παρακάτω. Κάθε φορά που παρκάρετε και σβήνετε το αυτοκίνητο, η θέση καταγράφεται άμεσα.",
    pairedDevices = "Συζευγμένες Συσκευές",
    pairNew = "Νέα Σύζευξη",
    add = "Προσθήκη",
    noPairedDevices = "Δεν βρέθηκαν συζευγμένες συσκευές",
    noPairedSubtitle = "Συνδέστε το τηλέφωνό σας με το Bluetooth του αυτοκινήτου στις Ρυθμίσεις Android ή προσθέστε ένα όχημα παρακάτω.",
    autoParkEnabled = "Αυτόματη Στάθμευση Ενεργή",
    tapToMonitor = "Πατήστε για παρακολούθηση αυτού του οχήματος",
    renameVehicle = "Μετονομασία Οχήματος",
    renameSubtitle = "Ορίστε προσαρμοσμένο όνομα για αυτή τη συσκευή Bluetooth.",
    vehicleName = "Όνομα Οχήματος",
    addVehicleTitle = "Προσθήκη Bluetooth Οχήματος",
    addVehicleSubtitle = "Εισαγάγετε το όνομα Bluetooth του οχήματος για ενεργοποίηση αυτόματης στάθμευσης.",
    macAddressOptional = "Διεύθυνση MAC (Προαιρετικό)",
    pairDevice = "Προσθήκη Οχήματος",

    settingsTitle = "Ρυθμίσεις",
    settingsSubtitle = "Διαχείριση εμφάνισης και γλώσσας",
    languageSection = "Γλώσσα",
    languageTitle = "Γλώσσα",
    languageSubtitle = "Επιλέξτε τη γλώσσα προβολής του Pixel Parking",
    english = "English",
    greek = "Ελληνικά (Greek)",
    appearanceSection = "Εμφάνιση & Θέμα",
    themeMode = "Λειτουργία Θέματος",
    systemTheme = "Σύστημα",
    lightTheme = "Φωτεινό",
    darkTheme = "Σκοτεινό",
    additionalSettings = "Πρόσθετες ρυθμίσεις",
    sunScheduleTitle = "Εναλλαγή με βάση την ανατολή & δύση",
    sunScheduleSubtitle = "Αυτόματη εναλλαγή φωτεινού/σκοτεινού θέματος ανεξάρτητα από το σύστημα.",
    oledMode = "Καθαρό Μαύρο",
    oledSubtitle = "Βαθύ μαύρο φόντο για οθόνες AMOLED",
    smoothThemeTransition = "Ομαλή Μετάβαση",
    smoothThemeTransitionSubtitle = "Επιταχυνόμενη μετάβαση βελτιστοποιημένη για οθόνες 60 έως 144 Hz",
    dynamicColor = "Δυναμικό Χρώμα (Material You)",
    dynamicColorSubtitle = "Προσαρμογή στην ταπετσαρία συστήματος",
    ignoreBatteryOptimizations = "Παράβλεψη Βελτιστοποίησης Μπαταρίας",
    ignoreBatteryOptimizationsDesc = "Αποτρέπει τον τερματισμό των λειτουργιών παρασκηνίου από το λειτουργικό σύστημα",
    hapticSection = "Ανάδραση Αφής & Δόνηση",
    hapticSubtitle = "Προφίλ δόνησης για πλοήγηση & ενέργειες",
    hapticAuto = "Αυτόματος Εντοπισμός (Προτείνεται)",
    hapticLra = "Γραμμικός Κινητήρας (LRA)",
    hapticLraDesc = "Υψηλής ακρίβειας στιγμιαίες μικρο-δονήσεις",
    hapticStandard = "Τυπικός Κινητήρας Δόνησης",
    hapticStandardDesc = "Κλασικοί ισχυροί παλμοί δόνησης",
    suggestedForYourDevice = "Προτείνεται για τη συσκευή σας",
    recommendedTag = "Προτείνεται",
    calibrateCompass = "Βαθμονόμηση Πυξίδας",
    compassCalibrationNeeded = "Προτείνεται Βαθμονόμηση Πυξίδας",
    compassCalibrationInstruction = "Κινήστε το τηλέφωνο σε σχήμα 8 για επαναφορά ακρίβειας",
    compassMagneticInterference = "Ανιχνεύθηκε Μαγνητική Παρεμβολή",
    compassMagneticInterferenceInstruction = "Απομακρυνθείτε από μεταλλικά αντικείμενα ή μαγνήτες",
    compassRecalibrated = "Η πυξίδα βαθμονομήθηκε!",
    mapsGroundingTitle = "Δεδομένα Google Maps",
    mapsGroundingSubtitle = "Ανίχνευση τοποθεσίας & ορόσημα μέσω Gemini 3.5 Flash",
    groundingButton = "Εμπλουτισμός με Google Maps",
    findNearbyParking = "Εύρεση Κοντινού Πάρκινγκ",
    landmarksHeading = "Κοντινά Ορόσημα",
    parkingTipsHeading = "Συμβουλές & Περιορισμοί Πάρκινγκ",
    groundingLoading = "Αναζήτηση δεδομένων Google Maps μέσω Gemini...",
    groundingError = "Αδυναμία λήψης δεδομένων Google Maps.",
    askGroundingQuestion = "Ρωτήστε το Maps AI για αυτή την τοποθεσία...",
    autoGroundingTitle = "Αυτόματα Εμπλουτισμένα Στοιχεία",
    dataStorageSection = "Δεδομένα & Αποθήκευση",
    clearAllSpots = "Διαγραφή Όλων των Θέσεων",
    clearSpotsSubtitle = "Διαγραφή όλων των καταγεγραμμένων θέσεων",
    historyClearedToast = "Το ιστορικό στάθμευσης διαγράφηκε",
    aboutSection = "Σχετικά με το Pixel Parking",
    appVersion = "Έκδοση 1.2.0 • Material You",
    sourceCodeGitHub = "Πηγαίος Κώδικας στο GitHub",
    sourceCodeSubtitle = "github.com/ChadRat/Pixel-Parking",
    licenseTitle = "GNU General Public License v3.0",
    licenseSubtitle = "GPL-3.0 • Ελεύθερο Λογισμικό",
    viewLicenseTerms = "Προβολή Άδειας",
    copyButton = "Αντιγραφή",
    gnuOrgButton = "GNU.org",
    aboutTitle = "Σχετικά",
    checkForUpdates = "Έλεγχος για ενημερώσεις",
    updateAvailable = "Μια νέα ενημέρωση είναι διαθέσιμη",
    download = "Λήψη",
    aboutSettingsSubtitle = "Ρυθμίσεις",
    reportAnIssueTitle = "Αναφορά σφάλματος",
    reportAnIssueUrl = "issues.new",
    carBadgeStyleTitle = "Στιλ Γραφικών Αυτοκινήτων",
    carBadgeStyleSubtitle = "Επιλέξτε ανάμεσα σε λιτά διανυσματικά γραφικά Material ή δυναμική κινηματογραφική απεικόνιση",
    carBadgeStyleMaterial = "Material",
    carBadgeStyleCinematic = "Κινηματογραφικό",
    carBadgeStyleCapy = "Capy",
    capyVariantTitle = "Εμφάνιση Capybara",
    capyVariantSubtitle = "Επιλέξτε ανάμεσα σε μωρό ή ενήλικο Capybara",
    capyVariantBaby = "Μωρό Capy",
    capyVariantAdult = "Ενήλικο Capy",
    devCapyCarsTitle = "Capy cars",
    devCapyCarsSubtitle = "Ενεργοποίηση αυτοκινήτων Capybara ως 3η επιλογή στιλ",

    timerStrings = TimerStrings(
        parkingTimerTitle = "Χρονόμετρο Στάθμευσης",
        setParkingTimer = "Ρύθμιση Χρονομέτρου Στάθμευσης",
        parkingTimerRunning = "Χρονόμετρο Στάθμευσης",
        earlyReminderAlerts = "Ειδοποιήσεις Έγκαιρης Υπενθύμισης",
        reminderMinutesBefore = "Λεπτά πριν τη λήξη",
        timerAlarmSound = "Ήχος Ειδοποίησης Χρονομέτρου",
        timerAlarmSoundDesc = "Ήχος ξυπνητηριού συσκευής κατά τη λήξη του χρονομέτρου",
        testAlarmSound = "Δοκιμή Ήχου Ειδοποίησης",
        openDeviceSoundSettings = "Ρυθμίσεις Ήχου Συσκευής",
        alarmExpiredTitle = "Το Χρονόμετρο Στάθμευσης Έληξε!",
        alarmExpiredMessage = "Ο χρόνος στάθμευσής σας τελείωσε. Παρακαλούμε ελέγξτε το όχημά σας.",
        dismissAlarm = "Κλείσιμο Ειδοποίησης",
        snooze5m = "Αναβολή 5 λεπτά"
    ),
    dialogStrings = DialogStrings(
        saveParkingSpotTitle = "Αποθήκευση Θέσης Στάθμευσης",
        editSpotDetailsTitle = "Επεξεργασία Στοιχείων Θέσης",
        recordVehiclePositionSubtitle = "Καταγραφή θέσης οχήματος",
        editSpotDetailsSubtitle = "Προσαρμογή σημειώσεων & ορόφου",
        parkingCoordinatesHeader = "ΣΥΝΤΕΤΑΓΜΕΝΕΣ ΣΤΑΘΜΕΥΣΗΣ",
        savedParkingCoordinatesHeader = "ΑΠΟΘΗΚΕΥΜΕΝΕΣ ΣΥΝΤΕΤΑΓΜΕΝΕΣ",
        recordedParkingSpot = "Καταγεγραμμένη Θέση Στάθμευσης",
        currentVehicleLocation = "Τρέχουσα τοποθεσία οχήματος",
        spotNameOrVehicleLabel = "Όνομα Θέσης / Όχημα",
        floorOrParkingLevelLabel = "Όροφος / Επίπεδο Στάθμευσης",
        notesOrPillarLabel = "Αρ. Κολώνας, Τομέας ή Σημειώσεις",
        notesOrPillarPlaceholder = "π.χ. Κοντά στην κολώνα 42C, Μπλε ασανσέρ",
        updateDetailsButton = "Ενημέρωση Στοιχείων",
        saveSpotButton = "Αποθήκευση Θέσης"
    ),
    floorStrings = FloorStrings(
        floorGroundLevel = "Ισόγειο",
        floorLevel1 = "1ος Όροφος",
        floorLevel2 = "2ος Όροφος",
        floorLevel3 = "3ος Όροφος",
        floorUndergroundP1 = "Υπόγειο -1",
        floorUndergroundP2 = "Υπόγειο -2",
        floorUndergroundP3 = "Υπόγειο -3",
        floorRoofDeck = "Ταράτσα"
    ),
    notificationStrings = NotificationStrings(
        notificationChannelAlertsName = "Ειδοποιήσεις Στάθμευσης Αυτοκινήτου",
        notificationChannelAlertsDesc = "Ειδοποιεί όταν αποσυνδέεται το Bluetooth του αυτοκινήτου και αποθηκεύει αυτόματα την τοποθεσία στάθμευσης",
        notificationChannelRadarName = "Ενεργή Καθοδήγηση Σημείου",
        notificationChannelRadarDesc = "Παρακολούθηση σημείου στο παρασκήνιο για πλοήγηση πίσω στο σταθμευμένο αυτοκίνητό σας",
        notificationChannelUpdatesName = "Ενημερώσεις Εφαρμογής",
        notificationCarBluetoothDefault = "Bluetooth Αυτοκινήτου",
        notificationDeviceDisconnected = "%s: Αποσυνδέθηκε",
        notificationLocationSavedAt = "Η τοποθεσία αποθηκεύτηκε αυτόματα στο %s",
        notificationFloorLabel = "Όροφος:",
        notificationSavedViaBtDisconnect = "Αποθηκεύτηκε αυτόματα λόγω αποσύνδεσης Bluetooth.",
        notificationActionGoogleMaps = "Χάρτες Google",
        notificationActionWaypointDirection = "Κατεύθυνση Σημείου",
        notificationActiveCompass = "Ενεργή Κατεύθυνση Πυξίδας: %s",
        notificationStopGuidance = "Διακοπή Καθοδήγησης",
        notificationTrackingDistance = "Υπολογισμός απόστασης...",
        notificationWaypointHaptics = "Προσαρμοστική Απτική Σημείου: %s",
        notificationStopHaptics = "Διακοπή Απτικής",
        notificationTimerAlertTitle = "Χρονόμετρο Στάθμευσης",
        notificationTimerAlertBody = "Απομένουν %d λεπτά πριν λήξει ο χρόνος στάθμευσης.",
        notificationTimerExpiredTitle = "Το Χρονόμετρο Στάθμευσης Έληξε",
        notificationTimerExpiredBody = "Ο χρόνος έληξε! Η συνεδρία στάθμευσης ολοκληρώθηκε. Μετακινήστε το όχημά σας τώρα.",
        notificationGpsAcquiring = "Λήψη τοποθεσίας GPS... Βεβαιωθείτε ότι η Τοποθεσία είναι ενεργοποιημένη ή αναζητήστε τη διεύθυνσή σας.",
        notificationLocationSaved = "Αποθηκεύτηκε η τοποθεσία: %s",
        notificationNavigatingTo = "Πλοήγηση προς: %s",
        notificationMeterAlarmSet = "Το χρονόμετρο ρυθμίστηκε για %d λεπτά",
        notificationDeviceEnabledAutoPark = "'%s' ενεργοποιήθηκε για Αυτόματη Στάθμευση",
        notificationRenamedVehicle = "Το όχημα μετονομάστηκε σε '%s'",
        notificationDeviceAddedAutoPark = "'%s' προστέθηκε & ενεργοποιήθηκε για Αυτόματη Στάθμευση",
        notificationOpenBluetoothSettings = "Παρακαλούμε ανοίξτε τις Ρυθμίσεις Bluetooth από τις ρυθμίσεις συστήματος",
        notificationSimulatingBtDisconnect = "Προσομοίωση αποσύνδεσης BT για '%s'...",
        notificationNoActiveSpotToNavigate = "Δεν υπάρχει ενεργή θέση στάθμευσης για πλοήγηση",
        notificationCouldNotOpenMaps = "Δεν ήταν δυνατή η έναρξη πλοήγησης στους Χάρτες",
        bluetoothDisconnectNote = "Αποθηκεύτηκε αυτόματα λόγω αποσύνδεσης Bluetooth.",
        autoSavedSpotDefaultName = "Θέση Στάθμευσης %s"
    ),
    devGeofenceStrings = DevGeofenceStrings(
        devGeofenceAutoParkTitle = "Αυτόματη αποθήκευση με Geofence",
        devGeofenceAutoParkSubtitle = "Αυτόματη αποθήκευση τοποθεσίας κατά την έξοδο από την περίμετρο του αυτοκινήτου",
        devSimulateGeofenceExit = "Προσομοίωση εξόδου geofence",
        geofenceExitNote = "Αυτόματη αποθήκευση κατά την έξοδο από την περίμετρο του οχήματος.",
        notificationGeofenceSaved = "Η τοποθεσία στάθμευσης αποθηκεύτηκε αυτόματα καθώς απομακρυνθήκατε: %s",
        geofenceSavedSpotDefaultName = "Αυτόματη Στάθμευση (Geofence)"
    ),
    exportStrings = ExportStrings(
        exportDataSection = "Εξαγωγή & Κοινοποίηση Δεδομένων",
        exportDataSubtitle = "Εξαγωγή τοποθεσιών στάθμευσης σε GPX ή KML για Google Earth & Χάρτες",
        exportFormatLabel = "Μορφή Αρχείου",
        exportScopeLabel = "Επιλογή Εξαγωγής",
        exportScopeActive = "Ενεργή Θέση",
        exportScopeAll = "Όλο το Ιστορικό (%d)",
        exportShareButton = "Εξαγωγή & Κοινοποίηση",
        exportSuccessToast = "Εξήχθησαν %d θέσεις σε %s",
        exportNoSpotsToast = "Δεν υπάρχουν διαθέσιμες θέσεις στάθμευσης για εξαγωγή",
        exportShareSubject = "Εξαγωγή Τοποθεσιών Pixel Parking"
    )
)

fun getAppStrings(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.ENGLISH -> EnglishStrings
    AppLanguage.GREEK -> GreekStrings
}

fun Context.getAppStrings(): AppStrings = getAppStrings(getSavedAppLanguage())

fun localizeFloor(floor: String, strings: AppStrings): String {
    return when (floor) {
        "Ground Level", "Ισόγειο", "" -> strings.floorGroundLevel
        "Level 1", "1ος Όροφος", "Επίπεδο 1" -> strings.floorLevel1
        "Level 2", "2ος Όροφος", "Επίπεδο 2" -> strings.floorLevel2
        "Level 3", "3ος Όροφος", "Επίπεδο 3" -> strings.floorLevel3
        "Underground P1", "Υπόγειο -1", "Υπόγειο 1" -> strings.floorUndergroundP1
        "Underground P2", "Υπόγειο -2", "Υπόγειο 2" -> strings.floorUndergroundP2
        "Underground P3", "Υπόγειο -3", "Υπόγειο 3" -> strings.floorUndergroundP3
        "Roof Deck", "Ταράτσα" -> strings.floorRoofDeck
        else -> floor
    }
}

fun localizeSpotName(name: String, strings: AppStrings): String {
    return when (name) {
        "My Parked Car", "Το Αυτοκίνητό μου", "" -> strings.myParkedCar
        else -> name
    }
}

val LocalAppStrings = compositionLocalOf { EnglishStrings }
