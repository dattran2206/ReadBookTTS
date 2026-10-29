package com.vietreader.tts.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object ReadingReminderScheduler {
    private const val TAG = "ReadingReminderScheduler"
    const val REQUEST_CODE_REMINDER = 2002

    fun scheduleReminder(
        context: Context,
        enabled: Boolean,
        hour: Int,
        minute: Int,
        days: Set<Int>
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReadingReminderReceiver::class.java).apply {
            action = ReadingReminderReceiver.ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled || days.isEmpty()) {
            Log.d(TAG, "Cancelling reading reminder alarm")
            alarmManager.cancel(pendingIntent)
            return
        }

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the time today has already passed, schedule for tomorrow
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        Log.d(TAG, "Scheduling next reminder at ${target.time}")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    target.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    target.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission not granted, falling back to setAndAllowWhileIdle", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                target.timeInMillis,
                pendingIntent
            )
        }
    }
}
