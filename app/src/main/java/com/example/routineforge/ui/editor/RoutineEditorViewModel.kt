package com.example.routineforge.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routineforge.data.Routine
import com.example.routineforge.data.RoutineCategory
import com.example.routineforge.data.RoutineRepository
import com.example.routineforge.data.RoutineStep
import com.example.routineforge.data.StepType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class RoutineEditorUiState(
    val routineId: String? = null,
    val title: String = "",
    val description: String = "",
    val categoryId: String = "",
    val colorHex: Long = 0xFF6366F1,
    val isSimpleMode: Boolean = false,
    val simpleDurationMinutes: Int = 30,
    val steps: List<RoutineStep> = emptyList(),
    val categories: List<RoutineCategory> = emptyList(),
    val isSaved: Boolean = false,
    val errorMessage: String? = null
) {
    val totalDurationSeconds: Int
        get() = if (isSimpleMode || steps.isEmpty()) {
            simpleDurationMinutes * 60
        } else {
            steps.sumOf { it.durationSeconds }
        }

    val formattedDuration: String
        get() {
            val total = totalDurationSeconds
            val hours = total / 3600
            val minutes = (total % 3600) / 60
            val seconds = total % 60
            return when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 -> "${hours}h"
                minutes > 0 && seconds > 0 -> "${minutes}m ${seconds}s"
                minutes > 0 -> "${minutes}m"
                else -> "${seconds}s"
            }
        }
}

class RoutineEditorViewModel(
    private val repository: RoutineRepository,
    private val initialRoutineId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineEditorUiState(routineId = initialRoutineId))
    val uiState: StateFlow<RoutineEditorUiState> = _uiState.asStateFlow()

    init {
        val categories = repository.categories.value
        val defaultCategoryId = categories.firstOrNull()?.id ?: "cat_study"

        if (initialRoutineId != null) {
            val existing = repository.getRoutine(initialRoutineId)
            if (existing != null) {
                val isSimple = existing.steps.size <= 1
                val durMin = if (existing.steps.isNotEmpty()) {
                    (existing.steps.sumOf { it.durationSeconds } / 60).coerceAtLeast(5)
                } else 30

                _uiState.value = _uiState.value.copy(
                    title = existing.title,
                    description = existing.description,
                    categoryId = existing.categoryId,
                    colorHex = existing.colorHex,
                    isSimpleMode = isSimple,
                    simpleDurationMinutes = durMin,
                    steps = existing.steps,
                    categories = categories
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    categoryId = defaultCategoryId,
                    categories = categories
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(
                categoryId = defaultCategoryId,
                categories = categories
            )
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title, errorMessage = null)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun selectCategory(categoryId: String) {
        _uiState.value = _uiState.value.copy(categoryId = categoryId)
    }

    fun selectColor(colorHex: Long) {
        _uiState.value = _uiState.value.copy(colorHex = colorHex)
    }

    fun setSimpleMode(isSimple: Boolean) {
        _uiState.value = _uiState.value.copy(isSimpleMode = isSimple, errorMessage = null)
    }

    fun updateSimpleDurationMinutes(minutes: Int) {
        val safeMin = minutes.coerceIn(1, 720)
        _uiState.value = _uiState.value.copy(simpleDurationMinutes = safeMin)
    }

    fun addStep(step: RoutineStep) {
        val updated = _uiState.value.steps.toMutableList().apply { add(step) }
        _uiState.value = _uiState.value.copy(steps = updated, errorMessage = null)
    }

    fun updateStep(step: RoutineStep) {
        val updated = _uiState.value.steps.map { if (it.id == step.id) step else it }
        _uiState.value = _uiState.value.copy(steps = updated)
    }

    fun deleteStep(stepId: String) {
        val updated = _uiState.value.steps.filter { it.id != stepId }
        _uiState.value = _uiState.value.copy(steps = updated)
    }

    fun moveStepUp(index: Int) {
        if (index <= 0 || index >= _uiState.value.steps.size) return
        val list = _uiState.value.steps.toMutableList()
        val temp = list[index]
        list[index] = list[index - 1]
        list[index - 1] = temp
        _uiState.value = _uiState.value.copy(steps = list)
    }

    fun moveStepDown(index: Int) {
        if (index < 0 || index >= _uiState.value.steps.size - 1) return
        val list = _uiState.value.steps.toMutableList()
        val temp = list[index]
        list[index] = list[index + 1]
        list[index + 1] = temp
        _uiState.value = _uiState.value.copy(steps = list)
    }

    fun saveRoutine() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter a routine title")
            return
        }

        // Subcategory is completely optional!
        // If simple mode is chosen or no sub-steps were added, auto-synthesize a clean focus step.
        val finalSteps = if (state.isSimpleMode || state.steps.isEmpty()) {
            listOf(
                RoutineStep(
                    id = UUID.randomUUID().toString(),
                    title = state.title.trim(),
                    durationSeconds = state.simpleDurationMinutes * 60,
                    stepType = StepType.WORK,
                    instruction = state.description.trim().ifBlank { "Continuous focus session" }
                )
            )
        } else {
            state.steps
        }

        viewModelScope.launch {
            val routine = Routine(
                id = state.routineId ?: UUID.randomUUID().toString(),
                title = state.title.trim(),
                description = state.description.trim(),
                categoryId = state.categoryId,
                colorHex = state.colorHex,
                steps = finalSteps
            )
            repository.saveRoutine(routine)
            _uiState.value = state.copy(isSaved = true)
        }
    }
}
