package com.example.routineforge.data

import androidx.compose.runtime.Immutable
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

/** hourKey (epochDay * 24 + local hour) → subject → focused seconds. */
typealias FocusLog = Map<Long, Map<String, Int>>

/** A daily target for one subject, e.g. "Math" for 120 minutes. */
@Immutable
data class FocusGoal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val minutes: Int,
    val colorHex: Long
) {
    val targetSeconds: Int get() = minutes * 60

    fun sanitized(): FocusGoal = copy(title = title.trim().take(40), minutes = minutes.coerceIn(1, 1440))

    companion object {
        val PALETTE = listOf(0xFFD71921, 0xFF3B82F6, 0xFF22C55E, 0xFFF59E0B, 0xFF8B5CF6, 0xFF06B6D4, 0xFFEC4899, 0xFF71717A)
    }
}

enum class StatsRange(val label: String) { DAY("Day"), WEEK("Week"), MONTH("Month"), YEAR("Year") }

/** One chart slot: [label] under the axis, [fullLabel] when tapped; [future] slots aren't drawn. */
@Immutable
data class Bucket(val label: String, val fullLabel: String, val seconds: Int, val targetSeconds: Int, val future: Boolean = false)

/** Opens at the previous slot's total and closes at this one; green when close >= open. */
@Immutable
data class Candle(val open: Int, val close: Int, val high: Int, val low: Int)

object FocusStats {
    private val DAY_FMT = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
    private val MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    fun hourKey(t: LocalDateTime): Long = t.toLocalDate().toEpochDay() * 24 + t.hour

    /** Adds [seconds] that ended at [end], spread over the local hours they actually happened in. */
    fun add(log: FocusLog, subject: String, end: LocalDateTime, seconds: Int): FocusLog {
        if (seconds <= 0 || subject.isBlank()) return log
        val out = log.toMutableMap()
        var left = seconds
        var t = end.truncatedTo(ChronoUnit.SECONDS)
        while (left > 0) {
            val inHour = t.minusSeconds(1)
            val avail = Duration.between(inHour.truncatedTo(ChronoUnit.HOURS), t).seconds.toInt()
            val take = minOf(left, avail)
            val key = hourKey(inHour)
            val hour = out[key].orEmpty().toMutableMap()
            hour[subject] = (hour[subject] ?: 0) + take
            out[key] = hour
            left -= take
            t = t.minusSeconds(take.toLong())
        }
        return out
    }

    /** epochDay → subject → seconds. */
    fun days(log: FocusLog): Map<Long, Map<String, Int>> {
        val out = HashMap<Long, MutableMap<String, Int>>()
        for ((key, subjects) in log) {
            val day = out.getOrPut(Math.floorDiv(key, 24L)) { HashMap() }
            subjects.forEach { (s, sec) -> day[s] = (day[s] ?: 0) + sec }
        }
        return out
    }

    /** Seconds counted toward goals: each goal is capped at its target so extra Math can't fill Physics. */
    fun goalProgress(goals: List<FocusGoal>, day: Map<String, Int>): Int =
        goals.sumOf { minOf(day[it.title] ?: 0, it.targetSeconds) }

    fun dailyTarget(goals: List<FocusGoal>): Int = goals.sumOf { it.targetSeconds }

    // ponytail: judges past days against today's goals; snapshot goals per day if users edit them often
    fun goalMet(goals: List<FocusGoal>, day: Map<String, Int>?): Boolean {
        if (day.isNullOrEmpty()) return false
        return if (goals.isEmpty()) day.values.sum() >= 60 else goalProgress(goals, day) >= dailyTarget(goals)
    }

    /** Consecutive goal days ending today (or yesterday, while today is still in progress). */
    fun streak(goals: List<FocusGoal>, days: Map<Long, Map<String, Int>>, today: LocalDate): Int {
        var d = today.toEpochDay()
        if (!goalMet(goals, days[d])) d--
        var n = 0
        while (goalMet(goals, days[d])) { n++; d-- }
        return n
    }

    fun periodStart(range: StatsRange, anchor: LocalDate): LocalDate = when (range) {
        StatsRange.DAY -> anchor
        StatsRange.WEEK -> anchor.minusDays((anchor.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
        StatsRange.MONTH -> anchor.withDayOfMonth(1)
        StatsRange.YEAR -> anchor.withDayOfYear(1)
    }

    fun shift(range: StatsRange, anchor: LocalDate, n: Int): LocalDate = when (range) {
        StatsRange.DAY -> anchor.plusDays(n.toLong())
        StatsRange.WEEK -> anchor.plusWeeks(n.toLong())
        StatsRange.MONTH -> anchor.plusMonths(n.toLong())
        StatsRange.YEAR -> anchor.plusYears(n.toLong())
    }

    fun periodDays(range: StatsRange, anchor: LocalDate): List<LocalDate> {
        val start = periodStart(range, anchor)
        val count = when (range) {
            StatsRange.DAY -> 1
            StatsRange.WEEK -> 7
            StatsRange.MONTH -> start.lengthOfMonth()
            StatsRange.YEAR -> start.lengthOfYear()
        }
        return (0 until count).map { start.plusDays(it.toLong()) }
    }

    fun periodTitle(range: StatsRange, anchor: LocalDate, today: LocalDate): String {
        val start = periodStart(range, anchor)
        if (start == periodStart(range, today)) {
            return when (range) {
                StatsRange.DAY -> "Today"
                StatsRange.WEEK -> "This week"
                StatsRange.MONTH -> "This month"
                StatsRange.YEAR -> "This year"
            }
        }
        return when (range) {
            StatsRange.DAY -> start.format(DAY_FMT)
            StatsRange.WEEK -> "${start.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))} – " +
                start.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
            StatsRange.MONTH -> start.format(MONTH_FMT)
            StatsRange.YEAR -> start.year.toString()
        }
    }

    fun buckets(
        log: FocusLog,
        days: Map<Long, Map<String, Int>>,
        range: StatsRange,
        anchor: LocalDate,
        dailyTarget: Int,
        now: LocalDateTime
    ): List<Bucket> {
        val today = now.toLocalDate()
        val start = periodStart(range, anchor)
        fun dayTotal(d: LocalDate) = days[d.toEpochDay()]?.values?.sum() ?: 0
        return when (range) {
            StatsRange.DAY -> (0 until 24).map { h ->
                val sec = log[start.toEpochDay() * 24 + h]?.values?.sum() ?: 0
                val future = start.isAfter(today) || (start == today && h > now.hour)
                Bucket(h.toString(), String.format(Locale.US, "%02d:00 – %02d:00", h, (h + 1) % 24), sec, 0, future)
            }
            StatsRange.WEEK, StatsRange.MONTH -> periodDays(range, anchor).map { d ->
                val label = if (range == StatsRange.WEEK) d.dayOfWeek.name.take(1) else d.dayOfMonth.toString()
                Bucket(label, d.format(DAY_FMT), dayTotal(d), dailyTarget, d.isAfter(today))
            }
            StatsRange.YEAR -> (0 until 12).map { m ->
                val first = start.plusMonths(m.toLong())
                val sec = (0 until first.lengthOfMonth()).sumOf { dayTotal(first.plusDays(it.toLong())) }
                Bucket(first.month.name.take(1), first.format(MONTH_FMT), sec, dailyTarget * first.lengthOfMonth(), first.isAfter(today))
            }
        }
    }

    /** [previous] is the slot just before the first bucket, so the first candle has an open too. */
    fun candles(buckets: List<Bucket>, previous: Int): List<Candle> {
        var open = previous
        return buckets.map { b ->
            Candle(open, b.seconds, maxOf(open, b.seconds), minOf(open, b.seconds)).also { open = b.seconds }
        }
    }

    /** Subject totals for the given days, largest first. */
    fun subjectTotals(days: Map<Long, Map<String, Int>>, period: List<LocalDate>): List<Pair<String, Int>> {
        val out = HashMap<String, Int>()
        period.forEach { d -> days[d.toEpochDay()]?.forEach { (s, sec) -> out[s] = (out[s] ?: 0) + sec } }
        return out.toList().sortedByDescending { it.second }
    }
}
