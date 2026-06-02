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
        
        if (isInPiPMode) {
          PiPWidget(viewModel = viewModel)
        } else if (userSettings.isFirstLaunch) {
          FirstLaunchSetupScreen(viewModel = viewModel)
        } else {
          var currentScreen by remember { mutableStateOf("DASHBOARD") }

          Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
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

  var flashBgColor by remember { mutableStateOf(Color.Transparent) }
  var isFlashingGreen by remember { mutableStateOf(false) }

  // 1. Core Entry Vibration + Bright Green flash when activeSignal changes
  LaunchedEffect(activeSignal?.id) {
    if (activeSignal != null) {
      viewModel.triggerSingleVibration()
      isFlashingGreen = true
      flashBgColor = Color(0xFF10B981).copy(alpha = 0.6f) // High-contrast solid flash
      kotlinx.coroutines.delay(250)
      flashBgColor = Color(0xFF10B981).copy(alpha = 0.25f) // Morph soft glow
      kotlinx.coroutines.delay(550)
      flashBgColor = Color.Transparent
      isFlashingGreen = false
    }
  }

  // 2. Pulsing Danger status Red flash when countdown <= 5s
  LaunchedEffect(countdown) {
    if (activeSignal != null && !isFlashingGreen) {
      if (countdown <= 5 && countdown > 0) {
        flashBgColor = if (countdown % 2 == 0) {
          Color(0xFFEF4444).copy(alpha = 0.45f) // Blink high
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
      .background(Color(0xFF090A0E))
      .background(flashBgColor) // Dynamic flash overlay background
      .padding(8.dp),
    contentAlignment = Alignment.Center
  ) {
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
            text = signal.displayName.uppercase(),
            color = Color(0xFF38BDF8),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
          )

          Text(
            text = "${countdown}S",
            color = if (countdown <= 5) Color(0xFFEF4444) else Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
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
            fontSize = 18.sp,
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

        // Line 3: Candidates list & warning indicator
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
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )

          Text(
            text = if (countdown <= 5) "DANGER ⛔" else "ACTIVE 📡",
            color = if (countdown <= 5) Color(0xFFEF4444) else Color(0xFF10B981),
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    } else {
      Text(
        text = "TACTICAL RADAR ACTIVE\nAwaiting signal stream...",
        color = Color.Gray,
        fontSize = 8.sp,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center,
        lineHeight = 11.sp
      )
    }
  }
}
