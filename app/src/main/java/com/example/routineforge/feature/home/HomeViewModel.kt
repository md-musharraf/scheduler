package com.example.routineforge.feature.home

import androidx.compose.runtime.Immutable
import com.example.routineforge.AppContainer
import com.example.routineforge.core.database.Subject
import com.example.routineforge.core.database.TaskItem
import com.example.routineforge.core.ui.FFViewModel
import com.example.routineforge.core.ui.todayFlow
import com.example.routineforge.domain.FocusAnalytics
import com.example.routineforge.domain.FocusStats
import com.example.routineforge.domain.StatsPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate
import java.time.LocalDateTime

@Immutable
data class SubjectToday(val subject: Subject, val focusedMs: Long, val targetMinutes: Int) {
    val progress: Float get() = FocusAnalytics.progress(focusedMs, targetMinutes)
    val met: Boolean get() = targetMinutes > 0 && focusedMs >= targetMinutes * 60_000L
}

@Immutable
data class HomeUi(
    val loading: Boolean = true,
    val name: String = "",
    val today: LocalDate = LocalDate.now(),
    val selected: LocalDate = LocalDate.now(),
    val stats: FocusStats? = null,
    val dailyTargetMinutes: Int = 0,
    val subjects: List<SubjectToday> = emptyList(),
    val allSubjects: List<Subject> = emptyList(),
    val tasks: List<TaskItem> = emptyList(),
    val streak: Int = 0,
    val remindersToday: Int = 0
) {
    val week: List<LocalDate> get() = StatsPeriod.WEEK.range(selected).days
    val focusedMs: Long get() = stats?.totalMs ?: 0L
    val progress: Float get() = FocusAnalytics.progress(focusedMs, dailyTargetMinutes)

    /** Daily subject targets plus tasks due that day. */
    val goalsTotal: Int get() = subjects.count { it.targetMinutes > 0 } + tasks.count { it.dueDate == selected.toEpochDay() }
    val goalsDone: Int get() = subjects.count { it.met } + tasks.count { it.dueDate == selected.toEpochDay() && it.isDone }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val c: AppContainer) : FFViewModel() {
    private val selected = MutableStateFlow(LocalDate.now())

    private val stats = selected.flatMapLatest { c.sessions.stats(StatsPeriod.DAY.range(it)) }

    private val remindersToday = combine(c.reminders.reminders, todayFlow()) { list, today ->
        val end = today.plusDays(1).atStartOfDay()
        list.count { r ->
            val next = c.reminders.nextTrigger(r) ?: return@count false
            java.time.Instant.ofEpochMilli(next).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime().isBefore(end)
        }
    }

    private val base = combine(c.profiles.profile, todayFlow(), selected, c.sessions.dayTotals(), remindersToday) { p, today, sel, totals, rem ->
        HomeUi(
            loading = false,
            name = p.name,
            today = today,
            selected = sel,
            streak = FocusAnalytics.streak(totals, today, p.streakMinMinutes * 60_000L),
            remindersToday = rem
        )
    }

    val ui: StateFlow<HomeUi> = combine(base, stats, c.subjects.subjects, c.tasks.tasks, c.goals.goals) { b, st, subjects, tasks, goals ->
        val day = b.selected.toEpochDay()
        val active = subjects.filter { !it.isArchived }
        b.copy(
            stats = st,
            allSubjects = active,
            dailyTargetMinutes = FocusAnalytics.overallTarget(goals, subjects, StatsPeriod.DAY, StatsPeriod.DAY.range(b.selected)),
            subjects = active.map { SubjectToday(it, st.bySubject[it.id] ?: 0L, it.dailyTargetMinutes) },
            tasks = tasks.filter { t ->
                t.dueDate == day || (b.selected == b.today && !t.isDone && t.dueDate != null && t.dueDate < day)
            }
        )
    }.stateIn(HomeUi())

    fun select(date: LocalDate) {
        selected.value = date
    }

    fun shiftWeek(weeks: Long) {
        selected.value = selected.value.plusWeeks(weeks)
    }

    fun addTask(title: String, subjectId: Long?, due: Long?) = launchSafe {
        c.tasks.save(TaskItem(title = title, subjectId = subjectId, dueDate = due))
        say("Task added")
    }

    fun toggleTask(id: Long, done: Boolean) = launchSafe { c.tasks.setDone(id, done) }
    fun deleteTask(id: Long) = launchSafe { c.tasks.delete(id) }

    fun greeting(now: LocalDateTime = LocalDateTime.now()): String = when (now.hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..21 -> "Good Evening"
        else -> "Hello"
    }
}
