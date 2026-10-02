package com.example.routineforge.domain

import com.example.routineforge.core.database.RepeatRule
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object Recurrence {

    fun occurrence(rule: RepeatRule, start: LocalDateTime, k: Long): LocalDateTime = when (rule) {
        RepeatRule.NONE -> start
        RepeatRule.DAILY -> start.plusDays(k)
        RepeatRule.WEEKLY -> start.plusWeeks(k)
        // Always stepped from the original start so Jan 31 → Feb 28 → Mar 31 stays anchored.
        RepeatRule.MONTHLY -> start.plusMonths(k)
    }

    /** Index of an occurrence at or just before [t], so loops start near the window instead of at the anchor. */
    private fun firstIndexNear(rule: RepeatRule, start: LocalDateTime, t: LocalDateTime): Long {
        if (!t.isAfter(start)) return 0
        val k = when (rule) {
            RepeatRule.NONE -> 0
            RepeatRule.DAILY -> ChronoUnit.DAYS.between(start, t)
            RepeatRule.WEEKLY -> ChronoUnit.WEEKS.between(start, t)
            RepeatRule.MONTHLY -> ChronoUnit.MONTHS.between(start, t)
        }
        return (k - 1).coerceAtLeast(0)
    }

    /** First occurrence strictly after [after], or null when a one-off is already past. */
    fun next(rule: RepeatRule, start: LocalDateTime, after: LocalDateTime): LocalDateTime? {
        if (start.isAfter(after)) return start
        if (rule == RepeatRule.NONE) return null
        var k = firstIndexNear(rule, start, after)
        while (true) {
            val t = occurrence(rule, start, k)
            if (t.isAfter(after)) return t
            k++
        }
    }

    /** Starts of occurrences lasting [durationMinutes] that overlap [from, to). */
    fun between(rule: RepeatRule, start: LocalDateTime, durationMinutes: Long, from: LocalDateTime, to: LocalDateTime): List<LocalDateTime> {
        val out = ArrayList<LocalDateTime>()
        if (rule == RepeatRule.NONE) {
            if (start.isBefore(to) && start.plusMinutes(durationMinutes.coerceAtLeast(1)).isAfter(from)) out += start
            return out
        }
        var k = firstIndexNear(rule, start, from.minusMinutes(durationMinutes))
        while (true) {
            val t = occurrence(rule, start, k)
            if (!t.isBefore(to)) break
            if (t.plusMinutes(durationMinutes.coerceAtLeast(1)).isAfter(from)) out += t
            k++
        }
        return out
    }
}
