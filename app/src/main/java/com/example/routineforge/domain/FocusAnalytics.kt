package com.example.routineforge.domain

import androidx.compose.runtime.Immutable
import com.example.routineforge.core.database.Goal
import com.example.routineforge.core.database.GoalPeriod
import com.example.routineforge.core.database.SessionSlice
import com.example.routineforge.core.database.Subject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class StatsPeriod(val label: String) { DAY("Day"), WEEK("Week"), MONTH("Month"), YEAR("Year");

    val goalPeriod: GoalPeriod
        get() = when (this) {
            DAY -> GoalPeriod.DAILY
            WEEK -> GoalPeriod.WEEKLY
            MONTH -> GoalPeriod.MONTHLY
            YEAR -> GoalPeriod.YEARLY
        }

    fun range(anchor: LocalDate): DateRange = when (this) {
        DAY -> DateRange(anchor, anchor.plusDays(1))
        WEEK -> anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { DateRange(it, it.plusWeeks(1)) }
        MONTH -> anchor.withDayOfMonth(1).let { DateRange(it, it.plusMonths(1)) }
        YEAR -> anchor.withDayOfYear(1).let { DateRange(it, it.plusYears(1)) }
    }

    fun shift(anchor: LocalDate, n: Long): LocalDate = when (this) {
        DAY -> anchor.plusDays(n)
        WEEK -> anchor.plusWeeks(n)
        MONTH -> anchor.plusMonths(n)
        YEAR -> anchor.plusYears(n)
    }
}

@Immutable
data class DateRange(val start: LocalDate, val endExclusive: LocalDate) {
    val dayCount: Int get() = ChronoUnit.DAYS.between(start, endExclusive).toInt()
    val days: List<LocalDate> get() = (0 until dayCount).map { start.plusDays(it.toLong()) }
    operator fun contains(d: LocalDate): Boolean = !d.isBefore(start) && d.isBefore(endExclusive)

    /** Days of this range up to and including [today] (the whole range if it's in the past). */
    fun elapsedDays(today: LocalDate): Int = when {
        today.isBefore(start) -> 0
        today.isBefore(endExclusive) -> ChronoUnit.DAYS.between(start, today).toInt() + 1
        else -> dayCount
    }

    fun startMillis(zone: ZoneId): Long = start.atStartOfDay(zone).toInstant().toEpochMilli()
    fun endMillis(zone: ZoneId): Long = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
}

@Immutable
data class FocusStats(
    val range: DateRange,
    val totalMs: Long,
    val byDay: Map<LocalDate, Long>,
    /** Hour of day (0..23) → focused ms, summed over the range. */
    val byHour: List<Long>,
    val bySubject: Map<Long?, Long>,
    val sessionCount: Int
) {
    fun activeDays(minMs: Long): Int = byDay.values.count { it >= minMs }

    /**
     * Busiest continuous window of up to [width] hours as start until end (exclusive), trimmed
     * of empty edge hours so all-9-AM focus reads "9–10 AM", not "7–10 AM". Null without data.
     */
    fun peakWindow(width: Int): Pair<Int, Int>? {
        if (totalMs <= 0) return null
        var start = (0..24 - width).maxBy { h -> (h until h + width).sumOf { byHour[it] } }
        var end = start + width
        while (byHour[start] == 0L) start++
        while (byHour[end - 1] == 0L) end--
        return start to end
    }

    companion object {
        fun empty(range: DateRange) = FocusStats(range, 0, emptyMap(), List(24) { 0L }, emptyMap(), 0)
    }
}

/** One reusable analytics engine: every screen derives its numbers from [stats]. */
object FocusAnalytics {

    /**
     * Focus time is spread over the session's wall-clock span, split at local hour boundaries.
     */
    // ponytail: pauses aren't stored as segments, so a long pause spreads focus into paused hours.
    // Persist run segments if hourly analytics must be exact.
    fun spread(s: SessionSlice, zone: ZoneId, visit: (hourStart: LocalDateTime, ms: Long) -> Unit) {
        val actual = s.actualDurationMs
        if (actual <= 0) return
        val span = s.endTime - s.startTime
        if (span <= 0) {
            visit(Instant.ofEpochMilli(s.endTime - 1).atZone(zone).truncatedTo(ChronoUnit.HOURS).toLocalDateTime(), actual)
            return
        }
        var t = s.startTime
        var assigned = 0L
        while (t < s.endTime) {
            val hour = Instant.ofEpochMilli(t).atZone(zone).truncatedTo(ChronoUnit.HOURS)
            val next = minOf(hour.plusHours(1).toInstant().toEpochMilli(), s.endTime)
            val share = if (next == s.endTime) actual - assigned else actual * (next - t) / span
            assigned += share
            visit(hour.toLocalDateTime(), share)
            t = next
        }
    }

    fun stats(sessions: List<SessionSlice>, range: DateRange, zone: ZoneId): FocusStats {
        val byDay = HashMap<LocalDate, Long>()
        val byHour = LongArray(24)
        val bySubject = HashMap<Long?, Long>()
        var total = 0L
        var count = 0
        for (s in sessions) {
            var counted = false
            spread(s, zone) { hour, ms ->
                val day = hour.toLocalDate()
                if (day in range) {
                    byDay[day] = (byDay[day] ?: 0L) + ms
                    byHour[hour.hour] += ms
                    bySubject[s.subjectId] = (bySubject[s.subjectId] ?: 0L) + ms
                    total += ms
                    counted = true
                }
            }
            if (counted) count++
        }
        return FocusStats(range, total, byDay, byHour.toList(), bySubject, count)
    }

    /** All-time focused ms per local day. */
    fun dayTotals(sessions: List<SessionSlice>, zone: ZoneId): Map<LocalDate, Long> {
        val out = HashMap<LocalDate, Long>()
        for (s in sessions) spread(s, zone) { hour, ms -> out[hour.toLocalDate()] = (out[hour.toLocalDate()] ?: 0L) + ms }
        return out
    }

    /**
     * Consecutive active days ending today, or ending yesterday while today isn't active yet.
     * Only real focus time counts; [minMs] is the "active day" threshold.
     */
    fun streak(dayTotals: Map<LocalDate, Long>, today: LocalDate, minMs: Long): Int {
        val threshold = minMs.coerceAtLeast(1L)
        var d = today
        if ((dayTotals[d] ?: 0L) < threshold) d = d.minusDays(1)
        var n = 0
        while ((dayTotals[d] ?: 0L) >= threshold) {
            n++
            d = d.minusDays(1)
        }
        return n
    }

    fun longestStreak(dayTotals: Map<LocalDate, Long>, minMs: Long): Int {
        val threshold = minMs.coerceAtLeast(1L)
        val active = dayTotals.filterValues { it >= threshold }.keys.sorted()
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in active) {
            run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
            best = maxOf(best, run)
            prev = d
        }
        return best
    }

    /** Goal progress capped at 100%. */
    fun progress(actualMs: Long, targetMinutes: Int): Float =
        if (targetMinutes <= 0) 0f else (actualMs / (targetMinutes * 60_000f)).coerceIn(0f, 1f)

    /** A subject's target for [period]; weekly/monthly/yearly fall back to the daily target × days. */
    fun subjectTarget(subject: Subject, period: StatsPeriod, range: DateRange): Int = when (period) {
        StatsPeriod.DAY -> subject.dailyTargetMinutes
        StatsPeriod.WEEK -> subject.weeklyTargetMinutes.takeIf { it > 0 } ?: (subject.dailyTargetMinutes * 7)
        StatsPeriod.MONTH -> subject.monthlyTargetMinutes.takeIf { it > 0 } ?: (subject.dailyTargetMinutes * range.dayCount)
        StatsPeriod.YEAR -> subject.yearlyTargetMinutes.takeIf { it > 0 } ?: (subject.dailyTargetMinutes * range.dayCount)
    }

    /**
     * Overall target for [period]: its own goal if set, else the daily goal × days in the range,
     * else the sum of active subjects' targets (0 = no target).
     */
    fun overallTarget(goals: Map<GoalPeriod, Goal>, subjects: List<Subject>, period: StatsPeriod, range: DateRange): Int {
        goals[period.goalPeriod]?.targetMinutes?.takeIf { it > 0 }?.let { return it }
        goals[GoalPeriod.DAILY]?.targetMinutes?.takeIf { it > 0 }?.let { return it * range.dayCount }
        return subjects.filter { !it.isArchived }.sumOf { subjectTarget(it, period, range) }
    }
}
