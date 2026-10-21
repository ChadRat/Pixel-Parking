# Persistent Background Bluetooth Disconnect Auto-Save Architecture

Resolve background termination and failed Bluetooth disconnect auto-parking when the app is closed on Android 12+ (Google Pixel and stock Android) by introducing a persistent lightweight foreground monitor service and an instant-save with background GPS refinement pipeline.

## User Review & Critical Decisions

> [!IMPORTANT]
> The architectural decisions below have been confirmed based on your preferences:

- **Target Device & OS**: Google Pixel and stock Android running Android 12, 13, 14, and 15+.
- **Background Execution Strategy**: Persistent lightweight foreground monitor service (`AutoParkLocationService`) started with `START_STICKY` and `stopWithTask="false"`. Runs a silent, low-priority status notification (`IMPORTANCE_LOW`) so Android never kills the app process when swiped away from Recents.
- **Location Strategy**: Two-stage instant-save. Disconnect immediately commits the best available last known location (0ms latency), followed by a 15-second background refinement pass that updates the spot with pinpoint GPS satellite coordinates and reverse geocoding as soon as the GPS receiver settles.

---

## 1. Overview & Core Concept

### What It Does
Ensures that whenever your phone disconnects from your vehicle's Bluetooth (such as turning off the car engine or walking away), your exact parking location is recorded and saved automatically—**even if the app has been closed, minimized, or swiped away from the Recent Apps screen days prior**.

### Target Audience & Persona
Drivers who expect zero-touch reliability without having to manually open the app before driving, worry about memory killers, or open the app after parking.

### Key Value
- **100% Zero-Touch Reliability**: Never miss a parking spot due to app process death or Android 12+ background execution limits.
- **Zero-Latency Response**: Eliminates the delay where the app used to wait 10 seconds for a GPS lock and abort if satellites were not acquired in time.
- **Instant Confidence**: Instant notification pops up upon turning off the engine with the parking spot saved, updating seamlessly if high-precision satellite data refines the accuracy.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **First-Launch / Persistent Monitor Toggle**:
   - The user opens the app.
   - The app verifies that background permissions and notification permissions are configured.
   - A modern Material 3 toggle in Settings and the Home Screen diagnostic banner: **"Always-On Bluetooth Detection"** (enabled by default).
   - The persistent silent notification appears in the notification shade: *"Auto-Park Active • Monitoring vehicle Bluetooth"*, indicating the system is actively guarding the car connection.

2. **Driving & Leaving the App Closed**:
   - The user closes or swipes away the app from Recents.
   - The persistent foreground service keeps the process alive in the background with negligible battery drain (<0.1% per day, as it only listens for Bluetooth events).
   - When entering the car, the notification updates to: *"Connected to [Car Name] • Drive in progress"*.

3. **Engine Off / Bluetooth Disconnect**:
   - The user parks and turns off the ignition.
   - **Stage 1 (Immediate - 0ms)**: Bluetooth disconnect fires. The service immediately captures the last known location, inserts the parking spot into Room DB, sends the notification (*"Parked at [Address/Coordinates]"*), and syncs with Wear OS.
   - **Stage 2 (Refinement - 0 to 12s)**: The service acquires live high-accuracy GPS fixes. If accuracy improves (e.g., from ±25m cellular/WiFi to ±4m GPS), it updates the existing spot in Room DB and refreshes the notification.
   - The monitor service resets back to standby state.

### Visual Identity & Theme
- **Material Design 3**: Matches existing Pixel Parking design with dynamic container elevation, clean typography, and standard M3 color scheme.
- **Persistent Notification Styling**: Compact, non-intrusive `IMPORTANCE_LOW` channel (silent, no sound, no popup banner, minimized icon in status bar).
- **Settings Screen Control**: Dedicated card in Settings allowing users to customize background monitoring behavior with clear status indicators.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Persistent Lightweight Monitor Service vs. Broadcast-Only Startup
- **Chosen Approach**: Run a persistent lightweight Foreground Service (`AutoParkLocationService`) with `START_STICKY` and an `onTaskRemoved` restart fallback.
- **Why**: Android 12+ (API 31+) explicitly forbids starting foreground services from background broadcast receivers (`ForegroundServiceStartNotAllowedException`). By starting the service while the app is in the foreground, Android permits it to run indefinitely. When the user closes the app, the service remains alive, preventing the OS from killing the process.
- **Alternatives Considered**: Relying solely on `ACTION_ACL_DISCONNECTED` broadcast in the manifest. While the broadcast is delivered, the app cannot reliably acquire fresh GPS coordinates or start a foreground service in the background, leading to silent drops and missing spots.

### Decision 2: Two-Stage Location Capture (Instant + Refinement) vs. Blocking Wait
- **Chosen Approach**: Immediately save the best available cached/network/cellular fix to Room DB, then run an asynchronous coroutine for up to 12 seconds to refine with satellite precision.
- **Why**: Previously, the app waited up to 10 seconds for a high-accuracy GPS fix; if GPS was cold or degraded inside a car/garage, `LocationHelper` timed out, returned `null`, and aborted the save completely. Two-stage capture guarantees a spot is *always* saved immediately.
- **Alternatives Considered**: Waiting 20 seconds for GPS. Fails if the user is in a concrete garage or if Android terminates the background task before the timer expires.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ANDROID SYSTEM LAYER                            │
│                                                                        │
│  Bluetooth Hardware / Kernel            Location Hardware / Fused      │
│  (ACL_CONNECTED / ACL_DISCONNECTED)     (GPS / Network / Passive)      │
└───────────────────┬──────────────────────────────────┬─────────────────┘
                    │                                  │
                    ▼                                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│                 AUTOPARK FOREGROUND MONITOR SERVICE                    │
│                 (AutoParkLocationService - START_STICKY)               │
│                                                                        │
│   • Persistent Foreground Notification (IMPORTANCE_LOW)                │
│   • Lifecycle Guard: onTaskRemoved() maintains background status      │
│   • Internal ACL & Profile Disconnect Listener                         │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │            DISCONNECT EVENT DISPATCH & LOCATION PIPELINE       │   │
│   │                                                                │   │
│   │   [Step 1: Immediate Commit]                                   │   │
│   │   Query lastLocation / cachedFix ──► Save to Room DB (0ms)     │   │
│   │                                  ──► Post Parked Notification  │   │
│   │                                  ──► Sync Wear OS DataLayer    │   │
│   │                                                                │   │
│   │   [Step 2: GPS Refinement Coroutine (10-12s)]                  │   │
│   │   Stream FusedLocation + GPS ──► If accuracy < existing accuracy│   │
│   │                              ──► Update Spot in Room DB        │   │
│   │                              ──► Update Notification & Geocode │   │
│   └────────────────────────────────────────────────────────────────┘   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          LOCAL PERSISTENCE                             │
│                                                                        │
│   • Room Database (ParkingSpotDao, BluetoothCarDeviceDao)              │
│   • SharedPreferences / DataStore (Cached Coordinates, Settings)       │
└────────────────────────────────────────────────────────────────────────┘
```

### Component & State Mapping

1. **`AutoParkLocationService.kt`**:
   - Add persistent monitor mode: `ACTION_START_MONITORING`.
   - Update `onStartCommand` to return `START_STICKY`.
   - Implement `onTaskRemoved(rootIntent: Intent?)` to ensure the service is restarted if the app task is swiped.
   - Add the two-stage save method: `executeTwoStageSave(rawAddress, rawName)`.
2. **`BluetoothDisconnectReceiver.kt`**:
   - Forward disconnect signals directly to `AutoParkLocationService` if running, or execute wake-locked fallback if triggered directly by the system.
   - Loosen overly strict filters (`ageBeforeTrigger > 3000ms`, `speed > 1.2m/s`) that previously caused valid fixes to be discarded.
3. **`LocationHelper.kt`**:
   - Add `getBestAvailableInstantFix(context)`: instantly retrieves Fused, GPS, Network, or shared-prefs fallback location without waiting.
   - Refactor `refineLocationFix(...)`: non-blocking coroutine that emits improved fixes to refine the spot.
4. **`AutoParkApplication.kt` & `MainActivity.kt`**:
   - Automatically initialize the monitor service on app boot and startup if auto-monitoring is enabled.
5. **`AndroidManifest.xml`**:
   - Ensure `AutoParkLocationService` has `stopWithTask="false"` and appropriate foreground service types (`location|connectedDevice`).
