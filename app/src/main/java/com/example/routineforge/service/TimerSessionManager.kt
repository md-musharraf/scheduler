package com.example.routineforge.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ActiveTimerType {
    NONE,
    SPECIAL_TIMER,
    STOPWATCH,
    ROUTINE
}

data class ActiveTimerSession(
    val type: ActiveTimerType = ActiveTimerType.NONE,
    val title: String = "",
    val categoryColorHex: Long = 0xFFD71921,
    val remainingMillis: Long = 0L,
    val elapsedMillis: Long = 0L,
    val totalDurationMillis: Long = 0L,
    val isRunning: Boolean = false,
    val routineId: String? = null
) {
    val formattedTime: String
        get() = when (type) {
            ActiveTimerType.STOPWATCH -> {
                val totalSec = elapsedMillis / 1000
                val m = (totalSec / 60)
                val s = totalSec % 60
                "%02d:%02d".format(m, s)
            }
            else -> {
                val totalSec = remainingMillis / 1000
                val m = (totalSec / 60)
                val s = totalSec % 60
                "%02d:%02d".format(m, s)
            }
        }
}

object TimerSessionManager {
    private val _session = MutableStateFlow(ActiveTimerSession())
    val session: StateFlow<ActiveTimerSession> = _session.asStateFlow()

    var onTogglePlayPause: (() -> Unit)? = null
    var onStop: (() -> Unit)? = null
    var appContext: android.content.Context? = null
    private var lastWidgetUpdate: Long = 0L

    fun notifyWidget() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastWidgetUpdate > 950L) {
            lastWidgetUpdate = now
            appContext?.let { ctx ->
                try {
                    com.example.routineforge.widget.NothingTimerWidgetProvider.updateAllWidgets(ctx)
                } catch (_: Exception) {}
            }
        }
    }

    fun updateSpecialTimer(
        title: String,
        colorHex: Long,
        remainingMillis: Long,
        totalDurationMillis: Long,
        isRunning: Boolean
    ) {
        _session.update {
            it.copy(
                type = ActiveTimerType.SPECIAL_TIMER,
                title = title,
                categoryColorHex = colorHex,
                remainingMillis = remainingMillis,
                totalDurationMillis = totalDurationMillis,
                isRunning = isRunning
            )
        }
        notifyWidget()
    }

    fun updateStopwatch(
        elapsedMillis: Long,
        isRunning: Boolean
    ) {
        _session.update {
            it.copy(
                type = ActiveTimerType.STOPWATCH,
                title = "STOPWATCH",
                categoryColorHex = 0xFFD71921,
                elapsedMillis = elapsedMillis,
                isRunning = isRunning
            )
        }
        notifyWidget()
    }

    fun updateRoutine(
        routineId: String,
        routineTitle: String,
        subtaskTitle: String,
        remainingSec: Int,
        totalSec: Int,
        isRunning: Boolean
    ) {
        _session.update {
            it.copy(
                type = ActiveTimerType.ROUTINE,
                title = "$routineTitle: $subtaskTitle",
                remainingMillis = remainingSec * 1000L,
                totalDurationMillis = totalSec * 1000L,
                isRunning = isRunning,
                routineId = routineId
            )
        }
        notifyWidget()
    }

    fun clearSession(type: ActiveTimerType) {
        if (_session.value.type == type) {
            _session.value = ActiveTimerSession()
            onTogglePlayPause = null
            onStop = null
            notifyWidget()
        }
    }
}
