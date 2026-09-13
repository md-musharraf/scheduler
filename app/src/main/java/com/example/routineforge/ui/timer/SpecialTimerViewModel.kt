package com.example.routineforge.ui.timer

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.routineforge.data.RoutineCategory
import com.example.routineforge.util.AlertHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class SpecialTimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

data class SpecialTimerUiState(
    val status: SpecialTimerStatus = SpecialTimerStatus.IDLE,
    val selectedCategory: RoutineCategory = RoutineCategory.DEFAULT_CATEGORIES[0],
    val availableCategories: List<RoutineCategory> = RoutineCategory.DEFAULT_CATEGORIES,
    val selectedHours: Int = 0,
    val selectedMinutes: Int = 5,
    val selectedSeconds: Int = 0,
    val totalDurationMillis: Long = 5 * 60 * 1000L,
    val remainingMillis: Long = 5 * 60 * 1000L
) {
    val progress: Float
        get() = if (totalDurationMillis > 0L) {
            (remainingMillis.toFloat() / totalDurationMillis.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val formattedRemaining: String
        get() {
            val totalSec = remainingMillis / 1000
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            return if (h > 0) {
                String.format("%02d:%02d:%02d", h, m, s)
            } else {
                String.format("%02d:%02d", m, s)
            }
        }

    val formattedMillis: String
        get() {
            val ms = (remainingMillis % 1000) / 10
            return String.format("%02d", ms)
        }
}

class SpecialTimerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val alertHelper = AlertHelper(application.applicationContext)

    private val _uiState = MutableStateFlow(SpecialTimerUiState())
    val uiState: StateFlow<SpecialTimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var targetEndTime: Long = 0L

    fun selectCategory(category: RoutineCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun setTime(hours: Int, minutes: Int, seconds: Int) {
        if (_uiState.value.status != SpecialTimerStatus.IDLE) return
        val totalMs = (hours * 3600L + minutes * 60L + seconds) * 1000L
        _uiState.update {
            it.copy(
                selectedHours = hours,
                selectedMinutes = minutes,
                selectedSeconds = seconds,
                totalDurationMillis = totalMs,
                remainingMillis = totalMs
            )
        }
    }

    fun addPresetTime(additionalMinutes: Int) {
        if (_uiState.value.status == SpecialTimerStatus.IDLE) {
            val currentTotalMinutes = _uiState.value.selectedHours * 60 + _uiState.value.selectedMinutes
            val newTotalMinutes = (currentTotalMinutes + additionalMinutes).coerceIn(1, 1440)
            val h = newTotalMinutes / 60
            val m = newTotalMinutes % 60
            setTime(h, m, _uiState.value.selectedSeconds)
        } else {
            // If already running or paused, add time dynamically
            val additionalMs = additionalMinutes * 60 * 1000L
            targetEndTime += additionalMs
            _uiState.update {
                val newTotal = it.totalDurationMillis + additionalMs
                val newRemaining = it.remainingMillis + additionalMs
                it.copy(
                    totalDurationMillis = newTotal,
                    remainingMillis = newRemaining
                )
            }
        }
    }

    fun start() {
        val currentRemaining = _uiState.value.remainingMillis
        if (currentRemaining <= 0L) return

        targetEndTime = SystemClock.elapsedRealtime() + currentRemaining
        _uiState.update { it.copy(status = SpecialTimerStatus.RUNNING) }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                val left = (targetEndTime - now).coerceAtLeast(0L)
                _uiState.update { it.copy(remainingMillis = left) }

                if (left <= 0L) {
                    _uiState.update { it.copy(status = SpecialTimerStatus.FINISHED, remainingMillis = 0L) }
                    // Trigger 1-second authoritative alert vibration & tone
                    alertHelper.playRoutineComplete(soundEnabled = true, vibeEnabled = true)
                    break
                }
                delay(12L) // ~60-80fps millisecond refresh
            }
        }
    }

    fun pause() {
        if (_uiState.value.status != SpecialTimerStatus.RUNNING) return
        timerJob?.cancel()
        val now = SystemClock.elapsedRealtime()
        val left = (targetEndTime - now).coerceAtLeast(0L)
        _uiState.update {
            it.copy(
                status = SpecialTimerStatus.PAUSED,
                remainingMillis = left
            )
        }
    }

    fun resume() {
        if (_uiState.value.status != SpecialTimerStatus.PAUSED) return
        start()
    }

    fun reset() {
        timerJob?.cancel()
        val initialMs = (_uiState.value.selectedHours * 3600L + _uiState.value.selectedMinutes * 60L + _uiState.value.selectedSeconds) * 1000L
        _uiState.update {
            it.copy(
                status = SpecialTimerStatus.IDLE,
                remainingMillis = initialMs,
                totalDurationMillis = initialMs
            )
        }
    }

    fun restart() {
        reset()
        start()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
