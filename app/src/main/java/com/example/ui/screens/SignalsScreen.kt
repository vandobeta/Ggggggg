package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.DigitAnalysisViewModel
import com.example.ui.theme.getDigitColor
import com.example.ui.theme.getQuadrantColor

@Composable
fun SignalsScreen(
    viewModel: DigitAnalysisViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = LocalContext.current
    val activeSignal by viewModel.activeSignal.collectAsState()
    val countdown by viewModel.signalCountdown.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val signalHistoryList by viewModel.signalHistory.collectAsState()

    val autoSessionProfit by viewModel.autoSessionProfit.collectAsState()
    val targetProfitReached by viewModel.targetProfitReached.collectAsState()
    val stopLossHit by viewModel.stopLossHit.collectAsState()

    var activeTab by remember { mutableStateOf("RADAR") } // "RADAR" or "HISTORY"

    var showTokenPromptDialog by remember { mutableStateOf(false) }
    var tokenPromptAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var tokenInputText by remember { mutableStateOf("") }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF07080F),
            Color(0xFF0C0D1A)
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- SCREEN HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📡 TACTICAL RADAR",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "HFT SIGNALS CORE",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Quick display of active risk profile with direct visual indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(
                        1.dp, 
                        if (userSettings.riskProfile == "RISKY") Color(0xFFEF4444).copy(alpha = 0.3f) 
                        else Color(0xFF10B981).copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (userSettings.riskProfile == "RISKY") Color(0xFFEF4444) else Color(0xFF10B981))
                    )
                    Text(
                        text = if (userSettings.riskProfile == "RISKY") "SNIPER (+900%)" else "SAFETY NET",
                        color = if (userSettings.riskProfile == "RISKY") Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // --- INTERACTIVE SUB-TABS SELECTOR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeTab == "RADAR") MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { activeTab = "RADAR" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "RADAR DECK",
                    color = if (activeTab == "RADAR") Color.White else Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeTab == "MANUAL") MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { activeTab = "MANUAL" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MANUAL",
                    color = if (activeTab == "MANUAL") Color.White else Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeTab == "TRADES") MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { activeTab = "TRADES" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                val tradesList by viewModel.tradeHistory.collectAsState()
                val pendingCount = tradesList.count { it.status == "PENDING" }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "TRADES",
                        color = if (activeTab == "TRADES") Color.White else Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (pendingCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF10B981))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$pendingCount",
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeTab == "HISTORY") MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { activeTab = "HISTORY" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                val pendingCount = signalHistoryList.count { it.isWin == null }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "SIGNALS",
                        color = if (activeTab == "HISTORY") Color.White else Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (pendingCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFBBF24))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$pendingCount",
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeTab == "RADAR") {
            // --- COUNTDOWN PROGRESS CIRCLE INDICATOR ---
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { countdown / 30f },
                    modifier = Modifier.fillMaxSize(),
                    color = if (countdown <= 3) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.5.dp,
                    trackColor = Color.White.copy(alpha = 0.06f),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${countdown}S",
                        color = if (countdown <= 3) Color(0xFFEF4444) else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "REFRESH",
                        color = Color.Gray,
                        fontSize = 7.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // --- SUB-TEXT ON AIR ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Text(
                    text = "REAL-TIME PARITY EXTRACTION PIPELINE ACTIVE",
                    color = Color.LightGray.copy(alpha = 0.5f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // --- AUTO PILOT CO-PILOT HUD FLIGHT DECK ---
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (userSettings.autoTraderEnabled) 
                        Color(0xFF0F172A).copy(alpha = 0.8f) 
                    else 
                        Color(0xFF1E293B).copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(
                        1.dp, 
                        if (userSettings.autoTraderEnabled) Color(0xFF10B981).copy(alpha = 0.3f) 
                        else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "🤖 AUTO-PILOT STATE:",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (userSettings.autoTraderEnabled) "ACTIVE PILOTIN'" else "ENGINES OFFLINE",
                                color = if (userSettings.autoTraderEnabled) Color(0xFF34D399) else Color(0xFFEF4444),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Toggle Pilot Switch directly
                        Switch(
                            checked = userSettings.autoTraderEnabled,
                            onCheckedChange = { isOn ->
                                if (isOn && userSettings.derivToken.isEmpty()) {
                                    tokenInputText = ""
                                    tokenPromptAction = {
                                        viewModel.updateSettingsInDb(userSettings.copy(autoTraderEnabled = true))
                                        viewModel.resetAutoTraderSession()
                                    }
                                    showTokenPromptDialog = true
                                } else {
                                    viewModel.updateSettingsInDb(userSettings.copy(autoTraderEnabled = isOn))
                                    if (isOn) {
                                        viewModel.resetAutoTraderSession()
                                    }
                                }
                            },
                            modifier = Modifier.scale(0.8f).testTag("flight_deck_toggle_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF34D399),
                                checkedTrackColor = Color(0xFF34D399).copy(alpha = 0.3f)
                            )
                        )
                    }

                    // Profit progress bar and statistics
                    val sign = if (autoSessionProfit >= 0) "+" else ""
                    val profitColor = if (autoSessionProfit >= 0) Color(0xFF34D399) else Color(0xFFEF4444)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SESSION NET GAIN",
                                color = Color.Gray,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = String.format("%s$%.2f USD", sign, autoSessionProfit),
                                color = profitColor,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TARGET PROPORTION",
                                color = Color.Gray,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = String.format("$%.2f / $%.2f", autoSessionProfit, userSettings.autoTraderTakeProfit),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Progress bar mapping Session Profit against Take Profit threshold limit
                    if (userSettings.autoTraderTakeProfit > 0) {
                        val fraction = (autoSessionProfit / userSettings.autoTraderTakeProfit).coerceIn(0.0, 1.0).toFloat()
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF34D399),
                            trackColor = Color.White.copy(alpha = 0.08f)
                        )
                    }

                    // Alert Messages for targets reached
                    if (targetProfitReached) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF34D399).copy(alpha = 0.15f))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "🏆 TARGET PROFIT DEFEATED! Session closed successfully. Co-pilot idle.",
                                color = Color(0xFF6EE7B7),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else if (stopLossHit) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "⛔ STOP LOSS DETECTED. Session halted automatically for balance defense.",
                                color = Color(0xFFFCA5A5),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else if (userSettings.autoTraderEnabled && userSettings.derivToken.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFBBF24).copy(alpha = 0.12f))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = "⚠️ NO DERIV TOKEN: Simulator execution fallback active.",
                                color = Color(0xFFFDE047),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else if (userSettings.autoTraderEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format("Current Base: $%.2f | Compounding: %s", userSettings.derivWalletBalance, if (userSettings.autoTraderCompoundingStake) "YES" else "NO"),
                                color = Color.Gray,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Text(
                                text = "Session Active",
                                color = Color(0xFF34D399),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .clickable { viewModel.resetAutoTraderSession() }
                                    .border(0.5.dp, Color(0xFF34D399), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // --- PRIMARY SIGNAL BOARD (LIQUID FROSTED GLASS EFFECT) ---
            Crossfade(
                targetState = activeSignal,
                animationSpec = tween(500),
                label = "signal_transition",
                modifier = Modifier.weight(1f)
            ) { signal ->
                if (signal != null) {
                    val isSuperseded = signal.id != activeSignal?.id
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Frosty card centering the main recommended trade
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF13141F).copy(alpha = 0.65f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Row 1: Target Asset Symbol info
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Text(
                                            text = signal.displayName.uppercase(),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )

                                        if (signal.crossoverActive) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF818CF8).copy(alpha = 0.2f))
                                                    .border(1.dp, Color(0xFF818CF8), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF818CF8))
                                                    )
                                                    Text(
                                                        text = "CROSSOVER",
                                                        color = Color(0xFFC7D2FE),
                                                        fontSize = 7.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = signal.zone,
                                        color = if (signal.zone == "CHILL ZONE") Color(0xFF38BDF8) else if (signal.zone == "SKY HIGH") Color(0xFFFBBF24) else Color(0xFFE2E8F0),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.04f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                // Huge highlighted recommendation
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isSuperseded) "⚠️ SIGNAL SUPERSEDED" else "RECOMMENDED ACTION",
                                        textDecoration = if (isSuperseded) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (isSuperseded) Color(0xFFEF4444) else Color.Gray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )

                                    val highlightColor = if (isSuperseded) {
                                        Color(0xFFEF4444).copy(alpha = 0.5f)
                                    } else if (signal.contractType == "UNDER") {
                                        Color(0xFF34D399)
                                    } else {
                                        Color(0xFFFBBF24)
                                    }
                                    Text(
                                        text = if (signal.contractType == "EVEN" || signal.contractType == "ODD") signal.contractType else "${signal.contractType} ${signal.barrier}",
                                        color = highlightColor,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center,
                                        textDecoration = if (isSuperseded) TextDecoration.LineThrough else TextDecoration.None
                                    )

                                    Text(
                                        text = if (isSuperseded) "REGENERATING SIGNAL..." else "ESTIMATED RETURN: ${signal.payoutPct}",
                                        color = if (isSuperseded) Color.Gray else Color.White.copy(alpha = 0.85f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        textDecoration = if (isSuperseded) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                }

                                // Horizontal list of the underlying digit candidates
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "DETECTED HOT DIGIT CANDIDATES",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.5.sp
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        signal.candidates.forEach { digit ->
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.05f))
                                                    .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = digit.toString(),
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }

                                // REAL-TIME PER-TICK WINS DISPLAY
                                val tickUpdate by viewModel.tickUpdateFlow.collectAsState()
                                val recentTicks = remember(tickUpdate, signal) {
                                    viewModel.getHistoryFor(signal.symbol).takeLast(10)
                                }
                                
                                if (recentTicks.isNotEmpty()) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "REAL-TIME PER-TICK MONITOR",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 0.5.sp
                                        )

                                        val barrierNum = signal.barrier.toIntOrNull() ?: 5
                                        val winCount = recentTicks.count { digit ->
                                            when (signal.contractType) {
                                                "UNDER" -> digit < barrierNum
                                                "OVER" -> digit > barrierNum
                                                "DIFFERS" -> digit != barrierNum
                                                "EVEN", "DIGITEVEN" -> digit % 2 == 0
                                                "ODD", "DIGITODD" -> digit % 2 != 0
                                                else -> false
                                            }
                                        }
                                        val winPercentage = (winCount.toFloat() / recentTicks.size) * 100f

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            recentTicks.forEach { digit ->
                                                val isWin = when (signal.contractType) {
                                                    "UNDER" -> digit < barrierNum
                                                    "OVER" -> digit > barrierNum
                                                    "DIFFERS" -> digit != barrierNum
                                                    "EVEN", "DIGITEVEN" -> digit % 2 == 0
                                                    "ODD", "DIGITODD" -> digit % 2 != 0
                                                    else -> false
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isWin) Color(0xFF065F46) else Color(0xFF7F1D1D))
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (isWin) Color(0xFF34D399) else Color(0xFFF87171),
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = digit.toString(),
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = String.format(java.util.Locale.US, "Current Session Ticks: %d/%d WINS (%.1f%% win)", winCount, recentTicks.size, winPercentage),
                                            color = if (winPercentage >= 80f) Color(0xFF34D399) else if (winPercentage >= 50f) Color(0xFFFBBF24) else Color(0xFFEF4444),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                // Dynamic risk message box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "info",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = signal.message,
                                            color = Color.LightGray,
                                            fontSize = 9.sp,
                                            lineHeight = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // MANUAL EXECUTION ENGINE DECK
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "⚡ MANUAL EXECUTION ENGINE",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )

                                    var manualStake by remember { mutableStateOf("5.00") }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = manualStake,
                                            onValueChange = { newValue ->
                                                if (newValue.toDoubleOrNull() != null || newValue.isEmpty()) {
                                                    manualStake = newValue
                                                }
                                            },
                                            label = { Text("STAKE ($)", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                                focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                                unfocusedLabelColor = Color.Gray,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                            modifier = Modifier.weight(1f)
                                        )

                                        Button(
                                            onClick = {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                               val stakeValue = manualStake.toDoubleOrNull() ?: 5.00
                                                val barrierNum = signal.barrier.toIntOrNull() ?: 5
                                                val runAction = {
                                                    viewModel.executeManualTrade(
                                                        symbolCode = signal.symbol,
                                                        displayName = signal.displayName,
                                                        contractType = signal.contractType,
                                                        barrier = barrierNum,
                                                        stake = stakeValue
                                                    )
                                                }
                                                if (userSettings.derivToken.isEmpty()) {
                                                    tokenInputText = ""
                                                    tokenPromptAction = runAction
                                                    showTokenPromptDialog = true
                                                } else {
                                                    runAction()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (signal.contractType == "UNDER") Color(0xFF10B981) else Color(0xFFFBBF24)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .height(56.dp)
                                                .testTag("manual_execute_trade_button")
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "play",
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "EXECUTE",
                                                    color = Color.Black,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }

                                    // Quick Stake selectors
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf("1.00", "5.00", "10.00", "25.00", "50.00").forEach { preset ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (manualStake == preset) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                                    .border(
                                                        0.5.dp,
                                                        if (manualStake == preset) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable { manualStake = preset }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$$preset",
                                                    color = if (manualStake == preset) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom card: Probability and accuracy gauge
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF13141F).copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "DIRECTION CONVICTION SCORE",
                                        color = Color.Gray,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        val barColor = if (signal.probabilityEst >= 70f) Color(0xFF10B981) else Color(0xFFFBBF24)
                                        Box(
                                            modifier = Modifier
                                                .height(6.dp)
                                                .weight(1f)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color.White.copy(alpha = 0.1f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(signal.probabilityEst / 100f)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(barColor)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "CONFIDENCE",
                                        color = Color.Gray,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "${signal.probabilityEst.toInt()}%",
                                        color = if (signal.probabilityEst >= 70f) Color(0xFF34D399) else Color(0xFFFCD34D),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Signal loading state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "COMPILING DIGITAL CHUCK MATRICES...",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- IMMEDIATE REFRESH DEVIATION SIGNAL CONTROLLER ---
            Button(
                onClick = { viewModel.generateFreshSignal() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Force query update"
                    )
                    Text(
                        text = "RE-CALCULATE RADAR SIGNAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Decoupled Warning info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Risk alert",
                    tint = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Signals degrade quickly. Enter contract within 5 seconds of alert.",
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        } else if (activeTab == "MANUAL") {
            val selectedPacket by viewModel.selectedPacket.collectAsState()
            val totalTicks = selectedPacket?.tickHistory?.size ?: 100
            val counts = selectedPacket?.digitBreakdowns ?: IntArray(10) { 10 }
            val percentages = counts.map { (it.toFloat() / totalTicks.coerceAtLeast(1)) * 100f }
            val maxPct = percentages.maxOrNull() ?: 0f
            val minPct = percentages.minOrNull() ?: 0f
            val maxIndex = percentages.indexOf(maxPct)
            val minIndex = percentages.indexOf(minPct)

            var isCustomMode by remember { mutableStateOf(false) }
            var targetSymbol by remember { mutableStateOf("1HZ100V") }
            var stakeInput by remember(userSettings.stake) { mutableStateOf(userSettings.stake.toString()) }
            var customContractType by remember { mutableStateOf("UNDER") }
            var customBarrier by remember { mutableStateOf(4) }

            // Parse AI configuration
            val signal = activeSignal
            val autoSymbol = signal?.symbol ?: "1HZ100V"
            val autoDisplayName = signal?.displayName ?: "Volatility 100 (1s) Index"
            val autoType = signal?.contractType ?: "UNDER"
            val autoBarrier = signal?.barrier?.toIntOrNull() ?: 5

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Real-time market detail info
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF13141F).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📊 STREAM: " + (selectedPacket?.displayName ?: "VOLATILITY 100 (1S)").uppercase(),
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "LIVE TICK: " + String.format("%.4f", selectedPacket?.lastTickValue ?: 1000.0),
                                    color = Color(0xFF34D399),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            // 0-9 Digit Percentage Grid (Circular Indicators)
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "REAL-TIME DIGIT DENSITY DISTRIBUTION (LAST ${totalTicks} TICKS)",
                                    color = Color.Gray,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                
                                // Row 1 (digits 0-4)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (digit in 0..4) {
                                        val pct = percentages.getOrNull(digit) ?: 10.0f
                                        val isMax = digit == maxIndex
                                        val isMin = digit == minIndex
                                        val strokeColor = when {
                                            isMax -> Color(0xFFF97316) // Warm
                                            isMin -> Color(0xFF06B6D4) // Cool exclusion zone
                                            else -> Color.White.copy(alpha = 0.1f)
                                        }
                                        val bgColor = when {
                                            isMax -> Color(0xFFF97316).copy(alpha = 0.1f)
                                            isMin -> Color(0xFF06B6D4).copy(alpha = 0.1f)
                                            else -> Color.Transparent
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(width = 62.dp, height = 62.dp)
                                                .clip(CircleShape)
                                                .background(bgColor)
                                                .border(1.dp, strokeColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "$digit",
                                                    color = when {
                                                        isMax -> Color(0xFFFBBF24)
                                                        isMin -> Color(0xFF22D3EE)
                                                        else -> Color.White
                                                    },
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = String.format("%.1f%%", pct),
                                                    color = Color.Gray,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                if (isMax) {
                                                    Text("HOT", color = Color(0xFFF97316), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                                } else if (isMin) {
                                                    Text("COLD", color = Color(0xFF06B6D4), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Row 1 (digits 5-9)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (digit in 5..9) {
                                        val pct = percentages.getOrNull(digit) ?: 10.0f
                                        val isMax = digit == maxIndex
                                        val isMin = digit == minIndex
                                        val strokeColor = when {
                                            isMax -> Color(0xFFF97316)
                                            isMin -> Color(0xFF06B6D4)
                                            else -> Color.White.copy(alpha = 0.1f)
                                        }
                                        val bgColor = when {
                                            isMax -> Color(0xFFF97316).copy(alpha = 0.1f)
                                            isMin -> Color(0xFF06B6D4).copy(alpha = 0.1f)
                                            else -> Color.Transparent
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(width = 62.dp, height = 62.dp)
                                                .clip(CircleShape)
                                                .background(bgColor)
                                                .border(1.dp, strokeColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "$digit",
                                                    color = when {
                                                        isMax -> Color(0xFFFBBF24)
                                                        isMin -> Color(0xFF22D3EE)
                                                        else -> Color.White
                                                    },
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = String.format("%.1f%%", pct),
                                                    color = Color.Gray,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                if (isMax) {
                                                    Text("HOT", color = Color(0xFFF97316), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                                } else if (isMin) {
                                                    Text("COLD", color = Color(0xFF06B6D4), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Control Segment Toggle: Mode Selector
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!isCustomMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { isCustomMode = false }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🤖 AI CO-PILOT PRESET",
                                color = if (!isCustomMode) Color.White else Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isCustomMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { isCustomMode = true }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚡ CUSTOM EXECUTION",
                                color = if (isCustomMode) Color.White else Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Config Details Card
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF080B15)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isCustomMode) "⚡ ABSOLUTE MANUAL CONTROL CONFIG" else "🤖 TACTICAL CO-PILOT PRESET ACTIVE",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )

                            if (isCustomMode) {
                                // Manual Config Grid
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Symbol selector
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("TARGET MARKET ASSET", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            val symbols = listOf("1HZ10V" to "V10 (1s)", "1HZ25V" to "V25 (1s)", "1HZ50V" to "V50 (1s)", "1HZ75V" to "V75 (1s)", "1HZ100V" to "V100 (1s)")
                                            symbols.forEach { (code, name) ->
                                                val sel = targetSymbol == code
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                                        .border(0.5.dp, if (sel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                                        .clickable { 
                                                            targetSymbol = code 
                                                            viewModel.selectSymbol(code)
                                                        }
                                                        .padding(vertical = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(name, color = if (sel) Color.White else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                }
                                            }
                                        }
                                    }

                                    // Contract Type Selector Row
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("CONTRACT CATEGORY STYLE", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val types = listOf(
                                                "UNDER", "OVER", "MATCHES", "DIFFERS", "EVEN", "ODD", 
                                                "RISE", "FALL", "ACCUM", "ASIANU", "ASIAND", "CALL", "PUT"
                                            )
                                            types.forEach { type ->
                                                val sel = customContractType == type
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                                        .border(0.5.dp, if (sel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                                        .clickable { customContractType = type }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(type, color = if (sel) Color.White else Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                }
                                            }
                                        }
                                    }

                                    // Dynamic Barrier/Param Select (Only if applicable)
                                    val needsBarrier = customContractType in listOf("UNDER", "OVER", "MATCHES", "DIFFERS")
                                    val needsGrowthRate = customContractType == "ACCUM"
                                    
                                    if (needsBarrier) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            val title = when (customContractType) {
                                                "UNDER" -> "UNDER TARGET BARRIER (STAKE PAYOUT EXITS < BARRIER)"
                                                "OVER" -> "OVER TARGET BARRIER (STAKE PAYOUT EXITS > BARRIER)"
                                                "MATCHES" -> "MATCHING DIGIT REQUIREMENT"
                                                else -> "DIFFERS EXCLUDED DIGIT MATCH"
                                            }
                                            Text(title, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                val start = if (customContractType == "UNDER") 1 else 0
                                                val end = if (customContractType == "OVER") 8 else 9
                                                for (b in start..end) {
                                                    val sel = customBarrier == b
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(if (sel) Color(0xFFF97316).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                                            .border(0.5.dp, if (sel) Color(0xFFF97316) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                                            .clickable { customBarrier = b }
                                                            .padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("$b", color = if (sel) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                                    }
                                                }
                                            }
                                        }
                                    } else if (needsGrowthRate) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("ACCUMULATOR TICK GROWTH RATE PERCENTAGE (%)", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                for (g in 1..5) {
                                                    val sel = customBarrier == g
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(if (sel) Color(0xFFF97316).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                                            .border(0.5.dp, if (sel) Color(0xFFF97316) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                                            .clickable { customBarrier = g }
                                                            .padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("$g%", color = if (sel) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // AI Preset Read-only Details
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("AUTO CHOSEN ASSET (BEST EDGE SCORING)", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        Text(autoDisplayName.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("HIGH RISK-PRESET ACTION PLAN", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        Text(if (autoType == "EVEN" || autoType == "ODD") autoType else "$autoType $autoBarrier", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                    }
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("TACTICAL CONFIDENCE FACTOR", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        Text(String.format("%.1f%%", signal?.probabilityEst ?: 92.5f), color = Color(0xFF818CF8), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                            // Inputs for Manual Stake
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("TRANSACTION QUANTIZATION STAKE SIZE (USD)", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = stakeInput,
                                        onValueChange = { stakeInput = it },
                                        placeholder = { Text("5.00", color = Color.DarkGray) },
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                                        )
                                    )
                                    
                                    val stakeOptions = listOf("1.00", "5.00", "10.00", "25.00", "50.00")
                                    stakeOptions.forEach { opt ->
                                        val sel = stakeInput == opt
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (sel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.04f))
                                                .clickable { stakeInput = opt }
                                                .padding(horizontal = 8.dp, vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("$$opt", color = if (sel) Color.White else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Action submission trigger
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    val runAction = {
                                        viewModel.executeManualTrade(
                                            symbolCode = if (isCustomMode) targetSymbol else autoSymbol,
                                            displayName = if (isCustomMode) {
                                                when(targetSymbol) {
                                                    "1HZ10V" -> "Volatility 10 (1s) Index"
                                                    "1HZ25V" -> "Volatility 25 (1s) Index"
                                                    "1HZ50V" -> "Volatility 50 (1s) Index"
                                                    "1HZ75V" -> "Volatility 75 (1s) Index"
                                                    else -> "Volatility 100 (1s) Index"
                                                }
                                            } else autoDisplayName,
                                            contractType = if (isCustomMode) customContractType else autoType,
                                            barrier = if (isCustomMode) customBarrier else autoBarrier,
                                            stake = stakeInput.toDoubleOrNull() ?: 5.0
                                        )
                                        Toast.makeText(context, "Trade Dispatched to Execution Pool!", Toast.LENGTH_SHORT).show()
                                    }
                                    if (userSettings.derivToken.isEmpty()) {
                                        tokenInputText = ""
                                        tokenPromptAction = runAction
                                        showTokenPromptDialog = true
                                    } else {
                                        runAction()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCustomMode) Color(0xFF10B981) else Color(0xFF4F46E5)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Submit"
                                    )
                                    Text(
                                        text = if (isCustomMode) "⚡ SUBMIT CUSTOM CONTRACT" else "🚀 EXECUTE CO-PILOT SIGNAL NOW",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (activeTab == "TRADES") {
            val tradesList by viewModel.tradeHistory.collectAsState()
            
            // Aggregate Stats
            val totalTrades = tradesList.size
            val winsVal = tradesList.count { it.status == "WIN" }
            val lossesVal = tradesList.count { it.status == "LOSS" }
            val winrateVal = if (totalTrades > 0) (winsVal.toFloat() / totalTrades) * 100f else 0.0f
            val netProfitVal = tradesList.sumOf { it.profitLoss }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF13141F).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (netProfitVal >= 0) Color(0xFF10B981) else Color(0xFFEF4444)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "NET P&L PERFORMANCE",
                                    color = Color.LightGray.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = String.format("%s$%.2f USD", if (netProfitVal >= 0) "+" else "", netProfitVal),
                                color = if (netProfitVal >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        LinearProgressIndicator(
                            progress = { (winrateVal / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape),
                            color = if (winrateVal >= 60f) Color(0xFF10B981) else Color(0xFFFBBF24),
                            trackColor = Color.White.copy(alpha = 0.05f)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WINRATE: ${String.format("%.1f", winrateVal)}%",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "TRADES: $totalTrades ($winsVal W - $lossesVal L)",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRANSACTION LOGS (${tradesList.size})",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (tradesList.isNotEmpty()) {
                        Text(
                            text = "PURGE LOGS",
                            color = Color(0xFFEF4444),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clickable { viewModel.clearTradeHistory() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (tradesList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO COMPLETED TRADES REGISTERED YET.\nACTIVATE AUTOPILOT OR RUN MANUAL TRANSACTIONS.",
                            color = Color.White.copy(alpha = 0.25f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tradesList) { trade ->
                            val dateStr = try {
                                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(trade.timestamp))
                            } catch(e: Exception) {
                                trade.timestamp.toString()
                            }
                            
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF13141F).copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        when (trade.status) {
                                            "WIN" -> Color(0xFF10B981).copy(alpha = 0.25f)
                                            "LOSS" -> Color(0xFFEF4444).copy(alpha = 0.25f)
                                            else -> Color.White.copy(alpha = 0.08f)
                                        },
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (trade.accountType == "DEMO") Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = trade.accountType,
                                                    color = if (trade.accountType == "DEMO") Color(0xFF34D399) else Color(0xFFEF4444),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            Text(
                                                text = "${trade.displayName} [${trade.tradeType}]",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Text(
                                            text = dateStr,
                                            color = Color.Gray,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "CONTRACT",
                                                color = Color.Gray,
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = if (trade.contractType == "EVEN" || trade.contractType == "ODD") trade.contractType else "${trade.contractType} ${trade.barrierValue}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "ENTRY/EXIT",
                                                color = Color.Gray,
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = trade.entryDigit.toString(),
                                                    color = getDigitColor(trade.entryDigit),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = " ➔ ",
                                                    color = Color.Gray.copy(alpha = 0.5f),
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                if (trade.status == "PENDING") {
                                                    Text(
                                                        text = "?",
                                                        color = Color.White.copy(alpha = 0.5f),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                } else {
                                                    val exitVal = trade.exitDigit ?: 0
                                                    Text(
                                                        text = exitVal.toString(),
                                                        color = getDigitColor(exitVal),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "STAKE/P&L",
                                                color = Color.Gray,
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = String.format("$%.2f", trade.stake),
                                                    color = Color.LightGray,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = if (trade.status == "PENDING") "PENDING (TICK ${viewModel.tradeTicksRemaining[trade.id] ?: 0}/${userSettings.virtualTradeCloseTicks})" else String.format("%s$%.2f", if (trade.profitLoss >= 0) "+" else "", trade.profitLoss),
                                                    color = when (trade.status) {
                                                        "WIN" -> Color(0xFF10B981)
                                                        "LOSS" -> Color(0xFFEF4444)
                                                        else -> Color(0xFFFBBF24)
                                                    },
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- SIGNAL HISTORICAL LEDGER TAB VIEW ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Real-time Winrate Dashboard Card
                val resolvedList = signalHistoryList.filter { it.isWin != null }
                val totalResolved = resolvedList.size
                val wins = resolvedList.count { it.isWin == true }
                val losses = totalResolved - wins
                val winrate = if (totalResolved > 0) (wins.toFloat() / totalResolved) * 100f else 0f

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10111F)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "REAL-TIME WIN RATE",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (winrate >= 80f) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (winrate >= 80f) "OPTIMAL SHIELD 🛡️" else "MONITOR UNCERTAINTIES ⚠",
                                    color = if (winrate >= 80f) Color(0xFF10B981) else Color(0xFFF59E0B),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f%%", winrate),
                                    color = if (winrate >= 80f) Color(0xFF10B981) else Color(0xFFFBBF24),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ACCURACY",
                                    color = Color.LightGray.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "STATS: $wins W - $losses L",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "OUT OF $totalResolved RESOLVED",
                                    color = Color.Gray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Progress bar for visual balance
                        LinearProgressIndicator(
                            progress = { (winrate / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape),
                            color = if (winrate >= 80f) Color(0xFF10B981) else Color(0xFFFBBF24),
                            trackColor = Color.White.copy(alpha = 0.05f)
                        )
                    }
                }

                // Header with clear history button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HISTORICAL LEDGER (${signalHistoryList.size} records)",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    if (signalHistoryList.isNotEmpty()) {
                        Text(
                            text = "CLEAR ALL",
                            color = Color(0xFFEF4444),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clickable { viewModel.clearSignalHistory() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (signalHistoryList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO HISTORIC SIGNAL DATA YET.\nSTAY TUNED FOR RE-CALCULATIONS.",
                            color = Color.White.copy(alpha = 0.25f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(signalHistoryList) { item ->
                            val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                            val dateStr = timeFormat.format(java.util.Date(item.timestamp))
                            
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF13141F).copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = when (item.isWin) {
                                            true -> Color(0xFF10B981).copy(alpha = 0.25f)
                                            false -> Color(0xFFEF4444).copy(alpha = 0.25f)
                                            else -> Color.White.copy(alpha = 0.08f)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Row 1: Header (ID, Timestamp, and Outcome tag)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.signalId,
                                                color = Color.LightGray,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "@ $dateStr",
                                                color = Color.Gray,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        // Outcome badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    when (item.isWin) {
                                                        true -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                        false -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                                        else -> Color(0xFFFBBF24).copy(alpha = 0.1f)
                                                    }
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = when (item.isWin) {
                                                    true -> "WIN 🟢"
                                                    false -> "LOSS 🔴"
                                                    else -> "PENDING 🟡"
                                                },
                                                color = when (item.isWin) {
                                                    true -> Color(0xFF34D399)
                                                    false -> Color(0xFFFCA5A5)
                                                    else -> Color(0xFFFBBF24)
                                                },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    // Row 2: Asset and trade recommendations
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Column {
                                            Text(
                                                text = item.displayName.uppercase(),
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Risk: ${item.riskProfile}",
                                                color = Color.Gray,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = if (item.contractType == "EVEN" || item.contractType == "ODD") item.contractType else "${item.contractType} ${item.barrierValue}",
                                                color = if (item.contractType == "UNDER") Color(0xFF34D399) else Color(0xFFFBBF24),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Payout: ${item.payoutPct}",
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    // Row 3: Triggered Candidates list & exit logs
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.02f))
                                            .padding(6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Candidates: { ${item.winDigits} }",
                                            color = Color.Gray,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )

                                        Column(horizontalAlignment = Alignment.End) {
                                            if (item.exitDigit != null) {
                                                Text(
                                                    text = "Exit: ${item.exitDigit}",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            } else {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "Awaiting:",
                                                        color = Color.Gray,
                                                        fontSize = 8.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    TickDotsProgress(elapsed = item.ticksObserved, total = item.targetTicks)
                                                }
                                            }
                                            if (item.observedTicks.isNotEmpty()) {
                                                Text(
                                                    text = "Path: [${item.observedTicks}]",
                                                    color = Color.Gray,
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTokenPromptDialog) {
        AlertDialog(
            onDismissRequest = { 
                showTokenPromptDialog = false 
                tokenPromptAction = null
            },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security Token Required",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "API AUTH TOKEN REQUIRED",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "A valid Deriv Personal Access Token (PAT) is required to perform authorized operations. Enter your token below to authenticate securely:",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = Color.LightGray
                    )
                    OutlinedTextField(
                        value = tokenInputText,
                        onValueChange = { tokenInputText = it },
                        placeholder = { Text("Paste your API token here...", color = Color.Gray, fontSize = 11.sp) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("pop_up_token_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanToken = tokenInputText.trim()
                        if (cleanToken.isNotEmpty()) {
                            viewModel.updateSettingsInDb(userSettings.copy(derivToken = cleanToken))
                            showTokenPromptDialog = false
                            viewModel.validateTokenAndInitializeEngine(cleanToken, userSettings.isDemoAccount) { success, msg ->
                                if (success) {
                                    tokenPromptAction?.invoke()
                                } else {
                                    Toast.makeText(context, "Auth Error: $msg", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("SAVE & ACTIVATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showTokenPromptDialog = false
                        tokenPromptAction = null
                    }
                ) {
                    Text("CANCEL", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun TickDotsProgress(elapsed: Int, total: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val count = total.coerceAtLeast(1)
        for (i in 1..count) {
            val isElapsed = i <= elapsed
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isElapsed) Color(0xFF10B981) else Color.White.copy(alpha = 0.2f))
                    .border(
                        width = 0.5.dp,
                        color = if (isElapsed) Color(0xFF6EE7B7) else Color.Transparent,
                        shape = CircleShape
                    )
            )
        }
    }
}
