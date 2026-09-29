package com.example.routineforge.ui.schedule

import android.app.Application
import android.app.NotificationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.routineforge.data.RoutineRepository
import com.example.routineforge.data.ScheduledRoutine
import com.example.routineforge.service.CalendarAlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class ScheduleEntry(val item: ScheduledRoutine, val next: LocalDateTime?)

data class ScheduleUiState(
    val upcoming: List<ScheduleEntry> = emptyList(),
    val past: List<ScheduleEntry> = emptyList()
)

class ScheduleViewModel(
    application: Application,
    private val repository: RoutineRepository
) : AndroidViewModel(application) {

    // ponytail: "next" is computed when data changes, not every minute; a reminder that passes while
    // the screen is open stays under Upcoming until the next edit or reopen.
    val uiState: StateFlow<ScheduleUiState> = repository.scheduledRoutines.map { items ->
        val now = LocalDateTime.now()
        val entries = items.map { ScheduleEntry(it, if (it.isCompleted) null else it.nextOccurrence(now)) }
        ScheduleUiState(
            upcoming = entries.filter { it.next != null }.sortedBy { it.next },
            past = entries.filter { it.next == null }
                .sortedWith(compareByDescending<ScheduleEntry> { it.item.dateEpochDay }.thenByDescending { it.item.timeOfDay })
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleUiState())

    val themeMode: StateFlow<String> = repository.themeMode

    /** Draft for the editor: an existing reminder, one pre-filled from a routine, or a blank one. */
    fun draftFor(id: String?, routineId: String?): ScheduledRoutine {
        id?.let { existingId -> repository.scheduledRoutines.value.firstOrNull { it.id == existingId }?.let { return it } }
        val nextHour = LocalTime.now().plusHours(1).withMinute(0)
        val blank = ScheduledRoutine(
            routineTitle = "",
            categoryName = "Reminder",
            categoryEmoji = "🔔",
            dateEpochDay = (if (nextHour.hour == 0) LocalDate.now().plusDays(1) else LocalDate.now()).toEpochDay(),
            timeOfDay = "%02d:00".format(nextHour.hour),
            durationMinutes = 0,
            remindBeforeMinutes = 0
        )
        val routine = routineId?.let { repository.getRoutine(it) } ?: return blank
        val category = repository.categories.value.firstOrNull { it.id == routine.categoryId }
        return blank.copy(
            routineId = routine.id,
            routineTitle = routine.title,
            categoryName = category?.name ?: blank.categoryName,
            categoryEmoji = category?.emoji ?: blank.categoryEmoji,
            categoryColorHex = routine.colorHex,
            durationMinutes = (routine.totalDurationSeconds / 60).coerceIn(1, 1440)
        )
    }

    /** Saves and arms the reminder. Returns when it will fire next (null = nothing scheduled). */
    fun save(item: ScheduledRoutine): LocalDateTime? {
        val clean = item.sanitized()
        repository.scheduleRoutine(clean)
        return CalendarAlarmScheduler.scheduleAlarmsForTask(getApplication(), clean)
    }

    fun delete(id: String) {
        CalendarAlarmScheduler.cancelAlarmsForTask(getApplication(), id)
        repository.deleteScheduledRoutine(id)
        getApplication<Application>().getSystemService(NotificationManager::class.java)?.cancel(id.hashCode())
    }

    fun setDone(item: ScheduledRoutine, done: Boolean) {
        repository.setScheduledCompleted(item.id, done)
        if (done) CalendarAlarmScheduler.cancelAlarmsForTask(getApplication(), item.id)
        else CalendarAlarmScheduler.scheduleAlarmsForTask(getApplication(), item.copy(isCompleted = false))
    }

    fun toggleTheme() = repository.toggleThemeMode()

    fun clearAllData() {
        CalendarAlarmScheduler.cancelAll(getApplication())
        repository.clearAllData()
    }
}
