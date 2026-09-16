package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.ParkingViewModel
import com.example.util.HapticHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Parking Timer Sheet:
 * - Opens to 65% of screen height and can be dragged to cover 95% of the screen.
 * - Fully affected by Material wallpaper dynamic theming (MaterialTheme.colorScheme).
 * - Thick circular timer ring with squiggly animated progress.
 * - Minutes only: Clockwise drag adds minutes, counter-clockwise subtracts minutes.
 * - Tap inside circle center to Play / Pause.
 * - Press & hold inside circle center for 3s to reset to 0 (releases strong haptic tick every 0.5s).
 * - Notification Reminders: 3 numbers (minutes) side-by-side with no box behind them,
 *   dragged upwards to add minutes, dragged downwards to subtract minutes.
 * - Explanatory text below notification reminders explaining how to use the main timer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingTimerDialog(
    viewModel: ParkingViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current

    val totalSeconds by viewModel.timerTotalSeconds.collectAsState()
    val remainingSeconds by viewModel.timerRemainingSeconds.collectAsState()
    val isRunning by viewModel.timerIsRunning.collectAsState()
    val reminders by viewModel.timerRemindersMinutes.collectAsState()

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val minSheetHeight = screenHeight * 0.65f
    val maxSheetHeight = screenHeight * 0.95f

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("parking_timer_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minSheetHeight, max = maxSheetHeight)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Clean Title only
            Text(
                text = strings.parkingTimerTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
            )
            Text(
                text = if (isRunning) "Timer Running • Tap circle to pause" else if (remainingSeconds > 0) "Ready • Tap circle to start" else "Drag ring to set minutes",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Thicker Circular Timer with Integrated Tap-to-Toggle and 3s Hold-to-Reset ---
            ParkingTimerCircleView(
                totalSeconds = totalSeconds,
                remainingSeconds = remainingSeconds,
                isRunning = isRunning,
                onDragRotate = { isClockwise ->
                    viewModel.adjustTimerByDrag(isClockwise)
                },
                onTogglePlayPause = {
                    viewModel.toggleTimerPlayPause()
                },
                onResetToZero = {
                    viewModel.resetTimerToZero()
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Quick preset duration chips (Minutes Only, affected by wallpaper theming)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(5 to "+5m", 10 to "+10m", 15 to "+15m", 30 to "+30m").forEach { (mins, label) ->
                    AssistChip(
                        onClick = {
                            viewModel.addTimerMinutes(mins)
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Notification Reminders (3 items horizontally with no box behind them) ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NOTIFICATION REMINDERS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Swipe number up/down to adjust alert minutes before expiry",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3 Horizontal Reminder Boxes (no background container box, compact, drag up/down)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentReminders = when {
                        reminders.size >= 3 -> reminders.take(3)
                        reminders.size == 2 -> reminders + listOf(5)
                        reminders.size == 1 -> reminders + listOf(10, 5)
                        else -> listOf(15, 10, 5)
                    }

                    currentReminders.forEachIndexed { index, minutes ->
                        ReminderDragBox(
                            index = index,
                            minutes = minutes,
                            onAdjust = { isAdd ->
                                viewModel.adjustReminderMinutes(index, isAdd)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // --- Explanatory Guide: How to set minutes of main timer ---
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timer_instructions_guide")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "HOW TO USE PARKING TIMER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "• Drag outer ring clockwise to add minutes, or counter-clockwise to subtract.\n• Tap circle center to Start or Pause countdown.\n• Press & hold circle center for 3s to reset timer to 0.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Compact draggable reminder box:
 * - Sitting horizontally with no container box behind it.
 * - Dragging upwards adds minutes (+1).
 * - Dragging downwards subtracts minutes (-1).
 * - Styled with dynamic Material wallpaper theme colors.
 */
@Composable
fun ReminderDragBox(
    index: Int,
    minutes: Int,
    onAdjust: (isAdd: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val stepPx = 20.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .width(92.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount.y
                        val step = stepPx.toPx()
                        if (dragAccumulator <= -step) {
                            // Dragged upwards -> add minutes
                            onAdjust(true)
                            dragAccumulator += step
                        } else if (dragAccumulator >= step) {
                            // Dragged downwards -> subtract minutes
                            onAdjust(false)
                            dragAccumulator -= step
                        }
                    }
                )
            }
    ) {
        // Up micro arrow indicator
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "Swipe up to add minutes",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )

        // Compact number pill
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("reminder_box_$index")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (minutes > 0) "${minutes}m" else "Off",
                    fontFamily = HeavyRoundFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Down micro arrow indicator
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Swipe down to subtract minutes",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Circular Timer View:
 * - Entirely styled with Material wallpaper dynamic theming colors.
 * - Thick track ring (`strokeWidth = 24.dp`).
 * - Light top indicator dot at 12 o'clock.
 * - Dynamic squiggly wavy progress circle with wave animation.
 * - Outer ring drag:
 *   - Clockwise dragging adds minutes.
 *   - Counter-clockwise dragging subtracts minutes.
 * - Center area:
 *   - Tap: Toggles Play / Pause.
 *   - Press & hold for 3 seconds: Strong haptic feedback released every 0.5s, then resets timer to 0!
 *   - Visual progress indicator around center shows the 3s hold progress.
 *   - Huge digital countdown rendered in Fredoka heavy rounded font.
 */
@Composable
fun ParkingTimerCircleView(
    totalSeconds: Long,
    remainingSeconds: Long,
    isRunning: Boolean,
    onDragRotate: (isClockwise: Boolean) -> Unit,
    onTogglePlayPause: () -> Unit,
    onResetToZero: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "squiggly_wave")
    val animatedPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Calculate progress (0.0 to 1.0)
    val progress = if (totalSeconds > 0) {
        ((totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    // Minutes-only time formatting for display (e.g. 45:00, 08:30)
    val totalMinutes = (remainingSeconds / 60).toInt()
    val seconds = (remainingSeconds % 60).toInt()
    val timeText = String.format(Locale.US, "%02d:%02d", totalMinutes, seconds)

    var lastAngle by remember { mutableFloatStateOf(0f) }
    var angleAccumulator by remember { mutableFloatStateOf(0f) }

    // Dynamic theming colors
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val progressColor = MaterialTheme.colorScheme.primary
    val indicatorColor = MaterialTheme.colorScheme.primary
    val leadGlowColor = MaterialTheme.colorScheme.primaryContainer

    // Hold to reset progress (0.0 to 1.0)
    var holdProgress by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .size(290.dp)
            .testTag("parking_timer_circle_view"),
        contentAlignment = Alignment.Center
    ) {
        // 1. Canvas with outer ring rotation drag
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            lastAngle = atan2(offset.y - cy, offset.x - cx)
                            angleAccumulator = 0f
                        },
                        onDrag = { change, dragAmount ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val currentPos = change.position
                            val prevPos = currentPos - dragAmount

                            val prevAngle = atan2(prevPos.y - cy, prevPos.x - cx)
                            val currentAngle = atan2(currentPos.y - cy, currentPos.x - cx)

                            var delta = currentAngle - prevAngle
                            if (delta > PI) delta -= (2 * PI).toFloat()
                            if (delta < -PI) delta += (2 * PI).toFloat()

                            angleAccumulator += delta

                            // Every ~14 degrees (0.24 rad) of rotation triggers 1 minute step
                            val stepThreshold = 0.24f
                            if (angleAccumulator >= stepThreshold) {
                                // Clockwise rotation -> Add minutes
                                onDragRotate(true)
                                angleAccumulator -= stepThreshold
                            } else if (angleAccumulator <= -stepThreshold) {
                                // Counter-clockwise rotation -> Subtract minutes
                                onDragRotate(false)
                                angleAccumulator += stepThreshold
                            }
                        }
                    )
                }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = (size.minDimension / 2f) - 26.dp.toPx()
            val trackStrokeWidth = 24.dp.toPx() // Thicker ring track

            // A. Background circular track (Material dynamic theme)
            drawCircle(
                color = trackColor,
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = trackStrokeWidth)
            )

            // B. Top Indicator Dot at 12 o'clock (-PI / 2)
            val pillAngle = -PI.toFloat() / 2f
            val pillX = cx + radius * cos(pillAngle)
            val pillY = cy + radius * sin(pillAngle)
            drawCircle(
                color = indicatorColor,
                radius = 12.dp.toPx(),
                center = Offset(pillX, pillY)
            )

            // C. Thick Squiggly Line Progress Circle (Material dynamic theme)
            if (progress > 0.005f) {
                val startAngle = -PI.toFloat() / 2f
                val sweepAngle = progress * (2f * PI.toFloat())
                val steps = (sweepAngle * 45f).toInt().coerceAtLeast(15)

                val squigglyPath = Path()
                val waveAmplitude = 5.dp.toPx()
                val waveFrequency = 18f // 18 oscillations per circle

                for (i in 0..steps) {
                    val theta = startAngle + (sweepAngle * (i.toFloat() / steps.toFloat()))
                    val waveOffset = waveAmplitude * sin((theta * waveFrequency) + animatedPhase)
                    val currentR = radius + waveOffset

                    val px = cx + currentR * cos(theta)
                    val py = cy + currentR * sin(theta)

                    if (i == 0) {
                        squigglyPath.moveTo(px, py)
                    } else {
                        squigglyPath.lineTo(px, py)
                    }
                }

                drawPath(
                    path = squigglyPath,
                    color = progressColor,
                    style = Stroke(
                        width = 18.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Leading glowing head circle at end of progress
                val endTheta = startAngle + sweepAngle
                val endR = radius + (waveAmplitude * sin((endTheta * waveFrequency) + animatedPhase))
                val endX = cx + endR * cos(endTheta)
                val endY = cy + endR * sin(endTheta)

                drawCircle(
                    color = leadGlowColor,
                    radius = 11.dp.toPx(),
                    center = Offset(endX, endY)
                )
            }
        }

        // 2. Center Touch Area:
        // - Tap to Play / Pause
        // - Press and hold for 3 seconds to Reset to 0 (with haptic tick every 0.5s)
        Box(
            modifier = Modifier
                .size(185.dp)
                .clip(CircleShape)
                .pointerInput(isRunning, remainingSeconds) {
                    coroutineScope {
                        while (true) {
                            val down = awaitPointerEventScope { awaitFirstDown(requireUnconsumed = false) }
                            val startTime = System.currentTimeMillis()
                            var isResetTriggered = false

                            val holdJob = launch {
                                // 3.0 seconds hold broken into 6 x 500ms intervals
                                for (step in 1..6) {
                                    delay(500)
                                    holdProgress = step / 6f
                                    HapticHelper.performHoldStepTick(context)
                                }
                                // Reached full 3.0 seconds!
                                isResetTriggered = true
                                HapticHelper.performResetConfirmation(context)
                                onResetToZero()
                                holdProgress = 0f
                            }

                            val upOrCancel = awaitPointerEventScope {
                                waitForUpOrCancellation()
                            }

                            holdJob.cancel()
                            holdProgress = 0f

                            if (upOrCancel != null && !isResetTriggered) {
                                val duration = System.currentTimeMillis() - startTime
                                if (duration < 500) {
                                    // Quick tap -> Toggle Play / Pause
                                    onTogglePlayPause()
                                }
                            }
                        }
                    }
                }
                .testTag("timer_center_interactive_area"),
            contentAlignment = Alignment.Center
        ) {
            // Visual hold-to-reset progress indicator ring
            if (holdProgress > 0f) {
                CircularProgressIndicator(
                    progress = { holdProgress },
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    color = MaterialTheme.colorScheme.error,
                    strokeWidth = 5.dp
                )
            }

            // Digital Countdown Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = timeText,
                    fontFamily = HeavyRoundFont,
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp,
                    modifier = Modifier.testTag("timer_countdown_text")
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (holdProgress > 0f) {
                        "HOLDING TO RESET..."
                    } else if (isRunning) {
                        "TAP TO PAUSE"
                    } else if (remainingSeconds > 0) {
                        "TAP TO START"
                    } else {
                        "DRAG RING"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (holdProgress > 0f) {
                        MaterialTheme.colorScheme.error
                    } else if (isRunning) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    },
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}
