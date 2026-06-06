package com.example.data.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SettingsRepository(private val dao: AppSettingsDao) {
    
    val settingsFlow: Flow<AppSettings?> = dao.getSettings()

    suspend fun getSettingsOneShot(): AppSettings {
        return withContext(Dispatchers.IO) {
            dao.getSettingsOneShot() ?: AppSettings()
        }
    }

    suspend fun saveSettings(settings: AppSettings) {
        val isDemo = if (settings.currentAccountType.lowercase().trim() == "real") false 
                     else if (settings.currentAccountType.lowercase().trim() == "demo") true 
                     else settings.isDemoAccount
        val accountType = if (isDemo) "demo" else "real"
        
        val autoTraderOn = if (settings.autoTraderStatus == 1) true 
                           else if (settings.autoTraderStatus == 0) false 
                           else settings.autoTraderEnabled
        val autoStatus = if (autoTraderOn) 1 else 0
        
        val token = if (settings.patToken.isNotEmpty()) settings.patToken else settings.derivToken
        val appId = if (settings.derivAppId.isNotEmpty()) settings.derivAppId else "33sKaNullz3jmWQs7OXxZ"
        
        val maxSignals = if (settings.maxSignalsPerDay != 50) settings.maxSignalsPerDay else settings.signalsPerDayLimit
        val maxSessions = if (settings.tradeSessionsPerDay != 3) settings.tradeSessionsPerDay else settings.sessionsPerDay
        
        val synced = settings.copy(
            isDemoAccount = isDemo,
            currentAccountType = accountType,
            autoTraderEnabled = autoTraderOn,
            autoTraderStatus = autoStatus,
            derivToken = token,
            patToken = token,
            derivAppId = appId,
            signalsPerDayLimit = maxSignals,
            maxSignalsPerDay = maxSignals,
            sessionsPerDay = maxSessions,
            tradeSessionsPerDay = maxSessions
        )
        withContext(Dispatchers.IO) {
            dao.insertOrUpdate(synced)
        }
    }
}
