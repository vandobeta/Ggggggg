package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SessionReminderReceiver : BroadcastReceiver() {
    private val TAG = "SessionReminderReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val sessionIndex = intent.getIntExtra("SESSION_INDEX", 1)
        Log.d(TAG, "Received Session Reminder alarm for Session #$sessionIndex")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val settings = db.appSettingsDao().getSettingsOneShot()
                val traderName = settings?.traderName ?: ""
                val totalSessions = settings?.sessionsPerDay ?: 3
                
                ReminderNotificationHelper.showReminderNotification(
                    context = context,
                    title = "Digit Radar Session Due!",
                    content = "Your scheduled daily trading session #$sessionIndex of $totalSessions is now active. Let's analyze index behaviors!",
                    traderName = traderName
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error showing session notification: ${e.message}")
                // Fallback to simpler notification
                ReminderNotificationHelper.showReminderNotification(
                    context = context,
                    title = "Digit Radar Session Due!",
                    content = "A scheduled daily trading session is ready. Let's analyze digit ticks!"
                )
            }
        }
    }
}
