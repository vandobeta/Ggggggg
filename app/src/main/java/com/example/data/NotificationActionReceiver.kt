package com.example.data

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.AppDatabase
import com.example.data.db.PracticeBet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val signalDesc = intent.getStringExtra("SIGNAL_DESC") ?: "General Signal"
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", 4452)

        if (action == "ACTION_RECORD_WIN" || action == "ACTION_RECORD_LOSS") {
            val isWin = action == "ACTION_RECORD_WIN"
            
            // Cancel notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)

            // Save to Room DB asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val bet = PracticeBet(
                        timestamp = java.lang.System.currentTimeMillis(),
                        signalDescription = signalDesc,
                        isWin = isWin
                    )
                    db.practiceBetDao().insertBet(bet)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
