package com.example.routineforge.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.routineforge.MainActivity
import com.example.routineforge.data.RoutineRepository
import com.example.routineforge.data.ScheduledRoutine
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules reminder alarms. Intents only carry the reminder id; the receiver reads the
 * reminder from the repository, so edits apply immediately and deleted reminders never fire.
 */
object CalendarAlarmScheduler {

    const val ACTION_CALENDAR_REMINDER = "com.example.routineforge.action.CALENDAR_REMINDER"
    const val ACTION_CALENDAR_MARK_DONE = "com.example.routineforge.action.CALENDAR_MARK_DONE"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_IS_PRE_REMINDER = "extra_is_pre_reminder"

    /** (Re)schedules the next occurrence. Returns its start time, or null if nothing was scheduled. */
    fun scheduleAlarmsForTask(
        context: Context,
        task: ScheduledRoutine,
        after: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime? {
        cancelAlarmsForTask(context, task.id)
        if (task.isCompleted) return null
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return null
        val clean = task.sanitized()
        val start = clean.nextOccurrence(after) ?: return null
        val startMillis = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        setAlarm(context, alarmManager, startMillis, reminderIntent(context, task.id, isPre = false))
        if (clean.remindBeforeMinutes > 0) {
            val preMillis = startMillis - clean.remindBeforeMinutes * 60_000L
            if (preMillis > System.currentTimeMillis()) {
                setAlarm(context, alarmManager, preMillis, reminderIntent(context, task.id, isPre = true))
            }
        }
        return start
    }

    fun cancelAlarmsForTask(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (isPre in listOf(true, false)) {
            val pi = PendingIntent.getBroadcast(
                context,
                requestCode(taskId, isPre),
                Intent(context, CalendarReminderReceiver::class.java).setAction(ACTION_CALENDAR_REMINDER),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: continue
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    fun rescheduleAllUpcoming(context: Context) {
        RoutineRepository.getInstance(context).scheduledRoutines.value
            .forEach { scheduleAlarmsForTask(context, it) }
    }

    fun cancelAll(context: Context) {
        RoutineRepository.getInstance(context).scheduledRoutines.value
            .forEach { cancelAlarmsForTask(context, it.id) }
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    private fun setAlarm(context: Context, alarmManager: AlarmManager, triggerAtMillis: Long, pi: PendingIntent) {
        try {
            if (canScheduleExact(context)) {
                // Alarm-clock alarms are exempt from Doze batching, so reminders arrive on time.
                val show = PendingIntent.getActivity(
                    context,
                    0,
                    MainActivity.launchIntent(context, MainActivity.NAV_SCHEDULE),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, show), pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun reminderIntent(context: Context, taskId: String, isPre: Boolean): PendingIntent {
        val intent = Intent(context, CalendarReminderReceiver::class.java).apply {
            action = ACTION_CALENDAR_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_IS_PRE_REMINDER, isPre)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(taskId, isPre),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(taskId: String, isPre: Boolean): Int = taskId.hashCode() * 31 + if (isPre) 1 else 2
}
