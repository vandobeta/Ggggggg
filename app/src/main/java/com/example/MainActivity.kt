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
  val activePacket by viewModel.selectedPacket.collectAsState()
  val notifierContract by viewModel.selectedNotifierContract.collectAsState()
  val backtestBets by viewModel.backtestBetsCount.collectAsState()
  val backtestWins by viewModel.backtestWinsCount.collectAsState()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF090A0E))
      .padding(8.dp),
    contentAlignment = Alignment.Center
  ) {
    if (activePacket != null) {
      val packet = activePacket!!
      Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Row 1: Header + Symbol
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = packet.displayName.uppercase(),
            color = Color(0xFF38BDF8),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )

          Text(
            text = "PiP Monitor",
            color = Color.LightGray.copy(alpha = 0.5f),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
          )
        }

        // Row 2: Last digit stream
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          val lastDigits = packet.tickHistory.takeLast(5)
          lastDigits.forEachIndexed { i, d ->
            val isLatest = i == lastDigits.lastIndex
            Box(
              modifier = Modifier
                .size(if (isLatest) 20.dp else 15.dp)
                .clip(CircleShape)
                .background(if (isLatest) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.08f))
                .border(
                  1.dp,
                  if (isLatest) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.2f),
                  CircleShape
                ),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = d.toString(),
                color = if (isLatest) Color.Black else Color.White,
                fontSize = if (isLatest) 10.sp else 8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }

        // Row 3: Automated filter representation
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "NOTIFIER",
              color = Color.Gray,
              fontSize = 7.sp,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = notifierContract ?: "ALL ACTIVE",
              color = MaterialTheme.colorScheme.primary,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // Compute selected contract strategic recommendation
          val displayRecommendCandidate = if (packet.predictionsList.isNotEmpty()) {
            val best = packet.predictionsList.first()
            if (notifierContract == "OVER") {
              "Over ${if (best.digit >= 6) 6 else 5}"
            } else if (notifierContract == "UNDER") {
              "Under ${if (best.digit <= 4) 4 else 5}"
            } else {
              "D${best.digit} [${String.format("%.0f%%", best.confidence)}]"
            }
          } else {
            "..."
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "REVERSION",
              color = Color.Gray,
              fontSize = 7.sp,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = displayRecommendCandidate,
              color = Color.White,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        // Row 4: Backtest Win Ratio for current selected index
        val currentBestDigit = packet.predictionsList.firstOrNull()?.digit ?: 0
        val wins = backtestWins[currentBestDigit] ?: 0
        val bets = backtestBets[currentBestDigit] ?: 0
        val winRate = if (bets > 0) (wins.toFloat() / bets.toFloat() * 100f) else 0f

        Surface(
          shape = RoundedCornerShape(4.dp),
          color = Color.White.copy(alpha = 0.05f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "BACKTEST WR D$currentBestDigit:",
              color = Color.White.copy(alpha = 0.6f),
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = if (bets > 0) String.format("%.1f%%", winRate) else "No tx yet",
              color = if (winRate >= 70f) Color(0xFF10B981) else if (winRate >= 50f) Color(0xFFFBBF24) else Color.White,
              fontSize = 8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }
    } else {
      Text(
        text = "PiP Monitor Active.\nAwaiting stream...",
        color = Color.Gray,
        fontSize = 8.sp,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center
      )
    }
  }
}
