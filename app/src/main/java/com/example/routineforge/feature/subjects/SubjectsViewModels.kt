package com.example.routineforge.feature.subjects

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.example.routineforge.AppContainer
import com.example.routineforge.core.database.FocusSession
import com.example.routineforge.core.database.Note
import com.example.routineforge.core.database.Subject
import com.example.routineforge.core.database.TaskItem
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.core.ui.FFViewModel
import com.example.routineforge.core.ui.todayFlow
import com.example.routineforge.domain.FocusAnalytics
import com.example.routineforge.domain.StatsPeriod
import com.example.routineforge.feature.home.SubjectToday
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class SubjectFilter(val label: String) { ALL("All"), ACTIVE("Active"), ARCHIVED("Archived") }

@Immutable
data class SubjectsUi(val loading: Boolean = true, val filter: SubjectFilter = SubjectFilter.ACTIVE, val rows: List<SubjectToday> = emptyList())

@OptIn(ExperimentalCoroutinesApi::class)
class SubjectsViewModel(private val c: AppContainer) : FFViewModel() {
    private val filter = MutableStateFlow(SubjectFilter.ACTIVE)
    private val today = todayFlow().flatMapLatest { c.sessions.stats(StatsPeriod.DAY.range(it)) }

    val ui: StateFlow<SubjectsUi> = combine(c.subjects.subjects, today, filter) { subjects, stats, f ->
        SubjectsUi(
            loading = false,
            filter = f,
            rows = subjects.filter {
                when (f) {
                    SubjectFilter.ALL -> true
                    SubjectFilter.ACTIVE -> !it.isArchived
                    SubjectFilter.ARCHIVED -> it.isArchived
                }
            }.map { SubjectToday(it, stats.bySubject[it.id] ?: 0L, it.dailyTargetMinutes) }
        )
    }.stateIn(SubjectsUi())

    fun setFilter(f: SubjectFilter) {
        filter.value = f
    }

    fun setArchived(s: Subject, archived: Boolean) = launchSafe {
        c.subjects.setArchived(s.id, archived)
        say(if (archived) "${s.name} archived" else "${s.name} restored")
    }

    fun delete(s: Subject) = launchSafe {
        c.subjects.delete(s.id)
        say("${s.name} deleted. Its focus time is kept.")
    }
}

/** Form state for creating or editing a subject. */
@Immutable
data class SubjectForm(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val icon: String = "book",
    val color: Long = 0xFF5B5FEF,
    val dailyMinutes: Int = 60,
    /** Blank = derived from the daily target. */
    val weeklyHours: String = "",
    val monthlyHours: String = "",
    val yearlyHours: String = "",
    val examDate: LocalDate? = null,
    val priority: Int = 1,
    val isArchived: Boolean = false,
    val createdAt: Long = 0
)

class SubjectEditorViewModel(private val c: AppContainer, private val id: Long) : FFViewModel() {
    val form = MutableStateFlow(SubjectForm())
    val saved = MutableStateFlow(false)
    val loaded = MutableStateFlow(id == 0L)

    init {
        if (id != 0L) viewModelScope.launch {
            val s = c.subjects.get(id)
            if (s == null) {
                say("That subject no longer exists")
                saved.value = true
            } else {
                fun h(m: Int) = if (m <= 0) "" else (m / 60.0).let { if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it) }
                form.value = SubjectForm(
                    s.id, s.name, s.description, s.icon, s.color, s.dailyTargetMinutes,
                    h(s.weeklyTargetMinutes), h(s.monthlyTargetMinutes), h(s.yearlyTargetMinutes),
                    s.examDate?.let { LocalDate.ofEpochDay(it) }, s.priority, s.isArchived, s.createdAt
                )
            }
            loaded.value = true
        }
    }

    fun update(transform: (SubjectForm) -> SubjectForm) {
        form.value = transform(form.value)
    }

    private fun hoursToMinutes(text: String, label: String): Int {
        if (text.isBlank()) return 0
        val v = text.trim().replace(',', '.').toDoubleOrNull()
        require(v != null && v >= 0) { "$label target must be a number of hours" }
        return (v * 60).toInt()
    }

    fun save() = launchSafe {
        val f = form.value
        val subject = Subject(
            id = f.id, name = f.name, description = f.description, icon = f.icon, color = f.color,
            dailyTargetMinutes = f.dailyMinutes,
            weeklyTargetMinutes = hoursToMinutes(f.weeklyHours, "Weekly"),
            monthlyTargetMinutes = hoursToMinutes(f.monthlyHours, "Monthly"),
            yearlyTargetMinutes = hoursToMinutes(f.yearlyHours, "Yearly"),
            examDate = f.examDate?.toEpochDay(), priority = f.priority, isArchived = f.isArchived, createdAt = f.createdAt
        )
        c.subjects.save(subject)
        say(if (f.id == 0L) "${f.name.trim()} added" else "Saved")
        saved.value = true
    }
}

@Immutable
data class DaySummary(val date: LocalDate, val focusedMs: Long, val sessions: Int)

@Immutable
data class SubjectDetailUi(
    val loading: Boolean = true,
    val subject: Subject? = null,
    val todayMs: Long = 0,
    val weekMs: Long = 0,
    val monthMs: Long = 0,
    val totalMs: Long = 0,
    val weekTargetMinutes: Int = 0,
    val recent: List<DaySummary> = emptyList(),
    val notes: List<Note> = emptyList(),
    val tasks: List<TaskItem> = emptyList(),
    val today: LocalDate = LocalDate.now()
) {
    val daysToExam: Long? get() = subject?.examDate?.let { it - today.toEpochDay() }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SubjectDetailViewModel(private val c: AppContainer, private val id: Long) : FFViewModel() {
    private val periodStats = todayFlow().flatMapLatest { d ->
        // The month and week both fit inside a 5-week window around today; one query covers both.
        val week = StatsPeriod.WEEK.range(d)
        val month = StatsPeriod.MONTH.range(d)
        val range = com.example.routineforge.domain.DateRange(minOf(week.start, month.start), maxOf(week.endExclusive, month.endExclusive))
        c.sessions.focusBetween(range).map { slices ->
            val mine = slices.filter { it.subjectId == id }
            val zone = java.time.ZoneId.systemDefault()
            Triple(
                FocusAnalytics.stats(mine, StatsPeriod.DAY.range(d), zone).totalMs,
                FocusAnalytics.stats(mine, week, zone).totalMs,
                FocusAnalytics.stats(mine, month, zone).totalMs
            ) to d
        }
    }
    private val total = c.sessions.allFocus.map { all -> all.filter { it.subjectId == id }.sumOf { it.actualDurationMs } }
    private val recent = c.sessions.recentForSubject(id, 200).map { sessions -> summarize(sessions) }

    val ui: StateFlow<SubjectDetailUi> = combine(
        c.subjects.observe(id), periodStats, total, recent,
        combine(c.notes.forSubject(id), c.tasks.forSubject(id)) { n, t -> n to t }
    ) { s, (triple, today), tot, rec, (notes, tasks) ->
        SubjectDetailUi(
            loading = false, subject = s,
            todayMs = triple.first, weekMs = triple.second, monthMs = triple.third, totalMs = tot,
            weekTargetMinutes = s?.let { FocusAnalytics.subjectTarget(it, StatsPeriod.WEEK, StatsPeriod.WEEK.range(today)) } ?: 0,
            recent = rec, notes = notes, tasks = tasks, today = today
        )
    }.stateIn(SubjectDetailUi())

    private fun summarize(sessions: List<FocusSession>): List<DaySummary> =
        sessions.groupBy { TimeFormat.toLocal(it.endTime ?: it.startTime).toLocalDate() }
            .map { (d, list) -> DaySummary(d, list.sumOf { it.actualDurationMs }, list.size) }
            .sortedByDescending { it.date }
            .take(10)

    fun addTask(title: String, due: Long?) = launchSafe {
        c.tasks.save(TaskItem(title = title, subjectId = id, dueDate = due))
    }

    fun toggleTask(taskId: Long, done: Boolean) = launchSafe { c.tasks.setDone(taskId, done) }
    fun deleteTask(taskId: Long) = launchSafe { c.tasks.delete(taskId) }
    fun setArchived(archived: Boolean) = launchSafe { c.subjects.setArchived(id, archived) }
}
