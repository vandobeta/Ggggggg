package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.DigitAnalysisViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DerivLiveScreen(
    viewModel: DigitAnalysisViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val rttLatency by viewModel.pingState.collectAsState()
    val liveLogs by viewModel.liveLogs.collectAsState()
    val activeContracts by viewModel.activeContracts.collectAsState()
    val realTradeHistory by viewModel.realTradeHistory.collectAsState()
    
    val userSettings by viewModel.userSettings.collectAsState()
    val authorizedTraderName by viewModel.authorizedTraderName.collectAsState()
    val authorizedEmail by viewModel.authorizedEmail.collectAsState()
    val authorizedCountry by viewModel.authorizedCountry.collectAsState()
    val authorizedCurrency by viewModel.authorizedCurrency.collectAsState()
    val authorizedUserId by viewModel.authorizedUserId.collectAsState()
    val authorizedBalance by viewModel.authorizedBalance.collectAsState()
    val authorizedScopes by viewModel.authorizedScopes.collectAsState()

    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07080C))
            .padding(16.dp)
    ) {
        // --- SCREEN TITLE ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DERIV LIVE CONSOLE",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Authentic Real-Time Socket Diagnostics & Profile Terminal",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Reconnect floating-style action button
            IconButton(
                onClick = { viewModel.forceReconnectWebSocket() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Force Reconnect",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            // ================= 1. TELEMETRY & DIAGNOSTICS =================
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111218)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CONNECTION TELEMETRY",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            
                            val statusColor = when (connectionState) {
                                "AUTHORIZED" -> Color(0xFF10B981)
                                "CONNECTED" -> Color(0xFF3B82F6)
                                "CONNECTING..." -> Color(0xFFFBBF24)
                                else -> Color(0xFFEF4444)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusColor.copy(alpha = 0.15f))
                                    .border(0.5.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = connectionState.uppercase(),
                                    color = statusColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "CLIENT LATENCY / RTT", color = Color.Gray, fontSize = 9.sp)
                                Text(
                                    text = if (rttLatency > 0) "${rttLatency} ms" else "Calculating...",
                                    color = if (rttLatency in 1..150) Color(0xFF34D399) else if (rttLatency > 150) Color(0xFFFBBF24) else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "TRANSMISSION MODEL", color = Color.Gray, fontSize = 9.sp)
                                val (modelText, modelColor) = when (connectionState) {
                                    "AUTHORIZED", "CONNECTED" -> "SECURE WSS REALTIME" to Color(0xFF34D399)
                                    "CONNECTING..." -> "ESTABLISHING..." to Color(0xFFFBBF24)
                                    else -> "OFFLINE / DISCONNECTED" to Color(0xFFEF4444)
                                }
                                Text(
                                    text = modelText,
                                    color = modelColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.End
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                        Spacer(modifier = Modifier.height(10.dp))

                        Column {
                            Text(text = "BROKER WEBSOCKET ENDPOINT", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "wss://ws.binaryws.com/websockets/v3?app_id=1089",
                                color = Color.LightGray.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // ================= 2. REAL-TIME ACCOUNT PROFILE =================
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111218)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "SECURE CLIENT PROFILE",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val textName = if (!authorizedTraderName.isNullOrBlank()) {
                                    authorizedTraderName!!
                                } else if (userSettings.traderName.isNotBlank()) {
                                    userSettings.traderName
                                } else {
                                    "Awaiting Secure Token Auth"
                                }
                                Text(
                                    text = textName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                val textEmail = authorizedEmail ?: "No email cached"
                                Text(
                                    text = textEmail,
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Demo / Real badge click to switch
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.toggleAccountType() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (userSettings.isDemoAccount) "GO REAL" else "GO DEMO",
                                    color = if (userSettings.isDemoAccount) Color(0xFFEF4444) else Color(0xFF10B981),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Net Balance display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val currentLabel = if (userSettings.isDemoAccount) "DEMO WALLET BALANCE" else "REAL WALLET BALANCE"
                                Text(text = currentLabel, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                
                                val amountVal = if (userSettings.isDemoAccount) userSettings.demoWalletBalance else userSettings.realWalletBalance
                                val currencySymbol = authorizedCurrency ?: "USD"
                                Text(
                                    text = String.format("$%.2f %s", amountVal, currencySymbol),
                                    color = if (userSettings.isDemoAccount) Color(0xFF34D399) else Color(0xFF00E5FF),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "CLIENT USER ID", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = authorizedUserId ?: "Unauthenticated",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        if (authorizedScopes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "AUTHORIZED PERMISSION SCOPES:",
                                color = Color.Gray,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                authorizedScopes.forEach { scope ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White.copy(alpha = 0.04f))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = scope.uppercase(),
                                            color = Color.LightGray,
                                            fontSize = 7.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= 3. ACTIVE DERIV CONTRACT TRACKER =================
            item {
                Column {
                    Text(
                        text = "LIVE ACTIVE TRADES CONTRACTS",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (activeContracts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF111218))
                                .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "No active trades",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "No real-time WebSocket contracts active.",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            activeContracts.forEach { contract ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF111218))
                                        .border(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${contract.symbol} - ${contract.contractType}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "Contract ID: ${contract.contractId}",
                                            color = Color.Gray,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format("Stake: $%.2f", contract.buyPrice),
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        
                                        val profitColor = if (contract.profit >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                        val plusSign = if (contract.profit >= 0) "+" else ""
                                        Text(
                                            text = String.format("%s$%.2f", plusSign, contract.profit),
                                            color = profitColor,
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

            // ================= 4. REAL-TIME MONOSPACE TERMINAL PANEL =================
            item {
                Column {
                    Text(
                        text = "WEBSOCKET CONSOLE LOGS",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF040406))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        if (liveLogs.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "[Console idle - awaiting network frames]",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(liveLogs, key = { it.id }) { log ->
                                    val textColor = when (log.type) {
                                        "ERROR" -> Color(0xFFF87171)
                                        "OUTBOUND" -> Color(0xFF60A5FA)
                                        "INBOUND" -> Color(0xFF34D399)
                                        else -> Color.Gray
                                    }
                                    
                                    val formattedTime = remember(log.timestamp) { timeFormatter.format(Date(log.timestamp)) }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = formattedTime,
                                            color = Color.DarkGray,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "[$${log.type}] ${log.message}",
                                            color = textColor,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= 5. HISTORICAL CONTRACT OUTPUTS =================
            item {
                Column {
                    Text(
                        text = "HISTORICAL CONTRACTS OUTPUTS",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (realTradeHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF111218))
                                .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No completed trades resolved.",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            realTradeHistory.forEach { contract ->
                                val statusColor = when (contract.status) {
                                    "WON" -> Color(0xFF10B981)
                                    "LOST" -> Color(0xFFEF4444)
                                    else -> Color.Gray
                                }
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF111218))
                                        .border(0.5.dp, statusColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${contract.symbol} - ${contract.contractType}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "Final Exit Digit: ${contract.exitDigit ?: "n/a"}",
                                            color = Color.Gray,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        val pnlSign = if (contract.profit >= 0) "+" else ""
                                        Text(
                                            text = String.format("%s$%.2f", pnlSign, contract.profit),
                                            color = statusColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(statusColor.copy(alpha = 0.12f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = contract.status,
                                                color = statusColor,
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
    }
}
