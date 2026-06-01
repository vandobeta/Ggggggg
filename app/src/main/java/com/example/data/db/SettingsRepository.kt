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
        withContext(Dispatchers.IO) {
            dao.insertOrUpdate(settings)
        }
    }
}
