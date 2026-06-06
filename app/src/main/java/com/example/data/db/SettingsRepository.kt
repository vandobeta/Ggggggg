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
        val isDemo = settings.isDemoAccount
        val accountType = if (isDemo) "demo" else "real"
        
        val autoTraderOn = settings.autoTraderEnabled
        val autoStatus = if (autoTraderOn) 1 else 0
        
        val token = settings.derivToken
        val appId = if (settings.derivAppId.isNotEmpty()) settings.derivAppId else "33sKaNullz3jmWQs7OXxZ"
        
        val maxSignals = settings.signalsPerDayLimit
        val maxSessions = settings.sessionsPerDay
        
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
