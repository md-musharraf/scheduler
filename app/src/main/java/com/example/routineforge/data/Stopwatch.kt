package com.example.routineforge.data

import android.content.Context
import androidx.core.content.edit
import com.example.routineforge.core.notifications.Notifier
import com.example.routineforge.domain.StopwatchState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The stopwatch use case: state in SharedPreferences, ongoing notification kept in sync. */
class Stopwatch(private val context: Context, private val clock: () -> Long = System::currentTimeMillis) {
    private val prefs = context.getSharedPreferences("stopwatch", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<StopwatchState> = _state.asStateFlow()

    fun toggle() = set { if (it.running) it.pause(clock()) else it.start(clock()) }
    fun lap() = set { it.lap(clock()) }
    fun reset() = set { StopwatchState() }

    /** Re-posts the notification (after reboot or app start). */
    fun restore() = sync(_state.value)

    @Synchronized
    private fun set(change: (StopwatchState) -> StopwatchState) {
        val s = change(_state.value)
        prefs.edit {
            putLong(K_STARTED, s.startedAt ?: -1L)
            putLong(K_BANKED, s.bankedMs)
            putString(K_LAPS, s.laps.joinToString(","))
        }
        _state.value = s
        sync(s)
    }

    private fun load() = StopwatchState(
        startedAt = prefs.getLong(K_STARTED, -1L).takeIf { it >= 0 },
        bankedMs = prefs.getLong(K_BANKED, 0L),
        laps = prefs.getString(K_LAPS, "").orEmpty().split(',').mapNotNull { it.toLongOrNull() }
    )

    private fun sync(s: StopwatchState) =
        if (s.isIdle) Notifier.cancel(context, Notifier.ID_STOPWATCH) else Notifier.showStopwatch(context, s, clock())

    private companion object {
        const val K_STARTED = "started_at"
        const val K_BANKED = "banked_ms"
        const val K_LAPS = "laps"
    }
}
