package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val signalStrengthMinimum: Float = 85.0f,
    val signalsPerDayLimit: Int = 50,
    val themeName: String = "Google Indigo", // "Google Indigo", "Google Emerald", "Google Cosmic", "Pixel Sunrise"
    val alarmsEnabled: Boolean = true,
    val alarmIntervalMinutes: Int = 15,
    val droughtThreshold: Float = 12.0f,
    val macroLookback: Int = 300,
    val microLookback: Int = 8,
    val showCompassPointer: Boolean = true,
    val smoothingIntensity: Float = 0.65f, // Range from 0.0f to 0.9f
    val ticksToCompare: Int = 300,        // Range from 10 to 1000
    
    // First Time Launch & Theme Settings
    val isFirstLaunch: Boolean = true,
    val traderName: String = "",
    val capital: Double = 1000.0,
    val currency: String = "USD",
    val goal: String = "",
    val isDarkMode: Boolean = true,
    val riskProfile: String = "LESS_RISKY", // "RISKY" or "LESS_RISKY"
    val virtualTradeCloseTicks: Int = 1,    // Number of ticks used to close virtual trades (1 to 10)
    val signalNotificationCooldownSecs: Int = 30, // Cooldown between push notifications in seconds
    val customContract: String = "ALL",     // "ALL" or precise format (e.g., "OVER 3", "UNDER 5")
    val triggerLowerOdds: Boolean = true,
    val triggerLowerEvens: Boolean = true,
    val triggerHigherOdds: Boolean = true,
    val triggerHigherEvens: Boolean = true,
    val alertBehavior: String = "VIB_AND_NOTIF", // "VIB_ONLY", "NOTIF_ONLY", "VIB_AND_NOTIF"
    val cushionSpacing: Int = 2, // Safety barrier cushion spacing (1 to 4)
    val derivToken: String = "",
    val autoTraderEnabled: Boolean = false,
    val autoTraderStake: Double = 1.0,
    val autoTraderTakeProfit: Double = 10.0,
    val autoTraderStopLoss: Double = 15.0,
    val autoTraderTrailingStopLoss: Boolean = false,
    val autoTraderCompoundingStake: Boolean = true,
    val derivWalletBalance: Double = 1000.0,
    val isDemoAccount: Boolean = true,
    val demoWalletBalance: Double = 10000.0,
    val realWalletBalance: Double = 100.0,
    val derivAppId: String = "33sKaNullz3jmWQs7OXxZ",
    val sessionsPerDay: Int = 3,
    
    // Explicit requested settings table fields
    val currentAccountType: String = "demo", // "demo" or "real"
    val stake: Double = 5.0,
    val tradeLogs: String = "",
    val patToken: String = "",
    val autoTraderStatus: Int = 0, // 1 for on, 0 for off
    val maxSignalsPerDay: Int = 50,
    val tradeSessionsPerDay: Int = 3,
    val aiProvider: String = "gemini",
    val aiToken: String = ""
)
