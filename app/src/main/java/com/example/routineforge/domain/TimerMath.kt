package com.example.routineforge.domain

import com.example.routineforge.core.database.FocusSession
import com.example.routineforge.core.database.SessionStatus
import com.example.routineforge.core.database.SessionType

/**
 * Timestamp-based timer math. Nothing here counts down: every value is derived from the
 * persisted session and the current wall clock, so it survives rotation, backgrounding and
 * process death.
 */
// ponytail: wall clock (not elapsedRealtime) so a session survives reboots; a manual clock change
// while RUNNING shifts elapsed time. Store elapsedRealtime + boot count too if that ever matters.
object TimerMath {
    const val MIN_SAVE_MS = 60_000L

    fun elapsed(s: FocusSession, now: Long): Long {
        val running = if (s.status == SessionStatus.RUNNING) s.segmentStartedAt?.let { (now - it).coerceAtLeast(0L) } ?: 0L else 0L
        return s.actualDurationMs + running
    }

    fun remaining(s: FocusSession, now: Long): Long = (s.plannedDurationMs - elapsed(s, now)).coerceAtLeast(0L)

    /** Wall-clock time the running session reaches its planned duration; null when not running. */
    fun endsAt(s: FocusSession): Long? =
        if (s.status == SessionStatus.RUNNING) s.segmentStartedAt?.let { it + s.plannedDurationMs - s.actualDurationMs } else null

    /** Delay until the rounded-up clock (TimeFormat.clock) shows its next value, so ticks land on the second. */
    fun msToNextSecond(remaining: Long): Long = (remaining % 1000).takeIf { it > 0 } ?: 1000

    fun isDue(s: FocusSession, now: Long): Boolean = s.status == SessionStatus.RUNNING && remaining(s, now) == 0L

    /** Progress of the planned duration, 0..1. */
    fun fraction(s: FocusSession, now: Long): Float =
        if (s.plannedDurationMs <= 0) 0f else (elapsed(s, now).toFloat() / s.plannedDurationMs).coerceIn(0f, 1f)

    /** After a focus: a long break every [every]th focus session, otherwise a short one. After a break: focus. */
    fun nextType(finished: SessionType, focusSessionsToday: Int, every: Int): SessionType = when (finished) {
        SessionType.FOCUS -> if (every > 0 && focusSessionsToday > 0 && focusSessionsToday % every == 0) SessionType.LONG_BREAK else SessionType.SHORT_BREAK
        else -> SessionType.FOCUS
    }
}
