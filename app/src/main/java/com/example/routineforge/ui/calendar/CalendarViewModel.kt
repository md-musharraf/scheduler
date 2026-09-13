package com.example.routineforge.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routineforge.data.CompletedSession
import com.example.routineforge.data.Routine
import com.example.routineforge.data.RoutineRepository
import com.example.routineforge.data.ScheduledRoutine
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
    val completedSessionsForSelectedDate: List<CompletedSession> = emptyList(),
    val scheduledDateSet: Set<Long> = emptySet(),
    val completedDateSet: Set<Long> = emptySet()
)

class CalendarViewModel(
    private val repository: RoutineRepository
) : ViewModel() {

    private val displayedMonth = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<CalendarUiState> = combine(
        displayedMonth,
        selectedDate,
        repository.scheduledRoutines,
        repository.routines,
        repository.sessions
    ) { month, selected, scheduled, routines, sessions ->
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

    fun scheduleRoutine(routine: Routine, date: LocalDate, timeStr: String) {
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
                isCompleted = false
            )
            repository.scheduleRoutine(item)
        }
    }

    fun deleteScheduledRoutine(id: String) {
        viewModelScope.launch {
            repository.deleteScheduledRoutine(id)
        }
    }

    fun toggleScheduledCompleted(id: String) {
        viewModelScope.launch {
            repository.toggleScheduledCompleted(id)
        }
    }
}
