package com.example.routineforge.data

import android.content.Context
import com.example.routineforge.util.TimeFormatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalDateTime

class RoutineRepository(private val storage: RoutineStorage) {

    private val lock = Any()

    private val _categories = MutableStateFlow<List<RoutineCategory>>(storage.getCategories())
    val categories: StateFlow<List<RoutineCategory>> = _categories.asStateFlow()

    private val _routines = MutableStateFlow<List<Routine>>(storage.getRoutines())
    val routines: StateFlow<List<Routine>> = _routines.asStateFlow()

    private val _sessions = MutableStateFlow<List<CompletedSession>>(storage.getCompletedSessions())
    val sessions: StateFlow<List<CompletedSession>> = _sessions.asStateFlow()

    private val _scheduledRoutines = MutableStateFlow<List<ScheduledRoutine>>(storage.getScheduledRoutines())
    val scheduledRoutines: StateFlow<List<ScheduledRoutine>> = _scheduledRoutines.asStateFlow()

    private val _themeMode = MutableStateFlow<String>(storage.getThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _notes = MutableStateFlow(storage.getNotes())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _timerSettings = MutableStateFlow(storage.getTimerSettings())
    val timerSettings: StateFlow<TimerSettings> = _timerSettings.asStateFlow()

    private val _goals = MutableStateFlow(storage.getGoals())
    val goals: StateFlow<List<FocusGoal>> = _goals.asStateFlow()

    private val _focusLog = MutableStateFlow(storage.getFocusLog())
    val focusLog: StateFlow<FocusLog> = _focusLog.asStateFlow()

    fun saveGoal(goal: FocusGoal) = synchronized(lock) {
        val clean = goal.sanitized()
        val current = _goals.value
        val updated = if (current.any { it.id == clean.id }) current.map { if (it.id == clean.id) clean else it } else current + clean
        storage.saveGoals(updated)
        _goals.value = updated
    }

    fun deleteGoal(id: String) = synchronized(lock) {
        val updated = _goals.value.filter { it.id != id }
        storage.saveGoals(updated)
        _goals.value = updated
    }

    // ponytail: rewrites the whole log JSON per flush (pause / step change / finish), not per second
    fun addFocus(subject: String, seconds: Int, end: LocalDateTime = LocalDateTime.now()) = synchronized(lock) {
        val updated = FocusStats.add(_focusLog.value, subject, end, seconds)
        if (updated === _focusLog.value) return@synchronized
        storage.saveFocusLog(updated)
        _focusLog.value = updated
    }

    /** One step per unfinished goal, sized to what's left today; replaces the previous plan routine. */
    fun buildTodayPlan(): Routine? = synchronized(lock) {
        val today = FocusStats.days(_focusLog.value)[LocalDate.now().toEpochDay()].orEmpty()
        val steps = _goals.value.mapNotNull { g ->
            val left = g.targetSeconds - (today[g.title] ?: 0)
            if (left >= 60) RoutineStep(title = g.title, durationSeconds = left, instruction = "Daily goal · ${TimeFormatters.formatDurationMinutes(g.minutes)}") else null
        }
        if (steps.isEmpty()) return@synchronized null
        _routines.value.filter { it.id.startsWith(PLAN_ID_PREFIX) }.forEach { storage.deleteRoutine(it.id) }
        val plan = Routine(
            id = PLAN_ID_PREFIX + System.currentTimeMillis(),
            title = "Today's plan",
            description = "Your daily goals, one countdown per subject",
            categoryId = "cat_study",
            colorHex = 0xFFD71921,
            steps = steps
        )
        storage.saveRoutine(plan)
        _routines.value = storage.getRoutines()
        plan
    }

    // ponytail: rewrites the whole notes JSON per save; move to Room if notes grow into the thousands
    fun saveNote(note: Note) = synchronized(lock) {
        val current = _notes.value
        val updated = if (current.any { it.id == note.id }) {
            current.map { if (it.id == note.id) note else it }
        } else {
            listOf(note) + current
        }
        storage.saveNotes(updated)
        _notes.value = updated
    }

    fun deleteNote(id: String) = synchronized(lock) {
        if (_notes.value.none { it.id == id }) return@synchronized
        val updated = _notes.value.filter { it.id != id }
        storage.saveNotes(updated)
        _notes.value = updated
    }

    fun saveTimerSettings(settings: TimerSettings) = synchronized(lock) {
        storage.saveTimerSettings(settings)
        _timerSettings.value = settings.copy(label = settings.label.take(40))
    }

    fun setThemeMode(mode: String) = synchronized(lock) {
        storage.setThemeMode(mode)
        _themeMode.value = mode
    }

    fun toggleThemeMode() = synchronized(lock) {
        val next = if (_themeMode.value == "DARK") "LIGHT" else "DARK"
        setThemeMode(next)
    }

    fun scheduleRoutine(scheduled: ScheduledRoutine) = synchronized(lock) {
        storage.saveScheduledRoutine(scheduled)
        _scheduledRoutines.value = storage.getScheduledRoutines()
    }

    fun deleteScheduledRoutine(id: String) = synchronized(lock) {
        storage.deleteScheduledRoutine(id)
        _scheduledRoutines.value = storage.getScheduledRoutines()
    }

    fun setScheduledCompleted(id: String, done: Boolean) = synchronized(lock) {
        storage.setScheduledRoutineCompleted(id, done)
        _scheduledRoutines.value = storage.getScheduledRoutines()
    }

    fun addCategory(category: RoutineCategory) = synchronized(lock) {
        storage.addCategory(category)
        _categories.value = storage.getCategories()
    }

    fun deleteCategory(categoryId: String) = synchronized(lock) {
        storage.deleteCategory(categoryId)
        _categories.value = storage.getCategories()
    }

    fun saveRoutine(routine: Routine) = synchronized(lock) {
        storage.saveRoutine(routine)
        _routines.value = storage.getRoutines()
    }

    fun deleteRoutine(routineId: String) = synchronized(lock) {
        storage.deleteRoutine(routineId)
        _routines.value = storage.getRoutines()
    }

    fun getRoutine(id: String): Routine? = synchronized(lock) {
        _routines.value.firstOrNull { it.id == id } ?: storage.getRoutineById(id)
    }

    fun getOrCreateRoutineForScheduledTask(scheduled: ScheduledRoutine): Routine = synchronized(lock) {
        if (scheduled.routineId.isNotBlank()) {
            val found = getRoutine(scheduled.routineId)
            if (found != null) return found
        }
        val existingTitleMatch = _routines.value.firstOrNull {
            it.title.equals(scheduled.routineTitle, ignoreCase = true)
        }
        if (existingTitleMatch != null) return existingTitleMatch

        // Auto-generate a clean 1-step routine for direct category tasks
        val categoryId = _categories.value.firstOrNull {
            it.name.equals(scheduled.categoryName, ignoreCase = true)
        }?.id ?: (_categories.value.firstOrNull()?.id ?: "cat_study")

        val durationSec = (if (scheduled.durationMinutes > 0) scheduled.durationMinutes else 30) * 60
        val singleStep = RoutineStep(
            id = java.util.UUID.randomUUID().toString(),
            title = scheduled.routineTitle.ifBlank { scheduled.categoryName },
            durationSeconds = durationSec,
            stepType = StepType.WORK,
            instruction = "Scheduled focus session: ${scheduled.categoryEmoji} ${scheduled.categoryName}"
        )
        val synthesizedRoutine = Routine(
            id = if (scheduled.routineId.isNotBlank()) scheduled.routineId else java.util.UUID.randomUUID().toString(),
            title = scheduled.routineTitle.ifBlank { scheduled.categoryName },
            description = "Calendar Protocol • ${scheduled.categoryName}",
            categoryId = categoryId,
            colorHex = scheduled.categoryColorHex,
            iconName = "timer",
            steps = listOf(singleStep)
        )
        saveRoutine(synthesizedRoutine)
        synthesizedRoutine
    }

    fun logSession(session: CompletedSession) = synchronized(lock) {
        storage.saveCompletedSession(session)
        _sessions.value = storage.getCompletedSessions()
    }

    fun clearHistory() = synchronized(lock) {
        storage.clearHistory()
        _sessions.value = emptyList()
    }

    fun clearAllData() = synchronized(lock) {
        storage.clearAllData()
        _routines.value = emptyList()
        _sessions.value = emptyList()
        _scheduledRoutines.value = emptyList()
        _notes.value = emptyList()
        _goals.value = emptyList()
        _focusLog.value = emptyMap()
    }


    companion object {
        private const val PLAN_ID_PREFIX = "routine_today_plan_"

        @Volatile
        private var INSTANCE: RoutineRepository? = null

        fun getInstance(context: Context): RoutineRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RoutineRepository(RoutineStorage(context.applicationContext)).also {
                    INSTANCE = it
                }
            }
        }
    }
}
