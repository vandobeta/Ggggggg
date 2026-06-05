package com.example.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

object AlarmSchedulerHelper {
    private const val TAG = "AlarmSchedulerHelper"
    private const val BASE_REQUEST_CODE = 2000

    fun scheduleSessionAlarms(context: Context, sessionsCount: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        // 1. Cancel any existing session alarms (up to 10 slots to ensure complete cleanup)
        for (i in 0 until 10) {
            val intent = Intent(context, SessionReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                BASE_REQUEST_CODE + i,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d(TAG, "Cancelled old alarm slot at index $i")
            }
        }
        
        if (sessionsCount <= 0) {
            Log.d(TAG, "Sessions count is 0 or less. Alarms cleared.")
            return
        }

        Log.d(TAG, "Scheduling $sessionsCount daily session reminder alarms...")

        // Waking hours range: 9:00 AM (9h) to 9:00 PM (21h) -> 12-hour window
        for (i in 0 until sessionsCount) {
            val targetHour = if (sessionsCount == 1) {
                12.0 // Single session per day is scheduled at noon
            } else {
                9.0 + i * (12.0 / (sessionsCount - 1))
            }

            val hourInt = targetHour.toInt()
            val minuteInt = ((targetHour - hourInt) * 60).toInt()

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hourInt)
                set(Calendar.MINUTE, minuteInt)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If scheduled time has already passed today, shift it to tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val intent = Intent(context, SessionReminderReceiver::class.java).apply {
                putExtra("SESSION_INDEX", i + 1)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                BASE_REQUEST_CODE + i,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                // Use standard daily repeating alarms
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
                Log.d(TAG, "Alarm #$i scheduled successfully for ${hourInt}:${String.format("%02d", minuteInt)} daily.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule alarm $i: ${e.message}")
            }
        }
    }
}
