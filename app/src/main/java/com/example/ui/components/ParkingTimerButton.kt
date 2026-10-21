package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import com.example.util.HapticHelper
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.viewmodel.ParkingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

// Follows user's device system font
val HeavyRoundFont = FontFamily.Default

enum class TimerPillEditMode {
    IDLE,
    SET_HOURS,
    SET_MINUTES
}

/**
 * Very small, crisp triangular arrows (5dp x 2.5dp)
 */
@Composable
fun TriangularIndicator(
    isUp: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier.size(width = 5.dp, height = 2.5.dp)) {
        val path = Path().apply {
            if (isUp) {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
            } else {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
            }
            close()
        }
        drawPath(path = path, color = color)
    }
}

/**
 * Interactive Parking Timer Pill Component:
 * - Fixed digit positioning with overlay triangular arrows (up and down) that never cause layout shift.
 * - When timer is stopped, both the icon and the color parts of the box turn red.
 * - No seconds display.
 * - Single robust gesture detector for Tap, Drag, and 450ms Long-Press Stop.
 * - Tap 1: Edit hours (arrows visible above and below hours digits).
 * - Tap 2: Edit minutes (arrows visible above and below minutes digits).
 * - Tap 3: Start timer (only if navigation/spot is active).
 * - Press and hold (450ms): Turns red, triggers haptic, stops timer & shows system notification.
 */
@Composable
fun ParkingTimerButton(
    isRunning: Boolean,
    remainingSeconds: Long,
    isStopped: Boolean = false,
    viewModel: ParkingViewModel? = null,
    onDirectStart: () -> Unit = {},
    onDirectStop: () -> Unit = {},
    onAdjustHours: (Int) -> Unit = {},
    onAdjustMinutes: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    isFullWidth: Boolean = false
) {
    var editMode by remember { mutableStateOf(TimerPillEditMode.IDLE) }
    var isHoldingToStop by remember { mutableStateOf(false) }

    // Auto-exit edit mode back to idle after 8 seconds of inactivity
    LaunchedEffect(editMode) {
        if (editMode != TimerPillEditMode.IDLE) {
            delay(8000L)
            editMode = TimerPillEditMode.IDLE
        }
    }

    val vmIsStopped = viewModel?.timerIsStopped?.collectAsStateWithLifecycle()?.value ?: false
    val isAlarmActive = viewModel?.isAlarmActive?.collectAsStateWithLifecycle()?.value ?: false
    val effectiveIsStopped = isStopped || vmIsStopped || isHoldingToStop

    // Determine whether a timer is actively running, stopped with remaining time, or in edit mode
    val isTimerActivelyRunningOrStopped = isRunning || (effectiveIsStopped && remainingSeconds > 0L)
    val isActivelyEditing = editMode != TimerPillEditMode.IDLE

    // When no timer is set or a timer has ended, strictly display 00:00
    val effectiveRemaining = when {
        isAlarmActive -> 0L
        remainingSeconds <= 0L -> 0L
        !isTimerActivelyRunningOrStopped && !isActivelyEditing -> 0L
        else -> remainingSeconds.coerceAtLeast(0L)
    }

    val hours = (effectiveRemaining / 3600).toInt()
    val minutes = ((effectiveRemaining % 3600) / 60).toInt()

    // Determine target pill color:
    // If stopped or holding to stop -> Red
    // If running or has active time set or in edit mode -> Google Blue
    // Otherwise -> surface container gray
    val targetPillColor = when {
        effectiveIsStopped -> Color(0xFFD93025) // Warning Red
        isRunning -> Color(0xFF1A73E8) // Google Blue
        editMode != TimerPillEditMode.IDLE -> Color(0xFF1A73E8)
        effectiveRemaining > 0 -> Color(0xFF1A73E8)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    val pillColor by animateColorAsState(
        targetValue = targetPillColor,
        animationSpec = tween(durationMillis = 200),
        label = "timer_pill_bg"
    )

    val finalModifier = if (isFullWidth) {
        modifier
            .fillMaxWidth()
            .testTag("parking_timer_blue_btn")
    } else {
        modifier
            .testTag("parking_timer_blue_btn")
    }

    val dragThreshold = 24f // px per increment/decrement step

    val currentIsRunning by rememberUpdatedState(isRunning)
    val currentEditMode by rememberUpdatedState(editMode)
    val currentRemainingSeconds by rememberUpdatedState(remainingSeconds)

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
        shadowElevation = 2.dp,
        modifier = finalModifier
            .height(48.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startTime = System.currentTimeMillis()
                    var dragAccumulator = 0f
                    var hasDragged = false
                    var stopHoldTriggered = false

                    // Immediate 450ms hold timer that fires automatically while finger remains held down
                    val holdJob = coroutineScope.launch {
                        delay(450L)
                        if (!hasDragged && !stopHoldTriggered) {
                            if (currentIsRunning || currentRemainingSeconds > 0 || currentEditMode != TimerPillEditMode.IDLE) {
                                stopHoldTriggered = true
                                isHoldingToStop = true
                                editMode = TimerPillEditMode.IDLE
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (viewModel != null) {
                                    viewModel.stopParkingTimerWithNotification()
                                } else {
                                    onDirectStop()
                                }
                            }
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Main)
                        val elapsed = System.currentTimeMillis() - startTime

                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            holdJob.cancel()
                            if (isHoldingToStop) {
                                isHoldingToStop = false
                            }

                            // If not held and not dragged, treat as tap
                            if (!stopHoldTriggered && !hasDragged && elapsed < 450L) {
                                if (effectiveIsStopped) {
                                    // When stopped and red, a single tap restarts if time > 0, or cancels to 00:00
                                    editMode = TimerPillEditMode.IDLE
                                    if (currentRemainingSeconds <= 0L) {
                                        if (viewModel != null) {
                                            viewModel.resetTimerToZero()
                                        } else {
                                            onDirectStop()
                                        }
                                    } else {
                                        if (viewModel != null) {
                                            viewModel.startParkingTimer()
                                        } else {
                                            onDirectStart()
                                        }
                                    }
                                } else {
                                    when (currentEditMode) {
                                        TimerPillEditMode.IDLE -> {
                                            editMode = TimerPillEditMode.SET_HOURS
                                        }
                                        TimerPillEditMode.SET_HOURS -> {
                                            editMode = TimerPillEditMode.SET_MINUTES
                                        }
                                        TimerPillEditMode.SET_MINUTES -> {
                                            editMode = TimerPillEditMode.IDLE
                                            if (currentRemainingSeconds <= 0L) {
                                                if (viewModel != null) {
                                                    viewModel.resetTimerToZero()
                                                } else {
                                                    onDirectStop()
                                                }
                                            } else {
                                                if (viewModel != null) {
                                                    viewModel.startParkingTimer()
                                                } else {
                                                    onDirectStart()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            break
                        }

                        // Drag handling during editing
                        val currentPos = change.position
                        val prevPos = change.previousPosition
                        val deltaY = currentPos.y - prevPos.y

                        if (abs(deltaY) > 0.5f) {
                            if (!hasDragged && abs(currentPos.y - down.position.y) > 8f) {
                                hasDragged = true
                                holdJob.cancel()
                            }
                            if (currentEditMode != TimerPillEditMode.IDLE) {
                                change.consume()
                                dragAccumulator += deltaY
                                if (dragAccumulator <= -dragThreshold) {
                                    val steps = (-dragAccumulator / dragThreshold).toInt()
                                    dragAccumulator += steps * dragThreshold
                                    var changed = false
                                    if (currentEditMode == TimerPillEditMode.SET_HOURS) {
                                        val curH = (currentRemainingSeconds / 3600).toInt()
                                        if (curH < 99) changed = true
                                        if (viewModel != null) viewModel.adjustTimerHours(steps) else onAdjustHours(steps)
                                    } else if (currentEditMode == TimerPillEditMode.SET_MINUTES) {
                                        val curM = ((currentRemainingSeconds % 3600) / 60).toInt()
                                        if (curM < 59) changed = true
                                        if (viewModel != null) viewModel.adjustTimerMinutes(steps) else onAdjustMinutes(steps)
                                    }
                                    if (changed) {
                                        HapticHelper.performTimerDigitChangeHaptic(context)
                                    }
                                } else if (dragAccumulator >= dragThreshold) {
                                    val steps = (dragAccumulator / dragThreshold).toInt()
                                    dragAccumulator -= steps * dragThreshold
                                    var changed = false
                                    if (currentEditMode == TimerPillEditMode.SET_HOURS) {
                                        val curH = (currentRemainingSeconds / 3600).toInt()
                                        if (curH > 0) changed = true
                                        if (viewModel != null) viewModel.adjustTimerHours(-steps) else onAdjustHours(-steps)
                                    } else if (currentEditMode == TimerPillEditMode.SET_MINUTES) {
                                        val curM = ((currentRemainingSeconds % 3600) / 60).toInt()
                                        if (curM > 0) changed = true
                                        if (viewModel != null) viewModel.adjustTimerMinutes(-steps) else onAdjustMinutes(-steps)
                                    }
                                    if (changed) {
                                        HapticHelper.performTimerDigitChangeHaptic(context)
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Stopwatch Clock Icon (turns red when stopped)
            ExactStopwatchClockIcon(
                modifier = Modifier.size(34.dp),
                animateHands = isRunning,
                isRedStopped = effectiveIsStopped
            )

            // Pill with timer numbers
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(pillColor)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Hours Box: fixed position digits with absolute top/bottom arrow overlays
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (editMode == TimerPillEditMode.SET_HOURS) {
                            TriangularIndicator(
                                isUp = true,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 1.dp)
                            )
                        }

                        Text(
                            text = String.format(Locale.US, "%02d", hours),
                            fontFamily = HeavyRoundFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isRunning || effectiveRemaining > 0 || editMode != TimerPillEditMode.IDLE || effectiveIsStopped) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        if (editMode == TimerPillEditMode.SET_HOURS) {
                            TriangularIndicator(
                                isUp = false,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = ":",
                        fontFamily = HeavyRoundFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isRunning || effectiveRemaining > 0 || editMode != TimerPillEditMode.IDLE || effectiveIsStopped) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 1.dp)
                    )

                    // Minutes Box: fixed position digits with absolute top/bottom arrow overlays
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (editMode == TimerPillEditMode.SET_MINUTES) {
                            TriangularIndicator(
                                isUp = true,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 1.dp)
                            )
                        }

                        Text(
                            text = String.format(Locale.US, "%02d", minutes),
                            fontFamily = HeavyRoundFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isRunning || effectiveRemaining > 0 || editMode != TimerPillEditMode.IDLE || effectiveIsStopped) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        if (editMode == TimerPillEditMode.SET_MINUTES) {
                            TriangularIndicator(
                                isUp = false,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
