package com.example.routineforge.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routineforge.data.CompletedSession
import com.example.routineforge.data.RoutineRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryStatsUiState(
    val sessions: List<CompletedSession> = emptyList(),
    val totalSessionsCount: Int = 0,
    val totalMinutesSpent: Int = 0,
    val categoryCounts: Map<String, Int> = emptyMap()
) {
    val formattedTotalTime: String
        get() {
            val hours = totalMinutesSpent / 60
            val minutes = totalMinutesSpent % 60
            return when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 -> "${hours}h"
                else -> "${minutes}m"
            }
        }
}

class HistoryStatsViewModel(
    private val repository: RoutineRepository
) : ViewModel() {

    val uiState: StateFlow<HistoryStatsUiState> = repository.sessions.map { sessions ->
        val totalSecs = sessions.sumOf { it.durationSeconds }
        val categoryCounts = sessions.groupBy { it.categoryName }.mapValues { it.value.size }
        HistoryStatsUiState(
            sessions = sessions,
            totalSessionsCount = sessions.size,
            totalMinutesSpent = totalSecs / 60,
            categoryCounts = categoryCounts
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HistoryStatsUiState()
    )

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
