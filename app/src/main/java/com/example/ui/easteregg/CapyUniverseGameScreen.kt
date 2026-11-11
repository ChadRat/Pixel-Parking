package com.example.ui.easteregg

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.HapticHelper
import com.example.util.HapticProfile
import java.util.Locale

/**
 * Full-screen 2D Universe Easter Egg game where the capybara drives the specific
 * selected car across smoothly transitioning biomes (Neo City, Emerald Forest,
 * Sapphire Lake, The Moon, Mars Dunes) with dynamic touch-based speed control.
 */
@Composable
fun CapyUniverseGameScreen(
    initialCarType: CapyCarType,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val context = LocalContext.current
        var currentCar by remember { mutableStateOf(initialCarType) }
        val engine = remember { CapyUniverseEngine() }

        var touchX by remember { mutableStateOf<Float?>(null) }
        var isTouching by remember { mutableStateOf(false) }
        var frameTick by remember { mutableLongStateOf(0L) }

        // HUD reactive states
        var currentSpeedDisplay by remember { mutableFloatStateOf(0f) }
        var currentDistanceDisplay by remember { mutableFloatStateOf(0f) }
        var treatsDisplay by remember { mutableIntStateOf(0) }
        var biomeNameDisplay by remember { mutableStateOf(UniverseBiome.NEO_CITY.displayName) }
        var isHonking by remember { mutableStateOf(false) }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                        touchX = down.position.x
                        isTouching = true

                        // Haptic start tick
                        HapticHelper.performClickTick(context, HapticProfile.AUTO)

                        do {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val ptr = event.changes.firstOrNull { it.id == down.id }
                            if (ptr != null && ptr.pressed) {
                                touchX = ptr.position.x
                            }
                        } while (event.changes.any { it.pressed })

                        touchX = null
                        isTouching = false
                    }
                }
                .testTag("capy_universe_game_dialog")
        ) {
            val screenWidthPx = constraints.maxWidth.toFloat()
            val screenHeightPx = constraints.maxHeight.toFloat()

            // Continuous 60fps simulation game loop
            LaunchedEffect(screenWidthPx) {
                var lastNanos = System.nanoTime()
                while (true) {
                    withFrameNanos { frameNanos ->
                        val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                        lastNanos = frameNanos

                        val effectiveWidth = if (screenWidthPx > 0f) screenWidthPx else 1080f

                        // Frame update
                        engine.update(
                            dt = dt,
                            touchX = touchX,
                            screenWidth = effectiveWidth
                        )

                        currentSpeedDisplay = engine.currentSpeedKmh
                        currentDistanceDisplay = engine.totalDistanceMeters
                        treatsDisplay = engine.treatsCollected
                        biomeNameDisplay = engine.currentBiomeName
                        frameTick = frameNanos // Triggers Canvas redraw every frame tick
                    }
                }
            }

            // 1. Full viewport Canvas for 2D Universe game rendering
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("capy_universe_canvas")
            ) {
                // Force Canvas redraw on every frame tick state update
                @Suppress("UNUSED_VARIABLE")
                val tick = frameTick

                val carW = screenWidthPx * 0.38f
                val carH = carW * 0.44f
                val carX = screenWidthPx * 0.34f
                val carY = screenHeightPx * 0.70f - carH * 0.38f

                engine.drawUniverse(
                    drawScope = this,
                    screenWidth = screenWidthPx,
                    screenHeight = screenHeightPx,
                    carX = carX,
                    carY = carY,
                    carWidth = carW,
                    carHeight = carH,
                    selectedCar = currentCar
                )
            }

            // 2. Top Header HUD: Close Button, Biome Badge, Car Switcher, Treats
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Close button with accessible touch target >= 48dp
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .testTag("capy_game_exit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Easter Egg Game",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Biome Badge Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.testTag("capy_game_biome_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = biomeNameDisplay,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDE047)
                            )
                        }
                    }

                    // Treats & Space Stars Counter
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.testTag("capy_game_treats_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = "Treats Collected",
                                tint = Color(0xFFFACC15),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$treatsDisplay",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Car Selection Quick Bar (allows driving any of the 4 capybara cars!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CapyCarType.entries.forEach { carType ->
                        val isSelected = carType == currentCar
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) carType.primaryColor else Color.Black.copy(alpha = 0.50f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.20f)
                            ),
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    currentCar = carType
                                    HapticHelper.performClickTick(context, HapticProfile.AUTO)
                                }
                                .testTag("select_car_${carType.name}")
                        ) {
                            Text(
                                text = when (carType) {
                                    CapyCarType.HAPPY_CABRIO -> "Cabrio"
                                    CapyCarType.COOL_SUPERCAR -> "Supercar"
                                    CapyCarType.LUXURY_SUV -> "SUV"
                                    CapyCarType.DRIFT_TUNER -> "Drift"
                                },
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Honk / Squeak Floating Action Button
            IconButton(
                onClick = {
                    isHonking = true
                    HapticHelper.performConfirmationHaptic(context, HapticProfile.AUTO)
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    .testTag("capy_game_honk_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Car Horn / Capybara Squeak",
                    tint = Color(0xFFFDE047),
                    modifier = Modifier.size(26.dp)
                )
            }

            // Animated Honk Note Bubble
            AnimatedVisibility(
                visible = isHonking,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 120.dp)
            ) {
                LaunchedEffect(isHonking) {
                    if (isHonking) {
                        kotlinx.coroutines.delay(1200)
                        isHonking = false
                    }
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE047))
                ) {
                    Text(
                        text = "* SQUEAK-SQUEAK! HONK! *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFDE047),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // 3. Bottom HUD: Live Speedometer, Distance, and Interactive Touch Dynamic Guide
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Live Digital Speedometer
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Speedometer",
                                    tint = if (engine.isTurboActive) Color(0xFFFF5252) else Color(0xFF40C4FF),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = String.format(Locale.US, "%d", currentSpeedDisplay.toInt()),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (engine.isTurboActive) Color(0xFFFF5252) else Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "km/h",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            }

                            // Total Distance
                            Text(
                                text = String.format(Locale.US, "%.1f km", currentDistanceDisplay / 1000f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Dynamic Speed Visualizer Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            val speedFraction = (currentSpeedDisplay / 220f).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(speedFraction)
                                    .height(6.dp)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(Color(0xFF38BDF8), Color(0xFFFACC15), Color(0xFFEF4444))
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Dynamic control hint
                        Text(
                            text = if (isTouching) {
                                if (engine.isTurboActive) "TURBO BOOST ACTIVE!" else "HOLDING SCREEN - DRIVING ACTIVE"
                            } else {
                                "Press and hold screen to drive (edges = faster turbo)"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (engine.isTurboActive) Color(0xFFFF5252) else Color.White.copy(alpha = 0.70f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
