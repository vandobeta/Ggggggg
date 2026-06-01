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
    val isDarkMode: Boolean = true
)
