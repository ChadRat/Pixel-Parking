package com.PixelParking.wear.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text
import com.PixelParking.wear.datalayer.WearParkingStateHolder
import com.PixelParking.wear.presentation.components.WearGoogleMapsButton
import com.PixelParking.wear.presentation.components.WearWaypointShapes
import com.PixelParking.wear.sensor.WearSensorManager
import com.PixelParking.wear.theme.WearColorProvider
import com.PixelParking.wear.util.WearHapticHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

@Composable
fun WearWaypointScreen(
    sensorManager: WearSensorManager,
    modifier: Modifier = Modifier,
    rotaryScrollEvents: Flow<Float>? = null
) {
    val context = LocalContext.current
    val stateHolder = remember { WearParkingStateHolder.getInstance(context) }
    val parkingState by stateHolder.parkingState.collectAsState()
    val telemetry by sensorManager.telemetry.collectAsState()

    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }

    // Listen to Activity-level rotary events with direct raw delta dispatch
    LaunchedEffect(rotaryScrollEvents) {
        rotaryScrollEvents?.collect { delta ->
            scrollState.dispatchRawDelta(delta)
        }
    }

    // Synchronize target coordinates whenever parking state changes
    LaunchedEffect(parkingState) {
        sensorManager.setCrownOrientation(parkingState.crownOnRight)
        sensorManager.setTarget(
            lat = parkingState.latitude,
            lng = parkingState.longitude,
            active = parkingState.hasActiveSpot
        )
        if (parkingState.phoneDistanceMeters != null && parkingState.phoneRelativeAngle != null) {
            sensorManager.updateFromPhoneTelemetry(
                phoneDist = parkingState.phoneDistanceMeters!!,
                phoneAngle = parkingState.phoneRelativeAngle!!
            )
        }
    }

    // Request rotary focus on composition for crown rotation
    LaunchedEffect(Unit) {
        stateHolder.refreshFromDataLayer()
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Effective telemetry values (sensor manager or phone fallback)
    val effectiveDistance = if (telemetry.hasTarget) {
        telemetry.distanceMeters
    } else {
        parkingState.phoneDistanceMeters ?: 0f
    }

    val effectiveAngle = if (telemetry.hasTarget) {
        telemetry.relativeArrowAngle
    } else {
        val phoneAngle = parkingState.phoneRelativeAngle ?: 0f
        if (!parkingState.crownOnRight) {
            (phoneAngle + 180f) % 360f
        } else {
            phoneAngle
        }
    }

    val hasActiveTarget = parkingState.hasActiveSpot && (parkingState.latitude != 0.0 || parkingState.longitude != 0.0)

    // User is considered "very close" when within 15 meters of target
    val isVeryClose = hasActiveTarget && effectiveDistance in 0.01f..15.0f

    // Dynamic M3 theme colors following the main companion app's design system & wallpaper palette
    val themeColors = WearColorProvider.rememberAppThemeColors()
    val themeScallopColor = themeColors.scallopColor
    val themeArrowColor = themeColors.arrowColor
    val mapsContainerColor = themeColors.mapsContainerColor
    val mapsContentColor = themeColors.mapsContentColor
    val foundContainerColor = themeColors.foundContainerColor
    val foundContentColor = themeColors.foundContentColor

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onRotaryScrollEvent { rotaryEvent ->
                scrollState.dispatchRawDelta(rotaryEvent.verticalScrollPixels)
                true
            }
    ) {
        val screenHeight = this.maxHeight
        val screenWidth = this.maxWidth
        val shapeSize = (if (screenWidth < screenHeight) screenWidth * 0.925f else screenHeight * 0.925f).coerceIn(172.dp, 224.dp)

        val scrollVal = scrollState.value.toFloat()

        // Bottom Action Button scroll growth progress:
        // Starts expanding with a bigger delay (only after scrolling past 60px) and smoothly reaches full bowl shape by 140px.
        val buttonScrollProgress = remember(scrollVal) {
            if (scrollVal <= 60f) {
                0f
            } else {
                ((scrollVal - 60f) / 80f).coerceIn(0f, 1f)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (hasActiveTarget) {
                        Modifier.verticalScroll(scrollState)
                    } else {
                        Modifier
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // MAIN VIEWPORT: Mini Waypoint Tab (Scallop & direction arrow shapes)
            // Perfectly centered on watch screen at resting scroll offset (scroll = 0)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight)
                    .then(
                        if (!hasActiveTarget) {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        val loc = sensorManager.currentLocation.value
                                        val lat = loc?.latitude ?: 0.0
                                        val lng = loc?.longitude ?: 0.0
                                        val alt = loc?.altitude ?: 0.0

                                        // Trigger strong haptic confirmation
                                        WearHapticHelper.performCarFoundHaptic(context)

                                        stateHolder.saveCurrentSpot(lat, lng, alt, "Saved Location")
                                    }
                                )
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                WearWaypointShapes(
                    distanceMeters = effectiveDistance,
                    relativeArrowAngle = effectiveAngle,
                    hasActiveTarget = hasActiveTarget,
                    size = shapeSize,
                    scallopColor = themeScallopColor,
                    arrowColor = themeArrowColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // BOTTOM DYNAMIC BUTTON: Google Maps / Found Car Button
            // Automatically turns into Found Car when very close, and allows manual 500ms toggle anytime
            WearGoogleMapsButton(
                latitude = parkingState.latitude,
                longitude = parkingState.longitude,
                hasActiveTarget = hasActiveTarget,
                isVeryClose = isVeryClose,
                scrollProgress = buttonScrollProgress,
                onMarkCarFound = {
                    stateHolder.markCarFound()
                },
                mapsContainerColor = mapsContainerColor,
                mapsContentColor = mapsContentColor,
                foundContainerColor = foundContainerColor,
                foundContentColor = foundContentColor
            )

            // Strict minimal bottom clearance: user cannot scroll much distance past fully expanded button
            // Increased to 32.dp to allow scrolling down past the button edges so it's not cropped by round screens
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
