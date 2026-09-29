package com.example.routineforge.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.example.routineforge.MainActivity
import com.example.routineforge.R
import com.example.routineforge.data.RoutineRepository
import com.example.routineforge.data.ScheduledRoutine
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class CalendarReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val taskId = intent.getStringExtra(CalendarAlarmScheduler.EXTRA_TASK_ID) ?: return
        val repository = RoutineRepository.getInstance(context)
        val task = repository.scheduledRoutines.value.firstOrNull { it.id == taskId }
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        when (intent.action) {
            CalendarAlarmScheduler.ACTION_CALENDAR_REMINDER -> {
                if (task == null || task.isCompleted) return
                val isPre = intent.getBooleanExtra(CalendarAlarmScheduler.EXTRA_IS_PRE_REMINDER, false)
                nm.notify(notificationId(taskId), buildNotification(context, nm, task.sanitized(), isPre))
                if (!isPre && task.isRepeating) {
                    // Queue the next occurrence (+1 min so this one isn't picked again).
                    CalendarAlarmScheduler.scheduleAlarmsForTask(context, task, LocalDateTime.now().plusMinutes(1))
                }
            }

            CalendarAlarmScheduler.ACTION_CALENDAR_MARK_DONE -> {
                nm.cancel(notificationId(taskId))
                // Repeating reminders stay active; "Done" just dismisses this occurrence.
                if (task != null && !task.isRepeating) {
                    repository.setScheduledCompleted(taskId, true)
                    CalendarAlarmScheduler.cancelAlarmsForTask(context, taskId)
                }
            }
        }
    }

    private fun buildNotification(
        context: Context,
        nm: NotificationManager,
        task: ScheduledRoutine,
        isPre: Boolean
    ): android.app.Notification {
        val title = task.routineTitle.ifBlank { task.categoryName }
        val time = LocalTime.parse(task.timeOfDay).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        val body = listOf(if (isPre) "Starts at $time" else "Now · $time", task.note)
            .filter { it.isNotBlank() }
            .joinToString("\n")

        val open = PendingIntent.getActivity(
            context,
            task.id.hashCode(),
            MainActivity.launchIntent(context, MainActivity.NAV_SCHEDULE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val done = PendingIntent.getBroadcast(
            context,
            task.id.hashCode() + 500,
            Intent(context, CalendarReminderReceiver::class.java).apply {
                action = CalendarAlarmScheduler.ACTION_CALENDAR_MARK_DONE
                putExtra(CalendarAlarmScheduler.EXTRA_TASK_ID, task.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelFor(nm, task.soundEnabled, task.vibrationEnabled))
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle(if (isPre) "In ${task.remindBeforeMinutes} min: $title" else title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(open)

        if (task.durationMinutes > 0) {
            val startTimer = PendingIntent.getActivity(
                context,
                task.id.hashCode() + 700,
                MainActivity.timerIntent(context, title, task.durationMinutes * 60),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_notif_play, "Start ${task.durationMinutes}-min timer", startTimer)
        }
        builder.addAction(R.drawable.ic_notif_close, "Done", done)
        return builder.build()
    }

    /** Channel sound/vibration can't change after creation, so each alert style gets its own channel. */
    private fun channelFor(nm: NotificationManager, sound: Boolean, vibrate: Boolean): String {
        val (id, name) = when {
            sound && vibrate -> "reminders_sound_vibe" to "Reminders"
            vibrate -> "reminders_vibe_only" to "Reminders (vibrate only)"
            else -> "reminders_sound_only" to "Reminders (sound only)"
        }
        if (nm.getNotificationChannel(id) == null) {
            nm.deleteNotificationChannel(LEGACY_CHANNEL_ID)
            nm.createNotificationChannel(
                NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Date and time reminders you schedule"
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                    enableVibration(vibrate)
                    if (vibrate) vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
                    if (sound) {
                        setSound(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .build()
                        )
                    } else {
                        setSound(null, null)
                    }
                }
            )
        }
        return id
    }

    private fun notificationId(taskId: String) = taskId.hashCode()

    private companion object {
        const val LEGACY_CHANNEL_ID = "calendar_routine_reminders_channel_v1"
    }
}
