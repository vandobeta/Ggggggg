package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    private val TAG = "BootCompletedReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d(TAG, "Device boot completed. Restoring session reminder schedules...")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    Log.d(TAG, "Device boot completed. Clearing stale ticks in local database...")
                    db.tickOccurrenceDao().clearTicks()
                    
                    val settings = db.appSettingsDao().getSettingsOneShot()
                    if (settings != null) {
                        val sessionsCount = settings.sessionsPerDay
                        Log.d(TAG, "Restoring $sessionsCount session alarms from database after system boot.")
                        AlarmSchedulerHelper.scheduleSessionAlarms(context, sessionsCount)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring alarms on boot: ${e.message}")
                }
            }
        }
    }
}
