package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.DigitAnalysisViewModel

@Composable
fun SignalsScreen(
    viewModel: DigitAnalysisViewModel,
    modifier: Modifier = Modifier
) {
    val activeSignal by viewModel.activeSignal.collectAsState()
    val countdown by viewModel.signalCountdown.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val signalHistoryList by viewModel.signalHistory.collectAsState()

    val autoSessionProfit by viewModel.autoSessionProfit.collectAsState()
    val targetProfitReached by viewModel.targetProfitReached.collectAsState()
    val stopLossHit by viewModel.stopLossHit.collectAsState()

    var activeTab by remember { mutableStateOf("RADAR") } // "RADAR" or "HISTORY"

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
                    fontSize = 10.sp,
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
                        fontSize = 10.sp,
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
                        fontSize = 10.sp,
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
                            onCheckedChange = {
                                viewModel.updateSettingsInDb(userSettings.copy(autoTraderEnabled = it))
                                if (it) {
                                    viewModel.resetAutoTraderSession()
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
                                        text = "${signal.contractType} ${signal.barrier}",
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
                                                val stakeValue = manualStake.toDoubleOrNull() ?: 5.00
                                                val barrierNum = signal.barrier.toIntOrNull() ?: 5
                                                viewModel.executeManualTrade(
                                                    symbolCode = signal.symbol,
                                                    displayName = signal.displayName,
                                                    contractType = signal.contractType,
                                                    barrier = barrierNum,
                                                    stake = stakeValue
                                                )
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
                                                text = "${trade.contractType} ${trade.barrierValue}",
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
                                            Text(
                                                text = "${trade.entryDigit} ➔ ${if (trade.status == "PENDING") "?" else trade.exitDigit}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
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
                                                    text = if (trade.status == "PENDING") "[PENDING]" else String.format("%s$%.2f", if (trade.profitLoss >= 0) "+" else "", trade.profitLoss),
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
                                                text = "${item.contractType} ${item.barrierValue}",
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
                                            Text(
                                                text = if (item.exitDigit != null) "Exit: ${item.exitDigit}" else "Awaiting: ${item.ticksObserved} / ${item.targetTicks}",
                                                color = if (item.exitDigit != null) Color.White else Color.Gray,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
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
}
