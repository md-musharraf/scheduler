package com.example.routineforge.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.routineforge.data.CompletedSession
import com.example.routineforge.data.Routine
import com.example.routineforge.data.RoutineCategory
import com.example.routineforge.data.RoutineRepository
import com.example.routineforge.data.ScheduledRoutine
import com.example.routineforge.service.CalendarAlarmScheduler
import com.example.routineforge.util.AlertHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

data class CalendarUiState(
    val displayedMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val scheduledRoutines: List<ScheduledRoutine> = emptyList(),
    val scheduledForSelectedDate: List<ScheduledRoutine> = emptyList(),
    val availableRoutines: List<Routine> = emptyList(),
    val availableCategories: List<RoutineCategory> = emptyList(),
    val completedSessionsForSelectedDate: List<CompletedSession> = emptyList(),
    val scheduledDateSet: Set<Long> = emptySet(),
    val completedDateSet: Set<Long> = emptySet()
)

class CalendarViewModel(
    application: Application,
    private val repository: RoutineRepository
) : AndroidViewModel(application) {

    private val displayedMonth = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val alertHelper by lazy { AlertHelper(application) }

    private val dateFilterFlow = combine(displayedMonth, selectedDate) { month, selected ->
        Pair(month, selected)
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        dateFilterFlow,
        repository.scheduledRoutines,
        repository.routines,
        repository.categories,
        repository.sessions
    ) { (month, selected), scheduled, routines, categories, sessions ->
        val selectedEpoch = selected.toEpochDay()
        val forSelected = scheduled.filter { it.dateEpochDay == selectedEpoch }

        val sessionsForSelected = sessions.filter { session ->
            val sessionDate = Instant.ofEpochMilli(session.completedAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            sessionDate == selected
        }

        val scheduledSet = scheduled.map { it.dateEpochDay }.toSet()
        val completedSet = sessions.map {
            Instant.ofEpochMilli(it.completedAt).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
        }.toSet()

        CalendarUiState(
            displayedMonth = month,
            selectedDate = selected,
            scheduledRoutines = scheduled,
            scheduledForSelectedDate = forSelected,
            availableRoutines = routines,
            availableCategories = categories,
            completedSessionsForSelectedDate = sessionsForSelected,
            scheduledDateSet = scheduledSet,
            completedDateSet = completedSet
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CalendarUiState()
    )

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        if (YearMonth.from(date) != displayedMonth.value) {
            displayedMonth.value = YearMonth.from(date)
        }
    }

    fun changeMonth(offsetMonths: Long) {
        displayedMonth.value = displayedMonth.value.plusMonths(offsetMonths)
    }

    fun jumpToToday() {
        val today = LocalDate.now()
        selectedDate.value = today
        displayedMonth.value = YearMonth.now()
    }

    /**
     * Schedules a quick Category-only task directly from the Calendar.
     * No pre-created routine or subcategory required!
     */
    fun scheduleCategoryTask(
        taskTitle: String,
        category: RoutineCategory,
        date: LocalDate,
        timeStr: String,
        durationMinutes: Int = 30,
        remindBeforeMinutes: Int = 1,
        remindAtTime: Boolean = true,
        vibrationPattern: String = "NOTHING_PULSE"
    ) {
        viewModelScope.launch {
            val title = taskTitle.trim().ifBlank { "${category.name} Focus" }
            val item = ScheduledRoutine(
                id = UUID.randomUUID().toString(),
                routineId = "", // Direct category task without requiring prior routine
                routineTitle = title,
                categoryName = category.name,
                categoryEmoji = category.emoji,
                categoryColorHex = category.colorHex,
                dateEpochDay = date.toEpochDay(),
                timeOfDay = timeStr.trim().ifBlank { "09:00" },
                durationMinutes = durationMinutes,
                remindBeforeMinutes = remindBeforeMinutes,
                remindAtTime = remindAtTime,
                vibrationPattern = vibrationPattern,
                isCompleted = false
            )
            repository.scheduleRoutine(item)
            CalendarAlarmScheduler.scheduleAlarmsForTask(getApplication(), item)
        }
    }

    /**
     * Schedules an existing multi-step Routine.
     */
    fun scheduleRoutine(
        routine: Routine,
        date: LocalDate,
        timeStr: String,
        remindBeforeMinutes: Int = 1,
        remindAtTime: Boolean = true,
        vibrationPattern: String = "NOTHING_PULSE"
    ) {
        viewModelScope.launch {
            val category = repository.categories.value.firstOrNull { it.id == routine.categoryId }
            val item = ScheduledRoutine(
                id = UUID.randomUUID().toString(),
                routineId = routine.id,
                routineTitle = routine.title,
                categoryName = category?.name ?: "General",
                categoryEmoji = category?.emoji ?: "🎯",
                categoryColorHex = routine.colorHex,
                dateEpochDay = date.toEpochDay(),
                timeOfDay = timeStr.trim().ifBlank { "09:00" },
                durationMinutes = (routine.totalDurationSeconds / 60).coerceAtLeast(1),
                remindBeforeMinutes = remindBeforeMinutes,
                remindAtTime = remindAtTime,
                vibrationPattern = vibrationPattern,
                isCompleted = false
            )
            repository.scheduleRoutine(item)
            CalendarAlarmScheduler.scheduleAlarmsForTask(getApplication(), item)
        }
    }

    fun addCustomCategory(name: String, emoji: String, colorHex: Long): RoutineCategory {
        val newCat = RoutineCategory(
            id = "cat_custom_${System.currentTimeMillis()}",
            name = name.trim().ifBlank { "Custom" },
            emoji = emoji.trim().ifBlank { "⚡" },
            colorHex = colorHex,
            isCustom = true
        )
        repository.addCategory(newCat)
        return newCat
    }

    fun deleteScheduledRoutine(id: String) {
        viewModelScope.launch {
            CalendarAlarmScheduler.cancelAlarmsForTask(getApplication(), id)
            repository.deleteScheduledRoutine(id)
        }
    }

    fun toggleScheduledCompleted(id: String) {
        viewModelScope.launch {
            val task = repository.scheduledRoutines.value.firstOrNull { it.id == id }
            if (task != null) {
                if (!task.isCompleted) {
                    // Being completed -> cancel upcoming alarms
                    CalendarAlarmScheduler.cancelAlarmsForTask(getApplication(), id)
                } else {
                    // Being re-opened -> re-schedule alarms
                    CalendarAlarmScheduler.scheduleAlarmsForTask(getApplication(), task.copy(isCompleted = false))
                }
            }
            repository.toggleScheduledCompleted(id)
        }
    }

    /**
     * Resolves or auto-creates a valid routine so clicking "START" on ANY calendar task
     * launches RoutinePlayer seamlessly.
     */
    fun startScheduledTask(scheduled: ScheduledRoutine): String {
        val routine = repository.getOrCreateRoutineForScheduledTask(scheduled)
        return routine.id
    }

    fun testVibrationPattern(patternName: String) {
        alertHelper.testVibrationPattern(patternName)
    }

    override fun onCleared() {
        super.onCleared()
        alertHelper.release()
    }
}
