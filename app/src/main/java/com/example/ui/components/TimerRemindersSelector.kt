package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Timer Notification Reminders selector matching Screenshot_20260929-180307.png:
 * - 3 square-ish tiles by default side-by-side
 * - Clicking one selects it, smoothly morphing its shape to circular and changing color
 * - Up and down arrow chevrons appear only when selected, expanding smoothly above and below the shape
 * - Vertical drag or arrow clicks adjust duration
 * - Clicking off the active pill unselects it smoothly
 * - Compact, minimal vertical gaps above and below pills
 */
@Composable
fun TimerRemindersSelectorRow(
    reminders: List<Int>,
    onAdjust: (index: Int, delta: Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedIndex: Int? = null,
    onSelectedIndexChange: ((Int) -> Unit)? = null,
    onSelectedPillBoundsChanged: ((Rect?) -> Unit)? = null
) {
    val defaultMinutes = listOf(60, 30, 15)
    var localSelectedIndex by remember { mutableIntStateOf(-1) }
    val effectiveSelectedIndex = selectedIndex ?: localSelectedIndex
    val updateSelectedIndex: (Int) -> Unit = { newIndex ->
        if (onSelectedIndexChange != null) {
            onSelectedIndexChange(newIndex)
        } else {
            localSelectedIndex = newIndex
        }
        if (newIndex == -1) {
            onSelectedPillBoundsChanged?.invoke(null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("timer_reminders_selector_row"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (index in 0 until 3) {
                val minutes = reminders.getOrNull(index) ?: defaultMinutes.getOrElse(index) { 15 }
                val currentOnAdjust = rememberUpdatedState(onAdjust)
                val isSelected = effectiveSelectedIndex == index

                // Smooth morph: squircle (20dp) to circular (34dp = half of 68dp)
                val cornerRadius by animateDpAsState(
                    targetValue = if (isSelected) 34.dp else 20.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "reminder_corner_$index"
                )

                // Smooth color transition matching the screenshot
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f)
                    },
                    animationSpec = tween(durationMillis = 250),
                    label = "reminder_bg_$index"
                )

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(durationMillis = 250),
                    label = "reminder_text_$index"
                )

                val arrowAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "reminder_arrow_alpha_$index"
                )

                val arrowHeight by animateDpAsState(
                    targetValue = if (isSelected) 22.dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "reminder_arrow_height_$index"
                )

                Column(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        if (isSelected) {
                            onSelectedPillBoundsChanged?.invoke(coordinates.boundsInRoot())
                        }
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Top Chevron: Increment (expands only when clicked on / selected, directly above shape)
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = arrowHeight)
                            .graphicsLayer { alpha = arrowAlpha }
                            .clip(CircleShape)
                            .then(
                                if (isSelected) {
                                    Modifier.clickable { currentOnAdjust.value(index, 1) }
                                } else {
                                    Modifier
                                }
                            )
                            .testTag("timer_reminder_up_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (arrowAlpha > 0.1f && arrowHeight > 10.dp) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Increase reminder $index",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (isSelected) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // Squircle -> Circle shape with smooth morph animation
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(cornerRadius))
                            .background(bgColor)
                            .clickable {
                                updateSelectedIndex(if (isSelected) -1 else index)
                            }
                            .then(
                                if (isSelected) {
                                    Modifier.pointerInput(index, isSelected) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            var dragAccumulator = 0f
                                            var didDrag = false
                                            val dragThreshold = 18f

                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                if (!change.pressed) {
                                                    // Pointer released (Up) without drag -> tap to unselect
                                                    if (!didDrag) {
                                                        updateSelectedIndex(-1)
                                                    }
                                                    break
                                                }

                                                val deltaY = change.position.y - change.previousPosition.y
                                                if (abs(deltaY) > 0.5f) {
                                                    change.consume()
                                                    dragAccumulator += deltaY

                                                    if (abs(dragAccumulator) >= dragThreshold) {
                                                        didDrag = true
                                                        val steps = (-dragAccumulator / dragThreshold).toInt()
                                                        if (steps != 0) {
                                                            dragAccumulator += steps * dragThreshold
                                                            currentOnAdjust.value(index, steps)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .testTag("timer_reminder_pill_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (minutes > 0) "${minutes}m" else "Off",
                            fontFamily = HeavyRoundFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = textColor
                        )
                    }

                    if (isSelected) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // Bottom Chevron: Decrement (expands only when clicked on / selected, directly below shape)
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = arrowHeight)
                            .graphicsLayer { alpha = arrowAlpha }
                            .clip(CircleShape)
                            .then(
                                if (isSelected) {
                                    Modifier.clickable { currentOnAdjust.value(index, -1) }
                                } else {
                                    Modifier
                                }
                            )
                            .testTag("timer_reminder_down_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (arrowAlpha > 0.1f && arrowHeight > 10.dp) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Decrease reminder $index",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
