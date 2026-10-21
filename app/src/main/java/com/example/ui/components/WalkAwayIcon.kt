package com.example.ui.components

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Standard Material 3 icon for the "Walk Away" auto-parking feature.
 * Automatically stays reactive to the "Fun walk away icon" toggle in Developer Options.
 *
 * Supports switching between:
 * - Default: The solid filled DirectionsWalk symbol matching standard settings icons.
 * - Fun: The custom illustrated car, departure trail, and walking stick person icon enabled via Developer Options.
 */
@Composable
fun WalkAwayIcon(
    modifier: Modifier = Modifier.size(28.dp),
    tint: Color = MaterialTheme.colorScheme.primary,
    isFun: Boolean? = null
) {
    val context = LocalContext.current
    val effectiveFun = if (isFun != null) {
        isFun
    } else {
        val prefs = remember(context) {
            context.getSharedPreferences("pixel_parking_prefs", Context.MODE_PRIVATE)
        }
        var funState by remember {
            mutableStateOf(prefs.getBoolean("dev_fun_walk_away_icon", false))
        }
        DisposableEffect(prefs) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == "dev_fun_walk_away_icon") {
                    funState = prefs.getBoolean("dev_fun_walk_away_icon", false)
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            onDispose {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }
        funState
    }

    if (effectiveFun) {
        FunWalkAwayCanvasIcon(
            modifier = modifier,
            tint = tint
        )
    } else {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
            contentDescription = "Walk Away",
            tint = tint,
            modifier = modifier
        )
    }
}

/**
 * The custom illustrated "Fun walk away icon" depicting the stylized car,
 * departure path dots, and walking stick figure person.
 */
@Composable
fun FunWalkAwayCanvasIcon(
    modifier: Modifier = Modifier.size(28.dp),
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)

        val strokeThick = (minDim * 0.058f).coerceIn(2.0f, 4.2f)
        val strokeMed = (minDim * 0.052f).coerceIn(1.8f, 3.8f)

        // 1. Car silhouette outline (left side)
        val carPath = Path().apply {
            // Rear bumper lip
            moveTo(w * 0.165f, h * 0.690f)
            lineTo(w * 0.185f, h * 0.690f)
            // Vertical rear
            lineTo(w * 0.185f, h * 0.585f)
            // Rear flat deck / trunk
            lineTo(w * 0.245f, h * 0.585f)
            // Slanted windshield
            lineTo(w * 0.295f, h * 0.470f)
            // Flat roof
            lineTo(w * 0.380f, h * 0.470f)
            // Slanted front hood
            lineTo(w * 0.455f, h * 0.695f)
        }
        drawPath(
            path = carPath,
            color = tint,
            style = Stroke(width = strokeMed, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Car wheels
        drawCircle(
            color = tint,
            radius = minDim * 0.035f,
            center = Offset(w * 0.235f, h * 0.705f)
        )
        drawCircle(
            color = tint,
            radius = minDim * 0.035f,
            center = Offset(w * 0.390f, h * 0.705f)
        )

        // 2. Departure path dots between car & person
        drawCircle(
            color = tint,
            radius = minDim * 0.018f,
            center = Offset(w * 0.482f, h * 0.625f)
        )
        drawCircle(
            color = tint,
            radius = minDim * 0.024f,
            center = Offset(w * 0.538f, h * 0.558f)
        )

        // 3. Walking stick figure person (right side)
        // Head
        drawCircle(
            color = tint,
            radius = minDim * 0.072f,
            center = Offset(w * 0.685f, h * 0.280f)
        )

        // Neck and vertical torso down to hips
        val torsoPath = Path().apply {
            moveTo(w * 0.675f, h * 0.355f)
            lineTo(w * 0.660f, h * 0.540f)
        }
        drawPath(
            path = torsoPath,
            color = tint,
            style = Stroke(width = strokeMed, cap = StrokeCap.Round)
        )

        // Arms (branching from mid-torso)
        val leftArmPath = Path().apply {
            moveTo(w * 0.665f, h * 0.450f)
            lineTo(w * 0.605f, h * 0.485f)
        }
        drawPath(
            path = leftArmPath,
            color = tint,
            style = Stroke(width = strokeMed, cap = StrokeCap.Round)
        )

        val rightArmPath = Path().apply {
            moveTo(w * 0.665f, h * 0.450f)
            lineTo(w * 0.755f, h * 0.495f)
        }
        drawPath(
            path = rightArmPath,
            color = tint,
            style = Stroke(width = strokeMed, cap = StrokeCap.Round)
        )

        // Left leg (bent back at knee)
        val leftLegPath = Path().apply {
            moveTo(w * 0.660f, h * 0.540f)
            lineTo(w * 0.615f, h * 0.640f)
            lineTo(w * 0.565f, h * 0.760f)
        }
        drawPath(
            path = leftLegPath,
            color = tint,
            style = Stroke(width = strokeThick, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Right leg (straight forward stride)
        val rightLegPath = Path().apply {
            moveTo(w * 0.660f, h * 0.540f)
            lineTo(w * 0.795f, h * 0.770f)
        }
        drawPath(
            path = rightLegPath,
            color = tint,
            style = Stroke(width = strokeThick, cap = StrokeCap.Round)
        )
    }
}
