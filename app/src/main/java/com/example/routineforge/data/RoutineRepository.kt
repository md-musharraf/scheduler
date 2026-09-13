package com.example.routineforge.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    fun toggleScheduledCompleted(id: String) = synchronized(lock) {
        storage.toggleScheduledRoutineCompleted(id)
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
    }


    companion object {
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
