package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * 3-pill Timer Notification Reminders selector matching Screenshot_20260923-000717.png:
 * - 3 horizontal rounded pill buttons (e.g. 60m, 30m, 15m)
 * - Up and down arrow chevrons above and below each pill
 * - Interacting via arrow taps or vertical dragging changes digits with tactile haptic feedback
 */
@Composable
fun TimerRemindersSelectorRow(
    reminders: List<Int>,
    onAdjust: (index: Int, delta: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillBgColor = Color(0xFF4A4E6B)
    val defaultMinutes = listOf(60, 30, 15)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("timer_reminders_selector_row"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (index in 0 until 3) {
            val minutes = reminders.getOrNull(index) ?: defaultMinutes.getOrElse(index) { 15 }
            val currentOnAdjust = rememberUpdatedState(onAdjust)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .testTag("timer_reminder_col_$index"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                // Top Chevron: Increment
                IconButton(
                    onClick = { currentOnAdjust.value(index, 1) },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("timer_reminder_up_$index")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Increase reminder $index",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Smaller Rounded Pill matching reference screenshot (e.g., 60m, 30m, 15m)
                // Supports both viewing and dragging up/down to adjust
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(pillBgColor)
                        .testTag("timer_reminder_pill_$index")
                        .pointerInput(index) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var dragAccumulator = 0f
                                val dragThreshold = 20f

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break

                                    val deltaY = change.position.y - change.previousPosition.y
                                    if (abs(deltaY) > 0.5f) {
                                        change.consume()
                                        dragAccumulator += deltaY

                                        if (dragAccumulator <= -dragThreshold) {
                                            val steps = (-dragAccumulator / dragThreshold).toInt()
                                            dragAccumulator += steps * dragThreshold
                                            currentOnAdjust.value(index, steps)
                                        } else if (dragAccumulator >= dragThreshold) {
                                            val steps = (dragAccumulator / dragThreshold).toInt()
                                            dragAccumulator -= steps * dragThreshold
                                            currentOnAdjust.value(index, -steps)
                                        }
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (minutes > 0) "${minutes}m" else "Off",
                        fontFamily = HeavyRoundFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                // Bottom Chevron: Decrement
                IconButton(
                    onClick = { currentOnAdjust.value(index, -1) },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("timer_reminder_down_$index")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Decrease reminder $index",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
