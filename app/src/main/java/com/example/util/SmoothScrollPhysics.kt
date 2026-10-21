package com.example.util

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import kotlin.math.abs

val LocalDisableSmoothScroll = compositionLocalOf { false }

/**
 * Creates an ultra-smooth, silky fling behavior with enhanced inertia and smooth exponential/spline deceleration.
 * Provides a fluid, momentum-preserving glide that feels exceptionally responsive and smooth at 60Hz and 120Hz.
 *
 * When smooth scrolling is removed (disabled via the Developer Options toggle), it cleanly returns Compose's
 * default simple and smooth fling behavior (ScrollableDefaults.flingBehavior()) without custom inertia physics.
 */
@Composable
fun rememberSmoothFlingBehavior(
    frictionMultiplier: Float = 0.82f,
    forceDisable: Boolean? = null
): FlingBehavior {
    val isLocalDisabled = LocalDisableSmoothScroll.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val isSmoothDisabled = forceDisable ?: (isLocalDisabled || remember(context) {
        try {
            context.getSharedPreferences("parking_app_prefs", android.content.Context.MODE_PRIVATE)
                .getBoolean("dev_simple_scroll_enabled", false)
        } catch (_: Exception) {
            false
        }
    })

    if (isSmoothDisabled) {
        return ScrollableDefaults.flingBehavior()
    }

    val splineDecay = rememberSplineBasedDecay<Float>()
    val expDecay = remember(frictionMultiplier) {
        exponentialDecay<Float>(
            frictionMultiplier = frictionMultiplier,
            absVelocityThreshold = 0.1f
        )
    }

    return remember(splineDecay, expDecay) {
        SmoothFlingBehavior(splineDecay, expDecay)
    }
}

class SmoothFlingBehavior(
    private val splineDecay: DecayAnimationSpec<Float>,
    private val expDecay: DecayAnimationSpec<Float>
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        // For very high-speed flings, blend with spline decay for natural physics limits.
        // For standard swipes, use tuned exponential decay for a velvety, continuous glide.
        val decaySpec = if (abs(initialVelocity) > 6500f) splineDecay else expDecay
        var lastValue = 0f
        var velocityLeft = initialVelocity

        AnimationState(
            initialValue = 0f,
            initialVelocity = initialVelocity
        ).animateDecay(decaySpec) {
            val delta = value - lastValue
            val consumed = scrollBy(delta)
            lastValue = value
            velocityLeft = velocity
            // If the scroll container couldn't consume the delta (hit bounds), gracefully stop
            if (abs(delta - consumed) > 0.5f) {
                cancelAnimation()
            }
        }
        return velocityLeft
    }
}
