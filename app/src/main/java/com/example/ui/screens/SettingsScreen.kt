package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.AppSettings
import com.example.ui.viewmodel.DigitAnalysisViewModel

@Composable
fun SettingsScreen(
    viewModel: DigitAnalysisViewModel,
    modifier: Modifier = Modifier
) {
    val userSettings by viewModel.userSettings.collectAsState()
    val triggeredToday by viewModel.triggeredSignalsToday.collectAsState()
    val scrollState = rememberScrollState()

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            Color(0xFF030305)
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- SCREEN HEADER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    text = "⚙️ ALGORITHMIC CONTROL",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "PRESET CONFIGURATOR",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Dynamic Room and SQLite real-time parameter tuner",
                    color = Color.LightGray.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // --- THEME CUSTOMIZER CUSTOM CHIPS ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SELECT APPLICATION VISUAL IDENTITY",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        // Light vs Dark Mode toggle!
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (userSettings.isDarkMode) "🌙 DARK" else "☀️ LIGHT",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Switch(
                                checked = userSettings.isDarkMode,
                                onCheckedChange = {
                                    viewModel.updateSettingsInDb(userSettings.copy(isDarkMode = it))
                                },
                                modifier = Modifier.scale(0.8f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }

                    val themes = listOf("Google Indigo", "Google Emerald", "Google Cosmic", "Pixel Sunrise")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        themes.forEach { theme ->
                            val isSelected = theme == userSettings.themeName
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        else Color.White.copy(alpha = 0.03f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.updateSettingsInDb(userSettings.copy(themeName = theme))
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = theme.substringAfter(" "),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // --- RISK PROFILE TUNER ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "SELECT ALGORITHMIC RISK CATEGORY",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("RISKY" to "⚡ SNIPER (RISKY)", "LESS_RISKY" to "🟢 SAFETY NET (LESS)").forEach { (profileKey, profileName) ->
                            val isSelected = userSettings.riskProfile == profileKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) {
                                            if (profileKey == "RISKY") Color(0xFFEF4444).copy(alpha = 0.15f)
                                            else Color(0xFF10B981).copy(alpha = 0.15f)
                                        } else Color.White.copy(alpha = 0.03f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) {
                                            if (profileKey == "RISKY") Color(0xFFEF4444) else Color(0xFF10B981)
                                        } else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.updateSettingsInDb(userSettings.copy(riskProfile = profileKey))
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profileName,
                                    color = if (isSelected) {
                                        if (profileKey == "RISKY") Color(0xFFFCA5A5) else Color(0xFF6EE7B7)
                                    } else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Text(
                        text = if (userSettings.riskProfile == "RISKY") 
                            "Sniper profile isolates extreme boundaries (e.g. UNDER 1 or OVER 8) to target maximum risk multipliers (~900%+ payout)." 
                            else "Safety Net profile expands contract boundaries (e.g. UNDER 4 or OVER 5) to maximize statistical padding.",
                        color = Color.LightGray.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // --- STRENGTH & DROUGHT THRESHOLDS ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Title
                    Text(
                        text = "SCANNER VECTOR TUNING PARAMS",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Slider 1: Signal strength standard
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Minimum Signal Confidence", color = Color.White, fontSize = 12.sp)
                            Text(
                                text = "${userSettings.signalStrengthMinimum.toInt()}%",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = userSettings.signalStrengthMinimum,
                            onValueChange = {
                                viewModel.updateSettingsInDb(userSettings.copy(signalStrengthMinimum = it))
                            },
                            valueRange = 50f..95f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // Slider 2: Anomaly / Drought trigger factor
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Drought Anomaly Limit", color = Color.White, fontSize = 12.sp)
                            Text(
                                text = "${String.format("%.1f", userSettings.droughtThreshold)}%",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = userSettings.droughtThreshold,
                            onValueChange = {
                                viewModel.updateSettingsInDb(userSettings.copy(droughtThreshold = it))
                            },
                            valueRange = 5f..15f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // --- HISTORICAL SAMPLE LOOKBACK PERIODS ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "RANKING NOISE & LOOKBACK CONTROL",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Noise Reduction Intensity
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Noise Reduction (EMA Smoothing)", color = Color.White, fontSize = 12.sp)
                                Text("Higher reduces ranking shifts / volatility", color = Color.Gray, fontSize = 9.sp)
                            }
                            Text(
                                text = "${(userSettings.smoothingIntensity * 100).toInt()}%",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = userSettings.smoothingIntensity,
                            onValueChange = {
                                viewModel.updateSettingsInDb(userSettings.copy(smoothingIntensity = it))
                            },
                            valueRange = 0f..0.9f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // Macro sample count to compare
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Ticks to Compare", color = Color.White, fontSize = 12.sp)
                                Text("Statistical lookback buffer (Max: 1000)", color = Color.Gray, fontSize = 9.sp)
                            }
                            Text(
                                text = "${userSettings.ticksToCompare} ticks",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = userSettings.ticksToCompare.toFloat(),
                            onValueChange = {
                                viewModel.updateSettingsInDb(userSettings.copy(ticksToCompare = it.toInt()))
                            },
                            valueRange = 10f..1000f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // Micro sample
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Micro lookback", color = Color.White, fontSize = 12.sp)
                                Text("Immediate momentum velocity window", color = Color.Gray, fontSize = 9.sp)
                            }
                            Text(
                                text = "${userSettings.microLookback} ticks",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = userSettings.microLookback.toFloat(),
                            onValueChange = {
                                viewModel.updateSettingsInDb(userSettings.copy(microLookback = it.toInt()))
                            },
                            valueRange = 3f..20f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // --- ALARMS & REMINDERS SWITCHES ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "SIGNAL NOTIFICATION EMITTER",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Trade Reminder Alarms", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Push recurrent alerts if top market edge goes above standard minimum limit",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = userSettings.alarmsEnabled,
                            onCheckedChange = {
                                viewModel.updateSettingsInDb(userSettings.copy(alarmsEnabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        )
                    }

                    if (userSettings.alarmsEnabled) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Maximum Daily Signals Limit", color = Color.White, fontSize = 12.sp)
                                Text(
                                    text = "${userSettings.signalsPerDayLimit} / day",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Slider(
                                value = userSettings.signalsPerDayLimit.toFloat(),
                                onValueChange = {
                                    viewModel.updateSettingsInDb(userSettings.copy(signalsPerDayLimit = it.toInt()))
                                },
                                valueRange = 10f..200f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Alarm Interval Rate", color = Color.White, fontSize = 12.sp)
                                Text(
                                    text = "${userSettings.alarmIntervalMinutes} mins",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Slider(
                                value = userSettings.alarmIntervalMinutes.toFloat(),
                                onValueChange = {
                                    viewModel.updateSettingsInDb(userSettings.copy(alarmIntervalMinutes = it.toInt()))
                                },
                                valueRange = 5f..120f,
                                steps = 22,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // --- CUSTOM SIGS & RADAR TRIGGERS PANEL ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "CUSTOM CONTRACT & TRIGGER TEMPLATES",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // 1. Core Target Contract Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Target Contract Strategy",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select 'ALL' to let the automatic algorithm decide, or pin to a specific contract type from the broker master patterns.",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )

                        val currentParts = userSettings.customContract.split(" ")
                        val selectedCategory = currentParts.getOrNull(0) ?: "ALL"
                        val selectedBarrierVal = currentParts.getOrNull(1) ?: "3"

                        // Horizontal Scroll of Categories
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val categories = listOf("ALL", "OVER", "UNDER", "DIFFERS", "MATCHES")
                            categories.forEach { cat ->
                                val isSelected = selectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f))
                                        .clickable {
                                            val nextContractVal = if (cat == "ALL") {
                                                "ALL"
                                            } else {
                                                "$cat $selectedBarrierVal"
                                            }
                                            viewModel.updateSettingsInDb(userSettings.copy(customContract = nextContractVal))
                                        }
                                        .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // If NOT ALL, show digit barrier selection
                        if (selectedCategory != "ALL") {
                            Text(
                                text = "Select Digit Barrier: $selectedBarrierVal",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            val validDigits = when (selectedCategory) {
                                "OVER" -> listOf("0", "1", "2", "3", "4", "5", "6", "7", "8")
                                "UNDER" -> listOf("1", "2", "3", "4", "5", "6", "7", "8", "9")
                                else -> listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                validDigits.forEach { digit ->
                                    val isSelected = selectedBarrierVal == digit
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.05f))
                                            .clickable {
                                                viewModel.updateSettingsInDb(userSettings.copy(customContract = "$selectedCategory $digit"))
                                            }
                                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = digit,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // 2. Custom Trigger Categories
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Micro-Predictor Digits Quadrant Triggers",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "If custom strategy is configured, the system matches recommended top predictions to these toggled quadrant selectors before firing.",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Lower Odds
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("LOWER ODDS [1, 3]", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Bypasses signals unless top model predictions contain odd digits < 5", color = Color.Gray, fontSize = 9.sp)
                                }
                                Switch(
                                    checked = userSettings.triggerLowerOdds,
                                    onCheckedChange = {
                                        viewModel.updateSettingsInDb(userSettings.copy(triggerLowerOdds = it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    )
                                )
                            }

                            // Lower Evens
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("LOWER EVENS [0, 2, 4]", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Bypasses signals unless top model predictions contain even digits < 5", color = Color.Gray, fontSize = 9.sp)
                                }
                                Switch(
                                    checked = userSettings.triggerLowerEvens,
                                    onCheckedChange = {
                                        viewModel.updateSettingsInDb(userSettings.copy(triggerLowerEvens = it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    )
                                )
                            }

                            // Higher Odds
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("HIGHER ODDS [5, 7, 9]", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Bypasses signals unless top model predictions contain odd digits >= 5", color = Color.Gray, fontSize = 9.sp)
                                }
                                Switch(
                                    checked = userSettings.triggerHigherOdds,
                                    onCheckedChange = {
                                        viewModel.updateSettingsInDb(userSettings.copy(triggerHigherOdds = it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    )
                                )
                            }

                            // Higher Evens
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("HIGHER EVENS [6, 8]", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Bypasses signals unless top model predictions contain even digits >= 5", color = Color.Gray, fontSize = 9.sp)
                                }
                                Switch(
                                    checked = userSettings.triggerHigherEvens,
                                    onCheckedChange = {
                                        viewModel.updateSettingsInDb(userSettings.copy(triggerHigherEvens = it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    )
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // 3. Signal alert behavior
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Signal Entry Alerts Mode & Behavior",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Decide the physical execution behavior when entries trigger.",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )

                        val behaviors = listOf(
                            "VIB_ONLY" to "Vibration Only",
                            "NOTIF_ONLY" to "Notification Only",
                            "VIB_AND_NOTIF" to "Vibration & Notification"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            behaviors.forEach { (code, label) ->
                                val isSelected = userSettings.alertBehavior == code
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f))
                                        .clickable {
                                            viewModel.updateSettingsInDb(userSettings.copy(alertBehavior = code))
                                        }
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- VIRTUAL PRACTICE & ALERT TIMERS ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "VIRTUAL SYSTEM & COOLDOWN TUNER",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Slider 1: Virtual Trade Close Ticks
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Virtual Trade Close Ticks", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Ticks to wait before closing what-if practice trades", color = Color.Gray, fontSize = 9.sp)
                            }
                            Text(
                                text = "${userSettings.virtualTradeCloseTicks} ticks",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = userSettings.virtualTradeCloseTicks.toFloat(),
                            onValueChange = {
                                viewModel.updateSettingsInDb(userSettings.copy(virtualTradeCloseTicks = it.toInt()))
                            },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // Slider 1.5: Adaptive Signal Cushion Spacing
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Safety Cushion Spacing", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Controls target barrier shift distance to insulate against tail risk", color = Color.Gray, fontSize = 9.sp)
                            }
                            Text(
                                text = "±${userSettings.cushionSpacing} digits",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = userSettings.cushionSpacing.toFloat(),
                            onValueChange = {
                                viewModel.updateSettingsInDb(userSettings.copy(cushionSpacing = it.toInt()))
                            },
                            valueRange = 1f..4f,
                            steps = 2,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // Slider 2: Push Notification Cooldown
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Notification Cooldown Rate", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Minimum wait time between consecutive push recommendations", color = Color.Gray, fontSize = 9.sp)
                            }
                            Text(
                                text = "${userSettings.signalNotificationCooldownSecs}s",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = userSettings.signalNotificationCooldownSecs.toFloat(),
                            onValueChange = {
                                viewModel.updateSettingsInDb(userSettings.copy(signalNotificationCooldownSecs = it.toInt()))
                            },
                            valueRange = 15f..300f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // --- PERSONAL TRADER PROFILE DESIGN ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "TRADER IDENT PROFILE",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Text Field: Name
                    var tempName by remember(userSettings.traderName) { mutableStateOf(userSettings.traderName) }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Personalization Name", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = {
                                tempName = it
                                viewModel.updateSettingsInDb(userSettings.copy(traderName = it))
                            },
                            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                            placeholder = { Text("e.g. Master Trader", color = Color.Gray, fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }

                    // Text Field: Capital & Currency
                    var tempCapital by remember(userSettings.capital) { mutableStateOf(userSettings.capital.toString()) }
                    var tempCurrency by remember(userSettings.currency) { mutableStateOf(userSettings.currency) }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Starting Capital", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = tempCapital,
                                onValueChange = {
                                    tempCapital = it
                                    it.toDoubleOrNull()?.let { dAmt ->
                                        viewModel.updateSettingsInDb(userSettings.copy(capital = dAmt))
                                    }
                                },
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                                )
                            )
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Currency", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = tempCurrency,
                                onValueChange = {
                                    tempCurrency = it
                                    viewModel.updateSettingsInDb(userSettings.copy(currency = it))
                                },
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }

                    // Text Field: Financial Goal Statement
                    var tempGoal by remember(userSettings.goal) { mutableStateOf(userSettings.goal) }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Financial Goal Statement", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = tempGoal,
                            onValueChange = {
                                tempGoal = it
                                viewModel.updateSettingsInDb(userSettings.copy(goal = it))
                            },
                            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                            placeholder = { Text("e.g. Gain 5% compounding daily target", color = Color.Gray, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // Switch 1: Compass Pointer toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show Radar Compass Guideline", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Overlay visual compass guideline inside quadrant widgets to assist spatial depth", color = Color.Gray, fontSize = 9.sp)
                        }
                        Switch(
                            checked = userSettings.showCompassPointer,
                            onCheckedChange = {
                                viewModel.updateSettingsInDb(userSettings.copy(showCompassPointer = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }

            // --- DERIV AUTOMATED CO-PILOT SETTINGS PANEL ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🤖 DERIV AUTOMATED CO-PILOT",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "Connect your secure Deriv WS Token to place automated contracts on parity signals with smart risk management.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )

                    // Wallet Balance Display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("CO-PILOT WALLET BALANCE", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = String.format("$%.2f %s", userSettings.derivWalletBalance, userSettings.currency.uppercase()),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.updateSettingsInDb(userSettings.copy(derivWalletBalance = 1000.0))
                                viewModel.resetAutoTraderSession()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("reset_wallet_balance_button")
                        ) {
                            Text("Reset", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Secure Deriv Token Input
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("DERIV SECURE WS TOKEN", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = userSettings.derivToken,
                            onValueChange = {
                                viewModel.updateSettingsInDb(userSettings.copy(derivToken = it))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("deriv_token_input"),
                            placeholder = { Text("Paste WS API Token here", color = Color.Gray, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                    }

                    // Auto Trader Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ENABLE AUTOMATED PILOT", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Automatically executes contracts whenever parity signals trigger matches", color = Color.Gray, fontSize = 9.sp)
                        }
                        Switch(
                            checked = userSettings.autoTraderEnabled,
                            onCheckedChange = {
                                viewModel.updateSettingsInDb(userSettings.copy(autoTraderEnabled = it))
                                if (it) {
                                    viewModel.resetAutoTraderSession()
                                }
                            },
                            modifier = Modifier.testTag("auto_trader_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        )
                    }

                    // Auto Recommend calculations shown prominently to satisfy requirements
                    val recStake = (userSettings.derivWalletBalance * 0.01).coerceAtLeast(1.0)
                    val recSL = (userSettings.derivWalletBalance * 0.15).coerceAtLeast(10.0)
                    val recTP = (userSettings.derivWalletBalance * 0.10).coerceAtLeast(5.0)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = String.format("💡 AUTO RECOMMENDATIONS:\n• Target Stake (1%%): $%.2f\n• Stop Loss Limit (15%%): $%.2f\n• Take Profit Target (10%%): $%.2f", recStake, recSL, recTP),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }

                    // Compounding Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AUTO STAKE COMPOUNDING", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Recalculate stake and stop loss (1% / 15%) as pilot balance increases", color = Color.Gray, fontSize = 9.sp)
                        }
                        Switch(
                            checked = userSettings.autoTraderCompoundingStake,
                            onCheckedChange = {
                                viewModel.updateSettingsInDb(userSettings.copy(autoTraderCompoundingStake = it))
                            },
                            modifier = Modifier.testTag("auto_trader_compounding_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        )
                    }

                    // Target Stake Input
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("TARGET COMPANION STAKE (USD)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = if (userSettings.autoTraderCompoundingStake) String.format("%.2f", recStake) else userSettings.autoTraderStake.toString(),
                            onValueChange = {
                                if (!userSettings.autoTraderCompoundingStake) {
                                    it.toDoubleOrNull()?.let { stake ->
                                        viewModel.updateSettingsInDb(userSettings.copy(autoTraderStake = stake))
                                    }
                                }
                            },
                            enabled = !userSettings.autoTraderCompoundingStake,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auto_trader_stake_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Take Profit Input
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("TAKE PROFIT (USD)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = userSettings.autoTraderTakeProfit.toString(),
                                onValueChange = {
                                    it.toDoubleOrNull()?.let { tp ->
                                        viewModel.updateSettingsInDb(userSettings.copy(autoTraderTakeProfit = tp))
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auto_trader_take_profit_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                        }

                        // Stop Loss Input
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("STOP LOSS (USD)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = if (userSettings.autoTraderCompoundingStake) String.format("%.2f", recSL) else userSettings.autoTraderStopLoss.toString(),
                                onValueChange = {
                                    if (!userSettings.autoTraderCompoundingStake) {
                                        it.toDoubleOrNull()?.let { sl ->
                                            viewModel.updateSettingsInDb(userSettings.copy(autoTraderStopLoss = sl))
                                        }
                                    }
                                },
                                enabled = !userSettings.autoTraderCompoundingStake,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auto_trader_stop_loss_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }

                    // Trailing Stop Loss Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TRAILING STOP LOSS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Binds and trails SL target dynamically relative to maximum peak session profits", color = Color.Gray, fontSize = 9.sp)
                        }
                        Switch(
                            checked = userSettings.autoTraderTrailingStopLoss,
                            onCheckedChange = {
                                viewModel.updateSettingsInDb(userSettings.copy(autoTraderTrailingStopLoss = it))
                            },
                            modifier = Modifier.testTag("auto_trader_trailing_sl_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }

            var showDeveloperOptions by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDeveloperOptions = !showDeveloperOptions }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showDeveloperOptions) "▲ HIDE DEVELOPER UTILITIES" else "▼ SHOW DEVELOPER UTILITIES",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            if (showDeveloperOptions) {
                // --- IMMEDIATE TEST ALARM ACTION CORRIDOR ---
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Test Fire",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "SIMULATE ALGO ALARM REMINDER",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "Click to trigger an instant system-level trade signal reminder right now. Verify that channel and notification triggers are working properly in this sandboxed preview.",
                            color = Color.LightGray.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = { viewModel.fireTestAlarm() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("test_fire_alarm_button")
                        ) {
                            Text(
                                text = "🔔 TEST ALARM (TODAY: $triggeredToday / ${userSettings.signalsPerDayLimit})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (triggeredToday > 0) {
                            Text(
                                text = "Reset Signals Counter",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .clickable { viewModel.resetDailySignalsCounter() }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            // Simple Info Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "SQLite info",
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Room-Database Local Cache System. Active & Fully Persistent.",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
