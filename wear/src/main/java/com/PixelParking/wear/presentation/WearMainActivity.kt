package com.PixelParking.wear.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.InputDeviceCompat
import androidx.core.view.MotionEventCompat
import androidx.core.view.ViewConfigurationCompat
import com.PixelParking.wear.sensor.WearSensorManager
import kotlinx.coroutines.flow.MutableSharedFlow

class WearMainActivity : ComponentActivity() {

    private lateinit var sensorManager: WearSensorManager
    val rotaryScrollFlow = MutableSharedFlow<Float>(extraBufferCapacity = 64)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        sensorManager.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = WearSensorManager(this)

        checkAndRequestPermissions()

        setContent {
            WearWaypointScreen(
                sensorManager = sensorManager,
                rotaryScrollEvents = rotaryScrollFlow
            )
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL &&
            MotionEventCompat.isFromSource(event, InputDeviceCompat.SOURCE_ROTARY_ENCODER)
        ) {
            // Retrieve rotary axis scroll distance (negative is counter-clockwise, positive is clockwise)
            val delta = -event.getAxisValue(MotionEventCompat.AXIS_SCROLL) *
                    ViewConfigurationCompat.getScaledVerticalScrollFactor(
                        android.view.ViewConfiguration.get(this), this
                    )
            rotaryScrollFlow.tryEmit(delta)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    private fun checkAndRequestPermissions() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            sensorManager.start()
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.start()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.stop()
    }
}
