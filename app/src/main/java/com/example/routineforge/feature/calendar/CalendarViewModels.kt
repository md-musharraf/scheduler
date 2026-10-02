package com.example.routineforge.feature.calendar

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.example.routineforge.AppContainer
import com.example.routineforge.core.database.CalendarEvent
import com.example.routineforge.core.database.EventCategory
import com.example.routineforge.core.database.RepeatRule
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.core.ui.FFViewModel
import com.example.routineforge.core.ui.todayFlow
import com.example.routineforge.data.EventOccurrence
import com.example.routineforge.domain.StatsPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

enum class CalendarView(val label: String) { MONTH("Month"), WEEK("Week"), DAY("Day"), AGENDA("Agenda") }

@Immutable
data class CalendarUi(
    val loading: Boolean = true,
    val view: CalendarView = CalendarView.MONTH,
    val selected: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    val occurrences: List<EventOccurrence> = emptyList()
) {
    fun on(day: LocalDate): List<EventOccurrence> =
        occurrences.filter { it.start.toLocalDate() <= day && it.end.isAfter(day.atStartOfDay()) }
    val daysWithEvents: Set<LocalDate> get() = occurrences.flatMap { o ->
        generateSequence(o.start.toLocalDate()) { it.plusDays(1) }.takeWhile { it.atStartOfDay().isBefore(o.end) }.toList()
    }.toSet()
}

/** First cell of the Sunday-first month grid. */
fun monthGridStart(month: LocalDate): LocalDate = month.withDayOfMonth(1).let { it.minusDays((it.dayOfWeek.value % 7).toLong()) }

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(private val c: AppContainer) : FFViewModel() {
    private val view = MutableStateFlow(CalendarView.MONTH)
    private val selected = MutableStateFlow(LocalDate.now())

    private val occurrences = combine(view, selected) { v, d ->
        when (v) {
            CalendarView.MONTH -> monthGridStart(d).let { it to it.plusDays(42) }
            CalendarView.WEEK -> StatsPeriod.WEEK.range(d).let { it.start to it.endExclusive }
            CalendarView.DAY -> d to d.plusDays(1)
            CalendarView.AGENDA -> d to d.plusDays(60)
        }
    }.flatMapLatest { (from, to) -> c.calendar.occurrences(from, to) }

    val ui: StateFlow<CalendarUi> = combine(view, selected, todayFlow(), occurrences) { v, s, t, o ->
        CalendarUi(false, v, s, t, o)
    }.stateIn(CalendarUi())

    fun setView(v: CalendarView) { view.value = v }
    fun select(d: LocalDate) { selected.value = d }
    fun shift(n: Long) {
        selected.value = when (view.value) {
            CalendarView.MONTH -> selected.value.plusMonths(n)
            CalendarView.WEEK -> selected.value.plusWeeks(n)
            CalendarView.DAY, CalendarView.AGENDA -> selected.value.plusDays(n)
        }
    }
    fun goToday() { selected.value = LocalDate.now() }
}

val REMINDER_OPTIONS: List<Pair<Int?, String>> = listOf(
    null to "No reminder", 0 to "At start time", 5 to "5 minutes before", 10 to "10 minutes before",
    15 to "15 minutes before", 30 to "30 minutes before", 60 to "1 hour before", 1440 to "1 day before"
)

@Immutable
data class EventForm(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val start: LocalTime = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0),
    val end: LocalTime = LocalTime.now().plusHours(2).withMinute(0).withSecond(0).withNano(0),
    val category: EventCategory = EventCategory.STUDY,
    val repeat: RepeatRule = RepeatRule.NONE,
    val remindBefore: Int? = 10,
    val reminderId: Long? = null,
    val createdAt: Long = 0
)

class EventEditorViewModel(private val c: AppContainer, private val id: Long, date: LocalDate?) : FFViewModel() {
    val form = MutableStateFlow(EventForm(date = date ?: LocalDate.now()))
    val loaded = MutableStateFlow(id == 0L)
    val done = MutableStateFlow(false)

    init {
        if (id != 0L) viewModelScope.launch {
            val e = c.calendar.get(id)
            if (e == null) {
                say("This event no longer exists")
                done.value = true
                return@launch
            }
            val start = TimeFormat.toLocal(e.startTime)
            val end = TimeFormat.toLocal(e.endTime)
            form.value = EventForm(e.id, e.title, e.description, start.toLocalDate(), start.toLocalTime(), end.toLocalTime(), e.category, e.repeatRule, c.calendar.reminderOffset(e), e.reminderId, e.createdAt)
            loaded.value = true
        }
    }

    fun update(t: (EventForm) -> EventForm) { form.value = t(form.value) }

    fun save() = launchSafe {
        val f = form.value
        val start = f.date.atTime(f.start)
        // An end time earlier than the start means the event runs past midnight.
        val end = f.date.atTime(f.end).let { if (!it.isAfter(start)) it.plusDays(1) else it }
        c.calendar.save(
            CalendarEvent(
                id = f.id, title = f.title, description = f.description,
                startTime = TimeFormat.toMillis(start), endTime = TimeFormat.toMillis(end),
                category = f.category, repeatRule = f.repeat, reminderId = f.reminderId, createdAt = f.createdAt
            ),
            f.remindBefore
        )
        say(if (f.id == 0L) "Event added" else "Event saved")
        done.value = true
    }

    fun delete() = launchSafe {
        c.calendar.delete(id)
        say("Event deleted")
        done.value = true
    }
}
