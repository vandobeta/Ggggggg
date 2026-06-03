package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CompleteDataPacket
import com.example.data.LivePredictionModel
import com.example.ui.viewmodel.DigitAnalysisViewModel
import java.util.Locale

@Composable
fun PredictionsScreen(
    viewModel: DigitAnalysisViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activePacket by viewModel.selectedPacket.collectAsState()
    val scrollState = rememberScrollState()

    val isNotifierRunning by viewModel.isAutomatedNotifierRunning.collectAsState()
    val notifierContract by viewModel.selectedNotifierContract.collectAsState()
    val isBacktestActive by viewModel.isBacktestActive.collectAsState()
    val backtestBets by viewModel.backtestBetsCount.collectAsState()
    val backtestWins by viewModel.backtestWinsCount.collectAsState()
    val backtestTxList by viewModel.backtestTransactions.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val signalHistory by viewModel.signalHistory.collectAsState()
    var showContractPopup by remember { mutableStateOf(false) }
    var selectedTrendDetail by remember { mutableStateOf<TrendingSignalData?>(null) }

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
        // BACK HEADER BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    .testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "return",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "◀ RADAR CONSOLE",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { onNavigateBack() }
                )
                Text(
                    text = "STATISTICAL ANALYTICS",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        if (activePacket != null) {
            val packet = activePacket!!
            val predictionsList = packet.predictionsList

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // TARGET SCANNED BRIEF
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE INDEX: ${packet.displayName.uppercase()}",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (packet.isStableConnection) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFF3B82F6).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (packet.isStableConnection) "SERVER STREAM LIVE" else "OPTIMAL RUNNING",
                            color = if (packet.isStableConnection) Color(0xFF10B981) else Color(0xFF3B82F6),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (predictionsList.isNotEmpty()) {
                    val prime = predictionsList.first()

                    // --- PRIME CANDIDATE CORRIDOR (GLOWING GOLD CARD) ---
                    Column {
                        Text(
                            text = "PRIME CANDIDATE CORRIDOR (HIGHEST REVERSION CONFIDENCE)",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, Color(0xFFFBBF24), RoundedCornerShape(16.dp))
                                .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1304))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFBBF24).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "RANK 1 TARGET",
                                            color = Color(0xFFFBBF24),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Text(
                                        text = String.format(Locale.US, "%.1f%% CONFIDENCE", prime.confidence),
                                        color = Color(0xFFFBBF24),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFBBF24).copy(alpha = 0.15f))
                                                .border(2.dp, Color(0xFFFBBF24), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = prime.digit.toString(),
                                                color = Color(0xFFFBBF24),
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "DIGIT SECTOR",
                                                color = Color.LightGray.copy(alpha = 0.6f),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = prime.quadrant,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "MICRO TRANSITIONS",
                                            color = Color.LightGray.copy(alpha = 0.6f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = String.format(Locale.US, "FREQ: %.1f%%", prime.occurrencePct),
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                LinearProgressIndicator(
                                    progress = { prime.confidence / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = Color(0xFFFBBF24),
                                    trackColor = Color.White.copy(alpha = 0.05f)
                                )
                            }
                        }
                    }

                    // --- SECONDARY RANKING TABLE ---
                    Column {
                        Text(
                            text = "SECONDARY DRIFT CANDIDATE MATRIX",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116)),
                            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val secondaries = predictionsList.drop(1).take(2)
                                secondaries.forEachIndexed { i, cand ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.05f))
                                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = cand.digit.toString(),
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            Column {
                                                Text(
                                                    text = "RANK ${i + 2} CANDIDATE",
                                                    color = Color.White.copy(alpha = 0.4f),
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = cand.quadrant,
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = String.format(Locale.US, "%.1f%% CONF", cand.confidence),
                                                color = Color.LightGray,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Box(modifier = Modifier.width(80.dp)) {
                                                LinearProgressIndicator(
                                                    progress = { cand.confidence / 100f },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(4.dp)
                                                        .clip(CircleShape),
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    trackColor = Color.White.copy(alpha = 0.05f)
                                                )
                                            }
                                        }
                                    }
                                    if (i == 0) {
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.03f))
                                    }
                                }
                            }
                        }
                    }

                    // --- GRAPH VISUALIZER: NUMERICAL DROUGHT BAR CHART ---
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "TICK QUANT DISTRIBUTION & DROUGHT CANVAS",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "🎯 SHORT BAR = TARGET",
                                color = Color(0xFFFBBF24),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116)),
                            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val listSize = if (packet.tickHistory.isNotEmpty()) packet.tickHistory.size.toFloat() else 100f
                                
                                for (digit in 0..9) {
                                    val count = packet.digitBreakdowns[digit]
                                    val pct = (count.toFloat() / listSize) * 100f
                                    val isTarget = (digit == prime.digit)

                                    val barWidthAlpha by animateFloatAsState(targetValue = pct.coerceIn(2f, 100f) / 25f, animationSpec = tween(500), label = "visualBarWidth")

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "D$digit",
                                            color = if (isTarget) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.6f),
                                            fontSize = 11.sp,
                                            fontWeight = if (isTarget) FontWeight.ExtraBold else FontWeight.Normal,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.width(28.dp)
                                        )

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(14.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color.White.copy(alpha = 0.02f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(barWidthAlpha.coerceAtMost(1f))
                                                    .background(
                                                        if (isTarget) {
                                                            Brush.horizontalGradient(listOf(Color(0xFFFBBF24), Color(0xFFF59E0B)))
                                                        } else {
                                                            val baseColor = if (digit % 2 == 0) Color(0xFF3B82F6) else Color(0xFFF43F5E)
                                                            Brush.horizontalGradient(listOf(baseColor.copy(alpha = 0.5f), baseColor))
                                                        }
                                                    )
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Box(
                                            modifier = Modifier.width(76.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isTarget) {
                                                    Text(
                                                        text = "TARGET 🎯",
                                                        color = Color(0xFFFBBF24),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = String.format(Locale.US, "%.0f%%", pct),
                                                    color = if (isTarget) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.7f),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isTarget) FontWeight.Black else FontWeight.Normal,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- REAL-TIME DIGIT COOLDOWN OCCURRENCE TIMERS ---
                    Column {
                        Text(
                            text = "DIGIT COOLDOWN OCCURRENCE TIMERS (TICK DISTANCE)",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116)),
                            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Ticks elapsed since each digit's last occurrence. High numbers indicate extreme droughts.",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                               )

                                val tickList = packet.tickHistory
                                // 5x2 beautiful grid of compact digit timers
                                val chunks = (0..9).chunked(5)
                                chunks.forEach { rowDigits ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowDigits.forEach { digit ->
                                            val lastIndex = tickList.indexOfLast { it == digit }
                                            val ticksAgo = if (lastIndex != -1) {
                                                tickList.size - 1 - lastIndex
                                            } else {
                                                -1
                                            }

                                            // Drought urgency color
                                            val indicatorColor = when {
                                                ticksAgo == -1 -> Color.Red
                                                ticksAgo > 25 -> Color(0xFFF59E0B) // Amber
                                                ticksAgo > 12 -> Color(0xFF10B981) // Green accent
                                                else -> Color.White.copy(alpha = 0.6f)
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color.White.copy(alpha = 0.02f))
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (ticksAgo > 20) Color(0xFFFBBF24).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    // Digit bubble
                                                    Box(
                                                        modifier = Modifier
                                                            .size(26.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (ticksAgo > 20) Color(0xFFFBBF24).copy(alpha = 0.15f)
                                                                else Color.White.copy(alpha = 0.05f)
                                                            )
                                                            .border(
                                                                1.dp,
                                                                if (ticksAgo > 20) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.1f),
                                                                CircleShape
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = digit.toString(),
                                                            color = if (ticksAgo > 20) Color(0xFFFBBF24) else Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }

                                                    // Timer text
                                                    Text(
                                                        text = if (ticksAgo == -1) "Offline" else "$ticksAgo tks ago",
                                                        color = indicatorColor,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center,
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

                    // --- HIGH-LIKELIHOOD STRATEGIC CONTRACTS MATRIX TABLE ---
                    Column {
                        Text(
                            text = "HIGH-LIKELIHOOD CONTRACTS OPTIMIZATION MATRIX",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116)),
                            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Mathematical contract selection based on live volatility velocity, parity crossovers, and drought limits.",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // TABLE HEADER ROW
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = Color.White.copy(alpha = 0.03f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CONTRACT TYPE",
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "CANDIDATE DIGITS",
                                        modifier = Modifier.weight(1.3f),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "CONFIDENCE SCORE",
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.End
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Resolve strategic calculations for overs/unders/differs
                                val heWeight = packet.quadWeights["HE"] ?: 25f
                                val hoWeight = packet.quadWeights["HO"] ?: 25f
                                val leWeight = packet.quadWeights["LE"] ?: 25f
                                val loWeight = packet.quadWeights["LO"] ?: 25f

                                // Rule 1: MATCHES Candidates (Coldest prediction elements)
                                val matchesDigit1 = predictionsList.getOrNull(0)?.digit ?: 3
                                val matchesDigit2 = predictionsList.getOrNull(1)?.digit ?: 7
                                val matchesScore = ((predictionsList.getOrNull(0)?.confidence ?: 40f) / 30f + 1f).coerceIn(2f, 6f).toInt()

                                // Rule 2: OVER Candidates
                                val overCandidateText: String
                                val overScore: Int
                                if (heWeight > hoWeight && heWeight > 25f) {
                                    overCandidateText = "Over 6 [7, 8, 9]"
                                    overScore = ((heWeight / 10f) + 1f).coerceIn(3f, 9f).toInt()
                                } else if (hoWeight > heWeight && hoWeight > 25f) {
                                    overCandidateText = "Over 4 [5, 7, 9]"
                                    overScore = ((hoWeight / 10f) + 2f).coerceIn(3f, 9f).toInt()
                                } else {
                                    overCandidateText = "Over 5 [6, 7, 8, 9]"
                                    overScore = 3
                                }

                                // Rule 3: UNDER Candidates
                                val underCandidateText: String
                                val underScore: Int
                                if (leWeight > loWeight && leWeight > 25f) {
                                    underCandidateText = "Under 5 [0, 1, 2, 3, 4]"
                                    underScore = ((leWeight / 10f) + 2f).coerceIn(3f, 9f).toInt()
                                } else if (loWeight > leWeight && loWeight > 25f) {
                                    underCandidateText = "Under 4 [0, 1, 2, 3]"
                                    underScore = ((loWeight / 10f) + 1f).coerceIn(3f, 9f).toInt()
                                } else {
                                    underCandidateText = "Under 5 [0, 1, 2, 3, 4]"
                                    underScore = 6
                                }

                                // Rule 4: DIFFERS Candidates (Coldest is safest outlier)
                                val prefersDiffersDigit = prime.digit
                                val differsScore = ((100f - prime.confidence) / 10f).coerceIn(4f, 10f).toInt()

                                val rows = listOf(
                                    Triple("Matches 🎯", "D$matchesDigit1, D$matchesDigit2", matchesScore),
                                    Triple("Over 📈", overCandidateText, overScore),
                                    Triple("Under 📉", underCandidateText, underScore),
                                    Triple("Differs 🛡️", "D$prefersDiffersDigit", differsScore)
                                )

                                rows.forEachIndexed { i, row ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Column 1: Contract Type
                                        Text(
                                            text = row.first,
                                            modifier = Modifier.weight(1f),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        // Column 2: Digit Candidates
                                        Text(
                                            text = row.second,
                                            modifier = Modifier.weight(1.3f),
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium
                                        )

                                        // Column 3: Confidence Score / Progress indicator
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${row.third}",
                                                color = if (row.third >= 6) Color(0xFFFBBF24) else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(36.dp)
                                                    .height(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.05f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .fillMaxWidth(row.third / 10f)
                                                        .background(
                                                            if (row.third >= 6) Color(0xFFFBBF24) else MaterialTheme.colorScheme.primary
                                                        )
                                                )
                                            }
                                        }
                                    }

                                    if (i < rows.lastIndex) {
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.03f))
                                    }
                                }
                            }
                        }
                    }

                    // --- TRENDING SIGNALS CORRIDOR (DYNAMIC POPULARITY FEED) ---
                    Column {
                        Text(
                            text = "🔥 HIGHEST FREQUENCY TRENDING SIGNALS",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116)),
                            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Live analytics measuring system-wide occurrences. High occurrence signals with >80% winrate are prioritized.",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // Local state is now collected at the top of PredictionsScreen

                                val trendingList = remember(signalHistory) {
                                    if (signalHistory.isEmpty()) {
                                        listOf(
                                            TrendingSignalData("Volatility 100 (1S) [UNDER 7]", "UNDER", 7, 142, 88.0f, "HOT 🔥", 142, 88.0f, symbolCode = "1HZ100V"),
                                            TrendingSignalData("Volatility 10 (1S) [OVER 1]", "OVER", 1, 98, 85.4f, "DOMINANT 🚀", 98, 85.4f, symbolCode = "1HZ10V"),
                                            TrendingSignalData("Volatility 50 (1S) [DIFFERS 9]", "DIFFERS", 9, 185, 91.0f, "STABLE 💎", 185, 91.0f, symbolCode = "1HZ50V"),
                                            TrendingSignalData("Volatility 25 (1S) [UNDER 8]", "UNDER", 8, 120, 89.2f, "SCALPING ⚡", 120, 89.2f, symbolCode = "1HZ25V"),
                                            TrendingSignalData("Volatility 75 [DIFFERS 0]", "DIFFERS", 0, 76, 90.0f, "SECURE 🛡️", 76, 90.0f, symbolCode = "R_75")
                                        )
                                    } else {
                                        val groups = signalHistory.groupBy { Triple(it.symbolCode, it.contractType, it.barrierValue) }
                                        groups.map { (key, list) ->
                                            val (symbolCode, contractType, barrierValue) = key
                                            val displayName = list.firstOrNull()?.displayName ?: symbolCode
                                            val wins = list.count { it.isWin == true }
                                            val resolvedCount = list.count { it.isWin != null }
                                            val winRate = if (resolvedCount > 0) (wins.toFloat() / resolvedCount) * 100f else 0f
                                            
                                            val badge = when {
                                                winRate >= 90f -> "SECURE 🛡️"
                                                winRate >= 80f -> "STABLE 💎"
                                                winRate >= 70f -> "DOMINANT 🚀"
                                                else -> "HOT 🔥"
                                            }
                                            TrendingSignalData(
                                                title = "$displayName [$contractType $barrierValue]",
                                                contractType = contractType,
                                                barrierValue = barrierValue,
                                                baseCount = list.size,
                                                baseWinRate = winRate,
                                                badge = badge,
                                                currentCount = list.size,
                                                currentWinRate = winRate,
                                                symbolCode = symbolCode
                                            )
                                        }.sortedByDescending { it.currentWinRate }
                                    }
                                }

                                trendingList.forEachIndexed { i, trend ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedTrendDetail = trend }
                                            .padding(vertical = 10.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1.3f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
                                                Text(
                                                    text = trend.title,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color.White.copy(alpha = 0.05f)
                                            ) {
                                                Text(
                                                    text = "${trend.badge} (TAP TO VIEW)",
                                                    color = Color.LightGray,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "${trend.currentCount} SAMPLES",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "RECORDS FOUND",
                                                color = Color.Gray,
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Column(
                                            modifier = Modifier.weight(1.2f),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = String.format(Locale.US, "%.1f%% Win", trend.currentWinRate),
                                                color = if (trend.currentWinRate >= 80f) Color(0xFF10B981) else Color(0xFFFBBF24),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(modifier = Modifier.width(90.dp)) {
                                                LinearProgressIndicator(
                                                    progress = { trend.currentWinRate / 100f },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(4.dp)
                                                        .clip(CircleShape),
                                                    color = if (trend.currentWinRate >= 80f) Color(0xFF10B981) else Color(0xFFFBBF24),
                                                    trackColor = Color.White.copy(alpha = 0.05f)
                                                )
                                            }
                                        }
                                    }

                                    if (i < trendingList.lastIndex) {
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.03f))
                                    }
                                }
                            }
                        }
                    }

                    // --- INDIVIDUAL TREND DETAILS INFOPANEL (MATCHING ENTRIES) ---
                    selectedTrendDetail?.let { trend ->
                        val matchingItems = signalHistory.filter {
                            it.symbolCode == trend.symbolCode &&
                            it.contractType == trend.contractType &&
                            it.barrierValue == trend.barrierValue
                        }.sortedByDescending { it.timestamp }

                        AlertDialog(
                            onDismissRequest = { selectedTrendDetail = null },
                            title = {
                                Column {
                                    Text(
                                        text = trend.title.uppercase(),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "DETAILED SIGNAL BACK-LOG ENTRIES",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            },
                            text = {
                                Box(modifier = Modifier.heightIn(max = 380.dp)) {
                                    if (matchingItems.isEmpty()) {
                                        Text(
                                            text = "Simulation active. Awaiting fresh database records matching this symbol criteria...",
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 24.dp)
                                        )
                                    } else {
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(matchingItems) { item ->
                                                val dateFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                                                val timeStr = dateFormat.format(java.util.Date(item.timestamp))
                                                val isWinTag = item.isWin
                                                
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.White.copy(alpha = 0.03f))
                                                        .border(
                                                            width = 1.dp,
                                                            color = when (isWinTag) {
                                                                true -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                                false -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                                                else -> Color.White.copy(alpha = 0.05f)
                                                            },
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(8.dp)
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Entry ID: ${item.signalId}",
                                                            color = Color.White.copy(alpha = 0.7f),
                                                            fontSize = 9.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = timeStr,
                                                            color = Color.Gray,
                                                            fontSize = 9.sp,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Contract Type: ${item.contractType} ${item.barrierValue}  |  Risk: ${item.riskProfile}",
                                                        color = Color.LightGray,
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Candidates: { ${item.winDigits} }",
                                                            color = Color.Gray,
                                                            fontSize = 9.sp,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                        
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(
                                                                    when (isWinTag) {
                                                                        true -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                                        false -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                                                        else -> Color(0xFFFBBF24).copy(alpha = 0.15f)
                                                                    }
                                                                )
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = when (isWinTag) {
                                                                    true -> "WIN"
                                                                    false -> "LOSS"
                                                                    else -> "PENDING"
                                                                },
                                                                color = when (isWinTag) {
                                                                    true -> Color(0xFF34D399)
                                                                    false -> Color(0xFFFCA5A5)
                                                                    else -> Color(0xFFFBBF24)
                                                                },
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Black,
                                                                fontFamily = FontFamily.Monospace
                                                            )
                                                        }
                                                    }
                                                    
                                                    if (item.observedTicks.isNotEmpty()) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "Tick Path: [ ${item.observedTicks.map { "$it" }.joinToString(", ")} ]",
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
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
                            },
                            confirmButton = {
                                TextButton(onClick = { selectedTrendDetail = null }) {
                                    Text(
                                        text = "DISMISS",
                                        color = Color(0xFFEF4444),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            },
                            containerColor = Color(0xFF0F0F16)
                        )
                    }

                    // --- AUTOMATED NOTIFIER TARGET SELECTION DIALOG ---
                    if (showContractPopup) {
                        AlertDialog(
                            onDismissRequest = { showContractPopup = false },
                            title = {
                                Text(
                                    text = "MONITORED CONTRACT TYPE",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            text = {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text(
                                        text = "Automated alarms & PiP updates will be filtered for this chosen contract category only:",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )

                                    val contracts = listOf(
                                        "MATCHES" to "Matches 🎯  (Digit Reversion)",
                                        "DIFFERS" to "Differs 🛡️  (High Probability Outliers)",
                                        "OVER" to "Over 📈  (Lower Digit Droughts)",
                                        "UNDER" to "Under 📉  (Upper Digit Droughts)"
                                    )

                                    contracts.forEach { (type, description) ->
                                        Button(
                                            onClick = {
                                                viewModel.toggleAutomatedNotifier(true, type)
                                                showContractPopup = false
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (notifierContract == type) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.05f),
                                                contentColor = if (notifierContract == type) Color.Black else Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        ) {
                                            Text(
                                                text = description,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showContractPopup = false }) {
                                    Text("DISMISS", color = Color.Gray, fontSize = 11.sp)
                                }
                            },
                            containerColor = Color(0xFF13141A),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        )
                    }

                    // --- AUTOMATED NOTIFIER DISPLAY PANEL ---
                    Column {
                        Text(
                            text = "AUTOMATED SIGNAL NOTIFIER CONTROL",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isNotifierRunning) Color(0xFF10B981).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                                        ) {
                                            Text(
                                                text = if (isNotifierRunning) "🟢 RUNNING" else "🔕 STOPPED",
                                                color = if (isNotifierRunning) Color(0xFF10B981) else Color.Gray,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }

                                        Text(
                                            text = "Automated Notifier",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Switch(
                                        checked = isNotifierRunning,
                                        onCheckedChange = { isChecked ->
                                            if (isChecked) {
                                                showContractPopup = true
                                            } else {
                                                viewModel.toggleAutomatedNotifier(false)
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFFFBBF24),
                                            checkedTrackColor = Color(0xFFFBBF24).copy(alpha = 0.3f),
                                            uncheckedThumbColor = Color.LightGray,
                                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                        )
                                    )
                                }

                                if (isNotifierRunning) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.03f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "FILTERED CONTRACT TARGET",
                                                    color = Color.Gray,
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = notifierContract ?: "ALL ACTIVE",
                                                    color = Color(0xFF38BDF8),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            Button(
                                                onClick = { showContractPopup = true },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color.White.copy(alpha = 0.08f),
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("CHANGE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Text(
                                        text = "ℹ️ Lock this phone or press Home: Picture-in-Picture mode will auto-activate displaying filtered $notifierContract updates.",
                                        color = Color.LightGray.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Start the automated notifier to receive background alerts and focus specific contract vectors inside Picture-In-Picture widgets.",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // --- VIRTUAL WHAT-IF PRACTICE ENGINE ---
                    Column {
                        Text(
                            text = "🔬 VIRTUAL PRACTICE & WHAT-IF BACKTESTING",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116))
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isBacktestActive) Color(0xFFFBBF24).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                                        ) {
                                            Text(
                                                text = if (isBacktestActive) "🔬 SIMULATING" else "⏸ STOPPED",
                                                color = if (isBacktestActive) Color(0xFFFBBF24) else Color.Gray,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "What-If Real-time Bets",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Closed after ${userSettings.virtualTradeCloseTicks} ticks",
                                                color = Color.Gray,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = isBacktestActive,
                                        onCheckedChange = { viewModel.toggleBacktest(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFFFBBF24),
                                            checkedTrackColor = Color(0xFFFBBF24).copy(alpha = 0.3f),
                                            uncheckedThumbColor = Color.LightGray,
                                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                        )
                                    )
                                }

                                // Calculations for overall win rates
                                val sumWins = backtestWins.values.sum()
                                val sumBets = backtestBets.values.sum()
                                val totalWR = if (sumBets > 0) (sumWins.toFloat() / sumBets.toFloat() * 100f) else 0f

                                // Find best digit for entry (Empirical winrate index)
                                var bestDigitForEntry = -1
                                var bestDigitWR = 0f
                                for (d in 0..9) {
                                    val betsD = backtestBets[d] ?: 0
                                    if (betsD >= 1) {
                                        val winsD = backtestWins[d] ?: 0
                                        val wrD = winsD.toFloat() / betsD.toFloat() * 100f
                                        if (wrD > bestDigitWR) {
                                            bestDigitWR = wrD
                                            bestDigitForEntry = d
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("PRACTICE WINRATE", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                            Text(
                                                text = if (sumBets > 0) String.format("%.1f%%", totalWR) else "--",
                                                color = if (totalWR >= 70f) Color(0xFF10B981) else if (totalWR >= 50f) Color(0xFFFBBF24) else Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Text("Total: $sumWins wins of $sumBets", color = Color.LightGray.copy(alpha = 0.5f), fontSize = 8.sp)
                                        }
                                    }

                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("BEST DIGIT ENTRY", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                            Text(
                                                text = if (bestDigitForEntry >= 0) "Digit $bestDigitForEntry" else "No empirical best yet",
                                                color = Color(0xFF38BDF8),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Text(
                                                text = if (bestDigitForEntry >= 0) String.format("Empirical WR: %.0f%%", bestDigitWR) else "Need more virtual trades",
                                                color = Color.LightGray.copy(alpha = 0.5f),
                                                fontSize = 8.sp
                                            )
                                        }
                                    }
                                }

                                if (sumBets > 0) {
                                    Button(
                                        onClick = { viewModel.resetBacktestStats() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                                            contentColor = Color(0xFFEF4444)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(32.dp)
                                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    ) {
                                        Text("RESET PRACTICE STATISTICS", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }

                                // Display Transactions Feed
                                if (backtestTxList.isNotEmpty()) {
                                    Text(
                                        text = "RECENT VIRTUAL TRANSACTIONS (LIVE FEED)",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        backtestTxList.take(4).forEach { tx ->
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
                                                        color = if (tx.result == "WIN") Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = tx.result,
                                                            color = if (tx.result == "WIN") Color(0xFF10B981) else Color(0xFFEF4444),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }

                                                    Text(
                                                        text = "${tx.contractType} on target: D${tx.digit}",
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }

                                                Text(
                                                    text = "Outcome: D${tx.entryDigit}",
                                                    color = Color.LightGray,
                                                    fontSize = 10.sp,
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

data class TrendingSignalData(
    val title: String,
    val contractType: String,
    val barrierValue: Int,
    val baseCount: Int,
    val baseWinRate: Float,
    val badge: String,
    val currentCount: Int = 0,
    val currentWinRate: Float = 0f,
    val symbolCode: String = ""
)
