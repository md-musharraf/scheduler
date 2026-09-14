package com.example.routineforge.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.routineforge.MainActivity
import com.example.routineforge.R
import com.example.routineforge.data.RoutineRepository
import com.example.routineforge.util.HapticFeedbackManager

class CalendarReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "calendar_routine_reminders_channel_v1"
        const val CHANNEL_NAME = "Routine Calendar Reminders"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            CalendarAlarmScheduler.ACTION_CALENDAR_REMINDER -> {
                val taskId = intent.getStringExtra(CalendarAlarmScheduler.EXTRA_TASK_ID) ?: return
                val taskTitle = intent.getStringExtra(CalendarAlarmScheduler.EXTRA_TASK_TITLE) ?: "Task"
                val categoryName = intent.getStringExtra(CalendarAlarmScheduler.EXTRA_CATEGORY_NAME) ?: "Routine"
                val categoryEmoji = intent.getStringExtra(CalendarAlarmScheduler.EXTRA_CATEGORY_EMOJI) ?: "🎯"
                val isPreReminder = intent.getBooleanExtra(CalendarAlarmScheduler.EXTRA_IS_PRE_REMINDER, false)
                val patternName = intent.getStringExtra(CalendarAlarmScheduler.EXTRA_VIBRATION_PATTERN) ?: HapticFeedbackManager.PATTERN_NOTHING_PULSE
                val routineId = intent.getStringExtra(CalendarAlarmScheduler.EXTRA_ROUTINE_ID) ?: ""

                handleAlarmAlert(
                    context = context,
                    taskId = taskId,
                    taskTitle = taskTitle,
                    categoryName = categoryName,
                    categoryEmoji = categoryEmoji,
                    isPreReminder = isPreReminder,
                    patternName = patternName,
                    routineId = routineId
                )
            }

            CalendarAlarmScheduler.ACTION_CALENDAR_MARK_DONE -> {
                val taskId = intent.getStringExtra(CalendarAlarmScheduler.EXTRA_TASK_ID) ?: return
                val repository = RoutineRepository.getInstance(context)
                repository.toggleScheduledCompleted(taskId)

                // Cancel notification safely
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(taskId.hashCode())
            }
        }
    }

    private fun handleAlarmAlert(
        context: Context,
        taskId: String,
        taskTitle: String,
        categoryName: String,
        categoryEmoji: String,
        isPreReminder: Boolean,
        patternName: String,
        routineId: String
    ) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "RoutineForge:CalendarAlarmWakeLock"
        )
        try {
            wakeLock?.acquire(10_000L) // 10s maximum timeout

            // 1) Trigger authoritative haptic pattern via DRY HapticFeedbackManager
            HapticFeedbackManager.vibratePattern(context, patternName, isPreReminder)

            // 2) Show High-Priority Heads-Up Notification
            createNotificationChannel(context)
            showReminderNotification(
                context = context,
                taskId = taskId,
                taskTitle = taskTitle,
                categoryName = categoryName,
                categoryEmoji = categoryEmoji,
                isPreReminder = isPreReminder,
                routineId = routineId
            )
        } finally {
            if (wakeLock?.isHeld == true) {
                try {
                    wakeLock.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "High priority vibration & heads-up alarms for scheduled tasks"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 400, 200, 400)
                    setSound(soundUri, audioAttributes)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun showReminderNotification(
        context: Context,
        taskId: String,
        taskTitle: String,
        categoryName: String,
        categoryEmoji: String,
        isPreReminder: Boolean,
        routineId: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // Tap on notification -> Open app securely with explicit package
        val openIntent = Intent(context, MainActivity::class.java).apply {
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (routineId.isNotBlank()) {
                putExtra("extra_start_routine_id", routineId)
            } else {
                putExtra("target_nav", "CALENDAR")
            }
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark Done directly with explicit package and class
        val markDoneIntent = Intent(context, CalendarReminderReceiver::class.java).apply {
            setPackage(context.packageName)
            action = CalendarAlarmScheduler.ACTION_CALENDAR_MARK_DONE
            putExtra(CalendarAlarmScheduler.EXTRA_TASK_ID, taskId)
        }
        val markDonePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode() + 500,
            markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isPreReminder) {
            "⏰ In 1 Minute: $taskTitle"
        } else {
            "⚡ Routine Time: $taskTitle"
        }

        val body = if (isPreReminder) {
            "$categoryEmoji $categoryName starts in 1 min. Get prepared!"
        } else {
            "$categoryEmoji $categoryName • Starting now. Stay consistent!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.mipmap.ic_launcher,
                if (routineId.isNotBlank()) "START NOW" else "OPEN CALENDAR",
                contentPendingIntent
            )
            .addAction(
                R.mipmap.ic_launcher,
                "MARK DONE",
                markDonePendingIntent
            )
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }
}
