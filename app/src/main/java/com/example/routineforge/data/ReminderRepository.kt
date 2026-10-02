package com.example.routineforge.data

import android.content.Context
import com.example.routineforge.core.database.Reminder
import com.example.routineforge.core.database.ReminderDao
import com.example.routineforge.core.database.RepeatRule
import com.example.routineforge.core.notifications.Alarms
import com.example.routineforge.core.notifications.Notifier
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.domain.Recurrence
import kotlinx.coroutines.flow.Flow

/** Every write re-arms (or cancels) the reminder's alarm, so the schedule always matches the table. */
class ReminderRepository(private val context: Context, private val dao: ReminderDao) {
    val reminders: Flow<List<Reminder>> = dao.observeAll()

    suspend fun get(id: Long): Reminder? = dao.get(id)

    suspend fun save(reminder: Reminder, now: Long = System.currentTimeMillis()): Long {
        val title = reminder.title.trim()
        validate(title.isNotEmpty()) { "Give the reminder a title" }
        validate(reminder.repeatRule != RepeatRule.NONE || reminder.triggerTime > now || reminder.id != 0L && !reminder.enabled) {
            "Pick a time in the future"
        }
        val clean = reminder.copy(title = title.take(80), description = reminder.description.trim().take(300), updatedAt = now)
        val id = if (clean.id == 0L) dao.insert(clean.copy(createdAt = now)) else clean.id.also {
            // A new time means it hasn't fired yet.
            val old = dao.get(it)
            dao.update(if (old != null && old.triggerTime != clean.triggerTime) clean.copy(lastFiredAt = null) else clean)
        }
        schedule(dao.get(id))
        return id
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        val r = dao.get(id) ?: return
        dao.update(r.copy(enabled = enabled, updatedAt = System.currentTimeMillis()))
        schedule(dao.get(id))
    }

    suspend fun setDone(id: Long, done: Boolean) {
        val r = dao.get(id) ?: return
        dao.update(r.copy(isDone = done, updatedAt = System.currentTimeMillis()))
        schedule(dao.get(id))
        if (done) Notifier.cancel(context, Notifier.notificationId(id))
    }

    suspend fun delete(id: Long) {
        Alarms.cancelReminder(context, id)
        Notifier.cancel(context, Notifier.notificationId(id))
        dao.delete(id)
    }

    /** The next time [r] fires after [now], or null if it won't fire again. */
    fun nextTrigger(r: Reminder, now: Long = System.currentTimeMillis()): Long? {
        if (!r.enabled || r.isDone) return null
        val next = Recurrence.next(r.repeatRule, TimeFormat.toLocal(r.triggerTime), TimeFormat.toLocal(now)) ?: return null
        return TimeFormat.toMillis(next)
    }

    fun schedule(r: Reminder?, now: Long = System.currentTimeMillis()) {
        if (r == null) return
        Alarms.cancelReminder(context, r.id)
        if (!r.enabled || r.isDone) return
        val next = nextTrigger(r, now)
        when {
            next != null -> Alarms.setReminder(context, r.id, next)
            // A one-off that came due while the phone was off still fires once, if recent.
            r.repeatRule == RepeatRule.NONE && r.lastFiredAt == null && now - r.triggerTime in 0..MISSED_GRACE_MS ->
                Alarms.setReminder(context, r.id, now + 2_000)
        }
    }

    suspend fun rescheduleAll() = dao.getActive().forEach { schedule(it) }

    /** Called by the alarm: shows the notification and queues the next repeat. */
    suspend fun onFire(id: Long, snooze: Boolean) {
        val r = dao.get(id) ?: return
        if (!r.enabled || r.isDone) return
        val now = System.currentTimeMillis()
        Notifier.showReminder(context, r, if (r.repeatRule == RepeatRule.NONE && !snooze) r.triggerTime else now)
        dao.markFired(id, now)
        if (!snooze) schedule(dao.get(id), now)
    }

    fun snooze(id: Long, minutes: Int = 10) {
        Notifier.cancel(context, Notifier.notificationId(id))
        Alarms.setReminder(context, id, System.currentTimeMillis() + minutes * 60_000L, snooze = true)
    }

    private companion object {
        const val MISSED_GRACE_MS = 12 * 3_600_000L
    }
}
