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
import com.example.routineforge.util.SmartSearchEngine
import java.util.UUID

data class RoutineListUiState(
    val categories: List<RoutineCategory> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val filteredRoutines: List<Routine> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val completedSessions: List<CompletedSession> = emptyList(),
    val themeMode: String = "DARK"
)

class RoutineListViewModel(
    private val repository: RoutineRepository
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")

    private data class RepoState(
        val categories: List<RoutineCategory>,
        val routines: List<Routine>,
        val sessions: List<CompletedSession>,
        val themeMode: String
    )

    val uiState: StateFlow<RoutineListUiState> = combine(
        combine(
            repository.categories,
            repository.routines,
            repository.sessions,
            repository.themeMode
        ) { categories, routines, sessions, theme ->
            RepoState(categories, routines, sessions, theme)
        },
        selectedCategoryId,
        searchQuery
    ) { repo, catId, query ->
        val filtered = SmartSearchEngine.search(
            routines = repo.routines,
            categories = repo.categories,
            rawQuery = query,
            selectedCategoryId = catId
        )

        RoutineListUiState(
            categories = repo.categories,
            routines = repo.routines,
            filteredRoutines = filtered,
            selectedCategoryId = catId,
            searchQuery = query,
            completedSessions = repo.sessions,
            themeMode = repo.themeMode
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
}
