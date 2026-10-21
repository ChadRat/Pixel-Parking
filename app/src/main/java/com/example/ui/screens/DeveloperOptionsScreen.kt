package com.example.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CustomFarAwayIcon
import com.example.ui.components.DevTimerStyleButton
import com.example.ui.components.SquigglyLoadingScreen
import com.example.ui.components.WavyDivider
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.ParkingViewModel
import kotlin.math.roundToInt

@Composable
fun DeveloperOptionsScreen(
    viewModel: ParkingViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isBtProximity by viewModel.isBtProximityEnabled.collectAsStateWithLifecycle()
    val isBtDisconnectTestEnabled by viewModel.isBtDisconnectTestEnabled.collectAsStateWithLifecycle()
    val isCapyCarsEnabled by viewModel.isDevCapyCarsEnabled.collectAsStateWithLifecycle()
    val isGeofenceAfterBtEnabled by viewModel.isDevGeofenceAfterBtEnabled.collectAsStateWithLifecycle()
    val isCustomCoordsEnabled by viewModel.isCustomFarAwayCoordsEnabled.collectAsStateWithLifecycle()
    val customCoordsInput by viewModel.customFarAwayCoordsInput.collectAsStateWithLifecycle()
    val isSimpleScrollEnabled by viewModel.isDevSimpleScrollEnabled.collectAsStateWithLifecycle()
    val isDevFunWalkAwayIcon by viewModel.isDevFunWalkAwayIcon.collectAsStateWithLifecycle()
    val textWeight by viewModel.devTextWeight.collectAsStateWithLifecycle()
    val whatsNewMessage by viewModel.whatsNewMessage.collectAsStateWithLifecycle()
    val devSimulatedProgress by viewModel.devSimulatedProgress.collectAsStateWithLifecycle()
    var messageInput by remember(whatsNewMessage) { mutableStateOf<String>(whatsNewMessage) }
    var showSquigglyLoadingPreview by remember { mutableStateOf(false) }

    if (showSquigglyLoadingPreview) {
        SquigglyLoadingScreen(
            title = "Testing Squiggly Loading Screen",
            subtitle = "Acquiring precision GPS telemetry and verifying car Bluetooth status",
            onDismissRequest = { showSquigglyLoadingPreview = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("developer_options_screen")
    ) {
        // Simple, clean header matching app navigation standard
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("dev_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "Developer options",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        WavyDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            waveLength = 34f,
            amplitude = 7f,
            thickness = 4.5f
        )

        val smoothFling = com.example.util.rememberSmoothFlingBehavior()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            flingBehavior = smoothFling
        ) {
            // Prominent Top Quick Actions Banner for What's New Window Trigger (Styled with Timer Style Button)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("card_dev_top_whats_new_trigger")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "What's New Window Test",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Preview the dynamic window with blur (\"${whatsNewMessage.trim()}\")",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Button in developer options styled with the requested Timer Button pill + spinning scallop icon style
                        DevTimerStyleButton(
                            text = "Trigger What's New Window",
                            icon = Icons.Default.AutoAwesome,
                            onClick = {
                                viewModel.showWhatsNewWindow(force = true)
                            },
                            testTag = "button_dev_top_trigger_whats_new"
                        )
                    }
                }
            }

            item {
                WavyDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    waveLength = 34f,
                    amplitude = 6f,
                    thickness = 4f,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Interactive Slider to control text weight dynamically across the app
            item {
                DeveloperOptionTextWeightSlider(
                    currentWeight = textWeight,
                    onWeightChange = { viewModel.setDevTextWeight(it) },
                    onReset = { viewModel.setDevTextWeight(400) }
                )
            }

            item {
                WavyDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    waveLength = 34f,
                    amplitude = 6f,
                    thickness = 4f,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Interactive Distance Slider to preview waypoint fill percentage (Phone & Wear OS)
            item {
                DeveloperOptionDistanceFillSlider(
                    currentProgress = devSimulatedProgress,
                    onProgressChange = { viewModel.setDevSimulatedProgress(it) },
                    onReset = { viewModel.resetDevSimulatedProgress() }
                )
            }

            item {
                WavyDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    waveLength = 34f,
                    amplitude = 6f,
                    thickness = 4f,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Section Header
            item {
                Text(
                    text = "Preferences & Features",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            item {
                DeveloperOptionSimpleRow(
                    title = "Bluetooth proximity",
                    subtitle = "Show signal scanner on paired devices",
                    icon = Icons.Default.BluetoothSearching,
                    checked = isBtProximity,
                    onCheckedChange = { viewModel.setBtProximityEnabled(it) },
                    testTag = "toggle_dev_bt_proximity"
                )
            }

            item {
                DeveloperOptionSimpleRow(
                    title = "Test BT disconnect",
                    subtitle = "Show small pill test disconnect button on each vehicle in Car BT menu",
                    icon = Icons.Default.Bluetooth,
                    checked = isBtDisconnectTestEnabled,
                    onCheckedChange = { viewModel.setBtDisconnectTestEnabled(it) },
                    testTag = "toggle_dev_bt_disconnect_test"
                )
            }

            item {
                DeveloperOptionSimpleRow(
                    title = "Remove smooth scrolling",
                    subtitle = "Remove custom inertia glide, keeping default simple and smooth scrolling without trying too hard",
                    icon = Icons.Default.SwapVert,
                    checked = isSimpleScrollEnabled,
                    onCheckedChange = { viewModel.setDevSimpleScrollEnabled(it) },
                    testTag = "toggle_dev_remove_smooth_scroll"
                )
            }

            item {
                DeveloperOptionSimpleRow(
                    title = "Capy cars",
                    subtitle = "Enable capibara Cars as a third car style option",
                    icon = Icons.Default.Pets,
                    checked = isCapyCarsEnabled,
                    onCheckedChange = { viewModel.setDevCapyCarsEnabled(it) },
                    testTag = "toggle_dev_capy_cars"
                )
            }

            item {
                DeveloperOptionSimpleRow(
                    title = "Geofence with Bluetooth save",
                    subtitle = "Allow geofence feature to work even if location was automatically saved by Bluetooth",
                    icon = Icons.Default.LocationOn,
                    checked = isGeofenceAfterBtEnabled,
                    onCheckedChange = { viewModel.setDevGeofenceAfterBtEnabled(it) },
                    testTag = "toggle_dev_geofence_after_bt"
                )
            }

            item {
                DeveloperOptionSimpleRow(
                    title = "Fun walk away icon",
                    subtitle = "Use the illustrated car and walking person icon for the Walk Away feature",
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    checked = isDevFunWalkAwayIcon,
                    onCheckedChange = { viewModel.setDevFunWalkAwayIcon(it) },
                    testTag = "toggle_dev_fun_walk_away_icon"
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("dev_custom_coords_container")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setDevCustomFarAwayCoordsEnabled(!isCustomCoordsEnabled) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isCustomCoordsEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CustomFarAwayIcon(
                                        modifier = Modifier.size(24.dp),
                                        tint = if (isCustomCoordsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Custom far away parking location",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Paste custom coordinates to simulate a distant parking spot and test long-distance navigation",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Switch(
                            checked = isCustomCoordsEnabled,
                            onCheckedChange = { viewModel.setDevCustomFarAwayCoordsEnabled(it) },
                            modifier = Modifier.testTag("toggle_dev_custom_far_away_coords")
                        )
                    }

                    AnimatedVisibility(
                        visible = isCustomCoordsEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = customCoordsInput,
                                onValueChange = { viewModel.setCustomFarAwayCoordsInput(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_dev_custom_coords"),
                                placeholder = {
                                    Text(
                                        text = "e.g. 48.8584, 2.2945 or Google Maps link",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                leadingIcon = {
                                    CustomFarAwayIcon(
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    if (customCoordsInput.isNotBlank()) {
                                        IconButton(
                                            onClick = { viewModel.setCustomFarAwayCoordsInput("") },
                                            modifier = Modifier.testTag("btn_dev_clear_custom_coords")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear input",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        IconButton(
                                            onClick = {
                                                val clip = clipboardManager.getText()?.text
                                                if (!clip.isNullOrBlank()) {
                                                    viewModel.setCustomFarAwayCoordsInput(clip)
                                                }
                                            },
                                            modifier = Modifier.testTag("btn_dev_paste_custom_coords")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = "Paste from clipboard",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f)
                                ),
                                maxLines = 2
                            )

                            Text(
                                text = "Quick far away presets:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val presets = remember {
                                listOf(
                                    "Paris" to "48.8584, 2.2945",
                                    "New York" to "40.7128, -74.0060",
                                    "Tokyo" to "35.6762, 139.6503",
                                    "Sydney" to "-33.8688, 151.2093"
                                )
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(presets) { (city, coords) ->
                                    AssistChip(
                                        onClick = { viewModel.setCustomFarAwayCoordsInput(coords) },
                                        label = { Text(city, fontSize = 12.sp) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                        ),
                                        border = AssistChipDefaults.assistChipBorder(
                                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            enabled = true
                                        )
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DevTimerStyleButton(
                                    text = "Set Location",
                                    icon = Icons.Default.Place,
                                    onClick = {
                                        viewModel.applyCustomFarAwayCoordinates(
                                            coordsText = customCoordsInput,
                                            startNavigation = false,
                                            onResult = { success, msg ->
                                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    pillColor = MaterialTheme.colorScheme.secondaryContainer,
                                    pillTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    isFullWidth = false,
                                    modifier = Modifier.weight(1f),
                                    testTag = "button_dev_apply_custom_coords"
                                )

                                DevTimerStyleButton(
                                    text = "Set & Navigate",
                                    icon = Icons.Default.NearMe,
                                    onClick = {
                                        viewModel.applyCustomFarAwayCoordinates(
                                            coordsText = customCoordsInput,
                                            startNavigation = true,
                                            onResult = { success, msg ->
                                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    pillColor = MaterialTheme.colorScheme.primary,
                                    pillTextColor = MaterialTheme.colorScheme.onPrimary,
                                    isFullWidth = false,
                                    modifier = Modifier.weight(1.2f),
                                    testTag = "button_dev_apply_and_navigate_custom_coords"
                                )
                            }
                        }
                    }
                }
            }

            item {
                WavyDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    waveLength = 34f,
                    amplitude = 6f,
                    thickness = 4f,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Feature Testing & Simulation Actions styled with the requested Timer Style Buttons
            item {
                Text(
                    text = "Feature Testing & Simulation",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // Preview Squiggly Loading Screen Button (Direct test for the user's requested squiggly line loading animation)
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    DevTimerStyleButton(
                        text = "Preview Squiggly Loading Screen",
                        icon = Icons.Default.Waves,
                        onClick = { showSquigglyLoadingPreview = true },
                        testTag = "action_dev_preview_squiggly_loading"
                    )
                }
            }

            // Preview Background Auto-Save Setup Pop-up (with screen blur)
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    DevTimerStyleButton(
                        text = "Preview Auto-Save Setup Pop-up",
                        icon = Icons.Default.Shield,
                        onClick = { viewModel.showBackgroundAutoSaveSetupSheet() },
                        testTag = "action_dev_preview_auto_save_popup"
                    )
                }
            }

            // Test Geofence Auto-Park
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    DevTimerStyleButton(
                        text = "Test Geofence auto-park",
                        icon = Icons.Default.LocationOn,
                        onClick = {
                            viewModel.simulateGeofenceExit()
                            android.widget.Toast.makeText(
                                context,
                                "Simulated walking departure from car (Geofence exit triggered)",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        testTag = "action_dev_test_geofence_autopark"
                    )
                }
            }

            // Test Bluetooth Auto-Park
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    DevTimerStyleButton(
                        text = "Test Bluetooth auto-park",
                        icon = Icons.Default.Bluetooth,
                        onClick = {
                            viewModel.testBluetoothParkingAutoSave()
                            android.widget.Toast.makeText(
                                context,
                                "Simulated Bluetooth disconnect (Car parking location saved)",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        testTag = "action_dev_test_bt_autopark"
                    )
                }
            }

            item {
                WavyDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    waveLength = 34f,
                    amplitude = 6f,
                    thickness = 4f,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // What's New Controls Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("card_dev_whats_new_controls")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "What's New Developer Controls",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "The window dynamically covers only as much space as needed by the text. As soon as the developer changes its message and an update download occurs, it automatically appears.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Dedicated Trigger Button with requested Timer Button style
                        DevTimerStyleButton(
                            text = "Trigger What's new window",
                            icon = Icons.Default.AutoAwesome,
                            onClick = {
                                viewModel.showWhatsNewWindow(force = true)
                            },
                            testTag = "button_dev_trigger_whats_new"
                        )

                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = {
                                messageInput = it
                                viewModel.setDeveloperWhatsNewMessage(it)
                            },
                            label = { Text("Message") },
                            placeholder = { Text("hello world ( ꈍᴗꈍ) ") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("textfield_dev_whats_new_message"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            maxLines = 4
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DevTimerStyleButton(
                                text = "Reset Default",
                                icon = Icons.Default.Refresh,
                                onClick = {
                                    viewModel.resetDeveloperWhatsNewMessage()
                                    messageInput = com.example.util.WhatsNewManager.CURRENT_MESSAGE
                                    android.widget.Toast.makeText(
                                        context,
                                        "Reset message to default: \"hello world ( ꈍᴗꈍ) \"",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                },
                                isFullWidth = false,
                                modifier = Modifier.weight(1f),
                                testTag = "button_dev_reset_whats_new"
                            )

                            DevTimerStyleButton(
                                text = "Simulate Download",
                                icon = Icons.Default.Grain,
                                onClick = {
                                    com.example.util.WhatsNewManager.onUpdateDownloadStarted(context)
                                    android.widget.Toast.makeText(
                                        context,
                                        "Simulated update download (Checking What's New message trigger)",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                },
                                isFullWidth = false,
                                modifier = Modifier.weight(1f),
                                testTag = "button_dev_simulate_update_download"
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Interactive slider in Developer Options allowing fine-grained control
 * of the text weight across the entire app, matching Settings squircle icon style.
 */
@Composable
fun DeveloperOptionTextWeightSlider(
    currentWeight: Int,
    onWeightChange: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember(currentWeight) { mutableFloatStateOf(currentWeight.toFloat()) }
    val displayWeight = sliderPosition.roundToInt().coerceIn(100, 900)
    val weightLabel = remember(displayWeight) {
        getFontWeightLabel(displayWeight)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("dev_text_weight_container")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FormatBold,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "System text weight",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = weightLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (currentWeight != 400) {
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.testTag("btn_reset_text_weight")
                ) {
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
                onWeightChange(it.roundToInt().coerceIn(100, 900))
            },
            valueRange = 100f..900f,
            steps = 7,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("slider_text_weight")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Thin (100)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Default (400)",
                style = MaterialTheme.typography.labelSmall,
                color = if (displayWeight in 350..450) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Black (900)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getFontWeightLabel(weight: Int): String {
    return when {
        weight <= 150 -> "Thin (100)"
        weight <= 250 -> "Extra Light (200)"
        weight <= 350 -> "Light (300)"
        weight <= 450 -> "Regular (400) • System Default"
        weight <= 550 -> "Medium (500)"
        weight <= 650 -> "Semi-Bold (600)"
        weight <= 750 -> "Bold (700)"
        weight <= 850 -> "Extra Bold (800)"
        else -> "Black (900)"
    }
}

/**
 * Interactive distance slider in Developer Options allowing live preview of
 * waypoint needle fill percentage (0% to 100%) on both Phone and Wear OS.
 */
@Composable
fun DeveloperOptionDistanceFillSlider(
    currentProgress: Float?,
    onProgressChange: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSimulating = currentProgress != null
    var sliderPosition by remember(currentProgress) {
        mutableFloatStateOf(currentProgress ?: 0.5f)
    }
    val percentageInt = (sliderPosition * 100f).roundToInt().coerceIn(0, 100)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("dev_distance_fill_container")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSimulating) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = if (isSimulating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "Waypoint fill preview slider",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isSimulating) "Simulated fill: $percentageInt% (Phone & Wear OS)" else "Real GPS distance • Inactive",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSimulating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSimulating) {
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.testTag("btn_reset_distance_fill")
                ) {
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
                onProgressChange(it)
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("slider_distance_fill")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "0% (Far away)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "50%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "100% (Arrived)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Developer Option Row with Settings-styled squircle icon badge and Material 3 Switch.
 */
@Composable
fun DeveloperOptionSimpleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun DeveloperOptionToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    DeveloperOptionSimpleRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        checked = checked,
        onCheckedChange = onCheckedChange,
        testTag = testTag
    )
}

@Composable
fun DeveloperOptionActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
