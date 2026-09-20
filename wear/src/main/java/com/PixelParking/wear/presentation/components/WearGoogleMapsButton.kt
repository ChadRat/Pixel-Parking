package com.PixelParking.wear.presentation.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Map
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import com.PixelParking.wear.util.WearHapticHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pixel Watch OS asymmetric stadium-bowl shape for the bottom button:
 * - Flat horizontal top edge spanning the middle
 * - Perfectly rounded convex pill shoulders on top-left and top-right (semi-circle caps)
 * - Deep circular arc curvature across the bottom that conforms to the round watch face chin
 */
val PixelWatchBowlShape: Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val rTop = h * 0.28f

    // Start at top-left shoulder
    moveTo(rTop * 1.5f, 0f)
    lineTo(w - (rTop * 1.5f), 0f)

    // Top-right rounded shoulder into side curve
    cubicTo(
        w - (rTop * 0.4f), 0f,
        w, rTop * 0.35f,
        w, rTop * 1.15f
    )

    // Right side smoothly flowing down into deep circular bottom bowl arc
    cubicTo(
        w, h * 0.72f,
        w * 0.76f, h,
        w * 0.50f, h
    )

    // Bottom center flowing up into left side curve
    cubicTo(
        w * 0.24f, h,
        0f, h * 0.72f,
        0f, rTop * 1.15f
    )

    // Top-left side flowing back into flat top edge
    cubicTo(
        0f, rTop * 0.35f,
        rTop * 0.4f, 0f,
        rTop * 1.5f, 0f
    )

    close()
}

/**
 * Dynamic Action Bottom Button:
 * - Dynamically turns into "Found Car" button when the user is very close to the car.
 * - User can manually switch between Google Maps and Found Car mode anytime by pressing and holding for 500ms.
 * - Displays vector Map icon or Checkmark icon (not emojis).
 * - Delivers strong enhanced haptic feedback on hold switch and found car confirmation.
 */
@Composable
fun WearGoogleMapsButton(
    latitude: Double,
    longitude: Double,
    hasActiveTarget: Boolean,
    isVeryClose: Boolean,
    scrollProgress: Float,
    onMarkCarFound: () -> Unit,
    modifier: Modifier = Modifier,
    mapsContainerColor: Color = Color(0xFFD4BFFF),
    mapsContentColor: Color = Color(0xFF381E72),
    foundContainerColor: Color = Color(0xFF00E676),
    foundContentColor: Color = Color(0xFF00391F)
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Internal state: track whether user manually toggled mode
    var manualModeOverride by remember { mutableStateOf<Boolean?>(null) }
    var isPressedDown by remember { mutableStateOf(false) }

    // When proximity changes significantly (or target changes), reset manual override if needed
    LaunchedEffect(isVeryClose, hasActiveTarget) {
        if (!hasActiveTarget) {
            manualModeOverride = null
        }
    }

    // Effective mode: manual override takes priority, otherwise auto-detect based on proximity (isVeryClose)
    val isFoundCarMode = manualModeOverride ?: isVeryClose

    val progress = scrollProgress.coerceIn(0f, 1f)
    val easedProgress = FastOutSlowInEasing.transform(progress)

    val animatedWidth = lerp(48.dp, 162.dp, easedProgress)
    val animatedHeight = lerp(22.dp, 68.dp, easedProgress)

    val contentAlpha = ((easedProgress - 0.22f) / 0.78f).coerceIn(0f, 1f)
    val contentScale = 0.70f + (0.30f * contentAlpha)

    val targetBgColor = if (isFoundCarMode) foundContainerColor else mapsContainerColor
    val targetFgColor = if (isFoundCarMode) foundContentColor else mapsContentColor

    val currentContainerColor by animateColorAsState(
        targetValue = if (isPressedDown) targetBgColor.copy(alpha = 0.82f) else targetBgColor,
        animationSpec = tween(durationMillis = 260),
        label = "action_btn_bg"
    )

    val currentContentColor by animateColorAsState(
        targetValue = targetFgColor,
        animationSpec = tween(durationMillis = 260),
        label = "action_btn_fg"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (isPressedDown) 0.93f else 1.0f,
        animationSpec = tween(100),
        label = "action_press_scale"
    )

    Box(
        modifier = modifier
            .width(animatedWidth)
            .height(animatedHeight)
            .scale(pressScale)
            .clip(PixelWatchBowlShape)
            .background(currentContainerColor)
            .pointerInput(hasActiveTarget, latitude, longitude, isFoundCarMode) {
                detectTapGestures(
                    onPress = {
                        isPressedDown = true
                        var holdToggled = false

                        val holdJob = coroutineScope.launch {
                            delay(500)
                            // 500ms hold elapsed -> toggle mode manually with strong haptics
                            holdToggled = true
                            val newMode = !isFoundCarMode
                            manualModeOverride = newMode

                            try {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                WearHapticHelper.performModeSwitchHaptic(context)
                            } catch (_: Exception) {}
                        }

                        val released = tryAwaitRelease()
                        isPressedDown = false
                        holdJob.cancel()

                        if (released && !holdToggled) {
                            // Quick tap released (< 500ms)
                            WearHapticHelper.performClickHaptic(context)
                            if (isFoundCarMode) {
                                // Mark car as found with strong celebration haptic feedback
                                WearHapticHelper.performCarFoundHaptic(context)
                                onMarkCarFound()
                            } else {
                                // Launch Google Maps
                                if (hasActiveTarget && latitude != 0.0 && longitude != 0.0) {
                                    launchGoogleMaps(context, latitude, longitude)
                                } else {
                                    Toast.makeText(context, "No parking coordinates synced", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (contentAlpha > 0.01f) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .alpha(contentAlpha)
                    .scale(contentScale)
            ) {
                Icon(
                    imageVector = if (isFoundCarMode) Icons.Filled.CheckCircle else Icons.Filled.Map,
                    contentDescription = null,
                    tint = currentContentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isFoundCarMode) "Found Car" else "Google Maps",
                    color = currentContentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun launchGoogleMaps(context: Context, lat: Double, lng: Double) {
    try {
        val navUri = Uri.parse("google.navigation:q=$lat,$lng&mode=w")
        val mapIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
            return
        }
    } catch (_: Exception) {}

    try {
        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Parked Car)")
        val fallbackIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(fallbackIntent)
    } catch (_: Exception) {
        Toast.makeText(context, "Google Maps unavailable", Toast.LENGTH_SHORT).show()
    }
}
