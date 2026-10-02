package com.example.routineforge.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.routineforge.container
import com.example.routineforge.core.database.SessionType
import kotlinx.coroutines.launch

/** Runs [block] on the app scope while keeping the broadcast alive until it finishes. */
private fun BroadcastReceiver.work(context: Context, block: suspend () -> Unit) {
    val pending = goAsync()
    context.container.scope.launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

/** End-of-session alarm and the ongoing notification's buttons. */
class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timer = context.container.timer
        work(context) {
            val active = context.container.db.sessionDao().getActive().firstOrNull()
            when (intent.action) {
                ACTION_DUE -> timer.completeIfDue()
                ACTION_PAUSE -> active?.let { timer.pause(it.id) }
                ACTION_RESUME -> active?.let { timer.resume(it.id) }
                ACTION_FINISH -> active?.let { timer.stop(it.id, save = true) }
                ACTION_WARN -> timer.warnIfEndingSoon()
                ACTION_START -> if (active == null) {
                    timer.startDefault(intent.getStringExtra(EXTRA_TYPE)?.let(SessionType::valueOf) ?: SessionType.FOCUS)
                }
                ACTION_DISMISS -> Notifier.cancel(context, Notifier.ID_TIMER_DONE)
                ACTION_SW_TOGGLE -> context.container.stopwatch.toggle()
                ACTION_SW_RESET -> context.container.stopwatch.reset()
            }
        }
    }

    companion object {
        const val ACTION_DUE = "com.example.routineforge.timer.DUE"
        const val ACTION_PAUSE = "com.example.routineforge.timer.PAUSE"
        const val ACTION_RESUME = "com.example.routineforge.timer.RESUME"
        const val ACTION_FINISH = "com.example.routineforge.timer.FINISH"
        const val ACTION_START = "com.example.routineforge.timer.START"
        const val ACTION_WARN = "com.example.routineforge.timer.WARN"
        const val ACTION_DISMISS = "com.example.routineforge.timer.DISMISS"
        const val EXTRA_TYPE = "type"
        const val ACTION_SW_TOGGLE = "com.example.routineforge.stopwatch.TOGGLE"
        const val ACTION_SW_RESET = "com.example.routineforge.stopwatch.RESET"
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L).takeIf { it > 0 } ?: return
        val reminders = context.container.reminders
        work(context) {
            when (intent.action) {
                ACTION_FIRE -> reminders.onFire(id, snooze = false)
                ACTION_SNOOZE_FIRE -> reminders.onFire(id, snooze = true)
                ACTION_SNOOZE -> reminders.snooze(id)
                ACTION_DONE -> {
                    val r = reminders.get(id)
                    // A repeating reminder stays active; "Done" only dismisses this occurrence.
                    if (r != null && r.repeatRule == com.example.routineforge.core.database.RepeatRule.NONE) reminders.setDone(id, true)
                    Notifier.cancel(context, Notifier.notificationId(id))
                }
            }
        }
    }

    companion object {
        const val EXTRA_ID = "reminder_id"
        const val ACTION_FIRE = "com.example.routineforge.reminder.FIRE"
        const val ACTION_SNOOZE_FIRE = "com.example.routineforge.reminder.SNOOZE_FIRE"
        const val ACTION_SNOOZE = "com.example.routineforge.reminder.SNOOZE"
        const val ACTION_DONE = "com.example.routineforge.reminder.DONE"
    }
}

/** Alarms are cleared on reboot and app update, and wall-clock alarms move with time-zone changes. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> work(context) {
                context.container.timer.restore()
                context.container.stopwatch.restore()
                context.container.reminders.rescheduleAll()
            }
        }
    }
}
