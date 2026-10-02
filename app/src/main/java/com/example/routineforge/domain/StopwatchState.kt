package com.example.routineforge.domain

/**
 * Timestamp-based stopwatch, like [TimerMath]: nothing counts, elapsed time is derived from
 * [startedAt] and the wall clock, so it survives backgrounding and process death.
 * [laps] holds cumulative split times; a lap's own length is the gap to the previous split.
 */
data class StopwatchState(val startedAt: Long? = null, val bankedMs: Long = 0L, val laps: List<Long> = emptyList()) {
    val running: Boolean get() = startedAt != null
    val isIdle: Boolean get() = !running && bankedMs == 0L

    fun elapsed(now: Long): Long = bankedMs + (startedAt?.let { (now - it).coerceAtLeast(0L) } ?: 0L)

    fun start(now: Long) = if (running) this else copy(startedAt = now)
    fun pause(now: Long) = if (running) copy(startedAt = null, bankedMs = elapsed(now)) else this
    fun lap(now: Long) = if (running) copy(laps = laps + elapsed(now)) else this

    /** Length of each lap, oldest first. */
    val lapLengths: List<Long> get() = laps.mapIndexed { i, split -> split - (laps.getOrNull(i - 1) ?: 0L) }
}
