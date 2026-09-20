package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ParkingSpot
import com.example.ui.components.EditSpotDialog
import com.example.ui.components.ParkingMeterWidget
import com.example.ui.components.ParkingTimerButton
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.blur
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.PixelAnimatedCarBadge
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.PixelGreen
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.NavigationTelemetry
import com.example.ui.viewmodel.ParkingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ParkingViewModel,
    activeSpot: ParkingSpot?,
    recentSpots: List<ParkingSpot>,
    telemetry: NavigationTelemetry,
    onNavigateTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val currentLocation by viewModel.currentLocation.collectAsState()
    val currentAddress by viewModel.currentAddress.collectAsState()
    val isGpsRefreshing by viewModel.isGpsRefreshing.collectAsState()
    val carBadgeStyle by viewModel.carBadgeStyle.collectAsState()
    val capyVariant by viewModel.capyVariant.collectAsState()
    val isTimerRunning by viewModel.timerIsRunning.collectAsStateWithLifecycle()
    val timerRemainingSeconds by viewModel.timerRemainingSeconds.collectAsStateWithLifecycle()
    val isParkingTimerEnabled by viewModel.isParkingTimerFeatureEnabled.collectAsStateWithLifecycle()

    var showNewParkDialog by remember { mutableStateOf(false) }
    var tapTimestamps by remember { mutableStateOf(emptyList<Long>()) }

    val backgroundBlur by animateDpAsState(
        targetValue = if (showNewParkDialog) 20.dp else 0.dp,
        label = "home_blur"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .then(if (backgroundBlur > 0.5.dp) Modifier.blur(backgroundBlur) else Modifier)
            .padding(horizontal = 18.dp)
            .testTag("home_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header: Pixel Parking Title + Theme Mode Pill + BT Pill
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.appName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            val now = System.currentTimeMillis()
                            val targetTaps = 5
                            val validTaps = (tapTimestamps.filter { now - it <= 6000L } + now).takeLast(targetTaps)
                            tapTimestamps = validTaps

                            if (validTaps.size == 1) {
                                val message = if (strings.languageSection == "Γλώσσα") {
                                    "Επιλογές προγραμματιστή"
                                } else {
                                    "Developer options"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }

                            if (validTaps.size >= targetTaps) {
                                tapTimestamps = emptyList()
                                viewModel.unlockDeveloperMode()
                                onNavigateTab(AppTab.DEVELOPER_OPTIONS)
                            }
                        }
                        .testTag("pixel_parking_title")
                )

                // Car BT Quick Pill
                Surface(
                    onClick = { onNavigateTab(AppTab.BLUETOOTH_AUTO) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .testTag("car_bt_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Car BT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // MAIN PARKING STATE CARD
        item {
            EmptyParkingStateCard(
                activeSpot = activeSpot,
                currentLocation = currentLocation,
                currentAddress = currentAddress,
                isGpsRefreshing = isGpsRefreshing,
                carBadgeStyle = carBadgeStyle,
                capyVariant = capyVariant,
                isTimerRunning = isTimerRunning,
                timerRemainingSeconds = timerRemainingSeconds,
                isParkingTimerEnabled = isParkingTimerEnabled,
                onOpenTimer = { viewModel.openParkingTimer() },
                onManualPark = { showNewParkDialog = true },
                onFoundCar = { viewModel.markCarFound() },
                onRefreshGps = { viewModel.refreshCurrentLocation() }
            )
        }

        // RECENT SPOTS PREVIEW
        if (recentSpots.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.recentSpots,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${strings.viewAll} (${recentSpots.size})",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onNavigateTab(AppTab.HISTORY) }
                    )
                }
            }

            items(recentSpots.take(5), key = { it.id }) { spot ->
                RecentSpotItem(
                    spot = spot,
                    onClick = {
                        viewModel.setActiveNavigationTarget(spot)
                        onNavigateTab(AppTab.COMPASS_RADAR)
                    },
                    onDelete = {
                        viewModel.deleteSpot(spot.id)
                    }
                )
            }
        }
    }

    if (showNewParkDialog) {
        EditSpotDialog(
            initialSpot = null,
            currentLocation = currentLocation,
            currentAddress = currentAddress,
            onDismiss = { showNewParkDialog = false },
            onSave = { name, floor, note, customLat, customLng, customAddress ->
                viewModel.saveCurrentLocationAsParking(name, floor, note, customLat, customLng, customAddress)
            }
        )
    }
}

@Composable
fun EmptyParkingStateCard(
    activeSpot: ParkingSpot?,
    currentLocation: android.location.Location?,
    currentAddress: String,
    isGpsRefreshing: Boolean,
    carBadgeStyle: com.example.ui.components.CarBadgeStyle = com.example.ui.components.CarBadgeStyle.CINEMATIC,
    capyVariant: com.example.ui.components.CapyVariant = com.example.ui.components.CapyVariant.BABY,
    isTimerRunning: Boolean = false,
    timerRemainingSeconds: Long = 0L,
    isParkingTimerEnabled: Boolean = false,
    onOpenTimer: () -> Unit = {},
    onManualPark: () -> Unit,
    onFoundCar: () -> Unit,
    onRefreshGps: () -> Unit
) {
    val strings = LocalAppStrings.current
    val hasActiveSpot = activeSpot != null
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .testTag("empty_parking_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PixelAnimatedCarBadge(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 2.dp),
                size = 176.dp,
                style = carBadgeStyle,
                capyVariant = capyVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (hasActiveSpot) (activeSpot?.spotName?.ifBlank { strings.myParkedCar } ?: strings.myParkedCar) else strings.noActiveParking,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (hasActiveSpot) (if (activeSpot?.isAutoSaved == true) strings.autoSavedViaBt else strings.gpsLocationSaved) else strings.noActiveSpotSubtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            // Real-Time Live GPS Status Strip
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val displayLocation = if (hasActiveSpot && !activeSpot?.address.isNullOrBlank() && activeSpot?.address != "Fetching address...") {
                        activeSpot.address
                    } else if (currentAddress.isNotBlank()) {
                        currentAddress
                    } else if (currentLocation != null) {
                        String.format(Locale.US, "GPS: %.5f, %.5f", currentLocation.latitude, currentLocation.longitude)
                    } else {
                        "Acquiring live satellite coordinates..."
                    }
                    Text(
                        text = displayLocation,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    IconButton(
                        onClick = onRefreshGps,
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (isGpsRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh GPS", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Blue Parking / Charging Timer Button when navigation has started
            if (hasActiveSpot && isParkingTimerEnabled) {
                ParkingTimerButton(
                    isRunning = isTimerRunning,
                    remainingSeconds = timerRemainingSeconds,
                    onClick = onOpenTimer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )
            }

            // Action Button: Park Here Now / Found Car
            if (hasActiveSpot) {
                Button(
                    onClick = onFoundCar,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("found_car_home_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.foundCar,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            } else {
                Button(
                    onClick = onManualPark,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("manual_park_now_btn"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.parkHereNow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun RecentSpotItem(
    spot: ParkingSpot,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val deleteRed = Color(0xFF92312E)
    val formattedTime = remember(spot.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(spot.timestamp))
    }
    val displayAddress = remember(spot.address, spot.spotName, spot.latitude, spot.longitude) {
        if (spot.address.isNotBlank() && spot.address != "Fetching address..." && spot.address != "Address not found") {
            spot.address
        } else if (spot.spotName.isNotBlank() && spot.spotName != "My Parked Car") {
            spot.spotName
        } else {
            String.format(Locale.US, "GPS: %.4f, %.4f", spot.latitude, spot.longitude)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_spot_item_${spot.id}"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main Spot Capsule
        Surface(
            onClick = { onClick() },
            modifier = Modifier
                .weight(1f)
                .height(68.dp)
                .clip(RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular icon container (darker circular inset)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = displayAddress,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedTime,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Separate Red Delete Pill (compact)
        Surface(
            onClick = { onDelete() },
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .testTag("delete_spot_${spot.id}"),
            shape = RoundedCornerShape(16.dp),
            color = deleteRed
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Spot",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
