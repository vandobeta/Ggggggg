package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object ReminderNotificationHelper {
    private const val CHANNEL_ID = "deriv_signal_reminders"
    private const val CHANNEL_NAME = "Algo-Radar Signal Reminders"
    private const val NOTIFICATION_ID = 4452

    fun showReminderNotification(
        context: Context, 
        title: String, 
        content: String,
        traderName: String = "",
        signalDescription: String? = null
    ) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for daily trade signal reminders"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val personalizedTitle = if (traderName.isNotBlank()) "[$traderName] $title" else title

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(personalizedTitle)
                .setContentText(content)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(0xFF3F51B5.toInt()) // Google Indigo

            if (signalDescription != null) {
                val winIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = "ACTION_RECORD_WIN"
                    putExtra("SIGNAL_DESC", signalDescription)
                    putExtra("NOTIFICATION_ID", NOTIFICATION_ID)
                }
                val winPI = PendingIntent.getBroadcast(
                    context, 101, winIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val lossIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = "ACTION_RECORD_LOSS"
                    putExtra("SIGNAL_DESC", signalDescription)
                    putExtra("NOTIFICATION_ID", NOTIFICATION_ID)
                }
                val lossPI = PendingIntent.getBroadcast(
                    context, 102, lossIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                builder.addAction(android.R.drawable.checkbox_on_background, "WIN 🟢", winPI)
                builder.addAction(android.R.drawable.checkbox_off_background, "LOSS 🔴", lossPI)
            }

            notificationManager.notify(NOTIFICATION_ID, builder.build())
            Log.d("NotificationHelper", "Reminder notification pushed successfully.")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error throwing notification: ${e.message}")
        }
    }
}
