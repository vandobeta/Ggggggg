package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.DigitAnalysisViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FirstLaunchSetupScreen(
    viewModel: DigitAnalysisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userSettings by viewModel.userSettings.collectAsState()
    val realPing by viewModel.pingState.collectAsState()
    var isSimulatingHighPing by remember { mutableStateOf(false) }
    var isHighPingOverridden by remember { mutableStateOf(false) }
    val pingToShow = if (isSimulatingHighPing) 324L else realPing
    
    var currentStep by remember { mutableStateOf(1) } // Steps 1 to 3
    
    // Step 1 Form States
    var nameInput by remember { mutableStateOf("") }
    var capitalInput by remember { mutableStateOf("1000.00") }
    var selectedCurrency by remember { mutableStateOf("USD") }
    var goalInput by remember { mutableStateOf("To maintain steady gains with cool head.") }
    
    // Step 2 Form States
    var disclaimerAccepted by remember { mutableStateOf(false) }
    
    // Step 3 Permissions Status Placeholder Checks
    var notificationsGranted by remember { mutableStateOf(false) }
    var batteryGranted by remember { mutableStateOf(false) }

    // Launcher for Notification Permission on Android 13+
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationsGranted = isGranted
        if (isGranted) {
            Toast.makeText(context, "Notifications Enabled!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Please allow notifications in system settings", Toast.LENGTH_LONG).show()
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F0F1A), Color(0xFF040409))
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // --- PLATFORM LOGO HEADER ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text(
                    text = "🤖 ALGO-RADAR SETUP",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "PLATFORM ACTIVATION",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = 0.5.sp
                )
                
                // Active Step indicator
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (step in 1..3) {
                        val active = step == currentStep
                        val passed = step < currentStep
                        Box(
                            modifier = Modifier
                                .size(height = 6.dp, width = 45.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    when {
                                        active -> MaterialTheme.colorScheme.primary
                                        passed -> Color(0xFF10B981)
                                        else -> Color.White.copy(alpha = 0.15f)
                                    }
                                )
                        )
                    }
                }
            }

            // --- ANIMATED CONTENT SWITCH ----
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "setup_steps"
                ) { step ->
                    when (step) {
                        1 -> StepTraderMetrics(
                            name = nameInput,
                            onNameChange = { nameInput = it },
                            capital = capitalInput,
                            onCapitalChange = { capitalInput = it },
                            valCurrency = selectedCurrency,
                            onCurrencySelected = { selectedCurrency = it },
                            goal = goalInput,
                            onGoalChange = { goalInput = it }
                        )
                        2 -> StepDisclaimers(
                            disclaimerAccepted = disclaimerAccepted,
                            onDisclaimerAcceptedChange = { disclaimerAccepted = it }
                        )
                        3 -> StepPermissionsAndIgnition(
                            context = context,
                            notificationsGranted = notificationsGranted,
                            batteryGranted = batteryGranted,
                            ping = pingToShow,
                            isSimulatingHighPing = isSimulatingHighPing,
                            onSimulateHighPingToggle = { isSimulatingHighPing = it },
                            isHighPingOverridden = isHighPingOverridden,
                            onHighPingOverrideChange = { isHighPingOverridden = it },
                            onRequestNotifications = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    notificationsGranted = true
                                    Toast.makeText(context, "Notifications ready!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onRequestBattery = {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                    batteryGranted = true
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Battery waivers can be granted in OS battery settings", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                }
            }

            // --- NAVIGATION CONTROLS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    Button(
                        onClick = { currentStep-- },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.08f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("BACK", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    Spacer(modifier = Modifier.width(60.dp))
                }

                val nextEnabled = when (currentStep) {
                    1 -> nameInput.trim().isNotBlank() && capitalInput.trim().toDoubleOrNull() != null
                    2 -> disclaimerAccepted
                    3 -> if (pingToShow > 200L) isHighPingOverridden else true
                    else -> true
                }

                Button(
                    onClick = {
                        if (currentStep < 3) {
                            currentStep++
                        } else {
                            // Activation! Save all states to Room setting isFirstLaunch = false
                            val finalCapital = capitalInput.trim().toDoubleOrNull() ?: 1000.0
                            val finalSettings = userSettings.copy(
                                traderName = nameInput.trim(),
                                capital = finalCapital,
                                currency = selectedCurrency,
                                goal = goalInput.trim(),
                                isFirstLaunch = false
                            )
                            viewModel.updateSettingsInDb(finalSettings)
                            Toast.makeText(context, "Welcome, Trader ${nameInput.trim()}! Algo-Radar Fired up.", Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = nextEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentStep == 3) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        disabledContainerColor = Color.White.copy(alpha = 0.04f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (currentStep == 3) "🚨 ACTIVATE RADAR PLATFORM" else "CONTINUE SETUP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = if (nextEnabled) Color.White else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun StepTraderMetrics(
    name: String,
    onNameChange: (String) -> Unit,
    capital: String,
    onCapitalChange: (String) -> Unit,
    valCurrency: String,
    onCurrencySelected: (String) -> Unit,
    goal: String,
    onGoalChange: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "STEP 1: SETUP TRADER PERSONA",
            color = Color.LightGray,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        
        Text(
            text = "Enter your profile credentials so all alerts and statistical analysis streams personalize around your live budget.",
            color = Color.Gray,
            fontSize = 11.sp
        )

        // Trader Name
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("TRADER NAME", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text("e.g. Satoshi", color = Color.DarkGray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        // Starting Capital
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("STARTING WORKING CAPITAL", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            OutlinedTextField(
                value = capital,
                onValueChange = onCapitalChange,
                placeholder = { Text("1000.00", color = Color.DarkGray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        // Currency
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("CURRENCY DENOMINATION", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            val currencies = listOf("USD", "EUR", "GBP", "AUD")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                currencies.forEach { cur ->
                    val isSelected = cur == valCurrency
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.02f))
                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .clickable { onCurrencySelected(cur) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cur,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Goal
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("PERFORMANCE/GROWTH GOAL", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            OutlinedTextField(
                value = goal,
                onValueChange = onGoalChange,
                placeholder = { Text("e.g. Reach 5% daily consistent payouts", color = Color.DarkGray) },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }
    }
}

@Composable
fun StepDisclaimers(
    disclaimerAccepted: Boolean,
    onDisclaimerAcceptedChange: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "STEP 2: SAFETY DISCLAIMER & RISKS",
            color = Color.LightGray,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(0.5.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "FINANCIAL RISKS RECOGNITION",
                        color = Color(0xFFFBBF24),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Algo-Radar and all corresponding digit distribution feeds are entirely educational tools computed from mathematical algorithms over live streaming indices. This program does NOT offer financial advice, trade fulfillment, or direct portfolio execution.\n\nDerivatives and Volatility trading involve high speculative risks and potential rapid loss of capital. No strategy, warning signal, drought threshold calculation, or automated notify alarm ensures static winning rates. past simulations do NOT represent or guarantee future results!\n\nYou must maintain a strict risk-reward profile, trade mathematically, and only simulation practice with funds you can afford losing.\n\nBy executing this setup, you certify that you understand the speculative risks, take full and exclusive legal responsibility for your trading activities, and agree that Algo-Radar shall not be held liable for any market losses.",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onDisclaimerAcceptedChange(!disclaimerAccepted) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                checked = disclaimerAccepted,
                onCheckedChange = onDisclaimerAcceptedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = Color.Gray
                )
            )
            Text(
                text = "I accept and fully assume all trading volatility risks and acknowledge legal disclaimers.",
                color = if (disclaimerAccepted) Color.White else Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StepPermissionsAndIgnition(
    context: android.content.Context,
    notificationsGranted: Boolean,
    batteryGranted: Boolean,
    ping: Long,
    isSimulatingHighPing: Boolean,
    onSimulateHighPingToggle: (Boolean) -> Unit,
    isHighPingOverridden: Boolean,
    onHighPingOverrideChange: (Boolean) -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestBattery: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "STEP 3: SYSTEM PERMISSION PERMIT",
                color = Color.LightGray,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "To let our real-time WebSocket distribution alarms send microsecond entry signals, the OS must allow background telemetry delivery.",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Notification Permission Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "1. PUSH SIGNAL ALERTS",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Pushes high-probability matches entry alerts with interactive WIN/LOSS click tracking.",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }

                Button(
                    onClick = onRequestNotifications,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (notificationsGranted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    if (notificationsGranted) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Granted", tint = Color.White, modifier = Modifier.size(14.dp))
                    } else {
                        Text("REQUEST", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Battery Ignorer Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "2. DISABLE BATTERY TIMEOUT",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Required. Prevents the Android system from shutting down the live background WebSocket channel.",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }

                Button(
                    onClick = onRequestBattery,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (batteryGranted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    if (batteryGranted) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Granted", tint = Color.White, modifier = Modifier.size(14.dp))
                    } else {
                        Text("WAIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // --- PING LATENCY METER ---
        if (notificationsGranted) {
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(
                            1.dp,
                            if (ping > 200L && !isHighPingOverridden) Color(0xFFEF4444).copy(alpha = 0.4f)
                            else if (ping > 200L) Color(0xFFFBBF24).copy(alpha = 0.3f)
                            else Color(0xFF10B981).copy(alpha = 0.3f)
                        ),
                        RoundedCornerShape(12.dp)
                    )
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
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (ping > 200L) Color(0xFFEF4444) else Color(0xFF10B981)
                                    )
                            )
                            Text(
                                text = "📡 REAL-TIME NETWORK LATENCY TEST",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        TextButton(
                            onClick = { onSimulateHighPingToggle(!isSimulatingHighPing) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (isSimulatingHighPing) "🛑 RESET LATENCY" else "⚡ SIMULATE SPIKE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Deriv WebSocket Server Ping Status:",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )

                        Text(
                            text = if (ping > 0) "$ping ms" else "Measuring...",
                            color = if (ping > 200L) Color(0xFFEF4444) else Color(0xFF10B981),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (ping > 200L) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEF4444).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "⚠️ NETWORK VOLATILITY HAZARD",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Consequences of poor connectivity:\n• Delays in receiving real-time streaming tick updates.\n• High slippage on match signal orders causing misplaced entries.\n• Divergence in digit distribution frequencies compared to actual live market conditions.\n• Latency spikes may freeze telemetry pipelines entirely.",
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                if (!isHighPingOverridden) {
                                    Button(
                                        onClick = { onHighPingOverrideChange(true) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFEF4444).copy(alpha = 0.4f),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "⚠️ ACKNOWLEDGE & CONTINUE ANYWAY",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Accepted",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Latency warning acknowledged and bypassed.",
                                            color = Color(0xFF10B981),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Excellent",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Highly optimized low latency pipeline detected.",
                                color = Color(0xFF10B981),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Yellow.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .border(1.dp, Color.Yellow.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Text(
                text = "⚠️ Note: On most Android devices, adding background execution waiver secures constant streaming. Go ahead and allow notifications and battery optimization exemptions.",
                color = Color(0xFFFBBF24),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
