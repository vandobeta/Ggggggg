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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            
            // --- CARD 1: DERIV INTEGRATION KEYS ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "🔌 DERIV INTEGRATION KEYS",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "Configure your WS Token connecting secure servers to run automated contracts on tactical signals.",
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
                            Text("CO-PILOT WALLET BALANCE", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = String.format("$%.2f %s", userSettings.derivWalletBalance, userSettings.currency.uppercase()),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        val context = androidx.compose.ui.platform.LocalContext.current
                        Button(
                            onClick = {
                                viewModel.resetDemoBalance { success, msg ->
                                    viewModel.resetAutoTraderSession()
                                    if (success) {
                                        android.widget.Toast.makeText(context, "Wallet reset completed: ${msg ?: "Success"}", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Demanded balance reset rejected: $msg", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("reset_wallet_balance_button")
                        ) {
                            Text("Reset Balance", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Account Environment Segmented Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ACCOUNT ENVIRONMENT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Connect to demo virtual or real servers", color = Color.Gray, fontSize = 9.sp)
                        }
                        
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            listOf("demo", "real").forEach { type ->
                                val sel = userSettings.currentAccountType.lowercase().trim() == type
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (sel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable {
                                            viewModel.updateSettingsInDb(userSettings.copy(currentAccountType = type, isDemoAccount = (type == "demo")))
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = type.uppercase(),
                                        color = if (sel) Color.Black else Color.Gray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
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

                    // Configurable Deriv App ID
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("DERIV APP ID", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = userSettings.derivAppId,
                            onValueChange = {
                                viewModel.updateSettingsInDb(userSettings.copy(derivAppId = it.trim()))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("deriv_appid_input"),
                            placeholder = { Text("Default App ID: 33sKaNullz3jmWQs7OXxZ", color = Color.Gray, fontSize = 12.sp) },
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
            }

            // --- CARD 2: ALGORITHMIC RISK PROFILE & AUDIO ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "🛡️ ALGORITHMIC PROFILE",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )

                    // Risk Type Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SELECT ALGORITHMIC RISK CATEGORY",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 9.sp,
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
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // Audio Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Trade Reminder Alarms", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Push recurrent alerts if top market edge goes above standard minimum limit",
                                color = Color.Gray,
                                fontSize = 9.sp
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
                }
            }

            // --- CARD 3: STAKE & LIMITS PILOT CONFIG ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "🤖 PILOT CONTROL DECK",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )

                    // Auto Trader Master Switch
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

                    // Recommendations Display
                    val recStake = (userSettings.derivWalletBalance * 0.01).coerceAtLeast(1.0)
                    val recSL = (userSettings.derivWalletBalance * 0.15).coerceAtLeast(10.0)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = String.format("💡 PILOT LIMIT RECOMMENDATIONS:\n• Target Stake (1%%): $%.2f\n• Stop Loss Limit (15%%): $%.2f", recStake, recSL),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }

                    // Dual-Stake Configuration (Manual & Automated Pilot)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Default Manual Stake Field
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("DEFAULT MANUAL STAKE (USD)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = userSettings.stake.toString(),
                                onValueChange = {
                                    it.toDoubleOrNull()?.let { st ->
                                        viewModel.updateSettingsInDb(userSettings.copy(stake = st))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("default_manual_stake_input"),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                        }

                        // Target Companion Stake (USD)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("TARGET PILOT STAKE (USD)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                modifier = Modifier.fillMaxWidth().testTag("auto_trader_stake_input"),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                        }
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
                                modifier = Modifier.fillMaxWidth().testTag("auto_trader_take_profit_input"),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
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
                                modifier = Modifier.fillMaxWidth().testTag("auto_trader_stop_loss_input"),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
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
                            Text("TRAILING STOP LOSS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DAILY SIGNALS ENGAGED COUNTER", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Maximum number of signals triggered before cooling off", color = Color.Gray, fontSize = 9.sp)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${userSettings.signalsPerDayLimit} limit",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
