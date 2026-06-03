package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FirstLaunchSetupScreen
import com.example.ui.screens.PredictionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignalsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DigitAnalysisViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer

class MainActivity : ComponentActivity() {
  
  private val viewModel: DigitAnalysisViewModel by viewModels()
  private val isInPiPModeState = mutableStateOf(false)

  override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    isInPiPModeState.value = isInPictureInPictureMode
  }

  override fun onUserLeaveHint() {
    super.onUserLeaveHint()
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      try {
        val params = android.app.PictureInPictureParams.Builder().build()
        enterPictureInPictureMode(params)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    setContent {
      val userSettings by viewModel.userSettings.collectAsState()
      val isInPiPMode by remember { isInPiPModeState }

      // Dynamically load themeName & isDarkMode from database-persisted Settings!
      MyApplicationTheme(
        themeName = userSettings.themeName,
        isDarkMode = userSettings.isDarkMode
      ) {
        
        val sharedPrefs = remember { getSharedPreferences("deriv_radar_prefs", android.content.Context.MODE_PRIVATE) }
        val hasCompletedSetup = remember { sharedPrefs.getBoolean("setup_completed", false) }
        val shouldShowSetup = userSettings.isFirstLaunch && !hasCompletedSetup

        if (isInPiPMode) {
          PiPWidget(viewModel = viewModel)
        } else if (shouldShowSetup) {
          FirstLaunchSetupScreen(viewModel = viewModel)
        } else {
          var currentScreen by remember { mutableStateOf("DASHBOARD") }
          val errorMessage by viewModel.navErrorMessage.collectAsState()

          Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
              Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF0F1015))) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .background(Color.White.copy(alpha = 0.05f))
                      .clickable { viewModel.toggleAccountType() }
                      .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                      .padding(horizontal = 10.dp, vertical = 6.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (userSettings.isDemoAccount) Color(0xFF10B981) else Color(0xFFEF4444))
                    )
                    Text(
                      text = if (userSettings.isDemoAccount) "DEMO ACCOUNT" else "REAL ACCOUNT",
                      color = Color.White,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace
                    )
                  }

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                      .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                      .padding(horizontal = 10.dp, vertical = 6.dp)
                  ) {
                    Text(
                      text = "BALANCE:",
                      color = Color.Gray,
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace
                    )
                    Text(
                      text = String.format("$%.2f", if (userSettings.isDemoAccount) userSettings.demoWalletBalance else userSettings.realWalletBalance),
                      color = MaterialTheme.colorScheme.primary,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace
                    )
                  }
                }

                errorMessage?.let { errorMsg ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .background(Color(0xFFEF4444).copy(alpha = 0.95f))
                      .clickable { viewModel.dismissErrorMessage() }
                      .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "⚠️ $errorMsg",
                      color = Color.White,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace,
                      modifier = Modifier.weight(1f)
                    )
                    Text(
                      text = "[CLOSE]",
                      color = Color.White.copy(alpha = 0.7f),
                      fontSize = 8.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace
                    )
                  }
                }
                
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
              }
            },
            bottomBar = {
            NavigationBar(
              containerColor = Color(0xFF0F1015),
              tonalElevation = 8.dp
            ) {
              NavigationBarItem(
                selected = currentScreen == "DASHBOARD",
                onClick = { currentScreen = "DASHBOARD" },
                icon = { 
                  Icon(
                    imageVector = Icons.Default.Home, 
                    contentDescription = "Radar Dashboard" 
                  ) 
                },
                label = { 
                  Text(
                    text = "RADAR LIST", 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold 
                  ) 
                },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.primary,
                  selectedTextColor = MaterialTheme.colorScheme.primary,
                  unselectedIconColor = Color.Gray,
                  unselectedTextColor = Color.Gray,
                  indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
              )
              NavigationBarItem(
                selected = currentScreen == "PREDICTIONS",
                onClick = { currentScreen = "PREDICTIONS" },
                icon = { 
                  Icon(
                    imageVector = Icons.Default.Star, 
                    contentDescription = "Predictions" 
                  ) 
                },
                label = { 
                  Text(
                    text = "PREDICTIONS", 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold 
                  ) 
                },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.primary,
                  selectedTextColor = MaterialTheme.colorScheme.primary,
                  unselectedIconColor = Color.Gray,
                  unselectedTextColor = Color.Gray,
                  indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
              )
              NavigationBarItem(
                selected = currentScreen == "SIGNALS",
                onClick = { currentScreen = "SIGNALS" },
                icon = { 
                  Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
                    Box(modifier = Modifier.size(16.dp).border(1.5.dp, if (currentScreen == "SIGNALS") MaterialTheme.colorScheme.primary else Color.Gray, CircleShape))
                    Box(modifier = Modifier.size(6.dp).background(if (currentScreen == "SIGNALS") MaterialTheme.colorScheme.primary else Color.Gray, CircleShape))
                  }
                },
                label = { 
                  Text(
                    text = "RADAR SIGNALS", 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold 
                  ) 
                },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.primary,
                  selectedTextColor = MaterialTheme.colorScheme.primary,
                  unselectedIconColor = Color.Gray,
                  unselectedTextColor = Color.Gray,
                  indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
              )
              NavigationBarItem(
                selected = currentScreen == "SETTINGS",
                onClick = { currentScreen = "SETTINGS" },
                icon = { 
                  Icon(
                    imageVector = Icons.Default.Settings, 
                    contentDescription = "Presets" 
                  ) 
                },
                label = { 
                  Text(
                    text = "TUNING KEYS", 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold 
                  ) 
                },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.primary,
                  selectedTextColor = MaterialTheme.colorScheme.primary,
                  unselectedIconColor = Color.Gray,
                  unselectedTextColor = Color.Gray,
                  indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
              )
            }
          }
        ) { innerPadding ->
          
          Crossfade(
            targetState = currentScreen,
            animationSpec = tween(350),
            label = "screen_navigation"
          ) { screen ->
            when (screen) {
              "DASHBOARD" -> {
                DashboardScreen(
                  viewModel = viewModel,
                  onNavigateToPredictions = { currentScreen = "PREDICTIONS" },
                  modifier = Modifier.padding(innerPadding)
                )
              }
              "PREDICTIONS" -> {
                PredictionsScreen(
                  viewModel = viewModel,
                  onNavigateBack = { currentScreen = "DASHBOARD" },
                  modifier = Modifier.padding(innerPadding)
                )
              }
              "SIGNALS" -> {
                SignalsScreen(
                  viewModel = viewModel,
                  modifier = Modifier.padding(innerPadding)
                )
              }
              "SETTINGS" -> {
                SettingsScreen(
                  viewModel = viewModel,
                  modifier = Modifier.padding(innerPadding)
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

@Composable
fun PiPWidget(viewModel: com.example.ui.viewmodel.DigitAnalysisViewModel) {
  val activeSignal by viewModel.activeSignal.collectAsState()
  val countdown by viewModel.signalCountdown.collectAsState()
  val userSettings by viewModel.userSettings.collectAsState()
  val autoSessionProfit by viewModel.autoSessionProfit.collectAsState()
  val targetProfitReached by viewModel.targetProfitReached.collectAsState()
  val stopLossHit by viewModel.stopLossHit.collectAsState()

  var flashBgColor by remember { mutableStateOf(Color.Transparent) }
  var isFlashing by remember { mutableStateOf(false) }

  // 1. Core Entry Vibration + Dynamic entry flash color (Blue = Manual, Green = Auto Pilot)
  LaunchedEffect(activeSignal?.id) {
    if (activeSignal != null) {
      viewModel.triggerSingleVibration()
      isFlashing = true
      val entryColor = if (userSettings.autoTraderEnabled) {
          Color(0xFF10B981) // Green flash for entry in auto trading
      } else {
          Color(0xFF00E5FF) // Blue flash for manual entry
      }
      flashBgColor = entryColor.copy(alpha = 0.6f) // High-contrast solid flash
      kotlinx.coroutines.delay(250)
      flashBgColor = entryColor.copy(alpha = 0.25f) // Morph soft glow
      kotlinx.coroutines.delay(550)
      flashBgColor = Color.Transparent
      isFlashing = false
    }
  }

  // 2. Pulsing Danger status Red flash when countdown <= 5s (only in manual mode, or generally when warning)
  LaunchedEffect(countdown) {
    if (activeSignal != null && !isFlashing) {
      if (countdown <= 5 && countdown > 0) {
        flashBgColor = if (countdown % 2 == 0) {
          Color(0xFFEF4444).copy(alpha = 0.4f) // Blink high
        } else {
          Color(0xFFEF4444).copy(alpha = 0.15f) // Blink low
        }
      } else {
        flashBgColor = Color.Transparent
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF07080C))
      .background(flashBgColor) // Dynamic flash overlay background
      .padding(6.dp),
    contentAlignment = Alignment.Center
  ) {
    if (userSettings.autoTraderEnabled) {
      // --- AUTO TRADING STATE VIEW ---
      Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
      ) {
        // Row 1: Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "PILOT AUTO TRADER 🤖",
            color = Color(0xFF10B981),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
          )

          Text(
            text = "${countdown}S",
            color = if (countdown <= 5) Color(0xFFEF4444) else Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }

        // Row 2: Live Profit Display relative to Target Profit
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
          val profitColor = if (autoSessionProfit >= 0) Color(0xFF34D399) else Color(0xFFEF4444)
          val profitSign = if (autoSessionProfit >= 0) "+" else ""
          Text(
            text = String.format("Net: %s$%.2f", profitSign, autoSessionProfit),
            color = profitColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
          )

          Text(
            text = String.format("Target: $%.2f | SL: -$%.2f", userSettings.autoTraderTakeProfit, userSettings.autoTraderStopLoss),
            color = Color.LightGray.copy(alpha = 0.7f),
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace
          )
        }

        // Row 3: Live Pilot Status Indicators & Safety triggers
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 4.dp, vertical = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          val progressPct = (autoSessionProfit / userSettings.autoTraderTakeProfit).coerceIn(0.0, 1.0) * 100
          Text(
            text = String.format("TP Goal: %.0f%%", progressPct),
            color = Color.Gray,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )

          val pilotStatus = when {
            targetProfitReached -> "LIMIT HIT ✅"
            stopLossHit -> "SL SHUT ⛔"
            else -> "PILOTING ⚡"
          }
          val pilotStatusColor = when {
            targetProfitReached -> Color(0xFF34D399)
            stopLossHit -> Color(0xFFEF4444)
            else -> Color(0xFF00E5FF)
          }
          Text(
            text = pilotStatus,
            color = pilotStatusColor,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    } else {
      // --- MANUAL TRADING STATE VIEW (Flash blue on entry!) ---
      if (activeSignal != null) {
        val signal = activeSignal!!
        Column(
          verticalArrangement = Arrangement.SpaceBetween,
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxSize()
        ) {
          // Line 1: Header + Symbol
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "MANUAL: ${signal.displayName.uppercase()}",
              color = Color(0xFF00E5FF),
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace
            )

            Text(
              text = "${countdown}S",
              color = if (countdown <= 5) Color(0xFFEF4444) else Color.White,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }

          // Line 2: Recommendation Block (Large font matching UNDER / OVER / DIFFERS)
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
          ) {
            val highlightColor = when (signal.contractType) {
              "UNDER" -> Color(0xFF34D399)
              "OVER" -> Color(0xFFFBBF24)
              else -> Color(0xFF818CF8) // Indigo/Blue-purple for DIFFERS
            }
            
            Text(
              text = "${signal.contractType} ${signal.barrier}",
              color = highlightColor,
              fontSize = 17.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace
            )
            
            Text(
              text = "Payout: ${signal.payoutPct} | ${signal.probabilityEst.toInt()}% Win",
              color = Color.LightGray.copy(alpha = 0.7f),
              fontSize = 7.5.sp,
              fontFamily = FontFamily.Monospace
            )
          }

          // Line 3: Candidates list & status flash indicator
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(4.dp))
              .background(Color.White.copy(alpha = 0.05f))
              .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "HOT: ${signal.candidates.joinToString(",")}",
              color = Color.Gray,
              fontSize = 7.5.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )

            Text(
              text = "FLASH BLUE ⚡",
              color = Color(0xFF00E5FF),
              fontSize = 7.5.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      } else {
        Text(
          text = "TACTICAL RADAR ACTIVE\nAwaiting manual signals...",
          color = Color.Gray,
          fontSize = 8.sp,
          fontFamily = FontFamily.Monospace,
          textAlign = TextAlign.Center,
          lineHeight = 11.sp
        )
      }
    }
  }
}
