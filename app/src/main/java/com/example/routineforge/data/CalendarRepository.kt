package com.example.routineforge.data

import androidx.compose.runtime.Immutable
import com.example.routineforge.core.database.CalendarEvent
import com.example.routineforge.core.database.EventCategory
import com.example.routineforge.core.database.EventDao
import com.example.routineforge.core.database.Reminder
import com.example.routineforge.core.database.ReminderCategory
import com.example.routineforge.core.database.RepeatRule
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.domain.Recurrence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** One concrete occurrence of a (possibly repeating) event. */
@Immutable
data class EventOccurrence(val event: CalendarEvent, val start: LocalDateTime, val end: LocalDateTime)

class CalendarRepository(private val dao: EventDao, private val reminders: ReminderRepository) {

    fun occurrences(from: LocalDate, toExclusive: LocalDate): Flow<List<EventOccurrence>> {
        val fromT = from.atStartOfDay()
        val toT = toExclusive.atStartOfDay()
        return dao.observeCandidates(TimeFormat.toMillis(fromT), TimeFormat.toMillis(toT)).map { events ->
            events.flatMap { e ->
                val start = TimeFormat.toLocal(e.startTime)
                val minutes = Duration.ofMillis(e.endTime - e.startTime).toMinutes()
                Recurrence.between(e.repeatRule, start, minutes, fromT, toT).map { EventOccurrence(e, it, it.plusMinutes(minutes)) }
            }.sortedBy { it.start }
        }.flowOn(Dispatchers.Default)
    }

    suspend fun get(id: Long): CalendarEvent? = dao.get(id)

    /** Minutes-before of the event's reminder, or null when it has none. */
    suspend fun reminderOffset(event: CalendarEvent): Int? =
        event.reminderId?.let { reminders.get(it) }?.let { ((event.startTime - it.triggerTime) / 60_000).toInt() }

    suspend fun save(event: CalendarEvent, remindMinutesBefore: Int?, now: Long = System.currentTimeMillis()): Long {
        val title = event.title.trim()
        validate(title.isNotEmpty()) { "Give the event a title" }
        validate(event.endTime > event.startTime) { "End time must be after the start time" }
        validate(event.endTime - event.startTime <= 7 * 86_400_000L) { "Events can last at most 7 days" }
        val clean = event.copy(title = title.take(80), description = event.description.trim().take(500), color = event.category.color, updatedAt = now)
        val id = if (clean.id == 0L) dao.insert(clean.copy(createdAt = now)) else clean.id.also { dao.update(clean) }
        val reminderId = syncReminder(clean.copy(id = id), remindMinutesBefore, now)
        if (reminderId != clean.reminderId) dao.update(clean.copy(id = id, reminderId = reminderId))
        return id
    }

    private suspend fun syncReminder(event: CalendarEvent, offset: Int?, now: Long): Long? {
        val existing = event.reminderId?.let { reminders.get(it) }
        val trigger = offset?.let { event.startTime - it * 60_000L }
        if (trigger == null || (event.repeatRule == RepeatRule.NONE && trigger <= now)) {
            existing?.let { reminders.delete(it.id) }
            return null
        }
        val category = when (event.category) {
            EventCategory.STUDY -> ReminderCategory.STUDY
            EventCategory.EXAM -> ReminderCategory.EXAM
            EventCategory.PERSONAL -> ReminderCategory.PERSONAL
            EventCategory.WORK, EventCategory.MEETING -> ReminderCategory.WORK
        }
        val description = "Starts at ${TimeFormat.time(event.startTime)}" + if (offset == 0) "" else " · in $offset min"
        val base = existing ?: Reminder(title = event.title, triggerTime = trigger)
        return reminders.save(
            base.copy(
                title = event.title, description = description, triggerTime = trigger, repeatRule = event.repeatRule,
                category = category, eventId = event.id, enabled = true, isDone = false
            ),
            now
        )
    }

    suspend fun delete(id: Long) {
        dao.get(id)?.reminderId?.let { reminders.delete(it) }
        dao.delete(id)
    }
}
