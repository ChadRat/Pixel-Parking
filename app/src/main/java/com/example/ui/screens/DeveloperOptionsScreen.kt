package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val isBtProximity by viewModel.isBtProximityEnabled.collectAsStateWithLifecycle()
    val isCapyCarsEnabled by viewModel.isDevCapyCarsEnabled.collectAsStateWithLifecycle()
    val textWeight by viewModel.devTextWeight.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("developer_options_screen")
    ) {
        // Simple, clean header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
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
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            thickness = 0.5.dp
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            // Working Slider to control text weight dynamically across the app
            item {
                DeveloperOptionTextWeightSlider(
                    currentWeight = textWeight,
                    onWeightChange = { viewModel.setDevTextWeight(it) },
                    onReset = { viewModel.setDevTextWeight(400) }
                )
            }

            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
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
                    title = "Capy cars",
                    subtitle = "Enable capibara Cars as a third car style option",
                    icon = Icons.Default.Pets,
                    checked = isCapyCarsEnabled,
                    onCheckedChange = { viewModel.setDevCapyCarsEnabled(it) },
                    testTag = "toggle_dev_capy_cars"
                )
            }
        }
    }
}

/**
 * Interactive slider in Developer Options allowing fine-grained control
 * of the text weight across the entire app.
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
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("dev_text_weight_container")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.FormatBold,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Text(
                        text = "System text weight",
                        style = MaterialTheme.typography.titleMedium,
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

        // Working Slider
        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
                onWeightChange(it.roundToInt().coerceIn(100, 900))
            },
            valueRange = 100f..900f,
            steps = 7, // 100, 200, 300, 400, 500, 600, 700, 800, 900
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
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
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
