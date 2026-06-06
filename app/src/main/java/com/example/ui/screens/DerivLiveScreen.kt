package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    val authorizedScopes by viewModel.authorizedScopes.collectAsState()

    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    var activeTab by remember { mutableStateOf("CONTRACTS") } // "CONTRACTS", "STATEMENT_LOGS", "HISTORY"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07080C))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- PROFILE HEADER SUMMARY ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DERIV LIVE WALLET",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (!authorizedTraderName.isNullOrBlank()) authorizedTraderName!! else "Secure Trading Session",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            IconButton(
                onClick = { viewModel.forceReconnectWebSocket() },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Force Reconnect",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ================= BIG MATURE WALLET CARD =================
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (userSettings.isDemoAccount) Color(0xFF0F1E19) else Color(0xFF221114)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (userSettings.isDemoAccount) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFEF4444).copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (userSettings.isDemoAccount) Color(0xFF10B981) else Color(0xFFEF4444))
                        )
                        Text(
                            text = if (userSettings.isDemoAccount) "DEMO (VIRTUAL) PORTFOLIO" else "REAL LIVE PORTFOLIO",
                            color = if (userSettings.isDemoAccount) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = authorizedUserId ?: "ID: OFFLINE",
                        color = Color.LightGray.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column {
                    val amountVal = if (userSettings.isDemoAccount) userSettings.demoWalletBalance else userSettings.realWalletBalance
                    val currencySymbol = authorizedCurrency ?: "USD"
                    Text(
                        text = String.format("$%,.2f", amountVal),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "ACCOUNT VALUE ($currencySymbol)",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                // Mini stats bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "CLIENT CODENAME", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text(text = if (userSettings.traderName.isBlank()) "CO-PILOT" else userSettings.traderName.uppercase(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "BROKER MODE TYPE", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text(text = if (userSettings.isDemoAccount) "VIRTUAL COINS" else "REAL MONEY", color = if (userSettings.isDemoAccount) Color(0xFF10B981) else Color(0xFFEF4444), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                // Interactive Switch button row inside the card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { if (!userSettings.isDemoAccount) viewModel.toggleAccountType() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (userSettings.isDemoAccount) Color(0xFF10B981).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                            contentColor = if (userSettings.isDemoAccount) Color(0xFF10B981) else Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .border(
                                width = 0.5.dp,
                                color = if (userSettings.isDemoAccount) Color(0xFF10B981).copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Text("USE DEMO ACCOUNT", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = { if (userSettings.isDemoAccount) viewModel.toggleAccountType() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!userSettings.isDemoAccount) Color(0xFFEF4444).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                            contentColor = if (!userSettings.isDemoAccount) Color(0xFFEF4444) else Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .border(
                                width = 0.5.dp,
                                color = if (!userSettings.isDemoAccount) Color(0xFFEF4444).copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Text("USE REAL ACCOUNT", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // ================= MODERN CONTROL TABS =================
        TabRow(
            selectedTabIndex = when (activeTab) {
                "CONTRACTS" -> 0
                "STATEMENT_LOGS" -> 1
                else -> 2
            },
            containerColor = Color(0xFF111218),
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[when (activeTab) {
                        "CONTRACTS" -> 0
                        "STATEMENT_LOGS" -> 1
                        else -> 2
                    }]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
        ) {
            Tab(
                selected = activeTab == "CONTRACTS",
                onClick = { activeTab = "CONTRACTS" },
                text = { Text("LIVE CONTRACTS", fontSize = 9.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace) }
            )
            Tab(
                selected = activeTab == "STATEMENT_LOGS",
                onClick = { activeTab = "STATEMENT_LOGS" },
                text = { Text("LOGS & STATS", fontSize = 9.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace) }
            )
            Tab(
                selected = activeTab == "HISTORY",
                onClick = { activeTab = "HISTORY" },
                text = { Text("HISTORY LOG", fontSize = 9.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace) }
            )
        }

        // ================= TAB CONTENTS =================
        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                "CONTRACTS" -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        var subContractsTab by remember { mutableStateOf("ACTIVE") } // "ACTIVE", "CLOSED", "STATEMENT"
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val subTabs = listOf(
                                "ACTIVE" to "Active (Open)",
                                "CLOSED" to "Closed (Resolved)",
                                "STATEMENT" to "Mini Statement"
                            )
                            subTabs.forEach { (key, label) ->
                                val sel = subContractsTab == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(0.5.dp, if (sel) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(6.dp))
                                        .clickable { subContractsTab = key }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (sel) Color.White else Color.Gray,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        if (subContractsTab == "ACTIVE") {
                            val activeListing = activeContracts.filter { it.status == "OPEN" }
                            if (activeListing.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF111218))
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "[No active real-time contract runs active]",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(activeListing) { contract ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF111218))
                                                .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "${contract.symbol} - ${contract.contractType}",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = "Id: ${contract.contractId}",
                                                    color = Color.Gray,
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = String.format("Stake: $%.2f", contract.buyPrice),
                                                    color = Color.LightGray,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                val sign = if (contract.profit >= 0) "+" else ""
                                                val pColor = if (contract.profit >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                                Text(
                                                    text = String.format("%s$%.2f", sign, contract.profit),
                                                    color = pColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (subContractsTab == "CLOSED") {
                            val closedListing = activeContracts.filter { it.status != "OPEN" }
                            if (closedListing.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF111218))
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "[No resolved real-time positions logged]",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(closedListing) { contract ->
                                        val earnColor = if (contract.status == "WON") Color(0xFF10B981) else Color(0xFFEF4444)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF111218))
                                                .border(0.5.dp, earnColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "${contract.symbol} - ${contract.contractType}",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = "Exit Digit: ${contract.exitDigit ?: "N/A"}",
                                                    color = Color.Gray,
                                                    fontSize = 8.5.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                val sign = if (contract.profit >= 0) "+" else ""
                                                Text(
                                                    text = String.format("%s$%.2f", sign, contract.profit),
                                                    color = earnColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(earnColor.copy(alpha = 0.15f))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(text = contract.status, color = earnColor, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Mini bank statement-styled ledger representing database settlements.
                            val walletStatement = realTradeHistory.take(15)
                            if (walletStatement.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF111218))
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "[Statements transaction ledger empty]",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(walletStatement) { contract ->
                                        val earnColor = if (contract.status == "WON") Color(0xFF10B981) else Color(0xFFEF4444)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF0D0E15))
                                                .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(6.dp))
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(earnColor)
                                                    )
                                                    Text(
                                                        text = "TX_ID #${contract.contractId.toString().takeLast(6)}",
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                                Text(
                                                    text = "Asset: ${contract.symbol} | Type: ${contract.contractType}",
                                                    color = Color.Gray,
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                val sign = if (contract.profit >= 0) "+" else ""
                                                Text(
                                                    text = String.format("%s$%.2f USD", sign, contract.profit),
                                                    color = earnColor,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(contract.timestamp)),
                                                    color = Color.DarkGray,
                                                    fontSize = 7.5.sp,
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

                "STATEMENT_LOGS" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111218)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("SYSTEM DIAGNOSTICS", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Broker Socket", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text(connectionState, color = if (connectionState == "AUTHORIZED") Color(0xFF10B981) else Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Round Trip Latency", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text(if (rttLatency > 0) "${rttLatency} ms" else "Measuring...", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                                
                                if (authorizedScopes.isNotEmpty()) {
                                    Text("AUTHORIZED SCOPES:", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 4.dp))
                                    Text(authorizedScopes.joinToString(", ").uppercase(), color = Color.LightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        // Code logs console terminal
                        Text(
                            text = "💻 BROKER WEBSOCKET TERMINAL STREAM",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF040406))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            if (liveLogs.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "[Console clean - waiting for communications]",
                                        color = Color.Gray,
                                        fontSize = 9.sp,
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
                                            Text(formattedTime, color = Color.DarkGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                            Text("[${log.type}] ${log.message}", color = textColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "HISTORY" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "📋 ARCHIVE STATEMENT TRANSACTIONS HISTORY",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        if (realTradeHistory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF111218))
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Statements logs database currently empty.",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(realTradeHistory) { contract ->
                                    val statusColor = when (contract.status) {
                                        "WON" -> Color(0xFF10B981)
                                        "LOST" -> Color(0xFFEF4444)
                                        else -> Color.Gray
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF111218))
                                            .border(0.5.dp, statusColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${contract.symbol} - ${contract.contractType}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "Contract ID: ${contract.contractId}",
                                                color = Color.Gray,
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(contract.timestamp)),
                                                color = Color.DarkGray,
                                                fontSize = 7.5.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            val sign = if (contract.profit >= 0) "+" else ""
                                            Text(
                                                text = String.format("%s$%.2f", sign, contract.profit),
                                                color = statusColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(statusColor.copy(alpha = 0.12f))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
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
}
