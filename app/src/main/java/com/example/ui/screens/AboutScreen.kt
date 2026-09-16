package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.ParkingViewModel

@Composable
fun AboutScreen(
    viewModel: ParkingViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var showLicenseDetail by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black) // Force OLED Black as shown in the screenshot
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (!showLicenseDetail) {
            // About Screen Main View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (Back Arrow + Title + Subtitle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF1C1C1E), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = strings.aboutTitle,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // App Icon (DirectionsCar symbol in a squircle)
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color(0xFF382F13), RoundedCornerShape(28.dp)), // Dark brownish gold squircle
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "App Icon",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Name & Version
                Text(
                    text = strings.appName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Main options Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)), // High contrast dark grey card
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val uriHandler = LocalUriHandler.current

                        // 1. Source Code Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        uriHandler.openUri("https://github.com/ChadRat/Pixel-Parking")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left circle icon containing "Code"
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF1A365D), CircleShape), // Dark blue circular background
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Code",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.sourceCodeGitHub,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = strings.sourceCodeSubtitle,
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Squiggly/Wavy Divider (Big & Thick)
                        WavyDivider(
                            color = Color(0xFF2C2C2E),
                            waveLength = 36f,
                            amplitude = 9f,
                            thickness = 6f
                        )

                        // 2. Report an Issue Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        uriHandler.openUri("https://github.com/ChadRat/Pixel-Parking/issues")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left circle with Bug icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF2A3439), CircleShape), // Slate-grey/blue circular background
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.reportAnIssueTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = strings.reportAnIssueUrl,
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Squiggly/Wavy Divider (Big & Thick)
                        WavyDivider(
                            color = Color(0xFF2C2C2E),
                            waveLength = 36f,
                            amplitude = 9f,
                            thickness = 6f
                        )

                        // 3. License Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showLicenseDetail = true
                                }
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left circle with Scales of justice icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF233E3B), CircleShape), // Dark teal circular background
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Gavel,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.licenseTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = strings.licenseSubtitle,
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // License Detail full-screen view (exactly as depicted in the second screenshot, WITHOUT the bottom close button)
            val clipboardManager = LocalClipboardManager.current
            val uriHandler = LocalUriHandler.current

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (Title + Subtitle + Close 'X' button on the right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.licenseTitle,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "GNU General Public License v3.0",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(
                        onClick = { showLicenseDetail = false },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF1C1C1E), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons Row (Copy + GNU.org)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Copy Button
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(gplv3LicenseText))
                            Toast.makeText(context, "License copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1F2937),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = strings.copyButton, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // GNU.org Button
                    OutlinedButton(
                        onClick = {
                            try {
                                uriHandler.openUri("https://www.gnu.org/licenses/gpl-3.0.html")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = strings.gnuOrgButton, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Scrollable License Text Card (taking up all remaining space)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "GNU GENERAL PUBLIC LICENSE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Version 3, 29 June 2007",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        Text(
                            text = gplv3LicenseText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WavyDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.Gray.copy(alpha = 0.5f),
    waveLength: Float = 36f,
    amplitude: Float = 9f,
    thickness: Float = 6f
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
    ) {
        val width = size.width
        val midY = size.height / 2f
        val path = Path().apply {
            moveTo(0f, midY)
            var x = 0f
            while (x < width) {
                val nextX = x + waveLength / 2f
                val ctrlX = x + waveLength / 4f
                val targetY = if ((x / (waveLength / 2f)).toInt() % 2 == 0) midY + amplitude else midY - amplitude
                quadraticTo(ctrlX, targetY, nextX, midY)
                x = nextX
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = thickness, cap = StrokeCap.Round)
        )
    }
}
