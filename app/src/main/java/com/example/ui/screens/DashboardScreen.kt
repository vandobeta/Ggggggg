package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CompleteDataPacket
import com.example.data.MarketScanResult
import com.example.ui.theme.getDigitColor
import com.example.ui.theme.getQuadrantColor
import com.example.ui.viewmodel.DigitAnalysisViewModel
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DigitAnalysisViewModel,
    onNavigateToPredictions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val pingState by viewModel.pingState.collectAsState()
    val marketRankings by viewModel.marketRankings.collectAsState()
    val selectedSymbol by viewModel.selectedSymbol.collectAsState()
    val activePacket by viewModel.selectedPacket.collectAsState()
    var showAiAdvisorSheet by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F0F13), Color(0xFF050507))
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- INDUSTRIAL HEADER TERMINAL ---
        HeaderWidget(
            connectionState = connectionState,
            pingState = pingState,
            onReconnect = { viewModel.reconnect() },
            onOpenAiAdvisor = { showAiAdvisorSheet = true }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- LIVE VOLATILITY RADAR MULTI-SCANNER ---
            Text(
                text = "LIVE VOLATILITY MULTI-SCANNER RATING MATRIX",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 2.dp)
            )

            MultiMarketScannerRow(
                marketRankings = marketRankings,
                selectedSymbol = selectedSymbol,
                onSymbolSelected = { viewModel.selectSymbol(it) }
            )

            // --- AI CO-PILOT ADVISOR INLINE BANNER ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { showAiAdvisorSheet = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🪄", fontSize = 20.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI QUANT CO-PILOT ADVISOR",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Evaluate connection health, live scanners, trade ratios & get actionable parameter recommendations.",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Open advisor",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // --- DETAILED ANALYSIS PACKET VIEWER ---
            if (activePacket != null) {
                DetailedPacketPanel(
                    packet = activePacket!!,
                    viewModel = viewModel,
                    onNavigateToPredictions = onNavigateToPredictions
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ESTABLISHING CHANNELS...",
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // --- TRADER'S PERSONAL PROFILE and REAL-TIME PRACTICE ENGINE LOGS ---
            val userSettings by viewModel.userSettings.collectAsState()
            val practiceBets by viewModel.practiceBets.collectAsState()

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Trader profile summary Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TRADER ACCOUNT PROFILE",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (userSettings.traderName.isNotBlank()) userSettings.traderName.uppercase() else "DEMO TRADER",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "STARTING BALANCE",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${String.format("%.2f", userSettings.capital)} ${userSettings.currency}",
                                color = Color(0xFF10B981),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (userSettings.goal.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎯 MISSION GOAL: ",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = userSettings.goal,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )

                    // Practice History Metrics
                    val totalBets = practiceBets.size
                    val totalWins = practiceBets.count { it.isWin }
                    val totalLosses = totalBets - totalWins
                    val winRate = if (totalBets > 0) (totalWins.toFloat() / totalBets.toFloat() * 100f) else 0f

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("PRACTICE TRADES", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Text("$totalBets", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("WINS / LOSSES", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Text("🟢 $totalWins / 🔴 $totalLosses", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("ALERT WIN RATE", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = if (totalBets > 0) String.format("%.1f%%", winRate) else "N/A",
                                color = if (winRate >= 70f) Color(0xFF10B981) else if (winRate >= 50f) Color(0xFFFBBF24) else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.triggerMorningEncouragement() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.08f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🌅 MOTIVATE ME NOW", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        if (totalBets > 0) {
                            Button(
                                onClick = { viewModel.clearPracticeBets() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                                    contentColor = Color(0xFFEF4444)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🧹 CLEAR LOGS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (practiceBets.isNotEmpty()) {
                        Text(
                            text = "PERSISTENT NOTIFICATION LOGS (REVIEW FEED)",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            practiceBets.take(6).forEach { bet ->
                                val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                                val timeStr = timeFormat.format(java.util.Date(bet.timestamp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(6.dp))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (bet.isWin) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (bet.isWin) "WIN" else "LOSS",
                                                color = if (bet.isWin) Color(0xFF10B981) else Color(0xFFEF4444),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = bet.signalDescription,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 1,
                                            modifier = Modifier.widthIn(max = 200.dp)
                                        )
                                    }

                                    Text(
                                        text = timeStr,
                                        color = Color.LightGray.copy(alpha = 0.4f),
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.01f), RoundedCornerShape(8.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Awaiting trade entries from notification alerts.\nWhenever a signal occurs, tap WIN/LOSS buttons in the notification to review your real-world performance statistics here.",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAiAdvisorSheet) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAiAdvisorSheet = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(12.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1015))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🤖 AI CO-PILOT INTEL",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            val userSettings by viewModel.userSettings.collectAsState()
                            Text(
                                text = "Provider: ${userSettings.aiProvider.uppercase()}",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        IconButton(
                            onClick = { showAiAdvisorSheet = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text("✕", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)).padding(vertical = 12.dp))

                    val userSettings by viewModel.userSettings.collectAsState()
                    if (userSettings.aiToken.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⚠️", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "AI API KEY NOT CONFIGURED",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Please enter your Gemini API Key or OpenAI API Key in the settings (Tuning Keys panel) to run trading advisory diagnostics.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        val advisory by viewModel.aiAdvisoryState.collectAsState()
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            if (advisory == null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🤖", fontSize = 48.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "READY FOR DIAGNOSTICS",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Click below to digest all metrics context, logs, and trade history into the AI model for optimization.",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            } else if (advisory!!.isLoading) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "AI ADVISOR IS THINKING...",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Synthesizing full application context",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            } else if (advisory!!.error != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("❌", fontSize = 44.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "DIAGNOSTIC ADVISORY FAILURE",
                                        color = Color(0xFFEF4444),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = advisory!!.error!!,
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(Color.White.copy(alpha = 0.01f), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    val reportScroll = rememberScrollState()
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(reportScroll)
                                    ) {
                                        advisory!!.text.split("\n").forEach { line ->
                                            val trimmed = line.trim()
                                            val isHeader = trimmed.startsWith("#") || trimmed.startsWith("**") && trimmed.endsWith("**")
                                            val isListItem = trimmed.startsWith("-") || trimmed.startsWith("*")
                                            
                                            val fontSize = if (trimmed.startsWith("###")) 12.sp else if (trimmed.startsWith("##")) 14.sp else if (trimmed.startsWith("#")) 16.sp else 11.sp
                                            val fontWeight = if (isHeader) FontWeight.Black else FontWeight.Normal
                                            val fontColor = if (isHeader) MaterialTheme.colorScheme.primary else if (trimmed.contains("⚠️") || trimmed.contains("WARNING")) Color(0xFFF59E0B) else Color.White
                                            val paddingBottom = if (isHeader) 6.dp else 4.dp
                                            
                                            val displayText = if (isListItem) "• " + trimmed.substring(1).trim() else trimmed
                                            
                                            if (displayText.isNotEmpty()) {
                                                Text(
                                                    text = displayText,
                                                    fontSize = fontSize,
                                                    fontWeight = fontWeight,
                                                    color = fontColor,
                                                    fontFamily = if (isHeader) FontFamily.Monospace else FontFamily.Default,
                                                    lineHeight = 16.sp,
                                                    modifier = Modifier.padding(bottom = paddingBottom)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (userSettings.aiToken.isNotEmpty()) {
                        val advisory by viewModel.aiAdvisoryState.collectAsState()
                        Button(
                            onClick = { viewModel.queryAiAdvisor() },
                            enabled = advisory?.isLoading != true,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (advisory?.isLoading == true) "RETRIEVING KNOWLEDGE PACK..." else "🪄 RUN DIAGNOSTICS & ADVISE ME",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderWidget(
    connectionState: String,
    pingState: Long,
    onReconnect: () -> Unit,
    onOpenAiAdvisor: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                connectionState.contains("CONNECTING") -> Color(0xFFEAB308)
                                connectionState.contains("CONNECTED") || connectionState.contains("ACTIVE") -> Color(0xFF22C55E)
                                else -> Color(0xFFEF4444)
                            }
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "ALGO-RADAR TERMINAL v1.1",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = connectionState.uppercase(),
                        color = if (connectionState.contains("ACTIVE") || connectionState.contains("CONNECTED")) Color(0xFF22C55E) else Color.LightGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable { onOpenAiAdvisor() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🤖 AI ADVISOR",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = "RTT: ${pingState}ms",
                        color = if (pingState < 150) Color(0xFF22C55E) else Color(0xFFF59E0B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onReconnect,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("reconnect_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reconnect stream",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MultiMarketScannerRow(
    marketRankings: List<MarketScanResult>,
    selectedSymbol: String,
    onSymbolSelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(marketRankings) { rank ->
            val isSelected = rank.symbol == selectedSymbol
            val backgroundGradient = if (isSelected) {
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF2563EB).copy(alpha = 0.45f), Color(0xFF1D4ED8).copy(alpha = 0.2f))
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E1E24).copy(alpha = 0.8f), Color(0xFF121216).copy(alpha = 0.8f))
                )
            }

            Box(
                modifier = Modifier
                    .width(136.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundGradient)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null, // Disable default indication to avoid ripple deprecated warning conflicts
                        onClick = { onSymbolSelected(rank.symbol) }
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("market_card_${rank.symbol}")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rank.symbol.replace("1HZ", "").replace("V", ""),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )

                        if (rank.totalEdgeScore > 65f) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFBBF24))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = rank.displayName.replace(" (1S)", " 1S"),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "EDGE",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f", rank.totalEdgeScore),
                                color = when {
                                    rank.totalEdgeScore >= 70f -> Color(0xFFEF4444)
                                    rank.totalEdgeScore >= 50f -> Color(0xFFF59E0B)
                                    else -> Color(0xFF38BDF8)
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "PRIME",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "D:${rank.primePredictionDigit}",
                                color = Color(0xFF10B981),
                                fontSize = 12.sp,
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

@Composable
fun DetailedPacketPanel(
    packet: CompleteDataPacket,
    viewModel: DigitAnalysisViewModel,
    onNavigateToPredictions: () -> Unit
) {
    val animateMomentum by animateFloatAsState(targetValue = packet.momentumScore, animationSpec = tween(500), label = "momentum")
    val animateNoise by animateFloatAsState(targetValue = packet.noiseScore, animationSpec = tween(500), label = "noise")
    val animateStability by animateFloatAsState(targetValue = packet.stabilityScore, animationSpec = tween(500), label = "stability")

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- ACTIVE RADAR TARGET BRIEFING ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "MONITORING CHANNEL",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = packet.displayName.uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "LAST TICK VALUE",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val formattedPrice = String.format(Locale.US, "%.3f", packet.lastTickValue)
                    val basePrice = if (formattedPrice.length > 1) formattedPrice.substring(0, formattedPrice.length - 1) else formattedPrice
                    val lastPriceChar = if (formattedPrice.isNotEmpty()) formattedPrice.substring(formattedPrice.length - 1) else ""

                    Text(
                        text = basePrice,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = lastPriceChar,
                        color = Color(0xFFEF4444),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // --- THE 4-QUADRANT DIGIT MATRIX CLUSTER ---
        Text(
            text = "4-QUADRANT ACCUMULATIVE DENSITY MATRIX CLUSTER",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        // CENTRAL RADAR DIAL TRANSITION POINTER POINTS TO "NEXT POSSIBLE QUADRANT"
        val targetQuadrant = packet.predictionsList.firstOrNull()?.quadrant ?: "LOWER EVEN"

        // Define delicate flags: if quadrant matches top target, or contains high density predictions over threshold
        val isLoGlowing = (targetQuadrant == "LOWER ODD") || packet.predictionsList.any { it.quadrant == "LOWER ODD" && it.confidence >= 35f }
        val isLeGlowing = (targetQuadrant == "LOWER EVEN") || packet.predictionsList.any { it.quadrant == "LOWER EVEN" && it.confidence >= 35f }
        val isHoGlowing = (targetQuadrant == "HIGHER ODD") || packet.predictionsList.any { it.quadrant == "HIGHER ODD" && it.confidence >= 35f }
        val isHeGlowing = (targetQuadrant == "HIGHER EVEN") || packet.predictionsList.any { it.quadrant == "HIGHER EVEN" && it.confidence >= 35f }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuadrantCard(
                        title = "LOWER ODD",
                        digits = listOf(1, 3),
                        pct = packet.quadWeights["LO"] ?: 0f,
                        breakdowns = packet.digitBreakdowns,
                        packetHistorySize = packet.tickHistory.size,
                        isGlowing = isLoGlowing
                    )

                    QuadrantCard(
                        title = "HIGHER ODD",
                        digits = listOf(5, 7, 9),
                        pct = packet.quadWeights["HO"] ?: 0f,
                        breakdowns = packet.digitBreakdowns,
                        packetHistorySize = packet.tickHistory.size,
                        isGlowing = isHoGlowing
                    )
                }

                // Right Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuadrantCard(
                        title = "LOWER EVEN",
                        digits = listOf(0, 2, 4),
                        pct = packet.quadWeights["LE"] ?: 0f,
                        breakdowns = packet.digitBreakdowns,
                        packetHistorySize = packet.tickHistory.size,
                        isGlowing = isLeGlowing
                    )

                    QuadrantCard(
                        title = "HIGHER EVEN",
                        digits = listOf(6, 8),
                        pct = packet.quadWeights["HE"] ?: 0f,
                        breakdowns = packet.digitBreakdowns,
                        packetHistorySize = packet.tickHistory.size,
                        isGlowing = isHeGlowing
                    )
                }
            }

            val targetAngle = when (targetQuadrant) {
                "LOWER ODD" -> -135f   // Top-Left
                "HIGHER ODD" -> 135f   // Bottom-Left
                "LOWER EVEN" -> -45f   // Top-Right
                "HIGHER EVEN" -> 45f   // Bottom-Right
                else -> 0f
            }

            val rotateAngle by animateFloatAsState(
                targetValue = targetAngle,
                animationSpec = tween(600),
                label = "quadrantNeedleRotation"
            )

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF14141A).copy(alpha = 0.95f))
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), CircleShape)
                    .border(3.dp, Color.Black, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Glow/Center Core
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFBBF24))
                )

                // Render vector Needle Pointer onto a Canvas rotating seamlessly
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = rotateAngle + 90f) // +90 aligns standard 0 to vertical UP
                ) {
                    val path = Path().apply {
                        moveTo(size.width / 2f, size.height * 0.15f) // Tip pointing up
                        lineTo(size.width * 0.42f, size.height * 0.48f) // Left flange
                        lineTo(size.width / 2f, size.height * 0.42f) // Inner spine recess
                        lineTo(size.width * 0.58f, size.height * 0.48f) // Right flange
                        close()
                    }
                    drawPath(path = path, color = Color(0xFFFBBF24))
                }
            }
        }

        // --- TIMELINE SAMPLER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F1216))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(90.dp)) {
                Text(
                    text = "TICK FLOW",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "SLIDING STREAM",
                    color = Color.Gray,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val fullList = packet.tickHistory
                val countToShow = 11
                val offsetList = fullList.takeLast(countToShow)
                
                offsetList.forEachIndexed { idx, t ->
                    val stepsFromNewest = (offsetList.size - 1) - idx
                    val tailOpacity = (1.0f - (stepsFromNewest * 0.08f)).coerceIn(0.25f, 1.0f)
                    val bubbleSize = (26f - (stepsFromNewest * 0.8f)).coerceAtLeast(16f).dp
                    val fontSize = (11f - (stepsFromNewest * 0.3f)).coerceAtLeast(8f).sp
                    val borderThickness = if (stepsFromNewest == 0) 1.5.dp else 0.8.dp
                    
                    val digitColor = getDigitColor(t)
                    
                    Box(
                        modifier = Modifier
                            .size(bubbleSize)
                            .clip(CircleShape)
                            .background(digitColor.copy(alpha = 0.1f * tailOpacity))
                            .border(borderThickness, digitColor.copy(alpha = tailOpacity), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = t.toString(),
                            color = digitColor.copy(alpha = tailOpacity),
                            fontSize = fontSize,
                            fontWeight = if (stepsFromNewest == 0) FontWeight.Black else FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                
                if (packet.tickHistory.size < 100) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "SYNC:${packet.tickHistory.size}%",
                        color = Color(0xFFFBBF24),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- TACTICAL MECHANICAL GAUGES ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111116))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "info",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "INDUSTRIAL STRATEGY COMPASS METERS",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // GAUGE 1: MOMENTUM SPEEDOMETER
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "MOMENTUM VELOCITY ENGINE",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${packet.momentumScore.toInt()}%",
                        color = Color(0xFF3B82F6),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { animateMomentum / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF3B82F6),
                    trackColor = Color(0xFF3B82F6).copy(alpha = 0.1f)
                )
            }

            // GAUGE 2: NOISE TRANSITION FILTER
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "BRIDGE NOISE TRANSITION FILTER",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (packet.noiseScore > 50f) "BURST CODES" else "FILTER CLEAR",
                        color = if (packet.noiseScore > 50f) Color(0xFFEF4444) else Color(0xFF10B981),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { animateNoise / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (packet.noiseScore > 50f) Color(0xFFEF4444) else Color(0xFF10B981),
                    trackColor = Color.White.copy(alpha = 0.05f)
                )
            }

            // GAUGE 3: CORE VARIANCE STABILITY
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "MARKET VARIANCE STABILITY INDEX",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${packet.stabilityScore.toInt()}%",
                        color = Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { animateStability / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFFBBF24),
                    trackColor = Color(0xFFFBBF24).copy(alpha = 0.1f)
                )
            }
        }

        // --- HIGH-FREQUENCY LIVE DEBUG ANALYZER PANEL ---
        val unifiedState by viewModel.unifiedTickState.collectAsState()
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F101A))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Header row
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
                                .background(if (packet.isStableConnection) Color(0xFF10B981) else Color(0xFFEF4444))
                        )
                        Text(
                            text = "HF GATEKEEPER DEBUGGER",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.White.copy(alpha = 0.05f)
                    ) {
                        Text(
                            text = unifiedState?.globalRegime ?: "INITIALIZING",
                            color = Color.LightGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.04f))

                // Stability score numeric/visual
                val stabilityFloat = unifiedState?.stabilityScore ?: 0f
                val isApproved = stabilityFloat >= 40.0f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = String.format(Locale.US, "ALGORITHMIC MARKET STABILITY: %.1f%%", stabilityFloat),
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isApproved) "HIGH CONVICTION: GATE PASSED" else "CHOPPY DEAD ZONE: LOCKED OUT",
                            color = if (isApproved) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isApproved) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isApproved) "SAFE 🛡️" else "CHOPPY 🛑",
                            color = if (isApproved) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { (stabilityFloat / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = if (isApproved) Color(0xFF10B981) else Color(0xFFEF4444),
                    trackColor = Color.White.copy(alpha = 0.05f)
                )

                // Render Risky vs Safer tracks
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Risky (Sniper Mode)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.02f))
                            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎯 RISKY (SNIPER)",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (unifiedState?.riskyProfile?.isSafeToExecute == true) Color(0xFF10B981) else Color(0xFFEF4444))
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val track = unifiedState?.riskyProfile
                        Text(
                            text = "CONTRACT: ${track?.contractType ?: "N/A"}",
                            color = Color.LightGray,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "BARRIER: ${track?.barrierParameter ?: "N/A"}",
                            color = Color.LightGray,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "PAYOUT: ${track?.brokerPayoutPct ?: "0%"}",
                            color = Color(0xFF10B981),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Less Risky (Safety Net)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.02f))
                            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🛡️ SAFER (NET)",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (unifiedState?.saferProfile?.isSafeToExecute == true) Color(0xFF10B981) else Color(0xFFEF4444))
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val track = unifiedState?.saferProfile
                        Text(
                            text = "CONTRACT: ${track?.contractType ?: "N/A"}",
                            color = Color.LightGray,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "BARRIER: ${track?.barrierParameter ?: "N/A"}",
                            color = Color.LightGray,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "PAYOUT: ${track?.brokerPayoutPct ?: "0%"}",
                            color = Color(0xFF10B981),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // --- MAIN PREDICTIONS ROUTER BUTTON ---
        Button(
            onClick = onNavigateToPredictions,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("view_predictions_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2563EB),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "➔ DECODE PREDICTIONS TARGET MATRIX & BACKLOGS",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun QuadrantCard(
    title: String,
    digits: List<Int>,
    pct: Float,
    breakdowns: IntArray,
    packetHistorySize: Int,
    isGlowing: Boolean = false
) {
    val sizeBase = if (packetHistorySize > 0) packetHistorySize.toFloat() else 1f
    val animatePct by animateFloatAsState(targetValue = pct, animationSpec = tween(500), label = "quadrantPct")
    val quadColor = getQuadrantColor(title)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.04f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF101014))
            .drawBehind {
                if (isGlowing) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(quadColor.copy(alpha = glowAlpha), Color.Transparent),
                            center = center,
                            radius = size.width * 0.8f
                        )
                    )
                }
            }
            .border(
                width = if (isGlowing) 2.dp else 1.dp,
                color = if (isGlowing) quadColor.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    if (isGlowing) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(quadColor)
                        )
                    }
                    Text(
                        text = title,
                        color = if (isGlowing) quadColor else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = String.format(Locale.US, "%.1f%%", pct),
                    color = if (isGlowing) quadColor else if (pct >= 35f) Color(0xFFEF4444) else if (pct <= 15f) Color(0xFF10B981) else Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            LinearProgressIndicator(
                progress = { animatePct / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape),
                color = if (isGlowing) quadColor else if (pct >= 35f) Color(0xFFEF4444) else if (pct <= 15f) Color(0xFF10B981) else Color(0xFF2563EB),
                trackColor = Color.White.copy(alpha = 0.02f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                digits.forEach { d ->
                    val dCount = breakdowns[d]
                    val dPct = (dCount.toFloat() / sizeBase) * 100f
                    val digitColor = getDigitColor(d)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(digitColor.copy(alpha = 0.12f))
                                .border(
                                    width = 1.5.dp,
                                    color = digitColor,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = d.toString(),
                                color = digitColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = String.format(Locale.US, "%.0f%%", dPct),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
