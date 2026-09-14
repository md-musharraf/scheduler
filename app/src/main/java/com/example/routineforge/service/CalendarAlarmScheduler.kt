package com.example.routineforge.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.routineforge.data.RoutineRepository
import com.example.routineforge.data.ScheduledRoutine
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object CalendarAlarmScheduler {

    const val ACTION_CALENDAR_REMINDER = "com.example.routineforge.action.CALENDAR_REMINDER"
    const val ACTION_CALENDAR_MARK_DONE = "com.example.routineforge.action.CALENDAR_MARK_DONE"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_CATEGORY_NAME = "extra_category_name"
    const val EXTRA_CATEGORY_EMOJI = "extra_category_emoji"
    const val EXTRA_IS_PRE_REMINDER = "extra_is_pre_reminder"
    const val EXTRA_VIBRATION_PATTERN = "extra_vibration_pattern"
    const val EXTRA_ROUTINE_ID = "extra_routine_id"
    const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"

    fun scheduleAlarmsForTask(context: Context, task: ScheduledRoutine) {
        if (task.isCompleted) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val date = LocalDate.ofEpochDay(task.dateEpochDay)
        val parts = task.timeOfDay.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val targetDateTime = LocalDateTime.of(date.year, date.monthValue, date.dayOfMonth, hour, min, 0)
        val startMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val nowMillis = System.currentTimeMillis()

        // 1) Pre-reminder: 1 min before (or customized remindBeforeMinutes)
        if (task.remindBeforeMinutes > 0) {
            val preReminderMillis = startMillis - (task.remindBeforeMinutes * 60 * 1000L)
            if (preReminderMillis > nowMillis) {
                val preIntent = createAlarmIntent(context, task, isPreReminder = true)
                val prePendingIntent = PendingIntent.getBroadcast(
                    context,
                    getPreReminderRequestCode(task.id),
                    preIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setExactAlarm(alarmManager, preReminderMillis, prePendingIntent)
            }
        }

        // 2) At-time reminder: exactly at task start time
        if (task.remindAtTime) {
            if (startMillis > nowMillis) {
                val atTimeIntent = createAlarmIntent(context, task, isPreReminder = false)
                val atTimePendingIntent = PendingIntent.getBroadcast(
                    context,
                    getAtTimeRequestCode(task.id),
                    atTimeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setExactAlarm(alarmManager, startMillis, atTimePendingIntent)
            }
        }
    }

    fun cancelAlarmsForTask(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Cancel Pre-reminder
        val preIntent = Intent(context, CalendarReminderReceiver::class.java).apply {
            action = ACTION_CALENDAR_REMINDER
        }
        val prePendingIntent = PendingIntent.getBroadcast(
            context,
            getPreReminderRequestCode(taskId),
            preIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (prePendingIntent != null) {
            alarmManager.cancel(prePendingIntent)
            prePendingIntent.cancel()
        }

        // Cancel At-time reminder
        val atTimePendingIntent = PendingIntent.getBroadcast(
            context,
            getAtTimeRequestCode(taskId),
            preIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (atTimePendingIntent != null) {
            alarmManager.cancel(atTimePendingIntent)
            atTimePendingIntent.cancel()
        }
    }

    fun rescheduleAllUpcoming(context: Context) {
        val repository = RoutineRepository.getInstance(context)
        val allScheduled = repository.scheduledRoutines.value
        allScheduled.forEach { task ->
            if (!task.isCompleted) {
                scheduleAlarmsForTask(context, task)
            }
        }
    }

    private fun setExactAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (_: Exception) {
            }
        } catch (_: Exception) {
        }
    }

    private fun createAlarmIntent(context: Context, task: ScheduledRoutine, isPreReminder: Boolean): Intent {
        return Intent(context, CalendarReminderReceiver::class.java).apply {
            action = ACTION_CALENDAR_REMINDER
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.routineTitle.ifBlank { task.categoryName })
            putExtra(EXTRA_CATEGORY_NAME, task.categoryName)
            putExtra(EXTRA_CATEGORY_EMOJI, task.categoryEmoji)
            putExtra(EXTRA_IS_PRE_REMINDER, isPreReminder)
            putExtra(EXTRA_VIBRATION_PATTERN, task.vibrationPattern)
            putExtra(EXTRA_ROUTINE_ID, task.routineId)
            putExtra(EXTRA_DURATION_MINUTES, task.durationMinutes)
        }
    }

    private fun getPreReminderRequestCode(taskId: String): Int = (taskId.hashCode() * 31) + 1
    private fun getAtTimeRequestCode(taskId: String): Int = (taskId.hashCode() * 31) + 2
}
