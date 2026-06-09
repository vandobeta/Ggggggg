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
    val marketChoppyBlocked by viewModel.marketChoppyBlocked.collectAsState()
    val entryTriggerAwaiting by viewModel.entryTriggerAwaiting.collectAsState()
    val dualVectorState by viewModel.dualVectorState.collectAsState()

    val autoSessionProfit by viewModel.autoSessionProfit.collectAsState()
    val targetProfitReached by viewModel.targetProfitReached.collectAsState()
    val stopLossHit by viewModel.stopLossHit.collectAsState()

    var activeTab by remember { mutableStateOf("ALL") } // "ALL" dashboard as default experience
    var paramsExpanded by remember { mutableStateOf(false) } // Collapse state for retractable stake & tick selector

    LaunchedEffect(Unit) {
        viewModel.refreshDerivBalance()
    }

    var isCustomMode by remember { mutableStateOf(false) }
    val targetSymbol by viewModel.selectedSymbol.collectAsState()
    var stakeInput by remember(userSettings.stake) { mutableStateOf(userSettings.stake.toString()) }
    var customContractType by remember { mutableStateOf("UNDER") }
    var customBarrier by remember { mutableStateOf(4) }
    var customConfigExpanded by remember { mutableStateOf(false) } // Default: collapsed to lift EXECUTE button into view
    var assetSelectorExpanded by remember { mutableStateOf(false) }
    var customSubTab by remember { mutableStateOf("MATCHES/DIFFERS") }

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
                    .weight(1.1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeTab == "ALL") MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { activeTab = "ALL" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ALL-IN-ONE",
                    color = if (activeTab == "ALL") Color.White else Color.Gray,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeTab == "RADAR") MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { activeTab = "RADAR" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "RADAR DECK",
                    color = if (activeTab == "RADAR") Color.White else Color.Gray,
                    fontSize = 8.sp,
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

        if (activeTab == "ALL") {
            val selectedPacket by viewModel.selectedPacket.collectAsState()
            val totalTicks = selectedPacket?.tickHistory?.size ?: 100
            val counts = selectedPacket?.digitBreakdowns ?: IntArray(10) { 10 }
            val percentages = counts.map { (it.toFloat() / totalTicks.coerceAtLeast(1)) * 100f }
            val maxPct = percentages.maxOrNull() ?: 0f
            val minPct = percentages.minOrNull() ?: 0f
            val maxIndex = percentages.indexOf(maxPct)
            val minIndex = percentages.indexOf(minPct)

            val signal = activeSignal
            val autoSymbol = signal?.symbol ?: "1HZ100V"
            val autoDisplayName = signal?.displayName ?: "Volatility 100 (1s) Index"
            val autoType = signal?.contractType ?: "UNDER"
            val autoBarrier = signal?.barrier?.toIntOrNull() ?: 5

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // --- Part 1: Auto Co-Pilot Flight Deck HUD Panel (Radar Tab Core) ---
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
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (userSettings.autoTraderEnabled) Color(0xFF10B981) else Color(0xFFEF4444))
                                    )
                                    Text(
                                        text = if (userSettings.autoTraderEnabled) "CO-PILOT ACTIVE" else "CO-PILOT STANDBY",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                
                                Switch(
                                    checked = userSettings.autoTraderEnabled,
                                    onCheckedChange = { checked ->
                                        viewModel.updateSettingsInDb(userSettings.copy(autoTraderEnabled = checked))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.05f)
                                    )
                                )
                            }
                            
                            if (signal != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF34D399).copy(alpha = 0.12f))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("ACTIVE DIRECTIVE DETECTED", color = Color(0xFF34D399), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Text("${signal.displayName.uppercase()}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        Text("${signal.contractType} ${signal.barrier} (CONFIDENCE: ${String.format("%.1f%%", signal.probabilityEst)})", color = Color.LightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFF34D399))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("AUTOEXEC", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            } else {
                                Text(
                                    text = "AWAITING PARITY CONVERGENCE FILTER (DIFF > 20% ONLY)...",
                                    color = Color.Gray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // --- Part 2: Digit Density Breakdown Grid (with pointer ▼) ---
                item {
                    val currentTickDigit = selectedPacket?.tickHistory?.lastOrNull()
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🌀 REALTIME DIGIT DENSITY PATTERNS",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            // Row 1 (digits 0-4)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                                    val hasDigitInRow1 = currentTickDigit != null && currentTickDigit in 0..4
                                    if (hasDigitInRow1) {
                                        val animatedRow1Bias by animateFloatAsState(
                                            targetValue = (currentTickDigit ?: 0) * 0.5f - 1.0f,
                                            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                                            label = "dashboard_cursor_row1"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(androidx.compose.ui.BiasAlignment(horizontalBias = animatedRow1Bias, verticalBias = 1.0f))
                                                .width(62.dp),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Text("▼", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (digit in 0..4) {
                                        val p = percentages.getOrNull(digit) ?: 10.0f
                                        val isMax = digit == maxIndex
                                        val isMin = digit == minIndex
                                        val isCurrentTick = currentTickDigit != null && digit == currentTickDigit
                                        
                                        val strokeColor = when {
                                            isCurrentTick -> MaterialTheme.colorScheme.primary
                                            isMax -> Color(0xFFF97316)
                                            isMin -> Color(0xFF06B6D4)
                                            else -> Color.White.copy(alpha = 0.1f)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(width = 62.dp, height = 62.dp)
                                                .clip(CircleShape)
                                                .background(if (isCurrentTick) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                                .border(if (isCurrentTick) 2.dp else 0.5.dp, strokeColor, CircleShape)
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("$digit", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                                Text(String.format("%.1f%%", p), color = strokeColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                }
                            }

                            // Row 2 (digits 5-9)
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Box(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                                    val hasDigitInRow2 = currentTickDigit != null && currentTickDigit in 5..9
                                    if (hasDigitInRow2) {
                                        val animatedRow2Bias by animateFloatAsState(
                                            targetValue = ((currentTickDigit ?: 5) - 5) * 0.5f - 1.0f,
                                            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                                            label = "dashboard_cursor_row2"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(androidx.compose.ui.BiasAlignment(horizontalBias = animatedRow2Bias, verticalBias = 1.0f))
                                                .width(62.dp),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Text("▼", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (digit in 5..9) {
                                        val p = percentages.getOrNull(digit) ?: 10.0f
                                        val isMax = digit == maxIndex
                                        val isMin = digit == minIndex
                                        val isCurrentTick = currentTickDigit != null && digit == currentTickDigit
                                        
                                        val strokeColor = when {
                                            isCurrentTick -> MaterialTheme.colorScheme.primary
                                            isMax -> Color(0xFFF97316)
                                            isMin -> Color(0xFF06B6D4)
                                            else -> Color.White.copy(alpha = 0.1f)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(width = 62.dp, height = 62.dp)
                                                .clip(CircleShape)
                                                .background(if (isCurrentTick) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                                .border(if (isCurrentTick) 2.dp else 0.5.dp, strokeColor, CircleShape)
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("$digit", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                                Text(String.format("%.1f%%", p), color = strokeColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Part 3: Even vs Odd Parity Convergences Matrix ---
                item {
                    val oddCount = counts.filterIndexed { index, _ -> index % 2 != 0 }.sum()
                    val evenCount = counts.filterIndexed { index, _ -> index % 2 == 0 }.sum()
                    val totalCount = (oddCount + evenCount).coerceAtLeast(1)
                    val oddPercentage = (oddCount.toFloat() / totalCount) * 100f
                    val evenPercentage = (evenCount.toFloat() / totalCount) * 100f
                    val parityDiff = kotlin.math.abs(oddPercentage - evenPercentage)
                    val diffIsGood = parityDiff > 20f

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚡ DIGITAL PARITY DETECTOR",
                                    color = Color.LightGray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (diffIsGood) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f))
                                        .border(0.5.dp, if (diffIsGood) Color(0xFF10B981) else Color(0xFFEF4444), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (diffIsGood) "GATE MET (>20% DIFF)" else "GATE CLOSED (DIFF <= 20%)",
                                        color = if (diffIsGood) Color(0xFF10B981) else Color(0xFFEF4444),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("EVEN DIGITS (0,2,4,6,8)", color = Color.Gray, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                                    Text(String.format("%.1f%%", evenPercentage), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("ACTUAL GAP", color = Color.Gray, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                                    Text(String.format("%.1f%%", parityDiff), color = if (diffIsGood) Color(0xFF10B981) else Color(0xFFFBBF24), fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                }
                                
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text("ODD DIGITS (1,3,5,7,9)", color = Color.Gray, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                                    Text(String.format("%.1f%%", oddPercentage), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.05f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(evenPercentage.coerceAtLeast(0.1f))
                                        .background(Color(0xFF06B6D4))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(oddPercentage.coerceAtLeast(0.1f))
                                        .background(Color(0xFFF97316))
                                )
                            }
                        }
                    }
                }

                // --- Part 4: Retractable Settings Accordeon ---
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .clickable { paramsExpanded = !paramsExpanded }
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚙️ TRANSACTION PARAMETERS",
                                    color = Color.LightGray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (paramsExpanded) "COLLAPE ▲" else "EXPAND ▼",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            if (!paramsExpanded) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "STAKE SIZE: $$stakeInput USD",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "DURATION: ${userSettings.virtualTradeCloseTicks} TICKS",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            
                            if (paramsExpanded) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                
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
                                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
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
                                
                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))
                                
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("NUMBER OF TICKS TO CONTRACT CLOSURE", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val tickOptions = listOf(1, 2, 3, 5, 10)
                                        tickOptions.forEach { t ->
                                            val sel = userSettings.virtualTradeCloseTicks == t
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (sel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.04f))
                                                    .border(
                                                        width = 0.5.dp,
                                                        color = if (sel) Color.Transparent else Color.White.copy(alpha = 0.05f),
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable {
                                                        viewModel.updateSettingsInDb(userSettings.copy(virtualTradeCloseTicks = t))
                                                    }
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (t == 1) "1 TICK" else "$t TICKS",
                                                    color = if (sel) Color.White else Color.Gray,
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
                    }
                }

                // Interactive Executers
                item {
                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                val runAction = {
                                    viewModel.executeManualTrade(
                                        symbolCode = "1HZ100V",
                                        displayName = "Volatility 100 (1s) Index",
                                        contractType = "EVEN",
                                        barrier = 0,
                                        stake = stakeInput.toDoubleOrNull() ?: 5.0
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("EXECUTE BUY EVEN", fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = Color.White)
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                val runAction = {
                                    viewModel.executeManualTrade(
                                        symbolCode = "1HZ100V",
                                        displayName = "Volatility 100 (1s) Index",
                                        contractType = "ODD",
                                        barrier = 0,
                                        stake = stakeInput.toDoubleOrNull() ?: 5.0
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("EXECUTE BUY ODD", fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = Color.White)
                        }
                    }
                }

                // --- Part 5: Active List (Recent Trades) ---
                item {
                    val tradesList by viewModel.tradeHistory.collectAsState()
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📜 ACTIVE & COMPLETED RUNS Logs",
                                color = Color.LightGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            
                            if (tradesList.isEmpty()) {
                                Text(
                                    text = "NO EXECUTIONS RECORDED IN CURRENT SESSION",
                                    color = Color.Gray,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    tradesList.take(5).forEach { b ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.White.copy(alpha = 0.02f))
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(b.contractType.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                Text("STAKE: ${b.stake} | DIGIT: ${b.entryDigit}", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                            }
                                            
                                            val isPending = b.status == "PENDING"
                                            val isWin = b.status == "WIN"
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        if (isPending) Color(0xFFFBBF24).copy(alpha = 0.15f)
                                                        else if (isWin) Color(0xFF10B981).copy(alpha = 0.15f)
                                                        else Color(0xFFEF4444).copy(alpha = 0.15f)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (isPending) "PENDING" else if (isWin) "+${String.format("%.2f", b.profitLoss)}" else "-${String.format("%.2f", b.stake)}",
                                                    color = if (isPending) Color(0xFFFBBF24) else if (isWin) Color(0xFF10B981) else Color(0xFFEF4444),
                                                    fontSize = 8.sp,
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
        } else if (activeTab == "RADAR") {
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
                                text = "⚠️ NO DERIV TOKEN: Autopilot inactive. Set up secure PAT token in settings.",
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

            // --- REAL-TIME PARITY DUAL VECTOR LOOP ANALYSIS ---
            dualVectorState?.let { dvs ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0E15)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(
                            width = 1.dp,
                            color = if (dvs.entryTriggerPassed) Color(0xFF0F172A).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(16.dp)
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
                            Text(
                                text = "🌀 PARITY DUAL VECTOR LOOP ENGINE",
                                color = Color.White,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (dvs.entryTriggerPassed) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFFBBF24).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (dvs.entryTriggerPassed) "TRIGGERED ⚡" else "AWAITING SQUEEZE ⌛",
                                    color = if (dvs.entryTriggerPassed) Color(0xFF10B981) else Color(0xFFFBBF24),
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // EVENS Vector Gauge
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.02f))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("EVENS VECTOR", color = Color.Gray, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                                    if (dvs.dominantSide == "EVENS") {
                                        Text("DOMINANT", color = Color(0xFF10B981), fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f%% Density", dvs.evenVector.percentage),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    val evSign = if (dvs.evenVector.delta >= 0) "▲" else "▼"
                                    val evColor = if (dvs.evenVector.delta >= 0) Color(0xFF34D399) else Color(0xFFEF4444)
                                    Text(
                                        text = String.format(java.util.Locale.US, "%s %.1f%% Shift", evSign, dvs.evenVector.delta),
                                        color = evColor,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "Momentum: %.1f%%", dvs.evenVector.momentum),
                                    color = Color.LightGray,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                LinearProgressIndicator(
                                    progress = { dvs.evenVector.momentum / 100f },
                                    modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 2.dp).clip(CircleShape),
                                    color = Color(0xFF34D399),
                                    trackColor = Color.White.copy(alpha = 0.05f)
                                )
                            }

                            // ODDS Vector Gauge
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.02f))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("ODDS VECTOR", color = Color.Gray, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                                    if (dvs.dominantSide == "ODDS") {
                                        Text("DOMINANT", color = Color(0xFF818CF8), fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f%% Density", dvs.oddVector.percentage),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    val odSign = if (dvs.oddVector.delta >= 0) "▲" else "▼"
                                    val odColor = if (dvs.oddVector.delta >= 0) Color(0xFF34D399) else Color(0xFFEF4444)
                                    Text(
                                        text = String.format(java.util.Locale.US, "%s %.1f%% Shift", odSign, dvs.oddVector.delta),
                                        color = odColor,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "Momentum: %.1f%%", dvs.oddVector.momentum),
                                    color = Color.LightGray,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                LinearProgressIndicator(
                                    progress = { dvs.oddVector.momentum / 100f },
                                    modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 2.dp).clip(CircleShape),
                                    color = Color(0xFF818CF8),
                                    trackColor = Color.White.copy(alpha = 0.05f)
                                )
                            }
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
                if (marketChoppyBlocked) {
                    // --- RISK SAFETY VALVE ENFORCED CARD ---
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1212).copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxSize()
                            .border(width = 1.dp, color = Color(0xFFEF4444).copy(alpha = 0.2f), shape = RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Safety Block",
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "🛡️ RISK SAFETY SYSTEM ACTIVATED",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "The current volatility index is experiencing high frequency noise / flat consolidation (stability < 40%). All alert emissions and autopilot buys are locked out temporarily to satisfy the Capital Protection Directive.",
                                color = Color.LightGray,
                                fontSize = 9.5.sp,
                                lineHeight = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (entryTriggerAwaiting) {
                    // --- AWAITING ENTRY CORRELATION CARD ---
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF13141F).copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxSize()
                            .border(width = 1.dp, color = Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "AWAITING ENTRY TRIGGER SQUEEZE",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "The dual vector engine is tracking and caching parity density. Waiting to identify momentum domination cross between Evens and Odds before firing radar recommendation.",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                lineHeight = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (signal != null) {
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

            // Parse AI configuration
            val signal = activeSignal
            val isSignalForThisSymbol = signal != null && signal.symbol == targetSymbol
            
            val autoSymbol = targetSymbol
            val autoDisplayName = when(autoSymbol) {
                "1HZ10V" -> "Volatility 10 (1s) Index"
                "1HZ25V" -> "Volatility 25 (1s) Index"
                "1HZ50V" -> "Volatility 50 (1s) Index"
                "1HZ75V" -> "Volatility 75 (1s) Index"
                else -> "Volatility 100 (1s) Index"
            }
            
            val marketRankings by viewModel.marketRankings.collectAsState()
            val currentScanResult = marketRankings.firstOrNull { it.symbol == targetSymbol }
            
            val autoType = if (isSignalForThisSymbol && signal != null) {
                signal.contractType
            } else {
                currentScanResult?.recommendedContract?.split(" ")?.getOrNull(0) ?: "UNDER"
            }
            
            val autoBarrier = if (isSignalForThisSymbol && signal != null) {
                signal.barrier.toIntOrNull() ?: 5
            } else {
                currentScanResult?.recommendedContract?.split(" ")?.getOrNull(1)?.toIntOrNull() ?: 5
            }
            
            val autoConfidence = if (isSignalForThisSymbol && signal != null) {
                signal.probabilityEst
            } else {
                currentScanResult?.confidence ?: 92.5f
            }

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
                                
                                // Row 1 (digits 0-4) with animated down cursor over active digit
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    val currentTickDigit = selectedPacket?.tickHistory?.lastOrNull()
                                    
                                    // Row 1 Cursor Track
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(14.dp)
                                    ) {
                                        val hasDigitInRow1 = currentTickDigit != null && currentTickDigit in 0..4
                                        if (hasDigitInRow1) {
                                            val animatedRow1Bias by animateFloatAsState(
                                                targetValue = (currentTickDigit ?: 0) * 0.5f - 1.0f,
                                                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                                                label = "digit_cursor_row1"
                                            )
                                            
                                            Box(
                                                modifier = Modifier
                                                    .align(androidx.compose.ui.BiasAlignment(horizontalBias = animatedRow1Bias, verticalBias = 1.0f))
                                                    .width(62.dp),
                                                contentAlignment = Alignment.BottomCenter
                                            ) {
                                                Text(
                                                    text = "▼",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        for (digit in 0..4) {
                                            val pct = percentages.getOrNull(digit) ?: 10.0f
                                            val isMax = digit == maxIndex
                                            val isMin = digit == minIndex
                                            val isCurrentTick = currentTickDigit != null && digit == currentTickDigit
                                            
                                            val strokeColor = when {
                                                isCurrentTick -> MaterialTheme.colorScheme.primary
                                                isMax -> Color(0xFFF97316)
                                                isMin -> Color(0xFF06B6D4)
                                                else -> Color.White.copy(alpha = 0.1f)
                                            }
                                            val outerBorderWidth = if (isCurrentTick) 2.5.dp else 1.dp
                                            val bgColor = when {
                                                isCurrentTick -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                isMax -> Color(0xFFF97316).copy(alpha = 0.1f)
                                                isMin -> Color(0xFF06B6D4).copy(alpha = 0.1f)
                                                else -> Color.Transparent
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 62.dp, height = 62.dp)
                                                    .clip(CircleShape)
                                                    .background(bgColor)
                                                    .border(outerBorderWidth, strokeColor, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = "$digit",
                                                        color = when {
                                                            isCurrentTick -> Color.White
                                                            isMax -> Color(0xFFFBBF24)
                                                            isMin -> Color(0xFF22D3EE)
                                                            else -> Color.White
                                                        },
                                                        fontSize = 14.sp,
                                                        fontWeight = if (isCurrentTick) FontWeight.Black else FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    Text(
                                                        text = String.format("%.1f%%", pct),
                                                        color = if (isCurrentTick) Color.White.copy(alpha = 0.8f) else Color.Gray,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    if (isCurrentTick) {
                                                        Text("LIVE", color = MaterialTheme.colorScheme.primary, fontSize = 7.sp, fontWeight = FontWeight.Black)
                                                    } else if (isMax) {
                                                        Text("HOT", color = Color(0xFFF97316), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                                    } else if (isMin) {
                                                        Text("COLD", color = Color(0xFF06B6D4), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Row 2 (digits 5-9) with animated down cursor over active digit
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                    val currentTickDigit = selectedPacket?.tickHistory?.lastOrNull()
                                    
                                    // Row 2 Cursor Track
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(14.dp)
                                    ) {
                                        val hasDigitInRow2 = currentTickDigit != null && currentTickDigit in 5..9
                                        if (hasDigitInRow2) {
                                            val animatedRow2Bias by animateFloatAsState(
                                                targetValue = ((currentTickDigit ?: 5) - 5) * 0.5f - 1.0f,
                                                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                                                label = "digit_cursor_row2"
                                            )
                                            
                                            Box(
                                                modifier = Modifier
                                                    .align(androidx.compose.ui.BiasAlignment(horizontalBias = animatedRow2Bias, verticalBias = 1.0f))
                                                    .width(62.dp),
                                                contentAlignment = Alignment.BottomCenter
                                            ) {
                                                Text(
                                                    text = "▼",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        for (digit in 5..9) {
                                            val pct = percentages.getOrNull(digit) ?: 10.0f
                                            val isMax = digit == maxIndex
                                            val isMin = digit == minIndex
                                            val isCurrentTick = currentTickDigit != null && digit == currentTickDigit
                                            
                                            val strokeColor = when {
                                                isCurrentTick -> MaterialTheme.colorScheme.primary
                                                isMax -> Color(0xFFF97316)
                                                isMin -> Color(0xFF06B6D4)
                                                else -> Color.White.copy(alpha = 0.1f)
                                            }
                                            val outerBorderWidth = if (isCurrentTick) 2.5.dp else 1.dp
                                            val bgColor = when {
                                                isCurrentTick -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                isMax -> Color(0xFFF97316).copy(alpha = 0.1f)
                                                isMin -> Color(0xFF06B6D4).copy(alpha = 0.1f)
                                                else -> Color.Transparent
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 62.dp, height = 62.dp)
                                                    .clip(CircleShape)
                                                    .background(bgColor)
                                                    .border(outerBorderWidth, strokeColor, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = "$digit",
                                                        color = when {
                                                            isCurrentTick -> Color.White
                                                            isMax -> Color(0xFFFBBF24)
                                                            isMin -> Color(0xFF22D3EE)
                                                            else -> Color.White
                                                        },
                                                        fontSize = 14.sp,
                                                        fontWeight = if (isCurrentTick) FontWeight.Black else FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    Text(
                                                        text = String.format("%.1f%%", pct),
                                                        color = if (isCurrentTick) Color.White.copy(alpha = 0.8f) else Color.Gray,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    if (isCurrentTick) {
                                                        Text("LIVE", color = MaterialTheme.colorScheme.primary, fontSize = 7.sp, fontWeight = FontWeight.Black)
                                                    } else if (isMax) {
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (isCustomMode) Modifier.clickable { customConfigExpanded = !customConfigExpanded } else Modifier),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isCustomMode) "⚡ ABSOLUTE MANUAL CONTROL CONFIG" else "🤖 TACTICAL CO-PILOT PRESET ACTIVE",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isCustomMode) {
                                    Text(
                                        text = if (customConfigExpanded) "COLLAPSE ▲" else "EXPAND ▼",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            if (isCustomMode) {
                                if (customConfigExpanded) {
                                    // Manual Config Grid
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Collapsible Symbol selector
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.White.copy(alpha = 0.02f))
                                                    .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                                    .clickable { assetSelectorExpanded = !assetSelectorExpanded }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val assetName = when (targetSymbol) {
                                                    "1HZ10V" -> "Volatility 10 (1s) Index"
                                                    "1HZ25V" -> "Volatility 25 (1s) Index"
                                                    "1HZ50V" -> "Volatility 50 (1s) Index"
                                                    "1HZ75V" -> "Volatility 75 (1s) Index"
                                                    "1HZ100V" -> "Volatility 100 (1s) Index"
                                                    else -> "Volatility 100 (1s) Index"
                                                }
                                                Text("ACTIVE MARKET: $assetName", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                Text(if (assetSelectorExpanded) "CLOSE ▲" else "CHANGE ▼", color = MaterialTheme.colorScheme.primary, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                                            }

                                            if (assetSelectorExpanded) {
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
                                                                    viewModel.selectSymbol(code)
                                                                    assetSelectorExpanded = false
                                                                }
                                                                .padding(vertical = 6.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(name, color = if (sel) Color.White else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Modern contract categories sub-tabs
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("SELECT CONTRACT STYLE", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val tabs = listOf(
                                                    "MATCHES/DIFFERS" to "🎯 DIGITS",
                                                    "OVER/UNDER" to "📏 BARRIERS",
                                                    "EVEN/ODD" to "🔢 EVEN/ODD",
                                                    "RISE/FALL" to "📈 RISE/FALL",
                                                    "ACCUMULATORS" to "🔋 ACCUM",
                                                    "ASIANS" to "🌏 ASIANS"
                                                )
                                                tabs.forEach { (tabId, label) ->
                                                    val sel = customSubTab == tabId
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                                            .border(0.5.dp, if (sel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                                            .clickable { 
                                                                customSubTab = tabId
                                                                // Auto select default contract types for newly active screen
                                                                customContractType = when(tabId) {
                                                                    "MATCHES/DIFFERS" -> "MATCHES"
                                                                    "OVER/UNDER" -> "OVER"
                                                                    "EVEN/ODD" -> "EVEN"
                                                                    "RISE/FALL" -> "RISE"
                                                                    "ACCUMULATORS" -> "ACCUM"
                                                                    "ASIANS" -> "ASIANU"
                                                                    else -> "MATCHES"
                                                                }
                                                            }
                                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(label, color = if (sel) Color.White else Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                    }
                                                }
                                            }
                                        }

                                        // Render individual dedicated sub-screen container based on selected category style
                                        Card(
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.015f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(0.5.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                when(customSubTab) {
                                                    "MATCHES/DIFFERS" -> {
                                                        // Sub-screen for Digit Matches/Differs
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            listOf("MATCHES" to "🎯 MATCHES (800% PAYOUT)", "DIFFERS" to "🛡️ DIFFERS (10% PAYOUT)").forEach { (t, lbl) ->
                                                                val sel = customContractType == t
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .clip(RoundedCornerShape(6.dp))
                                                                        .background(if (sel) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Transparent)
                                                                        .border(0.5.dp, if (sel) Color(0xFF10B981) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                                                        .clickable { customContractType = t }
                                                                        .padding(vertical = 8.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(lbl, color = if (sel) Color.White else Color.Gray, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                                }
                                                            }
                                                        }

                                                        Text(
                                                            text = if (customContractType == "MATCHES") "SELECT DIGIT THAT CORRESPONDS TO THE TICK'S LAST DIGIT TO WIN"
                                                                   else "SELECT DIGIT THAT THE TICK'S LAST DIGIT MUST NOT EQUAL TO WIN",
                                                            color = Color.Gray,
                                                            fontSize = 7.5.sp,
                                                            fontFamily = FontFamily.Monospace
                                                        )

                                                        // High-density digit choice grid 0-9 with real-time percentages synced
                                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                                for(dig in 0..4) {
                                                                    val pct = percentages.getOrNull(dig) ?: 10.0f
                                                                    val sel = customBarrier == dig
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .weight(1f)
                                                                            .clip(RoundedCornerShape(6.dp))
                                                                            .background(if (sel) Color(0xFFF97316).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f))
                                                                            .border(0.5.dp, if (sel) Color(0xFFF97316) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                                                            .clickable { customBarrier = dig }
                                                                            .padding(vertical = 6.dp),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                            Text("$dig", color = if (sel) Color.White else Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                                            Text(String.format("%.1f%%", pct), color = if (sel) Color(0xFFF97316) else Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                                for(dig in 5..9) {
                                                                    val pct = percentages.getOrNull(dig) ?: 10.0f
                                                                    val sel = customBarrier == dig
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .weight(1f)
                                                                            .clip(RoundedCornerShape(6.dp))
                                                                            .background(if (sel) Color(0xFFF97316).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f))
                                                                            .border(0.5.dp, if (sel) Color(0xFFF97316) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                                                            .clickable { customBarrier = dig }
                                                                            .padding(vertical = 6.dp),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                            Text("$dig", color = if (sel) Color.White else Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                                            Text(String.format("%.1f%%", pct), color = if (sel) Color(0xFFF97316) else Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    "OVER/UNDER" -> {
                                                        // Sub-screen for Over/Under
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            listOf("OVER" to "📈 OVER", "UNDER" to "📉 UNDER").forEach { (t, lbl) ->
                                                                val sel = customContractType == t
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .clip(RoundedCornerShape(6.dp))
                                                                        .background(if (sel) Color(0xFF3B82F6).copy(alpha = 0.15f) else Color.Transparent)
                                                                        .border(0.5.dp, if (sel) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                                                        .clickable { 
                                                                            customContractType = t
                                                                            // Enforce boundary compatibility
                                                                            if (t == "OVER" && customBarrier == 9) customBarrier = 8
                                                                            if (t == "UNDER" && customBarrier == 0) customBarrier = 1
                                                                        }
                                                                        .padding(vertical = 8.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(lbl, color = if (sel) Color.White else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                                }
                                                            }
                                                        }

                                                        val title = if (customContractType == "OVER") "SELECT BARRIER (WIN IF LAST DIGIT > BARRIER)" else "SELECT BARRIER (WIN IF LAST DIGIT < BARRIER)"
                                                        Text(title, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                                                        val start = if (customContractType == "UNDER") 1 else 0
                                                        val end = if (customContractType == "OVER") 8 else 9
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
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
                                                    "EVEN/ODD" -> {
                                                        // Sub-screen for Even/Odd digital modes
                                                        Text("CONTRACT PREDICTION TARGET", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            val modes = listOf("EVEN" to "🔢 EVEN TICK LAST DIGIT", "ODD" to "🔢 ODD TICK LAST DIGIT")
                                                            modes.forEach { (t, lbl) ->
                                                                val sel = customContractType == t
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .clip(RoundedCornerShape(6.dp))
                                                                        .background(if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                                                        .border(0.5.dp, if (sel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                                                        .clickable { customContractType = t }
                                                                        .padding(vertical = 12.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                        Text(t, color = if (sel) Color.White else Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                                                        Text(lbl, color = Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    "RISE/FALL" -> {
                                                        // Sub-screen for Standard digital Call/Put Rise/Fall options
                                                        Text("MARKET BIAS DIRECTION EXECUTOR", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            // Rise/Call option
                                                            val isRise = customContractType == "RISE" || customContractType == "CALL"
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(if (isRise) Color(0xFF10B981).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                                                    .border(0.5.dp, if (isRise) Color(0xFF10B981) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                                                    .clickable { customContractType = "RISE" }
                                                                    .padding(vertical = 12.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                    Text("▲ RISE / CALL", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Black)
                                                                    Text("Payout ~95%", color = Color.Gray, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                                                                }
                                                            }

                                                            // Fall/Put option
                                                            val isFall = customContractType == "FALL" || customContractType == "PUT"
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(if (isFall) Color(0xFFEF4444).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                                                    .border(0.5.dp, if (isFall) Color(0xFFEF4444) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                                                    .clickable { customContractType = "FALL" }
                                                                    .padding(vertical = 12.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                    Text("▼ FALL / PUT", color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Black)
                                                                    Text("Payout ~95%", color = Color.Gray, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    "ACCUMULATORS" -> {
                                                        // Sub-screen for Accumulators
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
                                                        Text(
                                                            text = "💡 Grows exponentially on every tick unless it breaches boundary limit. Higher growth rate equals faster yield but tighter crash margins.",
                                                            color = Color.Gray,
                                                            fontSize = 7.5.sp,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                    "ASIANS" -> {
                                                        // Sub-screen for Asians contracts
                                                        Text("SELECT ASIAN BOUNDARY DIRECTION", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            val isAsianU = customContractType == "ASIANU"
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(if (isAsianU) Color(0xFF3B82F6).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                                                    .border(0.5.dp, if (isAsianU) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                                                    .clickable { customContractType = "ASIANU" }
                                                                    .padding(vertical = 12.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                    Text("🌏 ASIAN UP", color = Color(0xFF60A5FA), fontSize = 11.sp, fontWeight = FontWeight.Black)
                                                                    Text("Avg tick > entry", color = Color.Gray, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                                                                }
                                                            }

                                                            val isAsianD = customContractType == "ASIAND"
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(if (isAsianD) Color(0xFF3B82F6).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                                                    .border(0.5.dp, if (isAsianD) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                                                    .clickable { customContractType = "ASIAND" }
                                                                    .padding(vertical = 12.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                    Text("🌏 ASIAN DOWN", color = Color(0xFF3B82F6), fontSize = 11.sp, fontWeight = FontWeight.Black)
                                                                    Text("Avg tick < entry", color = Color.Gray, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                                                                }
                                                            }
                                                        }
                                                        Text(
                                                            text = "💡 Asian contracts compare the average of all ticks in the duration against the starting or ending reference tick.",
                                                            color = Color.Gray,
                                                            fontSize = 7.5.sp,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Retracted Custom Config Summary
                                    val assetName = when (targetSymbol) {
                                        "1HZ10V" -> "V10 (1s)"
                                        "1HZ25V" -> "V25 (1s)"
                                        "1HZ50V" -> "V50 (1s)"
                                        "1HZ75V" -> "V75 (1s)"
                                        "1HZ100V" -> "V100 (1s)"
                                        else -> "V100 (1s)"
                                    }
                                    val bTitle = when (customContractType) {
                                        "UNDER" -> " | BARRIER: Under $customBarrier"
                                        "OVER" -> " | BARRIER: Over $customBarrier"
                                        "MATCHES" -> " | DIGIT: $customBarrier"
                                        "DIFFERS" -> " | EXCLUDE: $customBarrier"
                                        "ACCUM" -> " | GROWTH: $customBarrier%"
                                        else -> ""
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.02f))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$assetName | $customContractType$bTitle",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
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
                                        Text(String.format("%.1f%%", autoConfidence), color = Color(0xFF818CF8), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .clickable { paramsExpanded = !paramsExpanded }
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("⚙️ TRANSACTION CONFIG (RETRACTABLE)", color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Text(if (paramsExpanded) "COLLAPSE ▲" else "EXPAND ▼", color = MaterialTheme.colorScheme.primary, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                                    }

                                    if (!paramsExpanded) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("STAKE: $$stakeInput USD", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            Text("CLOSURE: ${userSettings.virtualTradeCloseTicks} TICKS", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                    }

                                    if (paramsExpanded) {
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

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
                                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
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

                                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))

                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("NUMBER OF TICKS TO CONTRACT CLOSURE", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                val tickOptions = listOf(1, 2, 3, 5, 10)
                                                tickOptions.forEach { t ->
                                                    val sel = userSettings.virtualTradeCloseTicks == t
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(if (sel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.04f))
                                                            .border(
                                                                width = 0.5.dp,
                                                                color = if (sel) Color.Transparent else Color.White.copy(alpha = 0.05f),
                                                                shape = RoundedCornerShape(6.dp)
                                                            )
                                                            .clickable {
                                                                viewModel.updateSettingsInDb(userSettings.copy(virtualTradeCloseTicks = t))
                                                            }
                                                            .padding(vertical = 10.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = if (t == 1) "1 TICK" else "$t TICKS",
                                                            color = if (sel) Color.White else Color.Gray,
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
