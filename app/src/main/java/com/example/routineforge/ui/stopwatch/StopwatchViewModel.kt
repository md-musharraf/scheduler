package com.example.routineforge.ui.stopwatch

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routineforge.service.ActiveTimerType
import com.example.routineforge.service.TimerSessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class StopwatchLap(
    val lapNumber: Int,
    val lapTimeMillis: Long,
    val splitTimeMillis: Long
)

data class StopwatchUiState(
    val elapsedMillis: Long = 0L,
    val isRunning: Boolean = false,
    val laps: List<StopwatchLap> = emptyList()
) {
    val formattedTime: String
        get() {
            val totalSeconds = elapsedMillis / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

    val formattedMillis: String
        get() {
            val hundredths = (elapsedMillis % 1000) / 10
            return "%02d".format(hundredths)
        }
}

class StopwatchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StopwatchUiState())
    val uiState: StateFlow<StopwatchUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var baseTime: Long = 0L

    fun togglePlayPause() {
        if (_uiState.value.isRunning) {
            pause()
        } else {
            start()
        }
    }

    fun start() {
        if (_uiState.value.isRunning) return
        baseTime = SystemClock.elapsedRealtime() - _uiState.value.elapsedMillis
        _uiState.value = _uiState.value.copy(isRunning = true)

        TimerSessionManager.onTogglePlayPause = { togglePlayPause() }
        TimerSessionManager.onStop = { reset() }

        timerJob = viewModelScope.launch {
            while (isActive && _uiState.value.isRunning) {
                val now = SystemClock.elapsedRealtime()
                val elapsed = now - baseTime
                _uiState.value = _uiState.value.copy(elapsedMillis = elapsed)
                TimerSessionManager.updateStopwatch(elapsed, isRunning = true)
                delay(10) // 10ms high-precision refresh (~100fps)
            }
        }
    }

    fun pause() {
        timerJob?.cancel()
        timerJob = null
        _uiState.value = _uiState.value.copy(isRunning = false)
        TimerSessionManager.updateStopwatch(_uiState.value.elapsedMillis, isRunning = false)
    }

    fun lap() {
        val state = _uiState.value
        if (!state.isRunning && state.elapsedMillis == 0L) return

        val lastSplit = state.laps.firstOrNull()?.splitTimeMillis ?: 0L
        val lapTime = state.elapsedMillis - lastSplit
        val newLap = StopwatchLap(
            lapNumber = state.laps.size + 1,
            lapTimeMillis = lapTime,
            splitTimeMillis = state.elapsedMillis
        )
        _uiState.value = state.copy(
            laps = listOf(newLap) + state.laps
        )
    }

    fun reset() {
        pause()
        _uiState.value = StopwatchUiState()
        TimerSessionManager.clearSession(ActiveTimerType.STOPWATCH)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        TimerSessionManager.clearSession(ActiveTimerType.STOPWATCH)
    }
}

