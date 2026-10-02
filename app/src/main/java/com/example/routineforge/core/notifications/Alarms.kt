package com.example.routineforge.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * AlarmManager wrapper. Each alarm has a stable request code, so re-scheduling replaces the
 * previous alarm instead of adding a duplicate.
 */
object Alarms {
    const val SLOT_FIRE = 0
    const val SLOT_SNOOZE = 1
    const val SLOT_DONE = 2
    const val SLOT_SNOOZE_ACTION = 3
    const val SLOT_OPEN = 4
    private const val TIMER_REQUEST_CODE = 9001
    private const val WARN_REQUEST_CODE = 9002

    fun requestCode(reminderId: Long, slot: Int): Int = ((reminderId % 100_000_000L).toInt()) * 8 + slot

    private fun set(context: Context, at: Long, pi: PendingIntent) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        try {
            if (Notifier.canScheduleExact(context)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun cancel(context: Context, requestCode: Int, intent: Intent) {
        val pi = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        context.getSystemService(AlarmManager::class.java)?.cancel(pi)
        pi.cancel()
    }

    private fun reminderIntent(context: Context, id: Long, snooze: Boolean) =
        Intent(context, ReminderReceiver::class.java)
            .setAction(if (snooze) ReminderReceiver.ACTION_SNOOZE_FIRE else ReminderReceiver.ACTION_FIRE)
            .putExtra(ReminderReceiver.EXTRA_ID, id)

    fun setReminder(context: Context, id: Long, at: Long, snooze: Boolean = false) {
        val slot = if (snooze) SLOT_SNOOZE else SLOT_FIRE
        val pi = PendingIntent.getBroadcast(
            context, requestCode(id, slot), reminderIntent(context, id, snooze),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        set(context, at, pi)
    }

    fun cancelReminder(context: Context, id: Long) {
        cancel(context, requestCode(id, SLOT_FIRE), reminderIntent(context, id, false))
        cancel(context, requestCode(id, SLOT_SNOOZE), reminderIntent(context, id, true))
    }

    private fun timerIntent(context: Context, action: String) = Intent(context, TimerReceiver::class.java).setAction(action)

    private fun timerAlarm(context: Context, code: Int, action: String) =
        PendingIntent.getBroadcast(context, code, timerIntent(context, action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    /** The end-of-session alarm, plus the one-minute warning at [warnAt] when given. */
    fun setTimer(context: Context, at: Long, warnAt: Long?) {
        set(context, at, timerAlarm(context, TIMER_REQUEST_CODE, TimerReceiver.ACTION_DUE))
        if (warnAt != null) set(context, warnAt, timerAlarm(context, WARN_REQUEST_CODE, TimerReceiver.ACTION_WARN))
        else cancel(context, WARN_REQUEST_CODE, timerIntent(context, TimerReceiver.ACTION_WARN))
    }

    fun cancelTimer(context: Context) {
        cancel(context, TIMER_REQUEST_CODE, timerIntent(context, TimerReceiver.ACTION_DUE))
        cancel(context, WARN_REQUEST_CODE, timerIntent(context, TimerReceiver.ACTION_WARN))
    }
}
