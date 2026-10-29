package com.vietreader.tts.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vietreader.tts.MainActivity
import com.vietreader.tts.R
import com.vietreader.tts.data.local.WordwaveLocalStorage
import java.util.Calendar

class ReadingReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReadingReminderReceiver"
        const val ACTION_TRIGGER_REMINDER = "com.vietreader.tts.action.TRIGGER_REMINDER"
        const val CHANNEL_ID = "wordwave_reading_reminder_channel"
        const val NOTIFICATION_ID = 3003
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_TRIGGER_REMINDER) return

        Log.i(TAG, "Received reminder trigger broadcast")

        val storage = WordwaveLocalStorage(context)
        val settings = storage.loadAppSettings()

        // Re-schedule for next day regardless of whether today fired
        ReadingReminderScheduler.scheduleReminder(
            context = context,
            enabled = settings.reminderEnabled,
            hour = settings.reminderHour,
            minute = settings.reminderMinute,
            days = settings.reminderDays
        )

        if (!settings.reminderEnabled) {
            Log.d(TAG, "Reminders are disabled in settings")
            return
        }

        // Check if today matches selected days of week
        // 1 = Monday, 2 = Tuesday, ... 7 = Sunday
        val calendar = Calendar.getInstance()
        val currentDay = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }

        if (currentDay !in settings.reminderDays) {
            Log.d(TAG, "Today (day $currentDay) is not in selected reminder days: ${settings.reminderDays}")
            return
        }

        showNotification(context, settings.userName)
    }

    private fun showNotification(context: Context, userName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nhắc nhở đọc sách",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Thông báo nhắc nhở rèn luyện thói quen đọc sách hàng ngày"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = runCatching {
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        }.getOrNull()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .apply {
                if (largeIcon != null) setLargeIcon(largeIcon)
            }
            .setContentTitle("Đến giờ đọc sách rồi! 📖")
            .setContentText("Chào $userName, hãy dành chút thời gian thư giãn cùng những trang sách hôm nay nhé.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Chào $userName, hãy dành 15-30 phút thư giãn cùng giọng đọc thông minh của Wordwave hôm nay nhé!")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
