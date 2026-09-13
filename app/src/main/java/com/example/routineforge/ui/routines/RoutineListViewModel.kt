package com.example.routineforge.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routineforge.data.CompletedSession
import com.example.routineforge.data.Routine
import com.example.routineforge.data.RoutineCategory
import com.example.routineforge.data.RoutineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class RoutineListUiState(
    val categories: List<RoutineCategory> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val filteredRoutines: List<Routine> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val completedSessions: List<CompletedSession> = emptyList()
)

class RoutineListViewModel(
    private val repository: RoutineRepository
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<RoutineListUiState> = combine(
        repository.categories,
        repository.routines,
        repository.sessions,
        selectedCategoryId,
        searchQuery
    ) { categories, routines, sessions, catId, query ->
        val filtered = routines.filter { routine ->
            val matchesCategory = catId == null || routine.categoryId == catId
            val matchesQuery = query.isBlank() ||
                    routine.title.contains(query, ignoreCase = true) ||
                    routine.description.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }

        RoutineListUiState(
            categories = categories,
            routines = routines,
            filteredRoutines = filtered,
            selectedCategoryId = catId,
            searchQuery = query,
            completedSessions = sessions
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RoutineListUiState()
    )

    fun selectCategory(categoryId: String?) {
        selectedCategoryId.value = categoryId
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun deleteRoutine(routineId: String) {
        viewModelScope.launch {
            repository.deleteRoutine(routineId)
        }
    }

    fun duplicateRoutine(routine: Routine) {
        viewModelScope.launch {
            val copy = routine.copy(
                id = UUID.randomUUID().toString(),
                title = "${routine.title} (Copy)",
                createdAt = System.currentTimeMillis()
            )
            repository.saveRoutine(copy)
        }
    }

    fun addCustomCategory(name: String, emoji: String, colorHex: Long) {
        viewModelScope.launch {
            val category = RoutineCategory(
                id = "cat_custom_${System.currentTimeMillis()}",
                name = name.trim(),
                emoji = emoji.trim(),
                colorHex = colorHex,
                isCustom = true
            )
            repository.addCategory(category)
            selectedCategoryId.value = category.id
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
            if (selectedCategoryId.value == categoryId) {
                selectedCategoryId.value = null
            }
        }
    }

    fun scheduleRoutine(routine: Routine, dateEpochDay: Long, timeStr: String) {
        viewModelScope.launch {
            val category = repository.categories.value.firstOrNull { it.id == routine.categoryId }
            val item = com.example.routineforge.data.ScheduledRoutine(
                id = UUID.randomUUID().toString(),
                routineId = routine.id,
                routineTitle = routine.title,
                categoryName = category?.name ?: "General",
                categoryEmoji = category?.emoji ?: "🎯",
                categoryColorHex = routine.colorHex,
                dateEpochDay = dateEpochDay,
                timeOfDay = timeStr.trim().ifBlank { "09:00" },
                isCompleted = false
            )
            repository.scheduleRoutine(item)
        }
    }
}

